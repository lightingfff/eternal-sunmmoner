# P0.5 — Performance checkpoint (2026-11-02)

**NOT DEVICE TESTED.** No Android phone/emulator is attached. These changes establish
bounded work and an instrumented phone build; they do not prove sustained phone FPS,
GPU cost, memory pressure, battery use or lower temperature.

## Evidence and bounded fixes

| Finding in baseline | Source | Change |
| --- | --- | --- |
| Lobby had one vsync loop, but no lifecycle/modal guard and no selected render budget. | `SanctuaryLobbyScreen.kt`, original `LaunchedEffect(Unit)` | Shared `SceneFrameLoop` runs only while lifecycle is RESUMED and scene/modals permit it; default Balanced 30 FPS, optional High 60. No busy loop. |
| Rendering frequency also controlled simulation steps. | Lobby update loop | A fixed 60 Hz simulation consumes elapsed render time, so both render modes advance identical logical ticks. Resume excludes paused time. Foreground stalls cap catch-up to 250 ms to prevent a spiral. |
| Lobby's frame/animation reads were already inside Canvas. | Lobby Canvas | Preserve draw-phase state reads and remove redundant frameTick; sampled hero-status HUD refresh is at most 4 Hz, its wall clock at most 1 Hz. Direct selection/dismissal remains immediate. Do not attribute baseline Lobby stutter to broad per-frame recomposition without device evidence. |
| Offscreen characters were culled by the sprite renderer, but their canopy pass still ran. | `PixelSpriteRenderer.drawCharacterSprite`, Lobby draw loop | Gate character and canopy passes together with the renderer's existing conservative bounds. Keep map, projection, anchors, art and depth order. |
| PNG images/family lists and static depth order were already cached. | `SanctuaryArtAssets`, `LobbyDrawQueue` | Retain these caches. Measure actual decoded PNG allocation bytes once per second; generated/premium atlases and GPU copies remain outside this counter. |

`ScenePerformanceProbe` is off by default. With the developer toggle enabled, the
overlay updates at most once per second: accepted-frame FPS, average/p95 frame
interval, jank count (>1.5 selected-frame budgets), CPU update/draw-command submission
times, heroes/monsters, visible props/heroes and known PNG-cache bytes. GPU completion,
GC/allocation, total graphics memory and temperature explicitly remain unmeasured.
Do not interpret CPU Canvas submission time as GPU rendering time.

## Host checkpoint: measured counters, not phone timings

Focused runner:
`/workspace/eternal-performance-evidence/run-focused-helper-tests.sh`

Captured output:
`/workspace/eternal-performance-evidence/focused-helper-tests.log`

Cached Kotlin 2.2.21, JUnit 4.13.2, OpenJDK 21, target JVM 11. **6/6 tests passed**
(last recorded run: 0.030 seconds). No Android draw workload was executed here.

| Same synthetic 60 Hz timestamp stream, 10 seconds | Baseline vsync render / High | Balanced |
| --- | ---: | ---: |
| Render callbacks after initial t=0 warm-up | 600 | 300 |
| Fixed logical updates at 60 Hz | 600 | 600 |
| Logical elapsed time | 10.0 s | 10.0 s |
| Render callbacks during lifecycle/modal stop | Loop previously lacked guard | 0 in controlled Compose/lifecycle test; device verification pending |

This proves a 50% reduction in scheduled world render callbacks for Balanced on the
same 60 Hz input, with equal logical update counts. It is not a measured 50% CPU/GPU,
energy or temperature reduction. Pause-reset test excludes a synthetic 120-second gap.

Probe-accounting test feeds known 30 Hz timestamps and supplied 2/4 ms update/draw
durations. It reports 30.0 FPS, average 33.333333 ms and p95 33.333334 ms; these are
synthetic expected values that verify accounting, not performance measurements.

Additional settings persistence tests are in
`PerformanceSettingsPersistenceRobolectricTest`: restart preserves mode/toggle and
guest identity; unsupported old mode falls back to Balanced and the absent developer
toggle stays off. Android integration/build results belong in the task's final report.

`SceneFrameLoopLifecycleRobolectricTest` additionally exercises the real Compose
effect and lifecycle registry under a controlled frame clock: STOPPED and modal
inactivity produce no callbacks, and each resume starts with a zero delta. It is a
functional cancellation test, not phone scheduling, power or FPS evidence. Its result
must be taken from the final Android test run, not inferred from source inspection.

## Required real-phone before/after pass

Use the same phone, build type, save, hero roster, viewport and zoom for the old and
new APK. Cool the phone between runs. Enable the developer overlay in Settings and
capture its 1 Hz values plus Perfetto/Android Studio traces where available.

1. Stationary Sanctuary: warm assets for 30 seconds, then record 60 seconds.
2. Repeat a fixed pan/zoom path for 60 seconds.
3. Frame the maximum typical roster in the same view for 60 seconds; record hero and
   visible-drawable counts with PNG-cache bytes.
4. Open/close settings, facility, chronicle and map ten times; verify world simulation
   pauses under blocking modals and resumes without a time jump.
5. Background the app for 60 seconds. Verify no scene update/draw callbacks while
   stopped in a trace, and no skipped combat/hero progress on return.
6. Run continuous gameplay 10–15 minutes in each mode. Record average/p95 frame
   intervals, jank, available total/graphics memory and temperature/thermal throttling
   if supported; otherwise report temperature as unverified.

The five scene conditions above have **no device FPS/frame-time/memory/thermal
baseline or after values in this environment**. Render budgets are 33.3 ms (Balanced)
and 16.7 ms (High); those budgets are targets, not verified sustained results.

## Tower integration

The previous Tower HUD explicitly read `frameTick` in composition every rendered frame.
It now has a separate 4 Hz HUD revision; Canvas keeps its own draw invalidation.
The battle draw queue caches prop wrappers/static ordering and moving-actor wrappers,
insertion-sorts the small active set in place and merges into a reusable result list.
Opening settings/retreat confirmation, intro and finished missions pause the loop.
Both scenes use the same lifecycle/pacer/fixed simulation helpers. Balanced retains
textures, core shadows and emissives, using the existing reduced optional-effect tier;
High permits the existing extra halo/penumbra pass. The Master remains an external
command sigil, never a walking avatar.

## APK and phone verification

Install the **direct `.apk`**, not the GitHub Actions outer artifact ZIP. If downloading
an artifact, extract the outer ZIP and, if choosing the portable archive, extract its
`Eternal-Summoner-debug.apk`. Verify its accompanying SHA256SUMS before installing.
Use `adb install -r <apk>` to preserve application data. If Android reports a different
signing identity, stop and report it; do not uninstall or erase the existing save.
Scene timing overlay is opt-in under Settings and reports CPU submission, not GPU time.
Retain the same save/roster/viewport/build type for before/after phone profiling.
