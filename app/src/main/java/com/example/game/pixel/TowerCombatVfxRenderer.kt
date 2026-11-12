package com.example.game.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.game.combat.CombatAoEEffect
import com.example.game.combat.CombatFxStyle
import com.example.game.combat.CombatProjectile
import com.example.game.combat.ProjectileVisual
import com.example.game.map.ObliqueCamera
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.roundToInt

/** Bounded, local execution feedback: no bitmap creation, screen bloom, or gameplay side effects. */
internal object TowerCombatVfxRenderer {
  private val shape = Path()
  private val directionsX = FloatArray(8) { cos(it*Math.PI.toFloat()/4f) }
  private val directionsY = FloatArray(8) { sin(it*Math.PI.toFloat()/4f) }

  fun DrawScope.drawExecutionFx(fx: CombatAoEEffect, camX: Float, camY: Float, zoom: Float, foreground: Boolean) {
    if ((fx.style != CombatFxStyle.GROUND_RING) != foreground) return
    val p = (fx.elapsed/fx.duration.coerceAtLeast(.001f)).coerceIn(0f,1f)
    if (p >= 1f) return
    val x=ObliqueCamera.worldToScreenX(fx.x,camX,zoom,size.width/2f)
    val y=ObliqueCamera.worldToScreenY(fx.y,camY,zoom,size.height/2f)
    val extent=(fx.maxRadius+110f)*zoom
    if(x < -extent || x > size.width+extent || y < -extent || y > size.height+extent) return
    val alpha=(1f-p)
    val color=fx.color.copy(alpha=alpha*.95f)
    val white=Color(0xFFFFF4D6).copy(alpha=alpha)
    val spread=fx.maxRadius*(.35f+.65f*p)*zoom
    TowerCombatFxAtlas.frame(fx.style,p)?.let { frame ->
      val scale=(if(fx.style==CombatFxStyle.GUARD)1.18f else 1.50f)*zoom
      drawImage(frame.image,
        dstOffset=IntOffset((x-frame.pivotX*scale).roundToInt(),(y-frame.pivotY*scale).roundToInt()),
        dstSize=IntSize((frame.image.width*scale).roundToInt().coerceAtLeast(1),
          (frame.image.height*scale).roundToInt().coerceAtLeast(1)),
        alpha=(1f-p*.65f),filterQuality=FilterQuality.None)
      return
    }
    when(fx.style) {
      CombatFxStyle.GROUND_RING -> {
        val ry=spread*ObliqueCamera.GROUND_ELLIPSE_RATIO
        drawOval(color.copy(alpha=alpha*.18f),Offset(x-spread,y-ry),Size(spread*2,ry*2))
        drawOval(color,Offset(x-spread,y-ry),Size(spread*2,ry*2),style=Stroke(2f*zoom))
      }
      CombatFxStyle.GUARD -> {
        val w=(28f+12f*p)*zoom
        val h=(55f+8f*p)*zoom
        val top=y-h
        shape.reset()
        shape.moveTo(x,top-10f*zoom);shape.lineTo(x+w,top+7f*zoom)
        shape.lineTo(x+w*.8f,y-9f*zoom);shape.lineTo(x,y+5f*zoom)
        shape.lineTo(x-w*.8f,y-9f*zoom);shape.lineTo(x-w,top+7f*zoom);shape.close()
        drawPath(shape,Color(0xFF075985).copy(alpha=alpha*.24f))
        drawPath(shape,color,style=Stroke(3f*zoom))
        drawLine(white,Offset(x,top+4f*zoom),Offset(x,y-8f*zoom),2f*zoom)
        drawLine(white,Offset(x-12f*zoom,top+22f*zoom),Offset(x+12f*zoom,top+22f*zoom),2f*zoom)
        for(i in 0..3) {
          val dx=(i-1.5f)*23f*zoom
          drawLine(color,Offset(x+dx,y+2f*zoom),Offset(x+dx,y-(12f+18f*alpha)*zoom),2f*zoom)
        }
      }
      CombatFxStyle.ARCANE -> {
        // Damage has already executed: the descending lance is an impact afterimage, never a delayed fake hit.
        val h=(95f*alpha+20f)*zoom
        shape.reset();shape.moveTo(x-12f*zoom,y-8f*zoom)
        shape.lineTo(x-5f*zoom,y-h);shape.lineTo(x+9f*zoom,y-h-22f*zoom)
        shape.lineTo(x+12f*zoom,y-8f*zoom);shape.lineTo(x,y+7f*zoom);shape.close()
        drawPath(shape,color.copy(alpha=alpha*.65f));drawPath(shape,white,style=Stroke(2f*zoom))
        for(i in 0..7) {
          val r=spread*(.5f+.5f*p)
          val px=x+directionsX[i]*r;val py=y+directionsY[i]*r*.72f
          val lift=(1f-p)*22f*zoom
          drawLine(color,Offset(px,py),Offset(px+directionsX[i]*9f*zoom,py-lift),3f*zoom)
        }
      }
      CombatFxStyle.SHADOW,CombatFxStyle.CLEAVE -> {
        val length=(if(fx.style==CombatFxStyle.SHADOW)42f else 75f)*zoom
        for(i in 0..2) {
          val offset=(i-1)*12f*zoom
          shape.reset();shape.moveTo(x-length,y+20f*zoom+offset)
          shape.quadraticTo(x+length*.5f,y-75f*zoom+offset,x+length,y-16f*zoom+offset)
          drawPath(shape,color.copy(alpha=alpha*(1f-i*.22f)),style=Stroke((5f-i)*zoom))
        }
        drawLine(white,Offset(x-15f*zoom,y+10f*zoom),Offset(x+22f*zoom,y-35f*zoom),2f*zoom)
      }
      CombatFxStyle.HOLY -> {
        for(i in -1..1) {
          val dx=i*26f*zoom
          val top=y-(65f+22f*alpha-10f*kotlin.math.abs(i))*zoom
          drawLine(color.copy(alpha=alpha*.45f),Offset(x+dx,y),Offset(x+dx,top),7f*zoom)
          drawLine(white,Offset(x+dx,top),Offset(x+dx,top+16f*zoom),2f*zoom)
          drawLine(white,Offset(x+dx-5f*zoom,top+7f*zoom),Offset(x+dx+5f*zoom,top+7f*zoom),2f*zoom)
        }
      }
      CombatFxStyle.IMPACT,CombatFxStyle.SHIELD_IMPACT -> {
        val center=Offset(x,y-22f*zoom)
        val length=(if(fx.style==CombatFxStyle.IMPACT)18f else 25f)*zoom*(.5f+p)
        for(i in 0..7) {
          val inner=length*.35f
          drawLine(if(i%2==0)white else color,
            center+Offset(directionsX[i]*inner,directionsY[i]*inner),
            center+Offset(directionsX[i]*length,directionsY[i]*length),2f*zoom)
        }
        if(fx.style==CombatFxStyle.SHIELD_IMPACT) {
          shape.reset();shape.moveTo(x-18f*zoom,y-45f*zoom);shape.lineTo(x,y-57f*zoom)
          shape.lineTo(x+18f*zoom,y-45f*zoom);shape.lineTo(x+14f*zoom,y-18f*zoom)
          shape.lineTo(x,y-9f*zoom);shape.lineTo(x-14f*zoom,y-18f*zoom);shape.close()
          drawPath(shape,color,style=Stroke(2f*zoom))
        }
      }
      CombatFxStyle.SLAM -> {
        for(i in 0..7) {
          val r=spread
          val px=x+directionsX[i]*r;val py=y+directionsY[i]*r*.72f
          val lift=25f*zoom*sin(p*Math.PI.toFloat())
          drawLine(Color(0xFFB69A73).copy(alpha=alpha*.8f),
            Offset(x+directionsX[i]*r*.55f,y+directionsY[i]*r*.55f*.72f),
            Offset(px,py),3f*zoom)
          drawRect(Color(0xFFD0BB8A).copy(alpha=alpha),Offset(px,py-lift),Size(5f*zoom,4f*zoom))
        }
      }
    }
  }

