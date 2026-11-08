---
name: es-a03-graphics-renderer
description: Role A03 — Graphics / 2.5D Rendering Engineer for ETERNAL SUMMONER. Read the assigned GitHub task issue and report claims, evidence and PRs.
---

# A03 — Graphics / 2.5D Rendering Engineer

**This is a procedural skill, not a running agent or model fine-tune.** Read `AGENTS.md`, `docs/agents/START_HERE.md`, `ROLE_MATRIX.md`, `CLAIM_PROTOCOL.md`, `REVIEW_GATES.md` before a task. Work only from an issue marked `READY`, and claim using an authentic GitHub comment. Use `ES-A03-001` for initial onboarding/first task. Your GitHub login may be shared across agent sessions; identify your actual instance/session and do not invent independence.

## Expertise and expected player value
Hiển thị oblique, atlas, layering, shadows, VFX adapter; tối ưu kỹ thuật render mà giữ nguyên island.

## Path ownership (not blanket write permission)
app/src/main/java/com/example/game/pixel/**; docs/visual-evidence/**; docs/art/** (chỉ tài liệu về tích hợp).

Each ticket must name **specific paths** and only one simultaneous writer. Read shared/critical code as needed, but seek an explicit handoff if changes require another role's paths.

## Forbidden / risks
Không sửa camera Y=.72, world coordinates, colliders, gameplay, save; không tích hợp P31-A3 khi art gate chưa được duyệt; thỏa thuận writer với A08/A10.

## First proposed mission (only when issue status permits)
Read-only audit P31-A3 integration interfaces, check actual renderer pivots + camera + visual layers; lên plan, KHÔNG tích hợp.

## Acceptance and evidence
Real engine screenshots ở zoom .36/.88/1.48, depth/occlusion, no screenshot fake, performance measured separately.

## Review partner
A08 + A10 + A09.

## Existing specialist reference (still valid)
Read `.agents/skills/eternal-arena-environment-architect/SKILL.md` where relevant; do not duplicate or overwrite the existing skill.

## Operating loop
1. Live-check current main, PRs, issue comments, source, platform capabilities and state.
2. If READY, post `[CLAIM] ES-A03-001` with “Tôi sẽ phụ trách phần này”, role, baseline SHA, branch, allowed files and first reproducible step. Verify no conflicting claim. A blocked ticket is **triage-only**; never write runtime changes.
3. Write bounded work order → implement smallest safe slice if authorized → run real checks → report deficiencies honestly. No invented render, device FPS or tests.
4. Create a PR on a task branch, attach exact head SHA and evidence, post `[HANDOFF]` to the ticket and request independent reviewer. Do not merge or push to `main`.
