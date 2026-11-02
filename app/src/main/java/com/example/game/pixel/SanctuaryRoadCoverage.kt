package com.example.game.pixel

import com.example.game.map.PathSegment
import kotlin.math.hypot

/**
 * Cached, CPU-side cobblestone coverage for the Sanctuary road network.
 *
 * The road pass used to submit every tile of the visible rectangle (`34 x 32 = 1088`
 * `drawImage` calls on a tall phone at the normal camera) and rely on the road clip path
 * to discard most of them. Each clipped draw still costs a Skia command plus a path-clip
 * test, which makes this one of the largest single sources of Lobby frame time and heat.
 *
 * A tile is skipped only when its whole 64 x 64 square provably lies outside the road:
 * its centre is farther than `segment width / 2 + half tile diagonal` from every segment.
 * Tiles that can contain a single cobble pixel are always kept, so the rendered image is
 * unchanged. The clearance grid is computed once per path list, then reused by identity.
 */
internal object SanctuaryRoadCoverage {
  const val TILE_WORLD = 64f
  const val GRID_COLS = 48
  const val GRID_ROWS = 40

  /** Half the diagonal of one tile: the farthest any tile pixel can be from its centre. */
  const val HALF_TILE_DIAGONAL = TILE_WORLD * 0.70710678f

  private const val CLEARANCE_CAP = 4096f

  internal var gridBuildCount = 0
    private set

  private var cachedPaths: List<PathSegment>? = null
  private var cachedClearance: FloatArray? = null

  /**
   * True => keep drawing this tile. False only when the entire tile square lies outside
   * every road segment expanded by [extraWorld] (curb band / verge ring width).
   */
  fun mayContainRoad(
    paths: List<PathSegment>,
    tileX: Int,
    tileY: Int,
    extraWorld: Float = 0f,
  ): Boolean {
    if (paths.isEmpty()) return true
    if (tileX < 0 || tileY < 0 || tileX >= GRID_COLS || tileY >= GRID_ROWS) return true
    val clearance = clearanceFor(paths)[tileY * GRID_COLS + tileX]
    return clearance <= HALF_TILE_DIAGONAL + extraWorld
  }

  /** Signed distance from the tile centre to the nearest road edge (negative = inside). */
  private fun clearanceFor(paths: List<PathSegment>): FloatArray {
    val cached = cachedClearance
    if (cached != null && cachedPaths === paths) return cached
    val clearance = FloatArray(GRID_COLS * GRID_ROWS)
    for (ty in 0 until GRID_ROWS) {
      val cy = ty * TILE_WORLD + TILE_WORLD * 0.5f
      for (tx in 0 until GRID_COLS) {
        val cx = tx * TILE_WORLD + TILE_WORLD * 0.5f
        var best = CLEARANCE_CAP
        for (seg in paths) {
          val candidate = distanceToSegment(cx, cy, seg) - seg.width * 0.5f
          if (candidate < best) best = candidate
        }
        clearance[ty * GRID_COLS + tx] = best
      }
    }
    cachedPaths = paths
    cachedClearance = clearance
    gridBuildCount++
    return clearance
  }

  private fun distanceToSegment(px: Float, py: Float, seg: PathSegment): Float {
    val vx = seg.endX - seg.startX
    val vy = seg.endY - seg.startY
    val lengthSq = vx * vx + vy * vy
    if (lengthSq <= 0.001f) return hypot(px - seg.startX, py - seg.startY)
    val t = (((px - seg.startX) * vx + (py - seg.startY) * vy) / lengthSq).coerceIn(0f, 1f)
    return hypot(px - (seg.startX + t * vx), py - (seg.startY + t * vy))
  }
}
