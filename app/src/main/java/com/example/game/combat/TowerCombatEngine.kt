package com.example.game.combat

import androidx.compose.ui.graphics.Color
import com.example.data.model.EquipmentRecord
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.SanctuaryMealRecipe
import com.example.data.model.TacticalOrder
import com.example.game.ai.AutonomousHeroAi
import com.example.game.map.CollisionPhysics
import com.example.game.map.EnemyArchetype
import com.example.game.map.TowerFloorSpec
import com.example.game.tower.GeneratedMapLayout
import com.example.game.tower.TowerFloorCatalog
import java.util.UUID
import kotlin.math.hypot
import kotlin.random.Random

enum class MissionStage {
  INTRO_DIALOGUE,
  IN_PROGRESS,
  EVENT_CUTSCENE_BANNER,
  VICTORY,
  DEFEAT,
}

class TowerCombatEngine(
  val floorSpec: TowerFloorSpec,
  squadRecords: List<HeroRecord>,
  equipment: List<EquipmentRecord>,
  val feastBuffActive: Boolean = false,
  val activeMealRecipe: SanctuaryMealRecipe? = null,
  enableLostCourier: Boolean = false,
) {
  val mapLayout: GeneratedMapLayout = TowerFloorCatalog.buildTowerFloorMap(floorSpec)

  private val hpBonusMult: Float = when {
    activeMealRecipe != null -> 1f + activeMealRecipe.hpBuffPct / 100f
    feastBuffActive -> 1.10f
    else -> 1.0f
  }
  private val atkBonusMult: Float = when {
    activeMealRecipe != null -> 1f + activeMealRecipe.atkBuffPct / 100f
    feastBuffActive -> 1.12f
    else -> 1.0f
  }
  private val defBonusFlat: Int = if (activeMealRecipe == SanctuaryMealRecipe.ROYAL_ASTRAL_BANQUET) 4 else 0

  var masterX: Float = floorSpec.spawnX
  var masterY: Float = floorSpec.spawnY
  var masterMaxHp: Int = (550 * hpBonusMult).toInt()
  var masterHp: Int = masterMaxHp
  var masterFacingRight: Boolean = true
  var masterIsMoving: Boolean = false
  private var masterBoltCooldown: Float = 0f

  var activeOrder: TacticalOrder = TacticalOrder.FOLLOW
    private set

  var designatedTargetId: String? = null
    private set

  var selectedHeroId: String? = null
    private set

  var moveBeacon: Pair<Float, Float>? = null
    private set

  var awaitingMoveToTap: Boolean = false
    private set

  var retreatCompleted: Boolean = false
    private set

  var commandFeedback: String? = null
    private set
  private var commandFeedbackTimer: Float = 0f

  var bonusDiscoveryGold: Long = 0L
    private set
  var bonusDiscoveryMana: Long = 0L
    private set

  var stage: MissionStage = MissionStage.INTRO_DIALOGUE
  var eventWaveTriggered: Boolean = false
    private set
  var eventBannerTimer: Float = 0f

  val heroes: List<HeroCombatUnit> = squadRecords.mapIndexed { idx, record ->
    val baseMaxHp = record.totalMaxHp(equipment)
    val maxHp = (baseMaxHp * hpBonusMult).toInt()
    val moraleMult = if (record.morale >= 90L) 1.08f else 1.0f
    val effectiveAtk = (record.totalAttack(equipment) * moraleMult * atkBonusMult).toInt()
    val startX = floorSpec.spawnX + if (record.classType.preferredRange < 80f) 65f else -42f
    val startY = floorSpec.spawnY + (idx - 1) * 58f
    HeroCombatUnit(
      id = record.id,
      name = record.name,
      title = record.title,
      heroClass = record.classType,
      personality = record.personalityType,
      maxHp = maxHp,
      hp = maxHp,
      attack = effectiveAtk,
      defense = record.totalDefense(equipment) + defBonusFlat,
      speed = record.totalSpeed(equipment).toFloat().coerceIn(75f, 185f),
      skillLevel = record.skillLevel.toInt(),
      x = startX,
      y = startY,
      anchorX = startX,
      anchorY = startY,
      formationIndex = idx,
    )
  }

  val enemies: MutableList<EnemyCombatUnit> = mutableListOf()
  val discoveryNodes: MutableList<BattleDiscoveryNode> = mutableListOf()
  val projectiles: MutableList<CombatProjectile> = mutableListOf()
  val aoeEffects: MutableList<CombatAoEEffect> = mutableListOf()
  val floatingTexts: MutableList<FloatingCombatText> = mutableListOf()
  val lostCourier: LostCourierEncounter? = if (enableLostCourier && floorSpec.floorNumber == 1) {
    LostCourierEncounter(this, squadRecords)
  } else null

  init {
    selectedHeroId = heroes.firstOrNull()?.id
    if (lostCourier != null) {
      lostCourier.initialize()
      activeOrder = TacticalOrder.PROTECT_ALLY
    } else {
      floorSpec.enemies.filter { !it.spawnOnEventWave }.forEachIndexed { idx, spawn ->
        enemies += createEnemyUnit("enemy_init_$idx", spawn.archetype, spawn.x, spawn.y, floorSpec.floorNumber)
      }
    }

    mapLayout.obstacles.forEach { obs ->
      when (obs.id) {
        "f1_discovery_chest_north" -> discoveryNodes += BattleDiscoveryNode(
          id = "f1_chest_north",
          obstacleId = obs.id,
          titleVi = "Rương Cổ Ngữ Bờ Bắc",
          x = obs.x,
          y = obs.y,
          kind = DiscoveryNodeKind.TREASURE_CHEST,
          goldBonus = 65L,
          manaBonus = 15L,
          healPct = 0f,
        )
        "f1_discovery_shrine_south" -> discoveryNodes += BattleDiscoveryNode(
          id = "f1_shrine_south",
          obstacleId = obs.id,
          titleVi = "Đài Thảo Dược Bờ Nam",
          x = obs.x,
          y = obs.y,
          kind = DiscoveryNodeKind.HEALING_SHRINE,
          goldBonus = 0L,
          manaBonus = 20L,
          healPct = 0.35f,
        )
      }
    }

    if (feastBuffActive || activeMealRecipe != null) {
      val mealTitle = activeMealRecipe?.titleVi ?: "Đại Tiệc Thánh Địa"
      floatingTexts += FloatingCombatText(
        x = masterX,
        y = masterY - 68f,
        text = "★ BUFF: $mealTitle!",
        color = Color(0xFFF5B841),
        isLarge = true,
        duration = 2.6f,
      )
    }
  }

  internal fun postFeedback(message: String) {
    commandFeedback = message
    commandFeedbackTimer = 3.2f
  }

  private fun createEnemyUnit(
    id: String,
    arch: EnemyArchetype,
    x: Float,
    y: Float,
    floor: Int,
  ): EnemyCombatUnit {
    val scale = 1f + (floor - 1) * 0.14f
    val hp = (arch.baseHp * scale).toInt()
    return EnemyCombatUnit(
      id = id,
      archetype = arch,
      maxHp = hp,
      hp = hp,
      attack = (arch.baseAtk * scale).toInt(),
      defense = (arch.baseDef * scale).toInt(),
      speed = arch.speed,
      x = x,
      y = y,
    )
  }

  fun isDiscoveryClaimed(obstacleId: String): Boolean =
    discoveryNodes.any { it.obstacleId == obstacleId && it.discovered }

  fun discoveredNodeIds(): Set<String> =
    discoveryNodes.filter { it.discovered }.map { it.id }.toSet()

  private fun claimDiscovery(node: BattleDiscoveryNode) {
    if (node.discovered) return
    node.discovered = true
    bonusDiscoveryGold += node.goldBonus
    bonusDiscoveryMana += node.manaBonus

    if (node.healPct > 0f) {
      heroes.filter { !it.knockedOut }.forEach { hero ->
        val healAmt = (hero.maxHp * node.healPct).toInt().coerceAtLeast(25)
        hero.hp = (hero.hp + healAmt).coerceAtMost(hero.maxHp)
      }
      masterHp = (masterHp + (masterMaxHp * node.healPct).toInt()).coerceAtMost(masterMaxHp)
      aoeEffects += CombatAoEEffect(node.x, node.y, 92f, Color(0xFF34D399), duration = 0.75f)
      floatingTexts += FloatingCombatText(
        x = node.x,
        y = node.y - 44f,
        text = "✦ ${node.titleVi}: HỒI 35% HP & +${node.manaBonus} MANA!",
        color = Color(0xFF34D399),
        isLarge = true,
        duration = 2.0f,
      )
      postFeedback("Khám phá [${node.titleVi}]: Hồi 35% HP toàn đội & +${node.manaBonus} Mana!")
    } else {
      aoeEffects += CombatAoEEffect(node.x, node.y, 82f, Color(0xFFF5B841), duration = 0.70f)
      floatingTexts += FloatingCombatText(
        x = node.x,
        y = node.y - 44f,
        text = "✦ ${node.titleVi}: +${node.goldBonus} VÀNG • +${node.manaBonus} MANA!",
        color = Color(0xFFF5B841),
        isLarge = true,
        duration = 2.0f,
      )
      postFeedback("Đã mở [${node.titleVi}]: +${node.goldBonus} Vàng & +${node.manaBonus} Mana!")
    }
  }

  fun confirmTacticalRetreat() {
    if (stage == MissionStage.VICTORY || stage == MissionStage.DEFEAT) return
    retreatCompleted = true
    lostCourier?.fail("Đã rút lui an toàn. Nhiệm vụ người đưa thư có thể thử lại.")
    activeOrder = TacticalOrder.RETREAT
    stage = MissionStage.DEFEAT
    postFeedback("Đã rút lui chiến thuật an toàn về Thánh Địa!")
  }

  internal fun eventWaveTriggeredForCourier() { eventWaveTriggered = true }

  fun chooseCourierOrder(order: CourierOrder) {
    val mission = lostCourier ?: return
    if (mission.outcome != null) return
    if (order == CourierOrder.PROTECT) {
      mission.order = order
      issueTacticalOrder(TacticalOrder.PROTECT_ALLY)
      postFeedback("BẢO VỆ: bọc lót người đưa thư, chặn lính gác; giảm 40% sát thương khi đứng gần. Trinh sát có thể gọi viện!")
    } else {
      val scout = enemies.firstOrNull { it.id == mission.scoutEnemyId && !it.dead }
      if (scout == null) {
        postFeedback("Chưa có trinh sát đang gọi viện. Bảo vệ người đưa thư trước!")
        return
      }
      mission.order = order
      designatedTargetId = scout.id
      issueTacticalOrder(TacticalOrder.ATTACK_TARGET)
      postFeedback("CHẶN TRINH SÁT: toàn đội truy đuổi; người đưa thư mất đội hình che chắn!")
    }
  }

  fun issueTacticalOrder(order: TacticalOrder) {
    activeOrder = order
    when (order) {
      TacticalOrder.HOLD_POSITION -> {
        awaitingMoveToTap = false
        heroes.forEach {
          it.anchorX = it.x
          it.anchorY = it.y
        }
        postFeedback("Lệnh Chỉ Huy: Giữ vững đội hình tại chỗ!")
        floatingTexts += FloatingCombatText(masterX, masterY - 58f, "LỆNH: GIỮ VỊ TRÍ!", Color(0xFF38BDF8), true)
      }

      TacticalOrder.FOCUS_FIRE, TacticalOrder.ATTACK_TARGET -> {
        awaitingMoveToTap = false
        val livingEnemies = enemies.filter { !it.dead }
        if (livingEnemies.isEmpty()) {
          postFeedback("Hiện không còn mục tiêu địch trong khu vực!")
          return
        }
        if (designatedTargetId == null || livingEnemies.none { it.id == designatedTargetId }) {
          designatedTargetId = livingEnemies.minByOrNull { hypot(it.x - masterX, it.y - masterY) }?.id
        }
        val targetName = livingEnemies.firstOrNull { it.id == designatedTargetId }?.archetype?.nameVi ?: "Mục tiêu"
        val label = if (order == TacticalOrder.FOCUS_FIRE) "LỆNH: DỒN HỎA LỰC!" else "LỆNH: TẤN CÔNG!"
        postFeedback("$label Nhắm vào [$targetName]!")
        floatingTexts += FloatingCombatText(masterX, masterY - 58f, label, Color(0xFFEF4444), true)
      }

      TacticalOrder.MOVE_TO -> {
        awaitingMoveToTap = true
        postFeedback("Chạm lên mặt đất chiến trường để cắm cờ điều quân!")
        floatingTexts += FloatingCombatText(masterX, masterY - 58f, "CHẠM BẢN ĐỒ ĐỂ CHỌN VỊ TRÍ!", Color(0xFFF5B841), true)
      }

      TacticalOrder.RETREAT -> {
        awaitingMoveToTap = false
        postFeedback("Lệnh Chỉ Huy: Toàn đội lùi về điểm xuất phát an toàn!")
        floatingTexts += FloatingCombatText(masterX, masterY - 58f, "LỆNH: RÚT LUI CHIẾN THUẬT!", Color(0xFFF59E0B), true)
      }

      TacticalOrder.REGROUP -> {
        awaitingMoveToTap = false
        postFeedback(if (lostCourier != null) "Lệnh Chỉ Huy: Tập hợp đội hình quanh người đưa thư!"
          else "Lệnh Chỉ Huy: Tập hợp đội hình quanh Ấn Lệnh Master!")
        floatingTexts += FloatingCombatText(masterX, masterY - 58f, "LỆNH: TẬP HỢP ĐỘI HÌNH!", Color(0xFFA855F7), true)
      }

      TacticalOrder.PROTECT_ALLY -> {
        awaitingMoveToTap = false
        postFeedback("Lệnh Chỉ Huy: Ưu tiên bọc lót đồng đội suy yếu!")
        floatingTexts += FloatingCombatText(masterX, masterY - 58f, "LỆNH: BẢO VỆ ĐỒNG ĐỘI!", Color(0xFF34D399), true)
      }

      TacticalOrder.FOLLOW -> {
        awaitingMoveToTap = false
        moveBeacon = null
        postFeedback("Lệnh Chỉ Huy: Tiến quân theo Ấn Lệnh Master!")
        floatingTexts += FloatingCombatText(masterX, masterY - 58f, "LỆNH: THEO ĐỘI HÌNH!", Color(0xFFE2E8F0), true)
      }
    }
  }

  fun onBattlefieldTapped(worldX: Float, worldY: Float) {
    // 1. If explicitly awaiting a Move-To tap, always place the Move-To tactical beacon!
    if (awaitingMoveToTap) {
      val clamped = CollisionPhysics.resolvePosition(
        targetX = worldX,
        targetY = worldY,
        entityRadius = 20f,
        mapWidth = mapLayout.width,
        mapHeight = mapLayout.height,
        marginWall = mapLayout.wallMargin,
        obstacles = mapLayout.obstacles,
      )
      moveBeacon = clamped
      activeOrder = TacticalOrder.MOVE_TO
      awaitingMoveToTap = false
      aoeEffects += CombatAoEEffect(clamped.first, clamped.second, 55f, Color(0xFFF5B841))
      postFeedback("Đã cắm cờ điều quân tới vị trí mới!")
      return
    }

    // 2. Check if player tapped an unclaimed Floor discovery object (Chest / Herb Shrine)
    val tappedDiscovery = discoveryNodes.firstOrNull {
      !it.discovered && hypot(it.x - worldX, it.y - worldY) <= 74f
    }
    if (tappedDiscovery != null) {
      val squadClose = hypot(masterX - tappedDiscovery.x, masterY - tappedDiscovery.y) <= 260f ||
        heroes.any { !it.knockedOut && hypot(it.x - tappedDiscovery.x, it.y - tappedDiscovery.y) <= 260f }
      if (squadClose) {
        claimDiscovery(tappedDiscovery)
      } else {
        moveBeacon = tappedDiscovery.x to tappedDiscovery.y
        activeOrder = TacticalOrder.MOVE_TO
        aoeEffects += CombatAoEEffect(tappedDiscovery.x, tappedDiscovery.y, 58f, Color(0xFF38BDF8))
        postFeedback("Đội hình đang tiến tới khám phá [${tappedDiscovery.titleVi}]!")
      }
      return
    }

    // 3. Check if player tapped on a living enemy to select/focus target!
    val tappedEnemy = enemies
      .filter { !it.dead && hypot(it.x - worldX, it.y - worldY) <= 58f * it.archetype.scale }
      .minByOrNull { hypot(it.x - worldX, it.y - worldY) }

    if (tappedEnemy != null) {
      designatedTargetId = tappedEnemy.id
      if (activeOrder != TacticalOrder.FOCUS_FIRE) {
        activeOrder = TacticalOrder.ATTACK_TARGET
      }
      postFeedback("Khóa mục tiêu: ${tappedEnemy.archetype.nameVi}!")
      floatingTexts += FloatingCombatText(
        tappedEnemy.x,
        tappedEnemy.y - 52f,
        "MỤC TIÊU: ${tappedEnemy.archetype.nameVi}",
        Color(0xFFEF4444),
        true,
      )
      return
    }

    // 4. Check if player tapped on an allied Hero to inspect live AI intent
    val tappedHero = heroes
      .filter { !it.knockedOut && hypot(it.x - worldX, it.y - worldY) <= 48f }
      .minByOrNull { hypot(it.x - worldX, it.y - worldY) }
    if (tappedHero != null) {
      selectedHeroId = tappedHero.id
      postFeedback("${tappedHero.name}: ${tappedHero.aiIntentLabelVi} (HP ${tappedHero.hp}/${tappedHero.maxHp})")
      return
    }

    // 5. Otherwise update Move-To tactical beacon if currently in MOVE_TO mode
    if (activeOrder == TacticalOrder.MOVE_TO) {
      val clamped = CollisionPhysics.resolvePosition(
        targetX = worldX,
        targetY = worldY,
        entityRadius = 20f,
        mapWidth = mapLayout.width,
        mapHeight = mapLayout.height,
        marginWall = mapLayout.wallMargin,
        obstacles = mapLayout.obstacles,
      )
      moveBeacon = clamped
      aoeEffects += CombatAoEEffect(clamped.first, clamped.second, 55f, Color(0xFFF5B841))
      postFeedback("Đã cập nhật tọa độ cờ điều quân!")
    }
  }

  fun triggerHeroManualSkill(heroId: String) {
    val hero = heroes.firstOrNull { it.id == heroId } ?: return
    selectedHeroId = hero.id
    if (hero.knockedOut) {
      postFeedback("${hero.name} đã trọng thương, không thể kích hoạt kỹ năng!")
      return
    }
    if (hero.manualCommandCooldown > 0.1f) {
      postFeedback("Lệnh kỹ năng của ${hero.name} đang hồi (${hero.manualCommandCooldown.toInt() + 1}s)!")
      return
    }
    hero.skillCooldown = 0f
    hero.manualCommandCooldown = 8f
    postFeedback("Lệnh Master: ${hero.name} kích hoạt [${hero.heroClass.skillNameVi}]!")
    floatingTexts += FloatingCombatText(
      x = hero.x,
      y = hero.y - 46f,
      text = "⚡ ${hero.heroClass.skillNameVi.uppercase()}!",
      color = Color(0xFFF5B841),
      isLarge = true,
    )
  }

  fun step(dt: Float, inputDx: Float, inputDy: Float) {
    if (stage == MissionStage.INTRO_DIALOGUE || stage == MissionStage.VICTORY || stage == MissionStage.DEFEAT) {
      return
    }
    lostCourier?.beforeStep(dt)

    if (commandFeedbackTimer > 0f) {
      commandFeedbackTimer = (commandFeedbackTimer - dt).coerceAtLeast(0f)
      if (commandFeedbackTimer <= 0f) commandFeedback = null
    }

    heroes.forEach { h ->
      if (h.manualCommandCooldown > 0f) {
        h.manualCommandCooldown = (h.manualCommandCooldown - dt).coerceAtLeast(0f)
      }
    }

    if (eventBannerTimer > 0f) {
      eventBannerTimer = (eventBannerTimer - dt).coerceAtLeast(0f)
      if (eventBannerTimer <= 0f && stage == MissionStage.EVENT_CUTSCENE_BANNER) {
        stage = MissionStage.IN_PROGRESS
      }
    }

    // 1. Move External Master Tactical Command Sigil
    val mag = hypot(inputDx, inputDy)
    if (mag > 0.05f) {
      val nx = inputDx / mag.coerceAtLeast(1f)
      val ny = inputDy / mag.coerceAtLeast(1f)
      val masterSpeed = 205f
      val (rx, ry) = CollisionPhysics.resolvePosition(
        targetX = masterX + nx * masterSpeed * dt,
        targetY = masterY + ny * masterSpeed * dt,
        entityRadius = 20f,
        mapWidth = mapLayout.width,
        mapHeight = mapLayout.height,
        marginWall = mapLayout.wallMargin,
        obstacles = mapLayout.obstacles,
      )
      masterX = rx
      masterY = ry
      if (kotlin.math.abs(nx) > 0.08f) masterFacingRight = nx > 0f
      masterIsMoving = true
    } else {
      masterIsMoving = false
    }

    // Check proximity to optional Floor Discovery Nodes
    discoveryNodes.forEach { node ->
      if (!node.discovered) {
        val nearMaster = hypot(masterX - node.x, masterY - node.y) <= 95f
        val nearHero = heroes.any { !it.knockedOut && hypot(it.x - node.x, it.y - node.y) <= 95f }
        if (nearMaster || nearHero) {
          claimDiscovery(node)
        }
      }
    }

    // Master tactical guiding arcane bolt support from command sigil
    masterBoltCooldown = (masterBoltCooldown - dt).coerceAtLeast(0f)
    if (masterBoltCooldown <= 0f && lostCourier == null) {
      val nearThreat = enemies.filter { !it.dead && hypot(it.x - masterX, it.y - masterY) <= 270f }
        .minByOrNull { hypot(it.x - masterX, it.y - masterY) }
      if (nearThreat != null) {
        masterBoltCooldown = 1.6f
        val dx = nearThreat.x - masterX
        val dy = nearThreat.y - masterY
        val d = hypot(dx, dy).coerceAtLeast(1f)
        masterFacingRight = dx >= 0f
        projectiles += CombatProjectile(
          id = UUID.randomUUID().toString(),
          fromHero = true,
          x = masterX,
          y = masterY - 18f,
          vx = (dx / d) * 380f,
          vy = (dy / d) * 380f,
          damage = 24 + floorSpec.floorNumber * 6,
          color = Color(0xFF38BDF8),
        )
      }
    }

    // 2. Check Mid-Mission Manhwa Event Trigger (Boss & Ambush Wave!)
    val leadX = maxOf(masterX, heroes.filter { !it.knockedOut }.maxOfOrNull { it.x } ?: masterX)
    val initialCleared = enemies.all { it.dead }
    if (lostCourier == null && !eventWaveTriggered && (leadX >= floorSpec.eventTriggerX || initialCleared)) {
      eventWaveTriggered = true
      floorSpec.enemies.filter { it.spawnOnEventWave }.forEachIndexed { idx, sp ->
        val bossUnit = createEnemyUnit("enemy_event_$idx", sp.archetype, sp.x, sp.y, floorSpec.floorNumber)
        bossUnit.aggroed = true
        enemies += bossUnit
        aoeEffects += CombatAoEEffect(sp.x, sp.y, 95f, Color(0xFFEF4444), duration = 0.8f)
        if (sp.archetype.isBoss) {
          designatedTargetId = bossUnit.id
        }
      }
      stage = MissionStage.EVENT_CUTSCENE_BANNER
      eventBannerTimer = 3.8f
    }

    // Retreat always uses the extraction point, even when the command sigil moved.
    // During an escort, regroup around the actual protected party rather than a sigil left behind.
    val commandX = when (activeOrder) {
      TacticalOrder.RETREAT -> floorSpec.spawnX
      TacticalOrder.REGROUP -> lostCourier?.courier?.x ?: masterX
      else -> masterX
    }
    val commandY = when (activeOrder) {
      TacticalOrder.RETREAT -> floorSpec.spawnY
      TacticalOrder.REGROUP -> lostCourier?.courier?.y ?: masterY
      else -> masterY
    }
    // 3. Step Autonomous Squad Heroes
    heroes.forEach { hero ->
      AutonomousHeroAi.updateHeroUnit(
        hero = hero,
        dt = dt,
        allHeroes = heroes,
        enemies = enemies,
        masterX = commandX,
        masterY = commandY,
        order = activeOrder,
        designatedTargetId = designatedTargetId,
        moveBeacon = moveBeacon,
        mapLayout = mapLayout,
        spawnProjectile = { projectiles += it },
        spawnAoE = { aoeEffects += it },
        spawnText = { floatingTexts += it },
        dealDamageToEnemy = { enemy, rawDmg, isSkill ->
          applyDamageToEnemy(enemy, rawDmg, isSkill)
        },
        protectedAllyPosition = lostCourier?.courier?.let { it.x to it.y },
      )
      if (hero.fearSecondsRemaining > 0f) hero.aiIntentLabelVi = "Lo sợ • -20% sát thương / -15% tốc độ"
    }

    // 4. Step Differentiated Enemy AI & Defeat Collapse Animation
    // A scout call can append reinforcements. Visit only the roster present at tick start.
    val enemyCountAtTickStart = enemies.size
    for (enemyIndex in 0 until enemyCountAtTickStart) {
      val enemy = enemies[enemyIndex]
      if (!enemy.dead) {
        if (lostCourier?.handles(enemy) == true) lostCourier.updateEnemy(enemy, dt) else updateEnemyAi(enemy, dt)
      } else if (enemy.deathAnimProgress < 1f) {
        enemy.deathAnimProgress = (enemy.deathAnimProgress + dt * 2.2f).coerceAtMost(1f)
      }
    }

    // 5. Step Projectiles
    val projIter = projectiles.iterator()
    while (projIter.hasNext()) {
      val proj = projIter.next()
      proj.x += proj.vx * dt
      proj.y += proj.vy * dt
      proj.remainingLife -= dt
      if (proj.remainingLife <= 0f) {
        projIter.remove()
        continue
      }

      if (proj.fromHero) {
        val hitEnemy = enemies.firstOrNull { !it.dead && hypot(it.x - proj.x, it.y - proj.y) <= 28f * it.archetype.scale }
        if (hitEnemy != null) {
          if (proj.isAoEOnHit) {
            aoeEffects += CombatAoEEffect(hitEnemy.x, hitEnemy.y, 70f, proj.color)
            enemies.filter { !it.dead && hypot(it.x - hitEnemy.x, it.y - hitEnemy.y) <= 72f }.forEach { e ->
              applyDamageToEnemy(e, (proj.damage * 0.85f).toInt(), false)
            }
          } else {
            applyDamageToEnemy(hitEnemy, proj.damage, false)
          }
          projIter.remove()
        }
      } else {
        // Enemy projectile hitting Hero or Command Link
        val hitHero = heroes.firstOrNull { !it.knockedOut && hypot(it.x - proj.x, it.y - proj.y) <= 24f }
        if (hitHero != null) {
          applyDamageToHero(hitHero, proj.damage)
          projIter.remove()
        } else if (hypot(masterX - proj.x, masterY - proj.y) <= 24f) {
          applyDamageToMaster(proj.damage)
          projIter.remove()
        }
      }
    }

    // 6. Step VFX & Floating Texts
    val aoeIter = aoeEffects.iterator()
    while (aoeIter.hasNext()) {
      val fx = aoeIter.next()
      fx.elapsed += dt
      if (fx.elapsed >= fx.duration) aoeIter.remove()
    }

    val txtIter = floatingTexts.iterator()
    while (txtIter.hasNext()) {
      val ft = txtIter.next()
      ft.elapsed += dt
      ft.y -= 26f * dt
      if (ft.elapsed >= ft.duration) txtIter.remove()
    }

    // 7. Check Mission Victory / Tactical Retreat / Defeat Conditions
    val livingHeroes = heroes.filter { !it.knockedOut }
    lostCourier?.afterStep()
    if (lostCourier != null) {
      if (activeOrder == TacticalOrder.RETREAT && livingHeroes.isNotEmpty() &&
        livingHeroes.all { hypot(it.x - floorSpec.spawnX, it.y - floorSpec.spawnY) <= 185f }) confirmTacticalRetreat()
      return
    }
    if (eventWaveTriggered && enemies.all { it.dead }) {
      stage = MissionStage.VICTORY
    } else if (masterHp <= 0 || livingHeroes.isEmpty()) {
      retreatCompleted = false
      stage = MissionStage.DEFEAT
    } else if (
      activeOrder == TacticalOrder.RETREAT &&
      enemies.any { it.aggroed || it.dead } &&
      livingHeroes.all { hypot(it.x - floorSpec.spawnX, it.y - floorSpec.spawnY) <= 185f }
    ) {
      confirmTacticalRetreat()
    }
  }

  private fun updateEnemyAi(enemy: EnemyCombatUnit, dt: Float) {
    enemy.attackCooldown = (enemy.attackCooldown - dt).coerceAtLeast(0f)
    enemy.attackAnim = (enemy.attackAnim - dt * 2.6f).coerceAtLeast(0f)
    enemy.hurtFlash = (enemy.hurtFlash - dt * 3.5f).coerceAtLeast(0f)
    enemy.warcryCooldown = (enemy.warcryCooldown - dt).coerceAtLeast(0f)
    enemy.atkBuffTimer = (enemy.atkBuffTimer - dt).coerceAtLeast(0f)

    if (enemy.archetype == EnemyArchetype.FIELD_GOBLIN_RAIDER && enemy.contactAttack.active) {
      enemy.isMoving = false
      if (enemy.contactAttack.advance(dt)) {
        val targetId = enemy.contactAttack.targetId
        if (targetId == null) {
          if (hypot(masterX-enemy.x,masterY-enemy.y) <= enemy.archetype.attackRange+12f)
            applyDamageToMaster(enemy.pendingAttackDamage)
        } else {
          val hero = heroes.firstOrNull { it.id == targetId && !it.knockedOut }
          if (hero != null && hypot(hero.x-enemy.x,hero.y-enemy.y) <= enemy.archetype.attackRange+12f)
            applyDamageToHero(hero,enemy.pendingAttackDamage)
        }
      }
      enemy.attackAnim = enemy.contactAttack.progress
      return
    }

    data class PotentialTarget(val x: Float, val y: Float, val hero: HeroCombatUnit?)

    val livingHeroes = heroes.filter { !it.knockedOut }
    val potentials = buildList {
      livingHeroes.forEach { add(PotentialTarget(it.x, it.y, it)) }
      add(PotentialTarget(masterX, masterY, null))
    }

    // Differentiated Archetype Target Priority:
    // - DIRE_WOLF: fast flanker prioritizing ranged/healer backline or weakened heroes
    // - Raiders / Bosses: drawn to frontline Knights first
    val nearest = potentials.minByOrNull { p ->
      val d = hypot(p.x - enemy.x, p.y - enemy.y)
      val h = p.hero
      when {
        enemy.archetype == EnemyArchetype.DIRE_WOLF && h != null -> {
          val isBackline = h.heroClass == HeroClassType.ARCHER ||
            h.heroClass == HeroClassType.MAGE ||
            h.heroClass == HeroClassType.PRIEST
          val lowHpFactor = if (h.hp < h.maxHp * 0.55f) 0.78f else 1.0f
          (if (isBackline) d * 0.62f else d * 1.15f) * lowHpFactor
        }
        h?.heroClass == HeroClassType.KNIGHT -> d * 0.72f
        else -> d
      }
    } ?: return

    val dx = nearest.x - enemy.x
    val dy = nearest.y - enemy.y
    val dist = hypot(dx, dy).coerceAtLeast(1f)

    if (!enemy.aggroed && dist <= enemy.archetype.aggroRange) {
      enemy.aggroed = true
    }
    if (!enemy.aggroed) {
      enemy.isMoving = false
      return
    }

    // Boss Warcry Ability: Kraag rallies nearby raiders/wolves with an AoE battle roar!
    if (enemy.archetype.isBoss && enemy.warcryCooldown <= 0f) {
      enemy.warcryCooldown = 9.5f
      enemies.filter { !it.dead && hypot(it.x - enemy.x, it.y - enemy.y) <= 360f }.forEach { ally ->
        ally.atkBuffTimer = 5.0f
        ally.aggroed = true
      }
      aoeEffects += CombatAoEEffect(enemy.x, enemy.y, 130f, Color(0xFFEF4444), duration = 0.70f)
      floatingTexts += FloatingCombatText(
        x = enemy.x,
        y = enemy.y - 58f,
        text = "★ HÚ CHIẾN TRẬN: CUỒNG NỘ!",
        color = Color(0xFFFDA4AF),
        isLarge = true,
        duration = 1.6f,
      )
    }

    val effectiveSpeed = if (enemy.atkBuffTimer > 0f) enemy.speed * 1.18f else enemy.speed
    val effectiveAttack = if (enemy.atkBuffTimer > 0f) (enemy.attack * 1.25f).toInt() else enemy.attack

    enemy.facingRight = dx >= 0f

    // Ranged Kiter Behavior: Goblin Archers & Forest Wraiths retreat when melee heroes close in!
    if (enemy.archetype.isRanged && dist < 105f) {
      val (rx, ry) = CollisionPhysics.resolvePosition(
        targetX = enemy.x - (dx / dist) * effectiveSpeed * 0.88f * dt,
        targetY = enemy.y - (dy / dist) * effectiveSpeed * 0.88f * dt,
        entityRadius = 18f * enemy.archetype.scale,
        mapWidth = mapLayout.width,
        mapHeight = mapLayout.height,
        marginWall = mapLayout.wallMargin,
        obstacles = mapLayout.obstacles,
      )
      enemy.x = rx
      enemy.y = ry
      enemy.isMoving = true
      if (enemy.attackCooldown <= 0f) {
        enemy.attackCooldown = 1.65f
        enemy.attackAnim = 1f
        projectiles += CombatProjectile(
          id = UUID.randomUUID().toString(),
          fromHero = false,
          x = enemy.x,
          y = enemy.y - 14f,
          vx = (dx / dist) * 300f,
          vy = (dy / dist) * 300f,
          damage = effectiveAttack,
          color = Color(enemy.archetype.accentColor),
        )
      }
      return
    }

    if (dist > enemy.archetype.attackRange) {
      val (rx, ry) = CollisionPhysics.resolvePosition(
        targetX = enemy.x + (dx / dist) * effectiveSpeed * dt,
        targetY = enemy.y + (dy / dist) * effectiveSpeed * dt,
        entityRadius = 18f * enemy.archetype.scale,
        mapWidth = mapLayout.width,
        mapHeight = mapLayout.height,
        marginWall = mapLayout.wallMargin,
        obstacles = mapLayout.obstacles,
      )
      enemy.x = rx
      enemy.y = ry
      enemy.isMoving = true
    } else {
      enemy.isMoving = false
      if (enemy.attackCooldown <= 0f) {
        enemy.attackCooldown = if (enemy.archetype.isBoss) 1.35f else 1.55f
        enemy.attackAnim = 1f
        if (enemy.archetype.isRanged) {
          projectiles += CombatProjectile(
            id = UUID.randomUUID().toString(),
            fromHero = false,
            x = enemy.x,
            y = enemy.y - 14f,
            vx = (dx / dist) * 300f,
            vy = (dy / dist) * 300f,
            damage = effectiveAttack,
            color = Color(enemy.archetype.accentColor),
          )
        } else {
          if (enemy.archetype == EnemyArchetype.FIELD_GOBLIN_RAIDER) {
            enemy.contactAttack.start(nearest.hero?.id)
            enemy.pendingAttackDamage = effectiveAttack
            return
          }
          if (enemy.archetype.isBoss) {
            aoeEffects += CombatAoEEffect(enemy.x + (dx / dist) * 28f, enemy.y, 75f, Color(0xFFE11D48))
          }
          if (nearest.hero != null) {
            applyDamageToHero(nearest.hero, effectiveAttack)
          } else {
            applyDamageToMaster(effectiveAttack)
          }
        }
      }
    }
  }

  private fun applyDamageToEnemy(enemy: EnemyCombatUnit, rawDmg: Int, isSkill: Boolean) {
    if (enemy.dead) return
    enemy.aggroed = true
    val variance = 0.92f + Random.nextFloat() * 0.16f
    val courierMission = lostCourier
    val counterMult = if (courierMission != null && enemy.id == courierMission.eliteEnemyId &&
      courierMission.eliteRecoveryRemaining > 0f && activeOrder == TacticalOrder.FOCUS_FIRE) 1.35f else 1f
    val mitigated = ((rawDmg * variance * counterMult).toInt() - enemy.defense / 2).coerceAtLeast(6)
    enemy.hp = (enemy.hp - mitigated).coerceAtLeast(0)
    enemy.hurtFlash = 1f
    aoeEffects += CombatAoEEffect(enemy.x, enemy.y - 12f, if (isSkill) 36f else 22f, Color(0xFFFEF08A), duration = 0.25f)
    floatingTexts += FloatingCombatText(
      x = enemy.x + Random.nextFloat() * 18f - 9f,
      y = enemy.y - 36f,
      text = "-$mitigated",
      color = if (isSkill) Color(0xFFF5B841) else Color(0xFFF8FAFC),
      isLarge = isSkill,
    )
    if (enemy.hp <= 0) {
      enemy.dead = true
      aoeEffects += CombatAoEEffect(enemy.x, enemy.y, 58f * enemy.archetype.scale, Color(0xFFEF4444), duration = 0.48f)
      if (designatedTargetId == enemy.id) {
        designatedTargetId = null
      }
    }
  }

  internal fun applyDamageToHero(hero: HeroCombatUnit, rawDmg: Int) {
    if (hero.knockedOut) return
    var mitigated = (rawDmg - hero.defense / 2).coerceAtLeast(4)
    if (hero.shieldHp > 0) {
      val absorbed = minOf(hero.shieldHp, mitigated)
      hero.shieldHp -= absorbed
      mitigated -= absorbed
    }
    hero.hp = (hero.hp - mitigated).coerceAtLeast(0)
    hero.hurtFlash = 1f
    if (mitigated > 0) {
      floatingTexts += FloatingCombatText(hero.x, hero.y - 34f, "-$mitigated", Color(0xFFF87171))
    }
    if (hero.hp <= 0) {
      hero.knockedOut = true
      floatingTexts += FloatingCombatText(hero.x, hero.y - 48f, "${hero.name} Trọng Thương!", Color(0xFFF59E0B), true)
    }
  }

  private fun applyDamageToMaster(rawDmg: Int) {
    val dmg = (rawDmg - 12).coerceAtLeast(5)
    masterHp = (masterHp - dmg).coerceAtLeast(0)
    floatingTexts += FloatingCombatText(masterX, masterY - 38f, "-$dmg", Color(0xFFEF4444), true)
  }

  fun fallenHeroIds(): Set<String> = heroes.filter { it.knockedOut }.map { it.id }.toSet()
}
