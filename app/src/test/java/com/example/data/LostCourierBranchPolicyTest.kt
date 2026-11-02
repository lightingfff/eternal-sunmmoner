package com.example.data

import com.example.data.model.CourierOutcome
import org.junit.Assert.*
import org.junit.Test

class LostCourierBranchPolicyTest {
  @Test fun missedSuppliesSaveIsARescueWithoutCompletingEitherRewardBranch() {
    val missed = CourierOutcome.RESCUED_WITHOUT_SUPPLIES
    assertTrue(LostCourierProgression.isRescue(missed))
    assertTrue(LostCourierProgression.isFirstRescue(emptyList(), missed))
    assertEquals(0L, LostCourierProgression.bonusGold(emptyList(), missed))
    assertEquals(0L, LostCourierProgression.bonusMana(emptyList(), missed))
    val flags = LostCourierProgression.outcomeFlags(emptyList(), missed)
    assertTrue(LostCourierProgression.FLAG_SUPPLIES_MISSED in flags)
    assertTrue(LostCourierProgression.FLAG_RESCUED in flags)
    assertFalse(LostCourierProgression.FLAG_SUPPORTED in flags)
    assertFalse(LostCourierProgression.FLAG_SUPPLIES_RECOVERED in flags)
    val replay = LostCourierProgression.outcomeFlags(flags, missed)
    assertEquals(flags, replay)
    assertEquals(0L, LostCourierProgression.bonusMana(replay, missed))
    assertEquals(20L, LostCourierProgression.bonusMana(replay, CourierOutcome.RESCUED))
    val supported = LostCourierProgression.outcomeFlags(replay, CourierOutcome.RESCUED)
    assertEquals(0L, LostCourierProgression.bonusMana(supported, CourierOutcome.RESCUED))
    assertEquals(60L, LostCourierProgression.bonusGold(supported, CourierOutcome.SUPPLIES_RECOVERED))
    val compacted = SanctuaryProgressionHelper.compactStoryFlags(flags + (1..40).map { "DISC_$it" })
    assertTrue(compacted.containsAll(flags))
  }

  @Test fun eachSuccessHasItsOwnPermanentConsequenceAndOneTimeReward() {
    val supplies = CourierOutcome.SUPPLIES_RECOVERED
    assertTrue(LostCourierProgression.isFirstRescue(emptyList(), supplies))
    assertEquals(60L, LostCourierProgression.bonusGold(emptyList(), supplies))
    val supplyFlags = LostCourierProgression.outcomeFlags(emptyList(), supplies)
    assertTrue(LostCourierProgression.FLAG_RESCUED in supplyFlags)
    assertTrue(LostCourierProgression.FLAG_SUPPLIES_RECOVERED in supplyFlags)
    assertFalse(LostCourierProgression.FLAG_SUPPORTED in supplyFlags)
    assertEquals(90L, LostCourierProgression.synthesisGoldCost(supplyFlags))
    assertEquals(0L, LostCourierProgression.bonusGold(supplyFlags, supplies))
    assertEquals(20L, LostCourierProgression.bonusMana(supplyFlags, CourierOutcome.RESCUED))
    val both = LostCourierProgression.outcomeFlags(supplyFlags, CourierOutcome.RESCUED)
    assertFalse(LostCourierProgression.isFirstRescue(both, CourierOutcome.RESCUED))
    assertEquals(0L, LostCourierProgression.bonusMana(both, CourierOutcome.RESCUED))
    val compacted = SanctuaryProgressionHelper.compactStoryFlags(both + (1..40).map { "DISC_$it" })
    assertTrue(compacted.containsAll(both))
    assertEquals(20, compacted.size)
  }

  @Test fun oldRescueSavesDoNotReceiveSupportRewardAgainAfterAnotherBranch() {
    val legacy = listOf(LostCourierProgression.FLAG_RESCUED)
    assertEquals(0L, LostCourierProgression.bonusMana(legacy, CourierOutcome.RESCUED))
    val migrated = LostCourierProgression.outcomeFlags(legacy, CourierOutcome.SUPPLIES_RECOVERED)
    assertTrue(LostCourierProgression.FLAG_SUPPORTED in migrated)
    assertEquals(0L, LostCourierProgression.bonusMana(migrated, CourierOutcome.RESCUED))
    assertEquals(0L, LostCourierProgression.bonusGold(migrated, CourierOutcome.SUPPLIES_RECOVERED))
  }

  @Test fun failureAndOrdinaryExpeditionsCannotGrantBranchRewardsOrEraseSuccess() {
    val rescued = LostCourierProgression.outcomeFlags(emptyList(), CourierOutcome.RESCUED)
    val failed = LostCourierProgression.outcomeFlags(rescued, CourierOutcome.FAILED)
    assertTrue(failed.containsAll(rescued))
    assertTrue(LostCourierProgression.FLAG_FAILED in failed)
    assertFalse(LostCourierProgression.isRescue(CourierOutcome.FAILED))
    assertFalse(LostCourierProgression.isRescue(null))
    for (outcome in listOf(null, CourierOutcome.FAILED)) {
      assertEquals(0L, LostCourierProgression.bonusGold(emptyList(), outcome))
      assertEquals(0L, LostCourierProgression.bonusMana(emptyList(), outcome))
    }
    assertEquals(failed, LostCourierProgression.outcomeFlags(failed, null))
  }
}
