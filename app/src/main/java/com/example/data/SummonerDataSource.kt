package com.example.data

import com.example.data.firebase.ExpeditionSettlementResult
import com.example.data.model.EquipmentRecord
import com.example.data.model.CourierOutcome
import com.example.data.model.HeroRecord
import com.example.data.model.HeroStatusType
import com.example.data.model.IslandExpansionMilestone
import com.example.data.model.MasterProfile
import com.example.data.model.SanctuaryMealRecipe
import com.example.data.model.TrainingFocus
import kotlinx.coroutines.flow.Flow

enum class SessionAccountMode {
  NONE,
  GUEST_LOCAL,
  GOOGLE_CLOUD,
}

enum class PerformanceMode(val targetFps: Int) {
  BALANCED(30),
  HIGH(60),
}

data class GameSettings(
  val defaultZoom: Float = 0.88f,
  val particlesEnabled: Boolean = true,
  val showHeroAiIntent: Boolean = true,
  val compactHud: Boolean = false,
  val performanceMode: PerformanceMode = PerformanceMode.BALANCED,
  val performanceOverlay: Boolean = false,
)

data class GuestSaveSnapshot(
  val schemaVersion: Int = CURRENT_SAVE_SCHEMA_VERSION,
  val guestPlayerId: String,
  val profile: MasterProfile,
  val heroes: List<HeroRecord>,
  val equipment: List<EquipmentRecord>,
  val settings: GameSettings = GameSettings(),
  val lastSavedEpochSec: Long = System.currentTimeMillis() / 1000L,
) {
  companion object {
    const val CURRENT_SAVE_SCHEMA_VERSION = 3
  }
}

/**
 * Unified persistence contract shared by both Cloud Firestore (SummonerRepository)
 * and Offline-First Local Guest Storage (LocalGuestDataSource).
 */
interface SummonerDataSource {
  val isGuestMode: Boolean

  fun observeMasterProfile(userId: String): Flow<MasterProfile?>
  fun observeHeroes(userId: String): Flow<List<HeroRecord>>
  fun observeEquipment(userId: String): Flow<List<EquipmentRecord>>

  suspend fun bootstrapMasterIfNeeded(preferredName: String = "Summoner")

  suspend fun summonNewHero(
    profile: MasterProfile,
    existingHeroes: List<HeroRecord>,
  ): HeroRecord

  suspend fun trainHero(
    profile: MasterProfile,
    hero: HeroRecord,
    upgradeSkill: Boolean,
  ) {
    trainHeroWithFocus(
      profile = profile,
      hero = hero,
      focus = if (upgradeSkill) TrainingFocus.ARCANE_SKILL else TrainingFocus.BALANCED,
    )
  }

  suspend fun trainHeroWithFocus(
    profile: MasterProfile,
    hero: HeroRecord,
    focus: TrainingFocus,
  )

  suspend fun equipItemToHero(
    hero: HeroRecord,
    item: EquipmentRecord,
    allHeroes: List<HeroRecord>,
    allEquipment: List<EquipmentRecord>,
  )

  suspend fun unequipItemFromHero(
    hero: HeroRecord,
    slot: String,
    allEquipment: List<EquipmentRecord>,
  )

  suspend fun assignHeroActivity(
    hero: HeroRecord,
    targetStatus: HeroStatusType,
  )

  suspend fun synthesizeEquipment(profile: MasterProfile): EquipmentRecord

  suspend fun enhanceEquipment(profile: MasterProfile, item: EquipmentRecord)

  suspend fun recoverInjuredHero(
    profile: MasterProfile,
    hero: HeroRecord,
    instantWithResources: Boolean,
  )

  suspend fun hostSanctuaryMeal(profile: MasterProfile, heroes: List<HeroRecord>) {
    hostSanctuaryMealWithRecipe(profile, heroes, SanctuaryMealRecipe.ROAST_BEAST_PLATTER)
  }

  suspend fun hostSanctuaryMealWithRecipe(
    profile: MasterProfile,
    heroes: List<HeroRecord>,
    recipe: SanctuaryMealRecipe,
  )

  suspend fun unlockIslandExpansion(
    profile: MasterProfile,
    milestone: IslandExpansionMilestone,
  )

  suspend fun assignLobbyHero(heroId: String, assignment: com.example.data.model.HeroAssignment)

  suspend fun healHeroAtInfirmary(
    profile: MasterProfile,
    hero: HeroRecord,
    goldCost: Long = LobbyHeroAssignments.INFIRMARY_HEAL_GOLD_COST,
  )

  suspend fun serveHeartyMealToHero(
    profile: MasterProfile,
    hero: HeroRecord,
    goldCost: Long = LobbyHeroAssignments.HEARTY_MEAL_GOLD_COST,
  )

  suspend fun performCruelHeroSynthesis(
    targetHeroId: String,
    sacrificedHeroIds: List<String>,
  ): CruelSynthesisOutcome

  suspend fun applySocialDisdainPenalty(lowStarHeroId: String, penalty: Int = 2, eventId: String = "")

  suspend fun toggleHeroInSquad(hero: HeroRecord, currentSquadSize: Int)

  suspend fun markStoryFlag(profile: MasterProfile, flag: String)

