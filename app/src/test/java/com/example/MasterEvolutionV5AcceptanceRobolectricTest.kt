package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.GuestSaveSnapshot
import com.example.data.SanctuaryProgressionHelper
import com.example.data.firebase.ExpeditionOutcomeType
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.game.map.BiomeTheme
import com.example.data.model.EquipmentRecord
import com.example.data.model.FacilityType
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.HeroStatusType
import com.example.data.model.IslandExpansionMilestone
import com.example.data.model.MasterProfile
import com.example.data.model.PersonalityType
import com.example.data.model.SanctuaryMealRecipe
import com.example.data.model.TacticalOrder
import com.example.data.model.TrainingFocus
import com.example.game.combat.EnemyCombatUnit
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.map.CollisionPhysics
import com.example.game.map.EnemyArchetype
import com.example.game.map.SanctuaryIslandGeometry
import com.example.game.tower.TowerFloorCatalog
import com.example.ui.SummonerUiState
import com.example.ui.screens.SanctuaryLobbyScreen
import com.example.ui.theme.EternalSummonerTheme
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.ui.test.performScrollTo
import com.example.game.story.ManhwaStoryData
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Comprehensive Robolectric Acceptance Suite verifying all 20 Acceptance Checks
 * in ETERNAL SUMMONER — Master Evolution Directive (Version 5.0, Part 17).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MasterEvolutionV5AcceptanceRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private fun sampleMasterProfile(): MasterProfile = MasterProfile(
    id = "u_v5_master",
    userId = "u_v5_master",
    masterName = "Master Lokir",
    masterLevel = 8,
    masterExp = 1750,
    gold = 4200,
    manaCrystals = 480,
    soulShards = 120,
    rations = 24,
    ironOre = 30,
    arcaneEssence = 20,
    highestClearedFloor = 2,
    totalExpeditions = 6,
    storyFlags = listOf(ManhwaStoryData.FLAG_PROLOGUE_SEEN),
  )

  private fun sampleSquadRoster(nowSec: Long = System.currentTimeMillis() / 1000L): List<HeroRecord> = listOf(
    HeroRecord(
      id = "h_knight",
      userId = "u_v5_master",
      name = "Kaelen",
      title = "Khiên Thép",
      heroClass = HeroClassType.KNIGHT.name,
      rarity = 4,
      level = 5,
      maxHp = 520,
      attack = 52,
      defense = 34,
      speed = 104,
      skillLevel = 2,
      personality = PersonalityType.STOIC.name,
      status = HeroStatusType.READY.name,
      morale = 94,
      equippedWeaponId = "eq_sword_1",
      inActiveSquad = true,
    ),
    HeroRecord(
      id = "h_mage",
      userId = "u_v5_master",
      name = "Vesper",
      title = "Tinh Vân",
      heroClass = HeroClassType.MAGE.name,
      rarity = 5,
      level = 5,
      maxHp = 340,
      attack = 68,
      defense = 16,
      speed = 114,
      skillLevel = 2,
      personality = PersonalityType.CALCULATING.name,
      status = HeroStatusType.TRAINING.name,
      morale = 90,
      inActiveSquad = true,
    ),
    HeroRecord(
      id = "h_injured",
      userId = "u_v5_master",
      name = "Lyra",
      title = "Mắt Ưng",
      heroClass = HeroClassType.ARCHER.name,
      rarity = 4,
      level = 4,
      maxHp = 330,
      attack = 56,
      defense = 17,
      speed = 132,
      skillLevel = 1,
      personality = PersonalityType.CYNICAL.name,
      status = HeroStatusType.INJURED.name,
      injuryRecoveryUntilEpochSec = nowSec + 180L,
      morale = 48,
      inActiveSquad = false,
    ),
  )

  private fun sampleEquipment(): List<EquipmentRecord> = listOf(
    EquipmentRecord(
      id = "eq_sword_1",
      userId = "u_v5_master",
      name = "Trường Kiếm Tinh Quang +2",
      slot = "WEAPON",
      rarity = 3,
      level = 2,
      bonusHp = 20,
      bonusAttack = 14,
      bonusDefense = 3,
      bonusSpeed = 2,
      iconType = "LONGSWORD",
      equippedByHeroId = "h_knight",
    ),
    EquipmentRecord(
      id = "eq_sword_2",
      userId = "u_v5_master",
      name = "Đại Kiếm Hỏa Long +4",
      slot = "WEAPON",
      rarity = 4,
      level = 4,
      bonusHp = 35,
      bonusAttack = 26,
      bonusDefense = 5,
      bonusSpeed = 0,
      iconType = "LONGSWORD",
      equippedByHeroId = "",
    ),
  )

  @Test
  @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun check01To03_sanctuaryMapGeometry_panZoomHudStability_andExternalCommanderTap() {
    // Check 1: Preserve recognizable 3200x2760 8-sided floating Sanctuary fortress-island & landmarks
    assertEquals(8, SanctuaryIslandGeometry.POLYGON_VERTICES.size)
    assertEquals(3200f, SanctuaryIslandGeometry.WORLD_WIDTH, 0.1f)
    assertEquals(2760f, SanctuaryIslandGeometry.WORLD_HEIGHT, 0.1f)
    assertEquals(1940f, SanctuaryIslandGeometry.PLAZA_CENTER_X, 0.1f)
    assertEquals(1240f, SanctuaryIslandGeometry.PLAZA_CENTER_Y, 0.1f)
    assertEquals(555f, SanctuaryIslandGeometry.PLAZA_RADIUS, 0.1f)
    assertEquals(7, TowerFloorCatalog.SANCTUARY_ZONES.size)

    // Check 3: Direct facility tap selects facility as external Master commander without walking an avatar
    val engine = SanctuaryLobbyEngine()
    val gateZone = TowerFloorCatalog.SANCTUARY_ZONES.first { it.facility == FacilityType.TOWER_GATE }
    val tappedFacility = engine.selectFacilityAt(gateZone.centerX, gateZone.centerY)
    assertEquals(FacilityType.TOWER_GATE, tappedFacility)
    assertEquals(FacilityType.TOWER_GATE, engine.selectedFacility)

    // Check 2: Pan/Zoom controls work smoothly while top HUD stays anchored & stable
    val state = SummonerUiState(
      isGuestAccount = true,
      profile = sampleMasterProfile(),
      heroes = sampleSquadRoster(),
      equipment = sampleEquipment(),
    )
    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      EternalSummonerTheme {
        SanctuaryLobbyScreen(
          state = state,
          onOpenFacility = {},
          onCloseFacility = {},
          onSummonHero = {},
          onTrainHero = { _, _ -> },
          onEquipItem = { _, _ -> },
          onSynthesizeGear = {},
          onEnhanceGear = {},
          onRecoverHero = { _, _ -> },
          onHostMeal = {},
          onToggleSquad = {},
          onCompletePrologue = {},
          onSignOut = {},
        )
      }
    }
    composeTestRule.mainClock.advanceTimeBy(64L)

    val hudBefore = composeTestRule.onNodeWithTag("sanctuary_top_hud").fetchSemanticsNode().boundsInRoot
    composeTestRule.onNodeWithTag("camera_zoom_in_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(48L)
    composeTestRule.onNodeWithTag("camera_zoom_out_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(48L)
    composeTestRule.onNodeWithTag("camera_reset_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(48L)
    val hudAfter = composeTestRule.onNodeWithTag("sanctuary_top_hud").fetchSemanticsNode().boundsInRoot

    assertEquals(hudBefore.top, hudAfter.top, 0.5f)
    assertEquals(hudBefore.left, hudAfter.left, 0.5f)
  }

  @Test
  fun check04To05_allFourEnterableInteriors_restoreExactCameraAndSelectionOnExit() {
    val nowSec = System.currentTimeMillis() / 1000L
    val engine = SanctuaryLobbyEngine()
    engine.syncHeroes(sampleSquadRoster(nowSec), nowSec)

    assertEquals(
      setOf(
        FacilityType.HERO_QUARTERS,
        FacilityType.RESTAURANT,
        FacilityType.TRAINING_GROUND,
        FacilityType.ARMORY,
      ),
      SanctuaryLobbyEngine.ENTERABLE_FACILITIES,
    )

    val expectedBiomes = mapOf(
      FacilityType.HERO_QUARTERS to BiomeTheme.DORMITORY_INTERIOR,
      FacilityType.RESTAURANT to BiomeTheme.RESTAURANT_INTERIOR,
      FacilityType.TRAINING_GROUND to BiomeTheme.TRAINING_HALL_INTERIOR,
      FacilityType.ARMORY to BiomeTheme.ARMORY_INTERIOR,
    )

    SanctuaryLobbyEngine.ENTERABLE_FACILITIES.forEach { facility ->
      engine.setCameraPosition(1420f, 1110f)
      engine.setZoomClamped(0.76f)
      engine.selectedHeroId = "h_knight"
      engine.selectedFacility = facility

      engine.enterFacilityInterior(facility)
      assertTrue("Expected inside interior for $facility", engine.isInsideInterior)
      assertEquals(facility, engine.activeInterior)
      assertEquals(expectedBiomes[facility], engine.mapLayout.biome)
      assertTrue("Expected furniture/obstacles inside $facility", engine.mapLayout.obstacles.isNotEmpty())
      assertTrue("Expected resident NPCs/heroes inside $facility", engine.heroActors.isNotEmpty())

      engine.exitFacilityInterior()
      assertFalse(engine.isInsideInterior)
      assertNull(engine.activeInterior)
      assertEquals(1420f, engine.camX, 0.5f)
      assertEquals(1110f, engine.camY, 0.5f)
      assertEquals(0.76f, engine.zoom, 0.01f)
      assertEquals("h_knight", engine.selectedHeroId)
      assertEquals(facility, engine.selectedFacility)
    }
  }

  @Test
  @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun check06To10_heroStateConsistency_injuredDormsPlacement_andFiveTabHeroProfileWithEquipDiff() {
    runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val saveManager = LocalGuestSaveManager(context)
    saveManager.primaryFileForTest().delete()
    saveManager.backupFileForTest().delete()
    saveManager.startOrResumeGuestSession("Master Lokir")

    val nowSec = System.currentTimeMillis() / 1000L
    val roster = sampleSquadRoster(nowSec)
    val equip = sampleEquipment()

    saveManager.mutateSnapshot { snap ->
      snap.copy(
        profile = sampleMasterProfile(),
        heroes = roster,
        equipment = equip,
      )
    }
    val dataSource = LocalGuestDataSource(saveManager)

    // Check 6 & 7: Injured hero is NOT patrolling outdoors or dining in Restaurant, IS resting on a Dormitory bed, and cannot deploy
    val engine = SanctuaryLobbyEngine()
    engine.syncHeroes(roster, nowSec)
    assertFalse("Injured hero must not patrol outdoors", engine.heroActors.any { it.heroId == "h_injured" })

    engine.enterFacilityInterior(FacilityType.RESTAURANT)
    assertFalse("Injured hero must not dine in Restaurant", engine.heroActors.any { it.heroId == "h_injured" })
    engine.exitFacilityInterior()

    engine.enterFacilityInterior(FacilityType.HERO_QUARTERS)
    val dormInjuredActor = engine.heroActors.firstOrNull { it.heroId == "h_injured" }
    assertNotNull("Injured hero must appear resting on a Dorms bed", dormInjuredActor)
    assertTrue(dormInjuredActor!!.isInjured)
    engine.exitFacilityInterior()

    var blockedInjuredSquad = false
    try {
      dataSource.toggleHeroInSquad(roster.first { it.id == "h_injured" }, currentSquadSize = 2)
    } catch (_: IllegalArgumentException) {
      blockedInjuredSquad = true
    }
    assertTrue("Injured hero must be blocked from active squad deployment", blockedInjuredSquad)

    // Check 9: Live Equipment Stat-Diff Preview & Unequip + Specialized Training Focus + Multi-Recipe Meal
    val knightBefore = saveManager.guestSnapshot.value!!.heroes.first { it.id == "h_knight" }
    val upgradeSword = equip.first { it.id == "eq_sword_2" }
    val previewHero = knightBefore.previewWithCandidateItem(upgradeSword)
    val hpDelta = (previewHero.totalMaxHp(equip) - knightBefore.totalMaxHp(equip)).toLong()
    val atkDelta = (previewHero.totalAttack(equip) - knightBefore.totalAttack(equip)).toLong()
    val defDelta = (previewHero.totalDefense(equip) - knightBefore.totalDefense(equip)).toLong()
    val spdDelta = (previewHero.totalSpeed(equip) - knightBefore.totalSpeed(equip)).toLong()
    assertEquals(15L, hpDelta)
    assertEquals(13L, atkDelta)
    assertEquals(2L, defDelta)
    assertEquals(-2L, spdDelta)

    dataSource.equipItemToHero(knightBefore, upgradeSword, roster, equip)
    val knightEquipped = saveManager.guestSnapshot.value!!.heroes.first { it.id == "h_knight" }
    assertEquals("eq_sword_2", knightEquipped.equippedWeaponId)

    dataSource.unequipItemFromHero(knightEquipped, "WEAPON", saveManager.guestSnapshot.value!!.equipment)
    val knightUnequipped = saveManager.guestSnapshot.value!!.heroes.first { it.id == "h_knight" }
    assertEquals("", knightUnequipped.equippedWeaponId)

    dataSource.trainHeroWithFocus(
      profile = saveManager.guestSnapshot.value!!.profile,
      hero = knightUnequipped,
      focus = TrainingFocus.STRENGTH_ATK,
    )
    val knightTrained = saveManager.guestSnapshot.value!!.heroes.first { it.id == "h_knight" }
    assertEquals(knightUnequipped.level + 1L, knightTrained.level)
    assertTrue(knightTrained.attack >= knightUnequipped.attack + 5L + TrainingFocus.STRENGTH_ATK.atkBonus)

    dataSource.hostSanctuaryMealWithRecipe(
      profile = saveManager.guestSnapshot.value!!.profile,
      heroes = saveManager.guestSnapshot.value!!.heroes,
      recipe = SanctuaryMealRecipe.ROYAL_ASTRAL_BANQUET,
    )
    val postMealSnap = saveManager.guestSnapshot.value!!
    assertTrue(postMealSnap.profile.storyFlags.any { it.startsWith("MEAL_BANQUET_") })

    // Check 8 & 10: Compact Hero Card -> Full 5-Tab Hero Profile Modal works in UI
    val uiState = SummonerUiState(
      isGuestAccount = true,
      profile = postMealSnap.profile,
      heroes = postMealSnap.heroes,
      equipment = postMealSnap.equipment,
    )
    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      EternalSummonerTheme {
        SanctuaryLobbyScreen(
          state = uiState,
          onOpenFacility = {},
          onCloseFacility = {},
          onSummonHero = {},
          onTrainHero = { _, _ -> },
          onEquipItem = { _, _ -> },
          onSynthesizeGear = {},
          onEnhanceGear = {},
          onRecoverHero = { _, _ -> },
          onHostMeal = {},
          onToggleSquad = {},
          onCompletePrologue = {},
          onSignOut = {},
        )
      }
    }
    composeTestRule.mainClock.advanceTimeBy(64L)

    composeTestRule.onNodeWithTag("hero_chip_h_knight").performScrollTo().performClick()
    composeTestRule.mainClock.advanceTimeBy(64L)
    composeTestRule.onNodeWithTag("selected_hero_compact_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("open_hero_profile_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(64L)
    composeTestRule.onNodeWithTag("hero_full_profile_modal").assertIsDisplayed()

    composeTestRule.onNodeWithTag("hero_tab_stats").performScrollTo().performClick()
    composeTestRule.mainClock.advanceTimeBy(48L)
    composeTestRule.onNodeWithTag("hero_tab_equipment").performScrollTo().performClick()
    composeTestRule.mainClock.advanceTimeBy(48L)
    composeTestRule.onNodeWithTag("hero_tab_skills").performScrollTo().performClick()
    composeTestRule.mainClock.advanceTimeBy(48L)
    composeTestRule.onNodeWithTag("hero_tab_activity").performScrollTo().performClick()
    composeTestRule.mainClock.advanceTimeBy(48L)
    composeTestRule.onNodeWithTag("hero_tab_overview").performScrollTo().performClick()
    composeTestRule.mainClock.advanceTimeBy(48L)
    composeTestRule.onNodeWithTag("close_hero_profile_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(48L)
    }
  }

  @Test
  fun check11To13_islandExpansionPhysicalUnlock_walkability_andSaveMigrationZeroLoss() {
    runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val saveManager = LocalGuestSaveManager(context)
    saveManager.primaryFileForTest().delete()
    saveManager.backupFileForTest().delete()
    saveManager.startOrResumeGuestSession("Master Lokir")

    saveManager.mutateSnapshot { snap ->
      snap.copy(
        profile = sampleMasterProfile(),
        heroes = sampleSquadRoster(),
        equipment = sampleEquipment(),
      )
    }
    val dataSource = LocalGuestDataSource(saveManager)
    val sanctuaryObstacles = TowerFloorCatalog.buildSanctuaryMap().obstacles

    // Locked state: East Celestial Garden (x = 3320, y = 1265) is NOT walkable yet
    val clampedLocked = CollisionPhysics.resolvePosition(
      targetX = 3320f,
      targetY = 1265f,
      entityRadius = 18f,
      mapWidth = SanctuaryIslandGeometry.WORLD_WIDTH,
      mapHeight = SanctuaryIslandGeometry.WORLD_HEIGHT,
      marginWall = 120f,
      obstacles = sanctuaryObstacles,
      eastGardenUnlocked = false,
      northDockUnlocked = false,
    )
    assertTrue("Locked East Terrace must clamp back inside main island", clampedLocked.first < 2920f)

    // Unlock East Celestial Garden milestone
    val beforeGold = saveManager.guestSnapshot.value!!.profile.gold
    dataSource.unlockIslandExpansion(
      profile = saveManager.guestSnapshot.value!!.profile,
      milestone = IslandExpansionMilestone.EAST_CELESTIAL_GARDEN,
    )

    val unlockedSnap = saveManager.guestSnapshot.value!!
    val unlockedSet = SanctuaryProgressionHelper.unlockedExpansions(unlockedSnap.profile.storyFlags)
    assertTrue(IslandExpansionMilestone.EAST_CELESTIAL_GARDEN in unlockedSet)
    assertEquals(beforeGold - IslandExpansionMilestone.EAST_CELESTIAL_GARDEN.goldCost, unlockedSnap.profile.gold)
    assertTrue("Firestore storyFlags cap <= 20 must hold", unlockedSnap.profile.storyFlags.size <= 20)

    // Unlocked state: East Celestial Garden (x = 3320, y = 1265) is now physically walkable!
    val clampedUnlocked = CollisionPhysics.resolvePosition(
      targetX = 3320f,
      targetY = 1265f,
      entityRadius = 18f,
      mapWidth = SanctuaryIslandGeometry.EXPANDED_WORLD_WIDTH,
      mapHeight = SanctuaryIslandGeometry.WORLD_HEIGHT,
      marginWall = 120f,
      obstacles = sanctuaryObstacles,
      eastGardenUnlocked = true,
      northDockUnlocked = false,
    )
    assertEquals(3320f, clampedUnlocked.first, 5f)
    assertEquals(1265f, clampedUnlocked.second, 5f)

    // Reload via fresh LocalGuestSaveManager to prove disk persistence
    val reloadedManager = LocalGuestSaveManager(context)
    val reloadedFlags = reloadedManager.guestSnapshot.value!!.profile.storyFlags
    assertTrue(SanctuaryProgressionHelper.isExpansionUnlocked(reloadedFlags, IslandExpansionMilestone.EAST_CELESTIAL_GARDEN))

    // Check 13: Schema v1 legacy JSON save migrates cleanly to CURRENT_SAVE_SCHEMA_VERSION (v3) with zero loss
    val rawV1 = reloadedManager.primaryFileForTest().readText()
      .replace("\"schemaVersion\":${GuestSaveSnapshot.CURRENT_SAVE_SCHEMA_VERSION}", "\"schemaVersion\":1")
      .replace("\"schemaVersion\": ${GuestSaveSnapshot.CURRENT_SAVE_SCHEMA_VERSION}", "\"schemaVersion\": 1")
    reloadedManager.primaryFileForTest().writeText(rawV1)

    val migratedManager = LocalGuestSaveManager(context)
    val migratedSnap = migratedManager.startOrResumeGuestSession("Master Lokir")
    assertEquals(GuestSaveSnapshot.CURRENT_SAVE_SCHEMA_VERSION, migratedSnap.schemaVersion)
    assertEquals(3, migratedSnap.heroes.size)
    assertEquals(2, migratedSnap.equipment.size)
    assertTrue(
      SanctuaryProgressionHelper.isExpansionUnlocked(
        migratedSnap.profile.storyFlags,
        IslandExpansionMilestone.EAST_CELESTIAL_GARDEN,
      )
    )
    }
  }

  @Test
  fun check14To20_floor1VerdantSteppeDiscoveries_enemyAi_tacticalCommands_andThreeWaySettlement() {
    runBlocking {
    val floor01 = TowerFloorCatalog.FLOORS.first { it.floorNumber == 1 }
    val healthySquad = sampleSquadRoster().filter { !it.isCurrentlyInjured() }
    val equip = sampleEquipment()

    val engine = TowerCombatEngine(
      floorSpec = floor01,
      squadRecords = healthySquad,
      equipment = equip,
      feastBuffActive = true,
      activeMealRecipe = SanctuaryMealRecipe.HERB_RATION_SOUP,
    )
    engine.stage = MissionStage.IN_PROGRESS

    // Check 14: Floor 1 Verdant Steppe optional discoveries (North Rune Chest & South Herb Shrine)
    assertEquals(2, engine.discoveryNodes.size)
    val northChest = engine.discoveryNodes.first { it.id == "f1_chest_north" }
    val southShrine = engine.discoveryNodes.first { it.id == "f1_shrine_south" }

    engine.masterX = northChest.x - 30f
    engine.masterY = northChest.y
    engine.onBattlefieldTapped(northChest.x, northChest.y)
    assertTrue(engine.isDiscoveryClaimed(northChest.obstacleId))
    assertEquals(65L, engine.bonusDiscoveryGold)

    engine.heroes.first().hp = 120
    engine.masterX = southShrine.x - 30f
    engine.masterY = southShrine.y
    engine.onBattlefieldTapped(southShrine.x, southShrine.y)
    assertTrue(engine.isDiscoveryClaimed(southShrine.obstacleId))
    assertTrue("Herb shrine must heal damaged squad members", engine.heroes.first().hp > 120)
    assertEquals(2, engine.discoveredNodeIds().size)

    // Check 15: Differentiated Enemy AI — (a) Goblin Archer kites backward at close range
    engine.enemies.clear()
    engine.heroes.first().apply {
      x = 700f
      y = 520f
    }
    engine.heroes[1].apply {
      x = 500f
      y = 520f
    }
    engine.masterX = 460f
    engine.masterY = 520f

    val kitingArcher = EnemyCombatUnit(
      id = "test_archer",
      archetype = EnemyArchetype.FIELD_GOBLIN_ARCHER,
      maxHp = 220,
      hp = 220,
      attack = 24,
      defense = 8,
      speed = 115f,
      x = 765f,
      y = 520f,
    ).apply {
      aggroed = true
    }
    engine.enemies += kitingArcher
    val archerStartX = kitingArcher.x
    engine.step(0.25f, 0f, 0f)
    assertTrue("Goblin Archer must kite away from close Knight", kitingArcher.x > archerStartX)

    // (b) Dire Wolf flanks backline Mage over equidistant Knight
    engine.enemies.clear()
    val knight = engine.heroes.first { it.heroClass == HeroClassType.KNIGHT }
    val mage = engine.heroes.first { it.heroClass == HeroClassType.MAGE }
    knight.x = 700f
    knight.y = 430f
    mage.x = 700f
    mage.y = 610f
    engine.masterX = 350f
    engine.masterY = 520f

    val flankerWolf = EnemyCombatUnit(
      id = "test_wolf",
      archetype = EnemyArchetype.DIRE_WOLF,
      maxHp = 260,
      hp = 260,
      attack = 30,
      defense = 9,
      speed = 140f,
      x = 820f,
      y = 520f,
    ).apply {
      aggroed = true
    }
    engine.enemies += flankerWolf
    engine.step(0.25f, 0f, 0f)
    assertTrue("Dire Wolf must flank toward backline Mage (y > 520)", flankerWolf.y > 520f)

    // (c) Goblin War Captain Boss triggers Warcry buffing nearby allies
    engine.enemies.clear()
    val boss = EnemyCombatUnit(
      id = "test_boss",
      archetype = EnemyArchetype.GOBLIN_WAR_CAPTAIN,
      maxHp = 950,
      hp = 950,
      attack = 52,
      defense = 22,
      speed = 96f,
      x = 860f,
      y = 520f,
    ).apply {
      aggroed = true
      warcryCooldown = 0f
    }
    val raiderAlly = EnemyCombatUnit(
      id = "test_raider",
      archetype = EnemyArchetype.FIELD_GOBLIN_RAIDER,
      maxHp = 280,
      hp = 280,
      attack = 26,
      defense = 12,
      speed = 104f,
      x = 910f,
      y = 535f,
    ).apply {
      aggroed = false
    }
    engine.enemies += boss
    engine.enemies += raiderAlly
    engine.step(0.15f, 0f, 0f)
    assertTrue("Boss Warcry must buff nearby Goblin ally", raiderAlly.atkBuffTimer > 0f && raiderAlly.aggroed)

    // Check 16, 17, 18: Tactical Orders & Explicit Manual Skill Cooldown Feedback
    engine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE)
    assertEquals(TacticalOrder.FOCUS_FIRE, engine.activeOrder)
    assertNotNull(engine.designatedTargetId)
    assertTrue(engine.commandFeedback.orEmpty().isNotBlank())

    engine.issueTacticalOrder(TacticalOrder.HOLD_POSITION)
    assertEquals(TacticalOrder.HOLD_POSITION, engine.activeOrder)

    engine.issueTacticalOrder(TacticalOrder.REGROUP)
    assertEquals(TacticalOrder.REGROUP, engine.activeOrder)

    engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
    assertTrue(engine.awaitingMoveToTap)
    engine.onBattlefieldTapped(920f, 540f)
    assertNotNull(engine.moveBeacon)
    assertEquals(TacticalOrder.MOVE_TO, engine.activeOrder)

    engine.triggerHeroManualSkill(knight.id)
    assertTrue(knight.manualCommandCooldown > 7f)
    engine.triggerHeroManualSkill(knight.id)
    assertTrue(
      "Re-triggering manual skill on cooldown must post explicit feedback",
      engine.commandFeedback.orEmpty().contains("đang hồi"),
    )

    // Check 19 & 20: 3-Way Expedition Settlement (Tactical Retreat vs Defeat vs Victory)
    val context = ApplicationProvider.getApplicationContext<Context>()
    val saveManager = LocalGuestSaveManager(context)
    saveManager.primaryFileForTest().delete()
    saveManager.backupFileForTest().delete()
    saveManager.startOrResumeGuestSession("Master Lokir")
    saveManager.mutateSnapshot { snap ->
      snap.copy(
        profile = sampleMasterProfile().copy(highestClearedFloor = 0L),
        heroes = healthySquad,
        equipment = equip,
      )
    }
    val dataSource = LocalGuestDataSource(saveManager)

    // 1) Tactical Retreat protects standing heroes from injury
    engine.confirmTacticalRetreat()
    assertTrue(engine.retreatCompleted)
    val retreatRes = dataSource.settleExpedition(
      profile = saveManager.guestSnapshot.value!!.profile,
      squadHeroes = saveManager.guestSnapshot.value!!.heroes,
      fallenHeroIds = emptySet(),
      floorNumber = 1,
      victory = false,
      retreatUsed = true,
      bonusGold = engine.bonusDiscoveryGold,
      bonusMana = engine.bonusDiscoveryMana,
      discoveredNodeIds = engine.discoveredNodeIds(),
    )
    assertEquals(ExpeditionOutcomeType.TACTICAL_RETREAT, retreatRes.outcomeType)
    assertTrue("Tactical retreat must keep standing heroes uninjured", retreatRes.injuredHeroNames.isEmpty())

    // 2) Defeat sends wiped squad to Dorms recovery
    val defeatRes = dataSource.settleExpedition(
      profile = saveManager.guestSnapshot.value!!.profile,
      squadHeroes = saveManager.guestSnapshot.value!!.heroes,
      fallenHeroIds = healthySquad.map { it.id }.toSet(),
      floorNumber = 1,
      victory = false,
      retreatUsed = false,
    )
    assertEquals(ExpeditionOutcomeType.DEFEAT, defeatRes.outcomeType)
    assertEquals(healthySquad.size, defeatRes.injuredHeroNames.size)
    assertTrue(saveManager.guestSnapshot.value!!.heroes.all { it.status == HeroStatusType.INJURED.name })

    // Recover heroes and verify 3) Victory settlement advances floor & grants loot
    saveManager.guestSnapshot.value!!.heroes.forEach { h ->
      dataSource.recoverInjuredHero(saveManager.guestSnapshot.value!!.profile, h, instantWithResources = true)
    }
    val victoryRes = dataSource.settleExpedition(
      profile = saveManager.guestSnapshot.value!!.profile,
      squadHeroes = saveManager.guestSnapshot.value!!.heroes,
      fallenHeroIds = emptySet(),
      floorNumber = 1,
      victory = true,
      retreatUsed = false,
      bonusGold = 65L,
      bonusMana = 35L,
      discoveredNodeIds = setOf("f1_chest_north", "f1_shrine_south"),
    )
    assertEquals(ExpeditionOutcomeType.VICTORY, victoryRes.outcomeType)
    assertNotNull(victoryRes.lootedItemName)
    assertEquals(1L, saveManager.guestSnapshot.value!!.profile.highestClearedFloor)
    assertTrue(abs(victoryRes.discoveredNodesCount - 2) == 0)
    }
  }
}
