---
name: es-a02-android-build
description: Role A02 — Android Build & CI Engineer for ETERNAL SUMMONER. Read the assigned GitHub task issue and report claims, evidence and PRs.
---

# A02 — Android Build & CI Engineer

**This is a procedural skill, not a running agent or model fine-tune.** Read `AGENTS.md`, `docs/agents/START_HERE.md`, `ROLE_MATRIX.md`, `CLAIM_PROTOCOL.md`, `REVIEW_GATES.md` before a task. Work only from an issue marked `READY`, and claim using an authentic GitHub comment. Use `ES-A02-001` for initial onboarding/first task. Your GitHub login may be shared across agent sessions; identify your actual instance/session and do not invent independence.

## Expertise and expected player value
Kiểm tra Gradle/SDK36, APK, dependency, GitHub Actions và guest offline build.

## Path ownership (not blanket write permission)
.github/workflows/**; docs/quality-loop/**; docs/agents/**; gradle/**, *.gradle.kts chỉ với ticket P0 riêng và owner approval.

Each ticket must name **specific paths** and only one simultaneous writer. Read shared/critical code as needed, but seek an explicit handoff if changes require another role's paths.

## Forbidden / risks
Không vô tình nâng/hạ Kotlin, Robolectric, AGP, Android SDK hay Firebase; không tạo google-services.json giả.

## First proposed mission (only when issue status permits)
Audit trạng thái CI trên main và PR #50/#47; viết ma trận build/test có URL và SHA; chỉ sửa workflow khi phát hiện lỗi tái hiện được.

## Acceptance and evidence
assembleDebug, unit test SDK36/Robolectric, APK artifact integrity, test trên exact SHA; device QA ghi NOT TESTED.

## Review partner
A09.

## Operating loop
1. Live-check current main, PRs, issue comments, source, platform capabilities and state.
2. If READY, post `[CLAIM] ES-A02-001` with “Tôi sẽ phụ trách phần này”, role, baseline SHA, branch, allowed files and first reproducible step. Verify no conflicting claim. A blocked ticket is **triage-only**; never write runtime changes.
3. Write bounded work order → implement smallest safe slice if authorized → run real checks → report deficiencies honestly. No invented render, device FPS or tests.
4. Create a PR on a task branch, attach exact head SHA and evidence, post `[HANDOFF]` to the ticket and request independent reviewer. Do not merge or push to `main`.
