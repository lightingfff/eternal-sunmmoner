package com.example.game.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.HeroClassType
import com.example.game.map.EnemyArchetype
import com.example.game.map.ObliqueCamera
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * FantasyHd2dSkillVfx: Cinematic HD-2D Shader-Style Skill & Combat Visual Effects for all 6 Hero
 * Classes, Master Summoner, and Tower Enemies/Bosses.
 *
 * Uses foreshortened 2.5D ground sigils (`OBLIQUE_Y_SCALE = 0.72f`), additive bloom (`BlendMode.Plus`),
 * multi-layered elemental crescent waves, specular cross-flares, and directional particle bursts.
 */
object FantasyHd2dSkillVfx {

  data class ClassSkillPalette(
    val primary: Color,
    val secondary: Color,
    val coreWhite: Color = Color(0xFFFFFDE7),
    val groundGlow: Color,
  )

  fun paletteFor(heroClass: HeroClassType, isMaster: Boolean): ClassSkillPalette {
    if (isMaster) {
      return ClassSkillPalette(
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFFF5B841),
        groundGlow = Color(0xFF7DD3FC),
      )
    }
    return when (heroClass) {
      HeroClassType.KNIGHT -> ClassSkillPalette(
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFFFACC15),
        groundGlow = Color(0xFFBAE6FD),
      )
      HeroClassType.ARCHER -> ClassSkillPalette(
        primary = Color(0xFF34D399),
        secondary = Color(0xFFFDE047),
        groundGlow = Color(0xFF6EE7B7),
      )
      HeroClassType.MAGE -> ClassSkillPalette(
        primary = Color(0xFFC084FC),
        secondary = Color(0xFF38BDF8),
        groundGlow = Color(0xFFE879F9),
      )
      HeroClassType.ASSASSIN -> ClassSkillPalette(
        primary = Color(0xFFF43F5E),
        secondary = Color(0xFFA855F7),
        groundGlow = Color(0xFFFB7185),
      )
      HeroClassType.PRIEST -> ClassSkillPalette(
        primary = Color(0xFFFDE047),
        secondary = Color(0xFF34D399),
        groundGlow = Color(0xFFFEF08A),
      )
      HeroClassType.BERSERKER -> ClassSkillPalette(
        primary = Color(0xFFFB923C),
        secondary = Color(0xFFEF4444),
        groundGlow = Color(0xFFFCA5A5),
      )
    }
  }

  /**
   * Drawn BEFORE the character sprite on the foreshortened `Y = 0.72` ground plane:
   * - Ambient subtle class aura under feet
   * - Expanding runic shockwave & rotating sigil during attacks / signature skills
   */
  fun DrawScope.drawHeroGroundSigilAndShockwave(
    cx: Float,
    cy: Float,
    zoom: Float,
    heroClass: HeroClassType,
    isMaster: Boolean,
    attackAnimProgress: Float,
    skillAnimation: Boolean,
    timeSec: Float,
  ) {
    val pal = paletteFor(heroClass, isMaster)

    // 1. Subtle HD-2D ambient class foot light pool so characters feel grounded in the world
    val idlePulse = 0.85f + 0.15f * sin(timeSec * 2.8f + cx * 0.01f)
    val baseRx = (if (isMaster) 28f else 22f) * zoom * idlePulse
    val baseRy = baseRx * ObliqueCamera.OBLIQUE_Y_SCALE
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(
          pal.groundGlow.copy(alpha = if (isMaster) 0.22f else 0.14f),
          pal.primary.copy(alpha = 0.05f),
          Color.Transparent,
        ),
        center = Offset(cx, cy + 2f * zoom),
        radius = baseRx.coerceAtLeast(6f),
      ),
      topLeft = Offset(cx - baseRx, cy + 2f * zoom - baseRy),
      size = Size(baseRx * 2f, baseRy * 2f),
      blendMode = BlendMode.Plus,
    )

    if (attackAnimProgress <= 0.04f && !skillAnimation) return

    // 2. Active Combat / Skill Expanding Foreshortened Runic Sigil on Ground
    val p = attackAnimProgress.coerceIn(0f, 1f)
    val fade = (1f - p * 0.82f).coerceIn(0.15f, 1f)
    val boost = if (skillAnimation) 1.42f else 1.0f
    val outerRx = (30f + 36f * p) * zoom * boost
    val outerRy = outerRx * ObliqueCamera.OBLIQUE_Y_SCALE
    val innerRx = outerRx * 0.64f
    val innerRy = outerRy * 0.64f

    // Additive radial ground burst
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(
          pal.coreWhite.copy(alpha = 0.28f * fade),
          pal.primary.copy(alpha = 0.22f * fade),
          pal.secondary.copy(alpha = 0.10f * fade),
          Color.Transparent,
        ),
        center = Offset(cx, cy + 2f * zoom),
        radius = outerRx.coerceAtLeast(8f),
      ),
      topLeft = Offset(cx - outerRx, cy + 2f * zoom - outerRy),
      size = Size(outerRx * 2f, outerRy * 2f),
      blendMode = BlendMode.Plus,
    )

    // Outer & inner runic ellipses
    drawOval(
      color = pal.primary.copy(alpha = 0.85f * fade),
      topLeft = Offset(cx - outerRx, cy + 2f * zoom - outerRy),
      size = Size(outerRx * 2f, outerRy * 2f),
      style = Stroke(width = (2.4f * zoom * boost).coerceAtLeast(1f)),
      blendMode = BlendMode.Plus,
    )
    drawOval(
      color = pal.secondary.copy(alpha = 0.78f * fade),
      topLeft = Offset(cx - innerRx, cy + 2f * zoom - innerRy),
      size = Size(innerRx * 2f, innerRy * 2f),
      style = Stroke(width = (1.6f * zoom * boost).coerceAtLeast(1f)),
      blendMode = BlendMode.Plus,
    )

    // 6 Rotating arcane spokes inside the ground sigil
    for (i in 0 until 6) {
      val ang = timeSec * 3.8f + i * (Math.PI.toFloat() / 3f)
      val cA = cos(ang)
      val sA = sin(ang)
      drawLine(
        color = pal.coreWhite.copy(alpha = 0.68f * fade),
        start = Offset(cx + cA * innerRx * 0.45f, cy + 2f * zoom + sA * innerRy * 0.45f),
        end = Offset(cx + cA * outerRx * 0.96f, cy + 2f * zoom + sA * outerRy * 0.96f),
        strokeWidth = (1.5f * zoom).coerceAtLeast(0.8f),
        blendMode = BlendMode.Plus,
      )
    }
  }

  /**
   * Drawn AFTER the character sprite:
   * - Subtle NW shoulder/helm specular rim glint
   * - Full multi-layered HD-2D class-specific slash waves, arcane pillars, arrow volleys, and sparks!
   */
  fun DrawScope.drawHeroSkillAndAttackOverlay(
    cx: Float,
    cy: Float,
    zoom: Float,
    heroClass: HeroClassType,
    isMaster: Boolean,
    facingRight: Boolean,
    attackAnimProgress: Float,
    skillAnimation: Boolean,
    timeSec: Float,
  ) {
    if (attackAnimProgress <= 0.05f && !skillAnimation) return

    val pal = paletteFor(heroClass, isMaster)
    val p = attackAnimProgress.coerceIn(0f, 1f)
    val fade = (1f - p * 0.78f).coerceIn(0.18f, 1f)
    val dir = if (facingRight) 1f else -1f
    val boost = if (skillAnimation) 1.38f else 1.0f
    val torsoY = cy - 34f * zoom

    when (heroClass) {
      HeroClassType.KNIGHT -> {
        // Excalibur Golden-Azure Twin Crescent Wave + Solar Pillar
        val arcW = (76f + 28f * p) * zoom * boost
        val arcH = (58f + 18f * p) * zoom * boost
        val startAng = if (facingRight) -68f else 112f
        // Outer chromatic azure glow
        drawArc(
          color = pal.primary.copy(alpha = 0.65f * fade),
          startAngle = startAng,
          sweepAngle = 136f,
          useCenter = false,
          topLeft = Offset(cx - arcW * 0.5f + dir * 10f * zoom, torsoY - arcH * 0.5f),
          size = Size(arcW, arcH),
          style = Stroke(width = 7.5f * zoom * boost),
          blendMode = BlendMode.Plus,
        )
        // Mid golden blade energy
        drawArc(
          color = pal.secondary.copy(alpha = 0.88f * fade),
          startAngle = startAng + 6f,
          sweepAngle = 124f,
          useCenter = false,
          topLeft = Offset(cx - arcW * 0.48f + dir * 10f * zoom, torsoY - arcH * 0.48f),
          size = Size(arcW * 0.96f, arcH * 0.96f),
          style = Stroke(width = 4.2f * zoom * boost),
          blendMode = BlendMode.Plus,
        )
        // Crisp white-hot core
        drawArc(
          color = pal.coreWhite.copy(alpha = 0.95f * fade),
          startAngle = startAng + 12f,
          sweepAngle = 112f,
          useCenter = false,
          topLeft = Offset(cx - arcW * 0.46f + dir * 10f * zoom, torsoY - arcH * 0.46f),
          size = Size(arcW * 0.92f, arcH * 0.92f),
          style = Stroke(width = 2.0f * zoom * boost),
          blendMode = BlendMode.Plus,
        )
        if (skillAnimation || p in 0.15f..0.65f) {
          // Heavenward solar light pillar on impact edge
          val pillarX = cx + dir * (28f + 22f * p) * zoom
          drawRect(
            brush = Brush.verticalGradient(
              colors = listOf(
                Color.Transparent,
                pal.secondary.copy(alpha = 0.42f * fade),
                pal.coreWhite.copy(alpha = 0.75f * fade),
              ),
              startY = cy - 115f * zoom * boost,
              endY = cy + 4f * zoom,
            ),
            topLeft = Offset(pillarX - 10f * zoom * boost, cy - 115f * zoom * boost),
            size = Size(20f * zoom * boost, 119f * zoom * boost),
            blendMode = BlendMode.Plus,
          )
        }
      }

      HeroClassType.ARCHER -> {
        // Wind-Sylph Emerald Vortex + Triple Golden-Tipped Sonic Arrow Streaks
        val bowX = cx + dir * 18f * zoom
        val bowY = torsoY - 2f * zoom
        // Sonic wind-ring around bow
        val ringR = (18f + 22f * p) * zoom * boost
        drawCircle(
          color = pal.primary.copy(alpha = 0.72f * fade),
          radius = ringR,
          center = Offset(bowX, bowY),
          style = Stroke(width = 2.6f * zoom),
          blendMode = BlendMode.Plus,
        )
        // 3 Fan-shot luminous arrow streaks
        for (shot in -1..1) {
          val spreadY = shot * 11f * zoom * (0.5f + p)
          val startX = bowX + dir * (8f * p) * zoom
          val endX = bowX + dir * (48f + 56f * p) * zoom * boost
          drawLine(
            color = pal.primary.copy(alpha = 0.65f * fade),
            start = Offset(startX, bowY + spreadY * 0.3f),
            end = Offset(endX, bowY + spreadY),
            strokeWidth = 4.6f * zoom,
            blendMode = BlendMode.Plus,
          )
          drawLine(
            color = pal.secondary.copy(alpha = 0.95f * fade),
            start = Offset(startX + dir * 10f * zoom, bowY + spreadY * 0.45f),
            end = Offset(endX + dir * 4f * zoom, bowY + spreadY),
            strokeWidth = 2.0f * zoom,
            blendMode = BlendMode.Plus,
          )
        }
      }

      HeroClassType.MAGE -> {
        // Astral Supernova Orb + Crackling Arcane Runes
        val castX = cx + dir * 24f * zoom
        val castY = torsoY - 8f * zoom
        val novaR = (22f + 34f * p) * zoom * boost
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              pal.coreWhite.copy(alpha = 0.90f * fade),
              pal.secondary.copy(alpha = 0.62f * fade),
              pal.primary.copy(alpha = 0.28f * fade),
              Color.Transparent,
            ),
            center = Offset(castX, castY),
            radius = novaR.coerceAtLeast(6f),
          ),
          radius = novaR,
          center = Offset(castX, castY),
          blendMode = BlendMode.Plus,
        )
        // Rotating starlight cross-flare rays
        for (r in 0 until 4) {
          val ang = timeSec * 4.5f + r * (Math.PI.toFloat() / 4f)
          val rx = cos(ang) * novaR * 1.15f
          val ry = sin(ang) * novaR * 1.15f
          drawLine(
            color = pal.coreWhite.copy(alpha = 0.85f * fade),
            start = Offset(castX - rx, castY - ry),
            end = Offset(castX + rx, castY + ry),
            strokeWidth = 2.2f * zoom,
            blendMode = BlendMode.Plus,
          )
        }
      }

      HeroClassType.ASSASSIN -> {
        // Blood-Moon Twin Phantom X-Slash
        val slashCx = cx + dir * (22f + 16f * p) * zoom
        val span = (34f + 22f * p) * zoom * boost
        // Diagonal Slash 1
        drawLine(
          color = pal.secondary.copy(alpha = 0.72f * fade),
          start = Offset(slashCx - span, torsoY - span * 0.75f),
          end = Offset(slashCx + span, torsoY + span * 0.75f),
          strokeWidth = 6.2f * zoom * boost,
          blendMode = BlendMode.Plus,
        )
        drawLine(
          color = pal.coreWhite.copy(alpha = 0.95f * fade),
          start = Offset(slashCx - span * 0.85f, torsoY - span * 0.64f),
          end = Offset(slashCx + span * 0.85f, torsoY + span * 0.64f),
          strokeWidth = 2.2f * zoom * boost,
          blendMode = BlendMode.Plus,
        )
        // Diagonal Slash 2
        drawLine(
          color = pal.primary.copy(alpha = 0.82f * fade),
          start = Offset(slashCx - span, torsoY + span * 0.75f),
          end = Offset(slashCx + span, torsoY - span * 0.75f),
          strokeWidth = 6.2f * zoom * boost,
          blendMode = BlendMode.Plus,
        )
        drawLine(
          color = pal.coreWhite.copy(alpha = 0.95f * fade),
          start = Offset(slashCx - span * 0.85f, torsoY + span * 0.64f),
          end = Offset(slashCx + span * 0.85f, torsoY - span * 0.64f),
          strokeWidth = 2.2f * zoom * boost,
          blendMode = BlendMode.Plus,
        )
      }

      HeroClassType.PRIEST -> {
        // Seraphim Golden Sanctuary Halo + Ascending Radiant Pillars
        val haloY = cy - 64f * zoom
        val haloRx = (24f + 14f * p) * zoom * boost
        val haloRy = haloRx * 0.42f
        drawOval(
          color = pal.primary.copy(alpha = 0.88f * fade),
          topLeft = Offset(cx - haloRx, haloY - haloRy),
          size = Size(haloRx * 2f, haloRy * 2f),
          style = Stroke(width = 3.0f * zoom),
          blendMode = BlendMode.Plus,
        )
        drawRect(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color.Transparent,
              pal.primary.copy(alpha = 0.35f * fade),
              pal.secondary.copy(alpha = 0.52f * fade),
            ),
            startY = cy - 110f * zoom * boost,
            endY = cy + 6f * zoom,
          ),
          topLeft = Offset(cx - 22f * zoom * boost, cy - 110f * zoom * boost),
          size = Size(44f * zoom * boost, 116f * zoom * boost),
          blendMode = BlendMode.Plus,
        )
      }

      HeroClassType.BERSERKER -> {
        // Hellfire Volcanic Magma Cleave + Erupting Flame Arc
        val arcW = (84f + 34f * p) * zoom * boost
        val arcH = (64f + 22f * p) * zoom * boost
        val startAng = if (facingRight) -75f else 105f
        drawArc(
          color = pal.secondary.copy(alpha = 0.78f * fade),
          startAngle = startAng,
          sweepAngle = 150f,
          useCenter = false,
          topLeft = Offset(cx - arcW * 0.5f + dir * 12f * zoom, torsoY - arcH * 0.5f),
          size = Size(arcW, arcH),
          style = Stroke(width = 9.0f * zoom * boost),
          blendMode = BlendMode.Plus,
        )
        drawArc(
          color = pal.primary.copy(alpha = 0.92f * fade),
          startAngle = startAng + 8f,
          sweepAngle = 134f,
          useCenter = false,
          topLeft = Offset(cx - arcW * 0.47f + dir * 12f * zoom, torsoY - arcH * 0.47f),
          size = Size(arcW * 0.94f, arcH * 0.94f),
          style = Stroke(width = 4.6f * zoom * boost),
          blendMode = BlendMode.Plus,
        )
        drawArc(
          color = pal.coreWhite.copy(alpha = 0.95f * fade),
          startAngle = startAng + 16f,
          sweepAngle = 118f,
          useCenter = false,
          topLeft = Offset(cx - arcW * 0.44f + dir * 12f * zoom, torsoY - arcH * 0.44f),
          size = Size(arcW * 0.88f, arcH * 0.88f),
          style = Stroke(width = 2.2f * zoom * boost),
          blendMode = BlendMode.Plus,
        )
      }
    }

    // Universal HD-2D Specular Impact Sparks & Embers flying outward during attack/skill
    for (sp in 0 until 7) {
      val spAng = (if (facingRight) -0.9f else 2.24f) + sp * 0.31f + p * 0.6f
      val dist = (18f + 46f * p + (sp % 3) * 11f) * zoom * boost
      val sx = cx + dir * 10f * zoom + cos(spAng) * dist
      val sy = torsoY + sin(spAng) * dist * 0.75f
      val r = (2.2f + (sp % 2) * 1.2f) * zoom * fade
      drawCircle(
        color = if (sp % 2 == 0) pal.coreWhite.copy(alpha = 0.90f * fade)
        else pal.secondary.copy(alpha = 0.82f * fade),
        radius = r,
        center = Offset(sx, sy),
        blendMode = BlendMode.Plus,
      )
    }
  }

  /**
   * Drawn for Enemies & Bosses:
   * - Bosses get a menacing foreshortened crimson-violet abyssal ground aura & floating embers
   * - Attacking enemies/bosses emit a sharp crimson-violet claw/weapon shockwave arc
   */
  fun DrawScope.drawEnemyCombatAuraAndSlash(
    cx: Float,
    cy: Float,
    zoom: Float,
    archetype: EnemyArchetype,
    facingRight: Boolean,
    attackAnimProgress: Float,
    timeSec: Float,
  ) {
    if (archetype.isBoss) {
      val pulse = 0.82f + 0.18f * sin(timeSec * 3.4f)
      val rx = 44f * zoom * pulse
      val ry = rx * ObliqueCamera.OBLIQUE_Y_SCALE
      drawOval(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0x40E11D48),
            Color(0x227C3AED),
            Color.Transparent,
          ),
          center = Offset(cx, cy + 4f * zoom),
          radius = rx.coerceAtLeast(8f),
        ),
        topLeft = Offset(cx - rx, cy + 4f * zoom - ry),
        size = Size(rx * 2f, ry * 2f),
        blendMode = BlendMode.Plus,
      )
    }

    if (attackAnimProgress > 0.08f) {
      val p = attackAnimProgress.coerceIn(0f, 1f)
      val fade = (1f - p * 0.75f).coerceIn(0.2f, 1f)
      val dir = if (facingRight) 1f else -1f
      val scale = if (archetype.isBoss) 1.55f else 1.0f
      val arcW = 62f * zoom * scale
      val arcH = 46f * zoom * scale
      val startAng = if (facingRight) -60f else 120f
      drawArc(
        color = Color(0xFFF43F5E).copy(alpha = 0.78f * fade),
        startAngle = startAng,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = Offset(cx - arcW * 0.5f + dir * 10f * zoom, cy - 38f * zoom * scale - arcH * 0.5f),
        size = Size(arcW, arcH),
        style = Stroke(width = 5.0f * zoom * scale),
        blendMode = BlendMode.Plus,
      )
      drawArc(
        color = Color(0xFFFFE4E6).copy(alpha = 0.90f * fade),
        startAngle = startAng + 10f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = Offset(cx - arcW * 0.46f + dir * 10f * zoom, cy - 38f * zoom * scale - arcH * 0.46f),
        size = Size(arcW * 0.92f, arcH * 0.92f),
        style = Stroke(width = 2.0f * zoom * scale),
        blendMode = BlendMode.Plus,
      )
    }
  }
}
