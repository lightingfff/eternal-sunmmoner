package com.example.game.pixel

import androidx.compose.ui.graphics.ImageBitmap
import com.example.data.model.HeroClassType
import org.json.JSONObject

/**
 * Named busts, legacy optional portraits and six class fallbacks, keyed by name and class.
 * Preloaded banks are shared by reference. Optional legacy images and misses are memoised;
 * a same-name/different-class hero must not borrow the wrong named bust.
 */
internal object PremiumPortraits {
  private val cache = mutableMapOf<Pair<String?, HeroClassType?>, ImageBitmap?>()

  internal fun get(heroName: String?, type: HeroClassType? = null): ImageBitmap? {
    val key = heroName to type
    AuthoredHeroAtlas.portrait(heroName, type)?.let { image ->
      if (cache[key] !== image) cache[key] = image
      return image
    }
    cache[key]?.let { return it }
    val fallback = CompletionClassAtlas.portrait(type)
    if (cache.containsKey(key)) return fallback?.also { cache[key] = it }
    val image = runCatching {
      if (heroName.isNullOrEmpty()) return@runCatching null
      // A known display name with the wrong class must not select that named portrait.
      val knownType = AuthoredHeroAtlas.knownClass(heroName)
      if (type != null && knownType != null && knownType != type) return@runCatching null
      val text = SanctuaryArtAssets.textAsset("premium_slice/manifest.json") ?: return@runCatching null
      val meta = JSONObject(text)
      if (!meta.has("portraits")) return@runCatching null
      val portraits = meta.getJSONObject("portraits")
      val manifestKey = heroName.lowercase()
      if (!portraits.has(manifestKey)) return@runCatching null
      val file = portraits.getJSONObject(manifestKey).getString("file")
      SanctuaryArtAssets.asset("premium_slice/$file")
    }.getOrNull() ?: fallback
    cache[key] = image
    return image
  }

  /** Test/QA hook: number of heroes with an authored bust resolved so far. */
  internal val resolvedCount get() = cache.count { it.value != null }
}
