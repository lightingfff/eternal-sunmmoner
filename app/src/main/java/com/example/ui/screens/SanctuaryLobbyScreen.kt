package com.example.ui.screens

import com.example.ui.theme.SanctuaryUi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import com.example.ui.components.FantasyButton as Button
import com.example.ui.components.FantasySkin
import com.example.ui.components.p28.ESBannerNotice
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import com.example.game.lobby.HeroTaskType
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FeastBuffHelper
import com.example.data.GameSettings
import com.example.performance.FixedSceneSimulation
import com.example.performance.SceneFrameLoop
import com.example.performance.ScenePerformanceOverlay
import com.example.performance.ScenePerformanceProbe
import com.example.performance.ScenePerformanceSnapshot
import com.example.data.SanctuaryProgressionHelper
import com.example.data.model.EquipmentRecord
import com.example.data.model.FacilityType
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.HeroStatusType
import com.example.data.model.IslandExpansionMilestone
import com.example.data.model.MasterProfile
import com.example.data.model.SanctuaryMealRecipe
import com.example.data.model.TrainingFocus
import com.example.game.lobby.LobbyHeroActor
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.map.ObliqueCamera
import com.example.game.map.SanctuaryIslandGeometry
import com.example.game.map.WorldObstacle
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawForegroundAtmosphere
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.PixelSpriteRenderer.drawSanctuaryZoneFloorRing
import com.example.game.pixel.PixelSpriteRenderer.drawSouthernForegroundWalls
import com.example.game.pixel.PixelSpriteRenderer.drawActorCanopyOcclusion
import com.example.game.pixel.PixelSpriteRenderer.drawWorldBackground
import com.example.game.story.ManhwaStoryData
import com.example.game.tower.TowerFloorCatalog
import com.example.ui.SummonerUiState
import com.example.ui.components.PixelHeroPortraitAvatar
import com.example.ui.components.ResourcePillBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ArcaneViolet
import com.example.ui.theme.AstralSlate
import com.example.ui.theme.BorderRune
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.EmeraldVitality
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.RuneSilver
import com.example.ui.theme.SanctuaryStone
import com.example.ui.theme.SummonerGold
import kotlin.math.abs
import kotlin.math.hypot

internal sealed interface LobbyDrawableItem {
  val sortY: Float

  data class Prop(val obs: WorldObstacle) : LobbyDrawableItem {
    override val sortY: Float get() = obs.y + obs.sortYBias
  }

  data class Master(
    val x: Float,
    val y: Float,
    val right: Boolean,
    val north: Boolean,
    val moving: Boolean,
  ) : LobbyDrawableItem {
    override val sortY: Float get() = y
  }

  data class HeroNpc(val actor: LobbyHeroActor) : LobbyDrawableItem {
    override val sortY: Float get() = actor.y
  }
}

/** Static props are sorted once. Reuse wrappers, insertion-sort the small moving roster,
 * then merge into one reusable list; no per-frame buildList/sorted copy/TimSort buffer. */
