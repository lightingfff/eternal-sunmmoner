package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.model.HeroRecord
import com.example.data.model.TacticalOrder
import com.example.game.combat.*
import com.example.game.map.EnemyArchetype
import com.example.game.tower.TowerFloorCatalog
import org.junit.Assert.*
import org.junit.Test

class TowerOffFieldMasterTest {
  private fun battle(): TowerCombatEngine {
    val engine = TowerCombatEngine(TowerFloorCatalog.FLOORS[1], listOf(HeroRecord(id="h",name="Hero")), emptyList())
    engine.stage=MissionStage.IN_PROGRESS
    engine.enemies.clear()
    val hero=engine.heroes.single()
    hero.x=engine.masterX+180f;hero.y=engine.masterY
    hero.attackCooldown=100f;hero.skillCooldown=100f
    engine.issueTacticalOrder(TacticalOrder.HOLD_POSITION)
    return engine
  }
  @Test fun genericRaiderAttacksHeroWhenCommandSigilIsCloser() {
    val e=battle();val hero=e.heroes.single();val type=EnemyArchetype.FIELD_GOBLIN_RAIDER
    e.enemies.add(EnemyCombatUnit("test_raider",type,type.baseHp,type.baseHp,type.baseAtk,type.baseDef,type.speed,e.masterX+30f,e.masterY))
    val initialMaster=e.masterHp;val initialHero=hero.hp
    repeat(60*4) { e.step(1f/60f,0f,0f) }
    assertEquals("Off-field sigil is not damageable",initialMaster,e.masterHp)
    assertTrue("Raider engages the field hero",hero.hp<initialHero)
  }
  @Test fun hostileProjectilePassesThroughSigilAndZeroLegacyMasterHpIsNotDefeat() {
    val e=battle();val type=EnemyArchetype.FIELD_GOBLIN_ARCHER
    e.enemies.add(EnemyCombatUnit("far_archer",type,type.baseHp,type.baseHp,type.baseAtk,type.baseDef,type.speed,e.masterX+400f,e.masterY))
    e.masterHp=0
    e.projectiles.add(CombatProjectile("test",false,e.masterX,e.masterY,0f,0f,25,Color.Red))
    e.step(.016f,0f,0f)
    assertTrue("Command anchor must not intercept hostile projectiles",e.projectiles.any { it.id=="test" })
    assertNotEquals("Living party must not lose due to a legacy Master HP field",MissionStage.DEFEAT,e.stage)
  }
  @Test fun zeroLivingHeroesEndsBattleWithoutAttemptingToTargetMaster() {
    val e=battle();val type=EnemyArchetype.FIELD_GOBLIN_RAIDER
    e.enemies.add(EnemyCombatUnit("test_raider",type,type.baseHp,type.baseHp,type.baseAtk,type.baseDef,type.speed,e.masterX+30f,e.masterY))
    e.heroes.single().knockedOut=true
    e.step(.016f,0f,0f)
    assertEquals(MissionStage.DEFEAT,e.stage)
  }
}
