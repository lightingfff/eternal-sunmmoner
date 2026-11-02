package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.HeroClassType
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.pixel.PixelSpriteRenderer
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.PremiumActorAtlas
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P17-C engine-render evidence for Lyra.
 *
 * These are the REAL draw calls (`drawSanctuaryFloatingWorld`, `drawCharacterSprite`,
 * depth-sorted props, canopy pass) executed on Robolectric's native Skia backend, so the
 * PNGs written to `build/visual_screenshots/p17c/` are renderer output, not a composite.
 *
 * Scope and honesty:
 *  - This proves asset lookup, clip/direction selection, atlas cropping, pivot/foot
 *    placement and depth/canopy layering. It is NOT a device, FPS or thermal benchmark.
 *  - Screens are software-rasterised at 1x density, so they do not show device DPI scaling.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LyraEngineRenderTest {

  private val outDir = File("build/visual_screenshots/p17c")

  private fun prepare() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(12f, WorldWeather.CLEAR)
    WorldEnvironment.update(50000)
  }

  /** Asset-level contract: the sheet really is the one the manifest pins, and it loads. */
  @Test
  fun lyraSheetIsWiredIntoTheRuntimeAtlas() {
    prepare()
    val atlas = PremiumActorAtlas.get("lyra")
    assertNotNull("PremiumActorAtlas must resolve the 'lyra' sheet", atlas)
    atlas!!
    assertEquals("pivotX", 32, atlas.pivotX)
    assertEquals("pivotY (ground anchor row)", 90, atlas.pivotY)
    assertEquals("worldPixelScale", 1.08f, atlas.worldPixelScale, 0.0001f)

    // Idle / walk / attack / skill / hit / rest / emotion truly select different bitmaps.
    val idle = atlas.frame(true, false, false, 0f, false, 0f, false, 0f)
    val walk = atlas.frame(true, false, true, 0f, false, 0f, false, 0f)
    val attack = atlas.frame(true, false, false, 0.5f, false, 0f, false, 0f)
    val skill = atlas.frame(true, false, false, 0.5f, true, 0f, false, 0f)
    val hit = atlas.frame(true, false, false, 0f, false, 0.9f, false, 0f)
    val rest = atlas.frame(true, false, false, 0f, false, 0f, true, 0f)
    val fear = atlas.frame(true, false, false, 0f, false, 0f, false, 0f, emotion = "fear")
    val victory = atlas.frame(true, false, false, 0f, false, 0f, false, 0f, emotion = "victory")
    val four = listOf(idle, walk, attack, skill, hit, rest, fear, victory).map { it.toPngHash() }
    assertEquals("every clip must resolve to a distinct frame", four.size, four.toSet().size)

    // Four facing rows must differ: a mapping bug would repeat one row.
    val dirs = listOf(
      atlas.frame(true, false, false, 0f, false, 0f, false, 0f),
      atlas.frame(false, false, false, 0f, false, 0f, false, 0f),
      atlas.frame(true, true, false, 0f, false, 0f, false, 0f),
      atlas.frame(false, true, false, 0f, false, 0f, false, 0f),
    ).map { it.toPngHash() }
    assertEquals("four distinct direction rows", 4, dirs.toSet().size)

    // The packaged PNG must equal the SHA-256 pinned in the manifest (same gate as CI).
    val context = RuntimeEnvironment.getApplication()
    val manifest = JSONObject(SanctuaryArtAssets.textAsset("premium_slice/manifest.json")!!)
    val entry = manifest.getJSONObject("assets").getJSONObject("lyra")
    val bytes = context.assets.open("premium_slice/${entry.getString("file")}").use { it.readBytes() }
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
      .joinToString("") { "%02x".format(it) }
    assertEquals("lyra.png SHA-256 vs manifest", entry.getString("sha256"), digest)
  }

  /**
   * The ground anchor must survive the real atlas crop for every clip: the lowest opaque row
   * of each runtime-cropped bitmap is the pivot row. A shadow-free, engine-side foot check.
   */
  @Test
  fun everyRuntimeFrameKeepsThePivotRow() {
    prepare()
    val atlas = PremiumActorAtlas.get("lyra")!!
    val states = listOf<Pair<String, ImageBitmap>>(
      "idle" to atlas.frame(true, false, false, 0f, false, 0f, false, 0f),
      "walk0" to atlas.frame(true, false, true, 0f, false, 0f, false, 0f, 0.1f),
      "walk4" to atlas.frame(true, false, true, 0f, false, 0f, false, 0f, 0.4f),
      "attack" to atlas.frame(true, false, false, 0.5f, false, 0f, false, 0f),
      "skill" to atlas.frame(false, false, true, 0.5f, true, 0f, false, 0f),
      "hit" to atlas.frame(true, true, false, 0f, false, 0.9f, false, 0f),
      "rest" to atlas.frame(false, true, false, 0f, false, 0f, true, 0f),
      "fear" to atlas.frame(true, false, false, 0f, false, 0f, false, 0f, emotion = "fear"),
      "victory" to atlas.frame(false, false, false, 0f, false, 0f, false, 0f, emotion = "victory"),
    )
    for ((name, frame) in states) {
      val bmp = frame.asAndroidBitmap()
      assertEquals("$name frame width", 64, bmp.width)
      assertEquals("$name frame height", 96, bmp.height)
      var lowest = -1
      for (y in 0 until bmp.height) for (x in 0 until bmp.width) {
        if ((bmp.getPixel(x, y) ushr 24) != 0) lowest = y
      }
      assertEquals("$name lowest opaque row must be the pivot row", atlas.pivotY, lowest)
    }
  }

  /** Wide shots at the three zooms the brief names, plus all four facings and clip states. */
  @Test
  fun captureLyraInTheRealSanctuaryScene() {
    prepare()
    outDir.mkdirs()
    val layout = SanctuaryLobbyEngine().mapLayout
    try {
      fun render(name: String, camX: Float, camY: Float, zoom: Float, actors: List<Actor>) {
        val w = 1280
        val h = 720
        val image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat())) {
          drawSanctuaryFloatingWorld(camX, camY, zoom, 0f, layout.paths)
          drawGroundShadows(layout.obstacles, camX, camY, zoom)
          val pending = actors.sortedBy { it.y }.toMutableList()
          for (obs in layout.obstacles.sortedBy { it.y + it.sortYBias }) {
            while (pending.isNotEmpty() && pending.first().y + pending.first().bias < obs.y + obs.sortYBias) {
              val a = pending.removeAt(0)
              drawLyra(a.x, a.y, camX, camY, zoom, a.right, a.north, a.moving, a.progress,
                a.skill, a.hurt, a.rest, a.time, a.emotion)
            }
            drawProp(obs, camX, camY, zoom, 0f, renderShadow = null)
          }
          for (a in pending) {
            drawLyra(a.x, a.y, camX, camY, zoom, a.right, a.north, a.moving, a.progress,
              a.skill, a.hurt, a.rest, a.time, a.emotion)
          }
          with(PixelSpriteRenderer) { drawUpperCanopyOcclusionPass(layout.obstacles, camX, camY, zoom, 0f) }
        }
        val file = File(outDir, "$name.png")
        file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("$name.png must not be empty", file.length() > 20_000)
      }

      // 1) Zoom sweep on the plaza: the brief asks for 0.36 / 0.88 / 1.48.
      for ((label, zoom) in listOf("zoom_036" to 0.36f, "zoom_088" to 0.88f, "zoom_148" to 1.48f)) {
        render(
          "lyra_$label", 1070f, 800f, zoom,
          listOf(Actor(950f, 1020f, true, false, false, 0f), Actor(990f, 1024f, true, false, true, 0f)),
        )
      }

      // 2) Four facings side by side at gameplay zoom, feet on the same ground line.
      render(
        "lyra_four_directions", 1070f, 800f, 1.48f,
        listOf(
          Actor(930f, 1030f, true, false, false, 0f),
          Actor(975f, 1030f, false, false, false, 0f),
          Actor(1020f, 1030f, true, true, false, 0f),
          Actor(1065f, 1030f, false, true, false, 0f),
        ),
      )

      // 3) Clip states driven by the same parameters gameplay uses.
      render(
        "lyra_clip_states", 1070f, 800f, 1.48f,
        listOf(
          Actor(880f, 1030f, true, false, false, 0.62f),
          Actor(925f, 1032f, true, false, false, 0.30f, skill = true),
          Actor(970f, 1030f, true, false, false, 0f, hurt = 0.9f),
          Actor(1015f, 1030f, true, false, true, 0f),
          Actor(1060f, 1032f, true, false, false, 0f, emotion = "fear"),
          Actor(1105f, 1030f, true, false, false, 0f, emotion = "victory"),
        ),
      )

      // 4) Walk cycle mid-stride at gameplay zoom for foot-slide review.
      render(
        "lyra_walk_cycle", 1200f, 900f, 1.48f,
        listOf(
          Actor(1140f, 1000f, true, false, true, 0.0f, time = 0.02f),
          Actor(1180f, 1000f, true, false, true, 0.0f, time = 0.18f),
          Actor(1220f, 1000f, true, false, true, 0.0f, time = 0.34f),
          Actor(1260f, 1000f, true, false, true, 0.0f, time = 0.50f),
        ),
      )
    } finally {
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
  }

  private data class Actor(
    val x: Float,
    val y: Float,
    val right: Boolean = true,
    val north: Boolean = false,
    val moving: Boolean = false,
    val progress: Float = 0f,
    val skill: Boolean = false,
    val hurt: Float = 0f,
    val rest: Boolean = false,
    val time: Float = 0f,
    val emotion: String = "calm",
    val bias: Float = 0f,
  )

  private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLyra(
    worldX: Float, worldY: Float, camX: Float, camY: Float, zoom: Float,
    right: Boolean, north: Boolean, moving: Boolean, progress: Float,
    skill: Boolean = false, hurt: Float = 0f, rest: Boolean = false,
    time: Float = 0f, emotion: String = "calm",
  ) = drawCharacterSprite(
    worldX = worldX, worldY = worldY, camX = camX, camY = camY, zoom = zoom,
    isMaster = false, heroClass = HeroClassType.ARCHER,
    facingRight = right, isMoving = moving, attackAnimProgress = progress,
    hurtFlash = hurt, hpFraction = 1f, shieldOrAuraColor = null,
    isInjuredResting = rest, timeSec = time, facingNorth = north,
    heroName = "Lyra", skillAnimation = skill, emotion = emotion,
  )

  private fun ImageBitmap.toPngHash(): String {
    val bitmap = asAndroidBitmap()
    val buffer = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, buffer)
    return MessageDigest.getInstance("SHA-256").digest(buffer.toByteArray())
      .joinToString("") { "%02x".format(it) }
  }
}
