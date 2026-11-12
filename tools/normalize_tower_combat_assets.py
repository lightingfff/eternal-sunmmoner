"""Normalize an original generated enemy atlas into runtime sprites; no image generation.
Requires Pillow, numpy and scipy. Retains the source's articulated poses and one scale
per actor. Connected components recover complete weapons beyond imperfect cell borders.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import statistics
import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

ACTORS = (
    ("FIELD_GOBLIN_RAIDER", "raider", .53, 1.0),
    ("FIELD_GOBLIN_ARCHER", "archer", .55, 1.0),
    ("DIRE_WOLF", "wolf", .62, 1.0),
    ("GOBLIN_WAR_CAPTAIN", "kraag", .60, 1.10),
)
FRAME = (128, 144)
PIVOT = (64, 132)

def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

def normalize(source: Path, output: Path, evidence: Path) -> None:
    image = Image.open(source).convert("RGBA")
    rgba = np.array(image)
    labels, _ = ndimage.label(rgba[:, :, 3] > 96, np.ones((3, 3)))
    counts = np.bincount(labels.ravel())
    components = []
    for label in np.flatnonzero(counts > 450):
        if label == 0:
            continue
        ys, xs = np.where(labels == label)
        components.append((int(label), (int(xs.min()), int(ys.min()), int(xs.max())+1, int(ys.max())+1)))
    if len(components) != 48:
        raise ValueError(f"Expected48 separately articulated bodies, got{len(components)}; review source, never fabricate missing poses.")
    components.sort(key=lambda item: (item[1][1]+item[1][3])/2)
    output.mkdir(parents=True, exist_ok=True)
    evidence.mkdir(parents=True, exist_ok=True)
    manifest = {
        "version": 1, "source_sha256": sha(source), "source_dimensions": list(image.size),
        "frame": list(FRAME), "pivot": list(PIVOT), "facing": "right",
        "direction_policy": "Left is runtime mirrored; shadows use fixed SanctuaryLight. No claimed separately authored four-direction atlas.",
        "clips": {
            "idle": {"frames": [0,1], "seconds": [.35,.35], "loop": True},
            "walk": {"frames": [2,3,4,5], "seconds": [.10,.10,.10,.10], "loop": True},
            "contact": {"frames": [6,6,7,8], "seconds": [.11,.11,.10,.16], "loop": False, "contact_seconds": .22},
            "instant_attack": {"frames": [7,8,0], "seconds": [.08,.17,.135], "loop": False},
            "hurt": {"frames": [9], "seconds": [.18], "loop": False},
            "defeat": {"frames": [10,11], "seconds": [.18,.275], "loop": False},
        },
        "actors": {},
    }
    contact = Image.new("RGBA", (FRAME[0]*12, FRAME[1]*4), (18,25,35,255))
    validation = []
    for row, (archetype, actor, scale, world_scale) in enumerate(ACTORS):
        cells = sorted(components[row*12:(row+1)*12], key=lambda item: (item[1][0]+item[1][2])/2)
        foot = statistics.median(b[3] for _,b in cells[:6])
        sheet = Image.new("RGBA", (FRAME[0]*12, FRAME[1]))
        hashes, frames = [], []
        for column,(label,bounds) in enumerate(cells):
            x0,y0,x1,y1 = bounds
            crop = image.crop(bounds)
            mask = Image.fromarray(np.where(labels[y0:y1,x0:x1]==label, 255, 0).astype("uint8"))
            crop.putalpha(mask)
            w,h = max(1,round(crop.width*scale)),max(1,round(crop.height*scale))
            crop = crop.resize((w,h), Image.Resampling.NEAREST)
            nominal_x = (column+.5)*image.width/12
            px = round(PIVOT[0]+(x0-nominal_x)*scale)
            py = round(PIVOT[1]+(y0-foot)*scale)
            if px < 2 or py < 2 or px+w > FRAME[0]-2 or py+h > FRAME[1]-2:
                raise ValueError(f"{actor}/{column}: needs padding, not clipping: {(px,py,w,h)}")
            frame = Image.new("RGBA", FRAME)
            frame.alpha_composite(crop, (px,py))
            sheet.alpha_composite(frame,(column*FRAME[0],0))
            contact.alpha_composite(frame,(column*FRAME[0],row*FRAME[1]))
            hashes.append(hashlib.sha256(frame.tobytes()).hexdigest())
            frames.append(frame)
        if len(set(hashes)) != 12:
            raise ValueError(f"{actor}: duplicate authored poses")
        path = output/f"{actor}.png"
        sheet.save(path,optimize=True)
        manifest["actors"][archetype] = {
            "file": path.name, "world_scale": world_scale,
            "sha256": sha(path), "source_scale": scale, "source_foot": foot,
        }
        frames[0].save(evidence/f"{actor}-walk.gif", save_all=True,
            append_images=frames[2:6], duration=[350,100,100,100,100], loop=0, disposal=2)
        frames[6].save(evidence/f"{actor}-contact.gif", save_all=True,
            append_images=[frames[7],frames[8],frames[0]], duration=[220,100,160,300], loop=0, disposal=2)
        validation.append({"actor":actor,"unique_poses":len(set(hashes)),
            "rgba_bytes_both_facings":FRAME[0]*FRAME[1]*4*24,
            "has_real_transparency":sheet.getchannel("A").getextrema()==(0,255),
            "sha256":sha(path)})
    (output/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n")
    contact.save(evidence/"enemy-contact-sheet.png",optimize=True)
    (evidence/"asset-validation.json").write_text(json.dumps({
        "source_sha256":sha(source),"actors":validation,
        "retained_rgba_bytes_both_facings":sum(x["rgba_bytes_both_facings"] for x in validation),
        "notes":"Calculated CPU bitmap pixels only, excludes GPU/shadow paths. No physical memory/FPS measurement.",
    },indent=2)+"\n")
    print(json.dumps(validation,indent=2))

def normalize_vfx(source: Path, output: Path, evidence: Path) -> None:
    image = Image.open(source).convert("RGBA")
    cell = (96, 96)
    pivot = (48, 84)
    sheet = Image.new("RGBA", (cell[0]*8, cell[1]*4))
    validation = []
    for row, name in enumerate(("guard", "arcane", "cleave", "holy")):
        frames = []
        raw = []
        for column in range(8):
            frame = image.crop((round(column*image.width/8), round(row*image.height/4),
                round((column+1)*image.width/8), round((row+1)*image.height/4)))
            raw.append(frame)
        baseline = statistics.median(frame.getchannel("A").point(lambda a:255 if a>96 else 0).getbbox()[3] for frame in raw)
        for column, frame in enumerate(raw):
            # Fixed .40 source scale, never per-frame size fitting: preserve the impact's growth.
            frame.putalpha(frame.getchannel("A").point(lambda a:0 if a<48 else a))
            frame = frame.resize((round(frame.width*.40), round(frame.height*.40)),Image.Resampling.NEAREST)
            normalized = Image.new("RGBA",cell)
            normalized.alpha_composite(frame,(round(pivot[0]-frame.width/2),round(pivot[1]-baseline*.40)))
            opaque = normalized.getchannel("A").point(lambda a:255 if a>96 else 0).getbbox()
            if not opaque or opaque[0]<1 or opaque[1]<1 or opaque[2]>95 or opaque[3]>95:
                raise ValueError(f"{name}/{column}: impact core would be clipped: {opaque}")
            sheet.alpha_composite(normalized,(column*96,row*96))
            frames.append(normalized)
        hashes = {hashlib.sha256(frame.tobytes()).hexdigest() for frame in frames}
        if len(hashes) != 8:
            raise ValueError(f"{name}: duplicate VFX poses")
        frames[0].save(evidence/f"{name}-vfx.gif",save_all=True,append_images=frames[1:],
            duration=[65]*8,loop=0,disposal=2)
        validation.append({"style":name,"unique_frames":len(hashes)})
    path = output/"vfx.png"
    sheet.save(path,optimize=True)
    (output/"vfx_manifest.json").write_text(json.dumps({
        "version":1,"file":"vfx.png","sha256":sha(path),"source_sha256":sha(source),
        "frame":list(cell),"pivot":list(pivot),"columns":8,"rows":["guard","arcane","cleave","holy"],
        "source_scale":.40,"rgba_bytes":96*96*4*32,"styles":validation,
    },indent=2)+"\n")
    sheet.save(evidence/"vfx-contact-sheet.png",optimize=True)

if __name__ == "__main__":
    p=argparse.ArgumentParser()
    p.add_argument("source",type=Path)
    p.add_argument("--output",type=Path,required=True)
    p.add_argument("--evidence",type=Path,required=True)
    p.add_argument("--vfx-source",type=Path)
    a=p.parse_args()
    normalize(a.source,a.output,a.evidence)
    if a.vfx_source:
        normalize_vfx(a.vfx_source,a.output,a.evidence)

