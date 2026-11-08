#!/usr/bin/env python3
"""Independent A08 checks; run: PYTHONPATH=/path/to/pillow python3 <this file>."""
import collections, hashlib, io, json, math, pathlib, struct, zlib
from PIL import Image, ImageDraw
R=pathlib.Path('docs/art/argon_production_v2'); O=pathlib.Path('docs/quality-loop/ARGON-WORK-002/a08_evidence'); O.mkdir(parents=True,exist_ok=True)
m=json.loads((R/'ASSET_MANIFEST_V2.json').read_text()); assets=[(b,a) for b, group in m['batches'].items() for a in group]
def validate(raw, dims):
    if raw[:8] != b'\x89PNG\r\n\x1a\n': raise ValueError('bad PNG signature')
    off=8; chunks=[]
    while off<len(raw):
        if len(raw)-off<12: raise ValueError('truncated chunk')
        n=struct.unpack('>I',raw[off:off+4])[0]; end=off+12+n
        if end>len(raw): raise ValueError('truncated payload')
        typ=raw[off+4:off+8]; data=raw[off+8:off+8+n]; crc=struct.unpack('>I',raw[off+8+n:end])[0]
        if zlib.crc32(typ+data)&0xffffffff != crc: raise ValueError('CRC mismatch '+repr(typ))
        chunks.append(typ); off=end
        if typ==b'IEND': break
    if not chunks or chunks[0]!=b'IHDR' or chunks[-1]!=b'IEND' or off!=len(raw) or b'IDAT' not in chunks: raise ValueError('missing required chunk or trailing data')
    im=Image.open(io.BytesIO(raw)); im.load()
    if im.format!='PNG' or im.mode!='RGBA': raise ValueError('not RGBA PNG')
    if im.size!=tuple(dims): raise ValueError('wrong dimensions')
    return im

def image_diff(a,b):
    p=list(a.getdata()); q=list(b.getdata()); return round(sum(abs(x-y) for px,py in zip(p,q) for x,y in zip(px,py))/(len(p)*4),3)
results=[]
for batch,a in assets:
    raw=pathlib.Path(a['path']).read_bytes(); im=validate(raw,(a['width'],a['height'])); alpha=collections.Counter(im.getchannel('A').getdata()); colors=len(set(im.getdata())); fw=a.get('frame_width',a['width']); fh=a.get('frame_height',a['height']); n=a['frames']; assert im.width==fw*n and im.height==fh
    frames=[im.crop((i*fw,0,(i+1)*fw,fh)) for i in range(n)]
    hashes=[hashlib.sha256(x.tobytes()).hexdigest() for x in frames]; nonempty=[sum(1 for p in f.getchannel('A').getdata() if p) for f in frames]
    row={'path':a['path'],'sha256':hashlib.sha256(raw).hexdigest(),'expected_sha256':a['sha256'],'sha_matches':hashlib.sha256(raw).hexdigest()==a['sha256'],'bytes':len(raw),'bytes_match':len(raw)==a['size_bytes'],'dimensions':list(im.size),'rgba_colors':colors,'alpha_zero':alpha[0],'alpha_semi':sum(v for k,v in alpha.items() if 0<k<255),'alpha_opaque':alpha[255],'alpha_stats_match':{'opaque_255':alpha[255],'semi_1_254':sum(v for k,v in alpha.items() if 0<k<255),'transparent_0':alpha[0],'total':im.width*im.height}==a['alpha_stats'],'png_ihdr_matches':raw[24]==a['bit_depth'] and raw[25]==a['color_type'],'bbox':im.getchannel('A').getbbox(),'pivot':a['pivot'],'pivot_in_or_on_cell':0<=a['pivot']['x']<=fw and 0<=a['pivot']['y']<=fh,'frames':n,'fps':a['fps'],'loop_mode':a.get('loop_mode'),'unique_frames':len(set(hashes)),'frame_active_pixels':nonempty,'frame_alpha_bounds':[f.getchannel('A').getbbox() for f in frames],'adjacent_delta_rgba':[image_diff(frames[i],frames[(i+1)%n]) for i in range(n)] if n>1 else []}
    if batch=='batch_01_terrain':
        p=im.load(); row['edge_delta_lr']=round(sum(sum(abs(p[0,y][c]-p[im.width-1,y][c]) for c in range(3)) for y in range(im.height))/(im.height*3),2); row['edge_delta_tb']=round(sum(sum(abs(p[x,0][c]-p[x,im.height-1][c]) for c in range(3)) for x in range(im.width))/(im.width*3),2)
    results.append(row)
