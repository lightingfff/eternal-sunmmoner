package com.example

import com.example.data.model.CourierOutcome
import com.example.data.model.HeroRecord
import com.example.data.model.TacticalOrder
import com.example.game.combat.CourierOrder
import com.example.game.combat.CourierPhase
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.map.CollisionPhysics
import com.example.game.tower.TowerFloorCatalog
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class LostCourierEncounterTest {
  private val novice = HeroRecord(id = "courier_novice", name = "Kaelen", level = 1L)

  private fun battle() = TowerCombatEngine(
    TowerFloorCatalog.FLOORS.first(), listOf(novice), emptyList(), enableLostCourier = true,
  ).apply { stage = MissionStage.IN_PROGRESS }

  private fun clearPath(engine: TowerCombatEngine) {
    engine.enemies.forEach { it.dead = true; it.hp = 0 }
    engine.step(0.01f, 0f, 0f)
  }

  private fun scoutBattle(): TowerCombatEngine = battle().apply {
    clearPath(this)
    val courier = lostCourier!!.courier
    courier.x = 1330f
    heroes.single().apply {
      x = 1330f; y = courier.y
      attackCooldown = 1000f; skillCooldown = 1000f
    }
    issueTacticalOrder(TacticalOrder.HOLD_POSITION)
    step(0.01f, 0f, 0f)
    // Isolate the horn mechanic from guard damage and keep the party out of attack range.
    enemies.filter { it.id != lostCourier!!.scoutEnemyId }.forEach { it.dead = true; it.hp = 0 }
    heroes.single().apply { x = 300f; y = 1100f }
    issueTacticalOrder(TacticalOrder.HOLD_POSITION)
  }

  @Test fun encounterIsOptInAndPreservesTheExistingFloorMap() {
    val spec = TowerFloorCatalog.FLOORS.first()
    val generic = TowerCombatEngine(spec, listOf(novice), emptyList())
    val authored = battle()
    assertNull(generic.lostCourier)
    assertEquals(spec.enemies.count { !it.spawnOnEventWave }, generic.enemies.size)
    assertEquals(2, authored.enemies.size)
    assertEquals(generic.mapLayout.obstacles, authored.mapLayout.obstacles)
    assertNull(TowerCombatEngine(TowerFloorCatalog.FLOORS[1], listOf(novice), emptyList(),
      enableLostCourier = true).lostCourier)
  }

  @Test fun escortWaitsForARealHeroAndRequiresTheGateAfterCombat() {
    val engine = battle()
    val mission = engine.lostCourier!!
    val courier = mission.courier
    clearPath(engine)
    engine.heroes.single().apply { x = 3000f; y = 1700f }
    engine.issueTacticalOrder(TacticalOrder.HOLD_POSITION)
    val waitingX = courier.x
    repeat(20) { engine.step(0.05f, 0f, 0f) }
    assertEquals(waitingX, courier.x, 0.001f)
    assertFalse(courier.isMoving)
    assertEquals(MissionStage.IN_PROGRESS, engine.stage)
    assertNull(mission.outcome)

    // Defeated actors are fixtures; the real escort movement/state machine must finish the route.
    repeat(1500) {
      engine.enemies.forEach { it.dead = true; it.hp = 0 }
      engine.heroes.single().apply { x = courier.x + 70f; y = courier.y }
      engine.step(0.05f, 0f, 0f)
    }
    assertEquals(CourierOutcome.RESCUED, mission.outcome)
    assertEquals(CourierPhase.RESCUED, mission.phase)
    assertEquals(MissionStage.VICTORY, engine.stage)
    assertTrue(hypot(courier.x - 2860f, courier.y - 1066f) <= 8.1f)
    assertTrue(mission.progress > 0.99f)
  }

  @Test fun scoutHasACounterWindowAndInterceptionPreventsRealReinforcements() {
    val ignored = scoutBattle()
    val warning = ignored.lostCourier!!
    assertTrue("The player has time to react", warning.scoutWarningRemaining >= 5f)
    repeat(200) { ignored.step(0.05f, 0f, 0f) }
    assertTrue(warning.scoutCalledReinforcements)
    assertEquals(2, ignored.enemies.count { it.id.startsWith("courier_reinforcement_") })

    val intercepted = scoutBattle()
    val saved = intercepted.lostCourier!!
    intercepted.chooseCourierOrder(CourierOrder.INTERCEPT)
    assertEquals(saved.scoutEnemyId, intercepted.designatedTargetId)
    assertEquals(TacticalOrder.ATTACK_TARGET, intercepted.activeOrder)
    intercepted.enemies.single { it.id == saved.scoutEnemyId }.apply { hp = 0; dead = true }
    intercepted.step(0.05f, 0f, 0f)
    repeat(200) { intercepted.step(0.05f, 0f, 0f) }
    assertFalse(saved.scoutCalledReinforcements)
    assertFalse(intercepted.enemies.any { it.id.startsWith("courier_reinforcement_") })
    assertEquals(0f, saved.scoutWarningRemaining, 0f)
  }

  @Test fun fearReducesRealStatsAndReassuranceRecoversWithoutRepeatingTheEvent() {
    val engine = scoutBattle()
    val mission = engine.lostCourier!!
    val hero = engine.heroes.single()
    assertEquals(hero.id, mission.frightenedHeroId)
    assertTrue(hero.fearSecondsRemaining in 0.01f..10f)
    assertEquals((hero.attack * 0.8f).toInt(), hero.effectiveAttack)
    assertEquals(hero.speed * 0.85f, hero.effectiveSpeed, 0.001f)
    engine.enemies.forEach { it.dead = true; it.hp = 0 }
    engine.chooseCourierOrder(CourierOrder.PROTECT)
    repeat(110) { engine.step(0.05f, 0f, 0f) }
    assertEquals(0f, hero.fearSecondsRemaining, 0f)
    assertEquals(hero.attack, hero.effectiveAttack)
    assertEquals(hero.speed, hero.effectiveSpeed, 0f)
    engine.step(0.05f, 0f, 0f)
    assertEquals(0f, hero.fearSecondsRemaining, 0f)
    mission.courier.hp = 0
    engine.step(0.05f, 0f, 0f)
    assertEquals(CourierOutcome.FAILED, mission.outcome)
    assertEquals(MissionStage.DEFEAT, engine.stage)
  }

  @Test fun authoredEscortRouteHasCollisionClearanceWithoutChangingNavigation() {
    val engine = battle()
    val points = listOf(600f to 1049f) + engine.lostCourier!!.waypoints
    for ((from, to) in points.zipWithNext()) {
      val samples = (hypot(to.first - from.first, to.second - from.second) / 8f).toInt() + 1
      for (index in 0..samples) {
        val fraction = index.toFloat() / samples
        val x = from.first + (to.first - from.first) * fraction
        val y = from.second + (to.second - from.second) * fraction
        assertFalse("Courier route blocked at ($x,$y)",
          CollisionPhysics.wouldCollide(x, y, 18f, engine.mapLayout.obstacles))
      }
    }
  }
}
