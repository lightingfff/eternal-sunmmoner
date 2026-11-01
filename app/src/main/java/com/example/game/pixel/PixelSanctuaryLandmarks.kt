package com.example.game.pixel

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Reference-accurate 2.5D Oblique Anime Pixel-Art Sanctuary Landmarks & Residential Architecture.
 * Modeled closely after the floating Sanctuary fortress-island blueprint:
 * - Monumental Dark-Obsidian Split-Monolith Northern Tower Gate with glowing azure portal & stairs
 * - Southeastern Classical Stepped Stone Amphitheatre with 9 fluted columns & crescent rune stage
 * - Varied Warm-Brown & Dusty-Blue-Slate Half-Timbered Guild/Residential Houses with brick chimneys
 */
internal object PixelSanctuaryLandmarks {

  /**
   * Northern Obsidian Split-Monolith Tower Gate (260 x 224 px).
   * Features twin beveled charcoal-obsidian obelisk pillars separated by a sky cleft at the apex,
   * carved geometric channels, a radiant azure-blue vertical oval portal core, stepped basalt dais,
   * and a central 8-step stone staircase.
   */
  fun buildTowerGateMonument(): ImageBitmap {
    val w = 260
    val h = 224
    val c = PixelCanvas(w, h)
    val cx = w / 2

    val obsDark = 0xFF1A1F28.toInt()
    val obsShadow = 0xFF262D3A.toInt()
    val obsMid = 0xFF2A3042.toInt()
    val obsLight = 0xFF3A4052.toInt()
    val obsChamfer = 0xFF647287.toInt()
    val portalDeep = 0xFF0369A1.toInt()
    val portalMid = 0xFF0EA5E9.toInt()
    val portalBright = 0xFF38BDF8.toInt()
    val portalCore = 0xFFBAE6FD.toInt()

    // 1. Wide Stepped Dark-Basalt Foundation Platform
    c.fillRect(22, 160, w - 44, 18, obsLight)
    c.hLine(22, 160, w - 44, obsChamfer)
    c.fillRect(18, 174, w - 36, 20, obsMid)
    c.hLine(18, 174, w - 36, obsChamfer)
    c.fillRect(18, 186, w - 36, 10, obsShadow)
    // Recessed dark stone panels on left and right plinth wings
    c.fillRect(28, 178, 52, 12, obsDark)
    c.fillRect(w - 80, 178, 52, 12, obsDark)
    c.fillRect(30, 180, 48, 8, obsShadow)
    c.fillRect(w - 78, 180, 48, 8, obsShadow)

    // 2. Twin Towering Tapered Obsidian Monolith Pillars with Central Sky Cleft
    for (py in 10..168) {
      val t = (py - 10) / 158f
      val outerHalfW = (52 + t * 34f).roundToInt() // widens from 52 at top to 86 at base

      // Sky cleft open from y=10..56, then solid bridge arch above portal y=57..76
      val innerGapHalfW = when {
        py <= 56 -> 8
        py <= 74 -> 0
        else -> {
          // Portal alcove cutout
          val normY = (py - 118f) / 46f
          val archCurve = (1f - normY * normY * 0.45f).coerceAtLeast(0.3f)
          (29f * archCurve).roundToInt().coerceAtLeast(18)
        }
      }

      // Left Monolith Wing
      val lx0 = cx - outerHalfW
      val lx1 = cx - innerGapHalfW
      if (lx1 > lx0) {
        for (px in lx0 until lx1) {
          val rel = (px - lx0).toFloat() / (lx1 - lx0).coerceAtLeast(1)
          val col = when {
            px <= lx0 + 2 -> obsChamfer
            px >= lx1 - 3 -> obsDark
            rel < 0.38f -> obsLight
            rel < 0.78f -> obsMid
            else -> obsShadow
          }
          c.set(px, py, col)
        }
      }

      // Right Monolith Wing
      val rx0 = cx + innerGapHalfW
      val rx1 = cx + outerHalfW
      if (rx1 > rx0) {
        for (px in rx0 until rx1) {
          val rel = (px - rx0).toFloat() / (rx1 - rx0).coerceAtLeast(1)
          val col = when {
            px <= rx0 + 2 -> obsChamfer
            px >= rx1 - 3 -> obsDark
            rel < 0.28f -> obsMid
            rel < 0.72f -> obsShadow
            else -> obsDark
          }
          c.set(px, py, col)
        }
      }
    }

    // Top horizontal bevel caps on twin peaks
    c.hLine(cx - 52, 10, 44, obsChamfer)
    c.hLine(cx + 8, 10, 44, obsLight)
    c.hLine(cx - 51, 11, 42, obsLight)
    c.hLine(cx + 9, 11, 42, obsMid)

    // Stepped side buttresses flanking the monolith base (matching reference!)
    for (by in 108..168) {
      val stepOut = (by - 108) * 16 / 60
      c.fillRect(cx - 84 - stepOut, by, 16, 2, obsLight)
      c.fillRect(cx + 68 + stepOut, by, 16, 2, obsShadow)
    }

    // Carved geometric obsidian rune grooves on both monolith faces
    c.vLine(cx - 32, 24, 128, obsDark)
    c.vLine(cx - 31, 24, 128, obsChamfer)
    c.vLine(cx + 31, 24, 128, obsDark)
    c.vLine(cx + 32, 24, 128, obsLight)
    c.hLine(cx - 48, 56, 24, obsDark)
    c.hLine(cx + 24, 56, 24, obsDark)
    c.hLine(cx - 58, 96, 26, obsDark)
    c.hLine(cx + 32, 96, 26, obsDark)

    // 3. Recessed Glowing Azure-Blue Vertical Portal Core (y: 74..160)
    val pCenterY = 118
    for (py in 72..162) {
      val dy = (py - pCenterY) / 44f
      if (abs(dy) <= 1f) {
        val halfSpan = (sqrt((1f - dy * dy).coerceAtLeast(0f)) * 28f).roundToInt()
        // Beveled dark-obsidian inner portal bezel
        c.hLine(cx - halfSpan - 5, py, 5, obsDark)
        c.hLine(cx + halfSpan, py, 5, obsLight)
        for (px in (cx - halfSpan)..(cx + halfSpan)) {
          val dx = (px - cx) / 28f
          val r = sqrt(dx * dx + dy * dy)
          val col = when {
            r < 0.28f -> 0xFFFFFFFF.toInt()
            r < 0.52f -> portalCore
            r < 0.76f -> portalBright
            r < 0.92f -> portalMid
            else -> portalDeep
          }
          c.set(px, py, col)
        }
      }
    }

    // Cyan portal light reflection on threshold slab
    c.fillEllipse(cx, 163, 28, 4, portalBright)
    c.fillEllipse(cx, 163, 18, 2, portalCore)

    // 4. Central 8-Step Dark-Stone Staircase with Cheek-Wall Balustrades (y: 164..216)
    for (step in 0..7) {
      val sy = 166 + step * 6
      val stepHalfW = 32 + step
      c.fillRect(cx - stepHalfW, sy, stepHalfW * 2, 3, obsLight)
      c.hLine(cx - stepHalfW, sy, stepHalfW * 2, obsChamfer)
      c.fillRect(cx - stepHalfW, sy + 3, stepHalfW * 2, 3, obsDark)
      if (step < 4) {
        val glowW = (18 - step * 4).coerceAtLeast(4)
        c.hLine(cx - glowW, sy, glowW * 2, portalBright)
      }
      // Flanking stepped stone cheek walls
      c.fillRect(cx - stepHalfW - 8, sy - 4, 8, 10, obsMid)
      c.hLine(cx - stepHalfW - 8, sy - 4, 8, obsChamfer)
      c.fillRect(cx + stepHalfW, sy - 4, 8, 10, obsShadow)
      c.hLine(cx + stepHalfW, sy - 4, 8, obsLight)
    }

    c.applyCrispOutline(0xFF12151C.toInt())
    return c.toImageBitmap()
  }

