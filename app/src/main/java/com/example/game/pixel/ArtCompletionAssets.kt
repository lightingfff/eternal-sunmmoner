package com.example.game.pixel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.model.HeroClassType
import com.example.game.combat.EnemyCombatUnit
import com.example.game.combat.ProjectileVisual
import com.example.game.map.EnemyArchetype
import com.example.game.map.PropKind
import java.security.MessageDigest
import kotlin.math.floor
import org.json.JSONObject

private fun completionPng(context: Context, root: String, file: String, sha: String,
  owned: MutableList<Bitmap>): Bitmap {
  require(file.matches(Regex("[a-z_]+\\.png")))
  val bytes = context.assets.open("$root/$file").use { it.readBytes() }
  require(bytes.size in 1..4_000_000)
  require(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } == sha)
  return requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
    BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 })).also {
    owned += it
    require(!it.hasAlpha() || it.isPremultiplied)
  }
}

/** Three additional classes. Existing named bodies are reused by reference for the other classes. */
internal object CompletionClassAtlas {
  private data class Body(val faces: Array<Array<AuthoredHeroAtlas.Frame>>, val portrait: ImageBitmap)
  @Volatile private var bodies: Map<HeroClassType, Body> = emptyMap()
  private var attempted = false
  internal var retainedBytes = 0L; private set
  internal var decodeCount = 0; private set
  internal val loadedCount get() = bodies.size
  private val names = mapOf(HeroClassType.KNIGHT to "Kaelen", HeroClassType.ARCHER to "Lyra", HeroClassType.MAGE to "Vesper")

  @Synchronized fun preload(context: Context) {
    if (attempted) return
    attempted = true
    val owned = mutableListOf<Bitmap>()
    try {
      val prepared = linkedMapOf<HeroClassType, Body>()
      for (type in listOf(HeroClassType.ASSASSIN, HeroClassType.PRIEST, HeroClassType.BERSERKER)) {
        val id = type.name.lowercase()
        val root = "art_completion_v1/classes/$id"
        val meta = context.assets.open("$root/metadata.json").bufferedReader().use { JSONObject(it.readText()) }
        require(meta.getString("class") == type.name && meta.getInt("columns") == 12)
        require(meta.getJSONArray("frame").let { it.getInt(0) == 112 && it.getInt(1) == 112 })
        require(meta.getJSONArray("pivot").let { it.getInt(0) == 56 && it.getInt(1) == 104 })
        require(meta.getJSONArray("rows").let { it.length() == 2 && it.getString(0) == "front_right" && it.getString(1) == "back_right" })
        val scale = meta.getDouble("world_scale").toFloat()
        require(scale.isFinite() && scale in 1f..1.25f)
        val source = completionPng(context, root, "$id.png", meta.getString("atlas_sha256"), owned)
        decodeCount++
        val faces = try {
          require(source.width == 112 * 12 && source.height == 224)
          Array(2) { row -> Array(12) { column ->
            val crop = Bitmap.createBitmap(source, column * 112, row * 112, 112, 112)
            owned += crop
            AuthoredHeroAtlas.Frame(crop.asImageBitmap(), 56, 104, scale, row * 12 + column)
          } }
        } finally { source.recycle(); owned.remove(source) }
        val portrait = completionPng(context, root, "portrait.png", meta.getString("portrait_sha256"), owned)
        decodeCount++
        require(portrait.width == 128 && portrait.height == 160)
        prepared[type] = Body(faces, portrait.asImageBitmap())
      }
      val total = owned.sumOf { it.allocationByteCount.toLong() }
      require(total <= 4L * 1024 * 1024)
      prepared.values.forEach { body -> body.faces.forEach { row -> row.forEach { frame ->
        ObliqueSpriteGeometry.bounds(frame.image)
        SanctuaryEnvironmentShadows.silhouette(frame.image)
      } } }
      retainedBytes = total
      bodies = prepared.toMap()
    } catch (error: Exception) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("CompletionClassAtlas", "Optional classes unavailable; retaining existing class sprites", error)
    } catch (error: OutOfMemoryError) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("CompletionClassAtlas", "Class memory unavailable; retaining existing class sprites")
    }
  }

  fun portrait(type: HeroClassType?): ImageBitmap? = bodies[type]?.portrait ?: names[type]?.let { AuthoredHeroAtlas.portrait(it) }

  fun frame(type: HeroClassType, moving: Boolean, progress: Float, hurt: Float, resting: Boolean,
    hp: Float, seconds: Float, back: Boolean, skill: Boolean): AuthoredHeroAtlas.Frame? {
    names[type]?.let { canonical ->
      return AuthoredHeroAtlas.frame(canonical, type, moving, progress, hurt, resting, hp,
        seconds, back, skill, contactTimeline = false)
    }
    val body = bodies[type] ?: return null
    val index = when {
      hp <= 0f -> 11
      hurt > .35f -> 9
      resting -> 10
      progress > .001f -> if ((1f - progress.coerceIn(0f, 1f)) / 2.8f < .10f) 7 else 8
      moving -> 2 + Math.floorMod(floor(seconds * 9f).toInt(), 4)
      else -> Math.floorMod(floor(seconds * 2.5f).toInt(), 2)
    }
    return body.faces[if (back) 1 else 0][index]
  }
}

