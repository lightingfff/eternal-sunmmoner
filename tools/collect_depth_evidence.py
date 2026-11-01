#!/usr/bin/env python3
"""Collect native Canvas depth captures; GIFs sample real engine steps, not device recordings."""
from pathlib import Path
import shutil
from PIL import Image

root=Path(__file__).resolve().parents[1]
screens=root/'app/build/visual_screenshots'
out=root/'docs/visual-evidence/bd2-depth'
out.mkdir(parents=True,exist_ok=True)
for name in ['sample_normal','sample_close','architecture','overview','sample_night',
             'kaelen_emotions_normal_zoom','canopy_behind_z1.6','canopy_front_z1.6']:
    shutil.copy2(screens/f'depth_addendum/{name}.png',out/f'{name}.png')
shutil.copy2(screens/'depth_addendum/focus_isolation.txt',out/'focus_isolation.txt')
shutil.copy2(screens/'p15a/combat/events.csv',out/'combat_events.csv')
def animation(files,name,durations):
    frames=[Image.open(f).convert('RGB').crop((300,70,700,360)) for f in files]
    frames[0].save(out/f'{name}.gif',save_all=True,append_images=frames[1:],
                   duration=durations,loop=0,optimize=False,disposal=2)
times=[0,120,210,220,290,300,380,480,620,720]
animation([screens/f'p15a/combat/skill_{t}_z2.0.png' for t in times],
          'skill',[120,90,20,70,20,80,100,140,100,400])
for kind in ['walk','defeat']:
    animation([screens/f'p15a/motion/{kind}_{i}.png' for i in range(6)],kind,[100]*5+[400])
print('Native Canvas evidence collected:',out)