internal class LobbyDrawQueue {
  private var sourceProps: List<WorldObstacle>? = null
  private var sourceActors: List<LobbyHeroActor>? = null
  private var props: List<LobbyDrawableItem.Prop> = emptyList()
  private val actors = ArrayList<LobbyDrawableItem.HeroNpc>()
  private val result = ArrayList<LobbyDrawableItem>()
  fun ordered(obstacles: List<WorldObstacle>, roster: List<LobbyHeroActor>): List<LobbyDrawableItem> {
    if (obstacles !== sourceProps) {
      sourceProps = obstacles
      props = obstacles.map { LobbyDrawableItem.Prop(it) }.sortedBy { it.sortY }
    }
    if (roster !== sourceActors) {
      sourceActors = roster
      actors.clear()
      roster.forEach { actors.add(LobbyDrawableItem.HeroNpc(it)) }
    }
    for (i in 1 until actors.size) {
      val item = actors[i]; var j = i-1
      while (j >= 0 && actors[j].sortY > item.sortY) { actors[j+1] = actors[j]; j-- }
      actors[j+1] = item
    }
    result.clear()
    var p = 0; var a = 0
    while (p < props.size || a < actors.size) {
      if (a == actors.size || (p < props.size && props[p].sortY <= actors[a].sortY)) result.add(props[p++])
      else result.add(actors[a++])
    }
    return result
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SanctuaryLobbyScreen(
  state: SummonerUiState,
  onOpenFacility: (FacilityType) -> Unit,
  onCloseFacility: () -> Unit,
  onSummonHero: () -> Unit,
  onTrainHero: (HeroRecord, Boolean) -> Unit,
  onEquipItem: (HeroRecord, EquipmentRecord) -> Unit,
  onSynthesizeGear: () -> Unit,
  onEnhanceGear: (EquipmentRecord) -> Unit,
  onRecoverHero: (HeroRecord, Boolean) -> Unit,
  onHostMeal: () -> Unit,
  onToggleSquad: (HeroRecord) -> Unit,
  onCompletePrologue: () -> Unit,
  onSignOut: () -> Unit,
  settings: GameSettings = GameSettings(),
  onOpenSettings: () -> Unit = {},
  onTrainHeroWithFocus: (HeroRecord, TrainingFocus) -> Unit = { h, f -> onTrainHero(h, f.isSkillUpgrade) },
  onUnequipItem: (HeroRecord, String) -> Unit = { _, _ -> },
  onAssignHeroActivity: (HeroRecord, HeroStatusType) -> Unit = { _, _ -> },
  onHostMealWithRecipe: (SanctuaryMealRecipe) -> Unit = { _ -> onHostMeal() },
  onUnlockExpansion: (IslandExpansionMilestone) -> Unit = {},
  sceneActive: Boolean = true,
) {
  val artContext = androidx.compose.ui.platform.LocalContext.current
  remember(artContext) { com.example.game.pixel.SanctuaryArtAssets.init(artContext) }
  remember(artContext) { com.example.game.world.WorldEnvironment.init(artContext) }
  val lobbyEngine = remember { SanctuaryLobbyEngine() }
  val depthQueue = remember { LobbyDrawQueue() }
  val textMeasurer = rememberTextMeasurer()
  val focusRequester = remember { FocusRequester() }

  var joyDx by remember { mutableFloatStateOf(0f) }
  var joyDy by remember { mutableFloatStateOf(0f) }
  var keyUp by remember { mutableStateOf(false) }
  var keyDown by remember { mutableStateOf(false) }
  var keyLeft by remember { mutableStateOf(false) }
  var keyRight by remember { mutableStateOf(false) }
  var zoom by remember { mutableFloatStateOf(settings.defaultZoom) }
  var animTimeSec by remember { mutableFloatStateOf(0f) }
  val fixedSimulation = remember { FixedSceneSimulation() }
  val performanceProbe = remember(settings.performanceOverlay, settings.performanceMode) {
    ScenePerformanceProbe(settings.performanceOverlay, settings.performanceMode.targetFps)
  }
  var performanceSnapshot by remember { mutableStateOf<ScenePerformanceSnapshot?>(null) }
  val drawStats = remember { intArrayOf(-1) }
  val hudClock = remember { floatArrayOf(0f) }
  var hudSelectedHeroId by remember { mutableStateOf<String?>(null) }
  var hudNowEpochSec by remember { mutableLongStateOf(System.currentTimeMillis() / 1000L) }
  var activeInteriorState by remember { mutableStateOf<FacilityType?>(null) }
  val insideInterior = activeInteriorState != null
  val insideRestaurantState = activeInteriorState == FacilityType.RESTAURANT
  var showQuickCommands by remember { mutableStateOf(false) }
  var managementMode by remember { mutableStateOf<String?>(null) }
  var promptedFacility by remember { mutableStateOf<FacilityType?>(null) }
  var inspectingHeroId by remember { mutableStateOf<String?>(null) }
  var showExpansionModal by remember { mutableStateOf(false) }
  var showPauseMapModal by remember { mutableStateOf(false) }
  var showChronicleModal by remember { mutableStateOf(false) }

  val profile = state.profile
  val unlockedExpansions = remember(profile?.storyFlags) {
    SanctuaryProgressionHelper.unlockedExpansions(profile?.storyFlags.orEmpty())
  }

  LaunchedEffect(unlockedExpansions) {
    lobbyEngine.syncExpansions(unlockedExpansions)
  }

  LaunchedEffect(settings.defaultZoom) {
    lobbyEngine.setZoomClamped(settings.defaultZoom)
    zoom = lobbyEngine.zoom
  }

  LaunchedEffect(state.heroes) {
    lobbyEngine.syncHeroes(state.heroes, System.currentTimeMillis() / 1000L)
  }

  BackHandler(
    enabled = (insideInterior || showPauseMapModal || showChronicleModal || showExpansionModal || inspectingHeroId != null || promptedFacility != null) &&
      state.activeFacilityModal == null
  ) {
    when {
      inspectingHeroId != null -> inspectingHeroId = null
      showExpansionModal -> showExpansionModal = false
      showPauseMapModal -> showPauseMapModal = false
      showChronicleModal -> showChronicleModal = false
      promptedFacility != null -> promptedFacility = null
      insideInterior -> {
        lobbyEngine.exitFacilityInterior()
        activeInteriorState = lobbyEngine.activeInterior
        zoom = lobbyEngine.zoom
      }
    }
  }

  val showPrologue = profile != null && !profile.storyFlags.contains(ManhwaStoryData.FLAG_PROLOGUE_SEEN)
  var prologueIndex by remember { mutableIntStateOf(0) }
  val feastActive = FeastBuffHelper.isFeastBuffActive(profile?.storyFlags.orEmpty())

  LaunchedEffect(Unit) { focusRequester.requestFocus() }
  SceneFrameLoop(
    active = sceneActive && state.activeFacilityModal == null && managementMode == null &&
      !showPauseMapModal && !showChronicleModal && !showExpansionModal && inspectingHeroId == null && !showPrologue,
    targetFps = settings.performanceMode.targetFps,
  ) { elapsed, nanos ->
    if (elapsed == 0f) {
      fixedSimulation.reset()
      performanceProbe.reset()
    }
    val started = if (settings.performanceOverlay) System.nanoTime() else 0L
    // A debugger/foreground stall never causes an unbounded catch-up burst. Normal 30/60 Hz
    // frames retain the same fixed 60 Hz simulation; background time is excluded by SceneFrameLoop.
    val dt = elapsed.coerceAtMost(0.25f)
    animTimeSec += dt
    val kDx = (if (keyRight) 1f else 0f) - (if (keyLeft) 1f else 0f)
    val kDy = (if (keyDown) 1f else 0f) - (if (keyUp) 1f else 0f)
    val dx = if (kotlin.math.abs(joyDx) > 0.05f) joyDx else kDx
    val dy = if (kotlin.math.abs(joyDy) > 0.05f) joyDy else kDy
    fixedSimulation.advance(dt) { step -> lobbyEngine.step(step, dx, dy) }
    activeInteriorState = lobbyEngine.activeInterior
    hudClock[0] += dt
    if (hudClock[0] >= 0.25f) {
      hudClock[0] %= 0.25f
      hudSelectedHeroId = lobbyEngine.selectedHeroId
      hudNowEpochSec = System.currentTimeMillis() / 1000L
    }
    if (settings.performanceOverlay) {
      performanceProbe.recordFrame(nanos, System.nanoTime() - started)
      // Cache traversal and immutable UI snapshots happen at most once per second.
      performanceProbe.poll(nanos)?.let { sample ->
        performanceSnapshot = sample.copy(
          activeHeroes = lobbyEngine.heroActors.size,
          activeMonsters = 0,
          visibleDrawables = drawStats[0],
          bitmapCacheBytes = com.example.game.pixel.SanctuaryArtAssets.cachedBitmapBytes,
        )
      }
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid)
      .focusRequester(focusRequester)
      .focusable()
      .onKeyEvent { event ->
        val down = event.type == KeyEventType.KeyDown
        when (event.key) {
          Key.W, Key.DirectionUp -> { keyUp = down; true }
          Key.S, Key.DirectionDown -> { keyDown = down; true }
          Key.A, Key.DirectionLeft -> { keyLeft = down; true }
          Key.D, Key.DirectionRight -> { keyRight = down; true }
          Key.E, Key.Enter -> {
            if (down) {
              if (lobbyEngine.isInsideInterior && lobbyEngine.isNearRestaurantExit) {
                lobbyEngine.exitFacilityInterior()
                activeInteriorState = lobbyEngine.activeInterior
                zoom = lobbyEngine.zoom
              } else {
                lobbyEngine.nearbyFacility?.let { fac ->
                  if (lobbyEngine.isInsideInterior) onOpenFacility(fac)
                  else promptedFacility = fac
                }
              }
            }
            true
          }
          else -> false
        }
      },
  ) {
    val isWideScreen = maxWidth >= 600.dp
    val isShortHeight = maxHeight < 460.dp
    val isNarrowScreen = maxWidth < 480.dp
    val fitOverviewZoom = minOf(
      constraints.maxWidth.toFloat() / 3380f,
      (constraints.maxHeight.toFloat() - 150f).coerceAtLeast(240f) / 2120f,
    ).coerceIn(0.27f, 0.54f)

    fun computeLobbyCam(viewportHalfW: Float, viewportHalfH: Float): Pair<Float, Float> {
      val layout = lobbyEngine.mapLayout
      val effectiveWorldW = if (!lobbyEngine.isInsideInterior && IslandExpansionMilestone.EAST_CELESTIAL_GARDEN in unlockedExpansions) {
        SanctuaryIslandGeometry.EXPANDED_WORLD_WIDTH
      } else {
        layout.width
      }
      val cx = ObliqueCamera.clampCamX(
        desiredCamX = lobbyEngine.camX,
        worldWidth = effectiveWorldW,
        wallMargin = layout.wallMargin,
        zoom = zoom,
        viewportHalfW = viewportHalfW,
      )
      val cy = ObliqueCamera.clampCamY(
        desiredCamY = lobbyEngine.camY,
        worldHeight = layout.height,
        wallMargin = layout.wallMargin,
        zoom = zoom,
        viewportHalfH = viewportHalfH,
        northHeadroomWorld = 220f,
      )
      return cx to cy
    }

    // 1. Interactive True Oblique 3/4 Pixel-Art Sanctuary & 4 Enterable 2.5D Facility Interiors Canvas
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .testTag("sanctuary_canvas")
        .pointerInput(Unit) {
          detectTransformGestures { centroid, pan, zoomChange, _ ->
            val halfW = size.width / 2f
            val halfH = size.height / 2f
            if (abs(zoomChange - 1f) > 0.003f) {
              val oldZoom = lobbyEngine.zoom
              val newZoom = (oldZoom * zoomChange).coerceIn(0.36f, 1.48f)
              if (abs(newZoom - oldZoom) > 0.0005f) {
                val anchorWx = ObliqueCamera.screenToWorldX(centroid.x, lobbyEngine.camX, oldZoom, halfW)
                val anchorWy = ObliqueCamera.screenToWorldY(centroid.y, lobbyEngine.camY, oldZoom, halfH)
                lobbyEngine.setZoomClamped(newZoom)
                zoom = lobbyEngine.zoom
                val anchoredCamX = anchorWx - (centroid.x - halfW) / zoom
                val anchoredCamY = anchorWy - (centroid.y - halfH) / (zoom * ObliqueCamera.OBLIQUE_Y_SCALE)
                lobbyEngine.setCameraPosition(anchoredCamX, anchoredCamY)
              }
            }
            if (abs(pan.x) > 0.1f || abs(pan.y) > 0.1f) {
              lobbyEngine.panCameraWorld(
                worldDx = -pan.x / zoom,
                worldDy = -pan.y / (zoom * ObliqueCamera.OBLIQUE_Y_SCALE),
              )
            }
          }
        }
        .pointerInput(zoom) {
          detectTapGestures { tapOffset ->
            val halfW = size.width / 2f
            val halfH = size.height / 2f
            val (camX, camY) = computeLobbyCam(halfW, halfH)
            val worldX = ObliqueCamera.screenToWorldX(tapOffset.x, camX, zoom, halfW)
            val worldY = ObliqueCamera.screenToWorldY(tapOffset.y, camY, zoom, halfH)

            if (lobbyEngine.isInsideInterior) {
              val activeInt = lobbyEngine.activeInterior ?: return@detectTapGestures
              val (exitX, exitY) = TowerFloorCatalog.interiorExitPoint(activeInt)
              if (hypot(worldX - exitX, worldY - exitY) <= 88f) {
                lobbyEngine.exitFacilityInterior()
                activeInteriorState = lobbyEngine.activeInterior
                zoom = lobbyEngine.zoom
                return@detectTapGestures
              }
              val tappedHero = lobbyEngine.selectHeroAt(worldX, worldY)
              if (tappedHero != null) {
                if (!tappedHero.isSanctuaryKeeperNpc) {
                  lobbyEngine.selectedHeroId = tappedHero.heroId
                  hudSelectedHeroId = tappedHero.heroId
                } else {
                  onOpenFacility(activeInt)
                }
                return@detectTapGestures
              }
              if (lobbyEngine.selectFacilityAt(worldX, worldY) != null) {
                onOpenFacility(activeInt)
                return@detectTapGestures
              }
              lobbyEngine.setTapDestination(worldX, worldY)
            } else {
              // 1. Contextual Facility Tap Priority (External Master Commander — no walking required!)
              val tappedFacility = lobbyEngine.selectFacilityAt(worldX, worldY)
              if (tappedFacility != null) {
                promptedFacility = tappedFacility
                return@detectTapGestures
              }

              // 2. Physical Island Expansion Beacon / Terrace Tap
              val tappedExpansion = lobbyEngine.selectExpansionAt(worldX, worldY)
              if (tappedExpansion != null) {
                promptedFacility = null
                showExpansionModal = true
                return@detectTapGestures
              }

              // 3. Hero or Keeper NPC Selection
              val tappedHero = lobbyEngine.selectHeroAt(worldX, worldY)
              if (tappedHero != null) {
                if (!tappedHero.isSanctuaryKeeperNpc) {
                  lobbyEngine.selectedHeroId = tappedHero.heroId
                  hudSelectedHeroId = tappedHero.heroId
                  promptedFacility = null
                } else {
                  promptedFacility = when (tappedHero.heroId) {
                    "npc_gatekeeper" -> FacilityType.TOWER_GATE
                    "npc_alchemist" -> FacilityType.SYNTHESIS_LAB
                    "npc_registry_officer" -> FacilityType.SUMMONING_HALL
                    "npc_trainer" -> FacilityType.TRAINING_GROUND
                    "npc_tavern_host" -> FacilityType.RESTAURANT
                    "npc_blacksmith" -> FacilityType.ARMORY
                    else -> FacilityType.TOWER_GATE
                  }
                }
                return@detectTapGestures
              }

              // 4. Empty Terrain Tap: Command selected hero or pan camera
              promptedFacility = null
              lobbyEngine.setTapDestination(worldX, worldY)
            }
          }
        },
    ) {
      // Animation state is read in draw only, so a world tick does not recompose the HUD.
      val drawStarted = if (settings.performanceOverlay) System.nanoTime() else 0L

      val halfW = size.width / 2f
      val halfH = size.height / 2f
      val (camX, camY) = computeLobbyCam(halfW, halfH)
      com.example.game.world.WorldEnvironment.update(indoors=lobbyEngine.isInsideInterior)
      val layout = lobbyEngine.mapLayout
      val highestClearedFloorInt = (profile?.highestClearedFloor ?: 0L).toInt()

      drawWorldBackground(
        biome = layout.biome,
        worldWidth = layout.width,
        worldHeight = layout.height,
        wallMargin = layout.wallMargin,
        camX = camX,
        camY = camY,
        zoom = zoom,
        timeSec = animTimeSec,
        patches = layout.patches,
        paths = layout.paths,
        unlockedExpansions = unlockedExpansions,
      )

      if (!lobbyEngine.isInsideInterior) {
        TowerFloorCatalog.SANCTUARY_ZONES.forEach { zone ->
          val inside = lobbyEngine.nearbyFacility == zone.facility || promptedFacility == zone.facility
          drawSanctuaryZoneFloorRing(zone, inside, camX, camY, zoom, animTimeSec)
        }
      }

      // Foreshortened 3/4 Elliptical Tap-to-Move Destination Rune Ring
      val selHeroActor = lobbyEngine.selectedHeroId?.let { id -> lobbyEngine.heroActors.find { it.heroId == id } }
      val tx = selHeroActor?.targetX
      val ty = selHeroActor?.targetY
      if (tx != null && ty != null) {
        val sx = ObliqueCamera.worldToScreenX(tx, camX, zoom, halfW)
        val sy = ObliqueCamera.worldToScreenY(ty, camY, zoom, halfH)
        val rx = 18f * zoom
        val ry = rx * ObliqueCamera.GROUND_ELLIPSE_RATIO
        drawOval(
          color = SummonerGold.copy(alpha = 0.78f),
          topLeft = Offset(sx - rx, sy - ry),
          size = Size(rx * 2f, ry * 2f),
          style = Stroke(width = 2.4f * zoom),
        )
      }

      if (!lobbyEngine.isInsideInterior) with(com.example.game.pixel.LivingWorldRenderer) { drawWeather(camX,camY,zoom,false) }
      // Y-Sorted 2.5D Architecture, Trees, Furnishings, and Autonomous 3/4 Anime Heroes
      val drawables = depthQueue.ordered(layout.obstacles,lobbyEngine.heroActors)
      var visibleDrawables = 0

      if (!lobbyEngine.isInsideInterior) {
        with(com.example.game.pixel.SanctuaryEnvironmentShadows) {
          drawGroundShadows(layout.obstacles, camX, camY, zoom)
        }
      }
      drawables.forEach { item ->
        // Share the existing renderer's generous margins; also skip offscreen canopy work.
        val visible = when (item) {
          is LobbyDrawableItem.Prop -> {
            val sx = ObliqueCamera.worldToScreenX(item.obs.x, camX, zoom, halfW)
            val sy = ObliqueCamera.worldToScreenY(item.obs.y, camY, zoom, halfH, item.obs.elevationZ)
            sx >= -380f && sx <= size.width + 380f && sy >= -400f && sy <= size.height + 380f
          }
          is LobbyDrawableItem.HeroNpc -> {
            val sx = ObliqueCamera.worldToScreenX(item.actor.x, camX, zoom, halfW)
            val sy = ObliqueCamera.worldToScreenY(item.actor.y, camY, zoom, halfH)
            sx >= -120f && sx <= size.width + 120f && sy >= -150f && sy <= size.height + 120f
          }
          is LobbyDrawableItem.Master -> false
        }
        if (!visible) return@forEach
        visibleDrawables++
        when (item) {
          is LobbyDrawableItem.Prop -> drawProp(
            obs = item.obs,
            camX = camX,
            camY = camY,
            zoom = zoom,
            timeSec = animTimeSec,
            highestClearedFloor = highestClearedFloorInt,
            renderShadow = if (lobbyEngine.isInsideInterior) true else null,
          )
          is LobbyDrawableItem.Master -> {} // External Commander is observing from outside world
          is LobbyDrawableItem.HeroNpc -> {
            val a = item.actor
            val isSelected = lobbyEngine.selectedHeroId == a.heroId
            drawCharacterSprite(
              worldX = a.x,
              worldY = a.y,
              camX = camX,
              camY = camY,
              zoom = zoom,
              isMaster = false,
              heroClass = a.heroClass,
              facingRight = a.facingRight,
              isMoving = a.isMoving,
              attackAnimProgress = a.attackPracticeAnim,
              hurtFlash = 0f,
              hpFraction = if (a.isInjured) 0.35f else 1f,
              shieldOrAuraColor = when {
                isSelected -> SummonerGold.copy(alpha = 0.90f)
                a.isSanctuaryKeeperNpc -> PortalCyan.copy(alpha = 0.45f)
                else -> null
              },
              isInjuredResting = a.isInjured || a.isSeatedDining,
              timeSec = animTimeSec,
              facingNorth = a.facingNorth,
              heroName = a.name,
              emotion = when {
                a.isInjured -> "fear"
                a.attackPracticeAnim > 0f || isSelected -> "alert"
                else -> "calm"
              },
            )
            drawActorCanopyOcclusion(layout.obstacles,a.x,a.y,camX,camY,zoom,animTimeSec)
          }
        }
      }

      // Foreground Southern Fortress Parapet Walls (layered above courtyard terrain & characters near rim)
      if (!lobbyEngine.isInsideInterior) {
        drawSouthernForegroundWalls(
          biome = layout.biome,
          camX = camX,
          camY = camY,
          zoom = zoom,
        )
      }

      if (!lobbyEngine.isInsideInterior) with(com.example.game.pixel.LivingWorldRenderer) {
        drawWorldGrade()
        drawLocalLights(layout.obstacles,camX,camY,zoom)
        drawWeather(camX,camY,zoom,true)
      }
      val topHudSafePx = (if (isWideScreen) 64.dp else 112.dp).toPx()

      val placedTagRects = mutableListOf<androidx.compose.ui.geometry.Rect>()

      fun drawLandmarkPlaque(wx: Float, wy: Float, elevationZ: Float, labelText: String, borderCol: Color) {
        val zx = ObliqueCamera.worldToScreenX(wx, camX, zoom, halfW)
        val zy = ObliqueCamera.worldToScreenY(wy, camY, zoom, halfH, elevationZ = elevationZ)
        if (zx !in -180f..(size.width + 180f) || zy !in (topHudSafePx + 14f)..(size.height - 18f)) return

        val fontSp = if (zoom < 0.60f) 8.5.sp else 10.5.sp
        val padH = if (zoom < 0.60f) 12f else 18f
        val padV = if (zoom < 0.60f) 4f else 6f
        val measured = textMeasurer.measure(
          text = labelText,
          style = TextStyle(
            color = Color(0xFFFEF08A),
            fontSize = fontSp,
            fontWeight = FontWeight.Bold,
          ),
        )
        val boxW = measured.size.width + padH
        val boxH = measured.size.height + padV
        val topLeft = Offset(zx - boxW / 2f, zy - boxH / 2f)
        val rect = androidx.compose.ui.geometry.Rect(topLeft, Size(boxW, boxH))
        if (placedTagRects.any { it.overlaps(rect) }) return
        placedTagRects += rect

        drawRoundRect(
          color = Color(0xDF172033),
          topLeft = topLeft,
          size = Size(boxW, boxH),
          cornerRadius = CornerRadius(8f, 8f),
        )
        drawRoundRect(
          color = borderCol.copy(alpha = 0.88f),
          topLeft = topLeft,
          size = Size(boxW, boxH),
          cornerRadius = CornerRadius(8f, 8f),
          style = Stroke(width = 1.4f),
        )
        drawText(measured, topLeft = Offset(zx - measured.size.width / 2f, zy - measured.size.height / 2f))
      }

      if (!lobbyEngine.isInsideInterior) {
        TowerFloorCatalog.SANCTUARY_ZONES.forEach { zone ->
          val (elev, labelText) = when (zone.facility) {
            FacilityType.TOWER_GATE -> 215f to "TOWER GATE"
            FacilityType.SUMMONING_HALL -> 165f to "SUMMONING HALL"
            FacilityType.RESTAURANT -> 142f to "DINING HALL"
            FacilityType.TRAINING_GROUND -> 86f to "TRAINING GROUNDS"
            FacilityType.HERO_QUARTERS -> 142f to "HERO DORMS"
            FacilityType.ARMORY -> 142f to "ARMORY"
            FacilityType.SYNTHESIS_LAB -> 146f to "SYNTHESIS LAB"
          }
          drawLandmarkPlaque(zone.centerX, zone.centerY - 20f, elev, labelText, Color(zone.colorArgb))
        }

        drawLandmarkPlaque(
          wx = TowerFloorCatalog.HERO_REGISTRY_X,
          wy = TowerFloorCatalog.HERO_REGISTRY_Y - 12f,
          elevationZ = 142f,
          labelText = "HERO REGISTRY",
          borderCol = SummonerGold,
        )

        val eastUnlocked = IslandExpansionMilestone.EAST_CELESTIAL_GARDEN in unlockedExpansions
        drawLandmarkPlaque(
          wx = if (eastUnlocked) SanctuaryIslandGeometry.EAST_TERRACE_CENTER_X else SanctuaryIslandGeometry.EAST_BEACON_X,
          wy = if (eastUnlocked) SanctuaryIslandGeometry.EAST_TERRACE_CENTER_Y - 40f else SanctuaryIslandGeometry.EAST_BEACON_Y,
          elevationZ = 92f,
          labelText = if (eastUnlocked) "✦ ĐÀI VƯỜN ĐÔNG" else "🔒 MỞ RỘNG ĐẢO ĐÔNG",
          borderCol = if (eastUnlocked) EmeraldVitality else PortalCyan,
        )

        val northUnlocked = IslandExpansionMilestone.NORTH_ASTRAL_DOCK in unlockedExpansions
        drawLandmarkPlaque(
          wx = if (northUnlocked) SanctuaryIslandGeometry.NORTH_DOCK_CENTER_X else SanctuaryIslandGeometry.NORTH_BEACON_X,
          wy = if (northUnlocked) SanctuaryIslandGeometry.NORTH_DOCK_CENTER_Y - 25f else SanctuaryIslandGeometry.NORTH_BEACON_Y,
          elevationZ = 92f,
          labelText = if (northUnlocked) "✦ BẾN PHI THUYỀN" else "🔒 MỞ RỘNG BẾN BẮC",
          borderCol = if (northUnlocked) EmeraldVitality else PortalCyan,
        )
      } else {
        val activeInt = lobbyEngine.activeInterior ?: FacilityType.RESTAURANT
        val (exitX, exitY) = TowerFloorCatalog.interiorExitPoint(activeInt)
        when (activeInt) {
          FacilityType.RESTAURANT ->
            drawLandmarkPlaque(620f, 255f, 118f, "★ QUẦY BẾP TRƯỞNG • ĐÃI TIỆC", SummonerGold)
          FacilityType.HERO_QUARTERS ->
            drawLandmarkPlaque(610f, 255f, 112f, "★ BÀN Y QUAN • HỒI PHỤC & LỊCH TRÌNH", EmeraldVitality)
          FacilityType.TRAINING_GROUND ->
            drawLandmarkPlaque(640f, 260f, 112f, "★ BỤC GIÁO ĐẦU • GIÁO ÁN CHUYÊN SÂU", AmberWarning)
          FacilityType.ARMORY ->
            drawLandmarkPlaque(600f, 265f, 112f, "★ ĐE RÈN TRƯỞNG • KHO VŨ KHÍ", PortalCyan)
          else -> Unit
        }
        drawLandmarkPlaque(
          wx = exitX,
          wy = exitY,
          elevationZ = 30f,
          labelText = "▾ LỐI RA THÁNH ĐỊA",
          borderCol = SummonerGold,
        )
      }

      // Crisp Under-Foot RPG Name Pills + Single Closest Speech Bubble (only at gameplay zoom >= 0.64f)
      val activeSpeakerId = if (zoom >= 0.64f) {
        lobbyEngine.heroActors
          .filter { it.speechBubble != null }
          .minByOrNull { hypot(it.x - lobbyEngine.masterX, it.y - lobbyEngine.masterY) }
          ?.takeIf { hypot(it.x - lobbyEngine.masterX, it.y - lobbyEngine.masterY) <= 420f }
          ?.heroId
      } else {
        null
      }

      lobbyEngine.heroActors.forEach { actor ->
        if (zoom < 0.62f && actor.isSanctuaryKeeperNpc) return@forEach
        val hx = ObliqueCamera.worldToScreenX(actor.x, camX, zoom, halfW)
        val footTagY = ObliqueCamera.worldToScreenY(actor.y, camY, zoom, halfH, elevationZ = -8f)
        if (hx in 24f..(size.width - 24f) && footTagY in (topHudSafePx + 8f)..(size.height - 22f)) {
          val tag = if (actor.isSanctuaryKeeperNpc) {
            actor.name.substringAfterLast(" ")
          } else {
            actor.name.substringBefore(" ").uppercase()
          }
          val nameLayout = textMeasurer.measure(
            text = tag,
            style = TextStyle(
              color = if (actor.isSanctuaryKeeperNpc) PortalCyan else ParchmentLight,
              fontSize = if (zoom < 0.62f) 8.5.sp else 9.5.sp,
              fontWeight = FontWeight.Bold,
            ),
          )
          val pillW = nameLayout.size.width + 12f
          val pillH = nameLayout.size.height + 3f
          val pillTopLeft = Offset(hx - pillW / 2f, footTagY)
          val candidateRect = androidx.compose.ui.geometry.Rect(pillTopLeft, Size(pillW, pillH))
          if (placedTagRects.none { it.overlaps(candidateRect) }) {
            placedTagRects += candidateRect
            drawRoundRect(
              color = Color(0xCC0D0A1A),
              topLeft = pillTopLeft,
              size = Size(pillW, pillH),
              cornerRadius = CornerRadius(6f, 6f),
            )
            drawRoundRect(
              color = (if (actor.isSanctuaryKeeperNpc) PortalCyan else SummonerGold).copy(alpha = 0.55f),
              topLeft = pillTopLeft,
              size = Size(pillW, pillH),
              cornerRadius = CornerRadius(6f, 6f),
              style = Stroke(width = 1f),
            )
            drawText(nameLayout, topLeft = Offset(hx - nameLayout.size.width / 2f, footTagY + 1.5f))
          }
        }
      }

      lobbyEngine.heroActors.firstOrNull { it.heroId == activeSpeakerId && it.speechBubble != null }?.let { actor ->
        val bubble = actor.speechBubble ?: return@let
        val hx = ObliqueCamera.worldToScreenX(actor.x, camX, zoom, halfW)
        val headY = ObliqueCamera.worldToScreenY(actor.y, camY, zoom, halfH, elevationZ = 84f)
        if (hx in 24f..(size.width - 24f) && headY in (topHudSafePx + 24f)..(size.height - 32f)) {
          val bLayout = textMeasurer.measure(
            text = bubble.take(42) + if (bubble.length > 42) "…" else "",
            style = TextStyle(color = SummonerGold, fontSize = 10.sp, fontWeight = FontWeight.Medium),
          )
          val bx = (hx - bLayout.size.width / 2f).coerceIn(12f, (size.width - bLayout.size.width - 18f).coerceAtLeast(12f))
          val by = (headY - bLayout.size.height - 6f).coerceAtLeast(topHudSafePx + 8f)
          val bubbleRect = androidx.compose.ui.geometry.Rect(Offset(bx - 7f, by - 4f), Size(bLayout.size.width + 14f, bLayout.size.height + 8f))
          if (placedTagRects.none { it.overlaps(bubbleRect) }) {
            placedTagRects += bubbleRect
            drawRoundRect(
              color = Color(0xEE141122),
              topLeft = Offset(bx - 7f, by - 4f),
              size = Size(bLayout.size.width + 14f, bLayout.size.height + 8f),
              cornerRadius = CornerRadius(8f, 8f),
            )
            drawRoundRect(
              color = SummonerGold.copy(alpha = 0.55f),
              topLeft = Offset(bx - 7f, by - 4f),
              size = Size(bLayout.size.width + 14f, bLayout.size.height + 8f),
              cornerRadius = CornerRadius(8f, 8f),
              style = Stroke(width = 1f),
            )
            drawText(bLayout, topLeft = Offset(bx, by))
          }
        }
      }

      drawForegroundAtmosphere(layout.biome, animTimeSec, settings.particlesEnabled)
      drawStats[0] = visibleDrawables
      if (settings.performanceOverlay) performanceProbe.recordDraw(System.nanoTime() - drawStarted)
    }

    if (settings.performanceOverlay) {
      ScenePerformanceOverlay(
        performanceSnapshot,
        Modifier.align(Alignment.CenterStart).padding(8.dp).testTag("sanctuary_performance_overlay"),
      )
    }

    // 2. Slim, Non-Intrusive Fantasy RPG Top HUD (7-9% height)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .testTag("sanctuary_top_hud"),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      val modeBadge = if (state.isGuestAccount) "Khách" else "Cloud"
      val locationTag = when (activeInteriorState) {
        FacilityType.RESTAURANT -> "Nhà Ăn"
        FacilityType.HERO_QUARTERS -> "Khu Nghỉ Ngơi"
        FacilityType.TRAINING_GROUND -> "Võ Đường"
        FacilityType.ARMORY -> "Kho Vũ Khí"
        else -> "Tầng ${profile?.highestClearedFloor ?: 0}"
      }

      com.example.ui.components.FantasyPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal=10.dp,vertical=2.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
          PixelHeroPortraitAvatar(heroClass=HeroClassType.MAGE,sizeDp=28.dp)
          Column(Modifier.weight(1f)) {
            Text("Lv.${profile?.masterLevel ?: 1} • ${profile?.masterName ?: "Master"}",color=SummonerGold,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelMedium)
            Text("Mục tiêu: chinh phục tầng ${(profile?.highestClearedFloor ?: 0)+1}",color=RuneSilver,style=MaterialTheme.typography.labelSmall,maxLines=1)
          }
          ResourcePillBadge("Vàng",profile?.gold ?: 0L,SummonerGold)
          if(!isNarrowScreen) ResourcePillBadge("Hồn",profile?.soulShards ?: 0L,SanctuaryUi.violetOnDark)
          IconButton({showChronicleModal=true},Modifier.size(48.dp).testTag("quick_notifications_button")) {Icon(Icons.Default.Notifications,"Nhật ký",tint=RuneSilver)}
          IconButton(onOpenSettings,Modifier.size(48.dp).testTag("open_settings_button")) {Icon(Icons.Default.Settings,"Cài đặt",tint=RuneSilver)}
          IconButton({showPauseMapModal=true},Modifier.size(48.dp).testTag("pause_game_button")) {Icon(Icons.Default.Pause,"Bản đồ",tint=SummonerGold)}
        }
      }

