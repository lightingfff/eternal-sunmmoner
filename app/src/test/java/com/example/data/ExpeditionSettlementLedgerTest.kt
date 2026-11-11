package com.example.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.data.model.CourierOutcome
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExpeditionSettlementLedgerTest {
  private lateinit var context: Context
  @Before fun isolateSave() {
    val base = ApplicationProvider.getApplicationContext<Context>()
    val dir = Files.createTempDirectory(base.cacheDir.toPath(), "settlement-ledger-").toFile()
    context = object : ContextWrapper(base) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = dir
    }
  }

  @Test fun duplicateAndRestartReplayFullResultWithoutChangingAnyGameState() = runTest {
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    val source = LocalGuestDataSource(manager)
    val id = UUID.randomUUID().toString()
    val result = source.settleExpedition(initial.profile, initial.heroes, emptySet(), 1, true,
      courierOutcome = CourierOutcome.SUPPLIES_RECOVERED, bonusGold = 13L,
      remainingHeroHpRatios = initial.heroes.associate { it.id to .12f }, attemptId = id)
    val after = manager.guestSnapshot.value!!
    val bytes = manager.primaryFileForTest().readBytes()
    assertEquals(result, source.settleExpedition(initial.profile, emptyList(), emptySet(), 1, false, attemptId = id))
    assertEquals(after, manager.guestSnapshot.value)
    assertArrayEquals(bytes, manager.primaryFileForTest().readBytes())
    val restarted = LocalGuestSaveManager(context)
    assertEquals(result, LocalGuestDataSource(restarted).settleExpedition(
      initial.profile, initial.heroes, emptySet(), 1, true, attemptId = id))
    assertEquals(after, restarted.guestSnapshot.value)
    assertEquals(1L, after.profile.totalExpeditions)
    assertEquals(result, after.settledExpeditions[id])
    assertEquals(result, ExpeditionSettlementCodec.decode(ExpeditionSettlementCodec.encode(result)))
    assertTrue(after.heroes.all { it.returnedUnder15PctHp && it.currentHp > 0L })
  }

  @Test fun failedAtomicWriteDoesNotReserveAttemptAndRetryCreditsOnce() = runTest {
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    val bytes = manager.primaryFileForTest().readBytes()
    val id = UUID.randomUUID().toString()
    val blocked = File(manager.primaryFileForTest().parentFile, "guest_save_v2.tmp")
    assertTrue(blocked.mkdir())
    val source = LocalGuestDataSource(manager)
    try {
      source.settleExpedition(initial.profile, initial.heroes, emptySet(), 1, true, attemptId = id)
      fail("Failed disk writes must propagate")
    } catch (_: IOException) {
      assertEquals(initial, manager.guestSnapshot.value)
      assertArrayEquals(bytes, manager.primaryFileForTest().readBytes())
    }
    assertTrue(blocked.delete())
    val result = source.settleExpedition(initial.profile, initial.heroes, emptySet(), 1, true, attemptId = id)
    val after = manager.guestSnapshot.value!!
    assertEquals(1L, after.profile.totalExpeditions)
    assertEquals(result, LocalGuestDataSource(LocalGuestSaveManager(context)).settleExpedition(
      initial.profile, initial.heroes, emptySet(), 1, true, attemptId = id))
    assertEquals(after.profile.gold, LocalGuestSaveManager(context).guestSnapshot.value!!.profile.gold)
  }

  @Test fun newAttemptEarnsNormalRewardsButEachBranchBonusOnlyOnce() = runTest {
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    val source = LocalGuestDataSource(manager)
    suspend fun play(outcome: CourierOutcome) = source.settleExpedition(
      initial.profile, initial.heroes, emptySet(), 1, true, courierOutcome = outcome,
      attemptId = UUID.randomUUID().toString())
    val support = play(CourierOutcome.RESCUED)
    val supportAgain = play(CourierOutcome.RESCUED)
    assertEquals(support.manaEarned - 20L, supportAgain.manaEarned)
    val supplies = play(CourierOutcome.SUPPLIES_RECOVERED)
    val suppliesAgain = play(CourierOutcome.SUPPLIES_RECOVERED)
    assertEquals(supplies.goldEarned - 60L, suppliesAgain.goldEarned)
    val partial = play(CourierOutcome.RESCUED_WITHOUT_SUPPLIES)
    assertEquals(suppliesAgain.goldEarned, partial.goldEarned)
    assertEquals(supportAgain.manaEarned, partial.manaEarned)
    val restored = LocalGuestSaveManager(context).guestSnapshot.value!!
    assertEquals(5L, restored.profile.totalExpeditions)
    assertTrue(restored.profile.storyFlags.containsAll(listOf(LostCourierProgression.FLAG_SUPPORTED,
      LostCourierProgression.FLAG_SUPPLIES_RECOVERED, LostCourierProgression.FLAG_SUPPLIES_MISSED)))
    assertEquals(1, restored.heroes.count { LostCourierProgression.hasRescueMemory(it.bondSummary) })
  }

  @Test fun ledgerRetainsLast32ReceiptsAcrossRestart() = runTest {
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    val source = LocalGuestDataSource(manager)
    val ids = (0 until 34).map { UUID.randomUUID().toString() }
    for (id in ids) source.settleExpedition(initial.profile, emptyList(), emptySet(), 1, false, attemptId = id)
    val restored = LocalGuestSaveManager(context).guestSnapshot.value!!
    assertEquals(32, restored.settledExpeditions.size)
    assertEquals(ids.takeLast(32).toSet(), restored.settledExpeditions.keys)
    val last = restored.settledExpeditions.getValue(ids.last())
    assertEquals(last, source.settleExpedition(initial.profile, emptyList(), emptySet(), 1, false, attemptId = ids.last()))
    assertEquals(34L, manager.guestSnapshot.value!!.profile.totalExpeditions)
    // The local contract is bounded, not indefinite exactly-once processing.
    assertFalse(ids.first() in restored.settledExpeditions)
    val reopened = LocalGuestSaveManager(context)
    LocalGuestDataSource(reopened).settleExpedition(initial.profile, emptyList(), emptySet(), 1, false, attemptId = ids.first())
    assertEquals(35L, reopened.guestSnapshot.value!!.profile.totalExpeditions)
    assertTrue(ids.first() in reopened.guestSnapshot.value!!.settledExpeditions)
    assertEquals(32, reopened.guestSnapshot.value!!.settledExpeditions.size)
  }

  @Test fun invalidAttemptPathsFailBeforeAnyMutation() = runTest {
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    for (id in listOf("../other", "abc", "00000000-0000-0000-0000-000000000000", UUID.randomUUID().toString().uppercase())) {
      try {
        LocalGuestDataSource(manager).settleExpedition(initial.profile, initial.heroes, emptySet(), 1, true, attemptId = id)
        fail("Invalid UUID accepted: $id")
      } catch (_: IllegalArgumentException) { assertEquals(initial, manager.guestSnapshot.value) }
    }
  }

  @Test fun legacyRescueDoesNotRegrantSupportBonusAndFailureNeverGetsBranchRewards() {
    assertEquals(0L, LostCourierProgression.bonusMana(listOf(LostCourierProgression.FLAG_RESCUED), CourierOutcome.RESCUED))
    assertEquals(0L, LostCourierProgression.bonusGold(emptyList(), CourierOutcome.FAILED))
    assertEquals(0L, LostCourierProgression.bonusMana(emptyList(), CourierOutcome.RESCUED_WITHOUT_SUPPLIES))
    val supplied = LostCourierProgression.outcomeFlags(emptyList(), CourierOutcome.SUPPLIES_RECOVERED)
    assertEquals(20L, LostCourierProgression.bonusMana(supplied, CourierOutcome.RESCUED))
    val supported = LostCourierProgression.outcomeFlags(supplied, CourierOutcome.RESCUED)
    assertEquals(0L, LostCourierProgression.bonusGold(supported, CourierOutcome.SUPPLIES_RECOVERED))
    assertEquals(0L, LostCourierProgression.bonusMana(supported, CourierOutcome.RESCUED))
  }

  @Test fun journeyChoicesKeepActualAndLegacyPrologueExpansionsCourierFlagsAndMealWithinCap() {
    val prologue = com.example.game.story.ManhwaStoryData.FLAG_PROLOGUE_SEEN
    val expansions = com.example.data.model.IslandExpansionMilestone.entries.map { it.flagKey }
    val permanentCourier = listOf(LostCourierProgression.FLAG_RESCUED, LostCourierProgression.FLAG_FAILED,
      LostCourierProgression.FLAG_SUPPORTED, LostCourierProgression.FLAG_SUPPLIES_RECOVERED,
      LostCourierProgression.FLAG_SUPPLIES_MISSED, LostCourierProgression.FLAG_ALARM_SILENCED,
      LostCourierProgression.FLAG_SCOUT_RESCUED, LostCourierProgression.FLAG_JOURNEY_COMPLETE)
    val meal = "MEAL_${com.example.data.model.SanctuaryMealRecipe.ROAST_BEAST_PLATTER.code}_9999999999"
    val chapters = com.example.game.combat.RescueJourneyContent.chapters.map { "DISC_m01b_${it.id}" }
    val choices = listOf("DISC_m01b_alarm_silenced", "DISC_m01b_spare_bandages",
      "DISC_m01b_medicine_courier", "DISC_m01b_medicine_squad")
    val raw = listOf(prologue, "PROLOGUE_SEEN") + expansions + permanentCourier + meal +
      (1..30).map { "DISC_previous_$it" } + chapters + choices
    val compact = SanctuaryProgressionHelper.compactStoryFlags(raw)
    assertEquals(20, compact.size)
    assertTrue(compact.containsAll(listOf(prologue, "PROLOGUE_SEEN", meal) + expansions + permanentCourier))
    assertEquals(compact, SanctuaryProgressionHelper.compactStoryFlags(compact))
    assertTrue(FeastBuffHelper.isFeastBuffActive(compact, nowEpochSec = 1000L))
  }

}
