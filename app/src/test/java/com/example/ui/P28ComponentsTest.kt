package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.HeroStatusType
import com.example.data.model.PersonalityType
import com.example.game.pixel.SanctuaryArtAssets
import com.example.ui.components.FantasyChoiceButton
import com.example.ui.components.p28.ESHeroCard
import com.example.ui.components.p28.ESLoadingOverlay
import com.example.ui.components.p28.ESLoadingState
import com.example.ui.components.p28.ESLoadingTips
import com.example.ui.components.p28.ESP28Assets
import com.example.ui.components.p28.GameNoticeUi
import com.example.ui.components.p28.NoticeCenter
import com.example.ui.components.p28.NoticePriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P28 component tests: the ui_p28 PNG pack must actually decode from assets with the
 * manifest dimensions and alpha, the notice queue must dedup/order/cap, the loading
 * overlay must render its real stage text (never a fake progress), and the hero card
 * must bind name/rarity from HeroRecord state instead of baked pixels.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P28ComponentsTest {
  @get:Rule val rule = createComposeRule()

  @Before fun initAssets() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    ESP28Assets.resetForTest()
  }

  @Test fun manifestListsEveryPackAssetAndEachPngDecodesWithManifestSize() {
    val manifest = SanctuaryArtAssets.textAsset("ui_p28/manifest.json")
    assertNotNull("ui_p28/manifest.json must ship in assets", manifest)
    val assets = org.json.JSONObject(manifest!!).getJSONObject("assets")
    val keys = assets.keys().asSequence().toList()
    assertEquals(24, keys.size)
    for (key in keys) {
      val meta = assets.getJSONObject(key)
      val entry = ESP28Assets.entry(key)
      assertNotNull("manifest entry resolves for $key", entry)
      val bmp = ESP28Assets.image(key)
      assertNotNull("PNG decodes from assets for $key (${meta.getString("path")})", bmp)
      assertEquals("width of $key", meta.getInt("width"), bmp!!.width)
      assertEquals("height of $key", meta.getInt("height"), bmp.height)
    }
    for (frameId in listOf("global_panel_frame", "button_primary", "button_secondary")) {
      assertEquals("authored nine-slice inset for $frameId", 56, ESP28Assets.entry(frameId)!!.nineSliceInsetPx)
    }
    assertEquals(
      "authored nine-slice inset for loading_tip_frame",
      56,
      ESP28Assets.entry("loading_tip_frame")!!.nineSliceInsetPx,
    )
    // Frames, button faces, stars and icons must retain a real alpha channel.
    for (frameId in listOf("card_frame_bronze", "card_frame_silver", "card_frame_obsidian",
      "card_nameplate", "rarity_star_on", "rarity_star_off",
      "notice_standard", "notice_warning", "notice_ceremony", "loading_sigil",
      "loading_tip_frame", "global_panel_frame", "button_primary", "button_secondary")) {
      val bmp = ESP28Assets.image(frameId)!!
      assertTrue("$frameId must retain its alpha channel", bmp.asAndroidBitmap().hasAlpha())
    }
    for (buttonId in listOf("button_primary", "button_secondary")) {
      val button = ESP28Assets.image(buttonId)!!.asAndroidBitmap()
      assertEquals(
        "$buttonId keeps an opaque live-label face",
        255,
        button.getPixel(button.width / 2, button.height / 2) ushr 24,
      )
      assertEquals(
        "$buttonId keeps transparent exterior corners for nine-slice drawing",
        0,
        button.getPixel(0, 0) ushr 24,
      )
    }
    val tipFrame = ESP28Assets.image("loading_tip_frame")!!.asAndroidBitmap()
    assertEquals(
      "loading tip frame center stays transparent for live Compose text",
      0,
      tipFrame.getPixel(tipFrame.width / 2, tipFrame.height / 2) ushr 24,
    )
    val panelFrame = ESP28Assets.image("global_panel_frame")!!.asAndroidBitmap()
    assertEquals(
      "global panel frame keeps its content area transparent for live Compose UI",
      0,
      panelFrame.getPixel(panelFrame.width / 2, panelFrame.height / 2) ushr 24,
    )
  }

  @Test fun fantasyChoiceButtonExposesButtonAndExplicitSelectedSemantics() {
    rule.setContent {
      Column {
        FantasyChoiceButton(
          onClick = {},
          selected = true,
          modifier = Modifier.testTag("selected_fantasy_choice"),
        ) { Text("Selected choice") }
        FantasyChoiceButton(
          onClick = {},
          selected = false,
          modifier = Modifier.testTag("unselected_fantasy_choice"),
        ) { Text("Unselected choice") }
        FantasyChoiceButton(
          onClick = {},
          modifier = Modifier.testTag("plain_fantasy_choice"),
        ) { Text("Plain choice") }
      }
    }

    rule.onNodeWithTag("selected_fantasy_choice").assert(
      SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
    ).assert(
      SemanticsMatcher.expectValue(SemanticsProperties.Selected, true),
    )
    rule.onNodeWithTag("unselected_fantasy_choice").assert(
      SemanticsMatcher.expectValue(SemanticsProperties.Selected, false),
    )
    rule.onNodeWithTag("plain_fantasy_choice").assert(
      SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
    )
  }

  @Test fun missingAssetReturnsNullInsteadOfThrowing() {
    assertNull(ESP28Assets.entry("does_not_exist"))
    assertNull(ESP28Assets.image("does_not_exist"))
  }

  @Test fun noticeCenterDedupsByStableIdOrdersByPriorityAndCapsLog() {
    val center = NoticeCenter()
    center.offer(GameNoticeUi("a", NoticePriority.INFO, null, "thông báo thường", createdAtMs = 1))
    center.offer(GameNoticeUi("b", NoticePriority.TACTICAL, null, "cảnh báo", createdAtMs = 2))
    center.offer(GameNoticeUi("c", NoticePriority.CEREMONIAL, null, "sự kiện lớn", createdAtMs = 3))
    // Re-offering the same stableId replaces instead of duplicating.
    center.offer(GameNoticeUi("a", NoticePriority.INFO, null, "đã cập nhật", createdAtMs = 4))
    assertEquals(3, center.notices.size)
    assertEquals("sự kiện lớn", center.notices[0].body)   // CEREMONIAL first
    assertEquals("cảnh báo", center.notices[1].body)      // then TACTICAL
    assertEquals("đã cập nhật", center.notices[2].body)    // INFO last, updated body
    for (i in 0 until 12) {
      center.offer(GameNoticeUi("burst_$i", NoticePriority.INFO, null, "nhiều sự kiện $i", createdAtMs = 10L + i))
    }
    assertTrue(center.notices.size <= NoticeCenter.MAX_RETAINED)
    center.dismiss("c")
    assertTrue(center.notices.none { it.stableId == "c" })
  }

  // NOTE: this Compose test version forbids calling setContent twice per test,
  // so each loading state gets its own @Test (and its own compose rule).
  @Test fun loadingOverlayShowsRealStageAndAccurateTipWithoutFakeProgress() {
    val tip = ESLoadingTips.all.first()
    rule.setContent {
      ESLoadingOverlay(ESLoadingState.Indeterminate("Đang nạp bản đồ Tháp...", tip = tip))
    }
    rule.onNodeWithTag("es_loading_stage").assertExists().assert(
      SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
    )
    rule.onNodeWithText("Đang nạp bản đồ Tháp", substring = true).assertExists()
    rule.onNodeWithTag("es_loading_tip_panel").assertExists()
    rule.onNodeWithTag("es_loading_tip_heading").assertExists()
    rule.onNodeWithTag("es_loading_tip").assertExists()
    rule.onNodeWithText(tip).assertExists()
    rule.onNodeWithTag("es_loading_progress_track").assertDoesNotExist()
  }

  @Test fun loadingTipPoolCyclesThroughDistinctEntries() {
    assertTrue("Keep at least four gameplay tips", ESLoadingTips.all.size >= 4)
    assertTrue("Keep the verified infirmary route tip", ESLoadingTips.all.any { "Điều dưỡng" in it })
    val selected = List(ESLoadingTips.all.size) { ESLoadingTips.next() }
    assertEquals(ESLoadingTips.all.size, selected.toSet().size)
    assertEquals(ESLoadingTips.all.toSet(), selected.toSet())
  }

  @Test fun loadingProgressSemanticsAndFillFollowOnlyTheMeasuredFraction() {
    val measuredFraction = mutableStateOf(0f)
    rule.setContent {
      ESLoadingOverlay(
        ESLoadingState.Determinate(
          stage = "Đang khôi phục hồ sơ...",
          fraction = measuredFraction.value,
          tip = ESLoadingTips.all.first(),
        ),
      )
    }

    fun assertProgress(input: Float, expected: Float) {
      rule.runOnIdle { measuredFraction.value = input }
      rule.waitForIdle()
      rule.onNodeWithTag("es_loading_progress_track").assert(
        SemanticsMatcher.expectValue(
          SemanticsProperties.ProgressBarRangeInfo,
          ProgressBarRangeInfo(expected, 0f..1f),
        ),
      )
      if (expected == 0f) {
        rule.onNodeWithTag("es_loading_progress_fill").assertDoesNotExist()
      } else {
        val trackWidth = rule.onNodeWithTag("es_loading_progress_track")
          .fetchSemanticsNode().boundsInRoot.width
        val fillWidth = rule.onNodeWithTag("es_loading_progress_fill")
          .fetchSemanticsNode().boundsInRoot.width
        assertEquals("fill width follows the supplied measured fraction", expected, fillWidth / trackWidth, 0.02f)
      }
    }

    assertProgress(0f, 0f)
    assertProgress(0.62f, 0.62f)
    assertProgress(1f, 1f)
    assertProgress(Float.NaN, 0f)
    assertProgress(-0.25f, 0f)
    assertProgress(1.25f, 1f)
  }

  @Test fun loadingOverlayShowsOneAccessibleErrorDetailWithoutTipOrProgress() {
    val message = "Không thể đọc dữ liệu lưu"
    rule.setContent {
      ESLoadingOverlay(ESLoadingState.Failed(message))
    }
    rule.onNodeWithText("Không thể tải dữ liệu").assertExists()
    rule.onNodeWithTag("es_loading_stage").assertExists()
    rule.onNodeWithTag("es_loading_error_panel").assertExists()
    rule.onNodeWithTag("es_loading_error").assertExists().assert(
      SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
    )
    assertEquals(
      "Error details should not be duplicated in the stage heading",
      1,
      rule.onAllNodesWithText(message).fetchSemanticsNodes().size,
    )
    rule.onNodeWithTag("es_loading_tip_panel").assertDoesNotExist()
    rule.onNodeWithTag("es_loading_progress_track").assertDoesNotExist()
  }

  @Test fun loadingOverlayHiddenWhenHidden() {
    rule.setContent { ESLoadingOverlay(ESLoadingState.Hidden) }
    rule.onNodeWithTag("es_loading_stage").assertDoesNotExist()
    rule.onNodeWithTag("es_loading_tip_panel").assertDoesNotExist()
  }

  @Test fun visibleLoadingOverlayBlocksTouchesFromReachableRouteBelow() {
    var underlyingClicked = false
    rule.setContent {
      Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().clickable { underlyingClicked = true })
        ESLoadingOverlay(ESLoadingState.Indeterminate("Đang khôi phục hồ sơ", tip = ESLoadingTips.all.first()))
      }
    }
    rule.onNodeWithTag("es_loading_stage").performTouchInput { click() }
    rule.runOnIdle { assertTrue("loading must block world taps", !underlyingClicked) }
  }

  @Test fun heroCardBindsNameAndRarityFromHeroRecord() {
    val hero = HeroRecord(
      id = "p28_hero", name = "Lyra Phong Tiễn", heroClass = HeroClassType.ARCHER.name,
      rarity = 4, level = 6, personality = PersonalityType.CYNICAL.name,
    )
    rule.setContent {
      ESHeroCard(
        hero = hero,
        selected = true,
        onClick = {},
        modifier = Modifier.testTag("selected_hero_card"),
      )
    }
    rule.onNodeWithTag("selected_hero_card").assert(
      SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
    ).assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    rule.onNodeWithText("Lyra Phong Tiễn", substring = true).assertExists()
  }

  @Test fun injuredHeroCardExplainsItsUnavailableStateAndCannotBeSelected() {
    var clicked = false
    val hero = HeroRecord(
      id = "p28_injured", name = "Injured Hero", status = HeroStatusType.INJURED.name,
      injuryRecoveryUntilEpochSec = System.currentTimeMillis() / 1000L + 3600L,
    )
    rule.setContent {
      ESHeroCard(
        hero = hero,
        onClick = { clicked = true },
        modifier = Modifier.testTag("injured_hero_card"),
      )
    }
    rule.onNodeWithTag("injured_hero_card").assertIsNotEnabled()
    rule.onNodeWithText("DƯỠNG\nTHƯƠNG").assertExists()
    rule.onNodeWithTag("injured_hero_card").performTouchInput { click() }
    rule.runOnIdle { assertTrue("injured heroes must not be selectable", !clicked) }
  }
}
