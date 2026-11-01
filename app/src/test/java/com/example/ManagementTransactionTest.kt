package com.example

import com.example.data.local.LocalGuestSaveManager
import com.example.data.local.LocalGuestDataSource
import com.example.ui.SummonerViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
class ManagementTransactionTest {
  @Test fun rapidSummonOnlySpendsOnceAndPersistsHero() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
      val manager=LocalGuestSaveManager(org.robolectric.RuntimeEnvironment.getApplication())
      val before=manager.startOrResumeGuestSession("Master Transaction")
      val vm=SummonerViewModel(LocalGuestDataSource(manager),before.guestPlayerId,"Master Transaction")
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
      advanceUntilIdle()
      assertNotNull(vm.uiState.value.profile)
      vm.performHeroSummon();vm.performHeroSummon()
      advanceUntilIdle()
      val after=manager.guestSnapshot.value!!
      assertEquals(before.heroes.size+1,after.heroes.size)
      assertEquals(before.profile.soulShards-20,after.profile.soulShards)
      assertEquals(before.profile.manaCrystals-40,after.profile.manaCrystals)
      val restored=LocalGuestSaveManager(org.robolectric.RuntimeEnvironment.getApplication()).guestSnapshot.value!!
      assertEquals(after.heroes.map{it.id},restored.heroes.map{it.id})
    } finally { Dispatchers.resetMain() }
  }
}
