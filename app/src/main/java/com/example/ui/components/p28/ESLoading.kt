package com.example.ui.components.p28

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Real loading state for screen transitions. [fraction] is only ever set from measured
 * init progress — never from a timer or random source; when no trustworthy progress
 * exists the caller uses [Indeterminate] with a stage label. Text is Vietnamese and
 * never claims a server connection, because guest mode runs fully offline.
 */
sealed interface ESLoadingState {
  data object Hidden : ESLoadingState
  data class Indeterminate(val stage: String) : ESLoadingState
  data class Determinate(val stage: String, val fraction: Float) : ESLoadingState
  data class Failed(val message: String) : ESLoadingState
}

/**
 * Cinematic loading overlay: original backdrop, original sigil, runic progress track
 * with a fill clipped by the real fraction. Static art — no per-frame animation — so
 * it cannot hurt the low-FPS budget; the fill is a clipped Image, not a scaled ornament.
 */
@Composable
fun ESLoadingOverlay(
  state: ESLoadingState,
  modifier: Modifier = Modifier,
) {
  if (state is ESLoadingState.Hidden) return
  val bg = ESP28Assets.image("loading_bg")
  val sigil = ESP28Assets.image("loading_sigil")
  val track = ESP28Assets.image("progress_track")
  val fill = ESP28Assets.image("progress_fill")
  val stage = when (state) {
    is ESLoadingState.Indeterminate -> state.stage
    is ESLoadingState.Determinate -> state.stage
    is ESLoadingState.Failed -> state.message
    ESLoadingState.Hidden -> ""
  }
  Box(modifier = modifier.fillMaxSize().background(Color(0xFF0A0E18))) {
    if (bg != null) {
      Image(bg, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
    }
    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      if (sigil != null) {
        Image(sigil, contentDescription = null, modifier = Modifier.size(96.dp))
      }
      Spacer(Modifier.height(16.dp))
      Text(
        text = stage,
        color = ESTokens.IvoryPaper,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.testTag("es_loading_stage"),
      )
      Spacer(Modifier.height(14.dp))
      if (state !is ESLoadingState.Failed) {
        Box(modifier = Modifier.width(240.dp).height(20.dp)) {
          if (track != null) {
            Image(track, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
          }
          if (state is ESLoadingState.Determinate && fill != null) {
            val f = if (state.fraction.isNaN()) 0f else state.fraction.coerceIn(0f, 1f)
            Image(
              fill,
              contentDescription = null,
              modifier = Modifier.fillMaxWidth(f).matchParentSize(),
              contentScale = ContentScale.FillBounds,
            )
          }
        }
      }
      if (state is ESLoadingState.Failed) {
        Text(
          text = state.message,
          color = ESTokens.DangerGarnet,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.testTag("es_loading_error"),
        )
      }
    }
  }
}
