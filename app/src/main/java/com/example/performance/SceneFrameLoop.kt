package com.example.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.isActive

/** One display-vsync driver per visible scene. No timers survive a modal or lifecycle pause. */
@Composable
fun SceneFrameLoop(
  active: Boolean,
  targetFps: Int,
  onFrame: (deltaSeconds: Float, frameTimeNanos: Long) -> Unit,
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val currentFrame by rememberUpdatedState(onFrame)
  LaunchedEffect(lifecycleOwner, active, targetFps) {
    if (!active) return@LaunchedEffect
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      // Recreated after every resume: time spent in the background never advances gameplay.
      val pacer = SceneFramePacer(targetFps)
      while (isActive) {
        withFrameNanos { nanos ->
          if (pacer.onVsync(nanos)) currentFrame(pacer.deltaSeconds, nanos)
        }
      }
    }
  }
}
