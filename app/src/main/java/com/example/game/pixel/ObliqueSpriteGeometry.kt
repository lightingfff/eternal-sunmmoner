package com.example.game.pixel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.game.map.PropKind

/** Upright sprite pixels retain vertical height; only world-ground Y is foreshortened by the camera. */
internal object ObliqueSpriteGeometry {
  data class Bounds(val top: Int, val bottom: Int, val imageHeight: Int) {
    val bottomPadding get() = imageHeight - 1 - bottom
    val visibleHeight get() = bottom - top + 1
  }
  private val cache = mutableMapOf<ImageBitmap, Bounds>()
  fun bounds(image: ImageBitmap): Bounds = cache.getOrPut(image) {
    val b = image.asAndroidBitmap()
    val pixels = IntArray(b.width * b.height)
    b.getPixels(pixels,0,b.width,0,0,b.width,b.height)
    var top = b.height - 1; var bottom = 0
    for (y in 0 until b.height) for (x in 0 until b.width) {
      if (pixels[y*b.width+x] ushr 24 >= 128) { top = minOf(top,y); bottom = maxOf(bottom,y) }
    }
    Bounds(top,bottom,b.height)
  }
  fun isTree(kind: PropKind) = when(kind) {
    PropKind.OAK_TREE, PropKind.MYSTIC_PLUM_TREE, PropKind.PINE_SPIRIT_TREE,
    PropKind.SAKURA_BLOSSOM_TREE, PropKind.CYPRESS_CONIFER_TREE -> true
    else -> false
  }
  fun pixelScale(kind: PropKind) = CompletionInteriorAtlas.entry(kind)?.pixelScale ?: when(kind) {
    PropKind.CRIMSON_CRYSTAL, PropKind.TATTERED_BANNER -> 1f
    PropKind.OAK_TREE, PropKind.MYSTIC_PLUM_TREE -> 2.05f
    PropKind.GARDEN_HEDGE_BUSH -> 1.9f
    PropKind.FLOWER_PLANTER_BED -> 1f
    else -> 2f
  }
  fun footOffset(kind: PropKind) = when(kind) {
    PropKind.OAK_TREE, PropKind.MYSTIC_PLUM_TREE, PropKind.PINE_SPIRIT_TREE,
    PropKind.SOUTHERN_GATE_TOWER, PropKind.FLOWER_PLANTER_BED -> 8f
    PropKind.CYPRESS_CONIFER_TREE, PropKind.GARDEN_HEDGE_BUSH, PropKind.FOREST_CANOPY_BUSH,
    PropKind.OBSIDIAN_COLUMN, PropKind.RUINED_PILLAR, PropKind.MOSSY_BOULDER, PropKind.STREET_LANTERN,
    PropKind.BARREL_CLUSTER, PropKind.WOODEN_BENCH_34, PropKind.STONE_BALUSTRADE_RAIL,
    PropKind.STONE_RETAINING_WALL, PropKind.TERRACE_CLIFF_WALL, PropKind.TRAINING_STRIKING_POST,
    PropKind.TRAINING_DUMMY, PropKind.WEAPON_RACK, PropKind.ANCIENT_MONOLITH, PropKind.SUMMON_OBELISK,
    PropKind.CRIMSON_CRYSTAL, PropKind.TATTERED_BANNER -> 4f
    else -> 6f
  }
  // The existing building art/door locations are preserved. Trees are explicitly grounded at obs.y.
  fun drawFootOffset(kind: PropKind, image: ImageBitmap): Float =
    if (CompletionInteriorAtlas.entry(kind)?.image === image)
      (image.height - CompletionInteriorAtlas.entry(kind)!!.pivotY) * pixelScale(kind)
    else if (isTree(kind) || (kind == PropKind.ROYAL_STATUE && CompletionPropAtlas.image("royal_statue") != null))
      bounds(image).bottomPadding * pixelScale(kind) else footOffset(kind)
}
