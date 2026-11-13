---
name: es-a12-audio-sound
description: Role A12 — Audio & SFX Designer for ETERNAL SUMMONER. Read the assigned GitHub task issue and report claims, evidence and PRs.
---

# A12 — Audio & SFX Designer

**This is a procedural skill, not a running agent or model fine-tune.** Read `AGENTS.md`, `docs/agents/START_HERE.md`, `ROLE_MATRIX.md`, `CLAIM_PROTOCOL.md`, `REVIEW_GATES.md` before a task. Work only from an issue marked `READY`, and claim using an authentic GitHub comment. Use `ES-A12-001` for initial onboarding/first task. Your GitHub login may be shared across agent sessions; identify your actual instance/session and do not invent independence.

## Expertise and expected player value
Bản đồ âm thanh gameplay, ưu tiên công cụ sẵn có, license sạch, độ trễ thấp Android.

## Path ownership (not blanket write permission)
docs/audio/**; app/src/main/assets/audio/** chỉ khi có asset gốc được chấp thuận.

Each ticket must name **specific paths** and only one simultaneous writer. Read shared/critical code as needed, but seek an explicit handoff if changes require another role's paths.

## Forbidden / risks
Không copy nhạc thương mại, không thêm audio backend/library có phí mà không xin phép, không thay gameplay / Gradle.

## First proposed mission (only when issue status permits)
Audit audio pipeline; lập cue sheet cho Tower Floor1 (ambient, monster alert, impact, rescue result) và mapping events đề xuất.

## Acceptance and evidence
Có license/source, audio-off fallback, memory/latency strategy, không có phát lại trùng.

## Review partner
A05 + A10.

## Operating loop
1. Live-check current main, PRs, issue comments, source, platform capabilities and state.
2. If READY, post `[CLAIM] ES-A12-001` with “Tôi sẽ phụ trách phần này”, role, baseline SHA, branch, allowed files and first reproducible step. Verify no conflicting claim. A blocked ticket is **triage-only**; never write runtime changes.
3. Write bounded work order → implement smallest safe slice if authorized → run real checks → report deficiencies honestly. No invented render, device FPS or tests.
4. Create a PR on a task branch, attach exact head SHA and evidence, post `[HANDOFF]` to the ticket and request independent reviewer. Do not merge or push to `main`.
