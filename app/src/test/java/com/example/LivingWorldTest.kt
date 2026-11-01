package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.*
import com.example.game.world.*
import com.example.game.pixel.*
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.PixelSpriteRenderer.drawUpperCanopyOcclusionPass
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.LivingWorldRenderer.drawWorldGrade
import com.example.game.pixel.LivingWorldRenderer.drawWeather
import com.example.game.pixel.LivingWorldRenderer.drawLocalLights
import com.example.data.model.HeroClassType
import java.io.File
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LivingWorldTest {
  @Before fun setup() {
    val context=org.robolectric.RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context);WorldEnvironment.init(context)
  }
  @After fun cleanup() { WorldEnvironment.debugOverride();WorldEnvironment.update() }
  @Test fun clockProgressesIndependentlyOfFramesAndWraps() {
    val clock=WorldClock(1000)
    assertEquals(0f,clock.hour(1000),.001f)
    assertEquals(12f,clock.hour(721000),.001f)
    assertEquals(0f,clock.hour(1441000),.001f)
    assertEquals(23f,WorldClock(0,24000).hour(-1000),.001f)
  }
  @Test fun originAndDurationSurviveReload() {
    val context=org.robolectric.RuntimeEnvironment.getApplication()
    val prefs=context.getSharedPreferences("sanctuary_world",0)
    prefs.edit().putLong("origin_millis",1000).putLong("day_duration_millis",24000).commit()
    WorldEnvironment.init(context);WorldEnvironment.update(13000)
    assertEquals(12f,WorldEnvironment.lighting.hour,.001f)
    WorldEnvironment.init(context);WorldEnvironment.update(14000)
    assertEquals(13f,WorldEnvironment.lighting.hour,.001f)
  }
  @Test fun lightingIsContinuousReadableAndSharesShadowDirection() {
    for(i in 0..2399) {
      val a=WorldLighting.at(i/100f);val b=WorldLighting.at((i+1)/100f)
      assertTrue(kotlin.math.abs(a.ambientIntensity-b.ambientIntensity)<.01f)
      assertTrue(a.ambientIntensity>=.7f)
      assertEquals(1f,a.directionX*a.directionX+a.directionY*a.directionY,.001f)
    }
    assertTrue(WorldLighting.at(18.5f).shadowLength>WorldLighting.at(12f).shadowLength)
    WorldEnvironment.debugOverride(18.5f,WorldWeather.CLEAR);WorldEnvironment.update(1000)
    assertEquals(WorldEnvironment.lighting.directionX,com.example.game.map.SanctuaryLight.SHADOW_DIR_X,0f)
    assertEquals(1.1106f/WorldEnvironment.lighting.shadowLength,com.example.game.map.SanctuaryLight.TAN_ELEV,.001f)
    assertTrue(WorldLighting.at(0f).localLightIntensity>WorldLighting.at(12f).localLightIntensity)
  }
  @Test fun weatherTransitionsAreSmoothAndFrameIndependent() {
    val start=WeatherState.blend(WorldWeather.CLEAR,WorldWeather.RAIN,0f)
    val middle=WeatherState.blend(WorldWeather.CLEAR,WorldWeather.RAIN,.5f)
    val end=WeatherState.blend(WorldWeather.CLEAR,WorldWeather.RAIN,1f)
    assertEquals(0f,start.rain,0f);assertEquals(.5f,middle.rain,.001f);assertEquals(1f,end.rain,0f)
    assertEquals(WeatherState.at(510000),WeatherState.at(510000))
    assertTrue(kotlin.math.abs(WeatherState.at(480000).rain-WeatherState.at(479999).rain)<.001f)
    WorldEnvironment.debugOverride(0f,WorldWeather.RAIN);WorldEnvironment.update(1000,true)
    assertEquals(0f,WorldEnvironment.weather.rain,0f)
  }
  @Test fun captureLivingSanctuaryRuntimeStates() {
    val out=File("build/visual_screenshots/p1").apply { mkdirs() }
    val layout=com.example.game.lobby.SanctuaryLobbyEngine().mapLayout
    val modes=listOf(Triple("midday",12f,WorldWeather.CLEAR),Triple("sunset",18.5f,WorldWeather.CLEAR),Triple("night",0f,WorldWeather.CLEAR),Triple("rain",14f,WorldWeather.RAIN),Triple("fog",7f,WorldWeather.FOG))
    for((name,hour,weather) in modes) {
      WorldEnvironment.debugOverride(hour,weather);WorldEnvironment.update(50000)
      val image=Bitmap.createBitmap(1280,960,Bitmap.Config.ARGB_8888).asImageBitmap()
      CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(1280f,960f)) {
        val cx=1530f;val cy=1220f;val z=.75f
        drawSanctuaryFloatingWorld(cx,cy,z,0f,layout.paths)
        drawWeather(cx,cy,z,false)
        drawGroundShadows(layout.obstacles,cx,cy,z)
        var hero=false
        for(obs in layout.obstacles.sortedBy { it.y+it.sortYBias }) {
          if(!hero && obs.y>1240) {
            drawCharacterSprite(1940f,1240f,cx,cy,z,false,HeroClassType.KNIGHT,true,false,0f,0f,1f,null,false,0f)
            hero=true
          }
          drawProp(obs,cx,cy,z,0f,renderShadow=SanctuaryEnvironmentShadows.sprite(obs)==null)
        }
        drawUpperCanopyOcclusionPass(layout.obstacles,cx,cy,z,0f)
        drawWorldGrade();drawLocalLights(layout.obstacles,cx,cy,z);drawWeather(cx,cy,z,true)
      }
      File(out,"$name.png").outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
  }
}
