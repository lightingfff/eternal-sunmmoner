#!/usr/bin/env python3
"""P25 deterministic ROI / full-frame measurement (auditable text output).

Compares a BASELINE (fresh origin/main b2a7162 engine render + git-HEAD authored
PNGs) against a CANDIDATE (CI-rendered engine PNGs + regenerated authored PNGs).

Everything is deterministic: fixed ROI polygons from roi_definitions.json,
fixed camera props, integer Rec.709 luminance, nearest-neighbour projection
through the real renderer transform (ObliqueCamera.worldToScreenX/Y +
PixelSpriteRenderer.drawCrispSprite bottom-centred blit at 2*s).

Usage:
  python3 measure_rois.py --baseline-assets DIR --candidate-assets DIR \
      --before-engine DIR --after-engine DIR --camera-props FILE \
      --json OUT.json [--text OUT.txt]
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools"))
from p25_face_grammar import load_spec, measure_faces, point_in_poly  # noqa: E402

LR, LG, LB = 2126, 7152, 722
OBLIQUE_Y_SCALE = 0.72
VIEW_W, VIEW_H = 1280, 720

# Which authored asset each camera_props prop id draws, and the exact sprite
# foot offset used by PixelSpriteRenderer for these three facilities.
PROP_TO_ASSET = {
    "building_hero_quarters": "hero_quarters",
    "building_restaurant": "restaurant",
    "building_summon_hall": "summoning_hall",
}
FOOT_OFFSET = {"hero_quarters": 6.0, "restaurant": 6.0, "summoning_hall": 6.0}
# Engine frames whose ROI projection is meaningful and unoccluded enough to report.
ENGINE_FRAMES = [
    "service_cluster_z088.png", "service_cluster_z100.png",
    "quarters_door_actor_z148.png", "restaurant_door_actor_z148.png",
    "summoning_hall_whole_z148.png", "sanctuary_overview_z036.png",
]
P24_KEY_FRAMES = [
    "sanctuary_overview_z036.png", "service_cluster_z088.png", "tower_gate_whole_z148.png",
]


def luma(r, g, b):
    return (LR * r + LG * g + LB * b) / 10000.0


def stats(vals):
    if not vals:
        return None
    vals = sorted(vals)
    n = len(vals)
    p10, p90 = vals[int(n * 0.10)], vals[int(n * 0.90)]
    return {"n": n, "mean": round(sum(vals) / n, 2), "p10": round(p10, 1),
            "p90": round(p90, 1), "spread": round(p90 - p10, 1)}


def full_frame(path: Path):
    im = Image.open(path).convert("RGB")
    px = im.load()
    vals = [luma(*px[x, y]) for y in range(im.height) for x in range(im.width)]
    return stats(vals)


def project_face(engine_png: Path, sprite_png: Path, frame: dict, prop: dict,
                 asset_key: str, poly: list, spec: dict):
    """Nearest-neighbour projection of a sprite-space ROI onto an engine frame."""
    W, H = spec["assets"][asset_key]["canvas"]
    zoom = float(frame["zoom"])
    scale = float(prop["scale"])
    s = zoom * scale
    cx = (float(prop["x"]) - float(frame["camX"])) * zoom + VIEW_W / 2.0
    cy = ((float(prop["y"]) - float(frame["camY"])) * OBLIQUE_Y_SCALE) * zoom + VIEW_H / 2.0
    foot_y = cy + FOOT_OFFSET[asset_key] * s
    pixel_scale = 2.0 * s
    dst_w = max(2, round(W * pixel_scale))
    dst_h = max(2, round(H * pixel_scale))
    left = round(cx - dst_w / 2.0)
    top = round(foot_y - dst_h)
    sx_scale, sy_scale = dst_w / W, dst_h / H

    engine = Image.open(engine_png).convert("RGB")
    ep = engine.load()
    sprite = Image.open(sprite_png).convert("RGBA")
    sp = sprite.load()
    xs = [p[0] for p in poly]
    ys = [p[1] for p in poly]
    # sprite bounding box -> screen bounding box (with 2px margin for rounding)
    x0 = max(0, int(left + min(xs) * sx_scale) - 2)
    x1 = min(engine.width, int(left + (max(xs) + 1) * sx_scale) + 3)
    y0 = max(0, int(top + min(ys) * sy_scale) - 2)
    y1 = min(engine.height, int(top + (max(ys) + 1) * sy_scale) + 3)
    vals = []
    for sy in range(y0, y1):
        for sx in range(x0, x1):
            fx = (sx + 0.5 - left) / sx_scale
            fy = (sy + 0.5 - top) / sy_scale
            if not point_in_poly(poly, fx, fy):
                continue
            ix, iy = int(fx), int(fy)
            if not (0 <= ix < W and 0 <= iy < H) or sp[ix, iy][3] < 200:
                continue
            vals.append(luma(*ep[sx, sy]))
    return stats(vals), {"cx": round(cx, 1), "cy": round(cy, 1), "left": left, "top": top,
                         "dst": [dst_w, dst_h], "screen_box": [x0, y0, x1, y1]}


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--baseline-assets", required=True)
    ap.add_argument("--candidate-assets", required=True)
    ap.add_argument("--before-engine", required=True)
    ap.add_argument("--after-engine", required=True)
    ap.add_argument("--camera-props", required=True)
    ap.add_argument("--json", default=None)
    ap.add_argument("--text", default=None)
    args = ap.parse_args()

    spec = load_spec()
    th = spec["thresholds"]
    ba, ca = Path(args.baseline_assets), Path(args.candidate_assets)
    be, ae = Path(args.before_engine), Path(args.after_engine)
    frames = {f["file"]: f for f in json.loads(Path(args.camera_props).read_text())}
    out = {"thresholds": th, "assets": {}, "full_frame": {}, "engine_roi": {}, "gates": {}}

    # ---- 1) authored PNG ROI stats (the hard B5 gate) ----
    for key, adef in spec["assets"].items():
        b = Image.open(ba / f"{key}.png").convert("RGBA")
        c = Image.open(ca / f"{key}.png").convert("RGBA")
        bm, cm = measure_faces(b, key, spec), measure_faces(c, key, spec)
        checks = {}
        F, S = cm["front_wall"]["mean"], cm["side_wall"]["mean"]
        RK, RR = cm["roof_key_pitch"]["mean"], cm["roof_shaded_return"]["mean"]
        SO, SP = cm["soffit_eave"]["mean"], cm["roof_key_pitch"]["spread"]
        checks["front_minus_side"] = {"before": round(bm["front_wall"]["mean"] - bm["side_wall"]["mean"], 2),
                                      "after": round(F - S, 2), "min": th["front_minus_side_min"]}
        checks["roof_key_minus_return"] = {"before": round(bm["roof_key_pitch"]["mean"] - bm["roof_shaded_return"]["mean"], 2),
                                           "after": round(RK - RR, 2), "min": th["roof_key_minus_return_min"]}
        checks["roof_front_abs"] = {"before": round(abs(bm["roof_key_pitch"]["mean"] - bm["front_wall"]["mean"]), 2),
                                    "after": round(abs(RK - F), 2), "min": th["roof_front_abs_min"]}
        checks["front_minus_soffit"] = {"before": round(bm["front_wall"]["mean"] - bm["soffit_eave"]["mean"], 2),
                                        "after": round(F - SO, 2), "min": th["front_minus_soffit_min"]}
        checks["roof_key_spread"] = {"before": bm["roof_key_pitch"]["spread"],
                                     "after": SP, "min": th["roof_key_spread_min"]}
        if "door_reveal" in cm:
            checks["door_below_front"] = {"before": round(bm["front_wall"]["mean"] - bm["door_reveal"]["mean"], 2),
                                          "after": round(F - cm["door_reveal"]["mean"], 2),
                                          "min": th["door_reveal_max_below_front"]}
        for v in checks.values():
            v["pass"] = v["after"] >= v["min"]
        out["assets"][key] = {"before": bm, "after": cm, "checks": checks,
                              "all_pass": all(v["pass"] for v in checks.values())}

    # ---- 2) full-frame stats on every engine PNG (no-flood + P24 disclosure) ----
    files = sorted(p.name for p in be.glob("*.png"))
    for f in files:
        if not (ae / f).exists():
            continue
        st_b, st_a = full_frame(be / f), full_frame(ae / f)
        dmean = st_a["mean"] - st_b["mean"]
        out["full_frame"][f] = {
            "before": st_b, "after": st_a,
            "d_mean": round(dmean, 2),
            "d_mean_pct": round(100.0 * dmean / st_b["mean"], 2),
            "d_spread": round(st_a["spread"] - st_b["spread"], 1),
        }
    # ---- 3) sprite-space ROI projected onto engine frames (corroboration) ----
    for f in ENGINE_FRAMES:
        if f not in frames or not (be / f).exists() or not (ae / f).exists():
            continue
        fr = frames[f]
        entry = {}
        for prop in fr["props"]:
            key = PROP_TO_ASSET.get(prop["id"])
            if not key:
                continue
            for face, fd in spec["assets"][key]["faces"].items():
                if not fd.get("measure") or not fd.get("poly") or face == "rune_stage":
                    continue
                sb, _ = project_face(be / f, ba / f"{key}.png", fr, prop, key, fd["poly"], spec)
                sa, meta = project_face(ae / f, ca / f"{key}.png", fr, prop, key, fd["poly"], spec)
                if sb and sa:
                    entry[f"{key}:{face}"] = {"before_mean": sb["mean"], "after_mean": sa["mean"],
                                              "before_spread": sb["spread"], "after_spread": sa["spread"],
                                              "n": sa["n"], "projection": meta}
        if entry:
            out["engine_roi"][f] = entry

    # ---- 4) gates ----
    key_frames = [f for f in P24_KEY_FRAMES if f in out["full_frame"]]
    g = out["gates"]
    g["no_flood"] = {
        "frames": {f: out["full_frame"][f]["d_mean_pct"] for f in key_frames},
        "max_abs_mean_pct": max(abs(out["full_frame"][f]["d_mean_pct"]) for f in key_frames),
        "limit": th["no_flood_max_abs_mean_pct"],
    }
    g["no_flood"]["pass"] = g["no_flood"]["max_abs_mean_pct"] < th["no_flood_max_abs_mean_pct"]
    g["max_abs_mean_pct_all_frames"] = max(abs(v["d_mean_pct"]) for v in out["full_frame"].values())
    g["p24_full_frame_contrast"] = {
        "per_frame_d_spread": {f: out["full_frame"][f]["d_spread"] for f in key_frames},
        "gain_at_least_3_count": sum(1 for f in key_frames if out["full_frame"][f]["d_spread"] >= 3),
        "gate": "P24 original: p90-p10 gain >= +3 on >= 2/3 (disclosed independently; NOT relabelled)",
    }
    g["roi"] = {"all_pass": all(a["all_pass"] for a in out["assets"].values()),
                "assets": {k: a["all_pass"] for k, a in out["assets"].items()}}

    if args.json:
        Path(args.json).parent.mkdir(parents=True, exist_ok=True)
        Path(args.json).write_text(json.dumps(out, indent=2) + "\n")
    lines = ["P25 ROI / full-frame measurement (deterministic, nearest-neighbour)",
             f"engine frames: {len(out['full_frame'])} | authored assets: {len(out['assets'])}", ""]
    for k, a in out["assets"].items():
        lines.append(f"[{k}] authored PNG ROI  {'PASS' if a['all_pass'] else 'FAIL'}")
        for name, v in a["checks"].items():
            lines.append(f"  {'PASS' if v['pass'] else 'FAIL'} {name:24s} before={v['before']:7.2f} after={v['after']:7.2f} min={v['min']}")
        lines.append("  face means before -> after: " + ", ".join(
            f"{f}={a['before'][f]['mean']}->{a['after'][f]['mean']}" for f in a["after"]))
        lines.append("")
    lines.append("[full frame, 3 P24 key cameras]")
    for f in key_frames:
        v = out["full_frame"][f]
        lines.append(f"  {f:36s} mean {v['before']['mean']:6.2f}->{v['after']['mean']:6.2f} "
                     f"(Δ{v['d_mean_pct']:+6.2f}%)  spread {v['before']['spread']:5.1f}->{v['after']['spread']:5.1f} (Δ{v['d_spread']:+5.1f})")
    lines.append(f"  max |Δmean| all frames = {g['max_abs_mean_pct_all_frames']:.2f}%  "
                 f"no-flood gate (<{th['no_flood_max_abs_mean_pct']}%) = {'PASS' if g['no_flood']['pass'] else 'FAIL'}")
    lines.append(f"  P24 full-frame p90-p10 gain >=+3 count = "
                 f"{g['p24_full_frame_contrast']['gain_at_least_3_count']}/3 (disclosed, NOT relabelled)")
    lines.append("")
    lines.append("[engine-projected ROI corroboration]")
    for f, faces in out["engine_roi"].items():
        lines.append(f"  {f}")
        for name, v in faces.items():
            lines.append(f"    {name:38s} mean {v['before_mean']:6.2f}->{v['after_mean']:6.2f} "
                         f"spread {v['before_spread']:5.1f}->{v['after_spread']:5.1f} n={v['n']}")
    text = "\n".join(lines) + "\n"
    if args.text:
        Path(args.text).parent.mkdir(parents=True, exist_ok=True)
        Path(args.text).write_text(text)
    print(text)
    return 0 if g["roi"]["all_pass"] and g["no_flood"]["pass"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
