package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.*
import com.example.data.model.*
import com.example.game.combat.*
import com.example.game.map.*
import com.example.game.pixel.*
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawEnemySprite
import com.example.game.tower.TowerFloorCatalog
import com.example.game.lobby.*
import com.example.game.world.*
import com.example.ui.screens.LobbyDrawQueue
import com.example.ui.screens.LobbyDrawableItem
import java.io.File
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PremiumSliceCombatTest {
  @Before fun assets() {
    val context=org.robolectric.RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context);WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(12f,WorldWeather.CLEAR);WorldEnvironment.update(50000)
  }
  @After fun cleanup() { WorldEnvironment.debugOverride();WorldEnvironment.update() }
  private fun battle(skill: Boolean = false): TowerCombatEngine {
    // Deterministic encounter configuration; use the real tower map, AI, mitigation and skill.
    val spec = TowerFloorCatalog.FLOORS.first().copy(eventTriggerX=Float.MAX_VALUE,
      enemies=listOf(EnemySpawnSpec(EnemyArchetype.FIELD_GOBLIN_RAIDER,940f,1100f)))
    return TowerCombatEngine(spec,listOf(HeroRecord(id="hero_starter_kaelen",name="Kaelen",maxHp=480,attack=42,defense=32)),emptyList()).apply {
      stage = MissionStage.IN_PROGRESS
      heroes.single().apply { x=900f; y=1100f; skillCooldown=if(skill) 0f else 100f }
      issueTacticalOrder(TacticalOrder.HOLD_POSITION)
      enemies.single().apply { attackCooldown=100f }
    }
  }
  @Test fun contactIsOnceAndIndependentOfStepSize() {
    val a=ContactAttackTimeline();a.start("target")
    assertFalse(a.advance(.12f)); assertFalse(a.advance(.09f)); assertTrue(a.advance(.01f))
    assertFalse(a.advance(.2f)); assertFalse(a.advance(.5f)); assertFalse(a.active)
    val b=ContactAttackTimeline();b.start("target",true)
    assertTrue(b.advance(1f));assertFalse(b.advance(1f));assertFalse(b.active)
    b.start("target");b.cancel();assertFalse(b.advance(1f))
  }
  @Test fun kaelenDamageAndShieldAreDeliveredAtAuthoredContacts() {
    for(skill in listOf(false,true)) {
      val engine=battle(skill); val enemy=engine.enemies.single();val hero=engine.heroes.single()
      engine.step(.01f,0f,0f)
      assertTrue(hero.contactAttack.active);assertEquals(skill,hero.contactAttack.skill)
      val hp=enemy.hp
      engine.step(if(skill) .29f else .21f,0f,0f)
      assertEquals("No damage during anticipation",hp,enemy.hp)
      assertEquals(0,hero.shieldHp)
      engine.step(.01f,0f,0f)
      assertTrue("Real combat mitigation runs at contact",enemy.hp<hp)
      if(skill) assertEquals(77,hero.shieldHp)
      val after=enemy.hp
      engine.step(.12f,0f,0f)
      assertEquals("Recovery cannot hit twice",after,enemy.hp)
    }
  }
  @Test fun goblinContactAndStaleTargetAreSafe() {
    for (moveAway in listOf(false,true)) {
      val engine=battle();val hero=engine.heroes.single();val enemy=engine.enemies.single()
      hero.attackCooldown=100f;enemy.attackCooldown=0f
      engine.step(.01f,0f,0f)
      assertTrue(enemy.contactAttack.active)
      val hp=hero.hp
      engine.step(.21f,0f,0f);assertEquals(hp,hero.hp)
      if(moveAway) hero.x=800f
      engine.step(.01f,0f,0f)
      if(moveAway) assertEquals(hp,hero.hp) else assertTrue(hero.hp<hp)
    }
  }
  @Test fun retreatCancelsWindupAndUnrelatedKnightsRemainUnchanged() {
    val engine=battle();val enemy=engine.enemies.single();val hp=enemy.hp;engine.step(.01f,0f,0f)
    engine.issueTacticalOrder(TacticalOrder.RETREAT)
    engine.step(.3f,0f,0f)
    assertEquals(hp,enemy.hp);assertFalse(engine.heroes.single().contactAttack.active)
    val record=HeroRecord(id="another",name="Other knight")
    val other=TowerCombatEngine(TowerFloorCatalog.FLOORS.first(),listOf(record),emptyList()).heroes.single()
    assertFalse(other.premiumSample)
  }
  @Test fun drawQueueReusesObjectsAndRespondsToMovingActorsAndSceneChanges() {
    val engine=SanctuaryLobbyEngine()
    engine.syncHeroes(listOf(HeroRecord(id="k",name="Kaelen")),1000)
    val roster=engine.heroActors
    assertSame(roster,engine.heroActors)
    val queue=LobbyDrawQueue();val first=queue.ordered(engine.mapLayout.obstacles,roster)
    val wrapper=first.filterIsInstance<LobbyDrawableItem.HeroNpc>().first { it.actor.heroId=="k" }
    wrapper.actor.y=900f
    val again=queue.ordered(engine.mapLayout.obstacles,roster)
    assertSame(first,again);assertTrue(again.any { it === wrapper })
    assertTrue(again.zipWithNext().all { it.first.sortY<=it.second.sortY })
    engine.enterFacilityInterior(FacilityType.ARMORY)
    val inside=engine.heroActors;assertNotSame(roster,inside);assertSame(inside,engine.heroActors)
    engine.exitFacilityInterior();assertTrue(engine.heroActors.any { it.heroId=="k" })
    engine.syncHeroes(emptyList(),1001);assertFalse(engine.heroActors.any { it.heroId=="k" })
  }
  @Test fun assetsAndShadowsAreCachedAndFarOffscreenCastersDoNotLoad() {
    val before=PixelCharacterAtlas.builtEnemySheetCount
    val legacy=PixelCharacterAtlas.enemySheet(EnemyArchetype.FIELD_GOBLIN_ARCHER)
    assertTrue(PixelCharacterAtlas.builtEnemySheetCount<=before+1)
    assertSame(legacy,PixelCharacterAtlas.enemySheet(EnemyArchetype.FIELD_GOBLIN_ARCHER))
    val atlas=PremiumActorAtlas.get("kaelen")!!
    assertSame(atlas,PremiumActorAtlas.get("kaelen"))
    assertEquals(90,atlas.pivotY)
    val release=atlas.frame(true,false,false,1f-.30f/.72f,true,0f,false,0f)
    val rounded=atlas.frame(true,false,false,1f-.29999995f/.72f,true,0f,false,0f)
    assertSame("Float rounding cannot leave damage on the windup pose",release,rounded)
    assertNotSame(release,atlas.frame(true,false,false,1f-.29f/.72f,true,0f,false,0f))
    val image=Bitmap.createBitmap(320,240,Bitmap.Config.ARGB_8888).asImageBitmap()
    val paths=SanctuaryEnvironmentShadows.cachedSilhouetteCount
    val assets=SanctuaryArtAssets.cachedImageCount
    CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(320f,240f)) {
      with(SanctuaryEnvironmentShadows) {
        drawGroundShadows(listOf(WorldObstacle("far",100000f,100000f,24f,propKind=PropKind.OAK_TREE)),0f,0f,1f)
      }
    }
    assertEquals(paths,SanctuaryEnvironmentShadows.cachedSilhouetteCount)
    assertEquals(assets,SanctuaryArtAssets.cachedImageCount)
  }
  @Test fun captureRealCombatSequence() {
    val out=File("build/visual_screenshots/p15a/combat").apply { mkdirs() }
    File(out,"events.csv").writeText("capture,action_seconds,goblin_hp,hero_shield\n")
    for(skill in listOf(false,true)) {
      val engine=battle(skill)
      engine.step(.01f,0f,0f)
      var elapsed=0f
      for(t in listOf(0f,.12f,.21f,.22f,.29f,.30f,.38f,.48f,.62f,.72f)) {
        while(elapsed+.0001f<t) { val dt=minOf(.01f,t-elapsed);engine.step(dt,0f,0f);elapsed+=dt }
        for(z in listOf(1f,2f)) capture(engine,out,"${if(skill) "skill" else "basic"}_${(t*1000).toInt()}_z$z",z,t)
      }
    }
  }
  @Test fun captureIdleWalkAndDefeatFromSimulation() {
    val out=File("build/visual_screenshots/p15a/motion").apply { mkdirs() }
    val idle=battle().apply { heroes.single().attackCooldown=100f }
    for(i in 0..8) { idle.step(.1f,0f,0f);capture(idle,out,"idle_$i",2f,i*.1f) }
    val walk=battle().apply {
      enemies.single().x=1460f
      issueTacticalOrder(TacticalOrder.ATTACK_TARGET)
    }
    for(i in 0..5) {
      walk.step(.1f,0f,0f)
      assertTrue("Actual AI movement selects the walk clip",walk.heroes.single().isMoving)
      capture(walk,out,"walk_$i",2f,(i+1)*.1f)
    }
    val defeat=battle()
    val wounded=defeat.enemies.single().apply { hp=1 }
    // A distant, existing raider keeps the mission running while this one collapses.
    defeat.enemies.add(EnemyCombatUnit("distant",EnemyArchetype.FIELD_GOBLIN_RAIDER,155,155,22,6,92f,2800f,1900f))
    defeat.step(.01f,0f,0f);repeat(22) { defeat.step(.01f,0f,0f) }
    assertTrue("Defeat is triggered by the real damage path",wounded.dead)
    for(i in 0..5) { if(i>0) repeat(8) { defeat.step(.01f,0f,0f) };capture(defeat,out,"defeat_$i",2f,i*.08f) }
  }
  private fun capture(engine:TowerCombatEngine,out:File,name:String,z:Float,time:Float) {
    val image=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888).asImageBitmap()
    CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(960f,540f)) {
      val cx=925f;val cy=1080f
      with(PixelSpriteRenderer) {
        drawWorldBackground(engine.mapLayout.biome,engine.mapLayout.width,engine.mapLayout.height,
          engine.mapLayout.wallMargin,cx,cy,z,time,engine.mapLayout.patches,engine.mapLayout.paths)
      }
      val hero=engine.heroes.single();val enemy=engine.enemies.first()
      drawCharacterSprite(hero.x,hero.y,cx,cy,z,false,hero.heroClass,hero.facingRight,hero.isMoving,
        hero.attackAnim,hero.hurtFlash,hero.hp.toFloat()/hero.maxHp,null,false,time,
        heroName=hero.name,skillAnimation=hero.contactAttack.skill)
      drawEnemySprite(enemy.x,enemy.y,cx,cy,z,enemy.archetype,enemy.facingRight,enemy.isMoving,
        enemy.attackAnim,enemy.hurtFlash,enemy.hp.toFloat()/enemy.maxHp,false,time,enemy.deathAnimProgress)
    }
    File(out,"$name.png").outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    File(out,"events.csv").appendText("$name,${engine.heroes.single().contactAttack.elapsed},${engine.enemies.first().hp},${engine.heroes.single().shieldHp}\n")
  }
}
