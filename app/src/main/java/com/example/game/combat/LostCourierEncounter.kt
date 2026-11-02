package com.example.game.combat

import com.example.data.model.CourierOutcome
import com.example.data.model.HeroRecord
import com.example.data.model.TacticalOrder
import com.example.game.map.CollisionPhysics
import com.example.game.map.EnemyArchetype
import kotlin.math.hypot

enum class CourierPhase { CLEAR_PATH, ESCORT, SCOUT_ALARM, GATE_GUARD, RESCUED, FAILED }
enum class CourierOrder { PROTECT, INTERCEPT }

class CourierCombatUnit {
  var x = 600f
  var y = 1049f
  val maxHp = 300
  var hp = maxHp
  var isMoving = false
  var facingRight = true
  var hurtFlash = 0f
}

data class CourierTelegraph(
  val x: Float,
  val y: Float,
  val radius: Float = 105f,
  var remainingSec: Float = 1.5f,
  val durationSec: Float = 1.5f,
)

/** One authored encounter on the existing road; no new navigation or mission framework. */
class LostCourierEncounter internal constructor(
  private val engine: TowerCombatEngine,
  records: List<HeroRecord>,
) {
  val courier = CourierCombatUnit()
  var choice: CourierChoice? = null
    private set
  val supplyX = 1220f
  val supplyY = 1130f
  var suppliesRecovered = false
    private set
  private var supplyWorkSeconds = 0f
  val choiceAvailable: Boolean
    get() = outcome == null && choice == null && courier.x >= 1100f

  /** A committed field decision, never a reward toggle in the result screen. */
  fun chooseChoice(selected: CourierChoice): Boolean {
    if (!choiceAvailable || engine.stage != MissionStage.IN_PROGRESS) return false
    choice = selected
    if (selected == CourierChoice.SUPPORT_COURIER) {
      courier.hp = (courier.hp + 60).coerceAtMost(courier.maxHp)
      statusVi = "Ưu tiên cứu người: đã băng bó +60 HP. Hộ tống tới cổng để lưu ký ức và +20 Mana lần đầu."
    } else {
      statusVi = "Thu hồi túi tiếp vận tại đường mòn (1220, 1130): giữ Anh hùng ở đó 3s rồi hộ tống tới cổng. +60 vàng lần đầu."
    }
    engine.postFeedback(statusVi)
    return true
  }

  val waypoints = listOf(680f to 1040f, 1220f to 1130f, 1780f to 990f, 2360f to 1080f, 2860f to 1066f)
  var phase = CourierPhase.CLEAR_PATH
    private set
  var order = CourierOrder.PROTECT
    internal set
  var statusVi = "Dọn hai lính gác phía trước để người đưa thư lên đường."
    private set
  val objectiveVi: String
    get() = when (phase) {
      CourierPhase.CLEAR_PATH -> "Mở đường cho người đưa thư bị thương"
      CourierPhase.RESCUED -> "Đã hộ tống người đưa thư tới cổng an toàn"
      CourierPhase.FAILED -> "Hộ tống thất bại • Có thể thử lại"
      else -> "Đưa người đưa thư tới cổng • HP ${courier.hp}/${courier.maxHp}"
    }
  val progress: Float get() = ((courier.x - 600f) / 2260f).coerceIn(0f, 1f)
  var outcome: CourierOutcome? = null
    private set
  var frightenedHeroId: String? = null
    private set
  private val noviceId = records.filter { it.level <= 3L || it.morale < 50L }
    .minWithOrNull(compareBy<HeroRecord> { it.level }.thenBy { if (it.name == "Kaelen") 0 else 1 })?.id
  var scoutEnemyId: String? = null
    private set
  var scoutWarningRemaining = 0f
    private set
  var scoutCalledReinforcements = false
    private set
  var eliteEnemyId: String? = null
    private set
  var eliteTelegraph: CourierTelegraph? = null
    private set
  var eliteRecoveryRemaining = 0f
    private set
  private var waypointIndex = 0
  private var scoutResolved = false

  internal fun initialize() {
    spawn("courier_path_guard_1", EnemyArchetype.FIELD_GOBLIN_RAIDER, 880f, 1000f)
    spawn("courier_path_guard_2", EnemyArchetype.FIELD_GOBLIN_RAIDER, 940f, 1140f)
  }

  private fun spawn(id: String, archetype: EnemyArchetype, x: Float, y: Float, hp: Int = archetype.baseHp) {
    engine.enemies += EnemyCombatUnit(id, archetype, hp, hp, archetype.baseAtk,
      archetype.baseDef, archetype.speed, x, y)
  }

  internal fun beforeStep(dt: Float) {
    if (outcome != null) return
    courier.hurtFlash = (courier.hurtFlash - dt * 3.5f).coerceAtLeast(0f)
    val reassurance = when (engine.activeOrder) {
      TacticalOrder.PROTECT_ALLY -> 2f
      TacticalOrder.REGROUP -> 2.5f
      else -> 1f
    }
    engine.heroes.forEach { it.fearSecondsRemaining = (it.fearSecondsRemaining - dt * reassurance).coerceAtLeast(0f) }
    eliteRecoveryRemaining = (eliteRecoveryRemaining - dt).coerceAtLeast(0f)
    if (phase == CourierPhase.CLEAR_PATH && engine.enemies.filter { it.id.startsWith("courier_path_") }.all { it.dead }) {
      phase = CourierPhase.ESCORT
      statusVi = "Người đưa thư: Cảm ơn! Hãy đi gần tôi. Bảo vệ sẽ giữ đội hình quanh tôi."
      engine.postFeedback(statusVi)
    }
    if (choice == CourierChoice.RECOVER_SUPPLIES && !suppliesRecovered) {
      val working = engine.heroes.any { !it.knockedOut && hypot(it.x - supplyX, it.y - supplyY) <= 90f }
      supplyWorkSeconds = if (working) supplyWorkSeconds + dt else 0f
      if (supplyWorkSeconds >= 3f) {
        suppliesRecovered = true
        statusVi = "Đã thu hồi tiếp vận! Bảo vệ người đưa thư tới cổng để nhận thưởng."
        engine.postFeedback(statusVi)
      }
    }
    courier.isMoving = false
    if (phase != CourierPhase.CLEAR_PATH && waypointIndex < waypoints.size &&
      engine.heroes.any { !it.knockedOut && hypot(it.x - courier.x, it.y - courier.y) <= 300f }) {
      val (tx, ty) = waypoints[waypointIndex]
      val distance = hypot(tx - courier.x, ty - courier.y)
      if (distance <= 8f) waypointIndex++ else {
        val step = minOf(distance, 44f * dt)
        val pos = CollisionPhysics.resolvePosition(courier.x + (tx - courier.x) / distance * step,
          courier.y + (ty - courier.y) / distance * step, 18f, engine.mapLayout.width,
          engine.mapLayout.height, engine.mapLayout.wallMargin, engine.mapLayout.obstacles)
        courier.x = pos.first; courier.y = pos.second
        courier.facingRight = tx >= courier.x
        courier.isMoving = true
      }
    }
    if (scoutEnemyId == null && courier.x >= 1320f) {
      scoutEnemyId = "courier_scout"
      spawn("courier_scout", EnemyArchetype.FIELD_GOBLIN_RAIDER, courier.x + 270f, courier.y - 70f, 100)
      spawn("courier_ambush_1", EnemyArchetype.FIELD_GOBLIN_RAIDER, courier.x + 80f, courier.y + 110f)
      spawn("courier_ambush_2", EnemyArchetype.FIELD_GOBLIN_RAIDER, courier.x + 100f, courier.y - 110f)
      scoutWarningRemaining = 9f // Alert + fleeing 3s, then a clearly displayed 6s horn call.
      phase = CourierPhase.SCOUT_ALARM
      noviceId?.let { id -> engine.heroes.firstOrNull { it.id == id && !it.knockedOut }?.let {
        frightenedHeroId = id
        it.fearSecondsRemaining = 10f
      } }
      statusVi = "Trinh sát chạy gọi viện! Chặn nó, hoặc Bảo vệ để giữ an toàn người đưa thư. Sợ: -20% sát thương, -15% tốc độ, tối đa 10s."
      engine.postFeedback(statusVi)
    }
    if (eliteEnemyId == null && courier.x >= 2220f) {
      eliteEnemyId = "courier_gate_captain"
      spawn("courier_gate_captain", EnemyArchetype.GOBLIN_WAR_CAPTAIN, 2540f, 1050f)
      phase = CourierPhase.GATE_GUARD
      engine.eventWaveTriggeredForCourier()
      statusVi = "Kraag chắn cổng! ĐIỂM ĐẾN để né vòng đỏ (1,5s), rồi DỒN HỎA LỰC khi hắn hồi sức: +35% sát thương."
      engine.postFeedback(statusVi)
    }
  }

  internal fun handles(enemy: EnemyCombatUnit): Boolean = enemy.id.startsWith("courier_")

  internal fun updateEnemy(enemy: EnemyCombatUnit, dt: Float) {
    enemy.attackCooldown = (enemy.attackCooldown - dt).coerceAtLeast(0f)
    enemy.hurtFlash = (enemy.hurtFlash - dt * 3.5f).coerceAtLeast(0f)
    enemy.attackAnim = (enemy.attackAnim - dt * 2.6f).coerceAtLeast(0f)
    if (enemy.id == scoutEnemyId && !scoutResolved) {
      scoutWarningRemaining = (scoutWarningRemaining - dt).coerceAtLeast(0f)
      enemy.aggroed = true
      if (scoutWarningRemaining > 6f) moveEnemy(enemy, enemy.x + 80f, 990f, 58f, dt)
      else enemy.isMoving = false
      enemy.attackAnim = if (scoutWarningRemaining <= 6f) 0.2f else 0f
      if (scoutWarningRemaining <= 0f) {
        scoutResolved = true
        scoutCalledReinforcements = true
        spawn("courier_reinforcement_1", EnemyArchetype.FIELD_GOBLIN_RAIDER, courier.x + 220f, courier.y - 90f)
        spawn("courier_reinforcement_2", EnemyArchetype.FIELD_GOBLIN_RAIDER, courier.x + 240f, courier.y + 90f)
        statusVi = "Tù và đã vang: hai lính gác tới. Tập hợp bảo vệ người đưa thư!"
        engine.postFeedback(statusVi)
      }
      return
    }
    if (enemy.id == eliteEnemyId) {
      updateElite(enemy, dt)
      return
    }
    val cover = if (engine.activeOrder == TacticalOrder.PROTECT_ALLY) engine.heroes
      .filter { !it.knockedOut && hypot(it.x - courier.x, it.y - courier.y) <= 165f }
      .minByOrNull { hypot(it.x - enemy.x, it.y - enemy.y) } else null
    if (enemy.contactAttack.active) {
      enemy.isMoving = false
      if (enemy.contactAttack.advance(dt)) {
        val targetId = enemy.contactAttack.targetId
        if (targetId == null) {
          if (hypot(enemy.x - courier.x, enemy.y - courier.y) <= 58f) damageCourier(enemy.pendingAttackDamage)
        } else engine.heroes.firstOrNull { it.id == targetId && !it.knockedOut }?.let {
          if (hypot(enemy.x - it.x, enemy.y - it.y) <= 58f) engine.applyDamageToHero(it, enemy.pendingAttackDamage)
        }
      }
      enemy.attackAnim = enemy.contactAttack.progress
      return
    }
    val tx = cover?.x ?: courier.x
    val ty = cover?.y ?: courier.y
    if (!enemy.aggroed && hypot(enemy.x - courier.x, enemy.y - courier.y) > 380f) return
    enemy.aggroed = true
    if (hypot(tx - enemy.x, ty - enemy.y) > 42f) moveEnemy(enemy, tx, ty, enemy.speed, dt)
    else {
      enemy.isMoving = false
      if (enemy.attackCooldown <= 0f) {
        enemy.attackCooldown = 1.55f
        enemy.contactAttack.start(cover?.id)
        enemy.pendingAttackDamage = enemy.attack
      }
    }
  }

  private fun updateElite(enemy: EnemyCombatUnit, dt: Float) {
    val warning = eliteTelegraph
    if (warning != null) {
      enemy.isMoving = false
      warning.remainingSec = (warning.remainingSec - dt).coerceAtLeast(0f)
      enemy.attackAnim = 0.22f
      if (warning.remainingSec <= 0f) {
        engine.heroes.filter { !it.knockedOut && hypot(it.x - warning.x, it.y - warning.y) <= warning.radius }.forEach {
          engine.applyDamageToHero(it, 58)
        }
        if (hypot(courier.x - warning.x, courier.y - warning.y) <= warning.radius) damageCourier(48)
        engine.aoeEffects += CombatAoEEffect(warning.x, warning.y, warning.radius,
          androidx.compose.ui.graphics.Color(0xFFEF4444), duration = 0.35f)
        eliteTelegraph = null
        eliteRecoveryRemaining = 1.8f
        enemy.attackCooldown = 4.5f
        enemy.attackAnim = 1f
      }
      return
    }
    if (eliteRecoveryRemaining > 0f) { enemy.isMoving = false; return }
    val target = engine.heroes.filter { !it.knockedOut }.minByOrNull { hypot(it.x - enemy.x, it.y - enemy.y) }
    val tx = target?.x ?: courier.x
    val ty = target?.y ?: courier.y
    if (hypot(tx - enemy.x, ty - enemy.y) > 165f) moveEnemy(enemy, tx, ty, 78f, dt)
    else {
      enemy.isMoving = false
      enemy.aggroed = true
      if (enemy.attackCooldown <= 0f) {
        eliteTelegraph = CourierTelegraph(tx, ty)
        engine.postFeedback("VÒNG ĐỎ: 1,5s! Dùng ĐIỂM ĐẾN né, rồi DỒN HỎA LỰC phản công.")
      }
    }
  }

  private fun moveEnemy(enemy: EnemyCombatUnit, tx: Float, ty: Float, speed: Float, dt: Float) {
    val distance = hypot(tx - enemy.x, ty - enemy.y).coerceAtLeast(1f)
    val step = minOf(distance, speed * dt)
    val pos = CollisionPhysics.resolvePosition(enemy.x + (tx - enemy.x) / distance * step,
      enemy.y + (ty - enemy.y) / distance * step, 18f, engine.mapLayout.width,
      engine.mapLayout.height, engine.mapLayout.wallMargin, engine.mapLayout.obstacles)
    enemy.facingRight = tx >= enemy.x
    enemy.x = pos.first; enemy.y = pos.second; enemy.isMoving = true
  }

  private fun damageCourier(raw: Int) {
    val protected = engine.activeOrder == TacticalOrder.PROTECT_ALLY && engine.heroes.any {
      !it.knockedOut && hypot(it.x - courier.x, it.y - courier.y) <= 165f
    }
    val damage = (raw * if (protected) 0.6f else 1f).toInt().coerceAtLeast(1)
    courier.hp = (courier.hp - damage).coerceAtLeast(0)
    courier.hurtFlash = 1f
    engine.floatingTexts += FloatingCombatText(courier.x, courier.y - 36f, "-$damage",
      androidx.compose.ui.graphics.Color(0xFFF87171))
  }

  internal fun afterStep() {
    if (outcome != null) return
    if (eliteEnemyId != null && engine.enemies.any { it.id == eliteEnemyId && it.dead }) {
      eliteTelegraph = null
      eliteRecoveryRemaining = 0f
    }
    if (!scoutResolved && scoutEnemyId != null && engine.enemies.any { it.id == scoutEnemyId && it.dead }) {
      scoutResolved = true
      scoutWarningRemaining = 0f
      statusVi = "Đã chặn trinh sát trước tiếng tù và! Không có viện binh."
      engine.postFeedback(statusVi)
      if (phase == CourierPhase.SCOUT_ALARM) phase = CourierPhase.ESCORT
    }
    if (courier.hp <= 0 || engine.heroes.none { !it.knockedOut }) {
      fail(if (courier.hp <= 0) "Người đưa thư đã gục ngã. Thử lại và giữ đội hình bảo vệ." else "Đội hình đã gục ngã. Có thể chuẩn bị rồi thử lại.")
    } else if (waypointIndex >= waypoints.size && eliteEnemyId != null && engine.enemies.any { it.id == eliteEnemyId && it.dead }) {
      outcome = if (choice == CourierChoice.RECOVER_SUPPLIES && suppliesRecovered)
        CourierOutcome.SUPPLIES_RECOVERED
      else if (choice == CourierChoice.RECOVER_SUPPLIES) CourierOutcome.RESCUED_WITHOUT_SUPPLIES
      else CourierOutcome.RESCUED
      phase = CourierPhase.RESCUED
      courier.isMoving = false
      statusVi = "Đã cứu người đưa thư! Mở dịch vụ rèn tiếp vận và lưu ký ức đồng đội."
      engine.stage = MissionStage.VICTORY
    }
  }

  internal fun fail(message: String) {
    if (outcome != null) return
    outcome = CourierOutcome.FAILED
    phase = CourierPhase.FAILED
    courier.isMoving = false
    statusVi = message
    eliteTelegraph = null
    engine.stage = MissionStage.DEFEAT
  }
}
