#!/usr/bin/env python3
"""Key AI chroma-green Sanctuary structure renders onto inherited runtime canvases.

The source images are art only: this tool does not change world geometry, collision,
footprints, hit areas, camera, projection, or sprite pivots. The output canvas sizes
come from the shipped sprite contracts. Keying is a border-connected green flood fill
so green foliage enclosed by the building silhouette is retained.
"""
from __future__ import annotations

import hashlib
import json
from collections import deque
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SOURCE_DIR = ROOT / 'art/p27b_sources_next'
ASSETS = ROOT / 'app/src/main/assets'

# source name -> asset output(s), inherited canvas size, asset ID in its manifest.
STRUCTURES = {
    'ai_tower_gate.png': [
        ('sanctuary_p27b/structures/tower_gate.png', (260, 224), 'tower_gate'),
    ],
    'ai_summoning_hall.png': [
        ('sanctuary_p27b/structures/summoning_hall.png', (268, 206), 'summoning_hall'),
        ('art_depth_v1/structures/summoning_hall.png', (268, 206), 'summoning_hall'),
    ],
    'ai_hero_registry.png': [
        ('sanctuary_p27b_2/structures/hero_registry.png', (188, 166), 'facility_hero_registry'),
    ],
    'ai_synthesis_lab.png': [
        ('sanctuary_p27b_2/structures/synthesis_lab.png', (188, 168), 'facility_synthesis_lab'),
    ],
    'ai_training_ground.png': [
        ('sanctuary_p27b_2/structures/training_ground.png', (192, 160), 'facility_training_ground'),
    ],
    'ai_floor_vista_arch.png': [
        ('sanctuary_p27b_2/structures/floor_vista_arch.png', (156, 168), 'floor_vista_arch'),
    ],
    'ai_townhouse_a.png': [
        ('sanctuary_p27b_2/structures/townhouse_a.png', (152, 156), 'house_timber_crest'),
    ],
    'ai_townhouse_b.png': [
        ('sanctuary_p27b_2/structures/townhouse_b.png', (144, 160), 'house_stone_guild'),
    ],
    'ai_cottage_brown.png': [
        ('sanctuary_p27b_2/structures/cottage_brown.png', (132, 144), 'house_thatched'),
    ],
    'ai_cottage_slate.png': [
        ('sanctuary_p27b_2/structures/cottage_slate.png', (132, 144), 'house_slate_wing'),
    ],
}


def key_green(image: Image.Image) -> Image.Image:
    rgb = image.convert('RGB')
    width, height = rgb.size
    pixels = rgb.load()
    candidate = bytearray(width * height)
    # Include antialiased green fringe, but don't classify merely green vegetation
    # unless it is connected to the outside plate.
    for y in range(height):
        for x in range(width):
            r, g, b = pixels[x, y]
            if g > 55 and g - r >= 20 and g - b >= 17 and g >= r * 1.10:
                candidate[y * width + x] = 1
    exterior = bytearray(width * height)
    queue: deque[int] = deque()
    for x in range(width):
        for i in (x, (height - 1) * width + x):
            if candidate[i] and not exterior[i]:
                exterior[i] = 1
                queue.append(i)
    for y in range(1, height - 1):
        for i in (y * width, y * width + width - 1):
            if candidate[i] and not exterior[i]:
                exterior[i] = 1
                queue.append(i)
    while queue:
        i = queue.popleft()
        x, y = i % width, i // width
        for n in (i - 1 if x else -1, i + 1 if x + 1 < width else -1,
                  i - width if y else -1, i + width if y + 1 < height else -1):
            if n >= 0 and candidate[n] and not exterior[n]:
                exterior[n] = 1
                queue.append(n)
    alpha = Image.frombytes('L', (width, height), bytes(0 if v else 255 for v in exterior))
    cut = rgb.convert('RGBA')
    cut.putalpha(alpha)
    # Clean key-color fringe on edge pixels before the final binary downsample.
    # Restrict to green-dominant pixels so foliage and turquoise glass stay intact.
    px = cut.load()
    for y in range(height):
        for x in range(width):
            if alpha.getpixel((x, y)):
                r, g, b, a = px[x, y]
                if g - max(r, b) > 12 and g > r * 1.12:
                    px[x, y] = (r, min(g, max(r, b) + 8), b, a)
    return cut


