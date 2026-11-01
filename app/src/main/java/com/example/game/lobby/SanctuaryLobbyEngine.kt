package com.example.game.lobby

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.model.FacilityType
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.HeroStatusType
import com.example.data.model.IslandExpansionMilestone
import com.example.data.model.PersonalityType
import com.example.game.map.CollisionPhysics
import com.example.game.map.SanctuaryHouseEntrances
import com.example.game.map.SanctuaryIslandGeometry
import com.example.game.tower.GeneratedMapLayout
import com.example.game.tower.TowerFloorCatalog
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

enum class LobbyActivityState(val labelVi: String) {
  PATROLLING("Tuần tra Đại Sảnh"),
  TRAINING("Đấu tập Sân Huấn Luyện"),
  DINING("Thưởng tiệc Nhà Ăn"),
  RESTING("Tịnh dưỡng hồi phục"),
  INSPECTING_ARMORY("Rèn & kiểm tra binh khí"),
  STUDYING_RUNES("Nghiên cứu cổ ngữ"),
  GUARDING_GATE("Canh gác Cổng Tháp"),
  CHATTING("Bàn chiến thuật"),
}

enum class HeroTaskType(val labelVi: String, val targetFacility: FacilityType) {
  TRAIN("Đấu Tập Khu Võ Học", FacilityType.TRAINING_GROUND),
  DINING("Thưởng Tiệc Nhà Ăn", FacilityType.RESTAURANT),
  ALCHEMY("Tu Luyện Lò Luyện Kim", FacilityType.SYNTHESIS_LAB),
  FORGE("Kiểm Tra Xưởng Rèn", FacilityType.ARMORY),
  PREPARE_TOWER("Gác Cổng Tháp Vĩnh Hằng", FacilityType.TOWER_GATE),
  RETURN_QUARTERS("Tịnh Dưỡng Dinh Biệt Thự", FacilityType.HERO_QUARTERS),
}

data class LobbyHeroActor(
  val heroId: String,
  val name: String,
  val heroClass: HeroClassType,
  val personality: PersonalityType,
  var isInjured: Boolean,
  val isSanctuaryKeeperNpc: Boolean = false,
  var x: Float,
  var y: Float,
  var targetX: Float,
  var targetY: Float,
  var facingRight: Boolean = true,
  var facingNorth: Boolean = false,
  var isMoving: Boolean = false,
  var activity: LobbyActivityState = LobbyActivityState.PATROLLING,
  var waitTimer: Float = 2f,
  var attackPracticeAnim: Float = 0f,
  var speechBubble: String? = null,
  var speechTimer: Float = 0f,
  var isSeatedDining: Boolean = false,
  var isPlayerCommanded: Boolean = false,
  val waypointQueue: ArrayDeque<Pair<Float, Float>> = ArrayDeque(),
  var stuckTimer: Float = 0f,
  var lastProgressX: Float = x,
  var lastProgressY: Float = y,
)

/**
 * 20-Node Connected Road & Courtyard Waypoint Graph for the Reconstructed 3200x2760 Floating Sanctuary Island.
 * Routes heroes smoothly along grey cobblestone streets, through the residential avenues, around the
 * Central Circular White Stone Plaza, into the SW Golden-Sand Training Field, and down to the SE Amphitheatre.
 */
private object SanctuaryNavGraph {
  val nodes: List<Pair<Float, Float>> = listOf(
    1560f to 445f,  // 0: North Arched Tunnel Threshold
    1560f to 665f,  // 1: North Cobble Bend
    1330f to 880f,  // 2: Upper Cobble Junction (Synthesis / Restaurant)
    1330f to 1120f, // 3: Hedge Courtyard East Gate
    1330f to 1340f, // 4: Central West Cobble Crossroads
    960f to 1340f,  // 5: Residential South Street Mid
    585f to 1340f,  // 6: West Residential Street Corner
    735f to 1025f,  // 7: Diagonal Residential Avenue Lower (Dorms / Registry)
    960f to 840f,   // 8: Diagonal Residential Avenue Upper (Armory / Restaurant)
    1165f to 675f,  // 9: Synthesis North Lane
    1955f to 665f,  // 10: Northern Obsidian Split-Monolith Stairs Base (Tower Gate)
    1660f to 930f,  // 11: Central Circular Plaza NW Rim
    1940f to 1240f, // 12: Central Circular White Stone Plaza Core
    2275f to 1020f, // 13: Central Circular Plaza NE Rim / Sakura Garden
    2210f to 1535f, // 14: Central Circular Plaza SE Steps
    2330f to 1845f, // 15: SE Classical Amphitheatre Stage Apron (Summoning Hall)
    1545f to 1735f, // 16: Central-South Cobble Spine
    1610f to 2120f, // 17: Southern Fortified Main Gate Forecourt
    1080f to 1660f, // 18: SW Golden-Sand Training Arena Entrance
    855f to 1815f,  // 19: SW Golden-Sand Training Arena Center
    823f to 808f,   // 20: Hero Quarters doorway (south-face arch)
    1103f to 943f,  // 21: Restaurant doorway (south-face arch)
    993f to 673f,   // 22: Armory doorway (south-face arch)
    890f to 1960f,  // 23: Training arena south gate
  )

  private val edges: Map<Int, List<Int>> = buildMap {
    fun link(a: Int, b: Int) {
      put(a, (get(a) ?: emptyList()) + b)
      put(b, (get(b) ?: emptyList()) + a)
    }
    link(0, 1)
    link(1, 2)
    link(1, 9)
    link(1, 10)
    link(1, 11)
    link(2, 3)
    link(2, 8)
    link(2, 9)
    link(2, 11)
    link(3, 4)
    link(3, 11)
    link(3, 12)
    link(4, 5)
    link(4, 12)
    link(4, 16)
    link(4, 18)
    link(5, 6)
    link(5, 7)
    link(5, 18)
    link(6, 7)
    link(7, 8)
    link(8, 9)
    link(10, 11)
    link(10, 12)
    link(10, 13)
    link(11, 12)
    link(12, 13)
    link(12, 14)
    link(12, 16)
    link(13, 14)
    link(14, 15)
    link(14, 16)
    link(15, 17)
    link(16, 17)
    link(16, 18)
    link(18, 19)
    // Doorway leaves (do not alter core 20-node topology, only extend)
    link(7, 20)
    link(8, 21)
    link(8, 22)
    link(9, 22)
    link(19, 23)
  }

  private fun nearestNode(x: Float, y: Float): Int =
    nodes.indices.minByOrNull { idx ->
      val (nx, ny) = nodes[idx]
      hypot(x - nx, y - ny)
    } ?: 12

