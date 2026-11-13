package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.SummonerUiState
import com.example.ui.components.FantasySkin
import com.example.ui.components.p28.ESTokens

/**
 * P29 (CP1B): dedicated, USER-OPENED Master management page.
 * The idle Sanctuary HUD only keeps a small master chip (testTag `master_chip`); this
 * screen appears solely on tap. It reads the REAL [com.example.data.model.MasterProfile]
 * from [SummonerUiState] — no fictional stats — and offers shortcuts to the existing
 * nav destinations. A fullscreen scrim is allowed here because this is an explicit modal.
 */
@Composable
fun MasterHubScreen(
  state: SummonerUiState,
  onNavigate: (String) -> Unit,
  onOpenSettings: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  androidx.activity.compose.BackHandler(onBack = onClose)
  val profile = state.profile
  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("master_hub_screen")
      .clickable(enabled = false) {},
  ) {
    // Explicit modal scrim (light crystal veil so the Sanctuary remains visible behind).
    Box(Modifier.fillMaxSize().clickable { onClose() })
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xCC132238),
      border = BorderStroke(1.5.dp, ESTokens.AntiqueGold.copy(alpha = 0.78f)),
      modifier = Modifier
        .align(Alignment.Center)
        .widthIn(max = 520.dp)
        .fillMaxWidth(0.92f)
        .heightIn(max = 640.dp)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Person, contentDescription = null, tint = ESTokens.AntiqueGold, modifier = Modifier.size(28.dp))
            Column {
              Text(
                text = profile?.masterName ?: "Master",
                color = ESTokens.IvoryPaper,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Cấp ${profile?.masterLevel ?: 1} • ${if (state.isGuestAccount) "Khách (offline)" else "Đồng bộ Cloud"} • Đã chinh phục tầng ${profile?.highestClearedFloor ?: 0}",
                color = ESTokens.FrostSilver,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          IconButton(onClick = onClose, modifier = Modifier.size(44.dp).testTag("master_hub_close")) {
            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = ESTokens.IvoryPaper)
          }
        }

        // Real resources from MasterProfile — nothing invented.
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0x94162842),
          border = BorderStroke(1.dp, ESTokens.BronzeMid.copy(alpha = 0.65f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Tài nguyên", color = ESTokens.AntiqueGold, style = MaterialTheme.typography.labelMedium)
            Text("Vàng ${profile?.gold ?: 0L} • Hồn ${profile?.soulShards ?: 0L} • Mana ${profile?.manaCrystals ?: 0L}", color = ESTokens.IvoryPaper, style = MaterialTheme.typography.bodySmall)
            Text("Sắt ${profile?.ironOre ?: 0L} • Tinh chất ${profile?.arcaneEssence ?: 0L} • Lương khô ${profile?.rations ?: 0L}", color = ESTokens.FrostSilver, style = MaterialTheme.typography.bodySmall)
          }
        }

        Text("Điều hướng nhanh", color = ESTokens.AntiqueGold, style = MaterialTheme.typography.labelMedium)
        val shortcuts = listOf(
          Triple("heroes", "Anh hùng", Icons.Default.Groups),
          Triple("team", "Đội hình", Icons.Default.Group),
          Triple("summon", "Triệu hồi", Icons.Default.AutoAwesome),
          Triple("tower", "Tháp", Icons.Default.Castle),
          Triple("inventory", "Túi đồ", Icons.Default.Backpack),
        )
        for ((id, label, icon) in shortcuts) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = FantasySkin.HudGlass,
            border = BorderStroke(1.dp, ESTokens.AntiqueGold.copy(alpha = 0.4f)),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("master_hub_nav_$id")
              .clickable { onNavigate(id) },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(icon, contentDescription = null, tint = ESTokens.AntiqueGold, modifier = Modifier.size(20.dp))
              Text(label, color = ESTokens.IvoryPaper, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
        Spacer(Modifier.height(4.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = FantasySkin.HudGlass,
          border = BorderStroke(1.dp, ESTokens.AntiqueGold.copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("master_hub_nav_settings")
            .clickable { onOpenSettings() },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = ESTokens.AntiqueGold, modifier = Modifier.size(20.dp))
            Text("Cài đặt & tài khoản", color = ESTokens.IvoryPaper, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}
