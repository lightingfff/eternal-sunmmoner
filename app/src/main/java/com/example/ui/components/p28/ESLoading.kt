package com.example.ui.components.p28

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FantasySkin
import com.example.ui.components.FantasySurface
import java.util.concurrent.atomic.AtomicInteger

/**
 * Real loading state for actual waits. [fraction] is only supplied when the caller has
 * measured trustworthy progress; otherwise use [Indeterminate] and a real stage label.
 * Text is Vietnamese and must not claim a cloud connection for guest/offline work.
 */
sealed interface ESLoadingState {
  data object Hidden : ESLoadingState
  data class Indeterminate(val stage: String, val tip: String? = null) : ESLoadingState
  data class Determinate(val stage: String, val fraction: Float, val tip: String? = null) : ESLoadingState
  data class Failed(val message: String) : ESLoadingState
}

/**
 * Verified, concise player tips. The cursor advances only when a loading entry needs a
 * fallback tip; it never drives progress or delays completion. Copy is grounded in the
 * current Tower gate/floor/squad checks in SummonerViewModel.
 */
internal object ESLoadingTips {
  val all: List<String> = listOf(
    "Anh hùng bị thương hoặc không còn HP sẽ không thể xuất chinh.",
    "Tầng chưa mở chỉ khả dụng sau khi bạn vượt qua tầng liền trước.",
    "Từ Cổng Tháp, hãy chọn tầng rồi chuẩn bị đội hình trước khi xuất chinh.",
    "Thiếu người khỏe? Mở Nhà → Điều dưỡng để trị thương rồi thử lại.",
  )

  private val cursor = AtomicInteger(0)

  fun next(): String = all[Math.floorMod(cursor.getAndIncrement(), all.size)]
}

