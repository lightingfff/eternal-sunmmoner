package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.game.combat.FieldRewardKind
import com.example.game.combat.FieldRewardOffer
import com.example.game.combat.FieldRewardOption
import com.example.ui.theme.ESDesignTokens

/** Explicitly opened reward tray; the battlefield and Master orders remain active. */
@Composable
fun ExpeditionFieldRewardHud(
  offer: FieldRewardOffer,
  pendingCount: Int,
  enabled: Boolean,
  canClaim: (String) -> Boolean,
  onClaim: (String) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
) {
  FantasySurface(
    modifier = modifier.testTag("field_reward_hud"),
    color = ESDesignTokens.panel,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, ESDesignTokens.cyan.copy(alpha = 0.58f)),
  ) {
    if (compact) {
      Row(
        Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        Column(Modifier.width(112.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            "CHIẾN LỢI PHẨM • $pendingCount",
            style = MaterialTheme.typography.labelSmall,
            color = ESDesignTokens.gold,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
          FantasyTextButton(
            onClick = onClose,
            modifier = Modifier.testTag("field_reward_close"),
            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 4.dp),
          ) {
            Text("Về chiến thuật", maxLines = 1, style = MaterialTheme.typography.labelSmall)
          }
        }
        offer.options.forEach { option ->
          FieldRewardButton(
            option = option,
            enabled = enabled,
            benefitAvailable = canClaim(option.id),
            onClick = { onClaim(option.id) },
            modifier = Modifier.weight(1f),
            compact = true,
          )
        }
      }
    } else {
      Column(
        Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              offer.titleVi,
              style = MaterialTheme.typography.titleSmall,
              color = ESDesignTokens.gold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              "Chọn 1 lợi ích • $pendingCount túi chưa dùng",
              style = MaterialTheme.typography.labelSmall,
              color = ESDesignTokens.success,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          FantasyTextButton(
            onClick = onClose,
            modifier = Modifier.testTag("field_reward_close"),
          ) { Text("Quay lại", maxLines = 1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
          offer.options.forEach { option ->
            FieldRewardButton(
              option = option,
              enabled = enabled,
              benefitAvailable = canClaim(option.id),
              onClick = { onClaim(option.id) },
              modifier = Modifier.weight(1f),
              compact = false,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FieldRewardButton(
  option: FieldRewardOption,
  enabled: Boolean,
  benefitAvailable: Boolean,
  onClick: () -> Unit,
  modifier: Modifier,
  compact: Boolean,
) {
  FantasyChoiceButton(
    onClick = onClick,
    enabled = enabled && benefitAvailable,
    modifier = modifier
      .heightIn(min = if (compact) 76.dp else 94.dp)
      .testTag("field_reward_${option.id}"),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, ESDesignTokens.goldMuted.copy(alpha = 0.48f)),
    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      ExpeditionRewardIcons.image(option.kind)?.let { image ->
        Image(
          image,
          contentDescription = null,
          modifier = Modifier.size(if (compact) 36.dp else 40.dp),
          filterQuality = FilterQuality.None,
        )
      }
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        val title = when (option.kind) {
          FieldRewardKind.READINESS -> "Sẵn sàng kỹ năng"
          FieldRewardKind.WARD -> "Khiên 25% HP"
          FieldRewardKind.HEALING -> "Hồi 25% HP"
          FieldRewardKind.VALUABLES -> "+${option.gold} Vàng • ${option.mana} Mana"
        }
        Text(
          title,
          style = MaterialTheme.typography.labelMedium,
          color = ESDesignTokens.gold,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          when (option.kind) {
            FieldRewardKind.READINESS -> "Giảm hồi bằng 60% hồi tối đa; dùng ngay."
            FieldRewardKind.WARD -> "Khiên tạm thời cho đội còn sống; giữ khiên mạnh hơn."
            FieldRewardKind.HEALING -> "Hồi cho đội còn sống; không hồi sinh."
            FieldRewardKind.VALUABLES -> "Nhận ở tổng kết, kể cả thua/rút; bỏ lợi ích dùng ngay."
          },
          style = MaterialTheme.typography.labelSmall,
          color = ESDesignTokens.textSecondary,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
        if (!benefitAvailable) {
          Text(
            "Chưa có người nhận lợi ích",
            style = MaterialTheme.typography.labelSmall,
            color = ESDesignTokens.warning,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
