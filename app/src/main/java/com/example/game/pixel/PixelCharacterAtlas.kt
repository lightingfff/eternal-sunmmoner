package com.example.game.pixel

import androidx.compose.ui.graphics.ImageBitmap
import com.example.data.model.HeroClassType
import com.example.game.map.EnemyArchetype

/**
 * Multi-frame 4-directional 3/4 pixel-art sprite sheet for a Hero, Master, NPC, or Monster.
 * Cached lazily once in ARGB_8888 ImageBitmaps and rendered with FilterQuality.None.
 */
class DirectionalCharacterSprites(
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
    if (attackProgress > 0.06f) {
      val list = if (facingRight) attackRight else attackLeft
      val idx = if (attackProgress > 0.55f) 0 else (1.coerceAtMost(list.lastIndex))
      return list[idx]
    }
    if (isMoving) {
      val list = when {
        facingNorth && facingRight -> walkBackRight
        facingNorth && !facingRight -> walkBackLeft
        facingRight -> walkRight
        else -> walkLeft
      }
      val idx = ((timeSec * 8.5f + seed).toInt() and 0x7FFFFFFF) % list.size
      return list[idx]
    }
    val list = when {
      facingNorth && facingRight -> idleBackRight
      facingNorth && !facingRight -> idleBackLeft
      facingRight -> idleRight
      else -> idleLeft
    }
    val idx = ((timeSec * 2.4f + seed).toInt() and 0x7FFFFFFF) % list.size
    return list[idx]
  }
}

object PixelCharacterAtlas {

  const val CHAR_WIDTH_PX = 38
  const val CHAR_HEIGHT_PX = 50

  val masterSprites: DirectionalCharacterSprites by lazy {
    buildHumanoidSheet(isMaster = true, heroClass = HeroClassType.MAGE)
  }

  private val heroSheets = mutableMapOf<HeroClassType, DirectionalCharacterSprites>()
  private val enemySheets = mutableMapOf<EnemyArchetype, DirectionalCharacterSprites>()
  internal val builtHeroSheetCount get() = heroSheets.size
  internal val builtEnemySheetCount get() = enemySheets.size

  fun heroSheet(heroClass: HeroClassType): DirectionalCharacterSprites =
    heroSheets.getOrPut(heroClass) { buildHumanoidSheet(isMaster = false, heroClass = heroClass) }

  fun enemySheet(archetype: EnemyArchetype): DirectionalCharacterSprites =
    enemySheets.getOrPut(archetype) { buildEnemySheet(archetype) }

  private enum class Pose {
    IDLE_0,
    IDLE_1,
    WALK_0,
    WALK_1,
    WALK_2,
    WALK_3,
    ATTACK_WINDUP,
    ATTACK_STRIKE,
    HURT,
    REST_SEATED,
  }

  private fun buildHumanoidSheet(
    isMaster: Boolean,
    heroClass: HeroClassType,
  ): DirectionalCharacterSprites {
    fun pair(pose: Pose, backView: Boolean = false, flashWhite: Boolean = false): Pair<ImageBitmap, ImageBitmap> {
      val rightCanvas = drawHumanoidFrame34(isMaster, heroClass, pose, backView, flashWhite)
      val leftCanvas = rightCanvas.flipHorizontalCanvas()
      return rightCanvas.toImageBitmap() to leftCanvas.toImageBitmap()
    }

    val i0 = pair(Pose.IDLE_0)
    val i1 = pair(Pose.IDLE_1)
    val w0 = pair(Pose.WALK_0)
    val w1 = pair(Pose.WALK_1)
    val w2 = pair(Pose.WALK_2)
    val w3 = pair(Pose.WALK_3)

    val bi0 = pair(Pose.IDLE_0, backView = true)
    val bi1 = pair(Pose.IDLE_1, backView = true)
    val bw0 = pair(Pose.WALK_0, backView = true)
    val bw1 = pair(Pose.WALK_1, backView = true)
    val bw2 = pair(Pose.WALK_2, backView = true)
    val bw3 = pair(Pose.WALK_3, backView = true)

    val a0 = pair(Pose.ATTACK_WINDUP)
    val a1 = pair(Pose.ATTACK_STRIKE)
    val hurt = pair(Pose.HURT, flashWhite = true)
    val rest = pair(Pose.REST_SEATED)

    return DirectionalCharacterSprites(
      idleRight = listOf(i0.first, i1.first),
      idleLeft = listOf(i0.second, i1.second),
      walkRight = listOf(w0.first, w1.first, w2.first, w3.first),
      walkLeft = listOf(w0.second, w1.second, w2.second, w3.second),
      idleBackRight = listOf(bi0.first, bi1.first),
      idleBackLeft = listOf(bi0.second, bi1.second),
      walkBackRight = listOf(bw0.first, bw1.first, bw2.first, bw3.first),
      walkBackLeft = listOf(bw0.second, bw1.second, bw2.second, bw3.second),
      attackRight = listOf(a0.first, a1.first),
      attackLeft = listOf(a0.second, a1.second),
      hurtRight = hurt.first,
      hurtLeft = hurt.second,
      restRight = rest.first,
      restLeft = rest.second,
    )
  }

