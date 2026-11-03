package com.example

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalGuestDataSource
import com.example.data.LostCourierProgression
import com.example.game.combat.CourierChoice
import com.example.data.local.LocalGuestSaveManager
import com.example.data.model.CourierOutcome
import com.example.data.model.HeroClassType
import com.example.data.model.TacticalOrder
import com.example.game.combat.CourierOrder
import com.example.game.combat.CourierTelegraph
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.tower.TowerFloorCatalog
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Uses the actual new-guest roster/equipment, without training, a healer or stat edits. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LostCourierStarterPartyTest {
  @Test fun newGuestCanRescueCourierWithTaughtOrders() = playThrough(CourierChoice.SUPPORT_COURIER)
  @Test fun newGuestCanRecoverSuppliesAndPersistADifferentOutcome() = playThrough(CourierChoice.RECOVER_SUPPLIES)

  @Test fun missedSupplyPickupRescuesWithoutGrantingSupportRewards() = playThrough(CourierChoice.RECOVER_SUPPLIES, skipPickup=true)

  private fun playThrough(choice: CourierChoice, skipPickup: Boolean = false) = runTest {
    val base = ApplicationProvider.getApplicationContext<Context>()
    val isolatedFiles = Files.createTempDirectory(base.cacheDir.toPath(), "courier-starter-").toFile()
    val context = object : ContextWrapper(base) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = isolatedFiles
    }
    val initial = LocalGuestSaveManager(context).startOrResumeGuestSession()
    val party = initial.heroes.filter { it.inActiveSquad }
    assertEquals(setOf(HeroClassType.KNIGHT, HeroClassType.ARCHER, HeroClassType.MAGE), party.map { it.classType }.toSet())
    assertTrue(party.all { it.level == 1L })
    val engine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), party, initial.equipment,
      enableLostCourier = true)
    val mission = engine.lostCourier!!
    engine.stage = MissionStage.IN_PROGRESS // Dismiss the two opening dialogue panels.
    engine.chooseCourierOrder(CourierOrder.PROTECT)
    var intercepted = false
    var returnedToCover = false
    var feared = false
    var telegraphs = 0
    var counters = 0
    var seconds = 0f
    var dodging: CourierTelegraph? = null
    val log = StringBuilder("event,simulation_seconds,courier_hp,living_heroes\n")
    fun record(event: String) {
      log.append("$event,$seconds,${mission.courier.hp},${engine.heroes.count { !it.knockedOut }}\n")
    }
    repeat(60 * 180) {
      if (mission.outcome != null) return@repeat
      engine.step(1f / 60f, 0f, 0f)
      seconds += 1f / 60f
      if (mission.choiceAvailable && (!skipPickup || mission.courier.x > 2300f)) {
        assertTrue(mission.chooseChoice(choice))
        record("CHOICE_$choice")
      }
      if (!skipPickup && mission.choice == CourierChoice.RECOVER_SUPPLIES && !mission.suppliesRecovered) {
        engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
        engine.onBattlefieldTapped(mission.supplyX, mission.supplyY)
      } else if (mission.suppliesRecovered && engine.activeOrder == TacticalOrder.MOVE_TO && mission.eliteEnemyId == null) {
        engine.chooseCourierOrder(CourierOrder.PROTECT)
        record("SUPPLIES_COLLECTED")
      }
      feared = feared || engine.heroes.any { it.fearSecondsRemaining > 0f }
      if (!intercepted && mission.scoutWarningRemaining > 0f) {
        engine.chooseCourierOrder(CourierOrder.INTERCEPT)
        intercepted = true
        record("INTERCEPT")
      }
      if (intercepted && !returnedToCover && mission.scoutWarningRemaining <= 0f) {
        engine.chooseCourierOrder(CourierOrder.PROTECT)
        returnedToCover = true
        record("PROTECT_AFTER_SCOUT")
      }
      val cue = mission.eliteTelegraph
      if (cue != null && cue !== dodging) {
        engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
        engine.onBattlefieldTapped(cue.x - 220f, cue.y)
        dodging = cue
        telegraphs++
        record("DODGE")
      }
      if (cue == null && dodging != null && mission.eliteRecoveryRemaining > 0f) {
        engine.enemies.firstOrNull { it.id == mission.eliteEnemyId && !it.dead }?.let { elite ->
          engine.onBattlefieldTapped(elite.x, elite.y)
          engine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE)
          counters++
          record("COUNTER")
        }
        dodging = null
      }
      if (mission.eliteEnemyId != null && engine.enemies.any { it.id == mission.eliteEnemyId && it.dead } &&
        engine.activeOrder != TacticalOrder.PROTECT_ALLY) {
        engine.chooseCourierOrder(CourierOrder.PROTECT)
        record("ESCORT_TO_GATE")
      }
    }
    record("OUTCOME_${mission.outcome}")
    File("build/visual_screenshots/p05").apply { mkdirs() }
      .resolve("starter_party_${choice}_${skipPickup}_playthrough.csv").writeText(log.toString())
    assertTrue("Scout must provide a real intercept opportunity", intercepted && returnedToCover)
    assertFalse("Timely starter-party interception must prevent reinforcements", mission.scoutCalledReinforcements)
    assertTrue("The inexperienced starter must visibly experience temporary fear", feared)
    assertTrue("Starter party must reach a telegraph and counterattack", telegraphs > 0 && counters > 0)
    assertEquals("Untrained starting party with taught orders must be able to rescue the courier", if (skipPickup) CourierOutcome.RESCUED_WITHOUT_SUPPLIES else if (choice == CourierChoice.RECOVER_SUPPLIES) CourierOutcome.SUPPLIES_RECOVERED else CourierOutcome.RESCUED, mission.outcome)
    assertTrue(mission.courier.hp > 0)
    assertEquals(choice, mission.choice)
    val source = LocalGuestDataSource(LocalGuestSaveManager(context))
    val settlement = source.settleExpedition(initial.profile, party, engine.fallenHeroIds(), 1, true,
      courierOutcome = mission.outcome, courierMemoryHeroId = mission.frightenedHeroId)
    assertEquals(mission.outcome, settlement.courierOutcome)
    val restored = LocalGuestSaveManager(context).guestSnapshot.value!!
    val flag = if (skipPickup) LostCourierProgression.FLAG_SUPPLIES_MISSED else if (choice == CourierChoice.RECOVER_SUPPLIES) LostCourierProgression.FLAG_SUPPLIES_RECOVERED else LostCourierProgression.FLAG_SUPPORTED
    assertTrue(flag in restored.profile.storyFlags)
    assertTrue(LostCourierProgression.FLAG_RESCUED in restored.profile.storyFlags)
    assertEquals(initial.profile.gold + settlement.goldEarned, restored.profile.gold)
    assertEquals(initial.profile.manaCrystals + settlement.manaEarned, restored.profile.manaCrystals)
    println("MISSION01 $choice outcome=${mission.outcome} skippedPickup=$skipPickup seconds=$seconds gold=${settlement.goldEarned} mana=${settlement.manaEarned} restart_flag=$flag")
  }
}
