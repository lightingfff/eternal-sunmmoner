package com.example.game.pixel

import androidx.compose.ui.graphics.ImageBitmap
import com.example.game.map.BiomeTheme
import com.example.game.map.ObliqueCamera
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math. hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Terrain and interior materials on the existing 64-world-unit grid and .72 ground projection.
 * Prefer authored continuous 64px cells when preloaded; retain procedural 128px materials
 * as the optional-pack fallback. Callers select continuous cells through ArtDepthAssets.tile.
 */
object FantasyHd128Atlas {

  const val HD_TILE_PX = 128
  const val WORLD_TILE_UNITS = 64f

  // ==================== 1. SANCTUARY LOBBY 128x128 TILES ====================

  val hdSanctuaryGrassTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("sanctuary_grass") ?: (0 until 8).map { createHdSanctuaryGrassTile(it) }
  }

  val hdSanctuaryStoneTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("limestone") ?: (0 until 8).map { createHdPlazaFlagstoneTile(it) }
  }

  val hdSanctuaryCausewayTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("cobble") ?: (0 until 8).map { createHdCobbleCausewayTile(it) }
  }

  val hdSanctuarySandTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("sand") ?: (0 until 6).map { createHdTrainingSandTile(it) }
  }

  val hdSanctuaryCurbTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("rock") ?: (0 until 4).map { createHdCurbStoneTile(it) }
  }

  val hdSanctuaryGravelTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("soil") ?: (0 until 4).map { createHdGravelVergeTile(it) }
  }

  val hdPlazaCenterMedallion: ImageBitmap by lazy {
    createHdPlazaCenterMedallion()
  }

  // ==================== 2. BUILDING INTERIOR 128x128 TILES ====================

  val hdTavernOakPlankTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("oak") ?: (0 until 6).map { createHdOakPlankTile(it) }
  }

  val hdInteriorMarbleFlagstoneTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("marble") ?: (0 until 6).map { createHdInteriorMarbleTile(it) }
  }

  val hdArmoryForgeBasaltTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("forge") ?: (0 until 4).map { createHdArmoryForgeTile(it) }
  }

  val hdDormitoryTapestryTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("carpet") ?: (0 until 4).map { createHdDormitoryTapestryTile(it) }
  }

  // ==================== 3. INSIDE TOWER BIOME 128x128 TILES ====================

  val hdMeadowGrassTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("meadow_grass") ?: (0 until 6).map { createHdMeadowGrassTile(it) }
  }
  val hdMeadowPathTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("soil") ?: (0 until 4).map { createHdMeadowPathTile(it) }
  }
  val hdMeadowBorderTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("sanctuary_grass") ?: (0 until 4).map { createHdMeadowBorderTile(it) }
  }

  val hdElderwoodFloorTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("forest_moss") ?: (0 until 6).map { createHdElderwoodFloorTile(it) }
  }
  val hdElderwoodPathTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("leaf_litter") ?: (0 until 4).map { createHdElderwoodPathTile(it) }
  }

  val hdSunkenRuinsFloorTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("ruin") ?: (0 until 6).map { createHdSunkenRuinsFloorTile(it) }
  }
  val hdSunkenRuinsPathTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("limestone") ?: (0 until 4).map { createHdSunkenRuinsPathTile(it) }
  }

  val hdCrimsonCavernFloorTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("volcanic") ?: (0 until 6).map { createHdCrimsonCavernFloorTile(it) }
  }
  val hdCrimsonCavernPathTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("forge") ?: (0 until 4).map { createHdCrimsonCavernPathTile(it) }
  }

  val hdFallenStarFloorTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("star_stone") ?: (0 until 6).map { createHdFallenStarFloorTile(it) }
  }
  val hdFallenStarPathTiles: List<ImageBitmap> by lazy {
    ArtDepthAssets.family("marble") ?: (0 until 4).map { createHdFallenStarPathTile(it) }
  }

  fun biomeFloorTiles(biome: BiomeTheme): List<ImageBitmap> = when (biome) {
    BiomeTheme.FIRST_FIELD_MEADOW -> hdMeadowGrassTiles
    BiomeTheme.ELDERWOOD_FOREST -> hdElderwoodFloorTiles
    BiomeTheme.SUNKEN_RUINS -> hdSunkenRuinsFloorTiles
    BiomeTheme.CRIMSON_CAVERN -> hdCrimsonCavernFloorTiles
    BiomeTheme.FALLEN_STAR_TEMPLE -> hdFallenStarFloorTiles
    else -> hdSanctuaryGrassTiles
  }

  fun biomePathTiles(biome: BiomeTheme): List<ImageBitmap> = when (biome) {
    BiomeTheme.FIRST_FIELD_MEADOW -> hdMeadowPathTiles
    BiomeTheme.ELDERWOOD_FOREST -> hdElderwoodPathTiles
    BiomeTheme.SUNKEN_RUINS -> hdSunkenRuinsPathTiles
    BiomeTheme.CRIMSON_CAVERN -> hdCrimsonCavernPathTiles
    BiomeTheme.FALLEN_STAR_TEMPLE -> hdFallenStarPathTiles
    else -> hdSanctuaryCausewayTiles
  }

  private fun PixelCanvas.drawBoxBorder(x: Int, y: Int, w: Int, h: Int, col: Int) {
    hLine(x, y, w, col)
    hLine(x, y + h - 1, w, col)
    vLine(x, y, h, col)
    vLine(x + w - 1, y, h, col)
  }

  // ==================== GENERATORS: SANCTUARY 128x128 ====================

  private fun createHdSanctuaryGrassTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val rng = Random(11000 + variant * 409)

    val deepHumus = 0xFF1F3824.toInt()
    val mossDark = 0xFF2B4D30.toInt()
    val turfBase = 0xFF39633B.toInt()
    val turfMid = 0xFF497A46.toInt()
    val bladeSun = 0xFF629954.toInt()
    val bladeGold = 0xFF84B864.toInt()
    val dewSpecular = 0xFFB5DE87.toInt()

    c.fillRect(0, 0, s, s, turfBase)

    // 1. Seamless macro-undulation (periodic sin/cos waves so tile edges wrap 100% seamlessly)
    val phaseX = (variant % 4) * (Math.PI / 2.0)
    val phaseY = (variant / 2) * (Math.PI / 2.0)
    for (y in 0 until s) {
      val wy = (y.toDouble() / s) * Math.PI * 2.0
      for (x in 0 until s) {
        val wx = (x.toDouble() / s) * Math.PI * 2.0
        val wave = sin(wx + phaseX) * cos(wy + phaseY) +
          0.5 * cos(wx * 2.0 - wy + phaseX) +
          0.35 * sin(wx * 3.0 + wy * 2.0)
        val col = when {
          wave < -0.68 -> deepHumus
          wave < -0.22 -> mossDark
          wave < 0.28 -> turfBase
          wave < 0.72 -> turfMid
          else -> bladeSun
        }
        c.set(x, y, col)
      }
    }

    // 2. Soft organic clover & moss cushions (wrapped modulo 128 for seamless tiling)
    repeat(28) {
      val cx = rng.nextInt(s)
      val cy = rng.nextInt(s)
      val rx = 6 + rng.nextInt(10)
      val ry = 4 + rng.nextInt(7)
      val tone = if (rng.nextBoolean()) turfMid else mossDark
      for (dy in -ry..ry) {
        for (dx in -rx..rx) {
          if ((dx * dx).toFloat() / (rx * rx) + (dy * dy).toFloat() / (ry * ry) <= 1f) {
            c.set((cx + dx + s) % s, (cy + dy + s) % s, tone)
          }
        }
      }
    }

    // 3. 320 Handcrafted multi-pixel grass blade clusters with NW sun-kissed tips & root AO
    repeat(320) {
      val gx = rng.nextInt(s)
      val gy = rng.nextInt(s)
      val bladeH = 3 + rng.nextInt(4)
      val lean = rng.nextInt(3) - 1
      // Root shadow
      c.set(gx, gy, deepHumus)
      c.set((gx + 1) % s, gy, mossDark)
      for (b in 1..bladeH) {
        val px = (gx + (b * lean) / 2 + s) % s
        val py = (gy - b + s) % s
        val col = when {
          b == bladeH -> if (rng.nextInt(4) == 0) dewSpecular else bladeGold
          b >= bladeH - 1 -> bladeSun
          else -> turfMid
        }
        c.set(px, py, col)
      }
    }

    // 4. Delicate three-leaf clover & tiny wildflower accents (sparse & refined)
    if (variant % 2 == 0) {
      repeat(5) {
        val fx = 12 + rng.nextInt(s - 24)
        val fy = 12 + rng.nextInt(s - 24)
        val petal = if (it % 2 == 0) 0xFFFFF3B0.toInt() else 0xFFE0F2FE.toInt()
        val core = 0xFFF59E0B.toInt()
        c.set(fx, fy, core)
        c.set(fx - 1, fy, petal)
        c.set(fx + 1, fy, petal)
        c.set(fx, fy - 1, petal)
        c.set(fx, fy + 1, bladeSun)
      }
    }

    return c.toImageBitmap()
  }

  private fun createHdPlazaFlagstoneTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val rng = Random(12000 + variant * 313)

    val mortarDeep = 0xFF4A443C.toInt()
    val mortarWarm = 0xFF635B50.toInt()
    val slabShadow = 0xFFABA190.toInt()
    val slabBase = 0xFFCEC4B2.toInt()
    val slabAlt = 0xFFD8CFC0.toInt()
    val slabWarm = 0xFFE3DACB.toInt()
    val bevelSun = 0xFFF5EFE4.toInt()
    val specularEdge = 0xFFFFFBF2.toInt()
    val goldFiligree = 0xFFD4A349.toInt()
    val goldHi = 0xFFF7D57E.toInt()

    c.fillRect(0, 0, s, s, mortarDeep)

    // 4 courses of grand royal ashlar flagstones (32px course height in 128x128)
    val rowHeights = intArrayOf(32, 32, 32, 32)
    var curY = 0
    for (row in 0 until 4) {
      val rh = rowHeights[row]
      val stagger = ((row % 2) * 32 + (variant % 3) * 16) % 64
      val colWidths = if (row % 2 == 0) intArrayOf(48, 44, 36) else intArrayOf(40, 52, 36)
      var curX = -stagger
      var colIdx = 0
      while (curX < s) {
        val cw = colWidths[colIdx % colWidths.size]
        val x0 = (curX + 2).coerceAtLeast(0)
        val y0 = (curY + 2).coerceAtLeast(0)
        val x1 = (curX + cw - 2).coerceAtMost(s - 1)
        val y1 = (curY + rh - 2).coerceAtMost(s - 1)

        if (x1 > x0 + 2 && y1 > y0 + 2) {
          val slabTone = when ((row + colIdx + variant) % 3) {
            0 -> slabBase
            1 -> slabAlt
            else -> slabWarm
          }
          // Stone face with subtle diagonal solar warmth from NW
          for (py in y0..y1) {
            for (px in x0..x1) {
              val nwBias = ((px - x0) + (py - y0)).toFloat() / ((x1 - x0) + (y1 - y0)).coerceAtLeast(1)
              val grain = ((px * 17 + py * 31 + row * 13) and 7)
              val pCol = when {
                nwBias < 0.22f && grain < 3 -> bevelSun
                nwBias > 0.82f && grain < 3 -> slabShadow
                grain == 0 -> slabAlt
                else -> slabTone
              }
              c.set(px, py, pCol)
            }
          }

          // Crisp 2px 3D NW sunlit bevel & SE shadow lip
          c.hLine(x0, y0, x1 - x0 + 1, specularEdge)
          c.hLine(x0, y0 + 1, x1 - x0 + 1, bevelSun)
          c.vLine(x0, y0, y1 - y0 + 1, bevelSun)
          c.hLine(x0, y1, x1 - x0 + 1, slabShadow)
          c.vLine(x1, y0, y1 - y0 + 1, slabShadow)

          // Warm mortar shoulder
          if (y0 > 0) c.hLine(x0, y0 - 1, x1 - x0 + 1, mortarWarm)
          if (x0 > 0) c.vLine(x0 - 1, y0, y1 - y0 + 1, mortarWarm)

          // Subtle chiselled corner chamfers & occasional gold rune inlay
          c.set(x0, y0, mortarWarm)
          c.set(x1, y0, mortarWarm)
          c.set(x0, y1, mortarDeep)
          c.set(x1, y1, mortarDeep)

          if ((row + colIdx + variant) % 5 == 0 && (x1 - x0) >= 24) {
            val ry = y0 + (y1 - y0) / 2
            c.hLine(x0 + 6, ry, (x1 - x0) - 12, goldFiligree)
            c.set(x0 + (x1 - x0) / 2, ry, goldHi)
            c.set(x0 + (x1 - x0) / 2, ry - 1, goldFiligree)
            c.set(x0 + (x1 - x0) / 2, ry + 1, goldFiligree)
          }

          // Micro hairline weathering crack on select stones
          if (rng.nextInt(4) == 0 && (x1 - x0) > 16) {
            var cx = x0 + 6 + rng.nextInt((x1 - x0 - 10).coerceAtLeast(1))
            var cy = y0 + 4
            repeat(6) {
              if (cx in x0..x1 && cy in y0..y1) {
                c.set(cx, cy, slabShadow)
                c.set((cx + 1).coerceAtMost(x1), cy, bevelSun)
              }
              cx += rng.nextInt(3) - 1
              cy += 1
            }
          }
        }
        curX += cw
        colIdx++
      }
      // Sparse emerald moss tufts in mortar intersections
      if ((row + variant) % 2 == 0) {
        val mx = 18 + rng.nextInt(s - 36)
        val my = (curY + rh - 1).coerceIn(1, s - 2)
        c.hLine(mx, my, 5, 0xFF3E6B42.toInt())
        c.hLine(mx + 1, my - 1, 3, 0xFF5A8F58.toInt())
      }
      curY += rh
    }

    return c.toImageBitmap()
  }

  private fun createHdCobbleCausewayTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val rng = Random(13000 + variant * 521)

    val jointDark = 0xFF2E2822.toInt()
    val jointSoil = 0xFF453C33.toInt()
    val cobbleShadow = 0xFF6E655A.toInt()
    val cobbleMid = 0xFF8F8476.toInt()
    val cobbleWarm = 0xFFA69988.toInt()
    val cobbleSun = 0xFFC4B7A4.toInt()
    val cobbleSpec = 0xFFE3D8C6.toInt()

    c.fillRect(0, 0, s, s, jointDark)
    for (y in 0 until s step 2) {
      for (x in 0 until s step 2) {
        if (((x + y + variant) and 3) == 0) c.set(x, y, jointSoil)
      }
    }

    // 8 staggered rows of hand-laid rounded 2.5D foreshortened cobblestones
    val rows = 8
    val rowH = s / rows // 16px per row
    for (row in 0 until rows) {
      val cy = row * rowH + rowH / 2
      val rowOffset = ((row % 2) * 11 + variant * 5) % 22
      var cx = rowOffset - 10
      var idx = 0
      while (cx < s + 10) {
        val rx = 8 + ((row * 3 + idx * 5 + variant) % 4)
        val ry = 5 + ((row + idx) % 2)
        val jitterY = rng.nextInt(3) - 1
        val stoneCy = cy + jitterY
        val baseCol = when ((row + idx + variant) % 3) {
          0 -> cobbleMid
          1 -> cobbleWarm
          else -> 0xFF9B8E7E.toInt()
        }

        // Paint rounded 3D stone with NW specular dome and SE shadow underside
        for (dy in -ry..ry) {
          val py = stoneCy + dy
          if (py !in 1 until (s - 1)) continue
          for (dx in -rx..rx) {
            val px = cx + dx
            if (px !in 1 until (s - 1)) continue
            val distSq = (dx * dx).toFloat() / (rx * rx) + (dy * dy).toFloat() / (ry * ry)
            if (distSq <= 1f) {
              val light = (-dx.toFloat() / rx * 0.55f) + (-dy.toFloat() / ry * 0.65f)
              val col = when {
                distSq > 0.82f && dy > 0 -> jointDark
                light > 0.52f -> cobbleSpec
                light > 0.18f -> cobbleSun
                light < -0.35f -> cobbleShadow
                else -> baseCol
              }
              c.set(px, py, col)
            }
          }
        }
        cx += rx * 2 + 3
        idx++
      }
    }

    // Subtle wagon wheel polish tracks & moss flecks in joints
    repeat(16) {
      val mx = rng.nextInt(s)
      val my = rng.nextInt(s)
      if (c.get(mx, my) == jointDark) {
        c.set(mx, my, 0xFF3E5E3C.toInt())
      }
    }

    return c.toImageBitmap()
  }

  private fun createHdTrainingSandTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val rng = Random(14000 + variant * 277)

    val sandShadow = 0xFFA38252.toInt()
    val sandBase = 0xFFC29E6B.toInt()
    val sandWarm = 0xFFD4B27D.toInt()
    val sandSun = 0xFFE6C896.toInt()
    val quartzSpec = 0xFFF7E4BE.toInt()

    c.fillRect(0, 0, s, s, sandBase)

    // Seamless wind-raked combat arena ripples
    for (y in 0 until s) {
      val wy = (y.toDouble() / s) * Math.PI * 4.0
      for (x in 0 until s) {
        val wx = (x.toDouble() / s) * Math.PI * 2.0
        val ripple = sin(wy + sin(wx + variant) * 0.8)
        val col = when {
          ripple > 0.58 -> sandSun
          ripple > 0.15 -> sandWarm
          ripple < -0.55 -> sandShadow
          else -> sandBase
        }
        c.set(x, y, col)
      }
    }

    // Fine quartz sparkles & small arena pebbles
    repeat(220) {
      val px = rng.nextInt(s)
      val py = rng.nextInt(s)
      c.set(px, py, if (rng.nextInt(5) == 0) quartzSpec else sandShadow)
    }

    return c.toImageBitmap()
  }

  private fun createHdCurbStoneTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    // Start from flagstone base and add heavy carved border coping blocks along edges
    val base = createHdPlazaFlagstoneTile(variant)
    val mortar = 0xFF3D3730.toInt()
    val curbBase = 0xFFB8AEA0.toInt()
    val curbSun = 0xFFEDE6DA.toInt()
    val curbShadow = 0xFF7D7468.toInt()

    c.fillRect(0, 0, s, s, curbBase)
    for (bx in 0 until s step 32) {
      c.fillRect(bx + 1, 2, 30, 22, curbBase)
      c.hLine(bx + 1, 2, 30, curbSun)
      c.hLine(bx + 1, 3, 30, curbSun)
      c.hLine(bx + 1, 23, 30, curbShadow)
      c.vLine(bx, 0, 26, mortar)

      c.fillRect(bx + 1, s - 24, 30, 22, curbBase)
      c.hLine(bx + 1, s - 24, 30, curbSun)
      c.hLine(bx + 1, s - 3, 30, curbShadow)
      c.vLine(bx, s - 26, 26, mortar)
    }
    // Center inlay with brass-gold trim line
    c.fillRect(0, 26, s, s - 52, 0xFFD4CBB8.toInt())
    c.hLine(0, 26, s, 0xFFD4A349.toInt())
    c.hLine(0, s - 27, s, 0xFFD4A349.toInt())
    return c.toImageBitmap()
  }

  private fun createHdGravelVergeTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val rng = Random(15000 + variant * 199)
    val earth = 0xFF473E35.toInt()
    val earthLight = 0xFF5E5347.toInt()
    val pebbleMid = 0xFF8C8275.toInt()
    val pebbleHi = 0xFFB8AEA0.toInt()
    val moss = 0xFF3B613B.toInt()

    c.fillRect(0, 0, s, s, earth)
    repeat(360) {
      val px = rng.nextInt(s - 3)
      val py = rng.nextInt(s - 3)
      val r = rng.nextInt(10)
      val col = when {
        r < 3 -> earthLight
        r < 6 -> pebbleMid
        r < 8 -> pebbleHi
        else -> moss
      }
      c.fillRect(px, py, 2 + (r and 1), 2, col)
    }
    return c.toImageBitmap()
  }

  private fun createHdPlazaCenterMedallion(): ImageBitmap {
    val w = 448
    val h = 304
    val c = PixelCanvas(w, h)
    val cx = w / 2
    val cy = h / 2
    val rxMax = w / 2 - 6f
    val ryMax = h / 2 - 6f

    val marbleDeep = 0xFF4E4759.toInt()
    val marbleMid = 0xFFADA496.toInt()
    val marbleLight = 0xFFDED6C8.toInt()
    val marbleIvory = 0xFFF4EFE6.toInt()
    val goldDark = 0xFF8F621C.toInt()
    val goldMid = 0xFFD49B35.toInt()
    val goldBright = 0xFFF7D060.toInt()
    val goldSpec = 0xFFFFF7CC.toInt()
    val astralBlue = 0xFF1E3A5F.toInt()
    val runeCyan = 0xFF38BDF8.toInt()
    val runeCore = 0xFFE0F2FE.toInt()

    for (y in 0 until h) {
      val ny = (y - cy) / ryMax
      for (x in 0 until w) {
        val nx = (x - cx) / rxMax
        val r = sqrt(nx * nx + ny * ny)
        if (r > 1f) continue

        val angle = kotlin.math.atan2(ny.toDouble(), nx.toDouble())
        val deg = ((Math.toDegrees(angle) + 360.0) % 360.0)
        val ray16 = abs(((deg + 11.25) % 22.5) - 11.25)
        val ray8 = abs(((deg + 22.5) % 45.0) - 22.5)
        val nwLight = (-nx * 0.45f - ny * 0.55f)

        val col = when {
          // Outer gold & obsidian bevel rim
          r > 0.96f -> marbleDeep
          r > 0.91f -> if (nwLight > 0f) goldSpec else goldMid
          r > 0.88f -> goldDark
          // Outer radial carved marble ring with 32 segments
          r > 0.72f -> {
            val segSeam = (deg % 11.25) < 0.9
            val runeTick = r in 0.78f..0.83f && (deg % 22.5) in 4.0..7.5
            when {
              segSeam -> marbleDeep
              runeTick -> runeCyan
              nwLight > 0.15f -> marbleIvory
              else -> marbleLight
            }
          }
          // Middle celestial ring with glowing cyan constellation inlay
          r > 0.67f -> if (nwLight > 0f) goldBright else goldMid
          r > 0.50f -> {
            val onConstellation = abs(r - 0.585f) < 0.014f || (ray8 < 1.6 && r in 0.52f..0.65f)
            when {
              onConstellation -> if (ray8 < 0.7) runeCore else runeCyan
              else -> astralBlue
            }
          }
          // Inner golden astrolabe ring
          r > 0.45f -> goldBright
          // Starburst compass rose over alabaster pavement
          r > 0.18f -> {
            val starWidth = (1f - (r - 0.18f) / 0.27f) * 6.5
            val minorWidth = (1f - (r - 0.18f) / 0.27f) * 3.2
            when {
              ray8 < starWidth * 0.35 -> goldSpec
              ray8 < starWidth -> goldMid
              ray16 < minorWidth -> goldDark
              else -> if (((x + y) and 1) == 0) marbleLight else marbleMid
            }
          }
          // Central sovereign arcane core
          r > 0.13f -> goldSpec
          r > 0.06f -> runeCyan
          else -> runeCore
        }
        c.set(x, y, col)
      }
    }
    return c.toImageBitmap()
  }

  // ==================== GENERATORS: BUILDING INTERIORS 128x128 ====================

  private fun createHdOakPlankTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val rng = Random(21000 + variant * 173)

    val seamDark = 0xFF21120A.toInt()
    val oakShadow = 0xFF52301B.toInt()
    val oakBase = 0xFF6E4326.toInt()
    val oakWarm = 0xFF825130.toInt()
    val oakLight = 0xFF99623C.toInt()
    val varnishSpec = 0xFFB57A50.toInt()
    val brassNail = 0xFFD9A34A.toInt()

    c.fillRect(0, 0, s, s, seamDark)
    val plankCount = 4
    val plankH = s / plankCount // 32px per horizontal oak plank
    for (row in 0 until plankCount) {
      val y0 = row * plankH + 1
      val y1 = (row + 1) * plankH - 2
      val tone = when ((row + variant) % 3) {
        0 -> oakBase
        1 -> oakWarm
        else -> 0xFF784A2B.toInt()
      }

      // Draw wood grain cathedral figuring along plank
      val knotX = 24 + ((row * 37 + variant * 23) % (s - 48))
      val knotY = (y0 + y1) / 2
      for (py in y0..y1) {
        for (px in 0 until s) {
          val distKnot = hypot((px - knotX) * 0.45, (py - knotY) * 1.4)
          val wave = sin(px * 0.09 + py * 0.35 + row * 1.7 + sin(distKnot * 0.4))
          val col = when {
            py == y0 -> varnishSpec
            py == y0 + 1 -> oakLight
            py == y1 -> oakShadow
            distKnot < 4.5 -> oakShadow
            wave > 0.65 -> oakLight
            wave < -0.58 -> oakShadow
            else -> tone
          }
          c.set(px, py, col)
        }
      }

      // Staggered butt-joint between planks + brass dowels
      val jointX = ((row * 53 + variant * 29) % (s - 32)) + 16
      c.vLine(jointX, y0, plankH - 2, seamDark)
      c.vLine((jointX + 1) % s, y0, plankH - 2, varnishSpec)
      for (dy in listOf(y0 + 5, y1 - 5)) {
        c.fillRect((jointX - 4 + s) % s, dy, 2, 2, brassNail)
        c.fillRect((jointX + 3) % s, dy, 2, 2, brassNail)
      }
    }
    return c.toImageBitmap()
  }

  private fun createHdInteriorMarbleTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val seam = 0xFF282436.toInt()
    val slateDark = 0xFF464059.toInt()
    val slateBase = 0xFF595270.toInt()
    val slateLight = 0xFF70688C.toInt()
    val slateSpec = 0xFF9188B0.toInt()
    val goldInlay = 0xFFD4A349.toInt()

    c.fillRect(0, 0, s, s, seam)
    // 2x2 large polished castle interior flagstones (64x64 each inside 128x128 tile)
    for (gy in 0..1) {
      for (gx in 0..1) {
        val x0 = gx * 64 + 2
        val y0 = gy * 64 + 2
        val x1 = (gx + 1) * 64 - 2
        val y1 = (gy + 1) * 64 - 2
        val isAlt = ((gx + gy + variant) and 1) == 0
        val base = if (isAlt) slateBase else 0xFF524B66.toInt()
        for (py in y0..y1) {
          for (px in x0..x1) {
            val vein = sin((px + py) * 0.11 + gx * 2.1 + variant) * cos((px - py) * 0.07)
            val col = when {
              vein > 0.72 -> slateLight
              vein < -0.68 -> slateDark
              else -> base
            }
            c.set(px, py, col)
          }
        }
        c.hLine(x0, y0, x1 - x0 + 1, slateSpec)
        c.vLine(x0, y0, y1 - y0 + 1, slateLight)
        c.hLine(x0, y1, x1 - x0 + 1, slateDark)
        c.vLine(x1, y0, y1 - y0 + 1, slateDark)
        // Corner gold diamond inlay
        if ((gx + gy) == 1) {
          c.fillRect(62, 62, 4, 4, goldInlay)
        }
      }
    }
    return c.toImageBitmap()
  }

  private fun createHdArmoryForgeTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val emberGlow = if (variant % 2 == 0) 0xFFD97706.toInt() else 0xFFB45309.toInt()
    val basaltDark = 0xFF232029.toInt()
    val basaltMid = 0xFF35313D.toInt()
    val basaltHi = 0xFF4F4A5C.toInt()
    c.fillRect(0, 0, s, s, basaltDark)
    for (gy in 0 until 4) {
      for (gx in 0 until 4) {
        val x0 = gx * 32 + 2
        val y0 = gy * 32 + 2
        c.fillRect(x0, y0, 28, 28, basaltMid)
        c.hLine(x0, y0, 28, basaltHi)
        c.vLine(x0, y0, 28, basaltHi)
        if ((gx + gy + variant) % 5 == 0) {
          c.hLine(x0, y0 + 29, 28, emberGlow)
        }
      }
    }
    return c.toImageBitmap()
  }

  private fun createHdDormitoryTapestryTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val crimsonDeep = 0xFF4C1122.toInt()
    val crimsonBase = 0xFF6E1A30.toInt()
    val crimsonBright = 0xFF8F243E.toInt()
    val goldThread = 0xFFD9A34A.toInt()

    c.fillRect(0, 0, s, s, crimsonBase)
    for (y in 0 until s) {
      for (x in 0 until s) {
        val weave = ((x + y) and 3) == 0
        val diamond = abs((x % 32) - 16) + abs((y % 32) - 16)
        val col = when {
          diamond == 12 -> goldThread
          diamond < 6 -> crimsonBright
          weave -> crimsonDeep
          else -> crimsonBase
        }
        c.set(x, y, col)
      }
    }
    return c.toImageBitmap()
  }

  // ==================== GENERATORS: INSIDE TOWER BIOMES 128x128 ====================

  private fun createHdMeadowGrassTile(variant: Int): ImageBitmap {
    // Rich sunlit highland meadow grass with wild clover & dandelion flecks
    return createHdSanctuaryGrassTile(variant + 2)
  }

  private fun createHdMeadowPathTile(variant: Int): ImageBitmap {
    return createHdCobbleCausewayTile(variant + 1)
  }

  private fun createHdMeadowBorderTile(variant: Int): ImageBitmap {
    return createHdGravelVergeTile(variant)
  }

  /**
   * Biome 2: ELDERWOOD_FOREST — Bioluminescent twilight moss, ancient roots & glowing cyan spores.
   */
  private fun createHdElderwoodFloorTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val rng = Random(31000 + variant * 337)

    val abyssMoss = 0xFF0E2124.toInt()
    val deepTeal = 0xFF153236.toInt()
    val mossMid = 0xFF1E474A.toInt()
    val mossLit = 0xFF2B6361.toInt()
    val sporeCyan = 0xFF38BDF8.toInt()
    val sporeBright = 0xFFA5F3FC.toInt()
    val rootBark = 0xFF3B261D.toInt()
    val rootHi = 0xFF593A2B.toInt()

    for (y in 0 until s) {
      val wy = (y.toDouble() / s) * Math.PI * 2.0
      for (x in 0 until s) {
        val wx = (x.toDouble() / s) * Math.PI * 2.0
        val w = sin(wx + variant) * cos(wy) + 0.4 * sin(wx * 2.0 + wy * 2.0)
        val col = when {
          w < -0.45 -> abyssMoss
          w < 0.20 -> deepTeal
          w < 0.65 -> mossMid
          else -> mossLit
        }
        c.set(x, y, col)
      }
    }

    // Winding gnarled roots across forest floor
    if (variant % 2 == 0) {
      var rx = rng.nextInt(s / 2)
      for (ry in 0 until s) {
        rx = (rx + rng.nextInt(3) - 1 + s) % s
        c.fillRect(rx, ry, 5, 2, rootBark)
        c.set(rx, ry, rootHi)
      }
    }

    // Bioluminescent moss blades & glowing cyan/violet forest spores
    repeat(240) {
      val gx = rng.nextInt(s)
      val gy = rng.nextInt(s)
      c.vLine(gx, (gy - 3).coerceAtLeast(0), 3, mossLit)
      if (rng.nextInt(14) == 0) {
        c.set(gx, gy, sporeCyan)
        c.set((gx + 1) % s, gy, sporeBright)
      }
    }
    return c.toImageBitmap()
  }

  private fun createHdElderwoodPathTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val rng = Random(32000 + variant * 211)
    c.fillRect(0, 0, s, s, 0xFF152C30.toInt())
    // Moss-draped ancient stepping stones with glowing cyan rune veins
    for (row in 0 until 4) {
      val cy = row * 32 + 16
      for (col in 0 until 4) {
        val cx = col * 32 + 16 + ((row % 2) * 8) - 4
        val rx = 11 + rng.nextInt(3)
        val ry = 8 + rng.nextInt(2)
        c.fillEllipse(cx, cy + 2, rx, ry, 0xFF1D2430.toInt())
        c.fillEllipse(cx, cy, rx, ry, 0xFF3D4B5E.toInt())
        c.fillEllipse(cx - 2, cy - 2, rx - 3, ry - 3, 0xFF586B82.toInt())
        if ((row + col + variant) % 5 == 0) {
          c.hLine(cx - 4, cy, 8, 0xFF38BDF8.toInt())
        }
      }
    }
    return c.toImageBitmap()
  }

  /**
   * Biome 3: SUNKEN_RUINS — Flooded Atlantis teal-marble flagstones & golden caustic ripples.
   */
  private fun createHdSunkenRuinsFloorTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val deepAbyss = 0xFF0F2430.toInt()
    val tealStone = 0xFF1C3F4F.toInt()
    val tealLit = 0xFF29586B.toInt()
    val causticCyan = 0xFF4CC9F0.toInt()
    val goldMosaic = 0xFFD4A349.toInt()

    c.fillRect(0, 0, s, s, deepAbyss)
    // Carved sunken marble slabs (32x32 grid) + shimmering water caustic network
    for (gy in 0 until 4) {
      for (gx in 0 until 4) {
        val x0 = gx * 32 + 2
        val y0 = gy * 32 + 2
        val base = if (((gx + gy + variant) and 1) == 0) tealStone else 0xFF214859.toInt()
        c.fillRect(x0, y0, 28, 28, base)
        c.hLine(x0, y0, 28, tealLit)
        c.vLine(x0, y0, 28, tealLit)
        if ((gx + gy) % 2 == 0) {
          c.drawBoxBorder(x0 + 3, y0 + 3, 22, 22, goldMosaic)
        }
      }
    }
    // Seamless water caustic wave lines
    for (y in 0 until s) {
      val wy = (y.toDouble() / s) * Math.PI * 2.0
      for (x in 0 until s) {
        val wx = (x.toDouble() / s) * Math.PI * 2.0
        val caustic = abs(sin(wx * 2.0 + sin(wy * 2.0 + variant)) + cos(wy * 2.0 - wx))
        if (caustic in 1.82..1.95) {
          c.set(x, y, causticCyan)
        }
      }
    }
    return c.toImageBitmap()
  }

  private fun createHdSunkenRuinsPathTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    c.fillRect(0, 0, s, s, 0xFF17313D.toInt())
    for (gy in 0 until 4) {
      for (gx in 0 until 2) {
        val x0 = gx * 64 + 3
        val y0 = gy * 32 + 2
        c.fillRect(x0, y0, 58, 28, 0xFF3A6073.toInt())
        c.hLine(x0, y0, 58, 0xFF6CA2BA.toInt())
        c.hLine(x0 + 4, y0 + 14, 50, 0xFFD9A34A.toInt())
      }
    }
    return c.toImageBitmap()
  }

  /**
   * Biome 4: CRIMSON_CAVERN — Fractured volcanic obsidian slabs with glowing magma veins.
   */
  private fun createHdCrimsonCavernFloorTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val rng = Random(34000 + variant * 401)
    val obsidianVoid = 0xFF170D12.toInt()
    val basaltDark = 0xFF291820.toInt()
    val basaltMid = 0xFF3D2430.toInt()
    val basaltRim = 0xFF593646.toInt()
    val magmaRed = 0xFFDC2626.toInt()
    val magmaOrange = 0xFFF97316.toInt()
    val magmaCore = 0xFFFDE047.toInt()

    c.fillRect(0, 0, s, s, obsidianVoid)
    for (gy in 0 until 4) {
      for (gx in 0 until 4) {
        val x0 = gx * 32 + 2
        val y0 = gy * 32 + 2
        val hasMagma = ((gx * 3 + gy * 5 + variant) % 4 == 0)
        if (hasMagma) {
          c.fillRect(x0 - 2, y0 - 2, 32, 32, magmaRed)
          c.hLine(x0, y0 - 1, 28, magmaOrange)
          c.set(x0 + 14, y0 - 1, magmaCore)
        }
        c.fillRect(x0, y0, 28, 28, if ((gx + gy) % 2 == 0) basaltMid else basaltDark)
        c.hLine(x0, y0, 28, basaltRim)
        c.vLine(x0, y0, 28, basaltRim)
      }
    }
    repeat(40) {
      c.set(rng.nextInt(s), rng.nextInt(s), magmaOrange)
    }
    return c.toImageBitmap()
  }

  private fun createHdCrimsonCavernPathTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    c.fillRect(0, 0, s, s, 0xFF2B1118.toInt())
    for (row in 0 until 4) {
      val y0 = row * 32 + 2
      c.fillRect(4, y0, s - 8, 28, 0xFF4F2D3A.toInt())
      c.hLine(4, y0, s - 8, 0xFF7A485B.toInt())
      c.hLine(8, y0 + 14, s - 16, 0xFFF59E0B.toInt())
    }
    return c.toImageBitmap()
  }

  /**
   * Biome 5: FALLEN_STAR_TEMPLE — Celestial obsidian-indigo sanctum inscribed with golden constellations.
   */
  private fun createHdFallenStarFloorTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    val rng = Random(35000 + variant * 509)
    val voidJoint = 0xFF120E24.toInt()
    val sanctumDark = 0xFF211B3D.toInt()
    val sanctumMid = 0xFF2E2654.toInt()
    val sanctumHi = 0xFF463B7A.toInt()
    val goldConstellation = 0xFFD9A34A.toInt()
    val starCyan = 0xFF38BDF8.toInt()
    val starWhite = 0xFFF8FAFC.toInt()

    c.fillRect(0, 0, s, s, voidJoint)
    for (gy in 0 until 4) {
      for (gx in 0 until 4) {
        val x0 = gx * 32 + 2
        val y0 = gy * 32 + 2
        c.fillRect(x0, y0, 28, 28, if ((gx + gy + variant) % 2 == 0) sanctumMid else sanctumDark)
        c.hLine(x0, y0, 28, sanctumHi)
        c.vLine(x0, y0, 28, sanctumHi)
        if ((gx + gy + variant) % 3 == 0) {
          c.drawBoxBorder(x0 + 4, y0 + 4, 20, 20, goldConstellation)
          c.set(x0 + 14, y0 + 14, starCyan)
          c.set(x0 + 14, y0 + 13, starWhite)
        }
      }
    }
    repeat(28) {
      val sx = rng.nextInt(s)
      val sy = rng.nextInt(s)
      c.set(sx, sy, if (it % 3 == 0) starWhite else starCyan)
    }
    return c.toImageBitmap()
  }

  private fun createHdFallenStarPathTile(variant: Int): ImageBitmap {
    val s = HD_TILE_PX
    val c = PixelCanvas(s, s)
    c.fillRect(0, 0, s, s, 0xFF1B1536.toInt())
    for (row in 0 until 4) {
      val y0 = row * 32 + 2
      c.fillRect(2, y0, s - 4, 28, 0xFF3E346E.toInt())
      c.hLine(2, y0, s - 4, 0xFF6858AD.toInt())
      c.hLine(6, y0 + 6, s - 12, 0xFFF5B841.toInt())
      c.hLine(6, y0 + 22, s - 12, 0xFF38BDF8.toInt())
    }
    return c.toImageBitmap()
  }
}