def fitted(source: Image.Image, size: tuple[int, int]) -> tuple[Image.Image, list[int], float]:
    cut = key_green(source)
    bbox = cut.getchannel('A').getbbox()
    if bbox is None:
        raise ValueError('keying removed all pixels')
    content = cut.crop(bbox)
    scale = min((size[0] - 4) / content.width, (size[1] - 4) / content.height)
    target = (max(1, round(content.width * scale * 0.98)),
              max(1, round(content.height * scale * 0.98)))
    resized = content.resize(target, Image.Resampling.LANCZOS)
    # Match the asset contract: crisp binary alpha, no glow or semi-transparent shadow.
    resized.putalpha(resized.getchannel('A').point(lambda v: 255 if v >= 128 else 0))
    canvas = Image.new('RGBA', size, (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((size[0] - target[0]) // 2, size[1] - target[1]))
    return canvas, list(bbox), scale


def darken_dynamic_gate_recess(image: Image.Image) -> Image.Image:
    """Seat the generated portal's opaque cavity under the existing live rift effect.

    The existing renderer is authored around the canvas centre at y=120. The imagegen
    portal's dark core lands a few pixels lower after fitting, so gently deepen only the
    small cavity centre; its cyan rim and the architecture remain untouched.
    """
    image = image.copy().convert('RGBA')
    px = image.load()
    cx, cy = image.width // 2, 120
    dark = (5, 12, 30)
    rx, ry = 7.0, 9.0
    for y in range(max(0, cy - 10), min(image.height, cy + 11)):
        for x in range(max(0, cx - 8), min(image.width, cx + 9)):
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if d > 1:
                continue
            strength = max(0.0, 1.0 - d) ** 2 * 0.98
            r, g, b, a = px[x, y]
            px[x, y] = (round(r * (1 - strength) + dark[0] * strength),
                        round(g * (1 - strength) + dark[1] * strength),
                        round(b * (1 - strength) + dark[2] * strength), a)
    return image


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()



def main() -> None:
    for source_name, outputs in STRUCTURES.items():
        source_path = SOURCE_DIR / source_name
        if not source_path.is_file():
            raise FileNotFoundError(source_path)
        source_hash = digest(source_path)
        with Image.open(source_path) as im:
            source_size = im.size
            source = im.copy()
        generated: dict[str, tuple[list[int], float, str, tuple[int, int]]] = {}
        for relpath, size, asset_id in outputs:
            image, bbox, scale = fitted(source, size)
            if asset_id == 'tower_gate':
                image = darken_dynamic_gate_recess(image)
            destination = ASSETS / relpath
            destination.parent.mkdir(parents=True, exist_ok=True)
            image.save(destination, optimize=True)
            generated[relpath] = (bbox, scale, digest(destination), size)
            print(f'{relpath}: {size}, source bbox={bbox}, scale={scale:.5f}, sha256={digest(destination)}')
        if source_name in ('ai_tower_gate.png', 'ai_summoning_hall.png'):
            path = ASSETS / 'sanctuary_p27b/manifest.json'
            data = json.loads(path.read_text())
            ids = {spec[2] for spec in outputs}
            for entry in data['assets']:
                if entry.get('id') in ids:
                    relpath = f"sanctuary_p27b/{entry['path']}"
                    bbox, scale, sha, size = generated[relpath]
                    alpha_bbox = list(Image.open(ASSETS / relpath).getchannel('A').getbbox())
                    entry.update({
                        'sha256': sha, 'source_path': f'art/p27b_sources_next/{source_name}',
                        'source_sha256': source_hash, 'source_type': 'original AI-generated green-screen Sanctuary environment art',
                        'alpha_bbox': alpha_bbox, 'source_dimensions': list(source_size),
                        'source_bbox': bbox, 'normalized_scale': scale,
                        'qa_status': 'GAME_CANDIDATE',
                        'visual_qa': 'keyed and fitted to inherited canvas; engine-side lighting/pivot render review pending',
                    })
            path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')
            if source_name == 'ai_summoning_hall.png':
                folder = ASSETS / 'art_depth_v1/structures'
                meta_path = folder / 'manifest.json'
                meta = json.loads(meta_path.read_text())
                relpath = 'art_depth_v1/structures/summoning_hall.png'
                bbox, scale, sha, size = generated[relpath]
                meta.update({'sha256': sha, 'source': f'art/p27b_sources_next/{source_name}',
                             'source_sha256': source_hash, 'source_dimensions': list(source_size),
                             'source_bbox': bbox, 'normalized_scale': scale,
                             'origin': 'original AI-generated green-screen fantasy structure',
                             'world_geometry_changed': False, 'native_generated_dimensions_claimed': False})
                meta_path.write_text(json.dumps(meta, indent=2) + '\n')
        elif source_name in ('ai_hero_registry.png', 'ai_synthesis_lab.png', 'ai_training_ground.png',
                             'ai_floor_vista_arch.png', 'ai_townhouse_a.png', 'ai_townhouse_b.png',
                             'ai_cottage_brown.png', 'ai_cottage_slate.png'):
            path = ASSETS / 'sanctuary_p27b_2/manifest_structures.json'
            data = json.loads(path.read_text())
            ids = {spec[2] for spec in outputs}
            for entry in data['assets']:
                if entry.get('id') in ids:
                    relpath = 'sanctuary_p27b_2/' + entry['file']
                    bbox, scale, sha, size = generated[relpath]
                    entry.update({
                        'sha256': sha, 'source_path': f'art/p27b_sources_next/{source_name}',
                        'source_sha256': source_hash, 'source_dimensions': list(source_size),
                        'source_bbox': bbox, 'normalized_scale': scale,
                        'size': list(size), 'pivot': [size[0] // 2, size[1]],
                        'alpha': 'binary RGBA', 'mean_luma': 0.0,
                        'qa_status': 'GAME_CANDIDATE',
                        'visual_qa': 'keyed and fitted to inherited canvas; engine-side lighting/pivot render review pending',
                    })
                    img = Image.open(ASSETS / relpath).convert('RGBA')
                    pix = img.load()
                    total = count = 0
                    for y in range(img.height):
                        for x in range(img.width):
                            r, g, b, a = pix[x, y]
                            if a:
                                total += 0.2126 * r + 0.7152 * g + 0.0722 * b
                                count += 1
                    entry['mean_luma'] = round(total / max(count, 1), 1)
            path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')
    # The hall manifest at both destinations and all P27B structure records carry hashes.
    print('DONE')


if __name__ == '__main__':
    main()
