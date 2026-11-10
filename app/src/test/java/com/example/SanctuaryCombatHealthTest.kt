package com.example

import com.example.data.model.EquipmentRecord
import com.example.data.model.HeroClassType
import com.example.data.model.HeroHiddenTrait
import com.example.data.model.HeroRecord
import com.example.data.model.TacticalOrder
import com.example.game.combat.EnemyCombatUnit
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.map.EnemyArchetype
import com.example.game.tower.TowerFloorCatalog
import kotlin.math.roundToLong
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class SanctuaryCombatHealthTest {
  private class ScriptedRandom(vararg rolls: Float) : Random() {
    private val values = ArrayDeque(rolls.toList())
    override fun nextBits(bitCount: Int): Int = 0
    override fun nextFloat(): Float = values.removeFirstOrNull() ?: .5f
  }

  private fun record(
    id: String = "hero",
    hp: Long = 400,
    heroClass: HeroClassType = HeroClassType.KNIGHT,
    criticalTraitUnlocked: Boolean = false,
  ) = HeroRecord(
    id = id, name = "Test Hero", maxHp = 400, currentHp = hp, attack = 100, defense = 0,
    speed = 100, morale = 60, heroClass = heroClass.name,
    hiddenTrait = HeroHiddenTrait(id = "assassin_instinct", unlocked = criticalTraitUnlocked),
  )

  private fun combat(
    hero: HeroRecord = record(),
    random: Random = ScriptedRandom(),
    enemyDistance: Float = 40f,
  ): TowerCombatEngine {
    val floor = TowerFloorCatalog.FLOORS.first().copy(eventTriggerX = Float.MAX_VALUE, enemies = emptyList())
    return TowerCombatEngine(floor, listOf(hero), emptyList(), random = random).apply {
      stage = MissionStage.IN_PROGRESS
      masterX = 2800f; masterY = 2100f
      heroes.single().apply {
        x = 900f; y = 1100f; skillCooldown = 1000f
      }
      enemies += EnemyCombatUnit(
        id = "durable_target", archetype = EnemyArchetype.FIELD_GOBLIN_RAIDER,
        maxHp = 10000, hp = 10000, attack = 0, defense = 0, speed = 0f,
        x = 900f + enemyDistance, y = 1100f,
      ).apply { attackCooldown = 1000f }
      issueTacticalOrder(TacticalOrder.HOLD_POSITION)
    }
  }

  @Test fun persistedHpFractionSurvivesEquipmentAndFeastBonusesWithoutStartingFull() {
    val floor = TowerFloorCatalog.FLOORS.first()
    val saved = record(hp = 100).copy(equippedArmorId = "armor")
    val armor = EquipmentRecord(id = "armor", slot = "ARMOR", bonusHp = 200, bonusAttack = 0)
    val engine = TowerCombatEngine(floor, listOf(saved), listOf(armor), feastBuffActive = true)
    val hero = engine.heroes.single()
    assertEquals(660, hero.maxHp)
    assertEquals(165, hero.hp)
    assertEquals(.25f, engine.remainingHeroHpRatios().getValue(saved.id), .0001f)
    assertFalse(hero.knockedOut)
  }

  @Test fun legacyUnknownHpStartsFullButExplicitZeroStaysKnockedOutAndOneHpStaysAlive() {
    val floor = TowerFloorCatalog.FLOORS.first()
    val engine = TowerCombatEngine(floor, listOf(record("legacy", -1), record("zero", 0), record("one", 1)), emptyList())
    val byId = engine.heroes.associateBy { it.id }
    assertEquals(byId.getValue("legacy").maxHp, byId.getValue("legacy").hp)
    assertEquals(0, byId.getValue("zero").hp)
    assertTrue(byId.getValue("zero").knockedOut)
    assertEquals(setOf("zero"), engine.fallenHeroIds())
    assertEquals(1, byId.getValue("one").hp)
    assertFalse(byId.getValue("one").knockedOut)
    assertEquals(0f, engine.remainingHeroHpRatios().getValue("zero"), 0f)
  }

  @Test fun equipmentHpScalingRoundTripsWithoutHealingOrDamageFromIntegerRounding() {
    val floor = TowerFloorCatalog.FLOORS.first()
    val armor = EquipmentRecord(id = "armor", slot = "ARMOR", bonusHp = 50, bonusAttack = 0)
    for (savedHp in listOf(1L, 27L, 91L, 349L)) {
      val saved = record(hp = savedHp).copy(maxHp = 350, equippedArmorId = armor.id)
      val engine = TowerCombatEngine(floor, listOf(saved), listOf(armor))
      assertEquals(400, engine.heroes.single().maxHp)
      val ratio = engine.remainingHeroHpRatios().getValue(saved.id)
      assertEquals("Gear changes cannot alter saved HP $savedHp", savedHp, (ratio * saved.maxHp).roundToLong())
      if (savedHp == 91L) assertEquals(104, engine.heroes.single().hp)
    }
  }

  @Test fun remainingHpSnapshotIsDetachedIncludesFallenHeroesAndCanStartTheNextBattleWounded() {
    val first = combat()
    val hero = first.heroes.single()
    first.applyDamageToHero(hero, 300)
    val snapshot = first.remainingHeroHpRatios()
    assertEquals(.25f, snapshot.getValue(hero.id), .0001f)
    val savedHp = (snapshot.getValue(hero.id) * 400).toLong()
    val next = combat(record(hp = savedHp))
    assertEquals(100, next.heroes.single().hp)
    first.applyDamageToHero(hero, 100)
    assertEquals(0f, first.remainingHeroHpRatios().getValue(hero.id), 0f)
    assertEquals(.25f, snapshot.getValue(hero.id), .0001f)
  }

  @Test fun awakenedAssassinTraitDealsRealOneAndHalfMeleeDamageWhileLockedTraitDoesNot() {
    val normal = combat(record(), ScriptedRandom(.5f))
    val critical = combat(record(criticalTraitUnlocked = true), ScriptedRandom(.1f, .5f))
    normal.step(.01f, 0f, 0f)
    critical.step(.01f, 0f, 0f)
    assertEquals(100, 10000 - normal.enemies.single().hp)
    assertEquals(150, 10000 - critical.enemies.single().hp)
  }

  @Test fun criticalChanceBoundaryIsThirtyPercentAndNotGuaranteedForAwakenedHeroes() {
    val succeeds = combat(record(criticalTraitUnlocked = true), ScriptedRandom(.2999f, .5f))
    val misses = combat(record(criticalTraitUnlocked = true), ScriptedRandom(.30f, .5f))
    succeeds.step(.01f, 0f, 0f)
    misses.step(.01f, 0f, 0f)
    assertEquals(150, 10000 - succeeds.enemies.single().hp)
    assertEquals(100, 10000 - misses.enemies.single().hp)
  }

  @Test fun premiumKnightCriticalHitStillWaitsForTheAuthoredContactFrame() {
    val engine = combat(record(criticalTraitUnlocked = true).copy(name = "Kaelen"), ScriptedRandom(.1f, .5f))
    val target = engine.enemies.single()
    engine.step(.01f, 0f, 0f)
    assertTrue(engine.heroes.single().contactAttack.active)
    assertEquals(10000, target.hp)
    engine.step(.21f, 0f, 0f)
    assertEquals(10000, target.hp)
    engine.step(.01f, 0f, 0f)
    assertEquals(9850, target.hp)
    engine.step(.1f, 0f, 0f)
    assertEquals("Recovery cannot apply the critical hit twice", 9850, target.hp)
  }

  @Test fun criticalArrowUsesProjectileDeliveryAndCannotApplyDamageAtLaunch() {
    val engine = combat(
      record(heroClass = HeroClassType.ARCHER, criticalTraitUnlocked = true),
      ScriptedRandom(.1f, .5f), enemyDistance = 200f,
    )
    val target = engine.enemies.single()
    engine.step(.01f, 0f, 0f)
    assertEquals(10000, target.hp)
    assertEquals(150, engine.projectiles.single { it.fromHero }.damage)
    repeat(60) { engine.step(.01f, 0f, 0f) }
    assertEquals(9850, target.hp)
    assertTrue(engine.projectiles.none { it.fromHero })
  }
}
