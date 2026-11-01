#!/usr/bin/env python3
"""Original, deliberately clustered P1.5A pixel art. Pillow is an offline authoring tool.
No downloaded/reference-game art, noise textures, runtime generation or blur.
Run: python3 tools/generate_premium_slice.py
"""
from pathlib import Path
from dataclasses import dataclass
import hashlib
import json
from math import hypot
from PIL import Image, ImageDraw, ImageChops

OUT = Path(__file__).resolve().parents[1] / 'app/src/main/assets/premium_slice'
OUT.mkdir(parents=True, exist_ok=True)
W, H = 64, 96
# Seconds, not render ticks. The active pose starts at the gameplay contact marker.
CLIPS = {
    'idle': [.24, .16, .20, .24, .16, .20],
    'walk': [.08, .07, .07, .08, .08, .07, .07, .08],
    # Frame 4 is the first active pose, exactly at .22 / .30 seconds.
    'attack': [.06, .06, .06, .04, .045, .055, .07, .09],
    'skill': [.08, .08, .07, .07, .06, .07, .08, .09, .12],
    'hit': [.045, .065, .085, .105], 'rest': [1.0],
    'defeat': [.09, .11, .13, .15, .20, .32],
    'calm': [.24, .18, .20, .24, .16, .18], 'alert': [.16, .20, .18, .26],
    'fear': [.12, .08, .16, .09, .18, .17],
    'victory': [.14, .16, .22, .24, .20, .24],
}
INK = '#18222d'

@dataclass(frozen=True)
class Pose:
    """Authored joints and delayed secondary motion; no repeated source frames."""
    x: int = 0
    y: int = 0
    stride: int = 0
    cloth: int = 0
    hair: int = 0
    head: int = 0
    offhand: int = 0
    blink: bool = False
    lift: int = 0

# Every row is a deliberately staged key pose. Cloth/hair lag body reversals.
POSES = {
    'idle': [Pose(0,0,0,0,0), Pose(0,-1,0,1,0), Pose(0,-2,0,2,-1),
             Pose(0,-1,0,2,0,1), Pose(0,0,0,1,1,1,blink=True), Pose(0,1,0,-1,1)],
    'walk': [Pose(-1,-1,-4,2,1,0,0,False,0), Pose(0,-2,-3,3,1,0,1,False,4),
             Pose(1,-1,0,2,0,0,1,False,6), Pose(1,0,3,0,-1,0,0,False,3),
             Pose(0,-1,4,-2,-1,0,-1,False,0), Pose(-1,-2,3,-3,0,0,-1,False,4),
             Pose(-1,-1,0,-2,1,0,0,False,6), Pose(-1,0,-3,0,1,0,1,False,3)],
    'attack': [Pose(-1,0,-1,0,0,-1,-1), Pose(-2,-1,-2,1,1,-1,-2),
               Pose(-3,-2,-3,3,1,-1,-2), Pose(-3,-1,-3,4,2,0,-1),
               Pose(4,0,3,-3,-2,1,1), Pose(3,1,4,-4,-2,1,2),
               Pose(1,0,2,-2,-1,0,1), Pose(0,0,1,1,1,0,0)],
    'skill': [Pose(-1,0,-1,0,0,-1,0), Pose(-2,-1,-2,1,1,-1,-1),
              Pose(-2,-2,-2,3,1,-1,-2), Pose(-1,-2,-1,4,2,0,-2),
              Pose(4,-1,3,-2,-2,1,1), Pose(4,0,4,-4,-2,1,2),
              Pose(3,1,3,-5,-1,1,2), Pose(1,0,2,-3,0,0,1), Pose(0,0,0,1,1)],
    'hit': [Pose(-4,2,-2,3,2,-2,2), Pose(-5,4,-3,4,2,-2,3,True),
            Pose(-3,2,-2,2,1,-1,2), Pose(-1,1,-1,-1,-1,0,1)],
    'rest': [Pose(0,7,0,1,0)],
    'defeat': [Pose(-3,4,-2,2,1,-1,2), Pose(-5,9,-3,3,2,-2,3),
               Pose(-6,16,-3,4,2,-2,4), Pose(-5,24,-2,2,1,-1,3),
               Pose(-2,30,0,1,0,0,2), Pose(0,33,0,0,-1,0,1)],
    'calm': [Pose(0,0,0,-1,0,-1,0), Pose(0,-1,0,0,-1,-1,1),
             Pose(0,-2,0,1,-1,0,1), Pose(0,-1,0,2,0,0,0),
             Pose(0,0,0,1,1,0,1,True), Pose(0,1,0,0,1,0,0)],
    'alert': [Pose(1,-1,1,-1,0,1,-1), Pose(2,-2,2,-2,-1,2,-2),
              Pose(1,-2,2,-1,-1,1,-2), Pose(1,-1,1,1,0,0,-1)],
    # Back foot retreats; shoulders close, shield rises, eyes widen and hesitate.
    'fear': [Pose(-2,1,-1,1,1,-1,-2), Pose(-3,2,-3,2,2,-2,-3),
             Pose(-4,1,-4,3,2,-2,-4), Pose(-3,2,-4,2,1,-1,-4),
             Pose(-4,1,-3,1,1,-2,-3), Pose(-2,1,-2,0,0,-1,-2)],
    'victory': [Pose(0,0,1,0,0,0,0), Pose(1,-1,2,-1,0,1,-1),
                Pose(1,-2,2,-2,-1,1,-2), Pose(0,-2,1,-2,-1,0,-2),
                Pose(0,-1,0,-1,0,0,-1,True), Pose(0,0,0,1,1,0,0)],
}

