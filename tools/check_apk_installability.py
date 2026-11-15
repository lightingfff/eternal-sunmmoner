#!/usr/bin/env python3
"""Install an APK on an explicitly selected, fresh Android emulator and retain evidence.

Never uninstalls an application or accepts physical devices. Signature/ZIP checks
are preflight only: PASS requires a real PackageManager install and package path.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import subprocess
import sys

from verify_apk_update import ACTIVITY, PACKAGE, AuditError, find_tool, inspect_apk


class ProbeError(Exception):
    pass


def sdk_tool(sdk: str | Path, name: str) -> Path:
    """Use the producer's known build-tools, not an unrelated executable on PATH."""
    return find_tool(name, str(Path(sdk) / "build-tools" / "36.0.0" / name), str(sdk))


def probe(adb: Path, serial: str, apk: Path, metadata: dict, output: Path,
          timeout: int = 300, runner=subprocess.run) -> dict:
    output.mkdir(parents=True, exist_ok=True)
    journal = []
    report = {
        "status": "ERROR", "installation_verified": False,
        "scope": "Fresh emulator PackageManager install; not a physical-phone test",
        "serial": serial, "apk": metadata, "commands": journal,
    }

    def run(args, limit=30):
        command = [str(adb), "-s", serial, *args]
        result = runner(command, capture_output=True, text=True, timeout=limit, check=False)
        journal.append({"argv": command, "exit_code": result.returncode,
                        "stdout": result.stdout, "stderr": result.stderr})
        return result

    def shell(*args):
        result = run(["shell", *args])
        if result.returncode:
            raise ProbeError(f"Device command failed: {' '.join(args)}: {result.stderr.strip()}")
        return result.stdout.strip()

    try:
        if not serial.startswith("emulator-"):
            raise ProbeError("Only an explicitly named emulator serial is accepted; physical devices are untouched")
        if metadata["application_id"] != PACKAGE or metadata["launchable_activity"] != ACTIVITY:
            raise ProbeError("APK is not the expected project package and launcher")
        qemu = shell("getprop", "ro.kernel.qemu")
        if qemu != "1" and shell("getprop", "ro.boot.qemu") != "1":
            raise ProbeError("Selected target is not confirmed as an emulator")
        if shell("getprop", "sys.boot_completed") != "1":
            raise ProbeError("Emulator boot has not completed; no install attempted")
        report["device"] = {
            "sdk": shell("getprop", "ro.build.version.sdk"),
            "fingerprint": shell("getprop", "ro.build.fingerprint"),
            "abis": shell("getprop", "ro.product.cpu.abilist"),
        }
        existing = run(["shell", "pm", "path", PACKAGE])
        if existing.returncode not in (0, 1) or existing.stderr.strip():
            raise ProbeError("Cannot establish fresh package state; no install attempted")
        if existing.stdout.strip():
            report["status"] = "BLOCKED_NOT_FRESH"
            raise ProbeError("Package already exists on this emulator; it was not removed")
        installed = run(["install", "--no-streaming", str(apk)], timeout)
        (output / "install.txt").write_text(installed.stdout + installed.stderr)
        install_output = installed.stdout + installed.stderr
        if (installed.returncode or "Success" not in installed.stdout.splitlines()
                or re.search(r"\bFailure\b|\bINSTALL_FAILED_", install_output)):
            report["status"] = "INSTALL_REJECTED"
            report["error"] = (installed.stdout + installed.stderr).strip()
            logs = run(["logcat", "-d", "-v", "threadtime", "-t", "2000"], 30)
            (output / "logcat.txt").write_text(logs.stdout + logs.stderr)
            return report
        package_path = shell("pm", "path", PACKAGE)
        if not package_path.startswith("package:") or "base.apk" not in package_path:
            raise ProbeError("Install reported Success but PackageManager does not expose the installed base APK")
        (output / "package.txt").write_text(shell("dumpsys", "package", PACKAGE) + "\n")
        report.update(status="PASS", installation_verified=True, installed_package_path=package_path)
    except (ProbeError, OSError, subprocess.TimeoutExpired) as error:
        report["error"] = str(error)
    finally:
        (output / "report.json").write_text(json.dumps(report, indent=2) + "\n")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--sdk-root", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--timeout", type=int, default=300)
    args = parser.parse_args()
    if not 1 <= args.timeout <= 900:
        parser.error("--timeout must be between 1 and 900 seconds")
    try:
        adb = args.sdk_root / "platform-tools" / "adb"
        if not adb.is_file():
            raise ProbeError(f"ADB not found: {adb}")
        signer = sdk_tool(args.sdk_root, "apksigner")
        aapt2 = sdk_tool(args.sdk_root, "aapt2")
        metadata = inspect_apk(args.apk, signer, aapt2)
        if len(metadata["certificate_sha256"]) != 1:
            raise ProbeError("Verified APK certificate metadata could not be read; no install attempted")
        result = probe(adb, args.serial, args.apk.resolve(), metadata, args.output, args.timeout)
        print(json.dumps(result, indent=2))
        return 0 if result["installation_verified"] else 1
    except (AuditError, ProbeError, OSError) as error:
        print(json.dumps({"status": "ERROR", "installation_verified": False, "error": str(error)}, indent=2))
        return 2


if __name__ == "__main__":
    sys.exit(main())
