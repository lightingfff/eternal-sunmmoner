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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.PersonalityType
import com.example.game.pixel.SanctuaryArtAssets
import com.example.ui.components.p28.ESHeroCard
import com.example.ui.components.p28.ESLoadingOverlay
import com.example.ui.components.p28.ESLoadingState
import com.example.ui.components.p28.ESMiniHeroPortrait
import com.example.ui.components.p28.ESNoticeStack
import com.example.ui.components.p28.ESP28Assets
import com.example.ui.components.p28.GameNoticeUi
import com.example.ui.components.p28.NoticeCenter
import com.example.ui.components.p28.NoticePriority
import java.io.File
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
    val view = content { ESLoadingOverlay(ESLoadingState.Determinate("Đang triệu hồi linh hồn...", 0.62f)) }
    rule.onNodeWithTag("es_loading_stage").assertExists()
    rule.onNodeWithText("Đang triệu hồi", substring = true).assertExists()
    capture(view(), "loading_port_360")
  }

  @Test @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun loadingOverlayLandscape() {
    val view = content { ESLoadingOverlay(ESLoadingState.Indeterminate("Đang đồng bộ dữ liệu người chơi...")) }
    rule.onNodeWithText("đồng bộ", substring = true).assertExists()
    capture(view(), "loading_land_800")
  }
}
