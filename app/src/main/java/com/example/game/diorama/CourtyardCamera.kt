package com.example.game.diorama

import android.opengl.Matrix
import kotlin.math.hypot

internal class CourtyardCamera {
  val projection = FloatArray(16)
  val view = FloatArray(16)
  val viewProjection = FloatArray(16)
  val inverse = FloatArray(16)
  val eye = FloatArray(3)
  private val clip = FloatArray(4)
  private val rayA = FloatArray(4)
  private val rayB = FloatArray(4)
  private var valid = false
  var width = 1
    private set
  var height = 1
    private set

  fun frame(actor: GroundPoint, width: Int, height: Int, zoom: Float) {
    this.width = width.coerceAtLeast(1)
    this.height = height.coerceAtLeast(1)
    val aspect = this.width / this.height.toFloat()
    val aimX = (actor.x * .65f).coerceIn(-5f, 5f)
    val aimZ = (actor.z - 3.8f).coerceIn(-8f, 1.3f)
    val aimY = Courtyard.height(actor.x, actor.z) * .46f + .5f
    val distance = zoom * if (aspect < 1f) 1.12f else 1f
    eye[0] = aimX + 4.7f * distance
    eye[1] = aimY + 9.6f * distance
    eye[2] = aimZ + 17.2f * distance
    Matrix.setLookAtM(view, 0, eye[0], eye[1], eye[2], aimX, aimY, aimZ, 0f, 1f, 0f)
    Matrix.perspectiveM(projection, 0, if (aspect < 1f) 49f else 41f, aspect, .65f, 80f)
    Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
    valid = Matrix.invertM(inverse, 0, viewProjection, 0)
  }

  fun depthAt(x: Float, y: Float, z: Float): Float = -(view[2] * x + view[6] * y + view[10] * z + view[14])

  fun groundAt(screenX: Float, screenY: Float): GroundPoint? {
    if (!valid || screenX !in 0f..width.toFloat() || screenY !in 0f..height.toFloat()) return null
    clip[0] = screenX / width * 2f - 1f
    clip[1] = 1f - screenY / height * 2f
    clip[2] = -1f
    clip[3] = 1f
    Matrix.multiplyMV(rayA, 0, inverse, 0, clip, 0)
    clip[2] = 1f
    Matrix.multiplyMV(rayB, 0, inverse, 0, clip, 0)
    val ax = rayA[0] / rayA[3]
    val ay = rayA[1] / rayA[3]
    val az = rayA[2] / rayA[3]
    val dx = rayB[0] / rayB[3] - ax
    val dy = rayB[1] / rayB[3] - ay
    val dz = rayB[2] / rayB[3] - az
    if (dy >= -.0001f) return null
    val count = (hypot(dx, dz) / .10f).toInt().coerceIn(64, 1100)
    for (i in 0..count) {
      val t = i / count.toFloat()
      val x = ax + dx * t
      val y = ay + dy * t
      val z = az + dz * t
      if (y < -.05f) break
      if (x !in Courtyard.LEFT..Courtyard.RIGHT || z !in Courtyard.NORTH..Courtyard.SOUTH) continue
      val roof = Courtyard.houses.firstOrNull { it.bounds.contains(x, z) }
      if (roof != null && y <= Courtyard.height(x, z) + roof.height + roof.roof) return null
      if (y <= Courtyard.height(x, z)) return GroundPoint(x, z).takeIf { Courtyard.canStand(x, z) }
    }
    return null
  }
}
