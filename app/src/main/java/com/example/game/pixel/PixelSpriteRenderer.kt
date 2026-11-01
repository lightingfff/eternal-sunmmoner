package com.example.game.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.data.model.HeroClassType
import com.example.data.model.IslandExpansionMilestone
import com.example.game.map.BiomeTheme
import com.example.game.map.EnemyArchetype
import com.example.game.map.ObliqueCamera
import com.example.game.map.PathSegment
import com.example.game.map.PropKind
import com.example.game.map.SanctuaryZoneZone
import com.example.game.map.TerrainPatch
import com.example.game.map.WorldObstacle
import com.example.game.tower.TowerFloorCatalog
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * True Oblique 3/4 (~43° camera elevation) 2D Pixel-Art World, Architecture, Character & Lighting Renderer.
 * Renders foreshortened ground planes simultaneously with full-height vertical building facades,
 * pitched tiled roofs, 2.5D side walls, directional cast shadows, and Y-sorted object occlusion.
 */
object PixelSpriteRenderer {

  private fun distanceToSegment(px: Float, py: Float, s: PathSegment): Float {
    val vx = s.endX - s.startX
    val vy = s.endY - s.startY
    val lenSq = vx * vx + vy * vy
    if (lenSq <= 0.001f) return hypot(px - s.startX, py - s.startY)
    val t = (((px - s.startX) * vx + (py - s.startY) * vy) / lenSq).coerceIn(0f, 1f)
    val projX = s.startX + t * vx
    val projY = s.startY + t * vy
    return hypot(px - projX, py - projY)
  }

  private fun DrawScope.drawForeshortenedGlow(
    cx: Float,
    cy: Float,
    radiusX: Float,
    innerColor: Color,
    midColor: Color? = null,
    ellipseRatio: Float = ObliqueCamera.GROUND_ELLIPSE_RATIO,
  ) {
    if (radiusX <= 2f) return
    if (cx + radiusX < -40f || cx - radiusX > size.width + 40f) return
    val ry = radiusX * ellipseRatio
    if (cy + ry < -40f || cy - ry > size.height + 40f) return
    withTransform({
      scale(scaleX = 1f, scaleY = ellipseRatio, pivot = Offset(cx, cy))
    }) {
      val stops = if (midColor != null) {
        listOf(innerColor, midColor, Color.Transparent)
      } else {
        listOf(innerColor, Color.Transparent)
      }
      drawCircle(
        brush = Brush.radialGradient(colors = stops, center = Offset(cx, cy), radius = radiusX),
        radius = radiusX,
        center = Offset(cx, cy),
      )
    }
  }