  fun computeRoute(fromX: Float, fromY: Float, rawDestX: Float, rawDestY: Float): List<Pair<Float, Float>> {
    val dest = com.example.game.map.SanctuaryIslandGeometry.clampInsideWalkableIsland(rawDestX, rawDestY, 26f)
    if (hypot(dest.first - fromX, dest.second - fromY) <= 230f) {
      return listOf(dest)
    }
    val startIdx = nearestNode(fromX, fromY)
    val endIdx = nearestNode(dest.first, dest.second)
    if (startIdx == endIdx) {
      return listOf(nodes[startIdx], dest)
    }

    // Standard BFS across the 20-node road & plaza graph
    val visited = BooleanArray(nodes.size)
    val parent = IntArray(nodes.size) { -1 }
    val queue = ArrayDeque<Int>()
    visited[startIdx] = true
    queue.addLast(startIdx)

    while (queue.isNotEmpty()) {
      val cur = queue.removeFirst()
      if (cur == endIdx) break
      edges[cur].orEmpty().forEach { nxt ->
        if (!visited[nxt]) {
          visited[nxt] = true
          parent[nxt] = cur
          queue.addLast(nxt)
        }
      }
    }

    if (!visited[endIdx]) return listOf(dest)

    val indexChain = mutableListOf<Int>()
    var walk = endIdx
    while (walk != -1) {
      indexChain += walk
      walk = parent[walk]
    }
    indexChain.reverse()

    val waypoints = indexChain.map { idx ->
      val (wx, wy) = nodes[idx]
      (wx + Random.nextFloat() * 18f - 9f) to (wy + Random.nextFloat() * 14f - 7f)
    }.toMutableList()
    waypoints += dest
    return waypoints
  }
}

class SanctuaryLobbyEngine {
  private val exteriorLayout: GeneratedMapLayout = TowerFloorCatalog.buildSanctuaryMap()
  private val restaurantInteriorLayout: GeneratedMapLayout = TowerFloorCatalog.buildRestaurantInteriorMap()
  private val dormitoryInteriorLayout: GeneratedMapLayout = TowerFloorCatalog.buildDormitoryInteriorMap()
  private val trainingInteriorLayout: GeneratedMapLayout = TowerFloorCatalog.buildTrainingHallInteriorMap()
  private val armoryInteriorLayout: GeneratedMapLayout = TowerFloorCatalog.buildArmoryInteriorMap()

  var activeInterior: FacilityType? = null
    private set

  val isInsideInterior: Boolean
    get() = activeInterior != null

  val isInsideRestaurant: Boolean
    get() = activeInterior == FacilityType.RESTAURANT

  var unlockedExpansions: Set<IslandExpansionMilestone> = emptySet()
    private set

  val mapLayout: GeneratedMapLayout
    get() = when (activeInterior) {
      FacilityType.RESTAURANT -> restaurantInteriorLayout
      FacilityType.HERO_QUARTERS -> dormitoryInteriorLayout
      FacilityType.TRAINING_GROUND -> trainingInteriorLayout
      FacilityType.ARMORY -> armoryInteriorLayout
      else -> exteriorLayout
    }

  // Free Camera State for External Master Commander (frames Circular Plaza + Residential + Obsidian Gate!)
  var camX: Float = 1660f
  var camY: Float = 1260f
  var zoom: Float = 0.88f

  // Backward compatibility getters for legacy references
  val masterX: Float get() = camX
  val masterY: Float get() = camY
  val masterFacingRight: Boolean get() = true
  val masterFacingNorth: Boolean get() = false
  val masterIsMoving: Boolean get() = false

  var selectedHeroId: String? by mutableStateOf(null)
  var selectedFacility: FacilityType? by mutableStateOf(null)

  private var savedExteriorX: Float = 1660f
  private var savedExteriorY: Float = 1260f
  private var savedExteriorZoom: Float = 0.88f
  private var savedSelectedHeroId: String? = null
  private var savedSelectedFacility: FacilityType? = null
  private var lastTransitionMs: Long = 0L
  private val TRANSITION_DEBOUNCE_MS: Long = 600L

  var nearbyFacility: FacilityType? = null
  var isNearRestaurantExit: Boolean = false
    private set
  var isNearChefCounter: Boolean = false
    private set

  private val actors = mutableMapOf<String, LobbyHeroActor>()
  private var cachedRoster: List<HeroRecord> = emptyList()
  private var lastSyncEpochSec: Long = System.currentTimeMillis() / 1000L
  private val interiorActors = mutableListOf<LobbyHeroActor>()

  companion object {
    val ENTERABLE_FACILITIES: Set<FacilityType> = setOf(
      FacilityType.HERO_QUARTERS,
      FacilityType.RESTAURANT,
      FacilityType.TRAINING_GROUND,
      FacilityType.ARMORY,
    )
  }

  // Resident Sanctuary Keepers tending the reconstructed 2.5D floating fortress-island facilities
  private val exteriorKeepers: List<LobbyHeroActor> = listOf(
    LobbyHeroActor(
      heroId = "npc_gatekeeper",
      name = "Hộ Cổng Elian",
      heroClass = HeroClassType.MAGE,
      personality = PersonalityType.STOIC,
      isInjured = false,
      isSanctuaryKeeperNpc = true,
      x = 1885f,
      y = 650f,
      targetX = 1885f,
      targetY = 650f,
      facingRight = true,
      activity = LobbyActivityState.GUARDING_GATE,
      speechBubble = "Cổng Đá Đen Vĩnh Hằng đã mở lối lên Tầng 01–10 thưa Master.",
      speechTimer = 6f,
    ),
    LobbyHeroActor(
      heroId = "npc_alchemist",
      name = "Giả Kim Sư Mira",
      heroClass = HeroClassType.PRIEST,
      personality = PersonalityType.CALCULATING,
      isInjured = false,
      isSanctuaryKeeperNpc = true,
      x = 1255f,
      y = 615f,
      targetX = 1255f,
      targetY = 615f,
      facingRight = true,
      activity = LobbyActivityState.STUDYING_RUNES,
      speechBubble = "Phòng Luyện Kim đã chuẩn bị sẵn trang bị tổng hợp!",
      speechTimer = 6f,
    ),
    LobbyHeroActor(
      heroId = "npc_registry_officer",
      name = "Thư Lại Sảnh Cael",
      heroClass = HeroClassType.ASSASSIN,
      personality = PersonalityType.CALCULATING,
      isInjured = false,
      isSanctuaryKeeperNpc = true,
      x = 835f,
      y = 1240f,
      targetX = 835f,
      targetY = 1240f,
      facingRight = true,
      activity = LobbyActivityState.STUDYING_RUNES,
    ),
    LobbyHeroActor(
      heroId = "npc_trainer",
      name = "Tổng Giáo Đầu Kael",
      heroClass = HeroClassType.KNIGHT,
      personality = PersonalityType.STOIC,
      isInjured = false,
      isSanctuaryKeeperNpc = true,
      x = 905f,
      y = 1745f,
      targetX = 905f,
      targetY = 1745f,
      facingRight = true,
      activity = LobbyActivityState.TRAINING,
      speechBubble = "Bãi Cát Luyện Võ đã dựng sẵn cọc gỗ cho các Anh Hùng!",
      speechTimer = 6f,
    ),
    LobbyHeroActor(
      heroId = "npc_tavern_host",
      name = "Tiếp Tân Liora",
      heroClass = HeroClassType.ARCHER,
      personality = PersonalityType.DEVOTED,
      isInjured = false,
      isSanctuaryKeeperNpc = true,
      x = 1125f,
      y = 985f,
      targetX = 1125f,
      targetY = 985f,
      facingRight = false,
      activity = LobbyActivityState.DINING,
    ),
    LobbyHeroActor(
      heroId = "npc_blacksmith",
      name = "Thợ Rèn Borin",
      heroClass = HeroClassType.BERSERKER,
      personality = PersonalityType.LOYAL,
      isInjured = false,
      isSanctuaryKeeperNpc = true,
      x = 1020f,
      y = 710f,
      targetX = 1020f,
      targetY = 710f,
      facingRight = true,
      activity = LobbyActivityState.INSPECTING_ARMORY,
    ),
  )

