package com.example.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.game.combat.FieldRewardKind
import java.io.InputStream

/** Four immutable 64px PNGs; decoding is explicit, once, outside composition/drawing. */
object ExpeditionRewardIcons {
  private val cache = ExpeditionRewardIconCache()
  val decodedBytes: Long get() = cache.decodedBytes
  val decodeCount: Int get() = cache.decodeCount
  fun preload(context: Context) = cache.preload { filename -> context.assets.open(filename) }
  fun image(kind: FieldRewardKind): ImageBitmap? = cache.image(kind)
}

/** Loader boundary also permits genuine corrupt/absent-file checks without changing global state. */
internal class ExpeditionRewardIconCache {
  @Volatile private var images: Map<FieldRewardKind, ImageBitmap> = emptyMap()
  private var attempted = false
  @Volatile var decodedBytes: Long = 0L
    private set
  @Volatile var decodeCount: Int = 0
    private set

  @Synchronized fun preload(openAsset: (String) -> InputStream) {
    if (attempted) return
    val allocated = mutableListOf<android.graphics.Bitmap>()
    try {
      val loaded = FieldRewardKind.entries.associateWith { kind ->
        val filename = kind.name.lowercase(java.util.Locale.ROOT)
        openAsset("tower_field_rewards/$filename.png").use { stream ->
          val bitmap = checkNotNull(BitmapFactory.decodeStream(stream, null,
            BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 }))
          allocated += bitmap
          check(bitmap.width == 64 && bitmap.height == 64) { "Unexpected reward icon size: $filename" }
          bitmap.asImageBitmap()
        }
      }
      decodedBytes = allocated.sumOf { it.allocationByteCount.toLong() }
      decodeCount += loaded.size
      images = loaded
    } catch (error: Exception) {
      allocated.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("ExpeditionRewardIcons", "Optional icon pack unavailable; using text rewards", error)
    } catch (error: OutOfMemoryError) {
      allocated.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("ExpeditionRewardIcons", "Optional icons exceeded available memory; using text rewards")
    } finally {
      attempted = true
    }
  }

  fun image(kind: FieldRewardKind): ImageBitmap? = images[kind]
}
