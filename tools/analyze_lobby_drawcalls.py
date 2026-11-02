#!/usr/bin/env python3
"""Static draw-call model for the Sanctuary/Lobby terrain pass.

This is NOT a device profile. It mirrors, in Python, the exact tile-range math used by
`SanctuaryWorldRenderer.drawSanctuaryFloatingWorld` (grass base loop + cobblestone road
loop) so that the number of `drawImage` commands submitted per rendered frame can be
compared before/after an optimization without an Android device.

Source of truth for the numbers below:
  - app/src/main/java/com/example/game/map/WorldMapModels.kt  (ObliqueCamera)
  - app/src/main/java/com/example/game/pixel/SanctuaryWorldRenderer.kt (tile loops)
  - app/src/main/java/com/example/game/tower/TowerFloorCatalog.kt (SANCTUARY paths)

The road-coverage rule mirrors `SanctuaryRoadCoverage`: a 64x64 world tile is skipped
only when its centre is farther than segment_half_width + half tile diagonal from every
road segment, i.e. only when the whole tile square lies outside the cobble path.
"""

import math
import sys

WORLD_W = 3200.0
WORLD_H = 2760.0
WALL_MARGIN = 120.0
TILE = 64.0
OBLIQUE_Y = 0.72
GRASS_BLOCK_TILES = 4
GRASS_BLOCK_COUNT = 8
# Screen tile height below which the pre-composed 4x4 grass blocks are used.
GRASS_BLOCK_MAX_TILE_SCREEN_H = 25

# PathSegment(startX, startY, endX, endY, width, curb, inner)
ROAD_SEGMENTS = [
    (1560, 380, 1560, 665, 118),
    (1560, 665, 1330, 880, 118),
    (1330, 880, 1330, 1340, 118),
    (585, 1340, 585, 1150, 102),
    (585, 1150, 1165, 675, 102),
    (345, 1340, 1330, 1340, 112),
    (1330, 1340, 1545, 1735, 126),
    (1545, 1735, 1610, 2240, 136),
]


def segment_distance(px, py, ax, ay, bx, by):
    vx, vy = bx - ax, by - ay
    length_sq = vx * vx + vy * vy
    if length_sq <= 0.001:
        return math.hypot(px - ax, py - ay)
    t = max(0.0, min(1.0, ((px - ax) * vx + (py - ay) * vy) / length_sq))
    return math.hypot(px - (ax + t * vx), py - (ay + t * vy))


HALF_DIAGONAL = TILE * math.sqrt(2.0) / 2.0


def road_clearance(tx, ty):
    """Signed distance from the tile centre to the nearest road edge (negative = inside)."""
    cx = tx * TILE + TILE / 2.0
    cy = ty * TILE + TILE / 2.0
    return min(segment_distance(cx, cy, x1, y1, x2, y2) - width * 0.5
               for x1, y1, x2, y2, width in ROAD_SEGMENTS)


def tile_is_road(tx, ty, extra_world=0.0):
    """Conservative coverage: True => the tile may contain road/curb pixels, keep drawing."""
    return road_clearance(tx, ty) <= HALF_DIAGONAL + extra_world


def visible_tile_range(viewport_w, viewport_h, cam_x, cam_y, zoom):
    half_w = viewport_w / 2.0
    half_h = viewport_h / 2.0
    min_wx = max((0 - half_w) / zoom + cam_x, 240.0)
    max_wx = min((viewport_w - half_w) / zoom + cam_x, 2960.0)
    min_wy = max((0 - half_h) / (zoom * OBLIQUE_Y) + cam_y, 280.0)
    max_wy = min((viewport_h - half_h) / (zoom * OBLIQUE_Y) + cam_y, 2420.0)
    start_tx = max(int(min_wx / TILE) - 1, 3)
    end_tx = min(int(max_wx / TILE) + 1, 47)
    start_ty = max(int(min_wy / TILE) - 1, 4)
    end_ty = min(int(max_wy / TILE) + 1, 38)
    return start_tx, end_tx, start_ty, end_ty


