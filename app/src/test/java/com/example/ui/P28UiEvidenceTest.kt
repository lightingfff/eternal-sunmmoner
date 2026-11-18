package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.PersonalityType
import com.example.game.pixel.SanctuaryArtAssets
import com.example.ui.components.p28.ESHeroCard
import com.example.ui.components.p28.ESLoadingOverlay
import com.example.ui.components.p28.ESLoadingState
import com.example.ui.components.p28.ESLoadingTips
import com.example.ui.components.p28.ESMiniHeroPortrait
import com.example.ui.components.p28.ESNoticeStack
import com.example.ui.components.HeroManagementDialog
import com.example.ui.components.p28.ESP28Assets
import com.example.ui.components.p28.GameNoticeUi
import com.example.ui.components.p28.NoticeCenter
import com.example.ui.components.p28.NoticePriority
import java.io.File
import org.junit.Assert.assertEquals
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
 * P28 UI render evidence: captures the real Compose components (rendered through
 * Robolectric NATIVE graphics) into build/visual_screenshots/p28/ui/ so the PR
 * carries engine screenshots of the new Astral Reliquary UI at two viewports,
 * plus semantic assertions so the test fails if a component stops binding data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P28UiEvidenceTest {
  @get:Rule val rule = createComposeRule()

  @Before fun initAssets() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    ESP28Assets.resetForTest()
  }

  private fun hero(id: String, name: String, rarity: Long, clazz: HeroClassType) = HeroRecord(
    id = id, name = name, heroClass = clazz.name, rarity = rarity, level = 6L,
    personality = PersonalityType.CYNICAL.name,
  )

  private val roster = listOf(
    hero("p28_a", "Aaron Delcut", 2L, HeroClassType.KNIGHT),
    hero("p28_b", "Han Islat", 2L, HeroClassType.ASSASSIN),
    hero("p28_c", "Jenna Cirai", 2L, HeroClassType.ARCHER),
    hero("p28_d", "Eouka Rivel Strasburg", 3L, HeroClassType.MAGE),
    hero("p28_e", "Edis Gallen", 3L, HeroClassType.PRIEST),
  )

  private fun capture(view: View?, name: String) {
    rule.mainClock.advanceTimeBy(200L)
    rule.waitForIdle()
    requireNotNull(view)
    val bmp = Bitmap.createBitmap(
      view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888,
    )
    view.draw(Canvas(bmp))
    val path = File("build/visual_screenshots/p28/ui/$name.png")
    path.parentFile?.mkdirs()
    path.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    require(path.length() > 10_000) { "$name: empty Compose screenshot" }
  }

  private fun content(block: @Composable () -> Unit): () -> View? {
    rule.mainClock.autoAdvance = false
    var view: View? = null
    rule.setContent { view = LocalView.current; block() }
    rule.mainClock.advanceTimeBy(160L)
    rule.waitForIdle()
    return { view }
  }

  private fun assertWithinViewport(label: String, bounds: Rect, viewport: Rect) {
    assertTrue(
      "$label must fit wholly inside the viewport (content=$bounds, viewport=$viewport)",
      bounds.left >= viewport.left && bounds.top >= viewport.top &&
        bounds.right <= viewport.right && bounds.bottom <= viewport.bottom,
    )
  }

  @Test @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun heroCardPortrait() {
    val view = content {
      Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ESHeroCard(hero = roster[3], modifier = Modifier.width(180.dp))
        ESHeroCard(hero = roster[0], selected = true, modifier = Modifier.width(180.dp))
      }
    }
    rule.onNodeWithText("Eouka Rivel Strasburg", substring = true).assertExists()
    rule.onNodeWithText("Aaron Delcut", substring = true).assertExists()
    capture(view(), "hero_cards_port_360")
  }

  @Test @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun partyGridLandscape() {
    val view = content {
      Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          roster.take(3).forEach { ESMiniHeroPortrait(it.name, HeroClassType.valueOf(it.heroClass), 84.dp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          roster.drop(3).forEach { ESMiniHeroPortrait(it.name, HeroClassType.valueOf(it.heroClass), 84.dp) }
        }
      }
    }
    capture(view(), "party_grid_land_800")
  }

  @Test @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun noticeStackPortrait() {
    val center = NoticeCenter()
    center.offer(GameNoticeUi("p28_n1", NoticePriority.CEREMONIAL, "Sự kiện", "LILINIE (★★★) đã trở về trong vòng tay của Nữ Thần.", iconKey = "icon_event", createdAtMs = 3L))
    center.offer(GameNoticeUi("p28_n2", NoticePriority.TACTICAL, null, "Kẻ địch đang áp sát — lui về căn cứ.", iconKey = "icon_retreat", createdAtMs = 2L))
    center.offer(GameNoticeUi("p28_n3", NoticePriority.INFO, null, "Đã nhận 120 vàng từ nhiệm vụ.", iconKey = "icon_summon", createdAtMs = 1L))
    val view = content { Box(Modifier.fillMaxSize()) { ESNoticeStack(center, Modifier.align(androidx.compose.ui.Alignment.TopEnd)) } }
    rule.onNodeWithTag("notice_p28_n1").assertExists()
    rule.onNodeWithTag("notice_p28_n2").assertExists()
    rule.onNodeWithText("Nữ Thần", substring = true).assertExists()
    capture(view(), "notice_stack_port_360")
  }

  @Test @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun loadingOverlayPortrait() {
    val tip = ESLoadingTips.all[0]
    val view = content {
      ESLoadingOverlay(ESLoadingState.Determinate("Đang triệu hồi linh hồn...", 0.62f, tip = tip))
    }
    val viewport = rule.onRoot().fetchSemanticsNode().boundsInRoot
    val stageBounds = rule.onNodeWithTag("es_loading_stage")
      .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    val tipBounds = rule.onNodeWithTag("es_loading_tip_panel")
      .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    rule.onNodeWithText("Đang triệu hồi", substring = true).assertIsDisplayed()
    rule.onNodeWithText(tip).assertIsDisplayed()
    rule.onNodeWithTag("es_loading_lobby_backdrop").assertExists()
    rule.onNodeWithTag("es_loading_progress_track").assertExists()
    rule.onNodeWithTag("es_loading_progress_fill").assertExists()
    assertWithinViewport("Portrait loading stage", stageBounds, viewport)
    assertWithinViewport("Portrait gameplay tip", tipBounds, viewport)
    capture(view(), "loading_port_360")
  }

  @Test @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun loadingOverlayLandscape() {
    val tip = ESLoadingTips.all[1]
    val view = content {
      ESLoadingOverlay(ESLoadingState.Indeterminate("Đang đồng bộ dữ liệu người chơi...", tip = tip))
    }
    val viewport = rule.onRoot().fetchSemanticsNode().boundsInRoot
    val stageBounds = rule.onNodeWithTag("es_loading_stage")
      .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    val tipBounds = rule.onNodeWithTag("es_loading_tip_panel")
      .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    rule.onNodeWithText("đồng bộ", substring = true).assertIsDisplayed()
    rule.onNodeWithText(tip).assertIsDisplayed()
    rule.onNodeWithTag("es_loading_lobby_backdrop").assertExists()
    rule.onNodeWithTag("es_loading_progress_track").assertDoesNotExist()
    assertWithinViewport("Landscape loading stage", stageBounds, viewport)
    assertWithinViewport("Landscape gameplay tip", tipBounds, viewport)
    capture(view(), "loading_land_800")
  }

  @Test @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun loadingOverlayErrorPortraitShowsDistinctAccessibleDetail() {
    val message = "Không thể đọc dữ liệu lưu"
    val view = content {
      ESLoadingOverlay(ESLoadingState.Failed(message))
    }
    val viewport = rule.onRoot().fetchSemanticsNode().boundsInRoot
    val stageBounds = rule.onNodeWithTag("es_loading_stage")
      .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    val errorBounds = rule.onNodeWithTag("es_loading_error_panel")
      .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    rule.onNodeWithText("Không thể tải dữ liệu").assertIsDisplayed()
    rule.onNodeWithText(message).assertIsDisplayed()
    rule.onNodeWithTag("es_loading_tip_panel").assertDoesNotExist()
    rule.onNodeWithTag("es_loading_progress_track").assertDoesNotExist()
    assertWithinViewport("Error loading stage", stageBounds, viewport)
    assertWithinViewport("Error detail panel", errorBounds, viewport)
    capture(view(), "loading_error_port_360")
  }

  @Test @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun loadingOverlayLongGuestBootstrapCopyStaysVisibleAtLargeFontScale() {
    val stage = "Đang triệu hồi Thánh Địa (The Sanctuary)... • " +
      "Khôi phục Hồ Sơ Khách & dữ liệu Thánh Địa trên thiết bị • " +
      "⚡ Chế độ Khách ngoại tuyến — đám mây chưa cấu hình"
    val tip = ESLoadingTips.all[3]
    val view = content {
      val platformDensity = LocalDensity.current
      CompositionLocalProvider(
        LocalDensity provides Density(platformDensity.density, fontScale = 1.3f),
      ) {
        ESLoadingOverlay(ESLoadingState.Indeterminate(stage, tip = tip))
      }
    }

    val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
    val stageBounds = rule.onNodeWithTag("es_loading_stage")
      .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    val tipBounds = rule.onNodeWithTag("es_loading_tip_panel")
      .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    rule.onNodeWithText(stage).assertIsDisplayed()
    rule.onNodeWithText(tip).assertIsDisplayed()
    rule.onNodeWithTag("es_loading_progress_track").assertDoesNotExist()
    assertWithinViewport("Large-font bootstrap stage", stageBounds, root)
    assertWithinViewport("Large-font gameplay tip", tipBounds, root)
    capture(view(), "loading_guest_bootstrap_port_360_font130")
  }

  @Test @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun heroManagementDialogWiresRealHeroCardsAndCallbacks() {
    var inspected: String? = null
    var toggled: String? = null
    val heroes = listOf(roster[0], roster[3])
    val view = content {
      HeroManagementDialog(
        heroes = heroes, teamMode = true, busy = false,
        onClose = {}, onInspect = { inspected = it.id }, onToggle = { toggled = it.id },
      )
    }
    rule.onNodeWithTag("hero_management_dialog").assertExists()
    rule.onNodeWithTag("hero_card_p28_a").assertExists()
    rule.onNodeWithTag("hero_card_p28_d").assertExists()
    rule.onNodeWithTag("assign_p28_a").assertExists().performClick()
    assertEquals("p28_a", toggled)
    rule.onNodeWithTag("inspect_p28_d").assertExists().performClick()
    assertEquals("p28_d", inspected)
    // NOTE: Dialog content lives in a separate window, so view.draw() would capture an
    // empty main view — no screenshot here; semantic assertions above are the evidence.
  }
}
