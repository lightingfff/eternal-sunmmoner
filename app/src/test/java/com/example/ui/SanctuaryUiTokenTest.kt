package com.example.ui

import com.example.ui.theme.SanctuaryUi
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P27B-V5 checkpoint 4 (debt D): the sanctuary surface palette must be a system, not
 * 37 hand-written hex codes.
 *
 * These are contrast measurements on the tokens themselves, so a future panel that
 * picks a new near-grey fails here instead of shipping unreadable text. The numbers
 * are WCAG ratios against the surface the token is meant to sit on.
 */
class SanctuaryUiTokenTest {

  @Test
  fun textTokensClearTheirContrastFloor() {
    // 4.5:1 is the WCAG AA floor for normal text. [SanctuaryUi.inkTertiary] is the one
    // documented exception (hints only) and is excluded on purpose.
    val onCard = listOf(
      "ink" to SanctuaryUi.ink,
      "inkSecondary" to SanctuaryUi.inkSecondary,
      "accent" to SanctuaryUi.accent,
      "accentDeep" to SanctuaryUi.accentDeep,
      "brown" to SanctuaryUi.brown,
      "warning" to SanctuaryUi.warning,
      "success" to SanctuaryUi.success,
    )
    for ((name, color) in onCard) {
      val ratio = SanctuaryUi.contrastRatio(color, SanctuaryUi.card)
      assertTrue("$name on card is $ratio, below the 4.5 AA floor", ratio >= 4.5)
    }
    val onSurface = listOf("ink" to SanctuaryUi.ink, "inkSecondary" to SanctuaryUi.inkSecondary)
    for ((name, color) in onSurface) {
      val ratio = SanctuaryUi.contrastRatio(color, SanctuaryUi.surface)
      assertTrue("$name on surface is $ratio, below the 4.5 AA floor", ratio >= 4.5)
    }
  }

  @Test
  fun onAccentTextIsReadableOnTheAccentFill() {
    val ratio = SanctuaryUi.contrastRatio(SanctuaryUi.onAccent, SanctuaryUi.accent)
    assertTrue("onAccent on accent is $ratio, below 4.5", ratio >= 4.5)
  }

  /** The gold rule is decorative. This documents that it cannot carry text. */
  @Test
  fun goldIsDecorativeAndMustNotCarryText() {
    val ratio = SanctuaryUi.contrastRatio(SanctuaryUi.gold, SanctuaryUi.card)
    assertTrue("gold is $ratio on card; if this ever passes 4.5, revisit the guidance", ratio < 4.5)
    val inkRatio = SanctuaryUi.contrastRatio(SanctuaryUi.ink, SanctuaryUi.gold)
    assertTrue("ink must stay readable ON a gold fill ($inkRatio)", inkRatio >= 4.5)
  }

  @Test
  fun surfaceLayersAreOrdered() {
    // Each layer must be lighter than the one below it, or a "raised" card stops reading
    // as raised. Luminance is the only measure that holds across hue.
    val layers = listOf(
      SanctuaryUi.pill, SanctuaryUi.cardPressed, SanctuaryUi.surface, SanctuaryUi.card,
    ).map { SanctuaryUi.relativeLuminance(it) }
    for (i in 1 until layers.size) {
      assertTrue(
        "surface layering broke at index $i: ${layers[i - 1]} -> ${layers[i]}",
        layers[i] > layers[i - 1],
      )
    }
  }

  @Test
  fun spacingAndRadiusScalesAreMonotonic() {
    assertTrue(SanctuaryUi.radiusSmall < SanctuaryUi.radiusMedium)
    assertTrue(SanctuaryUi.radiusMedium < SanctuaryUi.radiusLarge)
    assertTrue(SanctuaryUi.spaceXs < SanctuaryUi.spaceSm)
    assertTrue(SanctuaryUi.spaceSm < SanctuaryUi.spaceMd)
    assertTrue(SanctuaryUi.spaceMd < SanctuaryUi.spaceLg)
    assertTrue("touch targets must stay >= 48dp", SanctuaryUi.minTouchTarget.value >= 48f)
  }
}
