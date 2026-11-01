package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.FacilityType
import com.example.data.model.HeroClassType
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.map.*
import com.example.game.pixel.*
import com.example.game.pixel.PixelSpriteRenderer.drawActorCanopyOcclusion
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.tower.GeneratedMapLayout
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import java.io.File
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real Android Canvas pixels and production assets; these captures are not device FPS results. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DepthAddendumTest {
  private val out = File("build/visual_screenshots/depth_addendum").apply { mkdirs() }
  private var originalQuality = DepthPresentation.Quality.BALANCED
  private var originalFocus = true

  @Before fun initializeRuntimeAssets() {
    val context = org.robolectric.RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    originalQuality = DepthPresentation.quality
    originalFocus = DepthPresentation.selectiveFocusEnabled
    DepthPresentation.quality = DepthPresentation.Quality.BALANCED
    DepthPresentation.selectiveFocusEnabled = true
    timeOfDay(12f)
  }

  @After fun restorePresentationSettings() {
    DepthPresentation.quality = originalQuality
    DepthPresentation.selectiveFocusEnabled = originalFocus
    WorldEnvironment.debugOverride()
    WorldEnvironment.update()
  }

  @Test fun selectiveFocusChangesOnlyDistantDecorationInFullRenderer() {
    val layout = SanctuaryLobbyEngine().mapLayout
    // An overview exposes both far scenery and the complete opaque gameplay island.
    val camera = Scene(1530f, 1220f, .45f)
    DepthPresentation.selectiveFocusEnabled = false
    val sharp = scene(layout, camera, hud = true)
    DepthPresentation.selectiveFocusEnabled = true
    val focused = scene(layout, camera, hud = true)
    val mask = bitmap {
      val path = Path().apply {
        SanctuaryIslandGeometry.POLYGON_VERTICES.forEachIndexed { index, (x, y) ->
          val sx = ObliqueCamera.worldToScreenX(x, camera.cx, camera.zoom, size.width / 2)
          val sy = ObliqueCamera.worldToScreenY(y, camera.cy, camera.zoom, size.height / 2)
          if (index == 0) moveTo(sx, sy) else lineTo(sx, sy)
        }
        close()
      }
      drawPath(path, Color.White)
      // Protect actual sprite pixels, including any actor extending outside the ground mask.
      hero(camera.heroX, camera.heroY, camera)
      drawHud()
    }
    val a = pixels(sharp); val b = pixels(focused); val protected = pixels(mask)
    val changed = a.indices.filter { a[it] != b[it] }
    assertTrue("The focus toggle must visibly affect the decorative far layer", changed.size > 25)
    val changedSharpPixels = changed.count { protected[it] ushr 24 == 255 }
    assertEquals("Ground, active hero and UI must stay pixel-identical when focus changes", 0, changedSharpPixels)
    save("focus_off_overview", sharp)
    save("focus_on_overview", focused)
    File(out, "focus_isolation.txt").writeText(
      "Native Canvas comparison, 1280x720, camera=(1530,1220), zoom=0.45\n" +
        "Changed distant pixels=${changed.size}; changed opaque gameplay/actor/UI pixels=$changedSharpPixels\n")
  }

  @Test fun expandedClipsContainDistinctAuthoredFramesAndFixedGroundPivots() {
    val manifest = manifest()
    val width = manifest.getJSONArray("frame").getInt(0)
    val height = manifest.getJSONArray("frame").getInt(1)
    val order = clipOrder(manifest)
    val definitions = manifest.getJSONObject("clips")
    val minimums = mapOf("idle" to 6, "walk" to 8, "attack" to 6,
      "skill" to 8, "hit" to 3, "defeat" to 5)
    for ((clip, count) in minimums) {
      assertTrue("The authored atlas must include $clip", order.contains(clip))
      assertTrue("$clip must meet its authored frame minimum", definitions.getJSONArray(clip).length() >= count)
    }
    for (id in listOf("kaelen", "goblin_raider")) {
      val atlas = PremiumActorAtlas.get(id)!!
      assertSame("Decoded atlases must be reused", atlas, PremiumActorAtlas.get(id))
      assertEquals(32, atlas.pivotX)
      assertEquals(90, atlas.pivotY)
      assertEquals(1.08f, atlas.worldPixelScale, 0f)
      val source = SanctuaryArtAssets.decodeAsset("premium_slice/$id.png")!!.asAndroidBitmap()
      assertEquals(height * 4, source.height)
      assertEquals(order.sumOf { definitions.getJSONArray(it).length() } * width, source.width)
      var column = 0
      for (clip in order) {
        val count = definitions.getJSONArray(clip).length()
        minimums[clip]?.let { assertTrue("$id/$clip must meet the authored frame minimum", count >= it) }
        for (row in 0..3) {
          val frames = (0 until count).map { index ->
            Bitmap.createBitmap(source, (column + index) * width, row * height, width, height)
          }
          assertEquals("$id/$clip direction $row must contain unique poses, not duplicated padding",
            count, frames.map { pixels(it).contentHashCode() }.toSet().size)
          if (clip != "defeat") for ((index, frame) in frames.withIndex()) {
            val framePixels = pixels(frame)
            assertTrue("$id/$clip/$row/$index must retain a grounded foot at its shared pivot",
              (88..90).any { y -> (0 until width).any { x -> framePixels[y * width + x] ushr 24 >= 128 } })
          }
        }
        column += count
      }
    }
  }

  @Test fun damageContactSelectsTheAuthoredActionPoseForBothDirections() {
    val manifest = manifest()
    for (id in listOf("kaelen", "goblin_raider")) {
      val atlas = PremiumActorAtlas.get(id)!!
      for ((clip, contact) in listOf("attack" to .22f, "skill" to .30f)) {
        val durations = manifest.getJSONObject("clips").getJSONArray(clip)
        val duration = (0 until durations.length()).sumOf { durations.getDouble(it) }.toFloat()
        assertEquals(contact, manifest.getJSONObject("contact_seconds").getDouble(clip).toFloat(), .000001f)
        val contactFrame = manifest.getJSONObject("contact_frames").getInt(clip)
        for (row in 0..3) {
          val right = row % 2 == 0; val north = row >= 2
          val active = atlas.frame(right, north, false, 1f - contact / duration,
            clip == "skill", 0f, false, 0f)
          assertTrue("$id/$clip must show the authored action pose at real damage contact",
            active.asAndroidBitmap().sameAs(sourceFrame(id, clip, row, contactFrame)))
          val anticipation = atlas.frame(right, north, false, 1f - (contact - .001f) / duration,
            clip == "skill", 0f, false, 0f)
          assertFalse("$id/$clip anticipation must remain visually distinct until contact",
            active.asAndroidBitmap().sameAs(anticipation.asAndroidBitmap()))
        }
      }
    }
  }

  @Test fun heroEmotionsHaveDistinctBodyLanguageAtNormalGameplayZoom() {
    val atlas = PremiumActorAtlas.get("kaelen")!!
    val emotions = listOf("calm", "alert", "fear", "victory")
    val poses = emotions.map { emotion ->
      pixels(atlas.frame(true, false, false, 0f, false, 0f, false, 0f, emotion = emotion).asAndroidBitmap()).contentHashCode()
    }
    assertEquals("Each emotion needs a readable distinct authored pose", emotions.size, poses.toSet().size)
    save("kaelen_emotions_normal_zoom", bitmap(960, 300) {
      drawRect(Color(0xff263b37))
      emotions.forEachIndexed { index, emotion ->
        val s = Scene(0f, 0f, 1f, (index - 1.5f) * 190f, 65f)
        // Show each state's representative body language after any anticipation.
        hero(s.heroX, s.heroY, s, emotion, listOf(0f, .2f, .2f, .3f)[index])
      }
    })
    File(out, "kaelen_emotions_normal_zoom.txt").writeText(
      "Left to right: calm (0.0s), alert (0.2s), fear (0.2s), victory (0.3s). Production sprite renderer; zoom=1.0.\n")
  }

  @Test fun canopyRespectsActorDepthAndRetainsPartialVisibility() {
    // A smaller oak puts its crown at head height. A full-height oak correctly has
    // no low boughs over a south-side hero; do not extend its canopy into the trunk.
    val tree = WorldObstacle("depth_oak", 1500f, 1400f, 24f,
      propKind = PropKind.OAK_TREE, scale = .65f, occludesUpperCanopy = true)
    for (zoom in listOf(1f, 1.6f)) {
      val near = Scene(1500f, 1280f, zoom, 1515f, 1410f)
      val reference = isolatedTree(tree, near, occlusion = false)
      val covered = isolatedTree(tree, near, occlusion = true)
      val empty = isolatedTree(tree, near, occlusion = false, actor = false)
      assertTrue("Near canopy must overlap the actor's upper body at zoom $zoom",
        differences(reference, covered) > 10)
      assertTrue("Canopy overlap must retain visible actor pixels at zoom $zoom",
        differences(empty, covered) > 20)
      val front = near.copy(heroY = 1490f)
      assertArrayEquals("A hero farther south must never be covered by a global canopy redraw",
        pixels(isolatedTree(tree, front, occlusion = false)), pixels(isolatedTree(tree, front, occlusion = true)))
      val north = near.copy(heroY = 1360f)
      val sorted = isolatedTree(tree, north, occlusion = true, depthSorted = true)
      val sortedWithoutExtraPass = isolatedTree(tree, north, occlusion = false, depthSorted = true)
      assertArrayEquals("A tree later in world-Y order already occludes a northern actor",
        pixels(sortedWithoutExtraPass), pixels(sorted))
      assertTrue("North and south depth orders must produce a real overlap difference",
        differences(sorted, isolatedTree(tree, north, occlusion = false)) > 10)
      save("canopy_edge_z$zoom", covered)
      save("canopy_front_z$zoom", isolatedTree(tree, front, occlusion = true))
      save("canopy_behind_z$zoom", sorted)
    }
  }

  @Test fun projectionAnchorsLayoutAndEntrancesSurvivePresentationPasses() {
    val layout = SanctuaryLobbyEngine().mapLayout
    val beforeObstacles = layout.obstacles.map { it.copy() }
    val beforePaths = layout.paths.toList()
    val beforeEntrances = SanctuaryHouseEntrances.ENTRIES.toMap()
    val armory = layout.obstacles.single { it.id == "building_armory" }
    assertEquals(995f, armory.x, 0f); assertEquals(655f, armory.y, 0f)
    assertEquals(72f, armory.footprintHalfW, 0f); assertEquals(54f, armory.footprintDepthNorth, 0f)
    val door = SanctuaryHouseEntrances.ENTRIES.getValue(FacilityType.ARMORY)
    assertEquals(993f, door.doorWorldX, 0f); assertEquals(655f, door.doorWorldY, 0f)
    assertEquals(.72f, ObliqueCamera.OBLIQUE_Y_SCALE, 0f)
    for (zoom in listOf(.45f, 1f, 1.8f)) {
      val s = Scene(995f, 655f, zoom)
      val sx = ObliqueCamera.worldToScreenX(armory.x, s.cx, zoom, 640f)
      val sy = ObliqueCamera.worldToScreenY(armory.y, s.cy, zoom, 360f)
      assertEquals(armory.x, ObliqueCamera.screenToWorldX(sx, s.cx, zoom, 640f), .001f)
      assertEquals(armory.y, ObliqueCamera.screenToWorldY(sy, s.cy, zoom, 360f), .001f)
      assertEquals(100f * zoom, sy - ObliqueCamera.worldToScreenY(armory.y, s.cy, zoom, 360f, 100f), .001f)
      for (obstacle in layout.obstacles.filter { ObliqueSpriteGeometry.isTree(it.propKind) }) {
        val sprite = SanctuaryEnvironmentShadows.sprite(obstacle) ?: continue
        assertEquals("Visible tree roots must remain on world-ground anchors", 0f,
          ObliqueSpriteGeometry.drawFootOffset(obstacle.propKind, sprite) -
            ObliqueSpriteGeometry.bounds(sprite).bottomPadding * ObliqueSpriteGeometry.pixelScale(obstacle.propKind), .001f)
      }
      scene(layout, s)
    }
    assertEquals("Rendering must preserve obstacle anchors and collision footprints", beforeObstacles, layout.obstacles)
    assertEquals("Rendering must preserve navigation paths", beforePaths, layout.paths)
    assertEquals("Rendering must preserve entrances and outside spawns", beforeEntrances, SanctuaryHouseEntrances.ENTRIES)
  }

  @Test fun warmedShadowGeometryIsReusedAcrossCamerasAndLighting() {
    val obstacles = listOf(
      WorldObstacle("cache_tree", 1500f, 1400f, 24f, propKind = PropKind.OAK_TREE),
      WorldObstacle("cache_house", 1650f, 1450f, 40f, propKind = PropKind.SANCTUARY_COTTAGE_SLATE))
    bitmap { drawGroundShadows(obstacles, 1500f, 1400f, 1f) }
    val builds = SanctuaryEnvironmentShadows.casterGeometryBuildCount
    for (hour in listOf(12f, 20f)) {
      timeOfDay(hour)
      for (zoom in listOf(.65f, 1f, 1.6f)) bitmap {
        drawGroundShadows(obstacles, 1520f, 1390f, zoom)
      }
    }
    assertEquals("Warm shadow draws must reuse static silhouette/contact geometry", builds,
      SanctuaryEnvironmentShadows.casterGeometryBuildCount)
  }

  @Test fun foregroundWallsAndActorCanopiesPreserveTowerPortalPixels() {
    val layout = SanctuaryLobbyEngine().mapLayout
    val camera = Scene(1530f, 1220f, .45f)
    val before = scene(layout, camera, foregroundWalls = false, actorCanopy = false)
    val afterCanopy = scene(layout, camera, foregroundWalls = false)
    val afterWalls = scene(layout, camera)
    // This rectangle encloses the tower's complete blue portal and surrounding rim.
    fun portal(image: Bitmap) = Bitmap.createBitmap(image, 780, 0, 100, 130)
    val reference = pixels(portal(before))
    assertArrayEquals("Actor-local canopy layers must not cover the distant tower portal",
      reference, pixels(portal(afterCanopy)))
    assertArrayEquals("Southern foreground walls must not cover the distant tower portal",
      reference, pixels(portal(afterWalls)))
    val bluePixels = reference.count { pixel ->
      val red = pixel shr 16 and 255
      val green = pixel shr 8 and 255
      val blue = pixel and 255
      blue > 180 && green > 140 && blue > red + 20
    }
    assertTrue("The unchanged portal must retain its visible emissive blue core", bluePixels > 100)
    save("portal_checkpoint_before_canopy_and_walls", before)
    save("portal_checkpoint_after_canopy", afterCanopy)
    save("portal_checkpoint_after_foreground_walls", afterWalls)
    save("portal_core_before", portal(before))
    save("portal_core_after", portal(afterWalls))
    File(out, "portal_checkpoint.txt").writeText(
      "Native Android Canvas; noon; camera=(1530,1220), zoom=0.45.\n" +
        "Portal rectangle=(780,0)-(880,130). Actor canopy changes=0; southern foreground wall changes=0.\n" +
        "Visible emissive blue pixels=$bluePixels.\n")
  }

  @Test fun captureFullRuntimeSceneAtMatchedCamerasAndDepthPositions() {
    val layout = SanctuaryLobbyEngine().mapLayout
    for ((name, s) in listOf(
      "sample_normal" to Scene(1070f, 800f, 1f),
      "sample_close" to Scene(950f, 1000f, 1.8f),
      "architecture" to Scene(995f, 560f, 1.3f),
      "overview" to Scene(1530f, 1220f, .45f))) save(name, scene(layout, s))
    val tree = layout.obstacles.single { it.id == "sanctuary_sakura_14" }
    for (zoom in listOf(.65f, 1f, 1.6f)) {
      for ((name, offset) in listOf("behind" to -40f, "canopy_edge" to 10f, "front" to 100f)) {
        val s = Scene(tree.x, tree.y - 65f, zoom, tree.x + 30f, tree.y + offset)
        save("runtime_hero_${name}_z$zoom", scene(layout, s))
      }
    }
    timeOfDay(21f)
    save("sample_night", scene(layout, Scene(1070f, 800f, 1f)))
    save("overview_night", scene(layout, Scene(1530f, 1220f, .45f)))
    File(out, "README.txt").writeText(
      "Production Android Canvas rendering under Robolectric native graphics; 1280x720 unless named emotions.\n" +
        "sample_normal/sample_close/architecture/overview match the p15a evidence cameras and hero world anchor (950,1020).\n" +
        "runtime_hero_* uses the unchanged sanctuary_sakura_14 tree and world-Y sorted actor/prop order.\n" +
        "canopy_* isolates the exact same production occlusion helper for a readable regression comparison.\n" +
        "Focus off/on images vary only DepthPresentation.selectiveFocusEnabled.\n" +
        "No physical-device performance claim is made by these captures.\n")
  }

  private data class Scene(val cx: Float, val cy: Float, val zoom: Float,
    val heroX: Float = 950f, val heroY: Float = 1020f)

  private fun scene(layout: GeneratedMapLayout, s: Scene, hud: Boolean = false,
    foregroundWalls: Boolean = true, actorCanopy: Boolean = true): Bitmap = bitmap {
    drawSanctuaryFloatingWorld(s.cx, s.cy, s.zoom, 0f, layout.paths)
    with(LivingWorldRenderer) { drawWeather(s.cx, s.cy, s.zoom, false) }
    drawGroundShadows(layout.obstacles, s.cx, s.cy, s.zoom)
    var actorDrawn = false
    fun drawActor() {
      hero(s.heroX, s.heroY, s)
      if (actorCanopy) drawActorCanopyOcclusion(layout.obstacles, s.heroX, s.heroY, s.cx, s.cy, s.zoom, 0f)
      actorDrawn = true
    }
    for (obstacle in layout.obstacles.sortedBy { it.y + it.sortYBias }) {
      if (!actorDrawn && obstacle.y + obstacle.sortYBias > s.heroY) drawActor()
      drawProp(obstacle, s.cx, s.cy, s.zoom, 0f, renderShadow = null)
    }
    if (!actorDrawn) drawActor()
    if (foregroundWalls) with(PixelSpriteRenderer) { drawSouthernForegroundWalls(layout.biome, s.cx, s.cy, s.zoom) }
    with(LivingWorldRenderer) {
      drawWorldGrade()
      drawLocalLights(layout.obstacles, s.cx, s.cy, s.zoom)
      drawWeather(s.cx, s.cy, s.zoom, true)
    }
    if (hud) drawHud()
  }

  private fun isolatedTree(tree: WorldObstacle, s: Scene, occlusion: Boolean,
    actor: Boolean = true, depthSorted: Boolean = false): Bitmap = bitmap(640, 640) {
    drawRect(Color(0xff37483e))
    drawGroundShadows(listOf(tree), s.cx, s.cy, s.zoom)
    fun drawActor() {
      if (!actor) return
      hero(s.heroX, s.heroY, s)
      if (occlusion) drawActorCanopyOcclusion(listOf(tree), s.heroX, s.heroY, s.cx, s.cy, s.zoom, 0f)
    }
    if (depthSorted && s.heroY < tree.y + tree.sortYBias) {
      drawActor()
      drawProp(tree, s.cx, s.cy, s.zoom, 0f, renderShadow = false)
    } else {
      drawProp(tree, s.cx, s.cy, s.zoom, 0f, renderShadow = false)
      drawActor()
    }
  }

  private fun DrawScope.hero(x: Float, y: Float, s: Scene, emotion: String = "calm", timeSec: Float = 0f) =
    drawCharacterSprite(x, y, s.cx, s.cy, s.zoom, false, HeroClassType.KNIGHT, true,
      false, 0f, 0f, 1f, null, false, timeSec, heroName = "Kaelen", emotion = emotion)

  private fun DrawScope.drawHud() {
    drawRect(Color(0xff182027), Offset(18f, 18f), Size(172f, 38f))
    drawRect(Color(0xffd9bc86), Offset(24f, 24f), Size(24f, 24f))
    drawRect(Color(0xffe6dfcb), Offset(58f, 28f), Size(116f, 4f))
    drawRect(Color(0xff79ad8b), Offset(58f, 40f), Size(92f, 5f))
  }

  private fun bitmap(width: Int = 1280, height: Int = 720, draw: DrawScope.() -> Unit): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()),
      Size(width.toFloat(), height.toFloat()), draw)
    return bitmap
  }

  private fun pixels(bitmap: Bitmap) = IntArray(bitmap.width * bitmap.height).also {
    bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
  }
  private fun differences(a: Bitmap, b: Bitmap): Int {
    val ap = pixels(a); val bp = pixels(b)
    return ap.indices.count { ap[it] != bp[it] }
  }
  private fun save(name: String, bitmap: Bitmap) {
    File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
  }
  private fun timeOfDay(hour: Float) {
    WorldEnvironment.debugOverride(hour, WorldWeather.CLEAR)
    WorldEnvironment.update(50000)
  }
  private fun manifest() = JSONObject(SanctuaryArtAssets.textAsset("premium_slice/manifest.json")!!)
  private fun clipOrder(manifest: JSONObject): List<String> {
    val order = manifest.getJSONArray("clip_order")
    return (0 until order.length()).map { order.getString(it) }
  }
  private fun sourceFrame(id: String, clip: String, row: Int, index: Int): Bitmap {
    val manifest = manifest()
    val definitions = manifest.getJSONObject("clips")
    val order = clipOrder(manifest)
    val start = order.takeWhile { it != clip }.sumOf { definitions.getJSONArray(it).length() }
    val width = manifest.getJSONArray("frame").getInt(0)
    val height = manifest.getJSONArray("frame").getInt(1)
    val source = SanctuaryArtAssets.decodeAsset("premium_slice/$id.png")!!.asAndroidBitmap()
    return Bitmap.createBitmap(source, (start + index) * width, row * height, width, height)
  }
}
