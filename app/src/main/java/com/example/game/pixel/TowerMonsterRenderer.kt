package com.example.game.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.game.combat.EnemyCombatUnit
import com.example.game.map.ObliqueCamera
import com.example.game.pixel.PixelSpriteRenderer.drawEnemySprite
import kotlin.math.roundToInt
import kotlin.math.sin

/** Tower-only authored bodies. Shares the existing depth queue, camera, light and occlusion. */
internal object TowerMonsterRenderer {
  fun DrawScope.drawTowerMonster(enemy: EnemyCombatUnit, camX: Float, camY: Float, zoom: Float,
    seconds: Float, marked: Boolean, authored: Boolean, charging: Boolean, recovering: Boolean) {
    val x = ObliqueCamera.worldToScreenX(enemy.x, camX, zoom, size.width/2f)
    val y = ObliqueCamera.worldToScreenY(enemy.y, camY, zoom, size.height/2f)
    val extent = 170f*zoom.coerceAtLeast(1f)
    if (x < -extent || x > size.width+extent || y < -extent || y > size.height+extent) return
    val frame = if (authored) TowerMonsterAtlas.frame(enemy, seconds, charging, recovering) else null
    if (frame == null) {
      drawEnemySprite(enemy.x,enemy.y,camX,camY,zoom,enemy.archetype,enemy.facingRight,enemy.isMoving,
        enemy.attackAnim,enemy.hurtFlash,enemy.hp.toFloat()/enemy.maxHp.coerceAtLeast(1),marked,seconds,
        if (enemy.dead) enemy.deathAnimProgress else 0f)
      return
    }
    val scale = frame.worldScale*zoom
    if (!enemy.dead) with(SanctuaryEnvironmentShadows) {
      drawCharacterShadow(frame.image,x,y,zoom,scale,frame.pivotY,mirrored = !enemy.facingRight)
    }
    if (marked && !enemy.dead) {
      val radius = (if (enemy.archetype.isBoss) 42f else 29f)*zoom*(.94f+.06f*sin(seconds*8f))
      drawOval(Color(0xFFEF4444), Offset(x-radius,y-radius*ObliqueCamera.GROUND_ELLIPSE_RATIO),
        Size(radius*2f,radius*2f*ObliqueCamera.GROUND_ELLIPSE_RATIO),style=Stroke(2f*zoom))
    }
    withTransform({ if (!enemy.facingRight) scale(-1f, 1f, Offset(x, y)) }) {
      drawImage(frame.image,
      dstOffset=IntOffset((x-frame.pivotX*scale).roundToInt(),(y-frame.pivotY*scale).roundToInt()),
      dstSize=IntSize((frame.image.width*scale).roundToInt().coerceAtLeast(1),
        (frame.image.height*scale).roundToInt().coerceAtLeast(1)),
      alpha=if (enemy.dead) (1f-enemy.deathAnimProgress).coerceIn(0f,1f) else 1f,
        filterQuality=FilterQuality.None)
    }
    if (!enemy.dead) {
      val width=(if(enemy.archetype.isBoss)70f else 42f)*zoom
      val barHeight = if (frame.healthBarHeight > 0f) frame.healthBarHeight else if (enemy.archetype.isBoss) 128f else 86f
      val top=y-barHeight*zoom
      val height=(if(enemy.archetype.isBoss)7f else 5f)*zoom
      drawRect(Color(0xEE080C18),Offset(x-width/2f-zoom,top-zoom),Size(width+2*zoom,height+2*zoom))
      drawRect(Color(0xFFEA5C60),Offset(x-width/2f,top),
        Size(width*(enemy.hp.toFloat()/enemy.maxHp.coerceAtLeast(1)).coerceIn(0f,1f),height))
    }
  }
}
