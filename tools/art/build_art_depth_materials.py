#!/usr/bin/env python3
"""Normalize original imagegen material panels to coherent, periodic 4x4 tile sheets."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
NAMES = (
    'sanctuary_grass', 'meadow_grass', 'forest_moss', 'limestone',
    'cobble', 'soil', 'sand', 'oak',
    'marble', 'forge', 'carpet', 'leaf_litter',
    'ruin', 'volcanic', 'star_stone', 'rock',
)


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def periodic_panel(panel: Image.Image) -> Image.Image:
    # One common resample for the material, then a narrow symmetric wrap correction.
    # Internal 64px cuts retain the original continuous texture; never shuffle cells.
    pixels = np.array(panel.convert('RGB').resize((256, 256), Image.Resampling.NEAREST), dtype=np.float32)
    for axis in (1, 0):
        for inset in range(12):
            weight = (12 - inset) / 24
            a = [slice(None), slice(None), slice(None)]
            b = list(a)
            a[axis] = inset
            b[axis] = -1 - inset
            left, right = pixels[tuple(a)].copy(), pixels[tuple(b)].copy()
            pixels[tuple(a)] = left * (1 - weight) + right * weight
            pixels[tuple(b)] = right * (1 - weight) + left * weight
    image = Image.fromarray(np.rint(pixels).clip(0, 255).astype('uint8'))
    return image.quantize(colors=128, method=Image.Quantize.MEDIANCUT,
                          dither=Image.Dither.NONE).convert('RGBA')


def build(source: Path, destination: Path) -> dict:
    source = source.resolve()
    destination.mkdir(parents=True, exist_ok=True)
    with Image.open(source) as opened:
        original = opened.convert('RGB')
    families = []
    contact = Image.new('RGB', (4 * 256, 4 * 280), '#14212a')
    draw = ImageDraw.Draw(contact)
    for index, name in enumerate(NAMES):
        row, col = divmod(index, 4)
        panel = original.crop((round(col * original.width / 4), round(row * original.height / 4),
                               round((col + 1) * original.width / 4), round((row + 1) * original.height / 4)))
        output = periodic_panel(panel)
        data = np.array(output)
        assert np.array_equal(data[0], data[-1]) and np.array_equal(data[:, 0], data[:, -1])
        path = destination / f'{name}.png'
        output.save(path, optimize=True)
        families.append({'id': name, 'file': path.name, 'width': 256, 'height': 256,
                         'cell': [64, 64], 'grid': [4, 4], 'sha256': digest(path),
                         'rgba_bytes': 256 * 256 * 4, 'opaque': True,
                         'source_panel': [col, row], 'edge_pixels_equal': True})
        contact.paste(output.convert('RGB'), (col * 256, row * 280 + 24))
        draw.text((col * 256 + 5, row * 280 + 6), name, fill='#e3d3ac')
    manifest = {'version': 1, 'source': 'Original hosted imagegen material atlas; normalized derivatives, not native64 claims',
                'source_sha256': digest(source), 'source_dimensions': list(original.size),
                'world_tile_units': 64, 'camera_ground_y': 0.72, 'families': families,
                'sampling': '64px cells preserve 4x4 spatial order; periodic macro sheet, nearest-neighbor',
                'decoded_family_cells_bytes': len(families) * 256 * 256 * 4}
    (destination / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    evidence = ROOT / 'docs/art/art-depth-reforge'
    evidence.mkdir(parents=True, exist_ok=True)
    contact.save(evidence / 'material-contact.png')
    print(json.dumps({'families': len(families), 'source_dimensions': list(original.size),
                      'retained_cell_bytes': manifest['decoded_family_cells_bytes'],
                      'periodic_edges': 'PASS'}))
    return manifest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path,
                        default=ROOT / 'docs/art/art-depth-reforge/source/material-atlas-imagegen.png')
    parser.add_argument('--output', type=Path, default=ROOT / 'app/src/main/assets/art_depth_v1/materials')
    arguments = parser.parse_args()
    build(arguments.source, arguments.output)