  private val restaurantInteriorKeepers: List<LobbyHeroActor> = listOf(
    LobbyHeroActor(
      heroId = "npc_chef_gustav",
      name = "Bếp Trưởng Gustav",
      heroClass = HeroClassType.KNIGHT,
      personality = PersonalityType.DEVOTED,
      isInjured = false,
      isSanctuaryKeeperNpc = true,
      x = 505f,
      y = 266f,
      targetX = 505f,
      targetY = 266f,
      facingRight = true,
      activity = LobbyActivityState.DINING,
      speechBubble = "Chào mừng Master vào Nhà Ăn! Thịt quay thảo mộc & súp nóng đã sẵn sàng!",
      speechTimer = 6.5f,
    ),
    LobbyHeroActor(
      heroId = "npc_hearth_bard",
      name = "Phụ Bếp Elena",
      heroClass = HeroClassType.PRIEST,
      personality = PersonalityType.LOYAL,
      isInjured = false,
      isSanctuaryKeeperNpc = true,
      x = 248f,
      y = 286f,
      targetX = 248f,
      targetY = 286f,
      facingRight = true,
      activity = LobbyActivityState.DINING,
    ),
  )

  private val dormitoryKeepers: List<LobbyHeroActor> = listOf(
    LobbyHeroActor(
      heroId = "npc_dorm_matron",
      name = "Y Quan Celine",
      heroClass = HeroClassType.PRIEST,
      personality = PersonalityType.DEVOTED,
      isInjured = false,
      isSanctuaryKeeperNpc = true,
      x = 535f,
      y = 285f,
      targetX = 535f,
      targetY = 285f,
      facingRight = true,
      activity = LobbyActivityState.RESTING,
      speechBubble = "Các giường tịnh dưỡng đã thay thảo dược sạch! Chạm vào Anh Hùng để xem hồ sơ hồi phục.",
      speechTimer = 6.5f,
    ),
  )

  private val trainingInteriorKeepers: List<LobbyHeroActor> = listOf(
    LobbyHeroActor(
      heroId = "npc_dojo_master",
      name = "Tổng Giáo Đầu Kael",
      heroClass = HeroClassType.KNIGHT,
      personality = PersonalityType.STOIC,
      isInjured = false,
      isSanctuaryKeeperNpc = true,
      x = 640f,
      y = 295f,
      targetX = 640f,
      targetY = 295f,
      facingRight = true,
      activity = LobbyActivityState.TRAINING,
      speechBubble = "Đại Sảnh Võ Học hỗ trợ chọn chuyên môn Cường Công, Sinh Lực, Phòng Ngự, Tốc Độ & Cổ Ngữ!",
      speechTimer = 6.5f,
    ),
  )

  private val armoryInteriorKeepers: List<LobbyHeroActor> = listOf(
    LobbyHeroActor(
      heroId = "npc_armory_smith",
      name = "Thợ Rèn Borin",
      heroClass = HeroClassType.BERSERKER,
      personality = PersonalityType.LOYAL,
      isInjured = false,
      isSanctuaryKeeperNpc = true,
      x = 535f,
      y = 305f,
      targetX = 535f,
      targetY = 305f,
      facingRight = true,
      activity = LobbyActivityState.INSPECTING_ARMORY,
      speechBubble = "Kho Vũ Khí đã xếp sẵn kiếm thuẫn! Master có thể so sánh chỉ số và tháo lắp ngay.",
      speechTimer = 6.5f,
    ),
  )

  val heroActors: List<LobbyHeroActor>
    get() = when (activeInterior) {
      FacilityType.RESTAURANT -> interiorActors + restaurantInteriorKeepers
      FacilityType.HERO_QUARTERS -> interiorActors + dormitoryKeepers
      FacilityType.TRAINING_GROUND -> interiorActors + trainingInteriorKeepers
      FacilityType.ARMORY -> interiorActors + armoryInteriorKeepers
      else -> actors.values.toList() + exteriorKeepers
    }

  fun syncExpansions(expansions: Set<IslandExpansionMilestone>) {
    unlockedExpansions = expansions
  }

  fun setCameraPosition(newCamX: Float, newCamY: Float) {
    val margin = if (isInsideInterior) mapLayout.wallMargin + 40f else 180f
    val maxW = if (!isInsideInterior && IslandExpansionMilestone.EAST_CELESTIAL_GARDEN in unlockedExpansions) {
      SanctuaryIslandGeometry.EXPANDED_WORLD_WIDTH
    } else {
      mapLayout.width
    }
    camX = newCamX.coerceIn(margin, maxW - margin)
    camY = newCamY.coerceIn(margin, mapLayout.height - margin)
  }

  fun panCameraWorld(worldDx: Float, worldDy: Float) {
    setCameraPosition(camX + worldDx, camY + worldDy)
  }

  fun panCamera(dx: Float, dy: Float) {
    setCameraPosition(camX - dx, camY - dy)
  }

  fun setZoomClamped(newZoom: Float) {
    zoom = newZoom.coerceIn(0.26f, 1.52f)
  }

  fun zoomCamera(zoomDelta: Float) {
    setZoomClamped(zoom + zoomDelta)
  }

