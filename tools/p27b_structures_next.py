#!/usr/bin/env python3
"""P27B-V5 checkpoint 2: cut P27B house art into the inherited canvas contracts.

Debt A, measured before any art was made: the new P27B landmarks sit at luma ~175,
while every P03 building and prop sits at 50-59. That is not a subtle tone shift, it
is two different games sharing a map. This tool replaces the *houses* first, because
they are the largest and most repeated silhouettes in the residential rows.

Contract rules, inherited from P21/P27B and NOT changed here:
  * exact canvas size per prop kind (`SPECS`), because `WorldObstacle.footprintHalfW`
    and `footprintDepthNorth` were authored against those dimensions;
  * bottom-centre pivot, so the sprite's ground contact row still lands on `WorldObstacle.y`
    and the existing Y-depth sort keeps working;
  * alpha is binary: no soft halo, no drop shadow baked in (the renderer draws its own
    directional shadow from the footprint);
  * no collider, zone, doorway or camera value is touched.

Background removal: the image tool paints a flat grey plate instead of alpha. Only
pixels reachable from the border *and* neutral (low channel spread) are cut, so a warm
cream wall or a brown beam that happens to be light is never eaten through.

Requires Pillow>=11. Writes only to art/p27b_workbench_next and
app/src/main/assets/sanctuary_p27b_2/structures. Never writes sanctuary_p03.
"""
from __future__ import annotations

import hashlib
import json
from collections import deque
from pathlib import Path

from PIL import Image, ImageStat

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/p27b_sources_next'
OUT = ROOT / 'app/src/main/assets/sanctuary_p27b_2/structures'
PREVIEW = ROOT / 'docs/p27b_next/previews/structures'
OLD = ROOT / 'app/src/main/assets/sanctuary_p03'

# Canvas sizes are the SHIPPED P03 dimensions. Do not "improve" them: footprints and
# doorway triggers are authored against them.
SPECS = {
  'house_timber_crest': {'file': 'townhouse_a.png', 'size': (152, 156), 'replaces': 'townhouse_a'},
  'house_thatched':     {'file': 'cottage_brown.png', 'size': (132, 144), 'replaces': 'cottage_brown'},
  'house_stone_guild':  {'file': 'townhouse_b.png', 'size': (144, 160), 'replaces': 'townhouse_b'},
  'house_slate_wing':   {'file': 'cottage_slate.png', 'size': (132, 144), 'replaces': 'cottage_slate'},
}

# Props come from one painted sheet. Each entry names the n-th item left to right and the
# inherited canvas it must occupy. Canvas sizes come from the shipped P03 props.
PROP_SHEET = 'prop_set_01.png'
PROP_SPECS = [
  ('street_lantern',   0, (32, 66),  'street_lantern_0'),
  ('wooden_bench',     1, (58, 36),  'wooden_bench'),
  ('barrel_cluster',   2, (52, 44),  'barrels'),
  ('flower_planter',   3, (64, 36),  'flower_planter'),
  ('training_dummy',   4, (40, 58),  'training_dummy'),
]

# Facilities keep their premium_slice canvas sizes: same reason as the houses.
FACILITY_SPECS = {
  'facility_hero_quarters': {'file': 'hero_quarters.png', 'size': (200, 168), 'replaces_premium': 'hero_quarters'},
  'facility_armory':        {'file': 'armory.png',        'size': (190, 166), 'replaces_premium': 'armory'},
  'facility_synthesis_lab': {'file': 'synthesis_lab.png', 'size': (188, 168), 'replaces_premium': 'synthesis_lab'},
  'facility_restaurant':    {'file': 'restaurant.png',    'size': (206, 172), 'replaces_premium': 'restaurant'},
}

