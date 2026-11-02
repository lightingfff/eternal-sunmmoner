package com.example.game.ai

import androidx.compose.ui.graphics.Color
import com.example.data.model.HeroClassType
import com.example.data.model.PersonalityType
import com.example.data.model.TacticalOrder
import com.example.game.combat.CombatAoEEffect
import com.example.game.combat.CombatProjectile
import com.example.game.combat.EnemyCombatUnit
import com.example.game.combat.FloatingCombatText
import com.example.game.combat.HeroCombatUnit
import com.example.game.map.CollisionPhysics
import com.example.game.tower.GeneratedMapLayout
import java.util.UUID
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Autonomous Hero AI & Enemy AI Engine for Eternal Summoner.
 * Evaluates threat, ally health, class positioning, kiting, formations, and all 8 Tactical Orders.
 */
object AutonomousHeroAi {

  fun updateHeroUnit(
    hero: HeroCombatUnit,
    dt: Float,
    allHeroes: List<HeroCombatUnit>,
    enemies: List<EnemyCombatUnit>,
    masterX: Float,
    masterY: Float,
    order: TacticalOrder,
    designatedTargetId: String?,
    moveBeacon: Pair<Float, Float>?,
    mapLayout: GeneratedMapLayout,
    spawnProjectile: (CombatProjectile) -> Unit,
    spawnAoE: (CombatAoEEffect) -> Unit,
    spawnText: (FloatingCombatText) -> Unit,
    dealDamageToEnemy: (EnemyCombatUnit, Int, Boolean) -> Unit,
    protectedAllyPosition: Pair<Float, Float>? = null,
  ) {
    if (hero.knockedOut) return

    hero.attackCooldown = (hero.attackCooldown - dt).coerceAtLeast(0f)
    hero.skillCooldown = (hero.skillCooldown - dt).coerceAtLeast(0f)
    hero.attackAnim = (hero.attackAnim - dt * 2.8f).coerceAtLeast(0f)
    hero.hurtFlash = (hero.hurtFlash - dt * 3.5f).coerceAtLeast(0f)

    val livingEnemies = enemies.filter { !it.dead }
    val livingAllies = allHeroes.filter { !it.knockedOut }

    // The sample's contact frame is also the real damage event. Keep the existing
    // Knight skill values/cooldowns; only its delivery gains a visible windup.
    if (hero.premiumSample && hero.contactAttack.active) {
      if (order == TacticalOrder.RETREAT) {
        hero.contactAttack.cancel()
        hero.attackAnim = 0f
      } else {
        hero.isMoving = false
        if (hero.contactAttack.advance(dt)) {
          if (hero.contactAttack.skill) {
            hero.shieldHp = (hero.shieldHp + 65 + hero.skillLevel * 12).coerceAtMost(220)
            spawnAoE(CombatAoEEffect(hero.x,hero.y,115f,Color(0xFF38BDF8)))
            val mult = 1.65f + hero.skillLevel * .14f
            livingEnemies.forEach { enemy ->
              if (hypot(enemy.x-hero.x,enemy.y-hero.y) <= 120f)
                dealDamageToEnemy(enemy,(hero.effectiveAttack*mult).toInt(),true)
            }
          } else {
            val enemy = livingEnemies.firstOrNull { it.id == hero.contactAttack.targetId }
            if (enemy != null && hypot(enemy.x-hero.x,enemy.y-hero.y) <= hero.heroClass.preferredRange+35f)
              dealDamageToEnemy(enemy,hero.effectiveAttack,false)
          }
        }
        hero.attackAnim = hero.contactAttack.progress
        return
      }
    }

    // 1. Priest Emergency Ally Heal Priority (Ally Awareness)
    if (hero.heroClass == HeroClassType.PRIEST && hero.skillCooldown <= 0f) {
      val woundedAlly = livingAllies.minByOrNull { it.hp.toFloat() / it.maxHp.toFloat() }
      if (woundedAlly != null && woundedAlly.hp < woundedAlly.maxHp * 0.80f) {
        hero.aiIntentLabelVi = "Cứu thương khẩn"
        hero.skillCooldown = hero.maxSkillCooldown
        hero.attackAnim = 1f
        val healBase = (48 + hero.attack * 0.85f + hero.skillLevel * 14).toInt()
        livingAllies.forEach { ally ->
          if (hypot(ally.x - woundedAlly.x, ally.y - woundedAlly.y) <= 240f) {
            ally.hp = (ally.hp + healBase).coerceAtMost(ally.maxHp)
            ally.shieldHp = (ally.shieldHp + 25).coerceAtMost(90)
            spawnText(FloatingCombatText(ally.x, ally.y - 42f, "+$healBase HP", Color(0xFF34D399), true))
          }
        }
        spawnAoE(CombatAoEEffect(woundedAlly.x, woundedAlly.y, 135f, Color(0xFF34D399)))
        spawnText(FloatingCombatText(hero.x, hero.y - 54f, hero.heroClass.skillNameVi, Color(0xFFFef08A), true))
        return
      }
    }

    // 2. Handle Strict Non-Combat Movement Orders (RETREAT / REGROUP)
    if (order == TacticalOrder.RETREAT) {
      hero.aiIntentLabelVi = "Rút lui chiến thuật"
      val safeX = (masterX - 110f).coerceAtLeast(mapLayout.wallMargin + 40f)
      val safeY = masterY + formationOffsetY(hero.formationIndex)
      moveTowards(hero, safeX, safeY, hero.effectiveSpeed * 1.24f, dt, mapLayout)
      return
    }

    if (order == TacticalOrder.REGROUP) {
      val formPos = formationSlotAround(masterX, masterY, hero)
      val distToSlot = hypot(formPos.first - hero.x, formPos.second - hero.y)
      if (distToSlot > 52f) {
        hero.aiIntentLabelVi = "Tập hợp quanh Master"
        moveTowards(hero, formPos.first, formPos.second, hero.effectiveSpeed * 1.18f, dt, mapLayout)
        return
      }
    }

    // 3. Move-To Beacon Priority if far from beacon
    if (order == TacticalOrder.MOVE_TO && moveBeacon != null) {
      val slot = formationSlotAround(moveBeacon.first, moveBeacon.second, hero)
      val distBeacon = hypot(slot.first - hero.x, slot.second - hero.y)
      // Escort telegraphs need a precise dodge; the normal loose formation stop
      // radius can leave the frontline inside a clearly avoided danger circle.
      val arrivalRadius = if (protectedAllyPosition != null) 24f else 95f
      if (distBeacon > arrivalRadius) {
        hero.aiIntentLabelVi = "Tiến tới điểm chỉ định"
        moveTowards(hero, slot.first, slot.second, hero.effectiveSpeed * 1.12f, dt, mapLayout)
        return
      }
    }

    // 4. Target Selection & Threat Evaluation
    val target = selectBestTarget(
      hero = hero,
      livingEnemies = livingEnemies,
      livingAllies = livingAllies,
      masterX = masterX,
      masterY = masterY,
      order = order,
      designatedTargetId = designatedTargetId,
      protectedAllyPosition = protectedAllyPosition,
    )
    hero.currentTargetId = target?.id

    if (target == null) {
      // No active threat in engagement envelope -> maintain tactical formation!
      hero.aiIntentLabelVi = when (order) {
        TacticalOrder.HOLD_POSITION -> "Giữ vững vị trí"
        TacticalOrder.MOVE_TO -> "Canh gác điểm cờ"
        TacticalOrder.PROTECT_ALLY -> "Hộ vệ đồng đội"
        TacticalOrder.REGROUP -> "Đội hình quanh Master"
        else -> "Theo đội hình"
      }
      val anchor = when (order) {
        TacticalOrder.HOLD_POSITION -> hero.anchorX to hero.anchorY
        TacticalOrder.MOVE_TO -> {
          val b = moveBeacon ?: (masterX to masterY)
          formationSlotAround(b.first, b.second, hero)
        }
        TacticalOrder.PROTECT_ALLY -> {
          val vip = livingAllies.minByOrNull { it.hp.toFloat() / it.maxHp } ?: hero
          if (protectedAllyPosition != null) formationSlotAround(protectedAllyPosition.first, protectedAllyPosition.second, hero)
          else if (vip.id == hero.id) formationSlotAround(masterX, masterY, hero)
          else formationSlotAround(vip.x, vip.y, hero)
        }
        else -> formationSlotAround(masterX, masterY, hero)
      }
      if (hypot(anchor.first - hero.x, anchor.second - hero.y) > 22f) {
        moveTowards(hero, anchor.first, anchor.second, hero.effectiveSpeed, dt, mapLayout)
      } else {
        hero.isMoving = false
      }
      return
    }

    val dx = target.x - hero.x
    val dy = target.y - hero.y
    val dist = hypot(dx, dy).coerceAtLeast(1f)
    hero.facingRight = dx >= 0f

    val hpRatio = hero.hp.toFloat() / hero.maxHp.toFloat()
    val isRangedHero = hero.heroClass == HeroClassType.ARCHER ||
      hero.heroClass == HeroClassType.MAGE ||
      hero.heroClass == HeroClassType.PRIEST

    // 5. Health Awareness & Kiting Positioning
    if (order == TacticalOrder.HOLD_POSITION) {
      // Hold Position: stay anchored, only step slightly within 45f of anchor, attack if in range
      hero.aiIntentLabelVi = "Giữ chốt phản kích"
      val distFromAnchor = hypot(hero.x - hero.anchorX, hero.y - hero.anchorY)
      if (distFromAnchor > 48f) {
        moveTowards(hero, hero.anchorX, hero.anchorY, hero.effectiveSpeed, dt, mapLayout)
      } else {
        hero.isMoving = false
      }
      if (dist <= hero.heroClass.preferredRange + 35f) {
        executeCombatActions(hero, target, livingEnemies, spawnProjectile, spawnAoE, spawnText, dealDamageToEnemy)
      }
      return
    }

    // Ranged Kiting or Critical-HP Retreat Awareness (unless Reckless Berserker!)
    if (isRangedHero && dist < 96f) {
      // Kite away from melee rusher while firing if off cooldown
      hero.aiIntentLabelVi = "Thả diều tầm xa"
      val retreatX = hero.x - (dx / dist) * 120f
      val retreatY = hero.y - (dy / dist) * 120f
      moveTowards(hero, retreatX, retreatY, hero.effectiveSpeed * 0.95f, dt, mapLayout)
      if (hero.attackCooldown <= 0f || hero.skillCooldown <= 0f) {
        executeCombatActions(hero, target, livingEnemies, spawnProjectile, spawnAoE, spawnText, dealDamageToEnemy)
      }
      return
    }

    if (hpRatio < 0.24f && hero.personality != PersonalityType.RECKLESS && hero.heroClass != HeroClassType.KNIGHT) {
      // Wounded squishy hero pulls back toward Master / Knight protection
      val knightAlly = livingAllies.firstOrNull { it.heroClass == HeroClassType.KNIGHT }
      val safeRefX = knightAlly?.let { it.x - 75f } ?: (masterX - 50f)
      val safeRefY = knightAlly?.y ?: masterY
      if (hypot(safeRefX - hero.x, safeRefY - hero.y) > 45f && dist < 170f) {
        hero.aiIntentLabelVi = "Lùi về sau Khiên"
        moveTowards(hero, safeRefX, safeRefY, hero.effectiveSpeed * 1.05f, dt, mapLayout)
        return
      }
    }

    // 6. Standard Engagement Positioning & Attack / Skill Execution
    if (dist > hero.heroClass.preferredRange) {
      hero.aiIntentLabelVi = when {
        order == TacticalOrder.FOCUS_FIRE -> "Tập trung hỏa lực"
        order == TacticalOrder.PROTECT_ALLY -> "Bảo vệ đồng đội"
        hero.heroClass == HeroClassType.KNIGHT && hypot(target.x - masterX, target.y - masterY) < 190f -> "Đánh chặn hộ giá"
        else -> "Áp sát mục tiêu"
      }
      moveTowards(hero, target.x, target.y, hero.effectiveSpeed, dt, mapLayout)
    } else {
      hero.isMoving = false
      hero.aiIntentLabelVi = when {
        hero.skillCooldown <= 0.1f -> "Tung kỹ năng"
        order == TacticalOrder.FOCUS_FIRE -> "Dồn sát thương"
        order == TacticalOrder.PROTECT_ALLY -> "Chắn đòn hộ vệ"
        else -> "Giao chiến"
      }
      executeCombatActions(hero, target, livingEnemies, spawnProjectile, spawnAoE, spawnText, dealDamageToEnemy)
    }
  }

