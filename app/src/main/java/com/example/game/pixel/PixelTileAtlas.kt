package com.example.game.pixel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.game.map.ObliqueCamera
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * PixelCanvas: Fast integer ARGB pixel buffer for crafting authentic, crisp 16/32-bit
 * Dark Fantasy pixel-art tiles, 3/4 architecture, and character sprites rendered
 * strictly with FilterQuality.None.
 */
class PixelCanvas(val width: Int, val height: Int) {
  val pixels = IntArray(width * height)

  fun set(x: Int, y: Int, argb: Int) {
    if (x in 0 until width && y in 0 until height) {
      pixels[y * width + x] = argb
    }
  }

  fun get(x: Int, y: Int): Int {
    if (x in 0 until width && y in 0 until height) return pixels[y * width + x]
    return 0
  }

  fun fillRect(x: Int, y: Int, w: Int, h: Int, argb: Int) {
    val x0 = x.coerceAtLeast(0)
    val y0 = y.coerceAtLeast(0)
    val x1 = (x + w).coerceAtMost(width)
    val y1 = (y + h).coerceAtMost(height)
    for (py in y0 until y1) {
      val row = py * width
      for (px in x0 until x1) {
        pixels[row + px] = argb
      }
    }
  }

  fun hLine(x: Int, y: Int, len: Int, argb: Int) = fillRect(x, y, len, 1, argb)
  fun vLine(x: Int, y: Int, len: Int, argb: Int) = fillRect(x, y, 1, len, argb)

  fun beveledSlab(
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    base: Int,
    highlight: Int,
    shadow: Int,
    mortar: Int,
    rng: Random,
    grainLight: Int,
    grainDark: Int,
  ) {
    fillRect(x, y, w, h, mortar)
    if (w <= 2 || h <= 2) return
    fillRect(x + 1, y + 1, w - 2, h - 2, base)
    hLine(x + 1, y + 1, w - 2, highlight)
    if (h >= 5) {
      hLine(x + 1, y + h - 2, w - 2, shadow)
    }
    vLine(x + 1, y + 1, h - 2, highlight)
    vLine(x + w - 2, y + 1, h - 2, shadow)
    val specks = ((w * h) / 10).coerceAtLeast(1)
    repeat(specks) {
      val sx = x + 2 + rng.nextInt((w - 4).coerceAtLeast(1))
      val sy = y + 2 + rng.nextInt((h - 4).coerceAtLeast(1))
      set(sx, sy, if (rng.nextBoolean()) grainLight else grainDark)
    }
  }

  fun fillCircle(cx: Int, cy: Int, r: Int, argb: Int) {
    val r2 = r * r
    for (dy in -r..r) {
      for (dx in -r..r) {
        if (dx * dx + dy * dy <= r2) {
          set(cx + dx, cy + dy, argb)
        }
      }
    }
  }

  fun fillEllipse(cx: Int, cy: Int, rx: Int, ry: Int, argb: Int) {
    if (rx <= 0 || ry <= 0) return
    val rx2 = (rx * rx).toFloat()
    val ry2 = (ry * ry).toFloat()
    for (dy in -ry..ry) {
      for (dx in -rx..rx) {
        if ((dx * dx) / rx2 + (dy * dy) / ry2 <= 1f) {
          set(cx + dx, cy + dy, argb)
        }
      }
    }
  }

  fun strokeCircle(cx: Int, cy: Int, r: Int, argb: Int) {
    val minR2 = (r - 1) * (r - 1)
    val maxR2 = r * r
    for (dy in -r..r) {
      for (dx in -r..r) {
        val d2 = dx * dx + dy * dy
        if (d2 in minR2..maxR2) {
          set(cx + dx, cy + dy, argb)
        }
      }
    }
  }

  fun drawTerracottaRoof(
    centerX: Int,
    topY: Int,
    heightRows: Int,
    topHalfSpan: Int,
    bottomHalfSpan: Int,
    shingleDark: Int = 0xFF4E1B24.toInt(),
    shingleMid: Int = 0xFF7D2A33.toInt(),
    shingleWarm: Int = 0xFF9E383F.toInt(),
    shingleHi: Int = 0xFFBF4B4C.toInt(),
    ridgeGold: Int = 0xFFD69A43.toInt(),
  ) {
    for (ry in 0 until heightRows) {
      val y = topY + ry
      val span = topHalfSpan + (bottomHalfSpan - topHalfSpan) * ry / heightRows.coerceAtLeast(1)
      val xStart = centerX - span
      val xEnd = centerX + span
      val isRowLip = (ry % 4 == 3)
      for (px in xStart..xEnd) {
        val relX = px - centerX
        val shingleCol = ((px + (ry / 4) * 2) % 4 == 0)
        val col = when {
          ry == 0 -> ridgeGold
          ry == 1 -> shingleHi
          isRowLip -> shingleDark
          shingleCol -> shingleDark
          relX < -span / 3 -> shingleHi
          relX < span / 4 -> shingleWarm
          else -> shingleMid
        }
        set(px, y, col)
      }
      if (ry == heightRows - 1) {
        hLine(xStart - 1, y + 1, (span * 2) + 3, 0xFF2B151D.toInt())
      }
    }
  }

  fun drawHalfTimberWall(
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    plasterBase: Int = 0xFFC4A482.toInt(),
    plasterShadow: Int = 0xFF9C7E60.toInt(),
    timberDark: Int = 0xFF3B231A.toInt(),
    timberHi: Int = 0xFF593728.toInt(),
  ) {
    fillRect(x, y, w, h, plasterBase)
    fillRect(x, y, w, 3, plasterShadow)
    fillRect(x + w - 5, y, 5, h, plasterShadow)
    fillRect(x, y, 3, h, timberDark)
    fillRect(x + w - 3, y, 3, h, timberDark)
    hLine(x, y, w, timberDark)
    hLine(x, y + h - 2, w, timberDark)
    hLine(x, y + h / 2, w, timberDark)
    hLine(x + 1, y + h / 2 - 1, w - 2, timberHi)
    // Weathering follows construction: plaster under the eaves, grain along structural beams.
    for (row in 4 until h - 3 step 5) {
      val start = x + 5 + (row * 3 % 7)
      hLine(start, y + row, (w / 5).coerceAtLeast(2), plasterShadow)
    }
    for (row in 4 until h - 3) {
      if (row % 7 != 0) {
        set(x + 1, y + row, timberHi)
        set(x + w - 2, y + row, timberHi)
      }
    }
    val step = (w / 4).coerceAtLeast(8)
    var sx = x + step
    while (sx < x + w - 5) {
      fillRect(sx, y, 2, h, timberDark)
      sx += step
    }
    for (i in 0 until (h / 2 - 3).coerceAtMost(10)) {
      set(x + 3 + i, y + 2 + i, timberDark)
      set(x + w - 4 - i, y + 2 + i, timberDark)
    }
  }

  fun drawGlowingWindow(x: Int, y: Int, w: Int, h: Int) {
    fillRect(x - 1, y - 1, w + 2, h + 2, 0xFF2D1912.toInt())
    fillRect(x, y, w, h, 0xFFF59E0B.toInt())
    fillRect(x + 1, y + 1, (w - 2).coerceAtLeast(1), (h - 2).coerceAtLeast(1), 0xFFFBBF24.toInt())
    if (w >= 6 && h >= 6) {
      fillRect(x + 2, y + 2, w - 4, h - 4, 0xFFFEF08A.toInt())
    }
    vLine(x + w / 2, y, h, 0xFF3E2216.toInt())
    hLine(x, y + h / 2, w, 0xFF3E2216.toInt())
  }

