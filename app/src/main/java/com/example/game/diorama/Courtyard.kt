package com.example.game.diorama

import kotlin.math.abs
import kotlin.math.hypot

internal data class GroundPoint(val x: Float, val z: Float) {
  fun distance(other: GroundPoint): Float = hypot(x - other.x, z - other.z)
}

internal data class Footprint(val left: Float, val north: Float, val right: Float, val south: Float) {
  fun contains(x: Float, z: Float, margin: Float = 0f): Boolean =
    x >= left - margin && x <= right + margin && z >= north - margin && z <= south + margin

  val centerX get() = (left + right) * .5f
}

internal object Courtyard {
  const val LEFT = -12f
  const val RIGHT = 12f
  const val NORTH = -13f
  const val SOUTH = 9f
  const val STONE_EDGE = 1.5f
  const val UPPER_EDGE = -4.5f
  const val RISE = 1.2f
  const val STAIR_LENGTH = 2.4f
  const val STEP_COUNT = 6
  const val STEP_DEPTH = STAIR_LENGTH / STEP_COUNT
  const val STEP_HEIGHT = RISE / STEP_COUNT
  const val STAIR_HALF = 1.55f
  const val BRIDGE_END = 12.6f
  const val BRIDGE_LEFT = -7.3f
  const val BRIDGE_RIGHT = -4.55f
  val start = GroundPoint(-2.65f, 5.65f)

  data class Stair(val center: Float, val edge: Float, val base: Float) {
    val left = center - STAIR_HALF
    val right = center + STAIR_HALF
    val foot = edge + STAIR_LENGTH
  }

  val stairs = listOf(Stair(3.75f, STONE_EDGE, 0f), Stair(-2.55f, UPPER_EDGE, RISE))

  data class House(val bounds: Footprint, val height: Float, val roof: Float, val style: Int)
  val houses = listOf(
    House(Footprint(-10.1f, -3.75f, -6.0f, -.65f), 2.15f, 1.22f, 1),
    House(Footprint(5.75f, -4.25f, 10.85f, -.55f), 3.35f, 1.92f, 0),
    House(Footprint(-8.7f, -11.7f, -4.8f, -8.15f), 2.6f, 1.38f, 2),
    House(Footprint(1.3f, -11.1f, 5.65f, -7.75f), 2.45f, 1.5f, 1),
    House(Footprint(7.75f, -11.7f, 10.9f, -8.3f), 2.8f, 1.72f, 2),
  )
  val trees = listOf(
    GroundPoint(-10.5f, 6.1f), GroundPoint(10.45f, 7.9f),
    GroundPoint(-10.6f, -6.95f), GroundPoint(-3.9f, -10.4f),
    GroundPoint(6.65f, -9.0f), GroundPoint(11.1f, -6.25f), GroundPoint(-10.6f, -11.5f),
  )
  val bollards = listOf(
    GroundPoint(1.85f, 1.24f), GroundPoint(5.63f, 1.2f),
    GroundPoint(-4.42f, -4.72f), GroundPoint(-.70f, -4.72f),
    GroundPoint(-7.65f, 8.37f), GroundPoint(-4.25f, 8.37f),
    GroundPoint(-8.6f, -5.05f), GroundPoint(8.65f, .3f),
  )

  fun inBridge(x: Float, z: Float): Boolean =
    x in BRIDGE_LEFT..BRIDGE_RIGHT && z in (SOUTH - .12f)..BRIDGE_END

  fun isGround(x: Float, z: Float): Boolean =
    (x in LEFT..RIGHT && z in NORTH..SOUTH) || inBridge(x, z)

  fun height(x: Float, z: Float): Float {
    for (stair in stairs) {
      if (x in stair.left..stair.right && z >= stair.edge && z < stair.foot) {
        return stair.base + (STEP_COUNT - ((z - stair.edge) / STEP_DEPTH).toInt().coerceIn(0, STEP_COUNT - 1)) * STEP_HEIGHT
      }
    }
    return when {
      z < UPPER_EDGE -> RISE * 2
      z < STONE_EDGE -> RISE
      else -> 0f
    }
  }

  fun canStand(x: Float, z: Float): Boolean {
    val inCourt = x in (LEFT + .42f)..(RIGHT - .42f) && z in (NORTH + .46f)..(SOUTH - .42f)
    val onBridge = x in (BRIDGE_LEFT + .43f)..(BRIDGE_RIGHT - .43f) &&
      z in (SOUTH - .64f)..(BRIDGE_END - .42f)
    if (!inCourt && !onBridge) return false
    if (houses.any { it.bounds.contains(x, z, .33f) }) return false
    if (trees.any { abs(it.x - x) < .44f && abs(it.z - z) < .44f }) return false
    if (bollards.any { abs(it.x - x) < .33f && abs(it.z - z) < .33f }) return false
    return stairs.none { stair ->
      abs(abs(x - stair.center) - STAIR_HALF) < .25f && z in stair.edge..stair.foot
    }
  }
}

/** The preview has its own walkable graph. A route can change height only at its authored stairs. */
internal class CourtyardNavigator {
  private val gridStep = .4f
  private val width = ((Courtyard.RIGHT - Courtyard.LEFT) / gridStep).toInt() + 1
  private val depth = ((Courtyard.BRIDGE_END - Courtyard.NORTH) / gridStep).toInt() + 1
  private fun at(index: Int) = GroundPoint(Courtyard.LEFT + index % width * gridStep,
    Courtyard.NORTH + index / width * gridStep)

  private val nodes = Array(width * depth, ::at)
  private val walkable = BooleanArray(nodes.size) { i -> nodes[i].let { Courtyard.canStand(it.x, it.z) } }
  private val elevations = FloatArray(nodes.size) { i -> nodes[i].let { Courtyard.height(it.x, it.z) } }

  private fun nearest(point: GroundPoint): Int {
    var result = -1
    var distance = .82f
    for (i in nodes.indices) {
      if (!walkable[i]) continue
      val next = point.distance(nodes[i])
      if (next < distance) {
        distance = next
        result = i
      }
    }
    return result
  }

  fun route(from: GroundPoint, to: GroundPoint): List<GroundPoint> {
    if (!Courtyard.canStand(to.x, to.z)) return emptyList()
    val origin = nearest(from)
    val target = nearest(to)
    if (origin < 0 || target < 0) return emptyList()
    val prev = IntArray(nodes.size) { -1 }
    val queue = IntArray(nodes.size)
    queue[0] = origin
    prev[origin] = origin
    var read = 0
    var write = 1
    while (read < write && prev[target] < 0) {
      val current = queue[read++]
      val x = current % width
      val z = current / width
      for (direction in 0..3) {
        val nx = x + if (direction == 0) 1 else if (direction == 1) -1 else 0
        val nz = z + if (direction == 2) 1 else if (direction == 3) -1 else 0
        if (nx !in 0 until width || nz !in 0 until depth) continue
        val next = nz * width + nx
        if (!walkable[next] || prev[next] >= 0 || abs(elevations[next] - elevations[current]) > .41f) continue
        prev[next] = current
        queue[write++] = next
      }
    }
    if (prev[target] < 0) return emptyList()
    val result = ArrayList<GroundPoint>()
    var index = target
    while (index != origin) {
      result += nodes[index]
      index = prev[index]
    }
    result += nodes[origin]
    result.reverse()
    return result
  }
}
