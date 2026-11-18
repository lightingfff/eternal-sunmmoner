package com.example.game.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.game.world.WorldEnvironment
import kotlin.math.atan2
import kotlin.math.hypot

internal object SanctuaryDioramaLight {
  private val distance = Brush.verticalGradient(
    0f to Color(0xFF829797).copy(alpha = .18f),
    .38f to Color.Transparent,
    .70f to Color.Transparent,
    1f to Color(0xFF111E29).copy(alpha = .46f),
    startY = 0f,
    endY = 1f,
  )
  private val shade = Brush.radialGradient(
    0f to Color.White,
    .42f to Color.White,
    .76f to Color(0xFF85938F),
    1f to Color(0xFF475461),
    center = Offset(.39f, .41f),
    radius = .78f,
  )
  private val daylight = Brush.radialGradient(
    colors = listOf(Color(0xFFFFCE86).copy(alpha = .10f), Color.Transparent),
    center = Offset(.32f, .27f),
    radius = .62f,
  )

  private val shaft = Brush.radialGradient(
    .0f to Color(0xFFFFDBA1),
    .46f to Color(0x88FFD19E),
    1f to Color.Transparent,
    center = Offset.Zero,
    radius = 1f,
  )

  fun DrawScope.drawSoftShaft(origin: Offset, target: Offset, width: Float, intensity: Float) {
    val delta = target - origin
    val middle = origin + delta * .5f
    val angle = (atan2(delta.y, delta.x) * (180.0 / Math.PI)).toFloat()
    val length = hypot(delta.x, delta.y) * .65f
    withTransform({
      translate(middle.x, middle.y)
      rotate(angle, Offset.Zero)
      scale(length, width, Offset.Zero)
    }) {
      drawCircle(shaft, radius = 1f, center = Offset.Zero, alpha = intensity)
    }
  }

  /** World-only atmospheric perspective. The ungraded play area stays sharp and readable. */
  fun DrawScope.drawDioramaLight() {
    val sunlight = ((WorldEnvironment.lighting.ambientIntensity - .69f) / .31f).coerceIn(0f, 1f)
    withTransform({ scale(size.width, size.height, Offset.Zero) }) {
      drawRect(distance, size = Size(1f, 1f), alpha = .68f)
      drawRect(shade, size = Size(1f, 1f), alpha = .48f, blendMode = BlendMode.Multiply)
      if (sunlight > .01f) drawRect(daylight, size = Size(1f, 1f), alpha = sunlight)
    }
  }
}
