package com.example

import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.map.PropKind
import com.example.game.map.SanctuaryHouseEntrances
import com.example.game.pixel.ObliqueSpriteGeometry
import com.example.game.pixel.PixelStructureAtlas
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryVegetation
import java.security.MessageDigest
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P22 Vegetation Forge asset, anchor, doorway-clearance, safe-fallback, and locked-control contract tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapForgeP22ContractTest {

  @Before
  fun setUp() {
    SanctuaryArtAssets.init(RuntimeEnvironment.getApplication())
  }

  private fun assetSha256(path: String): String {
    val bytes = RuntimeEnvironment.getApplication().assets.open(path).use { it.readBytes() }
    return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
  }

  @Test
  fun lockedControlsOakAndCypressAreByteIdenticalToP18AndP20Baselines() {
    assertEquals(
      "556eefd5c1b1f14872b67e6e651b8c7d6645e4632519d0719a6ce614b240a229",
      assetSha256("sanctuary_p03/oak.png"),
    )
    assertEquals(
      "442cbc617c6d61f79f2105f7c4b2072a2fc721fa3a4e56ee11d695d576cf988b",
      assetSha256("sanctuary_p03/cypress.png"),
    )
    assertTrue(PixelStructureAtlas.oakTreeSprite.width >= 105)
    assertEquals(48, PixelStructureAtlas.cypressConiferSprite.width)
    assertEquals(88, PixelStructureAtlas.cypressConiferSprite.height)
  }

  @Test
  fun authoredTreesPreserveExactCanvasAndAlpha128FootRowOffsets() {
    val sakuraA = PixelStructureAtlas.sakuraTreeSpriteA
    assertEquals(114, sakuraA.width)
    assertEquals(132, sakuraA.height)
    val bA = ObliqueSpriteGeometry.bounds(sakuraA)
    assertEquals(126, bA.bottom)
    assertEquals(5, bA.bottomPadding)
    assertEquals(10.0f, ObliqueSpriteGeometry.drawFootOffset(PropKind.SAKURA_BLOSSOM_TREE, sakuraA), 0.001f)

    val sakuraB = PixelStructureAtlas.sakuraTreeSpriteB
    assertEquals(96, sakuraB.width)
    assertEquals(114, sakuraB.height)
    val bB = ObliqueSpriteGeometry.bounds(sakuraB)
    assertEquals(109, bB.bottom)
    assertEquals(4, bB.bottomPadding)
    assertEquals(8.0f, ObliqueSpriteGeometry.drawFootOffset(PropKind.SAKURA_BLOSSOM_TREE, sakuraB), 0.001f)

    val pine = PixelStructureAtlas.spiritPineSprite
    assertEquals(96, pine.width)
    assertEquals(128, pine.height)
    val bP = ObliqueSpriteGeometry.bounds(pine)
    assertEquals(122, bP.bottom)
    assertEquals(5, bP.bottomPadding)
    assertEquals(10.0f, ObliqueSpriteGeometry.drawFootOffset(PropKind.PINE_SPIRIT_TREE, pine), 0.001f)

    val plum = PixelStructureAtlas.mysticPlumTreeSprite
    assertEquals(112, plum.width)
    assertEquals(132, plum.height)
    assertTrue(plum.width >= 100)
    val bM = ObliqueSpriteGeometry.bounds(plum)
    assertEquals(126, bM.bottom)
    assertEquals(5, bM.bottomPadding)
    assertEquals(10.25f, ObliqueSpriteGeometry.drawFootOffset(PropKind.MYSTIC_PLUM_TREE, plum), 0.001f)
  }

  @Test
  fun authoredShrubsMatchProceduralCanvasFootRowAndSafeFallbackContract() {
    val fallbackShrub = SanctuaryVegetation.shrub()
    val fbBounds = ObliqueSpriteGeometry.bounds(fallbackShrub)
    assertEquals(64, fallbackShrub.width)
    assertEquals(40, fallbackShrub.height)
    assertEquals(36, fbBounds.bottom)
    assertEquals(3, fbBounds.bottomPadding)

    val hedge = PixelStructureAtlas.gardenHedgeBushSprite
    assertEquals(64, hedge.width)
    assertEquals(40, hedge.height)
    val hedgeBounds = ObliqueSpriteGeometry.bounds(hedge)
    assertEquals(36, hedgeBounds.bottom)
    assertEquals(3, hedgeBounds.bottomPadding)
    assertEquals(4.0f, ObliqueSpriteGeometry.drawFootOffset(PropKind.GARDEN_HEDGE_BUSH, hedge), 0.001f)

    val forest = PixelStructureAtlas.forestBushSprite
    assertEquals(64, forest.width)
    assertEquals(40, forest.height)
    val forestBounds = ObliqueSpriteGeometry.bounds(forest)
    assertEquals(36, forestBounds.bottom)
    assertEquals(3, forestBounds.bottomPadding)
    assertEquals(4.0f, ObliqueSpriteGeometry.drawFootOffset(PropKind.FOREST_CANOPY_BUSH, forest), 0.001f)

    // Missing paths return null cleanly via assetSafe without throwing or poisoning the cache.
    assertNull(SanctuaryArtAssets.assetSafe("sanctuary_p03/__missing_p22_probe__.png"))
  }

  @Test
  fun sanctuaryVegetationPreservesLayoutCountsAndLeavesAllFacilityEntrancesClear() {
    val layout = SanctuaryLobbyEngine().mapLayout
    val sakuras = layout.obstacles.filter { it.propKind == PropKind.SAKURA_BLOSSOM_TREE }
    val hedges = layout.obstacles.filter { it.propKind == PropKind.GARDEN_HEDGE_BUSH }
    val cypresses = layout.obstacles.filter { it.propKind == PropKind.CYPRESS_CONIFER_TREE }
    val pines = layout.obstacles.filter { it.propKind == PropKind.PINE_SPIRIT_TREE }
    val plums = layout.obstacles.filter { it.propKind == PropKind.MYSTIC_PLUM_TREE }

    assertEquals(18, sakuras.size)
    assertEquals(9, sakuras.count { (it.id.hashCode() and 1) == 0 })
    assertEquals(9, sakuras.count { (it.id.hashCode() and 1) == 1 })
    assertEquals(5, hedges.size)
    assertEquals(3, cypresses.size)
    assertEquals(0, pines.size)
    assertEquals(0, plums.size)

    // No vegetation obstacle may overlap any SanctuaryHouseEntrances trigger radius.
    val veg = sakuras + hedges + cypresses
    for (door in SanctuaryHouseEntrances.ALL) {
      for (obs in veg) {
        val dist = hypot(door.doorX - obs.x, door.doorY - obs.y)
        assertTrue(
          "Vegetation ${obs.id} too close to ${door.id} (${dist}px)",
          dist > door.triggerRadius + obs.radius + 16f,
        )
      }
    }
  }
}
