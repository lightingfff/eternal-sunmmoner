package com.example

import com.example.data.model.HeroRecord
import com.example.game.combat.MissionStage
import com.example.game.combat.RescueFieldAction
import com.example.game.combat.RescueFieldActionType
import com.example.game.combat.RescueFieldTarget
import com.example.game.combat.RescueJourneyContent
import com.example.game.combat.TowerCombatEngine
import com.example.game.map.CollisionPhysics
import com.example.game.map.EnemyArchetype
import com.example.game.tower.TowerFloorCatalog
import kotlin.math.hypot
import org.junit.Assert.*
import org.junit.Test

/** Focused simulation fixtures; repositioning/defeated actors here are not playthrough evidence. */
class RescueJourneyFieldworkTest {
  private fun engine(): TowerCombatEngine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(),
    listOf(HeroRecord(id = "fieldwork_hero", name = "Kaelen", maxHp = 480, attack = 46, defense = 28)),
    emptyList(), enableLostCourier = true, enableRescueJourney = true).apply {
    stage = MissionStage.IN_PROGRESS
    enemies.forEach { it.hp = 0; it.dead = true }
    step(.01f, 0f, 0f)
  }

  private fun completeCurrentChapter(engine: TowerCombatEngine, choice: Boolean = true) {
    val journey = engine.lostCourier!!.journey!!
    val original = journey.chapterIndex
    repeat(60) {
      if (journey.chapterIndex != original) return
      engine.heroes.first().apply { x = journey.targetX; y = journey.targetY }
      engine.enemies.forEach { it.hp = 0; it.dead = true }
      engine.step(.01f, 0f, 0f)
      if (journey.fieldReady) journey.interactField()
      if (journey.decisionPending) journey.chooseRoute(choice) else if (journey.canInteract) journey.interact()
    }
    fail("Chapter ${journey.phaseId} did not resolve from physical field actions")
  }

  @Test fun fieldActionRejectsWrongTargetDistanceAndRepeatedCompletion() {
    val action = RescueFieldAction(RescueFieldActionType.READ_TRACKS,
      listOf(RescueFieldTarget("tracks", "Dấu đường", 1340f, 640f)), "Đọc dấu đường")
    assertFalse(action.interact("other", listOf(1340f to 640f)))
    assertFalse(action.interact(emptyList()))
    assertFalse(action.interact(listOf(600f to 1049f)))
    assertFalse(action.completed)
    assertTrue(action.interact(listOf(1340f to 640f)))
    assertTrue(action.completed)
    assertFalse(action.interact(listOf(1340f to 640f)))
  }

  @Test fun courierStaysAtSafeCampUntilExplicitFieldworkAndDebriefAreComplete() {
    val engine = engine()
    val mission = engine.lostCourier!!
    val journey = mission.journey!!
    val start = mission.courier.x to mission.courier.y
    repeat(120) { engine.step(.05f, 0f, 0f) }
    assertEquals(0, journey.chapterIndex)
    assertEquals(start, mission.courier.x to mission.courier.y)
    assertNull(mission.scoutEnemyId)
    repeat(RescueJourneyContent.PRELUDE_CHAPTERS) { completeCurrentChapter(engine) }
    assertFalse(journey.active)
    assertEquals(5, journey.chapterIndex)
    assertEquals(7, journey.chapterCount)
    assertTrue(journey.completedIds.containsAll(listOf("m01b_watchpost", "m01b_lookout", "m01b_signal", "m01b_medicine", "m01b_debrief")))
    assertEquals(start, mission.courier.x to mission.courier.y)
    assertEquals(60, mission.courier.medicineWardHp)
  }

  @Test fun northernEncounterUsesExistingWolfAndRangedStatsAndNeverRespawns() {
    val engine = engine()
    val journey = engine.lostCourier!!.journey!!
    completeCurrentChapter(engine)
    repeat(8) {
      engine.heroes.first().apply { x = journey.targetX; y = journey.targetY }
      engine.step(.01f, 0f, 0f)
    }
    val northern = engine.enemies.filter { it.id.startsWith("journey_lookout_") }
    assertEquals(3, northern.size)
    assertEquals(2, northern.count { it.archetype == EnemyArchetype.DIRE_WOLF })
    assertEquals(1, northern.count { it.archetype == EnemyArchetype.FIELD_GOBLIN_ARCHER })
    northern.forEach {
      assertEquals(it.archetype.baseHp, it.maxHp)
      assertEquals(it.archetype.baseAtk, it.attack)
      assertEquals(it.archetype.speed, it.speed, 0f)
    }
    repeat(30) { engine.step(.01f, 0f, 0f) }
    assertEquals(3, engine.enemies.count { it.id.startsWith("journey_lookout_") })
  }

  @Test fun medicineIsOneExplicitChoiceAndAppliesOnlyToTheSelectedRecipient() {
    val engine = engine()
    val journey = engine.lostCourier!!.journey!!
    repeat(3) { completeCurrentChapter(engine) }
    val hero = engine.heroes.first()
    hero.hp = hero.maxHp / 2
    val before = hero.hp
    completeCurrentChapter(engine, choice = false)
    assertEquals(false, journey.medicineForCourier)
    assertTrue(hero.hp > before)
    assertEquals(0, engine.lostCourier!!.courier.medicineWardHp)
    assertFalse(journey.chooseRoute(true))
    assertEquals(0, engine.lostCourier!!.courier.medicineWardHp)
  }

  @Test fun signalDecisionChangesCrossingThreatsAndCrossingMustActuallyBeCleared() {
    for (silence in listOf(true, false)) {
      val engine = engine()
      val journey = engine.lostCourier!!.journey!!
      completeCurrentChapter(engine)
      completeCurrentChapter(engine)
      completeCurrentChapter(engine, choice = silence)
      completeCurrentChapter(engine)
      completeCurrentChapter(engine)
      engine.lostCourier!!.courier.apply { x = 1901f; y = 1030f }
      engine.heroes.first().apply { x = 1901f; y = 1030f }
      engine.step(.01f, 0f, 0f)
      assertTrue(journey.crossingBlocksEscort)
      assertEquals(if (silence) 2 else 3, engine.enemies.count { it.id.startsWith("journey_crossing_") })
      assertNull(engine.lostCourier!!.eliteEnemyId)
      val courierX = engine.lostCourier!!.courier.x
      repeat(3) { engine.step(.01f, 0f, 0f) }
      assertEquals(courierX, engine.lostCourier!!.courier.x, .001f)
      engine.enemies.filter { it.id.startsWith("journey_crossing_") }.forEach { it.hp = 0; it.dead = true }
      engine.step(.01f, 0f, 0f)
      assertFalse(journey.crossingBlocksEscort)
      assertEquals("gate", journey.phaseId)
      assertTrue("m01b_crossing" in journey.completedIds)
    }
  }

  @Test fun authoredWaypointsAndFieldTargetsRemainOnExistingWalkableMeadow() {
    val engine = engine()
    val points = RescueJourneyContent.routePoints
    for ((from, to) in points.zipWithNext()) {
      val samples = (hypot(to.first - from.first, to.second - from.second) / 8f).toInt() + 1
      for (index in 0..samples) {
        val fraction = index.toFloat() / samples
        val x = from.first + (to.first - from.first) * fraction
        val y = from.second + (to.second - from.second) * fraction
        assertTrue(x in engine.mapLayout.wallMargin..engine.mapLayout.width - engine.mapLayout.wallMargin)
        assertTrue(y in engine.mapLayout.wallMargin..engine.mapLayout.height - engine.mapLayout.wallMargin)
        assertFalse("Route blocked at ($x,$y)", CollisionPhysics.wouldCollide(x, y, 18f, engine.mapLayout.obstacles))
      }
    }
  }
}
