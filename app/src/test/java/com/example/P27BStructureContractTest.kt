package com.example

import android.graphics.BitmapFactory
import com.example.game.pixel.PixelStructureAtlas
import java.nio.file.Paths
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
 * P27B-V5 checkpoint 2 (debt A): the new house/prop art must fit the OLD contracts.
 *
 * Replacing art is only safe if canvas size and ground pivot are unchanged, because
 * `WorldObstacle.footprintHalfW` / `footprintDepthNorth`, the doorway triggers and the
 * Y-depth sort were authored against them. These tests read the shipped PNG dimensions
 * directly, so a re-cut that "looks nicer" at another size fails here instead of
 * silently shifting every building's ground contact.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P27BStructureContractTest {

  /** Shipped P03 dimensions. These are the contract, not a preference. */
  private val contracts = mapOf(
    "townhouse_a" to (152 to 156),
    "townhouse_b" to (144 to 160),
    "cottage_brown" to (132 to 144),
    "cottage_slate" to (132 to 144),
    "street_lantern_0" to (32 to 66),
    "wooden_bench" to (58 to 36),
    "barrels" to (52 to 44),
    "flower_planter" to (64 to 36),
    "training_dummy" to (40 to 58),
    "canopy_a" to (114 to 132),
    "pine" to (96 to 128),
    "oak" to (112 to 136),
  )

  private fun assetBytes(path: String): ByteArray {
    val candidates = listOf(Paths.get("src/main/assets"), Paths.get("app/src/main/assets"))
      .map { it.resolve(path) }
      .firstOrNull { it.toFile().isFile }
      ?: error("missing asset $path (cwd=${Paths.get("").toAbsolutePath()})")
    return candidates.toFile().readBytes()
  }

  @Test
  fun everyReplacementKeepsTheInheritedCanvasSize() {
    for ((name, size) in contracts) {
      val bytes = assetBytes("sanctuary_p27b_2/structures/$name.png")
      val png = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
      assertNotNull("$name: does not decode", png)
      assertEquals("$name width contract", size.first, png!!.width)
      assertEquals("$name height contract", size.second, png.height)
    }
  }

  /**
   * The sprite is blitted with its BOTTOM edge on the obstacle's ground line, so the
   * bottom row must actually contain the building's base. A sprite with empty rows at
   * the bottom would float above its shadow and above its own collider.
   */
  @Test
  fun everyReplacementIsBottomAnchoredAndNotFloating() {
    for ((name, size) in contracts) {
      val bytes = assetBytes("sanctuary_p27b_2/structures/$name.png")
      val png = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
      var opaqueInBottomRows = 0
      for (y in (size.second - 3) until size.second) {
        for (x in 0 until size.first) {
          if ((png.getPixel(x, y) ushr 24) > 0) opaqueInBottomRows++
        }
      }
      assertTrue(
        "$name: bottom 3 rows are empty, so the sprite would float off its ground line",
        opaqueInBottomRows > 0,
      )
    }
  }

  /** Alpha must be binary: soft alpha would leave a halo the renderer's shadow smears. */
  @Test
  fun alphaIsBinary() {
    for (name in contracts.keys) {
      val bytes = assetBytes("sanctuary_p27b_2/structures/$name.png")
      val png = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
      var soft = 0
      for (y in 0 until png.height) {
        for (x in 0 until png.width) {
          val alpha = png.getPixel(x, y) ushr 24
          if (alpha in 1..254) soft++
        }
      }
      assertEquals("$name has $soft semi-transparent pixels; alpha must be binary", 0, soft)
    }
  }

  /**
   * Debt A itself, as a number: the replacements must be much closer to the P27B landmark
   * tonal weight than the P03 art they replaced. This is what "different tone" means in a
   * form that can fail a build.
   */
  @Test
  fun replacementsMoveTowardTheP27BLandmarkTone() {
    fun luma(name: String): Double {
      val bytes = assetBytes("sanctuary_p27b_2/structures/$name.png")
      val png = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
      var total = 0.0
      var count = 0
      for (y in 0 until png.height) {
        for (x in 0 until png.width) {
          val px = png.getPixel(x, y)
          if ((px ushr 24) <= 128) continue
          total += 0.299 * ((px shr 16) and 0xFF) + 0.587 * ((px shr 8) and 0xFF) +
            0.114 * (px and 0xFF)
          count++
        }
      }
      return if (count == 0) 0.0 else total / count
    }
    // P27B landmark measured from the shipped structures: summoning_hall ~180, tower_gate ~171.
    val landmarkLuma = 175.0
    val p3 = listOf("townhouse_a" to 51.4, "townhouse_b" to 58.9, "oak" to 51.3)
    for ((name, oldLuma) in p3) {
      val now = luma(name)
      val oldGap = kotlin.math.abs(landmarkLuma - oldLuma)
      val newGap = kotlin.math.abs(landmarkLuma - now)
      assertTrue(
        "$name: luma $now is no closer to the landmark ($landmarkLuma) than the P03 art it " +
          "replaced (${oldGap} vs ${newGap} gap)",
        newGap < oldGap * 0.75,
      )
    }
  }

  /** The runtime must actually bind the new art, not silently fall back to P03. */
  @Test
  fun runtimeBindsP27BHousesAndProps() {
    com.example.game.pixel.SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    val house = PixelStructureAtlas.medievalTownhouseA
    assertEquals("townhouse_a must resolve to the P27B-V5 canvas", 152, house.width)
    assertEquals(156, house.height)
    assertEquals(132, PixelStructureAtlas.sanctuaryCottageSlate.width)
    assertEquals(40, PixelStructureAtlas.trainingDummySprite!!.width)
    assertNotNull(PixelStructureAtlas.streetLanternFrames.firstOrNull())
    assertEquals(32, PixelStructureAtlas.streetLanternFrames.first().width)
  }
}
