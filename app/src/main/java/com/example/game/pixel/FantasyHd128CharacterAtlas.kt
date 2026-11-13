package com.example.game.pixel

import androidx.compose.ui.graphics.ImageBitmap
import com.example.data.model.HeroClassType
import com.example.game.map.EnemyArchetype
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * FantasyHd128CharacterAtlas: High-Definition 2.5D Anime Dark-Fantasy Character & Monster
 * Sprite & Animation Atlas.
 *
 * Replaces boxy low-res 38x50 rectangles with sculpted 76x100 (Heroes/Master) and
 * 84x96 / 116x128 (Monsters & Bosses) multi-shaded pixel art featuring:
 * - 6-frame wind-rippled breathing Idle animations
 * - 8-frame articulated Walk/Run cycles with cape/hair physics & opposite arm swing
 * - 4-frame dynamic Attack/Skill animations with weapon smear arcs & rune glow
 * - Tapered limbs, NMM metallic armor shading, multi-layered hair locks, and anime eyes.
 */
object FantasyHd128CharacterAtlas {

  const val HD_CHAR_W = 76
  const val HD_CHAR_H = 100
  const val HD_ENEMY_W = 84
  const val HD_ENEMY_H = 96
  const val HD_BOSS_W = 116
  const val HD_BOSS_H = 128

  class HdAnimatedCharacterSheet(
    val idleRight: List<ImageBitmap>,
    val idleLeft: List<ImageBitmap>,
    val walkRight: List<ImageBitmap>,
    val walkLeft: List<ImageBitmap>,
    val idleBackRight: List<ImageBitmap>,
    val idleBackLeft: List<ImageBitmap>,
    val walkBackRight: List<ImageBitmap>,
    val walkBackLeft: List<ImageBitmap>,
    val attackRight: List<ImageBitmap>,
    val attackLeft: List<ImageBitmap>,
    val hurtRight: ImageBitmap,
    val hurtLeft: ImageBitmap,
    val restRight: ImageBitmap,
    val restLeft: ImageBitmap,
  ) {
    fun selectFrame(
      facingRight: Boolean,
      isMoving: Boolean,
      attackProgress: Float,
      hurtFlash: Float,
      isResting: Boolean,
      timeSec: Float,
      seed: Float = 0f,
      facingNorth: Boolean = false,
    ): ImageBitmap {
      if (hurtFlash > 0.35f) {
        return if (facingRight) hurtRight else hurtLeft
      }
      if (isResting) {
        return if (facingRight) restRight else restLeft
      }
      if (attackProgress > 0.04f) {
        val list = if (facingRight) attackRight else attackLeft
        // attackProgress goes 1.0 -> 0.0 during an attack
        val elapsed = (1f - attackProgress).coerceIn(0f, 0.999f)
        val idx = (elapsed * list.size).toInt().coerceIn(0, list.lastIndex)
        return list[idx]
      }
      if (isMoving) {
        val list = when {
          facingNorth && facingRight -> walkBackRight
          facingNorth && !facingRight -> walkBackLeft
          facingRight -> walkRight
          else -> walkLeft
        }
        val idx = ((timeSec * 11.5f + seed * 3.7f).toInt() and 0x7FFFFFFF) % list.size
        return list[idx]
      }
      val list = when {
        facingNorth && facingRight -> idleBackRight
        facingNorth && !facingRight -> idleBackLeft
        facingRight -> idleRight
        else -> idleLeft
      }
      val idx = ((timeSec * 4.2f + seed * 2.3f).toInt() and 0x7FFFFFFF) % list.size
      return list[idx]
    }
  }

  val masterSheet: HdAnimatedCharacterSheet by lazy {
    buildHeroOrMasterSheet(isMaster = true, heroClass = HeroClassType.MAGE)
  }

  private val heroCache = ConcurrentHashMap<HeroClassType, HdAnimatedCharacterSheet>()
  private val enemyCache = ConcurrentHashMap<EnemyArchetype, HdAnimatedCharacterSheet>()

  fun heroSheet(heroClass: HeroClassType): HdAnimatedCharacterSheet =
    heroCache.getOrPut(heroClass) { buildHeroOrMasterSheet(isMaster = false, heroClass = heroClass) }

  fun enemySheet(archetype: EnemyArchetype): HdAnimatedCharacterSheet =
    enemyCache.getOrPut(archetype) { buildEnemySheet(archetype) }

  private sealed interface AnimFrame {
    data class Idle(val frame: Int) : AnimFrame // 0..5
    data class Walk(val frame: Int) : AnimFrame // 0..7
    data class Attack(val frame: Int) : AnimFrame // 0..3
    data object Hurt : AnimFrame
    data object Rest : AnimFrame
  }

  // ==================== PALETTE & SHADING HELPERS ====================

  private data class ClassPalette(
    val armorDark: Int,
    val armorMid: Int,
    val armorHi: Int,
    val armorSpec: Int,
    val clothDark: Int,
    val clothMid: Int,
    val clothHi: Int,
    val capeDark: Int,
    val capeMid: Int,
    val capeHi: Int,
    val trimGold: Int,
    val trimSpec: Int,
    val hairDark: Int,
    val hairMid: Int,
    val hairHi: Int,
    val eyeIris: Int,
    val eyeGlow: Int,
    val weaponCore: Int,
    val weaponEdge: Int,
    val weaponGlow: Int,
  )