/** Independent <=4MiB one-facing bank; no geometry-cache mutation on Tower's IO preload. */
internal object CompletionEnemyAtlas {
  @Volatile private var frames: Map<EnemyArchetype, Array<TowerMonsterAtlas.Frame>> = emptyMap()
  private var attempted = false
  internal var retainedBytes = 0L; private set
  internal var decodeCount = 0; private set
  internal val loadedCount get() = frames.size
  @Synchronized fun preload(context: Context) {
    if (attempted) return
    attempted = true
    val owned = mutableListOf<Bitmap>()
    try {
      val prepared = linkedMapOf<EnemyArchetype, Array<TowerMonsterAtlas.Frame>>()
      for (type in listOf(EnemyArchetype.FOREST_WRAITH, EnemyArchetype.ELDER_TREANT,
        EnemyArchetype.RUIN_SENTINEL, EnemyArchetype.ABYSSAL_EXECUTIONER)) {
        val root = "art_completion_v1/enemies/${type.name.lowercase()}"
        val meta = context.assets.open("$root/metadata.json").bufferedReader().use { JSONObject(it.readText()) }
        require(meta.getInt("version") == 1 && meta.getString("type") == type.name && meta.getInt("columns") == 12)
        require(meta.getJSONArray("frame").let { it.getInt(0) == 128 && it.getInt(1) == 144 })
        require(meta.getJSONArray("pivot").let { it.getInt(0) == 64 && it.getInt(1) == 134 })
        val scale = meta.getDouble("world_scale").toFloat()
        require(scale.isFinite() && scale in .5f..1.25f)
        val source = completionPng(context, root, "atlas.png", meta.getString("atlas_sha256"), owned)
        decodeCount++
        try {
          require(source.width == 1536 && source.height == 144)
          val scanline = IntArray(source.width)
          val top = (0 until source.height).firstOrNull { y ->
            source.getPixels(scanline, 0, source.width, 0, y, source.width, 1)
            scanline.any { it ushr 24 >= 128 }
          } ?: source.height - 1
          val barHeight = ((134 - top) * scale + 10f).coerceAtLeast(if (type.isBoss) 128f else 86f)
          prepared[type] = Array(12) { column ->
            val bitmap = Bitmap.createBitmap(source, column * 128, 0, 128, 144)
            owned += bitmap
            TowerMonsterAtlas.Frame(bitmap.asImageBitmap(), 64, 134, scale, barHeight)
          }
        } finally { source.recycle(); owned.remove(source) }
      }
      val total = owned.sumOf { it.allocationByteCount.toLong() }
      require(total <= 4L * 1024 * 1024)
      retainedBytes = total
      frames = prepared.toMap()
    } catch (error: Exception) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("CompletionEnemyAtlas", "Optional enemy bank unavailable; retaining existing monsters", error)
    } catch (error: OutOfMemoryError) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("CompletionEnemyAtlas", "Enemy memory unavailable; retaining existing monsters")
    }
  }
  fun frame(enemy: EnemyCombatUnit, seconds: Float): TowerMonsterAtlas.Frame? {
    val bank = frames[enemy.archetype] ?: return null
    val index = when {
      enemy.dead -> if (enemy.deathAnimProgress < .6f) 10 else 11
      enemy.attackAnim > .001f -> if ((1f - enemy.attackAnim.coerceIn(0f, 1f)) / 2.6f < .10f) 7 else 8
      enemy.hurtFlash > .35f -> 9
      enemy.isMoving -> 2 + Math.floorMod(floor(seconds * if (enemy.archetype.isBoss) 6f else 10f).toInt(), 4)
      else -> Math.floorMod(floor(seconds * 2.5f).toInt(), 2)
    }
    return bank[index]
  }
}

