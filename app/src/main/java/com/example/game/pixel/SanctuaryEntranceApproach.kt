package com.example.game.pixel

import com.example.game.map.PathSegment
import com.example.game.map.SanctuaryIslandGeometry
import kotlin.math.abs

/** Presentation follows the existing road; navigation/collision geometry stays authoritative. */
internal object SanctuaryEntranceApproach {
  const val HALF_CARPET_WIDTH = 30f
  fun centerX(y: Float, paths: List<PathSegment>): Float {
    val road = paths.filter { abs(it.endX - SanctuaryIslandGeometry.SOUTH_GATE_X) < 1f }
      .maxByOrNull { it.endY } ?: return SanctuaryIslandGeometry.SOUTH_GATE_X
    val lengthY = road.endY - road.startY
    if (abs(lengthY) < 1f) return road.endX
    val t = ((y - road.startY) / lengthY).coerceIn(0f, 1f)
    return road.startX + (road.endX - road.startX) * t
  }
}
