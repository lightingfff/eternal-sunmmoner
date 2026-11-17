package com.example

import com.example.game.pixel.ArtDepthAssets
import com.example.game.pixel.AuthoredHeroAtlas
import com.example.game.pixel.SanctuaryArtAssets
import java.io.File
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Run this class alone to measure fresh asset init; host CPU walltime is not Android launch time. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtDepthColdStartTest {
  @Test fun recordsSynchronousInitializationSeparatelyFromWarmedDraws() {
    val context = RuntimeEnvironment.getApplication()
    val cold = ArtDepthAssets.decodeCount == 0 && AuthoredHeroAtlas.decodeCount == 0
    val start = System.nanoTime()
    SanctuaryArtAssets.init(context)
    val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
    val startWarm = System.nanoTime()
    repeat(100) { SanctuaryArtAssets.init(context) }
    val warmMs = (System.nanoTime() - startWarm) / 100_000_000.0
    assertEquals(16, ArtDepthAssets.decodeCount)
    assertEquals(6, AuthoredHeroAtlas.decodeCount)
    val report = JSONObject().put("observedColdLoad", cold).put("initWallMs", elapsedMs)
      .put("averageWarmInitWallMs", warmMs).put("materialsDecoded", ArtDepthAssets.decodeCount)
      .put("heroAndPortraitsDecoded", AuthoredHeroAtlas.decodeCount)
      .put("note", "Synchronous asset preparation on native Robolectric host; not phone cold launch, FPS, GPU or thermal evidence")
    val output = File("build/visual_screenshots/art-depth-reforge/asset-init-host.json")
    output.parentFile?.mkdirs()
    output.writeText(report.toString(2))
    println(report)
  }
}
