package com.example

import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.TacticalOrder
import com.example.game.combat.ExpeditionSiteState
import com.example.game.combat.LivingExpeditionContent
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.tower.TowerFloorCatalog
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

/** Public engine/control integration. Spatial fixtures below are not playthrough evidence. */
class LivingExpeditionControlTest {
  private fun battle(living: Boolean = true, legacy: Boolean = false) = TowerCombatEngine(
    TowerFloorCatalog.FLOORS.first(),
    listOf(
      HeroRecord(id = "control_knight", name = "Kaelen", heroClass = HeroClassType.KNIGHT.name,
        maxHp = 480, attack = 42, defense = 32, speed = 94),
      HeroRecord(id = "control_archer", name = "Lyra", heroClass = HeroClassType.ARCHER.name,
        maxHp = 320, attack = 54, defense = 16, speed = 118),
      HeroRecord(id = "control_mage", name = "Vesper", heroClass = HeroClassType.MAGE.name,
        maxHp = 290, attack = 60, defense = 14, speed = 102),
    ), emptyList(), enableLostCourier = true, random = Random(93),
    enableRescueJourney = legacy, enableLivingExploration = living,
  ).apply { stage = MissionStage.IN_PROGRESS }

  private fun tick(engine: TowerCombatEngine, count: Int) {
    repeat(count) { engine.step(1f / 60f, 0f, 0f) }
  }

  private fun reachNorthernDecision(engine: TowerCombatEngine) {
    val expedition = engine.lostCourier!!.livingExpedition!!
    engine.resumeLivingExploration("north")
    for (tick in 0 until 90 * 60) {
      if (expedition.decision != null || engine.stage != MissionStage.IN_PROGRESS) break
      engine.step(1f / 60f, 0f, 0f)
    }
    assertEquals("The squad must physically reach a real northern decision", "north_approach", expedition.decision?.id)
  }

  @Test fun controllerModesAreExplicitAndMutuallyExclusive() {
    val ordinary = battle(living = false).lostCourier!!
    assertNull(ordinary.livingExpedition)
    assertNull(ordinary.journey)
    val legacy = battle(living = false, legacy = true).lostCourier!!
    assertNotNull(legacy.journey)
    assertNull(legacy.livingExpedition)
    val current = battle(living = true, legacy = true).lostCourier!!
    assertNotNull(current.livingExpedition)
    assertNull("Legacy progression must not simultaneously gate the living expedition", current.journey)
    assertEquals(setOf("north", "south", "east"), current.livingExpedition!!.sites.map { it.id }.toSet())
  }

  @Test fun everyManualOrderKeepsAuthorityUntilExplicitResume() {
    for (order in TacticalOrder.entries) {
      val engine = battle()
      val expedition = engine.lostCourier!!.livingExpedition!!
      // Keep retreat away from immediate camp extraction; this isolates command ownership.
      engine.heroes.forEach { it.x = 1200f; it.y = 1125f }
      engine.resumeLivingExploration("north")
      assertTrue(expedition.automationEnabled)
      engine.issueTacticalOrder(order)
      assertFalse("$order must suspend autonomy immediately", expedition.automationEnabled)
      tick(engine, 120)
      assertEquals("Autonomy cannot replace $order on a later tick", order, engine.activeOrder)
      assertFalse("$order remains a manual override", expedition.automationEnabled)
      if (engine.stage == MissionStage.IN_PROGRESS) {
        engine.resumeLivingExploration("south")
        assertTrue("Explicit resume must restore exploration after $order", expedition.automationEnabled)
      }
    }
  }

  @Test fun physicalWorldDestinationSuspendsAutonomyAndRetainsItsBeacon() {
    val engine = battle()
    val expedition = engine.lostCourier!!.livingExpedition!!
    engine.resumeLivingExploration("north")
    engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
    engine.onBattlefieldTapped(1180f, 1125f)
    val beacon = engine.moveBeacon
    assertNotNull(beacon)
    assertFalse(expedition.automationEnabled)
    tick(engine, 120)
    assertEquals(TacticalOrder.MOVE_TO, engine.activeOrder)
    assertEquals(beacon, engine.moveBeacon)
    assertFalse(expedition.automationEnabled)
  }

