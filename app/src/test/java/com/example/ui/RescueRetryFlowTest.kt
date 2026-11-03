package com.example.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.example.data.SummonerDataSource
import com.example.data.firebase.ExpeditionSettlementResult
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.data.model.CourierOutcome
import com.example.data.model.HeroRecord
import com.example.data.model.MasterProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
class RescueRetryFlowTest {
  private fun isolatedContext(): Context {
    val base = ApplicationProvider.getApplicationContext<Context>()
    val isolated = Files.createTempDirectory(base.cacheDir.toPath(), "rescue-retry-").toFile()
    return object: ContextWrapper(base) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = isolated
    }
  }

  @Test fun retryWaitsForSavedOutcomeAndPreparationPreservesInjuriesAndRewards() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
      val context = isolatedContext()
      val manager = LocalGuestSaveManager(context)
      val initial = manager.startOrResumeGuestSession()
      val local = LocalGuestDataSource(manager)
      val saveGate = CompletableDeferred<Unit>()
      val repository = object: SummonerDataSource by local {
        override suspend fun settleExpedition(profile: MasterProfile, squadHeroes: List<HeroRecord>,
          fallenHeroIds: Set<String>, floorNumber: Int, victory: Boolean, retreatUsed: Boolean,
          bonusGold: Long, bonusMana: Long, discoveredNodeIds: Set<String>,
          courierOutcome: CourierOutcome?, courierMemoryHeroId: String?): ExpeditionSettlementResult {
          saveGate.await()
          return local.settleExpedition(profile,squadHeroes,fallenHeroIds,floorNumber,victory,
            retreatUsed,bonusGold,bonusMana,discoveredNodeIds,courierOutcome,courierMemoryHeroId)
        }
      }
      val vm = SummonerViewModel(repository,initial.guestPlayerId,"Retry Master")
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
      advanceUntilIdle()
      vm.launchTowerExpedition()
      advanceUntilIdle()
      assertEquals(GameScreenMode.TOWER_COMBAT,vm.uiState.value.screenMode)
      vm.retryTowerExpedition() // No result has even been submitted.
      advanceUntilIdle()
      assertEquals(GameScreenMode.TOWER_COMBAT,vm.uiState.value.screenMode)
      val fallen = initial.heroes.first { it.inActiveSquad }.id
      vm.finishTowerExpedition(false,setOf(fallen),retreatUsed=true,courierOutcome=CourierOutcome.FAILED)
      advanceUntilIdle()
      assertTrue(vm.uiState.value.isBusyAction)
      vm.retryTowerExpedition() // Atomic settlement is still waiting.
      advanceUntilIdle()
      assertEquals(GameScreenMode.TOWER_COMBAT,vm.uiState.value.screenMode)
      assertNull(vm.uiState.value.lastSettlement)
      saveGate.complete(Unit)
      advanceUntilIdle()
      assertNotNull(vm.uiState.value.lastSettlement)
      val settled = manager.guestSnapshot.value!!
      assertTrue(settled.heroes.first { it.id==fallen }.isCurrentlyInjured())
      assertEquals(1L,settled.profile.totalExpeditions)
      vm.retryTowerExpedition()
      advanceUntilIdle()
      assertEquals(GameScreenMode.SQUAD_PREPARATION,vm.uiState.value.screenMode)
      assertNull(vm.uiState.value.lastSettlement)
      vm.launchTowerExpedition()
      advanceUntilIdle()
      assertEquals(GameScreenMode.TOWER_COMBAT,vm.uiState.value.screenMode)
      assertEquals(settled,LocalGuestSaveManager(context).guestSnapshot.value)
      assertTrue(vm.uiState.value.heroes.first { it.id==fallen }.isCurrentlyInjured())
      assertNull(vm.uiState.value.lastSettlement)
    } finally { Dispatchers.resetMain() }
  }

  @Test fun failedSaveCannotPrepareRetryUntilSaveRetrySucceeds() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
      val manager = LocalGuestSaveManager(isolatedContext())
      val initial = manager.startOrResumeGuestSession()
      val local = LocalGuestDataSource(manager)
      var failSave = true
      val repository = object: SummonerDataSource by local {
        override suspend fun settleExpedition(profile: MasterProfile, squadHeroes: List<HeroRecord>,
          fallenHeroIds: Set<String>, floorNumber: Int, victory: Boolean, retreatUsed: Boolean,
          bonusGold: Long, bonusMana: Long, discoveredNodeIds: Set<String>,
          courierOutcome: CourierOutcome?, courierMemoryHeroId: String?): ExpeditionSettlementResult {
          if (failSave) throw IOException("Simulated atomic save failure")
          return local.settleExpedition(profile,squadHeroes,fallenHeroIds,floorNumber,victory,
            retreatUsed,bonusGold,bonusMana,discoveredNodeIds,courierOutcome,courierMemoryHeroId)
        }
      }
      val vm = SummonerViewModel(repository,initial.guestPlayerId,"Retry Master")
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
      advanceUntilIdle()
      vm.launchTowerExpedition()
      advanceUntilIdle()
      vm.finishTowerExpedition(false,emptySet(),retreatUsed=true,courierOutcome=CourierOutcome.FAILED)
      advanceUntilIdle()
      assertFalse(vm.uiState.value.isBusyAction)
      assertNull(vm.uiState.value.lastSettlement)
      vm.retryTowerExpedition()
      advanceUntilIdle()
      assertEquals(GameScreenMode.TOWER_COMBAT,vm.uiState.value.screenMode)
      assertEquals(0L,manager.guestSnapshot.value!!.profile.totalExpeditions)
      failSave = false
      vm.finishTowerExpedition(false,emptySet(),retreatUsed=true,courierOutcome=CourierOutcome.FAILED)
      advanceUntilIdle()
      vm.retryTowerExpedition()
      advanceUntilIdle()
      assertEquals(GameScreenMode.SQUAD_PREPARATION,vm.uiState.value.screenMode)
      assertEquals(1L,manager.guestSnapshot.value!!.profile.totalExpeditions)
    } finally { Dispatchers.resetMain() }
  }
}
