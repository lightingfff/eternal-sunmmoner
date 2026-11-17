package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ESDesignTokens

/** Encounter commands stay connected to the live courier state; this is presentation only. */
@Composable
fun CourierMissionHud(
  objective: String,
  status: String,
  hp: Int,
  maxHp: Int,
  progress: Float,
  protecting: Boolean,
  onProtect: () -> Unit,
  onIntercept: () -> Unit,
  modifier: Modifier = Modifier,
  interceptAvailable: Boolean = true,
  compact: Boolean = false,
  intercepting: Boolean = !protecting,
  choiceAvailable: Boolean = false,
  onSupport: () -> Unit = {},
  onRecoverSupplies: () -> Unit = {},
  suppliesPending: Boolean = false,
  onGoToSupplies: () -> Unit = {},
  canCollectSupplies: Boolean = false,
  onCollectSupplies: () -> Unit = {},
) {
  val healthFraction = (hp.toFloat() / maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
  val missionFraction = progress.coerceIn(0f, 1f)
  FantasySurface(
    modifier = modifier.testTag("courier_mission_hud"),
    color = ESDesignTokens.panel,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, ESDesignTokens.goldMuted.copy(alpha = 0.72f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "HỘ TỐNG • $hp/$maxHp HP • ${(missionFraction * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = ESDesignTokens.gold,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = objective,
            style = MaterialTheme.typography.labelMedium,
            color = ESDesignTokens.textPrimary,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      LinearProgressIndicator(
        progress = { healthFraction },
        modifier = Modifier.fillMaxWidth().height(if (compact) 3.dp else 4.dp),
        color = if (healthFraction <= 0.25f) ESDesignTokens.danger else ESDesignTokens.success,
        trackColor = ESDesignTokens.panelInset,
      )
      Text(
        text = status,
        style = MaterialTheme.typography.bodySmall,
        color = ESDesignTokens.textSecondary,
        maxLines = if (compact) 1 else 2,
        overflow = TextOverflow.Ellipsis,
      )

      Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        FantasyChoiceButton(
          onClick = onProtect,
          selected = protecting,
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("courier_protect_order"),
          contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
        ) {
          Text(
            if (compact) "Bảo vệ" else "Bảo vệ người đưa tin",
            color = ESDesignTokens.textPrimary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        FantasyChoiceButton(
          onClick = onIntercept,
          enabled = interceptAvailable,
          selected = intercepting,
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("courier_intercept_order"),
          contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
        ) {
          Text(
            "Chặn trinh sát",
            color = if (interceptAvailable) ESDesignTokens.textPrimary else ESDesignTokens.textMuted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      if (choiceAvailable) {
        Text(
          "ƯU TIÊN NHIỆM VỤ • lựa chọn giữ nguyên đến khi kết thúc",
          style = MaterialTheme.typography.labelSmall,
          color = ESDesignTokens.gold,
          fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
          FantasyChoiceButton(
            onClick = onSupport,
            modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("courier_support_choice"),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
          ) {
            Text(
              "Hỗ trợ • +60 HP\n+20 Mana khi cứu",
              style = MaterialTheme.typography.labelSmall,
              color = ESDesignTokens.textPrimary,
              maxLines = 2,
            )
          }
          FantasyChoiceButton(
            onClick = onRecoverSupplies,
            modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("courier_supplies_choice"),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
          ) {
            Text(
              "Tiếp vận • thu hồi kiện\n+60 Vàng khi cứu",
              style = MaterialTheme.typography.labelSmall,
              color = ESDesignTokens.textPrimary,
              maxLines = 2,
            )
          }
        }
      }

      if (suppliesPending) {
        FantasyChoiceButton(
          onClick = onGoToSupplies,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("courier_supply_target"),
        ) {
          Text("Đưa đội tới kiện tiếp vận", style = MaterialTheme.typography.labelMedium)
        }
        FantasyButton(
          onClick = onCollectSupplies,
          enabled = canCollectSupplies,
          modifier = Modifier.fillMaxWidth().testTag("courier_supply_collect"),
        ) {
          Text("Thu hồi tiếp vận", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}
