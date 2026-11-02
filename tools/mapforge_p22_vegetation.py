#!/usr/bin/env python3
"""MAP FORGE P22 — Deterministic 2.5D Pixel Vegetation Generator (v3 Master Botanical Edition).

Authors original, palette-locked 2.5D oblique (Y=0.72) pixel vegetation PNGs for
Eternal Summoner while keeping P18 `oak.png` and P20 `cypress.png` control baselines
byte-for-byte untouched.

Target assets:
  Phase B (Sanctuary vertical slice — species actually present in `buildSanctuaryMap`):
    - app/src/main/assets/sanctuary_p03/canopy_a.png    114x132  bottom=126 (pad=5), split=0.56 (row 74)
    - app/src/main/assets/sanctuary_p03/canopy_b.png     96x114  bottom=109 (pad=4), split=0.56 (row 64)
    - app/src/main/assets/sanctuary_p03/hedge_bush.png   64x40   bottom=36  (pad=3), courtyard hedge

  Phase C (Tower Floor 1 & 2 consistency pass — species absent from Sanctuary, active in Tower):
    - app/src/main/assets/sanctuary_p03/pine.png         96x128  bottom=122 (pad=5), split=0.52 (row 66)
    - app/src/main/assets/sanctuary_p03/twisted.png     112x132  bottom=126 (pad=5), split=0.54 (row 71)
    - app/src/main/assets/sanctuary_p03/forest_bush.png  64x40   bottom=36  (pad=3), woodland shrub
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SANCT = ROOT / "app/src/main/assets/sanctuary_p03"
MANIFEST = SANCT / "manifest.json"


def hex_rgba(h: str, a: int = 255) -> tuple[int, int, int, int]:
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def det_rng(seed: int):
    state = seed & 0xFFFFFFFF

    def nxt(mod: int = 0x10000) -> int:
        nonlocal state
        state = (1664525 * state + 1013904223) & 0xFFFFFFFF
        return (state >> 11) % max(1, mod)

    return nxt


# Night lawn ground-moss ramp harmonized directly with P18 `grass_0.png` & P20 `cypress.png`.
MOSS = {
    "occ": hex_rgba("#121e18"),   # lum 25
    "dk": hex_rgba("#1d2f24"),    # lum 39
    "mid": hex_rgba("#2e4734"),   # lum 59
    "lt": hex_rgba("#44623f"),    # lum 85
}

# Mature Sanctuary Sakura Blossom A — muted moonlit dusk-rose + deep cherry bark.
SAKURA_A_PAL = {
    "occ": hex_rgba("#191321"),       # lum 21 — deep interior branch cavity
    "shd_dk": hex_rgba("#2b1d32"),    # lum 33 — lower-right underside shadow
    "shd": hex_rgba("#442b44"),       # lum 51 — plum-rose shadow mass
    "mid": hex_rgba("#643f57"),       # lum 74 — core muted dusk-rose blossom
    "lit": hex_rgba("#86586f"),       # lum 99 — upper-left lit blossom spray
    "hi": hex_rgba("#a8778c"),        # lum 130 — moonlit blossom crest
    "rim": hex_rgba("#c397a8"),       # lum 161 — sparse silver-rose moonlight tip
    "leaf_dk": hex_rgba("#20302b"),   # lum 41 — dark emerald calyx leaf tuck
    "leaf_lt": hex_rgba("#32493f"),   # lum 63 — muted cherry leaf accent
    "bark_occ": hex_rgba("#181116"),  # lum 19 — canopy cast shadow on upper trunk
    "bark_dk": hex_rgba("#281b21"),   # lum 30 — right-face shadow bark
    "bark_mid": hex_rgba("#3e2a31"),  # lum 47 — core dark cherry bark
    "bark_lt": hex_rgba("#583d43"),   # lum 67 — upper-left lit bark plane
    "bark_hi": hex_rgba("#755459"),   # lum 90 — horizontal cherry lenticel dash
}

# Younger Wind-Leaning Sanctuary Sakura B — slightly cooler moon-mauve rose shift.
SAKURA_B_PAL = {
    "occ": hex_rgba("#171323"),
    "shd_dk": hex_rgba("#281c33"),
    "shd": hex_rgba("#402a45"),
    "mid": hex_rgba("#5f3d58"),
    "lit": hex_rgba("#81556f"),
    "hi": hex_rgba("#a3748c"),
    "rim": hex_rgba("#bf94a9"),
    "leaf_dk": hex_rgba("#1f2f2c"),
    "leaf_lt": hex_rgba("#304740"),
    "bark_occ": hex_rgba("#171117"),
    "bark_dk": hex_rgba("#261b23"),
    "bark_mid": hex_rgba("#3b2932"),
    "bark_lt": hex_rgba("#543c45"),
    "bark_hi": hex_rgba("#70525a"),
}

# Spirit Conifer Pine (Tower Floor 1 & 2) — cool blue-spruce needle sprays + red-umber conifer bark.
PINE_PAL = {
    "occ": hex_rgba("#0d181a"),       # lum 20
    "shd_dk": hex_rgba("#152629"),    # lum 33
    "shd": hex_rgba("#21393b"),       # lum 50
    "mid": hex_rgba("#2f504f"),       # lum 70
    "lit": hex_rgba("#426963"),       # lum 93
    "hi": hex_rgba("#59867b"),        # lum 119
    "rim": hex_rgba("#74a395"),       # lum 146 — sparse spirit-frost needle tip
    "bark_occ": hex_rgba("#17100f"),
    "bark_dk": hex_rgba("#281b19"),
    "bark_mid": hex_rgba("#3d2a24"),
    "bark_lt": hex_rgba("#563b32"),
    "bark_hi": hex_rgba("#704f42"),
}

# Mystic Plum Tree (Tower Floor 2 Elderwood) — gnarled violet-ash heartwood + twilight plum canopy.
PLUM_PAL = {
    "occ": hex_rgba("#141122"),       # lum 19
    "shd_dk": hex_rgba("#231b36"),    # lum 31
    "shd": hex_rgba("#36294d"),       # lum 47
    "mid": hex_rgba("#4e3b68"),       # lum 67
    "lit": hex_rgba("#6a5186"),       # lum 91
    "hi": hex_rgba("#8b70a8"),        # lum 120
    "rim": hex_rgba("#b097ca"),       # lum 158 — pale mystic plum-blossom bud
    "leaf_dk": hex_rgba("#1b3031"),   # lum 41
    "leaf_lt": hex_rgba("#2b4a49"),   # lum 64
    "bark_occ": hex_rgba("#151219"),
    "bark_dk": hex_rgba("#251f2a"),
    "bark_mid": hex_rgba("#39303f"),
    "bark_lt": hex_rgba("#514557"),
    "bark_hi": hex_rgba("#6c5d72"),
}

# Sanctuary Courtyard Trimmed Hedge Bush — sculpted 3/4 boxwood top plane + vertical shadow wall.
HEDGE_PAL = {
    "occ": hex_rgba("#0f1a15"),       # lum 22
    "shd_dk": hex_rgba("#172820"),    # lum 34
    "shd": hex_rgba("#22392c"),       # lum 49
    "mid": hex_rgba("#304d39"),       # lum 66
    "lit": hex_rgba("#426445"),       # lum 87
    "hi": hex_rgba("#567b53"),        # lum 108
    "rim": hex_rgba("#6c9163"),       # lum 128
    "stem_dk": hex_rgba("#261c16"),
    "stem_lt": hex_rgba("#3c2d23"),
    "petal_mid": hex_rgba("#86586f"),
    "petal_hi": hex_rgba("#b58599"),
}

# Tower Woodland Forest Canopy Bush — ragged untamed fern/bramble understory shrub.
FOREST_BUSH_PAL = {
    "occ": hex_rgba("#0f1c16"),
    "shd_dk": hex_rgba("#172b21"),
    "shd": hex_rgba("#233e2e"),
    "mid": hex_rgba("#33563d"),
    "lit": hex_rgba("#476f4b"),
    "hi": hex_rgba("#5e895a"),
    "rim": hex_rgba("#76a16b"),
    "stem_dk": hex_rgba("#281e17"),
    "stem_lt": hex_rgba("#413124"),
    "berry": hex_rgba("#5c7394"),
}


class PixelCanvas:
    def __init__(self, w: int, h: int):
        self.w = w
        self.h = h
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px_access = self.im.load()

    def px(self, x: float, y: float, c: tuple[int, int, int, int]) -> None:
        ix, iy = int(round(x)), int(round(y))
        if 0 <= ix < self.w and 0 <= iy < self.h:
            self.px_access[ix, iy] = c

    def get(self, x: int, y: int) -> tuple[int, int, int, int]:
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px_access[x, y]
        return (0, 0, 0, 0)

    def clear_px(self, x: int, y: int) -> None:
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px_access[x, y] = (0, 0, 0, 0)

    def poly(self, pts: list[tuple[float, float]], c: tuple[int, int, int, int]) -> None:
        if len(pts) < 3:
            return
        mask = Image.new("1", (self.w, self.h), 0)
        ImageDraw.Draw(mask).polygon([(int(round(x)), int(round(y))) for x, y in pts], fill=1)
        mpx = mask.load()
        for y in range(self.h):
            for x in range(self.w):
                if mpx[x, y]:
                    self.px_access[x, y] = c

    def line(self, pts: list[tuple[float, float]], c: tuple[int, int, int, int], width: int = 1) -> None:
        mask = Image.new("1", (self.w, self.h), 0)
        ImageDraw.Draw(mask).line([(int(round(x)), int(round(y))) for x, y in pts], fill=1, width=width)
        mpx = mask.load()
        for y in range(self.h):
            for x in range(self.w):
                if mpx[x, y]:
                    self.px_access[x, y] = c

    def faceted_spray(
        self,
        cx: float,
        cy: float,
        rx: float,
        ry: float,
        col: tuple[int, int, int, int],
        seed: int,
        vertices: int = 10,
        jag: float = 0.22,
    ) -> None:
        """Rasterize an angular/faceted pixel-art leaf/petal cluster polygon (never a smooth circle)."""
        r = det_rng(seed)
        pts: list[tuple[float, float]] = []
        for i in range(vertices):
            ang = -math.pi + (2.0 * math.pi * i) / vertices
            notch = (1.0 + jag * 0.65) if (i % 2 == 0) else (1.0 - jag * 0.75)
            jitter = 0.92 + (r(17) / 100.0)
            rad_scale = notch * jitter
            sy = ry * (0.86 if math.sin(ang) > 0.25 else 1.0)
            pts.append((cx + math.cos(ang) * rx * rad_scale, cy + math.sin(ang) * sy * rad_scale))
        self.poly(pts, col)

    def paint_trunk_and_collar(
        self,
        collar_poly: list[tuple[float, float]],
        limbs: list[list[tuple[float, float, float]]],
        pal: dict[str, tuple[int, int, int, int]],
        shadow_under_y: int,
        root_ridges: list[list[tuple[float, float]]],
    ) -> None:
        """Paint a solid, weighty wooden trunk base + 3D cylindrical limbs + sculpted root ridges."""
        self.poly(collar_poly, pal["bark_dk"])
        xs = [p[0] for p in collar_poly]
        mid_x = sum(xs) / len(xs)
        ys = [int(round(p[1])) for p in collar_poly]
        for y in range(max(0, min(ys)), min(self.h, max(ys) + 1)):
            row_xs = [x for x in range(self.w) if self.px_access[x, y][3] == 255]
            if not row_xs:
                continue
            x0, x1 = min(row_xs), max(row_xs)
            span = max(1, x1 - x0)
            for x in row_xs:
                frac = (x - x0) / span
                if y <= shadow_under_y:
                    self.px_access[x, y] = pal["bark_occ"] if frac > 0.45 else pal["bark_dk"]
                elif frac < 0.28:
                    self.px_access[x, y] = pal["bark_lt"]
                elif frac < 0.58:
                    self.px_access[x, y] = pal["bark_mid"]
                elif frac < 0.84:
                    self.px_access[x, y] = pal["bark_dk"]
                else:
                    self.px_access[x, y] = pal["bark_occ"]

        for pts in limbs:
            for i in range(len(pts) - 1):
                x0, y0, r0 = pts[i]
                x1, y1, r1 = pts[i + 1]
                steps = max(4, int(math.hypot(x1 - x0, y1 - y0) * 3))
                for s in range(steps + 1):
                    t = s / steps
                    cx = x0 + (x1 - x0) * t
                    cy = y0 + (y1 - y0) * t
                    rad = r0 + (r1 - r0) * t
                    ir = int(math.ceil(rad + 1))
                    for dy in range(-ir, ir + 1):
                        for dx in range(-ir, ir + 1):
                            dist = math.hypot(dx, dy)
                            if dist <= rad:
                                px_x, px_y = int(round(cx + dx)), int(round(cy + dy))
                                if 0 <= px_x < self.w and 0 <= px_y < self.h:
                                    if px_y <= shadow_under_y:
                                        col = pal["bark_occ"] if dx >= 0 else pal["bark_dk"]
                                    else:
                                        ul = (dx * -0.75 + dy * -0.66) / max(0.9, rad)
                                        if dist >= rad - 0.8 and dx > 0.3:
                                            col = pal["bark_occ"]
                                        elif ul > 0.30:
                                            col = pal["bark_lt"]
                                        elif ul > -0.22:
                                            col = pal["bark_mid"]
                                        elif ul > -0.65:
                                            col = pal["bark_dk"]
                                        else:
                                            col = pal["bark_occ"]
                                    self.px_access[px_x, px_y] = col

        for ridge in root_ridges:
            is_left = ridge[-1][0] <= mid_x + 2
            self.line(ridge, pal["bark_lt"] if is_left else pal["bark_mid"], 1)
            self.line([(x + 1, y) for x, y in ridge], pal["bark_mid"] if is_left else pal["bark_dk"], 1)
            self.line([(x + 2, y) for x, y in ridge], pal["bark_occ"], 1)

    def paint_unified_canopy(
        self,
        lobes: list[tuple[float, float, float, float, int]],
        pal: dict[str, tuple[int, int, int, int]],
        tree_cx: float,
        tree_cy: float,
        tree_rx: float,
        tree_ry: float,
        sky_gaps: list[tuple[float, float, float, float]],
        calyx_spots: list[tuple[float, float]],
        internal_creases: list[list[tuple[float, float]]] | None = None,
    ) -> None:
        """Build a cohesive, non-pillow-shaded deciduous/blossom crown from shingled faceted clusters."""
        for cx, cy, rx, ry, sd in lobes:
            self.faceted_spray(cx + 1.5, cy + 2.0, rx * 0.95, ry * 0.93, pal["occ"], sd + 1, vertices=12, jag=0.18)
            gx = (cx - tree_cx) / max(1.0, tree_rx)
            gy = (cy - tree_cy) / max(1.0, tree_ry)
            base_col = pal["shd"] if (gx * 0.65 + gy * 0.75) < 0.12 else pal["shd_dk"]
            self.faceted_spray(cx, cy, rx, ry, base_col, sd + 2, vertices=12, jag=0.20)

        if "leaf_dk" in pal and "leaf_lt" in pal:
            for lx, ly in calyx_spots:
                self.faceted_spray(lx, ly, 5.2, 3.6, pal["leaf_dk"], int(lx * 31 + ly * 17), vertices=8, jag=0.22)
                self.faceted_spray(lx - 1.2, ly - 0.8, 3.2, 2.2, pal["leaf_lt"], int(lx * 19 + ly * 23), vertices=8, jag=0.18)

        packets: list[tuple[float, float, float, float, int]] = []
        for cx, cy, rx, ry, sd in lobes:
            r = det_rng(sd + 100)
            offsets = [
                ( 0.42,  0.38, 0.46),
                (-0.32,  0.40, 0.45),
                ( 0.44, -0.18, 0.46),
                ( 0.02,  0.12, 0.50),
                (-0.42, -0.12, 0.48),
                ( 0.08, -0.38, 0.46),
                (-0.34, -0.42, 0.45),
            ]
            for k, (ox, oy, sc) in enumerate(offsets):
                jx = (r(20) - 10) * 0.012 * rx
                jy = (r(20) - 10) * 0.012 * ry
                packets.append((cx + ox * rx + jx, cy + oy * ry + jy, max(4.2, rx * sc), max(3.3, ry * sc), sd + 200 + k))

        packets.sort(key=lambda item: -(item[0] * 0.62 + item[1] * 0.78))

        for px_c, py_c, prx, pry, psd in packets:
            gx = (px_c - tree_cx) / max(1.0, tree_rx)
            gy = (py_c - tree_cy) / max(1.0, tree_ry)
            global_ul = -(gx * 0.64 + gy * 0.77)

            if global_ul < 0.35:
                self.faceted_spray(px_c + 1.1, py_c + 1.4, prx * 0.90, pry * 0.86, pal["shd_dk"] if global_ul > -0.25 else pal["occ"], psd + 1, vertices=10, jag=0.20)

            if global_ul > 0.42:
                self.faceted_spray(px_c, py_c, prx, pry, pal["mid"], psd + 2, vertices=10, jag=0.22)
                self.faceted_spray(px_c - 0.9, py_c - 0.9, prx * 0.80, pry * 0.78, pal["lit"], psd + 3, vertices=10, jag=0.20)
                self.faceted_spray(px_c - 1.8, py_c - 1.7, prx * 0.50, pry * 0.48, pal["hi"], psd + 4, vertices=8, jag=0.18)
                if global_ul > 0.64:
                    tx, ty = int(round(px_c - prx * 0.42)), int(round(py_c - pry * 0.54))
                    for dx in range(2):
                        if self.get(tx + dx, ty)[3] == 255:
                            self.px(tx + dx, ty, pal["rim"])
            elif global_ul > 0.04:
                self.faceted_spray(px_c, py_c, prx, pry, pal["shd"], psd + 2, vertices=10, jag=0.22)
                self.faceted_spray(px_c - 0.8, py_c - 0.8, prx * 0.82, pry * 0.80, pal["mid"], psd + 3, vertices=10, jag=0.20)
                self.faceted_spray(px_c - 1.7, py_c - 1.5, prx * 0.50, pry * 0.48, pal["lit"], psd + 4, vertices=8, jag=0.18)
            elif global_ul > -0.36:
                self.faceted_spray(px_c, py_c, prx, pry, pal["shd_dk"], psd + 2, vertices=10, jag=0.22)
                self.faceted_spray(px_c - 0.8, py_c - 0.8, prx * 0.78, pry * 0.76, pal["shd"], psd + 3, vertices=10, jag=0.20)
                self.faceted_spray(px_c - 1.6, py_c - 1.4, prx * 0.42, pry * 0.40, pal["mid"], psd + 4, vertices=8, jag=0.18)
            else:
                self.faceted_spray(px_c, py_c, prx, pry, pal["occ"], psd + 2, vertices=10, jag=0.22)
                self.faceted_spray(px_c - 0.8, py_c - 0.8, prx * 0.74, pry * 0.72, pal["shd_dk"], psd + 3, vertices=10, jag=0.20)
                self.faceted_spray(px_c - 1.5, py_c - 1.2, prx * 0.38, pry * 0.36, pal["shd"], psd + 4, vertices=8, jag=0.18)

        # Staggered scalloped sub-crown shadow pockets (broken pixel clusters, NEVER a continuous drawn line!)
        if internal_creases:
            for idx_c, crease in enumerate(internal_creases):
                for pt_i, (cx_p, cy_p) in enumerate(crease):
                    self.faceted_spray(cx_p + 0.8, cy_p + 1.4, 5.2, 3.2, pal["shd_dk"], 900 + idx_c * 17 + pt_i, vertices=8, jag=0.26)
                    self.faceted_spray(cx_p, cy_p - 0.8, 5.6, 3.6, pal["mid"], 950 + idx_c * 17 + pt_i, vertices=8, jag=0.24)
                    self.faceted_spray(cx_p - 1.2, cy_p - 1.8, 3.8, 2.5, pal["lit"], 980 + idx_c * 17 + pt_i, vertices=8, jag=0.20)

        # Carve genuine negative-space sky apertures around major branch forks
        for gx_c, gy_c, grx, gry in sky_gaps:
            x0, x1 = max(0, int(gx_c - grx - 2)), min(self.w - 1, int(gx_c + grx + 2))
            y0, y1 = max(0, int(gy_c - gry - 2)), min(self.h - 1, int(gy_c + gry + 2))
            for y in range(y0, y1 + 1):
                dy = (y - gy_c) / max(0.8, gry)
                for x in range(x0, x1 + 1):
                    dx = (x - gx_c) / max(0.8, grx)
                    d2 = dx * dx + dy * dy
                    if d2 <= 1.0:
                        self.clear_px(x, y)
                    elif d2 <= 1.55 and self.get(x, y)[3] == 255 and dy < 0.25:
                        self.px(x, y, pal["occ"])

    def deorphan_clusters(self, passes: int = 1) -> None:
        for _ in range(passes):
            updates: list[tuple[int, int, tuple[int, int, int, int]]] = []
            for y in range(1, self.h - 1):
                for x in range(1, self.w - 1):
                    cur = self.px_access[x, y]
                    nbrs = [
                        self.px_access[x - 1, y],
                        self.px_access[x + 1, y],
                        self.px_access[x, y - 1],
                        self.px_access[x, y + 1],
                    ]
                    opaque_nbrs = [n for n in nbrs if n[3] == 255]
                    if cur[3] == 255 and len(opaque_nbrs) == 0:
                        updates.append((x, y, (0, 0, 0, 0)))
                    elif cur[3] == 255 and len(opaque_nbrs) == 4 and all(n != cur for n in opaque_nbrs):
                        best = max(set(opaque_nbrs), key=opaque_nbrs.count)
                        updates.append((x, y, best))
            for x, y, col in updates:
                self.px_access[x, y] = col

    def enforce_margins_and_bottom(
        self,
        target_bottom: int,
        anchor_x_range: tuple[int, int],
        foot_col: tuple[int, int, int, int],
        side_margin: int = 3,
    ) -> None:
        """Guarantee exact bottom row and clean side/top margins so silhouettes never clip canvas borders."""
        for y in range(target_bottom + 1, self.h):
            for x in range(self.w):
                self.px_access[x, y] = (0, 0, 0, 0)
        for y in range(self.h):
            for x in range(side_margin):
                self.px_access[x, y] = (0, 0, 0, 0)
                self.px_access[self.w - 1 - x, y] = (0, 0, 0, 0)
        for x in range(self.w):
            self.px_access[x, 0] = (0, 0, 0, 0)
            self.px_access[x, 1] = (0, 0, 0, 0)
        if not any(self.px_access[x, target_bottom][3] >= 128 for x in range(self.w)):
            for x in range(anchor_x_range[0], anchor_x_range[1] + 1):
                self.px_access[x, target_bottom] = foot_col


def add_root_moss_and_petals(
    c: PixelCanvas,
    bottom_y: int,
    moss_spans: list[tuple[int, int]],
    petal_pts: list[tuple[int, int]],
    petal_cols: tuple[tuple[int, int, int, int], tuple[int, int, int, int]] | None = None,
) -> None:
    for x0, x1 in moss_spans:
        for x in range(x0, x1 + 1):
            c.px(x, bottom_y, MOSS["occ"])
            if x0 + 1 <= x <= x1 - 1:
                c.px(x, bottom_y - 1, MOSS["dk"])
        mid = (x0 + x1) // 2
        c.px(mid - 1, bottom_y - 1, MOSS["mid"])
        c.px(mid, bottom_y - 2, MOSS["lt"])
        c.px(mid - 1, bottom_y - 2, MOSS["mid"])
    if petal_cols is not None:
        c_dk, c_lt = petal_cols
        for px_x, px_y in petal_pts:
            c.px(px_x, px_y, c_dk)
            c.px(px_x + 1, px_y, c_lt)


# ==============================================================================
# 1. SANCTUARY MATURE SAKURA BLOSSOM A (`canopy_a.png`, 114x132, bottom=126)
# ==============================================================================
def generate_canopy_a() -> Image.Image:
    """114x132 Mature Sanctuary Sakura Blossom A (`bottom=126`, `topFraction=0.56` -> row 74)."""
    c = PixelCanvas(114, 132)
    p = SAKURA_A_PAL

    collar = [
        (39, 126), (44, 121), (48, 112), (49, 94), (48, 76),
        (61, 76), (60, 95), (62, 112), (67, 121), (73, 126),
    ]
    limbs = [
        [(51, 82, 4.2), (40, 69, 3.2), (28, 57, 2.4)],
        [(55, 78, 3.5), (57, 63, 2.6), (60, 50, 1.9)],
        [(58, 83, 4.0), (71, 71, 3.0), (85, 61, 2.2)],
    ]
    ridges = [
        [(49, 104), (47, 115), (42, 124), (39, 126)],
        [(53, 108), (52, 119), (49, 125)],
        [(58, 106), (61, 118), (66, 124), (70, 126)],
    ]
    c.paint_trunk_and_collar(collar, limbs, p, shadow_under_y=79, root_ridges=ridges)

    for lx, ly in [(50, 88), (49, 95), (51, 101), (50, 108), (48, 116), (53, 98), (45, 122)]:
        c.px(lx, ly, p["bark_hi"])
        c.px(lx + 1, ly, p["bark_hi"])

    lobes = [
        (55, 22, 25, 15, 1101),
        (34, 35, 22, 15, 1102),
        (77, 37, 22, 15, 1103),
        (25, 51, 18, 13, 1104),
        (54, 45, 21, 14, 1105),
        (85, 53, 18, 13, 1106),
        (38, 59, 13,  9, 1107),
        (73, 60, 14,  9, 1108),
    ]
    creases = [
        [(31, 39), (42, 42), (53, 40), (64, 36)],   # Upper dome overhang onto mid blossom boughs
        [(58, 50), (70, 53), (83, 50)],             # Mid crown overhang onto sweeping east cherry bough
    ]
    c.paint_unified_canopy(
        lobes=lobes,
        pal=p,
        tree_cx=55,
        tree_cy=41,
        tree_rx=43,
        tree_ry=27,
        sky_gaps=[(46, 61, 3.8, 2.8), (65, 60, 4.0, 2.8)],
        calyx_spots=[(29, 62), (62, 57), (81, 64)],
        internal_creases=creases,
    )

    c.line([(49, 75), (42, 66), (35, 59)], p["bark_dk"], 2)
    c.line([(48, 75), (41, 66)], p["bark_lt"], 1)
    c.line([(59, 76), (68, 67), (76, 61)], p["bark_occ"], 2)

    add_root_moss_and_petals(
        c,
        bottom_y=126,
        moss_spans=[(39, 46), (51, 58), (64, 72)],
        petal_pts=[(36, 125), (59, 125), (74, 125)],
        petal_cols=(p["mid"], p["hi"]),
    )
    c.deorphan_clusters(passes=1)
    c.enforce_margins_and_bottom(126, (40, 72), MOSS["occ"], side_margin=4)
    return c.im


# ==============================================================================
# 2. SANCTUARY WIND-LEANING SAKURA B (`canopy_b.png`, 96x114, bottom=109)
# ==============================================================================
def generate_canopy_b() -> Image.Image:
    """96x114 Wind-Leaning Sanctuary Sakura B (`bottom=109`, `topFraction=0.56` -> row 64)."""
    c = PixelCanvas(96, 114)
    p = SAKURA_B_PAL

    collar = [
        (36, 109), (41, 104), (43, 95), (42, 81), (44, 66),
        (53, 66), (51, 81), (53, 95), (57, 104), (62, 109),
    ]
    limbs = [
        [(44, 71, 3.1), (33, 60, 2.3), (23, 50, 1.7)],
        [(49, 67, 3.3), (57, 53, 2.4), (65, 38, 1.8)],
        [(46, 65, 2.5), (41, 49, 1.8)],
    ]
    ridges = [
        [(43, 91), (41, 101), (37, 108)],
        [(47, 94), (47, 104), (46, 109)],
        [(51, 92), (55, 102), (60, 108)],
    ]
    c.paint_trunk_and_collar(collar, limbs, p, shadow_under_y=67, root_ridges=ridges)

    for lx, ly in [(43, 76), (42, 83), (44, 90), (45, 97), (41, 104)]:
        c.px(lx, ly, p["bark_hi"])
        c.px(lx + 1, ly, p["bark_hi"])

    lobes = [
        (57, 20, 20, 13, 1201),
        (36, 29, 18, 12, 1202),
        (49, 39, 18, 12, 1203),
        (24, 46, 15, 10, 1204),
        (70, 43, 17, 11, 1205),
        (35, 51, 11,  8, 1206),
        (64, 51, 12,  8, 1207),
    ]
    creases = [
        [(39, 34), (50, 36), (62, 32)],
    ]
    c.paint_unified_canopy(
        lobes=lobes,
        pal=p,
        tree_cx=49,
        tree_cy=36,
        tree_rx=35,
        tree_ry=22,
        sky_gaps=[(54, 52, 3.4, 2.4)],
        calyx_spots=[(26, 53), (68, 51)],
        internal_creases=creases,
    )
    c.line([(49, 64), (55, 54), (60, 47)], p["bark_dk"], 2)

    add_root_moss_and_petals(
        c,
        bottom_y=109,
        moss_spans=[(36, 42), (46, 52), (56, 62)],
        petal_pts=[(33, 108), (53, 108)],
        petal_cols=(p["mid"], p["hi"]),
    )
    c.deorphan_clusters(passes=1)
    c.enforce_margins_and_bottom(109, (37, 61), MOSS["occ"], side_margin=4)
    return c.im


# ==============================================================================
# 3. SANCTUARY COURTYARD HEDGE BUSH (`hedge_bush.png`, 64x40, bottom=36)
# ==============================================================================
def generate_hedge_bush() -> Image.Image:
    """64x40 Sculpted 2.5D Courtyard Boxwood Shrub (`bottom=36`, natural scalloped overhang)."""
    c = PixelCanvas(64, 40)
    p = HEDGE_PAL

    # 1) Dense, flush-grounded base shadow skirt sitting squarely on y=36 (`x=[5..58]`)
    c.poly([(7, 36), (5, 31), (8, 21), (55, 21), (58, 31), (56, 36)], p["occ"])
    for sx in (19, 33, 45):
        c.px(sx, 34, p["stem_dk"])
        c.px(sx - 1, 33, p["stem_lt"])
        c.px(sx, 35, p["occ"])

    # 2) Sculpted vertical front/south hedge face (y=19..35)
    for lx, ly, rx, ry, sd in [
        (13, 27, 9.0, 7.5, 1301),
        (25, 28, 10.0, 7.5, 1302),
        (38, 28, 10.0, 7.5, 1303),
        (50, 27, 9.0, 7.5, 1304),
    ]:
        c.faceted_spray(lx + 0.8, ly + 0.8, rx, ry, p["occ"], sd, vertices=10, jag=0.16)
        c.faceted_spray(lx, ly - 0.5, rx * 0.92, ry * 0.88, p["shd_dk"], sd + 1, vertices=10, jag=0.16)
        c.faceted_spray(lx - 1.0, ly - 1.8, rx * 0.74, ry * 0.70, p["shd"], sd + 2, vertices=8, jag=0.15)

    # 3) Crisp 3/4 oblique moonlit top leaf table (`y=7..24`) with scalloped front lip overlapping front wall!
    top_clusters = [
        (49, 18, 9.0, 6.6, 1311, False),
        (38, 16, 10.6, 7.2, 1312, True),
        (25, 16, 10.8, 7.2, 1313, True),
        (14, 18, 9.2, 6.6, 1314, True),
        (31, 13, 11.8, 6.2, 1315, True),
    ]
    for lx, ly, rx, ry, sd, bright in top_clusters:
        # Scalloped shadow lip directly below front edge (not a straight horizontal bar!)
        c.faceted_spray(lx + 0.6, ly + 1.8, rx * 0.92, ry * 0.88, p["shd_dk"], sd + 9, vertices=10, jag=0.22)
        c.faceted_spray(lx + 0.4, ly + 0.8, rx * 0.94, ry * 0.90, p["shd"], sd, vertices=10, jag=0.20)
        c.faceted_spray(lx, ly, rx, ry, p["mid"], sd + 1, vertices=10, jag=0.18)
        c.faceted_spray(lx - 1.2, ly - 1.0, rx * 0.74, ry * 0.72, p["lit"], sd + 2, vertices=8, jag=0.16)
        if bright:
            c.faceted_spray(lx - 2.2, ly - 1.8, rx * 0.44, ry * 0.44, p["hi"], sd + 3, vertices=8, jag=0.14)
            tx, ty = int(round(lx - rx * 0.36)), int(round(ly - ry * 0.48))
            if c.get(tx, ty)[3] == 255:
                c.px(tx, ty, p["rim"])

    c.deorphan_clusters(passes=1)

    # Crisp 2px fallen Sanctuary dusk-rose sakura petal clusters on upper hedge table (placed after deorphan!)
    for bx, by in [(18, 14), (32, 12), (43, 16)]:
        c.px(bx, by, p["petal_hi"])
        c.px(bx + 1, by, p["petal_hi"])
        c.px(bx + 1, by + 1, p["petal_mid"])
        c.px(bx + 2, by + 1, p["petal_mid"])

    c.enforce_margins_and_bottom(36, (8, 55), p["occ"], side_margin=4)
    return c.im


# ==============================================================================
# 4. TOWER SPIRIT CONIFER PINE (`pine.png`, 96x128, bottom=122)
# ==============================================================================
def generate_pine() -> Image.Image:
    """96x128 Spirit Conifer Pine (`bottom=122`).

    9 stepped, swept conifer bough sprays modeled after P20 `cypress.png` pixel discipline,
    expanded to mountain spirit-pine proportions with clear bough-tier separation across BOTH
    the moonlit left side AND the shadowed right side!
    """
    c = PixelCanvas(96, 128)
    p = PINE_PAL
    cx = 48

    # 1) Sturdy tapered red-umber conifer trunk & solid root collar (`y=86..122` visible, mast up to y=14)
    collar = [
        (35, 122), (40, 117), (43, 108), (44, 86), (46, 16),
        (50, 16), (52, 86), (53, 108), (56, 117), (61, 122),
    ]
    ridges = [
        [(44, 102), (42, 114), (37, 121)],
        [(48, 105), (47, 116), (46, 122)],
        [(51, 103), (54, 114), (59, 121)],
    ]
    c.paint_trunk_and_collar(collar, [], p, shadow_under_y=91, root_ridges=ridges)
    c.line([(43, 95), (38, 97)], p["bark_dk"], 2)
    c.px(39, 96, p["bark_lt"])
    c.line([(53, 93), (58, 95)], p["bark_occ"], 2)
    for bx, by in [(45, 98), (46, 104), (45, 110), (44, 116), (48, 101)]:
        c.px(bx, by, p["bark_hi"])
        c.px(bx, by + 1, p["bark_hi"])

    # 2) 9 stepped bough sprays painted TOP-TO-BOTTOM or BOTTOM-TO-TOP?
    #    Notice in `cypress.png`, painting TOP-TO-BOTTOM or painting a `occ` scalloped bottom crease
    #    on every tier makes EVERY tier's flared outer wing tips stick out cleanly past the narrower
    #    upper part of the tier below, creating the classic stepped conifer sawtooth silhouette on BOTH sides!
    tiers = [
        # (y_top, half_w, drop_h, skew, seed) — painted BOTTOM-UP so upper tiers cast shadow onto lower tiers!
        (75, 33, 12,  1, 1401),
        (65, 30, 12, -1, 1402),
        (55, 27, 11,  1, 1403),
        (45, 23, 11, -1, 1404),
        (36, 19, 10,  1, 1405),
        (27, 15,  9,  0, 1406),
        (19, 11,  8, -1, 1407),
        (11,  8,  7,  0, 1408),
        ( 4,  4,  6,  0, 1409),
    ]
    for idx, (yt, hw, dh, skew, sd) in enumerate(tiers):
        r = det_rng(sd)
        tcx = cx + skew
        left = tcx - hw
        right = tcx + hw

        # Narrow upper neck (`hw * 0.36`) flaring out to wide swept bough tips (`left` / `right`)
        # so EVERY tier creates a crisp stepped conifer wing on BOTH left and right silhouettes!
        bot_pts: list[tuple[float, float]] = []
        x = left
        jag_toggle = True
        while x <= right:
            rel = abs(x - tcx) / max(1.0, hw)
            wing_sweep = rel * 2.2
            tooth = (2.2 if jag_toggle else -0.4) + (r(12) / 10.0)
            bot_pts.append((x, yt + dh + wing_sweep + tooth))
            x += 4
            jag_toggle = not jag_toggle
        if bot_pts[-1][0] < right:
            bot_pts.append((right, yt + dh + 2.0))

        spray_poly = [
            (left, yt + dh * 0.78),
            (left + hw * 0.32, yt + dh * 0.42),
            (tcx - hw * 0.34, yt + 1.0),
            (tcx, yt - 1.5),
            (tcx + hw * 0.34, yt + 1.0),
            (right - hw * 0.32, yt + dh * 0.42),
            (right, yt + dh * 0.78),
        ] + list(reversed(bot_pts))

        # Cast `occ` shadow 2px onto the tier below across the FULL width (left AND right!)
        c.poly([(px_x, px_y + 1.8) for px_x, px_y in spray_poly], p["occ"])
        # Main bough body (`mid` on left-center, `shd` on right)
        c.poly(spray_poly, p["mid"])

        # Right half of each tier gets `shd` on its upper slope and `shd_dk` near its lower-right rim,
        # preserving distinct tier contrast against the `occ` cast-shadow line underneath!
        right_slope = [
            (tcx + 1, yt - 1.0),
            (tcx + hw * 0.34, yt + 1.0),
            (right - hw * 0.32, yt + dh * 0.42),
            (right, yt + dh * 0.78),
        ] + [pt for pt in reversed(bot_pts) if pt[0] >= tcx]
        c.poly(right_slope, p["shd"])
        right_rim = [
            (tcx + hw * 0.38, yt + dh * 0.55),
            (right, yt + dh * 0.78),
        ] + [pt for pt in reversed(bot_pts) if pt[0] >= tcx + hw * 0.25]
        c.poly(right_rim, p["shd_dk"])

        # Crisp needle-cluster highlights stayed INSIDE the tier wing (no stray spikes sticking outside!)
        fan_count = max(2, hw // 8)
        for k in range(fan_count):
            t = (k + 0.45) / fan_count
            fx = left + hw * 0.18 + (tcx - left - hw * 0.14) * t
            fy = yt + dh * (0.60 - 0.22 * t)
            frx = max(2.4, hw * 0.18)
            fry = max(1.8, dh * 0.26)
            c.faceted_spray(fx, fy, frx, fry, p["lit"], sd + 40 + k, vertices=8, jag=0.18)
            if k == 0 or (idx >= 3 and k == 1):
                c.faceted_spray(fx - 0.8, fy - 0.8, frx * 0.58, fry * 0.55, p["hi"], sd + 70 + k, vertices=6, jag=0.14)

        # Subtle mid catchlight on one right-side needle cluster per tier so right side reads as needles, not flat!
        if hw >= 15:
            rx_c = tcx + hw * 0.34
            ry_c = yt + dh * 0.48
            c.faceted_spray(rx_c, ry_c, max(2.2, hw * 0.14), 1.8, p["mid"], sd + 90, vertices=6, jag=0.15)

        # Dark bark cavity peek near center on 3 tiers
        if idx in (1, 3, 5):
            gx_peek = tcx + (2 if idx % 2 else -2)
            gy_peek = int(round(yt + dh * 0.62))
            c.poly([(gx_peek - 1, gy_peek - 1), (gx_peek + 2, gy_peek - 1), (gx_peek + 1, gy_peek + 2), (gx_peek - 1, gy_peek + 2)], p["occ"])
            c.px(gx_peek, gy_peek, p["bark_mid"])

        # Sparse 2px spirit-frost moonlight rim on upper-left bough shoulder
        if idx >= 2:
            rx_tip = int(round(left + hw * 0.22))
            ry_tip = int(round(yt + dh * 0.50))
            if c.get(rx_tip, ry_tip)[3] == 255:
                c.px(rx_tip, ry_tip, p["rim"])

    c.px(cx, 3, p["hi"])
    c.px(cx - 1, 4, p["rim"])

    add_root_moss_and_petals(c, bottom_y=122, moss_spans=[(35, 42), (46, 51), (55, 61)], petal_pts=[])
    c.deorphan_clusters(passes=1)
    c.enforce_margins_and_bottom(122, (36, 60), MOSS["occ"], side_margin=4)
    return c.im


# ==============================================================================
# 5. TOWER MYSTIC PLUM TREE (`twisted.png`, 112x132, bottom=126, span >= 100)
# ==============================================================================
def generate_twisted() -> Image.Image:
    """112x132 Gnarled Elderwood Mystic Plum (`bottom=126`, connected bonsai-like clouds, span >= 100)."""
    c = PixelCanvas(112, 132)
    p = PLUM_PAL

    collar = [
        (35, 126), (41, 120), (42, 110), (37, 96), (42, 82), (51, 71),
        (62, 71), (54, 84), (49, 97), (55, 110), (62, 120), (70, 126),
    ]
    limbs = [
        [(41, 91, 3.8), (29, 77, 2.9), (19, 63, 2.1)],
        [(52, 76, 3.4), (44, 59, 2.5), (37, 43, 1.8)],
        [(59, 73, 3.6), (75, 63, 2.7), (89, 54, 2.0)],
        [(57, 71, 3.1), (63, 53, 2.3), (60, 35, 1.7)],
    ]
    ridges = [
        [(39, 98), (43, 111), (39, 121), (36, 126)],
        [(46, 102), (50, 115), (49, 125)],
        [(52, 100), (58, 114), (66, 124), (69, 126)],
    ]
    c.paint_trunk_and_collar(collar, limbs, p, shadow_under_y=75, root_ridges=ridges)

    for gx, gy in [(40, 114), (39, 104), (38, 94), (43, 84), (50, 76), (31, 76), (71, 63)]:
        c.px(gx, gy, p["bark_hi"])
        c.px(gx + 1, gy - 1, p["bark_hi"])

    lobes = [
        (55, 20, 22, 13, 1501),
        (35, 33, 21, 13, 1502),
        (74, 32, 21, 13, 1503),
        (54, 44, 20, 12, 1504),
        (21, 51, 17, 11, 1505),
        (90, 49, 17, 11, 1506),
        (36, 55, 13,  9, 1507),
        (75, 54, 14,  9, 1508),
    ]
    creases = [
        [(33, 38), (46, 40), (59, 36)],
        [(61, 47), (74, 49), (87, 45)],
    ]
    c.paint_unified_canopy(
        lobes=lobes,
        pal=p,
        tree_cx=55,
        tree_cy=38,
        tree_rx=45,
        tree_ry=24,
        sky_gaps=[(45, 55, 4.0, 2.8), (66, 54, 4.2, 2.8)],
        calyx_spots=[(24, 58), (56, 53), (85, 56)],
        internal_creases=creases,
    )
    c.line([(49, 69), (44, 59), (39, 50)], p["bark_lt"], 1)
    c.line([(50, 69), (45, 59), (40, 50)], p["bark_dk"], 1)
    c.line([(60, 70), (72, 62), (82, 56)], p["bark_occ"], 2)

    for x0, x1 in [(35, 42), (48, 55), (62, 70)]:
        for x in range(x0, x1 + 1):
            c.px(x, 126, p["occ"])
            if x0 + 1 <= x <= x1 - 1:
                c.px(x, 125, p["leaf_dk"])
        mid = (x0 + x1) // 2
        c.px(mid, 124, p["leaf_lt"])
        c.px(mid - 1, 124, p["leaf_dk"])

    c.deorphan_clusters(passes=1)
    c.enforce_margins_and_bottom(126, (36, 69), p["occ"], side_margin=4)
    return c.im


# ==============================================================================
# 6. TOWER WOODLAND FOREST CANOPY BUSH (`forest_bush.png`, 64x40, bottom=36)
# ==============================================================================
def generate_forest_bush() -> Image.Image:
    """64x40 Wild Asymmetric Woodland Fern/Bramble Shrub (`bottom=36`, cohesive & grounded)."""
    c = PixelCanvas(64, 40)
    p = FOREST_BUSH_PAL

    c.poly([(7, 36), (5, 30), (11, 21), (53, 22), (58, 31), (56, 36)], p["occ"])

    lobes = [
        (27, 17, 14.2, 10.0, 1601),
        (15, 25, 10.5,  8.0, 1602),
        (46, 24, 12.5,  8.5, 1603),
        (33, 27, 12.5,  7.5, 1604),
    ]
    c.paint_unified_canopy(
        lobes=lobes,
        pal=p,
        tree_cx=30,
        tree_cy=22,
        tree_rx=23,
        tree_ry=12,
        sky_gaps=[],
        calyx_spots=[],
        internal_creases=[[(21, 23), (30, 25), (39, 22)]],
    )

    c.deorphan_clusters(passes=1)

    for sx, sy in [(20, 33), (41, 33)]:
        c.px(sx, sy, p["stem_lt"])
        c.px(sx + 1, sy, p["stem_lt"])
        c.px(sx + 1, sy + 1, p["stem_dk"])
        c.px(sx + 2, sy + 1, p["stem_dk"])
    for bx, by in [(18, 25), (43, 24)]:
        c.px(bx, by, p["berry"])
        c.px(bx + 1, by, p["berry"])

    c.enforce_margins_and_bottom(36, (8, 55), p["occ"], side_margin=4)
    return c.im


# ==============================================================================
# MANIFEST STAMPING & PREVIEW CONTACT SHEET
# ==============================================================================
ASSET_SPECS = {
    "canopy_a": {
        "fn": generate_canopy_a,
        "size": [114, 132],
        "bottom": 126,
        "ground_anchor": [57, 124],
        "runtime_usage": "PropKind.SAKURA_BLOSSOM_TREE",
        "phase": "B",
    },
    "canopy_b": {
        "fn": generate_canopy_b,
        "size": [96, 114],
        "bottom": 109,
        "ground_anchor": [48, 107],
        "runtime_usage": "PropKind.SAKURA_BLOSSOM_TREE",
        "phase": "B",
    },
    "hedge_bush": {
        "fn": generate_hedge_bush,
        "size": [64, 40],
        "bottom": 36,
        "ground_anchor": [32, 36],
        "runtime_usage": "PropKind.GARDEN_HEDGE_BUSH",
        "phase": "B",
    },
    "pine": {
        "fn": generate_pine,
        "size": [96, 128],
        "bottom": 122,
        "ground_anchor": [48, 120],
        "runtime_usage": "PropKind.PINE_SPIRIT_TREE",
        "phase": "C",
    },
    "twisted": {
        "fn": generate_twisted,
        "size": [112, 132],
        "bottom": 126,
        "ground_anchor": [56, 124],
        "runtime_usage": "PropKind.MYSTIC_PLUM_TREE",
        "phase": "C",
    },
    "forest_bush": {
        "fn": generate_forest_bush,
        "size": [64, 40],
        "bottom": 36,
        "ground_anchor": [32, 36],
        "runtime_usage": "PropKind.FOREST_CANOPY_BUSH",
        "phase": "C",
    },
}


def measure_alpha_bounds(im: Image.Image) -> dict:
    w, h = im.size
    apx = im.load()
    xs, ys = [], []
    colors = set()
    semi = 0
    for y in range(h):
        for x in range(w):
            r, g, b, a = apx[x, y]
            if 0 < a < 255:
                semi += 1
            if a >= 128:
                xs.append(x)
                ys.append(y)
                colors.add((r, g, b))
    return {
        "size": [w, h],
        "x_range": [min(xs), max(xs)],
        "x_span": max(xs) - min(xs) + 1,
        "y_range": [min(ys), max(ys)],
        "top": min(ys),
        "bottom": max(ys),
        "bottom_padding": h - 1 - max(ys),
        "opaque_colors": len(colors),
        "semi_transparent_px": semi,
    }


def upsert_manifest(asset_id: str, png_path: Path, spec: dict) -> None:
    manifest = json.loads(MANIFEST.read_text())
    digest = hashlib.sha256(png_path.read_bytes()).hexdigest()
    assets = manifest["assets"]
    existing = next((a for a in assets if a["id"] == asset_id), None)
    if existing is not None:
        existing["path"] = f"{asset_id}.png"
        existing["size"] = list(spec["size"])
        existing["sha256"] = digest
    else:
        assets.append({
            "id": asset_id,
            "path": f"{asset_id}.png",
            "size": list(spec["size"]),
            "ground_anchor": list(spec["ground_anchor"]),
            "runtime_usage": spec["runtime_usage"],
            "sha256": digest,
        })
    MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n")


def build_contact_sheets(images: dict[str, Image.Image], out_dir: Path) -> None:
    """Write labeled ART CONTACT SHEET (1x and 3x nearest-neighbor) against Sanctuary lawn tone."""
    out_dir.mkdir(parents=True, exist_ok=True)
    order = ["canopy_a", "canopy_b", "pine", "twisted", "hedge_bush", "forest_bush", "cypress_ctrl", "oak_ctrl"]
    rendered = dict(images)
    if (SANCT / "cypress.png").exists():
        rendered["cypress_ctrl"] = Image.open(SANCT / "cypress.png").convert("RGBA")
    if (SANCT / "oak.png").exists():
        rendered["oak_ctrl"] = Image.open(SANCT / "oak.png").convert("RGBA")

    for scale in (1, 3):
        cell_w = 136 * scale
        cell_h = 164 * scale
        cols = 4
        rows = 2
        header_h = 26 * scale
        sheet = Image.new("RGBA", (cols * cell_w, header_h + rows * cell_h), (24, 34, 28, 255))
        draw = ImageDraw.Draw(sheet)
        draw.text((8 * scale, 6 * scale), f"ART CONTACT SHEET (OFFLINE {scale}X NEAREST) - P22 VEGETATION + P18/P20 CONTROLS", fill=(216, 224, 214, 255))
        split_map = {"canopy_a": 0.56, "canopy_b": 0.56, "pine": 0.52, "twisted": 0.54}

        for idx, key in enumerate(order):
            if key not in rendered:
                continue
            im = rendered[key]
            cx0 = (idx % cols) * cell_w
            cy0 = header_h + (idx // cols) * cell_h
            bg = (40, 56, 44, 255) if (idx % 2 == 0) else (33, 47, 38, 255)
            draw.rectangle([cx0 + 2, cy0 + 2, cx0 + cell_w - 3, cy0 + cell_h - 3], fill=bg, outline=(62, 78, 66, 255))
            up = im.resize((im.width * scale, im.height * scale), Image.Resampling.NEAREST)
            ox = cx0 + (cell_w - up.width) // 2
            oy = cy0 + cell_h - 14 * scale - up.height
            sheet.alpha_composite(up, (ox, oy))
            if scale == 3:
                b = measure_alpha_bounds(im)
                foot_y = oy + b["bottom"] * scale
                draw.line([(ox - 6, foot_y), (ox - 2, foot_y)], fill=(255, 196, 92, 255), width=2)
                if key in split_map:
                    sy = oy + int(round(im.height * split_map[key])) * scale
                    draw.line([(ox - 6, sy), (ox - 2, sy)], fill=(120, 210, 255, 255), width=2)
            draw.text((cx0 + 6 * scale, cy0 + 4 * scale), f"{key} ({im.width}x{im.height})", fill=(224, 216, 228, 255))

        sheet.save(out_dir / f"p22_vegetation_contact_sheet_{scale}x.png", optimize=True)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--phase", choices=["B", "all"], default="all")
    ap.add_argument("--preview-only", type=Path, default=None)
    args = ap.parse_args()

    selected = [k for k, v in ASSET_SPECS.items() if args.phase == "all" or v["phase"] == "B"]
    generated: dict[str, Image.Image] = {}
    summary: dict[str, dict] = {}

    for key in selected:
        spec = ASSET_SPECS[key]
        im = spec["fn"]()
        bounds = measure_alpha_bounds(im)
        assert bounds["size"] == spec["size"], f"{key}: size mismatch {bounds['size']} != {spec['size']}"
        assert bounds["bottom"] == spec["bottom"], f"{key}: bottom row {bounds['bottom']} != {spec['bottom']}"
        assert bounds["semi_transparent_px"] == 0, f"{key}: expected crisp binary alpha"
        generated[key] = im
        summary[key] = bounds

        if args.preview_only is None:
            out_png = SANCT / f"{key}.png"
            im.save(out_png, optimize=True)
            upsert_manifest(key, out_png, spec)

    sheet_dir = args.preview_only if args.preview_only is not None else (ROOT / "docs/visual-evidence/p22")
    build_contact_sheets(generated, sheet_dir)
    print(json.dumps({"phase": args.phase, "preview_only": str(args.preview_only), "assets": summary}, indent=2))


if __name__ == "__main__":
    main()
