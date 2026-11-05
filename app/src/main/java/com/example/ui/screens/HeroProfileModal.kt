package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import com.example.ui.components.FantasyButton as Button
import com.example.ui.components.FantasySkin
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SanctuaryProgressionHelper
import com.example.data.model.EquipmentRecord
import com.example.data.model.FacilityType
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.HeroStatusType
import com.example.data.model.IslandExpansionMilestone
import com.example.data.model.MasterProfile
import com.example.data.model.TrainingFocus
import com.example.game.lobby.HeroTaskType
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.ui.components.PixelHeroPortraitAvatar
import com.example.ui.components.ResourcePillBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ArcaneViolet
import com.example.ui.theme.ArcaneVioletDeep
import com.example.ui.theme.AstralSlate
import com.example.ui.theme.BorderRune
import com.example.ui.theme.CrimsonBlood
import com.example.ui.theme.EmeraldVitality
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.RuneSilver
import com.example.ui.theme.SanctuaryStone
import com.example.ui.theme.SanctuaryUi
import com.example.ui.theme.SummonerGold
import kotlinx.coroutines.delay

fun heroClassAccentColor(heroClass: HeroClassType): Color = when (heroClass) {
  HeroClassType.KNIGHT -> PortalCyan
  HeroClassType.ARCHER -> EmeraldVitality
  HeroClassType.MAGE -> SanctuaryUi.violetOnDark
  HeroClassType.ASSASSIN -> Color(0xFFF43F5E)
  HeroClassType.PRIEST -> SummonerGold
  HeroClassType.BERSERKER -> AmberWarning
}

