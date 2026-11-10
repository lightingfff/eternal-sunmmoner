package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.game.lobby.LobbyHeroActor
import com.example.game.lobby.LobbyLocomotion
import com.example.game.map.ObliqueCamera
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import kotlin.math.hypot
import kotlin.math.sin

fun pickupHeroAtScreen(actors: List<LobbyHeroActor>, point: Offset, camX: Float, camY: Float, zoom: Float, size: Size, minimumTargetPx: Float = 48f): LobbyHeroActor? =
  actors.filter { !it.isSanctuaryKeeperNpc }.filter { actor ->
    val x = ObliqueCamera.worldToScreenX(actor.x, camX, zoom, size.width / 2f)
    val y = ObliqueCamera.worldToScreenY(actor.y, camY, zoom, size.height / 2f)
    point.x in x - maxOf(minimumTargetPx / 2f, 30f * zoom)..x + maxOf(minimumTargetPx / 2f, 30f * zoom) &&
      point.y in y - maxOf(minimumTargetPx * .75f, 90f * zoom)..y + maxOf(minimumTargetPx * .25f, 12f * zoom)
  }.minByOrNull { actor ->
    hypot(point.x - ObliqueCamera.worldToScreenX(actor.x, camX, zoom, size.width / 2f),
      point.y - (ObliqueCamera.worldToScreenY(actor.y, camY, zoom, size.height / 2f) - 36f * zoom))
  }

fun DrawScope.drawLobbyHero(actor: LobbyHeroActor, camX: Float, camY: Float, zoom: Float, time: Float, selected: Boolean, held: Boolean = false) {
  val frightened = actor.locomotion == LobbyLocomotion.COWERING
  val limp = actor.lobbyData?.isLimping == true
  val sway = when { held -> sin(time * 20f) * 6f; frightened -> sin(time * 32f) * 2f; else -> 0f }
  val lift = if (held) -32f else if (limp && actor.isMoving) sin(time * 7f) * 2f else 0f
  withTransform({ translate(sway * zoom, lift * zoom) }) {
    drawCharacterSprite(worldX = actor.x, worldY = actor.y, camX = camX, camY = camY, zoom = zoom,
      isMaster = false, heroClass = actor.heroClass, facingRight = if (held) sin(time * 12f) > 0f else actor.facingRight,
      isMoving = held || actor.isMoving, attackAnimProgress = if (held) 0f else actor.attackPracticeAnim,
      hurtFlash = 0f, hpFraction = actor.lobbyData?.let { it.currentHP.toFloat() / it.maxHP } ?: if (actor.isInjured) .35f else 1f,
      shieldOrAuraColor = when { held || selected -> Color(0xFFE8C36C); actor.isSanctuaryKeeperNpc -> Color(0x7738BDF8); else -> null },
      isInjuredResting = actor.isInjured || actor.isSeatedDining, timeSec = time,
      facingNorth = !held && actor.facingNorth, heroName = actor.name,
      emotion = if (held || frightened || limp) "fear" else if (selected) "alert" else "calm")
  }
}
