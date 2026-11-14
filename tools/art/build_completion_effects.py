#!/usr/bin/env python3
"""Normalize eight genuine PixelFX execution clips, preserving simulation and original fallback assets."""
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
SPECS = [
    ('guard', 'shield-block', [1, 3, 6, 9, 13, 17, 22, 29], 0),
    ('arcane', 'mana-burst', [7, 8, 10, 14, 20, 27, 36, 48], 0),
    ('cleave', 'spin-slash', [2, 4, 6, 8, 10, 13, 17, 21], 0),
    ('holy', 'sunburst', [0, 3, 7, 11, 17, 23, 31, 41], 32),
    ('shadow', 'dagger-flurry', [3, 5, 7, 9, 11, 14, 18, 23], 0),
    ('impact', 'hit-spark', [0, 1, 2, 3, 5, 7, 10, 14], 0),
    ('shield_impact', 'shield-block', [2, 4, 7, 10, 14, 19, 24, 30], 0),
    ('slam', 'ground-slam', [0, 2, 4, 7, 12, 18, 27, 40], 0),
]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build():
    output = ROOT / 'app/src/main/assets/art_completion_v1/effects'
    output.mkdir(parents=True, exist_ok=True)
    atlas = Image.new('RGBA', (768, 768))
    contact = Image.new('RGBA', (768, 8 * 116), '#172632')
    draw = ImageDraw.Draw(contact)
    records = []
    for row, (style, name, requested, foot_offset) in enumerate(SPECS):
        folder = ROOT / 'docs/art/art-completion/effects/source' / name
        if not folder.exists():
            folder = ROOT / 'docs/art/art-depth-reforge/pixelfx-source' / name
        meta = json.loads((folder / f'{name}.json').read_text())
        source = Image.open(folder / f'{name}.png').convert('RGBA')
        info = meta['meta']['pixelfx']
        ox, oy = info['pivot']
        # The source dagger actor stands60px behind its target. Runtime SHADOW is
        # already emitted at the victim, so register the execution focus, not the stock actor.
        target_offset_x = 60 if style == 'shadow' else 0
        ox += target_offset_x
        oy += foot_offset
        frames = []
        ids = []
        seen = set()
        # Keep requested execution windows; substitute only a genuine nearby nonempty frame if needed.
        for wanted in requested:
            candidates = sorted(range(len(meta['frames'])), key=lambda i: (abs(i - wanted), i))
            for index in candidates:
                rect = meta['frames'][index]['frame']
                image = source.crop((rect['x'], rect['y'], rect['x'] + rect['w'], rect['y'] + rect['h']))
                digest = hashlib.sha256(image.tobytes()).hexdigest()
                if image.getbbox() and digest not in seen:
                    seen.add(digest); ids.append(index); frames.append(image); break
            else:
                raise ValueError(f'{name} lacks eight unique execution frames')
        boxes = [f.getbbox() for f in frames]
        left, top = min(b[0] for b in boxes), min(b[1] for b in boxes)
        right, bottom = max(b[2] for b in boxes), max(b[3] for b in boxes)
        scale = min(46 / max(1, ox - left), 46 / max(1, right - ox),
                    82 / max(1, oy - top), 10 / max(1, bottom - oy), 1)
        size = (max(1, round(frames[0].width * scale)), max(1, round(frames[0].height * scale)))
        position = (round(48 - ox * scale), round(84 - oy * scale))
        hashes = []
        for col, frame in enumerate(frames):
            scaled = frame.resize(size, Image.Resampling.NEAREST)
            cell = Image.new('RGBA', (96, 96)); cell.alpha_composite(scaled, position)
            assert cell.getbbox()
            assert sum(cell.getchannel('A').histogram()[1:]) == sum(scaled.getchannel('A').histogram()[1:]), (name, col, 'clipped')
            hashes.append(hashlib.sha256(cell.tobytes()).hexdigest())
            atlas.alpha_composite(cell, (col * 96, row * 96))
            contact.alpha_composite(cell, (col * 96, row * 116 + 20))
        assert len(set(hashes)) == 8, (name, 'normalization duplicates')
        draw.text((4, row * 116 + 4), style + ' / ' + name, fill='#eadbb4')
        records.append(dict(style=style, effect=info['id'], source_frames=ids, source_pivot=info['pivot'],
                            execution_anchor_x_offset=target_offset_x,
                            execution_anchor_y_offset=foot_offset, fixed_row_scale=scale,
                            source_sheet_sha256=sha(folder / f'{name}.png'), sampled_pose_hashes=hashes,
                            simulation_timing_changed=False))
    atlas.save(output / 'vfx.png', optimize=True)
    metadata = dict(version=1, file='vfx.png', frame=[96, 96], pivot=[48, 84], columns=8,
                    rows=[s[0] for s in SPECS], sha256=sha(output / 'vfx.png'), rgba_bytes=768 * 768 * 4,
                    source_commit='5294518ce7c72e48b6100a744596e0acf57f2826', license='MIT', selections=records)
    (output / 'vfx_manifest.json').write_text(json.dumps(metadata, indent=2) + '\n')
    for name in ('LICENSE', 'NOTICE.md'):
        (output / name).write_bytes((ROOT / 'tools/art/vendor/pixelfx' / name).read_bytes())
    contact.save(ROOT / 'docs/art/art-completion/effects/contact.png')
    print(json.dumps(dict(frames=64, retained_bytes=metadata['rgba_bytes'], clipping='PASS')))


if __name__ == '__main__':
    build()