  /**
   * Crafts a rich 38x50 True Oblique 3/4 Dark-Fantasy RPG Character Sprite (3-head heroic proportions
   * matching Reference Images 3 & 4: sculpted hair, layered cape, torso armor/coat, articulated legs & weapon).
   */
  private fun drawHumanoidFrame34(
    isMaster: Boolean,
    heroClass: HeroClassType,
    pose: Pose,
    backView: Boolean,
    flashWhite: Boolean,
  ): PixelCanvas {
    val w = CHAR_WIDTH_PX
    val h = CHAR_HEIGHT_PX
    val c = PixelCanvas(w, h)

    val bobY = when (pose) {
      Pose.IDLE_1 -> 1
      Pose.WALK_1, Pose.WALK_3 -> -1
      Pose.REST_SEATED -> 5
      else -> 0
    }
    val recoilX = if (pose == Pose.HURT) -2 else if (pose == Pose.ATTACK_STRIKE) 3 else 0

    val skin = 0xFFFED7AA.toInt()
    val skinShade = 0xFFDDA172.toInt()
    val bootDark = 0xFF1F172B.toInt()
    val bootMid = 0xFF36294A.toInt()
    val bootHi = 0xFF544270.toInt()
    val trouserCol = 0xFF28223D.toInt()

    val tunicMain: Int
    val tunicHi: Int
    val tunicShade: Int
    val trimAccent: Int
    val capeMain: Int
    val capeShade: Int
    val hairMain: Int
    val hairHi: Int
    val hairShade: Int
    val eyeColor: Int

    if (isMaster) {
      // Master Summoner (Ref 4 Sovereign Commander): Midnight-Astral Coat, Gold Filigree, Royal Violet Mantle, Raven-Silver Hair
      tunicMain = 0xFF28204D.toInt()
      tunicHi = 0xFF3E3273.toInt()
      tunicShade = 0xFF171130.toInt()
      trimAccent = 0xFFF5B841.toInt()
      capeMain = 0xFF5B21B6.toInt()
      capeShade = 0xFF3B0764.toInt()
      hairMain = 0xFF1E293B.toInt()
      hairHi = 0xFF475569.toInt()
      hairShade = 0xFF0F172A.toInt()
      eyeColor = 0xFF38BDF8.toInt()
    } else {
      when (heroClass) {
        HeroClassType.KNIGHT -> {
          tunicMain = 0xFF94A3B8.toInt()
          tunicHi = 0xFFE2E8F0.toInt()
          tunicShade = 0xFF475569.toInt()
          trimAccent = 0xFF38BDF8.toInt()
          capeMain = 0xFF1D4ED8.toInt()
          capeShade = 0xFF1E3A8A.toInt()
          hairMain = 0xFFCBD5E1.toInt()
          hairHi = 0xFFF8FAFC.toInt()
          hairShade = 0xFF64748B.toInt()
          eyeColor = 0xFF0284C7.toInt()
        }
        HeroClassType.ARCHER -> {
          // Crimson-haired or Emerald-cloaked Ranger (Ref 4 MIRA / ORIN inspiration!)
          tunicMain = 0xFF2D6A4F.toInt()
          tunicHi = 0xFF40916C.toInt()
          tunicShade = 0xFF1B4332.toInt()
          trimAccent = 0xFFF59E0B.toInt()
          capeMain = 0xFF2D6A4F.toInt()
          capeShade = 0xFF143628.toInt()
          hairMain = 0xFFDC2626.toInt()
          hairHi = 0xFFF87171.toInt()
          hairShade = 0xFF7F1D1D.toInt()
          eyeColor = 0xFFFBBF24.toInt()
        }
        HeroClassType.MAGE -> {
          tunicMain = 0xFF6B21A8.toInt()
          tunicHi = 0xFF9333EA.toInt()
          tunicShade = 0xFF3B0764.toInt()
          trimAccent = 0xFFE879F9.toInt()
          capeMain = 0xFF4C1D95.toInt()
          capeShade = 0xFF2E1065.toInt()
          hairMain = 0xFF7DD3FC.toInt()
          hairHi = 0xFFE0F2FE.toInt()
          hairShade = 0xFF0284C7.toInt()
          eyeColor = 0xFFFDE047.toInt()
        }
        HeroClassType.ASSASSIN -> {
          tunicMain = 0xFF334155.toInt()
          tunicHi = 0xFF475569.toInt()
          tunicShade = 0xFF0F172A.toInt()
          trimAccent = 0xFFF43F5E.toInt()
          capeMain = 0xFF9F1239.toInt()
          capeShade = 0xFF4C0519.toInt()
          hairMain = 0xFF94A3B8.toInt()
          hairHi = 0xFFE2E8F0.toInt()
          hairShade = 0xFF475569.toInt()
          eyeColor = 0xFFF43F5E.toInt()
        }
        HeroClassType.PRIEST -> {
          tunicMain = 0xFFF1F5F9.toInt()
          tunicHi = 0xFFFFFFFF.toInt()
          tunicShade = 0xFF94A3B8.toInt()
          trimAccent = 0xFFFBBF24.toInt()
          capeMain = 0xFF0284C7.toInt()
          capeShade = 0xFF075985.toInt()
          hairMain = 0xFFFDE047.toInt()
          hairHi = 0xFFFEF9C3.toInt()
          hairShade = 0xFFCA8A04.toInt()
          eyeColor = 0xFF0284C7.toInt()
        }
        HeroClassType.BERSERKER -> {
          tunicMain = 0xFF991B1B.toInt()
          tunicHi = 0xFFDC2626.toInt()
          tunicShade = 0xFF450A0A.toInt()
          trimAccent = 0xFFFB923C.toInt()
          capeMain = 0xFF57221B.toInt()
          capeShade = 0xFF2E100C.toInt()
          hairMain = 0xFFFB923C.toInt()
          hairHi = 0xFFFED7AA.toInt()
          hairShade = 0xFF9A3412.toInt()
          eyeColor = 0xFFFEF08A.toInt()
        }
      }
    }

    val cx = 18 + recoilX
    val headY = 6 + bobY
    val torsoY = 20 + bobY
    val legY = 35 + bobY / 2

    val capeFlap = when (pose) {
      Pose.WALK_0, Pose.WALK_2 -> -2
      Pose.WALK_1, Pose.WALK_3 -> -3
      Pose.ATTACK_STRIKE -> -4
      Pose.IDLE_1 -> -1
      else -> 0
    }

    // 1. Back Layer: Flowing Cloak (when facing South/Front 3/4)
    if (!backView) {
      c.fillRect(cx - 8 + capeFlap, torsoY + 1, 12, 19, capeShade)
      c.fillRect(cx - 7 + capeFlap, torsoY + 1, 9, 17, capeMain)
      if (!isMaster && heroClass == HeroClassType.ARCHER) {
        c.fillRect(cx - 8, torsoY - 3, 4, 12, 0xFF78350F.toInt())
        c.vLine(cx - 8, torsoY - 5, 3, 0xFFFEF08A.toInt())
        c.vLine(cx - 6, torsoY - 5, 3, 0xFFF87171.toInt())
      }
    }

    // 2. Articulated 3/4 Legs, Trousers & Armored Greaves/Boots
    val leftLegDx: Int
    val rightLegDx: Int
    val leftLegLift: Int
    val rightLegLift: Int
    when (pose) {
      Pose.WALK_0 -> { leftLegDx = -4; rightLegDx = 4; leftLegLift = -2; rightLegLift = 0 }
      Pose.WALK_1 -> { leftLegDx = -1; rightLegDx = 2; leftLegLift = 0; rightLegLift = -1 }
      Pose.WALK_2 -> { leftLegDx = 4; rightLegDx = -3; leftLegLift = 0; rightLegLift = -2 }
      Pose.WALK_3 -> { leftLegDx = 1; rightLegDx = -1; leftLegLift = -1; rightLegLift = 0 }
      Pose.ATTACK_STRIKE -> { leftLegDx = -4; rightLegDx = 4; leftLegLift = 0; rightLegLift = 0 }
      Pose.REST_SEATED -> { leftLegDx = 1; rightLegDx = 5; leftLegLift = -3; rightLegLift = -3 }
      else -> { leftLegDx = -3; rightLegDx = 3; leftLegLift = 0; rightLegLift = 0 }
    }

    // Thighs
    c.fillRect(cx + leftLegDx - 2, legY + leftLegLift, 4, 5, trouserCol)
    c.fillRect(cx + rightLegDx - 2, legY + rightLegLift, 4, 5, trouserCol)
    // Greaves & cuffed boots
    c.fillRect(cx + leftLegDx - 2, legY + 4 + leftLegLift, 4, 7, bootDark)
    c.fillRect(cx + leftLegDx - 1, legY + 4 + leftLegLift, 3, 6, bootMid)
    c.hLine(cx + leftLegDx - 2, legY + 4 + leftLegLift, 4, bootHi)

    c.fillRect(cx + rightLegDx - 2, legY + 4 + rightLegLift, 5, 7, bootDark)
    c.fillRect(cx + rightLegDx - 1, legY + 4 + rightLegLift, 3, 6, bootMid)
    c.hLine(cx + rightLegDx - 2, legY + 4 + rightLegLift, 4, bootHi)

    // 3. Sculpted 3/4 Torso Armor / Tapered Coat + Sleeves + Pauldrons (Zero boxy rectangle look!)
    // Flared lower coat hem / armored tassets
    c.fillRect(cx - 7, torsoY + 10, 15, 6, tunicShade)
    c.fillRect(cx - 6, torsoY + 10, 6, 5, tunicMain)
    c.fillRect(cx + 2, torsoY + 10, 6, 5, tunicMain)
    // Tapered waist & sculpted chest
    c.fillRect(cx - 5, torsoY + 2, 11, 9, tunicShade)
    c.fillRect(cx - 4, torsoY + 2, 9, 8, tunicMain)
    c.fillRect(cx - 3, torsoY + 2, 4, 7, tunicHi)
    // Back arm & sleeve
    c.fillRect(cx - 8, torsoY + 2, 3, 8, tunicShade)
    c.fillRect(cx - 8, torsoY + 9, 2, 2, skinShade)
    // Leather belt & gold buckle
    c.fillRect(cx - 5, torsoY + 9, 11, 2, 0xFF3B2314.toInt())
    c.fillRect(cx, torsoY + 9, 3, 2, 0xFFF5B841.toInt())
    // Rounded 3/4 Shoulder Pauldrons & Collar Trim
    c.fillEllipse(cx - 5, torsoY + 2, 3, 2, trimAccent)
    c.fillEllipse(cx + 5, torsoY + 2, 3, 2, trimAccent)
    c.hLine(cx - 4, torsoY + 1, 9, 0xFFFFFBEB.toInt())
    c.vLine(cx + 1, torsoY + 3, 6, trimAccent)

    // If walking North (Back-3/4 View), drape the full billowing Cloak & Quiver over the back!
    if (backView) {
      c.fillEllipse(cx + capeFlap / 2, torsoY + 11, 8, 9, capeShade)
      c.fillEllipse(cx + capeFlap / 2, torsoY + 10, 6, 8, capeMain)
      c.hLine(cx - 5, torsoY + 2, 11, trimAccent)
      if (!isMaster && heroClass == HeroClassType.ARCHER) {
        c.fillRect(cx - 2, torsoY + 3, 5, 12, 0xFF78350F.toInt())
        c.hLine(cx - 2, torsoY + 1, 5, 0xFFFEF08A.toInt())
      }
    }

    // 4. Organic Rounded 3/4 Head, Jawline, Expressive Eyes & Sculpted Hair Locks / Headgear
    c.fillCircle(cx, headY + 8, 6, skinShade)
    if (!backView) {
      c.fillCircle(cx + 1, headY + 8, 5, skin)
      // Crisp 2x3 expressive anime/manhwa pixel eyes facing 3/4 right!
      c.fillRect(cx, headY + 8, 2, 3, 0xFFFFFFFF.toInt())
      c.fillRect(cx + 1, headY + 8, 1, 3, eyeColor)
      c.fillRect(cx + 4, headY + 8, 2, 3, 0xFFFFFFFF.toInt())
      c.fillRect(cx + 5, headY + 8, 1, 3, eyeColor)
      c.set(cx + 3, headY + 11, skinShade)
    } else {
      c.fillCircle(cx, headY + 8, 6, hairShade)
      c.fillCircle(cx, headY + 7, 5, hairMain)
    }

    // Layered Sculpted Hair Crown & Class Headgear
    if (isMaster) {
      // Tousled raven-dark manhwa protagonist hair crown + bangs + Sovereign Gold Circlet!
      c.fillEllipse(cx, headY + 4, 7, 4, hairMain)
      c.fillEllipse(cx - 1, headY + 3, 4, 2, hairHi)
      c.fillRect(cx - 7, headY + 5, 3, 7, hairShade)
      if (!backView) {
        c.vLine(cx - 1, headY + 5, 3, hairMain)
        c.vLine(cx + 3, headY + 5, 3, hairMain)
        c.vLine(cx + 6, headY + 5, 4, hairShade)
      }
      c.hLine(cx - 5, headY + 4, 11, 0xFFF5B841.toInt())
      c.set(cx + 2, headY + 3, 0xFF38BDF8.toInt())
    } else if (heroClass == HeroClassType.KNIGHT) {
      // Rounded Silver Vanguard Helm + Crowned Visor + Flowing Azure Plume
      c.fillEllipse(cx, headY + 4, 7, 4, hairMain)
      c.fillEllipse(cx - 1, headY + 3, 4, 2, hairHi)
      c.fillRect(cx - 7, headY + 5, 3, 7, hairShade)
      c.hLine(cx - 4, headY + 6, 10, 0xFF38BDF8.toInt())
      c.fillEllipse(cx - 2, headY - 1, 4, 2, 0xFF1D4ED8.toInt())
      c.hLine(cx - 4, headY - 2, 5, 0xFF60A5FA.toInt())
    } else if (heroClass == HeroClassType.MAGE) {
      // Pointed Curved Astral Witch-Arcanist Hat + Flowing Ice-Blue Hair Locks
      c.fillRect(cx - 7, headY + 4, 4, 10, hairMain)
      if (backView) c.fillEllipse(cx, headY + 9, 6, 6, hairMain)
      c.fillEllipse(cx, headY + 4, 10, 2, 0xFF3B0764.toInt())
      c.fillEllipse(cx, headY + 1, 6, 3, 0xFF581C87.toInt())
      c.fillRect(cx - 4, headY - 3, 7, 3, 0xFF7E22CE.toInt())
      c.fillRect(cx - 3, headY - 6, 4, 3, 0xFFA855F7.toInt())
      c.hLine(cx - 5, headY + 3, 10, 0xFFF5B841.toInt())
    } else if (heroClass == HeroClassType.ARCHER) {
      // Flowing Crimson Hair + Ranger Hood Trim (Ref 4 MIRA / ORIN!)
      c.fillEllipse(cx, headY + 4, 7, 4, hairMain)
      c.fillEllipse(cx - 1, headY + 3, 4, 2, hairHi)
      c.fillRect(cx - 7, headY + 5, 4, 10, hairShade)
      c.vLine(cx + 6, headY + 5, 6, hairMain)
      if (backView) c.fillEllipse(cx, headY + 9, 6, 6, hairMain)
      c.hLine(cx - 5, headY + 4, 10, 0xFFF59E0B.toInt())
    } else if (heroClass == HeroClassType.ASSASSIN) {
      c.fillEllipse(cx, headY + 4, 7, 4, 0xFF1E293B.toInt())
      c.fillEllipse(cx - 1, headY + 3, 4, 2, 0xFF475569.toInt())
      c.fillRect(cx - 7, headY + 5, 3, 7, 0xFF0F172A.toInt())
      if (!backView) c.fillRect(cx - 4, headY + 10, 9, 3, 0xFFBE123C.toInt())
    } else if (heroClass == HeroClassType.PRIEST) {
      c.fillEllipse(cx, headY + 4, 7, 4, hairMain)
      c.fillEllipse(cx - 1, headY + 3, 4, 2, hairHi)
      c.fillRect(cx - 7, headY + 5, 4, 11, hairShade)
      c.vLine(cx + 6, headY + 5, 7, hairMain)
      if (backView) c.fillEllipse(cx, headY + 9, 6, 6, hairMain)
      c.hLine(cx - 4, headY + 3, 9, 0xFFFBBF24.toInt())
      c.set(cx + 2, headY + 2, 0xFF34D399.toInt())
    } else {
      // Berserker Horned War-Crown & Blazing Mane
      c.fillEllipse(cx, headY + 4, 7, 4, hairMain)
      c.fillEllipse(cx - 1, headY + 3, 4, 2, hairHi)
      c.fillRect(cx - 7, headY - 2, 2, 5, 0xFFE2E8F0.toInt())
      c.fillRect(cx + 5, headY - 2, 2, 5, 0xFFE2E8F0.toInt())
    }

    // 5. Detailed Class Weapons, Shields & Action Poses
    val armX = cx + (if (pose == Pose.ATTACK_STRIKE) 8 else if (pose == Pose.ATTACK_WINDUP) 3 else 5)
    val armY = torsoY + 3
    // Front sleeve + gauntlet hand
    c.fillRect(armX - 3, armY, 3, 4, tunicMain)
    c.fillRect(armX - 1, armY + 2, 3, 3, skin)

    if (isMaster) {
      val staffTop = if (pose == Pose.ATTACK_STRIKE) 4 else 8
      c.fillRect(armX + 2, staffTop + bobY, 2, 32, 0xFFD99B26.toInt())
      c.fillCircle(armX + 3, staffTop + bobY, 4, 0xFF38BDF8.toInt())
      c.fillCircle(armX + 3, staffTop + bobY, 2, 0xFFFFFFFF.toInt())
    } else {
      when (heroClass) {
        HeroClassType.KNIGHT -> {
          // Left-hand Azure Kite Shield with Gold Cross + Right-hand Silver Broadsword
          c.fillEllipse(cx - 7, torsoY + 7, 4, 6, 0xFF1E3A8A.toInt())
          c.fillEllipse(cx - 7, torsoY + 7, 3, 5, 0xFF2563EB.toInt())
          c.vLine(cx - 7, torsoY + 3, 8, 0xFFF5B841.toInt())
          c.hLine(cx - 9, torsoY + 6, 5, 0xFFF5B841.toInt())
          if (pose == Pose.ATTACK_STRIKE) {
            c.fillRect(armX, armY + 1, 13, 3, 0xFFF8FAFC.toInt())
            c.hLine(armX, armY + 1, 13, 0xFF38BDF8.toInt())
            c.vLine(armX, armY - 2, 9, 0xFFF5B841.toInt())
          } else {
            c.fillRect(armX + 2, 7 + bobY, 3, 20, 0xFFE2E8F0.toInt())
            c.vLine(armX + 2, 7 + bobY, 20, 0xFFFFFFFF.toInt())
            c.fillRect(armX - 1, 25 + bobY, 9, 2, 0xFFF5B841.toInt())
          }
        }
        HeroClassType.ARCHER -> {
          c.vLine(armX + 4, 12 + bobY, 20, 0xFFD97706.toInt())
          c.vLine(armX + 5, 13 + bobY, 18, 0xFF92400E.toInt())
          c.hLine(armX + 2, 11 + bobY, 3, 0xFFF5B841.toInt())
          c.hLine(armX + 2, 32 + bobY, 3, 0xFFF5B841.toInt())
          c.vLine(armX + 2, 12 + bobY, 20, 0xFFE2E8F0.toInt())
          if (pose == Pose.ATTACK_WINDUP || pose == Pose.ATTACK_STRIKE) {
            c.hLine(armX - 1, 22 + bobY, 10, 0xFFFEF08A.toInt())
            c.fillRect(armX + 8, 21 + bobY, 3, 3, 0xFF38BDF8.toInt())
          }
        }
        HeroClassType.MAGE, HeroClassType.PRIEST -> {
          val orb = if (heroClass == HeroClassType.MAGE) 0xFFE879F9.toInt() else 0xFF34D399.toInt()
          c.fillRect(armX + 2, 9 + bobY, 2, 29, 0xFF92400E.toInt())
          c.fillCircle(armX + 3, 8 + bobY, if (pose == Pose.ATTACK_STRIKE) 5 else 3, orb)
          c.fillCircle(armX + 3, 8 + bobY, 1, 0xFFFFFFFF.toInt())
        }
        HeroClassType.ASSASSIN -> {
          if (pose == Pose.ATTACK_STRIKE) {
            c.fillRect(armX, armY, 11, 2, 0xFFF43F5E.toInt())
            c.fillRect(armX - 2, armY + 4, 10, 2, 0xFFE2E8F0.toInt())
          } else {
            c.fillRect(armX + 2, 16 + bobY, 2, 11, 0xFFE2E8F0.toInt())
            c.vLine(armX + 3, 15 + bobY, 11, 0xFFF43F5E.toInt())
          }
        }
        HeroClassType.BERSERKER -> {
          c.fillRect(armX + 3, 6 + bobY, 2, 29, 0xFF78350F.toInt())
          c.fillRect(armX, 8 + bobY, 9, 10, 0xFFCBD5E1.toInt())
          c.vLine(armX + 8, 8 + bobY, 10, 0xFFEF4444.toInt())
        }
      }
    }

    if (flashWhite) {
      for (i in c.pixels.indices) {
        if ((c.pixels[i] ushr 24) > 128) {
          c.pixels[i] = 0xFFFFF1F2.toInt()
        }
      }
    }

    c.applyCrispOutline(0xFF090612.toInt())
    return c
  }