  suspend fun settleExpedition(
    profile: MasterProfile,
    squadHeroes: List<HeroRecord>,
    fallenHeroIds: Set<String>,
    floorNumber: Int,
    victory: Boolean,
    retreatUsed: Boolean = false,
    bonusGold: Long = 0L,
    bonusMana: Long = 0L,
    discoveredNodeIds: Set<String> = emptySet(),
    courierOutcome: CourierOutcome? = null,
    courierMemoryHeroId: String? = null,
    remainingHeroHpRatios: Map<String, Float> = emptyMap(),
  ): ExpeditionSettlementResult
}

object FeastBuffHelper {
  private const val LEGACY_PREFIX = "FEAST_UNTIL_"
  private const val RECIPE_PREFIX = "MEAL_"

  fun buildUpdatedStoryFlagsWithFeast(
    existingFlags: List<String>,
    untilEpochSec: Long,
    recipe: SanctuaryMealRecipe = SanctuaryMealRecipe.ROAST_BEAST_PLATTER,
  ): List<String> {
    val cleaned = existingFlags.filterNot {
      it.startsWith(LEGACY_PREFIX) || it.startsWith(RECIPE_PREFIX)
    }
    return SanctuaryProgressionHelper.compactStoryFlags(
      cleaned + "${RECIPE_PREFIX}${recipe.code}_$untilEpochSec"
    )
  }

  fun activeMealRecipeAndRemaining(
    storyFlags: List<String>,
    nowEpochSec: Long = System.currentTimeMillis() / 1000L,
  ): Pair<SanctuaryMealRecipe, Long>? {
    val recipeFlag = storyFlags.lastOrNull { it.startsWith(RECIPE_PREFIX) }
    if (recipeFlag != null) {
      val body = recipeFlag.removePrefix(RECIPE_PREFIX)
      val splitIdx = body.lastIndexOf('_')
      if (splitIdx > 0) {
        val code = body.substring(0, splitIdx)
        val until = body.substring(splitIdx + 1).toLongOrNull() ?: 0L
        val rem = (until - nowEpochSec).coerceAtLeast(0L)
        if (rem > 0L) return SanctuaryMealRecipe.fromCode(code) to rem
      }
    }
    val legacyFlag = storyFlags.lastOrNull { it.startsWith(LEGACY_PREFIX) } ?: return null
    val until = legacyFlag.removePrefix(LEGACY_PREFIX).toLongOrNull() ?: return null
    val rem = (until - nowEpochSec).coerceAtLeast(0L)
    return if (rem > 0L) SanctuaryMealRecipe.ROAST_BEAST_PLATTER to rem else null
  }

  fun activeRecipe(
    storyFlags: List<String>,
    nowEpochSec: Long = System.currentTimeMillis() / 1000L,
  ): SanctuaryMealRecipe? = activeMealRecipeAndRemaining(storyFlags, nowEpochSec)?.first

  fun activeFeastRemainingSeconds(
    storyFlags: List<String>,
    nowEpochSec: Long = System.currentTimeMillis() / 1000L,
  ): Long = activeMealRecipeAndRemaining(storyFlags, nowEpochSec)?.second ?: 0L

  fun isFeastBuffActive(
    storyFlags: List<String>,
    nowEpochSec: Long = System.currentTimeMillis() / 1000L,
  ): Boolean = activeFeastRemainingSeconds(storyFlags, nowEpochSec) > 0L
}

object SanctuaryProgressionHelper {
  fun isExpansionUnlocked(
    storyFlags: List<String>,
    milestone: IslandExpansionMilestone,
  ): Boolean = milestone.flagKey in storyFlags

  fun unlockedExpansions(storyFlags: List<String>): Set<IslandExpansionMilestone> =
    IslandExpansionMilestone.entries.filter { it.flagKey in storyFlags }.toSet()

  fun canMeetRequirements(
    profile: MasterProfile,
    milestone: IslandExpansionMilestone,
  ): Boolean =
    profile.masterLevel >= milestone.reqMasterLevel &&
      profile.highestClearedFloor >= milestone.reqClearedFloor

  fun canAffordExpansion(
    profile: MasterProfile,
    milestone: IslandExpansionMilestone,
  ): Boolean =
    profile.gold >= milestone.goldCost &&
      profile.ironOre >= milestone.oreCost &&
      profile.arcaneEssence >= milestone.essenceCost

  /**
   * Compacts storyFlags to <= 20 entries (matching Firestore security rule limit) while NEVER
   * evicting permanent progression and courier flags when transient flags accumulate.
   */
  fun compactStoryFlags(rawFlags: List<String>): List<String> {
    val distinct = rawFlags.map { it.take(50) }.filter { it.isNotBlank() }.distinct()
    if (distinct.size <= 20) return distinct
    val permanent = distinct.filter {
      it == "PROLOGUE_SEEN" || it.startsWith("EXP_") || LostCourierProgression.isPermanentFlag(it)
    }
    val transient = distinct.filterNot {
      it == "PROLOGUE_SEEN" || it.startsWith("EXP_") || LostCourierProgression.isPermanentFlag(it)
    }
    val remainingSlots = (20 - permanent.size).coerceAtLeast(0)
    return (permanent.take(20) + transient.takeLast(remainingSlots))
  }
}
