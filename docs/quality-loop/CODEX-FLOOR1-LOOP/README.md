# Floor 1 playable-loop integration — Issue #91

**DRAFT / pacing acceptance remains open.** This implements a functioning loop and reliability fixes; it does not establish the requested 5–10-minute experience. Do not merge on the strength of passing automation alone.

Base: `main` after owner merged PR #90 (`dc3a9b1`). Integration branch: `codex/floor1-playable-loop`. PR #28 remains a separate, unchanged draft branch. Selected encounter, command and HUD improvements were ported from its `09c553d81c53046fe1038b70c7046e2a6cff0661` head; its expanded map, generated artwork, Android configuration and evidence were not ported.

## Implemented behavior

- Actual existing meadow paths: safe-camp letter, northern map/tracks and wolf/archer encounter, signal choice, southern medicine/raider encounter, camp departure, crossing and original scout/guardian escort. Normal archetype HP, attack and speed; finite roster of at most 16 spawned enemies. No duration gate, repeated respawn or actor slowdown.
- Signal destruction removes an additional crossing archer; keeping bandages heals standing heroes by 20%. The single medicine allocation either absorbs 60 actual courier damage or heals standing heroes by 35%. Neither choice repeats.
- Support, recovered supplies and rescue without the chosen supplies remain distinct outcomes. Supplies require explicit living-hero collection off the escort road, with cancellable navigation over existing southern road corners and a return route. Protect/intercept/manual orders interrupt that navigation.
- All standing heroes must physically extract on retreat. Urgent movement can cancel an attack windup. Generic enemies attack actual heroes/courier instead of a human Master. The existing non-courier Master bolt behavior is preserved.
- UI acknowledges commands/skills/taps immediately and blocks underlying input during intro, pause, retreat confirmation and terminal settlement. Camera follows the actual exploring/diverting squad, retaining the existing `.72` projection. Return/retry preparation waits for a saved result. Save retry remains available after transient error banners disappear.
- VM freezes the launch squad/floor and UUID v4; duplicate deployment callbacks cannot replace an active attempt. Zero-HP heroes remain ineligible even after their injury timer expires.
- Atomic guest JSON stores 32 complete settlement receipts; same retained attempt replays its result without XP, Gold, drops, HP, morale or survival-count mutation, including after House treatment/restart. **This is a 32-receipt retry window, not indefinite exactly-once storage:** an evicted old ID is treated as new if explicitly resubmitted. Production UI does not offer old attempt IDs.
- Cloud transactions read immutable owner-scoped receipts before all writes and commit the receipt with the result. Concurrent duplicate requests/repository restart credit once. This keeps the existing client-authoritative reward model; it does not add a trusted anti-cheat server.
- Actual combat HP persists on return; paid House healing still costs 80 Gold. Original prologue/expansion/courier flags and the latest meal token survive accumulated journey flags.

## Evidence and acceptance

The two CSVs use the actual untrained three-hero guest roster, equipment, collision, movement, damage, scout warning, captain telegraph and public input actions. No actor teleport, forced enemy death, HP edit or outcome assignment is used in these playthroughs. They also check settlement/restart, actual remaining HP, selected paid treatment/restart and unchanged same-attempt replay after treatment. Spatial unit fixtures are explicitly labelled and are not playthrough evidence.

| Automated route | Simulation seconds | Walking seconds | Combat seconds | Result |
|---|---:|---:|---:|---|
| Support, silence alarm, courier medicine | 82.72 | 72.15 | 17.03 | RESCUED |
| Supplies, spare bandages, squad medicine | 85.54 | 77.90 | 20.52 | SUPPLIES_RECOVERED |

Walking/combat counters overlap: movement and combat may happen in the same tick. They are not additive time categories. The controller reacts at simulation-tick speed, including manual skills; these times are not human playtest measurements. Initial main's taught-order guest route is approximately 55 seconds. The added authored choices and interactions are functional, but this evidence **does not satisfy 5–10-minute pacing or sufficient encounter density**. Adding idle time, inflated enemy HP or a larger empty route would not close that gate. Further distinct mechanics and an interactive human playthrough are required.

PNG evidence is produced by the production Compose screen/Android Canvas through Robolectric native graphics on the cloud host, not a phone or installed Android emulator. Portrait fieldwork uses actual starter-party movement and UI actions. Physical input tests cover 400×800 portrait, 800×600 landscape and 800×360 short landscape, immediate feedback before the 250 ms periodic HUD sample, and touch/keyboard blocked by a modal. Root inspected the images. No Sanctuary renderer, shaders, artwork, map, camera projection, application ID, manifest, Gradle/signing configuration or activity entry declaration changed; MainActivity only wires the retry callback.

Validation commands are encoded in `.github/workflows/floor1-loop-tests.yml`. Separate batches retain route and UI XML reports, native images and CSVs. Enforced-rules tests use `firebase.house-test.json`, Auth/Firestore emulators and `-Deternal.test.firestore.database=(default)`; they must not silently use an unconfigured named database. The additional Node security suite passes 48 cases, including receipt ownership, UUID/field/size/timestamp validation, immutable receipts and existing House rules.

The local Android build uses the workspace's existing signing key. Its key is distinct from the cached CI update channel; do not present that local APK as a compatible update for the CI APK. Compare the **CI candidate** with the known CI baseline using `tools/verify_apk_update.py` before distributing it as an update. No key/configuration is committed here.

## Open gates

- Meaningful 5–10-minute human pacing: **NOT MET by current evidence / NOT DEVICE TESTED**.
- Real device install, touch latency, FPS, thermal behavior, long-session memory and offline/online lifecycle interruptions: **NOT TESTED** (`adb devices` lists none).
- New receipt Firestore rules: tested locally, **NOT DEPLOYED**. Cloud rollout requires the corresponding rules before the new client; production deployment is outside this change.
- Independent Argon experience review and A09 QA on the published exact head: **PENDING**. Native delegated technical reviews found and prompted fixes for camera tracking, duplicate deployment, lost prologue/meal flags and the transient-banner save softlock. A writer does not self-approve.
- Owner alone may merge. This document and passing CI do not mark Issue #91 complete.
