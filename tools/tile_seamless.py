#!/usr/bin/env python3
"""Make a repeating sprite tile horizontally seam-free by TRANSLATION ONLY.

Why translation and not a cross-fade: the sanctuary art is crisp pixel art drawn at
1:1 (FilterQuality.None). Blending a band across the seam turns 4-6 columns of sharp
blocks into mush, which is more visible than the seam it removes.

How it works: rotating the image circularly by s puts original column s-1 next to
column s at the tile boundary. So after ANY rotation the two touching columns were
already neighbours in the source art - the question is only WHICH pair of neighbours
looks most continuous. That is measurable: sum of |RGB| differences over the touching
columns. We pick the minimum, which usually lands inside a mortar joint.

Limitation, stated honestly: rotation does not fix the PHASE of a repeating feature
such as crenellations. It is therefore not enough to trust the number - the tool also
writes a 3-tile strip so the result can be looked at.

Usage:
  python3 tools/tile_seamless.py IN.png OUT.png --width 88 --height 48 [--strip out.png]
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


def seam_columns(image: Image.Image) -> tuple[float, int]:
    """Mean |RGB| difference between the right edge and the left edge."""
    width, height = image.size
    px = image.load()
    total = 0
    pairs = 0
    for y in range(height):
        a, b = px[width - 1, y], px[0, y]
        if a[3] == 0 or b[3] == 0:
            continue
        total += abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2])
        pairs += 1
    return (total / pairs if pairs else 0.0), pairs


def seam_rows(image: Image.Image) -> tuple[float, int]:
    width, height = image.size
    px = image.load()
    total = 0
    pairs = 0
    for x in range(width):
        a, b = px[x, height - 1], px[x, 0]
        if a[3] == 0 or b[3] == 0:
            continue
        total += abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2])
        pairs += 1
    return (total / pairs if pairs else 0.0), pairs


def best_shift(image: Image.Image) -> tuple[int, float]:
    """Circular shift that minimises the vertical seam."""
    width = image.width
    px = image.load()
    best_s, best_cost = 0, float("inf")
    for s in range(width):
        cost = 0
        for y in range(image.height):
            a, b = px[(s - 1) % width, y], px[s % width, y]
            if a[3] == 0 or b[3] == 0:
                continue
            cost += abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2])
        if cost < best_cost:
            best_s, best_cost = s, cost
    return best_s, best_cost


def strip(image: Image.Image, count: int = 3) -> Image.Image:
    out = Image.new("RGBA", (image.width * count, image.height), (0, 0, 0, 0))
    for i in range(count):
        out.paste(image, (i * image.width, 0))
    return out


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source")
    parser.add_argument("output")
    parser.add_argument("--width", type=int, required=True)
    parser.add_argument("--height", type=int, required=True)
    parser.add_argument("--strip", help="also write a 3-tile strip here for eyeballing")
    parser.add_argument("--keep-alpha", action="store_true",
                        help="resample with BOX instead of hard-fitting to the canvas")
    args = parser.parse_args()

    src = Image.open(args.source).convert("RGBA")
    before_v, _ = seam_columns(src)
    before_h, h_pairs = seam_rows(src)

    shift, _ = best_shift(src)
    rotated = Image.new("RGBA", src.size, (0, 0, 0, 0))
    width = src.width
    rotated.paste(src.crop((shift, 0, width, src.height)), (0, 0))
    rotated.paste(src.crop((0, 0, shift, src.height)), (width - shift, 0))

    if rotated.size != (args.width, args.height):
        rotated = rotated.resize((args.width, args.height), Image.Resampling.BOX)
        # BOX invents in-between alpha; the renderer needs binary alpha.
        rotated.putalpha(rotated.getchannel("A").point(lambda v: 255 if v >= 128 else 0))

    after_v, pairs_v = seam_columns(rotated)
    after_h, pairs_h = seam_rows(rotated)

    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    rotated.save(out, optimize=True)
    if args.strip:
        Path(args.strip).parent.mkdir(parents=True, exist_ok=True)
        strip(rotated).save(args.strip)

    print(f"shift used: {shift}px")
    print(f"vertical seam:   {before_v:7.2f} -> {after_v:7.2f}  "
          f"({pairs_v}/{rotated.height} edge rows compared)")
    print(f"horizontal seam: {before_h:7.2f} -> {after_h:7.2f}  "
          f"({pairs_h}/{rotated.width} edge columns compared)")
    if pairs_v < rotated.height // 2:
        print("NOTE: most edge rows are transparent, so the vertical seam is not really "
              "tested by this tile's geometry.")
    print(f"wrote {out}" + (f" and {args.strip}" if args.strip else ""))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
