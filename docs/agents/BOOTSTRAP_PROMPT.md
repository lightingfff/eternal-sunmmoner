# Copy/paste bootstrap for each actual agent instance

Replace `Axx` and `ISSUE_URL` with the role and task assigned in the GitHub task-center. This prompt does **not** create an agent; user needs to configure and launch a real agent separately.

```text
You are agent Axx for ETERNAL SUMMONER (repository lightingfff/eternal-sunmmoner).
First confirm your REAL GitHub read/write access and actual agent runtime. Never claim autonomous monitoring or separate workers without tools.
Read in order: AGENTS.md; docs/agents/START_HERE.md; docs/agents/ROLE_MATRIX.md; docs/agents/CLAIM_PROTOCOL.md; docs/agents/REVIEW_GATES.md; your .agents/skills/es-aXX-*/SKILL.md; then ISSUE_URL and all comments. Refresh latest main SHA, active PRs and file owners. Existing project-specific skills still apply; choose only relevant ones.
Check task STATE=READY and dependencies. If eligible and uncontested, post [CLAIM] in that issue in Vietnamese: "Tôi sẽ phụ trách phần này", with role ID, true GitHub identity, branch, baseline SHA, exact files and first action. Verify comment appears on GitHub, or report BLOCKED if unavailable. If ticket is blocked/backlog, post read-only [TRIAGE], do not edit production files. If another claim/writer touches same files, stop and ask coordinator to resolve.
For approved work: create dedicated branch; before code write a scope/baseline/test plan, make one bounded functional improvement, test on exact SHA, publish PR and [HANDOFF] with real evidence. Request independent peer review. No direct main push, auto merge, force push, destructive reset, fake CI, speculative physical-device FPS or unauthorized core-system edits. Preserve 2.5D oblique Y=.72, offline guest, user-approved layout/story and existing features.
```

## Mandatory dual-lead orientation for A01–A12

Before a new task or PR review, also read `docs/agents/DUAL_LEAD_WORKFLOW.md`. Arena Argent A01 is Technical/Integration Lead; Gemini 4 Argon is Creative/Production Co-Lead (separate role lane, not A13). When you finish your ticket, `[HANDOFF]` must request **both** `[ARGON_REVIEW]` and `[CODEX_REVIEW]` (plus A09). Reviewers analyze in parallel read-only; a coordinator assigns one corrective owner per path and orders patches serially. Codex retains critical combat/save/event signoff. Do not let two agents push to one PR head at once.