fun facilityAccentColor(facility: FacilityType): Color = when (facility) {
  FacilityType.TOWER_GATE -> PortalCyan
  FacilityType.SUMMONING_HALL -> SanctuaryUi.violetOnDark
  FacilityType.TRAINING_GROUND -> AmberWarning
  FacilityType.ARMORY -> PortalCyan
  FacilityType.HERO_QUARTERS -> EmeraldVitality
  FacilityType.SYNTHESIS_LAB -> EmeraldVitality
  FacilityType.RESTAURANT -> SummonerGold
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContextualFacilityPromptCard(
  facility: FacilityType,
  onEnterInterior: (() -> Unit)?,
  onOpenManagement: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accent = facilityAccentColor(facility)
  val isEnterable = facility in SanctuaryLobbyEngine.ENTERABLE_FACILITIES && onEnterInterior != null

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = FantasySkin.Panel.copy(alpha = 0.96f),
    border = BorderStroke(1.5.dp, accent.copy(alpha = 0.85f)),
    tonalElevation = 8.dp,
    modifier = modifier
      .widthIn(max = 390.dp)
      .testTag("contextual_facility_prompt"),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = facility.titleVi,
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = facility.subtitleVi,
            style = MaterialTheme.typography.labelSmall,
            color = RuneSilver,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(36.dp).testTag("dismiss_facility_prompt_button"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Đóng gợi ý công trình", tint = ParchmentLight, modifier = Modifier.size(18.dp))
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (isEnterable && onEnterInterior != null) {
          Button(
            onClick = onEnterInterior,
            modifier = Modifier
              .heightIn(min = 44.dp)
              .testTag("enter_facility_interior_button"),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = FantasySkin.buttonColors(containerColor = accent, contentColor = ObsidianVoid),
          ) {
            Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Bước Vào Nội Thất", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
          }
        }

        val manageText = when {
          facility == FacilityType.TOWER_GATE -> "Mở Cổng Tháp ⚔"
          isEnterable -> "Quản Lý Nhanh"
          else -> "Mở Bảng Quản Lý"
        }

        if (isEnterable) {
          OutlinedButton(
            onClick = onOpenManagement,
            modifier = Modifier
              .heightIn(min = 44.dp)
              .testTag("open_facility_panel_button"),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            border = BorderStroke(1.dp, SummonerGold.copy(alpha = 0.7f)),
          ) {
            Text(manageText, style = MaterialTheme.typography.labelMedium, color = SummonerGold, maxLines = 1)
          }
        } else {
          Button(
            onClick = onOpenManagement,
            modifier = Modifier
              .heightIn(min = 44.dp)
              .testTag("open_facility_panel_button"),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(manageText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedHeroCompactCard(
  hero: HeroRecord,
  equipment: List<EquipmentRecord>,
  nowEpochSec: Long,
  isInsideInterior: Boolean,
  onOpenFullProfile: () -> Unit,
  onCommandHold: () -> Unit,
  onCommandTask: (HeroTaskType) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val classAccent = heroClassAccentColor(hero.classType)
  val injured = hero.isCurrentlyInjured(nowEpochSec)
  val hpTotal = hero.totalMaxHp(equipment)
  val atkTotal = hero.totalAttack(equipment)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = FantasySkin.Panel.copy(alpha = 0.96f),
    border = BorderStroke(1.2.dp, classAccent.copy(alpha = 0.85f)),
    modifier = modifier
      .widthIn(max = 390.dp)
      .testTag("selected_hero_compact_card"),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          PixelHeroPortraitAvatar(heroName = hero.name, heroClass = hero.classType, sizeDp = 36.dp)
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${hero.name} • Lv.${hero.level} (${"★".repeat(hero.rarity.toInt().coerceIn(1, 5))})",
              style = MaterialTheme.typography.labelLarge,
              color = SummonerGold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${hero.classType.labelVi} • HP $hpTotal • ATK $atkTotal • Sĩ khí ${hero.morale}%",
              style = MaterialTheme.typography.labelSmall,
              color = classAccent,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = hero.consistentActivityLabelVi(nowEpochSec),
              style = MaterialTheme.typography.labelSmall,
              color = if (injured) AmberWarning else ParchmentLight,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(32.dp).testTag("dismiss_hero_card_button"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Bỏ chọn Anh hùng", tint = RuneSilver, modifier = Modifier.size(16.dp))
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Button(
          onClick = onOpenFullProfile,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
          modifier = Modifier
            .heightIn(min = 36.dp)
            .testTag("open_hero_profile_button"),
          colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
          shape = RoundedCornerShape(8.dp),
        ) {
          Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Hồ Sơ Chi Tiết", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }

        if (!isInsideInterior && !injured) {
          OutlinedButton(
            onClick = onCommandHold,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.heightIn(min = 36.dp),
            shape = RoundedCornerShape(8.dp),
          ) {
            Text("Giữ Vị Trí", style = MaterialTheme.typography.labelSmall, color = AmberWarning)
          }
          OutlinedButton(
            onClick = { onCommandTask(HeroTaskType.TRAIN) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.heightIn(min = 36.dp),
            shape = RoundedCornerShape(8.dp),
          ) {
            Text("Sân Tập", style = MaterialTheme.typography.labelSmall, color = PortalCyan)
          }
          OutlinedButton(
            onClick = { onCommandTask(HeroTaskType.DINING) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.heightIn(min = 36.dp),
            shape = RoundedCornerShape(8.dp),
          ) {
            Text("Nhà Ăn", style = MaterialTheme.typography.labelSmall, color = EmeraldVitality)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroFullProfileModal(
  hero: HeroRecord,
  profile: MasterProfile,
  allHeroes: List<HeroRecord>,
  equipment: List<EquipmentRecord>,
  isBusy: Boolean,
  onClose: () -> Unit,
  onToggleSquad: (HeroRecord) -> Unit,
  onTrainHeroWithFocus: (HeroRecord, TrainingFocus) -> Unit,
  onEquipItem: (HeroRecord, EquipmentRecord) -> Unit,
  onUnequipItem: (HeroRecord, String) -> Unit,
  onAssignHeroActivity: (HeroRecord, HeroStatusType) -> Unit,
  onRecoverHero: (HeroRecord, Boolean) -> Unit,
) {
  BackHandler(onBack = onClose)

  var selectedTab by remember { mutableIntStateOf(0) }
  var selectedFocus by remember { mutableStateOf(TrainingFocus.BALANCED) }
  var nowEpochSec by remember { mutableLongStateOf(System.currentTimeMillis() / 1000L) }

  LaunchedEffect(hero.id) {
    while (true) {
      nowEpochSec = System.currentTimeMillis() / 1000L
      delay(1000L)
    }
  }

  val classAccent = heroClassAccentColor(hero.classType)
  val injured = hero.isCurrentlyInjured(nowEpochSec)
  val remInjurySec = hero.remainingInjurySeconds(nowEpochSec)

  val tabs = listOf(
    "hero_tab_overview" to "Tổng Quan",
    "hero_tab_stats" to "Chỉ Số",
    "hero_tab_equipment" to "Trang Bị",
    "hero_tab_skills" to "Kỹ Năng",
    "hero_tab_activity" to "Hoạt Động",
  )

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid.copy(alpha = 0.88f))
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag("hero_full_profile_modal"),
    contentAlignment = Alignment.Center,
  ) {
    val isShort = maxHeight < 460.dp

    Surface(
      shape = RoundedCornerShape(18.dp),
      color = FantasySkin.Panel,
      tonalElevation = 10.dp,
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 760.dp)
        .heightIn(max = 660.dp)
        .border(1.5.dp, classAccent.copy(alpha = 0.75f), RoundedCornerShape(18.dp)),
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = if (isShort) 10.dp else 14.dp),
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
          ) {
            PixelHeroPortraitAvatar(heroName = hero.name, heroClass = hero.classType, sizeDp = if (isShort) 42.dp else 52.dp)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${hero.name} — ${hero.title}",
                style = if (isShort) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                color = SummonerGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${hero.classType.labelVi} • Lv.${hero.level} • ${"★".repeat(hero.rarity.toInt().coerceIn(1, 5))}",
                style = MaterialTheme.typography.labelMedium,
                color = classAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          IconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp).testTag("close_hero_profile_button"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Đóng hồ sơ Anh hùng", tint = ParchmentLight)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5-Tab Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          tabs.forEachIndexed { idx, (tag, label) ->
            val active = selectedTab == idx
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (active) classAccent.copy(alpha = 0.25f) else ObsidianVoid.copy(alpha = 0.72f),
              border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) classAccent else BorderRune),
              modifier = Modifier
                .heightIn(min = 42.dp)
                .testTag(tag)
                .clickable { selectedTab = idx },
            ) {
              Box(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelLarge,
                  color = if (active) classAccent else ParchmentLight,
                  fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                  maxLines = 1,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (selectedTab) {
            0 -> HeroOverviewTab(
              hero = hero,
              nowEpochSec = nowEpochSec,
              injured = injured,
              isBusy = isBusy,
              onToggleSquad = { onToggleSquad(hero) },
            )
            1 -> HeroStatsTab(
              hero = hero,
              profile = profile,
              equipment = equipment,
              selectedFocus = selectedFocus,
              onSelectFocus = { selectedFocus = it },
              isBusy = isBusy,
              onTrain = { onTrainHeroWithFocus(hero, selectedFocus) },
            )
            2 -> HeroEquipmentTab(
              hero = hero,
              allHeroes = allHeroes,
              equipment = equipment,
              isBusy = isBusy,
              onEquipItem = { item -> onEquipItem(hero, item) },
              onUnequipSlot = { slot -> onUnequipItem(hero, slot) },
            )
            3 -> HeroSkillsTab(
              hero = hero,
              profile = profile,
              isBusy = isBusy,
              onUpgradeSkill = { onTrainHeroWithFocus(hero, TrainingFocus.ARCANE_SKILL) },
            )
            else -> HeroActivityRecoveryTab(
              hero = hero,
              profile = profile,
              nowEpochSec = nowEpochSec,
              remInjurySec = remInjurySec,
              isBusy = isBusy,
              onAssignActivity = { target -> onAssignHeroActivity(hero, target) },
              onRecover = { instant -> onRecoverHero(hero, instant) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HeroOverviewTab(
  hero: HeroRecord,
  nowEpochSec: Long,
  injured: Boolean,
  isBusy: Boolean,
  onToggleSquad: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = AstralSlate,
      border = BorderStroke(1.dp, BorderRune),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Vai Trò Chiến Đấu", style = MaterialTheme.typography.labelLarge, color = SummonerGold)
        Text(hero.classType.roleSummaryVi, style = MaterialTheme.typography.bodyMedium, color = ParchmentLight)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Tính cách: ${hero.personalityType.labelVi} ${hero.personalityType.emotionTagVi}",
          style = MaterialTheme.typography.labelMedium,
          color = PortalCyan,
        )
        Text(
          text = "«${hero.personalityType.sampleQuoteVi}»",
          style = MaterialTheme.typography.bodySmall,
          color = RuneSilver,
        )
        if (hero.bondSummary.isNotBlank()) {
          Text(
            text = "Khế ước với Master: ${hero.bondSummary}",
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldVitality,
          )
        }
      }
    }

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = ObsidianVoid.copy(alpha = 0.72f),
      border = BorderStroke(1.dp, BorderRune),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Sĩ Khí Hiện Tại", style = MaterialTheme.typography.labelLarge, color = ParchmentLight)
          Text("${hero.morale}%", style = MaterialTheme.typography.labelLarge, color = if (hero.morale >= 85L) EmeraldVitality else AmberWarning)
        }
        LinearProgressIndicator(
          progress = { (hero.morale.toFloat() / 100f).coerceIn(0f, 1f) },
          modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
          color = if (hero.morale >= 85L) EmeraldVitality else AmberWarning,
          trackColor = SanctuaryStone,
        )
        Text(
          text = "Trạng thái thực tế: ${hero.consistentActivityLabelVi(nowEpochSec)}",
          style = MaterialTheme.typography.bodySmall,
          color = if (injured) AmberWarning else PortalCyan,
        )
      }
    }

    if (injured) {
      Text(
        text = "⚠ Anh hùng đang bị thương và nghỉ trên giường Khu Nghỉ Ngơi — không thể đưa vào Đội Xuất Chinh cho đến khi hồi phục.",
        style = MaterialTheme.typography.bodySmall,
        color = AmberWarning,
      )
    }

    Button(
      onClick = onToggleSquad,
      enabled = !isBusy && !injured,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("profile_toggle_squad_button"),
      colors = FantasySkin.buttonColors(
        containerColor = if (hero.inActiveSquad) EmeraldVitality else SummonerGold,
        contentColor = ObsidianVoid,
      ),
    ) {
      Text(
        text = when {
          injured -> "Đang Dưỡng Thương Tại Khu Nghỉ Ngơi"
          hero.inActiveSquad -> "✓ Đang Trong Đội Xuất Chinh (Chạm Để Rút)"
          else -> "+ Đưa Vào Đội Xuất Chinh Tháp"
        },
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroStatsTab(
  hero: HeroRecord,
  profile: MasterProfile,
  equipment: List<EquipmentRecord>,
  selectedFocus: TrainingFocus,
  onSelectFocus: (TrainingFocus) -> Unit,
  isBusy: Boolean,
  onTrain: () -> Unit,
) {
  val totalHp = hero.totalMaxHp(equipment)
  val totalAtk = hero.totalAttack(equipment)
  val totalDef = hero.totalDefense(equipment)
  val totalSpd = hero.totalSpeed(equipment)
  val combatPower = totalAtk * 3 + totalDef * 2 + totalHp / 4
  val goldCost = 80L + hero.level * 35L
  val skillManaCost = 25L + hero.skillLevel * 10L
  val canAfford = if (selectedFocus.isSkillUpgrade) {
    profile.manaCrystals >= skillManaCost && profile.arcaneEssence >= 1L
  } else {
    profile.gold >= goldCost
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = AstralSlate,
      border = BorderStroke(1.dp, SummonerGold.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Tổng Lực Chiến Thực Tế", style = MaterialTheme.typography.titleSmall, color = SummonerGold)
          Text("$combatPower CP", style = MaterialTheme.typography.titleSmall, color = EmeraldVitality, fontWeight = FontWeight.Bold)
        }
        StatComparisonRow("Sinh Lực (HP)", hero.maxHp.toInt(), totalHp, EmeraldVitality)
        StatComparisonRow("Công Kích (ATK)", hero.attack.toInt(), totalAtk, SummonerGold)
        StatComparisonRow("Phòng Ngự (DEF)", hero.defense.toInt(), totalDef, PortalCyan)
        StatComparisonRow("Tốc Độ (SPD)", hero.speed.toInt(), totalSpd, SanctuaryUi.violetOnDark)
      }
    }

    Text("Chọn Giáo Án Huấn Luyện:", style = MaterialTheme.typography.labelLarge, color = PortalCyan)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      TrainingFocus.entries.filter { !it.isSkillUpgrade }.forEach { focus ->
        val active = selectedFocus == focus
        Surface(
          shape = RoundedCornerShape(9.dp),
          color = if (active) SummonerGold.copy(alpha = 0.24f) else ObsidianVoid,
          border = BorderStroke(1.dp, if (active) SummonerGold else BorderRune),
          modifier = Modifier
            .heightIn(min = 40.dp)
            .clickable { onSelectFocus(focus) },
        ) {
          Text(
            text = focus.shortVi,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) SummonerGold else ParchmentLight,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
          )
        }
      }
    }

    Text(selectedFocus.descVi, style = MaterialTheme.typography.bodySmall, color = RuneSilver)

    if (!canAfford) {
      Text(
        text = "Cần $goldCost Vàng để rèn cấp (hiện có ${profile.gold} Vàng).",
        style = MaterialTheme.typography.labelSmall,
        color = AmberWarning,
      )
    }

    Button(
      onClick = onTrain,
      enabled = !isBusy && canAfford,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("profile_train_focus_button"),
      colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
    ) {
      Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Rèn Cấp ${selectedFocus.shortVi} ($goldCost Vàng)", fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun StatComparisonRow(label: String, baseVal: Int, totalVal: Int, accent: Color) {
  val diff = totalVal - baseVal
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = ParchmentLight)
    Text(
      text = if (diff >= 0) "$totalVal (Gốc $baseVal +$diff)" else "$totalVal (Gốc $baseVal $diff)",
      style = MaterialTheme.typography.labelMedium,
      color = accent,
    )
  }
}

@Composable
private fun HeroEquipmentTab(
  hero: HeroRecord,
  allHeroes: List<HeroRecord>,
  equipment: List<EquipmentRecord>,
  isBusy: Boolean,
  onEquipItem: (EquipmentRecord) -> Unit,
  onUnequipSlot: (String) -> Unit,
) {
  val equippedWeapon = equipment.firstOrNull { it.id == hero.equippedWeaponId }
  val equippedArmor = equipment.firstOrNull { it.id == hero.equippedArmorId }

  val curHp = hero.totalMaxHp(equipment)
  val curAtk = hero.totalAttack(equipment)
  val curDef = hero.totalDefense(equipment)
  val curSpd = hero.totalSpeed(equipment)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        EquippedSlotBox(
          slotLabel = "Vũ Khí",
          item = equippedWeapon,
          unequipTag = "unequip_weapon_button",
          isBusy = isBusy,
          onUnequip = { onUnequipSlot("WEAPON") },
          modifier = Modifier.weight(1f),
        )
        EquippedSlotBox(
          slotLabel = "Giáp Trụ",
          item = equippedArmor,
          unequipTag = "unequip_armor_button",
          isBusy = isBusy,
          onUnequip = { onUnequipSlot("ARMOR") },
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      Text(
        text = "Kho Trang Bị & Xem Trước Thay Đổi Chỉ Số (${equipment.size}):",
        style = MaterialTheme.typography.labelLarge,
        color = PortalCyan,
      )
    }

    items(equipment, key = { it.id }) { item ->
      val previewHero = hero.previewWithCandidateItem(item)
      val dHp = previewHero.totalMaxHp(equipment) - curHp
      val dAtk = previewHero.totalAttack(equipment) - curAtk
      val dDef = previewHero.totalDefense(equipment) - curDef
      val dSpd = previewHero.totalSpeed(equipment) - curSpd
      val holder = allHeroes.firstOrNull { it.id == item.equippedByHeroId }
      val alreadyEquippedHere = item.equippedByHeroId == hero.id

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = AstralSlate,
        border = BorderStroke(1.dp, if (alreadyEquippedHere) SummonerGold else BorderRune),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = if (item.slot == "WEAPON") SummonerGold else PortalCyan,
            modifier = Modifier.size(24.dp),
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "${item.name} +${item.level} (${"★".repeat(item.rarity.toInt().coerceIn(1, 5))})",
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
            if (!alreadyEquippedHere) {
              val diffParts = buildList {
                if (dHp != 0) add("${if (dHp > 0) "+" else ""}$dHp HP")
                if (dAtk != 0) add("${if (dAtk > 0) "+" else ""}$dAtk ATK")
                if (dDef != 0) add("${if (dDef > 0) "+" else ""}$dDef DEF")
                if (dSpd != 0) add("${if (dSpd > 0) "+" else ""}$dSpd SPD")
              }
              val netGain = dAtk * 3 + dDef * 2 + dHp / 4 + dSpd
              Text(
                text = if (diffParts.isEmpty()) "Không đổi chỉ số" else "So sánh: ${diffParts.joinToString(" • ")}",
                style = MaterialTheme.typography.labelSmall,
                color = if (netGain >= 0) EmeraldVitality else CrimsonBlood,
              )
            }
            if (holder != null) {
              Text(
                text = if (alreadyEquippedHere) "Đang trang bị trên ${hero.name}" else "Đang mặc bởi: ${holder.name} (sẽ chuyển sang)",
                style = MaterialTheme.typography.labelSmall,
                color = if (alreadyEquippedHere) SummonerGold else AmberWarning,
              )
            }
          }
          Button(
            onClick = { onEquipItem(item) },
            enabled = !isBusy && !alreadyEquippedHere,
            modifier = Modifier
              .heightIn(min = 46.dp)
              .testTag("profile_equip_${item.id}"),
            colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
          ) {
            Text(if (alreadyEquippedHere) "Đã Mặc" else "Trang Bị", maxLines = 1)
          }
        }
      }
    }
  }
}

@Composable
private fun EquippedSlotBox(
  slotLabel: String,
  item: EquipmentRecord?,
  unequipTag: String,
  isBusy: Boolean,
  onUnequip: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = ObsidianVoid.copy(alpha = 0.76f),
    border = BorderStroke(1.dp, if (item != null) SummonerGold.copy(alpha = 0.65f) else BorderRune),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(slotLabel, style = MaterialTheme.typography.labelSmall, color = PortalCyan)
      Text(
        text = item?.let { "${it.name} (+${it.level})" } ?: "Chưa trang bị",
        style = MaterialTheme.typography.labelLarge,
        color = if (item != null) ParchmentLight else RuneSilver,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (item != null) {
        OutlinedButton(
          onClick = onUnequip,
          enabled = !isBusy,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .testTag(unequipTag),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
          Text("Tháo Trang Bị", style = MaterialTheme.typography.labelSmall, color = AmberWarning)
        }
      }
    }
  }
}

@Composable
private fun HeroSkillsTab(
  hero: HeroRecord,
  profile: MasterProfile,
  isBusy: Boolean,
  onUpgradeSkill: () -> Unit,
) {
  val manaCost = 25L + hero.skillLevel * 10L
  val canUpgrade = profile.manaCrystals >= manaCost && profile.arcaneEssence >= 1L
  val curMultPct = ((1.65f + hero.skillLevel * 0.14f) * 100).toInt()
  val nextMultPct = ((1.65f + (hero.skillLevel + 1) * 0.14f) * 100).toInt()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = AstralSlate,
      border = BorderStroke(1.dp, ArcaneViolet.copy(alpha = 0.75f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
            text = "★ ${hero.classType.skillNameVi}",
            style = MaterialTheme.typography.titleMedium,
            color = SummonerGold,
          )
          Text(
            text = "Cấp ${hero.skillLevel}",
            style = MaterialTheme.typography.titleSmall,
            color = PortalCyan,
          )
        }
        Text(hero.classType.skillDescVi, style = MaterialTheme.typography.bodyMedium, color = ParchmentLight)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          Text("Hồi chiêu: ${hero.classType.skillCooldownSec}s", style = MaterialTheme.typography.labelMedium, color = EmeraldVitality)
          Text("Tầm đánh ưu tiên: ${hero.classType.preferredRange.toInt()}", style = MaterialTheme.typography.labelMedium, color = PortalCyan)
        }
        Text(
          text = "Uy lực kỹ năng: $curMultPct% ATK ➔ $nextMultPct% ATK khi lên Cấp ${hero.skillLevel + 1}",
          style = MaterialTheme.typography.bodySmall,
          color = RuneSilver,
        )
      }
    }

    if (!canUpgrade) {
      Text(
        text = "Cần $manaCost Tinh Thể Mana + 1 Tinh Hoa Cổ Ngữ để nâng cấp (Hiện có: ${profile.manaCrystals} Mana, ${profile.arcaneEssence} Tinh Hoa).",
        style = MaterialTheme.typography.labelSmall,
        color = AmberWarning,
      )
    }

    Button(
      onClick = onUpgradeSkill,
      enabled = !isBusy && canUpgrade,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("profile_upgrade_skill_button"),
      colors = FantasySkin.buttonColors(containerColor = ArcaneVioletDeep, contentColor = Color.White),
    ) {
      Text("Nâng Cấp Kỹ Năng ($manaCost Mana + 1 Tinh Hoa)", fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun HeroActivityRecoveryTab(
  hero: HeroRecord,
  profile: MasterProfile,
  nowEpochSec: Long,
  remInjurySec: Long,
  isBusy: Boolean,
  onAssignActivity: (HeroStatusType) -> Unit,
  onRecover: (Boolean) -> Unit,
) {
  val needsRecovery = hero.status == HeroStatusType.INJURED.name
  val canInstantHeal = profile.rations >= 2L && profile.manaCrystals >= 15L

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = AstralSlate,
      border = BorderStroke(1.dp, if (needsRecovery) AmberWarning else EmeraldVitality.copy(alpha = 0.65f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Lịch Trình & Sức Khỏe Hiện Tại", style = MaterialTheme.typography.titleSmall, color = SummonerGold)
        Text(
          text = hero.consistentActivityLabelVi(nowEpochSec),
          style = MaterialTheme.typography.bodyMedium,
          color = if (needsRecovery) AmberWarning else EmeraldVitality,
        )
        Text(
          text = "Thói quen tự nhiên: ${hero.personalityType.lobbyHabitVi}",
          style = MaterialTheme.typography.bodySmall,
          color = RuneSilver,
        )
      }
    }

    if (needsRecovery) {
      Button(
        onClick = { onRecover(remInjurySec > 0L) },
        enabled = !isBusy && (remInjurySec == 0L || canInstantHeal),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("profile_recover_button"),
        colors = FantasySkin.buttonColors(
          containerColor = if (remInjurySec == 0L) EmeraldVitality else AmberWarning,
          contentColor = ObsidianVoid,
        ),
      ) {
        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (remInjurySec == 0L) {
            "Hoàn Tất Xuất Viện (Miễn Phí)"
          } else {
            "Trị Thương Khẩn Cấp (2 Khẩu Phần • 15 Mana)"
          },
          fontWeight = FontWeight.Bold,
        )
      }
      if (remInjurySec > 0L && !canInstantHeal) {
        Text(
          text = "Chờ hết ${remInjurySec}s để xuất viện miễn phí, hoặc thu thập đủ 2 Khẩu Phần & 15 Mana để trị thương tức thì.",
          style = MaterialTheme.typography.labelSmall,
          color = RuneSilver,
        )
      }
    } else {
      Text("Phân Công Sinh Hoạt Tại Thánh Địa:", style = MaterialTheme.typography.labelLarge, color = PortalCyan)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        RoutineChoiceButton(
          label = "Tuần Tra Sẵn Sàng",
          selected = hero.statusType == HeroStatusType.READY,
          tag = "assign_ready_button",
          enabled = !isBusy,
          onClick = { onAssignActivity(HeroStatusType.READY) },
          modifier = Modifier.weight(1f),
        )
        RoutineChoiceButton(
          label = "Tập Luyện Sân Võ",
          selected = hero.statusType == HeroStatusType.TRAINING,
          tag = "assign_training_button",
          enabled = !isBusy,
          onClick = { onAssignActivity(HeroStatusType.TRAINING) },
          modifier = Modifier.weight(1f),
        )
        RoutineChoiceButton(
          label = "Nghỉ Ngơi (+Sĩ Khí)",
          selected = hero.statusType == HeroStatusType.RESTING,
          tag = "assign_resting_button",
          enabled = !isBusy,
          onClick = { onAssignActivity(HeroStatusType.RESTING) },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun RoutineChoiceButton(
  label: String,
  selected: Boolean,
  tag: String,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (selected) EmeraldVitality.copy(alpha = 0.25f) else ObsidianVoid,
    border = BorderStroke(1.dp, if (selected) EmeraldVitality else BorderRune),
    modifier = modifier
      .heightIn(min = 48.dp)
      .testTag(tag)
      .clickable(enabled = enabled, onClick = onClick),
  ) {
    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) EmeraldVitality else ParchmentLight,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
      )
    }
  }
}

