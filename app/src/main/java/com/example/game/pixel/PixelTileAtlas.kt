package com.example.game.pixel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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

  // Reference-Accurate Sunlit Anime Sanctuary Lawn, Sand, Alabaster Stone & Grey Cobble Tiles (64x64)
  val sanctuaryGrassTiles: List<ImageBitmap> by lazy {
    (0 until 8).map { variant -> createSanctuarySunlitGrassTile(variant) }
  }

  val sanctuarySandTiles: List<ImageBitmap> by lazy {
    (0 until 6).map { variant -> createSanctuaryTrainingSandTile(variant) }
  }

  val sanctuaryStoneTiles: List<ImageBitmap> by lazy {
    (0 until 8).map { variant -> createSanctuaryStoneTile(variant) }
  }

  val sanctuaryCausewayTiles: List<ImageBitmap> by lazy {
    (0 until 6).map { variant -> createSanctuaryGreyCobbleTile(variant) }
  }

  val sanctuaryRoadCurbTiles: List<ImageBitmap> by lazy {
    (0 until 4).map { variant -> createSanctuaryCurbTile(variant) }
  }

  // Handcrafted Foreshortened 3/4 Central Astrolabe Medallion (224 x 152)
  val astrolabePlazaMedallion: ImageBitmap by lazy { createAstrolabePlazaMedallion() }

  // Restaurant Interior 3/4 Oak Planks & Irregular Stone Walkway
  val tavernWoodPlankTiles: List<ImageBitmap> by lazy {
    (0 until 6).map { variant -> createTavernWoodPlankTile(variant) }
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
   * 64x64 Seamless Sunlit Anime Sanctuary Lawn Tile matching the vibrant emerald-chartreuse grass,
   * soft clover clusters, and scattered pink/white sakura blossoms of the floating Sanctuary island.
   */
  private fun createSanctuarySunlitGrassTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(7100 + variant * 173)
    // Dark Fantasy: desaturated ancient moss-forest floor - muted greens, stone, earth
    val baseLawn = 0xFF2B352E.toInt() // cold mossy stone-green base, not neon
    c.fillRect(0, 0, TILE_PX, TILE_PX, baseLawn)

    // V2 Premium: 11 organic moss/shadow patches + secondary highlight veins (seamless-safe interior)
    val patchCols = intArrayOf(0xFF25302A.toInt(), 0xFF2F3D33.toInt(), 0xFF232E28.toInt(), 0xFF334739.toInt(), 0xFF1F2A24.toInt(), 0xFF1B2620.toInt())
    repeat(11) { idx ->
      val cx = 8 + rng.nextInt(48)
      val cy = 8 + rng.nextInt(48)
      val col = patchCols[rng.nextInt(patchCols.size)]
      c.fillEllipse(cx, cy, 7 + rng.nextInt(9), 4 + rng.nextInt(6), col)
      // inner highlight vein on larger patches
      if (idx % 3 == 0) c.fillEllipse(cx + 2, cy - 1, 3 + rng.nextInt(2), 2, 0xFF3A4E3E.toInt())
    }
    // Earth exposure - trampled bare soil with irregular edges
    val dirtPatches = intArrayOf(0xFF2E2A24.toInt(), 0xFF3A352E.toInt(), 0xFF2A2620.toInt(), 0xFF3E342A.toInt())
    if (variant % 3 == 0) {
      repeat(3) {
        val cx = 12 + rng.nextInt(40)
        val cy = 12 + rng.nextInt(40)
        val col = dirtPatches[rng.nextInt(dirtPatches.size)]
        c.fillEllipse(cx, cy, 6 + rng.nextInt(6), 4 + rng.nextInt(3), col)
        c.fillEllipse(cx + rng.nextInt(3) - 1, cy + 1, 3, 2, 0xFF252018.toInt())
      }
    } else if (variant % 5 == 1) {
      // subtle single worn spot on other variants
      val cx = 18 + rng.nextInt(28)
      val cy = 18 + rng.nextInt(28)
      c.fillEllipse(cx, cy, 5, 3, 0xFF2E2A24.toInt())
    }
    // Fine stone grit - cold slate pebbles + slate shards embedded in moss
    repeat(26) {
      val sx = 4 + rng.nextInt(56)
      val sy = 4 + rng.nextInt(56)
      val col = when (rng.nextInt(4)) {
        0 -> 0xFF3A3F45.toInt()
        1 -> 0xFF4A4D55.toInt()
        2 -> 0xFF353A42.toInt()
        else -> 0xFF5A5F6E.toInt()
      }
      c.set(sx, sy, col)
      if (rng.nextInt(6) == 0 && sx < 63) c.set(sx + 1, sy, 0xFF2A2E34.toInt())
    }

    // Dark fantasy foliage specks - 7-tone depth with clustered micro-detail (shadow-mid-highlight + frost)
    val deepShadow = 0xFF162118.toInt()
    val midMoss = 0xFF2A3D2B.toInt()
    val mutedMid = 0xFF344E35.toInt()
    val mossLight = 0xFF3D5A36.toInt()
    val mossHi = 0xFF4A6B3E.toInt()
    val frostTip = 0xFF5A7A52.toInt()
    val earthUnder = 0xFF1E2620.toInt()
    repeat(135) {
      val gx = 2 + rng.nextInt(60)
      val gy = 2 + rng.nextInt(60)
      val roll = rng.nextInt(8)
      val col = when (roll) {
        0 -> deepShadow
        1 -> earthUnder
        2 -> midMoss
        3, 4 -> mutedMid
        5 -> mossLight
        6 -> mossHi
        else -> frostTip
      }
      c.set(gx, gy, col)
      if (rng.nextInt(4) == 0 && gx < 62) c.set(gx + 1, gy, midMoss)
      if (roll == 6 && rng.nextInt(3) == 0 && gy > 2) c.set(gx, gy - 1, mossLight)
    }

    // Premium wind-bent tufts - 3-stem clumped with AO base + highlight tip + bent lean
    repeat(15) {
      val tx = 6 + rng.nextInt(52)
      val ty = 10 + rng.nextInt(48)
      val lean = if (rng.nextBoolean()) 1 else 0
      c.hLine(tx - 1, ty + 1, 5, 0xFF1A1E1B.toInt()) // AO contact shadow
      c.hLine(tx - 1, ty, 4, deepShadow)
      c.vLine(tx - 1 + lean, ty - 2, 3, mossLight)
      c.vLine(tx + lean, ty - 4, 5, mossHi)
      c.vLine(tx + 1 + lean, ty - 2, 3, mutedMid)
      if (rng.nextInt(3) == 0) c.set(tx + lean, ty - 5, frostTip) // sun catch tip
    }

    // Micro clover / fern flecks - 3-pixel clusters for close-zoom readability
    repeat(6) {
      val cx = 8 + rng.nextInt(48)
      val cy = 8 + rng.nextInt(48)
      c.set(cx, cy, 0xFF2E4A32.toInt())
      c.set(cx + 1, cy, 0xFF3D5A36.toInt())
      c.set(cx, cy + 1, 0xFF253A28.toInt())
    }

    // Rare desaturated wildflowers - dusty lavender / dried rose with petal structure
    if (variant == 2 || variant == 5 || variant == 7) {
      repeat(2) { fIdx ->
        val fx = 11 + (variant * 13 + fIdx * 17) % 42
        val fy = 11 + (variant * 11 + fIdx * 19) % 42
        if ((variant + fIdx) % 2 == 0) {
          c.set(fx - 1, fy, 0xFF8A7A97.toInt())
          c.set(fx + 1, fy, 0xFF8A7A97.toInt())
          c.set(fx, fy - 1, 0xFF9A8AAE.toInt())
          c.set(fx, fy, 0xFF6E6080.toInt())
          c.set(fx, fy + 1, 0xFF5A4E6A.toInt())
        } else {
          c.set(fx - 1, fy, 0xFF9A6570.toInt())
          c.set(fx + 1, fy, 0xFF9A6570.toInt())
          c.set(fx, fy - 1, 0xFFB08088.toInt())
          c.set(fx, fy, 0xFF7A4A52.toInt())
        }
        c.set(fx, fy + 2, 0xFF1E2A1F.toInt())
        if (rng.nextInt(2) == 0) c.set(fx + 1, fy + 2, 0xFF2A3A2E.toInt())
      }
    }
    // Fallen leaf litter + tiny mushroom caps near old trees
    if (variant == 1 || variant == 6) {
      repeat(4) {
        val lx = 14 + rng.nextInt(36)
        val ly = 14 + rng.nextInt(36)
        c.set(lx, ly, 0xFF5A4630.toInt())
        c.set(lx + 1, ly, 0xFF4A3520.toInt())
        c.set(lx, ly + 1, 0xFF3E2E1E.toInt())
      }
      // pale mushroom dot
      val mx = 18 + rng.nextInt(28)
      val my = 18 + rng.nextInt(28)
      c.set(mx, my, 0xFFC2B8A8.toInt())
      c.set(mx, my + 1, 0xFF8A7A6A.toInt())
    }
    return c.toImageBitmap()
  }

  private fun createSanctuaryTrainingSandTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(7600 + variant * 131)
    // Ancient training ground: compacted earth & worn sand, darker than sunny beach
    val sandBase = 0xFF5C4E3A.toInt() // cold trodden earth-brown
    val sandMid = 0xFF6B5C45.toInt()
    val sandHi = 0xFF7A6B55.toInt()
    c.fillRect(0, 0, TILE_PX, TILE_PX, sandBase)

    // V2: layered trampled earth with moisture variation + compacted ridges
    repeat(7) { idx ->
      val cx = 10 + rng.nextInt(44)
      val cy = 10 + rng.nextInt(44)
      val col = when (idx % 3) {
        0 -> sandMid
        1 -> 0xFF4E3E2E.toInt()
        else -> 0xFF524A3A.toInt()
      }
      c.fillEllipse(cx, cy, 8 + rng.nextInt(8), 5 + rng.nextInt(5), col)
      if (idx % 2 == 0) c.fillEllipse(cx + 3, cy + 1, 4, 2, 0xFF6B5C45.toInt())
    }
    // Stone grit + pebbles + slate shards in sand
    repeat(65) {
      val sx = 2 + rng.nextInt(60)
      val sy = 2 + rng.nextInt(60)
      val col = when (rng.nextInt(3)) {
        0 -> sandHi
        1 -> 0xFF3E3328.toInt()
        else -> 0xFF8A7A6A.toInt()
      }
      c.set(sx, sy, col)
      if (rng.nextInt(8) == 0) c.set(sx + 1, sy, 0xFF4A3D2E.toInt())
    }
    // Sparring scuff arcs, footprints & weapon drag - directional story marks
    if (variant == 1 || variant == 4) {
      val px = 18 + variant * 5
      val py = 22 + variant * 4
      c.hLine(px, py, 9, 0xFF4A3D2E.toInt())
      c.hLine(px + 1, py + 1, 7, 0xFF7A6B55.toInt())
      c.hLine(px + 2, py + 2, 5, 0xFF3A2E24.toInt())
      // second scuff perpendicular
      c.vLine(px + 5, py - 2, 4, 0xFF5A4A3A.toInt())
    }
    // Moss/grass encroachment at arena edge
    if (variant % 2 == 0) {
      c.fillEllipse(6, 8, 6, 3, 0xFF2A3D2B.toInt())
      c.fillEllipse(7, 7, 3, 2, 0xFF3D5A36.toInt())
    }
    // Tiny puddle sheen on compacted variant
    if (variant == 3) {
      c.hLine(28, 34, 6, 0xFF6B6A5A.toInt())
      c.hLine(29, 35, 4, 0xFF8A8A7A.toInt())
    }
    return c.toImageBitmap()
  }

  private fun createSanctuaryStoneTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(1000 + variant * 137)
    val mortar = 0xFF1A1E24.toInt() // deep cold mortar, not warm beige
    val bases = intArrayOf(
      0xFF3A3F4A.toInt(), // cold stone blue-grey
      0xFF444A56.toInt(),
      0xFF353A45.toInt(),
      0xFF40454F.toInt(),
    )
    val hi = 0xFF5A5F6E.toInt()
    val sh = 0xFF23262E.toInt()

    val rowHeights = intArrayOf(16, 16, 16, 16)
    var curY = 0
    for (r in rowHeights.indices) {
      val rh = rowHeights[r]
      val offset = ((variant * 11 + r * 16) % 24)
      var curX = -offset
      var idx = 0
      while (curX < TILE_PX) {
        val cw = 20 + ((variant + r * 3 + idx) % 3) * 6
        val base = bases[(variant + r + idx) % bases.size]
        c.beveledSlab(curX, curY, cw, rh, base, hi, sh, mortar, rng, hi, sh)
        // Age: vertical crack + chip + lichen
        if (rng.nextInt(6) == 0) c.vLine(curX + cw / 2, curY + 3, rh - 6, 0xFF1E2026.toInt())
        if (rng.nextInt(10) == 0) c.set(curX + 3, curY + 4, 0xFF252830.toInt()) // chip
        if ((variant + r + idx) % 7 == 0) c.set(curX + cw - 4, curY + rh - 4, 0xFF2A332E.toInt()) // lichen in corner
        // chisel tool mark
        if (rng.nextInt(9) == 0) c.hLine(curX + 4, curY + rh / 2, 4, 0xFF4A4F5C.toInt())
        curX += cw
        idx++
      }
      curY += rh
    }
    // Rune scratch with glow + secondary moss vein
    if (variant == 3 || variant == 7) {
      c.hLine(18, 30, 10, 0xFF6B7A9A.toInt())
      c.set(22, 31, 0xFF7CA8C8.toInt())
      c.set(23, 31, 0xFF4A6080.toInt())
    }
    if (variant == 1 || variant == 5) {
      c.fillEllipse(12, 14, 4, 2, 0xFF2A332E.toInt())
      c.set(13, 13, 0xFF3D5A36.toInt())
    }
    return c.toImageBitmap()
  }

  private fun createSanctuaryGreyCobbleTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(2200 + variant * 97)
    val mortar = 0xFF1B1E24.toInt() // near-black cold grout
    val cobblePalette = intArrayOf(
      0xFF3E424D.toInt(), // weathered cold grey
      0xFF4B4F5C.toInt(),
      0xFF383C46.toInt(),
      0xFF525663.toInt(),
      0xFF2F333E.toInt(),
    )
    val hi = 0xFF5E636F.toInt()
    val sh = 0xFF252830.toInt()

    val rowHeights = intArrayOf(8, 8, 8, 8, 8, 8, 8, 8)
    var py = 0
    for (row in rowHeights.indices) {
      val rh = rowHeights[row]
      var px = -((row * 7 + variant * 5) % 12)
      var colIdx = 0
      while (px < TILE_PX) {
        val cw = 10 + ((variant + row + colIdx) % 3) * 4
        val base = cobblePalette[(variant + row * 2 + colIdx) % cobblePalette.size]
        c.beveledSlab(
          px, py, cw, rh,
          base, hi, sh, mortar, rng,
          0xFF5A5F6E.toInt(), 0xFF1E2024.toInt(),
        )
        // Moss in grout + wear chip
        if ((variant + row + colIdx) % 5 == 0) {
          c.set(px + 2, py + 2, 0xFF2A332E.toInt())
          c.set(px + 3, py + 3, 0xFF1E2A1F.toInt())
        }
        if (rng.nextInt(12) == 0) c.set(px + cw / 2, py + rh / 2, 0xFF525663.toInt()) // light wear spot
        c.set(px + 1, py + 1, mortar)
        c.set(px + cw - 2, py + 1, mortar)
        px += cw
        colIdx++
      }
      py += rh
    }
    // Age cracks + puddle sheen
    if (variant == 2) c.hLine(8, 30, 16, 0xFF1E2026.toInt())
    if (variant == 5) {
      c.hLine(10, 42, 8, 0xFF2A2E3A.toInt())
      c.set(14, 43, 0xFF4A4F5C.toInt())
    }
    return c.toImageBitmap()
  }

  private fun createSanctuaryCurbTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(2600 + variant * 61)
    val mortar = 0xFF1A1E24.toInt()
    val rowHeights = intArrayOf(16, 16, 16, 16)
    var py = 0
    for (r in rowHeights.indices) {
      val rh = rowHeights[r]
      var px = -((variant * 6 + r * 9) % 14)
      var idx = 0
      while (px < TILE_PX) {
        val cw = 16 + ((idx + r) % 2) * 6
        val base = if ((r + idx + variant) % 2 == 0) 0xFF3E424D.toInt() else 0xFF383C46.toInt()
        val hi = 0xFF525663.toInt()
        val sh = 0xFF1E2026.toInt()
        c.beveledSlab(px, py, cw, rh, base, hi, sh, mortar, rng, hi, sh)
        px += cw
        idx++
      }
      py += rh
    }
    if (variant % 2 == 0) {
      c.fillCircle(12 + variant * 8, 14, 3, 0xFF2A332E.toInt())
      c.set(12 + variant * 8, 13, 0xFF3D5A36.toInt())
      c.set(13 + variant * 8, 14, 0xFF1E2A1F.toInt())
    }
    if (variant == 1 || variant == 3) {
      c.vLine(6, 8, 4, 0xFF1E2026.toInt())
    }
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

  private fun createTavernWoodPlankTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(2800 + variant * 83)
    val seam = 0xFF3B2112.toInt()
    val planks = intArrayOf(
      0xFF8C5A38.toInt(),
      0xFF9C6641.toInt(),
      0xFF7D4E30.toInt(),
      0xFF945F3C.toInt(),
    )
    // 8 foreshortened oak floorboards summing to full 64px
    val heights = intArrayOf(8, 8, 8, 8, 8, 8, 8, 8)
    var y0 = 0
    for (row in heights.indices) {
      val rh = heights[row]
      val base = planks[(variant + row) % planks.size]
      c.fillRect(0, y0, TILE_PX, rh - 1, base)
      c.hLine(0, y0, TILE_PX, 0xFFB0774E.toInt())
      c.hLine(0, y0 + rh - 1, TILE_PX, seam)

      repeat(5) {
        val gx = rng.nextInt(54)
        val gy = y0 + 2 + rng.nextInt((rh - 3).coerceAtLeast(1))
        c.hLine(gx, gy, 6 + rng.nextInt(5), if (rng.nextBoolean()) 0xFF6E4328.toInt() else 0xFFA66D46.toInt())
      }

      val jointX = ((variant * 11 + row * 17) % 52) + 6
      c.vLine(jointX, y0, rh, seam)
      c.set(jointX - 2, y0 + 2, 0xFF4A2C1A.toInt())
      c.set(jointX + 2, y0 + 2, 0xFF4A2C1A.toInt())
      y0 += rh
    }
    return c.toImageBitmap()
  }

  private fun createTavernFlagstoneTile(variant: Int): ImageBitmap {
    val c = PixelCanvas(TILE_PX, TILE_PX)
    val rng = Random(2950 + variant * 71)
    val mortar = 0xFF4A4740.toInt()
    val palette = intArrayOf(0xFF8C877B.toInt(), 0xFF7E7A6E.toInt(), 0xFF999487.toInt())
    val hi = 0xFFADA89B.toInt()
    val sh = 0xFF636056.toInt()

    val rows = intArrayOf(16, 16, 16, 16)
    var py = 0
    for (r in rows.indices) {
      var px = -((variant * 7 + r * 11) % 14)
      var idx = 0
      while (px < TILE_PX) {
        val cw = 16 + ((variant + r + idx) % 3) * 5
        c.beveledSlab(px, py, cw, rows[r], palette[(r + idx) % palette.size], hi, sh, mortar, rng, hi, sh)
        px += cw
        idx++
      }
      py += rows[r]
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
}
