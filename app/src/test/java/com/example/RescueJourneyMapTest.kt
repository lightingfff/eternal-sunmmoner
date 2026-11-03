package com.example

import com.example.game.combat.RescueJourneyContent
import com.example.game.map.CollisionPhysics
import com.example.game.tower.TowerFloorCatalog
import com.example.data.LostCourierProgression
import com.example.data.SanctuaryProgressionHelper
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class RescueJourneyMapTest {
  @Test fun additiveDistrictPreservesOldGeometryAndHasWalkableCourierCorridor() {
    val spec=TowerFloorCatalog.FLOORS.first()
    val old=TowerFloorCatalog.buildTowerFloorMap(spec)
    val expanded=TowerFloorCatalog.buildRescueJourneyMap(spec)
    assertEquals(old.obstacles,expanded.obstacles.take(old.obstacles.size))
    assertEquals(old.paths,expanded.paths.take(old.paths.size))
    val blocked=mutableListOf<String>()
    RescueJourneyContent.routePoints.zipWithNext().forEachIndexed { index,(a,b) ->
      val steps=(hypot(b.first-a.first,b.second-a.second)/8).toInt().coerceAtLeast(1)
      for (step in 0..steps) {
        val f=step.toFloat()/steps;val x=a.first+(b.first-a.first)*f;val y=a.second+(b.second-a.second)*f
        if(CollisionPhysics.wouldCollide(x,y,22f,expanded.obstacles)) {
          blocked+="segment $index at ($x,$y): "+expanded.obstacles.filter { CollisionPhysics.wouldCollide(x,y,22f,listOf(it)) }.joinToString { it.id }
          break
        }
      }
    }
    assertTrue(blocked.joinToString("\n"),blocked.isEmpty())
  }
  @Test fun routeChoicesRemainAfterDiscoveryChurnAndDoNotReplaceEscortFlags() {
    val persistent=listOf(LostCourierProgression.FLAG_ALARM_SILENCED,LostCourierProgression.FLAG_SCOUT_RESCUED,
      LostCourierProgression.FLAG_JOURNEY_COMPLETE,LostCourierProgression.FLAG_SUPPLIES_RECOVERED)
    val flags=SanctuaryProgressionHelper.compactStoryFlags(persistent+(1..40).map { "DISC_other_$it" })
    assertTrue(flags.containsAll(persistent))
    assertTrue(flags.size<=20)
  }
}
