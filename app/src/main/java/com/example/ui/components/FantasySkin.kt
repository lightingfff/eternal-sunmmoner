package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.Surface as MaterialSurface
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.components.p28.ESP28Assets
import com.example.ui.components.p28.drawNineSliceScaled
import com.example.ui.theme.ESDesignTokens

/** Shared dark-fantasy chrome for management, Lobby, Tower preparation and battle HUD. */
object FantasySkin {
  // These values are also the shared Compose tokens; the generated frame is chrome, not copy.
  val Panel = ESDesignTokens.panel
  val HudGlass = ESDesignTokens.hudGlass
  val Gold = ESDesignTokens.gold

  const val PanelFrameId = "global_panel_frame"
  const val PrimaryButtonFrameId = "button_primary"
  const val ChoiceButtonFrameId = "button_secondary"

  /** A restrained raised-metal edge for code-drawn chrome without a matching bitmap frame. */
  fun Modifier.materialEdge(): Modifier = drawWithCache {
    val inset = 3.dp.toPx()
    val weight = 1.dp.toPx()
    onDrawWithContent {
      drawContent()
      if (size.width > inset * 2 && size.height > inset * 2) {
        drawLine(
          Color(0xFFEFDCAB).copy(alpha = .38f),
          Offset(inset * 2, inset),
          Offset(size.width - inset * 2, inset),
          weight,
        )
        drawLine(
          Color(0xFF050B14).copy(alpha = .70f),
          Offset(inset * 2, size.height - inset),
          Offset(size.width - inset * 2, size.height - inset),
          weight * 1.5f,
        )
      }
    }
  }

  /** Button fills are translucent over the authored face, retaining per-action tint. */
  @Composable
  fun buttonColors(
    containerColor: Color = Panel,
    contentColor: Color = Gold,
    disabledContainerColor: Color = Color(0xff242a30),
    disabledContentColor: Color = Color(0xff80868d),
  ): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = lerp(Panel, containerColor, .24f).copy(alpha = .24f),
    contentColor = if (contentColor.red + contentColor.green + contentColor.blue < 1f) Gold else contentColor,
    disabledContainerColor = disabledContainerColor.copy(alpha = .48f),
    disabledContentColor = disabledContentColor,
  )
}

/**
 * Material surface with the shared generated panel artwork between its live background
 * fill and Compose content. Small chips and HUD strips keep their simple shape; full cards
 * and dialogs receive the transparent nine-slice frame.
 */
@Composable
fun FantasySurface(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(12.dp),
  color: Color = MaterialTheme.colorScheme.surface,
  contentColor: Color = contentColorFor(color),
  tonalElevation: androidx.compose.ui.unit.Dp = 0.dp,
  shadowElevation: androidx.compose.ui.unit.Dp = 0.dp,
  border: BorderStroke? = null,
  frameAssetId: String? = FantasySkin.PanelFrameId,
  frameCornerInset: Dp = 20.dp,
  compactFrame: Boolean = false,
  content: @Composable () -> Unit,
) {
  val frame = frameAssetId?.let(ESP28Assets::image)
  val sourceInset = frameAssetId?.let { ESP28Assets.entry(it)?.nineSliceInsetPx } ?: 56
  val destinationInset = with(LocalDensity.current) { frameCornerInset.roundToPx() }

  MaterialSurface(
    modifier = modifier,
    shape = shape,
    color = Color.Transparent,
    contentColor = contentColor,
    tonalElevation = tonalElevation,
    shadowElevation = shadowElevation,
  ) {
    Box {
      MaterialSurface(
        modifier = Modifier.matchParentSize(),
        shape = shape,
        color = color,
        contentColor = contentColor,
        border = border,
      ) {}
      if (frame != null) {
        Canvas(Modifier.matchParentSize()) {
          if ((compactFrame || (size.width >= 120.dp.toPx() && size.height >= 84.dp.toPx())) &&
            size.height > destinationInset * 2f &&
            size.width > destinationInset * 2f
          ) {
            drawNineSliceScaled(frame, sourceInset, destinationInset, alpha = 0.94f)
          }
        }
      }
      content()
    }
  }
}

@Composable
fun FantasyButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape = RoundedCornerShape(8.dp),
  colors: ButtonColors = FantasySkin.buttonColors(),
  elevation: ButtonElevation? = null,
  border: BorderStroke? = null,
  contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
  interactionSource: MutableInteractionSource? = null,
  content: @Composable RowScope.() -> Unit,
) {
  val frameId = FantasySkin.PrimaryButtonFrameId
  val frame = ESP28Assets.image(frameId)
  val sourceInset = ESP28Assets.entry(frameId)?.nineSliceInsetPx ?: 56
  val destinationInset = with(LocalDensity.current) { 12.dp.roundToPx() }
  val raised = elevation ?: ButtonDefaults.buttonElevation(
    defaultElevation = 2.dp,
    pressedElevation = 0.dp,
    focusedElevation = 2.dp,
    hoveredElevation = 3.dp,
    disabledElevation = 0.dp,
  )
  val baseModifier = modifier.heightIn(min = ESDesignTokens.minTouchTarget)
  val chromeModifier = if (frame == null) {
    with(FantasySkin) { baseModifier.materialEdge() }
  } else {
    baseModifier.drawBehind {
      if (size.width > destinationInset * 2f && size.height > destinationInset * 2f) {
        drawNineSliceScaled(frame, sourceInset, destinationInset)
      }
    }
  }

  MaterialButton(
    onClick = onClick,
    modifier = chromeModifier,
    enabled = enabled,
    shape = shape,
    colors = colors,
    elevation = raised,
    border = border ?: if (frame == null) BorderStroke(1.dp, FantasySkin.Gold.copy(alpha = .5f)) else null,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content,
  )
}

