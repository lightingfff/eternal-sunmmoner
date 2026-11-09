package com.example.game.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.map.BiomeTheme
import com.example.game.map.ObliqueCamera
import com.example.game.map.SanctuaryIslandGeometry
import com.example.game.map.SanctuaryLight
import com.example.game.world.WorldEnvironment
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * SanctuarySunlightSystem: Cinematic HD-2D Shader-Style Runtime Lighting, Volumetric Sun Shafts,
 * Specular Micro-Reflections, Shadow-Occluded Ground Light Pools, and Atmospheric Depth Grade.
 *
 * Replaces static screen-locked overlays with world-anchored (`camX`, `camY`, `zoom`) 2.5D lighting
 * aligned with `SanctuaryLight` (`AZ=142°`, `EL=48°`) and `ObliqueCamera.OBLIQUE_Y_SCALE = 0.72f`.
 */
object SanctuarySunlightSystem {

  private data class SunAnchor(
    val worldX: Float,
    val worldY: Float,
    val radiusWorld: Float,
    val intensity: Float,
    val phaseOffset: Float,
  )

  private data class ShadowFootprint(
    val worldX: Float,
    val worldY: Float,
    val heightPx: Float,
    val radius: Float,
  )

  // Curated world-space solar corridors across the 3200x2760 Sanctuary island
  private val sanctuarySunAnchors = listOf(
    SunAnchor(1600f, 1380f, 310f, 1.00f, 0.0f), // Central Astrolabe Plaza & Fountain
    SunAnchor(1600f, 760f, 280f, 0.92f, 1.1f),  // Northern Tower Gate Forecourt
    SunAnchor(980f, 1080f, 240f, 0.85f, 2.3f),  // Summoning Hall Terrace
    SunAnchor(2220f, 1080f, 240f, 0.85f, 3.4f), // Hero Registry & Eastern Terrace
    SunAnchor(880f, 1680f, 260f, 0.88f, 4.2f),  // Western Training Arena
    SunAnchor(2280f, 1660f, 250f, 0.84f, 5.1f), // Armory & Synthesis Courtyard
    SunAnchor(1220f, 2040f, 230f, 0.82f, 1.7f), // Hero Quarters Garden
    SunAnchor(1980f, 2040f, 230f, 0.82f, 2.9f), // Restaurant / Tavern Patio
    SunAnchor(1600f, 2340f, 260f, 0.86f, 0.6f), // Southern Gate Causeway
  )

  // Major Sanctuary landmark shadow casters for real-time ground light-pool occlusion
  private val landmarkShadowCasters = listOf(
    ShadowFootprint(1960f, 610f, 220f, 140f),  // Tower Gate Monument
    ShadowFootprint(960f, 880f, 190f, 130f),   // Summoning Hall
    ShadowFootprint(2280f, 920f, 180f, 115f),  // Hero Registry
    ShadowFootprint(2360f, 1460f, 180f, 115f), // Armory
    ShadowFootprint(2380f, 1920f, 185f, 120f), // Restaurant
    ShadowFootprint(1020f, 1920f, 185f, 120f), // Hero Quarters
  )