  fun focusCameraOnHero(heroId: String) {
    val target = (if (isInsideInterior) interiorActors else actors.values).firstOrNull { it.heroId == heroId }
    if (target != null) {
      setCameraPosition(target.x, target.y)
      selectedHeroId = heroId
    }
  }

  fun focusCameraOnFacility(facility: FacilityType) {
    val zone = TowerFloorCatalog.SANCTUARY_ZONES.find { it.facility == facility }
    if (zone != null) {
      setCameraPosition(zone.centerX, zone.centerY)
      selectedFacility = facility
    }
  }

  fun selectHeroAt(worldX: Float, worldY: Float): LobbyHeroActor? {
    val pool = heroActors
    val candidate = pool.minByOrNull { hypot(worldX - it.x, worldY - it.y) }
    if (candidate != null && hypot(worldX - candidate.x, worldY - candidate.y) <= 72f) {
      if (!candidate.isSanctuaryKeeperNpc) {
        selectedHeroId = candidate.heroId
      }
      return candidate
    }
    return null
  }

  fun selectExpansionAt(worldX: Float, worldY: Float): IslandExpansionMilestone? {
    if (isInsideInterior) return null
    if (hypot(worldX - SanctuaryIslandGeometry.EAST_BEACON_X, worldY - SanctuaryIslandGeometry.EAST_BEACON_Y) <= 135f ||
      hypot(worldX - SanctuaryIslandGeometry.EAST_TERRACE_CENTER_X, worldY - SanctuaryIslandGeometry.EAST_TERRACE_CENTER_Y) <= 210f
    ) {
      return IslandExpansionMilestone.EAST_CELESTIAL_GARDEN
    }
    if (hypot(worldX - SanctuaryIslandGeometry.NORTH_BEACON_X, worldY - SanctuaryIslandGeometry.NORTH_BEACON_Y) <= 135f ||
      hypot(worldX - SanctuaryIslandGeometry.NORTH_DOCK_CENTER_X, worldY - SanctuaryIslandGeometry.NORTH_DOCK_CENTER_Y) <= 185f
    ) {
      return IslandExpansionMilestone.NORTH_ASTRAL_DOCK
    }
    return null
  }

  fun selectFacilityAt(worldX: Float, worldY: Float): FacilityType? {
    if (isInsideInterior) {
      // Tapping main station inside an interior returns that active facility
      val active = activeInterior ?: return null
      val (ex, ey) = TowerFloorCatalog.interiorExitPoint(active)
      if (hypot(worldX - ex, worldY - ey) <= 85f) return null
      if (worldY <= 345f && abs(worldX - mapLayout.width * 0.5f) <= 220f) {
        return active
      }
      return null
    }
    // Priority 1: doorway trigger (door south-face arch) — fixes 30px north bias
    val doorwayFac = SanctuaryHouseEntrances.facilityAtDoorway(worldX, worldY, zoom)
    if (doorwayFac != null) {
      selectedFacility = doorwayFac
      return doorwayFac
    }
    val zone = TowerFloorCatalog.SANCTUARY_ZONES.minByOrNull { hypot(worldX - it.centerX, worldY - it.centerY) }
    if (zone != null && hypot(worldX - zone.centerX, worldY - zone.centerY) <= zone.interactRadius * 1.15f) {
      selectedFacility = zone.facility
      return zone.facility
    }
    if (hypot(worldX - TowerFloorCatalog.HERO_REGISTRY_X, worldY - TowerFloorCatalog.HERO_REGISTRY_Y) <= 130f) {
      selectedFacility = FacilityType.SUMMONING_HALL
      return FacilityType.SUMMONING_HALL
    }
    if (hypot(worldX - TowerFloorCatalog.FLOOR_VISTA_X, worldY - TowerFloorCatalog.FLOOR_VISTA_Y) <= 130f) {
      selectedFacility = FacilityType.TOWER_GATE
      return FacilityType.TOWER_GATE
    }
    return null
  }

  private fun assignHeroDestination(actor: LobbyHeroActor, rawDestX: Float, rawDestY: Float) {
    val route = SanctuaryNavGraph.computeRoute(actor.x, actor.y, rawDestX, rawDestY)
    actor.waypointQueue.clear()
    route.forEach { actor.waypointQueue.addLast(it) }
    val firstStep = actor.waypointQueue.removeFirstOrNull() ?: (rawDestX to rawDestY)
    actor.targetX = firstStep.first
    actor.targetY = firstStep.second
    actor.stuckTimer = 0f
    actor.lastProgressX = actor.x
    actor.lastProgressY = actor.y
  }

  fun commandMoveHero(heroId: String, targetX: Float, targetY: Float) {
    actors[heroId]?.let { hero ->
      assignHeroDestination(hero, targetX, targetY)
      hero.isMoving = true
      hero.isPlayerCommanded = true
      hero.activity = LobbyActivityState.PATROLLING
      hero.speechBubble = "Rõ lệnh Master! Đang di chuyển."
      hero.speechTimer = 4f
    }
  }

  fun commandHeroHold(heroId: String) {
    actors[heroId]?.let { hero ->
      hero.waypointQueue.clear()
      hero.targetX = hero.x
      hero.targetY = hero.y
      hero.isMoving = false
      hero.isPlayerCommanded = true
      hero.activity = LobbyActivityState.GUARDING_GATE
      hero.speechBubble = "Đang giữ vị trí phòng thủ thưa Master!"
      hero.speechTimer = 4f
    }
  }

  fun commandHeroTask(heroId: String, task: HeroTaskType) {
    actors[heroId]?.let { hero ->
      val (zx, zy) = zoneCenter(task.targetFacility)
      val destX = zx + Random.nextFloat() * 44f - 22f
      val destY = zy + 28f + Random.nextFloat() * 28f
      assignHeroDestination(hero, destX, destY)
      hero.isMoving = true
      hero.isPlayerCommanded = true
      hero.activity = when (task) {
        HeroTaskType.TRAIN -> LobbyActivityState.TRAINING
        HeroTaskType.DINING -> LobbyActivityState.DINING
        HeroTaskType.ALCHEMY -> LobbyActivityState.STUDYING_RUNES
        HeroTaskType.FORGE -> LobbyActivityState.INSPECTING_ARMORY
        HeroTaskType.PREPARE_TOWER -> LobbyActivityState.GUARDING_GATE
        HeroTaskType.RETURN_QUARTERS -> LobbyActivityState.RESTING
      }
      hero.speechBubble = "Đang tiến về ${task.labelVi}!"
      hero.speechTimer = 4.5f
    }
  }