      if (insideInterior) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = AmberWarning,
          modifier = Modifier
            .align(Alignment.Start)
            .testTag("exit_interior_button")
            .clickable {
              lobbyEngine.exitFacilityInterior()
              activeInteriorState = lobbyEngine.activeInterior
              zoom = lobbyEngine.zoom
            },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = ObsidianVoid, modifier = Modifier.size(14.dp))
            Text(
              text = "← Ra Thánh Địa ($locationTag)",
              style = MaterialTheme.typography.labelSmall,
              color = ObsidianVoid,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
            )
          }
        }
      }

      if (state.bannerFeedback != null) {
        // P28D: real event banner on the Astral Reliquary notice frame.
        ESBannerNotice(
          text = state.bannerFeedback!!,
          modifier = Modifier.align(Alignment.CenterHorizontally),
        )
      }
    }

    // 3. External Commander Free Camera, Contextual Prompts & Hero Tactical Overlays
    Box(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()
        .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
      val cameraIconSize = if (isNarrowScreen) 30.dp else 38.dp
      val cameraIconInner = if (isNarrowScreen) 16.dp else 20.dp
      val maxPanelWidth = if (isNarrowScreen) 185.dp else 490.dp

      // Bottom-Left: Free Camera Quick Control Suite
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = FantasySkin.Panel.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, SummonerGold.copy(alpha = 0.60f)),
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(bottom=54.dp)
          .testTag("camera_controls_bar"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = if (isNarrowScreen) 3.dp else 6.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(if (isNarrowScreen) 2.dp else 4.dp),
        ) {
          IconButton(
            onClick = {
              lobbyEngine.zoomCamera(0.12f)
              zoom = lobbyEngine.zoom
            },
            modifier = Modifier.size(cameraIconSize).testTag("camera_zoom_in_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = "Phóng To", tint = SummonerGold, modifier = Modifier.size(cameraIconInner))
          }
          IconButton(
            onClick = {
              lobbyEngine.zoomCamera(-0.12f)
              zoom = lobbyEngine.zoom
            },
            modifier = Modifier.size(cameraIconSize).testTag("camera_zoom_out_button"),
          ) {
            Icon(Icons.Default.Remove, contentDescription = "Thu Nhỏ", tint = SummonerGold, modifier = Modifier.size(cameraIconInner))
          }
          IconButton(
            onClick = {
              if (!insideInterior && zoom > fitOverviewZoom + 0.08f) {
                // Frame Full Floating Sanctuary Island Overview!
                lobbyEngine.setCameraPosition(1600f, 1320f)
                lobbyEngine.setZoomClamped(fitOverviewZoom)
                zoom = lobbyEngine.zoom
              } else {
                lobbyEngine.setCameraPosition(1660f, 1260f)
                lobbyEngine.setZoomClamped(0.88f)
                zoom = lobbyEngine.zoom
              }
            },
            modifier = Modifier.size(cameraIconSize).testTag("camera_reset_button"),
          ) {
            Icon(Icons.Default.RestartAlt, contentDescription = "Toàn Cảnh / Căn Giữa Bản Đồ", tint = PortalCyan, modifier = Modifier.size(cameraIconInner))
          }
          IconButton(
            onClick = {
              val selId = lobbyEngine.selectedHeroId
              if (selId != null) {
                lobbyEngine.focusCameraOnHero(selId)
              } else {
                state.heroes.firstOrNull()?.let { lobbyEngine.focusCameraOnHero(it.id) }
              }
            },
            modifier = Modifier.size(cameraIconSize).testTag("camera_focus_button"),
          ) {
            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Theo Vết Anh Hùng", tint = ArcaneViolet, modifier = Modifier.size(cameraIconInner))
          }
          IconButton(
            onClick = {
              lobbyEngine.focusCameraOnFacility(FacilityType.TOWER_GATE)
            },
            modifier = Modifier.size(cameraIconSize).testTag("camera_gate_button"),
          ) {
            Icon(Icons.Default.Castle, contentDescription = "Đến Cổng Tháp", tint = EmeraldVitality, modifier = Modifier.size(cameraIconInner))
          }
        }
      }

      // Bottom-Right: Contextual Facility Prompt, Compact Hero Card & Quick Action Bar
      Column(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(bottom=54.dp)
          .padding(start = if (isNarrowScreen) 168.dp else 184.dp)
          .widthIn(max = maxPanelWidth)
          .testTag("hero_command_panel"),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        val selectedHeroRecord = state.heroes.find { it.id == hudSelectedHeroId }
        val activePrompt = promptedFacility

        if (activePrompt != null && !insideInterior) {
          ContextualFacilityPromptCard(
            facility = activePrompt,
            onEnterInterior = if (activePrompt in SanctuaryLobbyEngine.ENTERABLE_FACILITIES) {
              {
                promptedFacility = null
                lobbyEngine.enterFacilityInterior(activePrompt)
                activeInteriorState = lobbyEngine.activeInterior
                zoom = lobbyEngine.zoom
              }
            } else {
              null
            },
            onOpenManagement = {
              promptedFacility = null
              onOpenFacility(activePrompt)
            },
            onDismiss = { promptedFacility = null },
          )
        } else if (selectedHeroRecord != null) {
          SelectedHeroCompactCard(
            hero = selectedHeroRecord,
            equipment = state.equipment,
            nowEpochSec = hudNowEpochSec,
            isInsideInterior = insideInterior,
            onOpenFullProfile = { inspectingHeroId = selectedHeroRecord.id },
            onCommandHold = { lobbyEngine.commandHeroHold(selectedHeroRecord.id) },
            onCommandTask = { task -> lobbyEngine.commandHeroTask(selectedHeroRecord.id, task) },
            onDismiss = { lobbyEngine.selectedHeroId = null; hudSelectedHeroId = null },
          )
        }

        TextButton(onClick={showQuickCommands=!showQuickCommands},modifier=Modifier.testTag("quick_commands_toggle")) {
          Text(if(showQuickCommands) "Thu gọn lệnh ▴" else "Lệnh & công trình ▾",color=RuneSilver,style=MaterialTheme.typography.labelSmall)
        }
        // Secondary commands stay accessible without permanently covering the world.
        androidx.compose.animation.AnimatedVisibility(visible=showQuickCommands) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (insideRestaurantState) {
            Button(
              onClick = {
                lobbyEngine.exitRestaurantInterior()
                activeInteriorState = lobbyEngine.activeInterior
                zoom = lobbyEngine.zoom
              },
              modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag("exit_restaurant_interior_button"),
              contentPadding = PaddingValues(horizontal = if (isNarrowScreen) 8.dp else 10.dp),
              shape = RoundedCornerShape(10.dp),
              colors = FantasySkin.buttonColors(containerColor = AmberWarning, contentColor = ObsidianVoid),
            ) {
              Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Rời Quán", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }
          } else if (insideInterior) {
            Button(
              onClick = {
                lobbyEngine.exitFacilityInterior()
                activeInteriorState = lobbyEngine.activeInterior
                zoom = lobbyEngine.zoom
              },
              modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag("exit_active_interior_chip"),
              contentPadding = PaddingValues(horizontal = if (isNarrowScreen) 8.dp else 10.dp),
              shape = RoundedCornerShape(10.dp),
              colors = FantasySkin.buttonColors(containerColor = AmberWarning, contentColor = ObsidianVoid),
            ) {
              Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Rời Phòng", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }
          } else {
            Button(
              onClick = {
                promptedFacility = null
                lobbyEngine.enterRestaurantInterior()
                activeInteriorState = lobbyEngine.activeInterior
                zoom = lobbyEngine.zoom
              },
              modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag("enter_restaurant_interior_button"),
              contentPadding = PaddingValues(horizontal = if (isNarrowScreen) 8.dp else 10.dp),
              shape = RoundedCornerShape(10.dp),
              colors = FantasySkin.buttonColors(containerColor = SanctuaryStone.copy(alpha = 0.94f), contentColor = SummonerGold),
            ) {
              Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Nhà Ăn", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }

            Button(
              onClick = {
                promptedFacility = null
                showExpansionModal = true
              },
              modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag("open_expansion_modal_button"),
              contentPadding = PaddingValues(horizontal = if (isNarrowScreen) 8.dp else 10.dp),
              shape = RoundedCornerShape(10.dp),
              colors = FantasySkin.buttonColors(containerColor = SanctuaryStone.copy(alpha = 0.94f), contentColor = PortalCyan),
            ) {
              Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Mở Rộng", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }
          }

          state.heroes.take(5).forEach { hero ->
            val isSel = hero.id == lobbyEngine.selectedHeroId
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSel) SummonerGold else SanctuaryStone.copy(alpha = 0.90f),
              border = BorderStroke(1.dp, if (isSel) Color.White else BorderRune),
              modifier = Modifier
                .testTag("hero_chip_${hero.id}")
                .clickable {
                  promptedFacility = null
                  lobbyEngine.selectedHeroId = hero.id
                  hudSelectedHeroId = hero.id
                  lobbyEngine.focusCameraOnHero(hero.id)
                }
                .padding(2.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                PixelHeroPortraitAvatar(heroName = hero.name, heroClass = hero.classType, sizeDp = 22.dp)
                Text(
                  text = hero.name.substringBefore(" "),
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isSel) ObsidianVoid else ParchmentLight,
                  maxLines = 1,
                )
              }
            }
          }
        }

        }

        // Facility Proximity & Interaction Prompt Button
        val nearby = lobbyEngine.nearbyFacility
        Button(
          onClick = {
            val activeInt = activeInteriorState
            if (activeInt != null) {
              onOpenFacility(activeInt)
            } else {
              onOpenFacility(nearby ?: FacilityType.TOWER_GATE)
            }
          },
          modifier = Modifier
            .heightIn(min = 44.dp)
            .testTag("interact_facility_button"),
          shape = RoundedCornerShape(12.dp),
          colors = FantasySkin.buttonColors(
            containerColor = if (nearby != null || insideInterior) SummonerGold else SanctuaryStone.copy(alpha = 0.92f),
            contentColor = if (nearby != null || insideInterior) ObsidianVoid else ParchmentLight,
          ),
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = when (activeInteriorState) {
              FacilityType.RESTAURANT -> "Đãi Tiệc Bếp Trưởng"
              FacilityType.HERO_QUARTERS -> "Quản Lý Khu Nghỉ Ngơi"
              FacilityType.TRAINING_GROUND -> "Giáo Án Võ Đường"
              FacilityType.ARMORY -> "Quản Lý Kho Vũ Khí"
              else -> if (nearby != null) {
                "Vào: ${nearby.titleVi.substringAfter("• ").trim()}"
              } else {
                "Vào Tháp"
              }
            },
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    com.example.ui.components.SanctuaryManagementNav(
      modifier=Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal=10.dp,vertical=4.dp).widthIn(max=640.dp),
      onNavigate={ destination ->
        managementMode=null
        when(destination) {
          "heroes", "team" -> { onCloseFacility(); managementMode=destination }
          "summon" -> onOpenFacility(FacilityType.SUMMONING_HALL)
          "tower" -> onOpenFacility(FacilityType.TOWER_GATE)
          "inventory" -> onOpenFacility(FacilityType.ARMORY)
        }
      },
    )
    if(managementMode!=null) com.example.ui.components.HeroManagementDialog(
      heroes=state.heroes,teamMode=managementMode=="team",busy=state.isBusyAction,equipment=state.equipment,
      onClose={managementMode=null},onInspect={managementMode=null;inspectingHeroId=it.id},onToggle=onToggleSquad)

    // 4. Active Sanctuary Facility Modal
    val openFacility = state.activeFacilityModal
    if (openFacility != null) {
      SanctuaryFacilityModalOverlay(
        facility = openFacility,
        state = state,
        onClose = onCloseFacility,
        onSummonHero = onSummonHero,
        onTrainHero = onTrainHero,
        onEquipItem = onEquipItem,
        onSynthesizeGear = onSynthesizeGear,
        onEnhanceGear = onEnhanceGear,
        onRecoverHero = onRecoverHero,
        onHostMeal = onHostMeal,
        onToggleSquad = onToggleSquad,
        onEnterRestaurantInterior = if (!insideRestaurantState) {
          {
            onCloseFacility()
            lobbyEngine.enterRestaurantInterior()
            activeInteriorState = lobbyEngine.activeInterior
            zoom = lobbyEngine.zoom
          }
        } else {
          null
        },
        onTrainHeroWithFocus = onTrainHeroWithFocus,
        onUnequipItem = onUnequipItem,
        onAssignHeroActivity = onAssignHeroActivity,
        onHostMealWithRecipe = onHostMealWithRecipe,
        onEnterFacilityInterior = { targetFac ->
          onCloseFacility()
          if (lobbyEngine.activeInterior != targetFac) {
            lobbyEngine.enterFacilityInterior(targetFac)
            activeInteriorState = lobbyEngine.activeInterior
            zoom = lobbyEngine.zoom
          }
        },
        onOpenHeroProfile = { hero ->
          onCloseFacility()
          inspectingHeroId = hero.id
        },
        initialSelectedHeroId = lobbyEngine.selectedHeroId,
      )
    }

    // 4B. Full 5-Tab Hero Profile Inspection Modal
    val inspectingHero = inspectingHeroId?.let { id -> state.heroes.firstOrNull { it.id == id } }
    if (inspectingHero != null && openFacility == null) {
      HeroFullProfileModal(
        hero = inspectingHero,
        profile = profile ?: MasterProfile(),
        allHeroes = state.heroes,
        equipment = state.equipment,
        isBusy = state.isBusyAction,
        onClose = { inspectingHeroId = null },
        onToggleSquad = onToggleSquad,
        onTrainHeroWithFocus = onTrainHeroWithFocus,
        onEquipItem = onEquipItem,
        onUnequipItem = onUnequipItem,
        onAssignHeroActivity = onAssignHeroActivity,
        onRecoverHero = onRecoverHero,
      )
    }

    // 4C. Island Expansion Milestone Modal
    if (showExpansionModal && openFacility == null && inspectingHero == null) {
      IslandExpansionModal(
        profile = profile ?: MasterProfile(),
        isBusy = state.isBusyAction,
        onUnlockExpansion = onUnlockExpansion,
        onFocusExpansionOnMap = { milestone ->
          showExpansionModal = false
          if (insideInterior) {
            lobbyEngine.exitFacilityInterior()
            activeInteriorState = lobbyEngine.activeInterior
            zoom = lobbyEngine.zoom
          }
          when (milestone) {
            IslandExpansionMilestone.EAST_CELESTIAL_GARDEN ->
              lobbyEngine.setCameraPosition(SanctuaryIslandGeometry.EAST_TERRACE_CENTER_X, SanctuaryIslandGeometry.EAST_TERRACE_CENTER_Y)
            IslandExpansionMilestone.NORTH_ASTRAL_DOCK ->
              lobbyEngine.setCameraPosition(SanctuaryIslandGeometry.NORTH_DOCK_CENTER_X, SanctuaryIslandGeometry.NORTH_DOCK_CENTER_Y)
          }
        },
        onClose = { showExpansionModal = false },
      )
    }

    // 5. Active Pause & Sanctuary Overview Map Overlay
    if (showPauseMapModal) {
      SanctuaryOverviewMapOverlay(
        masterX = lobbyEngine.masterX,
        masterY = lobbyEngine.masterY,
        onFastTravel = { targetX, targetY ->
          if (insideInterior) {
            lobbyEngine.exitFacilityInterior()
            activeInteriorState = lobbyEngine.activeInterior
            zoom = lobbyEngine.zoom
          }
          lobbyEngine.camX = targetX
          lobbyEngine.camY = targetY
          lobbyEngine.clearTapDestination()
          showPauseMapModal = false
        },
        onClose = { showPauseMapModal = false },
        onOpenSettings = {
          showPauseMapModal = false
          onOpenSettings()
        },
        onSignOut = {
          showPauseMapModal = false
          onSignOut()
        },
      )
    }

    // 6. Active Sanctuary Chronicle & System Dispatch Overlay
    if (showChronicleModal) {
      SanctuaryChronicleOverlay(
        state = state,
        onClose = { showChronicleModal = false },
      )
    }

    // 7. Opening Manhwa Awakening Cutscene
    if (showPrologue && openFacility == null && !showPauseMapModal && !showExpansionModal && inspectingHero == null) {
      val beat = ManhwaStoryData.PROLOGUE_BEATS[prologueIndex.coerceIn(0, ManhwaStoryData.PROLOGUE_BEATS.lastIndex)]
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(ObsidianVoid.copy(alpha = 0.88f))
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(14.dp),
        contentAlignment = Alignment.Center,
      ) {
        if (isWideScreen && isShortHeight) {
          Row(
            modifier = Modifier.fillMaxWidth().widthIn(max = 820.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (beat.panelArtRes != null) {
              Image(
                painter = painterResource(id = beat.panelArtRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .weight(0.44f)
                  .height(210.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .border(1.5.dp, SummonerGold.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
              )
            }
            PrologueDialogueCard(
              beat = beat,
              prologueIndex = prologueIndex,
              totalBeats = ManhwaStoryData.PROLOGUE_BEATS.size,
              onSkip = onCompletePrologue,
              onNext = {
                if (prologueIndex < ManhwaStoryData.PROLOGUE_BEATS.lastIndex) prologueIndex++
                else onCompletePrologue()
              },
              modifier = Modifier.weight(0.56f),
            )
          }
        } else {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 620.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            if (beat.panelArtRes != null) {
              Image(
                painter = painterResource(id = beat.panelArtRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(175.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .border(1.5.dp, SummonerGold.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
              )
            }
            PrologueDialogueCard(
              beat = beat,
              prologueIndex = prologueIndex,
              totalBeats = ManhwaStoryData.PROLOGUE_BEATS.size,
              onSkip = onCompletePrologue,
              onNext = {
                if (prologueIndex < ManhwaStoryData.PROLOGUE_BEATS.lastIndex) prologueIndex++
                else onCompletePrologue()
              },
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SanctuaryOverviewMapOverlay(
  masterX: Float,
  masterY: Float,
  onFastTravel: (Float, Float) -> Unit,
  onClose: () -> Unit,
  onOpenSettings: () -> Unit,
  onSignOut: () -> Unit,
) {
  BackHandler(onBack = onClose)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid.copy(alpha = 0.88f))
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = FantasySkin.Panel,
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 760.dp)
        .heightIn(max = 580.dp)
        .border(1.5.dp, SummonerGold, RoundedCornerShape(18.dp)),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("TẠM DỪNG • BẢN ĐỒ ĐẢO NỔI THÁNH ĐỊA", style = MaterialTheme.typography.titleMedium, color = SummonerGold)
            Text("3200 × 2760px Pháo Đài Đảo Nổi • Chạm địa điểm để Dịch Chuyển Camera", style = MaterialTheme.typography.bodySmall, color = PortalCyan)
          }
          IconButton(onClick = onClose, modifier = Modifier.testTag("close_pause_map_button")) {
            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = ParchmentLight)
          }
        }

        // Miniature Schematic 2.5D Polygonal Floating Island Map Canvas
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF173654))
            .border(1.dp, BorderRune, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val mapW = size.width
            val mapH = size.height

            fun toMapX(wx: Float): Float = (wx / 3200f) * mapW
            fun toMapY(wy: Float): Float = (wy / 2760f) * mapH

            // 8-sided Floating Sanctuary Island Polygon
            val islandPath = androidx.compose.ui.graphics.Path().apply {
              com.example.game.map.SanctuaryIslandGeometry.POLYGON_VERTICES.forEachIndexed { idx, (vx, vy) ->
                if (idx == 0) moveTo(toMapX(vx), toMapY(vy))
                else lineTo(toMapX(vx), toMapY(vy))
              }
              close()
            }
            drawPath(islandPath, color = Color(0xFF3E6B38))
            drawPath(islandPath, color = Color(0xFFD8CEB8), style = Stroke(width = 3f))

            // Central-East Large Circular White Stone Plaza
            drawCircle(
              color = Color(0xFFEBEFF5).copy(alpha = 0.85f),
              radius = (555f / 3200f) * mapW,
              center = Offset(toMapX(1940f), toMapY(1240f)),
            )
            // SW Golden-Sand Training Arena
            drawOval(
              color = Color(0xFFE2BE82).copy(alpha = 0.85f),
              topLeft = Offset(toMapX(510f), toMapY(1560f)),
              size = Size((780f / 3200f) * mapW, (460f / 2760f) * mapH),
            )
            // SE Classical Stepped Amphitheatre
            drawCircle(
              color = Color(0xFFD5DBE5).copy(alpha = 0.85f),
              radius = (260f / 3200f) * mapW,
              center = Offset(toMapX(2430f), toMapY(2005f)),
            )

            // Cobblestone road spine
            drawLine(Color(0xFF9EA6B2), Offset(toMapX(1560f), toMapY(390f)), Offset(toMapX(1560f), toMapY(680f)), strokeWidth = 3f)
            drawLine(Color(0xFF9EA6B2), Offset(toMapX(1560f), toMapY(680f)), Offset(toMapX(1330f), toMapY(880f)), strokeWidth = 3f)
            drawLine(Color(0xFF9EA6B2), Offset(toMapX(1330f), toMapY(880f)), Offset(toMapX(1330f), toMapY(1340f)), strokeWidth = 3f)
            drawLine(Color(0xFF9EA6B2), Offset(toMapX(560f), toMapY(1340f)), Offset(toMapX(1330f), toMapY(1340f)), strokeWidth = 3f)
            drawLine(Color(0xFF9EA6B2), Offset(toMapX(1330f), toMapY(1340f)), Offset(toMapX(1610f), toMapY(2220f)), strokeWidth = 3f)

            // District Waypoint Rings
            val districtPoints = listOf(
              "Cổng Tháp" to (1960f to 635f),
              "Luyện Kim" to (1270f to 585f),
              "Khu Nhà Ở" to (735f to 875f),
              "Nhà Ăn" to (1085f to 935f),
              "Sảnh Hồ Sơ" to (785f to 1185f),
              "Bãi Cát Võ" to (890f to 1790f),
              "Quảng Trường" to (1940f to 1240f),
              "Khán Đài Triệu Hồi" to (2380f to 1930f),
            )

            districtPoints.forEach { (_, pt) ->
              val dx = toMapX(pt.first)
              val dy = toMapY(pt.second)
              drawCircle(Color(0xFF38BDF8).copy(alpha = 0.35f), radius = 14f, center = Offset(dx, dy))
              drawCircle(Color(0xFFF5B841), radius = 5.5f, center = Offset(dx, dy))
            }

            // Live Camera / Master focus dot with pulsing halo
            val px = toMapX(masterX)
            val py = toMapY(masterY)
            drawCircle(PortalCyan.copy(alpha = 0.45f), radius = 14f, center = Offset(px, py))
            drawCircle(PortalCyan, radius = 6f, center = Offset(px, py))
            drawCircle(Color.White, radius = 3f, center = Offset(px, py))
          }

          // Interactive Fast Travel District Buttons
          Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              FastTravelChip("NW: Khu Nhà Ở & Vũ Khí", 860f, 780f, onFastTravel)
              FastTravelChip("N: Luyện Kim & Cổng Vòm", 1360f, 595f, onFastTravel)
              FastTravelChip("N-NE: Cổng Tháp Đá Đen", 1960f, 635f, onFastTravel)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              FastTravelChip("W: Nhà Ăn & Hồ Sơ", 960f, 1060f, onFastTravel)
              FastTravelChip("★ Quảng Trường Đá Trắng", 1940f, 1240f, onFastTravel)
              FastTravelChip("E: Vườn Hoa Anh Đào", 2580f, 1240f, onFastTravel)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              FastTravelChip("SW: Bãi Cát Huấn Luyện", 890f, 1790f, onFastTravel)
              FastTravelChip("S: Cổng Nam & Cầu Treo", 1610f, 2180f, onFastTravel)
              FastTravelChip("SE: Khán Đài Triệu Hồi", 2380f, 1930f, onFastTravel)
            }
          }
        }

        // Bottom Controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onOpenSettings,
              colors = FantasySkin.buttonColors(containerColor = AstralSlate, contentColor = SummonerGold),
              modifier = Modifier.testTag("pause_settings_button"),
            ) {
              Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Cài Đặt")
            }
            Button(
              onClick = onSignOut,
              colors = FantasySkin.buttonColors(containerColor = CrimsonDark, contentColor = ParchmentLight),
              modifier = Modifier.testTag("pause_signout_button"),
            ) {
              Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Đăng Xuất")
            }
          }

          Button(
            onClick = onClose,
            colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
            modifier = Modifier.testTag("resume_game_button"),
          ) {
            Text("Tiếp Tục Leo Tháp ▸", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun FastTravelChip(
  label: String,
  targetX: Float,
  targetY: Float,
  onFastTravel: (Float, Float) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = FantasySkin.Panel.copy(alpha = 0.90f),
    modifier = Modifier
      .border(1.dp, PortalCyan.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
      .clickable { onFastTravel(targetX, targetY) },
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = ParchmentLight,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun SanctuaryChronicleOverlay(
  state: SummonerUiState,
  onClose: () -> Unit,
) {
  BackHandler(onBack = onClose)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid.copy(alpha = 0.88f))
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(14.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = FantasySkin.Panel,
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 680.dp)
        .heightIn(max = 520.dp)
        .border(1.5.dp, PortalCyan, RoundedCornerShape(18.dp)),
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = PortalCyan)
            Text("NHẬT KÝ & THÔNG BÁO THÁNH ĐỊA", style = MaterialTheme.typography.titleMedium, color = SummonerGold)
          }
          IconButton(onClick = onClose, modifier = Modifier.testTag("close_chronicle_button")) {
            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = ParchmentLight)
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ChronicleTile("Cổng Không Gian", "Cổng Tháp Vĩnh Hằng đang ổn định ở Tầng ${(state.profile?.highestClearedFloor ?: 0) + 1}. Sẵn sàng xuất chinh!", PortalCyan)
          ChronicleTile("Đại Tiệc Bếp Trưởng", "Bếp Trưởng Gustav tại Nhà Ăn luôn phục vụ súp nóng & thịt quay thảo mộc tăng +12% Công & +10% Máu.", SummonerGold)
          ChronicleTile("Tổ Hợp Huấn Luyện", "Sân Đấu Trường, Bãi Bắn Tiễn và Vòng Cổ Ngữ đã chuẩn bị đủ hình nộm rèn luyện cho các Anh hùng.", ArcaneViolet)
          ChronicleTile("Cập Nhật Đội Hình", "Hiện có ${state.heroes.count { it.inActiveSquad }} Anh hùng trong Đội Hình Xuất Chinh. Tổng lực lượng: ${state.heroes.size} Anh hùng.", EmeraldVitality)
        }

        Button(
          onClick = onClose,
          colors = FantasySkin.buttonColors(containerColor = PortalCyan, contentColor = ObsidianVoid),
          modifier = Modifier.align(Alignment.End),
        ) {
          Text("Đã Hiểu")
        }
      }
    }
  }
}

