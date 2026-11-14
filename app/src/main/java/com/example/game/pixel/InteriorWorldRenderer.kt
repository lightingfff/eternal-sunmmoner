package com.example.game.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.data.model.IslandExpansionMilestone
import com.example.game.map.BiomeTheme
import com.example.game.map.ObliqueCamera
import com.example.game.map.PropKind
import com.example.game.map.SanctuaryIslandGeometry
import com.example.game.map.WorldObstacle
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Modular 3/4 2.5D Renderer for:
 * 1. Enterable Sanctuary Building Interiors (Dormitory, Training Hall, Armory)
 * 2. Physical Sanctuary Island Expansions (East Celestial Garden & NE Astral Sky-Dock)
 * 3. Custom 3/4 Interactive Stations, Flower Planters & Tower Exploration Nodes
 */
object InteriorWorldRenderer {
  private val wallMaterialMask = Path()
  private fun DrawScope.stoneWallMaterial(left: Float, top: Float, width: Float, height: Float,
    zoom: Float, tone: Int) {
    wallMaterialMask.reset()
    wallMaterialMask.addRect(Rect(left, top, left + width, top + height))
    with(ArchitectureSurfaceArt) {
      drawSurface("limestone", wallMaterialMask, Offset(left, top),
        Offset(.7f * zoom, 0f), Offset(0f, .7f * zoom), alpha = .85f, tone = tone)
    }
  }

