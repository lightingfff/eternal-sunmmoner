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

  private fun effective(panel: Color): Color = panel.compositeOver(worstBackdrop)

  @Test
  fun hudPanelOpacityKeepsTextLegibleOverBrightTerrain() {
    val behind = effective(FantasySkin.Panel)
    val cases = listOf(
      "SummonerGold" to SummonerGold,
      "RuneSilver" to RuneSilver,
      "muted label" to Color(0xFF9EA6B2),
      "ArcaneViolet" to ArcaneViolet,
    )
    for ((name, color) in cases) {
      val ratio = SanctuaryUi.contrastRatio(color, behind)
      assertTrue("$name on the HUD panel is $ratio over bright terrain, below 4.5", ratio >= 4.5)
    }
  }

  @Test
  fun managementPanelGlassMeetsTheSameFloor() {
    val behind = effective(PanelGlass)
    // FantasySkin.Gold is the management panel's body text colour.
    val ratio = SanctuaryUi.contrastRatio(FantasySkin.Gold, behind)
    assertTrue("panel gold on glass is $ratio, below 4.5", ratio >= 4.5)
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
