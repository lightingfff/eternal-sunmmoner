package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.game.pixel.CompletionClassAtlas
import com.example.game.pixel.CompletionEnemyAtlas
import com.example.game.pixel.AuthoredHeroAtlas
import com.example.game.pixel.PremiumPortraits
import com.example.game.pixel.SanctuaryArtAssets
import com.example.data.model.HeroClassType
import com.example.ui.screens.TowerGateMapArt
import java.io.File
import java.lang.reflect.Field
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtCompletionFallbackTest {
  private val saved = mutableListOf<Pair<Field, Any?>>()
  @Before fun isolateBankPublication() {
    for ((type, fieldName) in listOf(CompletionClassAtlas::class.java to "bodies", CompletionEnemyAtlas::class.java to "frames")) {
      for ((name, value) in mapOf(fieldName to emptyMap<Any, Any>(), "attempted" to false, "retainedBytes" to 0L, "decodeCount" to 0)) {
        val field = type.getDeclaredField(name).apply { isAccessible = true }
        saved += field to field.get(null)
        field.set(null, value)
      }
    }
  }
  @After fun restoreBanks() { saved.asReversed().forEach { (field, value) -> field.set(null, value) } }

  private fun packageContext(paths: List<String>): Context {
    val base = RuntimeEnvironment.getApplication() as Context
    val file = File.createTempFile("partial-completion-", ".apk", base.cacheDir)
    ZipOutputStream(file.outputStream()).use { zip -> paths.forEach { path ->
      zip.putNextEntry(ZipEntry("assets/$path")); base.assets.open(path).use { it.copyTo(zip) }; zip.closeEntry()
    } }
    val assets = AssetManager::class.java.getDeclaredConstructor().newInstance()
    val cookie = AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java).invoke(assets, file.absolutePath) as Int
    assertTrue(cookie > 0)
    return object : ContextWrapper(base) { override fun getAssets() = assets }
  }
  @Test fun partialClassBankPublishesNoActorsOrPortraitsAndDoesNotRetry() {
    val root = "art_completion_v1/classes/assassin"
    val context = packageContext(listOf("$root/metadata.json", "$root/assassin.png", "$root/portrait.png"))
    repeat(10) { CompletionClassAtlas.preload(context) }
    assertEquals(2, CompletionClassAtlas.decodeCount)
    assertEquals(0, CompletionClassAtlas.loadedCount)
    assertEquals(0L, CompletionClassAtlas.retainedBytes)
  }
  @Test fun partialEnemyBankPublishesNoActorsAndDoesNotRetry() {
    val root = "art_completion_v1/enemies/forest_wraith"
    val context = packageContext(listOf("$root/metadata.json", "$root/atlas.png"))
    repeat(10) { CompletionEnemyAtlas.preload(context) }
    assertEquals(1, CompletionEnemyAtlas.decodeCount)
    assertEquals(0, CompletionEnemyAtlas.loadedCount)
    assertEquals(0L, CompletionEnemyAtlas.retainedBytes)
  }
  @Test fun partialNewGatePackFallsBackAsAWholeWithoutMixingSceneVersions() {
    val legacy = listOf("eternal_gate_court", "three_gate_nexus", "engraved_door_leaves").map { "tower_gate_map/$it.png" }
    val context = packageContext(legacy + listOf("art_completion_v1/gates/manifest.json", "art_completion_v1/gates/eternal_gate_court.png"))
    val art = TowerGateMapArt(context.assets)
    assertEquals("tower_gate_map", art.sourcePack)
    assertEquals(4_285_440L, art.retainedBytes)
    val original = RuntimeEnvironment.getApplication().assets.open(legacy.first()).use { BitmapFactory.decodeStream(it)!! }
    assertTrue(art.court.asAndroidBitmap().sameAs(original))
    original.recycle()
  }
  @Test fun missingNamedBankNeverUsesALegacyPortraitForTheWrongClass() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
    for ((name, value) in mapOf("heroes" to emptyMap<Any, Any>(), "attempted" to true)) {
      val field = AuthoredHeroAtlas::class.java.getDeclaredField(name).apply { isAccessible = true }
      saved += field to field.get(null)
      field.set(null, value)
    }
    val field = PremiumPortraits::class.java.getDeclaredField("cache").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val cache = field.get(null) as MutableMap<Any?, Any?>
    val old = cache.toMap()
    cache.clear()
    try {
      // The original legacy manifest contains Lyra even when the new optional named bank fails.
      assertNull(PremiumPortraits.get("Lyra", HeroClassType.KNIGHT))
      assertNull(PremiumPortraits.get("LYRA", HeroClassType.MAGE))
      assertNotNull(PremiumPortraits.get("Lyra", HeroClassType.ARCHER))
    } finally { cache.clear(); cache.putAll(old) }
  }
}
