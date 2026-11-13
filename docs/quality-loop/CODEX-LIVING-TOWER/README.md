# Living Tower exploration prototype — Issue #93

This replaces the runtime Floor1 click checklist with an autonomous party investigating independently selectable leads. It is a functional prototype for the owner's gameplay review, **not evidence that the game is now fun or that the5–10-minute pacing target is met**.

Base: `9150c0040a1a3160fb4959d9fbd835028420d2b6`, main after the owner merged PR92. Branch: `codex/tower-living-exploration`. The previous PR branch and all other agents' branches remain untouched.

## Playable differences

| Encounter | What the heroes actually do | Master choice and consequence |
|---|---|---|
| Northern watchpost | A scout physically moves to a signal switch while the others hold two normal guards; regroup is required. | Scout and silence the signal, or assault three defenders. The signal determines a real additional archer at the escort crossing. |
| Southern distress call | A wounded witness has80/120HP, takes real enemy contact/projectile damage and physically follows the protecting party to a safe junction. | Protect/extract the witness for information and40 courier ward, or use the single medicine for35% squad healing and leave him vulnerable. The medicine requires a living hero at the scene. |
| Eastern rune gate | Two distinct living heroes occupy different pressure plates concurrently. A solo survivor can latch two auxiliary levers sequentially. | Cooperate quietly, or break the lock and fight two normal raiders; a forced lock also brings an additional normal guard alongside Kraag. |

Any two resolved sites provide enough information to choose evacuation. The third site can remain unknown/unresolved, or Master can investigate it. Everyone must physically return to camp before the original courier escort begins. There is no global requirement to defeat every exploration enemy. Original scout interception, fear/agency, captain telegraph/counter, actual extraction, support/supplies/partial outcomes and paid House care remain.

The party selects useful exploration roles from the actual living roster. A fallen scout's task transfers to a surviving actor. Master HOLD/MOVE/FOCUS/REGROUP/RETREAT and world commands suspend autonomous steering; it resumes only explicitly, including when returning to camp. Unavailable lead changes are disabled. Decisions never time out into an automatically accepted reward or victory.

Sites reveal their contents when heroes reach them; unseen enemies are not exposed on the battle view/minimap or directly selectable. Hero conversations identify the speaker/intent. Normal short-landscape HUD occupies about105dp; decisions temporarily replace navigation controls rather than adding another panel. New controls remain at least48dp, with at least96dp of battlefield above the bottom controls in tested phone views. During split exploration, the scene frames the party with the existing `.72` projection; manual zoom/pan remains available and explicit follow restores framing.

Legacy journey behavior remains behind the existing/internal constructor paths for compatibility tests. Runtime Floor1 enables the new mode; both controllers cannot run together. Legacy automatic shrine/chest rewards are disabled in the new mode so the old free proximity heal cannot erase the medicine decision.

## Actual evidence

The three CSVs use the untrained three-hero guest roster, real equipment, public runtime commands, actual collision/movement/attacks, original scout and captain, actual exit, persisted remaining HP, one-attempt receipt replay and restart. No actors are teleported, forcibly killed, healed or assigned an outcome by these route fixtures. The controller and combat spatial tests are separately labelled; they are not presented as playthroughs.

| Actual optimized route | Simulation seconds | Moving seconds | Aggro seconds | Outcome |
|---|---:|---:|---:|---|
| North scout → East cooperation |89.94|79.57|12.90|RESCUED|
| South rescue → North assault |91.31|80.72|11.62|RESCUED|
| South medicine → East forced lock → supplies detour |87.25|79.39|32.32|SUPPLIES_RECOVERED|

Moving and aggro counters overlap; they are not additive categories. The route controller reacts at simulation-tick speed and requests available manual skills; this is not a human pace measurement. Walking still dominates. The earlier5–10-minute goal in #91 remains open; this patch changes agency/interactions, rather than claiming sufficient duration by adding waits, HP or empty travel.

Native UI tests physically select leads and decisions, issue HOLD/resume and inspect actual screens at400×800,800×360 and existing360×640/800×360 layout regressions. They include the real South rescue and split scout action, not injected quest state. Root inspected the screenshots. The [scout sequence](evidence/scout-role-sequence.mp4) is eight unmodified native Compose test captures at400ms test-clock intervals, encoded at2.5frames/s; **it is not a device recording or FPS measurement**.

Local verification: **137 distinct JUnit tests passed, zero failures/errors/skips**, including22 living-exploration/NPC tests,35 legacy courier/commands/receipts/launch tests,70 broader world/House/health/speech/retry tests and10 final UI/layout tests. The original courier world/native UI coverage was rerun after final framing/presentation changes. `:app:assembleDebug` passed. Local APK reference compatibility with the previous local APK passed; that signing channel differs from the established CI channel, so distribution must use the separately verified CI APK.

CI runs the existing legacy routes, the22 new mechanics/control/actual-route/NPC cases, physical UI, original escort/world compatibility, enforced-rule Node tests and real Auth/Firestore settlement concurrency/restart. Test result batches and native evidence are retained separately. Actual CI run results and exact head are reported on the PR; they must not be inferred from this document.

## Corrections from tests and independent review

- Revealing South from a fast scout alone spawned wolves while the frontline was too far away. Reveal now requires frontline proximity; a nearby guardian draws melee attacks. Real ranged attacks and physical projectile interception remain possible. NPC HP/damage were not weakened.
- A mutable controller passed to Compose could leave the decision HUD permanently stale under strong skipping. The HUD now consumes value snapshots; physical decision and64ms HOLD/resume tests cover refresh.
- Interrupted autonomous return previously could not resume. Explicit resume works on the existing return path; switching to another site during return remains blocked.
- Stale remote medicine use and simultaneous witness/whole-squad death cannot grant healing, resume autonomy or award source knowledge.
- Expired conversations stop producing TTL copies, history stays bounded at8 with at most3 active lines, spawn IDs are one-shot, and no per-frame image loading was added.
- Compact landscape layout preserves the previous <30% normal-HUD bound; split-party framing and witness label reservation were checked on actual renders.

## Limits and ownership

No Sanctuary renderer/shader, shared camera projection, map/art asset, Android package/manifest/Gradle/signing/entry point or shared save schema changed. Existing source files touched are `LostCourierEncounter`, `TowerCombatEngine`, `TowerBattleScreen`; new controller/content/models and UI/presentation stay separate. Existing tests were adapted to traverse the new runtime flow instead of expecting the removed checklist; their original escort/scout assertions remain. CI changes add checks without changing signing or deployment.

No physical Android device or hardware-accelerated Android emulator is available (`adb` lists no device; `/dev/kvm` is absent). Device install, FPS/thermal, real touch latency, lifecycle stress and human enjoyment/duration remain **NOT TESTED**. Native-host automation is not a substitute.

Existing settlement limitations remain: local receipts retain32 results, and production Firestore receipt rules were not deployed by this task. New site history uses existing bounded discovery/story flags; it does not introduce an indefinitely retained NPC roster or mid-battle resume save. No production deployment, main push, force push or merge is performed. Independent native technical review and requested Argon/A09 review target the published exact SHA; owner alone accepts gameplay and merges.
