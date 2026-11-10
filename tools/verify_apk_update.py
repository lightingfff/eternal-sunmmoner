#!/usr/bin/env python3
"""Compare ETERNAL SUMMONER debug APKs without modifying or installing either APK.

Example (Android SDK build-tools must be available):
  python tools/verify_apk_update.py --baseline old.apk --candidate new.apk

Exit 0: project debug-update reference checks passed; 1: incompatible reference;
2: an APK/tool could not be verified. This is deliberately a strict single-signer,
v2-debug guard. APK key rotation is outside this project's current signing policy.
A PASS does not establish that an unknown device has the supplied baseline installed.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile


PACKAGE = "com.aistudio.eternalsummoner.kvrpzm"
ACTIVITY = "com.example.MainActivity"


class AuditError(Exception):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


def find_tool(name: str, explicit: str | None, sdk_root: str | None) -> Path:
    if explicit:
        path = Path(explicit).expanduser().resolve()
        if path.is_file() and os.access(path, os.X_OK):
            return path
        raise AuditError("TOOL_UNAVAILABLE", f"Executable not found: {path}")
    found = shutil.which(name)
    if found:
        return Path(found).resolve()
    sdk = sdk_root or os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if sdk:
        build_tools = Path(sdk).expanduser() / "build-tools"
        if build_tools.is_dir():
            # Stable releases only; a preview tool must be selected explicitly.
            versions = [p for p in build_tools.iterdir() if re.fullmatch(r"\d+(?:\.\d+)+", p.name)]
            versions.sort(key=lambda p: tuple(map(int, p.name.split("."))), reverse=True)
            for version in versions:
                path = version / name
                if path.is_file() and os.access(path, os.X_OK):
                    return path.resolve()
    raise AuditError("TOOL_UNAVAILABLE", f"Cannot find {name}; provide --sdk-root or --{name}.")


def run_tool(argv: list[str], failure_code: str) -> str:
    try:
        result = subprocess.run(argv, capture_output=True, text=True, timeout=90, check=False)
    except (OSError, subprocess.TimeoutExpired) as error:
        raise AuditError(failure_code, str(error)) from error
    if result.returncode:
        detail = (result.stderr or result.stdout).strip()[:1600]
        raise AuditError(failure_code, f"{Path(argv[0]).name} exited {result.returncode}: {detail}")
    return result.stdout


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def inspect_apk(path: Path, apksigner: Path, aapt2: Path) -> dict:
    path = path.expanduser().resolve()
    if not path.is_file():
        raise AuditError("APK_MISSING", f"APK not found: {path}")
    try:
        with zipfile.ZipFile(path) as archive:
            if "AndroidManifest.xml" not in archive.namelist():
                raise AuditError("NOT_AN_APK", f"{path.name} has no root AndroidManifest.xml; extract the APK from its distribution ZIP.")
            bad_entry = archive.testzip()
            if bad_entry:
                raise AuditError("APK_CRC_FAILED", f"{path.name}: CRC failed for {bad_entry}")
    except (OSError, zipfile.BadZipFile, RuntimeError) as error:
        raise AuditError("APK_ZIP_INVALID", f"{path.name}: {error}") from error

    signature = run_tool([str(apksigner), "verify", "--verbose", "--print-certs", str(path)], "SIGNATURE_INVALID")
    badging = run_tool([str(aapt2), "dump", "badging", str(path)], "MANIFEST_INVALID")
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'", badging, re.M)
    activity = re.search(r"^launchable-activity: name='([^']+)'", badging, re.M)
    if not package or not activity:
        raise AuditError("METADATA_MISSING", f"{path.name}: cannot read package/versionCode/launcher.")
    signers = re.findall(r"^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]{64})$", signature, re.M)
    schemes = {name: value == "true" for name, value in re.findall(r"^Verified using (v[\d.]+) scheme[^:]*:\s*(true|false)", signature, re.M)}
    native = re.search(r"^native-code:\s*(.*)$", badging, re.M)
    return {
        "path": str(path),
        "bytes": path.stat().st_size,
        "sha256": sha256_file(path),
        "application_id": package[1],
        "version_code": int(package[2]),
        "version_name": package[3],
        "launchable_activity": activity[1],
        "debuggable": "application-debuggable" in badging.splitlines(),
        "certificate_sha256": sorted(value.lower() for value in signers),
        "signature_schemes": schemes,
        "native_abis": sorted(re.findall(r"'([^']+)'", native[1])) if native else [],
        "signature_verified": True,
        "zip_crc_verified": True,
    }


def compare(baseline: dict, candidate: dict) -> dict:
    profile_supported = all(
        apk["debuggable"] and len(apk["certificate_sha256"]) == 1
        and apk["signature_schemes"].get("v2", False)
        and not apk["signature_schemes"].get("v3", False)
        and not apk["signature_schemes"].get("v3.1", False)
        for apk in (baseline, candidate)
    )
    checks = {
        "project_application_id_preserved": baseline["application_id"] == candidate["application_id"] == PACKAGE,
        "launcher_preserved": baseline["launchable_activity"] == candidate["launchable_activity"] == ACTIVITY,
        "version_not_downgraded": candidate["version_code"] >= baseline["version_code"],
        "strict_v2_debug_profile": profile_supported,
        "signing_certificate_preserved": bool(baseline["certificate_sha256"]) and baseline["certificate_sha256"] == candidate["certificate_sha256"],
        "baseline_native_abis_retained": set(baseline["native_abis"]).issubset(candidate["native_abis"]),
    }
    remedies = {
        "project_application_id_preserved": "Restore the original applicationId; changing it does not repair an update and separates the player's app data.",
        "launcher_preserved": "Restore com.example.MainActivity as the launcher; review manifest merging without replacing the entry point.",
        "version_not_downgraded": "The candidate has a lower versionCode than the reference. Use the intended newer artifact; do not uninstall to bypass this check.",
        "strict_v2_debug_profile": "This guard only supports the project's single-signer v2 debug APKs. Audit release/key-rotation APKs separately.",
        "signing_certificate_preserved": "Signing channels differ. Obtain the candidate signed by the same original key as the installed/reference APK. Do not regenerate keys, change package name, or uninstall and lose saves.",
        "baseline_native_abis_retained": "The candidate omits a native ABI present in the baseline; inspect APK splits and device ABI compatibility.",
    }
    failures = [{"check": key, "action": remedies[key]} for key, passed in checks.items() if not passed]
    return {
        "status": "PASS" if not failures else "INCOMPATIBLE_REFERENCE",
        "scope": "ETERNAL SUMMONER debug APK reference compatibility; no device installation is attempted",
        "device_install_verified": False,
        "baseline": baseline,
        "candidate": candidate,
        "checks": checks,
        "failures": failures,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--baseline", required=True, type=Path)
    parser.add_argument("--candidate", required=True, type=Path)
    parser.add_argument("--sdk-root")
    parser.add_argument("--apksigner")
    parser.add_argument("--aapt2")
    parser.add_argument("--output", type=Path, help="Also save the JSON report to this file.")
    args = parser.parse_args()
    try:
        apksigner = find_tool("apksigner", args.apksigner, args.sdk_root)
        aapt2 = find_tool("aapt2", args.aapt2, args.sdk_root)
        report = compare(inspect_apk(args.baseline, apksigner, aapt2), inspect_apk(args.candidate, apksigner, aapt2))
        exit_code = 0 if report["status"] == "PASS" else 1
    except AuditError as error:
        report = {"status": "ERROR", "code": error.code, "message": str(error), "device_install_verified": False}
        exit_code = 2
    rendered = json.dumps(report, indent=2, ensure_ascii=False) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    sys.stdout.write(rendered)
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
