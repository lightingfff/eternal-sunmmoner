package com.example

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.example.data.LostCourierProgression
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.data.model.CourierOutcome
import com.example.data.model.TacticalOrder
import com.example.game.combat.CourierChoice
import com.example.game.combat.CourierOrder
import com.example.game.combat.CourierTelegraph
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.map.EnemyArchetype
import com.example.game.tower.TowerFloorCatalog
import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Actual starter roster, movement, damage and settlement. No teleports, forced deaths or stat edits. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RescueJourneyStarterPartyTest {
  @Test fun supportRouteCompletesFieldworkEscortAndRestart() = runTest {
    play(CourierChoice.SUPPORT_COURIER, silenceAlarm = true, medicineForCourier = true)
  }

  @Test fun suppliesRouteCompletesAlternateIncidentsPickupAndRestart() = runTest {
    play(CourierChoice.RECOVER_SUPPLIES, silenceAlarm = false, medicineForCourier = false)
  }

  private suspend fun play(choice: CourierChoice, silenceAlarm: Boolean, medicineForCourier: Boolean) {
    val base = ApplicationProvider.getApplicationContext<Context>()
    val files = Files.createTempDirectory(base.cacheDir.toPath(), "rescue-route-").toFile()
    val context = object : ContextWrapper(base) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = files
    }
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    val party = initial.heroes.filter { it.inActiveSquad }
    assertTrue(party.all { it.level == 1L })
    val engine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), party, initial.equipment,
      enableLostCourier = true, enableRescueJourney = true, random = Random(91))
    val mission = engine.lostCourier!!
    val journey = mission.journey!!
    engine.stage = MissionStage.IN_PROGRESS
    engine.chooseCourierOrder(CourierOrder.PROTECT)
    var seconds = 0f
    var lastPhase = ""
    var intercepted = false
    var protectedAfterScout = false
    var supplyTripFinished = false
    var dodge: CourierTelegraph? = null
    var telegraphs = 0
    var counters = 0
    var walkingSeconds = 0f
    var combatSeconds = 0f
    val log = StringBuilder("event,simulation_seconds,journey,courier_hp,courier_ward,living_heroes,living_enemies,order,hero_positions,walking_seconds,combat_seconds\n")
    fun record(event: String) {
      log.append("$event,$seconds,${journey.phaseId},${mission.courier.hp},${mission.courier.medicineWardHp}," +
        "${engine.heroes.count { !it.knockedOut }},${engine.enemies.count { !it.dead }},${engine.activeOrder}," +
        engine.heroes.joinToString(";") { "${it.id}:${it.x}/${it.y}:hp${it.hp}" } +
        ",$walkingSeconds,$combatSeconds\n")
    }
    fun followObjective() {
      if (engine.activeOrder != TacticalOrder.PROTECT_ALLY) engine.chooseCourierOrder(CourierOrder.PROTECT)
    }
    fun focusThreat() {
      val nearby = engine.enemies.filter { enemy -> !enemy.dead && engine.heroes.any {
        !it.knockedOut && hypot(it.x - enemy.x, it.y - enemy.y) < 550f
      } }
      val target = nearby.firstOrNull { it.archetype == EnemyArchetype.FIELD_GOBLIN_ARCHER }
        ?: nearby.firstOrNull { it.archetype == EnemyArchetype.DIRE_WOLF }
        ?: nearby.firstOrNull()
      if (target == null) followObjective() else if (engine.designatedTargetId != target.id ||
        engine.activeOrder !in setOf(TacticalOrder.ATTACK_TARGET, TacticalOrder.FOCUS_FIRE)) {
        engine.onBattlefieldTapped(target.x, target.y)
        engine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE)
      }
    }
    while (seconds < 900f && mission.outcome == null) {
      engine.step(1f / 60f, 0f, 0f)
      seconds += 1f / 60f
      if (engine.heroes.any { !it.knockedOut && it.isMoving }) walkingSeconds += 1f / 60f
      if (engine.enemies.any { !it.dead && it.aggroed }) combatSeconds += 1f / 60f
      if (journey.phaseId != lastPhase) {
        record("PHASE_${journey.phaseId}")
        lastPhase = journey.phaseId
        followObjective()
      }
      for (hero in engine.heroes) {
        if (!hero.knockedOut && engine.enemies.any { !it.dead && hypot(it.x - hero.x, it.y - hero.y) < 400f }) {
          engine.triggerHeroManualSkill(hero.id)
        }
      }
      if (journey.active) {
        focusThreat()
        if (journey.fieldReady && journey.interactField()) record("FIELD_${journey.phaseId}")
        if (journey.decisionPending) {
          val committed = journey.chooseRoute(if (journey.phaseId == "medicine") medicineForCourier else silenceAlarm)
          if (committed) { record("INCIDENT_COMMITTED"); followObjective() }
        } else if (journey.canInteract && journey.interact()) {
          record("CHAPTER_CONFIRMED")
          followObjective()
        }
        continue
      }
      if (mission.choiceAvailable) {
        assertTrue(mission.chooseChoice(choice))
        record("CHOICE_$choice")
      }
      if (!intercepted && mission.scoutWarningRemaining > 0f) {
        engine.chooseCourierOrder(CourierOrder.INTERCEPT)
        intercepted = true
        record("INTERCEPT")
      }
      if (intercepted && !protectedAfterScout && mission.scoutWarningRemaining <= 0f) {
        protectedAfterScout = true
        followObjective()
        record("PROTECT_AFTER_SCOUT")
      }
      // Invoke the same road navigation and explicit collection actions as the live HUD.
      if (choice == CourierChoice.RECOVER_SUPPLIES && mission.choice != null && !supplyTripFinished &&
        !(intercepted && !protectedAfterScout)) {
        if (!mission.suppliesRecovered) {
          if (!engine.navigatingCourierSupplies && engine.navigateToCourierSupplies()) record("START_SUPPLY_TRAIL")
          if (engine.collectCourierSupplies()) record("SUPPLIES_PICKED_UP")
        } else if (!engine.navigatingCourierSupplies) {
          supplyTripFinished = true
          record("RETURN_FROM_SUPPLIES")
          followObjective()
        }
        continue
      }
      if (journey.crossingBlocksEscort) {
        focusThreat()
        continue
      }
      if (lastPhase == "crossing" && journey.phaseId == "gate") followObjective()
      val cue = mission.eliteTelegraph
      if (cue != null && cue !== dodge) {
        engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
        engine.onBattlefieldTapped(cue.x - 220f, cue.y)
        dodge = cue
        telegraphs++
        record("DODGE")
      }
      if (cue == null && dodge != null && mission.eliteRecoveryRemaining > 0f) {
        engine.enemies.firstOrNull { it.id == mission.eliteEnemyId && !it.dead }?.let {
          engine.onBattlefieldTapped(it.x, it.y)
          engine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE)
          counters++
          record("COUNTER")
        }
        dodge = null
      }
      if (mission.eliteEnemyId != null && engine.enemies.any { it.id == mission.eliteEnemyId && it.dead }) followObjective()
    }
    record("OUTCOME_${mission.outcome}")
    val out = File("build/visual_screenshots/floor1-playable-loop").apply { mkdirs() }
    out.resolve("${choice.name.lowercase()}_playthrough.csv").writeText(log.toString())
    println("FLOOR1_ACTUAL_SIMULATION choice=$choice seconds=$seconds walking=$walkingSeconds combat=$combatSeconds outcome=${mission.outcome} completed=${journey.completedIds}")
    assertEquals("Actual movement/combat route must reach a terminal rescue; trace: $log",
      if (choice == CourierChoice.RECOVER_SUPPLIES) CourierOutcome.SUPPLIES_RECOVERED else CourierOutcome.RESCUED, mission.outcome)
    assertEquals(7, journey.chapterIndex)
    assertEquals(silenceAlarm, journey.alarmSilenced)
    assertEquals(medicineForCourier, journey.medicineForCourier)
    assertTrue(intercepted && protectedAfterScout)
    assertFalse(mission.scoutCalledReinforcements)
    assertTrue("Guardian must present actual dodge and counter opportunities", telegraphs > 0 && counters > 0)
    assertTrue(mission.courier.hp > 0)
    val attempt = UUID.randomUUID().toString()
    val hpRatios = engine.remainingHeroHpRatios()
    val source = LocalGuestDataSource(manager)
    val result = source.settleExpedition(initial.profile, party, engine.fallenHeroIds(), 1, true,
      bonusGold = engine.bonusDiscoveryGold, bonusMana = engine.bonusDiscoveryMana,
      discoveredNodeIds = engine.discoveredNodeIds() + journey.completedIds,
      courierOutcome = mission.outcome, courierMemoryHeroId = mission.frightenedHeroId,
      remainingHeroHpRatios = hpRatios, attemptId = attempt)
    assertEquals(mission.outcome, result.courierOutcome)
    val restored = LocalGuestSaveManager(context).guestSnapshot.value!!
    assertTrue(LostCourierProgression.FLAG_RESCUED in restored.profile.storyFlags)
    assertEquals(1, restored.heroes.count { LostCourierProgression.hasRescueMemory(it.bondSummary) })
    assertEquals(initial.profile.gold + result.goldEarned, restored.profile.gold)
    for (oldHero in party) {
      val expectedHp = (oldHero.maxHp * hpRatios.getValue(oldHero.id).toDouble()).roundToLong()
        .coerceAtLeast(if (hpRatios.getValue(oldHero.id) > 0f) 1 else 0)
      assertEquals("Return/restart must retain actual battle HP without free level-up healing", expectedHp,
        restored.heroes.first { it.id == oldHero.id }.currentHp)
    }
    val wounded = restored.heroes.first { it.currentHp > 0L && it.currentHp < it.maxHp }
    val healingManager = LocalGuestSaveManager(context)
    LocalGuestDataSource(healingManager).healHeroAtInfirmary(restored.profile, wounded, goldCost = 80L)
    val healed = LocalGuestSaveManager(context).guestSnapshot.value!!
    assertEquals(restored.profile.gold - 80L, healed.profile.gold)
    assertEquals((wounded.currentHp + wounded.maxHp / 2L).coerceAtMost(wounded.maxHp),
      healed.heroes.first { it.id == wounded.id }.currentHp)
    for (other in restored.heroes.filter { it.id != wounded.id }) {
      assertEquals(other.currentHp, healed.heroes.first { it.id == other.id }.currentHp)
    }
    val replayManager = LocalGuestSaveManager(context)
    val replay = LocalGuestDataSource(replayManager).settleExpedition(initial.profile, party, engine.fallenHeroIds(), 1, true,
      bonusGold = engine.bonusDiscoveryGold, bonusMana = engine.bonusDiscoveryMana,
      discoveredNodeIds = engine.discoveredNodeIds() + journey.completedIds,
      courierOutcome = mission.outcome, courierMemoryHeroId = mission.frightenedHeroId,
      remainingHeroHpRatios = hpRatios, attemptId = attempt)
    assertEquals(result, replay)
    assertEquals("Replay of the settled attempt must not undo paid healing or grant resources again", healed,
      LocalGuestSaveManager(context).guestSnapshot.value!!)
    println("FLOOR1_HEALTH_RESTART choice=$choice hero=${wounded.id} battleHp=${wounded.currentHp} healedHp=${healed.heroes.first { it.id == wounded.id }.currentHp} paidGold=80 sameAttemptReplayUnchanged=true")
  }
}
