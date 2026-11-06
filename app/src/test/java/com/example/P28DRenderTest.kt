package com.example

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.map.PropKind
import com.example.game.pixel.LivingWorldRenderer
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.PixelSpriteRenderer.drawSanctuaryZoneFloorRing
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.tower.TowerFloorCatalog
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import java.io.File
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P28-D render evidence + indicative frame-time probe for the Sanctuary buildings
 * (real footprint foundations, contact grounding). Captures the REAL world renderer
 * (native Skia via Robolectric) at multiple zooms and day/night around the facility
 * cluster. Frame times here are Robolectric CPU numbers — indicative only; device FPS
 * is reported as NOT TESTED ON PHYSICAL ANDROID.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P28DRenderTest {
  private val outDir = File("build/visual_screenshots/p28d/world")
  private val width = 1280
  private val height = 720

  private fun renderFrame(x: Float, y: Float, zoom: Float, hour: Float, weather: WorldWeather = WorldWeather.CLEAR): ImageBitmap {
    WorldEnvironment.debugOverride(hour, weather)
    WorldEnvironment.update(50000)
    val layout = SanctuaryLobbyEngine().mapLayout
    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat())) {
      drawSanctuaryFloatingWorld(x, y, zoom, 0f, layout.paths)
      TowerFloorCatalog.SANCTUARY_ZONES.forEach { zone ->
        drawSanctuaryZoneFloorRing(zone, false, x, y, zoom, 0f)
      }
      drawGroundShadows(layout.obstacles, x, y, zoom, clipToIsland = true)
      for (obs in layout.obstacles.sortedBy { it.y + it.sortYBias }) {
        drawProp(obs, x, y, zoom, 0f, renderShadow = null)
      }
      with(LivingWorldRenderer) {
        drawWorldGrade()
        drawLocalLights(layout.obstacles, x, y, zoom)
        drawWeather(x, y, zoom, foreground = true)
      }
    }
    return image
  }

  private fun capture(name: String, x: Float, y: Float, zoom: Float, hour: Float) {
    val bmp = renderFrame(x, y, zoom, hour).asAndroidBitmap()
    val file = File(outDir, "$name.png")
    file.parentFile?.mkdirs()
    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    require(file.length() > 20_000) { "$name: not a real nonempty renderer image" }
  }

  @Test fun structureFoundationsMultiZoomDayNight() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    val layout = SanctuaryLobbyEngine().mapLayout
    val hall = layout.obstacles.first { it.propKind == PropKind.FACILITY_SUMMONING_HALL }
    val quarters = layout.obstacles.first { it.propKind == PropKind.FACILITY_HERO_QUARTERS }
    // Summoning hall plaza: zoom out / mid / close, day and night.
    capture("p28d_hall_day_z036", hall.x, hall.y - 60f, 0.36f, 12f)
    capture("p28d_hall_day_z088", hall.x, hall.y - 40f, 0.88f, 12f)
    capture("p28d_hall_day_z148", hall.x, hall.y - 20f, 1.48f, 12f)
    capture("p28d_hall_night_z088", hall.x, hall.y - 40f, 0.88f, 23f)
    capture("p28d_hall_night_z148", hall.x, hall.y - 20f, 1.48f, 23f)
    // Hero quarters (cottage-scale building) close-up.
    capture("p28d_quarters_day_z148", quarters.x, quarters.y - 20f, 1.48f, 15f)
    capture("p28d_quarters_night_z148", quarters.x, quarters.y - 20f, 1.48f, 23f)
    // Wide overview for the whole plaza with all foundations.
    capture("p28d_plaza_day_z036", hall.x, hall.y - 160f, 0.36f, 12f)
    capture("p28d_plaza_night_z036", hall.x, hall.y - 160f, 0.36f, 23f)
  }

  @Test fun indicativeFrameTimeProbe() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    val layout = SanctuaryLobbyEngine().mapLayout
    val hall = layout.obstacles.first { it.propKind == PropKind.FACILITY_SUMMONING_HALL }
    val frames = mutableListOf<Long>()
    // Warm up JIT/asset caches, then measure 12 frames at mid zoom.
    repeat(3) { renderFrame(hall.x, hall.y - 40f, 0.88f, 12f) }
    repeat(12) {
      val t0 = System.nanoTime()
      renderFrame(hall.x, hall.y - 40f, 0.88f, 12f)
      frames += (System.nanoTime() - t0) / 1_000_000
    }
    val sorted = frames.sorted()
    val avg = frames.average()
    val p95 = sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)]
    val report = JSONObject()
      .put("note", "Robolectric CPU render, indicative only — NOT a device FPS measurement")
      .put("frames", frames.size)
      .put("avgMs", avg)
      .put("p95Ms", p95)
      .put("minMs", sorted.first())
      .put("maxMs", sorted.last())
      .put("zoom", 0.88f).put("viewport", "1280x720").put("hour", 12f)
    val f = File(outDir, "perf_probe.json")
    f.parentFile?.mkdirs()
    f.writeText(report.toString(2))
    println("P28D perf probe: avg=${avg}ms p95=${p95}ms min=${sorted.first()}ms max=${sorted.last()}ms (Robolectric CPU, indicative)")
  }
}
