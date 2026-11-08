# ETERNAL SUMMONER — Arena A01 + Gemini 4 Argon Dual-Lead Studio v2

**Authority:** human Game Director approves scope, major aesthetic and gameplay choices, and merges PRs. This is an operational policy, not automatic orchestration. **Keep all twelve A01–A12 roles and their existing Issue #52–#63 assignments.** Codex remains the senior critical-systems engineer; A09 remains independent QA. Argon is an **extra co-lead lane**, not a renumbering or replacement of any Axx agent.

## 1. Split the leadership load — approximately 50/50, not simultaneous ownership of the same files

| Arena Argent A01 — Technical & Integration Lead | Gemini 4 Argon High — Creative & Production Co-Lead |
|---|---|
| Kotlin/Compose/Canvas architecture, Android build/runtime integration | Pixel-art world quality, texture/atlas art direction, animation/VFX and environmental readability |
| Runtime renderer interoperability, reliable touch, battery/heat/frame pacing | Premium visual language, lighting/shadow look, atmosphere, storytelling, UI visual clarity |
| Assign implementation to A02/A03/A04/A09/A10/A11; reconcile shared interfaces | Assign creative tasks to A05/A06/A07/A08/A12; coordinate A03/A04 visual requests with A01 |
| Review technical contracts, scope/CI/build, own integration branch | Review genuine game screenshots/playthrough, visual consistency, gameplay pacing and art deliveries |
| Resolve runtime file collisions and maintain integration checkpoints | Prepare independently verifiable original art/animation assets and task-scoped design/implementation PRs |

**Equal standing for proposing/splitting work; not equal permission to override each other.** Shared files and technical API changes require a single named writer on a task issue and explicit co-lead handoff. A01 integrates shared Kotlin runtime; Argon can implement a standalone task or provide a patch to the authorized writer, with full tests. A08 remains the asset integration/manifest QA owner; Argon may author designated new assets, not silently overwrite validated existing PNGs. A03 owns renderer integration; A06 and Codex retain combat core boundaries.

## 2. Argon is allowed to DO WORK, not just advise

With an actual GitHub-connected coding environment and authorization, Argon can: design original Floor1 mission/story; build compliant texture/animation/VFX deliverables; prepare visual QA tools/docs; draft and implement isolated UI/art integration code; fix isolated noncritical issues; submit an independently scoped PR with real evidence. For generated images/assets, require actual PNGs and reproducible provenance, sizes, SHA256/alpha/tile/frame validation, 64×64 pixel density, atlas/anchor/lighting compatibility and visual references in original style. An attractive still image does not automatically satisfy a working spritesheet, transparent file, interactive object or Android rendering contract. Do not substitute mockups.

Capabilities are conditional: Gemini chat may not have GitHub write/file generation; AI Studio Build may have repo access subject to product, workspace and permissions. Verify actual tools, app subscription vs API access, file operations and execution. Otherwise report `BLOCKED_NO_WRITE_ACCESS` and supply a design/review or user-transferable file, never fabricate a pushed branch or successful build.

## 3. No duplicate work — GitHub task lease convention

**Task IDs:** unchanged A01–A12 issues. New Argon-owned work uses `ARGON-WORK-###` issue under #39, STATE BACKLOG/READY, distinct paths, dependencies, acceptance and reviewer. Existing A08 asset task #59 is *still blocked* without verifiable P31-A3 archive or a newly authorized **fresh original replacement** scope. Never claim inaccessible Gemini-local archive is integrated.

`[LEAD-ASSIGN]` in the issue records: accountable Axx or ARGON, exact paths, baseline main SHA, branch, expected interface, why one writer, reviewers, expected asset/code evidence. Only owner-approved `READY` issues can be `[CLAIM]`ed. A01 and Argon do NOT both edit a shared renderer, manifest, Gradle, battle system or save file. If work needs the same file, stop one side; first PR is reviewed/merged (owner), second rebases from new main or a single integrator owns the write.

An agent session is not continuously running simply because its name appears in an issue. True GitHub poll/auto-claim requires separate automation, authenticated write access and a collision policy. Comment time alone is not an atomic claim lock.

## 4. Universal two-model review of substantive PRs