  private fun paletteFor(isMaster: Boolean, heroClass: HeroClassType): ClassPalette {
    if (isMaster) {
      return ClassPalette(
        armorDark = 0xFF1A1636.toInt(),
        armorMid = 0xFF2E2759.toInt(),
        armorHi = 0xFF4C418C.toInt(),
        armorSpec = 0xFF8B7FC7.toInt(),
        clothDark = 0xFF120E24.toInt(),
        clothMid = 0xFF211A3D.toInt(),
        clothHi = 0xFF382D63.toInt(),
        capeDark = 0xFF2A0B4E.toInt(),
        capeMid = 0xFF4C1D95.toInt(),
        capeHi = 0xFF7C3AED.toInt(),
        trimGold = 0xFFD99B26.toInt(),
        trimSpec = 0xFFFFF3B0.toInt(),
        hairDark = 0xFF0F172A.toInt(),
        hairMid = 0xFF1E293B.toInt(),
        hairHi = 0xFF64748B.toInt(),
        eyeIris = 0xFF0284C7.toInt(),
        eyeGlow = 0xFF7DD3FC.toInt(),
        weaponCore = 0xFFD99B26.toInt(),
        weaponEdge = 0xFFFFF3B0.toInt(),
        weaponGlow = 0xFF38BDF8.toInt(),
      )
    }
    return when (heroClass) {
      HeroClassType.KNIGHT -> ClassPalette(
        armorDark = 0xFF334155.toInt(),
        armorMid = 0xFF64748B.toInt(),
        armorHi = 0xFFCBD5E1.toInt(),
        armorSpec = 0xFFF8FAFC.toInt(),
        clothDark = 0xFF1E3A8A.toInt(),
        clothMid = 0xFF1D4ED8.toInt(),
        clothHi = 0xFF3B82F6.toInt(),
        capeDark = 0xFF172554.toInt(),
        capeMid = 0xFF1D4ED8.toInt(),
        capeHi = 0xFF60A5FA.toInt(),
        trimGold = 0xFFD99B26.toInt(),
        trimSpec = 0xFFFEF08A.toInt(),
        hairDark = 0xFF92400E.toInt(),
        hairMid = 0xFFD97706.toInt(),
        hairHi = 0xFFFDE68A.toInt(),
        eyeIris = 0xFF0284C7.toInt(),
        eyeGlow = 0xFFBAE6FD.toInt(),
        weaponCore = 0xFF94A3B8.toInt(),
        weaponEdge = 0xFFFFFFFF.toInt(),
        weaponGlow = 0xFF38BDF8.toInt(),
      )
      HeroClassType.ARCHER -> ClassPalette(
        armorDark = 0xFF143628.toInt(),
        armorMid = 0xFF2D6A4F.toInt(),
        armorHi = 0xFF52B788.toInt(),
        armorSpec = 0xFF95D5B2.toInt(),
        clothDark = 0xFF3B2314.toInt(),
        clothMid = 0xFF5C3A21.toInt(),
        clothHi = 0xFF8A5A36.toInt(),
        capeDark = 0xFF0F291E.toInt(),
        capeMid = 0xFF1B4332.toInt(),
        capeHi = 0xFF40916C.toInt(),
        trimGold = 0xFFD97706.toInt(),
        trimSpec = 0xFFFDE68A.toInt(),
        hairDark = 0xFF7F1D1D.toInt(),
        hairMid = 0xFFDC2626.toInt(),
        hairHi = 0xFFFCA5A5.toInt(),
        eyeIris = 0xFF059669.toInt(),
        eyeGlow = 0xFF6EE7B7.toInt(),
        weaponCore = 0xFF92400E.toInt(),
        weaponEdge = 0xFFFDE68A.toInt(),
        weaponGlow = 0xFF34D399.toInt(),
      )
      HeroClassType.MAGE -> ClassPalette(
        armorDark = 0xFF2E1065.toInt(),
        armorMid = 0xFF581C87.toInt(),
        armorHi = 0xFF9333EA.toInt(),
        armorSpec = 0xFFE9D5FF.toInt(),
        clothDark = 0xFF1E1B4B.toInt(),
        clothMid = 0xFF312E81.toInt(),
        clothHi = 0xFF4F46E5.toInt(),
        capeDark = 0xFF240A47.toInt(),
        capeMid = 0xFF4C1D95.toInt(),
        capeHi = 0xFF8B5CF6.toInt(),
        trimGold = 0xFFEAB308.toInt(),
        trimSpec = 0xFFFEF9C3.toInt(),
        hairDark = 0xFF0369A1.toInt(),
        hairMid = 0xFF38BDF8.toInt(),
        hairHi = 0xFFE0F2FE.toInt(),
        eyeIris = 0xFF9333EA.toInt(),
        eyeGlow = 0xFFF0ABFC.toInt(),
        weaponCore = 0xFF7E22CE.toInt(),
        weaponEdge = 0xFFFDF4FF.toInt(),
        weaponGlow = 0xFFE879F9.toInt(),
      )
      HeroClassType.ASSASSIN -> ClassPalette(
        armorDark = 0xFF0F172A.toInt(),
        armorMid = 0xFF1E293B.toInt(),
        armorHi = 0xFF475569.toInt(),
        armorSpec = 0xFF94A3B8.toInt(),
        clothDark = 0xFF18181B.toInt(),
        clothMid = 0xFF27272A.toInt(),
        clothHi = 0xFF3F3F46.toInt(),
        capeDark = 0xFF4C0519.toInt(),
        capeMid = 0xFF9F1239.toInt(),
        capeHi = 0xFFF43F5E.toInt(),
        trimGold = 0xFFE11D48.toInt(),
        trimSpec = 0xFFFECDD3.toInt(),
        hairDark = 0xFF475569.toInt(),
        hairMid = 0xFF94A3B8.toInt(),
        hairHi = 0xFFF1F5F9.toInt(),
        eyeIris = 0xFFBE123C.toInt(),
        eyeGlow = 0xFFFB7185.toInt(),
        weaponCore = 0xFF334155.toInt(),
        weaponEdge = 0xFFF8FAFC.toInt(),
        weaponGlow = 0xFFF43F5E.toInt(),
      )
      HeroClassType.PRIEST -> ClassPalette(
        armorDark = 0xFF94A3B8.toInt(),
        armorMid = 0xFFE2E8F0.toInt(),
        armorHi = 0xFFF8FAFC.toInt(),
        armorSpec = 0xFFFFFFFF.toInt(),
        clothDark = 0xFF075985.toInt(),
        clothMid = 0xFF0284C7.toInt(),
        clothHi = 0xFF38BDF8.toInt(),
        capeDark = 0xFF0C4A6E.toInt(),
        capeMid = 0xFF0369A1.toInt(),
        capeHi = 0xFF7DD3FC.toInt(),
        trimGold = 0xFFF59E0B.toInt(),
        trimSpec = 0xFFFEF9C3.toInt(),
        hairDark = 0xFFB45309.toInt(),
        hairMid = 0xFFFBBF24.toInt(),
        hairHi = 0xFFFEF9C3.toInt(),
        eyeIris = 0xFF0284C7.toInt(),
        eyeGlow = 0xFFBAE6FD.toInt(),
        weaponCore = 0xFFD97706.toInt(),
        weaponEdge = 0xFFFFFBEB.toInt(),
        weaponGlow = 0xFF34D399.toInt(),
      )
      HeroClassType.BERSERKER -> ClassPalette(
        armorDark = 0xFF2A0A0A.toInt(),
        armorMid = 0xFF5C1414.toInt(),
        armorHi = 0xFF991B1B.toInt(),
        armorSpec = 0xFFF87171.toInt(),
        clothDark = 0xFF1C1917.toInt(),
        clothMid = 0xFF382E2B.toInt(),
        clothHi = 0xFF574944.toInt(),
        capeDark = 0xFF2E0909.toInt(),
        capeMid = 0xFF6B1414.toInt(),
        capeHi = 0xFFDC2626.toInt(),
        trimGold = 0xFFEA580C.toInt(),
        trimSpec = 0xFFFED7AA.toInt(),
        hairDark = 0xFF9A3412.toInt(),
        hairMid = 0xFFEA580C.toInt(),
        hairHi = 0xFFFDBA74.toInt(),
        eyeIris = 0xFFDC2626.toInt(),
        eyeGlow = 0xFFFEF08A.toInt(),
        weaponCore = 0xFF475569.toInt(),
        weaponEdge = 0xFFE2E8F0.toInt(),
        weaponGlow = 0xFFFB923C.toInt(),
      )
    }
  }

  private fun PixelCanvas.drawShadedPill(
    x0: Int,
    y0: Int,
    x1: Int,
    y1: Int,
    radius: Float,
    darkCol: Int,
    midCol: Int,
    hiCol: Int,
    specCol: Int = hiCol,
  ) {
    val minX = (minOf(x0, x1) - radius.toInt() - 1).coerceAtLeast(0)
    val maxX = (maxOf(x0, x1) + radius.toInt() + 1).coerceAtMost(width - 1)
    val minY = (minOf(y0, y1) - radius.toInt() - 1).coerceAtLeast(0)
    val maxY = (maxOf(y0, y1) + radius.toInt() + 1).coerceAtMost(height - 1)
    val dx = (x1 - x0).toFloat()
    val dy = (y1 - y0).toFloat()
    val lenSq = (dx * dx + dy * dy).coerceAtLeast(0.001f)

    for (py in minY..maxY) {
      for (px in minX..maxX) {
        val t = (((px - x0) * dx + (py - y0) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = x0 + t * dx
        val projY = y0 + t * dy
        val dist = sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY))
        if (dist <= radius) {
          // NW light bias (-0.7, -0.7)
          val nx = (px - projX) / radius.coerceAtLeast(1f)
          val ny = (py - projY) / radius.coerceAtLeast(1f)
          val light = (-nx * 0.72f - ny * 0.68f)
          val col = when {
            light > 0.48f -> specCol
            light > 0.12f -> hiCol
            light > -0.35f -> midCol
            else -> darkCol
          }
          set(px, py, col)
        }
      }
    }
  }

  private fun PixelCanvas.drawShadedOval(
    cx: Int,
    cy: Int,
    rx: Int,
    ry: Int,
    darkCol: Int,
    midCol: Int,
    hiCol: Int,
    specCol: Int = hiCol,
  ) {
    val rxF = rx.coerceAtLeast(1).toFloat()
    val ryF = ry.coerceAtLeast(1).toFloat()
    for (dy in -ry..ry) {
      for (dx in -rx..rx) {
        val nx = dx / rxF
        val ny = dy / ryF
        val r2 = nx * nx + ny * ny
        if (r2 <= 1.0f) {
          val light = -nx * 0.70f - ny * 0.70f
          val col = when {
            light > 0.52f && r2 < 0.72f -> specCol
            light > 0.14f -> hiCol
            light > -0.32f -> midCol
            else -> darkCol
          }
          set(cx + dx, cy + dy, col)
        }
      }
    }
  }

