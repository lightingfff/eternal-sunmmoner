package com.example.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

/** The snapshot changes at most once per second; normal gameplay never composes this panel. */
@Composable
fun ScenePerformanceOverlay(snapshot: ScenePerformanceSnapshot?, modifier: Modifier = Modifier) {
  val text = remember(snapshot) {
    if (snapshot == null) "Đang lấy mẫu hiệu năng…" else with(snapshot) {
      fun timing(samples: Int, value: Double) = if (samples > 0) String.format(Locale.US, "%.2f", value) else "N/A"
      val cache = if (bitmapCacheBytes >= 0) String.format(Locale.US, "%.1f MiB", bitmapCacheBytes / 1_048_576.0) else "N/A"
      val heroes = if (activeHeroes >= 0) "$activeHeroes" else "N/A"
      val monsters = if (activeMonsters >= 0) "$activeMonsters" else "N/A"
      val drawables = if (visibleDrawables >= 0) "$visibleDrawables" else "N/A"
      String.format(Locale.US, "%.1f FPS • avg %.2f / p95 %.2f ms • jank %d\n", fps, averageFrameMs, p95FrameMs, jankyFrames) +
        "CPU update ${timing(updateSamples, averageUpdateMs)} / draw ${timing(drawSamples, averageDrawMs)} ms\n" +
        "Hero $heroes • Quái $monsters • Drawable $drawables • PNG cache $cache\n" +
        "Draw = CPU submit, GPU/GC/allocation & atlas khác: chưa đo"
    }
  }
  Text(
    text = text,
    color = Color.White,
    style = MaterialTheme.typography.labelSmall,
    modifier = modifier.background(Color(0xDA090D15)).padding(8.dp),
  )
}
