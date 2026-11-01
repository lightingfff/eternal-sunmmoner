package com.example.game.pixel

import kotlin.math.abs
import kotlin.random.Random

/**
 * Handcrafted 2.5D Oblique 3/4 Pixel-Art Architectural & Environmental Primitives.
 * Builds volumetric roofs + visible front/side facades, stone masonry courses, half-timbered
 * jetties, dormers, scalloped market awnings, arched doorways, staircases, and organic foliage.
 */

fun PixelCanvas.strokeEllipse(cx: Int, cy: Int, rx: Int, ry: Int, argb: Int, thickness: Int = 1) {
  if (rx <= 0 || ry <= 0) return
  val outerRx2 = (rx * rx).toFloat()
  val outerRy2 = (ry * ry).toFloat()
  val inRx = (rx - thickness).coerceAtLeast(0)
  val inRy = (ry - thickness).coerceAtLeast(0)
  val innerRx2 = (inRx * inRx).toFloat().coerceAtLeast(0.1f)
  val innerRy2 = (inRy * inRy).toFloat().coerceAtLeast(0.1f)
  for (dy in -ry..ry) {
    for (dx in -rx..rx) {
      val outEq = (dx * dx) / outerRx2 + (dy * dy) / outerRy2
      val inEq = if (inRx == 0 || inRy == 0) 2f else (dx * dx) / innerRx2 + (dy * dy) / innerRy2
      if (outEq <= 1f && inEq >= 1f) {
        set(cx + dx, cy + dy, argb)
      }
    }
  }
}

/**
 * Paints a volumetric 3/4 pitched shingle roof (visible sloped roof plane + ridge tiles +
 * overhanging wooden eaves + optional 2.5D right-side depth bevel) that sits above a front facade.
 */
fun PixelCanvas.drawPitchedRoof34(
  leftX: Int,
  topY: Int,
  frontWidth: Int,
  roofHeight: Int,
  gableInset: Int = 10,
  sideDepthPx: Int = 10,
  shingleDark: Int = 0xFF471822.toInt(),
  shingleMid: Int = 0xFF782834.toInt(),
  shingleWarm: Int = 0xFF9C3740.toInt(),
  shingleHi: Int = 0xFFC24D50.toInt(),
  ridgeCap: Int = 0xFFE0A346.toInt(),
  timberTrim: Int = 0xFF361E16.toInt(),
) {
  // 1. Main Front-Sloping 3/4 Roof Plane (trapezoid widening from ridge to overhanging eaves)
  for (ry in 0 until roofHeight) {
    val y = topY + ry
    val inset = gableInset * (roofHeight - 1 - ry) / roofHeight.coerceAtLeast(1)
    val x0 = leftX + inset
    val x1 = leftX + frontWidth - inset
    val isCourseLip = (ry % 4 == 3)
    val courseIdx = ry / 4
    for (px in x0..x1) {
      val rel = (px - x0).toFloat() / (x1 - x0).coerceAtLeast(1)
      val tileSeam = ((px + (courseIdx % 2) * 3) % 5 == 0)
      val col = when {
        ry == 0 -> ridgeCap
        ry == 1 -> shingleHi
        isCourseLip -> shingleDark
        tileSeam -> shingleDark
        rel < 0.34f -> shingleHi
        rel < 0.72f -> shingleWarm
        else -> shingleMid
      }
      set(px, y, col)
    }
    // Crisp dark-oak gable bargeboards on left & right edges
    set(x0, y, timberTrim)
    set(x0 + 1, y, shingleHi)
    set(x1, y, timberTrim)
  }

  // 2. 2.5D Oblique Right-Side Angled Roof / Gable Return Plane for unmistakable 3D volume
  if (sideDepthPx > 0) {
    for (ry in 2 until roofHeight) {
      val y = topY + ry
      val inset = gableInset * (roofHeight - 1 - ry) / roofHeight.coerceAtLeast(1)
      val xStart = leftX + frontWidth - inset + 1
      val depth = (sideDepthPx * ry / roofHeight.coerceAtLeast(1)).coerceAtLeast(2)
      val isLip = (ry % 4 == 3)
      for (dx in 0 until depth) {
        val py = y - dx / 2
        set(xStart + dx, py, if (isLip || dx == depth - 1) 0xFF2B1018.toInt() else shingleDark)
      }
    }
  }

  // 3. Heavy Overhanging Eave Beam & Cast Shadow Lip at Bottom of Roof
  val eaveY = topY + roofHeight
  hLine(leftX - 2, eaveY, frontWidth + 4, timberTrim)
  hLine(leftX - 1, eaveY + 1, frontWidth + 2, 0xFF1E1016.toInt())
}

