package com.example

import com.example.data.model.HeroRecord
import com.example.data.model.TacticalOrder
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.tower.TowerFloorCatalog
import kotlin.math.hypot
import org.junit.Assert.*
import org.junit.Test

/** Real command/AI integration, with deterministic encounter positions rather than render mocks. */
class RescueTacticalCommandsTest {
  private fun battle(escort: Boolean = true): TowerCombatEngine = TowerCombatEngine(
    TowerFloorCatalog.FLOORS.first(),
    listOf(HeroRecord(id = "hero_starter_kaelen", name = "Kaelen", maxHp = 480, attack = 42, defense = 32)),
    emptyList(), enableLostCourier = escort,
  ).apply { stage = MissionStage.IN_PROGRESS }

  @Test fun retreatReturnsToExtractionEvenAfterCommandSigilMovesAway() {
    for (escort in listOf(true, false)) {
      val engine = battle(escort)
      // The player has moved the external command sigil deep into the level.
      engine.masterX = 1500f
      engine.masterY = 1100f
      val hero = engine.heroes.single()
      hero.x = 700f
      hero.y = 1100f
      engine.enemies.forEach { it.dead = true; it.hp = 0 }
      // Ordinary floors need evidence that combat started before accepting retreat.
      engine.issueTacticalOrder(TacticalOrder.RETREAT)
      repeat(1200) { engine.step(1f / 60f, 0f, 0f) }
      assertTrue("Retreat must complete independently of the movable sigil (escort=$escort)", engine.retreatCompleted)
      assertEquals(MissionStage.DEFEAT, engine.stage)
      assertTrue(hypot(hero.x - engine.floorSpec.spawnX, hero.y - engine.floorSpec.spawnY) <= 185f)
      assertEquals(1500f, engine.masterX, 0f)
    }
  }

  @Test fun regroupUsesCourierOnEscortAndSigilOnOrdinaryFloors() {
    for (escort in listOf(true, false)) {
      val engine = battle(escort)
      engine.masterX = 300f
      engine.masterY = 1100f
      engine.lostCourier?.courier?.apply { x = 1100f; y = 1100f }
      val hero = engine.heroes.single()
      hero.x = 900f
      hero.y = 1100f
      engine.issueTacticalOrder(TacticalOrder.REGROUP)
      val before = hero.x
      engine.step(.05f, 0f, 0f)
      if (escort) assertTrue("Escort regroup must approach the courier", hero.x > before)
      else assertTrue("Ordinary regroup must still approach the sigil", hero.x < before)
    }
  }

  @Test fun urgentMovementCancelsPendingContactWithoutDeliveringStaleDamage() {
    for (order in listOf(TacticalOrder.REGROUP, TacticalOrder.MOVE_TO, TacticalOrder.RETREAT)) {
      val engine = battle()
      val hero = engine.heroes.single()
      val enemy = engine.enemies.first()
      hero.x = 1000f; hero.y = 1100f
      hero.attackCooldown = 100f; hero.skillCooldown = 100f
      enemy.x = hero.x + 30f; enemy.y = hero.y; enemy.attackCooldown = 100f
      val hp = enemy.hp
      hero.contactAttack.start(enemy.id, skill = true)
      engine.issueTacticalOrder(order)
      if (order == TacticalOrder.MOVE_TO) engine.onBattlefieldTapped(700f, 1100f)
      engine.step(.31f, 0f, 0f)
      assertFalse("$order must cancel pending contact", hero.contactAttack.active)
      assertEquals("$order must not deliver canceled skill damage", hp, enemy.hp)
      assertEquals(0, hero.shieldHp)
      assertTrue("$order must start moving in the same tick", hero.x < 1000f)
    }
  }

  @Test fun moveOrderEvadesCaptainTelegraphDuringHeroWindup() {
    val engine = battle()
    val mission = engine.lostCourier!!
    engine.enemies.forEach { it.dead = true; it.hp = 0 }
    mission.courier.x = 2250f; mission.courier.y = 1080f
    val hero = engine.heroes.single()
    hero.x = 2460f; hero.y = 1050f
    hero.attackCooldown = 100f; hero.skillCooldown = 100f
    engine.step(.01f, 0f, 0f)
    val captain = engine.enemies.first { it.id == mission.eliteEnemyId }
    engine.enemies.filter { it !== captain }.forEach { it.dead = true; it.hp = 0 }
    captain.attackCooldown = 0f
    engine.step(.01f, 0f, 0f)
    val warning = mission.eliteTelegraph!!
    val hp = hero.hp
    hero.contactAttack.start(captain.id, skill = true)
    engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
    engine.onBattlefieldTapped(warning.x - 260f, warning.y)
    repeat(92) { engine.step(1f / 60f, 0f, 0f) }
    assertFalse(hero.contactAttack.active)
    assertTrue("Captain strike should enter its counter window", mission.eliteRecoveryRemaining > 0f)
    assertTrue(hypot(hero.x - warning.x, hero.y - warning.y) > warning.radius)
    assertEquals("A correctly executed dodge avoids captain damage", hp, hero.hp)
  }
}
