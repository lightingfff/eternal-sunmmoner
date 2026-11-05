package com.example.game.pixel

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.sin
import kotlin.random.Random

/**
 * PixelStructureAtlas: Handcrafted 2.5D Oblique 3/4 dark-fantasy buildings, terraced stone
 * architecture, 3/4 furniture with visible legs/depth, street lanterns, and multi-layered trees.
 */
object PixelStructureAtlas {

  // Sanctuary 3/4 Landmark Facilities.
  //
  // P20 introduced the authored-PNG-first pattern for the Armory; P21 extends it to the six other
  // facilities and makes the fallback real: `assetSafe` returns null for a missing/corrupt PNG
  // instead of throwing, so the Elvis branch can actually reach the procedural builder. Canvases
  // are identical between PNG and fallback, so the renderer's bottom-centred blit, the door
  // triggers and the depth sort anchors are unchanged either way. Nothing is generated at draw
  // time: one cached decode, then one blit per facility per frame.
  // P27B: PNG-first, exact old canvas/foot pivot. If optional art is absent or
  // malformed, fall back to the unchanged P21 PNG (then procedural). The projected
  // shadow gets the same selected ImageBitmap, so its silhouette cache remains valid.
  val towerGateMonument: ImageBitmap get() =
    SanctuaryArtAssets.preferredLandmark("tower_gate", 260, 224) ?: towerGateMonumentFallback
  private val towerGateMonumentFallback: ImageBitmap by lazy { PixelSanctuaryLandmarks.buildTowerGateMonument() }
  val summoningHallStructure: ImageBitmap get() =
    SanctuaryArtAssets.preferredLandmark("summoning_hall", 268, 206) ?: summoningHallFallback
  private val summoningHallFallback: ImageBitmap by lazy { PixelSanctuaryLandmarks.buildSummoningHall() }
  val heroRegistryStructure: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/hero_registry.png")
      ?: SanctuaryArtAssets.assetSafe("premium_slice/hero_registry.png") ?: heroRegistryFallback
  private val heroRegistryFallback: ImageBitmap by lazy { PixelSanctuaryLandmarks.buildHeroRegistryHall() }
  val trainingGroundStructure: ImageBitmap by lazy { PixelSanctuaryLandmarks.buildTrainingGround() }
  val armoryStructure: ImageBitmap get() = SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/armory.png")
      ?: SanctuaryArtAssets.assetSafe("premium_slice/armory.png") ?: armoryFallback
  private val armoryFallback: ImageBitmap by lazy { PixelSanctuaryLandmarks.buildArmory() }
  val synthesisLabStructure: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/synthesis_lab.png")
      ?: SanctuaryArtAssets.assetSafe("premium_slice/synthesis_lab.png") ?: synthesisLabFallback
  private val synthesisLabFallback: ImageBitmap by lazy { PixelSanctuaryLandmarks.buildSynthesisLab() }
  val heroQuartersStructure: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/hero_quarters.png")
      ?: SanctuaryArtAssets.assetSafe("premium_slice/hero_quarters.png") ?: heroQuartersFallback
  private val heroQuartersFallback: ImageBitmap by lazy { PixelSanctuaryLandmarks.buildHeroQuarters() }
  val restaurantStructure: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/restaurant.png")
      ?: SanctuaryArtAssets.assetSafe("premium_slice/restaurant.png") ?: restaurantFallback
  private val restaurantFallback: ImageBitmap by lazy { PixelSanctuaryLandmarks.buildRestaurantExterior() }
  val floorVistaArchStructure: ImageBitmap by lazy { PixelSanctuaryLandmarks.buildFloorVistaArch() }

  // 2.5D Oblique 3/4 Medieval Townhouses, Cottages, Terraces, Stairs, Balustrades & Street Props
  // P23: the Sanctuary background houses and perimeter towers were the last procedural
  // `paintAnimeHalfTimberHouse` callers. They are now authored PNGs with the same canvas, the
  // same bottom-centred blit and the same 6 s / 2 s draw contract, so anchors, footprints,
  // colliders and depth sort are untouched. The procedural builders stay as the fallback path.
  // P27B-V5 (checkpoint 2, debt A): the residential art was still P03, measured at luma
  // 51-59 against P27B landmarks at ~175. The P27B-V5 houses below share the landmark
  // palette and are cut to the SAME canvas and bottom-centre pivot, so footprints,
  // colliders, doorway triggers and depth sort are untouched. P03 stays as the next
  // fallback tier, then the procedural builder.
  val medievalTownhouseA: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/townhouse_a.png")
      ?: SanctuaryArtAssets.assetSafe("sanctuary_p03/townhouse_a.png")
      ?: medievalTownhouseAFallback
  private val medievalTownhouseAFallback: ImageBitmap by lazy { buildMedievalTownhouse(variant = 0) }
  val medievalTownhouseB: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/townhouse_b.png")
      ?: SanctuaryArtAssets.assetSafe("sanctuary_p03/townhouse_b.png")
      ?: medievalTownhouseBFallback
  private val medievalTownhouseBFallback: ImageBitmap by lazy { buildMedievalTownhouse(variant = 1) }
  val sanctuaryCottageBrown: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/cottage_brown.png")
      ?: SanctuaryArtAssets.assetSafe("sanctuary_p03/cottage_brown.png")
      ?: sanctuaryCottageBrownFallback
  private val sanctuaryCottageBrownFallback: ImageBitmap by lazy { buildSanctuaryCottage(useBlueSlate = false) }
  val sanctuaryCottageSlate: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/cottage_slate.png")
      ?: SanctuaryArtAssets.assetSafe("sanctuary_p03/cottage_slate.png")
      ?: sanctuaryCottageSlateFallback
  private val sanctuaryCottageSlateFallback: ImageBitmap by lazy { buildSanctuaryCottage(useBlueSlate = true) }
  val fortressCornerTowerSprite: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p03/fortress_tower.png") ?: fortressCornerTowerFallback
  private val fortressCornerTowerFallback: ImageBitmap by lazy { buildFortressTower(isGatehouse = false) }
  val southernGateTowerSprite: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p03/gate_tower.png") ?: southernGateTowerFallback
  private val southernGateTowerFallback: ImageBitmap by lazy { buildFortressTower(isGatehouse = true) }
  val sakuraTreeSpriteA: ImageBitmap get() = SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/canopy_a.png")
      ?: SanctuaryArtAssets.image("canopy_a") ?: sakuraTreeSpriteAFallback
  private val sakuraTreeSpriteAFallback: ImageBitmap by lazy { SanctuaryVegetation.tree(0, 114, 132) }
  val sakuraTreeSpriteB: ImageBitmap get() = SanctuaryArtAssets.image("canopy_b") ?: sakuraTreeSpriteBFallback
  private val sakuraTreeSpriteBFallback: ImageBitmap by lazy { SanctuaryVegetation.tree(1, 96, 114) }
  val cypressConiferSprite: ImageBitmap get() = SanctuaryArtAssets.image("cypress") ?: cypressConiferSpriteFallback
  private val cypressConiferSpriteFallback: ImageBitmap by lazy { SanctuaryVegetation.tree(2, 48, 88) }
  val gardenHedgeBushSprite: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p03/hedge_bush.png") ?: gardenHedgeBushFallback
  private val gardenHedgeBushFallback: ImageBitmap by lazy { SanctuaryVegetation.shrub() }
  val trainingStrikingPostSprite: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p03/striking_post.png") ?: trainingStrikingPostFallback
  private val trainingStrikingPostFallback: ImageBitmap by lazy { buildTrainingStrikingPost() }
  val terraceCliffWallSprite: ImageBitmap by lazy { buildTerraceCliffWall() }
  val terraceGrandStairsSprite: ImageBitmap by lazy { buildTerraceGrandStairs() }
  val stoneBalustradeSprite: ImageBitmap by lazy { buildStoneBalustradeRail() }
  // P23: three authored frames that share every static pixel (post, base, bracket, housing);
  // only the flame inside the glass differs, so the lamp cannot jitter between frames.
  val streetLanternFrames: List<ImageBitmap> by lazy {
    (0..2).map { frame ->
      // P27B-V5: the new lantern is ONE authored sprite, so all three frames use it. A
      // P03 frame-1/2 fallback would put two lantern designs in the same street, so the
      // only fallback from here is the procedural builder at the matching frame.
      SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/street_lantern_0.png")
        ?: SanctuaryArtAssets.assetSafe("sanctuary_p03/street_lantern_$frame.png")
        ?: buildStreetLantern(frame)
    }
  }
  val barrelClusterSprite: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/barrels.png")
      ?: SanctuaryArtAssets.image("barrels") ?: barrelClusterSpriteFallback
  private val barrelClusterSpriteFallback: ImageBitmap by lazy { buildBarrelCluster() }
  val woodenBenchSprite: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/wooden_bench.png")
      ?: SanctuaryArtAssets.assetSafe("sanctuary_p03/wooden_bench.png") ?: woodenBenchFallback
  private val woodenBenchFallback: ImageBitmap by lazy { buildWoodenBench34() }
  val arcaneBookshelfSprite: ImageBitmap by lazy { buildArcaneBookshelf34() }
  val stoneRetainingWallSprite: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p03/retaining_wall.png") ?: stoneRetainingWallFallback
  private val stoneRetainingWallFallback: ImageBitmap by lazy { buildStoneRetainingWall() }

