#!/usr/bin/env python3
"""MAP FORGE — P21 premium Sanctuary facilities (original 2.5D pixel architecture).

Seven facility sprites are authored offline, each replacing an existing procedural
`PixelSanctuaryLandmarks` builder in place, so that every world anchor, footprint, depth-sort
key, entrance trigger and camera contract keeps working unchanged:

  premium_slice/summoning_hall.png   268x206  (FACILITY_SUMMONING_HALL,  scale 1.08)
  premium_slice/tower_gate.png       260x224  (FACILITY_TOWER_GATE,     scale 0.96)
  premium_slice/hero_registry.png    188x166  (FACILITY_HERO_REGISTRY,  scale 0.80)
  premium_slice/hero_quarters.png    200x168  (FACILITY_HERO_QUARTERS,  scale 0.80, enterable)
  premium_slice/restaurant.png       206x172  (FACILITY_RESTAURANT,     scale 0.80, enterable)
  premium_slice/synthesis_lab.png    188x168  (FACILITY_SYNTHESIS_LAB,  scale 0.80)

(png for training ground is intentionally NOT authored: `PropKind.FACILITY_TRAINING_GROUND`
has no WorldObstacle on the Sanctuary map at this HEAD, so the sprite would never be drawn --
see docs/art/P21_REPORT_VI.md.)

The one geometric discovery this pass fixes is the door/anchor mapping. The renderer blits a
structure bottom-centred at `cx, cy + 6*s` with `pixelScale = 2*s`, so a sprite pixel is
`2*scale` world-X units wide and `2*scale/0.72` world-Y units deep. Inverting that gives the
only sprite-space position for a painted door that lands on a `SanctuaryHouseEntrances`
trigger:

    door_cx   = W/2 + (doorWorldX - obs.x) / (2*scale)
    door_rows = bottom at  H-3 - (obs.y - doorWorldY) * 0.72 / (2*scale)

For the enterable facilities (doorWorldY == obs.y, doorWorldX == obs.x-2) that is the canvas
centre and row H-3. Every door below is placed with that formula and asserted by
`doors_land_on_triggers()`; tests repeat the same arithmetic in Kotlin.

Authoring rules (see ART_DIRECTION.md / SKILL.md):
  * one key light upper-left, matching SanctuaryLight (azimuth 142deg, elevation 48deg): the
    lit face is the front/left, the side return drops one or two value steps, no mirrored light;
  * roofs are real volumes -- two planes, ridge cap, stepped eave fascia, soffit shadow on the
    wall underneath; shingle courses are regular with a controlled tone roll, never salt noise;
  * masonry is coursed: varied block widths, 1 px upper-left rim, darker lower face, mortar
    that is dark grey rather than black, quoins on load-bearing corners, a darker foundation;
  * doors get a stone surround, keystone, a real recess, plank leaf, iron furniture, threshold
    and a 2 px stoop; windows get frame, mullions, sill and a bounded glow ramp;
  * one or two profession marks per building, read at zoom 0.88;
  * a tight baked contact rim along the base instead of a broad fake shadow rectangle;
  * nothing is generated at draw time. This script is offline; the engine only decodes PNGs.

Run: python3 tools/mapforge_p21_architecture.py            # write the PNGs + manifests
     python3 tools/mapforge_p21_architecture.py --preview DIR   # + contact sheets for review
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
PREMIUM = ASSETS / "premium_slice"


# --------------------------------------------------------------------------- palette
def rgb(hex_str: str) -> int:
    return int(hex_str[1:], 16)


# graphite-blue Sanctuary stone (the P20 family, kept as the shared language)
STONE = dict(mortar="#222a30", dk="#333d45", base="#414c54", lt="#4f5a62", hi="#616d76",
             rim="#7b8790", foot="#2b333a")
# pale moonlit limestone (Summoning Hall, Registry trim) -- lighter, still cool
LIME = dict(mortar="#3b434a", dk="#4b545c", base="#59636b", lt="#69737c", hi="#7e8891",
            rim="#98a2aa", foot="#414a52")
# hard basalt (Tower Gate) -- darker, chipped, cold
BASA = dict(mortar="#151a21", dk="#1f252e", base="#2a313b", lt="#36404c", hi="#4a5563",
            rim="#61707f", foot="#1b2027")
# lime plaster / daub upper storeys
PLAS = dict(mortar="#2c3339", dk="#343c43", base="#3d454c", lt="#4a535b", hi="#5b646c",
            rim="#6d767e", foot="#2f363c")
# slate roofing
SLATE = dict(dk="#28313a", base="#333f49", lt="#3f4c57", hi="#4c5a66", ridge="#5f6d79")
# warm timber shingles (armory language, one building only)
SHING = dict(dk="#241811", base="#33241a", lt="#402e21", hi="#513a28", ridge="#66482f")
# structural oak
OAK = dict(dk="#1d150f", base="#2f2116", lt="#3f2d1d", hi="#553f28", warm="#6a5137")
# iron / steel
IRON = dict(dk="#181d22", base="#272e35", lt="#414b54", hi="#68737d", glint="#9aa6b0")
# glazing: restrained amber interior, no flat yellow slabs
AMBER = dict(deep="#5c3a16", dim="#a86b2e", base="#e0983f", lt="#f6c071", core="#ffe3ae")
# magic: cold arcane cyan (summoning / tower), alchemical teal (synthesis)
CYAN = dict(deep="#0a3f55", dim="#12657f", base="#1f93b4", lt="#35c6e6", core="#a8eef9")
TEAL = dict(deep="#0a3f3b", dim="#157a71", base="#1f9c8f", lt="#45cbb8", core="#9df3e6")
VIOL = dict(deep="#2c2340", base="#4a3a63", lt="#6a5288", core="#a98ecb")
MOSS = dict(dk="#26362b", base="#334a38", lt="#44604a")
AO = "#0f151a"
CONTACT = "#151c20"


def rgba(h: str, a: int = 255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


class Canvas:
    """Thin pixel-exact wrapper: no anti-aliasing anywhere in this pipeline."""

    def __init__(self, w: int, h: int):
        self.w, self.h = w, h
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    # -- primitives -------------------------------------------------------------
    def px(self, x, y, col, a=255):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.im.putpixel((int(x), int(y)), rgba(col, a))

    def rect(self, x0, y0, x1, y1, col, a=255):
        """Inclusive integer rectangle."""
        if x1 < x0 or y1 < y0:
            return
        self.d.rectangle([int(x0), int(y0), int(x1), int(y1)], fill=rgba(col, a))

    def hline(self, x0, x1, y, col, a=255):
        self.rect(x0, y, x1, y, col, a)

    def vline(self, x, y0, y1, col, a=255):
        self.rect(x, y0, x, y1, col, a)

    def poly(self, pts, col, a=255):
        self.d.polygon([(int(round(x)), int(round(y))) for x, y in pts], fill=rgba(col, a))

    def ellipse(self, cx, cy, rx, ry, col, a=255):
        self.d.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=rgba(col, a))

    def ring(self, cx, cy, rx, ry, col, t=1, a=255):
        for i in range(t):
            self.d.ellipse([cx - rx + i, cy - ry + i, cx + rx - i, cy + ry - i],
                           outline=rgba(col, a))

    def line(self, pts, col, width=1, a=255):
        self.d.line([(int(round(x)), int(round(y))) for x, y in pts], fill=rgba(col, a),
                    width=width)

    def mask(self, fn):
        """Restrict subsequent drawing: returns a clipped child canvas overlay."""
        return fn

    def blit(self, other: "Canvas", dx: int = 0, dy: int = 0):
        self.im.alpha_composite(other.im, (dx, dy))

    # -- reading ----------------------------------------------------------------
    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.im.getpixel((int(x), int(y)))
        return (0, 0, 0, 0)

    def opaque(self, x, y):
        return self.get(x, y)[3] > 0

    def blend(self, x, y, target_hex, amount):
        r, g, b, a = self.get(x, y)
        if a == 0:
            return
        tr, tg, tb = rgba(target_hex)[:3]
        self.im.putpixel((int(x), int(y)), (
            int(r + (tr - r) * amount), int(g + (tg - g) * amount),
            int(b + (tb - b) * amount), a))


def rr(seed):
    r = random.Random(seed)
    return r.randrange


def lerp(a, b, t):
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)


def bilinear(tl, tr, br, bl, u, v):
    top = lerp(tl, tr, u)
    bot = lerp(bl, br, u)
    return lerp(top, bot, v)


# --------------------------------------------------------------------------- masonry
def masonry(c: Canvas, quad, mat, row_h=7, block_w=13, seed=7, lit="tl", quoins=(),
            chip_chance=1, joint_down=True):
    """Coursed masonry inside a quad [tl, tr, br, bl] (parallelogram), lit from upper-left.

    Blocks vary in width, each gets a 1 px upper edge in the light tone and a darker lower
    face, and the vertical joint is drawn as mortar (not black). Two extra tone rolls put an
    occasional weathered block in, so the wall reads built rather than tiled.
    """
    tl, tr, br, bl = quad
    r = rr(seed)
    span = tr[0] - tl[0]
    depth = bl[1] - tl[1]
    n_rows = max(1, int(depth / row_h))
    for row in range(n_rows):
        v0, v1 = row / n_rows, (row + 1) / n_rows
        a = bilinear(tl, tr, br, bl, 0.0, v0)
        b = bilinear(tl, tr, br, bl, 1.0, v0)
        e = bilinear(tl, tr, br, bl, 0.0, v1)
        f = bilinear(tl, tr, br, bl, 1.0, v1)
        c.poly([a, b, f, e], mat["base"])
        # bottom mortar line of the course
        c.line([e, f], mat["mortar"], 1)
        x = -r(10)
        while x < span:
            w = block_w - 4 + r(9)
            u0, u1 = max(0.0, x / span), min(1.0, (x + w) / span)
            if (u1 - u0) * span > 3:
                p0 = bilinear(tl, tr, br, bl, u0, v0)
                p1 = bilinear(tl, tr, br, bl, u1, v0)
                q0 = bilinear(tl, tr, br, bl, u0, v1)
                q1 = bilinear(tl, tr, br, bl, u1, v1)
                roll = r(10)
                tone = mat["base"]
                if roll == 0:
                    tone = mat["dk"]
                elif roll < 3:
                    tone = mat["lt"]
                c.poly([p0, p1, q1, q0], tone)
                # upper edge catches the key light, lower edge sits in shade
                c.line([p0, p1], mat["lt"] if tone != mat["lt"] else mat["hi"], 1)
                c.line([q0, q1], mat["dk"], 1)
                if joint_down:
                    c.line([p1, q1], mat["mortar"], 1)
                if r(chip_chance + 6) == 0:
                    c.px(p1[0] - 1, p1[1] + 1, mat["dk"])
            x += w + 1
    # outer corner punctuation
    c.line([tl, tr], mat["lit" if False else "lt"] if lit == "tl" else mat["dk"], 1)


def stone_quad(c: Canvas, quad, mat, seed=5, **kw):
    masonry(c, quad, mat, seed=seed, **kw)


def quoin_column(c: Canvas, x, y0, y1, mat, seed=1, width=4, left=True):
    """Alternating long/short corner stones tying two faces together."""
    r = rr(seed)
    y = y0
    i = 0
    while y < y1:
        h = 6 + r(4)
        w = width + (1 if i % 2 == 0 else 0)
        x0 = x - w + 1 if left else x
        c.rect(x0, y, x0 + w - 1, min(y + h, y1) - 1, mat["lt"] if i % 2 else mat["base"])
        c.hline(x0, x0 + w - 1, min(y + h, y1) - 1, mat["dk"])
        c.vline(x0 + w - 1, y, min(y + h, y1) - 1, mat["mortar"])
        y += h + 1
        i += 1


# --------------------------------------------------------------------------- roofing
def shingle_plane(c: Canvas, eave_l, eave_r, ridge_l, ridge_r, ramp, seed, rows=9,
                  shingle=11, stagger=True, lip=True):
    """Fill one roof plane between an eave edge and a ridge edge in staggered courses."""
    r = rr(seed)
    for i in range(rows):
        v0, v1 = i / rows, (i + 1) / rows
        e0, e1 = lerp(eave_l, ridge_l, v0), lerp(eave_r, ridge_r, v0)
        f0, f1 = lerp(eave_l, ridge_l, v1), lerp(eave_r, ridge_r, v1)
        c.poly([e0, e1, f1, f0], ramp["base"])
        span = e1[0] - e0[0]
        if span < 2:
            continue
        x = e0[0] - (0 if (i % 2 == 0 or not stagger) else shingle // 2)
        while x < e0[0] + span:
            w = shingle - 3 + r(5)
            u0 = max(0.0, (x - e0[0]) / span)
            u1 = min(1.0, (x + w - e0[0]) / span)
            if u1 - u0 > 0.05:
                p0, p1 = lerp(e0, e1, u0), lerp(e0, e1, u1)
                q0, q1 = lerp(f0, f1, u0), lerp(f0, f1, u1)
                roll = r(11)
                tone = ramp["base"]
                if roll == 0:
                    tone = ramp["dk"]
                elif roll in (3, 4):
                    tone = ramp["lt"]
                c.poly([p0, p1, q1, q0], tone)
                if lip:
                    c.line([p0, p1], ramp["lt"], 1)
                c.line([q0, q1], ramp["dk"], 1)
            x += w + 1


def roof(c: Canvas, front_eave_l, front_eave_r, ridge_l, ridge_r, ramp, seed,
         hip_eave_l=None, hip_eave_r=None, rows=9, shingle=11, ridge_cap=True,
         eave_lip=True, soffit_band=True):
    """Main front pitch + optional right hip plane, ridge cap and eave fascia."""
    if hip_eave_r is not None:
        shingle_plane(c, front_eave_r, hip_eave_r, ridge_r, ridge_r, ramp, seed + 3,
                      rows=max(5, rows - 2), shingle=max(6, shingle - 3))
    shingle_plane(c, front_eave_l, front_eave_r, ridge_l, ridge_r, ramp, seed,
                  rows=rows, shingle=shingle)
    if ridge_cap:
        c.line([ridge_l, ridge_r], ramp["hi"], 2)
        c.line([ridge_l, ridge_r], ramp["ridge"], 1)
        # ridge ends get a small carved finial block
        for p in (ridge_l, ridge_r):
            c.rect(p[0] - 1, p[1] - 2, p[0] + 1, p[1], ramp["ridge"])
    if eave_lip:
        c.line([front_eave_l, front_eave_r], ramp["dk"], 2)
    return ramp


def window(c: Canvas, x, y, w, h, glow=AMBER, lit=1.0, shutters=False, flowerbox=False,
           mullions=(2, 2), frame=STONE, deep=True, pad=3):
    """Deep-framed mullioned window with a bounded interior glow ramp."""
    # stone surround with a lit upper-left edge and a dark under-sill
    p0 = pad
    c.rect(x - p0, y - p0, x + w + p0 - 1, y + h + p0 - 1, frame["dk"])
    if p0 > 1:
        c.rect(x - p0 + 1, y - p0 + 1, x + w + p0 - 2, y + h + p0 - 2, frame["base"])
    c.hline(x - p0, x + w + p0 - 1, y - p0, frame["lt"])
    c.vline(x - p0, y - p0, y + h + p0 - 1, frame["lt"])
    c.hline(x - p0, x + w + p0 - 1, y + h + p0 - 1, frame["dk"])
    c.vline(x + w + p0 - 1, y - p0, y + h + p0 - 1, frame["dk"])
    # recess
    c.rect(x, y, x + w - 1, y + h - 1, "#161d23")
    if lit > 0:
        # interior: darkest at the top, warmest at the bottom where a lamp would sit
        c.rect(x + 1, y + 1, x + w - 2, y + h - 2, glow["deep"])
        c.rect(x + 1, y + 2, x + w - 2, y + h - 3, glow["dim"])
        hh = h - 4
        c.rect(x + 1, y + 2 + max(0, hh // 3), x + w - 2, y + h - 3, glow["base"])
        if lit > 1.2:
            c.rect(x + 2, y + 3 + max(0, hh // 3), x + w - 3, y + h - 4, glow["lt"])
        if lit > 1.6:
            c.px(x + 2, y + h - 5, glow["core"])
    else:
        c.rect(x + 1, y + 1, x + w - 2, y + h - 2, "#1b232a")
        c.vline(x + 1, y + 1, y + h - 2, "#2a333b")
        c.hline(x + 1, x + w - 2, y + 1, "#2a333b")
    # mullions
    mx, my = mullions
    for i in range(1, mx):
        xx = x + (w * i) // mx
        c.vline(xx, y, y + h - 1, frame["dk"] if deep else frame["mortar"])
        c.vline(xx + (1 if i == mx - 1 else 0), y, y + h - 1, "#12181d")
    for j in range(1, my):
        yy = y + (h * j) // my
        c.hline(x, x + w - 1, yy, frame["dk"])
        c.hline(x, x + w - 1, yy + 1, "#12181d")
    # sill: lit top, dark underside, cast shade on the wall
    c.rect(x - p0 - 1, y + h + p0, x + w + p0, y + h + p0 + 1, frame["lt"])
    c.hline(x - p0 - 1, x + w + p0, y + h + p0, frame["hi"])
    c.hline(x - p0 - 1, x + w + p0, y + h + p0 + 2, frame["foot"])
    if flowerbox:
        c.rect(x - 2, y + h + 5, x + w + 1, y + h + 8, OAK["base"])
        c.hline(x - 2, x + w + 1, y + h + 5, OAK["lt"])
        for bx in range(x - 1, x + w, 3):
            c.px(bx, y + h + 4, MOSS["light"] if "light" in MOSS else MOSS["lt"])
            c.px(bx + 1, y + h + 3, MOSS["base"])
    if shutters:
        for sx, sgn in ((x - 8, -1), (x + w + 3, 1)):
            c.rect(sx, y - 1, sx + 4, y + h, OAK["base"])
            c.rect(sx + (1 if sgn < 0 else 0), y, sx + 3 + (0 if sgn < 0 else 1), y + h - 1,
                   OAK["lt"])
            for yy in range(y + 2, y + h - 1, 4):
                c.hline(sx + 1, sx + 3, yy, OAK["dk"])


def door(c: Canvas, cx, bottom, w=22, h=28, mat=STONE, leaf="plank", arch=True,
         steps=2, litter=True, lantern=False, keystone=True):
    """Cut-in doorway: stone surround, recess, leaf, iron furniture, threshold, stoop.

    `bottom` is the row the door leaf ends on. Two rows below it are the projecting stone
    stoop that reaches the base of the canvas, so the entrance reads walkable from the front.
    """
    x0 = cx - w // 2
    x1 = x0 + w - 1
    top = bottom - h + 1
    # outer stone surround
    c.rect(x0 - 4, top - 4, x1 + 4, bottom + 1, mat["dk"])
    c.rect(x0 - 3, top - 3, x1 + 3, bottom, mat["base"])
    c.hline(x0 - 4, x1 + 4, top - 4, mat["lt"])
    c.vline(x0 - 4, top - 4, bottom + 1, mat["lt"])
    c.hline(x0 - 4, x1 + 4, bottom + 1, mat["dk"])
    c.vline(x1 + 4, top - 4, bottom + 1, mat["dk"])
    if arch:
        # an arched head cut into the surround, one pixel shoulder each side
        for i in range(3):
            c.ellipse(cx, top + 1, w // 2 + 2 - i, 5 - i, mat["lt"] if i == 0 else mat["base"])
        c.ellipse(cx, top + 1, w // 2 - 1, 3, "#12181d")
    if keystone:
        c.rect(cx - 2, top - 6, cx + 2, top - 2, mat["lt"])
        c.hline(cx - 2, cx + 2, top - 6, mat["hi"])
        c.hline(cx - 2, cx + 2, top - 2, mat["dk"])
    # deep reveal
    c.rect(x0, top, x1, bottom, "#12181d")
    if arch:
        c.ellipse(cx, top + 1, w // 2 - 1, 4, "#12181d")
    # leaf
    if arch:
        c.ellipse(cx, top + 2, w // 2 - 2, 4, OAK["dk"])
    c.rect(x0 + 1, top + (2 if arch else 1), x1 - 1, bottom - 1, OAK["base"])
    for px in range(x0 + 2, x1 - 1, 4):
        c.vline(px, top + (3 if arch else 2), bottom - 2, OAK["lt"])
        c.vline(px + 2, top + (3 if arch else 2), bottom - 2, OAK["dk"])
    c.vline(cx, top + 2, bottom - 1, OAK["dk"])
    if leaf == "double":
        c.vline(cx - 1, top + 2, bottom - 1, OAK["dk"])
        c.vline(cx + 1, top + 2, bottom - 1, OAK["lt"])
    # iron straps + studs + ring handle
    for yy in (top + h // 3, bottom - h // 3):
        c.hline(x0 + 1, x1 - 1, yy, IRON["base"])
        c.hline(x0 + 1, x1 - 1, yy - 1, IRON["lt"])
        for sx in range(x0 + 3, x1 - 2, 5):
            c.px(sx, yy, IRON["hi"])
    hy = bottom - h // 2 - 2
    c.px(cx + w // 4, hy, IRON["hi"])
    c.px(cx + w // 4, hy + 1, IRON["glint"])
    c.px(cx + w // 4, hy + 2, IRON["base"])
    # warm spill on the threshold and a lit bottom edge of the leaf
    c.hline(x0 + 1, x1 - 1, bottom - 1, AMBER["dim"])
    if litter:
        c.px(x0 + 4, bottom, AMBER["dim"])
    # stoop: one stone row projecting south, clipped by the canvas base row
    c.rect(x0 - 4, bottom + 1, x1 + 4, bottom + 2, mat["lt"])
    c.hline(x0 - 4, x1 + 4, bottom + 1, mat["hi"])
    c.hline(x0 - 4, x1 + 4, bottom + 2, mat["foot"])
    if lantern:
        for sx, sgn in ((x0 - 8, -1), (x1 + 8, 1)):
            c.rect(sx, top - 2, sx + 1, top + 2, IRON["base"])
            c.px(sx + (1 if sgn < 0 else 0), top + 3, AMBER["base"])
            c.px(sx + (1 if sgn < 0 else 0), top + 4, AMBER["core"])
    return x0, top, x1, bottom


def stairs(c: Canvas, cx, w_top, y_top, y_bot, mat, steps=6, widen=3, cheek=True):
    """Frontal flight of steps receding up-screen: dark riser + lit tread per step."""
    w = w_top
    for s in range(steps):
        t = s / max(1, steps - 1)
        y = int(y_bot - (s + 1) * (y_bot - y_top) / steps)
        h = max(1, int((y_bot - y_top) / steps))
        ww = w + int(widen * s)
        c.rect(cx - ww // 2, y, cx + ww // 2, y + h - 1, mat["dk"])
        c.hline(cx - ww // 2, cx + ww // 2, y, mat["lt"])
        c.hline(cx - ww // 2, cx + ww // 2, y + 1, mat["base"])
        if cheek:
            c.rect(cx - ww // 2 - 3, y - 1, cx - ww // 2 - 1, y + h + 1, mat["base"])
            c.hline(cx - ww // 2 - 3, cx - ww // 2 - 1, y - 1, mat["lt"])
            c.rect(cx + ww // 2 + 1, y - 1, cx + ww // 2 + 3, y + h + 1, mat["dk"])
            c.hline(cx + ww // 2 + 1, cx + ww // 2 + 3, y - 1, mat["base"])


def timber(c: Canvas, x0, y0, x1, y1, posts=(), rails=(), braces=(), rail_h=3):
    """Structural oak frame on an axis-aligned facade: posts, rails, diagonal braces."""
    for p in posts:
        c.rect(p, y0, p + 2, y1, OAK["dk"])
        c.vline(p + 1, y0 + 1, y1 - 1, OAK["base"])
        c.vline(p, y0, y1, OAK["dk"])
    for ry in rails:
        c.rect(x0, ry, x1, ry + rail_h - 1, OAK["dk"])
        c.hline(x0, x1, ry + 1, OAK["base"])
        c.hline(x0, x1, ry, OAK["lt"])
    for (bx0, by0, bx1, by1) in braces:
        c.line([(bx0, by0), (bx1, by1)], OAK["dk"], 3)
        c.line([(bx0, by0), (bx1, by1)], OAK["base"], 1)


def chimney(c: Canvas, x, y_top, y_bot, w=14, mat=STONE, cap=True, smoke=True, soot=True):
    c.rect(x, y_top, x + w - 1, y_bot, mat["dk"])
    c.rect(x + 1, y_top + 1, x + w - 2, y_bot - 1, mat["base"])
    for yy in range(y_top + 3, y_bot, 5):
        c.hline(x + 1, x + w - 2, yy, mat["mortar"])
        c.hline(x + 1, x + w - 2, yy + 1, mat["lt"])
    c.vline(x + w - 2, y_top + 1, y_bot - 1, mat["dk"])
    c.vline(x + 1, y_top + 1, y_bot - 1, mat["lt"])
    if cap:
        c.rect(x - 2, y_top - 4, x + w + 1, y_top, mat["base"])
        c.hline(x - 2, x + w + 1, y_top - 4, mat["hi"])
        c.hline(x - 2, x + w + 1, y_top, mat["dk"])
        c.rect(x + 1, y_top - 3, x + w - 2, y_top - 2, "#141a1f")
    if soot:
        c.rect(x + 2, y_top - 3, x + w - 3, y_top - 2, "#12171c")
    if smoke:
        for i, (sx, sy, srx, sry) in enumerate((
            (x + w // 2, y_top - 8, 3, 2), (x + w // 2 - 3, y_top - 12, 4, 2),
            (x + w // 2 + 2, y_top - 16, 3, 2))):
            c.ellipse(sx, sy, srx, sry, "#38424b" if i else "#2e373f")
            c.ellipse(sx - 1, sy - 1, srx - 1, sry - 1, "#4a555f")


def contact(c: Canvas, x0, x1, base, depth=2):
    """Tight baked contact rim: dark only under the silhouette, no black rectangle."""
    for i in range(depth):
        yy = base - i
        c.blend_row = None
        for x in range(x0, x1 + 1):
            if c.opaque(x, yy):
                c.blend(x, yy, CONTACT, 0.55 - 0.15 * i)
        for x in range(x0, x1 + 1):
            if c.opaque(x, yy):
                c.blend(x, yy, AO, 0.30)
                break


def ground_ao(c: Canvas, x0, x1, base, w_strength=0.42):
    """Bake the ambient-occlusion band the renderer would otherwise fake: one tight rim."""
    for x in range(x0, x1 + 1):
        if c.opaque(x, base):
            c.blend(x, base, CONTACT, w_strength)
            c.blend(x, base - 1, AO, 0.22)


def edge_light(c: Canvas):
    """Upper-left moon kiss on the silhouette, shade on the lower-right. Subtle by design."""
    w, h = c.w, c.h
    todo = []
    for y in range(h):
        for x in range(w):
            if not c.opaque(x, y):
                continue
            up = c.opaque(x, y - 1)
            left = c.opaque(x - 1, y)
            right = c.opaque(x + 1, y)
            down = c.opaque(x, y + 1)
            if not up:
                todo.append((x, y, "#c9d4dc", 0.20))
            elif not left:
                todo.append((x, y, "#b9c5cf", 0.10))
            if not down:
                todo.append((x, y, "#070c10", 0.34))
            elif not right:
                todo.append((x, y, "#070c10", 0.20))
    for x, y, col, amt in todo:
        c.blend(x, y, col, amt)


def moss_patch(c: Canvas, x, y, n=4, seed=11, spread=14):
    """Damp moss only where the sprite is already opaque -- never a floating green fleck."""
    r = rr(seed)
    for _ in range(n):
        mx, my = x + r(spread), y - r(5)
        if not (c.opaque(mx, my) and c.opaque(mx + 1, my)):
            continue
        c.px(mx, my, MOSS["base"])
        c.px(mx + 1, my, MOSS["lt"])
        if c.opaque(mx, my + 1):
            c.px(mx, my + 1, MOSS["dk"])


def sign_board(c: Canvas, x, y, icon, w=15, h=12, hang=4):
    """Iron bracket + hanging board with a small legible profession icon."""
    c.vline(x, y - hang - 2, y - 1, IRON["base"])
    c.hline(x - w // 2, x + w - 2, y - 2, IRON["base"])
    c.px(x - w // 2, y - 3, IRON["lt"])
    c.rect(x - w // 2, y, x + w // 2, y + h - 1, OAK["base"])
    c.rect(x - w // 2 + 1, y + 1, x + w // 2 - 1, y + h - 2, OAK["lt"])
    c.hline(x - w // 2, x + w // 2, y, OAK["hi"])
    c.hline(x - w // 2, x + w // 2, y + h - 1, OAK["dk"])
    draw_icon(c, x, y + h // 2 - 1, icon)


def draw_icon(c: Canvas, cx, cy, kind):
    if kind == "book":                       # ledger: cover + pages + clasp
        c.rect(cx - 4, cy - 3, cx + 3, cy + 3, "#2b3b4c")
        c.rect(cx - 4, cy - 3, cx - 2, cy + 3, "#3d5468")
        c.hline(cx - 1, cx + 3, cy - 2, "#8fa3b5")
        c.hline(cx - 1, cx + 2, cy, "#8fa3b5")
        c.px(cx + 3, cy + 1, AMBER["base"])
    elif kind == "anvil":
        c.rect(cx - 4, cy - 1, cx + 3, cy + 1, IRON["base"])
        c.rect(cx - 3, cy - 3, cx + 2, cy - 2, IRON["lt"])
        c.rect(cx - 1, cy + 2, cx + 1, cy + 3, IRON["base"])
        c.px(cx - 5, cy, IRON["lt"])
    elif kind == "mug":                      # restaurant
        c.rect(cx - 3, cy - 3, cx + 2, cy + 3, "#8a6a45")
        c.rect(cx - 2, cy - 2, cx + 1, cy + 2, "#c9a06a")
        c.hline(cx - 3, cx + 2, cy - 3, AMBER["core"])
        c.px(cx + 4, cy - 1, "#8a6a45")
        c.px(cx + 4, cy, "#8a6a45")
    elif kind == "moon":                     # quarters / sleep
        c.ellipse(cx, cy, 4, 4, "#7f8fa6")
        c.ellipse(cx + 2, cy - 1, 3, 3, OAK["base"])
        c.px(cx - 2, cy - 2, "#c9d4dc")
    elif kind == "moon_quarter":
        c.ellipse(cx, cy, 4, 4, "#8b9bb2")
        c.ellipse(cx + 3, cy - 1, 3, 3, OAK["lt"])
    elif kind == "glyph":                    # summoning: crescent + star
        c.ellipse(cx, cy, 4, 4, CYAN["lt"])
        c.ellipse(cx + 2, cy - 1, 3, 3, OAK["base"])
        c.px(cx - 3, cy - 3, CYAN["core"])
    elif kind == "retort":                   # synthesis: flask + bubble
        c.poly([(cx - 1, cy - 4), (cx + 1, cy - 4), (cx + 1, cy - 1), (cx - 1, cy - 1)], "#a98ec8")
        c.ellipse(cx, cy + 1, 3, 3, TEAL["lt"])
        c.ellipse(cx, cy + 1, 2, 2, TEAL["core"])
        c.px(cx - 2, cy - 2, "#cbb7e0")
    elif kind == "sword":
        c.line([(cx - 3, cy + 3), (cx + 3, cy - 3)], "#aab6c0", 2)
        c.hline(cx - 4, cx - 1, cy + 1, IRON["base"])
        c.px(cx + 3, cy - 4, "#dbe6ee")
    elif kind == "shield":
        c.poly([(cx - 3, cy - 3), (cx + 3, cy - 3), (cx + 2, cy + 2), (cx, cy + 4),
                (cx - 2, cy + 2)], "#4a5568")
        c.poly([(cx - 2, cy - 2), (cx + 1, cy - 2), (cx, cy + 2), (cx - 2, cy + 1)], "#6b7a90")
        c.px(cx, cy, AMBER["base"])
    elif kind == "gear":
        c.ellipse(cx, cy, 3, 3, "#7c8b98")
        c.ellipse(cx, cy, 1, 1, "#2b333a")
        for dx, dy in ((0, -4), (0, 4), (-4, 0), (4, 0)):
            c.px(cx + dx, cy + dy, "#7c8b98")


# --------------------------------------------------------------------------- buildings
def side_return(c: Canvas, x, top_l, top_r, base, w, mat, recede=0.30):
    """Right-hand wall receding into depth: parallelogram, two value steps darker."""
    bot_l, bot_r = base, base - w * recede
    c.poly([(x, top_l), (x + w, top_r), (x + w, bot_r), (x, bot_l)], mat["dk"])
    rows = max(3, int((base - top_l) / 8))
    for i in range(1, rows):
        t = i / rows
        y0 = top_l + (bot_l - top_l) * t
        y1 = top_r + (bot_r - top_r) * t
        c.line([(x, y0), (x + w, y1)], mat["mortar"], 1)
    for i in range(1, 4):
        xx = x + w * i / 4
        c.line([(xx, top_l + (top_r - top_l) * i / 4),
                (xx, bot_l - (bot_l - bot_r) * i / 4)], mat["mortar"])
    c.line([(x, top_l), (x + w, top_r)], mat["lt"], 1)
    c.line([(x + w, top_r), (x + w, bot_r)], "#10161a", 1)


def hip_roof(c: Canvas, eave_l, eave_r, ridge_l, ridge_r, hip_tip, ramp, seed,
             rows=9, shingle=12, finial=False):
    """Front pitch + right hip return, with eave fascia, hip arris and ridge cap."""
    shingle_plane(c, eave_r, hip_tip, ridge_r, ridge_r, ramp, seed + 7,
                  rows=max(4, rows - 3), shingle=max(6, shingle - 4))
    shingle_plane(c, eave_l, eave_r, ridge_l, ridge_r, ramp, seed, rows=rows, shingle=shingle)
    c.line([eave_r, hip_tip], ramp["dk"], 1)
    c.line([eave_r, ridge_r], ramp["hi"], 1)
    c.line([ridge_l, ridge_r], ramp["hi"], 2)
    c.line([(ridge_l[0], ridge_l[1] - 2), (ridge_r[0], ridge_r[1] - 2)], ramp["ridge"], 1)
    c.line([eave_l, eave_r], ramp["lt"], 1)
    c.line([(eave_l[0], eave_l[1] + 1), (eave_r[0], eave_r[1] + 1)], ramp["dk"], 2)
    if finial:
        fx, fy = ridge_l
        c.vline(fx, fy - 8, fy - 1, IRON["base"])
        c.poly([(fx, fy - 8), (fx + 8, fy - 6), (fx, fy - 4)], "#8a6b34")
        c.px(fx + 1, fy - 7, "#c9a961")


def soffit(c: Canvas, eave_l, eave_r, band=3):
    """Dark band under the eave falling on the wall, plus a soft second step."""
    c.poly([(eave_l[0], eave_l[1] + 2), (eave_r[0], eave_r[1] + 2),
            (eave_r[0], eave_r[1] + 2 + band), (eave_l[0], eave_l[1] + 2 + band)],
           "#161d23")
    c.poly([(eave_l[0], eave_l[1] + 2 + band), (eave_r[0], eave_r[1] + 2 + band),
            (eave_r[0], eave_r[1] + 3 + band), (eave_l[0], eave_l[1] + 3 + band)],
           "#1f272e")


def two_face_block(c: Canvas, base, x0, xm, x1, eave_y, jetty_y, stone, plaster,
                   rise_l=10, rise_r=18, drop_r=22, posts=3, brace="bay"):
    """Two-storey oblique mass: lit front face + shaded receding right face.

    Vertical order on the lit face: eave line -> jettied plaster/timber storey -> jetty beam
    -> coursed stone ground floor -> baked contact rim on the base row. The right face recedes
    up-right, which is what gives the volume in this camera that a symmetrical gable loses.
    Returns the usable window bands: (upper, lower) for the lit face and (upper, lower) for
    the shaded face.
    """
    y0l, y0m = eave_y, eave_y + rise_l          # top edges of the two faces
    y1 = base - 2                               # ground line of the lit face
    y0r, y1r = y0m - rise_r, y1 - drop_r
    jl = jetty_y                                 # jetty line on the lit face
    jr = jetty_y - 8                             # jetty line on the receding face

    # ---- lit face: plaster storey ---------------------------------------------------
    c.poly([(x0, y0l), (xm, y0m), (xm, jl), (x0, jl - 2)], plaster["base"])
    masonry(c, [(x0, y0l), (xm, y0m), (xm, jl), (x0, jl - 2)], plaster, row_h=8,
            block_w=20, seed=4101, joint_down=False)
    c.line([(x0, y0l), (xm, y0m)], plaster["lt"], 1)
    # ---- lit face: coursed stone ground floor --------------------------------------
    c.poly([(x0, jl), (xm, jl + 2), (xm, y1), (x0, y1)], stone["base"])
    masonry(c, [(x0, jl), (xm, jl + 2), (xm, y1), (x0, y1)], stone, row_h=7, block_w=15,
            seed=4111)
    # quoin stack on the lit left corner
    quoin_column(c, x0, jl + 6, y1 - 1, stone, seed=4112, width=4)
    # ---- right face (shaded, receding) ---------------------------------------------
    c.poly([(xm, y0m), (x1, y0r), (x1, jr), (xm, jr + 2)], plaster["dk"])
    for i in range(1, 4):
        xx = xm + (x1 - xm) * i / 4
        c.line([(xx, y0m + (y0r - y0m) * i / 4), (xx, jr + 2 - 8 * i / 4)], plaster["mortar"])
    c.poly([(xm, jr + 2), (x1, jr), (x1, y1r), (xm, y1)], stone["dk"])
    rows = max(3, int((y1 - jr) / 8))
    for i in range(1, rows):
        t = i / rows
        c.line([(xm, jr + 2 + (y1 - jr - 2) * t), (x1, jr + (y1r - jr) * t)],
               stone["mortar"], 1)
        c.line([(xm, jr + 1 + (y1 - jr - 2) * t), (x1, jr - 1 + (y1r - jr) * t)],
               stone["base"], 1)
    for i in range(1, 4):
        xx = xm + (x1 - xm) * i / 4
        c.line([(xx, jr + 2 - 8 * i / 4), (xx, y1 + (y1r - y1) * i / 4)], stone["mortar"], 1)
    # corner edges
    c.line([(xm, y0m), (xm, y1)], stone["mortar"], 1)
    c.line([(xm - 1, y0m), (xm - 1, y1)], plaster["hi"], 1)
    c.line([(xm + 1, y0m), (xm + 1, y1)], "#10161a", 1)
    # ---- jetty beam: dark soffit band with joist ends, over the stone storey --------
    c.poly([(x0 - 3, jl), (xm + 3, jl + 2), (xm + 3, jl + 5), (x0 - 3, jl + 3)], "#141a1f")
    for bx in range(x0 - 1, xm + 1, 9):
        yy = jl + int((bx - x0) * 0.06) + 1
        c.rect(bx, yy, bx + 2, yy + 1, OAK["base"])
        c.px(bx, yy + 2, OAK["lt"])
    # ---- bake the contact rim on the base row --------------------------------------
    c.rect(x0, y1 + 1, xm, base, stone["foot"])
    c.hline(x0, xm, y1 + 1, stone["dk"])
    c.hline(x0, xm, base, "#151c21")
    c.poly([(xm, y1), (x1, y1r), (x1, y1r + 3), (xm, base)], "#1b2127")
    c.line([(xm, y1), (x1, y1r)], "#12181d", 1)
    # ---- oak frame on the lit plaster storey ---------------------------------------
    span = xm - x0
    cols = [x0 + 3 + span * i // max(1, posts) for i in range(posts)] + [xm - 3]
    for px in cols:
        top_y = y0l + rise_l * (px - x0) / max(1, span)
        c.line([(px, top_y + 1), (px, jl + 3)], OAK["dk"], 3)
        c.line([(px + 1, top_y + 2), (px + 1, jl + 2)], OAK["base"], 1)
    ry0 = y0l + rise_l * 0 + 26
    c.line([(x0 + 1, ry0), (xm - 1, ry0 + 8)], OAK["dk"], 3)
    c.line([(x0 + 1, ry0 - 1), (xm - 1, ry0 + 7)], OAK["lt"], 1)
    if brace == "bay":
        bx0, bx1 = cols[0] + 4, cols[1] - 4
        c.line([(bx0, jl + 1), (bx1, ry0 + rise_l + 3)], OAK["dk"], 3)
        c.line([(bx0 + 1, jl), (bx1 - 1, ry0 + rise_l + 2)], OAK["base"], 1)
    # wall plate under the eave
    c.line([(x0, y0l - 1), (xm, y0m - 1)], OAK["dk"], 2)
    return (y0l + 6, jl - 5), (jl + 4, y1 - 4), (y0r + 8, jr - 4), (jr + 4, y1r - 3)


def _roof_over(c: Canvas, x0, xm, x1, eave_y, rise_l, ramp, seed, ridge_y, hip_drop,
               shingle=12, rows=10, finial=False):
    """Roof that fits a two-face block: front pitch + right hip return."""
    eave_l = (x0 - 8, eave_y - 2)
    eave_r = (xm + 6, eave_y + rise_l)
    ridge_l = (x0 + 28, ridge_y)
    ridge_r = (xm + 16, ridge_y - 6)
    hip_tip = (x1 + 2, eave_r[1] - hip_drop)
    hip_roof(c, eave_l, eave_r, ridge_l, ridge_r, hip_tip, ramp, seed=seed, rows=rows,
             shingle=shingle, finial=finial)
    soffit(c, eave_l, eave_r, band=4)
    c.poly([(eave_r[0], eave_r[1]), hip_tip, (hip_tip[0], hip_tip[1] + 3),
            (eave_r[0], eave_r[1] + 3)], "#161d23")
    c.line([eave_l, ridge_l], ramp["dk"], 2)
    return eave_l, eave_r, ridge_l, ridge_r, hip_tip


def build_hero_registry() -> Image.Image:
    """188x166 -- civic archive hall: stone ground floor, jettied plaster storey, slate hip roof.

    Human scale rather than another portal: bronze plaque row, ledger lectern, bench, a ridge
    pennant. Door at canvas centre, leaf bottom on the H-3 trigger row.
    """
    W, H = 188, 166
    c = Canvas(W, H)
    base = H - 1
    door_cx, door_bottom = W // 2, base - 2

    x0, xm, x1 = 10, 112, 180
    eave_y, jetty = 76, 124
    up, low, rup, rlow = two_face_block(c, base, x0, xm, x1, eave_y, jetty,
                                        STONE, PLAS, rise_l=10, rise_r=18, drop_r=22,
                                        posts=3)
    _roof_over(c, x0, xm, x1, eave_y, 10, SLATE, seed=3131, ridge_y=24, hip_drop=20,
               rows=8, shingle=13, finial=True)
    chimney(c, 94, 16, 44, w=15, mat=STONE, smoke=False)
    # civic bell turret on the ridge: the registry's silhouette marker, visible at 0.36
    tur_x = x0 + 58
    c.rect(tur_x - 13, 12, tur_x + 13, 32, PLAS["base"])
    c.rect(tur_x - 12, 13, tur_x + 12, 31, PLAS["lt"])
    for yy in range(14, 31, 5):
        c.hline(tur_x - 12, tur_x + 12, yy, PLAS["mortar"])
    c.rect(tur_x - 15, 7, tur_x + 15, 12, PLAS["dk"])
    c.hline(tur_x - 15, tur_x + 15, 7, PLAS["hi"])
    c.poly([(tur_x - 17, 8), (tur_x, 1), (tur_x + 17, 8)], SLATE["base"])
    c.line([(tur_x - 17, 8), (tur_x, 1)], SLATE["hi"])
    c.px(tur_x, 2, "#ffffff")
    # louvred bell opening + bronze clock face below it
    c.rect(tur_x - 6, 14, tur_x + 6, 23, PLAS["dk"])
    for yy in range(15, 22, 2):
        c.hline(tur_x - 5, tur_x + 5, yy, OAK["base"])
    c.ellipse(tur_x, 28, 6, 5, "#4a5560")
    c.ellipse(tur_x, 28, 5, 4, "#9aa6b0")
    c.ellipse(tur_x, 28, 4, 3, "#c6cfd6")
    c.vline(tur_x, 25, 28, "#2b333a")
    c.px(tur_x + 2, 27, "#2b333a")

    window(c, 22, up[0] + 4, 12, 16, glow=AMBER, lit=1.2, pad=2, flowerbox=True)
    window(c, 58, up[0] + 7, 12, 16, glow=AMBER, lit=0.0, pad=2, flowerbox=True)
    window(c, 84, up[0] + 10, 12, 16, glow=AMBER, lit=1.3, pad=2)
    window(c, 138, rup[0] + 4, 12, 15, glow=AMBER, lit=1.1, pad=2)
    window(c, 132, low[0] + 6, 13, 16, glow=AMBER, lit=1.4)
    door(c, door_cx, door_bottom, w=22, h=28, mat=STONE, arch=True, lantern=True)
    # bronze plaque row: three small tablets, the civic marker
    for i, px in enumerate((24, 36, 48)):
        py = low[0] + 6
        c.rect(px, py, px + 9, py + 6, "#4d3c19")
        c.rect(px + 1, py + 1, px + 8, py + 5, "#7a5f2e")
        c.hline(px + 2, px + 7, py + 3, "#b3924f")
    # ledger lectern + stool
    c.rect(24, base - 20, 42, base - 17, OAK["dk"])
    c.rect(25, base - 19, 41, base - 18, "#3d5468")
    for xx in (27, 31, 35, 39):
        c.px(xx, base - 18, "#8fa3b5")
    c.rect(27, base - 15, 29, base - 1, OAK["base"])
    c.rect(37, base - 15, 39, base - 1, OAK["base"])
    # hanging ledger sign on an iron bracket (the registry's profession mark)
    sign_board(c, 150, base - 40, "book", w=16, h=13)
    c.rect(160, base - 30, 172, base - 14, "#3b4f61")
    c.rect(161, base - 29, 171, base - 15, "#4d657a")
    for yy in range(base - 27, base - 16, 3):
        c.hline(163, 169, yy, "#8fa3b5")
    # stone bench
    c.rect(112, base - 10, 130, base - 8, STONE["lt"])
    c.hline(112, 130, base - 10, STONE["hi"])
    c.rect(114, base - 7, 116, base - 1, STONE["base"])
    c.rect(126, base - 7, 128, base - 1, STONE["base"])
    moss_patch(c, x0 + 4, base - 4, n=3, seed=3141, spread=12)

    ground_ao(c, 4, W - 4, base, 0.40)
    edge_light(c)
    return c.im


def build_hero_quarters() -> Image.Image:
    """200x168 -- lived-in dormitory: warm shingle hip roof, dormer, jetty, porch.

    Quieter than the registry: lower ridge, roof vent, water barrel, boot scraper, bench.
    Door at canvas centre, leaf bottom on the H-3 trigger row (823,790).
    """
    W, H = 200, 168
    c = Canvas(W, H)
    base = H - 1
    door_cx, door_bottom = W // 2, base - 2

    x0, xm, x1 = 12, 118, 186
    eave_y, jetty = 82, 130
    up, low, rup, rlow = two_face_block(c, base, x0, xm, x1, eave_y, jetty,
                                        STONE, PLAS, rise_l=10, rise_r=16, drop_r=20,
                                        posts=3)
    _roof_over(c, x0, xm, x1, eave_y, 10, SHING, seed=3521, ridge_y=26, hip_drop=22,
               rows=8, shingle=12)
    # lean-to wing on the left: a smaller mono-pitch roof, the shape that says "dormitory
    # that grew over time" rather than one symmetrical hall
    wx0, wx1, wy = 6, x0 + 6, 112
    c.rect(wx0, wy, wx1, base - 4, STONE["base"])
    masonry(c, [(wx0, wy + 3), (wx1, wy), (wx1, base - 4), (wx0, base - 4)], STONE,
            row_h=7, block_w=11, seed=3551)
    shingle_plane(c, (wx0 - 1, wy + 4), (wx1 + 2, wy + 1), (wx0 + 1, wy - 12), (wx1 + 2, wy - 15),
                  SHING, seed=3552, rows=6, shingle=9)
    c.line([(wx0 - 1, wy + 4), (wx1 + 2, wy + 1)], SHING["dk"], 2)
    c.line([(wx0, wy + 6), (wx1 + 2, wy + 3)], "#141a1f", 1)
    window(c, wx0 + 5, wy + 14, 9, 11, glow=AMBER, lit=1.1, pad=1)
    c.rect(wx1 - 2, wy - 14, wx1 + 1, base - 4, OAK["dk"])
    # dormer through the front pitch
    dx0, dx1 = 62, 88
    c.poly([(dx0, 46), (dx1, 50), (dx1, 68), (dx0, 64)], PLAS["base"])
    c.poly([(dx0 - 3, 47), ((dx0 + dx1) // 2, 30), (dx1 + 3, 51)], SHING["base"])
    c.line([(dx0 - 3, 47), ((dx0 + dx1) // 2, 30)], SHING["hi"], 1)
    c.line([((dx0 + dx1) // 2, 30), (dx1 + 3, 51)], SHING["dk"], 1)
    window(c, dx0 + 6, 50, 11, 13, glow=AMBER, lit=1.6, mullions=(2, 1), pad=1)
    chimney(c, 28, 20, 62, w=13, mat=STONE, smoke=True)

    window(c, 22, up[0] + 4, 12, 16, glow=AMBER, lit=1.2, shutters=True, pad=2)
    window(c, 54, up[0] + 7, 12, 16, glow=AMBER, lit=1.3, shutters=True, pad=2)
    window(c, 86, up[0] + 10, 12, 16, glow=AMBER, lit=0.0, pad=2)
    window(c, 140, rup[0] + 4, 12, 15, glow=AMBER, lit=1.0, pad=2)
    # louvred roof vent on the shaded face
    c.rect(158, rup[0] + 2, 168, rup[0] + 14, OAK["dk"])
    c.rect(159, rup[0] + 3, 167, rup[0] + 13, OAK["base"])
    for yy in range(rup[0] + 4, rup[0] + 13, 2):
        c.hline(159, 167, yy, OAK["lt"])
    window(c, 132, low[0] + 6, 12, 15, glow=AMBER, lit=0.0, flowerbox=True)
    door(c, door_cx, door_bottom, w=24, h=30, mat=STONE, arch=True, lantern=True)
    # porch: shallow shingle canopy above the door head, posts flanking it
    px0, px1 = door_cx - 22, door_cx + 22
    cy0 = door_bottom - 30 - 12
    shingle_plane(c, (px0 - 2, cy0 + 9), (px1 + 3, cy0 + 12), (px0 + 5, cy0 - 1),
                  (px1 - 5, cy0 + 2), SHING, seed=3531, rows=4, shingle=8)
    c.line([(px0 - 2, cy0 + 9), (px1 + 3, cy0 + 12)], SHING["dk"], 2)
    c.line([(px0 - 1, cy0 + 11), (px1 + 2, cy0 + 14)], "#141a1f", 1)
    c.line([(px0 - 2, cy0 + 9), (px0 + 5, cy0 - 1)], SHING["hi"], 1)
    for px in (px0 - 1, px1 + 1):
        c.rect(px, cy0 + 10, px + 2, base - 6, OAK["dk"])
        c.vline(px + 1, cy0 + 11, base - 7, OAK["base"])
    # lived-in props
    c.rect(px0 - 13, base - 4, px0 - 6, base - 2, IRON["base"])
    c.hline(px0 - 13, px0 - 6, base - 4, IRON["lt"])
    c.rect(44, base - 13, 64, base - 11, OAK["lt"])
    c.rect(46, base - 10, 48, base - 1, OAK["dk"])
    c.rect(60, base - 10, 62, base - 1, OAK["dk"])
    c.rect(170, base - 22, 182, base - 1, OAK["base"])
    c.vline(170, base - 22, base - 1, OAK["lt"])
    c.hline(170, 182, base - 16, IRON["base"])
    c.hline(170, 182, base - 7, IRON["base"])
    sign_board(c, 150, base - 34, "moon", w=14, h=11)
    moss_patch(c, 16, base - 4, n=4, seed=3541, spread=14)

    ground_ao(c, 6, W - 6, base, 0.40)
    edge_light(c)
    return c.im


def build_restaurant() -> Image.Image:
    """206x172 -- dining hall: two-face mass, hipped slate roof, deep warm awning.

    The warmest of the three, but bounded: a canvas awning, terrace lanterns and dining-window
    glow instead of flat yellow panels. Door at canvas centre, leaf bottom on the H-3 row.
    """
    W, H = 206, 172
    c = Canvas(W, H)
    base = H - 1
    door_cx, door_bottom = W // 2, base - 2

    x0, xm, x1 = 10, 124, 192
    eave_y, jetty = 80, 128
    up, low, rup, rlow = two_face_block(c, base, x0, xm, x1, eave_y, jetty,
                                        STONE, PLAS, rise_l=10, rise_r=18, drop_r=22,
                                        posts=4)
    chimney(c, 18, 26, 88, w=22, mat=STONE, smoke=True)
    _roof_over(c, x0, xm, x1, eave_y, 10, SLATE, seed=3621, ridge_y=24, hip_drop=22,
               rows=8, shingle=14)
    # roof dormer
    c.poly([(96, 44), (122, 48), (122, 66), (96, 62)], PLAS["base"])
    c.poly([(93, 45), (109, 28), (125, 49)], SLATE["base"])
    c.line([(93, 45), (109, 28)], SLATE["hi"])
    window(c, 104, 48, 11, 13, glow=AMBER, lit=1.7, mullions=(2, 1), pad=1)

    window(c, 22, up[0] + 4, 12, 15, glow=AMBER, lit=1.1, pad=2)
    window(c, 58, up[0] + 7, 12, 15, glow=AMBER, lit=0.0, pad=2)
    window(c, 86, up[0] + 10, 12, 15, glow=AMBER, lit=1.3, pad=2)
    window(c, 152, rup[0] + 4, 12, 15, glow=AMBER, lit=1.2, pad=2)

    # deep canvas awning across the terrace (warm ochre + pale stripe, top-lit)
    ax0, ax1, ay = 26, 166, 132
    for i, ax in enumerate(range(ax0, ax1, 13)):
        warm = i % 2 == 0
        top_col = "#8a6a3c" if warm else "#8d8b80"
        body_col = "#6d5330" if warm else "#6f6d64"
        c.poly([(ax, ay), (ax + 6, ay - 7), (min(ax + 13, ax1), ay),
                (min(ax + 13, ax1), ay + 4), (ax, ay + 4)], body_col)
        c.line([(ax + 1, ay - 6), (min(ax + 12, ax1) - 1, ay - 6)], top_col, 1)
        c.px(ax + 2, ay - 5, "#c9b48c" if warm else "#c3c1b4")
    c.hline(ax0, ax1, ay - 8, "#a98f60")
    c.hline(ax0, ax1, ay + 4, "#2b2620")
    c.rect(ax0, ay + 5, ax1, ay + 6, "#1b1a15")
    for px in (ax0 + 2, ax1 - 4):
        c.rect(px, ay + 5, px + 2, base - 6, OAK["dk"])
        c.vline(px + 1, ay + 6, base - 7, OAK["lt"])

    # projecting entrance gable (porch bay): breaks the long facade into a front door
    gx0, gx1 = door_cx - 34, door_cx + 34
    c.poly([(gx0, 128), (gx1, 128), (gx1, 158), (gx0, 158)], PLAS["base"])
    timber(c, gx0, 128, gx1, 158, posts=(gx0 + 1, gx1 - 2), rails=(130, 154), rail_h=3)
    c.poly([(gx0 - 6, 128), ((gx0 + gx1) // 2, 104), (gx1 + 6, 128)], SLATE["base"])
    c.line([(gx0 - 6, 128), ((gx0 + gx1) // 2, 104)], SLATE["hi"])
    c.line([((gx0 + gx1) // 2, 104), (gx1 + 6, 128)], SLATE["dk"])
    c.line([(gx0 - 6, 128), (gx1 + 6, 128)], SLATE["dk"], 2)
    c.px((gx0 + gx1) // 2, 103, LIME["hi"])
    door(c, door_cx, door_bottom, w=26, h=32, mat=STONE, arch=False, lantern=True,
         leaf="double")
    window(c, 36, base - 34, 20, 22, glow=AMBER, lit=1.7, mullions=(3, 2))
    window(c, 146, base - 32, 16, 18, glow=AMBER, lit=1.2, mullions=(2, 2))
    # terrace props
    c.rect(176, base - 28, 189, base - 1, OAK["base"])
    c.vline(176, base - 28, base - 1, OAK["lt"])
    c.hline(176, 189, base - 21, IRON["base"])
    c.hline(176, 189, base - 9, IRON["base"])
    c.rect(28, base - 15, 46, base - 13, OAK["lt"])
    c.rect(30, base - 12, 32, base - 1, OAK["dk"])
    c.rect(42, base - 12, 44, base - 1, OAK["dk"])
    c.vline(ax0 - 3, base - 32, base - 28, IRON["base"])
    c.rect(ax0 - 5, base - 27, ax0 - 1, base - 22, IRON["dk"])
    c.rect(ax0 - 4, base - 26, ax0 - 2, base - 23, AMBER["base"])
    c.px(ax0 - 3, base - 25, AMBER["core"])
    sign_board(c, 160, base - 56, "mug", w=15, h=12)
    moss_patch(c, 62, base - 4, n=4, seed=3631, spread=20)

    ground_ao(c, 4, W - 4, base, 0.40)
    edge_light(c)
    return c.im


# ------------------------------------------------------------------- hero landmarks
def column(c: Canvas, cx, y_top, y_bot, w=10, mat=LIME, capital=True, base=True):
    """Fluted classical column: lit left third, shaded right, abacus + base mouldings."""
    x0 = cx - w // 2
    c.rect(x0, y_top, x0 + w - 1, y_bot, mat["base"])
    c.rect(x0, y_top, x0 + w // 3 - 1, y_bot, mat["lt"])
    c.vline(x0, y_top, y_bot, mat["hi"])
    c.vline(x0 + w - 2, y_top, y_bot, mat["dk"])
    # flutes
    for fx in range(x0 + 2, x0 + w - 2, 3):
        c.vline(fx, y_top + 2, y_bot - 2, mat["dk"])
    if capital:
        c.rect(x0 - 3, y_top - 4, x0 + w + 2, y_top - 2, mat["base"])
        c.rect(x0 - 4, y_top - 6, x0 + w + 3, y_top - 4, mat["lt"])
        c.hline(x0 - 4, x0 + w + 3, y_top - 6, mat["hi"])
        c.hline(x0 - 3, x0 + w + 2, y_top - 1, mat["dk"])
    if base:
        c.rect(x0 - 2, y_bot - 4, x0 + w + 1, y_bot - 1, mat["base"])
        c.hline(x0 - 2, x0 + w + 1, y_bot - 4, mat["lt"])
        c.rect(x0 - 3, y_bot - 5, x0 + w + 2, y_bot - 5, mat["lt"])


def arcane_ring(c: Canvas, cx, cy, rx, ry, ramp, marks=8, thickness=2, core=True):
    """Foreshortened inlaid rune ring on the ground plane (ellipse, camera-squashed)."""
    for i in range(thickness):
        c.ring(cx, cy, rx - i, ry - i, ramp["base"] if i else ramp["lt"])
    c.ring(cx, cy, int(rx * 0.62), int(ry * 0.62), ramp["deep"], 1)
    if core:
        c.ellipse(cx, cy, max(2, rx // 5), max(1, ry // 5), ramp["lt"])
        c.ellipse(cx, cy, max(1, rx // 8), max(1, ry // 8), ramp["core"])
    for m in range(marks):
        ang = m * math.tau / marks
        mx = cx + math.cos(ang) * rx * 0.82
        my = cy + math.sin(ang) * ry * 0.82
        c.px(mx, my, ramp["lt"])
        c.px(mx, my - 1, ramp["core"] if m % 2 == 0 else ramp["base"])
        c.px(mx + 1, my, ramp["base"])


def build_summoning_hall() -> Image.Image:
    """268x206 -- the seat of the summoning system: terraced ritual plaza + temple front.

    Contract: PropKind.FACILITY_SUMMONING_HALL, obstacle (2430,2005), scale 1.08, drawn
    bottom-centred at cx, cy+6s with pixelScale 2s. The interaction centre is the zone at
    (2380,1930), which inverts to sprite (111,178) -- so the glowing rune stage is authored
    there, on the plaza in front of the hall, not centred on the canvas.
    """
    W, H = 268, 206
    c = Canvas(W, H)
    base = H - 1
    cx = W // 2
    stage_x, stage_y = 111, 176

    # ---- terraced plaza -------------------------------------------------------------
    tiers = [(124, 40, base - 2), (112, 33, base - 14), (99, 26, base - 26)]
    for i, (rx, ry, cyy) in enumerate(tiers):
        top = LIME["lt"] if i == 0 else (LIME["base"] if i == 1 else LIME["dk"])
        c.ellipse(cx, cyy + 3, rx, ry, LIME["foot"])
        c.ellipse(cx, cyy, rx, ry, LIME["dk"])
        c.ellipse(cx, cyy - 2, rx - 2, ry - 1, top)
        # front rim: lit band + shadow under the lip
        for x in range(cx - rx + 2, cx + rx - 1):
            dy = ry - 2 - int((ry - 2) * abs(x - cx) / max(1, rx))
            if 0 <= dy:
                c.blend(x, cyy - 2 + dy, LIME["hi"], 0.35)
                c.blend(x, cyy - 1 + dy, LIME["foot"], 0.45)
        # radial slab joints across the visible front half
        seg = 22
        for s_i in range(seg):
            a0 = math.pi * (0.05 + 0.9 * s_i / seg)
            x0 = cx + math.cos(math.pi - a0) * (rx - 4)
            y0 = cyy - 2 + math.sin(a0) * (ry - 3)
            x1 = cx + math.cos(math.pi - a0) * (rx - 16)
            y1 = cyy - 2 + math.sin(a0) * (ry - 5) * 0.58
            c.line([(x0, y0), (x1, y1)], LIME["mortar"], 1)
    # contact rim on the base row
    for x in range(cx - 124, cx + 124):
        if c.opaque(x, base):
            c.blend(x, base, CONTACT, 0.42)
            c.blend(x, base - 1, AO, 0.18)

    # ---- ritual stage: inlaid rune ring at the interaction point -------------------
    c.ellipse(stage_x, stage_y + 2, 56, 19, LIME["dk"])
    c.ellipse(stage_x, stage_y, 54, 17, LIME["base"])
    c.ellipse(stage_x, stage_y - 1, 51, 15, LIME["lt"])
    c.ring(stage_x, stage_y - 1, 51, 15, LIME["mortar"])
    arcane_ring(c, stage_x, stage_y - 1, 38, 11, CYAN, marks=8, thickness=2)

    # ---- temple: podium, cella wall, colonnade, entablature, roof --------------------
    thx0, thx1 = 50, 218
    pod_top = 134
    # podium (three shallow steps in front of the stylobate)
    for i in range(3):
        yy = pod_top + 2 + i * 4
        c.rect(thx0 - 10 + i * 3, yy, thx1 + 10 - i * 3, yy + 3, LIME["base"] if i % 2 else LIME["lt"])
        c.hline(thx0 - 10 + i * 3, thx1 + 10 - i * 3, yy, LIME["hi"])
        c.hline(thx0 - 10 + i * 3, thx1 + 10 - i * 3, yy + 3, LIME["foot"])
    # cast shadow of the colonnade onto the podium
    c.rect(thx0 - 4, pod_top, thx1 + 4, pod_top + 2, "#1a2128")

    # cella: a recessed dark wall so the columns read as standing *in front* of something
    cell_x0, cell_x1 = thx0 + 16, thx1 - 16
    c.rect(cell_x0, 68, cell_x1, pod_top, LIME["dk"])
    masonry(c, [(cell_x0, 74), (cell_x1, 74), (cell_x1, pod_top), (cell_x0, pod_top)],
            LIME, row_h=7, block_w=15, seed=5101)
    for x in range(cell_x0, cell_x1, 12):     # darkened so it sits behind the columns
        for y in range(74, pod_top):
            c.blend(x, y, "#0e141a", 0.30)

    # central arched portal through the cella
    pcx = cx - 4
    p_top, p_bot = 84, pod_top
    c.rect(pcx - 24, p_top + 10, pcx + 24, p_bot, LIME["dk"])
    c.ellipse(pcx, p_top + 10, 24, 14, LIME["dk"])
    c.rect(pcx - 21, p_top + 10, pcx + 21, p_bot - 1, "#0c1218")
    c.ellipse(pcx, p_top + 10, 21, 12, "#0c1218")
    # inner light: cold arcane pool with a bright floor spill
    c.ellipse(pcx, p_top + 16, 18, 10, CYAN["deep"])
    c.ellipse(pcx, p_top + 18, 15, 8, CYAN["base"])
    c.ellipse(pcx, p_top + 20, 11, 6, CYAN["lt"])
    c.ellipse(pcx, p_top + 22, 6, 4, CYAN["core"])
    c.ellipse(pcx, p_bot - 2, 20, 4, CYAN["deep"])
    c.ellipse(pcx, p_bot - 3, 13, 3, CYAN["base"])
    # voussoirs
    for a in range(11):
        ang = math.pi + a * math.pi / 10
        vx = pcx + math.cos(ang) * 25
        vy = p_top + 10 + math.sin(ang) * 15
        c.rect(vx - 2, vy - 2, vx + 2, vy + 2, LIME["lt"])
        c.hline(vx - 2, vx + 2, vy - 2, LIME["hi"])
    # bronze doors swung open against the jambs
    for sgn in (-1, 1):
        jx = pcx + sgn * 20
        c.rect(jx - 3 if sgn < 0 else jx, p_top + 14, jx + 1 if sgn < 0 else jx + 3,
               p_bot - 1, "#5a441f")
        for yy in range(p_top + 16, p_bot - 2, 6):
            c.px(jx + sgn, yy, "#8a6b34")

    # six columns across the front, standing proud of the cella
    for i in range(6):
        colx = thx0 + 8 + i * ((thx1 - thx0 - 16) // 5)
        column(c, colx, 80, pod_top - 1, w=11, mat=LIME)
    # column shadows raking right-down across the cella
    for i in range(6):
        colx = thx0 + 8 + i * ((thx1 - thx0 - 16) // 5)
        for yy in range(82, pod_top - 2, 3):
            c.blend(colx + 8 + (yy - 82) // 6, yy, "#080d12", 0.14)

    # entablature: architrave, frieze with triglyphs, cornice
    c.rect(thx0 - 6, 62, thx1 + 6, 70, LIME["base"])
    c.rect(thx0 - 8, 56, thx1 + 8, 63, LIME["lt"])
    c.hline(thx0 - 8, thx1 + 8, 56, LIME["hi"])
    c.hline(thx0 - 6, thx1 + 6, 70, LIME["foot"])
    for fx in range(thx0 - 2, thx1, 12):
        c.rect(fx, 64, fx + 5, 69, LIME["dk"])
        c.vline(fx + 2, 64, 69, CYAN["deep"])

    # roof: two planes, bronze crest, antefixae
    shingle_plane(c, (thx0 - 18, 56), (thx1 + 18, 56), (cx - 44, 24), (cx + 30, 16),
                  SLATE, seed=5102, rows=8, shingle=15)
    shingle_plane(c, (thx1 + 18, 56), (thx1 + 34, 44), (cx + 30, 16), (cx + 30, 16),
                  SLATE, seed=5109, rows=6, shingle=11)
    c.line([(cx - 44, 24), (cx + 30, 16)], "#8a6b34", 2)
    c.line([(cx - 44, 23), (cx + 30, 15)], "#c9a961", 1)
    c.line([(thx0 - 18, 56), (cx - 44, 24)], SLATE["dk"], 2)
    c.line([(thx1 + 18, 56), (cx + 30, 16)], SLATE["dk"], 2)
    c.line([(thx0 - 18, 56), (thx1 + 18, 56)], SLATE["dk"], 2)
    c.rect(thx0 - 18, 57, thx1 + 18, 60, "#161d23")
    for ax in range(thx0 - 16, thx1 + 16, 13):     # antefixae along the eave
        c.px(ax, 55, LIME["hi"])
        c.px(ax, 54, LIME["lt"])
    # bronze finial + small star
    c.vline(cx - 6, 8, 16, "#8a6b34")
    c.px(cx - 6, 8, "#c9a961")
    c.px(cx - 6, 9, CYAN["core"])
    c.px(cx - 7, 11, CYAN["lt"])
    c.px(cx - 5, 12, CYAN["base"])

    # ---- side wings (lower, behind): each with its own hipped roof and a lit slit ----
    for sx, sgn in ((thx0 - 16, -1), (thx1 + 16, 1)):
        c.rect(sx - 14, 104, sx + 14, pod_top + 2, LIME["dk"])
        masonry(c, [(sx - 14, 108), (sx + 14, 108), (sx + 14, pod_top + 2),
                    (sx - 14, pod_top + 2)], LIME, row_h=7, block_w=13, seed=5103 + sgn)
        c.poly([(sx - 19, 104), (sx, 90), (sx + 19, 104)], SLATE["base"])
        c.line([(sx - 19, 104), (sx, 90)], SLATE["hi"])
        c.line([(sx, 90), (sx + 19, 104)], SLATE["dk"])
        c.line([(sx - 19, 104), (sx + 19, 104)], SLATE["dk"], 2)
        # arched slit window with cyan interior
        c.rect(sx - 4, 112, sx + 4, 128, LIME["dk"])
        c.rect(sx - 3, 113, sx + 3, 127, CYAN["deep"])
        c.rect(sx - 2, 114, sx + 2, 126, CYAN["base"])
        c.ellipse(sx, 113, 3, 3, CYAN["deep"])
        c.px(sx, 120, CYAN["core"])
        # wall pilasters
        c.vline(sx - 11, 110, pod_top + 1, LIME["lt"])
        c.vline(sx + 11, 110, pod_top + 1, LIME["lt"])

    # ---- grand stair from the plaza + flanking braziers ----------------------------
    stairs(c, 60, 30, base - 26, base - 8, LIME, steps=4, widen=4, cheek=True)
    for bx in (28, 94):
        c.rect(bx - 4, base - 26, bx + 4, base - 8, IRON["base"])
        c.rect(bx - 3, base - 25, bx + 3, base - 9, IRON["lt"])
        c.ellipse(bx, base - 28, 4, 4, CYAN["base"])
        c.ellipse(bx, base - 29, 2, 3, CYAN["core"])
        c.px(bx, base - 31, "#ffffff")

    moss_patch(c, 40, base - 2, n=4, seed=5151, spread=24)
    moss_patch(c, 200, base - 2, n=4, seed=5152, spread=24)
    ground_ao(c, 20, W - 20, base, 0.38)
    edge_light(c)
    return c.im


def block3d(c: Canvas, x0, x1, y_top, y_bot, mat, seed, side="left", depth=7,
            top_face=True, taper=0, chip=True):
    """An oblique block: lit front face, one receding side face, optional lit top plane.

    `taper` pulls the top of the block inward (battered masonry), which is what gives the gate
    piers their load-bearing silhouette instead of two flat rectangles.
    """
    tx0 = x0 + (taper if side == "left" else 0)
    tx1 = x1 - (taper if side == "right" else 0)
    # front face
    c.poly([(tx0, y_top), (tx1, y_top), (x1, y_bot), (x0, y_bot)], mat["base"])
    masonry(c, [(tx0, y_top), (tx1, y_top), (x1, y_bot), (x0, y_bot)], mat, row_h=8,
            block_w=17, seed=seed)
    # side face
    if side == "left":
        c.poly([(tx0 - depth, y_top - depth), (tx0, y_top), (x0, y_bot), (x0 - depth, y_bot)],
               mat["lt"])
        for i in range(1, 6):
            t = i / 6
            c.line([(tx0 - depth, y_top - depth + (y_bot - y_top + depth) * t),
                    (x0 - depth, y_bot - (y_bot - y_top) * 0.0 - (y_bot - y_top) * (1 - t)
                     + (y_bot - y_top) * (1 - t) * 0)],
                   mat["mortar"], 1)
        # correct coursing on the side: interpolate both edges
        for i in range(1, 7):
            t = i / 7
            c.line([(tx0 - depth + (x0 - depth - tx0 + depth) * 0,
                     y_top - depth + (y_bot - (y_top - depth)) * t),
                    (x0 - depth, y_bot - ((y_bot - y_top)) * (1 - t))], mat["mortar"], 1)
        c.vline(tx0 - depth, y_top - depth, y_bot, mat["hi"])
        c.vline(x0 - depth, y_top - depth, y_bot, mat["dk"])
    else:
        c.poly([(tx1, y_top), (tx1 + depth, y_top - depth), (x1 + depth, y_bot), (x1, y_bot)],
               mat["dk"])
        for i in range(1, 7):
            t = i / 7
            c.line([(tx1, y_top + (y_bot - y_top) * t),
                    (tx1 + depth, y_top - depth + (y_bot - y_top + depth) * t)],
                   mat["mortar"], 1)
        c.vline(tx1 + depth, y_top - depth, y_bot, "#11171c")
        c.line([(tx1, y_top), (tx1 + depth, y_top - depth)], mat["lt"])
    if top_face:
        c.poly([(tx0, y_top), (tx1, y_top), (tx1 + (depth if side == "right" else -depth),
                                           y_top - depth),
                (tx0 + (depth if side == "right" else -depth), y_top - depth)],
               mat["hi"])
        c.line([(tx0, y_top), (tx1, y_top)], mat["rim"])
    if chip:
        # broken edges: alternate notches on the lit edge only
        r = rr(seed + 5)
        for _ in range(6):
            yy = y_top + r(max(4, y_bot - y_top))
            xx = tx0 - (depth if side == "left" else 0) + (0 if side == "left" else tx1 - tx0)
            c.px(xx, yy, mat["dk"])
            c.px(xx + 1, yy + 1, mat["lt"])
    return tx0, tx1


def build_tower_gate() -> Image.Image:
    """260x224 -- the monumental northern gate: battered basalt piers and a deep portal.

    Contract: PropKind.FACILITY_TOWER_GATE, obstacle (1960,585), scale 0.96, drawn bottom-centred
    at cx, cy+6s. The renderer strokes an animated rift oval centred at screen (cx, cy-196s) with
    half-size 32s x 54s, i.e. sprite (130,120) with a 16 x 27 px radius: the painted portal void
    is authored to that exact box so the animated ring lands on the stone rim and reads as light
    *inside* the gate. No collider, zone or anchor is touched by this art.
    """
    W, H = 260, 224
    c = Canvas(W, H)
    base = H - 1
    cx = W // 2
    portal_cx, portal_cy = 130, 120
    prx, pry = 16, 27

    # ---- stepped basalt dais -------------------------------------------------------
    for i, (dx, y0, y1) in enumerate(((116, base - 6, base), (108, base - 12, base - 6),
                                      (100, base - 18, base - 12))):
        c.rect(cx - dx, y0, cx + dx, y1, BASA["base"] if i % 2 else BASA["dk"])
        c.hline(cx - dx, cx + dx, y0, BASA["lt"] if i % 2 else BASA["base"])
        c.hline(cx - dx, cx + dx, y1, BASA["mortar"])
        for bx in range(cx - dx + 7, cx + dx - 7, 19):
            c.vline(bx, y0 + 1, y1 - 1, BASA["mortar"])
            c.px(bx + 1, y0 + 1, BASA["hi"])

    # ---- gatehouse core (behind the piers) ----------------------------------------
    gx0, gx1 = cx - 46, cx + 46
    c.rect(gx0 - 4, 46, gx1 + 4, base - 18, BASA["base"])
    masonry(c, [(gx0, 52), (gx1, 52), (gx1, base - 18), (gx0, base - 18)], BASA,
            row_h=8, block_w=15, seed=5301)
    c.vline(gx0 - 4, 46, base - 18, BASA["lt"])
    c.vline(gx1 + 4, 46, base - 18, BASA["dk"])

    # ---- twin battered piers, left one stepping forward --------------------------
    block3d(c, cx - 112, cx - 44, 22, base - 20, BASA, seed=5201, side="left", depth=8,
            taper=5)
    block3d(c, cx + 44, cx + 112, 28, base - 20, BASA, seed=5202, side="right", depth=8,
            taper=5)
    # pierced caps + battlements, so the piers terminate decisively instead of as slabs
    for side in (-1, 1):
        px0 = cx + side * 112
        px1 = cx + side * 44
        c.rect(min(px0, px1), 12, max(px0, px1), 26, BASA["dk"])
        c.rect(min(px0, px1) + 2, 13, max(px0, px1) - 2, 24, BASA["base"])
        c.hline(min(px0, px1), max(px0, px1), 12, BASA["hi"])
        c.hline(min(px0, px1), max(px0, px1), 26, BASA["mortar"])
        for i, bx in enumerate(range(min(px0, px1) + 4, max(px0, px1) - 6, 12)):
            h = 9 if i % 2 == 0 else 5
            c.rect(bx, 12 - h, bx + 7, 12, BASA["lt"] if i % 2 == 0 else BASA["base"])
            c.hline(bx, bx + 7, 12 - h, BASA["rim"] if i % 2 == 0 else BASA["lt"])
            c.vline(bx + 7, 12 - h, 11, BASA["dk"])
        # pier shoulder: a moulded course where the shaft meets the cap
        c.rect(min(px0, px1) - 1, 26, max(px0, px1) + 1, 29, BASA["lt"])
        c.hline(min(px0, px1) - 1, max(px0, px1) + 1, 26, BASA["rim"])
        c.hline(min(px0, px1) - 1, max(px0, px1) + 1, 29, BASA["mortar"])

    # carved vertical channels + rune lamps on each pier face
    for side in (-1, 1):
        fx = cx + side * 74
        c.vline(fx, 34, base - 30, BASA["dk"])
        c.vline(fx + side, 34, base - 30, BASA["lt"])
        for ry in range(48, base - 44, 28):
            c.hline(fx - 8, fx + 8, ry, BASA["dk"])
            c.hline(fx - 8, fx + 8, ry - 1, BASA["lt"])
        c.rect(fx - 2, 40, fx + 2, 46, BASA["dk"])
        c.px(fx, 42, CYAN["lt"])
        c.px(fx, 44, CYAN["base"])
        c.hline(fx - 1, fx + 1, 45, CYAN["deep"])

    # ---- massive lintel with a heavy cornice --------------------------------------
    ly0 = 78
    c.rect(cx - 52, ly0, cx + 52, ly0 + 18, BASA["dk"])
    c.rect(cx - 50, ly0 + 1, cx + 50, ly0 + 16, BASA["base"])
    c.rect(cx - 56, ly0 - 5, cx + 56, ly0, BASA["lt"])
    c.hline(cx - 56, cx + 56, ly0 - 5, BASA["hi"])
    c.hline(cx - 56, cx + 56, ly0, BASA["dk"])
    c.hline(cx - 52, cx + 52, ly0 + 18, BASA["mortar"])
    for bx in range(cx - 50, cx + 50, 15):
        c.vline(bx, ly0 + 2, ly0 + 15, BASA["mortar"])
        c.px(bx + 1, ly0 + 2, BASA["lt"])
    # dentils under the cornice
    for bx in range(cx - 52, cx + 52, 6):
        c.rect(bx, ly0 + 1, bx + 2, ly0 + 3, BASA["lt"])
    # three carved celestial runes only (restraint reads as age, not decoration)
    for rx in (cx - 26, cx, cx + 26):
        c.rect(rx - 4, ly0 + 6, rx + 4, ly0 + 13, BASA["dk"])
        c.px(rx, ly0 + 7, CYAN["lt"])
        c.hline(rx - 2, rx + 2, ly0 + 9, CYAN["base"])
        c.px(rx, ly0 + 11, CYAN["core"])

    # ---- deep portal void, arch ring and rift -------------------------------------
    c.rect(cx - prx, portal_cy - pry, cx + prx, base - 22, "#0a1015")
    c.ellipse(cx, portal_cy - pry, prx, 13, "#0a1015")
    # voussoirs around the arch, one block per 15 degrees
    for a in range(13):
        ang = math.pi + a * math.pi / 12
        vx = cx + math.cos(ang) * (prx + 7)
        vy = portal_cy - pry + 2 + math.sin(ang) * (pry * 0.62)
        c.rect(vx - 2, vy - 2, vx + 2, vy + 2, BASA["lt"])
        c.hline(vx - 2, vx + 2, vy - 2, BASA["hi"])
        c.hline(vx - 2, vx + 2, vy + 2, BASA["mortar"])
    # keystone
    c.rect(cx - 3, portal_cy - pry - 12, cx + 3, portal_cy - pry - 5, BASA["lt"])
    c.hline(cx - 3, cx + 3, portal_cy - pry - 12, BASA["hi"])
    # jambs: lit inner face on the left of the reveal, deep shade on the right, then the
    # receding inner arch - that is what makes the opening read as a passage, not a decal
    c.rect(cx - prx, portal_cy - pry + 9, cx - prx + 3, base - 22, BASA["lt"])
    c.vline(cx - prx, portal_cy - pry + 9, base - 22, BASA["hi"])
    c.rect(cx + prx - 2, portal_cy - pry + 9, cx + prx, base - 22, "#070b0f")
    c.ellipse(cx, portal_cy - pry + 14, prx - 4, pry - 6, "#0e1620")
    c.rect(cx - prx + 4, portal_cy - pry + 12, cx + prx - 3, base - 23, "#071016")
    # inner depth bands, then the cold rift core
    c.ellipse(cx, portal_cy - pry + 12, prx - 6, pry - 8, "#122230")
    c.ellipse(cx, portal_cy - pry + 15, prx - 8, pry - 12, CYAN["deep"])
    c.ellipse(cx, portal_cy - pry + 17, max(3, prx - 10), max(4, pry - 17), CYAN["base"])
    c.ellipse(cx, portal_cy - pry + 19, max(3, prx - 12), max(3, pry - 21), CYAN["lt"])
    c.ellipse(cx, portal_cy - pry + 21, max(2, prx - 14), max(2, pry - 24), CYAN["core"])
    c.px(cx, portal_cy - pry + 16, "#ffffff")
    # light spilling down the passage floor
    c.ellipse(cx, base - 24, prx + 10, 7, CYAN["deep"])
    c.ellipse(cx, base - 25, prx + 2, 5, CYAN["base"])
    c.ellipse(cx, base - 26, prx - 6, 3, CYAN["lt"])

    # ---- threshold stairs ----------------------------------------------------------
    stairs(c, cx, 34, base - 25, base - 6, BASA, steps=4, widen=3, cheek=True)

    # ---- flanking rune obelisks (silhouette markers at overview zoom) -------------
    for sx in (cx - 120, cx + 120):
        c.poly([(sx - 4, base - 20), (sx - 2, base - 66), (sx + 2, base - 66),
                (sx + 4, base - 20)], BASA["base"])
        c.poly([(sx - 4, base - 20), (sx - 2, base - 66), (sx - 1, base - 66),
                (sx - 1, base - 20)], BASA["lt"])
        c.hline(sx - 5, sx + 5, base - 66, BASA["hi"])
        c.px(sx, base - 58, CYAN["lt"])
        c.px(sx, base - 56, CYAN["base"])
        c.px(sx, base - 50, CYAN["deep"])
        c.rect(sx - 6, base - 71, sx + 6, base - 66, BASA["dk"])
        c.hline(sx - 6, sx + 6, base - 71, BASA["hi"])

    # ---- crest between the piers, keeping the sky cleft ----------------------------
    for side in (-1, 1):
        x_in = cx + side * 18
        x_out = cx + side * 46
        c.poly([(x_in, ly0 - 6), (x_out, ly0 - 6), (x_out, ly0 - 16),
                (x_in, ly0 - 28)], BASA["dk"])
        c.line([(x_in, ly0 - 28), (x_out, ly0 - 16)], BASA["lt"])
        c.line([(x_out, ly0 - 16), (x_out, ly0 - 6)], BASA["mortar"])
        c.px(cx + side * 30, ly0 - 20, CYAN["base"])

    # weathering: damp moss at the pier feet, a few hairline cracks on the lit flanks
    for side in (-1, 1):
        moss_patch(c, cx + side * 100 - 6, base - 26, n=5, seed=5400 + side, spread=16)
    for k, (cxp, cyp) in enumerate(((cx - 96, 96), (cx + 88, 128), (cx - 60, 150))):
        c.line([(cxp, cyp), (cxp + 3, cyp + 7), (cxp + 1, cyp + 13)], BASA["mortar"], 1)
        c.px(cxp, cyp, BASA["dk"])
    ground_ao(c, 34, W - 34, base, 0.40)
    edge_light(c)
    return c.im


def build_synthesis_lab() -> Image.Image:
    """188x168 -- alchemist's workshop: stone-and-copper mass, retort, teal interior glow.

    Subtle arcane-industrial identity, not a magic portal: a copper condenser stack, a glass
    retort on the terrace, pipes along the shaded face and one bounded teal interior light.
    """
    W, H = 188, 168
    c = Canvas(W, H)
    base = H - 1
    door_cx, door_bottom = W // 2, base - 2

    x0, xm, x1 = 10, 104, 172
    eave_y, jetty = 80, 126
    up, low, rup, rlow = two_face_block(c, base, x0, xm, x1, eave_y, jetty,
                                        STONE, PLAS, rise_l=10, rise_r=17, drop_r=21,
                                        posts=3)

    # ---- copper condenser stack (the workshop's silhouette marker) -----------------
    c.rect(118, 34, 136, 86, "#5a3a2a")
    c.rect(119, 35, 135, 84, "#7a4f34")
    c.vline(120, 36, 83, "#9a6644")
    c.vline(134, 36, 83, "#4a2e22")
    for yy in range(40, 82, 7):
        c.hline(119, 135, yy, "#3d2519")
        c.hline(119, 135, yy + 1, "#8f5f3e")
    c.rect(114, 28, 140, 36, "#4a2e22")
    c.hline(114, 140, 28, "#a06e4a")
    c.rect(120, 30, 134, 33, "#1d2328")     # flue mouth
    # teal vapour leaking from the condenser
    c.px(126, 24, "#3fbfae")
    c.px(130, 21, "#2f9d90")
    c.px(133, 18, "#22645f")
    # copper pipe run down the shaded face into the wall
    c.vline(137, 84, 92, "#7a4f34")
    c.vline(138, 84, 92, "#4a2e22")
    c.hline(104, 138, 92, "#6b452f")
    c.hline(104, 138, 93, "#3d2519")

    _roof_over(c, x0, xm, x1, eave_y, 10, SLATE, seed=3721, ridge_y=26, hip_drop=21,
               rows=8, shingle=13)
    # ridge vent cupola: small and industrial, not decorative
    c.rect(62, 18, 78, 28, "#5a3a2a")
    c.rect(63, 19, 77, 27, "#7a4f34")
    c.hline(62, 78, 18, "#a06e4a")
    c.poly([(58, 18), (70, 8), (82, 18)], SLATE["base"])
    c.line([(58, 18), (70, 8)], SLATE["hi"])
    c.line([(70, 8), (82, 18)], SLATE["dk"])
    for yy in range(20, 27, 2):
        c.hline(64, 76, yy, "#3d2519")

    window(c, 20, up[0] + 4, 12, 16, glow=TEAL, lit=1.0, pad=2)
    window(c, 52, up[0] + 7, 12, 16, glow=TEAL, lit=0.0, pad=2)
    window(c, 80, up[0] + 10, 12, 16, glow=TEAL, lit=1.4, pad=2)
    window(c, 120, rup[0] + 6, 11, 14, glow=TEAL, lit=1.2, pad=2)
    window(c, 128, low[0] + 6, 12, 15, glow=TEAL, lit=0.0, pad=2)

    door(c, door_cx, door_bottom, w=22, h=29, mat=STONE, arch=True, lantern=False)
    # teal light spilling under the door leaf, and a copper threshold strip
    c.hline(door_cx - 9, door_cx + 9, door_bottom, TEAL["dim"])
    c.hline(door_cx - 11, door_cx + 11, door_bottom + 1, "#7a4f34")

    # ---- glass retort on the terrace, iron stand, coiled worm ----------------------
    rx, ry = 142, base - 18
    c.rect(rx - 9, ry - 26, rx + 9, ry - 24, IRON["base"])   # stand arm
    c.vline(rx + 8, ry - 24, ry + 2, IRON["base"])           # stand post
    c.rect(rx - 12, ry + 2, rx + 12, ry + 4, IRON["base"])   # stand foot
    c.ellipse(rx, ry - 12, 9, 11, "#2b3f47")                 # glass bulb
    c.ellipse(rx, ry - 12, 8, 10, "#3d5b63")
    c.ellipse(rx, ry - 10, 6, 6, TEAL["deep"])
    c.ellipse(rx, ry - 9, 5, 5, TEAL["base"])
    c.ellipse(rx - 1, ry - 10, 2, 2, TEAL["core"])
    c.rect(rx - 2, ry - 26, rx + 2, ry - 22, "#3d5b63")      # neck
    c.px(rx - 6, ry - 16, "#8fbac2")                          # glass highlight
    c.px(rx - 6, ry - 15, "#6f9aa2")
    # coiled copper worm beside the retort
    for k in range(4):
        c.ellipse(rx + 14, ry - 8 + k * 5, 5, 2, "#7a4f34")
        c.px(rx + 18, ry - 8 + k * 5, "#a06e4a")

    # ---- crates, a calibration table, chalk marks on the stone --------------------
    c.rect(26, base - 14, 44, base - 1, OAK["base"])
    c.rect(27, base - 13, 43, base - 2, OAK["lt"])
    c.hline(26, 44, base - 8, OAK["dk"])
    c.vline(35, base - 13, base - 2, OAK["dk"])
    c.rect(150, base - 12, 168, base - 10, OAK["lt"])
    c.rect(152, base - 9, 154, base - 1, OAK["dk"])
    c.rect(164, base - 9, 166, base - 1, OAK["dk"])
    c.rect(155, base - 15, 160, base - 12, "#3d5b63")
    c.px(157, base - 14, TEAL["base"])
    sign_board(c, 132, base - 32, "retort", w=15, h=12)
    moss_patch(c, 14, base - 4, n=3, seed=3731, spread=12)

    ground_ao(c, 6, W - 6, base, 0.40)
    edge_light(c)
    return c.im


BUILDERS = {
    "summoning_hall": (build_summoning_hall, 268, 206),
    "tower_gate": (build_tower_gate, 260, 224),
    "hero_registry": (build_hero_registry, 188, 166),
    "hero_quarters": (build_hero_quarters, 200, 168),
    "restaurant": (build_restaurant, 206, 172),
    "synthesis_lab": (build_synthesis_lab, 188, 168),
}


# --------------------------------------------------------------------------- checks
# Sprite-space door contract: (canvas W,H, obstacle x,y, scale, entrance door x,y or None)
DOOR_CONTRACT = {
    "hero_quarters": (200, 168, 825.0, 790.0, 0.80, 823.0, 790.0),
    "restaurant": (206, 172, 1105.0, 925.0, 0.80, 1103.0, 925.0),
}


def expected_door(key):
    W, H, ox, oy, sc, dx, dy = DOOR_CONTRACT[key]
    cx = W / 2 + (dx - ox) / (2 * sc)
    row = (H - 3) - (oy - dy) * 0.72 / (2 * sc)
    return cx, row


def validate(key: str, im: Image.Image):
    W, H, *_ = (im.width, im.height) + BUILDERS[key][1:]
    assert im.mode == "RGBA", (key, im.mode)
    assert im.size == (BUILDERS[key][1], BUILDERS[key][2]), (key, im.size)
    px = im.load()
    opaque = sum(1 for y in range(H) for x in range(W) if px[x, y][3] > 0)
    assert opaque > W * H * 0.28, (key, "too empty", opaque)
    # clean transparent margin: no edge bleed into neighbouring ground tiles
    for x in range(W):
        assert px[x, 0][3] == 0, (key, "top edge", x)
    for y in range(H):
        assert px[0, y][3] == 0 and px[W - 1, y][3] == 0, (key, "side edge", y)
    # the base row must carry the footprint, not float above a transparent gap
    base_rows = [y for y in range(H) if any(px[x, y][3] > 0 for x in range(W))]
    assert max(base_rows) == H - 1, (key, "base row", max(base_rows))
    ground_contact = sum(1 for x in range(W) if px[x, H - 1][3] > 0)
    assert ground_contact > W * 0.5, (key, "ground contact too narrow", ground_contact)
    return opaque


def world_of_door(key, art_cx, art_bottom):
    """Inverse of the blit: painted door pixel -> world position. Used by checks and docs."""
    W, H, ox, oy, sc, dx, dy = DOOR_CONTRACT[key]
    wx = ox + (art_cx - W / 2) * 2 * sc
    wy = oy - ((H - 3) - art_bottom) * (2 * sc) / 0.72
    return wx, wy


def stamp(manifest_path: Path, key: str, png: Path):
    """Refresh one premium_slice manifest entry (size + sha256) without touching other keys."""
    manifest = json.loads(manifest_path.read_text())
    digest = hashlib.sha256(png.read_bytes()).hexdigest()
    with Image.open(png) as im:
        size = list(im.size)
    entry = manifest["assets"].get(key, {})
    entry["file"] = png.name
    entry["size"] = size
    entry["sha256"] = digest
    manifest["assets"][key] = entry
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")



def write_contact_sheet(out: Path):
    """One labelled sheet: every facility at 1x, 2x and true gameplay scale (zoom 0.88).

    Label is deliberately explicit: this is ART, not an engine render. The engine frames are
    produced separately by MapForgeP21RenderTest through the real draw path.
    """
    scales = {"summoning_hall": 1.08, "tower_gate": 0.96}
    keys = list(BUILDERS)
    pad, label_h = 12, 22
    cols = []
    for key in keys:
        im = Image.open(PREMIUM / f"{key}.png").convert("RGBA")
        z = 2 * 0.88 * scales.get(key, 0.80)
        shots = [
            ("1x", im),
            ("2x", im.resize((im.width * 2, im.height * 2), Image.NEAREST)),
            (f"0.88 ({z:.2f}x)", im.resize((max(1, round(im.width * z)), max(1, round(im.height * z))),
                                          Image.NEAREST)),
        ]
        cell_w = max(s[1].width for s in shots) + pad
        cell_h = sum(s[1].height for s in shots) + pad * (len(shots) + 1) + label_h * len(shots)
        cell = Image.new("RGBA", (cell_w, cell_h), (18, 21, 24, 255))
        d = ImageDraw.Draw(cell)
        d.text((6, 4), key, fill=(210, 220, 228, 255))
        y = pad + 14
        for label, shot in shots:
            d.text((6, y - 12), label, fill=(140, 152, 162, 255))
            bg = Image.new("RGBA", shot.size, (26, 42, 34, 255))
            bg.alpha_composite(shot)
            cell.alpha_composite(bg, ((cell_w - shot.width) // 2, y))
            y += shot.height + pad + label_h - 6
        cols.append(cell)
    W = sum(cc.width for cc in cols) + pad
    H = max(cc.height for cc in cols) + 26
    sheet = Image.new("RGBA", (W, H), (14, 16, 20, 255))
    d = ImageDraw.Draw(sheet)
    d.text((8, 6), "ART CONTACT SHEET - ETERNAL SUMMONER P21 facilities (offline authored PNG, "
                   "nearest-neighbour previews; NOT an engine render)", fill=(228, 190, 120, 255))
    x = pad
    for cc in cols:
        sheet.alpha_composite(cc, (x, 22))
        x += cc.width
    sheet.save(out)
    print(f"contact sheet -> {out}")

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", default=None)
    args = ap.parse_args()

    written = []
    for key, (fn, w, h) in BUILDERS.items():
        im = fn()
        validate(key, im)
        out = PREMIUM / f"{key}.png"
        im.save(out, optimize=True)
        stamp(PREMIUM / "manifest.json", key, out)
        written.append(out)
        if args.preview:
            prev = Path(args.preview)
            prev.mkdir(parents=True, exist_ok=True)
            bg = Image.new("RGBA", im.size, (26, 32, 30, 255))
            bg.alpha_composite(im)
            bg.resize((im.width * 3, im.height * 3), Image.NEAREST).save(prev / f"{key}_3x.png")
    if args.preview:
        write_contact_sheet(Path(args.preview) / "P21_ART_CONTACT_SHEET.png")

    print(json.dumps({"written": [p.name for p in written]}, indent=2))


if __name__ == "__main__":
    main()
