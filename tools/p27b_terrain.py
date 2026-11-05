#!/usr/bin/env python3
"""Normalize four *individual original model-generated material sources* into candidates.

The candidate families are rectangular TORUS mosaics, not 8 arbitrary independent
crops: the atlas chooses (tx % columns, ty % rows), so neighboring 64x64 cells
share real stone/grass structure. Only the outer edge of each whole mosaic is
wrap-blended. Distinct 3x3 / 5x5 and projected ground previews are generated for
visual QA; a numeric zero edge jump alone cannot make a texture seamless.

Requires Pillow>=11. Sources are kept in art/p27b_sources/ (not user reference
images). Defaults to art/p27b_workbench/; never modifies P03 or old user work.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageStat

ROOT = Path(__file__).resolve().parents[1]
FAMILIES = {
    # Four-by-four mosaic means a five-by-five repeat preview exposes the *first*
    # repetition, rather than repeating identical rows twice inside a 5x5 crop.
    # Extra authored atlas cells are intentional; PixelTileAtlas reads all 16.
    'grass': {'cols': 4, 'rows': 4, 'colors': 88, 'target': (83, 107, 66)},
    'soil': {'cols': 4, 'rows': 4, 'colors': 80, 'target': (171, 140, 101)},
    'flagstone': {'cols': 4, 'rows': 4, 'colors': 96, 'target': (170, 174, 169)},
    'cobble': {'cols': 4, 'rows': 4, 'colors': 88, 'target': (105, 115, 118)},
    # A separately graded cut of the SAME original limestone illustration,
    # reserved for the existing Summoning Hall zone; no island-wide white wash.
    'summon_ivory': {'cols': 4, 'rows': 4, 'colors': 96, 'target': (227, 224, 213)},
}


def mix_edge(plane: Image.Image, width: int = 8) -> Image.Image:
    """Repair only the mosaic's periodic OUTER edge; interior joints stay natural."""
    out = plane.convert('RGB').copy()
    pix = out.load()
    w, h = out.size
    for d in range(width):
        alpha = 0.5 * (1 - d / width) ** 2
        for y in range(h):
            a, b = pix[d, y], pix[w - d - 1, y]
            pix[d, y] = tuple(round(x*(1-alpha)+z*alpha) for x,z in zip(a,b))
            pix[w-d-1, y] = tuple(round(z*(1-alpha)+x*alpha) for x,z in zip(a,b))
    for d in range(width):
        alpha = 0.5 * (1 - d / width) ** 2
        for x in range(w):
            a, b = pix[x, d], pix[x, h-d-1]
            pix[x, d] = tuple(round(q*(1-alpha)+r*alpha) for q,r in zip(a,b))
            pix[x, h-d-1] = tuple(round(r*(1-alpha)+q*alpha) for q,r in zip(a,b))
    return out