def model(viewport_w, viewport_h, cam_x, cam_y, zoom, optimized):
    stx, etx, sty, ety = visible_tile_range(viewport_w, viewport_h, cam_x, cam_y, zoom)
    cols = max(0, etx - stx + 1)
    rows = max(0, ety - sty + 1)

    tile_screen_h = int(TILE * OBLIQUE_Y * zoom)
    if optimized and tile_screen_h < GRASS_BLOCK_MAX_TILE_SCREEN_H:
        bx0, bx1 = stx // GRASS_BLOCK_TILES, etx // GRASS_BLOCK_TILES
        by0, by1 = sty // GRASS_BLOCK_TILES, ety // GRASS_BLOCK_TILES
        grass = (bx1 - bx0 + 1) * (by1 - by0 + 1)
        grass_mode = "4x4 blocks"
    else:
        grass = cols * rows
        grass_mode = "per tile"

    r_start_tx = max(stx, 6)
    r_end_tx = min(etx, 39)
    r_start_ty = max(sty, 5)
    r_end_ty = min(ety, 36)
    road = 0
    for ty in range(r_start_ty, r_end_ty + 1):
        for tx in range(r_start_tx, r_end_tx + 1):
            if tx * TILE > 2460.0:
                continue
            if optimized and not tile_is_road(tx, ty):
                continue
            road += 1

    # Road verge ring: drawn over the whole visible rectangle, then almost entirely clipped
    # away by the road clip path (SanctuaryWorldRenderer.drawMaterialBand call site).
    verge = cols * rows
    if optimized:
        verge = sum(
            1
            for ty in range(sty, ety + 1)
            for tx in range(stx, etx + 1)
            # 7.5 world px is the widest curb union the band can reach.
            if tile_is_road(tx, ty, extra_world=7.5)
        )

    return {
        "zoom": zoom,
        "grass": grass,
        "grass_mode": grass_mode,
        "road": road,
        "verge": verge,
        "tiles_total": grass + road + verge,
        "tile_screen_h": tile_screen_h,
    }


CASES = [
    ("overview fit (island wide)", 1080, 2400, 1600, 1320, 0.27),
    ("zoomed out", 1080, 2400, 1600, 1320, 0.36),
    ("mid zoom", 1080, 2400, 1600, 1320, 0.50),
    ("default gameplay zoom", 1080, 2400, 1600, 1320, 0.88),
    ("close inspection", 1080, 2400, 1600, 1320, 1.20),
    ("720x1600 default zoom", 720, 1600, 1600, 1320, 0.88),
]


def main():
    fps = 30
    print("Sanctuary/Lobby terrain drawImage model — per rendered frame")
    print("(static model of the Kotlin loops; not a device measurement)\n")
    header = f"{'case':27} {'zoom':>5} {'grass':>6} {'road':>5} {'verge':>6} {'total':>6} {'after':>6} {'saved':>6} {'%':>6}"
    print(header)
    print("-" * len(header))
    for name, vw, vh, cx, cy, zoom in CASES:
        before = model(vw, vh, cx, cy, zoom, optimized=False)
        after = model(vw, vh, cx, cy, zoom, optimized=True)
        saved = before["tiles_total"] - after["tiles_total"]
        pct = (saved / before["tiles_total"] * 100.0) if before["tiles_total"] else 0.0
        print(f"{name:27} {zoom:5.2f} {before['grass']:6d} {before['road']:5d} "
              f"{before['verge']:6d} {before['tiles_total']:6d} {after['tiles_total']:6d} "
              f"{saved:6d} {pct:5.1f}%")
        print(f"{'':27} {'':5} grass {before['grass_mode']} -> {after['grass_mode']}"
              f"  (one tile is {before['tile_screen_h']}px tall on screen)")
    print()
    print(f"drawImage submissions at {fps} FPS, default gameplay zoom (1080x2400):")
    b = model(1080, 2400, 1600, 1320, 0.88, optimized=False)["tiles_total"]
    a = model(1080, 2400, 1600, 1320, 0.88, optimized=True)["tiles_total"]
    print(f"  before: {b * fps:,}/s   after: {a * fps:,}/s")
    b = model(1080, 2400, 1600, 1320, 0.27, optimized=False)["tiles_total"]
    a = model(1080, 2400, 1600, 1320, 0.27, optimized=True)["tiles_total"]
    print(f"drawImage submissions at {fps} FPS, island overview (1080x2400):")
    print(f"  before: {b * fps:,}/s   after: {a * fps:,}/s")
    return 0


if __name__ == "__main__":
    sys.exit(main())
