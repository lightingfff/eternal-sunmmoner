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
import com.example.game.pixel.PixelSpriteRenderer.drawSanctuaryZoneFloorRing
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.tower.TowerFloorCatalog
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P27B-V5 render evidence. Same real renderer as [P27BRenderTest] (native Skia,
 * real SanctuaryWorldRenderer, real shadows/props/grade), but a wider camera matrix:
 * the four zoom levels the work order asks for, plus the hour sweep needed to judge a
 * landmark's night response instead of guessing from one night frame.
 *
 * Writes to docs/p27b_next/previews/<phase>/ so the merged P27B baseline under
 * docs/p27b/ stays byte-identical as a reference.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P27BNextRenderTest {
  private val outDir = File("build/visual_screenshots/p27b_next/world")
  private val width = 1280
  private val height = 720

  private data class Shot(
    val name: String,
    val x: Float,
    val y: Float,
    val zoom: Float,
    val hour: Float,
    val weather: WorldWeather = WorldWeather.CLEAR,
    val note: String = "",
  )

  private fun render(shot: Shot) {
    WorldEnvironment.debugOverride(shot.hour, shot.weather)
    WorldEnvironment.update(50000)
    val layout = SanctuaryLobbyEngine().mapLayout
    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat())) {
      drawSanctuaryFloatingWorld(shot.x, shot.y, shot.zoom, 0f, layout.paths)
      TowerFloorCatalog.SANCTUARY_ZONES.forEach { zone ->
        drawSanctuaryZoneFloorRing(zone, false, shot.x, shot.y, shot.zoom, 0f)
      }
      drawGroundShadows(layout.obstacles, shot.x, shot.y, shot.zoom, clipToIsland = true)
      for (obs in layout.obstacles.sortedBy { it.y + it.sortYBias }) {
        drawProp(obs, shot.x, shot.y, shot.zoom, 0f, renderShadow = null)
      }
      with(LivingWorldRenderer) {
        drawWorldGrade()
        drawLocalLights(layout.obstacles, shot.x, shot.y, shot.zoom)
        drawWeather(shot.x, shot.y, shot.zoom, foreground = true)
      }
    }
    val file = File(outDir, "${shot.name}.png")
    file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    require(file.length() > 20_000) { "${file.name}: not a real nonempty renderer image" }
  }

  /**
   * Zoom coverage is the point: debt B is only visible at/above z1.0, debt C only at
   * night, debt A at every gameplay zoom. Cameras are the existing authored anchors.
   */
  @Test fun terrainAndLandmarkMatrix() {
    outDir.mkdirs()
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    WorldEnvironment.init(RuntimeEnvironment.getApplication())
    val oldQuality = DepthPresentation.quality
    val shots = listOf(
      // Required by the work order: overview / service / plaza at .36 / .88 / 1.0
      Shot("overview_noon_z036", 1600f, 1380f, 0.36f, 12f, note = "island silhouette"),
      Shot("service_noon_z088", 960f, 860f, 0.88f, 12f, note = "houses, tone vs ground"),
      Shot("plaza_noon_z100", 1940f, 1240f, 1.0f, 12f, note = "flagstone period"),
      // Extra: the same plaza at the period-critical zoom, cropped by camera only
      Shot("plaza_noon_z148", 1940f, 1240f, 1.48f, 12f, note = "flagstone at max zoom"),
      Shot("plaza_night_z100", 1940f, 1240f, 1.0f, 0f, note = "ground grade at night"),
      // Landmarks across the hour sweep (debt C needs more than one night sample)
      Shot("summon_hour06_z148", 2430f, 1715f, 1.48f, 6f),
      Shot("summon_noon_z148", 2430f, 1715f, 1.48f, 12f),
      Shot("summon_hour18_z148", 2430f, 1715f, 1.48f, 18f),
      Shot("summon_night_z148", 2430f, 1715f, 1.48f, 0f),
      Shot("gate_hour06_z148", 1960f, 310f, 1.48f, 6f),
      Shot("gate_noon_z148", 1960f, 310f, 1.48f, 12f),
      Shot("gate_hour18_z148", 1960f, 310f, 1.48f, 18f),
      Shot("gate_night_z148", 1960f, 310f, 1.48f, 0f),
      // Legacy P03 houses, close up, so debt A can be judged on its own
      Shot("house_quarters_noon_z148", 825f, 790f, 1.48f, 12f, note = "P03 art vs P27B ground"),
      Shot("house_restaurant_noon_z148", 1105f, 925f, 1.48f, 12f, note = "P03 art vs P27B ground"),
    )
    try {
      DepthPresentation.quality = DepthPresentation.Quality.BALANCED
      shots.forEach(::render)
    } finally {
      DepthPresentation.quality = oldQuality
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
    File(outDir, "camera_props.json").writeText(JSONArray().apply {
      shots.forEach { shot ->
        put(JSONObject()
          .put("file", "${shot.name}.png").put("camX", shot.x.toDouble())
          .put("camY", shot.y.toDouble()).put("zoom", shot.zoom.toDouble())
          .put("timeSec", 0).put("hour", shot.hour.toDouble())
          .put("weather", shot.weather.name).put("tier", "BALANCED")
          .put("width", width).put("height", height)
          .put("note", shot.note)
          .put("render_method", "Robolectric native Compose Canvas")
          .put("source_sha", System.getenv("GITHUB_SHA") ?: "LOCAL_UNVERIFIED"))
      }
    }.toString(2))
  }
}