/** Static authored props share exact images with shadow lookup and preserve original anchor geometry. */
internal object CompletionPropAtlas {
  @Volatile private var images: Map<String, ImageBitmap> = emptyMap()
  private var attempted = false
  internal var retainedBytes = 0L; private set
  internal var decodeCount = 0; private set
  @Synchronized fun preload(context: Context) {
    if (attempted) return
    attempted = true
    val owned = mutableListOf<Bitmap>()
    try {
      val root = "art_completion_v1/props"
      val meta = context.assets.open("$root/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
      require(meta.getInt("version") == 1)
      val entries = meta.getJSONArray("props")
      require(entries.length() == 3)
      val sizes = mapOf("royal_statue" to (52 to 88), "crimson_crystal" to (48 to 64), "tattered_banner" to (80 to 80))
      val prepared = linkedMapOf<String, ImageBitmap>()
      for (i in 0 until entries.length()) {
        val entry = entries.getJSONObject(i)
        val name = entry.getString("id")
        val size = requireNotNull(sizes[name])
        require(!prepared.containsKey(name) && entry.getString("file") == "$name.png")
        val bitmap = completionPng(context, root, "$name.png", entry.getString("sha256"), owned)
        decodeCount++
        require(bitmap.width == size.first && bitmap.height == size.second)
        val image = bitmap.asImageBitmap()
        ObliqueSpriteGeometry.bounds(image)
        SanctuaryEnvironmentShadows.silhouette(image)
        prepared[name] = image
      }
      val total = owned.sumOf { it.allocationByteCount.toLong() }
      require(total <= 1024L * 1024)
      retainedBytes = total
      images = prepared.toMap()
    } catch (error: Exception) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("CompletionPropAtlas", "Optional props unavailable; keeping old prop rendering", error)
    } catch (error: OutOfMemoryError) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("CompletionPropAtlas", "Prop memory unavailable; keeping old prop rendering")
    }
  }
  fun image(name: String): ImageBitmap? = images[name]
  fun isAuthored(image: ImageBitmap): Boolean = images.values.any { it === image }
}

/** Twenty original flight glyphs. Simulation still owns projectile position, collision and lifetime. */
internal object CompletionProjectileAtlas {
  data class Frame(val image: ImageBitmap, val pivotX: Int = 40, val pivotY: Int = 16)
  @Volatile private var frames: Array<Array<Frame>>? = null
  private var attempted = false
  internal var retainedBytes = 0L; private set
  internal var decodeCount = 0; private set
  @Synchronized fun preload(context: Context) {
    if (attempted) return
    attempted = true
    val owned = mutableListOf<Bitmap>()
    try {
      val root = "art_completion_v1/projectiles"
      val meta = context.assets.open("$root/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
      require(meta.getInt("version") == 1 && meta.getString("file") == "projectiles.png" && meta.getInt("columns") == 4)
      require(meta.getJSONArray("frame").let { it.getInt(0) == 48 && it.getInt(1) == 32 })
      require(meta.getJSONArray("pivot").let { it.getInt(0) == 40 && it.getInt(1) == 16 })
      val names = ProjectileVisual.entries.map { it.name }
      require(meta.getJSONArray("rows").let { rows -> rows.length() == names.size && names.indices.all { rows.getString(it) == names[it] } })
      val source = completionPng(context, root, "projectiles.png", meta.getString("sha256"), owned)
      decodeCount++
      val prepared = try {
        require(source.width == 192 && source.height == 160)
        Array(5) { row -> Array(4) { column ->
          val bitmap = Bitmap.createBitmap(source, column * 48, row * 32, 48, 32)
          owned += bitmap
          Frame(bitmap.asImageBitmap())
        } }
      } finally { source.recycle(); owned.remove(source) }
      val total = owned.sumOf { it.allocationByteCount.toLong() }
      require(total <= 128L * 1024)
      require(total + TowerMonsterAtlas.retainedBytes + TowerCombatFxAtlas.retainedBytes <= 8L * 1024 * 1024)
      retainedBytes = total
      frames = prepared
    } catch (error: Exception) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("CompletionProjectileAtlas", "Optional flight art unavailable; retaining vector projectiles", error)
    } catch (error: OutOfMemoryError) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("CompletionProjectileAtlas", "Flight art memory unavailable; retaining vector projectiles")
    }
  }
  fun frame(visual: ProjectileVisual, remainingLife: Float): Frame? =
    frames?.get(visual.ordinal)?.get(Math.floorMod(floor(-remainingLife * 12f).toInt(), 4))
}

