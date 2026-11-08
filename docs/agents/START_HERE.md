# ETERNAL SUMMONER — 12-Agent Task Studio (v1)

**Status:** GitHub-based coordination documentation; NOT a deployment or an autonomous polling service. **User is Game Director.** ChatGPT coordinates planning and independent QA when asked; Arena Argent remains A01. OpenAI Codex is a *separate* senior reviewer/critical-systems owner, not one of the 12.

## Start a real agent (repeat for A01–A12)
1. Connect that agent's actual runtime to this GitHub repository and an account with appropriate permission. Confirm it can read issues/comments and clone the *merged* `main`. If it cannot, stop; never claim it can work autonomously.
2. Read [AGENTS.md](../../AGENTS.md), [ROLE_MATRIX.md](ROLE_MATRIX.md), [CLAIM_PROTOCOL.md](CLAIM_PROTOCOL.md), [REVIEW_GATES.md](REVIEW_GATES.md), and its `.agents/skills/es-aXX-.../SKILL.md`. Existing relevant Arena/codex skills remain in force. Do **not** load every old skill at once.
3. Open [AI Task Center #39](https://github.com/lightingfff/eternal-sunmmoner/issues/39) and the role's **own task issue** linked in the latest roster comment. Refresh PR status and ticket status before any code changes.
4. Only choose work marked `READY`, with dependencies met. Post the exact `[CLAIM]` comment. Re-read comments for collisions. If another live claim exists, report `BLOCKED`; do not race.
5. Use one new feature branch/PR per task; follow file ownership, baseline SHA, test and artifact evidence. Never push to main, merge, rewrite history, force push, or claim phone FPS without phone measurements.
6. Post progress, test report, PR link and `[HANDOFF]` on that issue. Reviewers independently comment `[REVIEW]`. The **user** merges only after the gates.

No GitHub comment itself starts or wakes up an agent. The owner must launch/configure each agent; an optional external automation can later poll GitHub, with explicit setup and credentials. A skill teaches workflow; it is not training model weights.

**Permanent project constraints:** Offline Android Kotlin/Jetpack Compose Canvas; 2.5D oblique ground Y-scale 0.72; Sanctuary island/plaza/building placements, collision, paths, anchors, Master off-map commander and save compatibility preserved. First Tower floor meadow. No fake scenery as a single screenshot, no visual downgrade without evidence.

## Optional Gemini 4 Argon High advisory (not another agent)

Gemini 4 Argon High may help with complex design, codebase/PR reasoning, cross-role impact analysis and grounded visual critique. **It has no Axx role, file ownership, CLAIM privileges or merge authority.** Use only the checkpoint rules and paste-ready prompt in [GEMINI4_ARGON_ADVISORY.md](GEMINI4_ARGON_ADVISORY.md). A01 directs the work; A09 remains independent QA; Codex remains required for core combat/save/event changes. GitHub/App/AI Studio access is capability-dependent; no implied API entitlement or background polling.
