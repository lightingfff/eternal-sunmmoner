package com.example.game.combat

import androidx.compose.ui.graphics.Color
import com.example.data.model.HeroClassType
import com.example.data.model.PersonalityType
import com.example.game.map.EnemyArchetype

class HeroCombatUnit(
  val id: String,
  val name: String,
  val title: String,
  val heroClass: HeroClassType,
  val personality: PersonalityType,
  val maxHp: Int,
  var hp: Int,
  val attack: Int,
  val defense: Int,
  val speed: Float,
  val skillLevel: Int,
  var x: Float,
  var y: Float,
  var anchorX: Float,
  var anchorY: Float,
  val formationIndex: Int,
) {
  var facingRight: Boolean = true
  var isMoving: Boolean = false
  var attackCooldown: Float = 0f
  var skillCooldown: Float = 1.5f
  val maxSkillCooldown: Float = heroClass.skillCooldownSec
  var attackAnim: Float = 0f
  var hurtFlash: Float = 0f
  var shieldHp: Int = 0
  var knockedOut: Boolean = false
  var currentTargetId: String? = null
  var aiIntentLabelVi: String = "Theo đội hình"
  var manualCommandCooldown: Float = 0f
}

class EnemyCombatUnit(
  val id: String,
  val archetype: EnemyArchetype,
  val maxHp: Int,
  var hp: Int,
  val attack: Int,
  val defense: Int,
  val speed: Float,
  var x: Float,
  var y: Float,
) {
  var facingRight: Boolean = false
  var isMoving: Boolean = false
  var attackCooldown: Float = 0.6f
  var attackAnim: Float = 0f
  var hurtFlash: Float = 0f
  var dead: Boolean = false
  var aggroed: Boolean = false
  var deathAnimProgress: Float = 0f
  var warcryCooldown: Float = 3.2f
  var atkBuffTimer: Float = 0f
}

enum class DiscoveryNodeKind {
  TREASURE_CHEST,
  HEALING_SHRINE,
}

data class BattleDiscoveryNode(
  val id: String,
  val obstacleId: String,
  val titleVi: String,
  val x: Float,
  val y: Float,
  val kind: DiscoveryNodeKind,
  val goldBonus: Long,
  val manaBonus: Long,
  val healPct: Float,
  var discovered: Boolean = false,
)

data class CombatProjectile(
  val id: String,
  val fromHero: Boolean,
  var x: Float,
  var y: Float,
  val vx: Float,
  val vy: Float,
  val damage: Int,
  val color: Color,
  val radius: Float = 6f,
  var remainingLife: Float = 1.6f,
  val isAoEOnHit: Boolean = false,
)

data class CombatAoEEffect(
  var x: Float,
  var y: Float,
  val maxRadius: Float,
  val color: Color,
  var elapsed: Float = 0f,
  val duration: Float = 0.55f,
)

data class FloatingCombatText(
  var x: Float,
  var y: Float,
  val text: String,
  val color: Color,
  val isLarge: Boolean = false,
  var elapsed: Float = 0f,
  val duration: Float = 0.95f,
)
