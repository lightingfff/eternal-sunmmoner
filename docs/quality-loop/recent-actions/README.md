# Recent Actions recovery — 2026-11-18

Task: [#128](https://github.com/lightingfff/eternal-sunmmoner/issues/128).
Base main: `b792f34d26fbaffc1944acf17eee89787404a586` (owner-merged PR126/127).
Branch: `codex/fix-recent-actions`. Review the exact HEAD recorded in the PR/issue;
this report is not a merge or physical-device acceptance.

## Reproduced failures and bounded corrections

Main APK [44308903554](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/44308903554)
and Floor1 [44308779819](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/44308779819)
fail in production Kotlin compilation. Local unmodified-base compilation reproduced
all five messages. Add the missing `height`, runtime delegate `getValue` and
`FantasyChoiceButton` imports; remove the nonexistent top-level `matchParentSize`
import (the two uses correctly resolve in `BoxScope`). Once production compiles,
all-test compilation exposes missing Compose touch `click` imports in P28/P29 tests.

Install smoke [44308779794](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/44308779794)
correctly refuses its failed exact-SHA APK producer. These four failed jobs never
installed an APK. Preserve the producer/hash/package/signature and installer guards.

Executing the existing Floor1 tests then exposes real UI contract regressions:
noncompact LivingExpeditionHud changed automation-state copy; compact/narrow padding
pushes the battle HUD past its existing size limits. Restore the established state
labels and reduce padding, retaining every action, authored frame and 48dp control.
Remove the extra 2dp top/bottom padding from the idle Lobby dock. Existing coverage
limits remain 26% idle / 45% expanded; measured results are **24.8611% / 34.2361%**,
previously 27.0833% / 36.4583%. Add a five-destination minimum-48dp assertion and
print measured bounds for future diagnostics. No coverage threshold was relaxed.

The Back test independently also fails on the older `b026689` baseline. Temporary
diagnostics confirmed a RESUMED host, enabled callbacks and one real Back dispatch
changing `showMasterHub` from true to false; only the manual-clock semantics tree
was stale. The test now requires the actual Back owner and sends snapshot apply
notifications before advancing its manual clock. It still dispatches exactly once
and asserts that the real modal disappears. Temporary reflection was removed;
production Back handling is unchanged.

## Local verification

JDK21, Gradle9.3.1, Android SDK36.1 / build-tools36.0.0. Production and all debug test
Kotlin compile; `gradle :app:assembleDebug` passes on the final code. Run each actual
Gradle test command from `.github/workflows/floor1-loop-tests.yml` separately to
retain the existing per-task timeout. XML totals from that execution:

| Workflow group | Tests | Result |
|---|---:|---|
| actual_routes | 24 | PASS |
| living_expedition_tests | 37 | PASS |
| tower_reforge_tests | 26 | PASS |
| ui_regression_tests | 27 | PASS |
| world_compatibility_tests | 7 | PASS |
| shared_ui_tests | 28 | PASS |
| Total | 149 | 0 failures, errors or skips |

`python3 -m unittest discover -s tools -p 'test_*apk*.py' -v`: **19 PASS**.
The shared P28/P29 group is now mandatory in CI; its JUnit results and P28/P29 native
captures are retained with the existing evidence artifact. The existing route,
living, combat, physical-input, world, rules and cloud gates remain mandatory.

Final local APK: 48,059,809 bytes, SHA-256
`6f3da9932d6cbf79a781f5aec427b5d1efa4bb760836a52de81f25128f578aeb`.
`verify_apk_update.py` against the previous local APK passes signature, CRC,
package/version/launcher/ABI retention; `package_apk.py` packaging and verification
pass. The retained local certificate starts `b6ae9520`; CI uses its existing
`9d179477` certificate. This local test APK is not offered as an update to CI builds.

`git diff --check` and workflow YAML/unique-step/filter checks pass. CI results and
actual artifact/install evidence are reported on the PR and task issue after runs
complete; local tests do not establish CI success.

## Remaining limits

Historical manual Lyra [43715843735](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/43715843735)
fails the P26C E4 world-exposure comparison (8.917%, 9.413%, 8.252% against a strict
8% accepted-art baseline). This is a separate visual gate, not a Kotlin build error.
Do not lower that threshold, accept new image baselines or change the renderer just
to turn it green. Latest main already runs Lyra manually; no Lyra/workflow threshold
or art baseline is changed by this repair.

Package ID, SDK/Gradle configuration, manifest, entrypoint, signing configuration,
key, gameplay/save/event contracts, renderer/shaders, map and artwork are unchanged.
Native Robolectric captures are not phone/system-bar/FPS/thermal measurements.
Fresh installs are verified separately by the existing CI matrix; the owner's
Samsung installation issue remains unproven without that device's result.
Independent Argon/A09 review is requested on the PR; the author does not self-approve.
