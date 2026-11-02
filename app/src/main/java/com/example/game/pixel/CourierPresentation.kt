package com.example.game.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.*
import com.example.game.map.ObliqueCamera
import com.example.data.model.HeroClassType
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite

/** Reuses a project-owned priest sprite for the courier; no new premium artwork is claimed. */
object CourierPresentation {
  private val flag=Path().apply { moveTo(0f,0f);lineTo(14f,4f);lineTo(0f,10f);close() }
  fun DrawScope.drawCourier(x:Float,y:Float,camX:Float,camY:Float,zoom:Float,
    moving:Boolean,right:Boolean,hurt:Float,hp:Int,maxHp:Int,time:Float) {
    drawCharacterSprite(x,y,camX,camY,zoom,false,HeroClassType.PRIEST,right,moving,0f,hurt,
      hp.toFloat()/maxHp.coerceAtLeast(1),null,false,time)
    drawScoutAlert(x,y-8f,camX,camY,zoom,Color(0xFFD0C39A))
  }
  fun DrawScope.drawScoutAlert(x:Float,y:Float,camX:Float,camY:Float,zoom:Float,color:Color=Color(0xFFEBC47A)) {
    val sx=ObliqueCamera.worldToScreenX(x,camX,zoom,size.width/2f)
    val sy=ObliqueCamera.worldToScreenY(y,camY,zoom,size.height/2f)-100f*zoom
    if(sx < -25f || sx>size.width+25f || sy < -25f || sy>size.height+25f) return
    withTransform({translate(sx,sy);scale(zoom,zoom,Offset.Zero)}) {
      drawLine(color,Offset.Zero,Offset(0f,18f),2f);drawPath(flag,color)
    }
  }
  fun DrawScope.drawEliteTelegraph(x:Float,y:Float,radius:Float,fraction:Float,camX:Float,camY:Float,zoom:Float) {
    val sx=ObliqueCamera.worldToScreenX(x,camX,zoom,size.width/2f)
    val sy=ObliqueCamera.worldToScreenY(y,camY,zoom,size.height/2f)
    val r=radius*zoom;val ry=r*ObliqueCamera.OBLIQUE_Y_SCALE
    if(sx+r<0 || sx-r>size.width || sy+ry<0 || sy-ry>size.height) return
    val alpha=.08f+(1f-fraction.coerceIn(0f,1f))*.14f
    drawOval(Color(0xFFE4A16D).copy(alpha=alpha),Offset(sx-r,sy-ry),Size(r*2,ry*2))
    drawOval(Color(0xFFFFB084),Offset(sx-r,sy-ry),Size(r*2,ry*2),style=Stroke(2.5f*zoom))
    drawLine(Color(0xFFFFD19A),Offset(sx-8*zoom,sy),Offset(sx+8*zoom,sy),2*zoom)
    drawLine(Color(0xFFFFD19A),Offset(sx,sy-6*zoom),Offset(sx,sy+6*zoom),2*zoom)
  }
}
