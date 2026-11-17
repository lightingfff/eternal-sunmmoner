package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.HeroStatusType
import com.example.data.model.MasterProfile
import com.example.ui.SummonerUiState
import com.example.ui.components.ESActionTile
import com.example.ui.components.ESMetricTile
import com.example.ui.components.ESSectionCard
import com.example.ui.components.ESStatePill
import com.example.ui.components.ESUiTone
import com.example.ui.components.FantasyIconButton
import com.example.ui.components.FantasySurface
import com.example.ui.theme.ESDesignTokens

/** One truthful destination; ids intentionally match SanctuaryManagementNav routing. */
private data class MasterShortcut(
  val id: String,
  val title: String,
  val description: String,
  val icon: ImageVector,
)

private data class MasterResource(val id: String, val label: String, val amount: String, val accent: Color)

/**
 * Dedicated, user-opened Master dashboard. All values come from [SummonerUiState];
 * the Master remains an off-map commander and the existing navigation IDs are retained.
 */
@Composable
fun MasterHubScreen(
  state: SummonerUiState,
  onNavigate: (String) -> Unit,
  onOpenSettings: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onClose)
  val profile = state.profile
  val readyHeroes = state.heroes.count { it.statusType == HeroStatusType.READY }
  val activeHeroes = state.heroes.count { it.inActiveSquad }

  Box(modifier = modifier.fillMaxSize().testTag("master_hub_screen")) {
    // Tap outside dismisses; the page itself remains the only foreground interaction layer.
    Box(
      Modifier
        .fillMaxSize()
        .background(ESDesignTokens.scrim)
        .clickable(onClick = onClose),
    )
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 12.dp, vertical = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      val narrow = maxWidth < 400.dp
      val compact = maxHeight < 500.dp
      val shortcuts = listOf(
        MasterShortcut("heroes", "Anh hùng", "Hồ sơ, cấp độ và trạng thái", Icons.Default.Groups),
        MasterShortcut("team", "Đội hình", "Chọn người sẵn sàng xuất trận", Icons.Default.Group),
        MasterShortcut("summon", "Triệu hồi", "Gọi thêm đồng minh", Icons.Default.AutoAwesome),
        MasterShortcut("tower", "Tháp Vĩnh Hằng", "Chọn cổng và tầng kế tiếp", Icons.Default.Castle),
        MasterShortcut("inventory", "Túi đồ", "Trang bị và vật phẩm", Icons.Default.Backpack),
      )
      val resources = listOf(
        MasterResource("gold", "Vàng", (profile?.gold ?: 0L).toString(), ESDesignTokens.gold),
        MasterResource("souls", "Mảnh hồn", (profile?.soulShards ?: 0L).toString(), ESDesignTokens.violet),
        MasterResource("mana", "Mana", (profile?.manaCrystals ?: 0L).toString(), ESDesignTokens.cyan),
        MasterResource("iron", "Quặng sắt", (profile?.ironOre ?: 0L).toString(), ESDesignTokens.textSecondary),
        MasterResource("essence", "Tinh hoa", (profile?.arcaneEssence ?: 0L).toString(), ESDesignTokens.success),
        MasterResource("rations", "Khẩu phần", (profile?.rations ?: 0L).toString(), ESDesignTokens.warning),
      )

      FantasySurface(
        modifier = Modifier
          .align(Alignment.Center)
          .fillMaxWidth()
          .widthIn(max = 620.dp)
          .fillMaxHeight(if (compact) 0.98f else 0.94f)
          .heightIn(max = 780.dp)
          // Keep taps on inert card content from reaching the outside-dismiss scrim.
          .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
          .testTag("master_hub_surface"),
        shape = RoundedCornerShape(22.dp),
        color = ESDesignTokens.panel,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, ESDesignTokens.gold.copy(alpha = 0.86f)),
        frameCornerInset = 22.dp,
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (narrow) 12.dp else 18.dp, vertical = if (compact) 10.dp else 16.dp),
          verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .size(if (compact) 44.dp else 52.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(ESDesignTokens.gold.copy(alpha = 0.14f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = ESDesignTokens.gold,
                modifier = Modifier.size(if (compact) 25.dp else 30.dp),
              )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "BẢNG ĐIỀU KHIỂN • MASTER",
                color = ESDesignTokens.gold,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = profile?.masterName ?: "Master",
                color = ESDesignTokens.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Cấp ${profile?.masterLevel ?: 1} • Đã chinh phục tầng ${profile?.highestClearedFloor ?: 0}",
                color = ESDesignTokens.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            FantasyIconButton(
              onClick = onClose,
              modifier = Modifier.size(48.dp).testTag("master_hub_close"),
            ) {
              Icon(Icons.Default.Close, contentDescription = "Đóng bảng Master", tint = ESDesignTokens.textPrimary)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ESStatePill(
              text = if (state.isGuestAccount) "Hồ sơ khách • lưu cục bộ" else "Tài khoản đã đồng bộ",
              tone = if (state.isGuestAccount) ESUiTone.ACCENT else ESUiTone.SUCCESS,
              modifier = Modifier.testTag("master_account_state"),
            )
            Spacer(Modifier.weight(1f))
            Text(
              text = "${activeHeroes} trong đội • $readyHeroes sẵn sàng",
              color = ESDesignTokens.textMuted,
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          ESSectionCard(
            title = "Tình hình chỉ huy",
            eyebrow = "HỒ SƠ HIỆN TẠI",
            accent = ESDesignTokens.gold,
            padding = 10.dp,
          ) {
            MasterMetricGrid(
              items = listOf(
                MasterResource("level", "Cấp Master", "Lv.${profile?.masterLevel ?: 1}", ESDesignTokens.gold),
                MasterResource("floor", "Tiến trình", "Tầng ${profile?.highestClearedFloor ?: 0}", ESDesignTokens.cyan),
                MasterResource("party", "Đội hình", "$activeHeroes / 4", ESDesignTokens.violet),
              ),
              columns = if (narrow) 1 else 3,
            )
          }

          ESSectionCard(
            title = "Kho tài nguyên",
            eyebrow = "SỐ DƯ ĐANG LƯU",
            subtitle = "Giá trị hiện tại của hồ sơ; không có số liệu ước tính.",
            accent = ESDesignTokens.cyan,
            padding = 10.dp,
          ) {
            MasterMetricGrid(items = resources, columns = if (narrow) 2 else 3)
          }

          ESSectionCard(
            title = "Điểm đến",
            eyebrow = "LỐI TẮT",
            subtitle = "Mở một khu vực hiện có trong Thánh Địa.",
            accent = ESDesignTokens.gold,
            padding = 10.dp,
          ) {
            MasterShortcutGrid(
              items = shortcuts,
              columns = if (narrow) 1 else 2,
              onClick = { onNavigate(it) },
            )
          }

          ESActionTile(
            title = "Cài đặt & tài khoản",
            description = "Hiển thị, hiệu năng và hồ sơ lưu trữ",
            icon = Icons.Default.Settings,
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth().testTag("master_hub_nav_settings"),
            accent = ESDesignTokens.cyan,
          )
        }
      }
    }
  }
}

@Composable
private fun MasterMetricGrid(items: List<MasterResource>, columns: Int) {
  Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
    items.chunked(columns.coerceAtLeast(1)).forEach { rowItems ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        rowItems.forEach { metric ->
          ESMetricTile(
            label = metric.label,
            value = metric.amount,
            accent = metric.accent,
            modifier = Modifier.weight(1f),
            testTag = "master_metric_${metric.id}",
          )
        }
        repeat((columns - rowItems.size).coerceAtLeast(0)) {
          Spacer(Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun MasterShortcutGrid(
  items: List<MasterShortcut>,
  columns: Int,
  onClick: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
    items.chunked(columns.coerceAtLeast(1)).forEach { rowItems ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        rowItems.forEach { shortcut ->
          ESActionTile(
            title = shortcut.title,
            description = shortcut.description,
            icon = shortcut.icon,
            onClick = { onClick(shortcut.id) },
            modifier = Modifier.weight(1f).testTag("master_hub_nav_${shortcut.id}"),
            accent = if (shortcut.id == "tower") ESDesignTokens.gold else ESDesignTokens.cyan,
          )
        }
        repeat((columns - rowItems.size).coerceAtLeast(0)) {
          Spacer(Modifier.weight(1f))
        }
      }
    }
  }
}
