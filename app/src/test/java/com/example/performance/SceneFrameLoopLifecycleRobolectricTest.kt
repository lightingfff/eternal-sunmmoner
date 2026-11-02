package com.example.performance

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks cancellation against Compose's controlled frame clock; this is not a device FPS test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SceneFrameLoopLifecycleRobolectricTest {
  @get:Rule val compose = createComposeRule()

  private class SceneOwner : LifecycleOwner {
    val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry
  }

  @Test
  fun stoppedLifecycleAndBlockingModalCancelFramesAndResumeWithoutTimeJump() {
    val owner = SceneOwner()
    var sceneActive by mutableStateOf(true)
    val deltas = ArrayList<Float>()
    compose.mainClock.autoAdvance = false
    compose.runOnUiThread { owner.registry.currentState = Lifecycle.State.RESUMED }
    compose.setContent {
      CompositionLocalProvider(LocalLifecycleOwner provides owner) {
        SceneFrameLoop(active = sceneActive, targetFps = 30) { delta, _ -> deltas.add(delta) }
      }
    }

    compose.mainClock.advanceTimeBy(200L)
    compose.runOnIdle { assertTrue("Visible resumed scene must receive frames", deltas.size > 1) }

    // CREATED is Android's stopped lifecycle state, below STARTED/RESUMED.
    val beforeStopped = compose.runOnIdle {
      owner.registry.currentState = Lifecycle.State.CREATED
      deltas.size
    }
    compose.mainClock.advanceTimeBy(5_000L)
    compose.runOnIdle { assertEquals("Stopped scene must do no frame work", beforeStopped, deltas.size) }

    compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
    compose.mainClock.advanceTimeBy(200L)
    compose.runOnIdle {
      assertTrue(deltas.size > beforeStopped)
      assertEquals("Resume starts a fresh clock, excluding the background gap", 0f, deltas[beforeStopped], 0f)
      assertTrue(deltas.drop(beforeStopped + 1).all { it in 0f..0.05f })
    }

    compose.runOnIdle { sceneActive = false }
    // Let the state change recompose and cancel the previous LaunchedEffect before counting.
    compose.mainClock.advanceTimeByFrame()
    val beforeModal = compose.runOnIdle { deltas.size }
    compose.mainClock.advanceTimeBy(5_000L)
    compose.runOnIdle { assertEquals("Blocking modal must do no frame work", beforeModal, deltas.size) }

    compose.runOnIdle { sceneActive = true }
    // With no active frame waiter, first apply recomposition and drain LaunchedEffect;
    // only then advance the observation window for its newly registered frame callbacks.
    compose.mainClock.advanceTimeByFrame()
    compose.waitForIdle()
    compose.mainClock.advanceTimeBy(200L)
    compose.runOnIdle {
      assertTrue(deltas.size > beforeModal)
      assertEquals("Closing a modal also starts a fresh clock", 0f, deltas[beforeModal], 0f)
      assertTrue(deltas.drop(beforeModal + 1).all { it in 0f..0.05f })
      owner.registry.currentState = Lifecycle.State.DESTROYED
    }
  }
}
