# Owned lobby heroes — implementation and verification

Owner task: Issue #86. Base main `ee18105` (owner-merged map PR #85).
Branch `codex/lobby-hero-wander-drag`. Kotlin/Compose Android, oblique ground Y .72.
This is actual application code wired through `MainActivity` and `SummonerViewModel`.

## 1. Hero data and durable state

`data/model/HeroData.kt` defines the complete lobby model:

| Field | Meaning and constraints |
|---|---|
| id, name | Owned record identity; ID must be nonblank |
| rarity, level | 1–5 stars; positive level |
| currentHP, maxHP | 0 ≤ HP ≤ positive maxHP |
| atk, def, speed | Nonnegative attack/defense, positive movement speed |
| morale | Integer 0–100 |
| sanityState | NORMAL, ANXIOUS, PANICKED, SELF_AWARE |
| personality | COWARD, ARROGANT, LOYAL, LONER, REBEL |
| currentAssignment | UNASSIGNED, IN_PARTY, RESTING, SACRIFICED |
| hiddenTrait | ID/name, minimum level/morale, optional self-awareness requirement, persistent unlocked flag |

`HeroData.fromRecord(record, now)` projects the **existing** owned HeroRecord,
not a second roster. Added storage fields are `currentHp`, `sanityState`,
`currentAssignment`, `partyJoinedOrder`, `restStartedAtEpochSec`, and `hiddenTrait`.
The original combat personality enum is preserved; the lobby adapter maps old
personality names and retains the five new raw names through guest save/import.
`inActiveSquad` stays authoritative for legacy party membership. Injury/rest
status takes precedence; explicit SACRIFICED heroes cannot re-enter the world.

Old saves require no reset: absent HP is -1 (legacy full HP, or 25% during injury),
absent sanity is NORMAL, and assignment derives from existing squad/status fields.
The existing schema remains additive/backward readable. Save bounds and atomic
write/backup handling remain in LocalGuestSaveManager.

The default hidden trait is “Linh hồn bất khuất”, initially shown as **???**, with
explicit condition level ≥10 and morale ≥60. Each trait can additionally require
SELF_AWARE. Guest mutations reveal eligible traits permanently; cloud training,
meal, activity, recovery and settlement writes include the full trait map, including
legacy documents without any trait fields. A low later morale does not relock a
stored unlocked trait. This supplies the requested metadata/unlock state; it does
not invent an extra combat stat bonus. Hero details display HP, personality,
sanity, locked name and unlock condition.

Resting heals base lobby HP at `maxHP / 120` per elapsed second, clamped to max.
It uses a persisted starting timestamp, so reopening does not lose progress or
require a background timer. Repeated assignment snapshots healed HP before
resetting its timestamp. Clock rollback grants no healing. Injury eligibility
still obeys the existing recovery deadline; House/drop never bypasses it.
Existing recovery and training preserve the new HP fields coherently.

## 2. Wanderer AI

`SanctuaryLobbyEngine` retains the current waypoint graph and CollisionPhysics.
There is no replacement map, collider, renderer, shader or combat AI.

- UNASSIGNED heroes wander via existing routines and stop for pauses. Nearby idle
  heroes can stop together, face one another and exchange timed speech; loners
  opt out. The engine accepts a seeded Random for reproducible behavior tests.
- HP strictly below 30% halves the hero's **actual travel speed**. Exactly 30%
  uses normal speed. Arrival movement is capped to remaining distance.
- Morale strictly below 25 or PANICKED selects the reachable nav node farthest
  from the Tower and then stays there trembling. It does not repeatedly choose
  an unrelated facility after reaching safety.
- IN_PARTY heroes walk to four stable row slots before the Tower. Formation has
  priority over fear. Existing explicit manual orders can run to completion;
  persistent assignment resumes at the next routine decision.
- RESTING/injured heroes appear in the existing House interior/roster, not as
  outdoor wanderers. SACRIFICED heroes never spawn. Resident NPCs remain separate.
- BEING_PICKED_UP is a runtime locomotion state, **never a persisted assignment**.
  The actor's AI pauses while held. Cancellation preserves its old route and
  restores its origin; successful drop clears the old wander route.

## 3. Android touch, camera and drop persistence

`ui/components/LobbyHeroGestures.kt` gives one detector ownership of the gesture:
ordinary tap → existing selection/facility action; early movement → camera pan;
two fingers → pinch; stationary long press on an owned hero → pickup. A held
hero consumes movement, and a second finger/cancellation aborts instead of
accidentally assigning. NPCs and indoor-only residents cannot be picked outdoors.

`LobbyHeroPresentation.kt` uses the existing sprite atlas/animation renderer for
lifting, writhing, a limp bob and trembling. It preserves ground projection and
normal canopy handling. The held actor draws above scenery and below UI, with
its two possible drop zones highlighted. No new PNG or shader is generated.

