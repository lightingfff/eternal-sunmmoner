package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared visual contract for player-facing Eternal Summoner UI.
 *
 * The map and Tower art remain their own renderers; these tokens describe the Compose
 * chrome above them. Bright-island parchment cards continue to use [SanctuaryUi]. The
 * values here are deliberately high-contrast dark-fantasy roles, not one-off screen colors.
 */
object ESDesignTokens {
  // Layer stack: backdrop -> inset -> panel -> raised panel.
  val backdrop = Color(0xFF080B12)
  val panel = Color(0xE0141D2A)
  val hudGlass = Color(0xCC141D2A)
  val panelInset = Color(0xF10C1420)
  val panelRaised = Color(0xF51B2736)
  val panelHighlight = Color(0xFF263547)
  val scrim = Color(0xC6080C14)

  // Warm metal and cool portal accents.
  val gold = Color(0xFFF0CA75)
  val goldMuted = Color(0xFFB79555)
  val cyan = Color(0xFF82D8E8)
  val violet = Color(0xFFC3A9FF)
  val success = Color(0xFF98D5A7)
  val warning = Color(0xFFF1C27B)
  val danger = Color(0xFFFF9696)

  // Body copy is intentionally brighter than the old muted HUD grey.
  val textPrimary = Color(0xFFF4EFE6)
  val textSecondary = Color(0xFFD0CDDA)
  val textMuted = Color(0xFFADB6C3)
  val border = Color(0xFF495365)
  val hairline = Color(0xFF3B4656)

  val radiusSmall = 8.dp
  val radiusMedium = 12.dp
  val radiusLarge = 18.dp
  val minTouchTarget = 48.dp
  val spaceXs = 4.dp
  val spaceSm = 8.dp
  val spaceMd = 12.dp
  val spaceLg = 16.dp
  val spaceXl = 24.dp
}
