package com.example.game.map

import androidx.annotation.DrawableRes
import com.example.data.model.FacilityType
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * True Oblique 3/4 RPG Camera Projection (~43° downward pitch above the ground plane).
 * Ground depth (world Y) is foreshortened by OBLIQUE_Y_SCALE while vertical architecture (Z)
 * rises at full 1:1 height, simultaneously revealing pitched rooftops AND front-facing facades.
 */
object ObliqueCamera {
  const val OBLIQUE_Y_SCALE: Float = 0.72f
  const val GROUND_ELLIPSE_RATIO: Float = 0.66f

  fun worldToScreenX(wx: Float, camX: Float, zoom: Float, viewportHalfW: Float): Float =
    (wx - camX) * zoom + viewportHalfW

  fun worldToScreenY(
    wy: Float,
    camY: Float,
    zoom: Float,
    viewportHalfH: Float,
    elevationZ: Float = 0f,
  ): Float = ((wy - camY) * OBLIQUE_Y_SCALE - elevationZ) * zoom + viewportHalfH

  fun screenToWorldX(sx: Float, camX: Float, zoom: Float, viewportHalfW: Float): Float =
    (sx - viewportHalfW) / zoom + camX

  fun screenToWorldY(sy: Float, camY: Float, zoom: Float, viewportHalfH: Float): Float =
    (sy - viewportHalfH) / (zoom * OBLIQUE_Y_SCALE) + camY

  fun clampCamX(
    desiredCamX: Float,
    worldWidth: Float,
    wallMargin: Float,
    zoom: Float,
    viewportHalfW: Float,
  ): Float {
    val halfVisibleWorldW = viewportHalfW / zoom.coerceAtLeast(0.26f)
    val minX = wallMargin * 0.35f + halfVisibleWorldW
    val maxX = worldWidth - wallMargin * 0.35f - halfVisibleWorldW
    return if (minX <= maxX) desiredCamX.coerceIn(minX, maxX) else worldWidth * 0.5f
  }

  fun clampCamY(
    desiredCamY: Float,
    worldHeight: Float,
    wallMargin: Float,
    zoom: Float,
    viewportHalfH: Float,
    northHeadroomWorld: Float = 160f,
  ): Float {
    val halfVisibleWorldY = viewportHalfH / (zoom.coerceAtLeast(0.26f) * OBLIQUE_Y_SCALE)
    val minY = (wallMargin - northHeadroomWorld) + halfVisibleWorldY
    val maxY = (worldHeight - wallMargin + 95f) - halfVisibleWorldY
    return if (minY <= maxY) desiredCamY.coerceIn(minY, maxY) else worldHeight * 0.48f
  }
}

enum class BiomeTheme {
  SANCTUARY_ASTRAL,
  RESTAURANT_INTERIOR,
  DORMITORY_INTERIOR,
  TRAINING_HALL_INTERIOR,
  ARMORY_INTERIOR,
  FIRST_FIELD_MEADOW,
  ELDERWOOD_FOREST,
  SUNKEN_RUINS,
  CRIMSON_CAVERN,
  FALLEN_STAR_TEMPLE;

  val isInteriorBiome: Boolean
    get() = this == RESTAURANT_INTERIOR ||
      this == DORMITORY_INTERIOR ||
      this == TRAINING_HALL_INTERIOR ||
      this == ARMORY_INTERIOR
}

