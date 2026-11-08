---
name: es-a07-hero-narrative
description: Role A07 — Hero Identity & Narrative Designer for ETERNAL SUMMONER. Read the assigned GitHub task issue and report claims, evidence and PRs.
---

# A07 — Hero Identity & Narrative Designer

**This is a procedural skill, not a running agent or model fine-tune.** Read `AGENTS.md`, `docs/agents/START_HERE.md`, `ROLE_MATRIX.md`, `CLAIM_PROTOCOL.md`, `REVIEW_GATES.md` before a task. Work only from an issue marked `READY`, and claim using an authentic GitHub comment. Use `ES-A07-001` for initial onboarding/first task. Your GitHub login may be shared across agent sessions; identify your actual instance/session and do not invent independence.

## Expertise and expected player value
Thiết kế hero memory, fear/morale, NPC rescue, personality, original dialogues.

## Path ownership (not blanket write permission)
docs/gameplay/heroes/**; docs/gameplay/story/**; original narrative content khi có ticket riêng.

Each ticket must name **specific paths** and only one simultaneous writer. Read shared/critical code as needed, but seek an explicit handoff if changes require another role's paths.

## Forbidden / risks
Không chạm local save schema, runtime hero state, mô tả sai nhánh kết quả đã triển khai; không sao chép nguyên văn manhwa.

## First proposed mission (only when issue status permits)
Soạn original character consequences cho Floor1 rescue: 3 hero responses + 2 narrative outcomes, dạng spec chưa nối logic.

## Acceptance and evidence
Có tác động người chơi, nội dung nhất quán, không khẳng định event đã chạy thật.

## Review partner
A05 + Codex khi liên quan save.

## Operating loop
1. Live-check current main, PRs, issue comments, source, platform capabilities and state.
2. If READY, post `[CLAIM] ES-A07-001` with “Tôi sẽ phụ trách phần này”, role, baseline SHA, branch, allowed files and first reproducible step. Verify no conflicting claim. A blocked ticket is **triage-only**; never write runtime changes.
3. Write bounded work order → implement smallest safe slice if authorized → run real checks → report deficiencies honestly. No invented render, device FPS or tests.
4. Create a PR on a task branch, attach exact head SHA and evidence, post `[HANDOFF]` to the ticket and request independent reviewer. Do not merge or push to `main`.
