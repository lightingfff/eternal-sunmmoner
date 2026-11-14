#!/usr/bin/env python3
"""Normalize genuine interior furniture sources onto the unchanged runtime canvases."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
OUT = ROOT / 'app/src/main/assets/art_completion_v1/interiors'
THRESHOLD = 96


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def binary_rgba(path: Path) -> Image.Image:
    image = Image.open(path).convert('RGBA')
    assert image.getchannel('A').getextrema()[0] == 0, (path, 'not a transparent source')
    image.putalpha(image.getchannel('A').point(lambda alpha: 255 if alpha >= THRESHOLD else 0))
    return image


def spans(image: Image.Image) -> list[tuple[int, int]]:
    alpha = image.getchannel('A')
    result = []
    start = None
    for x in range(image.width):
        occupied = alpha.crop((x, 0, x + 1, image.height)).getbbox() is not None
        if occupied and start is None:
            start = x
        elif not occupied and start is not None:
            result.append((start, x))
            start = None
    if start is not None:
        result.append((start, image.width))
    return result


def clean_palette(image: Image.Image) -> Image.Image:
    alpha = image.getchannel('A')
    quantized = image.convert('RGB').quantize(colors=128, method=Image.Quantize.MEDIANCUT,
                                            dither=Image.Dither.NONE).convert('RGBA')
    quantized.putalpha(alpha)
    clean = Image.new('RGBA', image.size)
    clean.paste(quantized, (0, 0), alpha)
    return clean


def build() -> dict:
    main_path = HERE / 'source/eight-furnishings-clean.png'
    wide_path = HERE / 'source/wide-furnishings.png'
    main = binary_rgba(main_path)
    wide = binary_rgba(wide_path)
    assert main.size == (1774, 887) and wide.size == (2172, 724)
    source_objects = {}
    for row in range(2):
        top = round(row * main.height / 2)
        bottom = round((row + 1) * main.height / 2)
        strip = main.crop((0, top, main.width, bottom))
        occupied = spans(strip)
        assert len(occupied) == 4, (row, occupied)
        for column, (left, right) in enumerate(occupied):
            crop = strip.crop((left, 0, right, strip.height))
            bounds = crop.getbbox()
            assert bounds is not None
            source_objects[f'main_{row * 4 + column}'] = (
                crop.crop(bounds), main_path,
                [left + bounds[0], top + bounds[1], left + bounds[2], top + bounds[3]])
    occupied = spans(wide)
    assert len(occupied) == 3, occupied
    for column, (left, right) in enumerate(occupied):
        crop = wide.crop((left, 0, right, wide.height))
        bounds = crop.getbbox()
        assert bounds is not None
        source_objects[f'wide_{column}'] = (
            crop.crop(bounds), wide_path,
            [left + bounds[0], bounds[1], left + bounds[2], bounds[3]])

    specs = (
        ('forge_anvil', (96, 80), (48, 72), 'main_0', 76, 68),
        ('armor_stand', (64, 96), (32, 88), 'main_1', 44, 68),
        ('personal_chest', (64, 48), (32, 40), 'main_2', 46, 34),
        ('care_table', (112, 80), (56, 72), 'wide_0', 94, 58),
        ('long_table', (108, 68), (54, 62), 'wide_1', 100, 56),
        ('bookshelf', (56, 64), (28, 58), 'main_5', 48, 54),
        ('tavern_counter', (160, 80), (80, 74), 'wide_2', 148, 70),
        ('recovery_bed', (96, 72), (48, 64), 'main_7', 78, 62),
    )
    OUT.mkdir(parents=True, exist_ok=True)
    entries = []
    contact = Image.new('RGB', (1536, 720), '#17252d')
    draw = ImageDraw.Draw(contact)
    for index, (name, cell, pivot, source_id, max_width, max_height) in enumerate(specs):
        crop, path, bounds = source_objects[source_id]
        scale = min(max_width / crop.width, max_height / crop.height)
        resized = crop.resize((round(crop.width * scale), round(crop.height * scale)),
                              Image.Resampling.NEAREST)
        x = (cell[0] - resized.width) // 2
        y = pivot[1] - resized.height
        target = Image.new('RGBA', cell)
        target.alpha_composite(resized, (x, y))
        source_count = sum(resized.getchannel('A').histogram()[1:])
        output_count = sum(target.getchannel('A').histogram()[1:])
        assert source_count == output_count, (name, 'clipped', source_count - output_count)
        target = clean_palette(target)
        box = target.getbbox()
        assert box and box[0] > 0 and box[1] > 0 and box[2] < cell[0] and box[3] < cell[1]
        assert set(target.getchannel('A').tobytes()) == {0, 255}
        filename = f'{name}.png'
        target.save(OUT / filename, optimize=True)
        colors = target.convert('RGB').getcolors(target.width * target.height)
        assert colors is not None and len(colors) <= 128
        entries.append(dict(id=name, file=filename, width=cell[0], height=cell[1],
                            pivot=list(pivot), alpha_bbox=list(box), sha256=digest(OUT / filename),
                            rgba_bytes=cell[0] * cell[1] * 4, source=str(path.relative_to(ROOT)),
                            source_sha256=digest(path), source_alpha_bbox=bounds, fixed_scale=scale,
                            paste=[x, y], opaque_pixels=output_count,
                            alpha_threshold=THRESHOLD, unique_rgb_colors=len(colors),
                            clipped_opaque_pixels=source_count - output_count,
                            normalization='one uniform scale per static prop; nearest; binary alpha;128-color palette'))
        row, col = divmod(index, 4)
        cx = col * 384 + 192
        oy = row * 360
        draw.text((col * 384 + 8, oy + 6), name, fill='#e7d5ad')
        draw.text((col * 384 + 8, oy + 24), f'{cell[0]}x{cell[1]} pivot{pivot}', fill='#b3c1c8')
        contact.paste(target, (cx - cell[0] // 2, oy + 132 - cell[1]), target)
        enlarged = target.resize((cell[0] * 2, cell[1] * 2), Image.Resampling.NEAREST)
        contact.paste(enlarged, (cx - enlarged.width // 2, oy + 346 - enlarged.height), enlarged)

    report = dict(version=1, pack='art_completion_v1/interiors',
                  origin='Genuine original hosted imagegen furniture; normalized derivatives, not native-size claims',
                  sources=[dict(file=str(path.relative_to(ROOT)), sha256=digest(path),
                                dimensions=list(Image.open(path).size))
                           for path in (HERE / 'source/eight-furnishings-v1.png', main_path, wide_path)],
                  source_notes='Initial source had semitransparent white matte and is never used for runtime. A genuine imagegen background-removal correction produced the transparent source. Three broader furniture designs were then generated to fit old wide canvases without nonuniform distortion.',
                  projection='2.5D oblique .72 ground; upright height, NW key/cool right shade',
                  props=entries, retained_rgba_bytes=sum(entry['rgba_bytes'] for entry in entries),
                  runtime_shadow_baked=False, native_generated_dimensions_claimed=False)
    assert report['retained_rgba_bytes'] == 225_984 < 1024 * 1024
    (OUT / 'manifest.json').write_text(json.dumps(report, indent=2) + '\n')
    (HERE / 'normalization-report.json').write_text(json.dumps(report, indent=2) + '\n')
    contact.save(HERE / 'contact.png')
    print(json.dumps(dict(result='PASS', props=len(entries), retained_rgba_bytes=report['retained_rgba_bytes'],
                          alpha='binary', clipped_pixels=0)))
    return report


if __name__ == '__main__':
    build()
