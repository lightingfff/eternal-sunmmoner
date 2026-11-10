package com.example.data

import com.example.data.model.*
import kotlin.random.Random

data class CruelSynthesisOutcome(
  val updatedRoster: List<HeroRecord>,
  val updatedTarget: HeroRecord,
  val sacrificedNames: List<String>,
  val bonusAtk: Long,
  val bonusDef: Long,
  val bonusMaxHp: Long,
  val bonusExp: Long,
  val inheritedTraitName: String?,
  val cowardHeroesAffected: Int,
)

data class SynthesisBonuses(val attack: Long, val defense: Long, val maxHp: Long, val experience: Long)

/** Pure operation reused inside the guest mutex and the cloud transaction. */
object LobbyHeroAssignments {
  const val PARTY_LIMIT = 4
  const val INFIRMARY_HEAL_GOLD_COST = 80L
  const val HEARTY_MEAL_GOLD_COST = 65L
  const val EXPEDITION_SURVIVAL_FLAG_PREFIX = "SURVIVED_EXP:"
  const val LOW_HP_RETURN_FLAG = "RETURNED_LOW_HP_15"

  private val socialEventIdPattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
  fun isValidSocialEventId(id: String): Boolean = socialEventIdPattern.matches(id)

  fun synthesisBonuses(target: HeroRecord, materials: List<HeroRecord>): SynthesisBonuses {
    val levelCapacity = (target.level.coerceIn(1L, 100L) until 100L).sumOf { it * 100L }
    val experienceCapacity = (10_000_000L + levelCapacity - target.exp).coerceAtLeast(0L)
    return SynthesisBonuses(
      attack = (materials.sumOf { it.attack } / 5L).coerceIn(0L, (50_000L - target.attack).coerceAtLeast(0L)),
      defense = (materials.sumOf { it.defense } / 5L).coerceIn(0L, (50_000L - target.defense).coerceAtLeast(0L)),
      maxHp = (materials.sumOf { it.maxHp } / 5L).coerceIn(0L, (100_000L - target.maxHp).coerceAtLeast(0L)),
      experience = materials.sumOf { 65L * it.level.coerceAtLeast(1L) + 35L * it.rarity.coerceAtLeast(1L) }
        .coerceIn(0L, experienceCapacity),
    )
  }

  fun survivedExpeditionCount(bondSummary: String): Int {
    return Regex("\\[${Regex.escape(EXPEDITION_SURVIVAL_FLAG_PREFIX)}(\\d+)]")
      .findAll(bondSummary)
      .map { it.groupValues[1].toLongOrNull()?.coerceIn(0L, Int.MAX_VALUE.toLong())?.toInt() ?: 0 }
      .maxOrNull() ?: 0
  }

  fun hasReturnedUnder15PctHp(bondSummary: String): Boolean =
    bondSummary.contains("[$LOW_HP_RETURN_FLAG]")

