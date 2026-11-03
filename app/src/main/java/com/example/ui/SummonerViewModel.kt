package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SummonerDataSource
import com.example.data.firebase.ExpeditionSettlementResult
import com.example.data.model.EquipmentRecord
import com.example.data.model.CourierOutcome
import com.example.data.model.FacilityType
import com.example.data.model.HeroRecord
import com.example.data.model.HeroStatusType
import com.example.data.model.IslandExpansionMilestone
import com.example.data.model.MasterProfile
import com.example.data.model.SanctuaryMealRecipe
import com.example.data.model.TrainingFocus
import com.example.game.map.TowerFloorSpec
import com.example.game.story.ManhwaStoryData
import com.example.game.tower.TowerFloorCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GameScreenMode {
  SANCTUARY_LOBBY,
  FLOOR_SELECTION,
  SQUAD_PREPARATION,
  TOWER_COMBAT,
}

data class SummonerUiState(
  val isBootstrapping: Boolean = true,
  val profile: MasterProfile? = null,
  val heroes: List<HeroRecord> = emptyList(),
  val equipment: List<EquipmentRecord> = emptyList(),
  val screenMode: GameScreenMode = GameScreenMode.SANCTUARY_LOBBY,
  val selectedFloor: TowerFloorSpec = TowerFloorCatalog.FLOORS.first(),
  val activeFacilityModal: FacilityType? = null,
  val newlySummonedHero: HeroRecord? = null,
  val newlyForgedItem: EquipmentRecord? = null,
  val lastSettlement: ExpeditionSettlementResult? = null,
  val bannerFeedback: String? = null,
  val isBusyAction: Boolean = false,
  val isGuestAccount: Boolean = false,
)

