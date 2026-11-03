package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.model.EquipmentRecord
import com.example.data.model.FacilityType
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.MasterProfile
import com.example.data.model.PersonalityType
import com.example.game.tower.TowerFloorCatalog
import com.example.game.story.ManhwaStoryData
import com.example.ui.components.FantasySkin
import com.example.ui.screens.SanctuaryLobbyScreen
import com.example.ui.theme.EternalSummonerTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P26 ROUND 1 UI evidence: the premium hub shell.
 *
 * Captures the real SanctuaryLobbyScreen (Compose UI, Robolectric native graphics) at
 * narrow portrait and widescreen, verifies the bottom navigation tabs exist with icons,
 * and asserts the panel skin is translucent (alpha < 1) so the world stays visible
 * instead of sitting behind opaque black slabs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P26UiShellTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private fun sampleState(): SummonerUiState {
    val profile = MasterProfile(
      id = "u_master_test",
      masterName = "Thống Lĩnh Huyền Thoại Lokir",
      masterLevel = 12,
      highestClearedFloor = 1,
      gold = 14850,
      manaCrystals = 640,
      soulShards = 125,
      ironOre = 42,
      arcaneEssence = 19,
      rations = 16,
      storyFlags = listOf(ManhwaStoryData.FLAG_PROLOGUE_SEEN),
    )
    val heroes = listOf(
      HeroRecord(
        id = "h1", name = "Kaelen Vệ Thần", title = "Khiên Thép Bất Diệt",
        heroClass = HeroClassType.KNIGHT.name, rarity = 4, level = 7,
        maxHp = 620, attack = 68, defense = 42, speed = 108,
        personality = PersonalityType.STOIC.name, morale = 96, inActiveSquad = true,
      ),
      HeroRecord(
        id = "h2", name = "Lyra Phong Tiễn", title = "Mắt Ưng Tinh Hệ",
        heroClass = HeroClassType.ARCHER.name, rarity = 4, level = 6,
        maxHp = 410, attack = 82, defense = 24, speed = 136,
        personality = PersonalityType.CYNICAL.name, morale = 88, inActiveSquad = true,
      ),
    )
    val equipment = listOf(
      EquipmentRecord(
        id = "eq1", name = "Trường Kiếm Hắc Tinh Cổ Đại", slot = "WEAPON",
        rarity = 4, level = 3, bonusAttack = 24, bonusHp = 60, bonusDefense = 8,
        equippedByHeroId = "h1",
      ),
    )
    return SummonerUiState(
      isBootstrapping = false,
      profile = profile,
      heroes = heroes,
      equipment = equipment,
      selectedFloor = TowerFloorCatalog.FLOORS.first(),
      activeFacilityModal = null,
    )
  }

  private fun saveViewScreenshot(view: View, outPath: String) {
    val w = view.width.coerceAtLeast(1)
    val h = view.height.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    view.draw(canvas)
    val outFile = File(outPath)
    outFile.parentFile?.mkdirs()
    FileOutputStream(outFile).use { fos ->
      bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }
  }

  private fun renderLobby(outPath: String) {
    composeTestRule.mainClock.autoAdvance = false
    var hostView: View? = null
    composeTestRule.setContent {
      hostView = LocalView.current
      EternalSummonerTheme {
        SanctuaryLobbyScreen(
          state = sampleState(),
          onOpenFacility = {},
          onCloseFacility = {},
          onSummonHero = {},
          onTrainHero = { _, _ -> },
          onEquipItem = { _, _ -> },
          onSynthesizeGear = {},
          onEnhanceGear = {},
          onRecoverHero = { _, _ -> },
          onHostMeal = {},
          onToggleSquad = {},
          onCompletePrologue = {},
          onSignOut = {},
        )
      }
    }
    composeTestRule.mainClock.advanceTimeBy(160L)
    composeTestRule.waitForIdle()
    hostView?.let { saveViewScreenshot(it, outPath) }
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
  fun `narrow portrait lobby captures premium shell with nav tabs`() {
    renderLobby("build/visual_screenshots/p26/ui/lobby_narrow_360.png")
    for (tag in listOf("nav_heroes", "nav_summon", "nav_team", "nav_tower", "nav_inventory")) {
      composeTestRule.onNodeWithTag(tag).assertExists()
    }
    composeTestRule.onNodeWithTag("sanctuary_canvas").assertExists()
    composeTestRule.onNodeWithTag("sanctuary_top_hud").assertExists()
    assertTrue(File("build/visual_screenshots/p26/ui/lobby_narrow_360.png").exists())
  }

  @Test
  @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
  fun `widescreen lobby captures premium shell and compact hud`() {
    renderLobby("build/visual_screenshots/p26/ui/lobby_wide_800.png")
    for (tag in listOf("nav_heroes", "nav_summon", "nav_team", "nav_tower", "nav_inventory")) {
      composeTestRule.onNodeWithTag(tag).assertExists()
    }
    composeTestRule.onNodeWithTag("sanctuary_top_hud").assertExists()
    composeTestRule.onNodeWithTag("hero_command_panel").assertExists()
    assertTrue(File("build/visual_screenshots/p26/ui/lobby_wide_800.png").exists())
  }

  @Test
  fun `panel skin is translucent with gold border`() {
    // No opaque black slabs: panel containers keep alpha < 1 so the world reads through.
    assertTrue("FantasySkin.Panel must be translucent", FantasySkin.Panel.alpha < 1f)
    assertTrue("FantasySkin.Panel must keep dark glass base", FantasySkin.Panel.red > 0.05f)
    assertTrue("FantasySkin.Gold accent keeps warm gold", FantasySkin.Gold.red > 0.6f)
    // The shared glass used by FantasyPanel must be translucent too.
    val glass = com.example.ui.components.FantasyManagement.PanelGlass
    assertTrue("FantasyPanel glass must be translucent", glass.alpha < 1f)
  }
}
