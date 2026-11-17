package com.example.game.pixel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.security.MessageDigest
import org.json.JSONObject

/** Optional retextured buildings retain the old canvas, world scale and shared shadow lookup. */
internal object AuthoredStructureAtlas {
  private var attempted = false
  private var hall: ImageBitmap? = null
  internal var retainedBytes = 0L; private set
  @Synchronized fun preload(context: Context) {
    if (attempted) return
    attempted = true
    var owned: Bitmap? = null
    try {
      val root = "art_depth_v1/structures"
      val meta = context.assets.open("$root/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
      require(meta.getInt("version") == 1 && meta.getString("id") == "summoning_hall")
      require(meta.getString("file") == "summoning_hall.png")
      val bytes = context.assets.open("$root/summoning_hall.png").use { it.readBytes() }
      require(bytes.size in 1..512_000)
      require(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } == meta.getString("sha256"))
      val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 }))
      owned = bitmap
      require(bitmap.width == 268 && bitmap.height == 206 && bitmap.isPremultiplied)
      require(bitmap.allocationByteCount <= 1024 * 1024)
      val image = bitmap.asImageBitmap()
      ObliqueSpriteGeometry.bounds(image)
      SanctuaryEnvironmentShadows.silhouette(image)
      retainedBytes = bitmap.allocationByteCount.toLong()
      hall = image
    } catch (error: Exception) {
      owned?.let { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("AuthoredStructureAtlas", "Optional temple unavailable; keeping original building", error)
    } catch (error: OutOfMemoryError) {
      owned?.let { if (!it.isRecycled) it.recycle() }
    }
  }
  fun landmark(name: String, width: Int, height: Int): ImageBitmap? =
    if (name == "summoning_hall" && width == 268 && height == 206) hall else null
}
