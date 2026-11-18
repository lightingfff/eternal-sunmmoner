package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.game.lobby.LobbyHeroActor
import com.example.game.lobby.LobbyLocomotion
import com.example.game.map.GroundView
import com.example.game.map.ObliqueCamera
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.ui.theme.ESDesignTokens
import kotlin.math.hypot
import kotlin.math.sin

internal fun pickupHeroAtScreen(actors: List<LobbyHeroActor>, point: Offset, camX: Float, camY: Float, zoom: Float, size: Size, minimumTargetPx: Float = 48f, view: GroundView = GroundView.STANDARD): LobbyHeroActor? =
  actors.filter { !it.isSanctuaryKeeperNpc }.filter { actor ->
    val x = ObliqueCamera.worldToScreenX(actor.x, camX, zoom, size.width / 2f)
    val y = view.projectY(actor.y, camY, zoom, size.height / 2f)
    point.x in x - maxOf(minimumTargetPx / 2f, 30f * zoom)..x + maxOf(minimumTargetPx / 2f, 30f * zoom) &&
      point.y in y - maxOf(minimumTargetPx * .75f, 90f * zoom)..y + maxOf(minimumTargetPx * .25f, 12f * zoom)
  }.minByOrNull { actor ->
    hypot(point.x - ObliqueCamera.worldToScreenX(actor.x, camX, zoom, size.width / 2f),
      point.y - (view.projectY(actor.y, camY, zoom, size.height / 2f) - 36f * zoom))
  }

fun DrawScope.drawLobbyHero(actor: LobbyHeroActor, camX: Float, camY: Float, zoom: Float, time: Float, selected: Boolean, held: Boolean = false) {
  val frightened = actor.locomotion == LobbyLocomotion.COWERING
  val limp = actor.lobbyData?.isLimping == true
  val sway = when { held -> sin(time * 20f) * 6f; frightened -> sin(time * 32f) * 2f; else -> 0f }
  val idleBreath = if (
    !held && !frightened && !actor.isMoving && !actor.isInjured && !actor.isSeatedDining
  ) {
    sin(time * 2.1f + actor.x * 0.013f + actor.y * 0.011f) * 1.8f
  } else {
    0f
  }
  val lift = when {
    held -> -32f
    limp && actor.isMoving -> sin(time * 7f) * 2f
    else -> idleBreath
  }
  withTransform({ translate(sway * zoom, lift * zoom) }) {
    drawCharacterSprite(worldX = actor.x, worldY = actor.y, camX = camX, camY = camY, zoom = zoom,
      isMaster = false, heroClass = actor.heroClass, facingRight = if (held) sin(time * 12f) > 0f else actor.facingRight,
      isMoving = held || actor.isMoving, attackAnimProgress = if (held) 0f else actor.attackPracticeAnim,
      hurtFlash = 0f, hpFraction = actor.lobbyData?.let { it.currentHP.toFloat() / it.maxHP } ?: if (actor.isInjured) .35f else 1f,
      shieldOrAuraColor = when {
        held || selected -> ESDesignTokens.gold
        actor.isSanctuaryKeeperNpc -> ESDesignTokens.cyan.copy(alpha = 0.47f)
        else -> null
      },
      isInjuredResting = actor.isInjured || actor.isSeatedDining, timeSec = time,
      facingNorth = !held && actor.facingNorth, heroName = actor.name,
      emotion = if (held || frightened || limp) "fear" else if (selected) "alert" else "calm")
  }
}