  /**
   * PASS 1 (Ground-Plane Shader Pass): Drawn immediately after the 128x128 terrain tiles & roads,
   * BEFORE buildings, trees, and characters so ground sun pools, cloud drift shadows, and stone/water
   * specular micro-reflections sit genuinely ON the ground plane underneath actors and shadows.
   */
  fun DrawScope.drawGroundSunPoolsAndSpecularity(
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
  ) {
    val halfW = size.width / 2f
    val halfH = size.height / 2f
    val envLight = WorldEnvironment.lighting
    val daylightStrength = (0.55f + 0.45f * envLight.ambientIntensity).coerceIn(0.35f, 1.0f)

    // 1. Slow-drifting organic high-altitude cloud shadow bands across the 2.5D ground plane
    for (band in 0 until 4) {
      val worldBandX = ((band * 820f + timeSec * 22f) % 3400f) - 100f
      val worldBandY = 500f + band * 560f + sin(timeSec * 0.18f + band) * 90f
      val sx = ObliqueCamera.worldToScreenX(worldBandX, camX, zoom, halfW)
      val sy = ObliqueCamera.worldToScreenY(worldBandY, camY, zoom, halfH)
      val rx = 460f * zoom
      val ry = rx * ObliqueCamera.OBLIQUE_Y_SCALE
      if (sx + rx >= 0f && sx - rx <= size.width && sy + ry >= 0f && sy - ry <= size.height) {
        drawOval(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0x240B1024),
              Color(0x100B1024),
              Color.Transparent,
            ),
            center = Offset(sx, sy),
            radius = rx.coerceAtLeast(12f),
          ),
          topLeft = Offset(sx - rx, sy - ry),
          size = Size(rx * 2f, ry * 2f),
        )
      }
    }

    // 2. Foreshortened (Y=0.72) Sunlit Ground Pools with Building Shadow Occlusion Attenuation
    val isGoldenHour = envLight.hour in 5.5f..8.5f || envLight.hour in 16.5f..19.5f
    val coreSunColor = if (isGoldenHour) Color(0xFFFFE0B2) else Color(0xFFFFF6D6)
    val outerSunColor = if (isGoldenHour) Color(0xFFFFB74D) else Color(0xFFFFDF80)

    sanctuarySunAnchors.forEach { anchor ->
      val sx = ObliqueCamera.worldToScreenX(anchor.worldX, camX, zoom, halfW)
      val sy = ObliqueCamera.worldToScreenY(anchor.worldY, camY, zoom, halfH)
      val pulse = 0.94f + 0.06f * sin(timeSec * 0.9f + anchor.phaseOffset)
      val rx = anchor.radiusWorld * zoom * pulse
      val ry = rx * ObliqueCamera.OBLIQUE_Y_SCALE
      if (sx + rx < -40f || sx - rx > size.width + 40f || sy + ry < -40f || sy - ry > size.height + 40f) {
        return@forEach
      }

      val occlusion = computeShadowOcclusionFactor(anchor.worldX, anchor.worldY)
      val alphaCore = (0.19f * anchor.intensity * daylightStrength * occlusion).coerceIn(0.03f, 0.24f)
      val alphaMid = (0.10f * anchor.intensity * daylightStrength * occlusion).coerceIn(0.02f, 0.14f)

      drawOval(
        brush = Brush.radialGradient(
          colors = listOf(
            coreSunColor.copy(alpha = alphaCore),
            outerSunColor.copy(alpha = alphaMid),
            Color.Transparent,
          ),
          center = Offset(sx, sy),
          radius = rx.coerceAtLeast(8f),
        ),
        topLeft = Offset(sx - rx, sy - ry),
        size = Size(rx * 2f, ry * 2f),
        blendMode = BlendMode.Plus,
      )
    }

    // 3. HD-2D Shader Specular Micro-Reflections on Plaza Flagstones, Astrolabe Runes & Cobble Roads
    val plazaX = SanctuaryIslandGeometry.PLAZA_CENTER_X
    val plazaY = SanctuaryIslandGeometry.PLAZA_CENTER_Y
    val centerSx = ObliqueCamera.worldToScreenX(plazaX, camX, zoom, halfW)
    val centerSy = ObliqueCamera.worldToScreenY(plazaY, camY, zoom, halfH)
    val medallionRx = 224f * zoom
    val medallionRy = 152f * zoom
    if (
      centerSx + medallionRx >= 0f && centerSx - medallionRx <= size.width &&
      centerSy + medallionRy >= 0f && centerSy - medallionRy <= size.height
    ) {
      val ringAlpha = 0.22f + 0.08f * sin(timeSec * 2.1f)
      drawOval(
        color = Color(0xFFFFE57F).copy(alpha = ringAlpha),
        topLeft = Offset(centerSx - medallionRx * 0.88f, centerSy - medallionRy * 0.88f),
        size = Size(medallionRx * 1.76f, medallionRy * 1.76f),
        style = Stroke(width = (2.2f * zoom).coerceAtLeast(1f)),
        blendMode = BlendMode.Plus,
      )
      drawOval(
        color = Color(0xFF38BDF8).copy(alpha = ringAlpha * 0.85f),
        topLeft = Offset(centerSx - medallionRx * 0.58f, centerSy - medallionRy * 0.58f),
        size = Size(medallionRx * 1.16f, medallionRy * 1.16f),
        style = Stroke(width = (1.8f * zoom).coerceAtLeast(1f)),
        blendMode = BlendMode.Plus,
      )

      // 24 Specular stone-bevel & gold-filigree micro-glints around the plaza and main causeways
      for (i in 0 until 24) {
        val angle = i * (Math.PI * 2.0 / 24.0) + 0.25
        val dist = 95f + (i % 4) * 72f
        val gxWorld = plazaX + cos(angle).toFloat() * dist
        val gyWorld = plazaY + sin(angle).toFloat() * dist
        val shimmer = sin(timeSec * 2.8f + i * 1.37f)
        if (shimmer > 0.25f) {
          val gsx = ObliqueCamera.worldToScreenX(gxWorld, camX, zoom, halfW)
          val gsy = ObliqueCamera.worldToScreenY(gyWorld, camY, zoom, halfH)
          val glintAlpha = ((shimmer - 0.25f) / 0.75f) * 0.48f * daylightStrength
          val glintR = (3.5f + (i % 3) * 1.4f) * zoom
          drawCircle(
            color = if (i % 3 == 0) Color(0xFFE0F2FE).copy(alpha = glintAlpha)
            else Color(0xFFFFF8E1).copy(alpha = glintAlpha),
            radius = glintR,
            center = Offset(gsx, gsy),
            blendMode = BlendMode.Plus,
          )
        }
      }
    }
  }

  /**
   * PASS 2 (Volumetric Sun Shafts & Floating Golden Motes): Drawn in world space along `AZ=142°`.
   */
  fun DrawScope.drawVolumetricSunShaftsAndMotes(
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
    particlesEnabled: Boolean = true,
  ) {
    val halfW = size.width / 2f
    val halfH = size.height / 2f
    val envLight = WorldEnvironment.lighting
    val isGoldenHour = envLight.hour in 5.5f..8.5f || envLight.hour in 16.5f..19.5f
    val beamTint = if (isGoldenHour) Color(0xFFFFD180) else Color(0xFFFFF3C4)

    sanctuarySunAnchors.forEachIndexed { idx, anchor ->
      val targetSx = ObliqueCamera.worldToScreenX(anchor.worldX, camX, zoom, halfW)
      val targetSy = ObliqueCamera.worldToScreenY(anchor.worldY, camY, zoom, halfH)
      val beamHalfW = (anchor.radiusWorld * 0.48f) * zoom
      val beamLen = 520f * zoom

      if (
        targetSx + beamHalfW * 2f < -120f || targetSx - beamLen > size.width + 120f ||
        targetSy + beamHalfW < -120f || targetSy - beamLen > size.height + 120f
      ) {
        return@forEachIndexed
      }

      val originX = targetSx - beamLen * 0.68f
      val originY = targetSy - beamLen * 0.82f
      val sway = sin(timeSec * 0.55f + anchor.phaseOffset) * 14f * zoom
      val breath = 0.86f + 0.14f * sin(timeSec * 0.85f + idx * 0.9f)
      val shaftAlpha = (0.11f * anchor.intensity * breath).coerceIn(0.03f, 0.15f)

      val path = Path().apply {
        moveTo(originX - beamHalfW * 0.52f + sway, originY)
        lineTo(originX + beamHalfW * 0.52f + sway, originY)
        lineTo(targetSx + beamHalfW * 1.12f, targetSy + beamHalfW * 0.35f)
        lineTo(targetSx - beamHalfW * 0.95f, targetSy + beamHalfW * 0.35f)
        close()
      }

      drawPath(
        path = path,
        brush = Brush.linearGradient(
          colors = listOf(
            beamTint.copy(alpha = shaftAlpha * 1.15f),
            beamTint.copy(alpha = shaftAlpha * 0.65f),
            Color.Transparent,
          ),
          start = Offset(originX + sway, originY),
          end = Offset(targetSx, targetSy + beamHalfW * 0.35f),
        ),
        blendMode = BlendMode.Plus,
      )
    }

    if (particlesEnabled) {
      for (i in 0 until 32) {
        val anchor = sanctuarySunAnchors[i % sanctuarySunAnchors.size]
        val localAngle = (i * 1.618f) + timeSec * 0.28f
        val localDist = ((i * 37) % 160) * (0.6f + 0.4f * sin(timeSec * 0.4f + i))
        val wx = anchor.worldX + cos(localAngle) * localDist
        val wy = anchor.worldY + sin(localAngle) * localDist
        val elevZ = 18f + ((i * 19f + timeSec * 14f) % 95f)

        val sx = ObliqueCamera.worldToScreenX(wx, camX, zoom, halfW)
        val sy = ObliqueCamera.worldToScreenY(wy, camY, zoom, halfH, elevationZ = elevZ)
        if (sx in 0f..size.width && sy in 0f..size.height) {
          val twinkle = 0.45f + 0.55f * abs(sin(timeSec * 2.2f + i * 0.77f))
          val r = (1.6f + (i % 3) * 0.85f) * zoom
          drawCircle(
            color = Color(0xFFFFECB3).copy(alpha = 0.22f * twinkle),
            radius = r * 2.4f,
            center = Offset(sx, sy),
            blendMode = BlendMode.Plus,
          )
          drawCircle(
            color = Color(0xFFFFFDE7).copy(alpha = 0.78f * twinkle),
            radius = r,
            center = Offset(sx, sy),
            blendMode = BlendMode.Plus,
          )
        }
      }
    }
  }

  /**
   * PASS 3 (HD-2D Shader Atmospheric Depth-of-Field, NW Key Bloom & Cinematic Vignette):
   */
  fun DrawScope.drawHd2dAtmosphericDepthGrade(
    timeSec: Float,
  ) {
    val w = size.width
    val h = size.height
    val envLight = WorldEnvironment.lighting
    val isGoldenHour = envLight.hour in 5.5f..8.5f || envLight.hour in 16.5f..19.5f

    // 1. Upper-left NW Warm Solar Key-Light Bloom
    val nwBloomColor = if (isGoldenHour) Color(0x30FFCC80) else Color(0x2CFFECB3)
    drawRect(
      brush = Brush.radialGradient(
        colors = listOf(nwBloomColor, Color.Transparent),
        center = Offset(w * 0.14f, h * 0.12f),
        radius = (w.coerceAtLeast(h) * 0.72f).coerceAtLeast(10f),
      ),
      size = size,
      blendMode = BlendMode.Plus,
    )

    // 2. Far-Horizon Aerial Perspective Haze (Top 22% of screen)
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0x2838BDF8),
          Color(0x12FEF08A),
          Color.Transparent,
        ),
        startY = 0f,
        endY = h * 0.22f,
      ),
      size = Size(w, h * 0.22f),
      blendMode = BlendMode.Plus,
    )

    // 3. Subtle Foreground Tilt-Shift Depth & Edge Vignette
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color.Transparent,
          Color(0x18070913),
          Color(0x44070913),
        ),
        startY = h * 0.76f,
        endY = h,
      ),
      topLeft = Offset(0f, h * 0.76f),
      size = Size(w, h * 0.24f),
    )
  }

  /**
   * BUILDING INTERIORS HD-2D LIGHTING PASS:
   */
  fun DrawScope.drawInteriorHd2dWindowBeamsAndReflections(
    roomWidth: Float,
    roomHeight: Float,
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
    accentColor: Color,
  ) {
    val halfW = size.width / 2f
    val halfH = size.height / 2f

    val windowXs = listOf(roomWidth * 0.26f, roomWidth * 0.50f, roomWidth * 0.74f)
    windowXs.forEachIndexed { idx, winX ->
      val winTopX = ObliqueCamera.worldToScreenX(winX, camX, zoom, halfW)
      val winTopY = ObliqueCamera.worldToScreenY(88f, camY, zoom, halfH, elevationZ = 68f)
      val floorPoolX = ObliqueCamera.worldToScreenX(winX + 56f, camX, zoom, halfW)
      val floorPoolY = ObliqueCamera.worldToScreenY(240f, camY, zoom, halfH)
      val beamW = 54f * zoom

      drawOval(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0x38FFF3C4),
            accentColor.copy(alpha = 0.14f),
            Color.Transparent,
          ),
          center = Offset(floorPoolX, floorPoolY),
          radius = (beamW * 1.45f).coerceAtLeast(8f),
        ),
        topLeft = Offset(floorPoolX - beamW * 1.45f, floorPoolY - beamW * 1.0f),
        size = Size(beamW * 2.9f, beamW * 2.0f),
        blendMode = BlendMode.Plus,
      )

      val beamPath = Path().apply {
        moveTo(winTopX - beamW * 0.45f, winTopY)
        lineTo(winTopX + beamW * 0.45f, winTopY)
        lineTo(floorPoolX + beamW * 1.15f, floorPoolY + beamW * 0.4f)
        lineTo(floorPoolX - beamW * 0.85f, floorPoolY + beamW * 0.4f)
        close()
      }
      drawPath(
        path = beamPath,
        brush = Brush.linearGradient(
          colors = listOf(
            Color(0x30FFF8E1),
            accentColor.copy(alpha = 0.12f),
            Color.Transparent,
          ),
          start = Offset(winTopX, winTopY),
          end = Offset(floorPoolX, floorPoolY + beamW * 0.4f),
        ),
        blendMode = BlendMode.Plus,
      )

      for (m in 0 until 5) {
        val t = ((timeSec * 0.18f + m * 0.21f + idx * 0.31f) % 1f)
        val mx = winTopX + (floorPoolX - winTopX) * t + sin(timeSec * 1.5f + m + idx) * 10f * zoom
        val my = winTopY + (floorPoolY - winTopY) * t
        drawCircle(
          color = Color(0xFFFFF8E1).copy(alpha = 0.55f * (1f - abs(t - 0.5f) * 1.8f).coerceAtLeast(0f)),
          radius = (1.8f + (m % 2) * 0.8f) * zoom,
          center = Offset(mx, my),
          blendMode = BlendMode.Plus,
        )
      }
    }
  }

  /**
   * INSIDE TOWER BIOME HD-2D SHADER LIGHTING PASS:
   */
  fun DrawScope.drawTowerBiomeHd2dLighting(
    biome: BiomeTheme,
    worldWidth: Float,
    worldHeight: Float,
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
  ) {
    val halfW = size.width / 2f
    val halfH = size.height / 2f

    val primaryRayColor = when (biome) {
      BiomeTheme.FIRST_FIELD_MEADOW -> Color(0xFFFFF3C4)
      BiomeTheme.ELDERWOOD_FOREST -> Color(0xFF67E8F9)
      BiomeTheme.SUNKEN_RUINS -> Color(0xFF38BDF8)
      BiomeTheme.CRIMSON_CAVERN -> Color(0xFFFB923C)
      BiomeTheme.FALLEN_STAR_TEMPLE -> Color(0xFFC084FC)
      else -> Color(0xFFFFF3C4)
    }
    val secondaryPoolColor = when (biome) {
      BiomeTheme.FIRST_FIELD_MEADOW -> Color(0xFFFDE047)
      BiomeTheme.ELDERWOOD_FOREST -> Color(0xFF34D399)
      BiomeTheme.SUNKEN_RUINS -> Color(0xFFFBBF24)
      BiomeTheme.CRIMSON_CAVERN -> Color(0xFFEF4444)
      BiomeTheme.FALLEN_STAR_TEMPLE -> Color(0xFF38BDF8)
      else -> Color(0xFFFDE047)
    }

    for (i in 0 until 5) {
      val wx = worldWidth * (0.18f + i * 0.16f)
      val wy = worldHeight * (0.32f + ((i * 37) % 3) * 0.18f)
      val sx = ObliqueCamera.worldToScreenX(wx, camX, zoom, halfW)
      val sy = ObliqueCamera.worldToScreenY(wy, camY, zoom, halfH)
      val rx = 175f * zoom
      val ry = rx * ObliqueCamera.OBLIQUE_Y_SCALE

      if (sx + rx >= -80f && sx - rx <= size.width + 80f && sy + ry >= -80f && sy - ry <= size.height + 80f) {
        val pulse = 0.85f + 0.15f * sin(timeSec * 1.4f + i * 1.3f)
        drawOval(
          brush = Brush.radialGradient(
            colors = listOf(
              primaryRayColor.copy(alpha = 0.16f * pulse),
              secondaryPoolColor.copy(alpha = 0.08f * pulse),
              Color.Transparent,
            ),
            center = Offset(sx, sy),
            radius = rx.coerceAtLeast(10f),
          ),
          topLeft = Offset(sx - rx, sy - ry),
          size = Size(rx * 2f, ry * 2f),
          blendMode = BlendMode.Plus,
        )

        val beamW = 68f * zoom
        val originX = sx - 260f * zoom
        val originY = sy - 320f * zoom
        val shaft = Path().apply {
          moveTo(originX - beamW * 0.4f, originY)
          lineTo(originX + beamW * 0.4f, originY)
          lineTo(sx + beamW * 0.95f, sy + beamW * 0.25f)
          lineTo(sx - beamW * 0.85f, sy + beamW * 0.25f)
          close()
        }
        drawPath(
          path = shaft,
          brush = Brush.linearGradient(
            colors = listOf(
              primaryRayColor.copy(alpha = 0.11f * pulse),
              secondaryPoolColor.copy(alpha = 0.05f * pulse),
              Color.Transparent,
            ),
            start = Offset(originX, originY),
            end = Offset(sx, sy),
          ),
          blendMode = BlendMode.Plus,
        )
      }
    }
  }

  private fun computeShadowOcclusionFactor(
    wx: Float,
    wy: Float,
  ): Float {
    var factor = 1.0f
    for (caster in landmarkShadowCasters) {
      val (offWx, offWy) = SanctuaryLight.shadowWorldOffset(caster.heightPx)
      val shadowCenterX = caster.worldX + offWx * 0.85f
      val shadowCenterY = caster.worldY + offWy * 0.85f
      val dist = hypot(wx - shadowCenterX, wy - shadowCenterY)
      if (dist < caster.radius * 1.25f) {
        factor = factor.coerceAtMost(0.45f)
      }
    }
    return factor
  }
}