/** Secondary outlined control; true [selected] uses the gold primary frame, false uses the
 * choice frame with an explicit unselected state, and null leaves selection semantics unset. */
@Composable
fun FantasyChoiceButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape = RoundedCornerShape(8.dp),
  colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
    contentColor = FantasySkin.Gold,
    disabledContentColor = Color(0xff80868d),
  ),
  border: BorderStroke? = null,
  contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
  interactionSource: MutableInteractionSource? = null,
  selected: Boolean? = null,
  content: @Composable RowScope.() -> Unit,
) {
  val frameId = if (selected == true) FantasySkin.PrimaryButtonFrameId else FantasySkin.ChoiceButtonFrameId
  val frame = ESP28Assets.image(frameId)
  val sourceInset = ESP28Assets.entry(frameId)?.nineSliceInsetPx ?: 56
  val destinationInset = with(LocalDensity.current) { 12.dp.roundToPx() }
  val baseModifier = modifier.heightIn(min = ESDesignTokens.minTouchTarget)
  val chromeModifier = if (frame == null) {
    baseModifier
  } else {
    baseModifier.drawBehind {
      if (size.width > destinationInset * 2f && size.height > destinationInset * 2f) {
        drawNineSliceScaled(frame, sourceInset, destinationInset)
      }
    }
  }

  val accessibleModifier = selected?.let { isSelected ->
    chromeModifier.semantics { this.selected = isSelected }
  } ?: chromeModifier
  MaterialOutlinedButton(
    onClick = onClick,
    modifier = accessibleModifier,
    enabled = enabled,
    shape = shape,
    colors = colors,
    border = border ?: if (frame == null) BorderStroke(1.dp, FantasySkin.Gold.copy(alpha = .45f)) else null,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content,
  )
}

/** Compact tertiary action used for tabs, dismissals and secondary HUD details. */
@Composable
fun FantasyTextButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape = RoundedCornerShape(8.dp),
  colors: ButtonColors = ButtonDefaults.textButtonColors(
    contentColor = FantasySkin.Gold,
    disabledContentColor = Color(0xff80868d),
  ),
  contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
  interactionSource: MutableInteractionSource? = null,
  selected: Boolean? = null,
  content: @Composable RowScope.() -> Unit,
) {
  val frameId = if (selected == true) FantasySkin.PrimaryButtonFrameId else FantasySkin.ChoiceButtonFrameId
  val frame = ESP28Assets.image(frameId)
  val sourceInset = ESP28Assets.entry(frameId)?.nineSliceInsetPx ?: 56
  val destinationInset = with(LocalDensity.current) { 8.dp.roundToPx() }
  val baseModifier = modifier.heightIn(min = ESDesignTokens.minTouchTarget)
  val chromeModifier = if (frame == null) baseModifier else baseModifier.drawBehind {
    if (size.width > destinationInset * 2f && size.height > destinationInset * 2f) {
      drawNineSliceScaled(frame, sourceInset, destinationInset, alpha = if (selected == true) 0.9f else 0.50f)
    }
  }

  val accessibleModifier = selected?.let { isSelected ->
    chromeModifier.semantics { this.selected = isSelected }
  } ?: chromeModifier
  MaterialTextButton(
    onClick = onClick,
    modifier = accessibleModifier,
    enabled = enabled,
    shape = shape,
    colors = colors,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content,
  )
}

/** Small icon control with matching inset brass trim and the standard Material ripple. */
@Composable
fun FantasyIconButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  interactionSource: MutableInteractionSource? = null,
  content: @Composable () -> Unit,
) {
  val chromeModifier = modifier.drawBehind {
    val corner = CornerRadius(8.dp.toPx())
    drawRoundRect(Color(0x80121B2B), cornerRadius = corner)
    drawRoundRect(
      color = FantasySkin.Gold.copy(alpha = if (enabled) 0.34f else 0.14f),
      cornerRadius = corner,
      style = Stroke(width = 1.dp.toPx()),
    )
    drawLine(
      color = Color(0xFFEFDCAB).copy(alpha = 0.22f),
      start = Offset(8.dp.toPx(), 1.dp.toPx()),
      end = Offset(size.width - 8.dp.toPx(), 1.dp.toPx()),
      strokeWidth = 1.dp.toPx(),
    )
  }
  CompositionLocalProvider(
    LocalContentColor provides if (enabled) FantasySkin.Gold else Color(0xff80868d),
  ) {
    MaterialIconButton(
      onClick = onClick,
      modifier = chromeModifier,
      enabled = enabled,
      interactionSource = interactionSource,
      content = content,
    )
  }
}

@Composable
fun RewardSummary(gold: Long, mana: Long, shards: Long, ore: Long, essence: Long) {
  androidx.compose.foundation.layout.Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    androidx.compose.material3.Text(
      "CHIẾN LỢI PHẨM ĐÃ LƯU",
      color = FantasySkin.Gold,
      style = MaterialTheme.typography.labelMedium,
    )
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      for ((label, value) in listOf("Vàng" to gold, "Mana" to mana, "Mảnh hồn" to shards)) {
        FantasyPanel(Modifier.weight(1f)) {
          androidx.compose.foundation.layout.Column(Modifier.padding(10.dp)) {
            androidx.compose.material3.Text(
              "+$value",
              color = Color(0xfff1e5c9),
              style = MaterialTheme.typography.titleMedium,
            )
            androidx.compose.material3.Text(
              label,
              color = Color(0xffaebcc9),
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }
    }
    androidx.compose.material3.Text(
      "+$ore Quặng sắt  •  +$essence Tinh hoa",
      color = Color(0xffb7c4bd),
      style = MaterialTheme.typography.bodySmall,
    )
  }
}
