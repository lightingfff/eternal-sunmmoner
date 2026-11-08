package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.*
import com.example.game.tower.TowerFloorCatalog
import com.example.ui.screens.TowerBattleScreen
import com.example.ui.theme.EternalSummonerTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class BattleTouchOwnershipTest {
  @get:Rule val rule=createComposeRule()
  private var host:View?=null
  private fun start() {
    com.example.game.pixel.SanctuaryArtAssets.init(org.robolectric.RuntimeEnvironment.getApplication())
    // Simulation is stopped by sceneActive=false; Compose's clock must run to
    // complete pointer-dispatch coroutines and recompose their acknowledgement.
    rule.mainClock.autoAdvance=true
    val state=SummonerUiState(isBootstrapping=false,profile=MasterProfile(id="touch_master"),
      selectedFloor=TowerFloorCatalog.FLOORS[1],
      heroes=listOf(HeroRecord(id="touch_knight",name="Kaelen",inActiveSquad=true)))
    rule.setContent {
      host=LocalView.current
      EternalSummonerTheme {
        // Freeze simulation to measure input acknowledgement independently of the 250ms HUD poll.
        TowerBattleScreen(state,onFinishExpedition={_,_->},onReturnToSanctuary={},sceneActive=false)
      }
    }
    repeat(8) {
      if(rule.onAllNodesWithTag("start_combat_button").fetchSemanticsNodes().isNotEmpty()) {
        rule.onNodeWithTag("start_combat_button").performClick()
        rule.mainClock.advanceTimeBy(64L)
      }
    }
    rule.waitForIdle()
  }
  private fun snap(name:String) {
    val hitboxes=if(name.endsWith("_world_beacon"))
      listOf("tower_battle_top_hud","battle_order_strip","virtual_joystick","zoom_in_button","zoom_out_button","skill_btn_touch_knight").map {
        it to rule.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot
      } else emptyList()
    rule.runOnIdle {
      val view=host!!;val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
      val canvas=Canvas(b);view.draw(canvas)
      val f=File("build/visual_screenshots/p30/$name.png");f.parentFile!!.mkdirs()
      f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) }
      if(name.endsWith("_world_beacon")) {
        val paint=android.graphics.Paint().apply { color=android.graphics.Color.MAGENTA;style=android.graphics.Paint.Style.STROKE;strokeWidth=2f }
        val bounds=StringBuilder("tag,left,top,right,bottom\n")
        for((tag,r) in hitboxes) {
          canvas.drawRect(r.left,r.top,r.right,r.bottom,paint)
          bounds.append("$tag,${r.left},${r.top},${r.right},${r.bottom}\n")
        }
        File(f.parentFile,"${name}_hitboxes.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) }
        File(f.parentFile,"${name}_hitboxes.csv").writeText(bounds.toString())
      }
      b.recycle()
    }
  }
  private fun worldPixels(top:Float,bottom:Float):IntArray {
    var pixels=IntArray(0)
    rule.runOnIdle {
      val view=host!!;val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
      view.draw(Canvas(b));val y=top.toInt()+4;val h=(bottom.toInt()-y-4).coerceAtLeast(1)
      pixels=IntArray((view.width-24)*h)
      b.getPixels(pixels,0,view.width-24,12,y,view.width-24,h);b.recycle()
    }
    return pixels
  }
  private fun tap(tag:String) {
    repeat(5) {
      rule.onNodeWithTag("battle_order_strip").performTouchInput {
        if(tag=="order_move_to") swipeLeft(durationMillis=120) else swipeRight(durationMillis=120)
      }
      rule.mainClock.advanceTimeBy(160L)
    }
    rule.onNodeWithTag(tag).performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L);rule.waitForIdle()
  }
  @Test @Config(qualifiers="w800dp-h360dp-land-mdpi")
  fun landscapeCommandsAndWorldHaveSeparateTouchOwnership() = ownership("landscape")
  @Test @Config(qualifiers="w360dp-h640dp-port-mdpi")
  fun portraitCommandsAndWorldHaveSeparateTouchOwnership() = ownership("portrait")

  @Test @Config(qualifiers="w360dp-h640dp-port-mdpi")
  fun portraitWorldTapBeforeZoom() = ownership("portrait_no_zoom", withZoom=false)

  private fun ownership(name:String, withZoom:Boolean=true) {
    start();snap("${name}_baseline")
    rule.onNodeWithTag("order_hold_position").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    rule.onNodeWithText("Giữ vững đội hình tại chỗ",substring=true).assertExists()
    snap("${name}_hold_ack")
    tap("order_move_to")
    snap("${name}_move_waiting")
    rule.onNodeWithText("Chạm lên mặt đất",substring=true).assertExists()
    // Another UI command must cancel pending placement, not commit a beacon behind itself.
    tap("order_hold_position")
    rule.onNodeWithText("Giữ vững đội hình tại chỗ",substring=true).assertExists()
    rule.onNodeWithText("Đã cắm cờ",substring=true).assertDoesNotExist()
    tap("order_move_to")
    // Zoom and skill controls own their taps too; they must not place the waiting beacon.
    if(withZoom) {
      rule.onNodeWithTag("zoom_in_button").performTouchInput { click() }
      rule.mainClock.advanceTimeBy(64L)
      rule.waitForIdle()
    }
    rule.onNodeWithText("Đã cắm cờ",substring=true).assertDoesNotExist()
    // Click uncovered world, outside the actual rendered HUD/control rectangles.
    val top=rule.onNodeWithTag("tower_battle_top_hud").fetchSemanticsNode().boundsInRoot
    val joy=rule.onNodeWithTag("virtual_joystick").fetchSemanticsNode().boundsInRoot
    val world=Offset(30f,(top.bottom+joy.top)/2f)
    assertTrue("World gap must be visible",joy.top-top.bottom>40f)
    if(withZoom) {
      val beforePan=worldPixels(top.bottom,joy.top)
      rule.onNodeWithTag("tower_battle_canvas").performTouchInput {
        // Pan into the world, away from the spawn-side camera clamp.
        swipe(Offset(230f,world.y),Offset(30f,world.y),durationMillis=160)
      }
      rule.waitForIdle()
      val afterPan=worldPixels(top.bottom,joy.top)
      assertFalse("Pan must change the production world render",beforePan.contentEquals(afterPan))
      rule.onNodeWithText("Đã cắm cờ",substring=true).assertDoesNotExist()
      rule.onNodeWithTag("tower_battle_canvas").performTouchInput {
        down(0,Offset(130f,world.y));down(1,Offset(190f,world.y))
        moveTo(0,Offset(95f,world.y));moveTo(1,Offset(225f,world.y))
        up(0);up(1)
      }
      rule.waitForIdle()
      val afterPinch=worldPixels(top.bottom,joy.top)
      assertFalse("Pinch must change the production world render",afterPan.contentEquals(afterPinch))
      rule.onNodeWithText("Đã cắm cờ",substring=true).assertDoesNotExist()
    }
    rule.onRoot().performTouchInput { click(world) }
    rule.mainClock.advanceTimeBy(64L);rule.waitForIdle()
    snap("${name}_after_world_tap")
    File("build/visual_screenshots/p30/${name}_semantics.txt").writeText(rule.onRoot().printToString())
    rule.onNodeWithText("Đã cắm cờ điều quân",substring=true).assertExists()
    snap("${name}_world_beacon")
    rule.onNodeWithTag("skill_btn_touch_knight").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    rule.onNodeWithText("Lệnh Master: Kaelen kích hoạt",substring=true).assertExists()
    snap("${name}_skill")
    rule.onRoot().performKeyInput { pressKey(Key.Six) }
    rule.waitForIdle()
    rule.onNodeWithText("Tập hợp đội hình",substring=true).assertExists()
  }
}
