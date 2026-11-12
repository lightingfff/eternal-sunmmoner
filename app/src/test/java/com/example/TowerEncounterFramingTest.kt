package com.example

import com.example.data.model.HeroRecord
import com.example.game.combat.EnemyCombatUnit
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.map.EnemyArchetype
import com.example.game.tower.TowerFloorCatalog
import com.example.ui.components.TowerEncounterFraming
import org.junit.Assert.*
import org.junit.Test

/** Arranged camera-interest fixtures; actual native assault sequence provides visual evidence. */
class TowerEncounterFramingTest {
  private fun engine()=TowerCombatEngine(TowerFloorCatalog.FLOORS.first(),
    listOf(HeroRecord(id="frame",name="Kaelen")),emptyList(),enableLostCourier=true,
    enableLivingExploration=true).apply {
      stage=MissionStage.IN_PROGRESS
      heroes.single().apply { x=1200f;y=1125f;attackCooldown=100f;skillCooldown=100f }
    }
  private fun add(engine:TowerCombatEngine,x:Float)=EnemyCombatUnit("frame_$x",
    EnemyArchetype.FIELD_GOBLIN_RAIDER,100,100,1,1,0f,x,1125f).also { engine.enemies+=it }

  @Test fun nearbyUnseenThreatCannotRevealItselfThroughCameraMotion() {
    val engine=engine();val enemy=add(engine,1380f);enemy.aggroed=true
    val before=TowerEncounterFraming.bounds(engine)!!
    assertEquals(1200f,before.right,0f)
    engine.step(.001f,0f,0f)
    assertTrue(engine.isEnemyRevealed(enemy))
    assertEquals(enemy.x,TowerEncounterFraming.bounds(engine)!!.right,0f)
  }

  @Test fun deadAndDistantIdleEnemiesCannotPullFrameAwayFromTheParty() {
    val engine=engine();val near=add(engine,1380f)
    engine.step(.001f,0f,0f)
    near.dead=true
    val far=add(engine,2400f);far.aggroed=false
    assertEquals(engine.heroes.single().x,TowerEncounterFraming.bounds(engine)!!.right,0f)
  }

  @Test fun manualCameraModeKeepsHeroOnlyFramingInsteadOfFollowingThreats() {
    val engine=engine();val near=add(engine,1380f);near.aggroed=true
    engine.step(.001f,0f,0f)
    assertEquals(near.x,TowerEncounterFraming.bounds(engine,true)!!.right,0f)
    assertEquals(engine.heroes.single().x,TowerEncounterFraming.bounds(engine,false)!!.right,0f)
  }

  @Test fun aStaleTargetIdCannotKeepAnOldDistantFightInFrame() {
    val engine=engine();val enemy=add(engine,1380f)
    engine.step(.001f,0f,0f)
    assertTrue(engine.isEnemyRevealed(enemy))
    engine.heroes.single().currentTargetId=enemy.id
    enemy.x=2400f
    assertEquals(engine.heroes.single().x,TowerEncounterFraming.bounds(engine)!!.right,0f)
  }
}