  // ==================== BUILD HERO / MASTER SHEET ====================

  private fun buildHeroOrMasterSheet(
    isMaster: Boolean,
    heroClass: HeroClassType,
  ): HdAnimatedCharacterSheet {
    fun renderPair(
      frame: AnimFrame,
      backView: Boolean = false,
      flashWhite: Boolean = false,
    ): Pair<ImageBitmap, ImageBitmap> {
      val right = drawHdHumanoid76x100(isMaster, heroClass, frame, backView, flashWhite)
      val left = right.flipHorizontalCanvas()
      return right.toImageBitmap() to left.toImageBitmap()
    }

    val idles = (0 until 6).map { renderPair(AnimFrame.Idle(it), backView = false) }
    val backIdles = (0 until 6).map { renderPair(AnimFrame.Idle(it), backView = true) }
    val walks = (0 until 8).map { renderPair(AnimFrame.Walk(it), backView = false) }
    val backWalks = (0 until 8).map { renderPair(AnimFrame.Walk(it), backView = true) }
    val attacks = (0 until 4).map { renderPair(AnimFrame.Attack(it), backView = false) }
    val hurt = renderPair(AnimFrame.Hurt, backView = false, flashWhite = true)
    val rest = renderPair(AnimFrame.Rest, backView = false)

    return HdAnimatedCharacterSheet(
      idleRight = idles.map { it.first },
      idleLeft = idles.map { it.second },
      walkRight = walks.map { it.first },
      walkLeft = walks.map { it.second },
      idleBackRight = backIdles.map { it.first },
      idleBackLeft = backIdles.map { it.second },
      walkBackRight = backWalks.map { it.first },
      walkBackLeft = backWalks.map { it.second },
      attackRight = attacks.map { it.first },
      attackLeft = attacks.map { it.second },
      hurtRight = hurt.first,
      hurtLeft = hurt.second,
      restRight = rest.first,
      restLeft = rest.second,
    )
  }

