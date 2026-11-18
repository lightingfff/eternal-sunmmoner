package com.example.game.map

/** Projection of the ground plane. Upright sprite heights and world coordinates are independent. */
internal enum class GroundView(val groundY: Float) {
  STANDARD(ObliqueCamera.OBLIQUE_Y_SCALE),
  SANCTUARY_DIORAMA(0.57f);

  val ellipseRatio get() = ObliqueCamera.GROUND_ELLIPSE_RATIO * (groundY / ObliqueCamera.OBLIQUE_Y_SCALE)

  fun projectY(worldY: Float, cameraY: Float, zoom: Float, halfHeight: Float, elevation: Float = 0f): Float =
    ((worldY - cameraY) * groundY - elevation) * zoom + halfHeight

  fun unprojectY(screenY: Float, cameraY: Float, zoom: Float, halfHeight: Float): Float =
    (screenY - halfHeight) / (zoom * groundY) + cameraY

  fun clampCameraY(
    cameraY: Float, worldHeight: Float, wallMargin: Float, zoom: Float, halfHeight: Float,
    northHeadroom: Float,
  ): Float {
    val visibleHalf = halfHeight / (zoom.coerceAtLeast(0.26f) * groundY)
    val minY = wallMargin - northHeadroom + visibleHalf
    val maxY = worldHeight - wallMargin + 95f - visibleHalf
    return if (minY <= maxY) cameraY.coerceIn(minY, maxY) else worldHeight * 0.48f
  }

  companion object {
    fun forSanctuary(indoors: Boolean): GroundView = if (indoors) STANDARD else SANCTUARY_DIORAMA
  }
}