  fun DrawScope.drawInteriorBackFacade(
    biome: BiomeTheme,
    worldWidth: Float,
    wallMargin: Float,
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
  ) {
    val halfW = size.width / 2f
    val halfH = size.height / 2f
    fun sx(wx: Float) = ObliqueCamera.worldToScreenX(wx, camX, zoom, halfW)
    fun sy(wy: Float, z: Float = 0f) = ObliqueCamera.worldToScreenY(wy, camY, zoom, halfH, z)

    val wallBaseY = sy(wallMargin)
    val wallHeightPx = 122f * zoom
    val wallTop = wallBaseY - wallHeightPx
    val roomL = sx(wallMargin - 14f)
    val roomR = sx(worldWidth - wallMargin + 14f)
    val roomW = (roomR - roomL).coerceAtLeast(1f)

    when (biome) {
      BiomeTheme.DORMITORY_INTERIOR -> {
        // Warm cream-plaster & dark-oak half-timbered Dormitory back wall with sunlit mullioned windows!
        drawRect(Color(0xFFD9CDB8), Offset(roomL, wallTop), Size(roomW, wallHeightPx))
        drawRect(Color(0xFF4F3220), Offset(roomL, wallTop), Size(roomW, 12f * zoom))
        drawRect(Color(0xFF6B442A), Offset(roomL, wallBaseY - 14f * zoom), Size(roomW, 14f * zoom))
        drawRect(Color(0xFF3A2315), Offset(roomL, wallBaseY - 4f * zoom), Size(roomW, 4f * zoom))

        var postX = wallMargin + 30f
        var idx = 0
        while (postX < worldWidth - wallMargin) {
          val px = sx(postX)
          drawRect(Color(0xFF4A2E1D), Offset(px - 5f * zoom, wallTop), Size(10f * zoom, wallHeightPx))
          drawRect(Color(0xFF6E462C), Offset(px - 3f * zoom, wallTop), Size(3f * zoom, wallHeightPx))

          if (idx % 2 == 0 && postX + 110f < worldWidth - wallMargin) {
            // Sunlit mullioned Dormitory window + warm angled floor light beam
            val winL = px + 26f * zoom
            val winT = wallTop + 24f * zoom
            val winW = 54f * zoom
            val winH = 52f * zoom
            drawRect(Color(0xFF3D2517), Offset(winL - 3f * zoom, winT - 3f * zoom), Size(winW + 6f * zoom, winH + 6f * zoom))
            drawRect(
              brush = Brush.verticalGradient(
                listOf(Color(0xFFBAE6FD), Color(0xFFFEF9C3)),
                startY = winT,
                endY = winT + winH,
              ),
              topLeft = Offset(winL, winT),
              size = Size(winW, winH),
            )
            // Window mullions
            drawRect(Color(0xFF4A2E1D), Offset(winL + winW * 0.48f, winT), Size(2.5f * zoom, winH))
            drawRect(Color(0xFF4A2E1D), Offset(winL, winT + winH * 0.48f), Size(winW, 2.5f * zoom))
            // Soft warm sunlight trapezoid cast onto the Dormitory floor!
            val beam = Path().apply {
              moveTo(winL, wallBaseY)
              lineTo(winL + winW, wallBaseY)
              lineTo(winL + winW + 34f * zoom, wallBaseY + 86f * zoom)
              lineTo(winL + 14f * zoom, wallBaseY + 86f * zoom)
              close()
            }
            drawPath(beam, Color(0x28FEF3C7))
          } else if (postX + 90f < worldWidth - wallMargin) {
            // Framed Sanctuary banner & warm amber wall lamp
            val lampX = px + 44f * zoom
            val lampY = wallTop + 56f * zoom
            val pulse = 0.30f + 0.06f * sin(timeSec * 5.2f + idx)
            drawCircle(
              brush = Brush.radialGradient(
                listOf(Color(0xFFFBBF24).copy(alpha = pulse), Color.Transparent),
                center = Offset(lampX, lampY),
                radius = 86f * zoom,
              ),
              radius = 86f * zoom,
              center = Offset(lampX, lampY),
            )
            drawRect(Color(0xFF332116), Offset(lampX - 3f * zoom, lampY), Size(6f * zoom, 10f * zoom))
            drawCircle(Color(0xFFFEF08A), radius = 4.2f * zoom, center = Offset(lampX, lampY))
          }
          postX += 144f
          idx++
        }
      }

      BiomeTheme.TRAINING_HALL_INTERIOR -> {
        // High ashlar-stone & timber Dojo Hall with class heraldic banners & sunlit clerestory windows
        drawRect(Color(0xFF8A857C), Offset(roomL, wallTop), Size(roomW, wallHeightPx))
        stoneWallMaterial(roomL, wallTop, roomW, wallHeightPx, zoom, 1)
        drawRect(Color(0xFF57534E), Offset(roomL, wallTop), Size(roomW, 15f * zoom))
        drawRect(Color(0xFF44403C), Offset(roomL, wallBaseY - 12f * zoom), Size(roomW, 12f * zoom))

        for (c in 1..4) {
          val yLine = wallTop + wallHeightPx * (c / 5f)
          drawLine(Color(0xFF6B665E), Offset(roomL, yLine), Offset(roomR, yLine), strokeWidth = 1.2f * zoom)
        }

        val bannerColors = listOf(Color(0xFF2563EB), Color(0xFFD97706), Color(0xFF7C3AED), Color(0xFFDC2626))
        var colX = wallMargin + 48f
        var bIdx = 0
        while (colX < worldWidth - wallMargin - 40f) {
          val px = sx(colX)
          // Heavy stone pilaster column
          drawRect(Color(0xFF57534E), Offset(px - 8f * zoom, wallTop), Size(16f * zoom, wallHeightPx))
          drawRect(Color(0xFFA8A29E), Offset(px - 5f * zoom, wallTop), Size(5f * zoom, wallHeightPx))

          if (colX + 110f < worldWidth - wallMargin) {
            val bx = px + 48f * zoom
            val bt = wallTop + 26f * zoom
            val bCol = bannerColors[bIdx % bannerColors.size]
            val banner = Path().apply {
              moveTo(bx - 16f * zoom, bt)
              lineTo(bx + 16f * zoom, bt)
              lineTo(bx + 16f * zoom, bt + 46f * zoom)
              lineTo(bx, bt + 58f * zoom)
              lineTo(bx - 16f * zoom, bt + 46f * zoom)
              close()
            }
            drawPath(banner, bCol)
            drawPath(banner, Color(0xFFF5B841), style = Stroke(width = 1.6f * zoom))
            drawRect(Color(0xFFF5B841), Offset(bx - 19f * zoom, bt - 3f * zoom), Size(38f * zoom, 4f * zoom))
          }
          colX += 156f
          bIdx++
        }
      }

      BiomeTheme.ARMORY_INTERIOR -> {
        // Forged dark-stone & iron Armory Guildhouse with glowing central smelter arch & tool racks
        drawRect(Color(0xFF475569), Offset(roomL, wallTop), Size(roomW, wallHeightPx))
        stoneWallMaterial(roomL, wallTop, roomW, wallHeightPx, zoom, 2)
        drawRect(Color(0xFF1E293B), Offset(roomL, wallTop), Size(roomW, 16f * zoom))
        drawRect(Color(0xFF0F172A), Offset(roomL, wallBaseY - 12f * zoom), Size(roomW, 12f * zoom))

        for (c in 1..4) {
          val yLine = wallTop + wallHeightPx * (c / 5f)
          drawLine(Color(0xFF334155), Offset(roomL, yLine), Offset(roomR, yLine), strokeWidth = 1.3f * zoom)
        }

        // Central Recessed Blazing Forge Arch on Northern Back Wall
        val forgeCx = sx(worldWidth * 0.5f)
        val archW = 112f * zoom
        val archH = 78f * zoom
        val emberPulse = 0.76f + 0.24f * sin(timeSec * 7.5f)
        drawRect(Color(0xFF1E293B), Offset(forgeCx - archW * 0.56f, wallBaseY - archH), Size(archW * 1.12f, archH))
        drawOval(Color(0xFF0F172A), Offset(forgeCx - archW * 0.44f, wallBaseY - archH * 0.88f), Size(archW * 0.88f, archH * 0.82f))
        drawOval(
          brush = Brush.radialGradient(
            listOf(
              Color(0xFFFEF08A).copy(alpha = 0.92f * emberPulse),
              Color(0xFFF97316).copy(alpha = 0.82f * emberPulse),
              Color(0xFF7C2D12).copy(alpha = 0.45f),
            ),
            center = Offset(forgeCx, wallBaseY - 22f * zoom),
            radius = 52f * zoom,
          ),
          topLeft = Offset(forgeCx - 42f * zoom, wallBaseY - 56f * zoom),
          size = Size(84f * zoom, 48f * zoom),
        )
        // Hanging shields & crossed blades along Armory back wall
        listOf(worldWidth * 0.24f, worldWidth * 0.76f).forEach { wx ->
          val sxPos = sx(wx)
          val syPos = wallTop + 52f * zoom
          drawLine(Color(0xFFCBD5E1), Offset(sxPos - 18f * zoom, syPos - 16f * zoom), Offset(sxPos + 18f * zoom, syPos + 16f * zoom), strokeWidth = 3f * zoom)
          drawLine(Color(0xFFCBD5E1), Offset(sxPos + 18f * zoom, syPos - 16f * zoom), Offset(sxPos - 18f * zoom, syPos + 16f * zoom), strokeWidth = 3f * zoom)
          drawCircle(Color(0xFF1E3A8A), radius = 13f * zoom, center = Offset(sxPos, syPos))
          drawCircle(Color(0xFFF5B841), radius = 13f * zoom, center = Offset(sxPos, syPos), style = Stroke(width = 2f * zoom))
        }
      }

      else -> Unit
    }
  }

