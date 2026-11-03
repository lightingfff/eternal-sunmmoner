package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.data.model.CourierOutcome
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LostCourierPersistenceRobolectricTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    File(context.filesDir, "eternal_summoner_saves").deleteRecursively()
  }

  @Test
  fun `missed supplies survive restart without support reward until actual support completion`() = runTest {
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession("Missed Supplies Master")
    val source = LocalGuestDataSource(manager)
    val missed = source.settleExpedition(initial.profile, initial.heroes, emptySet(), 1, true,
      courierOutcome = CourierOutcome.RESCUED_WITHOUT_SUPPLIES)
    assertEquals(CourierOutcome.RESCUED_WITHOUT_SUPPLIES, missed.courierOutcome)
    assertTrue(missed.courierForgeUnlocked)
    val restarted = LocalGuestSaveManager(context)
    val flags = restarted.guestSnapshot.value!!.profile.storyFlags
    assertTrue(LostCourierProgression.FLAG_RESCUED in flags)
    assertTrue(LostCourierProgression.FLAG_SUPPLIES_MISSED in flags)
    assertFalse(LostCourierProgression.FLAG_SUPPORTED in flags)
    assertFalse(LostCourierProgression.FLAG_SUPPLIES_RECOVERED in flags)
    val resumed = LocalGuestDataSource(restarted)
    val replay = resumed.settleExpedition(initial.profile, initial.heroes, emptySet(), 1, true,
      courierOutcome = CourierOutcome.RESCUED_WITHOUT_SUPPLIES)
    assertEquals(missed.manaEarned, replay.manaEarned)
    assertEquals(missed.goldEarned, replay.goldEarned)
    assertFalse(replay.courierForgeUnlocked)
    val support = resumed.settleExpedition(initial.profile, initial.heroes, emptySet(), 1, true,
      courierOutcome = CourierOutcome.RESCUED)
    assertEquals(20L, support.manaEarned - replay.manaEarned)
    assertTrue(LostCourierProgression.FLAG_SUPPORTED in LocalGuestSaveManager(context).guestSnapshot.value!!.profile.storyFlags)
    assertEquals(1, LocalGuestSaveManager(context).guestSnapshot.value!!.heroes.count {
      LostCourierProgression.hasRescueMemory(it.bondSummary)
    })
  }

  @Test
  fun `both rescue choices survive restart and stale replays cannot repeat branch bonuses`() = runTest {
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession("Branch Master")
    val source = LocalGuestDataSource(manager)
    val supplies = source.settleExpedition(initial.profile, initial.heroes, emptySet(), 1, true,
      courierOutcome = CourierOutcome.SUPPLIES_RECOVERED)
    assertEquals(CourierOutcome.SUPPLIES_RECOVERED, supplies.courierOutcome)
    assertTrue(supplies.courierForgeUnlocked)
    val restarted = LocalGuestSaveManager(context)
    assertTrue(LostCourierProgression.FLAG_SUPPLIES_RECOVERED in restarted.guestSnapshot.value!!.profile.storyFlags)
    val restartedSource = LocalGuestDataSource(restarted)
    val supplyReplay = restartedSource.settleExpedition(initial.profile, initial.heroes, emptySet(), 1, true,
      courierOutcome = CourierOutcome.SUPPLIES_RECOVERED)
    assertFalse(supplyReplay.courierForgeUnlocked)
    assertEquals(60L, supplies.goldEarned - supplyReplay.goldEarned)
    val support = restartedSource.settleExpedition(initial.profile, initial.heroes, emptySet(), 1, true,
      courierOutcome = CourierOutcome.RESCUED)
    val supportReplay = restartedSource.settleExpedition(initial.profile, initial.heroes, emptySet(), 1, true,
      courierOutcome = CourierOutcome.RESCUED)
    assertEquals(20L, support.manaEarned - supportReplay.manaEarned)
    val final = LocalGuestSaveManager(context).guestSnapshot.value!!
    assertTrue(final.profile.storyFlags.containsAll(listOf(LostCourierProgression.FLAG_RESCUED,
      LostCourierProgression.FLAG_SUPPORTED, LostCourierProgression.FLAG_SUPPLIES_RECOVERED)))
    assertEquals(initial.profile.gold + supplies.goldEarned + supplyReplay.goldEarned +
      support.goldEarned + supportReplay.goldEarned, final.profile.gold)
    assertEquals(1, final.heroes.count { LostCourierProgression.hasRescueMemory(it.bondSummary) })
  }

  @Test
  fun `rescue unlock and one hero memory survive restart and stale-profile replay`() = runTest {
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession("Courier Master")
    val source = LocalGuestDataSource(manager)
    val memoryHero = initial.heroes.last()
    val first = source.settleExpedition(
      initial.profile, initial.heroes, emptySet(), 1, true,
      courierOutcome = CourierOutcome.RESCUED, courierMemoryHeroId = memoryHero.id,
    )
    assertTrue(first.courierForgeUnlocked)
    assertEquals(memoryHero.name, first.courierMemoryHeroName)
    val replay = source.settleExpedition(
      initial.profile, initial.heroes, emptySet(), 1, true,
      courierOutcome = CourierOutcome.RESCUED, courierMemoryHeroId = initial.heroes.first().id,
      discoveredNodeIds = (1..40).map { "node_$it" }.toSet(),
    )
    assertFalse(replay.courierForgeUnlocked)
    val restored = LocalGuestSaveManager(context).guestSnapshot.value!!
    assertEquals(initial.guestPlayerId, restored.guestPlayerId)
    assertEquals(GuestSaveSnapshot.CURRENT_SAVE_SCHEMA_VERSION, restored.schemaVersion)
    assertTrue(LostCourierProgression.FLAG_RESCUED in restored.profile.storyFlags)
    assertTrue(restored.profile.storyFlags.size <= 20)
    assertEquals(1, restored.heroes.count { LostCourierProgression.hasRescueMemory(it.bondSummary) })
    assertTrue(LostCourierProgression.hasRescueMemory(restored.heroes.first { it.id == memoryHero.id }.bondSummary))
  }

  @Test
  fun `failed escort is saved and a later rescue remains available`() = runTest {
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    val source = LocalGuestDataSource(manager)
    val failed = source.settleExpedition(
      initial.profile, initial.heroes, emptySet(), 1, false, retreatUsed = true,
      courierOutcome = CourierOutcome.RESCUED,
    )
    assertEquals(CourierOutcome.FAILED, failed.courierOutcome)
    assertFalse(failed.courierForgeUnlocked)
    val afterFailure = LocalGuestSaveManager(context).guestSnapshot.value!!
    assertTrue(LostCourierProgression.FLAG_FAILED in afterFailure.profile.storyFlags)
    assertFalse(LostCourierProgression.FLAG_RESCUED in afterFailure.profile.storyFlags)
    assertFalse(afterFailure.heroes.any { LostCourierProgression.hasRescueMemory(it.bondSummary) })
    val rescued = source.settleExpedition(
      initial.profile, initial.heroes, emptySet(), 1, true, courierOutcome = CourierOutcome.RESCUED,
    )
    assertTrue(rescued.courierForgeUnlocked)
    val afterRetry = LocalGuestSaveManager(context).guestSnapshot.value!!
    assertTrue(LostCourierProgression.FLAG_RESCUED in afterRetry.profile.storyFlags)
    assertFalse(LostCourierProgression.FLAG_FAILED in afterRetry.profile.storyFlags)
  }

  @Test
  fun `saved courier service allows crafting with exactly ninety gold`() = runTest {
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    val source = LocalGuestDataSource(manager)
    source.settleExpedition(
      initial.profile, initial.heroes, emptySet(), 1, true, courierOutcome = CourierOutcome.RESCUED,
    )
    manager.mutateSnapshot { it.copy(profile = it.profile.copy(gold = 90L, ironOre = 3L, arcaneEssence = 2L)) }
    val restarted = LocalGuestSaveManager(context)
    val before = restarted.guestSnapshot.value!!
    LocalGuestDataSource(restarted).synthesizeEquipment(before.profile)
    val after = LocalGuestSaveManager(context).guestSnapshot.value!!
    assertEquals(0L, after.profile.gold)
    assertEquals(0L, after.profile.ironOre)
    assertEquals(0L, after.profile.arcaneEssence)
    assertEquals(before.equipment.size + 1, after.equipment.size)
  }

  @Test
  fun `failed atomic write does not claim rescue or alter valid saves and retry succeeds`() = runTest {
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    val primary = manager.primaryFileForTest()
    val backup = manager.backupFileForTest()
    val primaryBytes = primary.readBytes()
    val backupBytes = backup.readBytes()
    val blockedTemp = File(primary.parentFile, "guest_save_v2.tmp")
    assertTrue(blockedTemp.mkdir())
    val source = LocalGuestDataSource(manager)
    try {
      source.settleExpedition(
        initial.profile, initial.heroes, emptySet(), 1, true, courierOutcome = CourierOutcome.RESCUED,
      )
      fail("A failed save must propagate to the mission retry UI")
    } catch (_: IOException) {
      assertEquals(initial, manager.guestSnapshot.value)
      assertArrayEquals(primaryBytes, primary.readBytes())
      assertArrayEquals(backupBytes, backup.readBytes())
      assertEquals(initial.guestPlayerId, LocalGuestSaveManager(context).guestSnapshot.value!!.guestPlayerId)
    }
    assertTrue(blockedTemp.delete())
    val result = source.settleExpedition(
      initial.profile, initial.heroes, emptySet(), 1, true, courierOutcome = CourierOutcome.RESCUED,
    )
    assertTrue(result.courierForgeUnlocked)
    assertEquals(1L, LocalGuestSaveManager(context).guestSnapshot.value!!.profile.totalExpeditions)
  }

  @Test
  fun `full old bond retains new memory and permanent flags survive transient churn`() {
    val memory = LostCourierProgression.rememberRescue("x".repeat(200))
    assertTrue(memory.length <= 200)
    assertTrue(LostCourierProgression.hasRescueMemory(memory))
    assertEquals(memory, LostCourierProgression.rememberRescue(memory))
    val flags = SanctuaryProgressionHelper.compactStoryFlags(
      listOf("PROLOGUE_SEEN", "EXP_TEST", LostCourierProgression.FLAG_RESCUED, LostCourierProgression.FLAG_FAILED) +
        (1..50).map { "DISC_$it" },
    )
    assertEquals(20, flags.size)
    assertTrue(flags.containsAll(listOf("PROLOGUE_SEEN", "EXP_TEST", LostCourierProgression.FLAG_RESCUED, LostCourierProgression.FLAG_FAILED)))
    assertEquals(90L, LostCourierProgression.synthesisGoldCost(flags))
    assertEquals(120L, LostCourierProgression.synthesisGoldCost(emptyList()))
  }
}
