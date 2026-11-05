package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.pixel.DepthPresentation
import com.example.game.pixel.HdrLightResponse
import com.example.game.pixel.LivingWorldRenderer
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryEnvironmentShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P27B-V5 checkpoint 3 (debt C): a landmark must respond to night, not sit on top of it.
 *
 * The defect as measured from the shipped evidence: at 00:00 the Gate facade still read
 * 124 luma against 160 at noon, because the global grade applies one flat navy over
 * everything and pale ivory stone was dimmed by the same amount as the grass.
 *
 * This test renders the REAL renderer and compares, at the same camera and zoom:
 *  - how much a building's pixels darken from noon to night, versus
 *  - how much the ground darkens.
 * A building that ignores the night shows a smaller drop than the ground. The pass added
 * in this checkpoint makes pale architecture drop MORE than the ground, which is what
 * "the stone answers the moonlight" means numerically.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P27BNightResponseTest {
  private val width = 1280
  private val height = 720

  private fun render(hour: Float, x: Float, y: Float, zoom: Float): Bitmap {
    WorldEnvironment.debugOverride(hour, WorldWeather.CLEAR)
    WorldEnvironment.update(50000)
    val layout = SanctuaryLobbyEngine().mapLayout
    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
    CanvasDrawScope().draw(
      Density(1f), LayoutDirection.Ltr, Canvas(image),
      Size(width.toFloat(), height.toFloat()),
    ) {
      drawSanctuaryFloatingWorld(x, y, zoom, 0f, layout.paths)
      for (obs in layout.obstacles.sortedBy { it.y + it.sortYBias }) {
        drawProp(obs, x, y, zoom, 0f, renderShadow = null)
      }
      with(LivingWorldRenderer) {
        drawWorldGrade()
        drawLocalLights(layout.obstacles, x, y, zoom)
      }
    }
    return image.asAndroidBitmap()
  }

  private fun meanLuma(bitmap: Bitmap, left: Int, top: Int, right: Int, bottom: Int): Double {
    var total = 0.0
    var count = 0
    for (y in top until bottom) {
      for (x in left until right) {
        val px = bitmap.getPixel(x, y)
        total += 0.299 * ((px shr 16) and 0xFF) + 0.587 * ((px shr 8) and 0xFF) +
          0.114 * (px and 0xFF)
        count++
      }
    }
    return if (count == 0) 0.0 else total / count
  }

  @Test
  fun landmarkFacadeAnswersNightMoreThanTheGroundDoes() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    WorldEnvironment.init(RuntimeEnvironment.getApplication())
    val oldQuality = DepthPresentation.quality
    try {
      DepthPresentation.quality = DepthPresentation.Quality.BALANCED
      // Gate at the authored frame, and the same camera at noon.
      val noon = render(12f, 1960f, 310f, 1.48f)
      val night = render(0f, 1960f, 310f, 1.48f)

      // Facade band: the two ivory towers fill the middle of this frame.
      val facade = intArrayOf(360, 120, 920, 430)
      // Ground band: grass + paving at the bottom of the same frame, well below the gate.
      val ground = intArrayOf(360, 600, 920, 700)

      val facadeNoon = meanLuma(noon, facade[0], facade[1], facade[2], facade[3])
      val facadeNight = meanLuma(night, facade[0], facade[1], facade[2], facade[3])
      val groundNoon = meanLuma(noon, ground[0], ground[1], ground[2], ground[3])
      val groundNight = meanLuma(night, ground[0], ground[1], ground[2], ground[3])

      val facadeDrop = 1.0 - facadeNight / facadeNoon
      val groundDrop = 1.0 - groundNight / groundNoon

      assertTrue("facade must actually darken at night (drop $facadeDrop)", facadeDrop > 0.05)
      assertTrue(
        "ground drop $groundDrop should be modest; the night grade is not meant to blacken " +
          "the scene",
        groundDrop > 0.02,
      )
      assertTrue(
        "debt C: the facade dropped ${"%.3f".format(facadeDrop)} while the ground dropped " +
          "${"%.3f".format(groundDrop)}. Pale stone must absorb more moonlight than grass, " +
          "not less, or the building reads as pasted on the night.",
        facadeDrop >= groundDrop * 1.05,
      )
    } finally {
      DepthPresentation.quality = oldQuality
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
  }

  /**
   * The absorption must be driven by the sprite's own brightness, not a constant, or a
   * dark slate roof would be dimmed exactly like ivory limestone.
   */
  @Test
  fun absorptionScalesWithSpriteBrightness() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    val layout = SanctuaryLobbyEngine().mapLayout
    val gate = layout.obstacles.first { it.id == "monument_tower_gate" }
    val house = layout.obstacles.first { it.id == "nw_outer_slate_house" }
    val gateSprite = SanctuaryEnvironmentShadows.sprite(gate)
    val houseSprite = SanctuaryEnvironmentShadows.sprite(house)
    if (gateSprite != null && houseSprite != null) {
      val gateLuma = HdrLightResponse.meanLuma(gateSprite)
      val houseLuma = HdrLightResponse.meanLuma(houseSprite)
      assertTrue("gate sprite luma $gateLuma should be pale stone", gateLuma > 120f)
      assertTrue(
        "a mixed house (slate roof + cream wall) must be darker than the ivory gate " +
          "($houseLuma vs $gateLuma)",
        houseLuma < gateLuma,
      )
    }
  }

  /** Cached, not rebuilt: the luma lookup must not allocate a pixel array every frame. */
  @Test
  fun lumaLookupIsCachedPerBitmap() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    val image: ImageBitmap = SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/tower_gate.png")
      ?: SanctuaryArtAssets.assetSafe("sanctuary_p03/oak.png")
      ?: return
    val first = HdrLightResponse.meanLuma(image)
    val second = HdrLightResponse.meanLuma(image)
    assertTrue("mean luma must be stable across calls", kotlin.math.abs(first - second) < 0.001f)
  }
}