  /**
   * Southeastern Classical Stepped Stone Amphitheatre & Ceremonial Summoning Colosseum (268 x 206 px).
   * Features concentric curved pale-limestone stepped seating tiers, wide NW connecting stairs,
   * a sunken ritual stage with glowing pastel-azure crescent rune, and 9 tall classical columns.
   */
  fun buildSummoningHall(): ImageBitmap {
    val w = 268
    val h = 206
    val c = PixelCanvas(w, h)
    val cx = 138
    val cy = 122

    val stoneBright = 0xFF3E424D.toInt()
    val stoneLight = 0xFF343844.toInt()
    val stoneMid = 0xFF2A2D38.toInt()
    val stoneShadow = 0xFF1E2028.toInt()
    val stoneDark = 0xFF1A1C24.toInt()
    val runeOuter = 0xFF60A5FA.toInt()
    val runeInner = 0xFFBAE6FD.toInt()

    // 1. Outer Curved Amphitheatre Colonnade Base & Retaining Wall
    c.fillEllipse(cx, cy + 12, 118, 72, stoneDark)
    c.fillEllipse(cx, cy + 6, 118, 72, stoneShadow)
    c.fillEllipse(cx, cy, 118, 72, stoneLight)
    c.strokeEllipse(cx, cy, 118, 72, stoneDark, thickness = 2)
    c.strokeEllipse(cx, cy, 106, 64, stoneShadow, thickness = 2)

    // 2. Six Concentric Curved Stepped Stone Seating Tiers descending to Stage
    for (tier in 0..5) {
      val rx = 102 - tier * 9
      val ry = 61 - tier * 5
      val tCy = cy + tier * 2
      c.fillEllipse(cx, tCy + 2, rx, ry, stoneDark)
      c.fillEllipse(cx, tCy, rx, ry, if (tier % 2 == 0) stoneBright else stoneLight)
      c.strokeEllipse(cx, tCy, rx, ry, stoneShadow, thickness = 1)
    }

    // 3. Sunken Ceremonial Stage Floor & Glowing Pastel-Azure Crescent Rune
    val stageCx = cx - 4
    val stageCy = cy + 14
    c.fillEllipse(stageCx, stageCy, 48, 28, stoneBright)
    c.strokeEllipse(stageCx, stageCy, 48, 28, stoneMid, thickness = 2)
    // Crescent arcane summoning inlay
    c.strokeEllipse(stageCx + 4, stageCy + 2, 32, 18, runeOuter, thickness = 4)
    c.strokeEllipse(stageCx + 4, stageCy + 2, 30, 17, runeInner, thickness = 2)
    c.fillEllipse(stageCx - 6, stageCy - 4, 24, 14, stoneBright)

    // 4. Wide NW Connecting Grand Stone Staircase (linking Central Plaza to Stage!)
    for (step in 0..9) {
      val sx = 42 + step * 5
      val sy = 58 + step * 5
      val sw = 58 - step
      c.fillRect(sx, sy, sw, 3, stoneBright)
      c.fillRect(sx, sy + 3, sw, 2, stoneDark)
      // Stair side cheek walls
      c.fillRect(sx - 6, sy - 3, 6, 7, stoneMid)
      c.fillRect(sx + sw, sy - 3, 6, 7, stoneShadow)
    }

    // 5. Nine Tall Classical Fluted Limestone Columns Standing Around the Outer Colonnade Arc!
    val columnAnglesDeg = listOf(195.0, 162.0, 134.0, 108.0, 82.0, 56.0, 30.0, 4.0, -22.0)
    columnAnglesDeg.sortedBy { sin(Math.toRadians(it)) }.forEach { deg ->
      val rad = Math.toRadians(deg)
      val colX = cx + (cos(rad) * 108.0).roundToInt()
      val colY = cy + (sin(rad) * 65.0).roundToInt()
      val colH = 54
      val topY = colY - colH

      // Square stone plinth base
      c.fillRect(colX - 8, colY - 5, 16, 6, stoneMid)
      c.hLine(colX - 8, colY - 5, 16, stoneBright)
      c.hLine(colX - 8, colY, 16, stoneDark)

      // Fluted cylindrical marble shaft
      c.fillRect(colX - 5, topY + 6, 10, colH - 11, stoneLight)
      c.fillRect(colX - 5, topY + 6, 3, colH - 11, stoneBright)
      c.fillRect(colX + 2, topY + 6, 3, colH - 11, stoneShadow)
      c.vLine(colX - 1, topY + 7, colH - 13, stoneMid)

      // Classical flared capital & abacus slab
      c.fillRect(colX - 7, topY + 3, 14, 3, stoneLight)
      c.fillRect(colX - 8, topY, 16, 3, stoneBright)
      c.hLine(colX - 8, topY + 3, 16, stoneDark)
    }

    c.applyCrispOutline(0xFF231F20.toInt())
    return c.toImageBitmap()
  }

