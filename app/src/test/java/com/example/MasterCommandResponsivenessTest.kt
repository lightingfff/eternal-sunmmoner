package com.example

import com.example.data.model.*
import com.example.game.ai.AutonomousHeroAi
import com.example.game.combat.*
import com.example.game.map.*
import com.example.game.tower.*
import org.junit.Assert.*
import org.junit.Test

class MasterCommandResponsivenessTest {
  private val layout = GeneratedMapLayout(2400f,1800f,40f,BiomeTheme.FIRST_FIELD_MEADOW,emptyList(),emptyList(),emptyList())
  private fun hero(premium: Boolean = true, type: HeroClassType = HeroClassType.KNIGHT) = HeroCombatUnit(
    "h",if(premium) "Kaelen" else "Ordinary", "",type,PersonalityType.STOIC,
    400,200,42,24,110f,1,1100f,1000f,1100f,1000f,0).apply { attackCooldown=100f; skillCooldown=100f }
  private fun enemy() = EnemyCombatUnit("e",EnemyArchetype.FIELD_GOBLIN_RAIDER,155,155,22,6,92f,1140f,1000f)
  private fun tick(h: HeroCombatUnit, e: EnemyCombatUnit, order: TacticalOrder, hit: ()->Unit = {}) {
    AutonomousHeroAi.updateHeroUnit(h,1f/60f,listOf(h),listOf(e),600f,1000f,order,e.id,
      650f to 1000f,layout,{}, {}, {}, {_,_,_->hit()})
  }
  @Test fun regroupInterruptsWindupOnNextTickForBasicAndSkillWithoutLateHits() {
    for (premium in listOf(true,false)) for (skill in listOf(false,true)) {
      val h=hero(premium); val e=enemy(); var hits=0
      h.contactAttack.start(e.id,skill)
      h.contactAttack.advance(.10f)
      val x=h.x
      tick(h,e,TacticalOrder.REGROUP) { hits++ }
      assertFalse("REGROUP must cancel pending contact, premium=$premium skill=$skill",h.contactAttack.active)
      assertTrue("Movement must start next fixed tick",h.x<x)
      repeat(60) { tick(h,e,TacticalOrder.REGROUP) { hits++ } }
      assertEquals("Canceled windup must never hit",0,hits)
      assertEquals("Canceled skill must never grant its shield",0,h.shieldHp)
    }
  }
  @Test fun retreatAndMoveToCancelContactAndBeginMovementOnNextTick() {
    for(order in listOf(TacticalOrder.MOVE_TO,TacticalOrder.RETREAT)) for(premium in listOf(true,false)) {
      val h=hero(premium);val e=enemy();h.contactAttack.start(e.id)
      tick(h,e,order)
      assertFalse("$order leaves no pending hit",h.contactAttack.active)
      assertTrue(h.isMoving && h.x<1100f)
    }
  }
  @Test fun regroupAfterContactKeepsTheHitButNeverEmitsItAgain() {
    for(skill in listOf(false,true)) {
      val h=hero();val e=enemy();var hits=0
      h.contactAttack.start(e.id,skill)
      repeat(if(skill) 19 else 14) { tick(h,e,TacticalOrder.HOLD_POSITION) { hits++ } }
      assertEquals(1,hits)
      val shield=h.shieldHp
      tick(h,e,TacticalOrder.REGROUP) { hits++ }
      repeat(60) { tick(h,e,TacticalOrder.REGROUP) { hits++ } }
      assertEquals(1,hits);assertEquals(shield,h.shieldHp)
      assertFalse(h.contactAttack.active)
    }
  }
  @Test fun focusProtectAndHoldPreserveSingleContactDamage() {
    for(order in listOf(TacticalOrder.FOCUS_FIRE,TacticalOrder.PROTECT_ALLY,TacticalOrder.HOLD_POSITION)) {
      val h=hero(); val e=enemy();var hits=0;h.contactAttack.start(e.id)
      repeat(35) { tick(h,e,order) { hits++ } }
      assertEquals("$order must not cancel a normal attack",1,hits)
    }
  }
  @Test fun ordinaryHeroObeysRegroupWithoutAnAttackTimeline() {
    val h=hero(false);tick(h,enemy(),TacticalOrder.REGROUP)
    assertTrue(h.isMoving && h.x<1100f)
  }
  @Test fun urgentMovementTakesPriorityOverAutomaticEmergencyHealing() {
    for(order in listOf(TacticalOrder.REGROUP,TacticalOrder.RETREAT)) {
      val h=hero(false,HeroClassType.PRIEST);h.skillCooldown=0f;h.hp=100
      tick(h,enemy(),order)
      assertTrue("$order must move even when an emergency heal is ready",h.isMoving && h.x<1100f)
      assertEquals("The heal is not spent while leaving danger",0f,h.skillCooldown,0f)
    }
  }
  @Test fun engineAcceptsRegroupImmediatelyAndExecutesAtTheNextFixedTick() {
    val spec=TowerFloorCatalog.FLOORS.first().copy(eventTriggerX=Float.MAX_VALUE,
      enemies=listOf(EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_RAIDER,940f,1100f)))
    val engine=TowerCombatEngine(spec,listOf(HeroRecord(id="live",name="Kaelen")),emptyList())
    engine.stage=MissionStage.IN_PROGRESS
    val h=engine.heroes.single();h.x=900f;h.y=1100f;h.attackCooldown=100f;h.skillCooldown=100f
    h.contactAttack.start(engine.enemies.single().id)
    engine.issueTacticalOrder(TacticalOrder.REGROUP)
    assertEquals(TacticalOrder.REGROUP,engine.activeOrder)
    assertEquals("No simulation tick has elapsed yet",900f,h.x,0f)
    engine.step(1f/60f,0f,0f)
    assertFalse(h.contactAttack.active)
    assertTrue(h.isMoving && h.x<900f)
    assertTrue("Spent cooldown remains spent",h.skillCooldown>99f)
  }

  @Test fun regroupAndMoveToAllowAttacksToFinishAfterArrival() {
    for(order in listOf(TacticalOrder.REGROUP,TacticalOrder.MOVE_TO)) {
      val h=hero();val reference=if(order==TacticalOrder.REGROUP) 600f else 650f
      val slot=AutonomousHeroAi.formationSlotAround(reference,1000f,h)
      h.x=slot.first;h.y=slot.second;h.attackCooldown=0f
      val e=enemy().apply { x=h.x+40f;y=h.y };var hits=0
      repeat(35) { tick(h,e,order) { hits++ } }
      assertEquals("$order must not cancel every attack after reaching the slot",1,hits)
    }
  }

  @Test fun urgentCancelPreservesRealAttackCostsAcrossWindupContactAndRecovery() {
    val dt = 1f / 60f
    for (skill in listOf(false, true)) for (phase in listOf("windup", "contact", "recovery")) {
      for (order in listOf(TacticalOrder.REGROUP, TacticalOrder.RETREAT, TacticalOrder.MOVE_TO)) {
        val h = hero(); val e = enemy(); var hits = 0
        h.attackCooldown = 0f
        h.skillCooldown = if (skill) 0f else 100f
        // Start through the real AI, so cooldowns are charged by executeCombatActions.
        tick(h, e, TacticalOrder.HOLD_POSITION) { hits++ }
        assertTrue(h.contactAttack.active)
        assertEquals(skill, h.contactAttack.skill)
        val beforeContact = if (skill) 17 else 13
        repeat(beforeContact) { tick(h, e, TacticalOrder.HOLD_POSITION) { hits++ } }
        if (phase != "windup") tick(h, e, TacticalOrder.HOLD_POSITION) { hits++ }
        if (phase == "recovery") repeat(3) { tick(h, e, TacticalOrder.HOLD_POSITION) { hits++ } }
        val expectedHits = if (phase == "windup") 0 else 1
        assertEquals("$skill/$phase before cancel", expectedHits, hits)
        val attackCost = h.attackCooldown; val skillCost = h.skillCooldown; val shield = h.shieldHp
        tick(h, e, order) { hits++ }
        assertFalse(h.contactAttack.active)
        assertTrue(h.isMoving)
        assertEquals("Cancel must not refund or recharge basic cooldown", (attackCost-dt).coerceAtLeast(0f), h.attackCooldown, .0001f)
        assertEquals("Cancel must not refund or recharge skill cooldown", (skillCost-dt).coerceAtLeast(0f), h.skillCooldown, .0001f)
        assertEquals("No delayed or duplicate contact", expectedHits, hits)
        assertEquals("Already delivered shield survives", shield, h.shieldHp)
        // No targets after departure: let the old contact deadline pass without a new attack.
        e.hp = 0
        repeat(45) { tick(h, e, order) { hits++ } }
        assertEquals(expectedHits, hits)
      }
    }
  }

}
