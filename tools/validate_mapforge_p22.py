#!/usr/bin/env python3
"""Validator for P22 Vegetation Forge assets, geometry contracts, shadow scanline budget,
species distinctness, generator determinism, and locked P18/P20 control baselines.
"""
from __future__ import annotations

import hashlib
import io
import json
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SANCT = ROOT / "app/src/main/assets/sanctuary_p03"
MANIFEST = SANCT / "manifest.json"

sys.path.insert(0, str(ROOT / "tools"))
import mapforge_p22_vegetation as gen  # noqa: E402

LOCKED_CONTROLS = {
    "oak": {
        "sha256": "556eefd5c1b1f14872b67e6e651b8c7d6645e4632519d0719a6ce614b240a229",
        "size": [112, 136],
        "bottom": 134,
    },
    "cypress": {
        "sha256": "442cbc617c6d61f79f2105f7c4b2072a2fc721fa3a4e56ee11d695d576cf988b",
        "size": [48, 88],
        "bottom": 87,
    },
}

P22_EXPECTED = {
    "canopy_a": {"size": [114, 132], "bottom": 126, "min_x_span": 85},
    "canopy_b": {"size": [96, 114], "bottom": 109, "min_x_span": 70},
    "pine": {"size": [96, 128], "bottom": 122, "min_x_span": 55},
    "twisted": {"size": [112, 132], "bottom": 126, "min_x_span": 100},
    "hedge_bush": {"size": [64, 40], "bottom": 36, "min_x_span": 48},
    "forest_bush": {"size": [64, 40], "bottom": 36, "min_x_span": 48},
}


def shadow_scanline_rects(im: Image.Image) -> int:
    """Exact mirror of `SanctuaryEnvironmentShadows.silhouette` horizontal run-rect count."""
    w, h = im.size
    apx = im.load()
    rects = 0
    for y in range(0, h, 2):
        x = 0
        while x < w:
            while x < w and apx[x, y][3] < 128:
                x += 1
            if x >= w:
                break
            x += 1
            while x < w and apx[x, y][3] >= 128:
                x += 1
            rects += 1
    return rects


def normalized_mask_iou(a: Image.Image, b: Image.Image, grid: int = 64) -> float:
    ma = a.split()[3].resize((grid, grid), Image.Resampling.NEAREST)
    mb = b.split()[3].resize((grid, grid), Image.Resampling.NEAREST)
    pa, pb = ma.load(), mb.load()
    inter = union = 0
    for y in range(grid):
        for x in range(grid):
            oa = pa[x, y] >= 128
            ob = pb[x, y] >= 128
            if oa and ob:
                inter += 1
            if oa or ob:
                union += 1
    return inter / max(1, union)


