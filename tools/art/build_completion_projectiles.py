#!/usr/bin/env python3
"""Normalize genuine flight glyphs at their physical tips, not at image bounding-box centres."""
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'docs/art/art-completion/projectiles/source.png'
OUTPUT = ROOT / 'app/src/main/assets/art_completion_v1/projectiles'
NAMES = ['BOLT', 'ARROW', 'ARCANE', 'HOLY', 'HOSTILE_ARROW']


def build():
    full = Image.open(SOURCE).convert('RGBA')
    atlas = Image.new('RGBA', (192, 160))
    records = []
    preview = Image.new('RGBA', (384, 5 * 90), '#172632')
    draw = ImageDraw.Draw(preview)
    for row, name in enumerate(NAMES):
        frames = []
        anchors = []
        for column in range(4):
            box = (round(column * full.width / 4), round(row * full.height / 5),
                   round((column + 1) * full.width / 4), round((row + 1) * full.height / 5))
            frame = full.crop(box)
            data = np.array(frame)
            data[:, :, 3] = np.where(data[:, :, 3] < 16, 0, data[:, :, 3])
            frame = Image.fromarray(data)
            opaque = data[:, :, 3] >= 128
            ys, xs = np.where(opaque)
            assert len(xs), (name, column, 'empty')
            tip_x = int(xs.max())
            tip_y = float(np.median(np.where(opaque[:, max(0, tip_x - 2):tip_x + 1])[0]))
            anchors.append((tip_x, tip_y))
            frames.append(frame)
        extents = [frame.getbbox() for frame in frames]
        scale = min([1.] + [38 / max(1, tip[0] - b[0]) for tip, b in zip(anchors, extents)] +
                    [14 / max(1, tip[1] - b[1], b[3] - tip[1]) for tip, b in zip(anchors, extents)])
        per_frame = []
        hashes = []
        for column, (frame, tip) in enumerate(zip(frames, anchors)):
            size = (max(1, round(frame.width * scale)), max(1, round(frame.height * scale)))
            scaled = frame.resize(size, Image.Resampling.NEAREST)
            position = (round(40 - tip[0] * scale), round(16 - tip[1] * scale))
            cell = Image.new('RGBA', (48, 32)); cell.alpha_composite(scaled, position)
            assert sum(cell.getchannel('A').histogram()[1:]) == sum(scaled.getchannel('A').histogram()[1:]), (name, column, 'clipped')
            assert cell.getbbox()
            hashes.append(hashlib.sha256(cell.tobytes()).hexdigest())
            atlas.alpha_composite(cell, (column * 48, row * 32))
            preview.alpha_composite(cell.resize((96, 64), Image.Resampling.NEAREST), (column * 96, row * 90 + 22))
            per_frame.append(dict(source_tip=tip, paste=position, bbox=cell.getbbox()))
        assert len(set(hashes)) == 4, (name, 'duplicate flight poses')
        draw.text((4, row * 90 + 3), name, fill='#eadbb4')
        records.append(dict(visual=name, fixed_row_scale=scale, frames=per_frame, unique_pose_hashes=hashes))
    alpha = atlas.getchannel('A')
    atlas = atlas.convert('RGB').quantize(colors=128, method=Image.Quantize.MEDIANCUT,
                                        dither=Image.Dither.NONE).convert('RGBA')
    atlas.putalpha(alpha)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    path = OUTPUT / 'projectiles.png'; atlas.save(path, optimize=True)
    meta = dict(version=1, file=path.name, sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                frame=[48, 32], pivot=[40, 16], columns=4, rows=NAMES, retained_rgba_bytes=122880,
                source=str(SOURCE.relative_to(ROOT)), source_dimensions=list(full.size),
                source_sha256=hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
                origin='Original hosted imagegen flight glyphs; normalized, not native48 claims',
                anchor_policy='One scale per row; physical opaque tip registered per source pose; no trajectory imported',
                simulation_changed=False, clips=records)
    (OUTPUT / 'manifest.json').write_text(json.dumps(meta, indent=2) + '\n')
    preview.save(SOURCE.parent / 'contact.png')
    print(json.dumps(dict(frames=20, bytes=122880, clipping='PASS')))


if __name__ == '__main__':
    build()
