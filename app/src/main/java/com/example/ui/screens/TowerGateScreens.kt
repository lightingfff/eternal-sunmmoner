package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FeastBuffHelper
import com.example.data.SanctuaryProgressionHelper
import com.example.data.model.EquipmentRecord
import com.example.data.model.FacilityType
import com.example.data.model.HeroRecord
import com.example.data.model.HeroStatusType
import com.example.data.model.IslandExpansionMilestone
import com.example.data.model.SanctuaryMealRecipe
import com.example.data.model.TrainingFocus
import com.example.game.map.TowerFloorSpec
import com.example.game.tower.TowerFloorCatalog
import com.example.performance.SceneFrameLoop
import com.example.ui.SummonerUiState
import com.example.ui.components.FantasyButton as Button
import com.example.ui.components.FantasySkin
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

@Composable
fun FloorSelectionScreen(
  state: SummonerUiState,
  onSelectFloor: (TowerFloorSpec) -> Unit,
  onBackToLobby: () -> Unit,
  playEntryCutscene: Boolean = false,
  sceneActive: Boolean = true,
) {
  var playing by rememberSaveable { mutableStateOf(playEntryCutscene) }
  var cutsceneTime by rememberSaveable { mutableFloatStateOf(0f) }
  val ambientTime = remember { mutableFloatStateOf(0f) }
  var selectedGateId by rememberSaveable { mutableStateOf<String?>(null) }
  val selectedGate = DimensionalNexusGate.entries.firstOrNull { it.id == selectedGateId }
  val context = LocalContext.current
  val art = remember(context.assets) { TowerGateMapArt(context.assets) }
  val highestCleared = state.profile?.highestClearedFloor?.toInt() ?: 0

  BackHandler { if (selectedGateId != null) selectedGateId = null else onBackToLobby() }
  SceneFrameLoop(active = sceneActive && selectedGate == null, targetFps = 30) { dt, _ ->
    val step = dt.coerceAtMost(.1f)
    if (playing) {
      cutsceneTime = (cutsceneTime + step).coerceAtMost(GATE_CUTSCENE_DURATION_SEC)
      if (cutsceneTime >= GATE_CUTSCENE_DURATION_SEC) playing = false
    } else ambientTime.floatValue += step
  }

  BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF091424))) {
    Canvas(Modifier.fillMaxSize().testTag(if (playing) "tower_gate_cutscene_canvas" else "dimensional_nexus_minimap_canvas")
      .semantics {
        contentDescription = if (playing) "Đại Môn Tháp Vĩnh Hằng đang mở" else "Sân ba cổng: Sự kiện bên trái, Tòa Tháp ở giữa, Tùy chọn bên phải"
        if (!playing) customActions = listOf(CustomAccessibilityAction("Xem lại Đại Môn") {
          cutsceneTime = 0f; playing = true; true
        })
      }) {
      drawTowerGateMap(art, playing, if (playing) cutsceneTime else ambientTime.floatValue, state.heroes)
    }
    if (playing) {
      // Only a small optional skip control. The architecture and effects stay unobscured.
      Text("Bỏ qua ›", color = ParchmentLight, modifier = Modifier.align(Alignment.TopEnd)
        .windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp)
        .testTag("skip_gate_cutscene_button").clickable { playing = false }.padding(12.dp))
    } else {
      val density = LocalDensity.current
      val viewport = GateMapViewport(with(density) { maxWidth.toPx() }, with(density) { maxHeight.toPx() })
      // Invisible accessible hit targets occupy the actual architecture. Same transform as drawing.
      DimensionalNexusGate.entries.forEachIndexed { index, gate ->
        val rect = viewport.screenRect(NexusArches[index].bounds)
        Box(Modifier.offset { IntOffset(rect.left.toInt(), rect.top.toInt()) }
          .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
          .testTag("nexus_gate_${gate.id}")
          .semantics { contentDescription = gate.shortVi; role = Role.Button }
          .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { selectedGateId = gate.id })
      }
    }
    if (selectedGate != null) {
      Dialog(onDismissRequest = { selectedGateId = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.92f).fillMaxHeight(.88f).widthIn(max = 850.dp)
          .testTag("nexus_floor_picker"), color = Color(0xF20C1C2D), shape = RoundedCornerShape(18.dp)) {
          Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Text(selectedGate.shortVi, color = selectedGate.primaryColor, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
              IconButton(onClick = { selectedGateId = null }, modifier = Modifier.testTag("close_nexus_floor_picker")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở lại ba cổng", tint = ParchmentLight)
              }
            }
            var activeTab by remember(selectedGate) { mutableIntStateOf(when (selectedGate) {
              DimensionalNexusGate.EVENT_GATE -> 3
              DimensionalNexusGate.CUSTOM_GATE -> 1
              else -> 0
            }) }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("Tất cả", "Thám thính", "Phòng thủ", "Thủ lĩnh").forEachIndexed { index, title ->
                OutlinedButton(onClick = { activeTab = index }, modifier = Modifier.testTag("portal_tab_$index")) {
                  Text(title, color = if (activeTab == index) selectedGate.primaryColor else ParchmentLight)
                }
              }
            }
            val floors = when (activeTab) {
              1 -> TowerFloorCatalog.FLOORS.filter { it.floorNumber in setOf(1, 2, 3, 6, 8) }
              2 -> TowerFloorCatalog.FLOORS.filter { it.floorNumber in setOf(1, 4, 7, 9) }
              3 -> TowerFloorCatalog.FLOORS.filter { it.floorNumber in setOf(1, 5, 10) }
              else -> TowerFloorCatalog.FLOORS
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              items(floors, key = { it.floorNumber }) { floor ->
                val unlocked = floor.floorNumber <= highestCleared + 1
                Surface(Modifier.fillMaxWidth().testTag("floor_card_${floor.floorNumber}")
                  .clickable(enabled = unlocked) { onSelectFloor(floor) }, color = Color(0xFF172B40), shape = RoundedCornerShape(12.dp)) {
                  FloorDetailsBody(floor, unlocked, selectedGate.primaryColor, { onSelectFloor(floor) }, Modifier.padding(12.dp))
                }
              }
            }
          }
        }
      }
    }
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
  onHealHeroInfirmary: (HeroRecord) -> Unit,
  onServeHeartyMeal: (HeroRecord) -> Unit,
  onCruelSynthesis: (String, List<String>) -> Unit,
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
        onHealHeroInfirmary = onHealHeroInfirmary,
        onServeHeartyMeal = onServeHeartyMeal,
        onCruelSynthesis = onCruelSynthesis,
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
