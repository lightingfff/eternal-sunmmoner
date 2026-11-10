package com.example

import com.example.data.LobbyHeroAssignments
import com.example.data.model.HeroAssignment
import com.example.data.model.HeroData
import com.example.data.model.HeroHiddenTrait
import com.example.data.model.HeroRecord
import com.example.data.model.HeroSanityState
import com.example.data.model.MasterProfile
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SanctuaryHouseRulesTest {
  private val now = 1_000L

  private fun hero(
    id: String,
    hp: Long = 100L,
    maxHp: Long = 200L,
    morale: Long = 60L,
    personality: String = "LOYAL",
  ) = HeroRecord(
    id = id,
    name = "Anh hùng $id",
    maxHp = maxHp,
    currentHp = hp,
    attack = 100L,
    defense = 80L,
    speed = 100L,
    morale = morale,
    personality = personality,
    hiddenTrait = HeroHiddenTrait(unlocked = false),
  )

  private fun seedForRoll(roll: Int): Int =
    (0..100_000).first { Random(it).nextInt(100) == roll }

  @Test fun fusionPreviewAndOutcomeReportOnlyEffectiveGainsAtStatAndExperienceCaps() {
    val target = hero("target", hp = 123, maxHp = 99_999).copy(
      attack = 49_999, defense = 49_998, level = 100, exp = 9_999_999,
    )
    val materials = listOf(hero("one"), hero("two"))
    val preview = LobbyHeroAssignments.synthesisBonuses(target, materials)
    val result = LobbyHeroAssignments.performCruelSynthesis(listOf(target) + materials, target.id,
      materials.map { it.id }, Random(seedForRoll(20)), now)
    assertEquals(1L, preview.attack); assertEquals(2L, preview.defense)
    assertEquals(1L, preview.maxHp); assertEquals(1L, preview.experience)
    assertEquals(preview.attack, result.bonusAtk); assertEquals(preview.defense, result.bonusDef)
    assertEquals(preview.maxHp, result.bonusMaxHp); assertEquals(preview.experience, result.bonusExp)
    assertEquals(50_000L, result.updatedTarget.attack); assertEquals(50_000L, result.updatedTarget.defense)
    assertEquals(100_000L, result.updatedTarget.maxHp); assertEquals(10_000_000L, result.updatedTarget.exp)
    assertEquals(123L, result.updatedTarget.currentHp)
  }

  @Test fun alreadyRevealedLegacyGenericTraitMigratesOnceAndPreservesCustomOrKnownIdentity() {
    val legacy = hero("legacy").copy(hiddenTrait = HeroHiddenTrait(unlocked = true))
    val migrated = LobbyHeroAssignments.checkAndUnlockHiddenTrait(legacy, now)
    assertTrue(HeroHiddenTrait.AWAKENED_TRAIT_POOL.any { it.first == migrated.hiddenTrait.id })
    assertEquals(migrated, LobbyHeroAssignments.checkAndUnlockHiddenTrait(migrated, now))
    val known = hero("known").copy(hiddenTrait = HeroHiddenTrait(id = "assassin_instinct", unlocked = true))
    val normalized = LobbyHeroAssignments.checkAndUnlockHiddenTrait(known, now)
    assertEquals("assassin_instinct", normalized.hiddenTrait.id)
    assertEquals(.30f, normalized.criticalHitChance, 0f)
    val custom = hero("custom").copy(hiddenTrait = HeroHiddenTrait(id = "custom", name = "Tài năng riêng", unlocked = true))
    assertEquals(custom.hiddenTrait, LobbyHeroAssignments.checkAndUnlockHiddenTrait(custom, now).hiddenTrait)
  }

  @Test fun restingDoesNotRestoreHpWhenTimePassesOrClockMovesBackwards() {
    val resting = hero("rest", hp = 14L).copy(
      status = "RESTING",
      currentAssignment = HeroAssignment.RESTING.name,
      restStartedAtEpochSec = now,
    )
    for (readTime in listOf(now - 500L, now, now + 60L, now + 120L, now + 86_400L)) {
      assertEquals("Resting must not provide free recovery at $readTime", 14, HeroData.fromRecord(resting, readTime).currentHP)
    }
  }

  @Test fun legacyUnspecifiedHpRetainsFullHpFallbackWithoutStartingATimer() {
    val legacy = hero("legacy").copy(currentHp = -1L)
    assertEquals(200, HeroData.fromRecord(legacy, now).currentHP)
    assertEquals(200, HeroData.fromRecord(legacy, now + 86_400L).currentHP)
  }

  @Test fun highLevelAndMoraleDoNotBypassLowRarityTowerAppraisal() {
    for (rarity in 1L..2L) {
      val inexperienced = hero("rare$rarity", morale = 100L).copy(
        rarity = rarity,
        level = 100L,
        hiddenTrait = HeroHiddenTrait(minimumLevel = 1, minimumMorale = 0, unlocked = false),
      )
      assertFalse(LobbyHeroAssignments.checkAndUnlockHiddenTrait(inexperienced, now).hiddenTrait.unlocked)
    }
  }

  @Test fun lowCurrentHpWithoutARecordedReturnDoesNotUnlockHiddenTrait() {
    val woundedAtHome = hero("unproven", hp = 1L).copy(
      survivedExpeditions = 0,
      returnedUnder15PctHp = false,
    )
    assertTrue(HeroData.fromRecord(woundedAtHome, now).isCriticalReturnHp)
    assertFalse(LobbyHeroAssignments.checkAndUnlockHiddenTrait(woundedAtHome, now).hiddenTrait.unlocked)
  }

  @Test fun twoSurvivedBattlesRemainLockedAndThreeUnlockWithoutErasingStoryText() {
    val story = "K".repeat(200)
    val twice = hero("veteran", hp = 200L).copy(
      rarity = 2L,
      survivedExpeditions = 2,
      bondSummary = story,
    )
    assertFalse(LobbyHeroAssignments.checkAndUnlockHiddenTrait(twice, now).hiddenTrait.unlocked)
    val awakened = LobbyHeroAssignments.checkAndUnlockHiddenTrait(twice.copy(survivedExpeditions = 3), now)
    assertTrue(awakened.hiddenTrait.unlocked)
    assertEquals(3, awakened.survivedExpeditions)
    assertEquals(story, awakened.bondSummary)
    assertNotEquals("???", awakened.hiddenTrait.displayName)
  }

  @Test fun provenCriticalReturnUnlocksEvenAfterHealingWithoutGrantingAnotherHeal() {
    val returned = hero("returned", hp = 140L).copy(
      survivedExpeditions = 1,
      returnedUnder15PctHp = true,
    )
    val appraised = LobbyHeroAssignments.checkAndUnlockHiddenTrait(returned, now)
    assertTrue(appraised.hiddenTrait.unlocked)
    assertTrue(appraised.returnedUnder15PctHp)
    assertEquals(140L, appraised.currentHp)
  }

  @Test fun existingLegacySurvivalTagStillUnlocksDuringMigration() {
    val legacy = hero("legacy-progress").copy(bondSummary = "Một ký ức cũ. [SURVIVED_EXP:3]")
    assertTrue(LobbyHeroAssignments.checkAndUnlockHiddenTrait(legacy, now).hiddenTrait.unlocked)
  }

  @Test fun partyAndHouseAssignmentsPreserveHpAndDoNotInventSurvivalProgress() {
    val wounded = hero("traveller", hp = 20L)
    val party = LobbyHeroAssignments.assign(listOf(wounded), wounded.id, HeroAssignment.IN_PARTY, now).single()
    assertEquals(20L, party.currentHp)
    assertEquals(0, party.survivedExpeditions)
    assertFalse(party.hiddenTrait.unlocked)
    val housed = LobbyHeroAssignments.assign(listOf(party), party.id, HeroAssignment.RESTING, now).single()
    assertEquals(20L, housed.currentHp)
    assertEquals(20, HeroData.fromRecord(housed, now + 3_600L).currentHP)
    assertFalse(housed.hiddenTrait.unlocked)
  }

  @Test fun treatmentDebitsEightyGoldAndRestoresExactlyHalfMaximumHp() {
    val wounded = hero("patient", hp = 30L)
    val (profile, roster) = LobbyHeroAssignments.healHeroAtInfirmary(
      MasterProfile(gold = 200L), listOf(wounded), wounded.id, now,
    )
    assertEquals(120L, profile.gold)
    assertEquals(130L, roster.single().currentHp)
    assertEquals(200L, roster.single().maxHp)
    assertEquals(wounded.morale, roster.single().morale)
    assertFalse(roster.single().hiddenTrait.unlocked)
  }

  @Test fun treatmentCapsHpAndRejectsInsufficientGoldOrAFullHealthHero() {
    val almostWell = hero("almost", hp = 180L)
    val (profile, roster) = LobbyHeroAssignments.healHeroAtInfirmary(
      MasterProfile(gold = 80L), listOf(almostWell), almostWell.id, now,
    )
    assertEquals(0L, profile.gold)
    assertEquals(200L, roster.single().currentHp)
    assertThrows(IllegalArgumentException::class.java) {
      LobbyHeroAssignments.healHeroAtInfirmary(MasterProfile(gold = 79L), listOf(almostWell), almostWell.id, now)
    }
    val full = almostWell.copy(currentHp = almostWell.maxHp)
    assertThrows(IllegalArgumentException::class.java) {
      LobbyHeroAssignments.healHeroAtInfirmary(MasterProfile(gold = 500L), listOf(full), full.id, now)
    }
    assertEquals(180L, almostWell.currentHp)
  }

  @Test fun treatmentRejectsClientSuppliedFreeNegativeOrDiscountedPrices() {
    val wounded = hero("price")
    for (price in listOf(-80L, -1L, 0L, 1L, 79L, 81L)) {
      assertThrows("Invalid treatment price $price", IllegalArgumentException::class.java) {
        LobbyHeroAssignments.healHeroAtInfirmary(
          MasterProfile(gold = 500L), listOf(wounded), wounded.id, now, goldCost = price,
        )
      }
    }
  }

  @Test fun mealDebitsSixtyFiveGoldAddsFortyMoraleAndClearsPanicWithoutHealing() {
    val panicked = hero("hungry", hp = 30L, morale = 20L).copy(sanityState = "PANICKED")
    val (profile, roster) = LobbyHeroAssignments.serveHeartyMealToHero(
      MasterProfile(gold = 200L), listOf(panicked), panicked.id, now,
    )
    assertEquals(135L, profile.gold)
    assertEquals(60L, roster.single().morale)
    assertEquals(HeroSanityState.NORMAL.name, roster.single().sanityState)
    assertEquals(30L, roster.single().currentHp)
    assertFalse(roster.single().hiddenTrait.unlocked)
  }

  @Test fun mealCapsMoraleAndPreservesSelfAwareness() {
    val aware = hero("aware", morale = 85L).copy(sanityState = "SELF_AWARE")
    val (profile, roster) = LobbyHeroAssignments.serveHeartyMealToHero(
      MasterProfile(gold = 65L), listOf(aware), aware.id, now,
    )
    assertEquals(0L, profile.gold)
    assertEquals(100L, roster.single().morale)
    assertEquals("SELF_AWARE", roster.single().sanityState)
  }

  @Test fun mealRejectsInsufficientGoldAndAlreadyNormalFullMorale() {
    val hungry = hero("hungry", morale = 20L)
    assertThrows(IllegalArgumentException::class.java) {
      LobbyHeroAssignments.serveHeartyMealToHero(MasterProfile(gold = 64L), listOf(hungry), hungry.id, now)
    }
    val satisfied = hungry.copy(morale = 100L, sanityState = "NORMAL")
    assertThrows(IllegalArgumentException::class.java) {
      LobbyHeroAssignments.serveHeartyMealToHero(MasterProfile(gold = 500L), listOf(satisfied), satisfied.id, now)
    }
  }

  @Test fun mealStillClearsPanicWhenMoraleIsAlreadyFull() {
    val irrational = hero("irrational", morale = 100L).copy(sanityState = "PANICKED")
    val (profile, roster) = LobbyHeroAssignments.serveHeartyMealToHero(
      MasterProfile(gold = 65L), listOf(irrational), irrational.id, now,
    )
    assertEquals(0L, profile.gold)
    assertEquals(100L, roster.single().morale)
    assertEquals("NORMAL", roster.single().sanityState)
  }

  @Test fun mealRejectsClientSuppliedFreeNegativeOrDiscountedPrices() {
    val hungry = hero("meal-price", morale = 20L)
    for (price in listOf(-65L, -1L, 0L, 1L, 64L, 66L)) {
      assertThrows("Invalid meal price $price", IllegalArgumentException::class.java) {
        LobbyHeroAssignments.serveHeartyMealToHero(
          MasterProfile(gold = 500L), listOf(hungry), hungry.id, now, goldCost = price,
        )
      }
    }
  }

  @Test fun paidActionsCannotModifyPermanentlySacrificedHeroes() {
    val dead = hero("dead", morale = 20L).copy(currentAssignment = "SACRIFICED")
    assertThrows(IllegalArgumentException::class.java) {
      LobbyHeroAssignments.healHeroAtInfirmary(MasterProfile(gold = 500L), listOf(dead), dead.id, now)
    }
    assertThrows(IllegalArgumentException::class.java) {
      LobbyHeroAssignments.serveHeartyMealToHero(MasterProfile(gold = 500L), listOf(dead), dead.id, now)
    }
  }

  @Test fun fusionRejectsEmptyDuplicateSelfAndOverCapacityMaterialSelections() {
    val roster = listOf("target", "m1", "m2", "m3", "m4").map { hero(it) }
    val invalidSelections = listOf(
      emptyList(),
      listOf("target"),
      listOf("target", "m1"),
      listOf("m1", "m1"),
      listOf("m1", "m2", "m3", "m4"),
    )
    for (selection in invalidSelections) {
      assertThrows("Invalid selection $selection", IllegalArgumentException::class.java) {
        LobbyHeroAssignments.performCruelSynthesis(roster, "target", selection, Random(1), now)
      }
    }
    assertEquals(5, roster.size)
  }

  @Test fun fusionRejectsMissingOrAlreadySacrificedTargetsAndMaterials() {
    val target = hero("target")
    val material = hero("m1")
    val dead = hero("dead").copy(currentAssignment = "SACRIFICED")
    val roster = listOf(target, material, dead)
    for (targetId in listOf("missing", "dead")) {
      assertThrows(IllegalArgumentException::class.java) {
        LobbyHeroAssignments.performCruelSynthesis(roster, targetId, listOf("m1"), Random(1), now)
      }
    }
    for (materialId in listOf("missing", "dead")) {
      assertThrows(IllegalArgumentException::class.java) {
        LobbyHeroAssignments.performCruelSynthesis(roster, "target", listOf(materialId), Random(1), now)
      }
    }
  }

  @Test fun fusionTransfersFlooredTwentyPercentOfSummedStatsAndDoesNotHealTarget() {
    val target = hero("target", hp = 80L)
    val first = hero("m1", hp = 51L, maxHp = 51L).copy(attack = 7L, defense = 2L)
    val second = hero("m2", hp = 54L, maxHp = 54L).copy(attack = 8L, defense = 3L)
    val outcome = LobbyHeroAssignments.performCruelSynthesis(
      listOf(target, first, second), target.id, listOf(first.id, second.id), Random(1), now,
    )
    assertEquals(3L, outcome.bonusAtk)
    assertEquals(1L, outcome.bonusDef)
    assertEquals(21L, outcome.bonusMaxHp)
    assertEquals(103L, outcome.updatedTarget.attack)
    assertEquals(81L, outcome.updatedTarget.defense)
    assertEquals(221L, outcome.updatedTarget.maxHp)
    assertEquals(80L, outcome.updatedTarget.currentHp)
    assertEquals(200L, outcome.bonusExp)
    assertEquals(2L, outcome.updatedTarget.level)
    assertEquals(100L, outcome.updatedTarget.exp)
    assertEquals(listOf("target"), outcome.updatedRoster.map { it.id })
  }

  @Test fun allThreeMaterialsArePermanentlyDeletedAndCannotBeReused() {
    val roster = listOf("target", "m1", "m2", "m3", "witness").map { hero(it) }
    val result = LobbyHeroAssignments.performCruelSynthesis(
      roster, "target", listOf("m1", "m2", "m3"), Random(1), now,
    )
    assertEquals(setOf("target", "witness"), result.updatedRoster.map { it.id }.toSet())
    assertEquals(3, result.sacrificedNames.size)
    assertThrows(IllegalArgumentException::class.java) {
      LobbyHeroAssignments.performCruelSynthesis(result.updatedRoster, "target", listOf("m1"), Random(1), now)
    }
    assertEquals(5, roster.size)
  }

  @Test fun fusionPenalizesEveryLivingCowardOnceAndClampsMoraleAtZero() {
    val target = hero("target", morale = 8L, personality = "COWARD")
    val material = hero("material", personality = "COWARD")
    val restingCoward = hero("resting", morale = 34L, personality = "COWARD").copy(currentAssignment = "RESTING")
    val loyal = hero("loyal", morale = 20L)
    val dead = hero("dead", morale = 50L, personality = "COWARD").copy(currentAssignment = "SACRIFICED")
    val outcome = LobbyHeroAssignments.performCruelSynthesis(
      listOf(target, material, restingCoward, loyal, dead), target.id, listOf(material.id), Random(seedForRoll(20)), now,
    )
    assertEquals(2, outcome.cowardHeroesAffected)
    val byId = outcome.updatedRoster.associateBy { it.id }
    assertEquals(0L, byId.getValue("target").morale)
    assertEquals("PANICKED", byId.getValue("target").sanityState)
    assertEquals(24L, byId.getValue("resting").morale)
    assertEquals("PANICKED", byId.getValue("resting").sanityState)
    assertEquals(loyal, byId.getValue("loyal"))
    assertFalse(byId.containsKey("material"))
    assertFalse(byId.containsKey("dead"))
  }

  @Test fun selfAwareCowardKeepsSelfAwarenessAfterPsychologicalPenalty() {
    val aware = hero("aware", morale = 12L, personality = "COWARD").copy(sanityState = "SELF_AWARE")
    val material = hero("material")
    val outcome = LobbyHeroAssignments.performCruelSynthesis(
      listOf(aware, material), aware.id, listOf(material.id), Random(1), now,
    )
    assertEquals(2L, outcome.updatedTarget.morale)
    assertEquals("SELF_AWARE", outcome.updatedTarget.sanityState)
  }

  @Test fun traitInheritanceUsesStrictTwentyPercentBoundaryAndIsRepeatableForSameRoll() {
    val target = hero("target")
    val material = hero("material").copy(hiddenTrait = HeroHiddenTrait(
      id = "assassin_instinct",
      name = "Bản Năng Sát Thủ - Tăng 30% Crit",
      unlocked = true,
    ))
    val roster = listOf(target, material)
    val successSeed = seedForRoll(19)
    val inherited = LobbyHeroAssignments.performCruelSynthesis(roster, target.id, listOf(material.id), Random(successSeed), now)
    assertNotNull(inherited.inheritedTraitName)
    assertEquals(material.hiddenTrait.id, inherited.updatedTarget.hiddenTrait.id)
    assertTrue(inherited.updatedTarget.hiddenTrait.unlocked)
    val repeated = LobbyHeroAssignments.performCruelSynthesis(roster, target.id, listOf(material.id), Random(successSeed), now)
    assertEquals(inherited, repeated)
    val rejected = LobbyHeroAssignments.performCruelSynthesis(roster, target.id, listOf(material.id), Random(seedForRoll(20)), now)
    assertNull(rejected.inheritedTraitName)
    assertEquals(target.hiddenTrait, rejected.updatedTarget.hiddenTrait)
  }

  @Test fun unlockedAttackDefenseAndSpeedTraitsAffectEffectiveStatsOnly() {
    val baseline = hero("baseline", morale = 60L)
    val sword = baseline.copy(hiddenTrait = HeroHiddenTrait(id = "mortal_sword_saint", unlocked = true))
    val armor = baseline.copy(hiddenTrait = HeroHiddenTrait(id = "bloodline_bastion", unlocked = true))
    val sight = baseline.copy(hiddenTrait = HeroHiddenTrait(id = "astral_eye", unlocked = true))
    assertEquals(100, baseline.totalAttack(emptyList()))
    assertEquals(125, sword.totalAttack(emptyList()))
    assertEquals(100, armor.totalDefense(emptyList()))
    assertEquals(120, sight.totalSpeed(emptyList()))
    assertEquals(100L, sword.attack)
    assertEquals(80L, armor.defense)
    assertEquals(100L, sight.speed)
    assertEquals(125, sword.totalAttack(emptyList()))
    assertEquals(baseline.totalAttack(emptyList()), sword.copy(hiddenTrait = sword.hiddenTrait.copy(unlocked = false)).totalAttack(emptyList()))
    assertEquals(baseline.totalDefense(emptyList()), armor.copy(hiddenTrait = armor.hiddenTrait.copy(unlocked = false)).totalDefense(emptyList()))
    assertEquals(baseline.totalSpeed(emptyList()), sight.copy(hiddenTrait = sight.hiddenTrait.copy(unlocked = false)).totalSpeed(emptyList()))
  }

  @Test fun unlockedIndomitableTraitPreventsSocialPanicWhileRetainingMoralePenalty() {
    val brave = hero("brave", morale = 25L).copy(hiddenTrait = HeroHiddenTrait(id = "indomitable_will", unlocked = true))
    val penalized = LobbyHeroAssignments.applyProximityDisdainMoralePenalty(listOf(brave), brave.id).single()
    assertEquals(23L, penalized.morale)
    assertNotEquals("PANICKED", penalized.sanityState)
    val locked = brave.copy(hiddenTrait = brave.hiddenTrait.copy(unlocked = false))
    val frightened = LobbyHeroAssignments.applyProximityDisdainMoralePenalty(listOf(locked), locked.id).single()
    assertEquals("PANICKED", frightened.sanityState)
  }

  @Test fun assassinCritChanceRequiresAnUnlockedMatchingTrait() {
    val assassin = hero("assassin").copy(hiddenTrait = HeroHiddenTrait(id = "assassin_instinct", unlocked = true))
    assertTrue(assassin.hasActiveHiddenTrait("assassin_instinct"))
    assertFalse(assassin.hasActiveHiddenTrait("indomitable_will"))
    assertEquals(0.30f, assassin.criticalHitChance, 0f)
    val locked = assassin.copy(hiddenTrait = assassin.hiddenTrait.copy(unlocked = false))
    assertFalse(locked.hasActiveHiddenTrait("assassin_instinct"))
    assertEquals(0f, locked.criticalHitChance, 0f)
  }

  @Test fun highRarityAppraisalIsImmediateAndReturnsARealTrait() {
    val highRarity = hero("rare").copy(rarity = 3L)
    val appraised = LobbyHeroAssignments.checkAndUnlockHiddenTrait(highRarity, now)
    assertTrue(appraised.hiddenTrait.unlocked)
    assertTrue(HeroHiddenTrait.AWAKENED_TRAIT_POOL.any { it.first == appraised.hiddenTrait.id })
    assertEquals(0, appraised.survivedExpeditions)
    assertFalse(appraised.returnedUnder15PctHp)
  }

  @Test fun existingActiveKnownTraitKeepsItsIdWhenResolvedFromALegacyDisplayName() {
    val existing = HeroHiddenTrait(id = "assassin_instinct", name = "Linh hồn bất khuất", unlocked = false)
    val resolved = HeroHiddenTrait.resolveAwakenedTrait(existing, "a-different-hash")
    assertEquals("assassin_instinct", resolved.id)
    assertEquals("Bản Năng Sát Thủ - Tăng 30% Crit", resolved.name)
  }

  @Test fun legacyProgressTagsRetainRoomInsideLongStoryAndDoNotOverflow() {
    val summary = LobbyHeroAssignments.recordExpeditionSurvival("K".repeat(200), true, true)
    assertTrue(summary.length <= 200)
    assertEquals(1, LobbyHeroAssignments.survivedExpeditionCount(summary))
    assertTrue(LobbyHeroAssignments.hasReturnedUnder15PctHp(summary))
    val saturated = LobbyHeroAssignments.recordExpeditionSurvival("[SURVIVED_EXP:2147483647]", true, false)
    assertEquals(Int.MAX_VALUE, LobbyHeroAssignments.survivedExpeditionCount(saturated))
    val didNotSurvive = LobbyHeroAssignments.recordExpeditionSurvival("", false, true)
    assertEquals(0, LobbyHeroAssignments.survivedExpeditionCount(didNotSurvive))
    assertFalse(LobbyHeroAssignments.hasReturnedUnder15PctHp(didNotSurvive))
  }

  @Test fun socialPenaltyRejectsInvalidAmountsAndHighRarityOrMissingVictims() {
    val lowStar = hero("low")
    for (penalty in listOf(-2, -1, 0, 1, 3, 100)) {
      assertThrows(IllegalArgumentException::class.java) {
        LobbyHeroAssignments.applyProximityDisdainMoralePenalty(listOf(lowStar), lowStar.id, penalty)
      }
    }
    for (victim in listOf(lowStar.copy(rarity = 3L), lowStar.copy(currentAssignment = "SACRIFICED"))) {
      assertThrows(IllegalArgumentException::class.java) {
        LobbyHeroAssignments.applyProximityDisdainMoralePenalty(listOf(victim), victim.id)
      }
    }
    assertThrows(IllegalArgumentException::class.java) {
      LobbyHeroAssignments.applyProximityDisdainMoralePenalty(listOf(lowStar), "missing")
    }
  }

  @Test fun partialTreatmentKeepsActiveInjuryUntilFullyHealed() {
    val injured = hero("injured", hp = 10L).copy(
      status = "INJURED", injuryRecoveryUntilEpochSec = now + 1_000L,
      currentAssignment = "RESTING", restStartedAtEpochSec = now - 50L,
    )
    val (remaining, healed) = LobbyHeroAssignments.healHeroAtInfirmary(
      MasterProfile(gold = 160L), listOf(injured), injured.id, now,
    )
    assertEquals(110L, healed.single().currentHp)
    assertEquals("INJURED", healed.single().status)
    assertEquals("RESTING", healed.single().currentAssignment)
    assertEquals(injured.injuryRecoveryUntilEpochSec, healed.single().injuryRecoveryUntilEpochSec)
    val (paid, fullyHealed) = LobbyHeroAssignments.healHeroAtInfirmary(remaining, healed, injured.id, now)
    assertEquals(0L, paid.gold)
    assertEquals(200L, fullyHealed.single().currentHp)
    assertEquals("READY", fullyHealed.single().status)
    assertEquals(0L, fullyHealed.single().injuryRecoveryUntilEpochSec)
  }

  @Test fun fullHpInjuredHeroPaysForExplicitInjuryClearAndKeepsExistingPartyMembership() {
    for (inParty in listOf(false, true)) {
      val injured = hero("full-injured", hp = 200L).copy(
        status = "INJURED",
        injuryRecoveryUntilEpochSec = now + 1_000L,
        currentAssignment = "RESTING",
        restStartedAtEpochSec = now - 50L,
        inActiveSquad = inParty,
        partyJoinedOrder = if (inParty) 12L else 0L,
      )
      val (paid, roster) = LobbyHeroAssignments.healHeroAtInfirmary(
        MasterProfile(gold = 80L), listOf(injured), injured.id, now,
      )
      val healed = roster.single()
      assertEquals(0L, paid.gold)
      assertEquals(200L, healed.currentHp)
      assertEquals("READY", healed.status)
      assertEquals(0L, healed.injuryRecoveryUntilEpochSec)
      assertEquals(0L, healed.restStartedAtEpochSec)
      assertEquals(inParty, healed.inActiveSquad)
      assertEquals(injured.partyJoinedOrder, healed.partyJoinedOrder)
      assertEquals(if (inParty) "IN_PARTY" else "UNASSIGNED", healed.currentAssignment)
      assertThrows(IllegalArgumentException::class.java) {
        LobbyHeroAssignments.healHeroAtInfirmary(MasterProfile(gold = 80L), roster, healed.id, now)
      }
    }
  }
}
