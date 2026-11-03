#!/usr/bin/env python3
"""P25 — baked architecture face / material volume grammar (OFFLINE ONLY).

Reads the single precommitted source of truth
``docs/quality-loop/p25-architecture-volume/roi_definitions.json`` and applies
a deterministic per-face value ramp to an authored RGBA sprite at generation
time. No runtime Kotlin, no shader, no per-frame allocation, no alpha/size/
pivot change, no fullscreen pass.

Why it exists (P24 root cause, see docs/quality-loop/p24-sanctuary-hdr/ROUND_4.md):
the authored sprites gave roof / front wall / return wall nearly the same
luminance, so an additive HDR grade could only tint the whole silhouette and
never create a readable edge. Fix the VALUE of the faces in the asset itself;
P24's grade then rides on top of real geometry instead of replacing it.

DETERMINISM: pure integer/pixel arithmetic in a fixed face priority order, a
single pass, no RNG, no sampling, no wall-clock, no dependency on the output
file. Generating the same asset twice yields byte-identical PNGs.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ROI_PATH = ROOT / "docs" / "quality-loop" / "p25-architecture-volume" / "roi_definitions.json"

# Integer Rec.709 coefficients (matches the QA measurement scripts exactly).
LR, LG, LB = 2126, 7152, 722


def load_spec() -> dict:
    with ROI_PATH.open(encoding="utf-8") as fh:
        return json.load(fh)


def luma(r: int, g: int, b: int) -> float:
    return (LR * r + LG * g + LB * b) / 10000.0


def point_in_poly(poly, x: float, y: float) -> bool:
    n = len(poly)
    hit = False
    j = n - 1
    for i in range(n):
        xi, yi = poly[i]
        xj, yj = poly[j]
        if ((yi > y) != (yj > y)) and (x < (xj - xi) * (y - yi) / (yj - yi) + xi):
            hit = not hit
        j = i
    return hit


def is_emissive(r: int, g: int, b: int) -> bool:
    """Warm window / lantern / forge fire, or cyan/teal magic. Never graded."""
    warm = r >= 130 and r > b + 40
    cold_magic = b >= 110 and b > r + 30
    return warm or cold_magic


def _clamp(v: float) -> int:
    return 0 if v < 0 else (255 if v > 255 else int(v + 0.5))


def apply_face_grade(im, key: str, spec: dict | None = None) -> dict:
    """Grade one authored sprite in place. Returns a small deterministic report.

    ``im`` is a PIL RGBA Image. Only opaque, non-emissive, non-protected pixels
    inside an authored face polygon are touched. Each pixel is considered at
    most once, in the declared priority order, so narrow dark bands
    (soffit / eave / door reveal) win over the large wall planes.
    """
    spec = spec if spec is not None else load_spec()
    if key not in spec["assets"]:
        return {"key": key, "graded_pixels": 0, "faces": {}}
    asset = spec["assets"][key]
    cfg = spec["grade"]
    dark_floor = cfg["dark_floor"]
    bright_ceiling = cfg["bright_ceiling"]
    W, H = im.size
    if list(asset["canvas"]) != [W, H]:
        raise ValueError(f"{key}: ROI canvas {asset['canvas']} != sprite {W}x{H} — do not guess coordinates")
    px = im.load()
    done = bytearray(W * H)

    # Protected regions (door contract, dark portal, rune ring) stay byte-identical.
    for x0, y0, x1, y1 in asset.get("protect", []):
        for y in range(max(0, y0), min(H, y1)):
            for x in range(max(0, x0), min(W, x1)):
                done[y * W + x] = 1

    report = {"key": key, "graded_pixels": 0, "faces": {}}
    for face in cfg["priority"]:
        f = asset["faces"].get(face)
        if not f or f.get("protected") or not f.get("poly"):
            continue
        poly = f["poly"]
        xs = [p[0] for p in poly]
        ys = [p[1] for p in poly]
        pts = []
        for y in range(max(0, min(ys)), min(H, max(ys) + 1)):
            for x in range(max(0, min(xs)), min(W, max(xs) + 1)):
                idx = y * W + x
                if done[idx] or not point_in_poly(poly, x + 0.5, y + 0.5):
                    continue
                r, g, b, a = px[x, y]
                if a < 200:
                    continue
                pts.append((x, y, r, g, b, a, luma(r, g, b)))
        if not pts:
            report["faces"][face] = {"candidates": 0, "graded": 0}
            continue

        allow_dark = bool(f.get("allow_dark"))
        eligible = [
            p for p in pts
            if not is_emissive(p[2], p[3], p[4])
            and (allow_dark or (dark_floor <= p[6] <= bright_ceiling))
        ]
        # Every candidate is consumed by this face: lower-priority faces must
        # not re-grade protected dark outlines or emissive glass.
        for x, y, *_ in pts:
            done[y * W + x] = 1
        if not eligible:
            report["faces"][face] = {"candidates": len(pts), "graded": 0}
            continue

        old_mean = sum(p[6] for p in eligible) / len(eligible)
        target = float(f["mean"])
        gain = float(f["gain"])
        hr, hg, hb = (float(v) for v in f["hue"])
        for x, y, r, g, b, a, L in eligible:
            new_l = min(245.0, max(4.0, target + (L - old_mean) * gain))
            k = new_l / L if L > 0 else 1.0
            r2, g2, b2 = r * k * hr, g * k * hg, b * k * hb
            l2 = luma(r2, g2, b2)
            s = new_l / l2 if l2 > 0 else 1.0
            px[x, y] = (_clamp(r2 * s), _clamp(g2 * s), _clamp(b2 * s), a)
        report["faces"][face] = {
            "candidates": len(pts), "graded": len(eligible),
            "old_mean": round(old_mean, 2), "target_mean": target, "gain": gain,
        }
        report["graded_pixels"] += len(eligible)
    return report


def measure_faces(im, key: str, spec: dict | None = None) -> dict:
    """Deterministic ROI statistics over ALL opaque pixels (same rule as baseline).

    Returns {face: {n, mean, p10, p90, spread}}; no emissive exclusion so the
    numbers are directly comparable to the P25 baseline recorded in WORK_ORDER.
    """
    spec = spec if spec is not None else load_spec()
    asset = spec["assets"][key]
    W, H = im.size
    px = im.load()
    out = {}
    for face, f in asset["faces"].items():
        if not f.get("measure") or not f.get("poly"):
            continue
        poly = f["poly"]
        xs = [p[0] for p in poly]
        ys = [p[1] for p in poly]
        vals = []
        for y in range(max(0, min(ys)), min(H, max(ys) + 1)):
            for x in range(max(0, min(xs)), min(W, max(xs) + 1)):
                if not point_in_poly(poly, x + 0.5, y + 0.5):
                    continue
                r, g, b, a = px[x, y]
                if a < 200:
                    continue
                vals.append(luma(r, g, b))
        if not vals:
            out[face] = None
            continue
        vals.sort()
        n = len(vals)
        p10 = vals[int(n * 0.10)]
        p90 = vals[int(n * 0.90)]
        out[face] = {
            "n": n,
            "mean": round(sum(vals) / n, 2),
            "p10": round(p10, 1),
            "p90": round(p90, 1),
            "spread": round(p90 - p10, 1),
        }
    return out
