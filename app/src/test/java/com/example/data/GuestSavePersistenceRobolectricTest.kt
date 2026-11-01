package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.data.model.TacticalOrder
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.tower.TowerFloorCatalog
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GuestSavePersistenceRobolectricTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    File(context.filesDir, "eternal_summoner_saves").deleteRecursively()
  }

  @Test
  fun `one-tap guest creation initializes starter roster gear and persists GUEST_LOCAL session`() = runTest {
    val manager = LocalGuestSaveManager(context)
    assertEquals(SessionAccountMode.NONE, manager.sessionMode.value)

    val snapshot = manager.startOrResumeGuestSession("Master Kael")
    assertTrue(snapshot.guestPlayerId.startsWith("guest_"))
    assertEquals(SessionAccountMode.GUEST_LOCAL, manager.sessionMode.value)
    assertEquals("Master Kael", snapshot.profile.masterName)
    assertEquals(3, snapshot.heroes.size)
    assertEquals(4, snapshot.equipment.size)
    assertEquals(7, snapshot.profile.unlockedFacilities.size)
  }

  @Test
  fun `returning guest launch automatically restores saved progress across simulated app restart`() = runTest {
    val manager1 = LocalGuestSaveManager(context)
    val initial = manager1.startOrResumeGuestSession("Summoner Aria")
    val guestId = initial.guestPlayerId
    val dataSource1 = LocalGuestDataSource(manager1)

    // 1. Summon a 4th Hero
    val summoned = dataSource1.summonNewHero(initial.profile, initial.heroes)
    assertNotNull(summoned.id)

    // 2. Train starter Knight Kaelen
    val afterSummon = manager1.guestSnapshot.value!!
    val kaelen = afterSummon.heroes.first { it.name == "Kaelen" }
    dataSource1.trainHero(afterSummon.profile, kaelen, upgradeSkill = false)

    // 3. Host Sanctuary Restaurant Feast -> activates 30-min combat buff
    val afterTrain = manager1.guestSnapshot.value!!
    dataSource1.hostSanctuaryMeal(afterTrain.profile, afterTrain.heroes)

    // 4. Clear Tower Floor 01 and earn rewards + equipment drop
    val beforeExpedition = manager1.guestSnapshot.value!!
    val squad = beforeExpedition.heroes.filter { it.inActiveSquad }
    val settlement = dataSource1.settleExpedition(
      profile = beforeExpedition.profile,
      squadHeroes = squad,
      fallenHeroIds = emptySet(),
      floorNumber = 1,
      victory = true,
    )
    assertTrue(settlement.victory)
    assertNotNull(settlement.lootedItemName)

    // 5. Update Camera & AI Intent settings
    manager1.updateSettings(
      GameSettings(
        defaultZoom = 1.04f,
        particlesEnabled = false,
        showHeroAiIntent = true,
      )
    )

    // Simulate cold app restart with a brand-new LocalGuestSaveManager instance!
    val restartedManager = LocalGuestSaveManager(context)
    assertEquals(
      "Returning guest should bypass AuthGate automatically with GUEST_LOCAL mode",
      SessionAccountMode.GUEST_LOCAL,
      restartedManager.sessionMode.value,
    )
    val restored = restartedManager.guestSnapshot.value
    assertNotNull("Restored guest snapshot must not be null", restored)
    restored!!

    assertEquals(guestId, restored.guestPlayerId)
    assertEquals(1L, restored.profile.highestClearedFloor)
    assertEquals(4, restored.heroes.size)
    assertEquals(5, restored.equipment.size)
    assertTrue(restored.heroes.first { it.name == "Kaelen" }.level >= 2L)
    assertTrue(FeastBuffHelper.isFeastBuffActive(restored.profile.storyFlags))
    assertEquals(1.04f, restored.settings.defaultZoom, 0.01f)
    assertEquals(false, restored.settings.particlesEnabled)
  }

  @Test
  fun `corrupted primary save file automatically recovers from backup without losing player progress`() = runTest {
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession("Master Recovery")
    val ds = LocalGuestDataSource(manager)

    // Perform two mutations so both primary and .bak contain Floor 01 victory
    ds.settleExpedition(
      profile = initial.profile,
      squadHeroes = initial.heroes,
      fallenHeroIds = emptySet(),
      floorNumber = 1,
      victory = true,
    )
    val afterFloor1 = manager.guestSnapshot.value!!
    ds.hostSanctuaryMeal(afterFloor1.profile, afterFloor1.heroes)

    assertTrue(manager.backupFileForTest().exists())

    // Corrupt primary save file with truncated garbage JSON
    manager.primaryFileForTest().writeText("{corrupted_json_payload_truncated!!!", Charsets.UTF_8)

    // Cold restart must recover from .bak without resetting to an empty profile!
    val recoveredManager = LocalGuestSaveManager(context)
    val recovered = recoveredManager.guestSnapshot.value
    assertNotNull("Must recover valid snapshot from .bak backup", recovered)
    assertEquals(initial.guestPlayerId, recovered!!.guestPlayerId)
    assertEquals(1L, recovered.profile.highestClearedFloor)
  }

  @Test
  fun `logout to title gate preserves guest save on disk for seamless re-entry`() = runTest {
    val manager = LocalGuestSaveManager(context)
    val created = manager.startOrResumeGuestSession("Master Switch")
    val ds = LocalGuestDataSource(manager)
    ds.settleExpedition(created.profile, created.heroes, emptySet(), floorNumber = 1, victory = true)

    manager.logoutToTitleGate()
    assertEquals(SessionAccountMode.NONE, manager.sessionMode.value)
    assertTrue(manager.hasSavedGuestProfile())

    val resumed = manager.startOrResumeGuestSession("IgnoredNameBecauseExisting")
    assertEquals(created.guestPlayerId, resumed.guestPlayerId)
    assertEquals(1L, resumed.profile.highestClearedFloor)
    assertEquals(SessionAccountMode.GUEST_LOCAL, manager.sessionMode.value)
  }

  @Test
  fun `feast buff boosts squad combat stats and hero AI updates observable intent labels across all 8 orders`() = runTest {
    val manager = LocalGuestSaveManager(context)
    val snap = manager.startOrResumeGuestSession("Master Tactician")
    val floor1 = TowerFloorCatalog.FLOORS.first()

    val baseEngine = TowerCombatEngine(
      floorSpec = floor1,
      squadRecords = snap.heroes,
      equipment = snap.equipment,
      feastBuffActive = false,
    )
    val buffedEngine = TowerCombatEngine(
      floorSpec = floor1,
      squadRecords = snap.heroes,
      equipment = snap.equipment,
      feastBuffActive = true,
    )

    assertTrue(buffedEngine.heroes.first().maxHp > baseEngine.heroes.first().maxHp)
    assertTrue(buffedEngine.heroes.first().attack > baseEngine.heroes.first().attack)

    buffedEngine.stage = MissionStage.IN_PROGRESS
    for (order in TacticalOrder.entries) {
      buffedEngine.issueTacticalOrder(order)
      if (order == TacticalOrder.MOVE_TO) {
        buffedEngine.onBattlefieldTapped(620f, 540f)
      }
      buffedEngine.step(0.1f, 0f, 0f)
      assertTrue(buffedEngine.heroes.all { it.aiIntentLabelVi.isNotBlank() })
    }
  }
}
