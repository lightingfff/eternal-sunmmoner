package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.pixel.DepthPresentation
import com.example.game.pixel.LivingWorldRenderer
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
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
 * P26 ROUND 1 REAL ENGINE RENDER evidence for the sky / day-night / weather overhaul.
 *
 * Same production draw path as `HdrLightingRenderTest` (drawSanctuaryFloatingWorld +
 * drawGroundShadows + depth-sorted drawProp + the full lobby post pass), locked cameras
 * from `thresholds.json`, hours MORNING / NOON / EVENING / NIGHT and weather CLEAR /
 * CLOUDY / RAIN so the states are directly comparable.
 *
 * These are Robolectric native-graphics renders, NOT contact sheets, NOT emulator
 * screenshots and NOT device performance measurements.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P26SkyRenderTest {

  private val outDir = File("build/visual_screenshots/p26")

  private fun prepare(hour: Float, weather: WorldWeather) {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(hour, weather)
    WorldEnvironment.update(50000)
  }

  private fun render(name: String, camX: Float, camY: Float, zoom: Float, timeSec: Float = 0f) {
    val layout = SanctuaryLobbyEngine().mapLayout
    val w = 1280
    val h = 720
    val image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
    CanvasDrawScope().draw(
      Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat()),
    ) {
      drawSanctuaryFloatingWorld(camX, camY, zoom, timeSec, layout.paths)
      drawGroundShadows(layout.obstacles, camX, camY, zoom, clipToIsland = true)
      for (obs in layout.obstacles.sortedBy { it.y + it.sortYBias }) {
        drawProp(obs, camX, camY, zoom, timeSec, renderShadow = null)
      }
      with(LivingWorldRenderer) {
        drawWorldGrade()
        drawLocalLights(layout.obstacles, camX, camY, zoom)
        drawWeather(camX, camY, zoom, foreground = true)
      }
    }
    val file = File(outDir, "$name.png")
    file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    require(file.length() > 20_000) { "$name.png must be a real render (${file.length()} B)" }
  }

  private fun record(
    props: org.json.JSONArray,
    name: String, camX: Float, camY: Float, zoom: Float,
    timeSec: Float, hour: Float, weather: String, tier: String,
  ) {
    props.put(
      JSONObject()
        .put("file", "$name.png")
        .put("camX", camX.toDouble())
        .put("camY", camY.toDouble())
        .put("zoom", zoom.toDouble())
        .put("timeSec", timeSec.toDouble())
        .put("hour", hour.toDouble())
        .put("weather", weather)
        .put("tier", tier)
    )
  }

  @Test
  fun skyMatrixThroughTheRealRenderer() {
    outDir.mkdirs()
    val originalQuality = DepthPresentation.quality
    val props = org.json.JSONArray()
    try {
      // Service district at gameplay zoom 0.88 — morning / noon / evening / night, CLEAR.
      prepare(7f, WorldWeather.CLEAR)
      DepthPresentation.quality = DepthPresentation.Quality.BALANCED
      render("p26_sky_morning_z088", 960f, 860f, 0.88f)
      record(props, "p26_sky_morning_z088", 960f, 860f, 0.88f, 0f, 7f, "CLEAR", "BALANCED")

      prepare(12f, WorldWeather.CLEAR)
      render("p26_sky_noon_z088", 960f, 860f, 0.88f)
      record(props, "p26_sky_noon_z088", 960f, 860f, 0.88f, 0f, 12f, "CLEAR", "BALANCED")

      prepare(18.5f, WorldWeather.CLEAR)
      render("p26_sky_evening_z088", 960f, 860f, 0.88f)
      record(props, "p26_sky_evening_z088", 960f, 860f, 0.88f, 0f, 18.5f, "CLEAR", "BALANCED")

      prepare(0f, WorldWeather.CLEAR)
      render("p26_sky_night_z088", 960f, 860f, 0.88f)
      record(props, "p26_sky_night_z088", 960f, 860f, 0.88f, 0f, 0f, "CLEAR", "BALANCED")

      // Weather mood at the same camera and hour (noon).
      prepare(12f, WorldWeather.CLOUDY)
      render("p26_sky_noon_cloudy_z088", 960f, 860f, 0.88f)
      record(props, "p26_sky_noon_cloudy_z088", 960f, 860f, 0.88f, 0f, 12f, "CLOUDY", "BALANCED")

      prepare(12f, WorldWeather.RAIN)
      render("p26_sky_noon_rain_z088", 960f, 860f, 0.88f)
      record(props, "p26_sky_noon_rain_z088", 960f, 860f, 0.88f, 0f, 12f, "RAIN", "BALANCED")

      // Tower Gate close-ups — the portal drama check (rift dark, halo at night).
      prepare(0f, WorldWeather.CLEAR)
      render("p26_gate_night_z148", 1960f, 310f, 1.48f)
      record(props, "p26_gate_night_z148", 1960f, 310f, 1.48f, 0f, 0f, "CLEAR", "BALANCED")

      prepare(12f, WorldWeather.CLEAR)
      render("p26_gate_noon_z148", 1960f, 310f, 1.48f)
      record(props, "p26_gate_noon_z148", 1960f, 310f, 1.48f, 0f, 12f, "CLEAR", "BALANCED")

      prepare(0f, WorldWeather.RAIN)
      render("p26_gate_night_rain_z148", 1960f, 310f, 1.48f)
      record(props, "p26_gate_night_rain_z148", 1960f, 310f, 1.48f, 0f, 0f, "RAIN", "BALANCED")

      // Whole-island night read.
      render("p26_overview_night_z036", 1600f, 1380f, 0.36f)
      record(props, "p26_overview_night_z036", 1600f, 1380f, 0.36f, 0f, 0f, "CLEAR", "BALANCED")

      // Determinism: same camera/hour/weather rendered twice at timeSec 0 must be byte-identical.
      val first = File(outDir, "p26_sky_noon_z088.png").readBytes()
      render("p26_sky_noon_z088_det", 960f, 860f, 0.88f)
      val second = File(outDir, "p26_sky_noon_z088_det.png").readBytes()
      require(first.contentEquals(second)) { "P26 sky render must be deterministic at timeSec 0" }
    } finally {
      DepthPresentation.quality = originalQuality
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
    File(outDir, "camera_props.json").writeText(props.toString(2))
  }
}
