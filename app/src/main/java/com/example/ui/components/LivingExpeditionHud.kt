package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.game.combat.ExpeditionDecision
import com.example.game.combat.ExpeditionSiteKnowledge
import com.example.game.combat.ExpeditionSiteState
import com.example.ui.theme.ESDesignTokens

/** A decision replaces travel controls, keeping the real expedition visible on short screens. */
@Composable
fun LivingExpeditionHud(
  objective: String,
  decision: ExpeditionDecision?,
  sites: List<ExpeditionSiteKnowledge>,
  automationEnabled: Boolean,
  canRedirect: Boolean,
  enabled: Boolean,
  onResume: () -> Unit,
  onExplore: (String) -> Unit,
  onChoose: (String) -> Unit,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
) {
  FantasySurface(
    modifier = modifier.testTag("living_expedition_hud"),
    color = ESDesignTokens.panel,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, ESDesignTokens.cyan.copy(alpha = 0.52f)),
  ) {
    if (compact && decision == null) {
      Row(
        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            objective,
            modifier = Modifier.testTag("living_expedition_objective"),
            style = MaterialTheme.typography.labelMedium,
            color = ESDesignTokens.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            if (automationEnabled) "Đội tự thám hiểm" else "Đang chờ lệnh",
            style = MaterialTheme.typography.labelSmall,
            color = ESDesignTokens.success,
            maxLines = 1,
          )
        }
        ResumeExplorationButton(automationEnabled, enabled, onResume, Modifier.width(92.dp))
        sites.forEach { site ->
          ExploreSiteButton(site, enabled && canRedirect, onExplore, Modifier.width(88.dp))
        }
      }
    } else {
      Column(
        Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (decision != null) {
          Text(
            decision.promptVi,
            modifier = Modifier.testTag("living_expedition_objective"),
            style = MaterialTheme.typography.labelMedium,
            color = ESDesignTokens.gold,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            decision.options.forEach { option ->
              FantasyChoiceButton(
                onClick = { onChoose(option.id) },
                enabled = enabled && option.enabled,
                modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag("living_decision_${option.id}"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Text(
                    option.labelVi,
                    style = MaterialTheme.typography.labelMedium,
                    color = ESDesignTokens.gold,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    option.consequenceVi,
                    style = MaterialTheme.typography.labelSmall,
                    color = ESDesignTokens.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              objective,
              modifier = Modifier.weight(1f).testTag("living_expedition_objective"),
              style = MaterialTheme.typography.labelMedium,
              color = ESDesignTokens.textPrimary,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              if (automationEnabled) "TỰ ĐỘNG" else "CHỜ LỆNH",
              style = MaterialTheme.typography.labelSmall,
              color = ESDesignTokens.success,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
            )
          }
          Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ResumeExplorationButton(automationEnabled, enabled, onResume, Modifier.weight(1.15f))
            sites.forEach { site ->
              ExploreSiteButton(site, enabled && canRedirect, onExplore, Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ResumeExplorationButton(
  automationEnabled: Boolean,
  enabled: Boolean,
  onResume: () -> Unit,
  modifier: Modifier,
) {
  FantasyChoiceButton(
    onClick = onResume,
    enabled = enabled,
    modifier = modifier.heightIn(min = 48.dp).testTag("living_resume"),
    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 4.dp),
  ) {
    Text(
      if (automationEnabled) "Theo dõi đội" else "Tự thám hiểm",
      style = MaterialTheme.typography.labelSmall,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ExploreSiteButton(
  site: ExpeditionSiteKnowledge,
  enabled: Boolean,
  onExplore: (String) -> Unit,
  modifier: Modifier,
) {
  val direction = when (site.id) {
    "north" -> "Lối Bắc"
    "south" -> "Tiếng kêu"
    else -> "Dấu phía Đông"
  }
  val resolved = site.state == ExpeditionSiteState.RESOLVED
  FantasyChoiceButton(
    onClick = { onExplore(site.id) },
    enabled = enabled && !resolved,
    modifier = modifier.heightIn(min = 48.dp).testTag("living_lead_${site.id}"),
    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
  ) {
    Text(
      if (resolved) "✓ $direction" else direction,
      style = MaterialTheme.typography.labelSmall,
      color = if (resolved) ESDesignTokens.success else ESDesignTokens.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
