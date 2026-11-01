package com.example.game.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.data.model.IslandExpansionMilestone
import com.example.game.map.ObliqueCamera
import com.example.game.map.PathSegment
import com.example.game.map.SanctuaryIslandGeometry
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Dedicated multi-layered 2.5D Anime Pixel-Art Renderer for the Floating Sanctuary Fortress-Island.
 * Renders:
 * 1. Sunlit cerulean sky, diagonal sunbeams, drifting cumulus clouds & distant floating mini-islands
 * 2. Faceted rocky brown cliff underside + lush overhanging green grass lip
 * 3. Polygonal-clipped seamless sunlit grass, organic SW golden-sand training arena, hedge courtyard & smooth curbed cobble streets
 * 4. Monumental Central Circular White Stone Plaza with concentric rings, 4 pastel elemental runes & grand SE amphitheatre staircase
 * 5. Southern Main Gate stone bridge deck, flanking angled stone stairs & dual wooden plank drawbridges
 * 6. Tall continuous pale-limestone crenellated perimeter fortress walls, North Arched Gate Tunnel & Physical Island Expansions
 */
object SanctuaryWorldRenderer {

  fun DrawScope.drawSanctuaryFloatingWorld(
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
    paths: List<PathSegment>,
    unlockedExpansions: Set<IslandExpansionMilestone> = emptySet(),
  ) {
    val halfW = size.width / 2f
    val halfH = size.height / 2f
    fun sx(wx: Float) = ObliqueCamera.worldToScreenX(wx, camX, zoom, halfW)
    fun sy(wy: Float, z: Float = 0f) = ObliqueCamera.worldToScreenY(wy, camY, zoom, halfH, z)

    // 1. Sunlit Anime Cerulean Sky Gradient + Clouds + Floating Sky Islands
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0xFF539CE0),
          Color(0xFF76B9F2),
          Color(0xFFA5D7FC),
          Color(0xFFCBE9FE),
        ),
        startY = 0f,
        endY = size.height,
      ),
    )

    drawDriftingSkyClouds(camX, camY, zoom, timeSec)
    drawBackgroundFloatingIslands(::sx, ::sy, zoom, timeSec)

    // 2. Faceted Rocky Cliff Underside & Lush Green Grass Rim Beneath the 8-Sided Island Polygon
    val verts = SanctuaryIslandGeometry.POLYGON_VERTICES
    drawFloatingIslandCliffUnderside(verts, ::sx, ::sy, zoom)
    drawGrassOverhangRim(verts, ::sx, ::sy, zoom)

    // 3. Polygonal Walkable Interior Ground Plane
    val islandGroundPath = Path().apply {
      verts.forEachIndexed { idx, (vx, vy) ->
        val px = sx(vx)
        val py = sy(vy)
        if (idx == 0) moveTo(px, py) else lineTo(px, py)
      }
      close()
    }

    clipPath(islandGroundPath) {
      val worldTileSize = 64f
      val tileScreenW = (worldTileSize * zoom).roundToInt().coerceAtLeast(2)
      val tileScreenH = (worldTileSize * ObliqueCamera.OBLIQUE_Y_SCALE * zoom).roundToInt().coerceAtLeast(2)

      val minWx = ObliqueCamera.screenToWorldX(0f, camX, zoom, halfW).coerceAtLeast(240f)
      val maxWx = ObliqueCamera.screenToWorldX(size.width, camX, zoom, halfW).coerceAtMost(2960f)
      val minWy = ObliqueCamera.screenToWorldY(0f, camY, zoom, halfH).coerceAtLeast(280f)
      val maxWy = ObliqueCamera.screenToWorldY(size.height, camY, zoom, halfH).coerceAtMost(2420f)

      val startTx = ((minWx / worldTileSize).toInt() - 1).coerceAtLeast(3)
      val endTx = ((maxWx / worldTileSize).toInt() + 1).coerceAtMost(47)
      val startTy = ((minWy / worldTileSize).toInt() - 1).coerceAtLeast(4)
      val endTy = ((maxWy / worldTileSize).toInt() + 1).coerceAtMost(38)

      val grassTiles = PixelTileAtlas.sanctuaryGrassTiles

      // 3A. Seamless Sunlit Anime Grass Lawn Base Across Entire Island
      for (ty in startTy..endTy) {
        val wy = ty * worldTileSize
        val dstY = sy(wy).roundToInt()
        for (tx in startTx..endTx) {
          val wx = tx * worldTileSize
          val dstX = sx(wx).roundToInt()
          val hash = ((tx * 73856093) xor (ty * 19349663)) and 0x7FFFFFFF
          drawImage(
            image = grassTiles[hash % grassTiles.size],
            dstOffset = IntOffset(dstX, dstY),
            dstSize = IntSize(tileScreenW + 1, tileScreenH + 1),
            filterQuality = FilterQuality.None,
          )
        }
      }

      // 3B. Organic Smooth-Edged SW Golden-Sand Training Arena (tiled with pixel sand inside organic clip!)
      drawOrganicTrainingSandArena(
        sx = ::sx,
        sy = ::sy,
        zoom = zoom,
        tileScreenW = tileScreenW,
        tileScreenH = tileScreenH,
      )

      // 3C. Landscaped Residential Courtyard Dirt Paths, Hedge Maze & Low Stone Garden Borders
      drawResidentialGardenCourtyards(::sx, ::sy, zoom)

      // 3D. Smooth Continuous Curbed Grey-Cobblestone Road Network (zero jagged stair-steps!)
      drawCurbedCobblestoneRoadNetwork(
        paths = paths,
        sx = ::sx,
        sy = ::sy,
        zoom = zoom,
        startTx = startTx,
        endTx = endTx,
        startTy = startTy,
        endTy = endTy,
        tileScreenW = tileScreenW,
        tileScreenH = tileScreenH,
      )

      // 3E. Monumental Central Circular White Stone Plaza, 4 Pastel Elemental Runes & Grand SE Staircase
      drawCentralCircularWhitePlaza(::sx, ::sy, zoom, timeSec)
    }

    // 4. Southern Main Gate Raised Stone Courtyard, Flanking Angled Stairs & Dual Wooden Drawbridges
    drawSouthernMainGateAndBridges(::sx, ::sy, zoom)

    // 5. Tall Continuous Crenellated Pale-Limestone Back & Side Fortress Walls + North Arched Wall Tunnel
    val eastBridgeOpen = IslandExpansionMilestone.EAST_CELESTIAL_GARDEN in unlockedExpansions
    drawBackAndSideFortressWalls(verts, ::sx, ::sy, zoom, eastBridgeOpen = eastBridgeOpen)

    // 6. Physical Island Expansions (Locked Blueprint Preview OR Connected Arched Bridge & Floating Terrace!)
    with(InteriorWorldRenderer) {
      drawSanctuaryIslandExpansions(
        camX = camX,
        camY = camY,
        zoom = zoom,
        timeSec = timeSec,
        unlockedExpansions = unlockedExpansions,
      )
    }

    // 7. Soft Warm Diagonal Sunlight Beams Streaming from Top-Left Sky
    drawDiagonalSunlightRays()
  }

  /**
   * Drawn after Y-sorted world entities so heroes near the southern lawn/sand arena stand
   * naturally behind the southern crenellated walls, while exterior retaining masonry hangs down onto the cliff!
   */
  fun DrawScope.drawSouthernForegroundFortressWalls(
    camX: Float,
    camY: Float,
    zoom: Float,
  ) {
    val halfW = size.width / 2f
    val halfH = size.height / 2f
    fun sx(wx: Float) = ObliqueCamera.worldToScreenX(wx, camX, zoom, halfW)
    fun sy(wy: Float, z: Float = 0f) = ObliqueCamera.worldToScreenY(wy, camY, zoom, halfH, z)

    val verts = SanctuaryIslandGeometry.POLYGON_VERTICES
    val wBot = verts[6]     // (300, 1780)
    val swCorner = verts[5] // (900, 2360)
    val seCorner = verts[4] // (2320, 2360)
    val eBot = verts[3]     // (2900, 1780)

    // SW sloped wall + exterior retaining cliff masonry
    drawSouthernExteriorWallSegment(wBot.first, wBot.second, swCorner.first, swCorner.second, ::sx, ::sy, zoom)
    // South-West horizontal curtain wall up to West Gate Tower (x=1360)
    drawSouthernExteriorWallSegment(swCorner.first, swCorner.second, 1360f, 2360f, ::sx, ::sy, zoom)
    // South-East horizontal curtain wall from East Gate Tower (x=1860) to SE corner
    drawSouthernExteriorWallSegment(1860f, 2360f, seCorner.first, seCorner.second, ::sx, ::sy, zoom)
    // SE sloped wall + exterior retaining cliff masonry
    drawSouthernExteriorWallSegment(seCorner.first, seCorner.second, eBot.first, eBot.second, ::sx, ::sy, zoom)
  }

  private fun DrawScope.drawOrganicTrainingSandArena(
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
    tileScreenW: Int,
    tileScreenH: Int,
  ) {
    val cxWorld = SanctuaryIslandGeometry.TRAINING_SAND_X
    val cyWorld = SanctuaryIslandGeometry.TRAINING_SAND_Y
    val rxWorld = SanctuaryIslandGeometry.TRAINING_SAND_RX
    val ryWorld = SanctuaryIslandGeometry.TRAINING_SAND_RY

    fun buildSandPath(scaleFactor: Float): Path = Path().apply {
      val steps = 40
      for (i in 0 until steps) {
        val ang = i * (Math.PI * 2.0 / steps)
        val wobble = 1f + 0.038f * sin(ang * 5.0).toFloat() + 0.024f * cos(ang * 3.0).toFloat()
        val wx = cxWorld + cos(ang).toFloat() * rxWorld * scaleFactor * wobble
        val wy = cyWorld + sin(ang).toFloat() * ryWorld * scaleFactor * wobble
        val px = sx(wx)
        val py = sy(wy, 0f)
        if (i == 0) moveTo(px, py) else lineTo(px, py)
      }
      close()
    }

    val outerGrassRim = buildSandPath(1.035f)
    val midSandLip = buildSandPath(1.012f)
    val innerSandCore = buildSandPath(0.992f)

    drawPath(outerGrassRim, Color(0xFF4F963E))
    drawPath(midSandLip, Color(0xFFC9A46B))

    val sandTiles = PixelTileAtlas.sanctuarySandTiles
    clipPath(innerSandCore) {
      val worldTileSize = 64f
      for (ty in 23..33) {
        val wy = ty * worldTileSize
        val dstY = sy(wy, 0f).roundToInt()
        for (tx in 6..21) {
          val wx = tx * worldTileSize
          val dstX = sx(wx).roundToInt()
          val hash = ((tx * 49297) xor (ty * 91813)) and 0x7FFFFFFF
          drawImage(
            image = sandTiles[hash % sandTiles.size],
            dstOffset = IntOffset(dstX, dstY),
            dstSize = IntSize(tileScreenW + 1, tileScreenH + 1),
            filterQuality = FilterQuality.None,
          )
        }
      }
    }
    drawPath(innerSandCore, Color(0xFFB58E54), style = Stroke(width = (2.0f * zoom).coerceAtLeast(1f)))
  }

  private fun DrawScope.drawCurbedCobblestoneRoadNetwork(
    paths: List<PathSegment>,
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
    startTx: Int,
    endTx: Int,
    startTy: Int,
    endTy: Int,
    tileScreenW: Int,
    tileScreenH: Int,
  ) {
    fun Path.addClockwiseCap(cx: Float, cy: Float, rx: Float, ry: Float) {
      val segs = 16
      for (i in 0 until segs) {
        val a = i * (Math.PI * 2.0 / segs)
        val px = cx + cos(a).toFloat() * rx
        val py = cy + sin(a).toFloat() * ry
        if (i == 0) moveTo(px, py) else lineTo(px, py)
      }
      close()
    }

    fun buildRoadUnionPath(extraHalfWidthWorld: Float): Path = Path().apply {
      paths.forEach { seg ->
        val dx = seg.endX - seg.startX
        val dy = seg.endY - seg.startY
        val len = hypot(dx, dy).coerceAtLeast(1f)
        val halfW = (seg.width * 0.50f + extraHalfWidthWorld).coerceAtLeast(6f)
        val nx = (-dy / len) * halfW
        val ny = (dx / len) * halfW

        moveTo(sx(seg.startX - nx), sy(seg.startY - ny, 0f))
        lineTo(sx(seg.endX - nx), sy(seg.endY - ny, 0f))
        lineTo(sx(seg.endX + nx), sy(seg.endY + ny, 0f))
        lineTo(sx(seg.startX + nx), sy(seg.startY + ny, 0f))
        close()

        val capRx = halfW * zoom
        val capRy = halfW * ObliqueCamera.OBLIQUE_Y_SCALE * zoom
        addClockwiseCap(sx(seg.startX), sy(seg.startY, 0f), capRx, capRy)
        addClockwiseCap(sx(seg.endX), sy(seg.endY, 0f), capRx, capRy)
      }
    }

    val curbOuterShadow = buildRoadUnionPath(extraHalfWidthWorld = 7.5f)
    val curbPaleLimestone = buildRoadUnionPath(extraHalfWidthWorld = 4.5f)
    val cobbleInterior = buildRoadUnionPath(extraHalfWidthWorld = -2.0f)

    // Continuous smooth stone road curbs (outer shadow + sunlit limestone curb lip)
    drawPath(curbOuterShadow, Color(0xFF616873))
    drawPath(curbPaleLimestone, Color(0xFFDADEE6))

    // Pixel-art grey cobblestone interior clipped smoothly inside the road network
    val cobbleTiles = PixelTileAtlas.sanctuaryCausewayTiles
    clipPath(cobbleInterior) {
      val worldTileSize = 64f
      val rStartTx = startTx.coerceAtLeast(6)
      val rEndTx = endTx.coerceAtMost(39)
      val rStartTy = startTy.coerceAtLeast(5)
      val rEndTy = endTy.coerceAtMost(36)
      for (ty in rStartTy..rEndTy) {
        val wy = ty * worldTileSize
        val dstY = sy(wy, 0f).roundToInt()
        for (tx in rStartTx..rEndTx) {
          val wx = tx * worldTileSize
          if (wx > 2460f) continue
          val dstX = sx(wx).roundToInt()
          val hash = ((tx * 73856093) xor (ty * 19349663)) and 0x7FFFFFFF
          drawImage(
            image = cobbleTiles[hash % cobbleTiles.size],
            dstOffset = IntOffset(dstX, dstY),
            dstSize = IntSize(tileScreenW + 1, tileScreenH + 1),
            filterQuality = FilterQuality.None,
          )
        }
      }
    }
  }

  private fun DrawScope.drawDriftingSkyClouds(
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
  ) {
    val cloudSpecs = listOf(
      Triple(-120f, 260f, 1.25f),
      Triple(160f, 1650f, 1.35f),
      Triple(2980f, 420f, 1.20f),
      Triple(3080f, 1520f, 1.45f),
      Triple(420f, 2580f, 1.30f),
      Triple(2780f, 2560f, 1.35f),
    )
    val halfW = size.width / 2f
    val halfH = size.height / 2f
    cloudSpecs.forEachIndexed { idx, (wx, wy, cScale) ->
      val drift = sin(timeSec * 0.22f + idx * 1.4f) * 28f
      val cx = ObliqueCamera.worldToScreenX(wx + drift, camX, zoom, halfW)
      val cy = ObliqueCamera.worldToScreenY(wy, camY, zoom, halfH)
      val s = zoom * cScale
      if (cx in -320f..(size.width + 320f) && cy in -220f..(size.height + 220f)) {
        drawOval(Color(0xFFC4E1FA).copy(alpha = 0.85f), Offset(cx - 120f * s, cy - 18f * s), Size(240f * s, 54f * s))
        drawOval(Color(0xFFF4FAFF).copy(alpha = 0.94f), Offset(cx - 108f * s, cy - 34f * s), Size(110f * s, 52f * s))
        drawOval(Color(0xFFFFFFFF).copy(alpha = 0.96f), Offset(cx - 46f * s, cy - 52f * s), Size(118f * s, 64f * s))
        drawOval(Color(0xFFF4FAFF).copy(alpha = 0.94f), Offset(cx + 28f * s, cy - 32f * s), Size(92f * s, 48f * s))
      }
    }
  }

  private fun DrawScope.drawBackgroundFloatingIslands(
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
    timeSec: Float,
  ) {
    data class SkyIsle(val wx: Float, val wy: Float, val rw: Float, val rh: Float, val hasPines: Boolean)
    val isles = listOf(
      SkyIsle(90f, 520f, 165f, 68f, hasPines = false),
      SkyIsle(1280f, 60f, 190f, 72f, hasPines = false),
      SkyIsle(3090f, 260f, 225f, 88f, hasPines = true),
      SkyIsle(2660f, 150f, 85f, 38f, hasPines = false),
      SkyIsle(140f, 2480f, 115f, 48f, hasPines = false),
      SkyIsle(3060f, 2420f, 175f, 68f, hasPines = false),
    )

    isles.forEachIndexed { idx, isle ->
      val bob = sin(timeSec * 0.9f + idx * 1.3f) * 8f
      val cx = sx(isle.wx)
      val cy = sy(isle.wy, bob)
      val rw = isle.rw * zoom
      val rh = isle.rh * zoom
      if (cx + rw < -80f || cx - rw > size.width + 80f || cy + rh * 2.4f < -80f || cy - rh > size.height + 80f) return@forEachIndexed

      val rock = Path().apply {
        moveTo(cx - rw * 0.92f, cy)
        lineTo(cx - rw * 0.55f, cy + rh * 1.35f)
        lineTo(cx - rw * 0.08f, cy + rh * 2.15f)
        lineTo(cx + rw * 0.48f, cy + rh * 1.45f)
        lineTo(cx + rw * 0.92f, cy)
        close()
      }
      drawPath(rock, Color(0xFF6E676E))
      val rockShadow = Path().apply {
        moveTo(cx - rw * 0.08f, cy + rh * 0.2f)
        lineTo(cx - rw * 0.08f, cy + rh * 2.15f)
        lineTo(cx + rw * 0.48f, cy + rh * 1.45f)
        lineTo(cx + rw * 0.92f, cy)
        close()
      }
      drawPath(rockShadow, Color(0xFF534E59))
      drawPath(rock, Color(0xFF2B2830), style = Stroke(width = (1.6f * zoom).coerceAtLeast(1f)))

      drawOval(Color(0xFF478F43), Offset(cx - rw, cy - rh * 0.44f), Size(rw * 2f, rh * 0.96f))
      drawOval(Color(0xFF6CC258), Offset(cx - rw * 0.95f, cy - rh * 0.48f), Size(rw * 1.90f, rh * 0.82f))

      if (isle.hasPines) {
        listOf(-0.38f to -0.15f, -0.12f to -0.28f, 0.22f to -0.10f).forEach { (ox, oy) ->
          val px = cx + rw * ox
          val py = cy + rh * oy
          val pine = Path().apply {
            moveTo(px, py - 46f * zoom)
            lineTo(px - 18f * zoom, py)
            lineTo(px + 18f * zoom, py)
            close()
          }
          drawPath(pine, Color(0xFF3E8756))
          drawPath(pine, Color(0xFF244D36), style = Stroke(width = 1.2f * zoom))
        }
      }
    }
  }

  private fun DrawScope.drawFloatingIslandCliffUnderside(
    verts: List<Pair<Float, Float>>,
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
  ) {
    val dropMain = 265f * zoom
    val wTop = verts[7]
    val wBot = verts[6]
    val sw = verts[5]
    val se = verts[4]
    val eBot = verts[3]
    val eTop = verts[2]

    val leftCliff = Path().apply {
      moveTo(sx(wTop.first - 24f), sy(wTop.second, 0f))
      lineTo(sx(wBot.first - 28f), sy(wBot.second, 0f))
      lineTo(sx(sw.first), sy(sw.second + 18f, 0f))
      lineTo(sx(1440f), sy(2380f, 0f))
      lineTo(sx(1340f), sy(2380f, 0f) + dropMain * 1.02f)
      lineTo(sx(1040f), sy(2360f, 0f) + dropMain * 0.90f)
      lineTo(sx(720f), sy(2220f, 0f) + dropMain * 0.84f)
      lineTo(sx(430f), sy(1960f, 0f) + dropMain * 0.64f)
      lineTo(sx(285f), sy(1540f, 0f) + dropMain * 0.34f)
      close()
    }
    drawPath(leftCliff, Color(0xFF7B6756))

    val centerChasm = Path().apply {
      moveTo(sx(1360f), sy(2370f, 0f))
      lineTo(sx(1860f), sy(2370f, 0f))
      lineTo(sx(1920f), sy(2370f, 0f) + dropMain * 0.82f)
      lineTo(sx(1610f), sy(2370f, 0f) + dropMain * 0.38f)
      lineTo(sx(1300f), sy(2370f, 0f) + dropMain * 0.84f)
      close()
    }
    drawPath(centerChasm, Color(0xFF3E3A46))

    val rightCliff = Path().apply {
      moveTo(sx(1780f), sy(2380f, 0f))
      lineTo(sx(se.first), sy(se.second + 18f, 0f))
      lineTo(sx(eBot.first + 28f), sy(eBot.second, 0f))
      lineTo(sx(eTop.first + 24f), sy(eTop.second, 0f))
      lineTo(sx(2915f), sy(1540f, 0f) + dropMain * 0.34f)
      lineTo(sx(2760f), sy(1980f, 0f) + dropMain * 0.66f)
      lineTo(sx(2460f), sy(2240f, 0f) + dropMain * 0.86f)
      lineTo(sx(2120f), sy(2360f, 0f) + dropMain * 0.94f)
      lineTo(sx(1880f), sy(2380f, 0f) + dropMain * 1.00f)
      close()
    }
    drawPath(rightCliff, Color(0xFF5D5048))

    for (cxWorld in listOf(560f to 2040f, 880f to 2340f, 1160f to 2370f, 2060f to 2370f, 2360f to 2330f, 2660f to 2040f)) {
      val rx = sx(cxWorld.first)
      val ry = sy(cxWorld.second, 0f)
      val crag = Path().apply {
        moveTo(rx - 48f * zoom, ry + 26f * zoom)
        lineTo(rx + 42f * zoom, ry + 30f * zoom)
        lineTo(rx + 14f * zoom, ry + 160f * zoom)
        lineTo(rx - 22f * zoom, ry + 132f * zoom)
        close()
      }
      drawPath(crag, if (cxWorld.first < 1600f) Color(0xFF8C7764) else Color(0xFF473D38))
    }

    drawPath(leftCliff, Color(0xFF28211F), style = Stroke(width = (2.2f * zoom).coerceAtLeast(1.2f)))
    drawPath(rightCliff, Color(0xFF28211F), style = Stroke(width = (2.2f * zoom).coerceAtLeast(1.2f)))
  }

  private fun DrawScope.drawGrassOverhangRim(
    verts: List<Pair<Float, Float>>,
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
  ) {
    val outerOffsets = listOf(
      0f to -20f,
      0f to -20f,
      46f to -8f,
      50f to 18f,
      18f to 44f,
      -18f to 44f,
      -50f to 18f,
      -46f to -8f,
    )
    val rimShadowPath = Path()
    val rimBrightPath = Path()
    verts.forEachIndexed { idx, (vx, vy) ->
      val (ox, oy) = outerOffsets[idx]
      val px = sx(vx + ox)
      val py = sy(vy + oy, 0f)
      if (idx == 0) {
        rimShadowPath.moveTo(px, py + 12f * zoom)
        rimBrightPath.moveTo(px, py)
      } else {
        rimShadowPath.lineTo(px, py + 12f * zoom)
        rimBrightPath.lineTo(px, py)
      }
    }
    rimShadowPath.close()
    rimBrightPath.close()

    drawPath(rimShadowPath, Color(0xFF3E8234))
    drawPath(rimBrightPath, Color(0xFF69BD4F))
    drawPath(rimBrightPath, Color(0xFF264D22), style = Stroke(width = (2.0f * zoom).coerceAtLeast(1f)))
  }

  private fun DrawScope.drawResidentialGardenCourtyards(
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
  ) {
    // 1. Warm Ochre Garden Soil Paths in the Residential Hedge Courtyard (x: 935..1255, y: 985..1285)
    val dirtColor = Color(0xFFD4B282)
    val dirtBorder = Color(0xFFA68458)
    val p1Left = sx(935f)
    val p1Top = sy(985f, 0f)
    val p1W = sx(1255f) - p1Left
    val p1H = sy(1285f, 0f) - p1Top
    drawRect(dirtBorder, Offset(p1Left, p1Top), Size(p1W, p1H), style = Stroke(width = 3f * zoom))
    drawRect(dirtColor.copy(alpha = 0.88f), Offset(p1Left, p1Top), Size(p1W, p1H))

    // Green inner lawn beds inside courtyard
    fun drawLawnBed(x0: Float, y0: Float, x1: Float, y1: Float) {
      val l = sx(x0)
      val t = sy(y0, 0f)
      drawRect(Color(0xFF69BD4F), Offset(l, t), Size(sx(x1) - l, sy(y1, 0f) - t))
    }
    drawLawnBed(955f, 995f, 1085f, 1115f)
    drawLawnBed(1115f, 1135f, 1245f, 1270f)
    drawLawnBed(955f, 1145f, 1085f, 1270f)

    // Sculpted Boxwood Hedge Maze Bars (matching the L-shaped hedges in reference!)
    fun drawHedgeStrip(x0: Float, y0: Float, x1: Float, y1: Float) {
      val l = sx(x0)
      val r = sx(x1)
      val t = sy(y0, 10f)
      val b = sy(y1, 0f)
      val w = (r - l).coerceAtLeast(4f * zoom)
      val h = (b - t).coerceAtLeast(6f * zoom)
      drawRect(Color(0xFF245924), Offset(l, t + h * 0.45f), Size(w, h * 0.55f))
      drawRect(Color(0xFF5CB847), Offset(l, t), Size(w, h * 0.65f))
      drawRect(Color(0xFF86D968), Offset(l + 2f * zoom, t + 1f * zoom), Size((w - 4f * zoom).coerceAtLeast(1f), h * 0.25f))
      drawRect(Color(0xFF193D19), Offset(l, t), Size(w, h), style = Stroke(width = (1.3f * zoom).coerceAtLeast(1f)))
    }
    drawHedgeStrip(1125f, 1145f, 1238f, 1168f)
    drawHedgeStrip(1125f, 1195f, 1238f, 1218f)
    drawHedgeStrip(1125f, 1242f, 1238f, 1265f)
    drawHedgeStrip(1125f, 1145f, 1148f, 1218f)
    drawHedgeStrip(962f, 1155f, 1075f, 1178f)
    drawHedgeStrip(962f, 1238f, 1075f, 1262f)

    // Low Pale-Stone Courtyard Borders along Horizontal Mid-West Street (y = 1286f)
    fun drawLowGardenWall(x0: Float, y0: Float, x1: Float, y1: Float) {
      val l = sx(x0)
      val r = sx(x1)
      val t = sy(y0, 8f)
      val b = sy(y1, 0f)
      val w = (r - l).coerceAtLeast(4f * zoom)
      val h = (b - t).coerceAtLeast(5f * zoom)
      drawRect(Color(0xFF9C9588), Offset(l, t + h * 0.45f), Size(w, h * 0.55f))
      drawRect(Color(0xFFDFD9CC), Offset(l, t), Size(w, h * 0.50f))
      drawRect(Color(0xFF3A3530), Offset(l, t), Size(w, h), style = Stroke(width = (1.2f * zoom).coerceAtLeast(1f)))
    }
    drawLowGardenWall(675f, 1280f, 915f, 1292f)
    drawLowGardenWall(945f, 1280f, 1265f, 1292f)
    drawLowGardenWall(1255f, 965f, 1268f, 1292f)
  }

  private fun DrawScope.drawCentralCircularWhitePlaza(
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
    timeSec: Float,
  ) {
    val cx = sx(SanctuaryIslandGeometry.PLAZA_CENTER_X)
    val cy = sy(SanctuaryIslandGeometry.PLAZA_CENTER_Y, 0f)
    val rx = SanctuaryIslandGeometry.PLAZA_RADIUS * zoom
    val ry = rx * ObliqueCamera.OBLIQUE_Y_SCALE

    if (cx + rx < -60f || cx - rx > size.width + 60f || cy + ry < -60f || cy - ry > size.height + 60f) return

    // 1. Stepped Outer Limestone Curb & Drop Shadow
    drawOval(Color(0xFF8C867B), Offset(cx - rx * 1.02f, cy - ry * 1.01f + 6f * zoom), Size(rx * 2.04f, ry * 2.02f))
    drawOval(Color(0xFFDDD8CE), Offset(cx - rx * 1.01f, cy - ry * 1.01f), Size(rx * 2.02f, ry * 2.02f))
    drawOval(Color(0xFFF3F0EA), Offset(cx - rx * 0.96f, cy - ry * 0.96f), Size(rx * 1.92f, ry * 1.92f))

    // 2. Concentric Carved Alabaster Rings
    val ringFractions = listOf(1.01f, 0.95f, 0.72f, 0.62f, 0.44f, 0.34f, 0.22f)
    drawOval(Color(0xFFEAE6DE), Offset(cx - rx * 0.72f, cy - ry * 0.72f), Size(rx * 1.44f, ry * 1.44f))
    drawOval(Color(0xFFF5F3EE), Offset(cx - rx * 0.62f, cy - ry * 0.62f), Size(rx * 1.24f, ry * 1.24f))
    drawOval(Color(0xFFE8E4DC), Offset(cx - rx * 0.34f, cy - ry * 0.34f), Size(rx * 0.68f, ry * 0.68f))
    drawOval(Color(0xFFF7F5F0), Offset(cx - rx * 0.22f, cy - ry * 0.22f), Size(rx * 0.44f, ry * 0.44f))

    // Radial Stone Joints (16 divisions across outer & mid bands)
    for (i in 0 until 16) {
      val ang = i * (Math.PI * 2.0 / 16.0)
      val cA = cos(ang).toFloat()
      val sA = sin(ang).toFloat()
      drawLine(
        color = Color(0xFFC2BDB2),
        start = Offset(cx + cA * rx * 0.22f, cy + sA * ry * 0.22f),
        end = Offset(cx + cA * rx * 0.95f, cy + sA * ry * 0.95f),
        strokeWidth = (1.3f * zoom).coerceAtLeast(0.8f),
      )
    }

    ringFractions.forEachIndexed { idx, frac ->
      drawOval(
        color = if (idx == 0) Color(0xFF57534C) else Color(0xFFAFA99E),
        topLeft = Offset(cx - rx * frac, cy - ry * frac),
        size = Size(rx * frac * 2f, ry * frac * 2f),
        style = Stroke(width = (if (idx <= 1) 2.2f else 1.5f) * zoom),
      )
    }

    // 3. Subtle Central Astrolabe Energy Core Pulse & Four Inlaid Pastel Elemental Magic Rune Circles
    val pulse = 0.78f + 0.22f * sin(timeSec * 2.4f)
    drawOval(
      color = Color(0xFFBAE6FD).copy(alpha = 0.28f * pulse),
      topLeft = Offset(cx - rx * 0.16f, cy - ry * 0.16f),
      size = Size(rx * 0.32f, ry * 0.32f),
    )
    drawOval(
      color = Color(0xFFF5B841).copy(alpha = 0.62f * pulse),
      topLeft = Offset(cx - rx * 0.11f, cy - ry * 0.11f),
      size = Size(rx * 0.22f, ry * 0.22f),
      style = Stroke(width = 1.6f * zoom),
    )
    data class ElementalRune(val dxFrac: Float, val dyFrac: Float, val outerCol: Color, val innerCol: Color)
    val runes = listOf(
      ElementalRune(0f, -0.78f, Color(0xFFF472B6), Color(0xFFFCE7F3)), // North: Sakura Rose
      ElementalRune(-0.78f, 0f, Color(0xFFEAB308), Color(0xFFFEF9C3)), // West: Solar Gold
      ElementalRune(0.78f, 0f, Color(0xFFF59E0B), Color(0xFFFEF3C7)),  // East: Amber Star
      ElementalRune(0f, 0.78f, Color(0xFF818CF8), Color(0xFFE0E7FF)),  // South: Astral Azure
    )

    val runeRx = rx * 0.145f
    val runeRy = ry * 0.145f
    runes.forEach { r ->
      val rCx = cx + r.dxFrac * rx
      val rCy = cy + r.dyFrac * ry
      drawOval(
        color = r.innerCol.copy(alpha = 0.85f * pulse),
        topLeft = Offset(rCx - runeRx, rCy - runeRy),
        size = Size(runeRx * 2f, runeRy * 2f),
      )
      drawOval(
        color = r.outerCol.copy(alpha = 0.85f),
        topLeft = Offset(rCx - runeRx, rCy - runeRy),
        size = Size(runeRx * 2f, runeRy * 2f),
        style = Stroke(width = 2.2f * zoom),
      )
      drawOval(
        color = r.outerCol.copy(alpha = 0.72f),
        topLeft = Offset(rCx - runeRx * 0.65f, rCy - runeRy * 0.65f),
        size = Size(runeRx * 1.30f, runeRy * 1.30f),
        style = Stroke(width = 1.4f * zoom),
      )
      for (k in 0 until 6) {
        val a1 = k * (Math.PI / 3.0)
        val a2 = (k + 2) * (Math.PI / 3.0)
        drawLine(
          color = r.outerCol.copy(alpha = 0.65f),
          start = Offset(rCx + cos(a1).toFloat() * runeRx * 0.65f, rCy + sin(a1).toFloat() * runeRy * 0.65f),
          end = Offset(rCx + cos(a2).toFloat() * runeRx * 0.65f, rCy + sin(a2).toFloat() * runeRy * 0.65f),
          strokeWidth = (1.1f * zoom).coerceAtLeast(0.7f),
        )
      }
    }

    // 4. Grand Diagonal 8-Step Stone Staircase Linking SE Plaza Rim -> Classical Amphitheatre Stage
    val stairSteps = 8
    for (s in 0 until stairSteps) {
      val t0 = s.toFloat() / stairSteps
      val t1 = (s + 1f) / stairSteps
      val lx0 = 2115f + t0 * 145f
      val ly0 = 1690f + t0 * 145f
      val rx0 = 2255f + t0 * 145f
      val ry0 = 1575f + t0 * 145f
      val lx1 = 2115f + t1 * 145f
      val ly1 = 1690f + t1 * 145f
      val rx1 = 2255f + t1 * 145f
      val ry1 = 1575f + t1 * 145f
      val stepPath = Path().apply {
        moveTo(sx(lx0), sy(ly0, 0f))
        lineTo(sx(rx0), sy(ry0, 0f))
        lineTo(sx(rx1), sy(ry1, 0f))
        lineTo(sx(lx1), sy(ly1, 0f))
        close()
      }
      drawPath(stepPath, if (s % 2 == 0) Color(0xFFEDE9E0) else Color(0xFFC9C3B6))
      drawLine(Color(0xFF757065), Offset(sx(lx1), sy(ly1, 0f)), Offset(sx(rx1), sy(ry1, 0f)), strokeWidth = (1.5f * zoom).coerceAtLeast(0.9f))
    }
    // Sloped stone cheek balustrades flanking the grand plaza-to-amphitheatre steps
    drawLine(Color(0xFFDFDBD0), Offset(sx(2112f), sy(1688f, 10f)), Offset(sx(2258f), sy(1836f, 4f)), strokeWidth = 5.5f * zoom)
    drawLine(Color(0xFFDFDBD0), Offset(sx(2258f), sy(1572f, 10f)), Offset(sx(2402f), sy(1720f, 4f)), strokeWidth = 5.5f * zoom)
  }

  private fun DrawScope.drawSouthernMainGateAndBridges(
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
  ) {
    // 1. Raised Stone Gatehouse Courtyard Platform (x: 1380..1840, y: 2210..2390)
    val platL = sx(1380f)
    val platR = sx(1840f)
    val platT = sy(2210f, 0f)
    val platB = sy(2390f, 0f)
    val platW = (platR - platL).coerceAtLeast(2f)
    val platH = (platB - platT).coerceAtLeast(2f)

    // Front vertical ashlar stone cliff-wall under the gatehouse deck
    drawRect(Color(0xFF7D776C), Offset(platL, platB), Size(platW, 42f * zoom))
    drawRect(Color(0xFF45413B), Offset(platL, platB), Size(platW, 42f * zoom), style = Stroke(width = 1.6f * zoom))
    drawRect(Color(0xFFDAD5CA), Offset(platL, platT), Size(platW, platH))
    drawRect(Color(0xFF45413B), Offset(platL, platT), Size(platW, platH), style = Stroke(width = 2f * zoom))

    // Stone slab grid lines on gate platform
    for (gx in 1430..1790 step 60) {
      val lx = sx(gx.toFloat())
      drawLine(Color(0xFFA8A296), Offset(lx, platT), Offset(lx, platB), strokeWidth = 1.2f * zoom)
    }
    for (gy in 2245..2365 step 40) {
      val ly = sy(gy.toFloat(), 0f)
      drawLine(Color(0xFFA8A296), Offset(platL, ly), Offset(platR, ly), strokeWidth = 1.2f * zoom)
    }

    // Flanking Angled Grey-Stone Staircases on Left & Right of the Gatehouse (matching reference!)
    val leftWingStairs = Path().apply {
      moveTo(sx(1385f), sy(2255f, 0f))
      lineTo(sx(1465f), sy(2175f, 0f))
      lineTo(sx(1525f), sy(2175f, 0f))
      lineTo(sx(1465f), sy(2255f, 0f))
      close()
    }
    drawPath(leftWingStairs, Color(0xFFB5B0A6))
    drawPath(leftWingStairs, Color(0xFF3E3B36), style = Stroke(width = 1.5f * zoom))

    val rightWingStairs = Path().apply {
      moveTo(sx(1695f), sy(2175f, 0f))
      lineTo(sx(1755f), sy(2175f, 0f))
      lineTo(sx(1835f), sy(2255f, 0f))
      lineTo(sx(1755f), sy(2255f, 0f))
      close()
    }
    drawPath(rightWingStairs, Color(0xFFB5B0A6))
    drawPath(rightWingStairs, Color(0xFF3E3B36), style = Stroke(width = 1.5f * zoom))

    // Center entrance steps
    for (s in 0..5) {
      val stepY = sy(2175f + s * 6.5f, 0f)
      drawLine(
        color = if (s % 2 == 0) Color(0xFFE6E1D6) else Color(0xFF8C867B),
        start = Offset(sx(1530f), stepY),
        end = Offset(sx(1690f), stepY),
        strokeWidth = 3f * zoom,
      )
    }

    // Twin carved dark-stone gate pedestals flanking the central steps
    listOf(1495f, 1725f).forEach { pxWorld ->
      val bx = sx(pxWorld)
      val by = sy(2215f, 0f)
      drawRect(Color(0xFF57534E), Offset(bx - 18f * zoom, by - 26f * zoom), Size(36f * zoom, 32f * zoom))
      drawRect(Color(0xFF8A847C), Offset(bx - 14f * zoom, by - 30f * zoom), Size(28f * zoom, 10f * zoom))
      drawRect(Color(0xFF262320), Offset(bx - 18f * zoom, by - 26f * zoom), Size(36f * zoom, 32f * zoom), style = Stroke(width = 1.5f * zoom))
    }

    // 2. Twin Parallel Wooden Plank Drawbridges Extending Over the Southern Sky Chasm!
    listOf(1455f to 1535f, 1685f to 1765f).forEach { (bx0, bx1) ->
      val bl = sx(bx0)
      val br = sx(bx1)
      val bt = sy(2376f, 0f)
      val bb = sy(2605f, 0f)
      val bw = (br - bl).coerceAtLeast(4f)
      val bh = (bb - bt).coerceAtLeast(4f)

      drawRect(Color(0xFF4A2E1B), Offset(bl - 3f * zoom, bt), Size(bw + 6f * zoom, bh + 5f * zoom))
      drawRect(Color(0xFFB8804C), Offset(bl, bt), Size(bw, bh))
      var py = bt + 7f * zoom
      while (py < bb) {
        drawLine(Color(0xFF6E4528), Offset(bl, py), Offset(br, py), strokeWidth = (1.5f * zoom).coerceAtLeast(1f))
        py += 9f * zoom
      }
      drawRect(Color(0xFF2B1A0F), Offset(bl - 2f * zoom, bt), Size(bw + 4f * zoom, bh), style = Stroke(width = 1.6f * zoom))
    }
  }

  private fun DrawScope.drawBackAndSideFortressWalls(
    verts: List<Pair<Float, Float>>,
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
    eastBridgeOpen: Boolean = false,
  ) {
    val nwTop = verts[0] // (980, 340)
    val neTop = verts[1] // (2240, 340)
    val eTop = verts[2]  // (2900, 880)
    val eBot = verts[3]  // (2900, 1780)
    val wBot = verts[6]  // (300, 1780)
    val wTop = verts[7]  // (300, 880)

    // Tall Monumental North Horizontal Fortress Wall (with North Arched Gate Tunnel at x=1560!)
    drawFortressWallSegment(nwTop.first, nwTop.second, neTop.first, neTop.second, sx, sy, zoom, wallHeightZ = 104f, hasNorthTunnel = true)
    // Tall NW & NE Sloped Back Curtain Walls
    drawFortressWallSegment(wTop.first, wTop.second, nwTop.first, nwTop.second, sx, sy, zoom, wallHeightZ = 94f)
    drawFortressWallSegment(neTop.first, neTop.second, eTop.first, eTop.second, sx, sy, zoom, wallHeightZ = 94f)
    // West & East Vertical Rampart Side Walls
    drawSideRampartWall(wTop.first, wTop.second, wBot.first, wBot.second, sx, sy, zoom, isWestSide = true)
    if (eastBridgeOpen) {
      drawSideRampartWall(eTop.first, eTop.second, eTop.first, 1192f, sx, sy, zoom, isWestSide = false)
      drawSideRampartWall(eTop.first, 1338f, eBot.first, eBot.second, sx, sy, zoom, isWestSide = false)
    } else {
      drawSideRampartWall(eTop.first, eTop.second, eBot.first, eBot.second, sx, sy, zoom, isWestSide = false)
    }
  }

  private fun DrawScope.drawSideRampartWall(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
    isWestSide: Boolean,
  ) {
    val wallZ = 76f
    val top0Y = sy(y0, wallZ)
    val top1Y = sy(y1, wallZ)
    val base0Y = sy(y0, 0f)
    val base1Y = sy(y1, 0f)
    val cx = sx(x0)
    val walkHalfW = 14f * zoom
    val innerFaceW = 11f * zoom

    // Inner stone vertical face visible toward the island interior
    val innerLeft = if (isWestSide) cx + walkHalfW else cx - walkHalfW - innerFaceW
    drawRect(
      color = if (isWestSide) Color(0xFFB5AA94) else Color(0xFF9E937E),
      topLeft = Offset(innerLeft, top0Y),
      size = Size(innerFaceW, (base1Y - top0Y).coerceAtLeast(4f)),
    )
    drawRect(
      color = Color(0xFF2B2520),
      topLeft = Offset(innerLeft, top0Y),
      size = Size(innerFaceW, (base1Y - top0Y).coerceAtLeast(4f)),
      style = Stroke(width = (1.3f * zoom).coerceAtLeast(0.8f)),
    )

    // Sunlit stone top rampart walkway
    drawRect(
      color = Color(0xFFE6DEC9),
      topLeft = Offset(cx - walkHalfW, top0Y),
      size = Size(walkHalfW * 2f, (top1Y - top0Y).coerceAtLeast(4f)),
    )
    drawRect(
      color = Color(0xFF2B2520),
      topLeft = Offset(cx - walkHalfW, top0Y),
      size = Size(walkHalfW * 2f, (top1Y - top0Y).coerceAtLeast(4f)),
      style = Stroke(width = (1.6f * zoom).coerceAtLeast(1f)),
    )
  }

  private fun DrawScope.drawSouthernExteriorWallSegment(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
  ) {
    val wallHeightZ = 60f
    val cliffRetainingDrop = 54f * zoom
    val base0X = sx(x0)
    val base0Y = sy(y0, 0f)
    val base1X = sx(x1)
    val base1Y = sy(y1, 0f)
    val top0Y = sy(y0, wallHeightZ)
    val top1Y = sy(y1, wallHeightZ)
    val walkwayThick = 15f * zoom

    // 1. Exterior Ashlar Stone Retaining Wall Hanging Down onto the Cliff Edge!
    val extFace = Path().apply {
      moveTo(base0X, top0Y)
      lineTo(base1X, top1Y)
      lineTo(base1X, base1Y + cliffRetainingDrop)
      lineTo(base0X, base0Y + cliffRetainingDrop)
      close()
    }
    drawPath(extFace, if (x0 < 1600f) Color(0xFFA89D89) else Color(0xFF8F8573))

    for (c in 1..4) {
      val f = c / 5f
      val ly0 = top0Y + (base0Y + cliffRetainingDrop - top0Y) * f
      val ly1 = top1Y + (base1Y + cliffRetainingDrop - top1Y) * f
      drawLine(Color(0xFF6B6355), Offset(base0X, ly0), Offset(base1X, ly1), strokeWidth = (1.1f * zoom).coerceAtLeast(0.7f))
    }
    for (i in 1 until 6) {
      val t = i / 6f
      val px = base0X + (base1X - base0X) * t
      val pyTop = top0Y + (top1Y - top0Y) * t
      val pyBot = base0Y + (base1Y - base0Y) * t + cliffRetainingDrop
      drawLine(Color(0xFF7D7464), Offset(px, pyTop), Offset(px, pyBot), strokeWidth = 3.5f * zoom)
    }
    drawPath(extFace, Color(0xFF2B2520), style = Stroke(width = (1.8f * zoom).coerceAtLeast(1f)))

    // 2. Sunlit Top Rampart Walkway
    val walkwayPath = Path().apply {
      moveTo(base0X, top0Y - walkwayThick)
      lineTo(base1X, top1Y - walkwayThick)
      lineTo(base1X, top1Y)
      lineTo(base0X, top0Y)
      close()
    }
    drawPath(walkwayPath, Color(0xFFE6DEC9))
    drawPath(walkwayPath, Color(0xFF2B2520), style = Stroke(width = (1.6f * zoom).coerceAtLeast(1f)))

    // 3. Crenellated Battlement Merlons
    val lenWorld = hypot(x1 - x0, y1 - y0)
    val merlonCount = (lenWorld / 54f).toInt().coerceAtLeast(4)
    for (m in 0 until merlonCount) {
      val t0 = (m + 0.14f) / merlonCount
      val t1 = (m + 0.68f) / merlonCount
      val mx0 = base0X + (base1X - base0X) * t0
      val my0 = top0Y + (top1Y - top0Y) * t0 - walkwayThick
      val mx1 = base0X + (base1X - base0X) * t1
      val my1 = top0Y + (top1Y - top0Y) * t1 - walkwayThick
      val merlonH = 9.5f * zoom
      val merlon = Path().apply {
        moveTo(mx0, my0 - merlonH)
        lineTo(mx1, my1 - merlonH)
        lineTo(mx1, my1 + 1f)
        lineTo(mx0, my0 + 1f)
        close()
      }
      drawPath(merlon, Color(0xFFEDE5D2))
      drawPath(merlon, Color(0xFF2B2520), style = Stroke(width = (1.3f * zoom).coerceAtLeast(0.8f)))
    }
  }

  private fun DrawScope.drawFortressWallSegment(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
    wallHeightZ: Float,
    hasNorthTunnel: Boolean = false,
  ) {
    val base0X = sx(x0)
    val base0Y = sy(y0, 0f)
    val base1X = sx(x1)
    val base1Y = sy(y1, 0f)
    val top0Y = sy(y0, wallHeightZ)
    val top1Y = sy(y1, wallHeightZ)
    val walkwayThick = 16f * zoom

    // 1. Tall Vertical Ashlar Stone Masonry Face
    val facePath = Path().apply {
      moveTo(base0X, top0Y)
      lineTo(base1X, top1Y)
      lineTo(base1X, base1Y)
      lineTo(base0X, base0Y)
      close()
    }
    drawPath(facePath, Color(0xFFC2B7A1))

    // Recessed architectural wall panels & horizontal ashlar courses
    for (c in 1..4) {
      val f = c / 5f
      drawLine(
        color = Color(0xFF8F8572),
        start = Offset(base0X, top0Y + (base0Y - top0Y) * f),
        end = Offset(base1X, top1Y + (base1Y - top1Y) * f),
        strokeWidth = (1.1f * zoom).coerceAtLeast(0.7f),
      )
    }
    val segSteps = 9
    for (i in 1 until segSteps) {
      val t = i.toFloat() / segSteps
      val px = base0X + (base1X - base0X) * t
      val pyTop = top0Y + (top1Y - top0Y) * t
      val pyBot = base0Y + (base1Y - base0Y) * t
      drawLine(Color(0xFF9C917D), Offset(px, pyTop), Offset(px, pyBot), strokeWidth = 4.2f * zoom)
    }

    // 2. Sunlit Top Stone Parapet Walkway Ribbon
    val walkwayPath = Path().apply {
      moveTo(base0X, top0Y - walkwayThick)
      lineTo(base1X, top1Y - walkwayThick)
      lineTo(base1X, top1Y)
      lineTo(base0X, top0Y)
      close()
    }
    drawPath(walkwayPath, Color(0xFFEAE2CE))
    drawPath(facePath, Color(0xFF2B2520), style = Stroke(width = (1.8f * zoom).coerceAtLeast(1f)))
    drawPath(walkwayPath, Color(0xFF2B2520), style = Stroke(width = (1.8f * zoom).coerceAtLeast(1f)))

    // 3. Crenellated Battlement Merlons along Top Outer Edge!
    val lenWorld = hypot(x1 - x0, y1 - y0)
    val merlonCount = (lenWorld / 50f).toInt().coerceAtLeast(4)
    for (m in 0 until merlonCount) {
      val t0 = (m + 0.14f) / merlonCount
      val t1 = (m + 0.68f) / merlonCount
      val mx0 = base0X + (base1X - base0X) * t0
      val my0 = top0Y + (top1Y - top0Y) * t0 - walkwayThick
      val mx1 = base0X + (base1X - base0X) * t1
      val my1 = top0Y + (top1Y - top0Y) * t1 - walkwayThick
      val merlonH = 10.5f * zoom
      val merlon = Path().apply {
        moveTo(mx0, my0 - merlonH)
        lineTo(mx1, my1 - merlonH)
        lineTo(mx1, my1 + 1f)
        lineTo(mx0, my0 + 1f)
        close()
      }
      drawPath(merlon, Color(0xFFF0E8D6))
      drawPath(merlon, Color(0xFF2B2520), style = Stroke(width = (1.3f * zoom).coerceAtLeast(0.8f)))
    }

    // 4. North Wall Arched Gate Tunnel at x = 1560f (exact match to reference!)
    if (hasNorthTunnel) {
      val tx = sx(SanctuaryIslandGeometry.NORTH_TUNNEL_X)
      val tyBase = sy(346f, 0f)
      val archW = 84f * zoom
      val archH = 62f * zoom
      drawOval(
        color = Color(0xFF8C826F),
        topLeft = Offset(tx - archW * 0.58f, tyBase - archH * 1.08f),
        size = Size(archW * 1.16f, archH * 1.05f),
      )
      drawRect(Color(0xFF8C826F), Offset(tx - archW * 0.58f, tyBase - archH * 0.56f), Size(archW * 1.16f, archH * 0.58f))
      drawOval(
        color = Color(0xFF17161E),
        topLeft = Offset(tx - archW * 0.45f, tyBase - archH * 0.95f),
        size = Size(archW * 0.90f, archH * 0.88f),
      )
      drawRect(Color(0xFF17161E), Offset(tx - archW * 0.45f, tyBase - archH * 0.52f), Size(archW * 0.90f, archH * 0.54f))
    }
  }

  private fun DrawScope.drawDiagonalSunlightRays() {
    withTransform({
      rotate(degrees = -34f, pivot = Offset(0f, 0f))
    }) {
      val raySpecs = listOf(
        80f to 130f,
        290f to 95f,
        490f to 155f,
        760f to 85f,
      )
      raySpecs.forEach { (offsetX, widthPx) ->
        drawRect(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0x30FFFDF0),
              Color(0x16FEF9C3),
              Color.Transparent,
            ),
            startY = 0f,
            endY = size.height * 1.35f,
          ),
          topLeft = Offset(offsetX, -80f),
          size = Size(widthPx, size.height * 1.45f),
        )
      }
    }
  }
}
