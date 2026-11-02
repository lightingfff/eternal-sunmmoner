package com.example.game.pixel

import androidx.compose.ui.graphics.ImageBitmap
import org.json.JSONObject

/**
 * Authored HUD busts for premium heroes (premium_slice/portraits/), keyed by hero name.
 *
 * Before this existed the HUD avatar cropped a 26x26 patch out of the *Kaelen* world atlas
 * and stretched it for every hero, so Lyra showed Kaelen's face. A hero without an authored
 * bust keeps the old crop path, so nothing regresses for the other classes.
 *
 * Decoding happens at most once per hero: [SanctuaryArtAssets.asset] caches the bitmap and
 * the result (including "no portrait") is memoised here, so a 60 fps HUD never re-decodes.
 */
internal object PremiumPortraits {
  private val cache = mutableMapOf<String, ImageBitmap?>()

  internal fun get(heroName: String?): ImageBitmap? {
    // Keyed on the raw display name: normalising it here would allocate a String on every
    // frame the HUD avatar is drawn.
    val key = heroName ?: return null
    if (key.isEmpty()) return null
    if (cache.containsKey(key)) return cache[key]
    val image = runCatching {
      val text = SanctuaryArtAssets.textAsset("premium_slice/manifest.json") ?: return@runCatching null
      val meta = JSONObject(text)
      if (!meta.has("portraits")) return@runCatching null
      val portraits = meta.getJSONObject("portraits")
      val manifestKey = key.lowercase()
      if (!portraits.has(manifestKey)) return@runCatching null
      val file = portraits.getJSONObject(manifestKey).getString("file")
      SanctuaryArtAssets.asset("premium_slice/$file")
    }.getOrNull()
    cache[key] = image
    return image
  }

  /** Test/QA hook: number of heroes with an authored bust resolved so far. */
  internal val resolvedCount get() = cache.count { it.value != null }
}
