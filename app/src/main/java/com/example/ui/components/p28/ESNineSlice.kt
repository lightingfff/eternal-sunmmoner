package com.example.ui.components.p28

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawImageRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

/**
 * 9-slice renderer for the P28 frame assets.
 *
 * Corners are drawn 1:1 at their natural pixel size; edges stretch along ONE axis only;
 * the center stretches both. All nine regions are drawn with srcRect/dstRect directly —
 * no per-recompose Bitmap crops are allocated. If the destination is smaller than
 * 2*inset on an axis, the whole image is scaled instead of producing negative sizes.
 */
fun DrawScope.drawNineSlice(image: ImageBitmap, insetPx: Int, alpha: Float = 1f) {
  val iw = image.width
  val ih = image.height
  val sx = min(insetPx, iw / 2)
  val sy = min(insetPx, ih / 2)
  val dstW = size.width
  val dstH = size.height
  val paint = Paint().apply { this.alpha = alpha }
  if (dstW <= 0f || dstH <= 0f) return
  if (dstW < sx * 2f || dstH < sy * 2f) {
    drawImageRect(
      image,
      dstSize = IntSize(dstW.toInt().coerceAtLeast(1), dstH.toInt().coerceAtLeast(1)),
      paint = paint,
    )
    return
  }
  val dw = dstW.toInt()
  val dh = dstH.toInt()
  val mw = dw - 2 * sx
  val mh = dh - 2 * sy
  // (srcX, srcY, srcW, srcH, dstX, dstY, dstW, dstH)
  val regions = arrayOf(
    intArrayOf(0, 0, sx, sy, 0, 0, sx, sy),                       // top-left
    intArrayOf(sx, 0, iw - 2 * sx, sy, sx, 0, mw, sy),             // top
    intArrayOf(iw - sx, 0, sx, sy, dw - sx, 0, sx, sy),            // top-right
    intArrayOf(0, sy, sx, ih - 2 * sy, 0, sy, sx, mh),             // left
    intArrayOf(sx, sy, iw - 2 * sx, ih - 2 * sy, sx, sy, mw, mh),  // center
    intArrayOf(iw - sx, sy, sx, ih - 2 * sy, dw - sx, sy, sx, mh), // right
    intArrayOf(0, ih - sy, sx, sy, 0, dh - sy, sx, sy),            // bottom-left
    intArrayOf(sx, ih - sy, iw - 2 * sx, sy, sx, dh - sy, mw, sy), // bottom
    intArrayOf(iw - sx, ih - sy, sx, sy, dw - sx, dh - sy, sx, sy),// bottom-right
  )
  for (r in regions) {
    drawImageRect(
      image,
      srcOffset = IntOffset(r[0], r[1]),
      srcSize = IntSize(r[2], r[3]),
      dstOffset = IntOffset(r[4], r[5]),
      dstSize = IntSize(r[6], r[7]),
      paint = paint,
    )
  }
}
