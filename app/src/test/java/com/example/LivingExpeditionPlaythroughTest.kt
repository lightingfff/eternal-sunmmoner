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

/** Real guest actors and public commands; no teleports, forced kills, HP edits or stat buffs. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LivingExpeditionPlaythroughTest {
  @Test fun northernScoutAndEasternCooperationReachThePhysicalExitAndPersist() = runTest {
    play(listOf("north", "east"), mapOf("north_approach" to "north_scout", "east_gate" to "east_cooperate"))
  }

  @Test fun southernWitnessRescueAndNorthernAssaultReachThePhysicalExitAndPersist() = runTest {
    play(listOf("south", "north"), mapOf("south_witness" to "south_rescue", "north_approach" to "north_assault"))
  }

  @Test fun southernMedicineAndForcedEasternGateRecoverSuppliesAndPersist() = runTest {
    play(listOf("south", "east"), mapOf("south_witness" to "south_medicine", "east_gate" to "east_force"),
      CourierChoice.RECOVER_SUPPLIES)
  }

  private suspend fun play(route: List<String>, choices: Map<String, String>,
    courierChoice: CourierChoice = CourierChoice.SUPPORT_COURIER) {
    val base = ApplicationProvider.getApplicationContext<Context>()
    val files = Files.createTempDirectory(base.cacheDir.toPath(), "living-expedition-").toFile()
    val context = object : ContextWrapper(base) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = files
    }
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    val party = initial.heroes.filter { it.inActiveSquad }
    assertEquals(3, party.size)
    assertTrue(party.all { it.level == 1L })
    val engine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), party, initial.equipment,
      enableLostCourier = true, enableLivingExploration = true, random = Random(91))
    val mission = engine.lostCourier!!
    val expedition = mission.livingExpedition!!
    assertNull("The runtime expedition must not also run the legacy chapter checklist", mission.journey)
    engine.stage = MissionStage.IN_PROGRESS
    engine.resumeLivingExploration(route.first())
    val attemptId = UUID.randomUUID().toString()
    var seconds = 0f
    var nextSnapshot = 0f
    var siteIndex = 0
    var escortStarted = false
    var intercepted = false
    var protectedAfterScout = false
    var supplyTripFinished = false
    var witnessHpWhenMedicineChosen: Int? = null
    var lateWitnessDamageObserved = false
    var dodge: CourierTelegraph? = null
    var telegraphs = 0
    var counters = 0
    var walkingSeconds = 0f
    var combatSeconds = 0f
    var previousState = ""
    val resolvedOrder = mutableListOf<String>()
    val selectedOptions = linkedSetOf<String>()
    val log = StringBuilder("event,simulation_seconds,attempt_id,outcome,knowledge_sites,completed_ids,decision,automation,order,courier,witness,heroes,walking_seconds,combat_seconds\n")
    fun resolvedSites() = expedition.sites.filter { it.state.name == "RESOLVED" }.map { it.id }
    fun record(event: String) {
      val witness = expedition.witness
      val columns = listOf(event, seconds, attemptId, mission.outcome, resolvedSites().joinToString(";"),
        expedition.completedIds.joinToString(";"), expedition.decision?.id, expedition.automationEnabled,
        engine.activeOrder, "${mission.courier.x}/${mission.courier.y}:hp${mission.courier.hp}",
        witness?.let { "${it.id}:${it.x}/${it.y}:hp${it.hp}/${it.maxHp}:rescued=${it.rescued}:alive=${it.alive}" },
        engine.heroes.joinToString(";") { "${it.name}:${it.x}/${it.y}:hp${it.hp}/${it.maxHp}:intent=${it.aiIntentLabelVi}" },
        walkingSeconds, combatSeconds)
      log.append(columns.joinToString(",") { "\"${it.toString().replace("\"", "\"\"")}\"" }).append('\n')
    }
    fun protectEscort() {
      if (engine.activeOrder != TacticalOrder.PROTECT_ALLY) engine.chooseCourierOrder(CourierOrder.PROTECT)
    }
    record("START_${route.joinToString("_")}")
    while (seconds < 900f && mission.outcome == null) {
      engine.step(1f / 60f, 0f, 0f)
      seconds += 1f / 60f
      if (engine.heroes.any { !it.knockedOut && it.isMoving }) walkingSeconds += 1f / 60f
      if (engine.enemies.any { !it.dead && it.aggroed }) combatSeconds += 1f / 60f
      for (hero in engine.heroes) {
        if (!hero.knockedOut && engine.enemies.any { !it.dead && hypot(it.x - hero.x, it.y - hero.y) < 400f }) {
          engine.triggerHeroManualSkill(hero.id)
        }
      }
      val resolved = resolvedSites()
      if (witnessHpWhenMedicineChosen != null && expedition.witness!!.hp < witnessHpWhenMedicineChosen!!) {
        if (!lateWitnessDamageObserved) record("WITNESS_DAMAGED_AFTER_MEDICINE")
        lateWitnessDamageObserved = true
      }
      resolved.filterNot { it in resolvedOrder }.forEach {
        resolvedOrder += it
        record("RESOLVED_$it")
      }
      val state = expedition.sites.joinToString(";") { "${it.id}:${it.state}" } +
        ":${expedition.decision?.id}:${mission.phase}:${mission.scoutWarningRemaining > 0f}:${mission.eliteEnemyId}"
      if (state != previousState) { record("STATE"); previousState = state }
      if (seconds >= nextSnapshot) { record("SNAPSHOT"); nextSnapshot = seconds + 5f }
      if (mission.outcome != null) break

      if (expedition.active) {
        assertNull("Knowledge and decisions must not award a rescue before physical extraction", mission.outcome)
        assertEquals(MissionStage.IN_PROGRESS, engine.stage)
        if (resolved.size < 2) assertFalse("One site cannot unlock extraction", expedition.readyToExtract)
        if (siteIndex == 0 && route.first() in resolved) {
          siteIndex = 1
          engine.resumeLivingExploration(route[siteIndex])
          record("EXPLORE_${route[siteIndex]}")
        }
        val decision = expedition.decision
        val optionId = when {
          decision == null -> null
          decision.id == "extraction" && resolved.size >= 2 -> "evacuate"
          else -> choices[decision.id]
        }
        if (optionId != null && decision!!.options.any { it.id == optionId && it.enabled }) {
          if (optionId == "evacuate") {
            assertTrue(expedition.readyToExtract)
            assertEquals(route.toSet(), resolved.toSet())
            assertNull(mission.outcome)
            assertTrue("The courier must still be short of the exit when evacuation is chosen", mission.progress < .99f)
          }
          if (engine.chooseLivingExpedition(optionId)) {
            selectedOptions += optionId
            if (optionId == "south_medicine") witnessHpWhenMedicineChosen = expedition.witness!!.hp
            record("CHOOSE_$optionId")
            if (optionId == "evacuate") {
              escortStarted = true
              assertNull("Choosing evacuation must not itself win the mission", mission.outcome)
            }
          }
        }
        continue
      }

      assertTrue("Exploration must end through the public evacuation decision", escortStarted)
      if (mission.choiceAvailable) {
        assertTrue(mission.chooseChoice(courierChoice))
        record("COURIER_CHOICE_$courierChoice")
      }
      if (!intercepted && mission.scoutWarningRemaining > 0f) {
        engine.chooseCourierOrder(CourierOrder.INTERCEPT)
        intercepted = true
        record("INTERCEPT_SCOUT")
      }
      if (intercepted && !protectedAfterScout && mission.scoutWarningRemaining <= 0f) {
        protectedAfterScout = true
        protectEscort()
        record("PROTECT_AFTER_SCOUT")
      }
      if (courierChoice == CourierChoice.RECOVER_SUPPLIES && mission.choice != null && !supplyTripFinished &&
        !(intercepted && !protectedAfterScout)) {
        if (!mission.suppliesRecovered) {
          // Clear threats around the courier before resuming the side trip. Combat
          // orders intentionally interrupt supply navigation; don't leave the escort
          // exposed to the crossing ambush while collecting the alternate reward.
          val courierThreat = engine.enemies.filter { enemy ->
            !enemy.dead && hypot(enemy.x - mission.courier.x, enemy.y - mission.courier.y) <= 500f
          }.minByOrNull { enemy -> hypot(enemy.x - mission.courier.x, enemy.y - mission.courier.y) }
          if (courierThreat != null) {
            if (engine.designatedTargetId != courierThreat.id ||
              engine.activeOrder !in setOf(TacticalOrder.ATTACK_TARGET, TacticalOrder.FOCUS_FIRE)) {
              engine.onBattlefieldTapped(courierThreat.x, courierThreat.y)
              engine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE)
            }
            continue
          }
          if (!engine.navigatingCourierSupplies && engine.navigateToCourierSupplies()) record("START_SUPPLY_TRAIL")
          if (engine.collectCourierSupplies()) record("SUPPLIES_PICKED_UP")
        } else if (!engine.navigatingCourierSupplies) {
          supplyTripFinished = true
          record("RETURN_FROM_SUPPLIES")
          protectEscort()
        }
        continue
      }
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
      if (mission.eliteEnemyId != null && engine.enemies.any { it.id == mission.eliteEnemyId && it.dead }) protectEscort()
    }
    record("OUTCOME_${mission.outcome}")
    val output = File("build/visual_screenshots/floor1-living-expedition").apply { mkdirs() }
    output.resolve("${route.joinToString("_")}_playthrough.csv").writeText(log.toString())
    val trace = log.takeLast(12_000)
    println("LIVING_EXPEDITION_ACTUAL_SIMULATION route=$route seconds=$seconds walking=$walkingSeconds combat=$combatSeconds outcome=${mission.outcome} attempt=$attemptId lateWitnessDamage=$lateWitnessDamageObserved completed=${expedition.completedIds}")
    assertEquals("Real starter-party movement/combat must finish within the simulation bound. $trace",
      if (courierChoice == CourierChoice.RECOVER_SUPPLIES) CourierOutcome.SUPPLIES_RECOVERED else CourierOutcome.RESCUED,
      mission.outcome)
    assertEquals(route, resolvedOrder)
    assertTrue(choices.values.all { it in selectedOptions })
    assertEquals(2, resolvedSites().size)
    assertTrue("The unused third site must remain optional", expedition.sites.single { it.id !in route }.state.name != "RESOLVED")
    assertTrue(escortStarted && intercepted && protectedAfterScout)
    assertFalse("Actual interception must prevent the original scout's reinforcements", mission.scoutCalledReinforcements)
    assertTrue("The original guardian must offer actual dodge and counter opportunities", telegraphs > 0 && counters > 0)
    assertTrue(mission.courier.hp > 0)
    assertTrue("Rescue requires the courier at the physical extraction point",
      hypot(mission.courier.x - 2860f, mission.courier.y - 1066f) <= 8.1f)
    assertEquals(MissionStage.VICTORY, engine.stage)
    assertTrue(walkingSeconds > 0f && combatSeconds > 0f)
    if ("east" in route) {
      assertTrue(expedition.gateSecured)
      assertEquals("east_force" in selectedOptions, expedition.gateForced)
      if ("east_force" in selectedOptions) assertTrue("Forcing the gate must create the actual alert guard",
        engine.enemies.any { it.id == "expedition_gate_alert_guard" })
    }
    if ("south" in route) {
      assertNotNull(expedition.witness)
      if ("south_rescue" in selectedOptions) {
        assertTrue("The witness must physically survive rescue", expedition.witness!!.rescued && expedition.witness!!.alive)
      } else {
        assertFalse("Using the medicine must not silently count as rescuing the witness", expedition.witness!!.rescued)
        assertTrue("The abandoned witness must remain vulnerable to actual field combat", lateWitnessDamageObserved)
      }
    }
    if ("north" in route) assertEquals("north_scout" in selectedOptions, expedition.alarmSilenced)
    if (courierChoice == CourierChoice.RECOVER_SUPPLIES) assertTrue(supplyTripFinished && mission.suppliesRecovered)

    val hpRatios = engine.remainingHeroHpRatios()
    val discovered = engine.discoveredNodeIds() + expedition.completedIds
    val source = LocalGuestDataSource(manager)
    val settlement = source.settleExpedition(initial.profile, party, engine.fallenHeroIds(), 1, true,
      bonusGold = engine.bonusDiscoveryGold, bonusMana = engine.bonusDiscoveryMana,
      discoveredNodeIds = discovered, courierOutcome = mission.outcome,
      courierMemoryHeroId = mission.frightenedHeroId, remainingHeroHpRatios = hpRatios, attemptId = attemptId)
    val restored = LocalGuestSaveManager(context).guestSnapshot.value!!
    assertTrue(LostCourierProgression.FLAG_RESCUED in restored.profile.storyFlags)
    if (courierChoice == CourierChoice.RECOVER_SUPPLIES)
      assertTrue(LostCourierProgression.FLAG_SUPPLIES_RECOVERED in restored.profile.storyFlags)
    assertEquals(initial.profile.gold + settlement.goldEarned, restored.profile.gold)
    expedition.completedIds.forEach { id ->
      assertTrue("Completed branch $id must survive a fresh save-manager restart", "DISC_${id.take(36)}" in restored.profile.storyFlags)
    }
    for (hero in party) {
      val expectedHp = (hero.maxHp * hpRatios.getValue(hero.id).toDouble()).roundToLong()
        .coerceAtLeast(if (hpRatios.getValue(hero.id) > 0f) 1 else 0)
      assertEquals("Restart must retain actual battle HP", expectedHp, restored.heroes.single { it.id == hero.id }.currentHp)
    }
    val replay = LocalGuestDataSource(LocalGuestSaveManager(context)).settleExpedition(initial.profile, party,
      engine.fallenHeroIds(), 1, true, bonusGold = engine.bonusDiscoveryGold, bonusMana = engine.bonusDiscoveryMana,
      discoveredNodeIds = discovered, courierOutcome = mission.outcome,
      courierMemoryHeroId = mission.frightenedHeroId, remainingHeroHpRatios = hpRatios, attemptId = attemptId)
    assertEquals(settlement, replay)
    assertEquals("An already settled attempt must not grant rewards or overwrite HP twice", restored,
      LocalGuestSaveManager(context).guestSnapshot.value!!)
  }
}