  // 3/4 Restaurant Interior Furnishings (Reference 2)
  val tavernCounterSprite: ImageBitmap by lazy { buildTavernCounterBar() }
  val tavernShelfBarrelsSprite: ImageBitmap by lazy { buildTavernShelfBarrels() }
  val tavernFireplaceFrames: List<ImageBitmap> by lazy { (0..2).map { buildTavernFireplace(it) } }
  val tavernLongTableSprite: ImageBitmap by lazy { buildTavernLongTable() }

  // Monumental Statues, Columns, Braziers & Multi-Layered 3/4 Trees
  val obsidianColumnSprite: ImageBitmap get() = SanctuaryArtAssets.image("pillar") ?: obsidianColumnSpriteFallback
  private val obsidianColumnSpriteFallback: ImageBitmap by lazy { buildObsidianColumn() }
  val royalStatueSprite: ImageBitmap by lazy { buildRoyalStatue() }
  val oakTreeSprite: ImageBitmap get() = SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/oak.png")
      ?: SanctuaryArtAssets.image("oak") ?: oakTreeSpriteFallback
  private val oakTreeSpriteFallback: ImageBitmap by lazy { SanctuaryVegetation.tree(0, 112, 136) }
  val spiritPineSprite: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/pine.png")
      ?: SanctuaryArtAssets.image("pine") ?: spiritPineSpriteFallback
  private val spiritPineSpriteFallback: ImageBitmap by lazy { SanctuaryVegetation.tree(2, 96, 128) }
  val mysticPlumTreeSprite: ImageBitmap get() = SanctuaryArtAssets.image("twisted") ?: mysticPlumTreeSpriteFallback
  private val mysticPlumTreeSpriteFallback: ImageBitmap by lazy { SanctuaryVegetation.tree(3, 112, 132) }
  val forestBushSprite: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p03/forest_bush.png") ?: forestBushFallback
  private val forestBushFallback: ImageBitmap by lazy { SanctuaryVegetation.shrub() }
  val ruinArchSprite: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p03/ruin_arch.png") ?: ruinArchFallback
  private val ruinArchFallback: ImageBitmap by lazy { buildAncientRuinArch() }
  val monolithSprite: ImageBitmap get() =
    SanctuaryArtAssets.assetSafe("sanctuary_p03/monolith.png") ?: monolithFallback
  private val monolithFallback: ImageBitmap by lazy { buildAncientMonolith() }
  // P23 props that have no procedural builder: the planter and the training yard set are
  // PNG-only, and every caller keeps its previous Canvas path as the fallback (`?: null`).
  val flowerPlanterSprite: ImageBitmap?
    get() = SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/flower_planter.png")
      ?: SanctuaryArtAssets.assetSafe("sanctuary_p03/flower_planter.png")
  val trainingDummySprite: ImageBitmap?
    get() = SanctuaryArtAssets.assetSafe("sanctuary_p27b_2/structures/training_dummy.png")
      ?: SanctuaryArtAssets.assetSafe("sanctuary_p03/training_dummy.png")
  val weaponRackSprite: ImageBitmap?
    get() = SanctuaryArtAssets.assetSafe("sanctuary_p03/weapon_rack.png")
  val mossyBoulderSprite: ImageBitmap get() = SanctuaryArtAssets.image("boulder") ?: boulderFallback
  private val boulderFallback by lazy { buildMossyBoulder() }
  val brazierFrames: List<ImageBitmap> by lazy { (0..2).map { buildArcaneBrazier(it) } }

  private fun buildMedievalTownhouse(variant: Int): ImageBitmap {
    val w = if (variant == 0) 152 else 144
    val h = if (variant == 0) 156 else 160
    val c = PixelCanvas(w, h)
    PixelSanctuaryLandmarks.paintAnimeHalfTimberHouse(
      c = c,
      leftX = 16,
      roofTopY = 16,
      bodyW = w - 36,
      roofH = 36,
      upperH = 34,
      lowerH = 56,
      useBlueSlateRoof = variant == 1,
      hasDormer = variant == 1,
      chimneyLeft = variant == 0,
    )
    c.applyCrispOutline(0xFF221B17.toInt())
    return c.toImageBitmap()
  }

  private fun buildSanctuaryCottage(useBlueSlate: Boolean): ImageBitmap {
    val w = 132
    val h = 144
    val c = PixelCanvas(w, h)
    PixelSanctuaryLandmarks.paintAnimeHalfTimberHouse(
      c = c,
      leftX = 14,
      roofTopY = 16,
      bodyW = 98,
      roofH = 34,
      upperH = 28,
      lowerH = 50,
      useBlueSlateRoof = useBlueSlate,
      hasDormer = false,
      chimneyLeft = !useBlueSlate,
    )
    c.applyCrispOutline(0xFF221B17.toInt())
    return c.toImageBitmap()
  }