  fun DrawScope.drawInteriorFloorDetailsAndWalls(
    biome: BiomeTheme,
    worldWidth: Float,
    worldHeight: Float,
    wallMargin: Float,
    camX: Float,
    camY: Float,
    zoom: Float,
  ) {
    val halfW = size.width / 2f
    val halfH = size.height / 2f
    fun sx(wx: Float) = ObliqueCamera.worldToScreenX(wx, camX, zoom, halfW)
    fun sy(wy: Float) = ObliqueCamera.worldToScreenY(wy, camY, zoom, halfH)

    val mapLeft = sx(wallMargin)
    val mapRight = sx(worldWidth - wallMargin)
    val wallBaseY = sy(wallMargin)
    val southEdgeY = sy(worldHeight - wallMargin)
    val floorH = (southEdgeY - wallBaseY).coerceAtLeast(1f)

    when (biome) {
      BiomeTheme.DORMITORY_INTERIOR -> {
        // Cozy woven wool rugs under West & East bed rows
        listOf(305f to 430f, 915f to 430f).forEach { (rxWorld, ryWorld) ->
          val rl = sx(rxWorld - 135f)
          val rr = sx(rxWorld + 135f)
          val rt = sy(ryWorld - 145f)
          val rb = sy(ryWorld + 165f)
          drawRect(Color(0xFF7F1D1D).copy(alpha = 0.78f), Offset(rl, rt), Size(rr - rl, rb - rt))
          drawRect(Color(0xFFF5B841).copy(alpha = 0.65f), Offset(rl + 5f * zoom, rt + 4f * zoom), Size((rr - rl - 10f * zoom).coerceAtLeast(2f), (rb - rt - 8f * zoom).coerceAtLeast(2f)), style = Stroke(width = 1.8f * zoom))
        }
        // 3/4 Side & Southern Cutaway Timber Walls (showing visible wall thickness!)
        drawRect(Color(0xFF5C3A24), Offset(mapLeft - 14f * zoom, wallBaseY), Size(14f * zoom, floorH))
        drawRect(Color(0xFF5C3A24), Offset(mapRight, wallBaseY), Size(14f * zoom, floorH))
        drawRect(Color(0xFF3E2516), Offset(mapLeft - 14f * zoom, southEdgeY), Size((mapRight - mapLeft) + 28f * zoom, 16f * zoom))
      }

      BiomeTheme.TRAINING_HALL_INTERIOR -> {
        // Inlaid circular sparring ring markings on the indoor training hall floor
        val cx = sx(worldWidth * 0.5f)
        val cy = sy(470f)
        val rx = 215f * zoom
        val ry = rx * ObliqueCamera.OBLIQUE_Y_SCALE
        drawOval(Color(0x36F59E0B), Offset(cx - rx, cy - ry), Size(rx * 2f, ry * 2f), style = Stroke(width = 3.2f * zoom))
        drawOval(Color(0x28FDE68A), Offset(cx - rx * 0.62f, cy - ry * 0.62f), Size(rx * 1.24f, ry * 1.24f), style = Stroke(width = 2.0f * zoom))
        drawLine(Color(0x2CFDE68A), Offset(cx - rx, cy), Offset(cx + rx, cy), strokeWidth = 1.8f * zoom)

        drawRect(Color(0xFF57534E), Offset(mapLeft - 14f * zoom, wallBaseY), Size(14f * zoom, floorH))
        drawRect(Color(0xFF57534E), Offset(mapRight, wallBaseY), Size(14f * zoom, floorH))
        drawRect(Color(0xFF3F3B37), Offset(mapLeft - 14f * zoom, southEdgeY), Size((mapRight - mapLeft) + 28f * zoom, 16f * zoom))
      }

      BiomeTheme.ARMORY_INTERIOR -> {
        // Forged iron floor grate & brass trim around central anvil bay
        val gxL = sx(440f)
        val gxR = sx(760f)
        val gyT = sy(285f)
        val gyB = sy(465f)
        drawRect(Color(0x441E293B), Offset(gxL, gyT), Size(gxR - gxL, gyB - gyT))
        drawRect(Color(0x6694A3B8), Offset(gxL, gyT), Size(gxR - gxL, gyB - gyT), style = Stroke(width = 2.2f * zoom))

        drawRect(Color(0xFF334155), Offset(mapLeft - 14f * zoom, wallBaseY), Size(14f * zoom, floorH))
        drawRect(Color(0xFF334155), Offset(mapRight, wallBaseY), Size(14f * zoom, floorH))
        drawRect(Color(0xFF1E293B), Offset(mapLeft - 14f * zoom, southEdgeY), Size((mapRight - mapLeft) + 28f * zoom, 16f * zoom))
      }

      else -> Unit
    }
  }