  fun recordExpeditionSurvival(
    bondSummary: String,
    survivedThisRun: Boolean,
    returnedUnder15PctHp: Boolean,
  ): String {
    val prevCount = survivedExpeditionCount(bondSummary)
    val nextCount = if (survivedThisRun) (prevCount.toLong() + 1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() else prevCount
    val hadLowHp = hasReturnedUnder15PctHp(bondSummary) || (survivedThisRun && returnedUnder15PctHp)
    val stripped = bondSummary
      .replace(Regex("\\s*\\[$EXPEDITION_SURVIVAL_FLAG_PREFIX\\d+]"), "")
      .replace(Regex("\\s*\\[$LOW_HP_RETURN_FLAG]"), "")
      .trim()
    val tags = buildString {
      if (nextCount > 0) append(" [$EXPEDITION_SURVIVAL_FLAG_PREFIX$nextCount]")
      if (hadLowHp) append(" [$LOW_HP_RETURN_FLAG]")
    }
    return (stripped.take((200 - tags.length).coerceAtLeast(0)).trimEnd() + tags).trim()
  }

  fun checkAndUnlockHiddenTrait(hero: HeroRecord, now: Long = System.currentTimeMillis() / 1000L): HeroRecord {
    val survivedCount = maxOf(hero.survivedExpeditions.coerceAtLeast(0), survivedExpeditionCount(hero.bondSummary))
      .coerceAtMost(1_000_000)
    val lowHpReturn = hero.returnedUnder15PctHp || hasReturnedUnder15PctHp(hero.bondSummary)
    val migrated = hero.copy(survivedExpeditions = survivedCount, returnedUnder15PctHp = lowHpReturn)
    if (migrated.currentAssignment == HeroAssignment.SACRIFICED.name) return migrated
    val data = HeroData.fromRecord(migrated, now)
    val shouldUnlock = migrated.rarity >= 3L || migrated.hiddenTrait.canUnlock(
      level = data.level,
      morale = data.morale,
      sanity = data.sanityState,
      survivedExpeditions = survivedCount,
      returnedLowHpOnce = lowHpReturn,
    )
    val legacyRevealedTrait = migrated.hiddenTrait.unlocked &&
      (migrated.hiddenTrait.id == "unyielding_soul" || HeroHiddenTrait.AWAKENED_TRAIT_POOL.any { it.first == migrated.hiddenTrait.id }) &&
      (migrated.hiddenTrait.name.isBlank() || migrated.hiddenTrait.name == "Linh hồn bất khuất")
    val appraised = if (shouldUnlock && (!migrated.hiddenTrait.unlocked || legacyRevealedTrait)) {
      migrated.copy(hiddenTrait = HeroHiddenTrait.resolveAwakenedTrait(migrated.hiddenTrait, migrated.id))
    } else migrated
    return appraised.copy(sanityState = appraised.effectiveSanityState().name)
  }

  fun assign(
    roster: List<HeroRecord>,
    heroId: String,
    assignment: HeroAssignment,
    now: Long,
    replaceWhenFull: Boolean = true,
  ): List<HeroRecord> {
    require(assignment != HeroAssignment.SACRIFICED) { "Kéo-thả không được hiến tế Anh hùng." }
    val hero = requireNotNull(roster.find { it.id == heroId }) { "Anh hùng không còn trong danh sách sở hữu." }
    val data = HeroData.fromRecord(hero, now)
    require(data.currentAssignment != HeroAssignment.SACRIFICED) { "Anh hùng đã hiến tế không thể nhận nhiệm vụ." }
    if (assignment == HeroAssignment.IN_PARTY) {
      require(!hero.isCurrentlyInjured(now) && data.currentHP > 0) { "Anh hùng cần hồi phục tại Nhà trước khi vào đội." }
    }
    val party = roster.filter { it.id != heroId && it.inActiveSquad && it.currentAssignment != HeroAssignment.SACRIFICED.name }
      .sortedWith(compareBy<HeroRecord> { it.partyJoinedOrder }.thenBy { it.id })
    if (assignment == HeroAssignment.IN_PARTY && party.size >= PARTY_LIMIT) {
      require(replaceWhenFull) { "Đội hình viễn chinh tối đa 4 Anh hùng." }
    }
    val removed = if (assignment == HeroAssignment.IN_PARTY) party.take((party.size - PARTY_LIMIT + 1).coerceAtLeast(0)).map { it.id }.toSet() else emptySet()
    val nextOrder = (roster.maxOfOrNull { it.partyJoinedOrder } ?: 0L).coerceAtMost(Long.MAX_VALUE - 1) + 1
    return roster.map { h ->
      when {
        h.currentAssignment == HeroAssignment.SACRIFICED.name -> h.copy(inActiveSquad = false)
        h.id == heroId -> {
          val appraised = checkAndUnlockHiddenTrait(h, now)
          appraised.copy(
            currentAssignment = assignment.name,
            inActiveSquad = assignment == HeroAssignment.IN_PARTY,
            status = if (h.isCurrentlyInjured(now)) h.status else if (assignment == HeroAssignment.RESTING) HeroStatusType.RESTING.name else HeroStatusType.READY.name,
            currentHp = data.currentHP.toLong(),
            restStartedAtEpochSec = if (assignment == HeroAssignment.RESTING) now else 0L,
            partyJoinedOrder = if (assignment == HeroAssignment.IN_PARTY) {
              if (h.inActiveSquad) h.partyJoinedOrder else nextOrder
            } else 0L,
          )
        }
        h.id in removed -> h.copy(inActiveSquad = false, currentAssignment = HeroAssignment.UNASSIGNED.name, partyJoinedOrder = 0L)
        else -> h
      }
    }
  }

  /**
   * Infirmary "Trị Thương": Costs Gold, restores +50% of Max HP immediately, and clears injury if fully healed.
   */
  fun healHeroAtInfirmary(
    profile: MasterProfile,
    roster: List<HeroRecord>,
    heroId: String,
    now: Long = System.currentTimeMillis() / 1000L,
    goldCost: Long = INFIRMARY_HEAL_GOLD_COST,
  ): Pair<MasterProfile, List<HeroRecord>> {
    require(goldCost == INFIRMARY_HEAL_GOLD_COST) { "Chi phí Trị Thương phải là $INFIRMARY_HEAL_GOLD_COST Vàng." }
    require(profile.gold >= goldCost) { "Cần $goldCost Vàng để Trị Thương (+50% Max HP)." }
    val target = requireNotNull(roster.find { it.id == heroId && it.currentAssignment != HeroAssignment.SACRIFICED.name }) {
      "Không tìm thấy Anh hùng cần trị thương."
    }
    val data = HeroData.fromRecord(target, now)
    require(data.currentHP < data.maxHP || target.statusType == HeroStatusType.INJURED) {
      "Anh hùng đã đầy HP và không bị thương, không cần Trị Thương."
    }
    val healDelta = (data.maxHP / 2).coerceAtLeast(1)
    val nextHp = (data.currentHP + healDelta).coerceAtMost(data.maxHP).toLong()
    val fullyHealed = nextHp == data.maxHP.toLong()
    val nextProfile = profile.copy(gold = profile.gold - goldCost)
    val nextRoster = roster.map { h ->
      if (h.id != heroId) h else {
        val appraised = checkAndUnlockHiddenTrait(h, now)
        val resting = !fullyHealed && (h.isCurrentlyInjured(now) || data.currentAssignment == HeroAssignment.RESTING)
        appraised.copy(
          currentHp = nextHp,
          status = when {
            fullyHealed -> HeroStatusType.READY.name
            h.isCurrentlyInjured(now) -> HeroStatusType.INJURED.name
            resting -> HeroStatusType.RESTING.name
            else -> h.status
          },
          injuryRecoveryUntilEpochSec = if (fullyHealed) 0L else h.injuryRecoveryUntilEpochSec,
          restStartedAtEpochSec = if (resting) h.restStartedAtEpochSec else 0L,
          currentAssignment = when {
            h.inActiveSquad -> HeroAssignment.IN_PARTY.name
            resting -> HeroAssignment.RESTING.name
            else -> HeroAssignment.UNASSIGNED.name
          },
        )
      }
    }
    return nextProfile to nextRoster
  }

  /**
   * Canteen "Bữa Ăn Thịnh Soạn": Costs Gold, restores +40 Morale and clears PANICKED/ANXIOUS sanity state.
   */
  fun serveHeartyMealToHero(
    profile: MasterProfile,
    roster: List<HeroRecord>,
    heroId: String,
    now: Long = System.currentTimeMillis() / 1000L,
    goldCost: Long = HEARTY_MEAL_GOLD_COST,
  ): Pair<MasterProfile, List<HeroRecord>> {
    require(goldCost == HEARTY_MEAL_GOLD_COST) { "Chi phí Bữa Ăn Thịnh Soạn phải là $HEARTY_MEAL_GOLD_COST Vàng." }
    require(profile.gold >= goldCost) { "Cần $goldCost Vàng để dùng Bữa Ăn Thịnh Soạn (+40 Tinh thần & xóa Hoảng loạn)." }
    val target = requireNotNull(roster.find { it.id == heroId && it.currentAssignment != HeroAssignment.SACRIFICED.name }) {
      "Không tìm thấy Anh hùng dùng bữa."
    }
    val sanity = target.effectiveSanityState()
    require(target.morale < 100L || sanity == HeroSanityState.PANICKED || sanity == HeroSanityState.ANXIOUS) {
      "Anh hùng đã đầy Tinh thần và không cần dùng bữa hồi phục."
    }
    val nextProfile = profile.copy(gold = profile.gold - goldCost)
    val nextRoster = roster.map { h ->
      if (h.id != heroId) h else {
        val nextMorale = (h.morale.coerceIn(0L, 100L) + 40L).coerceAtMost(100L)
        val nextSanity = if (h.effectiveSanityState() == HeroSanityState.SELF_AWARE) {
          HeroSanityState.SELF_AWARE.name
        } else {
          HeroSanityState.NORMAL.name
        }
        checkAndUnlockHiddenTrait(
          h.copy(
            morale = nextMorale,
            sanityState = nextSanity,
          ),
          now,
        )
      }
    }
    return nextProfile to nextRoster
  }

  /**
   * Social Proximity Trigger: 3-5 star hero disdains 1-2 star hero, deducting 2 Morale.
   */
  fun applyProximityDisdainMoralePenalty(
    roster: List<HeroRecord>,
    lowStarHeroId: String,
    penalty: Int = 2,
  ): List<HeroRecord> {
    require(penalty == 2) { "Mỗi sự kiện khinh miệt chỉ được trừ 2 Tinh thần." }
    require(roster.any { it.id == lowStarHeroId && it.rarity in 1L..2L && it.currentAssignment != HeroAssignment.SACRIFICED.name }) {
      "Chỉ Anh hùng 1–2 sao còn sống mới chịu tác động khinh miệt."
    }
    return roster.map { h ->
      if (h.id != lowStarHeroId) h else {
        val nextMorale = (h.morale.coerceIn(0L, 100L) - penalty.toLong()).coerceAtLeast(0L)
        val sanity = h.effectiveSanityState()
        val nextSanity = if (nextMorale < 25L && sanity != HeroSanityState.SELF_AWARE && !h.isPanicImmune) {
          HeroSanityState.PANICKED
        } else sanity
        h.copy(morale = nextMorale, sanityState = nextSanity.name)
      }
    }
  }

  /**
   * Cruel Synthesis Chamber (Lò Dung Hợp Tàn Khốc):
   * - Permanently removes 1..3 sacrificed heroes (Permadeath).
   * - Transfers 20% of ATK, DEF, MaxHP and grants EXP to the target hero.
   * - Deducts 10 Morale from all surviving Coward heroes in the roster.
   * - 20% chance (or deterministic roll) for target hero to inherit a sacrificed hero's Hidden Trait.
   */
  fun performCruelSynthesis(
    roster: List<HeroRecord>,
    targetHeroId: String,
    sacrificedHeroIds: List<String>,
    random: Random = Random.Default,
    now: Long = System.currentTimeMillis() / 1000L,
  ): CruelSynthesisOutcome {
    require(sacrificedHeroIds.size in 1..3) { "Hãy chọn từ 1 đến 3 Anh hùng Hiến Tế cho Lò Dung Hợp." }
    require(targetHeroId.isNotBlank() && sacrificedHeroIds.all { it.isNotBlank() }) { "Mã Anh hùng không hợp lệ." }
    require(sacrificedHeroIds.toSet().size == sacrificedHeroIds.size) { "Không được chọn trùng Anh hùng Hiến Tế." }
    require(targetHeroId !in sacrificedHeroIds) { "Anh hùng Chính không thể tự hiến tế." }
    require(roster.map { it.id }.toSet().size == roster.size) { "Danh sách Anh hùng bị trùng mã sở hữu." }
    val activeRoster = roster.filter { it.currentAssignment != HeroAssignment.SACRIFICED.name }
    val target = requireNotNull(activeRoster.find { it.id == targetHeroId }) { "Không tìm thấy Anh hùng Chính." }
    val materials = sacrificedHeroIds.map { id ->
      requireNotNull(activeRoster.find { it.id == id }) { "Anh hùng hiến tế không tồn tại trong danh sách." }
    }
    require(activeRoster.size - materials.size >= 1) { "Phải giữ lại ít nhất 1 Anh hùng sống sót tại Thánh Địa." }

    (materials + target).forEach { h ->
      require(h.maxHp in 50L..100000L && h.attack in 5L..50000L && h.defense in 0L..50000L &&
        h.level in 1L..100L && h.rarity in 1L..5L && h.exp in 0L..10000000L) { "Chỉ số Anh hùng không hợp lệ để dung hợp." }
    }
    val bonuses = synthesisBonuses(target, materials)
    val bonusAtk = bonuses.attack
    val bonusDef = bonuses.defense
    val bonusMaxHp = bonuses.maxHp
    val bonusExp = bonuses.experience

    val totalExp = target.exp + bonusExp
    var nextLevel = target.level
    var remExp = totalExp
    while (nextLevel < 100L && remExp >= nextLevel * 100L) {
      remExp -= nextLevel * 100L
      nextLevel += 1L
    }

    val didInheritTrait = random.nextInt(100) < 20
    val nextHiddenTrait = if (didInheritTrait) {
      val material = if (materials.size == 1) materials.single() else materials[random.nextInt(materials.size)]
      HeroHiddenTrait.resolveAwakenedTrait(material.hiddenTrait, material.id)
    } else {
      target.hiddenTrait
    }

    val targetData = HeroData.fromRecord(target, now)
    val nextMaxHp = (target.maxHp + bonusMaxHp).coerceIn(50L, 100000L)
    val nextCurrentHp = targetData.currentHP.toLong().coerceIn(0L, nextMaxHp)

    val updatedTarget = checkAndUnlockHiddenTrait(
      target.copy(
        level = nextLevel,
        exp = remExp.coerceIn(0L, 10000000L),
        maxHp = nextMaxHp,
        currentHp = nextCurrentHp,
        attack = (target.attack + bonusAtk).coerceIn(5L, 50000L),
        defense = (target.defense + bonusDef).coerceIn(0L, 50000L),
        hiddenTrait = nextHiddenTrait,
      ),
      now,
    )

    val materialSet = sacrificedHeroIds.toSet()
    var cowardAffected = 0
    val updatedRoster = activeRoster
      .filterNot { it.id in materialSet }
      .map { h ->
        val baseHero = if (h.id == targetHeroId) updatedTarget else h
        val isCoward = LobbyPersonality.fromRaw(baseHero.personality) == LobbyPersonality.COWARD
        if (isCoward) {
          cowardAffected++
          val penalizedMorale = (baseHero.morale.coerceIn(0L, 100L) - 10L).coerceAtLeast(0L)
          val sanity = baseHero.effectiveSanityState()
          val nextSanity = if (penalizedMorale < 25L && sanity != HeroSanityState.SELF_AWARE && !baseHero.isPanicImmune) {
            HeroSanityState.PANICKED
          } else {
            sanity
          }
          baseHero.copy(morale = penalizedMorale, sanityState = nextSanity.name)
        } else {
          baseHero
        }
      }

    return CruelSynthesisOutcome(
      updatedRoster = updatedRoster,
      updatedTarget = updatedRoster.first { it.id == targetHeroId },
      sacrificedNames = materials.map { it.name },
      bonusAtk = bonusAtk,
      bonusDef = bonusDef,
      bonusMaxHp = bonusMaxHp,
      bonusExp = bonusExp,
      inheritedTraitName = if (didInheritTrait) nextHiddenTrait.name else null,
      cowardHeroesAffected = cowardAffected,
    )
  }
}

