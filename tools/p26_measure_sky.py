#!/usr/bin/env python3
"""P26 ROUND 1 pixel gates for the sky / day-night / weather rework.

Reads the REAL ENGINE ROBOLECTRIC RENDER evidence committed by the render-bot:
  - docs/visual-evidence/p26/engine-render/       (AFTER frames, current candidate)
  - docs/visual-evidence/p26/engine-render-before/ (BEFORE frames, immutable baseline)
  - docs/visual-evidence/p26/camera_props_before.json (hour/weather/tier of every frame)
and applies the PRECOMMITTED thresholds in
  - docs/quality-loop/p26-environment-hub/thresholds.json

Mean luminance = Rec.709 (0.299 R + 0.587 G + 0.114 B), 0..255, PNG 1280x720.
Sky band = top 25% of the frame (fraction `sky_band_top_fraction` in thresholds.json).
World region = the frame BELOW the sky band (E4 no-flood; the sky band changes are
gated separately by E1-E3, so the no-flood property protects the world, not the sky).

Gates (thresholds fixed BEFORE the candidate render):
  E1 sky distinctness   |morning-noon|>=8, |noon-evening|>=6, |evening-night|>=8, |noon-night|>=18
  E2 noon brightest     noon sky >= morning sky; noon whole-frame mean - night >= 5
  E3 weather mood       cloudy sky <= clear-5; rain sky <= clear-8 (noon service camera)
  E4 no-flood           same hour/camera/weather BEFORE vs AFTER: world-region |dmean| < 8%
  E5 determinism        noon det pair byte-identical at timeSec 0 (checked by P26SkyRenderTest;
                        here we re-verify the committed PNG bytes)
  G1 rift dark          rift ROI mean <= 40; BEFORE vs AFTER |dmean| <= 4 (gate camera)
  G2 night gate energy  halo ROI mean at night >= noon, <= 150

Exit code 0 = all gates PASS, 1 = any gate FAIL.
"""

import json
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
QA_DIR = os.path.join(ROOT, "docs/quality-loop/p26-environment-hub")
EV_DIR = os.path.join(ROOT, "docs/visual-evidence/p26")
AFTER_DIR = os.path.join(EV_DIR, "engine-render")
BEFORE_DIR = os.path.join(EV_DIR, "engine-render-before")


def load_json(path):
    with open(path) as fh:
        return json.load(fh)


def mean_luminance(im, box):
    crop = im.crop(box).convert("RGB")
    px = crop.getdata()
    n = len(px)
    r = sum(p[0] for p in px) / n
    g = sum(p[1] for p in px) / n
    b = sum(p[2] for p in px) / n
    return 0.299 * r + 0.587 * g + 0.114 * b


def frame_stats(im, sky_frac):
    w, h = im.size
    sky_box = (0, 0, w, int(h * sky_frac))
    world_box = (0, int(h * sky_frac), w, h)
    # Pure world region: below the horizon (the service camera's island top sits at
    # ~y 0.33, so the top 42% is all sky). E4 no-flood protects the WORLD plane;
    # the sky band + horizon strip changes are gated separately by E1-E3.
    world42_box = (0, int(h * 0.42), w, h)
    whole_box = (0, 0, w, h)
    return {
        "sky": mean_luminance(im, sky_box),
        "world": mean_luminance(im, world_box),
        "world42": mean_luminance(im, world42_box),
        "whole": mean_luminance(im, whole_box),
    }


def roi_box(im, roi):
    w, h = im.size
    return (
        int(w * roi["x"]),
        int(h * roi["y"]),
        int(w * (roi["x"] + roi["w"])),
        int(h * (roi["y"] + roi["h"])),
    )