  /**
   * Pale-Limestone Crenellated Cylindrical / Octagonal Fortress Tower matching perimeter walls.
   */
  private fun buildFortressTower(isGatehouse: Boolean): ImageBitmap {
    val w = if (isGatehouse) 96 else 80
    val h = if (isGatehouse) 136 else 118
    val c = PixelCanvas(w, h)
    val cx = w / 2
    val rx = if (isGatehouse) 38 else 31
    val topY = 18
    val botY = h - 14

    val stoneSun = 0xFF4A4E5E.toInt()
    val stoneLight = 0xFF3A3F4A.toInt()
    val stoneMid = 0xFF2F333E.toInt()
    val stoneShadow = 0xFF1E2028.toInt()
    val stoneDark = 0xFF23262E.toInt()
    val mortar = 0xFF5C5446.toInt()

    // Cylindrical stone shaft — vertical sun-to-shadow + horizontal ashlar courses + wear
    for (py in topY..botY) {
      val courseSeam = ((py - topY) % 8 == 0)
      for (dx in -rx..rx) {
        val normX = dx.toFloat() / rx
        val col = when {
          courseSeam -> mortar
          ((dx + (py / 8) * 7 + 100) % 13 == 0) -> mortar
          normX < -0.45f -> stoneSun
          normX < 0.15f -> stoneLight
          normX < 0.62f -> stoneMid
          else -> stoneShadow
        }
        c.set(cx + dx, py, col)
        // stone chip highlight on sun side
        if (!courseSeam && normX < -0.2f && (py + dx) % 17 == 0) c.set(cx + dx, py, 0xFF5A5F6E.toInt())
      }
    }
    // Rounded tower base plinth with AO shadow
    c.fillEllipse(cx, botY, rx + 2, 10, stoneDark)
    c.fillEllipse(cx, botY - 2, rx + 1, 9, stoneMid)
    c.strokeEllipse(cx, botY, rx + 2, 10, mortar, thickness = 1)
    c.fillEllipse(cx, botY + 6, rx, 4, 0xFF0A0C14.toInt()) // contact AO

    // Narrow arrow slits with inner shadow depth
    c.fillRect(cx - 2, topY + 38, 4, 16, 0xFF1A1818.toInt())
    c.vLine(cx - 2, topY + 38, 16, 0xFF2E2A28.toInt())
    c.vLine(cx + 1, topY + 38, 16, 0xFF0F0E14.toInt())
    if (isGatehouse) {
      c.fillRect(cx - 18, topY + 46, 3, 13, 0xFF1A1818.toInt())
      c.fillRect(cx + 15, topY + 46, 3, 13, 0xFF1A1818.toInt())
      c.vLine(cx - 18, topY + 46, 13, 0xFF2E2A28.toInt())
      c.vLine(cx + 17, topY + 46, 13, 0xFF0F0E14.toInt())
    }

    // Corbelled parapet ring & top deck with highlight chamfer
    c.fillEllipse(cx, topY + 2, rx + 4, 13, stoneDark)
    c.fillEllipse(cx, topY, rx + 4, 12, stoneLight)
    c.fillEllipse(cx, topY - 1, rx + 2, 8, 0xFF4A4E5E.toInt())
    c.fillEllipse(cx, topY, rx - 3, 8, stoneMid)
    c.strokeEllipse(cx, topY, rx + 4, 12, mortar, thickness = 1)

    // Crenellated merlons — cold moonlight highlight + side shadow depth
    for (mx in (cx - rx - 2)..(cx + rx - 6) step 11) {
      c.fillRect(mx, topY - 8, 7, 9, stoneSun)
      c.fillRect(mx + 5, topY - 8, 2, 9, stoneShadow)
      c.fillRect(mx, topY + 1, 7, 1, stoneShadow)
      c.hLine(mx, topY - 8, 7, 0xFF8A9AB8.toInt()) // cold moonlight
      c.set(mx + 6, topY - 7, 0xFF6B7A9A.toInt()) // highlight nick
    }

    c.applyCrispOutline(0xFF241F1B.toInt())
    return c.toImageBitmap()
  }

  /**
   * Lush Multi-Layered Anime Pink Sakura Cherry Blossom Tree with gnarled bark trunk & fallen petals!
   */
  private fun buildSakuraCherryTree(largeVariant: Boolean): ImageBitmap {
    val w = if (largeVariant) 114 else 96
    val h = if (largeVariant) 132 else 114
    val c = PixelCanvas(w, h)
    val cx = w / 2

    val trunkY = if (largeVariant) 74 else 62
    val trunkH = if (largeVariant) 44 else 38
    // Gnarled warm-walnut trunk & roots
    c.fillRect(cx - 9, trunkY, 18, trunkH, 0xFF4F3224.toInt())
    c.fillRect(cx - 6, trunkY + 2, 6, trunkH - 4, 0xFF6E4734.toInt())
    c.fillRect(cx + 4, trunkY + 2, 4, trunkH - 4, 0xFF362016.toInt())
    c.fillRect(cx - 16, trunkY + trunkH - 6, 32, 8, 0xFF4F3224.toInt())
    for (i in 0..10) {
      c.fillRect(cx - 8 - i, trunkY + 4 - i, 6, 4, 0xFF5C3B2B.toInt())
      c.fillRect(cx + 3 + i, trunkY + 4 - i, 6, 4, 0xFF3E261B.toInt())
    }

    val sh = 0xFF5A2540.toInt() // dark plum shadow
    val mid = 0xFF7A3358.toInt()
    val br = 0xFF9A6570.toInt() // muted dusty rose
    val hi = 0xFFB88A95.toInt()

    if (largeVariant) {
      c.drawOrganicLeafCluster(cx - 26, 62, 23, 17, sh, mid, br, hi, seed = 1701)
      c.drawOrganicLeafCluster(cx + 26, 64, 23, 17, sh, mid, br, hi, seed = 1702)
      c.drawOrganicLeafCluster(cx, 48, 33, 23, sh, mid, br, hi, seed = 1703)
      c.drawOrganicLeafCluster(cx - 18, 38, 22, 16, mid, br, hi, 0xFFFFFFFF.toInt(), seed = 1704)
      c.drawOrganicLeafCluster(cx + 18, 40, 21, 16, sh, mid, br, hi, seed = 1705)
      c.drawOrganicLeafCluster(cx - 2, 24, 25, 17, mid, br, hi, 0xFFFFFFFF.toInt(), seed = 1706)
    } else {
      c.drawOrganicLeafCluster(cx - 20, 54, 19, 14, sh, mid, br, hi, seed = 1711)
      c.drawOrganicLeafCluster(cx + 20, 56, 19, 14, sh, mid, br, hi, seed = 1712)
      c.drawOrganicLeafCluster(cx, 42, 27, 19, sh, mid, br, hi, seed = 1713)
      c.drawOrganicLeafCluster(cx - 3, 24, 21, 14, mid, br, hi, 0xFFFFFFFF.toInt(), seed = 1714)
    }

    c.applyCrispOutline(0xFF2B1220.toInt())

    // Scattered fallen sakura petals around trunk base (after outline so they sit softly on grass!)
    listOf(-22 to 2, -14 to 6, 12 to 5, 20 to 1, -6 to 8).forEach { (dx, dy) ->
      c.set(cx + dx, trunkY + trunkH + dy, br)
      c.set(cx + dx + 1, trunkY + trunkH + dy, hi)
    }
    return c.toImageBitmap()
  }

