package com.example.game.tower

import com.example.R
import com.example.data.model.FacilityType
import com.example.game.map.BiomeTheme
import com.example.game.map.EnemyArchetype
import com.example.game.map.EnemySpawnSpec
import com.example.game.map.PathSegment
import com.example.game.map.PropKind
import com.example.game.map.SanctuaryZoneZone
import com.example.game.map.TerrainPatch
import com.example.game.map.TowerFloorSpec
import com.example.game.map.WorldObstacle
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

data class GeneratedMapLayout(
  val width: Float,
  val height: Float,
  val wallMargin: Float,
  val biome: BiomeTheme,
  val patches: List<TerrainPatch>,
  val paths: List<PathSegment>,
  val obstacles: List<WorldObstacle>,
)

object TowerFloorCatalog {

  const val SANCTUARY_WIDTH = 3200f
  const val SANCTUARY_HEIGHT = 2760f
  const val SANCTUARY_WALL_MARGIN = 120f
  const val SANCTUARY_CENTER_X = 1940f
  const val SANCTUARY_PLAZA_Y = 1240f

  // Dedicated secondary 3/4 Sanctuary Landmarks (Interactive & Badged)
  const val HERO_REGISTRY_X = 785f
  const val HERO_REGISTRY_Y = 1185f
  const val FLOOR_VISTA_X = 1560f
  const val FLOOR_VISTA_Y = 425f

  const val RESTAURANT_INTERIOR_WIDTH = 1240f
  const val RESTAURANT_INTERIOR_HEIGHT = 860f
  const val RESTAURANT_INTERIOR_WALL_MARGIN = 82f
  const val RESTAURANT_EXIT_X = 620f
  const val RESTAURANT_EXIT_Y = 755f
  const val RESTAURANT_COUNTER_INTERACT_X = 620f
  const val RESTAURANT_COUNTER_INTERACT_Y = 335f

  const val DORMITORY_INTERIOR_WIDTH = 1220f
  const val DORMITORY_INTERIOR_HEIGHT = 860f
  const val DORMITORY_EXIT_X = 610f
  const val DORMITORY_EXIT_Y = 755f

  const val TRAINING_INTERIOR_WIDTH = 1280f
  const val TRAINING_INTERIOR_HEIGHT = 880f
  const val TRAINING_EXIT_X = 640f
  const val TRAINING_EXIT_Y = 775f

  const val ARMORY_INTERIOR_WIDTH = 1200f
  const val ARMORY_INTERIOR_HEIGHT = 840f
  const val ARMORY_EXIT_X = 600f
  const val ARMORY_EXIT_Y = 740f

  val SANCTUARY_ZONES: List<SanctuaryZoneZone> = listOf(
    SanctuaryZoneZone(
      facility = FacilityType.TOWER_GATE,
      centerX = 1960f, // Northern Obsidian Split-Monolith Portal Stairs
      centerY = 635f,
      interactRadius = 185f,
      colorArgb = 0xFF38BDF8L,
    ),
    SanctuaryZoneZone(
      facility = FacilityType.SUMMONING_HALL,
      centerX = 2380f, // Southeastern Classical Stepped Stone Amphitheatre
      centerY = 1930f,
      interactRadius = 210f,
      colorArgb = 0xFF60A5FAL,
    ),
    SanctuaryZoneZone(
      facility = FacilityType.TRAINING_GROUND,
      centerX = 890f, // Southwestern Open-Air Golden-Sand Training Grounds
      centerY = 1790f,
      interactRadius = 280f,
      colorArgb = 0xFFF59E0BL,
    ),
    SanctuaryZoneZone(
      facility = FacilityType.RESTAURANT,
      centerX = 1105f, // Inner Blue-Slate Dining Hall overlooking Hedge Courtyard
      centerY = 955f,
      interactRadius = 155f,
      colorArgb = 0xFFFBBF24L,
    ),
    SanctuaryZoneZone(
      facility = FacilityType.SYNTHESIS_LAB,
      centerX = 1270f, // Upper-Inner Blue-Slate Synthesis Manor
      centerY = 585f,
      interactRadius = 150f,
      colorArgb = 0xFF10B981L,
    ),
    SanctuaryZoneZone(
      facility = FacilityType.ARMORY,
      centerX = 995f, // NW Warm-Brown Smithy & Armory Guildhouse
      centerY = 685f,
      interactRadius = 150f,
      colorArgb = 0xFFEF4444L,
    ),
    SanctuaryZoneZone(
      facility = FacilityType.HERO_QUARTERS,
      centerX = 825f, // NW Warm-Brown Half-Timbered Hero Dormitories
      centerY = 820f,
      interactRadius = 155f,
      colorArgb = 0xFF60A5FAL,
    ),
  )