/**
 * Paints a 3/4 peaked dormer window jutting out from a pitched roof slope (Reference 1 signature).
 */
fun PixelCanvas.drawRoofDormer34(
  centerX: Int,
  topY: Int,
  halfSpan: Int = 9,
  roofDark: Int = 0xFF471822.toInt(),
  roofWarm: Int = 0xFF9C3740.toInt(),
  roofHi: Int = 0xFFC24D50.toInt(),
) {
  // Peaked mini-gable roof
  for (ry in 0..7) {
    val span = 1 + (halfSpan - 1) * ry / 7
    for (dx in -span..span) {
      val col = when {
        abs(dx) == span -> 0xFF361E16.toInt()
        dx < 0 -> roofHi
        else -> roofWarm
      }
      set(centerX + dx, topY + ry, col)
    }
  }
  // Dormer cheek wall & warm glowing attic window
  fillRect(centerX - halfSpan + 2, topY + 8, (halfSpan - 2) * 2 + 1, 9, 0xFFB8997A.toInt())
  fillRect(centerX - halfSpan + 2, topY + 8, 2, 9, 0xFF3B231A.toInt())
  fillRect(centerX + halfSpan - 3, topY + 8, 2, 9, 0xFF3B231A.toInt())
  drawGlowingWindow(centerX - 3, topY + 9, 7, 7)
  hLine(centerX - halfSpan + 1, topY + 17, (halfSpan - 1) * 2 + 1, roofDark)
}

/**
 * Paints a multi-course hand-laid 3/4 ashlar stone wall with corner quoins, mortar shadows,
 * eave under-shadow, and a stepped stone foundation plinth.
 */