/**
 * Cinematic loading overlay with the shipped backdrop and sigil, a high-contrast
 * pixel-fantasy tip frame, and a progress fill only for a measured fraction. Static
 * artwork/scrim keeps the wait screen inexpensive; no animation or timer controls work.
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
  val fontScale = LocalDensity.current.fontScale
  val largeFontScale = fontScale >= 1.15f
  val stage = when (state) {
    is ESLoadingState.Indeterminate -> state.stage
    is ESLoadingState.Determinate -> state.stage
    is ESLoadingState.Failed -> "Không thể tải dữ liệu"
    ESLoadingState.Hidden -> ""
  }
  val requestedTip = when (state) {
    is ESLoadingState.Indeterminate -> state.tip
    is ESLoadingState.Determinate -> state.tip
    is ESLoadingState.Failed, ESLoadingState.Hidden -> null
  }
  val needsFallbackTip =
    (state is ESLoadingState.Indeterminate || state is ESLoadingState.Determinate) &&
      requestedTip.isNullOrBlank()
  val fallbackTip = remember(needsFallbackTip) {
    if (needsFallbackTip) ESLoadingTips.next() else null
  }
  val tip = requestedTip?.takeIf { it.isNotBlank() } ?: fallbackTip

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      // Do not forward touches to a route while its real loading state is visible.
      .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
      .background(Color(0xFF0A0E18)),
  ) {
    val compact = maxHeight < 420.dp
    if (bg != null) {
      Image(
        bg,
        contentDescription = null,
        modifier = Modifier.matchParentSize(),
        contentScale = ContentScale.Crop,
      )
    }
    Box(
      modifier = Modifier
        .matchParentSize()
        .background(
          Brush.verticalGradient(
            listOf(Color(0xAA050811), Color(0x550B1420), Color(0xCC05080E)),
          ),
        ),
    )

    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .widthIn(max = 560.dp)
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(
          horizontal = if (compact) 16.dp else 24.dp,
          vertical = if (compact) 8.dp else 20.dp,
        ),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      if (sigil != null) {
        Image(
          sigil,
          contentDescription = null,
          modifier = Modifier.size(if (compact) 68.dp else 96.dp),
        )
      }
      Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
      Text(
        text = stage,
        color = if (state is ESLoadingState.Failed) ESTokens.DangerGarnet else ESTokens.IvoryPaper,
        style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        maxLines = when {
          compact && largeFontScale -> 4
          compact -> 3
          largeFontScale -> 6
          else -> 4
        },
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .fillMaxWidth()
          .semantics { liveRegion = LiveRegionMode.Polite }
          .testTag("es_loading_stage"),
      )

      if (state is ESLoadingState.Determinate) {
        val fraction = if (state.fraction.isNaN()) 0f else state.fraction.coerceIn(0f, 1f)
        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth(0.78f)
            .height(if (compact) 16.dp else 20.dp)
            .semantics {
              progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            }
            .testTag("es_loading_progress_track"),
        ) {
          if (track != null) {
            Image(
              track,
              contentDescription = null,
              modifier = Modifier.matchParentSize(),
              contentScale = ContentScale.FillBounds,
            )
          }
          if (fill != null && fraction > 0f) {
            Box(
              modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .testTag("es_loading_progress_fill"),
            ) {
              Image(
                fill,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
              )
            }
          }
        }
      }

      if (state is ESLoadingState.Failed) {
        Spacer(Modifier.height(10.dp))
        FantasySurface(
          modifier = Modifier.fillMaxWidth().testTag("es_loading_error_panel"),
          shape = CutCornerShape(
            topStart = 8.dp, topEnd = 2.dp, bottomEnd = 8.dp, bottomStart = 2.dp,
          ),
          color = Color(0xE6111823),
          border = BorderStroke(1.dp, ESTokens.DangerGarnet.copy(alpha = 0.82f)),
          frameAssetId = FantasySkin.PanelFrameId,
          compactFrame = true,
        ) {
          Text(
            text = state.message,
            color = ESTokens.IvoryPaper,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp)
              .semantics { liveRegion = LiveRegionMode.Polite }
              .testTag("es_loading_error"),
          )
        }
      } else if (tip != null) {
        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
        ESLoadingTipPanel(
          tip = tip,
          compact = compact,
          largeFontScale = largeFontScale,
        )
      }
    }
  }
}

@Composable
private fun ESLoadingTipPanel(
  tip: String,
  compact: Boolean,
  largeFontScale: Boolean,
) {
  val panelShape = CutCornerShape(
    topStart = 8.dp, topEnd = 2.dp, bottomEnd = 8.dp, bottomStart = 2.dp,
  )

  Box(
    modifier = Modifier
      .widthIn(max = 480.dp)
      .fillMaxWidth()
      .aspectRatio(2.6f)
      .testTag("es_loading_tip_panel"),
    contentAlignment = Alignment.Center,
  ) {
    FantasySurface(
      modifier = Modifier.matchParentSize(),
      shape = panelShape,
      color = Color(0xE6111823),
      border = if (ESP28Assets.image("loading_tip_frame") == null) {
        BorderStroke(1.dp, ESTokens.AntiqueGold.copy(alpha = 0.78f))
      } else {
        null
      },
      frameAssetId = "loading_tip_frame",
      frameCornerInset = 18.dp,
      compactFrame = true,
    ) {}
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          horizontal = 36.dp,
          vertical = when {
            largeFontScale -> 2.dp
            compact -> 4.dp
            else -> 6.dp
          },
        ),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "MẸO CHINH PHẠT",
        color = ESTokens.AntiqueGold,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 1.1.sp,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .semantics { heading() }
          .testTag("es_loading_tip_heading"),
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = tip,
        color = ESTokens.IvoryPaper,
        style = if (compact || largeFontScale) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        // Avoid ellipsizing a gameplay tip solely because the system font is enlarged.
        maxLines = if (compact || largeFontScale) 3 else 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag("es_loading_tip"),
      )
    }
  }
}