Picking uses projected **upright sprite bounds**, with a 48dp minimum hit target.
Drawing, taps, dragging and drop rectangles use the same clamped viewport camera;
there is no assumption that the requested free camera equals its visible clamp.
Pointer-to-foot grab offset is retained. While held near a viewport edge, the
camera pans and the hero follows the pointer in the newly projected world.
Lifecycle STOP, leaving composition and blocked modal input cancel a held pickup.

Drop rectangles come from the actual Tower/House obstacle footprints plus their
entrance apron. No building positions or navigation geometry change.

- Tower drop assigns IN_PARTY. A fifth member evicts the **earliest joined** hero;
  stable ID ordering resolves legacy ties. Auto-joined summons receive a later
  order. Re-dropping an existing member does not evict someone else.
- House drop assigns RESTING, removes party membership and exposes the hero in
  the existing House roster/recovery flow. It never confirms a sacrifice/fusion.
- Invalid drop rolls back immediately. A valid drop freezes its actor until save
  returns. Save failure/cancellation rolls back the visual pickup and shows an
  existing banner; the screen does not grant an unsaved assignment.

`LobbyHeroAssignments.assign` is the shared pure roster operation. Guest writes
run under the existing save mutex with one atomic snapshot. Cloud writes read a
fresh constrained roster and compare the profile timestamp/revision in a Firestore
transaction; changed membership is retried with a fresh query. Removing the old
member and adding the new one commit together. Existing toggle controls keep
their “party full” rejection policy and read fresh state. Summon auto-join also
checks the current roster inside the storage operation. No stale UI count is
trusted to enforce the four-member application rule.

New optional fields and bounded hidden-trait maps are validated by
`firestore.rules`, preserving owner-only access and legacy documents. The
Firestore rules tests use a **demo project on a local emulator**, never production.
The rules are included for review; production deployment has NOT been performed.

## Verification and reproduction

Final local result: **42 Android/JVM tests PASS**, zero failures/errors/skips;
**14 Firestore emulator tests PASS**; **assembleDebug PASS**, APK ZIP CRC PASS.
Exact suite counts, APK size/SHA-256 and JUnit XML are in `evidence`.
Remote PR CI is reported separately; local results are not a remote CI claim.

Android toolchain: Java21, Gradle9.3.1, AndroidSDK36.1, Robolectric4.16.1.

```bash
gradle :app:assembleDebug :app:testDebugUnitTest --offline --console=plain \
  --tests com.example.LobbyHeroSystemsTest \
  --tests com.example.data.LobbyHeroPersistenceTest \
  --tests com.example.ui.LobbyHeroTouchTest \
  --tests com.example.ui.LobbyHeroVisualTest \
  --tests com.example.data.GuestSavePersistenceRobolectricTest \
  --tests com.example.data.LostCourierPersistenceRobolectricTest \
  --tests com.example.performance.ScenePerformanceTest \
  --tests com.example.performance.SceneFrameLoopLifecycleRobolectricTest \
  --tests com.example.ui.TowerGateMapTest \
  --tests '*LayoutResponsivenessRobolectricTest.management navigation*'
```

The touch tests inject real pointer down/move/up/cancel events into the production
Sanctuary screen, including long-press, both destinations, ordinary camera pan,
invalid drop, second-finger cancellation and persistence failure. AI simulations
verify actual displacement, complete fear/formation journeys and mutual facing.
Save tests cover restart, old JSON, overlapping assignments, summon join order,
HP on training and persistent unlock. Existing guest/rescue/map/lifecycle tests
remain in the selected regression batch. This is not a claim of a full-suite run.

Rules tests (Node ≥20, Java21, development-only npm dependencies):

```bash
cd tools/lobby-hero-rules
npm install
npm test
```

Fourteen emulator cases cover legacy reads/query, full trait migration, atomic
replacement payload, House rest, new personality creation, invalid HP/sanity/
assignment/trait/revision rejection and anonymous/cross-owner denial.

The `evidence` directory contains JUnit XML, a machine-readable verification
summary and inspected native screenshots at overview and gameplay zoom. Fixture
heroes are labeled as such; these are actual Compose/production sprite renders,
not concept art or phone captures. Two held poses differ in actual pixels.

Limits: physical Android touch/predictive Back, FPS/GPU/thermal are NOT TESTED
(no attached device). Firestore JS rules/transaction payloads run on an emulator;
a signed-in Android client against deployed production rules is NOT TESTED.
Combat settlement still exposes fallen/injury flags rather than survivor remaining
HP: this change stores lobby HP, applies 25% after knockout and preserves the
existing combat contract. It does not claim a new survivor-HP combat protocol.
Argon, A09 and an independent critical save/rules reviewer must assess the final
SHA. Codex is the author, not an independent approver. No merge or force push.
