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
import com.example.game.tower.TowerFloorCatalog
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
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
 * Matched old/new selectors on the same code commit: actual native graphics Compose Canvas
 * + real SanctuaryWorldRenderer, shadows, Y-sorted props, grade, lights and weather.
 * No generated concept image, no device screenshot, and no claim of FPS.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SanctuaryArgonV2RenderTest {
  private val outDir = File("build/visual_screenshots/argon_v2/world")
  private val width = 1280
  private val height = 720

  private data class Shot(
    val name: String,
    val x: Float,
    val y: Float,
    val zoom: Float,
    val hour: Float,
    val weather: WorldWeather = WorldWeather.CLEAR,
  )

  private fun render(shot: Shot) {
    WorldEnvironment.debugOverride(shot.hour, shot.weather)
    WorldEnvironment.update(50000)
    val layout = SanctuaryLobbyEngine().mapLayout
    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat())) {
      drawSanctuaryFloatingWorld(shot.x, shot.y, shot.zoom, 0f, layout.paths)
      // Lobby's ground ring pass precedes Y-sorted props. Include it here so the
      // Summon world-floor change is observable in the actual-before/after pair.
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

  @Test fun sanctuaryArgonMatchedBeforeAfterMatrix() {
    outDir.mkdirs()
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    val oldQuality = DepthPresentation.quality
    val shots = listOf(
      Shot("overview_noon_z036", 1600f, 1380f, 0.36f, 12f),
      Shot("service_noon_z088", 960f, 860f, 0.88f, 12f),
      Shot("plaza_noon_z100", 1940f, 1240f, 1f, 12f),
      Shot("summon_noon_z148", 2430f, 1715f, 1.48f, 12f),
      Shot("summon_night_z148", 2430f, 1715f, 1.48f, 0f),
      Shot("gate_noon_z148", 1960f, 310f, 1.48f, 12f),
      Shot("gate_night_z148", 1960f, 310f, 1.48f, 0f),
    )
    try {
      DepthPresentation.quality = DepthPresentation.Quality.BALANCED
      for (enabled in listOf(false, true)) {
        SanctuaryArtAssets.argonEnabled = enabled
        shots.filter { it.zoom != 1f }.forEach { render(it.copy(name=(if(enabled) "after_" else "before_")+it.name)) }
      }
    } finally {
      SanctuaryArtAssets.argonEnabled = true
      DepthPresentation.quality = oldQuality
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
    File(outDir, "camera_props.json").writeText(JSONArray().apply {
      for (enabled in listOf(false,true)) shots.filter { it.zoom != 1f }.forEach { original ->
        val shot = original.copy(name=(if(enabled) "after_" else "before_")+original.name)
        put(JSONObject()
          .put("file", "${shot.name}.png").put("camX", shot.x.toDouble())
          .put("camY", shot.y.toDouble()).put("zoom", shot.zoom.toDouble())
          .put("timeSec", 0).put("hour", shot.hour.toDouble())
          .put("weather", shot.weather.name).put("tier", "BALANCED")
          .put("width", width).put("height", height)
          .put("render_method", "Robolectric native Compose Canvas")
          .put("source_sha", System.getenv("GITHUB_SHA") ?: "LOCAL_UNVERIFIED")
        )
      }
    }.toString(2))
  }

  @Test fun argonWarmSelectorsAndProjectionContracts() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    SanctuaryArtAssets.argonEnabled = true
    try {
      val sheet = com.example.game.pixel.PixelTileAtlas.sanctuaryGrassSheet
      org.junit.Assert.assertNotNull(sheet)
      org.junit.Assert.assertEquals(512, sheet!!.width)
      val pine = com.example.game.pixel.PixelStructureAtlas.spiritPineSprite
      val bytes = SanctuaryArtAssets.cachedBitmapBytes
      repeat(100) {
        org.junit.Assert.assertSame(sheet, com.example.game.pixel.PixelTileAtlas.sanctuaryGrassSheet)
        org.junit.Assert.assertSame(pine, com.example.game.pixel.PixelStructureAtlas.spiritPineSprite)
      }
      org.junit.Assert.assertEquals(bytes, SanctuaryArtAssets.cachedBitmapBytes)
      org.junit.Assert.assertEquals(.72f, com.example.game.map.ObliqueCamera.OBLIQUE_Y_SCALE, 0f)
      org.junit.Assert.assertNull(SanctuaryArtAssets.argon("batch_04_animated_vfx/vfx_tower_gate_portal_swirl_8f.png"))
      SanctuaryArtAssets.argonEnabled = false
      org.junit.Assert.assertNull(SanctuaryArtAssets.argonGround("grass"))
      org.junit.Assert.assertNotNull(com.example.game.pixel.PixelTileAtlas.sanctuaryGrassTiles)
    } finally { SanctuaryArtAssets.argonEnabled = true }
  }
}