fun PixelCanvas.drawStoneWall34(
  x: Int,
  y: Int,
  w: Int,
  h: Int,
  sideDepthPx: Int = 8,
  stoneBase: Int = 0xFF3A3159.toInt(),
  stoneAlt: Int = 0xFF453B69.toInt(),
  stoneHi: Int = 0xFF5D508A.toInt(),
  stoneShadow: Int = 0xFF241D3B.toInt(),
  mortar: Int = 0xFF161126.toInt(),
  seed: Int = 101,
) {
  val rng = Random(seed)
  fillRect(x, y, w, h, mortar)

  // Staggered horizontal ashlar courses (6px tall per course)
  var cy = y
  var rowIdx = 0
  while (cy < y + h - 5) {
    val ch = 6.coerceAtMost(y + h - cy)
    var cx = x - ((rowIdx % 2) * 5)
    var colIdx = 0
    while (cx < x + w) {
      val bw = 10 + ((rowIdx + colIdx) % 3) * 3
      val bx0 = cx.coerceAtLeast(x)
      val bx1 = (cx + bw - 1).coerceAtMost(x + w - 1)
      if (bx1 > bx0 + 1 && ch > 2) {
        val base = if ((rowIdx + colIdx) % 2 == 0) stoneBase else stoneAlt
        fillRect(bx0, cy + 1, bx1 - bx0, ch - 1, base)
        hLine(bx0, cy + 1, bx1 - bx0, stoneHi)
        hLine(bx0, cy + ch - 1, bx1 - bx0, stoneShadow)
        if (rng.nextBoolean()) {
          set((bx0 + bx1) / 2, cy + ch / 2, if (rng.nextBoolean()) stoneHi else stoneShadow)
        }
      }
      cx += bw
      colIdx++
    }
    cy += ch
    rowIdx++
  }

  // Heavy Corner Quoins with chamfer highlight
  for (qy in y until (y + h - 4) step 6) {
    val qw = if ((qy / 6) % 2 == 0) 6 else 4
    fillRect(x, qy, qw, 5, stoneHi)
    hLine(x, qy, qw, 0xFF8A9AB8.toInt()) // moonlight chamfer
    hLine(x, qy + 4, qw, stoneShadow)
    fillRect(x + w - qw, qy, qw, 5, stoneAlt)
    hLine(x + w - qw, qy + 4, qw, stoneShadow)
    // lichen on quoin
    if ((qy / 6) % 3 == 0) set(x + qw - 1, qy + 2, 0xFF2A3D2B.toInt())
  }

  // Cast AO + eave shadow along top of wall
  fillRect(x, y, w, 3, stoneShadow)
  hLine(x, y, w, 0xFF4A4E5E.toInt())

  // Stepped Stone Foundation Plinth with highlight coping
  fillRect(x - 2, y + h - 5, w + 4, 5, stoneShadow)
  hLine(x - 2, y + h - 5, w + 4, stoneHi)
  hLine(x - 2, y + h - 1, w + 4, 0xFF1A1C24.toInt())

  // 2.5D Oblique Right-Side Shaded Wall Return
  if (sideDepthPx > 0) {
    for (dx in 0 until sideDepthPx) {
      val yShift = dx / 2
      vLine(x + w + dx, y - yShift, h, if (dx == sideDepthPx - 1) mortar else stoneShadow)
      for (courseY in (y - yShift) until (y + h - yShift) step 6) {
        set(x + w + dx, courseY, mortar)
      }
    }
  }
}

/**
 * Paints a 2.5D Oblique 3/4 Half-Timbered Facade with optional 2.5D shaded side return wall,
 * overhanging jetty corbels, oak beams, diagonal corner braces, and plaster shading.
 */
fun PixelCanvas.drawTimberFacade34(
  x: Int,
  y: Int,
  w: Int,
  h: Int,
  sideDepthPx: Int = 8,
  plasterBase: Int = 0xFFC7A887.toInt(),
  plasterShadow: Int = 0xFF9E7F60.toInt(),
  timberDark: Int = 0xFF3B2218.toInt(),
  timberHi: Int = 0xFF613B2A.toInt(),
) {
  drawHalfTimberWall(x, y, w, h, plasterBase, plasterShadow, timberDark, timberHi)
  // Wooden corbel brackets under bottom beam
  for (cx in (x + 6) until (x + w - 6) step 16) {
    fillRect(cx, y + h - 2, 3, 3, timberHi)
  }
  // 2.5D Shaded Side Return Wall
  if (sideDepthPx > 0) {
    val sidePlaster = 0xFF785E47.toInt()
    for (dx in 0 until sideDepthPx) {
      val yShift = dx / 2
      val col = if (dx == 0 || dx == sideDepthPx - 1) timberDark else sidePlaster
      vLine(x + w + dx, y - yShift, h, col)
      set(x + w + dx, y + h / 2 - yShift, timberDark)
    }
  }
}

/**
 * Paints a deeply recessed 3/4 arched wooden doorway with stone arch voussoirs, interior shadow,
 * vertical oak boards, iron strap hinges, golden ring handle, and beveled stone doorstep.
 */