# Debt A, second pass: the corner towers and the hero-registry hall were still P03 art
# while the gate itself was already P27B ivory, so the wall line changed tone mid-run.
# These three close that gap. Canvas sizes again inherited from the shipped art, not
# chosen: the corner towers are the SLOT the renderer already reserves.
DEBT_A_SPECS = {
  'facility_hero_registry': {'file': 'hero_registry.png',  'size': (188, 166), 'replaces_premium': 'hero_registry'},
  'tower_corner':           {'file': 'fortress_tower.png',  'size': (80, 118),  'replaces': 'fortress_tower'},
  'tower_gatehouse':        {'file': 'gate_tower.png',      'size': (96, 136),  'replaces': 'gate_tower'},
}

# Debt A, third pass: the perimeter curtain wall. This one is a REPEATING TILE, not a
# sprite - 88x48, tiled along the whole rim. The shipped P03 tile has a measured seam of
# exactly 0.00 on both axes, so a replacement has to be at least as continuous; a visible
# vertical cut repeated 40 times around the island reads as a rendering bug.
WALL_SPEC = {
  'id': 'curtain_wall',
  'source': 'retaining_wall.png',
  'file': 'retaining_wall.png',
  'size': (88, 48),
  'replaces': 'retaining_wall',
}

# Vegetation comes from its own painted sheet: three trees, three inherited canvases.
TREE_SHEET = 'tree_set_01.png'
TREE_SPECS = [
  ('sakura_a', 0, (114, 132), 'canopy_a'),
  ('pine',     1, (96, 128),  'pine'),
  ('oak',      2, (112, 136), 'oak'),
]


def neutral_background_cutout(image: Image.Image, tolerance: int = 14,
                              min_value: int = 96) -> Image.Image:
    """Flood-fill the flat plate from the border; keep every enclosed pixel.

    A pixel is background only if it is neutral (max-min channel <= tolerance) and not
    dark (any channel >= min_value). The darkness floor protects dark slate roofs and
    timber that touch the frame edge; the neutrality test protects warm plaster.
    """
    src = image.convert('RGB')
    width, height = src.size
    data = src.tobytes()
    candidate = bytearray(width * height)
    for index in range(width * height):
        offset = index * 3
        r, g, b = data[offset], data[offset + 1], data[offset + 2]
        if max(r, g, b) - min(r, g, b) <= tolerance and max(r, g, b) >= min_value:
            candidate[index] = 1

    exterior = bytearray(width * height)
    queue: deque[int] = deque()
    for index in (*range(width), *range((height - 1) * width, height * width),
                  *(y * width for y in range(1, height - 1)),
                  *(y * width + width - 1 for y in range(1, height - 1))):
        if candidate[index] and not exterior[index]:
            exterior[index] = 1
            queue.append(index)
    while queue:
        index = queue.popleft()
        x, y = index % width, index // width
        for neighbour in ((index - 1 if x else -1), (index + 1 if x + 1 < width else -1),
                          (index - width if y else -1),
                          (index + width if y + 1 < height else -1)):
            if neighbour >= 0 and candidate[neighbour] and not exterior[neighbour]:
                exterior[neighbour] = 1
                queue.append(neighbour)

    alpha = Image.frombytes('L', (width, height), bytes(255 * (1 - v) for v in exterior))
    out = src.convert('RGBA')
    out.putalpha(alpha)
    return out


def crop_to_content(image: Image.Image, margin: int = 2) -> Image.Image:
    box = image.getchannel('A').getbbox()
    if box is None:
        return image
    left, top, right, bottom = box
    return image.crop((max(0, left - margin), max(0, top - margin),
                       min(image.width, right + margin), min(image.height, bottom + margin)))


