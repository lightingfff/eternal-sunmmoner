package com.example.ui.components.p28

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 9-slice renderer for P28 frame assets. The compatibility overload keeps source and
 * destination corners at the same pixel size; [drawNineSliceScaled] lets UI chrome keep
 * a consistent dp-sized corner while still sampling the complete authored corner art.
 * No Bitmap crops are allocated during drawing.
 */
fun DrawScope.drawNineSlice(image: ImageBitmap, insetPx: Int, alpha: Float = 1f) {
  drawNineSliceScaled(image, insetPx, insetPx, alpha)
}

/** Draw [image] with source-corner size [sourceInsetPx] and destination-corner size
 * [destinationInsetPx]. This lets 9-slice art retain a stable physical size across screen
 * densities while cropping the full authored ornaments from the source bitmap. */
fun DrawScope.drawNineSliceScaled(
  image: ImageBitmap,
  sourceInsetPx: Int,
  destinationInsetPx: Int,
  alpha: Float = 1f,
) {
  val iw = image.width
  val ih = image.height
  val sx = min(sourceInsetPx.coerceAtLeast(0), iw / 2)
  val sy = min(sourceInsetPx.coerceAtLeast(0), ih / 2)
  val dw = size.width.roundToInt()
  val dh = size.height.roundToInt()
  if (dw <= 0 || dh <= 0) return

  val requestedDestinationInset = destinationInsetPx.coerceAtLeast(0)
  if (dw < requestedDestinationInset * 2 || dh < requestedDestinationInset * 2) {
    drawImage(image, dstSize = IntSize(dw.coerceAtLeast(1), dh.coerceAtLeast(1)), alpha = alpha)
    return
  }
  val dx = min(requestedDestinationInset, dw / 2)
  val dy = min(requestedDestinationInset, dh / 2)

  val mw = dw - 2 * dx
  val mh = dh - 2 * dy

  // Keep the hot draw path allocation-free: panels/buttons can use many frames per screen.
  drawSlice(image, 0, 0, sx, sy, 0, 0, dx, dy, alpha)
  drawSlice(image, sx, 0, iw - 2 * sx, sy, dx, 0, mw, dy, alpha)
  drawSlice(image, iw - sx, 0, sx, sy, dw - dx, 0, dx, dy, alpha)
  drawSlice(image, 0, sy, sx, ih - 2 * sy, 0, dy, dx, mh, alpha)
  drawSlice(image, sx, sy, iw - 2 * sx, ih - 2 * sy, dx, dy, mw, mh, alpha)
  drawSlice(image, iw - sx, sy, sx, ih - 2 * sy, dw - dx, dy, dx, mh, alpha)
  drawSlice(image, 0, ih - sy, sx, sy, 0, dh - dy, dx, dy, alpha)
  drawSlice(image, sx, ih - sy, iw - 2 * sx, sy, dx, dh - dy, mw, dy, alpha)
  drawSlice(image, iw - sx, ih - sy, sx, sy, dw - dx, dh - dy, dx, dy, alpha)
}

private fun DrawScope.drawSlice(
  image: ImageBitmap,
  srcX: Int,
  srcY: Int,
  srcWidth: Int,
  srcHeight: Int,
  dstX: Int,
  dstY: Int,
  dstWidth: Int,
  dstHeight: Int,
  alpha: Float,
) {
  if (srcWidth <= 0 || srcHeight <= 0 || dstWidth <= 0 || dstHeight <= 0) return
  drawImage(
    image,
    srcOffset = IntOffset(srcX, srcY),
    srcSize = IntSize(srcWidth, srcHeight),
    dstOffset = IntOffset(dstX, dstY),
    dstSize = IntSize(dstWidth, dstHeight),
    alpha = alpha,
  )
}