  fun enterFacilityInterior(facility: FacilityType) {
    if (facility !in ENTERABLE_FACILITIES) return
    if (activeInterior == facility) return
    if (!isInsideInterior) {
      savedExteriorX = camX
      savedExteriorY = camY
      savedExteriorZoom = zoom
      savedSelectedHeroId = selectedHeroId
      savedSelectedFacility = selectedFacility ?: facility
    }
    activeInterior = facility
    val (ex, _) = TowerFloorCatalog.interiorExitPoint(facility)
    camX = ex
    camY = 470f
    zoom = 0.92f
    nearbyFacility = facility
    rebuildActiveInteriorActors()
  }

  fun exitFacilityInterior() {
    if (!isInsideInterior) return

    val exitedFrom = activeInterior
    activeInterior = null
    // The external Master's camera is not a colliding world actor.
    camX = savedExteriorX
    camY = savedExteriorY
    zoom = savedExteriorZoom
    selectedHeroId = savedSelectedHeroId
    selectedFacility = savedSelectedFacility ?: exitedFrom
    isNearRestaurantExit = false
    isNearChefCounter = false
    nearbyFacility = exitedFrom
  }

  fun enterRestaurantInterior() {
    enterFacilityInterior(FacilityType.RESTAURANT)
  }

  fun exitRestaurantInterior() {
    exitFacilityInterior()
  }

  private fun rebuildActiveInteriorActors() {
    interiorActors.clear()
    val interior = activeInterior ?: return
    val nowSec = lastSyncEpochSec

    when (interior) {
      FacilityType.HERO_QUARTERS -> {
        val bedSpots = listOf(
          265f to 332f,
          265f to 442f,
          265f to 552f,
          955f to 332f,
          955f to 442f,
          955f to 552f,
        )
        val restingOrInjured = cachedRoster.filter {
          it.isCurrentlyInjured(nowSec) || it.statusType == HeroStatusType.RESTING
        }
        val extraResidents = cachedRoster
          .filterNot { h -> restingOrInjured.any { it.id == h.id } || h.statusType == HeroStatusType.TRAINING }
          .take(2)

        restingOrInjured.take(bedSpots.size).forEachIndexed { idx, hero ->
          val (bx, by) = bedSpots[idx]
          val injured = hero.isCurrentlyInjured(nowSec)
          interiorActors += LobbyHeroActor(
            heroId = hero.id,
            name = hero.name,
            heroClass = hero.classType,
            personality = hero.personalityType,
            isInjured = true,
            x = bx,
            y = by,
            targetX = bx,
            targetY = by,
            facingRight = idx < 3,
            activity = LobbyActivityState.RESTING,
            speechBubble = if (injured) {
              "Đang tịnh dưỡng hồi thương (${hero.remainingInjurySeconds(nowSec)}s)..."
            } else {
              "Nghỉ ngơi trên giường êm giúp hồi lại Sĩ Khí thật nhanh!"
            },
            speechTimer = 5.5f,
          )
        }

        val loungeSpots = listOf(445f to 665f, 775f to 665f)
        extraResidents.forEachIndexed { idx, hero ->
          val (lx, ly) = loungeSpots[idx % loungeSpots.size]
          interiorActors += LobbyHeroActor(
            heroId = hero.id,
            name = hero.name,
            heroClass = hero.classType,
            personality = hero.personalityType,
            isInjured = false,
            x = lx,
            y = ly,
            targetX = lx,
            targetY = ly,
            facingRight = idx == 0,
            activity = LobbyActivityState.RESTING,
            speechBubble = if (idx == 0) "Phòng nghỉ luôn ấm áp ánh đèn thưa Master." else null,
            speechTimer = if (idx == 0) 4.5f else 0f,
          )
        }
      }

      FacilityType.RESTAURANT -> {
        data class BanquetSeat(val x: Float, val y: Float, val faceRight: Boolean, val faceNorth: Boolean)
        val banquetSeats = listOf(
          BanquetSeat(216f, 374f, faceRight = true, faceNorth = false),
          BanquetSeat(930f, 396f, faceRight = false, faceNorth = true),
          BanquetSeat(202f, 558f, faceRight = true, faceNorth = false),
          BanquetSeat(1038f, 558f, faceRight = false, faceNorth = false),
          BanquetSeat(288f, 694f, faceRight = true, faceNorth = false),
          BanquetSeat(952f, 694f, faceRight = false, faceNorth = false),
        )
        val quotes = listOf(
          "Món thịt nướng thảo nguyên ở đây tăng mạnh sức chiến đấu thật!",
          "Trước khi leo tầng mới, không gì tuyệt hơn một bữa tiệc nóng.",
          "Ngồi bên lò sưởi này làm tan biến hết mệt mỏi trong Tháp.",
          "Cảm ơn Master đã luôn chăm lo hậu cần cho cả đội!",
        )
        // Strict consistency: injured or actively training heroes never appear dining!
        val eligibleDiners = cachedRoster.filter {
          !it.isCurrentlyInjured(nowSec) && it.statusType != HeroStatusType.TRAINING
        }
        eligibleDiners.take(banquetSeats.size).forEachIndexed { idx, hero ->
          val seat = banquetSeats[idx]
          // First diner walks in from doorway -> approaches seat -> sits & eats!
          val startsWalkingIn = idx == 0
          val startX = if (startsWalkingIn) TowerFloorCatalog.RESTAURANT_EXIT_X else seat.x
          val startY = if (startsWalkingIn) TowerFloorCatalog.RESTAURANT_EXIT_Y - 35f else seat.y
          interiorActors += LobbyHeroActor(
            heroId = hero.id,
            name = hero.name,
            heroClass = hero.classType,
            personality = hero.personalityType,
            isInjured = false,
            x = startX,
            y = startY,
            targetX = seat.x,
            targetY = seat.y,
            facingRight = seat.faceRight,
            facingNorth = seat.faceNorth,
            isMoving = startsWalkingIn,
            activity = LobbyActivityState.DINING,
            waitTimer = 8f + idx * 2.5f,
            speechBubble = if (idx < 2) quotes[idx % quotes.size] else null,
            speechTimer = if (idx < 2) 5.5f + idx * 1.5f else 0f,
            isSeatedDining = !startsWalkingIn,
          )
        }
      }

      FacilityType.TRAINING_GROUND -> {
        val drillSpots = listOf(
          315f to 365f,
          315f to 520f,
          965f to 365f,
          965f to 520f,
          565f to 465f,
          715f to 465f,
        )
        val trainees = cachedRoster
          .filter { !it.isCurrentlyInjured(nowSec) }
          .sortedByDescending { it.statusType == HeroStatusType.TRAINING }
          .take(drillSpots.size)
        trainees.forEachIndexed { idx, hero ->
          val (dx, dy) = drillSpots[idx]
          interiorActors += LobbyHeroActor(
            heroId = hero.id,
            name = hero.name,
            heroClass = hero.classType,
            personality = hero.personalityType,
            isInjured = false,
            x = dx,
            y = dy,
            targetX = dx,
            targetY = dy,
            facingRight = dx < 640f,
            activity = LobbyActivityState.TRAINING,
            attackPracticeAnim = (idx * 0.27f) % 1f,
            speechBubble = if (hero.statusType == HeroStatusType.TRAINING) {
              "Đang rèn luyện chuyên sâu theo chỉ thị của Master!"
            } else null,
            speechTimer = if (hero.statusType == HeroStatusType.TRAINING) 5.0f else 0f,
          )
        }
      }

      FacilityType.ARMORY -> {
        val inspectSpots = listOf(
          335f to 395f,
          865f to 395f,
          455f to 585f,
          745f to 585f,
        )
        cachedRoster
          .filter { !it.isCurrentlyInjured(nowSec) && it.statusType != HeroStatusType.RESTING }
          .take(inspectSpots.size)
          .forEachIndexed { idx, hero ->
            val (ix, iy) = inspectSpots[idx]
            interiorActors += LobbyHeroActor(
              heroId = hero.id,
              name = hero.name,
              heroClass = hero.classType,
              personality = hero.personalityType,
              isInjured = false,
              x = ix,
              y = iy,
              targetX = ix,
              targetY = iy,
              facingRight = ix < 600f,
              activity = LobbyActivityState.INSPECTING_ARMORY,
              speechBubble = if (idx == 0) "Vũ khí vừa mài sắc bén hơn hẳn rồi Master!" else null,
              speechTimer = if (idx == 0) 4.8f else 0f,
            )
          }
      }

      else -> Unit
    }
  }

