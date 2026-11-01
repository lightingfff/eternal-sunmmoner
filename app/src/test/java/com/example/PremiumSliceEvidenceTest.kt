package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.*
import com.example.data.model.HeroClassType
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.pixel.*
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.world.*
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

/** Native Android Canvas evidence, not a physical-device or FPS benchmark. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PremiumSliceEvidenceTest {
  @Test fun captureSameSanctuaryScene() {
    val context = org.robolectric.RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(12f, WorldWeather.CLEAR)
    WorldEnvironment.update(50000)
    val layout = SanctuaryLobbyEngine().mapLayout
    val out = File("build/visual_screenshots/p15a").apply { mkdirs() }
    try {
      for ((name, camera) in listOf("sample_normal" to Triple(1070f,800f,1f),
        "sample_close" to Triple(950f,1000f,1.8f), "architecture" to Triple(995f,560f,1.3f),
        "overview" to Triple(1530f,1220f,.45f))) {
        val (cx, cy, z) = camera
        val image = Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888).asImageBitmap()
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(1280f,720f)) {
          drawSanctuaryFloatingWorld(cx,cy,z,0f,layout.paths)
          drawGroundShadows(layout.obstacles,cx,cy,z)
          var drawn = false
          for (obs in layout.obstacles.sortedBy { it.y + it.sortYBias }) {
            if (!drawn && obs.y + obs.sortYBias > 1020f) {
              drawCharacterSprite(950f,1020f,cx,cy,z,false,HeroClassType.KNIGHT,true,false,0f,0f,1f,null,false,0f,heroName="Kaelen")
              drawn = true
            }
            drawProp(obs,cx,cy,z,0f,renderShadow = null)
          }
          with(PixelSpriteRenderer) { drawUpperCanopyOcclusionPass(layout.obstacles,cx,cy,z,0f) }
          with(LivingWorldRenderer) { drawWorldGrade(); drawLocalLights(layout.obstacles,cx,cy,z) }
        }
        File(out,"$name.png").outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
      }
    } finally { WorldEnvironment.debugOverride(); WorldEnvironment.update() }
  }
}
