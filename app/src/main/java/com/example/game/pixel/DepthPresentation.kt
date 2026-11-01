package com.example.game.pixel

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.game.map.ObliqueCamera
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Painter contract for Sanctuary's existing Canvas renderer. Only decorative FAR_BACKDROP
 * may receive selective focus. Terrain, depth-sorted actors, foreground occluders and UI
 * keep their original nearest-neighbour rendering. Ground props / building backs still
 * sort by their shared world foot anchor inside the world pass, not by enum ordinal.
 */
internal enum class SanctuaryRenderLayer(val selectiveFocusAllowed: Boolean = false) {
  SKY_ATMOSPHERE,
  FAR_BACKDROP(selectiveFocusAllowed = true),
  TERRAIN,
  LOW_GROUND_PROPS,
  BUILDING_BACK,
  DEPTH_SORTED_ENTITIES,
  FOREGROUND_OCCLUDERS,
  SCREEN_ATMOSPHERE,
  UI,
}

/**
 * Small, cached presentation passes, compatible with Android Canvas back to API 24.
 *
 * The only focus input is original decorative island art authored below at 128 x 96.
 * Its radius-one, 24% mixed filter is computed once, in premultiplied colour space.
 * No scene capture, RenderEffect, runtime shader or fullscreen intermediate is used.
 * A normal frame allocates no Bitmap, Path, Paint, gradient or filter here.
 */
internal object DepthPresentation {
  enum class Quality { OFF, REDUCED, BALANCED }
  enum class LightTint { WARM_AMBER, COOL_AETHER, WINDOW_GOLD }

  /** Internal graphics/test control. It never changes scene state or art resolution. */
  var quality: Quality = Quality.BALANCED
  /** Independent control for exact sharp-plane comparisons while lighting stays identical. */
  var selectiveFocusEnabled: Boolean = true

  private const val ART_WIDTH = 128
  private const val ART_HEIGHT = 96
  private const val STAMP_SIZE = 48
  private const val FOCUS_MIX = .24f

  private data class BackdropArt(val original: ImageBitmap, val focused: ImageBitmap)
  private data class SkyIsle(
    val x: Float, val y: Float, val radiusX: Float, val radiusY: Float, val pines: Boolean = false,
  )

  // The original decorative anchors are retained; these are never collision or navigation data.
  private val islands = arrayOf(
    SkyIsle(90f, 520f, 165f, 68f),
    SkyIsle(1280f, 60f, 190f, 72f),
    SkyIsle(3090f, 260f, 225f, 88f, pines = true),
    SkyIsle(2660f, 150f, 85f, 38f),
    SkyIsle(140f, 2480f, 115f, 48f),
    SkyIsle(3060f, 2420f, 175f, 68f),
  )
  private val openIsland by lazy { makeBackdropArt(pines = false) }
  private val pineIsland by lazy { makeBackdropArt(pines = true) }
  private val warmStamp by lazy { makeLightStamp(0xffffc17d.toInt()) }
  private val coolStamp by lazy { makeLightStamp(0xff75c9d7.toInt()) }
  private val windowStamp by lazy { makeLightStamp(0xffefbb7f.toInt()) }

