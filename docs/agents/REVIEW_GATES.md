# Merge and integration quality gates

1. **Scope**: exact task/PR/base/head SHAs; all changed paths within ticket's ownership; no unapproved lock-file, Gradle, Firebase, shared renderer, map, collision, save, event or critical combat edits.
2. **Functional**: actual `gradle :app:assembleDebug`/GitHub APK CI and focused tests; preserve offline guest startup, Tower entry/exit, Master orders and save/retry when relevant. Test that passed on *old* SHA does not qualify.
3. **Art**: independently decode assets and validate manifest bytes/hash/pivot/frame/alpha/tiling. Contact sheet is not proof of in-engine image quality; inspect true engine screenshots at .36/.88/1.48 zoom, 800×360, before/after. P31-A3 art validation precedes all P31-B runtime integration.
4. **Performance**: Android real device for FPS/heat only; CI and Robolectric do not prove GPU time/thermal. Preserve high visual quality with culling/caching/capped VFX budgets and independent LOW/BALANCED/HIGH profiles.
5. **Gameplay**: for first Tower Floor 1 meadow rescue, real play path, ≥2 distinct enemy behaviors, meaningful Master tactics, partial/fail/retry, idempotent save/reward. 55-second test simulation is NOT 5–10min human playtest.
6. **Peer review**: A09 reviews code/UI and QA; A03+A08 cross-review P31 art; A10 performance; Codex **mandatory** for critical battle/save schema/event transaction/idempotency or high-risk changes. Reviewer cannot sign off own authored change.
7. **Approval**: reviewers say APPROVE only on exact SHA and evidence. User alone decides merge; no automatic bot/skill merges. Mark untested as NOT TESTED and unresolved blocker as BLOCKED.

8. **Optional Argon advisory**: For complex multi-module code/architecture, Floor-1 gameplay design and screenshot/playtest critique, A01/owner may request an `[ARGON_ADVISORY]` using [GEMINI4_ARGON_ADVISORY.md](GEMINI4_ARGON_ADVISORY.md). It is **not** mandatory merge approval, not a thirteenth agent, does not grant file ownership/CLAIM status, and **never replaces A09's tests or Codex's critical-system review**. Separate model observations from verified CI/render/device evidence.
