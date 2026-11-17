#!/usr/bin/env python3
"""Normalize original vertical/ground materials without changing runtime geometry."""
import hashlib
import json
from pathlib import Path
from PIL import Image
from build_art_depth_materials import periodic_panel

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'docs/art/art-completion/surfaces/source.png'
OUTPUT = ROOT / 'app/src/main/assets/art_completion_v1/surfaces'


def build():
    source = Image.open(SOURCE).convert('RGB')
    OUTPUT.mkdir(parents=True, exist_ok=True)
    entries = []
    for index, name in enumerate(('cliff', 'limestone', 'carpet', 'canal')):
        x, y = index % 2, index // 2
        panel = source.crop((round(x * source.width / 2), round(y * source.height / 2),
                             round((x + 1) * source.width / 2), round((y + 1) * source.height / 2)))
        image = periodic_panel(panel)
        path = OUTPUT / f'{name}.png'
        image.save(path, optimize=True)
        entries.append(dict(id=name, file=path.name, width=256, height=256,
                            sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
    manifest = dict(version=1, source=str(SOURCE.relative_to(ROOT)),
                    source_sha256=hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
                    source_dimensions=list(source.size), native_generated_dimensions_claimed=False,
                    normalization='Per quadrant nearest256, symmetric12px wrap,128colour palette; original source retained',
                    retained_rgba_bytes=4 * 256 * 256 * 4, materials=entries)
    (OUTPUT / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print(json.dumps(manifest))


if __name__ == '__main__':
    build()
