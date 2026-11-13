# GitHub Issue → Agent claim → Pull Request protocol

## The source of truth
- **#39** = index and planning. Each actionable task has its **own issue** containing Role, Task ID, state, dependencies, baseline, allowed/forbidden files, acceptance, reviewer and output.
- The issue body identifies intended role. A comment from an agent is **a declaration**, not proof of a real independent model or background worker. Only authenticated GitHub identity/session link + deliverables establish accountability.
- Status vocabulary: `BACKLOG`, `READY`, `CLAIMED`, `IN_PROGRESS`, `BLOCKED`, `REVIEW`, `DONE`. Without a separate bot, statuses change by explicit human/agent comments, not automatically.
- A task is executable only when issue body explicitly says `STATE: READY` and dependencies are satisfied; `BLOCKED`/`BACKLOG` tasks may be read/triaged, but **no production code changes**. Changes needing gameplay design/user acceptance are deferred to owner.

## Claim comment (copy exactly, fill truthful values)
```md
[CLAIM] TASK: ES-A08-001 | ROLE: A08 Pixel Art & VFX
Tôi sẽ phụ trách phần này.
AGENT RUNTIME: <actual provider/agent instance, or NOT CONFIGURED>
GITHUB IDENTITY: @<real user/bot username>
BASE SHA: <latest main SHA>
BRANCH: agent/a08-<ticket>-<topic>
SKILLS READ: AGENTS.md; .agents/skills/es-a08-pixel-art-vfx/SKILL.md; <other specific file>
ALLOWED FILES: <exact paths agreed in task>
DEPENDENCIES: <evidence met>
NEXT: <first measurable action>
```
A01/coordinator checks if the task has an older live claim, file conflict or missing permissions. Only **one active writer** per ticket and file. Two near-simultaneous claims do not grant both rights; coordinator resolves and losers post `[BLOCKED] COLLISION`. Same GitHub login controlling 12 agents must include DISTINCT agent runtime/session IDs; GitHub itself cannot verify identity.

## Required progress/report comments
```md
[PROGRESS] TASK: ... | ROLE: ... | STATUS: IN_PROGRESS/PARTIAL/BLOCKED
BASE SHA: ... | BRANCH: ... | CHANGED PATHS: ...
FINDINGS / REAL TESTS: ...
BLOCKER / NEXT STEP: ...
```
```md
[HANDOFF] TASK: ... | ROLE: ... | STATUS: REVIEW
PR: https://github.com/lightingfff/eternal-sunmmoner/pull/<number>
HEAD SHA: ...
CHANGED FILES: ...
TESTS (command, result, CI link): ...
SCREENSHOTS/ASSET VALIDATION: ...
ANDROID FPS/THERMAL: measured + method OR NOT TESTED
KNOWN RISKS: ...
REQUESTED REVIEWER: Axx / Codex
```
```md
[REVIEW] TASK: ... | REVIEWER: ... | SHA: ...
VERDICT: APPROVE / REQUEST_CHANGES / COMMENT
EVIDENCE: ... | REQUIRED FIX: ...
```

## Hard rules
No auto-merge; owner alone chooses when to merge. No direct main writes, no `git reset --hard`, `git clean`, force push, or secrets in issues. No fake CI/pixel measurements. Screenshots from mocked fixtures must be marked as such. GitHub issue comments DO NOT launch agents, assign repo permissions, or guarantee a race-free lock. Read repo/PR live, ignore stale status in old comments. Existing GitHub ruleset protects main; do not loosen protections for task convenience.

## Co-lead work and dual review (2026-11-08)

**Arena A01** is the technical lead; **Argon** is a creative/production co-lead, without changing A01–A12 IDs and without implied runtime/permissions. Argon implementation needs its own issue `ARGON-WORK-###` marked READY, exact file ownership, `[CLAIM]` with actual session/login and its own branch; role A08 still audits assets and A03 integrates renderer. A01 coordinates interfaces.

Every substantive PR gets parallel **READ-ONLY** `[ARGON_REVIEW]`, `[CODEX_REVIEW]`, and `[QA_REVIEW]` from A09. Each comment includes PR URL, HEAD SHA, critical severity, evidence/repro, tests required, and a truthful ACCESS/NOT TESTED declaration. After reviewers finish, coordinator posts `[FIX_PLAN]` with list of defects, exact files, **only one corrective writer per file**, and fix order. Fix in one writer's branch, or isolated patch from assigned reviewer applied serially by integrator. **Never co-edit/push the same PR branch concurrently**; reviewer who becomes author cannot self-approve. Repeat both reviews on updated SHA. See [DUAL_LEAD_WORKFLOW.md](DUAL_LEAD_WORKFLOW.md).

Example `[FIX_PLAN]`:
```md
[FIX_PLAN] PR: #NN | HEAD SHA: <sha> | ROUND: 1/3
BLOCKERS: <P0/P1 findings & review links>
OWNERS: <path-a> -> <single authorized writer>; <path-b> -> <single writer>
SEQUENCE: <fix order and integration checkpoints>
RETEST: <commands/art evidence>
REVIEW AGAIN: Argon + Codex + A09 on new SHA
```
