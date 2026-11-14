package com.example.game.pixel

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.game.map.ObliqueCamera
import com.example.game.map.PathSegment
import kotlin.math.hypot

/** Continuous textured paths, rather than centre-selected 64-world-unit stair steps.
 * The model's segment coordinates/widths and navigation remain untouched. */
internal object MeadowRoadArt {
  private var cachedSegments: List<PathSegment>? = null
  private var core = Path()
  private var verge = Path()
  private val paint = Paint().apply { isAntiAlias = false; isFilterBitmap = false }
  private val destination = RectF()

  private fun outline(segments: List<PathSegment>, extra: Float): Path {
    var result = Path()
    fun add(shape: Path) { result = Path.combine(PathOperation.Union, result, shape) }
    for (seg in segments) {
      val radius = seg.width * .5f + extra
      if (radius <= 0f) continue
      val dx = seg.endX - seg.startX
      val dy = seg.endY - seg.startY
      val length = hypot(dx, dy)
      add(Path().apply { addOval(Rect(seg.startX - radius, seg.startY - radius, seg.startX + radius, seg.startY + radius)) })
      if (length < .001f) continue
      val nx = -dy / length * radius
      val ny = dx / length * radius
      // Explicit union avoids winding-dependent holes at rounded caps and junctions.
      add(Path().apply {
        moveTo(seg.startX + nx, seg.startY + ny)
        lineTo(seg.startX - nx, seg.startY - ny)
        lineTo(seg.endX - nx, seg.endY - ny)
        lineTo(seg.endX + nx, seg.endY + ny)
        close()
      })
      add(Path().apply { addOval(Rect(seg.endX - radius, seg.endY - radius, seg.endX + radius, seg.endY + radius)) })
    }
    return result
  }

  fun DrawScope.draw(paths: List<PathSegment>, tiles: List<ImageBitmap>, camX: Float, camY: Float,
    zoom: Float, columns: IntRange, rows: IntRange) {
    if (paths.isEmpty() || columns.isEmpty() || rows.isEmpty()) return
    if (cachedSegments !== paths) {
      core = outline(paths, 0f)
      verge = outline(paths, 7f)
      cachedSegments = paths
    }
    withTransform({
      translate(size.width * .5f - camX * zoom, size.height * .5f - camY * zoom * ObliqueCamera.OBLIQUE_Y_SCALE)
      scale(zoom, zoom * ObliqueCamera.OBLIQUE_Y_SCALE, Offset.Zero)
    }) {
      fun DrawScope.material(alpha: Float) {
        paint.alpha = (alpha * 255).toInt()
        for (ty in rows) for (tx in columns) {
          if (!SanctuaryRoadCoverage.mayContainRoad(paths, tx, ty, 7f)) continue
          destination.set(tx * 64f, ty * 64f, (tx + 1) * 64f, (ty + 1) * 64f)
          // Shared fractional boundaries must not alpha-feather each tile separately.
          // The outer clip handles the road edge; material cells meet with nearest sampling.
          drawContext.canvas.nativeCanvas.drawBitmap(ArtDepthAssets.tile(tiles, tx, ty).asAndroidBitmap(),
            null, destination, paint)
        }
      }
      clipPath(verge) { material(.28f) }
      clipPath(core) { material(1f) }
    }
  }
}
