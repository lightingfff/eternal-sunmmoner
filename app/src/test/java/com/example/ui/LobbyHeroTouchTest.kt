package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.*
import com.example.data.model.*
import com.example.game.map.ObliqueCamera
import com.example.game.tower.TowerFloorCatalog
import com.example.ui.screens.SanctuaryLobbyScreen
import com.example.game.story.ManhwaStoryData
import com.example.ui.theme.EternalSummonerTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36],qualifiers="w800dp-h360dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LobbyHeroTouchTest {
  @get:Rule val rule=createComposeRule()
  private var view: View?=null
  private val received=mutableListOf<Pair<String,HeroAssignment>>()
  private val openedFacilities=mutableListOf<FacilityType>()
  private fun start(accept:Boolean=true) {
    rule.mainClock.autoAdvance=false
    val hero=HeroRecord(id="touch_hero",name="Kaelen",currentHp=50,maxHp=100)
    val initial=SummonerUiState(isBootstrapping=false,isGuestAccount=true,
      profile=MasterProfile(id="touch",storyFlags=listOf(ManhwaStoryData.FLAG_PROLOGUE_SEEN)),heroes=listOf(hero))
    rule.setContent {
      view=LocalView.current
      EternalSummonerTheme {
        SanctuaryLobbyScreen(initial,{ openedFacilities += it }, {}, {}, {_,_->}, {_,_->}, {}, {}, {_,_->}, {}, {}, {}, {},
          settings=GameSettings(defaultZoom=.36f),sceneActive=false,
          onDropHero={ id, assignment -> received+=id to assignment; accept })
      }
    }
    rule.mainClock.advanceTimeBy(100)
    rule.waitForIdle()
  }
  private fun screen(wx:Float,wy:Float):Offset {
    val rect=rule.onNodeWithTag("sanctuary_canvas").fetchSemanticsNode().boundsInRoot
    val layout=TowerFloorCatalog.buildSanctuaryMap()
    val cx=ObliqueCamera.clampCamX(1660f,layout.width,layout.wallMargin,.36f,rect.width/2f)
    val cy=ObliqueCamera.clampCamY(1260f,layout.height,layout.wallMargin,.36f,rect.height/2f,northHeadroomWorld=220f)
    return Offset(ObliqueCamera.worldToScreenX(wx,cx,.36f,rect.width/2f),ObliqueCamera.worldToScreenY(wy,cy,.36f,rect.height/2f))
  }
  private fun hold() {
    val start=screen(1785f,1185f)-Offset(0f,12f)
    rule.onNodeWithTag("sanctuary_canvas").performTouchInput { down(start) }
    rule.mainClock.advanceTimeBy(700)
    rule.waitForIdle()
  }
  private fun release(point:Offset) {
    rule.onNodeWithTag("sanctuary_canvas").performTouchInput { moveTo(point,100);up() }
    rule.mainClock.advanceTimeBy(100)
    rule.waitForIdle()
  }
  private fun capture(name:String) {
    val host=requireNotNull(view);val bitmap=Bitmap.createBitmap(host.width,host.height,Bitmap.Config.ARGB_8888)
    host.draw(Canvas(bitmap));val file=File("build/visual_screenshots/lobby_hero/$name.png")
    file.parentFile!!.mkdirs();file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    assertTrue(file.length()>10000)
  }
  @Test fun longPressPhysicalDragToGateDispatchesSinglePartyAssignment() {
    start();hold();capture("held_hero")
    release(screen(1960f,620f));assertEquals(listOf("touch_hero" to HeroAssignment.IN_PARTY),received)
    capture("released_at_gate")
  }
  @Test fun longPressPhysicalDragToHouseDispatchesRestingAssignment() {
    start();hold();release(screen(825f,820f))
    assertEquals(listOf("touch_hero" to HeroAssignment.RESTING),received)
  }
  @Test fun fastDragIsCameraPanAndInvalidDropAndCancelNeverAssign() {
    start()
    val node=rule.onNodeWithTag("sanctuary_canvas")
    val from=screen(1785f,1185f);val to=screen(1960f,620f)
    node.performTouchInput { swipe(from,to,120) }
    rule.mainClock.advanceTimeBy(100);assertTrue(received.isEmpty())
    // Overview clamp leaves the same world camera after this pan, so pickup stays reproducible.
    hold();release(screen(1400f,1100f));assertTrue(received.isEmpty())
    hold();node.performTouchInput { cancel() };rule.mainClock.advanceTimeBy(100)
    assertTrue(received.isEmpty())
  }
  @Test fun failedPersistenceRollsBackSoOriginalHeroCanBePickedAgain() {
    start(accept=false)
    repeat(2) { hold();release(screen(1960f,620f)) }
    assertEquals(2, received.size)
  }
  @Test fun secondFingerCancelsHeldHeroWithoutAssignment() {
    start();hold()
    val node=rule.onNodeWithTag("sanctuary_canvas")
    val second=screen(1800f,1200f);val target=screen(1960f,620f)
    node.performTouchInput { down(1,second) }
    rule.mainClock.advanceTimeBy(100)
    node.performTouchInput { up(1);moveTo(0,target);up(0) }
    rule.mainClock.advanceTimeBy(100);assertTrue(received.isEmpty())
  }
  @Test fun tappingHouseOrTowerGateWithoutDraggingHeroOpensFacilityUiDirectly() {
    start()
    val node=rule.onNodeWithTag("sanctuary_canvas")
    val housePoint=screen(735f,875f)
    val gatePoint=screen(1960f,585f)
    node.performTouchInput { click(housePoint) }
    rule.mainClock.advanceTimeBy(100)
    node.performTouchInput { click(gatePoint) }
    rule.mainClock.advanceTimeBy(100)
    assertEquals(listOf(FacilityType.HERO_QUARTERS, FacilityType.TOWER_GATE), openedFacilities)
    assertTrue(received.isEmpty())
  }
}