  fun syncHeroes(heroes: List<HeroRecord>, nowEpochSec: Long) {
    cachedRoster = heroes
    lastSyncEpochSec = nowEpochSec

    // Strict consistency: Injured heroes rest on Dormitory beds indoors — never patrol outdoors!
    val healthyOutdoorHeroes = heroes.filter { !it.isCurrentlyInjured(nowEpochSec) }
    val activeOutdoorIds = healthyOutdoorHeroes.map { it.id }.toSet()
    actors.keys.retainAll(activeOutdoorIds)

    healthyOutdoorHeroes.forEachIndexed { idx, hero ->
      val existing = actors[hero.id]
      if (existing == null) {
        val initialSpot = pickPreferredSpotForHero(hero, idx)
        val initialActivity = when (hero.statusType) {
          HeroStatusType.TRAINING -> LobbyActivityState.TRAINING
          HeroStatusType.RESTING -> LobbyActivityState.RESTING
          else -> LobbyActivityState.PATROLLING
        }
        val actor = LobbyHeroActor(
          heroId = hero.id,
          name = hero.name,
          heroClass = hero.classType,
          personality = hero.personalityType,
          isInjured = false,
          x = initialSpot.first,
          y = initialSpot.second,
          targetX = initialSpot.first,
          targetY = initialSpot.second,
          facingRight = idx % 2 == 0,
          activity = initialActivity,
          waitTimer = 1.2f + (idx % 3) * 0.9f,
          speechBubble = hero.personalityType.sampleQuoteVi,
          speechTimer = 4.5f + idx * 1.4f,
        )
        actors[hero.id] = actor
      } else {
        existing.isInjured = false
        when (hero.statusType) {
          HeroStatusType.TRAINING -> if (existing.activity != LobbyActivityState.TRAINING) {
            val spot = trainingPracticeSpot(idx)
            assignHeroDestination(existing, spot.first, spot.second)
            existing.activity = LobbyActivityState.TRAINING
          }
          HeroStatusType.RESTING -> if (existing.activity != LobbyActivityState.RESTING) {
            val restSpot = restingGardenSpot(idx)
            assignHeroDestination(existing, restSpot.first, restSpot.second)
            existing.activity = LobbyActivityState.RESTING
          }
          else -> Unit
        }
      }
    }

    if (isInsideInterior) {
      rebuildActiveInteriorActors()
    }
  }

  fun setTapDestination(worldX: Float, worldY: Float) {
    val currentSelected = selectedHeroId
    if (!isInsideInterior && currentSelected != null && actors.containsKey(currentSelected)) {
      commandMoveHero(currentSelected, worldX, worldY)
    } else {
      setCameraPosition(worldX, worldY)
    }
  }

  fun clearTapDestination() {
    selectedHeroId = null
  }

