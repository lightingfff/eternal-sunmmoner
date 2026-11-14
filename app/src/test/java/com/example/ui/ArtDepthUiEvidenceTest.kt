package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.game.pixel.AuthoredHeroAtlas
import com.example.game.pixel.SanctuaryArtAssets
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
@Config(sdk = [36], qualifiers = "w800dp-h360dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtDepthUiEvidenceTest {
  @get:Rule val rule = createComposeRule()

  @Test fun authoredPortraitCardsKeepNamesAndTapActionsOnShortLandscape() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    ESP28Assets.resetForTest()
    assertEquals(3, AuthoredHeroAtlas.loadedCount)
    val heroes = listOf("Kaelen" to HeroClassType.KNIGHT, "Lyra" to HeroClassType.ARCHER,
      "Vesper" to HeroClassType.MAGE).map { (name, type) ->
      HeroRecord(id = "art_$name", name = name, heroClass = type.name, rarity = 3L, level = 6L)
    }
    var view: View? = null
    var selected by mutableStateOf("Kaelen")
    rule.setContent {
      view = LocalView.current
      EternalSummonerTheme {
        Row(Modifier.fillMaxSize().background(Color(0xFF15202B)).padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          heroes.forEach { hero ->
            ESHeroCard(hero, selected = selected == hero.name, onClick = { selected = hero.name },
              modifier = Modifier.width(184.dp))
          }
        }
      }
    }
    for (hero in heroes) {
      rule.onNodeWithContentDescription("${hero.name}, cấp 6, 3 sao, ${hero.classType.labelVi}")
        .assertHasClickAction().performClick()
      rule.runOnIdle { assertEquals(hero.name, selected) }
    }
    rule.waitForIdle()
    val host = requireNotNull(view)
    val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
    host.draw(Canvas(bitmap))
    val path = File("build/visual_screenshots/art-depth-reforge/authored-hero-cards-800x360.png")
    path.parentFile?.mkdirs()
    path.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    assertTrue(path.length() > 10_000)
    bitmap.recycle()
  }
}
