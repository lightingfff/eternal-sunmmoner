#!/usr/bin/env python3
"""Create a same-key v1+v2 signing-envelope experiment without changing app payload.

The source remains untouched. This does not establish that v1 absence caused an
installer rejection. Only the existing project debug key is accepted; no key is
generated, replaced or included in the output.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile

from verify_apk_update import AuditError, compare, find_tool, inspect_apk, run_tool, sha256_file

APK_NAME = "Eternal-Summoner-compat.apk"
SIGNATURE_ENTRY = re.compile(r"META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))", re.I)


def payload(path: Path) -> dict:
    entries = {}
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)) or archive.testzip() is not None:
            raise ValueError("Duplicate or corrupt APK ZIP entry")
        for entry in archive.infolist():
            if not SIGNATURE_ENTRY.fullmatch(entry.filename):
                entries[entry.filename] = {
                    "sha256": hashlib.sha256(archive.read(entry)).hexdigest(),
                    "bytes": entry.file_size, "compression": entry.compress_type,
                    "offset": entry.header_offset,
                }
    return entries


def verify_payload(source: Path, candidate: Path) -> dict:
    before, after = payload(source), payload(candidate)
    if before.keys() != after.keys():
        raise ValueError("Non-signature APK entry set changed")
    for name in before:
        if any(before[name][key] != after[name][key] for key in ("sha256", "bytes", "compression")):
            raise ValueError(f"App payload or compression changed: {name}")
    return {
        "non_signature_entries": len(before), "payload_bytes_preserved": True,
        "compression_preserved": True, "payload_order_preserved": list(before) == list(after),
        "entries_with_changed_offsets": sum(before[name]["offset"] != after[name]["offset"] for name in before),
    }


def create(source: Path, keystore: Path, sdk: str, output: Path, expected_certificate: str | None = None) -> dict:
    source = source.resolve()
    target = (output / APK_NAME).resolve()
    if target == source or target.exists():
        raise ValueError("Output APK must be new and separate from the source")
    if not keystore.is_file():
        raise ValueError("Original debug keystore is unavailable; no replacement key will be generated")
    signer = find_tool("apksigner", None, sdk)
    aapt2 = find_tool("aapt2", None, sdk)
    aligner = find_tool("zipalign", None, sdk)
    original = inspect_apk(source, signer, aapt2)
    if expected_certificate is not None:
        if not re.fullmatch(r"[0-9a-fA-F]{64}", expected_certificate):
            raise ValueError("Expected certificate must be a SHA256 digest")
        if original["certificate_sha256"] != [expected_certificate.lower()]:
            raise ValueError("Source signer differs from the explicitly expected release certificate")
    if compare(original, original)["status"] != "PASS":
        raise ValueError("Source does not match the project's existing single-signer v2 debug profile")
    output.mkdir(parents=True, exist_ok=True)
    try:
        environment = os.environ.copy()
        environment.setdefault("ES_APK_COMPAT_PASSWORD", "android")
        signed = subprocess.run([
            str(signer), "sign", "--ks", str(keystore.resolve()),
            "--ks-key-alias", "androiddebugkey", "--ks-pass", "env:ES_APK_COMPAT_PASSWORD",
            "--key-pass", "env:ES_APK_COMPAT_PASSWORD", "--v1-signing-enabled", "true",
            "--v2-signing-enabled", "true", "--v3-signing-enabled", "false",
            "--v4-signing-enabled", "false", "--out", str(target), str(source),
        ], env=environment, capture_output=True, text=True, timeout=180, check=False)
        if signed.returncode:
            raise ValueError("Compatibility signing failed; no APK is approved for distribution")
        candidate = inspect_apk(target, signer, aapt2)
        identity = compare(original, candidate)
        if identity["status"] != "PASS":
            raise ValueError("Original signing identity/package/launcher/version/ABIs were not preserved")
        contents = verify_payload(source, target)
        v1 = run_tool([str(signer), "verify", "--verbose", "--min-sdk-version", "23",
                       "--max-sdk-version", "23", str(target)], "V1_VERIFICATION_FAILED")
        if not re.search(r"Verified using v1 scheme[^:]*:\s*true", v1):
            raise ValueError("Independent JAR/v1 verification did not succeed")
        v2 = run_tool([str(signer), "verify", "--verbose", "--min-sdk-version", "24",
                       "--max-sdk-version", "36", str(target)], "V2_VERIFICATION_FAILED")
        run_tool([str(aligner), "-c", "-P", "16", "4", str(target)], "ALIGNMENT_FAILED")
        if sha256_file(source) != original["sha256"]:
            raise ValueError("Source APK changed during the experiment")
        report = {
            "status": "VERIFIED_CANDIDATE", "samsung_failure_fixed": False,
            "scope": "Signing-envelope compatibility experiment; owner-device acceptance unresolved",
            "source": original, "candidate": candidate, "identity_checks": identity["checks"],
            "payload_checks": contents, "v1_verification_api23_only": v1,
            "normal_verification_api24_to36": v2, "zip_alignment_16k": "PASS",
            "min_sdk_note": "API23 bounds select the verifier's JAR/v1 path only; application manifest/SDK requirements are unchanged",
        }
        (output / "compat-report.json").write_text(json.dumps(report, indent=2) + "\n")
        (output / "SHA256SUMS").write_text(f'{candidate["sha256"]}  {APK_NAME}\n')
        return report
    except (AuditError, ValueError, OSError, zipfile.BadZipFile, subprocess.TimeoutExpired):
        if target.is_file():
            target.unlink()
        raise


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--keystore", required=True, type=Path)
    parser.add_argument("--sdk-root", required=True)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--expected-certificate-sha256")
    args = parser.parse_args()
    try:
        print(json.dumps(create(args.apk, args.keystore, args.sdk_root, args.output,
                                args.expected_certificate_sha256), indent=2))
        return 0
    except (AuditError, ValueError, OSError, zipfile.BadZipFile, subprocess.TimeoutExpired) as error:
        print(json.dumps({"status": "ERROR", "samsung_failure_fixed": False, "error": str(error)}, indent=2))
        return 1


if __name__ == "__main__":
    sys.exit(main())