# Actual repeated tiles, no interpolation: 4x4 at 1x and the same at 4x.
terrain=[a for b,a in assets if b=='batch_01_terrain']; tile=Image.new('RGB',(4*256,2*(256+24)),(20,23,29)); d=ImageDraw.Draw(tile)
for i,a in enumerate(terrain):
    im=Image.open(a['path']).convert('RGB'); x=(i%4)*256; y=(i//4)*280; d.text((x+2,y+4),a['id'].replace('terrain_',''),fill='white')
    for yy in range(4):
        for xx in range(4): tile.paste(im,(x+xx*64,y+24+yy*64))
tile.save(O/'terrain_4x4_1x.png'); tile.resize((tile.width*4,tile.height*4),Image.Resampling.NEAREST).save(O/'terrain_4x4_4x.png')
# Transparent checkerboard and true scale, all images and frames; enlarge with nearest neighbor separately.
items=[]
for b,a in assets:
    if b=='batch_01_terrain': continue
    im=Image.open(a['path']).convert('RGBA'); fw=a.get('frame_width',a['width']); fh=a.get('frame_height',a['height']);
    for i in range(a['frames']): items.append((a['id'] + (f' {i+1}' if a['frames']>1 else ''),im.crop((i*fw,0,(i+1)*fw,fh))))
cellw=144; cellh=160; sheet=Image.new('RGB',(cellw*6,cellh*math.ceil(len(items)/6)),(25,27,33)); draw=ImageDraw.Draw(sheet)
for i,(name,im) in enumerate(items):
    x=(i%6)*cellw; y=(i//6)*cellh; draw.text((x+1,y+2),name[:23],fill='white'); bg=Image.new('RGBA',im.size)
    pix=bg.load()
    for yy in range(im.height):
        for xx in range(im.width): pix[xx,yy]=((90,90,90,255) if ((xx//8+yy//8)%2) else (150,150,150,255))
    bg.alpha_composite(im); sheet.paste(bg.convert('RGB'),(x,y+22))
sheet.save(O/'props_vfx_1x.png'); sheet.resize((sheet.width*4,sheet.height*4),Image.Resampling.NEAREST).save(O/'props_vfx_4x.png')
# Negative tests invoke same validator, and require each rejection.
a=assets[0][1]; raw=pathlib.Path(a['path']).read_bytes(); mutations={'truncated':(raw[:33],(a['width'],a['height'])),'bad_crc':(raw[:29]+bytes([raw[29]^1])+raw[30:],(a['width'],a['height'])),'wrong_dimensions':(raw,(a['width']+1,a['height']))}; negative={}
for name,(data,dims) in mutations.items():
    try: validate(data,dims); negative[name]='ACCEPTED (FAIL)'
    except Exception as ex: negative[name]='REJECTED: '+str(ex)
assert all(x.startswith('REJECTED:') for x in negative.values())
out={'reference_sha':'17c99aea77860958346acf59cecab18aa514c9c1','negative_tests':negative,'assets':results}; (O/'measurements.json').write_text(json.dumps(out,indent=2)+'\n')
print('assets',len(results),'hash mismatches',sum(not r['sha_matches'] for r in results),'bytes mismatches',sum(not r['bytes_match'] for r in results)); print('manifest alpha mismatches',sum(not r['alpha_stats_match'] for r in results),'IHDR mismatches',sum(not r['png_ihdr_matches'] for r in results)); print('negative',negative)
for r in results: print(pathlib.Path(r['path']).name,r['rgba_colors'],r['unique_frames'],'/',r['frames'],'alpha',r['alpha_zero'],r['alpha_semi'],r['alpha_opaque'],'edge',r.get('edge_delta_lr'),r.get('edge_delta_tb'),'delta',r['adjacent_delta_rgba'])
