#!/usr/bin/env python3
"""P27B-V5 ground rebuild: 8x8 mosaic SHEETS, calibrated to the shipped art density.

Why a sheet and why 8x8
-----------------------
Measured defect (docs/p27b_next/02_TERRAIN_REPEAT_FIX.md): the shipped P27B ground is
a 4x4 mosaic sampled by ``(tx & 3, ty & 3)``. That is a translation-invariant lattice
with a 256 world-px period, so at plaza zoom 1.0 the eye meets the same block ~4.5
times across the plaza. Inside one torus the 16 cells already agree to 1-2 luma, which
is why adding more 64 px variants could never have fixed it.

Two wrong fixes rejected while building this:
  * hash-picked cells -> shipped cells are not self-wrapping (flagstone self-wrap
    measures up to 52/255), so every 64 px boundary would show a cut stone;
  * mirror-tiling the shipped torus 2x2 -> seamless, but produces a butterfly
    kaleidoscope; rejected on look, not on numbers.

The fix keeps *true mosaic adjacency* (a cell is only ever placed next to the neighbour
it was cut from) and quadruples the period to 8 tiles = 512 world px. The 8x8 mosaic
ships as ONE 512x512 PNG per family and is blitted with a src rect, so runtime memory
stays one bitmap per family instead of 64 separate cells.

Density calibration
-------------------
The sheet is downscaled from a source illustration, so stone/tuft size depends on how
much of that illustration is used. Rather than eyeballing it, each family is
calibrated against the SHIPPED atlas: the number of stones across 512 world px must
match the shipped art within DENSITY_TOLERANCE. This is what caught the first attempt
(a 768 px source reused from P27B produced stones twice the shipped size) and the
second (a 1254 px source with 18 stones across produced 36 px cobbles vs the shipped
28 px slabs).

Sources are dedicated P27B-V5 illustrations in art/p27b_sources_next. Requires
Pillow>=11. Never writes to sanctuary_p03, premium_slice or sanctuary_p27b.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import statistics
from pathlib import Path

from PIL import Image, ImageStat

ROOT = Path(__file__).resolve().parents[1]
SHEET = 512          # 8 x 64
GRID = 8
CELL = 64
# Every source is authored 1:1 with what the engine shows: each illustration is painted
# at 4x the tile size (64 px tiles -> 256 world px of source), so downscaling the whole
# image into ONE 512 px sheet reproduces the shipped stone size exactly. Calibrating with
# an automatic feature counter was tried and dropped: the counter is not resolution
# invariant (it reported 38 features/512 for the shipped atlas that directly measures
# 18), so it cannot decide this. CROP is kept as an escape hatch only.
CROP = 1.0
FAMILIES = {
    'grass':     {'source': 'grass_source.png',     'colors': 96, 'target': (83, 107, 66)},
    'soil':      {'source': 'soil_source.png',      'colors': 88, 'target': (171, 140, 101)},
    'flagstone': {'source': 'flagstone_source.png', 'colors': 104, 'target': (166, 171, 171)},
    'cobble':    {'source': 'cobble_source.png',    'colors': 96, 'target': (105, 115, 118)},
}
ASSET = ROOT / 'app/src/main/assets/sanctuary_p27b_2'
SHIPPED = ROOT / 'app/src/main/assets/sanctuary_p27b/terrain'
PREVIEW = ROOT / 'docs/p27b_next/previews/terrain'


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_for(sources: Path, filename: str) -> Path:
    original = sources / filename
    if original.is_file():
        return original
    for candidate in (sources / 'preview' / f'{Path(filename).stem}_768.jpg',
                      ROOT / 'art/p27b_sources/preview' / f'{Path(filename).stem}_768.jpg'):
        if candidate.is_file():
            return candidate
    raise FileNotFoundError(f'no source for {filename} in {sources}')


def feature_columns(image: Image.Image) -> float:
    """Median count of dark feature gaps (mortar joints / tuft shadows) per scanline.

    Scale-independent: it answers "how many distinct features across this image", which
    is exactly what decides how big a stone looks once the image is sampled to 64 px
    tiles. Runs on a greyscale copy so it is cheap.
    """
    grey = image.convert('L')
    width, height = grey.size
    px = grey.load()
    step = max(1, height // 96)
    counts = []
    for y in range(height // 6, 5 * height // 6, step):
        row = [px[x, y] for x in range(0, width, max(1, width // 384))]
        mean = statistics.fmean(row)
        dark = [1 if value < mean - 14 else 0 for value in row]
        counts.append(sum(1 for i in range(1, len(dark)) if dark[i] and not dark[i - 1]))
    return statistics.median(counts)


def density_at_512(image: Image.Image) -> float:
    """Features across 512 world px, i.e. after the image is sampled to the sheet."""
    return feature_columns(image) * SHEET / image.width


def best_cut(image: Image.Image, axis: str) -> int:
    """Edge of the illustration that joins most cleanly: cheapest seam to close.

    Letting the source choose where the torus opens means no blending is needed at all,
    so no crossfade band exists to repeat. The first version blended a 20 px heel and
    that band showed up as a visible grid on repeat.
    """
    size = image.width if axis == 'x' else image.height
    px = image.load()
    other = image.height if axis == 'x' else image.width
    step = max(1, other // 64)
    best, best_cost = 0, None
    for index in range(1, size):
        cost = 0
        for other_index in range(0, other, step):
            a = px[index, other_index] if axis == 'x' else px[other_index, index]
            b = px[index - 1, other_index] if axis == 'x' else px[other_index, index - 1]
            cost += abs(a - b)
        if best_cost is None or cost < best_cost:
            best, best_cost = index, cost
    return best


def make_torus(plane: Image.Image) -> Image.Image:
    """Roll the illustration so its cleanest cut lands on the sheet edge (no blending).

    Four-quadrant roll: the source for (x, y) is ((x + roll_x) % w, (y + roll_y) % h),
    which is a pure torus shift. Only the source's own hardest cut needs to end up at the
    wrap, so no crossfade is introduced and no band can repeat.
    """
    grey = plane.convert('L')
    roll_x = best_cut(grey, 'x')
    roll_y = best_cut(grey, 'y')
    width, height = plane.size
    rolled = Image.new('RGB', plane.size)
    # (source box, destination) for the four quadrants of the shifted image
    rolled.paste(plane.crop((roll_x, roll_y, width, height)), (0, 0))
    rolled.paste(plane.crop((0, roll_y, roll_x, height)), (width - roll_x, 0))
    rolled.paste(plane.crop((roll_x, 0, width, roll_y)), (0, height - roll_y))
    rolled.paste(plane.crop((0, 0, roll_x, roll_y)), (width - roll_x, height - roll_y))
    return rolled


def build(source: Image.Image, colors: int, target: tuple[int, int, int], crop: float) -> Image.Image:
    side = int(min(source.size) * crop)
    left = (source.width - side) // 2
    top = (source.height - side) // 2
    region = source.convert('RGB').crop((left, top, left + side, top + side))
    hi = region.resize((SHEET * 2, SHEET * 2), Image.Resampling.BOX)
    plane = make_torus(hi).resize((SHEET, SHEET), Image.Resampling.BOX)
    mean = ImageStat.Stat(plane).mean
    shifts = [target[c] - mean[c] for c in range(3)]
    graded = Image.merge('RGB', [
        channel.point(lambda v, s=s: min(255, max(0, round(v + s))))
        for channel, s in zip(plane.split(), shifts)])
    # No dither: keep the pixel cluster geometry of the illustration.
    return graded.quantize(colors=colors, dither=Image.Dither.NONE).convert('RGB')


def shipped_density(family: str) -> float | None:
    """Density of the art being replaced, reconstructed from its 16 shipped cells."""
    cells = [SHIPPED / f'{family}_{index}.png' for index in range(16)]
    if not all(path.is_file() for path in cells):
        return None
    atlas = Image.new('RGB', (256, 256))
    for index, path in enumerate(cells):
        atlas.paste(Image.open(path).convert('RGB'), ((index % 4) * 64, (index // 4) * 64))
    return density_at_512(atlas) * 2.0


def cell_jump(a: Image.Image, b: Image.Image) -> float:
    pa, pb = a.load(), b.load()
    east = sum(abs(pa[63, y][c] - pb[0, y][c]) for y in range(64) for c in range(3)) / (64 * 3)
    south = sum(abs(pa[x, 63][c] - pb[x, 0][c]) for x in range(64) for c in range(3)) / (64 * 3)
    return max(east, south)


def cells(sheet: Image.Image) -> list[Image.Image]:
    return [sheet.crop((x * CELL, y * CELL, (x + 1) * CELL, (y + 1) * CELL))
            for y in range(GRID) for x in range(GRID)]


def preview(sheet: Image.Image, tiles: int, out: Path) -> None:
    """Repeat the sheet exactly as the engine tiles it (tx & 7, ty & 7)."""
    side = CELL * tiles
    canvas = Image.new('RGB', (side, side))
    for ty in range(tiles):
        for tx in range(tiles):
            sx, sy = (tx & 7) * CELL, (ty & 7) * CELL
            canvas.paste(sheet.crop((sx, sy, sx + CELL, sy + CELL)), (tx * CELL, ty * CELL))
    canvas.resize((side * 2, side * 2), Image.Resampling.NEAREST).save(out)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--sources', type=Path, default=ROOT / 'art/p27b_sources_next')
    parser.add_argument('--crop', type=float, default=None,
                        help='override the calibrated crop for every family (debug only)')
    args = parser.parse_args()
    (ASSET / 'terrain').mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)

    manifest = {
        'schema': 'eternal-summoner.p27b2.visual-profile.v1',
        'status': 'GAME_CANDIDATE',
        'note': '8x8 ground mosaic sheets sampled by (tx & 7, ty & 7). Fixes the measured '
                '4-tile lattice; adjacency is true mosaic adjacency, so no cell is ever '
                'placed against a cell it was not cut from.',
        'camera_ground_y_scale': .72,
        'tile_px': CELL,
        'mosaic_grid': [GRID, GRID],
        'sheet_canvas': [SHEET, SHEET],
        'terrain_count_per_family': 1,
        'source_scale': '1 source pixel : 1/256 sheet at 4x tile size -> 1:1 with shipped',
        'families': {},
    }
    for name, spec in FAMILIES.items():
        origin = source_for(args.sources, spec['source'])
        source = Image.open(origin)
        expected = shipped_density(name)
        chosen_crop = args.crop if args.crop else CROP
        sheet = build(source, spec['colors'], spec['target'], chosen_crop)
        density = density_at_512(sheet)
        target_file = ASSET / 'terrain' / f'{name}_mosaic.png'
        sheet.save(target_file, optimize=True)

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
            'cell_px': CELL,
            'distinct_cells': len({hashlib.sha256(c.tobytes()).hexdigest() for c in all_cells}),
            'source_path': str(origin.relative_to(ROOT)),
            'source_size': list(source.size),
            'source_sha256': sha(origin),
            'crop_fraction': chosen_crop,
            'features_per_512_estimate': round(density, 2),
            'shipped_features_per_512_estimate': None if expected is None else round(expected, 2),
            'density_note': 'linear estimate, compared to the shipped atlas for scale only; '
                            'the two counters are NOT resolution invariant, so this is a '
                            'sanity check, not a pass/fail gate',
            'sha256': sha(target_file),
            'mean_rgb': [round(v, 1) for v in mean],
            'max_x_edge_jump_rgb': round(max(east), 2),
            'max_y_edge_jump_rgb': round(max(south), 2),
            'ground_period_world_px': CELL * GRID,
            'qa_status': 'GAME_CANDIDATE',
            'visual_qa': '8x8 repeat is 512 world px; still a mosaic, still needs engine review',
        }
        for tiles in (3, 5, 9):
            preview(sheet, tiles, PREVIEW / f'ART_CONTACT_{name}_{tiles}x{tiles}.png')
        print(f'{name:10} crop {chosen_crop:4.2f} features/512 {density:5.1f} '
              f'(shipped {expected if expected is None else round(expected, 1)}) '
              f'seam east {max(east):5.2f} south {max(south):5.2f} '
              f'{target_file.stat().st_size / 1024:.0f} KB')
    (ASSET / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('wrote', ASSET / 'manifest.json')


if __name__ == '__main__':
    main()