  @Test fun pendingDecisionCannotAutoResolveWinOrFloodActors() {
    val engine = battle()
    val expedition = engine.lostCourier!!.livingExpedition!!
    assertFalse(engine.chooseLivingExpedition("north_scout"))
    reachNorthernDecision(engine)
    val pending = expedition.decision
    val actors = engine.enemies.map { it.id }
    val completed = expedition.completedIds.toSet()
    val courierPosition = engine.lostCourier!!.courier.let { it.x to it.y }
    assertFalse(engine.chooseLivingExpedition("stale_or_unknown_option"))
    tick(engine, 30 * 60)
    assertEquals(pending, expedition.decision)
    assertEquals(completed, expedition.completedIds)
    assertEquals(actors, engine.enemies.map { it.id })
    assertEquals(actors.size, actors.distinct().size)
    assertTrue("The authored roster must remain finite while reading a decision", actors.size <= 16)
    assertTrue("Conversation history must remain bounded while reading", expedition.conversations.size <= 8)
    assertTrue("Visible conversations must remain bounded while reading",
      expedition.conversations.count { it.remainingSeconds > 0f } <= 3)
    assertEquals("A retained conversation cannot be duplicated on each tick",
      expedition.conversations.size, expedition.conversations.map { it.id }.distinct().size)
    assertFalse(expedition.readyToExtract)
    assertNull(engine.lostCourier!!.outcome)
    assertEquals(courierPosition, engine.lostCourier!!.courier.let { it.x to it.y })
    assertEquals(MissionStage.IN_PROGRESS, engine.stage)
    assertTrue(engine.chooseLivingExpedition("north_scout"))
    assertFalse("An accepted decision cannot be applied twice", engine.chooseLivingExpedition("north_scout"))
    assertFalse("Another site's choice cannot bypass investigation", engine.chooseLivingExpedition("south_medicine"))
  }

  @Test fun retryStartsWithUnresolvedKnowledgeAndNoInheritedDecision() {
    val first = battle()
    reachNorthernDecision(first)
    first.issueTacticalOrder(TacticalOrder.HOLD_POSITION)
    val retry = battle()
    val a = first.lostCourier!!.livingExpedition!!
    val b = retry.lostCourier!!.livingExpedition!!
    assertNotSame(a, b)
    assertNull(b.decision)
    assertTrue(b.completedIds.isEmpty())
    assertFalse(b.readyToExtract)
    assertTrue(b.sites.none { it.state == ExpeditionSiteState.RESOLVED })
    assertTrue(b.automationEnabled)
    assertNotSame(first.heroes.first(), retry.heroes.first())
    assertNull(retry.lostCourier!!.outcome)
  }

  @Test fun squadCollapseAtAPendingDecisionCannotGrantKnowledgeOrRescue() {
    val engine = battle()
    reachNorthernDecision(engine)
    val expedition = engine.lostCourier!!.livingExpedition!!
    val completed = expedition.completedIds.toSet()
    // Explicit failure fixture, after real navigation; this is not a balance playthrough.
    engine.heroes.forEach { it.hp = 0; it.knockedOut = true }
    tick(engine, 2)
    assertEquals(MissionStage.DEFEAT, engine.stage)
    assertNotNull(engine.lostCourier!!.outcome)
    assertFalse("A dead squad must not accept an earlier site's choice", engine.chooseLivingExpedition("north_scout"))
    assertEquals(completed, expedition.completedIds)
    assertFalse(expedition.readyToExtract)
  }

