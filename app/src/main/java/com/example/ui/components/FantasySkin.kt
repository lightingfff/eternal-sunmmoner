package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/** Shared controls across management, inventory, tower preparation and settlement. */
object FantasySkin {
  // P26: dark glass panel instead of an opaque slab, so the world reads through.
  // P27B-V5 CP5: 0xB5 (71%) was measured against the real ivory world and failed the
  // HUD legibility floor — over the brightest gate facade the standard gold HUD label
  // dropped to 4.0:1 and the muted 0x9EA6B2 labels to 2.9:1. Raised to 88%: the world
  // still reads through the glass (12% shows) but gold is 6.9:1 and muted 5.0:1 in the
  // worst case, i.e. legible over ANY pixel the lobby can put behind it.
  // Enforced by SanctuaryHudContrastTest.
  val Panel=Color(0xE0141D2A)
  // P29: permanent HUD chrome must let the world read through — 70% glass, used only
  // for small idle chips/docks (modals keep the denser Panel). Contrast enforced by tests.
  val HudGlass=Color(0xB3141D2A)
  val Gold=Color(0xffc6b27f)
  @Composable fun buttonColors(containerColor: Color=Panel,contentColor: Color=Gold,
    disabledContainerColor: Color=Color(0xff242a30),disabledContentColor: Color=Color(0xff80868d)): ButtonColors {
    return ButtonDefaults.buttonColors(
      containerColor=lerp(Panel,containerColor,.16f),
      contentColor=if(contentColor.red+contentColor.green+contentColor.blue<1f) Gold else contentColor,
      disabledContainerColor=disabledContainerColor,disabledContentColor=disabledContentColor)
  }
}
@Composable fun FantasyButton(onClick: ()->Unit,modifier: Modifier=Modifier,enabled: Boolean=true,
  shape: Shape=RoundedCornerShape(8.dp),colors: ButtonColors=FantasySkin.buttonColors(),
  elevation: ButtonElevation?=null,border: BorderStroke?=BorderStroke(1.dp,FantasySkin.Gold.copy(alpha=.5f)),
  contentPadding: PaddingValues=PaddingValues(horizontal=14.dp,vertical=9.dp),
  interactionSource: MutableInteractionSource?=null,content: @Composable RowScope.()->Unit) {
  Button(onClick,modifier.heightIn(min=48.dp),enabled,shape,colors,elevation,border,contentPadding,interactionSource,content)
}
@Composable fun RewardSummary(gold: Long,mana: Long,shards: Long,ore: Long,essence: Long) {
  Column(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
    Text("CHIẾN LỢI PHẨM ĐÃ LƯU",color=FantasySkin.Gold,style=MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
      for((label,value) in listOf("Vàng" to gold,"Mana" to mana,"Mảnh hồn" to shards)) {
        FantasyPanel(Modifier.weight(1f)) {
          Column(Modifier.padding(10.dp)) {
            Text("+$value",color=Color(0xfff1e5c9),style=MaterialTheme.typography.titleMedium)
            Text(label,color=Color(0xffaebcc9),style=MaterialTheme.typography.labelSmall)
          }
        }
      }
    }
    Text("+$ore Quặng sắt  •  +$essence Tinh hoa",color=Color(0xffb7c4bd),style=MaterialTheme.typography.bodySmall)
  }
}
