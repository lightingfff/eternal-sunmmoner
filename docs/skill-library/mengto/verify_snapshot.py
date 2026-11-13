#!/usr/bin/env python3
"""Offline integrity and reference checks; never execute the retained upstream tools."""

from __future__ import annotations

import ast
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parents[2]
UPSTREAM = ROOT / "upstream"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise ValueError(message)


def local_links(doc: Path) -> int:
    count = 0
    for target in re.findall(r"\[[^\]]*\]\(([^)]+)\)", doc.read_text()):
        if target.startswith(("http://", "https://", "mailto:", "#")):
            continue
        target = target.strip("<>").split("#", 1)[0].split("?", 1)[0]
        if any(c in target for c in "<>{}*"):
            continue
        require((doc.parent / target).exists(), f"Broken local link: {doc}: {target}")
        count += 1
    return count


def main() -> None:
    data = json.loads((ROOT / "inventory.json").read_text())
    expected = {row["path"] for row in data["files"]}
    actual = {p.relative_to(UPSTREAM).as_posix() for p in UPSTREAM.rglob("*") if p.is_file()}
    require(len(expected) == len(data["files"]) == 236, "Source file count/duplicates")
    require(expected == actual, f"Source set drift: {expected ^ actual}")
    py_count = json_count = 0
    for row in data["files"]:
        path = UPSTREAM / row["path"]
        require(not path.is_symlink() and path.resolve().is_relative_to(UPSTREAM), f"Unsafe path: {path}")
        content = path.read_bytes()
        require(len(content) == row["bytes"], f"Size drift: {path}")
        require(hashlib.sha256(content).hexdigest() == row["sha256"], f"Hash drift: {path}")
        if path.suffix == ".py":
            ast.parse(content, filename=str(path))
            py_count += 1
        elif path.suffix == ".json":
            json.loads(content)
            json_count += 1
    require((ROOT / "LICENSE").read_bytes() == (UPSTREAM / "LICENSE").read_bytes(), "License drift")
    entries = data["skills"]
    require(len(entries) == len({x["name"] for x in entries}) == 183, "Skill count/duplicates")
    require({x["path"] for x in entries} == {p.relative_to(ROOT).as_posix() for p in UPSTREAM.rglob("SKILL.md")}, "Skill inventory drift")
    require(len(data["selected_folders"]) == 10, "Selected count drift")
    selected_paths = {"agent-skills/" + name for name in data["selected_folders"]}
    selected_actual = {p for p in actual if any(p.startswith(folder + "/") for folder in selected_paths)}
    require(selected_actual == set(data["selected_support_files"]), "Selected support-file drift")
    require(len(selected_actual) == 61, "Selected support count drift")
    source_links = 0
    for entry in entries:
        path = ROOT / entry["path"]
        text = path.read_text()
        require(text.startswith("---\n"), f"Missing header: {path}")
        header = text.split("---", 2)[1]
        require(re.search(r"^name:\s*[\"']?" + re.escape(entry["name"]) + r"[\"']?\s*$", header, re.M) is not None, f"Name mismatch: {path}")
        require(re.search(r"^description:\s*\S", header, re.M) is not None, f"Description missing: {path}")
        if entry["supporting_files_retained"]:
            source_links += local_links(path)
    project_links = sum(local_links(p) for p in [ROOT / "README.md", ROOT / "CATALOG.md",
        PROJECT / ".agents/skills/mengto-game-design/SKILL.md",
        PROJECT / ".agents/skills/eternal-summoner-skill-router/SKILL.md",
        PROJECT / "docs/GAME_SKILL_SELECTION.md"])
    print(json.dumps({"status": "PASS", "source_commit": data["commit"], "source_files": len(actual),
        "skill_entrypoints": len(entries), "selected_skill_support_files": len(selected_actual),
        "python_syntax": py_count, "json_syntax": json_count, "selected_entrypoint_local_links": source_links,
        "project_links": project_links}, indent=2))


if __name__ == "__main__":
    main()
