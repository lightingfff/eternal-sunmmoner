#!/usr/bin/env python3
"""Original, deliberately clustered P1.5A pixel art. Pillow is an offline authoring tool.
No downloaded/reference-game art, noise textures, runtime generation or blur.
Run: python3 tools/generate_premium_slice.py
"""
from pathlib import Path
import hashlib
import json
from PIL import Image, ImageDraw, ImageChops

OUT = Path(__file__).resolve().parents[1] / 'app/src/main/assets/premium_slice'
OUT.mkdir(parents=True, exist_ok=True)
W, H = 64, 96
# Seconds, not render ticks. The active pose starts at the gameplay contact marker.
CLIPS = {
    'idle': [.36, .18, .36], 'walk': [.10, .08, .12, .10, .08, .12],
    'attack': [.12, .10, .08, .18], 'skill': [.16, .14, .09, .13, .20],
    'hit': [.11, .17], 'rest': [1.0], 'defeat': [.12, .18, .30],
}
INK = '#18222d'

def actor(goblin, clip, frame, back, left):
    im = Image.new('RGBA', (W, H))
    d = ImageDraw.Draw(im)
    # Only upper-body secondary motion; the shared foot pivot stays (32,90).
    breath = 1 if clip == 'idle' and frame == 1 else 0
    lean = [-2, -3, 4, 1][frame] if clip == 'attack' else 0
    if clip == 'skill': lean = [-1, -2, 5, 3, 0][frame]
    if clip == 'hit': lean = -4 + frame * 2
    step = [-4, -2, 1, 4, 2, -1][frame] if clip == 'walk' else 0
    if clip == 'defeat': lean = [-6, -10, -14][frame]; breath = [8, 19, 30][frame]
    def p(points, color, outline=None):
        d.polygon(points, fill=color, outline=outline)
    def line(points, color, width=1): d.line(points, fill=color, width=width)
    def limb(a, b, width, dark, mid, light):
        line([a,b], dark, width+2); line([a,b], mid, width)
        line([(a[0]-2,a[1]),(b[0]-2,b[1]-1)],light,2)

    if not goblin:
        x, y = lean, breath
        # Back cloak, split hem and shadowed trailing arm.
        p([(21+x,28+y),(36+x,29+y),(44+x,48+y),(45+x,70+y),
           (36,66),(31,71),(18-step//2,67),(20,47)], '#222936', INK)
        p([(22+x,32+y),(27+x,34+y),(23,61),(20-step//2,65),(19,56)], '#493944')
        # Articulated knees, shin plates and splayed leather boots.
        limb((26+x,56+y),(25-step,74),8,INK,'#343d4c','#59616a')
        limb((25-step,74),(24-step,87-abs(step)//2),7,INK,'#32323a','#626065')
        limb((35+x,56+y),(36+step,73),9,INK,'#424c59','#77818a')
        limb((36+step,73),(37+step,88-abs(step)//3),7,INK,'#3a3c47','#737377')
        for leg,(bx, by) in enumerate([(24-step,87-abs(step)//2),(37+step,88-abs(step)//3)]):
            floor = 87 if clip == 'walk' and ((step>0) == (leg==0)) else 90
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
        if clip == 'attack':
            elbow,hand,tip = [((36,36),(29,30),(21,9)), ((34,37),(27,29),(16,6)),
                              ((44,41),(49,42),(63,46)), ((42,44),(44,49),(57,57))][frame]
        elif clip == 'skill':
            elbow,hand,tip = [((36,39),(34,33),(37,10)),((35,37),(34,30),(37,5)),
                              ((45,38),(49,37),(63,33)),((44,43),(48,46),(63,53)),
                              ((41,46),(43,52),(56,36))][frame]
        elif clip == 'walk': elbow=(40+x,44-step//2);hand=(41+x,52-step//2);tip=(55+x,35-step//2)
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
        p([(17+x,37+y),(25+x,39+y),(24+x,54+y),(18+x,60+y),(12+x,53+y),(12+x,41+y)], '#788891', INK)
        p([(16+x,40+y),(22+x,41+y),(21+x,52+y),(18+x,56+y),(15+x,51+y)], '#344554')
        line([(14+x,42+y),(14+x,51+y),(18+x,57+y)], '#b7b6a3')
        p([(18+x,44+y),(19+x,47+y),(21+x,48+y),(18+x,49+y),(17+x,52+y),(16+x,48+y)], '#c3a77c')
        # Neck and face have separate shadow, plane, nose and brow clusters.
        p([(27+x,23+y),(35+x,23+y),(35+x,30+y),(31+x,33+y),(26+x,29+y)], '#9b7463', INK)
        if not back:
            p([(24+x,12+y),(35+x,11+y),(41+x,17+y),(40+x,24+y),
               (36+x,29+y),(29+x,28+y),(25+x,23+y)], '#bf9a7c', INK)
            p([(27+x,14+y),(34+x,14+y),(37+x,18+y),(35+x,24+y),(30+x,25+y),(27+x,21+y)], '#e0b994')
            line([(34+x,19+y),(38+x,19+y)], '#4b3e43')
            line([(35+x,20+y),(37+x,20+y)], '#aab8b5')
            d.point((37+x,20+y),fill='#273947')
            line([(39+x,21+y),(40+x,23+y),(38+x,23+y)], '#f2c79e')
            line([(35+x,26+y),(37+x,26+y)], '#795e57')
        # Ash-blond hair: interlocked locks, deep roots, three connected highlight planes.
        p([(22+x,19+y),(21+x,12+y),(24+x,7+y),(31+x,5+y),(38+x,8+y),
           (41+x,13+y),(40+x,19+y),(36+x,16+y),(34+x,21+y),(32+x,16+y),
           (29+x,20+y),(27+x,16+y),(25+x,24+y),(23+x,22+y)], '#554c4c', INK)
        p([(23+x,12+y),(26+x,8+y),(31+x,7+y),(36+x,9+y),(31+x,11+y),
           (29+x,16+y),(27+x,13+y),(24+x,18+y)], '#b7a48a')
        p([(31+x,11+y),(36+x,9+y),(39+x,13+y),(36+x,15+y),(34+x,18+y),(33+x,13+y)], '#8f806f')
        line([(25+x,10+y),(29+x,8+y),(32+x,8+y)], '#e0caa4',2)
        line([(24+x,13+y),(23+x,17+y)], '#d4bd98')
        if back:
            p([(23+x,17+y),(39+x,16+y),(38+x,25+y),(34+x,29+y),(25+x,26+y)],'#72675f')
            line([(24+x,19+y),(26+x,24+y),(29+x,25+y)],'#b7a48a',2)
            p([(24+x,29+y),(35+x,28+y),(39+x,42+y),(36+x,61+y),(28+x,66+y),
               (20+x,62+y)],'#3e3443',INK)
            line([(24+x,30+y),(27+x,44+y),(24+x,58+y)],'#735363',2)
            line([(35+x,32+y),(34+x,47+y)],'#554251')
    else:
        x,y=lean,breath
        # Short, bent legs and bare clawed toes; asymmetric hunched silhouette.
        limb((27+x,62+y),(20-step,76),8,INK,'#424b38','#697250')
        limb((20-step,76),(22-step,87),6,INK,'#5d6747','#939572')
        limb((37+x,60+y),(40+step,74),8,INK,'#343e33','#626e50')
        limb((40+step,74),(39+step,87),6,INK,'#4d5b42','#7d8961')
        for leg,bx in enumerate([22-step,39+step]):
            floor = 87 if clip == 'walk' and ((step>0) == (leg==0)) else 90
            p([(bx-3,floor-6),(bx+2,floor-6),(bx+6,floor-2),(bx+6,floor),(bx-5,floor),(bx-5,floor-2)],'#667454',INK)
            line([(bx+2,floor-2),(bx+5,floor-2)],'#b9af86')
        p([(20+x,42+y),(30+x,35+y),(39+x,38+y),(45+x,49+y),(41+x,66+y),
           (32+x,70+y),(20+x,65+y),(16+x,53+y)],'#5d6647',INK)
        p([(21+x,43+y),(30+x,40+y),(35+x,44+y),(31+x,58+y),(20+x,58+y)],'#839064')
        p([(18+x,47+y),(23+x,45+y),(25+x,55+y),(38+x,60+y),(40+x,66+y),
           (32+x,68+y),(20+x,63+y)],'#52423a',INK)
        line([(20+x,48+y),(22+x,54+y),(31+x,59+y)],'#a68c65',2)
        p([(23+x,60+y),(36+x,59+y),(40+x,67+y),(32+x,70+y),(27+x,68+y),
           (22+x,71+y),(20+x,67+y)],'#3e373b',INK)
        line([(24+x,62+y),(35+x,61+y)],'#a1855e',2)
        d.rectangle((29+x,61+y,32+x,64+y),fill='#b6a37e')
        # Long reaching forearm and chipped cleaver.
        shoulder=(40+x,46+y);elbow=(45+x,55+y);hand=(48+x,62+y);tip=(56+x,46+y)
        if clip=='attack':
            elbow,hand,tip=[((39,44),(36,38),(34,22)),((38,43),(35,34),(29,17)),
                            ((46,48),(50,49),(62,48)),((45,55),(47,60),(59,66))][frame]
        elif clip=='walk': elbow=(44,53-step//2);hand=(47,61-step//2);tip=(56,44-step//2)
        limb(shoulder,elbow,7,INK,'#5c6b4d','#98a075')
        limb(elbow,hand,6,INK,'#72805a','#abb18a')
        hx,hy=hand;tx,ty=tip
        line([(hx,hy),(tx,ty)],INK,6);line([(hx,hy),(tx,ty)],'#767e80',4)
        line([(hx-2,hy-2),(tx-1,ty)],'#b9b9a0')
        line([(hx-3,hy-1),(hx+3,hy+2)],'#78624a',3)
        p([(17+x,45+y),(23+x,46+y),(24+x,53+y),(18+x,59+y),(14+x,55+y)],'#45413c',INK)
        line([(16+x,48+y),(18+x,51+y)],'#9e8a68')
        # Forward jutting jaw, one long torn ear and one smaller upright ear.
        p([(20+x,25+y),(14+x,18+y),(13+x,29+y),(21+x,35+y)],'#77855c',INK)
        p([(15+x,22+y),(17+x,28+y),(20+x,30+y)],'#a49a73')
        p([(37+x,28+y),(48+x,24+y),(45+x,33+y),(38+x,37+y)],'#687951',INK)
        p([(41+x,29+y),(46+x,26+y),(43+x,31+y)],'#a29771')
        p([(22+x,24+y),(32+x,20+y),(39+x,24+y),(42+x,34+y),(46+x,40+y),
           (40+x,46+y),(30+x,46+y),(23+x,40+y),(20+x,32+y)],'#67794f',INK)
        p([(23+x,27+y),(31+x,24+y),(36+x,26+y),(37+x,33+y),(32+x,36+y),
           (25+x,34+y)],'#9aa576')
        p([(31+x,36+y),(40+x,34+y),(44+x,40+y),(38+x,42+y),(32+x,41+y)],'#85935f')
        line([(29+x,32+y),(34+x,31+y)],'#343c32',2)
        line([(31+x,33+y),(34+x,33+y)],'#d0b572')
        d.point((33+x,33+y),fill='#202c29')
        line([(38+x,30+y),(41+x,32+y)],'#36412f',2)
        line([(39+x,37+y),(42+x,39+y)],'#c1bd8b',2)
        line([(33+x,42+y),(41+x,41+y)],'#34362e',2)
        line([(35+x,42+y),(35+x,44+y)],'#d1c5a1')
        line([(40+x,41+y),(40+x,43+y)],'#c4ba96')
        p([(21+x,26+y),(22+x,21+y),(28+x,18+y),(35+x,20+y),(39+x,25+y),
           (33+x,23+y),(29+x,25+y),(25+x,24+y)],'#363e35',INK)
        line([(23+x,22+y),(27+x,20+y),(30+x,21+y)],'#6d7356')
        if back:
            p([(22+x,27+y),(36+x,25+y),(40+x,35+y),(36+x,43+y),(26+x,41+y)],'#65754f',INK)
            line([(24+x,28+y),(27+x,37+y)],'#95a071',2)
            p([(22+x,43+y),(31+x,40+y),(37+x,45+y),(34+x,59+y),(25+x,64+y)],'#4e4039',INK)
            line([(24+x,45+y),(28+x,50+y),(27+x,59+y)],'#937b58',2)
    if left: im=im.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    # Seated pose has bent lower legs, not a whole-sprite translation.
    if clip=='rest':
        im.paste((0,0,0,0),(0,70,W,H))
        dr=ImageDraw.Draw(im)
        dr.polygon([(21,65),(40,65),(45,72),(37,77),(25,74)],fill='#3b4352',outline=INK)
        dr.line([(28,74),(30,89)],fill='#5f6975',width=6)
        dr.line([(40,74),(44,88)],fill='#404958',width=6)
        dr.line([(26,90),(34,90)],fill='#282d36',width=3)
        dr.line([(40,90),(49,90)],fill='#282d36',width=3)
    return im

def atlas(goblin):
    poses=[(clip,i) for clip,times in CLIPS.items() for i in range(len(times))]
    sheet=Image.new('RGBA',(W*len(poses),H*4))
    for row,(back,left) in enumerate([(False,False),(False,True),(True,False),(True,True)]):
        for col,(clip,i) in enumerate(poses):sheet.paste(actor(goblin,clip,i,back,left),(col*W,row*H))
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
for name,im in [('kaelen',atlas(False)),('goblin_raider',atlas(True)),('armory',armory()),('smithy_verge',verge())]:
    path=OUT/(name+'.png');im.save(path,optimize=True)
    assets[name]={'file':path.name,'size':list(im.size),'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}
metadata={'version':1,'origin':'Original ETERNAL SUMMONER art; authored polygon/cluster recipes in tools/generate_premium_slice.py; no external art',
          'frame':[W,H],'pivot':[32,90],'world_pixel_scale':1.08,
          'directions':['front_right','front_left','back_right','back_left'],
          'clips':CLIPS,'contact_seconds':{'attack':.22,'skill':.30},
          'assets':assets,'sample':{'armory_anchor':[995,655],'verge_top_left':[950,720],'verge_world_size':[256,192]}}
(OUT/'manifest.json').write_text(json.dumps(metadata,indent=2)+'\n')
print('Wrote four original PNGs and animation metadata to',OUT)
