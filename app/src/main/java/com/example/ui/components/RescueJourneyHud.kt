package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ESDesignTokens

/** Live rescue objective and actions; presentation never changes the journey state. */
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
  medicineDecision: Boolean = false,
) {
  var expanded by remember(objective) { mutableStateOf(false) }
  val total = totalObjectives.coerceAtLeast(1)
  val fraction = (completedObjectives.toFloat() / total).coerceIn(0f, 1f)

  FantasySurface(
    modifier = modifier.testTag("rescue_journey_hud"),
    color = ESDesignTokens.panel,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, ESDesignTokens.goldMuted.copy(alpha = 0.72f)),
  ) {
    Column(
      Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = if (compact) 5.dp else 9.dp),
      verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 7.dp),
    ) {
      if (compact) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              "HÀNH TRÌNH ${completedObjectives.coerceIn(0, total)}/$total • NGƯỜI ĐƯA TIN $courierHp/$courierMaxHp",
              style = MaterialTheme.typography.labelSmall,
              color = ESDesignTokens.gold,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              objective,
              style = MaterialTheme.typography.labelMedium,
              color = ESDesignTokens.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("rescue_journey_objective"),
            )
          }
          FantasyChoiceButton(
            onClick = onFollow,
            modifier = Modifier.heightIn(min = 48.dp).testTag("rescue_journey_follow"),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          ) { Text("Theo dấu", style = MaterialTheme.typography.labelSmall) }
          FantasyButton(
            onClick = onInteract,
            enabled = canInteract && !decisionPending,
            modifier = Modifier.heightIn(min = 48.dp).testTag("rescue_journey_interact"),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          ) { Text("Khảo sát", style = MaterialTheme.typography.labelSmall) }
          FantasyTextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.heightIn(min = 48.dp).testTag("rescue_journey_details"),
            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 4.dp),
          ) { Text(if (expanded) "Thu gọn" else "Chi tiết", style = MaterialTheme.typography.labelSmall) }
        }
        if (expanded) {
          Text(status, style = MaterialTheme.typography.bodySmall, color = ESDesignTokens.textSecondary)
        }
      } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              "HÀNH TRÌNH • ${completedObjectives.coerceIn(0, total)}/$total",
              style = MaterialTheme.typography.labelSmall,
              color = ESDesignTokens.gold,
              fontWeight = FontWeight.Bold,
            )
            Text(
              "Người đưa tin • $courierHp/$courierMaxHp HP",
              style = MaterialTheme.typography.labelSmall,
              color = ESDesignTokens.textSecondary,
            )
          }
          FantasyTextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.heightIn(min = 48.dp).testTag("rescue_journey_details"),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp),
          ) { Text(if (expanded) "Thu gọn" else "Chi tiết", style = MaterialTheme.typography.labelSmall) }
        }
        LinearProgressIndicator(
          progress = { fraction },
          modifier = Modifier.fillMaxWidth().height(3.dp),
          color = ESDesignTokens.gold,
          trackColor = ESDesignTokens.panelInset,
        )
        Text(
          objective,
          style = MaterialTheme.typography.bodyMedium,
          color = ESDesignTokens.textPrimary,
          maxLines = if (expanded) Int.MAX_VALUE else 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("rescue_journey_objective"),
        )
        if (expanded) Text(status, style = MaterialTheme.typography.bodySmall, color = ESDesignTokens.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FantasyChoiceButton(
            onClick = onFollow,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("rescue_journey_follow"),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp),
          ) { Text("Theo dấu mục tiêu", style = MaterialTheme.typography.labelSmall) }
          FantasyButton(
            onClick = onInteract,
            enabled = canInteract && !decisionPending,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("rescue_journey_interact"),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp),
          ) { Text("Khảo sát", style = MaterialTheme.typography.labelSmall) }
        }
      }

      if (fieldTargetLabel != null) {
        Text(
          fieldHint.orEmpty(),
          style = MaterialTheme.typography.labelSmall,
          color = ESDesignTokens.textSecondary,
          maxLines = if (expanded) 3 else 2,
          overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FantasyChoiceButton(
            onClick = onFieldTravel,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("rescue_field_travel"),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp),
          ) { Text("Đến dấu mục tiêu", style = MaterialTheme.typography.labelSmall) }
          FantasyButton(
            onClick = onFieldInteract,
            enabled = fieldCanInteract,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("rescue_field_interact"),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp),
          ) {
            Text(fieldTargetLabel, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
          }
        }
      }

      if (decisionPending) {
        Text(
          if (medicineDecision) "PHÂN PHỐI TÚI THUỐC DUY NHẤT" else "XỬ LÝ TÍN HIỆU TRÊN ĐƯỜNG",
          style = MaterialTheme.typography.labelSmall,
          color = ESDesignTokens.warning,
          fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FantasyChoiceButton(
            onClick = onSilenceAlarm,
            enabled = canInteract,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("rescue_journey_silence"),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
          ) {
            Text(
              if (medicineDecision) "Băng bó người đưa tin\nChặn 60 sát thương" else "Tắt báo động\nBớt một xạ thủ ở điểm giao",
              style = MaterialTheme.typography.labelSmall,
              color = ESDesignTokens.textPrimary,
              maxLines = 3,
            )
          }
          FantasyChoiceButton(
            onClick = onRescueScout,
            enabled = canInteract,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("rescue_journey_rescue_scout"),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
          ) {
            Text(
              if (medicineDecision) "Chữa đội hộ tống\nHồi 35% HP người còn đứng vững" else "Giữ túi băng dự phòng\nHồi 20% HP đội; còn báo động",
              style = MaterialTheme.typography.labelSmall,
              color = ESDesignTokens.textPrimary,
              maxLines = 3,
            )
          }
        }
      }
    }
  }
}
