package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.game.pixel.PixelTileAtlas
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class SanctuaryMaterialTest {
  @org.junit.Before fun loadRuntimePngs() {
    com.example.game.pixel.SanctuaryArtAssets.init(org.robolectric.RuntimeEnvironment.getApplication())
  }
  @Test fun manifestAssetsDecodeAndMatchRuntimeAtlas() {
    val context = org.robolectric.RuntimeEnvironment.getApplication()
    val manifest = context.assets.open("sanctuary_p03/manifest.json").bufferedReader().use { org.json.JSONObject(it.readText()) }
    val assets = manifest.getJSONArray("assets")
    for (i in 0 until assets.length()) {
      val asset = assets.getJSONObject(i)
      val bytes = context.assets.open("sanctuary_p03/" + asset.getString("path")).use { it.readBytes() }
      val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
      org.junit.Assert.assertEquals(asset.getString("sha256"), digest)
      val bitmap = com.example.game.pixel.SanctuaryArtAssets.image(asset.getString("id"))!!
      org.junit.Assert.assertEquals(asset.getJSONArray("size").getInt(0),bitmap.width)
      org.junit.Assert.assertEquals(asset.getJSONArray("size").getInt(1),bitmap.height)
    }
    org.junit.Assert.assertSame(com.example.game.pixel.SanctuaryArtAssets.image("grass_0"), PixelTileAtlas.sanctuaryGrassTiles[0])
    org.junit.Assert.assertSame(com.example.game.pixel.SanctuaryArtAssets.image("boulder"), com.example.game.pixel.PixelStructureAtlas.mossyBoulderSprite)
  }
  @Test fun materialRepeatSheets() {
    val materials = listOf(
      "grass" to PixelTileAtlas.sanctuaryGrassTiles,
      "soil" to PixelTileAtlas.sanctuarySandTiles,
      "flagstone" to PixelTileAtlas.sanctuaryStoneTiles,
      "cobble" to PixelTileAtlas.sanctuaryCausewayTiles,
      "gravel" to PixelTileAtlas.sanctuaryGravelTiles,
      "wood" to PixelTileAtlas.tavernWoodPlankTiles,
    )
    val out = File("build/visual_screenshots/materials").apply { mkdirs() }
    for ((name, tiles) in materials) {
      for (repeatSize in listOf(3,5)) {
      val side = repeatSize * 64
      val sheet = Bitmap.createBitmap(tiles.size * side, side, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(sheet)
      for ((variant, tile) in tiles.withIndex()) {
        val bitmap = tile.asAndroidBitmap()
        val colors = IntArray(4096)
        bitmap.getPixels(colors, 0, 64, 0, 0, 64, 64)
        assertTrue("$name must fully cover its receiving surface", colors.all { it ushr 24 == 255 })
        assertTrue("$name needs readable material planes", colors.toSet().size >= 4)
        val reference = tiles[0].asAndroidBitmap()
        for (edge in 0 until 64) {
          for ((px,py) in listOf(0 to edge, 63 to edge, edge to 0, edge to 63)) {
            org.junit.Assert.assertEquals("$name must share edge motifs between variants",reference.getPixel(px,py),bitmap.getPixel(px,py))
          }
        }
        for (y in 0 until repeatSize) for (x in 0 until repeatSize) canvas.drawBitmap(bitmap, (variant * side + x * 64).toFloat(), (y * 64).toFloat(), Paint())
      }
      File(out, "$name-${repeatSize}x$repeatSize.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
      }
      val mixed = Bitmap.createBitmap(320,320,Bitmap.Config.ARGB_8888)
      val mixedCanvas = Canvas(mixed)
      for (y in 0..4) for (x in 0..4) {
        val index = ((x * 73856093 xor y * 19349663) and 0x7fffffff) % tiles.size
        mixedCanvas.drawBitmap(tiles[index].asAndroidBitmap(),x*64f,y*64f,Paint())
      }
      File(out,"$name-mixed-5x5.png").outputStream().use { mixed.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
  }
}
