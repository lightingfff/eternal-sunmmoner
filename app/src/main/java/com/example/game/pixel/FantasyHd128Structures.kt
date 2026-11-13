package com.example.game.pixel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * FantasyHd128Structures: High-Definition (2x Sub-Pixel Density / 128-grade) 2.5D Oblique 3/4
 * Architecture, Landmarks, Houses, Multi-Layered Trees, and Interior Props.
 *
 * Synthesizes true 2x resolution (`W*2 x H*2`) bitmaps using Edge-Directed Scale2x/EPX contour
 * refinement + Procedural HD-2D Material Micro-Detail (ashlar stone grain, roof shingle bevels,
 * NW solar specular rims, stained-glass diagonal reflections, and botanical leaf-cluster veins)
 * while preserving 100% identical world footprints, pivots, doorway triggers, and shadow bounds.
 */
object FantasyHd128Structures {

  private val hdCache = ConcurrentHashMap<Int, ImageBitmap>()

  fun hdFor(source: ImageBitmap, materialProfile: MaterialProfile = MaterialProfile.ARCHITECTURE): ImageBitmap {
    val key = System.identityHashCode(source) * 31 + materialProfile.ordinal
    return hdCache.getOrPut(key) {
      synthesizeHd128Sprite(source, materialProfile)
    }
  }

  enum class MaterialProfile {
    ARCHITECTURE,
    FOLIAGE,
    PROP,
    CHARACTER,
  }

  val hdTownhouseA: ImageBitmap get() = hdFor(PixelStructureAtlas.medievalTownhouseA, MaterialProfile.ARCHITECTURE)
  val hdTownhouseB: ImageBitmap get() = hdFor(PixelStructureAtlas.medievalTownhouseB, MaterialProfile.ARCHITECTURE)
  val hdCottageBrown: ImageBitmap get() = hdFor(PixelStructureAtlas.sanctuaryCottageBrown, MaterialProfile.ARCHITECTURE)
  val hdCottageSlate: ImageBitmap get() = hdFor(PixelStructureAtlas.sanctuaryCottageSlate, MaterialProfile.ARCHITECTURE)
  val hdFortressCornerTower: ImageBitmap get() = hdFor(PixelStructureAtlas.fortressCornerTowerSprite, MaterialProfile.ARCHITECTURE)
  val hdSouthernGateTower: ImageBitmap get() = hdFor(PixelStructureAtlas.southernGateTowerSprite, MaterialProfile.ARCHITECTURE)
  val hdStoneRetainingWall: ImageBitmap get() = hdFor(PixelStructureAtlas.stoneRetainingWallSprite, MaterialProfile.ARCHITECTURE)
  val hdTerraceCliffWall: ImageBitmap get() = hdFor(PixelStructureAtlas.terraceCliffWallSprite, MaterialProfile.ARCHITECTURE)
  val hdTerraceGrandStairs: ImageBitmap get() = hdFor(PixelStructureAtlas.terraceGrandStairsSprite, MaterialProfile.ARCHITECTURE)
  val hdStoneBalustrade: ImageBitmap get() = hdFor(PixelStructureAtlas.stoneBalustradeSprite, MaterialProfile.ARCHITECTURE)

  // HD-128 Multi-Layered Trees & Foliage
  val hdOakTree: ImageBitmap get() = hdFor(PixelStructureAtlas.oakTreeSprite, MaterialProfile.FOLIAGE)
  val hdSakuraTreeA: ImageBitmap get() = hdFor(PixelStructureAtlas.sakuraTreeSpriteA, MaterialProfile.FOLIAGE)
  val hdSakuraTreeB: ImageBitmap get() = hdFor(PixelStructureAtlas.sakuraTreeSpriteB, MaterialProfile.FOLIAGE)
  val hdSpiritPine: ImageBitmap get() = hdFor(PixelStructureAtlas.spiritPineSprite, MaterialProfile.FOLIAGE)
  val hdMysticPlumTree: ImageBitmap get() = hdFor(PixelStructureAtlas.mysticPlumTreeSprite, MaterialProfile.FOLIAGE)
  val hdCypressConifer: ImageBitmap get() = hdFor(PixelStructureAtlas.cypressConiferSprite, MaterialProfile.FOLIAGE)
  val hdGardenHedgeBush: ImageBitmap get() = hdFor(PixelStructureAtlas.gardenHedgeBushSprite, MaterialProfile.FOLIAGE)
  val hdForestBush: ImageBitmap get() = hdFor(PixelStructureAtlas.forestBushSprite, MaterialProfile.FOLIAGE)

