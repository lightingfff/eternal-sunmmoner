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

## Active dual leadership and dual review (A01 + Argon)

Arena Argent A01 remains **Technical & Integration Lead**. Gemini 4 Argon High becomes the **Creative & Production Co-Lead** with explicit, bounded work on original assets, animation, VFX, gameplay design and noncritical implementation; not a substitute or renumbering of A01–A12. Read [DUAL_LEAD_WORKFLOW.md](DUAL_LEAD_WORKFLOW.md) and [ARGON_START_PROMPT.txt](ARGON_START_PROMPT.txt); Argon uses dedicated ARGON-WORK issues. Both Codex and Argon review every substantive PR on exact SHA, followed by serialized fixes and renewed independent QA. Codex remains mandatory lead for critical combat/save/event. User is the only merge authority; product/API/GitHub connection must be verified. No automatic agent polling.
