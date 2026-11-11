package com.example

import com.example.data.model.CourierOutcome
import com.example.data.model.HeroRecord
import com.example.data.model.TacticalOrder
import com.example.game.combat.CourierChoice
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.map.CollisionPhysics
import com.example.game.tower.TowerFloorCatalog
import kotlin.math.hypot
import org.junit.Assert.*
import org.junit.Test

/** Isolated spatial/outcome fixtures; forced defeated enemies and repositioning are not playthrough evidence. */
class CourierSupplyDiversionTest {
  private fun atChoice(): TowerCombatEngine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(),
    listOf(HeroRecord(id = "supply_fixture", name = "Kaelen")), emptyList(), enableLostCourier = true).apply {
    stage = MissionStage.IN_PROGRESS
    enemies.forEach { it.hp = 0; it.dead = true }
    step(.01f, 0f, 0f)
    lostCourier!!.courier.apply { x = 1101f; y = 1130f }
    heroes.first().apply { x = 1101f; y = 1130f }
    issueTacticalOrder(TacticalOrder.HOLD_POSITION)
  }

  private fun finishEscort(engine: TowerCombatEngine) {
    val courier = engine.lostCourier!!.courier
    repeat(1800) {
      if (engine.lostCourier!!.outcome != null) return
      engine.enemies.forEach { it.hp = 0; it.dead = true }
      engine.heroes.first().apply { x = courier.x + 70f; y = courier.y }
      engine.step(.05f, 0f, 0f)
    }
    fail("Spatial escort fixture must reach extraction")
  }

  @Test fun supplyCommitmentRequiresLeavingEscortRoadAndExplicitPhysicalPickup() {
    val engine = atChoice()
    val mission = engine.lostCourier!!
    assertTrue(mission.chooseChoice(CourierChoice.RECOVER_SUPPLIES))
    assertTrue(hypot(engine.heroes.first().x - mission.supplyX, engine.heroes.first().y - mission.supplyY) > 600f)
    assertFalse(mission.interactSupplies())
    repeat(30) { engine.step(.05f, 0f, 0f) }
    assertFalse(mission.suppliesRecovered)
    engine.heroes.first().apply { x = mission.supplyX; y = mission.supplyY }
    assertFalse("Arrival alone must not claim the bag", mission.suppliesRecovered)
    assertTrue(mission.interactSupplies())
    assertFalse(mission.interactSupplies())
    finishEscort(engine)
    assertEquals(CourierOutcome.SUPPLIES_RECOVERED, mission.outcome)
  }

  @Test fun choosingSuppliesButExtractingWithoutPickupNeverBecomesSupportCompletion() {
    val engine = atChoice()
    val mission = engine.lostCourier!!
    assertTrue(mission.chooseChoice(CourierChoice.RECOVER_SUPPLIES))
    finishEscort(engine)
    assertFalse(mission.suppliesRecovered)
    assertEquals(CourierOutcome.RESCUED_WITHOUT_SUPPLIES, mission.outcome)
  }

  @Test fun explicitSupportRemainsASeparateSuccessfulOutcome() {
    val engine = atChoice()
    assertTrue(engine.lostCourier!!.chooseChoice(CourierChoice.SUPPORT_COURIER))
    finishEscort(engine)
    assertEquals(CourierOutcome.RESCUED, engine.lostCourier!!.outcome)
    assertFalse(engine.lostCourier!!.suppliesRecovered)
  }

  @Test fun supplyTrailAndReturnUseExistingCollisionClearCorridors() {
    val engine = atChoice()
    val mission = engine.lostCourier!!
    val trail = listOf(1180f to 1125f, 1480f to 1520f, mission.supplyX to mission.supplyY,
      1480f to 1520f, 1180f to 1125f)
    for ((from, to) in trail.zipWithNext()) {
      val samples = (hypot(to.first - from.first, to.second - from.second) / 8f).toInt() + 1
      for (index in 0..samples) {
        val fraction = index.toFloat() / samples
        val x = from.first + (to.first - from.first) * fraction
        val y = from.second + (to.second - from.second) * fraction
        assertFalse("Supply approach blocked at ($x,$y)", CollisionPhysics.wouldCollide(x, y, 18f, engine.mapLayout.obstacles))
      }
    }
  }
}
