package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.LostCourierProgression
import com.example.data.model.CourierOutcome
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SummonerRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun courierRescue_staleProfileReplay_grantsOneMemoryAndPermanentService() = runBlocking {
    val uid = signInTestUser("courier_${UUID.randomUUID().toString().take(8)}@test.com")
    val repo = SummonerRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) { repo.bootstrapMasterIfNeeded("Courier Master") }
    val profile = checkNotNull(withTimeout(FLOW_TIMEOUT_MS) { repo.observeMasterProfile(uid).first { it != null } })
    val heroes = withTimeout(FLOW_TIMEOUT_MS) { repo.observeHeroes(uid).first { it.size == 3 } }
    val first = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.settleExpedition(profile, heroes, emptySet(), 1, true,
        courierOutcome = CourierOutcome.RESCUED, courierMemoryHeroId = heroes.first().id)
    }
    assertTrue(first.courierForgeUnlocked)
    val replay = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.settleExpedition(profile, heroes, emptySet(), 1, true,
        courierOutcome = CourierOutcome.RESCUED, courierMemoryHeroId = heroes.last().id)
    }
    assertEquals(false, replay.courierForgeUnlocked)
    val saved = checkNotNull(withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMasterProfile(uid).first { it != null && LostCourierProgression.FLAG_RESCUED in it.storyFlags }
    })
    assertEquals(90L, LostCourierProgression.synthesisGoldCost(saved.storyFlags))
    val savedHeroes = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeHeroes(uid).first { roster -> roster.any { LostCourierProgression.hasRescueMemory(it.bondSummary) } }
    }
    assertEquals(1, savedHeroes.count { LostCourierProgression.hasRescueMemory(it.bondSummary) })
  }

  @Test
  fun bootstrapAndObserve_authenticatedOwner_createsStarterSanctuaryAndHeroes() = runBlocking {
    val uniqueEmail = "alice_${UUID.randomUUID().toString().take(8)}@test.com"
    val uid = signInTestUser(uniqueEmail)
    val repo = SummonerRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.bootstrapMasterIfNeeded("Master Loki")
    }

    val profile = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMasterProfile(uid).first { it != null }
    }
    assertNotNull(profile)
    assertEquals("Master Loki", profile?.masterName)

    val heroes = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeHeroes(uid).first { it.size >= 3 }
    }
    assertEquals(3, heroes.size)
  }

  @Test
  fun trainSummonAndSettleExpedition_authenticatedOwner_succeedsWithinRules() = runBlocking {
    val uniqueEmail = "summoner_${UUID.randomUUID().toString().take(8)}@test.com"
    val uid = signInTestUser(uniqueEmail)
    val repo = SummonerRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.bootstrapMasterIfNeeded("Master Han")
    }

    val profile = checkNotNull(
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeMasterProfile(uid).first { it != null } }
    )
    val heroes = withTimeout(FLOW_TIMEOUT_MS) { repo.observeHeroes(uid).first { it.size == 3 } }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.trainHero(profile, heroes.first(), upgradeSkill = false)
    }

    val updatedProfile = checkNotNull(
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeMasterProfile(uid).first { it != null } }
    )
    val summoned = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.summonNewHero(updatedProfile, heroes)
    }
    assertTrue(summoned.name.isNotBlank())

    val latestProfile = checkNotNull(
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeMasterProfile(uid).first { it != null } }
    )
    val settlement = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.settleExpedition(
        profile = latestProfile,
        squadHeroes = heroes,
        fallenHeroIds = emptySet(),
        floorNumber = 1,
        victory = true,
      )
    }
    assertTrue(settlement.victory)
    assertEquals(1, settlement.floorNumber)
  }

  @Test
  fun getHeroById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(8)}@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val aliceRepo = SummonerRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.bootstrapMasterIfNeeded("Alice") }

    val bobEmail = "bob_${UUID.randomUUID().toString().take(8)}@test.com"
    signInTestUser(bobEmail)
    val bobRepo = SummonerRepository(firestore, auth)

    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        bobRepo.getHeroById(aliceUid, "hero_starter_kaelen")
      }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  @Test
  fun observeHeroes_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = SummonerRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeHeroes("unauth_target_uid").first()
      }
      fail("Expected exception wrapping PERMISSION_DENIED")
    } catch (e: Exception) {
      val fsEx = checkNotNull(e.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
    }
  }

  @Test
  fun linkGuestSaveToCloudAccount_andFeastMeal_succeedWithinFirestoreRules() = runBlocking {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    java.io.File(context.filesDir, "eternal_summoner_saves").deleteRecursively()
    val guestManager = com.example.data.local.LocalGuestSaveManager(context)
    val guestSnapshot = guestManager.startOrResumeGuestSession("Master Guest Kael")
    val guestDataSource = com.example.data.local.LocalGuestDataSource(guestManager)

    // Advance Guest progress (clear Floor 1) before linking to Cloud
    guestDataSource.settleExpedition(
      profile = guestSnapshot.profile,
      squadHeroes = guestSnapshot.heroes.filter { it.inActiveSquad },
      fallenHeroIds = emptySet(),
      floorNumber = 1,
      victory = true,
    )
    val updatedSnapshot = checkNotNull(guestManager.guestSnapshot.value)

    val cloudEmail = "linked_${UUID.randomUUID().toString().take(8)}@test.com"
    val cloudUid = signInTestUser(cloudEmail)
    val repo = SummonerRepository(firestore, auth)

    val statusMessage = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.linkGuestSaveToCloudAccount(updatedSnapshot)
    }
    assertTrue(statusMessage.isNotBlank())

    val migratedProfile = checkNotNull(
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeMasterProfile(cloudUid).first { it != null } }
    )
    assertEquals(1, migratedProfile.highestClearedFloor)
    assertTrue(migratedProfile.gold > guestSnapshot.profile.gold)

    val migratedHeroes = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeHeroes(cloudUid).first { it.size >= 3 }
    }
    assertEquals(3, migratedHeroes.size)

    // Verify hosting a Restaurant Feast stores FEAST_UNTIL_<epoch> in storyFlags cleanly within firestore.rules
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.hostSanctuaryMeal(migratedProfile, migratedHeroes)
    }
    val postFeastProfile = checkNotNull(
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeMasterProfile(cloudUid).first { p ->
          p != null && com.example.data.FeastBuffHelper.isFeastBuffActive(p.storyFlags)
        }
      }
    )
    assertTrue(com.example.data.FeastBuffHelper.isFeastBuffActive(postFeastProfile.storyFlags))
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3500L
  }
}
