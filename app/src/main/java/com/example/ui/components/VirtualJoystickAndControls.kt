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
import com.example.game.pixel.FantasyHd128CharacterAtlas
import com.example.game.pixel.FantasyHd128Structures
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
          colors = listOf(Color(0x55223656), Color(0x88101C30)),
          center = center,
          radius = outerR,
        ),
        radius = outerR,
        center = center,
      )
      // Double gilded RPG ring with astral cyan inner compass
      drawCircle(
        color = SummonerGold.copy(alpha = 0.72f),
        radius = outerR,
        center = center,
        style = Stroke(width = 2.6f),
      )
      drawCircle(
        color = PortalCyan.copy(alpha = 0.45f),
        radius = outerR * 0.66f,
        center = center,
        style = Stroke(width = 1.6f),
      )

      val knobCenter = center + knobOffset
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0xFFFFF3C4), SummonerGold, Color(0xFFB47818)),
          center = knobCenter,
          radius = outerR * 0.36f,
        ),
        radius = outerR * 0.33f,
        center = knobCenter,
      )
      drawCircle(
        color = Color.White.copy(alpha = 0.75f),
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
    .background(
      Brush.verticalGradient(
        listOf(Color(0xA61A2B44), Color(0xBF0F1A2C))
      )
    )
    .border(1.2.dp, SummonerGold.copy(alpha = 0.62f), RoundedCornerShape(12.dp))

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
    HeroClassType.KNIGHT -> Color(0xFF234876)
    HeroClassType.ARCHER -> Color(0xFF1F543D)
    HeroClassType.MAGE -> Color(0xFF4B227A)
    HeroClassType.ASSASSIN -> Color(0xFF3A1E46)
    HeroClassType.PRIEST -> Color(0xFF524420)
    HeroClassType.BERSERKER -> Color(0xFF631C26)
    null -> Color(0xFF2D2256)
  }

  Box(
    modifier = modifier
      .size(sizeDp)
      .clip(RoundedCornerShape(10.dp))
      .background(
        Brush.radialGradient(
          listOf(bg.copy(alpha = 0.92f), Color(0xCC101828))
        )
      )
      .border(1.5.dp, SummonerGold.copy(alpha = 0.85f), RoundedCornerShape(10.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(sizeDp * 0.92f)) {
      val baseSprite = if (heroClass == null) {
        FantasyHd128CharacterAtlas.masterSheet.idleRight.first()
      } else {
        FantasyHd128CharacterAtlas.heroSheet(heroClass).idleRight.first()
      }
      val hdSprite = FantasyHd128Structures.hdFor(
        baseSprite,
        FantasyHd128Structures.MaterialProfile.CHARACTER,
      )
      drawImage(
        image = hdSprite,
        srcOffset = IntOffset(28, 6),
        srcSize = IntSize(96, 96),
        dstOffset = IntOffset(0, 0),
        dstSize = IntSize(size.width.toInt().coerceAtLeast(2), size.height.toInt().coerceAtLeast(2)),
        filterQuality = FilterQuality.None,
      )
    }
  }
}

/** Violet for text/marks on dark glass; Color.kt ArcaneViolet is a fill colour. */
private val arcaneVioletOnDark = Color(0xFFB79BFF)

@Composable
fun ResourcePillBadge(
  label: String,
  amount: Long,
  accent: Color,
  modifier: Modifier = Modifier,
  lightOnIvory: Boolean = false,
) {
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(9.dp))
      .background(
        if (lightOnIvory) {
          Brush.verticalGradient(listOf(Color(0xFFF3EFE6), Color(0xFFE5DEC9)))
        } else {
          Brush.verticalGradient(listOf(Color(0xAD1B2C46), Color(0xC4101B2E)))
        }
      )
      .border(
        1.dp,
        if (lightOnIvory) Color(0xFFA7977D) else accent.copy(alpha = 0.68f),
        RoundedCornerShape(9.dp),
      )
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    val dotColor = if (lightOnIvory || accent.red + accent.green + accent.blue >= 1.5f) {
      accent
    } else {
      arcaneVioletOnDark
    }
    Box(
      modifier = Modifier
        .size(7.dp)
        .clip(CircleShape)
        .background(dotColor),
    )
    Text(
      text = "$label $amount",
      style = MaterialTheme.typography.labelSmall,
      color = if (lightOnIvory) Color(0xFF283B4B) else MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
