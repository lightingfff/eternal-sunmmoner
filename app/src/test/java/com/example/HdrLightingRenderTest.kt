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
import com.example.game.map.PropKind
import com.example.game.pixel.DepthPresentation
import com.example.game.pixel.HdrLightResponse
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P24 REAL ENGINE RENDER evidence for the HDR-style light response.
 *
 * All captures go through the production draw path (`drawSanctuaryFloatingWorld` +
 * `drawGroundShadows` + depth-sorted `drawProp`), the same path used by
 * `MapForgeP23RenderTest`. Cameras are locked to the P23 bookmarks so OFF / REDUCED /
 * BALANCED / day / night / rain are directly comparable and the 24 P23 frames can be
 * diffed against their pre-P24 baseline.
 *
 * These are Robolectric native-graphics renders, NOT contact sheets, NOT emulator
 * screenshots and NOT device performance measurements.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HdrLightingRenderTest {

  private val outDir = File("build/visual_screenshots/p23")

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
    }
    val file = File(outDir, "$name.png")
    file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    require(file.length() > 20_000) { "$name.png must be a real render (${file.length()} B)" }
  }

  @Test
  fun hdrTiersAndWeatherThroughTheRealRenderer() {
    outDir.mkdirs()
    val originalQuality = DepthPresentation.quality
    try {
      // Service district at gameplay zoom 0.88 — same camera as service_cluster_z088.
      prepare(12f, WorldWeather.CLEAR)
      DepthPresentation.quality = DepthPresentation.Quality.OFF
      render("hdr_off_z088", 960f, 860f, 0.88f)
      DepthPresentation.quality = DepthPresentation.Quality.REDUCED
      render("hdr_reduced_z088", 960f, 860f, 0.88f)
      DepthPresentation.quality = DepthPresentation.Quality.BALANCED
      render("hdr_balanced_day_z088", 960f, 860f, 0.88f)

      prepare(0f, WorldWeather.CLEAR)
      render("hdr_balanced_night_z088", 960f, 860f, 0.88f)
      prepare(0f, WorldWeather.RAIN)
      render("hdr_balanced_night_rain_z088", 960f, 860f, 0.88f)

      // Tower Gate at 1.00 and 1.48 — the strongest cold source and the joint check.
      prepare(0f, WorldWeather.CLEAR)
      render("hdr_balanced_night_tower_gate_z100", 1960f, 460f, 1.00f, 0.25f)
      render("hdr_balanced_night_tower_gate_z148", 1960f, 310f, 1.48f)

      // Whole-island read at 0.36 — plaza focal point must be visible without a glow flood.
      render("hdr_balanced_night_overview_z036", 1600f, 1380f, 0.36f)
    } finally {
      DepthPresentation.quality = originalQuality
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
  }

  @Test
  fun cachedMasksAreReusedAndExposureIsBounded() {
    // Every tint resolves to the same cached ImageBitmap: no per-frame bitmap decode.
    for (tint in HdrLightResponse.Tint.values()) HdrLightResponse.maskFor(tint)
    val built = HdrLightResponse.cachedMaskCount
    for (tint in HdrLightResponse.Tint.values()) HdrLightResponse.maskFor(tint)
    assertEquals("masks must not be rebuilt", built, HdrLightResponse.cachedMaskCount)
    assertEquals("exactly one cached mask per tint", 7, built)

    val context = RuntimeEnvironment.getApplication()
    WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(12f, WorldWeather.CLEAR)
    WorldEnvironment.update(50000)
    val day = HdrLightResponse.exposureBalance()
    WorldEnvironment.debugOverride(0f, WorldWeather.CLEAR)
    WorldEnvironment.update(50000)
    val night = HdrLightResponse.exposureBalance()
    WorldEnvironment.debugOverride(12f, WorldWeather.RAIN)
    WorldEnvironment.update(50000)
    val rain = HdrLightResponse.exposureBalance()
    WorldEnvironment.debugOverride()
    WorldEnvironment.update()

    assertTrue("night local lights must be stronger than day: $night <= $day", night > day)
    assertTrue("rain local lights must be stronger than clear day: $rain <= $day", rain > day)
    assertTrue("exposure must stay bounded", day >= 0.80f && night <= 1.65f && rain <= 1.65f)

    // No invented light source on non-emitting props; buildings keep their grading.
    assertEquals(null, HdrLightResponse.specFor(PropKind.TALL_GRASS_CLUMP))
    assertEquals(null, HdrLightResponse.specFor(PropKind.WOODEN_BENCH_34))
    assertTrue(HdrLightResponse.specFor(PropKind.STREET_LANTERN) != null)
    assertTrue(HdrLightResponse.isBuilding(PropKind.FACILITY_ARMORY))
  }
}
