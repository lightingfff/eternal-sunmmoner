package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.R
import com.example.data.FeastBuffHelper
import com.example.data.SanctuaryProgressionHelper
import com.example.data.model.EquipmentRecord
import com.example.data.model.FacilityType
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.HeroStatusType
import com.example.data.model.IslandExpansionMilestone
import com.example.data.model.SanctuaryMealRecipe
import com.example.data.model.TrainingFocus
import com.example.game.map.TowerFloorSpec
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.tower.TowerFloorCatalog
import com.example.performance.SceneFrameLoop
import com.example.ui.SummonerUiState
import com.example.ui.components.FantasyButton as Button
import com.example.ui.components.FantasySkin
import com.example.ui.components.ResourcePillBadge
import com.example.ui.components.p28.ESBannerNotice
import com.example.ui.components.p28.ESHeroCard
import com.example.ui.components.p28.NoticePriority
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The three monumental Dimensional Portals inside the Astral Nexus Mini-Map after the Tower Gate opens.
 */
enum class DimensionalNexusGate(
  val id: String,
  val titleVi: String,
  val shortVi: String,
  val systemHeader: String,
  val systemRankBadge: String,
  val systemPromptVi: String,
  val primaryColor: Color,
  val secondaryColor: Color,
) {
  EVENT_GATE(
    id = "event",
    titleVi = "CỔNG SỰ KIỆN",
    shortVi = "Cổng Sự Kiện",
    systemHeader = "[ URGENT QUEST • DỊ BIẾN CỔNG SỰ KIỆN ]",
    systemRankBadge = "RANK S • RED GATE",
    systemPromptVi = "Phát hiện vết nứt Huyết Nguyệt & Thủ Lĩnh Dị Giới! Vượt Cổng Sự Kiện nhận thưởng Hồn Tinh & Cổ Ngữ đặc biệt.",
    primaryColor = Color(0xFFF43F5E),
    secondaryColor = ArcaneViolet,
  ),
  TOWER_GATE(
    id = "tower",
    titleVi = "CỔNG TÒA THÁP",
    shortVi = "Cổng Tòa Tháp",
    systemHeader = "[ MAIN SCENARIO • HỆ THỐNG THÁP VĨNH HẰNG ]",
    systemRankBadge = "FLOORS 01–100",
    systemPromptVi = "Trục chính Tháp Vĩnh Hằng đã mở! Chinh phục tuần tự các tầng tháp để phá giải phong ấn cổ đại.",
    primaryColor = SummonerGold,
    secondaryColor = PortalCyan,
  ),
  CUSTOM_GATE(
    id = "custom",
    titleVi = "CỔNG TÙY CHỌN",
    shortVi = "Cổng Tùy Chọn",
    systemHeader = "[ CUSTOM DUNGEON • KIẾN TẠO THỬ THÁCH ]",
    systemRankBadge = "CUSTOM TRIAL",
    systemPromptVi = "Tùy chỉnh loại hình tác chiến: Thám Thính, Phòng Thủ Sinh Tồn, hoặc Khiêu Chiến Tự Do theo ý muốn của Master.",
    primaryColor = EmeraldVitality,
    secondaryColor = PortalCyan,
  ),
}