  private fun buildEnemySheet(archetype: EnemyArchetype): DirectionalCharacterSprites {
    fun pair(pose: Pose, flashWhite: Boolean = false): Pair<ImageBitmap, ImageBitmap> {
      val rightCanvas = drawEnemyFrame34(archetype, pose, flashWhite)
      val leftCanvas = rightCanvas.flipHorizontalCanvas()
      return rightCanvas.toImageBitmap() to leftCanvas.toImageBitmap()
    }

    val i0 = pair(Pose.IDLE_0)
    val i1 = pair(Pose.IDLE_1)
    val w0 = pair(Pose.WALK_0)
    val w1 = pair(Pose.WALK_1)
    val w2 = pair(Pose.WALK_2)
    val w3 = pair(Pose.WALK_3)
    val a0 = pair(Pose.ATTACK_WINDUP)
    val a1 = pair(Pose.ATTACK_STRIKE)
    val hurt = pair(Pose.HURT, flashWhite = true)

    return DirectionalCharacterSprites(
      idleRight = listOf(i0.first, i1.first),
      idleLeft = listOf(i0.second, i1.second),
      walkRight = listOf(w0.first, w1.first, w2.first, w3.first),
      walkLeft = listOf(w0.second, w1.second, w2.second, w3.second),
      idleBackRight = listOf(i0.first, i1.first),
      idleBackLeft = listOf(i0.second, i1.second),
      walkBackRight = listOf(w0.first, w1.first, w2.first, w3.first),
      walkBackLeft = listOf(w0.second, w1.second, w2.second, w3.second),
      attackRight = listOf(a0.first, a1.first),
      attackLeft = listOf(a0.second, a1.second),
      hurtRight = hurt.first,
      hurtLeft = hurt.second,
      restRight = hurt.first,
      restLeft = hurt.second,
    )
  }

