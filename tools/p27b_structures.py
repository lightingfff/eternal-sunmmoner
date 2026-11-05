#!/usr/bin/env python3
"""Cut out genuine Arena-generated standalone Hall/Gate art into old canvas contracts.

Image tool outputs RGB with painted fake backgrounds even when asked for alpha.
Only exterior-connected neutral checker / near-white is removed. Keep full-canvas
proportions (both generated sources match old aspect). Never change obstacle
coordinates, door triggers or camera to compensate for an art cutout.

The output is a candidate, not READY. Compare against ACTUAL engine screenshots.
Requires Pillow>=11. No user reference image is read or copied.
"""
from collections import deque
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageStat

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/p27b_sources'
OUT = ROOT / 'art/p27b_workbench_4x4/structures'
ASSETS = ROOT / 'app/src/main/assets/premium_slice'
SPECS = {
    'summoning_hall': {'source': 'summoning_hall_source.png', 'size': (268,206),
                       'pivot': (134,206), 'focus': (111,176), 'background': 'checker'},
    'tower_gate': {'source': 'tower_gate_rework.png', 'size': (260,224),
                   'pivot': (130,224), 'focus': (130,120), 'background': 'white'},
}


def cutout(im: Image.Image, background: str) -> Image.Image:
    """Flood neutral pixels linked to the perimeter, never cut interior stone."""
    src = im.convert('RGB')
    w, h = src.size
    # Fast bytes access: avoid per-pixel Python object allocation for every iteration.
    data = src.tobytes()
    allowed = bytearray(w*h)
    for idx in range(w*h):
        off = idx*3
        r,g,b = data[off],data[off+1],data[off+2]
        if background == 'checker':
            allowed[idx] = int(min(r,g,b) >= 180 and max(r,g,b)-min(r,g,b) <= 12)
        else:
            allowed[idx] = int(min(r,g,b) >= 245 and max(r,g,b)-min(r,g,b) <= 12)
    exterior = bytearray(w*h)
    q: deque[int] = deque()
    for i in (*range(w), *range((h-1)*w, h*w),
              *(y*w for y in range(1,h-1)),
              *(y*w+w-1 for y in range(1,h-1))):
        if allowed[i] and not exterior[i]:
            q.append(i)
            exterior[i] = 1
    while q:
        i = q.popleft()
        x, y = i%w, i//w
        for j in ((i-1 if x else -1), (i+1 if x+1<w else -1),
                  (i-w if y else -1), (i+w if y+1<h else -1)):
            if j>=0 and allowed[j] and not exterior[j]:
                exterior[j] = 1
                q.append(j)
    alpha = Image.frombytes('L', (w,h), bytes(255 * (1-v) for v in exterior))
    output = src.convert('RGBA')
    output.putalpha(alpha)
    return output


def downscale(im: Image.Image, dims: tuple[int,int]) -> Image.Image:
    # Pillow downscales RGBA in premultiplied form, preventing checkerboard/white
    # bleeding into cutout edges. Quantization is not applied to alpha.
    small = im.resize(dims, Image.Resampling.BOX)
    a = small.getchannel('A').point(lambda v: 255 if v >= 128 else 0)
    small.putalpha(a)
    return small


def ground_gate_step(image: Image.Image) -> Image.Image:
    """Extend 3 missing *source-authored* step rows to the inherited ground pivot.

    The generated cutout ends at y=220 under the central stair, while P21's 260x224
    bottom-centre pivot has opaque masonry at y=223. Pad the same painted step
    texture downward by 3 px, not the whole building: moving the sprite would
    also move its portal opening away from the existing dynamic rift at (130,120).
    This is edge cleanup, NOT a new architectural illustration.
    """
    out = image.copy()
    for dst_y, src_y in ((221,218),(222,219),(223,220)):
        for x in range(44,233):
            if out.getpixel((x,dst_y))[3] == 0:
                rgba = image.getpixel((x,src_y))
                if rgba[3] == 255:
                    out.putpixel((x,dst_y),rgba)
    return out


def inspect(im: Image.Image, old: Image.Image) -> dict:
    mask = im.getchannel('A')
    oldmask = old.getchannel('A')
    new_pixels = mask.tobytes()
    old_pixels = oldmask.tobytes()
    intersection = sum(a>0 and b>0 for a,b in zip(new_pixels,old_pixels))
    count_new = sum(a>0 for a in new_pixels)
    count_old = sum(b>0 for b in old_pixels)
    a = ImageStat.Stat(im.convert('RGB')).mean
    return {'bbox_alpha': list(mask.getbbox() or ()),
            'opaque_pixels': count_new, 'transparent_pixels': len(new_pixels)-count_new,
            'alpha_intersection_over_union_old': round(intersection/(count_new+count_old-intersection),3),
            'mean_rgb_including_transparent_not_usable_for_palette': [round(c,1) for c in a]}


def main() -> None:
    OUT.mkdir(parents=True,exist_ok=True)
    rows = []
    manifest = {}
    for name, spec in SPECS.items():
        source = SOURCE/spec['source']
        old = Image.open(ASSETS/f'{name}.png').convert('RGBA')
        generated = Image.open(source)
        assert old.size == spec['size']
        new = downscale(cutout(generated,spec['background']),old.size)
        if name == 'tower_gate':
            new = ground_gate_step(new)
        output = OUT/f'{name}.png'
        new.save(output)
        metrics = inspect(new,old)
        if metrics['transparent_pixels'] < old.width*old.height*0.12:
            raise ValueError(f'{name}: fake transparency; review cutout classifier')
        manifest[name] = {
            'id': name, 'source': str(source.relative_to(ROOT)),
            'source_sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
            'source_size': list(generated.size), 'file': output.name,
            'size': list(new.size), 'sha256': hashlib.sha256(output.read_bytes()).hexdigest(),
            'alpha': 'binary true RGBA', 'pivot': spec['pivot'], 'focus': spec['focus'],
            'provider': f'PixelStructureAtlas.{name if name=="tower_gate" else "summoningHallStructure"}',
            'source_type': 'original Arena image generation edited from game-owned P21 sprite',
            'qa_status': 'DRAFT - not integrated; compare contact/engine', **metrics,
        }
        rows.append((name,old,new))
        print(name, metrics)
    (OUT/'manifest_candidates.json').write_text(json.dumps(manifest,indent=2)+'\n')

    board = Image.new('RGB',(1100,570),'#23323b')
    draw=ImageDraw.Draw(board)
    for row,(name,old,new) in enumerate(rows):
        y=row*280+24
        draw.text((16,y-14),f'{name} : OLD original PNG (left) vs NEW cutout candidate (right); NOT ENGINE',
                  fill='#f5f1e7',font=ImageFont.load_default())
        for x,im in ((18,old),(540,new)):
            base=Image.new('RGBA',im.size,'#3a4c57')
            base.alpha_composite(im)
            board.paste(base.convert('RGB'),(x,y+12))
            draw.line((x+im.width//2,y+12,x+im.width//2,y+12+im.height),fill='#d5ad72')
    board.save(OUT/'CONTACT_STRUCTURES_CANDIDATE.png')


if __name__ == '__main__':
    main()