def foot_padding(path: Path) -> int:
    """Rows of fully transparent pixels at the bottom of a shipped sprite.

    This is not cosmetic. For trees, `ObliqueSpriteGeometry.drawFootOffset()` IS this
    number (`bounds(image).bottomPadding * pixelScale`), so a re-cut tree with 0 padding
    draws 5 px lower than the art was authored against and sinks into the ground. Caught
    by MapForgeP22ContractTest, which is why the new art must inherit it, not re-invent it.
    """
    if not path.is_file():
        return 0
    alpha = Image.open(path).convert('RGBA').getchannel('A')
    width, height = alpha.size
    px = alpha.load()
    for y in range(height - 1, -1, -1):
        if any(px[x, y] > 128 for x in range(width)):
            return height - 1 - y
    return height


def to_canvas(image: Image.Image, size: tuple[int, int], foot_pad: int = 0) -> Image.Image:
    """Fit the building into the inherited canvas, anchored to the bottom-centre pivot.

    `foot_pad` keeps the SHIPPED number of transparent rows under the sprite, because the
    renderer derives the tree foot offset from it (see foot_padding()).
    """
    content = crop_to_content(image)
    usable_h = max(1, size[1] - foot_pad)
    scale = min(size[0] / content.width, usable_h / content.height)
    # 0.98 keeps a one-pixel transparent rim so no cut edge sits exactly on the canvas
    # border, which is where the renderer's shadow silhouette would smear.
    target = (max(1, int(content.width * scale * 0.98)),
              max(1, int(content.height * scale * 0.98)))
    resized = content.resize(target, Image.Resampling.BOX)
    # Binary alpha again: BOX resampling creates in-between alpha values on the rim.
    resized.putalpha(resized.getchannel('A').point(lambda v: 255 if v >= 128 else 0))
    canvas = Image.new('RGBA', size, (0, 0, 0, 0))
    x = (size[0] - resized.width) // 2
    y = size[1] - foot_pad - resized.height
    canvas.paste(resized, (x, y))
    return canvas


def inherited_foot_pad(*candidates: Path) -> int:
    for candidate in candidates:
        if candidate.is_file():
            return foot_padding(candidate)
    return 0


def slice_items(cut: Image.Image) -> list[Image.Image]:
    """Split a painted prop sheet into items using empty vertical bands.

    Detected from the alpha channel rather than hard-coded pixel columns, so the tool
    still works if the sheet is re-drawn at another resolution.
    """
    alpha = cut.getchannel('A')
    width, height = cut.size
    occupied = [any(alpha.getpixel((x, y)) > 0 for y in range(0, height, 4))
                for x in range(width)]
    runs: list[tuple[int, int]] = []
    start = None
    for x, filled in enumerate(occupied + [False]):
        if filled and start is None:
            start = x
        elif not filled and start is not None:
            runs.append((start, x))
            start = None
    items = []
    for left, right in runs:
        rows = [y for y in range(height)
                if any(alpha.getpixel((x, y)) > 0 for x in range(left, right, 2))]
        if not rows:
            continue
        items.append(cut.crop((left, rows[0], right, rows[-1] + 1)))
    return items


def vertical_seam(image: Image.Image) -> float:
    """Mean |RGB| difference between the tile's right and left edge columns."""
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
    return total / pairs if pairs else 0.0


def best_wall_shift(image: Image.Image) -> tuple[int, int]:
    """Circular shift that puts the most continuous pair of neighbour columns at the seam.

    Rotation is used instead of a cross-fade because this is 1:1 crisp pixel art: fading a
    band across the joint turns 4-6 columns of sharp blocks into mush, which is more visible
    than the joint it removes. See tools/tile_seamless.py for the standalone version.
    """
    width = image.width
    px = image.load()
    best_s, best_cost = 0, None
    for shift in range(width):
        cost = 0
        for y in range(image.height):
            a, b = px[(shift - 1) % width, y], px[shift % width, y]
            if a[3] == 0 or b[3] == 0:
                continue
            cost += abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2])
        if best_cost is None or cost < best_cost:
            best_s, best_cost = shift, cost
    return best_s, best_cost or 0


