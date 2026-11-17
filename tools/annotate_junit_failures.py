#!/usr/bin/env python3
"""Expose Gradle/JUnit failures in GitHub check annotations.

The Actions log/artifact hosts can be unavailable to this review environment. Keep the
failure's test name and message visible through the GitHub checks API instead.
"""

from __future__ import annotations

import argparse
from pathlib import Path
from xml.etree import ElementTree as ET

MAX_DETAIL_CHARS = 8000
ANNOTATION_CHUNK_CHARS = 1200


def escape_workflow_command(text: str) -> str:
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("results_dir", type=Path)
    parser.add_argument("--title", default="JUnit test failure")
    args = parser.parse_args()

    for result_file in sorted(args.results_dir.glob("*.xml")):
        try:
            suite = ET.parse(result_file).getroot()
        except (OSError, ET.ParseError) as error:
            print(
                f"::warning title=Invalid JUnit XML::{escape_workflow_command(f'{result_file.name}: {error}')}"
            )
            continue

        for case in suite.iter():
            if case.tag.rsplit("}", 1)[-1] != "testcase":
                continue
            for result in case:
                if result.tag.rsplit("}", 1)[-1] not in {"failure", "error"}:
                    continue

                detail = result.attrib.get("message", "") or (result.text or "")
                detail = " ".join(detail.split())
                if len(detail) > MAX_DETAIL_CHARS:
                    detail = "...[leading test output truncated]... " + detail[-MAX_DETAIL_CHARS:]
                label = f"{case.attrib.get('classname', result_file.stem)}#{case.attrib.get('name', 'unknown')}"
                message = f"{label}: {detail or 'test failed without a message'}"
                chunks = [
                    message[offset : offset + ANNOTATION_CHUNK_CHARS]
                    for offset in range(0, len(message), ANNOTATION_CHUNK_CHARS)
                ]
                for index, chunk in enumerate(chunks, start=1):
                    title = f"{args.title} [{index}/{len(chunks)}]"
                    print(f"::error title={title}::{escape_workflow_command(chunk)}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