  /**
   * True Two-Plane 2.5D Angled Half-Timbered Cottage Painter matching the NW Residential & Guild District.
   * Renders a sunlit SW long side wall + pitched shingle roof slope + warm brick chimney on the left,
   * paired with a crisp SE triangular A-frame gable facade on the right!
   */
  internal fun paintAnimeHalfTimberHouse(
    c: PixelCanvas,
    leftX: Int,
    roofTopY: Int,
    bodyW: Int,
    roofH: Int,
    upperH: Int,
    lowerH: Int,
    useBlueSlateRoof: Boolean,
    hasDormer: Boolean = true,
    chimneyLeft: Boolean = true,
    hasFlowerGardenTrim: Boolean = true,
  ) {
    val creamSunlit = 0xFF4C5860.toInt() // dark fantasy cold stone plaster
    val creamGable = 0xFF2C3742.toInt()
    val timberDark = 0xFF1B120E.toInt()
    val timberWarm = 0xFF2A1B12.toInt()
    val stoneBase = 0xFF2E3038.toInt()
    val stoneHi = 0xFF4A4E5E.toInt()

    val roofDark = if (useBlueSlateRoof) 0xFF1E2430.toInt() else 0xFF2A1B14.toInt()
    val roofMid = if (useBlueSlateRoof) 0xFF2E3440.toInt() else 0xFF3A2520.toInt()
    val roofLight = if (useBlueSlateRoof) 0xFF404A5E.toInt() else 0xFF4E3528.toInt()
    val roofHi = if (useBlueSlateRoof) 0xFF556080.toInt() else 0xFF5A3D2E.toInt()
    val ridgeCol = if (useBlueSlateRoof) 0xFF6A7088.toInt() else 0xFF6B4A32.toInt()

    val totalWallH = (upperH + lowerH) * 72 / 100
    val sideW = (bodyW * 56) / 100
    val gableW = bodyW - sideW
    val splitX = leftX + sideW
    val rightX = leftX + bodyW
    val gableCenter = splitX + gableW / 2
    val sideDrop = 16 // 2.5D isometric foreshortened slope from leftX down-right to splitX

    val leftEaveY = roofTopY + roofH - 4
    val splitEaveY = leftEaveY + sideDrop
    val rightEaveY = splitEaveY - 8
    val ridgeLeftX = leftX + gableW / 2 - 2
    val ridgeLeftY = roofTopY
    val ridgeApexX = gableCenter
    val ridgeApexY = roofTopY + sideDrop - 4

    // 1. Sunlit SW Long Side Wall (sloping down-right from leftX -> splitX)
    for (dx in 0..sideW) {
      val x = leftX + dx
      val topY = leftEaveY + (dx * sideDrop) / sideW.coerceAtLeast(1)
      val botY = topY + totalWallH
      c.vLine(x, topY, totalWallH - 5, creamSunlit)
      c.vLine(x, botY - 5, 5, stoneBase)
      c.set(x, botY - 5, stoneHi)
      // Recessed masonry courses on the lit wall, following its oblique slope.
      for (row in 5 until totalWallH - 5) {
        val course = row / 7
        if (row % 7 == 0) c.set(x, topY + row, 0xFF292F38.toInt())
        else if ((dx + course * 8) % 17 == 0) c.set(x, topY + row, 0xFF303640.toInt())
        else if (row % 7 == 1) c.set(x, topY + row, 0xFF505660.toInt())
      }
      // Horizontal waist timber beam
      val midBeamY = topY + totalWallH / 2
      c.vLine(x, midBeamY, 3, timberDark)
      c.vLine(x, topY, 3, timberDark)
    }
    // Vertical timber posts & diagonal braces + grain etch
    listOf(0, sideW / 3, (sideW * 2) / 3, sideW - 2).forEach { dx ->
      val x = leftX + dx
      val topY = leftEaveY + (dx * sideDrop) / sideW.coerceAtLeast(1)
      c.fillRect(x, topY, 3, totalWallH - 4, timberDark)
      c.vLine(x + 1, topY + 4, totalWallH - 10, timberWarm)
    }
    // Plaster grain speckles — subtle cold stone dust
    for (dx in 6 until sideW - 6 step 9) {
      val x = leftX + dx + (dx % 7)
      val y = leftEaveY + (dx * sideDrop) / sideW.coerceAtLeast(1) + totalWallH / 3
      c.set(x, y, 0xFF3A3F4A.toInt())
      c.set(x + 2, y + 3, 0xFF2A2D38.toInt())
    }

    // Two warm mullioned windows along SW side wall
    listOf(sideW / 5, (sideW * 3) / 5).forEach { wDx ->
      val wx = leftX + wDx
      val wy = leftEaveY + (wDx * sideDrop) / sideW.coerceAtLeast(1) + totalWallH / 2 - 14
      c.drawPanedWindow34(wx, wy, 14, 15, withFlowerbox = hasFlowerGardenTrim)
    }

    // 2. Shaded SE Gable End Wall (from splitX -> rightX, rising up into triangular A-frame peak!)
    for (gx in 0..gableW) {
      val x = splitX + gx
      val eaveBaseY = splitEaveY - (gx * 8) / gableW.coerceAtLeast(1)
      val botY = eaveBaseY + totalWallH
      val distFromPeak = kotlin.math.abs(x - ridgeApexX)
      val halfGable = (gableW / 2).coerceAtLeast(1)
      val gablePeakTopY = ridgeApexY + (distFromPeak * (eaveBaseY - ridgeApexY)) / halfGable

      c.vLine(x, gablePeakTopY, (botY - gablePeakTopY - 5).coerceAtLeast(2), creamGable)
      c.vLine(x, botY - 5, 5, 0xFF706B62.toInt())
      for (row in 4 until totalWallH - 5) {
        if (row % 7 == 0 || (gx + row / 7 * 8) % 17 == 0)
          c.set(x, eaveBaseY + row, 0xFF252A33.toInt())
        else if (row % 7 == 1) c.set(x, eaveBaseY + row, 0xFF414650.toInt())
      }
      // Horizontal tie-beams at eave level and mid-story
      c.vLine(x, eaveBaseY, 3, timberDark)
      c.vLine(x, eaveBaseY + totalWallH / 2, 3, timberDark)
    }
    // Gable vertical corner & king-post timbers
    c.fillRect(splitX - 1, splitEaveY - 2, 3, totalWallH, timberDark)
    c.fillRect(rightX - 2, rightEaveY - 2, 3, totalWallH, timberDark)
    c.fillRect(ridgeApexX - 1, ridgeApexY + 2, 3, (splitEaveY - ridgeApexY).coerceAtLeast(8), timberDark)

    // Attic gable window + ground floor arched oak door & side window on SE gable face
    c.drawPanedWindow34(ridgeApexX - 6, ridgeApexY + (splitEaveY - ridgeApexY) / 2, 12, 12, withFlowerbox = false)
    val gableGroundBotY = splitEaveY + totalWallH - 4
    c.drawArchedDoor34(gableCenter - 8, gableGroundBotY - 2, doorW = 20, doorH = 28)
    if (gableW >= 54) {
      c.drawPanedWindow34(gableCenter + 10, splitEaveY + totalWallH / 2 + 4, 13, 14, withFlowerbox = hasFlowerGardenTrim)
    }

    // 3. Broad Sunlit Pitched Shingle Roof Plane (parallelogram from ridgeLeft..ridgeApex down to leftEave..splitEave)
    for (dx in -4..(sideW + 2)) {
      val t = dx.toFloat() / sideW.coerceAtLeast(1)
      val topX = (ridgeLeftX + t * (ridgeApexX - ridgeLeftX)).roundToInt()
      val topY = (ridgeLeftY + t * (ridgeApexY - ridgeLeftY)).roundToInt()
      val botX = leftX + dx
      val botY = (leftEaveY + t * sideDrop).roundToInt() + 2
      val steps = (botY - topY).coerceAtLeast(4)
      for (s in 0..steps) {
        val sf = s.toFloat() / steps
        val px = (topX + sf * (botX - topX)).roundToInt()
        val py = topY + s
        val col = when {
          s <= 2 -> ridgeCol
          s >= steps - 2 -> roofDark
          (s % 6 == 0) -> roofMid
          ((px + s) % 9 == 0) -> roofHi
          else -> roofLight
        }
        c.fillRect(px, py, 2, 2, col)
      }
    }

    // Thick eave fascia: lit top, dark soffit and exposed rafter ends.
    for (dx in -3..sideW+2) {
      val y = leftEaveY + dx * sideDrop / sideW.coerceAtLeast(1) + 3
      c.vLine(leftX+dx, y, 3, 0xFF22262C.toInt())
      c.set(leftX+dx, y, ridgeCol)
      if (dx % 9 == 0) c.fillRect(leftX+dx, y+2, 2, 3, timberWarm)
    }

    // Crisp overhanging A-frame bargeboard rafters on the SE triangular gable peak!
    for (gx in -4..(gableW + 4)) {
      val x = splitX + gx
      val eaveBaseY = splitEaveY - (gx * 8) / gableW.coerceAtLeast(1)
      val distFromPeak = kotlin.math.abs(x - ridgeApexX)
      val halfGable = (gableW / 2 + 4).coerceAtLeast(1)
      val rafterY = ridgeApexY - 2 + (distFromPeak * (eaveBaseY + 3 - (ridgeApexY - 2))) / halfGable
      c.fillRect(x, rafterY, 2, 5, timberDark)
      c.set(x, rafterY, timberWarm)
      c.set(x, rafterY - 1, ridgeCol)
    }

    // 4. Brick-coursed chimney with mortar + cap highlight + smoke wisp
    val chimX = if (chimneyLeft) leftX + sideW / 3 + 6 else leftX + (sideW * 2) / 3
    val chimBaseY = roofTopY + 18
    c.fillRect(chimX, chimBaseY - 22, 13, 24, 0xFF7A3520.toInt())
    c.fillRect(chimX + 1, chimBaseY - 21, 5, 22, 0xFF9E523C.toInt())
    c.fillRect(chimX + 8, chimBaseY - 21, 4, 22, 0xFF4A2012.toInt())
    // brick mortar lines
    for (my in chimBaseY - 18..chimBaseY step 4) {
      c.hLine(chimX, my, 13, 0xFF4A2012.toInt())
      c.hLine(chimX + 6, my + 2, 7, 0xFF8A4A32.toInt())
    }
    c.fillRect(chimX - 2, chimBaseY - 25, 17, 4, 0xFF6B3224.toInt())
    c.hLine(chimX - 2, chimBaseY - 25, 17, 0xFFC48A60.toInt())
    c.fillEllipse(chimX + 6, chimBaseY - 26, 3, 2, 0xFF2A2E4A.toInt()) // smoke puff
    c.fillEllipse(chimX + 8, chimBaseY - 29, 2, 1, 0xFF4A4D55.toInt())

    // 5. Garden shrubs — desaturated dark fantasy greens + dusty blossoms + AO base
    if (hasFlowerGardenTrim) {
      listOf(14, sideW / 2, sideW - 10).forEachIndexed { idx, bDx ->
        val bx = leftX + bDx
        val by = leftEaveY + (bDx * sideDrop) / sideW.coerceAtLeast(1) + totalWallH - 4
        c.fillEllipse(bx, by + 2, 10, 3, 0xFF0F1A14.toInt()) // AO under shrub
        c.drawOrganicLeafCluster(
          bx, by, 13, 6,
          0xFF1A2E1F.toInt(), 0xFF2A3D2B.toInt(), 0xFF3D5A36.toInt(), 0xFF4A6B3E.toInt(), seed = leftX + idx * 17,
        )
        c.set(bx - 3, by - 2, 0xFF8A7A97.toInt()) // dusty lavender
        c.set(bx + 4, by - 1, 0xFF9A6570.toInt()) // dried rose
        c.set(bx, by - 3, 0xFF6B7A3E.toInt()) // highlight fleck
      }
    }
  }

