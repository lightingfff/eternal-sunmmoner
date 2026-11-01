package com.example.game.pixel

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Offline-authored PNGs. Cache lists as well as images; no decoding in warmed-up draw passes. */
object SanctuaryArtAssets {
  private var context: Context? = null
  private val images = mutableMapOf<String, ImageBitmap>()
  private val families = mutableMapOf<String, List<ImageBitmap>>()
  internal val cachedImageCount get() = images.size
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
  fun tiles(name: String, count: Int): List<ImageBitmap>? {
    if (context == null) return null
    return families.getOrPut(name) { (0 until count).map { image("${name}_$it")!! } }
  }
}
