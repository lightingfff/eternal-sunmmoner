#!/usr/bin/env python3
"""P26C render QA against *matched* P26B engine frames, never a sky-gradient proxy.

Run after the Robolectric native renderer has written P26C PNGs. The immutable BEFORE
files in docs/visual-evidence/p26c/engine-render-before/ are copied byte-for-byte
from P26B merged commit f0dc0de (see before_manifest.json for SHA-256 and exact camera
props). STORM did not exist then: no same-weather STORM BEFORE is claimed.

C1 uses luminance DIFFERENCE vs the real same-hour/camera/weather BEFORE image;
counting pixels that differ from the median of the AFTER image falsely includes the
sky gradient, rays and roofs. The denominator remains the entire top 25% sky band,
with the precommitted 5% threshold (NOT relaxed after seeing the first failure).
Two DIFFERENT baselines, because two different questions are being asked:

  * cloud work (C1) is measured against the P26B frames in engine-render-before/,
    because "are the new clouds visible against the old sky" only means something
    against the old sky;
  * world exposure (E4) is measured against the accepted P27B art frames in
    engine-render-art-baseline/, because P27B deliberately made the island brighter
    (landmark luma 46 -> 175). Comparing P27B art to P26B art measured the art change,
    not a flood: on 2026-11-05 E4 failed +19..+21% on all seven pairs with 0.00%
    highlight clipping. Re-seating that baseline keeps E4's real meaning - "the NEXT
    change must not move world exposure by 8%" - instead of relaxing the threshold or
    deleting the gate. See docs/quality-loop/p26c-cloud-storm/e4_rebaseline.json.

Exit 0 only if every automated gate passes; C2 (three-layer appearance), C6 (cull)
and legacy gate G1/G2 require their own review and are not silently auto-passed.
"""

import hashlib
import json
from pathlib import Path
import sys

from PIL import Image, ImageStat

ROOT = Path(__file__).resolve().parents[1]
QA_DIR = ROOT / "docs/quality-loop/p26c-cloud-storm"
EV_DIR = ROOT / "docs/visual-evidence/p26c"
AFTER_DIR = EV_DIR / "engine-render"
BEFORE_DIR = EV_DIR / "engine-render-before"


def mean_luminance(image, box):
    r, g, b = ImageStat.Stat(image.crop(box).convert("RGB")).mean
    return 0.299 * r + 0.587 * g + 0.114 * b


def frame_stats(image, sky_frac):
    w, h = image.size
    return {
        "sky": mean_luminance(image, (0, 0, w, int(h * sky_frac))),
        "world42": mean_luminance(image, (0, int(h * .42), w, h)),
        "whole": mean_luminance(image, (0, 0, w, h)),
    }


def delta_ratio(after, before, sky_frac, threshold=12.0):
    """Fraction of top-band pixels with an absolute Rec.709 AFTER−BEFORE delta >12."""
    if after.size != before.size:
        raise ValueError(f"BEFORE/AFTER dimensions differ: {before.size} vs {after.size}")
    w, h = after.size
    box = (0, 0, w, int(h * sky_frac))
    ap = after.crop(box).convert("RGB").getdata()
    bp = before.crop(box).convert("RGB").getdata()
    changed = 0
    for a, b in zip(ap, bp):
        delta = 0.299 * (a[0] - b[0]) + 0.587 * (a[1] - b[1]) + 0.114 * (a[2] - b[2])
        changed += abs(delta) > threshold
    return changed / (box[2] * box[3])