  fun buildHeroRegistryHall(): ImageBitmap {
    val w = 188
    val h = 166
    val c = PixelCanvas(w, h)
    paintAnimeHalfTimberHouse(
      c = c,
      leftX = 18,
      roofTopY = 16,
      bodyW = 144,
      roofH = 38,
      upperH = 36,
      lowerH = 62,
      useBlueSlateRoof = false,
      hasDormer = true,
      chimneyLeft = true,
    )
    // Arcane Registry Signboard & Glowing Ledger Desk beside Door
    c.fillRect(80, 102, 28, 20, 0xFF2D3F57.toInt())
    c.fillRect(82, 104, 24, 16, 0xFF38BDF8.toInt())
    c.hLine(85, 109, 18, 0xFFFFFFFF.toInt())
    c.hLine(85, 114, 14, 0xFFFEF08A.toInt())
    c.applyCrispOutline(0xFF221B17.toInt())
    return c.toImageBitmap()
  }

  fun buildRestaurantExterior(): ImageBitmap {
    val w = 206
    val h = 172
    val c = PixelCanvas(w, h)
    paintAnimeHalfTimberHouse(
      c = c,
      leftX = 18,
      roofTopY = 16,
      bodyW = 160,
      roofH = 40,
      upperH = 36,
      lowerH = 64,
      useBlueSlateRoof = true,
      hasDormer = true,
      chimneyLeft = false,
    )
    // Cozy market awning & oak feast barrel at front right
    c.drawScallopedAwning34(72, 94, 52, slopeH = 12)
    c.fillRect(168, 136, 14, 18, 0xFF7A4B2A.toInt())
    c.hLine(168, 141, 14, 0xFF334155.toInt())
    c.applyCrispOutline(0xFF1E222B.toInt())
    return c.toImageBitmap()
  }