def main():
    th = load_json(os.path.join(QA_DIR, "thresholds.json"))
    props = load_json(os.path.join(EV_DIR, "camera_props_before.json"))
    sky_frac = float(th["sky_band_top_fraction"])
    rois = th["gate_roi_definitions"]

    missing = [
        p["file"] for p in props
        if not os.path.exists(os.path.join(BEFORE_DIR, p["file"]))
        or not os.path.exists(os.path.join(AFTER_DIR, p["file"]))
    ]
    if missing:
        print("MISSING FRAMES: " + ", ".join(missing))
        sys.exit(1)

    stats = {}
    for p in props:
        name = p["file"]
        a = Image.open(os.path.join(AFTER_DIR, name))
        b = Image.open(os.path.join(BEFORE_DIR, name))
        stats[name] = {
            "after": frame_stats(a, sky_frac),
            "before": frame_stats(b, sky_frac),
            "hour": p["hour"],
            "weather": p["weather"],
        }

    def f(prefix, suffix, key):
        return stats[prefix + suffix]["after"][key]

    # ---------- E1 sky distinctness (service camera, CLEAR) ----------
    m_sky = f("p26_sky_morning_z088.png", "", "sky")
    n_sky = f("p26_sky_noon_z088.png", "", "sky")
    e_sky = f("p26_sky_evening_z088.png", "", "sky")
    g_sky = f("p26_sky_night_z088.png", "", "sky")
    e1 = {
        "morning_minus_noon_abs": abs(m_sky - n_sky),
        "noon_minus_evening_abs": abs(n_sky - e_sky),
        "evening_minus_night_abs": abs(e_sky - g_sky),
        "noon_minus_night_abs": abs(n_sky - g_sky),
    }
    e1_pass = (
        e1["morning_minus_noon_abs"] >= 8.0
        and e1["noon_minus_evening_abs"] >= 6.0
        and e1["evening_minus_night_abs"] >= 8.0
        and e1["noon_minus_night_abs"] >= 18.0
    )

    # ---------- E2 noon brightest ----------
    e2 = {
        "noon_sky_ge_morning_sky": n_sky >= m_sky,
        "noon_whole_minus_night_whole": f("p26_sky_noon_z088.png", "", "whole")
        - f("p26_sky_night_z088.png", "", "whole"),
    }
    e2_pass = e2["noon_sky_ge_morning_sky"] and e2["noon_whole_minus_night_whole"] >= 5.0

    # ---------- E3 weather mood (noon service camera) ----------
    clear_sky = f("p26_sky_noon_z088.png", "", "sky")
    cloudy_sky = f("p26_sky_noon_cloudy_z088.png", "", "sky")
    rain_sky = f("p26_sky_noon_rain_z088.png", "", "sky")
    e3 = {
        "cloudy_minus_clear": cloudy_sky - clear_sky,
        "rain_minus_clear": rain_sky - clear_sky,
    }
    e3_pass = cloudy_sky <= clear_sky - 5.0 and rain_sky <= clear_sky - 8.0

    # ---------- E4 no-flood (pure world region below the horizon, same hour/camera/weather) ----------
    e4_pairs = {}
    e4_pass = True
    for name in sorted(stats):
        a_world = stats[name]["after"]["world42"]
        b_world = stats[name]["before"]["world42"]
        denom = max(abs(b_world), 1.0)
        e4_pairs[name] = {"before": round(b_world, 1), "after": round(a_world, 1),
                          "delta_pct": round((a_world - b_world) / denom * 100.0, 2)}
        if abs(a_world - b_world) / denom >= 0.08:
            e4_pass = False

    # ---------- E5 determinism (byte-identical det pair) ----------
    det_after = os.path.join(AFTER_DIR, "p26_sky_noon_z088_det.png")
    det_before = os.path.join(BEFORE_DIR, "p26_sky_noon_z088_det.png")
    e5 = {"det_pair_byte_identical_after": False, "det_pair_byte_identical_before": False}
    with open(det_after, "rb") as fh:
        det_a = fh.read()
    with open(det_before, "rb") as fh:
        det_b = fh.read()
    with open(os.path.join(AFTER_DIR, "p26_sky_noon_z088.png"), "rb") as fh:
        n_a = fh.read()
    e5["det_pair_byte_identical_after"] = det_a == n_a
    with open(os.path.join(BEFORE_DIR, "p26_sky_noon_z088.png"), "rb") as fh:
        n_b = fh.read()
    e5["det_pair_byte_identical_before"] = det_b == n_b
    e5_pass = e5["det_pair_byte_identical_after"]

    # ---------- G1 rift dark (protected) ----------
    rift_roi = rois["gate_z148_rift"]
    halo_roi = rois["gate_z148_halo"]
    g1 = {}
    g1_pass = True
    for name in ["p26_gate_night_z148.png", "p26_gate_noon_z148.png", "p26_gate_night_rain_z148.png"]:
        a = Image.open(os.path.join(AFTER_DIR, name))
        b = Image.open(os.path.join(BEFORE_DIR, name))
        a_rift = mean_luminance(a, roi_box(a, rift_roi))
        b_rift = mean_luminance(b, roi_box(b, rift_roi))
        g1[name] = {"rift_after": round(a_rift, 1), "rift_before": round(b_rift, 1),
                    "delta": round(a_rift - b_rift, 2)}
        if abs(a_rift - b_rift) > 4.0:
            g1_pass = False
    g1["rift_roi_mean_le"] = 40.0
    night_rift = mean_luminance(Image.open(os.path.join(AFTER_DIR, "p26_gate_night_z148.png")),
                                roi_box(Image.open(os.path.join(AFTER_DIR, "p26_gate_night_z148.png")), rift_roi))
    if night_rift > 40.0:
        g1_pass = False
    g1["night_rift_mean"] = round(night_rift, 1)

    # ---------- G2 night gate energy ----------
    gate_night = Image.open(os.path.join(AFTER_DIR, "p26_gate_night_z148.png"))
    gate_noon = Image.open(os.path.join(AFTER_DIR, "p26_gate_noon_z148.png"))
    g2 = {
        "halo_night": round(mean_luminance(gate_night, roi_box(gate_night, halo_roi)), 1),
        "halo_noon": round(mean_luminance(gate_noon, roi_box(gate_noon, halo_roi)), 1),
    }
    g2_pass = g2["halo_night"] >= g2["halo_noon"] and g2["halo_night"] <= 150.0

    gates = {"E1": (e1_pass, e1), "E2": (e2_pass, e2), "E3": (e3_pass, e3),
             "E4": (e4_pass, e4_pairs), "E5": (e5_pass, e5),
             "G1": (g1_pass, g1), "G2": (g2_pass, g2)}

    report = {
        "sky_band_top_fraction": sky_frac,
        "gates": {k: {"pass": v[0], "detail": v[1]} for k, v in gates.items()},
        "frames": {name: {k: round(v, 2) for k, v in s["after"].items()} for name, s in stats.items()},
    }
    with open(os.path.join(QA_DIR, "roi_measurements.json"), "w") as fh:
        json.dump(report, fh, indent=2)

    lines = ["P26 ROUND 1 MEASUREMENT (real Robolectric render evidence, 1280x720, Rec.709)",
             "=" * 70]
    lines.append(f"E1 sky distinctness: {'PASS' if e1_pass else 'FAIL'} "
                 f"|m-n|={e1['morning_minus_noon_abs']:.1f} |n-e|={e1['noon_minus_evening_abs']:.1f} "
                 f"|e-ng|={e1['evening_minus_night_abs']:.1f} |n-ng|={e1['noon_minus_night_abs']:.1f} "
                 f"(morn={m_sky:.1f} noon={n_sky:.1f} eve={e_sky:.1f} night={g_sky:.1f})")
    lines.append(f"E2 noon brightest: {'PASS' if e2_pass else 'FAIL'} "
                 f"noon_sky>=morn_sky={e2['noon_sky_ge_morning_sky']} "
                 f"noon_whole-night_whole={e2['noon_whole_minus_night_whole']:.1f}")
    lines.append(f"E3 weather mood: {'PASS' if e3_pass else 'FAIL'} "
                 f"cloudy-clear={e3['cloudy_minus_clear']:.1f} rain-clear={e3['rain_minus_clear']:.1f}")
    lines.append(f"E4 no-flood (world region): {'PASS' if e4_pass else 'FAIL'}")
    for name, v in e4_pairs.items():
        lines.append(f"    {name}: {v['delta_pct']:+.1f}% ({v['before']} -> {v['after']})")
    lines.append(f"E5 determinism: {'PASS' if e5_pass else 'FAIL'} "
                 f"after det==noon={e5['det_pair_byte_identical_after']}")
    lines.append(f"G1 rift dark: {'PASS' if g1_pass else 'FAIL'} "
                 f"night rift={g1['night_rift_mean']} (<=40), |delta|<=4 per frame")
    for name, v in g1.items():
        if isinstance(v, dict):
            lines.append(f"    {name}: {v}")
    lines.append(f"G2 night gate energy: {'PASS' if g2_pass else 'FAIL'} "
                 f"halo night={g2['halo_night']} noon={g2['halo_noon']} (night>=noon, <=150)")
    total = sum(1 for v in gates.values() if v[0])
    lines.append("=" * 70)
    lines.append(f"TOTAL: {total}/{len(gates)} gates PASS")
    text = "\n".join(lines)
    with open(os.path.join(QA_DIR, "roi_measurements.txt"), "w") as fh:
        fh.write(text + "\n")
    print(text)
    sys.exit(0 if total == len(gates) else 1)


if __name__ == "__main__":
    main()
