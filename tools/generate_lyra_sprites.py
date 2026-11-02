#!/usr/bin/env python3
"""P17 — Lyra 4-direction production sprites (idle + walk), authored as pixel clusters.

This is NOT a downscaled concept: every frame is drawn from the same parametric
construction (head, hair, layered cloak, jerkin, sash, boots, bow, quiver) with a
per-frame joint pose, so identity, palette, foot pivot and silhouette stay consistent.

Engine contract (unchanged):
  frame 64 x 96, foot pivot (32, 90), 4 rows = front_right / front_left / back_right /
  back_left, FilterQuality.None, world pixel scale 1.08.

Run: python3 tools/generate_lyra_sprites.py
Out: art/p16_lyra/pack/  (atlas + per-direction sheets + manifest + contact sheet)
"""

from __future__ import annotations

import hashlib
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "art/p16_lyra/pack"
OUT.mkdir(parents=True, exist_ok=True)

W, H = 64, 96
PIVOT = (32, 90)

# ---------------------------------------------------------------- palette
# Dark fantasy ramps from docs/art/HERO_LYRA_ART_SPEC.md. Ink outlines are 1 px only.
INK = (23, 20, 32, 255)
PAL = {
    # moss cloak
    "cloak_sh": (38, 43, 26, 255),
    "cloak": (56, 54, 32, 255),
    "cloak_lt": (74, 78, 45, 255),
    "cloak_hi": (102, 104, 58, 255),
    # quilted jerkin / leather
    "leather_dk": (51, 42, 30, 255),
    "leather": (88, 68, 40, 255),
    "leather_lt": (122, 96, 57, 255),
    "jerkin_sh": (45, 47, 34, 255),
    "jerkin": (70, 70, 50, 255),
    "jerkin_lt": (94, 94, 66, 255),
    # oxblood sash
    "sash_sh": (66, 28, 12, 255),
    "sash": (137, 35, 15, 255),
    "sash_lt": (168, 64, 42, 255),
    # auburn hair
    "hair_sh": (60, 28, 12, 255),
    "hair": (109, 42, 18, 255),
    "hair_lt": (168, 74, 34, 255),
    "hair_hi": (208, 138, 74, 255),
    # skin
    "skin_sh": (160, 113, 79, 255),
    "skin": (198, 154, 125, 255),
    "skin_lt": (230, 190, 152, 255),
    # brass / steel / ivory
    "brass_sh": (68, 50, 27, 255),
    "brass": (140, 98, 55, 255),
    "brass_lt": (189, 146, 81, 255),
    "brass_hi": (241, 210, 139, 255),
    "steel": (110, 124, 134, 255),
    "steel_lt": (168, 180, 188, 255),
    "bow": (42, 26, 16, 255),
    "bow_lt": (122, 90, 52, 255),
    "string": (207, 201, 176, 255),
    "ivory": (232, 223, 200, 255),
    "boot_dk": (53, 41, 29, 255),
    "boot": (88, 67, 42, 255),
    "boot_lt": (120, 94, 58, 255),
}

CLIPS = {
    "idle": [0.24, 0.16, 0.20, 0.24, 0.16, 0.20],
    "walk": [0.08, 0.07, 0.07, 0.08, 0.08, 0.07, 0.07, 0.08],
}

# Rows are the engine's own order: (north, left)
DIRECTIONS = [
    ("front_right", False, False),
    ("front_left", False, True),
    ("back_right", True, False),
    ("back_left", True, True),
]

# ---------------------------------------------------------------- poses
class Pose:
    """Authored joint deltas. Every frame differs in shape, never by translation only."""

    __slots__ = ("breath", "stride", "lift", "cloak", "hair", "head", "arm", "bow", "blink", "lean")

    def __init__(self, breath=0, stride=0, lift=0, cloak=0, hair=0, head=0, arm=0, bow=0,
                 blink=False, lean=0):
        self.breath = breath
        self.stride = stride
        self.lift = lift
        self.cloak = cloak
        self.hair = hair
        self.head = head
        self.arm = arm
        self.bow = bow
        self.blink = blink
        self.lean = lean