  @Test fun aFallenScoutYieldsItsPhysicalTaskToALivingHero() {
    val engine = battle()
    reachNorthernDecision(engine)
    assertTrue(engine.chooseLivingExpedition("north_scout"))
    val expedition = engine.lostCourier!!.livingExpedition!!
    val archer = engine.heroes.first { it.heroClass == HeroClassType.ARCHER }
    val mage = engine.heroes.first { it.heroClass == HeroClassType.MAGE }
    assertEquals(LivingExpeditionContent.northSwitch, expedition.directiveFor(archer)?.destination)
    // Isolated casualty/interaction fixture: the replacement must perform the physical task.
    archer.hp = 0; archer.knockedOut = true
    assertNull(expedition.directiveFor(archer))
    assertEquals("The living mage inherits the scout route instead of stalling the party",
      LivingExpeditionContent.northSwitch, expedition.directiveFor(mage)?.destination)
    assertFalse(expedition.alarmSilenced)
    mage.x = LivingExpeditionContent.northSwitch.first
    mage.y = LivingExpeditionContent.northSwitch.second
    tick(engine, 2)
    assertTrue("Replacement reaches the actual switch before alarm state changes", expedition.alarmSilenced)
    assertTrue("living_alarm_silenced" in expedition.completedIds)
  }

  @Test fun returningPartyCanResumeItsExistingRouteAfterAManualHold() {
    val engine = battle()
    val expedition = engine.lostCourier!!.livingExpedition!!
    // Spatial two-source fixture isolates return ownership; this is not a playthrough.
    assertTrue(engine.resumeLivingExploration("east"))
    val east = expedition.sites.first { it.id == "east" }
    engine.heroes.forEach { it.x = east.x; it.y = east.y }
    tick(engine, 1)
    assertTrue(engine.chooseLivingExpedition("east_cooperate"))
    engine.heroes.forEachIndexed { index, hero ->
      val plate = LivingExpeditionContent.eastPlates[index % 2]
      hero.x = plate.first; hero.y = plate.second
    }
    tick(engine, 1)
    assertTrue(expedition.gateSecured)
    assertTrue(engine.resumeLivingExploration("south"))
    val south = expedition.sites.first { it.id == "south" }
    engine.heroes.forEach { it.x = south.x; it.y = south.y }
    tick(engine, 1)
    assertTrue(engine.chooseLivingExpedition("south_medicine"))
    assertTrue(expedition.readyToExtract)
    assertTrue(engine.chooseLivingExpedition("evacuate"))
    engine.issueTacticalOrder(TacticalOrder.HOLD_POSITION)
    tick(engine, 30)
    assertFalse(expedition.automationEnabled)
    assertTrue(expedition.active)
    assertFalse("Return cannot be replaced by another investigation", engine.resumeLivingExploration("north"))
    assertFalse("Rejected direction change must preserve the manual hold", expedition.automationEnabled)
    val positions = engine.heroes.associate { it.id to (it.x to it.y) }
    assertTrue("Explicit resume must restart the same return route", engine.resumeLivingExploration())
    tick(engine, 30)
    assertTrue(expedition.automationEnabled)
    assertTrue("Resuming return must move real heroes toward the camp corridor", engine.heroes.any {
      val previous = positions.getValue(it.id)
      kotlin.math.hypot(it.x - previous.first, it.y - previous.second) > 10f
    })
    assertFalse("living_returned_to_camp" in expedition.completedIds)
  }

  @Test fun simultaneousWitnessAndSquadDeathCannotAwardSouthernKnowledge() {
    val engine = battle()
    val expedition = engine.lostCourier!!.livingExpedition!!
    // Spatial simultaneous-casualty fixture; this does not establish encounter balance.
    assertTrue(engine.resumeLivingExploration("south"))
    val south = expedition.sites.first { it.id == "south" }
    engine.heroes.forEach { it.x = south.x; it.y = south.y }
    tick(engine, 1)
    assertEquals("south_witness", expedition.decision?.id)
    assertNotNull(expedition.witness)
    expedition.damageWitness(expedition.witness!!.maxHp)
    engine.heroes.forEach { it.hp = 0; it.knockedOut = true }
    tick(engine, 1)
    assertEquals(MissionStage.DEFEAT, engine.stage)
    assertNotNull(engine.lostCourier!!.outcome)
    assertFalse(expedition.witness!!.alive)
    assertFalse(expedition.witness!!.rescued)
    assertNotEquals("A squad that collapses with the witness cannot report the southern evidence",
      ExpeditionSiteState.RESOLVED, expedition.sites.first { it.id == "south" }.state)
    assertFalse("living_site_south" in expedition.completedIds)
    assertFalse("living_witness_rescued" in expedition.completedIds)
    assertFalse(expedition.readyToExtract)
  }
}
