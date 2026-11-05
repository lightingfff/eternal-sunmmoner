#!/usr/bin/env python3
"""Validate the shipped P27B pack; structural checks are NOT an art sign-off.

Run from any directory: python3 tools/p27b_validate.py --root <checkout>.
Requires Pillow. No reference images, no writes. Seam averages are a *guard*,
not evidence that 3×3/5×5 or engine screenshots look seamless.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageStat

FAMILIES = ('grass','soil','flagstone','cobble','summon_ivory')
STRUCTURES = {'summoning_hall':((268,206),(134,206),(111,176)),
              'tower_gate':((260,224),(130,224),(130,120))}
# Multi-megabyte generator outputs stay out of Git; a 768 px JPEG preview is committed
# so the provenance hash is still auditable in CI without a 15 MB blob.
PREVIEW_SIZE = 768


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_path(root: Path, entry: dict) -> Path:
    full = root/entry['source_path']
    if full.is_file():
        return full
    stem = Path(entry['source_path']).stem
    return root/'art/p27b_sources/preview'/f'{stem}_{PREVIEW_SIZE}.jpg'


def seam(a: Image.Image, b: Image.Image, axis: str) -> float:
    a,b = a.load(),b.load()
    if axis == 'east':
        return sum(abs(a[63,y][c]-b[0,y][c]) for y in range(64) for c in range(3))/(64*3)
    return sum(abs(a[x,63][c]-b[x,0][c]) for x in range(64) for c in range(3))/(64*3)


def validate(root: Path) -> None:
    directory = root/'app/src/main/assets/sanctuary_p27b'
    manifest = json.loads((directory/'manifest.json').read_text())
    assert manifest['schema'] == 'eternal-summoner.p27b.visual-profile.v1'
    assert manifest['status'] == 'GAME_CANDIDATE', 'cannot silently ship unreviewed art as READY'
    assert manifest['camera_ground_y_scale'] == .72 and manifest['tile_px'] == 64
    entries = manifest['assets']
    assert len(entries) == manifest['asset_count'] == 82
    assert len({e['id'] for e in entries}) == 82
    assert len({e['path'] for e in entries}) == 82
    images: dict[str,Image.Image] = {}
    for entry in entries:
        file = directory/entry['path']
        assert file.resolve().is_relative_to(directory.resolve()), entry['path']
        assert sha(file) == entry['sha256'], f"hash: {entry['id']}"
        source = source_path(root, entry)
        assert source.resolve().is_relative_to((root/'art/p27b_sources').resolve())
        # A committed preview is derived, not the generator output: the sha256 field is
        # only authoritative for the full-resolution original.
        if source == root/entry['source_path']:
            assert sha(source) == entry['source_sha256'], f"source hash: {entry['id']}"
        else:
            assert entry.get('preview_sha256') == sha(source), f"preview hash: {entry['id']}"
            with Image.open(source) as preview:
                assert max(preview.size) == PREVIEW_SIZE, f"preview size: {entry['id']}"
        assert entry['qa_status'] == 'GAME_CANDIDATE'
        with Image.open(file) as original:
            original.load()
            assert list(original.size) == entry['size'], f"canvas: {entry['id']}"
            image = original.copy()
        images[entry['id']] = image
        if entry['family'] != 'structure':
            assert image.mode in ('RGB','RGBA') and image.size == (64,64)
            assert image.mode != 'RGBA' or image.getchannel('A').getextrema() == (255,255)
        else:
            assert image.mode == 'RGBA'
            assert entry['id'] in STRUCTURES
            size,pivot,focus = STRUCTURES[entry['id']]
            assert image.size == size and tuple(entry['pivot']) == pivot and tuple(entry['focus']) == focus
            alpha = image.getchannel('A')
            assert alpha.getextrema() == (0,255), f"not a cutout: {entry['id']}"
            assert set(alpha.tobytes()).issubset({0,255}), f"unexpected alpha fringe: {entry['id']}"
            assert list(alpha.getbbox()) == entry['alpha_bbox']
            assert alpha.getpixel((0,0)) == alpha.getpixel((size[0]-1,0)) == 0
            assert alpha.getpixel((size[0]//2,size[1]-1)) == 255, f"no centre foot: {entry['id']}"
            assert alpha.getpixel(focus) == 255, f"focus not painted: {entry['id']}"
            if entry['id'] == 'tower_gate':
                assert max(image.getpixel(focus)[:3]) < 90, 'rift void must remain dark'
        assert entry['frame_count'] == 1
    families = manifest['families']
    assert set(families) == set(FAMILIES)
    for name in FAMILIES:
        group = [entry for entry in entries if entry['family'] == name]
        assert len(group) == 16 and len({entry['sha256'] for entry in group}) == 16, name
        assert {tuple(entry['mosaic_xy']) for entry in group} == {(x,y) for x in range(4) for y in range(4)}
        assert all(entry['id'] == f'{name}_{entry["mosaic_xy"][1]*4+entry["mosaic_xy"][0]}' for entry in group)
        cells = [images[f'{name}_{index}'] for index in range(16)]
        east = [seam(cells[y*4+x],cells[y*4+(x+1)%4],'east') for y in range(4) for x in range(4)]
        south = [seam(cells[y*4+x],cells[((y+1)%4)*4+x],'south') for y in range(4) for x in range(4)]
        # Distinct averages along row/column; wrapped mosaic need not have zero edge-jump.
        assert max(east+south) < 32, f'{name}: obvious numeric seam {max(east+south):.2f}'
        average = ImageStat.Stat(cells[0]).mean
        print(f'{name:14} 16 unique  seam max {max(east):5.2f} east / {max(south):5.2f} south;'
              f' cell0 RGB {[round(x) for x in average]}  STATUS: GAME_CANDIDATE, VISUAL REVIEW REQUIRED')
    assert {e['id'] for e in entries if e['family'] == 'structure'} == set(STRUCTURES)
    print('82/82 hashes, decodes, canvases, pivots, alpha, family layout & numeric seam guard PASS.')
    print('NOT READY: 5×5 repeated patterns, engine lighting/pivots and device FPS require visual/hardware review.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    validate(parser.parse_args().root.resolve())