  fun buildSanctuaryMap(): GeneratedMapLayout {
    val patches = listOf(
      TerrainPatch(SANCTUARY_CENTER_X, SANCTUARY_PLAZA_Y, 540f, 380f, 0x18FFFFFFL),
      TerrainPatch(890f, 1790f, 410f, 250f, 0x18FDE68AL),
    )

    // Reference-Accurate Grey Cobblestone Streets & Avenues across the Floating Fortress-Island
    val paths = listOf(
      // 1. North Arched Tunnel Avenue -> Upper Cobble Bend -> Central West Spine
      PathSegment(1560f, 380f, 1560f, 665f, 118f, 0xFFAFAEB5L, 0xFF8C8B92L),
      PathSegment(1560f, 665f, 1330f, 880f, 118f, 0xFFAFAEB5L, 0xFF8C8B92L),
      PathSegment(1330f, 880f, 1330f, 1340f, 118f, 0xFFAFAEB5L, 0xFF8C8B92L),
      // 2. Diagonal Residential Street between Outer & Inner Half-Timbered House Rows
      PathSegment(585f, 1340f, 585f, 1150f, 102f, 0xFFAFAEB5L, 0xFF8C8B92L),
      PathSegment(585f, 1150f, 1165f, 675f, 102f, 0xFFAFAEB5L, 0xFF8C8B92L),
      // 3. Horizontal Mid-West Street (separating Residential District & SW Sand Training Field)
      PathSegment(345f, 1340f, 1330f, 1340f, 112f, 0xFFAFAEB5L, 0xFF8C8B92L),
      // 4. Central-South Cobblestone Spine down to the Southern Fortified Main Gate
      PathSegment(1330f, 1340f, 1545f, 1735f, 126f, 0xFFAFAEB5L, 0xFF8C8B92L),
      PathSegment(1545f, 1735f, 1610f, 2240f, 136f, 0xFFAFAEB5L, 0xFF8C8B92L),
    )

    val obstacles = mutableListOf<WorldObstacle>()

    // 1. Northern Obsidian Split-Monolith Tower Gate (with central sky cleft & azure portal)
    obstacles += WorldObstacle(
      id = "monument_tower_gate",
      x = 1960f,
      y = 585f,
      radius = 96f,
      blocksMovement = true,
      propKind = PropKind.FACILITY_TOWER_GATE,
      scale = 0.96f,
      footprintHalfW = 155f,
      footprintDepthNorth = 110f,
    )

    // 2. Southeastern Classical Stepped Stone Amphitheatre & 9 Fluted Marble Columns (Summoning Hall)
    obstacles += WorldObstacle(
      id = "building_summon_hall",
      x = 2430f,
      y = 2005f,
      radius = 115f,
      blocksMovement = true,
      propKind = PropKind.FACILITY_SUMMONING_HALL,
      scale = 1.08f,
      footprintHalfW = 195f,
      footprintDepthNorth = 135f,
    )

    // 3. Outer NW Residential Row (4 Half-Timbered Houses stepping down-left along NW Fortress Wall)
    obstacles += WorldObstacle(
      id = "building_armory",
      x = 995f,
      y = 655f,
      radius = 68f,
      blocksMovement = true,
      propKind = PropKind.FACILITY_ARMORY,
      scale = 0.80f,
      footprintHalfW = 72f,
      footprintDepthNorth = 54f,
    )
    obstacles += WorldObstacle(
      id = "building_hero_quarters",
      x = 825f,
      y = 790f,
      radius = 68f,
      blocksMovement = true,
      propKind = PropKind.FACILITY_HERO_QUARTERS,
      scale = 0.80f,
      footprintHalfW = 74f,
      footprintDepthNorth = 54f,
    )
    obstacles += WorldObstacle(
      id = "nw_outer_slate_house",
      x = 655f,
      y = 930f,
      radius = 62f,
      blocksMovement = true,
      propKind = PropKind.MEDIEVAL_TOWNHOUSE_B,
      scale = 0.84f,
      footprintHalfW = 64f,
      footprintDepthNorth = 50f,
    )
    obstacles += WorldObstacle(
      id = "nw_outer_brown_cottage",
      x = 495f,
      y = 1075f,
      radius = 58f,
      blocksMovement = true,
      propKind = PropKind.SANCTUARY_COTTAGE_BROWN,
      scale = 0.86f,
      footprintHalfW = 58f,
      footprintDepthNorth = 46f,
    )

    // 4. Inner Residential & Guild Row (Synthesis Manor + Adjacent Slate Cottage + Restaurant + Hero Registry)
    obstacles += WorldObstacle(
      id = "building_synthesis_lab",
      x = 1270f,
      y = 555f,
      radius = 66f,
      blocksMovement = true,
      propKind = PropKind.FACILITY_SYNTHESIS_LAB,
      scale = 0.80f,
      footprintHalfW = 70f,
      footprintDepthNorth = 52f,
    )
    obstacles += WorldObstacle(
      id = "inner_slate_wing_cottage",
      x = 1395f,
      y = 675f,
      radius = 54f,
      blocksMovement = true,
      propKind = PropKind.SANCTUARY_COTTAGE_SLATE,
      scale = 0.82f,
      footprintHalfW = 54f,
      footprintDepthNorth = 44f,
    )
    obstacles += WorldObstacle(
      id = "building_restaurant",
      x = 1105f,
      y = 925f,
      radius = 68f,
      blocksMovement = true,
      propKind = PropKind.FACILITY_RESTAURANT,
      scale = 0.80f,
      footprintHalfW = 74f,
      footprintDepthNorth = 54f,
    )
    obstacles += WorldObstacle(
      id = "building_hero_registry",
      x = HERO_REGISTRY_X,
      y = HERO_REGISTRY_Y,
      radius = 66f,
      blocksMovement = true,
      propKind = PropKind.FACILITY_HERO_REGISTRY,
      scale = 0.80f,
      footprintHalfW = 72f,
      footprintDepthNorth = 54f,
    )

    // 5. Perimeter Pale-Limestone Fortress Corner Towers & Twin Southern Gatehouse Towers
    val cornerTowerSpots = listOf(
      980f to 355f,
      2240f to 355f,
      2890f to 895f,
      2890f to 1790f,
      2320f to 2375f,
      900f to 2375f,
      310f to 1790f,
      310f to 895f,
    )
    cornerTowerSpots.forEachIndexed { idx, (tx, ty) ->
      obstacles += WorldObstacle(
        id = "fortress_corner_tower_$idx",
        x = tx,
        y = ty,
        radius = 36f,
        blocksMovement = true,
        propKind = PropKind.FORTRESS_CORNER_TOWER,
        scale = 0.96f,
        footprintHalfW = 36f,
        footprintDepthNorth = 28f,
      )
    }
    obstacles += WorldObstacle(
      id = "south_gate_tower_west",
      x = 1360f,
      y = 2395f,
      radius = 44f,
      blocksMovement = true,
      propKind = PropKind.SOUTHERN_GATE_TOWER,
      scale = 1.02f,
      footprintHalfW = 44f,
      footprintDepthNorth = 32f,
    )
    obstacles += WorldObstacle(
      id = "south_gate_tower_east",
      x = 1860f,
      y = 2395f,
      radius = 44f,
      blocksMovement = true,
      propKind = PropKind.SOUTHERN_GATE_TOWER,
      scale = 1.02f,
      footprintHalfW = 44f,
      footprintDepthNorth = 32f,
    )

    // 6. Lush Pink Sakura Cherry Blossom Trees, Pointed Cypress Conifers & Courtyard Hedges
    val sakuraSpots = listOf(
      // Flanking North Arched Tunnel & Northern Obsidian Monolith Gate
      1435f to 490f,
      1690f to 495f,
      2245f to 520f,
      2385f to 605f,
      2520f to 725f,
      // Eastern Sakura Garden bordering the Circular White Plaza
      2645f to 890f,
      2765f to 1095f,
      2775f to 1355f,
      2715f to 1590f,
      // Western & Southern Borders of the Golden-Sand Training Grounds
      415f to 1485f,
      535f to 1475f,
      410f to 1655f,
      980f to 2245f,
      1115f to 2250f,
      // Central-South Sakura Lawn & SE Gate Flank
      1715f to 1845f,
      1865f to 2025f,
      2010f to 2240f,
      2135f to 2248f,
    )
    sakuraSpots.forEachIndexed { idx, (sx, sy) ->
      obstacles += WorldObstacle(
        id = "sanctuary_sakura_$idx",
        x = sx,
        y = sy,
        radius = 22f,
        blocksMovement = true,
        propKind = PropKind.SAKURA_BLOSSOM_TREE,
        scale = 0.92f + (idx % 3) * 0.06f,
        occludesUpperCanopy = true,
        footprintHalfW = 22f,
        footprintDepthNorth = 16f,
      )
    }

    // Pointed green cypress conifers in the upper residential lawn
    listOf(1060f to 485f, 1155f to 470f, 1485f to 655f).forEachIndexed { idx, (cx, cy) ->
      obstacles += WorldObstacle(
        id = "sanctuary_cypress_$idx",
        x = cx,
        y = cy,
        radius = 18f,
        blocksMovement = true,
        propKind = PropKind.CYPRESS_CONIFER_TREE,
        scale = 0.94f,
        footprintHalfW = 18f,
        footprintDepthNorth = 14f,
      )
    }

    // Sculpted round boxwood garden bushes in the residential hedge courtyard & north lawn
    listOf(
      1100f to 565f,
      965f to 1055f,
      1065f to 1095f,
      1215f to 1045f,
      2355f to 735f,
    ).forEachIndexed { idx, (hx, hy) ->
      obstacles += WorldObstacle(
        id = "sanctuary_hedge_bush_$idx",
        x = hx,
        y = hy,
        radius = 15f,
        blocksMovement = true,
        propKind = PropKind.GARDEN_HEDGE_BUSH,
        scale = 0.90f,
      )
    }

    // 7. Southwestern Golden-Sand Training Arena: 10 Martial T-Dummies, 4 Weapon Racks & 1 Tall Striking Post!
    val sandDummySpots = listOf(
      620f to 1595f,
      710f to 1645f,
      645f to 1745f,
      745f to 1715f,
      625f to 1905f,
      735f to 1935f,
      945f to 1925f,
      1015f to 1985f,
      1085f to 1665f,
      1145f to 1915f,
    )
    sandDummySpots.forEachIndexed { idx, (dx, dy) ->
      obstacles += WorldObstacle(
        id = "sand_training_dummy_$idx",
        x = dx,
        y = dy,
        radius = 15f,
        blocksMovement = true,
        propKind = PropKind.TRAINING_DUMMY,
        scale = 0.96f,
      )
    }

    // 4 Wooden Practice Weapon Racks framing the Golden-Sand Arena
    listOf(
      545f to 1665f,
      835f to 1545f,
      1025f to 1545f,
      815f to 2065f,
      1035f to 2070f,
    ).forEachIndexed { idx, (rx, ry) ->
      obstacles += WorldObstacle(
        id = "sand_weapon_rack_$idx",
        x = rx,
        y = ry,
        radius = 22f,
        blocksMovement = true,
        propKind = PropKind.WEAPON_RACK,
        scale = 0.94f,
      )
    }

    // Tall Wooden Martial Striking Post on the East edge of the Golden-Sand Arena
    obstacles += WorldObstacle(
      id = "sand_striking_post_east",
      x = 1255f,
      y = 1825f,
      radius = 16f,
      blocksMovement = true,
      propKind = PropKind.TRAINING_STRIKING_POST,
      scale = 1.0f,
    )

    // Twin T-Bar Street Lanterns flanking the Southern Fortified Main Gate Bridge
    obstacles += WorldObstacle(
      id = "south_gate_lantern_west",
      x = 1490f,
      y = 2155f,
      radius = 15f,
      blocksMovement = true,
      propKind = PropKind.STREET_LANTERN,
      scale = 1.02f,
    )
    obstacles += WorldObstacle(
      id = "south_gate_lantern_east",
      x = 1730f,
      y = 2155f,
      radius = 15f,
      blocksMovement = true,
      propKind = PropKind.STREET_LANTERN,
      scale = 1.02f,
    )

    // 8. Organized Garden Benches, Street Lanterns & Multi-Flower Planters around Dorms, Training & Plaza
    listOf(
      755f to 875f,
      1035f to 990f,
      1435f to 1085f,
      2520f to 1125f,
    ).forEachIndexed { idx, (bx, by) ->
      obstacles += WorldObstacle(
        id = "sanctuary_garden_bench_$idx",
        x = bx,
        y = by,
        radius = 18f,
        blocksMovement = true,
        propKind = PropKind.WOODEN_BENCH_34,
        scale = 0.94f,
      )
    }

    listOf(
      900f to 845f,
      1215f to 965f,
      650f to 1465f,
      1155f to 1465f,
      1435f to 965f,
      2315f to 835f,
      2565f to 1325f,
    ).forEachIndexed { idx, (fx, fy) ->
      obstacles += WorldObstacle(
        id = "sanctuary_flower_planter_$idx",
        x = fx,
        y = fy,
        radius = 18f,
        blocksMovement = true,
        propKind = PropKind.FLOWER_PLANTER_BED,
        scale = 0.96f,
      )
    }

    listOf(
      885f to 915f,
      1425f to 855f,
      1435f to 1245f,
      2075f to 675f,
    ).forEachIndexed { idx, (lx, ly) ->
      obstacles += WorldObstacle(
        id = "sanctuary_avenue_lantern_$idx",
        x = lx,
        y = ly,
        radius = 14f,
        blocksMovement = true,
        propKind = PropKind.STREET_LANTERN,
        scale = 0.96f,
      )
    }

    return GeneratedMapLayout(
      width = SANCTUARY_WIDTH,
      height = SANCTUARY_HEIGHT,
      wallMargin = SANCTUARY_WALL_MARGIN,
      biome = BiomeTheme.SANCTUARY_ASTRAL,
      patches = patches,
      paths = paths,
      obstacles = obstacles,
    )
  }

