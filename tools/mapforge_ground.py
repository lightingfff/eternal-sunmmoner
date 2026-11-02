#!/usr/bin/env python3
"""MAP FORGE — P18 vertical slice + P19 terrain cohesion.

Scope (deliberately bounded):
  * grass_0..7      dusk lawn, 8 variants   (PixelTileAtlas.grass, drives the 4x4 blocks)
  * cobble_0..5     road stone, 6 variants  (PixelTileAtlas.cobble)
  * flagstone_0..7  plaza slab, 8 variants  (PixelTileAtlas.flagstone)
  * soil_0..5       packed training earth, 6 variants  (P19; PixelTileAtlas.sandTiles)
  * gravel_0..3     gravel-in-dirt band, 4 variants    (P19; the grass/sand transition band)
  * verge           lawn-to-path fringe, transparent clusters
  * oak             the plaza/verge tree prop, repainted in place

Still untouched in sanctuary_p03/: wood_0..5, pine, cypress, twisted, canopy_a/b, boulder,
pillar, barrels — same file names, same counts, same loader contract.

P19 rules on top of the P18 ones:
  * soil is built at three scales (broad earth masses / compacted patches / connected
    pebble-and-grit clusters) and never by sprinkling independent pixels;
  * gravel is authored as 2-5 pebble CLUSTERS with a contact shadow and an upper-left
    highlight, so it reads as small stones bedded in dirt, not polka dots;
  * cobble courses carry break stones that straddle the grout seam, which removes the
    full-width "brick row" bands visible at 1x;
  * every family is checked as a 5x5 tiled mosaic, not just as a centred preview.

Authoring rules followed here:
  * painted on a torus (every motif is stamped at 9 offsets) so tiles join without seams;
  * a shared 2 px perimeter between variants keeps family adjacency stable;
  * three value groups per material (shade / body / light) plus a restrained accent;
  * stones are irregular polygons with their own top face, rim light and contact shadow —
    no grid of identical rounded rectangles;
  * light comes from the upper left, matching SanctuaryLight, and nothing is pre-squashed
    (the engine applies the 0.72 oblique Y compression at draw time).

Run: python3 tools/mapforge_ground.py
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

import random

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/assets/sanctuary_p03"

# ---------------------------------------------------------------- palettes (value groups)
GRASS = {
    "shade": "#1d2b23", "body": "#28382c", "body_b": "#2f4231", "light": "#3c5438",
    "blade": "#43603c", "crown": "#5f7a4c", "dry": "#778456",
    "earth": "#3a3225", "earth_lt": "#4a4130", "moss": "#45603f", "moss_lt": "#5c7a4c",
}
COBBLE = {
    "mortar": "#1e262b", "mortar_lt": "#283136",
    "shade": "#33404b", "body": "#42505d", "body_b": "#4b5a68",
    "light": "#5d6d7b", "rim": "#7c8b96", "glint": "#93a1a8",
    "moss": "#415a44", "moss_lt": "#587150", "wear": "#6b6a58",
}
SOIL = {
    "deep": "#302a20", "shade": "#42382a", "body": "#51452f", "body_b": "#5a4d36",
    "light": "#63553c", "dry": "#6d5f45", "damp": "#3a3226",
    # embedded stone: cool grey so the arena rim shares the cobble vocabulary
    "stone": "#565849", "stone_b": "#63655a", "stone_lt": "#767a68", "stone_sh": "#2a2820",
    "grit": "#5a5140",
}
GRAVEL = {
    "deep": "#2b2620", "shade": "#403930", "body": "#4c4439", "body_b": "#564d40",
    "light": "#5a5346", "dry": "#655e4e", "damp": "#3c352c",
    "stone": "#454d54", "stone_b": "#525a61", "stone_lt": "#69717a", "stone_sh": "#22262b",
    "moss": "#3d5340", "moss_lt": "#4f6b4d",
}
FLAG = {
    "mortar": "#2a2f2c", "mortar_lt": "#3a3f38",
    "shade": "#4c524c", "body": "#6a6f64", "body_b": "#777c6e",
    "light": "#8b8f80", "rim": "#a3a693", "cool": "#585f60",
    "moss": "#43563f", "moss_lt": "#5a7049", "grit": "#8e8a72",
}


class Torus:
    """64x64 tile painted on a 192x192 torus so edge-crossing motifs stay connected."""

    def __init__(self, base):
        self.im = Image.new("RGBA", (192, 192), base)
        self.d = ImageDraw.Draw(self.im)

    def _offsets(self):
        for oy in (0, 64, 128):
            for ox in (0, 64, 128):
                yield ox, oy

    def poly(self, pts, fill):
        for ox, oy in self._offsets():
            self.d.polygon([(x + ox, y + oy) for x, y in pts], fill=fill)

    def line(self, pts, fill, width=1):
        for ox, oy in self._offsets():
            self.d.line([(x + ox, y + oy) for x, y in pts], fill=fill, width=width)

    def px(self, x, y, fill):
        self.poly([(x, y), (x + 1, y), (x + 1, y + 1), (x, y + 1)], fill)

    def image(self) -> Image.Image:
        return self.im.crop((64, 64, 128, 128))


def rng(seed: int):
    """Mersenne Twister with a fixed seed: stable output, and unlike a small LCG it does not
    put successive samples on visible diagonals."""
    r = random.Random(seed)
    return r.randrange


# ---------------------------------------------------------------- grass
def blade_fan(t: Torus, x, y, scale, r, lit=True):
    """One clump as a compact connected fan: root shadow, a 3 px base, 2-3 short blades.
    Blades stay short and thick (2 px at the root, tapering to 1 px) so the lawn reads as
    grass at 1x instead of rain streaks."""
    # soft halo: the clump sits in the lawn instead of being pasted on it
    t.poly([(x - 4, y + 2), (x - 3, y - 1), (x + 3, y - 1), (x + 4, y + 2), (x, y + 3)],
           GRASS["shade"])
    t.poly([(x - 3, y + 1), (x - 2, y - 1), (x + 2, y - 1), (x + 3, y + 1), (x, y + 2)],
           GRASS["body_b"])
    h = 2 + scale
    t.poly([(x - 2, y), (x - 1, y - h), (x, y - h + 1), (x + 1, y)], GRASS["blade"])
    t.line([(x + 1, y), (x + 2, y - h + 1)], GRASS["blade"], 1)
    if lit:
        t.px(x - 1, y - h, GRASS["crown"])
        if r(3) == 0:
            t.px(x, y - h - 1, GRASS["crown"])
    if r(5) == 0:
        t.px(x + 2, y - h + 1, GRASS["dry"])


def grass(v: int) -> Image.Image:
    t = Torus(GRASS["body"])
    r = rng(1100 + v * 37)
    # 1. broad value masses: the field must not read as one flat green
    for _ in range(7):
        x, y = r(64), r(64)
        w, h = 10 + r(12), 5 + r(7)
        shape = [(x - w, y), (x - w // 2, y - h), (x + w // 3, y - h + 1), (x + w, y + 1),
                 (x + w // 2, y + h // 2), (x - w // 2, y + h // 2 + 1)]
        t.poly(shape, GRASS["body_b"] if r(2) else GRASS["body"])
    # 2. darker troughs give the lawn depth without noise
    for _ in range(5):
        x, y = r(64), r(64)
        t.poly([(x - 12, y + 2), (x - 4, y - 2), (x + 6, y - 1), (x + 13, y + 3),
                (x + 2, y + 5), (x - 8, y + 5)], GRASS["shade"])
    # 3. worn earth scars, only in some variants so the family is not uniform
    if v % 3 != 2:
        for _ in range(1 + (v % 2)):
            x, y = r(52) + 6, r(52) + 6
            t.poly([(x - 7, y), (x - 3, y - 4), (x + 4, y - 3), (x + 8, y + 1),
                    (x + 3, y + 4), (x - 4, y + 4)], GRASS["earth"])
            t.line([(x - 4, y - 2), (x + 2, y - 1)], GRASS["earth_lt"], 1)
    # 4. moss islands (connected leaves, not confetti)
    for _ in range(2 + (v % 2)):
        x, y = r(56) + 4, r(56) + 4
        t.poly([(x - 4, y), (x - 2, y - 2), (x, y - 1), (x + 2, y - 3), (x + 4, y - 1),
                (x + 2, y + 2), (x - 2, y + 2)], GRASS["moss"])
        t.line([(x - 3, y - 1), (x, y)], GRASS["moss_lt"], 1)
    # 5. clumps: 9-12 per tile, phyllotaxis-free placement, sizes vary
    clumps = 10 + (v % 4)
    for i in range(clumps):
        x, y = r(60) + 2, r(60) + 2
        roll = (i * 7 + v * 3) % 10
        scale = 0 if roll < 3 else (1 if roll < 7 else 2)
        blade_fan(t, x, y, scale, r, lit=(x + y) % 3 != 0 and scale > 0)
    # 6. a few taller clumps give the tile a focal point without a light path
    for _ in range(3):
        blade_fan(t, r(56) + 4, r(56) + 4, 3, r, lit=True)
    return t.image()


# ---------------------------------------------------------------- masonry helpers
def stone(t: Torus, pts, palette, r, moss_chance=6, chip_chance=5):
    """One authored stone: contact shadow, body, upper-left rim, optional chip/moss."""
    t.poly(pts, palette["mortar"])
    inner = [(x, y + 1) for x, y in pts]
    t.poly(inner, palette["body"])
    # light face toward the upper left, darker lower-right plane
    t.line([(pts[0][0], pts[0][1] + 1), (pts[1][0], pts[1][1] + 1), (pts[2][0], pts[2][1] + 1)],
           palette["light"], 1)
    t.line([(pts[5][0], pts[5][1] - 1), (pts[6][0], pts[6][1] - 1), (pts[7][0], pts[7][1] - 1)],
           palette["shade"], 1)
    if r(6) == 0:
        cx = (pts[0][0] + pts[3][0]) // 2
        cy = (pts[0][1] + pts[4][1]) // 2
        t.px(cx, cy, palette["rim"])
        t.px(cx + 1, cy, palette["body_b"])
    if r(chip_chance) == 0:
        k = r(4)
        t.poly([(pts[k][0], pts[k][1]), (pts[k][0] + 2, pts[k][1] + 1), (pts[k][0] + 1, pts[k][1] + 3)],
               palette["shade"])
    if r(moss_chance) == 0:
        mx = (pts[4][0] + pts[5][0]) // 2
        my = (pts[4][1] + pts[5][1]) // 2
        t.poly([(mx - 3, my), (mx - 1, my - 2), (mx + 2, my - 1), (mx + 3, my + 1),
                (mx, my + 2), (mx - 2, my + 1)], palette["moss"])
        t.line([(mx - 2, my - 1), (mx + 1, my)], palette["moss_lt"], 1)


def cobble(v: int) -> Image.Image:
    t = Torus(COBBLE["mortar"])
    r = rng(2200 + v * 53)
    # grit and moss settle in the mortar, not on the stone faces
    for _ in range(7):
        x, y = r(64), r(64)
        t.poly([(x - 6, y), (x - 2, y - 3), (x + 5, y - 2), (x + 7, y + 1),
                (x + 1, y + 3), (x - 4, y + 2)], COBBLE["mortar_lt"])
    y = -3 - (v * 2) % 7          # per-variant course phase kills the repeated seam rows
    row = 0
    bounds: list[int] = []
    while y < 64:
        row_h = 8 + r(5) + (1 if (row + v) % 4 == 0 else 0)
        x = -6 - r(10)
        while x < 64:
            # three size classes, staggered per row: no rigid grid, no identical dominoes
            w = (7 + r(4)) if r(3) == 0 else (11 + r(6))
            h = row_h - 1 - r(2)
            jitter = r(3) - 1
            x0, y0 = x, y + jitter
            x1, y1 = x0 + w, y0 + h
            pts = [
                (x0 + 1, y0 + 1), (x0 + w // 3, y0), (x1 - 2, y0 + 1), (x1, y0 + h // 3),
                (x1 - 1, y1 - 1), (x1 - w // 3, y1), (x0 + 1, y1 - 1), (x0, y0 + h // 2),
            ]
            stone(t, pts, COBBLE, r, moss_chance=8, chip_chance=5)
            x += w + 1 + r(2)
        bounds.append(y)
        y += row_h
        row += 1

    # P19: break stones that straddle a grout seam. A course line that runs the full tile
    # width is what made the P18 road read as brick; these taller stones cut the band into
    # short, uneven runs without changing stone count or road width.
    for _ in range(3 + (v % 2)):
        by = bounds[1 + r(len(bounds) - 2)]
        bw = 9 + r(8)
        bh = 12 + r(5)
        bx = r(64)
        pts = [
            (bx + 2, by - bh // 2), (bx + bw // 3, by - bh // 2 - 2),
            (bx + bw - 3, by - bh // 2 + 1), (bx + bw, by),
            (bx + bw - 2, by + bh // 2), (bx + bw // 2, by + bh // 2 + 2),
            (bx + 2, by + bh // 2 - 1), (bx, by - 1),
        ]
        stone(t, pts, COBBLE, r, moss_chance=7, chip_chance=4)
        # a single chipped notch at the seam keeps the join readable
        if r(2):
            t.px(bx + bw // 2, by + bh // 2, COBBLE["shade"])

    # worn polish where traffic runs: short broken segments, and only in some variants
    if v % 2 == 0:
        by = 10 + (v * 11) % 40
        for seg in range(3):
            sx = r(52) + seg * 3
            t.line([(sx, by), (sx + 3 + r(4), by + (1 if seg % 2 else 0))], COBBLE["wear"], 1)
    return t.image()


def flagstone(v: int) -> Image.Image:
    t = Torus(FLAG["body"])
    r = rng(3300 + v * 71)
    for _ in range(4):
        x, y = r(64), r(64)
        t.poly([(x - 11, y), (x - 2, y - 6), (x + 9, y - 3), (x + 12, y + 3),
                (x + 1, y + 6), (x - 8, y + 4)], FLAG["shade"])
    # large offset slabs: 3-4 courses, joins deliberately off-grid
    courses = [(0, 20), (20, 22), (42, 22)]
    for row, (y0, h) in enumerate(courses):
        x = -(6 + (row * 13 + v * 7) % 17)
        while x < 64:
            w = 20 + r(13) - (row * 2)
            x1 = x + w
            y1 = y0 + h - 2 - r(2)
            notch = 2 + r(2)
            pts = [
                (x + notch, y0 + 1), (x1 - notch - 1, y0 + 1), (x1, y0 + notch),
                (x1 - 1, y1 - notch), (x1 - notch, y1), (x + notch - 1, y1),
                (x, y1 - notch), (x + 1, y0 + notch + 1),
            ]
            tone = dict(FLAG)
            if (row * 5 + v + r(3)) % 3 == 0:
                tone["body"] = FLAG["body_b"]
                tone["light"] = FLAG["rim"]
            elif (row + v) % 5 == 0:
                tone["body"] = FLAG["cool"]
                tone["light"] = FLAG["body"]
            stone(t, pts, tone, r, moss_chance=9, chip_chance=2)
            # 1 px inner bevel so each slab reads as a raised plane
            t.line([(pts[0][0] + 1, pts[0][1] + 1), (pts[1][0], pts[1][1] + 1),
                    (pts[2][0] - 1, pts[2][1] + 1)], tone["rim"], 1)
            # mineral mottling inside the slab (cluster, not noise)
            for _ in range(3):
                mx, my = x + 4 + r(max(3, w - 8)), y0 + 4 + r(max(3, h - 8))
                t.line([(mx, my), (mx + 2, my - 1), (mx + 4, my)], FLAG["body_b"], 1)
                t.line([(mx + 1, my + 1), (mx + 3, my + 1)], FLAG["shade"], 1)
            if v in (2, 5) and r(2) == 0:
                # one restrained engraved mark per eligible variant
                gx, gy = x + w // 2, y0 + h // 2
                t.line([(gx - 3, gy), (gx, gy - 3), (gx + 3, gy), (gx, gy + 3), (gx - 3, gy)],
                       FLAG["cool"], 1)
            x += w + 3 + r(1)
    return t.image()


# ---------------------------------------------------------------- earth materials (P19)
def pebble(t: Torus, x, y, w, h, pal, r, lit=True):
    """One small stone, 3-7 px wide: contact shadow below-right, body, upper-left rim."""
    t.poly([(x - w + 1, y - h + 1), (x + w, y - h + 2), (x + w, y + h), (x - w + 1, y + h + 1)],
           pal["stone_sh"])
    t.poly([(x - w, y - h), (x + w - 1, y - h + 1), (x + w, y + h - 1), (x - w + 1, y + h)],
           pal["stone"])
    if lit:
        t.line([(x - w + 1, y - h), (x + w - 2, y - h + 1)], pal["stone_lt"], 1)
    if r(3) == 0:
        t.px(x, y + h - 1, pal["stone_b"])


def pebble_cluster(t: Torus, x, y, count, pal, r, spread=6):
    """2-5 stones sharing one patch of disturbed ground: a cluster, not scattered dots."""
    # disturbed earth ring under the cluster so the stones are bedded, not pasted
    t.poly([(x - spread + 1, y + 2), (x - spread // 2, y - 2), (x + spread // 2, y - 1),
            (x + spread - 1, y + 2), (x, y + 3)], pal["shade"])
    for _ in range(count):
        px_ = x + r(spread * 2) - spread
        py = y + r(spread) - spread // 2
        pebble(t, px_, py, 1 + r(3), 1 + r(2), pal, r, lit=(px_ + py) % 4 != 0)


def soil(v: int) -> Image.Image:
    """Packed training earth: three value scales (masses / compaction / clusters)."""
    darker = v % 3 == 2
    t = Torus(SOIL["damp"] if darker else SOIL["body"])
    r = rng(5500 + v * 61)

    # scale 1 - broad earth masses, one value step apart, never harsh
    for _ in range(5):
        x, y = r(64), r(64)
        w, h = 13 + r(13), 7 + r(7)
        # organic 10-gon: a hand-shaped earth lobe, not a stamped triangle
        pts = []
        for k in range(10):
            ang = k * 6.28318 / 10
            import math as _m
            wob = 1.0 + ((k * 5 + v * 3) % 7 - 3) * 0.07
            pts.append((x + w * wob * _m.cos(ang), y + h * 0.82 * wob * _m.sin(ang)))
        t.poly([(round(a), round(b)) for a, b in pts],
               SOIL["shade"] if (darker and r(3) == 0) else SOIL["body_b"])

    # a trodden route: wide, very low-contrast compaction band (traffic, not paint)
    if v % 2 == 0:
        by = 12 + r(38)
        t.poly([(0, by - 5), (18, by - 7), (40, by - 4), (64, by - 6),
                (64, by + 4), (42, by + 5), (18, by + 3), (0, by + 5)], SOIL["light"])
        for _ in range(6):
            sx = r(64)
            t.line([(sx, by - 3 + r(7)), (sx + 4 + r(5), by - 3 + r(7))], SOIL["dry"], 1)

    # scale 2 - compacted patches: grit and tiny chips in one small area
    for _ in range(3 + (v % 3)):
        x, y = r(56) + 4, r(56) + 4
        t.poly([(x - 6, y), (x - 2, y - 4), (x + 5, y - 2), (x + 7, y + 1),
                (x + 1, y + 4), (x - 4, y + 3)], SOIL["body_b"] if not darker else SOIL["shade"])
        for _ in range(2 + r(3)):
            t.px(x - 4 + r(10), y - 2 + r(6), SOIL["stone_b"] if r(2) else SOIL["light"])

    # shallow dry cracks: rare, 1 px, broken - the "used ground" read
    for _ in range(1 + (v % 2)):
        cx, cy = r(40) + 12, r(40) + 12
        t.line([(cx, cy), (cx + 4, cy + 2), (cx + 7, cy + 1), (cx + 11, cy + 3)], SOIL["deep"], 1)
        t.line([(cx + 7, cy + 1), (cx + 9, cy - 2)], SOIL["deep"], 1)
        t.line([(cx + 1, cy + 3), (cx + 6, cy + 4), (cx + 10, cy + 4)], SOIL["shade"], 1)

    # scale 3 - embedded stones as connected clusters with contact shadow and lit rim
    for _ in range(4 + (v % 3)):
        pebble_cluster(t, r(56) + 4, r(56) + 4, 2 + r(4), SOIL, r, spread=6)

    # fine grain: short 2-3 px runs of one low-contrast value, never bright confetti
    for _ in range(20):
        x, y = r(63), r(63)
        t.px(x, y, SOIL["grit"] if r(2) else SOIL["shade"])
        if r(2):
            t.px(x + 1, y + r(2), SOIL["shade"] if r(2) else SOIL["grit"])
    return t.image()


def gravel(v: int) -> Image.Image:
    """Gravel bedded in dirt: cohesive 2-5 pebble clusters, shared cobble stone values."""
    t = Torus(GRAVEL["body"])
    r = rng(6600 + v * 47)

    # broad dirt masses keep the band from reading as a flat grey carpet
    for _ in range(5):
        x, y = r(64), r(64)
        w, h = 12 + r(14), 6 + r(7)
        t.poly([(x - w, y + 1), (x - w // 2, y - h), (x + w // 3, y - h + 2), (x + w, y),
                (x + w // 2, y + h), (x - w // 2, y + h - 1)],
               GRAVEL["body_b"] if r(2) else GRAVEL["shade"])

    # loose grit fields (scale 2), confined to small patches
    for _ in range(3):
        x, y = r(52) + 6, r(52) + 6
        t.poly([(x - 8, y), (x - 3, y - 5), (x + 6, y - 3), (x + 9, y + 1),
                (x + 2, y + 5), (x - 5, y + 4)], GRAVEL["damp"])
        for _ in range(4 + r(4)):
            t.px(x - 6 + r(14), y - 3 + r(8), GRAVEL["light"] if r(2) else GRAVEL["body_b"])

    # scale 3 - the actual gravel: 6-8 clusters, density deliberately restrained so tiny
    # hero feet are not swallowed at 0.88
    for _ in range(6 + (v % 3)):
        pebble_cluster(t, r(56) + 4, r(56) + 4, 2 + r(4), GRAVEL, r, spread=5)

    # moss only in the two dampest variants, in the gaps
    if v % 2:
        for _ in range(2):
            x, y = r(56) + 4, r(56) + 4
            t.poly([(x - 3, y), (x - 1, y - 2), (x + 2, y - 1), (x + 3, y + 1),
                    (x, y + 2), (x - 2, y + 1)], GRAVEL["moss"])
            t.px(x, y - 1, GRAVEL["moss_lt"])
    return t.image()


def verge() -> Image.Image:
    """Transparent lawn-to-path fringe: tufts intruding onto stone plus a few grit pixels."""
    t = Torus((0, 0, 0, 0))
    r = rng(4400)
    for i in range(16):
        x, y = r(64), r(64)
        blade_fan(t, x, y, 1 if i % 4 else 2, r, lit=True)
    for _ in range(10):
        x, y = r(64), r(64)
        t.px(x, y, GRASS["earth"])
        t.px(x + 1, y, GRASS["earth_lt"])
    return t.image()


# ---------------------------------------------------------------- oak prop
def oak() -> Image.Image:
    """Same canvas (112x136) and pivot as the existing oak, repainted with real mass."""
    im = Image.new("RGBA", (112, 136), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    BARK = "#3b3327"
    BARK_LT = "#6a5a42"
    BARK_HI = "#8d7852"
    ROOT = "#2b2a22"
    LEAF = ["#16241f", "#1f3529", "#2b4433", "#3a563f", "#4f6a4a", "#6b8258"]

    # trunk: buttress roots, tapering bole, two visible branch forks
    d.polygon([(40, 134), (34, 126), (44, 118), (46, 96), (42, 78), (50, 60), (54, 52),
               (60, 58), (62, 78), (66, 96), (69, 118), (78, 126), (72, 134), (58, 130),
               (50, 134)], fill=ROOT)
    d.polygon([(46, 130), (48, 116), (50, 98), (48, 80), (54, 62), (58, 60), (57, 82),
               (59, 100), (60, 118), (64, 128), (56, 126), (52, 130)], fill=BARK)
    d.line([(54, 124), (55, 104), (54, 86), (57, 68)], fill=BARK_LT, width=2)
    d.line([(49, 120), (50, 100), (49, 84)], fill="#2e2a21", width=1)
    d.line([(60, 118), (60, 100), (58, 84)], fill="#2e2a21", width=1)
    d.line([(56, 92), (58, 90), (59, 86)], fill=BARK_HI, width=1)
    # branch forks reaching into the canopy
    d.line([(55, 76), (44, 58), (36, 46)], fill=BARK, width=3)
    d.line([(57, 78), (70, 60), (78, 44)], fill=BARK, width=3)
    d.line([(45, 57), (40, 50)], fill=BARK_LT, width=1)

    # canopy: five masses, upper-left lit, lower-right in shadow, dark underside band
    masses = [(38, 46, 24), (72, 44, 22), (54, 30, 28), (34, 26, 19), (76, 24, 18)]
    for ci, (cx, cy, rad) in enumerate(masses):
        outline = []
        steps = 14
        for i in range(steps):
            ang = i * 2 * 3.14159 / steps
            wobble = 1.0 + ((i * 7 + ci * 5) % 5 - 2) * 0.06
            outline.append((cx + rad * wobble * (1 if i < steps / 2 else 0.94) * __import__("math").cos(ang),
                            cy + rad * 0.78 * wobble * __import__("math").sin(ang)))
        d.polygon([(round(x), round(y)) for x, y in outline], fill=LEAF[1])
        # interior planes: shade low-right, body centre, light upper-left
        d.polygon([(round(cx - rad * 0.9), round(cy)),
                   (round(cx - rad * 0.4), round(cy - rad * 0.7)),
                   (round(cx + rad * 0.35), round(cy - rad * 0.55)),
                   (round(cx + rad * 0.1), round(cy + rad * 0.1)),
                   (round(cx - rad * 0.5), round(cy + rad * 0.35))], fill=LEAF[2])
        d.polygon([(round(cx - rad * 0.6), round(cy - rad * 0.1)),
                   (round(cx - rad * 0.2), round(cy - rad * 0.65)),
                   (round(cx + rad * 0.3), round(cy - rad * 0.45)),
                   (round(cx + rad * 0.05), round(cy - rad * 0.05)),
                   (round(cx - rad * 0.35), round(cy + rad * 0.1))], fill=LEAF[3])
        d.polygon([(round(cx - rad * 0.45), round(cy - rad * 0.2)),
                   (round(cx - rad * 0.15), round(cy - rad * 0.55)),
                   (round(cx + rad * 0.18), round(cy - rad * 0.38)),
                   (round(cx - rad * 0.05), round(cy - rad * 0.12))], fill=LEAF[4])
        # leaf plates on the crown edge
        for k in range(3):
            lx = cx - rad * 0.35 + k * rad * 0.34
            ly = cy - rad * 0.52 + (k % 2) * 3
            d.polygon([(round(lx), round(ly)), (round(lx + 4), round(ly - 3)),
                       (round(lx + 8), round(ly)), (round(lx + 4), round(ly + 2))], fill=LEAF[5])
        # underside shadow band, keeps the mass sitting on the branch
        d.polygon([(round(cx - rad * 0.8), round(cy + rad * 0.28)),
                   (round(cx), round(cy + rad * 0.55)),
                   (round(cx + rad * 0.8), round(cy + rad * 0.22))], fill=LEAF[0])
    return im


# ---------------------------------------------------------------- export
def main():
    manifest_path = OUT / "manifest.json"
    manifest = json.loads(manifest_path.read_text())
    by_id = {entry["id"]: entry for entry in manifest["assets"]}

    written = []
    # Shared 2 px perimeter keeps family adjacency stable for the structured materials.
    # Earth materials deliberately opt out: their low-frequency masses do not match the
    # reference border, and the copy paints a visible frame around every soil/gravel tile.
    families = [
        ("grass", 8, grass, True), ("cobble", 6, cobble, True), ("flagstone", 8, flagstone, True),
        ("soil", 6, soil, False), ("gravel", 4, gravel, False),
    ]
    for name, count, fn, shared in families:
        reference = fn(0)
        for v in range(count):
            image = fn(v)
            if shared:
                for box in [(0, 0, 64, 2), (0, 62, 64, 64), (0, 0, 2, 64), (62, 0, 64, 64)]:
                    image.paste(reference.crop(box), box)
            path = OUT / f"{name}_{v}.png"
            image.save(path, optimize=True)
            written.append(path.name)

    for name, image in (("verge", verge()), ("oak", oak())):
        path = OUT / f"{name}.png"
        image.save(path, optimize=True)
        written.append(path.name)

    for filename in written:
        asset_id = filename[:-4]
        entry = by_id.get(asset_id)
        if entry is None:
            raise SystemExit(f"manifest has no entry for {asset_id}; refusing to invent a contract")
        entry["sha256"] = hashlib.sha256((OUT / filename).read_bytes()).hexdigest()
        with Image.open(OUT / filename) as im:
            entry["size"] = list(im.size)

    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
    print(json.dumps({"written": written, "count": len(written)}, indent=2))


if __name__ == "__main__":
    main()
