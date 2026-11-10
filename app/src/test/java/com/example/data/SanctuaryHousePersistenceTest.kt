package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.data.model.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SanctuaryHousePersistenceTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  @Before fun clear() { File(context.filesDir, "eternal_summoner_saves").deleteRecursively() }

  private suspend fun seeded(gold: Long = 500): LocalGuestSaveManager {
    val save = LocalGuestSaveManager(context)
    save.startOrResumeGuestSession("House")
    save.mutateSnapshot { s -> s.copy(profile = s.profile.copy(gold = gold), heroes = listOf(
      HeroRecord(id = "target", userId = s.guestPlayerId, name = "Guard", maxHp = 400, currentHp = 40, morale = 20, personality = "COWARD"),
      HeroRecord(id = "material", userId = s.guestPlayerId, name = "Scout", maxHp = 100, currentHp = 60, attack = 45, defense = 25),
    ), equipment = listOf(EquipmentRecord(id = "gear", userId = s.guestPlayerId, equippedByHeroId = "material"))) }
    return save
  }

  @Test fun staleUiGoldAndHpCannotOverwriteFreshTreatment() = runTest {
    val save = seeded()
    val source = LocalGuestDataSource(save)
    val stale = save.guestSnapshot.value!!
    val hero = stale.heroes.first()
    source.healHeroAtInfirmary(stale.profile, hero)
    source.healHeroAtInfirmary(stale.profile, hero)
    val restored = LocalGuestSaveManager(context).startOrResumeGuestSession()
    assertEquals(340L, restored.profile.gold)
    assertEquals(400L, restored.heroes.first().currentHp)
    assertFalse(restored.heroes.first().hiddenTrait.unlocked)
  }

  @Test fun concurrentTreatmentsCannotOverspendGold() = runTest {
    val save = seeded(80)
    val source = LocalGuestDataSource(save)
    val stale = save.guestSnapshot.value!!
    val results = (1..8).map { async(Dispatchers.Default) {
      runCatching { source.healHeroAtInfirmary(stale.profile, stale.heroes.first()) }
    } }.awaitAll()
    assertEquals(1, results.count { it.isSuccess })
    val restored = LocalGuestSaveManager(context).startOrResumeGuestSession()
    assertEquals(0L, restored.profile.gold)
    assertEquals(240L, restored.heroes.first().currentHp)
  }

  @Test fun settingsSaveCannotOverwriteConcurrentPaidHeroMutation() = runTest {
    val save = seeded()
    val before = save.guestSnapshot.value!!
    listOf(
      async(Dispatchers.Default) { repeat(20) { save.updateSettings(GameSettings(defaultZoom = .8f)) } },
      async(Dispatchers.Default) { LocalGuestDataSource(save).healHeroAtInfirmary(before.profile, before.heroes.first()) },
    ).awaitAll()
    val restored = LocalGuestSaveManager(context).startOrResumeGuestSession()
    assertEquals(420L, restored.profile.gold)
    assertEquals(240L, restored.heroes.first().currentHp)
    assertEquals(.8f, restored.settings.defaultZoom, .001f)
  }

  @Test fun concurrentSynthesisCannotConsumeSameMaterialTwiceAndUnbindsGear() = runTest {
    val save = seeded()
    val source = LocalGuestDataSource(save)
    val results = (1..6).map { async(Dispatchers.Default) {
      runCatching { source.performCruelHeroSynthesis("target", listOf("material")) }
    } }.awaitAll()
    assertEquals(1, results.count { it.isSuccess })
    val restored = LocalGuestSaveManager(context).startOrResumeGuestSession()
    assertEquals(listOf("target"), restored.heroes.map { it.id })
    assertEquals(44L, restored.heroes.single().attack)
    assertEquals(40L, restored.heroes.single().currentHp)
    assertEquals(10L, restored.heroes.single().morale)
    assertEquals("", restored.equipment.single().equippedByHeroId)
  }

  @Test fun socialEventRetryIsIdempotentAcrossConcurrentRequestsAndRestart() = runTest {
    val save = seeded()
    val source = LocalGuestDataSource(save)
    (1..10).map { async(Dispatchers.Default) { source.applySocialDisdainPenalty("material", 2, "00000000-0000-0000-0000-000000000001") } }.awaitAll()
    val reloaded = LocalGuestSaveManager(context)
    reloaded.startOrResumeGuestSession()
    LocalGuestDataSource(reloaded).applySocialDisdainPenalty("material", 2, "00000000-0000-0000-0000-000000000001")
    LocalGuestDataSource(reloaded).applySocialDisdainPenalty("material", 2, "00000000-0000-0000-0000-000000000002")
    val hero = reloaded.guestSnapshot.value!!.heroes.first { it.id == "material" }
    assertEquals(96L, hero.morale)
    assertEquals(listOf("00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000002"), hero.recentDisdainEventIds)
  }

  @Test fun diskFailureDoesNotPublishOrChargePaidAction() = runTest {
    val save = seeded()
    val before = save.guestSnapshot.value!!
    val blocker = File(save.primaryFileForTest().parentFile, "guest_save_v2.tmp")
    assertTrue(blocker.mkdir())
    val result = runCatching { LocalGuestDataSource(save).healHeroAtInfirmary(before.profile, before.heroes.first()) }
    assertTrue(result.isFailure)
    assertEquals(before, save.guestSnapshot.value)
    assertTrue(blocker.delete())
    val restored = LocalGuestSaveManager(context).startOrResumeGuestSession()
    assertEquals(500L, restored.profile.gold)
    assertEquals(40L, restored.heroes.first().currentHp)
  }

  @Test fun actualCombatReturnUnlocksCriticalTraitAndPersistsHpWithoutLevelUpHeal() = runTest {
    val save = seeded()
    save.mutateSnapshot { s -> s.copy(heroes = s.heroes.map {
      if (it.id == "target") it.copy(exp = 99, inActiveSquad = true, currentAssignment = "IN_PARTY") else it
    }) }
    val before = save.guestSnapshot.value!!
    LocalGuestDataSource(save).settleExpedition(before.profile, listOf(before.heroes.first()), emptySet(), 1, true,
      remainingHeroHpRatios = mapOf("target" to .10f))
    val restored = LocalGuestSaveManager(context).startOrResumeGuestSession()
    val hero = restored.heroes.first()
    assertEquals(432L, hero.maxHp)
    assertEquals(40L, hero.currentHp)
    assertEquals(1, hero.survivedExpeditions)
    assertTrue(hero.returnedUnder15PctHp)
    assertTrue(hero.hiddenTrait.unlocked)
    assertEquals(40, HeroData.fromRecord(hero, Long.MAX_VALUE / 1000).currentHP)
  }

  @Test fun threeSurvivalsStillUnlockWithFullStoryMemoryAndNoCriticalHp() = runTest {
    val save = seeded()
    val source = LocalGuestDataSource(save)
    save.mutateSnapshot { s -> s.copy(heroes = s.heroes.map { it.copy(bondSummary = "K".repeat(200)) }) }
    repeat(3) {
      val s = save.guestSnapshot.value!!
      source.settleExpedition(s.profile, listOf(s.heroes.first()), emptySet(), 2, false, retreatUsed = true,
        remainingHeroHpRatios = mapOf("target" to .50f))
    }
    val hero = LocalGuestSaveManager(context).startOrResumeGuestSession().heroes.first()
    assertEquals(3, hero.survivedExpeditions)
    assertEquals("K".repeat(200), hero.bondSummary)
    assertTrue(hero.hiddenTrait.unlocked)
    assertFalse(hero.returnedUnder15PctHp)
  }

  @Test fun criticalReturnRequiresActualReturnAndKnockoutDoesNotCountAsSurvival() {
    val h = HeroRecord(id = "guard", maxHp = 400, currentHp = 10)
    val unknown = ExpeditionHeroHealth.returnFromBattle(h, 432, false, null)
    assertFalse(unknown.returnedUnder15PctHp)
    assertEquals(10L, unknown.currentHp)
    val knockedOut = ExpeditionHeroHealth.returnFromBattle(h, 432, true, 0f)
    assertEquals(0, knockedOut.survivedExpeditions)
    assertEquals(0L, knockedOut.currentHp)
    assertFalse(knockedOut.returnedUnder15PctHp)
  }

  @Test fun legacySurvivalCounterCannotOverflowOrExceedThePersistedSchemaBound() {
    val legacy = HeroRecord(id = "legacy", bondSummary = "[SURVIVED_EXP:2147483647]")
    val returned = ExpeditionHeroHealth.returnFromBattle(legacy, legacy.maxHp, false, .5f)
    assertEquals(1_000_000, returned.survivedExpeditions)
    assertTrue(returned.hiddenTrait.unlocked)
  }
}
