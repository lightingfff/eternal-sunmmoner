package com.example.game.diorama

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CourtyardNavigatorTest {
  @Test
  fun routeUsesBothStaircasesToReachTheUpperGarden() {
    val target = GroundPoint(0f, -9f)
    val route = CourtyardNavigator().route(Courtyard.start, target)
    assertTrue(route.isNotEmpty())
    assertTrue(route.last().distance(target) <= .29f)
    assertEquals(2.4f, Courtyard.height(route.last().x, route.last().z), .001f)
    assertTrue(route.all { Courtyard.canStand(it.x, it.z) })

    val crossed = route.zipWithNext()
    assertTrue(crossed.all { (a, b) ->
      a.distance(b) <= .41f && abs(Courtyard.height(a.x, a.z) - Courtyard.height(b.x, b.z)) <= .41f
    })
    for (edge in listOf(Courtyard.STONE_EDGE, Courtyard.UPPER_EDGE)) {
      val step = crossed.single { (a, b) -> a.z >= edge && b.z < edge }
      assertTrue(abs(step.second.x) < Courtyard.STAIR_HALF)
      assertTrue(route.count { it.z in edge..edge + Courtyard.STAIR_LENGTH && abs(it.x) < Courtyard.STAIR_HALF } >= 5)
    }
  }

  @Test
  fun aHouseAndTheCliffEdgeRejectMovement() {
    val navigator = CourtyardNavigator()
    val house = Courtyard.houses.first().bounds
    assertFalse(Courtyard.canStand(house.centerX, house.south - .5f))
    assertTrue(navigator.route(Courtyard.start, GroundPoint(house.centerX, house.south - .5f)).isEmpty())
    assertTrue(navigator.route(Courtyard.start, GroundPoint(Courtyard.RIGHT + .1f, 5f)).isEmpty())
    val around = navigator.route(Courtyard.start, GroundPoint(-9.4f, -2.6f))
    assertTrue(around.isNotEmpty())
    assertTrue(around.none { house.contains(it.x, it.z, .33f) })
  }
}
