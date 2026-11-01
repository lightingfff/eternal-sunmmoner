package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import com.example.ui.components.FantasyButton as Button
import com.example.ui.components.FantasySkin
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.text.font.FontWeight
import com.example.data.FeastBuffHelper
import com.example.data.SanctuaryProgressionHelper
import com.example.data.model.EquipmentRecord
import com.example.data.model.FacilityType
import com.example.data.model.HeroRecord
import com.example.data.model.HeroStatusType
import com.example.data.model.IslandExpansionMilestone
import com.example.data.model.MasterProfile
import com.example.data.model.SanctuaryMealRecipe
import com.example.data.model.TrainingFocus
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.ui.SummonerUiState
import com.example.ui.components.PixelHeroPortraitAvatar
import com.example.ui.components.ResourcePillBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ArcaneViolet
import com.example.ui.theme.AstralSlate
import com.example.ui.theme.BorderRune
import com.example.ui.theme.CrimsonBlood
import com.example.ui.theme.EmeraldVitality
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.RuneSilver
import com.example.ui.theme.SanctuaryStone
import com.example.ui.theme.SummonerGold
import kotlinx.coroutines.delay

@Composable
fun SanctuaryFacilityModalOverlay(
  facility: FacilityType,
  state: SummonerUiState,
  onClose: () -> Unit,
  onSummonHero: () -> Unit,
  onTrainHero: (HeroRecord, Boolean) -> Unit,
  onEquipItem: (HeroRecord, EquipmentRecord) -> Unit,
  onSynthesizeGear: () -> Unit,
  onEnhanceGear: (EquipmentRecord) -> Unit,
  onRecoverHero: (HeroRecord, Boolean) -> Unit,
  onHostMeal: () -> Unit,
  onToggleSquad: (HeroRecord) -> Unit,
  onEnterRestaurantInterior: (() -> Unit)? = null,
  onTrainHeroWithFocus: (HeroRecord, TrainingFocus) -> Unit = { h, f -> onTrainHero(h, f.isSkillUpgrade) },
  onUnequipItem: (HeroRecord, String) -> Unit = { _, _ -> },
  onAssignHeroActivity: (HeroRecord, HeroStatusType) -> Unit = { _, _ -> },
  onHostMealWithRecipe: (SanctuaryMealRecipe) -> Unit = { _ -> onHostMeal() },
  onEnterFacilityInterior: ((FacilityType) -> Unit)? = null,
  onOpenHeroProfile: ((HeroRecord) -> Unit)? = null,
  initialSelectedHeroId: String? = null,
) {
  BackHandler(onBack = onClose)

  val profile = state.profile ?: MasterProfile()
  val accentColor = facilityAccentColor(facility)
  var nowEpochSec by remember { mutableLongStateOf(System.currentTimeMillis() / 1000L) }
  LaunchedEffect(facility) {
    while (true) {
      nowEpochSec = System.currentTimeMillis() / 1000L
      delay(1000L)
    }
  }

  val enterInteriorCallback: (() -> Unit)? = when {
    facility == FacilityType.RESTAURANT && onEnterRestaurantInterior != null -> onEnterRestaurantInterior
    facility in SanctuaryLobbyEngine.ENTERABLE_FACILITIES && onEnterFacilityInterior != null -> {
      { onEnterFacilityInterior(facility) }
    }
    else -> null
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid.copy(alpha = 0.85f))
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(horizontal = 12.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    val isShort = maxHeight < 460.dp

    Surface(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 760.dp)
        .heightIn(max = 680.dp)
        .border(1.5.dp, accentColor.copy(alpha = 0.72f), RoundedCornerShape(18.dp)),
      shape = RoundedCornerShape(18.dp),
      color = FantasySkin.Panel,
      tonalElevation = 10.dp,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = if (isShort) 10.dp else 14.dp),
      ) {
        // Facility-Specific Ornate Modal Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = facility.titleVi,
              style = if (isShort) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
              color = accentColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (!isShort) {
              Text(
                text = facility.subtitleVi,
                style = MaterialTheme.typography.bodySmall,
                color = RuneSilver,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          if (enterInteriorCallback != null && facility != FacilityType.RESTAURANT) {
            OutlinedButton(
              onClick = enterInteriorCallback,
              modifier = Modifier
                .heightIn(min = 42.dp)
                .padding(end = 6.dp)
                .testTag("modal_enter_interior_${facility.name.lowercase()}"),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              border = BorderStroke(1.dp, accentColor),
            ) {
              Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Vào Phòng", style = MaterialTheme.typography.labelMedium, color = accentColor, maxLines = 1)
            }
          }

          IconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp).testTag("close_facility_modal_button"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = ParchmentLight)
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Single-line Horizontally Scrollable Resource Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ResourcePillBadge("Vàng", profile.gold, SummonerGold)
          ResourcePillBadge("Mana", profile.manaCrystals, PortalCyan)
          ResourcePillBadge("Mảnh Hồn", profile.soulShards, ArcaneViolet)
          ResourcePillBadge("Quặng Sắt", profile.ironOre, RuneSilver)
          ResourcePillBadge("Tinh Hoa", profile.arcaneEssence, EmeraldVitality)
          ResourcePillBadge("Khẩu Phần", profile.rations, AmberWarning)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (facility) {
            FacilityType.SUMMONING_HALL -> SummoningHallContent(
              profile = profile,
              heroes = state.heroes,
              equipment = state.equipment,
              newlySummoned = state.newlySummonedHero,
              isBusy = state.isBusyAction,
              nowEpochSec = nowEpochSec,
              onSummonHero = onSummonHero,
              onToggleSquad = onToggleSquad,
              onOpenHeroProfile = onOpenHeroProfile,
            )

            FacilityType.TRAINING_GROUND -> TrainingGroundContent(
              profile = profile,
              heroes = state.heroes,
              equipment = state.equipment,
              isBusy = state.isBusyAction,
              onTrainHeroWithFocus = onTrainHeroWithFocus,
              onAssignHeroActivity = onAssignHeroActivity,
            )

            FacilityType.ARMORY -> ArmoryContent(
              heroes = state.heroes,
              equipment = state.equipment,
              isBusy = state.isBusyAction,
              initialSelectedHeroId = initialSelectedHeroId,
              onEquipItem = onEquipItem,
              onUnequipItem = onUnequipItem,
            )

            FacilityType.SYNTHESIS_LAB -> SynthesisLabContent(
              profile = profile,
              equipment = state.equipment,
              newlyForged = state.newlyForgedItem,
              isBusy = state.isBusyAction,
              onSynthesizeGear = onSynthesizeGear,
              onEnhanceGear = onEnhanceGear,
            )

            FacilityType.HERO_QUARTERS -> HeroQuartersContent(
              profile = profile,
              heroes = state.heroes,
              nowEpochSec = nowEpochSec,
              isBusy = state.isBusyAction,
              onRecoverHero = onRecoverHero,
              onAssignHeroActivity = onAssignHeroActivity,
              onOpenHeroProfile = onOpenHeroProfile,
            )

            FacilityType.RESTAURANT -> RestaurantContent(
              profile = profile,
              heroes = state.heroes,
              nowEpochSec = nowEpochSec,
              isBusy = state.isBusyAction,
              onHostMealWithRecipe = onHostMealWithRecipe,
              onEnterRestaurantInterior = enterInteriorCallback,
            )

            FacilityType.TOWER_GATE -> Unit
          }
        }
      }
    }
  }
}

