package com.example.game.world

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.*

enum class WorldWeather { CLEAR, RAIN, FOG }
class WorldClock(val originMillis: Long, val dayDurationMillis: Long = DEFAULT_DAY_MILLIS) {
  init { require(dayDurationMillis > 0) }
  fun hour(nowMillis: Long): Float = Math.floorMod(nowMillis - originMillis, dayDurationMillis).toFloat() / dayDurationMillis * 24f
  companion object { const val DEFAULT_DAY_MILLIS = 24L * 60 * 1000 }
}
data class WeatherState(val rain: Float = 0f, val fog: Float = 0f) {
  companion object {
    fun blend(a: WorldWeather,b: WorldWeather,t: Float): WeatherState {
      val k=t.coerceIn(0f,1f).let { it*it*(3-2*it) }
      return WeatherState((if(a==WorldWeather.RAIN) 1f-k else 0f)+(if(b==WorldWeather.RAIN) k else 0f),
        (if(a==WorldWeather.FOG) 1f-k else 0f)+(if(b==WorldWeather.FOG) k else 0f))
    }
    fun at(elapsedMillis: Long): WeatherState {
      val slot=Math.floorDiv(elapsedMillis,240000L)
      val schedule=arrayOf(WorldWeather.CLEAR,WorldWeather.CLEAR,WorldWeather.RAIN,WorldWeather.CLEAR,WorldWeather.FOG)
      val a=schedule[Math.floorMod(slot-1,5L).toInt()]; val b=schedule[Math.floorMod(slot,5L).toInt()]
      return blend(a,b,Math.floorMod(elapsedMillis,240000L)/30000f)
    }
  }
}
data class WorldLighting(val hour: Float,val ambient: Color,val ambientIntensity: Float,
  val directionalColor: Color,val directionX: Float,val directionY: Float,
  val shadowStrength: Float,val shadowLength: Float,val skyTint: Color,val localLightIntensity: Float) {
  companion object {
    private val hours=floatArrayOf(0f,5f,7f,12f,16f,18.5f,21f,24f)
    private val colors=listOf(Color(0xff617ca6),Color(0xff9c89a2),Color(0xffffd4a1),Color(0xfff3efd8),Color(0xffe6cfb0),Color(0xffd59979),Color(0xff617ca6),Color(0xff617ca6))
    private val intensities=floatArrayOf(.73f,.79f,.94f,1f,.97f,.86f,.73f,.73f)
    private val lengths=floatArrayOf(1.1f,1.6f,1.4f,.55f,.9f,1.8f,1.1f,1.1f)
    fun at(hour: Float): WorldLighting {
      val h=((hour%24)+24)%24; val i=(0..6).first { h < hours[it+1] }
      val t=((h-hours[i])/(hours[i+1]-hours[i])).let { it*it*(3-2*it) }
      fun mix(a: Float,b: Float)=a+(b-a)*t
      val intensity=mix(intensities[i],intensities[i+1]);val tint=lerp(colors[i],colors[i+1],t)
      val dx=.615f+.16f*sin((h-12)*PI.toFloat()/12);val dy=.788f;val len=sqrt(dx*dx+dy*dy)
      return WorldLighting(h,tint,intensity,tint,dx/len,dy/len,.8f+.2f*(intensity-.73f)/.27f,mix(lengths[i],lengths[i+1]),tint, .35f+(1f-intensity)*3.8f)
    }
  }
}
object WorldEnvironment {
  private var clock=WorldClock(0)
  var lighting=WorldLighting.at(12f); private set
  var weather=WeatherState(); private set
  var elapsedSeconds=0f; private set
  private var debugHour: Float?=null
  private var debugWeather: WorldWeather?=null
  fun init(context: Context) {
    val prefs=context.applicationContext.getSharedPreferences("sanctuary_world",Context.MODE_PRIVATE)
    val origin=prefs.getLong("origin_millis",Long.MIN_VALUE)
    val duration=prefs.getLong("day_duration_millis",WorldClock.DEFAULT_DAY_MILLIS).coerceAtLeast(1000L)
    val saved=if(origin==Long.MIN_VALUE) System.currentTimeMillis()-duration/2 else origin
    if(origin==Long.MIN_VALUE) prefs.edit().putLong("origin_millis",saved).putLong("day_duration_millis",duration).apply()
    clock=WorldClock(saved,duration)
  }
  fun update(nowMillis: Long=System.currentTimeMillis(), indoors: Boolean=false) {
    elapsedSeconds=(Math.floorMod(nowMillis-clock.originMillis,86400000L)/1000.0).toFloat()
    lighting=WorldLighting.at(if(indoors) 12f else debugHour ?: clock.hour(nowMillis))
    weather=if(indoors) WeatherState() else debugWeather?.let { WeatherState.blend(it,it,1f) } ?: WeatherState.at(nowMillis-clock.originMillis)
    lighting=lighting.copy(ambientIntensity=lighting.ambientIntensity-.035f*weather.rain,
      shadowStrength=lighting.shadowStrength*(1-.35f*weather.rain-.25f*weather.fog))
  }
  /** Hidden developer/test control. No normal-gameplay buttons are added. */
  fun debugOverride(hour: Float?=null,weather: WorldWeather?=null) {
    check(com.example.BuildConfig.DEBUG);debugHour=hour;debugWeather=weather
  }
}
