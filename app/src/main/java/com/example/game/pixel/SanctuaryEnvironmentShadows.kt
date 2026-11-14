package com.example.game.pixel

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.geometry.*
import com.example.game.map.*

/** Static alpha silhouettes projected once into world ground space, shared at every camera zoom. */
object SanctuaryEnvironmentShadows {
  private val projection = Matrix()
  private val paths = mutableMapOf<ImageBitmap, Path>()
  internal val cachedSilhouetteCount get() = paths.size
  private val island by lazy { Path().apply {
    SanctuaryIslandGeometry.POLYGON_VERTICES.forEachIndexed { i, p ->
      if (i == 0) moveTo(p.first,p.second) else lineTo(p.first,p.second)
    }; close()
  } }
  internal fun sprite(obs: WorldObstacle): ImageBitmap? = with(PixelStructureAtlas) {
    CompletionInteriorAtlas.entry(obs.propKind)?.let { return it.image }
    when (obs.propKind) {
      PropKind.CRIMSON_CRYSTAL -> crimsonCrystalSprite
      PropKind.TATTERED_BANNER -> tatteredBannerSprite
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
      // P23 props: registered so each one gets the projected silhouette shadow + contact AO.
      PropKind.FLOWER_PLANTER_BED -> flowerPlanterSprite
      PropKind.TRAINING_DUMMY -> trainingDummySprite
      PropKind.WEAPON_RACK -> weaponRackSprite
      PropKind.ANCIENT_RUIN_ARCH -> ruinArchSprite
      PropKind.ANCIENT_MONOLITH, PropKind.SUMMON_OBELISK -> monolithSprite
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
  fun DrawScope.drawCharacterShadow(bitmap: ImageBitmap, cx: Float, cy: Float, zoom: Float, pixelScale: Float, groundPivotY: Int? = null, mirrored: Boolean = false) {
    val bounds = ObliqueSpriteGeometry.bounds(bitmap)
    val path = silhouette(bitmap)
    val shadowStrength = com.example.game.world.WorldEnvironment.lighting.shadowStrength
    projection.reset()
    // Mirror the caster's local silhouette, never the world-light shear direction.
    projection.values[0] = if (mirrored) -pixelScale else pixelScale
    projection.values[1] = 0f
    projection.values[4] = SanctuaryLight.SHADOW_DIR_X * pixelScale / SanctuaryLight.TAN_ELEV
    projection.values[5] = SanctuaryLight.SHADOW_DIR_Y * pixelScale * ObliqueCamera.OBLIQUE_Y_SCALE / SanctuaryLight.TAN_ELEV
    val pivotDelta = if (groundPivotY == null) 0f else (groundPivotY - bounds.bottom).toFloat()
    projection.values[12] = cx + projection.values[4]*pivotDelta
    projection.values[13] = if (groundPivotY == null) cy + 6f*zoom - bounds.bottomPadding*pixelScale
      else cy + projection.values[5]*pivotDelta
    withTransform({ transform(projection) }) {
      withTransform({ scale(1.06f, 1.04f, Offset.Zero) }) {
        drawPath(path, Color(0xFF080E1C).copy(alpha = (0.24f * shadowStrength).coerceIn(0f, 0.34f)))
      }
      drawPath(path, Color(0xFF050A14).copy(alpha = (0.56f * shadowStrength).coerceIn(0f, 0.72f)))
    }
    val contactRadius = (bitmap.width * pixelScale * 0.22f).coerceIn(10f * zoom, 28f * zoom)
    val contactRy = contactRadius * ObliqueCamera.GROUND_ELLIPSE_RATIO * 0.72f
    drawOval(
      color = Color(0x68060B16),
      topLeft = Offset(cx - contactRadius * 1.15f, cy - contactRy * 0.65f),
      size = Size(contactRadius * 2.30f, contactRy * 1.65f),
    )
    drawOval(
      color = Color(0xA8040811),
      topLeft = Offset(cx - contactRadius * 0.82f, cy - contactRy * 0.42f),
      size = Size(contactRadius * 1.64f, contactRy * 1.18f),
    )
    drawLine(Color(0x66050912), Offset(cx - contactRadius, cy + 1f * zoom), Offset(cx + contactRadius, cy + 1f * zoom), 4.5f * zoom)
    drawLine(Color(0x9903060D), Offset(cx - contactRadius * 0.72f, cy + 1f * zoom), Offset(cx + contactRadius * 0.72f, cy + 1f * zoom), 2.4f * zoom)
  }
  private class CasterGeometry(
    val silhouette: Path, val contact: Path, val spriteScale: Float,
    val visibleHeight: Float, val height: Float, val footOffset: Float, val tree: Boolean,
  )
  // Generated tower maps can be released; a global cache must not retain their obstacles.
  private val casters = java.util.WeakHashMap<WorldObstacle,CasterGeometry>()
  internal val cachedCasterCount get() = casters.size
  internal var casterGeometryBuildCount = 0; private set
  private fun geometry(obs: WorldObstacle): CasterGeometry? {
    casters[obs]?.let { return it }
    val bitmap = sprite(obs) ?: return null
    val bounds = ObliqueSpriteGeometry.bounds(bitmap)
    val scale = ObliqueSpriteGeometry.pixelScale(obs.propKind)
    val tree = ObliqueSpriteGeometry.isTree(obs.propKind)
    val depth = if (obs.propKind == PropKind.TATTERED_BANNER) 4f
      else if(obs.footprintDepthNorth>0f) obs.footprintDepthNorth else bitmap.width*scale*(if(tree) .5f else .35f)
    val height = (bounds.visibleHeight*scale-depth*ObliqueCamera.OBLIQUE_Y_SCALE).coerceAtLeast(8f)*obs.scale
    val foot = (ObliqueSpriteGeometry.drawFootOffset(obs.propKind,bitmap)-bounds.bottomPadding*scale)*obs.scale
    val hw = (if (obs.propKind == PropKind.TATTERED_BANNER) 4f * scale
      else if(tree) bitmap.width*scale*.13f else if(obs.footprintHalfW>0f) obs.footprintHalfW else bitmap.width*scale*.34f)*obs.scale
    val hd = (if(tree) hw*.42f else if(obs.footprintDepthNorth>0f) obs.footprintDepthNorth*.22f*obs.scale else hw*.22f).coerceAtLeast(2.5f)
    val contact = Path().apply {
      moveTo(-hw*.95f,-hd*.55f);lineTo(-hw*.62f,-hd);lineTo(hw*.75f,-hd)
      lineTo(hw,0f);lineTo(hw*.74f,hd*.58f);lineTo(-hw*.84f,hd*.58f);close()
    }
    return CasterGeometry(silhouette(bitmap),contact,scale*obs.scale,bounds.visibleHeight.toFloat(),height,
      (foot-obs.elevationZ)/ObliqueCamera.OBLIQUE_Y_SCALE,tree).also { casters[obs]=it;casterGeometryBuildCount++ }
  }
  fun DrawScope.drawGroundShadows(
    obstacles: List<WorldObstacle>,camX:Float,camY:Float,zoom:Float,
    clipToIsland:Boolean=true,groundWidth:Float=3200f,groundHeight:Float=2760f,
  ) {
    val halfWorldW=size.width/(2f*zoom)
    val halfWorldH=size.height/(2f*zoom*ObliqueCamera.OBLIQUE_Y_SCALE)
    withTransform({
      translate(size.width/2f-camX*zoom,size.height/2f-camY*zoom*ObliqueCamera.OBLIQUE_Y_SCALE)
      scale(zoom,zoom*ObliqueCamera.OBLIQUE_Y_SCALE,Offset.Zero)
    }) {
      if(clipToIsland) clipPath(island) { drawCasters(obstacles,camX,camY,halfWorldW,halfWorldH) }
      else clipRect(0f,0f,groundWidth,groundHeight) { drawCasters(obstacles,camX,camY,halfWorldW,halfWorldH) }
    }
  }
  private fun DrawScope.drawCasters(obstacles:List<WorldObstacle>,camX:Float,camY:Float,halfW:Float,halfH:Float) {
    val light=com.example.game.world.WorldEnvironment.lighting
    for(obs in obstacles) {
      val reach=500f*obs.scale*(1f+1f/SanctuaryLight.TAN_ELEV)+kotlin.math.abs(obs.elevationZ)/ObliqueCamera.OBLIQUE_Y_SCALE
      if(kotlin.math.abs(obs.x-camX)>halfW+reach || kotlin.math.abs(obs.y-camY)>halfH+reach) continue
      val g=geometry(obs) ?: continue
      projection.reset()
      projection.values[0]=g.spriteScale
      projection.values[4]=SanctuaryLight.SHADOW_DIR_X*g.height/(g.visibleHeight*SanctuaryLight.TAN_ELEV)
      projection.values[5]=SanctuaryLight.SHADOW_DIR_Y*g.height/(g.visibleHeight*SanctuaryLight.TAN_ELEV)
      projection.values[12]=obs.x
      projection.values[13]=obs.y+g.footOffset
      val rawOpacity=SanctuaryShadowRegistry.opacityFor(obs.propKind)*light.shadowStrength
      val deepOpacity=(rawOpacity * 1.68f).coerceAtMost(0.76f)
      withTransform({ transform(projection) }) {
        // Soft outer penumbra + deep obsidian-indigo cast shadow core
        if(DepthPresentation.quality==DepthPresentation.Quality.BALANCED) {
          withTransform({ scale(1.032f,1.022f,Offset.Zero) }) {
            drawPath(g.silhouette,Color(0xFF08101E).copy(alpha=(rawOpacity * 0.46f).coerceAtMost(0.32f)))
          }
        }
        drawPath(g.silhouette,Color(0xFF060C18).copy(alpha=deepOpacity*(if(g.tree) .90f else 1f)))
      }
      withTransform({ translate(obs.x,obs.y+g.footOffset) }) {
        withTransform({ scale(1.22f, 1.22f, Offset.Zero) }) {
          drawPath(g.contact,Color(0xFF070D19).copy(alpha=(if(g.tree) .34f else .28f)*light.shadowStrength))
        }
        drawPath(g.contact,Color(0xFF040811).copy(alpha=(if(g.tree) .58f else .48f)*light.shadowStrength))
      }
    }
  }
}
