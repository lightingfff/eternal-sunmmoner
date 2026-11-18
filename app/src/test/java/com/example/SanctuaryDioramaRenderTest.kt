package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.map.GroundView
import com.example.game.pixel.LivingWorldRenderer.drawWorldGrade
import com.example.game.pixel.PixelSpriteRenderer.drawActorCanopyOcclusion
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.PixelSpriteRenderer.drawSouthernForegroundWalls
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryDioramaLight.drawDioramaLight
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.pixel.withGroundView
import com.example.data.model.HeroClassType
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SanctuaryDioramaRenderTest {
  private data class Shot(val name: String, val x: Float, val y: Float, val zoom: Float, val hour: Float)

  @Test
  fun rendersPlayableIslandGeometryAtThreeZooms() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    val layout = SanctuaryLobbyEngine().mapLayout
    val tree = layout.obstacles.single { it.id == "sanctuary_sakura_14" }
    val actorX = tree.x + 37f
    val actorY = tree.y - 56f
    val shots = listOf(
      Shot("overview", 1600f, 1380f, .36f, 12f),
      Shot("residential", 1010f, 880f, .88f, 12f),
      Shot("canopy", tree.x, tree.y - 140f, 1.48f, 12f),
      Shot("residential-night", 1010f, 880f, .88f, 22f),
    )
    try {
      for (shot in shots) {
        WorldEnvironment.debugOverride(shot.hour, WorldWeather.CLEAR)
        WorldEnvironment.update(50_000)
        val bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(1280f, 720f)) {
          withGroundView(GroundView.SANCTUARY_DIORAMA) {
            drawSanctuaryFloatingWorld(shot.x, shot.y, shot.zoom, 1.25f, layout.paths)
            drawGroundShadows(layout.obstacles, shot.x, shot.y, shot.zoom)
            val orderedProps = layout.obstacles.sortedBy { it.y + it.sortYBias }
            val (behind, inFront) = orderedProps.partition { it.y + it.sortYBias <= actorY }
            behind.forEach { drawProp(it, shot.x, shot.y, shot.zoom, 1.25f, renderShadow = null) }
            drawCharacterSprite(
              actorX, actorY, shot.x, shot.y, shot.zoom, false, HeroClassType.KNIGHT,
              facingRight = true, isMoving = false, attackAnimProgress = 0f, hurtFlash = 0f,
              hpFraction = 1f, shieldOrAuraColor = null, isInjuredResting = false, timeSec = 1.25f,
              heroName = "Kaelen",
            )
            drawActorCanopyOcclusion(layout.obstacles, actorX, actorY, shot.x, shot.y, shot.zoom, 1.25f)
            inFront.forEach { drawProp(it, shot.x, shot.y, shot.zoom, 1.25f, renderShadow = null) }
            drawSouthernForegroundWalls(layout.biome, shot.x, shot.y, shot.zoom)
            drawWorldGrade()
            drawDioramaLight()
          }
        }
        val output = File("build/visual_screenshots/sanctuary-diorama/${shot.name}.png")
        output.parentFile?.mkdirs()
        output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        assertTrue(output.length() > 20_000)
      }
    } finally {
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
  }

  @Test
  fun groundProjectionPreservesElevationAndRoundTripsThroughGestureCoordinates() {
    val exterior = GroundView.forSanctuary(indoors = false)
    val interior = GroundView.forSanctuary(indoors = true)
    val zoom = 1.48f
    val cameraY = 1010f
    val halfHeight = 360f
    val worldY = 880f
    val screen = exterior.projectY(worldY, cameraY, zoom, halfHeight)
    assertEquals(worldY, exterior.unprojectY(screen, cameraY, zoom, halfHeight), .001f)
    assertEquals(60f * zoom, screen - exterior.projectY(worldY, cameraY, zoom, halfHeight, 60f), .001f)
    assertTrue(kotlin.math.abs(screen - halfHeight) < kotlin.math.abs(interior.projectY(worldY, cameraY, zoom, halfHeight) - halfHeight))
  }
}
