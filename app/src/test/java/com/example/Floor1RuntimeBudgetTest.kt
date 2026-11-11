package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.model.HeroRecord
import com.example.data.model.TacticalOrder
import com.example.game.combat.CombatAoEEffect
import com.example.game.combat.CombatProjectile
import com.example.game.combat.FloatingCombatText
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.tower.TowerFloorCatalog
import com.example.performance.FixedSceneSimulation
import com.example.performance.SceneFramePacer
import org.junit.Assert.*
import org.junit.Test

/** Deterministic simulation/resource-lifetime checks; these do not measure Android FPS. */
class Floor1RuntimeBudgetTest {
  private fun battle() = TowerCombatEngine(
    floorSpec = TowerFloorCatalog.FLOORS.first(),
    squadRecords = listOf(HeroRecord(id = "budget_knight", name = "Kaelen", maxHp = 100000L)),
    equipment = emptyList(), enableLostCourier = true, enableRescueJourney = true,
  ).apply { stage = MissionStage.IN_PROGRESS }

  @Test fun renderPacingKeepsTheSameFixedSimulationAndEncounterState() {
    fun simulate(fps: Int): Pair<TowerCombatEngine, Int> {
      val engine = battle()
      val pacer = SceneFramePacer(fps)
      val simulation = FixedSceneSimulation()
      var ticks = 0
      for (vsync in 0..600) {
        if (pacer.onVsync(vsync * 1_000_000_000L / 60L)) {
          ticks += simulation.advance(pacer.deltaSeconds.coerceIn(0f, .10f)) {
            engine.step(it, 0f, 0f)
          }
        }
      }
      return engine to ticks
    }
    val (balanced, balancedTicks) = simulate(30)
    val (high, highTicks) = simulate(60)
    assertEquals(600, balancedTicks)
    assertEquals(balancedTicks, highTicks)
    assertEquals(balanced.stage, high.stage)
    assertEquals(balanced.activeOrder, high.activeOrder)
    assertEquals(balanced.lostCourier!!.phase, high.lostCourier!!.phase)
    assertEquals(balanced.lostCourier!!.courier.hp, high.lostCourier!!.courier.hp)
    assertEquals(balanced.lostCourier!!.courier.x, high.lostCourier!!.courier.x, .001f)
    assertEquals(balanced.lostCourier!!.courier.y, high.lostCourier!!.courier.y, .001f)
    assertEquals(balanced.enemies.map { it.id to it.hp }, high.enemies.map { it.id to it.hp })
    assertEquals(balanced.heroes.map { it.hp }, high.heroes.map { it.hp })
    balanced.heroes.zip(high.heroes).forEach { (a, b) ->
      assertEquals(a.x, b.x, .001f)
      assertEquals(a.y, b.y, .001f)
    }
  }

  @Test fun expiredEffectsAreReleasedAndRetryOwnsFreshRuntimeState() {
    val engine = battle()
    // Isolate resource lifetime from damage/outcome rules; this is not a combat playthrough.
    engine.enemies.forEach { it.dead = true; it.hp = 0 }
    engine.issueTacticalOrder(TacticalOrder.HOLD_POSITION)
    engine.projectiles += CombatProjectile("budget_expiring", false, 50f, 50f, 0f, 0f, 1,
      Color.White, remainingLife = .1f)
    engine.aoeEffects += CombatAoEEffect(50f, 50f, 10f, Color.White, duration = .1f)
    engine.floatingTexts += FloatingCombatText(50f, 50f, "budget_expiring", Color.White, duration = .1f)
    repeat(240) { engine.step(1f / 60f, 0f, 0f) }
    assertTrue(engine.projectiles.isEmpty())
    assertTrue(engine.aoeEffects.isEmpty())
    assertTrue(engine.floatingTexts.isEmpty())
    engine.confirmTacticalRetreat()
    assertEquals(MissionStage.DEFEAT, engine.stage)
    val retry = battle()
    assertNotSame(engine, retry)
    assertNotSame(engine.lostCourier, retry.lostCourier)
    assertNotSame(engine.heroes.first(), retry.heroes.first())
    assertNotSame(engine.enemies.first(), retry.enemies.first())
    assertNull(retry.lostCourier!!.outcome)
    assertFalse(retry.retreatCompleted)
    assertTrue(retry.enemies.all { !it.dead && it.hp == it.maxHp })
    assertTrue(retry.projectiles.isEmpty())
    assertTrue(retry.aoeEffects.isEmpty())
    assertTrue(retry.floatingTexts.none { it.text == "budget_expiring" })
  }

  @Test fun authoredWavesAreFiniteAndClearedPhasesCannotRespawnThem() {
    val engine = battle()
    val mission = engine.lostCourier!!
    val journey = mission.journey!!
    engine.issueTacticalOrder(TacticalOrder.HOLD_POSITION)
    fun clearThreats() { engine.enemies.forEach { it.dead = true; it.hp = 0 } }
    fun putSquad(x: Float, y: Float) {
      engine.heroes.forEach { it.x = x; it.y = y; it.anchorX = x; it.anchorY = y }
    }
    // Spatial fixture deliberately skips fighting/travel to isolate one-shot spawning.
    // The independent playthrough suite verifies balance and navigation.
    repeat(250) {
      if (journey.active) {
        putSquad(journey.targetX, journey.targetY)
        clearThreats()
        engine.step(1f / 60f, 0f, 0f)
        clearThreats()
        if (journey.fieldReady) journey.interactField()
        if (journey.canInteract) {
          if (journey.decisionPending) journey.chooseRoute(false) else journey.interact()
        }
      }
    }
    assertFalse("All fieldwork must be reachable without a timer gate", journey.active)
    assertEquals(5, journey.chapterIndex)
    val preludeIds = engine.enemies.map { it.id }
    assertEquals("Each authored actor ID spawns once", preludeIds.size, preludeIds.toSet().size)
    assertEquals(3, preludeIds.count { it.startsWith("journey_lookout_") })
    assertEquals(2, preludeIds.count { it.startsWith("journey_medicine_") })

    // Reach the actual crossing trigger; leave its actors alive so repeated ticks
    // exercise the blocked-phase gate, then kill them to exercise the cleared gate.
    mission.courier.x = 1950f
    mission.courier.y = 1060f
    putSquad(1950f, 1060f)
    engine.step(1f / 60f, 0f, 0f)
    val crossingIds = engine.enemies.filter { it.id.startsWith("journey_crossing_") }.map { it.id }
    assertEquals("Unbroken signal adds exactly one archer", 3, crossingIds.size)
    repeat(180) {
      // Keep this a spawn-gate test rather than allowing combat balance to end it.
      mission.courier.hp = mission.courier.maxHp
      engine.heroes.forEach { it.attackCooldown = 1000f; it.skillCooldown = 1000f }
      engine.step(1f / 60f, 0f, 0f)
    }
    assertEquals(crossingIds, engine.enemies.filter { it.id.startsWith("journey_crossing_") }.map { it.id })
    clearThreats()
    repeat(120) { engine.step(1f / 60f, 0f, 0f) }
    assertEquals(crossingIds, engine.enemies.filter { it.id.startsWith("journey_crossing_") }.map { it.id })
    assertEquals("gate", journey.phaseId)
    assertTrue("Finite authored roster, including scout and guardian", engine.enemies.size <= 16)
    assertEquals(engine.enemies.size, engine.enemies.map { it.id }.toSet().size)
  }

}