  fun buildFloorVistaArch(): ImageBitmap {
    val w = 156
    val h = 168
    val c = PixelCanvas(w, h)
    val cx = w / 2

    // Pale-limestone Sanctuary Wall Tunnel Archway matching the North Wall Arched Gate!
    c.drawStoneWall34(
      x = 14, y = 26, w = w - 28, h = 128, sideDepthPx = 6,
      stoneBase = 0xFFC8BCA6.toInt(),
      stoneAlt = 0xFFBAB09B.toInt(),
      stoneHi = 0xFFE3DAC8.toInt(),
      stoneShadow = 0xFF8C816E.toInt(),
      mortar = 0xFF6B6252.toInt(),
      seed = 460,
    )
    // Crenellated battlements along top
    for (bx in 14..(w - 28) step 18) {
      c.fillRect(bx, 14, 11, 12, 0xFFD4C9B4.toInt())
      c.hLine(bx, 14, 11, 0xFFEDE4D3.toInt())
    }
    // Deep dark-stone arched gate tunnel
    c.fillRect(cx - 28, 72, 56, 82, 0xFF1E1D24.toInt())
    c.fillEllipse(cx, 72, 28, 18, 0xFF1E1D24.toInt())
    c.strokeEllipse(cx, 72, 31, 20, 0xFF8C816E.toInt(), thickness = 3)
    c.applyCrispOutline(0xFF221E1B.toInt())
    return c.toImageBitmap()
  }

