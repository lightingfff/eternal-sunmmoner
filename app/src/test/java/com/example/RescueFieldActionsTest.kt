package com.example

import com.example.game.combat.*
import com.example.game.map.CollisionPhysics
import com.example.game.tower.TowerFloorCatalog
import org.junit.Assert.*
import org.junit.Test

class RescueFieldActionsTest {
  @Test fun fieldWorkNeedsActualPresenceAndExplicitActionAtEachDistinctPoint() {
    val action = RescueFieldActions.forChapter("crossing", 5400f to 1500f)!!
    assertFalse(action.canInteract(emptyList()))
    assertFalse(action.interact(listOf(5400f to 1500f)))
    val first = action.currentTarget!!
    val second = action.targets.last()
    assertFalse("Being at the second lever cannot skip the safety brake", action.interact(second.id, listOf(second.x to second.y)))
    assertEquals(0, action.completedSteps)
    assertTrue(action.canInteract(listOf(first.x to first.y)))
    assertEquals("Proximity alone never performs an interaction", 0, action.completedSteps)
    assertTrue(action.interact(first.id, listOf(first.x to first.y)))
    assertFalse(action.interact(first.id, listOf(first.x to first.y)))
    assertTrue(action.interact(second.id, listOf(second.x to second.y)))
    assertTrue(action.completed)
    assertNull(action.currentTarget)
    assertFalse(action.interact(listOf(second.x to second.y)))
  }

  @Test fun sevenDifferentFieldTasksHaveFourteenOrderedSpatialStepsAndNoTimers() {
    val actions = RescueJourneyContent.chapters.mapNotNull { chapter ->
      RescueFieldActions.forChapter(chapter.id, RescueJourneyContent.routePoints[chapter.routeEnd])
    }
    assertEquals(RescueFieldActionType.entries.toSet(), actions.map { it.type }.toSet())
    assertEquals(14, actions.sumOf { it.targets.size })
    assertEquals(14, actions.flatMap { it.targets }.map { it.id }.distinct().size)
    actions.forEach { action ->
      assertTrue(action.hintVi.isNotBlank())
      for (target in action.targets) assertTrue(action.interact(target.id, listOf(target.x to target.y)))
      assertTrue(action.completed)
    }
  }

  @Test fun authoredFieldTargetsHaveClearPhysicalFootprintsInTheActualMap() {
    val map = TowerFloorCatalog.buildRescueJourneyMap(TowerFloorCatalog.FLOORS.first())
    RescueJourneyContent.chapters.forEach { chapter ->
      RescueFieldActions.forChapter(chapter.id, RescueJourneyContent.routePoints[chapter.routeEnd])?.targets?.forEach { target ->
        assertFalse("Blocked field objective ${target.id}", CollisionPhysics.wouldCollide(target.x, target.y, 24f, map.obstacles))
      }
    }
  }
}
