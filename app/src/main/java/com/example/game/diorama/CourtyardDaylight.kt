package com.example.game.diorama

import java.nio.ByteBuffer
import kotlin.math.hypot
import kotlin.math.sqrt

/** Parameters for the world sun and the market's baked contact shading. */
internal object CourtyardDaylight {
  private const val SUN_X = -24f
  private const val SUN_Y = 20f
  private const val SUN_Z = 15f
  const val contactSize = 128
  val bounds = Courtyard.market
  private val sunLength = sqrt(SUN_X * SUN_X + SUN_Y * SUN_Y + SUN_Z * SUN_Z)
  val sun = Point3(SUN_X / sunLength, SUN_Y / sunLength, SUN_Z / sunLength)
  val sunAnchor = Point3(SUN_X, SUN_Y, SUN_Z + 8f)

  private data class Contact(val x: Float, val z: Float, val rx: Float, val rz: Float)

  fun marketView(point: GroundPoint): Float {
    val distance = minOf(point.x - bounds.left, bounds.right - point.x,
      point.z - bounds.north, bounds.south - point.z)
    val coverage = ((distance + 1f) / 2.6f).coerceIn(0f, 1f)
    return coverage * coverage * (3f - 2f * coverage)
  }

  fun contactPixels(): ByteBuffer {
    val contacts = buildList {
      for (tree in Courtyard.gardenTrees) add(Contact(tree.x, tree.z, .67f, .58f))
      for (post in Courtyard.bollards) add(Contact(post.x, post.z, .52f, .49f))
      for ((left, north, right, south) in Courtyard.stalls) {
        for (x in floatArrayOf(left + .10f, right - .10f)) {
          for (z in floatArrayOf(north + .10f, south - .11f)) add(Contact(x, z, .37f, .35f))
        }
      }
    }
    val pixels = ByteBuffer.allocateDirect(contactSize * contactSize)
    for (j in 0 until contactSize) {
      val z = bounds.north + (j + .5f) * (bounds.south - bounds.north) / contactSize
      for (i in 0 until contactSize) {
        val x = bounds.left + (i + .5f) * (bounds.right - bounds.left) / contactSize
        var shade = 0f
        for (contact in contacts) {
          val dx = (x - contact.x) / contact.rx
          val dz = (z - contact.z) / contact.rz
          val falloff = (1f - dx * dx - dz * dz).coerceAtLeast(0f)
          shade = maxOf(shade, falloff * falloff)
        }
        val rim = hypot(x - Courtyard.fountain.x, z - Courtyard.fountain.z) - Courtyard.FOUNTAIN_RADIUS
        if (rim in 0f..0.55f) {
          val falloff = 1f - rim / .55f
          shade = maxOf(shade, .75f * falloff * falloff)
        }
        pixels.put((shade * 255f).toInt().toByte())
      }
    }
    pixels.rewind()
    return pixels
  }
}
