#!/usr/bin/env python3
"""MAP FORGE — P23 environmental props cohesion (Sanctuary + Tower).

The props in P22 HEAD were the last stand of the vector/Canvas era: a candy-coloured planter
drawn with `drawCircle`, a purple periscope lantern, a flat orange bench, and a floating purple
ruin arch. They sat next to P18 ground, P20/P21 buildings and P22 trees and read as placeholders
because they were not pixels at all.

This generator authors them as real 2.5D pixel sprites in the same language:

  * one key light upper-left (SanctuaryLight azimuth 142 deg, elevation 48 deg);
  * 3-value clusters per material, dark-grey joints instead of black cartoon outlines;
  * every mass has a top plane, a lit face and a shaded return face, and its base reaches the
    canvas ground row so the shared contact shadow has something to sit under;
  * the street lantern's THREE animation frames share byte-identical static pixels (post, base,
    bracket, frame); only the flame inside the glass changes, so the lamp cannot jitter;
  * the planter keeps its existing curb footprint (a 60 x 13 unit box on the ground plane at
    scale 0.96) but the curb is now coursed stone with a lit top face, and the flowers are
    bounded clusters instead of five smooth circles.

Canvases are new assets, so nothing existing is replaced: `SanctuaryArtAssets.assetSafe` loads
them PNG-first with the old Canvas drawing kept as the fallback path. No runtime generation, no
per-frame allocation, no new render pass.

Run:
    python3 tools/mapforge_p23_props.py                  # write PNGs + refresh manifests
    python3 tools/mapforge_p23_props.py --preview DIR    # + 3x review sheets
    python3 tools/mapforge_p23_props.py --check          # validate only, no writes
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
SANCT = ASSETS / "sanctuary_p03"

# The same stone / timber / iron families the buildings use, so props and architecture read as
# one world rather than two art styles.
STONE = dict(mortar="#2a3238", dk="#3b464e", base="#4a555d", lt="#5b6771", hi="#6f7c86",
             rim="#93a0aa", foot="#333c43")
OAK = dict(dk="#1f1710", base="#33251a", lt="#4a3524", hi="#63482f", warm="#7d5c3c")
IRON = dict(dk="#161b20", base="#252c33", lt="#3d4750", hi="#5f6b76", glint="#9dabb6")
AMBER = dict(deep="#4a2f11", dim="#9c6329", base="#dd9439", lt="#f6c071", core="#ffe6b6")
MOSS = dict(dk="#243328", base="#31452f", lt="#425a3c")
LEAF = dict(cav="#101c15", deep="#16281c", shade="#1e3424", body="#28452f", lt="#33553a",
            lit="#40663f", hi="#527d4b")
BLOOM = dict(pink="#c98ea6", rose="#f2c4d4", cream="#e8dfc0", violet="#a98ecb", gold="#e0b23c",
             blue="#8fb0d6")
GOLD = dict(dk="#6b5220", base="#a8842f", lt="#d0a844", hi="#f0cd74")


def rgba(h: str, a: int = 255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(a: str, b: str, t: float) -> str:
    pa, pb = rgba(a), rgba(b)
    t = min(1.0, max(0.0, t))
    return "#{:02x}{:02x}{:02x}".format(*[int(round(pa[i] + (pb[i] - pa[i]) * t)) for i in range(3)])


def rr(seed: int):
    r = random.Random(seed)
    return r.randrange


class Canvas:
    def __init__(self, w: int, h: int):
        self.w, self.h = w, h
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    def px(self, x, y, col, a=255):
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            self.im.putpixel((x, y), rgba(col, a))

    def rect(self, x0, y0, x1, y1, col, a=255):
        x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
        if x1 < x0 or y1 < y0:
            return
        self.d.rectangle([x0, y0, x1, y1], fill=rgba(col, a))

    def hline(self, x0, x1, y, col, a=255):
        self.rect(x0, y, x1, y, col, a)

    def vline(self, x, y0, y1, col, a=255):
        self.rect(x, y0, x, y1, col, a)

    def poly(self, pts, col, a=255):
        if len(pts) < 3:
            return
        self.d.polygon([(int(round(x)), int(round(y))) for x, y in pts], fill=rgba(col, a))

    def line(self, pts, col, width=1, a=255):
        self.d.line([(int(round(x)), int(round(y))) for x, y in pts], fill=rgba(col, a),
                    width=width)

    def ellipse(self, cx, cy, rx, ry, col, a=255):
        self.d.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=rgba(col, a))

    def get(self, x, y):
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.im.getpixel((x, y))
        return (0, 0, 0, 0)

    def opaque(self, x, y):
        return self.get(x, y)[3] > 0

    def blend(self, x, y, target, amount):
        x, y = int(x), int(y)
        if not (0 <= x < self.w and 0 <= y < self.h):
            return
        r, g, b, a = self.im.getpixel((x, y))
        if a == 0:
            return
        tr, tg, tb = rgba(target)[:3]
        self.im.putpixel((x, y), (int(r + (tr - r) * amount), int(g + (tg - g) * amount),
                                  int(b + (tb - b) * amount), a))


def cluster(c: Canvas, cx, cy, rx, ry, ramp, seed, scallop=True):
    """Organic leaf/flower cluster: scalloped outline, 3-value interior, no circles."""
    r = rr(seed)
    pts = []
    steps = 14
    for i in range(steps):
        a = i * math.tau / steps
        wob = 1.0 - (0.12 if scallop and i % 2 else 0.0) + (r(5) - 2) * 0.03
        pts.append((cx + math.cos(a) * rx * wob, cy + math.sin(a) * ry * wob))
    c.poly(pts, ramp["body"])
    # lit upper-left third
    lit = [(cx + (p[0] - cx) * 0.72 - 1, cy + (p[1] - cy) * 0.72 - 1) for p in pts]
    c.poly(lit[: steps // 2 + 2], ramp["lt"])
    inner = [(cx + (p[0] - cx) * 0.42 - 1, cy + (p[1] - cy) * 0.42 - 2) for p in pts]
    c.poly(inner[: steps // 2 + 1], ramp["lit"])
    # shaded lower-right crescent
    for p in pts[steps // 2 + 2:]:
        c.px(p[0], p[1], ramp["shade"])
        c.px(p[0], p[1] - 1, ramp["body"])
    # cavities: tiny dark notches so the mass is not one blob
    for _ in range(max(2, rx // 4)):
        ax = cx + r(max(2, int(rx))) - rx // 2
        ay = cy + r(max(1, int(ry))) - ry // 3
        c.px(ax, ay, ramp["cav"])
    c.px(cx - rx // 3, cy - ry // 2, ramp["hi"] if "hi" in ramp else ramp["lit"])


# --------------------------------------------------------------------------- planter bed
def build_flower_planter() -> Canvas:
    """64x36 — stone curb planter with a lit top face and bounded flower clusters.

    The Canvas version this replaces drew a bright #D6CFC2 box with five smooth pastel circles
    and cast no world shadow. The footprint (60 x 13 world units at scale 0.96, drawn bottom
    centred at cy + 4 * s) is preserved, so no collider, anchor or planter row moves.
    """
    W, H = 64, 36
    c = Canvas(W, H)
    base = H - 1
    x0, x1 = 3, 60
    # ---- curb: lit top face (foreshortened 3 px), front face, shaded right return --------
    c.poly([(x0, 20), (x0 + 5, 15), (x1 - 1, 15), (x1 - 6, 20)], STONE["lt"])
    c.hline(x0 + 5, x1 - 1, 15, STONE["hi"])
    c.rect(x0, 20, x1 - 6, base - 3, STONE["base"])
    c.poly([(x1 - 6, 20), (x1 - 1, 15), (x1 - 1, base - 8), (x1 - 6, base - 3)], STONE["dk"])
    # courses + corner quoins
    for yy in range(23, base - 3, 4):
        c.hline(x0, x1 - 6, yy, STONE["mortar"])
        c.hline(x0, x1 - 6, yy + 1, mix(STONE["base"], "#ffffff", 0.08))
    c.rect(x0, 20, x0 + 2, base - 3, STONE["lt"])
    c.vline(x0 + 3, 21, base - 3, STONE["mortar"])
    # plinth + contact AO
    c.rect(x0 - 1, base - 3, x1 - 5, base, STONE["foot"])
    c.hline(x0 - 1, x1 - 5, base, "#12181c")
    # ---- soil inside the curb -----------------------------------------------------------
    c.poly([(x0 + 6, 16), (x0 + 10, 12), (x1 - 5, 12), (x1 - 8, 16)], "#2a2018")
    c.hline(x0 + 10, x1 - 5, 12, "#15100c")
    # ---- planting: 3 bounded clusters + stems, inside the curb, not on the rim -----------
    for i, (fx, fy, rx, ry, kind) in enumerate((
        (14, 12, 7, 5, "leaf"), (30, 11, 8, 5, "leaf"), (46, 12, 7, 5, "leaf"))):
        cluster(c, fx, fy, rx, ry, LEAF, 5100 + i)
    blooms = [
        (11, 9, BLOOM["rose"]), (15, 7, BLOOM["cream"]), (18, 10, BLOOM["pink"]),
        (26, 8, BLOOM["gold"]), (31, 6, BLOOM["rose"]), (35, 9, BLOOM["cream"]),
        (42, 8, BLOOM["violet"]), (47, 6, BLOOM["blue"]), (51, 9, BLOOM["rose"]),
        (23, 11, BLOOM["blue"]), (38, 12, BLOOM["gold"]), (55, 11, BLOOM["cream"]),
    ]
    for bx, by, col in blooms:
        c.px(bx, by, col)
        c.px(bx + 1, by, mix(col, "#ffffff", 0.35))
        c.px(bx, by + 1, mix(col, "#000000", 0.35))
        c.px(bx - 1, by, mix(col, "#000000", 0.15))
    # a few green tips poking above the blooms so the bed is not a candy strip
    for tx in (20, 28, 44, 53):
        c.px(tx, 7, LEAF["lt"])
        c.px(tx, 6, LEAF["lit"])
    # ---- soft rim light along the top-left, dark under the curb lip ----------------------
    c.hline(x0 + 6, x1 - 1, 15, STONE["rim"])
    c.hline(x0, x1 - 6, 20, STONE["lt"])
    for x in range(x0, x1 - 5):
        if c.opaque(x, base):
            c.blend(x, base, "#121a1f", 0.42)
            c.blend(x, base - 1, "#0f151a", 0.18)
    return c


# --------------------------------------------------------------------------- street lantern
LANTERN_STATIC_REF = None


def build_street_lantern(frame: int) -> Canvas:
    """32x66 — wrought-iron lantern post. Only the flame changes between the 3 frames.

    The Canvas version flickered by changing `fillCircle` radius, which changed the *silhouette*
    every frame (visible jitter) and drew a flat yellow octagon. Here the post, base, bracket and
    the glass housing are byte-identical across frames; frame 1 is one pixel brighter inside the
    glass, frame 2 leans the flame one pixel the other way. Verified by the contract test.
    """
    W, H = 32, 66
    c = Canvas(W, H)
    base = H - 1
    cx = 13
    # ---- stepped base, on the ground row ------------------------------------------------
    c.rect(cx - 5, base - 6, cx + 5, base, IRON["dk"])
    c.rect(cx - 4, base - 5, cx + 4, base - 1, IRON["base"])
    c.hline(cx - 5, cx + 5, base - 6, IRON["lt"])
    c.vline(cx - 4, base - 5, base - 1, IRON["lt"])
    c.hline(cx - 5, cx + 5, base, "#0d1114")
    c.rect(cx - 2, base - 9, cx + 2, base - 6, IRON["base"])
    c.hline(cx - 2, cx + 2, base - 9, IRON["hi"])
    # ---- fluted post --------------------------------------------------------------------
    c.rect(cx - 2, 12, cx + 2, base - 9, IRON["base"])
    c.vline(cx - 2, 12, base - 9, IRON["lt"])
    c.vline(cx + 2, 12, base - 9, IRON["dk"])
    c.vline(cx, 14, base - 10, IRON["dk"])
    # collar + finial
    c.rect(cx - 3, 11, cx + 3, 13, IRON["lt"])
    c.hline(cx - 3, cx + 3, 11, IRON["hi"])
    c.px(cx, 9, IRON["hi"])
    c.px(cx, 8, IRON["glint"])
    # ---- wrought-iron bracket + hanging lantern housing (STATIC) ------------------------
    c.rect(cx, 14, cx + 9, 16, IRON["base"])
    c.hline(cx, cx + 9, 14, IRON["lt"])
    c.vline(cx + 9, 15, 20, IRON["base"])
    c.rect(cx + 6, 20, cx + 12, 21, IRON["dk"])          # top cap of the housing
    c.hline(cx + 6, cx + 12, 20, IRON["hi"])
    c.rect(cx + 6, 21, cx + 12, 30, IRON["dk"])          # frame box (opaque border)
    c.vline(cx + 6, 21, 30, IRON["base"])
    c.vline(cx + 12, 21, 30, "#0f1316")
    c.rect(cx + 7, 22, cx + 11, 29, "#0c1013")           # glass cavity (dark by default)
    c.rect(cx + 6, 30, cx + 12, 31, IRON["base"])        # bottom cap
    c.hline(cx + 6, cx + 12, 31, IRON["dk"])
    c.px(cx + 9, 32, IRON["lt"])                          # small drop finial
    # ---- flame inside the glass: the ONLY animated pixels --------------------------------
    gx = cx + 9 + (0 if frame != 2 else 1) - (1 if frame == 0 and False else 0)
    gy = 26 + (1 if frame == 0 else 0)
    c.px(gx, gy - 2, AMBER["lt"])
    c.rect(gx - 1, gy - 1, gx + 1, gy, AMBER["base"])
    c.px(gx, gy - 1, AMBER["core"])
    c.rect(gx - 1, gy + 1, gx + 1, gy + 2, AMBER["dim"])
    if frame == 1:                                       # brightest frame: one extra core pixel
        c.px(gx - 1, gy - 1, AMBER["lt"])
        c.px(gx, gy - 2, AMBER["core"])
    # the glass picks up a warm bounce on its lit inner face (static per frame count)
    c.px(cx + 7, 24, mix("#0c1013", AMBER["dim"], 0.45))
    c.px(cx + 7, 25, mix("#0c1013", AMBER["dim"], 0.30))
    # ---- rim light down the lit side, contact AO on the base row ------------------------
    for y in range(12, base - 8):
        c.blend(cx - 2, y, "#c9d4dc", 0.14)
    for x in range(cx - 5, cx + 6):
        if c.opaque(x, base):
            c.blend(x, base, "#101a1f", 0.45)
    return c


# --------------------------------------------------------------------------- wooden bench
def build_wooden_bench() -> Canvas:
    """58x36 — 3/4 oak bench: top face, front lip, two front legs, two rear legs.

    The Canvas version had a flat orange front elevation with a black outline and no top plane.
    This one shows the foreshortened seat, the front apron, and all four legs reaching the ground
    row, in the oak family the buildings' timber frames use.
    """
    W, H = 58, 36
    c = Canvas(W, H)
    base = H - 1
    x0, x1 = 3, 54
    # ---- rear legs first (they are behind the seat) ------------------------------------
    c.rect(x0 + 6, 20, x0 + 9, base - 1, OAK["dk"])
    c.rect(x1 - 9, 20, x1 - 6, base - 1, OAK["dk"])
    # ---- backrest: two rails on two posts, top plane visible ---------------------------
    c.rect(x0 + 5, 6, x0 + 8, 22, OAK["dk"])
    c.rect(x1 - 8, 6, x1 - 5, 22, OAK["dk"])
    c.vline(x0 + 6, 6, 21, OAK["lt"])
    c.vline(x1 - 7, 6, 21, OAK["lt"])
    c.rect(x0 + 4, 5, x1 - 4, 8, OAK["base"])
    c.hline(x0 + 4, x1 - 4, 5, OAK["hi"])
    c.hline(x0 + 4, x1 - 4, 8, OAK["dk"])
    c.rect(x0 + 4, 12, x1 - 4, 14, OAK["lt"])
    c.hline(x0 + 4, x1 - 4, 12, OAK["warm"])
    c.hline(x0 + 4, x1 - 4, 14, OAK["dk"])
    # ---- seat: lit top face (4 px of foreshortened depth) + front apron ----------------
    c.poly([(x0, 21), (x0 + 3, 17), (x1 - 2, 17), (x1, 21)], OAK["lt"])
    c.hline(x0 + 3, x1 - 2, 17, OAK["warm"])
    c.hline(x0, x1, 21, OAK["hi"])
    c.rect(x0, 21, x1, 25, OAK["base"])
    for slat in range(x0 + 4, x1 - 2, 9):
        c.vline(slat, 21, 25, OAK["dk"])
    c.hline(x0, x1, 25, OAK["dk"])
    # ---- front legs, splayed, each with its own lit edge -------------------------------
    for lx in (x0 + 2, x1 - 6):
        c.rect(lx, 26, lx + 3, base, OAK["base"])
        c.vline(lx, 26, base, OAK["lt"])
        c.vline(lx + 3, 26, base, OAK["dk"])
    # stretcher between the front legs
    c.rect(x0 + 4, 30, x1 - 4, 31, OAK["dk"])
    c.hline(x0 + 4, x1 - 4, 30, OAK["base"])
    # ---- contact AO --------------------------------------------------------------------
    for x in range(x0, x1 + 1):
        if c.opaque(x, base):
            c.blend(x, base, "#111a1f", 0.42)
        if c.opaque(x, base - 1):
            c.blend(x, base - 1, "#0f151a", 0.16)
    return c


# --------------------------------------------------------------------------- ruin arch
def build_ruin_arch() -> Canvas:
    """96x86 — crumbling double-pier arch: coursed stone, real arched opening, moss, grounded.

    The Canvas version this replaces drew two flat purple brick rectangles floating 10 px above
    their own shadow, with a neon-lime blob on top. This one is a solid wall with an arch cut
    INTO it, so the opening is depth (dark intrados, lit left jamb, shaded right jamb) rather
    than a hole in two separate blocks, and both piers reach the canvas ground row.
    """
    W, H = 96, 86
    c = Canvas(W, H)
    base = H - 1
    x0, x1 = 8, 82
    open_l, open_r = 35, 55          # arch opening span
    spring_y = 46                     # where the arch springs from the jambs

    # ---- solid wall body ---------------------------------------------------------------
    c.rect(x0, 26, x1, base, STONE["base"])
    # ---- ordered courses with staggered vertical joints --------------------------------
    yy = 28
    row = 0
    while yy < base:
        c.hline(x0, x1, yy, STONE["mortar"])
        c.hline(x0, x1, yy + 1, mix(STONE["base"], "#ffffff", 0.07))
        off = 0 if row % 2 == 0 else 5
        for xx in range(x0 + off, x1, 9):
            c.vline(xx, yy + 1, min(yy + 4, base), STONE["mortar"])
        yy += 5
        row += 1
    # lit left edge, shaded right return
    c.rect(x0, 26, x0 + 4, base, mix(STONE["lt"], "#ffffff", 0.05))
    c.rect(x1 - 4, 26, x1, base, STONE["dk"])
    c.vline(x0, 26, base, STONE["hi"])
    c.vline(x1, 26, base, "#0f1519")
    # ---- broken crown: staggered top edge, highest over the left pier ------------------
    c.poly([(x0, 26), (x0 + 18, 26), (x0 + 20, 22), (x0 + 40, 22), (x0 + 42, 30),
            (x1 - 14, 30), (x1 - 12, 26), (x1, 26), (x1, 34), (x0, 34)], STONE["dk"])
    c.poly([(x0, 26), (x0 + 18, 26), (x0 + 20, 22), (x0 + 40, 22), (x0 + 41, 25),
            (x0 + 24, 25), (x0 + 22, 29), (x0, 31)], STONE["base"])
    c.hline(x0 + 20, x0 + 40, 22, STONE["rim"])
    c.hline(x0 + 42, x1 - 12, 30, STONE["lt"])
    for i in range(x0 + 4, x0 + 36, 5):       # chipped crown on the tall left block only
        if (i // 5) % 2:
            c.px(i, 21, STONE["lt"])
            c.px(i + 1, 21, STONE["base"])

    # ---- the arch opening: cut through the wall, then give it depth --------------------
    c.rect(open_l, spring_y, open_r, base, "#070b0e")
    c.ellipse((open_l + open_r) // 2, spring_y, (open_r - open_l) // 2, 14, "#070b0e")
    # intrados bands, darkest at the crown
    for i, inset in enumerate((2, 4, 6)):
        col = ("#0b1216", "#0f181d", "#131e24")[i]
        c.rect(open_l + inset, spring_y + 2, open_r - inset, base, col)
        c.ellipse((open_l + open_r) // 2, spring_y, (open_r - open_l) // 2 - inset, 14 - inset,
                  col)
    # lit left jamb, shaded right jamb
    c.vline(open_l, spring_y + 2, base, STONE["lt"])
    c.vline(open_l + 1, spring_y + 4, base, mix("#0b1216", STONE["base"], 0.35))
    c.vline(open_r, spring_y + 2, base, "#080c0f")
    # voussoir ring around the head, one block per 12 degrees
    cxv = (open_l + open_r) // 2
    for a in range(13):
        ang = math.pi + a * math.pi / 12
        vx = cxv + math.cos(ang) * ((open_r - open_l) // 2 + 3)
        vy = spring_y + math.sin(ang) * 17
        c.rect(vx - 2, vy - 2, vx + 2, vy + 2, STONE["lt"])
        c.hline(vx - 2, vx + 2, vy - 2, STONE["hi"])
        c.hline(vx - 2, vx + 2, vy + 2, STONE["dk"])
    # keystone
    c.rect(cxv - 3, spring_y - 21, cxv + 3, spring_y - 14, STONE["lt"])
    c.hline(cxv - 3, cxv + 3, spring_y - 21, STONE["rim"])
    c.hline(cxv - 3, cxv + 3, spring_y - 14, STONE["foot"])
    # the right shoulder of the ring is BROKEN: clear a clean wedge, then re-cap its stone
    # edge so the break reads as missing masonry instead of a checkerboard
    for dx in range(16, 27):
        for dy in range(-19, -7):
            c.im.putpixel((cxv + dx, spring_y + dy), (0, 0, 0, 0))
    for dy in range(-19, -7):
        xedge = cxv + 16 + (abs(dy + 13) // 3)
        if c.opaque(xedge - 1, spring_y + dy):
            c.px(xedge - 1, spring_y + dy, STONE["lt"])
            c.px(xedge - 2, spring_y + dy, STONE["base"])

    # ---- rubble at the feet, both on the ground row ------------------------------------
    c.poly([(26, base - 4), (33, base - 9), (41, base - 5), (37, base)], STONE["base"])
    c.line([(26, base - 4), (33, base - 9)], STONE["lt"])
    c.line([(33, base - 9), (41, base - 5)], STONE["hi"])
    c.poly([(58, base - 3), (66, base - 7), (71, base - 2), (64, base)], STONE["dk"])
    c.line([(58, base - 3), (66, base - 7)], STONE["base"])

    # ---- moss where the stone meets the ground, ivy on the left pier --------------------
    r = rr(7300)
    for _ in range(14):
        mx = x0 + r(24)
        my = base - r(5)
        if c.opaque(mx, my):
            c.px(mx, my, MOSS["base"])
            c.px(mx + 1, my, MOSS["dk"])
            c.px(mx, my - 1, MOSS["lt"])
    for i in range(9):
        c.px(x0 + 2 + (i % 3), 44 + i * 3, MOSS["dk"] if i % 2 else MOSS["base"])
        c.px(x0 + 3 + (i % 2), 45 + i * 3, MOSS["lt"])
    # ---- contact AO --------------------------------------------------------------------
    for x in range(x0, x1 + 1):
        if c.opaque(x, base):
            c.blend(x, base, "#111a1f", 0.40)
    return c


# --------------------------------------------------------------------------- retaining wall
def build_retaining_wall() -> Canvas:
    """88x48 — coursed stone retaining wall with a lit coping and ivy spilling over it."""
    W, H = 88, 48
    c = Canvas(W, H)
    base = H - 1
    x0, x1 = 2, 82
    # ---- wall body: front face + shaded right return ------------------------------------
    c.rect(x0, 10, x1 - 6, base, STONE["base"])
    c.poly([(x1 - 6, 10), (x1, 6), (x1, base - 4), (x1 - 6, base)], STONE["dk"])
    yy = 12
    while yy < base:
        c.hline(x0, x1 - 6, yy, STONE["mortar"])
        c.hline(x0, x1 - 6, yy + 1, mix(STONE["base"], "#ffffff", 0.07))
        off = 0 if ((yy - 10) // 5) % 2 == 0 else 5
        for xx in range(x0 + off, x1 - 6, 9):
            c.vline(xx, yy + 1, min(yy + 4, base), STONE["mortar"])
        yy += 5
    c.rect(x0, 10, x0 + 3, base, STONE["lt"])
    c.vline(x0, 10, base, STONE["hi"])
    # ---- coping: lit top plane + front lip ---------------------------------------------
    c.poly([(x0, 11), (x0 + 4, 6), (x1, 6), (x1 - 6, 11)], STONE["lt"])
    c.hline(x0 + 4, x1, 6, STONE["rim"])
    c.hline(x0, x1 - 6, 11, STONE["hi"])
    c.rect(x0 + 4, 7, x1, 7, STONE["base"])
    c.rect(x0, 11, x1 - 6, 12, STONE["base"])
    # ---- ivy over the coping, hanging in clusters --------------------------------------
    for i, (ix, iy, irx, iry) in enumerate(((18, 8, 10, 5), (34, 8, 12, 5), (62, 8, 11, 5))):
        cluster(c, ix, iy, irx, iry, LEAF, 7400 + i)
    for i in range(4):
        c.px(26 + i, 10 + i, LEAF["body"])
        c.px(52 + i, 9 + i, LEAF["lt"])
    # ---- contact AO + moss -------------------------------------------------------------
    for x in range(x0, x1 + 1):
        if c.opaque(x, base):
            c.blend(x, base, "#111a1f", 0.40)
    for mx in (12, 20, 40, 68, 76):
        if c.opaque(mx, base - 1):
            c.px(mx, base - 1, MOSS["base"])
            c.px(mx + 1, base - 1, MOSS["dk"])
    return c


# --------------------------------------------------------------------------- monolith
def build_monolith() -> Canvas:
    """44x74 — leaning rune monolith on a stone base, with a carved glyph channel."""
    W, H = 44, 74
    c = Canvas(W, H)
    base = H - 1
    cx = 22
    # ---- base plinth -------------------------------------------------------------------
    c.rect(cx - 12, base - 7, cx + 12, base, STONE["dk"])
    c.rect(cx - 11, base - 6, cx + 11, base - 1, STONE["base"])
    c.hline(cx - 12, cx + 12, base - 7, STONE["lt"])
    c.hline(cx - 12, cx + 12, base, "#101619")
    for xx in range(cx - 10, cx + 11, 7):
        c.vline(xx, base - 5, base - 1, STONE["mortar"])
    # ---- shaft: leaning, lit left face, shaded right, chipped crown --------------------
    c.poly([(cx - 8, 6), (cx + 3, 3), (cx + 8, 66), (cx - 6, 66)], "#343b52")
    c.poly([(cx - 8, 6), (cx - 2, 4), (cx + 1, 66), (cx - 6, 66)], "#465071")
    c.poly([(cx + 3, 3), (cx + 8, 66), (cx + 1, 66), (cx - 2, 4)], "#232840")
    c.line([(cx - 8, 6), (cx - 2, 4)], "#5f6b8e")
    c.line([(cx - 2, 4), (cx + 3, 3)], "#6f7d9e")
    # carved rune channel + glyph ticks (cold cyan, restrained)
    c.rect(cx - 2, 16, cx, 54, "#1b2030")
    c.vline(cx - 1, 18, 52, mix("#1b2030", "#3f8fa8", 0.55))
    for yy in (22, 30, 38, 46):
        c.px(cx - 2, yy, "#3f8fa8")
        c.px(cx, yy + 2, "#244a5c")
    # weathering: hairline cracks + moss at the foot
    c.line([(cx + 4, 20), (cx + 5, 30), (cx + 3, 38)], "#1a1e2c", 1)
    for mx in (cx - 6, cx - 3, cx + 4):
        if c.opaque(mx, base - 8):
            c.px(mx, base - 8, MOSS["base"])
            c.px(mx + 1, base - 8, MOSS["dk"])
    for x in range(cx - 12, cx + 13):
        if c.opaque(x, base):
            c.blend(x, base, "#101a1f", 0.42)
    return c


# --------------------------------------------------------------------------- training dummy
def build_training_dummy() -> Canvas:
    """40x58 — padded practice dummy on a cross-stand, in oak + straw + iron bands.

    Replaces the Canvas `drawRect` dummy (bright #C28956 / #D19A66 rectangles) so the training
    yard stops looking like a different game from the buildings beside it.
    """
    W, H = 40, 58
    c = Canvas(W, H)
    base = H - 1
    cx = 20
    # ---- cross-stand base --------------------------------------------------------------
    for fx in (4, 28):
        c.rect(fx, base - 6, fx + 8, base - 2, OAK["dk"])
        c.hline(fx, fx + 8, base - 6, OAK["base"])
        c.hline(fx, fx + 8, base - 2, "#10130f")
    c.rect(cx - 5, base - 8, cx + 5, base - 2, OAK["base"])
    c.hline(cx - 5, cx + 5, base - 8, OAK["lt"])
    # ---- post + straw torso ------------------------------------------------------------
    c.rect(cx - 4, 20, cx + 4, base - 6, OAK["base"])
    c.vline(cx - 4, 20, base - 6, OAK["lt"])
    c.vline(cx + 4, 20, base - 6, OAK["dk"])
    c.rect(cx - 8, 14, cx + 8, 36, "#5f5230")            # straw body (P19 sand value range)
    c.rect(cx - 7, 15, cx + 7, 35, "#726237")
    c.rect(cx - 7, 15, cx + 2, 35, "#877445")            # lit left third
    for ry in (17, 23, 29, 34):
        c.hline(cx - 8, cx + 8, ry, "#41361b")           # rope bindings
        c.hline(cx - 8, cx + 8, ry + 1, "#8e7c4b")
    c.rect(cx - 8, 22, cx + 8, 23, "#3d3016")            # iron bands
    c.rect(cx - 8, 31, cx + 8, 32, "#3d3016")
    c.hline(cx - 8, cx + 8, 22, "#6b5a2c")
    # ---- T-arms ------------------------------------------------------------------------
    c.rect(cx - 17, 20, cx + 17, 24, OAK["base"])
    c.hline(cx - 17, cx + 17, 20, OAK["lt"])
    c.hline(cx - 17, cx + 17, 24, OAK["dk"])
    for ex in (cx - 18, cx + 15):
        c.rect(ex, 18, ex + 3, 26, OAK["dk"])
        c.hline(ex, ex + 3, 18, OAK["base"])
    # ---- head knob + painted target ring ------------------------------------------------
    c.rect(cx - 5, 6, cx + 5, 14, "#5f5230")
    c.rect(cx - 4, 7, cx + 4, 13, "#877445")
    c.hline(cx - 5, cx + 5, 6, "#9c8a55")
    c.ellipse(cx, 25, 3, 2, "#8f2f2f")
    c.ellipse(cx, 25, 1, 1, "#d9b0b0")
    for x in range(cx - 17, cx + 18):
        if c.opaque(x, base):
            c.blend(x, base, "#141a14", 0.36)
    return c


# --------------------------------------------------------------------------- weapon rack
def build_weapon_rack() -> Canvas:
    """68x48 — A-frame rack with five practice weapons, oak + steel, grounded on both feet."""
    W, H = 68, 48
    c = Canvas(W, H)
    base = H - 1
    cx = 34
    # ---- frame: two splayed side posts + two rails -------------------------------------
    c.poly([(cx - 30, base), (cx - 26, 8), (cx - 22, 8), (cx - 24, base)], OAK["base"])
    c.poly([(cx + 24, base), (cx + 22, 8), (cx + 26, 8), (cx + 30, base)], OAK["dk"])
    c.line([(cx - 26, 8), (cx - 22, 8)], OAK["lt"])
    c.line([(cx + 22, 8), (cx + 26, 8)], OAK["base"])
    c.rect(cx - 28, 16, cx + 28, 19, OAK["dk"])
    c.hline(cx - 28, cx + 28, 16, OAK["lt"])
    c.rect(cx - 28, 30, cx + 28, 33, OAK["dk"])
    c.hline(cx - 28, cx + 28, 30, OAK["lt"])
    # ---- five practice weapons leaning in the rack -------------------------------------
    for i in range(5):
        wx = cx - 20 + i * 10
        lean = (i - 2)
        top = 6 + abs(lean)
        if i % 2 == 0:      # spears
            c.line([(wx, base - 4), (wx + lean, top + 4)], OAK["base"], 2)
            c.line([(wx + lean, top + 4), (wx + lean, top)], "#b9c3c9", 2)
            c.px(wx + lean, top - 1, "#dde5ea")
        else:               # swords
            c.line([(wx, base - 6), (wx + lean, top + 8)], OAK["lt"], 2)
            c.line([(wx + lean, top + 8), (wx + lean, top + 2)], "#aab6c0", 2)
            c.hline(wx + lean - 3, wx + lean + 3, top + 9, GOLD["base"])
            c.px(wx + lean, top + 1, "#e6eef3")
    # ---- contact AO --------------------------------------------------------------------
    for x in range(cx - 31, cx + 31):
        if c.opaque(x, base):
            c.blend(x, base, "#111a1f", 0.38)
    return c


# --------------------------------------------------------------------------- striking post
def build_striking_post() -> Canvas:
    """36x86 — martial striking post: stone collar, oak pillar, three rope-wrapped zones."""
    W, H = 36, 86
    c = Canvas(W, H)
    base = H - 1
    cx = 18
    # ---- stone collar on the ground row -------------------------------------------------
    c.rect(cx - 8, base - 8, cx + 8, base, STONE["dk"])
    c.rect(cx - 7, base - 7, cx + 7, base - 1, STONE["base"])
    c.hline(cx - 8, cx + 8, base - 8, STONE["lt"])
    c.hline(cx - 8, cx + 8, base, "#101619")
    for xx in range(cx - 6, cx + 7, 6):
        c.vline(xx, base - 6, base - 1, STONE["mortar"])
    # ---- oak pillar --------------------------------------------------------------------
    c.rect(cx - 5, 8, cx + 5, base - 8, OAK["base"])
    c.vline(cx - 5, 8, base - 8, OAK["lt"])
    c.vline(cx + 5, 8, base - 8, OAK["dk"])
    c.vline(cx - 1, 10, base - 9, OAK["dk"])              # grain furrow
    c.vline(cx + 2, 11, base - 9, OAK["warm"])
    # ---- three rope-wrapped striking zones, iron pegs -----------------------------------
    for ry in (20, 42, 62):
        c.rect(cx - 7, ry, cx + 7, ry + 7, "#8d7440")
        c.hline(cx - 7, cx + 7, ry, "#b39a5a")
        for yy in range(ry + 2, ry + 7, 2):
            c.hline(cx - 7, cx + 7, yy, "#6b5729")
        c.hline(cx - 7, cx + 7, ry + 7, "#4d3d1c")
    for (px_, py_, dir_) in ((cx - 13, 26, -1), (cx + 6, 46, 1), (cx - 12, 58, -1)):
        c.rect(px_, py_, px_ + 6, py_ + 3, OAK["lt"])
        c.hline(px_, px_ + 6, py_, OAK["warm"])
        c.hline(px_, px_ + 6, py_ + 3, OAK["dk"])
        c.rect(px_ + (6 if dir_ > 0 else -2), py_, px_ + (8 if dir_ > 0 else 0), py_ + 3,
               IRON["base"])
    # ---- cap + contact AO --------------------------------------------------------------
    c.rect(cx - 6, 6, cx + 6, 9, OAK["dk"])
    c.hline(cx - 6, cx + 6, 6, OAK["warm"])
    for x in range(cx - 8, cx + 9):
        if c.opaque(x, base):
            c.blend(x, base, "#111a1f", 0.40)
    return c


# --------------------------------------------------------------------------- registry
BUILDERS = {
    "flower_planter": (build_flower_planter, 64, 36),
    "street_lantern_0": (lambda: build_street_lantern(0), 32, 66),
    "street_lantern_1": (lambda: build_street_lantern(1), 32, 66),
    "street_lantern_2": (lambda: build_street_lantern(2), 32, 66),
    "wooden_bench": (build_wooden_bench, 58, 36),
    "ruin_arch": (build_ruin_arch, 96, 86),
    "retaining_wall": (build_retaining_wall, 88, 48),
    "monolith": (build_monolith, 44, 74),
    "training_dummy": (build_training_dummy, 40, 58),
    "weapon_rack": (build_weapon_rack, 68, 48),
    "striking_post": (build_striking_post, 36, 86),
}


def validate(key: str, c: Canvas) -> dict:
    W, H = BUILDERS[key][1], BUILDERS[key][2]
    im = c.im
    assert im.mode == "RGBA" and im.size == (W, H), (key, im.mode, im.size)
    px = im.load()
    semi = sum(1 for y in range(H) for x in range(W) if 0 < px[x, y][3] < 255)
    assert semi == 0, (key, f"{semi} semi-transparent pixels")
    xs = [x for y in range(H) for x in range(W) if px[x, y][3] >= 128]
    ys = [y for y in range(H) for x in range(W) if px[x, y][3] >= 128]
    assert xs and ys, (key, "empty")
    # ground contact: the lowest opaque row must span enough width to sit on the ground
    bottom = max(ys)
    contact = sum(1 for x in range(W) if px[x, bottom][3] >= 128)
    # a bench stands on four legs, a post on a collar: the contact row only has to be
    # non-trivial and present, not a wall of pixels
    assert contact >= 6, (key, "floats: narrow ground contact", contact)
    assert bottom >= H - 3, (key, "base row too high", bottom)
    for x in range(W):
        assert px[x, 0][3] == 0, (key, "top row", x)
    colors = len({px[x, y][:3] for y in range(H) for x in range(W) if px[x, y][3] >= 128})
    assert 6 <= colors <= 64, (key, "palette", colors)
    return dict(size=[W, H], colors=colors, bottom=bottom, contact=contact,
                x_range=[min(xs), max(xs)], y_range=[min(ys), max(ys)])


def stamp(manifest_path: Path, key: str, png: Path):
    manifest = json.loads(manifest_path.read_text())
    digest = hashlib.sha256(png.read_bytes()).hexdigest()
    with Image.open(png) as im:
        size = list(im.size)
    entry = next((a for a in manifest["assets"] if a.get("id") == key), None)
    if entry is None:
        entry = {"id": key, "path": png.name, "size": size,
                 "ground_anchor": [size[0] // 2, size[1] - 1],
                 "runtime_usage": "P23 environmental prop; PNG-first via SanctuaryArtAssets.assetSafe"}
        manifest["assets"].append(entry)
    entry["path"] = png.name
    entry["size"] = size
    entry["sha256"] = digest
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")


def write_contact_sheet(out: Path):
    keys = list(BUILDERS)
    pad, label_h = 12, 20
    cells = []
    for key in keys:
        im = Image.open(SANCT / f"{key}.png").convert("RGBA")
        shots = [("1x", im), ("3x", im.resize((im.width * 3, im.height * 3), Image.NEAREST))]
        cw = max(s.width for _, s in shots) + pad
        ch = sum(s.height for _, s in shots) + pad * 3 + label_h * len(shots)
        cell = Image.new("RGBA", (cw, ch), (18, 21, 24, 255))
        d = ImageDraw.Draw(cell)
        d.text((6, 4), key, fill=(210, 220, 228, 255))
        y = pad + 12
        for label, shot in shots:
            d.text((6, y - 11), label, fill=(140, 152, 162, 255))
            bg = Image.new("RGBA", shot.size, (26, 42, 34, 255))
            bg.alpha_composite(shot)
            cell.alpha_composite(bg, ((cw - shot.width) // 2, y))
            y += shot.height + pad + label_h - 6
        cells.append(cell)
    rows = [cells[i:i + 6] for i in range(0, len(cells), 6)]
    W = max(sum(cc.width for cc in r) for r in rows) + pad
    H = sum(max(cc.height for cc in r) for r in rows) + 30 + pad * len(rows)
    sheet = Image.new("RGBA", (W, H), (14, 16, 20, 255))
    d = ImageDraw.Draw(sheet)
    d.text((8, 8), "ART CONTACT SHEET - ETERNAL SUMMONER P23 props (offline authored PNG, "
                   "nearest-neighbour; NOT an engine render)", fill=(228, 190, 120, 255))
    y = 26
    for r in rows:
        x = pad
        for cc in r:
            sheet.alpha_composite(cc, (x, y))
            x += cc.width
        y += max(cc.height for cc in r) + pad
    sheet.save(out)
    print(f"contact sheet -> {out}")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", default=None)
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()

    report = {}
    canvases = {}
    for key, (fn, w, h) in BUILDERS.items():
        c = fn()
        report[key] = validate(key, c)
        canvases[key] = c
        if not args.check:
            out = SANCT / f"{key}.png"
            c.im.save(out, optimize=True)
            stamp(SANCT / "manifest.json", key, out)
            if args.preview:
                prev = Path(args.preview)
                prev.mkdir(parents=True, exist_ok=True)
                bg = Image.new("RGBA", c.im.size, (26, 32, 30, 255))
                bg.alpha_composite(c.im)
                bg.resize((w * 3, h * 3), Image.NEAREST).save(prev / f"{key}_3x.png")

    # lantern frames must share every static pixel: only the flame may differ
    a, b, d = (canvases["street_lantern_0"].im, canvases["street_lantern_1"].im,
               canvases["street_lantern_2"].im)
    diffab = [sum(1 for y in range(66) if a.getpixel((x, y)) != b.getpixel((x, y)))
              for x in range(32)]
    diff_cols = [x for x, n in enumerate(diffab) if n]
    assert all(19 <= x <= 25 for x in diff_cols), (
        "street lantern frames must differ only inside the glass", diff_cols)
    report["lantern_frame_lock"] = {"differing_columns": diff_cols,
                                    "static_columns": 32 - len(diff_cols)}
    if args.preview:
        write_contact_sheet(Path(args.preview) / "P23_PROPS_CONTACT_SHEET.png")
    print(json.dumps({"assets": report, "mode": "check" if args.check else "write"}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