  private fun buildCypressConifer(): ImageBitmap {
    val w = 48
    val h = 88
    val c = PixelCanvas(w, h)
    val cx = w / 2
    // bark with highlight + deep furrow
    c.fillRect(cx - 4, 68, 8, 14, 0xFF4A2E1E.toInt())
    c.vLine(cx - 2, 68, 14, 0xFF6E4632.toInt())
    c.vLine(cx + 2, 69, 12, 0xFF2A1A12.toInt())
    c.fillEllipse(cx, 82, 6, 3, 0xFF0A0C14.toInt()) // AO
    for (ry in 10..70) {
      val t = (ry - 10) / 60f
      val halfW = (2 + (sin(t * 2.5f).coerceAtLeast(0.15f) * 15f)).toInt()
      for (dx in -halfW..halfW) {
        val col = when {
          dx < -halfW / 3 -> 0xFF3D5A36.toInt()
          dx < halfW / 3 -> 0xFF2A3D2B.toInt()
          else -> 0xFF162118.toInt()
        }
        c.set(cx + dx, ry, col)
        // snow tip on windward side
        if (ry % 8 == 0 && dx == -halfW + 1) c.set(cx + dx, ry, 0xFF8A9AB8.toInt())
      }
    }
    c.applyCrispOutline(0xFF0F1A14.toInt())
    return c.toImageBitmap()
  }

  private fun buildGardenHedgeBush(): ImageBitmap {
    val w = 46
    val h = 36
    val c = PixelCanvas(w, h)
    c.fillEllipse(w / 2, 32, 14, 4, 0xFF0A0C14.toInt()) // AO
    c.drawOrganicLeafCluster(
      w / 2, 20, 18, 12,
      0xFF162118.toInt(), 0xFF1A2E1F.toInt(), 0xFF2A3D2B.toInt(), 0xFF3D5A36.toInt(), seed = 1802,
    )
    // highlight flecks + micro flower
    c.set(w / 2 - 4, 16, 0xFF4A6B3E.toInt())
    c.set(w / 2 + 5, 18, 0xFF8A7A97.toInt())
    c.applyCrispOutline(0xFF0F1A14.toInt())
    return c.toImageBitmap()
  }

  private fun buildTrainingStrikingPost(): ImageBitmap {
    val w = 36
    val h = 86
    val c = PixelCanvas(w, h)
    val cx = w / 2
    // Stone/iron base collar
    c.fillEllipse(cx, 76, 11, 5, 0xFF5C4028.toInt())
    // Tall heavy wooden martial pillar with 3 staggered horizontal striking pegs!
    c.fillRect(cx - 5, 12, 10, 65, 0xFF6E4427.toInt())
    c.fillRect(cx - 4, 12, 4, 64, 0xFF96623D.toInt())
    // Rope-wrapped striking zones
    for (ry in listOf(24, 42, 58)) {
      c.fillRect(cx - 6, ry, 12, 6, 0xFFD6A86E.toInt())
      c.hLine(cx - 6, ry + 2, 12, 0xFFA67B46.toInt())
    }
    // Projecting wooden Wing-Chun pegs
    c.fillRect(cx - 13, 26, 8, 4, 0xFF8A5633.toInt())
    c.fillRect(cx + 5, 36, 9, 4, 0xFF8A5633.toInt())
    c.fillRect(cx - 12, 50, 7, 4, 0xFF8A5633.toInt())
    c.applyCrispOutline(0xFF21130B.toInt())
    return c.toImageBitmap()
  }

