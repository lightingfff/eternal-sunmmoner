package com.example.ui.components

import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.HeroRecord
import com.example.ui.components.p28.ESHeroCard

private val Gold=Color(0xffbda46d)
/** P26: dark glass instead of opaque ink — the world stays visible behind panels.
 *  P27B-V5 CP5: 0xB8 (72%) failed the same measurement as [FantasySkin.Panel]; raised to
 *  88% so panel text keeps >= 4.5:1 over the brightest terrain. */
val PanelGlass=Color(0xE0121B2E)
@Composable fun FantasyPanel(modifier: Modifier=Modifier,content: @Composable ()->Unit) {
  Surface(modifier,shape=RoundedCornerShape(12.dp),color=PanelGlass,border=BorderStroke(1.dp,Gold.copy(alpha=.55f)),content=content)
}
@Composable fun FantasyAction(text: String,onClick: ()->Unit,modifier: Modifier=Modifier,enabled: Boolean=true) {
  OutlinedButton(onClick,modifier.heightIn(min=48.dp),enabled=enabled,shape=RoundedCornerShape(8.dp),
    colors=ButtonDefaults.outlinedButtonColors(contentColor=Gold),border=BorderStroke(1.dp,Gold.copy(alpha=.45f)),
    contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)) {
    Text(text,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelMedium)
  }
}
@Composable fun SanctuaryManagementNav(onNavigate: (String)->Unit,modifier: Modifier=Modifier) {
  // P29: idle bottom bar is a compact ICON DOCK (labels hidden until the player expands it),
  // on translucent HudGlass so the world stays visible. All nav_<id> tags preserved.
  var expanded by remember { mutableStateOf(false) }
  Surface(modifier.testTag("management_navigation"),shape=RoundedCornerShape(12.dp),
    color=FantasySkin.HudGlass,border=BorderStroke(1.dp,Gold.copy(alpha=.55f))) {
    Row(Modifier.padding(horizontal=4.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
      for((id,title,icon) in listOf(
        Triple("heroes","Anh hùng",Icons.Default.Groups),
        Triple("summon","Triệu hồi",Icons.Default.AutoAwesome),
        Triple("team","Đội hình",Icons.Default.Group),
        Triple("tower","Tháp",Icons.Default.Castle),
        Triple("inventory","Túi đồ",Icons.Default.Backpack),
      )) {
        TextButton({onNavigate(id)},modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("nav_$id"),contentPadding=PaddingValues(2.dp)) {
          Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            Icon(icon,contentDescription=null,tint=Gold,modifier=Modifier.size(18.dp))
            if (expanded) Text(title,color=Gold,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)
          }
        }
      }
      IconButton({expanded=!expanded},modifier=Modifier.size(40.dp).testTag("nav_expand_toggle")) {
        Icon(if(expanded) Icons.Default.Close else Icons.Default.MoreVert,contentDescription="Mở/thu gọn nhãn",tint=Gold,modifier=Modifier.size(18.dp))
      }
    }
  }
}
@Composable fun HeroManagementDialog(heroes: List<HeroRecord>,teamMode: Boolean,busy: Boolean,
  onClose: ()->Unit,onInspect: (HeroRecord)->Unit,onToggle: (HeroRecord)->Unit,equipment: List<com.example.data.model.EquipmentRecord> = emptyList()) {
  var query by remember { mutableStateOf("") }
  val dialogHeight=minOf(540.dp,(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp*.88f).dp)
  Dialog(onDismissRequest=onClose) {
    FantasyPanel(Modifier.fillMaxWidth().height(dialogHeight).testTag("hero_management_dialog")) {
      Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
          Text(if(teamMode) "Đội hình • ${heroes.count { it.inActiveSquad }} đã chọn" else "Anh hùng • ${heroes.size}",modifier=Modifier.weight(1f),color=Gold,style=MaterialTheme.typography.titleMedium)
          TextButton(onClose,Modifier.testTag("close_hero_management")) {Text("Đóng",color=Gold)}
        }
        Text(if(teamMode) "Chọn anh hùng sẵn sàng, rồi vào Tháp để chuẩn bị xuất trận." else "Xem hồ sơ để luyện tập, nâng kỹ năng và quản lý trang bị.",color=Color(0xffc6cbd0),style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
          for((id,label) in listOf("" to "Tất cả","team" to "Trong đội","ready" to "Sẵn sàng")) {
            TextButton(onClick={query=id}) { Text(label,color=if(query==id) Gold else Color(0xffaeb9c4),style=MaterialTheme.typography.labelSmall) }
          }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier=Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
          items(heroes.filter { query.isBlank() || (query=="team" && it.inActiveSquad) || (query=="ready" && !it.isCurrentlyInjured()) },key={it.id}) { hero ->
            FantasyPanel(Modifier.fillMaxWidth()) {
              Column(Modifier.padding(10.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                  ESHeroCard(hero=hero,selected=hero.inActiveSquad,onClick={onInspect(hero)},modifier=Modifier.width(76.dp).testTag("hero_card_${hero.id}"))
                  Column(Modifier.weight(1f)) {
                    Text("${hero.name} • Lv.${hero.level}",color=Color(0xffece5d5),maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("${"★".repeat(hero.rarity.toInt().coerceIn(1,6))} • ${hero.classType.labelVi}",color=Gold,style=MaterialTheme.typography.labelSmall)
                    Text("LC ${hero.totalAttack(equipment)*3+hero.totalDefense(equipment)*2+hero.totalMaxHp(equipment)/4} • ${if(hero.isCurrentlyInjured()) "Dưỡng thương" else if(hero.inActiveSquad) "Đang trong đội" else "Sẵn sàng"}",color=Gold,style=MaterialTheme.typography.labelSmall)
                    Text("HP ${hero.maxHp} • ATK ${hero.attack} • DEF ${hero.defense}",color=Color(0xffb7c2ce),style=MaterialTheme.typography.labelSmall)
                  }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                  FantasyAction("Hồ sơ",{onInspect(hero)},Modifier.weight(1f).testTag("inspect_${hero.id}"))
                  if(teamMode) FantasyAction(if(hero.inActiveSquad) "Rời đội" else if(hero.isCurrentlyInjured()) "Đang dưỡng thương" else "Vào đội",
                    {onToggle(hero)},Modifier.weight(1f).testTag("assign_${hero.id}"),!busy && (!hero.isCurrentlyInjured() || hero.inActiveSquad))
                }
              }
            }
          }
          if(heroes.isEmpty()) item {Text("Chưa có anh hùng. Mở Triệu hồi để bắt đầu.",color=Gold)}
        }
      }
    }
  }
}
