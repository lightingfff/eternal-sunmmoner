package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.game.combat.ExpeditionSiteState
import com.example.game.combat.ExpeditionDecision
import com.example.game.combat.ExpeditionSiteKnowledge

/** A decision replaces the travel controls, leaving the expedition visible on short phones. */
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
  Surface(modifier.testTag("living_expedition_hud"), color=Color(0xEC10242A), shape=RoundedCornerShape(10.dp)) {
    if (compact && decision==null) {
      Row(Modifier.padding(horizontal=7.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(objective,modifier=Modifier.testTag("living_expedition_objective"),style=MaterialTheme.typography.labelMedium,
            color=Color(0xFFEBDFC4),maxLines=1,overflow=TextOverflow.Ellipsis)
          Text(if(automationEnabled) "Đội tự thám hiểm" else "Đang chờ lệnh",style=MaterialTheme.typography.labelSmall,color=Color(0xFF9CD5C6),maxLines=1)
        }
        OutlinedButton(onClick=onResume,enabled=enabled,
          modifier=Modifier.width(92.dp).heightIn(min=48.dp).testTag("living_resume"),
          contentPadding=PaddingValues(horizontal=4.dp,vertical=2.dp)) {
          Text(if(automationEnabled) "Theo dõi đội" else "Tự thám hiểm",style=MaterialTheme.typography.labelSmall,maxLines=1)
        }
        sites.forEach { site ->
          OutlinedButton(onClick={onExplore(site.id)},enabled=enabled && canRedirect && site.state!=ExpeditionSiteState.RESOLVED,
            modifier=Modifier.width(88.dp).heightIn(min=48.dp).testTag("living_lead_${site.id}"),
            contentPadding=PaddingValues(horizontal=3.dp,vertical=2.dp)) {
            val direction=when(site.id) { "north"->"Lối Bắc";"south"->"Tiếng kêu";else->"Dấu phía Đông" }
            Text(if(site.state==ExpeditionSiteState.RESOLVED) "✓ $direction" else direction,
              style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
          }
        }
      }
    } else Column(Modifier.padding(horizontal=7.dp,vertical=3.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
      if (decision != null) {
        Text(decision.promptVi, modifier=Modifier.testTag("living_expedition_objective"),
          style=MaterialTheme.typography.labelMedium,color=Color(0xFFFFDE9C),maxLines=1,overflow=TextOverflow.Ellipsis)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
          decision.options.forEach { option ->
            OutlinedButton(onClick={onChoose(option.id)},enabled=enabled && option.enabled,
              modifier=Modifier.weight(1f).heightIn(min=64.dp).testTag("living_decision_${option.id}"),
              contentPadding=PaddingValues(horizontal=7.dp,vertical=3.dp)) {
              Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(option.labelVi,style=MaterialTheme.typography.labelMedium,color=Color(0xFFFFDEA3),
                  maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(option.consequenceVi,style=MaterialTheme.typography.labelSmall,color=Color(0xFFDAE5DE),
                  maxLines=2,overflow=TextOverflow.Ellipsis)
              }
            }
          }
        }
      } else {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
          Text(objective,modifier=Modifier.weight(1f).testTag("living_expedition_objective"),
            style=MaterialTheme.typography.labelMedium,color=Color(0xFFEBDFC4),maxLines=1,overflow=TextOverflow.Ellipsis)
          Text(if(automationEnabled) "Đội tự thám hiểm" else "Đang chờ lệnh",
            style=MaterialTheme.typography.labelSmall,color=Color(0xFF9CD5C6),maxLines=1)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
          OutlinedButton(onClick=onResume,enabled=enabled,
            modifier=Modifier.weight(1.15f).heightIn(min=48.dp).testTag("living_resume"),
            contentPadding=PaddingValues(horizontal=4.dp,vertical=2.dp)) {
            Text(if(automationEnabled) "Theo dõi đội" else "Tự thám hiểm",style=MaterialTheme.typography.labelSmall,maxLines=1)
          }
          sites.forEach { site ->
            OutlinedButton(onClick={onExplore(site.id)},enabled=enabled && canRedirect && site.state!=ExpeditionSiteState.RESOLVED,
              modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("living_lead_${site.id}"),
              contentPadding=PaddingValues(horizontal=3.dp,vertical=2.dp)) {
              val direction=when(site.id) { "north"->"Lối Bắc";"south"->"Tiếng kêu";else->"Dấu phía Đông" }
              Text(if(site.state==ExpeditionSiteState.RESOLVED) "✓ $direction" else direction,
                style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
          }
        }
      }
    }
  }
}