private const val GATE_CUTSCENE_DURATION_SEC = 6.0f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloorSelectionScreen(
  state: SummonerUiState,
  onSelectFloor: (TowerFloorSpec) -> Unit,
  onBackToLobby: () -> Unit,
  playEntryCutscene: Boolean = false,
) {
  BackHandler(onBack = onBackToLobby)

  val highestCleared = state.profile?.highestClearedFloor?.toInt() ?: 0
  val textMeasurer = rememberTextMeasurer()

  // 1. 6-Second Cinematic Tower Gate Opening Cutscene State
  var isPlayingCutscene by remember { mutableStateOf(playEntryCutscene) }
  var cutsceneTimeSec by remember { mutableFloatStateOf(0f) }

  // 2. Interactive 2.5D 3-Portal Dimensional Nexus Mini-Map State
  var animTimeSec by remember { mutableFloatStateOf(1.2f) }
  var selectedNexusGate by remember { mutableStateOf(DimensionalNexusGate.TOWER_GATE) }
  var activePortalTab by remember { mutableIntStateOf(0) }
  var showManhwaSystemWindow by remember { mutableStateOf(true) }
  var portalBurstTimer by remember { mutableFloatStateOf(0f) }

  // Avatar position on the 2.5D Nexus Mini-Map (normalized -1..+1 around plaza)
  var avatarNormX by remember { mutableFloatStateOf(0f) }
  var avatarNormY by remember { mutableFloatStateOf(0.42f) }
  var targetNormX by remember { mutableFloatStateOf(0f) }
  var targetNormY by remember { mutableFloatStateOf(-0.18f) }
  var avatarMoving by remember { mutableStateOf(false) }
  var avatarFacingRight by remember { mutableStateOf(true) }

  fun selectGateOnNexusMap(gate: DimensionalNexusGate) {
    selectedNexusGate = gate
    showManhwaSystemWindow = true
    portalBurstTimer = 1.0f
    when (gate) {
      DimensionalNexusGate.EVENT_GATE -> {
        targetNormX = -0.58f
        targetNormY = 0.04f
        activePortalTab = 3 // Focus Boss / Event floors first, while keeping all tabs selectable
      }
      DimensionalNexusGate.TOWER_GATE -> {
        targetNormX = 0f
        targetNormY = -0.22f
        activePortalTab = 0 // All main tower floors
      }
      DimensionalNexusGate.CUSTOM_GATE -> {
        targetNormX = 0.58f
        targetNormY = 0.04f
        if (activePortalTab == 0) activePortalTab = 1 // Highlight custom archetype filter
      }
    }
  }

  SceneFrameLoop(active = true, targetFps = 60) { dt, _ ->
    val clampedDt = dt.coerceAtMost(0.1f)
    animTimeSec += clampedDt
    if (portalBurstTimer > 0f) {
      portalBurstTimer = (portalBurstTimer - clampedDt * 1.4f).coerceAtLeast(0f)
    }
    if (isPlayingCutscene) {
      cutsceneTimeSec += clampedDt
      if (cutsceneTimeSec >= GATE_CUTSCENE_DURATION_SEC) {
        isPlayingCutscene = false
        portalBurstTimer = 1.0f
      }
    } else {
      val dx = targetNormX - avatarNormX
      val dy = targetNormY - avatarNormY
      val dist = hypot(dx, dy)
      if (dist > 0.015f) {
        avatarMoving = true
        if (abs(dx) > 0.01f) avatarFacingRight = dx > 0f
        val step = (clampedDt * 1.15f).coerceAtMost(dist)
        avatarNormX += (dx / dist) * step
        avatarNormY += (dy / dist) * step
      } else {
        avatarMoving = false
      }
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF071326)),
  ) {
    val isWide = maxWidth >= 620.dp
    val isShortLandscape = maxHeight < 440.dp

    // Background 2.5D Canvas: Either the 6-Second Tower Gate Opening Cutscene OR the 3-Portal Dimensional Nexus Mini-Map!
    if (isPlayingCutscene) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .testTag("tower_gate_cutscene_canvas")
          .pointerInput(Unit) {
            detectTapGestures {
              // Tapping canvas does not accidentally close; user can watch or press Skip button
            }
          },
      ) {
        drawTowerGateOpeningCutscene25D(
          cutsceneSec = cutsceneTimeSec,
          totalSec = GATE_CUTSCENE_DURATION_SEC,
          heroes = state.heroes,
          textMeasurer = textMeasurer,
        )
      }
    } else {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .testTag("dimensional_nexus_minimap_canvas")
          .pointerInput(isWide, showManhwaSystemWindow) {
            detectTapGestures { tapOffset ->
              val w = size.width.toFloat()
              val h = size.height.toFloat()
              val mapRegionH = if (showManhwaSystemWindow) {
                if (isShortLandscape) h * 0.44f else h * 0.40f
              } else {
                h * 0.85f
              }
              val centerX = w * 0.50f
              val centerY = mapRegionH * 0.56f
              val spanX = w * 0.33f
              val spanY = mapRegionH * 0.25f

              val eventPos = Offset(centerX - spanX, centerY + spanY * 0.18f)
              val towerPos = Offset(centerX, centerY - spanY * 0.65f)
              val customPos = Offset(centerX + spanX, centerY + spanY * 0.18f)
              val hitRadius = minOf(w, h) * 0.16f

              when {
                hypot(tapOffset.x - eventPos.x, tapOffset.y - eventPos.y) <= hitRadius ->
                  selectGateOnNexusMap(DimensionalNexusGate.EVENT_GATE)
                hypot(tapOffset.x - towerPos.x, tapOffset.y - towerPos.y) <= hitRadius * 1.15f ->
                  selectGateOnNexusMap(DimensionalNexusGate.TOWER_GATE)
                hypot(tapOffset.x - customPos.x, tapOffset.y - customPos.y) <= hitRadius ->
                  selectGateOnNexusMap(DimensionalNexusGate.CUSTOM_GATE)
              }
            }
          },
      ) {
        drawDimensionalNexusThreePortalsMap(
          timeSec = animTimeSec,
          selectedGate = selectedNexusGate,
          portalBurst = portalBurstTimer,
          avatarNormX = avatarNormX,
          avatarNormY = avatarNormY,
          avatarMoving = avatarMoving,
          avatarFacingRight = avatarFacingRight,
          compactMapRegion = showManhwaSystemWindow,
          isShortLandscape = isShortLandscape,
          heroes = state.heroes,
          textMeasurer = textMeasurer,
        )
      }
    }

    // Foreground HUD & Manhwa System Overlay
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 12.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // 1. Ornate Slim Header Bar (Always present so `back_to_lobby_button` is always accessible!)
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xCC0B1A30),
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 900.dp)
          .border(1.5.dp, PortalCyan.copy(alpha = 0.78f), RoundedCornerShape(14.dp)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          IconButton(
            onClick = onBackToLobby,
            modifier = Modifier.size(48.dp).testTag("back_to_lobby_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Quay lại Thánh Địa",
              tint = SummonerGold,
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isPlayingCutscene) {
                "CUTSCENE • ĐẠI MÔN THÁP VĨNH HẰNG ĐANG MỞ..."
              } else {
                "KHÔNG GIAN BA CỔNG • ${selectedNexusGate.titleVi}"
              },
              style = MaterialTheme.typography.titleSmall,
              color = SummonerGold,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = if (isPlayingCutscene) {
                "Phong ấn Cổ Ngữ thức tỉnh (${String.format("%.1f", (GATE_CUTSCENE_DURATION_SEC - cutsceneTimeSec).coerceAtLeast(0f))}s) • Chạm Bỏ Qua để vào ngay"
              } else {
                "Chạm 1 trong 3 Cổng trên Map (Sự Kiện • Tòa Tháp • Tùy Chọn) • Đã vượt: Tầng $highestCleared"
              },
              style = MaterialTheme.typography.labelSmall,
              color = PortalCyan,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (isPlayingCutscene) {
            Button(
              onClick = {
                isPlayingCutscene = false
                portalBurstTimer = 1.0f
              },
              modifier = Modifier.heightIn(min = 38.dp).testTag("skip_gate_cutscene_button"),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              shape = RoundedCornerShape(10.dp),
              colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
            ) {
              Text("Bỏ Qua ▸", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
          } else {
            // Replay 6s Gate Cutscene Button + Map Fullscreen Toggle
            Surface(
              shape = RoundedCornerShape(9.dp),
              color = Color(0xAD132846),
              modifier = Modifier
                .border(1.dp, SummonerGold.copy(alpha = 0.65f), RoundedCornerShape(9.dp))
                .testTag("replay_gate_cutscene_button")
                .clickable {
                  cutsceneTimeSec = 0f
                  isPlayingCutscene = true
                },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SummonerGold, modifier = Modifier.size(14.dp))
                Text("Mở Cổng", style = MaterialTheme.typography.labelSmall, color = SummonerGold, maxLines = 1)
              }
            }

            if (isWide) {
              Surface(
                shape = RoundedCornerShape(9.dp),
                color = Color(0xAD132846),
                modifier = Modifier
                  .border(1.dp, PortalCyan.copy(alpha = 0.65f), RoundedCornerShape(9.dp))
                  .clickable { showManhwaSystemWindow = !showManhwaSystemWindow },
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(Icons.Default.Map, contentDescription = null, tint = PortalCyan, modifier = Modifier.size(14.dp))
                  Text(
                    text = if (showManhwaSystemWindow) "Ngắm Map" else "Mở Hệ Thống",
                    style = MaterialTheme.typography.labelSmall,
                    color = PortalCyan,
                    maxLines = 1,
                  )
                }
              }
            }

            ResourcePillBadge(
              label = "Vàng",
              amount = state.profile?.gold ?: 0L,
              accent = SummonerGold,
            )
          }
        }
      }

      if (isPlayingCutscene) {
        // Bottom Cinematic Subtitle Banner during the 6-second Gate Opening Cutscene
        Spacer(modifier = Modifier.weight(1f))
        val phaseTitle = when {
          cutsceneTimeSec < 1.8f -> "[ PHASE I • KHẾ ƯỚC THỨC TỈNH TRƯỚC ĐẠI MÔN ]"
          cutsceneTimeSec < 4.5f -> "[ PHASE II • ĐẠI MÔN HẮC DIỆP THẠCH MỞ RA ]"
          else -> "[ PHASE III • KẾT NỐI KHÔNG GIAN BA CỔNG VĨNH HẰNG ]"
        }
        val phaseLine = when {
          cutsceneTimeSec < 1.8f -> "Đội hình Anh hùng tiến lên bậc thềm đá cẩm thạch. Tám cột ấn Cổ Ngữ đồng loạt thức tỉnh dưới chân Đại Môn..."
          cutsceneTimeSec < 4.5f -> "Hai cánh cổng Hắc Diệp Thạch khổng lồ rung chuyển rồi tách đôi! Luồng sáng Tinh Không và sấm sét dị giới tuôn trào!"
          else -> "Cổng Tháp Vĩnh Hằng đã mở hoàn toàn — Dịch chuyển đội hình vào Không Gian Ba Cổng (Sự Kiện • Tòa Tháp • Tùy Chọn)!"
        }
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xD408182E),
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .border(1.5.dp, PortalCyan.copy(alpha = 0.85f), RoundedCornerShape(16.dp)),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = phaseTitle,
                style = MaterialTheme.typography.labelMedium,
                color = PortalCyan,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${String.format("%.1f", cutsceneTimeSec.coerceAtMost(GATE_CUTSCENE_DURATION_SEC))}s / 6.0s",
                style = MaterialTheme.typography.labelSmall,
                color = SummonerGold,
              )
            }
            Text(
              text = phaseLine,
              style = MaterialTheme.typography.bodySmall,
              color = ParchmentLight,
            )
          }
        }
      } else {
        // 2. Interactive 3-Portal Selector Row overlaid over the 2.5D Nexus Mini-Map
        Spacer(modifier = Modifier.height(if (isShortLandscape) 44.dp else 92.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 900.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          DimensionalNexusGate.entries.forEach { gate ->
            val isGateSelected = selectedNexusGate == gate
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isGateSelected) Color(0xDC0D2342) else Color(0xB409182E),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 42.dp)
                .border(
                  width = if (isGateSelected) 1.8.dp else 1.dp,
                  color = if (isGateSelected) gate.primaryColor else gate.primaryColor.copy(alpha = 0.48f),
                  shape = RoundedCornerShape(12.dp),
                )
                .testTag("nexus_gate_${gate.id}")
                .clickable { selectGateOnNexusMap(gate) },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = gate.primaryColor,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = gate.shortVi,
                  style = MaterialTheme.typography.labelMedium,
                  color = if (isGateSelected) gate.primaryColor else ParchmentLight,
                  fontWeight = if (isGateSelected) FontWeight.ExtraBold else FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (!showManhwaSystemWindow) {
          Spacer(modifier = Modifier.weight(1f))
          Button(
            onClick = { showManhwaSystemWindow = true },
            modifier = Modifier.heightIn(min = 46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = FantasySkin.buttonColors(containerColor = PortalCyan, contentColor = ObsidianVoid),
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mở Cửa Sổ Hệ Thống (${selectedNexusGate.shortVi})", fontWeight = FontWeight.Bold)
          }
        } else {
          // 3. MANHWA "[ SYSTEM ]" HOLOGRAPHIC WINDOW
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xD808172E),
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 900.dp)
              .weight(1f)
              .drawWithContent {
                drawContent()
                // Manhwa Holographic Corner Brackets & Glowing System Frame
                val bracketLen = 24.dp.toPx()
                val strokeW = 2.5.dp.toPx()
                val pad = 3.dp.toPx()
                val w = size.width
                val h = size.height
                val primaryGlow = selectedNexusGate.primaryColor.copy(alpha = 0.92f)
                val cyanGlow = PortalCyan.copy(alpha = 0.90f)

                // Top-Left
                drawLine(cyanGlow, Offset(pad, pad), Offset(pad + bracketLen, pad), strokeW)
                drawLine(cyanGlow, Offset(pad, pad), Offset(pad, pad + bracketLen), strokeW)
                // Top-Right
                drawLine(primaryGlow, Offset(w - pad - bracketLen, pad), Offset(w - pad, pad), strokeW)
                drawLine(primaryGlow, Offset(w - pad, pad), Offset(w - pad, pad + bracketLen), strokeW)
                // Bottom-Left
                drawLine(primaryGlow, Offset(pad, h - pad - bracketLen), Offset(pad, h - pad), strokeW)
                drawLine(primaryGlow, Offset(pad, h - pad), Offset(pad + bracketLen, h - pad), strokeW)
                // Bottom-Right
                drawLine(cyanGlow, Offset(w - pad - bracketLen, h - pad), Offset(w - pad, h - pad), strokeW)
                drawLine(cyanGlow, Offset(w - pad, h - pad - bracketLen), Offset(w - pad, h - pad), strokeW)

                // Subtle moving holographic scanline
                val scanY = ((animTimeSec * 0.24f) % 1f) * h
                drawRoundRect(
                  brush = Brush.verticalGradient(
                    colors = listOf(
                      Color.Transparent,
                      selectedNexusGate.primaryColor.copy(alpha = 0.08f),
                      PortalCyan.copy(alpha = 0.09f),
                      Color.Transparent,
                    ),
                    startY = (scanY - 28f).coerceAtLeast(0f),
                    endY = (scanY + 28f).coerceAtMost(h),
                  ),
                  topLeft = Offset.Zero,
                  size = size,
                  cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                )
              }
              .border(1.5.dp, selectedNexusGate.primaryColor.copy(alpha = 0.78f), RoundedCornerShape(18.dp)),
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
              // Manhwa System Notification Banner
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color(0xA60E2342))
                  .border(1.dp, PortalCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = "[ ! ]",
                      style = MaterialTheme.typography.labelMedium,
                      color = selectedNexusGate.primaryColor,
                      fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                      text = selectedNexusGate.systemHeader,
                      style = MaterialTheme.typography.labelMedium,
                      color = PortalCyan,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  if (!isShortLandscape) {
                    Text(
                      text = selectedNexusGate.systemPromptVi,
                      style = MaterialTheme.typography.labelSmall,
                      color = ParchmentLight,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = selectedNexusGate.primaryColor.copy(alpha = 0.22f),
                  modifier = Modifier.border(1.dp, selectedNexusGate.primaryColor, RoundedCornerShape(6.dp)),
                ) {
                  Text(
                    text = selectedNexusGate.systemRankBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = selectedNexusGate.primaryColor,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    maxLines = 1,
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Multi-Portal Mission Archetype Filter Bar (preserves `portal_tab_0..3` testTags)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                listOf(
                  Triple("FLOORS 01–10", PortalCyan, "Tất Cả (10/100)"),
                  Triple("EXPLORATION", ArcaneViolet, "Thám Thính & Truy Tìm"),
                  Triple("DEFENSE & SURVIVAL", EmeraldVitality, "Phòng Thủ & Sinh Tồn"),
                  Triple("BOSS CONQUEST", SummonerGold, "Trảm Thủ Lĩnh & Sự Kiện"),
                ).forEachIndexed { idx, (portalName, accentColor, shortLabel) ->
                  val selected = activePortalTab == idx
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) accentColor.copy(alpha = 0.25f) else SanctuaryStone.copy(alpha = 0.88f),
                    modifier = Modifier
                      .heightIn(min = 40.dp)
                      .border(
                        width = if (selected) 1.8.dp else 1.dp,
                        color = if (selected) accentColor else BorderRune,
                        shape = RoundedCornerShape(10.dp),
                      )
                      .testTag("portal_tab_$idx")
                      .clickable { activePortalTab = idx },
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp),
                      )
                      Text(
                        text = "$portalName • $shortLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) accentColor else ParchmentLight,
                        maxLines = 1,
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              val filteredFloors = remember(activePortalTab) {
                when (activePortalTab) {
                  1 -> TowerFloorCatalog.FLOORS.filter { it.floorNumber in setOf(1, 2, 3, 6, 8) }
                  2 -> TowerFloorCatalog.FLOORS.filter { it.floorNumber in setOf(1, 4, 7, 9) }
                  3 -> TowerFloorCatalog.FLOORS.filter { it.floorNumber in setOf(1, 5, 10) }
                  else -> TowerFloorCatalog.FLOORS
                }
              }

              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                contentPadding = PaddingValues(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                items(filteredFloors, key = { it.floorNumber }) { floor ->
                  val isCleared = floor.floorNumber <= highestCleared
                  val isUnlocked = floor.floorNumber <= highestCleared + 1
                  val borderColor = when {
                    isCleared -> EmeraldVitality.copy(alpha = 0.88f)
                    isUnlocked -> selectedNexusGate.primaryColor.copy(alpha = 0.88f)
                    else -> BorderRune.copy(alpha = 0.45f)
                  }

                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isUnlocked) Color(0xCE10223C) else Color(0x880D1B30),
                    modifier = Modifier
                      .fillMaxWidth()
                      .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                      .testTag("floor_card_${floor.floorNumber}")
                      .clickable(enabled = isUnlocked) { onSelectFloor(floor) },
                  ) {
                    if (isWide) {
                      Row(modifier = Modifier.fillMaxWidth()) {
                        FloorBannerBox(
                          floor = floor,
                          isCleared = isCleared,
                          isUnlocked = isUnlocked,
                          borderColor = borderColor,
                          gateBadge = selectedNexusGate.shortVi,
                          modifier = Modifier.width(235.dp).height(175.dp),
                        )
                        FloorDetailsBody(
                          floor = floor,
                          isUnlocked = isUnlocked,
                          gateAccent = selectedNexusGate.primaryColor,
                          onSelectFloor = { onSelectFloor(floor) },
                          modifier = Modifier.weight(1f).padding(12.dp),
                        )
                      }
                    } else {
                      Column {
                        FloorBannerBox(
                          floor = floor,
                          isCleared = isCleared,
                          isUnlocked = isUnlocked,
                          borderColor = borderColor,
                          gateBadge = selectedNexusGate.shortVi,
                          modifier = Modifier.fillMaxWidth().height(128.dp),
                        )
                        FloorDetailsBody(
                          floor = floor,
                          isUnlocked = isUnlocked,
                          gateAccent = selectedNexusGate.primaryColor,
                          onSelectFloor = { onSelectFloor(floor) },
                          modifier = Modifier.padding(12.dp),
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * 6-Second 2.5D Cinematic Cutscene standing in front of the Northern Obsidian Split-Monolith Tower Gate.
 */
private fun DrawScope.drawTowerGateOpeningCutscene25D(
  cutsceneSec: Float,
  totalSec: Float,
  heroes: List<HeroRecord>,
  textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
  val w = size.width
  val h = size.height

  // Phase progressions
  val approachFrac = (cutsceneSec / 1.8f).coerceIn(0f, 1f)
  val gateOpenFrac = ((cutsceneSec - 1.5f) / 2.8f).coerceIn(0f, 1f)
  val climaxFrac = ((cutsceneSec - 4.3f) / (totalSec - 4.3f)).coerceIn(0f, 1f)

  // Subtle camera tremor while the ancient obsidian monoliths grind open
  val tremorAmplitude = if (cutsceneSec in 1.5f..4.6f) sin(cutsceneSec * 45f) * 4.5f * (1f - climaxFrac * 0.5f) else 0f
  val camCenterX = w * 0.50f + tremorAmplitude
  val gateBaseY = h * 0.44f + tremorAmplitude * 0.6f

  // 1. Deep Starlit Dark-Fantasy Sky & Foreshortened Sanctuary Courtyard Floor
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF061020),
        Color(0xFF0E213D),
        Color(0xFF173154),
        Color(0xFF10233E),
      ),
    ),
  )

  // Foreshortened 2.5D White-Marble & Obsidian Sanctuary Forecourt Terrace
  val plazaRx = w * 0.62f
  val plazaRy = h * 0.42f
  val plazaCenterY = h * 0.72f
  drawOval(
    brush = Brush.radialGradient(
      colors = listOf(
        Color(0xFF334C6E),
        Color(0xFF213654),
        Color(0xFF122138),
      ),
      center = Offset(camCenterX, plazaCenterY),
      radius = maxOf(plazaRx, plazaRy),
    ),
    topLeft = Offset(camCenterX - plazaRx, plazaCenterY - plazaRy),
    size = Size(plazaRx * 2f, plazaRy * 2f),
  )

  // 3 Stepped Ceremonial Marble Stairs leading up to the Obsidian Gate
  for (step in 0..3) {
    val stepY = gateBaseY + 24f + step * 22f
    val stepW = w * (0.34f + step * 0.06f)
    val stepH = 18f
    drawRoundRect(
      color = Color(0xFF3E577A).copy(alpha = 0.88f),
      topLeft = Offset(camCenterX - stepW / 2f, stepY),
      size = Size(stepW, stepH),
      cornerRadius = CornerRadius(6f, 6f),
    )
    drawRoundRect(
      color = SummonerGold.copy(alpha = 0.45f + 0.35f * gateOpenFrac),
      topLeft = Offset(camCenterX - stepW / 2f, stepY),
      size = Size(stepW, stepH),
      cornerRadius = CornerRadius(6f, 6f),
      style = Stroke(width = 1.4f),
    )
  }

  // 2. Forecourt 8-Node Astral Awakening Seal (Ignites during Phase I)
  val sealCenterY = h * 0.67f
  val sealRx = minOf(w, h) * 0.34f
  val sealRy = sealRx * 0.44f
  drawOval(
    color = PortalCyan.copy(alpha = 0.35f + 0.45f * approachFrac),
    topLeft = Offset(camCenterX - sealRx, sealCenterY - sealRy),
    size = Size(sealRx * 2f, sealRy * 2f),
    style = Stroke(width = 2.6f),
  )
  drawOval(
    color = SummonerGold.copy(alpha = 0.30f + 0.55f * gateOpenFrac),
    topLeft = Offset(camCenterX - sealRx * 0.74f, sealCenterY - sealRy * 0.74f),
    size = Size(sealRx * 1.48f, sealRy * 1.48f),
    style = Stroke(width = 2.0f),
  )

  for (nodeIdx in 0 until 8) {
    val nodeTrigger = (approachFrac * 8f - nodeIdx).coerceIn(0f, 1f)
    if (nodeTrigger > 0f) {
      val angle = cutsceneSec * 0.45f + (nodeIdx * 2.0 * PI / 8.0).toFloat()
      val nx = camCenterX + cos(angle) * sealRx
      val ny = sealCenterY + sin(angle) * sealRy
      val nodeCol = if (nodeIdx % 2 == 0) SummonerGold else PortalCyan
      // Vertical pillar of rune light
      drawLine(
        brush = Brush.verticalGradient(
          colors = listOf(Color.Transparent, nodeCol.copy(alpha = 0.55f * nodeTrigger), nodeCol),
          startY = ny - 95f * nodeTrigger,
          endY = ny,
        ),
        start = Offset(nx, ny - 95f * nodeTrigger),
        end = Offset(nx, ny),
        strokeWidth = 4.5f,
        cap = StrokeCap.Round,
      )
      drawCircle(nodeCol.copy(alpha = 0.85f * nodeTrigger), radius = 6.5f, center = Offset(nx, ny))
    }
  }

  // 3. Colossal Obsidian Split-Monolith Gate & Sliding Doors!
  val gateTotalW = minOf(w * 0.52f, 420f)
  val gateHeight = minOf(h * 0.42f, 260f)
  val gateTopY = gateBaseY - gateHeight
  val cleftMaxOpenPx = gateTotalW * 0.24f
  val currentOpenPx = cleftMaxOpenPx * gateOpenFrac

  // Blazing Dimensional Rift Core inside the parting doors
  if (gateOpenFrac > 0.02f) {
    val riftW = (currentOpenPx * 2f + 16f).coerceAtLeast(12f)
    drawRoundRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color.White,
          PortalCyan,
          SummonerGold,
          ArcaneViolet,
        ),
        startY = gateTopY,
        endY = gateBaseY + 20f,
      ),
      topLeft = Offset(camCenterX - riftW / 2f, gateTopY + 12f),
      size = Size(riftW, gateHeight - 8f),
      cornerRadius = CornerRadius(14f, 14f),
    )

    // Volumetric God-Rays pouring out of the opening Gate across the forecourt floor!
    val rayCount = 9
    for (r in 0 until rayCount) {
      val fanFrac = (r.toFloat() / (rayCount - 1)) - 0.5f
      val rayEndSpreadX = camCenterX + fanFrac * w * 0.88f * (0.4f + 0.6f * gateOpenFrac)
      val rayEndY = h * 0.98f
      val rayPath = Path().apply {
        moveTo(camCenterX - currentOpenPx * 0.6f, gateBaseY)
        lineTo(camCenterX + currentOpenPx * 0.6f, gateBaseY)
        lineTo(rayEndSpreadX + 42f, rayEndY)
        lineTo(rayEndSpreadX - 42f, rayEndY)
        close()
      }
      val rayCol = if (r % 2 == 0) SummonerGold else PortalCyan
      drawPath(
        path = rayPath,
        brush = Brush.verticalGradient(
          colors = listOf(
            rayCol.copy(alpha = 0.28f * gateOpenFrac),
            rayCol.copy(alpha = 0.10f * gateOpenFrac),
            Color.Transparent,
          ),
          startY = gateBaseY,
          endY = rayEndY,
        ),
      )
    }
  }

  // Left & Right Sliding Obsidian Gate Doors (with glowing carved runes)
  val doorSlabW = gateTotalW * 0.23f
  val leftDoorX = camCenterX - doorSlabW - currentOpenPx
  val rightDoorX = camCenterX + currentOpenPx

  listOf(leftDoorX, rightDoorX).forEachIndexed { sideIdx, doorX ->
    drawRoundRect(
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A273D), Color(0xFF0E1726), Color(0xFF1F304D)),
      ),
      topLeft = Offset(doorX, gateTopY + 18f),
      size = Size(doorSlabW, gateHeight - 16f),
      cornerRadius = CornerRadius(6f, 6f),
    )
    drawRoundRect(
      color = SummonerGold.copy(alpha = 0.78f),
      topLeft = Offset(doorX, gateTopY + 18f),
      size = Size(doorSlabW, gateHeight - 16f),
      cornerRadius = CornerRadius(6f, 6f),
      style = Stroke(width = 2.2f),
    )
    // Glowing horizontal rune bars on the sliding doors
    for (bar in 1..4) {
      val by = gateTopY + 24f + bar * ((gateHeight - 48f) / 5f)
      drawLine(
        color = PortalCyan.copy(alpha = 0.45f + 0.50f * gateOpenFrac),
        start = Offset(doorX + 10f, by),
        end = Offset(doorX + doorSlabW - 10f, by),
        strokeWidth = 2.6f,
        cap = StrokeCap.Round,
      )
    }
  }

  // Outer Colossal Obsidian Monolith Pillars flanking the Gate
  val pillarW = gateTotalW * 0.16f
  val leftPillarX = camCenterX - gateTotalW * 0.50f
  val rightPillarX = camCenterX + gateTotalW * 0.50f - pillarW
  listOf(leftPillarX, rightPillarX).forEach { px ->
    drawRoundRect(
      color = Color(0xFF142036),
      topLeft = Offset(px, gateTopY - 16f),
      size = Size(pillarW, gateHeight + 24f),
      cornerRadius = CornerRadius(8f, 8f),
    )
    drawRoundRect(
      color = PortalCyan.copy(alpha = 0.82f),
      topLeft = Offset(px, gateTopY - 16f),
      size = Size(pillarW, gateHeight + 24f),
      cornerRadius = CornerRadius(8f, 8f),
      style = Stroke(width = 2.4f),
    )
  }

  // Crackling Lightning Bolts & Expanding Shockwave Rings during Phase II & III
  if (gateOpenFrac > 0.15f) {
    for (ring in 0..2) {
      val ringPhase = ((cutsceneSec * 0.9f + ring * 0.33f) % 1f)
      val rx = sealRx * (0.4f + ringPhase * 1.15f)
      val ry = sealRy * (0.4f + ringPhase * 1.15f)
      drawOval(
        color = (if (ring % 2 == 0) PortalCyan else SummonerGold).copy(alpha = (1f - ringPhase) * 0.65f * gateOpenFrac),
        topLeft = Offset(camCenterX - rx, gateBaseY - ry * 0.5f),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = 2.8f),
      )
    }
  }

  // 4. Active Hero Squad Standing in 2.5D Formation Before the Opening Gate
  val squad = heroes.filter { it.inActiveSquad }.ifEmpty { heroes }.take(4)
  val spriteZoom = (minOf(w, h) / 480f).coerceIn(0.62f, 0.96f)
  val walkOffsetY = (1f - approachFrac) * 56f
  val heroCenterScreenY = sealCenterY + walkOffsetY

  val slotOffsets = listOf(
    Offset(-105f, 18f),
    Offset(-36f, 4f),
    Offset(36f, 4f),
    Offset(105f, 18f),
  )
  squad.forEachIndexed { idx, hero ->
    val slot = slotOffsets[idx % slotOffsets.size]
    val hx = 1000f + slot.x
    val hy = 1000f + slot.y
    drawCharacterSprite(
      worldX = hx,
      worldY = hy,
      camX = 1000f - (camCenterX - w / 2f) / spriteZoom,
      camY = 1000f - (heroCenterScreenY - h / 2f) / (spriteZoom * 0.72f),
      zoom = spriteZoom,
      isMaster = idx == 1,
      heroClass = hero.classType,
      facingRight = idx < 2,
      isMoving = approachFrac < 0.95f,
      attackAnimProgress = if (climaxFrac > 0.1f) ((cutsceneSec * 1.8f) % 1f) else 0f,
      hurtFlash = 0f,
      hpFraction = 1f,
      shieldOrAuraColor = if (gateOpenFrac > 0.3f) SummonerGold.copy(alpha = 0.80f) else PortalCyan.copy(alpha = 0.55f),
      isInjuredResting = false,
      timeSec = cutsceneSec,
      facingNorth = true,
      heroName = hero.name,
    )
  }

  // 5. Phase III Portal Transition Flash
  if (climaxFrac > 0.25f) {
    val flashAlpha = ((climaxFrac - 0.25f) / 0.75f).coerceIn(0f, 0.55f)
    drawRect(
      brush = Brush.radialGradient(
        colors = listOf(
          Color.White.copy(alpha = flashAlpha),
          PortalCyan.copy(alpha = flashAlpha * 0.75f),
          Color.Transparent,
        ),
        center = Offset(camCenterX, gateBaseY - gateHeight * 0.4f),
        radius = maxOf(w, h) * 0.85f,
      ),
    )
  }
}