  private fun drawHdHumanoid76x100(
    isMaster: Boolean,
    heroClass: HeroClassType,
    frame: AnimFrame,
    backView: Boolean,
    flashWhite: Boolean,
  ): PixelCanvas {
    val c = PixelCanvas(HD_CHAR_W, HD_CHAR_H)
    val pal = paletteFor(isMaster, heroClass)

    val skinSpec = 0xFFFFF1E0.toInt()
    val skinLight = 0xFFFED7AA.toInt()
    val skinMid = 0xFFF2B880.toInt()
    val skinShade = 0xFFD08C60.toInt()
    val bootDark = 0xFF171324.toInt()
    val bootMid = 0xFF2D2542.toInt()
    val bootHi = 0xFF4D406E.toInt()

    // Compute kinematic offsets from animation state
    var phaseRad = 0f
    var bobY = 0
    var torsoLeanX = 0
    var leftFootDx = -6
    var leftFootDy = 0
    var rightFootDx = 6
    var rightFootDy = 0
    var frontArmAngle = 0.2f
    var backArmDx = 0
    var capeWavePhase = 0f
    var attackStage = -1

    when (frame) {
      is AnimFrame.Idle -> {
        phaseRad = (frame.frame / 6f) * (Math.PI.toFloat() * 2f)
        bobY = sin(phaseRad).roundToInt()
        capeWavePhase = phaseRad
        frontArmAngle = 0.15f + 0.08f * sin(phaseRad)
      }
      is AnimFrame.Walk -> {
        phaseRad = (frame.frame / 8f) * (Math.PI.toFloat() * 2f)
        val stride = sin(phaseRad)
        val liftL = (-cos(phaseRad)).coerceAtLeast(0f)
        val liftR = (cos(phaseRad)).coerceAtLeast(0f)
        bobY = (-abs(cos(phaseRad)) * 2.5f).roundToInt()
        torsoLeanX = 2
        leftFootDx = (-3 + stride * 9f).roundToInt()
        rightFootDx = (3 - stride * 9f).roundToInt()
        leftFootDy = (-liftL * 6f).roundToInt()
        rightFootDy = (-liftR * 6f).roundToInt()
        frontArmAngle = -stride * 0.55f
        backArmDx = (stride * 5f).roundToInt()
        capeWavePhase = phaseRad * 1.5f
      }
      is AnimFrame.Attack -> {
        attackStage = frame.frame
        when (attackStage) {
          0 -> { // Windup
            torsoLeanX = -3
            bobY = 1
            leftFootDx = -8
            rightFootDx = 4
            frontArmAngle = -0.85f
          }
          1 -> { // Explosive Strike
            torsoLeanX = 5
            bobY = -2
            leftFootDx = -9
            rightFootDx = 9
            frontArmAngle = 1.15f
          }
          2 -> { // Cleave Follow-Through
            torsoLeanX = 6
            bobY = 0
            leftFootDx = -8
            rightFootDx = 10
            frontArmAngle = 1.45f
          }
          else -> { // Recovery
            torsoLeanX = 2
            bobY = 0
            leftFootDx = -6
            rightFootDx = 6
            frontArmAngle = 0.55f
          }
        }
        capeWavePhase = attackStage * 1.4f
      }
      AnimFrame.Hurt -> {
        torsoLeanX = -5
        bobY = 2
        leftFootDx = -4
        rightFootDx = 5
      }
      AnimFrame.Rest -> {
        bobY = 10
        leftFootDx = 2
        rightFootDx = 9
        leftFootDy = -5
        rightFootDy = -5
      }
    }

    val cx = 36 + torsoLeanX
    val headCy = 25 + bobY
    val shoulderY = 42 + bobY
    val hipY = 64 + bobY
    val groundFootY = 89

    // 1. BACK LAYER: Dynamic Multi-Fold Billowing Cape (when facing South/3/4 Front)
    if (!backView) {
      drawDynamicCape(c, cx - 4, shoulderY + 1, 34, pal, capeWavePhase, isMoving = frame is AnimFrame.Walk || attackStage >= 1)
    }

    // 2. BACK ARM & OFF-HAND SHIELD / QUIVER
    val backHandX = cx - 12 + backArmDx
    val backHandY = shoulderY + 14
    c.drawShadedPill(cx - 8, shoulderY + 2, backHandX, backHandY, 3.6f, pal.clothDark, pal.clothMid, pal.clothHi)
    c.drawShadedOval(backHandX, backHandY + 1, 3, 3, skinShade, skinMid, skinLight)

    if (!isMaster && heroClass == HeroClassType.KNIGHT && !backView) {
      // Ornate Heater / Kite Shield on off-hand with gold lion cross
      c.drawShadedOval(backHandX - 2, backHandY + 2, 8, 12, pal.capeDark, pal.capeMid, pal.capeHi, pal.armorSpec)
      c.vLine(backHandX - 2, backHandY - 8, 20, pal.trimGold)
      c.hLine(backHandX - 8, backHandY, 12, pal.trimGold)
      c.set(backHandX - 2, backHandY, pal.trimSpec)
    }

    // 3. ARTICULATED LEGS, KNEE GUARDS & ARMORED BOOTS
    val lKneeX = cx + (leftFootDx / 2) - 2
    val lKneeY = (hipY + groundFootY + leftFootDy) / 2
    val rKneeX = cx + (rightFootDx / 2) + 2
    val rKneeY = (hipY + groundFootY + rightFootDy) / 2

    // Left (Back) Leg
    c.drawShadedPill(cx - 4, hipY, lKneeX, lKneeY, 4.0f, pal.clothDark, pal.clothMid, pal.clothHi)
    c.drawShadedPill(lKneeX, lKneeY, cx + leftFootDx, groundFootY + leftFootDy, 3.8f, bootDark, bootMid, bootHi, pal.armorHi)
    c.drawShadedOval(cx + leftFootDx + 1, groundFootY + leftFootDy + 1, 5, 3, bootDark, bootMid, bootHi)
    c.hLine(cx + leftFootDx - 3, groundFootY + leftFootDy - 5, 7, pal.trimGold)

    // Right (Front) Leg
    c.drawShadedPill(cx + 3, hipY, rKneeX, rKneeY, 4.2f, pal.clothDark, pal.clothMid, pal.clothHi)
    c.drawShadedPill(rKneeX, rKneeY, cx + rightFootDx, groundFootY + rightFootDy, 4.0f, bootDark, bootMid, bootHi, pal.armorSpec)
    c.drawShadedOval(cx + rightFootDx + 2, groundFootY + rightFootDy + 1, 5, 3, bootDark, bootMid, bootHi, pal.armorHi)
    c.hLine(cx + rightFootDx - 3, groundFootY + rightFootDy - 5, 7, pal.trimGold)

    // 4. SCULPTED 3/4 TORSO, TASSETS, TABARD SASH, CUIRASS & PAULDRONS
    // Flared coat tail / armored tassets
    for (row in 0..12) {
      val spread = 9 + row / 2
      val py = hipY - 4 + row
      val sway = (sin(capeWavePhase + row * 0.25f) * 1.5f).roundToInt()
      c.hLine(cx - spread + sway, py, spread * 2, if (row > 9) pal.trimGold else pal.armorDark)
      c.hLine(cx - spread + 2 + sway, py, spread - 2, pal.clothMid)
      c.hLine(cx + 2 + sway, py, spread - 3, pal.clothHi)
    }

    // Center embroidered royal tabard hanging between legs
    for (ty in 0..14) {
      val sway = (sin(capeWavePhase + ty * 0.2f) * 1.2f).roundToInt()
      val py = hipY - 3 + ty
      c.hLine(cx - 3 + sway, py, 7, pal.capeMid)
      c.set(cx - 3 + sway, py, pal.trimGold)
      c.set(cx + 3 + sway, py, pal.trimGold)
      if (ty % 3 == 0) c.set(cx + sway, py, pal.trimSpec)
    }

    // Sculpted hourglass/heroic chest cuirass
    c.drawShadedOval(cx, shoulderY + 10, 10, 12, pal.armorDark, pal.armorMid, pal.armorHi, pal.armorSpec)
    // Gold filigree chest crest / ribs
    c.vLine(cx + 1, shoulderY + 3, 14, pal.trimGold)
    c.hLine(cx - 6, shoulderY + 6, 13, pal.trimGold)
    c.set(cx + 1, shoulderY + 6, pal.trimSpec)

    // Waist belt & ornate gemstone buckle
    c.fillRect(cx - 9, hipY - 5, 19, 4, 0xFF2B1810.toInt())
    c.hLine(cx - 9, hipY - 5, 19, pal.trimGold)
    c.fillRect(cx - 2, hipY - 6, 6, 5, pal.trimGold)
    c.fillRect(cx - 1, hipY - 5, 4, 3, pal.weaponGlow)
    c.set(cx, hipY - 5, 0xFFFFFFFF.toInt())

    // Layered 3/4 Shoulder Pauldrons
    c.drawShadedOval(cx - 9, shoulderY + 2, 6, 5, pal.armorDark, pal.armorMid, pal.armorHi, pal.armorSpec)
    c.hLine(cx - 13, shoulderY + 5, 9, pal.trimGold)
    c.drawShadedOval(cx + 9, shoulderY + 2, 6, 5, pal.armorDark, pal.armorMid, pal.armorHi, pal.armorSpec)
    c.hLine(cx + 4, shoulderY + 5, 10, pal.trimGold)
    c.set(cx - 10, shoulderY, pal.trimSpec)
    c.set(cx + 8, shoulderY, pal.trimSpec)

    // If Back View (walking North), drape the full flowing cloak over the torso!
    if (backView) {
      drawDynamicCape(c, cx, shoulderY + 1, 36, pal, capeWavePhase, isMoving = frame is AnimFrame.Walk)
    }

    // 5. ANIME DARK-FANTASY HEAD, FACE, EXPRESSIVE EYES & LAYERED HAIR / HEADGEAR
    // Back hair volume behind neck
    c.drawShadedOval(cx - 3, headCy + 4, 12, 13, pal.hairDark, pal.hairMid, pal.hairHi)

    if (!backView) {
      // Neck
      c.fillRect(cx - 3, headCy + 11, 6, 5, skinShade)
      c.fillRect(cx - 2, headCy + 11, 4, 4, skinMid)

      // Sculpted 3/4 Anime Face (rounded cranium + tapered chin on right-forward side)
      c.drawShadedOval(cx + 1, headCy + 1, 10, 11, skinShade, skinMid, skinLight, skinSpec)
      // Tapered jawline highlight
      c.hLine(cx - 3, headCy + 10, 8, skinLight)
      c.set(cx + 3, headCy + 11, skinMid)

      // Expressive Anime Eyes (left-center eye & right-3/4 eye with lash arch, iris gradient & white catchlight)
      // Left eye (3x4)
      val eyeY = headCy + 1
      c.hLine(cx - 2, eyeY - 1, 4, pal.hairDark) // upper eyelash
      c.fillRect(cx - 2, eyeY, 4, 4, 0xFFFFFFFF.toInt())
      c.fillRect(cx - 1, eyeY, 3, 4, pal.eyeIris)
      c.fillRect(cx, eyeY + 1, 2, 2, 0xFF090D16.toInt()) // pupil
      c.set(cx - 1, eyeY, 0xFFFFFFFF.toInt()) // top-left catchlight
      c.set(cx + 1, eyeY + 3, pal.eyeGlow) // lower iris luminescence

      // Right (far 3/4) eye (3x4)
      c.hLine(cx + 5, eyeY - 1, 3, pal.hairDark)
      c.fillRect(cx + 5, eyeY, 3, 4, 0xFFFFFFFF.toInt())
      c.fillRect(cx + 6, eyeY, 2, 4, pal.eyeIris)
      c.set(cx + 6, eyeY + 1, 0xFF090D16.toInt())
      c.set(cx + 5, eyeY, 0xFFFFFFFF.toInt())
      c.set(cx + 6, eyeY + 3, pal.eyeGlow)

      // Subtle nose bridge & mouth
      c.set(cx + 4, eyeY + 4, skinShade)
      c.hLine(cx + 1, eyeY + 7, 3, if (attackStage >= 1) 0xFFBE123C.toInt() else skinShade)
    } else {
      // Back of head hair dome
      c.drawShadedOval(cx, headCy + 1, 12, 12, pal.hairDark, pal.hairMid, pal.hairHi)
    }

    // Sculpted Class Hair Bangs, Flowing Side Locks & Signature Headgear
    val hairSway = (sin(capeWavePhase) * 2f).roundToInt()
    // Crown volume
    c.drawShadedOval(cx, headCy - 6, 12, 6, pal.hairDark, pal.hairMid, pal.hairHi, pal.trimSpec)
    if (!backView) {
      // Swept anime bangs across forehead
      for (b in -8..8 step 3) {
        val bangLen = 4 + (abs(b) % 3)
        c.vLine(cx + b, headCy - 5, bangLen, if (b < 0) pal.hairHi else pal.hairMid)
        c.set(cx + b, headCy - 5 + bangLen, pal.hairDark)
      }
    }
    // Flowing side/back hair locks swaying with movement
    for (s in 0..8) {
      val sx = cx - 10 - (s / 3) + (hairSway * s) / 8
      val sy = headCy + s * 2
      c.fillRect(sx, sy, 4, 3, if (s < 4) pal.hairMid else pal.hairDark)
    }

    // Class-Specific Crown / Helm / Witch Hat / Halo
    when {
      isMaster -> {
        // Sovereign Astral Crown Circlet with Levitating Starlight Crest
        c.hLine(cx - 10, headCy - 6, 21, pal.trimGold)
        c.hLine(cx - 8, headCy - 7, 17, pal.trimSpec)
        c.fillRect(cx, headCy - 11, 3, 5, pal.trimGold)
        c.fillRect(cx + 1, headCy - 10, 2, 3, pal.weaponGlow)
        c.set(cx + 1, headCy - 12, 0xFFFFFFFF.toInt())
      }
      heroClass == HeroClassType.KNIGHT -> {
        // Winged Paladin Valkyrie/Vanguard Diadem & Flowing Azure Plume
        c.hLine(cx - 10, headCy - 5, 21, pal.armorSpec)
        c.hLine(cx - 9, headCy - 6, 19, pal.trimGold)
        // Winged sideplates
        c.drawShadedPill(cx - 11, headCy - 4, cx - 15, headCy - 11, 2.2f, pal.armorMid, pal.armorHi, pal.armorSpec)
        c.drawShadedPill(cx + 9, headCy - 4, cx + 13, headCy - 11, 2.2f, pal.armorMid, pal.armorHi, pal.armorSpec)
        // Flowing crest plume
        c.drawShadedPill(cx - 1, headCy - 10, cx - 9 + hairSway, headCy - 15, 3.2f, pal.capeDark, pal.capeMid, pal.capeHi, pal.weaponGlow)
      }
      heroClass == HeroClassType.MAGE -> {
        // Grand Curved Astral Witch-Arcanist Hat with Golden Buckle & Star Brim
        c.drawShadedOval(cx, headCy - 6, 17, 4, pal.armorDark, pal.armorMid, pal.armorHi)
        c.hLine(cx - 16, headCy - 6, 33, pal.trimGold)
        c.drawShadedPill(cx - 2, headCy - 9, cx - 7, headCy - 18, 6.0f, pal.armorDark, pal.armorMid, pal.armorHi)
        c.drawShadedPill(cx - 7, headCy - 18, cx - 14 + hairSway, headCy - 21, 3.2f, pal.armorDark, pal.armorMid, pal.armorSpec)
        c.fillRect(cx - 3, headCy - 9, 6, 3, pal.trimGold)
        c.set(cx, headCy - 8, pal.weaponGlow)
      }
      heroClass == HeroClassType.PRIEST -> {
        // Luminous Seraphim Golden Halo & Cathedral Mitre Circlet
        c.hLine(cx - 8, headCy - 6, 17, pal.trimGold)
        c.set(cx + 1, headCy - 7, pal.weaponGlow)
        // Floating radiant halo above head
        for (ang in 0 until 24) {
          val rad = ang * (Math.PI * 2.0 / 24.0)
          val hx = cx + (cos(rad) * 11.0).roundToInt()
          val hy = headCy - 15 + (sin(rad) * 3.5).roundToInt()
          c.set(hx, hy, if (ang % 2 == 0) pal.trimSpec else pal.trimGold)
        }
      }
      heroClass == HeroClassType.ASSASSIN -> {
        // Crimson Shadow-Mask & Flowing Ninja Scarf Tail
        if (!backView) {
          c.fillRect(cx - 6, headCy + 5, 14, 6, pal.capeDark)
          c.hLine(cx - 6, headCy + 5, 14, pal.capeHi)
        }
        c.drawShadedPill(cx - 8, headCy + 7, cx - 22, headCy + 4 + hairSway, 2.6f, pal.capeDark, pal.capeMid, pal.capeHi)
      }
      heroClass == HeroClassType.BERSERKER -> {
        // Dragon-Horned War Crown
        c.hLine(cx - 10, headCy - 5, 20, pal.trimGold)
        c.drawShadedPill(cx - 9, headCy - 6, cx - 15, headCy - 16, 2.5f, 0xFF475569.toInt(), 0xFFCBD5E1.toInt(), 0xFFFFFFFF.toInt())
        c.drawShadedPill(cx + 8, headCy - 6, cx + 14, headCy - 16, 2.5f, 0xFF475569.toInt(), 0xFFCBD5E1.toInt(), 0xFFFFFFFF.toInt())
      }
      heroClass == HeroClassType.ARCHER -> {
        // Sylvan Feather Circlet & High Ponytail
        c.hLine(cx - 9, headCy - 5, 18, pal.trimGold)
        c.drawShadedPill(cx + 5, headCy - 6, cx + 13, headCy - 14, 1.8f, pal.weaponGlow, 0xFFFEF08A.toInt(), 0xFFFFFFFF.toInt())
        c.drawShadedPill(cx - 9, headCy - 3, cx - 18, headCy + 6 + hairSway, 4.0f, pal.hairDark, pal.hairMid, pal.hairHi)
      }
    }

    // 6. FRONT ARTICULATED ARM & HIGH-DETAIL CLASS SIGNATURE WEAPON + SLASH SMEAR
    val handX = cx + 9 + (sin(frontArmAngle) * 8f).roundToInt()
    val handY = shoulderY + 11 - (cos(frontArmAngle) * 4f).roundToInt()
    c.drawShadedPill(cx + 6, shoulderY + 2, handX, handY, 3.8f, pal.armorDark, pal.armorMid, pal.armorHi, pal.armorSpec)
    c.drawShadedOval(handX, handY, 3, 3, skinShade, skinMid, skinLight)

    drawHdWeaponAndSlash(c, isMaster, heroClass, pal, handX, handY, attackStage, bobY)

    if (flashWhite) {
      for (i in c.pixels.indices) {
        if ((c.pixels[i] ushr 24) > 128) {
          c.pixels[i] = 0xFFFFF1F2.toInt()
        }
      }
    }

    c.applyCrispOutline(0xFF080612.toInt())
    return c
  }