  private fun selectBestTarget(
    hero: HeroCombatUnit,
    livingEnemies: List<EnemyCombatUnit>,
    livingAllies: List<HeroCombatUnit>,
    masterX: Float,
    masterY: Float,
    order: TacticalOrder,
    designatedTargetId: String?,
    protectedAllyPosition: Pair<Float, Float>?,
  ): EnemyCombatUnit? {
    if (livingEnemies.isEmpty()) return null

    // Focus Fire or Attack Target strictly locks designated target if alive
    if ((order == TacticalOrder.FOCUS_FIRE || order == TacticalOrder.ATTACK_TARGET) && designatedTargetId != null) {
      val marked = livingEnemies.firstOrNull { it.id == designatedTargetId }
      if (marked != null) return marked
    }

    // Protect Ally prioritizes enemy closest to the most vulnerable ally / Master
    if (order == TacticalOrder.PROTECT_ALLY) {
      val vulnerable = livingAllies.minByOrNull { it.hp.toFloat() / it.maxHp }
      val refX = protectedAllyPosition?.first ?: vulnerable?.x ?: masterX
      val refY = protectedAllyPosition?.second ?: vulnerable?.y ?: masterY
      return livingEnemies
        .filter { hypot(it.x - refX, it.y - refY) <= 420f }
        .minByOrNull { hypot(it.x - refX, it.y - refY) }
    }

    val maxScanRange = when (order) {
      TacticalOrder.ATTACK_TARGET, TacticalOrder.FOCUS_FIRE -> 950f
      TacticalOrder.HOLD_POSITION -> hero.heroClass.preferredRange + 85f
      else -> 460f
    }

    val candidates = livingEnemies.filter {
      hypot(it.x - hero.x, it.y - hero.y) <= maxScanRange ||
        hypot(it.x - masterX, it.y - masterY) <= 420f
    }
    if (candidates.isEmpty()) return null

    // Class-Intelligent Threat Scoring
    return candidates.maxByOrNull { enemy ->
      val dist = hypot(enemy.x - hero.x, enemy.y - hero.y)
      val distMaster = hypot(enemy.x - masterX, enemy.y - masterY)
      var score = 1000f - dist * 1.35f

      if (designatedTargetId == enemy.id) score += 450f
      if (distMaster < 190f && hero.heroClass == HeroClassType.KNIGHT) {
        // Knight actively intercepts threats rushing Master or backline!
        score += 360f
      }
      if (enemy.archetype.isRanged && (hero.heroClass == HeroClassType.MAGE || hero.heroClass == HeroClassType.ASSASSIN || hero.heroClass == HeroClassType.ARCHER)) {
        // Backline hunters & Mages prioritize dangerous ranged/caster enemies
        score += 210f
      }
      if (enemy.archetype.isBoss) score += 160f
      if (enemy.hp < enemy.maxHp * 0.30f && hero.heroClass == HeroClassType.ASSASSIN) {
        score += 280f
      }
      score
    }
  }

