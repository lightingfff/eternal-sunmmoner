package com.example

import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
 * P26 ROUND 1 contract test for the time-of-day / weather sky model.
 *
 * These are MODEL-level assertions (deterministic, no pixels); the pixel-level gates
 * (sky-band luminance distinctness, no-flood) are measured by
 * `tools/p26_measure_sky.py` against the real render evidence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class P26SkyContractTest {

  @Before fun setup() {
    val context = RuntimeEnvironment.getApplication()
    com.example.game.pixel.SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
  }

  @After fun cleanup() {
    WorldEnvironment.debugOverride()
    WorldEnvironment.update()
  }

  private fun luminance(rgb: Int): Float =
    (0.299f * ((rgb shr 16) and 0xFF) + 0.587f * ((rgb shr 8) and 0xFF) + 0.114f * (rgb and 0xFF))

  private fun skyMeanLuminance(hour: Float, weather: WorldWeather): Float {
    val lighting = lightingAt(hour, weather)
    // Sample the 4 anchor colors the renderer uses (zenith, mid-upper, mid, horizon),
    // scaled by the weather-applied sky brightness (the same factor the renderer bakes
    // into the cached sky bands).
    val k = lighting.skyBrightness
    return (listOf(lighting.skyColors[0], lighting.skyColors[1], lighting.skyColors[2], lighting.skyColors[3])
      .map { luminance(it.toArgb()) }.sum() / 4f) * k
  }

  private fun lightingAt(hour: Float, weather: WorldWeather): WorldLighting {
    WorldEnvironment.debugOverride(hour, weather)
    WorldEnvironment.update(50000)
    return WorldEnvironment.lighting
  }

  @Test
  fun skyStatesAreDistinctInLuminance() {
    val morning = skyMeanLuminance(7f, WorldWeather.CLEAR)
    val noon = skyMeanLuminance(12f, WorldWeather.CLEAR)
    val evening = skyMeanLuminance(18.5f, WorldWeather.CLEAR)
    val night = skyMeanLuminance(0f, WorldWeather.CLEAR)
    assertTrue("|morning-noon| >= 8 (got $morning vs $noon)", kotlin.math.abs(morning - noon) >= 8f)
    assertTrue("|noon-evening| >= 6 (got $noon vs $evening)", kotlin.math.abs(noon - evening) >= 6f)
    assertTrue("|evening-night| >= 8 (got $evening vs $night)", kotlin.math.abs(evening - night) >= 8f)
    assertTrue("|noon-night| >= 18 (got $noon vs $night)", kotlin.math.abs(noon - night) >= 18f)
    assertTrue("noon must be brightest state (noon=$noon morning=$morning)", noon >= morning)
    assertTrue("noon must be brightest state (noon=$noon evening=$evening)", noon >= evening)
    assertTrue("noon must be brightest state (noon=$noon night=$night)", noon >= night)
  }

  @Test
  fun weatherChangesSkyMood() {
    val noonClear = skyMeanLuminance(12f, WorldWeather.CLEAR)
    val noonCloudy = skyMeanLuminance(12f, WorldWeather.CLOUDY)
    val noonRain = skyMeanLuminance(12f, WorldWeather.RAIN)
    assertTrue("cloudy sky <= clear - 5 (got $noonCloudy vs $noonClear)", noonCloudy <= noonClear - 5f)
    assertTrue("rain sky <= clear - 8 (got $noonRain vs $noonClear)", noonRain <= noonClear - 8f)
    assertTrue("rain darker than cloudy (got $noonRain vs $noonCloudy)", noonRain <= noonCloudy - 1f)
  }

  @Test
  fun localLightsMatterMoreAtNight() {
    val day = WorldLighting.at(12f).localLightIntensity
    val night = WorldLighting.at(0f).localLightIntensity
    val evening = WorldLighting.at(18.5f).localLightIntensity
    assertTrue("night local lights stronger than noon (day=$day night=$night)", night > day)
    assertTrue("evening local lights stronger than noon (day=$day evening=$evening)", evening > day)
  }

  @Test
  fun skyColorsAreCachedNotReallocated() {
    // The renderer's per-frame sky band colours must come from a reused buffer:
    // two calls at the same state return the same array instance.
    val a = SanctuaryWorldRenderer.skyBandColors(12f, com.example.game.world.WeatherState())
    val b = SanctuaryWorldRenderer.skyBandColors(12f, com.example.game.world.WeatherState())
    assertTrue("sky band colours must be cached (no per-frame allocation)", a === b)
    assertEquals(12, a.size / 3) // 12 interpolated bands, RGB floats
  }

  @Test
  fun livingWorldInvariantsArePreserved() {
    // Preserved from LivingWorldTest: brightness bounded, continuous, shared shadow
    // direction with SanctuaryLight, longer evening shadows.
    for (i in 0..2399) {
      val a = WorldLighting.at(i / 100f)
      val b = WorldLighting.at((i + 1) / 100f)
      assertTrue("ambientIntensity continuous (${i / 100f}h)", kotlin.math.abs(a.ambientIntensity - b.ambientIntensity) < 0.01f)
      assertTrue("ambientIntensity >= 0.7 (${i / 100f}h)", a.ambientIntensity >= 0.7f)
      assertEquals(1f, a.directionX * a.directionX + a.directionY * a.directionY, 0.001f)
    }
    assertTrue("evening shadows longer than noon", WorldLighting.at(18.5f).shadowLength > WorldLighting.at(12f).shadowLength)
    // Direction still shares the SanctuaryLight shadow vector at 18.5h.
    WorldEnvironment.debugOverride(18.5f, WorldWeather.CLEAR)
    WorldEnvironment.update(1000)
    assertEquals(
      WorldEnvironment.lighting.directionX,
      com.example.game.map.SanctuaryLight.SHADOW_DIR_X,
      0f,
    )
    assertEquals(
      1.1106f / WorldEnvironment.lighting.shadowLength,
      com.example.game.map.SanctuaryLight.TAN_ELEV,
      0.001f,
    )
  }

  @Test
  fun celestialDiscSwitchesSunAndMoon() {
    val noon = lightingAt(12f, WorldWeather.CLEAR)
    val night = lightingAt(0f, WorldWeather.CLEAR)
    assertTrue("sun must exist at noon", noon.sunIntensity > 0.05f)
    assertTrue("sun must be warm at noon", Color.red(noon.sunColor.toArgb()) > Color.blue(noon.sunColor.toArgb()))
    assertTrue("moon must exist at night", night.moonIntensity > 0.05f)
    assertTrue("moon must be cool at night", Color.blue(night.moonColor.toArgb()) >= Color.red(night.moonColor.toArgb()))
    assertTrue("no sun above horizon at deep night", night.sunIntensity < 0.02f)
  }
}
