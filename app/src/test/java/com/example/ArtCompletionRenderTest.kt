package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.HeroClassType
import com.example.game.combat.*
import com.example.game.map.BiomeTheme
import com.example.game.map.EnemyArchetype
import com.example.game.pixel.*
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.PixelSpriteRenderer.drawWorldBackground
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.TowerCombatVfxRenderer.drawExecutionFx
import com.example.game.pixel.TowerCombatVfxRenderer.drawCombatProjectile
import com.example.game.pixel.TowerMonsterRenderer.drawTowerMonster
import com.example.game.tower.TowerFloorCatalog
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import java.io.File
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Native production-render specimens. Arranged actors/states are not unlocked player progression or phone captures. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtCompletionRenderTest {
  private val out = File("build/visual_screenshots/art-completion")
  private fun preload() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    TowerMonsterAtlas.preload(context)
    CompletionProjectileAtlas.preload(context)
    TowerCombatFxAtlas.preload(context)
    WorldEnvironment.debugOverride(12f, WorldWeather.CLEAR)
    WorldEnvironment.update(50_000)
  }
  @After fun clearEnvironment() { WorldEnvironment.debugOverride() }
  private fun render(name: String, width: Int = 1200, height: Int = 1000, block: DrawScope.() -> Unit) {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(width.toFloat(), height.toFloat()), block)
    val file = File(out, "$name.png"); file.parentFile?.mkdirs()
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    assertTrue(file.length() > 1000)
    bitmap.recycle()
  }

  @Test fun allClassesAndEnemiesRenderAtThreeZoomsIncludingExecutionHurtAndDefeat() {
    preload()
    val layout = TowerFloorCatalog.buildTowerFloorMap(TowerFloorCatalog.FLOORS.first())
    for (zoom in listOf(.36f, .88f, 1.48f)) for (phase in listOf("idle", "execution", "hurt", "defeat")) {
      render("actors_z${zoom}_$phase") {
        drawWorldBackground(layout.biome, layout.width, layout.height, layout.wallMargin,
          1600f, 1100f, zoom, 1.25f, layout.patches, layout.paths)
        HeroClassType.entries.forEachIndexed { i, type ->
          drawCharacterSprite(1380f + i % 3 * 220f, 900f + i / 3 * 160f,
            1600f, 1100f, zoom, false, type, i % 2 == 0, phase == "idle",
            if (phase == "execution") .98f else 0f, if (phase == "hurt") 1f else 0f,
            if (phase == "defeat") 0f else 1f, null, false, 1.25f,
            facingNorth = i % 2 != 0, heroName = "Unknown", skillAnimation = phase == "execution")
        }
        EnemyArchetype.entries.forEachIndexed { i, type ->
          val unit = EnemyCombatUnit("fixture_$i", type, 500, 500, 0, 0, 0f,
            1250f + i % 4 * 220f, 1260f + i / 4 * 200f).apply {
            facingRight = i % 2 == 0
            isMoving = phase == "idle"
            if (phase == "execution") attackAnim = .98f
            if (phase == "hurt") hurtFlash = 1f
            if (phase == "defeat") { dead = true; hp = 0; deathAnimProgress = .65f }
          }
          assertNotNull(TowerMonsterAtlas.frame(unit, 1.25f, false, false))
          drawTowerMonster(unit, 1600f, 1100f, zoom, 1.25f, false, true, false, false)
        }
      }
    }
  }

  @Test fun everyOutdoorBiomeUsesTheRealLayoutAndContinuousMaterials() {
    preload()
    val biomes = listOf(BiomeTheme.FIRST_FIELD_MEADOW, BiomeTheme.ELDERWOOD_FOREST,
      BiomeTheme.SUNKEN_RUINS, BiomeTheme.CRIMSON_CAVERN, BiomeTheme.FALLEN_STAR_TEMPLE)
    for (biome in biomes) {
      val floor = TowerFloorCatalog.FLOORS.first { it.biome == biome }
      val layout = TowerFloorCatalog.buildTowerFloorMap(floor)
      for (zoom in listOf(.36f, .88f, 1.48f)) {
        render("biome_${biome.name}_z$zoom", 1200, 720) {
          val x = layout.width * .5f; val y = layout.height * .5f
          drawWorldBackground(biome, layout.width, layout.height, layout.wallMargin,
            x, y, zoom, 1.25f, layout.patches, layout.paths)
          drawGroundShadows(layout.obstacles, x, y, zoom, clipToIsland = false,
            groundWidth = layout.width, groundHeight = layout.height)
          layout.obstacles.sortedBy { it.y + it.sortYBias }.forEach { drawProp(it, x, y, zoom, 1.25f, renderShadow = null) }
        }
      }
    }
  }

  @Test fun everyEffectAndProjectileRunsAtItsExistingSimulationPosition() {
    preload()
    render("execution-and-flight", 1280, 720) {
      drawRect(Color(0xFF172B2A))
      CombatFxStyle.entries.filter { it != CombatFxStyle.GROUND_RING }.forEachIndexed { i, style ->
        val x = 160f + i % 4 * 300f; val y = 180f + i / 4 * 240f
        val fx = CombatAoEEffect(0f, 0f, 110f, Color.Cyan, elapsed = .12f, style = style)
        drawExecutionFx(fx, 640f - x, (360f - y) / .72f, 1f, true)
        assertEquals(0f, fx.x); assertEquals(.12f, fx.elapsed)
      }
      ProjectileVisual.entries.forEachIndexed { i, visual ->
        for (frame in 0..3) {
          val x = 120f + i * 240f; val y = 570f + frame * 34f
          val projectile = CombatProjectile("$i-$frame", true, 0f, 0f, if (frame % 2 == 0) 300f else -200f,
            if (frame < 2) 0f else -100f, 10, Color.Cyan, remainingLife = 1.5f - frame / 12f, visual = visual)
          drawCombatProjectile(projectile, 640f - x, (360f - y - 28f) / .72f, 1f)
          assertEquals(0f, projectile.x); assertEquals(0f, projectile.y)
          assertEquals(10, projectile.damage)
        }
      }
    }
  }

  @Test fun surfacePhaseTracksPanAndDoesNotPaintOutsideItsMask() {
    preload()
    fun surface(dx: Float, dy: Float): Bitmap {
      val bitmap = Bitmap.createBitmap(300, 240, Bitmap.Config.ARGB_8888)
      CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(300f, 240f)) {
        val mask = Path().apply { addRect(Rect(30f - dx, 30f - dy, 270f - dx, 210f - dy)) }
        with(ArchitectureSurfaceArt) { assertTrue(drawSurface("limestone", mask, Offset(-dx, -dy), Offset(.88f, .13f), Offset(0f, .88f))) }
      }
      return bitmap
    }
    val a = surface(0f, 0f); val b = surface(9f, 11f)
    assertEquals(0, a.getPixel(5, 5))
    for (y in 50..190) for (x in 50..250) assertEquals(a.getPixel(x, y), b.getPixel(x - 9, y - 11))
    a.recycle(); b.recycle()
    assertEquals(4, ArchitectureSurfaceArt.decodeCount)
  }

  @Test fun retainedInteriorArchitectureStillMatchesTheNewMaterialPalette() {
    preload()
    val layouts = listOf(TowerFloorCatalog.buildRestaurantInteriorMap(), TowerFloorCatalog.buildDormitoryInteriorMap(),
      TowerFloorCatalog.buildTrainingHallInteriorMap(), TowerFloorCatalog.buildArmoryInteriorMap())
    for (layout in layouts) for (zoom in listOf(.36f, .88f)) render("interior_${layout.biome.name}${if (zoom == .36f) "_overview" else ""}", 800, 360) {
      val x = layout.width * .5f; val y = layout.height * .5f
      drawWorldBackground(layout.biome, layout.width, layout.height, layout.wallMargin,
        x, y, zoom, 1.25f, layout.patches, layout.paths)
      drawGroundShadows(layout.obstacles, x, y, zoom, clipToIsland = false,
        groundWidth = layout.width, groundHeight = layout.height)
      layout.obstacles.sortedBy { it.y + it.sortYBias }.forEach { drawProp(it, x, y, zoom, 1.25f, renderShadow = null) }
    }
  }

  @Test fun enemyBodiesMirrorExactlyWhileKeepingTheirFootAnchor() {
    preload()
    fun image(right: Boolean): Bitmap {
      val bitmap = Bitmap.createBitmap(400, 340, Bitmap.Config.ARGB_8888)
      val type = EnemyArchetype.DIRE_WOLF
      val enemy = EnemyCombatUnit("mirror", type, 100, 100, 0, 0, 0f, 0f, 0f).apply { facingRight = right }
      CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(400f, 340f)) {
        drawTowerMonster(enemy, 0f, -50f / .72f, 1f, 0f, false, true, false, false)
      }
      return bitmap
    }
    val right = image(true); val left = image(false)
    // Above the ground-contact penumbra, compare actual draw output including the symmetric HP bar.
    for (y in 0 until 205) for (x in 60..339) assertEquals(right.getPixel(x, y), left.getPixel(399 - x, y))
    right.recycle(); left.recycle()
  }
}
