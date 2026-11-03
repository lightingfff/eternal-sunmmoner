package com.example

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.example.data.LostCourierProgression
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.data.model.CourierOutcome
import com.example.data.model.HeroClassType
import com.example.data.model.TacticalOrder
import com.example.game.combat.CourierChoice
import com.example.game.combat.CourierOrder
import com.example.game.combat.CourierTelegraph
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.tower.TowerFloorCatalog
import java.io.File
import java.nio.file.Files
import kotlin.math.hypot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real new-guest actors and orders: no teleports, forced kills, stat edits or timer skips. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Mission01BPlaythroughTest {
  private fun isolatedContext(): Context {
    val base = ApplicationProvider.getApplicationContext<Context>()
    val directory = Files.createTempDirectory(base.cacheDir.toPath(), "mission01b-").toFile()
    return object : ContextWrapper(base) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = directory
    }
  }

  @Test fun silenceSignalThenSupportCourierCompletesWholeJourneyAndRestoresStory() = runTest {
    playThrough(silence = true, choice = CourierChoice.SUPPORT_COURIER)
  }

  @Test fun decoySignalThenCollectSuppliesCompletesDifferentPersistedStory() = runTest {
    playThrough(silence = false, choice = CourierChoice.RECOVER_SUPPLIES)
  }

  @Test fun missedPickupStillRescuesWithoutInventingSupportReward() = runTest {
    playThrough(silence = true, choice = CourierChoice.RECOVER_SUPPLIES, skipPickup = true)
  }

  private suspend fun playThrough(silence: Boolean, choice: CourierChoice, skipPickup: Boolean = false) {
    val context = isolatedContext()
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession("Mission01-B Master")
    val party = initial.heroes.filter { it.inActiveSquad }
    assertEquals(setOf(HeroClassType.KNIGHT, HeroClassType.ARCHER, HeroClassType.MAGE), party.map { it.classType }.toSet())
    assertTrue(party.all { it.level == 1L })
    val engine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), party, initial.equipment,
      enableLostCourier = true, enableRescueJourney = true)
    val mission = engine.lostCourier!!
    val journey = mission.journey!!
    engine.stage = MissionStage.IN_PROGRESS
    engine.chooseCourierOrder(CourierOrder.PROTECT)
    var seconds = 0f
    var journeyTravelSeconds = 0f
    var journeyCombatSeconds = 0f
    var peakLiveEnemies = 0
    var peakRoster = 0
    var interactions = 0
    var fieldInteractions = 0
    var focusCommands = 0
    var lastMeaningfulEventSeconds = 0f
    var longestMeaningfulEventGap = 0f
    var fieldTargetId: String? = null
    var selectedRoute = false
    var intercepted = false
    var returnedToCover = false
    var captainDodges = 0
    var captainCounters = 0
    var dodging: CourierTelegraph? = null
    var recordedIds = emptySet<String>()
    val trace = StringBuilder("event,seconds,courier_x,courier_y,courier_hp,living_heroes,travel_seconds,combat_seconds\n")
    fun record(event: String) {
      // Count elapsed time between actual tactical/field events, including movement,
      // instead of resetting at the one-frame pause at each route waypoint.
      val meaningful = event.startsWith("FOCUS_") || event.startsWith("FIELD_ACTION_") ||
        event.startsWith("SIGNAL_") || event.startsWith("INTERACTION_") ||
        event.startsWith("COMPLETED_") || event.startsWith("CHOICE_") ||
        event.startsWith("INTERCEPT") || event.startsWith("COUNTER") || event.startsWith("OUTCOME_")
      if (meaningful) {
        longestMeaningfulEventGap = maxOf(longestMeaningfulEventGap, seconds - lastMeaningfulEventSeconds)
        lastMeaningfulEventSeconds = seconds
      }
      trace.append("$event,$seconds,${mission.courier.x},${mission.courier.y},${mission.courier.hp},${engine.heroes.count { !it.knockedOut }},$journeyTravelSeconds,$journeyCombatSeconds\n")
    }
    val dt = 1f / 60f
    for (tick in 0 until 60 * 650) {
      if (mission.outcome != null) break
      if (journey.active) {
        val liveThreat = engine.enemies.filter { !it.dead && it.id.startsWith("journey_") }
          .sortedByDescending { it.archetype.isRanged }.firstOrNull()
        val action = journey.fieldAction
        val fieldTarget = action?.currentTarget
        if (liveThreat != null) {
          if (engine.designatedTargetId != liveThreat.id || engine.activeOrder != TacticalOrder.FOCUS_FIRE) {
            engine.onBattlefieldTapped(liveThreat.x, liveThreat.y)
            engine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE)
            focusCommands++
            record("FOCUS_${liveThreat.id}")
          }
        } else if (journey.fieldReady && fieldTarget != null) {
          if (fieldTargetId != fieldTarget.id || engine.activeOrder != TacticalOrder.MOVE_TO) {
            engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
            engine.onBattlefieldTapped(fieldTarget.x, fieldTarget.y)
            fieldTargetId = fieldTarget.id
            record("MOVE_TO_${fieldTarget.id}")
          }
          if (journey.interactField()) {
            fieldInteractions++
            record("FIELD_ACTION_${fieldTarget.id}")
          }
        } else if (journey.decisionPending) {
          assertTrue(journey.chooseRoute(silence))
          selectedRoute = true
          record("SIGNAL_${if (silence) "SILENCED" else "DECOY"}")
        } else if (journey.canInteract) {
          assertTrue(journey.interact())
          interactions++
          record("INTERACTION_$interactions")
        } else if (engine.activeOrder != TacticalOrder.PROTECT_ALLY) {
          engine.chooseCourierOrder(CourierOrder.PROTECT)
          record("REGROUP_AND_PROTECT_${journey.phaseId}")
        }
      } else {
        if (mission.choiceAvailable && (!skipPickup || mission.courier.x > 2300f)) {
          assertTrue(mission.chooseChoice(choice))
          record("CHOICE_$choice")
        }
        if (!skipPickup && mission.choice == CourierChoice.RECOVER_SUPPLIES && !mission.suppliesRecovered) {
          if (engine.activeOrder != TacticalOrder.MOVE_TO) {
            engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
            engine.onBattlefieldTapped(mission.supplyX, mission.supplyY)
          }
        } else if (mission.suppliesRecovered && engine.activeOrder == TacticalOrder.MOVE_TO && mission.eliteEnemyId == null) {
          engine.chooseCourierOrder(CourierOrder.PROTECT)
          record("SUPPLIES_COLLECTED")
        }
        if (!intercepted && mission.scoutWarningRemaining > 0f) {
          engine.chooseCourierOrder(CourierOrder.INTERCEPT)
          intercepted = true
          record("INTERCEPT")
        }
        if (intercepted && !returnedToCover && mission.scoutWarningRemaining <= 0f) {
          engine.chooseCourierOrder(CourierOrder.PROTECT)
          returnedToCover = true
        }
        val cue = mission.eliteTelegraph
        if (cue != null && cue !== dodging) {
          engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
          engine.onBattlefieldTapped(cue.x - 220f, cue.y)
          dodging = cue
          captainDodges++
        }
        if (cue == null && dodging != null && mission.eliteRecoveryRemaining > 0f) {
          engine.enemies.firstOrNull { it.id == mission.eliteEnemyId && !it.dead }?.let { captain ->
            engine.onBattlefieldTapped(captain.x, captain.y)
            engine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE)
            captainCounters++
          }
          dodging = null
        }
        if (mission.eliteEnemyId != null && engine.enemies.any { it.id == mission.eliteEnemyId && it.dead } &&
          engine.activeOrder != TacticalOrder.PROTECT_ALLY) engine.chooseCourierOrder(CourierOrder.PROTECT)
      }
      engine.step(dt, 0f, 0f)
      seconds += dt
      if (journey.active) {
        if (engine.enemies.any { !it.dead && it.aggroed }) journeyCombatSeconds += dt
        else if (mission.courier.isMoving) journeyTravelSeconds += dt
      }
      peakLiveEnemies = maxOf(peakLiveEnemies, engine.enemies.count { !it.dead })
      peakRoster = maxOf(peakRoster, engine.enemies.size)
      if (journey.completedIds != recordedIds) {
        record("COMPLETED_${journey.completedIds.minus(recordedIds).joinToString("+")}")
        recordedIds = journey.completedIds.toSet()
      }
      if (tick % (60 * 30) == 0) record("CHECKPOINT_${journey.phaseId}")
    }
    record("OUTCOME_${mission.outcome}")
    val name = "journey_${silence}_${choice}_${skipPickup}"
    File("build/visual_screenshots/mission01b").apply { mkdirs() }.resolve("$name.csv").writeText(trace.toString())
    assertFalse("Journey must reach the legacy escort: $trace", journey.active)
    assertTrue("Signal route requires a real committed decision", selectedRoute)
    assertTrue("Authored journey must require multiple field interactions", interactions >= 6)
    assertEquals("Each of the fourteen field steps, including the new ruin reconnaissance, requires an explicit action", 14, fieldInteractions)
    assertTrue("Multiple separate engagements require actual focus decisions", focusCommands >= 5)
    assertTrue("Pacing must come from real actions and movement, measured $seconds seconds", seconds in 300f..600f)
    assertTrue("Travel and combat must both actually execute", journeyTravelSeconds > 0f && journeyCombatSeconds > 0f)
    assertTrue("Finite chapter spawning must bound live enemies: $peakLiveEnemies", peakLiveEnemies <= 12)
    assertTrue("Finite chapter spawning must bound retained roster: $peakRoster", peakRoster <= 40)
    assertTrue("Old scout counter remains functional", intercepted && returnedToCover)
    assertFalse(mission.scoutCalledReinforcements)
    assertTrue("Old captain dodge/counter remains functional", captainDodges > 0 && captainCounters > 0)
    val expectedOutcome = if (skipPickup) CourierOutcome.RESCUED_WITHOUT_SUPPLIES
      else if (choice == CourierChoice.RECOVER_SUPPLIES) CourierOutcome.SUPPLIES_RECOVERED else CourierOutcome.RESCUED
    assertEquals("Unmodified starter party must survive the authored journey: $trace", expectedOutcome, mission.outcome)
    val discovered = engine.discoveredNodeIds()
    assertTrue(discovered.containsAll(journey.completedIds))
    val settlement = LocalGuestDataSource(manager).settleExpedition(initial.profile, party,
      engine.fallenHeroIds(), 1, true, discoveredNodeIds = discovered,
      courierOutcome = mission.outcome, courierMemoryHeroId = mission.frightenedHeroId)
    val restored = LocalGuestSaveManager(context).guestSnapshot.value!!
    assertEquals(expectedOutcome, settlement.courierOutcome)
    assertTrue(LostCourierProgression.FLAG_RESCUED in restored.profile.storyFlags)
    val outcomeFlag = if (skipPickup) LostCourierProgression.FLAG_SUPPLIES_MISSED
      else if (choice == CourierChoice.RECOVER_SUPPLIES) LostCourierProgression.FLAG_SUPPLIES_RECOVERED
      else LostCourierProgression.FLAG_SUPPORTED
    assertTrue(outcomeFlag in restored.profile.storyFlags)
    for (id in journey.completedIds) assertTrue("Chapter consequence lost on reload: $id", "DISC_${id.take(36)}" in restored.profile.storyFlags)
    println("MISSION01B $name seconds=$seconds travel=$journeyTravelSeconds combat=$journeyCombatSeconds live=$peakLiveEnemies roster=$peakRoster fieldActions=$fieldInteractions focusCommands=$focusCommands longestMeaningfulEventGap=$longestMeaningfulEventGap outcome=${mission.outcome} flags=${journey.completedIds}")
  }

  @Test fun retreatDuringJourneyPersistsFailureAndRetryStartsFresh() = runTest {
    val context = isolatedContext()
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    val party = initial.heroes.filter { it.inActiveSquad }
    fun fresh() = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), party, initial.equipment,
      enableLostCourier = true, enableRescueJourney = true).apply { stage = MissionStage.IN_PROGRESS }
    val failed = fresh()
    failed.chooseCourierOrder(CourierOrder.PROTECT)
    repeat(60 * 15) { failed.step(1f / 60f, 0f, 0f) }
    val beforeX = failed.lostCourier!!.courier.x
    failed.issueTacticalOrder(TacticalOrder.RETREAT)
    repeat(60 * 60) { failed.step(1f / 60f, 0f, 0f) }
    assertTrue("Retreat returns real heroes to extraction", failed.retreatCompleted)
    assertEquals(CourierOutcome.FAILED, failed.lostCourier!!.outcome)
    LocalGuestDataSource(manager).settleExpedition(initial.profile, party, failed.fallenHeroIds(), 1, false,
      retreatUsed = true, discoveredNodeIds = failed.discoveredNodeIds(), courierOutcome = failed.lostCourier!!.outcome)
    assertTrue(LostCourierProgression.FLAG_FAILED in LocalGuestSaveManager(context).guestSnapshot.value!!.profile.storyFlags)
    val retry = fresh()
    assertNull(retry.lostCourier!!.outcome)
    assertTrue(retry.lostCourier!!.journey!!.active)
    assertTrue(retry.lostCourier!!.journey!!.completedIds.isEmpty())
    assertTrue(retry.enemies.none { it.dead })
    assertTrue("Retry resets courier instead of retaining progress", hypot(retry.lostCourier!!.courier.x - beforeX, retry.lostCourier!!.courier.y - failed.lostCourier!!.courier.y) > 0f)
  }
  @Test fun retreatFromDistantSignalFollowsTheReturnRouteAndFreshRetryIsClean() = runTest {
    val context = isolatedContext()
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    val party = initial.heroes.filter { it.inActiveSquad }
    fun fresh() = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), party, initial.equipment,
      enableLostCourier = true, enableRescueJourney = true).apply { stage = MissionStage.IN_PROGRESS }
    val engine = fresh()
    val mission = engine.lostCourier!!
    val journey = mission.journey!!
    engine.chooseCourierOrder(CourierOrder.PROTECT)
    var targetId: String? = null
    var reachedSignal = false
    val signalChapter = com.example.game.combat.RescueJourneyContent.chapters.first { it.id == "signal" }
    val signalSite = com.example.game.combat.RescueJourneyContent.routePoints[signalChapter.routeEnd]
    for (tick in 0 until 60 * 500) {
      if (journey.phaseId == "signal" && hypot(mission.courier.x-signalSite.first, mission.courier.y-signalSite.second) <= 32f) { reachedSignal = true; break }
      if (mission.outcome != null) break
      val threat = engine.enemies.firstOrNull { !it.dead && it.id.startsWith("journey_") }
      val target = journey.fieldAction?.currentTarget
      if (threat != null) {
        if (engine.designatedTargetId != threat.id || engine.activeOrder != TacticalOrder.FOCUS_FIRE) {
          engine.onBattlefieldTapped(threat.x, threat.y)
          engine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE)
        }
      } else if (journey.fieldReady && target != null) {
        if (targetId != target.id || engine.activeOrder != TacticalOrder.MOVE_TO) {
          engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
          engine.onBattlefieldTapped(target.x,target.y)
          targetId = target.id
        }
        journey.interactField()
      } else if (journey.decisionPending) {
        journey.chooseRoute(true)
      } else if (journey.canInteract) {
        journey.interact()
      } else if (engine.activeOrder != TacticalOrder.PROTECT_ALLY) {
        engine.chooseCourierOrder(CourierOrder.PROTECT)
      }
      engine.step(1f / 60f,0f,0f)
    }
    assertTrue("Real starter party must reach the distant signal site before retreat", reachedSignal)
    engine.issueTacticalOrder(TacticalOrder.RETREAT)
    repeat(60 * 240) { if (mission.outcome == null) engine.step(1f / 60f,0f,0f) }
    assertTrue("Late retreat must navigate the authored route back to spawn", engine.retreatCompleted)
    assertEquals(CourierOutcome.FAILED, mission.outcome)
    LocalGuestDataSource(manager).settleExpedition(initial.profile, party, engine.fallenHeroIds(), 1,
      victory=false, retreatUsed=true, discoveredNodeIds=engine.discoveredNodeIds(), courierOutcome=mission.outcome)
    assertTrue(LostCourierProgression.FLAG_FAILED in LocalGuestSaveManager(context).guestSnapshot.value!!.profile.storyFlags)
    val retry = fresh()
    assertNull(retry.lostCourier!!.outcome)
    assertTrue(retry.lostCourier!!.journey!!.completedIds.isEmpty())
  }

}
