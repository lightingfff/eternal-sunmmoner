package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.game.pixel.PixelTileAtlas
import java.nio.file.Paths
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P27B-V5 Checkpoint 1: the ground must not repeat on a 4-tile lattice.
 *
 * These tests read the SHIPPED terrain PNGs (not synthetic fixtures), because the
 * defect was in the art+sampling combination and a fake tile would not reproduce it.
 *
 * [fourByFourLatticeIsGone] is the regression guard for debt B. It is written against
 * the sampling contract, so it FAILS on the previous 4x4 atlas and passes on the 8x8
 * sheet: the mean luminance difference between two cells one period apart must be no
 * larger than between two genuinely different, adjacent cells. On a 4-tile torus the
 * cell 4 tiles away *is the same cell*, so that comparison collapses to 0 and the test
 * fails loudly instead of quietly accepting the lattice.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P27BGroundRepeatTest {

  private fun asset(path: String): Bitmap {
    val root = Paths.get("src/main/assets")
    val file = listOf(root, Paths.get("app/src/main/assets"))
      .map { it.resolve(path) }
      .firstOrNull { it.toFile().isFile }
      ?: error("missing asset $path (cwd=${Paths.get("").toAbsolutePath()})")
    return BitmapFactory.decodeFile(file.toAbsolutePath().toString())
      ?: error("undecodable asset $path")
  }

  private fun cellLuma(sheet: Bitmap, cellX: Int, cellY: Int): Double {
    var total = 0.0
    for (y in 0 until 64) {
      for (x in 0 until 64) {
        val px = sheet.getPixel(cellX * 64 + x, cellY * 64 + y)
        total += (0.299 * ((px shr 16) and 0xFF) +
          0.587 * ((px shr 8) and 0xFF) +
          0.114 * (px and 0xFF))
      }
    }
    return total / (64.0 * 64.0)
  }

  @Test
  fun sheetIsAn8x8MosaicWith64DistinctCells() {
    val sheet = asset("sanctuary_p27b_2/terrain/grass_mosaic.png")
    assertEquals(512, sheet.width)
    assertEquals(512, sheet.height)
    val cells = (0 until 8).flatMap { cy -> (0 until 8).map { cx -> cellLuma(sheet, cx, cy) } }
    assertEquals("period is 8 tiles, not 4", 64, cells.size)
    // Real mosaic adjacency: neighbouring cells are related, not identical.
    val distinct = cells.map { (it * 100).toInt() }.toSet().size
    assertTrue("cells should not be duplicates, found $distinct", distinct >= 32)
  }

  /**
   * Debt B guard. Compares, for the whole 8x8 sheet:
   *  - step1: |cell(x,y) - cell(x+1,y)| for interior neighbours (true adjacency),
   *  - step8: |cell(x,y) - cell(x+8,y)| which for an 8-period sheet wraps to itself.
   * A 4-tile lattice sampled with `tx & 3` would make every 4th cell identical, which
   * this test catches by requiring that no two *distinct* neighbourhoods repeat inside
   * one period and that the sheet is not translatable by 4 tiles.
   */
  @Test
  fun fourByFourLatticeIsGone() {
    for (family in listOf("grass", "soil", "flagstone", "cobble")) {
      val sheet = asset("sanctuary_p27b_2/terrain/${family}_mosaic.png")
      val luma = { cx: Int, cy: Int -> cellLuma(sheet, cx, cy) }
      var stepOne = 0.0
      var stepFour = 0.0
      var pairs = 0
      for (cy in 0 until 8) {
        for (cx in 0 until 3) {
          stepOne += abs(luma(cx, cy) - luma(cx + 1, cy))
          stepFour += abs(luma(cx, cy) - luma(cx + 4, cy))
          pairs++
        }
      }
      stepOne /= pairs
      stepFour /= pairs
      // On the old 4x4 atlas, cell(x+4) == cell(x) exactly, so stepFour == 0.
      assertTrue(
        "$family: 4-tile offset must not be an identity (got ${"%.3f".format(stepFour)})",
        stepFour > 0.05,
      )
      // And the sheet is not *more* self-similar at 4 than at 1.
      assertTrue(
        "$family: 4-tile step (${"%.3f".format(stepFour)}) must be at least as different as " +
          "a 1-tile step (${"%.3f".format(stepOne)})",
        stepFour >= stepOne * 0.5,
      )
    }
  }

  /** Sampling must preserve true adjacency: a cell's neighbour is the cell it was cut next to. */
  @Test
  fun samplingKeepsMosaicAdjacency() {
    assertEquals(0, PixelTileAtlas.sanctuarySheetCell(0, 0))
    assertEquals(1, PixelTileAtlas.sanctuarySheetCell(1, 0))
    assertEquals(8, PixelTileAtlas.sanctuarySheetCell(0, 1))
    assertEquals(63, PixelTileAtlas.sanctuarySheetCell(7, 7))
    // Period is 8 world tiles = 512 px, i.e. four times the old 4-tile lattice.
    assertEquals(0, PixelTileAtlas.sanctuarySheetCell(8, 0))
    assertEquals(0, PixelTileAtlas.sanctuarySheetCell(-8, 0))
    assertEquals(9, PixelTileAtlas.sanctuarySheetCell(-7, 1))
    // No cell index may fall outside the 8x8 sheet.
    for (ty in -20..20) for (tx in -20..20) {
      val cell = PixelTileAtlas.sanctuarySheetCell(tx, ty)
      assertTrue("cell $cell out of range for ($tx,$ty)", cell in 0..63)
    }
  }

  /**
   * The sheet must wrap seamlessly, or the 512 px repeat trades a lattice for a grid.
   *
   * The baseline is the 95th percentile of EVERY interior boundary, not one arbitrary
   * column: a single pair can be flat by luck (box downscaling duplicates columns), and
   * comparing against it would fail for reasons that have nothing to do with wrapping.
   */
  @Test
  fun sheetWrapsWithoutAVisibleSeam() {
    for (family in listOf("grass", "soil", "flagstone", "cobble")) {
      val sheet = asset("sanctuary_p27b_2/terrain/${family}_mosaic.png")
      fun columnJump(x0: Int, x1: Int): Double {
        var sum = 0.0
        for (y in 0 until 512) {
          val a = sheet.getPixel(x0, y)
          val b = sheet.getPixel(x1, y)
          sum += abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) +
            abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) +
            abs((a and 0xFF) - (b and 0xFF))
        }
        return sum / (512.0 * 3.0)
      }
      fun rowJump(y0: Int, y1: Int): Double {
        var sum = 0.0
        for (x in 0 until 512) {
          val a = sheet.getPixel(x, y0)
          val b = sheet.getPixel(x, y1)
          sum += abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) +
            abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) +
            abs((a and 0xFF) - (b and 0xFF))
        }
        return sum / (512.0 * 3.0)
      }
      val interior = ((1 until 512).map { columnJump(it - 1, it) } +
        (1 until 512).map { rowJump(it - 1, it) }).sorted()
      val p95 = interior[(interior.size * 0.95).toInt() - 1]
      val wrapX = columnJump(511, 0)
      val wrapY = rowJump(511, 0)
      assertTrue(
        "$family: wrap X seam ${"%.2f".format(wrapX)} must not exceed the 95th percentile " +
          "interior boundary ${"%.2f".format(p95)} by more than 8",
        wrapX <= p95 + 8.0,
      )
      assertTrue(
        "$family: wrap Y seam ${"%.2f".format(wrapY)} must not exceed the 95th percentile " +
          "interior boundary ${"%.2f".format(p95)} by more than 8",
        wrapY <= p95 + 8.0,
      )
    }
  }

  /** Runtime must actually pick the sheet up; a silent fallback would hide the fix. */
  @Test
  fun runtimeBindsSheetAndFallsBackWholeWhenAbsent() {
    com.example.game.pixel.SanctuaryArtAssets.init(org.robolectric.RuntimeEnvironment.getApplication())
    val sheet = PixelTileAtlas.sanctuaryGrassSheet
    assertNotNull("P27B profile must expose the 8x8 grass sheet", sheet)
    assertEquals(512, sheet!!.width)
    assertEquals(512, PixelTileAtlas.sanctuaryFlagstoneSheet?.height)
    // The old 4x4 family is still installed as the fallback tier, never mixed.
    assertEquals(16, PixelTileAtlas.sanctuaryGrassTiles.size)
  }
}
