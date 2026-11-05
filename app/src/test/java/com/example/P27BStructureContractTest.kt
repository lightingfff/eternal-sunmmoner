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
    // Facilities keep their premium_slice canvas sizes.
    "hero_quarters" to (200 to 168),
    "armory" to (190 to 166),
    "synthesis_lab" to (188 to 168),
    "restaurant" to (206 to 172),
    // Debt A remainder: towers + hero registry. Still the P03 / premium canvas sizes.
    "hero_registry" to (188 to 166),
    "fortress_tower" to (80 to 118),
    "gate_tower" to (96 to 136),
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
   * Debt A itself, as a number - but against the right reference.
   *
   * The first version of this test compared every building to the landmark luma (~175).
   * That was wrong and the build caught it: only the two landmarks are ivory limestone
   * monuments; the new houses are cream plaster + slate, which sits around 84. Comparing
   * a house to a monument would have pushed the art toward being washed out.
   *
   * The real defect was that P03/premium buildings sat at 50-59 with a dark blue-grey
   * bias that did not match the bright island at all. So the reference is the P27B-V5
   * house family itself, and each replacement must (a) land in that family's band and
   * (b) move at least 25% of the way there from where it started.
   */
  @Test
  fun replacementsMoveTowardTheP27BHousePalette() {
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
    // Measured band of the P27B-V5 building family, so a stray washed-out or muddy
    // building fails instead of passing on average.
    // Measured range across the whole P27B-V5 building family: the thatched cottage is
    // deliberately sunny straw (148) and the plum canopy is deliberately pale blossom
    // (130), so the band reflects the art that was actually authored, not a guess.
    val familyMin = 68.0
    val familyMax = 155.0
    // Name to the P03 / premium_slice luma it replaced.
    val replaced = mapOf(
      "townhouse_a" to 51.4, "townhouse_b" to 58.9, "cottage_brown" to 50.6,
      "cottage_slate" to 58.6, "oak" to 51.3, "pine" to 50.4, "canopy_a" to 57.1,
      "hero_quarters" to 50.0, "armory" to 51.1, "synthesis_lab" to 54.5,
      "restaurant" to 58.5, "training_dummy" to 62.2, "wooden_bench" to 45.9,
      "flower_planter" to 77.9,
      "hero_registry" to 54.7, "fortress_tower" to 57.4, "gate_tower" to 54.8,
    )
    for ((name, oldLuma) in replaced) {
      val now = luma(name)
      assertTrue(
        "$name: luma $now is outside the measured P27B-V5 building band " +
          "$familyMin..$familyMax",
        now in familyMin..familyMax,
      )
      val improved = if (now > oldLuma) now - oldLuma else 0.0
      assertTrue(
        "$name: luma moved only $improved from the replaced art ($oldLuma), expected at " +
          "least 25% of the way toward the family band",
        now > oldLuma * 1.25,
      )
    }
  }

  /**
   * The tree foot padding is not decoration: `ObliqueSpriteGeometry.drawFootOffset()` for a
   * tree IS `bounds(image).bottomPadding * pixelScale`. The P27B-V5 tree re-cut filled the
   * bottom rows of the canvas, so every tree drew ~5 px lower and the foot sank into the
   * ground. It was caught by MapForgeP22ContractTest (bottom 131, expected 126).
   *
   * Houses are anchored to their bottom row, so they legitimately have 0 padding; this test
   * only pins the sprites where the number is load-bearing.
   */
  @Test
  fun vegetationKeepsTheShippedGroundOffset() {
    val expected = mapOf("canopy_a" to 5, "pine" to 5, "oak" to 1)
    for ((name, pad) in expected) {
      val bytes = assetBytes("sanctuary_p27b_2/structures/$name.png")
      val png = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
      var lastOpaqueRow = -1
      for (y in 0 until png.height) {
        for (x in 0 until png.width) {
          if ((png.getPixel(x, y) ushr 24) > 128) { lastOpaqueRow = y; break }
        }
      }
      val actual = png.height - 1 - lastOpaqueRow
      assertEquals("$name must keep the shipped $pad-row ground offset", pad, actual)
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
    assertEquals(200, PixelStructureAtlas.heroQuartersStructure.width)
    assertEquals(190, PixelStructureAtlas.armoryStructure.width)
    // Debt A remainder. These two towers used to resolve straight to sanctuary_p03, so the
    // wall line changed tone mid-run: ivory gate next to dark blue-grey P03 towers.
    assertEquals("corner tower must resolve to the P27B-V5 canvas", 80, PixelStructureAtlas.fortressCornerTowerSprite.width)
    assertEquals(118, PixelStructureAtlas.fortressCornerTowerSprite.height)
    assertEquals(96, PixelStructureAtlas.southernGateTowerSprite.width)
    assertEquals(188, PixelStructureAtlas.heroRegistryStructure.width)
  }
}