@Composable
private fun SummoningHallContent(
  profile: MasterProfile,
  heroes: List<HeroRecord>,
  equipment: List<EquipmentRecord>,
  newlySummoned: HeroRecord?,
  isBusy: Boolean,
  nowEpochSec: Long,
  onSummonHero: () -> Unit,
  onToggleSquad: (HeroRecord) -> Unit,
  onOpenHeroProfile: ((HeroRecord) -> Unit)? = null,
) {
  val infinite = rememberInfiniteTransition(label = "summonRune")
  val angle by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
    label = "angle",
  )

  val canSummon = profile.soulShards >= 20L && profile.manaCrystals >= 40L

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(AstralSlate)
          .border(1.dp, ArcaneViolet.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              rotate(angle) {
                drawCircle(ArcaneViolet, radius = size.minDimension * 0.45f, style = Stroke(3f))
                drawRect(
                  color = PortalCyan.copy(alpha = 0.7f),
                  topLeft = Offset(size.width * 0.22f, size.height * 0.22f),
                  size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.56f),
                  style = Stroke(2f),
                )
              }
              drawCircle(SummonerGold.copy(alpha = 0.85f), radius = 7f)
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Nghi Thức Triệu Hồi Tinh Hệ",
              style = MaterialTheme.typography.titleSmall,
              color = ParchmentLight,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Tiêu hao: 20 Mảnh Linh Hồn + 40 Tinh Thể Mana từ Tòa Tháp",
              style = MaterialTheme.typography.bodySmall,
              color = RuneSilver,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            if (!canSummon) {
              Text(
                text = "Thiếu nguyên liệu triệu hồi (Hiện có: ${profile.soulShards}/20 Hồn • ${profile.manaCrystals}/40 Mana).",
                style = MaterialTheme.typography.labelSmall,
                color = AmberWarning,
              )
            }
          }
        }

        Button(
          onClick = onSummonHero,
          enabled = !isBusy && canSummon,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("summon_ritual_button"),
          colors = FantasySkin.buttonColors(containerColor = ArcaneViolet, contentColor = Color.White),
        ) {
          Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Triệu Hồi (20 Hồn • 40 Mana)", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }

    if (newlySummoned != null) {
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("summon_result")
            .clip(RoundedCornerShape(14.dp))
            .background(FantasySkin.Panel)
            .border(1.5.dp, SummonerGold, RoundedCornerShape(14.dp))
            .padding(12.dp),
        ) {
          Text(
            text = "★ TRIỆU HỒI THÀNH CÔNG • ${"★".repeat(newlySummoned.rarity.toInt())}",
            style = MaterialTheme.typography.labelMedium,
            color = SummonerGold,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PixelHeroPortraitAvatar(heroClass = newlySummoned.classType, sizeDp = 52.dp)
            Column(modifier = Modifier.weight(1f)) {
              Text("${newlySummoned.name} — ${newlySummoned.title}", style = MaterialTheme.typography.titleSmall, color = ParchmentLight)
              Text("${newlySummoned.classType.labelVi} • ${newlySummoned.personalityType.labelVi}", style = MaterialTheme.typography.bodySmall, color = PortalCyan)
              Text("Đã lưu vào Anh hùng • mở Hồ sơ để nâng cấp và trang bị.",style=MaterialTheme.typography.labelSmall,color=FantasySkin.Gold)
              Text("«${newlySummoned.personalityType.sampleQuoteVi}»", style = MaterialTheme.typography.bodySmall, color = RuneSilver)
            }
          }
        }
      }
    }

    item {
      Text(
        text = "Danh Sách Anh Hùng Sở Hữu (${heroes.size})",
        style = MaterialTheme.typography.titleSmall,
        color = SummonerGold,
      )
    }

    items(heroes, key = { it.id }) { hero ->
      val injured = hero.isCurrentlyInjured(nowEpochSec)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(ObsidianVoid.copy(alpha = 0.72f))
          .border(1.dp, if (hero.inActiveSquad) SummonerGold.copy(alpha = 0.75f) else BorderRune, RoundedCornerShape(12.dp))
          .clickable(enabled = onOpenHeroProfile != null) { onOpenHeroProfile?.invoke(hero) }
          .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PixelHeroPortraitAvatar(heroClass = hero.classType, sizeDp = 46.dp)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${hero.name} (${"★".repeat(hero.rarity.toInt())}) • Lv.${hero.level}",
            style = MaterialTheme.typography.titleSmall,
            color = ParchmentLight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = hero.consistentActivityLabelVi(nowEpochSec),
            style = MaterialTheme.typography.labelSmall,
            color = if (injured) AmberWarning else PortalCyan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "HP ${hero.totalMaxHp(equipment)} | ATK ${hero.totalAttack(equipment)} | DEF ${hero.totalDefense(equipment)} | Sĩ khí ${hero.morale}%",
            style = MaterialTheme.typography.labelSmall,
            color = RuneSilver,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        OutlinedButton(
          onClick = { onToggleSquad(hero) },
          enabled = !injured,
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text(
            text = when {
              injured -> "Bị Thương"
              hero.inActiveSquad -> "Trong Đội"
              else -> "Vào Đội"
            },
            color = if (injured) AmberWarning else if (hero.inActiveSquad) SummonerGold else ParchmentLight,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainingGroundContent(
  profile: MasterProfile,
  heroes: List<HeroRecord>,
  equipment: List<EquipmentRecord>,
  isBusy: Boolean,
  onTrainHeroWithFocus: (HeroRecord, TrainingFocus) -> Unit,
  onAssignHeroActivity: (HeroRecord, HeroStatusType) -> Unit,
) {
  var selectedFocus by remember { mutableStateOf(TrainingFocus.BALANCED) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(ObsidianVoid.copy(alpha = 0.75f))
          .border(1.dp, AmberWarning.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("Chọn Chuyên Môn Huấn Luyện Thực Chiến:", style = MaterialTheme.typography.labelLarge, color = SummonerGold)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          TrainingFocus.entries.filter { !it.isSkillUpgrade }.forEach { focus ->
            val active = selectedFocus == focus
            Surface(
              shape = RoundedCornerShape(9.dp),
              color = if (active) AmberWarning.copy(alpha = 0.25f) else AstralSlate,
              border = BorderStroke(1.dp, if (active) AmberWarning else BorderRune),
              modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag("focus_chip_${focus.name.lowercase()}")
                .clickable { selectedFocus = focus },
            ) {
              Text(
                text = focus.shortVi,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) SummonerGold else ParchmentLight,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
              )
            }
          }
        }
        Text(selectedFocus.descVi, style = MaterialTheme.typography.bodySmall, color = PortalCyan)
      }
    }

    items(heroes, key = { it.id }) { hero ->
      val levelGoldCost = 80L + hero.level * 35L
      val skillManaCost = 25L + hero.skillLevel * 10L
      val injured = hero.isCurrentlyInjured()
      val inTraining = hero.statusType == HeroStatusType.TRAINING

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(AstralSlate)
          .border(1.dp, if (inTraining) AmberWarning else BorderRune, RoundedCornerShape(12.dp))
          .padding(12.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          PixelHeroPortraitAvatar(heroClass = hero.classType, sizeDp = 46.dp)
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${hero.name} — Lv.${hero.level}${if (inTraining) " • [Đang Tập Võ]" else ""}",
              style = MaterialTheme.typography.titleSmall,
              color = ParchmentLight,
            )
            Text(
              text = "HP ${hero.totalMaxHp(equipment)} • ATK ${hero.totalAttack(equipment)} • DEF ${hero.totalDefense(equipment)} • SPD ${hero.totalSpeed(equipment)}",
              style = MaterialTheme.typography.labelSmall,
              color = PortalCyan,
            )
            Text(
              text = "Tuyệt kỹ: ${hero.classType.skillNameVi} (Lv.${hero.skillLevel})",
              style = MaterialTheme.typography.bodySmall,
              color = SummonerGold,
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { onTrainHeroWithFocus(hero, selectedFocus) },
            enabled = !isBusy && !injured && profile.gold >= levelGoldCost,
            modifier = Modifier.heightIn(min = 48.dp).testTag("train_level_${hero.id}"),
            colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
          ) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "Rèn ${selectedFocus.shortVi} ($levelGoldCost Vàng)",
              style = MaterialTheme.typography.labelMedium,
              maxLines = 1,
            )
          }
          OutlinedButton(
            onClick = { onTrainHeroWithFocus(hero, TrainingFocus.ARCANE_SKILL) },
            enabled = !isBusy && !injured && profile.manaCrystals >= skillManaCost && profile.arcaneEssence >= 1L,
            modifier = Modifier.heightIn(min = 48.dp).testTag("train_skill_${hero.id}"),
          ) {
            Text(
              text = "Nâng Kỹ Năng ($skillManaCost Mana + 1 Tinh Hoa)",
              style = MaterialTheme.typography.labelSmall,
              color = PortalCyan,
              maxLines = 1,
            )
          }
          OutlinedButton(
            onClick = {
              onAssignHeroActivity(
                hero,
                if (inTraining) HeroStatusType.READY else HeroStatusType.TRAINING,
              )
            },
            enabled = !isBusy && !injured,
            modifier = Modifier.heightIn(min = 48.dp).testTag("assign_train_routine_${hero.id}"),
          ) {
            Text(
              text = if (inTraining) "Ngừng Tập Thường Trực" else "Giao Lịch Tập Võ",
              style = MaterialTheme.typography.labelSmall,
              color = if (inTraining) AmberWarning else EmeraldVitality,
              maxLines = 1,
            )
          }
        }
        if (injured) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Đang dưỡng thương tại Khu Nghỉ Ngơi — hãy hồi phục trước khi huấn luyện.",
            style = MaterialTheme.typography.labelSmall,
            color = AmberWarning,
          )
        }
      }
    }
  }
}