# Shoulder/elbow/hand/tip arcs are species-specific, including recovery poses.
HERO_ARMS = {
    'attack': [((39,41),(38,38),(35,17)), ((36,37),(30,31),(21,12)),
               ((33,36),(26,29),(15,9)), ((34,35),(28,27),(18,6)),
               ((41,40),(44,41),(61,40)), ((44,45),(48,49),(61,57)),
               ((43,47),(46,53),(56,66)), ((41,46),(43,53),(57,39))],
    'skill': [((38,39),(36,34),(37,13)), ((35,37),(33,29),(34,7)),
              ((34,34),(33,25),(35,4)), ((37,33),(36,26),(42,5)),
              ((43,36),(46,35),(61,24)), ((45,40),(49,42),(61,43)),
              ((46,45),(50,49),(61,58)), ((43,47),(46,54),(58,67)),
              ((41,46),(43,53),(57,37))],
    'hit': [((38,44),(39,49),(49,33)), ((36,47),(36,54),(44,39)),
            ((38,47),(39,55),(51,42)), ((40,46),(42,54),(55,38))],
    'alert': [((40,42),(42,47),(54,28)), ((41,40),(43,44),(54,24)),
              ((42,41),(44,45),(56,26)), ((40,43),(43,48),(56,30))],
    'fear': [((37,41),(35,46),(43,25)), ((35,40),(32,45),(37,24)),
             ((34,39),(31,44),(36,22)), ((35,40),(32,44),(38,23)),
             ((34,40),(31,45),(37,24)), ((37,42),(35,47),(45,28))],
    'victory': [((41,42),(44,46),(57,29)), ((41,36),(43,33),(48,12)),
                ((39,29),(40,24),(43,4)), ((39,28),(41,23),(45,3)),
                ((40,30),(42,26),(47,5)), ((41,35),(43,32),(50,12))],
}
GOBLIN_ARMS = {
    'attack': [((43,48),(43,43),(42,27)), ((40,43),(37,37),(33,20)),
               ((37,41),(33,32),(25,15)), ((38,40),(34,31),(28,13)),
               ((43,45),(46,46),(61,43)), ((46,50),(49,54),(61,60)),
               ((46,56),(48,62),(57,75)), ((45,55),(48,62),(58,47))],
    'skill': [((41,45),(39,39),(40,21)), ((39,41),(36,34),(36,16)),
              ((38,38),(35,30),(34,12)), ((40,37),(38,29),(41,11)),
              ((44,42),(47,41),(61,33)), ((46,47),(49,48),(61,47)),
              ((47,53),(51,57),(61,66)), ((45,56),(48,63),(57,76)),
              ((44,55),(47,62),(57,46))],
    'hit': [((40,52),(39,57),(46,44)), ((37,55),(35,61),(40,48)),
            ((39,55),(39,63),(48,50)), ((43,54),(45,62),(54,46))],
    'alert': [((44,48),(46,52),(55,34)), ((44,45),(46,48),(54,29)),
              ((45,46),(48,50),(58,32)), ((44,49),(47,54),(57,36))],
    'fear': [((39,47),(36,49),(39,33)), ((36,46),(32,48),(32,31)),
             ((35,45),(31,47),(30,30)), ((36,46),(33,49),(34,31)),
             ((35,46),(31,48),(31,31)), ((39,48),(37,51),(41,35))],
    'victory': [((44,48),(48,50),(59,34)), ((44,42),(47,39),(52,21)),
                ((42,36),(44,30),(47,13)), ((42,35),(45,29),(49,12)),
                ((43,37),(46,32),(51,15)), ((44,42),(48,40),(54,23))],
}

