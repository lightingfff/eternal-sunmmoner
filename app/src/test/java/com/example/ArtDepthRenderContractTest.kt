package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.HeroClassType
import com.example.game.map.ObliqueCamera
import com.example.game.map.PathSegment
import com.example.game.pixel.AuthoredHeroAtlas
import com.example.game.pixel.MeadowRoadArt
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawCharacterShadow
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import java.io.File
import kotlin.math.hypot
import kotlin.math.roundToInt
import org.junit.Assert.*
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtDepthRenderContractTest {
  @After fun clearEnvironmentOverride() { WorldEnvironment.debugOverride() }

  @Test fun roundedRoadJunctionsHaveNoWindingHolesAtGameplayZooms() {
    val paths = listOf(PathSegment(150f, 200f, 450f, 200f, 80f, 0, 0),
      PathSegment(450f, 200f, 600f, 400f, 80f, 0, 0),
      PathSegment(450f, 100f, 450f, 200f, 80f, 0, 0),
      PathSegment(250f, 350f, 250f, 350f, 80f, 0, 0))
    val original = paths.toList()
    val tile = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFFB58A5C.toInt()) }
    try {
      for (zoom in listOf(.36f, .88f, 1.48f)) {
        val bitmap = Bitmap.createBitmap(800, 360, Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(800f, 360f)) {
          drawRect(Color(0xFF203B2F))
          with(MeadowRoadArt) { draw(paths, listOf(tile.asImageBitmap()), 400.375f, 280.625f, zoom, 0..12, 0..8) }
        }
        // Sample interiors of both segments including the overlapping caps/junction.
        for (seg in paths) {
          val dx = seg.endX - seg.startX; val dy = seg.endY - seg.startY
          val length = hypot(dx, dy)
          for (n in 0..30) for (offset in listOf(-20f, 0f, 20f)) {
            val x = seg.startX + dx * n / 30 - dy / length.coerceAtLeast(1f) * offset
            val y = seg.startY + dy * n / 30 + dx / length.coerceAtLeast(1f) * offset
            val sx = ObliqueCamera.worldToScreenX(x, 400.375f, zoom, 400f).roundToInt()
            val sy = ObliqueCamera.worldToScreenY(y, 280.625f, zoom, 180f).roundToInt()
            if (sx in 0 until 800 && sy in 0 until 360) {
              assertEquals("Road hole: zoom=$zoom world=($x,$y)", 0xFFB58A5C.toInt(), bitmap.getPixel(sx, sy))
            }
          }
        }
        val outsideX = ObliqueCamera.worldToScreenX(100f, 400.375f, zoom, 400f).roundToInt()
        val outsideY = ObliqueCamera.worldToScreenY(100f, 280.625f, zoom, 180f).roundToInt()
        if (outsideX in 0 until 800 && outsideY in 0 until 360) {
          assertEquals(0xFF203B2F.toInt(), bitmap.getPixel(outsideX, outsideY))
        }
        bitmap.recycle()
      }
      assertEquals(original, paths)
    } finally { tile.recycle() }
  }

  @Test fun mirroredCasterKeepsTheSameWorldLightDirection() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    WorldEnvironment.debugOverride(12f, WorldWeather.CLEAR)
    WorldEnvironment.update(50_000)
    val frame = requireNotNull(AuthoredHeroAtlas.frame("Kaelen", HeroClassType.KNIGHT,
      false, 0f, 0f, false, 1f, 0f, false, false))
    val centers = mutableListOf<Pair<Double, Double>>()
    for (mirror in listOf(false, true)) {
      val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
      CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(400f, 300f)) {
        drawCharacterShadow(frame.image, 160f, 150f, 1f, frame.worldScale, frame.pivotY, mirror)
      }
      var count = 0L; var xSum = 0L; var ySum = 0L
      for (y in 0 until 300) for (x in 0 until 400) {
        if (bitmap.getPixel(x, y) ushr 24 > 20) { count++; xSum += x; ySum += y }
      }
      assertTrue(count > 100)
      centers += xSum.toDouble() / count to ySum.toDouble() / count
      val path = File("build/visual_screenshots/art-depth-reforge/shadow-mirror-$mirror.png")
      path.parentFile?.mkdirs()
      path.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
      bitmap.recycle()
    }
    val direction = WorldEnvironment.lighting.directionX
    centers.forEach { center -> assertTrue("Mirroring must not reverse the light", (center.first - 160) * direction > 0) }
    assertTrue("Both shadows lean the same way", (centers[0].first - 160) * (centers[1].first - 160) > 0)
    assertEquals(centers[0].second, centers[1].second, 2.0)
  }
}