fun PixelCanvas.drawArchedDoor34(
  centerX: Int,
  bottomY: Int,
  doorW: Int = 22,
  doorH: Int = 30,
  stoneTrim: Int = 0xFF685E82.toInt(),
  stoneDark: Int = 0xFF2B2440.toInt(),
  woodBase: Int = 0xFF6E4326.toInt(),
  woodHi: Int = 0xFF8C5835.toInt(),
  woodDark: Int = 0xFF381F12.toInt(),
) {
  val x0 = centerX - doorW / 2
  val y0 = bottomY - doorH
  // Stone arch surround
  fillRect(x0 - 3, y0 + 4, doorW + 6, doorH - 4, stoneTrim)
  fillEllipse(centerX, y0 + 5, doorW / 2 + 3, 6, stoneTrim)
  // Deep recessed doorway shadow
  fillRect(x0, y0 + 4, doorW, doorH - 4, stoneDark)
  fillEllipse(centerX, y0 + 5, doorW / 2, 4, stoneDark)
  // Recessed oak double doors (inset 2px inside shadow reveal!)
  fillRect(x0 + 2, y0 + 6, doorW - 4, doorH - 7, woodBase)
  for (px in (x0 + 4) until (x0 + doorW - 3) step 4) {
    vLine(px, y0 + 7, doorH - 8, woodHi)
    vLine(px + 2, y0 + 7, doorH - 8, woodDark)
  }
  vLine(centerX, y0 + 5, doorH - 6, 0xFF1F110A.toInt())
  // Iron hinges & golden ring handles
  hLine(x0 + 2, y0 + 12, 4, 0xFF334155.toInt())
  hLine(x0 + doorW - 6, y0 + 12, 4, 0xFF334155.toInt())
  set(centerX - 2, y0 + 18, 0xFFF5B841.toInt())
  set(centerX + 2, y0 + 18, 0xFFF5B841.toInt())
  // Projecting 3/4 stone threshold step at base
  fillRect(x0 - 4, bottomY - 2, doorW + 8, 4, stoneTrim)
  hLine(x0 - 4, bottomY - 2, doorW + 8, 0xFF9489B3.toInt())
  hLine(x0 - 4, bottomY + 1, doorW + 8, stoneDark)
}

/**
 * Paints a rich mullioned 3/4 window with wooden shutters, stone sill, and warm golden interior glow.
 */
fun PixelCanvas.drawPanedWindow34(
  x: Int,
  y: Int,
  w: Int,
  h: Int,
  withShutters: Boolean = true,
  withFlowerbox: Boolean = false,
  glowTint: Int = 0xFFFBBF24.toInt(),
  coreTint: Int = 0xFFFEF08A.toInt(),
) {
  if (withShutters) {
    fillRect(x - 4, y, 3, h, 0xFF4F3122.toInt())
    vLine(x - 3, y + 1, h - 2, 0xFF6E4632.toInt())
    fillRect(x + w + 1, y, 3, h, 0xFF42281B.toInt())
  }
  fillRect(x - 1, y - 1, w + 2, h + 2, 0xFF2B1810.toInt())
  fillRect(x, y, w, h, glowTint)
  if (w >= 6 && h >= 6) {
    fillRect(x + 2, y + 2, w - 4, h - 4, coreTint)
  }
  // Lead/wood mullions
  vLine(x + w / 2, y, h, 0xFF3B2218.toInt())
  hLine(x, y + h / 2, w, 0xFF3B2218.toInt())
  // Stone sill
  fillRect(x - 2, y + h + 1, w + 4, 2, 0xFF7A708F.toInt())
  if (withFlowerbox) {
    fillRect(x - 1, y + h + 2, w + 2, 4, 0xFF5C3822.toInt())
    for (fx in x until (x + w) step 3) {
      set(fx, y + h + 1, 0xFF3A7D3B.toInt())
      set(fx + 1, y + h, if (fx % 2 == 0) 0xFFF43F5E.toInt() else 0xFFFEF08A.toInt())
    }
  }
}

/**
 * Paints a 3/4 pitched striped cloth market awning with scalloped gold-trimmed valance (Ref 4 SUPPLIES!).
 */
