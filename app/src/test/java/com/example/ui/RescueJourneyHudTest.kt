package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.RescueJourneyHud
import com.example.ui.theme.EternalSummonerTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36],qualifiers="w360dp-h640dp-port-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RescueJourneyHudTest {
  @get:Rule val rule=createComposeRule()

  @Test fun investigationRequiresArrivalAndClearedThreatsBeforeItCanAdvance() {
    var canInteract by mutableStateOf(false)
    var completed by mutableIntStateOf(1)
    var following=0
    var host:View?=null
    rule.setContent {
      host=LocalView.current
      EternalSummonerTheme {
        RescueJourneyHud("Hồ sơ trạm bắc", "Bản đồ đã bị xé. Dọn cung thủ rồi khảo sát để tìm tuyến rút lui.",
          completed,7,280,300,canInteract,false,
          onFollow={following++},onInteract={completed++},onSilenceAlarm={},onRescueScout={},
          modifier=Modifier.fillMaxWidth())
      }
    }
    rule.onNodeWithTag("rescue_journey_follow").assertIsDisplayed().performClick()
    assertEquals(1,following)
    rule.onNodeWithTag("rescue_journey_interact").assertIsNotEnabled().performClick()
    assertEquals(1,completed)
    rule.runOnIdle { canInteract=true }
    rule.onNodeWithTag("rescue_journey_interact").assertIsEnabled()
    saveView(host!!,"journey_portrait.png")
    rule.onNodeWithTag("rescue_journey_interact").performClick()
    assertEquals(2,completed)
  }

  @Test
  @Config(qualifiers="w640dp-h360dp-land-xhdpi")
  fun signalDecisionHasVisibleConsequencesAndInvokesTheChosenAction() {
    var routeChoice:Boolean?=null
    var host:View?=null
    rule.setContent {
      host=LocalView.current
      EternalSummonerTheme {
        RescueJourneyHud("Trạm tín hiệu bị phá", "Trinh sát bị kẹt bên cơ cấu báo động. Chỉ kịp cứu người hoặc tắt chuông.",
          3,7,250,300,true,true,{}, {},{routeChoice=true},{routeChoice=false},
          modifier=Modifier.fillMaxWidth(),compact=true)
      }
    }
    rule.onNodeWithTag("rescue_journey_interact").assertIsNotEnabled()
    rule.onNodeWithTag("rescue_journey_silence").assertIsDisplayed().assertIsEnabled()
    rule.onNodeWithTag("rescue_journey_rescue_scout").assertIsDisplayed().assertIsEnabled()
    saveView(host!!,"journey_signal_landscape.png")
    rule.onNodeWithTag("rescue_journey_rescue_scout").performClick()
    assertEquals(false,routeChoice)
    rule.onNodeWithTag("rescue_journey_silence").performClick()
    assertEquals(true,routeChoice)
  }

  @Test fun fieldWorkRequiresArrivalAndAllowsExplicitMovementAndInteraction() {
    var arrived by mutableStateOf(false)
    var travelled = 0
    var examined = 0
    var host:View? = null
    rule.setContent {
      host = LocalView.current
      EternalSummonerTheme {
        RescueJourneyHud("Giải mã dấu vết", "Lần theo dấu cũ", 1, 7, 280, 300, false, false,
          onFollow={}, onInteract={}, onSilenceAlarm={}, onRescueScout={},
          modifier=Modifier.fillMaxWidth(), fieldTargetLabel="Đọc vết bánh xe",
          fieldHint="Đến dấu rồi đọc vết bánh xe.", fieldCanInteract=arrived,
          onFieldTravel={travelled++}, onFieldInteract={examined++})
      }
    }
    rule.onNodeWithTag("rescue_field_interact").assertIsNotEnabled().performClick()
    assertEquals(0, examined)
    rule.onNodeWithTag("rescue_field_travel").assertIsDisplayed().performClick()
    assertEquals(1, travelled)
    rule.runOnIdle { arrived = true }
    rule.onNodeWithTag("rescue_field_interact").assertIsEnabled()
    saveView(host!!,"journey_field_portrait.png")
    rule.onNodeWithTag("rescue_field_interact").performClick()
    assertEquals(1, examined)
  }

  private fun saveView(view:View,name:String) {
    rule.runOnIdle {
      val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
      view.draw(Canvas(bitmap))
      val file=File("build/visual_screenshots/mission01b",name)
      file.parentFile!!.mkdirs()
      file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
      bitmap.recycle()
    }
  }
}
