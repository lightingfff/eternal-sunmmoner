package com.example.game.pixel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.game.combat.EnemyCombatUnit
import com.example.game.map.EnemyArchetype
import java.security.MessageDigest
import org.json.JSONObject

/** Finite process-lifetime CPU cache. Loading happens before draw, never inside frame selection. */
internal object TowerMonsterAtlas {
  data class Frame(val image: ImageBitmap, val pivotX: Int, val pivotY: Int, val worldScale: Float)
  private class Clip(val indices: IntArray, val times: FloatArray, val loop: Boolean) {
    val duration = times.sum()
    fun index(seconds: Float): Int {
      var t = if (loop) ((seconds % duration) + duration) % duration else seconds.coerceIn(0f, duration)
      var i = 0
      while (i < times.lastIndex && t + .00001f >= times[i]) { t -= times[i]; i++ }
      return indices[i]
    }
  }
  private class Sheet(val right: Array<Frame>, val left: Array<Frame>)
  @Volatile private var sheets: Map<EnemyArchetype, Sheet> = emptyMap()
  private var clips: Map<String, Clip> = emptyMap()
  @Volatile private var attempted = false
  @Volatile internal var decodeCount: Int = 0; private set
  @Volatile internal var retainedBytes: Long = 0; private set
  internal val loadedCount get() = sheets.size

  @Synchronized fun preload(context: Context) {
    if (attempted) return
    val built = linkedMapOf<EnemyArchetype, Sheet>()
    val owned = mutableListOf<Bitmap>()
    try {
      val json = context.assets.open("tower_combat_v1/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
      require(json.getInt("version") == 1)
      val w = json.getJSONArray("frame").getInt(0)
      val h = json.getJSONArray("frame").getInt(1)
      val px = json.getJSONArray("pivot").getInt(0)
      val py = json.getJSONArray("pivot").getInt(1)
      require(w in 32..256 && h in 32..256 && px in 0 until w && py in 0 until h)
      val definitions = json.getJSONObject("clips")
      val preparedClips = linkedMapOf<String, Clip>()
      for (name in listOf("idle", "walk", "contact", "instant_attack", "hurt", "defeat")) {
        val clip = definitions.getJSONObject(name)
        val ids = clip.getJSONArray("frames")
        val times = clip.getJSONArray("seconds")
        require(ids.length() == times.length() && ids.length() in 1..16)
        val indices = IntArray(ids.length()) { ids.getInt(it) }
        val seconds = FloatArray(times.length()) { times.getDouble(it).toFloat() }
        require(indices.all { it in 0..11 } && seconds.all { it.isFinite() && it > 0f })
        preparedClips[name] = Clip(indices, seconds, clip.getBoolean("loop"))
      }
      require(kotlin.math.abs(preparedClips.getValue("contact").duration - .48f) < .0001f)
      require(kotlin.math.abs(definitions.getJSONObject("contact").getDouble("contact_seconds") - .22) < .0001)
      val actors = json.getJSONObject("actors")
      val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
      for (type in listOf(EnemyArchetype.FIELD_GOBLIN_RAIDER, EnemyArchetype.FIELD_GOBLIN_ARCHER,
        EnemyArchetype.DIRE_WOLF, EnemyArchetype.GOBLIN_WAR_CAPTAIN)) {
        val meta = actors.getJSONObject(type.name)
        val file = meta.getString("file")
        require(file.matches(Regex("[a-z_]+\\.png")))
        val bytes = context.assets.open("tower_combat_v1/$file").use { it.readBytes() }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        require(digest == meta.getString("sha256"))
        decodeCount++
        val source = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
          BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 }))
        try {
          require(source.width == w * 12 && source.height == h && source.isPremultiplied)
          val scale = meta.getDouble("world_scale").toFloat()
          require(scale.isFinite() && scale in .5f..2f)
          val right = Array(12) { column ->
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            owned += bitmap
            Canvas(bitmap).drawBitmap(source, android.graphics.Rect(column*w, 0, (column+1)*w, h),
              android.graphics.Rect(0, 0, w, h), paint)
            Frame(bitmap.asImageBitmap(), px, py, scale)
          }
          val left = Array(12) { column ->
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            owned += bitmap
            val canvas = Canvas(bitmap)
            canvas.translate(w.toFloat(), 0f); canvas.scale(-1f, 1f)
            canvas.drawBitmap(source, android.graphics.Rect(column*w, 0, (column+1)*w, h),
              android.graphics.Rect(0, 0, w, h), paint)
            Frame(bitmap.asImageBitmap(), w-px, py, scale)
          }
          built[type] = Sheet(right, left)
        } finally { source.recycle() }
      }
      val total = owned.sumOf { it.allocationByteCount.toLong() }
      require(total <= 8L * 1024 * 1024)
      clips = preparedClips
      retainedBytes = total
      sheets = built.toMap() // Publish only a complete pack after all crops and metadata are valid.
    } catch (error: Exception) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("TowerMonsterAtlas", "Optional Tower enemy pack unavailable; using existing renderer", error)
    } catch (error: OutOfMemoryError) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("TowerMonsterAtlas", "Enemy pack exceeded available memory; using existing renderer")
    } finally { attempted = true }
  }

  fun frame(enemy: EnemyCombatUnit, seconds: Float, charging: Boolean, recovering: Boolean): Frame? {
    val sheet = sheets[enemy.archetype] ?: return null
    val frames = if (enemy.facingRight) sheet.right else sheet.left
    val clipName = when {
      enemy.dead -> "defeat"
      charging -> return frames[6]
      recovering -> return frames[if (enemy.attackAnim > .60f) 7 else 8]
      enemy.contactAttack.active -> "contact"
      enemy.attackAnim > 0f -> "instant_attack"
      enemy.hurtFlash > .35f -> "hurt"
      enemy.isMoving -> "walk"
      else -> "idle"
    }
    val clip = clips.getValue(clipName)
    val elapsed = when (clipName) {
      "defeat" -> enemy.deathAnimProgress.coerceIn(0f, 1f) * clip.duration
      "contact" -> enemy.contactAttack.elapsed
      "instant_attack" -> ((1f-enemy.attackAnim)/2.6f).coerceAtLeast(0f)
      "hurt" -> (1f-enemy.hurtFlash).coerceAtLeast(0f)/3.5f
      else -> seconds
    }
    return frames[clip.index(elapsed)]
  }
}