def collapsed_actor(goblin, frame, back, left):
    """Kneel, buckle, fall and settle: newly drawn joints, never a sliding corpse."""
    im=Image.new('RGBA',(W,H));d=ImageDraw.Draw(im)
    def p(points,color,outline=INK): d.polygon(points,fill=color,outline=outline)
    def line(points,color,width=1): d.line(points,fill=color,width=width)
    def limb(a,b,width,color,light):
        line([a,b],INK,width+2);line([a,b],color,width)
        line([(a[0]-1,a[1]-1),(b[0]-1,b[1]-1)],light,2)
    # head, shoulder, hip, front knee, back knee, weapon hand, dropped blade tip.
    cx,cy,sx,sy,hx,hy,kx,ky,bkx,bky,wx,wy,tx,ty = [
        (25,49,30,63,35,76,22,82,43,82,41,71,57,79),
        (22,59,28,70,38,79,25,85,46,83,42,78,59,84),
        (20,69,28,77,42,82,33,86,49,84,43,83,60,86),
        (19,74,28,81,44,84,36,87,51,85,43,86,60,87),
    ][frame-2]
    skin,skinlight = ('#67794f','#9aa576') if goblin else ('#bf9a7c','#e0b994')
    cloth = '#52423a' if goblin else '#513641'
    armor = '#5d6647' if goblin else '#4b5d6b'
    light = '#939572' if goblin else '#8796a0'
    limb((hx-4,hy),(kx,ky),8,armor,light)
    limb((kx,ky),(kx-5,88),6,armor,light)
    limb((hx+1,hy),(bkx,bky),8,cloth,light)
    limb((bkx,bky),(min(bkx+5,58),88),6,cloth,light)
    for fx in [kx-5,min(bkx+5,58)]:
        p([(fx-3,85),(fx+3,85),(fx+6,89),(fx+6,90),(fx-4,90)],skin if goblin else '#292c35')
        line([(fx+1,88),(fx+4,89)],'#b9af86' if goblin else '#9b8669')
    # Split cloak/hide hangs off the spine; its settle changes independently.
    p([(sx-9,sy-10),(sx+5,sy-8),(hx+7,hy-2),(hx+2,hy+5),
       (hx-5,hy+3),(sx-12,sy+7)],'#3e373b' if goblin else '#222936')
    p([(sx-8,sy-9),(sx+6,sy-8),(hx+7,hy),(hx-4,hy+5),(sx-11,sy+5)],cloth)
    p([(sx-6,sy-8),(sx+5,sy-7),(hx+2,hy-1),(hx-5,hy+2),(sx-8,sy+1)],armor)
    line([(sx-7,sy-7),(sx-1,sy-5),(hx-4,hy)],light,2)
    line([(hx-5,hy-4),(hx+5,hy-2)],'#a1855e' if goblin else '#b39866',2)
    limb((sx+4,sy-5),(wx-6,wy-3),6,skin if goblin else armor,light)
    limb((wx-6,wy-3),(wx,wy),5,skin if goblin else '#383d4a',light)
    line([(wx,wy),(tx,ty)],INK,6 if goblin else 4)
    line([(wx,wy),(tx,ty)],'#767e80' if goblin else '#94a7b0',4 if goblin else 2)
    line([(wx+1,wy-1),(tx,ty-1)],'#b9b9a0' if goblin else '#d1d7c8')
    line([(wx-2,wy+2),(wx+2,wy-2)],'#a1855e',2)
    if goblin:
        p([(sx-9,sy-5),(sx-3,sy-4),(sx-1,sy+3),(sx-8,sy+6)],'#4e4039')
        for bx in [sx-3,sx+1,sx+5]:line([(bx,sy-4),(bx+1,sy)],'#c1b494',2)
        p([(cx-7,cy-4),(cx-14,cy-10),(cx-12,cy+1),(cx-5,cy+5)],'#77855c')
        p([(cx+5,cy-2),(cx+13,cy-6),(cx+9,cy+3)],'#687951')
        p([(cx-8,cy-8),(cx+3,cy-9),(cx+8,cy-3),(cx+12,cy+4),
           (cx+5,cy+9),(cx-5,cy+7),(cx-10,cy)],skin)
        p([(cx-6,cy-6),(cx+1,cy-7),(cx+5,cy-3),(cx,cy+1),(cx-6,cy)],skinlight,None)
        p([(cx-9,cy-4),(cx-7,cy-10),(cx-1,cy-12),(cx+5,cy-8),(cx+7,cy-4),
           (cx+1,cy-6),(cx-3,cy-4)],'#363e35')
        if not back:
            line([(cx,cy),(cx+5,cy-1)],'#343c32',2)
            line([(cx-5,cy-5),(cx-2,cy+1)],'#bbc096')
            line([(cx+4,cy+6),(cx+8,cy+5)],'#34362e',2)
            line([(cx+5,cy+5),(cx+5,cy+7)],'#d1c5a1')
    else:
        # Shield face remains legible beside the fallen steel-and-wine silhouette.
        p([(sx-13,sy-1),(sx-4,sy-2),(sx-2,sy+9),(sx-8,sy+13),(sx-16,sy+8)],'#788891')
        p([(sx-12,sy+1),(sx-6,sy),(sx-5,sy+7),(sx-9,sy+10),(sx-13,sy+6)],'#344554')
        line([(sx-10,sy+2),(sx-9,sy+7),(sx-7,sy+4)],'#c3a77c',2)
        p([(cx-6,cy-7),(cx+4,cy-8),(cx+10,cy-2),(cx+8,cy+5),(cx+2,cy+8),
           (cx-6,cy+4),(cx-9,cy-1)],skin)
        p([(cx-5,cy-5),(cx+3,cy-5),(cx+6,cy-1),(cx+2,cy+4),(cx-4,cy+2)],skinlight,None)
        p([(cx-9,cy+1),(cx-11,cy-6),(cx-7,cy-12),(cx,cy-14),(cx+7,cy-10),
           (cx+10,cy-5),(cx+8,cy-1),(cx+4,cy-4),(cx+1,cy),(cx-2,cy-4),
           (cx-5,cy+3)],'#554c4c')
        p([(cx-9,cy-6),(cx-6,cy-10),(cx,cy-12),(cx+6,cy-9),(cx+1,cy-7),
           (cx-2,cy-3),(cx-4,cy-6),(cx-7,cy-1)],'#b7a48a',None)
        line([(cx-7,cy-8),(cx-2,cy-11),(cx+1,cy-10)],'#e0caa4',2)
        if not back:
            line([(cx+2,cy),(cx+6,cy-1)],'#795e57')
            line([(cx+5,cy+4),(cx+7,cy+4)],'#795e57')
    if back:
        p([(sx-8,sy-6),(sx+4,sy-6),(hx+1,hy),(hx-5,hy+3),(sx-10,sy+3)],'#4e4039' if goblin else '#3e3443')
        line([(sx-7,sy-5),(sx-2,sy-2),(hx-5,hy)],'#937b58' if goblin else '#735363',2)
    if left: im=relight_left(im,goblin)
    return im

def relight_left(im,goblin):
    """Mirror pose, then reramp glints toward the fixed upper-left key light."""
    im=im.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    def rgba(hexcolor): return tuple(bytes.fromhex(hexcolor[1:]))+(255,)
    # Bright narrow accents lose their mirrored right-side gleam first.
    toned = {rgba(a):rgba(b) for a,b in [
        ('#c2c3b4','#8796a0'),('#c0c4b6','#8796a0'),('#acb5b2','#8796a0'),
        ('#d1d7c8','#94a7b0'),('#f2c79e','#e0b994'),
        ('#e0d0aa','#c1b494'),('#b9b9a0','#939b8c'),
    ]}
    # Explicit material ramps, only at authored-cluster edges facing the light.
    ramps = {rgba(a):rgba(b) for a,b in [
        ('#728591','#c2c3b4'),('#8796a0','#acb5b2'),('#596c79','#9ca9ab'),
        ('#94a7b0','#d1d7c8'),('#bf9a7c','#e0b994'),('#b7a48a','#e0caa4'),
        ('#67794f','#9aa576'),('#77855c','#a49a73'),('#72805a','#abb18a'),
        ('#767e80','#b9b9a0'),('#c1b494','#e0d0aa'),
    ]}
    source=im.copy();pixels=im.load();original=source.load()
    for y in range(H):
        for x in range(W):
            color=original[x,y]
            if color in toned: pixels[x,y]=toned[color]
            if color in ramps and x>0 and y>0:
                # A dark contour above/left separates one coherent material plane.
                adjacent=[original[x-1,y],original[x,y-1]]
                if any(c[3]==0 or c==rgba(INK) for c in adjacent):
                    pixels[x,y]=ramps[color]
    return im