  private fun executeCombatActions(
    hero: HeroCombatUnit,
    target: EnemyCombatUnit,
    livingEnemies: List<EnemyCombatUnit>,
    spawnProjectile: (CombatProjectile) -> Unit,
    spawnAoE: (CombatAoEEffect) -> Unit,
    spawnText: (FloatingCombatText) -> Unit,
    dealDamageToEnemy: (EnemyCombatUnit, Int, Boolean) -> Unit,
  ) {
    // Priority 1: Signature Class Active Skill
    if (hero.skillCooldown <= 0f) {
      hero.skillCooldown = hero.maxSkillCooldown
      hero.attackCooldown = 0.75f
      hero.attackAnim = 1f
      val skillMult = 1.65f + hero.skillLevel * 0.14f

      spawnText(
        FloatingCombatText(
          x = hero.x,
          y = hero.y - 56f,
          text = "★ ${hero.heroClass.skillNameVi}!",
          color = Color(0xFFF5B841),
          isLarge = true,
        )
      )

      if (hero.premiumSample) {
        hero.contactAttack.start(target.id,skill = true)
        hero.isMoving = false
        return
      }

      when (hero.heroClass) {
        HeroClassType.KNIGHT -> {
          // Shield Shockwave + Self Shield Barrier
          hero.shieldHp = (hero.shieldHp + 65 + hero.skillLevel * 12).coerceAtMost(220)
          spawnAoE(CombatAoEEffect(hero.x, hero.y, 115f, Color(0xFF38BDF8)))
          livingEnemies.filter { hypot(it.x - hero.x, it.y - hero.y) <= 120f }.forEach { e ->
            dealDamageToEnemy(e, (hero.effectiveAttack * skillMult).toInt(), true)
          }
        }

        HeroClassType.ARCHER -> {
          // Piercing 3-Arrow Fan Volley
          val baseAngle = kotlin.math.atan2(target.y - hero.y, target.x - hero.x)
          for (offsetDeg in listOf(-12f, 0f, 12f)) {
            val rad = baseAngle + Math.toRadians(offsetDeg.toDouble()).toFloat()
            val release = bowReleasePoint(hero, target)
            spawnProjectile(
              CombatProjectile(
                id = UUID.randomUUID().toString(),
                fromHero = true,
                x = release.first,
                y = release.second,
                vx = cos(rad) * 440f,
                vy = sin(rad) * 440f,
                damage = (hero.effectiveAttack * (0.95f + hero.skillLevel * 0.08f)).toInt(),
                color = Color(0xFFFBBF24),
                radius = 6.5f,
              )
            )
          }
        }

        HeroClassType.MAGE -> {
          // Astral Meteor AoE Blast at target cluster
          spawnAoE(CombatAoEEffect(target.x, target.y, 130f, Color(0xFFC084FC), duration = 0.68f))
          livingEnemies.filter { hypot(it.x - target.x, it.y - target.y) <= 135f }.forEach { e ->
            dealDamageToEnemy(e, (hero.effectiveAttack * (skillMult + 0.25f)).toInt(), true)
          }
        }

        HeroClassType.ASSASSIN -> {
          // Shadow Blink behind target + Critical Strike
          val behindDir = if (target.facingRight) -1f else 1f
          hero.x = target.x + behindDir * 30f
          hero.y = target.y
          spawnAoE(CombatAoEEffect(target.x, target.y, 65f, Color(0xFFF43F5E)))
          dealDamageToEnemy(target, (hero.effectiveAttack * (skillMult + 0.55f)).toInt(), true)
        }

        HeroClassType.BERSERKER -> {
          // Missing-HP Empowered Whirlwind Cleave
          val missingHpBonus = 1f + (1f - hero.hp.toFloat() / hero.maxHp.toFloat()) * 0.65f
          spawnAoE(CombatAoEEffect(hero.x, hero.y, 110f, Color(0xFFEF4444)))
          livingEnemies.filter { hypot(it.x - hero.x, it.y - hero.y) <= 115f }.forEach { e ->
            dealDamageToEnemy(e, (hero.effectiveAttack * skillMult * missingHpBonus).toInt(), true)
          }
        }

        HeroClassType.PRIEST -> {
          // Offensive Holy Smite if everyone is healthy
          spawnAoE(CombatAoEEffect(target.x, target.y, 85f, Color(0xFFFef08A)))
          dealDamageToEnemy(target, (hero.effectiveAttack * skillMult).toInt(), true)
        }
      }
      return
    }

    // Priority 2: Standard Real-Time Class Basic Attack
    if (hero.attackCooldown <= 0f) {
      val atkInterval = (1.15f - (hero.speed - 90f) * 0.003f).coerceIn(0.55f, 1.25f)
      hero.attackCooldown = atkInterval
      hero.attackAnim = 1f

      if (hero.premiumSample) {
        hero.contactAttack.start(target.id)
        hero.isMoving = false
        return
      }

      when (hero.heroClass) {
        HeroClassType.ARCHER, HeroClassType.MAGE, HeroClassType.PRIEST -> {
          val dx = target.x - hero.x
          val dy = target.y - hero.y
          val d = hypot(dx, dy).coerceAtLeast(1f)
          val projSpeed = if (hero.heroClass == HeroClassType.ARCHER) 420f else 350f
          val projColor = when (hero.heroClass) {
            HeroClassType.ARCHER -> Color(0xFFFDE047)
            HeroClassType.MAGE -> Color(0xFFA855F7)
            else -> Color(0xFF6EE7B7)
          }
          val release = if (hero.heroClass == HeroClassType.ARCHER)
            bowReleasePoint(hero, target) else hero.x to hero.y - 16f
          spawnProjectile(
            CombatProjectile(
              id = UUID.randomUUID().toString(),
              fromHero = true,
              x = release.first,
              y = release.second,
              vx = (dx / d) * projSpeed,
              vy = (dy / d) * projSpeed,
              damage = hero.effectiveAttack,
              color = projColor,
              isAoEOnHit = hero.heroClass == HeroClassType.MAGE,
            )
          )
        }

        else -> {
          dealDamageToEnemy(target, hero.effectiveAttack, false)
        }
      }
    }
  }

