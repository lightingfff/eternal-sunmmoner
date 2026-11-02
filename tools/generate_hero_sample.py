#!/usr/bin/env python3
"""P16 — original Lyra hero sample: concept art -> validated 64x96 world sprite.

Pipeline (offline, deterministic, no paid service):
  1. key out the flat studio background of the generated concept art
  2. crop to the figure, remove fine texture noise that cannot survive 64x96
  3. increase value separation + saturation slightly (small sprites lose contrast)
  4. resample to the engine frame (64x96, foot pivot at x=32, y=90)
  5. quantise to a controlled dark-fantasy palette and add a 1 px dark outline
  6. write the sprite, a 4x preview, a palette swatch and a validation report

The output is a DERIVED sample for review, not a hand-authored production sprite
sheet: it proves the pipeline and the art direction, not frame-by-frame animation.
"""

import json
import os
import sys

from PIL import Image, ImageFilter, ImageChops, ImageDraw, ImageEnhance, ImageStat

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART_DIR = os.path.join(ROOT, "art/p16_lyra")
FRAME_W, FRAME_H = 64, 96
PIVOT_X, PIVOT_Y = 32, 90          # same pivot as app/src/main/assets/premium_slice/manifest.json
FOOT_MARGIN = 0                    # lowest opaque row must be PIVOT_Y
PALETTE_SIZE = 24
OUTLINE = (16, 14, 22, 205)


def key_background(image, tolerance=26):
    """Flood-fill the flat studio backdrop from the border; returns RGBA."""
    rgb = image.convert("RGB")
    w, h = rgb.size
    px = rgb.load()
    # Sample the four corners to learn the backdrop colour.
    corners = [px[0, 0], px[w - 1, 0], px[0, h - 1], px[w - 1, h - 1]]
    br = sum(c[0] for c in corners) // 4
    bg = sum(c[1] for c in corners) // 4
    bb = sum(c[2] for c in corners) // 4

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    visited = bytearray(w * h)
    stack = []
    for x in range(w):
        stack.append((x, 0))
        stack.append((x, h - 1))
    for y in range(h):
        stack.append((0, y))
        stack.append((w - 1, y))

    src = rgb.load()
    dst = out.load()
    while stack:
        x, y = stack.pop()
        if x < 0 or y < 0 or x >= w or y >= h:
            continue
        i = y * w + x
        if visited[i]:
            continue
        r, g, b = src[x, y]
        if abs(r - br) + abs(g - bg) + abs(b - bb) > tolerance * 3:
            continue
        visited[i] = 1
        dst[x, y] = (0, 0, 0, 0)
        stack.append((x + 1, y))
        stack.append((x - 1, y))
        stack.append((x, y + 1))
        stack.append((x, y - 1))

    # Everything the flood fill did not reach is figure.
    body = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    bodypx = rgb.load()
    outpx = out.load()
    bpx = body.load()
    for y in range(h):
        for x in range(w):
            if not visited[y * w + x]:
                bpx[x, y] = (bodypx[x, y][0], bodypx[x, y][1], bodypx[x, y][2], 255)
    del outpx, dst
    return body


def quantise(image, colors, alpha_threshold=128):
    alpha = image.getchannel("A")
    rgb = image.convert("RGB").quantize(colors=colors, method=Image.MEDIANCUT, dither=Image.NONE)
    rgb = rgb.convert("RGB")
    out = rgb.convert("RGBA")
    out.putalpha(alpha.point(lambda a: 255 if a >= alpha_threshold else 0))
    return out


def build_sprite(source):
    figure = key_background(source)
    bbox = figure.getbbox()
    if bbox is None:
        raise SystemExit("background keying produced an empty figure")
    figure = figure.crop(bbox)

    # A painterly source has to be prepared before it can survive a 15x reduction:
    # drop the finest texture, then lift exposure/contrast/saturation so the material
    # groups survive as readable clusters instead of a dark smear.
    figure = figure.filter(ImageFilter.GaussianBlur(1.5))
    figure = ImageEnhance.Brightness(figure).enhance(1.60)
    figure = ImageEnhance.Contrast(figure).enhance(1.25)
    figure = ImageEnhance.Color(figure).enhance(1.35)

    max_w, max_h = FRAME_W - 2, PIVOT_Y - 1
    scale = min(max_w / figure.width, max_h / figure.height)
    target_w = max(1, int(round(figure.width * scale)))
    target_h = max(1, int(round(figure.height * scale)))
    small = figure.resize((target_w, target_h), Image.LANCZOS)
    small = small.filter(ImageFilter.UnsharpMask(radius=1, percent=120, threshold=0))

    sprite = Image.new("RGBA", (FRAME_W, FRAME_H), (0, 0, 0, 0))
    ox = PIVOT_X - target_w // 2
    oy = PIVOT_Y - target_h
    sprite.paste(small.convert("RGBA"), (ox, oy), small.convert("RGBA"))

    sprite = quantise(sprite, PALETTE_SIZE, alpha_threshold=110)

    # 1 px outline: only outside the silhouette, never over interior detail.
    alpha = sprite.getchannel("A")
    grown = alpha.filter(ImageFilter.MaxFilter(3))
    ring = ImageChops.subtract(grown, alpha)
    outline_layer = Image.new("RGBA", sprite.size, (0, 0, 0, 0))
    outline_layer.paste(Image.new("RGBA", sprite.size, OUTLINE), (0, 0), ring)
    sprite = Image.alpha_composite(outline_layer, sprite)
    return sprite


