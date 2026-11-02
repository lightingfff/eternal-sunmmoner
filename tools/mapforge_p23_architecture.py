#!/usr/bin/env python3
"""MAP FORGE — P23 architecture 2.5D integration repair (Sanctuary + Tower Gate).

This generator replaces the seven authored `premium_slice/` facilities and the six procedural
Sanctuary background buildings with sprites that obey ONE structural rule set, because the
visible defects at P22 HEAD are all the same class of failure: **a painted shape that does not
close onto its neighbour**, so the grass shows through a building.

What changes:
  * every mass is painted SILHOUETTE FIRST (one opaque body per face) and detailed second, so a
    column of pixels inside a wall can never contain a transparent hole;
  * roof skeleton is explicit: ridge cap -> front pitch + right hip return -> eave fascia ->
    opaque soffit band -> wall top. The roof is joined to the wall by construction, never by a
    dark band painted over a gap;
  * the right-hand return is a real receding plane (2 value steps darker) whose top edge follows
    the eave and whose base follows the plinth, so the building has volume instead of being a
    frontal decal;
  * enterable doors are cut INTO the wall (outer surround -> lit left jamb -> dark right jamb ->
    lintel/voussoirs -> near-black reveal -> recessed leaf -> threshold steps on rows H-2..H-1),
    and the leaf's base row is exactly H-3, which the renderer maps onto `obs.y`;
  * the Armory's door is re-authored onto its playable trigger (993, 655): canvas centre X,
    base row H-3. P20 painted it 76 world units east / 120 north of the trigger;
  * zone-only facilities (Registry, Synthesis Lab) get a NON-walkable recess: an arcaded
    counter / workshop bay with a stone lip across the opening, so the art stops advertising a
    door that has no interior;
  * the Summoning Hall is rebuilt as one continuous structure: plaza tiers -> podium -> cella
    wall -> colonnade -> entablature -> roof, with the rune stage still authored at sprite
    (111, 176) for the zone centre (2380, 1930);
  * the Tower Gate aperture stays DARK inside the renderer's animated rift box (sprite centre
    (130, 120) +/- (16, 27)); the painted light is only the threshold spill below it.

Canvases, sprite-space door rows, world anchors, footprints, colliders, zones, camera
(`ObliqueCamera.OBLIQUE_Y_SCALE = 0.72`), depth sort and the canopy occlusion fractions are
unchanged. Nothing is generated at draw time: this script is offline, the engine only decodes
PNGs once and blits with `FilterQuality.None`.

Run:
    python3 tools/mapforge_p23_architecture.py                # write PNGs + refresh manifests
    python3 tools/mapforge_p23_architecture.py --preview DIR  # + 1x/2x/3x review sheets
    python3 tools/mapforge_p23_architecture.py --check        # validate only, no writes
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
SANCT = ASSETS / "sanctuary_p03"

# --------------------------------------------------------------------------- palette
# One family, continued from P18 ground / P20 armory / P21 facilities / P22 vegetation.
STONE = dict(mortar="#222a30", dk="#333d45", base="#414c54", lt="#4f5a62", hi="#616d76",
             rim="#7b8790", foot="#2b333a")
LIME = dict(mortar="#3b434a", dk="#4b545c", base="#59636b", lt="#69737c", hi="#7e8891",
            rim="#98a2aa", foot="#414a52")
LIME_T = dict(mortar="#333b44", dk="#424b55", base="#4c5661", lt="#586472", hi="#67737f",
              rim="#7f8b97", foot="#39424c")
BASA = dict(mortar="#151a21", dk="#1f252e", base="#2a313b", lt="#36404c", hi="#4a5563",
            rim="#61707f", foot="#1b2027")
GATE = dict(mortar="#1b2129", dk="#28303a", base="#333c47", lt="#414c59", hi="#55616f",
            rim="#6f7f8e", foot="#232a32")
PLAS = dict(mortar="#2c3339", dk="#343c43", base="#3d454c", lt="#4a535b", hi="#5b646c",
            rim="#6d767e", foot="#2f363c")
# perimeter masonry: darker and bluer than the buildings, so the fortress wall reads as the
# boundary of the world rather than another house
WALLSTONE = dict(mortar="#1a222a", dk="#28323b", base="#333f49", lt="#404d58", hi="#4e5d69",
                 rim="#66757f", foot="#232c34")
SLATE = dict(dk="#28313a", base="#333f49", lt="#3f4c57", hi="#4c5a66", ridge="#5f6d79")
SHING = dict(dk="#241811", base="#33241a", lt="#402e21", hi="#513a28", ridge="#66482f")
THATCH = dict(dk="#2a2418", base="#3a3222", lt="#4a402c", hi="#5b4f36", ridge="#6d603f")
OAK = dict(dk="#1d150f", base="#2f2116", lt="#3f2d1d", hi="#553f28", warm="#6a5137")
IRON = dict(dk="#181d22", base="#272e35", lt="#414b54", hi="#68737d", glint="#9aa6b0")
AMBER = dict(deep="#5c3a16", dim="#a86b2e", base="#e0983f", lt="#f6c071", core="#ffe3ae")
CYAN = dict(deep="#0a3f55", dim="#12657f", base="#1f93b4", lt="#35c6e6", core="#a8eef9")
TEAL = dict(deep="#0a3f3b", dim="#157a71", base="#1f9c8f", lt="#45cbb8", core="#9df3e6")
VIOL = dict(deep="#2c2340", base="#4a3a63", lt="#6a5288", core="#a98ecb")
MOSS = dict(dk="#26362b", base="#334a38", lt="#44604a")
BRONZE = dict(dk="#3d3018", base="#6f5a2c", lt="#9a7f3f", hi="#c9a961")
AO = "#0f151a"
CONTACT = "#151c20"


def rgba(h: str, a: int = 255) -> tuple[int, int, int, int]:
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(a: str, b: str, t: float) -> str:
    pa, pb = rgba(a), rgba(b)
    t = min(1.0, max(0.0, t))
    return "#{:02x}{:02x}{:02x}".format(*[int(round(pa[i] + (pb[i] - pa[i]) * t)) for i in range(3)])


def rr(seed: int):
    r = random.Random(seed)
    return r.randrange


def lerp(a, b, t):
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)


def bilinear(tl, tr, br, bl, u, v):
    return lerp(lerp(tl, tr, u), lerp(bl, br, u), v)


# --------------------------------------------------------------------------- canvas
class Canvas:
    """Pixel-exact RGBA wrapper. No anti-aliasing anywhere in this pipeline."""

    def __init__(self, w: int, h: int):
        self.w, self.h = w, h
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    # -- primitives ---------------------------------------------------------
    def px(self, x, y, col, a=255):
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            if isinstance(col, tuple):          # already RGBA (used for clearing pixels)
                self.im.putpixel((x, y), col)
            else:
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

    def ellipse(self, cx, cy, rx, ry, col, a=255):
        self.d.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=rgba(col, a))

    def ring(self, cx, cy, rx, ry, col, t=1, a=255):
        for i in range(t):
            self.d.ellipse([cx - rx + i, cy - ry + i, cx + rx - i, cy + ry - i],
                           outline=rgba(col, a))

    def line(self, pts, col, width=1, a=255):
        self.d.line([(int(round(x)), int(round(y))) for x, y in pts], fill=rgba(col, a),
                    width=width)

    # -- reading ------------------------------------------------------------
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

    def blend_rect(self, x0, y0, x1, y1, target, amount):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                if self.opaque(x, y):
                    self.blend(x, y, target, amount)


# --------------------------------------------------------------------------- parts
def ashlar(c: Canvas, quad, mat, seed=7, row_h=7, block_w=14, lit="tl"):
    """Coursed masonry filling a quad [tl, tr, br, bl].

    Blocks vary in width, each carries a 1 px upper rim in the light tone, a darker lower face
    and a mortar joint (dark grey, never black). The quad is fully filled, so the wall has no
    holes. Written to survive being read at zoom 0.88: 3 tones, not 8 speckles.
    """
    tl, tr, br, bl = quad
    r = rr(seed)
    span = tr[0] - tl[0]
    depth = bl[1] - tl[1]
    if abs(span) < 2 or depth < 3:
        return
    n_rows = max(1, int(depth / row_h))
    for row in range(n_rows):
        v0, v1 = row / n_rows, (row + 1) / n_rows
        a = bilinear(tl, tr, br, bl, 0.0, v0)
        b = bilinear(tl, tr, br, bl, 1.0, v0)
        e = bilinear(tl, tr, br, bl, 0.0, v1)
        f = bilinear(tl, tr, br, bl, 1.0, v1)
        c.poly([a, b, f, e], mat["base"])
        c.line([e, f], mat["mortar"], 1)
        x = -r(12)
        while x < span:
            w = block_w - 4 + r(9)
            u0, u1 = max(0.0, x / span), min(1.0, (x + w) / span)
            if (u1 - u0) * abs(span) > 4:
                p0 = bilinear(tl, tr, br, bl, u0, v0)
                p1 = bilinear(tl, tr, br, bl, u1, v0)
                q0 = bilinear(tl, tr, br, bl, u0, v1)
                q1 = bilinear(tl, tr, br, bl, u1, v1)
                roll = r(10)
                tone = mat["dk"] if roll == 0 else (mat["lt"] if roll < 3 else mat["base"])
                c.poly([p0, p1, q1, q0], tone)
                c.line([p0, p1], mat["lt"] if tone != mat["lt"] else mat["hi"], 1)
                c.line([q0, q1], mat["dk"], 1)
                c.line([p1, q1], mat["mortar"], 1)
            x += w + 1


def quoin_column(c: Canvas, x, y0, y1, mat, seed=1, width=4, left=True):
    """Alternating long/short corner stones that tie two faces together."""
    r = rr(seed)
    y = y0
    i = 0
    while y < y1:
        h = min(6 + r(4), y1 - y)
        if h < 1:
            break
        w = width + (1 if i % 2 == 0 else 0)
        x0 = x - w + 1 if left else x
        c.rect(x0, y, x0 + w - 1, y + h - 1, mat["lt"] if i % 2 else mat["base"])
        c.hline(x0, x0 + w - 1, y, mat["hi"] if i % 2 else mat["lt"])
        c.hline(x0, x0 + w - 1, y + h - 1, mat["dk"])
        c.vline(x0 + w - 1, y, y + h - 1, mat["mortar"])
        y += h
        i += 1


def shingle_plane(c: Canvas, eave_l, eave_r, ridge_l, ridge_r, ramp, seed, rows=9, tile=11,
                  stagger=True):
    """Fill one roof plane (eave edge -> ridge edge) in staggered courses.

    Every course is a solid quad, so the plane is opaque; the ridge cap is painted by the
    caller so both pitches meet on one line.
    """
    r = rr(seed)
    for i in range(rows):
        v0, v1 = i / rows, (i + 1) / rows
        e0, e1 = lerp(eave_l, ridge_l, v0), lerp(eave_r, ridge_r, v0)
        f0, f1 = lerp(eave_l, ridge_l, v1), lerp(eave_r, ridge_r, v1)
        c.poly([e0, e1, f1, f0], ramp["base"])
        span = e1[0] - e0[0]
        if abs(span) < 2:
            continue
        x = e0[0] - (0 if (i % 2 == 0 or not stagger) else tile // 2)
        while x < e0[0] + span:
            w = tile - 3 + r(5)
            u0 = max(0.0, (x - e0[0]) / span)
            u1 = min(1.0, (x + w - e0[0]) / span)
            if u1 - u0 > 0.06:
                p0, p1 = lerp(e0, e1, u0), lerp(e0, e1, u1)
                q0, q1 = lerp(f0, f1, u0), lerp(f0, f1, u1)
                roll = r(11)
                tone = ramp["dk"] if roll == 0 else (ramp["lt"] if roll in (3, 4) else ramp["base"])
                c.poly([p0, p1, q1, q0], tone)
                c.line([p0, p1], ramp["lt"], 1)
                c.line([q0, q1], ramp["dk"], 1)
            x += w + 1


def hip_roof(c: Canvas, eave_l, eave_r, ridge_l, ridge_r, hip_tip, ramp, seed, rows=9,
             tile=12, finial=False, ridge_ornament=None):
    """Front pitch + right hip return + ridge cap + eave fascia. Returns the ridge line."""
    # hip return first, so the front pitch overlaps its inner edge and the planes meet cleanly
    shingle_plane(c, eave_r, hip_tip, ridge_r, ridge_r, ramp, seed + 7,
                  rows=max(4, rows - 3), tile=max(6, tile - 4))
    shingle_plane(c, eave_l, eave_r, ridge_l, ridge_r, ramp, seed, rows=rows, tile=tile)
    # hip arris (the outside corner of the roof) and the ridge cap
    c.line([eave_r, hip_tip], ramp["lt"], 1)
    c.line([(eave_r[0], eave_r[1] + 1), (hip_tip[0], hip_tip[1] + 1)], ramp["dk"], 2)
    c.line([(eave_r[0], eave_r[1] + 3), (hip_tip[0], hip_tip[1] + 3)], "#161d23", 1)
    c.line([eave_r, ridge_r], ramp["hi"], 1)
    c.line([ridge_l, ridge_r], ramp["ridge"], 2)
    c.line([(ridge_l[0], ridge_l[1] - 1), (ridge_r[0], ridge_r[1] - 1)], ramp["hi"], 1)
    # ridge ends get a carved block so the cap terminates instead of stopping mid-air
    for p in (ridge_l, ridge_r):
        c.rect(p[0] - 1, p[1] - 3, p[0] + 1, p[1], ramp["ridge"])
        c.px(p[0], p[1] - 3, ramp["hi"])
    # eave fascia: lit top edge + dark underside, 3 px thick, following the eave
    c.line([eave_l, eave_r], ramp["lt"], 1)
    c.line([(eave_l[0], eave_l[1] + 1), (eave_r[0], eave_r[1] + 1)], ramp["dk"], 2)
    c.line([(eave_l[0], eave_l[1] + 3), (eave_r[0], eave_r[1] + 3)], "#161d23", 1)
    if finial:
        fx, fy = ridge_l
        # a carved finial block riding the ridge cap -- solid contact, no floating pole
        c.rect(fx, fy - 6, fx + 2, fy, IRON["base"])
        c.vline(fx + 1, fy - 6, fy, IRON["lt"])
        c.hline(fx, fx + 2, fy - 6, BRONZE["base"])
        c.px(fx + 1, fy - 7, BRONZE["hi"])
    if ridge_ornament:
        ridge_ornament(c, (ridge_l[0] + ridge_r[0]) // 2, ridge_l[1])
    return ridge_l, ridge_r


def soffit(c: Canvas, eave_l, eave_r, band=4):
    """Opaque shade band between the eave underside and the wall top.

    This is the piece P20/P21 were missing: without it there is nothing under the eave, and the
    roof reads as floating 2-6 px above the wall.
    """
    c.poly([(eave_l[0], eave_l[1] + 4), (eave_r[0], eave_r[1] + 4),
            (eave_r[0], eave_r[1] + 4 + band), (eave_l[0], eave_l[1] + 4 + band)], "#191d1f")
    c.poly([(eave_l[0], eave_l[1] + 4 + band), (eave_r[0], eave_r[1] + 4 + band),
            (eave_r[0], eave_r[1] + 5 + band), (eave_l[0], eave_l[1] + 5 + band)], "#232729")
    # rafter tails: they make the overhang structural rather than a floating plate
    x = eave_l[0] + 3
    while x < eave_r[0] - 2:
        t = (x - eave_l[0]) / max(1.0, eave_r[0] - eave_l[0])
        y = eave_l[1] + (eave_r[1] - eave_l[1]) * t
        c.rect(x, y + 3, x + 2, y + 5, OAK["base"])
        c.px(x, y + 3, OAK["lt"])
        x += 11


def recessed_door(c: Canvas, cx, sill, w=24, h=32, mat=STONE, arch=True, leaf="plank",
                  steps=2, lantern=False, sign=False):
    """A doorway CUT INTO the wall: surround, lit left jamb, dark right jamb, lintel/voussoirs,
    near-black reveal, recessed leaf, then stone steps that reach row `sill` exactly.

    `sill` is the row the leaf ends on; the two rows below it are the projecting stoop, which is
    what the renderer maps south of the world anchor. For every facility here `sill == H-3`.
    """
    x0 = cx - w // 2
    x1 = x0 + w - 1
    top = sill - h + 1

    # outer stone surround, proud of the wall (2 px head, 4 px sides)
    c.rect(x0 - 5, top - 6, x1 + 5, sill + 1, mat["dk"])
    c.rect(x0 - 4, top - 5, x1 + 4, sill, mat["base"])
    c.hline(x0 - 5, x1 + 5, top - 6, mat["hi"])
    c.vline(x0 - 5, top - 6, sill + 1, mat["lt"])
    c.hline(x0 - 5, x1 + 5, sill + 1, mat["foot"])
    c.vline(x1 + 5, top - 6, sill + 1, mat["dk"])

    # arched head cut into the surround
    if arch:
        for i in range(3):
            c.ellipse(cx, top + 1, w // 2 + 3 - i, 6 - i, mat["lt"] if i == 0 else mat["base"])
        # voussoirs read as separate stones
        for a in range(9):
            ang = math.pi + a * math.pi / 8
            vx = cx + math.cos(ang) * (w // 2 + 2)
            vy = top + 2 + math.sin(ang) * 6
            c.rect(vx - 1, vy - 1, vx + 1, vy + 1, mat["lt"])
            c.px(vx, vy - 1, mat["hi"])
            c.px(vx, vy + 1, mat["foot"])

    # keystone
    c.rect(cx - 2, top - 8, cx + 2, top - 3, mat["lt"])
    c.hline(cx - 2, cx + 2, top - 8, mat["hi"])
    c.hline(cx - 2, cx + 2, top - 3, mat["dk"])

    # the reveal: near-black, then the leaf set back inside it
    c.rect(x0, top, x1, sill, "#0d1216")
    if arch:
        c.ellipse(cx, top + 1, w // 2 - 1, 5, "#0d1216")
    # leaf, 2 px inset from each jamb and from the sill so the reveal shows all round
    leaf_top = top + (3 if arch else 2)
    if arch:
        c.ellipse(cx, top + 3, w // 2 - 3, 4, OAK["dk"])
    c.rect(x0 + 2, leaf_top, x1 - 2, sill - 1, OAK["base"])
    # lit left face of the leaf, shaded right, plank seams
    c.vline(x0 + 2, leaf_top, sill - 1, OAK["lt"])
    c.vline(x1 - 2, leaf_top, sill - 1, OAK["dk"])
    px_ = x0 + 4
    while px_ < x1 - 2:
        c.vline(px_, leaf_top + 1, sill - 2, OAK["dk"])
        px_ += 4
    if leaf == "double":
        c.vline(cx, leaf_top, sill - 1, OAK["dk"])
        c.vline(cx - 1, leaf_top, sill - 1, OAK["lt"])
    # iron straps + studs + ring handle
    for yy in (leaf_top + h // 3, sill - h // 3):
        c.hline(x0 + 3, x1 - 3, yy, IRON["dk"])
        c.hline(x0 + 3, x1 - 3, yy - 1, IRON["base"])
        for sx in range(x0 + 5, x1 - 4, 6):
            c.px(sx, yy - 1, IRON["hi"])
    hy = sill - h // 2
    c.px(cx + w // 4, hy, IRON["hi"])
    c.px(cx + w // 4, hy + 1, IRON["glint"])
    c.px(cx + w // 4, hy + 2, IRON["base"])
    # warm spill on the sill row only (the animation owns any moving light)
    c.hline(x0 + 2, x1 - 2, sill - 1, AMBER["dim"])
    c.hline(x0 + 4, x1 - 4, sill - 2, AMBER["deep"])

    # threshold: two stone steps, facing south, ending exactly on the canvas ground row
    for i in range(steps):
        yy = sill + i
        c.rect(x0 - 5 + i, yy, x1 + 5 - i, yy, mat["lt"] if i == 0 else mat["base"])
        c.hline(x0 - 5 + i, x1 + 5 - i, yy, mat["hi"] if i == 0 else mat["lt"])
        c.hline(x0 - 5 + i, x1 + 5 - i, yy + 1, mat["foot"])
    # the deepest step reaches one row below `sill`, so nothing floats above the ground line
    c.hline(x0 - 4, x1 + 4, sill + steps, mat["foot"])

    if lantern:
        for sx, sgn in ((x0 - 10, -1), (x1 + 10, 1)):
            c.rect(sx, top - 3, sx + 1, top, IRON["base"])
            c.rect(sx - 1, top, sx + 2, top + 4, IRON["dk"])
            c.px(sx, top + 1, AMBER["base"])
            c.px(sx, top + 2, AMBER["core"])
            c.px(sx + (1 if sgn < 0 else 0), top + 3, AMBER["dim"])
    if sign:
        c.vline(x0 - 12, top - 12, top - 6, IRON["base"])
        c.hline(x0 - 12, x0 - 7, top - 12, IRON["base"])
    return x0, top, x1, sill


def niche(c: Canvas, cx, sill, w=28, h=30, mat=STONE, counter=True, contents=None):
    """A NON-walkable recess: arcaded counter / work bay with a stone lip across the opening.

    Used on the zone-only facilities so the art stops advertising a door that has no interior.
    The lip is what separates it from `recessed_door` at gameplay zoom.
    """
    x0, x1 = cx - w // 2, cx - w // 2 + w - 1
    top = sill - h + 1
    c.rect(x0 - 4, top - 4, x1 + 4, sill, mat["dk"])
    c.rect(x0 - 3, top - 3, x1 + 3, sill - 1, mat["base"])
    c.hline(x0 - 4, x1 + 4, top - 4, mat["hi"])
    c.vline(x0 - 4, top - 4, sill, mat["lt"])
    c.vline(x1 + 4, top - 4, sill, mat["dk"])
    # arch + voussoirs
    c.ellipse(cx, top + 1, w // 2 + 2, 6, mat["lt"])
    c.ellipse(cx, top + 3, w // 2 - 1, 5, "#0b1014")
    c.rect(x0 + 1, top + 4, x1 - 1, sill - 1, "#0b1014")
    # interior depth: two bands, darkest at the crown
    c.rect(x0 + 2, top + 5, x1 - 2, sill - 2, "#111a20")
    c.rect(x0 + 3, top + 8, x1 - 3, sill - 2, "#16222a")
    if contents:
        contents(c, cx, sill, x0, x1, top)
    if counter:
        # stone lip: this is the affordance break
        c.rect(x0 - 2, sill - 4, x1 + 2, sill - 1, mat["lt"])
        c.hline(x0 - 2, x1 + 2, sill - 4, mat["hi"])
        c.hline(x0 - 2, x1 + 2, sill - 1, mat["foot"])
        c.rect(x0, sill - 3, x1, sill - 1, mat["base"])
        c.hline(x0, x1, sill - 4, mat["rim"])
    return x0, top, x1, sill


def window(c: Canvas, x, y, w, h, glow=AMBER, lit=1.0, shutters=False, flowerbox=False,
           frame=STONE, mullions=(2, 2), pad=2):
    """Deep-framed mullioned window with a bounded interior glow ramp (no flat yellow slab)."""
    c.rect(x - pad, y - pad, x + w + pad - 1, y + h + pad - 1, frame["dk"])
    if pad > 1:
        c.rect(x - pad + 1, y - pad + 1, x + w + pad - 2, y + h + pad - 2, frame["base"])
    c.hline(x - pad, x + w + pad - 1, y - pad, frame["lt"])
    c.vline(x - pad, y - pad, y + h + pad - 1, frame["lt"])
    c.hline(x - pad, x + w + pad - 1, y + h + pad - 1, frame["dk"])
    c.vline(x + w + pad - 1, y - pad, y + h + pad - 1, frame["dk"])
    c.rect(x, y, x + w - 1, y + h - 1, "#141b21")
    if lit > 0:
        c.rect(x + 1, y + 1, x + w - 2, y + h - 2, glow["deep"])
        c.rect(x + 1, y + 2, x + w - 2, y + h - 3, glow["dim"])
        head = max(0, (h - 4) // 3)
        c.rect(x + 1, y + 2 + head, x + w - 2, y + h - 3, glow["base"])
        if lit > 1.2:
            c.rect(x + 2, y + 3 + head, x + w - 3, y + h - 4, glow["lt"])
        if lit > 1.6:
            c.px(x + 2, y + h - 5, glow["core"])
    else:
        c.rect(x + 1, y + 1, x + w - 2, y + h - 2, "#1a2228")
        c.vline(x + 1, y + 1, y + h - 2, "#283038")
        c.hline(x + 1, x + w - 2, y + 1, "#283038")
    mx, my = mullions
    for i in range(1, mx):
        xx = x + (w * i) // mx
        c.vline(xx, y, y + h - 1, frame["dk"])
    for j in range(1, my):
        yy = y + (h * j) // my
        c.hline(x, x + w - 1, yy, frame["dk"])
        c.hline(x, x + w - 1, yy + 1, "#12181d")
    # sill: lit top, dark underside, and its own cast shade on the wall
    c.rect(x - pad - 1, y + h + pad, x + w + pad, y + h + pad + 1, frame["lt"])
    c.hline(x - pad - 1, x + w + pad, y + h + pad, frame["hi"])
    c.hline(x - pad - 1, x + w + pad, y + h + pad + 2, frame["foot"])
    if flowerbox:
        c.rect(x - 2, y + h + 5, x + w + 1, y + h + 8, OAK["base"])
        c.hline(x - 2, x + w + 1, y + h + 5, OAK["lt"])
        for bx in range(x - 1, x + w, 3):
            c.px(bx, y + h + 4, MOSS["lt"])
            c.px(bx + 1, y + h + 3, MOSS["base"])
    if shutters:
        for sx in (x - 7, x + w + 3):
            c.rect(sx, y - 1, sx + 3, y + h, OAK["base"])
            c.vline(sx + 1, y, y + h - 1, OAK["lt"])
            for yy in range(y + 2, y + h - 1, 4):
                c.hline(sx, sx + 3, yy, OAK["dk"])


def timber_frame(c: Canvas, x0, y0, x1, y1, posts=(), rails=(), braces=(), overhang=0):
    """Structural oak frame on the lit facade: posts, rails, diagonal braces."""
    for p in posts:
        c.rect(p, y0, p + 2, y1, OAK["dk"])
        c.vline(p + 1, y0 + 1, y1 - 1, OAK["base"])
    for ry in rails:
        c.rect(x0 - overhang, ry, x1 + overhang, ry + 2, OAK["dk"])
        c.hline(x0 - overhang, x1 + overhang, ry + 1, OAK["base"])
        c.hline(x0 - overhang, x1 + overhang, ry, OAK["lt"])
    for (bx0, by0, bx1, by1) in braces:
        c.line([(bx0, by0), (bx1, by1)], OAK["dk"], 3)
        c.line([(bx0 + 1, by0 + 1), (bx1, by1)], OAK["base"], 1)


def plinth(c: Canvas, x0, x1, sill, mat, seed=11, drop_base=0, quoins=True):
    """Foundation course: a proper 3 px plinth with its own top shadow, plus corner quoins.

    Rows `sill+1 .. H-1` are the ground contact; the renderer maps row H-3 to the world anchor,
    so the plinth reads as the building sitting ON the ground, not hovering above it.
    """
    base = sill + 2
    c.rect(x0, base - 2, x1, base, mat["foot"])
    c.hline(x0, x1, base - 2, mat["dk"])
    c.hline(x0, x1, base, "#141b20")
    # weathering: damp moss only where the sprite is already opaque
    r = rr(seed)
    for _ in range(max(2, (x1 - x0) // 26)):
        mx = x0 + 4 + r(max(2, x1 - x0 - 8))
        if c.opaque(mx, base - 1):
            c.px(mx, base - 1, MOSS["base"])
            c.px(mx + 1, base - 1, MOSS["dk"])
            c.px(mx, base, MOSS["dk"])
    if quoins:
        quoin_column(c, x0 + 1, base - 14 if base - 14 > 0 else 0, base, mat, seed=seed + 1,
                     width=5, left=False)


def ground_ao(c: Canvas, x0, x1, sill):
    """Tight baked contact rim on the ground rows (the engine also casts a real shadow)."""
    base = sill + 2
    for x in range(x0, x1 + 1):
        if c.opaque(x, base):
            c.blend(x, base, CONTACT, 0.44)
            c.blend(x, base - 1, AO, 0.18)


def edge_light(c: Canvas, strength=1.0):
    """Upper-left moon rim, lower-right shade. One px, no halo, no outline."""
    todo = []
    for y in range(c.h):
        for x in range(c.w):
            if not c.opaque(x, y):
                continue
            up = c.opaque(x, y - 1)
            left = c.opaque(x - 1, y)
            right = c.opaque(x + 1, y)
            down = c.opaque(x, y + 1)
            if not up:
                todo.append((x, y, "#c9d4dc", 0.17 * strength))
            elif not left:
                todo.append((x, y, "#b9c5cf", 0.09 * strength))
            if not down:
                todo.append((x, y, "#070c10", 0.30 * strength))
            elif not right:
                todo.append((x, y, "#070c10", 0.18 * strength))
    for x, y, col, amt in todo:
        c.blend(x, y, col, amt)


def smoke(c: Canvas, cx, top, n=3, seed=5):
    """Two-tone puffs that stay inside the flue's own column span, so a puff can never sit in
    a column with nothing under it (that is how a 1 px smoke speck becomes an alpha 'hole')."""
    r = rr(seed)
    for i in range(n):
        sx = cx - 1 + r(3)
        sy = top - 5 - i * 4
        srx = 2 + i
        if sy - srx < 2:            # never touch the transparent 1 px top border
            continue
        c.ellipse(sx, sy, srx, max(1, srx - 1), "#2f3941")
        c.ellipse(sx - 1, sy - 1, max(1, srx - 1), max(1, srx - 2), "#3f4a53")


def chimney(c: Canvas, x, y_top, y_bot, w=14, mat=STONE, cap=True):
    """Coursed chimney with a moulded cap; y_bot is inside the roof so it never floats."""
    c.rect(x, y_top, x + w - 1, y_bot, mat["dk"])
    c.rect(x + 1, y_top + 1, x + w - 2, y_bot, mat["base"])
    yy = y_top + 3
    while yy < y_bot:
        c.hline(x + 1, x + w - 2, yy, mat["mortar"])
        c.hline(x + 1, x + w - 2, yy + 1, mat["lt"])
        yy += 5
    c.vline(x + 1, y_top + 1, y_bot, mat["lt"])
    c.vline(x + w - 2, y_top + 1, y_bot, mat["dk"])
    if cap:
        c.rect(x, y_top - 4, x + w - 1, y_top, mat["base"])
        c.hline(x, x + w - 1, y_top - 4, mat["hi"])
        c.hline(x, x + w - 1, y_top, mat["dk"])
        c.rect(x + 1, y_top - 3, x + w - 2, y_top - 2, "#12171c")
        c.rect(x + 2, y_top - 4, x + w - 3, y_top - 3, "#1b232a")


def sign_board(c: Canvas, x, y, icon, w=15, h=12, hang=4):
    c.vline(x, y - hang - 2, y - 1, IRON["base"])
    c.hline(x - w // 2, x + w - 2, y - 2, IRON["base"])
    c.px(x - w // 2, y - 3, IRON["lt"])
    c.rect(x - w // 2, y, x + w // 2, y + h - 1, OAK["base"])
    c.rect(x - w // 2 + 1, y + 1, x + w // 2 - 1, y + h - 2, OAK["lt"])
    c.hline(x - w // 2, x + w // 2, y, OAK["hi"])
    c.hline(x - w // 2, x + w // 2, y + h - 1, OAK["dk"])
    draw_icon(c, x, y + h // 2 - 1, icon)


def draw_icon(c: Canvas, cx, cy, kind):
    if kind == "anvil":
        c.rect(cx - 4, cy - 1, cx + 3, cy + 1, IRON["lt"])
        c.rect(cx - 3, cy - 3, cx + 2, cy - 2, IRON["hi"])
        c.rect(cx - 1, cy + 2, cx + 1, cy + 3, IRON["base"])
        c.px(cx - 5, cy, IRON["hi"])
        c.px(cx - 3, cy - 3, "#aab6c0")
    elif kind == "book":
        c.rect(cx - 4, cy - 3, cx + 3, cy + 3, "#2b3b4c")
        c.rect(cx - 4, cy - 3, cx - 2, cy + 3, "#3d5468")
        c.hline(cx - 1, cx + 3, cy - 2, "#8fa3b5")
        c.hline(cx - 1, cx + 2, cy, "#8fa3b5")
        c.px(cx + 3, cy + 1, AMBER["base"])
    elif kind == "mug":
        c.rect(cx - 3, cy - 3, cx + 2, cy + 3, "#8a6a45")
        c.rect(cx - 2, cy - 2, cx + 1, cy + 2, "#c9a06a")
        c.hline(cx - 3, cx + 2, cy - 3, AMBER["core"])
        c.px(cx + 4, cy - 1, "#8a6a45")
        c.px(cx + 4, cy, "#8a6a45")
    elif kind == "moon":
        c.ellipse(cx, cy, 4, 4, "#7f8fa6")
        c.ellipse(cx + 2, cy - 1, 3, 3, OAK["base"])
        c.px(cx - 2, cy - 2, "#c9d4dc")
    elif kind == "retort":
        c.poly([(cx - 1, cy - 4), (cx + 1, cy - 4), (cx + 1, cy - 1), (cx - 1, cy - 1)],
               "#a98ec8")
        c.ellipse(cx, cy + 1, 3, 3, TEAL["lt"])
        c.ellipse(cx, cy + 1, 2, 2, TEAL["core"])
        c.px(cx - 2, cy - 2, "#cbb7e0")
    elif kind == "glyph":
        c.ellipse(cx, cy, 4, 4, CYAN["lt"])
        c.ellipse(cx + 2, cy - 1, 3, 3, OAK["base"])
        c.px(cx - 3, cy - 3, CYAN["core"])
    elif kind == "sword":
        c.line([(cx - 3, cy + 3), (cx + 3, cy - 3)], "#aab6c0", 2)
        c.hline(cx - 4, cx - 1, cy + 1, IRON["base"])
        c.px(cx + 3, cy - 4, "#dbe6ee")
    elif kind == "gear":
        c.ellipse(cx, cy, 3, 3, "#7c8b98")
        c.ellipse(cx, cy, 1, 1, "#2b333a")
        for dx, dy in ((0, -4), (0, 4), (-4, 0), (4, 0)):
            c.px(cx + dx, cy + dy, "#7c8b98")


# --------------------------------------------------------------------------- validation
def alpha_report(im: Image.Image) -> dict:
    w, h = im.size
    px = im.load()
    col_gaps = 0
    worst = []
    for x in range(w):
        runs, run = [], None
        for y in range(h):
            if px[x, y][3] >= 128:
                if run is None:
                    run = y
            else:
                if run is not None:
                    runs.append((run, y - 1))
                    run = None
        if run is not None:
            runs.append((run, h - 1))
        if len(runs) > 1:
            col_gaps += 1
            worst.append((x, runs))
    opaque = sum(1 for y in range(h) for x in range(w) if px[x, y][3] >= 128)
    semi = sum(1 for y in range(h) for x in range(w) if 0 < px[x, y][3] < 128)
    cols = sorted({x for y in range(h) for x in range(w) if px[x, y][3] >= 128})
    rows = sorted({y for y in range(h) for x in range(w) if px[x, y][3] >= 128})
    colors = len({px[x, y][:3] for y in range(h) for x in range(w) if px[x, y][3] >= 128})
    return dict(size=[w, h], opaque=opaque, opaque_pct=round(100.0 * opaque / (w * h), 1),
                semi=semi, col_gaps=col_gaps, worst=worst,
                x_range=[cols[0], cols[-1]] if cols else [0, 0],
                y_range=[rows[0], rows[-1]] if rows else [0, 0],
                bottom=rows[-1] if rows else -1, colors=colors)


def seal_seams(c: Canvas, max_gap: int = 7):
    """Close sub-`max_gap` px slits between two opaque runs in the same column.

    This is the single defect class P20/P21 shipped: the roof plane ends on one line and the wall
    below it starts on another, leaving a 1-6 px transparent slit that the camera sees straight
    through. Sealing it with the eave's own dark underside tone reads as the shadow under an
    overhang, which is what it should have been. Wider gaps are left alone: those are design
    (space between two towers), not a seam.
    """
    px = c.im.load()
    for x in range(c.w):
        runs, run = [], None
        for y in range(c.h):
            if px[x, y][3]:
                if run is None:
                    run = y
            else:
                if run is not None:
                    runs.append((run, y - 1))
                    run = None
        if run is not None:
            runs.append((run, c.h - 1))
        for i in range(len(runs) - 1):
            gap0 = runs[i][1] + 1
            gap1 = runs[i + 1][0] - 1
            if 1 <= gap1 - gap0 + 1 <= max_gap:
                r, g, b, _ = px[x, runs[i][1]]
                shade = (int(r * 0.68), int(g * 0.68), int(b * 0.72), 255)
                for y in range(gap0, gap1 + 1):
                    px[x, y] = shade


def prune_specks(c: Canvas, min_area: int = 12):
    """Remove sub-`min_area` pixel islands (1 px line spill, stray dust).

    Builders draw diagonal edges with integer rounding, which can leave one or two pixels just
    outside the intended silhouette. Those specks are not part of the building, but a strict
    per-column alpha gate would still flag them, so they are pruned before validation. Anything
    that is real art (a smoke puff, a boulder, a petal) is far above the threshold.
    """
    w, h = c.w, c.h
    px = c.im.load()
    seen = [[False] * w for _ in range(h)]
    for y0 in range(h):
        for x0 in range(w):
            if seen[y0][x0] or px[x0, y0][3] == 0:
                continue
            stack = [(x0, y0)]
            seen[y0][x0] = True
            comp = []
            while stack:
                x, y = stack.pop()
                comp.append((x, y))
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and not seen[ny][nx] and px[nx, ny][3]:
                        seen[ny][nx] = True
                        stack.append((nx, ny))
            if len(comp) < min_area:
                for x, y in comp:
                    px[x, y] = (0, 0, 0, 0)


def check_solid(c: Canvas, label: str, allow_gaps: int = 0, x_from=None, x_to=None):
    """Raise if any column inside the mass has a transparent hole in it."""
    rep = alpha_report(c.im)
    if rep["col_gaps"] > allow_gaps:
        raise SystemExit(f"{label}: {rep['col_gaps']} columns contain an interior alpha hole "
                         f"(allowed {allow_gaps}) e.g. {rep['worst']}")
    if rep["semi"] != 0:
        raise SystemExit(f"{label}: {rep['semi']} semi-transparent pixels (halo)")
    x0, x1 = rep["x_range"]
    for x in range(x0, x1 + 1):
        if not c.opaque(x, rep["bottom"]) and not c.opaque(x, rep["bottom"] - 1):
            pass
    return rep


# --------------------------------------------------------------------------- mass assembly
def house_mass(c: Canvas, *, x0, xm, x1, eave, ridge_y, rise_r, drop_r, jetty, base,
               stone=STONE, upper=PLAS, roof_ramp=SLATE, seed=1, overhang=8, rows=9, tile=12,
               finial=False, roof_ridge_y=None):
    """Paint an OPAQUE two-face mass with a hip roof joined to the wall top.

    Vertical order on the lit front face: roof ridge -> pitch -> eave fascia -> soffit band ->
    wall top -> jettied plaster storey -> jetty beam -> coursed stone ground floor -> plinth ->
    ground row. The right-hand face recedes up-right and drops 2 value steps.

    Returns a dict of the rows the details need (front wall top, jetty line, base).
    """
    ridge_y = ridge_y if roof_ridge_y is None else roof_ridge_y
    # ---- 1. silhouette first: two solid faces, no holes possible inside them -------------
    c.poly([(x0, eave), (xm, eave), (xm, base + 1), (x0, base + 1)], upper["base"])
    c.poly([(xm, eave), (x1, eave - rise_r), (x1, base + 1 - drop_r), (xm, base + 1)],
           upper["dk"])
    # ---- 2. lit front face: plaster storey over coursed stone --------------------------
    c.poly([(x0, eave), (xm, eave), (xm, jetty), (x0, jetty)], upper["base"])
    ashlar(c, [(x0, eave), (xm, eave), (xm, jetty), (x0, jetty)], upper, row_h=8, block_w=22,
           seed=seed + 101)
    c.poly([(x0, jetty), (xm, jetty), (xm, base + 1), (x0, base + 1)], stone["base"])
    ashlar(c, [(x0, jetty), (xm, jetty), (xm, base + 1), (x0, base + 1)], stone, row_h=7,
           block_w=15, seed=seed + 102)
    # ---- 3. shaded right return: same courses, receding, 2 steps darker ---------------
    c.poly([(xm, eave), (x1, eave - rise_r), (x1, jetty - 6), (xm, jetty)], upper["dk"])
    n = 5
    for i in range(1, n):
        t = i / n
        c.line([(xm, eave + (jetty - eave) * t),
                (x1, eave - rise_r + (jetty - 6 - (eave - rise_r)) * t)],
               upper["mortar"], 1)
    for i in range(1, 5):
        xx = xm + (x1 - xm) * i / 5
        c.vline(xx, eave - rise_r * i / 5 + 1, jetty - 6 * i / 5, upper["mortar"])
    c.poly([(xm, jetty), (x1, jetty - 6), (x1, base + 1 - drop_r), (xm, base + 1)],
           stone["dk"])
    rows_r = max(3, int((base - jetty) / 8))
    for i in range(1, rows_r):
        t = i / rows_r
        c.line([(xm, jetty + (base + 1 - jetty) * t),
                (x1, jetty - 6 + (base + 1 - drop_r - (jetty - 6)) * t)], stone["mortar"], 1)
        c.line([(xm, jetty + 1 + (base + 1 - jetty) * t),
                (x1, jetty - 5 + (base + 1 - drop_r - (jetty - 6)) * t)], stone["base"], 1)
    for i in range(1, 5):
        xx = xm + (x1 - xm) * i / 5
        c.vline(xx, jetty - 6 * i / 5, base + 1 - drop_r * i / 5, stone["mortar"])
    # corner edges: lit left, dark right, so the two faces read as separate planes
    c.vline(xm - 1, eave, base, upper["hi"])
    c.vline(xm, eave, base, stone["mortar"])
    c.vline(x1, eave - rise_r, base - drop_r, "#0e1418")
    c.line([(x0, eave), (xm, eave)], upper["lt"], 1)
    c.line([(xm, eave), (x1, eave - rise_r)], upper["lt"], 1)
    # ---- 4. jetty beam: dark soffit band with joist ends, flush with both wall corners --
    # (it must not project past the wall: an overhanging beam with no wall under it is the
    # same "floating plate" defect the roofs had)
    c.poly([(x0, jetty - 1), (xm, jetty - 1), (xm, jetty + 3), (x0, jetty + 3)], "#12181d")
    c.hline(x0, xm, jetty - 1, OAK["lt"])
    bx = x0 + 1
    while bx < xm - 1:
        c.rect(bx, jetty + 1, bx + 2, jetty + 3, OAK["base"])
        c.px(bx, jetty + 1, OAK["lt"])
        bx += 9
    # ---- 5. roof: eave -> pitch -> ridge, then the opaque soffit that ties it to the wall
    eave_l = (x0, eave - 6)          # flush with the wall corner: no sky under the eave
    eave_m = (xm + overhang - 2, eave - 6)
    ridge_l = (x0 + 30, ridge_y)
    ridge_m = (xm - 4, ridge_y)
    hip_tip = (x1 + overhang, eave - 6 - rise_r)
    hip_roof(c, eave_l, eave_m, ridge_l, ridge_m, hip_tip, roof_ramp, seed=seed + 200,
             rows=rows, tile=tile, finial=finial)
    soffit(c, eave_l, eave_m, band=max(3, eave - (eave - 6) - 1))
    c.poly([(eave_m[0], eave_m[1] + 4), hip_tip,
            (hip_tip[0], hip_tip[1] + 5), (eave_m[0], eave_m[1] + 5)], "#141a20")
    # gable/hip shadow falling on the wall under the overhang
    c.poly([(x0, eave), (xm, eave), (xm, eave + 3), (x0, eave + 3)], "#161d22")
    c.poly([(xm, eave), (x1, eave - rise_r), (x1, eave - rise_r + 3), (xm, eave + 3)],
           "#10161b")
    return dict(front_top=eave, jetty=jetty, base=base, xm=xm, x1=x1, x0=x0,
                side_top=eave - rise_r, side_base=base - drop_r)


def carved_band(c: Canvas, x0, x1, y, mat, h=4):
    """Cornice / string course: lit top, dark bed, repeating dentils."""
    c.rect(x0, y, x1, y + h - 1, mat["base"])
    c.hline(x0, x1, y, mat["hi"])
    c.hline(x0, x1, y + h - 1, mat["dk"])
    for x in range(x0 + 2, x1 - 1, 6):
        c.px(x, y + h - 2, mat["lt"])


# --------------------------------------------------------------------------- 1. ARMORY
def build_armory() -> Canvas:
    """190x166 — smithy. Door re-authored onto the playable trigger (993, 655).

    P20 painted the doorway at x 133..150 / y 109..139, which inverts to world (1069.4, 601.7):
    76 units east and 120 north of the trigger, so the player who walked to the painted door was
    aiming at nothing. The trigger is NOT moved; the art is. At scale 0.80 the trigger inverts to
    canvas centre X (93.75) and row H-3 (163), so the door is cut there.
    """
    W, H = 190, 166
    c = Canvas(W, H)
    base, sill = H - 1, H - 3
    x0, xm, x1 = 20, 116, 176
    m = house_mass(c, x0=x0, xm=xm, x1=x1, eave=96, ridge_y=26, rise_r=16, drop_r=18,
                   jetty=118, base=base, stone=STONE, upper=PLAS, roof_ramp=SHING, seed=2300,
                   overhang=8, rows=9, tile=11, finial=True)
    # ---- timber frame on the lit plaster storey (armory speaks the village language) ----
    timber_frame(c, x0, 98, xm, 116, posts=(x0 + 4, x0 + 40, x0 + 76),
                 rails=(104,), braces=[(x0 + 6, 114, x0 + 36, 102)])
    # ---- coursed stone with corner quoins and a foundation course -----------------------
    quoin_column(c, x0 + 1, 118, base - 2, STONE, seed=2301, width=5, left=False)
    quoin_column(c, xm, 118, base - 2, STONE, seed=2302, width=5, left=True)
    plinth(c, x0 - 2, xm + 2, sill, STONE, seed=2303)
    # ---- chimney: on the roof, coursed, with its own roof shadow ------------------------
    chimney(c, 34, 16, 58, w=15, mat=STONE)
    smoke(c, 41, 16, n=3, seed=2310)
    c.blend_rect(50, 12, 64, 30, "#0a1015", 0.34)   # chimney shade on the roof plane
    # ---- the doorway the trigger lands on ----------------------------------------------
    dx0, dtop, dx1, dsill = recessed_door(c, cx=94, sill=sill, w=28, h=36, mat=STONE,
                                          arch=True, leaf="double", steps=2, lantern=True)
    # warm forge spill inside the reveal + a soot band over the arch
    c.rect(dx0 + 3, dtop + 4, dx1 - 3, dtop + 7, "#1a1208")
    c.hline(dx0 + 5, dx1 - 5, dtop + 6, AMBER["deep"])
    # ---- windows: shutters on the lit face, a forge-glow pair on the return -------------
    window(c, 34, 126, 14, 15, glow=AMBER, lit=1.5, shutters=True, pad=2, mullions=(2, 2))
    window(c, 128, 126, 13, 14, glow=AMBER, lit=1.2, pad=2, mullions=(2, 1))
    window(c, 152, 128, 11, 12, glow=AMBER, lit=1.0, pad=2, mullions=(2, 1))
    # ---- hanging anvil sign + a barrel and crate stacked against the wall --------------
    sign_board(c, 152, 112, "anvil", w=15, h=12)
    c.rect(166, 146, 176, 165, OAK["base"])
    c.rect(167, 147, 175, 164, OAK["lt"])
    for yy in (150, 158):
        c.hline(166, 176, yy, IRON["base"])
    c.ellipse(171, 146, 5, 2, OAK["lt"])
    ground_ao(c, x0 - 4, xm + 4, sill)
    edge_light(c)
    return c


# --------------------------------------------------------------------------- 2. HERO QUARTERS
def build_hero_quarters() -> Canvas:
    """200x168 — dormitory. Trigger (823, 790) inverts to canvas centre X (98.75) / row H-3."""
    W, H = 200, 168
    c = Canvas(W, H)
    base, sill = H - 1, H - 3
    x0, xm, x1 = 18, 124, 184
    m = house_mass(c, x0=x0, xm=xm, x1=x1, eave=96, ridge_y=30, rise_r=15, drop_r=20,
                   jetty=122, base=base, stone=STONE, upper=PLAS, roof_ramp=SHING, seed=2400,
                   overhang=8, rows=8, tile=12)
    # lean-to wing on the left: reads as a building that grew over time
    wx0, wx1, wy = 8, x0 + 4, 120
    c.rect(wx0, wy, wx1, base + 1, STONE["base"])
    ashlar(c, [(wx0, wy), (wx1, wy), (wx1, base + 1), (wx0, base + 1)], STONE, row_h=7,
           block_w=11, seed=2410)
    shingle_plane(c, (wx0, wy - 1), (wx1, wy - 1), (wx0 + 2, wy - 15),
                  (wx1, wy - 15), SHING, seed=2411, rows=5, tile=9)
    c.line([(wx0, wy - 1), (wx1, wy - 1)], SHING["lt"], 1)
    c.line([(wx0, wy + 1), (wx1, wy + 1)], "#141a20", 2)
    c.hline(wx0, wx1, wy + 2, "#161d22")
    window(c, wx0 + 6, wy + 12, 9, 11, glow=AMBER, lit=1.2, pad=1, mullions=(2, 1))
    # dormer through the front pitch
    dx0, dx1, dy = 66, 96, 60
    c.poly([(dx0, dy), (dx1, dy), (dx1, dy + 20), (dx0, dy + 20)], PLAS["base"])
    ashlar(c, [(dx0, dy), (dx1, dy), (dx1, dy + 20), (dx0, dy + 20)], PLAS, row_h=8,
           block_w=14, seed=2412)
    c.poly([(dx0 - 4, dy + 1), ((dx0 + dx1) // 2, dy - 17), (dx1 + 4, dy + 1)], SHING["base"])
    c.line([(dx0 - 4, dy + 1), ((dx0 + dx1) // 2, dy - 17)], SHING["hi"], 1)
    c.line([((dx0 + dx1) // 2, dy - 17), (dx1 + 4, dy + 1)], SHING["dk"], 1)
    c.rect(dx0 - 4, dy + 1, dx1 + 4, dy + 2, SHING["dk"])
    window(c, dx0 + 7, dy + 5, 12, 12, glow=AMBER, lit=1.7, pad=1, mullions=(2, 1))
    # chimney + vent
    chimney(c, 30, 14, 58, w=13, mat=STONE)
    smoke(c, 36, 14, n=2, seed=2413)
    c.rect(150, m["side_top"] + 8, 162, m["side_top"] + 22, OAK["dk"])
    c.rect(151, m["side_top"] + 9, 161, m["side_top"] + 21, OAK["base"])
    for yy in range(m["side_top"] + 11, m["side_top"] + 20, 2):
        c.hline(151, 161, yy, OAK["lt"])
    timber_frame(c, x0, 98, xm, 120, posts=(x0 + 5, x0 + 44, x0 + 82), rails=(112,))
    quoin_column(c, x0 + 1, 122, base - 2, STONE, seed=2401, width=5, left=False)
    quoin_column(c, xm, 122, base - 2, STONE, seed=2402, width=5, left=True)
    plinth(c, x0 - 2, xm + 2, sill, STONE, seed=2403)
    # windows
    window(c, 32, 128, 13, 15, glow=AMBER, lit=1.4, shutters=True, pad=2)
    window(c, 72, 132, 13, 15, glow=AMBER, lit=0.0, pad=2, flowerbox=True)
    window(c, 138, 132, 12, 14, glow=AMBER, lit=1.1, pad=2)
    # the door, then its porch canopy that actually rests on two posts
    dx0, dtop, dx1, dsill = recessed_door(c, cx=98, sill=sill, w=26, h=34, mat=STONE,
                                          arch=True, leaf="plank", steps=2, lantern=True)
    px0, px1 = dx0 - 7, dx1 + 7
    cy0 = dtop - 10
    # hood: two shallow pitches over the door, resting on two posts that reach the ground row
    c.poly([(px0, cy0 + 8), (px0 + 8, cy0 - 4), ((px0 + px1) // 2, cy0 - 7),
            (px1 - 8, cy0 - 4), (px1, cy0 + 8)], SHING["base"])
    c.poly([(px0, cy0 + 8), (px0 + 8, cy0 - 4), ((px0 + px1) // 2, cy0 - 7),
            ((px0 + px1) // 2, cy0 - 2), (px0 + 4, cy0 + 8)], SHING["lt"])
    c.line([(px0, cy0 + 8), (px0 + 8, cy0 - 4)], SHING["hi"], 1)
    c.line([(px0 + 8, cy0 - 4), ((px0 + px1) // 2, cy0 - 7)], SHING["hi"], 1)
    c.line([(px1, cy0 + 8), (px1 - 8, cy0 - 4)], SHING["dk"], 1)
    c.rect(px0, cy0 + 9, px1, cy0 + 10, "#191d1f")
    c.hline(px0, px1, cy0 + 9, "#2a2e30")
    for px in (px0, px1 - 2):
        c.rect(px, cy0 + 11, px + 2, base - 1, OAK["dk"])
        c.vline(px + 1, cy0 + 12, base - 2, OAK["base"])
    # lived-in props, all inside the mass so they cannot punch a column hole
    c.rect(20, base - 12, 40, base - 10, OAK["lt"])
    c.rect(22, base - 9, 24, base, OAK["dk"])
    c.rect(36, base - 9, 38, base, OAK["dk"])
    c.rect(158, base - 20, 170, base, OAK["base"])
    c.vline(158, base - 20, base, OAK["lt"])
    c.hline(158, 170, base - 14, IRON["base"])
    c.hline(158, 170, base - 6, IRON["base"])
    sign_board(c, 138, base - 30, "moon", w=14, h=11)
    ground_ao(c, x0 - 4, xm + 4, sill)
    edge_light(c)
    return c


# --------------------------------------------------------------------------- 3. RESTAURANT
def build_restaurant() -> Canvas:
    """206x172 — dining hall. Trigger (1103, 925) inverts to canvas centre X (101.75) / H-3."""
    W, H = 206, 172
    c = Canvas(W, H)
    base, sill = H - 1, H - 3
    x0, xm, x1 = 16, 128, 190
    m = house_mass(c, x0=x0, xm=xm, x1=x1, eave=98, ridge_y=30, rise_r=16, drop_r=20,
                   jetty=124, base=base, stone=STONE, upper=PLAS, roof_ramp=SLATE, seed=2500,
                   overhang=8, rows=8, tile=13)
    # hearth stack on the left, smoking, with its shadow raking across the roof
    chimney(c, 22, 20, 78, w=17, mat=STONE)
    smoke(c, 29, 20, n=3, seed=2510)
    c.blend_rect(39, 18, 58, 44, "#0a1015", 0.34)   # hearth stack shade on the roof plane
    # roof dormer
    dx0, dx1, dy = 100, 128, 62
    c.poly([(dx0, dy), (dx1, dy), (dx1, dy + 18), (dx0, dy + 18)], PLAS["base"])
    c.poly([(dx0 - 5, dy + 1), ((dx0 + dx1) // 2, dy - 16), (dx1 + 5, dy + 1)], SLATE["base"])
    c.line([(dx0 - 5, dy + 1), ((dx0 + dx1) // 2, dy - 16)], SLATE["hi"], 1)
    c.line([((dx0 + dx1) // 2, dy - 16), (dx1 + 5, dy + 1)], SLATE["dk"], 1)
    window(c, dx0 + 6, dy + 4, 12, 12, glow=AMBER, lit=1.8, pad=1, mullions=(2, 1))
    timber_frame(c, x0, 100, xm, 122, posts=(x0 + 4, x0 + 40, x0 + 78, x0 + 106),
                 rails=(112,))
    quoin_column(c, x0 + 1, 124, base - 2, STONE, seed=2501, width=5, left=False)
    quoin_column(c, xm, 124, base - 2, STONE, seed=2502, width=5, left=True)
    plinth(c, x0 - 2, xm + 2, sill, STONE, seed=2503)
    # projecting entrance gable: a bay that breaks the long facade around the real door
    gx0, gx1 = 82, 122
    c.poly([(gx0, 124), (gx1, 124), (gx1, base + 1), (gx0, base + 1)], PLAS["base"])
    ashlar(c, [(gx0, 124), (gx1, 124), (gx1, base + 1), (gx0, base + 1)], PLAS, row_h=8,
           block_w=13, seed=2511)
    timber_frame(c, gx0, 126, gx1, base - 2, posts=(gx0 + 1, gx1 - 3), rails=(128,))
    c.poly([(gx0 - 7, 124), ((gx0 + gx1) // 2, 100), (gx1 + 7, 124)], SLATE["base"])
    c.line([(gx0 - 7, 124), ((gx0 + gx1) // 2, 100)], SLATE["hi"], 1)
    c.line([((gx0 + gx1) // 2, 100), (gx1 + 7, 124)], SLATE["dk"], 1)
    c.rect(gx0 - 7, 125, gx1 + 7, 126, "#141a20")
    c.poly([(gx0 - 7, 126), (gx0, 126), (gx0, 132), (gx0 - 7, 132)], "#161d22")
    c.poly([(gx1, 126), (gx1 + 7, 126), (gx1 + 7, 132), (gx1, 132)], "#10161b")
    c.px((gx0 + gx1) // 2, 99, LIME["hi"])
    # the door inside the gable bay
    dx0, dtop, dx1, dsill = recessed_door(c, cx=101, sill=sill, w=26, h=34, mat=STONE,
                                          arch=False, leaf="double", steps=2, lantern=True)
    # striped canvas awning across the terrace bay: one flat plane on a slight pitch, a lit
    # top edge, a dark underside, and a scalloped valance. Straight runs, not lumpy blobs.
    ax0, ax1, ay = 26, 74, 126
    c.poly([(ax0, ay + 5), (ax0 + 3, ay - 3), (ax1, ay - 3), (ax1 + 3, ay + 5)],
           "#6d5330")
    stripe = 0
    x = ax0
    while x < ax1:
        warm = stripe % 2 == 0
        col = "#6d5330" if warm else "#7d7a6d"
        top = "#8a6a3c" if warm else "#9a968a"
        c.poly([(x, ay + 5), (x + 3, ay - 3), (min(x + 7, ax1), ay - 3), (min(x + 7, ax1), ay + 5)],
               col)
        c.line([(x + 3, ay - 3), (min(x + 7, ax1), ay - 3)], top, 1)
        x += 7
        stripe += 1
    c.poly([(ax0 + 3, ay - 4), (ax1, ay - 4), (ax1, ay - 3), (ax0 + 3, ay - 3)], "#a98f60")
    # valance: scalloped bottom edge, one notch per stripe
    for xx in range(ax0, ax1 + 3, 6):
        c.poly([(xx, ay + 5), (xx + 6, ay + 5), (xx + 3, ay + 8)], "#4f3d24")
    c.rect(ax0, ay + 5, ax1 + 3, ay + 6, "#241c12")
    c.rect(ax0 + 1, ay + 9, ax1 + 2, ay + 10, "#10161b")   # shade line on the wall
    for px in (ax0 + 2, ax1 - 1):
        c.rect(px, ay + 10, px + 1, base - 1, OAK["dk"])
        c.vline(px + 1, ay + 11, base - 2, OAK["lt"])
    window(c, 134, 130, 16, 18, glow=AMBER, lit=1.3, pad=2, mullions=(3, 2))
    window(c, 160, 132, 13, 15, glow=AMBER, lit=1.1, pad=2)
    window(c, 30, 134, 16, 18, glow=AMBER, lit=1.6, pad=2, mullions=(3, 2))
    # terrace props
    c.rect(168, base - 26, 182, base, OAK["base"])
    c.vline(168, base - 26, base, OAK["lt"])
    c.hline(168, 182, base - 19, IRON["base"])
    c.hline(168, 182, base - 8, IRON["base"])
    sign_board(c, 152, base - 52, "mug", w=15, h=12)
    ground_ao(c, x0 - 4, xm + 4, sill)
    edge_light(c)
    return c


# --------------------------------------------------------------------------- 4. HERO REGISTRY
def build_hero_registry() -> Canvas:
    """188x166 — civic archive. Zone-only: an arcaded loggia, NOT a walkable door.

    P21 painted a residential plank door on this building even though it has no interior map, so
    the art advertised an entrance that does not exist. The ground floor is a loggia of three
    arches with a stone counter lip and a lectern, which reads as "walk up and use the desk".
    """
    W, H = 188, 166
    c = Canvas(W, H)
    base, sill = H - 1, H - 3
    x0, xm, x1 = 12, 116, 176
    m = house_mass(c, x0=x0, xm=xm, x1=x1, eave=94, ridge_y=28, rise_r=15, drop_r=18,
                   jetty=118, base=base, stone=STONE, upper=PLAS, roof_ramp=SLATE, seed=2600,
                   overhang=8, rows=8, tile=12, finial=True)
    # bell turret + clock on the ridge: the registry's 0.36-zoom silhouette marker
    tx = 78
    c.rect(tx - 14, 12, tx + 14, 32, PLAS["base"])
    c.rect(tx - 13, 13, tx + 13, 31, PLAS["lt"])
    for yy in range(15, 31, 5):
        c.hline(tx - 13, tx + 13, yy, PLAS["mortar"])
    c.rect(tx - 14, 7, tx + 14, 12, PLAS["dk"])
    c.hline(tx - 14, tx + 14, 7, PLAS["hi"])
    c.poly([(tx - 14, 8), (tx, 1), (tx + 14, 8)], SLATE["base"])
    c.line([(tx - 14, 8), (tx, 1)], SLATE["hi"], 1)
    c.px(tx, 2, "#e6eef4")
    c.rect(tx - 6, 14, tx + 6, 23, PLAS["dk"])
    for yy in range(15, 22, 2):
        c.hline(tx - 5, tx + 5, yy, OAK["base"])
    c.ellipse(tx, 28, 7, 6, "#232a31")
    c.ellipse(tx, 28, 6, 5, BRONZE["base"])
    c.ellipse(tx, 28, 5, 4, "#39444d")
    for ox_, oy_ in ((-3, 0), (3, 0), (0, -2), (0, 2)):
        c.px(tx + ox_, 28 + oy_, BRONZE["hi"])
    c.vline(tx, 26, 28, "#dcc588")
    c.px(tx + 1, 28, "#dcc588")
    c.px(tx + 2, 29, "#dcc588")
    chimney(c, 60, 8, 40, w=13, mat=STONE)
    timber_frame(c, x0, 96, xm, 116, posts=(x0 + 5, x0 + 38, x0 + 74, x0 + 98), rails=(108,))
    quoin_column(c, x0 + 1, 118, base - 2, STONE, seed=2601, width=5, left=False)
    quoin_column(c, xm, 118, base - 2, STONE, seed=2602, width=5, left=True)
    plinth(c, x0 - 2, xm + 2, sill, STONE, seed=2603)
    # ---- ground floor loggia: three arcs, counter lip, lectern --------------------------
    def ledger(c_, cx_, sill_, xa, xb, top):
        c_.rect(cx_ - 5, sill_ - 9, cx_ + 5, sill_ - 6, "#4d657a")
        c_.hline(cx_ - 6, cx_ + 6, sill_ - 10, "#8fa3b5")
        c_.rect(cx_ - 3, sill_ - 5, cx_ - 2, sill_ - 4, OAK["base"])
        c_.rect(cx_ + 2, sill_ - 5, cx_ + 3, sill_ - 4, OAK["base"])

    for i, a in enumerate((40, 68, 96)):
        niche(c, cx=a, sill=sill, w=22, h=26, mat=STONE, counter=True,
              contents=(ledger if i == 1 else None))
    # bronze plaque row above the arcs
    for i, px_ in enumerate((30, 42, 54)):
        c.rect(px_, 96, px_ + 9, 102, BRONZE["dk"])
        c.rect(px_ + 1, 97, px_ + 8, 101, BRONZE["base"])
        c.hline(px_ + 2, px_ + 7, 99, BRONZE["hi"])
    window(c, 30, 122, 12, 14, glow=AMBER, lit=1.2, pad=2)
    window(c, 130, 124, 12, 14, glow=AMBER, lit=1.0, pad=2)
    window(c, 152, 126, 11, 13, glow=AMBER, lit=0.0, pad=2, flowerbox=True)
    sign_board(c, 150, 112, "book", w=16, h=13)
    # stone bench + lectern on the terrace
    c.rect(112, base - 16, 132, base - 14, STONE["lt"])
    c.hline(112, 132, base - 16, STONE["hi"])
    c.rect(114, base - 13, 116, base, STONE["base"])
    c.rect(128, base - 13, 130, base, STONE["base"])
    c.rect(24, base - 18, 44, base - 16, OAK["dk"])
    c.rect(25, base - 17, 43, base - 16, "#3d5468")
    c.rect(27, base - 15, 29, base, OAK["base"])
    c.rect(39, base - 15, 41, base, OAK["base"])
    ground_ao(c, x0 - 4, xm + 4, sill)
    edge_light(c)
    return c


# --------------------------------------------------------------------------- 5. SYNTHESIS LAB
def build_synthesis_lab() -> Canvas:
    """188x168 — alchemical workshop. Zone-only: an open workshop bay with a bench in it."""
    W, H = 188, 168
    c = Canvas(W, H)
    base, sill = H - 1, H - 3
    x0, xm, x1 = 12, 112, 174
    m = house_mass(c, x0=x0, xm=xm, x1=x1, eave=96, ridge_y=28, rise_r=16, drop_r=19,
                   jetty=116, base=base, stone=STONE, upper=PLAS, roof_ramp=SLATE, seed=2700,
                   overhang=8, rows=8, tile=12)
    # copper condenser stack: the workshop's silhouette marker
    c.rect(120, 32, 140, 88, "#5a3a2a")
    c.rect(121, 33, 139, 86, "#7a4f34")
    c.vline(122, 34, 85, "#9a6644")
    c.vline(138, 34, 85, "#4a2e22")
    for yy in range(38, 84, 7):
        c.hline(121, 139, yy, "#3d2519")
        c.hline(121, 139, yy + 1, "#8f5f3e")
    c.rect(120, 26, 140, 34, "#4a2e22")
    c.hline(120, 140, 26, "#a06e4a")
    c.rect(124, 28, 136, 31, "#1d2328")
    c.px(128, 22, "#3fbfae")
    c.px(132, 19, "#2f9d90")
    c.px(135, 16, "#22645f")
    # ridge vent cupola
    c.rect(58, 18, 76, 30, "#5a3a2a")
    c.rect(59, 19, 75, 29, "#7a4f34")
    c.hline(58, 76, 18, "#a06e4a")
    c.poly([(58, 18), (67, 7), (76, 18)], SLATE["base"])
    c.line([(58, 18), (67, 7)], SLATE["hi"], 1)
    c.line([(67, 7), (76, 18)], SLATE["dk"], 1)
    for yy in range(21, 28, 2):
        c.hline(60, 74, yy, "#3d2519")
    timber_frame(c, x0, 98, xm, 114, posts=(x0 + 4, x0 + 38, x0 + 74, x0 + 96), rails=(106,))
    quoin_column(c, x0 + 1, 116, base - 2, STONE, seed=2701, width=5, left=False)
    quoin_column(c, xm, 116, base - 2, STONE, seed=2702, width=5, left=True)
    plinth(c, x0 - 2, xm + 2, sill, STONE, seed=2703)
    # ---- open workshop bay: bench across the opening, teal interior --------------------
    def bench(c_, cx_, sill_, xa, xb, top):
        c_.rect(xa + 1, sill_ - 12, xb - 1, sill_ - 10, OAK["lt"])
        c_.hline(xa + 1, xb - 1, sill_ - 10, "#b98a54")
        c_.rect(xa + 3, sill_ - 9, xa + 5, sill_ - 1, OAK["dk"])
        c_.rect(xb - 6, sill_ - 9, xb - 4, sill_ - 1, OAK["dk"])
        c_.rect(cx_ - 4, sill_ - 18, cx_ + 4, sill_ - 13, "#2b4148")
        c_.ellipse(cx_, sill_ - 18, 4, 3, TEAL["dim"])
        c_.px(cx_ - 1, sill_ - 19, TEAL["base"])

    niche(c, cx=62, sill=sill, w=42, h=30, mat=STONE, counter=True, contents=bench)
    # plastered teal glow behind the bay
    c.blend_rect(45, sill - 24, 79, sill - 6, TEAL["deep"], 0.16)
    window(c, 24, 122, 12, 14, glow=TEAL, lit=1.2, pad=2)
    window(c, 96, 124, 11, 13, glow=TEAL, lit=0.0, pad=2)
    window(c, 140, 126, 11, 13, glow=TEAL, lit=1.1, pad=2)
    window(c, 158, 128, 10, 12, glow=TEAL, lit=1.0, pad=2)
    # glass retort on the terrace, iron stand, coiled worm
    rx, ry = 146, base - 22
    c.rect(rx - 9, ry - 24, rx + 9, ry - 22, IRON["base"])
    c.vline(rx + 8, ry - 22, ry + 2, IRON["base"])
    c.rect(rx - 12, ry + 2, rx + 12, ry + 4, IRON["base"])
    c.ellipse(rx, ry - 12, 9, 11, "#2b3f47")
    c.ellipse(rx, ry - 12, 8, 10, "#3d5b63")
    c.ellipse(rx, ry - 10, 6, 6, TEAL["deep"])
    c.ellipse(rx, ry - 9, 5, 5, TEAL["base"])
    c.ellipse(rx - 1, ry - 10, 2, 2, TEAL["core"])
    c.rect(rx - 2, ry - 24, rx + 2, ry - 21, "#3d5b63")
    c.px(rx - 6, ry - 16, "#8fbac2")
    for k in range(3):
        c.ellipse(rx + 15, ry - 6 + k * 5, 5, 2, "#7a4f34")
        c.px(rx + 19, ry - 6 + k * 5, "#a06e4a")
    c.rect(24, base - 16, 42, base, "#3d2519")
    c.rect(25, base - 15, 41, base - 1, "#5c3a24")
    c.hline(24, 42, base - 9, "#8f5f3e")
    c.vline(33, base - 15, base - 1, "#8f5f3e")
    sign_board(c, 128, base - 44, "retort", w=15, h=12)
    ground_ao(c, x0 - 4, xm + 4, sill)
    edge_light(c)
    return c


# --------------------------------------------------------------------------- 6. SUMMONING HALL
def build_summoning_hall() -> Canvas:
    """268x206 - terraced ritual plaza + hexastyle temple, built as ONE solid mass.

    P21's version had the failure the camera exposes worst: the entablature floated above the
    colonnade with grass visible between them, the right return stopped short of the cornice,
    and the podium hovered over a wide transparent band above the plaza. Here the vertical order
    is fixed and every joint is CLOSED by construction (no overhanging cornice that has nothing
    under it):

        pediment (30..66) -> entablature (66..83, cornice is its top 3 rows) ->
        capitals (84..89) -> column shafts (90..145) -> stylobate (146..161) ->
        cella wall behind the colonnade (84..161) -> terrace apron (142..205)

    The receding return wall's top edge follows the entablature's underside exactly, and the
    apron is the solid bed that keeps sky from appearing under the podium's outer corners.

    The rune stage stays authored at sprite (111, 176): the zone centre (2380, 1930) inverts
    there, so the visual focus and the place the hero actually stands still coincide.
    """
    W, H = 268, 206
    c = Canvas(W, H)
    base, sill = H - 1, H - 3
    cx = W // 2
    stage_x, stage_y = 111, 176

    PL, PR, RR = 30, 206, 236          # porch left / porch right / return right
    pod_top, pod_bot = 146, 161
    cell_top = 84
    RISE = 16                          # how far the return rises over its own depth

    # ---- 1. terrace apron: the solid bed plaza tiers and temple both stand on -----------
    c.poly([(PL, 142), (RR, 142), (RR, base + 1), (PL, base + 1)], LIME_T["foot"])
    c.hline(PL, RR, 142, LIME_T["mortar"])

    # ---- 2. plaza tiers: three stacked ellipses, front drawn last -----------------------
    for tx, ty, rx, ry, top in ((cx, 156, 92, 19, LIME_T["dk"]),
                                (cx, 172, 108, 25, LIME_T["base"]),
                                (cx, 190, 124, 31, LIME_T["lt"])):
        c.ellipse(tx, ty + 3, rx, ry, LIME_T["foot"])
        c.ellipse(tx, ty, rx, ry, top)
        c.ellipse(tx, ty - 2, rx - 2, max(2, ry - 2), LIME_T["hi"] if top == LIME_T["lt"] else LIME_T["lt"])
        for s_i in range(22):
            a0 = math.pi * (0.05 + 0.90 * s_i / 22)
            x0 = tx + math.cos(math.pi - a0) * (rx - 5)
            y0 = ty - 2 + math.sin(a0) * (ry - 4)
            x1 = tx + math.cos(math.pi - a0) * (rx - 22)
            y1 = ty - 2 + math.sin(a0) * (ry - 6) * 0.55
            c.line([(x0, y0), (x1, y1)], LIME_T["mortar"], 1)
        for x in range(tx - rx + 2, tx + rx - 1):
            dy = int((ry - 2) * math.sqrt(max(0.0, 1 - ((x - tx) / max(1, rx)) ** 2)))
            c.blend(x, ty - 2 + dy, LIME_T["hi"], 0.32)
            c.blend(x, ty - 1 + dy, LIME_T["foot"], 0.38)

    # ---- 3. temple body: cella + receding return, painted before anything is carved -----
    c.rect(PL, cell_top, PR, pod_bot, LIME_T["dk"])
    ashlar(c, [(PL, cell_top), (PR, cell_top), (PR, pod_bot), (PL, pod_bot)], LIME_T,
           row_h=7, block_w=14, seed=2801)
    c.poly([(PR, cell_top), (RR, cell_top - RISE), (RR, pod_bot - RISE), (PR, pod_bot)],
           LIME_T["foot"])
    ashlar(c, [(PR, cell_top), (RR, cell_top - RISE), (RR, pod_bot - RISE), (PR, pod_bot)],
           LIME_T, row_h=7, block_w=13, seed=2802)
    c.blend_rect(PR, cell_top - RISE, RR, pod_bot - 6, "#0b1116", 0.26)
    c.vline(PR - 1, cell_top, pod_bot, LIME_T["hi"])
    c.vline(PR, cell_top, pod_bot, LIME_T["mortar"])
    c.vline(RR, cell_top - RISE, pod_bot - RISE, "#0e1418")
    for k in range(7):
        qy = cell_top + 4 + k * 9
        c.rect(PR - 1, qy, PR + 3, min(qy + 6, pod_bot - 1), LIME_T["lt"])
        c.hline(PR - 1, PR + 3, min(qy + 6, pod_bot - 1), LIME_T["foot"])

    # ---- 4. podium: three steps inside the porch span -----------------------------------
    for i in range(3):
        yy = pod_bot - i * 5
        c.poly([(PL + 4 + i * 2, yy), (RR - 4 - i * 2, yy - RISE * 0.75),
                (RR - 4 - i * 2, yy - RISE * 0.75 + 3), (PL + 4 + i * 2, yy + 3)],
               LIME_T["base"] if i % 2 else LIME_T["lt"])
        c.line([(PL + 4 + i * 2, yy), (RR - 4 - i * 2, yy - RISE * 0.75)], LIME_T["hi"])
        c.line([(PL + 4 + i * 2, yy + 3), (RR - 4 - i * 2, yy - RISE * 0.75 + 3)], LIME_T["foot"])

    # ---- 5. central arched portal through the cella ------------------------------------
    pcx = 140
    p_top, p_bot = 100, pod_top
    c.rect(pcx - 22, p_top + 7, pcx + 22, p_bot, LIME_T["dk"])
    c.ellipse(pcx, p_top + 7, 22, 12, LIME_T["dk"])
    c.rect(pcx - 19, p_top + 7, pcx + 19, p_bot - 1, "#070d11")
    c.ellipse(pcx, p_top + 7, 19, 10, "#070d11")
    for y in range(p_top + 10, p_bot):
        t = (y - (p_top + 10)) / float(max(1, p_bot - (p_top + 10)))
        dy = (p_top + 16) - y
        half = 17
        if dy > 0:
            half = int(17 * max(0.0, 1 - (dy / 15.0) ** 2))
        if half < 1:
            continue
        c.rect(pcx - half, y, pcx + half, y,
               mix("#05090d", CYAN["deep"], min(1.0, (t ** 1.8) * 1.3)))
    for y in range(p_bot - 9, p_bot):
        t = (y - (p_bot - 9)) / 9.0
        half = int(5 + 4 * t)
        c.rect(pcx - half, y, pcx + half, y, mix(CYAN["deep"], CYAN["base"], 0.30 + 0.60 * t))
    c.rect(pcx - 4, p_bot - 3, pcx + 4, p_bot - 1, CYAN["lt"])
    for sgn in (-1, 1):
        jx = pcx + sgn * 17
        c.rect(min(jx, jx + sgn * 3), p_top + 11, max(jx, jx + sgn * 3), p_bot - 1, "#4f3b1c")
        c.vline(jx + sgn * 2, p_top + 11, p_bot - 1, "#6f5527")
        for yy in range(p_top + 13, p_bot - 2, 6):
            c.px(jx + sgn, yy, "#8a6b34")
    for a in range(11):
        ang = math.pi + a * math.pi / 10
        vx = pcx + math.cos(ang) * 23
        vy = p_top + 7 + math.sin(ang) * 13
        c.rect(vx - 2, vy - 2, vx + 2, vy + 2, LIME_T["lt"])
        c.hline(vx - 2, vx + 2, vy - 2, LIME_T["hi"])
        c.hline(vx - 2, vx + 2, vy + 2, LIME_T["foot"])

    # ---- 6. porch: corner antae + five fluted columns, shaft -> capital ----------------
    def pier(x0c, w):
        c.rect(x0c, cell_top, x0c + w - 1, pod_top - 1, LIME_T["base"])
        c.rect(x0c, cell_top, x0c + w // 3, pod_top - 1, LIME_T["lt"])
        c.vline(x0c, cell_top, pod_top - 1, LIME_T["hi"])
        c.vline(x0c + w - 2, cell_top, pod_top - 1, LIME_T["dk"])
        c.hline(x0c, x0c + w - 1, 84, LIME_T["hi"])
        c.hline(x0c, x0c + w - 1, 89, LIME_T["dk"])

    pier(PL, 14)
    pier(PR - 13, 14)
    for colx in (PL + 16 + i * ((PR - PL - 32) // 5) for i in range(5)):
        w = 11
        x0c = colx - w // 2
        c.rect(x0c, 90, x0c + w - 1, pod_top - 1, LIME_T["base"])
        c.rect(x0c, 90, x0c + w // 3 - 1, pod_top - 1, LIME_T["lt"])
        c.vline(x0c, 90, pod_top - 1, LIME_T["hi"])
        c.vline(x0c + w - 2, 90, pod_top - 1, LIME_T["dk"])
        for fx in range(x0c + 2, x0c + w - 2, 3):
            c.vline(fx, 92, pod_top - 3, LIME_T["dk"])
        c.rect(x0c - 3, 84, x0c + w + 2, 89, LIME_T["base"])
        c.hline(x0c - 3, x0c + w + 2, 84, LIME_T["hi"])
        c.hline(x0c - 3, x0c + w + 2, 89, LIME_T["foot"])
        c.rect(x0c - 2, pod_top - 4, x0c + w + 1, pod_top - 1, LIME_T["base"])
        c.hline(x0c - 2, x0c + w + 1, pod_top - 4, LIME_T["lt"])
        for yy in range(94, pod_top - 2, 3):
            c.blend(colx + 7 + (yy - 94) // 7, yy, "#080d12", 0.14)

    # ---- 7. entablature: front band 66..83, return band flush with the return wall ------
    c.rect(PL, 66, PR, 83, LIME_T["base"])
    c.poly([(PR, 66), (RR, 66 - RISE), (RR, 84 - RISE), (PR, 84)], LIME_T["dk"])
    c.hline(PL, PR, 66, LIME_T["rim"])                       # cornice lip, inside silhouette
    c.line([(PR, 66), (RR, 66 - RISE)], LIME_T["rim"])
    c.hline(PL, PR, 68, LIME_T["hi"])
    c.hline(PL, PR, 69, LIME_T["dk"])
    c.line([(PR, 69), (RR, 69 - RISE)], LIME_T["dk"])
    for fx in range(PL + 2, PR - 2, 11):                      # triglyph frieze
        c.rect(fx, 73, fx + 5, 79, LIME_T["dk"])
        c.vline(fx + 2, 73, 79, LIME_T["mortar"])
        c.px(fx + 1, 73, LIME_T["hi"])
    c.hline(PL, PR, 72, LIME_T["lt"])
    c.line([(PR + 1, 72), (RR, 72 - RISE)], LIME_T["lt"])
    c.hline(PL, PR, 83, LIME_T["foot"])
    c.line([(PR, 83), (RR, 83 - RISE)], LIME_T["foot"])

    # ---- 8. pediment over the porch + the return's roof plane ---------------------------
    c.poly([(PL, 66), (PR, 66), (cx + 4, 30)], SLATE["base"])
    shingle_plane(c, (PL, 66), (PR, 66), (cx - 40, 46), (cx + 48, 46), SLATE,
                  seed=2803, rows=5, tile=13)
    c.line([(PL, 66), (cx + 4, 30)], SLATE["hi"], 2)
    c.line([(cx + 4, 30), (PR, 66)], SLATE["dk"], 2)
    c.rect(PL, 66, PR, 67, SLATE["dk"])
    c.poly([(cx - 54, 64), (cx + 62, 64), (cx + 4, 40)], LIME_T["foot"])
    c.line([(cx - 54, 64), (cx + 4, 40)], LIME_T["lt"])
    c.line([(cx + 4, 40), (cx + 62, 64)], LIME_T["dk"])
    c.ellipse(cx + 4, 54, 7, 6, LIME_T["base"])
    c.ellipse(cx + 4, 54, 4, 3, CYAN["deep"])
    c.px(cx + 3, 53, CYAN["lt"])
    c.poly([(PR, 66), (RR, 66 - RISE), (cx + 46, 34), (cx + 4, 30)], SLATE["dk"])
    c.line([(cx + 4, 30), (cx + 46, 34)], SLATE["hi"])
    c.line([(RR, 66 - RISE), (cx + 46, 34)], "#141a20")

    # ---- 9. grand stair + flanking braziers, both inside the plaza silhouette ----------
    for s in range(4):
        yy = pod_bot + 6 + s * 5
        w = 30 + s * 5
        c.rect(66 - w // 2, yy, 66 + w // 2, yy + 4, LIME_T["dk"] if s % 2 else LIME_T["base"])
        c.hline(66 - w // 2, 66 + w // 2, yy, LIME_T["lt"])
        c.hline(66 - w // 2, 66 + w // 2, yy + 4, LIME_T["foot"])
    for bx in (44, 186):
        c.rect(bx - 4, base - 30, bx + 4, base - 12, IRON["base"])
        c.rect(bx - 3, base - 29, bx + 3, base - 13, IRON["lt"])
        c.hline(bx - 4, bx + 4, base - 30, IRON["hi"])
        c.ellipse(bx, base - 33, 4, 4, CYAN["base"])
        c.ellipse(bx, base - 34, 2, 3, CYAN["core"])
        c.px(bx, base - 36, "#e8fbff")

    # ---- 10. ritual stage: inlaid rune ring at the interaction point -------------------
    c.ellipse(stage_x, stage_y + 2, 40, 13, LIME_T["dk"])
    c.ellipse(stage_x, stage_y, 38, 11, LIME_T["base"])
    c.ellipse(stage_x, stage_y - 1, 35, 9, LIME_T["lt"])
    c.ring(stage_x, stage_y - 1, 35, 9, LIME_T["mortar"])
    for i in range(2):
        c.ring(stage_x, stage_y - 1, 26 - i, 7 - i, CYAN["base"] if i else CYAN["lt"])
    c.ring(stage_x, stage_y - 1, 16, 4, CYAN["deep"])
    c.ellipse(stage_x, stage_y - 1, 7, 2, CYAN["lt"])
    c.ellipse(stage_x, stage_y - 1, 4, 1, CYAN["core"])
    for m in range(8):
        ang = m * math.tau / 8
        c.px(stage_x + math.cos(ang) * 24, stage_y - 1 + math.sin(ang) * 6, CYAN["lt"])
        c.px(stage_x + math.cos(ang) * 24, stage_y - 2 + math.sin(ang) * 6,
             CYAN["core"] if m % 2 == 0 else CYAN["base"])
    ground_ao(c, PL, RR, sill)
    edge_light(c)
    return c


# --------------------------------------------------------------------------- 7. TOWER GATE
def build_tower_gate() -> Canvas:
    """260x224 — the northern gate. Two battered towers, curtain wall, dark rift aperture.

    Contract: the renderer strokes its animated rift oval at sprite (130, 120) +/- (16, 27), so
    the aperture is authored to that box and painted DARK. The only painted light is the cold
    spill on the threshold below the ring, which the animation then appears to come from.
    """
    W, H = 260, 224
    c = Canvas(W, H)
    base, sill = H - 1, H - 3
    cx = W // 2
    portal_cy, prx, pry = 120, 16, 27

    # ---- stepped basalt dais: one solid mass, so the towers cannot float over a gap ----
    for i, (x0, x1, y0, y1) in enumerate(((20, 240, 200, base), (30, 230, 192, 200),
                                          (42, 218, 186, 192))):
        c.rect(x0, y0, x1, y1, BASA["base"] if i % 2 else BASA["dk"])
        c.hline(x0, x1, y0, BASA["lt"] if i % 2 else BASA["base"])
        c.hline(x0, x1, y1, BASA["mortar"])
        for bx in range(x0 + 7, x1 - 7, 19):
            c.vline(bx, y0 + 1, y1 - 1, BASA["mortar"])
            c.px(bx + 1, y0 + 1, BASA["hi"])

    # ---- curtain wall between the towers, set back and lower, solid from 62 to the dais
    wx0, wx1 = cx - 34, cx + 34
    wall_top, wall_bot = 62, 200
    c.rect(wx0, wall_top, wx1, wall_bot, BASA["foot"])
    ashlar(c, [(wx0, wall_top), (wx1, wall_top), (wx1, wall_bot), (wx0, wall_bot)], BASA,
           row_h=8, block_w=16, seed=2901)
    c.blend_rect(wx0, wall_top, wx1, wall_bot, "#0a0e12", 0.26)
    for x in range(wx0, wx0 + 16):
        c.blend_rect(x, wall_top, x, wall_bot, "#0a0e12", 0.46 * (1 - (x - wx0) / 16.0))
    for x in range(wx1 - 16, wx1 + 1):
        c.blend_rect(x, wall_top, x, wall_bot, "#0a0e12", 0.46 * (1 - (wx1 - x) / 16.0))
    # corbelled parapet with crenels. The wall STAYS lower than the towers: the sky cleft
    # between two proud towers is the gate's silhouette (that gap is design, and the contract
    # test whitelists exactly its x range), while every column of wall that exists is solid.
    c.rect(wx0, 44, wx1, wall_top, GATE["base"])
    c.hline(wx0, wx1, 44, GATE["hi"])
    c.hline(wx0, wx1, 47, GATE["dk"])
    c.hline(wx0, wx1, wall_top - 1, BASA["mortar"])
    c.rect(wx0, wall_top, wx1, wall_top + 3, "#0c1116")
    for i, bx in enumerate(range(wx0 + 1, wx1 - 8, 15)):
        h = 9 if i % 2 == 0 else 5
        c.rect(bx, 44 - h, bx + 8, 44, GATE["lt"] if i % 2 == 0 else GATE["base"])
        c.hline(bx, bx + 8, 44 - h, GATE["rim"] if i % 2 == 0 else GATE["lt"])
        c.vline(bx + 8, 44 - h, 43, "#0c1116")
    # corbel row under the parapet, and the towers' own shadow on the recessed wall
    for bx in range(wx0 + 2, wx1 - 2, 7):
        c.px(bx, wall_top + 4, GATE["dk"])
        c.px(bx + 1, wall_top + 4, BASA["mortar"])

    # ---- twin battered towers, solid from the battlement cap to the dais --------------
    for idx, (tx0, tx1) in enumerate(((26, 98), (162, 234))):
        tow_top = 26
        side_left = tx0 < cx
        # front face with a batter (top narrower)
        taper = 4
        ax0 = tx0 + (taper if side_left else 0)
        ax1 = tx1 - (taper if not side_left else 0)
        c.poly([(ax0, tow_top), (ax1, tow_top), (tx1, wall_bot), (tx0, wall_bot)], GATE["base"])
        ashlar(c, [(ax0, tow_top), (ax1, tow_top), (tx1, wall_bot), (tx0, wall_bot)], GATE,
               row_h=8, block_w=17, seed=2910 + idx)
        # the outer side face recedes away from the key light
        if side_left:
            c.poly([(tx0 - 9, tow_top - 9), (ax0, tow_top), (tx0, wall_bot),
                    (tx0 - 9, wall_bot)], GATE["dk"])
            for i in range(1, 8):
                t = i / 8
                c.line([(tx0 - 9, tow_top - 9 + (wall_bot - tow_top + 9) * t),
                        (tx0, wall_bot - (wall_bot - tow_top) * (1 - t))], GATE["mortar"], 1)
            c.vline(tx0 - 9, tow_top - 9, wall_bot, GATE["lt"])
            c.vline(tx0, tow_top, wall_bot, GATE["mortar"])
        else:
            c.poly([(ax1, tow_top), (tx1 + 9, tow_top - 9), (tx1 + 9, wall_bot),
                    (tx1, wall_bot)], GATE["dk"])
            for i in range(1, 8):
                t = i / 8
                c.line([(ax1, tow_top + (wall_bot - tow_top) * t),
                        (tx1 + 9, tow_top - 9 + (wall_bot - tow_top + 9) * t)], GATE["mortar"], 1)
            c.vline(tx1 + 9, tow_top - 9, wall_bot, "#0e1418")
            c.vline(tx1, tow_top, wall_bot, GATE["mortar"])
        # quoin strip on the inner corner ties the tower to the curtain wall
        qx = tx1 - 5 if side_left else tx0
        quoin_column(c, qx, tow_top + 10, wall_bot - 16, GATE, seed=2920 + idx, width=5,
                     left=side_left)
        # battered plinth so the tower meets the ground with weight
        c.rect(tx0 - 3, wall_bot - 12, tx1 + 3, wall_bot, BASA["dk"])
        c.hline(tx0 - 3, tx1 + 3, wall_bot - 12, GATE["lt"])
        c.hline(tx0 - 3, tx1 + 3, wall_bot, BASA["mortar"])
        for bx in range(tx0 + 3, tx1 - 4, 13):
            c.vline(bx, wall_bot - 11, wall_bot - 1, BASA["mortar"])
        # string course
        c.hline(tx0 + 2, tx1 - 2, 96, GATE["lt"])
        c.hline(tx0 + 2, tx1 - 2, 97, BASA["mortar"])
        # rune-lit loophole
        fx = cx - 86 if side_left else cx + 86
        c.rect(fx - 3, 52, fx + 3, 90, BASA["dk"])
        c.rect(fx - 2, 54, fx + 2, 88, "#090f14")
        c.px(fx, 84, CYAN["base"])
        c.px(fx, 86, CYAN["dim"])
        c.px(fx - 1, 85, CYAN["deep"])
        # carved channel either side
        c.vline(fx - 13, 40, wall_bot - 16, BASA["dk"])
        c.vline(fx - 12, 40, wall_bot - 16, BASA["lt"])
        c.vline(fx + 13, 40, wall_bot - 16, BASA["dk"])
        # battlement cap: moulded course, then alternating merlons with lit top edges
        cap_x0, cap_x1 = tx0 - 4, tx1 + 4
        c.rect(cap_x0, 16, cap_x1, tow_top + 2, GATE["base"])
        c.rect(cap_x0 + 1, 17, cap_x1 - 1, tow_top + 1, GATE["lt"])
        c.hline(cap_x0, cap_x1, 16, GATE["rim"])
        c.hline(cap_x0, cap_x1, tow_top + 2, "#0b0f14")
        for i, bx in enumerate(range(cap_x0 + 2, cap_x1 - 7, 13)):
            h = 10 if i % 2 == 0 else 6
            c.rect(bx, 16 - h, bx + 8, 16, GATE["lt"] if i % 2 == 0 else GATE["base"])
            c.hline(bx, bx + 8, 16 - h, GATE["rim"] if i % 2 == 0 else GATE["lt"])
            c.vline(bx + 8, 16 - h, 15, GATE["dk"])

    # ---- heavy lintel over the passage ------------------------------------------------
    ly0 = 76
    c.rect(cx - 56, ly0 - 6, cx + 56, ly0, GATE["lt"])
    c.hline(cx - 56, cx + 56, ly0 - 6, GATE["rim"])
    c.rect(cx - 56, ly0 + 1, cx + 56, ly0 + 4, "#0a0e13")
    for bx in range(cx - 52, cx + 52, 6):
        c.rect(bx, ly0, bx + 2, ly0 + 1, GATE["lt"])
    for rx in (cx - 30, cx + 30):
        c.rect(rx - 5, ly0 + 7, rx + 5, ly0 + 14, BASA["dk"])
        c.hline(rx - 4, rx + 4, ly0 + 7, BASA["mortar"])
        c.px(rx, ly0 + 9, CYAN["dim"])
        c.hline(rx - 2, rx + 2, ly0 + 11, CYAN["deep"])
        c.px(rx, ly0 + 12, CYAN["dim"])

    # ---- the passage: dark release, lit reveal, cold spill only on the threshold ------
    top, bot = portal_cy - pry, base - 22
    c.rect(cx - prx, top, cx + prx, bot, "#070c11")
    c.ellipse(cx, top, prx, 13, "#070c11")
    for y in range(top + 2, bot):
        t = (y - top) / float(bot - top)
        half = prx - 2
        dy = (top + 12) - y
        if dy > 0:
            half = (prx - 2) * math.sqrt(max(0.0, 1 - (dy / 12.0) ** 2))
        if half < 1:
            continue
        c.rect(int(cx - half), y, int(cx + half), y,
               mix("#05080b", "#0d2230", min(1.0, t * 1.15)))
    # reveal: lit left jamb, dark right jamb
    c.vline(cx - prx + 1, top + 11, bot - 1, BASA["lt"])
    c.vline(cx - prx + 2, top + 14, bot - 2, mix(BASA["base"], CYAN["deep"], 0.5))
    c.vline(cx + prx - 1, top + 11, bot - 2, "#03060a")
    for i, (half, col) in enumerate(((13, "#0b1a22"), (10, "#0a151c"), (6, "#081016"))):
        c.rect(cx - half, top + 16, cx + half, bot - 2, col)
    c.ellipse(cx, top + 18, 12, 8, "#0b1a22")
    c.ellipse(cx, top + 18, 6, 5, "#081016")
    # painted light lives ONLY below the animated ring box (rows 174..186)
    for y in range(bot - 13, bot + 1):
        t = 1.0 - (bot - y) / 13.0
        half = int(3 + 8 * t)
        c.rect(cx - half, y, cx + half, y, mix("#0a1a22", CYAN["deep"], 0.55 + 0.45 * t))
        c.rect(cx - half + 3, y, cx + half - 3, y, mix(CYAN["deep"], CYAN["base"], t ** 1.4))
    c.rect(cx - 4, bot - 2, cx + 4, bot, CYAN["dim"])

    # voussoirs around the arch
    for a in range(13):
        ang = math.pi + a * math.pi / 12
        vx = cx + math.cos(ang) * (prx + 7)
        vy = top + 2 + math.sin(ang) * (pry * 0.62)
        c.rect(vx - 2, vy - 2, vx + 2, vy + 2, BASA["lt"])
        c.hline(vx - 2, vx + 2, vy - 2, BASA["hi"])
        c.hline(vx - 2, vx + 2, vy + 2, BASA["mortar"])
    # keystone flush with the arch intrados
    c.poly([(cx - 4, top - 5), (cx + 4, top - 5), (cx + 4, top + 2), (cx - 4, top + 2)],
           GATE["base"])
    c.line([(cx - 4, top - 5), (cx + 4, top - 5)], GATE["rim"])
    c.line([(cx + 4, top - 5), (cx + 4, top + 2)], BASA["dk"])
    c.line([(cx - 4, top + 2), (cx + 4, top + 2)], BASA["mortar"])

    # ---- threshold stairs on the dais -------------------------------------------------
    for s in range(4):
        yy = bot + 1 + s * 4
        w = 34 + s * 4
        c.rect(cx - w, yy, cx + w, yy + 3, BASA["dk"] if s % 2 else BASA["base"])
        c.hline(cx - w, cx + w, yy, BASA["lt"])
        c.hline(cx - w, cx + w, yy + 3, BASA["mortar"])

    # ---- gate braziers on the dais (silhouette markers at overview zoom) --------------
    for sx in (cx - 96, cx + 96):
        c.rect(sx - 4, base - 30, sx + 4, base - 10, BASA["dk"])
        c.rect(sx - 3, base - 29, sx + 3, base - 11, IRON["base"])
        c.hline(sx - 4, sx + 4, base - 30, IRON["hi"])
        c.rect(sx - 5, base - 34, sx + 5, base - 30, IRON["lt"])
        c.ellipse(sx, base - 36, 3, 3, CYAN["base"])
        c.ellipse(sx, base - 37, 1, 2, CYAN["core"])
        c.px(sx, base - 39, "#dff8ff")

    # weathering: damp moss at the tower feet and hairline cracks on the lit flanks
    for side in (-1, 1):
        mx0 = cx + side * 92 - 6
        for _ in range(5):
            r = rr(2950 + side + _)
            px_ = mx0 + r(13)
            if c.opaque(px_, 188):
                c.px(px_, 188, MOSS["base"])
                c.px(px_ + 1, 188, MOSS["dk"])
                c.px(px_, 189, MOSS["dk"])
    for cxp, cyp in ((cx - 104, 108), (cx + 100, 138), (cx - 62, 150)):
        c.line([(cxp, cyp), (cxp + 3, cyp + 7), (cxp + 1, cyp + 13)], BASA["mortar"], 1)
        c.px(cxp, cyp, BASA["dk"])
    ground_ao(c, 24, W - 24, sill)
    edge_light(c)
    return c


# --------------------------------------------------------------------------- village houses
def village_house(W, H, *, x0, xm, x1, eave, ridge_y, jetty, seed, roof_ramp, upper=PLAS,
                  stone=STONE, overhang=7, rise_r=14, drop_r=16, closed_door=True,
                  flowerbox=True, variant=0) -> Canvas:
    """A background Sanctuary house: same language as the facilities, but no fake entrance.

    These are set dressing (no interior map, no trigger), so the door is painted CLOSED and
    unlit: dark recess, cross-braced plank leaf, no amber spill and no lantern. That is the
    difference between a house and a shop you can walk into.
    """
    c = Canvas(W, H)
    base, sill = H - 1, H - 3
    m = house_mass(c, x0=x0, xm=xm, x1=x1, eave=eave, ridge_y=ridge_y, rise_r=rise_r,
                   drop_r=drop_r, jetty=jetty, base=base, stone=stone, upper=upper,
                   roof_ramp=roof_ramp, seed=seed, overhang=overhang, rows=8,
                   tile=11 + (variant % 3))
    dx = 30 if variant % 2 == 0 else 40
    window(c, x0 + 10, eave + 8, 12, 14, glow=AMBER, lit=1.2 - 0.2 * (variant % 3), pad=2,
           shutters=(variant % 2 == 0))
    window(c, x0 + dx, eave + 11, 12, 14, glow=AMBER, lit=0.0, pad=2, flowerbox=flowerbox)
    window(c, xm - 22, eave + 9, 12, 14, glow=AMBER, lit=1.0, pad=2)
    window(c, xm + 20, eave - rise_r + 12, 11, 13, glow=AMBER, lit=0.9, pad=2)
    window(c, x0 + dx + 6, jetty + 8, 12, 13, glow=AMBER, lit=0.0, pad=2,
           flowerbox=flowerbox)
    timber_frame(c, x0, eave + 2, xm, jetty - 2,
                 posts=(x0 + 4, x0 + 34, x0 + 64, xm - 6), rails=(eave + 24,))
    quoin_column(c, x0 + 1, jetty, base - 2, stone, seed=seed + 1, width=5, left=False)
    quoin_column(c, xm, jetty, base - 2, stone, seed=seed + 2, width=5, left=True)
    plinth(c, x0 - 2, xm + 2, sill, stone, seed=seed + 3)
    # closed door: recess + leaf + iron cross-brace, no light, no lantern
    dcx = (x0 + xm) // 2
    dw, dh = 20, 28
    x0d, x1d = dcx - dw // 2, dcx + dw // 2 - 1
    topd = sill - dh + 1
    c.rect(x0d - 4, topd - 5, x1d + 4, sill + 1, stone["dk"])
    c.rect(x0d - 3, topd - 4, x1d + 3, sill, stone["base"])
    c.hline(x0d - 4, x1d + 4, topd - 5, stone["hi"])
    c.ellipse(dcx, topd, dw // 2 + 2, 5, stone["lt"])
    c.ellipse(dcx, topd + 2, dw // 2 - 1, 4, "#0d1216")
    c.rect(x0d, topd + 3, x1d, sill, "#0d1216")
    c.rect(x0d + 1, topd + 4, x1d - 1, sill - 1, OAK["base"])
    c.vline(x0d + 1, topd + 4, sill - 1, OAK["lt"])
    c.vline(x1d - 1, topd + 4, sill - 1, OAK["dk"])
    for px_ in range(x0d + 3, x1d - 1, 4):
        c.vline(px_, topd + 5, sill - 2, OAK["dk"])
    c.line([(x0d + 2, sill - 3), (x1d - 2, topd + 7)], OAK["dk"], 2)
    c.line([(x0d + 2, topd + 7), (x1d - 2, sill - 3)], OAK["dk"], 1)
    c.px(dcx + 3, sill - dh // 2, IRON["hi"])
    c.hline(x0d - 3, x1d + 3, sill + 1, stone["lt"])
    c.hline(x0d - 3, x1d + 3, sill + 2, stone["foot"])
    # a bench and a barrel so the street feels lived in (inside the mass: no column holes)
    c.rect(x0 + 4, base - 12, x0 + 24, base - 10, OAK["lt"])
    c.rect(x0 + 6, base - 9, x0 + 8, base, OAK["dk"])
    c.rect(x0 + 20, base - 9, x0 + 22, base, OAK["dk"])
    c.rect(xm - 16, base - 20, xm - 6, base, OAK["base"])
    c.vline(xm - 16, base - 20, base, OAK["lt"])
    c.hline(xm - 16, xm - 6, base - 14, IRON["base"])
    c.hline(xm - 16, xm - 6, base - 6, IRON["base"])
    ground_ao(c, x0 - 4, xm + 4, sill)
    edge_light(c)
    return c


def build_townhouse(variant: int) -> Canvas:
    if variant == 0:
        return village_house(152, 156, x0=12, xm=96, x1=142, eave=88, ridge_y=28, jetty=112,
                             seed=3000, roof_ramp=SHING, variant=0)
    return village_house(144, 160, x0=10, xm=92, x1=134, eave=90, ridge_y=30, jetty=116,
                         seed=3100, roof_ramp=SLATE, variant=1)


def build_cottage(slate: bool) -> Canvas:
    return village_house(132, 144, x0=10, xm=84, x1=120, eave=84, ridge_y=26, jetty=106,
                         seed=3200 if slate else 3300,
                         roof_ramp=SLATE if slate else SHING, overhang=7, rise_r=13,
                         drop_r=15, variant=1 if slate else 0)


def cylindrical_tower(W, H, *, shaft_top, shaft_bot, half_w, seed, mat, merlon_h=10,
                      gatehouse=False) -> Canvas:
    """Crenellated ROUND tower: elliptical shaft, curved parapet ring, lit left third.

    The old procedural towers were axis-aligned rectangles with `mortar = #5C5446`, brighter
    than their stone (#4A4E5E), which printed a glowing tan wireframe over a dark shaft - and
    they stopped at `h - 14`, so the base floated. Here the silhouette is curved, the joints are
    darker than the stone, and the plinth reaches the ground row.
    """
    c = Canvas(W, H)
    base = H - 1
    cx = W // 2
    r = rr(seed)
    stone = dict(mat)
    stone["mortar"] = mix(mat["mortar"], "#0d1216", 0.55)
    stone["shade"] = mix(mat["foot"], "#080d11", 0.35)

    top = shaft_top
    # ---- shaft: elliptical, lit from the upper left, jointed in courses ------------------
    for y in range(top, base + 1):
        # keep the shaft a clean ellipse; the plinth widens it at the bottom
        half = half_w
        if y > base - 12:
            half = half_w + (y - (base - 12)) // 5
        for dx in range(-half, half + 1):
            nx = dx / max(1, half)
            col = (stone["hi"] if nx < -0.62 else stone["lt"] if nx < -0.18 else
                   stone["base"] if nx < 0.30 else stone["dk"] if nx < 0.72 else stone["foot"])
            c.px(cx + dx, y, col)
        # course seams follow the curve and wrap slightly, reading as a cylinder
        if (y - top) % 8 == 0:
            for dx in range(-half, half + 1):
                c.px(cx + dx, y, stone["mortar"])
        elif (y - top) % 8 == 1:
            for dx in range(-half, half + 1):
                if dx % 3:
                    c.px(cx + dx, y, mix(stone["base"] if dx < 0 else stone["dk"],
                                         "#e8f0f6", 0.09))
    # vertical joints, staggered per course, only on the visible face
    for band in range(0, base - top, 8):
        off = 0 if (band // 8) % 2 == 0 else 6
        for x in range(-half_w + off, half_w, 13):
            for dy in range(1, 7):
                y = top + band + dy
                if y < base and abs(x) <= half_w:
                    if abs(x) < half_w - 1:
                        c.px(cx + x, y, stone["mortar"])
    # curved silhouette edges: the rim is the brightest/darkest column, not a hard step
    for y in range(top, base + 1):
        half = half_w + (max(0, y - (base - 12)) // 5)
        c.px(cx - half, y, mix(stone["hi"], "#dbe6ee", 0.22))
        c.px(cx + half, y, stone["shade"])

    # ---- battered plinth, on the ground row --------------------------------------------
    plinth_w = half_w + 3
    c.rect(cx - plinth_w, base - 9, cx + plinth_w, base, stone["dk"])
    c.hline(cx - plinth_w, cx + plinth_w, base - 9, stone["lt"])
    c.hline(cx - plinth_w, cx + plinth_w, base, "#0f151a")
    for bx in range(cx - plinth_w + 3, cx + plinth_w - 2, 11):
        c.vline(bx, base - 8, base - 1, stone["mortar"])

    # ---- corbelled parapet ring + merlons ----------------------------------------------
    ring_y = top
    c.rect(cx - half_w, ring_y - 6, cx + half_w, ring_y + 1, stone["lt"])
    c.hline(cx - half_w, cx + half_w, ring_y - 6, stone["rim"])
    c.hline(cx - half_w, cx + half_w, ring_y + 1, stone["mortar"])
    for bx in range(cx - half_w + 1, cx + half_w - 1, 6):
        c.px(bx, ring_y + 2, stone["dk"])
        c.px(bx + 1, ring_y + 2, stone["mortar"])
    # the ring is an ellipse: chop its extreme corners so the top is curved, not a bar
    for dx in range(half_w - 5, half_w + 1):
        c.px(cx - dx, ring_y - 6, (0, 0, 0, 0))
        c.px(cx + dx, ring_y - 6, (0, 0, 0, 0))
    step = 11
    x = cx - half_w + 1
    i = 0
    while x < cx + half_w - 8:
        h = merlon_h if i % 2 == 0 else max(5, merlon_h - 4)
        c.rect(x, ring_y - 6 - h, x + 7, ring_y - 7,
               stone["base"] if i % 2 == 0 else stone["dk"])
        c.hline(x, x + 7, ring_y - 6 - h, stone["rim"] if i % 2 == 0 else stone["lt"])
        c.vline(x + 7, ring_y - 6 - h, ring_y - 8, stone["foot"])
        c.px(x + 1, ring_y - 6 - h, "#c9d4dc")
        x += step
        i += 1

    # ---- arrow slits + (gatehouse only) a dark decorative gate arch --------------------
    def slit(sx, sy, w, h):
        c.rect(sx - w // 2 - 1, sy - 1, sx + w // 2 + 1, sy + h + 1, stone["dk"])
        c.rect(sx - w // 2, sy, sx + w // 2, sy + h, "#12100f")
        c.vline(sx - w // 2, sy, sy + h, "#2e2a28")
        c.vline(sx + w // 2, sy, sy + h, "#0c0b0a")
        c.hline(sx - w // 2, sx + w // 2, sy + h, stone["lt"])

    slit(cx, top + 26, 4, 16)
    if gatehouse:
        slit(cx - 17, top + 34, 3, 12)
        slit(cx + 17, top + 34, 3, 12)
        c.rect(cx - 13, base - 33, cx + 13, base - 11, stone["dk"])
        c.ellipse(cx, base - 33, 13, 8, stone["dk"])
        c.rect(cx - 11, base - 31, cx + 11, base - 11, "#0b0f13")
        c.ellipse(cx, base - 31, 11, 6, "#0b0f13")
        for a in range(9):
            ang = math.pi + a * math.pi / 8
            vx = cx + math.cos(ang) * 14
            vy = base - 33 + math.sin(ang) * 8
            c.rect(vx - 1, vy - 1, vx + 1, vy + 1, stone["lt"])
        c.hline(cx - 13, cx + 13, base - 11, stone["lt"])
        c.hline(cx - 13, cx + 13, base - 10, stone["foot"])

    # ---- weathering: moss at the foot, one crack on the lit flank ----------------------
    for _ in range(6):
        mx = cx - half_w + 2 + r(half_w * 2 - 4)
        y = base - 2 - r(3)
        if c.opaque(mx, y):
            c.px(mx, y, MOSS["base"])
            c.px(mx + 1, y, MOSS["dk"])
            c.px(mx, y + 1, MOSS["dk"])
    cxr, cyr = cx - half_w + 5 + r(6), top + 20
    c.line([(cxr, cyr), (cxr + 2, cyr + 8), (cxr + 1, cyr + 16)], stone["mortar"], 1)
    edge_light(c, strength=0.75)
    return c


# --------------------------------------------------------------------------- registry
BUILDERS = {
    # premium_slice facilities (canvas W,H is the renderer's anchor)
    "summoning_hall": (build_summoning_hall, 268, 206, PREMIUM),
    "tower_gate": (build_tower_gate, 260, 224, PREMIUM),
    "hero_registry": (build_hero_registry, 188, 166, PREMIUM),
    "hero_quarters": (build_hero_quarters, 200, 168, PREMIUM),
    "restaurant": (build_restaurant, 206, 172, PREMIUM),
    "synthesis_lab": (build_synthesis_lab, 188, 168, PREMIUM),
    "armory": (build_armory, 190, 166, PREMIUM),
    # Sanctuary background buildings (sanctuary_p03, same bottom-centred blit)
    "townhouse_a": (lambda: build_townhouse(0), 152, 156, SANCT),
    "townhouse_b": (lambda: build_townhouse(1), 144, 160, SANCT),
    "cottage_brown": (lambda: build_cottage(False), 132, 144, SANCT),
    "cottage_slate": (lambda: build_cottage(True), 132, 144, SANCT),
    # Perimeter towers belong to the WALL family (dark slate-blue), not the temple family: the
    # first engine pass painted them in pale LIME and they read as bleached white towers standing
    # in front of the dark curtain wall. This is the same stone as `tower_gate`, one step up.
    "fortress_tower": (lambda: cylindrical_tower(80, 118, shaft_top=24, shaft_bot=118, half_w=30,
                                                 seed=3400, mat=WALLSTONE, merlon_h=9),
                       80, 118, SANCT),
    "gate_tower": (lambda: cylindrical_tower(96, 136, shaft_top=26, shaft_bot=136, half_w=38,
                                             seed=3500, mat=WALLSTONE, merlon_h=11,
                                             gatehouse=True),
                   96, 136, SANCT),
}

# Sprite-space door contract: (obstacleX, obstacleY, scale) -> expected (spriteX, spriteY)
DOOR_CONTRACT = {
    "hero_quarters": (825.0, 790.0, 0.80, 823.0, 790.0),
    "restaurant": (1105.0, 925.0, 0.80, 1103.0, 925.0),
    "armory": (995.0, 655.0, 0.80, 993.0, 655.0),
}


def expected_door(key):
    ox, oy, sc, dx, dy = DOOR_CONTRACT[key]
    W, H = BUILDERS[key][1], BUILDERS[key][2]
    return W / 2 + (dx - ox) / (2 * sc), (H - 3) - (oy - dy) * 0.72 / (2 * sc)


# Documented design gaps: columns where a transparent band ABOVE a lower mass is the intended
# silhouette (the sky cleft between the Tower Gate's two proud towers). Everything else must be
# hole-free. Keeping this explicit is what stops "the roof floats" from hiding in a tolerance.
GAP_ALLOW_X = {
    "tower_gate": (96, 166),
}


def validate(key: str, c: Canvas) -> dict:
    W, H, _, _ = BUILDERS[key][1], BUILDERS[key][2], None, None
    im = c.im
    assert im.mode == "RGBA", (key, im.mode)
    assert im.size == (W, H), (key, im.size, (W, H))
    rep = alpha_report(im)
    if rep["semi"] != 0:
        raise SystemExit(f"{key}: {rep['semi']} semi-transparent pixels (alpha halo)")
    allowed = GAP_ALLOW_X.get(key)
    bad = [w for w in rep["worst"]
           if not (allowed and allowed[0] <= w[0] <= allowed[1])]
    if bad or (rep["col_gaps"] > len(rep["worst"])):
        raise SystemExit(f"{key}: {rep['col_gaps']} columns contain an interior alpha hole "
                         f"e.g. {bad[:8]}")
    # clean transparent margin so neighbouring ground tiles cannot bleed in
    px = im.load()
    for x in range(W):
        assert px[x, 0][3] == 0, (key, "top row must be transparent", x)
    for y in range(H):
        assert px[0, y][3] == 0 and px[W - 1, y][3] == 0, (key, "side column", y)
    # the base row must carry the footprint: the building sits ON the ground row
    ground = sum(1 for x in range(W) if px[x, H - 1][3] > 0)
    assert ground > W * 0.30, (key, "ground contact too narrow", ground)
    assert rep["bottom"] == H - 1, (key, "base row", rep["bottom"])
    if rep["opaque_pct"] < 28.0:
        raise SystemExit(f"{key}: only {rep['opaque_pct']}% opaque - too empty")
    return rep


def stamp(manifest_path: Path, key: str, png: Path, list_style: bool):
    """Refresh ONE manifest entry (size + sha256) without touching any other key."""
    manifest = json.loads(manifest_path.read_text())
    digest = hashlib.sha256(png.read_bytes()).hexdigest()
    with Image.open(png) as im:
        size = list(im.size)
    if list_style:
        assets = manifest["assets"]
        entry = next((a for a in assets if a.get("id") == key), None)
        if entry is None:
            entry = {"id": key, "path": png.name, "size": size, "ground_anchor": [size[0] // 2, size[1] - 1],
                     "runtime_usage": "P23 Sanctuary background building; bottom-centred drawCrispSprite at 2x scale"}
            assets.append(entry)
        entry["path"] = png.name
        entry["size"] = size
        entry["sha256"] = digest
    else:
        assets = manifest["assets"]
        entry = assets.get(key, {})
        entry["file"] = png.name
        entry["size"] = size
        entry["sha256"] = digest
        assets[key] = entry
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")


def write_contact_sheet(out: Path):
    """Labelled ART CONTACT SHEET — never confused with an engine render."""
    keys = list(BUILDERS)
    pad, label_h = 12, 20
    cells = []
    for key in keys:
        im = Image.open(BUILDERS[key][3] / f"{key}.png").convert("RGBA")
        shots = [("1x", im), ("2x", im.resize((im.width * 2, im.height * 2), Image.NEAREST))]
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
    # pack into rows of 4
    rows = [cells[i:i + 4] for i in range(0, len(cells), 4)]
    W = max(sum(cc.width for cc in r) for r in rows) + pad
    H = sum(max(cc.height for cc in r) for r in rows) + 30 + pad * len(rows)
    sheet = Image.new("RGBA", (W, H), (14, 16, 20, 255))
    d = ImageDraw.Draw(sheet)
    d.text((8, 8), "ART CONTACT SHEET - ETERNAL SUMMONER P23 architecture (offline authored PNG, "
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
    for key, (fn, w, h, folder) in BUILDERS.items():
        c = fn()
        seal_seams(c)
        prune_specks(c)
        rep = validate(key, c)
        report[key] = {"size": rep["size"], "opaque_pct": rep["opaque_pct"],
                       "col_gaps": rep["col_gaps"], "colors": rep["colors"],
                       "bottom": rep["bottom"], "x_range": rep["x_range"],
                       "y_range": rep["y_range"]}
        if not args.check:
            out = folder / f"{key}.png"
            c.im.save(out, optimize=True)
            stamp(folder / "manifest.json", key, out, list_style=(folder == SANCT))
            if args.preview:
                prev = Path(args.preview)
                prev.mkdir(parents=True, exist_ok=True)
                bg = Image.new("RGBA", c.im.size, (26, 32, 30, 255))
                bg.alpha_composite(c.im)
                bg.resize((w * 3, h * 3), Image.NEAREST).save(prev / f"{key}_3x.png")

    doors = {}
    for key in DOOR_CONTRACT:
        px, py = expected_door(key)
        doors[key] = {"expected_sprite_x": round(px, 2), "expected_sprite_y": round(py, 2)}
    if args.preview:
        write_contact_sheet(Path(args.preview) / "P23_ARCH_CONTACT_SHEET.png")

    print(json.dumps({"assets": report, "door_targets": doors,
                      "mode": "check" if args.check else "write"}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