def normalize(source: Image.Image, cols: int, rows: int, target: tuple[int,int,int], colors: int) -> Image.Image:
    # Do not stretch shapes; crop a matching aspect-ratio rectangle, then downsample
    # into the ENTIRE mosaic once (not one 512px image into a single 64px tile).
    src = source.convert('RGB')
    ar = cols / rows
    cw = src.width
    ch = round(cw / ar)
    if ch > src.height:
        ch, cw = src.height, round(src.height * ar)
    crop = src.crop(((src.width-cw)//2, (src.height-ch)//2,
                    (src.width+cw)//2, (src.height+ch)//2))
    plane = crop.resize((64*cols, 64*rows), Image.Resampling.BOX)
    mean = ImageStat.Stat(plane).mean
    shifts = [target[c] - mean[c] for c in range(3)]
    graded = Image.merge('RGB', [c.point(lambda v, s=s: min(255,max(0,round(v+s))))
                                 for c,s in zip(plane.split(),shifts)])
    # Quantization after grading, without dither; keep local pixel cluster geometry.
    quantized = graded.quantize(colors=colors, dither=Image.Dither.NONE).convert('RGB')
    return mix_edge(quantized)


def seam_jump(a: Image.Image, b: Image.Image, axis: str) -> float:
    pa, pb = a.load(), b.load()
    if axis == 'x':
        n = a.height
        diffs = [abs(pa[a.width-1,y][c]-pb[0,y][c]) for y in range(n) for c in range(3)]
    else:
        n = a.width
        diffs = [abs(pa[x,a.height-1][c]-pb[x,0][c]) for x in range(n) for c in range(3)]
    return round(sum(diffs) / len(diffs), 2)


def tile_preview(tiles: list[Image.Image], cols: int, rows: int, count: int, out: Path) -> None:
    # Engine hashes are NOT used in this mosaic mode; previews use atlas index by
    # world-coordinate modulo, matching PixelTileAtlas.p27bTileIndex.
    mosaic = Image.new('RGB', (64*count, 64*count))
    for y in range(count):
        for x in range(count):
            mosaic.paste(tiles[(y%rows)*cols+x%cols], (64*x,64*y))
    mosaic.resize((64*count*2, 64*count*2), Image.Resampling.NEAREST).save(out)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--sources', type=Path, default=ROOT/'art/p27b_sources')
    parser.add_argument('--output', type=Path, default=ROOT/'art/p27b_workbench')
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    manifest = {'schema': 'p27b.terrain-candidate.v1', 'source': 'original Arena image-tool output',
                'runtime_integrated': False, 'families': {}}
    for family, spec in FAMILIES.items():
        source_path = args.sources / f'{"flagstone" if family == "summon_ivory" else family}_source.png'
        source = Image.open(source_path)
        cols, rows = spec['cols'], spec['rows']
        mosaic = normalize(source, cols, rows, spec['target'], spec['colors'])
        tiles = []
        paths = []
        for ty in range(rows):
            for tx in range(cols):
                im = mosaic.crop((tx*64,ty*64,(tx+1)*64,(ty+1)*64))
                file = args.output / f'{family}_{ty*cols+tx}.png'
                im.save(file)
                tiles.append(im)
                paths.append({'id': file.stem, 'file': file.name, 'size': [64,64],
                              'sha256': hashlib.sha256(file.read_bytes()).hexdigest(),
                              'mode': 'RGB', 'grid_pos': [tx,ty]})
        for count in (3,5):
            tile_preview(tiles, cols, rows, count, args.output/f'{family}_{count}x{count}.png')
        x_jumps = [seam_jump(tiles[y*cols+x], tiles[y*cols+(x+1)%cols], 'x')
                   for y in range(rows) for x in range(cols)]
        y_jumps = [seam_jump(tiles[y*cols+x], tiles[((y+1)%rows)*cols+x], 'y')
                   for y in range(rows) for x in range(cols)]
        hashes = {p['sha256'] for p in paths}
        assert len(hashes) == len(paths), f'{family}: duplicate atlas tile'
        manifest['families'][family] = {
            'source_file': str(source_path.relative_to(ROOT)),
            'source_sha256': hashlib.sha256(source_path.read_bytes()).hexdigest(),
            'source_dimensions': list(source.size), 'mosaic_grid': [cols,rows],
            'number_of_distinct_tiles': len(hashes), 'max_x_edge_jump_rgb': max(x_jumps),
            'max_y_edge_jump_rgb': max(y_jumps), 'mean_rgb': [round(x,1) for x in ImageStat.Stat(mosaic).mean],
            'pixel_source': 'single generated material source; BOX over full mosaic; no-dither palette; 8px periodic-wrap border',
            'seam_status': 'NEEDS_VISUAL_QA: numerical continuity alone does not prove seamless',
            'qa_status': 'DRAFT', 'tiles': paths,
        }
        print(family, f'{cols}x{rows}', 'unique', len(hashes), 'jump max', max(x_jumps),max(y_jumps))
    (args.output/'manifest_candidates.json').write_text(json.dumps(manifest, indent=2)+'\n')
    print(args.output)


if __name__ == '__main__':
    main()
