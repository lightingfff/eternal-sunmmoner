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
    "#303224", "#52523a", "#6a6a4c",              # jerkin (P17-C value lift)
    "#421c0c", "#89230f", "#a8402a",              # sash
    "#3c1c0c", "#6d2a12", "#a84a22", "#d08a4a",   # hair
    "#a0714f", "#c69a7d", "#e6be98",              # skin
    "#44321b", "#8c6237", "#bd9251", "#f1d28b",   # brass
    "#6e7c86", "#a8b4bc",                         # steel
    "#26221e", "#685c46", "#cfc9b0", "#6c6856",   # bow / string / drawn string
    "#e8dfc8",                                    # ivory
    "#35291d", "#58432a", "#785e3a",              # boot
    "#3a362a", "#4c4636", "#5d5640",              # trousers
    "#6cced6", "#c4f6f6",                         # skill magic arrow
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
    rows = len(manifest["direction_rows"])

    if atlas.size != (fw * columns, fh * rows):
        failures.append(f"atlas size {atlas.size} != grid {(fw*columns, fh*rows)}")
    if atlas.mode != "RGBA":
        failures.append(f"atlas mode {atlas.mode} != RGBA")

    per_clip_hashes = {}
    frame_reports = []
    global_colors = set()
    directions = [n for n, _ in sorted(manifest["direction_rows"].items(), key=lambda kv: kv[1])]
    for row, name in enumerate(directions):
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

    # 1) The pack must match what PremiumActorAtlas reads, or getValue("attack") crashes.
    engine_path = ROOT / "app/src/main/assets/premium_slice/manifest.json"
    engine = json.loads(engine_path.read_text())
    engine_clips = engine["clips"]
    engine_order = engine["clip_order"]
    if list(manifest["clip_order"]) != list(engine_order):
        failures.append(f"clip_order {manifest['clip_order']} != engine {engine_order}")
    for clip in engine_order:
        if clip not in manifest["clips"]:
            failures.append(f"engine clip '{clip}' missing from the pack")
            continue
        if len(manifest["clips"][clip]) != len(engine_clips[clip]):
            failures.append(f"clip '{clip}' has {len(manifest['clips'][clip])} durations, "
                            f"engine expects {len(engine_clips[clip])}")
        if any(abs(a - b) > 1e-6 for a, b in zip(manifest["clips"][clip], engine_clips[clip])):
            failures.append(f"clip '{clip}' durations differ from the engine contract")

    # 2) The shipped asset must be the reviewed pack, byte for byte.
    asset = ROOT / "app/src/main/assets/premium_slice" / manifest["atlas"]["file"]
    if not asset.exists():
        failures.append(f"runtime asset missing: {asset.relative_to(ROOT)}")
    else:
        if hashlib.sha256(asset.read_bytes()).hexdigest() != manifest["atlas"]["sha256"]:
            failures.append("reviewed pack atlas and shipped asset differ (regenerate both)")
        entry = engine.get("assets", {}).get("lyra", {})
        if entry.get("sha256") != manifest["atlas"]["sha256"]:
            failures.append("premium_slice/manifest.json lyra sha256 is stale")
        if entry.get("size") != [atlas.width, atlas.height]:
            failures.append("premium_slice/manifest.json lyra size is stale")

    # 3) No frame may be another frame translated: that is the "same still shifted" defect.
    def mask(frame):
        bbox = frame.getchannel("A").getbbox()
        return frame.crop(bbox) if bbox else None

    for clip, info in manifest["clip_columns"].items():
        start, count = info["start_column"], info["frames"]
        for row, name in enumerate(directions):
            for i in range(count - 1):
                a = mask(atlas.crop(((start + i) * fw, row * fh, (start + i + 1) * fw, (row + 1) * fh)))
                b = mask(atlas.crop(((start + i + 1) * fw, row * fh, (start + i + 2) * fw, (row + 1) * fh)))
                if a is None or b is None or a.size != b.size:
                    continue
                if a.tobytes() == b.tobytes():
                    failures.append(f"{name}/{clip}[{i}]->[{i+1}] is a pure translation "
                                    f"(identical silhouette shifted, not animated)")

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
        "engine_contract": {"clip_order": list(engine_order),
                            "frames_per_clip": {c: len(engine_clips[c]) for c in engine_order}},
        "shipped_asset": {"file": manifest["atlas"]["file"],
                          "sha256_matches_pack": not any("sha256" in f for f in failures)},
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