@Composable
private fun ChronicleTile(title: String, desc: String, accentCol: Color) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = Color(0xFF1B1632),
    modifier = Modifier.fillMaxWidth().border(1.dp, accentCol.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(title, style = MaterialTheme.typography.labelLarge, color = accentCol)
      Spacer(modifier = Modifier.height(2.dp))
      Text(desc, style = MaterialTheme.typography.bodySmall, color = ParchmentLight)
    }
  }
}

@Composable
private fun PrologueDialogueCard(
  beat: com.example.game.story.DialogueBeat,
  prologueIndex: Int,
  totalBeats: Int,
  onSkip: () -> Unit,
  onNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = FantasySkin.Panel,
    modifier = modifier.border(1.5.dp, ArcaneViolet, RoundedCornerShape(16.dp)),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PixelHeroPortraitAvatar(heroClass = beat.heroClassForPortrait, sizeDp = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${beat.speakerName} • ${beat.emotionTag}",
            style = MaterialTheme.typography.titleSmall,
            color = SummonerGold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = beat.speakerTitle,
            style = MaterialTheme.typography.labelSmall,
            color = PortalCyan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = beat.lineVi,
        style = MaterialTheme.typography.bodyMedium,
        color = ParchmentLight,
      )
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${prologueIndex + 1} / $totalBeats",
          style = MaterialTheme.typography.labelSmall,
          color = RuneSilver,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onSkip,
            colors = FantasySkin.buttonColors(containerColor = EmeraldVitality.copy(alpha = 0.22f), contentColor = ParchmentLight),
            modifier = Modifier.heightIn(min = 48.dp).testTag("skip_prologue_button"),
          ) {
            Text("Bỏ Qua", maxLines = 1)
          }
          Button(
            onClick = onNext,
            colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
            modifier = Modifier.heightIn(min = 48.dp).testTag("next_prologue_button"),
          ) {
            Text(if (prologueIndex < totalBeats - 1) "Tiếp Tục ▸" else "Bước Vào Sảnh", maxLines = 1)
          }
        }
      }
    }
  }
}

