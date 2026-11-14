#!/usr/bin/env python3
"""Normalize genuine generated props onto the existing game geometry's canvases.

This script crops/resamples/quantizes existing artwork; it does not paint replacement
objects. Each static object has one uniform scale and one fixed ground anchor.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
SOURCE = HERE / 'source/three-props-v1.png'
OUT = ROOT / 'app/src/main/assets/art_completion_v1/props'


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def quantized_rgba(image: Image.Image) -> Image.Image:
    alpha = image.getchannel('A')
    image = image.convert('RGB').quantize(colors=128, method=Image.Quantize.MEDIANCUT,
                                        dither=Image.Dither.NONE).convert('RGBA')
    image.putalpha(alpha)
    # Remove hidden RGB fringe too. Runtime premultiplication must not reveal keying colors.
    clean = Image.new('RGBA', image.size)
    clean.paste(image, (0, 0), alpha)
    return clean


def build() -> dict:
    source = Image.open(SOURCE).convert('RGBA')
    source.putalpha(source.getchannel('A').point(lambda value: 255 if value >= 96 else 0))
    assert source.size == (2172, 724)
    definitions = (
        dict(id='royal_statue', cell=(52, 88), pivot=(26, 82), column=0,
             max_width=46, max_height=80, bottom=82),
        dict(id='crimson_crystal', cell=(48, 64), pivot=(24, 60), column=1,
             max_width=42, max_height=56, bottom=60),
        dict(id='tattered_banner', cell=(80, 80), pivot=(40, 76), column=2,
             source_pole_x=281, max_right_extent=32, max_height=72, bottom=76),
    )
    OUT.mkdir(parents=True, exist_ok=True)
    entries = []
    contact = Image.new('RGB', (768, 440), '#18252b')
    draw = ImageDraw.Draw(contact)
    for index, spec in enumerate(definitions):
        column = spec['column']
        panel = source.crop((column * 724, 0, (column + 1) * 724, 724))
        bounds = panel.getbbox()
        assert bounds and bounds[1] > 0 and bounds[3] < 724
        cropped = panel.crop(bounds)
        if 'source_pole_x' in spec:
            scale = min(spec['max_height'] / cropped.height,
                        spec['max_right_extent'] / (bounds[2] - spec['source_pole_x']))
            x = round(spec['pivot'][0] + (bounds[0] - spec['source_pole_x']) * scale)
        else:
            scale = min(spec['max_width'] / cropped.width, spec['max_height'] / cropped.height)
            x = (spec['cell'][0] - round(cropped.width * scale)) // 2
        resized = cropped.resize((round(cropped.width * scale), round(cropped.height * scale)),
                                 Image.Resampling.NEAREST)
        y = spec['bottom'] - resized.height
        target = Image.new('RGBA', spec['cell'])
        target.alpha_composite(resized, (x, y))
        before = sum(resized.getchannel('A').histogram()[1:])
        after = sum(target.getchannel('A').histogram()[1:])
        assert before == after, (spec['id'], 'normalization clipped opaque pixels')
        target = quantized_rgba(target)
        output_bounds = target.getbbox()
        assert output_bounds and output_bounds[0] > 0 and output_bounds[1] > 0
        assert output_bounds[2] < target.width and output_bounds[3] < target.height
        assert set(target.getchannel('A').tobytes()) == {0, 255}
        path = OUT / f"{spec['id']}.png"
        target.save(path, optimize=True)
        entry = dict(id=spec['id'], file=path.name, width=target.width, height=target.height,
                     pivot=list(spec['pivot']), alpha_bbox=list(output_bounds), sha256=sha(path),
                     rgba_bytes=target.width * target.height * 4,
                     source_column=column, source_panel=[column * 724, 0, (column + 1) * 724, 724],
                     source_alpha_bbox=list(bounds), fixed_scale=scale, paste=[x, y],
                     alpha_threshold=96, unique_rgb_colors=len(target.convert('RGB').getcolors(target.width * target.height)),
                     normalization='single uniform static-prop scale; nearest; binary alpha;128-color palette',
                     clipped_opaque_pixels=before - after)
        if 'source_pole_x' in spec:
            entry['source_pole_x'] = spec['source_pole_x']
            entry['pole_anchor_x'] = spec['pivot'][0]
        entries.append(entry)
        center = 128 + index * 256
        draw.text((index * 256 + 10, 8), spec['id'], fill='#e4d4ab')
        draw.text((index * 256 + 10, 26), f"{target.width}x{target.height}, pivot{spec['pivot']}", fill='#b9c4c8')
        contact.paste(target, (center - target.width // 2, 120 - target.height), target)
        enlarged = target.resize((target.width * 3, target.height * 3), Image.Resampling.NEAREST)
        contact.paste(enlarged, (center - enlarged.width // 2, 420 - enlarged.height), enlarged)
    report = dict(version=1, pack='art_completion_v1/props', source=str(SOURCE.relative_to(ROOT)),
                  source_sha256=sha(SOURCE), source_dimensions=list(source.size),
                  origin='Genuine original hosted imagegen output; normalized derivatives, not native-size claims',
                  generator_output='/workspace/generated_images/exec-e9b2a027-661e-49a2-bc96-065e14346281.png',
                  projection='2.5D oblique .72 ground; upright sprite height; NW key/cool right shade',
                  props=entries, retained_rgba_bytes=sum(entry['rgba_bytes'] for entry in entries),
                  runtime_shadow_baked=False, native_generated_dimensions_claimed=False)
    assert report['retained_rgba_bytes'] == 56_192 < 1024 * 1024
    (OUT / 'manifest.json').write_text(json.dumps(report, indent=2) + '\n')
    (HERE / 'normalization-report.json').write_text(json.dumps(report, indent=2) + '\n')
    contact.save(HERE / 'contact.png')
    print(json.dumps(dict(result='PASS', props=len(entries), retained_rgba_bytes=report['retained_rgba_bytes'],
                          alpha='binary', clipped_pixels=0)))
    return report


if __name__ == '__main__':
    build()
