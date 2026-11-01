#!/usr/bin/env python3
"""Original Sanctuary art recipes. Pillow is an offline authoring dependency, never an app dependency.
Tiles are painted on a torus; all variants share perimeter motifs and directional light.
Run from any directory: python3 tools/generate_sanctuary_art.py
"""
from pathlib import Path
from PIL import Image, ImageDraw
import json, random, hashlib
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'app/src/main/assets/sanctuary_p03'
OUT.mkdir(parents=True, exist_ok=True)
manifest = {'version': 1, 'light': 'upper-left; world SanctuaryLight', 'logical_pixel_scale': 1,
            'generator': 'tools/generate_sanctuary_art.py', 'assets': []}
def save(image, name, usage, anchor=None):
    image.save(OUT/(name+'.png'), optimize=True)
    manifest['assets'].append({'id':name, 'path':name+'.png', 'size':list(image.size),
        'ground_anchor':anchor or [image.width//2,image.height], 'runtime_usage':usage,
        'sha256':hashlib.sha256((OUT/(name+'.png')).read_bytes()).hexdigest()})
class Tile:
    def __init__(self, base):
        self.im=Image.new('RGBA',(192,192),base); self.d=ImageDraw.Draw(self.im)
    def poly(self, pts, fill):
        for oy in (0,64,128):
            for ox in (0,64,128): self.d.polygon([(x+ox,y+oy) for x,y in pts],fill=fill)
    def line(self, pts, fill, width=1):
        for oy in (0,64,128):
            for ox in (0,64,128): self.d.line([(x+ox,y+oy) for x,y in pts],fill=fill,width=width)
    def image(self): return self.im.crop((64,64,128,128))
# Authored blades: five connected planes with low, varied posture; motifs are deliberately grouped.
def tuft(t,x,y,size=1):
    t.poly([(x-4,y+1),(x-3,y-2),(x,y),(x+3,y-1),(x+6,y+2),(x+1,y+3)],'#243c32')
    t.poly([(x-2,y),(x-5,y-4),(x-2,y-3),(x+1,y),(x,y-6-size),(x+2,y-4),(x+3,y),(x+7,y-4),(x+5,y+1)],'#456244')
    t.line([(x-4,y-4),(x-2,y-3),(x,y)],'#6a7c51')
    t.line([(x+1,y-6-size),(x+2,y-4)],'#81905b')
    t.line([(x+3,y),(x+5,y-2)],'#536d46')
def grass(v):
    t=Tile('#344b3b')
    # Shared boundary vegetation, no bare tile border. Variants alter only the interior.
    for x,y in [(0,17),(34,0)]: tuft(t,x,y)
    patches=[
        [(16,18),(43,43),(17,49)], [(18,39),(45,20)],
        [(13,22),(39,31),(46,51)], [(19,18),(44,46)],
        [(15,35),(40,19),(38,48)], [(24,25),(45,49)],
        [(14,48),(44,19),(28,38)], [(19,19),(44,36),(22,51)]
    ][v%8]
    r=random.Random(730+v)
    for i,(x,y) in enumerate(patches):
        x+=r.randrange(-4,5);y+=r.randrange(-3,4)
        t.poly([(x-10,y+1),(x-8,y-4),(x-2,y-7),(x+7,y-4),(x+11,y),(x+6,y+4),(x-3,y+5)],'#304736')
        if i==v%4:
            t.poly([(x-5,y+2),(x-4,y-1),(x+1,y-2),(x+7,y),(x+5,y+3),(x,y+4)],'#484537')
        for dx,dy in [(-5,-1),(2,-3),(6,2),(-1,4)][:3+(v%2)]:tuft(t,x+dx,y+dy,v%2)
    # Clover-like moss islands use connected leaves, not loose pixels.
    for x,y in [(29+(v%3)*3,14),(10,32+v%4),(49,53)]:
        t.poly([(x-4,y),(x-2,y-2),(x,y-1),(x+2,y-3),(x+4,y-1),(x+2,y+1),(x-1,y+2)],'#4e6543')
        t.line([(x-2,y-1),(x,y)],'#64764a')
    return t.image()
def masonry(v,large):
    t=Tile('#29343a');r=random.Random(90+v)
    heights=[12,20,14,18] if large else [7,9,8,7,9,8,7,9]
    y=0
    for row,h in enumerate(heights):
        widths=([29,15,20] if row%2==0 else [17,27,20]) if large else ([11,14,12,15,12] if row%2==0 else [14,10,15,11,14])
        offset=[-9,-20,-3,-15][row%4] if large else -[3,8,12][row%3]
        x=offset
        # Repeat one extra course at both sides for toroidal edge continuity.
        for col in range(len(widths)+2):
            w=widths[col%len(widths)];x0=x+1;y0=y+1;x1=x+w-1;y1=y+h-1
            # Perimeter stones use identical shape/value across all variants.
            interior = x0>3 and x1<61 and y0>3 and y1<61
            tone=(v+row+col)%4 if interior else (row+col)%4
            base=['#626e72','#707a7b','#59666d','#687477'][tone]
            pts=[(x0+2,y0),(x1-3,y0),(x1,y0+2),(x1-1,y1-1),(x1-3,y1),(x0+1,y1),(x0,y1-3),(x0,y0+2)]
            t.poly(pts,'#3c4b53')
            t.poly([(a,b-1 if b>y0+3 else b) for a,b in pts],base)
            t.line([(x0,y1-3),(x0,y0+2),(x0+2,y0),(x1-3,y0)],'#9b9e8b')
            t.line([(x1,y0+3),(x1-1,y1-1),(x1-3,y1),(x0+3,y1)],'#3e4b53')
            if interior and (row+col+v)%3==0:
                cx=x0+w//2; cy=y0+h//2
                t.line([(cx-2,y0+1),(cx-1,cy-1),(cx+2,cy),(cx,cy+3)],'#37454a')
                t.line([(cx+2,cy+1),(cx+3,cy+1)],'#87918a')
            if (row*3+col+v)%5==0 and interior:
                t.poly([(x0+2,y1-2),(x0+5,y1-3),(x0+8,y1-1),(x0+6,y1),(x0+2,y1)],'#455842')
                t.line([(x0+3,y1-2),(x0+5,y1-2)],'#738264')
            x+=w
        y+=h
    return t.image()
def soil(v,rocky=False):
    t=Tile('#514b3b');r=random.Random(431+v)
    for x,y in [(4,17),(20,36),(47,18),(43,53)]:
        # Low-contrast angular sediment deposits; no repeating sine-wave ovals.
        t.poly([(x-10,y),(x-7,y-4),(x-1,y-5),(x+10,y-2),(x+12,y+2),(x+2,y+4),(x-8,y+3)],'#554e3e')
        t.line([(x-7,y+2),(x-1,y+2),(x+2,y+1)],'#665c46')
    for i in range(17 if rocky else 5):
        x=r.randrange(7,55);y=r.randrange(8,56);w=r.randrange(3,8)
        t.poly([(x,y),(x+2,y-3),(x+w-2,y-2),(x+w,y+1),(x+w-2,y+3),(x+1,y+2)],'#3b4140')
        t.poly([(x,y),(x+2,y-3),(x+w-2,y-2),(x+w-1,y)],'#737a70')
        t.line([(x+1,y-1),(x+2,y-3),(x+w-3,y-2)],'#929681')
    if not rocky:
        for x,y in [(17+v%3,24),(40,46-v%4)]:
            t.line([(x-7,y+3),(x,y),(x+4,y-4),(x+10,y-5)],'#3d3e32',2)
            t.line([(x,y-1),(x+4,y-5),(x+9,y-6)],'#7b694b')
    return t.image()
def wood(v):
    t=Tile('#2d302e'); y=0
    for i,h in enumerate([13,18,15,18]):
        base=['#66543e','#705d43','#5f513c'][i%3]
        t.poly([(0,y+1),(63,y+1),(63,y+h-2),(0,y+h-2)],base)
        t.line([(0,y+1),(63,y+1)],'#9b8058')
        t.line([(0,y+h-2),(63,y+h-2)],'#40392e')
        end=(i*21+v*7)%64
        t.line([(end,y+2),(end-1,y+h-3)],'#33332c')
        for g in range(2,h-3,4):
            shift=(i*9+g*3+v*5)%64
            t.line([(shift-32,y+g),(shift-19,y+g),(shift-12,y+g+1),(shift+3,y+g+1),(shift+12,y+g)],'#514633')
            t.line([(shift-19,y+g-1),(shift-12,y+g),(shift+1,y+g)],'#7d6849')
        if i%2==v%2:
            x=16+(i*13+v*3)%32;cy=y+h//2
            t.poly([(x-7,cy),(x-3,cy-2),(x+3,cy-2),(x+7,cy),(x+2,cy+2),(x-3,cy+2)],'#8c7250')
            t.line([(x-6,cy),(x-2,cy-1),(x+2,cy-1),(x+5,cy)],'#3e392d')
        y+=h
    return t.image()
def boulder():
    im=Image.new('RGBA',(48,38));d=ImageDraw.Draw(im)
    d.polygon([(4,30),(6,18),(15,7),(28,5),(39,15),(44,29),(34,35),(13,35)],fill='#26343c')
    d.polygon([(6,28),(8,18),(16,9),(27,7),(23,20),(14,28)],fill='#87928b')
    d.polygon([(16,10),(27,7),(37,16),(32,23),(23,20)],fill='#697c7d')
    d.polygon([(23,21),(32,24),(38,17),(42,28),(33,33),(15,33),(14,29)],fill='#485c66')
    d.line([(16,10),(14,17),(19,21),(17,26)],fill='#324650',width=1)
    d.line([(19,21),(25,21),(28,26)],fill='#30414a')
    d.polygon([(9,16),(14,9),(21,7),(25,8),(22,12),(18,12),(17,17),(13,19)],fill='#3e5944')
    d.line([(12,13),(15,10),(20,9)],fill='#8a9566',width=2)
    d.polygon([(31,31),(34,28),(39,30),(37,33)],fill='#657b4f')
    return im
def verge():
    t=Tile((0,0,0,0))
    for x,y in [(3,12),(24,6),(42,19),(12,38),(35,48),(58,36),(55,60),(7,60)]:tuft(t,x,y)
    return t.image()
save(verge(),'verge','SanctuaryWorldRenderer; transparent terrain transition clusters')
def tree(w,h,kind):
    im=Image.new('RGBA',(w,h));d=ImageDraw.Draw(im)
    def pts(points):return [(round(x*w/114),round(y*h/132)) for x,y in points]
    def poly(p,c):d.polygon(pts(p),fill=c)
    def line(p,c,width=1):d.line(pts(p),fill=c,width=width)
    # Branching bole, knotted bark ridges and asymmetric buttress roots.
    poly([(28,124),(44,114),(47,86),(42,65),(26,51),(32,48),(51,64),(55,83),(61,66),(82,48),(87,48),(68,72),(65,99),(69,115),(86,124),(67,122),(57,117),(45,123)],'#1f3030')
    poly([(42,119),(51,108),(51,85),(47,66),(35,55),(52,67),(59,89),(59,111),(67,119),(56,114),(49,119)],'#655a43')
    line([(48,116),(54,104),(53,86),(49,74)],'#99815a',2)
    line([(60,112),(61,96),(57,77)],'#3c4035',2)
    line([(54,100),(58,96),(58,90)],'#343a31')
    line([(45,119),(39,122)],'#697453')
    pal= ['#1e3534','#304d43','#476354','#647b5c','#8f9c70'] if kind!=2 else ['#2c323e','#454555','#5d596c','#777185','#a39a9a']
    if kind==1:pal=['#20363e','#344d57','#4b6570','#708184','#95a299']
    if kind==3:
        masses=[(56,75,32),(53,61,29),(57,45,24),(54,31,19),(55,18,12)]
    elif kind==1:
        masses=[(28,65,23),(70,55,24),(48,40,27),(73,27,19),(50,18,18)]
    elif kind==2:
        masses=[(25,62,23),(81,57,23),(54,39,28),(28,30,20),(69,20,22)]
    else:masses=[(29,65,26),(83,66,24),(56,48,32),(30,34,24),(78,32,23),(54,19,24)]
    for index,(cx,cy,r) in enumerate(masses):
        outline=[(cx-r,cy+4),(cx-r+4,cy-4),(cx-r+3,cy-8),(cx-r//2,cy-12),(cx-r//3,cy-18),(cx+1,cy-19),(cx+r//3,cy-14),(cx+r//2+3,cy-13),(cx+r-2,cy-4),(cx+r,cy+6),(cx+r-8,cy+8),(cx+r//2,cy+14),(cx+4,cy+11),(cx-4,cy+16),(cx-r//2,cy+12)]
        poly(outline,pal[1])
        poly([(cx-r+1,cy+3),(cx-r//2,cy+6),(cx-3,cy+9),(cx+r//2,cy+5),(cx+r-1,cy+5),(cx+r-8,cy+9),(cx+r//2,cy+14),(cx+4,cy+11),(cx-4,cy+16),(cx-r//2,cy+12)],pal[0])
        # A small set of broad, shaped leaf plates follows the branch flow, each with a dark underside.
        for dx,dy,span in [(-r//2,-3,18),(1,-11,20),(r//2,0,16),(-5,7,18),(-r//2,-11,16)]:
            x=cx+dx;y=cy+dy
            poly([(x-span//2,y+1),(x-3,y-4),(x+1,y-5),(x+5,y-2),(x+span//2,y-1),(x+span//2-2,y+4),(x+1,y+6),(x-4,y+4)],pal[2 if dy<2 else 1])
            if dy<2:
                line([(x-span//2+2,y),(x-2,y-3),(x+1,y-4),(x+4,y-2)],pal[3])
                if dx<=0:line([(x-2,y-4),(x+1,y-5)],pal[4])
        line([(cx-7,cy+5),(cx-3,cy+3),(cx+2,cy+4)],pal[0],2)
    poly([(27,124),(35,121),(42,123),(40,126),(30,126)],'#596654')
    return im

def pillar():
    im=Image.new('RGBA',(44,92));d=ImageDraw.Draw(im)
    d.polygon([(5,85),(6,76),(12,70),(12,21),(8,18),(7,9),(33,7),(38,13),(35,20),(31,23),(31,69),(38,76),(39,86),(31,89),(12,89)],fill='#25343c')
    d.polygon([(8,77),(33,77),(36,82),(12,84)],fill='#8a9387')
    d.polygon([(12,25),(17,23),(17,70),(12,73)],fill='#8b958e')
    d.polygon([(18,23),(27,23),(27,72),(18,72)],fill='#5e757b')
    d.polygon([(28,23),(31,22),(31,70),(28,74)],fill='#3f555f')
    d.polygon([(8,10),(31,8),(35,12),(30,15),(10,15)],fill='#95a197')
    d.line([(10,16),(30,16),(34,13)],fill='#536b73',width=2)
    d.line([(25,26),(22,32),(24,38),(20,43),(22,48)],fill='#2c414d')
    d.line([(15,57),(20,58),(23,62)],fill='#304650')
    d.line([(19,30),(19,51)],fill='#91a3a0')
    d.polygon([(9,79),(13,75),(16,76),(18,81),(13,82)],fill='#4b664e')
    d.line([(11,78),(14,77)],fill='#8a9966')
    return im

def barrels():
    im=Image.new('RGBA',(52,44));d=ImageDraw.Draw(im)
    d.polygon([(26,9),(40,6),(47,11),(47,33),(28,35)],fill='#30382f')
    d.polygon([(27,10),(39,8),(44,11),(31,14)],fill='#9c8257')
    d.polygon([(29,15),(40,13),(40,31),(29,33)],fill='#705c40')
    d.polygon([(41,13),(45,12),(45,31),(41,33)],fill='#493f30')
    d.line([(30,16),(38,29)],fill='#a08256',width=2)
    d.line([(33,14),(33,31)],fill='#4d4533')
    for x,y in [(5,12),(15,20)]:
        d.polygon([(x,y+3),(x+13,y+3),(x+15,y+10),(x+13,y+20),(x+2,y+20),(x,y+13)],fill='#342f26')
        d.polygon([(x+2,y+4),(x+12,y+4),(x+13,y+11),(x+11,y+18),(x+3,y+18)],fill='#806747')
        d.polygon([(x+10,y+4),(x+12,y+4),(x+13,y+11),(x+11,y+18),(x+9,y+18)],fill='#514734')
        for dx in [4,8]:d.line([(x+dx,y+5),(x+dx-1,y+11),(x+dx,y+18)],fill='#4b4230')
        for dy in [6,15]:
            d.line([(x+1,y+dy),(x+13,y+dy)],fill='#344650',width=2)
            d.line([(x+2,y+dy),(x+9,y+dy)],fill='#8a9890')
        d.polygon([(x,y+3),(x+3,y),(x+10,y),(x+13,y+3),(x+10,y+5),(x+3,y+5)],fill='#aa8b5a')
        d.line([(x+3,y+2),(x+10,y+2)],fill='#5b4b34')
    return im

families=[('grass',8,grass),('flagstone',8,lambda v:masonry(v,True)),('cobble',6,lambda v:masonry(v,False)),('soil',6,soil),('gravel',4,lambda v:soil(v,True)),('wood',6,wood)]
for name,n,fn in families:
    reference=fn(0)
    for v in range(n):
        image=fn(v)
        # Shared two-pixel perimeter is the atlas adjacency contract. Interior motifs may vary freely.
        for box in [(0,0,64,2),(0,62,64,64),(0,0,2,64),(62,0,64,64)]:image.paste(reference.crop(box),box)
        save(image,f'{name}_{v}',f'PixelTileAtlas.{name}; 64 world units; nearest-neighbor oblique ground')
save(boulder(),'boulder','PixelStructureAtlas.mossyBoulderSprite; existing obstacle anchors and shadow silhouette',[24,35])
for name,w,h,kind in [('canopy_a',114,132,0),('canopy_b',96,114,1),('oak',112,136,0),('twisted',112,132,2),('pine',96,128,3),('cypress',48,88,3)]:
    save(tree(w,h,kind),name,'PixelStructureAtlas; existing tree position/scale; silhouette shadows',[w//2,round(h*124/132)])
save(pillar(),'pillar','PixelStructureAtlas.obsidianColumnSprite; existing column footprint',[22,89])
save(barrels(),'barrels','PixelStructureAtlas.barrelClusterSprite; existing props and shadow silhouette',[26,40])
(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(f'Generated {len(manifest["assets"])} runtime assets in {OUT}')
