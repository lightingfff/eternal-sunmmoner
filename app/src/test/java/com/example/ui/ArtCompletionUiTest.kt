package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.game.pixel.PremiumPortraits
import com.example.game.pixel.SanctuaryArtAssets
import com.example.ui.components.PixelHeroPortraitAvatar
import com.example.ui.components.p28.ESHeroCard
import com.example.ui.components.p28.ESP28Assets
import com.example.ui.theme.EternalSummonerTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtCompletionUiTest {
  @get:Rule val rule = createComposeRule()
  private fun inspect(portrait: Boolean) {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    ESP28Assets.resetForTest()
    var host: View? = null
    var selected by mutableStateOf("")
    val heroes = HeroClassType.entries.map { HeroRecord(id = it.name, name = "Test",
      heroClass = it.name, rarity = 3L, level = 6L) }
    val images = heroes.map { requireNotNull(PremiumPortraits.get(it.name, it.classType)) }
    assertEquals("Same-name heroes of different classes need different portraits", 6, images.distinct().size)
    rule.setContent {
      host = LocalView.current
      EternalSummonerTheme {
        Column(Modifier.fillMaxSize().background(Color(0xFF15202B)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          heroes.chunked(if (portrait) 3 else 6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              row.forEach { hero ->
                Column(Modifier.width(if (portrait) 96.dp else 116.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  PixelHeroPortraitAvatar(hero.classType, 56.dp, heroName = hero.name)
                  ESHeroCard(hero, selected == hero.id, { selected = hero.id })
                }
              }
            }
          }
        }
      }
    }
    heroes.forEach { hero ->
      rule.onNodeWithContentDescription("Test, cấp 6, 3 sao, ${hero.classType.labelVi}")
        .assertHasClickAction().performTouchInput { click(center) }
      rule.runOnIdle { assertEquals(hero.id, selected) }
    }
    rule.waitForIdle()
    val view = requireNotNull(host)
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    val output = File("build/visual_screenshots/art-completion/ui_${if (portrait) "portrait" else "800x360"}.png")
    output.parentFile?.mkdirs()
    output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    assertTrue(output.length() > 10_000)
    bitmap.recycle()
  }
  @Test @Config(qualifiers = "w800dp-h360dp-land-mdpi")
  fun actualHudAvatarsAndCardsKeepClassIdentityAndPhysicalTapOnShortLandscape() = inspect(false)
  @Test @Config(qualifiers = "w360dp-h640dp-port-mdpi")
  fun actualHudAvatarsAndCardsKeepClassIdentityAndPhysicalTapOnPortrait() = inspect(true)
}
