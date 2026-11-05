package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.model.FacilityType
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.MasterProfile
import com.example.data.model.PersonalityType
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import com.example.ui.screens.SanctuaryFacilityModalOverlay
import com.example.ui.screens.SanctuaryLobbyScreen
import com.example.ui.theme.EternalSummonerTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Actual Compose UI via View.draw on Robolectric native graphics; not model concept art. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P27BUiTest {
  @get:Rule val rule = createComposeRule()

  private fun state(shards: Long = 125L, mana: Long = 640L, summoned: Boolean = false): SummonerUiState {
    val hero = HeroRecord(
      id = "p27b_demo_hero", name = "Lyra Phong Tiễn", title = "Mắt Ưng Tinh Hệ",
      heroClass = HeroClassType.ARCHER.name, rarity = 4, level = 6, maxHp = 410,
      attack = 82, defense = 24, speed = 136,
      personality = PersonalityType.CYNICAL.name, morale = 88, inActiveSquad = true,
    )
    return SummonerUiState(
      isBootstrapping = false,
      profile = MasterProfile(id = "p27b_master", masterName = "Thống Lĩnh Lokir",
        masterLevel = 12, gold = 14850, manaCrystals = mana, soulShards = shards),
      heroes = listOf(hero), newlySummonedHero = if (summoned) hero else null,
      activeFacilityModal = FacilityType.SUMMONING_HALL,
    )
  }

  private fun capture(view: View?, name: String) {
    requireNotNull(view)
    val bmp = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    view.draw(AndroidCanvas(bmp))
    val path = File("build/visual_screenshots/p27b/ui/$name.png")
    path.parentFile?.mkdirs()
    path.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    require(path.length() > 10_000) { "$name: empty Compose screenshot" }
  }

  private fun modal(state: SummonerUiState, onSummon: () -> Unit = {}): () -> View? {
    rule.mainClock.autoAdvance = false
    var view: View? = null
    rule.setContent {
      view = LocalView.current
      EternalSummonerTheme {
        SanctuaryFacilityModalOverlay(
          facility = FacilityType.SUMMONING_HALL, state = state,
          onClose = {}, onSummonHero = onSummon, onTrainHero = { _, _ -> },
          onEquipItem = { _, _ -> }, onSynthesizeGear = {}, onEnhanceGear = {},
          onRecoverHero = { _, _ -> }, onHostMeal = {}, onToggleSquad = {},
        )
      }
    }
    rule.mainClock.advanceTimeBy(160L)
    rule.waitForIdle()
    return { view }
  }

  @Test @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun summonModalLandscapeAndActions() {
    val view = modal(state())
    rule.onNodeWithTag("summon_ritual_button").assertIsEnabled()
    capture(view(), "summon_modal_land_800")
    rule.onNodeWithTag("summon_ritual_button").performClick()
    // AlertDialog uses a Compose transition; the frozen screenshot clock has to
    // advance before an entering dialog is present in the semantics tree.
    rule.mainClock.advanceTimeBy(450L)
    rule.waitForIdle()
    rule.onNodeWithTag("cancel_summon_spend").assertExists().performClick()
    rule.mainClock.advanceTimeBy(450L)
    rule.waitForIdle()
    rule.onNodeWithTag("summon_ritual_button").performClick()
    rule.mainClock.advanceTimeBy(450L)
    rule.waitForIdle()
    rule.onNodeWithTag("confirm_summon_spend").assertExists()
  }

  @Test @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun summonModalPortraitWithInsufficientResources() {
    val view = modal(state(shards = 0, mana = 0))
    rule.onNodeWithTag("summon_ritual_button").assertIsNotEnabled()
    rule.onNodeWithTag("close_facility_modal_button").assertExists()
    capture(view(), "summon_modal_port_360")
  }

  @Test @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun summonResultLandscape() {
    val view = modal(state(summoned = true))
    rule.onNodeWithTag("summon_result").assertExists()
    capture(view(), "summon_result_land_800")
  }

  @Test @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun lobbyHudLandscape() {
    rule.mainClock.autoAdvance = false
    val context = RuntimeEnvironment.getApplication()
    WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(12f, WorldWeather.CLEAR)
    var view: View? = null
    try {
      rule.setContent {
        view = LocalView.current
        EternalSummonerTheme {
          SanctuaryLobbyScreen(state = state().copy(activeFacilityModal = null),
            onOpenFacility = {}, onCloseFacility = {}, onSummonHero = {},
            onTrainHero = { _, _ -> }, onEquipItem = { _, _ -> }, onSynthesizeGear = {},
            onEnhanceGear = {}, onRecoverHero = { _, _ -> }, onHostMeal = {},
            onToggleSquad = {}, onCompletePrologue = {}, onSignOut = {})
        }
      }
      rule.mainClock.advanceTimeBy(160L)
      rule.waitForIdle()
      for (tag in listOf("sanctuary_canvas", "sanctuary_top_hud", "hero_command_panel", "camera_controls_bar")) {
        rule.onNodeWithTag(tag).assertExists()
      }
      capture(view, "hud_land_800")
    } finally {
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
  }
}