  /**
   * Carved 3/4 Vertical Stone Terrace Retaining Cliff Wall with Buttress Pilasters & Crystal Post (Ref 4!).
   */
  private fun buildTerraceCliffWall(): ImageBitmap {
    val w = 164
    val h = 64
    val c = PixelCanvas(w, h)
    // Top beveled stone parapet coping
    c.fillRect(2, 8, w - 4, 8, 0xFF695BA3.toInt())
    c.hLine(2, 8, w - 4, 0xFF9384CC.toInt())
    c.hLine(2, 15, w - 4, 0xFFF5B841.toInt())
    // Vertical carved ashlar cliff face showing unmistakable 3/4 elevation drop!
    c.drawStoneWall34(4, 16, w - 8, 42, sideDepthPx = 0, stoneBase = 0xFF2E264F.toInt(), stoneAlt = 0xFF392F61.toInt(), stoneHi = 0xFF56488A.toInt(), stoneShadow = 0xFF1A142E.toInt(), seed = 620)
    // Buttress pilasters + glowing cyan rune slits
    for (px in listOf(14, w / 2 - 8, w - 30)) {
      c.fillRect(px, 12, 16, 46, 0xFF473B73.toInt())
      c.fillRect(px + 2, 14, 5, 42, 0xFF635499.toInt())
      c.fillRect(px + 7, 24, 2, 20, 0xFF38BDF8.toInt())
    }
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  /**
   * Oblique 3/4 Carved Stone Staircase with Side Balustrades & Glowing Crystal Newel Posts (Ref 4!).
   */
  private fun buildTerraceGrandStairs(): ImageBitmap {
    val w = 132
    val h = 76
    val c = PixelCanvas(w, h)
    val cx = w / 2
    for (step in 0..8) {
      val sy = 12 + step * 6
      // Sunlit horizontal tread + dark vertical riser
      c.fillRect(20, sy, w - 40, 3, 0xFF635599.toInt())
      c.hLine(20, sy, w - 40, 0xFF8878C2.toInt())
      c.fillRect(20, sy + 3, w - 40, 3, 0xFF211A3B.toInt())
      // Royal purple-gold center carpet inlay
      c.fillRect(cx - 16, sy, 32, 3, 0xFF4C1D95.toInt())
      c.vLine(cx - 17, sy, 6, 0xFFF5B841.toInt())
      c.vLine(cx + 16, sy, 6, 0xFFF5B841.toInt())
    }
    // Stepped side stone balustrade cheek-walls + glowing cyan crystal finials at bottom!
    for (step in 0..8) {
      val sy = 8 + step * 6
      c.fillRect(8, sy, 12, 14, 0xFF4E427A.toInt())
      c.fillRect(w - 20, sy, 12, 14, 0xFF423769.toInt())
      c.hLine(8, sy, 12, 0xFF7C6EB0.toInt())
      c.hLine(w - 20, sy, 12, 0xFF7C6EB0.toInt())
    }
    listOf(14, w - 14).forEach { fx ->
      c.fillCircle(fx, 52, 4, 0xFF38BDF8.toInt())
      c.set(fx, 51, 0xFFFFFFFF.toInt())
    }
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildStoneBalustradeRail(): ImageBitmap {
    val w = 112
    val h = 44
    val c = PixelCanvas(w, h)
    // Bottom plinth
    c.fillRect(6, 30, w - 12, 8, 0xFF322952.toInt())
    c.hLine(6, 30, w - 12, 0xFF5C4E8F.toInt())
    // Carved vertical stone balusters (visible gaps between balusters!)
    for (bx in 16..(w - 20) step 10) {
      c.fillRect(bx, 18, 5, 12, 0xFF4F437D.toInt())
      c.vLine(bx + 1, 18, 12, 0xFF7364A8.toInt())
    }
    // Top coping rail
    c.fillRect(6, 13, w - 12, 5, 0xFF635499.toInt())
    c.hLine(6, 13, w - 12, 0xFF9182C7.toInt())
    // Left & right newel posts with glowing blue crystals (Ref 4!)
    listOf(4, w - 16).forEach { px ->
      c.fillRect(px, 10, 12, 28, 0xFF473B73.toInt())
      c.hLine(px - 1, 10, 14, 0xFFF5B841.toInt())
      c.fillRect(px + 4, 3, 4, 7, 0xFF38BDF8.toInt())
      c.set(px + 5, 4, 0xFFFFFFFF.toInt())
    }
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  /**
   * 3/4 Wooden Street/Courtyard Bench with clearly visible backrest, slatted seat & legs!
   */
  private fun buildWoodenBench34(): ImageBitmap {
    val w = 58
    val h = 36
    val c = PixelCanvas(w, h)
    // Backrest slats & posts
    c.fillRect(8, 6, 4, 24, 0xFF4A2C1A.toInt())
    c.fillRect(w - 12, 6, 4, 24, 0xFF4A2C1A.toInt())
    c.fillRect(8, 8, w - 16, 4, 0xFF8C5835.toInt())
    c.hLine(8, 8, w - 16, 0xFFB5794E.toInt())
    c.fillRect(8, 14, w - 16, 4, 0xFF7A4B2A.toInt())
    // 3/4 Foreshortened Bench Seat
    c.fillRect(6, 19, w - 12, 6, 0xFF9C6641.toInt())
    c.hLine(6, 19, w - 12, 0xFFC48B60.toInt())
    c.hLine(6, 24, w - 12, 0xFF3B2214.toInt())
    // Visible front & side iron/oak legs
    for (lx in listOf(9, 27, w - 13)) {
      c.fillRect(lx, 25, 4, 7, 0xFF382114.toInt())
      c.vLine(lx + 1, 25, 6, 0xFF5E3924.toInt())
    }
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildArcaneBookshelf34(): ImageBitmap {
    val w = 56
    val h = 64
    val c = PixelCanvas(w, h)
    // 2.5D Oak Bookshelf Frame + Side Depth
    c.fillRect(6, 8, w - 16, 50, 0xFF472A18.toInt())
    c.fillRect(w - 10, 6, 5, 50, 0xFF2E1A0F.toInt())
    c.hLine(5, 8, w - 14, 0xFF9C6641.toInt())
    c.fillRect(9, 12, w - 22, 42, 0xFF24140D.toInt())
    for (ry in listOf(14, 27, 40)) {
      c.hLine(9, ry + 9, w - 22, 0xFF7A4B2A.toInt())
      for (bx in 11..(w - 17) step 4) {
        val col = when ((bx + ry) % 4) {
          0 -> 0xFFA855F7.toInt()
          1 -> 0xFF38BDF8.toInt()
          2 -> 0xFFF59E0B.toInt()
          else -> 0xFF10B981.toInt()
        }
        c.fillRect(bx, ry, 3, 9, col)
        c.hLine(bx, ry + 1, 3, 0xFFFDE047.toInt())
      }
    }
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildStreetLantern(frame: Int): ImageBitmap {
    val w = 32
    val h = 66
    val c = PixelCanvas(w, h)
    // Stepped iron base & fluted post
    c.fillRect(9, 54, 10, 8, 0xFF251F36.toInt())
    c.hLine(9, 54, 10, 0xFF4E436E.toInt())
    c.fillRect(12, 12, 4, 43, 0xFF352D4D.toInt())
    c.vLine(13, 12, 43, 0xFF5A4E80.toInt())
    // Curved wrought-iron crossarm
    c.fillRect(12, 12, 13, 3, 0xFF473D66.toInt())
    c.set(16, 15, 0xFF473D66.toInt())
    // Hanging Warm Amber Glass Lantern
    val glowRadius = if (frame == 1) 6 else 5
    c.fillCircle(22, 22, glowRadius, 0xFFF59E0B.toInt())
    c.fillRect(19, 17, 7, 10, 0xFFFBBF24.toInt())
    c.fillRect(21, 19, 3, 6, 0xFFFEF08A.toInt())
    c.hLine(18, 16, 9, 0xFF251F36.toInt())
    c.hLine(18, 27, 9, 0xFF251F36.toInt())
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildBarrelCluster(): ImageBitmap {
    val w = 52
    val h = 44
    val c = PixelCanvas(w, h)
    // 2.5D Wooden Supply Crate with top lid & side shadow
    c.fillRect(22, 12, 22, 22, 0xFF6E4427.toInt())
    c.fillRect(24, 10, 20, 4, 0xFFA66E46.toInt()) // Top 3/4 lid!
    c.fillRect(24, 15, 16, 16, 0xFF8C5835.toInt())
    c.fillRect(41, 12, 4, 22, 0xFF472A18.toInt()) // Shaded 2.5D side
    for (i in 0..12) c.set(25 + i, 16 + i, 0xFF5C371F.toInt()) // Diagonal crate brace

    // Two Stave-Banded 3/4 Oak Barrels with visible foreshortened top lids!
    listOf(6 to 12, 14 to 20).forEach { (bx, by) ->
      c.fillRect(bx, by + 2, 15, 18, 0xFF7A4B2A.toInt())
      c.fillRect(bx + 2, by + 3, 6, 16, 0xFF9E663E.toInt())
      c.hLine(bx, by + 6, 15, 0xFF334155.toInt())
      c.hLine(bx, by + 14, 15, 0xFF334155.toInt())
      c.fillEllipse(bx + 7, by + 2, 7, 3, 0xFFB5794E.toInt())
      c.strokeEllipse(bx + 7, by + 2, 7, 3, 0xFF472A18.toInt())
    }
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildStoneRetainingWall(): ImageBitmap {
    val w = 88
    val h = 48
    val c = PixelCanvas(w, h)
    c.drawStoneWall34(6, 14, w - 16, 28, sideDepthPx = 5, stoneBase = 0xFF544B6E.toInt(), stoneAlt = 0xFF635980.toInt(), stoneHi = 0xFF857AA6.toInt(), stoneShadow = 0xFF2E2840.toInt(), seed = 911)
    // Cascading emerald ivy & flowers over top stone coping (Ref 1!)
    c.drawOrganicLeafCluster(28, 14, 16, 8, 0xFF1B4332.toInt(), 0xFF2D6A4F.toInt(), 0xFF52B788.toInt(), 0xFF95D5B2.toInt(), seed = 912)
    c.drawOrganicLeafCluster(58, 15, 12, 7, 0xFF1B4332.toInt(), 0xFF2D6A4F.toInt(), 0xFF52B788.toInt(), 0xFF95D5B2.toInt(), seed = 913)
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildTavernCounterBar(): ImageBitmap {
    val w = 160
    val h = 80
    val c = PixelCanvas(w, h)
    // 2.5D Heavy Hand-Planed Oak Bar Counter (Top surface + Carved front panels + Side return!)
    c.fillRect(10, 30, w - 24, 40, 0xFF59351F.toInt())
    c.fillRect(w - 14, 26, 6, 42, 0xFF382012.toInt())
    c.fillRect(8, 20, w - 20, 12, 0xFF9E6740.toInt()) // Wide 3/4 countertop plane!
    c.hLine(8, 20, w - 20, 0xFFC78D63.toInt())
    c.hLine(8, 31, w - 20, 0xFF331D10.toInt())
    for (px in 18..(w - 32) step 20) {
      c.fillRect(px, 36, 14, 26, 0xFF704428.toInt())
      c.hLine(px, 36, 14, 0xFF8C5835.toInt())
    }
    // Food & Feast Props on Countertop
    c.fillEllipse(36, 25, 9, 4, 0xFFE2E8F0.toInt())
    c.fillCircle(35, 24, 3, 0xFFF97316.toInt())
    c.fillCircle(39, 25, 2, 0xFFFBBF24.toInt())
    c.fillRect(68, 22, 20, 7, 0xFFFEF3C7.toInt()) // Menu ledger
    c.fillEllipse(108, 25, 10, 4, 0xFFD97706.toInt()) // Roast platter
    c.fillRect(130, 19, 6, 9, 0xFFD97706.toInt()) // Frothy ale mug
    c.hLine(130, 19, 6, 0xFFF8FAFC.toInt())

    // 3/4 Wooden Bar Stools with visible legs!
    for (sx in listOf(34, 68, 102, 132)) {
      c.fillEllipse(sx, 62, 7, 4, 0xFF945E38.toInt())
      c.vLine(sx - 4, 64, 9, 0xFF472A18.toInt())
      c.vLine(sx, 65, 9, 0xFF361F12.toInt())
      c.vLine(sx + 4, 64, 9, 0xFF472A18.toInt())
    }
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildTavernShelfBarrels(): ImageBitmap {
    val w = 104
    val h = 78
    val c = PixelCanvas(w, h)
    c.fillRect(8, 8, w - 18, 60, 0xFF472A18.toInt())
    c.fillRect(w - 10, 6, 4, 60, 0xFF2B180E.toInt())
    c.fillRect(12, 12, w - 26, 52, 0xFF5E3821.toInt())
    c.hLine(10, 29, w - 22, 0xFF96613D.toInt())
    c.hLine(10, 47, w - 22, 0xFF96613D.toInt())
    for (bx in 16..50 step 6) {
      val col = if ((bx / 6) % 2 == 0) 0xFF10B981.toInt() else 0xFFF59E0B.toInt()
      c.fillRect(bx, 18, 4, 10, col)
      c.set(bx + 1, 17, 0xFFFEF3C7.toInt())
    }
    listOf(58 to 14, 76 to 14, 66 to 33).forEach { (cx, cy) ->
      c.fillRect(cx, cy, 18, 14, 0xFF7A4B2A.toInt())
      c.hLine(cx, cy + 3, 18, 0xFF9E663E.toInt())
      c.vLine(cx + 3, cy, 14, 0xFF334155.toInt())
      c.vLine(cx + 14, cy, 14, 0xFF334155.toInt())
    }
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildTavernFireplace(frame: Int): ImageBitmap {
    val w = 84
    val h = 94
    val c = PixelCanvas(w, h)
    c.drawStoneWall34(10, 6, w - 22, 80, sideDepthPx = 6, stoneBase = 0xFF575366.toInt(), stoneAlt = 0xFF68637A.toInt(), stoneHi = 0xFF857F99.toInt(), stoneShadow = 0xFF322F3D.toInt(), seed = 812)
    c.fillRect(6, 38, w - 14, 8, 0xFF704428.toInt())
    c.hLine(6, 38, w - 14, 0xFFA66D46.toInt())
    c.fillRect(20, 48, w - 42, 34, 0xFF1C0D0A.toInt())
    val shift = frame - 1
    c.fillCircle(w / 2 + shift * 2, 70, 13, 0xFFDC2626.toInt())
    c.fillCircle(w / 2 - shift, 72, 10, 0xFFF97316.toInt())
    c.fillCircle(w / 2 + shift, 74, 6, 0xFFFACC15.toInt())
    c.fillRect(24, 78, w - 50, 4, 0xFF451A03.toInt())
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  /**
   * Reference 2 Restaurant Interior: 3/4 Long Oak Banquet Table with Visible Table Legs,
   * Emerald Woven Runner, Benches with Legs, Roast Platter & Flickering Candle!
   */
  private fun buildTavernLongTable(): ImageBitmap {
    val w = 108
    val h = 68
    val c = PixelCanvas(w, h)
    // Back wooden bench (top plane + legs)
    c.fillRect(18, 8, w - 36, 7, 0xFF784B2A.toInt())
    c.hLine(18, 8, w - 36, 0xFF9E663E.toInt())
    c.fillRect(22, 15, 4, 5, 0xFF3D2415.toInt())
    c.fillRect(w - 26, 15, 4, 5, 0xFF3D2415.toInt())

    // Main 3/4 Oak Banquet Table Top + Apron + 4 Visible Heavy Wooden Legs!
    c.fillRect(10, 18, w - 20, 20, 0xFF945E38.toInt())
    c.hLine(10, 18, w - 20, 0xFFBD8054.toInt())
    c.fillRect(10, 38, w - 20, 5, 0xFF59351F.toInt()) // Table front thickness apron!
    for (lx in listOf(13, 38, w - 42, w - 17)) {
      c.fillRect(lx, 43, 4, 10, 0xFF3D2314.toInt())
      c.vLine(lx + 1, 43, 9, 0xFF633B22.toInt())
    }

    // Emerald-Green Woven Table Runner + Gold Fringe
    c.fillRect(22, 23, w - 44, 10, 0xFF2F6B48.toInt())
    c.hLine(22, 24, w - 44, 0xFF4A9468.toInt())
    c.hLine(22, 32, w - 44, 0xFFD99B26.toInt())

    // Plates, Golden Roast Platter & Table Candle
    c.fillEllipse(w / 2, 28, 9, 4, 0xFFE2E8F0.toInt())
    c.fillEllipse(w / 2, 27, 7, 3, 0xFFD97706.toInt())
    c.fillEllipse(30, 28, 4, 3, 0xFFF8FAFC.toInt())
    c.fillEllipse(w - 30, 28, 4, 3, 0xFFF8FAFC.toInt())
    c.vLine(w - 22, 20, 6, 0xFFFEF3C7.toInt())
    c.set(w - 22, 19, 0xFFFBBF24.toInt())

    // Foreground Wooden Bench with Visible Legs!
    c.fillRect(16, 48, w - 32, 7, 0xFF875636.toInt())
    c.hLine(16, 48, w - 32, 0xFFB0754D.toInt())
    c.fillRect(16, 55, w - 32, 3, 0xFF4F2F1B.toInt())
    for (blx in listOf(20, w / 2 - 2, w - 24)) {
      c.fillRect(blx, 58, 4, 6, 0xFF382012.toInt())
    }

    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildObsidianColumn(): ImageBitmap {
    val w = 44
    val h = 92
    val c = PixelCanvas(w, h)
    c.fillRect(6, 74, 32, 14, 0xFF251F3D.toInt())
    c.fillRect(8, 67, 28, 8, 0xFF382F5C.toInt())
    c.fillRect(11, 16, 22, 52, 0xFF2B2447.toInt())
    c.fillRect(13, 16, 6, 52, 0xFF4D417A.toInt())
    c.fillRect(26, 16, 5, 52, 0xFF1B162E.toInt())
    c.fillRect(21, 26, 3, 32, 0xFF38BDF8.toInt())
    c.fillRect(6, 8, 32, 9, 0xFF4D417A.toInt())
    c.hLine(6, 8, 32, 0xFFF5B841.toInt())
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildRoyalStatue(): ImageBitmap {
    val w = 52
    val h = 88
    val c = PixelCanvas(w, h)
    // Carved 3/4 Stone Pedestal
    c.drawStoneWall34(8, 58, 34, 24, sideDepthPx = 4, seed = 955)
    c.hLine(6, 58, 38, 0xFFF5B841.toInt())
    // Armored Guardian Knight Figure with Shield & Halberd
    c.fillRect(18, 36, 6, 22, 0xFF64748B.toInt())
    c.fillRect(25, 36, 6, 22, 0xFF475569.toInt())
    c.fillRect(16, 20, 18, 17, 0xFF94A3B8.toInt())
    c.fillRect(13, 19, 24, 5, 0xFFCBD5E1.toInt())
    c.fillRect(19, 10, 12, 10, 0xFFCBD5E1.toInt())
    c.hLine(21, 15, 8, 0xFF38BDF8.toInt())
    c.vLine(38, 4, 54, 0xFFF5B841.toInt()) // Halberd
    c.fillRect(36, 5, 5, 10, 0xFFE2E8F0.toInt())
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  /**
   * Multi-layered Handcrafted 3/4 Ancient Oak Tree (112 x 136 px) with Gnarled Roots,
   * Branching Limbs & Organic Scalloped Pixel Leaf Clusters (Zero flat geometric circles!).
   */
  private fun buildAncientOakTree(): ImageBitmap {
    val w = 112
    val h = 136
    val c = PixelCanvas(w, h)

    // Premium gnarled bark trunk — furrow, highlight, AO roots
    c.fillRect(45, 76, 22, 46, 0xFF3D251A.toInt())
    c.fillRect(48, 78, 8, 42, 0xFF633E2B.toInt())
    c.fillRect(60, 80, 5, 40, 0xFF26160F.toInt())
    c.vLine(53, 82, 34, 0xFF2A1A10.toInt()) // deep furrow
    c.vLine(50, 84, 28, 0xFF7A4B2E.toInt()) // sun highlight
    c.vLine(62, 86, 22, 0xFF1E120E.toInt()) // shadow side
    // Buttress roots with AO contact shadow
    c.fillRect(34, 114, 44, 10, 0xFF3D251A.toInt())
    c.fillRect(38, 112, 16, 7, 0xFF633E2B.toInt())
    c.fillRect(62, 115, 14, 7, 0xFF26160F.toInt())
    c.hLine(34, 124, 44, 0xFF1A120E.toInt())
    // Upper spreading boughs with highlight + secondary branches
    for (i in 0..14) {
      c.fillRect(46 - i, 78 - i, 7, 5, 0xFF4D2F21.toInt())
      c.fillRect(58 + i, 78 - i, 7, 5, 0xFF362016.toInt())
      if (i < 6) c.set(46 - i, 77 - i, 0xFF7A5A3E.toInt())
    }
    // Secondary branches — thin, connecting trunk to outer foliage masses
    c.fillRect(38, 58, 8, 2, 0xFF4A2E1D.toInt()); c.fillRect(40, 57, 6, 1, 0xFF6A4A32.toInt())
    c.fillRect(68, 60, 8, 2, 0xFF362016.toInt()); c.fillRect(66, 59, 6, 1, 0xFF4A2E1D.toInt())
    c.fillRect(44, 42, 6, 2, 0xFF5A3A24.toInt())
    // Ground AO oval under ancient oak
    c.fillEllipse(56, 128, 22, 6, 0xFF0A0C14.toInt())

    val sh = 0xFF0F1A1F.toInt()
    val mid = 0xFF1A2E1F.toInt()
    val br = 0xFF2F4A2E.toInt()
    val hi = 0xFF3D5A36.toInt()
    val crownHi = 0xFF6B8A5A.toInt()

    // Back shadow & lower bough leaf clusters - layered depth
    c.drawOrganicLeafCluster(30, 62, 24, 18, sh, mid, br, hi, seed = 701)
    c.drawOrganicLeafCluster(82, 64, 24, 18, sh, mid, br, hi, seed = 702)
    // Mid crown clusters with inner highlight veins
    c.drawOrganicLeafCluster(56, 48, 34, 24, sh, mid, br, hi, seed = 703)
    c.drawOrganicLeafCluster(34, 42, 22, 17, sh, mid, br, crownHi, seed = 704)
    c.drawOrganicLeafCluster(76, 44, 22, 17, sh, mid, br, crownHi, seed = 705)
    // Sunlit top crown - frost tip + scattered bright flecks
    c.drawOrganicLeafCluster(52, 26, 26, 18, mid, br, hi, crownHi, seed = 706)
    c.set(48, 20, 0xFF8AB87A.toInt())
    c.set(56, 18, 0xFF8AB87A.toInt())

    c.applyCrispOutline(0xFF0A1B21.toInt())
    return c.toImageBitmap()
  }

  /**
   * Mystic Dimensional Plum/Violet Blossom Tree from Reference Image 4's Sanctuary Terraces!
   */
  private fun buildMysticPlumTree(): ImageBitmap {
    val w = 108
    val h = 132
    val c = PixelCanvas(w, h)

    c.fillRect(44, 74, 20, 44, 0xFF332038.toInt())
    c.fillRect(47, 76, 7, 40, 0xFF523559.toInt())
    c.vLine(46, 76, 38, 0xFF6A4A7A.toInt()) // sun edge
    c.vLine(60, 78, 34, 0xFF1A0F22.toInt()) // shadow edge
    c.fillRect(35, 110, 38, 9, 0xFF332038.toInt())
    c.hLine(35, 119, 38, 0xFF1A0F22.toInt())
    for (i in 0..12) {
      c.fillRect(45 - i, 76 - i, 6, 5, 0xFF472D4F.toInt())
      c.fillRect(56 + i, 76 - i, 6, 5, 0xFF2A1930.toInt())
      if (i < 5) c.set(45 - i, 75 - i, 0xFF6A5290.toInt())
    }
    c.fillEllipse(54, 124, 18, 5, 0xFF0A0C14.toInt()) // AO

    val sh = 0xFF1A0F2A.toInt()
    val mid = 0xFF3A1F5A.toInt()
    val br = 0xFF5A2F7A.toInt()
    val hi = 0xFF8A6AB8.toInt()
    val bloom = 0xFFFAE8FF.toInt()

    c.drawOrganicLeafCluster(30, 60, 22, 16, sh, mid, br, hi, seed = 721)
    c.drawOrganicLeafCluster(78, 62, 22, 16, sh, mid, br, hi, seed = 722)
    c.drawOrganicLeafCluster(54, 46, 32, 22, sh, mid, br, hi, seed = 723)
    c.drawOrganicLeafCluster(48, 26, 24, 16, mid, br, hi, bloom, seed = 724)
    // bloom flecks
    c.set(46, 22, bloom)
    c.set(54, 20, 0xFFE8C8FF.toInt())

    c.applyCrispOutline(0xFF120824.toInt())
    return c.toImageBitmap()
  }

  private fun buildSpiritPine(): ImageBitmap {
    val w = 88
    val h = 124
    val c = PixelCanvas(w, h)
    c.fillRect(38, 84, 12, 32, 0xFF2E202E.toInt())
    c.fillRect(40, 84, 5, 30, 0xFF5A3D2E.toInt()) // warmer highlight
    c.vLine(44, 86, 28, 0xFF362A1A.toInt())
    for (tier in 0..4) {
      val baseY = 90 - tier * 16
      val maxSpan = 34 - tier * 5
      for (ry in 0..22) {
        val span = (22 - ry) * maxSpan / 22
        val jag = if (ry % 4 == 0) 2 else 0
        val col = when {
          ry < 5 -> 0xFF0F1A1F.toInt()
          ry < 11 -> 0xFF132A2E.toInt()
          ry < 17 -> 0xFF1B3A2E.toInt()
          ry < 20 -> 0xFF2F4A2E.toInt()
          else -> 0xFF3D5A36.toInt() // sun tip
        }
        c.hLine(44 - span - jag, baseY - ry, (span + jag) * 2, col)
        // snow/lichen speck on sun side
        if (ry == 19 && tier % 2 == 0) c.set(44 - span + 2, baseY - ry, 0xFF8A9AB8.toInt())
      }
    }
    c.fillEllipse(44, 116, 14, 4, 0xFF0A0C14.toInt()) // AO
    c.applyCrispOutline(0xFF08161F.toInt())
    return c.toImageBitmap()
  }

  private fun buildForestCanopyBush(): ImageBitmap {
    val w = 56
    val h = 44
    val c = PixelCanvas(w, h)
    c.drawOrganicLeafCluster(28, 24, 24, 15, 0xFF0F1E1B.toInt(), 0xFF1E2F24.toInt(), 0xFF2A3D2B.toInt(), 0xFF3D5A36.toInt(), seed = 740)
    listOf(18 to 20, 31 to 17, 37 to 25).forEach { (fx, fy) ->
      c.set(fx, fy, 0xFFFEF08A.toInt())
      c.set(fx + 1, fy, 0xFFF59E0B.toInt())
    }
    c.applyCrispOutline(0xFF0A1D21.toInt())
    return c.toImageBitmap()
  }

  private fun buildAncientRuinArch(): ImageBitmap {
    val w = 96
    val h = 86
    val c = PixelCanvas(w, h)
    c.drawStoneWall34(10, 24, 24, 52, sideDepthPx = 4, stoneBase = 0xFF5A5463.toInt(), stoneAlt = 0xFF6B6478.toInt(), seed = 944)
    c.drawStoneWall34(w - 38, 30, 24, 46, sideDepthPx = 4, stoneBase = 0xFF524C5C.toInt(), stoneAlt = 0xFF635C70.toInt(), seed = 945)
    c.drawStoneWall34(8, 14, 42, 14, sideDepthPx = 3, stoneBase = 0xFF666070.toInt(), stoneAlt = 0xFF787085.toInt(), seed = 946)
    c.drawOrganicLeafCluster(26, 14, 16, 7, 0xFF1B4332.toInt(), 0xFF3E7D3B.toInt(), 0xFF6AA63E.toInt(), 0xFF9FD64B.toInt(), seed = 947)
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildAncientMonolith(): ImageBitmap {
    val w = 44
    val h = 74
    val c = PixelCanvas(w, h)
    c.fillRect(10, 16, 24, 52, 0xFF2A2E4A.toInt()) // deeper indigo stone
    c.fillRect(12, 8, 20, 60, 0xFF3A3F5A.toInt())
    c.fillRect(14, 10, 7, 54, 0xFF5A5F7A.toInt()) // sun chamfer
    c.fillRect(26, 10, 4, 54, 0xFF1E1E2E.toInt()) // shadow side
    c.fillRect(12, 8, 16, 5, 0xFF2A3D2B.toInt())
    c.hLine(13, 8, 12, 0xFF3D5A36.toInt())
    c.set(18, 9, 0xFF4A6B3E.toInt()) // moss dot
    c.fillRect(21, 24, 3, 28, 0xFF38BDF8.toInt())
    c.hLine(16, 32, 13, 0xFF38BDF8.toInt())
    // weather cracks
    c.vLine(16, 16, 12, 0xFF1A1E24.toInt())
    c.fillEllipse(22, 68, 10, 4, 0xFF0A0C14.toInt()) // AO
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildMossyBoulder(): ImageBitmap {
    val w = 48
    val h = 38
    val c = PixelCanvas(w, h)
    c.fillEllipse(24, 22, 18, 12, 0xFF3A3F4A.toInt()) // cold stone base
    c.fillEllipse(19, 23, 13, 10, 0xFF4A4D55.toInt())
    c.fillEllipse(31, 25, 11, 8, 0xFF2E333E.toInt())
    // crack across boulder
    c.hLine(14, 22, 10, 0xFF1A1E24.toInt())
    c.set(18, 23, 0xFF1E2026.toInt())
    c.fillEllipse(21, 13, 13, 6, 0xFF2A3D2B.toInt()) // moss cap thicker
    c.fillEllipse(21, 11, 9, 4, 0xFF3D5A36.toInt())
    c.hLine(15, 11, 12, 0xFF4A6B3E.toInt())
    c.set(28, 18, 0xFF5A5F6E.toInt()) // quartz vein
    c.applyCrispOutline()
    return c.toImageBitmap()
  }

  private fun buildArcaneBrazier(frame: Int): ImageBitmap {
    val w = 36
    val h = 54
    val c = PixelCanvas(w, h)
    // Stone pedestal with highlight/so
    c.fillRect(11, 40, 14, 9, 0xFF27223D.toInt())
    c.hLine(11, 40, 14, 0xFF4A4E5E.toInt())
    c.fillRect(7, 28, 22, 12, 0xFF3A3F4A.toInt())
    c.hLine(7, 28, 22, 0xFF8A9AB8.toInt()) // cold moon rim
    c.fillRect(9, 30, 18, 8, 0xFF2A2D38.toInt()) // inner soot
    val shift = frame - 1
    // Triple flame layers: outer violet, mid cyan, core white with flicker shift
    c.fillCircle(18 + shift, 21, 9, 0xFF6B21A8.toInt())
    c.fillCircle(18 + shift, 21, 8, 0xFF7C3AED.toInt())
    c.fillCircle(18 - shift, 18, 6, 0xFF38BDF8.toInt())
    c.fillCircle(18, 18, 4, 0xFF7DD3FC.toInt())
    c.fillCircle(18 + shift, 13, 3, 0xFFE0F2FE.toInt())
    c.set(18, 11, 0xFFFFFFFF.toInt())
    c.set(16 + frame * 2, 7, 0xFFF59E0B.toInt()) // ember spark
    // Light pool on base
    c.fillEllipse(18, 38, 8, 2, 0xFF7C3AED.toInt())
    c.applyCrispOutline()
    return c.toImageBitmap()
  }
}
