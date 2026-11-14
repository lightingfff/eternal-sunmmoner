#!/usr/bin/env python3
"""Normalize genuine class sheets with one anatomical scale and logical root per class.

Connected components isolate touching x-projections (a thrust crosses a logical cell),
but placement always retains the logical source slot. Nothing is bbox-centered per pose.
Requires Pillow, NumPy and SciPy; none is part of the Android runtime.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import find_objects, label

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
OUT = ROOT / 'app/src/main/assets/art_completion_v1/classes'
ORDER = ['idle0', 'idle1', 'walk0', 'walk1', 'walk2', 'walk3',
         'anticipation', 'release', 'recovery', 'hurt', 'collapse', 'defeat']
SPECS = {
    'assassin': dict(scale=.34, root_x=85, baselines=[324, 319, 318, 318]),
    'priest': dict(scale=.36, root_x=90, baselines=[330, 314, 282, 272]),
    'berserker': dict(scale=.38, root_x=130, baselines=[245, 240, 243, 240]),
}
CELL = (112, 112)
PIVOT = (56, 104)


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def cleaned(image: Image.Image) -> Image.Image:
    data = np.array(image.convert('RGBA'))
    data[:, :, 3] = np.where(data[:, :, 3] >= 96, 255, 0)
    data[data[:, :, 3] == 0] = 0
    return Image.fromarray(data)


def isolate(image: Image.Image, count: int) -> list[tuple[Image.Image, dict]]:
    data = np.array(image)
    labels, _ = label(data[:, :, 3] > 0, structure=np.ones((3, 3), dtype=int))
    sizes = np.bincount(labels.ravel())
    rects = find_objects(labels)
    main_ids = sorted((i for i in range(1, len(sizes)) if sizes[i] > 5000),
                      key=lambda i: rects[i - 1][1].start + rects[i - 1][1].stop)
    assert len(main_ids) == count, ('Unexpected body components', sizes[main_ids], main_ids)
    groups = {i: [i] for i in main_ids}
    for i in range(1, len(sizes)):
        if i in groups:
            continue
        sy, sx = rects[i - 1]
        cx = (sx.start + sx.stop) / 2
        cy = (sy.start + sy.stop) / 2
        def distance(main):
            my, mx = rects[main - 1]
            dx = max(mx.start - cx, 0, cx - mx.stop)
            dy = max(my.start - cy, 0, cy - my.stop)
            return dx * dx + dy * dy
        groups[min(main_ids, key=distance)].append(i)
    result = []
    for main in main_ids:
        mask = np.isin(labels, groups[main])
        target = data.copy()
        target[~mask] = 0
        extracted = Image.fromarray(target)
        bounds = extracted.getbbox()
        assert bounds
        result.append((extracted.crop(bounds), dict(source_bbox=list(bounds),
                      source_opaque_pixels=int(mask.sum()), connected_islands=len(groups[main]))))
    assert sum(record['source_opaque_pixels'] for _, record in result) == int((data[:, :, 3] > 0).sum())
    return result


def quantize(image: Image.Image) -> Image.Image:
    alpha = image.getchannel('A')
    output = image.convert('RGB').quantize(colors=128, method=Image.Quantize.MEDIANCUT,
                                         dither=Image.Dither.NONE).convert('RGBA')
    output.putalpha(alpha)
    return output


def build():
    portraits_source = HERE / 'source/portraits.png'
    portraits = isolate(cleaned(Image.open(portraits_source)), 3)
    total_bytes = 0
    report = {}
    for who, spec in SPECS.items():
        source = HERE / who / 'source/sheet.png'
        sheet = cleaned(Image.open(source))
        poses = []
        records = []
        for row in range(4):
            y0, y1 = round(row * sheet.height / 4), round((row + 1) * sheet.height / 4)
            strip = sheet.crop((0, y0, sheet.width, y1))
            for column, (image, record) in enumerate(isolate(strip, 6)):
                left, top, _, _ = record['source_bbox']
                scale = spec['scale']
                scaled = image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.NEAREST)
                x = round(PIVOT[0] + (left - column * sheet.width / 6 - spec['root_x']) * scale)
                y = round(PIVOT[1] + (top - spec['baselines'][row]) * scale)
                pose = Image.new('RGBA', CELL)
                pose.alpha_composite(scaled, (x, y))
                before = sum(scaled.getchannel('A').histogram()[1:])
                after = sum(pose.getchannel('A').histogram()[1:])
                assert before == after, (who, row, column, 'clipped', before - after, x, y)
                bounds = pose.getbbox()
                assert bounds and 0 < bounds[0] < bounds[2] < CELL[0] and 0 < bounds[1] < bounds[3] < CELL[1], (who, row, column, bounds)
                poses.append(pose)
                records.append(dict(record, source_row=row, source_column=column,
                                    logical_root_x=spec['root_x'], source_baseline=spec['baselines'][row],
                                    scale=scale, paste=[x, y], normalized_bbox=list(bounds),
                                    normalized_opaque_pixels=after))
        atlas = Image.new('RGBA', (CELL[0] * 12, CELL[1] * 2))
        for index, pose in enumerate(poses):
            atlas.alpha_composite(pose, (index % 12 * CELL[0], index // 12 * CELL[1]))
        atlas = quantize(atlas)
        output = OUT / who
        output.mkdir(parents=True, exist_ok=True)
        atlas.save(output / f'{who}.png', optimize=True)
        portrait, portrait_record = portraits[list(SPECS).index(who)]
        portrait_scale = min(124 / portrait.width, 156 / portrait.height)
        portrait = portrait.resize((round(portrait.width * portrait_scale), round(portrait.height * portrait_scale)), Image.Resampling.NEAREST)
        portrait_canvas = Image.new('RGBA', (128, 160))
        portrait_canvas.alpha_composite(portrait, ((128 - portrait.width) // 2, 160 - portrait.height))
        quantize(portrait_canvas).save(output / 'portrait.png', optimize=True)
        poses = [atlas.crop((index % 12 * 112, index // 12 * 112,
                            index % 12 * 112 + 112, index // 12 * 112 + 112)) for index in range(24)]
        hashes = [hashlib.sha256(p.tobytes()).hexdigest() for p in poses]
        assert len(set(hashes)) == 24, (who, 'Repeated exact pose')
        decoded_bytes = 112 * 112 * 24 * 4 + 128 * 160 * 4
        total_bytes += decoded_bytes
        meta = {'class': who.upper(), 'frame': list(CELL), 'pivot': list(PIVOT), 'columns': 12,
                'rows': ['front_right', 'back_right'], 'frame_order': ORDER, 'world_scale': 1.08,
                'atlas_file': f'{who}.png', 'atlas_sha256': digest(output / f'{who}.png'),
                'portrait_file': 'portrait.png', 'portrait_sha256': digest(output / 'portrait.png'),
                'retained_rgba_bytes': decoded_bytes, 'unique_pose_hashes': hashes,
                'native_generated_dimensions_claimed': False,
                'left_direction': 'Draw-time horizontal mirror; not separately authored left art',
                'normalization': 'Nearest-only, binary alpha threshold96, shared anatomical scale/logical root per class; row baselines measured once; no per-frame recentering or body-height fitting',
                'source': {'path': str(source.relative_to(ROOT)), 'sha256': digest(source), 'dimensions': list(sheet.size)},
                'portrait_source': {'path': str(portraits_source.relative_to(ROOT)), 'sha256': digest(portraits_source), **portrait_record},
                'source_frames': records}
        (output / 'metadata.json').write_text(json.dumps(meta, indent=2) + '\n')
        (HERE / who / 'normalization-report.json').write_text(json.dumps(meta, indent=2) + '\n')
        contact = Image.new('RGBA', (112 * 12, 144 * 2), '#20313c')
        draw = ImageDraw.Draw(contact)
        for index, pose in enumerate(poses):
            x = index % 12 * 112; y = index // 12 * 144
            contact.alpha_composite(pose, (x, y))
            draw.text((x + 3, y + 115), ORDER[index % 12], fill='#e2d7b9')
        contact.convert('RGB').save(HERE / who / 'contact.png')
        for direction, offset in [('front', 0), ('back', 12)]:
            for action, indices, duration in [('idle', [0, 1], 350), ('walk', [2, 3, 4, 5], 111),
                                               ('attack', [6, 7, 8], 120), ('defeat', [9, 10, 11], 250)]:
                frames = [poses[offset + i].resize((336, 336), Image.Resampling.NEAREST) for i in indices]
                frames[0].save(HERE / who / f'{action}_{direction}.webp', save_all=True,
                               append_images=frames[1:], duration=duration, lossless=True,
                               exact=True, loop=0)
        report[who] = dict(poses=24, source_dimensions=sheet.size, scale=spec['scale'],
                           retained_rgba_bytes=decoded_bytes, alpha='binary', clipped_pixels=0,
                           unique_pose_hashes=len(set(hashes)))
    assert total_bytes == 3_858_432 and total_bytes < 4 * 1024 * 1024
    (HERE / 'validation.json').write_text(json.dumps(dict(classes=report, total_retained_bytes=total_bytes), indent=2) + '\n')
    print(json.dumps(dict(classes=report, total_retained_bytes=total_bytes), indent=2))


if __name__ == '__main__':
    build()
