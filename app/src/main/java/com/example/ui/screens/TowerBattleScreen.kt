package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Bolt
import com.example.ui.components.FantasyButton as Button
import com.example.ui.components.FantasySkin
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.layout.onSizeChanged
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
import com.example.data.firebase.ExpeditionOutcomeType
import com.example.data.model.TacticalOrder
import com.example.game.combat.DiscoveryNodeKind
import com.example.game.combat.EnemyCombatUnit
import com.example.game.combat.HeroCombatUnit
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.map.ObliqueCamera
import com.example.game.map.PropKind
import com.example.game.map.WorldObstacle
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawEnemySprite
import com.example.game.pixel.PixelSpriteRenderer.drawForegroundAtmosphere
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.PixelSpriteRenderer.drawActorCanopyOcclusion
import com.example.game.pixel.PixelSpriteRenderer.drawWorldBackground
import com.example.game.story.ManhwaStoryData
import com.example.ui.SummonerUiState
import com.example.ui.components.CameraZoomControls
import com.example.ui.components.PixelHeroPortraitAvatar
import com.example.ui.components.VirtualTouchJoystick
import com.example.ui.theme.ArcaneViolet
import com.example.ui.theme.BorderRune
import com.example.ui.theme.CrimsonBlood
import com.example.ui.theme.EmeraldVitality
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.RuneSilver
import com.example.ui.theme.SanctuaryStone
import com.example.ui.theme.SummonerGold
import com.example.ui.theme.WarningAmber
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private sealed interface BattleDrawable {
  val sortY: Float

  data class Prop(val obs: WorldObstacle) : BattleDrawable {
    override val sortY: Float get() = obs.y + obs.sortYBias
  }

  data class Master(
    val x: Float,
    val y: Float,
    val right: Boolean,
    val moving: Boolean,
    val hpFrac: Float,
  ) : BattleDrawable {
    override val sortY: Float get() = y
  }

  data class Hero(val unit: HeroCombatUnit) : BattleDrawable {
    override val sortY: Float get() = unit.y
  }

  data class Courier(val mission: com.example.game.combat.LostCourierEncounter) : BattleDrawable {
    override val sortY: Float get() = mission.courier.y
  }

  data class Enemy(val unit: EnemyCombatUnit) : BattleDrawable {
    override val sortY: Float get() = unit.y
  }
}

/** Cache wrappers/static ordering; merge a small in-place sorted moving set without per-frame list copies. */
private class BattleDrawQueue {
  private var sourceProps: List<WorldObstacle>? = null
  private var chestOpened = false
  private var props: List<BattleDrawable.Prop> = emptyList()
  private val heroWrappers = HashMap<String,BattleDrawable.Hero>()
  private val enemyWrappers = HashMap<String,BattleDrawable.Enemy>()
  private val moving = ArrayList<BattleDrawable>()
  private val result = ArrayList<BattleDrawable>()
  fun ordered(engine:TowerCombatEngine, extra:BattleDrawable? = null):List<BattleDrawable> {
    val obstacles=engine.mapLayout.obstacles
    val opened=engine.isDiscoveryClaimed("f1_discovery_chest_north")
    if(sourceProps!==obstacles || opened!=chestOpened) {
      sourceProps=obstacles;chestOpened=opened
      props=obstacles.map { obs -> BattleDrawable.Prop(
        if(opened && obs.id=="f1_discovery_chest_north") obs.copy(propKind=PropKind.TREASURE_CHEST_OPEN) else obs)
      }.sortedBy { it.sortY }
    }
    moving.clear()
    engine.heroes.forEach { if(!it.knockedOut) moving.add(heroWrappers.getOrPut(it.id){ BattleDrawable.Hero(it) }) }
    engine.enemies.forEach { if(!it.dead || it.deathAnimProgress<1f) moving.add(enemyWrappers.getOrPut(it.id){ BattleDrawable.Enemy(it) }) }
    if(extra!=null) moving.add(extra)
    for(i in 1 until moving.size) {
      val item=moving[i];var j=i-1
      while(j>=0 && moving[j].sortY>item.sortY) { moving[j+1]=moving[j];j-- }
      moving[j+1]=item
    }
    result.clear();var p=0;var a=0
    while(p<props.size || a<moving.size) {
      if(a==moving.size || (p<props.size && props[p].sortY<=moving[a].sortY)) result.add(props[p++])
      else result.add(moving[a++])
    }
    return result
  }
}

/** Confirmation issues a real order; the engine settles only after extraction. */
internal fun issueRetreatFromConfirmation(engine: TowerCombatEngine) {
  engine.issueTacticalOrder(TacticalOrder.RETREAT)
}

/** A brief actionable message from a hero; the engine owns the fear state and response. */
@Composable
fun HeroAgencyAlertCard(
  heroName: String,
  message: String,
  onRegroup: () -> Unit,
  onProtect: () -> Unit,
  onRetreat: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.testTag("hero_agency_alert").border(1.dp, Color(0xFFC9BEA2), RoundedCornerShape(8.dp)),
    shape = RoundedCornerShape(8.dp), color = Color(0xEE141519),
  ) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
      Text(heroName, color = Color(0xFFF1DCAD), style = MaterialTheme.typography.labelSmall,
        maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(message, color = Color(0xFFF4F0E4), style = MaterialTheme.typography.labelSmall,
        maxLines = 2, overflow = TextOverflow.Ellipsis)
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        TextButton(onClick = onRegroup, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("hero_alert_regroup"),
          contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)) { Text("Tập hợp", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        TextButton(onClick = onProtect, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("hero_alert_protect"),
          contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)) { Text("Bảo vệ", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        TextButton(onClick = onRetreat, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("hero_alert_retreat"),
          contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)) { Text("Rút lui", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("hero_alert_dismiss"),
          contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)) { Text("Đóng", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
      }
    }
  }
}

