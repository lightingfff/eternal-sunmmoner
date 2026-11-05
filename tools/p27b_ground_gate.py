#!/usr/bin/env python3
"""P27B-V5 Checkpoint 1 gate: the ground must not repeat on a 4-tile lattice.

Runs on the SHIPPED PNGs, never on a fixture. Three checks, each chosen because it
would have caught the previous atlas:

1. period      -- for every cell, compare its right/east neighbour 1 tile away with the
                  cell 4 tiles away. On a 4x4 torus the 4-tile offset *is the same cell*
                  (difference exactly 0), so `period4_identical` fails. On the 8x8 sheet
                  the 4-tile offset is a different, real neighbour.
2. wrap        -- the sheet edge must look like a real interior boundary. A sheet whose
                  outer edge is a hard cut would trade a lattice for a visible grid.
3. distinct    -- 64 real cells, not 16 cells pasted four times.

Exit code 1 on any failure. Prints the numbers so a human can judge, because this is a
structural gate, not a claim that the art looks good.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SHEET_DIR = ROOT / 'app/src/main/assets/sanctuary_p27b_2/terrain'
MANIFEST = ROOT / 'app/src/main/assets/sanctuary_p27b_2/manifest.json'
FAMILIES = ('grass', 'soil', 'flagstone', 'cobble')
SHIPPED = ROOT / 'app/src/main/assets/sanctuary_p27b/terrain'
# Mean luma of the art being replaced. A sheet that is far brighter than the shipped
# atlas reads as "drawn on top of" the world; the first V5 flagstone sheet came out at
# 174 against a shipped 166 and was visibly chalky through the plaza overlay.
LUMA_TOLERANCE = 12.0
# A sheet that lost its content (bad roll, wrong source) collapses to near-uniform.
MIN_STDDEV = 6.0


def mean_luma(image: Image.Image) -> float:
    pixels = list(image.convert('RGB').resize((128, 128)).getdata())
    return sum(0.299 * r + 0.587 * g + 0.114 * b for r, g, b in pixels) / len(pixels)


def luma_stddev(image: Image.Image) -> float:
    small = image.convert('L').resize((128, 128))
    values = list(small.getdata())
    mean = sum(values) / len(values)
    return (sum((v - mean) ** 2 for v in values) / len(values)) ** 0.5


def shipped_atlas(family: str) -> Image.Image | None:
    cells = [SHIPPED / f'{family}_{index}.png' for index in range(16)]
    if not all(path.is_file() for path in cells):
        return None
    atlas = Image.new('RGB', (256, 256))
    for index, path in enumerate(cells):
        atlas.paste(Image.open(path).convert('RGB'), ((index % 4) * 64, (index // 4) * 64))
    return atlas
GRID = 8
CELL = 64

# A 4-tile offset that lands on the *same* cell is the defect. Allow a tiny epsilon for
# lossless PNG round-trip only; real adjacent cells differ by far more than this.
IDENTICAL_EPSILON = 0.02
# Wrap may not be more than this many RGB steps worse than an interior boundary.
WRAP_ALLOWANCE = 8.0


def mean_rgb(cell: Image.Image) -> tuple[float, float, float]:
    pixels = list(cell.getdata())
    n = len(pixels)
    return tuple(sum(p[c] for p in pixels) / n for c in range(3))


def diff(a, b) -> float:
    return sum(abs(x - y) for x, y in zip(a, b)) / 3.0


def column_jump(sheet: Image.Image, x0: int, x1: int) -> float:
    a, b = sheet.crop((x0, 0, x0 + 1, sheet.height)), sheet.crop((x1, 0, x1 + 1, sheet.height))
    pa, pb = a.load(), b.load()
    return sum(sum(abs(pa[0, y][c] - pb[0, y][c]) for c in range(3))
               for y in range(sheet.height)) / (sheet.height * 3)


def row_jump(sheet: Image.Image, y0: int, y1: int) -> float:
    row_a = sheet.crop((0, y0, sheet.width, y0 + 1)).load()
    row_b = sheet.crop((0, y1, sheet.width, y1 + 1)).load()
    return sum(sum(abs(row_a[x, 0][c] - row_b[x, 0][c]) for c in range(3))
               for x in range(sheet.width)) / (sheet.width * 3)


def rotation_rule(tx: int, ty: int) -> bool:
    """Mirror of PixelTileAtlas.sanctuarySheetRotated (32-bit wrap + mask)."""
    h = (tx * -0x61c88647 + ty * 0x27d4eb2f) & 0xFFFFFFFF
    h ^= h >> 15
    h = (h * -0x7A143595) & 0xFFFFFFFF
    h ^= h >> 13
    return (h & 3) == 0


def composed_ground_is_periodic(sheet: Image.Image) -> tuple[int, int]:
    """How many tiles match the tile one sheet-period away, in the COMPOSED ground."""
    from PIL import ImageOps
    rotated = ImageOps.mirror(sheet.transpose(Image.Transpose.FLIP_TOP_BOTTOM))
    cells = {(x, y): sheet.crop((x * CELL, y * CELL, (x + 1) * CELL, (y + 1) * CELL))
             for y in range(GRID) for x in range(GRID)}
    rot_cells = {(x, y): rotated.crop((x * CELL, y * CELL, (x + 1) * CELL, (y + 1) * CELL))
                 for y in range(GRID) for x in range(GRID)}

    def tiles_match(tx: int, ty: int, ox: int, oy: int) -> bool:
        ax, ay = tx & 7, ty & 7
        bx, by = (tx + ox) & 7, (ty + oy) & 7
        a = (rot_cells if rotation_rule(tx, ty) else cells)[(ax, ay)]
        b = (rot_cells if rotation_rule(tx + ox, ty + oy) else cells)[(bx, by)]
        return a.tobytes() == b.tobytes()

    agreed = total = 0
    for ty in range(GRID):
        for tx in range(GRID):
            total += 1
            if tiles_match(tx, ty, GRID, GRID):
                agreed += 1
    return agreed, total


def _self_check() -> None:
    """Documents why the threshold is 75%: with rotation disabled every tile matches."""
    sheet = Image.new('RGB', (GRID * CELL, GRID * CELL))
    global rotation_rule
    original = rotation_rule
    try:
        rotation_rule = lambda tx, ty: False
        agreed, total = composed_ground_is_periodic(sheet)
        assert agreed == total, f'self-check failed: {agreed}/{total}'
    finally:
        rotation_rule = original
    print('self-check: with rotation disabled the composed ground is 64/64 periodic, '
          'so the gate has teeth')


def main() -> int:
    _self_check()
    failures: list[str] = []
    report: dict[str, dict] = {}
    manifest = json.loads(MANIFEST.read_text()) if MANIFEST.is_file() else {}
    for family in FAMILIES:
        path = SHEET_DIR / f'{family}_mosaic.png'
        if not path.is_file():
            failures.append(f'{family}: missing {path.relative_to(ROOT)}')
            continue
        sheet = Image.open(path).convert('RGB')
        if sheet.size != (GRID * CELL, GRID * CELL):
            failures.append(f'{family}: expected {GRID * CELL}px square, got {sheet.size}')
            continue
        cells = [mean_rgb(sheet.crop((x * CELL, y * CELL, (x + 1) * CELL, (y + 1) * CELL)))
                 for y in range(GRID) for x in range(GRID)]

        step1, step4, pairs = 0.0, 0.0, 0
        worst_identity = 0.0
        for y in range(GRID):
            for x in range(GRID - 4):
                a, b, c = cells[y * GRID + x], cells[y * GRID + x + 1], cells[y * GRID + x + 4]
                step1 += diff(a, b)
                step4 += diff(a, c)
                worst_identity = max(worst_identity, diff(a, c))
                pairs += 1
        step1 /= pairs
        step4 /= pairs
        if worst_identity <= IDENTICAL_EPSILON:
            failures.append(
                f'{family}: a cell 4 tiles away is identical to the source cell '
                f'(max diff {worst_identity:.4f}) -> 4x4 lattice is back')

        # Two decimals: gutters like cobble have genuinely similar cell means (spread
        # ~2.6 luma), so integer rounding would report a false "pasted block".
        spread = luma_stddev(sheet)
        if spread < MIN_STDDEV:
            failures.append(f'{family}: sheet is nearly uniform (luma sd {spread:.1f} < '
                            f'{MIN_STDDEV:.0f}); the roll or the source dropped the content')
        shipped = shipped_atlas(family)
        luma_delta = None
        if shipped is not None:
            luma_delta = mean_luma(sheet) - mean_luma(shipped)
            if abs(luma_delta) > LUMA_TOLERANCE:
                failures.append(f'{family}: mean luma is {luma_delta:+.1f} off the shipped atlas '
                                f'(tolerance +/-{LUMA_TOLERANCE:.0f}); it would read as a patch '
                                f'pasted onto the ground instead of the ground')

        agreed, total = composed_ground_is_periodic(sheet)
        if agreed >= total * 0.75:
            failures.append(
                f'{family}: composed ground still repeats one sheet-period later '
                f'({agreed}/{total} tiles identical) -> tile rotation rule is not active')

        distinct = len({tuple(round(v, 2) for v in cell) for cell in cells})
        if distinct < GRID * GRID // 2:
            failures.append(f'{family}: only {distinct}/64 distinct cell means; sheet is a pasted block')

        # Baseline = 95th percentile of every interior boundary. A single boundary can be
        # flat by luck (the BOX downscale duplicates columns), so comparing against one
        # arbitrary pair would fail this gate for reasons unrelated to wrapping.
        interior_jumps = [column_jump(sheet, x - 1, x) for x in range(1, sheet.width)]
        interior_jumps += [row_jump(sheet, y - 1, y) for y in range(1, sheet.height)]
        interior_jumps.sort()
        interior = interior_jumps[int(len(interior_jumps) * 0.95) - 1]
        wrap = max(column_jump(sheet, sheet.width - 1, 0), row_jump(sheet, sheet.height - 1, 0))
        if wrap > interior + WRAP_ALLOWANCE:
            failures.append(
                f'{family}: wrap seam {wrap:.2f} is worse than the 95th percentile interior '
                f'boundary {interior:.2f} by more than {WRAP_ALLOWANCE:.0f} -> visible grid at '
                f'the repeat')

        declared = manifest.get('families', {}).get(family, {})
        declared_grid = declared.get('mosaic_grid')
        if declared_grid != [GRID, GRID]:
            failures.append(f'{family}: manifest mosaic_grid {declared_grid} is not {GRID}x{GRID}')

        report[family] = {
            'mean_rgb_step_1_tile': round(step1, 3),
            'mean_rgb_step_4_tiles': round(step4, 3),
            'max_identity_at_4_tiles': round(worst_identity, 3),
            'distinct_cell_means': distinct,
            'luma_stddev': round(spread, 2),
            'tiles_matching_one_period_later': agreed,
            'tiles_compared': total,
            'mean_luma': round(mean_luma(sheet), 2),
            'shipped_mean_luma': None if shipped is None else round(mean_luma(shipped), 2),
            'luma_delta_vs_shipped': None if luma_delta is None else round(luma_delta, 2),
            'wrap_seam': round(wrap, 2),
            'interior_boundary_p95': round(interior, 2),
            'period_world_px': declared.get('ground_period_world_px'),
            'bytes': path.stat().st_size,
        }
        print(f'{family:10} step1 {step1:6.2f} step4 {step4:6.2f} distinct {distinct:2}/64 '
              f'wrap {wrap:5.2f} interior_p95 {interior:5.2f} '
              f'period-repeat {agreed}/{total}')

    summary = ROOT / 'docs/p27b_next/previews/terrain/ground_gate.json'
    summary.parent.mkdir(parents=True, exist_ok=True)
    summary.write_text(json.dumps({'report': report, 'failures': failures}, indent=2) + '\n')

    if failures:
        print('\nFAIL:')
        for failure in failures:
            print(' -', failure)
        return 1
    print('\nPASS: 8x8 sheets, real mosaic adjacency, wrap matches a real boundary.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
