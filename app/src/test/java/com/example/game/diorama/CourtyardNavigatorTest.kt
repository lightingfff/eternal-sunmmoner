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
    for (rail in Courtyard.lowerRailings) {
      val point = GroundPoint(rail.centerX, (rail.north + rail.south) * .5f)
      assertTrue(navigator.route(Courtyard.start, point).isEmpty())
    }
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

  @Test
  fun allDistrictsAreConnectedThroughTheBridgeAndGardenEntrance() {
    val navigator = CourtyardNavigator()
    val market = Courtyard.landmarks[1].destination
    val garden = Courtyard.landmarks[2].destination
    val first = navigator.route(Courtyard.start, market)
    val second = navigator.route(market, garden)
    val back = navigator.route(garden, Courtyard.start)
    assertTrue(first.last().distance(market) < .3f && second.last().distance(garden) < .3f)
    assertTrue(back.last().distance(Courtyard.start) < .3f)
    assertTrue(first.count { it.z in Courtyard.SOUTH..Courtyard.BRIDGE_END && Courtyard.inBridge(it.x, it.z) } >= 8)
    assertTrue(second.any { Courtyard.orchardEntrance.contains(it.x, it.z) })
    assertTrue((first + second + back).all { Courtyard.canStand(it.x, it.z) })
    assertFalse(Courtyard.canStand(Courtyard.fountain.x, Courtyard.fountain.z))
    assertFalse(Courtyard.canStand(Courtyard.gardenPond.centerX, 26.5f))
    assertFalse(Courtyard.canStand(9f, 28f))
  }

  @Test
  fun theMarketDockIsReachedFromLand() {
    val dock = GroundPoint(-13.5f, 11.7f)
    val route = CourtyardNavigator().route(Courtyard.landmarks[1].destination, dock)
    assertTrue(route.last().distance(dock) < .3f)
    assertTrue(route.all { Courtyard.isGround(it.x, it.z) })
    assertFalse(Courtyard.canStand(-10f, 11.7f))
  }
}