  /**
   * Renders physical Sanctuary Island Expansions attached to the 8-sided fortress-island:
   * - Locked state: subtle astral blueprint silhouette + glowing rune milestone pedestal
   * - Unlocked state: solid 3/4 floating rock cliff, arched stone bridge / timber sky-dock, sunlit grass,
   *   healing spring fountain, sakura trees & anchored scout skiff!
   */
  fun DrawScope.drawSanctuaryIslandExpansions(
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
    unlockedExpansions: Set<IslandExpansionMilestone>,
  ) {
    val halfW = size.width / 2f
    val halfH = size.height / 2f
    fun sx(wx: Float) = ObliqueCamera.worldToScreenX(wx, camX, zoom, halfW)
    fun sy(wy: Float, z: Float = 0f) = ObliqueCamera.worldToScreenY(wy, camY, zoom, halfH, z)

    val pulse = 0.65f + 0.35f * sin(timeSec * 2.8f)

    // 1. EAST CELESTIAL GARDEN TERRACE (Attached at East Wall x=2900, y=1265)
    val eastUnlocked = IslandExpansionMilestone.EAST_CELESTIAL_GARDEN in unlockedExpansions
    val eastCx = sx(SanctuaryIslandGeometry.EAST_TERRACE_CENTER_X)
    val eastCy = sy(SanctuaryIslandGeometry.EAST_TERRACE_CENTER_Y, 0f)

    if (eastCx > -420f && eastCx < size.width + 420f && eastCy > -360f && eastCy < size.height + 360f) {
      if (!eastUnlocked) {
        // Ghostly astral blueprint preview off the East Rampart (never blocks current map!)
        val previewBridgeL = sx(2905f)
        val previewBridgeR = sx(3035f)
        val previewBridgeT = sy(1210f, 0f)
        val previewBridgeB = sy(1320f, 0f)
        drawRect(
          color = Color(0xFF38BDF8).copy(alpha = 0.16f * pulse),
          topLeft = Offset(previewBridgeL, previewBridgeT),
          size = Size(previewBridgeR - previewBridgeL, previewBridgeB - previewBridgeT),
        )
        drawRect(
          color = Color(0xFFBAE6FD).copy(alpha = 0.45f * pulse),
          topLeft = Offset(previewBridgeL, previewBridgeT),
          size = Size(previewBridgeR - previewBridgeL, previewBridgeB - previewBridgeT),
          style = Stroke(width = 1.8f * zoom),
        )
        val rx = 175f * zoom
        val ry = 148f * zoom
        drawOval(
          color = Color(0xFF38BDF8).copy(alpha = 0.14f * pulse),
          topLeft = Offset(eastCx - rx, eastCy - ry),
          size = Size(rx * 2f, ry * 2f),
        )
        drawOval(
          color = Color(0xFFE0F2FE).copy(alpha = 0.50f * pulse),
          topLeft = Offset(eastCx - rx, eastCy - ry),
          size = Size(rx * 2f, ry * 2f),
          style = Stroke(width = 2.0f * zoom),
        )
        drawExpansionBeaconPedestal(
          bx = sx(SanctuaryIslandGeometry.EAST_BEACON_X),
          by = sy(SanctuaryIslandGeometry.EAST_BEACON_Y, 0f),
          zoom = zoom,
          pulse = pulse,
          unlocked = false,
        )
      } else {
        // SOLID 3/4 FLOATING CELESTIAL GARDEN ISLAND + ARCHED LIMESTONE BRIDGE!
        val rx = 182f * zoom
        val ry = 152f * zoom

        // Rocky floating cliff underside
        val cliff = Path().apply {
          moveTo(eastCx - rx * 0.96f, eastCy + 8f * zoom)
          lineTo(eastCx - rx * 0.54f, eastCy + ry * 1.48f)
          lineTo(eastCx + rx * 0.06f, eastCy + ry * 1.92f)
          lineTo(eastCx + rx * 0.62f, eastCy + ry * 1.42f)
          lineTo(eastCx + rx * 0.96f, eastCy + 8f * zoom)
          close()
        }
        drawPath(cliff, Color(0xFF6E6259))
        drawPath(cliff, Color(0xFF2B2420), style = Stroke(width = 2.0f * zoom))

        // Overhanging grass rim + tiled grass terrace
        drawOval(Color(0xFF3E8234), Offset(eastCx - rx * 1.03f, eastCy - ry * 0.96f + 9f * zoom), Size(rx * 2.06f, ry * 1.98f))
        val terraceOval = Path().apply {
          addOval(androidx.compose.ui.geometry.Rect(eastCx - rx, eastCy - ry, eastCx + rx, eastCy + ry))
        }
        drawPath(terraceOval, Color(0xFF69BD4F))
        val grassTiles = PixelTileAtlas.sanctuaryGrassTiles
        val tsW = (64f * zoom).roundToInt().coerceAtLeast(2)
        val tsH = (64f * ObliqueCamera.OBLIQUE_Y_SCALE * zoom).roundToInt().coerceAtLeast(2)
        clipPath(terraceOval) {
          for (ty in 16..23) {
            val dstY = sy(ty * 64f, 0f).roundToInt()
            for (tx in 47..53) {
              val dstX = sx(tx * 64f).roundToInt()
              val hash = ((tx * 73856093) xor (ty * 19349663)) and 0x7FFFFFFF
              drawGroundTile(
                PixelTileAtlas.sanctuaryGrassSheet, grassTiles, tx, ty, hash,
                dstX, dstY, tsW + 1, tsH + 1,
                rotated = PixelTileAtlas.sanctuaryGrassSheetRotated,
              )
            }
          }
        }
        drawPath(terraceOval, Color(0xFFDFD9CC), style = Stroke(width = 4.2f * zoom))
        drawPath(terraceOval, Color(0xFF2B2520), style = Stroke(width = 1.5f * zoom))

        // Pale-Limestone Arched Bridge connecting East Wall (x=2885) -> Celestial Terrace (x=3035)
        val brL = sx(2880f)
        val brR = sx(3042f)
        val brT = sy(1202f, 0f)
        val brB = sy(1328f, 0f)
        val brW = (brR - brL).coerceAtLeast(4f)
        val brH = (brB - brT).coerceAtLeast(4f)
        // Side stone arch thickness underneath bridge
        drawRect(Color(0xFF8C8577), Offset(brL, brB), Size(brW, 28f * zoom))
        drawOval(Color(0xFF539CE0), Offset(brL + brW * 0.24f, brB + 8f * zoom), Size(brW * 0.52f, 26f * zoom))
        // Bridge cobblestone deck & top/bottom stone balustrades
        drawRect(Color(0xFFE6E1D6), Offset(brL, brT), Size(brW, brH))
        for (s in 1..4) {
          val lx = brL + brW * (s / 5f)
          drawLine(Color(0xFFAFA99E), Offset(lx, brT), Offset(lx, brB), strokeWidth = 1.3f * zoom)
        }
        drawRect(Color(0xFFEDE8DC), Offset(brL, brT - 8f * zoom), Size(brW, 9f * zoom))
        drawRect(Color(0xFFD6CFC2), Offset(brL, brB - 5f * zoom), Size(brW, 9f * zoom))
        drawRect(Color(0xFF2B2520), Offset(brL, brT - 8f * zoom), Size(brW, brH + 12f * zoom), style = Stroke(width = 1.5f * zoom))

        // Central Celestial Healing Spring Fountain on the new island!
        val fRx = 56f * zoom
        val fRy = fRx * ObliqueCamera.OBLIQUE_Y_SCALE
        drawOval(Color(0xFF8C867B), Offset(eastCx - fRx * 1.08f, eastCy - fRy * 1.05f + 5f * zoom), Size(fRx * 2.16f, fRy * 2.10f))
        drawOval(Color(0xFFEDE9E0), Offset(eastCx - fRx, eastCy - fRy), Size(fRx * 2f, fRy * 2f))
        drawOval(Color(0xFF38BDF8), Offset(eastCx - fRx * 0.78f, eastCy - fRy * 0.78f), Size(fRx * 1.56f, fRy * 1.56f))
        val rippleFrac = 0.35f + 0.35f * ((timeSec * 0.9f) % 1f)
        drawOval(
          color = Color(0xFFE0F2FE).copy(alpha = 1f - rippleFrac),
          topLeft = Offset(eastCx - fRx * rippleFrac, eastCy - fRy * rippleFrac),
          size = Size(fRx * 2f * rippleFrac, fRy * 2f * rippleFrac),
          style = Stroke(width = 1.8f * zoom),
        )
        // Central crystal fountain spire
        drawRect(Color(0xFFF3F0EA), Offset(eastCx - 8f * zoom, eastCy - 28f * zoom), Size(16f * zoom, 28f * zoom))
        drawCircle(Color(0xFF7DD3FC), radius = 7f * zoom, center = Offset(eastCx, eastCy - 32f * zoom))

        // Mini Sakura trees & flower clusters gracing the unlocked Celestial Garden
        listOf(-105f to -95f, 95f to -90f, 102f to 88f, -96f to 92f).forEachIndexed { idx, (ox, oy) ->
          val tx = eastCx + ox * zoom
          val ty = eastCy + oy * ObliqueCamera.OBLIQUE_Y_SCALE * zoom
          drawOval(Color(0x55000000), Offset(tx - 24f * zoom, ty - 6f * zoom), Size(48f * zoom, 13f * zoom))
          drawRect(Color(0xFF5C3A24), Offset(tx - 4f * zoom, ty - 26f * zoom), Size(8f * zoom, 26f * zoom))
          drawCircle(Color(0xFFF472B6), radius = 20f * zoom, center = Offset(tx, ty - 34f * zoom))
          drawCircle(Color(0xFFFBCFE8), radius = 15f * zoom, center = Offset(tx - 4f * zoom, ty - 38f * zoom))
          if (idx % 2 == 0) {
            drawCircle(Color(0xFFFEF08A), radius = 3.2f * zoom, center = Offset(tx + 22f * zoom, ty + 4f * zoom))
          }
        }
      }
    }

    // 2. NORTH-EAST ASTRAL SKY-DOCK (Attached off NE Rampart x=2825, y=535)
    val northUnlocked = IslandExpansionMilestone.NORTH_ASTRAL_DOCK in unlockedExpansions
    val dockCx = sx(SanctuaryIslandGeometry.NORTH_DOCK_CENTER_X)
    val dockCy = sy(SanctuaryIslandGeometry.NORTH_DOCK_CENTER_Y, 0f)
    if (dockCx > -380f && dockCx < size.width + 380f && dockCy > -320f && dockCy < size.height + 320f) {
      if (!northUnlocked) {
        val dl = sx(2660f)
        val dr = sx(2990f)
        val dt = sy(455f, 0f)
        val db = sy(625f, 0f)
        drawRect(Color(0xFF38BDF8).copy(alpha = 0.14f * pulse), Offset(dl, dt), Size(dr - dl, db - dt))
        drawRect(Color(0xFFBAE6FD).copy(alpha = 0.45f * pulse), Offset(dl, dt), Size(dr - dl, db - dt), style = Stroke(width = 1.8f * zoom))
        drawExpansionBeaconPedestal(
          bx = sx(SanctuaryIslandGeometry.NORTH_BEACON_X),
          by = sy(SanctuaryIslandGeometry.NORTH_BEACON_Y, 0f),
          zoom = zoom,
          pulse = pulse,
          unlocked = false,
        )
      } else {
        // Solid Stone-and-Timber Cantilevered Sky-Dock + Moored Astral Scout Skiff!
        val dl = sx(2635f)
        val dr = sx(3015f)
        val dt = sy(445f, 0f)
        val db = sy(635f, 0f)
        val dw = (dr - dl).coerceAtLeast(4f)
        val dh = (db - dt).coerceAtLeast(4f)

        drawRect(Color(0xFF57534E), Offset(dl, db), Size(dw, 26f * zoom))
        drawRect(Color(0xFFB8804C), Offset(dl, dt), Size(dw, dh))
        var py = dt + 8f * zoom
        while (py < db) {
          drawLine(Color(0xFF6E4528), Offset(dl, py), Offset(dr, py), strokeWidth = 1.4f * zoom)
          py += 10f * zoom
        }
        drawRect(Color(0xFFDFD9CC), Offset(dl, dt - 6f * zoom), Size(dw, 8f * zoom))
        drawRect(Color(0xFF2B1A0F), Offset(dl, dt - 6f * zoom), Size(dw, dh + 6f * zoom), style = Stroke(width = 1.8f * zoom))

        // Moored Winged Astral Scout Skiff gently bobbing beside the NE Sky-Dock
        val bob = sin(timeSec * 1.8f) * 5f * zoom
        val shipX = dr + 48f * zoom
        val shipY = (dt + db) * 0.5f + bob
        val hull = Path().apply {
          moveTo(shipX - 42f * zoom, shipY)
          lineTo(shipX + 46f * zoom, shipY - 8f * zoom)
          lineTo(shipX + 32f * zoom, shipY + 18f * zoom)
          lineTo(shipX - 30f * zoom, shipY + 18f * zoom)
          close()
        }
        drawPath(hull, Color(0xFF7C4A27))
        drawPath(hull, Color(0xFFF5B841), style = Stroke(width = 1.8f * zoom))
        // White-and-gold astral sail
        val sail = Path().apply {
          moveTo(shipX + 4f * zoom, shipY - 52f * zoom)
          lineTo(shipX + 34f * zoom, shipY - 14f * zoom)
          lineTo(shipX - 8f * zoom, shipY - 10f * zoom)
          close()
        }
        drawPath(sail, Color(0xFFF8FAFC))
        drawLine(Color(0xFF4A2E1B), Offset(shipX + 2f * zoom, shipY), Offset(shipX + 2f * zoom, shipY - 56f * zoom), strokeWidth = 3f * zoom)
      }
    }
  }

