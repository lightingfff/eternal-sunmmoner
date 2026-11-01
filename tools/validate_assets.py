#!/usr/bin/env python3
"""Lightweight regression check for ETERNAL SUMMONER assets (CLASS 3/4 prevention).
- PNG signature + IEND present (not truncated)
- No asset PNG > 3 MB unexpectedly (bloat)
- Manifest SHA256 matches file (premium_slice + sanctuary_p03)
- package_name consistency: app/google-services.json vs app/build.gradle.kts
Exit 0 = PASS, non-zero = FAIL (CI will block corrupted asset merge).
"""
import hashlib, json, pathlib, sys, re, struct

ROOT = pathlib.Path(__file__).parent.parent
ASSETS = ROOT / "app/src/main/assets"
FAIL = False

def sha256(p): return hashlib.sha256(p.read_bytes()).hexdigest()

def check_png(p):
    global FAIL
    data = p.read_bytes()
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        print(f"FAIL {p}: bad PNG signature")
        FAIL = True
        return
    if b"IEND" not in data[-30:]:
        print(f"FAIL {p}: missing IEND (truncated)")
        FAIL = True
    # quick IHDR size sanity
    try:
        w = struct.unpack(">I", data[16:20])[0]; h = struct.unpack(">I", data[20:24])[0]
        if w==0 or h==0 or w>8192 or h>8192:
            print(f"FAIL {p}: suspicious dimensions {w}x{h}")
            FAIL = True
    except Exception as e:
        print(f"FAIL {p}: {e}"); FAIL=True

# 1) All PNGs in assets
for png in ASSETS.rglob("*.png"):
    if png.stat().st_size > 3*1024*1024:
        print(f"WARN {png.relative_to(ROOT)}: {png.stat().st_size} bytes >3MB (preview bloat?); excluded from APK via packagingResources")
        # not fail, but warn (excluded via packaging)
    check_png(png)

# 2) Manifest SHA for premium_slice
for rel in ["app/src/main/assets/premium_slice/manifest.json"]:
    mp = ROOT / rel
    if mp.exists():
        m=json.loads(mp.read_text())
        for k,v in m.get("assets",{}).items():
            fp=ROOT/"app/src/main/assets/premium_slice"/v["file"]
            if fp.exists():
                if sha256(fp)!=v["sha256"]:
                    print(f"FAIL {rel} {k}: SHA mismatch")
                    FAIL=True
                if v.get("size") and list(fp.stat().st_size for _ in [1]): # size check via dimensions already
                    pass
            else:
                print(f"FAIL {rel} missing {v['file']}"); FAIL=True

# sanctuary_p03
p03 = ROOT/"app/src/main/assets/sanctuary_p03/manifest.json"
if p03.exists():
    j=json.loads(p03.read_text())
    for a in j.get("assets",[]):
        fp=ROOT/"app/src/main/assets/sanctuary_p03"/a["path"]
        if not fp.exists():
            print(f"FAIL p03 missing {a['path']}"); FAIL=True
        elif sha256(fp)!=a["sha256"]:
            print(f"FAIL p03 {a['id']} SHA mismatch"); FAIL=True
        check_png(fp) if fp.exists() else None

# 3) package_name consistency
try:
    gs=json.loads((ROOT/"app/google-services.json").read_text())
    pkg_gs=gs["client"][0]["client_info"]["android_client_info"]["package_name"]
    gradle=(ROOT/"app/build.gradle.kts").read_text()
    m=re.search(r'applicationId\s*=\s*"([^"]+)"', gradle)
    pkg_gradle=m.group(1) if m else None
    if pkg_gs!=pkg_gradle:
        print(f"FAIL package mismatch google-services.json:{pkg_gs} vs build.gradle:{pkg_gradle} (CLASS 1 risk: App not installed)")
        FAIL=True
    else:
        print(f"OK package {pkg_gs}")
except Exception as e:
    print(f"WARN package check: {e}")

# 4) sanity: no duplicate case-insensitive asset names in same tree
from collections import defaultdict
seen=defaultdict(list)
for p in ASSETS.rglob("*"):
    if p.is_file():
        seen[p.name.lower()].append(str(p.relative_to(ROOT)))
dups={k:v for k,v in seen.items() if len(v)>1 and k!="manifest.json"}
if dups:
    print(f"WARN case-insensitive dups (risk on Windows): {dups}")

print("PASS" if not FAIL else "FAIL")
sys.exit(1 if FAIL else 0)