def actor(goblin, clip, frame, back, left):
    if clip=='defeat' and frame>=2:
        return collapsed_actor(goblin,frame,back,left)
    im = Image.new('RGBA', (W, H))
    d = ImageDraw.Draw(im)
    pose = POSES[clip][frame]
    lean, breath, step = pose.x, pose.y, pose.stride
    cloth, hair = pose.cloth, pose.hair
    def p(points, color, outline=None):
        d.polygon(points, fill=color, outline=outline)
    def line(points, color, width=1): d.line(points, fill=color, width=width)
    def limb(a, b, width, dark, mid, light):
        line([a,b], dark, width+2); line([a,b], mid, width)
        line([(a[0]-2,a[1]),(b[0]-2,b[1]-1)],light,2)

    if not goblin:
        x, y = lean, breath
        # Back cloak, split hem and shadowed trailing arm.
        p([(21+x,28+y),(36+x,29+y),(44+x,48+y),(45+cloth,70+y),
           (36+cloth,66),(31+cloth,71),(18-step//2+cloth,67),(20+x,47+y)], '#222936', INK)
        p([(22+x,32+y),(27+x,34+y),(23+cloth,61),(20-step//2+cloth,65),
           (19+cloth,56)], '#493944')
        line([(26+x,39+y),(28+cloth,57),(25+cloth,65)], '#665060')
        # Articulated knees, shin plates and splayed leather boots.
        lifts=[pose.lift if clip=='walk' and frame<4 else 0,
               pose.lift if clip=='walk' and frame>=4 else 0]
        ankles=[(24-step,87-abs(step)//2-lifts[0]),(37+step,88-abs(step)//3-lifts[1])]
        limb((26+x,56+y),(25-step,74),8,INK,'#343d4c','#59616a')
        limb((25-step,74),ankles[0],7,INK,'#32323a','#626065')
        limb((35+x,56+y),(36+step,73),9,INK,'#424c59','#77818a')
        limb((36+step,73),ankles[1],7,INK,'#3a3c47','#737377')
        for leg,(bx, by) in enumerate(ankles):
            airborne = clip == 'walk' and leg == (0 if frame < 4 else 1)
            floor = 90-pose.lift if airborne else 90
            p([(bx-4,by-5),(bx+3,by-5),(bx+5,by),(bx+7,by+1),
               (bx+7,floor),(bx-5,floor),(bx-5,by-1)], '#292c35', INK)
            line([(bx-3,by-4),(bx+2,by-4)], '#9b8669', 2)
            line([(bx-3,floor-1),(bx+5,floor-1)], '#69615c')
        # Wine-red gambeson, shoulder slope and layered steel cuirass.
        p([(24+x,28+y),(35+x,28+y),(42+x,36+y),(39+x,49+y),
           (41+x,63+y),(33+x,66+y),(22+x,62+y),(24+x,48+y),
           (18+x,39+y)], '#513641', INK)
        p([(25+x,43+y),(30+x,44+y),(27+x,60+y),(23+x,62+y)], '#78505b')
        p([(32+x,48+y),(37+x,47+y),(39+x,61+y),(34+x,64+y)], '#332c39')
        line([(28+x,50+y),(27+x,60+y)], '#ad7c72')
        p([(23+x,31+y),(34+x,30+y),(39+x,35+y),(37+x,46+y),
           (29+x,51+y),(23+x,45+y),(20+x,36+y)], '#4b5d6b', INK)
        p([(23+x,32+y),(29+x,32+y),(31+x,43+y),(27+x,47+y),(23+x,43+y)], '#8796a0')
        p([(31+x,33+y),(36+x,34+y),(37+x,42+y),(32+x,46+y)], '#5e7380')
        line([(23+x,32+y),(28+x,31+y),(33+x,32+y)], '#c0c4b6')
        line([(30+x,36+y),(31+x,42+y),(29+x,46+y)], '#acb5b2')
        # Belt, brass buckle and stitched split tabard.
        p([(23+x,49+y),(38+x,48+y),(39+x,52+y),(23+x,54+y)], '#332c2e', INK)
        d.rectangle((29+x,49+y,33+x,53+y), fill='#b39866')
        d.rectangle((30+x,50+y,32+x,52+y), fill='#4c4542')
        line([(22+x,57+y),(25+x,58+y)], '#b08b6c')
        # Near pauldron and gauntlet; deliberately different action poses.
        shoulder=(37+x,35+y)
        elbow=(41+x,46+y); hand=(43+x,53+y)
        tip=(57+x,35+y)
        if clip in HERO_ARMS:
            elbow,hand,tip = HERO_ARMS[clip][frame]
        elif clip == 'walk':
            swing=[-2,-3,-1,1,3,2,0,-1][frame]
            elbow=(40+x,44+swing);hand=(41+x,52+swing);tip=(55+x,35+swing)
        elif clip in ('idle','calm'):
            settle = [0,-1,-2,-1,0,1][frame]
            elbow=(41+x,46+y);hand=(43+x,53+settle);tip=(57+x,35+settle)
        limb(shoulder,elbow,6,INK,'#596c79','#9ca9ab')
        limb(elbow,hand,5,INK,'#383d4a','#7c8790')
        p([(34+x,30+y),(40+x,32+y),(42+x,38+y),(37+x,40+y),(32+x,36+y)], '#728591', INK)
        line([(34+x,30+y),(39+x,32+y)], '#c2c3b4',2)
        # Sword has a slim pointed silhouette, dark fuller and a brass guard.
        hx,hy=hand;tx,ty=tip
        line([(hx,hy),(tx,ty)], INK,5)
        line([(hx,hy),(tx,ty)], '#94a7b0',3)
        line([(hx-1,hy-1),(tx,ty)], '#d1d7c8')
        line([(hx-4,hy-2),(hx+3,hy+3)], '#b7a077',2)
        d.rectangle((hx-2,hy-1,hx+1,hy+2),fill='#41414a')
        # Off-hand heater shield: rim, bevel, recessed enamel and original broken-star badge.
        sx,sy=x+pose.offhand//2,y+pose.offhand
        p([(17+sx,37+sy),(25+sx,39+sy),(24+sx,54+sy),(18+sx,60+sy),
           (12+sx,53+sy),(12+sx,41+sy)], '#788891', INK)
        p([(16+sx,40+sy),(22+sx,41+sy),(21+sx,52+sy),(18+sx,56+sy),(15+sx,51+sy)], '#344554')
        line([(14+sx,42+sy),(14+sx,51+sy),(18+sx,57+sy)], '#b7b6a3')
        p([(18+sx,44+sy),(19+sx,47+sy),(21+sx,48+sy),(18+sx,49+sy),
           (17+sx,52+sy),(16+sx,48+sy)], '#c3a77c')
        x += pose.head
        # Neck and face have separate shadow, plane, nose and brow clusters.
        p([(27+x,23+y),(35+x,23+y),(35+x,30+y),(31+x,33+y),(26+x,29+y)], '#9b7463', INK)
        if not back:
            p([(24+x,12+y),(35+x,11+y),(41+x,17+y),(40+x,24+y),
               (36+x,29+y),(29+x,28+y),(25+x,23+y)], '#bf9a7c', INK)
            p([(27+x,14+y),(34+x,14+y),(37+x,18+y),(35+x,24+y),(30+x,25+y),(27+x,21+y)], '#e0b994')
            brow = -1 if clip in ('alert','fear') else 0
            line([(34+x,19+y+brow),(38+x,19+y)], '#4b3e43')
            if pose.blink:
                line([(35+x,21+y),(38+x,21+y)], '#795e57')
            else:
                line([(35+x,20+y),(37+x,20+y)], '#aab8b5')
                d.point((37+x,20+y),fill='#273947')
                if clip == 'fear': d.point((36+x,21+y),fill='#e9d9ba')
            line([(39+x,21+y),(40+x,23+y),(38+x,23+y)], '#f2c79e')
            if clip == 'fear': line([(35+x,25+y),(37+x,26+y)], '#795e57')
            elif clip == 'victory': line([(34+x,25+y),(36+x,26+y),(38+x,25+y)], '#795e57')
            else: line([(35+x,26+y),(37+x,26+y)], '#795e57')
        # Ash-blond hair: interlocked locks, deep roots, three connected highlight planes.
        p([(22+x+hair,19+y),(21+x,12+y),(24+x,7+y),(31+x,5+y),(38+x,8+y),
           (41+x,13+y),(40+x+hair,19+y),(36+x,16+y),(34+x+hair,21+y),(32+x,16+y),
           (29+x+hair,20+y),(27+x,16+y),(25+x+hair,24+y),(23+x+hair,22+y)], '#554c4c', INK)
        p([(23+x,12+y),(26+x,8+y),(31+x,7+y),(36+x,9+y),(31+x,11+y),
           (29+x,16+y),(27+x,13+y),(24+x,18+y)], '#b7a48a')
        p([(31+x,11+y),(36+x,9+y),(39+x,13+y),(36+x,15+y),(34+x,18+y),(33+x,13+y)], '#8f806f')
        line([(25+x,10+y),(29+x,8+y),(32+x,8+y)], '#e0caa4',2)
        line([(24+x,13+y),(23+x,17+y)], '#d4bd98')
        line([(23+x+hair,21+y),(22+x+hair,25+y)], '#8f806f')
        if back:
            p([(23+x,17+y),(39+x,16+y),(38+x,25+y),(34+x,29+y),(25+x,26+y)],'#72675f')
            line([(24+x,19+y),(26+x,24+y),(29+x,25+y)],'#b7a48a',2)
            p([(24+lean,29+y),(35+lean,28+y),(39+lean,42+y),(36+cloth,61+y),
               (28+cloth,66+y),(20+cloth,62+y)],'#3e3443',INK)
            line([(24+lean,30+y),(27+lean,44+y),(24+cloth,58+y)],'#735363',2)
            line([(35+x,32+y),(34+x,47+y)],'#554251')
    else:
        x,y=lean,breath
        # Short, bent legs and bare clawed toes; asymmetric hunched silhouette.
        limb((27+x,62+y),(20-step,76),8,INK,'#424b38','#697250')
        lift0=pose.lift if clip=='walk' and frame<4 else 0
        lift1=pose.lift if clip=='walk' and frame>=4 else 0
        limb((20-step,76),(22-step,87-lift0),6,INK,'#5d6747','#939572')
        limb((37+x,60+y),(40+step,74),8,INK,'#343e33','#626e50')
        limb((40+step,74),(39+step,87-lift1),6,INK,'#4d5b42','#7d8961')
        for leg,bx in enumerate([22-step,39+step]):
            airborne = clip == 'walk' and leg == (0 if frame < 4 else 1)
            floor = 90-pose.lift if airborne else 90
            p([(bx-3,floor-6),(bx+2,floor-6),(bx+6,floor-2),(bx+6,floor),(bx-5,floor),(bx-5,floor-2)],'#667454',INK)
            line([(bx+2,floor-2),(bx+5,floor-2)],'#b9af86')
        p([(20+x,42+y),(30+x,35+y),(39+x,38+y),(45+x,49+y),(41+x,66+y),
           (32+x,70+y),(20+x,65+y),(16+x,53+y)],'#5d6647',INK)
        p([(21+x,43+y),(30+x,40+y),(35+x,44+y),(31+x,58+y),(20+x,58+y)],'#839064')
        p([(18+x,47+y),(23+x,45+y),(25+x,55+y),(38+x,60+y),(40+x,66+y),
           (32+x,68+y),(20+x,63+y)],'#52423a',INK)
        line([(20+x,48+y),(22+x,54+y),(31+x,59+y)],'#a68c65',2)
        # Leather is stitched in groups; bone trophies remain a readable pale mass.
        line([(23+x,48+y),(24+x,51+y)],'#c0a77b')
        line([(25+x,53+y),(27+x,55+y)],'#c0a77b')
        line([(28+x,45+y),(37+x,47+y)],'#3b3932')
        for bx,by in [(29+x,46+y),(33+x,47+y),(37+x,48+y)]:
            line([(bx,by),(bx-1,by+5)],'#4b493b',4)
            line([(bx,by),(bx-1,by+4)],'#c1b494',2)
            d.point((bx,by),fill='#e0d0aa')
        # Healed flank cuts and skin-fold planes, never random pixel noise.
        line([(37+x,53+y),(40+x,56+y)],'#434e39')
        line([(37+x,52+y),(40+x,55+y)],'#a6a87d')
        line([(26+x,67+y),(29+x,65+y)],'#727957')
        p([(23+x,60+y),(36+x,59+y),(40+x,67+y),(32+x,70+y),(27+x,68+y),
           (22+x,71+y),(20+x,67+y)],'#3e373b',INK)
        line([(24+x,62+y),(35+x,61+y)],'#a1855e',2)
        d.rectangle((29+x,61+y,32+x,64+y),fill='#b6a37e')
        # Long reaching forearm and chipped cleaver.
        shoulder=(40+x,46+y);elbow=(45+x,55+y);hand=(48+x,62+y);tip=(56+x,46+y)
        if clip in GOBLIN_ARMS:
            elbow,hand,tip=GOBLIN_ARMS[clip][frame]
        elif clip=='walk':
            swing=[-2,-3,-1,1,3,2,0,-1][frame]
            elbow=(44+x,53+swing);hand=(47+x,61+swing);tip=(56+x,44+swing)
        elif clip in ('idle','calm'):
            settle=[0,-1,-2,-1,1,2][frame]
            elbow=(45+x,55+y);hand=(48+x,62+settle);tip=(56+x,46+settle)
        limb(shoulder,elbow,7,INK,'#5c6b4d','#98a075')
        limb(elbow,hand,6,INK,'#72805a','#abb18a')
        hx,hy=hand;tx,ty=tip
        # Broad notched edge, dull iron body and a bright chipped cutting plane.
        length=hypot(tx-hx,ty-hy)
        vx,vy=(tx-hx)/length,(ty-hy)/length
        nx,ny=-vy,vx
        def blade_point(distance,side):
            return (round(hx+vx*distance+nx*side),round(hy+vy*distance+ny*side))
        p([blade_point(4,-2),blade_point(length,-2),blade_point(length,3),
           blade_point(length*.70,4),blade_point(length*.59,2),
           blade_point(length*.48,4),blade_point(5,3)],'#6c7675',INK)
        line([blade_point(5,-1),blade_point(length-1,-1)],'#b9b9a0')
        line([blade_point(7,2),blade_point(length*.48,3)],'#939b8c')
        line([(hx,hy),blade_point(5,0)],'#705c45',3)
        line([(hx-3,hy-1),(hx+3,hy+2)],'#78624a',3)
        sy=y+pose.offhand
        p([(17+x,45+sy),(23+x,46+sy),(24+x,53+sy),(18+x,59+sy),(14+x,55+sy)],'#45413c',INK)
        line([(16+x,48+sy),(18+x,51+sy)],'#9e8a68')
        line([(18+x,52+sy),(21+x,54+sy)],'#9e8a68')
        x += pose.head
        ear=[0,-1,-2,-1,1,0,1,0,0][frame]
        # Forward jutting jaw, one long torn ear and one smaller upright ear.
        p([(20+x,25+y),(14+x,18+y+ear),(13+x,25+y+ear),(16+x,26+y+ear),
           (13+x,29+y+ear),(21+x,35+y)],'#77855c',INK)
        p([(15+x,22+y+ear),(17+x,28+y),(20+x,30+y)],'#a49a73')
        p([(37+x,28+y),(48+x,24+y),(45+x,33+y),(38+x,37+y)],'#687951',INK)
        p([(41+x,29+y),(46+x,26+y),(43+x,31+y)],'#a29771')
        p([(22+x,24+y),(32+x,20+y),(39+x,24+y),(42+x,34+y),(46+x,40+y),
           (40+x,46+y),(30+x,46+y),(23+x,40+y),(20+x,32+y)],'#67794f',INK)
        p([(23+x,27+y),(31+x,24+y),(36+x,26+y),(37+x,33+y),(32+x,36+y),
           (25+x,34+y)],'#9aa576')
        p([(31+x,36+y),(40+x,34+y),(44+x,40+y),(38+x,42+y),(32+x,41+y)],'#85935f')
        brow=-1 if clip in ('alert','fear') else 0
        line([(29+x,32+y+brow),(34+x,31+y)],'#343c32',2)
        if pose.blink: line([(31+x,34+y),(34+x,33+y)],'#434e39')
        else:
            line([(31+x,33+y),(34+x,33+y)],'#d0b572')
            d.point((33+x,33+y),fill='#202c29')
            if clip=='fear': d.point((32+x,34+y),fill='#ded1a4')
        line([(38+x,30+y),(41+x,32+y)],'#36412f',2)
        line([(39+x,37+y),(42+x,39+y)],'#c1bd8b',2)
        line([(33+x,42+y),(41+x,41+y)],'#34362e',2)
        line([(35+x,42+y),(35+x,44+y)],'#d1c5a1')
        line([(40+x,41+y),(40+x,43+y)],'#c4ba96')
        # One old brow scar and two creases distinguish the raider's face.
        line([(26+x,28+y),(29+x,34+y)],'#4e5c42')
        line([(27+x,28+y),(30+x,34+y)],'#bbc096')
        line([(25+x,36+y),(28+x,38+y)],'#4e5c42')
        line([(26+x,37+y),(28+x,39+y)],'#9ca777')
        if clip=='fear': line([(35+x,41+y),(38+x,43+y)],'#2e392e')
        if clip=='victory': line([(32+x,40+y),(35+x,42+y),(41+x,40+y)],'#34362e',2)
        p([(21+x,26+y),(22+x,21+y),(28+x,18+y),(35+x,20+y),(39+x,25+y),
           (33+x,23+y),(29+x,25+y),(25+x,24+y)],'#363e35',INK)
        line([(23+x,22+y),(27+x,20+y),(30+x,21+y)],'#6d7356')
        if back:
            p([(22+x,27+y),(36+x,25+y),(40+x,35+y),(36+x,43+y),(26+x,41+y)],'#65754f',INK)
            line([(24+x,28+y),(27+x,37+y)],'#95a071',2)
            p([(22+x,43+y),(31+x,40+y),(37+x,45+y),(34+x,59+y),(25+x,64+y)],'#4e4039',INK)
            line([(24+x,45+y),(28+x,50+y),(27+x,59+y)],'#937b58',2)
    # Seated pose has bent lower legs, not a whole-sprite translation.
    if clip=='rest':
        im.paste((0,0,0,0),(0,70,W,H))
        dr=ImageDraw.Draw(im)
        dr.polygon([(21,65),(40,65),(45,72),(37,77),(25,74)],fill='#52423a' if goblin else '#3b4352',outline=INK)
        dr.line([(28,74),(30,88)],fill='#7d8961' if goblin else '#5f6975',width=6)
        dr.line([(40,74),(44,88)],fill='#5d6747' if goblin else '#404958',width=6)
        dr.line([(26,89),(34,89)],fill='#667454' if goblin else '#282d36',width=3)
        dr.line([(40,89),(49,89)],fill='#667454' if goblin else '#282d36',width=3)
    if left: im=relight_left(im,goblin)
    return im

def atlas(goblin):
    poses=[(clip,i) for clip,times in CLIPS.items() for i in range(len(times))]
    sheet=Image.new('RGBA',(W*len(poses),H*4))
    for row,(back,left) in enumerate([(False,False),(False,True),(True,False),(True,True)]):
        authored={clip:set() for clip in CLIPS}
        for col,(clip,i) in enumerate(poses):
            image=actor(goblin,clip,i,back,left)
            digest=hashlib.sha256(image.tobytes()).hexdigest()
            assert digest not in authored[clip],f'Duplicate {goblin=} {row=} {clip=} {i=}'
            authored[clip].add(digest)
            sheet.paste(image,(col*W,row*H))
    return sheet

def armory():
    im=Image.new('RGBA',(190,166));d=ImageDraw.Draw(im)
    def p(pts,c):d.polygon(pts,fill=c)
    # Same 190x166 canvas, base/door anchor and projected building envelope as the original.
    p([(20,69),(104,89),(177,74),(177,136),(105,153),(20,130)],'#202b31')
    p([(22,73),(104,94),(104,147),(22,126)],'#5c625d')
    p([(106,94),(175,78),(175,132),(106,149)],'#424e50')
    for row in range(7):
        y=79+row*7
        d.line([(23,y),(102,y+20)],fill='#333d3e',width=2)
        for col in range(5):
            x=25+col*17+(8 if row%2 else 0)
            if x>100:continue
            yy=round(y+(x-23)*20/79)
            d.line([(x,yy),(x,yy+5)],fill='#3a4545')
            d.line([(x+2,yy+2),(min(x+12,100),yy+4)],fill='#72756b')
    for row in range(7):
        y=94+row*7
        d.line([(107,y),(173,y-15)],fill='#29363c',width=2)
        for x in [116+(row%2)*10,140+(row%2)*10,164]:
            yy=round(y-(x-107)*15/66)
            d.line([(x,yy),(x,yy+5)],fill='#2b373c')
    # Heavy foundations follow the wall's two ground-plane axes.
    p([(20,121),(105,143),(178,126),(179,136),(105,154),(19,131)],'#303a3d')
    d.line([(21,122),(105,144),(176,128)],fill='#8a8b79',width=2)
    for x,y in [(35,132),(61,139),(91,147),(122,147),(157,138)]:
        d.line([(x,y-6),(x,y)],fill='#1e2b30',width=2)
    # Exposed timber posts with checks, upper-left edge highlights, dark eave occlusion.
    for x,y in [(22,74),(62,84),(103,94),(173,79)]:
        d.line([(x,y),(x,y+49)],fill='#2c2a29',width=6)
        d.line([(x-2,y),(x-2,y+45)],fill='#796a50',width=2)
        d.line([(x+1,y+18),(x,y+29)],fill='#171f26')
    p([(15,66),(105,87),(181,70),(178,80),(105,99),(17,77)],'#202a31')
    # Broad overlapping rust-slate roof courses, deliberate stepped row rhythm.
    roof=[(12,66),(54,17),(147,37),(105,89)]
    p(roof,'#4c423e')
    mask=Image.new('1',im.size);ImageDraw.Draw(mask).polygon(roof,fill=1)
    tiles=Image.new('RGBA',im.size);td=ImageDraw.Draw(tiles)
    colors=['#5d5148','#69584b','#625448','#715d4e']
    for r in range(8):
        for c in range(10):
            x=6+c*13+(6 if r%2 else 0);y=19+r*7+round(x*.22)
            td.polygon([(x,y),(x+11,y+2),(x+8,y+8),(x-3,y+6)],fill=colors[(c+r*2)%4])
            td.line([(x,y),(x+10,y+2)],fill='#8d7860')
            td.line([(x-3,y+6),(x+8,y+8)],fill='#342f32',width=2)
    tiles.putalpha(ImageChops.multiply(tiles.getchannel('A'),mask.convert('L')))
    im.alpha_composite(tiles)
    d=ImageDraw.Draw(im)
    # Gabled front, layered bargeboards and ridge thickness.
    p([(105,89),(147,37),(182,71)],'#303d43')
    for y in [54,62,70,78]:d.line([(132,y),(166,y+3)],fill='#59605c')
    d.line([(11,67),(54,17),(147,37),(183,71),(105,90),(12,68)],fill='#222a30',width=5)
    d.line([(13,64),(54,18),(146,38)],fill='#b49b79',width=2)
    d.line([(147,39),(179,70)],fill='#8d7f6d',width=2)
    d.line([(105,89),(146,39)],fill='#716455',width=3)
    # Copper chimney, bevelled cap, dark seam wear.
    p([(45,18),(56,20),(56,44),(45,41)],'#805847')
    p([(56,20),(62,17),(62,40),(56,44)],'#593e38')
    for y in [23,29,35]:d.line([(45,y),(56,y+2)],fill='#a6795a')
    d.rectangle((43,14,60,19),fill='#363a39')
    d.line([(44,14),(60,14)],fill='#b1a187',width=2)
    # Recessed windows, muntins and tiny warm cores.
    for x,y in [(40,90),(79,101),(119,101),(156,91),(145,61)]:
        d.rectangle((x-6,y-7,x+6,y+10),fill='#1b2930')
        d.rectangle((x-5,y-6,x+4,y+7),fill='#a4875d')
        d.rectangle((x-3,y-4,x+3,y+6),fill='#c29559')
        d.rectangle((x-2,y-4,x+1,y+1),fill='#f2d38b')
        d.line([(x,y-4),(x,y+7)],fill='#625442')
        d.line([(x-4,y+2),(x+4,y+2)],fill='#625442')
        d.line([(x-6,y+10),(x+6,y+10)],fill='#8e8873',width=2)
    # Tall inset door and three foreshortened steps; same front-right entrance.
    p([(133,110),(151,106),(151,138),(133,142)],'#1b272e')
    d.line([(131,142),(131,110),(152,105),(153,137)],fill='#8c8067',width=3)
    p([(135,112),(149,109),(149,136),(135,139)],'#51443a')
    for x in [137,141,145]:d.line([(x,113),(x,137)],fill='#8b7050')
    d.line([(135,120),(149,117)],fill='#293235',width=2)
    d.point((146,127),fill='#d2b576')
    for i in range(3):
        p([(130-i*2,142+i*2),(155+i*2,136+i*2),(157+i*2,140+i*2),(129-i*2,146+i*2)],'#5f6965')
        d.line([(130-i*2,142+i*2),(155+i*2,136+i*2)],fill='#a8a58b')
    # Forged sword sign, wall brackets, moss in foundation joints; no new collider.
    d.line([(105,99),(114,97),(114,117)],fill='#1b2931',width=2)
    p([(108,103),(119,101),(120,116),(109,118)],'#3a3540')
    d.line([(114,105),(114,114)],fill='#b8bfad',width=2)
    d.line([(111,108),(117,108)],fill='#a88c61')
    for x,y in [(24,129),(49,136),(89,146),(110,151),(161,138)]:
        p([(x,y),(x+2,y-3),(x+5,y-2),(x+7,y),(x+4,y+1)],'#526342')
        d.line([(x+1,y-1),(x+3,y-2)],fill='#849062')
    return im

def verge():
    im=Image.new('RGBA',(128,96));d=ImageDraw.Draw(im)
    # Intentional groups beside (not across) the existing smithy lane.
    for x,y,s in [(17,30,1),(25,37,1),(11,45,2),(22,50,1),(99,26,2),(110,33,1),
                  (105,41,1),(108,70,2),(96,76,1),(21,76,1),(30,82,1)]:
        d.polygon([(x-6,y),(x-2,y-3),(x+6,y-1),(x+8,y+3),(x+2,y+5),(x-7,y+3)],fill='#2e3e32')
        d.polygon([(x-4,y+1),(x-6,y-5),(x-2,y-3),(x,y),(x+2,y-8-s),(x+4,y-4),
                   (x+4,y+1),(x+9,y-3),(x+7,y+3),(x,y+3)],fill='#576b46')
        d.line([(x+2,y-7-s),(x+2,y-3),(x+1,y)],fill='#8d9562')
        d.line([(x-5,y-4),(x-3,y-2)],fill='#79875a')
        if x>90:d.polygon([(x+3,y+1),(x+4,y-2),(x+6,y-1),(x+6,y+1)],fill='#8f8092')
    for x,y in [(26,59),(96,52),(18,65),(108,60),(86,16)]:
        d.polygon([(x-5,y),(x-2,y-3),(x+4,y-2),(x+6,y+1),(x+3,y+3),(x-5,y+2)],fill='#303c3c')
        d.polygon([(x-4,y),(x-1,y-2),(x+4,y-1),(x+2,y+1)],fill='#72786a')
        d.line([(x-3,y-1),(x-1,y-2),(x+2,y-2)],fill='#a0a08a')
    # A few embedded worn slabs along the diagonal existing path, with broken edges.
    for x,y in [(42,69),(57,57),(70,45),(83,34)]:
        d.polygon([(x-5,y),(x+1,y-3),(x+5,y-1),(x+4,y+2),(x-1,y+4)],fill='#333b3d')
        d.line([(x-4,y),(x+1,y-2),(x+4,y-1)],fill='#9c9b83')
    return im

assets = {}
assert abs(sum(CLIPS['attack'])-.48)<1e-8 and abs(sum(CLIPS['attack'][:4])-.22)<1e-8
assert abs(sum(CLIPS['skill'])-.72)<1e-8 and abs(sum(CLIPS['skill'][:4])-.30)<1e-8
assert all(len(POSES[key])==len(times) for key,times in CLIPS.items())
for name,im in [('kaelen',atlas(False)),('goblin_raider',atlas(True)),('armory',armory()),('smithy_verge',verge())]:
    path=OUT/(name+'.png');im.save(path,optimize=True)
    assets[name]={'file':path.name,'size':list(im.size),'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}
metadata={'version':2,'origin':'Original ETERNAL SUMMONER art; authored polygon/cluster recipes in tools/generate_premium_slice.py; no external art',
          'frame':[W,H],'pivot':[32,90],'world_pixel_scale':1.08,
          'directions':['front_right','front_left','back_right','back_left'],
          'clip_order':list(CLIPS),'clips':CLIPS,
          'loop_clips':['idle','walk','calm','alert','fear','victory'],
          'contact_seconds':{'attack':.22,'skill':.30},'contact_frames':{'attack':4,'skill':4},
          'emotion_clips':{'calm':'calm','alert':'alert','fear':'fear','victory':'victory'},
          'lighting':'upper-left; left-facing poses reramp material glints after mirroring',
          'assets':assets,'sample':{'armory_anchor':[995,655],'verge_top_left':[950,720],'verge_world_size':[256,192]}}
(OUT/'manifest.json').write_text(json.dumps(metadata,indent=2)+'\n')
print('Wrote four original PNGs and animation metadata to',OUT)