enum class PropKind {
  OAK_TREE,
  PINE_SPIRIT_TREE,
  MYSTIC_PLUM_TREE,
  SAKURA_BLOSSOM_TREE,
  CYPRESS_CONIFER_TREE,
  FOREST_CANOPY_BUSH,
  GARDEN_HEDGE_BUSH,
  TALL_GRASS_CLUMP,
  WILDFLOWERS,
  MOSSY_BOULDER,
  ANCIENT_MONOLITH,
  ANCIENT_RUIN_ARCH,
  RUINED_PILLAR,
  OBSIDIAN_COLUMN,
  ARCANE_BRAZIER,
  STREET_LANTERN,
  BARREL_CLUSTER,
  WOODEN_BENCH_34,
  ARCANE_BOOKSHELF_34,
  STONE_RETAINING_WALL,
  TERRACE_CLIFF_WALL,
  TERRACE_GRAND_STAIRS,
  STONE_BALUSTRADE_RAIL,
  MEDIEVAL_TOWNHOUSE_A,
  MEDIEVAL_TOWNHOUSE_B,
  SANCTUARY_COTTAGE_BROWN,
  SANCTUARY_COTTAGE_SLATE,
  FORTRESS_CORNER_TOWER,
  SOUTHERN_GATE_TOWER,
  TATTERED_BANNER,
  TRAINING_DUMMY,
  TRAINING_STRIKING_POST,
  WEAPON_RACK,
  ALCHEMICAL_CRUCIBLE,
  BANQUET_TABLE,
  RECOVERY_BED,
  SUMMON_OBELISK,
  TOWER_PORTAL_GATE,
  CRIMSON_CRYSTAL,
  ROYAL_STATUE,
  FACILITY_TOWER_GATE,
  FACILITY_SUMMONING_HALL,
  FACILITY_HERO_REGISTRY,
  FACILITY_TRAINING_GROUND,
  FACILITY_ARMORY,
  FACILITY_SYNTHESIS_LAB,
  FACILITY_HERO_QUARTERS,
  FACILITY_RESTAURANT,
  FLOOR_VISTA_ARCH,
  TAVERN_COUNTER_BAR,
  TAVERN_SHELF_BARRELS,
  TAVERN_HEARTH_FIREPLACE,
  TAVERN_LONG_TABLE,
  TAVERN_EXIT_MAT,
  FORGE_ANVIL_STATION,
  ARMOR_STAND_34,
  DORM_PERSONAL_CHEST,
  INFIRMARY_CARE_TABLE,
  FLOWER_PLANTER_BED,
  TREASURE_CHEST_CLOSED,
  TREASURE_CHEST_OPEN,
  ASTRAL_HERB_SHRINE,
}

data class TerrainPatch(
  val x: Float,
  val y: Float,
  val radiusX: Float,
  val radiusY: Float,
  val colorArgb: Long,
  val isTallGrass: Boolean = false,
)

data class PathSegment(
  val startX: Float,
  val startY: Float,
  val endX: Float,
  val endY: Float,
  val width: Float,
  val colorArgb: Long,
  val borderColorArgb: Long,
)

/**
 * Represents a 3/4 architectural structure, environmental prop, tree, or terrain wall.
 * [y] is the front-most ground contact baseline used for painter's Y-depth sorting so
 * characters walking south of [y] stand in front of the facade, while characters walking
 * north of the foundation ([y - footprintDepthNorth]) are naturally occluded behind the roof/canopy!
 */
data class WorldObstacle(
  val id: String,
  val x: Float,
  val y: Float,
  val radius: Float,
  val blocksMovement: Boolean = true,
  val propKind: PropKind,
  val scale: Float = 1f,
  val occludesUpperCanopy: Boolean = false,
  val footprintHalfW: Float = 0f,
  val footprintDepthNorth: Float = 0f,
  val elevationZ: Float = 0f,
  val sortYBias: Float = 0f,
)

data class SanctuaryZoneZone(
  val facility: FacilityType,
  val centerX: Float,
  val centerY: Float,
  val interactRadius: Float,
  val colorArgb: Long,
)