  fun DrawScope.drawWorldBackground(
    biome: BiomeTheme,
    worldWidth: Float,
    worldHeight: Float,
    wallMargin: Float,
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
    patches: List<TerrainPatch>,
    paths: List<PathSegment>,
    unlockedExpansions: Set<IslandExpansionMilestone> = emptySet(),
  ) {
    if (biome == BiomeTheme.SANCTUARY_ASTRAL) {
      with(SanctuaryWorldRenderer) {
        drawSanctuaryFloatingWorld(
          camX = camX,
          camY = camY,
          zoom = zoom,
          timeSec = timeSec,
          paths = paths,
          unlockedExpansions = unlockedExpansions,
        )
      }
      return
    }

    val halfW = size.width / 2f
    val halfH = size.height / 2f
    fun sx(wx: Float) = ObliqueCamera.worldToScreenX(wx, camX, zoom, halfW)
    fun sy(wy: Float, z: Float = 0f) = ObliqueCamera.worldToScreenY(wy, camY, zoom, halfH, z)

    val voidBg = when (biome) {
      BiomeTheme.RESTAURANT_INTERIOR, BiomeTheme.DORMITORY_INTERIOR -> Color(0xFF120B08)
      BiomeTheme.TRAINING_HALL_INTERIOR -> Color(0xFF141311)
      BiomeTheme.ARMORY_INTERIOR -> Color(0xFF0A0F1A)
      BiomeTheme.FIRST_FIELD_MEADOW -> Color(0xFF0E2228)
      BiomeTheme.ELDERWOOD_FOREST -> Color(0xFF08161E)
      else -> Color(0xFF0F2B36)
    }
    drawRect(color = voidBg)

    val mapLeft = sx(wallMargin)
    val mapRight = sx(worldWidth - wallMargin)
    val wallBaseY = sy(wallMargin)
    val southEdgeY = sy(worldHeight - wallMargin)

    // 2. 3/4 Elevated Northern Architectural Facade & Vista Horizon
    if (biome == BiomeTheme.RESTAURANT_INTERIOR) {
      // Reference 2: Tall 3/4 Half-Timbered Tavern Back Wall, Upper Wooden Mezzanine Balcony & Banners!
      val wallHeightPx = 118f * zoom
      val wallTop = wallBaseY - wallHeightPx
      val roomL = sx(wallMargin - 14f)
      val roomR = sx(worldWidth - wallMargin + 14f)
      val roomW = (roomR - roomL).coerceAtLeast(1f)

      // Plaster & dark oak 3/4 back wall rising vertically above floor line
      drawRect(Color(0xFF8F745C), Offset(roomL, wallTop), Size(roomW, wallHeightPx))
      // Upper mezzanine gallery recess & oak balustrade rail
      drawRect(Color(0xFF361F14), Offset(roomL, wallTop), Size(roomW, wallHeightPx * 0.36f))
      drawRect(Color(0xFF7D4E2D), Offset(roomL, wallTop + wallHeightPx * 0.36f), Size(roomW, 9f * zoom))
      drawRect(Color(0xFF422617), Offset(roomL, wallBaseY - 7f * zoom), Size(roomW, 7f * zoom))

      var postX = wallMargin + 36f
      var idx = 0
      while (postX < worldWidth - wallMargin) {
        val px = sx(postX)
        drawRect(Color(0xFF472A18), Offset(px - 6f * zoom, wallTop), Size(12f * zoom, wallHeightPx))
        drawRect(Color(0xFF6B4228), Offset(px - 4f * zoom, wallTop), Size(3f * zoom, wallHeightPx))
        if (idx % 2 == 0) {
          // Sage-emerald heraldic tavern banner
          drawRect(Color(0xFF2F6B48), Offset(px + 24f * zoom, wallTop + wallHeightPx * 0.40f), Size(30f * zoom, 42f * zoom))
          drawRect(Color(0xFFD99B26), Offset(px + 24f * zoom, wallTop + wallHeightPx * 0.40f), Size(30f * zoom, 4f * zoom))
        } else {
          // Warm tavern wall sconce casting golden cone down wall & onto floor!
          val flicker = 0.34f + 0.08f * sin(timeSec * 8f + idx)
          val sconceCenter = Offset(px + 36f * zoom, wallTop + wallHeightPx * 0.56f)
          drawCircle(
            brush = Brush.radialGradient(
              listOf(Color(0xFFFBBF24).copy(alpha = flicker), Color.Transparent),
              center = sconceCenter,
              radius = 105f * zoom,
            ),
            radius = 105f * zoom,
            center = sconceCenter,
          )
          drawRect(Color(0xFF33261E), Offset(sconceCenter.x - 3f * zoom, sconceCenter.y), Size(6f * zoom, 9f * zoom))
          drawCircle(Color(0xFFFEF08A), radius = 4.5f * zoom, center = sconceCenter)
        }
        postX += 148f
        idx++
      }
    } else if (biome.isInteriorBiome) {
      with(InteriorWorldRenderer) {
        drawInteriorBackFacade(
          biome = biome,
          worldWidth = worldWidth,
          wallMargin = wallMargin,
          camX = camX,
          camY = camY,
          zoom = zoom,
          timeSec = timeSec,
        )
      }
    } else {
      val vistaTop = (wallBaseY - 260f * zoom).coerceAtLeast(0f)
      if (wallBaseY > 0f) {
        val skyBrush = when (biome) {
          BiomeTheme.SANCTUARY_ASTRAL -> Brush.verticalGradient(
            listOf(Color(0xFF09222D), Color(0xFF133F4E), Color(0xFF1E586A)),
            startY = vistaTop,
            endY = wallBaseY,
          )
          BiomeTheme.FIRST_FIELD_MEADOW -> Brush.verticalGradient(
            listOf(Color(0xFF102536), Color(0xFF1E444D), Color(0xFF295E42)),
            startY = vistaTop,
            endY = wallBaseY + 24f * zoom,
          )
          BiomeTheme.ELDERWOOD_FOREST -> Brush.verticalGradient(
            listOf(Color(0xFF091526), Color(0xFF143542), Color(0xFF132B30)),
            startY = vistaTop,
            endY = wallBaseY,
          )
          else -> Brush.verticalGradient(
            listOf(Color(0xFF260E1B), Color(0xFF3D1A2E), Color(0xFF241D30)),
            startY = vistaTop,
            endY = wallBaseY,
          )
        }
        drawRect(
          brush = skyBrush,
          topLeft = Offset(0f, vistaTop),
          size = Size(size.width, (wallBaseY - vistaTop).coerceAtLeast(1f)),
        )

        if (biome == BiomeTheme.SANCTUARY_ASTRAL) {
          // Twinkling Dimensional Stars & Soaring 3/4 Octagonal Fortress Back Wall (Ref image_1b522f11.png!)
          for (i in 0..22) {
            val starX = sx(80f + i * 105f)
            val starY = wallBaseY - (115f + (i % 4) * 28f) * zoom
            if (starX in 0f..size.width && starY in 0f..wallBaseY) {
              val twinkle = 0.45f + 0.55f * abs(sin(timeSec * 2.1f + i))
              drawCircle(Color(0xFF99F6E4).copy(alpha = twinkle), radius = 2.0f * zoom, center = Offset(starX, starY))
            }
          }

          val wallH = 112f * zoom
          val wallTop = wallBaseY - wallH
          val spanW = (mapRight - mapLeft).coerceAtLeast(1f)
          // Monumental Townia Slate Stone Perimeter Wall rising in 3/4 height!
          drawRect(Color(0xFF1C3744), Offset(mapLeft, wallTop), Size(spanW, wallH))
          drawRect(Color(0xFF366072), Offset(mapLeft, wallTop), Size(spanW, 7f * zoom))
          drawRect(Color(0xFF2DD4BF).copy(alpha = 0.88f), Offset(mapLeft, wallTop + 7f * zoom), Size(spanW, 2.2f * zoom))
          drawRect(Color(0xFF112630), Offset(mapLeft, wallBaseY - 8f * zoom), Size(spanW, 8f * zoom))

          // Repeating Buttress Columns & Glowing Lancet Windows along Cathedral Back Wall
          var archX = wallMargin + 85f
          while (archX < worldWidth - wallMargin - 70f) {
            val ax = sx(archX)
            if (ax in -80f..(size.width + 80f)) {
              // Buttress pilaster
              drawRect(Color(0xFF392F63), Offset(ax - 34f * zoom, wallTop + 8f * zoom), Size(12f * zoom, wallH - 12f * zoom))
              if (abs(archX - TowerFloorCatalog.SANCTUARY_CENTER_X) > 190f) {
                // Tall Gothic stained-glass lancet window
                drawRect(Color(0xFF16112B), Offset(ax - 15f * zoom, wallTop + 20f * zoom), Size(30f * zoom, 66f * zoom))
                drawRect(Color(0xFF1E3A8A), Offset(ax - 12f * zoom, wallTop + 24f * zoom), Size(24f * zoom, 58f * zoom))
                drawRect(Color(0xFF38BDF8).copy(alpha = 0.55f), Offset(ax - 8f * zoom, wallTop + 28f * zoom), Size(16f * zoom, 48f * zoom))
                drawRect(Color(0xFFFBBF24).copy(alpha = 0.72f), Offset(ax - 5f * zoom, wallTop + 34f * zoom), Size(10f * zoom, 16f * zoom))
                // Mullion bars
                drawRect(Color(0xFF241D42), Offset(ax - 1.2f * zoom, wallTop + 24f * zoom), Size(2.4f * zoom, 58f * zoom))
                drawRect(Color(0xFF241D42), Offset(ax - 12f * zoom, wallTop + 50f * zoom), Size(24f * zoom, 2.4f * zoom))
              }
            }
            archX += 155f
          }
        }
      }
    }

    // 3. True Oblique 3/4 Foreshortened Pixel-Art Terrain Tilemap (64w x 46h screen ratio!)
    val worldTileSize = 64f
    val tileScreenW = (worldTileSize * zoom).roundToInt().coerceAtLeast(2)
    val tileScreenH = (worldTileSize * ObliqueCamera.OBLIQUE_Y_SCALE * zoom).roundToInt().coerceAtLeast(2)

    val minWorldX = ObliqueCamera.screenToWorldX(0f, camX, zoom, halfW)
    val maxWorldX = ObliqueCamera.screenToWorldX(size.width, camX, zoom, halfW)
    val minWorldY = ObliqueCamera.screenToWorldY(0f, camY, zoom, halfH)
    val maxWorldY = ObliqueCamera.screenToWorldY(size.height, camY, zoom, halfH)

    val minTxBound = if (biome == BiomeTheme.SANCTUARY_ASTRAL || biome.isInteriorBiome) {
      (wallMargin / worldTileSize).toInt()
    } else 0
    val maxTxBound = if (biome == BiomeTheme.SANCTUARY_ASTRAL || biome.isInteriorBiome) {
      ((worldWidth - wallMargin) / worldTileSize).toInt() - 1
    } else (worldWidth / worldTileSize).toInt()

    val minTyBound = if (biome == BiomeTheme.SANCTUARY_ASTRAL || biome.isInteriorBiome) {
      (wallMargin / worldTileSize).toInt()
    } else 0
    val maxTyBound = if (biome == BiomeTheme.SANCTUARY_ASTRAL || biome.isInteriorBiome) {
      ((worldHeight - wallMargin) / worldTileSize).toInt() - 1
    } else (worldHeight / worldTileSize).toInt()

    val startTx = ((minWorldX / worldTileSize).toInt() - 1).coerceAtLeast(minTxBound)
    val endTx = ((maxWorldX / worldTileSize).toInt() + 1).coerceAtMost(maxTxBound)
    val startTy = ((minWorldY / worldTileSize).toInt() - 1).coerceAtLeast(minTyBound)
    val endTy = ((maxWorldY / worldTileSize).toInt() + 1).coerceAtMost(maxTyBound)

    val baseTiles = if (biome == BiomeTheme.SANCTUARY_ASTRAL && SanctuaryAssetManager.tileAssets.isNotEmpty()) {
      SanctuaryAssetManager.tileAssets
    } else {
      when (biome) {
        BiomeTheme.SANCTUARY_ASTRAL, BiomeTheme.TRAINING_HALL_INTERIOR -> PixelTileAtlas.sanctuaryStoneTiles
        BiomeTheme.RESTAURANT_INTERIOR, BiomeTheme.DORMITORY_INTERIOR -> PixelTileAtlas.tavernWoodPlankTiles
        BiomeTheme.ARMORY_INTERIOR -> PixelTileAtlas.ruinCitadelFloorTiles
        BiomeTheme.FIRST_FIELD_MEADOW -> PixelTileAtlas.meadowGrassTiles
        BiomeTheme.ELDERWOOD_FOREST -> PixelTileAtlas.elderwoodFloorTiles
        else -> PixelTileAtlas.ruinCitadelFloorTiles
      }
    }

    val roadTiles = if (biome == BiomeTheme.SANCTUARY_ASTRAL && SanctuaryAssetManager.roadAssets.isNotEmpty()) {
      SanctuaryAssetManager.roadAssets
    } else {
      when (biome) {
        BiomeTheme.SANCTUARY_ASTRAL, BiomeTheme.TRAINING_HALL_INTERIOR -> PixelTileAtlas.sanctuaryCausewayTiles
        BiomeTheme.RESTAURANT_INTERIOR, BiomeTheme.DORMITORY_INTERIOR, BiomeTheme.ARMORY_INTERIOR -> PixelTileAtlas.tavernFlagstoneAisleTiles
        BiomeTheme.FIRST_FIELD_MEADOW -> PixelTileAtlas.meadowDirtRoadTiles
        else -> null
      }
    }

    val roadEdgeTiles = when (biome) {
      BiomeTheme.SANCTUARY_ASTRAL -> PixelTileAtlas.sanctuaryRoadCurbTiles
      BiomeTheme.FIRST_FIELD_MEADOW -> PixelTileAtlas.meadowRoadBorderTiles
      else -> null
    }

    for (ty in startTy..endTy) {
      val wy = ty * worldTileSize
      val dstY = sy(wy).roundToInt()
      for (tx in startTx..endTx) {
        val wx = tx * worldTileSize
        val dstX = sx(wx).roundToInt()
        val hash = ((tx * 73856093) xor (ty * 19349663)) and 0x7FFFFFFF

        val centerWx = wx + worldTileSize * 0.5f
        val centerWy = wy + worldTileSize * 0.5f

        var onRoadCore = false
        var onRoadEdge = false
        if (roadTiles != null) {
          for (seg in paths) {
            val d = distanceToSegment(centerWx, centerWy, seg)
            if (d <= seg.width * 0.50f) {
              onRoadCore = true
              break
            } else if (roadEdgeTiles != null && d <= seg.width * 0.76f) {
              onRoadEdge = true
            }
          }
        }

        val bmp = when {
          onRoadCore && roadTiles != null -> roadTiles[hash % roadTiles.size]
          onRoadEdge && roadEdgeTiles != null -> roadEdgeTiles[hash % roadEdgeTiles.size]
          else -> baseTiles[hash % baseTiles.size]
        }

        drawImage(
          image = bmp,
          dstOffset = IntOffset(dstX, dstY),
          dstSize = IntSize(tileScreenW + 1, tileScreenH + 1),
          filterQuality = FilterQuality.None,
        )
      }
    }

    // 4. Sanctuary Dimensional Cliff Drop-Off & Handcrafted Central Astrolabe Medallion (Reference 4!)
    if (biome == BiomeTheme.SANCTUARY_ASTRAL) {
      // Southern 3/4 Carved Stone Cliff Face dropping into the Astral Nebula Void!
      val cliffDropH = 68f * zoom
      if (southEdgeY < size.height + cliffDropH) {
        val spanW = (mapRight - mapLeft).coerceAtLeast(1f)
        drawRect(
          brush = Brush.verticalGradient(
            listOf(Color(0xFF3E3369), Color(0xFF231B42), Color(0xFF0E0A1F)),
            startY = southEdgeY,
            endY = southEdgeY + cliffDropH,
          ),
          topLeft = Offset(mapLeft, southEdgeY),
          size = Size(spanW, cliffDropH),
        )
        drawRect(Color(0xFF7B6BB8), Offset(mapLeft, southEdgeY - 4f * zoom), Size(spanW, 5f * zoom))
        drawRect(Color(0xFFF5B841).copy(alpha = 0.72f), Offset(mapLeft, southEdgeY + 1f * zoom), Size(spanW, 2f * zoom))
      }

      // Side 3/4 Stone Parapet Copings on West & East Terrace Edges
      val floorH = (southEdgeY - wallBaseY).coerceAtLeast(1f)
      drawRect(Color(0xFF473B75), Offset(mapLeft - 10f * zoom, wallBaseY), Size(10f * zoom, floorH))
      drawRect(Color(0xFF362C59), Offset(mapRight, wallBaseY), Size(10f * zoom, floorH))

      // Handcrafted Pixel-Art Foreshortened Central Astrolabe Medallion at Plaza Center!
      val pCx = sx(TowerFloorCatalog.SANCTUARY_CENTER_X)
      val pCy = sy(TowerFloorCatalog.SANCTUARY_PLAZA_Y)
      val medBmp = PixelTileAtlas.astrolabePlazaMedallion
      val medW = (medBmp.width * 1.95f * zoom).roundToInt().coerceAtLeast(4)
      val medH = (medBmp.height * 1.95f * zoom).roundToInt().coerceAtLeast(4)
      if (pCx + medW / 2f > -40f && pCx - medW / 2f < size.width + 40f &&
        pCy + medH / 2f > -40f && pCy - medH / 2f < size.height + 40f
      ) {
        val pulse = 0.24f + 0.08f * sin(timeSec * 2.2f)
        drawForeshortenedGlow(
          cx = pCx,
          cy = pCy,
          radiusX = 240f * zoom,
          innerColor = Color(0xFFF5B841).copy(alpha = pulse),
          midColor = Color(0xFF38BDF8).copy(alpha = pulse * 0.55f),
        )
        drawImage(
          image = medBmp,
          dstOffset = IntOffset((pCx - medW / 2f).roundToInt(), (pCy - medH / 2f).roundToInt()),
          dstSize = IntSize(medW, medH),
          alpha = 0.94f,
          filterQuality = FilterQuality.None,
        )
      }
    } else if (biome == BiomeTheme.RESTAURANT_INTERIOR) {
      // 3/4 Timber Side & Front Walls for Restaurant Interior
      val floorH = (southEdgeY - wallBaseY).coerceAtLeast(1f)
      drawRect(Color(0xFF4A2C1A), Offset(mapLeft - 12f * zoom, wallBaseY), Size(12f * zoom, floorH))
      drawRect(Color(0xFF4A2C1A), Offset(mapRight, wallBaseY), Size(12f * zoom, floorH))
      drawRect(Color(0xFF3B2214), Offset(mapLeft - 12f * zoom, southEdgeY), Size((mapRight - mapLeft) + 24f * zoom, 14f * zoom))
    } else if (biome.isInteriorBiome) {
      with(InteriorWorldRenderer) {
        drawInteriorFloorDetailsAndWalls(
          biome = biome,
          worldWidth = worldWidth,
          worldHeight = worldHeight,
          wallMargin = wallMargin,
          camX = camX,
          camY = camY,
          zoom = zoom,
        )
      }
    }

    // 5. Foreshortened Elliptical Warm Cobblestone / Sunlit Clearing / Hearth Light Contours
    for (patch in patches) {
      val cx = sx(patch.x)
      val cy = sy(patch.y)
      drawForeshortenedGlow(
        cx = cx,
        cy = cy,
        radiusX = patch.radiusX * zoom,
        innerColor = Color(patch.colorArgb).copy(alpha = 0.30f),
      )
    }
  }