def main():
    th = json.loads((QA_DIR / "thresholds.json").read_text())
    props = json.loads((EV_DIR / "camera_props.json").read_text())
    baseline = json.loads((EV_DIR / "before_manifest.json").read_text())
    sky_frac = float(th["sky_band_top_fraction"])
    by_name = {p["file"]: p for p in props}
    if len(by_name) != len(props):
        raise ValueError("Duplicate AFTER camera_props filenames")
    if not baseline["matched_pairs"]:
        raise ValueError("No actual matched BEFORE frames: cannot claim C1/E4")

    stats = {}
    for name, p in by_name.items():
        path = AFTER_DIR / name
        with Image.open(path) as image:
            if image.size != (1280, 720):
                raise ValueError(f"Not a full-size 1280x720 engine capture: {path}")
            stats[name] = {"after": frame_stats(image, sky_frac),
                           "hour": p["hour"], "weather": p["weather"]}

    for pair in baseline["matched_pairs"]:
        name = pair["file"]
        if name not in by_name:
            raise ValueError(f"No matched AFTER camera_props: {name}")
        for key, value in pair["camera"].items():
            actual = by_name[name][key]
            if isinstance(value, (int, float)):
                if abs(actual - value) > 0.0001:
                    raise ValueError(f"Unmatched {key} for {name}: {value} vs {actual}")
            elif value != actual:
                raise ValueError(f"Unmatched {key} for {name}: {value} vs {actual}")
        before_path = BEFORE_DIR / name
        data = before_path.read_bytes()
        if hashlib.sha256(data).hexdigest() != pair["sha256"]:
            raise ValueError(f"P26B baseline changed: {before_path}")
        with Image.open(before_path) as before, Image.open(AFTER_DIR / name) as after:
            stats[name]["before"] = frame_stats(before, sky_frac)
            stats[name]["cloud_delta_ratio"] = delta_ratio(after, before, sky_frac)

    def f(name, key):
        return stats[name]["after"][key]

    results = {}
    cloud_th = th["clouds"]
    env_th = th["environment"]

    # C1: actual changed sky pixels against *same* gameplay camera, hour and CLEAR.
    min_ratio = cloud_th["visible_cloud_in_sky_band"]["min_cloud_presence_ratio"]
    c1_names = ("p26c_morning_clear_z088.png", "p26c_noon_clear_z088.png")
    c1 = {n: round(stats[n]["cloud_delta_ratio"], 5) for n in c1_names}
    results["C1"] = (all(v >= min_ratio for v in c1.values()),
                     {"before_after_delta_ratio": c1, "min": min_ratio,
                      "source_commit": baseline["source_commit"]})

    clear = f("p26c_noon_clear_z088.png", "sky")
    cloudy = f("p26c_noon_cloudy_z088.png", "sky")
    rain = f("p26c_noon_rain_z088.png", "sky")
    storm = f("p26c_noon_storm_z088.png", "sky")
    c3_limit = cloud_th["cloudy_denser_than_clear"]["cloudy_sky_le_clear_minus"]
    results["C3"] = (cloudy <= clear - c3_limit,
                     {"clear": round(clear, 2), "cloudy": round(cloudy, 2),
                      "cloudy_minus_clear": round(cloudy - clear, 2)})
    c4_th = cloud_th["storm_cloud_character"]
    results["C4"] = (storm <= clear - c4_th["storm_sky_le_clear_minus"]
                     and storm <= rain - c4_th["storm_sky_le_rain_minus"],
                     {"clear": round(clear, 2), "rain": round(rain, 2),
                      "storm": round(storm, 2), "storm_minus_rain": round(storm - rain, 2),
                      "before_storm": "NOT AVAILABLE: no STORM in P26B"})

    # C5 and E5: native renderer's repeated frame at exactly the same state/time.
    det = (AFTER_DIR / "p26c_noon_clear_z088_det.png").read_bytes()
    original = (AFTER_DIR / "p26c_noon_clear_z088.png").read_bytes()
    results["C5"] = (det == original, {"byte_identical": det == original})

    m = f("p26c_morning_clear_z088.png", "sky")
    e = f("p26c_evening_clear_z088.png", "sky")
    n = f("p26c_night_clear_z088.png", "sky")
    e1 = {"morning_noon": abs(m - clear), "noon_evening": abs(clear - e),
          "evening_night": abs(e - n), "noon_night": abs(clear - n)}
    e1_th = env_th["sky_distinctness"]
    results["E1"] = (e1["morning_noon"] >= e1_th["morning_minus_noon_abs_ge"]
                     and e1["noon_evening"] >= e1_th["noon_minus_evening_abs_ge"]
                     and e1["evening_night"] >= e1_th["evening_minus_night_abs_ge"]
                     and e1["noon_night"] >= e1_th["noon_minus_night_abs_ge"],
                     {k: round(v, 2) for k, v in e1.items()})
    day_night = f("p26c_noon_clear_z088.png", "whole") - f("p26c_night_clear_z088.png", "whole")
    results["E2"] = (clear >= m and day_night >= env_th["noon_brightest"]["noon_frame_mean_minus_night_frame_mean_ge"],
                     {"noon_sky_ge_morning": clear >= m, "whole_noon_minus_night": round(day_night, 2)})
    mood = env_th["weather_mood_noon_service"]
    results["E3"] = (cloudy <= clear - mood["cloudy_sky_le_clear_minus"]
                     and rain <= clear - mood["rain_sky_le_clear_minus"]
                     and storm <= clear - mood["storm_sky_le_clear_minus"]
                     and (not mood["storm_darker_than_rain"] or storm < rain),
                     {"cloudy_minus_clear": round(cloudy - clear, 2),
                      "rain_minus_clear": round(rain - clear, 2),
                      "storm_minus_clear": round(storm - clear, 2)})

    # E4 is NOT a self-comparison. It covers only the 7 states with identical props, and
    # it compares the world band against the ACCEPTED P27B art baseline (see header).
    e4_pairs = {}
    e4_pass = True
    e4_limit = env_th["no_flood_same_hour"]["max_abs_delta_mean_lt_percent"]
    art_baseline = baseline.get("art_baseline") or {}
    if not art_baseline.get("frames"):
        raise ValueError("E4 has no art baseline; run tools/p26c_reseat_baseline.py once "
                         "after an accepted art change (never delete E4 instead)")
    art_means = {row["file"]: row["p27b_world_mean"] for row in art_baseline["frames"]}
    for pair in baseline["matched_pairs"]:
        name = pair["file"]
        if name not in art_means:
            raise ValueError(f"No re-seated art baseline for {name}")
        before_mean = art_means[name]
        after_mean = stats[name]["after"]["world42"]
        pct = 100.0 * (after_mean - before_mean) / max(abs(before_mean), 1.0)
        e4_pairs[name] = {"before": round(before_mean, 2), "after": round(after_mean, 2),
                          "delta_pct": round(pct, 3)}
        if abs(pct) >= e4_limit:
            e4_pass = False
    results["E4"] = (e4_pass, {"matched_only": e4_pairs, "max_abs_pct_lt": e4_limit,
                               "compared_against": "P27B art baseline (re-seated), "
                                                   "not P26B",
                               "no_baseline": "STORM, overview NOON, z1.0/z1.48 bookmarks"})
    results["E5"] = results["C5"]

    report = {
        "source": "REAL Robolectric renderer; AFTER camera_props.json; "
                  "C1 BEFORE = P26B merged SHA; E4 BEFORE = re-seated P27B art baseline",
        "before_source_commit": baseline["source_commit"],
        "sky_band_top_fraction": sky_frac,
        "gates": {k: {"pass": flag, "detail": detail} for k, (flag, detail) in results.items()},
        "frames": {name: {"sky": round(s["after"]["sky"], 2),
                          "cloud_before_after_delta": round(s["cloud_delta_ratio"], 5) if "cloud_delta_ratio" in s else None,
                          "hour": s["hour"], "weather": s["weather"]}
                   for name, s in stats.items()},
        "manual_review": {"C2": "NOT AUTOMATED", "C6": "NOT AUTOMATED",
                          "legacy_G1_G2": "NOT CLAIMED PASS"},
    }
    (QA_DIR / "roi_measurements.json").write_text(json.dumps(report, indent=2) + "\n")
    lines = ["P26C real render comparison to P26B f0dc0de (1280x720, Rec.709)", "=" * 64]
    for gate in ("C1", "C3", "C4", "C5", "E1", "E2", "E3", "E4", "E5"):
        flag, detail = results[gate]
        lines.append(f"{gate}: {'PASS' if flag else 'FAIL'} {detail}")
    passed = sum(flag for flag, _ in results.values())
    lines += ["=" * 64, f"Automated: {passed}/{len(results)} PASS (C2/C6/G1/G2 need separate review)"]
    text = "\n".join(lines) + "\n"
    (QA_DIR / "roi_measurements.txt").write_text(text)
    print(text, end="")
    return 0 if passed == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())
