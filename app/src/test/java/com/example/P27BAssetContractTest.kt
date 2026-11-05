package com.example

import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.FacilityType
import com.example.game.pixel.PixelSpriteRenderer.drawSanctuaryZoneFloorRing
import com.example.game.pixel.PixelStructureAtlas
import com.example.game.pixel.PixelTileAtlas
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryVisualProfile
import com.example.game.tower.TowerFloorCatalog
import java.security.MessageDigest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Manifest bytes/alpha + the *actual* cached runtime selectors, not a path-string test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class P27BAssetContractTest {
  @Test fun packagedOriginalsAndFallbacksLoadWithUnchangedFootprints() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    assertEquals(SanctuaryVisualProfile.P27B, SanctuaryArtAssets.visualProfile)
    val json = JSONObject(SanctuaryArtAssets.textAsset("sanctuary_p27b/manifest.json")!!)
    assertEquals(0.72, json.getDouble("camera_ground_y_scale"), 0.0001)
    assertEquals(64, json.getInt("tile_px"))
    val assets = json.getJSONArray("assets")
    assertEquals(82, assets.length())
    val digest = MessageDigest.getInstance("SHA-256")
    val familyCounts = mutableMapOf<String, Int>()
    for (i in 0 until assets.length()) {
      val entry = assets.getJSONObject(i)
      val family = entry.getString("family")
      familyCounts[family] = (familyCounts[family] ?: 0) + 1
      val bytes = context.assets.open("sanctuary_p27b/${entry.getString("path")}").use { it.readBytes() }
      val sha = digest.digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
      assertEquals(entry.getString("id"), entry.getString("sha256"), sha)
      val bmp = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) {
        "${entry.getString("id")} must decode"
      }
      val expected = entry.getJSONArray("size")
      assertEquals(expected.getInt(0), bmp.width)
      assertEquals(expected.getInt(1), bmp.height)
      val pixels = IntArray(bmp.width*bmp.height)
      bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
      if (family == "structure") {
        assertTrue("${entry.getString("id")}: transparent exterior", pixels.count { it ushr 24 == 0 } > pixels.size / 8)
        assertTrue("${entry.getString("id")}: opaque building", pixels.count { it ushr 24 == 255 } > pixels.size / 3)
      } else {
        assertEquals(64, bmp.width)
        assertEquals(64, bmp.height)
        assertTrue("${entry.getString("id")}: base is opaque", pixels.all { it ushr 24 == 255 })
      }
    }
    for (name in listOf("grass", "soil", "flagstone", "cobble", "summon_ivory")) {
      assertEquals("$name is a complete 4x4 mosaic", 16, familyCounts[name])
      assertEquals(16, SanctuaryArtAssets.p27bFamily(name)!!.size)
    }
    assertEquals(2, familyCounts["structure"])
    assertSame(SanctuaryArtAssets.assetSafe("sanctuary_p27b/terrain/grass_0.png"), PixelTileAtlas.sanctuaryGrassTiles[0])
    assertSame(SanctuaryArtAssets.assetSafe("sanctuary_p27b/terrain/cobble_0.png"), PixelTileAtlas.sanctuaryCausewayTiles[0])
    assertSame(SanctuaryArtAssets.assetSafe("sanctuary_p27b/structures/summoning_hall.png"),
      SanctuaryArtAssets.preferredLandmark("summoning_hall",268,206))
    assertSame(SanctuaryArtAssets.assetSafe("sanctuary_p27b/structures/tower_gate.png"),
      SanctuaryArtAssets.preferredLandmark("tower_gate",260,224))
    // Candidate absent -> premium_slice still loads; negative family is cached, not reopened per frame.
    assertSame(SanctuaryArtAssets.assetSafe("premium_slice/armory.png"),
      SanctuaryArtAssets.preferredLandmark("armory",190,166))
    assertNull(SanctuaryArtAssets.p27bFamily("__missing_family__"))
    assertEquals(8, SanctuaryArtAssets.tiles("grass", 8)!!.size)
    assertEquals(268 to 206, PixelStructureAtlas.summoningHallStructure.let { it.width to it.height })
    assertEquals(260 to 224, PixelStructureAtlas.towerGateMonument.let { it.width to it.height })
    val riftPixel = PixelStructureAtlas.towerGateMonument.asAndroidBitmap().getPixel(130, 120)
    assertTrue("dynamic rift must still sit over an authored dark recess", (riftPixel shr 16 and 255) < 55 &&
      (riftPixel shr 8 and 255) < 75 && (riftPixel and 255) < 85)
  }

  @Test fun adjacentMosaicIndicesAndOverviewUseTheSameWorldCoords() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    val grass = PixelTileAtlas.sanctuaryGrassTiles
    assertEquals(16, grass.size)
    for (ty in -8..8) for (tx in -8..8) {
      val index = PixelTileAtlas.sanctuaryTileIndex(grass, tx, ty, 517)
      assertEquals(((ty and 3) shl 2) or (tx and 3), index)
    }
    assertEquals(1, PixelTileAtlas.sanctuaryGrassBlocks.size)
    val block = PixelTileAtlas.sanctuaryGrassBlocks[0].asAndroidBitmap()
    assertEquals(256, block.width)
    assertEquals(184, block.height)
    // The overview block is only valid if it is the same art the per-tile path draws:
    // compare every 4x4 cell against its world-aligned mosaic tile, not just its size.
    val cellW = 64
    val cellH = (64 * 0.72f).toInt()
    for (cy in 0 until 4) for (cx in 0 until 4) {
      val tile = grass[((cy and 3) shl 2) or (cx and 3)].asAndroidBitmap()
      for (y in 0 until cellH) for (x in 0 until cellW) {
        val src = tile.getPixel((x * 64) / cellW, (y * 64) / cellH)
        val dst = block.getPixel(cx * cellW + x, cy * cellH + y)
        if (src != dst) fail("grass block cell ($cx,$cy) pixel ($x,$y) diverges from mosaic tile")
      }
    }
  }

  /** Ivory ground must never be painted as a second surface over the authored hall apron. */
  @Test fun zoneFloorRingStaysGroundNeutral() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    // Shipped and validated, but deliberately not painted by the zone ring: the hall sprite
    // already bakes its own limestone apron (see PixelSpriteRenderer history in P27B).
    requireNotNull(PixelTileAtlas.summoningIvoryTiles) { "P27B ivory family must stay packaged" }
    val width = 1280
    val height = 720
    val zones = TowerFloorCatalog.SANCTUARY_ZONES
    val summon = zones.first { it.facility == FacilityType.SUMMONING_HALL }
    fun renderOnly(target: FacilityType): android.graphics.Bitmap {
      val image = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        .asImageBitmap()
      CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat())) {
        zones.filter { it.facility == target }.forEach { zone ->
          drawSanctuaryZoneFloorRing(zone, false, summon.centerX, summon.centerY, 1.48f, 0f)
        }
      }
      return image.asAndroidBitmap()
    }
    // Fully opaque only: a glow blended onto a transparent canvas has partial alpha, so
    // counting every non-zero pixel would flag the ring's own antialiasing.
    fun fullyOpaque(bmp: android.graphics.Bitmap): Int {
      val pixels = IntArray(bmp.width * bmp.height)
      bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
      return pixels.count { it ushr 24 == 255 }
    }
    // The ring is translucent glow only. A large opaque footprint would mean a floor layer
    // came back and is covering the sprite apron / grass again.
    for (zone in zones) {
      val filled = fullyOpaque(renderOnly(zone.facility))
      assertTrue("${zone.facility}: zone ring must stay translucent, got $filled fully opaque px", filled == 0)
      val buffer = IntArray(width * height)
      renderOnly(zone.facility).getPixels(buffer, 0, width, 0, 0, width, height)
      val maxAlpha = buffer.maxOf { it ushr 24 }
      assertTrue("${zone.facility}: ring alpha stays a glow (max $maxAlpha)", maxAlpha < 200)
    }
  }
}
