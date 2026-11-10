# Lobby speech and transactional House repair — Issue #89

Base: `main e3bae5797d760f2f5c77c14611c9cb406c1269b5` (owner-merged PR #88).
Branch: `codex/sanctuary-house-speech-fixes`. Review/merge belongs to the owner.

## Result

The existing owned HeroRecord, Lobby engine, House modal and data sources are reused.
There is no second roster, Hero manager, input controller or save system.

- Physical sprite hit testing takes priority; accessibility padding yields to a building.
  Ordinary and stationary long-held House/Gate taps remain usable. Accepted House drops open management.
- Owned speech expires after 3.5 seconds, including held heroes. Ambient speech waits
  6–10 eligible idle seconds; player/context dialogue wins over routine conversation.
  The existing 31-line Vietnamese catalog supplies personality, injury, assignment and sacrifice lines.
- Social disdain is a directed proximity encounter with entry/exit hysteresis and cooldown,
  followed by a fresh atomic -2 morale write. Stable UUIDs and a bounded 32-entry ledger
  prevent repeated deductions after lost acknowledgements. Three failed attempts show feedback
  and reconcile the actor with authoritative owned data; an offline cloud commit is not promised.
- House has Dormitory, Infirmary/Canteen and Synthesis tabs. Selection survives stat updates;
  deleted heroes and stale destructive confirmations are invalidated. Both Lobby and
  **Tower → Squad Preparation → House** use the same real handlers.
- Treatment costs 80 Gold and adds 50% base MaxHP; food costs 65 Gold and adds 40 morale,
  clearing anxiety/panic while preserving self-awareness. Full/no-benefit payments are rejected;
  full-HP injury clearing is an explicit paid treatment. Resting does not refill HP or morale.
- Fusion requires 1–3 distinct owned materials, excluding the target. It permanently removes
  them and unbinds their gear, transfers floor(20% of summed base stats), grants the existing
  `sum(65 * level + 35 * rarity)` EXP, deducts 10 morale from surviving Cowards and has a
  20% hidden-trait inheritance roll. Shared preview/outcome calculation reports actual gains
  at existing stat/EXP caps. Increased MaxHP does not heal current HP.
- Low-rarity appraisal requires three actual survived expeditions or an actual surviving
  return below 15% HP. Dedicated progress fields replace truncated story-text counters;
  legacy tags and revealed generic traits migrate without deleting progress or custom traits.
  Traits affect crit chance (+30 percentage points, 1.5× damage), panic immunity, ATK/DEF or SPD.
- Combat starts with saved HP scaled for equipment/feast and returns a detached per-hero
  HP ratio through all three finish paths. Level-up MaxHP growth grants no free current HP.
- Guest writes serialize read/transform/write with settings, fsync temporary data and atomically
  rename in the same directory. State publishes after commit; backup recovery remains available.
  Cloud House operations read fresh profile/roster/gear before transaction writes. Profile
  revisions protect query membership, including equip/unequip. Fusion keeps its RNG seed on retry.

## Android compatibility

`applicationId` remains `com.aistudio.eternalsummoner.kvrpzm`; launchable Activity remains
`com.example.MainActivity`, versionCode 1/versionName 1.0. Manifest, both Gradle build files,
settings, version catalog, wrapper and signing configuration are byte-identical to the base.
MainActivity has only three additional callback bindings; its entry point is preserved.
No renderer, shader, map, art asset or other agent branch changed.

The actual earlier installation mismatch is between signing channels:

| APK channel | Signer SHA-256 |
| --- | --- |
| Earlier local PR87 APK | `5d6c1e109e50cf7ae057ea4728602447fe50e68b43385c50cb04aa743d9193f8` |
| PR87 CI and main releases 359/363 | `9d179477ce35520d6af58f29864c94d60ae4c8ca283e84541cd08192534a8b2b` |

Cross-channel replacement is incompatible even with the same package. No keys were regenerated,
and no uninstall/data-reset workaround is required by this code. Source/DEX audits found no
duplicate definitions in the inspected baseline artifacts; the candidate duplicate-class build check passes.

Use `python3 tools/verify_apk_update.py --baseline installed-channel-reference.apk --candidate candidate.apk`.
The guard checks package, version, entry Activity, ZIP CRC, signature and signer equality without
installing/modifying APKs. Final CI APK must match the release signer before distribution as its update.
Device installation remains unverified because `adb devices -l` has no attached devices.

## Verification

Selected checks are split to respect the repository's unchanged two-minute test-task timeout.
The JSON summaries record exact suites/counts. These are selected regression checks, not full-suite certification.

- `gradle :app:checkDebugDuplicateClasses :app:assembleDebug` — PASS.
- Core: House rules, speech, combat health/crit, Lobby systems, JSON persistence, concurrent
  mutations, failed disk write and social acknowledgement/reconciliation — PASS.
- Native Compose: House portrait/landscape, exact cap preview, stale confirmation,
  Hero drag/pan/pinch/cancel, padding near Gate, House-drop opening and actual paid House
  operations through Squad Preparation — PASS.
- Existing guest startup/restart, rescue branches/retry, combat, management and frame lifecycle — PASS.
- Actual Android/Kotlin `SummonerRepository` against local Auth/Firestore: stale/concurrent paid
  care, atomic synthesis/gear removal, social retries, guest import, rescue idempotency,
  anonymous/cross-owner denial and existing progression — PASS, zero skipped in the rules-loaded run.
- Rules/SDK harness tests positive and rejected schemas, full 32 UUID entries, malformed tails,
  atomic transaction payloads, ownership, gear query shape and membership revision drift.

Reproduce native cloud checks with Firebase CLI 14.22.0, the existing Gradle 9.3.1/JDK 21 toolchain:

```sh
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Deternal.test.firestore.database=(default)"
firebase emulators:exec --config firebase.house-test.json --project demo-no-project --only firestore,auth \
  "gradle :app:testDebugUnitTest --tests com.example.data.firebase.SanctuaryCloudHouseTest --tests com.example.data.firebase.SummonerRepositoryRuleTest"
```

The override is test-only. A single default demo database is deliberate: this CLI cannot load
the earlier multiple-database test configuration safely. Permissive-emulator results are excluded.
The existing unauthenticated Flow test now ignores an initial empty cache snapshot and waits
for unauthorized data or the real server denial; it still requires PERMISSION_DENIED.
For JS checks, install `tools/lobby-hero-rules/package.json` in an isolated runner, set
`LOBBY_RULES_PACKAGE` to that package path, then run its existing emulator test command.

The six House images in `screenshots/` were captured from the production Compose modal using
Robolectric native graphics and fixture records at 800×360/360×800 dp, then opened and inspected.
They prove UI rendering/interaction in that harness, not phone GPU performance. The held-Hero
image uses the actual Lobby renderer with a fixture owned hero.

## Review and limits

Independent native Codex read-only review caught query ownership, gear races, missing Squad
callbacks, optimistic-state reconciliation, cap display and malformed metadata issues. Corrective
writers fixed them and retested. The author cannot independently approve this PR. Argon visual,
A09 independent QA and owner acceptance remain pending on the exact final head SHA.

Physical Android installation, touch feel, GPU/FPS/thermal and signed-in production Firebase
remain NOT TESTED. Rules were tested locally and **not deployed**. Other open PRs (#28/#47/#50/#67)
were not merged or modified; shared combat/save/UI paths require serial integration review.
