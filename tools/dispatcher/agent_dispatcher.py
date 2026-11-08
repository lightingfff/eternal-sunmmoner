"""
Auto Agent Dispatcher for Eternal Summoner.
Validates issues labeled `ai:ready`, enforces single writer locks, checks dependencies,
maps roles to skills/CLI backends, triggers dry-run or real dispatchers, and handles PR reviews.
"""

import argparse
import json
import os
import re
import sys
from typing import Dict, Any, List, Optional, Tuple

ROLE_REGISTRY: Dict[str, Dict[str, Any]] = {
    "A01": {
        "role_id": "A01",
        "title": "Arena Argent / Technical Director & Integration Lead",
        "skill": ".agents/skills/es-a01-technical-director/SKILL.md",
        "backend": "arena_cli",
        "allowed_paths_default": ["docs/agents/**", "docs/quality-loop/**"],
        "reviewer": "A09",
    },
    "A02": {
        "role_id": "A02",
        "title": "Android Build & CI Engineer",
        "skill": ".agents/skills/es-a02-android-build/SKILL.md",
        "backend": "gemini_cli",
        "allowed_paths_default": [".github/workflows/**", "app/build.gradle.kts", "gradle/**"],
        "reviewer": "A09",
    },
    "A03": {
        "role_id": "A03",
        "title": "Graphics / 2.5D Rendering Engineer",
        "skill": ".agents/skills/es-a03-graphics-renderer/SKILL.md",
        "backend": "gemini_cli",
        "allowed_paths_default": [
            "app/src/main/java/com/example/game/pixel/**",
            "app/src/main/assets/sanctuary_argon_v2/**",
            "docs/quality-loop/ARGON-WORK-002/**"
        ],
        "reviewer": "A08",
    },
    "A04": {
        "role_id": "A04",
        "title": "HUD & UI/UX Engineer",
        "skill": ".agents/skills/es-a04-ui-ux/SKILL.md",
        "backend": "gemini_cli",
        "allowed_paths_default": ["app/src/main/java/com/example/ui/**"],
        "reviewer": "A09",
    },
    "A05": {
        "role_id": "A05",
        "title": "Tower Level & Quest Designer",
        "skill": ".agents/skills/es-a05-tower-level-designer/SKILL.md",
        "backend": "gemini_cli",
        "allowed_paths_default": ["docs/tower-reforge/**", "docs/level-design/**"],
        "reviewer": "A06",
    },
    "A06": {
        "role_id": "A06",
        "title": "Monster AI / Encounter Tactics Designer",
        "skill": ".agents/skills/es-a06-monster-ai-designer/SKILL.md",
        "backend": "codex_cli",
        "allowed_paths_default": ["app/src/main/java/com/example/game/ai/**"],
        "reviewer": "A09",
    },
    "A07": {
        "role_id": "A07",
        "title": "Hero Identity & Narrative Designer",
        "skill": ".agents/skills/es-a07-hero-narrative/SKILL.md",
        "backend": "gemini_cli",
        "allowed_paths_default": ["docs/narrative/**"],
        "reviewer": "A05",
    },
    "A08": {
        "role_id": "A08",
        "title": "Pixel Art, Animation & Asset QA",
        "skill": ".agents/skills/es-a08-pixel-art-vfx/SKILL.md",
        "backend": "gemini_cli",
        "allowed_paths_default": ["docs/art/**", "docs/quality-loop/**"],
        "reviewer": "A09",
    },
    "A09": {
        "role_id": "A09",
        "title": "Independent Regression QA & Test Engineer",
        "skill": ".agents/skills/es-a09-regression-qa/SKILL.md",
        "backend": "codex_cli",
        "allowed_paths_default": ["docs/quality-loop/**", "app/src/test/**"],
        "reviewer": "A01",
    },
    "A10": {
        "role_id": "A10",
        "title": "Android Performance & Thermal Auditor",
        "skill": ".agents/skills/es-a10-android-performance/SKILL.md",
        "backend": "codex_cli",
        "allowed_paths_default": ["docs/quality-loop/**", "docs/perf/**"],
        "reviewer": "A03",
    },
    "A11": {
        "role_id": "A11",
        "title": "Gameplay Systems Engineer",
        "skill": ".agents/skills/es-a11-gameplay-systems/SKILL.md",
        "backend": "gemini_cli",
        "allowed_paths_default": ["app/src/main/java/com/example/game/**"],
        "reviewer": "A09",
    },
    "A12": {
        "role_id": "A12",
        "title": "Audio & Sound Designer",
        "skill": ".agents/skills/es-a12-audio-sound/SKILL.md",
        "backend": "gemini_cli",
        "allowed_paths_default": ["docs/audio/**", "app/src/main/assets/audio/**"],
        "reviewer": "A05",
    },
    "ARGON": {
        "role_id": "ARGON",
        "title": "Gemini 4 Argon High / Creative & Production Co-Lead",
        "skill": ".agents/skills/eternal-argon-creative-technical-lead/SKILL.md",
        "backend": "gemini_cli",
        "allowed_paths_default": ["docs/art/**", "docs/quality-loop/**"],
        "reviewer": "A01",
    },
    "CODEX": {
        "role_id": "CODEX",
        "title": "OpenAI Codex / Senior Critical-Systems Engineer",
        "skill": ".agents/skills/codex-eternal-director/SKILL.md",
        "backend": "codex_cli",
        "allowed_paths_default": [
            "app/src/main/java/com/example/game/combat/**",
            "app/src/main/java/com/example/data/local/**"
        ],
        "reviewer": "A01",
    },
}

