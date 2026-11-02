#!/usr/bin/env python3
"""MAP FORGE — P20 vertical slice: armory architecture + cypress vegetation.

Two hand-authored sprites, both replacing an existing asset in place so that every
world anchor, door trigger, footprint, depth-sort key and canopy-occlusion fraction keeps
working unchanged:

  * app/src/main/assets/premium_slice/armory.png   190x166  (PropKind.FACILITY_ARMORY,
    drawn by `drawCrispSprite` bottom-centred; the doorway keeps the exact pixel box
    x 133..150, y 109..139 that the original sprite used, so door world (993,673) still
    lands on the visual door)
  * app/src/main/assets/sanctuary_p03/cypress.png   48x88   (PropKind.CYPRESS_CONIFER_TREE,
    ground anchor [24,83], upper-canopy occlusion splits at topFraction 0.54)

Authoring rules:
  * one key light from the upper left, matching SanctuaryLight; no mirrored light;
  * roofs are real volumes: two planes, ridge cap, eave thickness and a dark underside band
    that falls on the wall underneath;
  * masonry blocks vary in size, carry an upper-left rim and a lower-right shaded face, with
    dark-but-not-black mortar; corner quoins and a darker foundation course;
  * windows get frames and mullions, restrained amber emission; the door is recessed with a
    stone surround, iron studs, threshold and warm spill;
  * foliage is 5-12 intentional clusters with cavities and a visible trunk, never noise dots;
  * nothing is generated at draw time: this script is offline, the engine only decodes PNGs.

Run: python3 tools/mapforge_architecture.py
"""

from __future__ import annotations

import hashlib
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
PREMIUM = ASSETS / "premium_slice"
SANCT = ASSETS / "sanctuary_p03"

# ------------------------------------------------------------------ palette (one family)
ARCH = {
    # graphite-blue stone, three faces + rim
    "mortar": "#232b31", "stone_dk": "#3a444c", "stone": "#4b565e",
    "stone_lt": "#5d6870", "stone_hi": "#77838c", "quoin": "#414c54",
    "found": "#333c43", "found_lt": "#48525a",
    # deep warm timber
    "wood_dk": "#2c2115", "wood": "#43301d", "wood_md": "#543d25", "wood_lt": "#6b4e2e",
    "wood_hi": "#87653c",
    # slate (right roof plane / ridge caps)
    "slate_dk": "#28313a", "slate": "#36424c", "slate_lt": "#48565f", "slate_hi": "#5c6b73",
    # moss + accents
    "moss": "#3f5540", "moss_lt": "#55704e",
    "iron": "#242a30", "iron_lt": "#5a636c",
    "glow": "#f0b25a", "glow_lt": "#ffd692", "glow_dim": "#b8803c",
    "shadow": "#12171b", "ao": "#1a2126",
}

VEG = {
    "trunk_dk": "#2b2118", "trunk": "#3d3022", "trunk_lt": "#5a4732", "trunk_hi": "#76603f",
    "cav": "#0f1a15", "deep": "#17251d", "shade": "#1f3227", "body": "#2a4132",
    "body_lt": "#35503c", "lit": "#435f48", "lit_hi": "#577a56", "dry": "#6b8258",
}


def rng(seed: int):
    r = random.Random(seed)
    return r.randrange


# ------------------------------------------------------------------ drawing helpers
class Canvas:
    def __init__(self, w: int, h: int):
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)
        self.w, self.h = w, h

    def poly(self, pts, fill):
        self.d.polygon([(round(x), round(y)) for x, y in pts], fill=fill)

    def rect(self, x0, y0, x1, y1, fill):
        self.d.rectangle([x0, y0, x1, y1], fill=fill)

    def line(self, pts, fill, width=1):
        self.d.line([(round(x), round(y)) for x, y in pts], fill=fill, width=width)

    def px(self, x, y, fill):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.im.putpixel((x, y), ImageColor_get(fill))


