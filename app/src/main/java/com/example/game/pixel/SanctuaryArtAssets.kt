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
  fun init(context: Context) { this.context = context.applicationContext }
  fun image(id: String): ImageBitmap? {
    val app = context ?: return null
    return images.getOrPut(id) {
      app.assets.open("sanctuary_p03/$id.png").use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inScaled = false })!!.asImageBitmap()
      }
    }
  }
  fun tiles(name: String, count: Int): List<ImageBitmap>? {
    if (context == null) return null
    return families.getOrPut(name) { (0 until count).map { image("${name}_$it")!! } }
  }
}