  /**
   * Playable, Inhabited 3/4 Fantasy Restaurant Interior modeled directly after Reference 2!
   * Features half-timbered back wall + mezzanine balcony, warm oak floorboards, central flagstone
   * aisle, stocked Chef's bar counter, cellar barrels & bottle shelves, blazing stone fireplace,
   * 6 wooden banquet tables with visible 3/4 legs & benches, and southern exit mat.
   */
  fun buildRestaurantInteriorMap(): GeneratedMapLayout {
    val patches = listOf(
      TerrainPatch(215f, 315f, 240f, 190f, 0xFF7C3F18L),
      TerrainPatch(620f, 345f, 280f, 180f, 0xFF6E4720L),
      TerrainPatch(320f, 560f, 240f, 180f, 0xFF5C3B1EL),
      TerrainPatch(920f, 560f, 240f, 180f, 0xFF5C3B1EL),
    )

    val paths = listOf(
      PathSegment(620f, 790f, 620f, 290f, 152f, 0xFF878378L, 0xFF4A4740L),
      PathSegment(280f, 440f, 960f, 440f, 102f, 0xFF878378L, 0xFF4A4740L),
    )

    val obstacles = listOf(
      // Back Wall Hearth, Barrels/Shelves & Stocked Chef Counter (with accurate 3/4 footprints)
      WorldObstacle("tav_fireplace", 205f, 225f, 42f, true, PropKind.TAVERN_HEARTH_FIREPLACE, 1.05f, footprintHalfW = 64f, footprintDepthNorth = 48f),
      WorldObstacle("tav_shelves_l", 390f, 205f, 40f, true, PropKind.TAVERN_SHELF_BARRELS, 1.02f, footprintHalfW = 72f, footprintDepthNorth = 42f),
      WorldObstacle("tav_counter", 620f, 255f, 64f, true, PropKind.TAVERN_COUNTER_BAR, 1.08f, footprintHalfW = 128f, footprintDepthNorth = 56f),
      WorldObstacle("tav_shelves_r", 855f, 205f, 40f, true, PropKind.TAVERN_SHELF_BARRELS, 1.02f, footprintHalfW = 72f, footprintDepthNorth = 42f),
      WorldObstacle("tav_barrels_ne", 1045f, 235f, 24f, true, PropKind.BARREL_CLUSTER, 1.10f, footprintHalfW = 34f, footprintDepthNorth = 22f),

      // West Dining Wing 3/4 Banquet Tables (visible legs, emerald runners, candles & benches!)
      WorldObstacle("tav_table_w1", 310f, 380f, 42f, true, PropKind.TAVERN_LONG_TABLE, 1.0f, footprintHalfW = 78f, footprintDepthNorth = 44f),
      WorldObstacle("tav_table_w2", 295f, 565f, 42f, true, PropKind.TAVERN_LONG_TABLE, 1.02f, footprintHalfW = 78f, footprintDepthNorth = 44f),
      WorldObstacle("tav_table_w3", 380f, 700f, 42f, true, PropKind.TAVERN_LONG_TABLE, 0.98f, footprintHalfW = 76f, footprintDepthNorth = 42f),

      // East Dining Wing 3/4 Banquet Tables
      WorldObstacle("tav_table_e1", 930f, 380f, 42f, true, PropKind.TAVERN_LONG_TABLE, 1.0f, footprintHalfW = 78f, footprintDepthNorth = 44f),
      WorldObstacle("tav_table_e2", 945f, 565f, 42f, true, PropKind.TAVERN_LONG_TABLE, 1.02f, footprintHalfW = 78f, footprintDepthNorth = 44f),
      WorldObstacle("tav_table_e3", 860f, 700f, 42f, true, PropKind.TAVERN_LONG_TABLE, 0.98f, footprintHalfW = 76f, footprintDepthNorth = 42f),

      // Corner Barrels & Warm Interior Lantern Posts
      WorldObstacle("tav_barrels_sw", 155f, 715f, 22f, true, PropKind.BARREL_CLUSTER, 1.05f, footprintHalfW = 32f, footprintDepthNorth = 20f),
      WorldObstacle("tav_barrels_se", 1085f, 715f, 22f, true, PropKind.BARREL_CLUSTER, 1.05f, footprintHalfW = 32f, footprintDepthNorth = 20f),
      WorldObstacle("tav_lantern_l", 485f, 335f, 13f, true, PropKind.STREET_LANTERN, 0.96f),
      WorldObstacle("tav_lantern_r", 755f, 335f, 13f, true, PropKind.STREET_LANTERN, 0.96f),

      // Southern Doorway Threshold Mat
      WorldObstacle("tav_exit_mat", RESTAURANT_EXIT_X, RESTAURANT_EXIT_Y, 26f, false, PropKind.TAVERN_EXIT_MAT, 1.0f, sortYBias = -120f),
    )

    return GeneratedMapLayout(
      width = RESTAURANT_INTERIOR_WIDTH,
      height = RESTAURANT_INTERIOR_HEIGHT,
      wallMargin = RESTAURANT_INTERIOR_WALL_MARGIN,
      biome = BiomeTheme.RESTAURANT_INTERIOR,
      patches = patches,
      paths = paths,
      obstacles = obstacles,
    )
  }

  /**
   * Playable 3/4 Hero Dormitory & Recovery Infirmary Interior (HERO_QUARTERS):
   * 6 furnished recovery/sleeping beds across West & East wings, personal hero footlockers,
   * central Infirmary apothecary table, warm stone hearth, communal table & exit mat.
   */
  fun buildDormitoryInteriorMap(): GeneratedMapLayout {
    val patches = listOf(
      TerrainPatch(610f, 290f, 260f, 170f, 0xFF7C3F18L),
      TerrainPatch(305f, 440f, 220f, 180f, 0xFF6B3E26L),
      TerrainPatch(915f, 440f, 220f, 180f, 0xFF6B3E26L),
    )
    val paths = listOf(
      PathSegment(610f, 785f, 610f, 275f, 146f, 0xFF878378L, 0xFF4A4740L),
      PathSegment(300f, 635f, 920f, 635f, 96f, 0xFF878378L, 0xFF4A4740L),
    )
    val obstacles = listOf(
      // Back wall warm hearth, bookshelves & central Infirmary Apothecary Care Table
      WorldObstacle("dorm_hearth", 225f, 220f, 40f, true, PropKind.TAVERN_HEARTH_FIREPLACE, 1.02f, footprintHalfW = 62f, footprintDepthNorth = 46f),
      WorldObstacle("dorm_books_l", 405f, 205f, 32f, true, PropKind.ARCANE_BOOKSHELF_34, 0.98f, footprintHalfW = 46f, footprintDepthNorth = 28f),
      WorldObstacle("dorm_care_table", 610f, 255f, 44f, true, PropKind.INFIRMARY_CARE_TABLE, 1.12f, footprintHalfW = 54f, footprintDepthNorth = 32f),
      WorldObstacle("dorm_books_r", 815f, 205f, 32f, true, PropKind.ARCANE_BOOKSHELF_34, 0.98f, footprintHalfW = 46f, footprintDepthNorth = 28f),
      WorldObstacle("dorm_chest_ne", 995f, 220f, 26f, true, PropKind.DORM_PERSONAL_CHEST, 1.04f, footprintHalfW = 28f, footprintDepthNorth = 20f),

      // 6 Recovery & Living Beds (3 West Wing, 3 East Wing) + Personal Hero Footlockers
      WorldObstacle("dorm_bed_0", 265f, 335f, 28f, true, PropKind.RECOVERY_BED, 1.18f, footprintHalfW = 36f, footprintDepthNorth = 24f),
      WorldObstacle("dorm_bed_1", 265f, 445f, 28f, true, PropKind.RECOVERY_BED, 1.18f, footprintHalfW = 36f, footprintDepthNorth = 24f),
      WorldObstacle("dorm_bed_2", 265f, 555f, 28f, true, PropKind.RECOVERY_BED, 1.18f, footprintHalfW = 36f, footprintDepthNorth = 24f),
      WorldObstacle("dorm_bed_3", 955f, 335f, 28f, true, PropKind.RECOVERY_BED, 1.18f, footprintHalfW = 36f, footprintDepthNorth = 24f),
      WorldObstacle("dorm_bed_4", 955f, 445f, 28f, true, PropKind.RECOVERY_BED, 1.18f, footprintHalfW = 36f, footprintDepthNorth = 24f),
      WorldObstacle("dorm_bed_5", 955f, 555f, 28f, true, PropKind.RECOVERY_BED, 1.18f, footprintHalfW = 36f, footprintDepthNorth = 24f),

      WorldObstacle("dorm_locker_w", 375f, 335f, 20f, true, PropKind.DORM_PERSONAL_CHEST, 0.94f),
      WorldObstacle("dorm_locker_e", 845f, 335f, 20f, true, PropKind.DORM_PERSONAL_CHEST, 0.94f),
      WorldObstacle("dorm_bench_l", 445f, 685f, 22f, true, PropKind.WOODEN_BENCH_34, 0.96f),
      WorldObstacle("dorm_bench_r", 775f, 685f, 22f, true, PropKind.WOODEN_BENCH_34, 0.96f),
      WorldObstacle("dorm_lantern_l", 480f, 325f, 13f, true, PropKind.STREET_LANTERN, 0.94f),
      WorldObstacle("dorm_lantern_r", 740f, 325f, 13f, true, PropKind.STREET_LANTERN, 0.94f),
      WorldObstacle("dorm_exit_mat", DORMITORY_EXIT_X, DORMITORY_EXIT_Y, 26f, false, PropKind.TAVERN_EXIT_MAT, 1.0f, sortYBias = -120f),
    )
    return GeneratedMapLayout(
      width = DORMITORY_INTERIOR_WIDTH,
      height = DORMITORY_INTERIOR_HEIGHT,
      wallMargin = 82f,
      biome = BiomeTheme.DORMITORY_INTERIOR,
      patches = patches,
      paths = paths,
      obstacles = obstacles,
    )
  }

