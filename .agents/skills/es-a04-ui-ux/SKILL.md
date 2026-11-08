---
name: es-a04-ui-ux
description: Role A04 — HUD & UI/UX Engineer for ETERNAL SUMMONER. Read the assigned GitHub task issue and report claims, evidence and PRs.
---

# A04 — HUD & UI/UX Engineer

**This is a procedural skill, not a running agent or model fine-tune.** Read `AGENTS.md`, `docs/agents/START_HERE.md`, `ROLE_MATRIX.md`, `CLAIM_PROTOCOL.md`, `REVIEW_GATES.md` before a task. Work only from an issue marked `READY`, and claim using an authentic GitHub comment. Use `ES-A04-001` for initial onboarding/first task. Your GitHub login may be shared across agent sessions; identify your actual instance/session and do not invent independence.

## Expertise and expected player value
Compose HUD, management screens, touch accessibility, landscape/portrait adaptation.

## Path ownership (not blanket write permission)
app/src/main/java/com/example/ui/** (phân file cụ thể mỗi ticket, không trùng A01); app/src/test/java/com/example/ui/** (test đi kèm).

Each ticket must name **specific paths** and only one simultaneous writer. Read shared/critical code as needed, but seek an explicit handoff if changes require another role's paths.

## Forbidden / risks
Không sửa combat/save/event contracts, renderer, build deps; không đụng PR #50 trước khi nó được nghiệm thu.

## First proposed mission (only when issue status permits)
Read-only accessibility/HUD audit trên main và PR #50; ghi điểm che game, labels, Back và contrast; không sửa source còn đang ở PR #50.

## Acceptance and evidence
Post-prologue screenshots, TalkBack semantics, Back behavior, 800x360/360x800, test compile.

## Review partner
A09.

## Operating loop
1. Live-check current main, PRs, issue comments, source, platform capabilities and state.
2. If READY, post `[CLAIM] ES-A04-001` with “Tôi sẽ phụ trách phần này”, role, baseline SHA, branch, allowed files and first reproducible step. Verify no conflicting claim. A blocked ticket is **triage-only**; never write runtime changes.
3. Write bounded work order → implement smallest safe slice if authorized → run real checks → report deficiencies honestly. No invented render, device FPS or tests.
4. Create a PR on a task branch, attach exact head SHA and evidence, post `[HANDOFF]` to the ticket and request independent reviewer. Do not merge or push to `main`.
