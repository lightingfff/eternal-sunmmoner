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
 * P19 engine-render evidence for the training field / gravel band / cobble joints.
 *
 * Camera positions and zoom levels are FIXED and identical between the before and the after
 * run, so the two PNG sets in `docs/visual-evidence/p19/engine-render-before|after/` can be
 * compared pixel-for-pixel on the same framing.
 *
 * These are real renderer calls (`drawSanctuaryFloatingWorld`: island, sand arena, gravel
 * transition band, cobble roads, plaza, 4x4 grass blocks, then the depth-sorted props), run
 * on Robolectric's native Skia backend. They are NOT device FPS or thermal evidence, and
 * they are software-rasterised at 1x density.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapForgeTerrainRenderTest {

  private val outDir = File("build/visual_screenshots/p19")

  private fun prepare() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(12f, WorldWeather.CLEAR)
    WorldEnvironment.update(50000)
  }

  @Test
  fun captureTerrainSliceThroughTheRealRenderer() {
    prepare()
    outDir.mkdirs()
    val layout = SanctuaryLobbyEngine().mapLayout
    try {
      fun render(name: String, camX: Float, camY: Float, zoom: Float, props: Boolean = true) {
        val w = 1280
        val h = 720
        val image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat())) {
          drawSanctuaryFloatingWorld(camX, camY, zoom, 0f, layout.paths)
          drawGroundShadows(layout.obstacles, camX, camY, zoom)
          if (props) {
            for (obs in layout.obstacles.sortedBy { it.y + it.sortYBias }) {
              drawProp(obs, camX, camY, zoom, 0f, renderShadow = null)
            }
          }
        }
        val file = File(outDir, "$name.png")
        file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        require(file.length() > 20_000) { "$name.png must not be empty" }
      }

      // Golden-sand training arena (sand tiles = soil_0..5, gravel band = gravel_0..3).
      render("terrain_training_default", 855f, 1815f, 0.88f)
      render("terrain_training_close", 855f, 1815f, 1.48f)
      render("terrain_training_overview", 855f, 1815f, 0.36f)

      // Arena rim: grass -> gravel band -> sand, the transition the brief calls out.
      render("terrain_arena_rim", 700f, 1680f, 1.48f)

      // Cobble road joints at three scales, same spot for before/after comparison.
      render("terrain_road_joints_close", 1200f, 900f, 1.48f)
      render("terrain_road_mid", 1200f, 900f, 1.00f)
      render("terrain_road_overview", 1070f, 800f, 0.36f)

      // Lawn without props, for the grass density check.
      render("terrain_lawn_close", 900f, 980f, 1.48f, props = false)
    } finally {
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
  }
}