Every substantive implementation/content PR by A01–A12 **or Argon** gets:
- `[ARGON_REVIEW]`: design, original artwork/animation/lighting, gameplay feel, UI clarity, integration risks; inspect actual files, screenshots and issue acceptance. For pure build or critical combat PR, still flag high-level regressions, mark untested rather than fake expertise.
- `[CODEX_REVIEW]`: Kotlin/API correctness, CI and regression risk, state architecture, concurrency/saves, Android performance contracts, security/scope. For docs/art-only PR, lightweight static review is enough; deep focus reserved for combat/save/event/architecture.
- `[QA_REVIEW]` by A09: actual check results and reproduction; A03/A08/A10 extra specialist checks when relevant.

All reviews target the **same exact head SHA**. Separate READ-ONLY reviews can happen in parallel; concurrent writes to the PR branch are disallowed. Neither model's positive comment is a substitute for CI, real in-engine visual evidence, physical device measurements, or owner merge. If either reviewer finds P0/P1 blockers, the PR stays REQUEST_CHANGES. For P2, A01 + Argon + owner decide defer vs fix, with reasons.

### Review → fix → re-review, safely

1. Writer posts `[HANDOFF]` with PR, exact SHA, diff, tests/art evidence; then freeze writes while both reviewers evaluate.
2. Both post `[ARGON_REVIEW]` and `[CODEX_REVIEW]` with severity and reproducible findings. A09 posts `[QA_REVIEW]`.
3. A01 or Argon consolidates duplicate comments into `[FIX_PLAN]`: issue IDs, exact approved paths, **one corrective writer per file**, fix ordering, baseline SHA, acceptance tests. Nobody edits until ownership is explicit.
4. Original writer normally fixes in own PR branch. Argon/Codex **may implement a fix** only when explicitly assigned a nonoverlapping file set in their own branch/patch PR; original writer/integrator applies it serially. No two remote agents push to the same PR head concurrently.
5. New commit invalidates former reviews. Both reviewers check the **new SHA** (full repeat for P0/P1, focused repeat for P2), A09 re-runs impacted tests. Reviewer who authored a fix cannot provide the only approval on that fix; another independent reviewer must sign.
6. Max 3 correction cycles; remaining blockers -> `BLOCKED`/draft PR, never fake DONE. Only **owner** merges.

### Decision matrix

| PR type | Argon | Codex | A09 |
|---|---|---|---|
| Pixel art / atlas / shader look / animation | Full visual + artifact review | Lightweight integrity/API review | Decode/manifest/runtime evidence |
| Kotlin renderer / UI / Android performance | Visual and UX + cross-module review | Full technical review | Build/test/regression |
| Gameplay mission / monster behavior | Deep game-feel + design review | Full mechanical review when combat affected | Simulation + playthrough |
| Core combat, event IDs, save schema, transactions | Independent gameplay impact; **no solo core approval** | **Mandatory final critical-system review** | Unit/integration/restart tests |
| Docs/task planning | Scope and clarity review as needed | Lightweight review | Consistency |

## 5. Project invariants

Android native Kotlin + Compose Canvas offline single-player, existing guest data compatibility, ObliqueCamera ground Y=.72, Sanctuary footprints, island/plaza, roofs/occlusion, true 64px pixel assets, independent props, Master off-map, first Tower Floor1 meadow, 5–10min actual fun not artificial waiting. Preserve existing valid art unless verified defects and owner approves replacement. No destructive git cleanup/force push/direct main writes/secret exposure. High visual quality with measured culling/caching before downgrades; no made-up FPS/heat, fake sprite animations or fabricated test reports. Never imitate proprietary copyrighted panels/art/audio.

## 6. Startup prompts

- **A01:** read `AGENTS.md`, `docs/agents/START_HERE.md`, `docs/agents/DUAL_LEAD_WORKFLOW.md`, `docs/agents/CLAIM_PROTOCOL.md`, `docs/agents/REVIEW_GATES.md`, role A01 skill and Issue #52, plus live #39 comments. Welcome Argon as **parallel creative lead**, keep Android integration leadership, coordinate file owners through comments. Do not start new implementation unless issue READY and no collision.
- **Argon:** read `docs/agents/ARGON_START_PROMPT.txt` (copy text into actual Gemini/Studio session) and `.agents/skills/eternal-argon-creative-technical-lead/SKILL.md` once on main.
- **Codex:** read this document and `docs/agents/REVIEW_GATES.md` before reviewing a substantive new PR. Keep #47/#46 high-risk work priority; do not co-edit Argon/agent branch simultaneously.
