package com.example

import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.game.combat.CombatFxStyle
import com.example.game.combat.EnemyCombatUnit
import com.example.game.combat.MissionStage
import com.example.game.combat.ProjectileVisual
import com.example.game.combat.TowerCombatEngine
import com.example.game.map.EnemyArchetype
import com.example.game.pixel.*
import com.example.game.tower.TowerFloorCatalog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtCompletionAssetsTest {
  private fun preload() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    TowerMonsterAtlas.preload(context)
    CompletionProjectileAtlas.preload(context)
    TowerCombatFxAtlas.preload(context)
  }
  private fun fallback(type: HeroClassType, progress: Float = 0f, back: Boolean = false,
    hp: Float = 1f, hurt: Float = 0f, resting: Boolean = false) =
    requireNotNull(CompletionClassAtlas.frame(type, false, progress, hurt, resting, hp, 0f, back, false))

  @Test fun everyClassAndEnemyHasRealAuthoredArtWithoutDuplicateFacingPixels() {
    preload()
    assertEquals(3, CompletionClassAtlas.loadedCount)
    assertEquals(6, CompletionClassAtlas.decodeCount)
    assertEquals(3_858_432L, CompletionClassAtlas.retainedBytes)
    for (type in HeroClassType.entries) {
      assertNotNull(PremiumPortraits.get("Unknown", type))
      assertNotNull(fallback(type))
      assertEquals(7, fallback(type, progress = 1f).index)
      assertEquals(9, fallback(type, hurt = 1f).index)
      assertEquals(10, fallback(type, resting = true).index)
      assertEquals(11, fallback(type, hp = 0f).index)
      assertEquals(12, fallback(type, back = true).index)
    }
    assertSame(AuthoredHeroAtlas.portrait("Vesper"), PremiumPortraits.get("Kaelen", HeroClassType.MAGE))
    assertSame(AuthoredHeroAtlas.portrait("Kaelen"), PremiumPortraits.get("Unknown", HeroClassType.KNIGHT))
    assertNull(PremiumPortraits.get(null, null))
    assertEquals(4, CompletionEnemyAtlas.loadedCount)
    assertEquals(4, CompletionEnemyAtlas.decodeCount)
    assertEquals(3_538_944L, CompletionEnemyAtlas.retainedBytes)
    assertEquals("Original mirrored banks are now one-facing images", 3_538_944L, TowerMonsterAtlas.retainedBytes)
    for (type in EnemyArchetype.entries) {
      val unit = EnemyCombatUnit("art_$type", type, 100, 100, 10, 0, 0f, 1000f, 1000f)
      val right = requireNotNull(TowerMonsterAtlas.frame(unit, 0f, false, false))
      unit.facingRight = false
      assertSame(right.image, TowerMonsterAtlas.frame(unit, 0f, false, false)!!.image)
      assertEquals(128, right.image.width)
      assertEquals(144, right.image.height)
      if (right.healthBarHeight > 0f) {
        assertTrue(right.healthBarHeight > (right.pivotY - ObliqueSpriteGeometry.bounds(right.image).top) * right.worldScale)
      }
    }
  }

  @Test fun everyExecutionAndProjectileStyleLoadsWithinOriginalCombinedBudgetAndStaysWarm() {
    preload()
    assertEquals("art_completion_v1/effects", TowerCombatFxAtlas.sourcePack)
    assertEquals(2_359_296L, TowerCombatFxAtlas.retainedBytes)
    assertEquals(122_880L, CompletionProjectileAtlas.retainedBytes)
    assertEquals(1, CompletionProjectileAtlas.decodeCount)
    for (style in CombatFxStyle.entries) {
      if (style == CombatFxStyle.GROUND_RING) assertNull(TowerCombatFxAtlas.frame(style, 0f))
      else for (i in 0..7) assertNotNull(TowerCombatFxAtlas.frame(style, (i + .01f) / 8f))
    }
    for (visual in ProjectileVisual.entries) for (i in 0..3) {
      val frame = requireNotNull(CompletionProjectileAtlas.frame(visual, i / 12f))
      assertEquals(48, frame.image.width)
      assertEquals(32, frame.image.height)
    }
    val total = TowerMonsterAtlas.retainedBytes + TowerCombatFxAtlas.retainedBytes + CompletionProjectileAtlas.retainedBytes
    assertEquals(6_021_120L, total)
    assertTrue(total <= 8L * 1024 * 1024)
    assertEquals(1_048_576L, ArchitectureSurfaceArt.retainedBytes)
    assertEquals(4, ArchitectureSurfaceArt.loadedCount)
    assertEquals(56_192L, CompletionPropAtlas.retainedBytes)
    assertEquals(8, CompletionInteriorAtlas.loadedCount)
    assertEquals(8, CompletionInteriorAtlas.decodeCount)
    assertEquals(225_984L, CompletionInteriorAtlas.retainedBytes)
    val first = fallback(HeroClassType.ASSASSIN)
    repeat(100) { preload(); assertSame(first.image, fallback(HeroClassType.ASSASSIN).image) }
    assertEquals(6, CompletionClassAtlas.decodeCount)
    assertEquals(4, CompletionEnemyAtlas.decodeCount)
    assertEquals(1, CompletionProjectileAtlas.decodeCount)
    assertEquals(4, ArchitectureSurfaceArt.decodeCount)
    assertEquals(3, CompletionPropAtlas.decodeCount)
  }

  @Test fun actualGenericAttackEventsReleaseImmediatelyWhileKaelenKeepsHisContactMarker() {
    preload()
    for (type in HeroClassType.entries) {
      val engine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), listOf(
        HeroRecord(id = "generic", name = "Unknown", heroClass = type.name)), emptyList())
      engine.stage = MissionStage.IN_PROGRESS
      // Keep the fixture outside Master's automatic bolt range, isolating real hero events.
      val hero = engine.heroes.single().apply { x = 800f; y = 1100f; skillCooldown = 999f; attackCooldown = 0f }
      val ranged = type in listOf(HeroClassType.ARCHER, HeroClassType.MAGE, HeroClassType.PRIEST)
      val enemy = EnemyCombatUnit("target", EnemyArchetype.FIELD_GOBLIN_RAIDER, 500, 500, 0, 0, 0f,
        hero.x + if (ranged) 140f else 30f, hero.y).apply { attackCooldown = 999f; aggroed = true }
      engine.enemies.clear(); engine.enemies += enemy
      engine.step(.016f, 0f, 0f)
      assertFalse(hero.contactAttack.active)
      assertTrue("$type must execute its actual basic attack", if (ranged) engine.projectiles.any { it.sourceId == hero.id } else enemy.hp < 500)
      assertEquals("$type may not show a post-hit windup", 7, fallback(type, hero.attackAnim).index)
    }
    val engine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), listOf(
      HeroRecord(id = "kaelen", name = "Kaelen")), emptyList())
    engine.stage = MissionStage.IN_PROGRESS
    val hero = engine.heroes.single().apply { x = 800f; y = 1100f; skillCooldown = 999f }
    val enemy = EnemyCombatUnit("target", EnemyArchetype.FIELD_GOBLIN_RAIDER, 500, 500, 0, 0, 0f,
      hero.x + 30f, hero.y).apply { attackCooldown = 999f; aggroed = true }
    engine.enemies.clear(); engine.enemies += enemy
    engine.step(.016f, 0f, 0f)
    assertEquals(500, enemy.hp)
    assertTrue(hero.contactAttack.active)
    val untilJustBeforeContact = hero.contactAttack.contact - hero.contactAttack.elapsed - .001f
    assertTrue(untilJustBeforeContact > 0f)
    engine.step(untilJustBeforeContact, 0f, 0f)
    assertEquals(500, enemy.hp)
    engine.step(.001f, 0f, 0f)
    assertTrue(enemy.hp < 500)
    assertEquals(7, AuthoredHeroAtlas.frame("Kaelen", HeroClassType.KNIGHT, false, hero.attackAnim,
      0f, false, 1f, 0f, false, false)!!.index)
    val afterContact = enemy.hp
    engine.step(.02f, 0f, 0f)
    assertEquals("Contact must not be applied twice", afterContact, enemy.hp)
  }
}