  fun buildTrainingGround(): ImageBitmap {
    val w = 192
    val h = 160
    val c = PixelCanvas(w, h)
    paintAnimeHalfTimberHouse(
      c = c,
      leftX = 18,
      roofTopY = 16,
      bodyW = 148,
      roofH = 36,
      upperH = 34,
      lowerH = 58,
      useBlueSlateRoof = false,
      hasDormer = false,
      chimneyLeft = true,
    )
    c.applyCrispOutline(0xFF221B17.toInt())
    return c.toImageBitmap()
  }

  fun buildArmory(): ImageBitmap {
    val w = 190
    val h = 166
    val c = PixelCanvas(w, h)
    paintAnimeHalfTimberHouse(
      c = c,
      leftX = 18,
      roofTopY = 16,
      bodyW = 146,
      roofH = 38,
      upperH = 36,
      lowerH = 60,
      useBlueSlateRoof = false,
      hasDormer = false,
      chimneyLeft = true,
    )
    // Smithy anvil & weapon rack outside front-left
    c.fillRect(68, 134, 20, 14, 0xFF5C3A21.toInt())
    c.fillRect(64, 129, 28, 6, 0xFF64748B.toInt())
    c.hLine(64, 129, 28, 0xFFCBD5E1.toInt())
    c.applyCrispOutline(0xFF221B17.toInt())
    return c.toImageBitmap()
  }

