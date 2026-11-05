package com.example.game.pixel

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap

/** Immutable for a process: the 4×4 grass overview cache must never retain the wrong profile. */
enum class SanctuaryVisualProfile { LEGACY, P27B }

/** Offline-authored PNGs. Cache lists as well as images; no decoding in warmed-up draw passes. */
object SanctuaryArtAssets {
  // P27B is enabled for this review build, but absent/corrupt candidate art always falls
  // back to the shipped P03/P21 PNG. Never mutate this at draw time or stale 4×4 grass
  // blocks/shadow masks would survive a profile change; use a fresh process for QA.
  val visualProfile = SanctuaryVisualProfile.P27B
  private var context: Context? = null
  private val images = mutableMapOf<String, ImageBitmap>()
  private val families = mutableMapOf<String, List<ImageBitmap>>()
  private val optionalFamilies = mutableMapOf<String, List<ImageBitmap>?>()
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
    } catch (e: IllegalArgumentException) {
      // Bad optional PNG data is not a reason to crash the world renderer.
      null
    }
    if (decoded != null) images[path] = decoded
    return decoded
  }

  /** Negative-cache an incomplete P27B family: no repeated asset opens inside draw loops.
   * The legacy family is still returned in its entirety (8/6 tiles), never a mixed atlas.
   * Restoring a missing file is an APK/process restart, not a hot profile switch.
   */
  internal fun p27bFamily(name: String): List<ImageBitmap>? {
    if (visualProfile != SanctuaryVisualProfile.P27B || context == null) return null
    if (optionalFamilies.containsKey(name)) return optionalFamilies[name]
    val cells = (0 until 16).map { index ->
      assetSafe("sanctuary_p27b/terrain/${name}_$index.png")
        ?.takeIf { it.width == 64 && it.height == 64 }
        ?: run { optionalFamilies[name] = null; return null }
    }
    optionalFamilies[name] = cells
    return cells
  }

  internal fun preferredLandmark(name: String, width: Int, height: Int): ImageBitmap? {
    if (visualProfile == SanctuaryVisualProfile.P27B) {
      assetSafe("sanctuary_p27b/structures/$name.png")
        ?.takeIf { it.width == width && it.height == height }?.let { return it }
    }
    return assetSafe("premium_slice/$name.png")
  }

  fun tiles(name: String, count: Int): List<ImageBitmap>? {
    if (context == null) return null
    return families.getOrPut(name) { (0 until count).map { image("${name}_$it")!! } }
  }
}
