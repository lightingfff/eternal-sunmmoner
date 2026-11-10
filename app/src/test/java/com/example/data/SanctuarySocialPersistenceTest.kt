package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.ui.SummonerViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SanctuarySocialPersistenceTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  @Before fun clear() { File(context.filesDir, "eternal_summoner_saves").deleteRecursively() }

  @Test fun transientFailureRetriesTheSameEventAndCommitsOnlyOnePenalty() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
      val save = LocalGuestSaveManager(context)
      val initial = save.startOrResumeGuestSession()
      save.mutateSnapshot { s -> s.copy(heroes = s.heroes.map { it.copy(rarity = 1L) }) }
      val local = LocalGuestDataSource(save)
      val ids = mutableListOf<String>()
      val unreliable = object : SummonerDataSource by local {
        override suspend fun applySocialDisdainPenalty(lowStarHeroId: String, penalty: Int, eventId: String) {
          ids += eventId
          // Model a lost acknowledgement: the first commit succeeded before its response failed.
          local.applySocialDisdainPenalty(lowStarHeroId, penalty, eventId)
          if (ids.size < 3) error("response lost")
        }
      }
      val vm = SummonerViewModel(unreliable, initial.guestPlayerId, "Master")
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
      advanceUntilIdle()
      val victim = save.guestSnapshot.value!!.heroes.first()
      vm.applySocialDisdainPenalty(victim.id)
      advanceUntilIdle()
      assertEquals(3, ids.size)
      assertEquals(1, ids.distinct().size)
      assertEquals(victim.morale - 2L, save.guestSnapshot.value!!.heroes.first().morale)
      assertEquals(0L, vm.uiState.value.lobbySyncRevision)
    } finally { Dispatchers.resetMain() }
  }

  @Test fun terminalWriteFailureReconcilesOptimisticLobbyWithAuthoritativeRoster() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
      val save = LocalGuestSaveManager(context)
      val initial = save.startOrResumeGuestSession()
      val local = LocalGuestDataSource(save)
      var attempts = 0
      val offline = object : SummonerDataSource by local {
        override suspend fun applySocialDisdainPenalty(lowStarHeroId: String, penalty: Int, eventId: String) {
          attempts++; error("offline")
        }
      }
      val vm = SummonerViewModel(offline, initial.guestPlayerId, "Master")
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
      advanceUntilIdle()
      val before = vm.uiState.value.heroes
      repeat(2) {
        vm.applySocialDisdainPenalty(initial.heroes.first().id)
        advanceUntilIdle()
      }
      assertEquals(6, attempts)
      assertEquals(2L, vm.uiState.value.lobbySyncRevision)
      assertEquals(before, vm.uiState.value.heroes)
      assertTrue(vm.uiState.value.bannerFeedback.orEmpty().contains("dữ liệu lưu gần nhất"))
    } finally { Dispatchers.resetMain() }
  }
}
