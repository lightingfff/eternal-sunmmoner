package com.example

import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.game.combat.ExpeditionSiteState
import com.example.game.combat.LivingExpedition
import com.example.game.combat.LivingExpeditionContent
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.map.EnemyArchetype
import com.example.game.tower.TowerFloorCatalog
import org.junit.Assert.*
import org.junit.Test

/** Spatial mechanics fixtures; these are deliberately separate from actual movement playthroughs. */
class LivingExpeditionMechanicsTest {
  private fun fixture(solo: Boolean = false): Pair<TowerCombatEngine, LivingExpedition> {
    val records = listOf(
      HeroRecord(id = "plate_knight", name = "Kaelen", heroClass = HeroClassType.KNIGHT.name),
      HeroRecord(id = "plate_archer", name = "Lyra", heroClass = HeroClassType.ARCHER.name),
    ).let { if (solo) it.take(1) else it }
    val engine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), records, emptyList(), enableLostCourier = true)
    engine.stage = MissionStage.IN_PROGRESS
    return engine to LivingExpedition(engine, engine.lostCourier!!.courier)
  }

  private fun reveal(engine: TowerCombatEngine, expedition: LivingExpedition, id: String) {
    assertTrue(expedition.explore(id))
    val site = expedition.sites.first { it.id == id }
    engine.heroes.forEach { it.x = site.x; it.y = site.y }
    expedition.step(.02f)
    assertNotNull(expedition.decision)
  }

  private fun completeQuietNorth(engine: TowerCombatEngine, expedition: LivingExpedition) {
    reveal(engine, expedition, "north")
    assertTrue(expedition.choose("north_scout"))
    val scout = engine.heroes.firstOrNull { it.heroClass == HeroClassType.ARCHER } ?: engine.heroes.first()
    scout.x = LivingExpeditionContent.northSwitch.first; scout.y = LivingExpeditionContent.northSwitch.second
    expedition.step(.02f)
    val north = expedition.sites.first { it.id == "north" }
    engine.heroes.forEach { it.x = north.x; it.y = north.y }
    expedition.step(.02f)
    assertTrue(expedition.alarmSilenced)
    assertTrue("living_site_north" in expedition.completedIds)
  }

  @Test fun scoutMustReachSwitchAndRegroupWhileTwoRealGuardsRemain() {
    val (engine, expedition) = fixture()
    reveal(engine, expedition, "north")
    assertTrue(expedition.choose("north_scout"))
    assertFalse(expedition.choose("north_assault"))
    val guards = engine.enemies.filter { it.id.startsWith("expedition_north_") }
    assertEquals(2, guards.size)
    assertTrue(guards.all { it.archetype == EnemyArchetype.FIELD_GOBLIN_RAIDER && it.hp == it.archetype.baseHp })
    assertFalse(expedition.alarmSilenced)
    val archer = engine.heroes.last()
    val knight = engine.heroes.first()
    assertEquals(LivingExpeditionContent.northSwitch, expedition.directiveFor(archer)!!.destination)
    assertNotEquals(expedition.directiveFor(archer)!!.destination, expedition.directiveFor(knight)!!.destination)
    archer.x = LivingExpeditionContent.northSwitch.first; archer.y = LivingExpeditionContent.northSwitch.second
    expedition.step(.02f)
    assertTrue(expedition.alarmSilenced)
    assertFalse("living_site_north" in expedition.completedIds)
    archer.x = 1540f; archer.y = 585f
    expedition.step(.02f)
    assertTrue("living_site_north" in expedition.completedIds)
    assertTrue("Scouting resolves the objective without globally killing combatants", guards.all { !it.dead })
  }

  @Test fun cooperationRequiresTwoDistinctConcurrentBodiesAndReassignsAfterLoss() {
    val (engine, expedition) = fixture()
    reveal(engine, expedition, "east")
    assertTrue(expedition.choose("east_cooperate"))
    val first = LivingExpeditionContent.eastPlates[0]
    val second = LivingExpeditionContent.eastPlates[1]
    engine.heroes.forEach { it.x = first.first; it.y = first.second }
    expedition.step(.02f)
    assertFalse(expedition.gateSecured)
    engine.heroes.last().apply { x = second.first; y = second.second }
    expedition.step(.02f)
    assertTrue(expedition.gateSecured)
    assertFalse(expedition.gateForced)
    assertTrue("living_gate_cooperation" in expedition.completedIds)
    assertTrue(engine.enemies.none { it.id.startsWith("expedition_east_") })

    val (soloEngine, solo) = fixture()
    reveal(soloEngine, solo, "east")
    assertTrue(solo.choose("east_cooperate"))
    soloEngine.heroes.last().knockedOut = true
    assertNull(solo.directiveFor(soloEngine.heroes.last()))
    soloEngine.heroes.first().apply { x = first.first; y = first.second }
    solo.step(.02f)
    assertFalse(solo.gateSecured)
    assertEquals(second, solo.directiveFor(soloEngine.heroes.first())!!.destination)
    soloEngine.heroes.first().apply { x = second.first; y = second.second }
    solo.step(.02f)
    assertTrue(solo.gateSecured)
    assertTrue("living_gate_solo_latches" in solo.completedIds)
    assertFalse("living_gate_cooperation" in solo.completedIds)
  }

  @Test fun forcedGateSpawnsOnlyTwoNormalGuardsOnceAndRequiresTheirDefeat() {
    val (engine, expedition) = fixture()
    reveal(engine, expedition, "east")
    assertTrue(expedition.choose("east_force"))
    assertFalse(expedition.choose("east_force"))
    repeat(10) { expedition.step(.1f); expedition.afterStep() }
    val guards = engine.enemies.filter { it.id.startsWith("expedition_east_") }
    assertEquals(2, guards.size)
    guards.forEach {
      assertEquals(it.archetype.baseHp, it.maxHp)
      assertEquals(it.archetype.baseAtk, it.attack)
      assertEquals(it.archetype.speed, it.speed, 0f)
    }
    assertFalse(expedition.gateSecured)
    guards.forEach { it.hp = 0; it.dead = true }
    expedition.afterStep()
    assertTrue(expedition.gateSecured && expedition.gateForced)
  }

  @Test fun medicineHealsOnceAndWitnessDeathRecordsKnowledgeWithoutRescue() {
    val (engine, expedition) = fixture()
    reveal(engine, expedition, "south")
    val hero = engine.heroes.first()
    hero.hp = hero.maxHp / 3
    val before = hero.hp
    assertTrue(expedition.choose("south_medicine"))
    assertEquals((before + (hero.maxHp * .35f).toInt()).coerceAtMost(hero.maxHp), hero.hp)
    val healed = hero.hp
    assertFalse(expedition.choose("south_medicine"))
    assertEquals(healed, hero.hp)
    assertFalse(expedition.witness!!.rescued)
    assertEquals(80, expedition.witness!!.hp)

    val (otherEngine, other) = fixture()
    reveal(otherEngine, other, "south")
    assertTrue(other.choose("south_rescue"))
    other.damageWitness(999)
    other.afterStep()
    assertFalse(other.witness!!.alive)
    assertFalse(other.witness!!.rescued)
    assertTrue("living_witness_lost" in other.completedIds)
    assertFalse("living_witness_rescued" in other.completedIds)
    assertEquals(ExpeditionSiteState.RESOLVED, other.sites.first { it.id == "south" }.state)
    assertTrue(other.active)
  }

  @Test fun staleMedicineChoiceCannotHealRemotelyOrResumeSuspendedAutomation() {
    val (engine, expedition) = fixture()
    assertTrue(expedition.canRedirect)
    reveal(engine, expedition, "south")
    assertFalse(expedition.canRedirect)
    val pending = expedition.decision
    engine.heroes.forEach { it.hp = it.maxHp / 3; it.x = 600f; it.y = 1049f }
    val hpBefore = engine.heroes.map { it.hp }
    expedition.suspendAutomation()
    assertFalse(expedition.choose("south_medicine"))
    assertEquals(hpBefore, engine.heroes.map { it.hp })
    assertEquals(pending, expedition.decision)
    assertFalse(expedition.automationEnabled)
    assertFalse("living_medicine_squad" in expedition.completedIds)
    engine.heroes.first().apply { x = 1660f; y = 1565f }
    assertTrue(expedition.choose("south_medicine"))
    assertTrue("living_medicine_squad" in expedition.completedIds)
    assertTrue(expedition.canRedirect)
  }

  @Test fun twoSourcesRequireExplicitExtractionAndEveryLivingHeroPhysicallyAtCamp() {
    val (engine, expedition) = fixture()
    completeQuietNorth(engine, expedition)
    reveal(engine, expedition, "east")
    assertTrue(expedition.choose("east_cooperate"))
    engine.heroes.forEachIndexed { index, hero ->
      val point = LivingExpeditionContent.eastPlates[index]
      hero.x = point.first; hero.y = point.second
    }
    expedition.step(.02f)
    assertTrue(expedition.readyToExtract)
    assertEquals("extraction", expedition.decision!!.id)
    assertTrue(expedition.decision!!.options.any { it.id == "explore_south" })
    repeat(20) { expedition.step(.1f) }
    assertTrue(expedition.active)
    assertTrue(expedition.choose("evacuate"))
    assertTrue(expedition.active)
    engine.heroes.first().apply { x = 600f; y = 1049f }
    expedition.step(.02f)
    assertTrue("The last hero must also return", expedition.active)
    engine.heroes.last().apply { x = 600f; y = 1049f }
    expedition.step(.02f)
    assertFalse(expedition.active)
    assertTrue("living_returned_to_camp" in expedition.completedIds)
  }
}