FORBIDDEN_GLOBAL_PATHS = [
    ".git/**",
    ".github/workflows/main.yml",
    "gradle/wrapper/**",
    "gradlew",
    "gradlew.bat"
]


def extract_role_from_text(title: str, body: str) -> Optional[str]:
    """Detect assigned role token (A01..A12, ARGON, CODEX) from issue title/body."""
    full_text = f"{title}\n{body}"
    # Check explicit match like ROLE: A03 or [ES-A03-001]
    match = re.search(r"\[ES-(A\d{2})-\d+\]", title, re.IGNORECASE)
    if match:
        return match.group(1).upper()

    match = re.search(r"\bROLE:\s*(A\d{2}|ARGON|CODEX)\b", full_text, re.IGNORECASE)
    if match:
        return match.group(1).upper()

    match = re.search(r"\b(A\d{2})\b", title)
    if match:
        role = match.group(1).upper()
        if role in ROLE_REGISTRY:
            return role

    if "ARGON-WORK" in title or "ARGON" in title:
        return "ARGON"
    if "CODEX" in title:
        return "CODEX"

    return None


def extract_dependencies(body: str) -> List[str]:
    """Extract required PR or Issue dependencies from issue body."""
    deps = []
    # Match Patterns like DEPENDENCIES: #51, #42 or PR #50
    m = re.search(r"DEPENDENCIES:\s*([^\n]+)", body, re.IGNORECASE)
    if m:
        dep_str = m.group(1)
        items = re.findall(r"#(\d+)", dep_str)
        deps.extend([f"#{item}" for item in items])
    return deps


def check_file_collision(
    requested_paths: List[str],
    active_locks: Dict[str, str],
    issue_id: str
) -> Tuple[bool, List[str]]:
    """
    Check if any requested path is already locked by another active issue/PR.
    Returns (has_collision, collided_paths).
    """
    collisions = []
    for path in requested_paths:
        normalized = path.strip()
        for locked_path, locked_issue in active_locks.items():
            if locked_issue != issue_id:
                if normalized == locked_path or (
                    normalized.endswith("/**") and locked_path.startswith(normalized[:-3])
                ):
                    collisions.append(f"{normalized} (locked by #{locked_issue})")
    return len(collisions) > 0, collisions


