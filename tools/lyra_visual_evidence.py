#!/usr/bin/env python3
"""Build the P17-B visual evidence from the shipped Lyra atlas + the real Kaelen atlas.

Everything here is derived from the actual assets (no redraws, no upscaled AI art):
  * lyra_contact_sheet_3x.png      full atlas at 3x with the pivot row and clip dividers
  * lyra_1x_actual_size.png        frames at their real on-screen pixel size
  * lyra_vs_kaelen_scene_scale.png Lyra and Kaelen as the engine draws them (1.08 px scale)
  * lyra_ingame_mockup.png         composite over a real P1.5A engine capture (labelled mockup)

Run: python3 tools/lyra_visual_evidence.py
"""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets/premium_slice"
PACK = ROOT / "art/p16_lyra/pack"
EVID = ROOT / "docs/visual-evidence/p17"
EVID.mkdir(parents=True, exist_ok=True)

W, H = 64, 96
PIVOT = (32, 90)
SCALE = 1.08          # PremiumActorAtlas.worldPixelScale
BG = (26, 34, 29, 255)
INKC = (232, 226, 206, 255)


def load_manifest() -> dict:
    return json.loads((ASSETS / "manifest.json").read_text())


def columns(manifest: dict) -> dict:
    start, out = 0, {}
    for clip in manifest["clip_order"]:
        n = len(manifest["clips"][clip])
        out[clip] = (start, n)
        start += n
    return out


def frame(sheet: Image.Image, manifest: dict, clip: str, index: int, row: int) -> Image.Image:
    start, n = columns(manifest)[clip]
    col = start + index % n
    return sheet.crop((col * W, row * H, (col + 1) * W, (row + 1) * H))


def scaled(im: Image.Image, scale: float) -> Image.Image:
    return im.resize((round(im.width * scale), round(im.height * scale)), Image.NEAREST)


def label(d: ImageDraw.ImageDraw, xy, text, color=INKC):
    d.text(xy, text, fill=color)


def actual_size(lyra: Image.Image, manifest: dict):
    """1x: exactly the pixel size the engine blits at zoom = 1."""
    rows = [("idle", 6), ("walk", 8), ("attack", 8), ("skill", 9), ("hit", 4), ("victory", 6)]
    gap, pad, head = 6, 12, 22
    width = pad * 2 + max(n * (W + gap) for _, n in rows) + 70
    height = head + len(rows) * (H + gap) + pad
    out = Image.new("RGBA", (width, height), BG)
    d = ImageDraw.Draw(out)
    label(d, (pad, 4), "Lyra — 1x actual size (64x96 frame, engine worldPixelScale 1.08 at zoom 1.0)")
    for r, (clip, n) in enumerate(rows):
        y = head + r * (H + gap)
        label(d, (pad, y + H // 2), clip, (200, 214, 190, 255))
        x0 = pad + 70
        d.line([(x0, y + PIVOT[1]), (x0 + n * (W + gap) - gap, y + PIVOT[1])], fill=(120, 200, 255, 60))
        for i in range(n):
            out.alpha_composite(frame(lyra, manifest, clip, i, 0), (x0 + i * (W + gap), y))
    out.save(EVID / "lyra_1x_actual_size.png")
    return out


def vs_kaelen(lyra: Image.Image, kaelen: Image.Image, manifest: dict):
    """Both heroes at the exact engine pixel scale, feet on one shared ground line."""
    poses = [("idle", 0), ("walk", 2), ("attack", 4), ("skill", 4), ("hit", 1), ("victory", 2)]
    cell_w = round(W * SCALE) + 18
    head, foot_pad = 30, 16
    row_h = round(H * SCALE) + 8
    out = Image.new("RGBA", (cell_w * len(poses) + 190, head + 2 * row_h + foot_pad), BG)
    d = ImageDraw.Draw(out)
    label(d, (10, 6), "Scene scale: engine draw path (64x96 * worldPixelScale 1.08), nearest filter")
    for c, (clip, _i) in enumerate(poses):
        label(d, (90 + c * cell_w + 2, head - 13), clip, (190, 210, 235, 255))
    for r, (name, sheet) in enumerate((("Lyra", lyra), ("Kaelen", kaelen))):
        y_off = head + r * row_h
        ground = y_off + round(PIVOT[1] * SCALE)
        d.line([(0, ground), (out.width, ground)], fill=(150, 190, 150, 90))
        label(d, (10, y_off + round(H * SCALE) // 2), name, (230, 220, 190, 255))
        for c, (clip, idx) in enumerate(poses):
            im = scaled(frame(sheet, manifest, clip, idx, 0), SCALE)
            out.alpha_composite(im, (90 + c * cell_w, y_off))
    out.save(EVID / "lyra_vs_kaelen_scene_scale.png")
    return out


def mockup(lyra: Image.Image, kaelen: Image.Image, manifest: dict):
    """Composite on a real P1.5A engine capture. This is a mockup and says so on the image."""
    base_path = ROOT / "docs/visual-evidence/p15a/after/sample_normal.png"
    if not base_path.exists():
        return None
    base = Image.open(base_path).convert("RGBA")
    d = ImageDraw.Draw(base)
    zoom = 1.0
    scale = SCALE * zoom
    slots = [("Lyra", lyra, "attack", 4, 0.42), ("Lyra", lyra, "idle", 0, 0.55),
             ("Kaelen", kaelen, "idle", 0, 0.68)]
    for name, sheet, clip, idx, fx in slots:
        im = scaled(frame(sheet, manifest, clip, idx, 0), scale)
        x = round(base.width * fx)
        y = round(base.height * 0.62) - round(PIVOT[1] * scale)
        base.alpha_composite(im, (x, y))
        d.text((x + 4, y + round(PIVOT[1] * scale) + 4), name, fill=(240, 235, 220, 255))
    banner = ("MOCKUP - NOT ENGINE RENDER  |  sprites pasted on a real P1.5A capture at "
              "worldPixelScale 1.08; no runtime frame was produced")
    d.rectangle([0, base.height - 26, base.width, base.height], fill=(12, 14, 18, 235))
    d.text((10, base.height - 19), banner, fill=(255, 210, 120, 255))
    d.rectangle([0, 0, base.width, 22], fill=(12, 14, 18, 210))
    d.text((10, 6), "MOCKUP - NOT ENGINE RENDER", fill=(255, 150, 120, 255))
    base.save(EVID / "lyra_ingame_mockup.png")
    return base


def main():
    manifest = load_manifest()
    lyra = Image.open(ASSETS / "lyra.png").convert("RGBA")
    kaelen = Image.open(ASSETS / "kaelen.png").convert("RGBA")

    sheet = Image.open(PACK / "lyra_contact_sheet_3x.png")
    sheet.save(EVID / "lyra_contact_sheet_3x.png")
    actual_size(lyra, manifest)
    vs_kaelen(lyra, kaelen, manifest)
    mockup(lyra, kaelen, manifest)
    print("evidence written to", EVID.relative_to(ROOT))


if __name__ == "__main__":
    main()
