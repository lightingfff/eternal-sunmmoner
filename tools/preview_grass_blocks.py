#!/usr/bin/env python3
"""Offline preview for the P16 overview grass batching.

Rebuilds, in Python, exactly what `PixelTileAtlas.buildSanctuaryGrassBlocks()` composes
from the shipped 64x64 sanctuary grass PNGs, then renders a small patch of island at an
overview zoom twice:

  A) the previous per-tile path (one drawImage per 64x64 tile)
  B) the new 4x4 pre-composed block path (one drawImage per 256x184 block)

and writes a side-by-side comparison plus a difference metric. This validates the art
math off-device; it is not an in-game screenshot.
"""

import math
import os
import sys

from PIL import Image, ImageChops, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TILE_DIR = os.path.join(ROOT, "app/src/main/assets/sanctuary_p03")
OUT_DIR = os.path.join(ROOT, "docs/visual-evidence/p16")

TILE_PX = 64
OBLIQUE_Y = 0.72
BLOCK_TILES = 4
BLOCK_COUNT = 8
CELL_H = int(TILE_PX * OBLIQUE_Y)  # 46, same as the Kotlin code


def load_tiles():
    tiles = []
    for i in range(8):
        tiles.append(Image.open(os.path.join(TILE_DIR, f"grass_{i}.png")).convert("RGBA"))
    return tiles


def build_blocks(tiles):
    block_w = TILE_PX * BLOCK_TILES
    block_h = CELL_H * BLOCK_TILES
    blocks = []
    for block in range(BLOCK_COUNT):
        canvas = Image.new("RGBA", (block_w, block_h), (0, 0, 0, 0))
        for cell_y in range(BLOCK_TILES):
            for cell_x in range(BLOCK_TILES):
                tx = (block & 3) * BLOCK_TILES + cell_x
                ty = (block >> 2) * BLOCK_TILES + cell_y
                h = ((tx * 73856093) ^ (ty * 19349663)) & 0x7FFFFFFF
                tile = tiles[h % len(tiles)].resize((TILE_PX, CELL_H), Image.NEAREST)
                canvas.paste(tile, (cell_x * TILE_PX, cell_y * CELL_H))
        blocks.append(canvas)
    return blocks


def render_per_tile(tiles, zoom, cols, rows, origin_x=0, origin_y=0):
    tile_w = round(TILE_PX * zoom)
    tile_h = round(TILE_PX * OBLIQUE_Y * zoom)
    w = round(TILE_PX * cols * zoom)
    h = round(TILE_PX * OBLIQUE_Y * rows * zoom)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    for ty in range(rows):
        for tx in range(cols):
            gx = tx + origin_x
            gy = ty + origin_y
            hsh = ((gx * 73856093) ^ (gy * 19349663)) & 0x7FFFFFFF
            tile = tiles[hsh % len(tiles)].resize((tile_w + 1, tile_h + 1), Image.NEAREST)
            img.paste(tile, (round(gx * TILE_PX * zoom) - round(origin_x * TILE_PX * zoom),
                             round(gy * TILE_PX * OBLIQUE_Y * zoom) - round(origin_y * TILE_PX * OBLIQUE_Y * zoom)))
    return img


