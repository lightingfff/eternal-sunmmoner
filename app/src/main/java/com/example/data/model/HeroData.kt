package com.example.data.model

/** Persisted assignment and transient locomotion are deliberately separate state machines. */
enum class HeroAssignment { UNASSIGNED, IN_PARTY, RESTING, SACRIFICED }
enum class HeroSanityState { NORMAL, ANXIOUS, PANICKED, SELF_AWARE;
  companion object { fun fromRaw(raw: String) = entries.firstOrNull { it.name == raw } ?: NORMAL }
}
enum class LobbyPersonality { COWARD, ARROGANT, LOYAL, LONER, REBEL;
  companion object {
    fun fromRaw(raw: String) = entries.firstOrNull { it.name == raw } ?: when (raw) {
      "RECKLESS" -> ARROGANT
      "CALCULATING", "CYNICAL" -> LONER
      else -> LOYAL
    }
  }
}

data class HeroHiddenTrait(
  val id: String = "unyielding_soul",
  val name: String = "Linh hồn bất khuất",
  val minimumLevel: Int = 10,
  val minimumMorale: Int = 60,
  val requiresSelfAwareness: Boolean = false,
  val unlocked: Boolean = false,
) {
  fun canUnlock(level: Int, morale: Int, sanity: HeroSanityState) =
    level >= minimumLevel && morale >= minimumMorale && (!requiresSelfAwareness || sanity == HeroSanityState.SELF_AWARE)
  val displayName: String get() = if (unlocked) name else "???"
  val unlockCondition: String get() = "Cấp $minimumLevel • Tinh thần ≥ $minimumMorale" +
    if (requiresSelfAwareness) " • Thức tỉnh" else ""
}

/** Complete lobby projection of the existing owned HeroRecord, never a second roster/save. */
data class HeroData(
  val id: String, val name: String, val rarity: Int, val level: Int,
  val currentHP: Int, val maxHP: Int, val atk: Int, val def: Int, val speed: Int,
  val morale: Int, val sanityState: HeroSanityState, val personality: LobbyPersonality,
  val currentAssignment: HeroAssignment, val hiddenTrait: HeroHiddenTrait,
) {
  init {
    require(id.isNotBlank()); require(rarity in 1..5); require(level >= 1)
    require(maxHP > 0 && currentHP in 0..maxHP); require(morale in 0..100)
    require(atk >= 0 && def >= 0 && speed > 0)
  }
  val isLimping get() = currentHP.toLong() * 100L < maxHP.toLong() * 30L
  val isFrightened get() = morale < 25 || sanityState == HeroSanityState.PANICKED
  val movementSpeed get() = speed * if (isLimping) .5f else 1f

  companion object {
    fun fromRecord(hero: HeroRecord, nowEpochSec: Long = System.currentTimeMillis() / 1000L): HeroData {
      val max = hero.maxHp.coerceIn(1L, 100000L).toInt()
      val assignment = when {
        hero.currentAssignment == HeroAssignment.SACRIFICED.name -> HeroAssignment.SACRIFICED
        hero.isCurrentlyInjured(nowEpochSec) || hero.statusType == HeroStatusType.RESTING -> HeroAssignment.RESTING
        hero.inActiveSquad -> HeroAssignment.IN_PARTY
        hero.currentAssignment == HeroAssignment.RESTING.name -> HeroAssignment.RESTING
        else -> HeroAssignment.UNASSIGNED
      }
      val hp = if (hero.currentHp < 0L) {
        if (hero.isCurrentlyInjured(nowEpochSec)) max / 4 else max
      } else hero.currentHp.coerceIn(0L, max.toLong()).toInt()
      val elapsed = if (assignment == HeroAssignment.RESTING && hero.restStartedAtEpochSec > 0L)
        (nowEpochSec - hero.restStartedAtEpochSec).coerceIn(0L, 86400L) else 0L
      val healed = (hp + elapsed * max / 120L).coerceAtMost(max.toLong()).toInt()
      return HeroData(hero.id, hero.name, hero.rarity.coerceIn(1L, 5L).toInt(), hero.level.coerceIn(1L, 100L).toInt(),
        healed, max, hero.attack.coerceIn(0L, 50000L).toInt(), hero.defense.coerceIn(0L, 50000L).toInt(),
        hero.speed.coerceIn(1L, 500L).toInt(), hero.morale.coerceIn(0L, 100L).toInt(),
        HeroSanityState.fromRaw(hero.sanityState), LobbyPersonality.fromRaw(hero.personality), assignment, hero.hiddenTrait)
    }
  }
}
