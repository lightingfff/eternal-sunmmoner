package com.example

import com.example.game.pixel.SanctuaryWorldRenderer
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldLighting
import com.example.game.world.WorldWeather
import com.example.game.world.WeatherState
import androidx.compose.ui.graphics.toArgb
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * P26C ROUND 1 contract test: STORM weather state + visible clouds in gameplay camera.
 *
 * MODEL-level assertions (deterministic, no pixels); pixel-level gates are measured by
 * render evidence (docs/visual-evidence/p26c/) and tools/p26c_measure_clouds.py.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class P26CCloudStormContractTest {

  @Before fun setup() {
    val context = RuntimeEnvironment.getApplication()
    com.example.game.pixel.SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
  }

  @After fun cleanup() {
    WorldEnvironment.debugOverride()
    WorldEnvironment.update()
  }

  private fun lightingAt(hour: Float, weather: WorldWeather): WorldLighting {
    WorldEnvironment.debugOverride(hour, weather)
    WorldEnvironment.update(50000)
    return WorldEnvironment.lighting
  }

  @Test
  fun stormWeatherStateExists() {
    // STORM là thành viên của enum WorldWeather (P26C yêu cầu).
    assertEquals(WorldWeather.STORM, WorldWeather.STORM)
    assertEquals(5, WorldWeather.values().size) // CLEAR, CLOUDY, RAIN, STORM, FOG
  }

  @Test
  fun weatherCycleIncludesStorm() {
    // Chu kỳ 6 slot (24 phút) phải chứa STORM.
    val slot0 = WeatherState.at(0L).weatherType
    val slot1 = WeatherState.at(240000L).weatherType
    val slot2 = WeatherState.at(480000L).weatherType
    val slot3 = WeatherState.at(720000L).weatherType
    val slot4 = WeatherState.at(960000L).weatherType
    val slot5 = WeatherState.at(1200000L).weatherType
    val types = listOf(slot0, slot1, slot2, slot3, slot4, slot5)
    // At a slot boundary the 30-second blend starts at the previous state (FOG at t=0).
    assertEquals("Trạng thái ngay ranh giới sáu slot", listOf(
      WorldWeather.FOG, WorldWeather.CLEAR, WorldWeather.CLOUDY,
      WorldWeather.RAIN, WorldWeather.STORM, WorldWeather.CLEAR,
    ), types)
    // After 30 seconds the next state must have fully arrived, not merely appear in the enum.
    val settled = (0L..5L).map { slot -> WeatherState.at(slot * 240000L + 30000L).weatherType }
    assertEquals("Thứ tự chu kỳ sau khi chuyển trạng thái", listOf(
      WorldWeather.CLEAR, WorldWeather.CLOUDY, WorldWeather.RAIN,
      WorldWeather.STORM, WorldWeather.CLEAR, WorldWeather.FOG,
    ), settled)
    assertEquals("Chu kỳ lặp lại sau 24 phút", WorldWeather.CLEAR,
      WeatherState.at(1440000L + 30000L).weatherType)
    assertEquals("Giông có mưa, không bị gộp thành RAIN", WorldWeather.STORM,
      WeatherState.at(3L * 240000L + 30000L).weatherType)
    assertTrue("Chu kỳ thời tiết phải chứa STORM (đã có: $types)", types.contains(WorldWeather.STORM))
    assertTrue("Chu kỳ thời tiết phải chứa CLEAR", types.contains(WorldWeather.CLEAR))
    assertTrue("Chu kỳ thời tiết phải chứa CLOUDY", types.contains(WorldWeather.CLOUDY))
    assertTrue("Chu kỳ thời tiết phải chứa RAIN", types.contains(WorldWeather.RAIN))
    assertTrue("Chu kỳ thời tiết phải chứa FOG", types.contains(WorldWeather.FOG))
  }

  @Test
  fun stormIsDistinctFromRainAndClear() {
    // Cùng giờ trưa, cùng camera: STORM phải khác RAIN và CLEAR bằng lớp mây, sáng, bóng.
    val noonClear = lightingAt(12f, WorldWeather.CLEAR)
    val noonRain = lightingAt(12f, WorldWeather.RAIN)
    val noonStorm = lightingAt(12f, WorldWeather.STORM)

    // STORM: ambiennt lạnh hơn RAIN và CLEAR
    assertTrue("STORM ambientIntensity <= CLEAR (storm=$noonStorm clear=$noonClear)",
      noonStorm.ambientIntensity <= noonClear.ambientIntensity)
    assertTrue("STORM ambientIntensity <= RAIN (storm=$noonStorm rain=$noonRain)",
      noonStorm.ambientIntensity <= noonRain.ambientIntensity)

    // STORM: shadowStrength mềm hơn
    assertTrue("STORM shadowStrength <= CLEAR", noonStorm.shadowStrength <= noonClear.shadowStrength)
    assertTrue("STORM shadowStrength <= RAIN", noonStorm.shadowStrength <= noonRain.shadowStrength)

    // STORM: mặt trời suy giảm mạnh hơn
    assertTrue("STORM sunIntensity <= CLEAR", noonStorm.sunIntensity <= noonClear.sunIntensity)
    assertTrue("STORM sunIntensity <= RAIN", noonStorm.sunIntensity <= noonRain.sunIntensity)

    // STORM: haze nặng hơn
    assertTrue("STORM hazeDensity >= CLEAR", noonStorm.hazeDensity >= noonClear.hazeDensity)
    assertTrue("STORM hazeDensity >= RAIN", noonStorm.hazeDensity >= noonRain.hazeDensity)

    // STORM: cloudDensity cao nhất
    assertTrue("STORM cloudDensity >= CLEAR", noonStorm.cloudDensity >= noonClear.cloudDensity)
    assertTrue("STORM cloudDensity >= RAIN", noonStorm.cloudDensity >= noonRain.cloudDensity)

    // STORM: localLightIntensity cao hơn (đèn/cổng nổi bật hơn trong bão)
    assertTrue("STORM localLightIntensity >= CLEAR", noonStorm.localLightIntensity >= noonClear.localLightIntensity)
  }

  @Test
  fun stormCloudDarkerThanClear() {
    // STORM cloudColor tối hơn CLEAR (xám-than vs màu theo giờ).
    val noonClear = lightingAt(12f, WorldWeather.CLEAR)
    val noonStorm = lightingAt(12f, WorldWeather.STORM)
    // Luminance Rec.709
    fun lum(c: androidx.compose.ui.graphics.Color): Float {
      val a = c.toArgb()
      return 0.299f * ((a shr 16) and 0xFF) + 0.587f * ((a shr 8) and 0xFF) + 0.114f * (a and 0xFF)
    }
    assertTrue("STORM cloud tối hơn CLEAR (storm=${lum(noonStorm.cloudColor)} clear=${lum(noonClear.cloudColor)})",
      lum(noonStorm.cloudColor) < lum(noonClear.cloudColor))
  }

  @Test
  fun skyBandColorsCachedNotReallocated() {
    // Clouds và sky bands không được cấp phát mỗi frame (P26C perf).
    val a = SanctuaryWorldRenderer.skyBandColors(12f, WeatherState())
    val b = SanctuaryWorldRenderer.skyBandColors(12f, WeatherState())
    assertTrue("sky band colours must be cached (no per-frame allocation)", a === b)
  }

  @Test
  fun stormSkyBandCacheKeepsWeatherTypeSeparateFromRain() {
    // At a transition both states can quantise to identical rain/cloud/fog buckets.
    // The STORM sky is colder/darker and must not inherit RAIN's cached band buffer.
    val rain = SanctuaryWorldRenderer.skyBandColors(12f,
      WeatherState(rain = 0.7f, cloud = 1f, weatherType = WorldWeather.RAIN))
    val storm = SanctuaryWorldRenderer.skyBandColors(12f,
      WeatherState(rain = 0.7f, cloud = 1f, weatherType = WorldWeather.STORM))
    assertTrue("STORM sky band cannot reuse RAIN's cache entry", rain !== storm)
    assertTrue("STORM sky must be darker than RAIN at equal precipitation", storm[0] < rain[0])
  }

  @Test
  fun lightingContinuousAcrossHour() {
    // Ánh sáng liên tục qua từng giờ (không nhảy đột ngột khi vào STORM).
    for (i in 0..2399) {
      val a = WorldLighting.at(i / 100f)
      val b = WorldLighting.at((i + 1) / 100f)
      assertTrue("ambientIntensity continuous (${i / 100f}h)",
        kotlin.math.abs(a.ambientIntensity - b.ambientIntensity) < 0.02f)
    }
  }
}
