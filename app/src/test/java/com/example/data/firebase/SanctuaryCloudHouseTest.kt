package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.LobbyHeroPersistenceFields
import com.example.data.model.*
import com.google.firebase.firestore.FieldValue
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

/** Runs the actual Android repository against local Auth/Firestore, never production. */
class SanctuaryCloudHouseTest : FirestoreEmulatorTestBase() {
  private suspend fun seed(gold: Long = 500): Pair<String, SummonerRepository> {
    val uid = signInTestUser("house_${UUID.randomUUID()}@test.com")
    val repo = SummonerRepository(firestore, auth)
    repo.bootstrapMasterIfNeeded("House test")
    firestore.collection("users").document(uid).update("gold", gold).await()
    createHero(uid, "target", hp = 20, morale = 50, personality = "COWARD")
    createHero(uid, "material", hp = 350, morale = 70)
    return uid to repo
  }

  private suspend fun createHero(uid: String, id: String, hp: Long, morale: Long, personality: String = "LOYAL") {
    val h = HeroRecord(id = id, userId = uid, name = id, title = "Guard", rarity = 1,
      maxHp = if (id == "target") 400 else 350, currentHp = hp, attack = if (id == "target") 40 else 35,
      defense = 20, morale = morale, personality = personality)
    firestore.collection("users").document(uid).collection("heroes").document(id).set(mapOf(
      "userId" to uid, "name" to h.name, "title" to h.title, "heroClass" to h.heroClass, "rarity" to h.rarity,
      "level" to h.level, "exp" to h.exp, "maxHp" to h.maxHp, "attack" to h.attack, "defense" to h.defense,
      "speed" to h.speed, "skillLevel" to h.skillLevel, "personality" to h.personality, "status" to h.status,
      "injuryRecoveryUntilEpochSec" to 0L, "morale" to h.morale, "equippedWeaponId" to "", "equippedArmorId" to "",
      "inActiveSquad" to false, "bondSummary" to "", "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    ) + LobbyHeroPersistenceFields.fields(h)).await()
  }

  private suspend fun profile(uid: String) = firestore.collection("users").document(uid).get().await()
    .toObject(MasterProfile::class.java)!!
  private suspend fun hero(uid: String, id: String) = firestore.collection("users").document(uid)
    .collection("heroes").document(id).get().await().toObject(HeroRecord::class.java)!!

  @Test fun actualCloudTreatmentUsesFreshGoldAndHpUnderConcurrentStaleUiCalls() = runBlocking {
    withTimeout(30_000) {
      val (uid, repo) = seed(80)
      val staleProfile = profile(uid); val staleHero = hero(uid, "target")
      val attempts = coroutineScope { (1..2).map { async {
        runCatching { repo.healHeroAtInfirmary(staleProfile, staleHero) }
      } }.awaitAll() }
      assertEquals(1, attempts.count { it.isSuccess })
      assertEquals(0L, profile(uid).gold)
      assertEquals(220L, hero(uid, "target").currentHp)
    }
  }

  @Test fun actualCloudMealAndSocialRetriesPreserveIndependentFreshMorale() = runBlocking {
    withTimeout(30_000) {
      val (uid, repo) = seed()
      val staleProfile = profile(uid); val staleHero = hero(uid, "target")
      repo.serveHeartyMealToHero(staleProfile, staleHero)
      repo.serveHeartyMealToHero(staleProfile, staleHero)
      assertEquals(100L, hero(uid, "target").morale)
      assertEquals(370L, profile(uid).gold)
      coroutineScope { (1..3).map { async { repo.applySocialDisdainPenalty("target", 2, "00000000-0000-0000-0000-000000000001") } }.awaitAll() }
      repo.applySocialDisdainPenalty("target", 2, "00000000-0000-0000-0000-000000000002")
      assertEquals(96L, hero(uid, "target").morale)
      assertEquals(370L, profile(uid).gold)
    }
  }

  @Test fun actualCloudFusionQueriesOwnedGearAndAtomicallyDeletesOnlyOnce() = runBlocking {
    withTimeout(30_000) {
      val (uid, repo) = seed()
      val user = firestore.collection("users").document(uid)
      user.collection("equipment").document("material_weapon").set(mapOf(
        "userId" to uid, "name" to "Sword", "slot" to "WEAPON", "rarity" to 1L, "level" to 1L,
        "bonusHp" to 0L, "bonusAttack" to 10L, "bonusDefense" to 0L, "bonusSpeed" to 0L,
        "iconType" to "LONGSWORD", "equippedByHeroId" to "material", "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )).await()
      user.collection("heroes").document("material").update("equippedWeaponId", "material_weapon").await()
      val attempts = coroutineScope { (1..2).map { async {
        runCatching { repo.performCruelHeroSynthesis("target", listOf("material")) }
      } }.awaitAll() }
      assertEquals(1, attempts.count { it.isSuccess })
      val target = hero(uid, "target")
      assertEquals(47L, target.attack); assertEquals(24L, target.defense)
      assertEquals(470L, target.maxHp); assertEquals(20L, target.currentHp); assertEquals(40L, target.morale)
      assertEquals("", user.collection("equipment").document("material_weapon").get().await().getString("equippedByHeroId"))
      val owned = user.collection("heroes").whereEqualTo("userId", uid).get().await()
      assertFalse(owned.documents.any { it.id == "material" })
    }
  }
}
