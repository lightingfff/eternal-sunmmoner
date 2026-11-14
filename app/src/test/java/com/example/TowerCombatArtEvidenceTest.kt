package com.example

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas as NativeCanvas
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.LocalGuestSaveManager
import com.example.data.model.HeroClassType
import com.example.game.combat.CombatAoEEffect
import com.example.game.combat.CombatFxStyle
import com.example.game.combat.CombatProjectile
import com.example.game.combat.EnemyCombatUnit
import com.example.game.combat.ProjectileVisual
import com.example.game.map.EnemyArchetype
import com.example.game.map.ObliqueCamera
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawWorldBackground
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.TowerCombatVfxRenderer.drawCombatProjectile
import com.example.game.pixel.TowerCombatVfxRenderer.drawExecutionFx
import com.example.game.pixel.TowerMonsterAtlas
import com.example.game.pixel.TowerCombatFxAtlas
import com.example.game.pixel.TowerMonsterRenderer.drawTowerMonster
import com.example.game.tower.TowerFloorCatalog
import com.example.ui.SummonerUiState
import com.example.ui.screens.TowerBattleScreen
import com.example.ui.theme.EternalSummonerTheme
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Integrated from an independently prepared read-only QA draft.
 * Native host Canvas evidence, not device/GPU/FPS evidence.
 * Specimens deliberately arrange actors and animation states: NOT a combat playthrough.
 * Production input tests below use an actual fresh guest and no injected engine/state edits.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TowerCombatArtEvidenceTest {
  @get:Rule val rule = createComposeRule()
  private lateinit var host: View
  private val types = listOf(EnemyArchetype.FIELD_GOBLIN_RAIDER,
    EnemyArchetype.FIELD_GOBLIN_ARCHER, EnemyArchetype.DIRE_WOLF,
    EnemyArchetype.GOBLIN_WAR_CAPTAIN)
  private val output = File("build/visual_screenshots/tower-reforge")

  private fun enemy(type: EnemyArchetype, x: Float = 1600f, y: Float = 1100f) =
    EnemyCombatUnit("fixture_${type.name}", type, type.baseHp, type.baseHp,
      type.baseAtk, type.baseDef, type.speed, x, y)

  private fun preload() {
    val context = RuntimeEnvironment.getApplication() as Context
    SanctuaryArtAssets.init(context)
    TowerMonsterAtlas.preload(context)
    TowerCombatFxAtlas.preload(context)
    assertEquals("Optional loader fallback must not conceal a broken authored pack", 4, TowerMonsterAtlas.loadedCount)
    assertEquals("Each of the four source PNGs decodes once", 4, TowerMonsterAtlas.decodeCount)
    assertTrue(TowerMonsterAtlas.retainedBytes in 1L..8L * 1024 * 1024)
    assertEquals(1,TowerCombatFxAtlas.decodeCount)
    assertEquals(2359296L,TowerCombatFxAtlas.retainedBytes)
    assertTrue(TowerMonsterAtlas.retainedBytes+TowerCombatFxAtlas.retainedBytes<=8L*1024*1024)
  }

  /** Enumerates the actual selectors, not copies of private frame-index arithmetic. */
  private fun allFrames(type: EnemyArchetype, right: Boolean): List<TowerMonsterAtlas.Frame> {
    fun select(configure: (EnemyCombatUnit) -> Unit = {}, seconds: Float = 0f): TowerMonsterAtlas.Frame {
      val unit = enemy(type).apply { facingRight = right; configure(this) }
      return requireNotNull(TowerMonsterAtlas.frame(unit, seconds, false, false))
    }
    return listOf(select(), select(seconds = .351f)) +
      listOf(0f, .101f, .201f, .301f).map { time -> select({ it.isMoving = true }, time) } +
      listOf(0f, .22f, .321f).map { time -> select({ it.contactAttack.start("target"); it.contactAttack.advance(time) }) } +
      listOf(select({ it.hurtFlash = 1f }), select({ it.dead = true; it.deathAnimProgress = 0f }),
        select({ it.dead = true; it.deathAnimProgress = .65f }))
  }

  @Test fun preloadValidatesEveryPremultipliedFrameBothFacesAndKeepsWarmDecodeCountStable() {
    preload()
    val initialBytes = TowerMonsterAtlas.retainedBytes
    for (type in types) {
      val right = allFrames(type, true)
      val left = allFrames(type, false)
      assertEquals(12, right.size)
      assertEquals(12, (right + left).map { System.identityHashCode(it.image.asAndroidBitmap()) }.distinct().size)
      (right + left).forEach { frame ->
        val bitmap = frame.image.asAndroidBitmap()
        assertFalse(bitmap.isRecycled)
        assertTrue(bitmap.isPremultiplied)
        assertTrue(bitmap.hasAlpha())
        assertEquals(128, bitmap.width)
        assertEquals(144, bitmap.height)
        assertEquals(132, frame.pivotY)
      }
      right.zip(left).forEach { (a, b) ->
        assertSame(a.image, b.image)
        assertEquals(a.pivotX, b.pivotX)
      }
    }
    repeat(20) {
      TowerMonsterAtlas.preload(RuntimeEnvironment.getApplication())
      types.forEach { type -> allFrames(type, it % 2 == 0) }
    }
    assertEquals(4, TowerMonsterAtlas.decodeCount)
    assertEquals(initialBytes, TowerMonsterAtlas.retainedBytes)
    output.mkdirs()
    output.resolve("atlas_native_measurements.txt").writeText(
      "Robolectric SDK36 NATIVE; decoded=${TowerMonsterAtlas.decodeCount}; actors=${TowerMonsterAtlas.loadedCount}; " +
        "retainedBytes=${TowerMonsterAtlas.retainedBytes}; 48 cached enemy surfaces, draw-time mirror; " +
        "vfxDecode=${TowerCombatFxAtlas.decodeCount}; vfxBytes=${TowerCombatFxAtlas.retainedBytes}; " +
        "combinedBytes=${TowerMonsterAtlas.retainedBytes+TowerCombatFxAtlas.retainedBytes}; NOT device memory/FPS\n")
  }

  @Test fun contactPoseChangesAtTheActualPoint22SecondDamageMarker() {
    preload()
    for (type in types) for (right in listOf(false, true)) {
      val unit = enemy(type).apply { facingRight = right }
      unit.contactAttack.start("actual_timeline_target")
      assertFalse(unit.contactAttack.advance(.219f))
      val before = requireNotNull(TowerMonsterAtlas.frame(unit, 99f, false, false))
      assertSame(before, TowerMonsterAtlas.frame(enemy(type).apply { facingRight = right }, 0f, true, false))
      assertTrue("The same timeline must emit the damage contact at .22s", unit.contactAttack.advance(.001f))
      val at = requireNotNull(TowerMonsterAtlas.frame(unit, 99f, false, false))
      assertNotSame("Anticipation and contact must select different authored surfaces", before.image, at.image)
      val instantContact = enemy(type).apply { facingRight = right; attackAnim = 1f }
      assertSame("Contact agrees with the manifest's actual impact pose", at,
        TowerMonsterAtlas.frame(instantContact, 0f, false, false))
      assertFalse("Drawing cannot emit another simulation contact", unit.contactAttack.advance(0f))
    }
  }

  @Test fun authoredSkillClipsAdvanceThroughDistinctPremultipliedSurfacesWithoutReload() {
    preload()
    val styles=listOf(CombatFxStyle.GUARD,CombatFxStyle.ARCANE,CombatFxStyle.CLEAVE,CombatFxStyle.HOLY)
    val bitmaps=styles.flatMap { style -> (0..7).map { index ->
      requireNotNull(TowerCombatFxAtlas.frame(style,(index+.1f)/8f)).image.asAndroidBitmap()
    } }
    assertEquals(32,bitmaps.map { System.identityHashCode(it) }.distinct().size)
    bitmaps.forEach { assertFalse(it.isRecycled);assertTrue(it.isPremultiplied);assertTrue(it.hasAlpha()) }
    repeat(100) { index -> styles.forEach { TowerCombatFxAtlas.frame(it,(index%8)/8f) } }
    assertEquals(1,TowerCombatFxAtlas.decodeCount)
    assertEquals(2359296L,TowerCombatFxAtlas.retainedBytes)
  }

  @Test fun captainSlamShowsContactBeforeSettlingIntoRecovery() {
    preload()
    val captain=enemy(EnemyArchetype.GOBLIN_WAR_CAPTAIN).apply { attackAnim=1f }
    val contact=TowerMonsterAtlas.frame(captain,0f,false,false)
    assertSame(contact,TowerMonsterAtlas.frame(captain,0f,false,true))
    captain.attackAnim=.3f
    val recovery=TowerMonsterAtlas.frame(captain,0f,false,true)
    assertNotSame(contact,recovery)
    val afterContact=enemy(EnemyArchetype.GOBLIN_WAR_CAPTAIN).apply {
      contactAttack.start("spatial_contact");contactAttack.advance(.321f)
    }
    assertSame(recovery,TowerMonsterAtlas.frame(afterContact,0f,false,false))
  }

  private data class Specimen(val zoom: Float, val panel: String, val defeat: Float?, val authored: Boolean = true)

  @Test @Config(qualifiers = "w1200dp-h800dp-land-mdpi")
  fun arrangedRealRendererMatrixOnTowerGrassAndEdgeBackgrounds() {
    preload()
    val layout = TowerFloorCatalog.buildTowerFloorMap(TowerFloorCatalog.FLOORS.first())
    val fixedSeconds = 1.25f
    fun render(current:Specimen,name:String) {
      val bitmap=Bitmap.createBitmap(1200,800,Bitmap.Config.ARGB_8888)
      val canvas=androidx.compose.ui.graphics.Canvas(bitmap.asImageBitmap())
      androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
        androidx.compose.ui.unit.Density(1f),androidx.compose.ui.unit.LayoutDirection.Ltr,
        canvas,Size(1200f,800f)) {
            val cx = 1600f; val cy = 1100f; val zoom = current.zoom
            drawWorldBackground(layout.biome, layout.width, layout.height, layout.wallMargin,
              cx, cy, zoom, fixedSeconds, layout.patches, layout.paths)
            if (current.panel != "grass") drawRect(
              if (current.panel == "light") Color(0xFFF0E8D4) else Color(0xFF101321),
              Offset(32f, 48f), Size(size.width - 64f, size.height - 80f))
            val monsters = (0..1).flatMap { facing -> types.mapIndexed { column, type ->
              enemy(type, 1300f + column * 200f, if (facing == 0) 1000f else 1300f).apply {
                facingRight = facing == 0
                if (current.defeat != null) { dead = true; hp = 0; deathAnimProgress = current.defeat }
                else if (column == 0) { contactAttack.start("arranged_hero"); contactAttack.advance(.22f) }
                else if (column == 1) isMoving = true
                else if (column == 2) hurtFlash = .8f
              }
            } }
            val effects = CombatFxStyle.entries.mapIndexed { index, style ->
              CombatAoEEffect(1340f + (index % 3) * 260f, 1020f + (index / 3) * 150f,
                45f, if (index % 2 == 0) Color(0xFF65CFFF) else Color(0xFFF7CF71),
                elapsed = .12f, duration = .55f, style = style)
            }
            effects.forEach { drawExecutionFx(it, cx, cy, zoom, false) }
            monsters.take(4).forEach { drawTowerMonster(it, cx, cy, zoom, fixedSeconds, false, current.authored, false, false) }
            drawCharacterSprite(1600f, 1120f, cx, cy, zoom, false, HeroClassType.KNIGHT,
              true, false, 0f, 0f, 1f, null, false, fixedSeconds, heroName = "Kaelen")
            monsters.drop(4).forEach { drawTowerMonster(it, cx, cy, zoom, fixedSeconds, false, current.authored, false, false) }
            effects.forEach { drawExecutionFx(it, cx, cy, zoom, true) }
            ProjectileVisual.entries.forEachIndexed { index, visual ->
              drawCombatProjectile(CombatProjectile("specimen_$index", true, 1390f + index * 105f,
                1450f, 230f, -50f, 1, Color(0xFF9CE8F4), visual = visual), cx, cy, zoom)
            }
      }
      val caption=android.graphics.Paint().apply { color=android.graphics.Color.WHITE;textSize=13f }
      NativeCanvas(bitmap).drawText("ARRANGED NATIVE RENDER FIXTURE - NOT PLAYTHROUGH - fixed t=1.25s - groundY=.72",8f,18f,caption)
      NativeCanvas(bitmap).drawText("zoom=${current.zoom} panel=${current.panel} defeat=${current.defeat} authored=${current.authored}",8f,36f,caption)
      output.mkdirs()
      output.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
      bitmap.recycle()
    }
    for(zoom in listOf(.36f,.88f,1.48f))for(panel in listOf("grass","light","dark"))
      for(defeat in listOf<Float?>(null,.10f,.65f)) {
        render(Specimen(zoom,panel,defeat),"arranged_z${zoom.toString().replace('.','_')}_${panel}_"+
          if(defeat==null) "alive" else "defeat_${defeat.toString().replace('.','_')}")
      }
    for(authored in listOf(false,true))render(Specimen(.88f,"grass",null,authored),
      if(authored) "enemy_comparison_authored" else "enemy_comparison_original")
    assertEquals("Warm native drawing must not decode PNGs",4,TowerMonsterAtlas.decodeCount)
  }

  @Test @Config(qualifiers = "w400dp-h800dp-port-mdpi")
  fun productionPortraitPinnedMoveCommandAndPhysicalGroundTap() = productionMove("portrait")

  @Test @Config(qualifiers = "w800dp-h360dp-land-mdpi")
  fun productionShortLandscapePinnedMoveCommandAndPhysicalGroundTap() = productionMove("landscape_short")

  private fun startProduction() {
    preload()
    val base = RuntimeEnvironment.getApplication() as Context
    val directory = Files.createTempDirectory(base.cacheDir.toPath(), "tower-art-input-").toFile()
    val context = object : ContextWrapper(base) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = directory
    }
    val guest = runBlocking { LocalGuestSaveManager(context).startOrResumeGuestSession() }
    assertEquals(3, guest.heroes.count { it.inActiveSquad })
    rule.mainClock.autoAdvance = false
    rule.setContent {
      host = LocalView.current
      EternalSummonerTheme {
        TowerBattleScreen(SummonerUiState(isBootstrapping = false, profile = guest.profile,
          selectedFloor = TowerFloorCatalog.FLOORS.first(), heroes = guest.heroes, equipment = guest.equipment),
          onFinishExpedition = { _, _ -> }, onReturnToSanctuary = {})
      }
    }
    rule.mainClock.advanceTimeByFrame()
    repeat(2) {
      rule.onNodeWithTag("start_combat_button").performTouchInput { click() }
      rule.mainClock.advanceTimeBy(32L)
    }
  }

  @Test @Config(qualifiers = "w400dp-h800dp-port-mdpi")
  fun actualGuestNorthernAssaultProducesANativeExecutionSequence() {
    startProduction()
    rule.onNodeWithTag("living_lead_north").performTouchInput { click() }
    var pending=false
    repeat(90) {
      if(!pending) {
        rule.mainClock.advanceTimeBy(1000L)
        pending=rule.onAllNodesWithTag("living_decision_north_assault").fetchSemanticsNodes().isNotEmpty()
      }
    }
    assertTrue("Fresh guest party must physically reach North before choosing assault",pending)
    capture("production_assault_00_decision")
    rule.onNodeWithTag("living_decision_north_assault").performTouchInput { click() }
    repeat(24) { index ->
      rule.mainClock.advanceTimeBy(150L)
      capture("production_assault_sequence_${index.toString().padStart(2,'0')}")
    }
  }

  private fun productionMove(orientation: String) {
    startProduction()
    val move = rule.onNodeWithTag("order_move_to").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    assertTrue(move.width >= 48f && move.height >= 48f)
    val before = rule.mainClock.currentTime
    rule.onNodeWithTag("order_move_to").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    assertTrue(rule.mainClock.currentTime - before < 250L)
    rule.onNodeWithText("Chạm lên mặt đất chiến trường", substring = true).assertExists()
    rule.onNodeWithText("Đang chờ lệnh").assertExists()
    capture("production_${orientation}_01_move_prompt")
    val top = rule.onNodeWithTag("tower_battle_top_hud").fetchSemanticsNode().boundsInRoot
    val joystick = rule.onNodeWithTag("virtual_joystick").fetchSemanticsNode().boundsInRoot
    val canvas = rule.onNodeWithTag("tower_battle_canvas").fetchSemanticsNode().boundsInRoot
    assertTrue("Physical ground tap requires exposed battlefield", joystick.top - top.bottom >= 96f)
    val ground = Offset(canvas.center.x, (top.bottom + joystick.top) * .5f)
    rule.onNodeWithTag("tower_battle_canvas").performTouchInput { click(ground - canvas.topLeft) }
    rule.mainClock.advanceTimeBy(64L)
    rule.onNodeWithText("Đã cắm cờ điều quân tới vị trí mới", substring = true).assertExists()
    rule.onNodeWithText("Di chuyển: chạm vị trí", substring = true).assertDoesNotExist()
    rule.onNodeWithText("Đang chờ lệnh").assertExists()
    capture("production_${orientation}_02_placed_flag")
    rule.onNodeWithTag("living_resume").performTouchInput { click() }
    rule.mainClock.advanceTimeBy(64L)
    rule.onNodeWithText("Đội tự thám hiểm").assertExists()
    capture("production_${orientation}_03_resume")
  }

  private fun capture(name: String) {
    rule.runOnIdle {
      output.mkdirs()
      val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
      host.draw(NativeCanvas(bitmap))
      output.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
      bitmap.recycle()
    }
    output.resolve("${name}_semantics.txt").writeText(rule.onRoot().printToString())
  }
}
