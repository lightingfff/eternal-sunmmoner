package com.example.game.world

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.*

enum class WorldWeather { CLEAR, CLOUDY, RAIN, STORM, FOG }
class WorldClock(val originMillis: Long, val dayDurationMillis: Long = DEFAULT_DAY_MILLIS) {
  init { require(dayDurationMillis > 0) }
  fun hour(nowMillis: Long): Float = Math.floorMod(nowMillis - originMillis, dayDurationMillis).toFloat() / dayDurationMillis * 24f
  companion object { const val DEFAULT_DAY_MILLIS = 24L * 60 * 1000 }
}
data class WeatherState(
  val rain: Float = 0f,
  val fog: Float = 0f,
  val cloud: Float = 0f,
  val weatherType: WorldWeather = WorldWeather.CLEAR,
) {
  companion object {
    fun blend(a: WorldWeather, b: WorldWeather, t: Float): WeatherState {
      val k = t.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
      fun cloudOf(w: WorldWeather): Float = when (w) {
        WorldWeather.CLEAR -> 0f
        WorldWeather.CLOUDY, WorldWeather.RAIN, WorldWeather.STORM -> 1f
        WorldWeather.FOG -> 0.5f
      }
      fun rainOf(w: WorldWeather): Float = when (w) {
        WorldWeather.RAIN -> 1f
        WorldWeather.STORM -> 0.7f // giông có mưa nhưng không liệt tục như mưa thường
        else -> 0f
      }
      // weatherType theo trạng thái đích (b) khi vượt nửa chu kỳ chuyển đổi
      val type = if (k >= 0.5f) b else a
      return WeatherState(
        (rainOf(a) * (1f - k)) + (rainOf(b) * k),
        (if (a == WorldWeather.FOG) 1f - k else 0f) + (if (b == WorldWeather.FOG) k else 0f),
        (cloudOf(a) * (1f - k)) + (cloudOf(b) * k),
        type,
      )
    }

    fun at(elapsedMillis: Long): WeatherState {
      val slot = Math.floorDiv(elapsedMillis, 240000L)
      // P26C: thêm STORM vào chu kỳ thời tiết để world đọc được trạng thái giông bão.
      val schedule = arrayOf(WorldWeather.CLEAR, WorldWeather.CLOUDY, WorldWeather.RAIN, WorldWeather.STORM, WorldWeather.CLEAR, WorldWeather.FOG)
      val a = schedule[Math.floorMod(slot - 1, 6L).toInt()]
      val b = schedule[Math.floorMod(slot, 6L).toInt()]
      return blend(a, b, Math.floorMod(elapsedMillis, 240000L) / 30000f)
    }
  }
}

/**
 * P26: richer time-of-day lighting model. Anchors MORNING / NOON / EVENING / NIGHT plus a
 * dawn transition; each state carries its own sky gradient stops, sun/moon disc, cloud
 * character, ray colour and haze, so the four states read differently at a glance.
 * Weather multiplies the model (cloud cover, rain, fog) instead of only adding particles.
 */