fun PixelCanvas.drawScallopedAwning34(
  x: Int,
  topY: Int,
  w: Int,
  slopeH: Int = 14,
  stripeA: Int = 0xFF1D4ED8.toInt(),
  stripeAHi: Int = 0xFF3B82F6.toInt(),
  stripeB: Int = 0xFFE2E8F0.toInt(),
  goldTrim: Int = 0xFFF5B841.toInt(),
) {
  for (ry in 0 until slopeH) {
    val y = topY + ry
    val expand = ry / 3
    for (px in (x - expand)..(x + w + expand)) {
      val stripeIdx = ((px - x + 100) / 6) % 2
      val col = if (stripeIdx == 0) {
        if (ry < 4) stripeAHi else stripeA
      } else {
        if (ry > slopeH - 3) 0xFF94A3B8.toInt() else stripeB
      }
      set(px, y, col)
    }
  }
  // Scalloped hanging valance tabs + gold trim
  val valY = topY + slopeH
  val expand = (slopeH - 1) / 3
  hLine(x - expand, valY, w + expand * 2 + 1, goldTrim)
  for (px in (x - expand)..(x + w + expand)) {
    val tab = ((px - x + 100) / 6) % 2
    val tabH = if ((px % 6) in 1..4) 4 else 2
    val col = if (tab == 0) 0xFF1E3A8A.toInt() else 0xFFCBD5E1.toInt()
    vLine(px, valY + 1, tabH, col)
    set(px, valY + 1 + tabH, 0xFF0F172A.toInt())
  }
}

/**
 * Paints organic, pixel-clustered multi-shade foliage tufts instead of smooth geometric circles!
 */
fun PixelCanvas.drawOrganicLeafCluster(
  cx: Int,
  cy: Int,
  rx: Int,
  ry: Int,
  deepShadow: Int,
  midShade: Int,
  brightLeaf: Int,
  crownHi: Int,
  seed: Int = 77,
) {
  val rng = Random(seed)
  // Base irregular cluster silhouette made of overlapping sub-tufts
  val offsets = listOf(
    0 to 2, -rx / 2 to 3, rx / 2 to 3,
    -rx / 3 to -ry / 3, rx / 3 to -ry / 3, 0 to -ry / 2,
  )
  offsets.forEach { (ox, oy) ->
    fillEllipse(cx + ox, cy + oy + 2, (rx * 0.62f).toInt().coerceAtLeast(3), (ry * 0.62f).toInt().coerceAtLeast(3), deepShadow)
  }
  offsets.forEach { (ox, oy) ->
    fillEllipse(cx + ox, cy + oy, (rx * 0.58f).toInt().coerceAtLeast(3), (ry * 0.56f).toInt().coerceAtLeast(3), midShade)
  }
  // Sunlit upper-left & crown leaf clusters
  fillEllipse(cx - rx / 5, cy - ry / 4, (rx * 0.55f).toInt().coerceAtLeast(3), (ry * 0.48f).toInt().coerceAtLeast(2), brightLeaf)
  fillEllipse(cx + rx / 5, cy - ry / 5, (rx * 0.42f).toInt().coerceAtLeast(2), (ry * 0.40f).toInt().coerceAtLeast(2), brightLeaf)
  fillEllipse(cx - rx / 4, cy - ry / 2 + 2, (rx * 0.36f).toInt().coerceAtLeast(2), (ry * 0.30f).toInt().coerceAtLeast(2), crownHi)

  // Handcrafted individual pixel leaf tips for crisp pixel-art texture
  repeat((rx * ry) / 14) {
    val lx = cx + rng.nextInt(rx * 2 - 2) - (rx - 1)
    val ly = cy + rng.nextInt(ry * 2 - 2) - (ry - 1)
    if ((get(lx, ly) ushr 24) > 128) {
      val c = when {
        ly < cy - ry / 3 -> crownHi
        ly < cy + 1 -> brightLeaf
        else -> deepShadow
      }
      set(lx, ly, c)
      if (lx + 1 < width && (get(lx + 1, ly) ushr 24) > 128) set(lx + 1, ly, c)
    }
  }
}
