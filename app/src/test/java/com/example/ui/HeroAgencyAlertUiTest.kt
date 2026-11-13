package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalGuestSaveManager
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.platform.LocalView
import java.io.File
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.HeroRecord
import com.example.data.model.TacticalOrder
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.tower.TowerFloorCatalog
import com.example.ui.screens.HeroAgencyAlertCard
import com.example.ui.screens.issueRetreatFromConfirmation
import com.example.game.combat.CourierOrder
import com.example.ui.theme.EternalSummonerTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h640dp-port-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HeroAgencyAlertUiTest {
  @get:Rule val rule = createComposeRule()

  @Test fun regroupRespondsToRealFearAndRetreatOpensConfirmationOnly() {
    val engine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), listOf(HeroRecord(id="alert",name="Kaelen")), emptyList(), enableLostCourier=true)
    engine.stage = MissionStage.IN_PROGRESS
    engine.heroes.single().fearSecondsRemaining = 10f
    engine.step(.01f,0f,0f)
    assertNotNull(engine.heroAgencyAlert)
    val before = engine.heroes.single().fearSecondsRemaining
    var revision by mutableIntStateOf(0)
    var confirmation by mutableStateOf(false)
    var host:View? = null
    rule.setContent {
      host=LocalView.current
      val tick = revision
      if (tick >= 0) engine.heroAgencyAlert?.let { alert ->
        EternalSummonerTheme {
          HeroAgencyAlertCard(alert.heroName, alert.messageVi,
            onRegroup={engine.issueTacticalOrder(TacticalOrder.REGROUP); revision++},
            onProtect={engine.issueTacticalOrder(TacticalOrder.PROTECT_ALLY); revision++},
            onRetreat={confirmation=true}, onDismiss={engine.dismissHeroAgencyAlert(); revision++})
        }
      }
    }
    rule.onNodeWithTag("hero_alert_retreat").assertIsDisplayed()
    rule.runOnIdle {
      val view=host!!
      val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
      view.draw(Canvas(bitmap))
      val file=File("build/visual_screenshots/mission01b/hero_alert_portrait.png")
      file.parentFile!!.mkdirs()
      file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
      bitmap.recycle()
    }
    rule.onNodeWithTag("hero_alert_retreat").performClick()
    assertTrue(confirmation)
    assertEquals(MissionStage.IN_PROGRESS, engine.stage)
    rule.onNodeWithTag("hero_alert_regroup").performClick()
    assertEquals(TacticalOrder.REGROUP, engine.activeOrder)
    assertTrue(engine.heroes.single().fearSecondsRemaining < before)
    rule.onNodeWithTag("hero_agency_alert").assertDoesNotExist()
  }
  @Test fun confirmedRetreatWalksToExtractionBeforeSettlement() {
    val base = ApplicationProvider.getApplicationContext<Context>()
    val files = Files.createTempDirectory(base.cacheDir.toPath(), "retreat-ui-").toFile()
    val context = object : ContextWrapper(base) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = files
    }
    val guest = runBlocking { LocalGuestSaveManager(context).startOrResumeGuestSession() }
    val engine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), guest.heroes.filter { it.inActiveSquad }, guest.equipment, enableLostCourier = true)
    engine.stage = MissionStage.IN_PROGRESS
    engine.chooseCourierOrder(CourierOrder.PROTECT)
    repeat(60 * 20) { engine.step(1f / 60f, 0f, 0f) }
    assertEquals(MissionStage.IN_PROGRESS, engine.stage)
    assertTrue(engine.heroes.any { !it.knockedOut && it.x > engine.floorSpec.spawnX + 200f })
    issueRetreatFromConfirmation(engine)
    assertEquals(TacticalOrder.RETREAT, engine.activeOrder)
    assertFalse(engine.retreatCompleted)
    assertEquals(MissionStage.IN_PROGRESS, engine.stage)
    repeat(60 * 90) { engine.step(1f / 60f, 0f, 0f) }
    assertTrue(engine.retreatCompleted)
    assertEquals(MissionStage.DEFEAT, engine.stage)
  }

}