  /**
   * Call after sky / clouds and before any playable island cliff, ground or entity draw.
   * OFF and REDUCED retain crisp, identical decoration; BALANCED selects the cached
   * slightly softened copy. There is still exactly one image draw per visible island.
   */
  fun DrawScope.drawDistantFocus(camX: Float, camY: Float, zoom: Float, timeSec: Float) {
    if (zoom <= 0f) return
    val focused = selectiveFocusEnabled && quality == Quality.BALANCED
    for (i in islands.indices) {
      val isle = islands[i]
      val bob = sin(timeSec * .9f + i * 1.3f) * 8f
      val x = ObliqueCamera.worldToScreenX(isle.x, camX, zoom, size.width / 2f)
      val y = ObliqueCamera.worldToScreenY(isle.y, camY, zoom, size.height / 2f, bob)
      // Raster (64,31) is the top-plane/base anchor. Upright pines keep full height.
      val pixelX = isle.radiusX * zoom / 56f
      val pixelY = isle.radiusY * zoom / 24f
      val left = x - 64f * pixelX
      val top = y - 31f * pixelY
      val width = ART_WIDTH * pixelX
      val height = ART_HEIGHT * pixelY
      if (left + width < 0f || top + height < 0f || left > size.width || top > size.height) continue
      val art = if (isle.pines) pineIsland else openIsland
      drawImage(
        image = if (focused) art.focused else art.original,
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1)),
        filterQuality = FilterQuality.None,
      )
    }
  }

  /**
   * Bounded terrain spill or emissive halo, never a screen overlay. Colour and radial
   * falloff live in three reused 48px textures instead of rebuilding radial gradients.
   * The stamp uses normal alpha compositing, so it cannot produce additive white bloom.
   */
  fun DrawScope.drawLightStamp(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    tint: LightTint,
    alpha: Float,
  ) {
    if (quality == Quality.OFF || alpha <= 0f || radiusX <= 0f || radiusY <= 0f) return
    val rx = radiusX.coerceAtMost(180f)
    val ry = radiusY.coerceAtMost(180f)
    if (center.x + rx < 0f || center.x - rx > size.width || center.y + ry < 0f || center.y - ry > size.height) return
    val image = when (tint) {
      LightTint.WARM_AMBER -> warmStamp
      LightTint.COOL_AETHER -> coolStamp
      LightTint.WINDOW_GOLD -> windowStamp
    }
    drawImage(
      image = image,
      dstOffset = IntOffset((center.x - rx).roundToInt(), (center.y - ry).roundToInt()),
      dstSize = IntSize((rx * 2f).roundToInt().coerceAtLeast(1), (ry * 2f).roundToInt().coerceAtLeast(1)),
      alpha = alpha.coerceIn(0f, .22f),
      filterQuality = FilterQuality.Low,
    )
  }

  private fun makeBackdropArt(pines: Boolean): BackdropArt {
    val pixels = IntArray(ART_WIDTH * ART_HEIGHT)
    fun rect(x: Int, y: Int, w: Int, h: Int, color: Int) {
      for (py in y.coerceAtLeast(0) until (y + h).coerceAtMost(ART_HEIGHT)) {
        for (px in x.coerceAtLeast(0) until (x + w).coerceAtMost(ART_WIDTH)) pixels[py * ART_WIDTH + px] = color
      }
    }
    fun polygon(color: Int, vararg xy: Int) {
      for (y in 0 until ART_HEIGHT) {
        for (x in 0 until ART_WIDTH) {
          var inside = false
          var j = xy.size - 2
          var i = 0
          while (i < xy.size) {
            val xi = xy[i]; val yi = xy[i + 1]
            val xj = xy[j]; val yj = xy[j + 1]
            if ((yi > y) != (yj > y) && x < (xj - xi).toFloat() * (y - yi) / (yj - yi) + xi) inside = !inside
            j = i
            i += 2
          }
          if (inside) pixels[y * ART_WIDTH + x] = color
        }
      }
    }
    fun ellipse(cx: Int, cy: Int, rx: Int, ry: Int, color: Int) {
      for (y in (cy - ry).coerceAtLeast(0)..(cy + ry).coerceAtMost(ART_HEIGHT - 1)) {
        for (x in (cx - rx).coerceAtLeast(0)..(cx + rx).coerceAtMost(ART_WIDTH - 1)) {
          val dx = (x - cx).toFloat() / rx
          val dy = (y - cy).toFloat() / ry
          if (dx * dx + dy * dy <= 1f) pixels[y * ART_WIDTH + x] = color
        }
      }
    }
    // Authored, quiet colour clusters: three rock planes, broken strata and a moss shelf.
    // Cool atmospheric values belong only to this far artwork, not to the sharp world.
    polygon(0xff353747.toInt(), 12,31, 35,66, 59,83, 95,68, 116,31)
    polygon(0xff414555.toInt(), 12,31, 60,35, 59,83, 35,66)
    polygon(0xff292c3e.toInt(), 60,35, 116,31, 95,68, 59,83)
    polygon(0xff353b4c.toInt(), 61,43, 96,37, 89,55, 65,68)
    polygon(0xff505565.toInt(), 22,37, 48,43, 43,55, 34,52)
    rect(34,55,9,2,0xff393e4f.toInt())
    rect(43,66,9,2,0xff474b5b.toInt())
    rect(74,60,9,2,0xff262a3a.toInt())
    ellipse(64,31,56,10,0xff273a3d.toInt())
    ellipse(64,27,54,8,0xff3e5350.toInt())
    ellipse(47,24,29,4,0xff4d605a.toInt())
    ellipse(91,28,22,5,0xff344948.toInt())
    rect(24,29,11,2,0xff637168.toInt())
    rect(38,22,16,2,0xff617268.toInt())
    rect(56,31,16,2,0xff334849.toInt())
    rect(94,30,9,2,0xff53635d.toInt())
    // Tiny fallen masonry and grounded shrubs give the silhouettes scale without noise.
    rect(42,18,6,7,0xff677278.toInt())
    rect(48,21,8,4,0xff4d5e65.toInt())
    rect(42,17,7,2,0xff7a8385.toInt())
    ellipse(77,22,8,3,0xff34494a.toInt())
    ellipse(84,23,6,3,0xff405654.toInt())
    if (pines) {
      fun pine(x: Int, base: Int, height: Int) {
        rect(x - 1, base - 6, 2, 8, 0xff343c45.toInt())
        polygon(0xff293e43.toInt(), x,base-height, x-7,base-6, x+7,base-6)
        polygon(0xff314a4c.toInt(), x-1,base-height+3, x-7,base-6, x,base-8)
        polygon(0xff354e50.toInt(), x,base-height+3, x-5,base-11, x+4,base-11)
      }
      pine(48,26,19)
      pine(62,24,23)
      pine(81,28,17)
    }
    return BackdropArt(imageFrom(pixels, ART_WIDTH, ART_HEIGHT), imageFrom(soften(pixels), ART_WIDTH, ART_HEIGHT))
  }

  /** Premultiplied 3x3 tent, then a restrained mix back into the original pixel art. */
  private fun soften(source: IntArray): IntArray {
    val result = IntArray(source.size)
    for (y in 0 until ART_HEIGHT) for (x in 0 until ART_WIDTH) {
      var aSum = 0
      var rSum = 0
      var gSum = 0
      var bSum = 0
      for (dy in -1..1) for (dx in -1..1) {
        val nx = x + dx; val ny = y + dy
        if (nx !in 0 until ART_WIDTH || ny !in 0 until ART_HEIGHT) continue
        val weight = (if (dx == 0) 2 else 1) * (if (dy == 0) 2 else 1)
        val c = source[ny * ART_WIDTH + nx]
        val a = (c ushr 24) * weight
        aSum += a
        rSum += (c ushr 16 and 255) * a
        gSum += (c ushr 8 and 255) * a
        bSum += (c and 255) * a
      }
      val c = source[y * ART_WIDTH + x]
      val originalA = (c ushr 24).toFloat()
      val blurredA = aSum / 16f
      val a = originalA * (1f - FOCUS_MIX) + blurredA * FOCUS_MIX
      if (a < .5f) continue
      fun mixedChannel(shift: Int, sum: Int): Int {
        val originalPremultiplied = (c ushr shift and 255) * originalA
        val blurredPremultiplied = sum / 16f
        return ((originalPremultiplied * (1f - FOCUS_MIX) + blurredPremultiplied * FOCUS_MIX) / a).roundToInt().coerceIn(0, 255)
      }
      result[y * ART_WIDTH + x] = (a.roundToInt().coerceIn(0, 255) shl 24) or
        (mixedChannel(16, rSum) shl 16) or (mixedChannel(8, gSum) shl 8) or mixedChannel(0, bSum)
    }
    return result
  }

  private fun makeLightStamp(rgb: Int): ImageBitmap {
    val pixels = IntArray(STAMP_SIZE * STAMP_SIZE)
    val half = (STAMP_SIZE - 1) / 2f
    for (y in 0 until STAMP_SIZE) for (x in 0 until STAMP_SIZE) {
      val dx = (x - half) / half; val dy = (y - half) / half
      val radius = sqrt(dx * dx + dy * dy)
      val falloff = (1f - radius * radius).coerceAtLeast(0f)
      val a = (255f * falloff * falloff * falloff).roundToInt().coerceIn(0, 255)
      pixels[y * STAMP_SIZE + x] = (a shl 24) or (rgb and 0x00ffffff)
    }
    return imageFrom(pixels, STAMP_SIZE, STAMP_SIZE)
  }

  private fun imageFrom(pixels: IntArray, width: Int, height: Int): ImageBitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
