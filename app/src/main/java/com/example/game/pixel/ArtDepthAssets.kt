package com.example.game.pixel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.security.MessageDigest
import java.util.IdentityHashMap
import kotlin.math.roundToInt
import org.json.JSONObject

/** A finite optional original material pack. Prepare before rendering; never decode in draw. */
internal object ArtDepthAssets {
  private const val ROOT = "art_depth_v1/materials"
  private const val MAX_RETAINED = 5L * 1024 * 1024
  private val names = listOf("sanctuary_grass", "meadow_grass", "forest_moss", "limestone",
    "cobble", "soil", "sand", "oak", "marble", "forge", "carpet", "leaf_litter",
    "ruin", "volcanic", "star_stone", "rock")
  private var attempted = false
  private var families: Map<String, List<ImageBitmap>> = emptyMap()
  private var macroGrass: ImageBitmap? = null
  private var authoredLists = IdentityHashMap<List<ImageBitmap>, Boolean>()
  internal var decodeCount = 0; private set
  internal var retainedBytes = 0L; private set
  internal val familyCount get() = families.size

  @Synchronized fun preload(context: Context) {
    if (attempted) return
    attempted = true
    val owned = mutableListOf<Bitmap>()
    try {
      val meta = context.assets.open("$ROOT/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
      require(meta.getInt("version") == 1 && meta.getInt("world_tile_units") == 64)
      require(kotlin.math.abs(meta.getDouble("camera_ground_y") - .72) < .00001)
      val entries = meta.getJSONArray("families")
      require(entries.length() == names.size)
      val prepared = linkedMapOf<String, List<ImageBitmap>>()
      var grass: ImageBitmap? = null
      for (i in names.indices) {
        val entry = entries.getJSONObject(i)
        val name = names[i]
        require(entry.getString("id") == name && entry.getString("file") == "$name.png")
        require(entry.getInt("width") == 256 && entry.getInt("height") == 256)
        val bytes = context.assets.open("$ROOT/$name.png").use { it.readBytes() }
        require(bytes.size in 1..512_000)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        require(digest == entry.getString("sha256"))
        val sheet = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
          BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 }))
        decodeCount++
        try {
          require(sheet.width == 256 && sheet.height == 256 && sheet.isPremultiplied)
          val pixels = IntArray(256 * 256)
          sheet.getPixels(pixels, 0, 256, 0, 0, 256, 256)
          require(pixels.all { it ushr 24 == 255 })
          for (p in 0 until 256) {
            require(pixels[p] == pixels[255 * 256 + p])
            require(pixels[p * 256] == pixels[p * 256 + 255])
          }
          prepared[name] = List(16) { cell ->
            Bitmap.createBitmap(sheet, cell % 4 * 64, cell / 4 * 64, 64, 64).also { owned += it }.asImageBitmap()
          }
          if (name == "sanctuary_grass") {
            owned += sheet
            grass = sheet.asImageBitmap()
          }
        } finally {
          if (name != "sanctuary_grass" || grass == null) sheet.recycle()
        }
      }
      val total = owned.sumOf { it.allocationByteCount.toLong() }
      require(total <= MAX_RETAINED)
      val identities = IdentityHashMap<List<ImageBitmap>, Boolean>()
      prepared.values.forEach { identities[it] = true }
      authoredLists = identities
      macroGrass = grass
      retainedBytes = total
      families = prepared.toMap()
    } catch (error: Exception) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("ArtDepthAssets", "Optional material pack unavailable; retaining original materials", error)
    } catch (error: OutOfMemoryError) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("ArtDepthAssets", "Material memory unavailable; retaining original materials")
    }
  }

  fun family(name: String): List<ImageBitmap>? = families[name]

  /** Authored cells are a continuous image, not interchangeable random variants. */
  fun tile(tiles: List<ImageBitmap>, x: Int, y: Int): ImageBitmap {
    val index = if (authoredLists.containsKey(tiles)) Math.floorMod(y, 4) * 4 + Math.floorMod(x, 4)
      else (((x * 73856093) xor (y * 19349663)) and 0x7fffffff) % tiles.size
    return tiles[index]
  }

  /** A 256-world-unit grass patch replaces sixteen ground blits without moving any world geometry. */
  fun DrawScope.drawGrassMacros(startX: Int, endX: Int, startY: Int, endY: Int,
    screenX: (Float) -> Float, screenY: (Float) -> Float, zoom: Float): Boolean {
    val image = macroGrass ?: return false
    if (zoom <= 0f || startX > endX || startY > endY) return true
    val left = Math.floorDiv(startX, 4) * 4
    val top = Math.floorDiv(startY, 4) * 4
    for (ty in top..endY step 4) for (tx in left..endX step 4) {
      val x = screenX(tx * 64f).roundToInt()
      val y = screenY(ty * 64f).roundToInt()
      val right = screenX((tx + 4) * 64f).roundToInt()
      val bottom = screenY((ty + 4) * 64f).roundToInt()
      drawImage(image, dstOffset = IntOffset(x, y),
        dstSize = IntSize((right - x + 1).coerceAtLeast(1), (bottom - y + 1).coerceAtLeast(1)),
        filterQuality = FilterQuality.None)
    }
    return true
  }
}