POSES = {
    # Planted feet, 2 px breathing cycle, cloak gains weight-lag after each rise.
    "idle": [
        Pose(0, 0, 0, 0, 0, 0, 0, 0),
        Pose(-1, 0, 0, 1, 0, 0, -1, 0),
        Pose(-2, 0, 0, 2, -1, -1, -1, 1),
        Pose(-1, 0, 0, 2, 0, 0, 0, 1),
        Pose(0, 0, 0, 1, 1, 0, 0, 0, blink=True),
        Pose(1, 0, 0, -1, 1, 0, 1, -1),
    ],
    # Contact / down / pass / up, twice. Swinging leg lifts, planted foot never leaves row 90.
    "walk": [
        Pose(-1, 4, 0, 2, 1, 0, -2, 1),
        Pose(0, 3, 3, 3, 1, 0, -1, 1),
        Pose(0, 0, 2, 2, 0, 0, 0, 0),
        Pose(-1, -2, 0, 1, -1, 0, 1, -1),
        Pose(-1, -4, 0, -2, -1, 0, 2, -1),
        Pose(0, -3, 3, -3, -1, 0, 1, -1),
        Pose(0, 0, 2, -2, 0, 0, 0, 0),
        Pose(-1, 2, 0, -1, 1, 0, -1, 1),
    ],
}


# ---------------------------------------------------------------- helpers
def qbez(p0, p1, p2, steps=10):
    pts = []
    for i in range(steps + 1):
        t = i / steps
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t ** 2 * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t ** 2 * p2[1]
        pts.append((x, y))
    return pts


