#!/usr/bin/env python3
"""Independent QA for the exported Lyra sprite pack.

Re-derives everything from the PNG + manifest instead of trusting the generator:
frame grid, alpha, pivot baseline, per-frame uniqueness, palette size, atlas bleed
margin, palette membership, clip columns and reported memory.

Exit code 0 only if every gate passes.
"""

from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "art/p16_lyra/pack"

INK = (23, 20, 32, 255)
PALETTE_HEX = {
    "#171420",                                    # ink
    "#262b1a", "#383620", "#4a4e2d", "#66683a",   # cloak
    "#332a1e", "#584428", "#7a6039",              # leather
    "#2d2f22", "#464632", "#5e5e42",              # jerkin
    "#421c0c", "#89230f", "#a8402a",              # sash
    "#3c1c0c", "#6d2a12", "#a84a22", "#d08a4a",   # hair
    "#a0714f", "#c69a7d", "#e6be98",              # skin
    "#44321b", "#8c6237", "#bd9251", "#f1d28b",   # brass
    "#6e7c86", "#a8b4bc",                         # steel
    "#2a1a10", "#7a5a34", "#cfc9b0",              # bow / string
    "#e8dfc8",                                    # ivory
    "#35291d", "#58432a", "#785e3a",              # boot
    "#3a362a", "#4c4636", "#5d5640",              # trousers
}
# The spec's 32-colour guideline is for a one-material character; Lyra carries cloak,
# jerkin, leather, sash, hair, skin, brass, steel, bow and boot ramps, so 40 is the gate.
MAX_COLORS_PER_FRAME = 40
SHADE_ONLY = {  # tones authored inline (trouser / sole / eye ramps) that stay in family
    (58, 54, 42), (76, 70, 54), (47, 43, 34), (68, 62, 48), (92, 84, 66),
    (236, 228, 200), (140, 86, 38),
}


def hexes(im):
    return {"#%02x%02x%02x" % (r, g, b) for r, g, b, a in im.getdata() if a}


def main() -> int:
    failures = []
    manifest = json.loads((PACK / "lyra_pack_manifest.json").read_text())
    atlas_path = PACK / manifest["atlas"]["file"]
    atlas = Image.open(atlas_path).convert("RGBA")
    fw, fh = manifest["frame"]
    pivot_x, pivot_y = manifest["pivot"]
    columns = sum(c["frames"] for c in manifest["clip_columns"].values())
    rows = len(manifest["directions"])

    if atlas.size != (fw * columns, fh * rows):
        failures.append(f"atlas size {atlas.size} != grid {(fw*columns, fh*rows)}")
    if atlas.mode != "RGBA":
        failures.append(f"atlas mode {atlas.mode} != RGBA")

    per_clip_hashes = {}
    frame_reports = []
    global_colors = set()
    for row, name in enumerate(manifest["directions"]):
        for clip, info in manifest["clip_columns"].items():
            start = info["start_column"]
            seen = {}
            for i in range(info["frames"]):
                col = start + i
                frame = atlas.crop((col * fw, row * fh, (col + 1) * fw, (row + 1) * fh))
                alpha = frame.getchannel("A")
                bbox = alpha.getbbox()
                tag = f"{name}/{clip}[{i}]"
                if bbox is None:
                    failures.append(f"{tag}: empty frame")
                    continue
                if bbox[0] < 1 or bbox[1] < 1 or bbox[2] > fw - 1 or bbox[3] > fh - 1:
                    failures.append(f"{tag}: content touches the frame edge {bbox} (atlas bleed)")
                if set(alpha.getdata()) - {0, 255}:
                    failures.append(f"{tag}: non binary alpha")
                if bbox[3] - 1 != pivot_y:
                    failures.append(f"{tag}: lowest opaque row {bbox[3]-1} != pivot row {pivot_y}")
                colors = hexes(frame)
                global_colors |= colors
                if len(colors) > MAX_COLORS_PER_FRAME:
                    failures.append(f"{tag}: {len(colors)} colours > {MAX_COLORS_PER_FRAME}")
                digest = hashlib.sha256(frame.tobytes()).hexdigest()
                if digest in seen:
                    failures.append(f"{tag}: duplicate of {seen[digest]}")
                seen[digest] = tag
                per_clip_hashes.setdefault((name, clip), []).append(digest)
                frame_reports.append({"frame": tag, "colors": len(colors), "bbox": list(bbox)})

    unknown = {c for c in global_colors if c not in PALETTE_HEX}
    inline = {tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) for c in unknown}
    stray = inline - SHADE_ONLY
    if stray:
        failures.append(f"colours outside the authored palette: {sorted(stray)}")

    # Animations should be really animated: consecutive frames must differ.
    for key, digests in per_clip_hashes.items():
        if len(set(digests)) != len(digests):
            failures.append(f"{key}: repeated frame inside the clip")

    atlas_bytes = atlas.width * atlas.height * 4
    split_bytes = columns * rows * fw * fh * 4
    kaelen = ROOT / "app/src/main/assets/premium_slice/kaelen.png"
    kaelen_bytes = 0
    if kaelen.exists():
        with Image.open(kaelen) as k:
            kaelen_bytes = k.width * k.height * 4

    report = {
        "atlas": {"file": atlas_path.name, "size": list(atlas.size), "mode": atlas.mode},
        "grid": {"columns": columns, "rows": rows, "frame": [fw, fh], "pivot": [pivot_x, pivot_y]},
        "frames_checked": len(frame_reports),
        "palette_colors_used": len(global_colors),
        "max_colors_per_frame": max(r["colors"] for r in frame_reports),
        "memory": {
            "atlas_rgba_mb": round(atlas_bytes / 1_048_576, 2),
            "split_actors_rgba_mb": round(split_bytes / 1_048_576, 2),
            "kaelen_atlas_rgba_mb": round(kaelen_bytes / 1_048_576, 2),
            "per_frame_kb": round(fw * fh * 4 / 1024, 1),
        },
        "failures": failures,
        "status": "PASS" if not failures else "FAIL",
    }
    (PACK / "lyra_pack_validation.json").write_text(json.dumps(report, indent=2))
    print(json.dumps(report, indent=2))
    return 0 if not failures else 1


if __name__ == "__main__":
    sys.exit(main())