  private fun drawEnemyFrame34(
    archetype: EnemyArchetype,
    pose: Pose,
    flashWhite: Boolean,
  ): PixelCanvas {
    val isBoss = archetype.isBoss
    val w = if (isBoss) 56 else 38
    val h = if (isBoss) 62 else 46
    val c = PixelCanvas(w, h)

    val bob = when (pose) {
      Pose.IDLE_1 -> 1
      Pose.WALK_1, Pose.WALK_3 -> -1
      else -> 0
    }
    val lunge = if (pose == Pose.ATTACK_STRIKE) 3 else if (pose == Pose.HURT) -2 else 0

    when (archetype) {
      EnemyArchetype.DIRE_WOLF -> {
        val bx = 18 + lunge
        val by = 26 + bob
        val stride = when (pose) {
          Pose.WALK_0, Pose.ATTACK_STRIKE -> 3
          Pose.WALK_2 -> -3
          else -> 0
        }
        c.fillRect(bx - 10 - stride / 2, by + 7, 4, 8, 0xFF334155.toInt())
        c.fillRect(bx - 5 + stride / 2, by + 7, 4, 8, 0xFF1E293B.toInt())
        c.fillRect(bx + 4 - stride / 2, by + 7, 4, 8, 0xFF334155.toInt())
        c.fillRect(bx + 8 + stride / 2, by + 7, 4, 8, 0xFF1E293B.toInt())
        c.fillEllipse(bx - 13, by, 5, 3, 0xFF475569.toInt())
        c.fillEllipse(bx, by + 2, 11, 6, 0xFF475569.toInt())
        c.fillEllipse(bx + 3, by, 8, 6, 0xFF64748B.toInt())
        c.hLine(bx - 5, by - 4, 10, 0xFF94A3B8.toInt())
        c.fillEllipse(bx + 10, by - 3, 6, 4, 0xFF475569.toInt())
        c.fillRect(bx + 7, by - 9, 3, 4, 0xFF334155.toInt())
        c.fillRect(bx + 11, by - 4, 2, 2, 0xFFEF4444.toInt())
        c.hLine(bx + 13, by, 3, 0xFFF8FAFC.toInt())
      }

      EnemyArchetype.FIELD_GOBLIN_RAIDER, EnemyArchetype.FIELD_GOBLIN_ARCHER -> {
        // Crouched, organic 3/4 Goblin Raider/Archer with leather hood/cap, pointed ears, buckler & scimitar!
        val cx = 18 + lunge
        val skin = 0xFF4D7C3F.toInt()
        val skinHi = 0xFF74A843.toInt()
        val skinDark = 0xFF2D4F25.toInt()
        val leather = if (archetype.isRanged) 0xFF854D0E.toInt() else 0xFF78350F.toInt()
        val leatherHi = 0xFFA16207.toInt()
        val stride = if (pose == Pose.WALK_0) 3 else if (pose == Pose.WALK_2) -3 else 0

        // Boots & bent legs
        c.fillRect(cx - 4 + stride, 34, 4, 6, 0xFF3B2314.toInt())
        c.fillRect(cx + 2 - stride, 34, 4, 6, 0xFF3B2314.toInt())
        // Rounded leather brigandine + fur shoulder pad + belt
        c.fillEllipse(cx, 27 + bob, 6, 7, leather)
        c.fillEllipse(cx - 1, 26 + bob, 4, 5, leatherHi)
        c.fillEllipse(cx - 5, 22 + bob, 4, 3, 0xFF57534E.toInt()) // Fur pauldron
        c.hLine(cx - 5, 30 + bob, 11, 0xFFD97706.toInt())
        // Sculpted Goblin head + long pointed ears + crimson/amber eyes
        c.fillCircle(cx + 1, 16 + bob, 6, skinDark)
        c.fillCircle(cx + 2, 16 + bob, 5, skin)
        c.fillEllipse(cx + 1, 13 + bob, 4, 2, skinHi)
        // Swept-back pointed goblin ears
        c.fillRect(cx - 9, 14 + bob, 5, 2, skinDark)
        c.hLine(cx - 8, 13 + bob, 4, skin)
        c.fillRect(cx + 6, 14 + bob, 4, 2, skin)
        // Hood / bandana crest
        if (archetype.isRanged) {
          c.fillEllipse(cx, 12 + bob, 6, 3, 0xFF3F6212.toInt())
        } else {
          c.fillEllipse(cx, 11 + bob, 5, 2, 0xFF991B1B.toInt())
        }
        c.fillRect(cx + 1, 16 + bob, 2, 2, 0xFFFEF08A.toInt())
        c.fillRect(cx + 5, 16 + bob, 2, 2, 0xFFEF4444.toInt())

        if (archetype.isRanged) {
          c.vLine(cx + 8, 14 + bob, 16, 0xFFD97706.toInt())
          c.vLine(cx + 6, 15 + bob, 14, 0xFFE2E8F0.toInt())
          c.hLine(cx + 4, 22 + bob, 8, 0xFFFEF08A.toInt())
        } else {
          // Wooden round buckler on left arm + curved iron scimitar on right!
          c.fillCircle(cx - 6, 27 + bob, 4, 0xFF5C3A21.toInt())
          c.set(cx - 6, 27 + bob, 0xFFCBD5E1.toInt())
          c.fillRect(cx + 7, 13 + bob, 3, 13, 0xFFCBD5E1.toInt())
          c.fillRect(cx + 8, 11 + bob, 4, 4, 0xFFE2E8F0.toInt())
          c.hLine(cx + 5, 25 + bob, 6, 0xFFB45309.toInt())
        }
      }

      EnemyArchetype.FOREST_WRAITH -> {
        val cx = 18 + lunge
        val floatY = bob - 1
        c.fillRect(cx - 6, 14 + floatY, 13, 21, 0xFF1E3A8A.toInt())
        c.fillRect(cx - 4, 11 + floatY, 9, 19, 0xFF2563EB.toInt())
        c.vLine(cx - 5, 33 + floatY, 6, 0xFF38BDF8.toInt())
        c.vLine(cx + 3, 34 + floatY, 6, 0xFF38BDF8.toInt())
        c.fillRect(cx + 1, 16 + floatY, 2, 2, 0xFF7DD3FC.toInt())
        c.fillRect(cx + 4, 16 + floatY, 2, 2, 0xFF7DD3FC.toInt())
        c.fillCircle(cx + 9, 22 + floatY, 4, 0xFF38BDF8.toInt())
      }

      EnemyArchetype.RUIN_SENTINEL -> {
        val cx = 18 + lunge
        val stride = if (pose == Pose.WALK_0) 3 else if (pose == Pose.WALK_2) -3 else 0
        c.fillRect(cx - 5 + stride, 34, 4, 8, 0xFF1E293B.toInt())
        c.fillRect(cx + 2 - stride, 34, 4, 8, 0xFF1E293B.toInt())
        c.fillRect(cx - 6, 19 + bob, 13, 15, 0xFF334155.toInt())
        c.fillRect(cx - 8, 19 + bob, 17, 4, 0xFF94A3B8.toInt())
        c.fillRect(cx - 5, 8 + bob, 11, 11, 0xFF475569.toInt())
        c.hLine(cx - 1, 14 + bob, 6, 0xFFF43F5E.toInt())
        c.fillRect(cx + 9, 5 + bob, 2, 34, 0xFF94A3B8.toInt())
        c.fillRect(cx + 10, 6 + bob, 5, 8, 0xFFF43F5E.toInt())
      }

      EnemyArchetype.GOBLIN_WAR_CAPTAIN, EnemyArchetype.ELDER_TREANT, EnemyArchetype.ABYSSAL_EXECUTIONER -> {
        val cx = 27 + lunge
        val stride = if (pose == Pose.WALK_0) 4 else if (pose == Pose.WALK_2) -4 else 0
        val mainCol: Int
        val hiCol: Int
        val trimCol: Int

        when (archetype) {
          EnemyArchetype.GOBLIN_WAR_CAPTAIN -> {
            mainCol = 0xFF365314.toInt()
            hiCol = 0xFF65A30D.toInt()
            trimCol = 0xFFEF4444.toInt()
          }
          EnemyArchetype.ELDER_TREANT -> {
            mainCol = 0xFF3E2723.toInt()
            hiCol = 0xFF16A34A.toInt()
            trimCol = 0xFF22D3EE.toInt()
          }
          else -> {
            mainCol = 0xFF1E1B4B.toInt()
            hiCol = 0xFF4F46E5.toInt()
            trimCol = 0xFFE11D48.toInt()
          }
        }

        c.fillRect(cx - 15, 20 + bob, 25, 28, trimCol)
        c.fillRect(cx - 12, 22 + bob, 20, 24, mainCol)
        c.fillRect(cx - 8 + stride, 46, 6, 11, 0xFF1E1B2E.toInt())
        c.fillRect(cx + 3 - stride, 46, 6, 11, 0xFF1E1B2E.toInt())
        c.fillRect(cx - 10, 21 + bob, 21, 25, mainCol)
        c.fillRect(cx - 7, 23 + bob, 15, 19, hiCol)
        c.fillRect(cx - 14, 20 + bob, 29, 6, trimCol)
        c.fillRect(cx - 7, 9 + bob, 15, 12, hiCol)
        c.fillRect(cx - 11, 3 + bob, 4, 8, 0xFFE2E8F0.toInt())
        c.fillRect(cx + 8, 3 + bob, 4, 8, 0xFFE2E8F0.toInt())
        c.fillRect(cx + 1, 14 + bob, 6, 2, 0xFFFEF08A.toInt())

        val wepX = cx + (if (pose == Pose.ATTACK_STRIKE) 14 else 11)
        c.fillRect(wepX, 6 + bob, 6, 36, 0xFFE2E8F0.toInt())
        c.vLine(wepX + 5, 6 + bob, 34, trimCol)
        c.fillRect(wepX - 3, 33 + bob, 12, 3, 0xFFF5B841.toInt())
      }
    }

    if (flashWhite) {
      for (i in c.pixels.indices) {
        if ((c.pixels[i] ushr 24) > 128) {
          c.pixels[i] = 0xFFFFFFFF.toInt()
        }
      }
    }

    c.applyCrispOutline(0xFF080510.toInt())
    return c
  }
}