  // HD-128 Props & Interior Furniture
  val hdTavernCounter: ImageBitmap get() = hdFor(PixelStructureAtlas.tavernCounterSprite, MaterialProfile.PROP)
  val hdTavernShelfBarrels: ImageBitmap get() = hdFor(PixelStructureAtlas.tavernShelfBarrelsSprite, MaterialProfile.PROP)
  val hdTavernLongTable: ImageBitmap get() = hdFor(PixelStructureAtlas.tavernLongTableSprite, MaterialProfile.PROP)
  val hdArcaneBookshelf: ImageBitmap get() = hdFor(PixelStructureAtlas.arcaneBookshelfSprite, MaterialProfile.PROP)
  val hdWoodenBench: ImageBitmap get() = hdFor(PixelStructureAtlas.woodenBenchSprite, MaterialProfile.PROP)
  val hdBarrelCluster: ImageBitmap get() = hdFor(PixelStructureAtlas.barrelClusterSprite, MaterialProfile.PROP)
  val hdTrainingStrikingPost: ImageBitmap get() = hdFor(PixelStructureAtlas.trainingStrikingPostSprite, MaterialProfile.PROP)
  val hdObsidianColumn: ImageBitmap get() = hdFor(PixelStructureAtlas.obsidianColumnSprite, MaterialProfile.ARCHITECTURE)
  val hdRoyalStatue: ImageBitmap get() = hdFor(PixelStructureAtlas.royalStatueSprite, MaterialProfile.ARCHITECTURE)
  val hdRuinArch: ImageBitmap get() = hdFor(PixelStructureAtlas.ruinArchSprite, MaterialProfile.ARCHITECTURE)
  val hdMonolith: ImageBitmap get() = hdFor(PixelStructureAtlas.monolithSprite, MaterialProfile.ARCHITECTURE)
  val hdMossyBoulder: ImageBitmap get() = hdFor(PixelStructureAtlas.mossyBoulderSprite, MaterialProfile.PROP)

  fun hdStreetLantern(frame: Int): ImageBitmap {
    val frames = PixelStructureAtlas.streetLanternFrames
    return hdFor(frames[frame.coerceIn(0, frames.lastIndex)], MaterialProfile.PROP)
  }

  fun hdBrazier(frame: Int): ImageBitmap {
    val frames = PixelStructureAtlas.brazierFrames
    return hdFor(frames[frame.coerceIn(0, frames.lastIndex)], MaterialProfile.PROP)
  }

  fun hdTavernFireplace(frame: Int): ImageBitmap {
    val frames = PixelStructureAtlas.tavernFireplaceFrames
    return hdFor(frames[frame.coerceIn(0, frames.lastIndex)], MaterialProfile.PROP)
  }

