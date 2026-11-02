package com.example.game.pixel

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap

/** Offline-authored PNGs. Cache lists as well as images; no decoding in warmed-up draw passes. */
object SanctuaryArtAssets {
  private var context: Context? = null
  private val images = mutableMapOf<String, ImageBitmap>()
  private val families = mutableMapOf<String, List<ImageBitmap>>()
  internal val cachedImageCount get() = images.size
  /** Decoded PNG cache only; generated actor atlases and GPU copies are not included. Sample at 1 Hz. */
  internal val cachedBitmapBytes: Long get() = images.values.sumOf { it.asAndroidBitmap().allocationByteCount.toLong() }
  fun init(context: Context) { this.context = context.applicationContext }
  fun image(id: String): ImageBitmap? = asset("sanctuary_p03/$id.png")
  internal fun textAsset(path: String): String? = context?.assets?.open(path)?.bufferedReader()?.use { it.readText() }
  internal fun asset(path: String): ImageBitmap? {
    if (context == null) return null
    return images.getOrPut(path) {
      decodeAsset(path)!!
    }
  }
  // Atlas source is temporary: only the extracted frames stay cached, not both copies.
  internal fun decodeAsset(path: String): ImageBitmap? = context?.assets?.open(path)?.use {
    BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inScaled = false })!!.asImageBitmap()
  }
  /**
   * P21 loader: a missing or corrupt *optional* PNG falls back to the procedural sprite instead of
   * throwing. [asset] uses `decodeAsset(path)!!`, so an absent file would throw before any
   * `?: fallback` Elvis branch could run — this helper closes that gap without changing [asset].
   *
   * A failed read is never cached (one bad file cannot poison the cache); a successful decode is
   * cached exactly like [asset], so steady-state cost stays one blit per facility per frame.
   */
  internal fun assetSafe(path: String): ImageBitmap? {
    val ctx = context ?: return null
    images[path]?.let { return it }
    val decoded = try {
      ctx.assets.open(path).use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inScaled = false })
          ?.asImageBitmap()
      }
    } catch (e: java.io.IOException) {
      null
    }
    if (decoded != null) images[path] = decoded
    return decoded
  }
  fun tiles(name: String, count: Int): List<ImageBitmap>? {
    if (context == null) return null
    return families.getOrPut(name) { (0 until count).map { image("${name}_$it")!! } }
  }
}
