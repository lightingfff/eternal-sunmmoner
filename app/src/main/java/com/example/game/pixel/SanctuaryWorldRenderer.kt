package com.example.game.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
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
import kotlin.math.PI
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

  /**
   * Screen height of one 64 x 64 world tile, in px, below which the grass base switches to
   * pre-composed 4 x 4 blocks. 25 px keeps the change well below gameplay zoom (a tile is
   * 40 px tall at the 0.88 default camera), so the sharp art seen up close is untouched.
   */
  private const val GRASS_BLOCK_MIN_TILE_SCREEN_PX = 25

  // P26C: screen-space anchors, kept at object level (no per-frame list allocation).
  // Gameplay camera (960, 860, 0.88) shows sky only above/left of the fortress wall;
  // anchors MUST intersect that real visible wedge, not just the centre of the framebuffer.
  private data class CloudAnchor(val x: Float, val y: Float, val scale: Float, val parallax: Float, val depth: Int)
  private val cloudSpecs = listOf(
    // Far bank: small, slow shapes behind the fortress silhouette.
    CloudAnchor(-565f, 36f, 0.95f, 0.12f, 0),
    CloudAnchor(-355f, 20f, 1.00f, 0.18f, 0),
    CloudAnchor(760f, 70f, 0.95f, 0.10f, 0),
    // Middle cumulus: readable volume inside the open sky at gameplay zoom.
    CloudAnchor(-495f, 87f, 1.15f, 0.42f, 1),
    CloudAnchor(-345f, 65f, 0.93f, 0.48f, 1),
    CloudAnchor(540f, 115f, 1.08f, 0.38f, 1),
    // Foreground wisps: smaller/faster, masked naturally by walls and buildings.
    CloudAnchor(-495f, 140f, 0.80f, 0.60f, 2),
    CloudAnchor(700f, 180f, 0.70f, 0.55f, 2),
  )

  // Authored cloud contours. Unlike per-frame Path()s, these are built ONCE and reused
  // under a transform. Stepped edges echo the Sanctuary's pixel-art silhouettes.
  private val cumulusBody = Path().apply {
    moveTo(-98f, 14f); lineTo(-98f, -2f); lineTo(-80f, -2f)
    lineTo(-80f, -15f); lineTo(-61f, -15f); lineTo(-61f, -24f)
    lineTo(-45f, -24f); lineTo(-45f, -33f); lineTo(-17f, -33f)
    lineTo(-17f, -43f); lineTo(22f, -43f); lineTo(22f, -35f)
    lineTo(49f, -35f); lineTo(49f, -23f); lineTo(71f, -23f)
    lineTo(71f, -12f); lineTo(90f, -12f); lineTo(90f, 2f)
    lineTo(100f, 2f); lineTo(100f, 15f); lineTo(79f, 15f)
    lineTo(79f, 27f); lineTo(-75f, 27f); lineTo(-75f, 17f)
    lineTo(-98f, 17f); close()
  }
  private val cumulusUnder = Path().apply {
    moveTo(-92f, 7f); lineTo(-66f, 7f); lineTo(-66f, 13f)
    lineTo(78f, 13f); lineTo(78f, 23f); lineTo(61f, 23f)
    lineTo(61f, 27f); lineTo(-75f, 27f); lineTo(-75f, 17f)
    lineTo(-92f, 17f); close()
  }
  private val cumulusRim = Path().apply {
    moveTo(-45f, -33f); lineTo(-17f, -33f); lineTo(-17f, -43f)
    lineTo(22f, -43f); lineTo(22f, -35f); lineTo(49f, -35f)
    lineTo(49f, -29f); lineTo(16f, -29f); lineTo(16f, -35f)
    lineTo(-10f, -35f); lineTo(-10f, -26f); lineTo(-45f, -26f); close()
  }
  private val skyWisp = Path().apply {
    moveTo(-91f, 6f); lineTo(-65f, -2f); lineTo(-49f, -2f)
    lineTo(-41f, -11f); lineTo(-14f, -11f); lineTo(-6f, -20f)
    lineTo(15f, -16f); lineTo(23f, -7f); lineTo(59f, -7f)
    lineTo(84f, 2f); lineTo(98f, 5f); lineTo(68f, 11f)
    lineTo(-75f, 11f); close()
  }

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

    // 1. Time-of-day sky gradient, celestial discs, clouds and horizon haze.
    // P26: the sky is the mood carrier. Morning / noon / evening / night each read
    // differently (bright noon blue, warm dawn/evening, deep night) instead of one
    // fixed dark twilight gradient.
    drawSkyBands()
    drawCelestialDiscs()
    drawDriftingSkyClouds(camX, camY, zoom, timeSec)
    drawHorizonHaze()
    val daylight = ((com.example.game.world.WorldEnvironment.lighting.ambientIntensity-.73f)/.27f).coerceIn(0f,1f)
    drawRect(com.example.game.world.WorldEnvironment.lighting.skyTint.copy(alpha=daylight*.26f))
    with(DepthPresentation) { drawDistantFocus(camX,camY,zoom,timeSec) }

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

      // Continuous authored grass macros; procedural tile fallback retains the existing ground grid.
      val hdGrassTiles = FantasyHd128Atlas.hdSanctuaryGrassTiles
      val macroDrawn = with(ArtDepthAssets) {
        drawGrassMacros(startTx, endTx, startTy, endTy, ::sx, { sy(it) }, zoom)
      }
      if (!macroDrawn) {
        for (ty in startTy..endTy) {
          val wy = ty * worldTileSize
          val dstY = sy(wy).roundToInt()
          for (tx in startTx..endTx) {
            val wx = tx * worldTileSize
            val dstX = sx(wx).roundToInt()
            drawImage(
              image = ArtDepthAssets.tile(hdGrassTiles, tx, ty),
              dstOffset = IntOffset(dstX, dstY),
              dstSize = IntSize(tileScreenW + 1, tileScreenH + 1),
              filterQuality = FilterQuality.None,
            )
          }
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

      // 3C-2. Eastern Sakura Reflection Pond & Lotus Stepping-Stone Terrace + Northern Tower Gate Starlight Forecourt
      drawEasternSakuraLotusGarden(::sx, ::sy, zoom, timeSec)
      drawNorthernTowerGateForecourt(::sx, ::sy, zoom, timeSec)

      // 3C-3. Celestial Crystal River, Sacred Spring & Animated Koi/Caustics (drawn before roads/bridges so bridges span over water!)
      drawCelestialRiverChannel(::sx, ::sy, zoom, timeSec)

      // 3D. Smooth Continuous Curbed Grey-Cobblestone Road Network + Royal Crimson-Gold Processional Carpet Runners
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
      drawArchedMarbleFootbridges(::sx, ::sy, zoom)
      drawSouthernProcessionCarpet(paths, ::sx, ::sy, zoom, 1735f, 2210f)

      // 3E. Monumental Central Circular White Stone Plaza, 4 Pastel Elemental Runes, Central Astral Fountain & Grand SE Staircase
      with(PremiumSliceRenderer) { drawSmithyVerge(camX,camY,zoom) }
      drawCentralCircularWhitePlaza(::sx, ::sy, zoom, timeSec)

      // 3F. Dark Fantasy Ambient Occlusion - deep contact shadows under walls, plaza rim, river banks, gate
      drawSanctuaryAmbientOcclusion(::sx, ::sy, zoom)

      // 3G. HD-2D Shader Ground Sun Pools, Cloud Drift Shadows, Dappled Canopy Gobos & Specular Reflections
      with(SanctuarySunlightSystem) {
        drawGroundSunPoolsAndSpecularity(camX, camY, zoom, timeSec)
      }
    }

    // Southern entrance is a dry ceremonial approach; no front canal outlets.

    // 4. Southern Main Gate Raised Stone Courtyard, Flanking Angled Stairs & Dual Wooden Drawbridges
    drawSouthernMainGateAndBridges(paths, ::sx, ::sy, zoom)

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

    // 7. World-Anchored HD-2D Volumetric Sun Shafts & Floating Golden Motes
    with(SanctuarySunlightSystem) {
      drawVolumetricSunShaftsAndMotes(camX, camY, zoom, timeSec)
    }

    // 8. Local Light Glows - warm amber from gate braziers + cold rune light on plaza
    drawSanctuaryLocalLights(::sx, ::sy, zoom, timeSec)

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

    drawPath(outerGrassRim, Color(0xFF2A3D2B))
    drawPath(midSandLip, Color(0xFF4A3D2E))

    val sandTiles = FantasyHd128Atlas.hdSanctuarySandTiles
    clipPath(innerSandCore) {
      val worldTileSize = 64f
      for (ty in 23..33) {
        val wy = ty * worldTileSize
        val dstY = sy(wy, 0f).roundToInt()
        for (tx in 6..21) {
          val wx = tx * worldTileSize
          val dstX = sx(wx).roundToInt()
          drawImage(
            image = ArtDepthAssets.tile(sandTiles, tx, ty),
            dstOffset = IntOffset(dstX, dstY),
            dstSize = IntSize(tileScreenW + 1, tileScreenH + 1),
            filterQuality = FilterQuality.None,
          )
        }
      }
    }
    // Ground-only transition band: embedded rock at the soil rim, tuft silhouettes intrude inward.
    clipPath(outerGrassRim) {
      clipPath(innerSandCore, androidx.compose.ui.graphics.ClipOp.Difference) {
        drawMaterialBand(FantasyHd128Atlas.hdSanctuaryGravelTiles, sx, sy, zoom, 6..21, 23..33)
      }
    }
    SanctuaryArtAssets.image("verge")?.let { fringe ->
      clipPath(innerSandCore) {
        clipPath(buildSandPath(0.94f), androidx.compose.ui.graphics.ClipOp.Difference) {
          drawMaterialBand(listOf(fringe), sx, sy, zoom, 6..21, 23..33)
        }
      }
    }
  }

  private fun DrawScope.drawMaterialBand(
    tiles: List<androidx.compose.ui.graphics.ImageBitmap>,
    sx: (Float) -> Float, sy: (Float, Float) -> Float, zoom: Float,
    columns: IntRange, rows: IntRange,
    roadPaths: List<PathSegment>? = null,
    roadExtraWorld: Float = 0f,
  ) {
    for (ty in rows) for (tx in columns) {
      // The road verge ring is a clip ring: tiles fully outside the road cannot contribute.
      if (roadPaths != null && !SanctuaryRoadCoverage.mayContainRoad(roadPaths, tx, ty, roadExtraWorld)) continue
      drawImage(ArtDepthAssets.tile(tiles, tx, ty), dstOffset=IntOffset(sx(tx*64f).roundToInt(),sy(ty*64f,0f).roundToInt()),
        dstSize=IntSize((64*zoom).roundToInt().coerceAtLeast(1)+1,(64*zoom*ObliqueCamera.OBLIQUE_Y_SCALE).roundToInt().coerceAtLeast(1)+1),filterQuality=FilterQuality.None)
    }
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
    drawPath(curbOuterShadow, Color(0xFF1E2028))
    drawPath(curbPaleLimestone, Color(0xFF3A3D4A))

    // 128x128 HD-2D Fantasy cobblestone interior clipped smoothly inside the road network
    val cobbleTiles = FantasyHd128Atlas.hdSanctuaryCausewayTiles
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
          if (!SanctuaryRoadCoverage.mayContainRoad(paths, tx, ty)) continue
          val dstX = sx(wx).roundToInt()
          drawImage(
            image = ArtDepthAssets.tile(cobbleTiles, tx, ty),
            dstOffset = IntOffset(dstX, dstY),
            dstSize = IntSize(tileScreenW + 1, tileScreenH + 1),
            filterQuality = FilterQuality.None,
          )
        }
      }
    }
    SanctuaryArtAssets.image("verge")?.let { fringe ->
      clipPath(cobbleInterior) {
        clipPath(buildRoadUnionPath(-12f), androidx.compose.ui.graphics.ClipOp.Difference) {
          drawMaterialBand(
            listOf(fringe), sx, sy, zoom, startTx..endTx, startTy..endTy,
            // Outer curb union is the widest ring this band can reach.
            roadPaths = paths, roadExtraWorld = 7.5f,
          )
        }
      }
    }
  }

  /** P26: pre-interpolated 12-band sky colours (RGB floats, 0..255). Cached by quantised
   *  hour + weather buckets, so the render loop never allocates a gradient brush or a
   *  colour list per frame. Same state key => the same buffer instance (contract-tested). */
  private val skyBandCache = mutableMapOf<Long, FloatArray>()
  fun skyBandColors(hour: Float, weather: com.example.game.world.WeatherState): FloatArray {
    val hb = ((hour * 4f).roundToInt() and 0xFF).toLong() // 0.25h sky buckets
    val rb = ((weather.rain * 3f).roundToInt().coerceIn(0, 3)).toLong()
    val cb = ((weather.cloud * 3f).roundToInt().coerceIn(0, 3)).toLong()
    val fb = ((weather.fog * 3f).roundToInt().coerceIn(0, 3)).toLong()
    // STORM and RAIN can quantise to the same rain/cloud buckets during a transition.
    // Keep their different sky brightness out of one another's cache entries.
    val weatherKey = when (weather.weatherType) {
      com.example.game.world.WorldWeather.CLEAR -> 0L
      com.example.game.world.WorldWeather.CLOUDY -> 1L
      com.example.game.world.WorldWeather.RAIN -> 2L
      com.example.game.world.WorldWeather.STORM -> 3L
      com.example.game.world.WorldWeather.FOG -> 4L
    }
    val key = (hb * 64L + rb * 16L + cb * 4L + fb) * 8L + weatherKey
    val cached = skyBandCache[key]
    if (cached != null) return cached
    val lighting = com.example.game.world.WorldLighting.at(hour).withWeather(weather)
    val stops = lighting.skyColors
    val bands = FloatArray(36)
    for (b in 0 until 12) {
      val t = b / 12f
      val col = when {
        t < .333f -> androidx.compose.ui.graphics.lerp(stops[0], stops[1], t / .333f)
        t < .666f -> androidx.compose.ui.graphics.lerp(stops[1], stops[2], (t - .333f) / .333f)
        else -> androidx.compose.ui.graphics.lerp(stops[2], stops[3], (t - .666f) / .334f)
      }
      val k = lighting.skyBrightness
      bands[b * 3] = col.red * 255f * k
      bands[b * 3 + 1] = col.green * 255f * k
      bands[b * 3 + 2] = col.blue * 255f * k
    }
    skyBandCache[key] = bands
    return bands
  }

  private fun DrawScope.drawSkyBands() {
    val lighting = com.example.game.world.WorldEnvironment.lighting
    val bands = skyBandColors(lighting.hour, com.example.game.world.WorldEnvironment.weather)
    val bandH = size.height / 12f
    for (b in 0 until 12) {
      drawRect(
        color = Color(
          red = bands[b * 3].coerceIn(0f, 255f).toInt(),
          green = bands[b * 3 + 1].coerceIn(0f, 255f).toInt(),
          blue = bands[b * 3 + 2].coerceIn(0f, 255f).toInt(),
        ),
        Offset(0f, b * bandH),
        Size(size.width, bandH + 1f),
      )
    }
  }

  private fun celestialScreen(elevation: Float, dirX: Float): Pair<Float, Float> {
    // Horizon sits at ~0.62 of the frame; elevation 0..π/2 lifts the disc to the zenith.
    val yFrac = (0.62f - (elevation.coerceIn(0f, PI.toFloat() / 2) / (PI.toFloat() / 2)) * 0.55f)
      .coerceIn(0.05f, 0.62f)
    val xFrac = (0.5f + dirX * 0.30f).coerceIn(0.18f, 0.82f)
    return xFrac to yFrac
  }

  private fun DrawScope.drawCelestialDiscs() {
    val lighting = com.example.game.world.WorldEnvironment.lighting
    if (lighting.sunIntensity > 0.02f) {
      val (xFrac, yFrac) = celestialScreen(lighting.sunElevation, lighting.directionX)
      drawCelestialDisc(size.width * xFrac, size.height * yFrac, lighting.sunColor, lighting.sunIntensity)
    }
    if (lighting.moonIntensity > 0.02f) {
      val (xFrac, yFrac) = celestialScreen(lighting.moonElevation, lighting.directionX)
      drawCelestialDisc(size.width * xFrac, size.height * yFrac, lighting.moonColor, lighting.moonIntensity)
    }
  }

  private fun DrawScope.drawCelestialDisc(x: Float, y: Float, color: Color, intensity: Float) {
    // Glow halo + crisp disc. Alpha is bounded by intensity so noon never washes the frame.
    drawOval(
      color = color.copy(alpha = (0.12f * intensity).coerceAtMost(0.12f)),
      topLeft = Offset(x - 110f, y - 110f),
      size = Size(220f, 220f),
    )
    drawCircle(
      color = color.copy(alpha = (0.55f * intensity).coerceAtMost(0.55f)),
      radius = 24f,
      center = Offset(x, y),
    )
  }

  private fun DrawScope.drawHorizonHaze() {
    val lighting = com.example.game.world.WorldEnvironment.lighting
    if (lighting.hazeDensity <= 0.02f) return
    // One wide, flat haze band straddling the horizon (more at evening/night/fog/rain).
    drawOval(
      color = lighting.hazeColor.copy(alpha = (lighting.hazeDensity * 0.30f).coerceAtMost(0.30f)),
      topLeft = Offset(-size.width * 0.2f, size.height * 0.40f),
      size = Size(size.width * 1.4f, size.height * 0.24f),
    )
  }

  private fun DrawScope.drawDriftingSkyClouds(
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
  ) {
    val lighting = com.example.game.world.WorldEnvironment.lighting
    val weather = com.example.game.world.WorldEnvironment.weather
    val baseCloudCol = lighting.cloudColor

    // P26C: mây nằm trong layer bầu trời (screen-space, vùng trên cùng của khung), có parallax nhẹ
    // theo camera world. Không đặt mây ở world Y cố định ngoài vùng nhìn rồi chụp camera riêng.
    // Cloud cover theo thời tiết: CLEAR < CLOUDY < RAIN < STORM.
    val weatherType = weather.weatherType
    val cloudCover = when (weatherType) {
      com.example.game.world.WorldWeather.CLEAR -> 0.30f
      com.example.game.world.WorldWeather.CLOUDY -> 0.70f
      com.example.game.world.WorldWeather.RAIN -> 0.85f
      com.example.game.world.WorldWeather.STORM -> 1.00f
      else -> 0.30f
    }
    // STORM: mây tối hơn (màu xám-than), các thời tiết khác giữ cloudColor từ lighting model.
    val cloudCol = if (weatherType == com.example.game.world.WorldWeather.STORM) {
      Color(0xff4A5568)
    } else {
      baseCloudCol
    }
    // The original CLEAR value (~0.135) disappeared against the sky after compositing.
    // Keep the sparse CLEAR bank legible; weather still increases density/opacity.
    val alphaScale = (0.28f + lighting.cloudDensity * lighting.cloudBrightness * cloudCover * 0.95f).coerceIn(0f, 1f)

    val halfW = size.width / 2f
    // Parallax nhẹ: mây di chuyển theo camera nhưng chậm hơn đảo (0.15f hệ số).
    val parallaxX = camX * 0.15f

    // cloudSpecs là hằng số cấp object-level (đ khai báo một lần, không cấp phát mỗi frame).
    cloudSpecs.forEachIndexed { idx, anchor ->
      val cx = anchor.x
      val cy = anchor.y
      val scale = anchor.scale
      val parallax = anchor.parallax
      val depth = anchor.depth
      // Drift deterministic: sin(timeSec) với chu kỳ khác nhau theo layer, không giật khi zoom/quay lại.
      val drift = sin(timeSec * (0.12f + depth * 0.06f) + idx * 2.1f) * (35f + depth * 12f)
      // Parallax X: camera di chuyển làm mây dịch nhẹ ngược hướng.
      val screenX = halfW + cx - parallaxX * parallax + drift
      val screenY = cy
      val s = zoom * scale

      // Cull: không vẽ mây hoàn toàn ngoài màn hình (dành margin để mây vào khung mượt).
      if (screenX !in -300f..(size.width + 300f) || screenY !in -120f..(size.height * 0.72f)) return@forEachIndexed

      val layerAlpha = when (depth) {
        0 -> 0.68f // far bank
        1 -> 0.92f // middle volume
        else -> 0.70f // foreground wisps
      }

      drawCloudPuff(screenX, screenY, s, cloudCol, alphaScale * layerAlpha,
        depth = depth, storm = weatherType == com.example.game.world.WorldWeather.STORM)
    }
  }

  /** Far = soft bank, middle = stepped three-tone cumulus, near = narrow wind wisp.
   * All Paths are immutable object-level art; this hot loop allocates no Brush/Path/Bitmap. */
  private fun DrawScope.drawCloudPuff(
    cx: Float,
    cy: Float,
    s: Float,
    color: Color,
    alphaScale: Float,
    depth: Int,
    storm: Boolean,
  ) {
    val a = alphaScale
    val underside = if (storm) Color(0xFF233449) else lerp(color, Color(0xFF64748B), 0.42f)
    val rim = if (storm) Color(0xFF9FB6C9) else lerp(color, Color.White, 0.30f)
    when (depth) {
      0 -> {
        // A low-contrast, elongated bank behind the crisper cumulus, not stacked bubbles.
        drawOval(color.copy(alpha = 0.65f * a), Offset(cx - 100f * s, cy - 9f * s), Size(200f * s, 29f * s))
        drawOval(color.copy(alpha = 0.43f * a), Offset(cx - 67f * s, cy - 23f * s), Size(82f * s, 32f * s))
        drawOval(color.copy(alpha = 0.38f * a), Offset(cx + 7f * s, cy - 18f * s), Size(77f * s, 26f * s))
        drawOval(underside.copy(alpha = 0.18f * a), Offset(cx - 65f * s, cy + 7f * s), Size(126f * s, 11f * s))
      }
      1 -> withTransform({
        translate(cx, cy)
        scale(s, s, Offset.Zero)
      }) {
        drawPath(cumulusBody, color.copy(alpha = 0.85f * a))
        drawPath(cumulusUnder, underside.copy(alpha = 0.35f * a))
        drawPath(cumulusRim, rim.copy(alpha = 0.42f * a))
      }
      else -> withTransform({
        translate(cx, cy)
        scale(s, s, Offset.Zero)
      }) {
        drawPath(skyWisp, color.copy(alpha = 0.59f * a))
        drawRect(underside.copy(alpha = 0.25f * a), Offset(-65f, 6f), Size(125f, 5f))
        drawRect(rim.copy(alpha = 0.26f * a), Offset(-19f, -13f), Size(30f, 5f))
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
    drawPath(leftCliff, Color(0xFF2A2E3A))
    // Strata lines on left cliff for depth
    for (frac in listOf(0.25f, 0.55f, 0.80f)) {
      val ly = dropMain * frac
      drawPath(leftCliff, Color(0xFF1E2028).copy(alpha = 0.35f), style = Stroke(width = (1.0f*zoom).coerceAtLeast(0.7f)))
      // subtle horizontal strata via extra path would clutter — keep minimal
    }

    val centerChasm = Path().apply {
      moveTo(sx(1360f), sy(2370f, 0f))
      lineTo(sx(1860f), sy(2370f, 0f))
      lineTo(sx(1920f), sy(2370f, 0f) + dropMain * 0.82f)
      lineTo(sx(1610f), sy(2370f, 0f) + dropMain * 0.38f)
      lineTo(sx(1300f), sy(2370f, 0f) + dropMain * 0.84f)
      close()
    }
    drawPath(centerChasm, Color(0xFF0B0D1A))
    // Chasm depth gradient overlay
    drawPath(centerChasm, Color(0xFF1A1F33).copy(alpha = 0.22f))

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
    drawPath(rightCliff, Color(0xFF242735))

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
      drawPath(crag, if (cxWorld.first < 1600f) Color(0xFF3A3F4A) else Color(0xFF2B2E38))
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

    drawPath(rimShadowPath, Color(0xFF0F1A14))
    drawPath(rimBrightPath, Color(0xFF2A3D2B))
    // highlight lip on rim top edge
    drawPath(rimBrightPath, Color(0xFF3D5A36).copy(alpha = 0.45f), style = Stroke(width = (1.2f*zoom).coerceAtLeast(0.8f)))
    drawPath(rimBrightPath, Color(0xFF0A0C14), style = Stroke(width = (1.6f * zoom).coerceAtLeast(1f)))
  }

  private fun DrawScope.drawResidentialGardenCourtyards(
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
  ) {
    // 1. Warm Ochre Garden Soil Paths in the Residential Hedge Courtyard (x: 935..1255, y: 985..1285)
    val dirtColor = Color(0xFF3A352E)
    val dirtBorder = Color(0xFF2A2620)
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
      drawRect(Color(0xFF2F3D33), Offset(l, t), Size(sx(x1) - l, sy(y1, 0f) - t))
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
      drawRect(Color(0xFF1A2520), Offset(l, t + h * 0.45f), Size(w, h * 0.55f))
      drawRect(Color(0xFF2F4A2E), Offset(l, t), Size(w, h * 0.65f))
      drawRect(Color(0xFF3D5A36), Offset(l + 2f * zoom, t + 1f * zoom), Size((w - 4f * zoom).coerceAtLeast(1f), h * 0.25f))
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
      drawRect(Color(0xFF3A3F4A), Offset(l, t + h * 0.45f), Size(w, h * 0.55f))
      drawRect(Color(0xFF4E525E), Offset(l, t), Size(w, h * 0.50f))
      drawRect(Color(0xFF3A3530), Offset(l, t), Size(w, h), style = Stroke(width = (1.2f * zoom).coerceAtLeast(1f)))
    }
    drawLowGardenWall(675f, 1280f, 915f, 1292f)
    drawLowGardenWall(945f, 1280f, 1265f, 1292f)
    drawLowGardenWall(1255f, 965f, 1268f, 1292f)
  }

  private fun DrawScope.drawEasternSakuraLotusGarden(
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
    timeSec: Float,
  ) {
    // Eastern Sacred Sakura Reflection Pond & Lotus Terrace (x=2595, y=1320)
    val pondCx = sx(2595f)
    val pondCy = sy(1320f, 0f)
    val rx = 185f * zoom
    val ry = 132f * zoom * ObliqueCamera.OBLIQUE_Y_SCALE
    if (pondCx + rx < -40f || pondCx - rx > size.width + 40f || pondCy + ry < -40f || pondCy - ry > size.height + 40f) return

    // Carved white-marble reflection basin curb & gold inlay
    drawOval(Color(0xFF172234), Offset(pondCx - rx * 1.12f, pondCy - ry * 1.10f + 4f * zoom), Size(rx * 2.24f, ry * 2.20f))
    drawOval(Color(0xFFE5DFD3), Offset(pondCx - rx * 1.08f, pondCy - ry * 1.08f), Size(rx * 2.16f, ry * 2.16f))
    drawOval(Color(0xFFD4AF37), Offset(pondCx - rx * 1.02f, pondCy - ry * 1.02f), Size(rx * 2.04f, ry * 2.04f), style = Stroke(width = 2.0f * zoom))

    // Deep crystal-sapphire & turquoise water fill
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFF0EA5E9), Color(0xFF0284C7), Color(0xFF0F294A)),
        center = Offset(pondCx - rx * 0.15f, pondCy - ry * 0.15f),
        radius = rx.coerceAtLeast(10f),
      ),
      topLeft = Offset(pondCx - rx, pondCy - ry),
      size = Size(rx * 2f, ry * 2f),
    )

    // Animated water ripples, floating lotus lilypads, and swimming golden koi
    for (r in 0..2) {
      val phase = ((timeSec * 0.45f + r * 0.33f) % 1f)
      val rrx = rx * (0.25f + phase * 0.65f)
      val rry = ry * (0.25f + phase * 0.65f)
      drawOval(
        color = Color(0xFFBAE6FD).copy(alpha = (1f - phase) * 0.42f),
        topLeft = Offset(pondCx - rrx, pondCy - rry),
        size = Size(rrx * 2f, rry * 2f),
        style = Stroke(width = (1.4f * zoom).coerceAtLeast(0.8f)),
      )
    }

    // 5 Emerald lilypads with glowing pink sakura lotus blossoms
    val lilyOffsets = listOf(-0.48f to -0.25f, 0.42f to -0.32f, -0.32f to 0.38f, 0.36f to 0.34f, 0.05f to -0.08f)
    lilyOffsets.forEachIndexed { idx, (dx, dy) ->
      val lx = pondCx + dx * rx + sin(timeSec * 1.2f + idx) * 2.5f * zoom
      val ly = pondCy + dy * ry
      val padRx = 14f * zoom
      val padRy = 10f * zoom
      drawOval(Color(0xFF15803D), Offset(lx - padRx, ly - padRy), Size(padRx * 2f, padRy * 2f))
      drawOval(Color(0xFF4ADE80), Offset(lx - padRx * 0.75f, ly - padRy * 0.75f), Size(padRx * 1.5f, padRy * 1.5f))
      // Blooming lotus flower
      drawCircle(Color(0xFFF472B6), radius = 5.2f * zoom, center = Offset(lx, ly - 1.5f * zoom))
      drawCircle(Color(0xFFFBCFE8), radius = 3.2f * zoom, center = Offset(lx, ly - 2.0f * zoom))
      drawCircle(Color(0xFFFEF08A), radius = 1.6f * zoom, center = Offset(lx, ly - 2.0f * zoom))
    }

    // 3 Swimming Golden Koi fish
    for (k in 0..2) {
      val ang = timeSec * (0.85f + k * 0.22f) + k * 2.1f
      val kx = pondCx + cos(ang) * rx * (0.35f + k * 0.14f)
      val ky = pondCy + sin(ang) * ry * (0.35f + k * 0.14f)
      drawOval(Color(0xFFFB923C), Offset(kx - 5.5f * zoom, ky - 2.8f * zoom), Size(11f * zoom, 5.6f * zoom))
      drawCircle(Color(0xFFFFFBEB), radius = 2.2f * zoom, center = Offset(kx + 2f * zoom, ky))
    }
  }

  private fun DrawScope.drawNorthernTowerGateForecourt(
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
    timeSec: Float,
  ) {
    // Grand Obsidian & Starlight Zodiac Forecourt at the foot of the Tower Gate (x=1960, y=645)
    val fCx = sx(1960f)
    val fCy = sy(645f, 0f)
    val rx = 245f * zoom
    val ry = 165f * zoom * ObliqueCamera.OBLIQUE_Y_SCALE
    if (fCx + rx < -40f || fCx - rx > size.width + 40f || fCy + ry < -40f || fCy - ry > size.height + 40f) return

    val pulse = 0.80f + 0.20f * sin(timeSec * 2.6f)
    drawOval(Color(0xFF0D1326), Offset(fCx - rx * 1.04f, fCy - ry * 1.04f + 4f * zoom), Size(rx * 2.08f, ry * 2.08f))
    drawOval(Color(0xFF1E293B), Offset(fCx - rx, fCy - ry), Size(rx * 2f, ry * 2f))
    drawOval(Color(0xFFD4AF37), Offset(fCx - rx * 0.96f, fCy - ry * 0.96f), Size(rx * 1.92f, ry * 1.92f), style = Stroke(width = 2.2f * zoom))
    drawOval(Color(0xFF38BDF8).copy(alpha = 0.72f * pulse), Offset(fCx - rx * 0.76f, fCy - ry * 0.76f), Size(rx * 1.52f, ry * 1.52f), style = Stroke(width = 1.8f * zoom))
    drawOval(Color(0xFFA78BFA).copy(alpha = 0.58f * pulse), Offset(fCx - rx * 0.52f, fCy - ry * 0.52f), Size(rx * 1.04f, ry * 1.04f), style = Stroke(width = 1.4f * zoom))

    // 8 Starlight constellation spokes radiating from the portal forecourt
    for (i in 0 until 8) {
      val a = i * (Math.PI / 4.0)
      val c = cos(a).toFloat()
      val s = sin(a).toFloat()
      drawLine(
        color = Color(0xFF38BDF8).copy(alpha = 0.45f * pulse),
        start = Offset(fCx + c * rx * 0.25f, fCy + s * ry * 0.25f),
        end = Offset(fCx + c * rx * 0.92f, fCy + s * ry * 0.92f),
        strokeWidth = (1.5f * zoom).coerceAtLeast(0.8f),
      )
    }
  }

  private fun DrawScope.drawCelestialRiverChannel(
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
    timeSec: Float,
  ) {
    // Celestial Mana River winding from the Eastern Sacred Spring around the Plaza crescent down to the Southern Cliff Waterfalls
    val riverSpine = listOf(
      2595f to 1320f,
      2480f to 980f,
      2210f to 785f,
      1750f to 760f,
      1475f to 995f,
      1455f to 1340f,
      // The garden watercourse ends before the processional avenue.
    )

    fun drawWaterBranch(pts: List<Pair<Float, Float>>, widthWorld: Float) {
      for (i in 0 until pts.size - 1) {
        val (x0, y0) = pts[i]
        val (x1, y1) = pts[i + 1]
        val p0 = Offset(sx(x0), sy(y0, 0f))
        val p1 = Offset(sx(x1), sy(y1, 0f))
        val wPx = widthWorld * zoom
        // Carved stone canal banks + deep shadow lip
        drawLine(Color(0xFF141E30), p0, p1, strokeWidth = wPx * 1.32f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(Color(0xFFD6CFC2), p0, p1, strokeWidth = wPx * 1.18f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Deep sapphire outer water
        drawLine(Color(0xFF0369A1), p0, p1, strokeWidth = wPx * 0.96f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Luminous turquoise-cyan inner current
        drawLine(Color(0xFF0EA5E9), p0, p1, strokeWidth = wPx * 0.68f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Animated shimmering water caustics along the channel
        for (m in 0..3) {
          val t = ((timeSec * 0.38f + m * 0.25f + i * 0.17f) % 1f)
          val mx = p0.x + (p1.x - p0.x) * t
          val my = p0.y + (p1.y - p0.y) * t
          drawOval(
            color = Color(0xFFE0F2FE).copy(alpha = 0.55f),
            topLeft = Offset(mx - 8f * zoom, my - 3.5f * zoom),
            size = Size(16f * zoom, 7f * zoom),
          )
        }
      }
    }

    drawWaterBranch(riverSpine, 56f)
  }

  private fun DrawScope.drawArchedMarbleFootbridges(
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
  ) {
    // Sculpted 2.5D Arched White-Marble & Gold-Filigree Stone Footbridges where avenues cross the Celestial River
    val bridges = listOf(
      Triple(1455f, 1340f, true),  // Mid-West Avenue Bridge
      Triple(1545f, 825f, false),  // Northern Spine Bridge
    )
    bridges.forEach { (bxWorld, byWorld, horizontal) ->
      val cx = sx(bxWorld)
      val cy = sy(byWorld, 0f)
      val bridgeW = (if (horizontal) 96f else 112f) * zoom
      val bridgeH = (if (horizontal) 62f else 54f) * zoom
      if (cx + bridgeW < -20f || cx - bridgeW > size.width + 20f) return@forEach

      // Bridge shadow on water
      drawRoundRect(
        color = Color(0x88040B18),
        topLeft = Offset(cx - bridgeW * 0.54f, cy - bridgeH * 0.46f + 6f * zoom),
        size = Size(bridgeW * 1.08f, bridgeH * 0.96f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * zoom, 10f * zoom),
      )
      // Ivory marble deck
      drawRoundRect(
        color = Color(0xFFEAE3D2),
        topLeft = Offset(cx - bridgeW * 0.50f, cy - bridgeH * 0.50f),
        size = Size(bridgeW, bridgeH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * zoom, 8f * zoom),
      )
      // Royal crimson & gold carpet runner over bridge center
      drawRoundRect(
        color = Color(0xFF991B1B),
        topLeft = Offset(cx - bridgeW * 0.34f, cy - bridgeH * 0.34f),
        size = Size(bridgeW * 0.68f, bridgeH * 0.68f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * zoom, 4f * zoom),
      )
      drawRoundRect(
        color = Color(0xFFF5B841),
        topLeft = Offset(cx - bridgeW * 0.50f, cy - bridgeH * 0.50f),
        size = Size(bridgeW, bridgeH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * zoom, 8f * zoom),
        style = Stroke(width = 2.2f * zoom),
      )
      // Corner marble newel posts with golden orbs
      for (dx in listOf(-0.48f, 0.48f)) {
        for (dy in listOf(-0.48f, 0.48f)) {
          val px = cx + bridgeW * dx
          val py = cy + bridgeH * dy
          drawRect(Color(0xFFD6CFC2), Offset(px - 4.5f * zoom, py - 9f * zoom), Size(9f * zoom, 12f * zoom))
          drawCircle(Color(0xFFFBBF24), radius = 3.6f * zoom, center = Offset(px, py - 10f * zoom))
        }
      }
    }
  }

  private fun DrawScope.drawSouthernCliffWaterfalls(
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
    timeSec: Float,
  ) {
    // Twin Cascading Crystal Waterfalls plunging off the Southern Cliff at x=1315 and x=1895
    val fallSpots = listOf(1315f to 2372f, 1895f to 2372f)
    fallSpots.forEachIndexed { idx, (fxWorld, fyWorld) ->
      val cx = sx(fxWorld)
      val topY = sy(fyWorld, 0f)
      val fallW = 46f * zoom
      val fallH = 225f * zoom
      if (cx + fallW < -40f || cx - fallW > size.width + 40f || topY > size.height + 40f) return@forEachIndexed

      // Deep sapphire waterfall body
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0x440EA5E9), Color.Transparent),
          startY = topY,
          endY = topY + fallH,
        ),
        topLeft = Offset(cx - fallW * 0.5f, topY),
        size = Size(fallW, fallH),
      )
      // Animated white-water foam ribbons cascading downward
      for (s in 0..5) {
        val dropFrac = ((timeSec * 0.85f + s * 0.17f + idx * 0.3f) % 1f)
        val syFoam = topY + dropFrac * fallH * 0.88f
        val sxOff = (s - 2.5f) * 6.5f * zoom
        drawLine(
          color = Color(0xFFE0F2FE).copy(alpha = (1f - dropFrac) * 0.82f),
          start = Offset(cx + sxOff, syFoam),
          end = Offset(cx + sxOff, syFoam + 26f * zoom),
          strokeWidth = 3.2f * zoom,
          cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
      }
      // Top crest foam lip & bottom rising mist cloud
      drawOval(
        color = Color(0xFFF0F9FF).copy(alpha = 0.88f),
        topLeft = Offset(cx - fallW * 0.62f, topY - 6f * zoom),
        size = Size(fallW * 1.24f, 14f * zoom),
      )
      val mistPulse = 0.85f + 0.15f * sin(timeSec * 2.2f + idx)
      drawOval(
        brush = Brush.radialGradient(
          colors = listOf(Color(0x66E0F2FE), Color(0x2838BDF8), Color.Transparent),
          center = Offset(cx, topY + fallH * 0.78f),
          radius = (fallW * 1.6f * mistPulse).coerceAtLeast(8f),
        ),
        topLeft = Offset(cx - fallW * 1.6f, topY + fallH * 0.55f),
        size = Size(fallW * 3.2f, fallW * 1.5f),
      )
    }
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

    // 0. Royal Crimson & Gold Ceremonial Processional Carpet Runners leading to the Plaza
    val northCarpet = Path().apply {
      moveTo(sx(1912f), sy(640f, 0f))
      lineTo(sx(1968f), sy(640f, 0f))
      lineTo(sx(1972f), sy(940f, 0f))
      lineTo(sx(1908f), sy(940f, 0f))
      close()
    }
    drawPath(northCarpet, Color(0xFF881337))
    drawPath(northCarpet, Color(0xFFF5B841), style = Stroke(width = 2.0f * zoom))

    // 1. Stepped Outer Limestone & Royal Gold Curb + Deep Drop Shadow
    drawOval(Color(0xFF0A1020), Offset(cx - rx * 1.03f, cy - ry * 1.02f + 8f * zoom), Size(rx * 2.06f, ry * 2.04f))
    drawOval(Color(0xFFD6CFC2), Offset(cx - rx * 1.01f, cy - ry * 1.01f), Size(rx * 2.02f, ry * 2.02f))
    drawOval(Color(0xFFEAE3D2), Offset(cx - rx * 0.96f, cy - ry * 0.96f), Size(rx * 1.92f, ry * 1.92f))

    val plazaClip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx-rx*0.95f,cy-ry*0.95f,cx+rx*0.95f,cy+ry*0.95f)) }
    clipPath(plazaClip) {
      val tiles = FantasyHd128Atlas.hdSanctuaryStoneTiles
      val tw = (64 * zoom).roundToInt() + 1
      val th = (64 * ObliqueCamera.OBLIQUE_Y_SCALE * zoom).roundToInt() + 1
      for (ty in 10..28) for (tx in 21..39) {
        val hash = ((tx * 73856093 xor ty * 19349663) and 0x7fffffff)
        drawImage(
          image = ArtDepthAssets.tile(tiles, tx, ty),
          dstOffset = IntOffset(sx(tx * 64f).roundToInt(), sy(ty * 64f, 0f).roundToInt()),
          dstSize = IntSize(tw, th),
          filterQuality = FilterQuality.None,
        )
      }
      // HD-128 Central Astrolabe Sunburst Medallion
      val medallion = FantasyHd128Atlas.hdPlazaCenterMedallion
      val medW = (456f * zoom).roundToInt().coerceAtLeast(8)
      val medH = (310f * zoom).roundToInt().coerceAtLeast(8)
      drawImage(
        image = medallion,
        dstOffset = IntOffset((cx - medW / 2f).roundToInt(), (cy - medH / 2f).roundToInt()),
        dstSize = IntSize(medW, medH),
        alpha = 0.92f,
        filterQuality = FilterQuality.None,
      )
    }

    // 2. Concentric Carved Royal Alabaster, Sapphire Lapis & Gold-Filigree Mosaic Rings
    val ringFractions = listOf(1.01f, 0.95f, 0.72f, 0.62f, 0.44f, 0.34f, 0.22f)
    drawOval(Color(0xFF1E3A8A).copy(alpha = 0.22f), Offset(cx - rx * 0.72f, cy - ry * 0.72f), Size(rx * 1.44f, ry * 1.44f))
    drawOval(Color(0xFFFef3c7).copy(alpha = 0.20f), Offset(cx - rx * 0.62f, cy - ry * 0.62f), Size(rx * 1.24f, ry * 1.24f))
    drawOval(Color(0xFF0284C7).copy(alpha = 0.24f), Offset(cx - rx * 0.34f, cy - ry * 0.34f), Size(rx * 0.68f, ry * 0.68f))
    drawOval(Color(0xFFFDE68A).copy(alpha = 0.30f), Offset(cx - rx * 0.22f, cy - ry * 0.22f), Size(rx * 0.44f, ry * 0.44f))

    // Radial Gold-Inlaid Stone Joints (16 divisions across outer & mid bands)
    for (i in 0 until 16) {
      val ang = i * (Math.PI * 2.0 / 16.0)
      val cA = cos(ang).toFloat()
      val sA = sin(ang).toFloat()
      drawLine(
        color = if (i % 2 == 0) Color(0xFFD4AF37).copy(alpha = 0.62f) else Color(0xFF64748B).copy(alpha = 0.45f),
        start = Offset(cx + cA * rx * 0.22f, cy + sA * ry * 0.22f),
        end = Offset(cx + cA * rx * 0.95f, cy + sA * ry * 0.95f),
        strokeWidth = (1.5f * zoom).coerceAtLeast(0.8f),
      )
    }

    ringFractions.forEachIndexed { idx, frac ->
      drawOval(
        color = when (idx) {
          0 -> Color(0xFF1E293B)
          1, 3, 5 -> Color(0xFFD4AF37).copy(alpha = 0.82f)
          else -> Color(0xFF38BDF8).copy(alpha = 0.55f)
        },
        topLeft = Offset(cx - rx * frac, cy - ry * frac),
        size = Size(rx * frac * 2f, ry * frac * 2f),
        style = Stroke(width = (if (idx <= 1) 2.4f else 1.7f) * zoom),
      )
    }

    // 3. Sculpted 2.5D Central Astral Fountain Basin & Floating Crystal Sundial Core + Four Elemental Rune Circles
    val pulse = 0.78f + 0.22f * sin(timeSec * 2.4f)
    val hoverY = sin(timeSec * 2.8f) * 4.5f * zoom
    // Fountain marble basin rim & glowing azure water pool
    drawOval(
      color = Color(0xFF0F172A).copy(alpha = 0.55f),
      topLeft = Offset(cx - rx * 0.155f, cy - ry * 0.150f + 4f * zoom),
      size = Size(rx * 0.31f, ry * 0.30f),
    )
    drawOval(
      color = Color(0xFFEAE3D2),
      topLeft = Offset(cx - rx * 0.15f, cy - ry * 0.15f),
      size = Size(rx * 0.30f, ry * 0.30f),
    )
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFF7DD3FC), Color(0xFF0284C7), Color(0xFF1E3A8A)),
        center = Offset(cx, cy),
        radius = (rx * 0.13f).coerceAtLeast(6f),
      ),
      topLeft = Offset(cx - rx * 0.13f, cy - ry * 0.13f),
      size = Size(rx * 0.26f, ry * 0.26f),
    )
    drawOval(
      color = Color(0xFFF5B841),
      topLeft = Offset(cx - rx * 0.15f, cy - ry * 0.15f),
      size = Size(rx * 0.30f, ry * 0.30f),
      style = Stroke(width = 2.2f * zoom),
    )
    // Floating 3D Astral Crystal Spire rising from the center of the fountain
    val crystalBaseY = cy - 4f * zoom + hoverY
    val crystalTopY = cy - 44f * zoom + hoverY
    val crystalW = 14f * zoom
    val crystalPath = Path().apply {
      moveTo(cx, crystalTopY)
      lineTo(cx + crystalW, crystalBaseY - 14f * zoom)
      lineTo(cx, crystalBaseY)
      lineTo(cx - crystalW, crystalBaseY - 14f * zoom)
      close()
    }
    drawPath(crystalPath, Color(0xFF38BDF8).copy(alpha = 0.90f))
    drawPath(crystalPath, Color(0xFFE0F2FE), style = Stroke(width = 1.6f * zoom))
    drawCircle(Color(0xFFFFFBEB).copy(alpha = pulse), radius = 4.5f * zoom, center = Offset(cx, crystalTopY + 12f * zoom))

    data class ElementalRune(val dxFrac: Float, val dyFrac: Float, val outerCol: Color, val innerCol: Color)
    val runes = listOf(
      ElementalRune(0f, -0.78f, Color(0xFFF472B6), Color(0xFFFCE7F3)), // North: Sakura Rose
      ElementalRune(-0.78f, 0f, Color(0xFFEAB308), Color(0xFFFEF9C3)), // West: Solar Gold
      ElementalRune(0.78f, 0f, Color(0xFFF59E0B), Color(0xFFFEF3C7)),  // East: Amber Star
      ElementalRune(0f, 0.78f, Color(0xFF38BDF8), Color(0xFFE0F2FE)),  // South: Astral Azure
    )

    val runeRx = rx * 0.145f
    val runeRy = ry * 0.145f
    runes.forEach { r ->
      val rCx = cx + r.dxFrac * rx
      val rCy = cy + r.dyFrac * ry
      drawOval(
        color = r.innerCol.copy(alpha = 0.88f * pulse),
        topLeft = Offset(rCx - runeRx, rCy - runeRy),
        size = Size(runeRx * 2f, runeRy * 2f),
      )
      drawOval(
        color = r.outerCol.copy(alpha = 0.92f),
        topLeft = Offset(rCx - runeRx, rCy - runeRy),
        size = Size(runeRx * 2f, runeRy * 2f),
        style = Stroke(width = 2.4f * zoom),
      )
      drawOval(
        color = r.outerCol.copy(alpha = 0.78f),
        topLeft = Offset(rCx - runeRx * 0.65f, rCy - runeRy * 0.65f),
        size = Size(runeRx * 1.30f, runeRy * 1.30f),
        style = Stroke(width = 1.5f * zoom),
      )
      for (k in 0 until 6) {
        val a1 = k * (Math.PI / 3.0)
        val a2 = (k + 2) * (Math.PI / 3.0)
        drawLine(
          color = r.outerCol.copy(alpha = 0.72f),
          start = Offset(rCx + cos(a1).toFloat() * runeRx * 0.65f, rCy + sin(a1).toFloat() * runeRy * 0.65f),
          end = Offset(rCx + cos(a2).toFloat() * runeRx * 0.65f, rCy + sin(a2).toFloat() * runeRy * 0.65f),
          strokeWidth = (1.2f * zoom).coerceAtLeast(0.7f),
        )
      }
    }

    // 4. Grand Diagonal 8-Step Royal Marble & Carpeted Staircase Linking SE Plaza Rim -> Classical Amphitheatre Stage
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
      drawPath(stepPath, if (s % 2 == 0) Color(0xFFD6CFC2) else Color(0xFFBDB5A6))
      drawLine(Color(0xFF5C5449), Offset(sx(lx1), sy(ly1, 0f)), Offset(sx(rx1), sy(ry1, 0f)), strokeWidth = (1.6f * zoom).coerceAtLeast(0.9f))
    }
    // Sloped stone cheek balustrades with golden trim flanking the grand plaza-to-amphitheatre steps
    drawLine(Color(0xFFEAE3D2), Offset(sx(2112f), sy(1688f, 10f)), Offset(sx(2258f), sy(1836f, 4f)), strokeWidth = 6.0f * zoom)
    drawLine(Color(0xFFEAE3D2), Offset(sx(2258f), sy(1572f, 10f)), Offset(sx(2402f), sy(1720f, 4f)), strokeWidth = 6.0f * zoom)
    drawLine(Color(0xFFD4AF37), Offset(sx(2112f), sy(1688f, 12f)), Offset(sx(2258f), sy(1836f, 6f)), strokeWidth = 2.0f * zoom)
    drawLine(Color(0xFFD4AF37), Offset(sx(2258f), sy(1572f, 12f)), Offset(sx(2402f), sy(1720f, 6f)), strokeWidth = 2.0f * zoom)
  }

  private fun DrawScope.drawSouthernProcessionCarpet(
    paths: List<PathSegment>, sx: (Float) -> Float, sy: (Float, Float) -> Float,
    zoom: Float, startY: Float, endY: Float,
  ) {
    val halfWidth = SanctuaryEntranceApproach.HALF_CARPET_WIDTH
    // Retain the road's endpoint bend before extending straight through the gate.
    val ys = listOf(startY, 2240f.coerceIn(startY, endY), endY).distinct()
    val runner = Path().apply {
      ys.forEachIndexed { index, y ->
        val x = SanctuaryEntranceApproach.centerX(y, paths) - halfWidth
        if (index == 0) moveTo(sx(x), sy(y, 0f)) else lineTo(sx(x), sy(y, 0f))
      }
      ys.asReversed().forEach { y ->
        lineTo(sx(SanctuaryEntranceApproach.centerX(y, paths) + halfWidth), sy(y, 0f))
      }
      close()
    }
    drawPath(runner, Color(0xFF881337))
    drawPath(runner, Color(0xFFF5B841), style = Stroke(width = 2f * zoom))
  }

  private fun DrawScope.drawSouthernMainGateAndBridges(
    paths: List<PathSegment>,
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
    drawRect(Color(0xFF2E3038), Offset(platL, platB), Size(platW, 42f * zoom))
    drawRect(Color(0xFF45413B), Offset(platL, platB), Size(platW, 42f * zoom), style = Stroke(width = 1.6f * zoom))
    drawRect(Color(0xFF3A3D4A), Offset(platL, platT), Size(platW, platH))
    drawRect(Color(0xFF1A1C24), Offset(platL, platT), Size(platW, platH), style = Stroke(width = 2f * zoom))

    // Stone slab grid lines on gate platform
    for (gx in 1430..1790 step 60) {
      val lx = sx(gx.toFloat())
      drawLine(Color(0xFFA8A296), Offset(lx, platT), Offset(lx, platB), strokeWidth = 1.2f * zoom)
    }
    for (gy in 2245..2365 step 40) {
      val ly = sy(gy.toFloat(), 0f)
      drawLine(Color(0xFFA8A296), Offset(platL, ly), Offset(platR, ly), strokeWidth = 1.2f * zoom)
    }

    // Continue the SAME road-aligned runner across the opaque gatehouse deck.
    drawSouthernProcessionCarpet(paths, sx, sy, zoom, 2175f, 2390f)

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
        color = if (s % 2 == 0) Color(0xFF4A4E5E) else Color(0xFF343845),
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
      drawRect(Color(0xFF343845), Offset(bx - 14f * zoom, by - 30f * zoom), Size(28f * zoom, 10f * zoom))
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

      drawRect(Color(0xFF1E1A16), Offset(bl - 3f * zoom, bt), Size(bw + 6f * zoom, bh + 5f * zoom))
      drawRect(Color(0xFF4A3A2E), Offset(bl, bt), Size(bw, bh))
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
    val innerSize = Size(innerFaceW, (base1Y - top0Y).coerceAtLeast(4f))
    drawRect(
      // Same repaint as drawFortressWall: this segment is the side wall of the same ring.
      color = if (isWestSide) Color(0xFFB9AF9C) else Color(0xFFAFA592),
      topLeft = Offset(innerLeft, top0Y),
      size = innerSize,
    )
    // See drawFortressWall: geometry drawn in code needs its own night response, otherwise
    // it stays bright while the sky and grass go night-blue.
    with(HdrLightResponse) {
      drawGeometryNightAbsorption(Rect(Offset(innerLeft, top0Y), innerSize), 185f)
    }
    drawRect(
      color = Color(0xFF6F6656),
      topLeft = Offset(innerLeft, top0Y),
      size = Size(innerFaceW, (base1Y - top0Y).coerceAtLeast(4f)),
      style = Stroke(width = (1.3f * zoom).coerceAtLeast(0.8f)),
    )

    // Sunlit stone top rampart walkway
    val walkSize = Size(walkHalfW * 2f, (top1Y - top0Y).coerceAtLeast(4f))
    drawRect(
      color = Color(0xFFF2EAD9),
      topLeft = Offset(cx - walkHalfW, top0Y),
      size = walkSize,
    )
    with(HdrLightResponse) {
      drawGeometryNightAbsorption(Rect(Offset(cx - walkHalfW, top0Y), walkSize), 232f)
    }
    drawRect(
      color = Color(0xFF6F6656),
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
    // P27B-V5 CP5 (debt A): this ring was the last dark surface on the island - a charcoal
    // wall (0xFF3A3F4A / 0xFF323540 / 0xFF4A4E5E) around a rim whose gate, towers and
    // retaining walls are all ivory limestone at ~152 luma. From z0.36 up it was the single
    // clearest "two different games" tell, and it is drawn here in code rather than from an
    // atlas entry, which is why repainting an asset did not change it.
    // Repainted to the P27B-V5 limestone family; geometry is untouched.
    drawPath(extFace, if (x0 < 1600f) Color(0xFFB9AF9C) else Color(0xFFAFA592))
    // Night: this face is code, not a sprite, so it needs its own absorption or it stays
    // bright while the sky and grass go night-blue (debt C, resurfacing on the ring).
    with(HdrLightResponse) {
      drawGeometryNightAbsorption(extFace, 185f)
    }

    for (c in 1..4) {
      val f = c / 5f
      val ly0 = top0Y + (base0Y + cliffRetainingDrop - top0Y) * f
      val ly1 = top1Y + (base1Y + cliffRetainingDrop - top1Y) * f
      // Course lines: warm mortar, lighter than the face so the ashlar reads as masonry.
      drawLine(Color(0xFF8E8474), Offset(base0X, ly0), Offset(base1X, ly1), strokeWidth = (1.1f * zoom).coerceAtLeast(0.7f))
    }
    for (i in 1 until 6) {
      val t = i / 6f
      val px = base0X + (base1X - base0X) * t
      val pyTop = top0Y + (top1Y - top0Y) * t
      val pyBot = base0Y + (base1Y - base0Y) * t + cliffRetainingDrop
      drawLine(Color(0xFFCFC5B0), Offset(px, pyTop), Offset(px, pyBot), strokeWidth = 3.5f * zoom)
    }
    drawPath(extFace, Color(0xFF6F6656), style = Stroke(width = (1.8f * zoom).coerceAtLeast(1f)))

    // 2. Sunlit Top Rampart Walkway
    val walkwayPath = Path().apply {
      moveTo(base0X, top0Y - walkwayThick)
      lineTo(base1X, top1Y - walkwayThick)
      lineTo(base1X, top1Y)
      lineTo(base0X, top0Y)
      close()
    }
    // Walkway is the sunlit top surface: brightest ivory in the ring.
    drawPath(walkwayPath, Color(0xFFF2EAD9))
    with(HdrLightResponse) { drawGeometryNightAbsorption(walkwayPath, 232f) }
    drawPath(walkwayPath, Color(0xFF6F6656), style = Stroke(width = (1.6f * zoom).coerceAtLeast(1f)))

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
      drawPath(merlon, Color(0xFFDCD3C0))
      with(HdrLightResponse) { drawGeometryNightAbsorption(merlon, 220f) }
      drawPath(merlon, Color(0xFF6F6656), style = Stroke(width = (1.3f * zoom).coerceAtLeast(0.8f)))
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
    // Same ring, front segments: repainted to the P27B-V5 limestone family (see
    // drawFortressWall). Charcoal here was what still showed as a dark band after the
    // atlas art was replaced, because this face is drawn in code, not from a PNG.
    drawPath(facePath, Color(0xFFB9AF9C))

    // Recessed architectural wall panels & horizontal ashlar courses
    for (c in 1..4) {
      val f = c / 5f
      drawLine(
        color = Color(0xFF8E8474),
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
      drawLine(Color(0xFFCFC5B0), Offset(px, pyTop), Offset(px, pyBot), strokeWidth = 4.2f * zoom)
    }

    // 2. Sunlit Top Stone Parapet Walkway Ribbon
    val walkwayPath = Path().apply {
      moveTo(base0X, top0Y - walkwayThick)
      lineTo(base1X, top1Y - walkwayThick)
      lineTo(base1X, top1Y)
      lineTo(base0X, top0Y)
      close()
    }
    drawPath(walkwayPath, Color(0xFFF2EAD9))
    drawPath(facePath, Color(0xFF6F6656), style = Stroke(width = (1.8f * zoom).coerceAtLeast(1f)))
    drawPath(walkwayPath, Color(0xFF6F6656), style = Stroke(width = (1.8f * zoom).coerceAtLeast(1f)))

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
      drawPath(merlon, Color(0xFFDCD3C0))
      with(HdrLightResponse) { drawGeometryNightAbsorption(merlon, 220f) }
      drawPath(merlon, Color(0xFF6F6656), style = Stroke(width = (1.3f * zoom).coerceAtLeast(0.8f)))
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

  private fun DrawScope.drawSkyRays() {
    val lighting = com.example.game.world.WorldEnvironment.lighting
    if (lighting.rayAlpha <= 0.02f) return
    // Warm gold sunbeams by day, cool moonlight at night. Solid rects with a bounded
    // alpha and a vertical fade (bright at the top, fading out before the world plane):
    // no gradient brush allocation per frame and no screen-wide flood of the lower frame.
    withTransform({
      rotate(degrees = -34f, pivot = Offset(0f, 0f))
    }) {
      val rayCol = lighting.rayColor
      val aFull = lighting.rayAlpha.coerceAtMost(0.14f)
      val aMid = aFull * 0.5f
      val aFaint = aFull * 0.22f
      val raySpecs = listOf(
        80f to 130f,
        290f to 95f,
        490f to 155f,
        760f to 85f,
      )
      raySpecs.forEach { (offsetX, widthPx) ->
        drawRect(color = rayCol.copy(alpha = aFull), topLeft = Offset(offsetX, -80f), size = Size(widthPx, size.height * 0.42f))
        drawRect(color = rayCol.copy(alpha = aMid), topLeft = Offset(offsetX, size.height * 0.34f), size = Size(widthPx, size.height * 0.41f))
        drawRect(color = rayCol.copy(alpha = aFaint), topLeft = Offset(offsetX, size.height * 0.70f), size = Size(widthPx, size.height * 0.75f))
      }
    }
  }

  private fun DrawScope.drawSanctuaryAmbientOcclusion(
    sx: (Float) -> Float,
    sy: (Float, Float) -> Float,
    zoom: Float,
  ) {
    with(SanctuaryDarkFantasyShaders) {
      // Deep AO under northern fortress wall + stone crevice
      val nL = sx(980f)
      val nR = sx(2240f)
      val nT = sy(340f, 0f)
      drawRect(Color(0xFF060A14).copy(alpha = 0.38f), Offset(nL, nT + 6f*zoom), Size(nR-nL, 24f*zoom))
      drawStoneCreviceAO(cx = (nL+nR)/2, cy = nT+9f*zoom, rx=(nR-nL)*0.45f, ry=8f*zoom, alpha=0.26f)
      // Deep AO ring around plaza outer rim
      val pCx = sx(SanctuaryIslandGeometry.PLAZA_CENTER_X)
      val pCy = sy(SanctuaryIslandGeometry.PLAZA_CENTER_Y, 0f)
      val rx = SanctuaryIslandGeometry.PLAZA_RADIUS * zoom
      val ry = rx * ObliqueCamera.OBLIQUE_Y_SCALE
      drawOval(
        color = Color(0xFF060A14).copy(alpha = 0.32f),
        topLeft = Offset(pCx - rx*1.03f, pCy - ry*1.03f),
        size = Size(rx*2.06f, ry*2.06f),
        style = Stroke(width = 7f * zoom),
      )
      // AO under southern gate platform
      val gL = sx(1380f); val gR = sx(1840f); val gB = sy(2390f, 0f)
      drawOval(Color(0xFF060A14).copy(alpha = 0.36f), Offset(gL - 12f*zoom, gB - 6f*zoom), Size((gR-gL)+24f*zoom, 26f*zoom))
      // Building contact belongs to drawProp at each live obstacle anchor.
      // A second hardcoded AO list drifted away from moved doors/facades.
      // Tree canopy AO — oak/pine/sakura clusters produce rich darkening under foliage mass
      drawTreeCanopyAO(sx(750f), sy(920f,0f), 34f*zoom, 12f*zoom, alpha=0.28f)
      drawTreeCanopyAO(sx(1280f), sy(640f,0f), 28f*zoom, 10f*zoom, alpha=0.26f)
      drawTreeCanopyAO(sx(2645f), sy(890f,0f), 36f*zoom, 13f*zoom, alpha=0.28f)
      // Dithered shadow transition for dark stone near plaza staircase
      drawDitheredShadow(sx(2180f), sy(1760f,0f), 68f*zoom, 22f*zoom, alpha=0.22f)
    }
  }

  private fun DrawScope.drawSanctuaryLocalLights(
    sx:(Float)->Float,sy:(Float,Float)->Float,zoom:Float,timeSec:Float,
  ) {
    if (DepthPresentation.quality == DepthPresentation.Quality.OFF || zoom <= 0f) return
    val exposure = HdrLightResponse.exposureBalance()
    val flicker = .94f + .06f * sin(timeSec*3.1f + 1.7f)
    with(HdrLightResponse) {
      // P24: ONE controlled focal point on the plaza, on the .72 ground plane. The
      // exposure balance raises it at night without lifting the whole scene.
      val px = sx(SanctuaryIslandGeometry.PLAZA_CENTER_X)
      val py = sy(SanctuaryIslandGeometry.PLAZA_CENTER_Y, 0f)
      drawHdrStamp(
        Offset(px, py), 120f*zoom, 120f*zoom*ObliqueCamera.OBLIQUE_Y_SCALE,
        HdrLightResponse.Tint.MOON, (.160f*exposure).coerceAtMost(.30f),
      )
      // Tower Gate is the strongest cold source; its pool reaches the monolith stairs.
      // The emissive core/rift halo itself is drawn by drawProp AFTER the monument sprite.
      val gx = sx(1960f); val gy = sy(585f, 0f)
      drawHdrStamp(
        Offset(gx, gy + 12f*zoom), 118f*zoom, 118f*zoom*ObliqueCamera.OBLIQUE_Y_SCALE,
        HdrLightResponse.Tint.AETHER, (.13f*exposure*flicker).coerceAtMost(.22f),
      )
      if (DepthPresentation.quality == DepthPresentation.Quality.BALANCED) {
        // Two warm posts near the southern approach keep the amber/cold hierarchy.
        for (i in 0..1) {
          val wx = if (i == 0) 1490f else 1730f
          drawHdrStamp(
            Offset(sx(wx), sy(2155f, 0f)), 62f*zoom, 62f*zoom*ObliqueCamera.OBLIQUE_Y_SCALE,
            HdrLightResponse.Tint.AMBER, (.10f*exposure*flicker).coerceAtMost(.22f),
          )
        }
      }
    }
  }
}