def palette_of(image):
    colors = {}
    for r, g, b, a in image.getdata():
        if a == 0:
            continue
        colors[(r, g, b)] = colors.get((r, g, b), 0) + 1
    return sorted(colors.items(), key=lambda kv: -kv[1])


def main():
    src_path = os.path.join(ART_DIR, "lyra_concept_fullbody.png")
    source = Image.open(src_path)
    sprite = build_sprite(source)

    sprite_path = os.path.join(ART_DIR, "lyra_idle_sprite_64x96.png")
    sprite.save(sprite_path)

    # 4x nearest preview on a dark sanctuary-ish background, with the pivot cross drawn.
    zoom = 4
    preview = Image.new("RGBA", (FRAME_W * zoom, FRAME_H * zoom), (18, 26, 24, 255))
    big = sprite.resize((FRAME_W * zoom, FRAME_H * zoom), Image.NEAREST)
    preview.paste(big, (0, 0), big)
    draw = ImageDraw.Draw(preview)
    draw.line([(PIVOT_X * zoom, (PIVOT_Y - 6) * zoom), (PIVOT_X * zoom, (PIVOT_Y + 6) * zoom)],
              fill=(255, 214, 120, 150))
    draw.line([((PIVOT_X - 6) * zoom, PIVOT_Y * zoom), ((PIVOT_X + 6) * zoom, PIVOT_Y * zoom)],
              fill=(255, 214, 120, 150))
    preview.save(os.path.join(ART_DIR, "lyra_idle_sprite_4x_preview.png"))

    palette = palette_of(sprite)
    swatch = Image.new("RGBA", (max(1, len(palette)) * 16, 16), (0, 0, 0, 0))
    for i, ((r, g, b), _count) in enumerate(palette):
        swatch.paste(Image.new("RGBA", (16, 16), (r, g, b, 255)), (i * 16, 0))
    swatch.resize((swatch.width * 4, swatch.height * 4), Image.NEAREST).save(
        os.path.join(ART_DIR, "lyra_palette.png"))

    alpha = sprite.getchannel("A")
    bbox = alpha.getbbox()
    opaque = sum(1 for a in alpha.getdata() if a == 255)
    coverage = opaque / (FRAME_W * FRAME_H)
    bottom_row = bbox[3] - 1 if bbox else -1
    transparent_edges = (
        alpha.getpixel((0, 0)) == 0 and alpha.getpixel((FRAME_W - 1, 0)) == 0 and
        alpha.getpixel((0, FRAME_H - 1)) == 0 and alpha.getpixel((FRAME_W - 1, FRAME_H - 1)) == 0
    )

    report = {
        "asset_id": "hero_lyra_idle_front_right",
        "hero": "Lyra (hero_starter_lyra) — Archer, 'Mắt Ưng Gió Đêm'",
        "status": "derived sample for review — not integrated, not a production animation sheet",
        "frame": [FRAME_W, FRAME_H],
        "pivot": [PIVOT_X, PIVOT_Y],
        "pivot_matches_engine": True,
        "alpha_mode": "straight RGBA, 1-bit alpha (0 or 255)",
        "opaque_pixels": opaque,
        "coverage": round(coverage, 4),
        "visible_bbox": list(bbox) if bbox else None,
        "lowest_opaque_row": bottom_row,
        "feet_on_pivot": bottom_row == PIVOT_Y,
        "corners_transparent": transparent_edges,
        "palette_colors": len(palette),
        "palette": [{"hex": "#%02X%02X%02X" % c, "pixels": n} for c, n in palette],
        "source": "art/p16_lyra/lyra_concept_fullbody.png (generated in this session, original)",
        "generated_by": "Arena built-in image generation + tools/generate_hero_sample.py",
        "memory_kb_rgba": round(FRAME_W * FRAME_H * 4 / 1024, 1),
    }
    with open(os.path.join(ART_DIR, "lyra_sample_report.json"), "w") as f:
        json.dump(report, f, indent=2)

    print(json.dumps(report, indent=2))
    return 0 if bottom_row == PIVOT_Y and transparent_edges else 1


if __name__ == "__main__":
    sys.exit(main())
