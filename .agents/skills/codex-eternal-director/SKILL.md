---
name: codex-eternal-director
description: Coordinate parallel specialized Codex subagents for ETERNAL SUMMONER's Android game development without file conflicts, broken gameplay or performance regressions. Use when planning or executing multi-module features, PR integration and vertical slices.
---

# ETERNAL SUMMONER — Codex Game Director protocol

**Not a new neural model.** This skill provides repeatable planning, delegation, traceability and QA. Follow the real `AGENTS.md` and the repo's `eternal-summoner-skill-router`; load 1–3 relevant specialist reference skills per task, not all 174.

## Mission and canon

Create a real, compelling offline Android top-down oblique 2.5D dark-fantasy game. Do not reinvent Sanctuary: preserve island silhouette, plazas, roads, facilities, interactions, renderer and camera ground `OBLIQUE_Y_SCALE=0.72`. Master is a command interface outside the field, not an actor. Heroes have tactics, memories, injuries/rest, combat and story consequences. Tower Floor 1 starts as meadow. Add original authored content; maintain attractive pixel-art presentation and phone performance.

## Director's 7-stage workflow

1. **Snapshot/repo discovery (NO WRITES).** Check branch, `git status`, latest merged PRs, `AGENTS.md`, skill router, files for Tower, AI, events, persistence, tests and performance. Note that as of pack authoring P23 Environment Cohesion PR #26 is open; re-check it live. Preserve user work. No `git reset --hard`, `git clean`, silent revert or unauthorized merge.
2. **Trace first playable loop.** From login/guest → Sanctuary → Tower 1 → encounter → battle state → conclusion → rewards/save → return. Locate actual functions and event flow before inventing new systems.
3. **Define one vertical-slice acceptance sheet.** User-visible objective, Master orders, hero/enemy behavior, branch outcome, victory/failure and retry, reward persisted locally, no crashes, no architecture/art regression. Create one deterministic representative playthrough fixture and a second alternate outcome.
4. **Parallel delegation after file ownership.** Spawn `gameplay_architect` and `tower_encounter_builder` for mutually exclusive file sets or parallel *read-only design* if ownership is not yet known. Concurrently ask `android_performance_auditor` to review without writes. Reserve `TowerFloorCatalog.kt`, central renderer, shared save schema, maps, `AGENTS.md` and manifest for DIRECTOR-ONLY serial integration unless explicit coordination. The reviewer starts with read-only acceptance planning and reviews post-integration.
5. **Bounded implementation.** Each worker returns files owned/modified, precise interfaces used, tests run, known blockers. If two tasks need one file, stop simultaneous edits and apply changes sequentially by coordinator. No automatic force push/merge. Prefer dedicated branches/worktrees for independently editing agents.
6. **Integration + evidence.** Merge commits/PRs manually or combine on a coordinator integration branch in dependency order. Run unit tests and `gradle :app:assembleDebug` if SDK available; else push a PR for GitHub CI. Request real-engine render of the affected scenes, inspect not just store images; animation/controls must be validated, not only static screenshots. Report hardware FPS/thermal as not tested without device.
7. **Gate + report.** Read-only reviewer audits final diff and evidence. Fix blockers, rerun checks, present PR to owner for review. Do NOT claim completed if merely documented. Avoid declaring quality equal to a commercial title absent gameplay evidence.

## Required task tickets (every delegated job)

Ticket ID, goal, dependencies, skill(s), *allowed files*, forbidden files, expected interface, acceptance criteria, commands/tests, evidence and rollback. Ask subagent to report a blocking dependency immediately rather than silently edit forbidden files.

## Conflict gate

- Repo-level `.codex` and `.agents/skills` are setup files, not editable by mission workers.
- P23 Environment/Architecture PR #26 owns its art scope. Never edit `premium_slice` PNGs, art generators, vegetation manifests, `PixelSpriteRenderer`, `SanctuaryWorldRenderer`, `PixelStructureAtlas`, `SanctuaryEnvironmentShadows` or graphical camera without coordinated handoff.
- Never assume subagents automatically create git worktrees or that parallel agents own isolated file systems; check actual environment. Parallel independent file edits might share one filesystem.
- Prefer concurrent *investigations* when modifying code would share files. Root can integrate after investigations finish.

## Performance acceptance notes

The existing `ScenePerformanceProbe` is an opt-in CPU/frame probe and `ScenePerformanceOverlay` updates ~1 Hz. It does not report GPU completion or physical heat. Don't add full-screen bloom or shaders as a gameplay feature. Respect caching, culling and no per-frame PNG decoding. Mobile targets are aspirational until tested.
