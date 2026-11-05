#!/usr/bin/env python3
"""Package only reviewed P27B candidates, never overwrite user V2/V3 or P03.

Sources: art/p27b_sources/ (Arena original generation, 4 material images +
Hall + Gate rework); transforms: p27b_terrain.py / p27b_structures.py.
Output: Android assets, JSON manifest, explicitly labelled *ART CONTACT*
previews outside the APK. Status remains GAME_CANDIDATE until engine review.
Requires Pillow>=11. Run validators after packaging.
"""
from __future__ import annotations
import hashlib
import json
from pathlib import Path
import shutil
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT/'art/p27b_sources'
WORK = ROOT/'art/p27b_workbench_v2'
STRUCT = ROOT/'art/p27b_workbench_4x4/structures'
ASSET = ROOT/'app/src/main/assets/sanctuary_p27b'
PREVIEW = ROOT/'docs/p27b/previews/terrain'
ORIGIN_PREVIEW = ROOT/'art/p27b_sources/preview'
PREVIEW_SIZE = 768


def digest(p: Path) -> str:
    return hashlib.sha256(p.read_bytes()).hexdigest()


def write_origin_preview(origin: Path) -> str:
    """The generator originals are multi-MB and not tracked; commit a small JPEG so CI
    can still verify which image each shipped tile was derived from."""
    ORIGIN_PREVIEW.mkdir(parents=True, exist_ok=True)
    target = ORIGIN_PREVIEW/f'{origin.stem}_{PREVIEW_SIZE}.jpg'
    with Image.open(origin) as image:
        preview = image.convert('RGB')
        preview.thumbnail((PREVIEW_SIZE, PREVIEW_SIZE), Image.LANCZOS)
        preview.save(target, quality=86, optimize=True)
    return digest(target)


def main() -> None:
    families = json.loads((WORK/'manifest_candidates.json').read_text())['families']
    buildings = json.loads((STRUCT/'manifest_candidates.json').read_text())
    entries = []
    origin_previews: dict[str, str] = {}
    for family, info in families.items():
        cols, rows = info['mosaic_grid']
        assert (cols,rows) == (4,4) and len(info['tiles']) == 16
        origin = ROOT/info['source_file']
        assert digest(origin) == info['source_sha256']
        origin_previews[info['source_file']] = write_origin_preview(origin)
        for item in info['tiles']:
            f = WORK/item['file']
            with Image.open(f) as im:
                assert im.size==(64,64) and im.mode=='RGB'
            dest = ASSET/'terrain'/f.name
            dest.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(f,dest)
            assert digest(dest) == item['sha256']
            entries.append({
                'id':item['id'],'family':family,'path':f'terrain/{f.name}',
                'size':[64,64],'channels':'RGB opaque','frame_count':1,
                'mosaic_xy':item['grid_pos'],'world_tile_px':[64,64],
                'source_type':'original image-tool material sample; BOX mosaic normalization',
                'source_path':info['source_file'],'source_sha256':info['source_sha256'],
                'preview_sha256':origin_previews[info['source_file']],
                'sha256':digest(dest), 'qa_status':'GAME_CANDIDATE',
                'visual_qa':'5x5 checked offline; engine still required; visible 4x4 repeat',
            })
        PREVIEW.mkdir(parents=True, exist_ok=True)
        for count in (3,5):
            shutil.copyfile(WORK/f'{family}_{count}x{count}.png',
                            PREVIEW/f'ART_CONTACT_{family}_{count}x{count}.png')
    for name, info in buildings.items():
        source = ROOT/info['source']
        assert digest(source) == info['source_sha256']
        origin_previews[info['source']] = write_origin_preview(source)
        f = STRUCT/info['file']
        with Image.open(f) as im:
            assert im.size==tuple(info['size']) and im.mode=='RGBA'
        dest = ASSET/'structures'/f.name
        dest.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(f,dest)
        assert digest(dest)==info['sha256']
        entries.append({
            'id':name,'family':'structure','path':f'structures/{f.name}',
            'size':info['size'],'channels':'RGBA binary alpha','frame_count':1,
            'pivot':info['pivot'],'focus':info['focus'],
            'world_scale_multiplier':'2 * worldObstacle.scale * zoom (unchanged)',
            'source_type':info['source_type'],'source_path':info['source'],
            'source_sha256':info['source_sha256'],
            'preview_sha256':origin_previews[info['source']],'sha256':digest(dest),
            'alpha_bbox':info['bbox_alpha'],'qa_status':'GAME_CANDIDATE',
            'visual_qa':'contact cutout checked; pending renderer pivot/light review',
        })
    manifest = {
        'schema':'eternal-summoner.p27b.visual-profile.v1', 'status':'GAME_CANDIDATE',
        'asset_count':len(entries), 'number_of_image_tool_sources':6,
        'note':'5 material families from four original sources (ivory recolors limestone); two edited original structure sources; 4x4 mosaic variants from each material. Distinguish original images from derived atlas cells.',
        'visual_profile':'P27B PNG-first, P03/premium_slice fallback when absent',
        'camera_ground_y_scale':0.72, 'tile_px':64,
        'terrain_mosaic_grid':[4,4], 'terrain_count_per_family':16,
        'families':{family:{k:v for k,v in info.items() if k not in ('tiles',)}
                    for family,info in families.items()},
        'assets':entries,
    }
    (ASSET/'manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n')
    shutil.copyfile(STRUCT/'CONTACT_STRUCTURES_CANDIDATE.png',
                    ROOT/'docs/p27b/previews/CONTACT_STRUCTURES_CANDIDATE_NOT_ENGINE.png')
    print('Packaged',len(entries),'PNGs (material-source originals=4, structure edits=2)')

if __name__ == '__main__': main()