  /**
   * Draws a foreshortened 3/4 elliptical magical floor ring (never a flat 90° overhead circle!).
   */
  fun DrawScope.drawSanctuaryZoneFloorRing(
    zone: SanctuaryZoneZone,
    isMasterInside: Boolean,
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
  ) {
    val cx = ObliqueCamera.worldToScreenX(zone.centerX, camX, zoom, size.width / 2f)
    val cy = ObliqueCamera.worldToScreenY(zone.centerY, camY, zoom, size.height / 2f)
    val rx = zone.interactRadius * 0.56f * zoom
    val ry = rx * ObliqueCamera.GROUND_ELLIPSE_RATIO
    if (cx + rx < 0f || cx - rx > size.width || cy + ry < 0f || cy - ry > size.height) return

    val baseColor = Color(zone.colorArgb)
    val glowAlpha = if (isMasterInside) 0.42f else (0.12f + 0.05f * sin(timeSec * 2.4f))

    if (isMasterInside) {
      drawForeshortenedGlow(
        cx = cx,
        cy = cy,
        radiusX = rx * 1.05f,
        innerColor = baseColor.copy(alpha = 0.20f),
      )
    }
    drawOval(
      color = baseColor.copy(alpha = glowAlpha),
      topLeft = Offset(cx - rx * 0.68f, cy - ry * 0.68f),
      size = Size(rx * 1.36f, ry * 1.36f),
      style = Stroke(width = (if (isMasterInside) 2.4f else 1.2f) * zoom),
    )
  }

  private fun DrawScope.drawCrispSprite(
    bmp: ImageBitmap,
    cx: Float,
    footY: Float,
    pixelScale: Float,
    alpha: Float = 1f,
  ) {
    val dstW = (bmp.width * pixelScale).roundToInt().coerceAtLeast(2)
    val dstH = (bmp.height * pixelScale).roundToInt().coerceAtLeast(2)
    drawImage(
      image = bmp,
      dstOffset = IntOffset((cx - dstW / 2f).roundToInt(), (footY - dstH).roundToInt()),
      dstSize = IntSize(dstW, dstH),
      alpha = alpha.coerceIn(0f, 1f),
      filterQuality = FilterQuality.None,
    )
  }

  private fun DrawScope.drawCrispUpperCanopyPortion(
    bmp: ImageBitmap,
    cx: Float,
    footY: Float,
    pixelScale: Float,
    topFraction: Float = 0.58f,
    alpha: Float = 0.94f,
  ) {
    val srcH = (bmp.height * topFraction).roundToInt().coerceIn(2, bmp.height)
    val fullDstW = (bmp.width * pixelScale).roundToInt().coerceAtLeast(2)
    val fullDstH = (bmp.height * pixelScale).roundToInt().coerceAtLeast(2)
    val topDstH = (fullDstH * topFraction).roundToInt().coerceAtLeast(2)
    drawImage(
      image = bmp,
      srcOffset = IntOffset(0, 0),
      srcSize = IntSize(bmp.width, srcH),
      dstOffset = IntOffset((cx - fullDstW / 2f).roundToInt(), (footY - fullDstH).roundToInt()),
      dstSize = IntSize(fullDstW, topDstH),
      alpha = alpha,
      filterQuality = FilterQuality.None,
    )
  }

