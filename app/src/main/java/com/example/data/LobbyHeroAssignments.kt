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

/** Pure operation reused inside the guest mutex and the cloud transaction. */
object LobbyHeroAssignments {
  const val PARTY_LIMIT = 4
  const val INFIRMARY_HEAL_GOLD_COST = 80L
  const val HEARTY_MEAL_GOLD_COST = 65L
  const val EXPEDITION_SURVIVAL_FLAG_PREFIX = "SURVIVED_EXP:"
  const val LOW_HP_RETURN_FLAG = "RETURNED_LOW_HP_15"

  fun survivedExpeditionCount(bondSummary: String): Int {
    val match = Regex("$EXPEDITION_SURVIVAL_FLAG_PREFIX(\\d+)").find(bondSummary) ?: return 0
    return match.groupValues[1].toIntOrNull() ?: 0
  }

  fun hasReturnedUnder15PctHp(bondSummary: String): Boolean =
    bondSummary.contains(LOW_HP_RETURN_FLAG)

  fun recordExpeditionSurvival(
    bondSummary: String,
    survivedThisRun: Boolean,
    returnedUnder15PctHp: Boolean,
  ): String {
    val prevCount = survivedExpeditionCount(bondSummary)
    val nextCount = if (survivedThisRun) prevCount + 1 else prevCount
    val hadLowHp = hasReturnedUnder15PctHp(bondSummary) || returnedUnder15PctHp
    val stripped = bondSummary
      .replace(Regex("\\s*\\[$EXPEDITION_SURVIVAL_FLAG_PREFIX\\d+]"), "")
      .replace(Regex("\\s*\\[$LOW_HP_RETURN_FLAG]"), "")
      .trim()
    val tags = buildString {
      if (nextCount > 0) append(" [$EXPEDITION_SURVIVAL_FLAG_PREFIX$nextCount]")
      if (hadLowHp) append(" [$LOW_HP_RETURN_FLAG]")
    }
    return (stripped + tags).trim().take(200)
  }

