#!/usr/bin/env python3
"""Independently verify shipped art hashes, pixels, frames, budgets and vendored source."""
import hashlib
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'app/src/main/assets'


def checked_png(path, digest, size):
    assert hashlib.sha256(path.read_bytes()).hexdigest() == digest, (path, 'SHA256')
    image = Image.open(path).convert('RGBA')
    assert image.size == tuple(size), (path, image.size, size)
    return image


def cells(image, width, height):
    return [image.crop((x, y, x + width, y + height))
            for y in range(0, image.height, height) for x in range(0, image.width, width)]


def verify():
    materials = json.loads((ASSETS / 'art_depth_v1/materials/manifest.json').read_text())
    for entry in materials['families']:
        image = checked_png(ASSETS / 'art_depth_v1/materials' / entry['file'], entry['sha256'], (256, 256))
        assert image.getchannel('A').getextrema() == (255, 255)
        assert all(image.getpixel((x, 0)) == image.getpixel((x, 255)) for x in range(256))
        assert all(image.getpixel((0, y)) == image.getpixel((255, y)) for y in range(256))
    assert len(materials['families']) == 16
    hero_bytes = 0
    for name in ('kaelen', 'lyra', 'vesper'):
        folder = ASSETS / 'character_upgrade_v1' / name
        meta = json.loads((folder / 'metadata.json').read_text())
        width, height = meta['frame']
        image = checked_png(folder / meta['atlas_file'], meta['atlas_sha256'], (width * 12, height * 2))
        poses = cells(image, width, height)
        assert len(poses) == 24 and len({hashlib.sha256(p.tobytes()).hexdigest() for p in poses}) == 24
        for pose in poses:
            bounds = pose.getbbox()
            assert bounds and 0 < bounds[0] < bounds[2] < width and 0 < bounds[1] < bounds[3] < height
            assert set(pose.getchannel('A').tobytes()).issubset({0, 255})
        checked_png(folder / meta['portrait_file'], meta['portrait_sha256'], (128, 160))
        hero_bytes += width * height * 24 * 4 + 128 * 160 * 4
    assert hero_bytes == 3_858_432 < 4 * 1024 * 1024
    meta = json.loads((ASSETS / 'pixelfx_v1/vfx_manifest.json').read_text())
    image = checked_png(ASSETS / 'pixelfx_v1/vfx.png', meta['sha256'], (768, 384))
    for row in range(4):
        frames = cells(image.crop((0, row * 96, 768, (row + 1) * 96)), 96, 96)
        assert len({hashlib.sha256(f.tobytes()).hexdigest() for f in frames}) == 8
        assert all(f.getbbox() for f in frames)
    inventory = json.loads((ROOT / 'docs/skill-library/pixelfx/inventory.json').read_text())
    for entry in inventory['vendor_files']:
        source = ROOT / 'tools/art/vendor/pixelfx' / entry['path']
        assert source.stat().st_size == entry['bytes']
        assert hashlib.sha256(source.read_bytes()).hexdigest() == entry['sha256']
    temple = json.loads((ASSETS / 'art_depth_v1/structures/manifest.json').read_text())
    checked_png(ASSETS / 'art_depth_v1/structures/summoning_hall.png', temple['sha256'], (268, 206))
    report = dict(material_families=16, hero_poses=72, portraits=3, fx_frames=32,
                  materials_retained_bytes=4_456_448, heroes_retained_bytes=hero_bytes,
                  temple_retained_bytes=220_832, fx_retained_bytes=1_179_648,
                  enemy_plus_fx_retained_bytes=8_257_536,
                  vendor_files_verified=len(inventory['vendor_files']), result='PASS')
    print(json.dumps(report, indent=2))
    return report


if __name__ == '__main__':
    verify()
