#!/usr/bin/env python3
"""P17-C — Lyra HUD portrait (original pixel-art bust, 64x64 native).

The HUD avatar used to crop a 26x26 patch out of Kaelen's world atlas and stretch it.
This authors a dedicated bust at 64x64 native pixels — the size a 48 dp avatar needs on a
2x-3x density screen without upscaling a tiny crop — using the same palette and light
direction as the world sprite so the card and the character read as the same person.

Run: python3 tools/generate_lyra_portrait.py
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw

from generate_lyra_sprites import INK, PAL, qbez

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets/premium_slice"
PACK = ROOT / "art/p16_lyra/pack"
EVID = ROOT / "docs/visual-evidence/p17c"
for d in (ASSETS / "portraits", PACK, EVID):
    d.mkdir(parents=True, exist_ok=True)

S = 64
GLINT = (236, 228, 200, 255)
EYE = (140, 86, 38, 255)
SCLERA = (246, 240, 224, 255)
IRIS = (74, 42, 22, 255)
PUPIL_HI = (255, 252, 244, 255)


def col(c):
    if c is None:
        return None
    if c == "INK":
        return INK
    return PAL.get(c, c) if isinstance(c, str) else c


def build() -> Image.Image:
    """64x64 bust: 3/4-lit head, braids, collar, brass pauldron, bow stave at the edge.
    Staged so the silhouette reads at 32-48 dp: face > hair > cloak > metal."""
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)

    def poly(points, color, outline=INK):
        d.polygon([(round(x), round(y)) for x, y in points], fill=col(color), outline=col(outline))

    def line(points, color, width=1):
        d.line([(round(x), round(y)) for x, y in points], fill=col(color), width=width)

    def px(x, y, color):
        d.point((round(x), round(y)), fill=col(color))

    # ---------------------------------------------------------------- bow stave (behind)
    stave = qbez((9, 63), (0, 38), (10, 7), 22)
    line(stave, "INK", 4)
    line(stave, "bow", 2)
    line([(x + 1, y) for x, y in stave[3:-3]], "bow_lt", 1)
    for tipx, tipy in (stave[0], stave[-1]):
        px(tipx, tipy, "brass")
    grip = stave[len(stave) // 2]
    line([(grip[0] - 1, grip[1] - 2), (grip[0] + 1, grip[1] + 2)], "brass_lt", 2)
    line([stave[0], stave[-1]], "string_dim", 1)

    # ---------------------------------------------------------------- shoulders / cloak
    poly([(4, 64), (8, 52), (18, 46), (46, 46), (56, 52), (60, 64)], "cloak", INK)
    poly([(7, 64), (11, 54), (22, 49), (31, 56), (27, 64)], "cloak_lt", None)
    poly([(37, 49), (47, 47), (55, 53), (57, 64), (39, 64)], "cloak_sh", None)
    line([(12, 55), (24, 50)], "cloak_hi", 1)
    # jerkin chest under the cloak
    poly([(19, 47), (45, 47), (43, 64), (21, 64)], "jerkin", INK)
    poly([(21, 48), (31, 48), (30, 63), (22, 63)], "jerkin_lt", None)
    for row, qy in enumerate(range(50, 63, 4)):
        for qx in range(23, 43, 5):
            ox = 2 if row % 2 else 0
            line([(qx + ox, qy), (qx + 1 + ox, qy + 2), (qx + ox, qy + 3)], "jerkin_sh", 1)
    # quiver strap
    line([(19, 47), (44, 61)], "leather", 3)
    line([(19, 47), (44, 61)], "leather_lt", 1)
    # oxblood sash + knot
    poly([(19, 58), (45, 58), (44, 63), (20, 63)], "sash", INK)
    line([(22, 59), (42, 59)], "sash_lt", 1)
    poly([(40, 60), (44, 60), (43, 64), (39, 64)], "sash_sh", None)

    # ---------------------------------------------------------------- collar + pauldron
    poly([(15, 43), (25, 47), (23, 55), (13, 51)], "cloak_lt", INK)
    line([(16, 45), (24, 49)], "cloak_hi", 1)
    poly([(41, 47), (49, 43), (51, 51), (43, 55)], "cloak", INK)
    poly([(6, 47), (16, 44), (19, 52), (10, 56), (4, 53)], "brass", INK)
    line([(8, 49), (15, 46)], "brass_hi", 1)
    line([(7, 53), (15, 51)], "brass_lt", 1)

    # ---------------------------------------------------------------- neck
    poly([(28, 34), (37, 34), (36, 48), (29, 48)], "skin_sh", INK)
    poly([(29, 36), (35, 36), (34, 46), (30, 46)], "skin", None)
    line([(30, 37), (34, 37)], "skin_sh", 1)      # chin shadow on the neck
    line([(30, 44), (34, 44)], "skin_sh", 1)

    # ---------------------------------------------------------------- head
    poly([(20, 14), (24, 8), (32, 6), (41, 8), (45, 14), (46, 24), (43, 33), (37, 38),
          (29, 38), (23, 33), (20, 25)], "skin", INK)
    poly([(22, 14), (27, 9), (34, 8), (40, 10), (43, 16), (43, 24), (40, 31), (35, 34),
          (30, 34), (25, 30), (22, 24)], "skin_lt", None)
    # jaw + cheek shading on the shadow side
    poly([(23, 30), (27, 36), (33, 38), (38, 35), (34, 37), (27, 35)], "skin_sh", None)
    line([(40, 16), (42, 26)], "skin_sh", 2)
    px(22, 26, "skin_sh")

    # eyes: socket shadow, pale sclera, dark iris with a catchlight, brow above.
    # At 48 dp only 3x2 px of this survives, so the value contrast has to be explicit.
    for ex in (24, 34):
        line([(ex - 1, 21), (ex + 4, 20)], "hair_sh", 2)      # brow
        line([(ex - 1, 23), (ex + 4, 23)], "skin_sh", 1)      # lid crease
        d.rectangle([ex, 24, ex + 3, 25], fill=SCLERA)        # eye white
        d.rectangle([ex + 1, 24, ex + 2, 25], fill=IRIS)      # iris
        px(ex + 1, 24, PUPIL_HI)                              # catchlight
        line([(ex - 1, 26), (ex + 4, 26)], "skin_sh", 1)      # lower lid
    # nose bridge + nostril, mouth, chin
    line([(30, 26), (29, 30)], "skin_sh", 1)
    px(28, 30, "skin_sh")
    line([(26, 33), (31, 33)], "sash_sh", 1)
    px(27, 34, "skin_sh")
    px(31, 34, "skin_sh")
    line([(27, 34), (30, 34)], "skin_lt", 1)

    # ---------------------------------------------------------------- hair
    poly([(17, 28), (15, 16), (21, 7), (32, 3), (43, 6), (49, 15), (50, 26), (46, 20),
          (44, 12), (33, 8), (23, 10), (18, 17), (17, 26)], "hair", INK)
    poly([(19, 20), (23, 10), (32, 5), (41, 8), (46, 15), (46, 21), (39, 12), (29, 11),
          (21, 16)], "hair_lt", None)
    line([(21, 16), (27, 9), (35, 7)], "hair_hi", 1)
    # fringe: three clumps breaking the hairline so the forehead is not a blank dome
    poly([(21, 13), (27, 12), (24, 20), (20, 18)], "hair", INK)
    poly([(27, 12), (34, 11), (33, 19), (26, 19)], "hair_lt", INK)
    poly([(34, 11), (41, 13), (42, 20), (35, 18)], "hair", INK)
    line([(28, 13), (30, 18)], "hair_sh", 1)
    # side locks framing the jaw
    poly([(16, 20), (21, 17), (21, 31), (18, 40), (14, 33), (14, 25)], "hair", INK)
    poly([(45, 18), (50, 23), (50, 33), (46, 41), (43, 32), (44, 23)], "hair_sh", INK)
    line([(17, 22), (17, 34)], "hair_lt", 1)
    line([(47, 24), (47, 34)], "hair", 1)
    # braids over the shoulders, darker at the shadow side
    for bx, dirx, tone in ((13, -1, "hair"), (50, 1, "hair_sh")):
        pts = [(bx, 33), (bx + dirx * 2, 41), (bx + dirx, 49), (bx + dirx * 2, 57)]
        line(pts, "hair_sh", 3)
        line([(p[0] + dirx, p[1]) for p in pts], tone, 2)
        px(pts[-1][0] + dirx, pts[-1][1] + 1, "ivory")

    # upper-left key rim on hair and shoulder
    line([(20, 9), (17, 17)], "hair_hi", 1)
    line([(10, 55), (13, 52)], "cloak_hi", 1)
    return im


def main():
    im = build()
    assert im.size == (S, S) and im.mode == "RGBA"
    alpha = im.getchannel("A")
    assert set(alpha.getdata()) <= {0, 255}, "binary alpha only"
    bbox = alpha.getbbox()
    assert bbox is not None and bbox[0] >= 0 and bbox[1] >= 1 and bbox[2] <= S and bbox[3] == S, bbox

    out = ASSETS / "portraits/lyra_portrait.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    im.save(out)
    digest = hashlib.sha256(out.read_bytes()).hexdigest()

    manifest_path = ASSETS / "manifest.json"
    manifest = json.loads(manifest_path.read_text())
    manifest.setdefault("portraits", {})["lyra"] = {
        "file": "portraits/lyra_portrait.png",
        "size": [S, S],
        "sha256": digest,
        "native_px": S,
        "usage": "HUD hero avatar; drawn at 1:1 up to 64 dp, nearest filter, alpha over the UI gradient",
    }
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")

    im.save(PACK / "lyra_portrait_64.png")
    big = im.resize((S * 6, S * 6), Image.NEAREST)
    canvas = Image.new("RGBA", (big.width + 24, big.height + 24), (22, 30, 26, 255))
    canvas.alpha_composite(big, (12, 12))
    canvas.save(EVID / "lyra_portrait_6x.png")

    # 1x + 48dp card mock (art preview, not an app screenshot)
    hud = Image.new("RGBA", (300, 110), (18, 24, 20, 255))
    dd = ImageDraw.Draw(hud)
    for i, size in enumerate((48, 40, 32)):
        x = 12 + i * 96
        dd.rounded_rectangle([x, 26, x + size, 26 + size], radius=9,
                             fill=(27, 67, 50, 255), outline=(212, 175, 55, 230), width=2)
        art = im.resize((round(size * 0.9), round(size * 0.9)), Image.NEAREST)
        hud.alpha_composite(art, (x + (size - art.width) // 2, 26 + (size - art.height) // 2))
        dd.text((x, 12), f"{size}dp", fill=(228, 222, 200, 255))
        dd.text((x, 80), "Lyra", fill=(240, 235, 215, 255))
        dd.text((x, 92), "ARCHER · 3*", fill=(200, 190, 160, 255))
    dd.text((12, 96), "PORTRAIT PREVIEW (art asset at 1x, not a runtime screenshot)",
            fill=(255, 200, 120, 255))
    hud.save(EVID / "lyra_portrait_hud_preview.png")

    print(json.dumps({"portrait": str(out.relative_to(ROOT)), "size": [S, S],
                      "sha256": digest, "bbox": list(bbox)}, indent=2))


if __name__ == "__main__":
    main()
