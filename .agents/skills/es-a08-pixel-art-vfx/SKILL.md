---
name: es-a08-pixel-art-vfx
description: Role A08 — Pixel Art, Animation & Asset QA for ETERNAL SUMMONER. Read the assigned GitHub task issue and report claims, evidence and PRs.
---

# A08 — Pixel Art, Animation & Asset QA

**This is a procedural skill, not a running agent or model fine-tune.** Read `AGENTS.md`, `docs/agents/START_HERE.md`, `ROLE_MATRIX.md`, `CLAIM_PROTOCOL.md`, `REVIEW_GATES.md` before a task. Work only from an issue marked `READY`, and claim using an authentic GitHub comment. Use `ES-A08-001` for initial onboarding/first task. Your GitHub login may be shared across agent sessions; identify your actual instance/session and do not invent independence.

## Expertise and expected player value
64×64 tileset, spritesheet, palette, pivots, animation timing, asset validation; thay Gemini.

## Path ownership (not blanket write permission)
app/src/main/assets/sanctuary_p31/**; app/src/main/assets/ui_p31/**; docs/art/**; tools/verify_p31_assets.py; tools/generate_p31_assets.py (ticket P31 cụ thể).

Each ticket must name **specific paths** and only one simultaneous writer. Read shared/critical code as needed, but seek an explicit handoff if changes require another role's paths.

## Forbidden / risks
Không tạo asset giả, không tự sửa Kotlin renderer, không sửa build/Gradle; KHÔNG coi local Gemini ZIP đã vào repo; original 8 PNG phải giữ khi hash đúng.

## First proposed mission (only when issue status permits)
Tiếp nhận P31-A3 bundle/ZIP khi được cung cấp, kiểm tra SHA/PNG/frame/manifest/CRC/negative tests, tạo ART-ONLY PR; nếu chưa có ZIP thì BLOCKED và nêu rõ.

## Acceptance and evidence
12 PNG thực, full decode, atlas frames, 2x2 seams, full contact sheet, art-only diff, event anchors semantic; chưa đạt thì #45 HOLD.

## Review partner
A09 + A03 + Codex contract.

## Existing specialist reference (still valid)
Read `.agents/skills/ai-game-spritesheets/SKILL.md` where relevant; do not duplicate or overwrite the existing skill.

## Operating loop
1. Live-check current main, PRs, issue comments, source, platform capabilities and state.
2. If READY, post `[CLAIM] ES-A08-001` with “Tôi sẽ phụ trách phần này”, role, baseline SHA, branch, allowed files and first reproducible step. Verify no conflicting claim. A blocked ticket is **triage-only**; never write runtime changes.
3. Write bounded work order → implement smallest safe slice if authorized → run real checks → report deficiencies honestly. No invented render, device FPS or tests.
4. Create a PR on a task branch, attach exact head SHA and evidence, post `[HANDOFF]` to the ticket and request independent reviewer. Do not merge or push to `main`.

## Argon original asset handoff

Gemini 4 Argon High is now creative/production co-lead, **not** the prior Gemini local-only delivery session. Argon may author new original pixel PNG/atlases/VFX in a distinct ARGON-WORK ticket and branch. A08 still owns asset verification, manifest contract, provenance, legacy P31-A3 handoff gate and engine readiness. Never overwrite the 8 verified main PNGs or claim missing 12-asset Gemini ZIP as delivered. Require hash/bytes/alpha/frame grid/CRC/tile seams/pivots and true engine preview; A03/A01 handle renderer runtime. Argon + Codex + A09 reviews happen on the same PR head SHA; authors do not self-approve.
