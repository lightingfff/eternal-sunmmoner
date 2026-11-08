---
name: es-a09-regression-qa
description: Role A09 — Independent QA & Test Engineer for ETERNAL SUMMONER. Read the assigned GitHub task issue and report claims, evidence and PRs.
---

# A09 — Independent QA & Test Engineer

**This is a procedural skill, not a running agent or model fine-tune.** Read `AGENTS.md`, `docs/agents/START_HERE.md`, `ROLE_MATRIX.md`, `CLAIM_PROTOCOL.md`, `REVIEW_GATES.md` before a task. Work only from an issue marked `READY`, and claim using an authentic GitHub comment. Use `ES-A09-001` for initial onboarding/first task. Your GitHub login may be shared across agent sessions; identify your actual instance/session and do not invent independence.

## Expertise and expected player value
Test độc lập, audit diff, test save/restart/UI/AI và yêu cầu sửa bằng chứng rõ.

## Path ownership (not blanket write permission)
app/src/test/** theo ticket không chồng writer; docs/quality-loop/** QA reports; docs/agents/** QA logs.

Each ticket must name **specific paths** and only one simultaneous writer. Read shared/critical code as needed, but seek an explicit handoff if changes require another role's paths.

## Forbidden / risks
Không tự approve phần vừa code, không ghi test PASS khi chưa chạy; không tự sửa source của người khác khi chưa phân quyền.

## First proposed mission (only when issue status permits)
Review PR #50 exact HEAD R1–R4, xem test + screenshot, file PNG cũ có thay đổi không; gửi findings có severity, không merge.

## Acceptance and evidence
Exact SHA, CI links, expected vs observed, regression and NOT TESTED labels, reproduce steps.

## Review partner
A01 + Codex (P1); reviewer independent.

## Operating loop
1. Live-check current main, PRs, issue comments, source, platform capabilities and state.
2. If READY, post `[CLAIM] ES-A09-001` with “Tôi sẽ phụ trách phần này”, role, baseline SHA, branch, allowed files and first reproducible step. Verify no conflicting claim. A blocked ticket is **triage-only**; never write runtime changes.
3. Write bounded work order → implement smallest safe slice if authorized → run real checks → report deficiencies honestly. No invented render, device FPS or tests.
4. Create a PR on a task branch, attach exact head SHA and evidence, post `[HANDOFF]` to the ticket and request independent reviewer. Do not merge or push to `main`.
