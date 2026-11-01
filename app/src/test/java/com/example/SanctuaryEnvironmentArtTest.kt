package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.HeroClassType
import com.example.game.map.*
import com.example.game.pixel.*
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawUpperCanopyOcclusionPass
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class SanctuaryEnvironmentArtTest {
  @org.junit.Before fun loadRuntimeAssets() {
    SanctuaryArtAssets.init(org.robolectric.RuntimeEnvironment.getApplication())
  }
  private val out = File("build/visual_screenshots/p03").apply { mkdirs() }
  @Test fun environmentDetailsAtMultipleZooms() {
    val kinds = listOf(PropKind.SAKURA_BLOSSOM_TREE, PropKind.OAK_TREE, PropKind.MYSTIC_PLUM_TREE,
      PropKind.PINE_SPIRIT_TREE, PropKind.SANCTUARY_COTTAGE_SLATE, PropKind.FACILITY_ARMORY,
      PropKind.MOSSY_BOULDER, PropKind.OBSIDIAN_COLUMN, PropKind.GARDEN_HEDGE_BUSH)
    for (kind in kinds) {
      val obs = WorldObstacle("art_$kind",1500f,1400f,24f,propKind=kind,occludesUpperCanopy=true)
      val sprite = SanctuaryEnvironmentShadows.sprite(obs)!!
      assertSame("Shadow geometry should be reused",SanctuaryEnvironmentShadows.silhouette(sprite),SanctuaryEnvironmentShadows.silhouette(sprite))
      for (zoom in listOf(0.65f,1f,1.6f)) {
        val image = Bitmap.createBitmap(720,640,Bitmap.Config.ARGB_8888).asImageBitmap()
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(720f,640f)) {
          drawRect(Color(0xff37483e))
          drawGroundShadows(listOf(obs),1500f,1280f,zoom)
          drawProp(obs,1500f,1280f,zoom,0f,renderShadow=false)
        }
        File(out,"${kind.name}_${zoom}.png").outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
      }
    }
  }
  @Test fun terrainCloseupsInRuntimeRenderer() {
    val layout = com.example.game.lobby.SanctuaryLobbyEngine().mapLayout
    for ((name,position) in listOf("grass" to (2650f to 1450f), "plaza" to (1940f to 1240f), "soil_transition" to (1230f to 1770f))) {
      for (zoom in listOf(0.8f,1.6f)) {
        val image = Bitmap.createBitmap(720,640,Bitmap.Config.ARGB_8888).asImageBitmap()
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(720f,640f)) {
          with(SanctuaryWorldRenderer) { drawSanctuaryFloatingWorld(position.first,position.second,zoom,0f,layout.paths) }
        }
        File(out,"ground_${name}_$zoom.png").outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
      }
    }
  }
  @Test fun characterBehindCanopy() {
    val obs = WorldObstacle("occlusion",1500f,1400f,24f,propKind=PropKind.SAKURA_BLOSSOM_TREE,occludesUpperCanopy=true)
    val images = mutableListOf<ImageBitmap>()
    for (withCanopy in listOf(false,true)) {
      val image = Bitmap.createBitmap(640,640,Bitmap.Config.ARGB_8888).asImageBitmap()
      CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(640f,640f)) {
        drawRect(Color(0xff37483e))
        drawGroundShadows(listOf(obs),1500f,1280f,1.6f)
        drawProp(obs,1500f,1280f,1.6f,0f,renderShadow=false)
        // Place hero at the projected crown to verify that the foreground canopy masks him.
        drawCharacterSprite(1500f,1250f,1500f,1280f,1.6f,false,HeroClassType.KNIGHT,true,false,0f,0f,1f,null,false,0f)
        if (withCanopy) drawUpperCanopyOcclusionPass(listOf(obs),1500f,1280f,1.6f,0f)
      }
      images.add(image)
      File(out,"occlusion_${withCanopy}.png").outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    val a = IntArray(640*640); val b = IntArray(a.size)
    images[0].asAndroidBitmap().getPixels(a,0,640,0,0,640,640)
    images[1].asAndroidBitmap().getPixels(b,0,640,0,0,640,640)
    assertTrue("Canopy foreground must affect pixels over the actor",a.indices.count { a[it] != b[it] } > 50)
  }
}
