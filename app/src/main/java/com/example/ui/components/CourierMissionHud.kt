package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One small mission card; orders drive the encounter, rather than changing decorative labels. */
@Composable
fun CourierMissionHud(objective:String,status:String,hp:Int,maxHp:Int,progress:Float,
  protecting:Boolean,onProtect:()->Unit,onIntercept:()->Unit,modifier:Modifier=Modifier,
  interceptAvailable:Boolean=true,compact:Boolean=false,intercepting:Boolean = !protecting) {
  Surface(modifier.testTag("courier_mission_hud"),color=Color(0xEB14212A),shape=RoundedCornerShape(10.dp)) {
    if(compact) {
      Row(Modifier.padding(horizontal=6.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),
        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
          Text("Người đưa tin $hp/$maxHp • ${(progress*100).toInt()}% • $objective",
            style=MaterialTheme.typography.labelSmall,color=Color(0xFFE4CE9B),maxLines=1,overflow=TextOverflow.Ellipsis)
          LinearProgressIndicator(progress={ (hp.toFloat()/maxHp.coerceAtLeast(1)).coerceIn(0f,1f) },
            modifier=Modifier.fillMaxWidth().height(2.dp),color=Color(0xFF8AB99D))
          Text(status,style=MaterialTheme.typography.labelSmall,color=Color(0xFFC8D6D8),maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        FilterChip(selected=protecting,onClick=onProtect,label={Text("Bảo vệ")},
          modifier=Modifier.heightIn(min=44.dp).testTag("courier_protect_order"))
        FilterChip(selected=intercepting,onClick=onIntercept,enabled=interceptAvailable,label={Text("Chặn trinh sát")},
          modifier=Modifier.heightIn(min=44.dp).testTag("courier_intercept_order"))
      }
    } else {
    Box {
    Column(Modifier.padding(horizontal=9.dp,vertical=2.dp),verticalArrangement=Arrangement.spacedBy(1.dp)) {
      Text("Người đưa tin • $hp/$maxHp • ${(progress*100).toInt()}% — $objective",
        style=MaterialTheme.typography.labelSmall,color=Color(0xFFE4CE9B),maxLines=1,overflow=TextOverflow.Ellipsis)
      Text(status,style=MaterialTheme.typography.labelSmall,color=Color(0xFFC8D6D8),maxLines=2,overflow=TextOverflow.Ellipsis)
      Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        FilterChip(selected=protecting,onClick=onProtect,label={Text("Bảo vệ người đưa tin",style=MaterialTheme.typography.labelSmall,maxLines=1)},
          modifier=Modifier.weight(1f).heightIn(min=44.dp).testTag("courier_protect_order"))
        FilterChip(selected=intercepting,onClick=onIntercept,enabled=interceptAvailable,label={Text("Chặn trinh sát",style=MaterialTheme.typography.labelSmall,maxLines=1)},
          modifier=Modifier.weight(1f).heightIn(min=44.dp).testTag("courier_intercept_order"))
      }
    }
    LinearProgressIndicator(progress={ (hp.toFloat()/maxHp.coerceAtLeast(1)).coerceIn(0f,1f) },
      modifier=Modifier.align(androidx.compose.ui.Alignment.BottomCenter).fillMaxWidth().height(2.dp),color=Color(0xFF8AB99D))
    }
    }
  }
}
