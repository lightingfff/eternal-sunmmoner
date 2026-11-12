package com.example

import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.TacticalOrder
import com.example.game.combat.CombatFxStyle
import com.example.game.combat.EnemyCombatUnit
import com.example.game.combat.MissionStage
import com.example.game.combat.ProjectileVisual
import com.example.game.combat.TowerCombatEngine
import com.example.game.map.EnemyArchetype
import com.example.game.tower.TowerFloorCatalog
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

/** Control ownership and real execution events. Arranged contact cases are spatial fixtures, not playthroughs. */
class TowerTrustAndFeedbackTest {
  private fun battle(living: Boolean = true, heroClass: HeroClassType = HeroClassType.KNIGHT,
    name: String = "Kaelen") = TowerCombatEngine(
    TowerFloorCatalog.FLOORS.first(),
    listOf(HeroRecord(id="trust",name=name,heroClass=heroClass.name,maxHp=480,
      attack=42,defense=32,speed=110)),
    emptyList(),enableLostCourier=true,random=Random(99),enableLivingExploration=living,
  ).apply { stage=MissionStage.IN_PROGRESS }

  @Test fun inspectionAndEmptyTapLeaveAutonomousNavigationIdenticalToUntouchedTwin() {
    val observed=battle();val untouched=battle()
    repeat(60) { observed.step(1f/60,0f,0f);untouched.step(1f/60,0f,0f) }
    val hero=observed.heroes.single()
    observed.onBattlefieldTapped(hero.x,hero.y)
    assertEquals(hero.id,observed.selectedHeroId)
    observed.onBattlefieldTapped(1f,1f)
    assertTrue(observed.lostCourier!!.livingExpedition!!.automationEnabled)
    repeat(180) { observed.step(1f/60,0f,0f);untouched.step(1f/60,0f,0f) }
    assertEquals(untouched.heroes.single().x,hero.x,.001f)
    assertEquals(untouched.heroes.single().y,hero.y,.001f)
    assertEquals(untouched.heroes.single().aiIntentLabelVi,hero.aiIntentLabelVi)
  }

  @Test fun inspectionCannotResumeOrReplaceAnIntentionalManualHold() {
    val engine=battle()
    engine.issueTacticalOrder(TacticalOrder.HOLD_POSITION)
    val hero=engine.heroes.single()
    val point=hero.x to hero.y
    engine.onBattlefieldTapped(hero.x,hero.y)
    engine.onBattlefieldTapped(1f,1f)
    repeat(60) { engine.step(1f/60,0f,0f) }
    assertFalse(engine.lostCourier!!.livingExpedition!!.automationEnabled)
    assertEquals(TacticalOrder.HOLD_POSITION,engine.activeOrder)
    assertEquals(point.first,hero.x,.001f)
    assertEquals(point.second,hero.y,.001f)
  }

  @Test fun armedDestinationHasPriorityOverInspectAndRetainsManualOwnership() {
    val engine=battle();val hero=engine.heroes.single()
    val selected=engine.selectedHeroId
    engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
    engine.onBattlefieldTapped(hero.x,hero.y)
    assertFalse(engine.awaitingMoveToTap)
    assertNotNull(engine.moveBeacon)
    assertEquals("Armed movement must preserve selection rather than inspect",selected,engine.selectedHeroId)
    assertFalse(engine.lostCourier!!.livingExpedition!!.automationEnabled)
    engine.resumeLivingExploration("north")
    assertTrue(engine.lostCourier!!.livingExpedition!!.automationEnabled)
  }

  @Test fun overlappingMeleeHitboxesStillDistinguishInspectionFromEnemyFocus() {
    val engine=battle();val hero=engine.heroes.single()
    hero.x=1000f;hero.y=1100f
    val enemy=EnemyCombatUnit("overlap",EnemyArchetype.FIELD_GOBLIN_RAIDER,100,100,0,0,0f,1030f,1100f)
    engine.enemies += enemy
    engine.step(.001f,0f,0f)
    assertTrue(engine.isEnemyRevealed(enemy))
    engine.onBattlefieldTapped(hero.x,hero.y)
    assertTrue("Exact hero tap next to a revealed enemy must remain inspection",
      engine.lostCourier!!.livingExpedition!!.automationEnabled)
    engine.onBattlefieldTapped(enemy.x,enemy.y)
    assertEquals(enemy.id,engine.designatedTargetId)
    assertFalse("Exact enemy tap must still issue a real focus command",
      engine.lostCourier!!.livingExpedition!!.automationEnabled)
  }

