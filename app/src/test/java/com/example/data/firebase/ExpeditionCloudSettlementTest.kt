package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.ExpeditionSettlementCodec
import com.example.data.model.CourierOutcome
import com.example.data.model.HeroRecord
import com.example.data.model.MasterProfile
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

/** Uses actual Auth/Firestore emulators, never a production database. */
class ExpeditionCloudSettlementTest : FirestoreEmulatorTestBase() {
  @Test fun concurrentSameAttemptAndRepositoryRestartCreditExactlyOnce() = runBlocking {
    withTimeout(45_000) {
      val uid = signInTestUser("receipt_${UUID.randomUUID()}@test.com")
      val repository = SummonerRepository(firestore, auth)
      repository.bootstrapMasterIfNeeded("Receipt Master")
      val user = firestore.collection("users").document(uid)
      val initial = user.get().await().toObject(MasterProfile::class.java)!!
      val heroes = user.collection("heroes").whereEqualTo("userId", uid).get().await().documents.map { it.toObject(HeroRecord::class.java)!! }
      assertEquals(3, heroes.size)
      assertTrue(heroes.all { it.userId == uid })
      val id = UUID.randomUUID().toString()
      val outcomes = coroutineScope {
        List(2) { async {
          repository.settleExpedition(initial, heroes, emptySet(), 1, true,
            courierOutcome = CourierOutcome.SUPPLIES_RECOVERED,
            remainingHeroHpRatios = heroes.associate { it.id to .25f }, attemptId = id)
        } }.awaitAll()
      }
      assertEquals(outcomes[0], outcomes[1])
      val profileAfter = user.get().await().data
      val heroAfter = user.collection("heroes").whereEqualTo("userId", uid).get().await().documents.associate { it.id to it.data }
      val equipmentAfter = user.collection("equipment").whereEqualTo("userId", uid).get().await().documents.associate { it.id to it.data }
      val replay = SummonerRepository(firestore, auth).settleExpedition(initial, emptyList(), emptySet(), 1, false, attemptId = id)
      assertEquals(outcomes.first(), replay)
      assertEquals(profileAfter, user.get().await().data)
      assertEquals(heroAfter, user.collection("heroes").whereEqualTo("userId", uid).get().await().documents.associate { it.id to it.data })
      assertEquals(equipmentAfter, user.collection("equipment").whereEqualTo("userId", uid).get().await().documents.associate { it.id to it.data })
      assertEquals(initial.totalExpeditions + 1L, user.get().await().getLong("totalExpeditions"))
      val receipt = user.collection("expeditionSettlements").document(id).get().await()
      assertEquals(uid, receipt.getString("userId"))
      assertEquals(id, receipt.getString("attemptId"))
      assertEquals(replay, ExpeditionSettlementCodec.decode(receipt.getString("resultJSON")!!))
      assertNotNull(receipt.getTimestamp("createdAt"))
      repository.settleExpedition(initial, heroes, emptySet(), 1, true, attemptId = UUID.randomUUID().toString())
      assertEquals(initial.totalExpeditions + 2L, user.get().await().getLong("totalExpeditions"))
    }
  }
}
