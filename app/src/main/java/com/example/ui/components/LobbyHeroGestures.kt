package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput

/** One owner arbitrates tap, early pan/pinch and held-hero drag, without competing detectors. */
@Composable
fun Modifier.lobbyHeroGestures(
  enabled: Boolean,
  onTap: (Offset) -> Unit,
  onTransform: (Offset, Offset, Float) -> Unit,
  onPickUp: (Offset) -> Boolean,
  onDrag: (Offset) -> Unit,
  onDrop: (Offset) -> Unit,
  onCancel: () -> Unit,
): Modifier {
  val tap = rememberUpdatedState(onTap); val transform = rememberUpdatedState(onTransform)
  val pick = rememberUpdatedState(onPickUp); val drag = rememberUpdatedState(onDrag)
  val drop = rememberUpdatedState(onDrop); val cancel = rememberUpdatedState(onCancel)
  return pointerInput(enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
      val down = awaitFirstDown()
      var firstMotion: PointerEvent? = null
      var released = false
      var cancelled = false
      val beforeTimeout = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
        while (true) {
          val event = awaitPointerEvent()
          if (event.changes.any { it.isConsumed }) { cancelled = true; break }
          val current = event.changes.firstOrNull { it.id == down.id }
          if (current == null || !current.pressed) { released = true; break }
          if (event.changes.count { it.pressed } > 1 || (current.position - down.position).getDistance() > viewConfiguration.touchSlop) {
            firstMotion = event; break
          }
        }
        true
      }
      if (cancelled) return@awaitEachGesture
      if (released) { tap.value(down.position); return@awaitEachGesture }
      val picked = beforeTimeout == null && pick.value(down.position)
      if (picked) {
        var dropped = false
        try {
          while (true) {
            val event = awaitPointerEvent()
            val current = event.changes.firstOrNull { it.id == down.id }
            // A second finger cancels pickup; it cannot turn a pinch into an assignment.
            if (event.changes.count { it.pressed } > 1 || current == null || current.isConsumed) break
            current.consume()
            if (!current.pressed) { dropped = true; drop.value(current.position); break }
            drag.value(current.position)
          }
        } finally { if (!dropped) cancel.value() }
      } else {
        var event = firstMotion ?: awaitPointerEvent()
        var transformed = firstMotion != null
        while (true) {
          if (event.changes.any { it.isConsumed }) break
          if (event.changes.none { it.pressed }) {
            if (!transformed) tap.value(down.position)
            break
          }
          val current = event.changes.firstOrNull { it.id == down.id }
          transformed = transformed || event.changes.count { it.pressed } > 1 ||
            current == null || (current.position - down.position).getDistance() > viewConfiguration.touchSlop
          if (transformed) {
            val pan = event.calculatePan(); val zoom = event.calculateZoom()
            transform.value(event.calculateCentroid(useCurrent = false), pan, zoom)
            event.changes.forEach { if (it.position != it.previousPosition) it.consume() }
          }
          event = awaitPointerEvent()
        }
      }
    }
  }
}
