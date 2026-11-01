#!/usr/bin/env python3
"""Collect native-Canvas test captures; GIFs are sampled simulation evidence, not screen recordings."""
from pathlib import Path
import argparse
import shutil
from PIL import Image

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--baseline', type=Path, required=True, help='Untouched-commit test output directory')
args = parser.parse_args()
source = root/'app/build/visual_screenshots'
out = root/'docs/visual-evidence/p15a'
for phase, directory in [('before',args.baseline),('after',source/'p15a')]:
    target=out/phase;target.mkdir(parents=True,exist_ok=True)
    for name in ['sample_normal','sample_close','architecture','overview']:
        shutil.copy2(directory/(name+'.png'),target/(name+'.png'))
shutil.copy2(source/'oblique/occlusion_true.png',out/'after/canopy.png')
shutil.copy2(source/'p15a/combat/skill_300_z1.0.png',out/'after/skill_normal_zoom.png')
shutil.copy2(source/'p15a/combat/events.csv',out/'combat_events.csv')

def animation(files, name, durations, crop=(330,70,630,360)):
    frames=[Image.open(f).convert('RGB').crop(crop) for f in files]
    frames[0].save(out/(name+'.gif'),save_all=True,append_images=frames[1:],
                   duration=durations,loop=0,optimize=False,disposal=2)

times=[0,120,210,220,290,300,380,480,620,720]
for kind in ['basic','skill']:
    files=[source/f'p15a/combat/{kind}_{t}_z2.0.png' for t in times]
    animation(files,kind,[120,90,20,70,20,80,100,140,100,400])
for kind,count in [('idle',9),('walk',6),('defeat',6)]:
    animation([source/f'p15a/motion/{kind}_{i}.png' for i in range(count)],kind,
              [100]*(count-1)+[400],(300,70,700,360))
print('Collected matched before/after PNGs, five simulation GIFs and contact events:',out)
