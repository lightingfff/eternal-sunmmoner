package com.example.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.data.model.*
import com.example.ui.screens.SquadPreparationScreen
import com.example.ui.theme.EternalSummonerTheme
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h360dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SquadHouseIntegrationTest {
  @get:Rule val rule = createComposeRule()
  private fun settle() { rule.mainClock.advanceTimeBy(160); rule.waitForIdle() }

  private fun show(list: String, tag: String) {
    for (index in 0..6) {
      rule.onNodeWithTag(list).performScrollToIndex(index)
      settle()
      repeat(20) {
        if (rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() &&
          runCatching { rule.onNodeWithTag(tag).assertIsDisplayed() }.isSuccess) return
        rule.onNodeWithTag(list).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 64f) }
        rule.mainClock.advanceTimeBy(512); rule.waitForIdle()
      }
    }
    throw AssertionError("Missing $tag in $list")
  }

  @Test fun squadPreparationHouseUsesRealPaidCareAndSynthesisThenReturnsToPreparation() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    File(context.filesDir, "eternal_summoner_saves").deleteRecursively()
    val save = LocalGuestSaveManager(context)
    save.startOrResumeGuestSession()
    save.mutateSnapshot { s -> s.copy(profile = s.profile.copy(gold = 500), heroes = listOf(
      HeroRecord(id = "target", userId = s.guestPlayerId, name = "Kaelen", rarity = 5, maxHp = 400, currentHp = 40,
        morale = 20, personality = "COWARD", attack = 50, defense = 30, inActiveSquad = true),
      HeroRecord(id = "material", userId = s.guestPlayerId, name = "Scout", rarity = 1, maxHp = 100, currentHp = 100,
        morale = 70, attack = 30, defense = 20),
    ), equipment = emptyList()) }
    val source = LocalGuestDataSource(save)
    var state by mutableStateOf(SummonerUiState(isBootstrapping = false, isGuestAccount = true,
      profile = save.guestSnapshot.value!!.profile, heroes = save.guestSnapshot.value!!.heroes,
      screenMode = GameScreenMode.SQUAD_PREPARATION))
    fun refresh() {
      val snapshot = save.guestSnapshot.value!!
      state = state.copy(profile = snapshot.profile, heroes = snapshot.heroes)
    }
    rule.mainClock.autoAdvance = false
    rule.setContent { EternalSummonerTheme {
      SquadPreparationScreen(state = state, onToggleHero = {}, onOpenFacility = { state = state.copy(activeFacilityModal = it) },
        onCloseFacility = { state = state.copy(activeFacilityModal = null) }, onSummonHero = {}, onTrainHero = { _, _ -> },
        onEquipItem = { _, _ -> }, onSynthesizeGear = {}, onEnhanceGear = {},
        onRecoverHero = { _, _ -> error("House must not call legacy recovery") }, onHostMeal = {}, onDeploy = {}, onBack = {},
        onHealHeroInfirmary = { h -> runBlocking { source.healHeroAtInfirmary(state.profile!!, h) }; refresh() },
        onServeHeartyMeal = { h -> runBlocking { source.serveHeartyMealToHero(state.profile!!, h) }; refresh() },
        onCruelSynthesis = { target, materials -> runBlocking { source.performCruelHeroSynthesis(target, materials) }; refresh() })
    } }
    settle()
    rule.onNodeWithTag("prep_open_quarters_button").performClick(); settle()
    rule.onNodeWithTag("quarters_tab_infirmary").performClick(); settle()
    show("quarters_infirmary_list", "infirmary_heal_button")
    rule.onNodeWithTag("infirmary_heal_button").performClick(); settle()
    show("quarters_infirmary_list", "canteen_hearty_meal_button")
    rule.onNodeWithTag("canteen_hearty_meal_button").performClick(); settle()
    rule.onNodeWithTag("quarters_tab_synthesis").performClick(); settle()
    show("quarters_synthesis_list", "synthesis_material_material")
    rule.onNodeWithTag("synthesis_material_material").performClick(); settle()
    show("quarters_synthesis_list", "confirm_cruel_synthesis_button")
    rule.onNodeWithTag("confirm_cruel_synthesis_button").performClick(); settle()
    rule.onNodeWithTag("execute_cruel_synthesis_button").performClick(); settle()
    rule.onNodeWithTag("close_facility_modal_button").performClick(); settle()
    assertNull(state.activeFacilityModal)
    rule.onNodeWithTag("prep_open_quarters_button").assertIsDisplayed()
    val restored = LocalGuestSaveManager(context).startOrResumeGuestSession()
    assertEquals(355L, restored.profile.gold)
    assertEquals(listOf("target"), restored.heroes.map { it.id })
    assertEquals(56L, restored.heroes.single().attack)
    assertEquals(420L, restored.heroes.single().maxHp)
    assertEquals(240L, restored.heroes.single().currentHp)
    assertEquals(50L, restored.heroes.single().morale)
  }
}
