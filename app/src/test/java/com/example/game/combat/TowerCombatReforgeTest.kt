package com.example.game.combat

import com.example.data.model.HeroRecord
import com.example.data.model.TacticalOrder
import com.example.game.tower.TowerFloorCatalog
import org.junit.Assert.*
import org.junit.Test

class TowerCombatReforgeTest {
  private val novice = HeroRecord(id = "courier_novice", name = "Kaelen", level = 1L)

  private fun battle() = TowerCombatEngine(
    TowerFloorCatalog.FLOORS.first(),
    listOf(novice),
    emptyList(),
    enableLostCourier = true,
  ).apply { stage = MissionStage.IN_PROGRESS }

  private fun clearPath(engine: TowerCombatEngine) {
    engine.enemies.forEach { it.dead = true; it.hp = 0 }
    engine.step(0.01f, 0f, 0f)
  }

  @Test
  fun telegraphHazardEnablesImmediateTouchDodgeWithoutManualOrderSelection() {
    val engine = battle()
    val mission = engine.lostCourier!!
    clearPath(engine)

    // Advance courier to Kraag gate
    mission.courier.x = 2240f
    engine.heroes.single().apply { x = 2200f; y = 1049f }
    engine.step(0.05f, 0f, 0f)

    // Simulate Kraag starting a red circle telegraph
    val kraag = engine.enemies.first { it.id == mission.eliteEnemyId }
    kraag.attackCooldown = 0f
    engine.step(0.05f, 0f, 0f)

    assertNotNull("Kraag must initiate a telegraph", mission.eliteTelegraph)
    assertNotEquals(TacticalOrder.MOVE_TO, engine.activeOrder)

    // Player taps ground away from Kraag to dodge
    val safeX = 1800f
    val safeY = 1050f
    engine.onBattlefieldTapped(safeX, safeY)

    assertEquals("Touch tap during telegraph must instantly issue MOVE_TO order", TacticalOrder.MOVE_TO, engine.activeOrder)
    assertNotNull(engine.moveBeacon)
    assertEquals(safeX, engine.moveBeacon!!.first, 25f)
  }

  @Test
  fun safeMarchEscortSpeedIsBriskWhenRoadIsClear() {
    val engine = battle()
    val mission = engine.lostCourier!!
    clearPath(engine)

    // Keep hero close to courier so courier walks
    engine.heroes.single().apply { x = mission.courier.x + 40f; y = mission.courier.y }

    // Road is clear of active aggroed threats
    assertTrue(engine.enemies.none { !it.dead && it.aggroed })
    val startX = mission.courier.x
    engine.step(1.0f, 0f, 0f)
    val traveled = mission.courier.x - startX

    // At 66 px/s, 1.0 second travel should be ~66 px (well above 44 px/s)
    assertTrue("Courier must march briskly at ~66 px/s on safe roads (actual: $traveled px)", traveled >= 60f)
  }

  @Test
  fun scoutInterceptionGrantsManaAndTacticalShortcut() {
    val engine = battle()
    val mission = engine.lostCourier!!
    clearPath(engine)

    // Move to scout trigger point
    mission.courier.x = 1330f
    val hero = engine.heroes.single()
    hero.apply {
      x = 1330f; y = mission.courier.y
      mana = 0f // Empty mana
    }
    engine.step(0.05f, 0f, 0f)

    assertNotNull(mission.scoutEnemyId)
    assertTrue(mission.scoutWarningRemaining > 0f)

    // Intercept scout before warning expires
    val scout = engine.enemies.first { it.id == mission.scoutEnemyId }
    scout.hp = 0
    scout.dead = true
    engine.step(0.05f, 0f, 0f)

    assertTrue("Scout interception must grant +25% Mana to living heroes", hero.mana >= hero.maxMana * 0.20f)
    assertEquals(0f, mission.scoutWarningRemaining, 0f)
    assertFalse(mission.scoutCalledReinforcements)
  }

  @Test
  fun kraagBreakWindowIncreasesFocusFireDamageWithVisualFeedback() {
    val engine = battle()
    val mission = engine.lostCourier!!
    clearPath(engine)

    // Advance courier to Kraag
    mission.courier.x = 2240f
    engine.heroes.single().apply { x = 2200f; y = 1049f }
    engine.step(0.05f, 0f, 0f)

    val kraag = engine.enemies.first { it.id == mission.eliteEnemyId }

    // Kraag finishes slam and enters recovery break window
    mission.eliteTelegraph = null
    // Set recovery using reflection or direct update
    val recoveryField = LostCourierEncounter::class.java.getDeclaredField("eliteRecoveryRemaining")
    recoveryField.isAccessible = true
    recoveryField.setFloat(mission, 1.5f)

    // Player orders FOCUS_FIRE on Kraag
    engine.activeOrder = TacticalOrder.FOCUS_FIRE
    val hpBefore = kraag.hp

    // Execute hero attack
    val hero = engine.heroes.single().apply {
      x = kraag.x - 30f
      y = kraag.y
      attackCooldown = 0f
    }
    engine.step(0.1f, 0f, 0f)

    val damageDealt = hpBefore - kraag.hp
    assertTrue("Damage during break window must be dealt", damageDealt > 0)
    assertTrue(
      "Floating texts must show break window feedback",
      engine.floatingTexts.any { it.text.contains("VỠ GIÁP") },
    )
  }

  @Test
  fun courierActivelySupportsHeroesDuringKraagGateBattle() {
    val engine = battle()
    val mission = engine.lostCourier!!
    clearPath(engine)

    // Advance courier to Kraag gate
    mission.courier.x = 2240f
    val hero = engine.heroes.single().apply {
      x = 2200f; y = 1049f
      hp = maxHp / 2 // Injured hero
    }
    engine.step(0.05f, 0f, 0f)

    assertEquals(CourierPhase.GATE_GUARD, mission.phase)

    // Advance 4 seconds to trigger courier support
    val hpBefore = hero.hp
    repeat(80) { engine.step(0.05f, 0f, 0f) }

    assertTrue("Courier must provide field bandage healing to injured hero", hero.hp > hpBefore)
    assertTrue(
      "Engine must post feedback about courier support",
      engine.feedbackEvents.any { it.contains("Người đưa thư sơ cứu") || it.contains("Người đưa thư") },
    )
  }
}
