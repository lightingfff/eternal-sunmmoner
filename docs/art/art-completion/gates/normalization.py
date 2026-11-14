#!/usr/bin/env python3
"""Normalize genuine gate edits, register scene art to unchanged game apertures.

This script does technical resampling only: it does not invent architecture, paint
new columns or generate missing artwork. Runtime masks, hitboxes and timing stay
owned by the coordinator. Source images remain untouched beside this script.
"""
from __future__ import annotations

import bisect
import hashlib
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
OUT = ROOT / 'app/src/main/assets/art_completion_v1/gates'
NEXUS_X = [(0, 0), (157, 158), (250, 252), (420, 420), (539, 540),
           (709, 708), (803, 802), (959, 959)]
NEXUS_Y = [(0, 0), (78, 85), (138, 145), (145, 159), (191, 206),
           (259, 285), (302, 323), (539, 539)]
APERTURES = {
    'eternal_gate_court': [(480, 326, 72, 231)],
    'three_gate_nexus': [(205, 323, 47, 164), (480, 285, 60, 200), (755, 323, 47, 164)],
}


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def inverse_map(points, length):
    assert all(points[i][0] < points[i + 1][0] and points[i][1] < points[i + 1][1]
               for i in range(len(points) - 1))
    ends = [p[1] for p in points]
    output = []
    for target in range(length):
        i = max(0, min(len(points) - 2, bisect.bisect_right(ends, target) - 1))
        s0, t0 = points[i]
        s1, t1 = points[i + 1]
        source = round(s0 + (target - t0) * (s1 - s0) / (t1 - t0))
        output.append(max(0, min(length - 1, source)))
    return output


def register(image):
    # Whole-scene monotonic resampling has no local seams or changed portal data.
    horizontal = Image.new('RGB', image.size)
    for x, source in enumerate(inverse_map(NEXUS_X, image.width)):
        horizontal.paste(image.crop((source, 0, source + 1, image.height)), (x, 0))
    result = Image.new('RGB', image.size)
    for y, source in enumerate(inverse_map(NEXUS_Y, image.height)):
        result.paste(horizontal.crop((0, source, image.width, source + 1)), (0, y))
    return result


def aperture_mask(spec, inset=0):
    cx, base, radius, height = spec
    spring = base - height + radius
    result = Image.new('1', (960, 540))
    pix = result.load()
    r = radius - inset
    for y in range(max(0, base - height + inset), base - inset):
        half = r if y >= spring else math.sqrt(max(0, r * r - (y + .5 - spring) ** 2))
        for x in range(math.ceil(cx - half), math.floor(cx + half)):
            pix[x, y] = 1
    return result


def inspect_apertures(name, image):
    report = []
    overlay = image.convert('RGBA')
    pixels = image.load()
    draw = ImageDraw.Draw(overlay)
    for spec in APERTURES[name]:
        mask = aperture_mask(spec)
        inside = []
        bright = []
        core = aperture_mask(spec, 4)
        core_pixels = core.load()
        for y in range(540):
            for x in range(960):
                if mask.getpixel((x, y)):
                    inside.append((x, y))
                    if max(pixels[x, y][:3]) > 45:
                        bright.append((x, y))
                        draw.point((x, y), fill=(255, 123, 62, 255))
        cx, base, radius, height = spec
        spring = base - height + radius
        path = [(cx - radius, base), (cx - radius, spring)]
        path += [(cx + radius * math.cos(math.pi + i * math.pi / 64),
                  spring + radius * math.sin(math.pi + i * math.pi / 64)) for i in range(65)]
        path += [(cx + radius, base)]
        draw.line(path, fill=(52, 242, 244, 255), width=1)
        core_total = sum(1 for value in core.convert('L').tobytes() if value)
        core_bright = sum(1 for x, y in bright if core_pixels[x, y])
        report.append(dict(center_x=cx, base=base, radius=radius, height=height,
                           runtime_aperture_pixels=len(inside),
                           bright_pixels_in_runtime_aperture=len(bright),
                           bright_pixels_in_four_pixel_inset=core_bright,
                           inset_core_pixels=core_total,
                           inset_core_dark_fraction=round(1 - core_bright / max(1, core_total), 6),
                           note='Orange marks non-dark pixels touched by existing runtime aperture; cyan is actual unchanged mask boundary. Interior is not painted over by normalizer.'))
    overlay.convert('RGB').save(HERE / f'{name}-aperture-overlay.png')
    return report


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    entries = []
    aperture_reports = {}
    contact = Image.new('RGB', (960, 580), '#101b28')
    text = ImageDraw.Draw(contact)
    for row, name in enumerate(['eternal_gate_court', 'three_gate_nexus', 'engraved_door_leaves']):
        path = HERE / 'source' / f'{name}.png'
        opened = Image.open(path)
        size = (144, 240) if name == 'engraved_door_leaves' else (960, 540)
        image = opened.convert('RGB').resize(size, Image.Resampling.NEAREST)
        registration = None
        if name == 'three_gate_nexus':
            image = register(image)
            registration = dict(method='global monotonic nearest coordinate registration',
                                horizontal_source_to_runtime=NEXUS_X, vertical_source_to_runtime=NEXUS_Y)
        image = image.quantize(colors=256, method=Image.Quantize.MEDIANCUT,
                               dither=Image.Dither.NONE).convert('RGBA')
        if name == 'engraved_door_leaves':
            # Preserve the exact old leaf silhouette/gutters, not generated alpha guesses.
            old = Image.open(ROOT / 'app/src/main/assets/tower_gate_map/engraved_door_leaves.png').convert('RGBA')
            image.putalpha(old.getchannel('A'))
        else:
            assert image.getchannel('A').getextrema() == (255, 255)
            aperture_reports[name] = inspect_apertures(name, image)
            old = Image.open(ROOT / 'app/src/main/assets/tower_gate_map' / f'{name}.png').convert('RGB')
            y = row * 290
            contact.paste(old.resize((480, 270), Image.Resampling.NEAREST), (0, y + 20))
            contact.paste(image.convert('RGB').resize((480, 270), Image.Resampling.NEAREST), (480, y + 20))
            text.text((8, y + 5), f'{name}: original', fill='#e7d5aa')
            text.text((488, y + 5), 'registered original imagegen edit', fill='#e7d5aa')
        dest = OUT / f'{name}.png'
        image.save(dest, optimize=True)
        entries.append(dict(id=name, file=dest.name, width=size[0], height=size[1],
                            sha256=sha(dest), rgba_bytes=size[0] * size[1] * 4,
                            source=str(path.relative_to(ROOT)), source_sha256=sha(path),
                            source_dimensions=list(opened.size), source_origin='genuine hosted imagegen edit',
                            normalization='nearest resample; fixed scene registration where declared;256color palette; no generated geometry/actor/portal state changes',
                            registration=registration, native_generated_dimensions_claimed=False))
    total = sum(entry['rgba_bytes'] for entry in entries)
    assert total == 4_285_440
    manifest = dict(version=1, assets=entries, retained_rgba_bytes=total,
                    architecture_camera='existing oblique 2.5D composition', runtime_geometry_changed=False,
                    cutscene_timing_changed=False, mask_alignment=aperture_reports,
                    review_status='Asset normalization only; coordinator must inspect actual runtime opening overlay')
    (OUT / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    (HERE / 'normalization-report.json').write_text(json.dumps(manifest, indent=2) + '\n')
    contact.save(HERE / 'before-after-contact.png')
    print(json.dumps(dict(total_bytes=total, mask_alignment=aperture_reports), indent=2))


if __name__ == '__main__':
    build()
