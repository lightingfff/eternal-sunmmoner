#!/usr/bin/env python3
"""Normalize inspected original Kaelen/Vesper sources with fixed clip-space roots."""
from __future__ import annotations
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
DOCS = ROOT / 'docs/art/art-depth-reforge/hero-production'
ORDER = ['idle0', 'idle1', 'walk0', 'walk1', 'walk2', 'walk3',
         'anticipation', 'contact', 'recovery', 'hurt', 'collapse', 'defeat']


def sha(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()


def opaque(im):
    out = im.convert('RGBA')
    out.putalpha(out.getchannel('A').point(lambda a: 255 if a >= 96 else 0))
    return out


def spans(im):
    a = im.getchannel('A'); runs = []; begin = None
    for x in range(im.width):
        on = a.crop((x, 0, x + 1, im.height)).getbbox() is not None
        if on and begin is None: begin = x
        if not on and begin is not None:
            runs.append([begin, x]); begin = None
    if begin is not None: runs.append([begin, im.width])
    merged = []
    for run in runs:
        if merged and run[0] - merged[-1][1] < 25: merged[-1][1] = run[1]
        else: merged.append(run)
    return merged


def strip(hero, file, count, root_x, baseline, body_top, target_height, cell, pivot,
          row=0, source_rows=1):
    path = DOCS / hero / ('sources' if hero == 'kaelen' else 'source') / file
    full = opaque(Image.open(path))
    y0 = round(full.height * row / source_rows); y1 = round(full.height * (row + 1) / source_rows)
    source = full.crop((0, y0, full.width, y1))
    occupied = spans(source)
    assert len(occupied) == count, (file, occupied)
    scale = target_height / (baseline - body_top)
    frames = []; records = []
    for i, (left, right) in enumerate(occupied):
        image = source.crop((left, 0, right, source.height))
        image = image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.NEAREST)
        x = round(pivot[0] + (left - i * source.width / count - root_x) * scale)
        y = round(pivot[1] - baseline * scale)
        target = Image.new('RGBA', cell); target.alpha_composite(image, (x, y))
        before = sum(image.getchannel('A').histogram()[1:]); after = sum(target.getchannel('A').histogram()[1:])
        assert before == after, (hero, file, i, 'clipped', before - after, x, y)
        bounds = target.getbbox()
        assert bounds and bounds[0] > 0 and bounds[1] > 0 and bounds[2] < cell[0] and bounds[3] < cell[1], (hero,file,i,bounds)
        frames.append(target)
        records.append({'index':i,'source_span':[left,right],'paste':[x,y],'bbox':bounds,'opaque_pixels':after})
    return frames, {'file':str(path.relative_to(ROOT)), 'sha256':sha(path), 'source_row':row,
                    'source_rows':source_rows,'count':count,'root_x':root_x,'baseline':baseline,
                    'anatomical_top':body_top,'target_anatomical_height':target_height,
                    'fixed_clip_scale':scale,'per_frame_recentering':False,'frames':records}


