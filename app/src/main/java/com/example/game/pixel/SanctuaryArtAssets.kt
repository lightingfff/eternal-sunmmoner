package com.example.game.pixel

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap

/** Immutable for a process: the 4×4 grass overview cache must never retain the wrong profile. */
enum class SanctuaryVisualProfile { LEGACY, P27B }

/** Offline-authored PNGs. Cache lists as well as images; no decoding in warmed-up draw passes. */
object SanctuaryArtAssets {
  // P27B is enabled for this review build, but absent/corrupt candidate art always falls
  // back to the shipped P03/P21 PNG. Never mutate this at draw time or stale 4×4 grass
  // blocks/shadow masks would survive a profile change; use a fresh process for QA.
  val visualProfile = SanctuaryVisualProfile.P27B

  internal var argonEnabled = true // test-only baseline selector; production defaults to V2.
  private var argonLoaded = false
  private val argonImages = mutableMapOf<String, ImageBitmap?>()
  private val argonSheets = mutableMapOf<String, ImageBitmap>()
  private val argonSpecs = mapOf(
    "batch_01_terrain/terrain_forest_grass_v1_64.png" to (64 to 64),
    "batch_01_terrain/terrain_forest_grass_v2_64.png" to (64 to 64),
    "batch_01_terrain/terrain_forest_grass_v3_64.png" to (64 to 64),
    "batch_01_terrain/terrain_mossy_flagstone_v1_64.png" to (64 to 64),
    "batch_01_terrain/terrain_mossy_flagstone_v2_64.png" to (64 to 64),
    "batch_01_terrain/terrain_compacted_earth_64.png" to (64 to 64),
    "batch_01_terrain/terrain_tower_gate_runic_stone_64.png" to (64 to 64),
    "batch_01_terrain/terrain_grass_stone_transition_64.png" to (64 to 64),
    "batch_02_foliage_props/foliage_ancient_pine_96x128.png" to (96 to 128),
    "batch_02_foliage_props/foliage_moss_flower_bush_64x64.png" to (64 to 64),
    "batch_02_foliage_props/foliage_ashen_violet_shrub_64x64.png" to (64 to 64),
    "batch_02_foliage_props/foliage_dorm_blossom_clump_64x48.png" to (64 to 48),
    "batch_02_foliage_props/prop_wall_ivy_drape_64x96.png" to (64 to 96),
    "batch_02_foliage_props/prop_wooden_bronze_wayfinder_64x96.png" to (64 to 96),
    "batch_03_architecture_details/arch_black_slate_roof_moss_96x64.png" to (96 to 64),
    "batch_03_architecture_details/arch_ancient_timber_bronze_door_64x80.png" to (64 to 80),
    "batch_03_architecture_details/arch_summoning_hall_pedestal_trim_96x48.png" to (96 to 48),
    "batch_03_architecture_details/arch_warm_stained_window_frame_48x64.png" to (48 to 64),
    "batch_03_architecture_details/arch_tower_gate_buttress_pillar_64x128.png" to (64 to 128),
    "batch_03_architecture_details/arch_dorm_carved_timber_frieze_96x32.png" to (96 to 32)
  )
  internal fun argon(path: String): ImageBitmap? = if (argonEnabled) argonImages[path] else null
  internal fun argonGround(name: String): List<ImageBitmap>? {
    if (!argonEnabled || !argonLoaded) return null
    return argonFamilies[name]
  }
  private val argonFamilies = mutableMapOf<String, List<ImageBitmap>>()
  internal fun argonSheet(name: String): ImageBitmap? = if (argonEnabled) argonSheets[name] else null
  private fun buildArgonGroundSheets() {
    val names = mapOf(
      "grass" to listOf("terrain_forest_grass_v1_64", "terrain_forest_grass_v2_64", "terrain_forest_grass_v3_64"),
      "soil" to listOf("terrain_compacted_earth_64"),
      "flagstone" to listOf("terrain_mossy_flagstone_v1_64", "terrain_mossy_flagstone_v2_64"),
      "cobble" to listOf("terrain_mossy_flagstone_v1_64", "terrain_mossy_flagstone_v2_64")
    )
    for ((family, ids) in names) {
      val tiles = ids.mapNotNull { argonImages["batch_01_terrain/$it.png"] }
      if (tiles.size != ids.size) continue // family-level fallback, no mixed missing variants.
      argonFamilies[family] = tiles
      val bitmap = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888)
      val canvas = android.graphics.Canvas(bitmap)
      for (y in 0 until 8) for (x in 0 until 8) {
        val hash = ((x * 73856093) xor (y * 19349663)) and Int.MAX_VALUE
        canvas.drawBitmap(tiles[hash % tiles.size].asAndroidBitmap(), x * 64f, y * 64f, null)
      }
      argonSheets[family] = bitmap.asImageBitmap()
    }
  }

  private var context: Context? = null
  private val images = mutableMapOf<String, ImageBitmap>()
  private val families = mutableMapOf<String, List<ImageBitmap>>()
  private val optionalFamilies = mutableMapOf<String, List<ImageBitmap>?>()
  private val groundSheets = mutableMapOf<String, ImageBitmap?>()
  private val mirroredSheets = mutableMapOf<String, ImageBitmap?>()
  internal val cachedImageCount get() = images.size
  /** Decoded PNG cache only; generated actor atlases and GPU copies are not included. Sample at 1 Hz. */
  internal val cachedBitmapBytes: Long get() = images.values.sumOf { it.asAndroidBitmap().allocationByteCount.toLong() }
  fun init(context: Context) {
    this.context = context.applicationContext
    if (!argonLoaded) {
      // Only the A08/A01 whitelist. Decode once at initialization, never in draw getters.
      argonSpecs.forEach { (path, size) ->
        argonImages[path] = assetSafe("sanctuary_argon_v2/$path")
          ?.takeIf { it.width == size.first && it.height == size.second }
      }
      argonLoaded = true
      buildArgonGroundSheets()
    }
  }
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
  /**
   * P21 loader: a missing or corrupt *optional* PNG falls back to the procedural sprite instead of
   * throwing. [asset] uses `decodeAsset(path)!!`, so an absent file would throw before any
   * `?: fallback` Elvis branch could run — this helper closes that gap without changing [asset].
   *
   * A failed read is never cached (one bad file cannot poison the cache); a successful decode is
   * cached exactly like [asset], so steady-state cost stays one blit per facility per frame.
   */
  internal fun assetSafe(path: String): ImageBitmap? {
    val ctx = context ?: return null
    images[path]?.let { return it }
    val decoded = try {
      ctx.assets.open(path).use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inScaled = false })
          ?.asImageBitmap()
      }
    } catch (e: java.io.IOException) {
      null
    } catch (e: IllegalArgumentException) {
      // Bad optional PNG data is not a reason to crash the world renderer.
      null
    }
    if (decoded != null) images[path] = decoded
    return decoded
  }

  /** Negative-cache an incomplete P27B family: no repeated asset opens inside draw loops.
   * The legacy family is still returned in its entirety (8/6 tiles), never a mixed atlas.
   * Restoring a missing file is an APK/process restart, not a hot profile switch.
   */
  internal fun p27bFamily(name: String): List<ImageBitmap>? {
    if (visualProfile != SanctuaryVisualProfile.P27B || context == null) return null
    if (optionalFamilies.containsKey(name)) return optionalFamilies[name]
    val cells = (0 until 16).map { index ->
      assetSafe("sanctuary_p27b/terrain/${name}_$index.png")
        ?.takeIf { it.width == 64 && it.height == 64 }
        ?: run { optionalFamilies[name] = null; return null }
    }
    optionalFamilies[name] = cells
    return cells
  }

  /** P27B-V5 ground: one 512x512 sheet per family, sampled as an 8x8 mosaic.
   *
   * The shipped 4x4 atlas became a translation-invariant lattice: the same block
   * repeated every 256 world px, which is what the plaza z1.0 review saw. A sheet
   * quadruples the period to 512 world px and, because it is a single bitmap, holds
   * *less* memory than 64 separate 64x64 cells (1 MB vs ~1 MB plus 64 objects, and
   * one texture instead of 64). Negative-cached exactly like [p27bFamily]: a missing
   * or mis-sized sheet falls back to the whole P27B 4x4 family, never a half atlas.
   */
  internal fun groundSheet(name: String): ImageBitmap? {
    if (visualProfile != SanctuaryVisualProfile.P27B || context == null) return null
    if (groundSheets.containsKey(name)) return groundSheets[name]
    val sheet = assetSafe("sanctuary_p27b_2/terrain/${name}_mosaic.png")
      ?.takeIf { it.width == 512 && it.height == 512 }
    groundSheets[name] = sheet
    return sheet
  }

  /** Rotated copy of a ground sheet, used to break the sheet's own repeat period.
   *
   * The 8x8 sheet is still periodic: the same 512 world-px block recurs. Rather than
   * paying for a 1024 px sheet (4x the memory, and it needs source art at the correct
   * stone density which does not exist yet), each world tile picks at random between
   * the sheet and its 180-degree rotation. The cell art is identical either way, but the
   * *arrangement* is no longer periodic, so the eye cannot lock onto the lattice.
   *
   * Built once per family by a Matrix copy (never inside a draw pass) and negative
   * cached like every other optional asset.
   */
  internal fun groundSheetMirrored(name: String): ImageBitmap? {
    if (mirroredSheets.containsKey(name)) return mirroredSheets[name]
    val original = groundSheet(name)
    if (original == null) {
      mirroredSheets[name] = null
      return null
    }
    val rotated = try {
      val src = original.asAndroidBitmap()
      val matrix = android.graphics.Matrix().apply { setScale(-1f, -1f) }
      android.graphics.Bitmap
        .createBitmap(src, 0, 0, src.width, src.height, matrix, false)
        .asImageBitmap()
    } catch (e: IllegalArgumentException) {
      null
    } catch (e: OutOfMemoryError) {
      // A cheap-quality device keeps the unmirrored sheet; the map stays correct.
      null
    }
    mirroredSheets[name] = rotated
    return rotated
  }

  internal fun preferredLandmark(name: String, width: Int, height: Int): ImageBitmap? {
    if (visualProfile == SanctuaryVisualProfile.P27B) {
      assetSafe("sanctuary_p27b/structures/$name.png")
        ?.takeIf { it.width == width && it.height == height }?.let { return it }
    }
    return assetSafe("premium_slice/$name.png")
  }

  fun tiles(name: String, count: Int): List<ImageBitmap>? {
    if (context == null) return null
    return families.getOrPut(name) { (0 until count).map { image("${name}_$it")!! } }
  }
}
