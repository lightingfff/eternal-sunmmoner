package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.firebase.ExpeditionSettlementResult
import com.example.data.model.MasterProfile
import com.example.game.pixel.SanctuaryArtAssets
import com.example.ui.screens.TowerBattleScreen
import com.example.ui.theme.EternalSummonerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36], qualifiers="w400dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Floor1SettlementRecoveryUiTest {
  @get:Rule val rule = createComposeRule()

  @Test fun dismissedTransientErrorCannotHideSaveRetryOrAllowUnsavedExit() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    var state by mutableStateOf(SummonerUiState(isBootstrapping=false,
      profile=MasterProfile(id="save_retry"), expeditionAttemptId="12345678-1234-4123-8123-123456789abc"))
    var calls=0
    var returned=0
    rule.setContent {
      EternalSummonerTheme {
        TowerBattleScreen(state,
          onFinishExpedition={ _, _ ->
            calls++
            state=if(calls==1) state.copy(bannerFeedback="Mất kết nối") else state.copy(
              lastSettlement=ExpeditionSettlementResult(false,1,0,0,0,0,0,emptyList(),emptyList(),null))
          }, onReturnToSanctuary={ returned++ })
      }
    }
    // An empty squad naturally defeats on the first combat step; no terminal state is injected.
    repeat(2) { rule.onNodeWithTag("start_combat_button").performClick() }
    rule.waitUntil(10_000) { calls == 1 }
    rule.onNodeWithTag("return_sanctuary_button").assertIsNotEnabled()
    rule.runOnIdle { state=state.copy(bannerFeedback=null) }
    rule.mainClock.advanceTimeBy(4_100)
    rule.onNodeWithTag("retry_settlement_button").assertIsDisplayed().performTouchInput { click() }
    rule.waitForIdle()
    assertEquals(2,calls)
    rule.onNodeWithTag("return_sanctuary_button").assertIsEnabled().performClick()
    assertEquals(1,returned)
  }
}