def render_blocks(blocks, zoom, cols, rows, origin_x=0, origin_y=0):
    block_world = TILE_PX * BLOCK_TILES
    block_w = round(block_world * zoom)
    block_h = round(block_world * OBLIQUE_Y * zoom)
    w = round(TILE_PX * cols * zoom)
    h = round(TILE_PX * OBLIQUE_Y * rows * zoom)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    bx0 = origin_x // BLOCK_TILES
    by0 = origin_y // BLOCK_TILES
    bx1 = (origin_x + cols - 1) // BLOCK_TILES
    by1 = (origin_y + rows - 1) // BLOCK_TILES
    for by in range(by0, by1 + 1):
        for bx in range(bx0, bx1 + 1):
            hsh = ((bx * 73856093) ^ (by * 19349663)) & 0x7FFFFFFF
            block = blocks[hsh % len(blocks)].resize((block_w + 1, block_h + 1), Image.NEAREST)
            px = round(bx * block_world * zoom) - round(origin_x * TILE_PX * zoom)
            py = round(by * block_world * OBLIQUE_Y * zoom) - round(origin_y * TILE_PX * OBLIQUE_Y * zoom)
            img.paste(block, (px, py))
    return img


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    tiles = load_tiles()
    blocks = build_blocks(tiles)

    sheet = Image.new("RGBA", (BLOCK_COUNT * 130, 100), (16, 18, 30, 255))
    for i, block in enumerate(blocks):
        sheet.paste(block.resize((128, 92), Image.NEAREST), (i * 130, 4))
    sheet.save(os.path.join(OUT_DIR, "grass_blocks_sheet.png"))

    zoom = 0.45
    cols, rows = 24, 18
    origin_x, origin_y = 8, 12
    before = render_per_tile(tiles, zoom, cols, rows, origin_x, origin_y)
    after = render_blocks(blocks, zoom, cols, rows, origin_x, origin_y)

    from PIL import ImageStat

    diff = ImageChops.difference(before.convert("RGB"), after.convert("RGB"))
    bbox = diff.getbbox()
    stat = ImageStat.Stat(diff)
    mean = sum(stat.mean) / 3.0
    maximum = max(e[1] for e in stat.extrema)
    # Fraction of pixels whose largest channel delta exceeds a visible 8/255 threshold.
    diff_max = Image.new("L", diff.size)
    diff_max = ImageChops.lighter(diff_max, diff.split()[0])
    diff_max = ImageChops.lighter(diff_max, diff.split()[1])
    diff_max = ImageChops.lighter(diff_max, diff.split()[2])
    hist = diff_max.histogram()
    total = diff.size[0] * diff.size[1]
    over8 = sum(hist[9:])
    changed = sum(hist[1:])

    pad = 8
    label = 20
    out = Image.new("RGBA", (before.width * 2 + pad * 3, before.height + label + pad * 2), (10, 12, 20, 255))
    out.paste(before, (pad, label + pad))
    out.paste(after, (pad * 2 + before.width, label + pad))
    draw = ImageDraw.Draw(out)
    draw.text((pad, 4), "BEFORE: one drawImage per 64x64 tile", fill=(230, 210, 160, 255))
    draw.text((pad * 2 + before.width, 4), "AFTER: one drawImage per 4x4 block", fill=(150, 230, 190, 255))
    draw.text((pad, out.height - 12),
              f"zoom {zoom} | tile {round(TILE_PX*zoom)}x{round(TILE_PX*OBLIQUE_Y*zoom)}px | "
              f"draws {cols*rows} -> {((cols+3)//4+1)*((rows+3)//4+1)} | "
              f"delta mean {mean:.2f}/255 max {maximum} | >8 delta {over8*100.0/total:.1f}%",
              fill=(200, 200, 210, 255))
    out.save(os.path.join(OUT_DIR, "grass_blocks_before_after.png"))

    print(f"zoom={zoom} cols={cols} rows={rows}")
    print(f"per-tile draws: {cols * rows}, block draws: {((cols + 3) // 4 + 1) * ((rows + 3) // 4 + 1)}")
    print(f"channel delta: mean {mean:.3f}/255, max {maximum}")
    print(f"pixels differing at all: {changed}/{total} ({changed*100.0/total:.1f}%), "
          f"differing by more than 8/255 on any channel: {over8}/{total} ({over8*100.0/total:.1f}%)")
    print(f"diff bbox: {bbox}")
    print(f"wrote {OUT_DIR}/grass_blocks_sheet.png and grass_blocks_before_after.png")
    return 0


if __name__ == "__main__":
    sys.exit(main())