enum class EnemyArchetype(
  val nameVi: String,
  val titleVi: String,
  val isRanged: Boolean,
  val isBoss: Boolean,
  val baseHp: Int,
  val baseAtk: Int,
  val baseDef: Int,
  val speed: Float,
  val attackRange: Float,
  val aggroRange: Float,
  val primaryColor: Long,
  val accentColor: Long,
  val scale: Float = 1f,
) {
  FIELD_GOBLIN_RAIDER(
    nameVi = "Goblin Trinh Sát",
    titleVi = "Tiền Phong Đồng Cỏ",
    isRanged = false,
    isBoss = false,
    baseHp = 155,
    baseAtk = 22,
    baseDef = 6,
    speed = 92f,
    attackRange = 42f,
    aggroRange = 340f,
    primaryColor = 0xFF4D7C3FL,
    accentColor = 0xFFB45309L,
  ),
  FIELD_GOBLIN_ARCHER(
    nameVi = "Goblin Xạ Tiễn",
    titleVi = "Phục Kích Bụi Rậm",
    isRanged = true,
    isBoss = false,
    baseHp = 120,
    baseAtk = 26,
    baseDef = 4,
    speed = 86f,
    attackRange = 225f,
    aggroRange = 370f,
    primaryColor = 0xFF3F6212L,
    accentColor = 0xFFD97706L,
  ),
  DIRE_WOLF(
    nameVi = "Sói Hoang Gió Lốc",
    titleVi = "Thú Săn Cánh Đồng",
    isRanged = false,
    isBoss = false,
    baseHp = 175,
    baseAtk = 28,
    baseDef = 5,
    speed = 128f,
    attackRange = 40f,
    aggroRange = 380f,
    primaryColor = 0xFF475569L,
    accentColor = 0xFF94A3B8L,
  ),
  GOBLIN_WAR_CAPTAIN(
    nameVi = "Thủ Lĩnh Kraag",
    titleVi = "Chỉ Huy Chiến Đoàn Tầng 1",
    isRanged = false,
    isBoss = true,
    baseHp = 780,
    baseAtk = 44,
    baseDef = 16,
    speed = 96f,
    attackRange = 58f,
    aggroRange = 460f,
    primaryColor = 0xFF365314L,
    accentColor = 0xFFEF4444L,
    scale = 1.45f,
  ),
  FOREST_WRAITH(
    nameVi = "Oán Linh Rừng Thẳm",
    titleVi = "Bóng Ma Sương Lam",
    isRanged = true,
    isBoss = false,
    baseHp = 210,
    baseAtk = 36,
    baseDef = 9,
    speed = 102f,
    attackRange = 215f,
    aggroRange = 380f,
    primaryColor = 0xFF1E3A8AL,
    accentColor = 0xFF38BDF8L,
  ),
  ELDER_TREANT(
    nameVi = "Cổ Thụ Hắc Ám Sylvan",
    titleVi = "Hộ Pháp Rừng Thì Thầm",
    isRanged = false,
    isBoss = true,
    baseHp = 1150,
    baseAtk = 56,
    baseDef = 24,
    speed = 82f,
    attackRange = 64f,
    aggroRange = 460f,
    primaryColor = 0xFF14532DL,
    accentColor = 0xFF22D3EEL,
    scale = 1.55f,
  ),
  RUIN_SENTINEL(
    nameVi = "Giáp Binh Phế Tích",
    titleVi = "Kỵ Sĩ Đá Cổ",
    isRanged = false,
    isBoss = false,
    baseHp = 290,
    baseAtk = 44,
    baseDef = 18,
    speed = 88f,
    attackRange = 48f,
    aggroRange = 360f,
    primaryColor = 0xFF334155L,
    accentColor = 0xFFF43F5EL,
  ),
  ABYSSAL_EXECUTIONER(
    nameVi = "Phán Quyết Giả Vực Thẳm",
    titleVi = "Chúa Tể Pháo Đài Chìm",
    isRanged = false,
    isBoss = true,
    baseHp = 1580,
    baseAtk = 72,
    baseDef = 28,
    speed = 98f,
    attackRange = 66f,
    aggroRange = 480f,
    primaryColor = 0xFF1E1B4BL,
    accentColor = 0xFFE11D48L,
    scale = 1.6f,
  ),
}

data class EnemySpawnSpec(
  val archetype: EnemyArchetype,
  val x: Float,
  val y: Float,
  val spawnOnEventWave: Boolean = false,
)

data class TowerFloorSpec(
  val floorNumber: Int,
  val codeName: String,
  val titleVi: String,
  val subtitleVi: String,
  val missionTypeVi: String,
  val objectiveVi: String,
  val descriptionVi: String,
  @param:DrawableRes val bannerRes: Int,
  val biome: BiomeTheme,
  val mapWidth: Float,
  val mapHeight: Float,
  val recommendedPower: Int,
  val rewardGold: Int,
  val rewardMana: Int,
  val rewardShards: Int,
  val knownMonstersVi: List<String>,
  val spawnX: Float,
  val spawnY: Float,
  val eventTriggerX: Float,
  val eventHeadlineVi: String,
  val eventDialogueSpeaker: String,
  val eventDialogueLineVi: String,
  val enemies: List<EnemySpawnSpec>,
)

/**
 * Authoritative 8-sided polygonal geometry of the floating Sanctuary Fortress-Island.
 * Shared by terrain clipping, perimeter fortress wall rendering, cliff drop-offs, and walkability physics.
 */
object SanctuaryIslandGeometry {
  const val WORLD_WIDTH = 3200f
  const val WORLD_HEIGHT = 2760f
  const val WALL_MARGIN = 120f

  // Clockwise 8-vertex outer wall centerline in world coordinates
  val POLYGON_VERTICES: List<Pair<Float, Float>> = listOf(
    980f to 340f,   // 0: NW_TOP
    2240f to 340f,  // 1: NE_TOP
    2900f to 880f,  // 2: E_TOP
    2900f to 1780f, // 3: E_BOT
    2320f to 2360f, // 4: SE_CORNER
    900f to 2360f,  // 5: SW_CORNER
    300f to 1780f,  // 6: W_BOT
    300f to 880f,   // 7: W_TOP
  )

