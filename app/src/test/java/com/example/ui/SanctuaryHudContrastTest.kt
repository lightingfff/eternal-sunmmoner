package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.example.ui.components.FantasySkin
import com.example.ui.components.PanelGlass
import com.example.ui.theme.ArcaneViolet
import com.example.ui.theme.RuneSilver
import com.example.ui.theme.SummonerGold
import com.example.ui.theme.SanctuaryUi
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P27B-V5 checkpoint 5 (debt D): the lobby HUD is translucent glass floating over a real
 * pixel-art world, so its text has no fixed background. Contrast therefore has to be
 * checked against the WORST pixel the lobby can place behind a panel, not against the
 * panel colour itself.
 *
 * This exists because measuring it found a genuine defect: at the pre-CP5 opacity of
 * 71% the standard gold HUD label fell to 4.0:1 and the muted 0x9EA6B2 labels to 2.9:1
 * when a panel sat over the bright ivory gate facade — the state that made the top bar
 * look washed out in the landscape screenshot.
 */
class SanctuaryHudContrastTest {

  /** Brightest backdrop the sanctuary world produces: lit ivory stone highlights. */
  private val worstBackdrop = Color(0xFFF2EAD9)

  /** Same value the pill uses for its identity dot on dark glass. */
  private val accentDot = SanctuaryUi.violetOnDark

  private fun effective(panel: Color): Color = panel.compositeOver(worstBackdrop)

  @Test
  fun hudPanelOpacityKeepsTextLegibleOverBrightTerrain() {
    val behind = effective(FantasySkin.Panel)
    // Text tokens: 4.5:1 (WCAG AA for normal text).
    val textCases = listOf(
      "SummonerGold" to SummonerGold,
      "RuneSilver" to RuneSilver,
      "muted label" to Color(0xFF9EA6B2),
      "violetOnDark" to SanctuaryUi.violetOnDark,
    )
    for ((name, color) in textCases) {
      val ratio = SanctuaryUi.contrastRatio(color, behind)
      assertTrue("$name on the HUD panel is $ratio over bright terrain, below 4.5", ratio >= 4.5)
    }
    // Graphic tokens: 3:1, the WCAG non-text floor. These are the resource pill's identity
    // dot and the parchment-tone gold; they carry meaning but not glyphs.
    val markCases = listOf("accent dot" to accentDot)
    for ((name, color) in markCases) {
      val ratio = SanctuaryUi.contrastRatio(color, behind)
      assertTrue("$name on the HUD panel is $ratio, below the 3:1 mark floor", ratio >= 3.0)
    }
  }

  /**
   * The bug this test was written for, kept as a regression.
   *
   * ArcaneViolet (0xFF9D6BFF) is a good FILL violet, but it was being used as text for the
   * mage class label and the summoning-hall title, where it measures 3.5:1 on the HUD panel
   * and ~1.3:1 against the ivory world. If someone switches the light violet back to
   * ArcaneViolet for text, the assertion above fails; this one explains why.
   */
  @Test
  fun arcaneVioletIsAFillNotATextColourOnGlass() {
    val behind = effective(FantasySkin.Panel)
    val asText = SanctuaryUi.contrastRatio(ArcaneViolet, behind)
    assertTrue("ArcaneViolet unexpectedly passes as text ($asText); revisit the token split", asText < 4.5)
    val replacement = SanctuaryUi.contrastRatio(SanctuaryUi.violetOnDark, behind)
    assertTrue("violetOnDark must be the readable replacement ($replacement)", replacement >= 4.5)
  }

  @Test
  fun managementPanelGlassMeetsTheSameFloor() {
    val behind = effective(PanelGlass)
    // FantasySkin.Gold is the management panel's body text colour.
    val ratio = SanctuaryUi.contrastRatio(FantasySkin.Gold, behind)
    assertTrue("panel gold on glass is $ratio, below 4.5", ratio >= 4.5)
  }

  /**
   * Login screen (CP6). Its scrim is what makes the whole screen readable: the title is
   * SummonerGold, the tagline PortalCyan, the subtitle ParchmentLight, and those measure
   * 1.37 / 1.34 / 1.3:1 on a light backdrop - so the scrim, not the text colour, carries
   * it. At the old 0.42 alpha the title sat at 4.45:1 and the tagline at 3.69:1 over the
   * brightest world. This pins the current values so a restyle cannot quietly undo it.
   */
  @Test
  fun authGateScrimKeepsItsTextReadableOverTheBrightestWorld() {
    val scrim = Color(0xFF0A0812).copy(alpha = 0.76f).compositeOver(worstBackdrop)
    val cases = listOf(
      "title (SummonerGold)" to SummonerGold,
      "tagline (PortalCyan)" to Color(0xFF38BDF8),
      "subtitle (ParchmentLight)" to Color(0xFFF5F1E7),
    )
    for ((name, color) in cases) {
      val ratio = SanctuaryUi.contrastRatio(color, scrim)
      assertTrue("$name on the auth scrim is $ratio, below 4.5", ratio >= 4.5)
    }
  }

  /**
   * The panel must stay translucent: an opaque slab would hide the world, which is the
   * whole point of the lobby. This pins the other side of the trade-off so nobody
   * "fixes" contrast by solidifying every panel.
   */
  @Test
  fun panelsStayTranslucentSoTheWorldStillReadsThrough() {
    assertTrue("HUD panel became opaque", FantasySkin.Panel.alpha in 0.80f..0.94f)
    assertTrue("management glass became opaque", PanelGlass.alpha in 0.80f..0.94f)
  }
}
