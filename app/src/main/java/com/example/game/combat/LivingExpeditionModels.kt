package com.example.game.combat

import com.example.data.model.TacticalOrder

enum class ExpeditionSiteState { UNKNOWN, REVEALED, RESOLVED }

data class ExpeditionSiteKnowledge(
  val id: String,
  val titleVi: String,
  val x: Float,
  val y: Float,
  val state: ExpeditionSiteState,
  val detailVi: String,
)

data class ExpeditionOption(
  val id: String,
  val labelVi: String,
  val consequenceVi: String,
  val enabled: Boolean = true,
)

data class ExpeditionDecision(val id: String, val promptVi: String, val options: List<ExpeditionOption>)

data class ExpeditionDirective(val order: TacticalOrder, val destination: Pair<Float, Float>, val intentVi: String)

data class ExpeditionConversation(
  val id: String,
  val speakerHeroId: String?,
  val textVi: String,
  val remainingSeconds: Float,
)

class ExpeditionWitness internal constructor() {
  val id: String = TARGET_ID
  var x = 1660f
    internal set
  var y = 1565f
    internal set
  val maxHp = 120
  var hp = 80
    internal set
  var hurtFlash = 0f
    internal set
  var isMoving = false
    internal set
  var facingRight = true
    internal set
  var rescued = false
    internal set
  val alive: Boolean get() = hp > 0

  companion object { const val TARGET_ID = "__expedition_witness__" }
}
