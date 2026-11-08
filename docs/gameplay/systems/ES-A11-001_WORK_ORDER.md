# ES-A11-001 — Work order (A11 / user dispatch A011)

- Date: 2026-11-08 UTC. Scope: **read-only source audit + proposed API**, no runtime implementation.
- Task: [#62](https://github.com/lightingfff/eternal-sunmmoner/issues/62); coordination: [#39](https://github.com/lightingfff/eternal-sunmmoner/issues/39).
- Baseline: `bdff6f9976dc4927e1e095a503bc339509c7a9aa`, checkout HEAD = refreshed origin/main at claim.
- Dedicated fixed session branch: `arena/a7a501d9-eternal-sunmmoner` (platform constraint; do not create another branch).
- Actual runtime: one Arena.ai Agent Mode session; GitHub identity `lightingfff`. No native independent subagent/reviewer execution.
- Claim: [verified comment](https://github.com/lightingfff/eternal-sunmmoner/issues/62#issuecomment-6718049807). No prior claims on initial read; re-read before writing found only this claim.

## Outcome and boundaries

Future player value: show authoritative expedition/quest/reward facts in Sanctuary without claiming another reward when a notice is displayed, dismissed or replayed. **This PR changes nothing visible/playable**; it supplies an implementation gate and proposal for later owner approval.

Writer: A11 only. Exact new files locked in the claim:

1. `docs/gameplay/systems/ES-A11-001_WORK_ORDER.md` (this plan, written before proposal).
2. `docs/gameplay/systems/ES-A11-001_TOWER_SANCTUARY_API_PROPOSAL.md` (audit + presentation interface).
3. `docs/gameplay/systems/ES-A11-001_AUDIT_REPORT.md` (validation, limitations, handoff).

No-go: **all other paths**, especially runtime, tests, save schema/transactions, event ledger, SummonerViewModel, combat, renderer/camera Y=.72, Sanctuary layout/art, Gradle, Firebase, existing docs and other agents' branches. Reading these paths is allowed. General quality-loop report templates are consolidated into these three task-scoped docs, not written outside the allowed directory.

## Dependencies and ownership

- Issue #62 OPEN, body `STATE: READY` at claim. PR [#51](https://github.com/lightingfff/eternal-sunmmoner/pull/51) merged (`5c54b808c2704df8a92197b2f4dcda7f30653adc`); dual-lead PR [#65](https://github.com/lightingfff/eternal-sunmmoner/pull/65) merged into baseline.
- [#46 CP0](https://github.com/lightingfff/eternal-sunmmoner/issues/46#issuecomment-6690195127) explicitly **PROPOSED**, implementation not submitted. Adequate for this spec; **not** permission to implement events/save changes.
- Open PRs #28, #47, #50, #67 and #13 inspected for path collisions; none lists these three new docs. Do not integrate or edit them. Recheck before handoff.
- A01 owns technical integration/interface coordination; Argon creative/production review; Codex mandatory source identity/settlement/event contract authority; A09 independent QA. A04 owns future approved UI work, A03 renderer consumption. No concurrent writers.

## Acceptance rubric (locked before proposal)

| Gate | Required evidence |
|---|---|
| Scope | Exactly the three new Markdown files; no runtime/binary/config changes |
| Source truth | Baseline-relative path/line references for settlement, profile facts, quest/reward UI, notices and activities |
| Provenance | Separate merged main, unmerged PR behavior and #46 proposal; no invented implemented hooks |
| API safety | Read-only projection; no resource grant/settlement operation from UI/ack; unknown/missing data fallback |
| Idempotency/offline | Explicit Codex-owned stable source/atomic commit/restart boundaries; no exact-once rendering claim |
| Testability | Success/partial/fail/abandon/retry/restart, queue limits and guest/offline acceptance matrix |
| Evidence honesty | Real static checks only; Android build/runtime/device results NOT TESTED unless actually run |
| Review | Request Argon + Codex + A09 on exact head; REVIEW PENDING, owner alone merges |

Visual coherence, gameplay feel, tactical value, polish, performance and physical-device metrics: **NOT VERIFIED / not scored** for a docs-only deliverable. No screenshot requirement applies to this PR because no graphics/runtime are changed.

## Work and validation plan

1. Trace baseline source and focused existing test assertions; inventory facts/gaps and proposed features.
2. Write a small presentation-only API that references (does not redefine/implement) #46, including unavailable/loading/error states and no mutation permission.
3. Self-critique for five risks: false durable identity, reward-on-notice, stale/mixed data, unmerged mission facts and unsafe queue/anchor coupling. Fix doc ambiguity before handoff (max three rounds). This is author review, not independent QA.
4. Validate UTF-8, relative Markdown links and baseline source line anchors; `git diff --check`; exact changed-path allowlist and no runtime diff. Record reproducible commands/results in audit report.
5. Commit/push only this session branch, open one PR, report actual CI status and `[HANDOFF]`. No self-review approval or merge; freeze writes awaiting reviewers and any coordinator `[FIX_PLAN]`.

Local Android tool availability will be checked; no build claim based on old CI. Network allows GitHub but not arbitrary Android/Gradle download hosts. Rollback: owner may decline this docs-only PR; no game/save rollback is needed. Do not revert anyone else's work.
