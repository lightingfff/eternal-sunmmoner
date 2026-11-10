package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import com.example.data.model.FacilityType
import com.example.data.model.HeroAssignment
import com.example.data.model.HeroHiddenTrait
import com.example.data.model.HeroRecord
import com.example.data.model.HeroSanityState
import com.example.data.model.MasterProfile
import com.example.ui.screens.SanctuaryFacilityModalOverlay
import com.example.ui.theme.EternalSummonerTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Native Compose interaction evidence, not a replacement for physical Android touch/installation QA. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h360dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SanctuaryHouseUiTest {
  @get:Rule val rule = createComposeRule()

  private lateinit var uiState: MutableState<SummonerUiState>
  private val open = mutableStateOf(true)
  private var hostView: View? = null
  private var closeCount = 0
  private val healed = mutableListOf<String>()
  private val fed = mutableListOf<String>()
  private val synthesized = mutableListOf<Pair<String, List<String>>>()

  private fun roster(): List<HeroRecord> = listOf(
    HeroRecord(
      id = "target", name = "Kaelen", rarity = 5, level = 3, currentHp = 200, maxHp = 200,
      attack = 60, defense = 40, morale = 100, personality = "LOYAL",
      hiddenTrait = HeroHiddenTrait(id = "indomitable_will", name = "Bất Khuất - Miễn nhiễm Hoảng loạn", unlocked = true),
    ),
    HeroRecord(
      id = "coward", name = "Mira", rarity = 1, level = 1, currentHp = 20, maxHp = 200,
      attack = 7, defense = 9, morale = 20, personality = "COWARD", sanityState = HeroSanityState.PANICKED.name,
      hiddenTrait = HeroHiddenTrait(id = "assassin_instinct", name = "Bản Năng Sát Thủ - Tăng 30% Crit"),
    ),
    HeroRecord(id = "arrogant", name = "Dorian", rarity = 2, level = 2, currentHp = 120, maxHp = 200, attack = 8, defense = 6, morale = 35, personality = "ARROGANT"),
    HeroRecord(id = "loner", name = "Vesper", rarity = 2, currentHp = 90, maxHp = 205, attack = 9, defense = 4, morale = 80, personality = "LONER"),
    HeroRecord(id = "rebel", name = "Jenna", rarity = 1, currentHp = 100, maxHp = 200, attack = 11, defense = 5, morale = 75, personality = "REBEL"),
  )

  private fun start(
    heroes: List<HeroRecord> = roster(),
    gold: Long = 1000,
    selectedHeroId: String? = null,
  ) {
    rule.mainClock.autoAdvance = false
    uiState = mutableStateOf(SummonerUiState(
      isBootstrapping = false, isGuestAccount = true, heroes = heroes,
      profile = MasterProfile(id = "house_ui", gold = gold),
    ))
    rule.setContent {
      hostView = LocalView.current
      EternalSummonerTheme {
        Box(Modifier.fillMaxSize()) {
          Text("Lobby", modifier = Modifier.testTag("returned_lobby"))
          if (open.value) {
            SanctuaryFacilityModalOverlay(
              facility = FacilityType.HERO_QUARTERS,
              state = uiState.value,
              onClose = { closeCount++; open.value = false },
              onSummonHero = {}, onTrainHero = { _, _ -> }, onEquipItem = { _, _ -> },
              onSynthesizeGear = {}, onEnhanceGear = {}, onRecoverHero = { _, _ -> },
              onHostMeal = {}, onToggleSquad = {},
              initialSelectedHeroId = selectedHeroId,
              onHealHeroInfirmary = { healed += it.id },
              onServeHeartyMeal = { fed += it.id },
              onCruelSynthesis = { target, materials -> synthesized += target to materials },
            )
          }
        }
      }
    }
    settle()
  }

  private fun settle(durationMillis: Long = 512) {
    rule.waitForIdle()
    rule.mainClock.advanceTimeBy(durationMillis)
    rule.waitForIdle()
    rule.mainClock.advanceTimeByFrame()
    rule.waitForIdle()
  }

  private fun tab(tag: String) {
    rule.onNodeWithTag(tag).performClick()
    settle()
  }

  private fun show(listTag: String, tag: String) {
    // Compose's search-and-scroll loop cannot advance our deliberately frozen animation clock.
    // Scroll one list item, advance a frame, and fail after the finite fixture's last item instead.
    val lastPossibleIndex = uiState.value.heroes.count { it.currentAssignment != HeroAssignment.SACRIFICED.name } + 3
    for (index in 0..lastPossibleIndex) {
      rule.onNodeWithTag(listTag).performScrollToIndex(index)
      settle(512)
      for (step in 0 until 20) {
        if (rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isEmpty()) break
        if (runCatching { rule.onNodeWithTag(tag).assertIsDisplayed() }.isSuccess) return
        rule.onNodeWithTag(listTag).performSemanticsAction(SemanticsActions.ScrollBy) { scroll -> scroll(0f, 64f) }
        settle(512)
      }
    }
    throw AssertionError("$tag was not found in $listTag after checking $lastPossibleIndex list items")
  }

  private fun material(id: String) {
    show("quarters_synthesis_list", "synthesis_material_$id")
    rule.onNodeWithTag("synthesis_material_$id").performClick()
    settle()
  }

  private fun requestSynthesis() {
    show("quarters_synthesis_list", "confirm_cruel_synthesis_button")
    rule.onNodeWithTag("confirm_cruel_synthesis_button").performClick()
    settle()
  }

  private fun capture(name: String) {
    settle()
    rule.runOnIdle {
      val view = requireNotNull(hostView)
      val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
      view.draw(Canvas(bitmap))
      val file = File("build/visual_screenshots/sanctuary_house/$name.png")
      file.parentFile!!.mkdirs()
      file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
      assertTrue("Native Compose capture must contain rendered content: $name", file.length() > 10_000)
      bitmap.recycle()
    }
  }

  private fun semanticsDump(node: SemanticsNode): String = buildString {
    appendLine(node.config)
    node.children.forEach { append(semanticsDump(it)) }
  }

  @Test fun dormitoryUsesOwnedHpMoraleAndActualPersonalityWithoutInventingAnAwakening() {
    val sacrificed = HeroRecord(id = "dead", name = "Removed", currentAssignment = HeroAssignment.SACRIFICED.name)
    start(heroes = roster() + sacrificed)
    rule.onNodeWithTag("quarters_tab_dormitory").assertIsSelected()
    show("quarters_dormitory_list", "quarters_hero_coward")
    rule.onNodeWithTag("quarters_stats_coward", useUnmergedTree = true).assertTextContains("HP: 20/200", substring = true)
    rule.onNodeWithTag("quarters_stats_coward", useUnmergedTree = true).assertTextContains("Morale: 20/100", substring = true)
    rule.onNodeWithTag("quarters_trait_coward", useUnmergedTree = true).assertTextContains("??? (Chưa thức tỉnh)", substring = true)
    rule.onNodeWithTag("quarters_hp_coward", useUnmergedTree = true).assert(
      SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(.1f, 0f..1f)),
    )
    capture("landscape_dormitory_injured")
    for (id in listOf("target", "arrogant", "loner", "rebel")) {
      show("quarters_dormitory_list", "quarters_hero_$id")
      rule.onNodeWithTag("quarters_hero_$id").assertExists()
    }
    rule.onNodeWithTag("quarters_hero_dead").assertDoesNotExist()
  }

  @Test fun treatmentTargetsChosenHeroAndKeepsSelectionWhenPersistedStatsArrive() {
    start()
    tab("quarters_tab_infirmary")
    show("quarters_infirmary_list", "infirmary_select_hero_arrogant")
    rule.onNodeWithTag("infirmary_select_hero_arrogant").performClick()
    show("quarters_infirmary_list", "infirmary_heal_button")
    rule.onNodeWithTag("infirmary_heal_button").assertIsEnabled().performClick()
    assertEquals(listOf("arrogant"), healed)
    rule.runOnIdle {
      uiState.value = uiState.value.copy(
        profile = uiState.value.profile!!.copy(gold = 920),
        heroes = uiState.value.heroes.map { if (it.id == "arrogant") it.copy(currentHp = 200) else it },
      )
    }
    settle()
    show("quarters_infirmary_list", "infirmary_selected_hero")
    rule.onNodeWithTag("infirmary_selected_hero").assertTextContains("Dorian (Ngạo mạn)", substring = true)
    show("quarters_infirmary_list", "infirmary_heal_button")
    rule.onNodeWithTag("infirmary_heal_button").assertIsNotEnabled()
    show("quarters_infirmary_list", "canteen_hearty_meal_button")
    rule.onNodeWithTag("canteen_hearty_meal_button").performClick()
    assertEquals(listOf("arrogant"), fed)
    rule.runOnIdle {
      uiState.value = uiState.value.copy(
        profile = uiState.value.profile!!.copy(gold = 855),
        heroes = uiState.value.heroes.map { if (it.id == "arrogant") it.copy(morale = 100, sanityState = HeroSanityState.NORMAL.name) else it },
      )
    }
    settle()
    rule.onNodeWithTag("canteen_hearty_meal_button").assertIsNotEnabled()
    capture("landscape_infirmary_selected")
  }

  @Test fun insufficientGoldAndBusyStateBlockCareAndHeroChanges() {
    start(gold = 64, selectedHeroId = "coward")
    show("quarters_infirmary_list", "infirmary_heal_button")
    rule.onNodeWithTag("infirmary_heal_button").assertIsNotEnabled()
    show("quarters_infirmary_list", "canteen_hearty_meal_button")
    rule.onNodeWithTag("canteen_hearty_meal_button").assertIsNotEnabled()
    rule.runOnIdle { uiState.value = uiState.value.copy(profile = uiState.value.profile!!.copy(gold = 80), isBusyAction = true) }
    settle()
    rule.onNodeWithTag("canteen_hearty_meal_button").assertIsNotEnabled()
    show("quarters_infirmary_list", "infirmary_select_hero_arrogant")
    rule.onNodeWithTag("infirmary_select_hero_arrogant").assertIsNotEnabled()
    rule.runOnIdle { uiState.value = uiState.value.copy(profile = uiState.value.profile!!.copy(gold = 65), isBusyAction = false) }
    settle()
    show("quarters_infirmary_list", "infirmary_heal_button")
    rule.onNodeWithTag("infirmary_heal_button").assertIsNotEnabled()
    show("quarters_infirmary_list", "canteen_hearty_meal_button")
    rule.onNodeWithTag("canteen_hearty_meal_button").assertIsEnabled()
    assertTrue(healed.isEmpty() && fed.isEmpty())
  }

  @Test fun synthesisShowsImmediatePleaAndExactPreviewThenRequiresSeparateConfirmation() {
    start()
    tab("quarters_tab_synthesis")
    show("quarters_synthesis_list", "confirm_cruel_synthesis_button")
    rule.onNodeWithTag("confirm_cruel_synthesis_button").assertIsNotEnabled()
    material("coward")
    rule.onNodeWithTag("synthesis_plea_coward", useUnmergedTree = true)
      .assertTextContains("Xin ngài Master! Tôi sẽ làm bất cứ điều gì", substring = true)
    material("arrogant")
    rule.onNodeWithTag("synthesis_plea_arrogant", useUnmergedTree = true)
      .assertTextContains("Ngươi dám hiến tế một chiến binh cao quý", substring = true)
    show("quarters_synthesis_list", "synthesis_preview_stats")
    rule.onNodeWithTag("synthesis_preview_stats").assertTextContains("+3 ATK • +3 DEF • +80 MaxHP • +300 EXP", substring = true)
    capture("landscape_synthesis_preview")
    requestSynthesis()
    rule.onNodeWithTag("cruel_synthesis_confirmation").assertExists()
    assertTrue("Opening the confirmation must not consume any Hero", synthesized.isEmpty())
    rule.onNodeWithTag("cancel_cruel_synthesis_button").performClick()
    settle()
    rule.onNodeWithTag("cruel_synthesis_confirmation").assertDoesNotExist()
    requestSynthesis()
    rule.onNodeWithTag("execute_cruel_synthesis_button").performClick()
    settle()
    assertEquals(listOf("target" to listOf("coward", "arrogant")), synthesized)
    rule.onNodeWithTag("cruel_synthesis_confirmation").assertDoesNotExist()
    show("quarters_synthesis_list", "confirm_cruel_synthesis_button")
    rule.onNodeWithTag("confirm_cruel_synthesis_button").assertIsNotEnabled()
  }

  @Test fun synthesisCapPreviewMatchesTheDestructiveConfirmation() {
    start(heroes = roster().map { if (it.id == "target") it.copy(
      attack = 49_999, defense = 49_998, maxHp = 99_999, level = 100, exp = 9_999_999,
    ) else it })
    tab("quarters_tab_synthesis")
    material("coward"); material("arrogant")
    show("quarters_synthesis_list", "synthesis_preview_stats")
    rule.onNodeWithTag("synthesis_preview_stats").assertTextContains("+1 ATK • +2 DEF • +1 MaxHP • +1 EXP", substring = true)
    requestSynthesis()
    rule.onNodeWithTag("cruel_synthesis_confirmation").assertExists()
    rule.onNodeWithTag("cancel_cruel_synthesis_button").performClick()
    settle()
    assertTrue(synthesized.isEmpty())
  }

  @Test fun synthesisCapsAtThreeAndSelectedMaterialCannotAlsoBeTheTarget() {
    start()
    tab("quarters_tab_synthesis")
    material("coward")
    material("arrogant")
    material("loner")
    show("quarters_synthesis_list", "synthesis_material_rebel")
    rule.onNodeWithTag("synthesis_material_rebel").assertIsOff().assertIsNotEnabled()
    show("quarters_synthesis_list", "synthesis_target_coward")
    rule.onNodeWithTag("synthesis_target_coward").performClick()
    settle()
    rule.onNodeWithTag("synthesis_target_coward").assertIsSelected()
    rule.onNodeWithTag("synthesis_material_coward").assertDoesNotExist()
    show("quarters_synthesis_list", "synthesis_material_rebel")
    rule.onNodeWithTag("synthesis_material_rebel").assertIsEnabled()
    show("quarters_synthesis_list", "synthesis_material_arrogant")
    rule.onNodeWithTag("synthesis_material_arrogant").assertIsOn()
    requestSynthesis()
    rule.onNodeWithTag("execute_cruel_synthesis_button").performClick()
    settle()
    assertEquals(listOf("coward" to listOf("arrogant", "loner")), synthesized)
  }

  @Test fun deletedHeroesDoNotSurviveSelectionOrPendingConfirmation() {
    start()
    tab("quarters_tab_synthesis")
    material("coward")
    requestSynthesis()
    rule.onNodeWithTag("cruel_synthesis_confirmation").assertExists()
    rule.runOnIdle { uiState.value = uiState.value.copy(heroes = uiState.value.heroes.filterNot { it.id == "coward" }) }
    settle()
    rule.onNodeWithTag("cruel_synthesis_confirmation").assertDoesNotExist()
    rule.onNodeWithTag("synthesis_material_coward").assertDoesNotExist()
    show("quarters_synthesis_list", "confirm_cruel_synthesis_button")
    rule.onNodeWithTag("confirm_cruel_synthesis_button").assertIsNotEnabled()
    assertTrue(synthesized.isEmpty())
    tab("quarters_tab_infirmary")
    show("quarters_infirmary_list", "infirmary_selected_hero")
    rule.onNodeWithTag("infirmary_selected_hero").assertTextContains("Dorian", substring = true)
  }

  @Test fun busyStateBlocksSynthesisAndStateUpdatesInvalidateThePreviewedConfirmation() {
    start()
    tab("quarters_tab_synthesis")
    material("coward")
    requestSynthesis()
    rule.runOnIdle { uiState.value = uiState.value.copy(isBusyAction = true) }
    settle()
    rule.onNodeWithTag("execute_cruel_synthesis_button").assertIsNotEnabled()
    rule.runOnIdle {
      uiState.value = uiState.value.copy(isBusyAction = false,
        heroes = uiState.value.heroes.map { if (it.id == "coward") it.copy(attack = 70) else it })
    }
    assertEquals(70L, uiState.value.heroes.single { it.id == "coward" }.attack)
    settle()
    // Dialog disposal may complete on a separate window frame from the host composition.
    rule.mainClock.advanceTimeBy(512)
    rule.waitForIdle()
    val remainingDialogs = rule.onAllNodes(hasTestTag("cruel_synthesis_confirmation"), useUnmergedTree = true).fetchSemanticsNodes()
    if (remainingDialogs.isNotEmpty()) {
      println("Confirmation after roster changed to attack=70: " + remainingDialogs.joinToString { semanticsDump(it) })
      capture("diagnostic_stale_confirmation_host")
    }
    rule.onNodeWithTag("cruel_synthesis_confirmation").assertDoesNotExist()
    show("quarters_synthesis_list", "synthesis_material_coward")
    rule.onNodeWithTag("synthesis_material_coward").assertIsOn()
    assertTrue(synthesized.isEmpty())
  }

  @Test
  @Config(qualifiers = "w360dp-h800dp-port-xhdpi")
  fun portraitTabsTreatmentAndCloseRemainAccessibleAndReturnToLobby() {
    start()
    rule.onNodeWithTag("quarters_tab_dormitory").assertIsDisplayed()
    show("quarters_dormitory_list", "quarters_hero_coward")
    capture("portrait_dormitory")
    tab("quarters_tab_infirmary")
    show("quarters_infirmary_list", "canteen_hearty_meal_button")
    rule.onNodeWithTag("canteen_hearty_meal_button").assertIsDisplayed().assertIsEnabled()
    capture("portrait_infirmary")
    tab("quarters_tab_synthesis")
    material("coward")
    rule.onNodeWithTag("synthesis_plea_coward", useUnmergedTree = true).assertExists()
    capture("portrait_synthesis_plea")
    rule.onNodeWithTag("close_facility_modal_button").assertIsDisplayed().performClick()
    settle()
    assertEquals(1, closeCount)
    rule.onNodeWithTag("returned_lobby").assertIsDisplayed()
    rule.onNodeWithTag("quarters_tab_synthesis").assertDoesNotExist()
  }
}
