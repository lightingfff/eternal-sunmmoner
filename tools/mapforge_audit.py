#!/usr/bin/env python3
"""MAP FORGE — Pass A audit: contact sheets of the CURRENT Sanctuary ground families.

Reads the shipped `sanctuary_p03/*.png` exactly as the engine loads them (64x64, nearest),
and also previews them through the real projection: the ground is drawn at 0.72 screen Y
compression (ObliqueCamera.OBLIQUE_Y_SCALE), so a tile that looks fine square can still
read as mush on screen. Nothing here is a renderer output — it is an authoring preview.

Run: python3 tools/mapforge_audit.py --tag before
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets/sanctuary_p03"
EVID = ROOT / "docs/visual-evidence/p18"

FAMILIES = [
    ("grass", 8, "lawn"),
    ("soil", 6, "packed earth"),
    ("cobble", 6, "road stone"),
    ("flagstone", 8, "plaza slab"),
    ("gravel", 4, "training gravel"),
]
OBLIQUE = 0.72
BG = (18, 22, 26, 255)
INKC = (228, 224, 206, 255)


def load(fam: str, i: int) -> Image.Image:
    return Image.open(ASSETS / f"{fam}_{i}.png").convert("RGBA")


def contact(scale: int) -> Image.Image:
    cell = 64 * scale
    pad, label_w, gap = 10, 92, 6
    rows = len(FAMILIES)
    cols = max(n for _, n, _ in FAMILIES)
    out = Image.new("RGBA", (label_w + cols * (cell + gap) + pad, pad + rows * (cell + 16) + pad), BG)
    d = ImageDraw.Draw(out)
    d.text((pad, 3), f"Sanctuary ground families — {scale}x native (draw path: nearest, 0.72 oblique Y)", fill=INKC)
    for r, (fam, n, what) in enumerate(FAMILIES):
        y = pad + 12 + r * (cell + 16)
        d.text((6, y + cell // 2 - 6), fam, fill=INKC)
        d.text((6, y + cell // 2 + 6), what, fill=(170, 180, 170, 255))
        for i in range(n):
            tile = load(fam, i).resize((cell, cell), Image.NEAREST)
            out.alpha_composite(tile, (label_w + i * (cell + gap), y))
    return out


def mosaic(fam: str, n: int, cells: int, zoom: float) -> Image.Image:
    """Seeded 4x4 mosaic of the family, then projected with the real 0.72 Y compression."""
    step = 64 * zoom
    w = int(step * cells)
    h = int(step * OBLIQUE * cells)
    canvas = Image.new("RGBA", (w, h), BG)
    seed_rows = [[(i * 3 + row * 5) % n for i in range(cells)] for row in range(cells)]
    for row in range(cells):
        for col in range(cells):
            tile = load(fam, seed_rows[row][col])
            tw = int(round(step)) + 1
            th = int(round(step * OBLIQUE)) + 1
            canvas.alpha_composite(tile.resize((tw, th), Image.NEAREST), (int(col * step), int(row * step * OBLIQUE)))
    return canvas


def mosaic_sheet(tag: str) -> Image.Image:
    zooms = [(0.36, "overview 0.36"), (0.88, "gameplay 0.88"), (1.48, "close 1.48")]
    rows = []
    for fam, n, _what in FAMILIES:
        rows.append((fam, [mosaic(fam, n, 4, z) for z, _ in zooms]))
    pad = 10
    label_w = 92
    colw = max(im.width for _, ims in rows for im in ims) + 8
    out = Image.new("RGBA", (label_w + len(zooms) * colw + pad, pad + sum(ims[0].height + 18 for _, ims in rows) + pad), BG)
    d = ImageDraw.Draw(out)
    d.text((pad, 2), "4x4 mosaic through the 0.72 oblique projection (authoring preview, not engine output)",
           fill=INKC)
    y = pad + 12
    for fam, ims in rows:
        d.text((6, y + ims[0].height // 2), fam, fill=INKC)
        for c, im in enumerate(ims):
            out.alpha_composite(im, (label_w + c * colw, y))
            if fam == rows[0][0]:
                d.text((label_w + c * colw + 2, y - 10), zooms[c][1], fill=(170, 200, 220, 255))
        y += ims[0].height + 18
    out.save(EVID / f"tiles_mosaic_{tag}.png")
    return out


def stats(tag: str) -> dict:
    report = {}
    for fam, n, _ in FAMILIES:
        rows = []
        for i in range(n):
            im = load(fam, i)
            colors = {c for c in im.getdata() if c[3]}
            rows.append(len(colors))
        report[fam] = {"variants": n, "colors_min": min(rows), "colors_max": max(rows)}
    (EVID / f"tiles_stats_{tag}.json").write_text(json.dumps(report, indent=2))
    return report


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--tag", default="before")
    args = ap.parse_args()
    EVID.mkdir(parents=True, exist_ok=True)
    contact(1).save(EVID / f"tiles_contact_1x_{args.tag}.png")
    contact(4).save(EVID / f"tiles_contact_4x_{args.tag}.png")
    mosaic_sheet(args.tag)
    print(json.dumps(stats(args.tag), indent=2))
    print("wrote", EVID)


if __name__ == "__main__":
    main()
