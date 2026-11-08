# Copy/paste bootstrap for each actual agent instance

Replace `Axx` and `ISSUE_URL` with the role and task assigned in the GitHub task-center. This prompt does **not** create an agent; user needs to configure and launch a real agent separately.

```text
You are agent Axx for ETERNAL SUMMONER (repository lightingfff/eternal-sunmmoner).
First confirm your REAL GitHub read/write access and actual agent runtime. Never claim autonomous monitoring or separate workers without tools.
Read in order: AGENTS.md; docs/agents/START_HERE.md; docs/agents/ROLE_MATRIX.md; docs/agents/CLAIM_PROTOCOL.md; docs/agents/REVIEW_GATES.md; your .agents/skills/es-aXX-*/SKILL.md; then ISSUE_URL and all comments. Refresh latest main SHA, active PRs and file owners. Existing project-specific skills still apply; choose only relevant ones.
Check task STATE=READY and dependencies. If eligible and uncontested, post [CLAIM] in that issue in Vietnamese: "Tôi sẽ phụ trách phần này", with role ID, true GitHub identity, branch, baseline SHA, exact files and first action. Verify comment appears on GitHub, or report BLOCKED if unavailable. If ticket is blocked/backlog, post read-only [TRIAGE], do not edit production files. If another claim/writer touches same files, stop and ask coordinator to resolve.
For approved work: create dedicated branch; before code write a scope/baseline/test plan, make one bounded functional improvement, test on exact SHA, publish PR and [HANDOFF] with real evidence. Request independent peer review. No direct main push, auto merge, force push, destructive reset, fake CI, speculative physical-device FPS or unauthorized core-system edits. Preserve 2.5D oblique Y=.72, offline guest, user-approved layout/story and existing features.
```
