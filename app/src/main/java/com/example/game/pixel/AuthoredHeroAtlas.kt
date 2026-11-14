package com.example.game.pixel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.model.HeroClassType
import java.security.MessageDigest
import kotlin.math.floor
import org.json.JSONObject

/** Original named heroes, shared by Lobby, Tower and portraits. Left is a draw transform, not another bank. */
internal object AuthoredHeroAtlas {
  data class Frame(val image: ImageBitmap, val pivotX: Int, val pivotY: Int, val worldScale: Float, val index: Int)
  private data class Hero(val type: HeroClassType, val faces: Array<Array<Frame>>, val portrait: ImageBitmap)
  private val roster = listOf("kaelen" to HeroClassType.KNIGHT, "lyra" to HeroClassType.ARCHER, "vesper" to HeroClassType.MAGE)
  @Volatile private var heroes: Map<String, Hero> = emptyMap()
  private var attempted = false
  internal var decodeCount = 0; private set
  internal var retainedBytes = 0L; private set
  internal val loadedCount get() = heroes.size
  private const val MAX_BYTES = 4L * 1024 * 1024

  private fun decode(context: Context, path: String, digest: String): Bitmap {
    val bytes = context.assets.open(path).use { it.readBytes() }
    require(bytes.size in 1..2_000_000)
    require(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } == digest)
    val result = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
      BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 }))
    decodeCount++
    require(result.isPremultiplied)
    return result
  }

  @Synchronized fun preload(context: Context) {
    if (attempted) return
    attempted = true
    val owned = mutableListOf<Bitmap>()
    try {
      val prepared = linkedMapOf<String, Hero>()
      for ((id, type) in roster) {
        val root = "character_upgrade_v1/$id"
        val meta = context.assets.open("$root/metadata.json").bufferedReader().use { JSONObject(it.readText()) }
        require(meta.getInt("columns") == 12 && meta.getString("hero") == id.replaceFirstChar { it.uppercase() })
        val rows = meta.getJSONArray("rows")
        require(rows.length() == 2 && rows.getString(0) == "front_right" && rows.getString(1) == "back_right")
        val width = meta.getJSONArray("frame").getInt(0)
        val height = meta.getJSONArray("frame").getInt(1)
        val px = meta.getJSONArray("pivot").getInt(0)
        val py = meta.getJSONArray("pivot").getInt(1)
        val scale = meta.getDouble("world_scale").toFloat()
        require(width in listOf(96, 112, 128) && height == 112 && px == width / 2 && py == 104)
        require(scale.isFinite() && scale in 1f..1.25f)
        require(meta.getString("atlas_file") == "$id.png" && meta.getString("portrait_file") == "portrait.png")
        val source = decode(context, "$root/$id.png", meta.getString("atlas_sha256"))
        val faces = try {
          require(source.width == width * 12 && source.height == height * 2)
          Array(2) { row -> Array(12) { column ->
            val bitmap = Bitmap.createBitmap(source, column * width, row * height, width, height)
            owned += bitmap
            Frame(bitmap.asImageBitmap(), px, py, scale, row * 12 + column)
          } }
        } finally { source.recycle() }
        val portrait = decode(context, "$root/portrait.png", meta.getString("portrait_sha256"))
        owned += portrait
        require(portrait.width == 128 && portrait.height == 160)
        prepared[id.replaceFirstChar { it.uppercase() }] = Hero(type, faces, portrait.asImageBitmap())
      }
      val total = owned.sumOf { it.allocationByteCount.toLong() }
      require(total <= MAX_BYTES)
      // Warm geometry on the same serialized initialization path as asset loading.
      // No getPixels/path construction or bitmap synthesis is needed in a warmed draw.
      prepared.values.forEach { hero -> hero.faces.forEach { row -> row.forEach { frame ->
        ObliqueSpriteGeometry.bounds(frame.image)
        SanctuaryEnvironmentShadows.silhouette(frame.image)
      } } }
      retainedBytes = total
      heroes = prepared.toMap()
    } catch (error: Exception) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("AuthoredHeroAtlas", "Optional hero pack unavailable; retaining class sprites", error)
    } catch (error: OutOfMemoryError) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("AuthoredHeroAtlas", "Hero memory unavailable; retaining class sprites")
    }
  }

  fun portrait(name: String?, type: HeroClassType? = null): ImageBitmap? =
    heroes[name]?.takeIf { type == null || it.type == type }?.portrait

  /** Identity is independent of optional bitmap availability. */
  fun knownClass(name: String?): HeroClassType? = roster.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second

  fun frame(name: String?, type: HeroClassType, moving: Boolean, attackProgress: Float,
    hurtFlash: Float, resting: Boolean, hpFraction: Float, timeSec: Float,
    back: Boolean, skill: Boolean, contactTimeline: Boolean = name == "Kaelen"): Frame? {
    val hero = heroes[name]?.takeIf { it.type == type } ?: return null
    val index = when {
      hpFraction <= 0f -> 11
      hurtFlash > .35f -> 9
      resting -> 10
      attackProgress > .001f -> {
        val elapsed = (1f - attackProgress.coerceIn(0f, 1f))
        if (contactTimeline) {
          val duration = if (skill) .72f else .48f
          val contact = if (skill) .30f else .22f
          when {
            elapsed * duration + .00001f < contact -> 6
            elapsed * duration < contact + .10f -> 7
            else -> 8
          }
        } else {
          // Existing ranged attacks spawn/damage immediately. Start at release,
          // rather than adding a visual wind-up after the projectile has already left.
          if (elapsed / 2.8f < .10f) 7 else 8
        }
      }
      moving -> 2 + Math.floorMod(floor(timeSec * 9f).toInt(), 4)
      else -> Math.floorMod(floor(timeSec * 2.5f).toInt(), 2)
    }
    return hero.faces[if (back) 1 else 0][index]
  }
}
