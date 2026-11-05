#!/usr/bin/env python3
"""Document the game's ORIGINAL sprite canvas sizes + P27B target colors.

No user reference images are read. This creates a *styleboard*, never a game asset
or renderer screenshot. Requires Pillow >=11.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
OUT = ROOT / 'docs/p27b/previews/STYLEBOARD_FROM_EXISTING_ASSETS.png'

COLORS = [
    ('IVORY', '#F5F1E7'), ('LIMESTONE', '#E8E5DC'),
    ('SHADE', '#AAB9C7'), ('CARVING', '#687B8D'),
    ('PORTAL', '#74D6EE'), ('BRONZE', '#C7A36C'),
    ('GRASS', '#557044'), ('EARTH', '#B09471'),
    ('COBBLE', '#78848A'), ('INK', '#283B4B'),
]
FONT = ImageFont.load_default()
board = Image.new('RGB', (1120, 770), '#202c36')
draw = ImageDraw.Draw(board)
draw.text((24, 18), 'P27B / STYLE LOCK - BASELINE SPRITES + TARGET MATERIAL SWATCHES', font=FONT, fill='#f5f1e7')
draw.text((24, 39), 'PRODUCTION GUIDE ONLY / NOT RUNTIME / NOT USER REFERENCE PIXELS', font=FONT, fill='#c7a36c')

for idx, (name, rgb) in enumerate(COLORS):
    x, y = 24 + (idx % 5) * 218, 75 + (idx // 5) * 98
    draw.rectangle((x, y, x+195, y+55), fill=rgb)
    draw.text((x, y+62), f'{name} {rgb}', fill='#e8e5dc', font=FONT)

draw.text((24, 285), 'EXISTING 64x64 SOURCES: only inspect scale + material; these are NOT P27B tiles', fill='#f5f1e7')
for idx, (family, name) in enumerate([
    ('grass', 'grass_0'), ('soil', 'soil_0'), ('flagstone', 'flagstone_0'),
    ('cobble', 'cobble_0'), ('gravel', 'gravel_0'),
]):
    im = Image.open(ASSETS / 'sanctuary_p03' / f'{name}.png').convert('RGBA')
    x = 24 + idx*218
    board.paste(im.resize((128,128), Image.Resampling.NEAREST), (x, 307),
                im.getchannel('A').resize((128,128), Image.Resampling.NEAREST))
    draw.text((x, 444), f'{family} 64x64 x2', fill='#f5f1e7')

draw.text((24, 479), 'EXISTING LANDMARK CANVASES: same-pivot target must fit these bounds', fill='#f5f1e7')
for x, name in [(24, 'summoning_hall'), (465, 'tower_gate')]:
    im = Image.open(ASSETS / 'premium_slice' / f'{name}.png').convert('RGBA')
    # Original alpha stays intact; checker is board-only for pivot visibility.
    field = Image.new('RGBA', im.size, '#3b5360')
    field.alpha_composite(im)
    board.paste(field.convert('RGB'), (x, 507))
    draw.line((x+im.width//2, 507, x+im.width//2, 507+im.height), fill='#c7a36c', width=1)
    draw.text((x+im.width+9, 542), f'{name}\n{im.width}x{im.height}\nfoot: bottom center', fill='#e8e5dc', font=FONT)

OUT.parent.mkdir(parents=True, exist_ok=True)
board.save(OUT)
print(OUT)
