package com.example.game.pixel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONObject

/** A bounded, offline atlas for the premium actors that actually ship an authored sheet
 * (Kaelen, the field goblin raider, and Lyra). Animation timing and feet are authored data.
 * Selection allocates nothing after first load. Unknown ids, missing assets or malformed
 * sheets all return null so every call site keeps its procedural fallback instead of
 * reaching a getValue() crash. */
internal class PremiumActorAtlas private constructor(
  private val clips: Map<String, Clip>,
  private val emotions: Map<String, String>,
  val pivotX: Int, val pivotY: Int, val worldPixelScale: Float,
) {
  class Clip(val rows: Array<Array<ImageBitmap>>, val durations: FloatArray, val loop: Boolean) {
    val duration = durations.sum()
    fun frame(row: Int, seconds: Float, loop: Boolean): ImageBitmap {
      var t = if (loop) ((seconds % duration) + duration) % duration else seconds.coerceIn(0f,duration)
      var index = 0
      while (index < durations.lastIndex && t + .00001f >= durations[index]) { t -= durations[index]; index++ }
      return rows[row][index]
    }
  }
  fun frame(right: Boolean, north: Boolean, moving: Boolean, progress: Float,
    skill: Boolean, hurt: Float, rest: Boolean, seconds: Float, defeat: Float = 0f,
    emotion: String = "calm"): ImageBitmap {
    val row = (if (north) 2 else 0) + if (right) 0 else 1
    val key = when {
      defeat > 0f -> "defeat"
      progress > 0f -> if (skill) "skill" else "attack"
      hurt > .35f -> "hit"
      rest -> "rest"
      emotion == "fear" -> emotions[emotion] ?: "idle"
      moving -> "walk"
      else -> emotions[emotion] ?: "idle"
    }
    val clip = clips.getValue(key)
    val time = when (key) {
      "attack", "skill" -> (1f-progress).coerceIn(0f,1f)*clip.duration
      "hit" -> ((1f-hurt)/.65f).coerceIn(0f,1f)*clip.duration
      "defeat" -> defeat.coerceIn(0f,1f)*clip.duration
      else -> seconds
    }
    return clip.frame(row,time,clip.loop)
  }
  companion object {
    /** Only ids with a complete clip_order sheet may be built. */
    private val authoredIds = setOf("kaelen","goblin_raider","lyra")
    private val cache = mutableMapOf<String,PremiumActorAtlas>()
    internal val loadedCount get() = cache.size
    fun get(id: String): PremiumActorAtlas? {
      if (id !in authoredIds) return null
      cache[id]?.let { return it }
      return try { build(id) } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
    }
    private fun build(id: String): PremiumActorAtlas? {
      val text = SanctuaryArtAssets.textAsset("premium_slice/manifest.json") ?: return null
      val meta = JSONObject(text)
      val assets = meta.getJSONObject("assets")
      if (!assets.has(id)) return null
      val source = SanctuaryArtAssets.decodeAsset("premium_slice/${assets.getJSONObject(id).getString("file")}")
        ?.asAndroidBitmap() ?: return null
      val width = meta.getJSONArray("frame").getInt(0)
      val height = meta.getJSONArray("frame").getInt(1)
      val clips = linkedMapOf<String,Clip>()
      var column = 0
      val definitions = meta.getJSONObject("clips")
      val order = meta.getJSONArray("clip_order")
      val loopDefinitions = meta.getJSONArray("loop_clips")
      val loops = HashSet<String>(loopDefinitions.length())
      for (i in 0 until loopDefinitions.length()) loops.add(loopDefinitions.getString(i))
      try {
        // JSON objects do not promise order; the authored list defines atlas columns.
        for (index in 0 until order.length()) {
          val key = order.getString(index)
          val times = definitions.getJSONArray(key)
          val durations = FloatArray(times.length()) { times.getDouble(it).toFloat() }
          require(durations.isNotEmpty() && durations.all { it > 0f })
          val rows = Array(4) { row -> Array(durations.size) { frame ->
            Bitmap.createBitmap(source,(column+frame)*width,row*height,width,height).asImageBitmap()
          } }
          clips[key] = Clip(rows,durations,key in loops)
          column += durations.size
        }
        require(source.width == column * width && source.height == 4 * height)
      } finally {
        // decodeAsset is uncached. Retain only frame crops, never a second full sheet.
        if (!source.isRecycled) source.recycle()
      }
      val emotionDefinitions = meta.getJSONObject("emotion_clips")
      val emotions = HashMap<String,String>()
      for (key in emotionDefinitions.keys()) {
        val clip = emotionDefinitions.getString(key)
        require(clips.containsKey(clip))
        emotions[key] = clip
      }
      val pivot = meta.getJSONArray("pivot")
      return PremiumActorAtlas(clips,emotions,pivot.getInt(0),pivot.getInt(1),meta.getDouble("world_pixel_scale").toFloat())
        .also { cache[id] = it }
    }
  }
}