  private fun DrawScope.drawDirectionalShadow34(
    cx: Float,
    footY: Float,
    halfW: Float,
    halfH: Float,
    alpha: Float = 0.52f,
  ) {
    // Layer 1: Softer directional sunlight shadow cast down-right (+14% X, +8% Y)
    drawOval(
      color = Color.Black.copy(alpha = (alpha * 0.54f).coerceIn(0f, 0.55f)),
      topLeft = Offset(cx - halfW * 0.84f, footY - halfH * 0.72f),
      size = Size(halfW * 2.18f, halfH * 1.88f),
    )
    // Layer 2: Compact grounding contact occlusion directly beneath object base
    drawOval(
      color = Color.Black.copy(alpha = (alpha * 0.88f).coerceIn(0f, 0.72f)),
      topLeft = Offset(cx - halfW * 0.90f, footY - halfH * 0.88f),
      size = Size(halfW * 1.80f, halfH * 1.48f),
    )
  }

  fun DrawScope.drawProp(
    obs: WorldObstacle,
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
    highestClearedFloor: Int = 0,
  ) {
    val cx = ObliqueCamera.worldToScreenX(obs.x, camX, zoom, size.width / 2f)
    val cy = ObliqueCamera.worldToScreenY(obs.y, camY, zoom, size.height / 2f, obs.elevationZ)
    val s = zoom * obs.scale
    if (cx < -380f || cx > size.width + 380f || cy < -400f || cy > size.height + 380f) return

    when (obs.propKind) {
      PropKind.FACILITY_TOWER_GATE, PropKind.TOWER_PORTAL_GATE -> {
        val pulse = 0.88f + 0.12f * sin(timeSec * 3.2f)
        val portalCoreCol = when {
          highestClearedFloor >= 5 -> Color(0xFFA78BFA)
          highestClearedFloor >= 1 -> Color(0xFF38BDF8)
          else -> Color(0xFF60A5FA)
        }
        drawDirectionalShadow34(cx + 12f * s, cy - 6f * s, 218f * s, 38f * s, alpha = 0.36f)
        // Radiant portal light spilling down the monolith staircase onto nearby stone!
        drawForeshortenedGlow(
          cx = cx,
          cy = cy + 12f * s,
          radiusX = (172f + highestClearedFloor * 6f) * s * pulse,
          innerColor = portalCoreCol.copy(alpha = 0.34f),
          midColor = Color(0xFF38BDF8).copy(alpha = 0.14f),
        )
        drawCrispSprite(PixelStructureAtlas.towerGateMonument, cx, cy + 6f * s, 2.0f * s)
        // Animated swirling portal oval + floor-milestone outer ring inside the split-monolith cleft!
        val riftCenter = Offset(cx, cy - 196f * s)
        drawOval(
          color = Color(0xFFE0F2FE).copy(alpha = 0.68f * pulse),
          topLeft = Offset(riftCenter.x - 32f * s, riftCenter.y - 54f * s),
          size = Size(64f * s, 108f * s),
          style = Stroke(width = 2.4f * s),
        )
        if (highestClearedFloor >= 1) {
          drawOval(
            color = Color(0xFFF5B841).copy(alpha = 0.58f * pulse),
            topLeft = Offset(riftCenter.x - 40f * s, riftCenter.y - 62f * s),
            size = Size(80f * s, 124f * s),
            style = Stroke(width = 1.6f * s),
          )
        }
        for (m in 0..5) {
          val lift = ((timeSec * 36f + m * 22f) % 105f) * s
          val mx = cx + sin(timeSec * 2.4f + m * 1.3f) * 24f * s
          drawCircle(Color(0xFFBAE6FD), radius = 2.4f * s, center = Offset(mx, cy - 135f * s - lift))
        }
      }

      PropKind.FACILITY_SUMMONING_HALL -> {
        drawDirectionalShadow34(cx + 12f * s, cy - 8f * s, 238f * s, 44f * s, alpha = 0.32f)
        drawCrispSprite(PixelStructureAtlas.summoningHallStructure, cx, cy + 6f * s, 2.0f * s)
        // Gentle azure starlight motes over the classical amphitheatre summoning stage
        for (w in 0..3) {
          val ang = timeSec * 1.8f + w * 1.57f
          val wx = cx + 22f * s + cos(ang) * 44f * s
          val wy = cy - 66f * s + sin(ang) * 16f * s
          drawCircle(Color(0xFFE0F2FE).copy(alpha = 0.78f), radius = 2.6f * s, center = Offset(wx, wy))
        }
      }

      PropKind.FACILITY_HERO_REGISTRY -> {
        drawDirectionalShadow34(cx + 8f * s, cy - 4f * s, 168f * s, 30f * s, alpha = 0.34f)
        drawCrispSprite(PixelStructureAtlas.heroRegistryStructure, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.FACILITY_TRAINING_GROUND -> {
        drawDirectionalShadow34(cx + 8f * s, cy - 4f * s, 176f * s, 32f * s, alpha = 0.34f)
        drawCrispSprite(PixelStructureAtlas.trainingGroundStructure, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.FACILITY_ARMORY -> {
        drawDirectionalShadow34(cx + 8f * s, cy - 4f * s, 174f * s, 30f * s, alpha = 0.34f)
        drawForeshortenedGlow(cx + 6f * s, cy + 8f * s, 72f * s, Color(0xFFFB923C).copy(alpha = 0.18f))
        drawCrispSprite(PixelStructureAtlas.armoryStructure, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.FACILITY_SYNTHESIS_LAB -> {
        drawDirectionalShadow34(cx + 8f * s, cy - 4f * s, 172f * s, 30f * s, alpha = 0.34f)
        drawForeshortenedGlow(cx, cy + 6f * s, 68f * s, Color(0xFF34D399).copy(alpha = 0.15f))
        drawCrispSprite(PixelStructureAtlas.synthesisLabStructure, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.FACILITY_HERO_QUARTERS -> {
        drawDirectionalShadow34(cx + 8f * s, cy - 4f * s, 182f * s, 32f * s, alpha = 0.34f)
        // Warm cozy residential interior window & doorway glow on ground!
        drawForeshortenedGlow(cx, cy + 10f * s, 76f * s, Color(0xFFFDE68A).copy(alpha = 0.22f))
        drawCrispSprite(PixelStructureAtlas.heroQuartersStructure, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.FACILITY_RESTAURANT -> {
        drawDirectionalShadow34(cx + 8f * s, cy - 4f * s, 188f * s, 32f * s, alpha = 0.34f)
        drawForeshortenedGlow(cx, cy + 10f * s, 82f * s, Color(0xFFFBBF24).copy(alpha = 0.24f))
        drawCrispSprite(PixelStructureAtlas.restaurantStructure, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.FLOOR_VISTA_ARCH -> {
        drawDirectionalShadow34(cx + 6f * s, cy - 4f * s, 138f * s, 24f * s, alpha = 0.32f)
        drawCrispSprite(PixelStructureAtlas.floorVistaArchStructure, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.MEDIEVAL_TOWNHOUSE_A -> {
        drawDirectionalShadow34(cx + 8f * s, cy - 4f * s, 134f * s, 25f * s, alpha = 0.34f)
        drawCrispSprite(PixelStructureAtlas.medievalTownhouseA, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.MEDIEVAL_TOWNHOUSE_B -> {
        drawDirectionalShadow34(cx + 8f * s, cy - 4f * s, 126f * s, 24f * s, alpha = 0.34f)
        drawCrispSprite(PixelStructureAtlas.medievalTownhouseB, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.SANCTUARY_COTTAGE_BROWN -> {
        drawDirectionalShadow34(cx + 8f * s, cy - 4f * s, 114f * s, 22f * s, alpha = 0.34f)
        drawCrispSprite(PixelStructureAtlas.sanctuaryCottageBrown, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.SANCTUARY_COTTAGE_SLATE -> {
        drawDirectionalShadow34(cx + 8f * s, cy - 4f * s, 114f * s, 22f * s, alpha = 0.34f)
        drawCrispSprite(PixelStructureAtlas.sanctuaryCottageSlate, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.FORTRESS_CORNER_TOWER -> {
        drawDirectionalShadow34(cx + 6f * s, cy, 48f * s, 14f * s, alpha = 0.36f)
        drawCrispSprite(PixelStructureAtlas.fortressCornerTowerSprite, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.SOUTHERN_GATE_TOWER -> {
        drawDirectionalShadow34(cx + 8f * s, cy, 58f * s, 16f * s, alpha = 0.40f)
        drawCrispSprite(PixelStructureAtlas.southernGateTowerSprite, cx, cy + 8f * s, 2.0f * s)
      }

      PropKind.SAKURA_BLOSSOM_TREE -> {
        val sway = sin(timeSec * 1.7f + obs.x * 0.015f) * 1.8f * s
        drawDirectionalShadow34(cx + 10f * s, cy, 70f * s, 18f * s, alpha = 0.32f)
        val bmp = if ((obs.id.hashCode() and 1) == 0) {
          PixelStructureAtlas.sakuraTreeSpriteA
        } else {
          PixelStructureAtlas.sakuraTreeSpriteB
        }
        drawCrispSprite(bmp, cx + sway, cy + 6f * s, 2.0f * s)
      }

      PropKind.CYPRESS_CONIFER_TREE -> {
        val sway = sin(timeSec * 1.9f + obs.x * 0.02f) * 1.2f * s
        drawDirectionalShadow34(cx + 6f * s, cy, 34f * s, 11f * s, alpha = 0.34f)
        drawCrispSprite(PixelStructureAtlas.cypressConiferSprite, cx + sway, cy + 4f * s, 2.0f * s)
      }

      PropKind.GARDEN_HEDGE_BUSH -> {
        drawDirectionalShadow34(cx + 4f * s, cy, 34f * s, 9f * s, alpha = 0.30f)
        drawCrispSprite(PixelStructureAtlas.gardenHedgeBushSprite, cx, cy + 4f * s, 1.9f * s)
      }

      PropKind.TRAINING_STRIKING_POST -> {
        drawDirectionalShadow34(cx + 8f * s, cy, 24f * s, 8f * s, alpha = 0.36f)
        drawCrispSprite(PixelStructureAtlas.trainingStrikingPostSprite, cx, cy + 4f * s, 2.0f * s)
      }

      PropKind.TERRACE_CLIFF_WALL -> {
        drawDirectionalShadow34(cx, cy, 158f * s, 18f * s, alpha = 0.42f)
        drawCrispSprite(PixelStructureAtlas.terraceCliffWallSprite, cx, cy + 4f * s, 2.0f * s)
      }

      PropKind.TERRACE_GRAND_STAIRS -> {
        drawCrispSprite(PixelStructureAtlas.terraceGrandStairsSprite, cx, cy + 4f * s, 2.0f * s)
      }

      PropKind.STONE_BALUSTRADE_RAIL -> {
        drawDirectionalShadow34(cx, cy, 104f * s, 14f * s, alpha = 0.44f)
        drawCrispSprite(PixelStructureAtlas.stoneBalustradeSprite, cx, cy + 4f * s, 2.0f * s)
      }

      PropKind.WOODEN_BENCH_34 -> {
        drawDirectionalShadow34(cx, cy, 52f * s, 11f * s, alpha = 0.45f)
        drawCrispSprite(PixelStructureAtlas.woodenBenchSprite, cx, cy + 4f * s, 2.0f * s)
      }

      PropKind.ARCANE_BOOKSHELF_34 -> {
        drawDirectionalShadow34(cx, cy, 50f * s, 13f * s, alpha = 0.50f)
        drawCrispSprite(PixelStructureAtlas.arcaneBookshelfSprite, cx, cy + 4f * s, 2.0f * s)
      }

      PropKind.STREET_LANTERN -> {
        val frameIdx = ((timeSec * 6f + obs.x * 0.03f).toInt() and 0x7FFFFFFF) % 3
        val glowAlpha = 0.34f + 0.08f * sin(timeSec * 7.5f + obs.y * 0.02f)
        drawForeshortenedGlow(
          cx = cx + 14f * s,
          cy = cy + 2f * s,
          radiusX = 118f * s,
          innerColor = Color(0xFFFBBF24).copy(alpha = glowAlpha),
          midColor = Color(0xFFD97706).copy(alpha = glowAlpha * 0.45f),
        )
        drawDirectionalShadow34(cx, cy, 16f * s, 6f * s, alpha = 0.50f)
        drawCrispSprite(PixelStructureAtlas.streetLanternFrames[frameIdx], cx, cy + 4f * s, 2.0f * s)
      }

      PropKind.BARREL_CLUSTER -> {
        drawDirectionalShadow34(cx, cy, 44f * s, 11f * s, alpha = 0.48f)
        drawCrispSprite(PixelStructureAtlas.barrelClusterSprite, cx, cy + 4f * s, 2.0f * s)
      }

      PropKind.STONE_RETAINING_WALL -> {
        drawDirectionalShadow34(cx, cy, 78f * s, 14f * s, alpha = 0.48f)
        drawCrispSprite(PixelStructureAtlas.stoneRetainingWallSprite, cx, cy + 4f * s, 2.0f * s)
      }

      PropKind.TAVERN_COUNTER_BAR -> {
        drawDirectionalShadow34(cx, cy, 152f * s, 22f * s, alpha = 0.50f)
        drawCrispSprite(PixelStructureAtlas.tavernCounterSprite, cx, cy + 8f * s, 2.05f * s)
      }

      PropKind.TAVERN_SHELF_BARRELS -> {
        drawDirectionalShadow34(cx, cy, 98f * s, 16f * s, alpha = 0.48f)
        drawCrispSprite(PixelStructureAtlas.tavernShelfBarrelsSprite, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.TAVERN_HEARTH_FIREPLACE -> {
        val frameIdx = ((timeSec * 8f).toInt() and 0x7FFFFFFF) % 3
        val fireGlow = 0.42f + 0.12f * sin(timeSec * 9.5f)
        drawForeshortenedGlow(
          cx = cx,
          cy = cy + 14f * s,
          radiusX = 175f * s,
          innerColor = Color(0xFFFB923C).copy(alpha = fireGlow),
          midColor = Color(0xFFFACC15).copy(alpha = fireGlow * 0.4f),
        )
        drawCrispSprite(PixelStructureAtlas.tavernFireplaceFrames[frameIdx], cx, cy + 8f * s, 2.05f * s)
      }

      PropKind.TAVERN_LONG_TABLE, PropKind.BANQUET_TABLE -> {
        drawDirectionalShadow34(cx, cy, 102f * s, 20f * s, alpha = 0.46f)
        drawForeshortenedGlow(cx + 32f * s, cy - 18f * s, 68f * s, Color(0xFFFBBF24).copy(alpha = 0.20f))
        drawCrispSprite(PixelStructureAtlas.tavernLongTableSprite, cx, cy + 8f * s, 2.0f * s)
      }

      PropKind.TAVERN_EXIT_MAT -> {
        val pulse = 0.65f + 0.35f * sin(timeSec * 3.2f)
        drawRect(Color(0xFF5C3522), Offset(cx - 56f * s, cy - 16f * s), Size(112f * s, 32f * s))
        drawRect(Color(0xFF991B1B), Offset(cx - 48f * s, cy - 12f * s), Size(96f * s, 24f * s))
        drawRect(
          color = Color(0xFFF5B841).copy(alpha = pulse),
          topLeft = Offset(cx - 50f * s, cy - 14f * s),
          size = Size(100f * s, 28f * s),
          style = Stroke(width = 2.2f * s),
        )
      }

      PropKind.ROYAL_STATUE -> {
        drawDirectionalShadow34(cx, cy, 42f * s, 12f * s, alpha = 0.52f)
        drawCrispSprite(PixelStructureAtlas.royalStatueSprite, cx, cy + 4f * s, 2.0f * s)
      }

      PropKind.OBSIDIAN_COLUMN, PropKind.RUINED_PILLAR -> {
        drawDirectionalShadow34(cx, cy, 38f * s, 13f * s, alpha = 0.56f)
        drawCrispSprite(PixelStructureAtlas.obsidianColumnSprite, cx, cy + 4f * s, 2.0f * s)
      }

      PropKind.ARCANE_BRAZIER -> {
        val frameIdx = ((timeSec * 8f + obs.x * 0.02f).toInt() and 0x7FFFFFFF) % 3
        val lightPulse = 0.28f + 0.08f * sin(timeSec * 9f + obs.x)
        drawForeshortenedGlow(
          cx = cx,
          cy = cy - 4f * s,
          radiusX = 104f * s,
          innerColor = Color(0xFF38BDF8).copy(alpha = lightPulse),
          midColor = Color(0xFF8B5CF6).copy(alpha = lightPulse * 0.5f),
        )
        drawDirectionalShadow34(cx, cy, 24f * s, 8f * s, alpha = 0.50f)
        drawCrispSprite(PixelStructureAtlas.brazierFrames[frameIdx], cx, cy + 4f * s, 1.95f * s)
      }

      PropKind.OAK_TREE -> {
        val sway = sin(timeSec * 1.9f + obs.x * 0.015f) * 2.0f * s
        drawDirectionalShadow34(cx + 8f * s, cy, 82f * s, 22f * s, alpha = 0.50f)
        drawCrispSprite(PixelStructureAtlas.oakTreeSprite, cx + sway, cy + 8f * s, 2.05f * s)
      }

      PropKind.MYSTIC_PLUM_TREE -> {
        val sway = sin(timeSec * 1.8f + obs.x * 0.015f) * 2.0f * s
        drawDirectionalShadow34(cx + 8f * s, cy, 78f * s, 20f * s, alpha = 0.50f)
        drawForeshortenedGlow(cx, cy - 12f * s, 88f * s, Color(0xFFD946EF).copy(alpha = 0.16f))
        drawCrispSprite(PixelStructureAtlas.mysticPlumTreeSprite, cx + sway, cy + 8f * s, 2.05f * s)
      }

      PropKind.PINE_SPIRIT_TREE -> {
        val sway = sin(timeSec * 2.0f + obs.x * 0.02f) * 1.6f * s
        drawDirectionalShadow34(cx + 6f * s, cy, 62f * s, 17f * s, alpha = 0.50f)
        drawCrispSprite(PixelStructureAtlas.spiritPineSprite, cx + sway, cy + 8f * s, 2.0f * s)
      }

      PropKind.FOREST_CANOPY_BUSH -> {
        val sway = sin(timeSec * 2.5f + obs.y * 0.03f) * 1.4f * s
        drawDirectionalShadow34(cx, cy, 46f * s, 11f * s, alpha = 0.44f)
        drawCrispSprite(PixelStructureAtlas.forestBushSprite, cx + sway, cy + 4f * s, 2.0f * s)
      }

      PropKind.ANCIENT_RUIN_ARCH -> {
        drawDirectionalShadow34(cx, cy, 88f * s, 19f * s, alpha = 0.52f)
        drawCrispSprite(PixelStructureAtlas.ruinArchSprite, cx, cy + 6f * s, 2.0f * s)
      }

      PropKind.ANCIENT_MONOLITH, PropKind.SUMMON_OBELISK -> {
        drawDirectionalShadow34(cx, cy, 32f * s, 10f * s, alpha = 0.50f)
        drawCrispSprite(PixelStructureAtlas.monolithSprite, cx, cy + 4f * s, 2.0f * s)
      }

      PropKind.MOSSY_BOULDER -> {
        drawDirectionalShadow34(cx, cy, 38f * s, 11f * s, alpha = 0.46f)
        drawCrispSprite(PixelStructureAtlas.mossyBoulderSprite, cx, cy + 4f * s, 1.95f * s)
      }

      PropKind.TALL_GRASS_CLUMP -> {
        val gSway = sin(timeSec * 3.1f + obs.x * 0.05f + obs.y * 0.02f) * 4.0f * s
        for (i in -3..3) {
          val bx = cx + i * 4.2f * s
          val h = (11f + (3 - abs(i)) * 3.2f) * s
          drawLine(
            color = Color(0xFF102421),
            start = Offset(bx, cy + 3f * s),
            end = Offset(bx + gSway, cy - h - 1f * s),
            strokeWidth = (3.4f * s).coerceAtLeast(1.6f),
          )
          drawLine(
            color = if (i % 2 == 0) Color(0xFF4E8C3E) else Color(0xFF9ED148),
            start = Offset(bx, cy + 3f * s),
            end = Offset(bx + gSway, cy - h),
            strokeWidth = (2.2f * s).coerceAtLeast(1.1f),
          )
        }
      }

      PropKind.WILDFLOWERS -> {
        drawOval(Color(0xFF1E4A35), Offset(cx - 10f * s, cy - 6f * s), Size(20f * s, 12f * s))
        drawOval(Color(0xFF42803B), Offset(cx - 8f * s, cy - 5f * s), Size(16f * s, 10f * s))
        drawCircle(Color(0xFFFEF08A), 3.0f * s, Offset(cx - 3.5f * s, cy - 2f * s))
        drawCircle(Color(0xFFFBBF24), 2.6f * s, Offset(cx + 4f * s, cy - 1f * s))
        drawCircle(Color(0xFFBAE6FD), 2.6f * s, Offset(cx, cy + 2.2f * s))
      }

      PropKind.TATTERED_BANNER -> {
        val wave = sin(timeSec * 4.2f + obs.x) * 4.5f * s
        drawLine(Color(0xFF120D1D), Offset(cx, cy + 4f * s), Offset(cx, cy - 64f * s), strokeWidth = 5f * s)
        drawLine(Color(0xFFD99B26), Offset(cx, cy + 4f * s), Offset(cx, cy - 64f * s), strokeWidth = 3f * s)
        val banner = Path().apply {
          moveTo(cx, cy - 60f * s)
          lineTo(cx + 24f * s + wave, cy - 52f * s)
          lineTo(cx + 18f * s + wave * 1.2f, cy - 22f * s)
          lineTo(cx, cy - 30f * s)
          close()
        }
        drawPath(banner, Color(0xFF6B21A8))
        drawRect(Color(0xFFF5B841), Offset(cx - 3f * s, cy - 64f * s), Size(8f * s, 4f * s))
      }

      PropKind.TRAINING_DUMMY -> {
        val wobble = sin(timeSec * 5.5f + obs.y) * 1.4f * s
        drawDirectionalShadow34(cx + 6f * s, cy, 18f * s, 6f * s, alpha = 0.36f)
        // Base cross-stand on sand
        drawRect(Color(0xFF2B1B10), Offset(cx - 12f * s, cy - 4f * s), Size(24f * s, 6f * s))
        drawRect(Color(0xFF8C5A32), Offset(cx - 10f * s, cy - 3f * s), Size(20f * s, 4f * s))
        // Vertical oak post + torso cylinder + horizontal wooden T-arms
        drawRect(Color(0xFF2B1B10), Offset(cx - 5f * s, cy - 46f * s), Size(10f * s, 46f * s))
        drawRect(Color(0xFFA87042), Offset(cx - 3.5f * s, cy - 44f * s), Size(7f * s, 42f * s))
        drawRect(Color(0xFF2B1B10), Offset(cx - 21f * s + wobble, cy - 33f * s), Size(42f * s, 8f * s))
        drawRect(Color(0xFFC28956), Offset(cx - 19f * s + wobble, cy - 31.5f * s), Size(38f * s, 5f * s))
        // Padded wooden torso barrel & head knob
        drawRect(Color(0xFF2B1B10), Offset(cx - 9f * s + wobble, cy - 38f * s), Size(18f * s, 22f * s))
        drawRect(Color(0xFFB87E4B), Offset(cx - 7.5f * s + wobble, cy - 36.5f * s), Size(15f * s, 19f * s))
        drawRect(Color(0xFF5C3B21), Offset(cx - 7.5f * s + wobble, cy - 29f * s), Size(15f * s, 3f * s))
        drawCircle(Color(0xFF2B1B10), 7.5f * s, Offset(cx + wobble, cy - 45f * s))
        drawCircle(Color(0xFFD19A66), 6.0f * s, Offset(cx + wobble, cy - 45f * s))
      }

      PropKind.WEAPON_RACK -> {
        drawDirectionalShadow34(cx + 6f * s, cy, 34f * s, 8f * s, alpha = 0.36f)
        // A-frame wooden side posts & horizontal crossbar
        drawRect(Color(0xFF2B1B10), Offset(cx - 32f * s, cy - 34f * s), Size(7f * s, 36f * s))
        drawRect(Color(0xFF2B1B10), Offset(cx + 25f * s, cy - 34f * s), Size(7f * s, 36f * s))
        drawRect(Color(0xFF8C5A32), Offset(cx - 30.5f * s, cy - 32.5f * s), Size(4f * s, 33f * s))
        drawRect(Color(0xFF8C5A32), Offset(cx + 26.5f * s, cy - 32.5f * s), Size(4f * s, 33f * s))
        drawRect(Color(0xFF2B1B10), Offset(cx - 34f * s, cy - 26f * s), Size(68f * s, 6f * s))
        drawRect(Color(0xFFA87042), Offset(cx - 32f * s, cy - 24.5f * s), Size(64f * s, 3.5f * s))
        drawRect(Color(0xFF2B1B10), Offset(cx - 32f * s, cy - 11f * s), Size(64f * s, 5f * s))
        drawRect(Color(0xFF8C5A32), Offset(cx - 30f * s, cy - 9.8f * s), Size(60f * s, 3f * s))
        // Upright wooden & steel practice spears/swords resting in rack
        for (i in -2..2) {
          val wx = cx + i * 10.5f * s
          drawLine(Color(0xFF2B1B10), Offset(wx, cy - 3f * s), Offset(wx, cy - 41f * s), strokeWidth = 4.2f * s)
          drawLine(Color(0xFFCBD5E1), Offset(wx, cy - 18f * s), Offset(wx, cy - 39f * s), strokeWidth = 2.4f * s)
          drawLine(Color(0xFF9A6638), Offset(wx, cy - 4f * s), Offset(wx, cy - 18f * s), strokeWidth = 2.4f * s)
          drawRect(Color(0xFFD99B26), Offset(wx - 4f * s, cy - 19f * s), Size(8f * s, 2.8f * s))
        }
      }

      PropKind.ALCHEMICAL_CRUCIBLE -> {
        val glow = 0.6f + 0.4f * sin(timeSec * 4.5f)
        drawForeshortenedGlow(cx, cy - 4f * s, 54f * s, Color(0xFF10B981).copy(alpha = 0.30f * glow))
        drawOval(Color(0xFF334155), Offset(cx - 22f * s, cy - 28f * s), Size(44f * s, 30f * s))
        drawOval(Color(0xFF34D399), Offset(cx - 16f * s, cy - 25f * s), Size(32f * s, 14f * s))
      }

      PropKind.RECOVERY_BED -> {
        drawDirectionalShadow34(cx, cy, 26f * s, 10f * s)
        drawRect(Color(0xFF47362B), Offset(cx - 24f * s, cy - 22f * s), Size(48f * s, 24f * s))
        drawRect(Color(0xFFE2E8F0), Offset(cx - 20f * s, cy - 19f * s), Size(14f * s, 16f * s))
        drawRect(Color(0xFF2563EB), Offset(cx - 5f * s, cy - 19f * s), Size(25f * s, 16f * s))
      }

      PropKind.CRIMSON_CRYSTAL -> {
        val p = Path().apply {
          moveTo(cx, cy - 54f * s)
          lineTo(cx + 18f * s, cy - 14f * s)
          lineTo(cx, cy + 4f * s)
          lineTo(cx - 18f * s, cy - 14f * s)
          close()
        }
        drawForeshortenedGlow(cx, cy - 6f * s, 46f * s, Color(0xFFF43F5E).copy(alpha = 0.34f))
        drawPath(p, Color(0xFFE11D48))
      }

      else -> {
        with(InteriorWorldRenderer) {
          drawCustomProp34(obs, cx, cy, s, timeSec)
        }
      }
    }
  }

  /**
   * Overhanging Tree Canopy Occlusion Pass: keeps high spreading tree boughs layered over
   * characters walking right beside wide trunks while Y-sorting handles full building/roof occlusion!
   */
  fun DrawScope.drawUpperCanopyOcclusionPass(
    obstacles: List<WorldObstacle>,
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
  ) {
    for (obs in obstacles) {
      if (!obs.occludesUpperCanopy) continue
      val cx = ObliqueCamera.worldToScreenX(obs.x, camX, zoom, size.width / 2f)
      val cy = ObliqueCamera.worldToScreenY(obs.y, camY, zoom, size.height / 2f, obs.elevationZ)
      val s = zoom * obs.scale
      if (cx < -320f || cx > size.width + 320f || cy < -340f || cy > size.height + 320f) continue

      when (obs.propKind) {
        PropKind.OAK_TREE -> {
          val sway = sin(timeSec * 1.9f + obs.x * 0.015f) * 2.0f * s
          drawCrispUpperCanopyPortion(PixelStructureAtlas.oakTreeSprite, cx + sway, cy + 8f * s, 2.05f * s, topFraction = 0.54f, alpha = 0.94f)
        }
        PropKind.MYSTIC_PLUM_TREE -> {
          val sway = sin(timeSec * 1.8f + obs.x * 0.015f) * 2.0f * s
          drawCrispUpperCanopyPortion(PixelStructureAtlas.mysticPlumTreeSprite, cx + sway, cy + 8f * s, 2.05f * s, topFraction = 0.54f, alpha = 0.94f)
        }
        PropKind.PINE_SPIRIT_TREE -> {
          val sway = sin(timeSec * 2.0f + obs.x * 0.02f) * 1.6f * s
          drawCrispUpperCanopyPortion(PixelStructureAtlas.spiritPineSprite, cx + sway, cy + 8f * s, 2.0f * s, topFraction = 0.52f, alpha = 0.94f)
        }
        PropKind.SAKURA_BLOSSOM_TREE -> {
          val sway = sin(timeSec * 1.7f + obs.x * 0.015f) * 1.8f * s
          val bmp = if ((obs.id.hashCode() and 1) == 0) {
            PixelStructureAtlas.sakuraTreeSpriteA
          } else {
            PixelStructureAtlas.sakuraTreeSpriteB
          }
          drawCrispUpperCanopyPortion(bmp, cx + sway, cy + 6f * s, 2.0f * s, topFraction = 0.56f, alpha = 0.96f)
        }
        else -> Unit
      }
    }
  }

  fun DrawScope.drawSouthernForegroundWalls(
    biome: BiomeTheme,
    camX: Float,
    camY: Float,
    zoom: Float,
  ) {
    if (biome == BiomeTheme.SANCTUARY_ASTRAL) {
      with(SanctuaryWorldRenderer) {
        drawSouthernForegroundFortressWalls(camX = camX, camY = camY, zoom = zoom)
      }
    }
  }

  /**
   * Renders Master Summoner, Autonomous Heroes, or Sanctuary Keepers in True 3/4 Perspective
   * (38x50 native pixel art at crisp nearest-neighbor scale + foreshortened elliptical ground shadow).
   */
  fun DrawScope.drawCharacterSprite(
    worldX: Float,
    worldY: Float,
    camX: Float,
    camY: Float,
    zoom: Float,
    isMaster: Boolean,
    heroClass: HeroClassType,
    facingRight: Boolean,
    isMoving: Boolean,
    attackAnimProgress: Float,
    hurtFlash: Float,
    hpFraction: Float,
    shieldOrAuraColor: Color?,
    isInjuredResting: Boolean,
    timeSec: Float,
    facingNorth: Boolean = false,
  ) {
    val cx = ObliqueCamera.worldToScreenX(worldX, camX, zoom, size.width / 2f)
    val cy = ObliqueCamera.worldToScreenY(worldY, camY, zoom, size.height / 2f)
    if (cx < -120f || cx > size.width + 120f || cy < -150f || cy > size.height + 120f) return

    val pixelScale = 1.85f * zoom

    // Foreshortened 3/4 Elliptical Ground Contact Shadow
    drawOval(
      color = Color(0x7C000000),
      topLeft = Offset(cx - 22f * zoom, cy - 6.5f * zoom),
      size = Size(44f * zoom, 13f * zoom),
    )

    // Foreshortened 3/4 Sovereign Sigil / Shield Barrier Ellipse on Ground
    if (shieldOrAuraColor != null) {
      drawOval(
        color = shieldOrAuraColor.copy(alpha = 0.52f),
        topLeft = Offset(cx - 28f * zoom, cy - 12f * zoom),
        size = Size(56f * zoom, 24f * zoom),
        style = Stroke(width = 2.0f * zoom),
      )
    }

    val sheet = if (isMaster) {
      PixelCharacterAtlas.masterSprites
    } else {
      PixelCharacterAtlas.heroSheet(heroClass)
    }

    val frameBmp = sheet.selectFrame(
      facingRight = facingRight,
      isMoving = isMoving,
      attackProgress = attackAnimProgress,
      hurtFlash = hurtFlash,
      isResting = isInjuredResting,
      timeSec = timeSec,
      seed = (worldX * 0.01f + worldY * 0.01f),
      facingNorth = facingNorth,
    )

    drawCrispSprite(
      bmp = frameBmp,
      cx = cx,
      footY = cy + 6f * zoom,
      pixelScale = pixelScale,
    )

    if (attackAnimProgress > 0.08f && !isMaster) {
      val slashColor = when (heroClass) {
        HeroClassType.KNIGHT -> Color(0xFF38BDF8)
        HeroClassType.ARCHER -> Color(0xFFFDE047)
        HeroClassType.MAGE -> Color(0xFFE879F9)
        HeroClassType.ASSASSIN -> Color(0xFFF43F5E)
        HeroClassType.PRIEST -> Color(0xFF34D399)
        HeroClassType.BERSERKER -> Color(0xFFFB923C)
      }
      drawArc(
        color = slashColor.copy(alpha = (1f - attackAnimProgress).coerceIn(0.28f, 0.95f)),
        startAngle = if (facingRight) -65f else 115f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(cx - 32f * zoom, cy - 58f * zoom),
        size = Size(64f * zoom, 48f * zoom),
        style = Stroke(width = 3.2f * zoom),
      )
    }

    if (!isMaster && (hpFraction < 0.999f || isInjuredResting)) {
      val barW = 40f * zoom
      val barH = 5.2f * zoom
      val bx = cx - barW / 2f
      val by = cy - 86f * zoom
      drawRect(Color(0xDD090710), Offset(bx - 1.2f, by - 1.2f), Size(barW + 2.4f, barH + 2.4f))
      val hpCol = when {
        isInjuredResting -> Color(0xFFF59E0B)
        hpFraction > 0.55f -> Color(0xFF10B981)
        hpFraction > 0.28f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
      }
      drawRect(hpCol, Offset(bx, by), Size(barW * hpFraction.coerceIn(0f, 1f), barH))
    }
  }

  fun DrawScope.drawEnemySprite(
    worldX: Float,
    worldY: Float,
    camX: Float,
    camY: Float,
    zoom: Float,
    archetype: EnemyArchetype,
    facingRight: Boolean,
    isMoving: Boolean = false,
    attackAnimProgress: Float,
    hurtFlash: Float,
    hpFraction: Float,
    isTargetMarked: Boolean,
    timeSec: Float,
    deathAnimProgress: Float = 0f,
  ) {
    val cx = ObliqueCamera.worldToScreenX(worldX, camX, zoom, size.width / 2f)
    val cy = ObliqueCamera.worldToScreenY(worldY, camY, zoom, size.height / 2f)
    if (cx < -150f || cx > size.width + 150f || cy < -180f || cy > size.height + 150f) return

    val scaleMult = if (archetype.isBoss) 2.0f else 1.85f
    val p = scaleMult * zoom

    if (deathAnimProgress > 0f) {
      val fade = (1f - deathAnimProgress).coerceIn(0f, 1f)
      val dropY = deathAnimProgress * 14f * zoom
      val sheet = PixelCharacterAtlas.enemySheet(archetype)
      val bmp = if (facingRight) sheet.hurtRight else sheet.hurtLeft
      drawCrispSprite(bmp, cx, cy + 6f * zoom + dropY, p * (1f - deathAnimProgress * 0.25f), alpha = fade)
      for (e in 0..4) {
        val ex = cx + (e - 2) * 9f * zoom * (0.4f + deathAnimProgress)
        val ey = cy - 18f * zoom - deathAnimProgress * (24f + e * 6f) * zoom
        drawCircle(Color(0xFFFB7185).copy(alpha = fade), radius = 3.0f * zoom, center = Offset(ex, ey))
      }
      return
    }

    val shadowW = (if (archetype.isBoss) 72f else 44f) * zoom
    drawOval(Color(0x78000000), Offset(cx - shadowW / 2f, cy - 6.5f * zoom), Size(shadowW, 14f * zoom))

    if (isTargetMarked) {
      val pulse = 0.85f + 0.15f * sin(timeSec * 8f)
      val rx = (if (archetype.isBoss) 40f else 28f) * zoom * pulse
      val ry = rx * ObliqueCamera.GROUND_ELLIPSE_RATIO
      drawOval(
        color = Color(0xFFEF4444),
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = 2.4f * zoom),
      )
    }

    val sheet = PixelCharacterAtlas.enemySheet(archetype)
    val frameBmp = sheet.selectFrame(
      facingRight = facingRight,
      isMoving = isMoving,
      attackProgress = attackAnimProgress,
      hurtFlash = hurtFlash,
      isResting = false,
      timeSec = timeSec,
      seed = worldX * 0.02f,
    )
    drawCrispSprite(
      bmp = frameBmp,
      cx = cx,
      footY = cy + 6f * zoom,
      pixelScale = p,
    )

    val barW = (if (archetype.isBoss) 68f else 40f) * zoom
    val barH = (if (archetype.isBoss) 6.8f else 5.0f) * zoom
    val bx = cx - barW / 2f
    val by = cy - (if (archetype.isBoss) 124f else 82f) * zoom
    drawRect(Color(0xDD090710), Offset(bx - 1.2f, by - 1.2f), Size(barW + 2.4f, barH + 2.4f))
    drawRect(
      color = if (archetype.isBoss) Color(0xFFE11D48) else Color(0xFFEF4444),
      topLeft = Offset(bx, by),
      size = Size(barW * hpFraction.coerceIn(0f, 1f), barH),
    )
  }

  fun DrawScope.drawForegroundAtmosphere(
    biome: BiomeTheme,
    timeSec: Float,
    particlesEnabled: Boolean = true,
  ) {
    if (biome == BiomeTheme.SANCTUARY_ASTRAL) {
      if (!particlesEnabled) return
      // Gentle Sunlit Sakura Blossom Petals & Golden Daylight Motes drifting across the Sanctuary
      val petalCount = 22
      for (i in 0 until petalCount) {
        val seed = i * 43.3f
        val px = ((seed * 31.7f + timeSec * (22f + (i % 4) * 6f)) % size.width.coerceAtLeast(1f))
        val py = ((seed * 19.3f + timeSec * (14f + (i % 3) * 5f) + sin(timeSec * 1.8f + i) * 16f) % size.height.coerceAtLeast(1f))
        val alpha = (0.48f + 0.36f * abs(sin(timeSec * 1.9f + i))).coerceIn(0.25f, 0.88f)
        val col = when (i % 3) {
          0 -> Color(0xFFFBCFE8) // Soft sakura pink
          1 -> Color(0xFFFFF1F2) // Blossom white
          else -> Color(0xFFFEF9C3) // Warm sunlit mote
        }
        drawOval(
          color = col.copy(alpha = alpha),
          topLeft = Offset(px, py),
          size = Size(5.5f, 3.5f),
        )
      }
      return
    }

    // 1. Subtle Diagonal Volumetric Light Shafts for Battlefields & Tavern Interior
    val rayColor = when (biome) {
      BiomeTheme.RESTAURANT_INTERIOR -> Color(0x1EFBBF24)
      BiomeTheme.FIRST_FIELD_MEADOW -> Color(0x22FDE68A)
      BiomeTheme.ELDERWOOD_FOREST -> Color(0x1F38BDF8)
      else -> Color(0x1EF43F5E)
    }
    for (i in 0..3) {
      val drift = sin(timeSec * 0.42f + i * 1.9f) * 36f
      val baseX = size.width * (0.12f + i * 0.28f) + drift
      val ray = Path().apply {
        moveTo(baseX, 0f)
        lineTo(baseX + size.width * 0.14f, 0f)
        lineTo(baseX - size.width * 0.05f, size.height)
        lineTo(baseX - size.width * 0.19f, size.height)
        close()
      }
      drawPath(ray, rayColor)
    }

    // 2. Foreground Overhanging Boughs (Forest / Grassland) & Cinematic Edge Framing
    if (biome == BiomeTheme.FIRST_FIELD_MEADOW || biome == BiomeTheme.ELDERWOOD_FOREST) {
      val swayL = sin(timeSec * 1.3f) * 8f
      val swayR = cos(timeSec * 1.2f) * 8f
      val canopyShade = Color(0xCC07131A)
      val canopyEdge = Color(0xAA0E2830)
      for (i in 0..5) {
        val r = (52f - i * 5f)
        val lx = 26f + i * 38f + swayL * (0.4f + i * 0.1f)
        val ly = 14f + (i % 2) * 18f
        drawCircle(canopyEdge, radius = r + 5f, center = Offset(lx, ly))
        drawCircle(canopyShade, radius = r, center = Offset(lx, ly - 3f))

        val rx = size.width - (26f + i * 38f) + swayR * (0.4f + i * 0.1f)
        val ry = 14f + ((i + 1) % 2) * 18f
        drawCircle(canopyEdge, radius = r + 5f, center = Offset(rx, ry))
        drawCircle(canopyShade, radius = r, center = Offset(rx, ry - 3f))
      }
    }

    if (!particlesEnabled) return

    // 3. Crisp Pixel-Sized Drifting Mana Embers & Lantern Sparks
    val particleCount = 26
    val pPrimary = when (biome) {
      BiomeTheme.RESTAURANT_INTERIOR -> Color(0xFFFBBF24)
      BiomeTheme.FIRST_FIELD_MEADOW -> Color(0xFFD9F99D)
      BiomeTheme.ELDERWOOD_FOREST -> Color(0xFF38BDF8)
      else -> Color(0xFFFB7185)
    }
    val pSecondary = Color(0xFFF5B841)

    for (i in 0 until particleCount) {
      val seed = i * 37.7f
      val px = ((seed * 29.3f + timeSec * (12f + (i % 4) * 4f)) % size.width.coerceAtLeast(1f))
      val pyRaw = size.height - ((seed * 19.1f + timeSec * (18f + (i % 3) * 7f)) % size.height.coerceAtLeast(1f))
      val alpha = (0.35f + 0.45f * abs(sin(timeSec * 2.3f + i))).coerceIn(0.15f, 0.85f)
      val col = if (i % 2 == 0) pPrimary else pSecondary
      drawRect(
        color = col.copy(alpha = alpha),
        topLeft = Offset(px, pyRaw),
        size = Size(3f, 3f),
      )
    }
  }
}
