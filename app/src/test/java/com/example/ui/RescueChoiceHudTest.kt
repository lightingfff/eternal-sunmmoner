package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.platform.LocalView
import java.io.File
import org.robolectric.annotation.GraphicsMode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.components.CourierMissionHud
import com.example.ui.theme.EternalSummonerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h640dp-port-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RescueChoiceHudTest {
  @get:Rule val rule = createComposeRule()

  @Test fun suppliesChoiceDisappearsAfterCommitAndProvidesActionableWaypoint() {
    var choiceAvailable by mutableStateOf(true)
    var suppliesPending by mutableStateOf(false)
    var targets = 0
    var hostView: View? = null
    rule.setContent {
      EternalSummonerTheme {
        hostView = LocalView.current
        CourierMissionHud("Hộ tống tới cổng", "Túi tiếp vận trên đường mòn", 200, 300, .3f,
          true, {}, {}, modifier=Modifier.fillMaxWidth(), choiceAvailable=choiceAvailable,
          onRecoverSupplies={ choiceAvailable=false; suppliesPending=true },
          suppliesPending=suppliesPending, onGoToSupplies={ targets++ })
      }
    }
    rule.onNodeWithTag("courier_support_choice").assertIsDisplayed()
    rule.onNodeWithTag("courier_supplies_choice").assertIsDisplayed()
    saveView(hostView!!, "choice_portrait.png")
    rule.onNodeWithTag("courier_supplies_choice").performClick()
    rule.onNodeWithTag("courier_support_choice").assertDoesNotExist()
    rule.onNodeWithTag("courier_supplies_choice").assertDoesNotExist()
    rule.onNodeWithTag("courier_supply_target").assertIsDisplayed().performClick()
    assertEquals(1,targets)
  }

  @Test
  @Config(qualifiers="w640dp-h360dp-land-xhdpi")
  fun compactHudKeepsBothChoiceActionsVisible() {
    var supports = 0
    var hostView: View? = null
    rule.setContent {
      EternalSummonerTheme {
        hostView = LocalView.current
        CourierMissionHud("Hộ tống tới cổng", "Chọn ưu tiên", 200, 300, .3f,
          true, {}, {}, modifier=Modifier.fillMaxWidth(), compact=true,
          choiceAvailable=true,onSupport={ supports++ })
      }
    }
    rule.onNodeWithTag("courier_supplies_choice").assertIsDisplayed()
    rule.onNodeWithTag("courier_support_choice").assertIsDisplayed()
    saveView(hostView!!, "choice_landscape.png")
    rule.onNodeWithTag("courier_support_choice").performClick()
    assertEquals(1,supports)
  }
  private fun saveView(view: View, name: String) {
    rule.runOnIdle {
      val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
      view.draw(Canvas(bitmap))
      val output = File("build/visual_screenshots/mission01", name)
      output.parentFile!!.mkdirs()
      output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
      bitmap.recycle()
    }
  }

}