  fun DrawScope.drawCombatProjectile(projectile: CombatProjectile,camX:Float,camY:Float,zoom:Float) {
    val x=ObliqueCamera.worldToScreenX(projectile.x,camX,zoom,size.width/2f)
    val y=ObliqueCamera.worldToScreenY(projectile.y,camY,zoom,size.height/2f,elevationZ=28f)
    if(x < -60f || x > size.width+60f || y < -60f || y > size.height+60f) return
    val magnitude=hypot(projectile.vx,projectile.vy*.72f).coerceAtLeast(.001f)
    val dx=projectile.vx/magnitude;val dy=projectile.vy*.72f/magnitude
    val nx=-dy;val ny=dx
    val arrow=projectile.visual==ProjectileVisual.ARROW || projectile.visual==ProjectileVisual.HOSTILE_ARROW
    if(arrow) {
      val tail=Offset(x-dx*25f*zoom,y-dy*25f*zoom)
      drawLine(Color(0xFF62432F),tail,Offset(x,y),3f*zoom)
      drawLine(Color(0xFFE7CCA1),tail,Offset(x-dx*5f*zoom,y-dy*5f*zoom),zoom)
      shape.reset();shape.moveTo(x+dx*4f*zoom,y+dy*4f*zoom)
      shape.lineTo(x-dx*6f*zoom+nx*4f*zoom,y-dy*6f*zoom+ny*4f*zoom)
      shape.lineTo(x-dx*6f*zoom-nx*4f*zoom,y-dy*6f*zoom-ny*4f*zoom);shape.close()
      drawPath(shape,Color(0xFFF2EBD7))
      drawLine(projectile.color,tail+Offset(nx*4f*zoom,ny*4f*zoom),
        tail+Offset(dx*6f*zoom,dy*6f*zoom),2f*zoom)
      drawLine(projectile.color,tail-Offset(nx*4f*zoom,ny*4f*zoom),
        tail+Offset(dx*6f*zoom,dy*6f*zoom),2f*zoom)
      return
    }
    val length=(if(projectile.visual==ProjectileVisual.ARCANE)23f else 16f)*zoom
    drawLine(projectile.color.copy(alpha=.30f),Offset(x-dx*length*1.5f,y-dy*length*1.5f),
      Offset(x,y),7f*zoom)
    shape.reset();shape.moveTo(x+dx*5f*zoom,y+dy*5f*zoom)
    shape.lineTo(x-dx*length+nx*4f*zoom,y-dy*length+ny*4f*zoom)
    shape.lineTo(x-dx*length*.65f,y-dy*length*.65f)
    shape.lineTo(x-dx*length-nx*4f*zoom,y-dy*length-ny*4f*zoom);shape.close()
    drawPath(shape,projectile.color)
    drawLine(Color(0xFFFFF4DE),Offset(x-dx*9f*zoom,y-dy*9f*zoom),Offset(x,y),2f*zoom)
    if(projectile.visual==ProjectileVisual.HOLY) {
      drawLine(Color(0xFFD1FAE5),Offset(x-nx*5f*zoom,y-ny*5f*zoom),
        Offset(x+nx*5f*zoom,y+ny*5f*zoom),2f*zoom)
    }
  }
}