  fun buildSynthesisLab(): ImageBitmap {
    val w = 188
    val h = 168
    val c = PixelCanvas(w, h)
    paintAnimeHalfTimberHouse(
      c = c,
      leftX = 18,
      roofTopY = 16,
      bodyW = 144,
      roofH = 40,
      upperH = 36,
      lowerH = 60,
      useBlueSlateRoof = true,
      hasDormer = true,
      chimneyLeft = false,
    )
    // Glowing alchemical synthesis orb beside entrance
    c.fillRect(78, 128, 18, 22, 0xFF475569.toInt())
    c.fillCircle(87, 124, 8, 0xFF10B981.toInt())
    c.fillCircle(86, 122, 4, 0xFFA7F3D0.toInt())
    c.applyCrispOutline(0xFF1E222B.toInt())
    return c.toImageBitmap()
  }

  fun buildHeroQuarters(): ImageBitmap {
    val w = 200
    val h = 168
    val c = PixelCanvas(w, h)
    paintAnimeHalfTimberHouse(
      c = c,
      leftX = 18,
      roofTopY = 16,
      bodyW = 156,
      roofH = 38,
      upperH = 36,
      lowerH = 62,
      useBlueSlateRoof = false,
      hasDormer = true,
      chimneyLeft = false,
    )
    c.applyCrispOutline(0xFF221B17.toInt())
    return c.toImageBitmap()
  }
}