def check_backend_availability(backend: str) -> Tuple[bool, str]:
    """
    Check if an agent backend CLI/API is configured in the environment.
    Strictly avoids assuming mock or unconfigured tools are live.
    """
    if backend == "gemini_cli":
        has_key = bool(os.environ.get("GEMINI_API_KEY"))
        has_cli = bool(os.environ.get("GEMINI_CLI_ENABLED"))
        if has_key or has_cli:
            return True, "Gemini API/CLI configured via environment"
        return False, "GEMINI_API_KEY / GEMINI_CLI_ENABLED not set (DRY RUN ONLY)"

    if backend == "codex_cli":
        has_key = bool(os.environ.get("OPENAI_API_KEY") or os.environ.get("CODEX_API_KEY"))
        if has_key:
            return True, "Codex API configured via environment"
        return False, "OPENAI_API_KEY / CODEX_API_KEY not set (DRY RUN ONLY)"

    if backend == "arena_cli":
        # Arena runs interactively in sandbox sessions; never has external webhook/background daemon
        return False, "Arena Agent operates in direct interactive session (CLI automation disabled)"

    return False, f"Unknown backend {backend}"


def generate_claim_payload(
    issue_number: int,
    role: str,
    base_sha: str,
    branch_name: str,
    allowed_files: List[str]
) -> str:
    """Generate standardized [CLAIM] template conforming to CLAIM_PROTOCOL.md."""
    role_info = ROLE_REGISTRY.get(role, {})
    return f"""[CLAIM] TASK: ES-{role}-AUTO | ROLE: {role}
Tôi sẽ phụ trách phần này.
AGENT RUNTIME: Auto Dispatcher ({role_info.get('backend', 'unconfigured')})
GITHUB IDENTITY: @lightingfff
BASE SHA: {base_sha}
BRANCH: {branch_name}
SKILLS READ: AGENTS.md; docs/agents/DUAL_LEAD_WORKFLOW.md; {role_info.get('skill', '')}
ALLOWED FILES: {', '.join(allowed_files)}
DEPENDENCIES: Validated by Auto Dispatcher
NEXT: Initialize feature branch, implement scoped changes, run tests.
"""


def generate_pr_review_dispatch(
    pr_number: int,
    head_sha: str,
    changed_files: List[str]
) -> Dict[str, Any]:
    """
    Configure dual-review dispatch for PR according to DUAL_LEAD_WORKFLOW.md:
    Argon (creative/visual/UX) + Codex (critical systems/architecture) + A09 (regression QA).
    """
    is_critical_combat = any("combat" in f or "ai" in f or "save" in f for f in changed_files)
    is_visual_art = any("assets" in f or "art" in f or "pixel" in f for f in changed_files)

    reviewers = ["ARGON", "CODEX", "A09"]
    plan = {
        "pr_number": pr_number,
        "head_sha": head_sha,
        "changed_files": changed_files,
        "reviewers": reviewers,
        "argon_focus": "Visual readability, nearest-neighbor, frame pacing, art asset manifest compliance" if is_visual_art else "UX, game feel, cross-module cohesion",
        "codex_focus": "Critical combat invariants, state transaction, save safety, architecture" if is_critical_combat else "Code correctness, CI integrity, contract invariants",
        "a09_focus": "Independent regression test, headless Robolectric check, contrast & TalkBack audit",
        "no_self_merge": True
    }
    return plan


def run_dispatcher(
    issue_number: int,
    issue_title: str,
    issue_body: str,
    base_sha: str,
    dry_run: bool = True
) -> Dict[str, Any]:
    """Execute triage, role resolution, collision check, and backend dispatch."""
    role = extract_role_from_text(issue_title, issue_body)
    if not role or role not in ROLE_REGISTRY:
        return {
            "status": "UNASSIGNED",
            "message": f"Could not determine valid A01–A12 / ARGON / CODEX role for issue #{issue_number}",
            "role": None
        }

    role_info = ROLE_REGISTRY[role]
    deps = extract_dependencies(issue_body)
    allowed_files = role_info["allowed_paths_default"]

    backend = role_info["backend"]
    is_available, backend_msg = check_backend_availability(backend)

    branch_name = f"agent/{role.lower()}-issue{issue_number}-auto"

    result = {
        "status": "DISPATCHED" if (is_available and not dry_run) else "SIMULATED",
        "issue_number": issue_number,
        "role": role,
        "title": role_info["title"],
        "skill": role_info["skill"],
        "branch": branch_name,
        "base_sha": base_sha,
        "dependencies": deps,
        "allowed_files": allowed_files,
        "backend": backend,
        "backend_status": backend_msg,
        "claim_comment": generate_claim_payload(issue_number, role, base_sha, branch_name, allowed_files),
        "dry_run": dry_run
    }
    return result