  const val PLAZA_CENTER_X = 1940f
  const val PLAZA_CENTER_Y = 1240f
  const val PLAZA_RADIUS = 555f

  const val TRAINING_SAND_X = 890f
  const val TRAINING_SAND_Y = 1790f
  const val TRAINING_SAND_RX = 435f
  const val TRAINING_SAND_RY = 270f

  const val SOUTH_GATE_X = 1610f
  const val SOUTH_GATE_Y = 2360f

  const val NORTH_TUNNEL_X = 1560f
  const val NORTH_TUNNEL_Y = 365f

  // Physical Island Expansion Milestones (East Celestial Garden & NE Astral Sky-Dock)
  const val EAST_BEACON_X = 2845f
  const val EAST_BEACON_Y = 1265f
  const val EAST_BRIDGE_Y = 1265f
  const val EAST_TERRACE_CENTER_X = 3195f
  const val EAST_TERRACE_CENTER_Y = 1265f
  const val EXPANDED_WORLD_WIDTH = 3460f

  const val NORTH_BEACON_X = 2565f
  const val NORTH_BEACON_Y = 615f
  const val NORTH_DOCK_CENTER_X = 2825f
  const val NORTH_DOCK_CENTER_Y = 535f

  fun clampInsideWalkableIsland(
    x: Float,
    y: Float,
    wallClearance: Float = 54f,
    eastGardenUnlocked: Boolean = false,
    northDockUnlocked: Boolean = false,
  ): Pair<Float, Float> {
    if (eastGardenUnlocked) {
      // Walkable arched stone bridge + Eastern Celestial Garden floating terrace
      if (x >= 2835f && y in 1190f..1340f && x <= 3045f) {
        return x.coerceIn(2835f, 3045f) to y.coerceIn(1198f, 1332f)
      }
      if (x > 3020f && y in 1070f..1460f) {
        return x.coerceIn(3025f, 3355f) to y.coerceIn(1085f, 1445f)
      }
    }
    if (northDockUnlocked && x >= 2535f && y in 440f..685f) {
      return x.coerceIn(2535f, 3035f) to y.coerceIn(455f, 675f)
    }

    var rx = x.coerceIn(300f + wallClearance, 2900f - wallClearance)
    var ry = y.coerceIn(340f + wallClearance, 2360f - wallClearance * 0.85f)

    // 1. NW sloped wall: (300, 880) -> (980, 340), inward normal (0.6228, 0.7824)
    val dNw = 0.6228f * (rx - 300f) + 0.7824f * (ry - 880f)
    if (dNw < wallClearance) {
      val push = wallClearance - dNw
      rx += 0.6228f * push
      ry += 0.7824f * push
    }

    // 2. NE sloped wall: (2240, 340) -> (2900, 880), inward normal (-0.6332, 0.7740)
    val dNe = -0.6332f * (rx - 2240f) + 0.7740f * (ry - 340f)
    if (dNe < wallClearance) {
      val push = wallClearance - dNe
      rx -= 0.6332f * push
      ry += 0.7740f * push
    }

    // 3. SW sloped wall: (300, 1780) -> (900, 2360), inward normal (0.6950, -0.7190)
    val dSw = 0.6950f * (rx - 300f) - 0.7190f * (ry - 1780f)
    if (dSw < wallClearance) {
      val push = wallClearance - dSw
      rx += 0.6950f * push
      ry -= 0.7190f * push
    }

    // 4. SE sloped wall: (2900, 1780) -> (2320, 2360), inward normal (-0.7071, -0.7071)
    val dSe = -0.7071f * (rx - 2900f) - 0.7071f * (ry - 1780f)
    if (dSe < wallClearance) {
      val push = wallClearance - dSe
      rx -= 0.7071f * push
      ry -= 0.7071f * push
    }

    rx = rx.coerceIn(300f + wallClearance, 2900f - wallClearance)
    ry = ry.coerceIn(340f + wallClearance, 2360f - wallClearance * 0.85f)
    return rx to ry
  }