  private fun drawDynamicCape(
    c: PixelCanvas,
    topCx: Int,
    topY: Int,
    heightPx: Int,
    pal: ClassPalette,
    wavePhase: Float,
    isMoving: Boolean,
  ) {
    val flutterAmp = if (isMoving) 4.2f else 2.0f
    for (row in 0 until heightPx) {
      val t = row.toFloat() / heightPx.coerceAtLeast(1)
      val py = topY + row
      val halfW = (7f + t * 11f).roundToInt()
      val waveShift = (sin(wavePhase - t * 3.4f) * flutterAmp * t).roundToInt() - (if (isMoving) (t * 5f).roundToInt() else 0)
      val rowCx = topCx + waveShift
      for (dx in -halfW..halfW) {
        val u = dx.toFloat() / halfW.coerceAtLeast(1)
        val fold = sin(u * 4.5f + wavePhase * 0.5f)
        val col = when {
          row == heightPx - 1 || abs(dx) == halfW -> pal.trimGold
          row == heightPx - 2 && abs(dx) % 2 == 0 -> pal.trimSpec
          fold > 0.38f -> pal.capeHi
          fold > -0.25f -> pal.capeMid
          else -> pal.capeDark
        }
        c.set(rowCx + dx, py, col)
      }
    }
  }

