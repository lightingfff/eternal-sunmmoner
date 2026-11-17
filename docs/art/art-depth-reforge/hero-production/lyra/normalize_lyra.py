"""Normalize inspected imagegen strips without drawing or per-frame auto-centering.

Each source strip has one reviewed common scale, root-x and ground baseline.
Column occupancy isolates figures; placement still uses the source's logical slots,
not each frame's bbox center. Collapse height and gait motion remain intact.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
OUT = ROOT / 'app/src/main/assets/character_upgrade_v1/lyra'
CELL = (96, 112)
PIVOT = (48, 104)
ALPHA_THRESHOLD = 96
SPECS = {
    'walk_front': {'count': 4, 'root_x': 300, 'baseline': 697, 'body_top': 13},
    'walk_back': {'count': 4, 'root_x': 280, 'baseline': 710, 'body_top': 14},
    'attack_front': {'count': 3, 'root_x': 365, 'baseline': 695, 'body_top': 42},
    'attack_back': {'count': 3, 'root_x': 350, 'baseline': 719, 'body_top': 63},
    'state_front': {'count': 5, 'root_x': 265, 'baseline': 690, 'body_top': 26},
    'state_back': {'count': 5, 'root_x': 260, 'baseline': 695, 'body_top': 18},
}


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def spans(alpha: Image.Image) -> list[tuple[int, int]]:
    result, start = [], None
    for x in range(alpha.width):
        occupied = alpha.crop((x, 0, x + 1, alpha.height)).getbbox() is not None
        if occupied and start is None:
            start = x
        elif not occupied and start is not None:
            result.append((start, x))
            start = None
    if start is not None:
        result.append((start, alpha.width))
    return result


def normalize(name: str, spec: dict) -> tuple[list[Image.Image], dict]:
    source = HERE / 'references' / f'{name}_source.png'
    im = Image.open(source).convert('RGBA')
    alpha = im.getchannel('A').point(lambda a: 255 if a >= ALPHA_THRESHOLD else 0)
    im.putalpha(alpha)
    columns = spans(alpha)
    assert len(columns) == spec['count'], (name, columns)
    # Equal anatomical standing-height targets give one common scale for the clip.
    scale = 80 / (spec['baseline'] - spec['body_top'])
    frames, records = [], []
    for index, (left, right) in enumerate(columns):
        logical_slot_left = index * im.width / spec['count']
        crop = im.crop((left, 0, right, im.height))
        crop = crop.resize((round(crop.width * scale), round(crop.height * scale)), Image.Resampling.NEAREST)
        x = round(PIVOT[0] + (left - logical_slot_left - spec['root_x']) * scale)
        y = round(PIVOT[1] - spec['baseline'] * scale)
        target = Image.new('RGBA', CELL)
        target.alpha_composite(crop, (x, y))
        original_count = sum(crop.getchannel('A').histogram()[1:])
        actual_count = sum(target.getchannel('A').histogram()[1:])
        assert original_count == actual_count, f'{name}:{index} clipped {original_count-actual_count} pixels'
        bounds = target.getbbox()
        assert bounds and bounds[0] > 0 and bounds[1] > 0 and bounds[2] < CELL[0] and bounds[3] < CELL[1], (name,index,bounds)
        frames.append(target)
        records.append({'column':index, 'source_occupied_columns':[left,right], 'logical_slot_left':logical_slot_left,
                        'paste':[x,y], 'bbox':bounds, 'opaque_pixels':actual_count})
    return frames, {'source': str(source.relative_to(HERE)), 'source_sha256':sha(source),
                    'source_size':im.size, 'method':'hosted_imagegen', 'common_scale':scale,
                    'common_source_root_x':spec['root_x'], 'common_source_baseline':spec['baseline'],
                    'no_per_frame_fit_or_center':True, 'frames':records}


def checker(size: tuple[int, int], step=8) -> Image.Image:
    im = Image.new('RGBA', size, '#303742')
    draw = ImageDraw.Draw(im)
    for y in range(0,size[1],step):
        for x in range(0,size[0],step):
            if (x//step+y//step)%2:draw.rectangle((x,y,x+step-1,y+step-1),fill='#48515c')
    return im


def main() -> None:
    OUT.mkdir(parents=True,exist_ok=True)
    normalized, source_records = {}, {}
    for name,spec in SPECS.items():
        normalized[name],source_records[name] = normalize(name,spec)
    all_frames=[]
    for side in ['front','back']:
        states=normalized['state_'+side]
        all_frames += states[:2]+normalized['walk_'+side]+normalized['attack_'+side]+states[2:]
    atlas=Image.new('RGBA',(CELL[0]*12,CELL[1]*2))
    for i,frame in enumerate(all_frames):atlas.alpha_composite(frame,((i%12)*CELL[0],(i//12)*CELL[1]))
    alpha=atlas.getchannel('A')
    quantized=atlas.convert('RGB').quantize(colors=128,method=Image.Quantize.MEDIANCUT,dither=Image.Dither.NONE).convert('RGBA')
    quantized.putalpha(alpha);atlas=quantized
    atlas.save(OUT/'lyra.png',optimize=True)
    unique=[]
    for i in range(24):
        f=atlas.crop(((i%12)*96,(i//12)*112,(i%12+1)*96,(i//12+1)*112))
        unique.append(hashlib.sha256(f.tobytes()).hexdigest())
    assert len(set(unique))==24, 'All 24 delivered poses must be distinct'
    portrait_source=HERE/'references/portrait_source.png'
    portrait=Image.open(portrait_source).convert('RGBA')
    pa=portrait.getchannel('A').point(lambda a:255 if a>=ALPHA_THRESHOLD else 0);portrait.putalpha(pa)
    portrait=portrait.resize((128,160),Image.Resampling.NEAREST)
    portrait.save(OUT/'portrait.png',optimize=True)
    contact=checker((12*96,2*136))
    labels=['idle0','idle1','walk0','walk1','walk2','walk3','draw','release','recover','hurt','collapse','defeat']
    for i,frame in enumerate(all_frames):
        x=(i%12)*96;y=(i//12)*136
        contact.alpha_composite(atlas.crop(((i%12)*96,(i//12)*112,(i%12+1)*96,(i//12+1)*112)),(x,y))
        ImageDraw.Draw(contact).text((x+2,y+114),labels[i%12],fill='white')
    contact.resize((2304,544),Image.Resampling.NEAREST).save(HERE/'contact.png')
    for row,side in enumerate(['front','back']):
        for action,indices in {'idle':[0,1],'walk':[2,3,4,5],'attack':[6,7,8],'defeat':[10,11]}.items():
            fs=[]
            for i in indices:
                frame=atlas.crop((i*96,row*112,(i+1)*96,(row+1)*112))
                fs.append(frame.resize((288,336),Image.Resampling.NEAREST))
            duration={'idle':350,'walk':110,'attack':130,'defeat':250}[action]
            fs[0].save(HERE/f'{action}_{side}.webp',save_all=True,append_images=fs[1:],duration=duration,
                       lossless=True,exact=True,loop=0)
    meta={'hero':'Lyra','frame':CELL,'pivot':PIVOT,'columns':12,'rows':['front_right','back_right'],
          'world_scale':1.08,'frame_order':labels,'atlas_file':'lyra.png','atlas_sha256':sha(OUT/'lyra.png'),
          'portrait_file':'portrait.png','portrait_sha256':sha(OUT/'portrait.png'),
          'retained_rgba_bytes':96*112*24*4+128*160*4,'native_generated_dimensions_claimed':False,
          'normalization':'Imagegen source normalized to runtime cells; shared scale/root per clip; nearest-only; no drawn/invented frames',
          'left_direction':'root renderer derives via flip at draw; no separately authored left claim',
          'unique_pose_hashes':unique,'sources':source_records,
          'portrait_source':{'path':'references/portrait_source.png','sha256':sha(portrait_source)}}
    (OUT/'metadata.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2)+'\n')
    (HERE/'normalization-report.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'atlas':str(OUT/'lyra.png'),'size':atlas.size,'distinct_frames':len(set(unique)),
                      'retained_rgba_bytes':meta['retained_rgba_bytes'],'source_strips':len(SPECS)},indent=2))


if __name__=='__main__':main()
