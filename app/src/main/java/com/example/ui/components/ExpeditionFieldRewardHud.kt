package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.game.combat.FieldRewardKind
import com.example.game.combat.FieldRewardOffer
import com.example.game.combat.FieldRewardOption

/** An explicitly opened tray in the existing HUD slot; world and Master orders stay live. */
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
  Surface(modifier.testTag("field_reward_hud"), color = Color(0xF010242A), shape = RoundedCornerShape(10.dp)) {
    if (compact) Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      Column(Modifier.width(104.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Chọn 1 • $pendingCount túi", style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFFFDEA3), maxLines = 1)
        TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp).testTag("field_reward_close"),
          contentPadding = PaddingValues(4.dp)) { Text("Về chiến thuật", maxLines = 1, style = MaterialTheme.typography.labelSmall) }
      }
      offer.options.forEach { option ->
        FieldRewardButton(option, enabled, canClaim(option.id), { onClaim(option.id) }, Modifier.weight(1f), true)
      }
    } else Column(Modifier.padding(horizontal = 7.dp, vertical = 2.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(offer.titleVi, style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFDEA3),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text("Chọn 1 lợi ích • $pendingCount túi chưa dùng", style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF9CD5C6), maxLines = 1)
        }
        TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp).testTag("field_reward_close")) {
          Text("Quay lại", maxLines = 1)
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        offer.options.forEach { option ->
          FieldRewardButton(option, enabled, canClaim(option.id), { onClaim(option.id) }, Modifier.weight(1f), false)
        }
      }
    }
  }
}

@Composable
private fun FieldRewardButton(option: FieldRewardOption, enabled: Boolean, benefitAvailable: Boolean, onClick: () -> Unit,
  modifier: Modifier, compact: Boolean) {
  OutlinedButton(onClick = onClick, enabled = enabled && benefitAvailable,
    modifier = modifier.heightIn(min = if (compact) 76.dp else 94.dp).testTag("field_reward_${option.id}"),
    shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
      ExpeditionRewardIcons.image(option.kind)?.let { image ->
        Image(image, contentDescription = null, modifier = Modifier.size(if (compact) 36.dp else 40.dp),
          filterQuality = FilterQuality.None)
      }
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val title = when (option.kind) {
          FieldRewardKind.READINESS -> "Sẵn sàng kỹ năng"
          FieldRewardKind.WARD -> "Khiên 25% HP"
          FieldRewardKind.HEALING -> "Hồi 25% HP"
          FieldRewardKind.VALUABLES -> "+${option.gold} Vàng • ${option.mana} Mana"
        }
        Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFDEA3),
          maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(when (option.kind) {
          FieldRewardKind.READINESS -> "Giảm hồi bằng 60% hồi tối đa; dùng ngay."
          FieldRewardKind.WARD -> "Khiên tạm thời cho đội còn sống; giữ khiên mạnh hơn."
          FieldRewardKind.HEALING -> "Hồi cho đội còn sống; không hồi sinh."
          FieldRewardKind.VALUABLES -> "Nhận ở tổng kết, kể cả thua/rút; bỏ lợi ích dùng ngay."
        }, style = MaterialTheme.typography.labelSmall, color = Color(0xFFDAE5DE),
          maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (!benefitAvailable) Text("Chưa có người nhận lợi ích", style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFE5BB87), maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
  }
}