  /**
   * Performs a 2x Scale2x/EPX Edge-Directed Super-Resolution pass + HD-2D Normal/Material
   * Micro-Texturing pass (NW sun bevels, ashlar stone grain, leaf cluster highlights, and
   * emissive window/rune speculars).
   */
  private fun synthesizeHd128Sprite(srcBitmap: ImageBitmap, profile: MaterialProfile): ImageBitmap {
    val androidSrc = srcBitmap.asAndroidBitmap()
    val sw = androidSrc.width
    val sh = androidSrc.height
    if (sw <= 0 || sh <= 0) return srcBitmap

    val srcPixels = IntArray(sw * sh)
    androidSrc.getPixels(srcPixels, 0, sw, 0, 0, sw, sh)

    val dw = sw * 2
    val dh = sh * 2
    val dstPixels = IntArray(dw * dh)

    fun sample(x: Int, y: Int): Int {
      val cx = x.coerceIn(0, sw - 1)
      val cy = y.coerceIn(0, sh - 1)
      return srcPixels[cy * sw + cx]
    }

    for (y in 0 until sh) {
      for (x in 0 until sw) {
        val p = sample(x, y)
        val a = p ushr 24
        if (a < 16) continue

        val bTop = sample(x, y - 1)
        val hBot = sample(x, y + 1)
        val dLeft = sample(x - 1, y)
        val fRight = sample(x + 1, y)

        // Scale2x / EPX diagonal contour refinement
        var e0 = if (colorsClose(dLeft, bTop) && !colorsClose(dLeft, hBot) && !colorsClose(bTop, fRight)) dLeft else p
        var e1 = if (colorsClose(bTop, fRight) && !colorsClose(bTop, dLeft) && !colorsClose(fRight, hBot)) fRight else p
        var e2 = if (colorsClose(dLeft, hBot) && !colorsClose(dLeft, bTop) && !colorsClose(hBot, fRight)) dLeft else p
        var e3 = if (colorsClose(hBot, fRight) && !colorsClose(dLeft, hBot) && !colorsClose(bTop, fRight)) fRight else p

        // Keep transparent silhouette crisp
        if ((e0 ushr 24) < 128) e0 = p
        if ((e1 ushr 24) < 128) e1 = p
        if ((e2 ushr 24) < 128) e2 = p
        if ((e3 ushr 24) < 128) e3 = p

        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        val luma = (r * 77 + g * 150 + b * 29) shr 8

        val topLuma = lumaOf(bTop)
        val leftLuma = lumaOf(dLeft)
        val botLuma = lumaOf(hBot)
        val rightLuma = lumaOf(fRight)

        // Detect emissive windows (warm amber/gold) or arcane crystals (cyan/violet)
        val isWarmEmissive = r > 215 && g > 155 && b < 160
        val isCyanEmissive = b > 205 && g > 150 && r < 130
        val isFoliage = g > r + 12 && g > b + 8
        val isSakura = r > 135 && b > 110 && r > g + 12

        if (isWarmEmissive || isCyanEmissive) {
          // HD-2D glowing window / rune crystal specular gleam on NW sub-pixel
          e0 = shiftRgb(e0, 26, 24, 18)
          if (((x + y) and 3) == 0) {
            e1 = shiftRgb(e1, 34, 34, 34)
          }
        } else if (profile == MaterialProfile.FOLIAGE || isFoliage || isSakura) {
          // Botanical leaf-cluster micro-detail: NW sunlit leaf tip + SE deep canopy shadow
          val hash = ((x * 73 + y * 151) and 7)
          if (luma > 42) {
            if (hash < 3 || luma > topLuma + 10) {
              e0 = shiftRgb(e0, 12, 16, 6) // NW sun-kissed leaf tip
            }
            if (hash == 4) {
              e1 = shiftRgb(e1, 6, 10, 3)
            }
            if (hash > 5 || luma < botLuma - 10) {
              e3 = shiftRgb(e3, -10, -12, -8) // SE under-leaf pocket AO
            }
          }
        } else if (profile == MaterialProfile.ARCHITECTURE || profile == MaterialProfile.PROP) {
          // Architectural stone/roof/timber micro-bevel & grain
          if (luma in 36..235) {
            val nwEdge = (luma - topLuma > 14) || (luma - leftLuma > 14)
            val seEdge = (luma - botLuma > 14) || (luma - rightLuma > 14)
            val grain = ((x * 37 + y * 89) and 7)
            if (nwEdge) {
              e0 = shiftRgb(e0, 14, 12, 9) // Warm NW solar bevel highlight
            } else if (grain == 0) {
              e0 = shiftRgb(e0, 7, 6, 4)
            }
            if (seEdge) {
              e3 = shiftRgb(e3, -12, -12, -10) // Crisp SE chiselled shadow lip
            } else if (grain == 5) {
              e2 = shiftRgb(e2, -6, -6, -5)
            }
          }
        } else if (profile == MaterialProfile.CHARACTER) {
          // Character & Monster HD-2D NMM armor/cloth rim highlight
          if (luma in 40..240) {
            if (luma - topLuma > 16 || luma - leftLuma > 16) {
              e0 = shiftRgb(e0, 16, 15, 12)
            }
            if (luma - botLuma > 16 || luma - rightLuma > 16) {
              e3 = shiftRgb(e3, -11, -11, -9)
            }
          }
        }

        val dx = x * 2
        val dy = y * 2
        dstPixels[dy * dw + dx] = e0
        dstPixels[dy * dw + dx + 1] = e1
        dstPixels[(dy + 1) * dw + dx] = e2
        dstPixels[(dy + 1) * dw + dx + 1] = e3
      }
    }

    val out = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888)
    out.setPixels(dstPixels, 0, dw, 0, 0, dw, dh)
    return out.asImageBitmap()
  }

  private fun colorsClose(c1: Int, c2: Int): Boolean {
    val a1 = c1 ushr 24
    val a2 = c2 ushr 24
    if ((a1 < 32) != (a2 < 32)) return false
    if (a1 < 32 && a2 < 32) return true
    val rDiff = abs(((c1 shr 16) and 0xFF) - ((c2 shr 16) and 0xFF))
    val gDiff = abs(((c1 shr 8) and 0xFF) - ((c2 shr 8) and 0xFF))
    val bDiff = abs((c1 and 0xFF) - (c2 and 0xFF))
    return (rDiff + gDiff + bDiff) <= 36
  }

  private fun lumaOf(c: Int): Int {
    if ((c ushr 24) < 32) return 0
    val r = (c shr 16) and 0xFF
    val g = (c shr 8) and 0xFF
    val b = c and 0xFF
    return (r * 77 + g * 150 + b * 29) shr 8
  }

  private fun shiftRgb(c: Int, dr: Int, dg: Int, db: Int): Int {
    val a = c and 0xFF000000.toInt()
    val r = min(255, max(0, ((c shr 16) and 0xFF) + dr))
    val g = min(255, max(0, ((c shr 8) and 0xFF) + dg))
    val b = min(255, max(0, (c and 0xFF) + db))
    return a or (r shl 16) or (g shl 8) or b
  }
}
