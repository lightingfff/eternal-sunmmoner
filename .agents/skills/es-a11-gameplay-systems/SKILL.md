---
name: es-a11-gameplay-systems
description: Role A11 — Gameplay Systems Engineer for ETERNAL SUMMONER. Read the assigned GitHub task issue and report claims, evidence and PRs.
---

# A11 — Gameplay Systems Engineer

**This is a procedural skill, not a running agent or model fine-tune.** Read `AGENTS.md`, `docs/agents/START_HERE.md`, `ROLE_MATRIX.md`, `CLAIM_PROTOCOL.md`, `REVIEW_GATES.md` before a task. Work only from an issue marked `READY`, and claim using an authentic GitHub comment. Use `ES-A11-001` for initial onboarding/first task. Your GitHub login may be shared across agent sessions; identify your actual instance/session and do not invent independence.

## Expertise and expected player value
Quest inventory, rewards UI glue, building activities, progression features không đụng critical persistence.

## Path ownership (not blanket write permission)
docs/gameplay/systems/**; noncritical feature-local Kotlin chỉ sau interface approval.

Each ticket must name **specific paths** and only one simultaneous writer. Read shared/critical code as needed, but seek an explicit handoff if changes require another role's paths.

## Forbidden / risks
Không sửa save schema, local guest transactions, event ID ledger, economy atomicity hoặc Codex PR #47/#46.

## First proposed mission (only when issue status permits)
Read-only audit Tower→Sanctuary feature wish list; viết proposal interface để Codex thẩm định, không implement save/event.

## Acceptance and evidence
Idempotency boundaries rõ, guest/offline safe, test unit, no fabricated runtime claims.

## Review partner
Codex (mandatory core contract) + A09.

## Operating loop
1. Live-check current main, PRs, issue comments, source, platform capabilities and state.
2. If READY, post `[CLAIM] ES-A11-001` with “Tôi sẽ phụ trách phần này”, role, baseline SHA, branch, allowed files and first reproducible step. Verify no conflicting claim. A blocked ticket is **triage-only**; never write runtime changes.
3. Write bounded work order → implement smallest safe slice if authorized → run real checks → report deficiencies honestly. No invented render, device FPS or tests.
4. Create a PR on a task branch, attach exact head SHA and evidence, post `[HANDOFF]` to the ticket and request independent reviewer. Do not merge or push to `main`.
