#!/usr/bin/env python3
"""P24 evidence helper — measure REAL engine render PNGs (not contact sheets).

Requires Pillow (`python3 -m venv /tmp/qa-venv && /tmp/qa-venv/bin/pip install pillow`).
This script is NOT part of the Android build and does NOT run in CI; it is a local
inspection tool for the WORK_ORDER acceptance gate.

Usage:
  /tmp/qa-venv/bin/python docs/quality-loop/p24-sanctuary-hdr/measure_luminance.py \
      docs/visual-evidence/p24/engine-render-before \
      docs/visual-evidence/p23/engine-render

It reports, per image and as a before -> after delta:
  mean      : global luminance (gate: |delta|/before < 8% => not just brighter)
  p10 / p90 : shadow / highlight percentiles
  p90-p10   : LOCAL contrast (gate: increase >= 3.0 on >= 2 of 3 key frames)
  dark<20   : crushed-shadow pixel share (must not grow much)
  hi>150    : controlled-highlight pixel share (should grow modestly)
"""
import os
import sys
from PIL import Image

KEY_FRAMES = (
    "service_cluster_z088.png",
    "tower_gate_whole_z148.png",
    "sanctuary_overview_z036.png",
)


def measure(path):
    im = Image.open(path).convert("RGB")
    px = list(im.getdata())
    lum = sorted(0.2126 * r + 0.7152 * g + 0.0722 * b for r, g, b in px)
    n = len(lum)
    mean = sum(lum) / n
    var = sum((v - mean) ** 2 for v in lum) / n
    return {
        "mean": mean,
        "p10": lum[int(n * 0.10)],
        "p90": lum[int(n * 0.90)],
        "spread": lum[int(n * 0.90)] - lum[int(n * 0.10)],
        "std": var ** 0.5,
        "dark": sum(1 for v in lum if v < 20) / n,
        "hi": sum(1 for v in lum if v > 150) / n,
    }


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    before_dir, after_dir = sys.argv[1], sys.argv[2]
    names = sorted(f for f in os.listdir(before_dir) if f.endswith(".png"))
    print(f"{'frame':44s} {'mean b->a':>16s} {'dMean%':>8s} {'spread b->a':>16s} {'dSpread':>8s} {'dark%b/a':>12s} {'hi%b/a':>12s}")
    gains = []
    for name in names:
        ap = os.path.join(after_dir, name)
        if not os.path.exists(ap):
            continue
        b, a = measure(os.path.join(before_dir, name)), measure(ap)
        dmean = 100.0 * (a["mean"] - b["mean"]) / b["mean"] if b["mean"] else 0.0
        dspread = a["spread"] - b["spread"]
        if name in KEY_FRAMES:
            gains.append((name, dmean, dspread))
        print(f"{name:44s} {b['mean']:7.2f}->{a['mean']:7.2f} {dmean:+8.2f} "
              f"{b['spread']:7.1f}->{a['spread']:7.1f} {dspread:+8.1f} "
              f"{100*b['dark']:5.2f}/{100*a['dark']:5.2f} "
              f"{100*b['hi']:5.2f}/{100*a['hi']:5.2f}")
    print()
    print("GATES on key frames:")
    for name, dmean, dspread in gains:
        print(f"  {name:40s} |dMean| {abs(dmean):5.2f}% (<8)  dSpread {dspread:+5.1f} (>=3)")
    if gains:
        no_flood = sum(1 for _, d, _ in gains if abs(d) < 8.0)
        contrast = sum(1 for _, _, s in gains if s >= 3.0)
        print(f"  no-flood {no_flood}/{len(gains)}   contrast-gain {contrast}/{len(gains)} (need >=2/3)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
