package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.HeroRecord
import com.example.game.lobby.*
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.ui.components.drawLobbyHero
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LobbyHeroVisualTest {
  @Test fun heldWrithingUsesExistingSpriteFramesAndRendersAtGameZoom() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    com.example.game.world.WorldEnvironment.init(RuntimeEnvironment.getApplication())
    val lobby=SanctuaryLobbyEngine();lobby.syncHeroes(listOf(HeroRecord(id="visual",name="Kaelen",currentHp=20,maxHp=100)),1000)
    val actor=lobby.heroActors.first { !it.isSanctuaryKeeperNpc }
    fun frame(name:String,time:Float,held:Boolean):Bitmap {
      val bitmap=Bitmap.createBitmap(1200,720,Bitmap.Config.ARGB_8888)
      CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(bitmap.asImageBitmap()),Size(1200f,720f)) {
        val x=actor.x;val y=actor.y
        drawSanctuaryFloatingWorld(x,y,1.48f,0f,lobby.mapLayout.paths)
        lobby.mapLayout.obstacles.sortedBy { it.y+it.sortYBias }.forEach { drawProp(it,x,y,1.48f,0f,renderShadow=null) }
        drawLobbyHero(actor,x,y,1.48f,time,false,held)
      }
      val file=File("build/visual_screenshots/lobby_hero/$name.png");file.parentFile!!.mkdirs()
      file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
      return bitmap
    }
    val ground=frame("grounded_z148",0f,false)
    lobby.beginHeroPickUp(actor.heroId)
    val heldA=frame("picked_z148_a",.05f,true)
    val heldB=frame("picked_z148_b",.25f,true)
    assertFalse(ground.sameAs(heldA));assertFalse(heldA.sameAs(heldB))
  }
}
