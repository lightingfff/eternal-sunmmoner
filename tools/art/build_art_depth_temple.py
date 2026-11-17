#!/usr/bin/env python3
"""Normalize the inspected original temple edit onto its unchanged runtime canvas."""
import hashlib
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'docs/art/art-depth-reforge/source/summoning-hall-imagegen.png'
DEST = ROOT / 'app/src/main/assets/art_depth_v1/structures'


def build():
    source = Image.open(SOURCE).convert('RGBA')
    source.putalpha(source.getchannel('A').point(lambda a: 255 if a >= 96 else 0))
    bounds = source.getbbox()
    if not bounds:
        raise ValueError('Temple source is empty')
    crop = source.crop(bounds)
    scale = min(252 / crop.width, 184 / crop.height)
    crop = crop.resize((round(crop.width * scale), round(crop.height * scale)), Image.Resampling.NEAREST)
    canvas = Image.new('RGBA', (268, 206))
    canvas.alpha_composite(crop, ((268 - crop.width) // 2, 206 - crop.height))
    alpha = canvas.getchannel('A')
    canvas = canvas.convert('RGB').quantize(colors=128, method=Image.Quantize.MEDIANCUT,
                                          dither=Image.Dither.NONE).convert('RGBA')
    canvas.putalpha(alpha)
    DEST.mkdir(parents=True, exist_ok=True)
    path = DEST / 'summoning_hall.png'
    canvas.save(path, optimize=True)
    manifest = dict(version=1, id='summoning_hall', file=path.name,
                    width=268, height=206, sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                    source=str(SOURCE.relative_to(ROOT)),
                    source_sha256=hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
                    source_dimensions=list(source.size), source_bbox=list(bounds), normalized_scale=scale,
                    origin='hosted imagegen edit of original temple; original armory used as material style reference',
                    world_geometry_changed=False, native_generated_dimensions_claimed=False)
    (DEST / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print(json.dumps(dict(size=canvas.size, bbox=canvas.getbbox(), sha256=manifest['sha256'])))


if __name__ == '__main__':
    build()
