package com.example.ui

import android.graphics.Bitmap
import android.view.View
import androidx.compose.ui.platform.LocalView
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.MasterProfile
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import com.example.ui.screens.*
import com.example.ui.theme.EternalSummonerTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h360dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TowerGateMapTest {
  @get:Rule val rule = createComposeRule()
  private val heroes = listOf(HeroClassType.KNIGHT, HeroClassType.MAGE, HeroClassType.ARCHER).mapIndexed { i, cls ->
    HeroRecord(id = "gate_$i", name = "Guard $i", heroClass = cls.name, inActiveSquad = true)
  }
  private val state get() = SummonerUiState(isBootstrapping = false,
    profile = MasterProfile(id = "gate_test", highestClearedFloor = 0), heroes = heroes)

  @Test fun naturalSixSecondCutsceneReachesUnobstructedThreeGates() {
    rule.mainClock.autoAdvance = false
    rule.setContent { EternalSummonerTheme { FloorSelectionScreen(state, {}, {}, playEntryCutscene = true) } }
    rule.mainClock.advanceTimeBy(1500)
    rule.onNodeWithTag("tower_gate_cutscene_canvas").assertExists()
    rule.onNodeWithTag("nexus_floor_picker").assertDoesNotExist()
    rule.mainClock.advanceTimeBy(4900)
    rule.onNodeWithTag("tower_gate_cutscene_canvas").assertDoesNotExist()
    for (id in listOf("event", "tower", "custom")) rule.onNodeWithTag("nexus_gate_$id").assertIsDisplayed()
    rule.onNodeWithTag("nexus_floor_picker").assertDoesNotExist()
  }

  @Test fun skipPhysicalPortalTouchesLockedFloorAndBackPreserveNavigation() {
    var selected = 0
    var backs = 0
    var dispatcher: OnBackPressedDispatcher? = null
    var view: View? = null
    rule.mainClock.autoAdvance = false
    rule.setContent {
      view = LocalView.current
      dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
      EternalSummonerTheme { FloorSelectionScreen(state, { selected = it.floorNumber }, { backs++ }, true, sceneActive = false) }
    }
    rule.mainClock.advanceTimeBy(64)
    rule.onNodeWithTag("skip_gate_cutscene_button").performClick()
    rule.mainClock.autoAdvance = true
    rule.waitForIdle()
    rule.runOnIdle {
      val host = requireNotNull(view)
      val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
      host.draw(android.graphics.Canvas(bitmap))
      val file = File("build/visual_screenshots/tower_map/nexus_compose_view.png")
      file.parentFile!!.mkdirs()
      file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    for (id in listOf("event", "custom", "tower")) {
      rule.onNodeWithTag("nexus_gate_$id").performTouchInput { click(center) }
      rule.onNodeWithTag("nexus_floor_picker").assertIsDisplayed()
      rule.onNodeWithTag("enter_floor_1_button").performScrollTo().assertIsEnabled()
      rule.onNodeWithTag("close_nexus_floor_picker").performClick()
      rule.onNodeWithTag("nexus_floor_picker").assertDoesNotExist()
    }
    rule.onNodeWithTag("nexus_gate_tower").performTouchInput { click(center) }
    rule.onNodeWithTag("enter_floor_2_button").performScrollTo().assertIsNotEnabled()
    assertEquals(0, selected)
    rule.onNodeWithTag("enter_floor_1_button").performScrollTo().performClick()
    assertEquals(1, selected)
    rule.onNodeWithTag("close_nexus_floor_picker").performClick()
    rule.runOnIdle { requireNotNull(dispatcher).onBackPressed() }
    rule.waitForIdle()
    assertEquals(1, backs)
  }

  @Test @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun portraitGateTargetsAreSeparatedAndFloorOneRemainsReachable() {
    var selected = 0
    rule.setContent { EternalSummonerTheme { FloorSelectionScreen(state, { selected = it.floorNumber }, {}, sceneActive = false) } }
    val bounds = listOf("event", "tower", "custom").map {
      rule.onNodeWithTag("nexus_gate_$it").fetchSemanticsNode().boundsInRoot
    }
    assertTrue(bounds.zipWithNext().all { (a, b) -> a.right < b.left })
    assertTrue(bounds.all { it.width >= 48 * 2 }) // xhdpi, real targets remain >=48dp.
    rule.onNodeWithTag("nexus_gate_tower").performTouchInput { click(center) }
    rule.onNodeWithTag("enter_floor_1_button").performScrollTo().performClick()
    assertEquals(1, selected)
  }

  @Test fun carpetTracksTheActualSlopingRoadInsteadOfAnIndependentVerticalAxis() {
    val paths = SanctuaryLobbyEngine().mapLayout.paths
    val approach = com.example.game.pixel.SanctuaryEntranceApproach
    assertEquals(1545f, approach.centerX(1735f, paths), .01f)
    assertEquals(1577.5f, approach.centerX(1987.5f, paths), .01f)
    assertEquals(1610f, approach.centerX(2240f, paths), .01f)
    assertEquals(1610f, approach.centerX(2390f, paths), .01f)
  }

  @Test fun productionRendererCapturesArchitectureAnimationAndSanctuaryApproach() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(12f, WorldWeather.CLEAR)
    WorldEnvironment.update(50000)
    try {
      val art = TowerGateMapArt(context.assets)
      fun render(name: String, w: Int = 1600, h: Int = 720, scene: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(w.toFloat(), h.toFloat()), scene)
        val file = File("build/visual_screenshots/tower_map/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 10000)
        return bitmap
      }
      val closed = render("court_closed") { drawTowerGateMap(art, true, 0f, heroes) }
      val open = render("court_open") { drawTowerGateMap(art, true, 4.3f, heroes) }
      render("court_awakening") { drawTowerGateMap(art, true, 1.4f, heroes) }
      render("court_parting") { drawTowerGateMap(art, true, 2.8f, heroes) }
      render("court_flash") { drawTowerGateMap(art, true, 5.8f, heroes) }
      assertFalse("Opening must visibly change rendered pixels", closed.sameAs(open))
      render("nexus_landscape") { drawTowerGateMap(art, false, 1.2f, heroes) }
      render("nexus_portrait", 720, 1280) { drawTowerGateMap(art, false, 1.2f, heroes) }
      val portraitSquad = render("court_portrait_mdpi", 360, 640) { drawTowerGateMap(art, true, 4.3f, heroes) }
      val emptyCourt = render("court_portrait_mdpi_empty", 360, 640) { drawTowerGateMap(art, true, 4.3f, emptyList()) }
      val vp = GateMapViewport(360f, 640f)
      for (i in heroes.indices) {
        val x = 480f + (i - 1) * 44f
        val region = vp.screenRect(androidx.compose.ui.geometry.Rect(x - 18f, 360f, x + 18f, 440f))
        var changed = 0
        for (py in region.top.toInt() until region.bottom.toInt()) for (px in region.left.toInt() until region.right.toInt()) {
          if (portraitSquad.getPixel(px, py) != emptyCourt.getPixel(px, py)) changed++
        }
        assertTrue("Squad slot $i must not be culled on a 360px portrait canvas", changed > 12)
      }
      val layout = SanctuaryLobbyEngine().mapLayout
      for ((name, camera) in listOf(
        "approach_z088" to floatArrayOf(1610f, 2020f, .88f),
        "approach_z148" to floatArrayOf(1610f, 2020f, 1.48f),
        "overview_z036" to floatArrayOf(1600f, 1380f, .36f),
      )) {
        render(name, 1280, 720) {
          val (x, y, z) = camera
          drawSanctuaryFloatingWorld(x, y, z, 1.2f, layout.paths)
          drawGroundShadows(layout.obstacles, x, y, z, clipToIsland = true)
          layout.obstacles.sortedBy { it.y + it.sortYBias }.forEach { drawProp(it, x, y, z, 1.2f, renderShadow = null) }
        }
      }
    } finally { WorldEnvironment.debugOverride(); WorldEnvironment.update() }
  }
}