  fun applyCrispOutline(outlineArgb: Int = 0xFF0B0814.toInt()) {
    val copy = pixels.copyOf()
    for (y in 0 until height) {
      for (x in 0 until width) {
        if ((copy[y * width + x] ushr 24) == 0) {
          val hasNeighbor =
            (x > 0 && (copy[y * width + (x - 1)] ushr 24) > 128) ||
              (x + 1 < width && (copy[y * width + (x + 1)] ushr 24) > 128) ||
              (y > 0 && (copy[(y - 1) * width + x] ushr 24) > 128) ||
              (y + 1 < height && (copy[(y + 1) * width + x] ushr 24) > 128)
          if (hasNeighbor) {
            pixels[y * width + x] = outlineArgb
          }
        }
      }
    }
  }

  fun flipHorizontalCanvas(): PixelCanvas {
    val flipped = PixelCanvas(width, height)
    for (y in 0 until height) {
      for (x in 0 until width) {
        flipped.pixels[y * width + (width - 1 - x)] = pixels[y * width + x]
      }
    }
    return flipped
  }

  fun toImageBitmap(): ImageBitmap {
    val bmp = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    return bmp.asImageBitmap()
  }
}

object PixelTileAtlas {

  const val TILE_PX = 64

  /** 4 x 4 grass tiles per pre-composed overview block, with 8 authored block variants. */
  const val GRASS_BLOCK_TILES = 4
  const val GRASS_BLOCK_COUNT = 8
  const val GRASS_BLOCK_WORLD_SIZE = TILE_PX * GRASS_BLOCK_TILES

  // Reference-Accurate Sunlit Anime Sanctuary Lawn, Sand, Alabaster Stone & Grey Cobble Tiles (64x64)
  val sanctuaryGrassTiles: List<ImageBitmap> get() = SanctuaryArtAssets.tiles("grass", 8) ?: sanctuaryGrassTilesFallback
  private val sanctuaryGrassTilesFallback: List<ImageBitmap> by lazy {
    (0 until 8).map { variant -> SanctuaryMaterials.grass(variant) }
  }

  val sanctuarySandTiles: List<ImageBitmap> get() = SanctuaryArtAssets.tiles("soil", 6) ?: sanctuarySandTilesFallback
  private val sanctuarySandTilesFallback: List<ImageBitmap> by lazy {
    (0 until 6).map { variant -> SanctuaryMaterials.soil(variant) }
  }

  val sanctuaryStoneTiles: List<ImageBitmap> get() = SanctuaryArtAssets.tiles("flagstone", 8) ?: sanctuaryStoneTilesFallback
  private val sanctuaryStoneTilesFallback: List<ImageBitmap> by lazy {
    (0 until 8).map { variant -> SanctuaryMaterials.stone(variant, large = true) }
  }

  val sanctuaryCausewayTiles: List<ImageBitmap> get() = SanctuaryArtAssets.tiles("cobble", 6) ?: sanctuaryCausewayTilesFallback
  private val sanctuaryCausewayTilesFallback: List<ImageBitmap> by lazy {
    (0 until 6).map { variant -> SanctuaryMaterials.stone(variant, large = false) }
  }

  val sanctuaryRoadCurbTiles: List<ImageBitmap> by lazy {
    (0 until 4).map { variant -> createSanctuaryCurbTile(variant) }
  }

  /**
   * Pre-composed 4 x 4 grass blocks, 256 x 184 px: sixteen authored 64 x 64 tiles already
   * placed on the same 0.72 foreshortened ground plane the renderer uses. The overview
   * camera draws one block instead of sixteen tiles, which removes ~90% of the terrain
   * draw commands. Gameplay and inspection zoom keep the per-tile path, so the sharp
   * nearest-neighbour art seen up close is completely unaffected.
   */
  val sanctuaryGrassBlocks: List<ImageBitmap> by lazy { buildSanctuaryGrassBlocks() }

  private fun buildSanctuaryGrassBlocks(): List<ImageBitmap> {
    val tiles = sanctuaryGrassTiles
    if (tiles.isEmpty()) return emptyList()
    val sources = ArrayList<IntArray>(tiles.size)
    var srcW = 0
    var srcH = 0
    for (tile in tiles) {
      val bitmap = tile.asAndroidBitmap()
      if (bitmap.width <= 0 || bitmap.height <= 0) return emptyList()
      srcW = bitmap.width
      srcH = bitmap.height
      val pixels = IntArray(srcW * srcH)
      bitmap.getPixels(pixels, 0, srcW, 0, 0, srcW, srcH)
      sources.add(pixels)
    }
    if (sources.isEmpty() || srcW <= 0 || srcH <= 0) return emptyList()

    val cellW = TILE_PX
    val cellH = (TILE_PX * ObliqueCamera.OBLIQUE_Y_SCALE).toInt().coerceAtLeast(1)
    val blockW = cellW * GRASS_BLOCK_TILES
    val blockH = cellH * GRASS_BLOCK_TILES
    val blocks = ArrayList<ImageBitmap>(GRASS_BLOCK_COUNT)
    for (block in 0 until GRASS_BLOCK_COUNT) {
      val pixels = IntArray(blockW * blockH)
      for (cellY in 0 until GRASS_BLOCK_TILES) {
        for (cellX in 0 until GRASS_BLOCK_TILES) {
          // Same deterministic variant rule the per-tile renderer uses, applied to a
          // representative tile coordinate for this block variant.
          val tx = (block and 3) * GRASS_BLOCK_TILES + cellX
          val ty = (block shr 2) * GRASS_BLOCK_TILES + cellY
          val hash = ((tx * 73856093) xor (ty * 19349663)) and 0x7FFFFFFF
          val src = sources[hash % sources.size]
          for (y in 0 until cellH) {
            val srcRow = ((y * srcH) / cellH) * srcW
            val dstRow = (cellY * cellH + y) * blockW + cellX * cellW
            for (x in 0 until cellW) {
              pixels[dstRow + x] = src[srcRow + (x * srcW) / cellW]
            }
          }
        }
      }
      blocks.add(Bitmap.createBitmap(pixels, blockW, blockH, Bitmap.Config.ARGB_8888).asImageBitmap())
    }
    return blocks
  }

  val sanctuaryWaterTiles: List<ImageBitmap> by lazy {
    (0 until 6).map { variant -> createSanctuaryWaterTile(variant) }
  }

  val sanctuaryGravelTiles: List<ImageBitmap> get() = SanctuaryArtAssets.tiles("gravel", 4) ?: sanctuaryGravelTilesFallback
  private val sanctuaryGravelTilesFallback: List<ImageBitmap> by lazy {
    (0 until 4).map { variant -> SanctuaryMaterials.soil(variant, gravel = true) }
  }

  // Handcrafted Foreshortened 3/4 Central Astrolabe Medallion (224 x 152)
  val astrolabePlazaMedallion: ImageBitmap by lazy { createAstrolabePlazaMedallion() }

  // Restaurant Interior 3/4 Oak Planks & Irregular Stone Walkway
  val tavernWoodPlankTiles: List<ImageBitmap> get() = SanctuaryArtAssets.tiles("wood", 6) ?: tavernWoodPlankTilesFallback
  private val tavernWoodPlankTilesFallback: List<ImageBitmap> by lazy {
    (0 until 6).map { variant -> SanctuaryMaterials.wood(variant) }
  }

  val tavernFlagstoneAisleTiles: List<ImageBitmap> by lazy {
    (0 until 4).map { variant -> createTavernFlagstoneTile(variant) }
  }

