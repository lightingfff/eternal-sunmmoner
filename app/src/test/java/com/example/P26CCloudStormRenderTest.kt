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
 * P26C ROUND 1 render evidence: visible clouds + STORM mood through the REAL engine draw path.
 *
 * Same production draw path as P26SkyRenderTest (drawSanctuaryFloatingWorld + drawGroundShadows +
 * depth-sorted drawProp + full lobby post pass). Cameras from thresholds: service_z088 (960,860,0.88),
 * gate_z148 (1960,310,1.48), overview_z036 (1600,1380,0.36).
 *
 * These are Robolectric native-graphics renders, NOT contact sheets, NOT emulator screenshots,
 * NOT device performance measurements.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P26CCloudStormRenderTest {

  private val outDir = File("build/visual_screenshots/p26c")

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
  fun p26cCloudStormMatrixThroughTheRealRenderer() {
    outDir.mkdirs()
    val originalQuality = DepthPresentation.quality
    val props = org.json.JSONArray()
    try {
      // A. Morning CLEAR (7h) gameplay z0.88 — phải có mây và trời xanh.
      prepare(7f, WorldWeather.CLEAR)
      DepthPresentation.quality = DepthPresentation.Quality.BALANCED
      render("p26c_morning_clear_z088", 960f, 860f, 0.88f)
      record(props, "p26c_morning_clear_z088", 960f, 860f, 0.88f, 0f, 7f, "CLEAR", "BALANCED")

      // B. Noon CLEAR (12h) gameplay z0.88 — sáng nhất, có ít nhất một mây sáng có cấu trúc.
      prepare(12f, WorldWeather.CLEAR)
      render("p26c_noon_clear_z088", 960f, 860f, 0.88f)
      record(props, "p26c_noon_clear_z088", 960f, 860f, 0.88f, 0f, 12f, "CLEAR", "BALANCED")

      // C. Noon CLOUDY z0.88 — mây dày hơn và ánh nắng yếu đi.
      prepare(12f, WorldWeather.CLOUDY)
      render("p26c_noon_cloudy_z088", 960f, 860f, 0.88f)
      record(props, "p26c_noon_cloudy_z088", 960f, 860f, 0.88f, 0f, 12f, "CLOUDY", "BALANCED")

      // D. Noon RAIN z0.88 — sắc trời âm, mưa hợp lý.
      prepare(12f, WorldWeather.RAIN)
      render("p26c_noon_rain_z088", 960f, 860f, 0.88f)
      record(props, "p26c_noon_rain_z088", 960f, 860f, 0.88f, 0f, 12f, "RAIN", "BALANCED")

      // E. Noon STORM z0.88 — có vùng trời giông, mây rõ, không chỉ mờ/tối cả ảnh.
      prepare(12f, WorldWeather.STORM)
      render("p26c_noon_storm_z088", 960f, 860f, 0.88f)
      record(props, "p26c_noon_storm_z088", 960f, 860f, 0.88f, 0f, 12f, "STORM", "BALANCED")

      // F. Evening CLEAR (18.5h) z0.88 — mây bắt ánh ấm.
      prepare(18.5f, WorldWeather.CLEAR)
      render("p26c_evening_clear_z088", 960f, 860f, 0.88f)
      record(props, "p26c_evening_clear_z088", 960f, 860f, 0.88f, 0f, 18.5f, "CLEAR", "BALANCED")

      // G. Night CLEAR (0h) z0.88 — mây mỏng, đèn/gate nổi bật.
      prepare(0f, WorldWeather.CLEAR)
      render("p26c_night_clear_z088", 960f, 860f, 0.88f)
      record(props, "p26c_night_clear_z088", 960f, 860f, 0.88f, 0f, 0f, "CLEAR", "BALANCED")

      // H. Night STORM (0h) z0.88 — local light rõ, mưa/sương được kiểm soát.
      prepare(0f, WorldWeather.STORM)
      render("p26c_night_storm_z088", 960f, 860f, 0.88f)
      record(props, "p26c_night_storm_z088", 960f, 860f, 0.88f, 0f, 0f, "STORM", "BALANCED")

      // I. Gate z1.48 x hour+weather relevant — hốc portal phải giữ tối, halo không che kiến trúc.
      prepare(0f, WorldWeather.STORM)
      render("p26c_gate_night_storm_z148", 1960f, 310f, 1.48f)
      record(props, "p26c_gate_night_storm_z148", 1960f, 310f, 1.48f, 0f, 0f, "STORM", "BALANCED")

      prepare(12f, WorldWeather.CLEAR)
      render("p26c_gate_noon_clear_z148", 1960f, 310f, 1.48f)
      record(props, "p26c_gate_noon_clear_z148", 1960f, 310f, 1.48f, 0f, 12f, "CLEAR", "BALANCED")

      // J. Island overview z0.36 — trời, đảo, mái, cây và HUD không bị cháy sáng.
      prepare(12f, WorldWeather.CLEAR)
      render("p26c_overview_noon_z036", 1600f, 1380f, 0.36f)
      record(props, "p26c_overview_noon_z036", 1600f, 1380f, 0.36f, 0f, 12f, "CLEAR", "BALANCED")

      // K. Cloud-visible bookmark z1.0 + z1.48 (nhìn thấy trời) — kiểm tra scaling/độ hạt và culling.
      prepare(7f, WorldWeather.CLEAR)
      render("p26c_cloud_bookmark_z100", 960f, 860f, 1.00f)
      record(props, "p26c_cloud_bookmark_z100", 960f, 860f, 1.00f, 0f, 7f, "CLEAR", "BALANCED")

      prepare(7f, WorldWeather.CLEAR)
      render("p26c_cloud_bookmark_z148", 960f, 860f, 1.48f)
      record(props, "p26c_cloud_bookmark_z148", 960f, 860f, 1.48f, 0f, 7f, "CLEAR", "BALANCED")

      // Gate is a normal gameplay camera that still exposes sky at z1.48; the service
      // close-up above contains buildings edge-to-edge and is a negative occlusion control.
      prepare(7f, WorldWeather.CLEAR)
      render("p26c_gate_cloud_bookmark_z148", 1960f, 310f, 1.48f)
      record(props, "p26c_gate_cloud_bookmark_z148", 1960f, 310f, 1.48f, 0f, 7f, "CLEAR", "BALANCED")

      // L. Motion A/B timeSec 0, 4, 8 tại cùng camera/hour/weather (động mây thật); frame t=0 lặp hai lần phải byte-identical.
      prepare(12f, WorldWeather.CLEAR)
      render("p26c_motion_t0", 960f, 860f, 0.88f, 0f)
      render("p26c_motion_t4", 960f, 860f, 0.88f, 4f)
      render("p26c_motion_t8", 960f, 860f, 0.88f, 8f)
      record(props, "p26c_motion_t0", 960f, 860f, 0.88f, 0f, 12f, "CLEAR", "BALANCED")
      record(props, "p26c_motion_t4", 960f, 860f, 0.88f, 4f, 12f, "CLEAR", "BALANCED")
      record(props, "p26c_motion_t8", 960f, 860f, 0.88f, 8f, 12f, "CLEAR", "BALANCED")

      // Determinism: same camera/hour/weather/timeSec=0 rendered twice must be byte-identical.
      val first = File(outDir, "p26c_noon_clear_z088.png").readBytes()
      render("p26c_noon_clear_z088_det", 960f, 860f, 0.88f, 0f)
      val second = File(outDir, "p26c_noon_clear_z088_det.png").readBytes()
      require(first.contentEquals(second)) { "P26C cloud render must be deterministic at timeSec 0" }

    } finally {
      DepthPresentation.quality = originalQuality
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
    File(outDir, "camera_props.json").writeText(props.toString(2))
  }
}
