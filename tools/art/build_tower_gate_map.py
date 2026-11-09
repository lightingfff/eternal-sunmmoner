"""Original, deterministic pixel architecture. Pillow is authoring-only, never runtime.
960x540 oblique scene: ground depth 0.72; upright masonry retains its height.
No external/source image is sampled. Portals are separate animated runtime layers.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import math, random, json, hashlib
OUT = Path(__file__).resolve().parents[2] / 'app/src/main/assets/tower_gate_map'
W,H=960,540
rng=random.Random(8341)
im=None; d=None
STONE=['#526879','#607989','#6e8592','#7d9198','#4b6073']
GOLD='#cda56a'; LIGHT='#e1cf9c'; DARK='#182b40'
def poly(p,c): d.polygon([(int(x),int(y)) for x,y in p],fill=c)
def rect(box,c): d.rectangle(tuple(int(v) for v in box),fill=c)
def line(p,c,w=1): d.line([(int(x),int(y)) for x,y in p],fill=c,width=w)
def masonry(x,y,w,h,unit=14):
    rect((x,y,x+w,y+h),DARK)
    for row,yy in enumerate(range(int(y),int(y+h),unit)):
        for xx in range(int(x)-unit+ (row%2)*unit,int(x+w),unit*2):
            x0=max(x,xx);x1=min(x+w,xx+unit*2-1);y1=min(y+h,yy+unit-1)
            if x1<=x0:continue
            c=rng.choice(STONE);rect((x0,yy,x1,y1),c)
            line([(x0,yy),(x1,yy)],'#8c9d9f');line([(x0,y1),(x1,y1)],'#354b60')
            for j in range(3):
                px=rng.uniform(x0,x1);py=rng.uniform(yy,y1);rect((px,py,px+1,py+1),rng.choice([c,'#4b6170','#819393']))
def diamond(x,y,r,c):poly([(x,y-r),(x+r*.6,y),(x,y+r),(x-r*.6,y)],c)
def bush(x,y,s=1):
    for _ in range(44):
        bx=x+rng.gauss(0,13)*s;by=y+rng.gauss(0,5)*s;r=rng.randint(3,8)*s
        d.ellipse((int(bx-r),int(by-r*.6),int(bx+r),int(by+r*.6)),fill=rng.choice(['#172f39','#244741','#356052','#477460','#67836a']))
    for _ in range(8):
        bx=x+rng.gauss(0,12)*s;by=y+rng.gauss(0,4)*s
        rect((bx,by,bx+2,by+1),rng.choice(['#abb6c6','#8191c2','#bba5c6']))
def pine(x,y,s=1):
    line([(x,y),(x-5*s,y-110*s)],'#26343d',int(5*s))
    for k in range(10):
        yy=y-105*s+k*9*s;ww=(9+k*3)*s
        poly([(x-2*s,yy-25*s),(x-ww,yy+10*s),(x-ww*.35,yy+7*s),(x-ww*.7,yy+20*s),(x+ww,yy+15*s)],['#182f3e','#1e3d46','#274f4c'][k%3])
        line([(x-3*s,yy-15*s),(x-ww*.75,yy+8*s)],'#3a605a',max(1,int(s)))
def archpath(cx,base,r,h):
    spring=base-h+r
    return [(cx-r,base),(cx-r,spring)]+[(cx+r*math.cos(a),spring+r*math.sin(a)) for a in [math.pi+i*math.pi/32 for i in range(33)]]+[(cx+r,base)]
def spire(x,base,s):
    masonry(x-10*s,base-95*s,20*s,95*s,max(4,int(8*s)))
    poly([(x+10*s,base),(x+19*s,base-7*s),(x+19*s,base-103*s),(x+10*s,base-95*s)],'#263f54')
    for yy in [base-91*s,base-40*s,base-5*s]:
        rect((x-14*s,yy,x+14*s,yy+4*s),'#9aa5a0');line([(x-14*s,yy),(x+14*s,yy)],LIGHT)
    poly([(x-17*s,base-96*s),(x,base-144*s),(x+18*s,base-96*s)],'#304f60')
    poly([(x,base-144*s),(x+18*s,base-96*s),(x+5*s,base-100*s)],'#192d44')
    line([(x,base-144*s),(x-17*s,base-96*s)],GOLD,max(1,int(s)))
    diamond(x,base-147*s,5*s,LIGHT)
    for off in [-5,3]:line([(x+off*s,base-87*s),(x+off*s,base-51*s)],'#b3af97',max(1,int(s)))
def gate(cx,base,r,h):
    # Broad staircase: visible treads + risers, never a flat rectangle silhouette.
    for k in range(7,-1,-1):
        yy=base+k*6;ww=r+42+k*7
        poly([(cx-ww,yy),(cx+ww,yy),(cx+ww+10,yy+6),(cx-ww-10,yy+6)],'#263d52')
        line([(cx-ww,yy),(cx+ww,yy)],'#a7aaa0',2)
        for xx in range(int(cx-ww+10),int(cx+ww),29):line([(xx,yy),(xx+3,yy+4)],'#506477')
    # Back/side plane offset makes the upright architecture read in three dimensions.
    outer=archpath(cx+13,base-9,r+24,h+31)
    poly(outer,'#273d50')
    for side in [-1,1]:
        x=cx+side*(r+20)
        masonry(x-14,base-h+r,28,h-r,10)
        for o in [-8,0,8]:line([(x+o,base-h+r+5),(x+o,base-8)],'#93a3a0' if o<0 else '#3b5364',2)
        for yy in [base-h+r,base-26,base-6]:
            rect((x-19,yy,x+19,yy+5),'#89968f');line([(x-19,yy),(x+19,yy)],LIGHT)
        spire(cx+side*(r+54),base+3,h/195)
    spring=base-h+r
    # Individually cut radial voussoirs and three inset carved archivolts.
    for k in range(19):
        a=math.pi+k*math.pi/19;b=math.pi+(k+1)*math.pi/19
        points=[(cx+rad*math.cos(t),spring+rad*math.sin(t)) for rad,t in [(r+28,a+.012),(r+28,b-.012),(r+2,b-.012),(r+2,a+.012)]]
        poly(points,rng.choice(STONE));line(points+[points[0]],'#263e53')
        line(points[:2],LIGHT)
        mid=(a+b)/2;diamond(cx+(r+16)*math.cos(mid),spring+(r+16)*math.sin(mid),2,GOLD)
    for rad,col in [(r+5,GOLD),(r+1,'#091b30'),(r+32,'#8d9c9c')]:
        line([(cx+rad*math.cos(math.pi+i*math.pi/64),spring+rad*math.sin(math.pi+i*math.pi/64)) for i in range(65)],col,2)
    # Recessed aperture. Same path is used by the runtime glow/door masks.
    poly(archpath(cx,base,r,h),'#081322')
    for side in [-1,1]:line([(cx+side*r,base),(cx+side*r,spring)],GOLD,2)
    crown=base-h-41
    diamond(cx,crown,19,'#203d50');diamond(cx,crown,13,GOLD);diamond(cx,crown,8,'#b9e9d8')
    for side in [-1,1]:
        line([(cx+side*17,crown+4),(cx+side*43,crown+15),(cx+side*65,crown+7)],GOLD,2)
        for j in range(5):line([(cx+side*(24+j*7),crown+7+j),(cx+side*(29+j*7),crown-4+j)],'#81998f',2)
        bush(cx+side*(r+44),base+35,.75)
def background(nexus):
    global im,d
    im=Image.new('RGB',(W,H));d=ImageDraw.Draw(im)
    for y in range(H):
        t=y/H;rect((0,y,W,y),(int(9+19*t),int(20+27*t),int(36+30*t)))
    for _ in range(160):
        x=rng.randrange(W);y=rng.randrange(220)
        rect((x,y,x+1,y+1),rng.choice(['#647e96','#3e5878','#9cb7bb']))
    d.ellipse((763,20,810,67),fill='#9eadae');d.ellipse((773,17,815,58),fill='#16293f')
    for layer in range(3):
        points=[(0,270)]+[(x,155+layer*30+rng.randint(-45,45)) for x in range(0,W+40,40)]+[(W,320),(0,320)]
        poly(points,['#20354b','#263f52','#304b5c'][layer])
    for x in range(-15,980,57):pine(x,305,rng.uniform(.8,1.4))
    # Raised, chamfered stone terrace, flagstones following the oblique ground plane.
    poly([(75,260),(885,260),(960,431),(854,537),(106,537),(0,431)],'#132637')
    poly([(75,247),(885,247),(960,411),(854,510),(106,510),(0,411)],'#7b8985')
    poly([(83,252),(877,252),(950,409),(850,503),(110,503),(10,409)],'#334b5b')
    for row,y in enumerate(range(255,510,12)):
        inset=max(15,int(abs(y-385)*.48))
        for x in range(-20+row%2*15,W,30):
            if x<inset or x+30>W-inset:continue
            col=rng.choice(['#405665','#4b626e','#536a73','#5a7079','#465d6c'])
            poly([(x,y),(x+28,y),(x+31,y+10),(x+3,y+10)],col)
            line([(x,y),(x+28,y)],'#71828a')
            if rng.random()<.27:line([(x+6,y+4),(x+10,y+7),(x+18,y+7)],'#2c4355')
    # Radial brass inlay on arrival plaza, real stone surrounds it.
    for r in [57,72,134]:
        d.ellipse((480-r,417-r*.72,480+r,417+r*.72),outline='#8e8b71',width=2)
    for j in range(24):
        a=j*math.pi/12
        line([(480+125*math.cos(a),417+90*math.sin(a)),(480+133*math.cos(a),417+96*math.sin(a))],GOLD,2)
    for x in [42,108,852,918]:
        for y in [330,447]:
            bush(x,y,1.35)
    if not nexus:
        # Receding wings: patterned masonry, buttresses, slit windows and roof planes.
        for x in [130,605]:
            masonry(x,155,225,136,12)
            poly([(x-12,155),(x+110,104),(x+240,155),(x+225,169),(x,169)],'#263f54')
            for yy in range(130,161,6):line([(x+max(0,(155-yy)*2),yy),(x+225-max(0,(155-yy)*2),yy)],'#4f6c76')
            for wx in range(x+25,x+225,44):
                poly(archpath(wx,257,8,58),'#0d2234');line([(wx,208),(wx,248)],'#789d9a',2)
                rect((wx-12,263,wx+12,268),'#a3a89a')
            spire(x+5,295,1.15);spire(x+223,295,1.15)
        # Crimson procession carpet shares the central stair axis.
        poly([(452,376),(508,376),(530,526),(430,526)],'#402739')
        for off in [0,5]:
            line([(452+off,376),(430+off,526)],GOLD,2);line([(508-off,376),(530-off,526)],GOLD,2)
        for yy in range(391,525,22):diamond(480,yy,7,'#a17e5b')
    # Foreground ruin fragments and foliage frame the scene, not the doors.
    for x in [37,923]:
        spire(x,520,.88);bush(x+12,516,1.65)
    return im

def save(name):
    OUT.mkdir(parents=True,exist_ok=True);im.save(OUT/name,optimize=True)
background(False);gate(480,326,72,231);save('eternal_gate_court.png')
background(True)
for cx,base,r,h in [(205,323,47,164),(755,323,47,164),(480,285,60,200)]:gate(cx,base,r,h)
save('three_gate_nexus.png')
# Two richly engraved door leaves; runtime clips them to the arch as they part.
im=Image.new('RGB',(144,240),'#21374a');d=ImageDraw.Draw(im)
for x in range(0,144,12):
    rect((x,0,x+10,240),rng.choice(['#26394d','#304658','#203347']))
    line([(x,0),(x,240)],'#526373')
for side in [0,72]:
    for inset in [3,8]:d.rectangle((side+inset,inset,side+71-inset,239-inset),outline=GOLD,width=1)
    for y in [37,105,173]:
        diamond(side+36,y,27,'#141f31');diamond(side+36,y,24,GOLD);diamond(side+36,y,20,'#344b5c')
        for j in [-1,0,1]:
            line([(side+26,y+j*7),(side+34,y+j*7-4),(side+42,y+j*7+3)],'#8dc6c9',2)
    for y in range(15,235,20):
        for x in [side+12,side+59]:diamond(x,y,2,LIGHT)
save('engraved_door_leaves.png')
manifest={'author':'Codex, original deterministic Pillow pixel artwork','seed':8341,'ground_y_scale':.72,'scene_size':[960,540], 'portals':{'court':[[480,326,72,231]],'nexus':[[205,323,47,164],[480,285,60,200],[755,323,47,164]]},'files':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(OUT.glob('*.png'))}}
(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(json.dumps(manifest,indent=2))
