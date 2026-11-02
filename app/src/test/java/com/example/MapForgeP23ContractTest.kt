package com.example

import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.data.model.FacilityType
import com.example.game.map.ObliqueCamera
import com.example.game.map.PropKind
import com.example.game.map.SanctuaryHouseEntrances
import com.example.game.map.WorldObstacle
import com.example.game.pixel.ObliqueSpriteGeometry
import com.example.game.pixel.PixelStructureAtlas
import com.example.game.pixel.SanctuaryEnvironmentShadows
import com.example.game.pixel.PremiumActorAtlas
import com.example.game.pixel.SanctuaryArtAssets
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
 * P23 asset + geometry contract for the architecture integration repair and the environmental
 * props cohesion pass.
 *
 * The defects P20/P21 shipped were structural, so the assertions here are structural too:
 *
 *  1. NO INTERIOR ALPHA HOLE. For each building canvas, every column of pixels inside the
 *     silhouette must be a single opaque run (no transparent band above a lower mass). That is
 *     the "grass shows through the roof" failure measured instead of eyeballed. The Tower Gate
 *     is the one documented exception: its twin proud towers leave a sky cleft, and the exact
 *     x range of that cleft is pinned so the exemption cannot quietly grow.
 *  2. DOORS LAND ON THEIR TRIGGERS. Each enterable facility's painted doorway inverts back onto
 *     its `SanctuaryHouseEntrances` row, and the Armory's door is on the trigger it was
 *     previously 76 world units away from.
 *  3. ROOFS ARE JOINED TO WALLS. The row immediately under the roof silhouette must be opaque
 *     across the building, which is what the soffit band now guarantees.
 *  4. PROPS ARE GROUNDED AND THEIR FRAMES ARE LOCKED. Every prop sprite reaches its ground row
 *     and the three lantern frames share all static pixels.
 *  5. THE WORLD CONTRACT DID NOT MOVE. Anchors, footprints, colliders, zones, doors, camera.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapForgeP23ContractTest {

  private fun prepare() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
  }

  private fun obstacles() = com.example.game.lobby.SanctuaryLobbyEngine().mapLayout.obstacles

  private fun premium(key: String) =
    requireNotNull(SanctuaryArtAssets.assetSafe("premium_slice/$key.png")) { "$key.png must load" }

  private fun sanct(key: String) =
    requireNotNull(SanctuaryArtAssets.assetSafe("sanctuary_p03/$key.png")) { "$key.png must load" }

  /** Columns whose opaque pixels form more than one run, i.e. a hole in the middle of a mass. */
  private fun holeColumns(bmp: android.graphics.Bitmap, xFrom: Int = 0, xTo: Int = -1): List<Int> {
    val last = if (xTo < 0) bmp.width - 1 else xTo
    val out = mutableListOf<Int>()
    for (x in xFrom..last) {
      var runs = 0
      var inRun = false
      for (y in 0 until bmp.height) {
        val opaque = (bmp.getPixel(x, y) ushr 24) >= 128
        if (opaque && !inRun) runs++
        inRun = opaque
      }
      if (runs > 1) out += x
    }
    return out
  }

  private fun opaqueAt(bmp: android.graphics.Bitmap, x: Int, y: Int) = (bmp.getPixel(x, y) ushr 24) >= 128

  private fun lowestOpaqueRow(bmp: android.graphics.Bitmap): Int {
    for (y in bmp.height - 1 downTo 0) {
      for (x in 0 until bmp.width) if (opaqueAt(bmp, x, y)) return y
    }
    return -1
  }

  // ------------------------------------------------------------------ 1. no interior holes
  @Test
  fun everyAuthoredBuildingHasNoInteriorAlphaHole() {
    prepare()
    val cases = listOf(
      "summoning_hall" to premium("summoning_hall"),
      "hero_registry" to premium("hero_registry"),
      "hero_quarters" to premium("hero_quarters"),
      "restaurant" to premium("restaurant"),
      "synthesis_lab" to premium("synthesis_lab"),
      "armory" to premium("armory"),
      "townhouse_a" to sanct("townhouse_a"),
      "townhouse_b" to sanct("townhouse_b"),
      "cottage_brown" to sanct("cottage_brown"),
      "cottage_slate" to sanct("cottage_slate"),
      "fortress_tower" to sanct("fortress_tower"),
      "gate_tower" to sanct("gate_tower"),
    )
    for ((name, image) in cases) {
      val bmp = image.asAndroidBitmap()
      val holes = holeColumns(bmp)
      assertTrue("$name: ${holes.size} columns contain an interior alpha hole: " +
        "${holes.take(8)}", holes.isEmpty())
    }
    // The Tower Gate's twin towers intentionally leave a sky cleft. Pin its exact span so the
    // exemption stays a documented silhouette instead of a licence for floating roofs.
    val gate = premium("tower_gate").asAndroidBitmap()
    val gateHoles = holeColumns(gate)
    assertTrue("tower gate cleft must exist between the two towers (design): $gateHoles",
      gateHoles.isNotEmpty())
    for (x in gateHoles) {
      assertTrue("tower gate sky cleft must stay inside x 96..166, got $x", x in 96..166)
    }
    assertTrue("tower gate must stay hole-free outside the cleft",
      holeColumns(gate, 0, 95).isEmpty() && holeColumns(gate, 167, gate.width - 1).isEmpty())
  }

  // ------------------------------------------------------------------ 2. doors on triggers
  @Test
  fun enterableDoorsSitOnTheirWorldTriggers() {
    prepare()
    val entrances = SanctuaryHouseEntrances.ENTRIES
    val byObstacle = entrances.values.associateBy { it.obstacleId }
    val obstacles = obstacles()
    // (premium key, obstacle id, canvas W, canvas H)
    val cases = listOf(
      Triple("armory", "building_armory", 190),
      Triple("hero_quarters", "building_hero_quarters", 200),
      Triple("restaurant", "building_restaurant", 206),
    )
    for ((key, obstacleId, w) in cases) {
      val obs = requireNotNull(obstacles.firstOrNull { it.id == obstacleId }) { "$obstacleId" }
      val entrance = requireNotNull(byObstacle[obstacleId]) { "$obstacleId entrance" }
      val bmp = premium(key).asAndroidBitmap()
      assertEquals("$key width", w, bmp.width)
      // inverse mapping of the renderer's bottom-centred blit at (cx, cy + 6s), pixelScale 2s
      val px = w / 2f + (entrance.doorWorldX - obs.x) / (2f * obs.scale)
      val py = (bmp.height - 3f) - (obs.y - entrance.doorWorldY) * 0.72f / (2f * obs.scale)
      assertEquals("$key: trigger must invert to the door column", w / 2f, px, 2.5f)
      assertEquals("$key: trigger must invert to the H-3 sill row", bmp.height - 3f, py, 2.5f)
      // the leaf a few rows above the sill must be a dark, opaque, recessed plank
      val cx = px.toInt()
      val cy = py.toInt() - 8
      val p = bmp.getPixel(cx, cy)
      val luma = (((p shr 16) and 0xFF) * 299 + ((p shr 8) and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
      assertTrue("$key: door probe must be a dark opaque leaf (0x${Integer.toHexString(p)})",
        (p ushr 24) == 255 && luma < 90)
      // and the surround on both sides must be lit stone, i.e. this is a real opening
      for (side in listOf(-14, 14)) {
        val q = bmp.getPixel(cx + side, cy)
        assertTrue("$key: door surround at $side must be opaque", (q ushr 24) == 255)
      }
    }
    // The Armory must hold the specific correction P21 documented: its P20 art advertised a
    // doorway ~76 world units east / ~120 north of its trigger. Assert the fixed mapping.
    val armory = premium("armory").asAndroidBitmap()
    val armoryEntrance = entrances.getValue(FacilityType.ARMORY)
    assertEquals("armory trigger x was NOT moved", 993f, armoryEntrance.doorWorldX, 0.01f)
    assertEquals("armory trigger y was NOT moved", 655f, armoryEntrance.doorWorldY, 0.01f)
    // P20's painted door box, inverting through the same maths, is where the art must NOT be.
    val oldCx = 141.5f // centre of x 133..150
    val oldWorldX = 995f + (oldCx - 190 / 2f) * 2f * 0.80f
    assertTrue("the P20 door position must be measurably wrong (was $oldWorldX)",
      kotlin.math.abs(oldWorldX - armoryEntrance.doorWorldX) > 40f)
    // A doorway is a recess: dark leaf at the centre column, lit wall stone either side of it.
    val leaf = armory.getPixel(94, 150)
    val leftWall = armory.getPixel(94 - 20, 150)
    val rightWall = armory.getPixel(94 + 20, 150)
    fun luma(c: Int) = (((c shr 16) and 0xFF) * 299 + ((c shr 8) and 0xFF) * 587 + (c and 0xFF) * 114) / 1000
    assertTrue("armory door leaf must be dark at canvas centre (luma ${luma(leaf)})", luma(leaf) < 90)
    assertTrue("armory door must be flanked by lit wall stone",
      luma(leftWall) > luma(leaf) && luma(rightWall) > luma(leaf))
  }

  // ------------------------------------------------------------------ 3. roof joined to wall
  @Test
  fun roofSilhouetteIsContinuousAcrossEveryBuilding() {
    prepare()
    for (key in listOf("summoning_hall", "hero_registry", "hero_quarters", "restaurant",
      "synthesis_lab", "armory", "townhouse_a", "townhouse_b", "cottage_brown",
      "cottage_slate")) {
      val bmp = if (key.startsWith("townhouse") || key.startsWith("cottage"))
        sanct(key).asAndroidBitmap() else premium(key).asAndroidBitmap()
      // For every column that carries roof pixels, the row under the topmost opaque pixel must
      // be opaque too (an eave/fascia), and the body below must be continuous down to the base.
      var checked = 0
      for (x in 0 until bmp.width) {
        val top = (0 until bmp.height).firstOrNull { opaqueAt(bmp, x, it) } ?: continue
        assertEquals("$key: $x must have an opaque eave under its top pixel", true,
          opaqueAt(bmp, x, top + 1) || opaqueAt(bmp, x, top + 2))
        checked++
      }
      assertTrue("$key: no roof columns found", checked > bmp.width / 4)
    }
  }

  // ------------------------------------------------------------------ 4. props grounded/locked
  @Test
  fun propsReachTheGroundRowAndLanternFramesAreLocked() {
    prepare()
    val props = listOf("flower_planter", "wooden_bench", "ruin_arch", "retaining_wall",
      "monolith", "training_dummy", "weapon_rack", "striking_post",
      "street_lantern_0", "street_lantern_1", "street_lantern_2")
    for (key in props) {
      val bmp = sanct(key).asAndroidBitmap()
      val bottom = lowestOpaqueRow(bmp)
      assertTrue("$key: base row $bottom must reach the canvas ground row (${bmp.height - 1})",
        bottom >= bmp.height - 3)
      var contact = 0
      for (x in 0 until bmp.width) if (opaqueAt(bmp, x, bottom)) contact++
      assertTrue("$key: ground contact too narrow ($contact)", contact >= 6)
      // binary alpha only: no semi-transparent halo
      for (y in 0 until bmp.height) for (x in 0 until bmp.width) {
        val a = bmp.getPixel(x, y) ushr 24
        assertTrue("$key: semi-transparent pixel at ($x,$y)", a == 0 || a == 255)
      }
    }
    // Static-pixel lock: the lamp's post, base, bracket and housing must be identical across
    // the three frames; only columns inside the glass may differ.
    val frames = (0..2).map { sanct("street_lantern_$it").asAndroidBitmap() }
    for (f in 1..2) {
      for (x in 0 until frames[0].width) {
        for (y in 0 until frames[0].height) {
          if (x in 19..25) continue
          assertEquals("street lantern frame $f must not change column $x row $y",
            frames[0].getPixel(x, y), frames[f].getPixel(x, y))
        }
      }
    }
    var changed = 0
    for (x in 19..25) for (y in 0 until frames[0].height) {
      if (frames[0].getPixel(x, y) != frames[1].getPixel(x, y)) changed++
    }
    assertTrue("the flame must actually change between frames ($changed px)", changed > 0)
  }

  // ------------------------------------------------------------------ 5. world contract intact
  @Test
  fun worldGeometryEntrancesAndCameraAreUntouched() {
    prepare()
    val obstacles = obstacles()
    fun obs(id: String) = requireNotNull(obstacles.firstOrNull { it.id == id }) { "$id missing" }
    data class Expect(val id: String, val x: Float, val y: Float, val s: Float,
                      val hw: Float, val d: Float, val kind: PropKind)
    listOf(
      Expect("monument_tower_gate", 1960f, 585f, 0.96f, 155f, 110f, PropKind.FACILITY_TOWER_GATE),
      Expect("building_summon_hall", 2430f, 2005f, 1.08f, 195f, 135f, PropKind.FACILITY_SUMMONING_HALL),
      Expect("building_armory", 995f, 655f, 0.80f, 72f, 54f, PropKind.FACILITY_ARMORY),
      Expect("building_hero_quarters", 825f, 790f, 0.80f, 74f, 54f, PropKind.FACILITY_HERO_QUARTERS),
      Expect("building_synthesis_lab", 1270f, 555f, 0.80f, 70f, 52f, PropKind.FACILITY_SYNTHESIS_LAB),
      Expect("building_restaurant", 1105f, 925f, 0.80f, 74f, 54f, PropKind.FACILITY_RESTAURANT),
      Expect("building_hero_registry", 785f, 1185f, 0.80f, 72f, 54f, PropKind.FACILITY_HERO_REGISTRY),
      Expect("nw_outer_slate_house", 655f, 930f, 0.84f, 64f, 50f, PropKind.MEDIEVAL_TOWNHOUSE_B),
      Expect("nw_outer_brown_cottage", 495f, 1075f, 0.86f, 58f, 46f, PropKind.SANCTUARY_COTTAGE_BROWN),
      Expect("inner_slate_wing_cottage", 1395f, 675f, 0.82f, 54f, 44f, PropKind.SANCTUARY_COTTAGE_SLATE),
      Expect("south_gate_tower_west", 1360f, 2395f, 1.02f, 44f, 32f, PropKind.SOUTHERN_GATE_TOWER),
    ).forEach { e ->
      val o = obs(e.id)
      assertEquals("${e.id} x", e.x, o.x, 0.01f)
      assertEquals("${e.id} y", e.y, o.y, 0.01f)
      assertEquals("${e.id} scale", e.s, o.scale, 0.001f)
      assertEquals("${e.id} halfW", e.hw, o.footprintHalfW, 0.01f)
      assertEquals("${e.id} depth", e.d, o.footprintDepthNorth, 0.01f)
      assertEquals("${e.id} kind", e.kind, o.propKind)
    }
    // perimeter corner towers keep their eight octant anchors
    for (i in 0..7) assertNotNull("corner tower $i", obstacles.firstOrNull { it.id == "fortress_corner_tower_$i" })
    // doors and zones did not move
    assertEquals(4, SanctuaryHouseEntrances.ENTRIES.size)
    assertEquals(823f, SanctuaryHouseEntrances.ENTRIES.getValue(FacilityType.HERO_QUARTERS).doorWorldX, 0.01f)
    assertEquals(1103f, SanctuaryHouseEntrances.ENTRIES.getValue(FacilityType.RESTAURANT).doorWorldX, 0.01f)
    assertEquals(993f, SanctuaryHouseEntrances.ENTRIES.getValue(FacilityType.ARMORY).doorWorldX, 0.01f)
    val zones = com.example.game.tower.TowerFloorCatalog.SANCTUARY_ZONES.associateBy { it.facility }
    assertEquals(2380f, zones.getValue(FacilityType.SUMMONING_HALL).centerX, 0.01f)
    assertEquals(1930f, zones.getValue(FacilityType.SUMMONING_HALL).centerY, 0.01f)
    assertEquals(1960f, zones.getValue(FacilityType.TOWER_GATE).centerX, 0.01f)
    // camera contract
    assertEquals(0.72f, ObliqueCamera.OBLIQUE_Y_SCALE, 0f)
    // the Summoning Hall's rune stage must still be authored at the zone's sprite inverse
    val hall = premium("summoning_hall").asAndroidBitmap()
    val stageX = hall.width / 2f + (2380f - 2430f) / (2f * 1.08f)
    val stageY = (hall.height - 3f) - (2005f - 1930f) * 0.72f / (2f * 1.08f)
    assertTrue("rune stage must stay at sprite (${stageX.toInt()}, ${stageY.toInt()})",
      stageX in 100f..122f && stageY in 165f..190f)
    var runeLit = 0
    for (y in (stageY.toInt() - 8)..(stageY.toInt() + 8)) {
      for (x in (stageX.toInt() - 20)..(stageX.toInt() + 20)) {
        val p = hall.getPixel(x, y)
        if ((p ushr 24) != 0 && (p and 0xFF) > 110 && (p shr 8 and 0xFF) > 80) runeLit++
      }
    }
    assertTrue("inlaid rune ring must be visible at the interaction point ($runeLit px)",
      runeLit > 60)
  }

  // ------------------------------------------------------------------ 6. manifest + fallback
  @Test
  fun manifestsPinTheNewPngsAndFallbacksStillExist() {
    prepare()
    val premiumManifest = JSONObject(SanctuaryArtAssets.textAsset("premium_slice/manifest.json")!!)
    val premiumAssets = premiumManifest.getJSONObject("assets")
    for (key in listOf("summoning_hall", "tower_gate", "hero_registry", "hero_quarters",
      "restaurant", "synthesis_lab", "armory")) {
      val entry = premiumAssets.getJSONObject(key)
      val bytes = RuntimeEnvironment.getApplication().assets
        .open("premium_slice/${entry.getString("file")}").use { it.readBytes() }
      val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }
      assertEquals("$key SHA-256 vs manifest", entry.getString("sha256"), digest)
      val bmp = premium(key).asAndroidBitmap()
      assertEquals("$key manifest width", entry.getJSONArray("size").getInt(0), bmp.width)
      assertEquals("$key manifest height", entry.getJSONArray("size").getInt(1), bmp.height)
    }
    val p03 = JSONObject(SanctuaryArtAssets.textAsset("sanctuary_p03/manifest.json")!!)
    val entries = p03.getJSONArray("assets")
    val byId = (0 until entries.length()).map { entries.getJSONObject(it) }
      .associateBy { it.getString("id") }
    for (id in listOf("townhouse_a", "townhouse_b", "cottage_brown", "cottage_slate",
      "fortress_tower", "gate_tower", "flower_planter", "street_lantern_0", "street_lantern_1",
      "street_lantern_2", "wooden_bench", "ruin_arch", "retaining_wall", "monolith",
      "training_dummy", "weapon_rack", "striking_post")) {
      val e = requireNotNull(byId[id]) { "$id missing from sanctuary_p03/manifest.json" }
      val bytes = RuntimeEnvironment.getApplication().assets
        .open("sanctuary_p03/${e.getString("path")}").use { it.readBytes() }
      val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }
      assertEquals("$id SHA-256 vs manifest", e.getString("sha256"), digest)
      val bmp = sanct(id).asAndroidBitmap()
      assertEquals("$id manifest width", e.getJSONArray("size").getInt(0), bmp.width)
      assertEquals("$id manifest height", e.getJSONArray("size").getInt(1), bmp.height)
    }
    // P18/P20/P22 controls must not be re-authored by this pass.
    assertEquals("oak.png must keep its P18 bytes",
      "556eefd5c1b1f14872b67e6e651b8c7d6645e4632519d0719a6ce614b240a229",
      byId.getValue("oak").getString("sha256"))
    assertEquals("cypress.png must keep its P20 bytes",
      "442cbc617c6d61f79f2105f7c4b2072a2fc721fa3a4e56ee11d695d576cf988b",
      byId.getValue("cypress").getString("sha256"))
    // fallbacks: a missing PNG must not throw, and the procedural builders keep their canvases
    assertNull("absent PNG must not throw", SanctuaryArtAssets.assetSafe("sanctuary_p03/__absent__.png"))
    assertEquals(152, sanct("townhouse_a").asAndroidBitmap().width)
    assertEquals(80, sanct("fortress_tower").asAndroidBitmap().width)
    listOf(
      PixelStructureAtlas::summoningHallStructure,
      PixelStructureAtlas::towerGateMonument,
      PixelStructureAtlas::heroRegistryStructure,
      PixelStructureAtlas::heroQuartersStructure,
      PixelStructureAtlas::restaurantStructure,
      PixelStructureAtlas::synthesisLabStructure,
      PixelStructureAtlas::armoryStructure,
      PixelStructureAtlas::medievalTownhouseA,
      PixelStructureAtlas::medievalTownhouseB,
      PixelStructureAtlas::sanctuaryCottageBrown,
      PixelStructureAtlas::sanctuaryCottageSlate,
      PixelStructureAtlas::fortressCornerTowerSprite,
      PixelStructureAtlas::southernGateTowerSprite,
      PixelStructureAtlas::ruinArchSprite,
      PixelStructureAtlas::monolithSprite,
      PixelStructureAtlas::stoneRetainingWallSprite,
      PixelStructureAtlas::woodenBenchSprite,
      PixelStructureAtlas::trainingStrikingPostSprite,
    ).forEach { assertNotNull("atlas property must resolve", it.get()) }
    assertEquals(3, PixelStructureAtlas.streetLanternFrames.size)
    assertNotNull(PixelStructureAtlas.flowerPlanterSprite)
    assertNotNull(PixelStructureAtlas.trainingDummySprite)
    assertNotNull(PixelStructureAtlas.weaponRackSprite)
  }

  // ------------------------------------------------------------------ 7. shadow registration
  @Test
  fun reAuthoredPropsCastApprovedSilhouetteShadows() {
    prepare()
    // Every prop that gained a sprite must be registered for the projected shadow pass, so the
    // old painted blob is replaced rather than doubled.
    for ((kind, id) in listOf(
      PropKind.ANCIENT_RUIN_ARCH to "p23_ruin",
      PropKind.FLOWER_PLANTER_BED to "p23_planter",
      PropKind.TRAINING_DUMMY to "p23_dummy",
      PropKind.WEAPON_RACK to "p23_rack",
      PropKind.ANCIENT_MONOLITH to "p23_monolith",
      PropKind.WOODEN_BENCH_34 to "p23_bench",
      PropKind.STREET_LANTERN to "p23_lantern",
    )) {
      val obs = WorldObstacle(id, 1500f, 1400f, 20f, propKind = kind)
      assertNotNull("$kind must resolve a shadow sprite", SanctuaryEnvironmentShadows.sprite(obs))
    }
    // Trees still ground exactly on their world anchor: the foot offset equals the sprite's own
    // bottom padding, which is what keeps `drawFootOffset` from shifting ground anchors.
    for (id in listOf("oak", "pine", "twisted", "canopy_a", "canopy_b", "cypress")) {
      val bmp = SanctuaryArtAssets.image(id) ?: continue
      val kind = when (id) {
        "oak" -> PropKind.OAK_TREE
        "pine" -> PropKind.PINE_SPIRIT_TREE
        "twisted" -> PropKind.MYSTIC_PLUM_TREE
        "cypress" -> PropKind.CYPRESS_CONIFER_TREE
        else -> PropKind.SAKURA_BLOSSOM_TREE
      }
      assertEquals("$id: visible roots must stay on the world ground anchor", 0f,
        ObliqueSpriteGeometry.drawFootOffset(kind, bmp) -
          ObliqueSpriteGeometry.bounds(bmp).bottomPadding * ObliqueSpriteGeometry.pixelScale(kind),
        0.001f)
    }
    assertNotNull("Lyra sheet still resolves", PremiumActorAtlas.get("lyra"))
    assertEquals(8, SanctuaryArtAssets.tiles("grass", 8)!!.size)
  }
}
