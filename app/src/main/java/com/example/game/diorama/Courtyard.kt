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
  val start = GroundPoint(-.6f, 5.1f)

  data class House(val bounds: Footprint, val height: Float, val roof: Float, val style: Int)
  val houses = listOf(
    House(Footprint(-8.4f, -3.7f, -3.7f, -.35f), 2.8f, 1.65f, 0),
    House(Footprint(2.75f, -10.8f, 7.8f, -6.75f), 3.1f, 1.95f, 1),
    House(Footprint(-8.1f, -11.35f, -4.65f, -8.65f), 2.25f, 1.25f, 2),
    House(Footprint(5.4f, -3.85f, 9.1f, -1.1f), 2.6f, 1.5f, 2),
  )
  val trees = listOf(
    GroundPoint(-10.1f, 5.1f), GroundPoint(9.75f, 5.7f), GroundPoint(10.4f, -.2f),
    GroundPoint(-10.3f, -5.8f), GroundPoint(2.8f, -11.5f), GroundPoint(9.9f, -10.3f),
    GroundPoint(-1.8f, -11.9f), GroundPoint(-9.5f, -10.8f),
  )
  val bollards = listOf(
    GroundPoint(-2.4f, 1.05f), GroundPoint(2.4f, 1.05f),
    GroundPoint(-2.4f, -4.95f), GroundPoint(2.4f, -4.95f),
    GroundPoint(-8.9f, -6.75f), GroundPoint(7.9f, -5.55f),
  )

  fun height(x: Float, z: Float): Float {
    val flat = when {
      z < UPPER_EDGE -> RISE * 2
      z < STONE_EDGE -> RISE
      else -> 0f
    }
    if (abs(x) > STAIR_HALF) return flat
    for ((edge, base) in listOf(STONE_EDGE to 0f, UPPER_EDGE to RISE)) {
      if (z >= edge && z < edge + STAIR_LENGTH) {
        return base + (STEP_COUNT - ((z - edge) / STEP_DEPTH).toInt().coerceIn(0, STEP_COUNT - 1)) * STEP_HEIGHT
      }
    }
    return flat
  }

  fun canStand(x: Float, z: Float): Boolean {
    if (x !in (LEFT + .42f)..(RIGHT - .42f) || z !in (NORTH + .46f)..(SOUTH - .42f)) return false
    if (houses.any { it.bounds.contains(x, z, .33f) }) return false
    if (trees.any { abs(it.x - x) < .44f && abs(it.z - z) < .44f }) return false
    if (bollards.any { abs(it.x - x) < .31f && abs(it.z - z) < .31f }) return false
    // Preserve the stair rail clearance and prevent cuts across the side faces.
    if (abs(abs(x) - STAIR_HALF) < .23f &&
      ((z in STONE_EDGE..STONE_EDGE + STAIR_LENGTH) || (z in UPPER_EDGE..UPPER_EDGE + STAIR_LENGTH))) return false
    return true
  }
}

/** The preview has its own walkable graph. A route can change height only at its authored stairs. */
internal class CourtyardNavigator {
  private val gridStep = .4f
  private val width = 61
  private val depth = 56
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
