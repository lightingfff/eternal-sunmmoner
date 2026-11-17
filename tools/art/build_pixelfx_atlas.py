#!/usr/bin/env python3
"""Normalize selected PixelFX execution windows to the existing four-by-eight96px runtime contract."""
from __future__ import annotations
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[2]
SPECS=[('guard','shield-block',[1,3,6,9,13,17,22,29]),
       ('arcane','mana-burst',[7,8,10,14,20,27,36,48]),
       ('cleave','sword-slash',[2,3,4,6,9,12,14,16]),
       ('holy','heal-aoe',[10,13,17,23,31,40,54,72])]

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def build():
    output=ROOT/'app/src/main/assets/pixelfx_v1';output.mkdir(parents=True,exist_ok=True)
    atlas=Image.new('RGBA',(768,384));records=[]
    contact=Image.new('RGBA',(768,4*116),'#172632');draw=ImageDraw.Draw(contact)
    for row,(style,name,indices) in enumerate(SPECS):
        path=ROOT/'docs/art/art-depth-reforge/pixelfx-source'/name
        metadata=json.loads((path/f'{name}.json').read_text());sheet=Image.open(path/f'{name}.png').convert('RGBA')
        meta=metadata['meta']['pixelfx'];ox,oy=meta['pivot'];frames=[]
        for index in indices:
            box=metadata['frames'][index]['frame'];frames.append(sheet.crop((box['x'],box['y'],box['x']+box['w'],box['y']+box['h'])))
        boxes=[f.getbbox() for f in frames];assert all(boxes),(name,boxes)
        left=min(b[0] for b in boxes);top=min(b[1] for b in boxes);right=max(b[2] for b in boxes);bottom=max(b[3] for b in boxes)
        scale=min(46/max(1,ox-left),46/max(1,right-ox),82/max(1,oy-top),10/max(1,bottom-oy),1)
        source_w,source_h=frames[0].size;w=max(1,round(source_w*scale));h=max(1,round(source_h*scale))
        x=round(48-ox*scale);y=round(84-oy*scale)
        hashes=[]
        for col,f in enumerate(frames):
            f=f.resize((w,h),Image.Resampling.NEAREST)
            normalized=Image.new('RGBA',(96,96));normalized.alpha_composite(f,(x,y))
            assert normalized.getbbox() is not None
            # Only transparent source margins may lie outside the fixed runtime cell.
            assert sum(normalized.getchannel('A').histogram()[1:])==sum(f.getchannel('A').histogram()[1:]),(name,col,'clipping')
            hashes.append(hashlib.sha256(normalized.tobytes()).hexdigest())
            atlas.alpha_composite(normalized,(col*96,row*96));contact.alpha_composite(normalized,(col*96,row*116+20))
        assert len(set(hashes))==8,(name,'duplicate sampled frames')
        draw.text((5,row*116+4),style+' / '+name,fill='#eadbb4')
        records.append({'style':style,'effect':meta['id'],'source_frames':indices,'source_fps':meta['fps'],
                        'source_pivot':[ox,oy],'fixed_row_scale':scale,'sampled_pose_hashes':hashes,
                        'source_sheet_sha256':sha(path/f'{name}.png'),
                        'timing':'execution-only window; no source wind-up/hit-stop imported; simulation unchanged'})
    atlas.save(output/'vfx.png',optimize=True)
    meta={'version':1,'file':'vfx.png','frame':[96,96],'pivot':[48,84],'columns':8,
          'rows':['guard','arcane','cleave','holy'],'sha256':sha(output/'vfx.png'),
          'rgba_bytes':96*96*32*4,'source_repository':'https://github.com/patch-gg/pixelfx',
          'source_commit':'5294518ce7c72e48b6100a744596e0acf57f2826','license':'MIT; see LICENSE and NOTICE.md',
          'selections':records,'left_direction':'existing execution-origin policy; source art only, no stock actor',
          'simulation_damage_timing_changed':False}
    (output/'vfx_manifest.json').write_text(json.dumps(meta,indent=2)+'\n')
    contact.resize((1536,928),Image.Resampling.NEAREST).save(ROOT/'docs/art/art-depth-reforge/pixelfx-contact.png')
    for file in ['LICENSE','NOTICE.md']:
        (output/file).write_bytes((ROOT/'tools/art/vendor/pixelfx'/file).read_bytes())
    print(json.dumps({'rows':4,'frames':32,'retained_bytes':meta['rgba_bytes'],'clipping':'PASS'}))

if __name__=='__main__':build()
