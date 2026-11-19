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
    for (stair in Courtyard.stairs) {
      val crossing = crossed.single { (a, b) -> a.z >= stair.edge && b.z < stair.edge }
      assertTrue(crossing.second.x in stair.left..stair.right)
      assertTrue(route.count { it.z in stair.edge..stair.foot && it.x in stair.left..stair.right } >= 5)
    }
  }

  @Test
  fun aHouseAndTheCliffEdgeRejectMovement() {
    val navigator = CourtyardNavigator()
    val house = Courtyard.houses.first().bounds
    assertFalse(Courtyard.canStand(house.centerX, house.south - .5f))
    assertTrue(navigator.route(Courtyard.start, GroundPoint(house.centerX, house.south - .5f)).isEmpty())
    assertTrue(navigator.route(Courtyard.start, GroundPoint(Courtyard.RIGHT + .1f, 5f)).isEmpty())
    val around = navigator.route(Courtyard.start, GroundPoint(-10.75f, -2.6f))
    assertTrue(around.isNotEmpty())
    assertTrue(around.none { house.contains(it.x, it.z, .33f) })
  }

  @Test
  fun theBridgeIsReachableAndTheWaterIsBlocked() {
    val navigator = CourtyardNavigator()
    val bridge = GroundPoint(-5.9f, 11.5f)
    val route = navigator.route(Courtyard.start, bridge)
    assertTrue(route.last().distance(bridge) < .3f)
    assertTrue(route.filter { it.z > Courtyard.SOUTH }.all { Courtyard.inBridge(it.x, it.z) })
    assertTrue(navigator.route(bridge, Courtyard.start).isNotEmpty())
    assertTrue(navigator.route(Courtyard.start, GroundPoint(-3.9f, 10.3f)).isEmpty())
  }
}