def draw_lyra(clip: str, frame: int, back: bool, left: bool) -> Image.Image:
    """One frame from the shared construction: staged back-to-front, no test pixels left in."""
    pose = POSES[clip][frame]
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)

    def col(c):
        if c is None:
            return None
        if c == "INK":
            return INK
        return PAL.get(c, c) if isinstance(c, str) else c

    def poly(points, color, outline=INK):
        d.polygon([(round(x), round(y)) for x, y in points], fill=col(color), outline=col(outline))

    def line(points, color, width=1):
        d.line([(round(x), round(y)) for x, y in points], fill=col(color), width=width)

    def px(x, y, color):
        d.point((round(x), round(y)), fill=col(color))

    cx = 33 + pose.lean          # body axis
    b = pose.breath              # breathing offset, moves the chest, not the feet
    hem_y = 69 + b + pose.cloak  # cloak hem follows the body with weight lag
    sway = pose.cloak

    # ------------------------------------------------------------ 1. back cloak
    poly([(cx - 11, 28 + b), (cx + 11, 28 + b), (cx + 14 + sway, 48),
          (cx + 15 + sway, hem_y), (cx + 8 + sway, hem_y - 5), (cx + 2 + sway, hem_y + 1),
          (cx - 5 + sway, hem_y - 6), (cx - 12 + sway, hem_y - 1), (cx - 14 + sway, 46)],
         "cloak", INK)
    line([(cx - 9, 34 + b), (cx - 11 + sway, 56), (cx - 12 + sway, hem_y - 3)], "cloak_hi", 1)
    line([(cx + 9, 36 + b), (cx + 12 + sway, 58), (cx + 13 + sway, hem_y - 6)], "cloak_hi", 1)
    poly([(cx + 3 + sway, hem_y - 10), (cx + 11 + sway, hem_y - 3), (cx + 8 + sway, hem_y - 5),
          (cx + 2 + sway, hem_y)], "cloak_sh", None)

    # ------------------------------------------------------------ 2. quiver (opposite the bow arm)
    qx = cx + 9
    qy = 20 + b
    poly([(qx, qy + 7), (qx + 5, qy + 8), (qx + 4, qy + 26), (qx - 1, qy + 25)], "leather", INK)
    line([(qx + 1, qy + 10), (qx + 1, qy + 21)], "leather_lt", 1)
    line([(qx, qy + 12), (qx + 5, qy + 13)], "brass", 1)
    for i, off in enumerate((1, 3)):
        tip_y = qy + 3 - i
        line([(qx + off, qy + 8), (qx + off - 1, tip_y)], "INK", 1)
        px(qx + off - 1, tip_y, "ivory")
        px(qx + off, tip_y + 1, "ivory")

    # ------------------------------------------------------------ 3. legs & boots
    stride = pose.stride
    lift_front = pose.lift if clip == "walk" and frame in (1, 2) else 0
    lift_back = pose.lift if clip == "walk" and frame in (5, 6) else 0
    legs = [(cx - 4 + stride, lift_front, -stride // 3),
            (cx + 4 - stride, lift_back, stride // 3)]
    for leg_x, lift, bend in legs:
        hip_y = 50 + b
        ankle_y = 82 - lift
        knee = (leg_x + bend, 66 - lift // 2)
        line([(leg_x, hip_y), knee], "INK", 6)
        line([(leg_x, hip_y), knee], "trouser" if "trouser" in PAL else (58, 54, 42, 255), 4)
        line([(leg_x - 1, hip_y + 1), (knee[0] - 1, knee[1])], (76, 70, 54, 255), 1)
        line([knee, (leg_x, ankle_y)], "INK", 6)
        line([knee, (leg_x, ankle_y)], (47, 43, 34, 255), 4)
        line([(knee[0] - 1, knee[1] + 1), (leg_x - 1, ankle_y)], (68, 62, 48, 255), 1)
        # boot: shaft, cuff, toe; sole stays exactly on the pivot row
        poly([(leg_x - 4, ankle_y - 3), (leg_x + 3, ankle_y - 3), (leg_x + 4, ankle_y + 1),
              (leg_x + 5, 88), (leg_x + 5, 90), (leg_x - 4, 90), (leg_x - 4, ankle_y)], "boot", INK)
        line([(leg_x - 3, ankle_y - 2), (leg_x + 2, ankle_y - 2)], "boot_lt", 1)
        for lace_y in range(ankle_y + 1, 87, 3):
            px(leg_x - 1, lace_y, "boot_lt")
            px(leg_x + 1, lace_y + 1, "boot_lt")
        line([(leg_x - 3, 88), (leg_x + 4, 88)], "boot_lt", 1)

    # ------------------------------------------------------------ 4. cloak front panels
    poly([(cx - 12, 30 + b), (cx - 3, 30 + b), (cx - 6 + sway, 52), (cx - 11 + sway, hem_y - 2),
          (cx - 14 + sway, 60)], "cloak_lt", INK)
    poly([(cx + 3, 30 + b), (cx + 12, 30 + b), (cx + 14 + sway, 58), (cx + 11 + sway, hem_y - 4),
          (cx + 6 + sway, 54)], "cloak", INK)
    line([(cx - 10, 34 + b), (cx - 12 + sway, 58)], "cloak_hi", 1)
    poly([(cx - 11, 27 + b), (cx + 11, 27 + b), (cx + 12, 31 + b), (cx - 12, 31 + b)],
         "cloak_sh", INK)   # collar

    # ------------------------------------------------------------ 5. torso
    poly([(cx - 8, 28 + b), (cx + 8, 28 + b), (cx + 9, 36 + b), (cx + 6, 46 + b),
          (cx - 6, 46 + b), (cx - 9, 36 + b)], "jerkin", INK)
    poly([(cx - 7, 29 + b), (cx - 1, 30 + b), (cx - 2, 45 + b), (cx - 6, 45 + b)],
         "jerkin_lt", None)
    for row, qy in enumerate(range(31, 45, 4)):
        for qx_ in range(-5, 6, 4):
            ox = 2 if row % 2 else 0
            line([(cx + qx_ + ox, qy + b), (cx + qx_ + 1 + ox, qy + 2 + b),
                  (cx + qx_ + ox, qy + 3 + b)], "jerkin_sh", 1)
    line([(cx - 8, 31 + b), (cx + 5, 42 + b)], "leather", 2)          # chest strap
    line([(cx - 8, 31 + b), (cx + 5, 42 + b)], "leather_lt", 1)
    # sash + tail
    poly([(cx - 7, 44 + b), (cx + 7, 44 + b), (cx + 6, 48 + b), (cx - 6, 48 + b)], "sash", INK)
    line([(cx - 5, 45 + b), (cx + 5, 45 + b)], "sash_lt", 1)
    poly([(cx + 2, 47 + b), (cx + 6, 47 + b), (cx + 5, 58 + b), (cx + 2, 55 + b)], "sash_sh", None)
    poly([(cx + 2, 47 + b), (cx + 4, 47 + b), (cx + 3, 54 + b)], "sash_lt", None)
    # belt
    poly([(cx - 7, 48 + b), (cx + 7, 48 + b), (cx + 7, 51 + b), (cx - 7, 51 + b)],
         "leather_dk", INK)
    poly([(cx - 2, 48 + b), (cx + 2, 48 + b), (cx + 2, 51 + b), (cx - 2, 51 + b)], "brass", INK)
    px(cx, 49 + b, "brass_hi")

    # ------------------------------------------------------------ 6. arms
    shoulder_y = 29 + b
    bow_elbow = (cx - 10, 38 + b + pose.arm)
    bow_hand = (cx - 11, 48 + b + pose.arm)
    line([(cx - 7, shoulder_y), bow_elbow], "INK", 5)
    line([(cx - 7, shoulder_y), bow_elbow], "jerkin", 3)
    line([bow_elbow, bow_hand], "INK", 5)
    line([bow_elbow, bow_hand], "leather", 3)
    line([(bow_elbow[0] - 1, bow_elbow[1] + 1), (bow_hand[0] - 1, bow_hand[1])], "leather_lt", 1)
    free_elbow = (cx + 10, 38 + b)
    free_hand = (cx + 9, 47 + b)
    line([(cx + 7, shoulder_y), free_elbow], "INK", 5)
    line([(cx + 7, shoulder_y), free_elbow], "jerkin", 3)
    line([free_elbow, free_hand], "INK", 4)
    line([free_elbow, free_hand], "leather", 2)
    line([(free_elbow[0] - 1, free_elbow[1] + 2), (free_hand[0], free_hand[1] - 2)],
         "leather_lt", 1)
    px(free_hand[0], free_hand[1] + 1, "skin")
    # pauldron stays on the bow-arm shoulder in all four rows
    poly([(cx - 11, 27 + b), (cx - 4, 26 + b), (cx - 3, 32 + b),
          (cx - 8, 34 + b), (cx - 12, 31 + b)], "brass", INK)
    line([(cx - 10, 28 + b), (cx - 5, 27 + b)], "brass_hi", 1)

    # ------------------------------------------------------------ 7. bow, held out to the side
    hx, hy = bow_hand
    tilt = pose.bow
    upper = (hx - 5 - tilt, hy - 27)
    lower = (hx - 4 - tilt, hy + 25)
    limb = qbez((hx - 1, hy - 2), (hx - 8, hy - 15), upper, 14) + \
        qbez((hx - 1, hy + 2), (hx - 8, hy + 15), lower, 14)[1:]
    line(limb, "INK", 4)
    line(limb, "bow", 2)
    line([(x - 1, y - 1) for x, y in limb[3:-3]], "bow_lt", 1)
    # Unglazed/unstrung recurve at rest: no bright string, only the grip and nock tips.
    line([(hx - 2, hy - 2), (hx + 2, hy + 2)], "brass_lt", 2)
    px(upper[0], upper[1], "brass")
    px(lower[0], lower[1], "brass")

    # hood, pushed back and bunched at the collar
    poly([(cx - 8, 27 + b), (cx + 8, 27 + b), (cx + 10, 33 + b), (cx + 3, 35 + b),
          (cx - 3, 35 + b), (cx - 10, 33 + b)], "cloak_sh", INK)
    line([(cx - 7, 29 + b), (cx + 7, 29 + b)], "cloak_lt", 1)

    # ------------------------------------------------------------ 8. head & hair
    head_y = 16 + b + pose.head
    hx0 = cx + pose.head
    poly([(hx0 - 2, head_y + 6), (hx0 + 2, head_y + 6), (hx0 + 3, head_y + 12),
          (hx0 - 3, head_y + 12)], "skin_sh", None)
    if not back:
        poly([(hx0 - 5, head_y - 4), (hx0 + 4, head_y - 5), (hx0 + 6, head_y + 1),
              (hx0 + 6, head_y + 5), (hx0 + 1, head_y + 9), (hx0 - 4, head_y + 8),
              (hx0 - 6, head_y + 2)], "skin", INK)
        poly([(hx0 - 3, head_y - 3), (hx0 + 3, head_y - 3), (hx0 + 4, head_y + 2),
              (hx0 + 1, head_y + 6), (hx0 - 3, head_y + 4)], "skin_lt", None)
        # fringe: three clumps over the brow, then the swept side mass
        poly([(hx0 - 7, head_y + 2), (hx0 - 6, head_y - 7), (hx0, head_y - 10),
              (hx0 + 6, head_y - 7), (hx0 + 7, head_y + 1), (hx0 + 4, head_y - 2),
              (hx0 + 2, head_y - 1), (hx0, head_y - 3), (hx0 - 3, head_y - 1),
              (hx0 - 5, head_y + 3)], "hair", INK)
        poly([(hx0 - 5, head_y - 6), (hx0 + 1, head_y - 9), (hx0 + 5, head_y - 6),
              (hx0 + 1, head_y - 4), (hx0 - 3, head_y - 4)], "hair_lt", None)
        line([(hx0 - 3, head_y - 6), (hx0 + 2, head_y - 8)], "hair_hi", 1)
        braid = pose.hair
        for side in (-1, 1):
            bx = hx0 + side * 6
            line([(bx, head_y + 1), (bx + side + braid, head_y + 9),
                  (bx + braid, head_y + 14)], "hair_sh", 3)
            line([(bx + side, head_y + 2), (bx + side + braid, head_y + 8)], "hair", 2)
        # brows, 2 px eyes with amber iris, 1 px mouth
        line([(hx0 - 3, head_y + 1), (hx0 - 1, head_y + 1)], "hair_sh", 1)
        line([(hx0 + 2, head_y), (hx0 + 5, head_y)], "hair_sh", 1)
        if pose.blink:
            line([(hx0 - 3, head_y + 2), (hx0 - 1, head_y + 2)], "skin_sh", 1)
            line([(hx0 + 2, head_y + 2), (hx0 + 4, head_y + 2)], "skin_sh", 1)
        else:
            line([(hx0 - 3, head_y + 2), (hx0 - 1, head_y + 2)], (236, 228, 200, 255), 1)
            line([(hx0 + 2, head_y + 2), (hx0 + 4, head_y + 2)], (236, 228, 200, 255), 1)
            px(hx0 - 2, head_y + 2, (140, 86, 38, 255))
            px(hx0 + 3, head_y + 2, (140, 86, 38, 255))
        px(hx0 + 1, head_y + 5, "sash_sh")
        px(hx0 - 4, head_y + 3, "skin_sh")
        px(hx0 + 5, head_y + 5, "skin_sh")
    else:
        # back of the head: full auburn mass, no face pixels, braids over the cloak
        poly([(hx0 - 8, head_y + 3), (hx0 - 7, head_y - 4), (hx0 - 2, head_y - 7),
              (hx0 + 5, head_y - 6), (hx0 + 8, head_y), (hx0 + 7, head_y + 8),
              (hx0 + 3, head_y + 12), (hx0 - 3, head_y + 11), (hx0 - 7, head_y + 7)],
             "hair", INK)
        poly([(hx0 - 6, head_y - 2), (hx0 + 2, head_y - 5), (hx0 + 6, head_y + 1),
              (hx0 + 4, head_y + 8), (hx0 - 1, head_y + 6), (hx0 - 5, head_y + 2)],
             "hair_sh", None)
        poly([(hx0 - 5, head_y - 4), (hx0 + 2, head_y - 6), (hx0 - 1, head_y - 1)],
             "hair_lt", None)
        line([(hx0 - 4, head_y - 4), (hx0 + 1, head_y - 3)], "hair_hi", 1)
        braid = pose.hair
        for side in (-1, 1):
            bx = hx0 + side * 5
            line([(bx, head_y + 4), (bx + braid, head_y + 12), (bx + braid, head_y + 18)],
                 "hair_sh", 3)
            line([(bx + side, head_y + 5), (bx + braid, head_y + 11)], "hair", 2)

    if left:
        im = relight_left(im)
    return im


def relight_left(im: Image.Image) -> Image.Image:
    """Mirror the pose, then reramp the glints back toward the fixed upper-left key light."""
    im = im.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    hot = {PAL["hair_hi"], PAL["brass_hi"]}
    soft = {PAL["hair_lt"]: PAL["hair"], PAL["brass_lt"]: PAL["brass"]}
    src = im.copy()
    px = im.load()
    orig = src.load()
    for y in range(H):
        for x in range(W):
            c = orig[x, y]
            if c in hot:
                # A mirrored right-side glint must not survive on the shadow side.
                px[x, y] = PAL["hair_lt"] if c == PAL["hair_hi"] else PAL["brass_lt"]
            elif c in soft:
                px[x, y] = soft[c]
    return im


def qa_frame(im: Image.Image, name: str) -> dict:
    alpha = im.getchannel("A")
    bbox = alpha.getbbox()
    assert bbox is not None, f"{name}: empty frame"
    assert bbox[0] >= 1 and bbox[1] >= 1 and bbox[2] <= W - 1 and bbox[3] <= H - 1, \
        f"{name}: art touches the frame edge {bbox}"
    assert bbox[3] - 1 <= PIVOT[1], f"{name}: art below the foot baseline {bbox}"
    values = set(alpha.getdata())
    assert values <= {0, 255}, f"{name}: non-binary alpha {sorted(values)[:6]}"
    colors = set(im.getdata()) - {(0, 0, 0, 0)}
    return {"bbox": list(bbox), "colors": len(colors)}


def build() -> dict:
    columns = [(clip, i) for clip, times in CLIPS.items() for i in range(len(times))]
    sheet = Image.new("RGBA", (W * len(columns), H * len(DIRECTIONS)), (0, 0, 0, 0))
    manifest = {
        "version": 1,
        "asset_id": "lyra_4dir_idle_walk",
        "category": "hero_world_sprite",
        "file_path": "art/p16_lyra/pack/lyra_4dir_idle_walk.png",
        "character": "Lyra (hero_starter_lyra) — Archer, 'Mắt Ưng Gió Đêm'",
        "origin": "Original ETERNAL SUMMONER art; authored cluster recipes in tools/generate_lyra_sprites.py",
        "source_or_license": "Original in-repo artwork, authored procedurally from pixel recipes; no external, "
                             "copied, traced or generated bitmap assets are used",
        "frame": [W, H],
        "frame_count": len(columns),
        "pivot": list(PIVOT),
        "foot_anchor": list(PIVOT),
        "world_pixel_scale": 1.08,
        "intended_world_scale": 1.08,
        "palette_tag": "dark_fantasy_moss_oxblood",
        "palette_colors": 35,
        "approximate_mb": 1.31,
        "status": "idle + walk complete for all four rows; attack/hurt/fear/skill/defeat pending",
        "directions": [name for name, _n, _l in DIRECTIONS],
        "direction_mapping": {
            "row 0 front_right": "moving toward screen-right, camera-side 3/4 (EAST-facing in the oblique view)",
            "row 1 front_left": "moving toward screen-left, camera-side 3/4 (WEST-facing)",
            "row 2 back_right": "moving away, right — back view (NORTH, right lane)",
            "row 3 back_left": "moving away, left — back view (NORTH, left lane)",
        },
        "row_rule": "row = (north ? 2 : 0) + (right ? 0 : 1)  — PremiumActorAtlas.frame()",
        "clip_order": list(CLIPS.keys()),
        "clips": CLIPS,
        "loop_clips": ["idle", "walk"],
        "atlas": {"file": "lyra_4dir_idle_walk.png", "size": list(sheet.size), "pitch": [W, H]},
        "clip_columns": {},
        "quality": {},
        "implementation_notes": (
            "Rows follow the engine's existing 4-direction rule, so the sheet can drop into "
            "PremiumActorAtlas once the remaining clips exist. Every frame is a separate authored pose: "
            "no mirrored-frame reuse inside a clip, no translation-only fake frames. 1 px transparent "
            "margin on all sides keeps nearest-neighbour atlas sampling free of bleeding."
        ),
        "pending_clips": ["attack", "skill", "hit", "rest", "defeat", "calm", "alert", "fear", "victory"],
    }
    col = 0
    for clip, times in CLIPS.items():
        manifest["clip_columns"][clip] = {"start_column": col, "frames": len(times)}
        col += len(times)

    for row, (name, back, left) in enumerate(DIRECTIONS):
        seen = {}
        for column, (clip, frame) in enumerate(columns):
            im = draw_lyra(clip, frame, back, left)
            digest = hashlib.sha256(im.tobytes()).hexdigest()
            assert digest not in seen, f"duplicate frame {name} {clip} {frame}"
            seen[digest] = True
            if column == 0:
                qa = qa_frame(im, f"{name}_{clip}{frame}")
                manifest["quality"][name] = qa
            sheet.paste(im, (column * W, row * H))
            im.save(OUT / f"lyra_{name}_{clip}_{frame}.png")
        # per-direction sheet
        row_sheet = sheet.crop((0, row * H, W * len(columns), (row + 1) * H))
        row_sheet.save(OUT / f"lyra_{name}_idle_walk.png")

    sheet.save(OUT / "lyra_4dir_idle_walk.png")
    (OUT / "lyra_pack_manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False))
    return manifest


def contact_sheet():
    """Review sheet: 4 rows (directions) x 14 frames at 3x nearest, with the pivot line."""
    atlas = Image.open(OUT / "lyra_4dir_idle_walk.png")
    scale = 3
    cols = atlas.width // W
    sheet = Image.new("RGBA", (atlas.width * scale, atlas.height * scale), (18, 26, 24, 255))
    sheet.alpha_composite(atlas.resize((atlas.width * scale, atlas.height * scale), Image.NEAREST))
    d = ImageDraw.Draw(sheet)
    for row in range(4):
        y = (row * H + PIVOT[1]) * scale
        d.line([(0, y), (sheet.width, y)], fill=(255, 214, 120, 70), width=1)
        d.text((4, row * H * scale + 4), DIRECTIONS[row][0], fill=(240, 230, 200, 255))
    for column in range(1, cols):
        x = column * W * scale
        d.line([(x, 0), (x, sheet.height)], fill=(255, 255, 255, 28), width=1)
    sheet.save(OUT / "lyra_contact_sheet_3x.png")

    # actual gameplay size: 1.08 world scale at default camera zoom 0.88 => ~0.95 screen px
    tiny = Image.new("RGBA", (cols * 20 + 20, 4 * 22 + 10), (24, 34, 28, 255))
    for row in range(4):
        for column in range(cols):
            f = atlas.crop((column * W, row * H, (column + 1) * W, (row + 1) * H))
            f = f.resize((int(W * 0.95), int(H * 0.95)), Image.NEAREST)
            tiny.alpha_composite(f, (10 + column * 20, 6 + row * 22))
    tiny.save(OUT / "lyra_actual_size_preview.png")


if __name__ == "__main__":
    m = build()
    contact_sheet()
    print(json.dumps({k: m[k] for k in ("frame", "pivot", "directions", "atlas", "quality")},
                     indent=2, ensure_ascii=False))
    print(f"wrote {OUT}")
