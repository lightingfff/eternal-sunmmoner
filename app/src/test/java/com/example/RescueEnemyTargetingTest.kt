package com.example

import com.example.data.model.HeroRecord
import com.example.game.combat.*
import com.example.game.map.EnemyArchetype
import com.example.game.tower.TowerFloorCatalog
import org.junit.Assert.*
import org.junit.Test

class RescueEnemyTargetingTest {
  private fun exercise(archetype: EnemyArchetype, distance: Float) {
    val engine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(),
      listOf(HeroRecord(id="hero", name="Hero")), emptyList(), enableLostCourier=true, enableRescueJourney=true)
    engine.stage=MissionStage.IN_PROGRESS
    engine.enemies.clear() // Isolate the real journey enemy's attack logic.
    val mission=engine.lostCourier!!
    engine.heroes.single().apply { x=2500f; y=1800f }
    engine.masterX=mission.courier.x+distance; engine.masterY=mission.courier.y
    val enemy=EnemyCombatUnit("journey_test",archetype,archetype.baseHp,archetype.baseHp,
      archetype.baseAtk,archetype.baseDef,archetype.speed,mission.courier.x+distance,mission.courier.y)
    engine.enemies.add(enemy)
    val initialMaster=engine.masterHp
    val initialCourier=mission.courier.hp
    repeat(60*5) { engine.step(1f/60f,0f,0f) }
    assertEquals("The off-field command sigil cannot receive enemy attacks",initialMaster,engine.masterHp)
    assertTrue("The courier must be a real damageable target for $archetype",mission.courier.hp < initialCourier)
  }
  @Test fun meleeThreatensCourierRatherThanCommandSigil() = exercise(EnemyArchetype.FIELD_GOBLIN_RAIDER,38f)
  @Test fun rangedThreatensCourierRatherThanCommandSigil() = exercise(EnemyArchetype.FIELD_GOBLIN_ARCHER,160f)
}