  // Forest & Grassland 3/4 Flora, Weathered Cobbles & Ochre Borders
  val meadowGrassTiles: List<ImageBitmap> by lazy {
    (0 until 8).map { variant -> createMeadowGrassTile(variant) }
  }

  val meadowDirtRoadTiles: List<ImageBitmap> by lazy {
    (0 until 6).map { variant -> createMeadowStonePathTile(variant) }
  }

  val meadowRoadBorderTiles: List<ImageBitmap> by lazy {
    (0 until 4).map { variant -> createMeadowOchreBorderTile(variant) }
  }

  val elderwoodFloorTiles: List<ImageBitmap> by lazy {
    (0 until 4).map { variant -> createElderwoodTile(variant) }
  }

  val ruinCitadelFloorTiles: List<ImageBitmap> by lazy {
    (0 until 4).map { variant -> createRuinCitadelTile(variant) }
  }

  /**
   * V3 PREMIUM GRASS — 8 genuinely distinct 64×64 seamless variants (not seed-only).
   * Large: organic moss/earth mats (rx 10-16) define silhouette; Medium: blade clusters + tufts + fern flecks;
   * Small: grit/quartz + highlight tips reinforcing large form. Palette disciplined to dark-fantasy forest floor.
   * Each variant has intentional density/character: 0 prime lawn, 1 deep shade, 2 bloom glade, 3 worn verge,
   * 4 thicket fringe, 5 stonefield, 6 ridge highlight, 7 ancient hollow. Tested 3×3 — interior 8..56, 2px edge clear.
   */
  private fun createSanctuarySunlitGrassTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(7100L + variant * 9731L)
    // Base shifts subtly per variant within dark-fantasy greens — preserves palette, adds variety
    val baseByVar = intArrayOf(0xFF2B352E.toInt(), 0xFF27302C.toInt(), 0xFF2C362F.toInt(), 0xFF2E352D.toInt(), 0xFF283128.toInt(), 0xFF2D342A.toInt(), 0xFF31392E.toInt(), 0xFF252E2A.toInt())
    val baseLawn = baseByVar[variant % 8]
    c.fillRect(0, 0, TILE_PX, TILE_PX, baseLawn)

    val deepShadow = 0xFF162118.toInt(); val midMoss = 0xFF2A3D2B.toInt()
    val mutedMid = 0xFF344E35.toInt(); val mossLight = 0xFF3D5A36.toInt()
    val mossHi = 0xFF4A6B3E.toInt(); val frostTip = 0xFF5A7A52.toInt()

    // LARGE: variant-authored organic mats — each variant different patch logic (not random 11 ellipses)
    fun mossMat(cx: Int, cy: Int, rx: Int, ry: Int, col: Int, withVein: Boolean) {
      c.fillEllipse(cx, cy, rx, ry, col)
      if (withVein) c.fillEllipse(cx + 2, cy - 1, (rx*0.35f).toInt().coerceAtLeast(2), 2, 0xFF3A4E3E.toInt())
      // second lobe for organic irregularity
      c.fillEllipse(cx - rx/3, cy + ry/3, (rx*0.62f).toInt(), (ry*0.72f).toInt(), col)
    }
    when (variant) {
      0 -> { // Prime lawn — 3 broad moss mats, airy
        mossMat(22, 18, 12, 7, 0xFF25302A.toInt(), true); mossMat(42, 30, 11, 6, 0xFF2F3D33.toInt(), false); mossMat(28, 48, 10, 6, 0xFF232E28.toInt(), true)
      }
      1 -> { // Deep shade — 4 darker, denser mats + damp center
        mossMat(18, 22, 13, 8, 0xFF1F2A24.toInt(), false); mossMat(40, 14, 9, 5, 0xFF1B2620.toInt(), false); mossMat(44, 42, 12, 7, 0xFF232E28.toInt(), true); mossMat(18, 46, 9, 5, 0xFF25302A.toInt(), false)
        c.fillEllipse(32, 34, 8, 5, 0xFF1A2120.toInt())
      }
      2 -> { // Bloom glade — open center, 2 mats at edges for flower breathing room
        mossMat(16, 16, 10, 6, 0xFF2F3D33.toInt(), true); mossMat(48, 48, 11, 7, 0xFF25302A.toInt(), true); c.fillEllipse(42, 12, 6, 3, 0xFF2F3D33.toInt())
      }
      3 -> { // Worn verge — earth braid replaces one moss mat
        mossMat(20, 20, 11, 6, 0xFF25302A.toInt(), false); mossMat(46, 46, 9, 5, 0xFF232E28.toInt(), false)
        c.fillEllipse(34, 32, 14, 7, 0xFF2E2A24.toInt()); c.fillEllipse(36, 34, 6, 3, 0xFF252018.toInt()) // compacted soil large
      }
      4 -> { // Thicket fringe — 4 small tight mats, dense
        mossMat(16, 18, 9, 5, 0xFF25302A.toInt(), false); mossMat(38, 12, 8, 5, 0xFF334739.toInt(), false); mossMat(44, 32, 9, 5, 0xFF2F3D33.toInt(), true); mossMat(24, 44, 10, 6, 0xFF232E28.toInt(), false)
      }
      5 -> { // Stonefield — moss mats reduced, stone emphasis later
        mossMat(20, 24, 10, 6, 0xFF25302A.toInt(), true); mossMat(44, 36, 10, 6, 0xFF232E28.toInt(), false)
      }
      6 -> { // Ridge highlight — lighter mats, airy
        mossMat(24, 20, 12, 7, 0xFF2F3D33.toInt(), true); mossMat(40, 44, 11, 6, 0xFF334739.toInt(), true)
      }
      7 -> { // Ancient hollow — deepest, 3 mats + central leaf litter zone
        mossMat(18, 18, 13, 8, 0xFF1B2620.toInt(), false); mossMat(44, 20, 11, 6, 0xFF25302A.toInt(), true); mossMat(30, 46, 12, 7, 0xFF1F2A24.toInt(), false)
        c.fillEllipse(34, 32, 10, 6, 0xFF2A2620.toInt())
      }
    }
    // MEDIUM: earth exposure — variant-specific, not generic random
    val dirtCols = intArrayOf(0xFF2E2A24.toInt(), 0xFF3A352E.toInt(), 0xFF2A2620.toInt(), 0xFF3E342A.toInt())
    when (variant) {
      3 -> { repeat(2) { i -> val cx = 28 + i*12; c.fillEllipse(cx, 30, 7+i, 4, dirtCols[i]); c.fillEllipse(cx+1, 32, 3, 2, 0xFF252018.toInt()) } }
      7 -> { c.fillEllipse(30, 34, 8, 4, 0xFF3A352E.toInt()); c.fillEllipse(32, 36, 4, 2, 0xFF2A2620.toInt()) }
      0, 2 -> { c.fillEllipse(40, 18, 5, 3, 0xFF2E2A24.toInt()) } // single faint worn spot
      5 -> { c.fillEllipse(38, 28, 6, 3, 0xFF2A2620.toInt()) }
    }
    // LARGE grit — cold slate pebbles, restrained 12-18 per variant, 2px edge clear, clustered not uniform
    val gritCount = when (variant) { 5 -> 18; 1 -> 12; 3 -> 14; else -> 14 }
    repeat(gritCount) {
      val sx = 8 + rng.nextInt(48); val sy = 8 + rng.nextInt(48)
      val col = when (rng.nextInt(4)) { 0->0xFF3A3F45.toInt(); 1->0xFF4A4D55.toInt(); 2->0xFF353A42.toInt(); else->0xFF5A5F6E.toInt() }
      // cluster grit near moss mats / earth, not uniform noise
      c.set(sx, sy, col); if (rng.nextInt(8)==0 && sx<62) c.set(sx+1, sy, 0xFF2A2E34.toInt())
      if (rng.nextInt(9)==0 && sx<62 && sy<62) c.set(sx, sy+1, 0xFF3A3F45.toInt())
    }
    // If stonefield, add 2 larger pebbles with highlight
    if (variant==5) { c.fillEllipse(18, 40, 3, 2, 0xFF4A4D55.toInt()); c.set(17,39,0xFF5A5F6E.toInt()); c.fillEllipse(46, 16, 2,2,0xFF3A3F45.toInt()) }

