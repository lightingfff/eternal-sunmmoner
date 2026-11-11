package com.example.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.data.model.HeroStatusType
import com.example.ui.GameScreenMode
import com.example.ui.SummonerViewModel
import java.io.File
import java.nio.file.Files
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExpeditionLaunchVmTest {
  @Test fun expiredInjuryWithZeroHpStillRequiresHouseCareBeforeDeployment() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
      val base = ApplicationProvider.getApplicationContext<Context>()
      val dir = Files.createTempDirectory(base.cacheDir.toPath(), "launch-hp-").toFile()
      val context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = dir
      }
      val manager = LocalGuestSaveManager(context)
      val initial = manager.startOrResumeGuestSession()
      manager.mutateSnapshot { s -> s.copy(heroes = s.heroes.map {
        it.copy(currentHp = 0L, inActiveSquad = true, status = HeroStatusType.INJURED.name,
          injuryRecoveryUntilEpochSec = 1L)
      }) }
      val vm = SummonerViewModel(LocalGuestDataSource(manager), initial.guestPlayerId, "House first")
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
      advanceUntilIdle()
      assertTrue(vm.uiState.value.heroes.all { !it.isCurrentlyInjured() && it.currentHp == 0L })
      vm.launchTowerExpedition()
      advanceUntilIdle()
      assertEquals(GameScreenMode.SANCTUARY_LOBBY, vm.uiState.value.screenMode)
      assertNull(vm.uiState.value.expeditionAttemptId)
      assertNull(vm.uiState.value.expeditionSquad)
      assertTrue(vm.uiState.value.bannerFeedback!!.contains("Nhà"))
      assertEquals(0L, manager.guestSnapshot.value!!.profile.totalExpeditions)
      assertTrue(manager.guestSnapshot.value!!.heroes.all { it.currentHp == 0L })
    } finally { Dispatchers.resetMain() }
  }

  @Test fun duplicateCombatLaunchPreservesAttemptAndSquadThenPreparedRetryCreatesNewAttempt() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
      val base = ApplicationProvider.getApplicationContext<Context>()
      val dir = Files.createTempDirectory(base.cacheDir.toPath(), "launch-duplicate-").toFile()
      val context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = dir
      }
      val manager = LocalGuestSaveManager(context)
      val initial = manager.startOrResumeGuestSession()
      val vm = SummonerViewModel(LocalGuestDataSource(manager), initial.guestPlayerId, "Stable attempt")
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
      advanceUntilIdle()
      vm.launchTowerExpedition()
      advanceUntilIdle()
      val originalId = vm.uiState.value.expeditionAttemptId!!
      val originalSquad = vm.uiState.value.expeditionSquad!!
      assertTrue(originalSquad.isNotEmpty())
      manager.mutateSnapshot { s -> s.copy(heroes = s.heroes.map { it.copy(morale = 20L) }) }
      advanceUntilIdle()
      // A delayed deployment callback must neither re-snapshot the party nor replace the ID.
      vm.launchTowerExpedition()
      vm.launchTowerExpedition()
      advanceUntilIdle()
      assertEquals(originalId, vm.uiState.value.expeditionAttemptId)
      assertEquals(originalSquad, vm.uiState.value.expeditionSquad)
      vm.finishTowerExpedition(false, emptySet(), retreatUsed = true)
      advanceUntilIdle()
      assertNotNull(vm.uiState.value.lastSettlement)
      assertEquals(1L, manager.guestSnapshot.value!!.profile.totalExpeditions)
      vm.retryTowerExpedition()
      advanceUntilIdle()
      assertEquals(GameScreenMode.SQUAD_PREPARATION, vm.uiState.value.screenMode)
      vm.launchTowerExpedition()
      advanceUntilIdle()
      val retryId = vm.uiState.value.expeditionAttemptId!!
      assertNotEquals(originalId, retryId)
      // Stale retry/deploy callbacks must not restart an unsettled new attempt.
      vm.retryTowerExpedition() // No saved result for this new attempt; cannot leave combat.
      vm.launchTowerExpedition()
      advanceUntilIdle()
      assertEquals(retryId, vm.uiState.value.expeditionAttemptId)
      assertEquals(GameScreenMode.TOWER_COMBAT, vm.uiState.value.screenMode)
      assertNull(vm.uiState.value.lastSettlement)
    } finally { Dispatchers.resetMain() }
  }

}
