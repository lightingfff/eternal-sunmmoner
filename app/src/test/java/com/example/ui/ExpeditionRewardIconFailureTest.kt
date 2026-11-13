package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import com.example.game.combat.FieldRewardKind
import com.example.ui.components.ExpeditionRewardIconCache
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Cosmetic input failures must not terminate the battle or repeatedly retry allocations. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExpeditionRewardIconFailureTest {
  @Test fun missingSecondIconDiscardsThePartialPackAndCachesFailure() {
    val context = RuntimeEnvironment.getApplication() as Context
    val cache = ExpeditionRewardIconCache()
    var reads = 0
    cache.preload { filename ->
      reads++
      if (reads == 2) throw IOException("Injected missing optional icon")
      context.assets.open(filename)
    }
    assertEquals(2, reads)
    assertEmpty(cache)
    cache.preload { error("A failed optional pack must not be retried on every screen") }
    assertEmpty(cache)
  }

  @Test fun corruptPngFallsBackWithoutPublishingOrThrowing() {
    val cache = ExpeditionRewardIconCache()
    cache.preload { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
    assertEmpty(cache)
    cache.preload { error("Corrupt pack should be negatively cached") }
  }

  @Test fun wrongSizePngIsRejectedAsOneAtomicPack() {
    val cache = ExpeditionRewardIconCache()
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val bytes = ByteArrayOutputStream().use { stream ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
      stream.toByteArray()
    }
    bitmap.recycle()
    cache.preload { ByteArrayInputStream(bytes) }
    assertEmpty(cache)
  }

  private fun assertEmpty(cache: ExpeditionRewardIconCache) {
    FieldRewardKind.entries.forEach { assertNull(cache.image(it)) }
    assertEquals(0L, cache.decodedBytes)
    assertEquals(0, cache.decodeCount)
  }
}
