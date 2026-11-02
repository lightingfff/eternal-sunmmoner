package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.HeroClassType
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P21 engine-render evidence: the six repainted facilities through the real draw path.
 *
 * Cameras and zooms are FIXED, so re-running this file against a pre-P21 checkout yields a
 * byte-for-byte comparable "before" set. This branch ships only `engine-render/` (P21): the
 * pre-P21 sprites still exist in code as the procedural fallback. The draw path is production code:
 * `drawSanctuaryFloatingWorld` (ground) + `drawGroundShadows` + depth-sorted `drawProp`
 * (buildings, trees, props) + `drawCharacterSprite` for the actor frames.
 *
 * Robolectric native Skia, software raster at 1x density: this is real renderer output, not a
 * mockup, but it is NOT a device FPS or thermal measurement.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapForgeP21RenderTest {

  private val outDir = File("build/visual_screenshots/p21")

  private fun prepare() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(12f, WorldWeather.CLEAR)
    WorldEnvironment.update(50000)
  }

  @Test
  fun capturePremiumFacilitiesThroughTheRealRenderer() {
    prepare()
    outDir.mkdirs()
    val layout = SanctuaryLobbyEngine().mapLayout
    try {
      fun render(name: String, camX: Float, camY: Float, zoom: Float, actors: List<Actor> = emptyList(), minBytes: Long = 20_000) {
        val w = 1280
        val h = 720
        val image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat())) {
          drawSanctuaryFloatingWorld(camX, camY, zoom, 0f, layout.paths)
          drawGroundShadows(layout.obstacles, camX, camY, zoom)
          val pending = actors.sortedBy { it.y }.toMutableList()
          for (obs in layout.obstacles.sortedBy { it.y + it.sortYBias }) {
            while (pending.isNotEmpty() && pending.first().y < obs.y + obs.sortYBias) {
              drawActor(pending.removeAt(0), camX, camY, zoom)
            }
            drawProp(obs, camX, camY, zoom, 0f, renderShadow = null)
          }
          for (a in pending) drawActor(a, camX, camY, zoom)
        }
        val file = File(outDir, "$name.png")
        file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        require(file.length() > minBytes) { "$name.png must not be empty" }
      }

      // 1) Service facilities at the default gameplay zoom and at 1.00.
      //    The camera is centred between the quarters / restaurant cluster so the whole roofs,
      //    both painted doors and the surrounding P19 lawn and road are inside the frame.
      render("svc_cluster_default", 950f, 860f, 0.88f)
      render("svc_cluster_mid", 950f, 860f, 1.00f)
      render("svc_cluster_overview", 1000f, 900f, 0.36f)

      // 2) Individual service facilities at close zoom, with the full roof and the full door.
      //    Hero Quarters (825,790) is enterable: the door trigger is at (823,790).
      render("svc_quarters_close", 825f, 760f, 1.48f)
      render("svc_quarters_door_actor", 823f, 800f, 1.48f,
        listOf(Actor(823f, 800f, moving = true, progress = 0.3f)))
      //    Restaurant (1105,925): door trigger (1103,925).
      render("svc_restaurant_close", 1105f, 895f, 1.48f)
      render("svc_restaurant_door_actor", 1103f, 935f, 1.48f,
        listOf(Actor(1103f, 935f, moving = true, progress = 0.3f)))
      //    Hero Registry (785,1185) is a zone-only facility: no entrance trigger exists.
      render("svc_registry_close", 785f, 1155f, 1.48f)
      //    Synthesis Lab (1270,555).
      render("svc_synthesis_close", 1270f, 525f, 1.48f)

      // 3) Hero landmarks: the tower gate (1960,585) and the summoning hall (2430,2005).
      //    Both canvases are taller than the viewport at 1.48 (224 px * 2 * 0.96 * 1.48 = 636,
      //    206 px * 2 * 1.08 * 1.48 = 659), so those frames pull the camera north until the
      //    whole silhouette is inside the 720 px frame instead of cropping the roof.
      render("hero_tower_gate_whole", 1960f, 300f, 1.48f)
      render("hero_tower_gate_portal", 1960f, 388f, 1.48f)
      render("hero_tower_gate_default", 1960f, 480f, 0.88f)
      render("hero_tower_gate_actor", 1960f, 430f, 1.20f, listOf(Actor(1960f, 640f)))
      render("hero_summoning_whole", 2400f, 1705f, 1.48f)
      render("hero_summoning_default", 2400f, 1885f, 0.88f)
      //    Interaction centre of the Summoning Hall (zone 2380,1930) must be on the rune stage.
      render("hero_summoning_zone", 2380f, 1930f, 1.48f,
        listOf(Actor(2380f, 1930f)))

      // 4) Whole-island cohesion: overview at 0.36 with the new facilities in context.
      render("island_overview", 1600f, 1380f, 0.36f, minBytes = 60_000)
    } finally {
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
  }

  private data class Actor(
    val x: Float,
    val y: Float,
    val moving: Boolean = false,
    val progress: Float = 0f,
  )

  private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawActor(
    a: Actor, camX: Float, camY: Float, zoom: Float,
  ) = drawCharacterSprite(
    worldX = a.x, worldY = a.y, camX = camX, camY = camY, zoom = zoom,
    isMaster = false, heroClass = HeroClassType.ARCHER,
    facingRight = true, isMoving = a.moving, attackAnimProgress = a.progress,
    hurtFlash = 0f, hpFraction = 1f, shieldOrAuraColor = null,
    isInjuredResting = false, timeSec = 0f, facingNorth = false,
    heroName = "Lyra", skillAnimation = false, emotion = "calm",
  )
}
