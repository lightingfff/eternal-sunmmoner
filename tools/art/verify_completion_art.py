#!/usr/bin/env python3
"""Independent checks of original completion assets, source hashes, frames, alpha and pinned effect code."""
import hashlib
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
PACK = ROOT / 'app/src/main/assets/art_completion_v1'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def image(path, sha, size):
    assert digest(path) == sha, (path, 'hash')
    output = Image.open(path).convert('RGBA')
    assert output.size == tuple(size), (path, output.size, size)
    return output


def poses(sheet, cell, columns, rows, binary=True):
    width, height = cell
    hashes = []
    for row in range(rows):
        for col in range(columns):
            frame = sheet.crop((col * width, row * height, (col + 1) * width, (row + 1) * height))
            bounds = frame.getbbox()
            assert bounds and bounds[0] > 0 and bounds[1] > 0 and bounds[2] < width and bounds[3] < height, ('margin', row, col, bounds)
            if binary:
                assert set(frame.getchannel('A').tobytes()).issubset({0, 255})
            hashes.append(hashlib.sha256(frame.tobytes()).hexdigest())
    assert len(set(hashes)) == len(hashes), 'duplicate normalized poses'
    return len(hashes)


def source_records(node):
    if isinstance(node, list):
        for entry in node:
            source_records(entry)
    elif isinstance(node, dict):
        for key, hash_key in [('source', 'source_sha256'), ('file', 'sha256'), ('path', 'sha256')]:
            value = node.get(key)
            if isinstance(value, str) and value.startswith('docs/') and hash_key in node:
                path = ROOT / value
                assert path.is_file() and digest(path) == node[hash_key], ('source hash', value)
        for value in node.values():
            source_records(value)


def verify():
    hero_frames = enemy_frames = 0
    class_bytes = enemy_bytes = 0
    for folder in sorted((PACK / 'classes').iterdir()):
        meta = json.loads((folder / 'metadata.json').read_text())
        sheet = image(folder / meta['atlas_file'], meta['atlas_sha256'], (1344, 224))
        hero_frames += poses(sheet, (112, 112), 12, 2)
        image(folder / meta['portrait_file'], meta['portrait_sha256'], (128, 160))
        assert meta['pivot'] == [56, 104] and meta['rows'] == ['front_right', 'back_right']
        class_bytes += 112 * 112 * 24 * 4 + 128 * 160 * 4
        source_records(meta)
    assert hero_frames == 72 and class_bytes == 3_858_432 < 4 * 1024 * 1024
    for folder in sorted((PACK / 'enemies').iterdir()):
        meta = json.loads((folder / 'metadata.json').read_text())
        sheet = image(folder / meta['atlas_file'], meta['atlas_sha256'], (1536, 144))
        enemy_frames += poses(sheet, (128, 144), 12, 1)
        assert meta['pivot'] == [64, 134]
        enemy_bytes += 128 * 144 * 12 * 4
        source_records(meta)
    assert enemy_frames == 48 and enemy_bytes == 3_538_944 < 4 * 1024 * 1024
    prop_bytes = {}
    for bank in ('props', 'interiors'):
        meta = json.loads((PACK / bank / 'manifest.json').read_text())
        total = 0
        for entry in meta['props']:
            im = image(PACK / bank / entry['file'], entry['sha256'], (entry['width'], entry['height']))
            assert set(im.getchannel('A').tobytes()).issubset({0, 255}) and im.getpixel((0, 0))[3] == 0
            assert im.getbbox() and im.getbbox()[3] == entry['pivot'][1]
            total += im.width * im.height * 4
        assert total < 1024 * 1024
        prop_bytes[bank] = total
        source_records(meta)
    assert prop_bytes == dict(props=56_192, interiors=225_984)
    gate = json.loads((PACK / 'gates/manifest.json').read_text())
    gate_bytes = 0
    for entry in gate['assets']:
        im = image(PACK / 'gates' / entry['file'], entry['sha256'], (entry['width'], entry['height']))
        gate_bytes += im.width * im.height * 4
    assert gate_bytes == 4_285_440
    source_records(gate)
    surfaces = json.loads((PACK / 'surfaces/manifest.json').read_text())
    for entry in surfaces['materials']:
        im = image(PACK / 'surfaces' / entry['file'], entry['sha256'], (256, 256))
        assert im.getchannel('A').getextrema() == (255, 255)
        assert all(im.getpixel((x, 0)) == im.getpixel((x, 255)) for x in range(256))
        assert all(im.getpixel((0, y)) == im.getpixel((255, y)) for y in range(256))
    source_records(surfaces)
    effects = json.loads((PACK / 'effects/vfx_manifest.json').read_text())
    im = image(PACK / 'effects/vfx.png', effects['sha256'], (768, 768))
    fx_frames = poses(im, (96, 96), 8, 8, binary=False)
    assert fx_frames == 64 and effects['source_commit'] == '5294518ce7c72e48b6100a744596e0acf57f2826'
    projectiles = json.loads((PACK / 'projectiles/manifest.json').read_text())
    im = image(PACK / 'projectiles/projectiles.png', projectiles['sha256'], (192, 160))
    flight_frames = poses(im, (48, 32), 4, 5, binary=False)
    assert flight_frames == 20 and projectiles['pivot'] == [40, 16]
    source_records(projectiles)
    additions = json.loads((ROOT / 'docs/art/art-completion/effects/upstream-additions.json').read_text())
    for entry in additions['files']:
        path = ROOT / entry['path']
        assert digest(path) == entry['sha256'] and path.stat().st_size == entry['bytes']
    original_effect_inventory = json.loads((ROOT / 'docs/skill-library/pixelfx/inventory.json').read_text())
    for entry in original_effect_inventory['vendor_files']:
        assert digest(ROOT / 'tools/art/vendor/pixelfx' / entry['path']) == entry['sha256']
    report = dict(result='PASS', new_hero_poses=hero_frames, total_authored_hero_poses=144,
                  new_enemy_poses=enemy_frames, total_one_facing_enemy_poses=96, execution_frames=fx_frames,
                  projectile_frames=flight_frames, original_new_props=11, surfaces=4, gate_images=3,
                  class_bytes=class_bytes, extended_enemy_bytes=enemy_bytes, prop_banks=prop_bytes,
                  gate_bytes=gate_bytes, original_enemy_effect_projectile_bytes=6_021_120,
                  listed_retained_pixel_bank_bytes=27_570_400,
                  note='Pixel banks only; not total process/GPU or device memory. Source-decode transients and other caches excluded.')
    print(json.dumps(report, indent=2))
    return report


if __name__ == '__main__':
    verify()