def luma(image: Image.Image) -> float:
    pixels = [p for p in image.convert('RGBA').getdata() if p[3] > 128]
    if not pixels:
        return 0.0
    total = sum(0.299 * r + 0.587 * g + 0.114 * b for r, g, b, _ in pixels)
    return total / len(pixels)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    manifest = {
        'schema': 'eternal-summoner.p27b2.structures.v1',
        'status': 'GAME_CANDIDATE',
        'note': 'Houses re-cut into the inherited P03 canvas/pivot contracts. Canvas size, '
                'pivot and collider are unchanged; only the art is new.',
        'assets': [],
    }
    for name, spec in SPECS.items():
        source_path = SOURCE / f'{name}.png'
        if not source_path.is_file():
            print(f'skip {name}: no source')
            continue
        cut = neutral_background_cutout(Image.open(source_path))
        canvas = to_canvas(cut, spec['size'], inherited_foot_pad(OLD / spec['replaces']))
        out_path = OUT / spec['file']
        canvas.save(out_path, optimize=True)
        old_path = OLD / spec['replaces']
        record = {
            'id': name,
            'file': f'structures/{spec["file"]}',
            'size': list(canvas.size),
            'replaces': f'sanctuary_p03/{spec["replaces"]}',
            'pivot': [spec['size'][0] // 2, spec['size'][1]],
            'alpha': 'binary RGBA',
            'source_path': str(source_path.relative_to(ROOT)),
            'source_sha256': hashlib.sha256(source_path.read_bytes()).hexdigest(),
            'sha256': hashlib.sha256(out_path.read_bytes()).hexdigest(),
            'mean_luma': round(luma(canvas), 1),
            'old_mean_luma': round(luma(Image.open(old_path)), 1) if old_path.is_file() else None,
            'qa_status': 'GAME_CANDIDATE',
            'visual_qa': 'needs engine-side review at z0.88 and z1.48',
        }
        manifest['assets'].append(record)
        print(f'{name:20} -> {spec["file"]:18} luma {record["mean_luma"]:5.1f} '
              f'(was {record["old_mean_luma"]})  {out_path.stat().st_size / 1024:.0f} KB')
    # ---- props -------------------------------------------------------------
    prop_source = SOURCE / PROP_SHEET
    if prop_source.is_file():
        items = slice_items(neutral_background_cutout(Image.open(prop_source)))
        if len(items) != len(PROP_SPECS):
            raise SystemExit(f'prop sheet: expected {len(PROP_SPECS)} items, sliced {len(items)}')
        for (name, _index, size, replaces), item in zip(PROP_SPECS, items):
            canvas = to_canvas(item, size, inherited_foot_pad(OLD / f'{replaces}.png'))
            out_path = OUT / f'{replaces}.png'
            canvas.save(out_path, optimize=True)
            old_path = OLD / f'{replaces}.png'
            record = {
                'id': name,
                'file': f'structures/{replaces}.png',
                'size': list(canvas.size),
                'replaces': f'sanctuary_p03/{replaces}',
                'pivot': [size[0] // 2, size[1]],
                'alpha': 'binary RGBA',
                'source_path': str(prop_source.relative_to(ROOT)),
                'source_sha256': hashlib.sha256(prop_source.read_bytes()).hexdigest(),
                'sha256': hashlib.sha256(out_path.read_bytes()).hexdigest(),
                'mean_luma': round(luma(canvas), 1),
                'old_mean_luma': round(luma(Image.open(old_path)), 1) if old_path.is_file() else None,
                'qa_status': 'GAME_CANDIDATE',
                'visual_qa': 'needs engine-side review',
            }
            manifest['assets'].append(record)
            print(f'{name:20} -> {replaces+".png":18} luma {record["mean_luma"]:5.1f} '
                  f'(was {record["old_mean_luma"]})')
    # ---- facilities --------------------------------------------------------
    premium = ROOT / 'app/src/main/assets/premium_slice'
    for name, spec in FACILITY_SPECS.items():
        source_path = SOURCE / f'{name}.png'
        if not source_path.is_file():
            print(f'skip {name}: no source')
            continue
        canvas = to_canvas(neutral_background_cutout(Image.open(source_path)), spec['size'],
                           inherited_foot_pad(premium / f'{spec["replaces_premium"]}.png'))
        out_path = OUT / spec['file']
        canvas.save(out_path, optimize=True)
        old_path = premium / f'{spec["replaces_premium"]}.png'
        record = {
            'id': name,
            'file': f'structures/{spec["file"]}',
            'size': list(canvas.size),
            'replaces': f'premium_slice/{spec["replaces_premium"]}',
            'pivot': [spec['size'][0] // 2, spec['size'][1]],
            'alpha': 'binary RGBA',
            'source_path': str(source_path.relative_to(ROOT)),
            'source_sha256': hashlib.sha256(source_path.read_bytes()).hexdigest(),
            'sha256': hashlib.sha256(out_path.read_bytes()).hexdigest(),
            'mean_luma': round(luma(canvas), 1),
            'old_mean_luma': round(luma(Image.open(old_path)), 1) if old_path.is_file() else None,
            'qa_status': 'GAME_CANDIDATE',
            'visual_qa': 'needs engine-side review at z0.88 and z1.48',
        }
        manifest['assets'].append(record)
        print(f'{name:24} -> {spec["file"]:18} luma {record["mean_luma"]:5.1f} '
              f'(was {record["old_mean_luma"]})')

    # ---- debt A remainder: towers + hero registry --------------------------
    for name, spec in DEBT_A_SPECS.items():
        source_path = SOURCE / f'{name}.png'
        if not source_path.is_file():
            print(f'skip {name}: no source')
            continue
        canvas = to_canvas(neutral_background_cutout(Image.open(source_path)), spec['size'],
                           inherited_foot_pad(premium / f'{spec.get("replaces_premium", "")}.png',
                                              OLD / f'{spec.get("replaces", "")}.png'))
        out_path = OUT / spec['file']
        canvas.save(out_path, optimize=True)
        if 'replaces_premium' in spec:
            old_path = premium / f'{spec["replaces_premium"]}.png'
            replaces = f'premium_slice/{spec["replaces_premium"]}'
        else:
            old_path = OLD / f'{spec["replaces"]}.png'
            replaces = f'sanctuary_p03/{spec["replaces"]}'
        record = {
            'id': name,
            'file': f'structures/{spec["file"]}',
            'size': list(canvas.size),
            'replaces': replaces,
            'pivot': [spec['size'][0] // 2, spec['size'][1]],
            'alpha': 'binary RGBA',
            'source_path': str(source_path.relative_to(ROOT)),
            'source_sha256': hashlib.sha256(source_path.read_bytes()).hexdigest(),
            'sha256': hashlib.sha256(out_path.read_bytes()).hexdigest(),
            'mean_luma': round(luma(canvas), 1),
            'old_mean_luma': round(luma(Image.open(old_path)), 1) if old_path.is_file() else None,
            'qa_status': 'GAME_CANDIDATE',
            'visual_qa': 'needs engine-side review; corner tower repeats around the wall',
        }
        manifest['assets'].append(record)
        print(f'{name:24} -> {spec["file"]:18} luma {record["mean_luma"]:5.1f} '
              f'(was {record["old_mean_luma"]})')

    # ---- curtain wall tile --------------------------------------------------
    wall_source = SOURCE / WALL_SPEC['source']
    if wall_source.is_file():
        # Full-bleed: a wall tile must own every column, unlike a sprite which keeps air
        # around it. Then rotate to the least-visible joint; see tools/tile_seamless.py.
        content = crop_to_content(neutral_background_cutout(Image.open(wall_source)))
        full = content.resize(WALL_SPEC['size'], Image.Resampling.BOX)
        full.putalpha(full.getchannel('A').point(lambda v: 255 if v >= 128 else 0))
        shifted, _ = best_wall_shift(full)
        width = full.width
        healed = Image.new('RGBA', full.size, (0, 0, 0, 0))
        healed.paste(full.crop((shifted, 0, width, full.height)), (0, 0))
        healed.paste(full.crop((0, 0, shifted, full.height)), (width - shifted, 0))
        seam_before = vertical_seam(full)
        seam_after = vertical_seam(healed)
        out_path = OUT / WALL_SPEC['file']
        healed.save(out_path, optimize=True)
        old_path = OLD / f"{WALL_SPEC['replaces']}.png"
        manifest['assets'].append({
            'id': WALL_SPEC['id'],
            'file': f"structures/{WALL_SPEC['file']}",
            'size': list(healed.size),
            'replaces': f"sanctuary_p03/{WALL_SPEC['replaces']}",
            'pivot': [WALL_SPEC['size'][0] // 2, WALL_SPEC['size'][1]],
            'alpha': 'binary RGBA',
            'tiling': 'horizontal',
            'seam_shift_px': shifted,
            'seam_before': round(seam_before, 2),
            'seam_after': round(seam_after, 2),
            'source_path': str(wall_source.relative_to(ROOT)),
            'source_sha256': hashlib.sha256(wall_source.read_bytes()).hexdigest(),
            'sha256': hashlib.sha256(out_path.read_bytes()).hexdigest(),
            'mean_luma': round(luma(healed), 1),
            'old_mean_luma': round(luma(Image.open(old_path)), 1) if old_path.is_file() else None,
            'qa_status': 'GAME_CANDIDATE',
            'visual_qa': 'tiled around the whole perimeter; check at z0.36 and z1.48',
        })
        print(f"{WALL_SPEC['id']:20} -> {WALL_SPEC['file']:18} seam {seam_before:5.1f} -> "
              f"{seam_after:5.1f} (shift {shifted}px)")

    # ---- vegetation --------------------------------------------------------
    tree_source = SOURCE / TREE_SHEET
    if tree_source.is_file():
        trees = slice_items(neutral_background_cutout(Image.open(tree_source)))
        if len(trees) != len(TREE_SPECS):
            raise SystemExit(f'tree sheet: expected {len(TREE_SPECS)} items, sliced {len(trees)}')
        for (name, _index, size, replaces), item in zip(TREE_SPECS, trees):
            # Trees MUST keep the shipped foot padding: it is the draw offset.
            canvas = to_canvas(item, size, inherited_foot_pad(OLD / f'{replaces}.png'))
            out_path = OUT / f'{replaces}.png'
            canvas.save(out_path, optimize=True)
            old_path = OLD / f'{replaces}.png'
            record = {
                'id': name,
                'file': f'structures/{replaces}.png',
                'size': list(canvas.size),
                'replaces': f'sanctuary_p03/{replaces}',
                'pivot': [size[0] // 2, size[1]],
                'alpha': 'binary RGBA',
                'source_path': str(tree_source.relative_to(ROOT)),
                'source_sha256': hashlib.sha256(tree_source.read_bytes()).hexdigest(),
                'sha256': hashlib.sha256(out_path.read_bytes()).hexdigest(),
                'mean_luma': round(luma(canvas), 1),
                'old_mean_luma': round(luma(Image.open(old_path)), 1) if old_path.is_file() else None,
                'qa_status': 'GAME_CANDIDATE',
                'visual_qa': 'needs engine-side review for canopy occlusion',
            }
            manifest['assets'].append(record)
            print(f'{name:20} -> {replaces+".png":18} luma {record["mean_luma"]:5.1f} '
                  f'(was {record["old_mean_luma"]})')
    (OUT.parent / 'manifest_structures.json').write_text(json.dumps(manifest, indent=2) + '\n')


if __name__ == '__main__':
    main()