  private fun moveTowards(
    hero: HeroCombatUnit,
    destX: Float,
    destY: Float,
    moveSpeed: Float,
    dt: Float,
    mapLayout: GeneratedMapLayout,
  ) {
    val dx = destX - hero.x
    val dy = destY - hero.y
    val dist = hypot(dx, dy)
    if (dist < 4f) {
      hero.isMoving = false
      return
    }
    val stepX = hero.x + (dx / dist) * moveSpeed * dt
    val stepY = hero.y + (dy / dist) * moveSpeed * dt
    val (rx, ry) = CollisionPhysics.resolvePosition(
      targetX = stepX,
      targetY = stepY,
      entityRadius = 18f,
      mapWidth = mapLayout.width,
      mapHeight = mapLayout.height,
      marginWall = mapLayout.wallMargin,
      obstacles = mapLayout.obstacles,
    )
    hero.x = rx
    hero.y = ry
    if (kotlin.math.abs(dx) > 2f) hero.facingRight = dx >= 0f
    hero.isMoving = true
  }

  fun formationSlotAround(
    refX: Float,
    refY: Float,
    hero: HeroCombatUnit,
  ): Pair<Float, Float> {
    val forwardOffset = when (hero.heroClass) {
      HeroClassType.KNIGHT, HeroClassType.BERSERKER -> 72f
      HeroClassType.ASSASSIN -> 40f
      HeroClassType.ARCHER, HeroClassType.MAGE, HeroClassType.PRIEST -> -58f
    }
    val lateralOffset = formationOffsetY(hero.formationIndex)
    return (refX + forwardOffset) to (refY + lateralOffset)
  }

  private fun formationOffsetY(index: Int): Float = when (index % 4) {
    0 -> -48f
    1 -> 48f
    2 -> -92f
    else -> 92f
  }

  /**
   * World-space point where an archer's arrow leaves the bow.
   *
   * The old spawn was the hero's own centre (`x`, `y - 16`), so arrows visibly left her torso
   * at knee height. The authored aim frame puts the bow hand ~57 frame px above the ground
   * row (1 frame px = 1 / 1.08 world px), and the battle renderer already lifts projectiles
   * by 28 world px, so a 12 px step forward along the aim line at this height lands the
   * arrow on the bow itself.
   *
   * This moves the visual origin only: damage, cadence and hit resolution are untouched
   * (the projectile still resolves on the same proximity check).
   */
  private fun bowReleasePoint(
    hero: HeroCombatUnit,
    target: EnemyCombatUnit,
  ): Pair<Float, Float> {
    val dx = target.x - hero.x
    val dy = target.y - hero.y
    val d = hypot(dx, dy).coerceAtLeast(1f)
    val step = 12f / d
    return (hero.x + dx * step) to (hero.y - 24f + dy * step * OBLIQUE_STEP_VERTICAL)
  }

  private const val OBLIQUE_STEP_VERTICAL = 0.36f
}
