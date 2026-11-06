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

  // P24: light stamps are owned by HdrLightResponse (single pipeline, see drawProp).
  // This lobby-only pass keeps just the wet ground reflection beside real emitters.
  private fun wetTint(kind: PropKind): Color? = when (kind) {
    PropKind.STREET_LANTERN, PropKind.FACILITY_RESTAURANT, PropKind.FACILITY_ARMORY,
    PropKind.FACILITY_HERO_QUARTERS, PropKind.FACILITY_HERO_REGISTRY,
    PropKind.FACILITY_SYNTHESIS_LAB, PropKind.SANCTUARY_COTTAGE_BROWN,
    PropKind.SANCTUARY_COTTAGE_SLATE -> Color(0xffefbb7f.toInt())
    PropKind.ARCANE_BRAZIER, PropKind.FACILITY_TOWER_GATE,
    PropKind.TOWER_PORTAL_GATE -> Color(0xff75c9d7.toInt())
    else -> null
  }

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
    // P29 perf: rain streak count scales with the quality tier (REDUCED halves fill-rate;
    // BALANCED keeps the authored density; OFF is handled by callers/quality gates).
    val tierScale=when(DepthPresentation.quality){DepthPresentation.Quality.OFF->0f;DepthPresentation.Quality.REDUCED->0.5f;else->1f}
    if(weather.rain>.01f&&tierScale>0f) {
      val count=((if(foreground) 90 else 60)*tierScale).toInt().coerceAtLeast(8)
      for(i in 0 until count) {
        // World-anchored repeating volume: camera motion changes parallax; time is not frame-integrated.
        val x=((i*127.13f+layer*43-camX*zoom*(.65f+layer*.35f)-time*(28+layer*12))%size.width+size.width)%size.width
        val y=((i*83.71f-camY*zoom*.72f+time*(230+layer*100))%size.height+size.height)%size.height
        if(i.toFloat()/count > weather.rain) continue
        drawLine(Color(0xffb4d0dc).copy(alpha=weather.rain*(if(foreground) .26f else .13f)),Offset(x,y),Offset(x-4f,y+10f+layer*6),strokeWidth=if(foreground) 1.2f else 1f)
      }
    }
    if(weather.fog>.01f&&tierScale>0.5f) {
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
  /**
   * P24: per-prop ground spill, emissive core, halo and paired key/shade material
   * response now live in `PixelSpriteRenderer.drawProp` -> `HdrLightResponse`, which
   * runs in BOTH the lobby and the Robolectric render harness. Keeping the old stamps
   * here would double every local light, so this pass owns only the rain reflection:
   * one short wet streak on stone beside a real emitter. No per-frame allocation.
   */
  fun DrawScope.drawLocalLights(obstacles: List<WorldObstacle>,camX: Float,camY: Float,zoom: Float) {
    if (DepthPresentation.quality == DepthPresentation.Quality.OFF || zoom <= 0f) return
    val rain = WorldEnvironment.weather.rain
    if (rain <= 0.1f) return
    val intensity = WorldEnvironment.lighting.localLightIntensity
    for (obs in obstacles) {
      val tint = wetTint(obs.propKind) ?: continue
      val x = ObliqueCamera.worldToScreenX(obs.x,camX,zoom,size.width/2)
      val y = ObliqueCamera.worldToScreenY(obs.y,camY,zoom,size.height/2,obs.elevationZ)
      if (x < -40f || x > size.width + 40f || y < -40f || y > size.height + 40f) continue
      val scale = zoom*obs.scale
      drawLine(
        tint.copy(alpha = .08f*intensity*rain),
        Offset(x-5f*scale,y+4f*scale),
        Offset(x+5f*scale,y+4f*scale),
        scale,
      )
    }
  }
}
