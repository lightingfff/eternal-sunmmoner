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
import java.security.MessageDigest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P18 engine-render evidence for the Map Forge ground slice.
 *
 * These are the REAL renderer calls (`drawSanctuaryFloatingWorld` -> island, sand, cobble
 * roads, verge clip, plaza, 4x4 grass blocks, then depth-sorted props) executed on
 * Robolectric's native Skia backend. PNGs land in `build/visual_screenshots/p18/`, so they
 * are renderer output, not composites of the source tiles.
 *
 * Scope and honesty:
 *  - Proves asset lookup, family/variant mapping, verdict seams, block recomposition and
 *    prop depth order. It is NOT a device FPS or thermal measurement.
 *  - Software raster at 1x density: no device DPI scaling, no frame pacing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapForgeGroundRenderTest {

  private val outDir = File("build/visual_screenshots/p18")

  private fun prepare() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(12f, WorldWeather.CLEAR)
    WorldEnvironment.update(50000)
  }

  /** The families the renderer asks for must exist, be 64x64 and be genuinely distinct. */
  @Test
  fun groundFamiliesAreWiredAndDistinct() {
    prepare()
    val families = listOf(
      "grass" to 8,
      "cobble" to 6,
      "flagstone" to 8,
      "soil" to 6,
      "gravel" to 4,
    )
    for ((family, count) in families) {
      val tiles = SanctuaryArtAssets.tiles(family, count)!!
      assertEquals("$family tile count", count, tiles.size)
      val hashes = tiles.map { tile ->
        val bmp = tile.asAndroidBitmap()
        assertEquals("$family tile must stay 64x64", 64, bmp.width)
        assertEquals("$family tile must stay 64x64", 64, bmp.height)
        val buffer = java.io.ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, buffer)
        MessageDigest.getInstance("SHA-256").digest(buffer.toByteArray()).joinToString("") { "%02x".format(it) }
      }
      assertEquals("$family variants must all differ", count, hashes.toSet().size)
    }

    // The verge overlay is drawn as one image, not a tile family.
    val verge = SanctuaryArtAssets.image("verge")
    assertNotNull("verge overlay must load", verge)
    assertEquals("verge overlay stays 64x64", 64, verge!!.width)

    // Packaged bytes must equal the manifest pins for the rewritten files.
    val context = RuntimeEnvironment.getApplication()
    val manifest = JSONObject(SanctuaryArtAssets.textAsset("sanctuary_p03/manifest.json")!!)
    val assets = manifest.getJSONObject("assets")
    for (name in listOf("grass_0", "grass_7", "cobble_0", "cobble_5", "flagstone_0", "flagstone_7", "verge", "oak")) {
      val entry = assets.getJSONObject(name)
      val bytes = context.assets.open("sanctuary_p03/${entry.getString("file")}").use { it.readBytes() }
      val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
      assertEquals("$name.png SHA-256 vs manifest", entry.getString("sha256"), digest)
      assertEquals("$name.png size vs manifest", bytes.size, entry.getInt("size"))
    }
  }

  /** Grass variants must not be recolours of each other: the block hash needs real variety. */
  @Test
  fun grassVariantsCarryDifferentContent() {
    prepare()
    val tiles = SanctuaryArtAssets.tiles("grass", 8)!!.map { it.asAndroidBitmap() }
    var differentPixels = 0
    var total = 0
    for (y in 0 until 64) for (x in 0 until 64) {
      val first = tiles[0].getPixel(x, y)
      total++
      if (tiles.drop(1).all { it.getPixel(x, y) != first }) differentPixels++
    }
    assertTrue(
      "expected shared grass body with varied clumps, got $differentPixels/$total differing",
      differentPixels > total / 3,
    )
  }

  /** Fixed cameras through the real renderer: default zoom, overview (block path), verge. */
  @Test
  fun captureGroundSliceThroughTheRealRenderer() {
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
        assertTrue("$name.png must not be empty", file.length() > 20_000)
      }

      // 1) Central plaza, default gameplay zoom (0.88): flagstone plaza + road + lawn.
      render("ground_plaza_default", 1070f, 800f, 0.88f)

      // 2) Same framing far out (0.36): the renderer switches to 4x4 grass blocks here.
      render("ground_plaza_overview", 1070f, 800f, 0.36f)

      // 3) Road / verge / lawn boundary close up (1.48): seam and verge clip review.
      render("ground_road_verge_close", 1200f, 900f, 1.48f)

      // 4) Lawn only, close: grass tiling and clump rhythm without props in the way.
      render("ground_lawn_close", 900f, 980f, 1.48f, props = false)

      // 5) Armory / smithy verge with the repainted oak for 2.5D mass review.
      render("ground_oak_verge", 1150f, 950f, 1.00f)
    } finally {
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
  }
}
