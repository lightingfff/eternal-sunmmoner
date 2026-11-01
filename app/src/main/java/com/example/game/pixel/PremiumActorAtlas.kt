package com.example.game.pixel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONObject

/** A bounded, offline atlas for exactly Kaelen and the existing field goblin raider.
 * Animation timing and feet are authored data. Selection allocates nothing after first load. */
internal class PremiumActorAtlas private constructor(
  private val clips: Map<String, Clip>,
  val pivotX: Int, val pivotY: Int, val worldPixelScale: Float,
) {
  class Clip(val rows: Array<Array<ImageBitmap>>, val durations: FloatArray) {
    val duration = durations.sum()
    fun frame(row: Int, seconds: Float, loop: Boolean): ImageBitmap {
      var t = if (loop) ((seconds % duration) + duration) % duration else seconds.coerceIn(0f,duration)
      var index = 0
      while (index < durations.lastIndex && t + .00001f >= durations[index]) { t -= durations[index]; index++ }
      return rows[row][index]
    }
  }
  fun frame(right: Boolean, north: Boolean, moving: Boolean, progress: Float,
    skill: Boolean, hurt: Float, rest: Boolean, seconds: Float, defeat: Float = 0f): ImageBitmap {
    val row = (if (north) 2 else 0) + if (right) 0 else 1
    val key = when {
      defeat > 0f -> "defeat"
      progress > 0f -> if (skill) "skill" else "attack"
      hurt > .35f -> "hit"
      rest -> "rest"
      moving -> "walk"
      else -> "idle"
    }
    val clip = clips.getValue(key)
    val time = when (key) {
      "attack", "skill" -> (1f-progress).coerceIn(0f,1f)*clip.duration
      "hit" -> (1f-hurt).coerceIn(0f,1f)*clip.duration
      "defeat" -> defeat.coerceIn(0f,1f)*clip.duration
      else -> seconds
    }
    return clip.frame(row,time,key == "idle" || key == "walk")
  }
  companion object {
    private val cache = mutableMapOf<String,PremiumActorAtlas>()
    internal val loadedCount get() = cache.size
    fun get(id: String): PremiumActorAtlas? {
      cache[id]?.let { return it }
      val text = SanctuaryArtAssets.textAsset("premium_slice/manifest.json") ?: return null
      val meta = JSONObject(text)
      val source = SanctuaryArtAssets.decodeAsset("premium_slice/$id.png")!!.asAndroidBitmap()
      val width = meta.getJSONArray("frame").getInt(0)
      val height = meta.getJSONArray("frame").getInt(1)
      val clips = linkedMapOf<String,Clip>()
      var column = 0
      val definitions = meta.getJSONObject("clips")
      // Explicit order is important: JSONObject does not promise iteration order.
      for (key in arrayOf("idle","walk","attack","skill","hit","rest","defeat")) {
        val times = definitions.getJSONArray(key)
        val durations = FloatArray(times.length()) { times.getDouble(it).toFloat() }
        val rows = Array(4) { row -> Array(durations.size) { frame ->
          Bitmap.createBitmap(source,(column+frame)*width,row*height,width,height).asImageBitmap()
        } }
        clips[key] = Clip(rows,durations)
        column += durations.size
      }
      val pivot = meta.getJSONArray("pivot")
      return PremiumActorAtlas(clips,pivot.getInt(0),pivot.getInt(1),meta.getDouble("world_pixel_scale").toFloat())
        .also { cache[id] = it }
    }
  }
}
