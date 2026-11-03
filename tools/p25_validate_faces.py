#!/usr/bin/env python3
"""P25 pre-push validator: geometry/alpha/door/manifest/determinism/ROI gates.

Runs WITHOUT a JDK or Android SDK. It is deliberately a VALIDATOR, not an
authoring tool: it never edits the generator or the assets (except an optional
determinism re-generation that must reproduce byte-identical files).

Usage:
  python3 tools/p25_validate_faces.py --baseline-assets DIR
"""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools"))
from p25_face_grammar import load_spec, measure_faces  # noqa: E402

ASSETS = ROOT / "app/src/main/assets/premium_slice"


def sha(p: Path) -> str:
    return hashlib.sha256(p.read_bytes()).hexdigest()


def run(cmd, **kw):
    return subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True, **kw)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--baseline-assets", required=True)
    ap.add_argument("--skip-determinism", action="store_true")
    args = ap.parse_args()
    baseline = Path(args.baseline_assets)
    spec = load_spec()
    th = spec["thresholds"]
    keys = list(spec["assets"])
    failures, notes = [], []

    def check(cond, msg):
        (notes if cond else failures).append(("PASS" if cond else "FAIL") + " " + msg)
        return cond

    # 1) whitespace / patch hygiene
    r = run(["git", "diff", "--check"])
    check(r.returncode == 0, f"git diff --check (rc={r.returncode})")

    # 2) generator structural validation on the exact pilot set
    py = sys.executable
    r = run([py, "tools/mapforge_p23_architecture.py", "--check", "--only", *keys])
    check(r.returncode == 0, f"generator --check --only {','.join(keys)} (rc={r.returncode})")
    if r.returncode != 0:
        notes.append(r.stderr[-1200:])

    # 3) manifest / PNG integrity
    r = run([py, "tools/validate_assets.py"])
    check(r.returncode == 0 and r.stdout.strip().endswith("PASS"),
          f"validate_assets.py (rc={r.returncode})")

    # 4) geometry, alpha, foot row, door/jamb and protected-region preservation
    for key, adef in spec["assets"].items():
        bp, cp = baseline / f"{key}.png", ASSETS / f"{key}.png"
        b, c = Image.open(bp).convert("RGBA"), Image.open(cp).convert("RGBA")
        check(b.size == c.size == tuple(adef["canvas"]),
              f"{key}: canvas unchanged {b.size} -> {c.size} (expected {tuple(adef['canvas'])})")
        ba = b.getchannel("A").tobytes()
        ca = c.getchannel("A").tobytes()
        check(ba == ca, f"{key}: alpha channel byte-identical (no holes / floating / silhouette drift)")
        # opaque ground row (bottom-most opaque pixel row) must be unchanged
        def bottom(im):
            a = im.getchannel("A")
            w, h = im.size
            px = a.load()
            for y in range(h - 1, -1, -1):
                if any(px[x, y] >= 128 for x in range(w)):
                    return y
            return -1
        check(bottom(b) == bottom(c) == adef["canvas"][1] - 1,
              f"{key}: ground contact row unchanged ({bottom(b)} -> {bottom(c)})")
        # protected rects (door trigger contract, dark portal, rune ring)
        bpix, cpix = b.load(), c.load()
        drift = 0
        for x0, y0, x1, y1 in adef.get("protect", []):
            for y in range(y0, y1):
                for x in range(x0, x1):
                    if bpix[x, y] != cpix[x, y]:
                        drift += 1
        check(drift == 0, f"{key}: {len(adef.get('protect', []))} protected region(s) byte-identical ({drift} drifted px)")

    # 5) ROI thresholds (hard B5, thresholds precommitted before candidate)
    all_roi = True
    for key in keys:
        st = measure_faces(Image.open(ASSETS / f"{key}.png").convert("RGBA"), key, spec)
        F, S = st["front_wall"]["mean"], st["side_wall"]["mean"]
        RK, RR = st["roof_key_pitch"]["mean"], st["roof_shaded_return"]["mean"]
        SO, SP = st["soffit_eave"]["mean"], st["roof_key_pitch"]["spread"]
        checks = {
            "front-side>=12": (F - S, th["front_minus_side_min"]),
            "roofkey-return>=10": (RK - RR, th["roof_key_minus_return_min"]),
            "|roofkey-front|>=10": (abs(RK - F), th["roof_front_abs_min"]),
            "front-soffit>=18": (F - SO, th["front_minus_soffit_min"]),
            "roof spread>=14": (SP, th["roof_key_spread_min"]),
        }
        if "door_reveal" in st:
            checks["door<=front-20"] = (F - st["door_reveal"]["mean"], th["door_reveal_max_below_front"])
        for name, (val, lim) in checks.items():
            ok = val >= lim
            all_roi &= ok
            check(ok, f"{key}: ROI {name} = {val:.2f} (min {lim})")
    check(all_roi, "ALL precommitted ROI face-separation thresholds")

    # 6) deterministic regeneration: two runs must produce byte-identical PNGs
    if not args.skip_determinism:
        before = {k: sha(ASSETS / f"{k}.png") for k in keys}
        r = run([py, "tools/mapforge_p23_architecture.py", "--only", *keys])
        after = {k: sha(ASSETS / f"{k}.png") for k in keys}
        check(r.returncode == 0 and before == after,
              f"deterministic regeneration byte-identical (rc={r.returncode})")

    # 7) manifest SHA agreement for the regenerated pilots
    m = json.loads((ASSETS / "manifest.json").read_text())
    for k in keys:
        e = m["assets"].get(k, {})
        check(e.get("sha256") == sha(ASSETS / f"{k}.png"), f"manifest.json sha256 matches {k}.png")
        check(e.get("size") == list(Image.open(ASSETS / f"{k}.png").size), f"manifest.json size matches {k}.png")

    print("\n".join(notes))
    print(f"\nP25 VALIDATOR: {len(notes)} PASS / {len(failures)} FAIL")
    if failures:
        print("\n".join(failures))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
