#!/usr/bin/env python3
"""P17-B — Lyra full production sprite pack (11 clips x 4 directions) + runtime atlas.

Every frame is drawn from one parametric construction (hair, layered cloak, jerkin,
sash, boots, bow, quiver), so identity, palette, foot pivot and silhouette stay
consistent. Clip names, frame counts and durations are copied from the engine contract
in app/src/main/assets/premium_slice/manifest.json, which PremiumActorAtlas reads for
every premium actor — a sheet that does not match it would crash on getValue().

The finished atlas is written straight into the APK assets and its SHA-256 entry is
updated in the manifest, so tools/validate_assets.py keeps passing in CI.

Run: python3 tools/generate_lyra_sprites.py
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets/premium_slice"
PACK = ROOT / "art/p16_lyra/pack"
PACK.mkdir(parents=True, exist_ok=True)

W, H = 64, 96
PIVOT = (32, 90)
ATLAS_NAME = "lyra.png"

# Engine contract — must equal premium_slice/manifest.json clip_order + frame counts.
CLIPS = {
    "idle": [0.24, 0.16, 0.20, 0.24, 0.16, 0.20],
    "walk": [0.08, 0.07, 0.07, 0.08, 0.08, 0.07, 0.07, 0.08],
    "attack": [0.06, 0.06, 0.06, 0.04, 0.045, 0.055, 0.07, 0.09],
    "skill": [0.08, 0.08, 0.07, 0.07, 0.06, 0.07, 0.08, 0.09, 0.12],
    "hit": [0.045, 0.065, 0.085, 0.105],
    "rest": [1.0],
    "defeat": [0.09, 0.11, 0.13, 0.15, 0.20, 0.32],
    "calm": [0.24, 0.18, 0.20, 0.24, 0.16, 0.18],
    "alert": [0.16, 0.20, 0.18, 0.26],
    "fear": [0.12, 0.08, 0.16, 0.09, 0.18, 0.17],
    "victory": [0.14, 0.16, 0.22, 0.24, 0.20, 0.24],
}
DIRECTIONS = [
    ("front_right", False, False),
    ("front_left", False, True),
    ("back_right", True, False),
    ("back_left", True, True),
]

# ---------------------------------------------------------------- palette
INK = (23, 20, 32, 255)
PAL = {
    "cloak_sh": (38, 43, 26, 255), "cloak": (56, 54, 32, 255),
    "cloak_lt": (74, 78, 45, 255), "cloak_hi": (102, 104, 58, 255),
    "leather_dk": (51, 42, 30, 255), "leather": (88, 68, 40, 255),
    "leather_lt": (122, 96, 57, 255),
    "jerkin_sh": (48, 50, 36, 255), "jerkin": (82, 82, 58, 255),
    "jerkin_lt": (106, 106, 76, 255),
    "sash_sh": (66, 28, 12, 255), "sash": (137, 35, 15, 255), "sash_lt": (168, 64, 42, 255),
    "hair_sh": (60, 28, 12, 255), "hair": (109, 42, 18, 255),
    "hair_lt": (168, 74, 34, 255), "hair_hi": (208, 138, 74, 255),
    "skin_sh": (160, 113, 79, 255), "skin": (198, 154, 125, 255), "skin_lt": (230, 190, 152, 255),
    "brass_sh": (68, 50, 27, 255), "brass": (140, 98, 55, 255),
    "brass_lt": (189, 146, 81, 255), "brass_hi": (241, 210, 139, 255),
    "steel": (110, 124, 134, 255), "steel_lt": (168, 180, 188, 255),
    "bow": (38, 34, 30, 255), "bow_lt": (104, 92, 70, 255), "string": (207, 201, 176, 255),
    "string_dim": (108, 104, 86, 255),
    "ivory": (232, 223, 200, 255),
    "boot_dk": (53, 41, 29, 255), "boot": (88, 67, 42, 255), "boot_lt": (120, 94, 58, 255),
    "trouser": (58, 54, 42, 255), "trouser_lt": (76, 70, 54, 255),
    "magic": (108, 206, 214, 255), "magic_lt": (196, 246, 246, 255),
}
# Authored inline tones (eye, sole, magic spark) kept out of PAL to avoid ramp noise.
EYE = (140, 86, 38, 255)
SOLE = (92, 84, 66, 255)
GLINT = (236, 228, 200, 255)


class Pose:
    """Authored joint deltas per frame. Nothing here is a whole-sprite translation."""

    __slots__ = ("breath", "stride", "lift", "cloak", "hair", "head", "arm", "bowtilt",
                 "blink", "lean", "crouch", "bowup", "draw", "arrow")

    def __init__(self, breath=0, stride=0, lift=0, cloak=0, hair=0, head=0, arm=0,
                 bowtilt=0, blink=False, lean=0, crouch=0, bowup=0, draw=0, arrow=0):
        self.breath = breath
        self.stride = stride
        self.lift = lift
        self.cloak = cloak
        self.hair = hair
        self.head = head
        self.arm = arm
        self.bowtilt = bowtilt
        self.blink = blink
        self.lean = lean
        self.crouch = crouch
        self.bowup = bowup      # 0 side carry, 1 chest, 2 aimed, 3 overhead
        self.draw = draw        # 0 relaxed string, 1-3 string pulled to the cheek
        self.arrow = arrow      # 0 none, 1 arrow nocked, 2 magic arrow


POSES = {
    "idle": [Pose(), Pose(-1, cloak=1, arm=-1), Pose(-2, cloak=2, hair=-1, head=-1, arm=-1, bowtilt=1),
             Pose(-1, cloak=2, bowtilt=1), Pose(0, cloak=1, hair=1, blink=True),
             Pose(1, cloak=-1, hair=1, arm=1, bowtilt=-1)],
    "walk": [Pose(-1, 4, 0, 2, 1, 0, -2, 1), Pose(0, 3, 3, 3, 1, 0, -1, 1),
             Pose(0, 0, 2, 2, 0, 0, 0, 0), Pose(-1, -2, 0, 1, -1, 0, 1, -1),
             Pose(-1, -4, 0, -2, -1, 0, 2, -1), Pose(0, -3, 3, -3, -1, 0, 1, -1),
             Pose(0, 0, 2, -2, 0, 0, 0, 0), Pose(-1, 2, 0, -1, 1, -1, -1, 1)],
    # Reach -> nock -> raise -> draw -> full draw -> release -> follow-through -> recover.
    "attack": [Pose(0, lean=1, cloak=1),
               Pose(-1, lean=1, cloak=1, bowup=1, arrow=1),
               Pose(-1, lean=1, cloak=1, bowup=2, draw=1, arrow=1),
               Pose(-1, lean=1, cloak=0, head=-1, bowup=2, draw=2, arrow=1),
               Pose(-2, lean=2, cloak=-1, head=-1, bowup=2, draw=3, arrow=1),
               Pose(0, lean=1, cloak=-3, hair=1, bowup=2),
               Pose(1, lean=0, cloak=-3, hair=2, bowup=2),
               Pose(0, cloak=1, bowup=1), Pose(0, lean=1, cloak=-1, bowtilt=-1)],
    # Magic nock -> aim -> charge -> release overhead -> spark -> settle.
    "skill": [Pose(0, cloak=1, bowup=1, arrow=2),
              Pose(-1, lean=1, cloak=2, bowup=2, draw=1, arrow=2),
              Pose(-1, lean=2, cloak=2, head=-1, bowup=2, draw=2, arrow=2),
              Pose(-2, lean=1, cloak=1, head=-1, bowup=3, draw=3, arrow=2),
              Pose(0, lean=-1, cloak=-3, hair=2, bowup=3),
              Pose(1, cloak=-4, hair=3, bowup=3),
              Pose(0, cloak=-2, hair=1, bowup=2),
              Pose(0, cloak=1, bowup=1, head=-1),
              Pose(0, cloak=0, bowup=0, arm=1, hair=1)],
    # Recoil back, deepest impact, weight returns, settle.
    "hit": [Pose(-3, lean=-3, crouch=1, cloak=3, head=1, arm=2, bowtilt=-3),
            Pose(-4, lean=-5, crouch=3, cloak=5, head=3, arm=4, blink=True, bowtilt=-5),
            Pose(-2, lean=-3, crouch=2, cloak=3, head=1, arm=2, bowtilt=-2),
            Pose(-1, lean=-1, crouch=1, cloak=0, arm=0, bowtilt=0)],
    "rest": [Pose(0, crouch=9, cloak=2)],
    # Stagger, buckle, then a dedicated collapsed body (see draw_collapsed).
    "defeat": [Pose(-3, lean=-4, crouch=3, cloak=3, arm=3, bowtilt=-3),
               Pose(-5, lean=-5, crouch=7, cloak=4, head=2, arm=4, bowtilt=-4),
               Pose(-5, lean=-6, crouch=11, cloak=5, head=3),
               Pose(-4, lean=-6, crouch=11, cloak=5, head=4),
               Pose(-3, lean=-6, crouch=13, cloak=4, head=4),
               Pose(-2, lean=-6, crouch=14, cloak=3, head=4)],
    # Quiet standing variant: shallower breathing, head turned, bow lowered.
    "calm": [Pose(0, hair=1, head=1), Pose(0, cloak=-1, hair=2, head=1),
             Pose(-1, cloak=-1, hair=2, head=2, blink=True), Pose(-1, cloak=0, hair=1, head=1),
             Pose(0, cloak=1, hair=2, head=1), Pose(0, cloak=1, hair=0, head=2)],
    "alert": [Pose(-1, lean=1, cloak=1, bowup=1, head=-1),
              Pose(-2, lean=2, cloak=2, bowup=1, head=-2),
              Pose(-1, lean=2, cloak=1, bowup=1, head=-2),
              Pose(-1, lean=1, cloak=0, bowup=1, head=-1)],
    # Pull in, step back, hesitate, partly recover.
    "fear": [Pose(-1, stride=-1, lean=-2, crouch=3, cloak=-2, head=1, arm=2, bowtilt=-2),
             Pose(-2, stride=-2, lean=-3, crouch=5, cloak=-3, head=3, arm=3, blink=True, bowtilt=-3),
             Pose(-2, stride=-2, lean=-4, crouch=7, cloak=-4, head=4, arm=4, bowtilt=-3),
             Pose(-2, stride=-1, lean=-3, crouch=5, cloak=-3, head=3, arm=3, bowtilt=-2),
             Pose(-1, stride=-1, lean=-2, crouch=4, cloak=-2, head=2, arm=2, hair=1, bowtilt=-1),
             Pose(-1, stride=0, lean=-1, crouch=2, cloak=-1, head=1, arm=1)],
    # Bow raised overhead, chest open, then settle.
    "victory": [Pose(0, lean=0, cloak=1, bowup=2), Pose(1, lean=-1, cloak=2, hair=1, bowup=3),
                Pose(1, lean=-2, cloak=3, hair=2, bowup=3, head=-1),
                Pose(0, lean=-2, cloak=2, hair=2, bowup=3, head=-1),
                Pose(0, lean=-1, cloak=1, hair=1, bowup=2), Pose(0, cloak=0, bowup=1)],
}


def qbez(p0, p1, p2, steps=12):
    pts = []
    for i in range(steps + 1):
        t = i / steps
        pts.append(((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t ** 2 * p2[0],
                    (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t ** 2 * p2[1]))
    return pts


def _tools(d):
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

    def limb(a, b, width, dark, mid, light):
        line([a, b], "INK", width + 2)
        line([a, b], mid, width)
        line([(a[0] - 1, a[1]), (b[0] - 1, b[1] - 1)], light, 1)

    return col, poly, line, px, limb


def draw_bow(poly, line, px, bx, by, tilt, draw_hand=None, arrow=0, tips=None, flare=8):
    """Recurve bow gripped at (bx,by). `flare` is how far the belly bows away from the
    grip: a wide belly (>=12) is what makes it read as a bow and not as a staff at 1x.
    The string and nocked arrow only appear when a draw hand is supplied, so the release
    frame is genuinely string-clear instead of a recoloured still."""
    if tips is None:
        upper = (bx - 4 - tilt, by - 24)
        lower = (bx - 3 - tilt, by + 22)
    else:
        upper, lower = tips
    limb = qbez((bx, by - 1), (bx - flare, by - 12), upper, 14) + \
        qbez((bx, by + 1), (bx - flare, by + 12), lower, 14)[1:]
    line(limb, "INK", 3)
    line(limb, "bow", 1)
    line([(x - 1, y - 1) for x, y in limb[2:-2]], "bow_lt", 1)
    # riser
    poly([(bx - 1, by - 3), (bx + 1, by - 3), (bx + 1, by + 3), (bx - 1, by + 3)], "brass", INK)
    px(bx - 1, by - 2, "brass_hi")
    px(upper[0], upper[1], "brass")
    px(lower[0], lower[1], "brass")
    if draw_hand is not None:
        line([upper, draw_hand, lower], "string_dim", 1)
        if arrow:
            shaft = "magic" if arrow == 2 else "bow_lt"
            tip = "magic_lt" if arrow == 2 else "steel_lt"
            line([draw_hand, (bx + 11, by)], "INK", 2)
            line([draw_hand, (bx + 11, by)], shaft, 1)
            px(bx + 11, by, tip)
            px(bx + 10, by - 1, tip)
            px(draw_hand[0], draw_hand[1] - 1, "ivory")
            px(draw_hand[0] + 1, draw_hand[1], "ivory")
            if arrow == 2:
                px(bx + 7, by - 2, "magic_lt")
                px(bx + 5, by + 2, "magic")
    else:
        line([upper, (bx + 1, by), lower], "string_dim", 1)
        if arrow:
            # Arrow resting on the bow, not yet drawn — keeps the nock frame distinct
            # from the plain carry frame instead of being a still that only differs by luck.
            shaft = "magic" if arrow == 2 else "bow_lt"
            tip = "magic_lt" if arrow == 2 else "steel_lt"
            line([(bx - 7, by), (bx + 11, by)], "INK", 2)
            line([(bx - 7, by), (bx + 11, by)], shaft, 1)
            px(bx + 11, by, tip)
            px(bx - 7, by - 1, "ivory")
            px(bx - 6, by, "ivory")


def draw_lyra(clip: str, frame: int, back: bool, left: bool) -> Image.Image:
    pose = POSES[clip][frame]
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    col, poly, line, px, limb = _tools(d)

    if clip == "defeat" and frame >= 2:
        return draw_collapsed(frame, back, left, im, col, poly, line, px, limb)
    if clip == "rest":
        return draw_seated(back, left, im, col, poly, line, px, limb)

    cx = 33 + pose.lean
    b = pose.breath
    crouch = pose.crouch
    hip_y = 50 + b + crouch
    shoulder_y = 29 + b + crouch
    head_y = 16 + b + pose.head + crouch
    hem_y = 69 + b + pose.cloak + crouch // 2
    sway = pose.cloak

    # ---------------------------------------------------------- back cloak
    poly([(cx - 11, 28 + b + crouch), (cx + 11, 28 + b + crouch), (cx + 14 + sway, 48 + crouch),
          (cx + 15 + sway, hem_y), (cx + 8 + sway, hem_y - 5), (cx + 2 + sway, hem_y + 1),
          (cx - 5 + sway, hem_y - 6), (cx - 12 + sway, hem_y - 1), (cx - 14 + sway, 46 + crouch)],
         "cloak", INK)
    line([(cx - 9, 34 + b + crouch), (cx - 11 + sway, 56), (cx - 12 + sway, hem_y - 3)],
         "cloak_hi", 1)
    line([(cx - 11, 29 + b + crouch), (cx - 14 + sway, 47 + crouch), (cx - 15 + sway, hem_y - 2)],
         "cloak_hi", 1)
    line([(cx + 9, 36 + b + crouch), (cx + 12 + sway, 58), (cx + 13 + sway, hem_y - 6)],
         "cloak_hi", 1)
    poly([(cx + 3 + sway, hem_y - 10), (cx + 11 + sway, hem_y - 3), (cx + 8 + sway, hem_y - 5),
          (cx + 2 + sway, hem_y)], "cloak_sh", None)

    # ---------------------------------------------------------- quiver
    qx = cx + 9
    qy = 20 + b + crouch
    poly([(qx, qy + 7), (qx + 5, qy + 8), (qx + 4, qy + 26), (qx - 1, qy + 25)], "leather", INK)
    line([(qx + 1, qy + 10), (qx + 1, qy + 21)], "leather_lt", 1)
    line([(qx, qy + 12), (qx + 5, qy + 13)], "brass", 1)
    for i, off in enumerate((1, 3)):
        tip_y = qy + 3 - i
        line([(qx + off, qy + 8), (qx + off - 1, tip_y)], "INK", 1)
        px(qx + off - 1, tip_y, "ivory")
        px(qx + off, tip_y + 1, "ivory")

    # ---------------------------------------------------------- legs & boots
    stride = pose.stride
    hip_x = 33                       # feet stay planted under the pivot while the torso leans
    lift_front = pose.lift if clip == "walk" and frame in (1, 2) else 0
    lift_back = pose.lift if clip == "walk" and frame in (5, 6) else 0
    for leg_x, lift, bend in ((hip_x - 4 + stride, lift_front, -stride // 3),
                              (hip_x + 4 - stride, lift_back, stride // 3)):
        ankle_y = 82 - lift
        knee = (leg_x + bend, (hip_y + ankle_y) // 2 + crouch // 2)
        limb((leg_x, hip_y), knee, 4, INK, "trouser", "trouser_lt")
        limb(knee, (leg_x, ankle_y), 4, INK, (47, 43, 34, 255), (68, 62, 48, 255))
        poly([(leg_x - 4, ankle_y - 3), (leg_x + 3, ankle_y - 3), (leg_x + 4, ankle_y + 1),
              (leg_x + 5, 88), (leg_x + 5, 90), (leg_x - 4, 90), (leg_x - 4, ankle_y)],
             "boot", INK)
        line([(leg_x - 3, ankle_y - 2), (leg_x + 2, ankle_y - 2)], "boot_lt", 1)
        for lace_y in range(ankle_y + 1, 87, 3):
            px(leg_x - 1, lace_y, "boot_lt")
            px(leg_x + 1, lace_y + 1, "boot_lt")
        line([(leg_x - 3, 88), (leg_x + 4, 88)], "SOLE_LINE" if False else SOLE, 1)

    # ---------------------------------------------------------- cloak front panels
    poly([(cx - 12, 30 + b + crouch), (cx - 3, 30 + b + crouch), (cx - 6 + sway, 52 + crouch),
          (cx - 11 + sway, hem_y - 2), (cx - 14 + sway, 60 + crouch)], "cloak_lt", INK)
    poly([(cx + 3, 30 + b + crouch), (cx + 12, 30 + b + crouch), (cx + 14 + sway, 58 + crouch),
          (cx + 11 + sway, hem_y - 4), (cx + 6 + sway, 54 + crouch)], "cloak", INK)
    line([(cx - 10, 34 + b + crouch), (cx - 12 + sway, 58 + crouch)], "cloak_hi", 1)
    poly([(cx - 11, 27 + b + crouch), (cx + 11, 27 + b + crouch), (cx + 12, 31 + b + crouch),
          (cx - 12, 31 + b + crouch)], "cloak_sh", INK)

    # ---------------------------------------------------------- torso
    poly([(cx - 8, 28 + b + crouch), (cx + 8, 28 + b + crouch), (cx + 9, 36 + b + crouch),
          (cx + 6, 46 + b + crouch), (cx - 6, 46 + b + crouch), (cx - 9, 36 + b + crouch)],
         "jerkin", INK)
    poly([(cx - 7, 29 + b + crouch), (cx - 1, 30 + b + crouch), (cx - 2, 45 + b + crouch),
          (cx - 6, 45 + b + crouch)], "jerkin_lt", None)
    for row, qy in enumerate(range(31, 45, 4)):
        for qx_ in range(-5, 6, 4):
            ox = 2 if row % 2 else 0
            line([(cx + qx_ + ox, qy + b + crouch), (cx + qx_ + 1 + ox, qy + 2 + b + crouch),
                  (cx + qx_ + ox, qy + 3 + b + crouch)], "jerkin_sh", 1)
    line([(cx - 8, 31 + b + crouch), (cx + 5, 42 + b + crouch)], "leather", 2)
    line([(cx - 8, 31 + b + crouch), (cx + 5, 42 + b + crouch)], "leather_lt", 1)
    poly([(cx - 7, 44 + b + crouch), (cx + 7, 44 + b + crouch), (cx + 6, 48 + b + crouch),
          (cx - 6, 48 + b + crouch)], "sash", INK)
    line([(cx - 5, 45 + b + crouch), (cx + 5, 45 + b + crouch)], "sash_lt", 1)
    poly([(cx + 2, 47 + b + crouch), (cx + 6, 47 + b + crouch), (cx + 5, 58 + b + crouch),
          (cx + 2, 55 + b + crouch)], "sash_sh", None)
    poly([(cx + 2, 47 + b + crouch), (cx + 4, 47 + b + crouch), (cx + 3, 54 + b + crouch)],
         "sash_lt", None)
    poly([(cx - 7, 48 + b + crouch), (cx + 7, 48 + b + crouch), (cx + 7, 51 + b + crouch),
          (cx - 7, 51 + b + crouch)], "leather_dk", INK)
    poly([(cx - 2, 48 + b + crouch), (cx + 2, 48 + b + crouch), (cx + 2, 51 + b + crouch),
          (cx - 2, 51 + b + crouch)], "brass", INK)

    # ---------------------------------------------------------- arms + bow
    # The bow arm rotates around the shoulder: carried low at the hip, then lifted so the
    # bow sits at the silhouette edge with the arrow leaving the body. Keeping the grip
    # outside the torso is what makes the aiming frames readable at 1x.
    bow_y = [48 + b + crouch + pose.arm, 36 + b + crouch, 33 + b + crouch, 27 + b + crouch][pose.bowup]
    bow_x = cx + (-11, 12, 15, 11)[pose.bowup]
    tips = None
    flare = 8
    if pose.bowup == 1:
        tips = ((bow_x + 2, bow_y - 22), (bow_x + 1, bow_y + 21))
        flare = 12
    elif pose.bowup >= 2:
        tips = ((bow_x + 3, bow_y - 24), (bow_x + 2, bow_y + 23))
        flare = 14
    if pose.bowup == 3:
        tips = ((bow_x + 5, bow_y - 16), (bow_x - 4, bow_y + 21))
        flare = 10
    draw_hand = None
    if pose.draw > 0:
        # anchor point walks back to the jaw as the string is pulled
        draw_hand = [(cx + 9, 30 + b), (cx + 7, 28 + b), (cx + 6, 26 + b)][min(pose.draw, 3) - 1]
    if pose.bowup < 1:
        shoulder = (cx - 7, shoulder_y)
        elbow = (cx - 10, (shoulder_y + bow_y) // 2)
    elif pose.bowup == 1:
        shoulder = (cx + 4, shoulder_y)
        elbow = (cx + 8, bow_y + 4)
    else:
        shoulder = (cx + 6, shoulder_y)
        elbow = (cx + 11, shoulder_y + 1)
    limb(shoulder, elbow, 3, INK, "jerkin", "jerkin_lt")
    limb(elbow, (bow_x - 1, bow_y), 3 if pose.bowup > 0 else 3, INK, "leather", "leather_lt")
    # gripping hand: two skin pixels wrap the riser so the grip reads at 1x
    px(bow_x - 1, bow_y, "skin_lt")
    px(bow_x, bow_y + 1, "skin")
    px(bow_x - 1, bow_y + 2, "skin_sh")
    # draw arm: from the far shoulder, forearm pulled to the string
    if draw_hand is not None:
        limb((cx - 5, shoulder_y), (cx + 2, shoulder_y + 6), 3, INK, "jerkin", "jerkin_lt")
        limb((cx + 2, shoulder_y + 6), draw_hand, 2, INK, "leather", "leather_lt")
        px(draw_hand[0], draw_hand[1], "skin_lt")
        px(draw_hand[0] + 1, draw_hand[1], "skin")
        px(draw_hand[0], draw_hand[1] + 1, "skin_sh")
    else:
        limb((cx - 6, shoulder_y), (cx - 11, 38 + b + crouch), 3, INK, "jerkin", "jerkin_lt")
        limb((cx - 11, 38 + b + crouch), (cx - 9, 46 + b + crouch), 2, INK, "leather", "leather_lt")
        px(cx - 9, 47 + b + crouch, "skin")
    # pauldron stays on the far shoulder for every clip: no side-swap pop mid-animation
    poly([(cx - 11, 27 + b + crouch), (cx - 4, 26 + b + crouch), (cx - 3, 32 + b + crouch),
          (cx - 8, 34 + b + crouch), (cx - 12, 31 + b + crouch)], "brass", INK)
    line([(cx - 10, 28 + b + crouch), (cx - 5, 27 + b + crouch)], "brass_hi", 1)

    draw_bow(poly, line, px, bow_x, bow_y, pose.bowtilt, draw_hand, pose.arrow, tips, flare)

    # ---------------------------------------------------------- head & hair
    hx0 = cx + pose.head
    poly([(hx0 - 2, head_y + 6), (hx0 + 2, head_y + 6), (hx0 + 3, head_y + 12),
          (hx0 - 3, head_y + 12)], "skin_sh", None)
    if not back:
        poly([(hx0 - 5, head_y - 4), (hx0 + 4, head_y - 5), (hx0 + 6, head_y + 1),
              (hx0 + 6, head_y + 5), (hx0 + 1, head_y + 9), (hx0 - 4, head_y + 8),
              (hx0 - 6, head_y + 2)], "skin", INK)
        poly([(hx0 - 3, head_y - 3), (hx0 + 3, head_y - 3), (hx0 + 4, head_y + 2),
              (hx0 + 1, head_y + 6), (hx0 - 3, head_y + 4)], "skin_lt", None)
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
            line([(bx, head_y + 1), (bx + side + braid, head_y + 9), (bx + braid, head_y + 14)],
                 "hair_sh", 3)
            line([(bx + side, head_y + 2), (bx + side + braid, head_y + 8)], "hair", 2)
        line([(hx0 - 3, head_y + 1), (hx0 - 1, head_y + 1)], "hair_sh", 1)
        line([(hx0 + 2, head_y), (hx0 + 5, head_y)], "hair_sh", 1)
        if pose.blink:
            line([(hx0 - 3, head_y + 2), (hx0 - 1, head_y + 2)], "skin_sh", 1)
            line([(hx0 + 2, head_y + 2), (hx0 + 4, head_y + 2)], "skin_sh", 1)
        else:
            line([(hx0 - 3, head_y + 2), (hx0 - 1, head_y + 2)], GLINT, 1)
            line([(hx0 + 2, head_y + 2), (hx0 + 4, head_y + 2)], GLINT, 1)
            px(hx0 - 2, head_y + 2, EYE)
            px(hx0 + 3, head_y + 2, EYE)
        px(hx0 + 1, head_y + 5, "sash_sh")
        px(hx0 - 4, head_y + 3, "skin_sh")
        px(hx0 + 5, head_y + 5, "skin_sh")
    else:
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

    return finish(im, left)


def finish(im, left):
    im = ground_clip(im)
    if left:
        im = relight_left(im)
    return im


def draw_collapsed(frame, back, left, im, col, poly, line, px, limb):
    """Kneel -> buckle -> down. Newly drawn body, never a sliding corpse."""
    d = ImageDraw.Draw(im)
    cx = 32
    stage = frame - 2
    hip_y = [58, 68, 76, 82][stage]
    head = [(30, 46), (24, 58), (18, 68), (16, 74)][stage]
    knee = [(30, 74), (26, 80), (22, 84), (20, 86)][stage]
    # cloak spread on the ground
    poly([(cx - 14, hip_y - 8), (cx + 12, hip_y - 10), (cx + 20, hip_y + 8),
          (cx + 24, 88), (cx + 6, 90), (cx - 12, 89), (cx - 20, hip_y + 6)], "cloak", INK)
    poly([(cx - 12, hip_y - 6), (cx + 4, hip_y - 7), (cx + 10, hip_y + 4), (cx + 6, 86),
          (cx - 8, 86), (cx - 14, hip_y + 4)], "cloak_lt", None)
    # legs folded under the body
    limb((cx + 2, hip_y), (knee[0] + 10, knee[1]), 5, INK, "trouser", "trouser_lt")
    limb((knee[0] + 10, knee[1]), (44, 88), 4, INK, (47, 43, 34, 255), (68, 62, 48, 255))
    poly([(40, 84), (47, 84), (48, 88), (49, 90), (38, 90), (38, 86)], "boot", INK)
    # torso slumped over the knee
    poly([(head[0] + 2, head[1] + 6), (cx + 4, hip_y - 12), (cx + 10, hip_y - 2),
          (cx + 2, hip_y + 2), (head[0] - 4, head[1] + 14)], "jerkin", INK)
    line([(head[0] + 3, head[1] + 9), (cx + 6, hip_y - 8)], "sash", 3)
    # head down, hair falling forward
    poly([(head[0] - 5, head[1] - 4), (head[0] + 4, head[1] - 5), (head[0] + 6, head[1] + 2),
          (head[0] + 2, head[1] + 7), (head[0] - 4, head[1] + 6)], "skin", INK)
    poly([(head[0] - 7, head[1]), (head[0] - 6, head[1] - 7), (head[0], head[1] - 9),
          (head[0] + 6, head[1] - 6), (head[0] + 3, head[1] - 2), (head[0] - 3, head[1] - 3),
          (head[0] - 6, head[1] + 3)], "hair", INK)
    line([(head[0] - 4, head[1] - 5), (head[0] + 2, head[1] - 7)], "hair_lt", 1)
    for side in (-1, 1):
        line([(head[0] + side * 5, head[1] + 2), (head[0] + side * 4, head[1] + 12)],
             "hair_sh", 2)
    if not back:
        line([(head[0] - 2, head[1] + 1), (head[0], head[1] + 1)], "skin_sh", 1)
        line([(head[0] + 2, head[1] + 1), (head[0] + 4, head[1] + 1)], "skin_sh", 1)
    # bow dropped beside the body
    upper = (cx - 16, hip_y - 4)
    lower = (cx + 10, 86)
    line([upper, (cx - 6, hip_y + 12), lower], "INK", 3)
    line([upper, (cx - 6, hip_y + 12), lower], "bow", 1)
    px(upper[0], upper[1], "brass")
    px(lower[0], lower[1], "brass")
    return finish(im, left)


def draw_seated(back, left, im, col, poly, line, px, limb):
    """Rest frame: sitting against the wall, knees up, bow across the lap."""
    d = ImageDraw.Draw(im)
    cx = 33
    # cloak pooled around the hips
    poly([(cx - 13, 56), (cx + 13, 56), (cx + 16, 76), (cx + 8, 84), (cx - 10, 84),
          (cx - 16, 74)], "cloak", INK)
    poly([(cx - 11, 58), (cx + 2, 58), (cx + 4, 80), (cx - 9, 80), (cx - 13, 70)],
         "cloak_lt", None)
    # folded legs and boots flat on the ground
    limb((cx - 6, 62), (cx - 12, 78), 4, INK, "trouser", "trouser_lt")
    limb((cx - 12, 78), (cx - 6, 86), 4, INK, "trouser", "trouser_lt")
    poly([(cx - 12, 84), (cx - 2, 84), (cx - 1, 90), (cx - 13, 90)], "boot", INK)
    limb((cx + 5, 62), (cx + 13, 76), 4, INK, (47, 43, 34, 255), (68, 62, 48, 255))
    poly([(cx + 9, 80), (cx + 18, 84), (cx + 18, 88), (cx + 7, 88)], "boot", INK)
    # torso upright, head down a little
    poly([(cx - 8, 34), (cx + 8, 34), (cx + 7, 54), (cx - 7, 54)], "jerkin", INK)
    poly([(cx - 7, 35), (cx - 1, 36), (cx - 2, 53), (cx - 6, 53)], "jerkin_lt", None)
    for row, qy in enumerate(range(38, 52, 4)):
        for qx_ in range(-4, 5, 4):
            ox = 2 if row % 2 else 0
            line([(cx + qx_ + ox, qy), (cx + qx_ + 1 + ox, qy + 2), (cx + qx_ + ox, qy + 3)],
                 "jerkin_sh", 1)
    poly([(cx - 7, 50), (cx + 7, 50), (cx + 6, 54), (cx - 6, 54)], "sash", INK)
    # arms resting on the knees
    limb((cx - 7, 36), (cx - 12, 50), 3, INK, "jerkin", "jerkin_lt")
    limb((cx - 12, 50), (cx - 10, 60), 2, INK, "leather", "leather_lt")
    limb((cx + 7, 36), (cx + 12, 50), 3, INK, "jerkin", "jerkin_lt")
    poly([(cx - 11, 34), (cx - 4, 33), (cx - 3, 39), (cx - 8, 41), (cx - 12, 38)],
         "brass", INK)
    # head
    hx0 = cx
    head_y = 22
    poly([(hx0 - 2, head_y + 6), (hx0 + 2, head_y + 6), (hx0 + 3, head_y + 12),
          (hx0 - 3, head_y + 12)], "skin_sh", None)
    if not back:
        poly([(hx0 - 5, head_y - 4), (hx0 + 4, head_y - 5), (hx0 + 6, head_y + 1),
              (hx0 + 6, head_y + 5), (hx0 + 1, head_y + 9), (hx0 - 4, head_y + 8),
              (hx0 - 6, head_y + 2)], "skin", INK)
        poly([(hx0 - 7, head_y + 2), (hx0 - 6, head_y - 7), (hx0, head_y - 10),
              (hx0 + 6, head_y - 7), (hx0 + 7, head_y + 1), (hx0 + 4, head_y - 2),
              (hx0, head_y - 3), (hx0 - 4, head_y - 1), (hx0 - 5, head_y + 3)], "hair", INK)
        poly([(hx0 - 5, head_y - 6), (hx0 + 1, head_y - 9), (hx0 + 5, head_y - 6),
              (hx0 + 1, head_y - 4)], "hair_lt", None)
        line([(hx0 - 3, head_y + 2), (hx0 - 1, head_y + 2)], "skin_sh", 1)
        line([(hx0 + 2, head_y + 2), (hx0 + 4, head_y + 2)], "skin_sh", 1)
        for side in (-1, 1):
            bx = hx0 + side * 6
            line([(bx, head_y + 1), (bx + side, head_y + 9), (bx, head_y + 14)],
                 "hair_sh", 3)
    else:
        poly([(hx0 - 8, head_y + 3), (hx0 - 7, head_y - 4), (hx0 - 2, head_y - 7),
              (hx0 + 5, head_y - 6), (hx0 + 8, head_y), (hx0 + 7, head_y + 8),
              (hx0 + 3, head_y + 12), (hx0 - 3, head_y + 11), (hx0 - 7, head_y + 7)],
             "hair", INK)
        poly([(hx0 - 5, head_y - 4), (hx0 + 2, head_y - 6), (hx0 - 1, head_y - 1)],
             "hair_lt", None)
    # bow leaning across the lap
    line([(cx - 14, 54), (cx - 4, 68), (cx + 10, 76)], "INK", 3)
    line([(cx - 14, 54), (cx - 4, 68), (cx + 10, 76)], "bow", 1)
    px(cx - 14, 54, "brass")
    px(cx + 10, 76, "brass")
    return finish(im, left)


def relight_left(im: Image.Image) -> Image.Image:
    """Mirror the pose, then reramp glints back toward the fixed upper-left key light."""
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
                px[x, y] = PAL["hair_lt"] if c == PAL["hair_hi"] else PAL["brass_lt"]
            elif c in soft:
                px[x, y] = soft[c]
    return im


def ground_clip(im: Image.Image) -> Image.Image:
    """Nothing may render below the ground anchor row. Collapsed poses (defeat) and wide
    outlines can spill past PIVOT[1]; clipping there is what the engine expects (feet and
    bodies are anchored at row 90), and it keeps the pivot assertion structural."""
    px = im.load()
    for y in range(PIVOT[1] + 1, H):
        for x in range(W):
            if px[x, y][3]:
                px[x, y] = (0, 0, 0, 0)
    return im


def qa_frame(im: Image.Image, name: str):
    alpha = im.getchannel("A")
    bbox = alpha.getbbox()
    assert bbox is not None, f"{name}: empty frame"
    assert bbox[0] >= 1 and bbox[1] >= 1 and bbox[2] <= W - 1 and bbox[3] <= H - 1, \
        f"{name}: art touches the frame edge {bbox}"
    assert bbox[3] - 1 == PIVOT[1], f"{name}: baseline {bbox[3]-1} != pivot row {PIVOT[1]}"
    assert set(alpha.getdata()) <= {0, 255}, f"{name}: non-binary alpha"
    return {"bbox": list(bbox), "colors": len(set(im.getdata()) - {(0, 0, 0, 0)})}


def build():
    columns = [(clip, i) for clip, times in CLIPS.items() for i in range(len(times))]
    sheet = Image.new("RGBA", (W * len(columns), H * len(DIRECTIONS)), (0, 0, 0, 0))
    per_direction = {}
    for row, (name, back, left) in enumerate(DIRECTIONS):
        seen = {}
        stats = {}
        for column, (clip, frame) in enumerate(columns):
            im = draw_lyra(clip, frame, back, left)
            digest = hashlib.sha256(im.tobytes()).hexdigest()
            assert digest not in seen, f"duplicate frame {name} {clip} {frame} (also {seen.get(digest)})"
            seen[digest] = f"{clip}[{frame}]"
            sheet.paste(im, (column * W, row * H))
            report = qa_frame(im, f"{name}/{clip}[{frame}]")
            if frame == 0:
                stats[clip] = report
            elif report["bbox"][3] - 1 != PIVOT[1]:
                raise AssertionError(f"{name}/{clip}[{frame}] baseline drift {report['bbox']}")
        per_direction[name] = stats
        row_sheet = sheet.crop((0, row * H, W * len(columns), (row + 1) * H))
        row_sheet.save(PACK / f"lyra_{name}_all_clips.png")
    sheet.save(PACK / ATLAS_NAME)
    sheet.save(ASSETS / ATLAS_NAME)

    # Runtime manifest entry, SHA-256 kept exact so tools/validate_assets.py passes.
    digest = hashlib.sha256((ASSETS / ATLAS_NAME).read_bytes()).hexdigest()
    manifest_path = ASSETS / "manifest.json"
    manifest = json.loads(manifest_path.read_text())
    manifest.setdefault("assets", {})["lyra"] = {
        "file": ATLAS_NAME,
        "size": [sheet.width, sheet.height],
        "sha256": digest,
    }
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")

    starts = {}
    _col = 0
    for clip, times in CLIPS.items():
        starts[clip] = _col
        _col += len(times)

    # Markers are derived from the authored poses, never typed by hand, so they cannot
    # drift away from the frames that actually contain the release / contact / impact.
    def release_frame(clip):
        drawn = False
        for i, pose in enumerate(POSES[clip]):
            if pose.draw > 0:
                drawn = True
            elif drawn:
                return i
        return 0

    markers = {
        "attack_release_column": starts["attack"] + release_frame("attack"),
        "skill_release_column": starts["skill"] + release_frame("skill"),
        "skill_charge_columns": [starts["skill"] + i for i, p in enumerate(POSES["skill"])
                                 if p.arrow == 2 and p.draw > 0],
        "hit_reaction_column": starts["hit"] + max(
            range(len(POSES["hit"])), key=lambda i: POSES["hit"][i].crouch),
        "walk_contact_columns": [starts["walk"] + i for i, p in enumerate(POSES["walk"])
                                 if p.stride == 0],
        "defeat_start_column": starts["defeat"],
        "notes": "Column = atlas column index. Attack contact is the frame after the string "
                 "clears (arrow spawn); skill release is the magic projectile spawn; hit "
                 "reaction is the deepest impact frame; walk contacts are the zero-stride "
                 "frames. Data only — the runtime does not consume these yet.",
    }

    pack_manifest = {
        "asset_id": "lyra_4dir_all_clips",
        "category": "hero_world_sprite",
        "file_path": "app/src/main/assets/premium_slice/lyra.png",
        "character": "Lyra (hero_starter_lyra) — Archer, 'Mắt Ưng Gió Đêm'",
        "origin": "Original ETERNAL SUMMONER art; authored cluster recipes in tools/generate_lyra_sprites.py",
        "source_or_license": "Original in-repo artwork authored from pixel recipes; no external or copied art",
        "frame": [W, H],
        "frame_count_per_row": len(columns),
        "total_frames": len(columns) * len(DIRECTIONS),
        "pivot": list(PIVOT),
        "foot_anchor": list(PIVOT),
        "world_pixel_scale": 1.08,
        "palette_tag": "dark_fantasy_moss_oxblood",
        "atlas": {"file": ATLAS_NAME, "size": [sheet.width, sheet.height], "pitch": [W, H],
                  "sha256": digest},
        "clip_order": list(CLIPS.keys()),
        "clips": CLIPS,
        "clip_columns": {},
        "direction_rows": {name: row for row, (name, _b, _l) in enumerate(DIRECTIONS)},
        "direction_mapping": {
            "row 0 front_right": "camera-side 3/4 facing screen-right",
            "row 1 front_left": "camera-side 3/4 facing screen-left",
            "row 2 back_right": "back view moving away, right",
            "row 3 back_left": "back view moving away, left",
        },
        "row_rule": "row = (north ? 2 : 0) + (right ? 0 : 1) — PremiumActorAtlas.frame()",
        "vfx_markers": markers,
        "quality": per_direction,
    }
    for clip, times in CLIPS.items():
        pack_manifest["clip_columns"][clip] = {"start_column": starts[clip],
                                               "frames": len(times)}

    (PACK / "lyra_pack_manifest.json").write_text(
        json.dumps(pack_manifest, indent=2, ensure_ascii=False))
    return pack_manifest


def contact_sheets():
    atlas = Image.open(ASSETS / ATLAS_NAME)
    cols = atlas.width // W
    # 3x review grid with pivot line and clip boundaries
    scale = 3
    sheet = Image.new("RGBA", (atlas.width * scale, atlas.height * scale), (18, 26, 24, 255))
    sheet.alpha_composite(atlas.resize((atlas.width * scale, atlas.height * scale), Image.NEAREST))
    d = ImageDraw.Draw(sheet)
    for row in range(len(DIRECTIONS)):
        y = (row * H + PIVOT[1]) * scale
        d.line([(0, y), (sheet.width, y)], fill=(255, 214, 120, 60), width=1)
        d.text((4, row * H * scale + 4), DIRECTIONS[row][0], fill=(240, 230, 200, 255))
    col = 0
    for clip, times in CLIPS.items():
        x = col * W * scale
        d.line([(x, 0), (x, sheet.height)], fill=(120, 200, 255, 140), width=2)
        d.text((x + 3, sheet.height - 14), clip, fill=(180, 220, 255, 255))
        col += len(times)
    sheet.save(PACK / "lyra_contact_sheet_3x.png")

    # per-clip sheets at 3x, one file per clip (4 direction rows)
    col = 0
    for clip, times in CLIPS.items():
        part = atlas.crop((col * W, 0, (col + len(times)) * W, atlas.height))
        big = part.resize((part.width * 3, part.height * 3), Image.NEAREST)
        canvas = Image.new("RGBA", (big.width, big.height + 16), (18, 26, 24, 255))
        canvas.alpha_composite(big, (0, 16))
        dd = ImageDraw.Draw(canvas)
        dd.text((4, 2), f"Lyra — {clip} ({len(times)} frames x 4 directions)", fill=(240, 230, 200, 255))
        canvas.save(PACK / f"lyra_clip_{clip}_3x.png")
        col += len(times)

    # 1x gameplay-size preview of every clip, idle column repeated for readability
    px_per_frame = 20
    tiny = Image.new("RGBA", (16 + px_per_frame * 16, 8 + 24 * len(DIRECTIONS)), (24, 34, 28, 255))
    col = 0
    for clip, times in CLIPS.items():
        for row in range(len(DIRECTIONS)):
            for i, f in enumerate(range(len(times))):
                src = atlas.crop(((col + f) * W, row * H, (col + f + 1) * W, (row + 1) * H))
                src = src.resize((round(W * 0.95), round(H * 0.95)), Image.NEAREST)
                tiny.alpha_composite(src, (8 + (f * 1 + 0) * 2 + 0, 6 + row * 22))
        col += len(times)
    tiny.save(PACK / "lyra_actual_size_preview.png")


if __name__ == "__main__":
    m = build()
    contact_sheets()
    print(json.dumps({"atlas": m["atlas"], "frames": m["total_frames"],
                      "clip_columns": m["clip_columns"]}, indent=2))
    print(f"wrote {ASSETS / ATLAS_NAME} and {PACK}")
