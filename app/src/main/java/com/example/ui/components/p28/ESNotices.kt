package com.example.ui.components.p28

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

/** Priority levels for game notices. CEREMONIAL/CRITICAL outrank TACTICAL outrank INFO. */
enum class NoticePriority { INFO, TACTICAL, CRITICAL, CEREMONIAL }

/**
 * UI adapter for a real game event. This is NOT a domain event and carries no
 * side effects: the combat/lobby code maps its own events into this shape, and the
 * notice layer only reads it. Percentages, hero names and skill names must come from
 * the source event — nothing here is hard-coded from reference art.
 */
data class GameNoticeUi(
  val stableId: String,
  val priority: NoticePriority,
  val title: String?,
  val body: String,
  val iconKey: String? = null,
  val createdAtMs: Long = 0L,
  val relatedHeroIds: List<String> = emptyList(),
  val dismissible: Boolean = true,
)

/**
 * Single state owner for the notice queue. Compose observes [notices]; producers call
 * [offer] from their own event handlers — offering never happens during composition.
 * Dedup is by [GameNoticeUi.stableId] (re-offering the same id updates the entry),
 * ordering is priority-desc then recency, and the retained log is capped so a burst of
 * events cannot grow memory without bound.
 */
class NoticeCenter {
  private val _notices = mutableStateOf<List<GameNoticeUi>>(emptyList())
  val notices: List<GameNoticeUi> by _notices

  fun offer(notice: GameNoticeUi) {
    val next = _notices.value.filterNot { it.stableId == notice.stableId } + notice
    _notices.value = next
      .sortedWith(compareByDescending<GameNoticeUi> { it.priority.ordinal }.thenByDescending { it.createdAtMs })
      .take(MAX_RETAINED)
  }

  fun dismiss(stableId: String) {
    _notices.value = _notices.value.filterNot { it.stableId == stableId }
  }

  fun clear() {
    _notices.value = emptyList()
  }

  companion object {
    const val MAX_RETAINED = 8
    /** At most this many notices render at once: 1 major + 2 minor. */
    const val MAX_VISIBLE = 3
  }
}

/** Frame asset id for a priority. */
fun noticeFrameId(priority: NoticePriority): String = when (priority) {
  NoticePriority.INFO -> "notice_standard"
  NoticePriority.TACTICAL, NoticePriority.CRITICAL -> "notice_warning"
  NoticePriority.CEREMONIAL -> "notice_ceremony"
}

/**
 * 9-slice notice frame with a dark center fill, so body text stays readable over any
 * world pixel behind it. Falls back to a vector panel when the PNG is missing.
 */
@Composable
fun ESNoticeFrame(
  priority: NoticePriority,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val frameId = noticeFrameId(priority)
  val frame = ESP28Assets.image(frameId)
  val inset = ESP28Assets.entry(frameId)?.nineSliceInsetPx ?: 24
  Box(modifier) {
    if (frame != null) {
      Canvas(Modifier.matchParentSize()) {
        drawRect(color = ESTokens.NoticeFill)
        drawNineSlice(frame, inset)
      }
    } else {
      Box(
        Modifier.matchParentSize()
          .background(ESTokens.NoticeFill, RoundedCornerShape(10.dp))
          .border(1.dp, ESTokens.FrostSilver, RoundedCornerShape(10.dp)),
      )
    }
    content()
  }
}

/** One notice row: optional icon, title+body, dismiss button. */
@Composable
fun ESNoticeSurface(
  notice: GameNoticeUi,
  onDismiss: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  ESNoticeFrame(priority = notice.priority, modifier = modifier.testTag("notice_${notice.stableId}")) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val icon = notice.iconKey?.let { ESP28Assets.icon(it) }
      if (icon != null) Image(icon, null, Modifier.size(20.dp))
      Column(Modifier.weight(1f)) {
        if (notice.title != null) {
          Text(
            notice.title,
            color = ESTokens.AntiqueGold,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          notice.body,
          color = ESTokens.IvoryPaper,
          style = MaterialTheme.typography.bodySmall,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (notice.dismissible) {
        IconButton(
          onClick = { onDismiss(notice.stableId) },
          modifier = Modifier.size(32.dp).testTag("dismiss_notice_${notice.stableId}"),
        ) {
          Image(Icons.Default.Close, contentDescription = "Đóng thông báo")
        }
      }
    }
  }
}

/**
 * Visible queue: at most one CEREMONIAL/CRITICAL notice plus up to [NoticeCenter.MAX_VISIBLE]
 * total, newest-priority first. Rendered top-end so it never covers bottom tactical
 * commands or the joystick.
 */
@Composable
fun ESNoticeStack(
  center: NoticeCenter,
  modifier: Modifier = Modifier,
) {
  val visible = center.notices
    .let { list ->
      val major = list.firstOrNull { it.priority == NoticePriority.CEREMONIAL || it.priority == NoticePriority.CRITICAL }
      val others = list.filter { it !== major }.take(if (major != null) NoticeCenter.MAX_VISIBLE - 1 else NoticeCenter.MAX_VISIBLE)
      if (major != null) listOf(major) + others else others
    }
  if (visible.isEmpty()) return
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
    horizontalAlignment = Alignment.End,
  ) {
    for (notice in visible) {
      ESNoticeSurface(notice = notice, onDismiss = { center.dismiss(it) })
    }
  }
}

/**
 * P28D: single in-flow banner driven by a REAL game event string (summon result, save
 * status, expedition outcome). Uses the same Astral Reliquary notice frame as the
 * battle/stack notices so every transient message shares one visual language.
 */
@Composable
fun ESBannerNotice(
  text: String,
  modifier: Modifier = Modifier,
  priority: NoticePriority = NoticePriority.INFO,
) {
  ESNoticeFrame(priority = priority, modifier = modifier) {
    Text(
      text,
      color = ESTokens.IvoryPaper,
      style = MaterialTheme.typography.labelMedium,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
    )
  }
}