def ImageColor_get(c):
    return c if isinstance(c, tuple) else tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def rows_fill(c: Canvas, quad, top, bottom, ramp, seed, shingle_w=11, seam=ARCH["wood_dk"]):
    """Fill a roof plane with staggered shingle rows between two edges of `quad`.

    `quad` is [tl, tr, br, bl]; rows run tl->tr, stepping toward bl->br. Each shingle gets a
    one-step tone from `ramp`, a dark seam under it and a 1 px lit top edge, so the plane
    reads as tiles rather than a gradient.
    """
    r = rng(seed)
    tl, tr, br, bl = quad
    n = 7
    for i in range(n):
        t0, t1 = i / n, (i + 1) / n
        a = (tl[0] + (bl[0] - tl[0]) * t0, tl[1] + (bl[1] - tl[1]) * t0)
        b = (tr[0] + (br[0] - tr[0]) * t0, tr[1] + (br[1] - tr[1]) * t0)
        e = (tl[0] + (bl[0] - tl[0]) * t1, tl[1] + (bl[1] - tl[1]) * t1)
        f = (tr[0] + (br[0] - tr[0]) * t1, tr[1] + (br[1] - tr[1]) * t1)
        c.poly([a, b, f, e], top)
        # shingles: alternating widths, staggered offset per row
        x = a[0] - (0 if i % 2 else shingle_w // 2)
        width_total = b[0] - a[0]
        while x < b[0]:
            w = shingle_w - 2 + r(5)
            x0 = max(x, a[0])
            x1 = min(x + w, b[0])
            if x1 - x0 > 2:
                tone = ramp[1] if r(3) else (ramp[2] if r(2) else ramp[0])
                t0f = (x0 - a[0]) / max(1.0, width_total)
                t1f = (x1 - a[0]) / max(1.0, width_total)
                p0 = (a[0] + (b[0] - a[0]) * t0f, a[1] + (b[1] - a[1]) * t0f)
                p1 = (a[0] + (b[0] - a[0]) * t1f, a[1] + (b[1] - a[1]) * t1f)
                q0 = (e[0] + (f[0] - e[0]) * t0f, e[1] + (f[1] - e[1]) * t0f)
                q1 = (e[0] + (f[0] - e[0]) * t1f, e[1] + (f[1] - e[1]) * t1f)
                c.poly([p0, p1, q1, q0], tone)
                c.line([q0, q1], seam, 1)
            x += w + 1
        c.line([e, f], seam, 1)
    # lower edge of the plane is the eave: give it thickness + a dark underside
    c.line([bl, br], ramp[2], 2)


# ------------------------------------------------------------------ armory
DOOR_X0, DOOR_X1 = 133, 150
DOOR_Y0, DOOR_Y1 = 109, 139

# Geometry, fixed once so every plane closes on its neighbour.
ROOF_OUTLINE = [(14, 96), (58, 30), (150, 16), (186, 84), (99, 112)]
RIDGE = ((58, 30), (150, 16))
RETURN_SPLIT = ((150, 16), (99, 112))          # ridge end -> front corner
LEFT_WALL = [(20, 100), (97, 110), (97, 162), (20, 152)]
RIGHT_WALL = [(97, 110), (176, 92), (176, 140), (97, 162)]


def _lerp(a, b, t):
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)


def _row_band(c: Canvas, a, b, t0, t1):
    return [_lerp(a[0], b[0], t0) if False else (0, 0)]


