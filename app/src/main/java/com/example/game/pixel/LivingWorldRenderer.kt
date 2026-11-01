package com.example.game.pixel

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.game.map.*
import com.example.game.world.WorldEnvironment
import kotlin.math.*

/** Canvas-only atmospheric passes; no blur, shader or changes to world collision. */
object LivingWorldRenderer {
  fun DrawScope.drawWorldGrade() {
    val light=WorldEnvironment.lighting
    // Preserve texture values: daytime gently lifts existing twilight art, night stays readable.
    if(light.ambientIntensity>.9f) drawRect(Color(0xffffefd5).copy(alpha=(light.ambientIntensity-.9f)*.65f))
    drawRect(light.ambient.copy(alpha=(1-light.ambientIntensity)*.55f))
    drawRect(Color(0xff101d33).copy(alpha=(1-light.ambientIntensity)*.42f))
    drawRect(light.skyTint.copy(alpha=.04f))
    if(WorldEnvironment.weather.rain>0f) drawRect(Color(0xff142c31).copy(alpha=.10f*WorldEnvironment.weather.rain))
  }
  fun DrawScope.drawWeather(camX: Float,camY: Float,zoom: Float,foreground: Boolean) {
    val weather=WorldEnvironment.weather;val time=WorldEnvironment.elapsedSeconds
    val layer=if(foreground) 1 else 0
    if(weather.rain>.01f) {
      val count=(if(foreground) 90 else 60)
      for(i in 0 until count) {
        // World-anchored repeating volume: camera motion changes parallax; time is not frame-integrated.
        val x=((i*127.13f+layer*43-camX*zoom*(.65f+layer*.35f)-time*(28+layer*12))%size.width+size.width)%size.width
        val y=((i*83.71f-camY*zoom*.72f+time*(230+layer*100))%size.height+size.height)%size.height
        if(i.toFloat()/count > weather.rain) continue
        drawLine(Color(0xffb4d0dc).copy(alpha=weather.rain*(if(foreground) .26f else .13f)),Offset(x,y),Offset(x-4f,y+10f+layer*6),strokeWidth=if(foreground) 1.2f else 1f)
      }
    }
    if(weather.fog>.01f) {
      for(i in 0..3) {
        val drift=sin(time*.022f+i*1.7f)*size.width*.18f
        val x=size.width*(.12f+i*.28f)+drift-camX*zoom*.06f
        val y=size.height*(.3f+i*.19f)-camY*zoom*.025f
        val radius=size.width*.42f
        drawOval(Brush.radialGradient(listOf(Color(0xff9bb0bb).copy(alpha=weather.fog*(if(foreground) .12f else .09f)),Color.Transparent),Offset(x,y),radius),Offset(x-radius,y-radius*.24f),Size(radius*2,radius*.48f))
      }
    }
  }
  fun DrawScope.drawLocalLights(obstacles: List<WorldObstacle>,camX: Float,camY: Float,zoom: Float) {
    val intensity=WorldEnvironment.lighting.localLightIntensity
    val time=WorldEnvironment.elapsedSeconds
    for(obs in obstacles) {
      val color=when(obs.propKind) {
        PropKind.STREET_LANTERN,PropKind.ARCANE_BRAZIER -> Color(0xffffc17d)
        PropKind.FACILITY_TOWER_GATE,PropKind.TOWER_PORTAL_GATE -> Color(0xff75c9d7)
        PropKind.FACILITY_RESTAURANT,PropKind.FACILITY_ARMORY,PropKind.FACILITY_HERO_QUARTERS,
        PropKind.FACILITY_HERO_REGISTRY,PropKind.FACILITY_SYNTHESIS_LAB,
        PropKind.SANCTUARY_COTTAGE_BROWN,PropKind.SANCTUARY_COTTAGE_SLATE -> Color(0xffefbb7f)
        else -> continue
      }
      val x=ObliqueCamera.worldToScreenX(obs.x,camX,zoom,size.width/2)
      val y=ObliqueCamera.worldToScreenY(obs.y,camY,zoom,size.height/2,obs.elevationZ)
      val r=75f*zoom*obs.scale
      if(x+r<0 || x-r>size.width || y+r<0 || y-r>size.height) continue
      val flicker=.94f+.06f*sin(time*3.7f+obs.x*.03f)
      withTransform({scale(1f,.72f,Offset(x,y))}) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha=(.16f*intensity*flicker).coerceAtMost(.3f)),Color.Transparent),Offset(x,y),r),r,Offset(x,y))
      }
      // Small window/lantern halo above the ground, not a floor reflection.
      val height=if(obs.propKind==PropKind.STREET_LANTERN) 70f else 55f
      val center=Offset(x,y-height*zoom*obs.scale)
      drawCircle(Brush.radialGradient(listOf(color.copy(alpha=.09f*intensity),Color.Transparent),center,r*.42f),r*.42f,center)
      if(WorldEnvironment.weather.rain>.1f) drawLine(color.copy(alpha=.12f*WorldEnvironment.weather.rain),Offset(x-7*zoom,y+3*zoom),Offset(x+5*zoom,y+3*zoom),2*zoom)
    }
  }
}
