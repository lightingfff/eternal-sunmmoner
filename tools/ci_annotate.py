#!/usr/bin/env python3
"""Print a log file as a GitHub check annotation.

Why: the Actions log blob hosts are unreachable from this repo's review environment, so a
failure that only exists in the raw log is effectively invisible. The run summary is not
enough either, because a summary is read only if someone opens the run - an annotation
shows up on the PR's Files/Checks view.

Usage: ci_annotate.py <logfile> [--title TEXT] [--max-bytes N]

Keeps the TAIL of the log (the part that holds the cause) and escapes the three
characters the ::error:: command syntax treats specially.
"""

from __future__ import annotations

import argparse
from pathlib import Path


def escape(text: str) -> str:
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("logfile")
    parser.add_argument("--title", default="CI failure diagnostics")
    parser.add_argument("--max-bytes", type=int, default=5000)
    args = parser.parse_args()

    path = Path(args.logfile)
    if not path.is_file():
        print(f"::warning title={args.title}::no log at {path}")
        return 0

    text = path.read_text(errors="replace")
    if not text.strip():
        print(f"::warning title={args.title}::log at {path} is empty")
        return 0

    print(f"::error title={args.title}::{escape(text[-args.max_bytes:])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
