package com.example.game.combat

import com.example.game.map.CollisionPhysics
import com.example.game.map.EnemyArchetype
import kotlin.math.hypot

/** Seven one-shot situations, advanced by travel, combat and player decisions, never elapsed time. */
class RescueJourney internal constructor(
  private val engine: TowerCombatEngine,
  private val courier: CourierCombatUnit,
) {
  var chapterIndex = 0
    private set
  val chapterCount: Int get() = RescueJourneyContent.chapters.size
  val active: Boolean get() = chapterIndex < chapterCount
  val phaseId: String get() = chapter?.id ?: "complete"

  /** Return along visited waypoints, starting at the current courier position.
   * The next waypoint has not necessarily been reached yet, so exclude it.
   */
  internal fun retreatWaypoints(): List<Pair<Float, Float>> {
    if (!active) return emptyList()
    return listOf(courier.x to courier.y) +
      RescueJourneyContent.routePoints.take(waypointIndex.coerceAtLeast(1)).asReversed()
  }
  private val chapter: RescueJourneyChapter? get() = RescueJourneyContent.chapters.getOrNull(chapterIndex)
  private var waypointIndex = 1
  private var threatSpawned = false
  private val currentEnemyIds = mutableSetOf<String>()
  private val completed = linkedSetOf<String>()
  var fieldAction: RescueFieldAction? = RescueFieldActions.forChapter(phaseId, site)
    private set
  val fieldReady: Boolean get() = active && threatSpawned && threatsCleared &&
    waypointIndex >= (chapter?.routeEnd ?: Int.MAX_VALUE) && hypot(courier.x-site.first,courier.y-site.second) <= 32f &&
    fieldAction?.completed == false
  fun interactField(): Boolean {
    if (!fieldReady || engine.stage != MissionStage.IN_PROGRESS) return false
    return fieldAction?.interact(engine.heroes.filter { !it.knockedOut }.map { it.x to it.y }) == true
  }

  val completedIds: Set<String> get() = completed.toSet()
  var alarmSilenced: Boolean? = null
    private set
  var statusVi = RescueJourneyContent.chapters.first().briefingVi
    private set
  val objectiveVi: String get() = chapter?.titleVi ?: "Đã mở tuyến hộ tống về cổng"
  val targetX: Float get() = if (fieldReady) fieldAction?.currentTarget?.x ?: target.first else target.first
  val targetY: Float get() = if (fieldReady) fieldAction?.currentTarget?.y ?: target.second else target.second
  private val target: Pair<Float, Float>
    get() = RescueJourneyContent.routePoints[waypointIndex.coerceAtMost(RescueJourneyContent.routePoints.lastIndex)]
  private val site: Pair<Float, Float> get() = RescueJourneyContent.routePoints[chapter?.routeEnd ?: RescueJourneyContent.routePoints.lastIndex]
  private val threatsCleared: Boolean get() = currentEnemyIds.none { id -> engine.enemies.any { it.id == id && !it.dead } }
  val canInteract: Boolean
    get() = active && engine.stage == MissionStage.IN_PROGRESS && threatSpawned && threatsCleared && fieldAction?.completed != false &&
      waypointIndex >= (chapter?.routeEnd ?: Int.MAX_VALUE) && hypot(courier.x - site.first, courier.y - site.second) <= 32f &&
      engine.heroes.count { !it.knockedOut && hypot(it.x - site.first, it.y - site.second) <= 220f } >=
      if (phaseId == "seal") minOf(2, engine.heroes.count { !it.knockedOut }).coerceAtLeast(1) else 1
  val decisionPending: Boolean get() = canInteract && phaseId == "signal" && alarmSilenced == null

  internal fun step(dt: Float) {
    if (!active) return
    courier.isMoving = false
    if (!threatSpawned && hypot(courier.x - site.first, courier.y - site.second) < 250f) spawnThreats()
    if (!threatsCleared || fieldReady || canInteract || decisionPending) return
    if (engine.heroes.none { !it.knockedOut && hypot(it.x - courier.x, it.y - courier.y) <= 300f }) return
    val (tx, ty) = target
    val distance = hypot(tx - courier.x, ty - courier.y)
    if (distance <= 8f) {
      if (waypointIndex < chapter!!.routeEnd) waypointIndex++
      return
    }
    val step = minOf(distance, 44f * dt)
    val pos = CollisionPhysics.resolvePosition(courier.x + (tx - courier.x) / distance * step,
      courier.y + (ty - courier.y) / distance * step, 18f, engine.mapLayout.width,
      engine.mapLayout.height, engine.mapLayout.wallMargin, engine.mapLayout.obstacles)
    courier.facingRight = tx >= courier.x
    courier.isMoving = hypot(pos.first - courier.x, pos.second - courier.y) > 0f
    courier.x = pos.first; courier.y = pos.second
  }

  private fun spawnThreats() {
    threatSpawned = true
    val archetypes = chapter!!.enemies.toMutableList()
    if (phaseId == "crossing" && alarmSilenced == false) archetypes += EnemyArchetype.FIELD_GOBLIN_ARCHER
    archetypes.forEachIndexed { index, archetype ->
      val id = "journey_${phaseId}_$index"
      val pos = CollisionPhysics.resolvePosition(site.first + if (index % 2 == 0) -200f else 200f,
        site.second + if (index % 2 == 0) -130f else 130f, 18f, engine.mapLayout.width,
        engine.mapLayout.height, engine.mapLayout.wallMargin, engine.mapLayout.obstacles)
      engine.enemies += EnemyCombatUnit(id, archetype, archetype.baseHp, archetype.baseHp,
        archetype.baseAtk, archetype.baseDef, archetype.speed, pos.first, pos.second)
      currentEnemyIds += id
    }
    statusVi = chapter!!.briefingVi
    engine.postFeedback(statusVi)
  }

  fun interact(): Boolean {
    if (!canInteract || decisionPending) return false
    when (phaseId) {
      "recon" -> engine.heroes.firstOrNull { !it.knockedOut }?.let {
        it.shieldHp = (it.shieldHp + 30).coerceAtMost(220)
      }
      "wardens" -> healLivingHeroes(.20f)
      "relay" -> healLivingHeroes(.10f)
      "medicine" -> {
        healLivingHeroes(if (alarmSilenced == false) .50f else .35f)
        courier.hp = (courier.hp + 60).coerceAtMost(courier.maxHp)
      }
    }
    completeChapter()
    return true
  }

  fun chooseRoute(silence: Boolean): Boolean {
    if (!decisionPending) return false
    alarmSilenced = silence
    completed += if (silence) "m01b_alarm_silenced" else "m01b_scout_rescued"
    if (!silence) healLivingHeroes(.20f)
    completeChapter()
    return true
  }

  private fun healLivingHeroes(fraction: Float) {
    engine.heroes.forEach { if (!it.knockedOut) it.hp = (it.hp + (it.maxHp * fraction).toInt()).coerceAtMost(it.maxHp) }
  }

  private fun completeChapter() {
    val previous = chapter!!
    completed += "m01b_${previous.id}"
    chapterIndex++
    currentEnemyIds.clear()
    threatSpawned = false
    fieldAction = if (active) RescueFieldActions.forChapter(phaseId, site) else null
    statusVi = previous.resolutionVi + if (active) " " + chapter!!.briefingVi else ""
    engine.postFeedback(statusVi)
    if (active) waypointIndex++
  }
}
