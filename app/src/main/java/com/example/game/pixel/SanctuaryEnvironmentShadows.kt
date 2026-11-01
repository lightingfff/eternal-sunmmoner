package com.example.game.pixel

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.game.map.*

/** Static alpha silhouettes projected once into world ground space, shared at every camera zoom. */
object SanctuaryEnvironmentShadows {
  private val projection = Matrix()
  private val bottomPadding = mutableMapOf<ImageBitmap, Int>()
  private val paths = mutableMapOf<ImageBitmap, Path>()
  private val island by lazy { Path().apply {
    SanctuaryIslandGeometry.POLYGON_VERTICES.forEachIndexed { i, p ->
      if (i == 0) moveTo(p.first,p.second) else lineTo(p.first,p.second)
    }; close()
  } }
  internal fun sprite(obs: WorldObstacle): ImageBitmap? = with(PixelStructureAtlas) {
    when (obs.propKind) {
      PropKind.SAKURA_BLOSSOM_TREE -> if (obs.id.hashCode() and 1 == 0) sakuraTreeSpriteA else sakuraTreeSpriteB
      PropKind.OAK_TREE -> oakTreeSprite
      PropKind.MYSTIC_PLUM_TREE -> mysticPlumTreeSprite
      PropKind.PINE_SPIRIT_TREE -> spiritPineSprite
      PropKind.CYPRESS_CONIFER_TREE -> cypressConiferSprite
      PropKind.GARDEN_HEDGE_BUSH -> gardenHedgeBushSprite
      PropKind.FOREST_CANOPY_BUSH -> forestBushSprite
      PropKind.FACILITY_HERO_QUARTERS -> heroQuartersStructure
      PropKind.FACILITY_ARMORY -> armoryStructure
      PropKind.FACILITY_RESTAURANT -> restaurantStructure
      PropKind.FACILITY_SYNTHESIS_LAB -> synthesisLabStructure
      PropKind.FACILITY_HERO_REGISTRY -> heroRegistryStructure
      PropKind.FACILITY_SUMMONING_HALL -> summoningHallStructure
      PropKind.FACILITY_TOWER_GATE, PropKind.TOWER_PORTAL_GATE -> towerGateMonument
      PropKind.MEDIEVAL_TOWNHOUSE_A -> medievalTownhouseA
      PropKind.MEDIEVAL_TOWNHOUSE_B -> medievalTownhouseB
      PropKind.SANCTUARY_COTTAGE_BROWN -> sanctuaryCottageBrown
      PropKind.SANCTUARY_COTTAGE_SLATE -> sanctuaryCottageSlate
      PropKind.FORTRESS_CORNER_TOWER -> fortressCornerTowerSprite
      PropKind.SOUTHERN_GATE_TOWER -> southernGateTowerSprite
      PropKind.OBSIDIAN_COLUMN, PropKind.RUINED_PILLAR -> obsidianColumnSprite
      PropKind.MOSSY_BOULDER -> mossyBoulderSprite
      PropKind.STREET_LANTERN -> streetLanternFrames[0]
      PropKind.BARREL_CLUSTER -> barrelClusterSprite
      PropKind.WOODEN_BENCH_34 -> woodenBenchSprite
      PropKind.STONE_BALUSTRADE_RAIL -> stoneBalustradeSprite
      PropKind.STONE_RETAINING_WALL -> stoneRetainingWallSprite
      PropKind.TERRACE_CLIFF_WALL -> terraceCliffWallSprite
      PropKind.TRAINING_STRIKING_POST -> trainingStrikingPostSprite
      else -> null
    }
  }
  // Cache normalized silhouette; height and sprite width are applied independently when drawing.
  internal fun silhouette(bitmap: ImageBitmap): Path = paths.getOrPut(bitmap) {
    val b = bitmap.asAndroidBitmap()
    val pixels = IntArray(b.width * b.height)
    b.getPixels(pixels,0,b.width,0,0,b.width,b.height)
    val bottom = (b.height - 1 downTo 0).firstOrNull { y ->
      (0 until b.width).any { x -> pixels[y*b.width+x] ushr 24 >= 128 }
    } ?: b.height - 1
    bottomPadding[bitmap] = b.height - 1 - bottom
    Path().apply {
      for (y in 0 until b.height) {
        var x = 0
        while (x < b.width) {
          if (pixels[y*b.width+x] ushr 24 < 128) { x++; continue }
          val start = x
          while (x < b.width && pixels[y*b.width+x] ushr 24 >= 128) x++
          // Union-like non-overlapping scanline quads preserve portal/window gaps and branch tips.
          addRect(androidx.compose.ui.geometry.Rect(start-b.width/2f, (bottom-y).toFloat(), x-b.width/2f, (bottom-y+1).toFloat()))
        }
      }
    }
  }
  fun DrawScope.drawGroundShadows(obstacles: List<WorldObstacle>, camX: Float, camY: Float, zoom: Float) {
    withTransform({
      translate(size.width/2f-camX*zoom,size.height/2f-camY*zoom*ObliqueCamera.OBLIQUE_Y_SCALE)
      scale(zoom,zoom*ObliqueCamera.OBLIQUE_Y_SCALE,androidx.compose.ui.geometry.Offset.Zero)
    }) {
      clipPath(island) {
        for (obs in obstacles) {
          val bitmap = sprite(obs) ?: continue
          val height = SanctuaryShadowRegistry.heightFor(obs.propKind) * obs.scale
          val path = silhouette(bitmap)
          val matrix = projection
          matrix.reset()
          // x'=2*scale*x + lightX*h; y'=lightY*h. Sprite height maps to authored world height.
          matrix.values[0] = 2f*obs.scale
          matrix.values[1] = 0f
          matrix.values[4] = SanctuaryLight.SHADOW_DIR_X * height / (bitmap.height*SanctuaryLight.TAN_ELEV)
          matrix.values[5] = SanctuaryLight.SHADOW_DIR_Y * height / (bitmap.height*SanctuaryLight.TAN_ELEV)
          matrix.values[12] = obs.x
          // Match the sprite's visible foot, including its transparent atlas padding.
          val footOffset = if (obs.propKind == PropKind.OAK_TREE || obs.propKind == PropKind.MYSTIC_PLUM_TREE || obs.propKind == PropKind.PINE_SPIRIT_TREE) 8f else 6f
          matrix.values[13] = obs.y + (footOffset - (bottomPadding[bitmap] ?: 0) * 2f) * obs.scale / ObliqueCamera.OBLIQUE_Y_SCALE
          withTransform({ transform(matrix) }) {
            drawPath(path,Color(0xff111b26).copy(alpha=SanctuaryShadowRegistry.opacityFor(obs.propKind)))
          }
        }
      }
    }
  }
}
