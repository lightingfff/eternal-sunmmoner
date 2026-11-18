package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.game.lobby.LobbyHeroActor
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.map.PropKind
import com.example.game.map.WorldObstacle
import com.example.ui.components.drawLobbyHero
import com.example.game.pixel.DepthPresentation
import com.example.game.pixel.LivingWorldRenderer
import com.example.game.pixel.PixelSpriteRenderer.drawForegroundAtmosphere
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
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Actual native graphics Compose Canvas + real SanctuaryWorldRenderer, shadows, Y-sorted
 * props, AI-authored P27B sprites, grade, lights, weather and presentation motion. This is
 * a source-render harness, not a device screenshot or an FPS claim.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P27BRenderTest {
  private val outDir = File("build/visual_screenshots/p27b/world")
  private val width = 1280
  private val height = 720

  private data class Shot(
    val name: String,
    val x: Float,
    val y: Float,
    val zoom: Float,
    val hour: Float,
    val weather: WorldWeather = WorldWeather.CLEAR,
    val timeSec: Float = 0f,
  )

  private fun render(shot: Shot): Bitmap {
    WorldEnvironment.debugOverride(shot.hour, shot.weather)
    WorldEnvironment.update(50000)
    val engine = SanctuaryLobbyEngine()
    engine.syncHeroes(emptyList(), 1_800_000_000L)
    val layout = engine.mapLayout
    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat())) {
      drawSanctuaryFloatingWorld(shot.x, shot.y, shot.zoom, shot.timeSec, layout.paths)
      // Lobby's ground ring pass precedes Y-sorted props. Include it here so the
      // Summon world-floor change is observable in the actual-before/after pair.
      TowerFloorCatalog.SANCTUARY_ZONES.forEach { zone ->
        drawSanctuaryZoneFloorRing(zone, false, shot.x, shot.y, shot.zoom, shot.timeSec)
      }
      drawGroundShadows(layout.obstacles, shot.x, shot.y, shot.zoom, clipToIsland = true)
      val drawables: List<Pair<Float, Any>> =
        layout.obstacles.map { (it.y + it.sortYBias) to (it as Any) } +
          engine.heroActors.map { it.y to (it as Any) }
      drawables.sortedBy { it.first }.forEach { (_, drawable) ->
        when (drawable) {
          is WorldObstacle -> drawProp(drawable, shot.x, shot.y, shot.zoom, shot.timeSec, renderShadow = null)
          is LobbyHeroActor -> drawLobbyHero(drawable, shot.x, shot.y, shot.zoom, shot.timeSec, selected = false)
        }
      }
      with(LivingWorldRenderer) {
        drawWorldGrade()
        drawLocalLights(layout.obstacles, shot.x, shot.y, shot.zoom)
        drawWeather(shot.x, shot.y, shot.zoom, foreground = true)
      }
      drawForegroundAtmosphere(layout.biome, shot.timeSec, particlesEnabled = true)
    }
    val file = File(outDir, "${shot.name}.png")
    file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    require(file.length() > 20_000) { "${file.name}: not a real nonempty renderer image" }
    return requireNotNull(image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)) {
      "${file.name}: could not retain bitmap for frame comparison"
    }
  }

  @Test fun sanctuaryLobbyShowsVisiblePresentationMotionAcrossFrames() {
    outDir.mkdirs()
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    val oldQuality = DepthPresentation.quality
    try {
      DepthPresentation.quality = DepthPresentation.Quality.BALANCED
      val early = render(Shot("living_lobby_frame_early", 960f, 860f, 0.88f, 12f, timeSec = 0.4f))
      val later = render(Shot("living_lobby_frame_later", 960f, 860f, 0.88f, 12f, timeSec = 4.6f))
      assertFalse("sakura breeze / blossom motes / scene lighting must animate without moving hit geometry", early.sameAs(later))
      early.recycle()
      later.recycle()
    } finally {
      DepthPresentation.quality = oldQuality
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
  }

  @Test fun luminousFacilityEmittersAnimateInPlaceAcrossFrames() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(0f, WorldWeather.CLEAR)
    WorldEnvironment.update(50_000)
    val oldQuality = DepthPresentation.quality
    val kinds = listOf(
      PropKind.FACILITY_TOWER_GATE,
      PropKind.FACILITY_SUMMONING_HALL,
      PropKind.FACILITY_HERO_REGISTRY,
      PropKind.FACILITY_SYNTHESIS_LAB,
      PropKind.FACILITY_TRAINING_GROUND,
      PropKind.FACILITY_ARMORY,
      PropKind.FACILITY_HERO_QUARTERS,
      PropKind.FACILITY_RESTAURANT,
      PropKind.MEDIEVAL_TOWNHOUSE_A,
      PropKind.SANCTUARY_COTTAGE_SLATE,
      PropKind.FLOOR_VISTA_ARCH,
    )
    fun renderFacility(kind: PropKind, timeSec: Float): Bitmap {
      val bitmap = Bitmap.createBitmap(720, 560, Bitmap.Config.ARGB_8888).asImageBitmap()
      val obstacle = WorldObstacle("vfx_${kind.name}", 1500f, 1400f, 40f, propKind = kind,
        footprintHalfW = 64f, footprintDepthNorth = 40f)
      CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(720f, 560f)) {
        drawProp(obstacle, 1500f, 1400f, 0.86f, timeSec, renderShadow = false)
      }
      return requireNotNull(bitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false))
    }
    try {
      DepthPresentation.quality = DepthPresentation.Quality.BALANCED
      for (kind in kinds) {
        val early = renderFacility(kind, 0.25f)
        val later = renderFacility(kind, 1.47f)
        assertFalse("$kind emissive VFX should animate while its sprite anchor remains fixed", early.sameAs(later))
        early.recycle()
        later.recycle()
      }
    } finally {
      DepthPresentation.quality = oldQuality
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
  }

  @Test fun sanctuaryP27BBeforeAfterMatrix() {
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
      shots.forEach { shot -> render(shot).recycle() }
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
          .put("timeSec", shot.timeSec.toDouble()).put("hour", shot.hour.toDouble())
          .put("weather", shot.weather.name).put("tier", "BALANCED")
          .put("width", width).put("height", height)
          .put("render_method", "Robolectric native Compose Canvas")
          .put("source_sha", System.getenv("GITHUB_SHA") ?: "LOCAL_UNVERIFIED")
        )
      }
    }.toString(2))
  }
}
