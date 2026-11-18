package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.EquipmentRecord
import com.example.data.model.HeroRecord
import com.example.ui.components.p28.ESHeroCard
import com.example.ui.theme.ESDesignTokens

private val Gold = FantasySkin.Gold

/** Existing translucent Sanctuary glass; retained for the floating management dock. */
val PanelGlass = Color(0xE0121B2E)

@Composable
fun FantasyPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  FantasySurface(
    modifier = with(FantasySkin) { modifier.materialEdge() },
    shape = RoundedCornerShape(14.dp),
    color = PanelGlass,
    border = BorderStroke(1.dp, Gold.copy(alpha = 0.55f)),
    content = content,
  )
}

@Composable
fun FantasyAction(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  selected: Boolean? = null,
) {
  FantasyChoiceButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    selected = selected,
    colors = ButtonDefaults.outlinedButtonColors(
      contentColor = Gold,
      disabledContentColor = ESDesignTokens.textMuted,
    ),
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
  ) {
    Text(
      text,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      style = MaterialTheme.typography.labelMedium,
    )
  }
}

/** Compact icon dock that expands into the same five established destinations. */
@Composable
fun SanctuaryManagementNav(onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
  var expanded by remember { mutableStateOf(false) }
  FantasySurface(
    modifier = modifier.testTag("management_navigation"),
    shape = RoundedCornerShape(16.dp),
    color = FantasySkin.HudGlass,
    border = BorderStroke(1.dp, Gold.copy(alpha = 0.58f)),
    frameCornerInset = 10.dp,
    compactFrame = true,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 5.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val destinations = listOf(
        Triple("heroes", "Anh hùng", Icons.Default.Groups),
        Triple("summon", "Triệu hồi", Icons.Default.AutoAwesome),
        Triple("team", "Đội hình", Icons.Default.Group),
        Triple("tower", "Tháp", Icons.Default.Castle),
        Triple("inventory", "Túi đồ", Icons.Default.Backpack),
      )
      destinations.forEach { (id, title, icon) ->
        FantasyTextButton(
          onClick = { onNavigate(id) },
          modifier = Modifier.weight(1f).heightIn(min = ESDesignTokens.minTouchTarget).testTag("nav_$id"),
          contentPadding = PaddingValues(horizontal = 3.dp, vertical = 4.dp),
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
          ) {
            Icon(
              icon,
              contentDescription = if (expanded) null else title,
              tint = Gold,
              modifier = Modifier.size(18.dp),
            )
            if (expanded) {
              Text(
                title,
                color = ESDesignTokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
              )
            }
          }
        }
      }
      FantasyIconButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.size(48.dp).testTag("nav_expand_toggle"),
      ) {
        Icon(
          if (expanded) Icons.Default.Close else Icons.Default.MoreVert,
          contentDescription = if (expanded) "Thu gọn tên mục" else "Hiện tên mục",
          tint = Gold,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
fun HeroManagementDialog(
  heroes: List<HeroRecord>,
  teamMode: Boolean,
  busy: Boolean,
  onClose: () -> Unit,
  onInspect: (HeroRecord) -> Unit,
  onToggle: (HeroRecord) -> Unit,
  equipment: List<EquipmentRecord> = emptyList(),
) {
  var filter by remember { mutableStateOf("all") }
  val dialogHeight = minOf(600.dp, (LocalConfiguration.current.screenHeightDp * 0.88f).dp)
  val visibleHeroes = heroes.filter { hero ->
    when (filter) {
      "team" -> hero.inActiveSquad
      "ready" -> !hero.isCurrentlyInjured()
      else -> true
    }
  }

  Dialog(onDismissRequest = onClose) {
    FantasyPanel(
      Modifier
        .fillMaxWidth()
        .widthIn(max = 720.dp)
        .height(dialogHeight)
        .testTag("hero_management_dialog"),
    ) {
      Column(
        Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              if (teamMode) "Đội hình • ${heroes.count { it.inActiveSquad }} đã chọn" else "Anh hùng • ${heroes.size}",
              color = Gold,
              style = MaterialTheme.typography.titleLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              if (teamMode) "Chọn anh hùng khỏe mạnh trước khi xuất chinh." else "Mở hồ sơ để luyện tập, nâng kỹ năng và quản lý trang bị.",
              color = ESDesignTokens.textSecondary,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
          FantasyIconButton(onClick = onClose, modifier = Modifier.size(48.dp).testTag("close_hero_management")) {
            Icon(Icons.Default.Close, contentDescription = "Đóng danh sách", tint = ESDesignTokens.textPrimary)
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("all" to "Tất cả", "team" to "Trong đội", "ready" to "Sẵn sàng").forEach { (id, label) ->
            FantasyTextButton(
              onClick = { filter = id },
              selected = filter == id,
              modifier = Modifier.testTag("hero_filter_$id"),
              contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
            ) {
              Text(
                label,
                color = if (filter == id) Gold else ESDesignTokens.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
              )
            }
          }
        }

        if (visibleHeroes.isEmpty()) {
          Text(
            if (heroes.isEmpty()) "Chưa có anh hùng. Mở Triệu hồi để bắt đầu." else "Không có anh hùng phù hợp với bộ lọc.",
            color = ESDesignTokens.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
          )
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
          ) {
            items(visibleHeroes, key = { it.id }) { hero ->
              val injured = hero.isCurrentlyInjured()
              FantasyPanel(Modifier.fillMaxWidth().testTag("hero_management_${hero.id}")) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    ESHeroCard(
                      hero = hero,
                      selected = hero.inActiveSquad,
                      onClick = { onInspect(hero) },
                      modifier = Modifier.width(76.dp).testTag("hero_card_${hero.id}"),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                      Text(
                        "${hero.name} • Lv.${hero.level}",
                        color = ESDesignTokens.textPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        "${"★".repeat(hero.rarity.toInt().coerceIn(1, 5))} • ${hero.classType.labelVi}",
                        color = Gold,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        "LC ${hero.totalAttack(equipment) * 3 + hero.totalDefense(equipment) * 2 + hero.totalMaxHp(equipment) / 4}",
                        color = ESDesignTokens.cyan,
                        style = MaterialTheme.typography.labelSmall,
                      )
                      Text(
                        "HP ${hero.totalMaxHp(equipment)} • ATK ${hero.totalAttack(equipment)} • DEF ${hero.totalDefense(equipment)}",
                        color = ESDesignTokens.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      ESStatePill(
                        text = when {
                          injured -> "Đang dưỡng thương"
                          hero.inActiveSquad -> "Trong đội hình"
                          else -> "Sẵn sàng"
                        },
                        tone = when {
                          injured -> ESUiTone.WARNING
                          hero.inActiveSquad -> ESUiTone.SUCCESS
                          else -> ESUiTone.NEUTRAL
                        },
                      )
                    }
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FantasyAction("Mở hồ sơ", { onInspect(hero) }, Modifier.weight(1f).testTag("inspect_${hero.id}"))
                    if (teamMode) {
                      FantasyAction(
                        text = if (hero.inActiveSquad) "Rời đội" else if (injured) "Đang điều trị" else "Vào đội",
                        onClick = { onToggle(hero) },
                        modifier = Modifier.weight(1f).testTag("assign_${hero.id}"),
                        enabled = !busy && (!injured || hero.inActiveSquad),
                        selected = hero.inActiveSquad,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
