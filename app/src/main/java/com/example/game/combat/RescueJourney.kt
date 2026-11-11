package com.example.game.combat

import com.example.game.map.CollisionPhysics
import com.example.game.map.EnemyArchetype
import kotlin.math.hypot

/** Field investigation before escort, then two milestones in the original escort. No clock gates. */
class RescueJourney internal constructor(
  private val engine: TowerCombatEngine,
  private val courier: CourierCombatUnit,
) {
  var chapterIndex = 0
    private set
  val chapterCount: Int get() = RescueJourneyContent.chapters.size
  /** Only fieldwork suspends the courier at the safe camp. Crossing/gate use the real escort. */
  val active: Boolean get() = chapterIndex < RescueJourneyContent.PRELUDE_CHAPTERS
  val phaseId: String get() = chapter?.id ?: "complete"
  private val chapter: RescueJourneyChapter? get() = RescueJourneyContent.chapters.getOrNull(chapterIndex)
  private val site: Pair<Float, Float> get() = RescueJourneyContent.routePoints[chapter?.routeEnd ?: RescueJourneyContent.routePoints.lastIndex]
  private var waypointIndex = 0
  private var routeArrived = false
  private var threatSpawned = false
  private val enemyIds = linkedSetOf<String>()
  private val completed = linkedSetOf<String>()
  private val visitedPoints = mutableListOf(RescueJourneyContent.routePoints.first())
  private var crossingSpawned = false
  val completedIds: Set<String> get() = completed.toSet()
  var alarmSilenced: Boolean? = null
    private set
  var medicineForCourier: Boolean? = null
    private set
  var statusVi: String = RescueJourneyContent.chapters.first().briefingVi
    private set
  val objectiveVi: String get() = chapter?.titleVi ?: "Đã đưa người đưa thư về cổng"
  var fieldAction: RescueFieldAction? = RescueFieldActions.forChapter(phaseId, site)
    private set
  private val threatsCleared: Boolean get() = enemyIds.all { id -> engine.enemies.any { it.id == id && it.dead } }
  private val livingPositions: List<Pair<Float, Float>> get() = engine.heroes.filter { !it.knockedOut }.map { it.x to it.y }
  private val fieldStageReached: Boolean get() = routeArrived ||
    (phaseId == "lookout" && fieldAction?.completedSteps == 0 && waypointIndex >= 3)
  val fieldReady: Boolean get() = active && engine.stage == MissionStage.IN_PROGRESS && threatSpawned && threatsCleared &&
    fieldStageReached && fieldAction?.completed == false
  val canInteract: Boolean get() = active && engine.stage == MissionStage.IN_PROGRESS && routeArrived &&
    threatSpawned && threatsCleared && fieldAction?.completed != false &&
    livingPositions.any { hypot(it.first - site.first, it.second - site.second) <= RescueFieldAction.INTERACTION_RADIUS }
  val decisionPending: Boolean get() = canInteract &&
    ((phaseId == "signal" && alarmSilenced == null) || (phaseId == "medicine" && medicineForCourier == null))
  private val target: Pair<Float, Float> get() = when {
    !active -> site
    fieldStageReached && fieldAction?.completed == false -> fieldAction!!.currentTarget!!.let { it.x to it.y }
    routeArrived -> site
    else -> RescueJourneyContent.routePoints[waypointIndex]
  }
  val targetX: Float get() = target.first
  val targetY: Float get() = target.second
  val crossingBlocksEscort: Boolean get() = chapterIndex == RescueJourneyContent.PRELUDE_CHAPTERS && crossingSpawned && !threatsCleared

  /** Breadcrumbs already visited by the squad; the stationary courier did not tour the meadow. */
  internal fun retreatWaypoints(): List<Pair<Float, Float>> = if (active) visitedPoints.asReversed().toList() else emptyList()

  internal fun step(@Suppress("UNUSED_PARAMETER") dt: Float) {
    if (!active || engine.stage != MissionStage.IN_PROGRESS) return
    courier.isMoving = false
    val positions = livingPositions
    if (!threatSpawned && positions.any { hypot(it.first - site.first, it.second - site.second) <= 420f }) spawnThreats()
    if (!routeArrived) {
      val point = RescueJourneyContent.routePoints[waypointIndex]
      if (positions.any { hypot(it.first - point.first, it.second - point.second) <= 140f }) {
        if (visitedPoints.lastOrNull() != point) visitedPoints += point
        if (waypointIndex >= chapter!!.routeEnd) routeArrived = true else waypointIndex++
      }
    }
  }

  fun interactField(): Boolean {
    if (!fieldReady) return false
    val action = fieldAction ?: return false
    val result = action.interact(livingPositions)
    if (result) {
      statusVi = if (action.completed) "Đã kiểm tra hiện trường. " + chapter!!.briefingVi else action.hintVi
      engine.postFeedback(statusVi)
    }
    return result
  }

  fun interact(): Boolean {
    if (!canInteract || decisionPending) return false
    completeChapter()
    return true
  }

  /** Signal: true disables an extra archer, false uses spare bandages. Medicine: true protects courier, false heals squad. */
  fun chooseRoute(silence: Boolean): Boolean {
    if (!decisionPending) return false
    if (phaseId == "signal") {
      alarmSilenced = silence
      completed += if (silence) "m01b_alarm_silenced" else "m01b_spare_bandages"
      if (!silence) healLivingHeroes(.20f)
    } else {
      medicineForCourier = silence
      completed += if (silence) "m01b_medicine_courier" else "m01b_medicine_squad"
      if (silence) courier.medicineWardHp = 60 else healLivingHeroes(.35f)
    }
    completeChapter()
    return true
  }

  private fun spawnThreats() {
    threatSpawned = true
    val spawns = when (phaseId) {
      "lookout" -> listOf(Triple(EnemyArchetype.DIRE_WOLF, 1400f, 620f),
        Triple(EnemyArchetype.DIRE_WOLF, 1510f, 700f), Triple(EnemyArchetype.FIELD_GOBLIN_ARCHER, 1610f, 640f))
      "medicine" -> listOf(Triple(EnemyArchetype.FIELD_GOBLIN_RAIDER, 1540f, 1490f), Triple(EnemyArchetype.DIRE_WOLF, 1660f, 1540f))
      "crossing" -> buildList {
        add(Triple(EnemyArchetype.DIRE_WOLF, 2160f, 890f))
        add(Triple(EnemyArchetype.FIELD_GOBLIN_ARCHER, 2220f, 1240f))
        if (alarmSilenced == false) add(Triple(EnemyArchetype.FIELD_GOBLIN_ARCHER, 2450f, 1150f))
      }
      else -> emptyList()
    }
    spawns.forEachIndexed { index, (archetype, x, y) ->
      val id = "journey_${phaseId}_$index"
      val pos = CollisionPhysics.resolvePosition(x, y, 18f, engine.mapLayout.width,
        engine.mapLayout.height, engine.mapLayout.wallMargin, engine.mapLayout.obstacles)
      engine.enemies += EnemyCombatUnit(id, archetype, archetype.baseHp, archetype.baseHp,
        archetype.baseAtk, archetype.baseDef, archetype.speed, pos.first, pos.second)
      enemyIds += id
    }
    statusVi = chapter!!.briefingVi
    engine.postFeedback(statusVi)
  }

  /** Called by the real escort loop; crossing and gate never require decorative completion taps. */
  internal fun observeEscortState() {
    if (active || chapterIndex >= chapterCount) return
    if (phaseId == "crossing") {
      if (!crossingSpawned && courier.x >= 1900f) {
        crossingSpawned = true
        spawnThreats()
      }
      if (crossingSpawned && threatsCleared) completeChapter()
    }
  }

  internal fun rescued() {
    if (phaseId == "gate") completeChapter()
  }

  private fun healLivingHeroes(fraction: Float) {
    engine.heroes.forEach { if (!it.knockedOut) it.hp = (it.hp + (it.maxHp * fraction).toInt()).coerceAtMost(it.maxHp) }
  }

  private fun completeChapter() {
    val previous = chapter ?: return
    completed += "m01b_${previous.id}"
    chapterIndex++
    enemyIds.clear()
    threatSpawned = false
    routeArrived = false
    if (active) waypointIndex = previous.routeEnd + 1
    fieldAction = if (active) RescueFieldActions.forChapter(phaseId, site) else null
    statusVi = previous.resolutionVi + if (chapter != null) " " + chapter!!.briefingVi else ""
    engine.postFeedback(statusVi)
  }
}
