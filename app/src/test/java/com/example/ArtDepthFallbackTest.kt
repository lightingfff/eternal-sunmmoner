package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.game.combat.CombatAoEEffect
import com.example.game.combat.CombatFxStyle
import com.example.game.pixel.AuthoredHeroAtlas
import com.example.game.pixel.TowerCombatFxAtlas
import com.example.game.pixel.TowerCombatVfxRenderer.drawExecutionFx
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.lang.reflect.Field
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Isolated asset packages exercise actual production decoding and negative caching. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtDepthFallbackTest {
  private val saved = mutableListOf<Pair<Field, Any?>>()

  @Before fun isolateProcessLifetimeLoaders() {
    // Robolectric reuses the application classloader across tests. Simulate a fresh
    // process without adding a runtime reset API or mutating the APK assets on disk.
    fun isolate(type: Class<*>, fields: Map<String, Any?>) {
      for ((name, value) in fields) {
        val field = type.getDeclaredField(name).apply { isAccessible = true }
        saved += field to field.get(null)
        field.set(null, value)
      }
    }
    isolate(TowerCombatFxAtlas::class.java, mapOf("frames" to null, "attempted" to false,
      "sourcePack" to null, "decodeCount" to 0, "retainedBytes" to 0L))
    isolate(AuthoredHeroAtlas::class.java, mapOf("heroes" to emptyMap<String, Any>(),
      "attempted" to false, "decodeCount" to 0, "retainedBytes" to 0L))
  }

  @After fun restoreProcessLifetimeLoaders() {
    saved.asReversed().forEach { (field, value) -> field.set(null, value) }
    saved.clear()
  }

  private fun packageContext(valid: List<String>, corrupt: Map<String, ByteArray> = emptyMap()): Context {
    val base = RuntimeEnvironment.getApplication() as Context
    val file = File.createTempFile("degraded-art-", ".apk", base.cacheDir)
    ZipOutputStream(file.outputStream()).use { zip ->
      for (path in valid) {
        zip.putNextEntry(ZipEntry("assets/$path"))
        base.assets.open(path).use { it.copyTo(zip) }
        zip.closeEntry()
      }
      for ((path, bytes) in corrupt) {
        zip.putNextEntry(ZipEntry("assets/$path"))
        zip.write(bytes)
        zip.closeEntry()
      }
    }
    val assets = AssetManager::class.java.getDeclaredConstructor().newInstance()
    val cookie = AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java)
      .invoke(assets, file.absolutePath) as Int
    assertTrue("The degraded test package must actually be installed", cookie > 0)
    return object : ContextWrapper(base) { override fun getAssets() = assets }
  }

  @Test fun corruptPixelFxFallsBackToOriginalPngAndDoesNotRetryDuringFrames() {
    val context = packageContext(listOf("tower_combat_v1/vfx_manifest.json", "tower_combat_v1/vfx.png"),
      mapOf("pixelfx_v1/vfx_manifest.json" to "{broken".toByteArray()))
    TowerCombatFxAtlas.preload(context)
    assertEquals("tower_combat_v1", TowerCombatFxAtlas.sourcePack)
    assertEquals(1, TowerCombatFxAtlas.decodeCount)
    val frame = requireNotNull(TowerCombatFxAtlas.frame(CombatFxStyle.GUARD, .2f))
    repeat(20) {
      TowerCombatFxAtlas.preload(context)
      assertSame(frame.image, TowerCombatFxAtlas.frame(CombatFxStyle.GUARD, .2f)!!.image)
    }
    assertEquals(1, TowerCombatFxAtlas.decodeCount)
  }

  @Test fun missingBothPixelPacksLeavesVectorFallbackAvailableWithoutRetries() {
    val context = packageContext(emptyList())
    repeat(20) { TowerCombatFxAtlas.preload(context) }
    assertNull(TowerCombatFxAtlas.sourcePack)
    assertNull(TowerCombatFxAtlas.frame(CombatFxStyle.GUARD, .2f))
    assertEquals(0, TowerCombatFxAtlas.decodeCount)
    assertEquals(0L, TowerCombatFxAtlas.retainedBytes)
    val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(400f, 300f)) {
      drawExecutionFx(CombatAoEEffect(0f, 0f, 60f, Color.Cyan, elapsed = .12f,
        style = CombatFxStyle.GUARD), 0f, 0f, 1f, foreground = true)
    }
    val pixels = IntArray(400 * 300)
    bitmap.getPixels(pixels, 0, 400, 0, 0, 400, 300)
    assertTrue("The no-pack case must draw the real vector effect", pixels.count { it ushr 24 > 0 } > 100)
    bitmap.recycle()
  }

  @Test fun missingSecondHeroDoesNotPublishPartiallyPreparedFirstHero() {
    val context = packageContext(listOf("character_upgrade_v1/kaelen/metadata.json",
      "character_upgrade_v1/kaelen/kaelen.png", "character_upgrade_v1/kaelen/portrait.png"))
    AuthoredHeroAtlas.preload(context)
    assertEquals(2, AuthoredHeroAtlas.decodeCount)
    assertEquals(0, AuthoredHeroAtlas.loadedCount)
    assertEquals(0L, AuthoredHeroAtlas.retainedBytes)
    assertNull(AuthoredHeroAtlas.portrait("Kaelen"))
    repeat(20) { AuthoredHeroAtlas.preload(context) }
    assertEquals(2, AuthoredHeroAtlas.decodeCount)
  }
}
