package com.example.ui.components.p28

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

/**
 * 9-slice renderer for the P28 frame assets.
 *
 * Corners are drawn 1:1 at their natural pixel size; edges stretch along ONE axis only;
 * the center stretches both. All nine regions use [DrawScope.drawImage] srcOffset/srcSize
 * vs dstOffset/dstSize directly — no per-recompose Bitmap crops are allocated. If the
 * destination is smaller than 2*inset on an axis, the whole image is scaled instead of
 * producing negative sizes.
 */
fun DrawScope.drawNineSlice(image: ImageBitmap, insetPx: Int, alpha: Float = 1f) {
  val iw = image.width
  val ih = image.height
  val sx = min(insetPx, iw / 2)
  val sy = min(insetPx, ih / 2)
  val dw = size.width.toInt()
  val dh = size.height.toInt()
  if (dw <= 0 || dh <= 0) return
  if (dw < sx * 2 || dh < sy * 2) {
    drawImage(image, dstSize = IntSize(dw.coerceAtLeast(1), dh.coerceAtLeast(1)), alpha = alpha)
    return
  }
  val mw = dw - 2 * sx
  val mh = dh - 2 * sy
  // (srcX, srcY, srcW, srcH, dstX, dstY, dstW, dstH)
  val regions = arrayOf(
    intArrayOf(0, 0, sx, sy, 0, 0, sx, sy),
    intArrayOf(sx, 0, iw - 2 * sx, sy, sx, 0, mw, sy),
    intArrayOf(iw - sx, 0, sx, sy, dw - sx, 0, sx, sy),
    intArrayOf(0, sy, sx, ih - 2 * sy, 0, sy, sx, mh),
    intArrayOf(sx, sy, iw - 2 * sx, ih - 2 * sy, sx, sy, mw, mh),
    intArrayOf(iw - sx, sy, sx, ih - 2 * sy, dw - sx, sy, sx, mh),
    intArrayOf(0, ih - sy, sx, sy, 0, dh - sy, sx, sy),
    intArrayOf(sx, ih - sy, iw - 2 * sx, sy, sx, dh - sy, mw, sy),
    intArrayOf(iw - sx, ih - sy, sx, sy, dw - sx, dh - sy, sx, sy),
  )
  for (r in regions) {
    drawImage(
      image,
      srcOffset = IntOffset(r[0], r[1]),
      srcSize = IntSize(r[2], r[3]),
      dstOffset = IntOffset(r[4], r[5]),
      dstSize = IntSize(r[6], r[7]),
      alpha = alpha,
    )
  }
}
