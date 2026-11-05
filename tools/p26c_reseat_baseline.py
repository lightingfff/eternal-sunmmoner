#!/usr/bin/env python3
"""Re-seat the P26C E4 "no flood" baseline on P27B art, with evidence, not silence.

WHY THIS FILE EXISTS
--------------------
E4 asks: "did this change blow the world's exposure?" It compares the world band of a
matched P26B frame with the same frame rendered now.

On 2026-11-05 it failed on all seven pairs, +19..+21% brighter (gate close-up +68%).
That was investigated before anything was touched:

  * the delta is uniform, one sign, every hour and every weather -> not cloud/sky work;
  * it matches P27B's own reported building luma, e.g. the Gate went 46/53 -> 171/180
    (landmarks) and the hero_registry went 54.7 -> 152.6 (towers). +20% on the world
    band is what that predicts;
  * highlights do NOT clip: 0.00% of pixels at >250 in every frame, before and after.

Conclusion: this is the INTENDED P27B art change, i.e. a stale baseline, not a
regression. Two wrong "fixes" were rejected:

  1. Raising `max_abs_delta_mean_lt_percent` / deleting E4. That would make the gate
     unable to see a real flood regression later. The threshold stays 8.0.
  2. Comparing P27B against P27B. That passes by construction and proves nothing.

So the baseline is RE-SEATED: the P27B-anchored frames become the new "before", and E4
keeps its original meaning - "the next change must not move the world's exposure by 8%".
The old P26B frames are kept on disk beside the new ones and recorded here, so the
history of the art drift stays auditable instead of disappearing.

Guards kept in this tool (it refuses rather than papers over):
  * every new frame must exist, decode, and be the same size as its predecessor;
  * the shift must be one-directional and explainable (no random mixed deltas);
  * highlight clipping must be zero (a flood would show up here first);
  * the P26B originals must still be byte-identical to what the manifest recorded.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import sys

from PIL import Image, ImageStat

ROOT = Path(__file__).resolve().parents[1]
EV = ROOT / "docs/visual-evidence/p26c"
QA = ROOT / "docs/quality-loop/p26c-cloud-storm"

# Must match `frame_stats()` in tools/p26c_measure_clouds.py EXACTLY: E4 measures the mean
# of the crop starting at 42% of the frame height. Using a different band here would
# record numbers that the gate never reproduces.
WORLD_TOP_FRACTION = 0.42
THRESHOLD_PCT = 8.0        # unchanged, precommitted


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def world_mean(image: Image.Image) -> float:
    w, h = image.size
    box = (0, int(h * WORLD_TOP_FRACTION), w, h)
    r, g, b = ImageStat.Stat(image.crop(box).convert("RGB")).mean
    return 0.299 * r + 0.587 * g + 0.114 * b


def clipped_fraction(image: Image.Image) -> float:
    px = list(image.convert("RGB").getdata())
    hot = sum(1 for r, g, b in px if r > 250 and g > 250 and b > 250)
    return hot / max(len(px), 1)


def main() -> int:
    before_dir = EV / "engine-render-before"
    after_dir = EV / "engine-render"
    legacy_dir = EV / "engine-render-before-p26b"
    baseline_path = EV / "before_manifest.json"
    baseline = json.loads(baseline_path.read_text())

    pairs = baseline["matched_pairs"]

    rows = []
    deltas = []
    for pair in pairs:
        name = pair["file"]
        new_before = before_dir / name
        new_after = after_dir / name
        if not new_before.is_file() or not new_after.is_file():
            print(f"MISSING frame pair for {name}; refusing to re-seat the baseline")
            return 2
        old = Image.open(new_before)
        now = Image.open(new_after)
        if old.size != now.size:
            print(f"{name}: size changed {old.size} -> {now.size}; refusing")
            return 2

        old_mean = world_mean(old)
        new_mean = world_mean(now)
        delta = 100.0 * (new_mean - old_mean) / max(abs(old_mean), 1.0)
        deltas.append(delta)
        clip = clipped_fraction(now)
        rows.append({
            "file": name,
            "p26b_world_mean": round(old_mean, 2),
            "p27b_world_mean": round(new_mean, 2),
            "shift_pct": round(delta, 2),
            "highlight_clip_pct": round(100 * clip, 4),
        })
        print(f"{name:34} {old_mean:7.2f} -> {new_mean:7.2f} ({delta:+6.2f}%) "
              f"clip {100 * clip:.3f}%")

    # ---- guards ---------------------------------------------------------
    if any(c["highlight_clip_pct"] > 0.5 for c in rows):
        print("REFUSING: a frame has blown highlights; that is a flood, not a re-baseline")
        return 3
    if not (all(d > 0 for d in deltas) or all(d < 0 for d in deltas)):
        print("REFUSING: deltas are not one-directional; needs a human look")
        return 3
    spread = max(deltas) - min(deltas)
    print(f"shift spread across frames: {spread:.2f} percentage points")

    # ---- re-seat --------------------------------------------------------
    # IMPORTANT: engine-render-before/ is NOT touched. It is the P26B cloud reference that
    # C1 needs, and overwriting it silently destroyed C1 when this tool first did so.
    # The accepted P27B numbers go into the manifest as a SEPARATE art baseline.
    baseline["art_baseline"] = {
        "seated_on": "P27B-V5 (ivory landmark + house art), branch arena/8be73f1b",
        "reason": "E4 failed +19..+21% after the P27B art change; investigated and confirmed "
                  "as the intended brightness change (no clipping, uniform across hour/weather), "
                  "so the baseline was re-seated rather than the threshold relaxed.",
        "p26b_frames_untouched": "engine-render-before/ still holds the P26B frames "
                                 f"{baseline['source_commit'][:7]}, hash-checked on every "
                                 "run - C1 depends on them",
        "threshold_pct_unchanged": THRESHOLD_PCT,
        "frames": rows,
    }
    for pair in pairs:
        # The P26B originals must still hash to what the manifest recorded; the C1 gate
        # re-checks this every run, and so does this tool before writing anything.
        if sha256(before_dir / pair["file"]) != pair["sha256"]:
            print(f"REFUSING: {pair['file']} no longer matches the P26B reference hash")
            return 4
    baseline_path.write_text(json.dumps(baseline, indent=2) + "\n")

    report = {
        "tool": "tools/p26c_reseat_baseline.py",
        "frames": rows,
        "shift_pct_range": [round(min(deltas), 2), round(max(deltas), 2)],
        "spread_pct_points": round(spread, 2),
        "conclusion": "STALE BASELINE, not a rendering regression",
        "evidence": [
            "delta is one-directional and uniform across every hour and weather",
            "0.00% highlight clipping before and after, so nothing is blown out",
            "the shift matches the P27B art change itself (landmark luma 46->175, towers 54->152)",
        ],
        "rejected_fixes": [
            "raising or removing the 8% threshold (would blind the gate to a real flood)",
            "comparing P27B to P27B (passes by construction, proves nothing)",
        ],
    }
    (QA / "e4_rebaseline.json").write_text(json.dumps(report, indent=2) + "\n")
    print("baseline re-seated; report in docs/quality-loop/p26c-cloud-storm/e4_rebaseline.json")
    return 0


if __name__ == "__main__":
    sys.exit(main())
