package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.HeroRecord
import com.example.data.model.MasterProfile
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.tower.TowerFloorCatalog
import com.example.ui.screens.TowerBattleScreen
import com.example.ui.theme.EternalSummonerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h360dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PinnedDodgeInputOwnershipTest {
  @get:Rule val rule = createComposeRule()

  @Test fun pinnedMoveHonorsIntroInactiveSceneAndRetreatAndAcknowledgesImmediately() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    var active by mutableStateOf(true)
    val state = SummonerUiState(isBootstrapping = false, profile = MasterProfile(id = "pinned-input"),
      selectedFloor = TowerFloorCatalog.FLOORS.first(),
      heroes = listOf(HeroRecord(id = "pinned-knight", name = "Kaelen", inActiveSquad = true)))
    rule.mainClock.autoAdvance = false
    rule.setContent {
      EternalSummonerTheme {
        TowerBattleScreen(state, onFinishExpedition = { _, _ -> }, onReturnToSanctuary = {}, sceneActive = active)
      }
    }
    rule.mainClock.advanceTimeByFrame()
    rule.onNodeWithTag("pinned_dodge_move_button").assertIsNotEnabled()
    repeat(2) {
      rule.onNodeWithTag("start_combat_button").performTouchInput { click() }
      rule.mainClock.advanceTimeBy(32L)
    }
    rule.onNodeWithTag("pinned_dodge_move_button").assertIsEnabled()
    rule.onNodeWithTag("order_hold_position").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(32L)
    rule.onNodeWithText("Giữ vững đội hình tại chỗ", substring = true).assertExists()
    rule.runOnIdle { active = false }
    rule.mainClock.advanceTimeBy(32L)
    rule.onNodeWithTag("pinned_dodge_move_button").assertIsNotEnabled()
    rule.runOnIdle { active = true }
    rule.mainClock.advanceTimeBy(32L)
    val before = rule.mainClock.currentTime
    rule.onNodeWithTag("pinned_dodge_move_button").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(32L)
    assertTrue("Pinned control must acknowledge before the250ms HUD poll", rule.mainClock.currentTime - before < 250L)
    rule.onNodeWithText("Chạm lên mặt đất chiến trường để cắm cờ điều quân", substring = true).assertExists()
    rule.onNodeWithTag("retreat_mission_button").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(32L)
    rule.onNodeWithTag("pinned_dodge_move_button").assertIsNotEnabled()
    rule.onNodeWithText("Tiếp Tục").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(32L)
    rule.onNodeWithTag("pinned_dodge_move_button").assertIsEnabled()
  }
}
