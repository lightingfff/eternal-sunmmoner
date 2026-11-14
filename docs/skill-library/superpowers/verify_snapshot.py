#!/usr/bin/env python3
"""Read-only verification of the pinned, inactive Superpowers reference snapshot."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import stat
import subprocess
from urllib.parse import unquote, urlsplit

LIBRARY = Path(__file__).resolve().parent
ROOT = LIBRARY.parents[2]
PIN = "22f0b463e7a5e579904dd6211a520321035526f7"
EXTRAS = {
    "README.md", "LICENSE", "package.json", ".codex-plugin/plugin.json",
    "scripts/package-codex-plugin.sh", "scripts/sync-to-codex-plugin.sh",
}
ROUTER = ".agents/skills/eternal-summoner-skill-router/SKILL.md"
ADAPTER = ".agents/skills/superpowers-eternal/SKILL.md"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def git(directory, *args):
    return subprocess.check_output(["git", "-C", str(directory), *args], text=True)


def selected(paths):
    return {p for p in paths if p.startswith(("skills/", "hooks/")) or p in EXTRAS}


def verify(upstream=None, base=None):
    meta = json.loads((LIBRARY / "inventory.json").read_text())
    require(meta["commit"] == PIN and meta["version"] == "7.0.2", "Unexpected pin/version")
    require(meta["activation"] == "INACTIVE_REFERENCE_ONLY", "Unexpected activation mode")
    archive = LIBRARY / "upstream"
    paths = set()
    total = 0
    for entry in meta["sourceFiles"]:
        relative = PurePosixPath(entry["path"])
        require(not relative.is_absolute() and ".." not in relative.parts, "Unsafe source path")
        require(str(relative) not in paths, "Duplicate source path")
        paths.add(str(relative))
        path = archive / relative
        require(path.is_file() and not path.is_symlink(), f"Missing/nonregular source: {relative}")
        require(not path.stat().st_mode & (stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH),
                f"Reference file has executable permission: {relative}")
        data = path.read_bytes()
        require(len(data) == entry["bytes"], f"Size mismatch: {relative}")
        require(hashlib.sha256(data).hexdigest() == entry["sha256"], f"Hash mismatch: {relative}")
        total += len(data)
    actual = {str(p.relative_to(archive)) for p in archive.rglob("*") if p.is_file()}
    require(paths == actual == selected(paths), "Source inventory/selection mismatch")
    require(len(paths) == 90 and total == 487_203, "Unexpected source coverage")
    require(len([p for p in paths if p.startswith("skills/")]) == 80, "Missing skill support")
    license_text = (archive / "LICENSE").read_text()
    require("MIT License" in license_text and "Copyright (c) 2025 Jesse Vincent" in license_text,
            "Missing source license/copyright")
    for source in ("package.json", ".codex-plugin/plugin.json"):
        require(json.loads((archive / source).read_text())["version"] == "7.0.2", "Version mismatch")
    skills = meta["skills"]
    mains = {p for p in paths if re.fullmatch(r"skills/[^/]+/SKILL\.md", p)}
    require(len(skills) == 15 and {s["path"] for s in skills} == mains, "Missing skill main text")
    catalog = (LIBRARY / "CATALOG.md").read_text()
    for skill in skills:
        require(f'[{skill["name"]}](upstream/{skill["path"]})' in catalog,
                f'Missing catalog entry: {skill["name"]}')
    authored = [LIBRARY / name for name in ("README.md", "CATALOG.md", "READING_NOTES.md")]
    authored.append(ROOT / ADAPTER)
    link_count = 0
    for path in authored:
        for target in re.findall(r"\[[^\]]*\]\(([^\s)]+)\)", path.read_text()):
            url = urlsplit(target)
            if url.scheme or url.netloc or not url.path:
                continue
            linked = (path.parent / unquote(url.path)).resolve()
            require(linked.is_relative_to(ROOT) and linked.exists(), f"Broken local link: {path}: {target}")
            link_count += 1
    require("../superpowers-eternal/SKILL.md" in (ROOT / ROUTER).read_text(), "Missing router link")
    adapter = (ROOT / ADAPTER).read_text()
    require(adapter.startswith("---\nname: superpowers-eternal\ndescription: Use when"),
            "Invalid adapter frontmatter")
    original_checked = False
    if upstream:
        source = Path(upstream).resolve()
        require(git(source, "rev-parse", "HEAD").strip() == PIN, "Wrong upstream revision")
        tracked = git(source, "ls-files", "-z").split("\0")[:-1]
        require(selected(tracked) == paths, "Original tracked-file selection mismatch")
        for relative in paths:
            path = source / relative
            require(not path.is_symlink() and path.read_bytes() == (archive / relative).read_bytes(),
                    f"Original byte mismatch: {relative}")
        original_checked = True
    scope_checked = False
    if base:
        require(re.fullmatch(r"[0-9a-f]{40}", base), "Use a full base SHA")
        previous = git(ROOT, "show", f"{base}:{ROUTER}")
        require((ROOT / ROUTER).read_text().startswith(previous), "Existing router content changed")
        changed = set(git(ROOT, "diff", "--name-only", base).splitlines())
        changed.update(git(ROOT, "ls-files", "--others", "--exclude-standard").splitlines())
        require(all(p in (ROUTER, ADAPTER) or p.startswith("docs/skill-library/superpowers/")
                    for p in changed), "Changes outside task ownership")
        scope_checked = True
    return dict(result="PASS", sourceCommit=PIN, version="7.0.2", sourceFiles=len(paths),
                sourceBytes=total, skillMainTexts=len(mains), retainedSkillSupportFiles=80 - len(mains),
                authoredLocalLinks=link_count, originalCheckoutChecked=original_checked,
                appendOnlyRouterAndTaskScopeChecked=scope_checked, activation="INACTIVE_REFERENCE_ONLY",
                behavioralEvals="NOT RUN", androidBuild="NOT RUN; documentation only")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--upstream", help="Optional checkout of the pinned upstream revision")
    parser.add_argument("--base", help="Optional full task-base SHA for scope/append-only review")
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.upstream, args.base), indent=2))
    except (ValueError, KeyError, OSError, subprocess.CalledProcessError) as error:
        print(json.dumps(dict(result="FAIL", error=str(error)), indent=2))
        raise SystemExit(1)
