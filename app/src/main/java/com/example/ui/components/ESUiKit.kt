package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ESDesignTokens

/** Reusable status meaning used by badges, prompts and compact HUD readouts. */
enum class ESUiTone {
  ACCENT,
  NEUTRAL,
  SUCCESS,
  WARNING,
  DANGER,
}

private fun ESUiTone.color(): Color = when (this) {
  ESUiTone.ACCENT -> ESDesignTokens.cyan
  ESUiTone.NEUTRAL -> ESDesignTokens.textSecondary
  ESUiTone.SUCCESS -> ESDesignTokens.success
  ESUiTone.WARNING -> ESDesignTokens.warning
  ESUiTone.DANGER -> ESDesignTokens.danger
}

/**
 * A deliberate section card: shared frame, restrained accent rail, and consistent
 * heading/body spacing. Content remains live Compose UI and is never baked into art.
 */
@Composable
fun ESSectionCard(
  modifier: Modifier = Modifier,
  title: String? = null,
  eyebrow: String? = null,
  subtitle: String? = null,
  accent: Color = ESDesignTokens.gold,
  padding: Dp = 14.dp,
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = RoundedCornerShape(ESDesignTokens.radiusLarge)
  FantasySurface(
    modifier = modifier.border(1.dp, accent.copy(alpha = 0.42f), shape),
    shape = shape,
    color = ESDesignTokens.panelRaised,
    contentColor = ESDesignTokens.textPrimary,
    frameCornerInset = 18.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(padding),
      verticalArrangement = Arrangement.spacedBy(ESDesignTokens.spaceMd),
    ) {
      if (title != null) {
        ESSectionHeading(
          title = title,
          eyebrow = eyebrow,
          subtitle = subtitle,
          accent = accent,
        )
      }
      content()
    }
  }
}

/** Clear, repeatable hierarchy for panels without inventing a different title style. */
@Composable
fun ESSectionHeading(
  title: String,
  modifier: Modifier = Modifier,
  eyebrow: String? = null,
  subtitle: String? = null,
  accent: Color = ESDesignTokens.gold,
  trailing: (@Composable RowScope.() -> Unit)? = null,
) {
  Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (!eyebrow.isNullOrBlank()) {
          Text(
            text = eyebrow,
            color = accent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = title,
          color = ESDesignTokens.textPrimary,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.semantics { heading() },
        )
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            color = ESDesignTokens.textMuted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      if (trailing != null) {
        Spacer(Modifier.size(ESDesignTokens.spaceSm))
        Row(content = trailing)
      }
    }
    Box(
      Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(ESDesignTokens.hairline),
    )
  }
}

/** A compact, high-contrast data tile for real profile, party and objective values. */
@Composable
fun ESMetricTile(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  detail: String? = null,
  icon: ImageVector? = null,
  accent: Color = ESDesignTokens.gold,
  testTag: String? = null,
) {
  val shape = RoundedCornerShape(ESDesignTokens.radiusMedium)
  Column(
    modifier = modifier
      .clip(shape)
      .background(ESDesignTokens.panelInset)
      .border(1.dp, accent.copy(alpha = 0.34f), shape)
      .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
      .padding(horizontal = 10.dp, vertical = 9.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (icon != null) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
      } else {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(accent))
      }
      Text(
        text = label,
        color = ESDesignTokens.textMuted,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Text(
      text = value,
      color = ESDesignTokens.textPrimary,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    if (!detail.isNullOrBlank()) {
      Text(
        text = detail,
        color = ESDesignTokens.textSecondary,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/** Small state chip with both color and readable text; color is never the only signal. */
@Composable
fun ESStatePill(
  text: String,
  modifier: Modifier = Modifier,
  tone: ESUiTone = ESUiTone.ACCENT,
  icon: ImageVector? = null,
) {
  val accent = tone.color()
  val shape = RoundedCornerShape(999.dp)
  Row(
    modifier = modifier
      .clip(shape)
      .background(accent.copy(alpha = 0.14f))
      .border(1.dp, accent.copy(alpha = 0.52f), shape)
      .padding(horizontal = 9.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Icon(
      imageVector = icon ?: Icons.Default.AutoAwesome,
      contentDescription = null,
      tint = accent,
      modifier = Modifier.size(13.dp),
    )
    Text(
      text = text,
      color = accent,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/** Tappable destination tile with a stable 48dp minimum target and semantic button role. */
@Composable
fun ESActionTile(
  title: String,
  description: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  accent: Color = ESDesignTokens.gold,
  testTag: String? = null,
) {
  FantasyChoiceButton(
    onClick = onClick,
    modifier = modifier
      .heightIn(min = ESDesignTokens.minTouchTarget + 16.dp)
      .then(if (testTag == null) Modifier else Modifier.testTag(testTag)),
    shape = RoundedCornerShape(ESDesignTokens.radiusMedium),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.46f)),
    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
      }
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          color = ESDesignTokens.textPrimary,
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = description,
          color = ESDesignTokens.textMuted,
          style = MaterialTheme.typography.labelSmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
