package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.TacticalOrder
import com.example.game.combat.*
import com.example.game.map.EnemyArchetype
import com.example.game.tower.TowerFloorCatalog
import org.junit.Assert.*
import org.junit.Test

/** Explicitly isolated spatial combat fixtures, not end-to-end playthrough evidence. */
class LivingNpcCombatTest {
  private fun witnessBattle(): TowerCombatEngine {
    val engine=TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), listOf(
      HeroRecord(id="guardian",name="Kaelen",heroClass=HeroClassType.KNIGHT.name),
      HeroRecord(id="scout",name="Lyra",heroClass=HeroClassType.ARCHER.name)),emptyList(),
      enableLostCourier=true,enableLivingExploration=true)
    engine.stage=MissionStage.IN_PROGRESS
    engine.resumeLivingExploration("south")
    engine.heroes.forEach { it.x=1600f;it.y=1510f }
    engine.step(.01f,0f,0f)
    assertNotNull(engine.lostCourier!!.livingExpedition!!.witness)
    engine.issueTacticalOrder(TacticalOrder.HOLD_POSITION)
    engine.heroes.forEach { it.x=900f;it.y=1050f;it.anchorX=it.x;it.anchorY=it.y }
    engine.enemies.clear()
    return engine
  }

  private fun raider(engine: TowerCombatEngine): EnemyCombatUnit {
    val npc=engine.lostCourier!!.livingExpedition!!.witness!!
    val type=EnemyArchetype.FIELD_GOBLIN_RAIDER
    return EnemyCombatUnit("expedition_south_contact_fixture",type,type.baseHp,type.baseHp,
      type.baseAtk,type.baseDef,0f,npc.x-15f,npc.y).also { engine.enemies+=it }
  }

  @Test fun raiderContactHitsLivingWitnessAndRechecksImpactRange() {
    val engine=witnessBattle()
    val expedition=engine.lostCourier!!.livingExpedition!!
    val npc=expedition.witness!!
    val attacker=raider(engine)
    val before=npc.hp
    repeat(100) { if (npc.hp==before) engine.step(.02f,0f,0f) }
    assertTrue("Actual contact attack must hurt the living witness",npc.hp<before)
    assertTrue(attacker.aggroed)
    attacker.attackCooldown=0f
    attacker.contactAttack.cancel()
    engine.step(.01f,0f,0f)
    assertTrue(attacker.contactAttack.active)
    val after=npc.hp
    npc.x+=400f
    repeat(40) { engine.step(.02f,0f,0f) }
    assertEquals("A locked contact cannot hit an NPC who moved out of reach",after,npc.hp)
  }

  @Test fun actualProjectileCollisionHurtsWitnessButDeadBodyDoesNotAbsorbTheNextShot() {
    val engine=witnessBattle()
    val expedition=engine.lostCourier!!.livingExpedition!!
    val npc=expedition.witness!!
    fun shot(id:String)=CombatProjectile(id,false,npc.x,npc.y,0f,0f,13,Color.Red)
    val before=npc.hp
    engine.projectiles+=shot("living_shot")
    engine.step(.01f,0f,0f)
    assertEquals(before-13,npc.hp)
    assertTrue(engine.projectiles.none { it.id=="living_shot" })
    expedition.damageWitness(Int.MAX_VALUE)
    engine.projectiles+=shot("after_death")
    engine.step(.01f,0f,0f)
    assertFalse(npc.alive)
    assertTrue("A dead witness must not intercept subsequent shots",engine.projectiles.any { it.id=="after_death" })
  }

  @Test fun nearbyGuardianDrawsMeleeAttackAndCanPhysicallyInterceptAnArrow() {
    val engine=witnessBattle()
    val npc=engine.lostCourier!!.livingExpedition!!.witness!!
    val guardian=engine.heroes.first()
    guardian.x=npc.x-25f;guardian.y=npc.y
    guardian.anchorX=guardian.x;guardian.anchorY=guardian.y
    raider(engine)
    val hp=npc.hp
    val heroHp=guardian.hp
    repeat(100) { if (guardian.hp==heroHp) engine.step(.02f,0f,0f) }
    assertEquals("Close guardian must draw melee intent off the witness",hp,npc.hp)
    assertTrue("Protection must cost actual guardian HP",guardian.hp<heroHp)
    engine.enemies.clear()
    val afterContact=guardian.hp
    engine.projectiles+=CombatProjectile("blocked_arrow",false,guardian.x,guardian.y,0f,0f,100,Color.Red)
    engine.step(.01f,0f,0f)
    assertTrue(guardian.hp<afterContact)
    assertEquals(hp,npc.hp)
  }
  @Test fun rangedEnemyCanAimAtWitnessEvenWithAGuardianNearby() {
    val engine=witnessBattle()
    val npc=engine.lostCourier!!.livingExpedition!!.witness!!
    val guardian=engine.heroes.first()
    guardian.x=npc.x-130f;guardian.y=npc.y
    guardian.anchorX=guardian.x;guardian.anchorY=guardian.y
    val type=EnemyArchetype.FIELD_GOBLIN_ARCHER
    val archer=EnemyCombatUnit("expedition_south_ranged_fixture",type,type.baseHp,type.baseHp,
      type.baseAtk,type.baseDef,0f,npc.x+180f,npc.y)
    archer.attackCooldown=0f
    engine.enemies+=archer
    val before=npc.hp
    val heroHp=guardian.hp
    engine.step(.01f,0f,0f)
    assertTrue("A distant archer must see the witness as a valid target",engine.projectiles.any { !it.fromHero && it.vx<0f })
    repeat(60) { if(npc.hp==before) engine.step(.02f,0f,0f) }
    assertTrue("Standing nearby is not an invisible shield against arrows",npc.hp<before)
    assertEquals(heroHp,guardian.hp)
  }

}
