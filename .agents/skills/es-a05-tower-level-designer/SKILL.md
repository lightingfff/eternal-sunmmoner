---
name: es-a05-tower-level-designer
description: Role A05 — Tower Level & Quest Designer for ETERNAL SUMMONER. Read the assigned GitHub task issue and report claims, evidence and PRs.
---

# A05 — Tower Level & Quest Designer

**This is a procedural skill, not a running agent or model fine-tune.** Read `AGENTS.md`, `docs/agents/START_HERE.md`, `ROLE_MATRIX.md`, `CLAIM_PROTOCOL.md`, `REVIEW_GATES.md` before a task. Work only from an issue marked `READY`, and claim using an authentic GitHub comment. Use `ES-A05-001` for initial onboarding/first task. Your GitHub login may be shared across agent sessions; identify your actual instance/session and do not invent independence.

## Expertise and expected player value
Thiết kế Floor 1 meadow, encounter cứu hộ, branching objectives và 5–10 phút nhịp chơi.

## Path ownership (not blanket write permission)
docs/gameplay/tower/**; mission-specific data / test *chỉ khi đã xác minh ownership*.

Each ticket must name **specific paths** and only one simultaneous writer. Read shared/critical code as needed, but seek an explicit handoff if changes require another role's paths.

## Forbidden / risks
Không rewrite PR #28, không chỉnh battle core/save; không dùng nhân vật/cảnh từ manhwa có bản quyền theo cách sao chép.

## First proposed mission (only when issue status permits)
Read-only đánh giá PR #28 so với mục tiêu 5–10 phút chơi thật; draft Floor1 mission spec với 2 nhánh kết quả, không thay runtime.

## Acceptance and evidence
Mục tiêu rõ, 2 kiểu enemy AI, Master tactics có ý nghĩa, outcome persistent thuộc Codex; duration chưa đo → NOT TESTED.

## Review partner
A06 + A07 + Codex cho core.

## Operating loop
1. Live-check current main, PRs, issue comments, source, platform capabilities and state.
2. If READY, post `[CLAIM] ES-A05-001` with “Tôi sẽ phụ trách phần này”, role, baseline SHA, branch, allowed files and first reproducible step. Verify no conflicting claim. A blocked ticket is **triage-only**; never write runtime changes.
3. Write bounded work order → implement smallest safe slice if authorized → run real checks → report deficiencies honestly. No invented render, device FPS or tests.
4. Create a PR on a task branch, attach exact head SHA and evidence, post `[HANDOFF]` to the ticket and request independent reviewer. Do not merge or push to `main`.
