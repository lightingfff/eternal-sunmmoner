# P0.5 — Performance + The Lost Courier

Sanctuary coordinates, camera ground scale 0.72, buildings, collision/navigation,
application ID, save schema and signing configuration remain unchanged. No texture
atlas was rewritten. Master remains an external command sigil.

## Implementation

- `performance/SceneFrameLoop.kt`, `ScenePerformance.kt`, `ScenePerformanceOverlay.kt`:
  one lifecycle-aware vsync loop per scene, Balanced 30/High 60 render budgets,
  fixed 60 Hz simulation, opt-in sampled CPU/frame counters. See
  [performance evidence and phone checklist](P0_5_PERFORMANCE_CHECKPOINT.md).
- `SanctuaryLobbyScreen.kt`, `TowerBattleScreen.kt`, `MainActivity.kt`: pause while
  stopped or blocked by menus; limit sampled HUD refresh; immediate direct hero
  selection; cull actor/canopy together; cache battle depth wrappers/static sort;
  preserve existing art, lighting and reduced optional-effect tier in Balanced.
  Measured top-HUD bounds keep world labels below the actual mission HUD. The
  escort camera follows the moving party/NPC instead of leaving the encounter
  offscreen at the stationary external sigil; existing pan/zoom and projection remain.
- `LostCourierEncounter.kt`, `TowerCombatEngine.kt`, `AutonomousHeroAi.kt`,
  `CombatEntities.kt`: Floor 1 guards → escort → scout → gate captain → safe gate.
  NPC waits for living nearby heroes. Protect changes formation, diverts guards
  and reduces covered NPC damage 40%; Intercept pursues the actual scout and
  sacrifices that cover. A 9-second scout window can prevent two reinforcements.
  Temporary novice fear gives -20% attack/-15% speed, at most 10 seconds, with
  faster recovery under Protect/Regroup. The captain locks a ground telegraph
  1.5 seconds before damage; Move To dodges, Focus Fire during recovery gives
  +35% damage. Existing attack wind-up/impact/recovery and project VFX
  are reused. Courier/scout flags and the warning use the oblique projection.
- `CourierMissionHud.kt`, `CourierPresentation.kt`, Tower screen: two short
  opening panels, real orders/HP/progress/fear and telegraph feedback, compact
  landscape strip, normal portrait card. Courier reuses the owned priest sprite;
  this task does not supply new premium NPC/captain animation sheets or audio.
- `LostCourierProgression.kt`, `CourierOutcome.kt`, datasource/local/cloud saves,
  ViewModel and forge UI: permanent rescue record, one bounded hero memory and
  actual forge synthesis cost 90 gold instead of 120. First unlock/memory dedupe
  uses the current saved profile; cloud escort settlement uses a transaction.
  Ordinary repeat-expedition reward semantics are unchanged. Fail/retreat permits
  retry; atomic save failure preserves the valid previous snapshot and reports
  failure rather than displaying a saved reward.

## Verification evidence

Native Android Canvas/Compose captures are in [visual-evidence/p05](visual-evidence/p05).
The CSVs record real engine commands and outcomes, without forcing damage, positions
or quest phases. The explicit healer fixture and the actual new-guest party are
separate checks. The starter check uses the real level-1 Knight/Archer/Mage and their
starter equipment, no training or invented stats; its run rescued the courier with
208/300 HP and all three heroes alive. This is a short encounter slice, not a claim
of ten minutes of new story content. Controlled simulation time is not phone FPS.

World captures reuse production rendering; full Tower portrait/landscape captures
include the real screen, opening dismissed, scene paused for a reproducible image.
`tower_scout_gameplay_400dp.png` instead advances the real screen's frame clock
20.5 seconds after starting combat, proving the actual scene loop starts, the
escort moves into view, and the scout/fear HUD and Intercept button become active.
An explicit Compose intro/finish gate avoids relying on plain engine stage changes
to restart a loop outside the HUD's separate recomposition scope.
The individual HUD captures also exercise real command callbacks. These are desktop
native-renderer test screenshots, **not physical-device screenshots**.

**NOT DEVICE TESTED:** stationary/pan/roster/UI/background phone FPS, p95, total
graphics memory, GPU/GC cost, power and thermal changes remain unmeasured. Do not
claim the heating complaint is solved. Firebase rules tests require an emulator;
local restart, stale replay, failed-write/retry and actual 90-gold crafting are
covered separately. Next smallest task: run the included same-phone before/after
checklist and a 10–15 minute play/heat soak, then tune only measured bottlenecks.

Install the direct APK with `adb install -r` to retain the save. The portable ZIP
contains that APK plus its checksum; the Actions artifact is an additional outer
ZIP. Stop on signature mismatch instead of uninstalling or deleting the save.

## Final build results — 2026-11-02

- `:app:assembleDebug :app:testDebugUnitTest`: **BUILD SUCCESSFUL**, 86 tests,
  **80 passed, 0 failed, 6 skipped**. The six skips require Firebase Emulator.
  Existing art/material/projection, responsive layout, save/backup and gameplay
  regressions passed alongside new clock/lifecycle, settings, real escort, orders,
  fear, route, starter-party, save-failure/retry and forge tests.
- `python3 tools/validate_assets.py`: PASS; existing oversized preview warning
  remains (preview excluded from APK). No new raster assets or atlas changes.
- `tools/package_apk.py`: APK entry/CRC, portable ZIP CRC, embedded complete APK
  byte/hash comparison, classic STORE ZIP and SHA256SUMS verified.
- `apksigner verify --verbose`: PASS, APK Signature Scheme v2. Existing debug
  signing configuration reused; no physical installation or Actions certificate
  matching is claimed.
- Direct APK: `app/build/p05-download/Eternal-Summoner-debug.apk`, **34,678,065 bytes**.
  SHA-256: `38523b2e8cf43f762fa1f37711bbdbd5ac24ca87777fe0b69a1b916cef7b804b`.
- Portable archive: `app/build/p05-download/Eternal-Summoner-Android.zip`;
  checksums: `app/build/p05-download/SHA256SUMS`. Generated binaries stay outside Git.