  /**
   * Playable 3/4 Indoor Training Dojo Hall (TRAINING_GROUND):
   * Practice targets, striking posts, weapon racks, central sparring circle & arcane braziers.
   */
  fun buildTrainingHallInteriorMap(): GeneratedMapLayout {
    val patches = listOf(
      TerrainPatch(640f, 465f, 275f, 195f, 0xFFF59E0BL),
      TerrainPatch(290f, 385f, 185f, 145f, 0xFF38BDF8L),
      TerrainPatch(990f, 385f, 185f, 145f, 0xFF38BDF8L),
    )
    val paths = listOf(
      PathSegment(640f, 800f, 640f, 245f, 160f, 0xFFAFAEB5L, 0xFF6B665EL),
    )
    val obstacles = listOf(
      WorldObstacle("dojo_rack_nw", 290f, 225f, 26f, true, PropKind.WEAPON_RACK, 1.04f),
      WorldObstacle("dojo_rack_n_mid", 640f, 220f, 28f, true, PropKind.WEAPON_RACK, 1.08f),
      WorldObstacle("dojo_rack_ne", 990f, 225f, 26f, true, PropKind.WEAPON_RACK, 1.04f),
      WorldObstacle("dojo_brazier_l", 465f, 235f, 18f, true, PropKind.ARCANE_BRAZIER, 0.98f),
      WorldObstacle("dojo_brazier_r", 815f, 235f, 18f, true, PropKind.ARCANE_BRAZIER, 0.98f),

      // West & East Practice Dummy Stations
      WorldObstacle("dojo_dummy_w1", 265f, 365f, 18f, true, PropKind.TRAINING_DUMMY, 1.04f),
      WorldObstacle("dojo_dummy_w2", 265f, 520f, 18f, true, PropKind.TRAINING_DUMMY, 1.04f),
      WorldObstacle("dojo_post_w", 365f, 445f, 18f, true, PropKind.TRAINING_STRIKING_POST, 1.04f),

      WorldObstacle("dojo_dummy_e1", 1015f, 365f, 18f, true, PropKind.TRAINING_DUMMY, 1.04f),
      WorldObstacle("dojo_dummy_e2", 1015f, 520f, 18f, true, PropKind.TRAINING_DUMMY, 1.04f),
      WorldObstacle("dojo_post_e", 915f, 445f, 18f, true, PropKind.TRAINING_STRIKING_POST, 1.04f),

      WorldObstacle("dojo_bench_sw", 330f, 705f, 22f, true, PropKind.WOODEN_BENCH_34, 0.96f),
      WorldObstacle("dojo_bench_se", 950f, 705f, 22f, true, PropKind.WOODEN_BENCH_34, 0.96f),
      WorldObstacle("dojo_exit_mat", TRAINING_EXIT_X, TRAINING_EXIT_Y, 26f, false, PropKind.TAVERN_EXIT_MAT, 1.0f, sortYBias = -120f),
    )
    return GeneratedMapLayout(
      width = TRAINING_INTERIOR_WIDTH,
      height = TRAINING_INTERIOR_HEIGHT,
      wallMargin = 82f,
      biome = BiomeTheme.TRAINING_HALL_INTERIOR,
      patches = patches,
      paths = paths,
      obstacles = obstacles,
    )
  }

  /**
   * Playable 3/4 Armory & Forge Interior (ARMORY):
   * Blacksmith anvil station, weapon racks, plate armor stands, equipment inspection table & storage chests.
   */
  fun buildArmoryInteriorMap(): GeneratedMapLayout {
    val patches = listOf(
      TerrainPatch(600f, 315f, 255f, 165f, 0xFFFB923CL),
      TerrainPatch(290f, 485f, 190f, 150f, 0xFF38BDF8L),
      TerrainPatch(910f, 485f, 190f, 150f, 0xFF38BDF8L),
    )
    val paths = listOf(
      PathSegment(600f, 765f, 600f, 285f, 148f, 0xFF64748BL, 0xFF334155L),
      PathSegment(285f, 485f, 915f, 485f, 96f, 0xFF64748BL, 0xFF334155L),
    )
    val obstacles = listOf(
      // Central Blacksmith Anvil & Quenching Barrel in front of the glowing smelter arch
      WorldObstacle("armory_anvil", 600f, 285f, 36f, true, PropKind.FORGE_ANVIL_STATION, 1.15f, footprintHalfW = 44f, footprintDepthNorth = 28f),
      WorldObstacle("armory_rack_nw1", 285f, 230f, 26f, true, PropKind.WEAPON_RACK, 1.06f),
      WorldObstacle("armory_rack_nw2", 415f, 230f, 26f, true, PropKind.WEAPON_RACK, 1.06f),
      WorldObstacle("armory_rack_ne1", 785f, 230f, 26f, true, PropKind.WEAPON_RACK, 1.06f),
      WorldObstacle("armory_rack_ne2", 915f, 230f, 26f, true, PropKind.WEAPON_RACK, 1.06f),

      // Plate Armor Stands & Equipment Inspection Tables
      WorldObstacle("armory_stand_w1", 245f, 385f, 22f, true, PropKind.ARMOR_STAND_34, 1.08f),
      WorldObstacle("armory_stand_w2", 245f, 555f, 22f, true, PropKind.ARMOR_STAND_34, 1.08f),
      WorldObstacle("armory_stand_e1", 955f, 385f, 22f, true, PropKind.ARMOR_STAND_34, 1.08f),
      WorldObstacle("armory_stand_e2", 955f, 555f, 22f, true, PropKind.ARMOR_STAND_34, 1.08f),

      WorldObstacle("armory_table_w", 375f, 615f, 38f, true, PropKind.TAVERN_LONG_TABLE, 0.92f, footprintHalfW = 68f, footprintDepthNorth = 38f),
      WorldObstacle("armory_chest_e1", 815f, 615f, 22f, true, PropKind.DORM_PERSONAL_CHEST, 1.04f),
      WorldObstacle("armory_chest_e2", 895f, 615f, 22f, true, PropKind.DORM_PERSONAL_CHEST, 1.04f),
      WorldObstacle("armory_barrels_sw", 175f, 685f, 22f, true, PropKind.BARREL_CLUSTER, 1.02f),
      WorldObstacle("armory_exit_mat", ARMORY_EXIT_X, ARMORY_EXIT_Y, 26f, false, PropKind.TAVERN_EXIT_MAT, 1.0f, sortYBias = -120f),
    )
    return GeneratedMapLayout(
      width = ARMORY_INTERIOR_WIDTH,
      height = ARMORY_INTERIOR_HEIGHT,
      wallMargin = 80f,
      biome = BiomeTheme.ARMORY_INTERIOR,
      patches = patches,
      paths = paths,
      obstacles = obstacles,
    )
  }

