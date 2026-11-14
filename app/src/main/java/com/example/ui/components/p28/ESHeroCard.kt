package com.example.ui.components.p28

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.game.pixel.PremiumPortraits

/**
 * P28 hero card — the full portrait card (REF-01 language, original art).
 *
 * Render order is fixed: backdrop -> portrait -> frame -> nameplate/stars -> selection
 * outline, so the frame can never end up under the portrait and the portrait can never
 * push the frame's anchors. All dynamic data (name, stars, class, level, status) is
 * rendered from [HeroRecord] state in code — nothing is baked into the PNGs.
 *
 * Frame material follows rarity: bronze (1-2), silver (3-4), obsidian (5).
 */
@Composable
fun ESHeroCard(
  hero: HeroRecord,
  selected: Boolean = false,
  onClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val rarity = hero.rarity.toInt().coerceIn(1, 5)
  val frameId = when {
    rarity >= 5 -> "card_frame_obsidian"
    rarity >= 3 -> "card_frame_silver"
    else -> "card_frame_bronze"
  }
  val backdrop = ESP28Assets.image("card_backdrop")
  val frame = ESP28Assets.image(frameId)
  val nameplate = ESP28Assets.image("card_nameplate")
  val starOn = ESP28Assets.image("rarity_star_on")
  val starOff = ESP28Assets.image("rarity_star_off")
  val portrait = remember(hero.name) { PremiumPortraits.get(hero.name) }
  val injured = hero.isCurrentlyInjured()

  Box(
    modifier = modifier
      .aspectRatio(2f / 3f)
      .shadow(if (selected) 6.dp else 2.dp, RoundedCornerShape(10.dp))
      .clip(RoundedCornerShape(10.dp))
      .clickable(enabled = !injured) { onClick() }
      .then(
        if (selected) Modifier.border(2.dp, ESTokens.AntiqueGold, RoundedCornerShape(10.dp))
        else Modifier
      )
      .semantics {
        contentDescription =
          "${hero.name}, cấp ${hero.level}, $rarity sao, ${hero.classType.labelVi}" +
            if (injured) ", đang dưỡng thương" else ""
      },
  ) {
    if (backdrop != null) {
      Image(backdrop, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    } else {
      Box(
        Modifier.fillMaxSize().background(
          Brush.radialGradient(listOf(Color(0xFF232B38), ESTokens.InkBlack)),
        ),
      )
    }
    if (portrait != null) {
      Image(
        portrait,
        contentDescription = null,
        modifier = Modifier.fillMaxSize().padding(top = 26.dp, bottom = 58.dp, start = 10.dp, end = 10.dp),
        contentScale = ContentScale.Crop,
      )
    } else {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = hero.classType.labelVi.firstOrNull()?.toString() ?: "?",
          color = ESTokens.FrostSilver.copy(alpha = 0.8f),
          style = MaterialTheme.typography.displaySmall,
        )
      }
    }
    if (frame != null) {
      Image(frame, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    }
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        if (nameplate != null) {
          Image(nameplate, null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
        } else {
          Box(
            Modifier.matchParentSize()
              .background(ESTokens.InkBlack.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
              .border(1.dp, ESTokens.BronzeMid, RoundedCornerShape(6.dp)),
          )
        }
        Text(
          text = hero.name,
          color = ESTokens.IvoryPaper,
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = 14.dp),
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) {
          val star = if (i <= rarity) starOn else starOff
          if (star != null) Image(star, null, Modifier.size(12.dp))
        }
      }
    }
  }
}

/**
 * Compact framed portrait used inside roster rows (party prep, hero management list).
 * Same asset language as [ESHeroCard] but sized for a row avatar; falls back to a
 * class-glyph tile when no authored portrait exists, never crashes.
 */
@Composable
fun ESMiniHeroPortrait(
  heroName: String?,
  heroClass: HeroClassType?,
  sizeDp: Dp,
  modifier: Modifier = Modifier,
) {
  val backdrop = ESP28Assets.image("card_backdrop")
  val frame = ESP28Assets.image("card_frame_bronze")
  val portrait = remember(heroName) { PremiumPortraits.get(heroName) }
  Box(modifier = modifier.size(sizeDp).clip(RoundedCornerShape(8.dp))) {
    if (backdrop != null) {
      Image(backdrop, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    } else {
      Box(Modifier.fillMaxSize().background(ESTokens.MidnightSlate))
    }
    if (portrait != null) {
      Image(
        portrait,
        contentDescription = null,
        modifier = Modifier.fillMaxSize().padding(sizeDp * 0.16f),
        contentScale = ContentScale.Crop,
      )
    } else {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = heroClass?.labelVi?.firstOrNull()?.toString() ?: "?",
          color = ESTokens.AntiqueGold,
          style = MaterialTheme.typography.titleMedium,
        )
      }
    }
    if (frame != null) {
      Image(frame, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    } else {
      Box(Modifier.matchParentSize().border(1.5.dp, ESTokens.AntiqueGold.copy(alpha = 0.78f), RoundedCornerShape(8.dp)))
    }
  }
}
