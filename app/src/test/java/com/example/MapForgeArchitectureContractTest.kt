package com.example

import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.game.pixel.PremiumActorAtlas
import com.example.game.pixel.SanctuaryArtAssets
import java.security.MessageDigest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P20 asset contract for the architecture / vegetation slice.
 *
 * Proves the two repainted sprites keep the exact canvas the renderer's anchors, door box and
 * canopy-occlusion fraction depend on, that they are still opaque art (not a blank canvas),
 * and that the packaged bytes match the manifest pins.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapForgeArchitectureContractTest {

  private fun prepare() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
  }

  @Test
  fun armoryKeepsCanvasDoorBoxAndManifestPin() {
    prepare()
    val armory = SanctuaryArtAssets.asset("premium_slice/armory.png")
    assertNotNull("armory sprite must load", armory)
    val bmp = armory!!.asAndroidBitmap()
    assertEquals("armory width (drawCrispSprite anchors on this)", 190, bmp.width)
    assertEquals("armory height", 166, bmp.height)

    // The doorway the world trigger (993, 673) lands on must stay a real opening: a dark,
    // opaque, recessed box at x 133..150, y 109..139 with a lit threshold below it.
    var opaque = 0
    var darkDoorPixels = 0
    for (y in 0 until bmp.height) for (x in 0 until bmp.width) {
      val p = bmp.getPixel(x, y)
      if ((p ushr 24) != 0) opaque++
      if (x in 133..149 && y in 109..139) {
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        if ((p ushr 24) != 0 && r < 110 && g < 90) darkDoorPixels++
      }
    }
    assertTrue("sprite must be filled in, not a blank canvas ($opaque px)", opaque > 12_000)
    assertTrue("door box must stay a recessed dark opening ($darkDoorPixels px)",
      darkDoorPixels > 300)

    val manifest = JSONObject(SanctuaryArtAssets.textAsset("premium_slice/manifest.json")!!)
    val entry = manifest.getJSONObject("assets").getJSONObject("armory")
    val bytes = RuntimeEnvironment.getApplication().assets
      .open("premium_slice/${entry.getString("file")}").use { it.readBytes() }
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    assertEquals("armory SHA-256 vs manifest", entry.getString("sha256"), digest)
  }

  @Test
  fun cypressKeepsCanvasAnchorAndIsRealFoliage() {
    prepare()
    val cypress = SanctuaryArtAssets.image("cypress")
    assertNotNull("cypress sprite must load", cypress)
    val bmp = cypress!!.asAndroidBitmap()
    assertEquals("cypress width", 48, bmp.width)
    assertEquals("cypress height", 88, bmp.height)

    // Canopy occlusion splits the sprite at topFraction 0.54: the canopy above that line must
    // be dense foliage and the trunk band below it must exist but be much thinner.
    var upper = 0
    var lowerOpaque = 0
    val split = (bmp.height * 0.54f).toInt()
    for (y in 0 until bmp.height) for (x in 0 until bmp.width) {
      if ((bmp.getPixel(x, y) ushr 24) == 0) continue
      if (y < split) upper++ else lowerOpaque++
    }
    assertTrue("canopy band must be dense ($upper px)", upper > 900)
    assertTrue("trunk band must be present but sparse ($lowerOpaque px)", lowerOpaque in 60..900)

    val manifest = JSONObject(SanctuaryArtAssets.textAsset("sanctuary_p03/manifest.json")!!)
    val entries = manifest.getJSONArray("assets")
    val entry = (0 until entries.length()).map { entries.getJSONObject(it) }
      .first { it.getString("id") == "cypress" }
    assertEquals("cypress ground anchor x", 24, entry.getJSONArray("ground_anchor").getInt(0))
    assertEquals("cypress ground anchor y", 83, entry.getJSONArray("ground_anchor").getInt(1))
    val bytes = RuntimeEnvironment.getApplication().assets
      .open("sanctuary_p03/${entry.getString("path")}").use { it.readBytes() }
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    assertEquals("cypress SHA-256 vs manifest", entry.getString("sha256"), digest)
  }

  /** P19 terrain and the Lyra sheet must be untouched by the P20 sprite work. */
  @Test
  fun priorSlicesStillLoad() {
    prepare()
    assertEquals(8, SanctuaryArtAssets.tiles("grass", 8)!!.size)
    assertEquals(6, SanctuaryArtAssets.tiles("soil", 6)!!.size)
    assertEquals(4, SanctuaryArtAssets.tiles("gravel", 4)!!.size)
    assertNotNull("Lyra sheet still resolves", PremiumActorAtlas.get("lyra"))
  }
}