@Composable
fun TowerBattleScreen(
  state: SummonerUiState,
  onFinishExpedition: (Boolean, Set<String>) -> Unit,
  onReturnToSanctuary: () -> Unit,
  onPrepareRetry: (() -> Unit)? = null,
  settings: GameSettings = GameSettings(),
  sceneActive: Boolean = true,
  onFinishExpeditionDetailed: ((Boolean, Set<String>, Boolean, Long, Long, Set<String>, com.example.data.model.CourierOutcome?, String?) -> Unit)? = null,
) {
  val nowSec = remember { System.currentTimeMillis() / 1000L }
  val feastBuffActive = remember(state.profile?.storyFlags) {
    FeastBuffHelper.isFeastBuffActive(state.profile?.storyFlags.orEmpty(), nowSec)
  }
  val activeMealRecipe = remember(state.profile?.storyFlags) {
    FeastBuffHelper.activeRecipe(state.profile?.storyFlags.orEmpty(), nowSec)
  }
  val activeSquadRecords = remember(state.selectedFloor.floorNumber) {
    state.heroes.filter { it.inActiveSquad && !it.isCurrentlyInjured(nowSec) }.ifEmpty {
      state.heroes.take(3)
    }
  }
  val combatEngine = remember(state.selectedFloor.floorNumber) {
    TowerCombatEngine(
      floorSpec = state.selectedFloor,
      squadRecords = activeSquadRecords,
      equipment = state.equipment,
      feastBuffActive = feastBuffActive,
      activeMealRecipe = activeMealRecipe,
      enableLostCourier = state.selectedFloor.floorNumber == 1,
      enableRescueJourney = state.selectedFloor.floorNumber == 1,
    )
  }

  val courierDrawable = remember(combatEngine) { combatEngine.lostCourier?.let { BattleDrawable.Courier(it) } }
  val depthQueue = remember(combatEngine) { BattleDrawQueue() }

  val introBeats = remember(state.selectedFloor.floorNumber) {
    if(state.selectedFloor.floorNumber==1) listOf(
      com.example.game.story.DialogueBeat("Người đưa tin", "Người đưa tin thất lạc", "Bị thương", com.example.data.model.HeroClassType.PRIEST,
        "Cổng đã bị khóa. Hồ sơ ở trạm bắc sẽ chỉ đường tìm ấn mở cổng… Xin Master đưa đội đi cùng tôi!"),
      com.example.game.story.DialogueBeat("Kaelen", "Khiên thép tiền phong", "Cảnh giác", com.example.data.model.HeroClassType.KNIGHT,
        "Hộ tống người đưa tin tới từng dấu hiệu, dọn hiểm họa rồi Khảo sát. Tại trạm tín hiệu, Master quyết định cách mở đường rút.")
    ) else ManhwaStoryData.floorIntroBeats(state.selectedFloor.floorNumber)
  }
  var introIndex by remember { mutableIntStateOf(0) }
  var showRetreatConfirm by remember { mutableStateOf(false) }
  var settlementDispatched by remember { mutableStateOf(false) }
  var introDismissed by remember(combatEngine) { mutableStateOf(false) }

  val textMeasurer = rememberTextMeasurer()
  val focusRequester = remember { FocusRequester() }

  var joyDx by remember { mutableFloatStateOf(0f) }
  var joyDy by remember { mutableFloatStateOf(0f) }
  var keyUp by remember { mutableStateOf(false) }
  var keyDown by remember { mutableStateOf(false) }
  var keyLeft by remember { mutableStateOf(false) }
  var keyRight by remember { mutableStateOf(false) }
  var zoom by remember { mutableFloatStateOf(settings.defaultZoom) }
  var camOffsetX by remember { mutableFloatStateOf(0f) }
  var camOffsetY by remember { mutableFloatStateOf(0f) }
  var frameTick by remember { mutableIntStateOf(0) }
  var animTimeSec by remember { mutableFloatStateOf(0f) }
  var hudRevision by remember { mutableIntStateOf(0) }
  var topHudHeightPx by remember { mutableIntStateOf(0) }
  val hudElapsed = remember { floatArrayOf(0f) }
  val visibleDrawables = remember { intArrayOf(0) }
  val lastProbeSample = remember { longArrayOf(0L) }
  val fixedSimulation = remember(combatEngine) { com.example.performance.FixedSceneSimulation() }
  val performanceProbe = remember(settings.performanceOverlay,settings.performanceMode) {
    com.example.performance.ScenePerformanceProbe(settings.performanceOverlay,settings.performanceMode.targetFps)
  }
  var performanceSnapshot by remember { mutableStateOf<com.example.performance.ScenePerformanceSnapshot?>(null) }


  LaunchedEffect(settings.defaultZoom) {
    zoom = settings.defaultZoom
  }

  BackHandler {
    if (combatEngine.stage == MissionStage.VICTORY || combatEngine.stage == MissionStage.DEFEAT) {
      if (!state.isBusyAction && state.lastSettlement != null) onReturnToSanctuary()
    } else {
      showRetreatConfirm = !showRetreatConfirm
    }
  }

  // Terminal states reached from UI commands must settle even though their frame loop is paused.
  LaunchedEffect(combatEngine.stage) {
    if(!settlementDispatched && (combatEngine.stage==MissionStage.VICTORY || combatEngine.stage==MissionStage.DEFEAT)) {
      settlementDispatched=true
      if(onFinishExpeditionDetailed!=null) onFinishExpeditionDetailed(
        combatEngine.stage==MissionStage.VICTORY,combatEngine.fallenHeroIds(),combatEngine.retreatCompleted,
        combatEngine.bonusDiscoveryGold,combatEngine.bonusDiscoveryMana,combatEngine.discoveredNodeIds(),
        combatEngine.lostCourier?.outcome,combatEngine.lostCourier?.frightenedHeroId)
      else onFinishExpedition(combatEngine.stage==MissionStage.VICTORY,combatEngine.fallenHeroIds())
    }
  }
  LaunchedEffect(combatEngine) { focusRequester.requestFocus() }
  com.example.performance.SceneFrameLoop(
    // Engine stage is plain simulation state. Explicit Compose state must restart/cancel
    // this effect outside the independently recomposing HUD subcomposition.
    active = sceneActive && !showRetreatConfirm && introDismissed && !settlementDispatched,
    targetFps = settings.performanceMode.targetFps,
  ) { elapsed, nanos ->
    if (elapsed == 0f) { performanceProbe.reset(); fixedSimulation.reset(); lastProbeSample[0] = nanos }
    val dt = elapsed.coerceIn(0f,.10f) // Bound catch-up after a foreground stall; background time is excluded.
    val started = if(settings.performanceOverlay) System.nanoTime() else 0L
    animTimeSec += dt
    val kDx = (if(keyRight) 1f else 0f) - (if(keyLeft) 1f else 0f)
    val kDy = (if(keyDown) 1f else 0f) - (if(keyUp) 1f else 0f)
    val dx = if(abs(joyDx)>.05f) joyDx else kDx
    val dy = if(abs(joyDy)>.05f) joyDy else kDy
    if(abs(dx)>.05f || abs(dy)>.05f) {
      val decay = Math.pow(.88, (dt*60f).toDouble()).toFloat()
      camOffsetX *= decay;camOffsetY *= decay
    }
    fixedSimulation.advance(dt) { step -> combatEngine.step(step,dx,dy) }
    frameTick++
    hudElapsed[0] += dt
    val finished = combatEngine.stage == MissionStage.VICTORY || combatEngine.stage == MissionStage.DEFEAT
    if (hudElapsed[0]>=.25f || finished) { hudElapsed[0]=0f;hudRevision++ }
    if(!settlementDispatched && finished) {
      settlementDispatched = true
      if(onFinishExpeditionDetailed!=null) onFinishExpeditionDetailed(
        combatEngine.stage==MissionStage.VICTORY,combatEngine.fallenHeroIds(),combatEngine.retreatCompleted,
        combatEngine.bonusDiscoveryGold,combatEngine.bonusDiscoveryMana,combatEngine.discoveredNodeIds(),
        combatEngine.lostCourier?.outcome,combatEngine.lostCourier?.frightenedHeroId)
      else onFinishExpedition(combatEngine.stage==MissionStage.VICTORY,combatEngine.fallenHeroIds())
    }
    if(settings.performanceOverlay) {
      performanceProbe.recordFrame(nanos,System.nanoTime()-started)
      if(nanos-lastProbeSample[0]>=1_000_000_000L) {
        performanceProbe.poll(nanos,combatEngine.heroes.count{!it.knockedOut},combatEngine.enemies.count{!it.dead},
          visibleDrawables[0],com.example.game.pixel.SanctuaryArtAssets.cachedBitmapBytes)?.let { performanceSnapshot=it }
        lastProbeSample[0]=nanos
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
          Key.One -> { if (down) combatEngine.issueTacticalOrder(TacticalOrder.FOLLOW); true }
          Key.Two -> { if (down) combatEngine.issueTacticalOrder(TacticalOrder.HOLD_POSITION); true }
          Key.Three -> { if (down) combatEngine.issueTacticalOrder(TacticalOrder.ATTACK_TARGET); true }
          Key.Four -> { if (down) combatEngine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE); true }
          Key.Five -> { if (down) combatEngine.issueTacticalOrder(TacticalOrder.PROTECT_ALLY); true }
          Key.Six -> { if (down) combatEngine.issueTacticalOrder(TacticalOrder.REGROUP); true }
          Key.Seven -> { if (down) combatEngine.issueTacticalOrder(TacticalOrder.RETREAT); true }
          Key.Eight -> { if (down) combatEngine.issueTacticalOrder(TacticalOrder.MOVE_TO); true }
          else -> false
        }
      },
  ) {
    val isWideScreen = maxWidth >= 640.dp
    val isShortHeight = maxHeight < 460.dp

    fun computeBattleCam(viewportHalfW: Float, viewportHalfH: Float): Pair<Float, Float> {
      val layout = combatEngine.mapLayout
      var focusX = combatEngine.masterX
      var focusY = combatEngine.masterY
      combatEngine.lostCourier?.let { mission ->
        // The external command sigil can remain behind while Protect escorts the NPC.
        // Keep that actual moving party visible, retaining the same projection/pan/zoom.
        var squadX = 0f
        var squadY = 0f
        var living = 0
        for (hero in combatEngine.heroes) if (!hero.knockedOut) {
          squadX += hero.x; squadY += hero.y; living++
        }
        focusX = if (living > 0) mission.courier.x * .4f + squadX / living * .6f else mission.courier.x
        focusY = if (living > 0) mission.courier.y * .4f + squadY / living * .6f else mission.courier.y
      }
      val cx = ObliqueCamera.clampCamX(
        desiredCamX = focusX + camOffsetX,
        worldWidth = layout.width,
        wallMargin = layout.wallMargin,
        zoom = zoom,
        viewportHalfW = viewportHalfW,
      )
      val cy = ObliqueCamera.clampCamY(
        desiredCamY = focusY - 45f + camOffsetY,
        worldHeight = layout.height,
        wallMargin = layout.wallMargin,
        zoom = zoom,
        viewportHalfH = viewportHalfH,
        northHeadroomWorld = 55f,
      )
      return cx to cy
    }

    // 1. Real-Time Multi-Layered True Oblique 3/4 Pixel-Art Battlefield Canvas
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .testTag("tower_battle_canvas")
        .pointerInput(Unit) {
          detectTransformGestures { _, pan, zoomChange, _ ->
            if (abs(zoomChange - 1f) > 0.003f) {
              zoom = (zoom * zoomChange).coerceIn(0.45f, 1.45f)
            }
            if (abs(pan.x) > 0.2f || abs(pan.y) > 0.2f) {
              camOffsetX = (camOffsetX - pan.x / zoom).coerceIn(-850f, 850f)
              camOffsetY = (camOffsetY - pan.y / (zoom * ObliqueCamera.OBLIQUE_Y_SCALE)).coerceIn(-550f, 550f)
            }
          }
        }
        .pointerInput(zoom) {
          detectTapGestures { tapOffset ->
            val halfW = size.width / 2f
            val halfH = size.height / 2f
            val (camX, camY) = computeBattleCam(halfW, halfH)
            val worldX = ObliqueCamera.screenToWorldX(tapOffset.x, camX, zoom, halfW)
            val worldY = ObliqueCamera.screenToWorldY(tapOffset.y, camY, zoom, halfH)
            combatEngine.onBattlefieldTapped(worldX, worldY)
          }
        },
    ) {
      val tickRead = frameTick
      val drawStarted = if(settings.performanceOverlay) System.nanoTime() else 0L
      visibleDrawables[0] = 0
      if (tickRead < 0) return@Canvas

      val halfW = size.width / 2f
      val halfH = size.height / 2f
      val (camX, camY) = computeBattleCam(halfW, halfH)
      val layout = combatEngine.mapLayout

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
      )

      // Foreshortened 3/4 Elliptical Ground AoE Spells, Shockwaves & Heal Rings
      combatEngine.aoeEffects.forEach { fx ->
        val progress = (fx.elapsed / fx.duration).coerceIn(0f, 1f)
        val sx = ObliqueCamera.worldToScreenX(fx.x, camX, zoom, halfW)
        val sy = ObliqueCamera.worldToScreenY(fx.y, camY, zoom, halfH)
        val rx = fx.maxRadius * (0.35f + 0.65f * progress) * zoom
        val ry = rx * ObliqueCamera.GROUND_ELLIPSE_RATIO
        drawOval(
          color = fx.color.copy(alpha = (1f - progress) * 0.35f),
          topLeft = Offset(sx - rx, sy - ry),
          size = Size(rx * 2f, ry * 2f),
        )
        drawOval(
          color = fx.color.copy(alpha = (1f - progress) * 0.9f),
          topLeft = Offset(sx - rx, sy - ry),
          size = Size(rx * 2f, ry * 2f),
          style = Stroke(width = 3f * zoom),
        )
      }

      // Foreshortened 3/4 Tactical Move-To Flag Beacon
      combatEngine.moveBeacon?.let { (bxWorld, byWorld) ->
        val bx = ObliqueCamera.worldToScreenX(bxWorld, camX, zoom, halfW)
        val by = ObliqueCamera.worldToScreenY(byWorld, camY, zoom, halfH)
        val rx = 24f * zoom
        val ry = rx * ObliqueCamera.GROUND_ELLIPSE_RATIO
        drawOval(
          color = SummonerGold.copy(alpha = 0.58f),
          topLeft = Offset(bx - rx, by - ry),
          size = Size(rx * 2f, ry * 2f),
          style = Stroke(width = 2.5f * zoom),
        )
        drawLine(
          color = SummonerGold,
          start = Offset(bx, by),
          end = Offset(bx, by - 38f * zoom),
          strokeWidth = 3f * zoom,
        )
        drawRect(
          color = SummonerGold,
          topLeft = Offset(bx, by - 38f * zoom),
          size = Size(20f * zoom, 12f * zoom),
        )
      }

      // External Master Commander Tactical Ground Sigil (No human Master sprite on the battlefield!)
      run {
        val sx = ObliqueCamera.worldToScreenX(combatEngine.masterX, camX, zoom, halfW)
        val sy = ObliqueCamera.worldToScreenY(combatEngine.masterY, camY, zoom, halfH)
        val pulse = 0.92f + 0.08f * sin(animTimeSec * 3.6f)
        val outerRx = 34f * zoom * pulse
        val outerRy = outerRx * ObliqueCamera.GROUND_ELLIPSE_RATIO
        val innerRx = 20f * zoom
        val innerRy = innerRx * ObliqueCamera.GROUND_ELLIPSE_RATIO
        drawOval(
          color = PortalCyan.copy(alpha = 0.18f),
          topLeft = Offset(sx - outerRx, sy - outerRy),
          size = Size(outerRx * 2f, outerRy * 2f),
        )
        drawOval(
          color = PortalCyan.copy(alpha = 0.82f),
          topLeft = Offset(sx - outerRx, sy - outerRy),
          size = Size(outerRx * 2f, outerRy * 2f),
          style = Stroke(width = 2.2f * zoom),
        )
        drawOval(
          color = SummonerGold.copy(alpha = 0.88f),
          topLeft = Offset(sx - innerRx, sy - innerRy),
          size = Size(innerRx * 2f, innerRy * 2f),
          style = Stroke(width = 1.8f * zoom),
        )
        for (i in 0 until 4) {
          val ang = animTimeSec * 1.4f + i * (Math.PI.toFloat() / 2f)
          val x1 = sx + cos(ang) * innerRx * 0.7f
          val y1 = sy + sin(ang) * innerRy * 0.7f
          val x2 = sx + cos(ang) * outerRx * 1.06f
          val y2 = sy + sin(ang) * outerRy * 1.06f
          drawLine(
            color = SummonerGold.copy(alpha = 0.85f),
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = 2f * zoom,
          )
        }
      }

      // Y-Sorted 3/4 Props + Autonomous Heroes + Living Monsters
      val drawables = depthQueue.ordered(combatEngine,courierDrawable)

      with(com.example.game.pixel.SanctuaryEnvironmentShadows) {
        drawGroundShadows(layout.obstacles,camX,camY,zoom,clipToIsland=false,
          groundWidth=layout.width,groundHeight=layout.height)
      }
      combatEngine.lostCourier?.let { mission ->
        with(com.example.game.pixel.CourierPresentation) {
          mission.eliteTelegraph?.let { cue ->
            drawEliteTelegraph(cue.x,cue.y,cue.radius,cue.remainingSec/cue.durationSec,camX,camY,zoom)
          }
          if(mission.scoutWarningRemaining>0f && !mission.scoutCalledReinforcements) {
            combatEngine.enemies.firstOrNull { !it.dead && it.id==mission.scoutEnemyId }?.let { scout ->
              drawScoutAlert(scout.x,scout.y,camX,camY,zoom)
            }
          }
        }
      }
      if(settings.performanceOverlay) visibleDrawables[0] = drawables.count { item ->
        val (x,y) = when(item) {
          is BattleDrawable.Prop -> item.obs.x to item.obs.y
          is BattleDrawable.Hero -> item.unit.x to item.unit.y
          is BattleDrawable.Enemy -> item.unit.x to item.unit.y
          is BattleDrawable.Master -> item.x to item.y
          is BattleDrawable.Courier -> item.mission.courier.x to item.mission.courier.y
        }
        val sx=ObliqueCamera.worldToScreenX(x,camX,zoom,halfW)
        val sy=ObliqueCamera.worldToScreenY(y,camY,zoom,halfH)
        sx>=-380f && sx<=size.width+380f && sy>=-400f && sy<=size.height+380f
      }
      drawables.forEach { item ->
        when (item) {
          is BattleDrawable.Prop -> drawProp(item.obs, camX, camY, zoom, animTimeSec,renderShadow=null)
          is BattleDrawable.Master -> Unit // External commander, no controllable avatar.
          is BattleDrawable.Courier -> {
            val npc=item.mission.courier
            with(com.example.game.pixel.CourierPresentation) {
              drawCourier(npc.x,npc.y,camX,camY,zoom,npc.isMoving,npc.facingRight,npc.hurtFlash,npc.hp,npc.maxHp,animTimeSec)
            }
            drawActorCanopyOcclusion(layout.obstacles,npc.x,npc.y,camX,camY,zoom,animTimeSec)
          }
          is BattleDrawable.Hero -> {
            val h = item.unit
            val aura = when {
              h.shieldHp > 0 -> PortalCyan
              combatEngine.selectedHeroId == h.id -> SummonerGold.copy(alpha = 0.65f)
              else -> null
            }
            drawCharacterSprite(
              worldX = h.x,
              worldY = h.y,
              camX = camX,
              camY = camY,
              zoom = zoom,
              isMaster = false,
              heroClass = h.heroClass,
              facingRight = h.facingRight,
              isMoving = h.isMoving,
              attackAnimProgress = h.attackAnim,
              hurtFlash = h.hurtFlash,
              hpFraction = h.hp.toFloat() / h.maxHp.coerceAtLeast(1),
              shieldOrAuraColor = aura,
              isInjuredResting = false,
              timeSec = animTimeSec,
              heroName = h.name,
              skillAnimation = h.premiumSample && h.contactAttack.skill,
              emotion = when {
                combatEngine.stage == MissionStage.VICTORY -> "victory"
                h.fearSecondsRemaining > 0f || combatEngine.activeOrder == TacticalOrder.RETREAT || h.hp < h.maxHp*.25f -> "fear"
                h.currentTargetId != null -> "alert"
                else -> "calm"
              },
            )
            drawActorCanopyOcclusion(layout.obstacles,h.x,h.y,camX,camY,zoom,animTimeSec)
          }
          is BattleDrawable.Enemy -> {
            val e = item.unit
            drawEnemySprite(
              worldX = e.x,
              worldY = e.y,
              camX = camX,
              camY = camY,
              zoom = zoom,
              archetype = e.archetype,
              facingRight = e.facingRight,
              isMoving = e.isMoving,
              attackAnimProgress = e.attackAnim,
              hurtFlash = e.hurtFlash,
              hpFraction = e.hp.toFloat() / e.maxHp.coerceAtLeast(1),
              isTargetMarked = !e.dead && combatEngine.designatedTargetId == e.id,
              timeSec = animTimeSec,
              deathAnimProgress = if (e.dead) e.deathAnimProgress else 0f,
            )
            drawActorCanopyOcclusion(layout.obstacles,e.x,e.y,camX,camY,zoom,animTimeSec)
          }
        }
      }

      // Walk-behind upper forest canopy occlusion pass
      // 3/4 Elevated Projectiles (flying at torso height above ground shadow)
      combatEngine.projectiles.forEach { p ->
        val px = ObliqueCamera.worldToScreenX(p.x, camX, zoom, halfW)
        val py = ObliqueCamera.worldToScreenY(p.y, camY, zoom, halfH, elevationZ = 28f)
        val tailX = ObliqueCamera.worldToScreenX(p.x - p.vx * 0.045f, camX, zoom, halfW)
        val tailY = ObliqueCamera.worldToScreenY(p.y - p.vy * 0.045f, camY, zoom, halfH, elevationZ = 28f)
        drawLine(
          color = p.color.copy(alpha = 0.55f),
          start = Offset(tailX, tailY),
          end = Offset(px, py),
          strokeWidth = (p.radius * 1.4f) * zoom,
        )
        drawCircle(
          color = p.color,
          radius = p.radius * zoom,
          center = Offset(px, py),
        )
      }

      // Overhead 3/4 Hero Labels + Live Observable AI Intent Tag & Boss Nameplate (Exact Rect non-overlap!)
      val topHudSafePx = if (topHudHeightPx > 0) topHudHeightPx.toFloat()
        else (if (isWideScreen) 64.dp else 112.dp).toPx()
      val placedHeroPillRects = mutableListOf<androidx.compose.ui.geometry.Rect>()
      combatEngine.heroes.filter { !it.knockedOut }.forEach { h ->
        val hx = ObliqueCamera.worldToScreenX(h.x, camX, zoom, halfW)
        var hy = ObliqueCamera.worldToScreenY(h.y, camY, zoom, halfH, elevationZ = 88f)
        val shortName = h.name.substringBefore(" ")
        val label = if (settings.showHeroAiIntent) "$shortName • ${h.aiIntentLabelVi}" else shortName
        val measured = textMeasurer.measure(
          text = label,
          style = TextStyle(color = ParchmentLight, fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
        )
        val boxW = measured.size.width + 10f
        val boxH = measured.size.height + 4f
        val clampedLeft = (hx - boxW / 2f).coerceIn(14f, (size.width - boxW - 14f).coerceAtLeast(14f))

        var attempts = 0
        while (
          attempts < 6 &&
          placedHeroPillRects.any {
            it.overlaps(androidx.compose.ui.geometry.Rect(Offset(clampedLeft - 3f, hy - 4f), Size(boxW + 6f, boxH + 4f)))
          }
        ) {
          hy -= (boxH + 4f)
          attempts++
        }
        if (hy < topHudSafePx + 6f) {
          hy = ObliqueCamera.worldToScreenY(h.y, camY, zoom, halfH, elevationZ = -10f)
        }
        if (hx in 16f..(size.width - 16f) && hy in (topHudSafePx + 4f)..(size.height - 96f)) {
          val rect = androidx.compose.ui.geometry.Rect(Offset(clampedLeft, hy - 2f), Size(boxW, boxH))
          if (placedHeroPillRects.none { it.overlaps(rect) }) {
            placedHeroPillRects += rect
            drawRoundRect(
              color = Color(0xBB090712),
              topLeft = rect.topLeft,
              size = rect.size,
              cornerRadius = CornerRadius(5f, 5f),
            )
            drawText(measured, topLeft = Offset(clampedLeft + 5f, hy))
          }
        }
      }

      // Mission waypoint uses the existing ground projection; it never changes world geometry.
      combatEngine.lostCourier?.let { mission ->
        mission.journey?.takeIf { it.active }?.let { journey ->
          val fieldTarget = journey.fieldAction?.currentTarget?.takeIf { journey.fieldReady }
          val sx = ObliqueCamera.worldToScreenX(fieldTarget?.x ?: journey.targetX, camX, zoom, halfW)
          val sy = ObliqueCamera.worldToScreenY(fieldTarget?.y ?: journey.targetY, camY, zoom, halfH)
          if (sx in -100f..(size.width+100f) && sy in -100f..(size.height+100f)) {
            val rx = 34f*zoom
            val ry = rx*ObliqueCamera.OBLIQUE_Y_SCALE
            drawOval(PortalCyan.copy(alpha=.9f),Offset(sx-rx,sy-ry),Size(rx*2f,ry*2f),style=Stroke(2.5f*zoom))
            drawLine(PortalCyan,Offset(sx-10f*zoom,sy),Offset(sx+10f*zoom,sy),2f*zoom)
            drawLine(PortalCyan,Offset(sx,sy-7f*zoom),Offset(sx,sy+7f*zoom),2f*zoom)
            val label = textMeasurer.measure(fieldTarget?.labelVi ?: if(journey.canInteract) "Khảo sát tại đây" else "Lối tới mục tiêu",
              style=TextStyle(color=PortalCyan,fontSize=9.5.sp,fontWeight=FontWeight.Bold))
            val ly=sy-46f*zoom
            if(ly in (topHudSafePx+8f)..(size.height-96f)) {
              val lx=(sx-label.size.width/2f).coerceIn(12f,(size.width-label.size.width-12f).coerceAtLeast(12f))
              drawRoundRect(Color(0xDD0D1322),Offset(lx-6f,ly-2f),Size(label.size.width+12f,label.size.height+4f),CornerRadius(6f,6f))
              drawText(label,topLeft=Offset(lx,ly))
            }
          }
        }
        if ((mission.choiceAvailable || mission.choice == com.example.game.combat.CourierChoice.RECOVER_SUPPLIES) &&
          !mission.suppliesRecovered) {
          val sx = ObliqueCamera.worldToScreenX(mission.supplyX, camX, zoom, halfW)
          val sy = ObliqueCamera.worldToScreenY(mission.supplyY, camY, zoom, halfH)
          val rx = 26f * zoom
          val ry = rx * ObliqueCamera.OBLIQUE_Y_SCALE
          drawOval(SummonerGold.copy(alpha = .9f), Offset(sx-rx,sy-ry), Size(rx*2,ry*2),
            style = Stroke(2f*zoom))
          drawLine(SummonerGold,Offset(sx-8f*zoom,sy),Offset(sx+8f*zoom,sy),2f*zoom)
          drawLine(SummonerGold,Offset(sx,sy-6f*zoom),Offset(sx,sy+6f*zoom),2f*zoom)
          val label = textMeasurer.measure("Tiếp vận • đứng cạnh 3s",
            style = TextStyle(color=SummonerGold,fontSize=9.5.sp,fontWeight=FontWeight.Bold))
          val ly = sy - 42f*zoom
          if (sx in 18f..(size.width-18f) && ly in (topHudSafePx+8f)..(size.height-96f)) {
            drawRoundRect(Color(0xDD0D1322),Offset(sx-label.size.width/2f-6f,ly-2f),
              Size(label.size.width+12f,label.size.height+4f),CornerRadius(6f,6f))
            drawText(label,topLeft=Offset(sx-label.size.width/2f,ly))
          }
        }
      }

      // Unclaimed Exploration Discovery Callouts
      combatEngine.discoveryNodes.filter { !it.discovered }.forEach { node ->
        val nx = ObliqueCamera.worldToScreenX(node.x, camX, zoom, halfW)
        val ny = ObliqueCamera.worldToScreenY(node.y, camY, zoom, halfH, elevationZ = 62f)
        if (nx in 18f..(size.width - 18f) && ny in (topHudSafePx + 8f)..(size.height - 96f)) {
          val badgeColor = if (node.kind == DiscoveryNodeKind.HEALING_SHRINE) EmeraldVitality else SummonerGold
          val measured = textMeasurer.measure(
            text = "✦ ${node.titleVi}",
            style = TextStyle(color = badgeColor, fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
          )
          drawRoundRect(
            color = Color(0xCC0D1322),
            topLeft = Offset(nx - measured.size.width / 2f - 6f, ny - 2f),
            size = Size(measured.size.width + 12f, measured.size.height + 4f),
            cornerRadius = CornerRadius(6f, 6f),
          )
          drawText(measured, topLeft = Offset(nx - measured.size.width / 2f, ny))
        }
      }

      combatEngine.enemies.filter { !it.dead && it.archetype.isBoss }.forEach { boss ->
        val bx = ObliqueCamera.worldToScreenX(boss.x, camX, zoom, halfW)
        val by = ObliqueCamera.worldToScreenY(boss.y, camY, zoom, halfH, elevationZ = 128f)
        if (bx in -80f..(size.width + 80f) && by in 24f..size.height) {
          val measured = textMeasurer.measure(
            text = "★ BOSS: ${boss.archetype.nameVi}",
            style = TextStyle(color = Color(0xFFFDA4AF), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold),
          )
          drawRoundRect(
            color = Color(0xDD1E0912),
            topLeft = Offset(bx - measured.size.width / 2f - 6f, by - 3f),
            size = Size(measured.size.width + 12f, measured.size.height + 6f),
            cornerRadius = CornerRadius(6f, 6f),
          )
          drawText(measured, topLeft = Offset(bx - measured.size.width / 2f, by))
        }
      }

      // Floating Damage / Heal / Tactical Callout Numbers in 3/4 Space
      combatEngine.floatingTexts.forEach { ft ->
        val fx = ObliqueCamera.worldToScreenX(ft.x, camX, zoom, halfW)
        val fy = ObliqueCamera.worldToScreenY(ft.y, camY, zoom, halfH, elevationZ = 56f)
        if (fx in 12f..(size.width - 12f) && fy in 24f..(size.height - 24f)) {
          val alpha = (1f - ft.elapsed / ft.duration).coerceIn(0f, 1f)
          val measured = textMeasurer.measure(
            text = ft.text,
            style = TextStyle(
              color = ft.color.copy(alpha = alpha),
              fontSize = if (ft.isLarge) 12.sp else 10.sp,
              fontWeight = FontWeight.ExtraBold,
            ),
          )
          drawText(measured, topLeft = Offset(fx - measured.size.width / 2f, fy))
        }
      }

      drawForegroundAtmosphere(layout.biome, animTimeSec, settings.particlesEnabled)
      if(settings.performanceOverlay) performanceProbe.recordDraw(System.nanoTime()-drawStarted)
    }

    if(settings.performanceOverlay) com.example.performance.ScenePerformanceOverlay(performanceSnapshot,
      Modifier.align(Alignment.CenterEnd).padding(8.dp))
    // 2. Slim, Responsive Top Tactical HUD (Never covers the battlefield or collides with Joystick!)
    val hudTick = hudRevision
    if (hudTick >= 0) {
      val remainingEnemies = combatEngine.enemies.count { !it.dead }
      val focusedEnemy = combatEngine.enemies.firstOrNull { !it.dead && it.id == combatEngine.designatedTargetId }
        ?: combatEngine.enemies.firstOrNull { !it.dead && it.archetype.isBoss }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .onSizeChanged { topHudHeightPx = it.height }
          .testTag("tower_battle_top_hud"),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Left: Compact Master HP + Floor Objective Pill
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = FantasySkin.Panel.copy(alpha = 0.90f),
            modifier = Modifier
              .weight(if (isWideScreen) 0.78f else 1f)
              .border(1.dp, SummonerGold.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
          ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = state.selectedFloor.titleVi,
                  style = MaterialTheme.typography.labelLarge,
                  color = SummonerGold,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "HP ${combatEngine.masterHp} • Quái: $remainingEnemies",
                  style = MaterialTheme.typography.labelSmall,
                  color = PortalCyan,
                  maxLines = 1,
                  softWrap = false,
                )
              }
              Spacer(modifier = Modifier.height(3.dp))
              LinearProgressIndicator(
                progress = { (combatEngine.masterHp.toFloat() / combatEngine.masterMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp)),
                color = PortalCyan,
                trackColor = ObsidianVoid,
              )
            }
          }

          // Center (Widescreen Landscape): Horizontal Squad Party & Signature Skill Strip!
          if (isWideScreen) {
            SquadSkillHorizontalBar(
              heroes = combatEngine.heroes,
              onTriggerSkill = combatEngine::triggerHeroManualSkill,
              modifier = Modifier.weight(1.62f),
            )
          }

          // Right: Live Tactical Minimap + Retreat Icon
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = ObsidianVoid.copy(alpha = 0.90f),
            modifier = Modifier
              .size(width = 92.dp, height = 52.dp)
              .border(1.dp, BorderRune, RoundedCornerShape(10.dp))
              .testTag("tactical_minimap"),
          ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
              val mw = combatEngine.mapLayout.width.coerceAtLeast(1f)
              val mh = combatEngine.mapLayout.height.coerceAtLeast(1f)
              if (!combatEngine.eventWaveTriggered) {
                val trigX = (combatEngine.floorSpec.eventTriggerX / mw) * size.width
                drawLine(WarningAmber.copy(alpha = 0.45f), Offset(trigX, 0f), Offset(trigX, size.height), strokeWidth = 1.2f)
              }
              combatEngine.discoveryNodes.filter { !it.discovered }.forEach { node ->
                drawCircle(
                  color = if (node.kind == DiscoveryNodeKind.HEALING_SHRINE) EmeraldVitality else SummonerGold,
                  radius = 3.2f,
                  center = Offset((node.x / mw) * size.width, (node.y / mh) * size.height),
                )
              }
              combatEngine.enemies.filter { !it.dead }.forEach { e ->
                drawCircle(
                  color = if (e.archetype.isBoss) Color(0xFFF43F5E) else CrimsonBlood,
                  radius = if (e.archetype.isBoss) 4.6f else 2.6f,
                  center = Offset((e.x / mw) * size.width, (e.y / mh) * size.height),
                )
              }
              combatEngine.heroes.filter { !it.knockedOut }.forEach { h ->
                drawCircle(PortalCyan, 2.8f, Offset((h.x / mw) * size.width, (h.y / mh) * size.height))
              }
              drawCircle(
                color = SummonerGold,
                radius = 3.8f,
                center = Offset((combatEngine.masterX / mw) * size.width, (combatEngine.masterY / mh) * size.height),
              )
            }
          }

          IconButton(
            onClick = { showRetreatConfirm = true },
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(SanctuaryStone.copy(alpha = 0.90f))
              .border(1.dp, BorderRune, RoundedCornerShape(10.dp))
              .testTag("retreat_mission_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ExitToApp,
              contentDescription = "Rút lui về Thánh Địa",
              tint = WarningAmber,
            )
          }
        }

        combatEngine.lostCourier?.let { mission ->
          val missionHint=buildString {
            if(mission.scoutWarningRemaining>0f && !mission.scoutCalledReinforcements)
              append("Trinh sát gọi viện sau ${kotlin.math.ceil(mission.scoutWarningRemaining).toInt()}s. ")
            mission.eliteTelegraph?.let { append("Né vòng đỏ! ${String.format(java.util.Locale.US,"%.1f",it.remainingSec)}s. ") }
            combatEngine.heroes.firstOrNull { it.fearSecondsRemaining>0f }?.let {
              append("${it.name}: Sợ ${kotlin.math.ceil(it.fearSecondsRemaining).toInt()}s (-20% lực đánh/-15% tốc độ). ")
            }
            append(mission.statusVi)
          }
          val journey = mission.journey
          if (journey != null && journey.active) {
            com.example.ui.components.RescueJourneyHud(
              objective=journey.objectiveVi,status=journey.statusVi,
              completedObjectives=journey.chapterIndex,totalObjectives=journey.chapterCount,
              courierHp=mission.courier.hp,courierMaxHp=mission.courier.maxHp,
              canInteract=journey.canInteract,decisionPending=journey.decisionPending,
              onFollow={combatEngine.chooseCourierOrder(com.example.game.combat.CourierOrder.PROTECT);hudRevision++},
              onInteract={journey.interact();hudRevision++},
              onSilenceAlarm={journey.chooseRoute(true);hudRevision++},
              onRescueScout={journey.chooseRoute(false);hudRevision++},
              fieldTargetLabel=journey.fieldAction?.currentTarget?.labelVi?.takeIf { journey.fieldReady },
              fieldHint=journey.fieldAction?.hintVi,
              fieldCanInteract=journey.fieldReady && journey.fieldAction?.canInteract(
                combatEngine.heroes.filter { !it.knockedOut }.map { it.x to it.y }) == true,
              onFieldTravel={
                journey.fieldAction?.currentTarget?.takeIf { journey.fieldReady }?.let { target ->
                  combatEngine.issueTacticalOrder(TacticalOrder.MOVE_TO)
                  combatEngine.onBattlefieldTapped(target.x,target.y)
                  hudRevision++
                }
              },
              onFieldInteract={journey.interactField();hudRevision++},
              modifier=Modifier.fillMaxWidth(),compact=isWideScreen)
          } else {
          com.example.ui.components.CourierMissionHud(mission.objectiveVi,missionHint,
            mission.courier.hp,mission.courier.maxHp,mission.progress,
            combatEngine.activeOrder == TacticalOrder.PROTECT_ALLY,
            onProtect={combatEngine.chooseCourierOrder(com.example.game.combat.CourierOrder.PROTECT);hudRevision++},
            onIntercept={combatEngine.chooseCourierOrder(com.example.game.combat.CourierOrder.INTERCEPT);hudRevision++},
            modifier=Modifier.fillMaxWidth(),
            interceptAvailable=combatEngine.enemies.any { !it.dead && it.id==mission.scoutEnemyId },
            compact=isWideScreen,
            intercepting=combatEngine.activeOrder==TacticalOrder.ATTACK_TARGET &&
              combatEngine.designatedTargetId==mission.scoutEnemyId,
            choiceAvailable=mission.choiceAvailable,
            onSupport={mission.chooseChoice(com.example.game.combat.CourierChoice.SUPPORT_COURIER);hudRevision++},
            onRecoverSupplies={mission.chooseChoice(com.example.game.combat.CourierChoice.RECOVER_SUPPLIES);hudRevision++},
            suppliesPending=mission.choice==com.example.game.combat.CourierChoice.RECOVER_SUPPLIES && !mission.suppliesRecovered,
            onGoToSupplies={
              combatEngine.issueTacticalOrder(TacticalOrder.MOVE_TO)
              combatEngine.onBattlefieldTapped(mission.supplyX,mission.supplyY)
              hudRevision++
            })
          }
        }

        if (introDismissed && !settlementDispatched && !showRetreatConfirm) {
          combatEngine.heroAgencyAlert?.let { alert ->
            HeroAgencyAlertCard(
              heroName = alert.heroName,
              message = alert.messageVi,
              onRegroup = { combatEngine.issueTacticalOrder(TacticalOrder.REGROUP); hudRevision++ },
              onProtect = { combatEngine.issueTacticalOrder(TacticalOrder.PROTECT_ALLY); hudRevision++ },
              onRetreat = { showRetreatConfirm = true },
              onDismiss = { combatEngine.dismissHeroAgencyAlert(); hudRevision++ },
              modifier = Modifier.fillMaxWidth().widthIn(max = 540.dp),
            )
          }
        }

        // Narrow Portrait: Place Squad Skill Strip cleanly on a slim horizontal row!
        if (!isWideScreen) {
          SquadSkillHorizontalBar(
            heroes = combatEngine.heroes,
            onTriggerSkill = combatEngine::triggerHeroManualSkill,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        // Compact Targeted Enemy / Boss HP Bar (only when locked on or Boss active)
        if (focusedEnemy != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xDD1F0B14),
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .widthIn(max = 310.dp)
              .fillMaxWidth(0.75f)
              .border(1.dp, CrimsonBlood, RoundedCornerShape(8.dp)),
          ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "🎯 ${focusedEnemy.archetype.nameVi}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFFECDD3),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Text(
                  text = "${focusedEnemy.hp}/${focusedEnemy.maxHp}",
                  style = MaterialTheme.typography.labelSmall,
                  color = SummonerGold,
                  maxLines = 1,
                )
              }
              LinearProgressIndicator(
                progress = { (focusedEnemy.hp.toFloat() / focusedEnemy.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = CrimsonBlood,
                trackColor = ObsidianVoid,
              )
            }
          }
        }

        if (combatEngine.awaitingMoveToTap) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SummonerGold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
          ) {
            Text(
              text = "📍 Chạm lên bản đồ để cắm cờ điều quân!",
              style = MaterialTheme.typography.labelSmall,
              color = ObsidianVoid,
              maxLines = 1,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            )
          }
        }
      }
    }

    // 3. Mid-Mission Manhwa Event Cut-In Banner
    if (combatEngine.stage == MissionStage.EVENT_CUTSCENE_BANNER) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xEE1A0B18),
        modifier = Modifier
          .align(Alignment.Center)
          .padding(horizontal = 20.dp)
          .widthIn(max = 520.dp)
          .border(1.5.dp, CrimsonBlood, RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = combatEngine.floorSpec.eventHeadlineVi,
            style = MaterialTheme.typography.titleSmall,
            color = SummonerGold,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${combatEngine.floorSpec.eventDialogueSpeaker}: «${combatEngine.floorSpec.eventDialogueLineVi}»",
            style = MaterialTheme.typography.bodySmall,
            color = ParchmentLight,
          )
        }
      }
    }

    // 4. Bottom Controls: Virtual Joystick (Bottom-Left) + Zoom & 8 Tactical Orders (Bottom-Right, Zero Overlap!)
    val joyDiameter = if (isShortHeight) 96.dp else 106.dp
    Box(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()
        .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
      VirtualTouchJoystick(
        onVectorChanged = { dx, dy ->
          joyDx = dx
          joyDy = dy
        },
        diameter = joyDiameter,
        modifier = Modifier.align(Alignment.BottomStart),
      )

      Column(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(start = joyDiameter + 10.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        val feedbackMsg = combatEngine.commandFeedback
        if (feedbackMsg != null && combatEngine.stage == MissionStage.IN_PROGRESS) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xEA121829),
            modifier = Modifier
              .border(1.dp, PortalCyan.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
              .testTag("tactical_command_feedback_pill"),
          ) {
            Text(
              text = feedbackMsg,
              style = MaterialTheme.typography.labelSmall,
              color = SummonerGold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        CameraZoomControls(
          zoom = zoom,
          onZoomChange = { zoom = it },
          horizontal = true,
        )

        Row(
          modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .background(ObsidianVoid.copy(alpha = 0.86f), RoundedCornerShape(12.dp))
            .border(1.dp, SummonerGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          TacticalOrder.entries.forEach { order ->
            val isSelected = combatEngine.activeOrder == order
            Surface(
              shape = RoundedCornerShape(9.dp),
              color = if (isSelected) SummonerGold else SanctuaryStone.copy(alpha = 0.94f),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .border(
                  1.dp,
                  if (isSelected) Color.White else BorderRune,
                  RoundedCornerShape(9.dp),
                )
                .testTag("order_${order.name.lowercase()}")
                .clickable { combatEngine.issueTacticalOrder(order) },
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (isSelected) ObsidianVoid else PortalCyan,
                    modifier = Modifier.size(12.dp),
                  )
                  Text(
                    text = "[${order.hotkey}] ${order.shortVi}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) ObsidianVoid else SummonerGold,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
                Text(
                  text = order.labelVi,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isSelected) ObsidianVoid else ParchmentLight,
                  maxLines = 1,
                  softWrap = false,
                )
              }
            }
          }
        }
      }
    }

    // 5. Floor Intro Manhwa Dialogue Box
    if (combatEngine.stage == MissionStage.INTRO_DIALOGUE) {
      val beat = introBeats[introIndex.coerceIn(0, introBeats.lastIndex)]
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(ObsidianVoid.copy(alpha = 0.72f))
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(14.dp),
        contentAlignment = Alignment.BottomCenter,
      ) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = FantasySkin.Panel,
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .border(1.5.dp, SummonerGold, RoundedCornerShape(16.dp)),
        ) {
          Column(
            modifier = Modifier
              .verticalScroll(rememberScrollState())
              .padding(16.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                  text = "${state.selectedFloor.titleVi} — ${beat.speakerTitle}",
                  style = MaterialTheme.typography.labelSmall,
                  color = PortalCyan,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = beat.lineVi,
              style = MaterialTheme.typography.bodyMedium,
              color = ParchmentLight,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Button(
                onClick = {
                  if (introIndex < introBeats.lastIndex) {
                    introIndex++
                  } else {
                    combatEngine.stage = MissionStage.IN_PROGRESS
                    if (combatEngine.lostCourier?.journey?.active == true) {
                      combatEngine.chooseCourierOrder(com.example.game.combat.CourierOrder.PROTECT)
                    }
                    introDismissed = true
                    hudRevision++
                  }
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag("start_combat_button"),
                colors = FantasySkin.buttonColors(
                  containerColor = SummonerGold,
                  contentColor = ObsidianVoid,
                ),
              ) {
                Text(
                  text = if (introIndex < introBeats.lastIndex) "Tiếp Tục ▸" else "Bắt Đầu Chiến Đấu ⚔",
                  style = MaterialTheme.typography.labelLarge,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
    }

    // 6. Tactical Retreat Confirmation Modal
    if (showRetreatConfirm) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(ObsidianVoid.copy(alpha = 0.80f))
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(16.dp),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = FantasySkin.Panel,
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .border(1.5.dp, WarningAmber, RoundedCornerShape(16.dp)),
        ) {
          Column(
            modifier = Modifier
              .verticalScroll(rememberScrollState())
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Rút Lui Về Thánh Địa?",
              style = MaterialTheme.typography.titleMedium,
              color = WarningAmber,
            )
            Text(
              text = "Anh hùng sẽ di chuyển về điểm rút lui. Bảo vệ họ trên đường; nhiệm vụ kết thúc khi những người còn đứng vững tới nơi.",
              style = MaterialTheme.typography.bodySmall,
              color = ParchmentLight,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedButton(
                onClick = { showRetreatConfirm = false },
                modifier = Modifier.heightIn(min = 48.dp),
              ) {
                Text("Tiếp Tục", color = ParchmentLight, maxLines = 1)
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = {
                  showRetreatConfirm = false
                  issueRetreatFromConfirmation(combatEngine)
                  hudRevision++
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_retreat_button"),
                colors = FantasySkin.buttonColors(
                  containerColor = WarningAmber,
                  contentColor = ObsidianVoid,
                ),
              ) {
                Text("Xác Nhận Rút Lui", maxLines = 1)
              }
            }
          }
        }
      }
    }

    // 7. Expedition Victory / Tactical Retreat / Defeat Settlement Modal
    if (combatEngine.stage == MissionStage.VICTORY || combatEngine.stage == MissionStage.DEFEAT) {
      val isWin = combatEngine.stage == MissionStage.VICTORY
      val settlement = state.lastSettlement
      val isTacticalRetreat = !isWin && (combatEngine.retreatCompleted || settlement?.outcomeType == ExpeditionOutcomeType.TACTICAL_RETREAT)
      val modalBorderColor = when {
        isWin -> SummonerGold
        isTacticalRetreat -> PortalCyan
        else -> CrimsonBlood
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(ObsidianVoid.copy(alpha = 0.86f))
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(14.dp),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = FantasySkin.Panel,
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 540.dp)
            .border(
              2.dp,
              modalBorderColor,
              RoundedCornerShape(18.dp),
            ),
        ) {
          Column(
            modifier = Modifier
              .verticalScroll(rememberScrollState())
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            if (!isShortHeight) {
              Image(
                painter = painterResource(id = state.selectedFloor.bannerRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(100.dp)
                  .clip(RoundedCornerShape(10.dp)),
              )
            }

            Text(
              text = when {
                isWin -> "✦ CHINH PHỤC THÀNH CÔNG • ${state.selectedFloor.codeName} ✦"
                isTacticalRetreat -> "⚑ RÚT LUI CHIẾN THUẬT AN TOÀN — BẢO TOÀN ĐỘI HÌNH"
                else -> "⚠ NHIỆM VỤ THẤT BẠI — ĐỘI HÌNH DƯỠNG THƯƠNG"
              },
              style = MaterialTheme.typography.titleMedium,
              color = modalBorderColor,
            )

            if (settlement == null) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                CircularProgressIndicator(color = SummonerGold, modifier = Modifier.size(22.dp))
                if(!state.isBusyAction && state.bannerFeedback != null) {
                  Button(onClick={
                    if(onFinishExpeditionDetailed!=null) onFinishExpeditionDetailed(isWin,combatEngine.fallenHeroIds(),combatEngine.retreatCompleted,combatEngine.bonusDiscoveryGold,combatEngine.bonusDiscoveryMana,combatEngine.discoveredNodeIds(),combatEngine.lostCourier?.outcome,combatEngine.lostCourier?.frightenedHeroId)
                    else onFinishExpedition(isWin,combatEngine.fallenHeroIds())
                  },modifier=Modifier.testTag("retry_settlement_button")) { Text("Thử lưu lại") }
                }
                Text(
                  text = "Đang đồng bộ chiến lợi phẩm về Thánh Địa...",
                  style = MaterialTheme.typography.bodySmall,
                  color = ParchmentLight,
                )
              }
            } else {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = ObsidianVoid.copy(alpha = 0.72f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  com.example.ui.components.RewardSummary(settlement.goldEarned,settlement.manaEarned,settlement.shardsEarned,settlement.oreEarned,settlement.essenceEarned)
                  Text(if(isWin) "Tiếp theo: nâng cấp đội hình và mở thử thách mới." else "Tiếp theo: hồi phục đội hình tại Khu Nghỉ Ngơi.",color=RuneSilver,style=MaterialTheme.typography.bodySmall)
                  if (settlement.discoveredNodesCount > 0) {
                    Text(
                      text = "✦ Khám phá thực địa (${settlement.discoveredNodesCount} điểm): +${settlement.bonusDiscoveryGold} Vàng • +${settlement.bonusDiscoveryMana} Mana",
                      style = MaterialTheme.typography.labelMedium,
                      color = PortalCyan,
                    )
                  }
                  if (settlement.lootedItemName != null) {
                    Text(
                      text = "🗡 Chiến lợi phẩm đặc biệt: [${settlement.lootedItemName}]!",
                      style = MaterialTheme.typography.labelLarge,
                      color = ArcaneViolet,
                    )
                  }
                  if(settlement.courierOutcome!=null) {
                    Text(com.example.data.LostCourierProgression.outcomeDescription(settlement.courierOutcome),
                      color=PortalCyan,style=MaterialTheme.typography.bodySmall)
                  }
                  if (settlement.leveledUpHeroNames.isNotEmpty()) {
                    Text(
                      text = "⬆ Thăng cấp: ${settlement.leveledUpHeroNames.joinToString(", ")}",
                      style = MaterialTheme.typography.bodySmall,
                      color = EmeraldVitality,
                    )
                  }
                  if (settlement.injuredHeroNames.isNotEmpty()) {
                    Text(
                      text = "🩹 Đưa về giường Khu Nghỉ Ngơi để hồi phục: ${settlement.injuredHeroNames.joinToString(", ")}",
                      style = MaterialTheme.typography.bodySmall,
                      color = WarningAmber,
                    )
                  } else if (isTacticalRetreat) {
                    Text(
                      text = "✓ Nhờ lệnh rút lui chiến thuật kịp thời, toàn bộ Anh hùng còn đứng vững đều bảo toàn thể trạng!",
                      style = MaterialTheme.typography.bodySmall,
                      color = PortalCyan,
                    )
                  }
                }
              }
            }

            if (!isWin && onPrepareRetry != null) {
              OutlinedButton(
                onClick = onPrepareRetry,
                enabled = !state.isBusyAction && settlement != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("prepare_rescue_retry_button"),
              ) { Text("Chuẩn bị thử lại • Giữ tiến trình và thương tích") }
            }

            Button(
              onClick = onReturnToSanctuary,
              enabled = !state.isBusyAction && settlement != null,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("return_sanctuary_button"),
              shape = RoundedCornerShape(12.dp),
              colors = FantasySkin.buttonColors(
                containerColor = SummonerGold,
                contentColor = ObsidianVoid,
              ),
            ) {
              Text(
                text = "Trở Về Thánh Địa (The Sanctuary)",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SquadSkillHorizontalBar(
  heroes: List<HeroCombatUnit>,
  onTriggerSkill: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    heroes.forEach { hero ->
      val ready = !hero.knockedOut && hero.skillCooldown <= 0.05f
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = FantasySkin.Panel.copy(alpha = if (hero.knockedOut) 0.55f else 0.90f),
        modifier = Modifier
          .width(124.dp)
          .heightIn(min = 48.dp)
          .border(
            1.dp,
            if (ready) SummonerGold else BorderRune,
            RoundedCornerShape(10.dp),
          )
          .testTag("skill_btn_${hero.id}")
          .clickable(enabled = !hero.knockedOut) { onTriggerSkill(hero.id) },
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PixelHeroPortraitAvatar(heroName = hero.name, heroClass = hero.heroClass, sizeDp = 30.dp)
          Column(modifier = Modifier.weight(1f)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = hero.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (hero.knockedOut) CrimsonBlood else ParchmentLight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = when {
                  hero.knockedOut -> "KO"
                  ready -> "⚡"
                  else -> "${hero.skillCooldown.toInt() + 1}s"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (ready) SummonerGold else RuneSilver,
                maxLines = 1,
                softWrap = false,
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
              progress = { (hero.hp.toFloat() / hero.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
              color = if (hero.shieldHp > 0) PortalCyan else EmeraldVitality,
              trackColor = ObsidianVoid,
            )
          }
        }
      }
    }
  }
}