  @Test fun contactFeedbackExistsOnlyAfterRealDamageAndCanceledSkillHasNoGuardEffect() {
    val engine=battle(living=false);val hero=engine.heroes.single();val enemy=engine.enemies.first()
    hero.x=1000f;hero.y=1100f;hero.attackCooldown=100f;hero.skillCooldown=100f
    enemy.x=1030f;enemy.y=1100f;enemy.attackCooldown=100f
    val hp=enemy.hp
    hero.contactAttack.start(enemy.id)
    engine.step(.20f,0f,0f)
    assertEquals(hp,enemy.hp)
    assertFalse(engine.aoeEffects.any { it.style==CombatFxStyle.IMPACT && it.sourceId==hero.id })
    engine.step(.03f,0f,0f)
    assertTrue(enemy.hp<hp)
    assertEquals(1,engine.aoeEffects.count { it.style==CombatFxStyle.IMPACT && it.sourceId==hero.id })
    engine.step(.04f,0f,0f)
    assertEquals(1,engine.aoeEffects.count { it.style==CombatFxStyle.IMPACT && it.sourceId==hero.id })
    hero.contactAttack.start(enemy.id,skill=true)
    engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
    engine.onBattlefieldTapped(700f,1100f)
    engine.step(.31f,0f,0f)
    assertFalse(hero.contactAttack.active)
    assertEquals(0,hero.shieldHp)
    assertFalse(engine.aoeEffects.any { it.style==CombatFxStyle.GUARD })
  }

  @Test fun shieldSkillAppearsAtActualContactAndAbsorptionIsDistinctFromHpDamage() {
    val engine=battle(living=false);val hero=engine.heroes.single();val enemy=engine.enemies.first()
    hero.x=1000f;hero.y=1100f;hero.attackCooldown=100f;hero.skillCooldown=100f
    enemy.x=1030f;enemy.y=1100f;enemy.attackCooldown=100f
    hero.contactAttack.start(enemy.id,skill=true)
    engine.step(.29f,0f,0f)
    assertEquals(0,hero.shieldHp)
    assertFalse(engine.aoeEffects.any { it.style==CombatFxStyle.GUARD })
    engine.step(.02f,0f,0f)
    assertTrue(hero.shieldHp>0)
    assertEquals(1,engine.aoeEffects.count { it.style==CombatFxStyle.GUARD && it.sourceId==hero.id })
    val before=hero.hp
    engine.applyDamageToHero(hero,28)
    assertEquals(before,hero.hp)
    assertTrue(engine.aoeEffects.any { it.style==CombatFxStyle.SHIELD_IMPACT })
    repeat(90) { engine.step(1f/60,0f,0f) }
    assertFalse(hero.contactAttack.active)
    assertFalse(engine.aoeEffects.any { it.style==CombatFxStyle.GUARD })
  }

  @Test fun archerVolleyHasRealAttributedArrowsAndCannotDamageBeforeCollision() {
    val engine=battle(living=false,heroClass=HeroClassType.ARCHER,name="Lyra")
    val hero=engine.heroes.single();val target=engine.enemies.first()
    engine.enemies.filter { it!==target }.forEach { it.dead=true;it.hp=0 }
    hero.x=1000f;hero.y=1100f;hero.skillCooldown=0f
    target.x=1190f;target.y=1100f;target.attackCooldown=100f
    engine.issueTacticalOrder(TacticalOrder.HOLD_POSITION)
    val hp=target.hp
    engine.step(.01f,0f,0f)
    assertEquals(hp,target.hp)
    val arrows=engine.projectiles.filter { it.sourceId==hero.id }
    assertEquals(3,arrows.size)
    assertTrue(arrows.all { it.visual==ProjectileVisual.ARROW })
    assertEquals(3,arrows.map { it.vy }.distinct().size)
    repeat(40) { engine.step(1f/60,0f,0f) }
    assertTrue("Real travelling arrows must reach their target",target.hp<hp)
    assertTrue(engine.aoeEffects.any { it.style==CombatFxStyle.IMPACT && it.sourceId==hero.id } ||
      engine.floatingTexts.any { it.text.startsWith("-") })
  }
}
