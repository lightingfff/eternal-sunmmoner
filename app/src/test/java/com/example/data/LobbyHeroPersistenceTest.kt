package com.example.data

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
class LobbyHeroPersistenceTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  @Before fun clear() { File(context.filesDir,"eternal_summoner_saves").deleteRecursively() }

  @Test fun assignmentHealthSanityPersonalityAndHiddenTraitSurviveRestart() = runTest {
    val manager = LocalGuestSaveManager(context)
    manager.startOrResumeGuestSession("Lobby")
    manager.mutateSnapshot { s -> s.copy(heroes=(1..5).map { HeroRecord(id="h$it", userId=s.guestPlayerId,
      name="Hero $it", inActiveSquad=it<5, partyJoinedOrder=it.toLong(), level=12, survivedExpeditions=3,
      maxHp=400, currentHp=90, morale=80, sanityState="SELF_AWARE", personality="REBEL") }) }
    val source = LocalGuestDataSource(manager)
    source.assignLobbyHero("h5",HeroAssignment.IN_PARTY)
    val first = LocalGuestSaveManager(context).startOrResumeGuestSession()
    assertEquals(setOf("h2","h3","h4","h5"),first.heroes.filter { it.inActiveSquad }.map { it.id }.toSet())
    val hero = first.heroes.first { it.id=="h5" }
    assertEquals(90L,hero.currentHp); assertEquals("SELF_AWARE",hero.sanityState)
    assertEquals("REBEL",hero.personality); assertTrue(hero.hiddenTrait.unlocked)
    source.assignLobbyHero("h5",HeroAssignment.RESTING)
    val second = LocalGuestSaveManager(context).startOrResumeGuestSession()
    assertEquals("RESTING",second.heroes.first { it.id=="h5" }.status)
    assertFalse(second.heroes.first { it.id=="h5" }.inActiveSquad)
  }

  @Test fun legacySaveWithoutNewFieldsLoadsWithoutResettingProgress() = runTest {
    val manager = LocalGuestSaveManager(context); val original=manager.startOrResumeGuestSession("Legacy")
    val json=JSONObject(manager.primaryFileForTest().readText()); val heroes=json.getJSONArray("heroes")
    for(i in 0 until heroes.length()) for(key in listOf("currentHp","sanityState","currentAssignment","partyJoinedOrder","restStartedAtEpochSec","hiddenTrait")) heroes.getJSONObject(i).remove(key)
    manager.primaryFileForTest().writeText(json.toString())
    val restored=LocalGuestSaveManager(context).startOrResumeGuestSession()
    assertEquals(original.guestPlayerId,restored.guestPlayerId);assertEquals(original.profile.gold,restored.profile.gold)
    assertEquals(original.heroes.size,restored.heroes.size)
    restored.heroes.forEach { assertEquals(it.maxHp.toInt(),HeroData.fromRecord(it).currentHP) }
  }

  @Test fun hiddenTraitUnlockIsPermanentAndCloudImportFieldsKeepLobbyState() = runTest {
    val manager=LocalGuestSaveManager(context);manager.startOrResumeGuestSession()
    manager.mutateSnapshot { s -> s.copy(heroes=s.heroes.map { it.copy(level=10,morale=80,personality="COWARD",sanityState="SELF_AWARE",survivedExpeditions=3) }) }
    manager.mutateSnapshot { s -> s.copy(heroes=s.heroes.map { it.copy(morale=10) }) }
    val hero=LocalGuestSaveManager(context).startOrResumeGuestSession().heroes.first()
    assertTrue(hero.hiddenTrait.unlocked)
    val map=LobbyHeroPersistenceFields.fields(hero)
    assertEquals("COWARD",map["personality"]);assertEquals("SELF_AWARE",map["sanityState"])
    assertEquals(true,(map["hiddenTrait"] as Map<*,*>)["unlocked"])
  }

  @Test fun autoJoinedSummonGetsNewerOrderThanExistingPartyAndTrainingDoesNotLoseHp() = runTest {
    val manager=LocalGuestSaveManager(context);val initial=manager.startOrResumeGuestSession()
    val source=LocalGuestDataSource(manager)
    val created=source.summonNewHero(initial.profile,initial.heroes)
    assertTrue(created.partyJoinedOrder > initial.heroes.maxOf { it.partyJoinedOrder })
    manager.mutateSnapshot { s -> s.copy(heroes=s.heroes.map { if(it.id==created.id) it.copy(currentHp=it.maxHp) else it }) }
    val before=manager.guestSnapshot.value!!;val h=before.heroes.first { it.id==created.id }
    source.trainHero(before.profile,h,false)
    val trained=manager.guestSnapshot.value!!.heroes.first { it.id==created.id }
    assertEquals(trained.maxHp,trained.currentHp)
  }

  @Test fun overlappingDropRequestsNeverPersistAFullPartyPlusOne() = runTest {
    val manager=LocalGuestSaveManager(context);manager.startOrResumeGuestSession()
    manager.mutateSnapshot { s -> s.copy(heroes=(1..8).map { HeroRecord(id="h$it",userId=s.guestPlayerId,inActiveSquad=it<5) }) }
    val source=LocalGuestDataSource(manager)
    (1..30).map { n -> async { source.assignLobbyHero("h${n%8+1}",HeroAssignment.IN_PARTY) } }.awaitAll()
    val restored=LocalGuestSaveManager(context).startOrResumeGuestSession()
    assertEquals(4,restored.heroes.count { it.inActiveSquad })
    assertEquals(8,restored.heroes.map { it.id }.distinct().size)
    assertEquals(4,restored.heroes.count { it.currentAssignment=="IN_PARTY" || it.inActiveSquad })
  }

  @Test fun sanctuaryInfirmaryHealHeartyMealAndCruelSynthesisPersistImmediatelyToLocalJson() = runTest {
    val manager = LocalGuestSaveManager(context)
    manager.startOrResumeGuestSession("SanctuarySave")
    manager.mutateSnapshot { s ->
      s.copy(
        profile = s.profile.copy(gold = 500L),
        heroes = listOf(
          HeroRecord(id = "target", userId = s.guestPlayerId, name = "Han Ysl", rarity = 4, attack = 100, defense = 60, maxHp = 200, currentHp = 80, morale = 20, sanityState = "PANICKED"),
          HeroRecord(id = "mat", userId = s.guestPlayerId, name = "Tân Binh", rarity = 1, attack = 40, defense = 20, maxHp = 100, currentHp = 100, morale = 50),
          HeroRecord(id = "coward", userId = s.guestPlayerId, name = "Kẻ Nhát Gan", rarity = 2, personality = "COWARD", morale = 70, maxHp = 120, currentHp = 120),
        ),
      )
    }
    val source = LocalGuestDataSource(manager)
    val snap1 = manager.guestSnapshot.value!!
    source.healHeroAtInfirmary(snap1.profile, snap1.heroes.first { it.id == "target" })
    val snap2 = manager.guestSnapshot.value!!
    source.serveHeartyMealToHero(snap2.profile, snap2.heroes.first { it.id == "target" })
    source.performCruelHeroSynthesis("target", listOf("mat"))

    val reloaded = LocalGuestSaveManager(context).startOrResumeGuestSession()
    assertEquals(355L, reloaded.profile.gold) // 500 - 80 - 65
    assertEquals(2, reloaded.heroes.size)
    assertTrue(reloaded.heroes.none { it.id == "mat" })
    val targetReloaded = reloaded.heroes.first { it.id == "target" }
    assertEquals(108L, targetReloaded.attack)
    assertEquals(64L, targetReloaded.defense)
    assertEquals(220L, targetReloaded.maxHp)
    assertEquals(60L, targetReloaded.morale)
    assertEquals("SELF_AWARE", HeroData.fromRecord(targetReloaded).sanityState.name)
    val cowardReloaded = reloaded.heroes.first { it.id == "coward" }
    assertEquals(60L, cowardReloaded.morale)
  }
}

