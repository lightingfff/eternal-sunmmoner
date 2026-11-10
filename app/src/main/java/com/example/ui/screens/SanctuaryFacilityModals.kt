package com.example.ui.screens

import com.example.data.LostCourierProgression
import com.example.ui.theme.SanctuaryUi

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
import androidx.compose.ui.platform.LocalConfiguration
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
  onHealHeroInfirmary: (HeroRecord) -> Unit = { h -> onRecoverHero(h, false) },
  onServeHeartyMeal: (HeroRecord) -> Unit = { h -> onRecoverHero(h, true) },
  onCruelSynthesis: (String, List<String>) -> Unit = { _, _ -> },
) {
  BackHandler(onBack = onClose)

  val profile = state.profile ?: MasterProfile()
  // A dedicated Summon parchment surface; the rest of the facility windows remain
  // dark glass. Ink is explicitly dark here (light gold text on ivory is illegible).
  val isIvory = facility == FacilityType.SUMMONING_HALL
  val ink = if (isIvory) SanctuaryUi.ink else ParchmentLight
  val secondaryInk = if (isIvory) SanctuaryUi.inkSecondary else RuneSilver
  val accentColor = if (isIvory) SanctuaryUi.accentDeep else facilityAccentColor(facility)
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
      .background(Color(0x26081224))
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    val isShort = maxHeight < 460.dp

    Surface(
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .fillMaxSize(if (isShort) 0.94f else 0.88f)
        .widthIn(max = 720.dp)
        .heightIn(max = 620.dp)
        .border(1.5.dp, if (isIvory) SanctuaryUi.gold else accentColor.copy(alpha = 0.85f), RoundedCornerShape(20.dp)),
      shape = RoundedCornerShape(20.dp),
      color = if (isIvory) SanctuaryUi.surface.copy(alpha = 0.93f) else Color(0xD2132238),
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
                color = secondaryInk,
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
            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = ink)
          }
        }

        if (isIvory) {
          Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(SanctuaryUi.gold.copy(alpha = 0.65f)))
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Single-line Horizontally Scrollable Resource Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ResourcePillBadge("Vàng", profile.gold, SummonerGold, lightOnIvory = isIvory)
          ResourcePillBadge("Mana", profile.manaCrystals, PortalCyan, lightOnIvory = isIvory)
          ResourcePillBadge("Mảnh Hồn", profile.soulShards, ArcaneViolet, lightOnIvory = isIvory)
          ResourcePillBadge("Quặng Sắt", profile.ironOre, RuneSilver, lightOnIvory = isIvory)
          ResourcePillBadge("Tinh Hoa", profile.arcaneEssence, EmeraldVitality, lightOnIvory = isIvory)
          ResourcePillBadge("Khẩu Phần", profile.rations, AmberWarning, lightOnIvory = isIvory)
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
              onHealHeroInfirmary = onHealHeroInfirmary,
              onServeHeartyMeal = onServeHeartyMeal,
              onCruelSynthesis = onCruelSynthesis,
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
  var confirmSummon by remember { mutableStateOf(false) }
  if(confirmSummon) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest={confirmSummon=false},
      containerColor=SanctuaryUi.surface,
      // The app theme may still supply a light content colour; state it explicitly so
      // the message never becomes ivory-on-ivory in either light or dark app themes.
      textContentColor=SanctuaryUi.inkSecondary,
      titleContentColor=SanctuaryUi.ink,
      title={Text("Xác nhận triệu hồi",color=SanctuaryUi.ink)},
      text={Text("Tiêu hao 20 Mảnh Linh Hồn và 40 Mana. Anh hùng nhận được sẽ được lưu vào đội ngũ của bạn.",color=SanctuaryUi.inkSecondary)},
      confirmButton={Button(onClick={confirmSummon=false;onSummonHero()},
        enabled=!isBusy && profile.soulShards>=20 && profile.manaCrystals>=40,
        modifier=Modifier.testTag("confirm_summon_spend"),
        colors=ButtonDefaults.buttonColors(containerColor=SanctuaryUi.accent,contentColor=Color.White)) {
        Text("Triệu hồi • 20 Hồn / 40 Mana")
      }},
      dismissButton={androidx.compose.material3.TextButton(onClick={confirmSummon=false},modifier=Modifier.testTag("cancel_summon_spend")) {Text("Hủy",color=SanctuaryUi.inkSecondary)}})
  }
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
          .background(SanctuaryUi.card)
          .border(1.dp, SanctuaryUi.gold.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
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
                drawCircle(Color(0xFF2D8EAC), radius = size.minDimension * 0.45f, style = Stroke(3f))
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
              color = SanctuaryUi.ink,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Tiêu hao: 20 Mảnh Linh Hồn + 40 Tinh Thể Mana từ Tòa Tháp",
              style = MaterialTheme.typography.bodySmall,
              color = SanctuaryUi.inkSecondary,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            if (!canSummon) {
              Text(
                text = "Thiếu nguyên liệu triệu hồi (Hiện có: ${profile.soulShards}/20 Hồn • ${profile.manaCrystals}/40 Mana).",
                style = MaterialTheme.typography.labelSmall,
                color = SanctuaryUi.warning,
              )
            }
          }
        }

        Button(
          onClick = { confirmSummon=true },
          enabled = !isBusy && canSummon,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("summon_ritual_button"),
          colors = ButtonDefaults.buttonColors(
            containerColor = SanctuaryUi.accent, contentColor = Color.White,
            disabledContainerColor = Color(0xFFD5D9D9), disabledContentColor = SanctuaryUi.inkSecondary,
          ),
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
            .background(SanctuaryUi.card)
            .border(1.5.dp, SanctuaryUi.gold, RoundedCornerShape(14.dp))
            .padding(12.dp),
        ) {
          Text(
            text = "★ TRIỆU HỒI THÀNH CÔNG • ${"★".repeat(newlySummoned.rarity.toInt())}",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8A5A16),
          )
          Spacer(modifier = Modifier.height(6.dp))
          // Debt E: on a 360dp-high landscape viewport this card used to overflow the
          // window and the quote line was cut mid-glyph. Three changes, all local:
          //  1. the avatar shrinks with the viewport instead of a fixed 52dp;
          //  2. every line is capped with an ellipsis, so it degrades by truncating one
          //     line instead of painting outside the card;
          //  3. the quote is the line that carries the least information, so it is the
          //     one allowed to disappear on a short viewport.
          val compact = LocalConfiguration.current.screenHeightDp < 420
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PixelHeroPortraitAvatar(heroClass = newlySummoned.classType, sizeDp = if (compact) 40.dp else 52.dp)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                "${newlySummoned.name} — ${newlySummoned.title}",
                style = MaterialTheme.typography.titleSmall, color = SanctuaryUi.ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
              )
              Text(
                "${newlySummoned.classType.labelVi} • ${newlySummoned.personalityType.labelVi}",
                style = MaterialTheme.typography.bodySmall, color = SanctuaryUi.accent,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
              )
              if (!compact) {
                Text(
                  "Đã lưu vào Anh hùng • mở Hồ sơ để nâng cấp và trang bị.",
                  style = MaterialTheme.typography.labelSmall, color = SanctuaryUi.brown,
                  maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                  "«${newlySummoned.personalityType.sampleQuoteVi}»",
                  style = MaterialTheme.typography.bodySmall, color = SanctuaryUi.inkSecondary,
                  maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }

    item {
      Text(
        text = "Danh Sách Anh Hùng Sở Hữu (${heroes.size})",
        style = MaterialTheme.typography.titleSmall,
        color = SanctuaryUi.brown,
      )
    }

    items(heroes, key = { it.id }) { hero ->
      val injured = hero.isCurrentlyInjured(nowEpochSec)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(SanctuaryUi.card)
          .border(1.dp, if (hero.inActiveSquad) SanctuaryUi.gold else SanctuaryUi.divider, RoundedCornerShape(12.dp))
          .clickable(enabled = onOpenHeroProfile != null) { onOpenHeroProfile?.invoke(hero) }
          .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PixelHeroPortraitAvatar(heroName = hero.name, heroClass = hero.classType, sizeDp = 46.dp)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${hero.name} (${"★".repeat(hero.rarity.toInt())}) • Lv.${hero.level}",
            style = MaterialTheme.typography.titleSmall,
            color = SanctuaryUi.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = hero.consistentActivityLabelVi(nowEpochSec),
            style = MaterialTheme.typography.labelSmall,
            color = if (injured) SanctuaryUi.warning else SanctuaryUi.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "HP ${hero.totalMaxHp(equipment)} | ATK ${hero.totalAttack(equipment)} | DEF ${hero.totalDefense(equipment)} | Sĩ khí ${hero.morale}%",
            style = MaterialTheme.typography.labelSmall,
            color = SanctuaryUi.inkSecondary,
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
            color = if (injured) SanctuaryUi.warning else if (hero.inActiveSquad) SanctuaryUi.brown else SanctuaryUi.ink,
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
          .background(Color(0x98162842))
          .border(1.dp, AmberWarning.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
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
              color = if (active) AmberWarning.copy(alpha = 0.28f) else Color(0x9E192D48),
              border = BorderStroke(1.dp, if (active) AmberWarning else PortalCyan.copy(alpha = 0.42f)),
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
          .background(Color(0xA4182B46))
          .border(1.dp, if (inTraining) AmberWarning else PortalCyan.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
          .padding(12.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          PixelHeroPortraitAvatar(heroName = hero.name, heroClass = hero.classType, sizeDp = 46.dp)
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
            .background(if (active) PortalCyan.copy(alpha = 0.28f) else Color(0x96162842))
            .border(1.dp, if (active) SummonerGold else PortalCyan.copy(alpha = 0.42f), RoundedCornerShape(10.dp))
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
            .background(Color(0xA4182B46))
            .border(1.dp, if (alreadyOnSelected) SummonerGold else PortalCyan.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
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
    color = Color(0x98162842),
    border = BorderStroke(1.dp, if (item != null) SummonerGold.copy(alpha = 0.68f) else PortalCyan.copy(alpha = 0.42f)),
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
  val goldCost = LostCourierProgression.synthesisGoldCost(profile.storyFlags)
  val canSynthesize = profile.ironOre >= 3L && profile.arcaneEssence >= 2L && profile.gold >= goldCost

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
          text = "Kết hợp 3 Quặng Sắt + 2 Tinh Hoa Cổ Ngữ + $goldCost Vàng để rèn Vũ Khí hoặc Giáp Trụ 2★–4★.",
          style = MaterialTheme.typography.bodySmall,
          color = RuneSilver,
        )
        if (LostCourierProgression.FLAG_RESCUED in profile.storyFlags) {
          Text("Ân tình người đưa thư: giảm vĩnh viễn 25% phí Vàng chế tác.", style = MaterialTheme.typography.labelSmall, color = EmeraldVitality)
        }
        if (!canSynthesize) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Thiếu nguyên liệu rèn (Hiện có: ${profile.ironOre}/3 Quặng • ${profile.arcaneEssence}/2 Tinh Hoa • ${profile.gold}/$goldCost Vàng).",
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
          Text("Dung Hợp Chế Tác (3 Quặng • 2 Tinh Hoa • $goldCost Vàng)", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
          .background(Color(0x9E172944))
          .border(1.dp, PortalCyan.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
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
  onHealHeroInfirmary: (HeroRecord) -> Unit = { h -> onRecoverHero(h, false) },
  onServeHeartyMeal: (HeroRecord) -> Unit = { h -> onRecoverHero(h, true) },
  onCruelSynthesis: (String, List<String>) -> Unit = { _, _ -> },
) {
  var activeTab by remember { mutableStateOf(0) } // 0 = Dormitory & Blind Appraisal, 1 = Infirmary & Canteen, 2 = Cruel Synthesis Chamber
  val activeHeroes = remember(heroes) {
    heroes.filter { it.currentAssignment != com.example.data.model.HeroAssignment.SACRIFICED.name }
  }
  val eastGardenUnlocked = SanctuaryProgressionHelper.isExpansionUnlocked(
    profile.storyFlags,
    IslandExpansionMilestone.EAST_CELESTIAL_GARDEN,
  )
  val injuredCount = activeHeroes.count {
    it.status == HeroStatusType.INJURED.name || (it.currentHp in 1 until it.maxHp) || it.morale < 60L
  }

  var selectedInfirmaryHeroId by remember(activeHeroes) {
    val defaultHero = activeHeroes.firstOrNull {
      it.status == HeroStatusType.INJURED.name || (it.currentHp in 1 until it.maxHp) || it.morale < 80L ||
        it.sanityState == com.example.data.model.HeroSanityState.PANICKED.name
    } ?: activeHeroes.firstOrNull()
    mutableStateOf(defaultHero?.id)
  }

  var synthesisTargetId by remember(activeHeroes) {
    mutableStateOf(activeHeroes.maxByOrNull { it.rarity * 100L + it.level }?.id)
  }
  var synthesisMaterialIds by remember(activeHeroes, synthesisTargetId) {
    mutableStateOf(emptyList<String>())
  }

  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // 3-Tab Sanctuary House Navigation Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      OutlinedButton(
        onClick = { activeTab = 0 },
        modifier = Modifier.weight(1f).heightIn(min = 42.dp).testTag("quarters_tab_dormitory"),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = if (activeTab == 0) EmeraldVitality.copy(alpha = 0.24f) else Color(0x88132238),
        ),
        border = BorderStroke(1.dp, if (activeTab == 0) EmeraldVitality else PortalCyan.copy(alpha = 0.45f)),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
      ) {
        Text(
          text = "1. Ký Túc Xá & Giám Định",
          style = MaterialTheme.typography.labelMedium,
          color = if (activeTab == 0) SummonerGold else ParchmentLight,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      OutlinedButton(
        onClick = { activeTab = 1 },
        modifier = Modifier.weight(1f).heightIn(min = 42.dp).testTag("quarters_tab_infirmary"),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = if (activeTab == 1) PortalCyan.copy(alpha = 0.24f) else Color(0x88132238),
        ),
        border = BorderStroke(1.dp, if (activeTab == 1) PortalCyan else PortalCyan.copy(alpha = 0.45f)),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
      ) {
        Text(
          text = "2. Nghỉ Dưỡng & Nhà Ăn",
          style = MaterialTheme.typography.labelMedium,
          color = if (activeTab == 1) SummonerGold else ParchmentLight,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      OutlinedButton(
        onClick = { activeTab = 2 },
        modifier = Modifier.weight(1f).heightIn(min = 42.dp).testTag("quarters_tab_synthesis"),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = if (activeTab == 2) CrimsonBlood.copy(alpha = 0.28f) else Color(0x88132238),
        ),
        border = BorderStroke(1.dp, if (activeTab == 2) CrimsonBlood else PortalCyan.copy(alpha = 0.45f)),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
      ) {
        Text(
          text = "3. Lò Dung Hợp Tàn Khốc",
          style = MaterialTheme.typography.labelMedium,
          color = if (activeTab == 2) SummonerGold else ParchmentLight,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    when (activeTab) {
      // ===================================================
      // TAB 1: KÝ TÚC XÁ & GIÁM ĐỊNH MÙ (DORMITORY & BLIND APPRAISAL)
      // ===================================================
      0 -> {
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x98162842))
                .border(1.dp, EmeraldVitality.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "Ký Túc Xá & Giám Định Mù • Đang điều trị/nghỉ ngơi: $injuredCount Anh hùng",
                style = MaterialTheme.typography.titleSmall,
                color = EmeraldVitality,
              )
              Text(
                text = "Các Anh hùng 1★–2★ mang kỹ năng ẩn [??? (Chưa thức tỉnh)]. Sống sót qua 3 trận leo tháp hoặc trở về với mức HP dưới 15% sẽ tự động thức tỉnh kỹ năng thật!",
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

          items(activeHeroes, key = { it.id }) { hero ->
            val remSec = hero.remainingInjurySeconds(nowEpochSec)
            val needsStatusClear = hero.status == HeroStatusType.INJURED.name
            val isResting = hero.statusType == HeroStatusType.RESTING ||
              hero.currentAssignment == com.example.data.model.HeroAssignment.RESTING.name
            val heroData = com.example.data.model.HeroData.fromRecord(hero, nowEpochSec)
            val survivedRuns = com.example.data.LobbyHeroAssignments.survivedExpeditionCount(hero.bondSummary)
            val lowHpSurvivor = com.example.data.LobbyHeroAssignments.hasReturnedUnder15PctHp(hero.bondSummary) || heroData.isCriticalReturnHp
            val hpRatio = (heroData.currentHP.toFloat() / heroData.maxHP.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            val moraleRatio = (heroData.morale.toFloat() / 100f).coerceIn(0f, 1f)

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xA4182B46))
                .border(
                  1.dp,
                  if (needsStatusClear) AmberWarning else if (isResting) EmeraldVitality else PortalCyan.copy(alpha = 0.42f),
                  RoundedCornerShape(12.dp),
                )
                .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                PixelHeroPortraitAvatar(heroName = hero.name, heroClass = hero.classType, sizeDp = 46.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "${hero.name} (${"★".repeat(hero.rarity.toInt().coerceIn(1, 5))}) • Lv.${hero.level} • Tính cách: ${hero.personalityType.labelVi}",
                    style = MaterialTheme.typography.titleSmall,
                    color = ParchmentLight,
                  )
                  Text(
                    text = "HP: ${heroData.currentHP}/${heroData.maxHP} (${(hpRatio * 100).toInt()}%) • Tinh thần (Morale): ${heroData.morale}% • Tâm trí: ${heroData.sanityState.labelVi}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hpRatio < 0.4f || heroData.morale < 30) AmberWarning else EmeraldVitality,
                  )
                  Text(
                    text = "Kỹ năng ẩn: ${heroData.hiddenTrait.detailedDisplayNameVi}" +
                      if (!heroData.hiddenTrait.unlocked) " (Leo tháp: $survivedRuns/3 trận${if (lowHpSurvivor) " • Đã vượt ngưỡng <15% HP" else ""})" else " ★ ĐÃ THỨC TỈNH",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (heroData.hiddenTrait.unlocked) SummonerGold else PortalCyan,
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

              // Visual HP & Morale Progress Bars
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .clip(RoundedCornerShape(3.dp))
                      .background(Color(0xFF0E1B2E)),
                  ) {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth(hpRatio)
                        .height(6.dp)
                        .background(if (hpRatio < 0.4f) CrimsonBlood else EmeraldVitality),
                    )
                  }
                }
                Column(modifier = Modifier.weight(1f)) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .clip(RoundedCornerShape(3.dp))
                      .background(Color(0xFF0E1B2E)),
                  ) {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth(moraleRatio)
                        .height(6.dp)
                        .background(if (heroData.morale < 30) AmberWarning else PortalCyan),
                    )
                  }
                }
              }
            }
          }
        }
      }

      // ===================================================
      // TAB 2: PHÒNG NGHỈ DƯỠNG & NHÀ ĂN (INFIRMARY & CANTEEN)
      // ===================================================
      1 -> {
        val selectedHero = activeHeroes.firstOrNull { it.id == selectedInfirmaryHeroId } ?: activeHeroes.firstOrNull()
        val healCost = com.example.data.LobbyHeroAssignments.INFIRMARY_HEAL_GOLD_COST
        val mealCost = com.example.data.LobbyHeroAssignments.HEARTY_MEAL_GOLD_COST

        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xA4182B46))
                .border(1.dp, PortalCyan.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "Phòng Nghỉ Dưỡng & Nhà Ăn Nội Khu",
                style = MaterialTheme.typography.titleSmall,
                color = SummonerGold,
              )
              Text(
                text = "Máu (HP) và Tinh thần (Morale) sau khi đi Tháp về KHÔNG tự đầy. Chọn Anh hùng bên dưới để Trị Thương (+50% Max HP) hoặc dùng Bữa Ăn Thịnh Soạn (+40 Morale & xóa Hoảng loạn).",
                style = MaterialTheme.typography.bodySmall,
                color = RuneSilver,
              )

              if (selectedHero != null) {
                val heroData = com.example.data.model.HeroData.fromRecord(selectedHero, nowEpochSec)
                val isPanicked = heroData.sanityState == com.example.data.model.HeroSanityState.PANICKED || heroData.isFrightened
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Color(0x98102038),
                  border = BorderStroke(1.dp, SummonerGold.copy(alpha = 0.65f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                      text = "Đang chọn: ${selectedHero.name} (${selectedHero.personalityType.labelVi})",
                      style = MaterialTheme.typography.titleSmall,
                      color = SummonerGold,
                    )
                    Text(
                      text = "HP hiện tại: ${heroData.currentHP}/${heroData.maxHP} • Tinh thần: ${heroData.morale}/100 • Trạng thái: ${if (isPanicked) "HOẢNG LOẠN (Panic)" else "Bình ổn"}",
                      style = MaterialTheme.typography.bodySmall,
                      color = if (isPanicked || heroData.currentHP < heroData.maxHP) AmberWarning else EmeraldVitality,
                    )

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Button(
                        onClick = { onHealHeroInfirmary(selectedHero) },
                        enabled = !isBusy && profile.gold >= healCost,
                        modifier = Modifier
                          .weight(1f)
                          .heightIn(min = 48.dp)
                          .testTag("infirmary_heal_button"),
                        colors = FantasySkin.buttonColors(containerColor = EmeraldVitality, contentColor = ObsidianVoid),
                      ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Trị Thương (+50% HP • $healCost Vàng)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }

                      Button(
                        onClick = { onServeHeartyMeal(selectedHero) },
                        enabled = !isBusy && profile.gold >= mealCost,
                        modifier = Modifier
                          .weight(1f)
                          .heightIn(min = 48.dp)
                          .testTag("canteen_hearty_meal_button"),
                        colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
                      ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bữa Ăn Thịnh Soạn (+40 Morale • $mealCost Vàng)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }
                    }
                  }
                }
              }
            }
          }

          items(activeHeroes, key = { it.id }) { hero ->
            val heroData = com.example.data.model.HeroData.fromRecord(hero, nowEpochSec)
            val isSelected = selectedHero?.id == hero.id
            val isPanicked = heroData.sanityState == com.example.data.model.HeroSanityState.PANICKED || heroData.isFrightened
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) Color(0xBA1E3658) else Color(0x98162842))
                .border(1.dp, if (isSelected) SummonerGold else PortalCyan.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
                .testTag("infirmary_select_hero_${hero.id}")
                .clickable { selectedInfirmaryHeroId = hero.id }
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              PixelHeroPortraitAvatar(heroName = hero.name, heroClass = hero.classType, sizeDp = 42.dp)
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${hero.name} • HP ${heroData.currentHP}/${heroData.maxHP} • Morale ${heroData.morale}%",
                  style = MaterialTheme.typography.titleSmall,
                  color = if (isSelected) SummonerGold else ParchmentLight,
                )
                Text(
                  text = if (isPanicked) "⚠ Đang Hoảng Loạn (Panic) — Cần dùng Bữa Ăn Thịnh Soạn"
                  else if (heroData.currentHP < heroData.maxHP) "Đang bị thương — Cần Trị Thương (+50% Max HP)"
                  else "Sức khỏe & Tinh thần ổn định",
                  style = MaterialTheme.typography.bodySmall,
                  color = if (isPanicked || heroData.currentHP < heroData.maxHP) AmberWarning else EmeraldVitality,
                )
              }
            }
          }
        }
      }

      // ===================================================
      // TAB 3: LÒ DUNG HỢP TÀN KHỐC (CRUEL SYNTHESIS CHAMBER)
      // ===================================================
      2 -> {
        val targetHero = activeHeroes.firstOrNull { it.id == synthesisTargetId } ?: activeHeroes.firstOrNull()
        val selectedMaterials = activeHeroes.filter { it.id in synthesisMaterialIds && it.id != targetHero?.id }
        val gainAtk = selectedMaterials.sumOf { (it.attack * 0.20).toLong().coerceAtLeast(1L) }
        val gainDef = selectedMaterials.sumOf { (it.defense * 0.20).toLong().coerceAtLeast(1L) }
        val gainHp = selectedMaterials.sumOf { (it.maxHp * 0.20).toLong().coerceAtLeast(5L) }
        val gainExp = selectedMaterials.sumOf { 65L + it.level * 35L + it.rarity * 25L }

        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xB424111E))
                .border(1.5.dp, CrimsonBlood.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "Lò Dung Hợp Tàn Khốc (Permadeath Synthesis)",
                style = MaterialTheme.typography.titleSmall,
                color = SummonerGold,
              )
              Text(
                text = "Chọn 1 Hero Chính và tối đa 3 Hero Hiến Tế. Hero Hiến Tế sẽ bị xóa vĩnh viễn để truyền +20% ATK, DEF, MaxHP & EXP sang Hero Chính (20% cơ hội kế thừa Kỹ năng ẩn). Mọi Hero [Hèn nhát] ở sảnh sẽ bị trừ 10 điểm Morale vì kinh hãi!",
                style = MaterialTheme.typography.bodySmall,
                color = ParchmentLight,
              )

              // 1. Target Hero Selector Row
              Text("1. Chọn Hero Chính (Target):", style = MaterialTheme.typography.labelLarge, color = PortalCyan)
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                activeHeroes.forEach { h ->
                  val isTarget = h.id == targetHero?.id
                  OutlinedButton(
                    onClick = {
                      synthesisTargetId = h.id
                      synthesisMaterialIds = synthesisMaterialIds.filterNot { it == h.id }
                    },
                    modifier = Modifier.heightIn(min = 40.dp).testTag("synthesis_target_${h.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(
                      containerColor = if (isTarget) PortalCyan.copy(alpha = 0.26f) else Color(0x88132238),
                    ),
                    border = BorderStroke(1.dp, if (isTarget) SummonerGold else PortalCyan.copy(alpha = 0.45f)),
                  ) {
                    Text(
                      text = "${h.name} (${"★".repeat(h.rarity.toInt().coerceIn(1, 5))})",
                      color = if (isTarget) SummonerGold else ParchmentLight,
                      maxLines = 1,
                    )
                  }
                }
              }

              // 2. Final Plea / Last Words Box when Material Hero(es) are selected
              if (selectedMaterials.isNotEmpty()) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Color(0xD82A0B14),
                  border = BorderStroke(1.5.dp, AmberWarning),
                  modifier = Modifier.fillMaxWidth().testTag("synthesis_sacrifice_plea_box"),
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = "⚠ TIẾNG VAN XIN / TRĂN TRỐI TỪ LÒ HIẾN TẾ (${selectedMaterials.size}/3):",
                      style = MaterialTheme.typography.labelLarge,
                      color = AmberWarning,
                    )
                    selectedMaterials.forEach { mat ->
                      val matData = com.example.data.model.HeroData.fromRecord(mat, nowEpochSec)
                      val plea = com.example.data.model.ContextAwareDialogueDatabase.lineForSacrificePlea(matData)
                      Text(
                        text = "• ${mat.name} (${mat.personalityType.labelVi}): \"$plea\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = ParchmentLight,
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Dự kiến truyền sang ${targetHero?.name ?: "Hero Chính"}: +$gainAtk ATK • +$gainDef DEF • +$gainHp MaxHP • +$gainExp EXP (20% cơ hội kế thừa Kỹ năng ẩn)",
                      style = MaterialTheme.typography.labelSmall,
                      color = EmeraldVitality,
                    )
                  }
                }
              }

              Button(
                onClick = {
                  val tId = targetHero?.id ?: return@Button
                  if (selectedMaterials.isNotEmpty()) {
                    val matIds = selectedMaterials.map { it.id }
                    synthesisMaterialIds = emptyList()
                    onCruelSynthesis(tId, matIds)
                  }
                },
                enabled = !isBusy && targetHero != null && selectedMaterials.isNotEmpty(),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("confirm_cruel_synthesis_button"),
                colors = FantasySkin.buttonColors(containerColor = CrimsonBlood, contentColor = ParchmentLight),
              ) {
                Text(
                  text = "Xác Nhận Dung Hợp (${selectedMaterials.size}/3 Hiến Tế • Vĩnh Viễn)",
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                )
              }
            }
          }

          item {
            Text(
              text = "2. Chọn tối đa 3 Hero Hiến Tế (Materials — Chạm để đưa vào lò):",
              style = MaterialTheme.typography.titleSmall,
              color = AmberWarning,
            )
          }

          items(activeHeroes.filter { it.id != targetHero?.id }, key = { it.id }) { candidate ->
            val isMaterial = candidate.id in synthesisMaterialIds
            val candidateData = com.example.data.model.HeroData.fromRecord(candidate, nowEpochSec)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isMaterial) Color(0xBA3B121E) else Color(0x98162842))
                .border(1.dp, if (isMaterial) CrimsonBlood else PortalCyan.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
                .testTag("synthesis_material_${candidate.id}")
                .clickable {
                  synthesisMaterialIds = if (isMaterial) {
                    synthesisMaterialIds - candidate.id
                  } else if (synthesisMaterialIds.size < 3) {
                    synthesisMaterialIds + candidate.id
                  } else {
                    synthesisMaterialIds
                  }
                }
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              PixelHeroPortraitAvatar(heroName = candidate.name, heroClass = candidate.classType, sizeDp = 42.dp)
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${candidate.name} (${"★".repeat(candidate.rarity.toInt().coerceIn(1, 5))}) • Tính cách: ${candidate.personalityType.labelVi}",
                  style = MaterialTheme.typography.titleSmall,
                  color = if (isMaterial) AmberWarning else ParchmentLight,
                )
                Text(
                  text = "Truyền 20%: +${(candidate.attack * 0.20).toLong().coerceAtLeast(1L)} ATK • +${(candidate.defense * 0.20).toLong().coerceAtLeast(1L)} DEF • +${(candidate.maxHp * 0.20).toLong().coerceAtLeast(5L)} HP • Kỹ năng: ${candidateData.hiddenTrait.detailedDisplayNameVi}",
                  style = MaterialTheme.typography.bodySmall,
                  color = RuneSilver,
                )
              }
              Text(
                text = if (isMaterial) "🔥 HIẾN TẾ" else "Chọn",
                style = MaterialTheme.typography.labelMedium,
                color = if (isMaterial) AmberWarning else PortalCyan,
              )
            }
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
          .background(Color(0xA4182B46))
          .border(1.dp, SummonerGold.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
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
            color = if (isSel) SummonerGold.copy(alpha = 0.22f) else Color(0x9815263E),
            border = BorderStroke(1.dp, if (isSel) SummonerGold else PortalCyan.copy(alpha = 0.42f)),
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
          .background(Color(0x98162842))
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