class SummonerViewModel(
  private val repository: SummonerDataSource,
  private val currentUserId: String,
  preferredDisplayName: String,
) : ViewModel() {

  private val _screenMode = MutableStateFlow(GameScreenMode.SANCTUARY_LOBBY)
  private val _selectedFloor = MutableStateFlow(TowerFloorCatalog.FLOORS.first())
  private val _activeFacilityModal = MutableStateFlow<FacilityType?>(null)
  private val _newlySummonedHero = MutableStateFlow<HeroRecord?>(null)
  private val _newlyForgedItem = MutableStateFlow<EquipmentRecord?>(null)
  private val _lastSettlement = MutableStateFlow<ExpeditionSettlementResult?>(null)
  private val _bannerFeedback = MutableStateFlow<String?>(null)
  private val _isBusyAction = MutableStateFlow(false)
  private val _isBootstrapping = MutableStateFlow(true)

  private val profileFlow = repository.observeMasterProfile(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing MasterProfile", e)
      emit(null)
    }

  private val heroesFlow = repository.observeHeroes(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing Heroes", e)
      emit(emptyList())
    }

  private val equipmentFlow = repository.observeEquipment(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing Equipment", e)
      emit(emptyList())
    }

  val uiState: StateFlow<SummonerUiState> = combine(
    combine(profileFlow, heroesFlow, equipmentFlow) { p, h, eq -> Triple(p, h, eq) },
    combine(_screenMode, _selectedFloor, _activeFacilityModal) { m, f, fac -> Triple(m, f, fac) },
    combine(_newlySummonedHero, _newlyForgedItem, _lastSettlement) { s, fg, st -> Triple(s, fg, st) },
    combine(_bannerFeedback, _isBusyAction, _isBootstrapping) { msg, busy, boot -> Triple(msg, busy, boot) },
  ) { dataTriple, navTriple, rewardTriple, statusTriple ->
    SummonerUiState(
      isBootstrapping = statusTriple.third && dataTriple.first == null,
      profile = dataTriple.first,
      heroes = dataTriple.second.sortedWith(compareByDescending<HeroRecord> { it.inActiveSquad }.thenByDescending { it.rarity }.thenByDescending { it.level }),
      equipment = dataTriple.third.sortedWith(compareByDescending<EquipmentRecord> { it.rarity }.thenByDescending { it.level }),
      screenMode = navTriple.first,
      selectedFloor = navTriple.second,
      activeFacilityModal = navTriple.third,
      newlySummonedHero = rewardTriple.first,
      newlyForgedItem = rewardTriple.second,
      lastSettlement = rewardTriple.third,
      bannerFeedback = statusTriple.first,
      isBusyAction = statusTriple.second,
      isGuestAccount = repository.isGuestMode,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = SummonerUiState(isGuestAccount = repository.isGuestMode),
  )

  init {
    viewModelScope.launch {
      try {
        repository.bootstrapMasterIfNeeded(preferredDisplayName)
      } catch (e: Exception) {
        Log.e(TAG, "Bootstrap failed", e)
        _bannerFeedback.value = e.localizedMessage ?: "Không thể khởi tạo Thánh Địa."
      } finally {
        _isBootstrapping.value = false
      }
    }
  }

  fun dismissBanner() {
    _bannerFeedback.value = null
  }

  fun clearSpotlightRewards() {
    _newlySummonedHero.value = null
    _newlyForgedItem.value = null
  }

  fun openFacility(facility: FacilityType) {
    if (facility == FacilityType.TOWER_GATE) {
      _activeFacilityModal.value = null
      _screenMode.value = GameScreenMode.FLOOR_SELECTION
    } else {
      _activeFacilityModal.value = facility
    }
  }

  fun closeFacilityModal() {
    _activeFacilityModal.value = null
    clearSpotlightRewards()
  }

  fun selectFloorForPrep(floor: TowerFloorSpec) {
    val highestCleared = uiState.value.profile?.highestClearedFloor?.toInt() ?: 0
    if (floor.floorNumber > highestCleared + 1) {
      _bannerFeedback.value = "Hãy vượt qua Tầng ${floor.floorNumber - 1} trước khi mở khóa tầng này!"
      return
    }
    _selectedFloor.value = floor
    _screenMode.value = GameScreenMode.SQUAD_PREPARATION
  }

  fun backToFloorSelection() {
    _screenMode.value = GameScreenMode.FLOOR_SELECTION
  }

  fun returnToSanctuaryLobby() {
    _lastSettlement.value = null
    _activeFacilityModal.value = null
    _screenMode.value = GameScreenMode.SANCTUARY_LOBBY
  }

  fun retryTowerExpedition() {
    // Only retry a settled attempt; normal preparation/launch still enforces healthy heroes.
    if (_isBusyAction.value || _lastSettlement.value == null) return
    _lastSettlement.value = null
    _screenMode.value = GameScreenMode.SQUAD_PREPARATION
  }

  fun launchTowerExpedition() {
    val state = uiState.value
    val activeHealthySquad = state.heroes.filter { it.inActiveSquad && !it.isCurrentlyInjured() }
    if (activeHealthySquad.isEmpty()) {
      _bannerFeedback.value = "Đội hình trống hoặc đang bị thương! Hãy chọn ít nhất 1 Anh hùng khỏe mạnh."
      return
    }
    _lastSettlement.value = null
    _screenMode.value = GameScreenMode.TOWER_COMBAT
  }

  fun completePrologueCutscene() {
    val profile = uiState.value.profile ?: return
    viewModelScope.launch {
      try {
        repository.markStoryFlag(profile, ManhwaStoryData.FLAG_PROLOGUE_SEEN)
      } catch (e: Exception) {
        Log.w(TAG, "Failed marking prologue flag", e)
      }
    }
  }

  fun toggleSquadHero(hero: HeroRecord) {
    val currentSquadCount = uiState.value.heroes.count { it.inActiveSquad && !it.isCurrentlyInjured() }
    viewModelScope.launch {
      try {
        repository.toggleHeroInSquad(hero, currentSquadCount)
      } catch (e: Exception) {
        _bannerFeedback.value = e.message ?: "Không thể thay đổi đội hình."
      }
    }
  }

  fun performHeroSummon() {
    val state = uiState.value
    val profile = state.profile ?: return
    if (_isBusyAction.value) return
    _isBusyAction.value = true
    viewModelScope.launch {
      try {
        val hero = repository.summonNewHero(profile, state.heroes)
        _newlySummonedHero.value = hero
        _bannerFeedback.value = "Triệu hồi thành công [${hero.name} — ${hero.title}]!"
      } catch (e: Exception) {
        _bannerFeedback.value = e.message ?: "Không thể thực hiện nghi thức triệu hồi."
      } finally {
        _isBusyAction.value = false
      }
    }
  }

  fun trainHero(hero: HeroRecord, upgradeSkill: Boolean) {
    trainHeroWithFocus(
      hero = hero,
      focus = if (upgradeSkill) TrainingFocus.ARCANE_SKILL else TrainingFocus.BALANCED,
    )
  }

  fun trainHeroWithFocus(hero: HeroRecord, focus: TrainingFocus) {
    val profile = uiState.value.profile ?: return
    if (_isBusyAction.value) return
    _isBusyAction.value = true
    viewModelScope.launch {
      try {
        repository.trainHeroWithFocus(profile, hero, focus)
        _bannerFeedback.value = if (focus.isSkillUpgrade) {
          "Đã nâng cấp kỹ năng [${hero.classType.skillNameVi}] lên Tầng ${hero.skillLevel + 1} cho ${hero.name}!"
        } else {
          "${hero.name} đã thăng lên Lv.${hero.level + 1} (${focus.shortVi})!"
        }
      } catch (e: Exception) {
        _bannerFeedback.value = e.message ?: "Huấn luyện thất bại."
      } finally {
        _isBusyAction.value = false
      }
    }
  }

  fun equipItem(hero: HeroRecord, item: EquipmentRecord) {
    val state = uiState.value
    if (_isBusyAction.value) return
    _isBusyAction.value = true
    viewModelScope.launch {
      try {
        repository.equipItemToHero(hero, item, state.heroes, state.equipment)
        _bannerFeedback.value = "Đã trang bị [${item.name}] cho ${hero.name}!"
      } catch (e: Exception) {
        _bannerFeedback.value = e.message ?: "Không thể trang bị vật phẩm."
      } finally {
        _isBusyAction.value = false
      }
    }
  }

  fun unequipItem(hero: HeroRecord, slot: String) {
    val state = uiState.value
    if (_isBusyAction.value) return
    _isBusyAction.value = true
    viewModelScope.launch {
      try {
        repository.unequipItemFromHero(hero, slot, state.equipment)
        val slotLabel = if (slot == "ARMOR") "Giáp Trụ" else "Vũ Khí"
        _bannerFeedback.value = "Đã tháo $slotLabel của ${hero.name} về Kho Vũ Khí!"
      } catch (e: Exception) {
        _bannerFeedback.value = e.message ?: "Không thể tháo trang bị."
      } finally {
        _isBusyAction.value = false
      }
    }
  }

  fun assignHeroActivity(hero: HeroRecord, targetStatus: HeroStatusType) {
    if (_isBusyAction.value) return
    _isBusyAction.value = true
    viewModelScope.launch {
      try {
        repository.assignHeroActivity(hero, targetStatus)
        val label = when (targetStatus) {
          HeroStatusType.TRAINING -> "đã nhận lệnh đến Sân Tập rèn luyện"
          HeroStatusType.RESTING -> "đã trở về Khu Nghỉ Ngơi thư giãn (+8 Sĩ Khí)"
          else -> "đã sẵn sàng tuần tra Thánh Địa"
        }
        _bannerFeedback.value = "${hero.name} $label!"
      } catch (e: Exception) {
        _bannerFeedback.value = e.message ?: "Không thể thay đổi lịch trình."
      } finally {
        _isBusyAction.value = false
      }
    }
  }

  fun synthesizeNewGear() {
    val profile = uiState.value.profile ?: return
    if (_isBusyAction.value) return
    _isBusyAction.value = true
    viewModelScope.launch {
      try {
        val crafted = repository.synthesizeEquipment(profile)
        _newlyForgedItem.value = crafted
        _bannerFeedback.value = "Chế tác thành công [${crafted.name}]!"
      } catch (e: Exception) {
        _bannerFeedback.value = e.message ?: "Chế tác thất bại."
      } finally {
        _isBusyAction.value = false
      }
    }
  }

  fun enhanceGear(item: EquipmentRecord) {
    val profile = uiState.value.profile ?: return
    if (_isBusyAction.value) return
    _isBusyAction.value = true
    viewModelScope.launch {
      try {
        repository.enhanceEquipment(profile, item)
        _bannerFeedback.value = "Đã cường hóa [${item.name}] lên +${item.level + 1}!"
      } catch (e: Exception) {
        _bannerFeedback.value = e.message ?: "Cường hóa thất bại."
      } finally {
        _isBusyAction.value = false
      }
    }
  }

  fun recoverHero(hero: HeroRecord, instantWithResources: Boolean) {
    val profile = uiState.value.profile ?: return
    if (_isBusyAction.value) return
    _isBusyAction.value = true
    viewModelScope.launch {
      try {
        repository.recoverInjuredHero(profile, hero, instantWithResources)
        _bannerFeedback.value = "${hero.name} đã hồi phục hoàn toàn và sẵn sàng chiến đấu!"
      } catch (e: Exception) {
        _bannerFeedback.value = e.message ?: "Chưa thể hồi phục."
      } finally {
        _isBusyAction.value = false
      }
    }
  }

  fun hostRestaurantMeal() {
    hostRestaurantMealWithRecipe(SanctuaryMealRecipe.ROAST_BEAST_PLATTER)
  }

  fun hostRestaurantMealWithRecipe(recipe: SanctuaryMealRecipe) {
    val state = uiState.value
    val profile = state.profile ?: return
    if (_isBusyAction.value) return
    _isBusyAction.value = true
    viewModelScope.launch {
      try {
        repository.hostSanctuaryMealWithRecipe(profile, state.heroes, recipe)
        _bannerFeedback.value = "Đã phục vụ [${recipe.titleVi}]: ${recipe.subtitleVi}!"
      } catch (e: Exception) {
        _bannerFeedback.value = e.message ?: "Không đủ nguyên liệu mở tiệc."
      } finally {
        _isBusyAction.value = false
      }
    }
  }

  fun unlockIslandExpansion(milestone: IslandExpansionMilestone) {
    val profile = uiState.value.profile ?: return
    if (_isBusyAction.value) return
    _isBusyAction.value = true
    viewModelScope.launch {
      try {
        repository.unlockIslandExpansion(profile, milestone)
        _bannerFeedback.value = "Mở rộng Thánh Địa thành công: [${milestone.titleVi}] đã kết nối vĩnh viễn!"
      } catch (e: Exception) {
        _bannerFeedback.value = e.message ?: "Chưa đủ điều kiện mở rộng đảo."
      } finally {
        _isBusyAction.value = false
      }
    }
  }

  fun finishTowerExpedition(
    victory: Boolean,
    fallenHeroIds: Set<String>,
    retreatUsed: Boolean = false,
    bonusGold: Long = 0L,
    bonusMana: Long = 0L,
    discoveredNodeIds: Set<String> = emptySet(),
    courierOutcome: CourierOutcome? = null,
    courierMemoryHeroId: String? = null,
  ) {
    val state = uiState.value
    val profile = state.profile ?: return
    if (_lastSettlement.value != null || _isBusyAction.value) return

    val squad = state.heroes.filter { it.inActiveSquad && !it.isCurrentlyInjured() }
    _isBusyAction.value = true
    viewModelScope.launch {
      try {
        val result = repository.settleExpedition(
          profile = profile,
          squadHeroes = squad,
          fallenHeroIds = fallenHeroIds,
          floorNumber = state.selectedFloor.floorNumber,
          victory = victory,
          retreatUsed = retreatUsed,
          bonusGold = bonusGold,
          bonusMana = bonusMana,
          discoveredNodeIds = discoveredNodeIds,
          courierOutcome = courierOutcome,
          courierMemoryHeroId = courierMemoryHeroId,
        )
        _lastSettlement.value = result
      } catch (e: Exception) {
        Log.e(TAG, "Expedition settlement error", e)
        _bannerFeedback.value = e.localizedMessage ?: "Lỗi lưu kết quả nhiệm vụ."
      } finally {
        _isBusyAction.value = false
      }
    }
  }

  private companion object {
    const val TAG = "SummonerVM"
  }
}