@Composable
private fun ArmoryContent(
  heroes: List<HeroRecord>,
  equipment: List<EquipmentRecord>,
  isBusy: Boolean,
  initialSelectedHeroId: String?,
  onEquipItem: (HeroRecord, EquipmentRecord) -> Unit,
  onUnequipItem: (HeroRecord, String) -> Unit,
) {
  var selectedHeroId by remember(heroes, initialSelectedHeroId) {
    mutableStateOf(
      initialSelectedHeroId?.takeIf { id -> heroes.any { it.id == id } }
        ?: heroes.firstOrNull()?.id
        ?: ""
    )
  }
  val selectedHero = heroes.firstOrNull { it.id == selectedHeroId } ?: heroes.firstOrNull()
  val curWeapon = selectedHero?.let { h -> equipment.firstOrNull { it.id == h.equippedWeaponId } }
  val curArmor = selectedHero?.let { h -> equipment.firstOrNull { it.id == h.equippedArmorId } }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text("1. Chọn Anh hùng kiểm tra & lắp trang bị:", style = MaterialTheme.typography.labelLarge, color = SummonerGold)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      heroes.forEach { h ->
        val active = h.id == selectedHero?.id
        Box(
          modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) PortalCyan.copy(alpha = 0.24f) else ObsidianVoid)
            .border(1.dp, if (active) SummonerGold else BorderRune, RoundedCornerShape(10.dp))
            .clickable { selectedHeroId = h.id }
            .padding(horizontal = 12.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(h.name, style = MaterialTheme.typography.labelLarge, color = if (active) SummonerGold else ParchmentLight, maxLines = 1)
        }
      }
    }

    if (selectedHero != null) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ArmoryEquippedSlotPill(
          slotLabel = "Vũ Khí",
          item = curWeapon,
          tag = "armory_unequip_weapon",
          isBusy = isBusy,
          onUnequip = { onUnequipItem(selectedHero, "WEAPON") },
          modifier = Modifier.weight(1f),
        )
        ArmoryEquippedSlotPill(
          slotLabel = "Giáp Trụ",
          item = curArmor,
          tag = "armory_unequip_armor",
          isBusy = isBusy,
          onUnequip = { onUnequipItem(selectedHero, "ARMOR") },
          modifier = Modifier.weight(1f),
        )
      }
    }

    Text("2. Kho Binh Khí & Giáp Trụ (${equipment.size}):", style = MaterialTheme.typography.labelLarge, color = PortalCyan)

    val baseHp = selectedHero?.totalMaxHp(equipment) ?: 0
    val baseAtk = selectedHero?.totalAttack(equipment) ?: 0
    val baseDef = selectedHero?.totalDefense(equipment) ?: 0
    val baseSpd = selectedHero?.totalSpeed(equipment) ?: 0

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(equipment, key = { it.id }) { item ->
        val holderName = heroes.firstOrNull { it.id == item.equippedByHeroId }?.name
        val alreadyOnSelected = selectedHero != null && item.equippedByHeroId == selectedHero.id

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AstralSlate)
            .border(1.dp, if (alreadyOnSelected) SummonerGold else BorderRune, RoundedCornerShape(12.dp))
            .padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = if (item.slot == "WEAPON") SummonerGold else PortalCyan,
            modifier = Modifier.size(26.dp),
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${item.name} • +${item.level} (${"★".repeat(item.rarity.toInt())})",
              style = MaterialTheme.typography.titleSmall,
              color = ParchmentLight,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "+${item.bonusHp} HP • +${item.bonusAttack} ATK • +${item.bonusDefense} DEF • +${item.bonusSpeed} SPD",
              style = MaterialTheme.typography.labelSmall,
              color = RuneSilver,
            )
            if (selectedHero != null && !alreadyOnSelected) {
              val preview = selectedHero.previewWithCandidateItem(item)
              val dHp = preview.totalMaxHp(equipment) - baseHp
              val dAtk = preview.totalAttack(equipment) - baseAtk
              val dDef = preview.totalDefense(equipment) - baseDef
              val dSpd = preview.totalSpeed(equipment) - baseSpd
              val deltaText = buildList {
                if (dHp != 0) add("${if (dHp > 0) "+" else ""}$dHp HP")
                if (dAtk != 0) add("${if (dAtk > 0) "+" else ""}$dAtk ATK")
                if (dDef != 0) add("${if (dDef > 0) "+" else ""}$dDef DEF")
                if (dSpd != 0) add("${if (dSpd > 0) "+" else ""}$dSpd SPD")
              }.joinToString(" • ")
              if (deltaText.isNotEmpty()) {
                val positive = (dAtk * 3 + dDef * 2 + dHp / 4 + dSpd) >= 0
                Text(
                  text = "Khi trang bị: $deltaText",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (positive) EmeraldVitality else CrimsonBlood,
                )
              }
            }
            if (holderName != null) {
              Text(
                text = if (alreadyOnSelected) "Đang trang bị trên $holderName" else "Đang mặc: $holderName (chuyển trực tiếp)",
                style = MaterialTheme.typography.bodySmall,
                color = if (alreadyOnSelected) SummonerGold else AmberWarning,
              )
            }
          }
          if (selectedHero != null) {
            Button(
              onClick = { onEquipItem(selectedHero, item) },
              enabled = !isBusy && !alreadyOnSelected,
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("armory_equip_${item.id}"),
              colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
            ) {
              Text(if (alreadyOnSelected) "Đã Mặc" else "Trang Bị", maxLines = 1)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ArmoryEquippedSlotPill(
  slotLabel: String,
  item: EquipmentRecord?,
  tag: String,
  isBusy: Boolean,
  onUnequip: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = ObsidianVoid.copy(alpha = 0.78f),
    border = BorderStroke(1.dp, if (item != null) SummonerGold.copy(alpha = 0.6f) else BorderRune),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(slotLabel, style = MaterialTheme.typography.labelSmall, color = PortalCyan)
        Text(
          text = item?.name ?: "Trống",
          style = MaterialTheme.typography.labelMedium,
          color = if (item != null) ParchmentLight else RuneSilver,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (item != null) {
        OutlinedButton(
          onClick = onUnequip,
          enabled = !isBusy,
          modifier = Modifier.heightIn(min = 38.dp).testTag(tag),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
          Text("Tháo", style = MaterialTheme.typography.labelSmall, color = AmberWarning)
        }
      }
    }
  }
}

@Composable
private fun SynthesisLabContent(
  profile: MasterProfile,
  equipment: List<EquipmentRecord>,
  newlyForged: EquipmentRecord?,
  isBusy: Boolean,
  onSynthesizeGear: () -> Unit,
  onEnhanceGear: (EquipmentRecord) -> Unit,
) {
  val canSynthesize = profile.ironOre >= 3L && profile.arcaneEssence >= 2L && profile.gold >= 120L

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(AstralSlate)
          .border(1.dp, EmeraldVitality.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
          .padding(14.dp),
      ) {
        Text("Lò Dung Hợp Cổ Ngữ", style = MaterialTheme.typography.titleSmall, color = EmeraldVitality)
        Text(
          text = "Kết hợp 3 Quặng Sắt + 2 Tinh Hoa Cổ Ngữ + 120 Vàng để rèn Vũ Khí hoặc Giáp Trụ 2★–4★.",
          style = MaterialTheme.typography.bodySmall,
          color = RuneSilver,
        )
        if (!canSynthesize) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Thiếu nguyên liệu rèn (Hiện có: ${profile.ironOre}/3 Quặng • ${profile.arcaneEssence}/2 Tinh Hoa • ${profile.gold}/120 Vàng).",
            style = MaterialTheme.typography.labelSmall,
            color = AmberWarning,
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = onSynthesizeGear,
          enabled = !isBusy && canSynthesize,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("synthesize_gear_button"),
          colors = FantasySkin.buttonColors(containerColor = EmeraldVitality, contentColor = ObsidianVoid),
        ) {
          Text("Dung Hợp Chế Tác (3 Quặng • 2 Tinh Hoa • 120 Vàng)", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }

    if (newlyForged != null) {
      item {
        AnimatedVisibility(visible = true) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF14332B))
              .border(1.5.dp, SummonerGold, RoundedCornerShape(12.dp))
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "★ Vừa rèn thành công: ${newlyForged.name} (+${newlyForged.bonusAttack} ATK, +${newlyForged.bonusHp} HP, +${newlyForged.bonusDefense} DEF)",
              style = MaterialTheme.typography.bodySmall,
              color = SummonerGold,
            )
          }
        }
      }
    }

    item {
      Text("Cường Hóa Trang Bị Hiện Có (+1 Quặng Sắt mỗi lần):", style = MaterialTheme.typography.titleSmall, color = SummonerGold)
    }

    items(equipment, key = { it.id }) { item ->
      val enhanceGold = 90L + item.level * 45L
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(ObsidianVoid.copy(alpha = 0.65f))
          .border(1.dp, BorderRune, RoundedCornerShape(12.dp))
          .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("${item.name} (+${item.level})", style = MaterialTheme.typography.titleSmall, color = ParchmentLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text("+${item.bonusHp} HP • +${item.bonusAttack} ATK • +${item.bonusDefense} DEF", style = MaterialTheme.typography.labelSmall, color = RuneSilver)
        }
        OutlinedButton(
          onClick = { onEnhanceGear(item) },
          enabled = !isBusy && profile.gold >= enhanceGold && profile.ironOre >= 1L,
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text("+Cấp ($enhanceGold Vàng)", style = MaterialTheme.typography.labelSmall, color = SummonerGold, maxLines = 1)
        }
      }
    }
  }
}

