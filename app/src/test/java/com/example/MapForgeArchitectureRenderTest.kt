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
 * P20 engine-render evidence for the armory architecture + cypress vegetation slice.
 *
 * Cameras, zoom levels and actor positions are FIXED, so `docs/visual-evidence/p20/`
 * `engine-render-before/` (main) and `engine-render/` (P20) compare on identical framing.
 *
 * The draw path is the production one: `drawSanctuaryFloatingWorld` (ground) +
 * `drawGroundShadows` + depth-sorted `drawProp` (buildings, trees, lanterns) +
 * `drawCharacterSprite` for the actor tests. Robolectric native Skia, software raster at
 * 1x density: this is real renderer output, not a mockup, but it is NOT device FPS.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapForgeArchitectureRenderTest {

  private val outDir = File("build/visual_screenshots/p20")

  // Armory anchor is (995, 655) in TowerFloorCatalog.buildSanctuaryMap; the nearest tree is
  // the cypress conifer at (1060, 485).
  private val focusX = 1030f
  private val focusY = 640f

  private fun prepare() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(12f, WorldWeather.CLEAR)
    WorldEnvironment.update(50000)
  }

  @Test
  fun captureArchitectureSliceThroughTheRealRenderer() {
    prepare()
    outDir.mkdirs()
    val layout = SanctuaryLobbyEngine().mapLayout
    try {
      fun render(name: String, camX: Float, camY: Float, zoom: Float, actors: List<Actor> = emptyList()) {
        val w = 1280
        val h = 720
        val image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat())) {
          drawSanctuaryFloatingWorld(camX, camY, zoom, 0f, layout.paths)
          drawGroundShadows(layout.obstacles, camX, camY, zoom)
          val pending = actors.sortedBy { it.y }.toMutableList()
          for (obs in layout.obstacles.sortedBy { it.y + it.sortYBias }) {
            while (pending.isNotEmpty() && pending.first().y < obs.y + obs.sortYBias) {
              val a = pending.removeAt(0)
              drawActor(a, camX, camY, zoom)
            }
            drawProp(obs, camX, camY, zoom, 0f, renderShadow = null)
          }
          for (a in pending) drawActor(a, camX, camY, zoom)
        }
        val file = File(outDir, "$name.png")
        file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        require(file.length() > 20_000) { "$name.png must not be empty" }
      }

      // 1) Armory + cypress at the four gameplay zooms the brief names.
      render("arch_slice_overview", focusX, focusY, 0.36f)
      render("arch_slice_default", focusX, focusY, 0.88f)
      render("arch_slice_mid", focusX, focusY, 1.00f)
      render("arch_slice_close", focusX, focusY, 1.48f)

      // 2) Occlusion: actor in front of the cypress (south of it) and behind it (north).
      render(
        "arch_slice_actor_front", focusX, focusY, 1.48f,
        listOf(Actor(1060f, 520f, moving = false)),
      )
      render(
        "arch_slice_actor_behind", focusX, focusY, 1.48f,
        listOf(Actor(1060f, 455f, moving = false)),
      )

      // 3) Door egress: the armory doorway sits at world (993, 673). An actor leaving the
      //    door must stay readable against the facade (no silhouette swallow).
      render(
        "arch_slice_door_actor", 995f, 690f, 1.48f,
        listOf(Actor(993f, 700f, moving = true, progress = 0.30f)),
      )

      // 4) Armory from the other side, to check the side facade plane and the roof volume.
      render("arch_slice_side", 900f, 660f, 1.48f)
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
