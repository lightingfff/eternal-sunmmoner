package com.example.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.platform.LocalView
import java.io.File
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.MasterProfile
import com.example.data.model.PersonalityType
import com.example.ui.components.SanctuaryManagementNav
import com.example.ui.theme.EternalSummonerTheme
import com.example.ui.screens.MasterHubScreen
import com.example.ui.screens.SanctuaryLobbyScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P29 regression tests:
 *  - idle bottom nav is a collapsed icon dock and every nav_<id> tag still fires;
 *  - MasterHubScreen is user-opened from the master chip, binds REAL MasterProfile,
 *    and its shortcuts/back fire callbacks;
 *  - on an 800x360 landscape idle hub, no permanent HUD element covers the screen center.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P29HubHudTest {
  @get:Rule val rule = createComposeRule()

  private fun sampleState(): SummonerUiState {
    val profile = MasterProfile(
      id = "u_master_test", masterName = "Thống Lĩnh Lokir", masterLevel = 12,
      highestClearedFloor = 1, gold = 14850, manaCrystals = 640, soulShards = 125,
      storyFlags = listOf(com.example.game.story.ManhwaStoryData.FLAG_PROLOGUE_SEEN),
    )
    val hero = HeroRecord(
      id = "h1", name = "Kaelen Vệ Thần", heroClass = HeroClassType.KNIGHT.name,
      rarity = 4, level = 7, personality = PersonalityType.CYNICAL.name,
    )
    return SummonerUiState(
      isBootstrapping = false, profile = profile, heroes = listOf(hero),
      isGuestAccount = true,
    )
  }

  private fun rectCovers(rect: Rect, x: Float, y: Float): Boolean =
    x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom

  private fun capture(view: View?, name: String) {
    rule.mainClock.advanceTimeBy(200L)
    rule.waitForIdle()
    requireNotNull(view)
    val bmp = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bmp))
    val path = File("build/visual_screenshots/p29/hub/$name.png")
    path.parentFile?.mkdirs()
    path.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    require(path.length() > 10_000) { "$name: empty screenshot" }
  }

  @Test fun navDockCollapsedByDefaultAndAllNavTagsFire() {
    var navigated: String? = null
    rule.setContent { SanctuaryManagementNav(onNavigate = { navigated = it }) }
    rule.onNodeWithTag("management_navigation").assertExists()
    // Collapsed: labels hidden, icons present.
    rule.onNodeWithTag("nav_heroes").assertExists()
    rule.onNodeWithText("Anh hùng").assertDoesNotExist()
    // Expand shows labels, then every tag still fires its callback.
    rule.onNodeWithTag("nav_expand_toggle").performClick()
    rule.onNodeWithText("Anh hùng").assertExists()
    rule.onNodeWithTag("nav_tower").performClick()
    assertEquals("tower", navigated)
    rule.onNodeWithTag("nav_summon").performClick()
    assertEquals("summon", navigated)
  }

  @Test fun masterHubScreenBindsRealProfileAndShortcutsFire() {
    var navigated: String? = null
    var settingsOpened = false
    var closed = false
    rule.setContent {
      MasterHubScreen(
        state = sampleState(),
        onNavigate = { navigated = it },
        onOpenSettings = { settingsOpened = true },
        onClose = { closed = true },
      )
    }
    rule.onNodeWithTag("master_hub_screen").assertExists()
    rule.onNodeWithText("Thống Lĩnh Lokir", substring = true).assertExists()
    rule.onNodeWithText("Cấp 12", substring = true).assertExists()
    rule.onNodeWithText("14850", substring = true).assertExists() // real gold from profile
    rule.onNodeWithTag("master_hub_nav_tower").performClick()
    assertEquals("tower", navigated)
    rule.onNodeWithTag("master_hub_nav_settings").performScrollTo().performClick()
    assertTrue(settingsOpened)
    rule.onNodeWithTag("master_hub_close").performClick()
    assertTrue(closed)
  }


  @Test
  fun masterModalDismissedByAndroidBack() {
    rule.mainClock.autoAdvance = false
    var owner: androidx.activity.OnBackPressedDispatcherOwner? = null
    rule.setContent {
      owner = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current
      EternalSummonerTheme {
        SanctuaryLobbyScreen(
          state = sampleState(),
          sceneActive = false,
          onOpenFacility = {}, onCloseFacility = {}, onSummonHero = {},
          onTrainHero = { _, _ -> }, onEquipItem = { _, _ -> }, onSynthesizeGear = {},
          onEnhanceGear = {}, onRecoverHero = { _, _ -> }, onHostMeal = {},
          onToggleSquad = {}, onCompletePrologue = {}, onSignOut = {}, onOpenSettings = {},
        )
      }
    }
    rule.mainClock.advanceTimeBy(200L)
    rule.waitForIdle()
    rule.onNodeWithTag("master_chip").performClick()
    rule.mainClock.advanceTimeBy(120L)
    rule.waitForIdle()
    rule.onNodeWithTag("master_hub_screen").assertExists()
    rule.runOnIdle { owner?.onBackPressedDispatcher?.onBackPressed() }
    rule.mainClock.advanceTimeBy(200L)
    rule.waitForIdle()
    rule.onNodeWithTag("master_hub_screen").assertDoesNotExist()
  }

  @Test
  fun collapsedNavHasAccessibleDestinationNames() {
    rule.mainClock.autoAdvance = false
    rule.setContent {
      EternalSummonerTheme {
        SanctuaryLobbyScreen(
          state = sampleState(),
          sceneActive = false,
          onOpenFacility = {}, onCloseFacility = {}, onSummonHero = {},
          onTrainHero = { _, _ -> }, onEquipItem = { _, _ -> }, onSynthesizeGear = {},
          onEnhanceGear = {}, onRecoverHero = { _, _ -> }, onHostMeal = {},
          onToggleSquad = {}, onCompletePrologue = {}, onSignOut = {}, onOpenSettings = {},
        )
      }
    }
    rule.mainClock.advanceTimeBy(200L)
    rule.waitForIdle()
    val node = rule.onNodeWithTag("nav_heroes").fetchSemanticsNode()
    val desc = node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription) { emptyList() }
    val text = node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }
    assertTrue(desc.any { it.isNotBlank() } || text.any { it.text.isNotBlank() })
  }

  @Test @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun idleLandscapeHudDoesNotCoverCenter() {
    rule.mainClock.autoAdvance = false
    var view: View? = null
    rule.setContent {
      view = LocalView.current
      EternalSummonerTheme {
        SanctuaryLobbyScreen(
          state = sampleState(),
          onOpenFacility = {}, onCloseFacility = {}, onSummonHero = {},
          onTrainHero = { _, _ -> }, onEquipItem = { _, _ -> }, onSynthesizeGear = {},
          onEnhanceGear = {}, onRecoverHero = { _, _ -> }, onHostMeal = {},
          onToggleSquad = {}, onCompletePrologue = {}, onSignOut = {}, onOpenSettings = {},
        )
      }
    }
    rule.mainClock.advanceTimeBy(200L)
    rule.waitForIdle()
    val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
    val cx = (root.left + root.right) / 2f
    val cy = (root.top + root.bottom) / 2f
    // Permanent idle chrome must exist (tags preserved)...
    val topHud = rule.onNodeWithTag("sanctuary_top_hud").fetchSemanticsNode().boundsInRoot
    val nav = rule.onNodeWithTag("management_navigation").fetchSemanticsNode().boundsInRoot
    val camera = rule.onNodeWithTag("camera_controls_bar").fetchSemanticsNode().boundsInRoot
    val commandPanel = rule.onNodeWithTag("hero_command_panel").fetchSemanticsNode().boundsInRoot
    // ...be compact (<= 25% of viewport height each)...
    assertTrue("top HUD too tall: ${topHud.height}px of ${root.height}px", topHud.height <= root.height * 0.25f)
    assertTrue("bottom nav too tall: ${nav.height}px of ${root.height}px", nav.height <= root.height * 0.25f)
    assertTrue("camera bar too tall: ${camera.height}px of ${root.height}px", camera.height <= root.height * 0.25f)
    // ...and none of them may cover the central play area while idle.
    assertFalse("top HUD covers center", rectCovers(topHud, cx, cy))
    assertFalse("bottom nav covers center", rectCovers(nav, cx, cy))
    assertFalse("camera bar covers center", rectCovers(camera, cx, cy))
    assertFalse("command panel covers center", rectCovers(commandPanel, cx, cy))
    // Master page opens only from the chip.
    rule.onNodeWithTag("master_hub_screen").assertDoesNotExist()
    capture(view, "hub_idle_land_800x360")
    rule.onNodeWithTag("master_chip").performClick()
    rule.mainClock.advanceTimeBy(120L)
    rule.waitForIdle()
    rule.onNodeWithTag("master_hub_screen").assertExists()
    capture(view, "master_hub_land_800x360")
  }


  /** Gemini review follow-up: MEASURED pixel coverage of permanent HUD chrome at
   *  800x360, idle and with a hero selected. Writes hud_coverage.json as evidence. */
  @Test @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun hudCoverageMeasured800x360() {
    rule.mainClock.autoAdvance = false
    var view: View? = null
    rule.setContent {
      view = LocalView.current
      EternalSummonerTheme {
        SanctuaryLobbyScreen(
          state = sampleState(),
          onOpenFacility = {}, onCloseFacility = {}, onSummonHero = {},
          onTrainHero = { _, _ -> }, onEquipItem = { _, _ -> }, onSynthesizeGear = {},
          onEnhanceGear = {}, onRecoverHero = { _, _ -> }, onHostMeal = {},
          onToggleSquad = {}, onCompletePrologue = {}, onSignOut = {}, onOpenSettings = {},
        )
      }
    }
    rule.mainClock.advanceTimeBy(200L)
    rule.waitForIdle()

    fun coveragePct(): Double {
      val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
      // Measure VISIBLE chrome pixels: the actual chip row (not the transparent full-width
      // container), the nav dock, the (collapsed) camera toggle and the command panel.
      val rects = listOf("top_hud_chip_row", "management_navigation", "camera_controls_bar", "hero_command_panel")
        .map { rule.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot }
      val gx = 80; val gy = 36
      var covered = 0
      for (i in 0 until gx) for (j in 0 until gy) {
        val x = root.left + (i + 0.5f) * root.width / gx
        val y = root.top + (j + 0.5f) * root.height / gy
        if (rects.any { rectCovers(it, x, y) }) covered++
      }
      return covered * 100.0 / (gx * gy)
    }

    val idlePct = coveragePct()
    // Expand the quick-command dock (hero chips + facility buttons) and re-measure.
    rule.onNodeWithTag("quick_commands_toggle").performClick()
    rule.mainClock.advanceTimeBy(800L)
    rule.waitForIdle()
    val expandedPct = coveragePct()
    capture(view, "hub_commands_expanded_land_800x360")

    val report = org.json.JSONObject()
      .put("viewport", "800x360").put("idlePermanentHudCoveragePct", idlePct)
      .put("quickCommandsExpandedCoveragePct", expandedPct)
      .put("note", "sampled 80x36 over root bounds; permanent tags only (top hud, nav dock, camera bar, command panel incl. expanded commands)")
    val f = File("build/visual_screenshots/p29/hub/hud_coverage.json")
    f.parentFile?.mkdirs(); f.writeText(report.toString(2))
    println("P29 HUD coverage 800x360: idle=${idlePct}% expanded=${expandedPct}%")
    assertTrue("idle HUD chrome covers ${idlePct}% (>26%)", idlePct <= 26.0)
    assertTrue("expanded HUD covers ${expandedPct}% (>45%)", expandedPct <= 45.0)
  }

  @Test @Config(qualifiers = "w360dp-h800dp-port-xhdpi")
  fun idlePortraitHudStaysCompact() {
    rule.mainClock.autoAdvance = false
    var view: View? = null
    rule.setContent {
      view = LocalView.current
      EternalSummonerTheme {
        SanctuaryLobbyScreen(
          state = sampleState(),
          onOpenFacility = {}, onCloseFacility = {}, onSummonHero = {},
          onTrainHero = { _, _ -> }, onEquipItem = { _, _ -> }, onSynthesizeGear = {},
          onEnhanceGear = {}, onRecoverHero = { _, _ -> }, onHostMeal = {},
          onToggleSquad = {}, onCompletePrologue = {}, onSignOut = {}, onOpenSettings = {},
        )
      }
    }
    rule.mainClock.advanceTimeBy(200L)
    rule.waitForIdle()
    val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
    val cx = (root.left + root.right) / 2f
    val cy = (root.top + root.bottom) / 2f
    val topHud = rule.onNodeWithTag("sanctuary_top_hud").fetchSemanticsNode().boundsInRoot
    val nav = rule.onNodeWithTag("management_navigation").fetchSemanticsNode().boundsInRoot
    assertTrue("portrait top HUD too tall", topHud.height <= root.height * 0.18f)
    assertFalse("portrait top HUD covers center", rectCovers(topHud, cx, cy))
    assertFalse("portrait nav covers center", rectCovers(nav, cx, cy))
    capture(view, "hub_idle_port_360x800")
  }
}
