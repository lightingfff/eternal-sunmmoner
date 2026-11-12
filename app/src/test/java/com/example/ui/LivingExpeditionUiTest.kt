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
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.tower.TowerFloorCatalog
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

/** Production screen, real guest party and physical touch; native host renders are not phone FPS. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LivingExpeditionUiTest {
  @get:Rule val rule = createComposeRule()
  private lateinit var host: View

  @Test @Config(qualifiers = "w400dp-h800dp-port-mdpi")
  fun portraitRealPartyInvestigationAndMasterOverride() = verifyProductionExploration("portrait")

  @Test @Config(qualifiers = "w800dp-h360dp-land-mdpi")
  fun shortLandscapeLeavesWorldVisibleThroughAnActualDecision() = verifyProductionExploration("landscape_short")

  @Test @Config(qualifiers = "w400dp-h800dp-port-mdpi")
  fun southernWitnessMovesWithTheRealPartyAfterAPhysicalRescueChoice() {
    start()
    rule.onNodeWithTag("living_lead_south").performTouchInput { click() }
    var reachedDecision = false
    repeat(90) {
      if (!reachedDecision) {
        rule.mainClock.advanceTimeBy(1_000L)
        reachedDecision = rule.onAllNodesWithTag("living_decision_south_rescue").fetchSemanticsNodes().isNotEmpty()
      }
    }
    capture(if (reachedDecision) "portrait_south_00_decision" else "portrait_south_timeout")
    assertTrue("Actual guest heroes must reach the wounded witness through production controls. " +
      "Clock=${rule.mainClock.currentTime}ms; semantics:\n${rule.onRoot().printToString()}", reachedDecision)
    assertTouchTarget("living_decision_south_rescue")
    assertTouchTarget("living_decision_south_medicine")
    assertBattlefieldBudget()
    rule.onNodeWithTag("living_decision_south_rescue").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    rule.onNodeWithTag("living_decision_south_rescue").assertDoesNotExist()
    capture("portrait_south_01_witness_escort")
    rule.mainClock.advanceTimeBy(1_200L)
    capture("portrait_south_02_witness_escort")
  }

  private fun start() {
    val base = RuntimeEnvironment.getApplication() as Context
    val files = Files.createTempDirectory(base.cacheDir.toPath(), "living-ui-").toFile()
    val context = object : ContextWrapper(base) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = files
    }
    val guest = runBlocking { LocalGuestSaveManager(context).startOrResumeGuestSession() }
    assertEquals(3, guest.heroes.count { it.inActiveSquad })
    SanctuaryArtAssets.init(base)
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
    rule.onNodeWithTag("start_combat_button").assertDoesNotExist()
    rule.onNodeWithTag("living_expedition_hud").assertIsDisplayed()
    rule.onNodeWithTag("rescue_journey_hud").assertDoesNotExist()
  }

  private fun verifyProductionExploration(orientation: String) {
    start()
    assertBattlefieldBudget()
    listOf("living_resume", "living_lead_north", "living_lead_south", "living_lead_east").forEach {
      assertTouchTarget(it)
    }
    capture("${orientation}_01_exploring")
    rule.onNodeWithTag("living_lead_north").performTouchInput { click() }
    var reachedDecision = false
    repeat(90) {
      if (!reachedDecision) {
        rule.mainClock.advanceTimeBy(1_000L)
        reachedDecision = rule.onAllNodesWithTag("living_decision_north_scout").fetchSemanticsNodes().isNotEmpty()
      }
    }
    capture("${orientation}_02_" + if (reachedDecision) "north_decision" else "north_timeout")
    assertTrue("Actual guest heroes must reach the northern investigation without teleporting. " +
      "Clock=${rule.mainClock.currentTime}ms; semantics:\n${rule.onRoot().printToString()}", reachedDecision)
    rule.onNodeWithTag("living_expedition_objective").assertIsDisplayed()
    assertTouchTarget("living_decision_north_scout")
    assertTouchTarget("living_decision_north_assault")
    assertBattlefieldBudget()
    rule.onNodeWithTag("living_decision_north_scout").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    rule.onNodeWithTag("living_decision_north_scout").assertDoesNotExist()
    rule.onNodeWithTag("living_decision_north_assault").assertDoesNotExist()

    val before = rule.mainClock.currentTime
    rule.onNodeWithTag("order_hold_position").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    assertTrue(rule.mainClock.currentTime - before < 250L)
    rule.onNodeWithText("Giữ vững đội hình tại chỗ", substring = true).assertExists()
    rule.onNodeWithText("Đang chờ lệnh").assertExists()
    rule.onNodeWithTag("living_resume").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    rule.onNodeWithText("Đội tự thám hiểm").assertExists()
    rule.onNodeWithTag("living_expedition_objective").assertIsDisplayed()
    capture("${orientation}_03_resume_after_hold")
    if (orientation == "portrait") {
      repeat(8) { frame ->
        rule.mainClock.advanceTimeBy(400L)
        capture("portrait_role_sequence_${frame.toString().padStart(2, '0')}")
      }
    }
  }

  private fun assertTouchTarget(tag: String) {
    val node = rule.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode()
    assertTrue("$tag must be at least 48dp tall", node.boundsInRoot.height >= 48f)
    assertTrue("$tag must be at least 48dp wide", node.boundsInRoot.width >= 48f)
  }

  private fun assertBattlefieldBudget() {
    val top = rule.onNodeWithTag("tower_battle_top_hud").fetchSemanticsNode().boundsInRoot
    val joystick = rule.onNodeWithTag("virtual_joystick").fetchSemanticsNode().boundsInRoot
    assertTrue("Live decisions must leave at least 96dp exposed world above controls: top=$top joystick=$joystick",
      joystick.top - top.bottom >= 96f)
  }

  private fun capture(moment: String) {
    val output = File("build/visual_screenshots/living-expedition/$moment")
    val tags = listOf("living_expedition_hud", "living_expedition_objective", "living_resume",
      "living_lead_north", "living_lead_south", "living_lead_east",
      "living_decision_north_scout", "living_decision_north_assault",
      "living_decision_south_rescue", "living_decision_south_medicine")
    val rows = tags.mapNotNull { tag ->
      val node = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull() ?: return@mapNotNull null
      val r = node.boundsInRoot
      "$tag,${r.left},${r.top},${r.right},${r.bottom}"
    }.joinToString("\n")
    rule.runOnIdle {
      output.parentFile!!.mkdirs()
      val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
      host.draw(Canvas(bitmap))
      File("${output.path}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
      bitmap.recycle()
      File("${output.path}_hitboxes.csv").writeText("tag,left,top,right,bottom\n$rows")
    }
    File("${output.path}_semantics.txt").writeText(rule.onRoot().printToString())
  }
}