data class WorldLighting(
  val hour: Float,
  val ambient: Color,
  val ambientIntensity: Float,
  val directionalColor: Color,
  val directionX: Float,
  val directionY: Float,
  val shadowStrength: Float,
  val shadowLength: Float,
  val skyTint: Color,
  val localLightIntensity: Float,
  val skyColors: List<Color>,
  val skyBrightness: Float,
  val sunColor: Color,
  val sunIntensity: Float,
  val sunElevation: Float,
  val moonColor: Color,
  val moonIntensity: Float,
  val moonElevation: Float,
  val cloudColor: Color,
  val cloudDensity: Float,
  val cloudBrightness: Float,
  val rayColor: Color,
  val rayAlpha: Float,
  val hazeColor: Color,
  val hazeDensity: Float,
) {
  /** Weather multipliers: mood must change with weather, not just particle counts. */
  fun withWeather(state: WeatherState): WorldLighting = copy(
    ambientIntensity = ambientIntensity * (1f - 0.08f * state.cloud - 0.10f * state.rain - 0.02f * state.fog - (if (state.weatherType == WorldWeather.STORM) 0.10f else 0f)),
    shadowStrength = shadowStrength * (1f - 0.20f * state.rain - 0.15f * state.fog - (if (state.weatherType == WorldWeather.STORM) 0.15f else 0f)),
    sunIntensity = sunIntensity * (1f - 0.45f * state.cloud - 0.65f * state.rain - (if (state.weatherType == WorldWeather.STORM) 0.25f else 0f)),
    cloudDensity = cloudDensity * (1f + 0.50f * state.cloud + 0.60f * state.rain + 0.10f * state.fog + (if (state.weatherType == WorldWeather.STORM) 0.30f else 0f)),
    cloudBrightness = cloudBrightness * (1f - 0.05f * state.cloud - 0.20f * state.rain - (if (state.weatherType == WorldWeather.STORM) 0.15f else 0f)),
    rayAlpha = rayAlpha * (1f - 0.55f * state.cloud - 0.80f * state.rain - (if (state.weatherType == WorldWeather.STORM) 0.40f else 0f)),
    skyBrightness = skyBrightness * (1f - 0.15f * state.cloud - 0.22f * state.rain - 0.05f * state.fog - (if (state.weatherType == WorldWeather.STORM) 0.10f else 0f)),
    hazeDensity = hazeDensity * (1f + 1.00f * state.fog + 0.50f * state.rain + (if (state.weatherType == WorldWeather.STORM) 0.80f else 0f)),
    localLightIntensity = localLightIntensity * (1f + 0.15f * state.rain + 0.05f * state.fog + (if (state.weatherType == WorldWeather.STORM) 0.20f else 0f)),
    cloudColor = if (state.weatherType == WorldWeather.STORM) Color(0xff4A5568) else cloudColor,
  )
  companion object {
    // Anchor hours: deep night -> dawn -> morning -> noon -> evening -> night -> deep night
    private val hours = floatArrayOf(0f, 5f, 8f, 12f, 17f, 20f, 24f)
    // sky stops: zenith, midUpper, mid, horizon (per anchor)
    private val skyZenith = listOf(
      Color(0xff0E1230), Color(0xff4A6A9E), Color(0xff5589C4), Color(0xff5E9BE6),
      Color(0xff4A427A), Color(0xff141A3A), Color(0xff0E1230),
    )
    private val skyMidUpper = listOf(
      Color(0xff1B2350), Color(0xff7E9BC8), Color(0xff88A4D0), Color(0xff8FBEF2),
      Color(0xff9A6E96), Color(0xff262E58), Color(0xff1B2350),
    )
    private val skyMid = listOf(
      Color(0xff2C3360), Color(0xffB8C8E0), Color(0xffAEC2DE), Color(0xffB8D4F2),
      Color(0xffD09A96), Color(0xff3A3D6E), Color(0xff2C3360),
    )
    private val skyHorizon = listOf(
      Color(0xff3A3D6E), Color(0xffF5DFC0), Color(0xffEDDDB8), Color(0xffD8EAF8),
      Color(0xffF0A06A), Color(0xff4A4D7E), Color(0xff3A3D6E),
    )
    private val ambientCols = listOf(
      Color(0xff617ca6), Color(0xffB8C8E0), Color(0xffDCE8F8), Color(0xffF0F6FF),
      Color(0xffE8B88A), Color(0xff40507A), Color(0xff617ca6),
    )
    private val ambientIntensities = floatArrayOf(.73f, .80f, .90f, 1f, .88f, .75f, .73f)
    private val shadowStrengths = floatArrayOf(.78f, .75f, .72f, .85f, .80f, .78f, .78f)
    private val shadowLengths = floatArrayOf(1.0f, 1.4f, 1.2f, .5f, 1.6f, 1.0f, 1.0f)
    private val localLights = floatArrayOf(1.0f, .85f, .55f, .30f, .60f, .95f, 1.0f)
    // Sun: colour / intensity / elevation radians (below horizon => 0 intensity)
    private val sunCols = listOf(
      Color(0xffFFD9A0), Color(0xffFFD9A0), Color(0xffFFE3B0), Color(0xffFFF6E0),
      Color(0xffFF9E5E), Color(0xffFFD9A0), Color(0xffFFD9A0),
    )
    private val sunIntensities = floatArrayOf(0f, .30f, .90f, 1f, .85f, 0f, 0f)
    private val sunElevations = floatArrayOf(-.20f, .25f, .65f, 1.15f, .28f, -.15f, -.20f)
    private val moonCol = Color(0xffC8D8F0)
    private val moonIntensities = floatArrayOf(.35f, .15f, 0f, 0f, 0f, .30f, .35f)
    private val moonElevations = floatArrayOf(.75f, .35f, 0f, 0f, 0f, .70f, .75f)
    // Cloud character per state: colour / density multiplier / brightness multiplier
    private val cloudCols = listOf(
      Color(0xff2E3558), Color(0xffE8EEF8), Color(0xffFFFFFF), Color(0xffFFFFFF),
      Color(0xffF5C9B8), Color(0xff333A5E), Color(0xff2E3558),
    )
    private val cloudDensities = floatArrayOf(.35f, .50f, .50f, .45f, .50f, .38f, .35f)
    private val cloudBrightnesses = floatArrayOf(.55f, .80f, .90f, 1f, .85f, .50f, .55f)
    // Sky rays: colour + alpha (moonlight at night, warm gold sunbeams by day)
    private val rayCols = listOf(
      Color(0xff7FA8E8), Color(0xffFFD9A0), Color(0xffFFE0A0), Color(0xffFFF2C8),
      Color(0xffFFB36A), Color(0xff7FA8E8), Color(0xff7FA8E8),
    )
    private val rayAlphas = floatArrayOf(.10f, .08f, .10f, .12f, .14f, .09f, .10f)
    private val hazeCols = listOf(
      Color(0xff2A3358), Color(0xffF5DFC0), Color(0xffF8E8C8), Color(0xffD8EAF8),
      Color(0xffF0A06A), Color(0xff3A3D6E), Color(0xff2A3358),
    )
    private val hazeDensities = floatArrayOf(.35f, .25f, .15f, .10f, .30f, .32f, .35f)

    fun at(hour: Float): WorldLighting {
      val h = ((hour % 24) + 24) % 24
      val i = (0..6).first { h < hours[it + 1] }
      val t = ((h - hours[i]) / (hours[i + 1] - hours[i])).let { it * it * (3 - 2 * it) }
      fun mix(a: Float, b: Float) = a + (b - a) * t
      fun mixCol(a: Color, b: Color) = lerp(a, b, t)
      val intensity = mix(ambientIntensities[i], ambientIntensities[i + 1])
      val tint = mixCol(ambientCols[i], ambientCols[i + 1])
      val dx = .615f + .16f * sin((h - 12) * PI.toFloat() / 12)
      val dy = .788f
      val len = sqrt(dx * dx + dy * dy)
      return WorldLighting(
        h, tint, intensity, tint, dx / len, dy / len,
        mix(shadowStrengths[i], shadowStrengths[i + 1]),
        mix(shadowLengths[i], shadowLengths[i + 1]),
        mixCol(skyHorizon[i], skyHorizon[i + 1]),
        mix(localLights[i], localLights[i + 1]),
        listOf(
          mixCol(skyZenith[i], skyZenith[i + 1]),
          mixCol(skyMidUpper[i], skyMidUpper[i + 1]),
          mixCol(skyMid[i], skyMid[i + 1]),
          mixCol(skyHorizon[i], skyHorizon[i + 1]),
        ),
        1f,
        mixCol(sunCols[i], sunCols[i + 1]),
        mix(sunIntensities[i], sunIntensities[i + 1]),
        mix(sunElevations[i], sunElevations[i + 1]),
        moonCol,
        mix(moonIntensities[i], moonIntensities[i + 1]),
        mix(moonElevations[i], moonElevations[i + 1]),
        mixCol(cloudCols[i], cloudCols[i + 1]),
        mix(cloudDensities[i], cloudDensities[i + 1]),
        mix(cloudBrightnesses[i], cloudBrightnesses[i + 1]),
        mixCol(rayCols[i], rayCols[i + 1]),
        mix(rayAlphas[i], rayAlphas[i + 1]),
        mixCol(hazeCols[i], hazeCols[i + 1]),
        mix(hazeDensities[i], hazeDensities[i + 1]),
      )
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
    weather=if(indoors) WeatherState() else debugWeather?.let { WeatherState.blend(it,it,1f) } ?: WeatherState.at(nowMillis-clock.originMillis)
    lighting=WorldLighting.at(if(indoors) 12f else debugHour ?: clock.hour(nowMillis)).withWeather(weather)
  }
  /** Hidden developer/test control. No normal-gameplay buttons are added. */
  fun debugOverride(hour: Float?=null,weather: WorldWeather?=null) {
    check(com.example.BuildConfig.DEBUG);debugHour=hour;debugWeather=weather
  }
}