def build(hero):
    if hero == 'kaelen':
        cell=(128,112); pivot=(64,104); world_scale=1.16
        specs={
          'idle_front':('idle-front.png',2,475,833,39,74),
          'idle_back':('idle-back.png',2,490,812,62,74),
          'walk_front':('walk-front-v1.png',4,230,727,151,74),
          'walk_back':('walk-back.png',4,315,666,30,74),
          'attack_front':('attack-front.png',3,300,667,118,74),
          'attack_back':('attack-back.png',3,300,684,120,74),
          'damage_front':('damage-front.png',3,335,637,93,74),
          'damage_back':('damage-back.png',3,330,659,35,74),
        }
    else:
        cell=(112,112); pivot=(56,104); world_scale=1.08
        specs={
          'idle_front':('idle.png',2,300,686,33,80,0,2),
          'idle_back':('idle.png',2,290,703,29,80,1,2),
          'walk_front':('walk-front.png',4,300,701,15,80),
          'walk_back':('walk-back.png',4,285,692,18,80),
          'attack_front':('cast-front.png',3,300,692,80,80),
          'attack_back':('cast-back.png',3,300,697,79,80),
          'damage_front':('hurt-defeat-clean.png',3,235,486,18,77,0,2),
          'damage_back':('hurt-defeat-clean.png',3,230,488,14,77,1,2),
        }
    result={}; sources={}
    for name,spec in specs.items():
        file,count,rx,base,top,height,*rows=spec
        result[name],sources[name]=strip(hero,file,count,rx,base,top,height,cell,pivot,
                                        row=rows[0] if rows else 0,source_rows=rows[1] if rows else 1)
    poses=[]
    for side in ['front','back']:
        poses+=result['idle_'+side]+result['walk_'+side]+result['attack_'+side]+result['damage_'+side]
    atlas=Image.new('RGBA',(cell[0]*12,cell[1]*2))
    for i,frame in enumerate(poses):atlas.alpha_composite(frame,((i%12)*cell[0],i//12*cell[1]))
    alpha=atlas.getchannel('A');atlas=atlas.convert('RGB').quantize(colors=128,method=Image.Quantize.MEDIANCUT,dither=Image.Dither.NONE).convert('RGBA');atlas.putalpha(alpha)
    output=ROOT/'app/src/main/assets/character_upgrade_v1'/hero;output.mkdir(parents=True,exist_ok=True)
    atlas.save(output/f'{hero}.png',optimize=True)
    if hero=='kaelen':
        source=DOCS/hero/'sources/canonical-upgraded.png'
        portrait=opaque(Image.open(source)).crop((105,145,345,445)).resize((128,160),Image.Resampling.NEAREST)
        portrait_source={'file':str(source.relative_to(ROOT)),'sha256':sha(source),'crop':[105,145,345,445],
                         'method':'crop of actual generated canonical art, not a separately painted bust'}
    else:
        source=DOCS/hero/'source/portrait-source.png';portrait=opaque(Image.open(source)).resize((128,160),Image.Resampling.NEAREST)
        portrait_source={'file':str(source.relative_to(ROOT)),'sha256':sha(source),'method':'hosted_imagegen portrait'}
    portrait.save(output/'portrait.png',optimize=True)
    hashes=[];contact=Image.new('RGBA',(cell[0]*12,2*(cell[1]+20)),'#25313d');draw=ImageDraw.Draw(contact)
    for i in range(24):
        x=i%12*cell[0];y=i//12*cell[1]
        frame=atlas.crop((x,y,x+cell[0],y+cell[1]));hashes.append(hashlib.sha256(frame.tobytes()).hexdigest())
        contact.alpha_composite(frame,(x,i//12*(cell[1]+20)));draw.text((x+2,i//12*(cell[1]+20)+cell[1]+3),ORDER[i%12],fill='white')
    assert len(set(hashes))==24
    doc=DOCS/hero;contact.resize((contact.width*2,contact.height*2),Image.Resampling.NEAREST).save(doc/'contact.png')
    for row,side in enumerate(['front','back']):
        for action,ids,durations in [('idle',[0,1],[350,350]),('walk',[2,3,4,5],[110]*4),
                                    ('attack',[6,7,8],[220,100,160]),('defeat',[10,11],[200,350])]:
            frames=[atlas.crop((i*cell[0],row*cell[1],(i+1)*cell[0],(row+1)*cell[1])).resize((cell[0]*3,cell[1]*3),Image.Resampling.NEAREST) for i in ids]
            frames[0].save(doc/f'{action}_{side}.webp',save_all=True,append_images=frames[1:],duration=durations,lossless=True,exact=True,loop=0)
    meta={'hero':hero.title(),'frame':cell,'pivot':pivot,'columns':12,'rows':['front_right','back_right'],
          'world_scale':world_scale,'frame_order':ORDER,'atlas_file':f'{hero}.png','atlas_sha256':sha(output/f'{hero}.png'),
          'portrait_file':'portrait.png','portrait_sha256':sha(output/'portrait.png'),
          'retained_rgba_bytes':cell[0]*cell[1]*24*4+128*160*4,'native_generated_dimensions_claimed':False,
          'normalization':'reviewed one scale/root per action strip, nearest normalization; original imagegen poses; no invented missing frames',
          'unique_pose_hashes':hashes,'sources':sources,'portrait_source':portrait_source}
    (output/'metadata.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2)+'\n')
    (doc/'normalization-report.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2)+'\n')
    print(hero,atlas.size,'24 distinct poses',meta['retained_rgba_bytes'])


if __name__=='__main__':
    for name in ['kaelen','vesper']:build(name)
