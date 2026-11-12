package com.example.game.combat

import com.example.data.model.HeroClassType

/** Runtime-only presentation metadata attached at execution; never serialized into saves. */
enum class CombatFxStyle {
  GROUND_RING, GUARD, ARCANE, SHADOW, CLEAVE, HOLY, IMPACT, SHIELD_IMPACT, SLAM;
  companion object {
    fun skill(heroClass: HeroClassType): CombatFxStyle = when(heroClass) {
      HeroClassType.KNIGHT -> GUARD
      HeroClassType.ARCHER -> IMPACT
      HeroClassType.MAGE -> ARCANE
      HeroClassType.ASSASSIN -> SHADOW
      HeroClassType.BERSERKER -> CLEAVE
      HeroClassType.PRIEST -> HOLY
    }
  }
}
enum class ProjectileVisual { BOLT, ARROW, ARCANE, HOLY, HOSTILE_ARROW }
