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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.text.font.FontWeight
import com.example.data.FeastBuffHelper
import com.example.data.LobbyHeroAssignments
import com.example.data.SanctuaryProgressionHelper
import com.example.data.model.EquipmentRecord
import com.example.data.model.FacilityType
import com.example.data.model.HeroRecord
import com.example.data.model.HeroAssignment
import com.example.data.model.HeroData
import com.example.data.model.HeroSanityState
import com.example.data.model.LobbyPersonality
import com.example.data.model.ContextAwareDialogueDatabase
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
  onHealHeroInfirmary: (HeroRecord) -> Unit,
  onServeHeartyMeal: (HeroRecord) -> Unit,
  onCruelSynthesis: (String, List<String>) -> Unit,
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
              onAssignHeroActivity = onAssignHeroActivity,
              onOpenHeroProfile = onOpenHeroProfile,
              initialSelectedHeroId = initialSelectedHeroId,
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
  onAssignHeroActivity: (HeroRecord, HeroStatusType) -> Unit,
  onOpenHeroProfile: ((HeroRecord) -> Unit)?,
  initialSelectedHeroId: String?,
  onHealHeroInfirmary: (HeroRecord) -> Unit,
  onServeHeartyMeal: (HeroRecord) -> Unit,
  onCruelSynthesis: (String, List<String>) -> Unit,
) {
  val activeHeroes = remember(heroes) {
    heroes.filter { it.currentAssignment != HeroAssignment.SACRIFICED.name }
  }
  fun defaultInfirmaryHero(): HeroRecord? = activeHeroes.firstOrNull {
    val data = HeroData.fromRecord(it, nowEpochSec)
    it.status == HeroStatusType.INJURED.name || data.currentHP < data.maxHP ||
      data.morale < 100 || data.sanityState in setOf(HeroSanityState.PANICKED, HeroSanityState.ANXIOUS)
  } ?: activeHeroes.firstOrNull()

  var activeTab by remember(initialSelectedHeroId) {
    mutableStateOf(if (initialSelectedHeroId != null) 1 else 0)
  }
  var selectedInfirmaryHeroId by remember(initialSelectedHeroId) {
    mutableStateOf(initialSelectedHeroId ?: defaultInfirmaryHero()?.id)
  }
  var synthesisTargetId by remember {
    mutableStateOf(activeHeroes.maxByOrNull { it.rarity * 100L + it.level }?.id)
  }
  var synthesisMaterialIds by remember { mutableStateOf(emptyList<String>()) }
  val selectedHero = activeHeroes.firstOrNull { it.id == selectedInfirmaryHeroId } ?: defaultInfirmaryHero()
  val targetHero = activeHeroes.firstOrNull { it.id == synthesisTargetId }
    ?: activeHeroes.maxByOrNull { it.rarity * 100L + it.level }
  val selectedMaterials = synthesisMaterialIds.mapNotNull { id ->
    activeHeroes.firstOrNull { it.id == id && it.id != targetHero?.id }
  }
  // A destructive action is bound to the actual records previewed, not just their IDs.
  var synthesisConfirmation by remember(targetHero, selectedMaterials) {
    mutableStateOf<HouseSynthesisConfirmation?>(null)
  }
  val activeHeroIds = activeHeroes.map { it.id }

  // Keep the user's selection when a saved HP/morale update arrives, but remove deleted identities.
  LaunchedEffect(activeHeroIds, targetHero?.id) {
    if (selectedInfirmaryHeroId !in activeHeroIds) selectedInfirmaryHeroId = defaultInfirmaryHero()?.id
    if (synthesisTargetId != targetHero?.id) synthesisTargetId = targetHero?.id
    synthesisMaterialIds = synthesisMaterialIds.filter { it in activeHeroIds && it != targetHero?.id }.distinct().take(3)
  }
  val bonuses = targetHero?.let { LobbyHeroAssignments.synthesisBonuses(it, selectedMaterials) }
  val gainAtk = bonuses?.attack ?: 0L
  val gainDef = bonuses?.defense ?: 0L
  val gainHp = bonuses?.maxHp ?: 0L
  val gainExp = bonuses?.experience ?: 0L
  val canSynthesize = !isBusy && targetHero != null && selectedMaterials.size in 1..3 &&
    selectedMaterials.size == synthesisMaterialIds.size && synthesisMaterialIds.distinct().size == synthesisMaterialIds.size
  val eastGardenUnlocked = SanctuaryProgressionHelper.isExpansionUnlocked(
    profile.storyFlags, IslandExpansionMilestone.EAST_CELESTIAL_GARDEN,
  )
  val tabs = listOf(
    Triple("Ký túc xá", "quarters_tab_dormitory", EmeraldVitality),
    Triple("Điều dưỡng", "quarters_tab_infirmary", PortalCyan),
    Triple("Dung hợp", "quarters_tab_synthesis", CrimsonBlood),
  )

  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      tabs.forEachIndexed { index, (label, tag, accent) ->
        OutlinedButton(
          onClick = { activeTab = index },
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(tag).semantics { selected = activeTab == index },
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (activeTab == index) accent.copy(alpha = 0.24f) else Color(0x88132238),
          ),
          border = BorderStroke(1.dp, if (activeTab == index) accent else PortalCyan.copy(alpha = 0.45f)),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        ) {
          Text(
            label, style = MaterialTheme.typography.labelMedium,
            color = if (activeTab == index) SummonerGold else ParchmentLight,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    when (activeTab) {
      0 -> LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth().testTag("quarters_dormitory_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          HouseSectionCard(accent = EmeraldVitality) {
            Text("Ký Túc Xá & Giám Định Mù", style = MaterialTheme.typography.titleSmall, color = EmeraldVitality)
            Text(
              "Anh hùng 1★–2★ giữ kỹ năng ẩn là ??? cho đến khi sống sót qua 3 trận leo tháp hoặc thực sự trở về với HP dưới 15%. Nghỉ tại Nhà không tự hồi HP hay Tinh thần.",
              style = MaterialTheme.typography.bodySmall, color = RuneSilver,
            )
            if (eastGardenUnlocked) {
              Text("Vườn Đông giảm 25% thời gian chấn thương; HP vẫn cần được điều trị.", style = MaterialTheme.typography.labelSmall, color = PortalCyan)
            }
          }
        }
        if (activeHeroes.isEmpty()) {
          item { Text("Chưa có Anh hùng trong Ký Túc Xá.", color = RuneSilver, modifier = Modifier.testTag("quarters_empty_roster")) }
        }
        items(activeHeroes, key = { it.id }) { hero ->
          val data = HeroData.fromRecord(hero, nowEpochSec)
          val injured = hero.status == HeroStatusType.INJURED.name
          val resting = data.currentAssignment == HeroAssignment.RESTING
          HouseSectionCard(
            modifier = Modifier.testTag("quarters_hero_${hero.id}"),
            accent = if (injured) AmberWarning else if (resting) EmeraldVitality else PortalCyan,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              PixelHeroPortraitAvatar(heroName = hero.name, heroClass = hero.classType, sizeDp = 42.dp)
              Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  "${hero.name} (${"★".repeat(data.rarity)}) • Lv.${data.level} • ${data.personality.houseLabelVi}",
                  style = MaterialTheme.typography.titleSmall, color = ParchmentLight,
                )
                Text(
                  "HP: ${data.currentHP}/${data.maxHP} • Morale: ${data.morale}/100 • ${data.sanityState.labelVi}",
                  style = MaterialTheme.typography.bodySmall, color = if (data.isLowHpNearHouse || data.morale < 30) AmberWarning else EmeraldVitality,
                  modifier = Modifier.testTag("quarters_stats_${hero.id}"),
                )
                Text(
                  "Kỹ năng ẩn: ${data.hiddenTrait.detailedDisplayNameVi}" + if (data.hiddenTrait.unlocked) " ★ ĐÃ THỨC TỈNH" else
                    " • Leo tháp: ${hero.survivedExpeditions}/3 trận" + if (hero.returnedUnder15PctHp) " • Đã trở về dưới 15% HP" else "",
                  style = MaterialTheme.typography.labelSmall, color = if (data.hiddenTrait.unlocked) SummonerGold else PortalCyan,
                  modifier = Modifier.testTag("quarters_trait_${hero.id}"),
                )
              }
              if (onOpenHeroProfile != null) {
                IconButton(
                  onClick = { onOpenHeroProfile(hero) }, enabled = !isBusy,
                  modifier = Modifier.size(48.dp).testTag("quarters_profile_${hero.id}"),
                ) { Icon(Icons.Default.Person, contentDescription = "Mở hồ sơ ${hero.name}", tint = SummonerGold) }
              }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              HouseStatBar(data.currentHP.toFloat() / data.maxHP, "quarters_hp_${hero.id}", EmeraldVitality, Modifier.weight(1f))
              HouseStatBar(data.morale / 100f, "quarters_morale_${hero.id}", PortalCyan, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { selectedInfirmaryHeroId = hero.id; activeTab = 1 },
                enabled = !isBusy,
                modifier = Modifier.heightIn(min = 48.dp).testTag("quarters_treat_${hero.id}"),
              ) { Text("Điều trị / Bữa ăn", color = SummonerGold, maxLines = 1) }
              if (!injured) {
                OutlinedButton(
                  onClick = { onAssignHeroActivity(hero, if (resting) HeroStatusType.READY else HeroStatusType.RESTING) },
                  enabled = !isBusy,
                  modifier = Modifier.heightIn(min = 48.dp).testTag("quarters_rest_${hero.id}"),
                ) { Text(if (resting) "Ra sảnh" else "Nghỉ ngơi", color = EmeraldVitality, maxLines = 1) }
              }
            }
          }
        }
      }

      1 -> LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth().testTag("quarters_infirmary_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          HouseSectionCard(accent = PortalCyan) {
            Text("Phòng Nghỉ Dưỡng & Nhà Ăn", style = MaterialTheme.typography.titleSmall, color = SummonerGold)
            Text("Chọn Anh hùng cần chăm sóc. Mỗi thao tác chỉ trừ Vàng khi thành công và được lưu ngay.", style = MaterialTheme.typography.bodySmall, color = RuneSilver)
            if (selectedHero != null) {
              val data = HeroData.fromRecord(selectedHero, nowEpochSec)
              Text("Đang chọn: ${selectedHero.name} (${data.personality.houseLabelVi})", style = MaterialTheme.typography.titleSmall, color = SummonerGold, modifier = Modifier.testTag("infirmary_selected_hero"))
              Text(
                "HP: ${data.currentHP}/${data.maxHP} • Morale: ${data.morale}/100 • ${data.sanityState.labelVi}",
                style = MaterialTheme.typography.bodySmall, color = ParchmentLight, modifier = Modifier.testTag("infirmary_selected_stats"),
              )
            } else {
              Text("Chưa có Anh hùng để điều trị.", color = RuneSilver, modifier = Modifier.testTag("quarters_empty_roster"))
            }
          }
        }
        if (selectedHero != null) {
          item {
            val data = HeroData.fromRecord(selectedHero, nowEpochSec)
            val needsHealing = data.currentHP < data.maxHP || selectedHero.status == HeroStatusType.INJURED.name
            val needsMeal = data.morale < 100 || data.sanityState == HeroSanityState.PANICKED || data.sanityState == HeroSanityState.ANXIOUS
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = { onHealHeroInfirmary(selectedHero) },
                enabled = !isBusy && needsHealing && profile.gold >= LobbyHeroAssignments.INFIRMARY_HEAL_GOLD_COST,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("infirmary_heal_button"),
                colors = FantasySkin.buttonColors(containerColor = EmeraldVitality, contentColor = ObsidianVoid),
              ) {
                Text("Trị Thương: +50% Max HP • ${LobbyHeroAssignments.INFIRMARY_HEAL_GOLD_COST} Vàng", maxLines = 2, overflow = TextOverflow.Ellipsis)
              }
              Button(
                onClick = { onServeHeartyMeal(selectedHero) },
                enabled = !isBusy && needsMeal && profile.gold >= LobbyHeroAssignments.HEARTY_MEAL_GOLD_COST,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("canteen_hearty_meal_button"),
                colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
              ) {
                Text("Bữa Ăn: +40 Morale, xóa Hoảng loạn • ${LobbyHeroAssignments.HEARTY_MEAL_GOLD_COST} Vàng", maxLines = 2, overflow = TextOverflow.Ellipsis)
              }
              if (isBusy) Text("Đang lưu thay đổi…", color = RuneSilver, style = MaterialTheme.typography.labelSmall)
            }
          }
        }
        items(activeHeroes, key = { it.id }) { hero ->
          val data = HeroData.fromRecord(hero, nowEpochSec)
          val isSelected = selectedHero?.id == hero.id
          HouseSectionCard(
            modifier = Modifier.testTag("infirmary_select_hero_${hero.id}").selectable(
              selected = isSelected, enabled = !isBusy, role = Role.RadioButton,
              onClick = { selectedInfirmaryHeroId = hero.id },
            ),
            accent = if (isSelected) SummonerGold else PortalCyan,
          ) {
            Text("${hero.name} • ${data.personality.houseLabelVi}", style = MaterialTheme.typography.titleSmall, color = if (isSelected) SummonerGold else ParchmentLight)
            Text("HP: ${data.currentHP}/${data.maxHP} • Morale: ${data.morale}/100 • ${data.sanityState.labelVi}", style = MaterialTheme.typography.bodySmall, color = RuneSilver)
          }
        }
      }

      2 -> LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth().testTag("quarters_synthesis_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          HouseSectionCard(accent = CrimsonBlood) {
            Text("Lò Dung Hợp Tàn Khốc", style = MaterialTheme.typography.titleSmall, color = SummonerGold)
            Text(
              "Chọn 1 Hero Chính và 1–3 Hero Hiến Tế. Hiến tế xóa vĩnh viễn nhân vật, truyền 20% ATK, DEF, MaxHP và cộng EXP; có 20% cơ hội kế thừa kỹ năng ẩn. Hero hèn nhát còn sống bị trừ 10 Morale.",
              style = MaterialTheme.typography.bodySmall, color = ParchmentLight,
            )
            Text("1. Hero Chính:", style = MaterialTheme.typography.labelLarge, color = PortalCyan)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              activeHeroes.forEach { hero ->
                val isTarget = hero.id == targetHero?.id
                OutlinedButton(
                  onClick = {
                    synthesisTargetId = hero.id
                    synthesisMaterialIds = synthesisMaterialIds.filterNot { it == hero.id }
                    synthesisConfirmation = null
                  },
                  enabled = !isBusy,
                  modifier = Modifier.heightIn(min = 48.dp).testTag("synthesis_target_${hero.id}").semantics { selected = isTarget },
                  border = BorderStroke(1.dp, if (isTarget) SummonerGold else PortalCyan.copy(alpha = 0.45f)),
                ) { Text(hero.name, color = if (isTarget) SummonerGold else ParchmentLight, maxLines = 1) }
              }
            }
            Text("2. Hero Hiến Tế: ${selectedMaterials.size}/3 — Chạm để chọn hoặc bỏ chọn", style = MaterialTheme.typography.labelMedium, color = AmberWarning)
            if (activeHeroes.size < 2) Text("Cần ít nhất 2 Anh hùng để dung hợp.", color = RuneSilver, modifier = Modifier.testTag("synthesis_insufficient_roster"))
          }
        }
        items(activeHeroes.filter { it.id != targetHero?.id }, key = { it.id }) { candidate ->
          val isMaterial = candidate.id in synthesisMaterialIds
          val data = HeroData.fromRecord(candidate, nowEpochSec)
          HouseSectionCard(
            modifier = Modifier.testTag("synthesis_material_${candidate.id}").toggleable(
              value = isMaterial,
              enabled = !isBusy && (isMaterial || synthesisMaterialIds.size < 3),
              role = Role.Checkbox,
              onValueChange = { selected ->
                synthesisMaterialIds = if (selected) synthesisMaterialIds + candidate.id else synthesisMaterialIds - candidate.id
                synthesisConfirmation = null
              },
            ),
            accent = if (isMaterial) CrimsonBlood else PortalCyan,
          ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${candidate.name} (${"★".repeat(data.rarity)}) • ${data.personality.houseLabelVi}", style = MaterialTheme.typography.titleSmall, color = if (isMaterial) AmberWarning else ParchmentLight)
                Text("ATK ${candidate.attack} • DEF ${candidate.defense} • MaxHP ${candidate.maxHp}", style = MaterialTheme.typography.bodySmall, color = RuneSilver)
                Text("Kỹ năng: ${data.hiddenTrait.detailedDisplayNameVi}", style = MaterialTheme.typography.labelSmall, color = RuneSilver)
              }
              Text(if (isMaterial) "ĐÃ CHỌN" else "Chọn", style = MaterialTheme.typography.labelMedium, color = if (isMaterial) AmberWarning else PortalCyan)
            }
            if (isMaterial) {
              Text(
                "${candidate.name}: “${ContextAwareDialogueDatabase.lineForSacrificePlea(data)}”",
                modifier = Modifier.fillMaxWidth().testTag("synthesis_plea_${candidate.id}"),
                style = MaterialTheme.typography.bodySmall, color = AmberWarning,
              )
            }
          }
        }
        if (selectedMaterials.isNotEmpty()) {
          item {
            HouseSectionCard(modifier = Modifier.testTag("synthesis_sacrifice_plea_box"), accent = AmberWarning) {
              Text("Đã chọn ${selectedMaterials.size}/3 Hero Hiến Tế cho ${targetHero?.name.orEmpty()}", style = MaterialTheme.typography.titleSmall, color = AmberWarning)
              Text("Truyền +$gainAtk ATK • +$gainDef DEF • +$gainHp MaxHP • +$gainExp EXP", style = MaterialTheme.typography.bodySmall, color = EmeraldVitality, modifier = Modifier.testTag("synthesis_preview_stats"))
              Text("MaxHP tăng không hồi HP hiện tại. Hiến tế không thể hoàn tác.", style = MaterialTheme.typography.labelSmall, color = ParchmentLight)
            }
          }
        }
        item {
          Button(
            onClick = {
              if (canSynthesize) {
                synthesisConfirmation = HouseSynthesisConfirmation(targetHero, selectedMaterials)
              }
            },
            enabled = canSynthesize,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("confirm_cruel_synthesis_button"),
            colors = FantasySkin.buttonColors(containerColor = CrimsonBlood, contentColor = ParchmentLight),
          ) { Text("Xác Nhận Dung Hợp (${selectedMaterials.size}/3)", fontWeight = FontWeight.Bold, maxLines = 2) }
        }
      }
    }
  }

  val confirmation = synthesisConfirmation
  if (confirmation != null && targetHero != null && confirmation.target == targetHero &&
    confirmation.materials == selectedMaterials && selectedMaterials.size in 1..3
  ) {
    AlertDialog(
      onDismissRequest = { if (!isBusy) synthesisConfirmation = null },
      modifier = Modifier.testTag("cruel_synthesis_confirmation"),
      containerColor = ObsidianVoid,
      title = { Text("Xác nhận hiến tế vĩnh viễn", color = SummonerGold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Hero Chính: ${targetHero.name}", color = ParchmentLight)
          Text("Xóa vĩnh viễn: ${selectedMaterials.joinToString { it.name }}.", color = AmberWarning)
          Text("+$gainAtk ATK • +$gainDef DEF • +$gainHp MaxHP • +$gainExp EXP. Hero hèn nhát còn sống mất 10 Morale. Không thể khôi phục Hero Hiến Tế.", color = ParchmentLight)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (synthesisConfirmation == confirmation && canSynthesize) {
              val targetId = targetHero.id
              val materialIds = selectedMaterials.map { it.id }
              synthesisConfirmation = null
              synthesisMaterialIds = emptyList()
              onCruelSynthesis(targetId, materialIds)
            }
          },
          enabled = canSynthesize,
          modifier = Modifier.heightIn(min = 48.dp).testTag("execute_cruel_synthesis_button"),
          colors = FantasySkin.buttonColors(containerColor = CrimsonBlood, contentColor = ParchmentLight),
        ) { Text("Hiến tế vĩnh viễn") }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { synthesisConfirmation = null }, enabled = !isBusy,
          modifier = Modifier.heightIn(min = 48.dp).testTag("cancel_cruel_synthesis_button"),
        ) { Text("Hủy", color = ParchmentLight) }
      },
    )
  }
}

private data class HouseSynthesisConfirmation(val target: HeroRecord, val materials: List<HeroRecord>)

@Composable
private fun HouseSectionCard(
  modifier: Modifier = Modifier,
  accent: Color,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xE5182B46))
      .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(12.dp)).padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
    content = content,
  )
}

@Composable
private fun HouseStatBar(fraction: Float, tag: String, color: Color, modifier: Modifier = Modifier) {
  val value = fraction.coerceIn(0f, 1f)
  Box(
    modifier.height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF0E1B2E))
      .testTag(tag).semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) },
  ) {
    Box(Modifier.fillMaxWidth(value).height(6.dp).background(color))
  }
}

private val LobbyPersonality.houseLabelVi: String
  get() = when (this) {
    LobbyPersonality.COWARD -> "Hèn nhát"
    LobbyPersonality.ARROGANT -> "Ngạo mạn"
    LobbyPersonality.LOYAL -> "Trung thành"
    LobbyPersonality.LONER -> "Cô độc"
    LobbyPersonality.REBEL -> "Bất tuân"
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
