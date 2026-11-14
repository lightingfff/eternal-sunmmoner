#!/usr/bin/env python3
"""Verify an optional Actions mirror without conflating retrieval and integrity failures."""

from __future__ import annotations

import argparse
from contextlib import redirect_stdout
import io
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import tempfile
import time
import zipfile

from package_apk import APK, PORTABLE, digest, verify


class RetrievalError(RuntimeError):
    pass


class IntegrityError(RuntimeError):
    pass


def verify_payload(archive: Path, reference: Path, destination: Path, apksigner: str) -> None:
    expected = {APK, PORTABLE, "SHA256SUMS"}
    try:
        with zipfile.ZipFile(archive) as downloaded:
            entries = downloaded.infolist()
            if len(entries) != len(expected) or {x.filename for x in entries} != expected:
                raise IntegrityError("Artifact has missing, duplicate or unexpected paths.")
            for entry in entries:
                if (entry.is_dir() or stat.S_IFMT(entry.external_attr >> 16) not in (0, stat.S_IFREG) or
                        entry.flag_bits & 1 or entry.file_size != (reference / entry.filename).stat().st_size):
                    raise IntegrityError("Artifact entry type or size differs from the runner distribution.")
            if downloaded.testzip() is not None:
                raise IntegrityError("Artifact ZIP CRC verification failed.")
            destination.mkdir()
            for entry in entries:
                target = destination / entry.filename
                with downloaded.open(entry) as source, target.open("wb") as output:
                    shutil.copyfileobj(source, output, length=1024 * 1024)
                if digest(target) != digest(reference / entry.filename):
                    raise IntegrityError("Artifact bytes differ from the locally verified distribution.")
        with redirect_stdout(io.StringIO()):
            verify(destination)
    except (OSError, zipfile.BadZipFile, RuntimeError, AssertionError) as error:
        if isinstance(error, IntegrityError):
            raise
        raise IntegrityError("Downloaded ZIP/package verification failed.") from error
    try:
        signed = subprocess.run([apksigner, "verify", "--verbose", str(destination / APK)],
                                capture_output=True, text=True, timeout=60, check=False)
    except (OSError, subprocess.TimeoutExpired) as error:
        raise IntegrityError("Downloaded APK signature verifier could not complete.") from error
    if signed.returncode:
        raise IntegrityError("Downloaded APK signature verification failed.")


def verify_uploaded(repository: str, artifact_id: str, reference: Path, apksigner: str,
                    attempts: int = 3, retry_delay: float = 2) -> dict:
    if (not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9-]*/[A-Za-z0-9_.-]+", repository) or
            repository.rsplit("/", 1)[-1] in {".", ".."}):
        raise RetrievalError("Invalid GitHub repository argument.")
    if not re.fullmatch(r"[1-9][0-9]*", artifact_id):
        raise RetrievalError("Missing or invalid upload artifact ID.")
    if not 1 <= attempts <= 5 or not 0 <= retry_delay <= 5:
        raise RetrievalError("Retry settings exceed the bounded policy.")
    try:
        with redirect_stdout(io.StringIO()):
            verify(reference)
    except (OSError, zipfile.BadZipFile, RuntimeError, AssertionError) as error:
        raise IntegrityError("Local reference distribution verification failed.") from error

    endpoint = f"repos/{repository}/actions/artifacts/{artifact_id}/zip"
    with tempfile.TemporaryDirectory(prefix="apk-artifact-verification-") as scratch:
        archive = Path(scratch) / "artifact.zip"
        for number in range(1, attempts + 1):
            status = None
            try:
                # Truncate each attempt; never treat a partial/error response as an APK ZIP.
                with archive.open("wb") as output:
                    response = subprocess.run(["gh", "api", endpoint], stdout=output,
                        stderr=subprocess.PIPE, timeout=30, check=False,
                        env={**os.environ, "GH_PROMPT_DISABLED": "1"})
                if response.returncode == 0:
                    verify_payload(archive, reference, Path(scratch) / "contents", apksigner)
                    return {"status": "verified", "artifact_id": artifact_id, "attempts": number}
                detail = response.stderr.decode("utf-8", errors="replace")
                match = re.search(r"\bHTTP\s+(\d{3})\b", detail)
                status = int(match[1]) if match else None
                if response.returncode == 4:
                    raise RetrievalError("GitHub CLI authentication is not configured.")
                if status not in {None, 404, 408, 429} and not 500 <= status <= 599:
                    raise RetrievalError(f"Artifact API request rejected (HTTP {status}); check Actions access/configuration.")
                if status is None and any(x in detail.lower() for x in ("gh auth login", "not logged", "authentication")):
                    raise RetrievalError("GitHub CLI authentication is not configured.")
                network_errors = ("could not resolve host", "no such host", "dial tcp", "i/o timeout",
                    "context deadline exceeded", "tls handshake timeout", "connection reset",
                    "connection refused", "unexpected eof", "temporary failure in name resolution",
                    "network is unreachable", "failed to connect")
                if status is None and not any(x in detail.lower() for x in network_errors):
                    raise RetrievalError("Unrecognized GitHub CLI failure; check tool/configuration.")
            except subprocess.TimeoutExpired:
                status = None
            except OSError as error:
                raise RetrievalError("GitHub CLI could not execute.") from error
            archive.unlink(missing_ok=True)
            # A rate limit needs no further pressure; leave the optional mirror unavailable.
            if number == attempts or status == 429:
                return {"status": "unavailable", "artifact_id": artifact_id,
                        "attempts": number, "http_status": status}
            time.sleep(retry_delay * 2 ** (number - 1))
    raise RetrievalError("Artifact verification exited without a result.")


def record(result: dict, report: Path | None) -> None:
    if report is not None:
        report.parent.mkdir(parents=True, exist_ok=True)
        report.write_text(json.dumps(result, indent=2) + "\n")
    status = result["status"]
    if status == "unavailable":
        print(f"::warning::Actions artifact verification UNAVAILABLE after {result['attempts']} attempt(s) "
              f"(HTTP {result.get('http_status') or 'network/timeout'}). Remote bytes were NOT verified; "
              "the previously verified runner APK is unchanged.")
    elif status == "verified":
        print("Artifact ZIP, CRC, local-byte SHA-256 and inner APK signature verified.")
    else:
        print(f"::error::{result['error']}")
    if os.environ.get("GITHUB_OUTPUT"):
        with Path(os.environ["GITHUB_OUTPUT"]).open("a") as output:
            output.write(f"artifact_verification={status}\n")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as summary:
            summary.write(f"\n### Uploaded APK mirror verification: {status.upper()}\n")
            summary.write("```json\n" + json.dumps(result, indent=2) + "\n```\n")
            if status == "unavailable":
                summary.write("Remote verification was not completed. This is an optional retrieval failure, "
                              "not an APK compile/signing result. Main's configured release can still use "
                              "the locally verified runner files; PRs do not publish releases.\n")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--artifact-id", required=True)
    parser.add_argument("--reference-dir", required=True, type=Path)
    parser.add_argument("--apksigner", required=True)
    parser.add_argument("--attempts", type=int, default=3)
    parser.add_argument("--retry-delay", type=float, default=2)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    try:
        result = verify_uploaded(args.repository, args.artifact_id, args.reference_dir,
                                 args.apksigner, args.attempts, args.retry_delay)
    except (RetrievalError, IntegrityError) as error:
        record({"status": "failed", "error": str(error)}, args.report)
        return 1
    record(result, args.report)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