/**
 * Interactive 2.5D Oblique 3-Portal Dimensional Nexus Mini-Map with HD-2D Shader Lighting:
 * - West Portal: CỔNG SỰ KIỆN (Crimson-Violet Rift)
 * - Center-North Portal: CỔNG TÒA THÁP (Golden-Cyan Eternal Spire)
 * - East Portal: CỔNG TÙY CHỌN (Emerald-Sapphire Astrolabe Trial Gate)
 */
private fun DrawScope.drawDimensionalNexusThreePortalsMap(
  timeSec: Float,
  selectedGate: DimensionalNexusGate,
  portalBurst: Float,
  avatarNormX: Float,
  avatarNormY: Float,
  avatarMoving: Boolean,
  avatarFacingRight: Boolean,
  compactMapRegion: Boolean,
  isShortLandscape: Boolean,
  heroes: List<HeroRecord>,
  textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
  val w = size.width
  val h = size.height
  val mapH = if (compactMapRegion) {
    if (isShortLandscape) h * 0.46f else h * 0.42f
  } else {
    h * 0.88f
  }

  // 1. Deep Starfield & Astral Nebula Sky Backdrop
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF061122),
        Color(0xFF0C1E3A),
        Color(0xFF102748),
        Color(0xFF081528),
      ),
    ),
  )

  val centerX = w * 0.50f
  val centerY = mapH * 0.58f
  val spanX = w * 0.33f
  val spanY = mapH * 0.25f

  val eventGateCenter = Offset(centerX - spanX, centerY + spanY * 0.16f)
  val towerGateCenter = Offset(centerX, centerY - spanY * 0.62f)
  val customGateCenter = Offset(centerX + spanX, centerY + spanY * 0.16f)
  val hubPlazaCenter = Offset(centerX, centerY + spanY * 0.54f)

  // 2. Volumetric Sun & Astral God-Rays Sweeping Down onto the Floating 3-Portal Island
  for (ray in 0 until 6) {
    val sway = sin(timeSec * 0.5f + ray * 1.1f) * 28f
    val rx = w * (0.18f + ray * 0.13f) + sway
    val rayPath = Path().apply {
      moveTo(rx - 22f, 0f)
      lineTo(rx + 22f, 0f)
      lineTo(rx + 120f, mapH * 1.15f)
      lineTo(rx - 60f, mapH * 1.15f)
      close()
    }
    val rayCol = when (ray % 3) {
      0 -> SummonerGold
      1 -> PortalCyan
      else -> selectedGate.primaryColor
    }
    drawPath(
      path = rayPath,
      brush = Brush.verticalGradient(
        colors = listOf(
          rayCol.copy(alpha = 0.16f),
          rayCol.copy(alpha = 0.06f),
          Color.Transparent,
        ),
        startY = 0f,
        endY = mapH * 1.1f,
      ),
    )
  }

  // 3. 2.5D Oblique Floating Sanctuary Island Base & Starlit Rune Bridges
  val islandRx = w * 0.44f
  val islandRy = mapH * 0.36f
  // Deep floating island cliff underside shadow
  drawOval(
    color = Color(0xAA040B16),
    topLeft = Offset(centerX - islandRx * 0.96f, centerY - islandRy * 0.72f + 18f),
    size = Size(islandRx * 1.92f, islandRy * 1.68f),
  )
  // Floating Marble & Obsidian Nexus Terrace
  drawOval(
    brush = Brush.radialGradient(
      colors = listOf(
        Color(0xFF2B4568),
        Color(0xFF1B304E),
        Color(0xFF102038),
      ),
      center = Offset(centerX, centerY),
      radius = maxOf(islandRx, islandRy),
    ),
    topLeft = Offset(centerX - islandRx, centerY - islandRy * 0.80f),
    size = Size(islandRx * 2f, islandRy * 1.65f),
  )
  drawOval(
    color = SummonerGold.copy(alpha = 0.55f),
    topLeft = Offset(centerX - islandRx, centerY - islandRy * 0.80f),
    size = Size(islandRx * 2f, islandRy * 1.65f),
    style = Stroke(width = 2.0f),
  )

  // Three Glowing Rune Bridges connecting Central Arrival Hub to the 3 Portals
  val portals = listOf(
    DimensionalNexusGate.EVENT_GATE to eventGateCenter,
    DimensionalNexusGate.TOWER_GATE to towerGateCenter,
    DimensionalNexusGate.CUSTOM_GATE to customGateCenter,
  )
  portals.forEach { (gate, gatePos) ->
    val isSel = selectedGate == gate
    drawLine(
      color = Color(0xFF39547A),
      start = hubPlazaCenter,
      end = gatePos,
      strokeWidth = if (isSel) 22f else 16f,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = gate.primaryColor.copy(alpha = if (isSel) 0.85f else 0.42f),
      start = hubPlazaCenter,
      end = gatePos,
      strokeWidth = if (isSel) 5.5f else 3.0f,
      cap = StrokeCap.Round,
    )
  }

  // Central Arrival Astral Circle
  val hubRx = minOf(w, mapH) * 0.13f
  val hubRy = hubRx * 0.46f
  drawOval(
    color = PortalCyan.copy(alpha = 0.55f),
    topLeft = Offset(hubPlazaCenter.x - hubRx, hubPlazaCenter.y - hubRy),
    size = Size(hubRx * 2f, hubRy * 2f),
    style = Stroke(width = 2.0f),
  )

  // 4. Render the 3 Monumental 2.5D Dimensional Portals with Volumetric Beams & Magic Circles
  portals.forEach { (gate, pos) ->
    val isSel = selectedGate == gate
    val pulse = 1f + 0.08f * sin(timeSec * 2.6f + gate.ordinal)
    val portalR = minOf(w, mapH) * (if (gate == DimensionalNexusGate.TOWER_GATE) 0.15f else 0.125f) * pulse
    val floorRx = portalR * 1.25f
    val floorRy = floorRx * 0.45f

    // Vertical Volumetric Portal Pillar of Light
    val beamW = portalR * (if (isSel) 1.35f else 0.85f)
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color.Transparent,
          gate.primaryColor.copy(alpha = if (isSel) 0.34f else 0.14f),
          gate.secondaryColor.copy(alpha = if (isSel) 0.52f else 0.22f),
        ),
        startY = (pos.y - portalR * 2.8f).coerceAtLeast(0f),
        endY = pos.y,
      ),
      topLeft = Offset(pos.x - beamW / 2f, (pos.y - portalR * 2.8f).coerceAtLeast(0f)),
      size = Size(beamW, (pos.y - (pos.y - portalR * 2.8f).coerceAtLeast(0f))),
    )

    // 2.5D Foreshortened Floor Magic Circle under the Portal
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(
          gate.primaryColor.copy(alpha = if (isSel) 0.68f else 0.36f),
          gate.secondaryColor.copy(alpha = 0.18f),
          Color.Transparent,
        ),
        center = pos,
        radius = floorRx * 1.2f,
      ),
      topLeft = Offset(pos.x - floorRx, pos.y - floorRy),
      size = Size(floorRx * 2f, floorRy * 2f),
    )
    drawOval(
      color = gate.primaryColor.copy(alpha = if (isSel) 0.95f else 0.58f),
      topLeft = Offset(pos.x - floorRx, pos.y - floorRy),
      size = Size(floorRx * 2f, floorRy * 2f),
      style = Stroke(width = if (isSel) 2.8f else 1.6f),
    )

    // Upright 2.5D Dimensional Gate Archway & Swirling Vortex Core
    val archW = portalR * 1.28f
    val archH = portalR * 1.62f
    val archTopY = pos.y - archH * 0.88f

    // Swirling Portal Vortex Oval
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(
          Color.White.copy(alpha = if (isSel) 0.95f else 0.72f),
          gate.primaryColor.copy(alpha = 0.88f),
          gate.secondaryColor.copy(alpha = 0.65f),
          Color(0xFF091526),
        ),
        center = Offset(pos.x, archTopY + archH * 0.5f),
        radius = archH * 0.55f,
      ),
      topLeft = Offset(pos.x - archW * 0.42f, archTopY + archH * 0.10f),
      size = Size(archW * 0.84f, archH * 0.82f),
    )

    // Twin Stone Pillars flanking each Portal
    val pillarW = archW * 0.16f
    drawRoundRect(
      color = Color(0xFF182944),
      topLeft = Offset(pos.x - archW * 0.52f, archTopY),
      size = Size(pillarW, archH),
      cornerRadius = CornerRadius(5f, 5f),
    )
    drawRoundRect(
      color = Color(0xFF182944),
      topLeft = Offset(pos.x + archW * 0.52f - pillarW, archTopY),
      size = Size(pillarW, archH),
      cornerRadius = CornerRadius(5f, 5f),
    )
    drawOval(
      color = gate.primaryColor,
      topLeft = Offset(pos.x - archW * 0.46f, archTopY + archH * 0.06f),
      size = Size(archW * 0.92f, archH * 0.88f),
      style = Stroke(width = if (isSel) 3.2f else 2.0f),
    )

    // Burst shockwave when tapped
    if (isSel && portalBurst > 0.01f) {
      val burstR = floorRx * (1f + (1f - portalBurst) * 1.4f)
      drawOval(
        color = gate.primaryColor.copy(alpha = portalBurst * 0.85f),
        topLeft = Offset(pos.x - burstR, pos.y - burstR * 0.45f),
        size = Size(burstR * 2f, burstR * 0.90f),
        style = Stroke(width = 3.2f),
      )
    }

    // Floating 2.5D Portal Label Plaque
    val plaqueMeasured = textMeasurer.measure(
      text = if (isSel) "✦ ${gate.titleVi} ✦" else gate.titleVi,
      style = TextStyle(
        color = if (isSel) gate.primaryColor else ParchmentLight,
        fontSize = if (isShortLandscape) 8.5.sp else 10.sp,
        fontWeight = FontWeight.ExtraBold,
      ),
    )
    val pW = plaqueMeasured.size.width + 14f
    val pH = plaqueMeasured.size.height + 5f
    val pTopLeft = Offset(pos.x - pW / 2f, (archTopY - pH - 4f).coerceAtLeast(52f))
    drawRoundRect(
      color = Color(0xCC09182E),
      topLeft = pTopLeft,
      size = Size(pW, pH),
      cornerRadius = CornerRadius(7f, 7f),
    )
    drawRoundRect(
      color = gate.primaryColor.copy(alpha = if (isSel) 0.95f else 0.65f),
      topLeft = pTopLeft,
      size = Size(pW, pH),
      cornerRadius = CornerRadius(7f, 7f),
      style = Stroke(width = if (isSel) 1.8f else 1.1f),
    )
    drawText(plaqueMeasured, topLeft = Offset(pTopLeft.x + 7f, pTopLeft.y + 2.5f))
  }

  // 5. Player's Animated HD-128 Squad Leader Avatar on the 3-Portal Mini-Map
  val leadHero = heroes.firstOrNull { it.inActiveSquad } ?: heroes.firstOrNull()
  val avatarScreenX = centerX + avatarNormX * spanX
  val avatarScreenY = centerY + avatarNormY * spanY
  val avatarZoom = (minOf(w, mapH) / 420f).coerceIn(0.46f, 0.78f)

  drawCharacterSprite(
    worldX = 1000f,
    worldY = 1000f,
    camX = 1000f - (avatarScreenX - w / 2f) / avatarZoom,
    camY = 1000f - (avatarScreenY - h / 2f) / (avatarZoom * 0.72f),
    zoom = avatarZoom,
    isMaster = true,
    heroClass = leadHero?.classType ?: HeroClassType.MAGE,
    facingRight = avatarFacingRight,
    isMoving = avatarMoving,
    attackAnimProgress = 0f,
    hurtFlash = 0f,
    hpFraction = 1f,
    shieldOrAuraColor = selectedGate.primaryColor.copy(alpha = 0.80f),
    isInjuredResting = false,
    timeSec = timeSec,
    facingNorth = true,
    heroName = leadHero?.name ?: "Master",
  )

  // 6. Floating Astral Mana Motes
  for (m in 0 until 32) {
    val seed = m * 41.7f
    val mx = ((seed * 53.1f + sin(timeSec * 0.8f + m) * 20f) % w + w) % w
    val my = ((seed * 29.7f - timeSec * 16f) % mapH + mapH) % mapH
    val mCol = when (m % 3) {
      0 -> SummonerGold
      1 -> PortalCyan
      else -> selectedGate.primaryColor
    }
    drawCircle(
      color = mCol.copy(alpha = 0.55f),
      radius = 2.2f + (m % 3) * 0.8f,
      center = Offset(mx, my),
    )
  }
}

