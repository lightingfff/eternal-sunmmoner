package com.example

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.CourierOutcome
import com.example.data.model.TacticalOrder
import com.example.game.combat.*
import com.example.game.map.ObliqueCamera
import com.example.game.pixel.*
import com.example.game.pixel.CourierPresentation.drawCourier
import com.example.game.pixel.CourierPresentation.drawEliteTelegraph
import com.example.game.pixel.CourierPresentation.drawScoutAlert
import com.example.game.pixel.PixelSpriteRenderer.drawActorCanopyOcclusion
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawEnemySprite
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.PixelSpriteRenderer.drawWorldBackground
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.tower.TowerFloorCatalog
import com.example.ui.components.CourierMissionHud
import com.example.ui.theme.EternalSummonerTheme
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.SummonerGold
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Production Android Canvas and Compose evidence, not a physical-device performance test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LostCourierVisualTest {
  @get:Rule val compose = createComposeRule()
  private val out = File("build/visual_screenshots/p05").apply { mkdirs() }

  @Before fun loadRuntimeAssets() {
    SanctuaryArtAssets.init(org.robolectric.RuntimeEnvironment.getApplication())
  }

  @Test fun captureARealEscortScoutFearTelegraphAndRescuePlaythrough() {
    val engine = encounter()
    val mission = engine.lostCourier!!
    val kaelen = engine.heroes.first()
    val log = StringBuilder("event,simulation_seconds,phase,courier_hp,courier_x,courier_y,order,tactical_order,scout_warning,kaelen_fear,alive_enemies\n")
    var seconds = 0f
    fun record(event: String) {
      log.append("$event,$seconds,${mission.phase},${mission.courier.hp},${mission.courier.x},${mission.courier.y},${mission.order},${engine.activeOrder},${mission.scoutWarningRemaining},${kaelen.fearSecondsRemaining},${engine.enemies.count { !it.dead }}\n")
    }
    fun capture(name: String) {
      record(name)
      this@LostCourierVisualTest.capture(engine, name, seconds)
    }
    engine.stage = MissionStage.IN_PROGRESS // The same action as dismissing the opening dialogue.
    engine.chooseCourierOrder(CourierOrder.PROTECT)
    capture("01_opening_clear_path")
    var departed = false
    var scoutCaptured = false
    var fearCaptured = false
    var scoutResolved = false
    var telegraphCaptured = false
    var recoveryCaptured = false
    var exitEscortOrdered = false
    var dodging: CourierTelegraph? = null
    for (tick in 0 until 4800) {
      engine.step(.05f, 0f, 0f)
      seconds += .05f
      if (!departed && mission.phase == CourierPhase.ESCORT) {
        assertTrue("Escort must follow actual guard defeats", engine.enemies.filter { it.id.startsWith("courier_path_") }.all { it.dead })
        capture("02_escort_departure")
        departed = true
      }
      if (!scoutCaptured && mission.scoutWarningRemaining > 0f) {
        capture("03_scout_warning")
        engine.chooseCourierOrder(CourierOrder.INTERCEPT)
        assertEquals(TacticalOrder.ATTACK_TARGET, engine.activeOrder)
        assertEquals(mission.scoutEnemyId, engine.designatedTargetId)
        record("COMMAND_INTERCEPT")
        scoutCaptured = true
      }
      if (scoutCaptured && !fearCaptured && kaelen.fearSecondsRemaining > 0f && !kaelen.contactAttack.active) {
        assertTrue("Fear must affect the real combat unit", kaelen.effectiveAttack < kaelen.attack && kaelen.effectiveSpeed < kaelen.speed)
        capture("04_intercept_and_kaelen_fear")
        fearCaptured = true
      }
      if (scoutCaptured && !scoutResolved && mission.scoutWarningRemaining <= 0f) {
        engine.chooseCourierOrder(CourierOrder.PROTECT)
        record(if (mission.scoutCalledReinforcements) "COMMAND_PROTECT_AFTER_ALARM" else "COMMAND_PROTECT_AFTER_INTERCEPT")
        scoutResolved = true
      }
      val cue = mission.eliteTelegraph
      if (cue != null && cue !== dodging) {
        if (!telegraphCaptured) {
          capture("05_elite_telegraph_before_damage")
          telegraphCaptured = true
        }
        engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
        engine.onBattlefieldTapped(cue.x - 220f, cue.y)
        record("COMMAND_MOVE_OUT_OF_LOCKED_TELEGRAPH")
        dodging = cue
      }
      if (cue == null && dodging != null && mission.eliteRecoveryRemaining > 0f) {
        val elite = engine.enemies.firstOrNull { it.id == mission.eliteEnemyId && !it.dead }
        if (elite != null) {
          engine.onBattlefieldTapped(elite.x, elite.y)
          engine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE)
          record("COMMAND_FOCUS_FIRE_DURING_RECOVERY")
        }
        if (!recoveryCaptured) {
          capture("06_elite_recovery_counterattack")
          recoveryCaptured = true
        }
        dodging = null
      }
      if (!exitEscortOrdered && mission.eliteEnemyId != null && engine.enemies.any { it.id == mission.eliteEnemyId && it.dead }) {
        engine.chooseCourierOrder(CourierOrder.PROTECT)
        record("COMMAND_PROTECT_TO_SAFE_GATE")
        exitEscortOrdered = true
      }
      if (mission.outcome != null) break
    }
    capture(if (mission.outcome == CourierOutcome.RESCUED) "07_rescued_at_safe_gate" else "07_route_final_state")
    File(out, "courier_playthrough.csv").writeText(log.toString())
    File(out, "README.txt").writeText(
      "Native Android Canvas production renderer helpers, Floor 1's unchanged map/props, world-Y sorted heroes/enemies/courier, normal zoom=1, 1280x720.\n" +
        "One real TowerCombatEngine playthrough at 0.05s simulation steps. Commands and phase/HP/fear values are in courier_playthrough.csv.\n" +
        "Explicit test party: Kaelen knight level1 HP480/ATK42/DEF32, Lyra archer level4 HP350/ATK44/DEF20, Mira priest level4 HP330/ATK30/DEF20. No equipment.\n" +
        "No hero/enemy/courier position, HP, damage, outcome, or encounter phase is assigned by the world-capture fixture. Intro dismissal matches the runtime button.\n" +
        "World images isolate production rendering without fabricating a mission HUD. courier_hud_* separately captures the real Compose component and real order callbacks.\n" +
        "These are native desktop test captures, NOT DEVICE TESTED; they establish neither phone FPS nor thermal behavior.\n")
    assertTrue("The real playthrough must reach escort departure", departed)
    assertTrue("The real playthrough must show the scout's counterplay window", scoutCaptured)
    assertTrue("The real playthrough must expose Kaelen's temporary fear", fearCaptured)
    assertTrue("The real playthrough must reach a telegraphed elite attack", telegraphCaptured)
    assertTrue("The real playthrough must reach the elite's recovery window", recoveryCaptured)
    assertEquals("Tactical responses must leave the escort completable", CourierOutcome.RESCUED, mission.outcome)
  }

  @Test
  @Config(qualifiers = "w400dp-h720dp-port-mdpi")
  fun courierHudOrdersDriveTheRealEncounterAndRemainTouchable() {
    val engine = encounter()
    val mission = engine.lostCourier!!
    var revision by mutableIntStateOf(0)
    var host: View? = null
    compose.setContent {
      host = LocalView.current
      revision // This small component follows the same explicit HUD invalidation as the battle screen.
      EternalSummonerTheme {
        CourierMissionHud(mission.objectiveVi, mission.statusVi, mission.courier.hp,
          mission.courier.maxHp, mission.progress, engine.activeOrder == TacticalOrder.PROTECT_ALLY,
          onProtect = { engine.chooseCourierOrder(CourierOrder.PROTECT); revision++ },
          onIntercept = { engine.chooseCourierOrder(CourierOrder.INTERCEPT); revision++ },
          modifier = Modifier.fillMaxWidth(),
          interceptAvailable = mission.scoutWarningRemaining > 0f &&
            engine.enemies.any { it.id == mission.scoutEnemyId && !it.dead },
          intercepting = engine.activeOrder == TacticalOrder.ATTACK_TARGET &&
            engine.designatedTargetId == mission.scoutEnemyId)
      }
    }
    compose.waitForIdle()
    val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
    val card = compose.onNodeWithTag("courier_mission_hud").fetchSemanticsNode().boundsInRoot
    for (tag in listOf("courier_protect_order", "courier_intercept_order")) {
      val button = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
      assertTrue("$tag must retain a 44dp touch target", button.height >= 44f)
      assertTrue("$tag must stay inside the mission card", button.left >= card.left && button.right <= card.right)
      assertTrue("$tag must stay inside the viewport", button.bottom <= root.bottom)
    }
    compose.onNodeWithTag("courier_protect_order").assertIsSelected()
    compose.onNodeWithTag("courier_intercept_order").assertIsNotEnabled()
    saveView(host!!, "courier_hud_opening_400dp")
    compose.runOnIdle {
      engine.stage = MissionStage.IN_PROGRESS
      engine.chooseCourierOrder(CourierOrder.PROTECT)
      for (tick in 0 until 1600) {
        engine.step(.05f, 0f, 0f)
        if (mission.scoutWarningRemaining > 0f || mission.outcome != null) break
      }
      assertTrue("A real active scout must enable the intercept order", mission.scoutWarningRemaining > 0f)
      revision++
    }
    compose.onNodeWithTag("courier_intercept_order").assertIsEnabled()
    compose.onNodeWithTag("courier_intercept_order").performClick()
    compose.runOnIdle {
      assertEquals(CourierOrder.INTERCEPT, mission.order)
      assertEquals(TacticalOrder.ATTACK_TARGET, engine.activeOrder)
      assertEquals(mission.scoutEnemyId, engine.designatedTargetId)
    }
    compose.onNodeWithTag("courier_intercept_order").assertIsSelected()
    saveView(host!!, "courier_hud_intercept_400dp")
    compose.onNodeWithTag("courier_protect_order").performClick()
    compose.runOnIdle {
      assertEquals(CourierOrder.PROTECT, mission.order)
      assertEquals(TacticalOrder.PROTECT_ALLY, engine.activeOrder)
    }
    compose.onNodeWithTag("courier_protect_order").assertIsSelected()
    saveView(host!!, "courier_hud_protect_400dp")
    compose.runOnIdle { engine.issueTacticalOrder(TacticalOrder.MOVE_TO); revision++ }
    compose.onNodeWithTag("courier_protect_order").assertIsNotSelected()
    compose.onNodeWithTag("courier_intercept_order").assertIsNotSelected()
  }

  private fun encounter() = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), listOf(
    HeroRecord(id = "visual_kaelen", name = "Kaelen", heroClass = HeroClassType.KNIGHT.name,
      level = 1, maxHp = 480, attack = 42, defense = 32, speed = 110),
    HeroRecord(id = "visual_lyra", name = "Lyra", heroClass = HeroClassType.ARCHER.name,
      level = 4, maxHp = 350, attack = 44, defense = 20, speed = 115),
    HeroRecord(id = "visual_mira", name = "Mira", heroClass = HeroClassType.PRIEST.name,
      level = 4, maxHp = 330, attack = 30, defense = 20, speed = 105)),
    emptyList(), enableLostCourier = true)

  @Test
  @Config(qualifiers = "w400dp-h720dp-port-mdpi")
  fun fullTowerPortraitKeepsMissionAndControlsApart() = captureFullBattle("tower_portrait_400dp")

  @Test
  @Config(qualifiers = "w720dp-h400dp-land-mdpi")
  fun fullTowerLandscapeKeepsMissionAndControlsApart() = captureFullBattle("tower_landscape_720dp")

  @Test
  @Config(qualifiers = "w400dp-h720dp-port-mdpi")
  fun actualTowerScreenShowsJourneyWaypointAtGameplayZoom() =
    captureFullBattle("tower_journey_gameplay_400dp", advanceToJourney = true)

  private fun captureFullBattle(name: String, advanceToJourney: Boolean = false) {
    val base = org.robolectric.RuntimeEnvironment.getApplication()
    val files = java.nio.file.Files.createTempDirectory(base.cacheDir.toPath(), "courier-ui-").toFile()
    val context = object : android.content.ContextWrapper(base) {
      override fun getApplicationContext(): android.content.Context = this
      override fun getFilesDir(): File = files
    }
    val initial = kotlinx.coroutines.runBlocking {
      com.example.data.local.LocalGuestSaveManager(context).startOrResumeGuestSession()
    }
    var host: View? = null
    compose.mainClock.autoAdvance = false
    compose.setContent {
      host = LocalView.current
      EternalSummonerTheme {
        com.example.ui.screens.TowerBattleScreen(
          state = com.example.ui.SummonerUiState(profile = initial.profile, heroes = initial.heroes,
            equipment = initial.equipment, isBootstrapping = false),
          onFinishExpedition = { _, _ -> }, onReturnToSanctuary = {}, sceneActive = advanceToJourney)
      }
    }
    compose.onNodeWithTag("start_combat_button").performClick()
    compose.onNodeWithTag("start_combat_button").performClick()
    compose.mainClock.advanceTimeByFrame()
    compose.waitForIdle()
    if (advanceToJourney) {
      compose.mainClock.advanceTimeBy(20_500L)
      compose.waitForIdle()
      saveView(host!!, name)
      compose.onNodeWithText("Hành trình", substring = true).assertExists()
      compose.onNodeWithTag("rescue_journey_follow").assertIsEnabled()
    }
    val mission = compose.onNodeWithTag("rescue_journey_hud").fetchSemanticsNode().boundsInRoot
    val command = compose.onNodeWithTag("order_follow").fetchSemanticsNode().boundsInRoot
    assertTrue("Mission HUD must leave battlefield space above the command strip", command.top - mission.bottom >= 60f)
    saveView(host!!, name)
  }

  private class WorldDraw(val y: Float, val paint: DrawScope.() -> Unit)

  private fun capture(engine: TowerCombatEngine, name: String, seconds: Float) {
    val mission = engine.lostCourier!!
    val layout = engine.mapLayout
    val cx = (mission.courier.x + 90f).coerceIn(680f, layout.width - 680f)
    val cy = mission.courier.y
    val z = 1f
    val image = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image.asImageBitmap()), Size(1280f, 720f)) {
      drawWorldBackground(layout.biome, layout.width, layout.height, layout.wallMargin,
        cx, cy, z, seconds, layout.patches, layout.paths)
      engine.aoeEffects.forEach { fx ->
        val progress = (fx.elapsed / fx.duration).coerceIn(0f, 1f)
        val x = ObliqueCamera.worldToScreenX(fx.x, cx, z, size.width / 2)
        val y = ObliqueCamera.worldToScreenY(fx.y, cy, z, size.height / 2)
        val r = fx.maxRadius * (.35f + .65f * progress) * z
        val ry = r * ObliqueCamera.GROUND_ELLIPSE_RATIO
        drawOval(fx.color.copy(alpha = (1f - progress) * .35f), Offset(x - r, y - ry), Size(r * 2, ry * 2))
        drawOval(fx.color.copy(alpha = (1f - progress) * .9f), Offset(x - r, y - ry), Size(r * 2, ry * 2), style = Stroke(3f * z))
      }
      drawGroundShadows(layout.obstacles, cx, cy, z, clipToIsland = false,
        groundWidth = layout.width, groundHeight = layout.height)
      mission.eliteTelegraph?.let { cue ->
        drawEliteTelegraph(cue.x, cue.y, cue.radius, cue.remainingSec / cue.durationSec, cx, cy, z)
      }
      val drawables = mutableListOf<WorldDraw>()
      layout.obstacles.forEach { obstacle -> drawables += WorldDraw(obstacle.y + obstacle.sortYBias) {
        drawProp(obstacle, cx, cy, z, seconds, renderShadow = null)
      } }
      engine.heroes.filter { !it.knockedOut }.forEach { hero -> drawables += WorldDraw(hero.y) {
        drawCharacterSprite(hero.x, hero.y, cx, cy, z, false, hero.heroClass, hero.facingRight,
          hero.isMoving, hero.attackAnim, hero.hurtFlash, hero.hp.toFloat() / hero.maxHp,
          if (hero.shieldHp > 0) PortalCyan else if (engine.selectedHeroId == hero.id) SummonerGold.copy(alpha = .65f) else null,
          false, seconds, heroName = hero.name, skillAnimation = hero.premiumSample && hero.contactAttack.skill,
          emotion = when {
            engine.stage == MissionStage.VICTORY -> "victory"
            hero.fearSecondsRemaining > 0f || hero.hp < hero.maxHp * .25f -> "fear"
            hero.currentTargetId != null -> "alert"
            else -> "calm"
          })
        drawActorCanopyOcclusion(layout.obstacles, hero.x, hero.y, cx, cy, z, seconds)
      } }
      engine.enemies.filter { !it.dead || it.deathAnimProgress < 1f }.forEach { enemy -> drawables += WorldDraw(enemy.y) {
        drawEnemySprite(enemy.x, enemy.y, cx, cy, z, enemy.archetype, enemy.facingRight,
          enemy.isMoving, enemy.attackAnim, enemy.hurtFlash, enemy.hp.toFloat() / enemy.maxHp,
          !enemy.dead && engine.designatedTargetId == enemy.id, seconds, if (enemy.dead) enemy.deathAnimProgress else 0f)
        drawActorCanopyOcclusion(layout.obstacles, enemy.x, enemy.y, cx, cy, z, seconds)
      } }
      val npc = mission.courier
      drawables += WorldDraw(npc.y) {
        drawCourier(npc.x, npc.y, cx, cy, z, npc.isMoving, npc.facingRight, npc.hurtFlash, npc.hp, npc.maxHp, seconds)
        drawActorCanopyOcclusion(layout.obstacles, npc.x, npc.y, cx, cy, z, seconds)
      }
      drawables.sortedBy { it.y }.forEach { it.paint(this) }
      if (mission.scoutWarningRemaining > 0f && !mission.scoutCalledReinforcements) {
        engine.enemies.firstOrNull { it.id == mission.scoutEnemyId && !it.dead }?.let { scout ->
          drawScoutAlert(scout.x, scout.y, cx, cy, z)
        }
      }
      engine.projectiles.forEach { projectile ->
        val x = ObliqueCamera.worldToScreenX(projectile.x, cx, z, size.width / 2)
        val y = ObliqueCamera.worldToScreenY(projectile.y, cy, z, size.height / 2, 28f)
        val tailX = ObliqueCamera.worldToScreenX(projectile.x - projectile.vx * .045f, cx, z, size.width / 2)
        val tailY = ObliqueCamera.worldToScreenY(projectile.y - projectile.vy * .045f, cy, z, size.height / 2, 28f)
        drawLine(projectile.color.copy(alpha = .55f), Offset(tailX, tailY), Offset(x, y), projectile.radius * 1.4f * z)
        drawCircle(projectile.color, projectile.radius * z, Offset(x, y))
      }
    }
    save(name, image)
  }

  private fun saveView(view: View, name: String) {
    val image = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    view.draw(AndroidCanvas(image))
    save(name, image)
  }
  private fun save(name: String, image: Bitmap) {
    File(out, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
  }
}
