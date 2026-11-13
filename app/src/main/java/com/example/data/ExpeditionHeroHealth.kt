package com.example.data

import com.example.data.model.HeroRecord
import kotlin.math.roundToLong

/** Converts combat HP (which includes gear/buffs) back to persisted base HP without a free heal. */
object ExpeditionHeroHealth {
  fun returnFromBattle(
    hero: HeroRecord,
    nextMaxHp: Long,
    knockedOut: Boolean,
    remainingHpRatio: Float?,
  ): HeroRecord {
    require(nextMaxHp > 0L)
    require(remainingHpRatio == null || (remainingHpRatio.isFinite() && remainingHpRatio in 0f..1f)) {
      "Tỷ lệ HP khi trở về không hợp lệ."
    }
    val migrated = LobbyHeroAssignments.checkAndUnlockHiddenTrait(hero)
    val hp = when {
      remainingHpRatio != null -> (hero.maxHp * remainingHpRatio.toDouble()).roundToLong()
        .coerceIn(if (remainingHpRatio > 0f) 1L else 0L, nextMaxHp)
      knockedOut -> (hero.maxHp / 4L).coerceIn(0L, nextMaxHp)
      hero.currentHp < 0L -> -1L
      else -> hero.currentHp.coerceIn(0L, nextMaxHp)
    }
    val survived = !knockedOut && (remainingHpRatio == null || remainingHpRatio > 0f)
    return migrated.copy(
      currentHp = hp,
      survivedExpeditions = (migrated.survivedExpeditions + if (survived) 1 else 0).coerceAtMost(1_000_000),
      returnedUnder15PctHp = migrated.returnedUnder15PctHp ||
        (survived && remainingHpRatio != null && remainingHpRatio > 0f && remainingHpRatio < .15f),
    )
  }
}