@Composable
fun IslandExpansionModal(
  profile: MasterProfile,
  isBusy: Boolean,
  onUnlockExpansion: (IslandExpansionMilestone) -> Unit,
  onFocusExpansionOnMap: (IslandExpansionMilestone) -> Unit,
  onClose: () -> Unit,
) {
  BackHandler(onBack = onClose)

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid.copy(alpha = 0.86f))
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(12.dp)
      .testTag("island_expansion_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = FantasySkin.Panel,
      tonalElevation = 10.dp,
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 720.dp)
        .heightIn(max = 600.dp)
        .border(1.5.dp, SummonerGold, RoundedCornerShape(18.dp)),
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "KIẾN THIẾT & MỞ RỘNG ĐẢO NỔI THÁNH ĐỊA",
              style = MaterialTheme.typography.titleMedium,
              color = SummonerGold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Mở khóa các phân khu đảo lơ lửng mới bằng tài nguyên thu hoạch từ Tòa Tháp Vĩnh Hằng",
              style = MaterialTheme.typography.bodySmall,
              color = PortalCyan,
              maxLines = 2,
            )
          }
          IconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp).testTag("close_expansion_modal_button"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Đóng kiến thiết đảo", tint = ParchmentLight)
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ResourcePillBadge("Vàng", profile.gold, SummonerGold)
          ResourcePillBadge("Quặng Sắt", profile.ironOre, RuneSilver)
          ResourcePillBadge("Tinh Hoa", profile.arcaneEssence, EmeraldVitality)
          ResourcePillBadge("Master Lv", profile.masterLevel, PortalCyan)
          ResourcePillBadge("Tầng Đã Vượt", profile.highestClearedFloor, SanctuaryUi.violetOnDark)
        }

        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(IslandExpansionMilestone.entries, key = { it.name }) { milestone ->
            val unlocked = SanctuaryProgressionHelper.isExpansionUnlocked(profile.storyFlags, milestone)
            val meetsReqs = SanctuaryProgressionHelper.canMeetRequirements(profile, milestone)
            val canAfford = SanctuaryProgressionHelper.canAffordExpansion(profile, milestone)

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = AstralSlate,
              border = BorderStroke(
                width = 1.5.dp,
                color = if (unlocked) EmeraldVitality else SummonerGold.copy(alpha = 0.65f),
              ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                  ) {
                    Icon(
                      imageVector = if (unlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                      contentDescription = null,
                      tint = if (unlocked) EmeraldVitality else SummonerGold,
                      modifier = Modifier.size(18.dp),
                    )
                    Text(
                      text = milestone.titleVi,
                      style = MaterialTheme.typography.titleSmall,
                      color = if (unlocked) EmeraldVitality else SummonerGold,
                    )
                  }
                  Text(
                    text = if (unlocked) "ĐÃ KẾT NỐI" else "BẢN VẼ CỔ NGỮ",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unlocked) EmeraldVitality else PortalCyan,
                  )
                }

                Text(milestone.subtitleVi, style = MaterialTheme.typography.bodySmall, color = ParchmentLight)
                Text("★ Hiệu quả lâu dài: ${milestone.benefitVi}", style = MaterialTheme.typography.labelMedium, color = EmeraldVitality)

                if (!unlocked) {
                  Text(
                    text = "Yêu cầu: Master Lv.${milestone.reqMasterLevel} • Vượt Tầng ${milestone.reqClearedFloor} | Chi phí: ${milestone.goldCost} Vàng • ${milestone.oreCost} Quặng Sắt • ${milestone.essenceCost} Tinh Hoa",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (meetsReqs && canAfford) PortalCyan else AmberWarning,
                  )
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  OutlinedButton(
                    onClick = { onFocusExpansionOnMap(milestone) },
                    modifier = Modifier.heightIn(min = 46.dp),
                  ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xem Trên Bản Đồ", color = PortalCyan)
                  }

                  if (!unlocked) {
                    Button(
                      onClick = { onUnlockExpansion(milestone) },
                      enabled = !isBusy && meetsReqs && canAfford,
                      modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .testTag("unlock_expansion_${milestone.name.lowercase()}"),
                      colors = FantasySkin.buttonColors(containerColor = SummonerGold, contentColor = ObsidianVoid),
                    ) {
                      Text(
                        text = when {
                          !meetsReqs -> "Cần Lv.${milestone.reqMasterLevel} & Tầng ${milestone.reqClearedFloor}"
                          !canAfford -> "Thiếu Tài Nguyên"
                          else -> "Xây Dựng Mở Rộng ✦"
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