  fun isInsideIsland(x: Float, y: Float, inset: Float = 0f): Boolean {
    if (x < 300f + inset || x > 2900f - inset) return false
    if (y < 340f + inset || y > 2360f - inset) return false
    if (0.6228f * (x - 300f) + 0.7824f * (y - 880f) < inset) return false
    if (-0.6332f * (x - 2240f) + 0.7740f * (y - 340f) < inset) return false
    if (0.6950f * (x - 300f) - 0.7190f * (y - 1780f) < inset) return false
    if (-0.7071f * (x - 2900f) - 0.7071f * (y - 1780f) < inset) return false
    return true
  }
}

object CollisionPhysics {
  /**
   * Resolves movement against map boundaries (including the 8-sided polygonal Sanctuary island
   * and unlocked island expansions), rectangular 3/4 building foundations, and solid circular props.
   */
  fun resolvePosition(
    targetX: Float,
    targetY: Float,
    entityRadius: Float,
    mapWidth: Float,
    mapHeight: Float,
    marginWall: Float,
    obstacles: List<WorldObstacle>,
    eastGardenUnlocked: Boolean = false,
    northDockUnlocked: Boolean = false,
  ): Pair<Float, Float> {
    val isSanctuaryExterior =
      (abs(mapWidth - SanctuaryIslandGeometry.WORLD_WIDTH) < 1f ||
        abs(mapWidth - SanctuaryIslandGeometry.EXPANDED_WORLD_WIDTH) < 1f) &&
        abs(mapHeight - SanctuaryIslandGeometry.WORLD_HEIGHT) < 1f

    val effectiveMaxW = if (isSanctuaryExterior && eastGardenUnlocked) {
      SanctuaryIslandGeometry.EXPANDED_WORLD_WIDTH
    } else {
      mapWidth
    }

    var rx = targetX.coerceIn(marginWall + entityRadius, effectiveMaxW - marginWall - entityRadius)
    var ry = targetY.coerceIn(marginWall + entityRadius, mapHeight - marginWall - entityRadius)

    if (isSanctuaryExterior) {
      val clamped = SanctuaryIslandGeometry.clampInsideWalkableIsland(
        x = rx,
        y = ry,
        wallClearance = 44f + entityRadius,
        eastGardenUnlocked = eastGardenUnlocked,
        northDockUnlocked = northDockUnlocked,
      )
      rx = clamped.first
      ry = clamped.second
    }

    for (obs in obstacles) {
      if (!obs.blocksMovement) continue
      if (obs.footprintHalfW > 0f && obs.footprintDepthNorth > 0f) {
        val left = obs.x - obs.footprintHalfW
        val right = obs.x + obs.footprintHalfW
        val top = obs.y - obs.footprintDepthNorth
        val bottom = obs.y
        val closestX = rx.coerceIn(left, right)
        val closestY = ry.coerceIn(top, bottom)
        val dx = rx - closestX
        val dy = ry - closestY
        val distSq = dx * dx + dy * dy
        if (distSq < entityRadius * entityRadius) {
          if (distSq > 0.0001f) {
            val dist = sqrt(distSq)
            val push = entityRadius - dist
            rx += (dx / dist) * push
            ry += (dy / dist) * push
          } else {
            val penLeft = abs(rx - left)
            val penRight = abs(right - rx)
            val penTop = abs(ry - top)
            val penBottom = abs(bottom - ry)
            val minPen = minOf(penLeft, penRight, penTop, penBottom)
            when (minPen) {
              penBottom -> ry = bottom + entityRadius
              penTop -> ry = top - entityRadius
              penLeft -> rx = left - entityRadius
              else -> rx = right + entityRadius
            }
          }
        }
      } else {
        val dx = rx - obs.x
        val dy = ry - obs.y
        val minDist = entityRadius + obs.radius
        val distSq = dx * dx + dy * dy
        if (distSq < minDist * minDist) {
          val dist = sqrt(distSq).coerceAtLeast(0.001f)
          val push = minDist - dist
          rx += (dx / dist) * push
          ry += (dy / dist) * push
        }
      }
    }

    if (isSanctuaryExterior) {
      val clamped = SanctuaryIslandGeometry.clampInsideWalkableIsland(
        x = rx,
        y = ry,
        wallClearance = 44f + entityRadius,
        eastGardenUnlocked = eastGardenUnlocked,
        northDockUnlocked = northDockUnlocked,
      )
      rx = clamped.first
      ry = clamped.second
    } else {
      rx = rx.coerceIn(marginWall + entityRadius, effectiveMaxW - marginWall - entityRadius)
      ry = ry.coerceIn(marginWall + entityRadius, mapHeight - marginWall - entityRadius)
    }
    return rx to ry
  }
}
