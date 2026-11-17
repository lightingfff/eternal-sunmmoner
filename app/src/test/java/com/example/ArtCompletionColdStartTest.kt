package com.example

import com.example.game.pixel.*
import com.example.ui.screens.TowerGateMapArt
import java.io.File
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Run alone for fresh process-lifetime cache timing; host measurements are not Android device launch/FPS. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtCompletionColdStartTest {
  @Test fun recordsStartupTowerAndGateCostsSeparatelyWithCompletePixelBankAccounting() {
    val context = RuntimeEnvironment.getApplication()
    val cold = ArtDepthAssets.decodeCount == 0 && AuthoredHeroAtlas.decodeCount == 0 && CompletionClassAtlas.decodeCount == 0
    val start = System.nanoTime()
    SanctuaryArtAssets.init(context)
    val afterLobby = System.nanoTime()
    TowerMonsterAtlas.preload(context)
    CompletionProjectileAtlas.preload(context)
    TowerCombatFxAtlas.preload(context)
    val afterTower = System.nanoTime()
    val gates = TowerGateMapArt(context.assets)
    val afterGate = System.nanoTime()
    val warmStart = System.nanoTime()
    repeat(100) { SanctuaryArtAssets.init(context); TowerMonsterAtlas.preload(context); TowerCombatFxAtlas.preload(context) }
    val warmAverage = (System.nanoTime() - warmStart) / 100_000_000.0
    val banks = linkedMapOf("materials" to ArtDepthAssets.retainedBytes, "namedHeroes" to AuthoredHeroAtlas.retainedBytes,
      "hall" to AuthoredStructureAtlas.retainedBytes, "originalEnemies" to TowerMonsterAtlas.retainedBytes,
      "effects" to TowerCombatFxAtlas.retainedBytes, "projectiles" to CompletionProjectileAtlas.retainedBytes,
      "additionalClasses" to CompletionClassAtlas.retainedBytes, "additionalEnemies" to CompletionEnemyAtlas.retainedBytes,
      "surfaces" to ArchitectureSurfaceArt.retainedBytes, "props" to CompletionPropAtlas.retainedBytes,
      "interiors" to CompletionInteriorAtlas.retainedBytes, "gates" to gates.retainedBytes)
    assertEquals("art_completion_v1/gates", gates.sourcePack)
    assertEquals(27_570_400L, banks.values.sum())
    assertEquals(6, CompletionClassAtlas.decodeCount)
    assertEquals(4, CompletionEnemyAtlas.decodeCount)
    assertEquals(1, CompletionProjectileAtlas.decodeCount)
    val report = JSONObject().put("observedColdLoad", cold)
      .put("lobbyInitWallMs", (afterLobby - start) / 1_000_000.0)
      .put("firstTowerDecodeWallMs", (afterTower - afterLobby) / 1_000_000.0)
      .put("firstGateDecodeWallMs", (afterGate - afterTower) / 1_000_000.0)
      .put("averageWarmPreloadMs", warmAverage).put("retainedPixelBanks", JSONObject(banks as Map<*, *>))
      .put("retainedPixelTotalBytes", banks.values.sum())
      .put("note", "Native host CPU timing and bitmap-bank bytes; excludes other legacy/UI caches, shared geometry, transient source sheets, Java/native overhead and GPU. Not physical phone launch/FPS/heat evidence.")
    val file = File("build/visual_screenshots/art-completion/cold-init-host.json")
    file.parentFile?.mkdirs(); file.writeText(report.toString(2))
    println(report)
  }
}
