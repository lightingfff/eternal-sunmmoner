package com.example.ui.components.p28

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.min

/**
 * 9-slice renderer for the P28 frame assets.
 *
 * Corners are drawn 1:1 at their natural pixel size; edges stretch along ONE axis only;
 * the center stretches both. All nine regions are drawn with srcRect/dstRect directly on
 * the native canvas — no per-recompose Bitmap crops are allocated. If the destination is
 * smaller than 2*inset on an axis, the whole image is scaled instead of producing
 * negative sizes.
 */
fun DrawScope.drawNineSlice(image: ImageBitmap, insetPx: Int, alpha: Float = 1f) {
  val iw = image.width
  val ih = image.height
  val sx = min(insetPx, iw / 2)
  val sy = min(insetPx, ih / 2)
  val dstW = size.width
  val dstH = size.height
  if (dstW <= 0f || dstH <= 0f) return
  val bmp = image.asAndroidBitmap()
  val paint = Paint().apply {
    isAntiAlias = true
    this.alpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
  }
  val canvas = drawContext.canvas.nativeCanvas
  if (dstW < sx * 2f || dstH < sy * 2f) {
    canvas.drawBitmapRect(
      bmp, null,
      RectF(0f, 0f, dstW.coerceAtLeast(1f), dstH.coerceAtLeast(1f)), paint,
    )
    return
  }
  val dw = dstW
  val dh = dstH
  val mw = dw - 2 * sx
  val mh = dh - 2 * sy
  // (srcL, srcT, srcR, srcB, dstL, dstT, dstR, dstB)
  val regions = arrayOf(
    floatArrayOf(0f, 0f, sx.toFloat(), sy.toFloat(), 0f, 0f, sx.toFloat(), sy.toFloat()),
    floatArrayOf(sx.toFloat(), 0f, (iw - sx).toFloat(), sy.toFloat(), sx.toFloat(), 0f, sx + mw, sy.toFloat()),
    floatArrayOf((iw - sx).toFloat(), 0f, iw.toFloat(), sy.toFloat(), dw - sx, 0f, dw, sy.toFloat()),
    floatArrayOf(0f, sy.toFloat(), sx.toFloat(), (ih - sy).toFloat(), 0f, sy.toFloat(), sx.toFloat(), sy + mh),
    floatArrayOf(sx.toFloat(), sy.toFloat(), (iw - sx).toFloat(), (ih - sy).toFloat(), sx.toFloat(), sy.toFloat(), sx + mw, sy + mh),
    floatArrayOf((iw - sx).toFloat(), sy.toFloat(), iw.toFloat(), (ih - sy).toFloat(), dw - sx, sy.toFloat(), dw, sy + mh),
    floatArrayOf(0f, (ih - sy).toFloat(), sx.toFloat(), ih.toFloat(), 0f, dh - sy, sx.toFloat(), dh),
    floatArrayOf(sx.toFloat(), (ih - sy).toFloat(), (iw - sx).toFloat(), ih.toFloat(), sx.toFloat(), dh - sy, sx + mw, dh),
    floatArrayOf((iw - sx).toFloat(), (ih - sy).toFloat(), iw.toFloat(), ih.toFloat(), dw - sx, dh - sy, dw, dh),
  )
  for (r in regions) {
    canvas.drawBitmapRect(
      bmp,
      Rect(r[0].toInt(), r[1].toInt(), r[2].toInt(), r[3].toInt()),
      RectF(r[4], r[5], r[6], r[7]),
      paint,
    )
  }
}
