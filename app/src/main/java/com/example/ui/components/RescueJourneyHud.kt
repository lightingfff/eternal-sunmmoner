package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Commands remain live while the player reads the expedition objective. */
@Composable
fun RescueJourneyHud(
  objective: String,
  status: String,
  completedObjectives: Int,
  totalObjectives: Int,
  courierHp: Int,
  courierMaxHp: Int,
  canInteract: Boolean,
  decisionPending: Boolean,
  onFollow: () -> Unit,
  onInteract: () -> Unit,
  onSilenceAlarm: () -> Unit,
  onRescueScout: () -> Unit,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
  fieldTargetLabel: String? = null,
  fieldHint: String? = null,
  fieldCanInteract: Boolean = false,
  onFieldTravel: () -> Unit = {},
  onFieldInteract: () -> Unit = {},
) {
  var expanded by remember(objective) { mutableStateOf(false) }
  Surface(modifier.testTag("rescue_journey_hud"), color=Color(0xEB14212A), shape=RoundedCornerShape(10.dp)) {
    Column(Modifier.padding(horizontal=8.dp, vertical=if(compact) 0.dp else 3.dp), verticalArrangement=Arrangement.spacedBy(2.dp)) {
      if (compact) {
        // Landscape keeps the live objective and actions in one 48dp strip.
        // Long context is available on demand; choices and field work stay visible below.
        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
          Column(Modifier.weight(1f)) {
            Text("Hành trình ${completedObjectives.coerceAtMost(totalObjectives)}/$totalObjectives • Người đưa tin $courierHp/$courierMaxHp",
              style=MaterialTheme.typography.labelSmall, color=Color(0xFFE4CE9B), maxLines=1, overflow=TextOverflow.Ellipsis)
            Text(objective, style=MaterialTheme.typography.labelMedium, color=Color(0xFFF1DCA8),
              maxLines=1, overflow=TextOverflow.Ellipsis, modifier=Modifier.testTag("rescue_journey_objective"))
          }
          OutlinedButton(onClick=onFollow, modifier=Modifier.heightIn(min=48.dp).testTag("rescue_journey_follow"),
            contentPadding=PaddingValues(horizontal=8.dp,vertical=4.dp)) {
            Text("Hộ tống",style=MaterialTheme.typography.labelSmall)
          }
          Button(onClick=onInteract, enabled=canInteract && !decisionPending,
            modifier=Modifier.heightIn(min=48.dp).testTag("rescue_journey_interact"),
            contentPadding=PaddingValues(horizontal=8.dp,vertical=4.dp)) {
            Text("Khảo sát",style=MaterialTheme.typography.labelSmall)
          }
          TextButton(onClick={expanded=!expanded}, modifier=Modifier.heightIn(min=48.dp).testTag("rescue_journey_details"),
            contentPadding=PaddingValues(horizontal=4.dp,vertical=4.dp)) {
            Text(if(expanded) "Thu gọn" else "Chi tiết",style=MaterialTheme.typography.labelSmall)
          }
        }
        if (expanded) Text(status,style=MaterialTheme.typography.labelSmall,color=Color(0xFFC8D6D8))
      } else {
        Text("Hành trình ${completedObjectives.coerceAtMost(totalObjectives)}/$totalObjectives • Người đưa tin $courierHp/$courierMaxHp",
          style=MaterialTheme.typography.labelSmall, color=Color(0xFFE4CE9B))
        Text(objective, style=MaterialTheme.typography.labelMedium, color=Color(0xFFF1DCA8),
          maxLines=if(expanded) Int.MAX_VALUE else 2, overflow=TextOverflow.Ellipsis,
          modifier=Modifier.testTag("rescue_journey_objective"))
        if (expanded) Text(status, style=MaterialTheme.typography.labelSmall, color=Color(0xFFC8D6D8))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
          OutlinedButton(onClick=onFollow, modifier=Modifier.weight(1f).heightIn(min=44.dp).testTag("rescue_journey_follow"),
            contentPadding=PaddingValues(horizontal=6.dp,vertical=4.dp)) {
            Text("Theo người dẫn đường",style=MaterialTheme.typography.labelSmall)
          }
          Button(onClick=onInteract, enabled=canInteract && !decisionPending,
            modifier=Modifier.weight(1f).heightIn(min=44.dp).testTag("rescue_journey_interact"),
            contentPadding=PaddingValues(horizontal=6.dp,vertical=4.dp)) {
            Text("Khảo sát",style=MaterialTheme.typography.labelSmall)
          }
          TextButton(onClick={expanded=!expanded},
            modifier=Modifier.heightIn(min=44.dp).testTag("rescue_journey_details"),
            contentPadding=PaddingValues(horizontal=4.dp,vertical=4.dp)) {
            Text(if(expanded) "Thu gọn" else "Chi tiết",style=MaterialTheme.typography.labelSmall)
          }
        }
      }
      if (fieldTargetLabel != null) {
        Text(fieldHint.orEmpty(), style=MaterialTheme.typography.labelSmall, color=Color(0xFFC8D6D8),
          maxLines=if(expanded) Int.MAX_VALUE else 2, overflow=TextOverflow.Ellipsis)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
          OutlinedButton(onClick=onFieldTravel,
            modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("rescue_field_travel"),
            contentPadding=PaddingValues(horizontal=6.dp,vertical=4.dp)) {
            Text("Đến dấu mục tiêu",style=MaterialTheme.typography.labelSmall)
          }
          Button(onClick=onFieldInteract,enabled=fieldCanInteract,
            modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("rescue_field_interact"),
            contentPadding=PaddingValues(horizontal=6.dp,vertical=4.dp)) {
            Text(fieldTargetLabel,style=MaterialTheme.typography.labelSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
          }
        }
      }
      if(decisionPending) {
        Text("Chọn cách mở đường rút", style=MaterialTheme.typography.labelSmall, color=Color(0xFFE4CE9B))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
          OutlinedButton(onClick=onSilenceAlarm,enabled=canInteract,
            modifier=Modifier.weight(1f).heightIn(min=44.dp).testTag("rescue_journey_silence"),
            contentPadding=PaddingValues(horizontal=6.dp,vertical=4.dp)) {
            Text("Tắt báo động\nGiảm quân truy đuổi",style=MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(onClick=onRescueScout,enabled=canInteract,
            modifier=Modifier.weight(1f).heightIn(min=44.dp).testTag("rescue_journey_rescue_scout"),
            contentPadding=PaddingValues(horizontal=6.dp,vertical=4.dp)) {
            Text("Cứu trinh sát\nHồi 20% HP đội; thêm cung thủ địch",style=MaterialTheme.typography.labelSmall)
          }
        }
      }
    }
  }
}
