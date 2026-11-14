#!/usr/bin/env python3
"""Normalize original enemy sheets using fixed clip-space roots, not per-frame boxes.

The imagegen sheets contain six physical figures in each row. Some weapons cross
nominal cell boundaries. Connected silhouettes recover them intact; placement
still uses each figure's logical slot and one fixed root/scale for its whole row.
Requires Pillow, NumPy and SciPy (offline authoring only).
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
OUT = ROOT / 'app/src/main/assets/art_completion_v1/enemies'
CELL = (128, 144)
PIVOT = (64, 134)
ORDER = ['idle0', 'idle1', 'walk0', 'walk1', 'walk2', 'walk3',
         'anticipation', 'contact', 'recovery', 'hurt', 'collapse', 'defeat']
SPECS = {
    'forest_wraith': dict(type='FOREST_WRAITH', scale=.33, root_x=181,
                         baselines=[343, 336], world_scale=.82),
    'elder_treant': dict(type='ELDER_TREANT', scale=.32, root_x=181,
                        baselines=[340, 333], world_scale=1.05),
    'ruin_sentinel': dict(type='RUIN_SENTINEL', scale=.30, root_x=189,
                         baselines=[345, 345], world_scale=.82),
    'abyssal_executioner': dict(type='ABYSSAL_EXECUTIONER', scale=.30, root_x=185,
                               baselines=[352, 356], world_scale=1.05),
}


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def recover_row(image: Image.Image) -> list[tuple[Image.Image, tuple[int, int, int, int], int]]:
    pixels = np.asarray(image).copy()
    opaque = pixels[:, :, 3] >= 96
    labels, _ = ndimage.label(opaque, np.ones((3, 3)))
    counts = np.bincount(labels.ravel())
    objects = ndimage.find_objects(labels)
    figures = sorted([i + 1 for i, box in enumerate(objects)
                      if box is not None and counts[i + 1] > 10_000],
                     key=lambda i: objects[i - 1][1].start)
    assert len(figures) == 6, ('Expected six complete connected figures', figures)
    major = np.isin(labels, figures)
    _, nearest = ndimage.distance_transform_edt(~major, return_indices=True)
    owners = labels[tuple(nearest)]
    result = []
    for label in figures:
        selected = opaque & (owners == label)
        ys, xs = np.nonzero(selected)
        bounds = (int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1)
        foreground = np.zeros_like(pixels)
        foreground[selected] = pixels[selected]
        foreground[selected, 3] = 255
        result.append((Image.fromarray(foreground).crop(bounds), bounds, int(selected.sum())))
    assert sum(r[2] for r in result) == int(opaque.sum()), 'All source foreground belongs to one recovered figure'
    return result


def build(name: str, spec: dict) -> dict:
    source_path = HERE / 'source' / f'{name}.png'
    original = Image.open(source_path).convert('RGBA')
    width, height = original.size
    assert width % 6 == 0 and height % 2 == 0
    logical_width, row_height = width // 6, height // 2
    poses = []
    records = []
    for row in range(2):
        source_row = original.crop((0, row * row_height, width, (row + 1) * row_height))
        for col, (figure, bounds, source_count) in enumerate(recover_row(source_row)):
            scale = spec['scale']
            resized = figure.resize((round(figure.width * scale), round(figure.height * scale)), Image.Resampling.NEAREST)
            x = round(PIVOT[0] + (bounds[0] - col * logical_width - spec['root_x']) * scale)
            y = round(PIVOT[1] + (bounds[1] - spec['baselines'][row]) * scale)
            target = Image.new('RGBA', CELL)
            target.alpha_composite(resized, (x, y))
            before = sum(resized.getchannel('A').histogram()[1:])
            after = sum(target.getchannel('A').histogram()[1:])
            assert before == after, (name, row, col, 'clipped pixels', before - after, x, y)
            final_bounds = target.getbbox()
            assert final_bounds and 0 < final_bounds[0] < final_bounds[2] < CELL[0]
            assert 0 < final_bounds[1] < final_bounds[3] < CELL[1], (name, row, col, final_bounds)
            poses.append(target)
            records.append(dict(pose=ORDER[row * 6 + col], source_bounds=list(bounds),
                                source_opaque_pixels=source_count, paste=[x, y],
                                output_bounds=list(final_bounds), output_opaque_pixels=after,
                                clipped_pixels=before - after))
    atlas = Image.new('RGBA', (CELL[0] * 12, CELL[1]))
    for i, frame in enumerate(poses):
        atlas.paste(frame, (i * CELL[0], 0))
    alpha = atlas.getchannel('A')
    atlas = atlas.convert('RGB').quantize(colors=128, method=Image.Quantize.MEDIANCUT,
                                        dither=Image.Dither.NONE).convert('RGBA')
    atlas.putalpha(alpha)
    hashes = [hashlib.sha256(atlas.crop((i * CELL[0], 0, (i + 1) * CELL[0], CELL[1])).tobytes()).hexdigest()
              for i in range(12)]
    assert len(set(hashes)) == 12, (name, 'Repeated normalized pose')
    directory = OUT / name
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / 'atlas.png'
    atlas.save(path, optimize=True)
    meta = dict(version=1, type=spec['type'], frame=list(CELL), pivot=list(PIVOT),
                columns=12, rows=1, world_scale=spec['world_scale'], atlas_file='atlas.png',
                atlas_sha256=sha(path), frame_order=ORDER,
                retained_rgba_bytes=CELL[0] * CELL[1] * 12 * 4,
                native_generated_dimensions_claimed=False,
                left_direction='Mirrored by runtime at draw; no separately authored left claim',
                source=dict(file=str(source_path.relative_to(ROOT)), sha256=sha(source_path),
                            dimensions=list(original.size), grid=[6, 2], alpha_threshold=96),
                normalization=dict(fixed_scale=spec['scale'], fixed_root_x=spec['root_x'],
                                   row_baselines=spec['baselines'], logical_source_cell=[logical_width, row_height],
                                   extraction='Connected silhouettes plus nearest detached clusters; no weapon crop at nominal cell boundaries',
                                   per_frame_recentering=False, per_frame_rescaling=False),
                unique_pose_hashes=hashes)
    (directory / 'metadata.json').write_text(json.dumps(meta, indent=2) + '\n')
    evidence = HERE / name
    evidence.mkdir(exist_ok=True)
    (evidence / 'normalization-report.json').write_text(json.dumps(dict(metadata=meta, poses=records), indent=2) + '\n')
    contact = Image.new('RGB', (6 * 256, 2 * 312), '#26313a')
    draw = ImageDraw.Draw(contact)
    frames = []
    for i in range(12):
        frame = atlas.crop((i * CELL[0], 0, (i + 1) * CELL[0], CELL[1]))
        frames.append(frame)
        x, y = i % 6 * 256, i // 6 * 312
        draw.text((x + 6, y + 4), ORDER[i], fill='#ead7ae')
        enlarged = frame.resize((256, 288), Image.Resampling.NEAREST)
        contact.paste(enlarged, (x, y + 24), enlarged)
        draw.line((x, y + 24 + PIVOT[1] * 2, x + 256, y + 24 + PIVOT[1] * 2), fill='#547269')
    contact.save(evidence / 'contact.png', optimize=True)
    for clip, indices, duration in [('idle', [0, 1], 350), ('walk', [2, 3, 4, 5], 110),
                                    ('attack', [6, 7, 8], 160), ('defeat', [9, 10, 11], 260)]:
        animation = [frames[i] for i in indices]
        animation[0].save(evidence / f'{clip}.webp', save_all=True, append_images=animation[1:],
                          lossless=True, exact=True, duration=duration, loop=0)
    return dict(type=spec['type'], poses=12, retained_bytes=meta['retained_rgba_bytes'],
                atlas_sha256=meta['atlas_sha256'], clipped_pixels=sum(r['clipped_pixels'] for r in records))


if __name__ == '__main__':
    results = [build(name, spec) for name, spec in SPECS.items()]
    total = sum(item['retained_bytes'] for item in results)
    assert total == 3_538_944 and total < 4 * 1024 * 1024
    report = dict(result='PASS', enemies=results, total_frames=48, retained_rgba_bytes=total,
                  physical_device_validation='NOT TESTED', runtime_integration='Root task; not claimed by asset writer')
    (HERE / 'validation.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))