def _shingle_plane(c: Canvas, eave_l, eave_r, ridge_l, ridge_r, ramp, seam, seed,
                   rows=7, shingle=12):
    """Paint a roof plane between an eave edge and a ridge edge, in staggered courses."""
    r = rng(seed)
    for i in range(rows):
        t0, t1 = i / rows, (i + 1) / rows
        e0, e1 = _lerp(eave_l, ridge_l, t0), _lerp(eave_r, ridge_r, t0)
        f0, f1 = _lerp(eave_l, ridge_l, t1), _lerp(eave_r, ridge_r, t1)
        c.poly([e0, e1, f1, f0], ramp[1])
        span = e1[0] - e0[0]
        x = e0[0] - (0 if i % 2 else shingle // 2)
        while x < e0[0] + span:
            w = shingle - 3 + r(6)
            u0 = max(0.0, (x - e0[0]) / max(1.0, span))
            u1 = min(1.0, (x + w - e0[0]) / max(1.0, span))
            if u1 - u0 > 0.04:
                p0 = _lerp(e0, e1, u0)
                p1 = _lerp(e0, e1, u1)
                q0 = _lerp(f0, f1, u0)
                q1 = _lerp(f0, f1, u1)
                roll = r(9)
                tone = ramp[0] if roll == 0 else (ramp[2] if roll < 4 else ramp[1])
                c.poly([p0, p1, q1, q0], tone)
                c.line([p0, p1], ramp[2], 1)        # lit upper lip of the shingle
                c.line([q0, q1], seam, 1)           # course seam under it
            x += w + 1
    # eave edge: thickness + the dark underside that reads as overhang
    c.line([eave_l, eave_r], ramp[0], 2)


def armory() -> Image.Image:
    c = Canvas(190, 166)
    r = rng(2020)

    # ---- wall volumes (key light upper-left: left face lit, right face shaded) -------
    c.poly(LEFT_WALL, ARCH["stone"])
    c.poly(RIGHT_WALL, ARCH["stone_dk"])

    def masonry(quad, base, lt, dk, seed, row_h=7, block_w=13):
        rr = rng(seed)
        tl, tr, br, bl = quad
        height = bl[1] - tl[1]
        row = 0
        y = 0.0
        while y < height - 1:
            h = row_h + rr(2)
            t0, t1 = y / height, min(1.0, (y + h) / height)
            a, b = _lerp(tl, bl, t0), _lerp(tr, br, t0)
            e, f = _lerp(tl, bl, t1), _lerp(tr, br, t1)
            c.poly([a, b, f, e], base)
            span = b[0] - a[0]
            x = -rr(10)
            while x < span:
                w = block_w - 4 + rr(8)
                x0, x1 = max(0.0, x), min(span, x + w)
                if x1 - x0 > 3:
                    u0, u1 = x0 / span, x1 / span
                    p0, p1 = _lerp(a, b, u0), _lerp(a, b, u1)
                    q0, q1 = _lerp(e, f, u0), _lerp(e, f, u1)
                    roll = rr(8)
                    tone = dk if roll == 0 else (lt if roll < 3 else base)
                    c.poly([p0, p1, q1, q0], tone)
                    c.line([p0, p1], lt, 1)                 # upper rim in the light
                    c.line([q0, q1], dk, 1)                 # lower face in shade
                    c.line([p1, q1], ARCH["mortar"], 1)     # vertical joint
                    if rr(5) == 0:
                        c.px(round(p1[0]) - 1, round(p1[1]) + 1, dk)
                x += w + 1
            c.line([e, f], ARCH["mortar"], 1)
            y += h
            row += 1

    masonry(LEFT_WALL, ARCH["stone"], ARCH["stone_lt"], ARCH["stone_dk"], 31)
    masonry(RIGHT_WALL, ARCH["stone_dk"], ARCH["stone"], ARCH["mortar"], 47)

    # front corner quoins: alternating long/short blocks tying the two faces together
    for i, yy in enumerate(range(108, 162, 9)):
        c.poly([(90, yy), (99, yy + 2), (99, yy + 9), (90, yy + 7)],
               ARCH["quoin"] if i % 2 else ARCH["stone_lt"])
        c.line([(90, yy), (99, yy + 2)], ARCH["stone_hi"], 1)
        c.line([(90, yy + 7), (99, yy + 9)], ARCH["stone_dk"], 1)

    # foundation course + contact AO, so nothing floats
    c.poly([(20, 148), (97, 158), (97, 162), (20, 152)], ARCH["found"])
    c.poly([(97, 158), (176, 136), (176, 140), (97, 162)], ARCH["found"])
    c.line([(20, 148), (97, 158)], ARCH["found_lt"], 1)
    c.line([(97, 158), (176, 136)], ARCH["found_lt"], 1)
    c.poly([(15, 158), (97, 165), (181, 143), (179, 139), (97, 161), (18, 152)], ARCH["ao"])

    # ---- roof: timber plane + slate return, both with real eave thickness ------------
    c.poly(ROOF_OUTLINE, ARCH["wood"])
    # underside band first (roof overhang shadow falling on the walls)
    c.poly([(18, 98), (99, 112), (184, 84), (184, 94), (99, 120), (16, 106)], ARCH["shadow"])

    # timber plane: eave edge -> ridge, courses parallel to the ridge
    _shingle_plane(c, (14, 96), (99, 112), (58, 30), (150, 16),
                   [ARCH["wood_dk"], ARCH["wood"], ARCH["wood_md"]], ARCH["wood_dk"], 7,
                   rows=8, shingle=13)
    # hip return on the right: triangle from the front eave corner up to the ridge end
    _shingle_plane(c, (99, 112), (186, 84), (150, 16), (150, 16),
                   [ARCH["slate_dk"], ARCH["slate"], ARCH["slate_lt"]], ARCH["slate_dk"], 11,
                   rows=8, shingle=7)

    # ridge cap and the crisp silhouette edges
    c.line([RIDGE[0], RIDGE[1]], ARCH["wood_hi"], 2)
    c.line([(58, 30), (14, 96)], ARCH["wood_md"], 1)
    c.line([(150, 16), (186, 84)], ARCH["slate_hi"], 1)
    c.line([(14, 96), (99, 112)], ARCH["wood_dk"], 2)
    c.line([(99, 112), (186, 84)], ARCH["slate_dk"], 2)
    c.line([RETURN_SPLIT[0], RETURN_SPLIT[1]], ARCH["wood_lt"], 1)

    # moss only in the damp lower-left pocket of the timber plane
    for _ in range(4):
        mx, my = 24 + r(24), 78 + r(12)
        c.poly([(mx, my), (mx + 3, my - 2), (mx + 6, my), (mx + 4, my + 2), (mx + 1, my + 1)],
               ARCH["moss"])
        c.px(mx + 2, my, ARCH["moss_lt"])

    # eave brackets: they make the overhang structurally believable
    for bx in (30, 56, 82):
        c.line([(bx, 96), (bx, 88)], ARCH["wood_dk"], 2)
        c.px(bx, 87, ARCH["wood_lt"])
    for bx in (124, 152, 174):
        c.line([(bx, 92), (bx, 84)], ARCH["slate_dk"], 2)

    # ---- chimney on the timber plane -------------------------------------------------
    c.rect(66, 20, 86, 60, ARCH["stone_dk"])
    c.rect(67, 21, 84, 58, ARCH["stone"])
    for yy in range(28, 58, 6):
        c.line([(67, yy), (84, yy)], ARCH["mortar"], 1)
        c.line([(67, yy + 1), (84, yy + 1)], ARCH["stone_lt"], 1)
    c.line([(83, 21), (83, 58)], ARCH["stone_dk"], 1)
    c.rect(63, 14, 89, 21, ARCH["stone_dk"])
    c.rect(64, 15, 87, 19, ARCH["stone_lt"])
    c.line([(64, 15), (87, 15)], ARCH["stone_hi"], 1)
    c.rect(68, 16, 83, 18, ARCH["shadow"])
    # chimney shadow cast down-right onto the roof plane
    c.poly([(86, 22), (96, 20), (100, 34), (88, 38)], ARCH["shadow"])

    # ---- windows ---------------------------------------------------------------------
    def window(x, y, w=13, h=16, lit=2, shutters=False):
        c.rect(x - 2, y - 2, x + w + 1, y + h + 1, ARCH["stone_dk"])
        c.rect(x - 1, y - 1, x + w, y + h, ARCH["stone_lt"])
        c.rect(x, y, x + w - 1, y + h - 1, ARCH["shadow"])
        if lit:
            c.rect(x + 1, y + 1, x + w - 2, y + h - 2, ARCH["glow_dim"])
            c.rect(x + 1, y + 2, x + w - 2, y + h - 4, ARCH["glow"])
            if lit > 1:
                c.rect(x + 2, y + 2, x + w - 4, y + h - 7, ARCH["glow_lt"])
            c.line([(x + 1, y + 1), (x + 1, y + h - 2)], ARCH["glow_dim"], 1)
        else:
            c.rect(x + 1, y + 1, x + w - 2, y + h - 2, "#242c33")
            c.line([(x + 2, y + 2), (x + w - 3, y + 2)], "#39424a", 1)
        c.line([(x + w // 2, y), (x + w // 2, y + h - 1)], ARCH["iron"], 1)
        c.line([(x, y + h // 2), (x + w - 1, y + h // 2)], ARCH["iron"], 1)
        c.line([(x - 2, y + h + 1), (x + w + 1, y + h + 1)], ARCH["stone_hi"], 1)
        c.line([(x - 2, y + h + 2), (x + w + 1, y + h + 2)], ARCH["stone_dk"], 1)
        if shutters:
            c.rect(x - 6, y - 1, x - 3, y + h, ARCH["wood_md"])
            c.rect(x + w + 1, y - 1, x + w + 4, y + h, ARCH["wood_dk"])
            for yy in range(y + 1, y + h, 4):
                c.line([(x - 5, yy), (x - 4, yy)], ARCH["wood_dk"], 1)
                c.line([(x + w + 2, yy), (x + w + 3, yy)], ARCH["wood_lt"], 1)

    window(32, 110, lit=2, shutters=True)
    window(62, 118, lit=1)

    # ---- door: fixed pixel box x 133..150, y 109..139 --------------------------------
    dx0, dx1, dy0, dy1 = DOOR_X0, DOOR_X1, DOOR_Y0, DOOR_Y1
    c.rect(dx0 - 4, dy0 - 7, dx1 + 4, dy1 + 2, ARCH["stone_dk"])
    c.rect(dx0 - 3, dy0 - 6, dx1 + 3, dy1 + 1, ARCH["stone_lt"])
    c.rect(dx0 - 5, dy0 - 9, dx1 + 5, dy0 - 6, ARCH["stone"])
    c.rect(dx0 + 1, dy0 - 8, dx0 + 9, dy0 - 6, ARCH["stone_hi"])
    c.line([(dx0 - 3, dy0 - 6), (dx0 - 3, dy1 + 1)], ARCH["stone_hi"], 1)
    c.rect(dx0, dy0, dx1 - 1, dy1, ARCH["shadow"])
    c.rect(dx0 + 1, dy0 + 1, dx1 - 2, dy1 - 1, ARCH["wood_dk"])
    c.rect(dx0 + 2, dy0 + 2, dx1 - 3, dy1 - 2, ARCH["wood"])
    for pxx in range(dx0 + 3, dx1 - 3, 4):
        c.line([(pxx, dy0 + 2), (pxx, dy1 - 2)], ARCH["wood_dk"], 1)
        c.line([(pxx + 1, dy0 + 3), (pxx + 1, dy1 - 3)], ARCH["wood_md"], 1)
    for yy in (dy0 + 6, dy0 + 19):
        c.line([(dx0 + 1, yy), (dx1 - 2, yy)], ARCH["iron"], 1)
        c.px(dx0 + 3, yy, ARCH["iron_lt"])
        c.px(dx0 + 9, yy, ARCH["iron_lt"])
    c.px(dx0 + 11, dy0 + 12, ARCH["iron_lt"])
    c.px(dx0 + 11, dy0 + 13, ARCH["iron"])
    c.line([(dx0 + 1, dy1 + 1), (dx1 - 2, dy1 + 1)], ARCH["glow_dim"], 1)
    c.px(dx0 + 5, dy1 + 2, ARCH["glow_dim"])
    c.rect(dx0 - 3, dy1 + 3, dx1 + 4, dy1 + 5, ARCH["stone_lt"])
    c.rect(dx0 - 4, dy1 + 6, dx1 + 5, dy1 + 8, ARCH["stone"])
    c.line([(dx0 - 4, dy1 + 6), (dx1 + 5, dy1 + 6)], ARCH["stone_hi"], 1)
    c.rect(dx0 - 5, dy1 + 9, dx1 + 6, dy1 + 10, ARCH["stone_dk"])

    # ---- handcrafted accents: hanging armory sign ------------------------------------
    c.line([(168, 96), (168, 102)], ARCH["iron"], 1)
    c.line([(160, 102), (170, 102)], ARCH["iron"], 1)
    c.rect(157, 103, 169, 113, ARCH["wood_md"])
    c.rect(158, 104, 168, 112, ARCH["wood_lt"])
    c.line([(158, 104), (168, 104)], ARCH["wood_hi"], 1)
    c.rect(161, 106, 166, 108, ARCH["iron"])       # anvil body
    c.rect(162, 109, 165, 110, ARCH["iron"])
    c.px(160, 106, ARCH["iron"])
    return c.im


# ------------------------------------------------------------------ cypress
def cypress() -> Image.Image:
    """48x88 conifer: 8 overlapping branch sprays, visible trunk, cavities between tiers.

    Each tier is a scalloped spray (jagged underside, tapered top) rather than a horizontal
    bar, so the silhouette reads as a tree and not as stacked boxes. Light stays upper-left:
    the left half of every spray carries the lit cluster, the right half sits in shade.
    """
    c = Canvas(48, 88)
    r = rng(5150)
    cx = 24

    # ---- trunk: tapered, bark strips, root flare ------------------------------------
    c.poly([(20, 86), (19, 79), (22, 56), (26, 56), (29, 79), (28, 86), (24, 87)], VEG["trunk_dk"])
    c.poly([(22, 85), (21, 78), (23, 58), (26, 59), (26, 85)], VEG["trunk"])
    c.line([(23, 83), (23, 62)], VEG["trunk_lt"], 1)
    c.line([(25, 82), (24, 63)], VEG["trunk_dk"], 1)
    c.px(24, 68, VEG["trunk_hi"])
    c.px(22, 74, VEG["trunk_hi"])
    for rx, ry in ((17, 86), (31, 86), (15, 84), (33, 83)):
        c.line([(rx, ry), (23 if rx < cx else 26, ry - 3)], VEG["trunk_dk"], 1)
        c.px(rx, ry - 1, VEG["trunk"])

    # ---- branch sprays ---------------------------------------------------------------
    tiers = [
        (4, 4), (10, 7), (17, 10), (24, 12), (31, 14), (38, 16), (46, 17), (54, 18), (62, 16),
    ]
    for i, (y, half) in enumerate(tiers):
        skew = (-1, 0, 1, 0)[i % 4]
        left = cx - half + skew
        right = cx + half + skew
        # scalloped underside: zigzag every 4 px
        bottom = []
        x = left
        down = True
        while x <= right:
            bottom.append((x, y + 7 + (2 if down else 0) + r(2)))
            x += 4
            down = not down
        spray = [(left, y + 7), (left + half * 0.45, y + 1), (cx, y - 1),
                 (right - half * 0.45, y + 1), (right, y + 7)] + list(reversed(bottom[1:-1]))
        c.poly(spray, VEG["body"])
        # right half in shade, left half carrying the key light
        c.poly([(cx, y - 1), (right - half * 0.45, y + 1), (right, y + 7),
                (right - 3, y + 8), (cx + 1, y + 3)], VEG["shade"])
        for k in range(3):
            t = 0.08 + k * 0.28
            lx = left + (cx - left) * t
            rad = 3 + r(2)
            ly = y + 2 + k
            c.poly([(lx - rad, ly + 1), (lx - rad * 0.3, ly - rad * 0.8),
                    (lx + rad * 0.6, ly - rad * 0.5), (lx + rad, ly + 2),
                    (lx, ly + 3)], VEG["body_lt"] if (k + i) % 3 else VEG["lit"])
            c.line([(lx - rad + 1, ly - 1), (lx + rad * 0.4, ly - 2)],
                   VEG["lit_hi"] if (k + i) % 2 else VEG["lit"], 1)
        # dark cavity under the spray, then a couple of branch tips poking out
        c.line(bottom, VEG["deep"], 1)
        for k in range(2):
            tx = left + 3 + r(max(2, right - left - 6))
            c.px(tx, y + 8, VEG["cav"])
            c.px(tx + 1, y + 7 + r(2), VEG["shade"])

    # ---- crown: dry tips catch the last light ---------------------------------------
    c.px(cx - 1, 3, VEG["lit_hi"])
    c.px(cx + 1, 4, VEG["lit"])
    c.px(cx - 3, 6, VEG["lit_hi"])

    # ---- foot: moss and a shade line so it meets the P18/P19 lawn -------------------
    for _ in range(7):
        mx = 13 + r(22)
        c.px(mx, 84 + r(3), ARCH["moss"] if r(2) else ARCH["moss_lt"])
    c.line([(16, 85), (33, 84)], VEG["cav"], 1)
    return c.im


# ------------------------------------------------------------------ export
def stamp(manifest_path: Path, key: str, png: Path, array: bool):
    manifest = json.loads(manifest_path.read_text())
    digest = hashlib.sha256(png.read_bytes()).hexdigest()
    if array:
        entry = next(a for a in manifest["assets"] if a["id"] == key)
        entry["sha256"] = digest
        with Image.open(png) as im:
            entry["size"] = list(im.size)
    else:
        entry = manifest["assets"][key]
        entry["sha256"] = digest
        with Image.open(png) as im:
            entry["size"] = list(im.size)
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")


def main():
    armory_path = PREMIUM / "armory.png"
    armory().save(armory_path, optimize=True)
    stamp(PREMIUM / "manifest.json", "armory", armory_path, array=False)

    cypress_path = SANCT / "cypress.png"
    cypress().save(cypress_path, optimize=True)
    stamp(SANCT / "manifest.json", "cypress", cypress_path, array=True)

    print(json.dumps({"written": [str(armory_path.name), str(cypress_path.name)]}, indent=2))


if __name__ == "__main__":
    main()
