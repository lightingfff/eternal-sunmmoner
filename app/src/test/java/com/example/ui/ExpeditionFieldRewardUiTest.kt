package com.example.ui

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.LocalGuestSaveManager
import com.example.game.combat.FieldRewardKind
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.tower.TowerFloorCatalog
import com.example.ui.components.ExpeditionRewardIcons
import com.example.ui.screens.TowerBattleScreen
import com.example.ui.theme.EternalSummonerTheme
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Production touch/movement/decision path; native host images are not real-phone evidence. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExpeditionFieldRewardUiTest {
  @get:Rule val rule = createComposeRule()
  private lateinit var host: View

  @Test @Config(qualifiers = "w400dp-h800dp-port-mdpi")
  fun portraitRewardTapCloseAndClaimPreserveThePlayersHeldOrder() = productionSpoils("portrait")

  @Test @Config(qualifiers = "w800dp-h360dp-land-mdpi")
  fun shortLandscapeRewardChoiceLeavesBattlefieldAndMasterControlsUsable() = productionSpoils("landscape")

  @Test fun iconsArePreloadedOnceAndStayWithinTheDeclaredSmallMemoryBudget() {
    val context = RuntimeEnvironment.getApplication() as Context
    ExpeditionRewardIcons.preload(context)
    val count = ExpeditionRewardIcons.decodeCount
    repeat(30) {
      ExpeditionRewardIcons.preload(context)
      FieldRewardKind.entries.forEach { kind ->
        val icon = ExpeditionRewardIcons.image(kind)!!
        assertEquals(64, icon.width)
        assertEquals(64, icon.height)
      }
    }
    assertEquals(count, ExpeditionRewardIcons.decodeCount)
    assertEquals(4, count)
    assertEquals(65_536L, ExpeditionRewardIcons.decodedBytes)
  }

  private fun start() {
    val base = RuntimeEnvironment.getApplication() as Context
    val files = Files.createTempDirectory(base.cacheDir.toPath(), "field-reward-ui-").toFile()
    val context = object : ContextWrapper(base) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = files
    }
    val guest = runBlocking { LocalGuestSaveManager(context).startOrResumeGuestSession() }
    SanctuaryArtAssets.init(base)
    ExpeditionRewardIcons.preload(base)
    rule.mainClock.autoAdvance = false
    rule.setContent {
      host = LocalView.current
      EternalSummonerTheme {
        TowerBattleScreen(SummonerUiState(isBootstrapping = false, profile = guest.profile,
          selectedFloor = TowerFloorCatalog.FLOORS.first(), heroes = guest.heroes, equipment = guest.equipment),
          onFinishExpedition = { _, _ -> }, onReturnToSanctuary = {})
      }
    }
    rule.mainClock.advanceTimeByFrame()
    repeat(2) {
      rule.onNodeWithTag("start_combat_button").performTouchInput { click() }
      rule.mainClock.advanceTimeBy(32L)
    }
  }

  private fun waitFor(tag: String, seconds: Int = 140) {
    var found = false
    repeat(seconds) {
      if (!found) {
        rule.mainClock.advanceTimeBy(1_000L)
        found = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
      }
    }
    assertTrue("Actual starter party must expose $tag through production controls.\n${rule.onRoot().printToString()}", found)
  }

  private fun productionSpoils(orientation: String) {
    start()
    rule.onNodeWithTag("field_reward_open").assertDoesNotExist()
    waitFor("living_decision_north_scout")
    rule.onNodeWithTag("field_reward_open").assertDoesNotExist()
    rule.onNodeWithTag("living_decision_north_scout").performTouchInput { click() }
    waitFor("field_reward_open")
    target("field_reward_open")
    rule.onNodeWithTag("field_reward_open").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(32L)
    target("field_reward_close")
    target("field_reward_north_valuables")
    assertBattlefieldBudget()
    capture("${orientation}_01_offer")
    rule.onNodeWithTag("field_reward_close").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(32L)
    rule.onNodeWithTag("field_reward_hud").assertDoesNotExist()
    rule.onNodeWithTag("living_expedition_hud").assertIsDisplayed()
    rule.onNodeWithTag("order_hold_position").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(32L)
    rule.onNodeWithText("Đang chờ lệnh").assertExists()
    rule.onNodeWithTag("field_reward_open").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(32L)
    rule.onNodeWithTag("field_reward_north_valuables").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    rule.onNodeWithTag("field_reward_hud").assertDoesNotExist()
    rule.onNodeWithTag("field_reward_open").assertDoesNotExist()
    rule.onNodeWithText("Đang chờ lệnh").assertExists()
    rule.onNodeWithText("Đã giữ +60 Vàng", substring = true).assertExists()
    rule.onNodeWithTag("living_resume").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    rule.onNodeWithText("Đội tự thám hiểm").assertExists()
    assertBattlefieldBudget()
    capture("${orientation}_02_claimed_and_resumed")
  }

  private fun target(tag: String) {
    val bounds = rule.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    assertTrue("$tag width=$bounds", bounds.width >= 48f)
    assertTrue("$tag height=$bounds", bounds.height >= 48f)
  }

  private fun assertBattlefieldBudget() {
    val top = rule.onNodeWithTag("tower_battle_top_hud").fetchSemanticsNode().boundsInRoot
    val joystick = rule.onNodeWithTag("virtual_joystick").fetchSemanticsNode().boundsInRoot
    assertTrue("Reward choices leave >=96dp exposed world: top=$top joystick=$joystick", joystick.top - top.bottom >= 96f)
  }

  private fun capture(name: String) {
    val target = File("build/visual_screenshots/tower-field-rewards/$name.png")
    rule.runOnIdle {
      target.parentFile!!.mkdirs()
      val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
      host.draw(Canvas(bitmap))
      target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
      bitmap.recycle()
    }
    File(target.parentFile, "${name}_semantics.txt").writeText(rule.onRoot().printToString())
  }
}