  private fun drawHdWeaponAndSlash(
    c: PixelCanvas,
    isMaster: Boolean,
    heroClass: HeroClassType,
    pal: ClassPalette,
    handX: Int,
    handY: Int,
    attackStage: Int,
    bobY: Int,
  ) {
    if (isMaster || heroClass == HeroClassType.MAGE || heroClass == HeroClassType.PRIEST) {
      // Ornate Grand Staff / Scepter with Floating Celestial Astrolabe Head
      val tiltX = if (attackStage == 1 || attackStage == 2) 10 else 3
      val topX = handX + tiltX
      val topY = handY - 30
      val botX = handX - tiltX / 2
      val botY = handY + 22
      c.drawShadedPill(botX, botY, topX, topY, 2.0f, 0xFF451A03.toInt(), pal.trimGold, pal.trimSpec)
      // Golden wings / crescent cradle at staff crown
      c.drawShadedOval(topX, topY, 7, 5, pal.trimGold, pal.trimSpec, 0xFFFFFFFF.toInt())
      // Floating luminous mana core
      val orbR = if (attackStage == 1 || attackStage == 2) 6 else 4
      c.drawShadedOval(topX, topY - 3, orbR, orbR, pal.weaponCore, pal.weaponGlow, 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
      if (attackStage == 1 || attackStage == 2) {
        // Arcane cast burst arc in front of staff
        for (a in -50..50 step 5) {
          val rad = Math.toRadians(a.toDouble())
          val rx = topX + (cos(rad) * 14.0).roundToInt()
          val ry = topY - 2 + (sin(rad) * 14.0).roundToInt()
          c.set(rx, ry, pal.weaponGlow)
          c.set(rx + 1, ry, 0xFFFFFFFF.toInt())
        }
      }
      return
    }

    when (heroClass) {
      HeroClassType.KNIGHT -> {
        // Excalibur Broadsword with Golden Winged Crossguard & Glowing Rune Fuller
        val tipX = when (attackStage) {
          0 -> handX - 8
          1 -> handX + 26
          2 -> handX + 24
          else -> handX + 7
        }
        val tipY = when (attackStage) {
          0 -> handY - 32
          1 -> handY - 4
          2 -> handY + 12
          else -> handY - 30
        }
        // Blade
        c.drawShadedPill(handX, handY, tipX, tipY, 3.0f, pal.armorDark, pal.weaponCore, pal.weaponEdge, 0xFFFFFFFF.toInt())
        // Glowing central rune fuller
        c.drawShadedPill(handX, handY, (handX + tipX) / 2, (handY + tipY) / 2, 1.1f, pal.weaponGlow, pal.weaponGlow, 0xFFFFFFFF.toInt())
        // Winged Gold Crossguard
        c.drawShadedPill(handX - 5, handY - 3, handX + 5, handY + 3, 2.2f, pal.trimGold, pal.trimSpec, 0xFFFFFFFF.toInt())
        if (attackStage == 1 || attackStage == 2) {
          drawPixelSlashArc(c, handX - 2, handY - 6, 26, -55, 45, pal.weaponGlow, pal.trimSpec)
        }
      }

      HeroClassType.ARCHER -> {
        // Carved Elven Greatbow with Glowing Emerald Wind-String & Nocked Arrow
        val bowCx = handX + 4
        val bowCy = handY - 2
        for (a in -68..68 step 4) {
          val rad = Math.toRadians(a.toDouble())
          val bx = bowCx + (cos(rad) * 11.0).roundToInt()
          val by = bowCy + (sin(rad) * 21.0).roundToInt()
          c.drawShadedOval(bx, by, 2, 2, 0xFF451A03.toInt(), pal.weaponCore, pal.trimGold, pal.trimSpec)
        }
        // Bowstring
        val pullX = if (attackStage == 0 || attackStage == 1) bowCx - 5 else bowCx + 1
        c.drawShadedPill(bowCx + 4, bowCy - 18, pullX, bowCy, 0.9f, pal.weaponEdge, pal.weaponEdge, 0xFFFFFFFF.toInt())
        c.drawShadedPill(pullX, bowCy, bowCx + 4, bowCy + 18, 0.9f, pal.weaponEdge, pal.weaponEdge, 0xFFFFFFFF.toInt())
        // Luminous Wind Arrow
        if (attackStage >= 0) {
          c.drawShadedPill(pullX, bowCy, bowCx + 22, bowCy, 1.3f, pal.weaponGlow, pal.trimSpec, 0xFFFFFFFF.toInt())
        }
      }

      HeroClassType.ASSASSIN -> {
        // Twin Curved Blood-Moon Daggers with Crimson Edge Trail
        val tipX = if (attackStage == 1 || attackStage == 2) handX + 22 else handX + 12
        val tipY = if (attackStage == 1 || attackStage == 2) handY - 6 else handY - 14
        c.drawShadedPill(handX, handY, tipX, tipY, 2.2f, pal.weaponCore, pal.weaponEdge, 0xFFFFFFFF.toInt())
        c.drawShadedPill(handX, handY + 1, tipX, tipY + 1, 1.0f, pal.weaponGlow, pal.weaponGlow, pal.trimSpec)
        if (attackStage == 1 || attackStage == 2) {
          drawPixelSlashArc(c, handX, handY - 4, 22, -45, 50, pal.weaponGlow, 0xFFFFFFFF.toInt())
        }
      }

      HeroClassType.BERSERKER -> {
        // Colossal Volcanic Twin-Blade Greataxe dripping with Magma
        val headX = if (attackStage == 1 || attackStage == 2) handX + 20 else handX + 6
        val headY = if (attackStage == 1 || attackStage == 2) handY + 2 else handY - 26
        c.drawShadedPill(handX - 3, handY + 14, headX, headY, 2.2f, 0xFF3B2314.toInt(), 0xFF78350F.toInt(), pal.trimGold)
        // Massive crescent axe blades
        c.drawShadedOval(headX + 2, headY + 2, 10, 8, pal.armorDark, pal.weaponCore, pal.weaponEdge, 0xFFFFFFFF.toInt())
        c.drawShadedOval(headX + 5, headY + 2, 5, 6, pal.trimGold, pal.weaponGlow, 0xFFFEF08A.toInt())
        if (attackStage == 1 || attackStage == 2) {
          drawPixelSlashArc(c, handX, handY - 4, 25, -65, 55, pal.weaponGlow, 0xFFFEF08A.toInt())
        }
      }

      else -> Unit
    }
  }

  private fun drawPixelSlashArc(
    c: PixelCanvas,
    cx: Int,
    cy: Int,
    radius: Int,
    startDeg: Int,
    endDeg: Int,
    outerCol: Int,
    coreCol: Int,
  ) {
    for (deg in startDeg..endDeg step 2) {
      val rad = Math.toRadians(deg.toDouble())
      val cosA = cos(rad)
      val sinA = sin(rad)
      for (dr in -3..2) {
        val r = radius + dr
        val px = cx + (cosA * r).roundToInt()
        val py = cy + (sinA * r).roundToInt()
        c.set(px, py, if (dr in -1..0) coreCol else outerCol)
      }
    }
  }

  // ==================== BUILD ENEMY / BOSS SHEET ====================

  private fun buildEnemySheet(archetype: EnemyArchetype): HdAnimatedCharacterSheet {
    fun renderPair(
      frame: AnimFrame,
      flashWhite: Boolean = false,
    ): Pair<ImageBitmap, ImageBitmap> {
      val right = drawHdEnemyFrame(archetype, frame, flashWhite)
      val left = right.flipHorizontalCanvas()
      return right.toImageBitmap() to left.toImageBitmap()
    }

    val idles = (0 until 6).map { renderPair(AnimFrame.Idle(it)) }
    val walks = (0 until 8).map { renderPair(AnimFrame.Walk(it)) }
    val attacks = (0 until 4).map { renderPair(AnimFrame.Attack(it)) }
    val hurt = renderPair(AnimFrame.Hurt, flashWhite = true)

    return HdAnimatedCharacterSheet(
      idleRight = idles.map { it.first },
      idleLeft = idles.map { it.second },
      walkRight = walks.map { it.first },
      walkLeft = walks.map { it.second },
      idleBackRight = idles.map { it.first },
      idleBackLeft = idles.map { it.second },
      walkBackRight = walks.map { it.first },
      walkBackLeft = walks.map { it.second },
      attackRight = attacks.map { it.first },
      attackLeft = attacks.map { it.second },
      hurtRight = hurt.first,
      hurtLeft = hurt.second,
      restRight = hurt.first,
      restLeft = hurt.second,
    )
  }

  private fun drawHdEnemyFrame(
    archetype: EnemyArchetype,
    frame: AnimFrame,
    flashWhite: Boolean,
  ): PixelCanvas {
    val isBoss = archetype.isBoss
    val w = if (isBoss) HD_BOSS_W else HD_ENEMY_W
    val h = if (isBoss) HD_BOSS_H else HD_ENEMY_H
    val c = PixelCanvas(w, h)

    val phase = when (frame) {
      is AnimFrame.Idle -> (frame.frame / 6f) * (Math.PI.toFloat() * 2f)
      is AnimFrame.Walk -> (frame.frame / 8f) * (Math.PI.toFloat() * 2f)
      is AnimFrame.Attack -> (frame.frame / 4f) * (Math.PI.toFloat() * 2f)
      else -> 0f
    }
    val stride = if (frame is AnimFrame.Walk) sin(phase) else 0f
    val bob = (sin(phase) * (if (isBoss) 3f else 2f)).roundToInt()
    val lunge = when (frame) {
      is AnimFrame.Attack -> if (frame.frame in 1..2) 7 else -3
      AnimFrame.Hurt -> -5
      else -> 0
    }
    val isStriking = frame is AnimFrame.Attack && frame.frame in 1..2

    when (archetype) {
      EnemyArchetype.DIRE_WOLF -> {
        val cx = 40 + lunge
        val cy = 54 + bob
        val legSwing = (stride * 9f).roundToInt()
        val furDark = 0xFF1E293B.toInt()
        val furMid = 0xFF334155.toInt()
        val furHi = 0xFF64748B.toInt()
        val furSilver = 0xFFCBD5E1.toInt()
        // 4 Articulated wolf legs with claws
        c.drawShadedPill(cx - 16, cy + 4, cx - 20 - legSwing, 84, 3.5f, furDark, furMid, furHi)
        c.drawShadedPill(cx - 8, cy + 6, cx - 6 + legSwing, 86, 3.8f, furDark, furMid, furHi)
        c.drawShadedPill(cx + 10, cy + 4, cx + 14 - legSwing, 84, 3.8f, furDark, furMid, furHi)
        c.drawShadedPill(cx + 16, cy + 6, cx + 20 + legSwing, 86, 4.0f, furDark, furMid, furSilver)
        // Bushy tail
        c.drawShadedPill(cx - 18, cy - 2, cx - 32, cy - 10 + bob, 4.5f, furDark, furMid, furHi)
        // Muscular ribcage & haunches
        c.drawShadedOval(cx - 8, cy, 13, 9, furDark, furMid, furHi)
        c.drawShadedOval(cx + 8, cy - 3, 14, 11, furDark, furMid, furHi, furSilver)
        // Jagged silver mane spikes along spine
        for (m in -12..14 step 4) {
          c.drawShadedPill(cx + m, cy - 10, cx + m - 3, cy - 18, 2.0f, furMid, furHi, furSilver)
        }
        // Ferocious Wolf Head, Pointed Ears, Glowing Crimson Eye & Fangs
        c.drawShadedOval(cx + 21, cy - 9, 10, 8, furDark, furMid, furHi, furSilver)
        c.drawShadedPill(cx + 24, cy - 8, cx + 34, cy - 5, 4.2f, furDark, furMid, furHi)
        c.drawShadedPill(cx + 16, cy - 15, cx + 14, cy - 24, 2.4f, furDark, furMid, furSilver)
        c.drawShadedPill(cx + 22, cy - 15, cx + 21, cy - 24, 2.4f, furDark, furMid, furSilver)
        // Glowing red eye + white fangs
        c.fillRect(cx + 24, cy - 11, 4, 3, 0xFFEF4444.toInt())
        c.set(cx + 25, cy - 11, 0xFFFEF08A.toInt())
        c.hLine(cx + 26, cy - 3, 8, 0xFFFFFFFF.toInt())
        if (isStriking) {
          drawPixelSlashArc(c, cx + 28, cy - 4, 18, -45, 45, 0xFFEF4444.toInt(), 0xFFFECDD3.toInt())
        }
      }

      EnemyArchetype.FIELD_GOBLIN_RAIDER, EnemyArchetype.FIELD_GOBLIN_ARCHER -> {
        val cx = 40 + lunge
        val footY = 86
        val legSwing = (stride * 8f).roundToInt()
        val skinDark = 0xFF23421C.toInt()
        val skinMid = 0xFF3F6B30.toInt()
        val skinHi = 0xFF659E47.toInt()
        val leatherDark = 0xFF3B2212.toInt()
        val leatherMid = 0xFF693E20.toInt()
        val leatherHi = 0xFF9C6034.toInt()

        // Legs & wrapped boots
        c.drawShadedPill(cx - 5, 62 + bob, cx - 7 + legSwing, footY, 3.8f, leatherDark, leatherMid, leatherHi)
        c.drawShadedPill(cx + 5, 62 + bob, cx + 7 - legSwing, footY, 4.0f, leatherDark, leatherMid, leatherHi)
        // Hunched brigandine torso + spiked fur pauldron
        c.drawShadedOval(cx, 52 + bob, 11, 13, leatherDark, leatherMid, leatherHi)
        c.hLine(cx - 10, 58 + bob, 20, 0xFFD97706.toInt())
        c.drawShadedOval(cx - 9, 43 + bob, 7, 5, 0xFF27272A.toInt(), 0xFF52525B.toInt(), 0xFFA1A1AA.toInt())
        // Sculpted Goblin Head, Long Swept Ears, Hooked Nose & Crimson/Gold Eyes
        c.drawShadedOval(cx + 3, 33 + bob, 10, 9, skinDark, skinMid, skinHi)
        c.drawShadedPill(cx - 6, 33 + bob, cx - 19, 29 + bob, 2.5f, skinDark, skinMid, skinHi)
        c.drawShadedPill(cx + 10, 33 + bob, cx + 20, 29 + bob, 2.2f, skinDark, skinMid, skinHi)
        // Bandana / Hood
        val hoodCol = if (archetype.isRanged) 0xFF15803D.toInt() else 0xFFB91C1C.toInt()
        c.drawShadedOval(cx + 1, 26 + bob, 10, 4, 0xFF18181B.toInt(), hoodCol, 0xFFF87171.toInt())
        // Glowing menacing eyes
        c.fillRect(cx + 2, 32 + bob, 3, 3, 0xFFEF4444.toInt())
        c.set(cx + 3, 32 + bob, 0xFFFEF08A.toInt())
        c.fillRect(cx + 8, 32 + bob, 3, 3, 0xFFEF4444.toInt())
        c.set(cx + 9, 32 + bob, 0xFFFEF08A.toInt())

        if (archetype.isRanged) {
          // Bone Shortbow & Poison-Tipped Arrow
          val bx = cx + 15
          val by = 48 + bob
          for (a in -60..60 step 6) {
            val rad = Math.toRadians(a.toDouble())
            c.drawShadedOval(bx + (cos(rad) * 8).roundToInt(), by + (sin(rad) * 16).roundToInt(), 2, 2, leatherDark, leatherHi, 0xFFFDE68A.toInt())
          }
          c.drawShadedPill(bx - 3, by, bx + 18, by, 1.3f, 0xFF22C55E.toInt(), 0xFF86EFAC.toInt(), 0xFFFFFFFF.toInt())
        } else {
          // Spiked wooden buckler + Jagged Iron Cleaver Scimitar
          c.drawShadedOval(cx - 10, 52 + bob, 7, 9, leatherDark, leatherMid, leatherHi, 0xFFE2E8F0.toInt())
          val tipX = if (isStriking) cx + 32 else cx + 18
          val tipY = if (isStriking) 52 + bob else 24 + bob
          c.drawShadedPill(cx + 10, 50 + bob, tipX, tipY, 3.0f, 0xFF334155.toInt(), 0xFF94A3B8.toInt(), 0xFFF8FAFC.toInt())
          if (isStriking) {
            drawPixelSlashArc(c, cx + 12, 48 + bob, 20, -50, 50, 0xFFEF4444.toInt(), 0xFFFDE68A.toInt())
          }
        }
      }

      EnemyArchetype.FOREST_WRAITH -> {
        val cx = 42 + lunge
        val floatY = (sin(phase) * 4f).roundToInt()
        val cy = 46 + floatY
        val wraithDark = 0xFF0F172A.toInt()
        val wraithMid = 0xFF1E3A8A.toInt()
        val wraithCyan = 0xFF06B6D4.toInt()
        val wraithGlow = 0xFFA5F3FC.toInt()

        // Tattered spectral cloak tails billowing in sine waves
        for (row in 0..34) {
          val t = row / 34f
          val wave = (sin(phase * 1.5f - t * 4f) * 6f * t).roundToInt()
          val halfW = (14f * (1f - t * 0.55f)).roundToInt().coerceAtLeast(2)
          val py = cy + 6 + row
          c.hLine(cx - halfW + wave, py, halfW * 2, if (row % 4 == 0) wraithCyan else wraithMid)
          c.set(cx - halfW + wave, py, wraithGlow)
          c.set(cx + halfW + wave, py, wraithDark)
        }
        // Hooded Spectral Torso & Cowl
        c.drawShadedOval(cx, cy, 13, 15, wraithDark, wraithMid, wraithCyan, wraithGlow)
        c.drawShadedOval(cx + 2, cy - 14, 11, 11, wraithDark, wraithMid, wraithCyan)
        c.drawShadedOval(cx + 5, cy - 13, 7, 7, 0xFF020617.toInt(), 0xFF090D16.toInt(), wraithDark)
        // Twin glowing cyan soul-eyes inside dark hood
        c.fillRect(cx + 3, cy - 14, 3, 3, wraithGlow)
        c.fillRect(cx + 8, cy - 14, 3, 3, wraithGlow)
        // Floating Soul-Lantern & Spectral Scythe Claw
        val orbX = cx + 20
        val orbY = cy - 4 + floatY
        c.drawShadedOval(orbX, orbY, 6, 6, wraithMid, wraithCyan, wraithGlow, 0xFFFFFFFF.toInt())
        if (isStriking) {
          drawPixelSlashArc(c, cx + 12, cy - 2, 22, -55, 55, wraithCyan, wraithGlow)
        }
      }

      EnemyArchetype.RUIN_SENTINEL -> {
        val cx = 42 + lunge
        val footY = 88
        val legSwing = (stride * 7f).roundToInt()
        val stoneDark = 0xFF1E293B.toInt()
        val stoneMid = 0xFF475569.toInt()
        val stoneHi = 0xFF94A3B8.toInt()
        val bronze = 0xFFD97706.toInt()
        val runeRed = 0xFFF43F5E.toInt()

        // Heavy stone-pillar legs
        c.drawShadedPill(cx - 8, 60 + bob, cx - 9 + legSwing, footY, 5.0f, stoneDark, stoneMid, stoneHi)
        c.drawShadedPill(cx + 8, 60 + bob, cx + 9 - legSwing, footY, 5.0f, stoneDark, stoneMid, stoneHi)
        // Runic Golem Chest & Interlocking Pauldrons
        c.drawShadedOval(cx, 46 + bob, 16, 16, stoneDark, stoneMid, stoneHi, 0xFFF8FAFC.toInt())
        c.drawShadedOval(cx - 15, 38 + bob, 8, 7, stoneDark, bronze, 0xFFFDE68A.toInt())
        c.drawShadedOval(cx + 15, 38 + bob, 8, 7, stoneDark, bronze, 0xFFFDE68A.toInt())
        // Glowing crimson core rune on chest
        c.drawShadedOval(cx + 1, 46 + bob, 5, 5, 0xFF881337.toInt(), runeRed, 0xFFFFE4E6.toInt())
        // Helm Visor
        c.drawShadedOval(cx + 1, 24 + bob, 10, 10, stoneDark, stoneMid, stoneHi)
        c.hLine(cx - 5, 24 + bob, 14, runeRed)
        c.hLine(cx - 3, 25 + bob, 10, 0xFFFFE4E6.toInt())
        // Giant Sentinel Halberd
        val tipX = if (isStriking) cx + 32 else cx + 20
        val tipY = if (isStriking) 44 + bob else 10 + bob
        c.drawShadedPill(cx + 14, 82, tipX, tipY, 2.5f, stoneDark, bronze, 0xFFFDE68A.toInt())
        c.drawShadedOval(tipX, tipY + 4, 8, 6, stoneDark, runeRed, 0xFFFFE4E6.toInt())
      }

      EnemyArchetype.GOBLIN_WAR_CAPTAIN, EnemyArchetype.ELDER_TREANT, EnemyArchetype.ABYSSAL_EXECUTIONER -> {
        val cx = 56 + lunge
        val footY = 114
        val legSwing = (stride * 10f).roundToInt()

        val armorDark: Int
        val armorMid: Int
        val armorHi: Int
        val accentCol: Int
        val glowCol: Int

        when (archetype) {
          EnemyArchetype.GOBLIN_WAR_CAPTAIN -> {
            armorDark = 0xFF1A2E05.toInt()
            armorMid = 0xFF3F6212.toInt()
            armorHi = 0xFF84CC16.toInt()
            accentCol = 0xFFDC2626.toInt()
            glowCol = 0xFFFEF08A.toInt()
          }
          EnemyArchetype.ELDER_TREANT -> {
            armorDark = 0xFF271712.toInt()
            armorMid = 0xFF4E342E.toInt()
            armorHi = 0xFF795548.toInt()
            accentCol = 0xFF16A34A.toInt()
            glowCol = 0xFF22D3EE.toInt()
          }
          else -> {
            armorDark = 0xFF0F0B29.toInt()
            armorMid = 0xFF312E81.toInt()
            armorHi = 0xFF6366F1.toInt()
            accentCol = 0xFFE11D48.toInt()
            glowCol = 0xFFFDA4AF.toInt()
          }
        }

        // Boss Flowing Tattered War-Mantle / Bark Roots
        for (row in 0..56) {
          val t = row / 56f
          val wave = (sin(phase - t * 3.2f) * 5f * t).roundToInt()
          val halfW = (18 + (t * 14f).roundToInt())
          c.hLine(cx - halfW + wave, 46 + bob + row, halfW * 2, if (row > 52) accentCol else armorDark)
        }

        // Colossal Boss Legs
        c.drawShadedPill(cx - 12, 78 + bob, cx - 14 + legSwing, footY, 6.5f, armorDark, armorMid, armorHi)
        c.drawShadedPill(cx + 12, 78 + bob, cx + 14 - legSwing, footY, 6.5f, armorDark, armorMid, armorHi)

        // Massive Boss Torso & Spiked Pauldrons
        c.drawShadedOval(cx, 58 + bob, 22, 24, armorDark, armorMid, armorHi, glowCol)
        c.drawShadedOval(cx - 22, 46 + bob, 12, 9, armorDark, accentCol, glowCol)
        c.drawShadedOval(cx + 22, 46 + bob, 12, 9, armorDark, accentCol, glowCol)

        // Glowing Boss Core / Ribcage Fissures
        for (r in -6..6 step 4) {
          c.hLine(cx - 10, 56 + bob + r, 20, accentCol)
          c.hLine(cx - 5, 56 + bob + r, 10, glowCol)
        }

        // Boss Head, Curved Abyssal Horns / Treant Crown & Blazing Eyes
        c.drawShadedOval(cx + 2, 28 + bob, 14, 14, armorDark, armorMid, armorHi)
        c.drawShadedPill(cx - 10, 22 + bob, cx - 22, 6 + bob, 3.8f, armorDark, accentCol, glowCol)
        c.drawShadedPill(cx + 12, 22 + bob, cx + 24, 6 + bob, 3.8f, armorDark, accentCol, glowCol)
        c.fillRect(cx - 3, 27 + bob, 5, 4, glowCol)
        c.fillRect(cx + 6, 27 + bob, 5, 4, glowCol)

        // Colossal Boss Greatsword / War-Cleaver / Root-Hammer
        val tipX = if (isStriking) cx + 48 else cx + 28
        val tipY = if (isStriking) 64 + bob else 8 + bob
        c.drawShadedPill(cx + 18, 72 + bob, tipX, tipY, 5.2f, armorDark, 0xFFCBD5E1.toInt(), 0xFFFFFFFF.toInt())
        c.drawShadedPill(cx + 18, 72 + bob, (cx + 18 + tipX) / 2, (72 + bob + tipY) / 2, 2.0f, accentCol, glowCol, 0xFFFFFFFF.toInt())
        if (isStriking) {
          drawPixelSlashArc(c, cx + 18, 56 + bob, 34, -60, 55, accentCol, glowCol)
        }
      }
    }

    if (flashWhite) {
      for (i in c.pixels.indices) {
        if ((c.pixels[i] ushr 24) > 128) {
          c.pixels[i] = 0xFFFFFFFF.toInt()
        }
      }
    }

    c.applyCrispOutline(0xFF07050E.toInt())
    return c
  }
}
