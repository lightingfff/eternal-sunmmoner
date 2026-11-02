package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.model.HeroClassType
import com.example.game.pixel.PixelCharacterAtlas
import com.example.ui.theme.ArcaneViolet
import com.example.ui.theme.BorderRune
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.SanctuaryStone
import com.example.ui.theme.SummonerGold
import kotlin.math.hypot

@Composable
fun VirtualTouchJoystick(
  onVectorChanged: (Float, Float) -> Unit,
  modifier: Modifier = Modifier,
  diameter: Dp = 108.dp,
) {
  var knobOffset by remember { mutableStateOf(Offset.Zero) }

  Box(
    modifier = modifier
      .size(diameter)
      .testTag("virtual_joystick")
      .pointerInput(Unit) {
        val maxRadius = size.width / 2f * 0.68f
        detectDragGestures(
          onDragStart = { start ->
            val center = Offset(size.width / 2f, size.height / 2f)
            val raw = start - center
            val d = hypot(raw.x, raw.y)
            val clamped = if (d > maxRadius && d > 0f) raw * (maxRadius / d) else raw
            knobOffset = clamped
            onVectorChanged(
              (clamped.x / maxRadius).coerceIn(-1f, 1f),
              (clamped.y / maxRadius).coerceIn(-1f, 1f),
            )
          },
          onDragEnd = {
            knobOffset = Offset.Zero
            onVectorChanged(0f, 0f)
          },
          onDragCancel = {
            knobOffset = Offset.Zero
            onVectorChanged(0f, 0f)
          },
          onDrag = { change, dragAmount ->
            change.consume()
            val maxR = size.width / 2f * 0.68f
            val next = knobOffset + dragAmount
            val d = hypot(next.x, next.y)
            val clamped = if (d > maxR && d > 0f) next * (maxR / d) else next
            knobOffset = clamped
            onVectorChanged(
              (clamped.x / maxR).coerceIn(-1f, 1f),
              (clamped.y / maxR).coerceIn(-1f, 1f),
            )
          },
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(diameter)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val outerR = size.minDimension / 2f - 4f
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0x991C1635), Color(0xCC0A0812)),
          center = center,
          radius = outerR,
        ),
        radius = outerR,
        center = center,
      )
      // Double gilded RPG ring
      drawCircle(
        color = SummonerGold.copy(alpha = 0.55f),
        radius = outerR,
        center = center,
        style = Stroke(width = 2.5f),
      )
      drawCircle(
        color = ArcaneViolet.copy(alpha = 0.35f),
        radius = outerR * 0.62f,
        center = center,
        style = Stroke(width = 1.5f),
      )

      val knobCenter = center + knobOffset
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(SummonerGold, Color(0xFFA86B10)),
          center = knobCenter,
          radius = outerR * 0.36f,
        ),
        radius = outerR * 0.33f,
        center = knobCenter,
      )
      drawCircle(
        color = Color.White.copy(alpha = 0.6f),
        radius = outerR * 0.33f,
        center = knobCenter,
        style = Stroke(width = 2f),
      )
    }
  }
}

@Composable
fun CameraZoomControls(
  zoom: Float,
  onZoomChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  horizontal: Boolean = true,
) {
  val containerMod = modifier
    .clip(RoundedCornerShape(12.dp))
    .background(SanctuaryStone.copy(alpha = 0.90f))
    .border(1.dp, SummonerGold.copy(alpha = 0.45f), RoundedCornerShape(12.dp))

  if (horizontal) {
    Row(
      modifier = containerMod,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = { onZoomChange((zoom - 0.15f).coerceAtLeast(0.65f)) },
        modifier = Modifier.size(48.dp).testTag("zoom_out_button"),
      ) {
        Icon(Icons.Default.Remove, contentDescription = "Thu nhỏ camera", tint = PortalCyan)
      }
      IconButton(
        onClick = { onZoomChange((zoom + 0.15f).coerceAtMost(1.65f)) },
        modifier = Modifier.size(48.dp).testTag("zoom_in_button"),
      ) {
        Icon(Icons.Default.Add, contentDescription = "Phóng to camera", tint = SummonerGold)
      }
    }
  } else {
    Column(
      modifier = containerMod,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      IconButton(
        onClick = { onZoomChange((zoom + 0.15f).coerceAtMost(1.65f)) },
        modifier = Modifier.size(48.dp).testTag("zoom_in_button"),
      ) {
        Icon(Icons.Default.Add, contentDescription = "Phóng to camera", tint = SummonerGold)
      }
      IconButton(
        onClick = { onZoomChange((zoom - 0.15f).coerceAtLeast(0.65f)) },
        modifier = Modifier.size(48.dp).testTag("zoom_out_button"),
      ) {
        Icon(Icons.Default.Remove, contentDescription = "Thu nhỏ camera", tint = PortalCyan)
      }
    }
  }
}

@Composable
fun PixelHeroPortraitAvatar(
  heroClass: HeroClassType?,
  sizeDp: Dp = 48.dp,
  modifier: Modifier = Modifier,
  heroName: String? = null,
) {
  val bg = when (heroClass) {
    HeroClassType.KNIGHT -> Color(0xFF1E3A5F)
    HeroClassType.ARCHER -> Color(0xFF1B4332)
    HeroClassType.MAGE -> Color(0xFF3B1963)
    HeroClassType.ASSASSIN -> Color(0xFF281A2D)
    HeroClassType.PRIEST -> Color(0xFF3B341F)
    HeroClassType.BERSERKER -> Color(0xFF4A151B)
    null -> Color(0xFF221A40)
  }

  Box(
    modifier = modifier
      .size(sizeDp)
      .clip(RoundedCornerShape(10.dp))
      .background(
        Brush.radialGradient(
          listOf(bg.copy(alpha = 0.95f), Color(0xFF0B0814))
        )
      )
      .border(1.5.dp, SummonerGold.copy(alpha = 0.78f), RoundedCornerShape(10.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(sizeDp * 0.90f)) {
      // Authored bust first (Lyra ships one), otherwise the legacy world-atlas crop.
      val bust = com.example.game.pixel.PremiumPortraits.get(heroName)
      if (bust != null) {
        drawImage(
          image = bust,
          dstOffset = IntOffset(0, 0),
          dstSize = IntSize(size.width.toInt().coerceAtLeast(2), size.height.toInt().coerceAtLeast(2)),
          filterQuality = FilterQuality.None,
        )
        return@Canvas
      }
      val premium = if (heroName == "Kaelen" && heroClass == HeroClassType.KNIGHT)
        com.example.game.pixel.PremiumActorAtlas.get("kaelen") else null
      val sprite = premium?.frame(true,false,false,0f,false,0f,false,0f) ?: if (heroClass == null) {
        PixelCharacterAtlas.masterSprites.idleRight.first()
      } else {
        PixelCharacterAtlas.heroSheet(heroClass).idleRight.first()
      }
      drawImage(
        image = sprite,
        srcOffset = if (premium == null) IntOffset(6, 3) else IntOffset(19, 4),
        srcSize = IntSize(26, 26),
        dstOffset = IntOffset(0, 0),
        dstSize = IntSize(size.width.toInt().coerceAtLeast(2), size.height.toInt().coerceAtLeast(2)),
        filterQuality = FilterQuality.None,
      )
    }
  }
}

@Composable
fun ResourcePillBadge(
  label: String,
  amount: Long,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(ObsidianVoid.copy(alpha = 0.88f))
      .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
      .padding(horizontal = 7.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(
      modifier = Modifier
        .size(7.dp)
        .clip(CircleShape)
        .background(accent),
    )
    Text(
      text = "$label $amount",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
