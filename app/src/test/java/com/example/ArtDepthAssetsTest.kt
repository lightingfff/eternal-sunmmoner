package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.HeroClassType
import com.example.game.combat.ContactAttackTimeline
import com.example.game.combat.CombatFxStyle
import com.example.game.pixel.ArtDepthAssets
import com.example.game.pixel.AuthoredHeroAtlas
import com.example.game.pixel.AuthoredStructureAtlas
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.TowerCombatFxAtlas
import com.example.game.pixel.TowerMonsterAtlas
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Production asset paths and selectors. Native host renders are arranged specimens, not phone/playthrough evidence. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtDepthAssetsTest {
  private fun preload() = SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
  private val roster = listOf("Kaelen" to HeroClassType.KNIGHT,
    "Lyra" to HeroClassType.ARCHER, "Vesper" to HeroClassType.MAGE)

  private fun frame(name: String = "Kaelen", type: HeroClassType = HeroClassType.KNIGHT,
    progress: Float = 0f, back: Boolean = false, skill: Boolean = false,
    moving: Boolean = false, time: Float = 0f, hurt: Float = 0f,
    resting: Boolean = false, hp: Float = 1f) = AuthoredHeroAtlas.frame(
    name, type, moving, progress, hurt, resting, hp, time, back, skill)

  @Test fun realPacksLoadWithinBudgetsAndStayWarm() {
    preload()
    assertEquals(16, ArtDepthAssets.familyCount)
    assertEquals(16, ArtDepthAssets.decodeCount)
    assertEquals(4_456_448L, ArtDepthAssets.retainedBytes)
    assertEquals(3, AuthoredHeroAtlas.loadedCount)
    assertEquals(6, AuthoredHeroAtlas.decodeCount)
    assertEquals(3_858_432L, AuthoredHeroAtlas.retainedBytes)
    val hall = requireNotNull(AuthoredStructureAtlas.landmark("summoning_hall", 268, 206))
    assertEquals(268, hall.width)
    assertEquals(206, hall.height)
    assertEquals(220_832L, AuthoredStructureAtlas.retainedBytes)
    assertNull(AuthoredStructureAtlas.landmark("summoning_hall", 200, 200))
    TowerMonsterAtlas.preload(RuntimeEnvironment.getApplication())
    TowerCombatFxAtlas.preload(RuntimeEnvironment.getApplication())
    assertEquals("pixelfx_v1", TowerCombatFxAtlas.sourcePack)
    assertEquals(1, TowerCombatFxAtlas.decodeCount)
    assertEquals(1_179_648L, TowerCombatFxAtlas.retainedBytes)
    assertTrue(TowerMonsterAtlas.retainedBytes + TowerCombatFxAtlas.retainedBytes <= 8L * 1024 * 1024)
    val initial = requireNotNull(frame())
    repeat(200) {
      preload()
      TowerCombatFxAtlas.preload(RuntimeEnvironment.getApplication())
      assertSame(initial.image, frame()!!.image)
      assertSame(hall, AuthoredStructureAtlas.landmark("summoning_hall", 268, 206))
    }
    assertEquals(16, ArtDepthAssets.decodeCount)
    assertEquals(6, AuthoredHeroAtlas.decodeCount)
    assertEquals(1, TowerCombatFxAtlas.decodeCount)
    assertNull(frame(name = "Unknown"))
    assertNull(frame(type = HeroClassType.MAGE))
  }

  @Test fun materialCellsReassembleContinuouslyIncludingNegativeWorldCoordinates() {
    preload()
    val grass = requireNotNull(ArtDepthAssets.family("sanctuary_grass"))
    val cells = grass.map { it.asAndroidBitmap() }
    assertEquals(16, cells.size)
    for (y in -8..8) for (x in -8..8) {
      assertSame(grass[Math.floorMod(y, 4) * 4 + Math.floorMod(x, 4)], ArtDepthAssets.tile(grass, x, y))
    }
    for (n in 0 until 256) {
      fun pixel(x: Int, y: Int) = cells[y / 64 * 4 + x / 64].getPixel(x % 64, y % 64)
      assertEquals(pixel(n, 0), pixel(n, 255))
      assertEquals(pixel(0, n), pixel(255, n))
    }
    assertNull(ArtDepthAssets.family("missing"))
  }

  @Test fun contactPoseFollowsSimulationAndRangedReleaseIsImmediate() {
    preload()
    for (skill in listOf(false, true)) {
      val timeline = ContactAttackTimeline().apply { start("target", skill) }
      assertEquals(6, frame(progress = timeline.progress, skill = skill)!!.index)
      assertFalse(timeline.advance(timeline.contact - .001f))
      assertEquals(6, frame(progress = timeline.progress, skill = skill)!!.index)
      assertTrue(timeline.advance(.001f))
      assertEquals(7, frame(progress = timeline.progress, skill = skill)!!.index)
      assertFalse(timeline.advance(.101f))
      assertEquals(8, frame(progress = timeline.progress, skill = skill)!!.index)
      assertFalse(timeline.advance(1f))
      assertEquals(0, frame(progress = timeline.progress, skill = skill)!!.index)
    }
    for ((name, type) in roster.drop(1)) {
      assertEquals(7, frame(name, type, progress = 1f)!!.index)
      assertEquals(8, frame(name, type, progress = .70f)!!.index)
    }
  }

  @Test fun allNamedPosesAreDistinctAndRenderedThroughSharedProductionPath() {
    preload()
    TowerMonsterAtlas.preload(RuntimeEnvironment.getApplication())
    TowerCombatFxAtlas.preload(RuntimeEnvironment.getApplication())
    val bitmap = Bitmap.createBitmap(1440, 640, Bitmap.Config.ARGB_8888)
    val scope = CanvasDrawScope()
    scope.draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(1440f, 640f)) {
      drawRect(Color(0xFF263B36))
      for ((row, entry) in roster.withIndex()) {
        val (name, type) = entry
        val frames = listOf(frame(name, type), frame(name, type, time = .41f)) +
          (0..3).map { frame(name, type, moving = true, time = it / 9f + .001f) } +
          listOf(frame(name, type, progress = 1f), frame(name, type, progress = .5f),
            frame(name, type, progress = .1f), frame(name, type, hurt = 1f),
            frame(name, type, resting = true), frame(name, type, hp = 0f))
        // Ranged release intentionally skips the anticipation cell in runtime selection.
        val expectedUnique = if (name == "Kaelen") 12 else 11
        assertEquals(expectedUnique, frames.map { requireNotNull(it).index }.toSet().size)
        for (back in listOf(false, true)) for (right in listOf(false, true)) {
          val col = (if (back) 2 else 0) + (if (right) 0 else 1)
          drawCharacterSprite(0f, 0f, 720f - (180f + col * 360f),
            (320f - (180f + row * 190f)) / .72f, 1f,
            false, type, right, false, 0f, 0f, 1f, null, false, 0f,
            facingNorth = back, heroName = name)
        }
      }
    }
    val file = File("build/visual_screenshots/art-depth-reforge/heroes-production-directions.png")
    file.parentFile?.mkdirs()
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    assertTrue(file.length() > 10_000)
    bitmap.recycle()
    for (style in listOf(CombatFxStyle.GUARD, CombatFxStyle.ARCANE, CombatFxStyle.CLEAVE, CombatFxStyle.HOLY)) {
      val hashes = (0..7).map { i ->
        val image = requireNotNull(TowerCombatFxAtlas.frame(style, (i + .01f) / 8f)).image.asAndroidBitmap()
        val pixels = IntArray(image.width * image.height)
        image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
        assertTrue(pixels.any { it ushr 24 > 0 })
        pixels.contentHashCode()
      }
      assertEquals(8, hashes.toSet().size)
    }
  }
}
