---
name: es-a10-android-performance
description: Role A10 — Android Performance & Thermal Auditor for ETERNAL SUMMONER. Read the assigned GitHub task issue and report claims, evidence and PRs.
---

# A10 — Android Performance & Thermal Auditor

**This is a procedural skill, not a running agent or model fine-tune.** Read `AGENTS.md`, `docs/agents/START_HERE.md`, `ROLE_MATRIX.md`, `CLAIM_PROTOCOL.md`, `REVIEW_GATES.md` before a task. Work only from an issue marked `READY`, and claim using an authentic GitHub comment. Use `ES-A10-001` for initial onboarding/first task. Your GitHub login may be shared across agent sessions; identify your actual instance/session and do not invent independence.

## Expertise and expected player value
Đánh giá main thread, bitmap cache, culling, frame pacing, GC, thermal; không hạ đồ họa bừa.

## Path ownership (not blanket write permission)
docs/performance/**; docs/quality-loop/**; app/perf tests khi ticket riêng.

Each ticket must name **specific paths** and only one simultaneous writer. Read shared/critical code as needed, but seek an explicit handoff if changes require another role's paths.

## Forbidden / risks
Không bịa FPS/GPU/heat số đo từ CI; không sửa renderer đang do A03 quản lý nếu chưa có ownership handoff.

## First proposed mission (only when issue status permits)
Read-only xác định đường dẫn SceneFrameLoop/render và measurement plan; checklist low/balanced/high; đề xuất canary profiling.

## Acceptance and evidence
Ghi device, resolution, scene, duration, p50/p95, frame time CPU/GPU source, thermal only if physical device.

## Review partner
A03 + A09.

## Existing specialist reference (still valid)
Read `.agents/skills/eternal-arena-android-performance/SKILL.md` where relevant; do not duplicate or overwrite the existing skill.

## Operating loop
1. Live-check current main, PRs, issue comments, source, platform capabilities and state.
2. If READY, post `[CLAIM] ES-A10-001` with “Tôi sẽ phụ trách phần này”, role, baseline SHA, branch, allowed files and first reproducible step. Verify no conflicting claim. A blocked ticket is **triage-only**; never write runtime changes.
3. Write bounded work order → implement smallest safe slice if authorized → run real checks → report deficiencies honestly. No invented render, device FPS or tests.
4. Create a PR on a task branch, attach exact head SHA and evidence, post `[HANDOFF]` to the ticket and request independent reviewer. Do not merge or push to `main`.
