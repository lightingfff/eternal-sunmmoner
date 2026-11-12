#!/usr/bin/env python3
"""Verify the pinned reference without executing any upstream code."""

from __future__ import annotations

import ast
import hashlib
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parents[2]
UPSTREAM = ROOT / "upstream"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def main() -> None:
    inventory = json.loads((ROOT / "inventory.json").read_text())
    expected = {row["path"] for row in inventory["files"]}
    require(len(expected) == len(inventory["files"]), "Duplicate inventory file")
    actual = {p.relative_to(UPSTREAM).as_posix() for p in UPSTREAM.rglob("*") if p.is_file()}
    require(actual == expected, f"Snapshot set mismatch: {actual ^ expected}")
    python_count = json_count = 0
    for row in inventory["files"]:
        path = UPSTREAM / row["path"]
        require(path.resolve().is_relative_to(UPSTREAM), f"Escaping path: {path}")
        require(not path.is_symlink(), f"Symlink: {path}")
        data = path.read_bytes()
        require(len(data) == row["bytes"], f"Size mismatch: {path}")
        require(hashlib.sha256(data).hexdigest() == row["sha256"], f"Hash mismatch: {path}")
        if path.suffix == ".py":
            ast.parse(data, filename=str(path))
            python_count += 1
        elif path.suffix == ".json":
            json.loads(data)
            json_count += 1
    for filename in ("LICENSE", "NOTICE"):
        require((ROOT / filename).read_bytes() == (UPSTREAM / filename).read_bytes(),
                f"Attribution mismatch: {filename}")

    entries = inventory["skills"]
    require(len(entries) == 80, "Expected 79 specialized skills and one router")
    require(len({entry["name"] for entry in entries}) == 80, "Duplicate skill name")
    found = {p.relative_to(ROOT).as_posix() for p in UPSTREAM.rglob("SKILL.md")}
    require(found == {entry["path"] for entry in entries}, "Skill inventory mismatch")
    require(len(inventory["selected_entrypoints"]) == 10, "Selected review count mismatch")
    resource_count = 0
    for entry in entries:
        path = ROOT / entry["path"]
        body = path.read_text()
        require(body.startswith("---\n"), f"Missing frontmatter: {path}")
        header = body.split("---", 2)[1]
        require(re.search(r"^name:\s*" + re.escape(entry["name"]) + r"\s*$", header, re.M)
                is not None, f"Name mismatch: {path}")
        require(re.search(r"^description:\s*\S", header, re.M) is not None,
                f"Missing description: {path}")
        for resource in re.findall(r"`((?:references|scripts|assets)/[^`\s]+)`", body):
            if any(char in resource for char in "*<>{}"):
                continue  # Dynamic examples do not designate one bundled file.
            require((path.parent / resource).exists(), f"Missing resource: {path}: {resource}")
            resource_count += 1

    docs = [ROOT / "README.md", ROOT / "CATALOG.md",
            PROJECT / ".agents/skills/gamedev-agent-skills/SKILL.md",
            PROJECT / ".agents/skills/eternal-summoner-skill-router/SKILL.md",
            PROJECT / "docs/GAME_SKILL_SELECTION.md"]
    link_count = 0
    for doc in docs:
        for target in re.findall(r"\[[^\]]*\]\(([^)]+)\)", doc.read_text()):
            if target.startswith(("https://", "http://", "mailto:", "#")):
                continue
            target = target.split("#", 1)[0].split("?", 1)[0].strip("<>")
            require((doc.parent / target).exists(), f"Broken link: {doc}: {target}")
            link_count += 1
    print(json.dumps({"status": "PASS", "source_commit": inventory["commit"],
                      "source_files": len(expected), "skill_entrypoints": len(entries),
                      "selected_specialized_entrypoints": 10, "python_syntax": python_count,
                      "json_syntax": json_count, "bundled_resource_paths": resource_count,
                      "project_links": link_count}, indent=2))


if __name__ == "__main__":
    main()