  private fun DrawScope.drawExpansionBeaconPedestal(
    bx: Float,
    by: Float,
    zoom: Float,
    pulse: Float,
    unlocked: Boolean,
  ) {
    val col = if (unlocked) Color(0xFF34D399) else Color(0xFFF5B841)
    drawOval(
      color = col.copy(alpha = 0.28f * pulse),
      topLeft = Offset(bx - 30f * zoom, by - 14f * zoom),
      size = Size(60f * zoom, 28f * zoom),
    )
    drawOval(
      color = col.copy(alpha = 0.85f),
      topLeft = Offset(bx - 24f * zoom, by - 11f * zoom),
      size = Size(48f * zoom, 22f * zoom),
      style = Stroke(width = 2.0f * zoom),
    )
    drawRect(Color(0xFF475569), Offset(bx - 9f * zoom, by - 26f * zoom), Size(18f * zoom, 24f * zoom))
    drawCircle(col, radius = 6.5f * zoom, center = Offset(bx, by - 31f * zoom))
  }

  /**
   * Renders custom 3/4 furniture, forge stations, flower planters, and Floor 1 exploration chests/shrines.
   */
  fun DrawScope.drawCustomProp34(
    obs: WorldObstacle,
    cx: Float,
    cy: Float,
    s: Float,
    timeSec: Float,
  ): Boolean {
    when (obs.propKind) {
      PropKind.FORGE_ANVIL_STATION -> {
        // Layered shadow
        drawOval(Color(0x44000000), Offset(cx - 46f * s, cy - 11f * s), Size(98f * s, 24f * s))
        drawOval(Color(0x66000000), Offset(cx - 32f * s, cy - 8f * s), Size(64f * s, 16f * s))
        // Oak stump base + dark iron horned anvil + glowing heated blade!
        drawRect(Color(0xFF5C3A24), Offset(cx - 18f * s, cy - 20f * s), Size(36f * s, 20f * s))
        drawRect(Color(0xFF2B1A10), Offset(cx - 18f * s, cy - 20f * s), Size(36f * s, 20f * s), style = Stroke(width = 1.6f * s))
        val anvil = Path().apply {
          moveTo(cx - 28f * s, cy - 28f * s)
          lineTo(cx + 26f * s, cy - 30f * s)
          lineTo(cx + 18f * s, cy - 20f * s)
          lineTo(cx - 18f * s, cy - 20f * s)
          close()
        }
        drawPath(anvil, Color(0xFF334155))
        drawRect(Color(0xFF94A3B8), Offset(cx - 20f * s, cy - 31f * s), Size(38f * s, 4f * s))
        val glow = 0.7f + 0.3f * sin(timeSec * 8f)
        drawLine(Color(0xFFFB923C).copy(alpha = glow), Offset(cx - 10f * s, cy - 32f * s), Offset(cx + 12f * s, cy - 32f * s), strokeWidth = 3.2f * s)
        // Quenching water barrel beside anvil
        drawOval(Color(0xFF6E4528), Offset(cx + 24f * s, cy - 24f * s), Size(20f * s, 22f * s))
        drawOval(Color(0xFF38BDF8), Offset(cx + 26f * s, cy - 23f * s), Size(16f * s, 8f * s))
        return true
      }

      PropKind.ARMOR_STAND_34 -> {
        drawOval(Color(0x48000000), Offset(cx - 24f * s, cy - 7f * s), Size(52f * s, 15f * s))
        // Wooden plinth & post
        drawRect(Color(0xFF5C3A24), Offset(cx - 16f * s, cy - 7f * s), Size(32f * s, 7f * s))
        drawRect(Color(0xFF4A2E1B), Offset(cx - 3.5f * s, cy - 48f * s), Size(7f * s, 42f * s))
        // Silver-steel breastplate, pauldrons & crested helm
        drawOval(Color(0xFF64748B), Offset(cx - 18f * s, cy - 38f * s), Size(36f * s, 22f * s))
        drawOval(Color(0xFFCBD5E1), Offset(cx - 13f * s, cy - 36f * s), Size(26f * s, 18f * s))
        drawCircle(Color(0xFF94A3B8), radius = 8.5f * s, center = Offset(cx, cy - 46f * s))
        drawRect(Color(0xFFEF4444), Offset(cx - 3f * s, cy - 58f * s), Size(6f * s, 8f * s))
        return true
      }

      PropKind.DORM_PERSONAL_CHEST -> {
        drawOval(Color(0x46000000), Offset(cx - 24f * s, cy - 6f * s), Size(50f * s, 13f * s))
        drawRect(Color(0xFF6E4528), Offset(cx - 20f * s, cy - 18f * s), Size(40f * s, 18f * s))
        drawRect(Color(0xFF8C5A35), Offset(cx - 20f * s, cy - 23f * s), Size(40f * s, 7f * s))
        drawRect(Color(0xFFF5B841), Offset(cx - 14f * s, cy - 23f * s), Size(4f * s, 23f * s))
        drawRect(Color(0xFFF5B841), Offset(cx + 10f * s, cy - 23f * s), Size(4f * s, 23f * s))
        drawRect(Color(0xFF2B1A0F), Offset(cx - 20f * s, cy - 23f * s), Size(40f * s, 23f * s), style = Stroke(width = 1.5f * s))
        return true
      }

      PropKind.INFIRMARY_CARE_TABLE -> {
        drawOval(Color(0x46000000), Offset(cx - 42f * s, cy - 9f * s), Size(86f * s, 18f * s))
        drawRect(Color(0xFF5C3A24), Offset(cx - 36f * s, cy - 26f * s), Size(72f * s, 10f * s))
        drawRect(Color(0xFF3E2516), Offset(cx - 32f * s, cy - 16f * s), Size(6f * s, 16f * s))
        drawRect(Color(0xFF3E2516), Offset(cx + 26f * s, cy - 16f * s), Size(6f * s, 16f * s))
        // White linen cloth, green herbal potion vials & clean bandages
        drawRect(Color(0xFFF8FAFC), Offset(cx - 26f * s, cy - 28f * s), Size(52f * s, 6f * s))
        drawCircle(Color(0xFF34D399), radius = 5.0f * s, center = Offset(cx - 12f * s, cy - 32f * s))
        drawCircle(Color(0xFF38BDF8), radius = 4.5f * s, center = Offset(cx + 2f * s, cy - 31f * s))
        drawRect(Color(0xFFE2E8F0), Offset(cx + 12f * s, cy - 33f * s), Size(12f * s, 6f * s))
        return true
      }

      PropKind.FLOWER_PLANTER_BED -> {
        val planterSprite = PixelStructureAtlas.flowerPlanterSprite
        if (planterSprite != null) {
          // P23: authored stone-curb planter. The 64x36 canvas is blitted bottom-centred at
          // 2s with a +8s foot offset, which is where the Canvas curb used to sit, so the prop
          // keeps its anchor, its footprint and its collider.
          val planterW = (64f * s).roundToInt().coerceAtLeast(2)
          val planterH = (36f * s).roundToInt().coerceAtLeast(2)
          drawImage(
            image = planterSprite,
            dstOffset = IntOffset(
              (cx - planterW / 2f).roundToInt(),
              (cy + 8f * s - planterH).roundToInt(),
            ),
            dstSize = IntSize(planterW, planterH),
            filterQuality = FilterQuality.None,
          )
          return true
        }
        drawOval(Color(0x38000000), Offset(cx - 34f * s, cy - 8f * s), Size(72f * s, 18f * s))
        // Low pale-stone curb planter box + rich soil + multi-colored blossoms
        drawRect(Color(0xFFD6CFC2), Offset(cx - 30f * s, cy - 13f * s), Size(60f * s, 13f * s))
        drawRect(Color(0xFF5C3D24), Offset(cx - 26f * s, cy - 12f * s), Size(52f * s, 8f * s))
        drawRect(Color(0xFF3A3530), Offset(cx - 30f * s, cy - 13f * s), Size(60f * s, 13f * s), style = Stroke(width = 1.3f * s))
        val colors = listOf(Color(0xFFF472B6), Color(0xFFFEF08A), Color(0xFF60A5FA), Color(0xFFFB7185), Color(0xFFC084FC))
        for (i in -2..2) {
          val fx = cx + i * 10f * s
          drawCircle(Color(0xFF3E8742), radius = 5.5f * s, center = Offset(fx, cy - 12f * s))
          drawCircle(colors[(i + 2) % colors.size], radius = 3.6f * s, center = Offset(fx, cy - 14f * s))
        }
        return true
      }

      PropKind.TREASURE_CHEST_CLOSED, PropKind.TREASURE_CHEST_OPEN -> {
        val isOpen = obs.propKind == PropKind.TREASURE_CHEST_OPEN
        val pulse = 0.65f + 0.35f * sin(timeSec * 4.2f)
        if (!isOpen) {
          drawOval(
            color = Color(0xFFF5B841).copy(alpha = 0.28f * pulse),
            topLeft = Offset(cx - 38f * s, cy - 16f * s),
            size = Size(76f * s, 32f * s),
          )
        }
        drawOval(Color(0x52000000), Offset(cx - 26f * s, cy - 7f * s), Size(54f * s, 15f * s))
        drawRect(Color(0xFF78350F), Offset(cx - 22f * s, cy - 20f * s), Size(44f * s, 20f * s))
        if (isOpen) {
          drawRect(Color(0xFF451A03), Offset(cx - 19f * s, cy - 20f * s), Size(38f * s, 8f * s))
          drawRect(Color(0xFF92400E), Offset(cx - 22f * s, cy - 32f * s), Size(44f * s, 10f * s))
        } else {
          drawRect(Color(0xFFB45309), Offset(cx - 22f * s, cy - 28f * s), Size(44f * s, 10f * s))
          drawRect(Color(0xFFFBBF24), Offset(cx - 4f * s, cy - 22f * s), Size(8f * s, 8f * s))
        }
        drawRect(Color(0xFFF59E0B), Offset(cx - 16f * s, cy - 28f * s), Size(4f * s, 28f * s))
        drawRect(Color(0xFFF59E0B), Offset(cx + 12f * s, cy - 28f * s), Size(4f * s, 28f * s))
        drawRect(Color(0xFF1F130A), Offset(cx - 22f * s, cy - 28f * s), Size(44f * s, 28f * s), style = Stroke(width = 1.6f * s))
        return true
      }

      PropKind.ASTRAL_HERB_SHRINE -> {
        val pulse = 0.70f + 0.30f * sin(timeSec * 3.4f)
        drawOval(
          color = Color(0xFF34D399).copy(alpha = 0.28f * pulse),
          topLeft = Offset(cx - 46f * s, cy - 20f * s),
          size = Size(92f * s, 40f * s),
        )
        drawOval(Color(0x52000000), Offset(cx - 30f * s, cy - 8f * s), Size(62f * s, 17f * s))
        drawOval(Color(0xFFCBD5E1), Offset(cx - 26f * s, cy - 12f * s), Size(52f * s, 18f * s))
        drawRect(Color(0xFF94A3B8), Offset(cx - 12f * s, cy - 38f * s), Size(24f * s, 32f * s))
        drawCircle(Color(0xFF34D399), radius = 10f * s * pulse, center = Offset(cx, cy - 46f * s))
        drawCircle(Color(0xFFD1FAE5), radius = 5.5f * s, center = Offset(cx, cy - 46f * s))
        return true
      }

      else -> return false
    }
  }
}