  fun interiorExitPoint(facility: FacilityType): Pair<Float, Float> = when (facility) {
    FacilityType.HERO_QUARTERS -> DORMITORY_EXIT_X to DORMITORY_EXIT_Y
    FacilityType.TRAINING_GROUND -> TRAINING_EXIT_X to TRAINING_EXIT_Y
    FacilityType.ARMORY -> ARMORY_EXIT_X to ARMORY_EXIT_Y
    else -> RESTAURANT_EXIT_X to RESTAURANT_EXIT_Y
  }

  val FLOORS: List<TowerFloorSpec> = listOf(
    // FLOOR 01 — VERDANT FRONTIER (Subjugation & Scouting)
    TowerFloorSpec(
      floorNumber = 1,
      codeName = "FLOOR 01 — VERDANT FRONTIER",
      titleVi = "Cánh Đồng Khởi Nguyên",
      subtitleVi = "Thảo nguyên biên giới ngập nắng và con đường đá dẫn tới Phế Tích Bia Đá",
      missionTypeVi = "Chinh Phạt (Subjugation)",
      objectiveVi = "Quét sạch toán trinh sát Goblin trên đường đá cổ và hạ gục Thủ Lĩnh Kraag tại Phế Tích Bia Đá.",
      descriptionVi = "Tầng đầu tiên của Tòa Tháp mở ra vùng thảo nguyên xanh thẫm đẫm ánh nắng xuyên qua tán lá cổ thụ. Các lối mòn phía Bắc và phía Nam dẫn qua phế tích đá rêu phong, nơi toán Goblin Trinh Sát và bầy Sói Hoang đang phục kích.",
      bannerRes = R.drawable.img_floor01_field,
      biome = BiomeTheme.FIRST_FIELD_MEADOW,
      mapWidth = 3400f,
      mapHeight = 2150f,
      recommendedPower = 380,
      rewardGold = 275,
      rewardMana = 65,
      rewardShards = 23,
      knownMonstersVi = listOf("Goblin Trinh Sát", "Goblin Xạ Tiễn", "Sói Hoang Gió Lốc", "Thủ Lĩnh Kraag (Boss)"),
      spawnX = 300f,
      spawnY = 1100f,
      eventTriggerX = 1760f,
      eventHeadlineVi = "TIẾNG TÙ VÀ BÁO ĐỘNG VANG VỌNG BIÊN GIỚI!",
      eventDialogueSpeaker = "Lyra",
      eventDialogueLineVi = "Master coi chừng! Từ phế tích đá cổ phía Đông, Thủ Lĩnh Chiến Đoàn Kraag đã dẫn quân chủ lực xuất hiện!",
      enemies = listOf(
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_RAIDER, 880f, 1000f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_RAIDER, 940f, 1140f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_ARCHER, 1030f, 1050f),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 1400f, 620f),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 1510f, 700f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_ARCHER, 1610f, 640f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_RAIDER, 1540f, 1490f),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 1660f, 1540f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_RAIDER, 2360f, 1010f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_ARCHER, 2450f, 1150f),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 2160f, 890f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_ARCHER, 2220f, 1240f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.GOBLIN_WAR_CAPTAIN, 2720f, 1070f, spawnOnEventWave = true),
      ),
    ),

    // FLOOR 02 — WINDING WILDERNESS (Search & Breakthrough)
    TowerFloorSpec(
      floorNumber = 2,
      codeName = "FLOOR 02 — WINDING WILDERNESS",
      titleVi = "Đại Ngàn Uốn Khúc",
      subtitleVi = "Hẻm rừng cổ thụ phủ sương lam và thực vật phát quang ma mị",
      missionTypeVi = "Thám Thính (Search)",
      objectiveVi = "Truy dấu mạch ma lực qua sương mù linh hồn và thanh tẩy Cổ Thụ Hắc Ám Sylvan.",
      descriptionVi = "Ánh sáng xanh xuyên qua tán lá dày đặc của đại ngàn uốn khúc. Những Oán Linh tấn công từ tầm xa trong khi bầy Sói Hoang rình rập hai bên hẻm rừng.",
      bannerRes = R.drawable.img_floor02_forest,
      biome = BiomeTheme.ELDERWOOD_FOREST,
      mapWidth = 3100f,
      mapHeight = 2000f,
      recommendedPower = 540,
      rewardGold = 370,
      rewardMana = 85,
      rewardShards = 31,
      knownMonstersVi = listOf("Oán Linh Rừng Thẳm", "Sói Hoang Gió Lốc", "Cổ Thụ Sylvan (Boss)"),
      spawnX = 280f,
      spawnY = 1000f,
      eventTriggerX = 1600f,
      eventHeadlineVi = "SƯƠNG MÙ DÀY ĐẶC — RỄ CỔ THỤ TRỖI DẬY!",
      eventDialogueSpeaker = "Vesper",
      eventDialogueLineVi = "Ma lực trong khu rừng đang hội tụ về phía cổng đá cổ! Giữ đội hình chặt chẽ quanh Kaelen!",
      enemies = listOf(
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 820f, 920f),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 940f, 1060f),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 1360f, 840f),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 1450f, 980f),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 1490f, 1160f),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 2050f, 880f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 2180f, 1140f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.ELDER_TREANT, 2520f, 1000f, spawnOnEventWave = true),
      ),
    ),

    // FLOOR 03 — FORGOTTEN ORCHARD (Exploration)
    TowerFloorSpec(
      floorNumber = 3,
      codeName = "FLOOR 03 — FORGOTTEN ORCHARD",
      titleVi = "Vườn Quả Lãng Quên",
      subtitleVi = "Khu vườn cổ tích hoang phế của triều đại cũ đầy hoa anh đào và bẫy cổ ngữ",
      missionTypeVi = "Khám Phá (Exploration)",
      objectiveVi = "Khám phá di tích Vườn Quả cổ, thu hồi Tinh Hoa Thảo Mộc và đánh bại Giáp Binh Giữ Vườn.",
      descriptionVi = "Một khu vườn thượng uyển bị bỏ hoang từ kỷ nguyên trước. Dưới những tán cây rực nắng là toán Goblin cướp bóc và các tượng đá canh vườn tự động thức tỉnh.",
      bannerRes = R.drawable.img_floor01_field,
      biome = BiomeTheme.FIRST_FIELD_MEADOW,
      mapWidth = 3200f,
      mapHeight = 2050f,
      recommendedPower = 690,
      rewardGold = 450,
      rewardMana = 100,
      rewardShards = 36,
      knownMonstersVi = listOf("Goblin Xạ Tiễn", "Giáp Binh Phế Tích", "Cổ Thụ Sylvan (Boss)"),
      spawnX = 290f,
      spawnY = 1060f,
      eventTriggerX = 1680f,
      eventHeadlineVi = "PHONG ẤN VƯỜN CỔ VỠ VỤN!",
      eventDialogueSpeaker = "Aurelia",
      eventDialogueLineVi = "Các bia đá giữ vườn vừa phát sáng! Đội hình tiến chậm và tập trung hỏa lực vào mục tiêu lớn!",
      enemies = listOf(
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_ARCHER, 860f, 980f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_RAIDER, 940f, 1120f),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 1440f, 920f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_ARCHER, 1520f, 1140f),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 2040f, 900f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 2140f, 1150f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.ELDER_TREANT, 2620f, 1050f, spawnOnEventWave = true),
      ),
    ),

    // FLOOR 04 — WINDSCAR RIDGE (Defend & Hold)
    TowerFloorSpec(
      floorNumber = 4,
      codeName = "FLOOR 04 — WINDSCAR RIDGE",
      titleVi = "Sườn Đồi Vết Gió",
      subtitleVi = "Hẻm đá lộng gió nơi bầy Sói Gió Lốc và Giáp Binh vây hãm lối lên đỉnh đèo",
      missionTypeVi = "Phòng Thủ (Defend)",
      objectiveVi = "Giữ vững đội hình trước đợt xung kích trên đèo đá và hạ Thủ Lĩnh Gió Lốc.",
      descriptionVi = "Gió gào thét qua những cột đá nứt vỡ trên sườn đồi cao. Bầy Sói Gió Lốc lao xuống với tốc độ cực nhanh, thử thách khả năng bọc lót tiền tuyến của Kỵ Sĩ.",
      bannerRes = R.drawable.img_floor03_ruins,
      biome = BiomeTheme.SUNKEN_RUINS,
      mapWidth = 3000f,
      mapHeight = 1950f,
      recommendedPower = 840,
      rewardGold = 530,
      rewardMana = 115,
      rewardShards = 42,
      knownMonstersVi = listOf("Sói Hoang Gió Lốc", "Giáp Binh Phế Tích", "Thủ Lĩnh Kraag (Tinh Anh)"),
      spawnX = 280f,
      spawnY = 980f,
      eventTriggerX = 1520f,
      eventHeadlineVi = "ĐỢT XUNG KÍCH TỪ ĐỈNH ĐÈO GIÓ LỐC!",
      eventDialogueSpeaker = "Kaelen",
      eventDialogueLineVi = "Giữ vững vị trí phòng ngự! Đừng để bầy sói vòng ra sau lưng Cung Thủ và Pháp Sư!",
      enemies = listOf(
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 820f, 900f),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 880f, 1060f),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 1360f, 940f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_ARCHER, 1440f, 1080f),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 1940f, 860f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 1980f, 1100f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.GOBLIN_WAR_CAPTAIN, 2460f, 980f, spawnOnEventWave = true),
      ),
    ),

    // FLOOR 05 — HOLLOW NEST (Mid-Boss Conquest)
    TowerFloorSpec(
      floorNumber = 5,
      codeName = "FLOOR 05 — HOLLOW NEST",
      titleVi = "Hang Ổ Hư Không",
      subtitleVi = "Cột mốc Tầng 05 — Sào huyệt đá đen rực sáng tinh thể đỏ của Phán Quyết Giả",
      missionTypeVi = "Trảm Tướng (Conquest)",
      objectiveVi = "Đột kích trung tâm Hang Ổ Hư Không và tiêu diệt Phán Quyết Giả Vực Thẳm.",
      descriptionVi = "Cột mốc thử thách đầu tiên của Tòa Tháp Vĩnh Hằng. Giữa quần thể cột đá đen và tinh thể huyết thạch, Phán Quyết Giả Vực Thẳm đang canh giữ chìa khóa mở lối lên các tầng trên.",
      bannerRes = R.drawable.img_floor03_ruins,
      biome = BiomeTheme.CRIMSON_CAVERN,
      mapWidth = 3100f,
      mapHeight = 2000f,
      recommendedPower = 1020,
      rewardGold = 680,
      rewardMana = 145,
      rewardShards = 55,
      knownMonstersVi = listOf("Giáp Binh Phế Tích", "Oán Linh Rừng Thẳm", "Phán Quyết Giả Vực Thẳm (Boss)"),
      spawnX = 280f,
      spawnY = 980f,
      eventTriggerX = 1560f,
      eventHeadlineVi = "ẤN CHÚ HUYẾT THẠCH TẦNG 05 KÍCH HOẠT!",
      eventDialogueSpeaker = "Kaelen",
      eventDialogueLineVi = "Hắn bước ra từ đại điện Hư Không rồi! Đứng sau khiên của tôi, dồn toàn lực kết liễu hắn!",
      enemies = listOf(
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 850f, 940f),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 960f, 1080f),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 1420f, 860f),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 1460f, 1100f),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 1980f, 890f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 2150f, 1120f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.ABYSSAL_EXECUTIONER, 2480f, 980f, spawnOnEventWave = true),
      ),
    ),

    // FLOOR 06 — LOST OBSERVATORY (Search & Relic Recon)
    TowerFloorSpec(
      floorNumber = 6,
      codeName = "FLOOR 06 — LOST OBSERVATORY",
      titleVi = "Đài Thiên Văn Thất Lạc",
      subtitleVi = "Đền đài sao rơi cổ kính lơ lửng giữa rừng sương đêm",
      missionTypeVi = "Thám Thính (Search)",
      objectiveVi = "Mở đường qua các hành lang tinh tú và thu hồi Bản Đồ Tinh Cầu Cổ.",
      descriptionVi = "Những vòng quay tinh tú gãy vỡ nằm rải rác giữa khu rừng cổ thụ phát sáng. Các Oán Linh Thiên Văn canh giữ từng bậc thềm dẫn lên đài quan sát.",
      bannerRes = R.drawable.img_floor02_forest,
      biome = BiomeTheme.ELDERWOOD_FOREST,
      mapWidth = 3100f,
      mapHeight = 2000f,
      recommendedPower = 1190,
      rewardGold = 760,
      rewardMana = 165,
      rewardShards = 62,
      knownMonstersVi = listOf("Oán Linh Rừng Thẳm", "Giáp Binh Phế Tích", "Cổ Thụ Sylvan (Tinh Anh)"),
      spawnX = 280f,
      spawnY = 1000f,
      eventTriggerX = 1620f,
      eventHeadlineVi = "TINH BÀN CỔ ĐẠI PHÁT QUANG DỮ DỘI!",
      eventDialogueSpeaker = "Vesper",
      eventDialogueLineVi = "Năng lượng sao rơi đang thức tỉnh hộ vệ cổ đại của Đài Thiên Văn! Sẵn sàng nghênh chiến!",
      enemies = listOf(
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 820f, 930f),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 930f, 1070f),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 1390f, 960f),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 1510f, 1120f),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 2060f, 880f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 2160f, 1120f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.ELDER_TREANT, 2520f, 1000f, spawnOnEventWave = true),
      ),
    ),

    // FLOOR 07 — ASHEN WATCHTOWER (Survival)
    TowerFloorSpec(
      floorNumber = 7,
      codeName = "FLOOR 07 — ASHEN WATCHTOWER",
      titleVi = "Vọng Lâu Tàn Tro",
      subtitleVi = "Pháo đài canh gác phủ tro tàn nơi quân đoàn quái vật tấn công dồn dập",
      missionTypeVi = "Sinh Tồn (Survival)",
      objectiveVi = "Sinh tồn vượt qua hai lớp phục kích liên hoàn quanh chân Vọng Lâu Tàn Tro.",
      descriptionVi = "Vọng lâu biên thùy từng bảo vệ cư dân trong Tháp nay đã rơi vào tay quân đoàn hỗn mang. Kẻ địch xuất hiện từ cả tiền phương lẫn hai cánh.",
      bannerRes = R.drawable.img_floor03_ruins,
      biome = BiomeTheme.SUNKEN_RUINS,
      mapWidth = 3000f,
      mapHeight = 1950f,
      recommendedPower = 1360,
      rewardGold = 860,
      rewardMana = 185,
      rewardShards = 70,
      knownMonstersVi = listOf("Goblin Xạ Tiễn", "Sói Hoang Gió Lốc", "Giáp Binh Phế Tích", "Thủ Lĩnh Kraag"),
      spawnX = 280f,
      spawnY = 980f,
      eventTriggerX = 1540f,
      eventHeadlineVi = "CHUÔNG BÁO ĐỘNG VỌNG LÂU RỀN VANG!",
      eventDialogueSpeaker = "Lyra",
      eventDialogueLineVi = "Quân tiếp viện của địch đang tràn ra từ cổng Vọng Lâu! Giữ nhịp hồi máu của Aurelia thật đều!",
      enemies = listOf(
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_RAIDER, 810f, 920f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_ARCHER, 920f, 1060f),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 1340f, 880f),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 1440f, 1060f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_ARCHER, 1980f, 860f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 2100f, 1100f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.GOBLIN_WAR_CAPTAIN, 2480f, 980f, spawnOnEventWave = true),
      ),
    ),

    // FLOOR 08 — SILENT CARAVAN (Guard / Escort Route)
    TowerFloorSpec(
      floorNumber = 8,
      codeName = "FLOOR 08 — SILENT CARAVAN",
      titleVi = "Thương Đoàn Tĩnh Lặng",
      subtitleVi = "Tuyến đường buôn cổ xuyên thảo nguyên sương sớm bị bầy sói và phục binh phong tỏa",
      missionTypeVi = "Hộ Tống (Guard / Escort)",
      objectiveVi = "Mở thông tuyến đường Thương Đoàn cổ và quét sạch quân phục kích dọc lộ trình.",
      descriptionVi = "Những cỗ xe gỗ bị bỏ lại dọc con đường đá xuyên thảo nguyên. Để khôi phục tuyến tiếp tế của Thánh Địa, đội hình phải mở thông toàn bộ hành lang này.",
      bannerRes = R.drawable.img_floor01_field,
      biome = BiomeTheme.FIRST_FIELD_MEADOW,
      mapWidth = 3400f,
      mapHeight = 2150f,
      recommendedPower = 1540,
      rewardGold = 980,
      rewardMana = 210,
      rewardShards = 78,
      knownMonstersVi = listOf("Sói Hoang Gió Lốc", "Goblin Xạ Tiễn", "Giáp Binh Phế Tích", "Thủ Lĩnh Kraag"),
      spawnX = 300f,
      spawnY = 1100f,
      eventTriggerX = 1760f,
      eventHeadlineVi = "PHỤC BINH HAI BÊN ĐƯỜNG ĐÁ XUẤT HIỆN!",
      eventDialogueSpeaker = "Aurelia",
      eventDialogueLineVi = "Đội hình hộ tống khép chặt! Đừng để bầy sói chia cắt tiền tuyến và hậu tuyến!",
      enemies = listOf(
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 880f, 990f),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 960f, 1140f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_ARCHER, 1460f, 640f),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 1580f, 1500f),
        EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_ARCHER, 2240f, 980f),
        EnemySpawnSpec(EnemyArchetype.DIRE_WOLF, 2180f, 880f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 2320f, 1180f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.GOBLIN_WAR_CAPTAIN, 2720f, 1070f, spawnOnEventWave = true),
      ),
    ),

    // FLOOR 09 — EMBER SETTLEMENT (Capture Stronghold)
    TowerFloorSpec(
      floorNumber = 9,
      codeName = "FLOOR 09 — EMBER SETTLEMENT",
      titleVi = "Trấn Tàn Lửa",
      subtitleVi = "Khu định cư cổ rực ánh than hồng trước thềm Ngưỡng Cửa Tầng 10",
      missionTypeVi = "Chiếm Đóng (Capture)",
      objectiveVi = "Đột nhập Trấn Tàn Lửa, phá vỡ đội hình Giáp Binh canh cổng và chiếm lĩnh đài cổ ngữ.",
      descriptionVi = "Ánh lửa đỏ hắt lên những bức tường thành rạn nứt của Trấn Tàn Lửa. Đây là tiền đồn cuối cùng bảo vệ Ngưỡng Cửa Cổ Đại ở Tầng 10.",
      bannerRes = R.drawable.img_floor03_ruins,
      biome = BiomeTheme.CRIMSON_CAVERN,
      mapWidth = 3000f,
      mapHeight = 1950f,
      recommendedPower = 1740,
      rewardGold = 1120,
      rewardMana = 240,
      rewardShards = 88,
      knownMonstersVi = listOf("Giáp Binh Phế Tích", "Oán Linh Rừng Thẳm", "Phán Quyết Giả Hộ Vệ"),
      spawnX = 280f,
      spawnY = 980f,
      eventTriggerX = 1560f,
      eventHeadlineVi = "ĐÀI CỔ NGỮ TRẤN TÀN LỬA BÙNG CHÁY!",
      eventDialogueSpeaker = "Vesper",
      eventDialogueLineVi = "Chỉ còn khu vực trung tâm này nữa là chúng ta chạm tới Tầng 10! Tập trung hỏa lực dứt điểm!",
      enemies = listOf(
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 840f, 920f),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 920f, 1080f),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 1400f, 880f),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 1480f, 1080f),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 1980f, 880f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 2120f, 1120f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.ABYSSAL_EXECUTIONER, 2480f, 980f, spawnOnEventWave = true),
      ),
    ),

    // FLOOR 10 — ANCIENT THRESHOLD (Milestone Boss Conquest — Gate to Floor 11-100!)
    TowerFloorSpec(
      floorNumber = 10,
      codeName = "FLOOR 10 — ANCIENT THRESHOLD",
      titleVi = "Ngưỡng Cửa Cổ Đại",
      subtitleVi = "Đại Điện Ngưỡng Cửa Tầng 10 — Thử thách mở khóa tầng trung lưu của Tòa Tháp 100 Tầng",
      missionTypeVi = "Đại Boss Tầng 10 (Conquest)",
      objectiveVi = "Vượt qua đội cận vệ Tinh Anh và đánh bại Chúa Tể Ngưỡng Cửa Cổ Đại để mở lối lên Tầng 11+.",
      descriptionVi = "Ngưỡng Cửa Cổ Đại sừng sững giữa đền đài sao rơi cổ xưa. Vượt qua thử thách sinh tử tại Tầng 10 sẽ chứng minh tư cách của Master và đội hình trước hành trình chinh phục 90 tầng cao hơn của Tòa Tháp Vĩnh Hằng.",
      bannerRes = R.drawable.img_sanctuary_gate,
      biome = BiomeTheme.FALLEN_STAR_TEMPLE,
      mapWidth = 3100f,
      mapHeight = 2000f,
      recommendedPower = 2050,
      rewardGold = 1500,
      rewardMana = 320,
      rewardShards = 120,
      knownMonstersVi = listOf("Giáp Binh Tinh Tú", "Oán Linh Thượng Cổ", "Chúa Tể Ngưỡng Cửa (Milestone Boss)"),
      spawnX = 280f,
      spawnY = 980f,
      eventTriggerX = 1560f,
      eventHeadlineVi = "CỔNG ĐẠI ĐIỆN TẦNG 10 MỞ TUNG — CHÚA TỂ GIÁNG LÂM!",
      eventDialogueSpeaker = "Kaelen",
      eventDialogueLineVi = "Toàn đội nghe lệnh Master! Vượt qua Ngưỡng Cửa Cổ Đại này, bầu trời tầng cao của Tòa Tháp sẽ thuộc về chúng ta!",
      enemies = listOf(
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 820f, 900f),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 820f, 1060f),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 1360f, 860f),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 1420f, 980f),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 1360f, 1100f),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 2020f, 860f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.RUIN_SENTINEL, 2020f, 1100f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.FOREST_WRAITH, 2180f, 980f, spawnOnEventWave = true),
        EnemySpawnSpec(EnemyArchetype.ABYSSAL_EXECUTIONER, 2520f, 980f, spawnOnEventWave = true),
      ),
    ),
  )

  private fun distToAnyPath(px: Float, py: Float, paths: List<PathSegment>): Float {
    var best = Float.MAX_VALUE
    for (s in paths) {
      val vx = s.endX - s.startX
      val vy = s.endY - s.startY
      val lenSq = vx * vx + vy * vy
      val d = if (lenSq <= 0.001f) {
        hypot(px - s.startX, py - s.startY)
      } else {
        val t = (((px - s.startX) * vx + (py - s.startY) * vy) / lenSq).coerceIn(0f, 1f)
        hypot(px - (s.startX + t * vx), py - (s.startY + t * vy))
      }
      if (d < best) best = d
    }
    return best
  }

  fun buildTowerFloorMap(spec: TowerFloorSpec): GeneratedMapLayout {
    val rng = Random(spec.floorNumber * 9173)
    val patches = mutableListOf<TerrainPatch>()
    val paths = mutableListOf<PathSegment>()
    val obstacles = mutableListOf<WorldObstacle>()

    when (spec.biome) {
      BiomeTheme.FIRST_FIELD_MEADOW -> {
        val mainRoad = listOf(
          160f to 1100f,
          680f to 1040f,
          1220f to 1130f,
          1780f to 990f,
          2360f to 1080f,
          3060f to 1060f,
        )
        for (i in 0 until mainRoad.lastIndex) {
          val (x1, y1) = mainRoad[i]
          val (x2, y2) = mainRoad[i + 1]
          paths += PathSegment(x1, y1, x2, y2, 134f, 0xFFA69886L, 0xFF9E5A2BL)
        }

        val northBranch = listOf(960f to 1085f, 1340f to 640f, 1740f to 630f, 2120f to 1030f)
        for (i in 0 until northBranch.lastIndex) {
          val (x1, y1) = northBranch[i]
          val (x2, y2) = northBranch[i + 1]
          paths += PathSegment(x1, y1, x2, y2, 98f, 0xFFA69886L, 0xFF9E5A2BL)
        }
        val southBranch = listOf(1180f to 1125f, 1480f to 1520f, 1820f to 1530f, 2240f to 1075f)
        for (i in 0 until southBranch.lastIndex) {
          val (x1, y1) = southBranch[i]
          val (x2, y2) = southBranch[i + 1]
          paths += PathSegment(x1, y1, x2, y2, 98f, 0xFFA69886L, 0xFF9E5A2BL)
        }

        repeat(36) { idx ->
          val px = 200f + rng.nextFloat() * (spec.mapWidth - 400f)
          val py = 180f + rng.nextFloat() * (spec.mapHeight - 360f)
          val shade = when (idx % 4) {
            0 -> 0x2E102A38L
            1 -> 0x2884BD3EL
            2 -> 0x26B4DF4EL
            else -> 0x24296638L
          }
          patches += TerrainPatch(
            x = px,
            y = py,
            radiusX = 175f + rng.nextFloat() * 210f,
            radiusY = 115f + rng.nextFloat() * 140f,
            colorArgb = shade,
            isTallGrass = idx % 2 == 0,
          )
        }

        // Landmark Mossy 3/4 Forest Ruins (Reference 3 Crumbling Stone Turrets & Arches!)
        val ruinSpots = listOf(
          520f to 1290f,
          640f to 930f,
          1560f to 460f,
          1620f to 1710f,
          2580f to 830f,
          2580f to 1310f,
        )
        ruinSpots.forEachIndexed { idx, (rx, ry) ->
          obstacles += WorldObstacle(
            id = "f1_ruin_arch_$idx",
            x = rx,
            y = ry,
            radius = 38f,
            blocksMovement = true,
            propKind = PropKind.ANCIENT_RUIN_ARCH,
            scale = 1.06f,
            footprintHalfW = 54f,
            footprintDepthNorth = 26f,
          )
        }

        // Dense Perimeter 3/4 Woodland Framing + Multi-Layered Ancient Oaks & Spirit Pines
        var borderX = 200f
        var bIdx = 0
        while (borderX < spec.mapWidth - 200f) {
          val topKind = if (bIdx % 3 == 0) PropKind.PINE_SPIRIT_TREE else PropKind.OAK_TREE
          val botKind = if (bIdx % 3 == 1) PropKind.PINE_SPIRIT_TREE else PropKind.OAK_TREE
          obstacles += WorldObstacle(
            id = "f1_tree_n_$bIdx",
            x = borderX,
            y = 210f + (bIdx % 2) * 52f,
            radius = 22f,
            blocksMovement = true,
            propKind = topKind,
            scale = 1.08f,
            occludesUpperCanopy = true,
            footprintHalfW = 22f,
            footprintDepthNorth = 18f,
          )
          obstacles += WorldObstacle(
            id = "f1_tree_s_$bIdx",
            x = borderX,
            y = spec.mapHeight - 175f - (bIdx % 2) * 52f,
            radius = 22f,
            blocksMovement = true,
            propKind = botKind,
            scale = 1.08f,
            occludesUpperCanopy = true,
            footprintHalfW = 22f,
            footprintDepthNorth = 18f,
          )
          borderX += 185f
          bIdx++
        }

        // Guaranteed lush framing trees near the starting clearing so Reference 3 forest depth is immediate!
        listOf(
          Triple(210f, 945f, PropKind.OAK_TREE),
          Triple(430f, 920f, PropKind.PINE_SPIRIT_TREE),
          Triple(255f, 1275f, PropKind.OAK_TREE),
          Triple(460f, 1255f, PropKind.FOREST_CANOPY_BUSH),
          Triple(760f, 895f, PropKind.OAK_TREE),
          Triple(790f, 1245f, PropKind.PINE_SPIRIT_TREE),
        ).forEachIndexed { idx, (tx, ty, kind) ->
          val isTree = kind != PropKind.FOREST_CANOPY_BUSH
          obstacles += WorldObstacle(
            id = "f1_spawn_grove_$idx",
            x = tx,
            y = ty,
            radius = 20f,
            blocksMovement = true,
            propKind = kind,
            scale = 1.04f,
            occludesUpperCanopy = isTree,
            footprintHalfW = 20f,
            footprintDepthNorth = 16f,
          )
        }

        repeat(66) { i ->
          val ox = 260f + rng.nextFloat() * (spec.mapWidth - 520f)
          val oy = 250f + rng.nextFloat() * (spec.mapHeight - 500f)
          if (distToAnyPath(ox, oy, paths) > 122f && hypot(ox - 2720f, oy - 1070f) > 210f) {
            val kind = when (i % 8) {
              0, 1, 2 -> PropKind.OAK_TREE
              3 -> PropKind.PINE_SPIRIT_TREE
              4, 5 -> PropKind.FOREST_CANOPY_BUSH
              6 -> PropKind.MOSSY_BOULDER
              else -> PropKind.ANCIENT_MONOLITH
            }
            val isTree = kind == PropKind.OAK_TREE || kind == PropKind.PINE_SPIRIT_TREE
            obstacles += WorldObstacle(
              id = "f1_solid_$i",
              x = ox,
              y = oy,
              radius = 22f,
              blocksMovement = true,
              propKind = kind,
              scale = 0.96f + rng.nextFloat() * 0.24f,
              occludesUpperCanopy = isTree,
              footprintHalfW = if (isTree) 22f else 0f,
              footprintDepthNorth = if (isTree) 16f else 0f,
            )
          }
        }

        repeat(96) { i ->
          val gx = 180f + rng.nextFloat() * (spec.mapWidth - 340f)
          val gy = 170f + rng.nextFloat() * (spec.mapHeight - 340f)
          if (distToAnyPath(gx, gy, paths) > 68f) {
            obstacles += WorldObstacle(
              id = "f1_foliage_$i",
              x = gx,
              y = gy,
              radius = 12f,
              blocksMovement = false,
              propKind = if (i % 3 == 0) PropKind.WILDFLOWERS else PropKind.TALL_GRASS_CLUMP,
              scale = 0.9f + rng.nextFloat() * 0.35f,
            )
          }
        }

        obstacles += WorldObstacle("f1_lantern_w", 495f, 965f, 14f, true, PropKind.STREET_LANTERN, 1.0f)
        obstacles += WorldObstacle("f1_lantern_mid", 1680f, 895f, 14f, true, PropKind.STREET_LANTERN, 1.0f)
        obstacles += WorldObstacle("f1_brazier_n", 2890f, 935f, 18f, true, PropKind.ARCANE_BRAZIER)
        obstacles += WorldObstacle("f1_brazier_s", 2890f, 1185f, 18f, true, PropKind.ARCANE_BRAZIER)

        // Optional Exploration Discoveries on North & South Meadow Branches!
        obstacles += WorldObstacle(
          id = "f1_discovery_chest_north",
          x = 1540f,
          y = 585f,
          radius = 22f,
          blocksMovement = false,
          propKind = PropKind.TREASURE_CHEST_CLOSED,
          scale = 1.08f,
        )
        obstacles += WorldObstacle(
          id = "f1_discovery_shrine_south",
          x = 1660f,
          y = 1565f,
          radius = 24f,
          blocksMovement = false,
          propKind = PropKind.ASTRAL_HERB_SHRINE,
          scale = 1.08f,
        )

        obstacles += WorldObstacle(
          id = "f1_goal_portal",
          x = 3035f,
          y = 1060f,
          radius = 68f,
          blocksMovement = true,
          propKind = PropKind.FACILITY_TOWER_GATE,
          scale = 0.88f,
          footprintHalfW = 140f,
          footprintDepthNorth = 95f,
        )
      }

      BiomeTheme.ELDERWOOD_FOREST -> {
        val roadPoints = listOf(180f to 1000f, 950f to 920f, 1750f to 1060f, 2760f to 1000f)
        for (i in 0 until roadPoints.lastIndex) {
          val (x1, y1) = roadPoints[i]
          val (x2, y2) = roadPoints[i + 1]
          paths += PathSegment(x1, y1, x2, y2, 104f, 0xFF2B353AL, 0xFF1E262BL)
        }
        repeat(46) { i ->
          val ox = 260f + rng.nextFloat() * (spec.mapWidth - 480f)
          val oy = 170f + rng.nextFloat() * (spec.mapHeight - 340f)
          if (abs(oy - 990f) > 150f) {
            val kind = when (i % 6) {
              0 -> PropKind.ANCIENT_RUIN_ARCH
              1 -> PropKind.MYSTIC_PLUM_TREE
              2 -> PropKind.FOREST_CANOPY_BUSH
              3 -> PropKind.OAK_TREE
              else -> PropKind.PINE_SPIRIT_TREE
            }
            val isTree = kind == PropKind.PINE_SPIRIT_TREE || kind == PropKind.OAK_TREE || kind == PropKind.MYSTIC_PLUM_TREE
            obstacles += WorldObstacle(
              id = "f2_obs_$i",
              x = ox,
              y = oy,
              radius = 22f,
              blocksMovement = true,
              propKind = kind,
              scale = 1.04f,
              occludesUpperCanopy = isTree,
              footprintHalfW = if (isTree) 22f else 0f,
              footprintDepthNorth = if (isTree) 16f else 0f,
            )
          }
        }
        obstacles += WorldObstacle("f2_goal", 2790f, 1000f, 64f, true, PropKind.FACILITY_TOWER_GATE, 0.88f, footprintHalfW = 140f, footprintDepthNorth = 95f)
      }

      else -> {
        val roadPoints = listOf(180f to 980f, 1100f to 980f, 1950f to 980f, 2720f to 980f)
        for (i in 0 until roadPoints.lastIndex) {
          val (x1, y1) = roadPoints[i]
          val (x2, y2) = roadPoints[i + 1]
          paths += PathSegment(x1, y1, x2, y2, 112f, 0xFF332B54L, 0xFF463857L)
        }
        repeat(36) { i ->
          val ox = 280f + rng.nextFloat() * (spec.mapWidth - 500f)
          val oy = 175f + rng.nextFloat() * (spec.mapHeight - 350f)
          if (abs(oy - 980f) > 150f) {
            val kind = when (i % 4) {
              0 -> PropKind.OBSIDIAN_COLUMN
              1 -> PropKind.CRIMSON_CRYSTAL
              2 -> PropKind.ROYAL_STATUE
              else -> PropKind.TATTERED_BANNER
            }
            obstacles += WorldObstacle("f3_obs_$i", ox, oy, 26f, kind != PropKind.TATTERED_BANNER, kind, 1.10f)
          }
        }
        obstacles += WorldObstacle("f3_goal", 2750f, 980f, 64f, true, PropKind.FACILITY_TOWER_GATE, 0.88f, footprintHalfW = 140f, footprintDepthNorth = 95f)
      }
    }

    return GeneratedMapLayout(
      width = spec.mapWidth,
      height = spec.mapHeight,
      wallMargin = 75f,
      biome = spec.biome,
      patches = patches,
      paths = paths,
      obstacles = obstacles,
    )
  }
}
