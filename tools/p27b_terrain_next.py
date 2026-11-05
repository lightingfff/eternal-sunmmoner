#!/usr/bin/env python3
"""P27B-V5 ground rebuild: 8x8 mosaic SHEETS, replacing the visible 4x4 lattice.

Why a sheet and why 8x8
-----------------------
Measured defect (docs/p27b_next/02_TERRAIN_REPEAT_FIX.md): the shipped P27B
ground is a 4x4 mosaic sampled by ``(tx & 3, ty & 3)``. That is a translation
invariant lattice with a 256 world-px period, so at plaza zoom 1.0 the eye sees
the same 4-tile block ~4.5 times across the plaza.

Two wrong fixes were rejected before coding:
  * more 64x64 variants inside the same 4x4 torus -> identical period, no gain;
  * hash-picked cells -> each shipped cell is NOT self-wrapping (flagstone
    self-wrap measures up to 52/255), so arbitrary neighbours would show a grid
    of cut stones along every 64 px boundary.

The fix keeps *true mosaic adjacency* (a cell is only ever placed next to the
neighbour it was cut from) but quadruples the period: 8 tiles = 512 world px.
The 8x8 mosaic is shipped as ONE 512x512 PNG per family and blitted with a
srcRect, so runtime memory stays one bitmap per family (1 MB at ARGB_8888)
instead of 64 separate 64x64 bitmaps.

Source: the SAME original Arena image-tool illustrations (no new generation, no
reference-image reuse). Prefers the full-resolution original in art/p27b_sources
when present; if only the committed 768 px preview survives in a workspace, the
sheet is built from that and the manifest records `source_kind=preview_768`.
Either way 512 px is a smaller downscale than the shipped 256 px atlas, so the
new sheet is not softer than what it replaces.

Requires Pillow>=11. Never touches sanctuary_p03, premium_slice or sanctuary_p27b.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageStat

ROOT = Path(__file__).resolve().parents[1]
SHEET = 512          # 8 x 64
GRID = 8
FAMILIES = {
    'grass':      {'source': 'grass_source.png',      'colors': 96, 'target': (83, 107, 66)},
    'soil':       {'source': 'soil_source.png',       'colors': 88, 'target': (171, 140, 101)},
    'flagstone':  {'source': 'flagstone_source.png',  'colors': 104, 'target': (170, 174, 169)},
    'cobble':     {'source': 'cobble_source.png',     'colors': 96, 'target': (105, 115, 118)},
}
ASSET = ROOT / 'app/src/main/assets/sanctuary_p27b_2'
PREVIEW = ROOT / 'docs/p27b_next/previews/terrain'


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def resolve_source(sources: Path, filename: str) -> tuple[Path, str]:
    """Full-resolution original if the workspace still has it, else the tracked preview."""
    original = sources / filename
    if original.is_file():
        return original, 'original'
    preview = sources / 'preview' / f'{Path(filename).stem}_768.jpg'
    if preview.is_file():
        return preview, 'preview_768'
    raise FileNotFoundError(f'no source or preview for {filename}')


def square_crop(image: Image.Image) -> Image.Image:
    src = image.convert('RGB')
    side = min(src.size)
    return src.crop(((src.width - side) // 2, (src.height - side) // 2,
                     (src.width + side) // 2, (src.height + side) // 2))


def make_torus(plane: Image.Image, width: int = 20) -> Image.Image:
    """Classic roll-and-heal: turns a plain illustration into a true torus.

    Rolling by half the sheet moves the source's own hard outer cut to the centre,
    where a narrow crossfade is invisible next to stone edges. The sheet's outer
    edges then sample neighbouring source pixels, so *every* cell adjacency --
    interior and wrap alike -- is real adjacency, and nothing is blended at the
    cell grid itself.
    """
    w, h = plane.size
    rolled = Image.new('RGB', plane.size)
    rolled.paste(plane.crop((w // 2, h // 2, w, h)), (0, 0))
    rolled.paste(plane.crop((0, h // 2, w // 2, h)), (w // 2, 0))
    rolled.paste(plane.crop((w // 2, 0, w, h // 2)), (0, h // 2))
    rolled.paste(plane.crop((0, 0, w // 2, h // 2)), (w // 2, h // 2))

    src = plane.load()
    pix = rolled.load()
    half = w // 2
    for d in range(width):
        # Crossfade outwards from the seam line, using pixels mirrored across it.
        alpha = 0.5 * (1 - d / width) ** 2
        for y in range(h):
            a, b = pix[half - 1 - d, y], pix[half + d, y]
            pix[half - 1 - d, y] = tuple(round(q * (1 - alpha) + r * alpha) for q, r in zip(a, b))
            pix[half + d, y] = tuple(round(r * (1 - alpha) + q * alpha) for q, r in zip(a, b))
        for x in range(w):
            a, b = pix[x, half - 1 - d], pix[x, half + d]
            pix[x, half - 1 - d] = tuple(round(q * (1 - alpha) + r * alpha) for q, r in zip(a, b))
            pix[x, half + d] = tuple(round(r * (1 - alpha) + q * alpha) for q, r in zip(a, b))
    del src
    return rolled


def build(source: Image.Image, colors: int, target: tuple[int, int, int]) -> Image.Image:
    plane = make_torus(square_crop(source).resize((SHEET, SHEET), Image.Resampling.BOX))
    mean = ImageStat.Stat(plane).mean
    shifts = [target[c] - mean[c] for c in range(3)]
    graded = Image.merge('RGB', [
        channel.point(lambda v, s=s: min(255, max(0, round(v + s))))
        for channel, s in zip(plane.split(), shifts)])
    # No dither: keep the pixel cluster geometry of the illustration. Quantising after
    # the heel keeps the crossfade band from reading as a soft stripe.
    return graded.quantize(colors=colors, dither=Image.Dither.NONE).convert('RGB')


def cell_jump(a: Image.Image, b: Image.Image) -> float:
    pa, pb = a.load(), b.load()
    east = sum(abs(pa[63, y][c] - pb[0, y][c]) for y in range(64) for c in range(3)) / (64 * 3)
    south = sum(abs(pa[x, 63][c] - pb[x, 0][c]) for x in range(64) for c in range(3)) / (64 * 3)
    return max(east, south)


def cells(sheet: Image.Image) -> list[Image.Image]:
    return [sheet.crop((x * 64, y * 64, (x + 1) * 64, (y + 1) * 64))
            for y in range(GRID) for x in range(GRID)]


def preview(sheet: Image.Image, tiles: int, out: Path) -> None:
    """Repeat the sheet exactly as the engine tiles it (tx & 7, ty & 7)."""
    side = 64 * tiles
    canvas = Image.new('RGB', (side, side))
    for ty in range(tiles):
        for tx in range(tiles):
            canvas.paste(sheet.crop((((tx & 7) * 64), ((ty & 7) * 64),
                                     ((tx & 7) * 64) + 64, ((ty & 7) * 64) + 64)),
                         (tx * 64, ty * 64))
    canvas.resize((side * 2, side * 2), Image.Resampling.NEAREST).save(out)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--sources', type=Path, default=ROOT / 'art/p27b_sources')
    args = parser.parse_args()
    (ASSET / 'terrain').mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    manifest = {
        'schema': 'eternal-summoner.p27b2.visual-profile.v1',
        'status': 'GAME_CANDIDATE',
        'note': '8x8 ground mosaic sheets, sampled by (tx & 7, ty & 7). Fixes the '
                'measured 4-tile lattice; adjacency is true mosaic adjacency so no '
                'cell is ever placed against a cell it was not cut from.',
        'camera_ground_y_scale': .72,
        'tile_px': 64,
        'mosaic_grid': [GRID, GRID],
        'sheet_canvas': [SHEET, SHEET],
        'terrain_count_per_family': 1,
        'families': {},
    }
    for name, spec in FAMILIES.items():
        origin, kind = resolve_source(args.sources, spec['source'])
        sheet = build(Image.open(origin), spec['colors'], spec['target'])
        target = ASSET / 'terrain' / f'{name}_mosaic.png'
        sheet.save(target, optimize=True)
        all_cells = cells(sheet)
        east = [cell_jump(all_cells[y * GRID + x], all_cells[y * GRID + (x + 1) % GRID])
                for y in range(GRID) for x in range(GRID)]
        south = [cell_jump(all_cells[y * GRID + x], all_cells[((y + 1) % GRID) * GRID + x])
                 for y in range(GRID) for x in range(GRID)]
        mean = ImageStat.Stat(sheet).mean
        manifest['families'][name] = {
            'path': f'terrain/{name}_mosaic.png',
            'size': [SHEET, SHEET],
            'channels': 'RGB opaque',
            'mosaic_grid': [GRID, GRID],
            'cell_px': 64,
            'distinct_cells': len({hashlib.sha256(c.tobytes()).hexdigest() for c in all_cells}),
            'source_path': str(origin.relative_to(ROOT)),
            'source_kind': kind,
            'source_size': list(Image.open(origin).size),
            'source_sha256': sha(origin),
            'sha256': sha(target),
            'mean_rgb': [round(v, 1) for v in mean],
            'max_x_edge_jump_rgb': round(max(east), 2),
            'max_y_edge_jump_rgb': round(max(south), 2),
            'ground_period_world_px': 64 * GRID,
            'qa_status': 'GAME_CANDIDATE',
            'visual_qa': '8x8 repeat is 512 world px; still a mosaic, still needs engine review',
        }
        for tiles in (3, 5, 9):
            preview(sheet, tiles, PREVIEW / f'ART_CONTACT_{name}_{tiles}x{tiles}.png')
        print(f'{name:10} {SHEET}x{SHEET} distinct cells {manifest["families"][name]["distinct_cells"]:3} '
              f'seam east {max(east):5.2f} south {max(south):5.2f} '
              f'bytes {target.stat().st_size / 1024:.0f} KB')
    (ASSET / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('wrote', ASSET / 'manifest.json')


if __name__ == '__main__':
    main()