def main() -> int:
    failures: list[str] = []
    report: dict[str, object] = {"controls": {}, "p22_assets": {}, "pairwise_iou": {}}

    manifest = json.loads(MANIFEST.read_text())
    by_id = {entry["id"]: entry for entry in manifest["assets"]}

    # 1. Verify P18 oak & P20 cypress locked baselines are byte-identical to main.
    for cid, spec in LOCKED_CONTROLS.items():
        p = SANCT / f"{cid}.png"
        if not p.exists():
            failures.append(f"missing control {cid}.png")
            continue
        sha = hashlib.sha256(p.read_bytes()).hexdigest()
        im = Image.open(p).convert("RGBA")
        bounds = gen.measure_alpha_bounds(im)
        if sha != spec["sha256"]:
            failures.append(f"LOCKED CONTROL {cid}.png modified! got {sha}, expected {spec['sha256']}")
        if bounds["size"] != spec["size"] or bounds["bottom"] != spec["bottom"]:
            failures.append(f"control {cid}.png geometry drift: {bounds}")
        report["controls"][cid] = {"sha256_ok": sha == spec["sha256"], "size": bounds["size"], "bottom": bounds["bottom"]}

    # 2. Verify all 6 P22 vegetation assets, determinism, bounds, palette discipline & shadow budget.
    loaded_imgs: dict[str, Image.Image] = {}
    for aid, spec in P22_EXPECTED.items():
        p = SANCT / f"{aid}.png"
        if not p.exists():
            failures.append(f"missing P22 asset {aid}.png")
            continue
        raw = p.read_bytes()
        sha = hashlib.sha256(raw).hexdigest()
        im = Image.open(io.BytesIO(raw))
        if im.mode != "RGBA":
            failures.append(f"{aid}.png mode is {im.mode}, expected RGBA")
        im = im.convert("RGBA")
        loaded_imgs[aid] = im
        bounds = gen.measure_alpha_bounds(im)
        rects = shadow_scanline_rects(im)

        if bounds["size"] != spec["size"]:
            failures.append(f"{aid}: size {bounds['size']} != {spec['size']}")
        if bounds["bottom"] != spec["bottom"]:
            failures.append(f"{aid}: alpha>=128 bottom {bounds['bottom']} != {spec['bottom']}")
        if bounds["x_span"] < spec["min_x_span"]:
            failures.append(f"{aid}: x_span {bounds['x_span']} < {spec['min_x_span']}")
        if bounds["semi_transparent_px"] != 0:
            failures.append(f"{aid}: {bounds['semi_transparent_px']} semi-transparent halo pixels found")
        if bounds["top"] < 2:
            failures.append(f"{aid}: crown clips top border (top={bounds['top']})")
        if bounds["x_range"][0] < 3 or bounds["x_range"][1] > spec["size"][0] - 4:
            failures.append(f"{aid}: silhouette touches side border ({bounds['x_range']})")
        if not (8 <= bounds["opaque_colors"] <= 24):
            failures.append(f"{aid}: opaque_colors={bounds['opaque_colors']} outside [8..24] pixel-art ramp")
        if rects > 180:
            failures.append(f"{aid}: shadow scanline rect count {rects} > 180")

        # Manifest check
        if aid not in by_id:
            failures.append(f"{aid}: missing from sanctuary_p03/manifest.json")
        else:
            m = by_id[aid]
            if m["sha256"] != sha:
                failures.append(f"{aid}: manifest sha256 {m['sha256']} != file {sha}")
            if m["size"] != spec["size"]:
                failures.append(f"{aid}: manifest size {m['size']} != {spec['size']}")
            if m["path"] != f"{aid}.png":
                failures.append(f"{aid}: manifest path {m['path']} != {aid}.png")

        # Determinism check against generator function
        regen = gen.ASSET_SPECS[aid]["fn"]()
        if list(regen.getdata()) != list(im.getdata()):
            failures.append(f"{aid}: disk PNG pixels do not match deterministic generator output")

        report["p22_assets"][aid] = {
            **bounds,
            "shadow_scanline_rects": rects,
            "sha256": sha,
        }

    # 3. Verify species silhouette distinctness (no rescaled clones).
    tree_keys = ["canopy_a", "canopy_b", "pine", "twisted"]
    for i in range(len(tree_keys)):
        for j in range(i + 1, len(tree_keys)):
            k1, k2 = tree_keys[i], tree_keys[j]
            if k1 in loaded_imgs and k2 in loaded_imgs:
                iou = normalized_mask_iou(loaded_imgs[k1], loaded_imgs[k2])
                report["pairwise_iou"][f"{k1}_vs_{k2}"] = round(iou, 4)
                if iou > 0.80:
                    failures.append(f"silhouette clone detected: {k1} vs {k2} IoU={iou:.3f} > 0.80")

    if "hedge_bush" in loaded_imgs and "forest_bush" in loaded_imgs:
        bush_iou = normalized_mask_iou(loaded_imgs["hedge_bush"], loaded_imgs["forest_bush"])
        report["pairwise_iou"]["hedge_bush_vs_forest_bush"] = round(bush_iou, 4)
        if bush_iou > 0.88:
            failures.append(f"bush clone detected: hedge_bush vs forest_bush IoU={bush_iou:.3f}")
        # Also verify distinct color sets between courtyard hedge and woodland bush
        c_hedge = set(loaded_imgs["hedge_bush"].getdata())
        c_forest = set(loaded_imgs["forest_bush"].getdata())
        if c_hedge == c_forest:
            failures.append("hedge_bush and forest_bush have identical palettes")

    report["failures"] = failures
    report["status"] = "PASS" if not failures else "FAIL"
    print(json.dumps(report, indent=2))
    return 0 if not failures else 1


if __name__ == "__main__":
    raise SystemExit(main())