/** Original small furnishings: exact legacy canvas where applicable, one shared visible/shadow pivot. */
internal object CompletionInteriorAtlas {
  data class Entry(val image: ImageBitmap, val pivotX: Int, val pivotY: Int, val pixelScale: Float)
  private data class Definition(val width: Int, val height: Int, val x: Int, val y: Int, val scale: Float)
  private val definitions = linkedMapOf(
    "forge_anvil" to Definition(96, 80, 48, 72, 1f), "armor_stand" to Definition(64, 96, 32, 88, 1f),
    "personal_chest" to Definition(64, 48, 32, 40, 1f), "care_table" to Definition(112, 80, 56, 72, 1f),
    "long_table" to Definition(108, 68, 54, 62, 2f), "bookshelf" to Definition(56, 64, 28, 58, 2f),
    "tavern_counter" to Definition(160, 80, 80, 74, 2f), "recovery_bed" to Definition(96, 72, 48, 64, 1f))
  private val kindIds = mapOf(PropKind.FORGE_ANVIL_STATION to "forge_anvil", PropKind.ARMOR_STAND_34 to "armor_stand",
    PropKind.DORM_PERSONAL_CHEST to "personal_chest", PropKind.INFIRMARY_CARE_TABLE to "care_table",
    PropKind.TAVERN_LONG_TABLE to "long_table", PropKind.BANQUET_TABLE to "long_table",
    PropKind.ARCANE_BOOKSHELF_34 to "bookshelf", PropKind.TAVERN_COUNTER_BAR to "tavern_counter", PropKind.RECOVERY_BED to "recovery_bed")
  @Volatile private var images: Map<String, Entry> = emptyMap()
  private var attempted = false
  internal var retainedBytes = 0L; private set
  internal var decodeCount = 0; private set
  internal val loadedCount get() = images.size
  @Synchronized fun preload(context: Context) {
    if (attempted) return
    attempted = true
    val owned = mutableListOf<Bitmap>()
    try {
      val root = "art_completion_v1/interiors"
      val meta = context.assets.open("$root/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
      require(meta.getInt("version") == 1)
      val entries = meta.getJSONArray("props")
      require(entries.length() == definitions.size)
      val prepared = linkedMapOf<String, Entry>()
      for (i in 0 until entries.length()) {
        val entry = entries.getJSONObject(i)
        val name = entry.getString("id")
        val definition = requireNotNull(definitions[name])
        require(!prepared.containsKey(name) && entry.getString("file") == "$name.png")
        require(entry.getJSONArray("pivot").let { it.getInt(0) == definition.x && it.getInt(1) == definition.y })
        val bitmap = completionPng(context, root, "$name.png", entry.getString("sha256"), owned)
        decodeCount++
        require(bitmap.width == definition.width && bitmap.height == definition.height)
        val image = bitmap.asImageBitmap()
        ObliqueSpriteGeometry.bounds(image)
        SanctuaryEnvironmentShadows.silhouette(image)
        prepared[name] = Entry(image, definition.x, definition.y, definition.scale)
      }
      val total = owned.sumOf { it.allocationByteCount.toLong() }
      require(total <= 1024L * 1024)
      retainedBytes = total
      images = prepared.toMap()
    } catch (error: Exception) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("CompletionInteriorAtlas", "Optional furniture unavailable; keeping original room props", error)
    } catch (error: OutOfMemoryError) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("CompletionInteriorAtlas", "Furniture memory unavailable; keeping original room props")
    }
  }
  fun entry(kind: PropKind): Entry? = images[kindIds[kind]]
  fun image(name: String): ImageBitmap? = images[name]?.image
  fun isAuthored(image: ImageBitmap): Boolean = images.values.any { it.image === image }
}
