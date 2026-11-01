package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.example.data.GameSettings
import com.example.data.SessionAccountMode
import com.example.data.model.EquipmentRecord
import com.example.data.model.FacilityType
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.MasterProfile
import com.example.data.model.PersonalityType
import com.example.game.map.CollisionPhysics
import com.example.game.map.EnemyArchetype
import com.example.game.map.ObliqueCamera
import com.example.game.pixel.PixelCharacterAtlas
import com.example.game.pixel.PixelStructureAtlas
import com.example.game.pixel.PixelTileAtlas
import com.example.game.story.ManhwaStoryData
import com.example.game.tower.TowerFloorCatalog
import com.example.ui.components.SettingsAndAccountModal
import com.example.ui.screens.AuthGateScreen
import com.example.ui.screens.FloorSelectionScreen
import com.example.ui.screens.SanctuaryFacilityModalOverlay
import com.example.ui.screens.SanctuaryLobbyScreen
import com.example.ui.screens.SquadPreparationScreen
import com.example.ui.screens.TowerBattleScreen
import com.example.ui.theme.EternalSummonerTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayoutResponsivenessRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private fun buildRichSampleState(activeFacility: FacilityType? = null): SummonerUiState {
    val profile = MasterProfile(
      id = "u_master_test",
      masterName = "Thống Lĩnh Huyền Thoại Lokir",
      masterLevel = 12,
      highestClearedFloor = 1,
      gold = 14850,
      manaCrystals = 640,
      soulShards = 125,
      ironOre = 42,
      arcaneEssence = 19,
      rations = 16,
      storyFlags = listOf(ManhwaStoryData.FLAG_PROLOGUE_SEEN),
    )

    val heroes = listOf(
      HeroRecord(
        id = "h1",
        name = "Kaelen Vệ Thần",
        title = "Khiên Thép Bất Diệt",
        heroClass = HeroClassType.KNIGHT.name,
        rarity = 4,
        level = 7,
        maxHp = 620,
        attack = 68,
        defense = 42,
        speed = 108,
        personality = PersonalityType.STOIC.name,
        morale = 96,
        inActiveSquad = true,
      ),
      HeroRecord(
        id = "h2",
        name = "Lyra Phong Tiễn",
        title = "Mắt Ưng Tinh Hệ",
        heroClass = HeroClassType.ARCHER.name,
        rarity = 4,
        level = 6,
        maxHp = 410,
        attack = 82,
        defense = 24,
        speed = 136,
        personality = PersonalityType.CYNICAL.name,
        morale = 88,
        inActiveSquad = true,
      ),
      HeroRecord(
        id = "h3",
        name = "Vesper Huyền Tinh",
        title = "Pháp Sư Hư Không",
        heroClass = HeroClassType.MAGE.name,
        rarity = 5,
        level = 8,
        maxHp = 390,
        attack = 96,
        defense = 22,
        speed = 116,
        personality = PersonalityType.CALCULATING.name,
        morale = 100,
        inActiveSquad = true,
      ),
      HeroRecord(
        id = "h4",
        name = "Aurelia Thánh Quang",
        title = "Nữ Tư Tế Ánh Sao",
        heroClass = HeroClassType.PRIEST.name,
        rarity = 4,
        level = 6,
        maxHp = 440,
        attack = 58,
        defense = 30,
        speed = 114,
        personality = PersonalityType.DEVOTED.name,
        morale = 94,
        inActiveSquad = true,
      ),
    )

    val equipment = listOf(
      EquipmentRecord(
        id = "eq1",
        name = "Trường Kiếm Hắc Tinh Cổ Đại",
        slot = "WEAPON",
        rarity = 4,
        level = 3,
        bonusAttack = 24,
        bonusHp = 60,
        bonusDefense = 8,
        equippedByHeroId = "h1",
      ),
      EquipmentRecord(
        id = "eq2",
        name = "Giáp Hộ Tâm Thánh Điện",
        slot = "ARMOR",
        rarity = 3,
        level = 2,
        bonusAttack = 6,
        bonusHp = 110,
        bonusDefense = 18,
      ),
    )

    return SummonerUiState(
      isBootstrapping = false,
      profile = profile,
      heroes = heroes,
      equipment = equipment,
      selectedFloor = TowerFloorCatalog.FLOORS.first(),
      activeFacilityModal = activeFacility,
    )
  }

  private fun assertInsideViewport(label: String, nodeRect: Rect, rootRect: Rect) {
    assertTrue("$label should have positive width (${nodeRect.width})", nodeRect.width > 1f)
    assertTrue("$label should have positive height (${nodeRect.height})", nodeRect.height > 1f)
    assertTrue("$label left (${nodeRect.left}) clipped outside viewport", nodeRect.left >= -2f)
    assertTrue("$label top (${nodeRect.top}) clipped outside viewport", nodeRect.top >= -2f)
    assertTrue(
      "$label right (${nodeRect.right}) overflows viewport width (${rootRect.width})",
      nodeRect.right <= rootRect.width + 2f,
    )
    assertTrue(
      "$label bottom (${nodeRect.bottom}) overflows viewport height (${rootRect.height})",
      nodeRect.bottom <= rootRect.height + 2f,
    )
  }

  private fun rectsOverlap(a: Rect, b: Rect): Boolean {
    val horizOverlap = a.left < b.right - 1f && a.right > b.left + 1f
    val vertOverlap = a.top < b.bottom - 1f && a.bottom > b.top + 1f
    return horizOverlap && vertOverlap
  }

  @Test
  fun `pixel tile and monumental structure atlases generate crisp pixel art sprites`() {
    assertTrue(PixelTileAtlas.sanctuaryStoneTiles.size >= 6)
    assertTrue(PixelTileAtlas.meadowGrassTiles.size >= 8)
    assertTrue(PixelTileAtlas.sanctuaryStoneTiles.all { it.width == PixelTileAtlas.TILE_PX && it.height == PixelTileAtlas.TILE_PX })
    assertTrue(PixelTileAtlas.meadowGrassTiles.all { it.width == PixelTileAtlas.TILE_PX && it.height == PixelTileAtlas.TILE_PX })
    assertEquals(224, PixelTileAtlas.astrolabePlazaMedallion.width)
    assertEquals(152, PixelTileAtlas.astrolabePlazaMedallion.height)

    assertTrue(PixelStructureAtlas.towerGateMonument.width >= 240)
    assertTrue(PixelStructureAtlas.summoningHallStructure.width >= 180)
    assertTrue(PixelStructureAtlas.heroRegistryStructure.width >= 180)
    assertTrue(PixelStructureAtlas.floorVistaArchStructure.width >= 150)
    assertTrue(PixelStructureAtlas.trainingGroundStructure.width >= 180)
    assertTrue(PixelStructureAtlas.armoryStructure.width >= 180)
    assertTrue(PixelStructureAtlas.synthesisLabStructure.width >= 180)
    assertTrue(PixelStructureAtlas.heroQuartersStructure.width >= 190)
    assertTrue(PixelStructureAtlas.restaurantStructure.width >= 200)
    assertTrue(PixelStructureAtlas.medievalTownhouseA.width >= 140)
    assertTrue(PixelStructureAtlas.terraceCliffWallSprite.width >= 150)
    assertTrue(PixelStructureAtlas.terraceGrandStairsSprite.width >= 110)
    assertTrue(PixelStructureAtlas.stoneBalustradeSprite.width >= 100)
    assertTrue(PixelStructureAtlas.tavernCounterSprite.width >= 140)
    assertTrue(PixelStructureAtlas.oakTreeSprite.width >= 105)
    assertTrue(PixelStructureAtlas.mysticPlumTreeSprite.width >= 100)
    assertTrue(PixelStructureAtlas.ruinArchSprite.width >= 80)
    assertTrue(PixelStructureAtlas.brazierFrames.size == 3)
    assertTrue(PixelStructureAtlas.tavernFireplaceFrames.size == 3)

    // Verify 38x50 3/4 multi-frame directional pixel character & monster atlases (including back-3/4!)
    assertEquals(38, PixelCharacterAtlas.masterSprites.idleRight.first().width)
    assertEquals(50, PixelCharacterAtlas.masterSprites.idleRight.first().height)
    assertEquals(4, PixelCharacterAtlas.masterSprites.walkRight.size)
    assertEquals(4, PixelCharacterAtlas.masterSprites.walkBackRight.size)
    HeroClassType.entries.forEach { cls ->
      val sheet = PixelCharacterAtlas.heroSheet(cls)
      assertEquals(38, sheet.idleRight.first().width)
      assertEquals(50, sheet.idleRight.first().height)
      assertTrue(sheet.idleRight.size >= 2)
      assertEquals(4, sheet.walkRight.size)
      assertEquals(4, sheet.walkBackRight.size)
      assertTrue(sheet.attackRight.size >= 2)
    }
    EnemyArchetype.entries.forEach { arch ->
      val sheet = PixelCharacterAtlas.enemySheet(arch)
      assertEquals(4, sheet.walkRight.size)
      assertTrue(sheet.attackRight.size >= 2)
      if (arch.isBoss) {
        assertEquals(56, sheet.idleRight.first().width)
        assertEquals(62, sheet.idleRight.first().height)
      }
    }

    // Verify ObliqueCamera round-trip projection & 2.5D walk-behind building collision
    val wx = 1345f
    val wy = 910f
    val sx = ObliqueCamera.worldToScreenX(wx, 1140f, 0.88f, 800f)
    val sy = ObliqueCamera.worldToScreenY(wy, 740f, 0.88f, 360f)
    assertEquals(wx, ObliqueCamera.screenToWorldX(sx, 1140f, 0.88f, 800f), 0.05f)
    assertEquals(wy, ObliqueCamera.screenToWorldY(sy, 740f, 0.88f, 360f), 0.05f)

    val layout = TowerFloorCatalog.buildSanctuaryMap()
    val registry = layout.obstacles.first { it.id == "building_hero_registry" }
    // Walking behind the rooftop along the northern terrace avenue (y < registry.y - footprintDepthNorth) is unobstructed!
    val behindY = registry.y - registry.footprintDepthNorth - 28f
    val (resX, resY) = CollisionPhysics.resolvePosition(
      targetX = registry.x,
      targetY = behindY,
      entityRadius = 18f,
      mapWidth = layout.width,
      mapHeight = layout.height,
      marginWall = layout.wallMargin,
      obstacles = layout.obstacles,
    )
    assertTrue("Character must be able to walk behind 3/4 building roof (resY=$resY < registry.y=${registry.y})", resY < registry.y - registry.footprintDepthNorth)
    assertEquals(registry.x, resX, 1f)
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun `login gate renders one-tap guest button google sign-in and settings cleanly`() {
    var guestClicked = false
    var settingsClicked = false
    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      EternalSummonerTheme {
        AuthGateScreen(
          hasExistingGuestSave = true,
          savedGuestSummary = "Master Kael (Lv.4) • Tầng 1",
          allowSilentAutoSignIn = false,
          onContinueAsGuest = { guestClicked = true },
          onAuthenticated = {},
          onOpenSettings = { settingsClicked = true },
        )
      }
    }
    composeTestRule.mainClock.advanceTimeBy(64L)

    val rootBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
    val guestBtnBounds = composeTestRule.onNodeWithTag("continue_as_guest_button").fetchSemanticsNode().boundsInRoot
    val googleBtnBounds = composeTestRule.onNodeWithTag("google_sign_in_button").fetchSemanticsNode().boundsInRoot
    val settingsBtnBounds = composeTestRule.onNodeWithTag("login_settings_button").fetchSemanticsNode().boundsInRoot

    assertInsideViewport("Continue as Guest Button", guestBtnBounds, rootBounds)
    assertInsideViewport("Google Sign-In Button", googleBtnBounds, rootBounds)
    assertInsideViewport("Login Settings Button", settingsBtnBounds, rootBounds)
    assertFalse("Guest and Google buttons must not overlap", rectsOverlap(guestBtnBounds, googleBtnBounds))

    composeTestRule.onNodeWithTag("continue_as_guest_button").performClick()
    composeTestRule.onNodeWithTag("login_settings_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(32L)
    assertTrue("One-tap Continue as Guest must fire immediately", guestClicked)
    assertTrue("Login Settings button must fire immediately", settingsClicked)
  }

  @Test
  @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun `playable restaurant interior entry exit and settings account modal render cleanly`() {
    val state = buildRichSampleState().copy(isGuestAccount = true)
    var showSettings by mutableStateOf(false)
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
          onOpenSettings = { showSettings = true },
        )
        if (showSettings) {
          SettingsAndAccountModal(
            accountMode = SessionAccountMode.GUEST_LOCAL,
            accountSummary = "ID: guest_test • Tầng 1",
            settings = GameSettings(),
            onUpdateSettings = {},
            onClose = { showSettings = false },
            onLinkGoogleAccount = {},
            onLogoutOrSwitchAccount = {},
          )
        }
      }
    }
    composeTestRule.mainClock.advanceTimeBy(64L)

    // Enter Playable Restaurant Interior!
    composeTestRule.onNodeWithTag("enter_restaurant_interior_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(64L)

    val rootBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
    val exitRestaurantBounds = composeTestRule.onNodeWithTag("exit_restaurant_interior_button").fetchSemanticsNode().boundsInRoot
    val cameraBarBounds = composeTestRule.onNodeWithTag("camera_controls_bar").fetchSemanticsNode().boundsInRoot
    assertInsideViewport("Exit Restaurant Interior Button", exitRestaurantBounds, rootBounds)
    assertFalse("Exit Restaurant button must not overlap Camera Bar", rectsOverlap(exitRestaurantBounds, cameraBarBounds))

    // Open Settings & Account Modal
    composeTestRule.onNodeWithTag("open_settings_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(64L)

    val linkBtnBounds = composeTestRule.onNodeWithTag("link_google_account_button").fetchSemanticsNode().boundsInRoot
    assertInsideViewport("Link Google Account Button", linkBtnBounds, rootBounds)

    composeTestRule.onNodeWithTag("close_settings_modal_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(48L)

    // Exit Restaurant Interior back to Sanctuary Street
    composeTestRule.onNodeWithTag("exit_restaurant_interior_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(64L)
    val enterAgainBounds = composeTestRule.onNodeWithTag("enter_restaurant_interior_button").fetchSemanticsNode().boundsInRoot
    assertInsideViewport("Re-rendered Enter Restaurant Button", enterAgainBounds, rootBounds)
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun `narrow portrait 360x640 sanctuary lobby has slim HUD and non-overlapping controls`() {
    val state = buildRichSampleState()
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
    composeTestRule.mainClock.advanceTimeBy(80L)

    val rootBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
    val topHudBounds = composeTestRule.onNodeWithTag("sanctuary_top_hud").fetchSemanticsNode().boundsInRoot
    val cameraBarBounds = composeTestRule.onNodeWithTag("camera_controls_bar").fetchSemanticsNode().boundsInRoot
    val interactBtnBounds = composeTestRule.onNodeWithTag("interact_facility_button").fetchSemanticsNode().boundsInRoot
    val openSettingsBounds = composeTestRule.onNodeWithTag("open_settings_button").fetchSemanticsNode().boundsInRoot

    assertInsideViewport("Narrow Portrait Top HUD", topHudBounds, rootBounds)
    assertInsideViewport("Narrow Portrait Camera Controls Bar", cameraBarBounds, rootBounds)
    assertInsideViewport("Narrow Portrait Interact Button", interactBtnBounds, rootBounds)
    assertInsideViewport("Narrow Portrait Settings Button", openSettingsBounds, rootBounds)

    assertTrue(
      "Top HUD height (${topHudBounds.height}) must never cover most of the game world (${rootBounds.height})",
      topHudBounds.height < rootBounds.height * 0.26f,
    )
    assertFalse("Camera Bar must not overlap Top HUD", rectsOverlap(cameraBarBounds, topHudBounds))
    assertFalse("Camera Bar must not overlap Interact Facility Button", rectsOverlap(cameraBarBounds, interactBtnBounds))
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun `narrow portrait 360x640 tower battle HUD and 8 tactical orders never overlap joystick`() {
    val state = buildRichSampleState()
    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      EternalSummonerTheme {
        TowerBattleScreen(
          state = state,
          onFinishExpedition = { _, _ -> },
          onReturnToSanctuary = {},
        )
      }
    }
    composeTestRule.mainClock.advanceTimeBy(64L)

    // Advance past intro manhwa dialogue beats to inspect active real-time combat HUD
    repeat(3) {
      val startNodes = composeTestRule.onAllNodes(hasTestTag("start_combat_button")).fetchSemanticsNodes()
      if (startNodes.isNotEmpty()) {
        composeTestRule.onNodeWithTag("start_combat_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(48L)
      }
    }

    val rootBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
    val topHudBounds = composeTestRule.onNodeWithTag("tower_battle_top_hud").fetchSemanticsNode().boundsInRoot
    val minimapBounds = composeTestRule.onNodeWithTag("tactical_minimap").fetchSemanticsNode().boundsInRoot
    val retreatBounds = composeTestRule.onNodeWithTag("retreat_mission_button").fetchSemanticsNode().boundsInRoot
    val joystickBounds = composeTestRule.onNodeWithTag("virtual_joystick").fetchSemanticsNode().boundsInRoot
    val followOrderBounds = composeTestRule.onNodeWithTag("order_follow").fetchSemanticsNode().boundsInRoot

    assertInsideViewport("Narrow Portrait Battle Top HUD", topHudBounds, rootBounds)
    assertInsideViewport("Narrow Portrait Minimap", minimapBounds, rootBounds)
    assertInsideViewport("Narrow Portrait Retreat Button", retreatBounds, rootBounds)
    assertInsideViewport("Narrow Portrait Battle Joystick", joystickBounds, rootBounds)
    assertInsideViewport("Narrow Portrait Follow Order Chip", followOrderBounds, rootBounds)

    assertTrue(
      "Battle Top HUD height (${topHudBounds.height}) must remain compact (<32% of ${rootBounds.height})",
      topHudBounds.height < rootBounds.height * 0.32f,
    )
    assertFalse("Battle Joystick must not overlap Top HUD", rectsOverlap(joystickBounds, topHudBounds))
    assertFalse("Battle Joystick must not overlap Tactical Order Strip", rectsOverlap(joystickBounds, followOrderBounds))
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun `narrow portrait 360x640 floor selection squad prep and facility modals fit cleanly`() {
    val state = buildRichSampleState()
    composeTestRule.mainClock.autoAdvance = false
    var currentScreen by mutableIntStateOf(0)

    composeTestRule.setContent {
      EternalSummonerTheme {
        when (currentScreen) {
          0 -> FloorSelectionScreen(
            state = state,
            onSelectFloor = { currentScreen = 1 },
            onBackToLobby = {},
          )
          1 -> SquadPreparationScreen(
            state = state,
            onToggleHero = {},
            onOpenFacility = {},
            onCloseFacility = {},
            onSummonHero = {},
            onTrainHero = { _, _ -> },
            onEquipItem = { _, _ -> },
            onSynthesizeGear = {},
            onEnhanceGear = {},
            onRecoverHero = { _, _ -> },
            onHostMeal = {},
            onDeploy = { currentScreen = 2 },
            onBack = {},
          )
          else -> SanctuaryFacilityModalOverlay(
            facility = FacilityType.SUMMONING_HALL,
            state = state,
            onClose = {},
            onSummonHero = {},
            onTrainHero = { _, _ -> },
            onEquipItem = { _, _ -> },
            onSynthesizeGear = {},
            onEnhanceGear = {},
            onRecoverHero = { _, _ -> },
            onHostMeal = {},
            onToggleSquad = {},
          )
        }
      }
    }
    composeTestRule.mainClock.advanceTimeBy(64L)

    val rootBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
    val floor1Btn = composeTestRule.onNodeWithTag("enter_floor_1_button").fetchSemanticsNode().boundsInRoot
    assertInsideViewport("Narrow Portrait Floor 01 Button", floor1Btn, rootBounds)

    composeTestRule.onNodeWithTag("enter_floor_1_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(64L)

    val deployBtn = composeTestRule.onNodeWithTag("deploy_tower_button").fetchSemanticsNode().boundsInRoot
    assertInsideViewport("Narrow Portrait Deploy Squad Button", deployBtn, rootBounds)

    composeTestRule.onNodeWithTag("deploy_tower_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(64L)

    val summonBtn = composeTestRule.onNodeWithTag("summon_ritual_button").fetchSemanticsNode().boundsInRoot
    val closeBtn = composeTestRule.onNodeWithTag("close_facility_modal_button").fetchSemanticsNode().boundsInRoot
    assertInsideViewport("Narrow Portrait Summon Ritual Button", summonBtn, rootBounds)
    assertInsideViewport("Narrow Portrait Close Modal Button", closeBtn, rootBounds)
  }

  @Test
  @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun `widescreen landscape 800x360 sanctuary and tower battle HUDs stay compact and collision-free`() {
    val state = buildRichSampleState()
    composeTestRule.mainClock.autoAdvance = false
    var inBattle by mutableStateOf(false)

    composeTestRule.setContent {
      EternalSummonerTheme {
        if (!inBattle) {
          SanctuaryLobbyScreen(
            state = state,
            onOpenFacility = { inBattle = true },
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
        } else {
          TowerBattleScreen(
            state = state,
            onFinishExpedition = { _, _ -> },
            onReturnToSanctuary = {},
          )
        }
      }
    }
    composeTestRule.mainClock.advanceTimeBy(64L)

    val rootBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
    val lobbyHud = composeTestRule.onNodeWithTag("sanctuary_top_hud").fetchSemanticsNode().boundsInRoot
    val lobbyCamBar = composeTestRule.onNodeWithTag("camera_controls_bar").fetchSemanticsNode().boundsInRoot
    val lobbyInteract = composeTestRule.onNodeWithTag("interact_facility_button").fetchSemanticsNode().boundsInRoot

    assertInsideViewport("Landscape Lobby HUD", lobbyHud, rootBounds)
    assertInsideViewport("Landscape Lobby Camera Bar", lobbyCamBar, rootBounds)
    assertInsideViewport("Landscape Lobby Interact", lobbyInteract, rootBounds)
    assertTrue(
      "Landscape Lobby HUD height (${lobbyHud.height}) must leave >72% of viewport (${rootBounds.height}) visible",
      lobbyHud.height < rootBounds.height * 0.28f,
    )
    assertFalse("Landscape Lobby Camera Bar must not overlap Top HUD", rectsOverlap(lobbyCamBar, lobbyHud))
    assertFalse("Landscape Lobby Camera Bar must not overlap Interact Button", rectsOverlap(lobbyCamBar, lobbyInteract))

    // Switch to Widescreen Tower Battle
    composeTestRule.onNodeWithTag("interact_facility_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(64L)

    repeat(3) {
      val startNodes = composeTestRule.onAllNodes(hasTestTag("start_combat_button")).fetchSemanticsNodes()
      if (startNodes.isNotEmpty()) {
        composeTestRule.onNodeWithTag("start_combat_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(48L)
      }
    }

    val battleHud = composeTestRule.onNodeWithTag("tower_battle_top_hud").fetchSemanticsNode().boundsInRoot
    val battleJoy = composeTestRule.onNodeWithTag("virtual_joystick").fetchSemanticsNode().boundsInRoot
    val battleOrder = composeTestRule.onNodeWithTag("order_follow").fetchSemanticsNode().boundsInRoot

    assertInsideViewport("Landscape Battle HUD", battleHud, rootBounds)
    assertInsideViewport("Landscape Battle Joystick", battleJoy, rootBounds)
    assertInsideViewport("Landscape Battle Follow Order", battleOrder, rootBounds)
    assertTrue(
      "Landscape Battle HUD height (${battleHud.height}) must remain slim (<30% of ${rootBounds.height})",
      battleHud.height < rootBounds.height * 0.30f,
    )
    assertFalse("Landscape Battle Joystick must not overlap Battle Top HUD", rectsOverlap(battleJoy, battleHud))
    assertFalse("Landscape Battle Joystick must not overlap Tactical Orders", rectsOverlap(battleJoy, battleOrder))
  }

  private fun saveViewScreenshot(view: View, outPath: String) {
    val w = view.width.coerceAtLeast(1)
    val h = view.height.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    view.draw(canvas)
    val outFile = File(outPath)
    outFile.parentFile?.mkdirs()
    FileOutputStream(outFile).use { fos ->
      bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }
  }

  @Test
  @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun `render widescreen oblique 3_4 sanctuary restaurant and floor01 screenshots`() {
    val artifactDir = "build/visual_screenshots"
    val state = buildRichSampleState()
    composeTestRule.mainClock.autoAdvance = false
    var inBattle by mutableStateOf(false)
    var hostView: View? = null

    composeTestRule.setContent {
      hostView = LocalView.current
      EternalSummonerTheme {
        if (!inBattle) {
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
        } else {
          TowerBattleScreen(
            state = state,
            onFinishExpedition = { _, _ -> },
            onReturnToSanctuary = {},
          )
        }
      }
    }
    composeTestRule.mainClock.advanceTimeBy(160L)
    composeTestRule.waitForIdle()
    hostView?.let { saveViewScreenshot(it, "$artifactDir/sanctuary_34_exterior.png") }

    // Toggle camera_reset_button to frame the FULL 8-sided Floating Sanctuary Island Overview!
    composeTestRule.onNodeWithTag("camera_reset_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(160L)
    composeTestRule.waitForIdle()
    hostView?.let { saveViewScreenshot(it, "$artifactDir/sanctuary_full_island_overview.png") }

    // Toggle back to gameplay zoom and enter Playable 3/4 Restaurant Interior!
    composeTestRule.onNodeWithTag("camera_reset_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(80L)
    composeTestRule.onNodeWithTag("enter_restaurant_interior_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(160L)
    composeTestRule.waitForIdle()
    hostView?.let { saveViewScreenshot(it, "$artifactDir/restaurant_34_interior.png") }

    // Switch to Floor 01 3/4 Forest Battlefield, advance past intro dialogue, and capture!
    inBattle = true
    composeTestRule.mainClock.advanceTimeBy(96L)
    repeat(3) {
      val startNodes = composeTestRule.onAllNodes(hasTestTag("start_combat_button")).fetchSemanticsNodes()
      if (startNodes.isNotEmpty()) {
        composeTestRule.onNodeWithTag("start_combat_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(64L)
      }
    }
    // Step combat for ~2.5s so squad engages scouts along the 3/4 stone forest road!
    repeat(50) {
      composeTestRule.mainClock.advanceTimeBy(48L)
    }
    composeTestRule.waitForIdle()
    hostView?.let { saveViewScreenshot(it, "$artifactDir/floor01_34_battle.png") }

    assertTrue("TowerFloorCatalog must contain 10 playable floors", com.example.game.tower.TowerFloorCatalog.FLOORS.size == 10)
    assertTrue(File("$artifactDir/sanctuary_34_exterior.png").exists())
    assertTrue(File("$artifactDir/sanctuary_full_island_overview.png").exists())
    assertTrue(File("$artifactDir/restaurant_34_interior.png").exists())
    assertTrue(File("$artifactDir/floor01_34_battle.png").exists())
  }

  @Test
  @Config(qualifiers = "w960dp-h720dp-land-xhdpi")
  fun `render high resolution 4_3 full sanctuary island overview screenshot`() {
    val artifactDir = "build/visual_screenshots"
    val state = buildRichSampleState()
    composeTestRule.mainClock.autoAdvance = false
    var hostView: View? = null

    composeTestRule.setContent {
      hostView = LocalView.current
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
    composeTestRule.mainClock.advanceTimeBy(160L)
    composeTestRule.onNodeWithTag("camera_reset_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(160L)
    composeTestRule.waitForIdle()
    hostView?.let { saveViewScreenshot(it, "$artifactDir/sanctuary_master_43_overview.png") }
    assertTrue(File("$artifactDir/sanctuary_master_43_overview.png").exists())
  }
}