  fun step(dt: Float, inputDx: Float = 0f, inputDy: Float = 0f) {
    if (abs(inputDx) > 0.02f || abs(inputDy) > 0.02f) {
      panCameraWorld(inputDx * 440f * dt, inputDy * 440f * dt)
    }

    val interior = activeInterior
    if (interior != null) {
      val (exitX, exitY) = TowerFloorCatalog.interiorExitPoint(interior)
      isNearRestaurantExit = hypot(camX - exitX, camY - exitY) <= 130f
      isNearChefCounter = hypot(camX - mapLayout.width * 0.5f, camY - 310f) <= 195f
      nearbyFacility = interior

      heroActors.forEachIndexed { idx, actor ->
        if (actor.speechTimer > 0f) {
          actor.speechTimer = (actor.speechTimer - dt).coerceAtLeast(0f)
          if (actor.speechTimer <= 0f) actor.speechBubble = null
        }

        // Animate indoor training loops, smithing hammer swings, and dining enter -> sit -> leave cycle!
        if (actor.activity == LobbyActivityState.TRAINING || actor.heroId == "npc_armory_smith") {
          actor.attackPracticeAnim = (actor.attackPracticeAnim + dt * 1.9f) % 1f
        } else if (interior == FacilityType.RESTAURANT && !actor.isSanctuaryKeeperNpc) {
          val dx = actor.targetX - actor.x
          val dy = actor.targetY - actor.y
          val dist = hypot(dx, dy)
          if (dist > 10f) {
            val spd = 98f
            actor.x += (dx / dist) * spd * dt
            actor.y += (dy / dist) * spd * dt
            actor.facingRight = dx >= 0f
            actor.isMoving = true
            actor.isSeatedDining = false
          } else {
            actor.isMoving = false
            val nearExit = hypot(actor.x - exitX, actor.y - (exitY - 35f)) <= 24f
            actor.isSeatedDining = !nearExit
            actor.waitTimer -= dt
            if (actor.waitTimer <= 0f) {
              if (!nearExit) {
                // Finished meal -> walk politely toward doorway
                actor.targetX = exitX
                actor.targetY = exitY - 35f
                actor.waitTimer = 3.0f
              } else {
                // Re-enter to a banquet table
                actor.targetX = if (idx % 2 == 0) 216f else 930f
                actor.targetY = 374f + (idx % 3) * 160f
                actor.waitTimer = 9.5f
              }
            }
          }
        }
      }
      return
    }

    isNearRestaurantExit = false
    isNearChefCounter = false

    // Detect nearby — doorway trigger has priority over zone-center
    val doorwayNearby = SanctuaryHouseEntrances.facilityAtDoorway(camX, camY, zoom)
    val primaryNearby = if (doorwayNearby != null) doorwayNearby else TowerFloorCatalog.SANCTUARY_ZONES
      .minByOrNull { hypot(camX - it.centerX, camY - it.centerY) }
      ?.takeIf { hypot(camX - it.centerX, camY - it.centerY) <= it.interactRadius * 1.45f }
      ?.facility
    nearbyFacility = primaryNearby ?: when {
      hypot(camX - TowerFloorCatalog.HERO_REGISTRY_X, camY - (TowerFloorCatalog.HERO_REGISTRY_Y + 35f)) <= 185f ->
        FacilityType.SUMMONING_HALL
      hypot(camX - TowerFloorCatalog.FLOOR_VISTA_X, camY - (TowerFloorCatalog.FLOOR_VISTA_Y + 35f)) <= 185f ->
        FacilityType.TOWER_GATE
      else -> null
    }

    // Step Autonomous & Commanded Heroes along Waypoint Graph Routes
    actors.values.forEachIndexed { idx, actor ->
      if (actor.speechTimer > 0f) {
        actor.speechTimer = (actor.speechTimer - dt).coerceAtLeast(0f)
        if (actor.speechTimer <= 0f) actor.speechBubble = null
      }

      val dx = actor.targetX - actor.x
      val dy = actor.targetY - actor.y
      val dist = hypot(dx, dy)

      if (dist > 14f) {
        val heroSpeed = 124f
        val (rx, ry) = CollisionPhysics.resolvePosition(
          targetX = actor.x + (dx / dist) * heroSpeed * dt,
          targetY = actor.y + (dy / dist) * heroSpeed * dt,
          entityRadius = 16f,
          mapWidth = mapLayout.width,
          mapHeight = mapLayout.height,
          marginWall = mapLayout.wallMargin,
          obstacles = mapLayout.obstacles,
          eastGardenUnlocked = IslandExpansionMilestone.EAST_CELESTIAL_GARDEN in unlockedExpansions,
          northDockUnlocked = IslandExpansionMilestone.NORTH_ASTRAL_DOCK in unlockedExpansions,
        )
        val movedDist = hypot(rx - actor.lastProgressX, ry - actor.lastProgressY)
        if (movedDist < 2.5f) {
          actor.stuckTimer += dt
          if (actor.stuckTimer > 0.85f) {
            actor.stuckTimer = 0f
            val nextWp = actor.waypointQueue.removeFirstOrNull()
            if (nextWp != null) {
              actor.targetX = nextWp.first
              actor.targetY = nextWp.second
            } else {
              actor.targetX = actor.x
              actor.targetY = actor.y
            }
          }
        } else {
          actor.stuckTimer = 0f
          actor.lastProgressX = rx
          actor.lastProgressY = ry
        }

        actor.x = rx
        actor.y = ry
        actor.facingRight = dx >= 0f
        actor.facingNorth = (dy / dist) < -0.45f
        actor.isMoving = true
        actor.attackPracticeAnim = 0f
      } else if (actor.waypointQueue.isNotEmpty()) {
        val nextWp = actor.waypointQueue.removeFirst()
        actor.targetX = nextWp.first
        actor.targetY = nextWp.second
        actor.isMoving = true
      } else {
        actor.isMoving = false
        actor.facingNorth = false
        if (actor.activity == LobbyActivityState.TRAINING) {
          actor.facingRight = (idx % 2 == 0)
          actor.attackPracticeAnim = (actor.attackPracticeAnim + dt * 2.0f) % 1f
        } else {
          actor.attackPracticeAnim = 0f
        }

        actor.waitTimer -= dt
        if (actor.waitTimer <= 0f) {
          actor.isPlayerCommanded = false
          chooseNextRoutine(actor, idx)
        }
      }

      val distToMaster = hypot(masterX - actor.x, masterY - actor.y)
      if (distToMaster < 160f && actor.speechBubble == null && Random.nextFloat() < 0.014f) {
        actor.speechBubble = if (actor.isInjured) {
          "Vết thương sắp lành rồi... Xin hãy đợi tôi nhé Master!"
        } else {
          actor.personality.sampleQuoteVi
        }
        actor.speechTimer = 4.5f
      }
    }

    // Step Resident Sanctuary Keepers
    exteriorKeepers.forEach { keeper ->
      if (keeper.speechTimer > 0f) {
        keeper.speechTimer = (keeper.speechTimer - dt).coerceAtLeast(0f)
        if (keeper.speechTimer <= 0f) keeper.speechBubble = null
      }
      if (keeper.heroId == "npc_blacksmith" || keeper.heroId == "npc_trainer") {
        keeper.attackPracticeAnim = (keeper.attackPracticeAnim + dt * 1.4f) % 1f
      }
      if (hypot(masterX - keeper.x, masterY - keeper.y) < 165f && keeper.speechBubble == null && Random.nextFloat() < 0.018f) {
        keeper.speechBubble = when (keeper.heroId) {
          "npc_gatekeeper" -> "Bước lên bậc thang Đá Đen để mở Cổng Tháp Vĩnh Hằng (Tầng 1–10)!"
          "npc_registry_officer" -> "Sảnh Hồ Sơ Anh Hùng đang cập nhật đội hình xuất chinh!"
          "npc_blacksmith" -> "Lò rèn luôn đỏ lửa! Hãy rèn giáp trụ tốt nhất cho đội quân."
          "npc_alchemist" -> "Quặng Sắt và Tinh Hoa từ Tháp có thể dung hợp thành thần binh!"
          "npc_trainer" -> "Bãi cát và cọc gỗ luyện võ luôn sẵn sàng nâng cấp sức mạnh!"
          else -> "Bước vào cửa Nhà Ăn để dùng Đại Tiệc tăng +12% Sức Mạnh trước giờ xuất chinh!"
        }
        keeper.speechTimer = 4.5f
      }
    }
  }

  private fun trainingPracticeSpot(idx: Int): Pair<Float, Float> {
    val practiceSpots = listOf(
      675f to 1625f,
      795f to 1735f,
      985f to 1895f,
      1215f to 1835f,
    )
    return practiceSpots[idx % practiceSpots.size]
  }