    // MEDIUM foliage ground spice — 75-95 flecks, tiered dark→light, reinforcing moss mats
    val fleckCount = when (variant) { 4->95; 1->85; 6->75; else->82 }
    repeat(fleckCount) {
      val gx = 8 + rng.nextInt(48); val gy = 8 + rng.nextInt(48)
      val roll = rng.nextInt(8); val col = when (roll) { 0->deepShadow; 1->0xFF1E2620.toInt(); 2->midMoss; 3,4->mutedMid; 5->mossLight; 6->mossHi; else->frostTip }
      c.set(gx, gy, col); if (rng.nextInt(6)==0 && gx<62) c.set(gx+1, gy, midMoss)
      if (roll==6 && rng.nextInt(5)==0 && gy>6) c.set(gx, gy-1, mossLight)
    }
    // MEDIUM tufts — variant-specific density, bent lean per texture-art-direction
    val tuftCount = when (variant) { 4->14; 1->9; 3->8; 6->10; else->11 }
    repeat(tuftCount) {
      val tx = 10 + rng.nextInt(44); val ty = 14 + rng.nextInt(40); val lean = if (rng.nextBoolean()) 1 else 0
      c.hLine(tx-1, ty+1, 4, 0xFF1A1E1B.toInt()); c.hLine(tx-1, ty, 3, deepShadow)
      c.vLine(tx-1+lean, ty-2, 3, mossLight); c.vLine(tx+lean, ty-4, 5, mossHi); c.vLine(tx+1+lean, ty-2, 3, mutedMid)
      if (rng.nextInt(4)==0) c.set(tx+lean, ty-5, frostTip)
    }
    // SMALL clover/fern — 3-pixel clusters, 4-6 per tile
    repeat(if (variant==4) 7 else 5) {
      val cx = 10 + rng.nextInt(44); val cy = 10 + rng.nextInt(44)
      c.set(cx, cy, 0xFF2E4A32.toInt()); c.set(cx+1, cy, 0xFF3D5A36.toInt()); c.set(cx, cy+1, 0xFF253A28.toInt())
      if (rng.nextInt(3)==0) c.set(cx+1, cy+1, 0xFF2A3D2B.toInt())
    }
    // SMALL wildflowers — variant 2 & 7 only, petal structure with stem AO
    if (variant==2 || variant==7) {
      repeat(if (variant==2) 2 else 1) { fIdx ->
        val fx = 14 + (variant*13 + fIdx*17) % 38; val fy = 14 + (variant*11 + fIdx*19) % 38
        if ((variant+fIdx)%2==0) { c.set(fx-1, fy, 0xFF8A7A97.toInt()); c.set(fx+1, fy, 0xFF8A7A97.toInt()); c.set(fx, fy-1, 0xFF9A8AAE.toInt()); c.set(fx, fy, 0xFF6E6080.toInt()); c.set(fx, fy+1, 0xFF5A4E6A.toInt()) }
        else { c.set(fx-1, fy, 0xFF9A6570.toInt()); c.set(fx+1, fy, 0xFF9A6570.toInt()); c.set(fx, fy-1, 0xFFB08088.toInt()); c.set(fx, fy, 0xFF7A4A52.toInt()) }
        c.set(fx, fy+2, 0xFF1E2A1F.toInt())
      }
    }
    // SMALL leaf litter / mushrooms — variants 1 & 7 only
    if (variant==1 || variant==7) {
      repeat(3) { val lx=14+rng.nextInt(36); val ly=14+rng.nextInt(36); c.set(lx, ly, 0xFF5A4630.toInt()); c.set(lx+1, ly, 0xFF4A3520.toInt()); c.set(lx, ly+1, 0xFF3E2E1E.toInt()) }
      if (variant==7) { val mx=20+rng.nextInt(24); val my=20+rng.nextInt(24); c.set(mx, my, 0xFFC2B8A8.toInt()); c.set(mx, my+1, 0xFF8A7A6A.toInt()) }
    }
    return c.toImageBitmap()
  }

  /**
   * V3 PREMIUM DIRT — 6 distinct dark-fantasy forest floor variants.
   * Large: broad moisture/value patches (rx 10-16, damp vs trampled); Medium: embedded stones + fine grains + root veins;
   * Small: wear scuffs/moss transitions/puddle sheen reinforcing large form. Not flat brown with dots.
   */
  private fun createSanctuaryTrainingSandTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(7600L + variant * 1731L)
    val sandBase = 0xFF5C4E3A.toInt(); val sandMid = 0xFF6B5C45.toInt(); val sandHi = 0xFF7A6B55.toInt()
    c.fillRect(0, 0, TILE_PX, TILE_PX, sandBase)
    // LARGE: broad irregular earth patches — variant-authored, not 7 random ellipses
    fun earthPatch(cx: Int, cy: Int, rx: Int, ry: Int, col: Int, withRidge: Boolean) {
      c.fillEllipse(cx, cy, rx, ry, col); if (withRidge) c.fillEllipse(cx+3, cy+1, (rx*0.38f).toInt(), 2, 0xFF6B5C45.toInt())
    }
    when (variant) {
      0 -> { earthPatch(22, 20, 14, 8, sandMid, true); earthPatch(44, 36, 12, 7, 0xFF524A3A.toInt(), false); earthPatch(18, 44, 8, 5, 0xFF4E3E2E.toInt(), false) }
      1 -> { earthPatch(30, 28, 16, 9, 0xFF4E3E2E.toInt(), false); earthPatch(16, 38, 9, 5, sandMid, true) } // damp compacted center
      2 -> { earthPatch(20, 16, 12, 7, sandMid, false); earthPatch(42, 26, 10, 6, 0xFF524A3A.toInt(), true); earthPatch(36, 48, 9, 5, 0xFF4E3E2E.toInt(), false) }
      3 -> { earthPatch(28, 32, 15, 8, 0xFF524A3A.toInt(), true); earthPatch(18, 42, 8, 4, 0xFF4E3E2E.toInt(), false) } // puddle sheen variant
      4 -> { earthPatch(24, 22, 13, 7, sandMid, true); earthPatch(42, 40, 11, 6, 0xFF524A3A.toInt(), false) }
      5 -> { earthPatch(32, 18, 11, 6, 0xFF4E3E2E.toInt(), false); earthPatch(16, 32, 12, 7, sandMid, true); earthPatch(46, 44, 8, 4, 0xFF524A3A.toInt(), false) }
    }
    // MEDIUM: embedded stones — 8-12 per tile, size 1-2, clustered near patches, not 65 uniform dots
    val stoneCols = intArrayOf(sandHi, 0xFF3E3328.toInt(), 0xFF8A7A6A.toInt(), 0xFF6B5C45.toInt())
    val stoneCount = when (variant) { 2->12; 5->10; else->9 }
    repeat(stoneCount) {
      val sx = 10 + rng.nextInt(44); val sy = 10 + rng.nextInt(44); val col = stoneCols[rng.nextInt(stoneCols.size)]
      val sz = if (rng.nextInt(4)==0) 2 else 1; if (sz==1) c.set(sx, sy, col) else { c.fillEllipse(sx, sy, 1,1,col); c.set(sx-1, sy-1, 0xFF7A6B55.toInt()) }
      if (rng.nextInt(6)==0) c.set(sx+1, sy, 0xFF4A3D2E.toInt())
    }
    // fine soil grains — 22-30 restrained, darker between stones
    repeat(24) { val gx=8+rng.nextInt(48); val gy=8+rng.nextInt(48); c.set(gx, gy, 0xFF4A3D2E.toInt()); if (rng.nextInt(5)==0) c.set(gx+1, gy, 0xFF3A2E24.toInt()) }
    // root / organic vein — 1-2 per variant, 2px lines following patch edge
    when (variant) {
      0 -> { c.hLine(12, 28, 8, 0xFF3E2E1E.toInt()); c.hLine(13, 29, 6, 0xFF4A3520.toInt()) }
      1 -> { c.vLine(40, 16, 8, 0xFF3E2E1E.toInt()); c.set(41, 18, 0xFF5A4630.toInt()) }
      2 -> { c.hLine(18, 36, 10, 0xFF3A2E24.toInt()) }
      5 -> { c.vLine(24, 30, 7, 0xFF382A1E.toInt()) }
    }
    // SMALL wear: scuff arcs, footprints, weapon drag — story marks, not every tile
    if (variant==1 || variant==4) {
      val px=18+variant*5; val py=22+variant*4; c.hLine(px, py, 9, 0xFF4A3D2E.toInt()); c.hLine(px+1, py+1, 7, 0xFF7A6B55.toInt()); c.hLine(px+2, py+2, 5, 0xFF3A2E24.toInt()); c.vLine(px+5, py-2, 4, 0xFF5A4A3A.toInt())
    }
    if (variant%2==0) { c.fillEllipse(8, 10, 6, 3, 0xFF2A3D2B.toInt()); c.fillEllipse(9, 9, 3, 2, 0xFF3D5A36.toInt()) } // moss encroachment
    if (variant==3) { c.hLine(28, 34, 6, 0xFF6B6A5A.toInt()); c.hLine(29, 35, 4, 0xFF8A8A7A.toInt()) } // puddle sheen
    return c.toImageBitmap()
  }

  /**
   * V3 PREMIUM STONE — 8 distinct plaza/flagstone variants. Each slab readable shape (cw 18-28, ch 16),
   * irregular staggering, recessed mortar 1A1E24, top-plane highlight AZ142, selective cracks/moss/runes.
   * Large: course layout; Medium: individual slab faces + joints; Small: chip/crack/moss reinforcing stone.
   */
  private fun createSanctuaryStoneTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(1000L + variant * 7137L)
    val mortar = 0xFF1A1E24.toInt()
    val bases = intArrayOf(0xFF3A3F4A.toInt(), 0xFF444A56.toInt(), 0xFF353A45.toInt(), 0xFF40454F.toInt(), 0xFF3C414E.toInt())
    val hi = 0xFF5A5F6E.toInt(); val sh = 0xFF23262E.toInt()
    val rowHeights = intArrayOf(16,16,16,16); var curY=0
    for (r in rowHeights.indices) {
      val rh=rowHeights[r]; val offset=((variant*13 + r*17) % 26); var curX=-offset; var idx=0
      while (curX < TILE_PX) {
        val cw = when (variant) { 0->20+((idx)%3)*6; 1->22+((idx+1)%3)*5; 2->18+((idx+2)%3)*7; 3->20+((idx)%2)*8; 4->19+((idx)%3)*6; 5->21+((idx+1)%2)*7; 6->18+((idx)%3)*6; else->20+((idx+2)%3)*6 }
        val base = bases[(variant + r*2 + idx)%bases.size]
        val hiRow = when { r==0 -> 0xFF656A7A.toInt(); variant==2 && r==1 -> 0xFF60667A.toInt(); else->hi }
        // Vary slab height subtly per variant for non-square feel
        val slabH = if (variant==3 && r==2) rh-1 else rh
        c.beveledSlab(curX, curY, cw, slabH, base, hiRow, sh, mortar, rng, hi, sh)
        // Stone face detail: horizontal tool mark + vertical hairline, sparse
        if (rng.nextInt(8)==0) c.hLine(curX+4, curY+slabH/2, 5, 0xFF4A4F5C.toInt())
        if (rng.nextInt(9)==0) c.vLine(curX+cw/2, curY+3, slabH-6, 0xFF1E2026.toInt())
        // Moss only where plausible: damp corner/joint, not every stone
        if ((variant + r + idx)%9==0) { c.set(curX+cw-4, curY+slabH-3, 0xFF2A332E.toInt()); if (rng.nextBoolean()) c.set(curX+cw-3, curY+slabH-4, 0xFF1E2A1F.toInt()) }
        if (rng.nextInt(12)==0) c.set(curX+3, curY+4, 0xFF252830.toInt()) // chip
        curX+=cw; idx++
      }; curY+=rh
    }
    // Variant-specific storytelling: rune scratches (3,7), moss veins (1,5), quarry vein (2)
    when (variant) {
      3,7 -> { c.hLine(18, 30, 10, 0xFF6B7A9A.toInt()); c.set(22,31,0xFF7CA8C8.toInt()); c.set(23,31,0xFF4A6080.toInt()) }
      1,5 -> { c.fillEllipse(12,14,4,2,0xFF2A332E.toInt()); c.set(13,13,0xFF3D5A36.toInt()); c.set(14,12,0xFF1E2A1F.toInt()) }
      2 -> { c.hLine(8, 44, 12, 0xFF2A332E.toInt()); c.set(10,43,0xFF1E2A1F.toInt()) } // damp joint
      6 -> { c.vLine(42, 18, 6, 0xFF1E2026.toInt()); c.set(43,20,0xFF2A332E.toInt()) }
    }
    return c.toImageBitmap()
  }

  /**
   * V3 PREMIUM COBBLE — 6 variants, each stone readable (cw 10-16, ch 8), irregular but consistent scale,
   * dark recessed joints, chipped weathered edges, selective moss only where damp, stone tone variation.
   * Path silhouette seamless via roadUnionPath; this tile supplies interior. Large→course, Medium→slab, Small→wear.
   */
  private fun createSanctuaryGreyCobbleTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(2200L + variant * 917L)
    val mortar = 0xFF1B1E24.toInt()
    val cobblePalette = intArrayOf(0xFF3E424D.toInt(), 0xFF4B4F5C.toInt(), 0xFF383C46.toInt(), 0xFF525663.toInt(), 0xFF2F333E.toInt(), 0xFF424654.toInt())
    val hi = 0xFF5E636F.toInt(); val sh = 0xFF252830.toInt()
    val rowHeights = intArrayOf(8,8,8,8,8,8,8,8); var py=0
    for (row in rowHeights.indices) {
      val rh=rowHeights[row]; var px = -((row*7 + variant*5 + (variant%2)*3) % 12); var colIdx=0
      while (px < TILE_PX) {
        val cw = when (variant) { 0->10+((colIdx)%3)*4; 1->12+((colIdx+1)%2)*4; 2->10+((colIdx+2)%3)*3; 3->11+((colIdx)%2)*5; 4->10+((colIdx)%3)*4; else->12+((colIdx)%3)*3 }
        val base = cobblePalette[(variant*2 + row + colIdx)%cobblePalette.size]
        c.beveledSlab(px, py, cw, rh, base, hi, sh, mortar, rng, 0xFF5A5F6E.toInt(), 0xFF1E2024.toInt())
        // Distinct stone face: highlight on top lip already via beveledSlab, add secondary edge wear
        if (rng.nextInt(14)==0) c.set(px+cw/2, py+rh/2, 0xFF525663.toInt())
        if ((variant+row+colIdx)%7==0) { c.set(px+2, py+2, 0xFF2A332E.toInt()); c.set(px+3, py+3, 0xFF1E2A1F.toInt()) } // moss in joint, sparse
        else if (rng.nextInt(18)==0) c.set(px+2, py+1, 0xFF1B1E24.toInt()) // recessed joint darkening
        // Corner chip reinforcing stone shape
        if (rng.nextInt(20)==0) c.set(px+1, py+1, mortar)
        px+=cw; colIdx++
      }; py+=rh
    }
    when (variant) { 2->{ c.hLine(8,30,16,0xFF1E2026.toInt()) } 5->{ c.hLine(10,42,8,0xFF2A2E3A.toInt()); c.set(14,43,0xFF4A4F5C.toInt()) } 0->{ c.vLine(30, 12, 4, 0xFF1E2026.toInt()) } }
    return c.toImageBitmap()
  }

  /** V3 PREMIUM CURB — 4 variants, weathered stone coping, 16px courses, edge wear, selective moss. */
  private fun createSanctuaryCurbTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(2600L + variant * 1061L)
    val mortar = 0xFF1A1E24.toInt(); val rowHeights = intArrayOf(16,16,16,16); var py=0
    for (r in rowHeights.indices) {
      val rh=rowHeights[r]; var px=-((variant*8 + r*11)%15); var idx=0
      while (px < TILE_PX) {
        val cw = 16 + ((idx+r+variant)%2)*6
        val base = if ((r+idx+variant)%2==0) 0xFF3E424D.toInt() else 0xFF383C46.toInt()
        val hi=0xFF525663.toInt(); val sh=0xFF1E2026.toInt()
        c.beveledSlab(px, py, cw, rh, base, hi, sh, mortar, rng, hi, sh)
        // curb top highlight stronger, edge chip
        if (r==0 && rng.nextInt(3)==0) c.hLine(px+2, py+1, cw-4, 0xFF5C6170.toInt())
        if (rng.nextInt(16)==0) c.set(px+1, py+1, 0xFF1B1E24.toInt())
        px+=cw; idx++
      }; py+=rh
    }
    when (variant) { 0,2->{ c.fillEllipse(14+variant*4, 16, 4,2,0xFF2A332E.toInt()); c.set(14+variant*4,15,0xFF3D5A36.toInt()) } 1,3->{ c.vLine(8+variant*6, 12, 5, 0xFF1E2026.toInt()); c.set(9+variant*6,14,0xFF2A332E.toInt()) } }
    return c.toImageBitmap()
  }

  /**
   * Handcrafted Pixel-Art Foreshortened 3/4 Central Astrolabe Medallion (224 x 152 px).
   */
  private fun createAstrolabePlazaMedallion(): ImageBitmap {
    val w = 224
    val h = 152
    val c = PixelCanvas(w, h)
    val cx = w / 2
    val cy = h / 2

    val stoneOuter = 0xFFE5E1D8.toInt()
    val stoneInner = 0xFFF2EFE9.toInt()
    val ringLine = 0xFFB8B2A6.toInt()
    val azureGlow = 0xFF7DD3FC.toInt()
    val goldMid = 0xFFEAB308.toInt()
    val goldBright = 0xFFFDE047.toInt()

    c.fillEllipse(cx, cy + 2, 108, 71, 0xFFC7C1B6.toInt())
    c.fillEllipse(cx, cy, 106, 69, stoneOuter)
    c.fillEllipse(cx, cy, 92, 60, stoneInner)
    c.fillEllipse(cx, cy, 62, 40, stoneOuter)
    c.fillEllipse(cx, cy, 38, 25, stoneInner)

    c.strokeEllipse(cx, cy, 105, 68, ringLine, thickness = 2)
    c.strokeEllipse(cx, cy, 92, 60, ringLine, thickness = 2)
    c.strokeEllipse(cx, cy, 76, 49, goldMid, thickness = 1)
    c.strokeEllipse(cx, cy, 62, 40, ringLine, thickness = 2)
    c.strokeEllipse(cx, cy, 38, 25, azureGlow, thickness = 2)

    for (i in 0 until 12) {
      val ang = i * (Math.PI * 2.0 / 12.0)
      val cosA = cos(ang).toFloat()
      val sinA = sin(ang).toFloat()
      for (step in 26..92 step 2) {
        val px = cx + (cosA * step).roundToInt()
        val py = cy + (sinA * step * 0.65f).roundToInt()
        c.set(px, py, ringLine)
      }
      val rxNode = cx + (cosA * 84f).roundToInt()
      val ryNode = cy + (sinA * 84f * 0.65f).roundToInt()
      c.fillEllipse(rxNode, ryNode, 3, 2, if (i % 2 == 0) goldBright else azureGlow)
    }

    c.fillEllipse(cx, cy, 8, 5, azureGlow)
    c.fillEllipse(cx, cy, 4, 3, 0xFFFFFFFF.toInt())
    return c.toImageBitmap()
  }

  /**
   * V3 PREMIUM WOOD — 6 oak board variants, each board readable (ch 8, seam 3B2112), end joints staggered,
   * warm oak palette with distinct grain per board, knots/figuring, scuff wear, nail heads, edge AO.
   * Large: board course; Medium: individual grain/knot; Small: lip highlight + nail shadow.
   */
  private fun createTavernWoodPlankTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(2800L + variant * 913L)
    val seam = 0xFF3B2112.toInt()
    val planks = intArrayOf(0xFF8C5A38.toInt(), 0xFF9C6641.toInt(), 0xFF7D4E30.toInt(), 0xFF945F3C.toInt(), 0xFF855030.toInt(), 0xFFA06A42.toInt())
    val heights = intArrayOf(8,8,8,8,8,8,8,8); var y0=0
    for (row in heights.indices) {
      val rh=heights[row]; val base=planks[(variant*3 + row)%planks.size]
      // board with subtle tone variation per board, not flat
      c.fillRect(0, y0, TILE_PX, rh-1, base)
      c.hLine(0, y0, TILE_PX, 0xFFB0774E.toInt()); c.hLine(0, y0+rh-1, TILE_PX, seam)
      // distinct grain per board: 5-7 strokes + 1 knot / figuring
      val grainCount = 5 + (row+variant)%3
      repeat(grainCount) {
        val gx=4+rng.nextInt(52); val gy=y0+2+rng.nextInt((rh-3).coerceAtLeast(1))
        val grainCol = when (rng.nextInt(3)) { 0->0xFF6E4328.toInt(); 1->0xFFA66D46.toInt(); else->0xFF5C3821.toInt() }
        val len = 4 + rng.nextInt(5) + (row%2)*2; c.hLine(gx, gy, len, grainCol)
        if (rng.nextInt(8)==0) { c.set(gx+len/2, gy, 0xFF3B2214.toInt()); c.set(gx+len/2+1, gy, 0xFF4A2C1A.toInt()) } // figuring
      }
      if (rng.nextInt(6)==0) { val kx=12+rng.nextInt(40); c.set(kx, y0+3, 0xFF3B2214.toInt()); c.set(kx+1, y0+3, 0xFF4A2C1A.toInt()); c.set(kx, y0+4, 0xFF2A1A0E.toInt()) }
      if ((variant+row)%4==0) c.hLine(6+rng.nextInt(30), y0+3, 10, 0xFFC48B60.toInt()) // wear scuff
      val jointX = ((variant*13 + row*17 + (row%2)*26) % 52)+6
      c.vLine(jointX, y0, rh, seam); c.set(jointX-2, y0+2, 0xFF4A2C1A.toInt()); c.set(jointX+2, y0+2, 0xFF4A2C1A.toInt())
      c.set(jointX, y0+rh-3, 0xFF3B2112.toInt()); c.set(jointX+1, y0+rh-3, 0xFF4D2A18.toInt()) // nail pair
      y0+=rh
    }
    c.hLine(0, TILE_PX-2, TILE_PX, 0xFF2B1A0F.toInt()); // AO
    return c.toImageBitmap()
  }

  /** V3 PREMIUM FLAGSTONE — interior tavern stone, warm sand palette, readable slabs, restrained moss. */
  private fun createTavernFlagstoneTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(2950L + variant * 1071L)
    val mortar = 0xFF4A4740.toInt()
    val palette = intArrayOf(0xFF8C877B.toInt(), 0xFF7E7A6E.toInt(), 0xFF999487.toInt(), 0xFF8A8578.toInt())
    val hi = 0xFFADA89B.toInt(); val sh = 0xFF636056.toInt()
    val rows = intArrayOf(16,16,16,16); var py=0
    for (r in rows.indices) {
      var px=-((variant*9 + r*13)%15); var idx=0
      while (px < TILE_PX) {
        val cw = 16 + ((variant+r*2+idx)%3)*5 + (if (variant==2)2 else 0)
        val base = palette[(r*2+idx+variant)%palette.size]
        c.beveledSlab(px, py, cw, rows[r], base, hi, sh, mortar, rng, hi, sh)
        if ((variant+r+idx)%7==0) c.set(px+3, py+4, 0xFF4A4740.toInt())
        if ((variant+r+idx)%6==2) c.set(px+cw-3, py+rows[r]-3, 0xFF3D5A36.toInt())
        if (rng.nextInt(14)==0) c.hLine(px+4, py+rows[r]/2, 4, 0xFF9A9588.toInt())
        px+=cw; idx++
      }; py+=rows[r]
    }
    return c.toImageBitmap()
  }

  private fun createMeadowGrassTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(3300 + variant * 191)
    val seamlessBase = 0xFF285C37.toInt()
    c.fillRect(0, 0, TILE_PX, TILE_PX, seamlessBase)

    // Organic soft moss & clover patches inside each 64x64 tile
    repeat(6) {
      val cx = 8 + rng.nextInt(48)
      val cy = 8 + rng.nextInt(48)
      val patchCol = if (it % 2 == 0) 0xFF214F33.toInt() else 0xFF2F693B.toInt()
      c.fillEllipse(cx, cy, 10 + rng.nextInt(8), 6 + rng.nextInt(4), patchCol)
    }

    val deepShadow = 0xFF193E36.toInt()
    val midEmerald = 0xFF39783E.toInt()
    val brightLeaf = 0xFF63A13E.toInt()
    val sunlitChartreuse = 0xFF98CC47.toInt()

    repeat(120) {
      val gx = rng.nextInt(60) + 2
      val gy = rng.nextInt(60) + 2
      val col = when (rng.nextInt(5)) {
        0 -> deepShadow
        1, 2 -> midEmerald
        3 -> brightLeaf
        else -> sunlitChartreuse
      }
      c.set(gx, gy, col)
      if (gx < 60 && rng.nextBoolean()) c.set(gx + 1, gy, midEmerald)
    }

    // Crisp 3/4 pixel grass blade tufts with layered heights
    repeat(14) {
      val tx = 6 + rng.nextInt(52)
      val ty = 10 + rng.nextInt(48)
      c.hLine(tx - 2, ty, 5, deepShadow)
      c.vLine(tx - 1, ty - 3, 4, brightLeaf)
      c.vLine(tx, ty - 5, 6, sunlitChartreuse)
      c.vLine(tx + 1, ty - 3, 4, brightLeaf)
    }

    // Individual Wildflower Clusters with recognizable petals (cyan, magenta, gold, white)
    if (variant == 1 || variant == 3 || variant == 5 || variant == 7) {
      repeat(3) { fIdx ->
        val fx = 10 + (variant * 11 + fIdx * 19) % 44
        val fy = 12 + (variant * 13 + fIdx * 17) % 40
        val (petal, core) = when ((variant + fIdx) % 4) {
          0 -> 0xFFBAE6FD.toInt() to 0xFF38BDF8.toInt() // Cyan Astral Flower
          1 -> 0xFFFBCFE8.toInt() to 0xFFE11D48.toInt() // Magenta Rose
          2 -> 0xFFFEF08A.toInt() to 0xFFF59E0B.toInt() // Gold Star Flower
          else -> 0xFFFFFFFF.toInt() to 0xFFFACC15.toInt() // White Lily
        }
        // 5-pixel cross petals
        c.set(fx - 2, fy, petal)
        c.set(fx + 2, fy, petal)
        c.set(fx, fy - 2, petal)
        c.set(fx, fy + 2, petal)
        c.set(fx - 1, fy - 1, petal)
        c.set(fx + 1, fy - 1, petal)
        c.set(fx - 1, fy + 1, petal)
        c.set(fx + 1, fy + 1, petal)
        c.fillCircle(fx, fy, 1, core)
      }
    }
    return c.toImageBitmap()
  }

  private fun createMeadowStonePathTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(4400 + variant * 151)
    val terracottaSoil = 0xFF7D4B2A.toInt()
    c.fillRect(0, 0, TILE_PX, TILE_PX, terracottaSoil)

    val stones = intArrayOf(
      0xFFA89A88.toInt(),
      0xFFB8A995.toInt(),
      0xFF998B7A.toInt(),
      0xFFB0A18E.toInt(),
    )
    val hi = 0xFFCCC0AC.toInt()
    val sh = 0xFF6E6254.toInt()

    val rows = intArrayOf(12, 14, 12, 14, 12)
    var py = 0
    for (row in rows.indices) {
      val rh = rows[row]
      var px = -((variant * 5 + row * 6) % 9)
      var idx = 0
      while (px < TILE_PX) {
        val cw = 9 + ((variant + row + idx) % 3) * 3
        c.beveledSlab(
          px, py, cw, rh,
          stones[(variant + row + idx) % stones.size],
          hi, sh, terracottaSoil, rng,
          0xFFBFB19D.toInt(), 0xFF827566.toInt(),
        )
        c.set(px + 1, py + 1, terracottaSoil)
        px += cw
        idx++
      }
      py += rh
    }

    if (variant % 2 == 0) {
      val lx = 5 + (variant * 7) % 20
      val ly = 4 + (variant * 5) % 22
      c.set(lx, ly, 0xFF4D8A3E.toInt())
      c.set(lx + 1, ly, 0xFF7BB844.toInt())
    }
    return c.toImageBitmap()
  }

  private fun createMeadowOchreBorderTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(4800 + variant * 109)
    // Seamless meadow grass base + organic ochre dirt & scattered cobble stones (Ref 3!)
    c.fillRect(0, 0, TILE_PX, TILE_PX, 0xFF285C37.toInt())

    repeat(5) {
      val ex = 4 + rng.nextInt(24)
      val ey = 4 + rng.nextInt(24)
      c.fillEllipse(ex, ey, 6 + rng.nextInt(4), 4 + rng.nextInt(2), 0xFF8A522C.toInt())
      c.fillEllipse(ex, ey, 4 + rng.nextInt(3), 2 + rng.nextInt(2), 0xFFA36235.toInt())
    }
    // Scattered rounded edge cobbles
    repeat(3) {
      val sx = 5 + rng.nextInt(22)
      val sy = 5 + rng.nextInt(22)
      c.fillEllipse(sx, sy, 4, 2, 0xFF736759.toInt())
      c.fillEllipse(sx, sy - 1, 3, 2, 0xFFA89A88.toInt())
    }
    // Grass tufts encroaching over dirt edge
    repeat(7) {
      val tx = 2 + rng.nextInt(27)
      val ty = 3 + rng.nextInt(26)
      c.set(tx, ty, 0xFF39783E.toInt())
      c.set(tx, ty - 1, 0xFF63A13E.toInt())
      if (tx + 1 < TILE_PX) c.set(tx + 1, ty, 0xFF98CC47.toInt())
    }
    return c.toImageBitmap()
  }

  private fun createElderwoodTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(5500 + variant * 113)
    c.fillRect(0, 0, TILE_PX, TILE_PX, if (variant % 2 == 0) 0xFF132B30.toInt() else 0xFF173338.toInt())
    repeat(48) {
      val x = rng.nextInt(TILE_PX)
      val y = rng.nextInt(TILE_PX)
      c.set(x, y, if (rng.nextBoolean()) 0xFF1F474D.toInt() else 0xFF0D2024.toInt())
    }
    if (variant % 2 == 1) {
      val sx = 8 + rng.nextInt(16)
      val sy = 8 + rng.nextInt(16)
      c.set(sx, sy, 0xFF38BDF8.toInt())
      c.set(sx + 1, sy, 0xFF7DD3FC.toInt())
    }
    return c.toImageBitmap()
  }

  private fun createRuinCitadelTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(6600 + variant * 127)
    val mortar = 0xFF100C17.toInt()
    val rows = intArrayOf(8, 8, 8, 8)
    var py = 0
    for (r in rows.indices) {
      var px = -((variant * 5 + r * 6) % 9)
      var idx = 0
      while (px < TILE_PX) {
        val cw = 11 + ((idx + r) % 3) * 3
        val base = if ((r + idx) % 2 == 0) 0xFF2A2238.toInt() else 0xFF231C30.toInt()
        c.beveledSlab(px, py, cw, rows[r], base, 0xFF3E3354.toInt(), 0xFF171221.toInt(), mortar, rng, 0xFF352C47.toInt(), 0xFF1D1729.toInt())
        px += cw
        idx++
      }
      py += rows[r]
    }
    if (variant == 2) {
      c.hLine(6, 16, 9, 0xFFBE123C.toInt())
    }
    return c.toImageBitmap()
  }

  private fun createSanctuaryWaterTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(9000 + variant * 211)
    // Dark Fantasy void-water: deep navy base + depth variation
    val deepWater = 0xFF0F1A2E.toInt()
    val midWater = 0xFF1A2A4A.toInt()
    val shallowEdge = 0xFF1E3A5A.toInt()
    c.fillRect(0, 0, TILE_PX, TILE_PX, deepWater)
    // Depth variation — darker pockets
    repeat(5) {
      val cx = 8 + rng.nextInt(48)
      val cy = 8 + rng.nextInt(48)
      c.fillEllipse(cx, cy, 10 + rng.nextInt(8), 6 + rng.nextInt(5), 0xFF0B1222.toInt())
    }
    // Subtle ripples — pixel wave patterns (2px horizontal streaks)
    repeat(12) {
      val rx = 4 + rng.nextInt(56)
      val ry = 6 + rng.nextInt(52)
      val len = 4 + rng.nextInt(6)
      val col = if (rng.nextInt(3) == 0) 0xFF2A4A7A.toInt() else 0xFF1E3A6A.toInt()
      c.hLine(rx, ry, len, col)
      if (rng.nextInt(3) == 0) c.hLine(rx + 2, ry + 1, len - 2, 0xFF1A2E5A.toInt())
    }
    // Reflection highlights — moonlight on water
    repeat(8) {
      val sx = 6 + rng.nextInt(52)
      val sy = 6 + rng.nextInt(52)
      c.set(sx, sy, 0xFF5A7AB8.toInt())
      if (rng.nextInt(2) == 0) c.set(sx + 1, sy, 0xFF8A9AB8.toInt())
    }
    // Edge shallow variation for chasm lip
    if (variant % 2 == 0) {
      c.fillEllipse(32, 12, 14, 6, midWater)
      c.fillEllipse(33, 11, 8, 3, shallowEdge)
    }
    // Dithering for depth transition
    if (variant == 1 || variant == 4) {
      for (x in 0 until TILE_PX step 4) {
        for (y in 0 until TILE_PX step 4) {
          if ((x + y + variant) % 8 == 0) c.set(x, y, 0xFF0D1A33.toInt())
        }
      }
    }
    return c.toImageBitmap()
  }

  /**
   * V3 PREMIUM GRAVEL — 4 distinct ancient path-shoulder variants.
   * Large: damp earth lobes; Medium: varied gravel (1-3px, 18-22 per tile, highlight facets); Small: grain + moss flecks.
   */
  private fun createSanctuaryGravelTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(9200L + variant * 1167L)
    val baseEarth = 0xFF2B2620.toInt(); c.fillRect(0, 0, TILE_PX, TILE_PX, baseEarth)
    // Large damp patches — 2-3 per tile, variant-authored
    when (variant) { 0->{ c.fillEllipse(20,18,10,6,0xFF252018.toInt()); c.fillEllipse(42,40,8,5,0xFF2A241C.toInt()) } 1->{ c.fillEllipse(32,28,12,7,0xFF252018.toInt()) } 2->{ c.fillEllipse(18,32,11,6,0xFF2A241C.toInt()); c.fillEllipse(40,20,9,5,0xFF252018.toInt()) } 3->{ c.fillEllipse(24,36,10,6,0xFF252018.toInt()); c.fillEllipse(44,16,7,4,0xFF2A241C.toInt()) } }
    val gravelCols = intArrayOf(0xFF4A4A50.toInt(), 0xFF5A5A62.toInt(), 0xFF3E3E46.toInt(), 0xFF6A6A72.toInt(), 0xFF35353E.toInt(), 0xFF4E4E56.toInt())
    val gravelCount = when (variant) { 0->20; 1->18; 2->22; else->19 }
    repeat(gravelCount) {
      val gx=6+rng.nextInt(52); val gy=6+rng.nextInt(52); val r=1+rng.nextInt(3); val col=gravelCols[rng.nextInt(gravelCols.size)]
      if (r==1) c.set(gx, gy, col) else c.fillEllipse(gx, gy, r, r, col)
      if (r>=2 && rng.nextBoolean()) c.set(gx-1, gy-1, 0xFF7A7A82.toInt()) // facet highlight
      if (r==3 && rng.nextInt(3)==0) c.set(gx+1, gy+1, 0xFF2A2A32.toInt()) // AO
    }
    // Medium sand fines between gravel — 14-18, not just dark patches
    repeat(16) { val fx=8+rng.nextInt(48); val fy=8+rng.nextInt(48); c.set(fx,fy,0xFF3A342E.toInt()); if (rng.nextInt(4)==0) c.set(fx+1,fy,0xFF4A4038.toInt()) }
    if (variant%2==0) repeat(4) { val mx=8+rng.nextInt(48); val my=8+rng.nextInt(48); c.set(mx,my,0xFF2A3D2B.toInt()); c.set(mx+1,my,0xFF1E2A1F.toInt()) }
    return c.toImageBitmap()
  }
}
