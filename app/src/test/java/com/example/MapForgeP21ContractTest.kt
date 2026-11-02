package com.example

import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.data.model.FacilityType
import com.example.game.map.PropKind
import com.example.game.map.SanctuaryHouseEntrances
import com.example.game.pixel.PixelSanctuaryLandmarks
import com.example.game.pixel.PixelStructureAtlas
import com.example.game.pixel.PremiumActorAtlas
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.tower.TowerFloorCatalog
import java.security.MessageDigest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P21 asset + geometry contract for the six authored Sanctuary facilities.
 *
 * This test is deliberately arithmetic rather than decorative: it re-derives the sprite-space
 * door box from the *world* contract (obstacle anchor, renderer pixel scale, entrance trigger)
 * and asserts the painted door really lands there. That is the failure the P20 slice found on
 * the Armory (its door sat ~76 world units right and ~53 south of its trigger), so every
 * facility is checked separately — Armory coordinates are never reused for another building.
 *
 * Sprite -> world for a structure blitted bottom-centred at `cx, cy + 6s` with pixelScale `2s`:
 *   worldX = obs.x + (spriteX - W/2) * 2 * scale
 *   worldY = obs.y - ((H-3) - spriteY) * (2 * scale) / OBLIQUE_Y_SCALE(0.72)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapForgeP21ContractTest {

  private fun prepare() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
  }

  private data class DoorCase(val key: String, val obstacleId: String, val w: Int, val h: Int)

  private data class ExpectedObstacle(
    val id: String, val x: Float, val y: Float, val scale: Float,
    val halfW: Float, val depth: Float, val prop: PropKind,
  )

  private fun obstacles() = com.example.game.lobby.SanctuaryLobbyEngine().mapLayout.obstacles

  private fun obstacle(id: String) = obstacles().firstOrNull { it.id == id }

  private fun bitmap(key: String) =
    requireNotNull(SanctuaryArtAssets.assetSafe("premium_slice/$key.png")) { "$key.png must load" }

  private fun worldOf(obsX: Float, obsY: Float, w: Int, h: Int, scale: Float, spriteX: Float, spriteY: Float): Pair<Float, Float> {
    val wx = obsX + (spriteX - w / 2f) * 2f * scale
    val wy = obsY - ((h - 3f) - spriteY) * (2f * scale) / 0.72f
    return wx to wy
  }

  @Test
  fun everyAuthoredFacilityKeepsItsCanvasAndLoads() {
    prepare()
    // Canvas W/H is the renderer's anchor: it decides the bottom-centred blit and the cast
    // shadow silhouette. Changing one without an explicit mapping test is a contract break.
    fun assertCanvas(key: String, w: Int, h: Int) {
      val bmp = bitmap(key).asAndroidBitmap()
      assertEquals("$key width", w, bmp.width)
      assertEquals("$key height", h, bmp.height)
      var opaque = 0
      for (y in 0 until bmp.height) for (x in 0 until bmp.width) {
        if ((bmp.getPixel(x, y) ushr 24) != 0) opaque++
      }
      assertTrue("$key must be filled in, not a blank canvas ($opaque px)", opaque > w * h * 0.28)
      // A clean transparent margin keeps neighbouring ground tiles from bleeding.
      for (x in 0 until bmp.width) assertEquals("$key top row must be transparent", 0, bmp.getPixel(x, 0) ushr 24)
      for (y in 0 until bmp.height) {
        assertEquals("$key left column must be transparent", 0, bmp.getPixel(0, y) ushr 24)
        assertEquals("$key right column must be transparent", 0, bmp.getPixel(bmp.width - 1, y) ushr 24)
      }
    }
    assertCanvas("summoning_hall", 268, 206)
    assertCanvas("tower_gate", 260, 224)
    assertCanvas("hero_registry", 188, 166)
    assertCanvas("hero_quarters", 200, 168)
    assertCanvas("restaurant", 206, 172)
    assertCanvas("synthesis_lab", 188, 168)
  }

  @Test
  fun premiumManifestPinsMatchPackagedBytes() {
    prepare()
    val manifest = JSONObject(SanctuaryArtAssets.textAsset("premium_slice/manifest.json")!!)
    val assets = manifest.getJSONObject("assets")
    for (key in listOf("summoning_hall", "tower_gate", "hero_registry", "hero_quarters",
      "restaurant", "synthesis_lab", "armory", "smithy_verge")) {
      val entry = assets.getJSONObject(key)
      val bytes = RuntimeEnvironment.getApplication().assets
        .open("premium_slice/${entry.getString("file")}").use { it.readBytes() }
      val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }
      assertEquals("$key SHA-256 vs manifest", entry.getString("sha256"), digest)
      val size = entry.getJSONArray("size")
      assertEquals("$key manifest width", size.getInt(0), bitmap(key).asAndroidBitmap().width)
      assertEquals("$key manifest height", size.getInt(1), bitmap(key).asAndroidBitmap().height)
    }
  }

  /** P20 armory + cypress must survive the P21 pass untouched. */
  @Test
  fun p20SliceIsIntact() {
    prepare()
    val armory = bitmap("armory").asAndroidBitmap()
    assertEquals(190, armory.width)
    assertEquals(166, armory.height)
    val cypress = SanctuaryArtAssets.image("cypress")
    assertNotNull("cypress sprite must still resolve", cypress)
    assertEquals(48, cypress!!.asAndroidBitmap().width)
    assertEquals(88, cypress.asAndroidBitmap().height)
    assertEquals(8, SanctuaryArtAssets.tiles("grass", 8)!!.size)
    assertNotNull("Lyra sheet still resolves", PremiumActorAtlas.get("lyra"))
  }

  /**
   * The enterable facilities' painted door must invert back onto its SanctuaryHouseEntrances
   * trigger: door centre on canvas centre (x == obstacle.x - 2 at scale 0.8) and the leaf's
   * bottom row on H-3 (doorWorldY == obstacle.y). Each door is probed as a dark opening
   * flanked by a lit stone surround, so a repaint cannot silently move it.
   */
  @Test
  fun enterableDoorsLandOnTheirWorldTriggers() {
    prepare()
    val cases = listOf(
      DoorCase("hero_quarters", "building_hero_quarters", 200, 168),
      DoorCase("restaurant", "building_restaurant", 206, 172),
    )
    val entrances = SanctuaryHouseEntrances.ENTRIES
    val facilityByObstacle = entrances.entries.associate { it.value.obstacleId to it.value }
    for (case in cases) {
      val obs = requireNotNull(obstacle(case.obstacleId)) { "${case.obstacleId} missing" }
      val entrance = requireNotNull(facilityByObstacle[case.obstacleId]) {
        "${case.obstacleId} must keep a SanctuaryHouseEntrance"
      }
      val bmp = bitmap(case.key).asAndroidBitmap()
      // Inverse mapping: which sprite pixel does the trigger land on?
      val px = case.w / 2f + (entrance.doorWorldX - obs.x) / (2f * obs.scale)
      val py = (case.h - 3f) - (obs.y - entrance.doorWorldY) * 0.72f / (2f * obs.scale)
      // 1 sprite px = 2*scale = 1.6 world units at scale 0.80, and the door trigger sits
      // (obstacle.x - 2) world units from the anchor: that is 1.25 px, i.e. sub-pixel. The
      // painted door must therefore be centred within 1.5 px of the canvas centre.
      assertEquals("${case.key}: trigger must invert onto canvas centre X", case.w / 2f, px, 1.5f)
      assertEquals("${case.key}: trigger must invert onto the H-3 base row", case.h - 3f, py, 1.5f)
      // Probe the leaf a few pixels above the sill: the sill row itself is the contact rim.
      val liftPx = 6
      val cx = px.toInt()
      val cy = py.toInt() - liftPx
      val p = bmp.getPixel(cx, cy)
      val luminance = (((p shr 16) and 0xFF) * 299 + ((p shr 8) and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
      assertTrue("${case.key}: door pixel must be a dark opaque leaf (0x${Integer.toHexString(p)}, luma $luminance)",
        (p ushr 24) == 255 && luminance < 96)
      // Forward mapping: the probe must land on the trigger's column, and exactly `liftPx` sprite
      // rows of world space *north* of it (6 px * 1.6 / 0.72 = 13.33 world units at scale 0.80;
      // sprite rows run down-canvas like world +Y, so a row above the sill is -Y). Tolerances
      // absorb the 1 px truncation of the probe coordinate.
      val liftWorld = liftPx * (2f * obs.scale) / 0.72f
      val (wx, wy) = worldOf(obs.x, obs.y, case.w, case.h, obs.scale, cx.toFloat(), cy.toFloat())
      assertTrue("${case.key}: door must map to world x ${entrance.doorWorldX}, got $wx",
        kotlin.math.abs(wx - entrance.doorWorldX) <= 4f)
      assertTrue(
        "${case.key}: door probe must map -$liftWorld u from the trigger, got ${wy - entrance.doorWorldY}",
        kotlin.math.abs((wy - entrance.doorWorldY) + liftWorld) <= 2f,
      )
      // Ancillary contract: the entrance and the obstacle were not edited to fit the art.
      assertEquals("${case.key}: entrance row stays on the obstacle anchor", obs.y, entrance.doorWorldY, 0.01f)
    }
  }

  /** No world contract may move for an art pass: props, anchors, footprints, zones, colliders. */
  @Test
  fun worldGeometryIsUnchanged() {
    prepare()
    val obstacles = obstacles()
    fun obs(id: String) = obstacles.first { it.id == id }
    listOf(
      ExpectedObstacle("monument_tower_gate", 1960f, 585f, 0.96f, 155f, 110f, PropKind.FACILITY_TOWER_GATE),
      ExpectedObstacle("building_summon_hall", 2430f, 2005f, 1.08f, 195f, 135f, PropKind.FACILITY_SUMMONING_HALL),
      ExpectedObstacle("building_armory", 995f, 655f, 0.80f, 72f, 54f, PropKind.FACILITY_ARMORY),
      ExpectedObstacle("building_hero_quarters", 825f, 790f, 0.80f, 74f, 54f, PropKind.FACILITY_HERO_QUARTERS),
      ExpectedObstacle("building_synthesis_lab", 1270f, 555f, 0.80f, 70f, 52f, PropKind.FACILITY_SYNTHESIS_LAB),
      ExpectedObstacle("building_restaurant", 1105f, 925f, 0.80f, 74f, 54f, PropKind.FACILITY_RESTAURANT),
    ).forEach { e ->
      val o = obs(e.id)
      assertEquals("${e.id} x", e.x, o.x, 0.01f)
      assertEquals("${e.id} y", e.y, o.y, 0.01f)
      assertEquals("${e.id} scale", e.scale, o.scale, 0.001f)
      assertEquals("${e.id} footprintHalfW", e.halfW, o.footprintHalfW, 0.01f)
      assertEquals("${e.id} footprintDepthNorth", e.depth, o.footprintDepthNorth, 0.01f)
      assertEquals("${e.id} PropKind", e.prop, o.propKind)
    }
    assertEquals("hero registry anchor x", 785f, obs("building_hero_registry").x, 0.01f)
    assertEquals("hero registry anchor y", 1185f, obs("building_hero_registry").y, 0.01f)

    val zones = TowerFloorCatalog.SANCTUARY_ZONES.associateBy { it.facility }
    assertEquals("summoning zone x", 2380f, zones.getValue(FacilityType.SUMMONING_HALL).centerX, 0.01f)
    assertEquals("summoning zone y", 1930f, zones.getValue(FacilityType.SUMMONING_HALL).centerY, 0.01f)
    assertEquals("tower gate zone x", 1960f, zones.getValue(FacilityType.TOWER_GATE).centerX, 0.01f)

    assertEquals(4, SanctuaryHouseEntrances.ENTRIES.size)
    assertEquals(823f, SanctuaryHouseEntrances.ENTRIES.getValue(FacilityType.HERO_QUARTERS).doorWorldX, 0.01f)
    assertEquals(1103f, SanctuaryHouseEntrances.ENTRIES.getValue(FacilityType.RESTAURANT).doorWorldX, 0.01f)
    assertEquals(993f, SanctuaryHouseEntrances.ENTRIES.getValue(FacilityType.ARMORY).doorWorldX, 0.01f)
  }

  /**
   * Fallback path: `assetSafe` must return null (not throw) for a PNG that is not packaged, and
   * the atlas property must still resolve to the procedural sprite with the same canvas.
   */
  @Test
  fun missingPngFallsBackToProceduralSprite() {
    prepare()
    assertNull("absent PNG must not throw", SanctuaryArtAssets.assetSafe("premium_slice/__p21_absent__.png"))
    // The procedural builders are still the documented fallback and keep the same canvases.
    val fallbackSummoning = PixelSanctuaryLandmarks.buildSummoningHall().asAndroidBitmap()
    assertEquals(268, fallbackSummoning.width)
    assertEquals(206, fallbackSummoning.height)
    val fallbackGate = PixelSanctuaryLandmarks.buildTowerGateMonument().asAndroidBitmap()
    assertEquals(260, fallbackGate.width)
    assertEquals(224, fallbackGate.height)
    val fallbackRegistry = PixelSanctuaryLandmarks.buildHeroRegistryHall().asAndroidBitmap()
    assertEquals(188, fallbackRegistry.width)
    assertEquals(166, fallbackRegistry.height)
    // And the atlas returns a live bitmap for every P21 facility.
    listOf(
      PixelStructureAtlas::towerGateMonument, PixelStructureAtlas::summoningHallStructure,
      PixelStructureAtlas::heroRegistryStructure, PixelStructureAtlas::heroQuartersStructure,
      PixelStructureAtlas::restaurantStructure, PixelStructureAtlas::synthesisLabStructure,
      PixelStructureAtlas::armoryStructure,
    ).forEach { assertNotNull("atlas property must resolve", it.get()) }
  }

  /**
   * The tower gate's aperture must be the box the renderer animates the rift into.
   *
   * The renderer draws a bright, moving rift oval at sprite (W/2, 120) +/- (16, 27). The art must
   * therefore paint a DARK recess in exactly that box -- if it painted its own bright orb, the
   * animated ring would sit on a static light and read as a sticker. The painted light belongs
   * below the ring, on the threshold, where it can look like the source of the spill.
   */
  @Test
  fun towerGatePortalMatchesAnimatedRiftBox() {
    prepare()
    val bmp = bitmap("tower_gate").asAndroidBitmap()
    // Renderer: rect at (cx-32s, cy-196s-54s)..(cx+32s, cy-196s+54s), pixelScale 2s
    // => sprite centre (W/2, 120) with a 16 x 27 px half-size.
    val cx = bmp.width / 2
    val cy = 120
    var dark = 0
    var bright = 0
    for (y in (cy - 20)..(cy + 20)) for (x in (cx - 14)..(cx + 14)) {
      val p = bmp.getPixel(x, y)
      val r = (p shr 16) and 0xFF; val g = (p shr 8) and 0xFF; val b = p and 0xFF
      if ((p ushr 24) == 0) continue
      if (r < 40 && g < 40 && b < 46) dark++
      if (g > 140 && b > 160) bright++
    }
    assertTrue("the rift box must be a dark recess, not sky ($dark px)", dark > 600)
    assertTrue("the rift box must not paint its own light ($bright px)", bright < 60)
    // The animated ring's outer edge must stay inside opaque stone, not float in the sky.
    for (x in (cx - 16)..(cx + 16)) {
      assertTrue("rift top edge must sit on stone at x=$x", (bmp.getPixel(x, cy - 27) ushr 24) != 0)
    }
    // And the painted light lives on the threshold, below the rift box (rows H-38..H-24).
    var spill = 0
    for (y in (bmp.height - 38)..(bmp.height - 24)) for (x in (cx - 8)..(cx + 8)) {
      val p = bmp.getPixel(x, y)
      if ((p ushr 24) != 0 && ((p shr 8) and 0xFF) > 40) spill++
    }
    assertTrue("threshold must carry the cold spill ($spill px)", spill > 40)
  }
}