@Composable
private fun FloorBannerBox(
  floor: TowerFloorSpec,
  isCleared: Boolean,
  isUnlocked: Boolean,
  borderColor: Color,
  gateBadge: String,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    Image(
      painter = painterResource(id = floor.bannerRes),
      contentDescription = floor.titleVi,
      contentScale = ContentScale.Crop,
      alpha = if (isUnlocked) 0.94f else 0.40f,
      modifier = Modifier.fillMaxSize(),
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(Color(0x100B1628), Color(0x740C172B))
          )
        ),
    )

    Surface(
      shape = RoundedCornerShape(8.dp),
      color = Color(0xB80E203A),
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(8.dp)
        .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = when {
            isCleared -> Icons.Default.CheckCircle
            isUnlocked -> Icons.Default.AutoAwesome
            else -> Icons.Default.Lock
          },
          contentDescription = null,
          tint = when {
            isCleared -> EmeraldVitality
            isUnlocked -> SummonerGold
            else -> RuneSilver
          },
          modifier = Modifier.size(13.dp),
        )
        Text(
          text = when {
            isCleared -> "ĐÃ CHINH PHỤC"
            isUnlocked -> "SẴN SÀNG"
            else -> "PHONG ẤN"
          },
          style = MaterialTheme.typography.labelSmall,
          color = ParchmentLight,
          maxLines = 1,
        )
      }
    }

    Surface(
      shape = RoundedCornerShape(8.dp),
      color = Color(0xB80E203A),
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(8.dp)
        .border(1.dp, PortalCyan.copy(alpha = 0.75f), RoundedCornerShape(8.dp)),
    ) {
      Text(
        text = "[ $gateBadge ] • ${floor.missionTypeVi}",
        style = MaterialTheme.typography.labelSmall,
        color = PortalCyan,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      )
    }

    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(10.dp),
    ) {
      Text(
        text = "[ SYSTEM QUEST • ${floor.codeName} ]",
        style = MaterialTheme.typography.labelSmall,
        color = PortalCyan,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = floor.titleVi,
        style = MaterialTheme.typography.titleMedium,
        color = SummonerGold,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FloorDetailsBody(
  floor: TowerFloorSpec,
  isUnlocked: Boolean,
  gateAccent: Color,
  onSelectFloor: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = floor.descriptionVi,
      style = MaterialTheme.typography.bodySmall,
      color = ParchmentLight,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )

    Text(
      text = "[ MỤC TIÊU HỆ THỐNG ]: ${floor.objectiveVi}",
      style = MaterialTheme.typography.labelMedium,
      color = WarningAmber,
      fontWeight = FontWeight.Bold,
    )

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      floor.knownMonstersVi.forEach { monsterName ->
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color(0x981A2C48),
          modifier = Modifier.border(1.dp, CrimsonBlood.copy(alpha = 0.55f), RoundedCornerShape(6.dp)),
        ) {
          Text(
            text = "⚔ $monsterName",
            style = MaterialTheme.typography.labelSmall,
            color = ParchmentLight,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
          )
        }
      }
    }

    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column {
        Text(
          text = "Thưởng: +${floor.rewardGold} Vàng • +${floor.rewardMana} Mana • +${floor.rewardShards} Hồn",
          style = MaterialTheme.typography.labelMedium,
          color = EmeraldVitality,
        )
        Text(
          text = "Lực chiến đề xuất: ${floor.recommendedPower}",
          style = MaterialTheme.typography.labelSmall,
          color = RuneSilver,
        )
      }

      Button(
        onClick = onSelectFloor,
        enabled = isUnlocked,
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("enter_floor_${floor.floorNumber}_button"),
        shape = RoundedCornerShape(10.dp),
        colors = FantasySkin.buttonColors(
          containerColor = if (isUnlocked) gateAccent else BorderRune,
          contentColor = ObsidianVoid,
        ),
      ) {
        Text(
          text = if (isUnlocked) "Chuẩn Bị Đội ▸" else "Cần Tầng ${floor.floorNumber - 1}",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
fun SquadPreparationScreen(
  state: SummonerUiState,
  onToggleHero: (HeroRecord) -> Unit,
  onOpenFacility: (FacilityType) -> Unit,
  onCloseFacility: () -> Unit,
  onSummonHero: () -> Unit,
  onTrainHero: (HeroRecord, Boolean) -> Unit,
  onEquipItem: (HeroRecord, EquipmentRecord) -> Unit,
  onSynthesizeGear: () -> Unit,
  onEnhanceGear: (EquipmentRecord) -> Unit,
  onRecoverHero: (HeroRecord, Boolean) -> Unit,
  onHostMeal: () -> Unit,
  onDeploy: () -> Unit,
  onBack: () -> Unit,
  onTrainHeroWithFocus: (HeroRecord, TrainingFocus) -> Unit = { h, f -> onTrainHero(h, f.isSkillUpgrade) },
  onUnequipItem: (HeroRecord, String) -> Unit = { _, _ -> },
  onAssignHeroActivity: (HeroRecord, HeroStatusType) -> Unit = { _, _ -> },
  onHostMealWithRecipe: (SanctuaryMealRecipe) -> Unit = { _ -> onHostMeal() },
) {
  BackHandler {
    if (state.activeFacilityModal != null) {
      onCloseFacility()
    } else {
      onBack()
    }
  }

  val floor = state.selectedFloor
  val nowSec = System.currentTimeMillis() / 1000L
  val activeSquad = state.heroes.filter { it.inActiveSquad && !it.isCurrentlyInjured(nowSec) }
  val squadPower = activeSquad.sumOf { h ->
    h.totalAttack(state.equipment) * 3 + h.totalDefense(state.equipment) * 2 + h.totalMaxHp(state.equipment) / 4
  }
  val storyFlags = state.profile?.storyFlags.orEmpty()
  val activeMealRecipe = FeastBuffHelper.activeRecipe(storyFlags, nowSec)
  val dockBonusActive = SanctuaryProgressionHelper.isExpansionUnlocked(storyFlags, IslandExpansionMilestone.NORTH_ASTRAL_DOCK)

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF102038)),
  ) {
    val isWide = maxWidth >= 620.dp

    Image(
      painter = painterResource(id = floor.bannerRes),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      alpha = 0.76f,
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 14.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Compact Responsive Top Bar (Zero text wrapping on 360dp phones!)
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xC4132238),
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 920.dp)
          .border(1.5.dp, SummonerGold.copy(alpha = 0.72f), RoundedCornerShape(14.dp)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp).testTag("back_to_floors_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Quay lại chọn tầng",
              tint = SummonerGold,
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "XUẤT CHINH • ${floor.titleVi}",
              style = MaterialTheme.typography.titleSmall,
              color = SummonerGold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Tối đa 4 Anh hùng • Chạm để thêm/rút",
              style = MaterialTheme.typography.labelSmall,
              color = RuneSilver,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          OutlinedButton(
            onClick = { onOpenFacility(FacilityType.ARMORY) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("prep_open_armory_button"),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
          ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = PortalCyan, modifier = Modifier.size(15.dp))
            if (isWide) {
              Spacer(modifier = Modifier.width(5.dp))
              Text("Kho Vũ Khí", color = ParchmentLight, maxLines = 1)
            }
          }

          OutlinedButton(
            onClick = { onOpenFacility(FacilityType.HERO_QUARTERS) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("prep_open_quarters_button"),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
          ) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = EmeraldVitality, modifier = Modifier.size(15.dp))
            if (isWide) {
              Spacer(modifier = Modifier.width(5.dp))
              Text("Trị Thương", color = ParchmentLight, maxLines = 1)
            }
          }
        }
      }

      if (state.bannerFeedback != null) {
        Spacer(modifier = Modifier.height(6.dp))
        ESBannerNotice(text = state.bannerFeedback!!, priority = NoticePriority.TACTICAL)
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (isWide) {
        // Widescreen Landscape: Canonical 2-Pane Tactical Briefing + Hero Roster!
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 920.dp)
            .weight(1f),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = FantasySkin.Panel.copy(alpha = 0.93f),
            modifier = Modifier
              .weight(0.38f)
              .fillMaxHeight()
              .border(1.5.dp, ArcaneViolet.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Image(
                  painter = painterResource(id = floor.bannerRes),
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(10.dp)),
                )
                Text(
                  text = "ĐỘI HÌNH (${activeSquad.size} / 4)",
                  style = MaterialTheme.typography.labelLarge,
                  color = PortalCyan,
                )
                val powerColor = if (squadPower >= floor.recommendedPower) EmeraldVitality else WarningAmber
                Text(
                  text = "Lực Chiến: $squadPower / ${floor.recommendedPower}",
                  style = MaterialTheme.typography.titleSmall,
                  color = powerColor,
                )
                Text(
                  text = if (activeSquad.isEmpty()) {
                    "Hãy chọn ít nhất 1 Anh hùng khỏe mạnh ở danh sách bên phải."
                  } else {
                    activeSquad.joinToString(" • ") { it.name }
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = ParchmentLight,
                )
                if (activeMealRecipe != null) {
                  Text(
                    text = "★ Buff Bữa Ăn: ${activeMealRecipe.titleVi}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldVitality,
                  )
                }
                if (dockBonusActive) {
                  Text(
                    text = "✦ Bến Phi Thuyền: +20% Vàng & Mana xuất chinh",
                    style = MaterialTheme.typography.labelSmall,
                    color = SummonerGold,
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Button(
                onClick = onDeploy,
                enabled = activeSquad.isNotEmpty(),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 50.dp)
                  .testTag("deploy_tower_button"),
                shape = RoundedCornerShape(12.dp),
                colors = FantasySkin.buttonColors(
                  containerColor = SummonerGold,
                  contentColor = ObsidianVoid,
                ),
              ) {
                Text(
                  text = "XUẤT CHINH VÀO THÁP ⚔",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                )
              }
            }
          }

          SquadHeroRosterList(
            state = state,
            nowSec = nowSec,
            onToggleHero = onToggleHero,
            modifier = Modifier.weight(0.62f).fillMaxHeight(),
          )
        }
      } else {
        // Narrow Portrait: Stacked Briefing Card + Weighted Roster List!
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = FantasySkin.Panel.copy(alpha = 0.93f),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, ArcaneViolet.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "ĐỘI HÌNH (${activeSquad.size} / 4)",
                style = MaterialTheme.typography.labelLarge,
                color = PortalCyan,
              )
              val powerColor = if (squadPower >= floor.recommendedPower) EmeraldVitality else WarningAmber
              Text(
                text = "Lực chiến: $squadPower / ${floor.recommendedPower}",
                style = MaterialTheme.typography.labelLarge,
                color = powerColor,
              )
            }

            Button(
              onClick = onDeploy,
              enabled = activeSquad.isNotEmpty(),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("deploy_tower_button"),
              shape = RoundedCornerShape(12.dp),
              colors = FantasySkin.buttonColors(
                containerColor = SummonerGold,
                contentColor = ObsidianVoid,
              ),
            ) {
              Text(
                text = "XUẤT CHINH VÀO THÁP ⚔",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SquadHeroRosterList(
          state = state,
          nowSec = nowSec,
          onToggleHero = onToggleHero,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
      }
    }

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
        onToggleSquad = onToggleHero,
        onTrainHeroWithFocus = onTrainHeroWithFocus,
        onUnequipItem = onUnequipItem,
        onAssignHeroActivity = onAssignHeroActivity,
        onHostMealWithRecipe = onHostMealWithRecipe,
      )
    }
  }
}

