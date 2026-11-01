package com.example.game.pixel

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.game.map.*
import com.example.game.world.WorldEnvironment
import kotlin.math.*

/** Canvas-only atmosphere and bounded emissive lighting. Gameplay sprites stay pixel sharp. */
object LivingWorldRenderer {
  // Unit-space gradient is transformed with the fog oval and reused at every camera position.
  private val fogBrush = Brush.radialGradient(listOf(Color(0xff9bb0bb), Color.Transparent), Offset.Zero, 1f)

  private data class LocalLight(
    val tint: DepthPresentation.LightTint,
    val groundRadius: Float,
    val groundAlpha: Float,
    val haloRadius: Float,
    val haloAlpha: Float,
    val flickers: Boolean = false,
  )
  private val lanternLight = LocalLight(DepthPresentation.LightTint.WARM_AMBER,58f,.085f,20f,.09f,true)
  private val brazierLight = LocalLight(DepthPresentation.LightTint.COOL_AETHER,64f,.08f,22f,.085f,true)
  private val gateLight = LocalLight(DepthPresentation.LightTint.COOL_AETHER,86f,.075f,28f,.065f)
  private val windowLight = LocalLight(DepthPresentation.LightTint.WINDOW_GOLD,48f,.055f,18f,.045f)

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
        withTransform({ translate(x,y); scale(radius,radius*.24f,Offset.Zero) }) {
          drawCircle(fogBrush,1f,Offset.Zero,alpha=weather.fog*(if(foreground) .12f else .09f))
        }
      }
    }
  }
  fun DrawScope.drawLocalLights(obstacles: List<WorldObstacle>,camX: Float,camY: Float,zoom: Float) {
    if (DepthPresentation.quality == DepthPresentation.Quality.OFF || zoom <= 0f) return
    val intensity=WorldEnvironment.lighting.localLightIntensity
    val time=WorldEnvironment.elapsedSeconds
    for(obs in obstacles) {
      val light=when(obs.propKind) {
        PropKind.STREET_LANTERN -> lanternLight
        PropKind.ARCANE_BRAZIER -> brazierLight
        PropKind.FACILITY_TOWER_GATE,PropKind.TOWER_PORTAL_GATE -> gateLight
        PropKind.FACILITY_RESTAURANT,PropKind.FACILITY_ARMORY,PropKind.FACILITY_HERO_QUARTERS,
        PropKind.FACILITY_HERO_REGISTRY,PropKind.FACILITY_SYNTHESIS_LAB,
        PropKind.SANCTUARY_COTTAGE_BROWN,PropKind.SANCTUARY_COTTAGE_SLATE -> windowLight
        else -> continue
      }
      val x=ObliqueCamera.worldToScreenX(obs.x,camX,zoom,size.width/2)
      val y=ObliqueCamera.worldToScreenY(obs.y,camY,zoom,size.height/2,obs.elevationZ)
      val scale=zoom*obs.scale
      val radius=light.groundRadius*scale
      // Cull a conservative upright envelope BEFORE looking up the shared cached sprite.
      if(x+radius<0 || x-radius>size.width || y+radius<0 || y-500f*scale>size.height) continue
      val flicker=if(light.flickers) .96f+.04f*sin(time*3.7f+obs.x*.03f) else 1f
      val sprite=if(obs.propKind==PropKind.ARCANE_BRAZIER) PixelStructureAtlas.brazierFrames[0]
        else SanctuaryEnvironmentShadows.sprite(obs)
      val spriteScale=if(obs.propKind==PropKind.ARCANE_BRAZIER) 1.95f else ObliqueSpriteGeometry.pixelScale(obs.propKind)
      val footOffset=if(sprite==null) 0f else if(obs.propKind==PropKind.ARCANE_BRAZIER) 4f
        else ObliqueSpriteGeometry.drawFootOffset(obs.propKind,sprite)
      val emitterHeight=if(sprite==null) 48f else when(obs.propKind) {
        // Actual source glass/flame pixels; do not float halos at an unrelated fixed height.
        PropKind.STREET_LANTERN -> (sprite.height-22f)*spriteScale-footOffset
        PropKind.ARCANE_BRAZIER -> (sprite.height-18f)*spriteScale-footOffset
        else -> {
          val bounds=ObliqueSpriteGeometry.bounds(sprite)
          (bounds.bottomPadding+bounds.visibleHeight*.25f)*spriteScale-footOffset
        }
      }
      val emitterOffsetX=if(obs.propKind==PropKind.STREET_LANTERN && sprite!=null)
        (22f-sprite.width/2f)*spriteScale*scale else 0f
      with(DepthPresentation) {
        // Broadest component lives on the .72 ground plane; the emissive core remains crisp.
        drawLightStamp(Offset(x+emitterOffsetX,y+5f*scale),radius,radius*ObliqueCamera.OBLIQUE_Y_SCALE,
          light.tint,light.groundAlpha*intensity*flicker)
        if(quality==DepthPresentation.Quality.BALANCED) {
          val haloRadius=light.haloRadius*scale
          drawLightStamp(Offset(x+emitterOffsetX,y-emitterHeight*scale),haloRadius,haloRadius,
            light.tint,light.haloAlpha*intensity*flicker)
        }
      }
      if(WorldEnvironment.weather.rain>.1f) {
        val color=if(light.tint==DepthPresentation.LightTint.COOL_AETHER) Color(0xff75c9d7) else Color(0xffefbb7f)
        drawLine(color.copy(alpha=.08f*intensity*WorldEnvironment.weather.rain),
          Offset(x-5f*scale,y+4f*scale),Offset(x+5f*scale,y+4f*scale),scale)
      }
    }
  }
}
