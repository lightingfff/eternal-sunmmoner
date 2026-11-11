package com.example.ui

import android.content.Context
import android.content.ContextWrapper
import com.example.data.local.LocalGuestSaveManager
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.platform.LocalView
import java.io.File
import androidx.compose.ui.input.key.Key
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
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class Floor1InputOwnershipTest {
  @get:Rule val rule = createComposeRule()
  private lateinit var host: View

  private fun start(initialState: SummonerUiState? = null) {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    // Keep the actual scene active, but control frames so a 250 ms HUD poll cannot
    // accidentally make an input acknowledgment appear immediate.
    rule.mainClock.autoAdvance = false
    val state = initialState ?: SummonerUiState(
      isBootstrapping = false, profile = MasterProfile(id = "floor1_input"),
      selectedFloor = TowerFloorCatalog.FLOORS.first(),
      heroes = listOf(HeroRecord(id = "floor1_input_knight", name = "Kaelen", inActiveSquad = true)),
    )
    rule.setContent {
      host = LocalView.current
      EternalSummonerTheme {
        TowerBattleScreen(state, onFinishExpedition = { _, _ -> }, onReturnToSanctuary = {})
      }
    }
    repeat(4) {
      rule.mainClock.advanceTimeByFrame()
      if (rule.onAllNodesWithTag("start_combat_button").fetchSemanticsNodes().isNotEmpty()) {
        rule.onNodeWithTag("start_combat_button").performClick()
      }
    }
    rule.mainClock.advanceTimeByFrame()
    rule.onNodeWithTag("start_combat_button").assertDoesNotExist()
  }

  @Test @Config(qualifiers = "w800dp-h600dp-land-mdpi")
  fun landscapeCommandsAcknowledgeBeforeHudPollAndModalOwnsTouch() = verifyInputOwnership()

  @Test @Config(qualifiers = "w400dp-h800dp-port-mdpi")
  fun portraitCommandsAcknowledgeBeforeHudPollAndModalOwnsTouch() = verifyInputOwnership()

  @Test @Config(qualifiers = "w800dp-h360dp-land-mdpi")
  fun shortLandscapeCommandsAndModalRemainReachable() = verifyInputOwnership()

  @Test @Config(qualifiers = "w400dp-h800dp-port-mdpi")
  fun actualStarterPartyTravelsToFieldworkThroughProductionControls() {
    val base = RuntimeEnvironment.getApplication() as Context
    val files = Files.createTempDirectory(base.cacheDir.toPath(), "floor1-ui-evidence-").toFile()
    val context = object : ContextWrapper(base) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = files
    }
    val initial = runBlocking { LocalGuestSaveManager(context).startOrResumeGuestSession() }
    start(SummonerUiState(isBootstrapping = false, profile = initial.profile,
      selectedFloor = TowerFloorCatalog.FLOORS.first(), heroes = initial.heroes,
      equipment = initial.equipment))
    rule.onNodeWithTag("rescue_journey_follow").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(6_000L)
    // A real starter party must clear the path guards. Allow their last attack
    // contact to finish rather than bypassing actors or injecting encounter flags.
    repeat(3) {
      if (rule.onAllNodesWithTag("rescue_field_interact").fetchSemanticsNodes().isEmpty()) {
        rule.mainClock.advanceTimeBy(2_000L)
      }
    }
    capture("05_watchpost_field_action")
    rule.onNodeWithTag("rescue_field_interact").assertIsEnabled().performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    rule.onNodeWithTag("rescue_journey_interact").assertIsEnabled().performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    rule.onNodeWithText("Thu hồi bản đồ ở rương Bắc", substring = true).assertExists()
    rule.onNodeWithTag("rescue_journey_follow").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(15_000L)
    capture("06_lookout_live_fieldwork")
    val top = rule.onNodeWithTag("tower_battle_top_hud").fetchSemanticsNode().boundsInRoot
    val joystick = rule.onNodeWithTag("virtual_joystick").fetchSemanticsNode().boundsInRoot
    assertTrue("Fieldwork actions must leave an exposed world area", top.bottom + 32f < joystick.top)
    rule.onNodeWithTag("rescue_journey_objective").assertIsDisplayed()
    rule.onNodeWithTag("rescue_journey_follow").assertIsDisplayed()
    rule.onNodeWithText("Thu hồi bản đồ ở rương Bắc", substring = true).assertExists()
  }

  private fun verifyInputOwnership() {
    start()
    val top = rule.onNodeWithTag("tower_battle_top_hud").fetchSemanticsNode().boundsInRoot
    val joystick = rule.onNodeWithTag("virtual_joystick").fetchSemanticsNode().boundsInRoot
    assertTrue("Objective HUD must leave an exposed world area above controls", top.bottom + 32f < joystick.top)
    rule.onNodeWithTag("rescue_journey_objective").assertIsDisplayed()
    rule.onNodeWithTag("rescue_journey_follow").assertIsDisplayed()
    rule.onNodeWithTag("rescue_journey_interact").assertIsDisplayed()
    capture("01_objective")
    val clockBefore = rule.mainClock.currentTime
    rule.onNodeWithTag("order_hold_position").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    assertTrue("Acknowledgment must precede periodic 250ms HUD sampling",
      rule.mainClock.currentTime - clockBefore < 250L)
    rule.onNodeWithText("Giữ vững đội hình tại chỗ", substring = true).assertExists()

    capture("02_hold_ack")
    val blockedOrderPosition = rule.onNodeWithTag("order_follow").fetchSemanticsNode().boundsInRoot.center
    rule.onNodeWithTag("retreat_mission_button").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(32L)
    rule.onNodeWithText("Rút Lui Về Thánh Địa?").assertExists()
    rule.onNodeWithTag("retreat_confirmation_overlay").assertExists()
    capture("03_retreat_modal")
    rule.onRoot().performKeyInput { pressKey(Key.One) }

    // A coordinate tap exercises physical hit testing, rather than invoking the
    // underlying control's accessibility click action through a modal.
    rule.onRoot().performTouchInput { click(blockedOrderPosition) }
    rule.mainClock.advanceTimeBy(32L)
    rule.onNodeWithText("Rút Lui Về Thánh Địa?").assertExists()
    rule.onNodeWithText("Tiếp Tục").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(32L)
    rule.onNodeWithText("Rút Lui Về Thánh Địa?").assertDoesNotExist()
    rule.onNodeWithText("Giữ vững đội hình tại chỗ", substring = true).assertExists()
    capture("04_resume")
  }

  private fun capture(moment: String) {
    val bounds = rule.onRoot().fetchSemanticsNode().boundsInRoot
    val orientation = if (bounds.width > bounds.height) {
      if (bounds.height <= 400f) "landscape_short" else "landscape"
    } else "portrait"
    val output = File("build/visual_screenshots/floor1/${orientation}_$moment")
    val tags = listOf("tower_battle_top_hud", "rescue_journey_hud", "rescue_journey_follow",
      "rescue_journey_interact", "rescue_field_interact", "rescue_field_travel", "virtual_joystick", "order_hold_position", "retreat_mission_button")
    val rows = tags.mapNotNull { tag ->
      val node = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull() ?: return@mapNotNull null
      val r = node.boundsInRoot
      "$tag,${r.left},${r.top},${r.right},${r.bottom}"
    }
    rule.runOnIdle {
      output.parentFile!!.mkdirs()
      val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
      host.draw(Canvas(bitmap))
      File("${output.path}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
      bitmap.recycle()
      File("${output.path}_hitboxes.csv").writeText("tag,left,top,right,bottom\n" + rows.joinToString("\n"))
    }
    File("${output.path}_semantics.txt").writeText(rule.onRoot().printToString())
  }
}