@Composable
private fun HeroQuartersContent(
  profile: MasterProfile,
  heroes: List<HeroRecord>,
  nowEpochSec: Long,
  isBusy: Boolean,
  onRecoverHero: (HeroRecord, Boolean) -> Unit,
  onAssignHeroActivity: (HeroRecord, HeroStatusType) -> Unit,
  onOpenHeroProfile: ((HeroRecord) -> Unit)? = null,
) {
  val eastGardenUnlocked = SanctuaryProgressionHelper.isExpansionUnlocked(
    profile.storyFlags,
    IslandExpansionMilestone.EAST_CELESTIAL_GARDEN,
  )
  val injuredCount = heroes.count { it.status == HeroStatusType.INJURED.name }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(ObsidianVoid.copy(alpha = 0.72f))
          .border(1.dp, EmeraldVitality.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "Giường Tịnh Dưỡng Khu Nghỉ Ngơi: Đang sử dụng $injuredCount / 6 giường",
          style = MaterialTheme.typography.titleSmall,
          color = EmeraldVitality,
        )
        Text(
          text = "Anh hùng trọng thương trong Tháp được đưa thẳng về đây nghỉ ngơi, không mất vĩnh viễn và không tự ý tuần tra ngoài trời.",
          style = MaterialTheme.typography.bodySmall,
          color = RuneSilver,
        )
        if (eastGardenUnlocked) {
          Text(
            text = "✦ Suối Thánh Đài Vườn Đông đang hoạt động: Giảm 25% thời gian hồi phục chấn thương!",
            style = MaterialTheme.typography.labelSmall,
            color = PortalCyan,
          )
        }
      }
    }

    items(heroes, key = { it.id }) { hero ->
      val remSec = hero.remainingInjurySeconds(nowEpochSec)
      val needsStatusClear = hero.status == HeroStatusType.INJURED.name
      val isResting = hero.statusType == HeroStatusType.RESTING

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(AstralSlate)
          .border(1.dp, if (needsStatusClear) AmberWarning else if (isResting) EmeraldVitality else BorderRune, RoundedCornerShape(12.dp))
          .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PixelHeroPortraitAvatar(heroClass = hero.classType, sizeDp = 46.dp)
        Column(modifier = Modifier.weight(1f)) {
          Text("${hero.name} • Sĩ khí ${hero.morale}%", style = MaterialTheme.typography.titleSmall, color = ParchmentLight)
          Text(
            text = when {
              remSec > 0L -> "Đang băng bó trên giường bệnh • Còn ${remSec}s"
              needsStatusClear -> "Đã tịnh dưỡng xong • Sẵn sàng xuất viện!"
              isResting -> "Đang nghỉ ngơi thư giãn trong Khu Nghỉ Ngơi"
              else -> "Khỏe mạnh • ${hero.personalityType.lobbyHabitVi}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (remSec > 0L) AmberWarning else EmeraldVitality,
          )
        }

        if (onOpenHeroProfile != null) {
          IconButton(
            onClick = { onOpenHeroProfile(hero) },
            modifier = Modifier.size(44.dp).testTag("quarters_profile_${hero.id}"),
          ) {
            Icon(Icons.Default.Person, contentDescription = "Mở hồ sơ ${hero.name}", tint = SummonerGold)
          }
        }

        if (needsStatusClear) {
          Button(
            onClick = { onRecoverHero(hero, remSec > 0L) },
            enabled = !isBusy && (remSec == 0L || (profile.rations >= 2L && profile.manaCrystals >= 15L)),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("recover_hero_${hero.id}"),
            colors = FantasySkin.buttonColors(
              containerColor = if (remSec == 0L) EmeraldVitality else AmberWarning,
              contentColor = ObsidianVoid,
            ),
          ) {
            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (remSec == 0L) "Hoàn Tất" else "Trị Ngay", maxLines = 1)
          }
        } else {
          OutlinedButton(
            onClick = {
              onAssignHeroActivity(
                hero,
                if (isResting) HeroStatusType.READY else HeroStatusType.RESTING,
              )
            },
            enabled = !isBusy,
            modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Text(
              text = if (isResting) "Ra Tuần Tra" else "Nghỉ Ngơi",
              color = if (isResting) PortalCyan else EmeraldVitality,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RestaurantContent(
  profile: MasterProfile,
  heroes: List<HeroRecord>,
  nowEpochSec: Long,
  isBusy: Boolean,
  onHostMealWithRecipe: (SanctuaryMealRecipe) -> Unit,
  onEnterRestaurantInterior: (() -> Unit)? = null,
) {
  var selectedRecipe by remember { mutableStateOf(SanctuaryMealRecipe.ROAST_BEAST_PLATTER) }
  val activeMealPair = FeastBuffHelper.activeMealRecipeAndRemaining(profile.storyFlags, nowEpochSec)
  val canAffordMeal = profile.rations >= selectedRecipe.rationCost && profile.gold >= selectedRecipe.goldCost

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(AstralSlate)
          .border(1.dp, SummonerGold.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.Restaurant, contentDescription = null, tint = SummonerGold)
          Text("Thực Đơn Bếp Trưởng Thánh Địa", style = MaterialTheme.typography.titleSmall, color = SummonerGold)
        }

        if (activeMealPair != null) {
          val (activeRecipe, remFeastSec) = activeMealPair
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF14332B),
            modifier = Modifier.border(1.dp, EmeraldVitality, RoundedCornerShape(8.dp)),
          ) {
            Text(
              text = "★ ĐANG CÓ HIỆU LỰC [${activeRecipe.titleVi}]: +${activeRecipe.atkBuffPct}% CÔNG • +${activeRecipe.hpBuffPct}% MÁU (còn ${remFeastSec / 60}p ${remFeastSec % 60}s)",
              style = MaterialTheme.typography.labelMedium,
              color = EmeraldVitality,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
        }

        SanctuaryMealRecipe.entries.forEach { recipe ->
          val isSel = selectedRecipe == recipe
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isSel) SummonerGold.copy(alpha = 0.18f) else ObsidianVoid.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, if (isSel) SummonerGold else BorderRune),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("meal_recipe_${recipe.code.lowercase()}")
              .clickable { selectedRecipe = recipe },
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(recipe.titleVi, style = MaterialTheme.typography.labelLarge, color = if (isSel) SummonerGold else ParchmentLight)
                Text("${recipe.rationCost} Khẩu Phần • ${recipe.goldCost} Vàng", style = MaterialTheme.typography.labelSmall, color = PortalCyan)
              }
              Text(recipe.subtitleVi, style = MaterialTheme.typography.bodySmall, color = RuneSilver)
            }
          }
        }

        if (!canAffordMeal) {
          Text(
            text = "Thiếu nguyên liệu mở tiệc (Cần ${selectedRecipe.rationCost} Khẩu Phần & ${selectedRecipe.goldCost} Vàng).",
            style = MaterialTheme.typography.labelSmall,
            color = AmberWarning,
          )
        }

        Button(
          onClick = { onHostMealWithRecipe(selectedRecipe) },
          enabled = !isBusy && canAffordMeal,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("host_meal_button"),
          colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
        ) {
          Text(
            text = "Phục Vụ ${selectedRecipe.titleVi} (${selectedRecipe.rationCost} Khẩu Phần • ${selectedRecipe.goldCost} Vàng)",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (onEnterRestaurantInterior != null) {
          OutlinedButton(
            onClick = onEnterRestaurantInterior,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("modal_enter_restaurant_interior_button"),
          ) {
            Text(
              text = "Bước Vào Không Gian Nhà Ăn (Playable Tavern Interior)",
              color = PortalCyan,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    items(heroes, key = { it.id }) { h ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(ObsidianVoid.copy(alpha = 0.65f))
          .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${h.name} (${h.personalityType.labelVi})",
          style = MaterialTheme.typography.bodySmall,
          color = ParchmentLight,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = "Sĩ khí: ${h.morale}%",
          style = MaterialTheme.typography.labelMedium,
          color = if (h.morale >= 90L) EmeraldVitality else AmberWarning,
          maxLines = 1,
        )
      }
    }
  }
}
