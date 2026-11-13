package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.example.game.combat.ExpeditionSiteState
import com.example.game.combat.ExpeditionWitness
import com.example.game.combat.LivingExpeditionContent
import com.example.game.combat.TowerCombatEngine
import com.example.game.map.ObliqueCamera
import kotlin.math.hypot

/** Existing sprites remain depth-sorted by the battle screen. These are world interaction cues. */
object LivingExpeditionPresentation {
  private fun witnessLabel(npc:ExpeditionWitness):String = when {
    !npc.alive -> "Người gác trạm đã gục"
    npc.rescued -> "Nhân chứng • an toàn"
    else -> "Nhân chứng ${npc.hp}/${npc.maxHp}"
  }

  fun DrawScope.witnessLabelBounds(npc:ExpeditionWitness,camX:Float,camY:Float,zoom:Float,topHudPx:Float,textMeasurer:TextMeasurer):Rect? {
    val x=ObliqueCamera.worldToScreenX(npc.x,camX,zoom,size.width/2f)
    val y=ObliqueCamera.worldToScreenY(npc.y,camY,zoom,size.height/2f)
    if(y !in topHudPx+50f..size.height-85f || x !in 0f..size.width) return null
    val text=textMeasurer.measure(witnessLabel(npc),TextStyle(color=Color(0xFFFFD9A4),fontSize=10.sp,fontWeight=FontWeight.Bold))
    val left=(x-text.size.width/2f).coerceIn(3f,(size.width-text.size.width-3f).coerceAtLeast(3f))
    return Rect(Offset(left-3f,y-112f*zoom),Size(text.size.width+6f,text.size.height+4f))
  }

  fun DrawScope.drawExplorationCues(engine: TowerCombatEngine, camX: Float, camY: Float, zoom: Float,
    topHudPx: Float, textMeasurer: TextMeasurer) {
    val expedition=engine.lostCourier?.livingExpedition ?: return
    fun screen(x:Float,y:Float)=Offset(
      ObliqueCamera.worldToScreenX(x,camX,zoom,size.width/2f),
      ObliqueCamera.worldToScreenY(y,camY,zoom,size.height/2f))
    fun ring(x:Float,y:Float,occupied:Boolean,label:String) {
      val p=screen(x,y)
      if(p.x !in -50f..size.width+50f || p.y !in topHudPx..size.height-85f) return
      val color=if(occupied) Color(0xFF8DE6BC) else Color(0xFFF4D38B)
      val rx=27f*zoom; val ry=rx*ObliqueCamera.OBLIQUE_Y_SCALE
      drawOval(color.copy(alpha=.18f),Offset(p.x-rx,p.y-ry),Size(rx*2,ry*2))
      drawOval(color,Offset(p.x-rx,p.y-ry),Size(rx*2,ry*2),style=Stroke(2f*zoom))
      drawLine(color,Offset(p.x-9*zoom,p.y),Offset(p.x+9*zoom,p.y),2f*zoom)
      val text=textMeasurer.measure(label,TextStyle(color=color,fontSize=10.sp,fontWeight=FontWeight.Bold))
      val left=(p.x-text.size.width/2f).coerceIn(4f,(size.width-text.size.width-4f).coerceAtLeast(4f))
      drawRoundRect(Color(0xE614242A),Offset(left-3f,p.y-ry-20f),Size(text.size.width+6f,text.size.height+3f),CornerRadius(4f,4f))
      drawText(text,topLeft=Offset(left,p.y-ry-20f))
    }
    if(expedition.sites.any { it.id=="north" && it.state!=ExpeditionSiteState.UNKNOWN }) {
      val (x,y)=LivingExpeditionContent.northSwitch
      ring(x,y,expedition.alarmSilenced,if(expedition.alarmSilenced) "Đã cắt tín hiệu" else "Chốt dây tù và")
    }
    if(expedition.sites.any { it.id=="east" && it.state!=ExpeditionSiteState.UNKNOWN }) {
      LivingExpeditionContent.eastPlates.forEachIndexed { index,(x,y) ->
        val standing=engine.heroes.any { !it.knockedOut && hypot(it.x-x,it.y-y)<=70f }
        ring(x,y,standing||expedition.gateSecured,if(expedition.gateSecured) "Ấn đã mở" else "Ấn ${index+1}")
      }
    }
    expedition.witness?.let { npc ->
      witnessLabelBounds(npc,camX,camY,zoom,topHudPx,textMeasurer)?.let { bounds ->
        val text=textMeasurer.measure(witnessLabel(npc),TextStyle(color=Color(0xFFFFD9A4),fontSize=10.sp,fontWeight=FontWeight.Bold))
        drawRoundRect(Color(0xE61A2025),bounds.topLeft,bounds.size,CornerRadius(4f,4f))
        drawText(text,topLeft=bounds.topLeft+Offset(3f,0f))
      }
    }
    val line=expedition.conversations.lastOrNull { it.remainingSeconds>0f && it.speakerHeroId!=null } ?: return
    val hero=engine.heroes.firstOrNull { it.id==line.speakerHeroId && !it.knockedOut } ?: return
    val p=screen(hero.x,hero.y)
    if(p.x !in 0f..size.width || p.y !in topHudPx+130f*zoom..size.height-85f) return
    val width=minOf(250f,size.width-24f).toInt().coerceAtLeast(1)
    val text=textMeasurer.measure("${hero.name}: ${line.textVi}",
      TextStyle(color=Color(0xFFF8F0D8),fontSize=11.sp),maxLines=2,
      constraints=Constraints(maxWidth=width))
    val left=(p.x-text.size.width/2f).coerceIn(6f,(size.width-text.size.width-6f).coerceAtLeast(6f))
    val top=(p.y-122f*zoom-text.size.height).coerceAtLeast(topHudPx+5f)
    drawRoundRect(Color(0xEF162A30),Offset(left-5f,top-4f),Size(text.size.width+10f,text.size.height+8f),CornerRadius(6f,6f))
    drawText(text,topLeft=Offset(left,top))
  }
}
