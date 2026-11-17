package com.example.performance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.FantasySurface
import com.example.ui.theme.ESDesignTokens
import java.util.Locale

/** Snapshot updates at most once per second; normal gameplay never composes this panel. */
@Composable
fun ScenePerformanceOverlay(snapshot: ScenePerformanceSnapshot?, modifier: Modifier = Modifier) {
  val text = remember(snapshot) {
    if (snapshot == null) "Đang lấy mẫu hiệu năng…" else with(snapshot) {
      fun timing(samples: Int, value: Double) =
        if (samples > 0) String.format(Locale.US, "%.2f", value) else "N/A"
      val cache = if (bitmapCacheBytes >= 0) {
        String.format(Locale.US, "%.1f MiB", bitmapCacheBytes / 1_048_576.0)
      } else "N/A"
      val heroes = if (activeHeroes >= 0) "$activeHeroes" else "N/A"
      val monsters = if (activeMonsters >= 0) "$activeMonsters" else "N/A"
      val drawables = if (visibleDrawables >= 0) "$visibleDrawables" else "N/A"
      String.format(Locale.US, "%.1f FPS • avg %.2f / p95 %.2f ms • jank %d\n", fps, averageFrameMs, p95FrameMs, jankyFrames) +
        "CPU update ${timing(updateSamples, averageUpdateMs)} / draw ${timing(drawSamples, averageDrawMs)} ms\n" +
        "Hero $heroes • Quái $monsters • Drawable $drawables • PNG cache $cache\n" +
        "Draw = CPU submit; GPU & atlas chưa đo\n${allocationGcWarning}"
    }
  }
  FantasySurface(
    modifier = modifier.widthIn(max = 360.dp).testTag("scene_performance_overlay"),
    shape = RoundedCornerShape(12.dp),
    color = ESDesignTokens.panel,
    border = BorderStroke(1.dp, ESDesignTokens.cyan.copy(alpha = 0.58f)),
    frameCornerInset = 12.dp,
    compactFrame = true,
  ) {
    Column(
      Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          "HIỆU NĂNG",
          color = ESDesignTokens.cyan,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          "CPU frame pacing • GPU chưa đo",
          color = ESDesignTokens.textMuted,
          style = MaterialTheme.typography.labelSmall,
        )
      }
      Text(
        text = text,
        color = ESDesignTokens.textPrimary,
        style = MaterialTheme.typography.labelSmall,
      )
    }
  }
}