  fun checkAndUnlockHiddenTrait(hero: HeroRecord, now: Long = System.currentTimeMillis() / 1000L): HeroRecord {
    if (hero.hiddenTrait.unlocked) return hero
    val data = HeroData.fromRecord(hero, now)
    val survivedCount = survivedExpeditionCount(hero.bondSummary)
    val lowHpReturn = hasReturnedUnder15PctHp(hero.bondSummary) || data.isCriticalReturnHp
    val shouldUnlock = hero.hiddenTrait.canUnlock(
      level = data.level,
      morale = data.morale,
      sanity = data.sanityState,
      survivedExpeditions = survivedCount,
      returnedLowHpOnce = lowHpReturn,
    )
    return if (shouldUnlock) {
      val nextTrait = if (survivedCount >= 3 || lowHpReturn || hero.hiddenTrait.name.isBlank()) {
        HeroHiddenTrait.resolveAwakenedTrait(hero.hiddenTrait, hero.id)
      } else {
        hero.hiddenTrait.copy(unlocked = true)
      }
      hero.copy(hiddenTrait = nextTrait)
    } else {
      hero
    }
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
    require(profile.gold >= goldCost) { "Cần $goldCost Vàng để Trị Thương (+50% Max HP)." }
    val target = requireNotNull(roster.find { it.id == heroId && it.currentAssignment != HeroAssignment.SACRIFICED.name }) {
      "Không tìm thấy Anh hùng cần trị thương."
    }
    val data = HeroData.fromRecord(target, now)
    val healDelta = (data.maxHP / 2).coerceAtLeast(1)
    val nextHp = (data.currentHP + healDelta).coerceAtMost(data.maxHP).toLong()
    val nextProfile = profile.copy(gold = (profile.gold - goldCost).coerceAtLeast(0L))
    val nextRoster = roster.map { h ->
      if (h.id != heroId) h else {
        val appraised = checkAndUnlockHiddenTrait(h, now)
        appraised.copy(
          currentHp = nextHp,
          status = HeroStatusType.READY.name,
          injuryRecoveryUntilEpochSec = 0L,
          restStartedAtEpochSec = 0L,
          currentAssignment = if (h.inActiveSquad) HeroAssignment.IN_PARTY.name else HeroAssignment.UNASSIGNED.name,
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
    require(profile.gold >= goldCost) { "Cần $goldCost Vàng để dùng Bữa Ăn Thịnh Soạn (+40 Tinh thần & xóa Hoảng loạn)." }
    require(roster.any { it.id == heroId && it.currentAssignment != HeroAssignment.SACRIFICED.name }) {
      "Không tìm thấy Anh hùng dùng bữa."
    }
    val nextProfile = profile.copy(gold = (profile.gold - goldCost).coerceAtLeast(0L))
    val nextRoster = roster.map { h ->
      if (h.id != heroId) h else {
        val nextMorale = (h.morale + 40L).coerceIn(0L, 100L)
        val nextSanity = if (h.sanityState == HeroSanityState.SELF_AWARE.name) {
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
  ): List<HeroRecord> = roster.map { h ->
    if (h.id == lowStarHeroId && h.currentAssignment != HeroAssignment.SACRIFICED.name) {
      val nextMorale = (h.morale - penalty.toLong()).coerceIn(0L, 100L)
      val nextSanity = if (nextMorale < 25L && h.sanityState != HeroSanityState.SELF_AWARE.name) {
        HeroSanityState.PANICKED.name
      } else {
        h.sanityState
      }
      h.copy(morale = nextMorale, sanityState = nextSanity)
    } else {
      h
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
    val distinctSacrificed = sacrificedHeroIds.distinct().filter { it != targetHeroId }
    require(distinctSacrificed.size in 1..3) { "Hãy chọn từ 1 đến 3 Anh hùng Hiến Tế cho Lò Dung Hợp." }
    val activeRoster = roster.filter { it.currentAssignment != HeroAssignment.SACRIFICED.name }
    val target = requireNotNull(activeRoster.find { it.id == targetHeroId }) { "Không tìm thấy Anh hùng Chính." }
    val materials = distinctSacrificed.map { id ->
      requireNotNull(activeRoster.find { it.id == id }) { "Anh hùng hiến tế không tồn tại trong danh sách." }
    }
    require(activeRoster.size - materials.size >= 1) { "Phải giữ lại ít nhất 1 Anh hùng sống sót tại Thánh Địa." }

    val bonusAtk = materials.sumOf { (it.attack * 20L / 100L).coerceAtLeast(1L) }
    val bonusDef = materials.sumOf { (it.defense * 20L / 100L).coerceAtLeast(1L) }
    val bonusMaxHp = materials.sumOf { (it.maxHp * 20L / 100L).coerceAtLeast(5L) }
    val bonusExp = materials.sumOf { 65L * it.level.coerceAtLeast(1L) + 35L * it.rarity.coerceAtLeast(1L) }

    val totalExp = target.exp + bonusExp
    var nextLevel = target.level
    var remExp = totalExp
    while (nextLevel < 100L && remExp >= nextLevel * 100L) {
      remExp -= nextLevel * 100L
      nextLevel += 1L
    }

    val inheritedTraitCandidate = materials
      .map { HeroHiddenTrait.resolveAwakenedTrait(it.hiddenTrait, it.id) }
      .firstOrNull()
    val didInheritTrait = inheritedTraitCandidate != null && random.nextInt(100) < 20
    val nextHiddenTrait = if (didInheritTrait && inheritedTraitCandidate != null) {
      inheritedTraitCandidate.copy(unlocked = true)
    } else {
      target.hiddenTrait
    }

    val targetData = HeroData.fromRecord(target, now)
    val nextMaxHp = (target.maxHp + bonusMaxHp).coerceIn(50L, 100000L)
    val nextCurrentHp = (targetData.currentHP.toLong() + bonusMaxHp).coerceIn(1L, nextMaxHp)

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

    val materialSet = distinctSacrificed.toSet()
    var cowardAffected = 0
    val updatedRoster = activeRoster
      .filterNot { it.id in materialSet }
      .map { h ->
        val baseHero = if (h.id == targetHeroId) updatedTarget else h
        val isCoward = LobbyPersonality.fromRaw(baseHero.personality) == LobbyPersonality.COWARD
        if (isCoward) {
          cowardAffected++
          val penalizedMorale = (baseHero.morale - 10L).coerceIn(0L, 100L)
          val nextSanity = if (penalizedMorale < 25L && baseHero.sanityState != HeroSanityState.SELF_AWARE.name) {
            HeroSanityState.PANICKED.name
          } else {
            baseHero.sanityState
          }
          baseHero.copy(morale = penalizedMorale, sanityState = nextSanity)
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