  private fun restingGardenSpot(idx: Int): Pair<Float, Float> {
    if (IslandExpansionMilestone.EAST_CELESTIAL_GARDEN in unlockedExpansions && idx % 2 == 0) {
      return (SanctuaryIslandGeometry.EAST_TERRACE_CENTER_X - 45f + (idx % 3) * 35f) to
        (SanctuaryIslandGeometry.EAST_TERRACE_CENTER_Y + 42f)
    }
    val q = zoneCenter(FacilityType.HERO_QUARTERS)
    return (q.first - 25f + (idx % 3) * 28f) to (q.second + 52f)
  }

  private fun chooseNextRoutine(actor: LobbyHeroActor, indexSeed: Int) {
    val heroRecord = cachedRoster.firstOrNull { it.id == actor.heroId }
    if (actor.isInjured || heroRecord?.statusType == HeroStatusType.RESTING) {
      val spot = restingGardenSpot(indexSeed)
      assignHeroDestination(actor, spot.first, spot.second)
      actor.activity = LobbyActivityState.RESTING
      actor.waitTimer = 6f + Random.nextFloat() * 4f
      return
    }
    if (heroRecord?.statusType == HeroStatusType.TRAINING) {
      val spot = trainingPracticeSpot(indexSeed)
      assignHeroDestination(actor, spot.first, spot.second)
      actor.activity = LobbyActivityState.TRAINING
      actor.waitTimer = 6.5f + Random.nextFloat() * 3.5f
      return
    }

    val roll = Random.nextInt(100)
    val chosenFacility = when (actor.personality) {
      PersonalityType.RECKLESS, PersonalityType.STOIC -> when {
        roll < 42 -> FacilityType.TRAINING_GROUND
        roll < 65 -> FacilityType.TOWER_GATE
        roll < 82 -> FacilityType.ARMORY
        else -> FacilityType.RESTAURANT
      }
      PersonalityType.CALCULATING -> when {
        roll < 40 -> FacilityType.SYNTHESIS_LAB
        roll < 70 -> FacilityType.SUMMONING_HALL
        else -> FacilityType.TOWER_GATE
      }
      PersonalityType.DEVOTED, PersonalityType.LOYAL -> when {
        roll < 38 -> FacilityType.TOWER_GATE
        roll < 68 -> FacilityType.RESTAURANT
        else -> FacilityType.HERO_QUARTERS
      }
      PersonalityType.CYNICAL -> when {
        roll < 42 -> FacilityType.RESTAURANT
        roll < 75 -> FacilityType.ARMORY
        else -> FacilityType.TRAINING_GROUND
      }
    }

    if (roll > 80) {
      val useEastGarden = IslandExpansionMilestone.EAST_CELESTIAL_GARDEN in unlockedExpansions && indexSeed % 2 == 0
      val destX = if (useEastGarden) {
        SanctuaryIslandGeometry.EAST_TERRACE_CENTER_X + Random.nextFloat() * 90f - 45f
      } else {
        TowerFloorCatalog.SANCTUARY_CENTER_X + Random.nextFloat() * 340f - 170f
      }
      val destY = if (useEastGarden) {
        SanctuaryIslandGeometry.EAST_TERRACE_CENTER_Y + Random.nextFloat() * 80f - 40f
      } else {
        TowerFloorCatalog.SANCTUARY_PLAZA_Y + Random.nextFloat() * 240f - 120f
      }
      if (useEastGarden) {
        actor.waypointQueue.clear()
        actor.waypointQueue.addLast(2850f to 1265f)
        actor.waypointQueue.addLast(destX to destY)
        val first = actor.waypointQueue.removeFirst()
        actor.targetX = first.first
        actor.targetY = first.second
      } else {
        assignHeroDestination(actor, destX, destY)
      }
      actor.activity = LobbyActivityState.CHATTING
      actor.speechBubble = when (actor.personality) {
        PersonalityType.RECKLESS -> "Tầng tiếp theo để tôi mở đường cho!"
        PersonalityType.CALCULATING -> "Đội hình 3 lớp Tiền tuyến - Cung - Phép đang rất cân bằng."
        PersonalityType.CYNICAL -> "Nhớ mang đủ trang bị từ Kho Vũ Khí trước khi vào Cổng."
        else -> "Nắng ấm và hoa anh đào trên Thánh Địa hôm nay đẹp thật."
      }
      actor.speechTimer = 4.2f
    } else {
      val (destX, destY) = when (chosenFacility) {
        FacilityType.TRAINING_GROUND -> trainingPracticeSpot(indexSeed)
        FacilityType.SUMMONING_HALL -> (2315f + (indexSeed % 3) * 32f) to (1855f + (indexSeed % 2) * 28f)
        FacilityType.TOWER_GATE -> (1915f + (indexSeed % 3) * 42f) to 665f
        else -> {
          val (zx, zy) = zoneCenter(chosenFacility)
          (zx + Random.nextFloat() * 56f - 28f) to (zy + 42f + (indexSeed % 2) * 22f)
        }
      }
      assignHeroDestination(actor, destX, destY)
      actor.activity = when (chosenFacility) {
        FacilityType.TRAINING_GROUND -> LobbyActivityState.TRAINING
        FacilityType.RESTAURANT -> LobbyActivityState.DINING
        FacilityType.ARMORY -> LobbyActivityState.INSPECTING_ARMORY
        FacilityType.SYNTHESIS_LAB, FacilityType.SUMMONING_HALL -> LobbyActivityState.STUDYING_RUNES
        FacilityType.TOWER_GATE -> LobbyActivityState.GUARDING_GATE
        else -> LobbyActivityState.PATROLLING
      }
    }
    actor.waitTimer = 5.5f + Random.nextFloat() * 5.0f
  }

  private fun pickPreferredSpotForHero(
    hero: HeroRecord,
    idx: Int,
  ): Pair<Float, Float> {
    if (hero.statusType == HeroStatusType.TRAINING) return trainingPracticeSpot(idx)
    if (hero.statusType == HeroStatusType.RESTING) return restingGardenSpot(idx)
    return when (idx % 5) {
      0 -> 1785f to 1185f // West Gold Rune on Circular White Stone Plaza
      1 -> 1945f to 995f  // North Sakura Rune on Circular White Stone Plaza
      2 -> 1335f to 1245f // Cobblestone promenade beside Hedge Courtyard
      3 -> 855f to 1785f  // Golden-Sand Training Arena
      else -> when (hero.personalityType) {
        PersonalityType.CALCULATING -> 1560f to 675f
        else -> 2085f to 1365f // SE Circular White Stone Plaza
      }
    }
  }

  private fun zoneCenter(facility: FacilityType): Pair<Float, Float> {
    val z = TowerFloorCatalog.SANCTUARY_ZONES.first { it.facility == facility }
    return z.centerX to z.centerY
  }
}
