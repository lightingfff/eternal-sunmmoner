package com.example.ui.components.p28

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.example.game.pixel.SanctuaryArtAssets
import org.json.JSONObject

/**
 * P28 "Astral Reliquary" design tokens — the dark-fantasy palette layered UNDER the
 * ivory sanctuary surfaces of [com.example.ui.theme.SanctuaryUi]. The two palettes
 * serve different worlds: SanctuaryUi is parchment-on-ivory for panels over the
 * bright island; these tokens are ink-on-dark for cards, notices and loading over
 * the dark manhwa theme.
 */
object ESTokens {
  val InkBlack = Color(0xFF10131A)
  val MidnightSlate = Color(0xFF1B2730)
  val BronzeDeep = Color(0xFF4B362A)
  val BronzeMid = Color(0xFF8C694B)
  val AntiqueGold = Color(0xFFD9BD7A)
  val IvoryPaper = Color(0xFFF0E8D8)
  val FrostSilver = Color(0xFFB6C5CB)
  val MysticCyan = Color(0xFF7CBFD3)
  val QuietViolet = Color(0xFF9F91BF)
  val DangerGarnet = Color(0xFFC96D70)
  val SuccessSage = Color(0xFF92B49A)
  val DarkText = Color(0xFF2E3438)

  val spaceXs = 4.dp
  val spaceSm = 8.dp
  val spaceMd = 12.dp
  val spaceLg = 16.dp
  val spaceXl = 24.dp

  /** Dark fill used behind notice text so the transparent 9-slice center stays readable. */
  val NoticeFill = Color(0xE6101826)
}

/** One entry of `ui_p28/manifest.json`. */
data class P28AssetEntry(
  val id: String,
  val path: String,
  val width: Int,
  val height: Int,
  val nineSliceInsetPx: Int,
)

/**
 * Resolver + cache for the P28 PNG pack (`app/src/main/assets/ui_p28/`).
 *
 * Decoding goes through [SanctuaryArtAssets.asset], which already caches per path and
 * never decodes on the draw pass; this layer adds a stable id -> manifest lookup and a
 * per-id memo (including "asset missing") so a recomposing grid does not even ask for
 * the same bitmap twice. Missing assets return null and callers MUST fall back to a
 * vector look instead of crashing — the fallback is a feature, not a hidden failure.
 */
object ESP28Assets {
  private val cache = mutableMapOf<String, ImageBitmap?>()
  private var manifest: Map<String, P28AssetEntry>? = null

  /** Manifest entry for [id], or null when the manifest or the id is missing. */
  fun entry(id: String): P28AssetEntry? {
    loadManifest()
    return manifest?.get(id)
  }

  /** Cached bitmap for [id], or null when the PNG is absent/corrupt. Never throws:
   *  [SanctuaryArtAssets.assetSafe] swallows decode failures and does not cache them. */
  fun image(id: String): ImageBitmap? {
    if (cache.containsKey(id)) return cache[id]
    val e = entry(id)
    val bmp = if (e == null) null else SanctuaryArtAssets.assetSafe(e.path)
    cache[id] = bmp
    return bmp
  }

  /** Convenience for `icons/icon_<name>.png` manifest entries. */
  fun icon(name: String): ImageBitmap? = image("icon_$name")

  private fun loadManifest() {
    if (manifest != null) return
    val text = runCatching { SanctuaryArtAssets.textAsset("ui_p28/manifest.json") }.getOrNull()
    if (text == null) {
      manifest = emptyMap()
      return
    }
    val parsed = mutableMapOf<String, P28AssetEntry>()
    runCatching {
      val assets = JSONObject(text).getJSONObject("assets")
      for (key in assets.keys()) {
        val a = assets.getJSONObject(key)
        parsed[key] = P28AssetEntry(
          id = key,
          path = a.getString("path"),
          width = a.getInt("width"),
          height = a.getInt("height"),
          nineSliceInsetPx = a.optInt("nineSliceInsetPx", 0),
        )
      }
    }
    manifest = parsed
  }

  /** Test/QA hook. */
  fun resetForTest() {
    cache.clear()
    manifest = null
  }
}
