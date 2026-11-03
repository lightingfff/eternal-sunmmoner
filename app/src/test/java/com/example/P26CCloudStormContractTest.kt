package com.example

import com.example.game.pixel.SanctuaryWorldRenderer
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldLighting
import com.example.game.world.WorldWeather
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
 * render evidence (docs/visual-evidence/p26c/) and tools/p26_measure_sky.py.
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
    val slot0 = WorldWeather.WeatherState.at(0L).weatherType
    val slot1 = WorldWeather.WeatherState.at(240000L).weatherType
    val slot2 = WorldWeather.WeatherState.at(480000L).weatherType
    val slot3 = WorldWeather.WeatherState.at(720000L).weatherType
    val slot4 = WorldWeather.WeatherState.at(960000L).weatherType
    val slot5 = WorldWeather.WeatherState.at(1200000L).weatherType
    val types = listOf(slot0, slot1, slot2, slot3, slot4, slot5)
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
    val a = SanctuaryWorldRenderer.skyBandColors(12f, com.example.game.world.WeatherState())
    val b = SanctuaryWorldRenderer.skyBandColors(12f, com.example.game.world.WeatherState())
    assertTrue("sky band colours must be cached (no per-frame allocation)", a === b)
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
