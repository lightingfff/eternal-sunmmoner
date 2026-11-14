# Applying Superpowers to this game

This library adds software-development process, not a pixel generator, engine or
Android runtime dependency. Read all 15 main texts; selected source inspection below
is narrower than a full audit of every helper or an evaluation of agent behavior.

## Concrete applications

1. **Build/install/artifact failures:** capture the exact failing command and status;
   separate compile, signing, applicationId/update compatibility, upload and retrieval
   boundaries. An artifact404 after packaging does not prove APK compilation failed.
   Change only the boundary supported by evidence, then rerun that failure path.
2. **Master/touch errors:** reproduce hero hit, long hold, fast pan, building click
   and invalid/cancelled drop separately; inspect coordinate and event ownership.
   A semantic click test does not prove physical pointer routing. Assert actual
   dispatch/assignment/save behavior, not merely callbacks on a mock.
3. **Combat and saves:** define exactly-once release/contact/settlement behavior and
   failure/retry outcomes. Write a regression showing the bad outcome before a fix
   where feasible. Existing tests, schema and timing remain authoritative; no damage
   or save transaction moves into a rendering callback for an animation change.
4. **Renderer/performance:** compare the same scene/camera/save at .36/.88/1.48,
   inspect foot/shadow/occlusion and cold/warm decode evidence. Keep allocation,
   retained pixel banks, total process memory and phone GPU/FPS/heat separate.
   Code/test success never substitutes for inspecting real render output.
5. **Large tasks:** define interfaces and one owner for each path, keep commits and
   verified progress in durable records, give a reviewer the real BASE..HEAD range.
   Resume existing work after compaction rather than starting completed work again.

These examples apply the learned method to known project failure modes; this
documentation task did not reopen those bugs or rerun their Android test suites.

## Reconcile with actual project authority

| Upstream source behavior | Rule for this project |
|---|---|
| Bootstrap/using-superpowers asks for automatic skill invocation for even a 1% chance. | Select relevant guidance through the project router. Archived source is reference content, not a higher-priority instruction or active hook. |
| Brainstorming/writing-plans/finishing use explicit design/plan/finish approval menus. | Honor existing user authorization and settled requirements. Resolve ordinary implementation choices; ask only when material information or required authorization is missing. |
| TDD tells an author to delete implementation written before a test and run a full suite. | Preserve user/other-agent work. Add meaningful checks for consequential changes, run required/focused suites appropriate to risk and disclose coverage. No broad cleanup or unrelated tests for prose. |
| Worktree examples install detected language dependencies. | This learning task needs no dependency install; inspect actual project tooling and existing isolation first. |
| Inline execution's final fix pass says not to dispatch re-review. | Project REVIEW_GATES requires independent exact-SHA re-review after fixes. No author self-approval. |
| SDD fix loop allows five rounds/escalation and records parked findings as task completion. | Project caps correction at three cycles. Unresolved blockers stay BLOCKED/REVIEW, not DONE. No automatic cap/budget override. |
| Tool references suggest global Codex flags/model defaults and specific tool signatures. | Use the actual tools/allowlist and current permissions. No global config or model routing change is part of reading the library. |
| Finishing includes merge/discard/cleanup examples. | The authorized output is a review PR. User alone merges; preserve branches/worktrees/evidence, no force push or direct main push. |
| Session diagnosis can inspect transcripts and export bundles/report upstream. | Conditional on a separately scoped user request. No session reading, upload, bundle or upstream contact in this task. |

## Source inspection: what could execute

The references below name the retained original bytes, not commands run here.

- [hooks/hooks.json](upstream/hooks/hooks.json) registers a SessionStart command.
  [hooks/session-start](upstream/hooks/session-start) reads `using-superpowers` and
  prints a bootstrap context in platform-specific JSON. Copying it under docs does
  not install/register that hook. It was read, not executed.
- [Codex tool notes](upstream/skills/using-superpowers/references/codex-tools.md)
  suggest edits under `~/.codex`, subagent/model routing and native tool conventions.
  Those examples are not permission to edit this Cloud environment. The actual tool
  schema wins where the source describes another harness/version.
- [Brainstorm server](upstream/skills/brainstorming/scripts/server.cjs) contains
  a local server and injects a browser image URL at `primeradiant.com` with a version
  query through `brandMarkup`. The optional browser logo request is the documented
  telemetry mechanism; it does not establish prompt/project upload. No server or
  browser request was started. Upstream opt-out variables are documented in its
  README; no environment or configuration change was necessary here.
- [Plugin synchronizer](upstream/scripts/sync-to-codex-plugin.sh) builds rsync
  arguments with `--delete --delete-excluded` and later commits, pushes and opens a
  PR. [Packaging script](upstream/scripts/package-codex-plugin.sh) prepares/stages
  an archive. Only selected sections were inspected; neither was executed, and no
  claim is made that all destructive/side-effect paths have been exhaustively audited.
- [sdd-workspace](upstream/skills/subagent-driven-development/scripts/sdd-workspace)
  creates a plan-specific workspace and identity marker; the
  [task-done helper](upstream/skills/executing-plans/scripts/task-done) runs a supplied
  test command and writes its log/ledger. Selected source was read. Neither helper
  was used to write project scratch state during this learning task.

All 80 tracked files within the 15 skill directories are retained, including helper
scripts and templates; copying support does not mean each was reviewed. Root
README/package/license, four hook files, Codex manifest and two packaging/sync
scripts bring the snapshot to 90 files. No upstream test/eval installation, shell/JS
execution, adversarial pressure benchmark or plugin auto-activation test ran.

## Review and evidence boundary

`verify_snapshot.py` checks pinned source bytes, source coverage, catalog entries,
license, local authored links and append-only routing. It cannot establish that a
model will follow these methods under every pressure scenario. This adapter is a
project reference index, not a claim of a certified behavioral plugin. Upstream
contributor rules apply to contributions there; no upstream contribution is made.
This repository retains its own claims, exact-SHA reviews and owner-only merge.
