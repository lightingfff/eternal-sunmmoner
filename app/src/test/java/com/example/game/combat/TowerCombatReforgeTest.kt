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

  /** Isolated gate snapshot; unmodified starter-party routes are tested separately. */
  private fun gate(): Pair<TowerCombatEngine, EnemyCombatUnit> {
    val engine = battle()
    clearPath(engine)
    engine.lostCourier!!.courier.x = 2240f
    engine.heroes.single().apply { x = 2200f; y = 1049f }
    engine.step(.05f, 0f, 0f)
    val guardian = engine.enemies.first { it.id == engine.lostCourier!!.eliteEnemyId }
    engine.enemies.filter { it !== guardian }.forEach { it.hp = 0; it.dead = true }
    return engine to guardian
  }

  @Test
  fun telegraphHazardEnablesImmediateTouchDodgeWithoutManualOrderSelection() {
    val (engine, kraag) = gate()
    val mission = engine.lostCourier!!
    engine.heroes.single().apply { x = kraag.x - 80f; y = kraag.y; attackCooldown = 999f }
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
  fun courierDoesNotUseBriskProtectedMarchWhenTheMasterDivertsTheParty() {
    val engine = battle()
    val mission = engine.lostCourier!!
    clearPath(engine)
    engine.heroes.single().apply { x = mission.courier.x + 40f; y = mission.courier.y }
    engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
    val start = mission.courier.x
    engine.step(1f, 0f, 0f)
    val distance = mission.courier.x - start
    assertTrue("An unprotected diversion must retain the original44px/s pace: $distance", distance in 40f..48f)
  }

  @Test
  fun scoutInterceptionGrantsOneReadinessShortcutAndPreventsReinforcements() {
    val engine = battle()
    val mission = engine.lostCourier!!
    clearPath(engine)

    // Move to scout trigger point
    mission.courier.x = 1330f
    val hero = engine.heroes.single()
    hero.apply {
      x = 1330f; y = mission.courier.y
      skillCooldown = maxSkillCooldown
    }
    engine.step(0.05f, 0f, 0f)

    assertNotNull(mission.scoutEnemyId)
    assertTrue(mission.scoutWarningRemaining > 0f)

    // Intercept scout before warning expires
    val scout = engine.enemies.first { it.id == mission.scoutEnemyId }
    val cooldownBefore = hero.skillCooldown
    scout.hp = 0
    scout.dead = true
    engine.step(0.05f, 0f, 0f)

    val expected = (cooldownBefore - .05f - hero.maxSkillCooldown * .25f).coerceAtLeast(0f)
    assertEquals("Shortcut uses the existing skill-readiness resource", expected, hero.skillCooldown, .001f)
    assertEquals(0f, mission.scoutWarningRemaining, 0f)
    assertFalse(mission.scoutCalledReinforcements)
    engine.step(.05f, 0f, 0f)
    assertEquals("Repeated observations cannot grant the shortcut again", (expected - .05f).coerceAtLeast(0f), hero.skillCooldown, .001f)
  }

  @Test
  fun kraagBreakWindowIncreasesFocusFireDamageWithVisualFeedback() {
    val (engine, kraag) = gate()
    val mission = engine.lostCourier!!

    // Kraag finishes slam and enters recovery break window
    assertNull("This recovery fixture starts before a slam is scheduled", mission.eliteTelegraph)
    // Set recovery using reflection or direct update
    val recoveryField = LostCourierEncounter::class.java.getDeclaredField("eliteRecoveryRemaining")
    recoveryField.isAccessible = true
    recoveryField.setFloat(mission, 1.5f)

    // Player orders FOCUS_FIRE on Kraag
    engine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE)
    engine.onBattlefieldTapped(kraag.x, kraag.y)
    val hpBefore = kraag.hp

    // Execute hero attack
    val hero = engine.heroes.single().apply {
      x = kraag.x - 30f
      y = kraag.y
      attackCooldown = 0f
      skillCooldown = 999f
    }
    kraag.attackCooldown = 999f
    repeat(24) { engine.step(1f / 60f, 0f, 0f) }

    val damageDealt = hpBefore - kraag.hp
    assertTrue("Damage during break window must be dealt", damageDealt > 0)
    assertTrue(
      "Floating texts must show break window feedback",
      engine.floatingTexts.any { it.text.contains("VỠ GIÁP") },
    )
  }

  @Test
  fun courierActivelySupportsHeroesDuringKraagGateBattle() {
    val (engine, kraag) = gate()
    val mission = engine.lostCourier!!
    val hero = engine.heroes.single().apply {
      hp = maxHp / 2 // Injured hero
    }

    assertEquals(CourierPhase.GATE_GUARD, mission.phase)
    kraag.attackCooldown = 999f
    hero.attackCooldown = 999f
    hero.skillCooldown = 999f

    // Advance 4 seconds to trigger courier support
    val hpBefore = hero.hp
    repeat(80) { engine.step(0.05f, 0f, 0f) }

    assertTrue("Courier must provide field bandage healing to injured hero", hero.hp > hpBefore)
    assertTrue(
      "Engine must post feedback about courier support",
      engine.commandFeedback?.contains("Người đưa thư sơ cứu") == true,
    )
  }

  @Test
  fun finalCourierStoneMarksGuardianDeadClearsTargetAndAllowsExtraction() {
    val (engine, kraag) = gate()
    val mission = engine.lostCourier!!
    val hero = engine.heroes.single()
    // Spatial final-hit fixture, not an unmodified full playthrough.
    kraag.hp = 12
    kraag.attackCooldown = 999f
    hero.hp = hero.maxHp
    hero.attackCooldown = 999f
    hero.skillCooldown = 999f
    engine.onBattlefieldTapped(kraag.x, kraag.y)
    assertEquals(kraag.id, engine.designatedTargetId)
    val support = LostCourierEncounter::class.java.getDeclaredField("courierSupportCooldown")
    support.isAccessible = true
    support.setFloat(mission, 0f)
    engine.step(.05f, 0f, 0f)
    assertEquals(0, kraag.hp)
    assertTrue("A support finishing hit must complete normal enemy death", kraag.dead)
    assertNull(engine.designatedTargetId)
    assertNull(mission.eliteTelegraph)
    assertEquals(0f, mission.eliteRecoveryRemaining, 0f)
    engine.chooseCourierOrder(CourierOrder.PROTECT)
    repeat(3_600) { if (engine.stage != MissionStage.VICTORY) engine.step(.05f, 0f, 0f) }
    assertEquals("Dead guardian must no longer block the actual exit", MissionStage.VICTORY, engine.stage)
  }
}