@Composable
private fun SquadHeroRosterList(
  state: SummonerUiState,
  nowSec: Long,
  onToggleHero: (HeroRecord) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(state.heroes, key = { it.id }) { hero ->
      val injured = hero.isCurrentlyInjured(nowSec)
      val selected = hero.inActiveSquad && !injured
      val weapon = state.equipment.firstOrNull { it.id == hero.equippedWeaponId }
      val armor = state.equipment.firstOrNull { it.id == hero.equippedArmorId }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xC81C2E50) else Color(0xAD15253E),
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = if (selected) 2.dp else 1.dp,
            color = when {
              injured -> CrimsonBlood.copy(alpha = 0.7f)
              selected -> SummonerGold
              else -> PortalCyan.copy(alpha = 0.45f)
            },
            shape = RoundedCornerShape(12.dp),
          )
          .testTag("prep_hero_${hero.id}")
          .clickable(enabled = !injured) { onToggleHero(hero) },
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ESHeroCard(
            hero = hero,
            selected = selected,
            onClick = { onToggleHero(hero) },
            modifier = Modifier.width(84.dp).testTag("prep_card_${hero.id}"),
          )

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${hero.name} • Lv.${hero.level} (${"★".repeat(hero.rarity.toInt().coerceIn(1, 5))})",
              style = MaterialTheme.typography.titleSmall,
              color = if (selected) SummonerGold else ParchmentLight,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${hero.classType.labelVi} • HP ${hero.totalMaxHp(state.equipment)} • ATK ${hero.totalAttack(state.equipment)}",
              style = MaterialTheme.typography.labelSmall,
              color = EmeraldVitality,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${weapon?.name ?: "Tay không"} | ${armor?.name ?: "Áo vải"}",
              style = MaterialTheme.typography.labelSmall,
              color = RuneSilver,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )

            if (injured) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(13.dp))
                Text(
                  text = "Đang dưỡng thương tại Khu Nghỉ Ngơi (${hero.remainingInjurySeconds(nowSec)}s)",
                  style = MaterialTheme.typography.labelSmall,
                  color = WarningAmber,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }

          Button(
            onClick = { onToggleHero(hero) },
            enabled = !injured,
            modifier = Modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            colors = FantasySkin.buttonColors(
              containerColor = if (selected) EmeraldVitality else SanctuaryStone,
              contentColor = if (selected) ObsidianVoid else ParchmentLight,
            ),
          ) {
            Text(
              text = when {
                injured -> "Nghỉ"
                selected -> "✓ Đội"
                else -> "+ Chọn"
              },
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}
