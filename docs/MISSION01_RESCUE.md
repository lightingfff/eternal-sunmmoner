# Mission 01: Floor 1 rescue integration

Branch: `mission01/floor1-rescue`, based on main `a5c1b5b56541d11cf5fad02497d342d94706f714` (includes merged P23 PR #26).

This patch extends the existing Lost Courier encounter; it preserves the floor geometry, assets, oblique Y=.72 camera, caching and Sanctuary layout.

## Ownership and review

- gameplay_architect: engine command anchors, autonomous movement cancellation, tactical regression tests.
- tower_encounter_builder: branch choice/outcome policy, permanent flags, legacy compatibility and persistence tests.
- android_performance_auditor: read-only frame-loop/cache audit; no confirmed performance blocker, no hardware measurements.
- rescue_ui: live choice controls/waypoint marker, saved-result retry routing, save-gated back, UI and ViewModel tests.
- Root: encounter integration, local/cloud settlement adapters, physical pickup tests, three real starter-party playthroughs, Sanctuary consequence notes, CI and final integration.
- integration_reviewer: read-only review. Found a false support reward when supplies were missed; fixed with a distinct partial rescue outcome and regression coverage. Final code review found no further blocking code issue.

All writers shared this checkout and owned separate files. Final Gradle verification ran serially after an earlier overlapping run collided in test output files. No force push or merge.

## Player flow

Guest -> Sanctuary -> Tower Floor 1 prep -> launch -> clear path guards -> escort -> commit support/supplies choice -> intercept scout or protect -> evade captain tell and counter -> extraction -> reward/save -> return or failure preparation/retry.

- Support: one-time 60 HP bandage capped at courier max HP. First completion grants 20 mana.
- Supplies: a living hero must remain within 90 world units of the marked road cache for three continuous seconds; leaving resets collection. Successful extraction grants 60 gold on first completion.
- Extracting without collecting supplies still rescues the courier, grants no branch bonus and records a missed-supplies consequence. It never pretends support was selected.
- Separate permanent story flags survive restart and flag compaction. Rewards read authoritative saved flags inside mutation/transaction. Old rescue saves remain compatible.
- Retreat targets the real spawn rather than a movable command sigil. Escort regroup anchors on the courier. Urgent movement cancels pending contact attacks.
- Failed/pending saves cannot leave via terminal hardware back or prepare retry. Saved defeat can return to preparation; injuries and normal launch eligibility remain intact.

## Verified on the cloud worker

JDK21, Gradle9.3.1, Android SDK36.1. `:app:assembleDebug` succeeded and apksigner verified the APK's v2 signature. Asset validator passed (existing excluded preview-size warning). Final combined test run: **42 tests, zero failures/errors/skips**, 12 suites. It includes encounter, three actual starter-party simulations, physical pickup, tactical commands, policy, persistence/restart/replay, HUD, actual ViewModel retry flow, native render evidence, lifecycle and cache tests.

Reproduction (activate the configured environment toolchain first):

```sh
gradle :app:assembleDebug :app:testDebugUnitTest --console=plain \
  --tests 'com.example.LostCourier*' \
  --tests 'com.example.RescueTacticalCommandsTest' \
  --tests 'com.example.CourierChoiceInteractionTest' \
  --tests 'com.example.data.LostCourier*' \
  --tests 'com.example.ui.Rescue*' \
  --tests 'com.example.performance.ScenePerformanceTest' \
  --tests 'com.example.performance.SceneFrameLoopLifecycleRobolectricTest' \
  --tests 'com.example.PremiumSliceCombatTest.drawQueueReusesObjectsAndRespondsToMovingActorsAndSceneChanges' \
  --tests 'com.example.PremiumSliceCombatTest.assetsAndShadowsAreCachedAndFarOffscreenCastersDoNotLoad'
python3 tools/validate_assets.py
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify app/build/outputs/apk/debug/app-debug.apk
```

Do not run Gradle tests concurrently against the same checkout/build outputs.

### Deterministic new-guest results

No hero stat changes, forced enemy kills or fake transitions in these playthroughs. Uses the real starting knight/archer/mage and equipment, tactical decisions and actual simulation; then settles through LocalGuestDataSource and creates a new save manager to verify restart.

| Path | Simulation seconds | Gold | Mana | Persisted consequence |
| --- | ---: | ---: | ---: | --- |
| Support | 54.27 | 275 | 85 | COURIER_SUPPORTED |
| Supplies collected | 54.55 | 335 | 65 | COURIER_SUPPLIES_RECOVERED |
| Supplies missed | 54.55 | 275 | 65 | COURIER_SUPPLIES_MISSED |

CSV traces and inspected screenshots: [visual-evidence/mission01](visual-evidence/mission01/). Screenshots use production Compose/Android Canvas through Robolectric native graphics, **not emulator or physical-device screenshots**. Portrait/landscape choices are readable; whole-battle portrait shows the supply marker and retained camera/art. Existing visual suite also captures captain telegraph, recovery and extraction at runtime.

## Remaining acceptance limits

- **The requested 5–10-minute encounter duration is not achieved by the deterministic taught-order route.** The measured route is ~55 seconds of simulation excluding intro/decision time. Additional authored combat/pacing and human playtesting remain necessary; no artificial waiting was added to claim the target.
- Device/emulator input playthrough, physical-device FPS, GPU time, memory/thermal profiling and real Firebase integration are untested. Local CPU/lifecycle/cache checks cannot substitute for those.
- `.github/workflows/mission01-rescue.yml` adds PR acceptance checks. Cloud-worker passing checks are not a remote CI result.
- Draft PR publication depends on repository/API access. See the final chat report for the actual push/PR result; no merge is authorized or performed.

## Remote publication result

Implementation commit: `84295cd46b77aa371ec6022895eb2fb19d36af96`. Branch push to origin succeeded. `gh pr create --draft` returned `Post https://api.github.com/graphql: Forbidden`; no PR was created and remote CI is not established. Open https://github.com/lightingfff/eternal-sunmmoner/pull/new/mission01/floor1-rescue to create a draft for review. No merge performed.

## Exact changed files

- `.github/workflows/mission01-rescue.yml`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/data/LostCourierProgression.kt`
- `app/src/main/java/com/example/data/firebase/SummonerRepository.kt`
- `app/src/main/java/com/example/data/local/LocalGuestSaveManager.kt`
- `app/src/main/java/com/example/data/model/CourierOutcome.kt`
- `app/src/main/java/com/example/game/ai/AutonomousHeroAi.kt`
- `app/src/main/java/com/example/game/combat/CourierChoice.kt`
- `app/src/main/java/com/example/game/combat/LostCourierEncounter.kt`
- `app/src/main/java/com/example/game/combat/TowerCombatEngine.kt`
- `app/src/main/java/com/example/ui/SummonerViewModel.kt`
- `app/src/main/java/com/example/ui/components/CourierMissionHud.kt`
- `app/src/main/java/com/example/ui/screens/SanctuaryFacilityModals.kt`
- `app/src/main/java/com/example/ui/screens/TowerBattleScreen.kt`
- `app/src/test/java/com/example/CourierChoiceInteractionTest.kt`
- `app/src/test/java/com/example/LostCourierStarterPartyTest.kt`
- `app/src/test/java/com/example/RescueTacticalCommandsTest.kt`
- `app/src/test/java/com/example/data/LostCourierBranchPolicyTest.kt`
- `app/src/test/java/com/example/data/LostCourierPersistenceRobolectricTest.kt`
- `app/src/test/java/com/example/ui/RescueChoiceHudTest.kt`
- `app/src/test/java/com/example/ui/RescueRetryFlowTest.kt`
- `docs/MISSION01_RESCUE.md`
- `docs/visual-evidence/mission01/choice_landscape.png`
- `docs/visual-evidence/mission01/choice_portrait.png`
- `docs/visual-evidence/mission01/starter_party_RECOVER_SUPPLIES_false_playthrough.csv`
- `docs/visual-evidence/mission01/starter_party_RECOVER_SUPPLIES_true_playthrough.csv`
- `docs/visual-evidence/mission01/starter_party_SUPPORT_COURIER_false_playthrough.csv`
- `docs/visual-evidence/mission01/tower_landscape_720dp.png`
- `docs/visual-evidence/mission01/tower_scout_gameplay_400dp.png`
