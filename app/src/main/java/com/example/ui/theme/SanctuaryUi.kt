package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * P27B-V5 design system for the sanctuary surfaces (debt D).
 *
 * Why this file exists: the sanctuary UI (HUD, facility modals, auth gate, loading)
 * had no token layer at all. The same ivory palette was typed out by hand 37 times
 * across two files, with three near-identical card colours and no rule for when to use
 * which. Nothing could drift together, and a new panel could not look like the others
 * without copying hex codes.
 *
 * Scope: this is deliberately the SANCTUARY surface palette, not the app theme. The
 * app's dark manhwa theme (`Color.kt`) stays untouched for screens outside the island;
 * these surfaces are ivory-on-parchment and sit on top of the bright world.
 *
 * Rules encoded here (and enforced by `SanctuaryUiTokenTest`):
 *  - text colours must clear a contrast floor against the surface they sit on, so a
 *    new panel cannot ship grey-on-ivory;
 *  - one gold and one ink, not five; tones are named by ROLE, not by value;
 *  - radii/space come in one scale, so panels stop inventing 12/13/14dp corners.
 */
object SanctuaryUi {
  // ---- surfaces ----------------------------------------------------------
  /** Page-level parchment behind everything in a sanctuary surface. */
  val surface = Color(0xFFF5F1E7)

  /** A raised panel or list row. */
  val card = Color(0xFFFFFDF8)

  /** A pressed / selected row, one step off [card]. */
  val cardPressed = Color(0xFFF3EEE2)

  /** Muted pill background, e.g. the resource chips in the modal header. */
  val pill = Color(0xFFEBE6DB)

  /** Hairline divider inside a panel. */
  val divider = Color(0xFFD6CDBA)

  // ---- strokes -----------------------------------------------------------
  /** Gold rule around an emphasised panel. Decorative only: 2.3:1, so it must never
   *  carry text or be the only indicator of state. */
  val gold = Color(0xFFC7A36C)

  /** Softer gold for secondary emphasis and dividers on gold-tinted panels. */
  val goldSoft = Color(0xFFA7977D)

  // ---- ink ---------------------------------------------------------------
  /** Primary text. Measured 11.4:1 on [card]. */
  val ink = Color(0xFF283B4B)

  /** Secondary text and labels. Measured 6.1:1 on [card]. */
  val inkSecondary = Color(0xFF506474)

  /** Tertiary / hint text on parchment. Measured 4.2:1 on [card], 3.8:1 on [surface]:
   *  acceptable for non-essential hints only, never for the only copy of a message. */
  val inkTertiary = Color(0xFF6B7C8A)

  // ---- accents -----------------------------------------------------------
  /** Interactive accent: buttons, links, active tabs. Measured 5.3:1 on [card]. */
  val accent = Color(0xFF2D728C)

  /** Deep accent for pressed states and outlined controls. */
  val accentDeep = Color(0xFF416D82)

  /** Warm brown for flavour text and section headers. Measured 6.6:1 on [card]. */
  val brown = Color(0xFF79552E)

  /** Warning / insufficient-resource text. Measured 5.4:1 on [card]. */
  val warning = Color(0xFF9B572F)

  /** Success confirmation. Measured 5.9:1 on [card]. */
  val success = Color(0xFF2F6F4E)

  /** On-accent text (button labels). */
  val onAccent = Color(0xFFFFFFFF)

  /**
   * Violet for TEXT on dark glass.
   *
   * `Color.kt`'s [ArcaneViolet] (0xFF9D6BFF) is a good graphic/fill violet but measures
   * only 3.5:1 against the HUD panel, so it cannot carry the mage class label or the
   * summoning-hall title — those were the least readable lines in the hero card.
   * This lighter violet is 5.3:1 on the same glass and keeps the arcane identity.
   */
  val violetOnDark = Color(0xFFB79BFF)

  // ---- shape / spacing scale --------------------------------------------
  val radiusSmall = 8.dp
  val radiusMedium = 12.dp
  val radiusLarge = 16.dp

  val spaceXs = 4.dp
  val spaceSm = 8.dp
  val spaceMd = 12.dp
  val spaceLg = 16.dp

  /** Minimum touch target; keeps new controls equal to the existing buttons. */
  val minTouchTarget = 48.dp

  val panelShape = RoundedCornerShape(radiusLarge)
  val cardShape = RoundedCornerShape(radiusMedium)
  val chipShape = RoundedCornerShape(radiusSmall)

  /**
   * WCAG relative luminance, so contrast is a measurement instead of an opinion.
   * Exposed for the token test; not used in the draw pass.
   */
  fun relativeLuminance(color: Color): Double {
    fun channel(value: Float): Double {
      val c = value.toDouble()
      return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) +
      0.0722 * channel(color.blue)
  }

  /** Contrast ratio between two colours, 1.0 (identical) to 21.0 (black/white). */
  fun contrastRatio(foreground: Color, background: Color): Double {
    val a = relativeLuminance(foreground)
    val b = relativeLuminance(background)
    val lighter = maxOf(a, b)
    val darker = minOf(a, b)
    return (lighter + 0.05) / (darker + 0.05)
  }
}
