# ES-A10-001 — Sanctuary Android profiling plan

**Status:** scoped read-only audit / proposed measurement runbook
**Audit baseline:** `bdff6f9976dc4927e1e095a503bc339509c7a9aa` (`main`, 2026-11-08)
**Task:** GitHub Issue [#61](https://github.com/lightingfff/eternal-sunmmoner/issues/61)
**Owner:** A10 Android Performance & Thermal Auditor
**Code changes:** none; this document does not authorize renderer or runtime changes.

## Work order / boundaries

- **Goal:** collect repeatable Sanctuary frame-pacing, CPU, memory/cache, and (where hardware permits) GPU and thermal evidence on an actual Android device. Separate CPU Canvas command submission from GPU completion and distinguish device evidence from CI/Robolectric.
- **Current behavior to test:** SanctuaryLobbyScreen uses `SceneFrameLoop` and `ScenePerformanceProbe`; the probe is opt-in. The UI settings expose `BALANCED` (30 Hz) and `HIGH` (60 Hz), not a `LOW` mode. `MainActivity` currently maps those settings to `DepthPresentation.REDUCED` and `BALANCED`, respectively. Do not describe these as independent quality tiers beyond that actual mapping.
- **Allowed file:** this document only, `docs/performance/ES-A10-001_SANCTUARY_PROFILING_PLAN.md`.
- **Must not change:** runtime/renderer/performance Kotlin, `DepthPresentation` behavior, camera/projection, world layout, assets, save/settings schema, Gradle, and open PR files. Do not reduce quality or change a profile based on an unmeasured hypothesis.
- **Rollback:** revert this documentation-only file if its runbook is superseded or inaccurate; no runtime rollback is involved.
- **Review:** A03 and A09 are the assigned role reviewers. Per current dual-review policy, request Argon and Codex lightweight read-only review as well; no review is represented as complete until an actual review comment exists.

## Baseline source audit

| Path / symbol | What it provides | Measurement caveat |
|---|---|---|
| `app/src/main/java/com/example/performance/SceneFrameLoop.kt` — `SceneFrameLoop` | One `withFrameNanos` driver; active only while the scene is enabled and lifecycle is RESUMED; creates a `SceneFramePacer` for each resume. | Frame callbacks are scheduled through Compose; this is not a display/GPU completion timestamp. |
| `app/src/main/java/com/example/ui/screens/SanctuaryLobbyScreen.kt` — `SceneFrameLoop`, `ScenePerformanceProbe`, Canvas draw | Fixed 60 Hz logical simulation; settings select the 30/60 Hz render target. Optional probe records update duration, draw-command submission duration, frame intervals, hero count, visible drawable count and known PNG-cache bytes. | Update timing is CPU-side. Draw timing wraps Compose Canvas command submission; it does not wait for GPU completion. The overlay adds instrumentation overhead and is off by default. |
| `app/src/main/java/com/example/performance/ScenePerformance.kt` — `ScenePerformanceProbe` | One-second snapshots: interval-derived FPS, average/p95 frame interval, jank count, average update/draw timings and supplied counters. | Do not treat callback interval as SurfaceFlinger-presented frame time. The jank threshold is 1.5 times the selected target interval. |
| `app/src/main/java/com/example/performance/ScenePerformanceOverlay.kt` | Explicitly labels draw as CPU submit and GPU/GC/allocation/atlas data as unmeasured. | Overlay values are a diagnostic signal, not a standalone benchmark. |
| `app/src/main/java/com/example/game/pixel/SanctuaryArtAssets.kt` — `cachedBitmapBytes` | Sum of `allocationByteCount` for decoded images in the `images` map. | Not total app/native/GPU memory; generated actor atlases, GPU copies and some derived/mirrored images are outside this counter. Cache growth must be correlated with process memory and asset warm-up state. |
| `SanctuaryLobbyScreen.kt` — visibility filter / `drawStats` | Counts on-screen drawables after conservative prop/hero visibility checks. | A drawable count is not a draw-call count or GPU-work measurement. Shadows/terrain/sky and other passes are not represented by that counter. |
| `app/src/main/java/com/example/game/pixel/DepthPresentation.kt` and `MainActivity.kt` | Existing optional presentation paths and mapping from player mode to render quality. | Compare only the shipped settings. `OFF` is an internal/test control, not a user-facing low-quality tier. Preserve crisp nearest-neighbor world art and the existing oblique camera. |

Existing relevant evidence/runbooks: [`docs/P0_5_PERFORMANCE_CHECKPOINT.md`](../P0_5_PERFORMANCE_CHECKPOINT.md) and [`docs/P15A_VISUAL_SLICE.md`](../P15A_VISUAL_SLICE.md). Their historic/synthetic values are not fresh measurements for this baseline. `ScenePerformanceTest` validates probe arithmetic with synthetic timestamps; it does not benchmark Android rendering.

## Measurement matrix (physical device required for device claims)

### Hold constant for every comparison

Record device model/chipset/RAM, Android build/API, display resolution and refresh rate, app version/build type and exact APK SHA, battery/charging state, ambient conditions, save/roster, Sanctuary unlock/layout, viewport/orientation, default zoom and camera position, weather/time, overlay state, and test start/end time. Use the same device and APK except when a before/after comparison is explicitly being made. Note whether the device is cool/idle or thermally saturated; allow it to return to a comparable starting state between long runs.

Warm the app and assets for 30 seconds before each sample. Capture a stationary overview (zoom 0.36), normal gameplay framing (0.88), and close framing (1.48); repeat a fixed 60-second pan/zoom path and a representative hero-roster view. Keep actors, particles, camera path, and scene state comparable. Repeat each mode at least three times; retain every run, including outliers, rather than selecting the best result.

| Run | Shipped setting | Render target | Existing quality mapping | Purpose |
|---|---|---:|---|---|
| A | `BALANCED` | 30 Hz | `DepthPresentation.REDUCED` | Default sustained-work baseline |
| B | `HIGH` | 60 Hz | `DepthPresentation.BALANCED` | High-refresh / higher-work comparison |
| C (optional) | `LOW` | — | Not implemented in player settings | **Do not run or imply this mode exists.** Ask the owner before proposing any new tier. |

The 33.3 ms (30 Hz) and 16.7 ms (60 Hz) frame intervals are target budgets, not achieved FPS or pass results.

### Capture channels

1. **In-app CPU/pacing diagnostic:** enable the developer performance overlay only for a diagnostic pass; save one-second snapshots for frame interval average/p95, jank, CPU update/draw-submit timing, hero/visible-drawable counts and PNG cache bytes. Then repeat the core scene with the overlay disabled to estimate instrumentation overhead. Do not compare the overlay's callback-FPS directly with presented FPS.
2. **Presented frames / main thread / RenderThread:** on a physical device, use Android Studio System Trace or Perfetto with FrameTimeline / SurfaceFlinger and sched/CPU tracks where supported. Capture at least 60 seconds per matrix cell, mark the same camera-path start/end, and export the trace. Use `adb shell dumpsys gfxinfo "$PKG" reset` before the route and `adb shell dumpsys gfxinfo "$PKG"` after it as a secondary view; preserve raw output. These are Android rendering-pipeline signals, not a universal GPU-time measurement.
3. **CPU and GC/allocation:** use Android Studio CPU/Memory profiler or a Perfetto trace with ART/runtime tracks, and record GC count/pause plus allocation hotspots during warm and steady-state passes. Attribute allocations to a stack/path before proposing fixes; distinguish startup/decode from warmed frames.
4. **Memory/cache:** capture `adb shell dumpsys meminfo "$PKG"` at cold launch, after warm-up, and after each repeated route. Record Java heap, native heap, graphics/PSS fields available on that OS, and `cachedBitmapBytes` separately. Do not add incomparable fields or call PNG-cache bytes “total bitmap/GPU memory.”
5. **Thermal/power:** only on a physical device, record supported thermal status/temperature and battery/power readings at start and at least once per minute through a 10–15 minute sustained run per setting. Use the device's supported Android thermal service/API and vendor tooling where available; save raw output and note sensor provenance. If the device/OS does not expose a reliable value, report thermal measurement as `NOT TESTED`/`UNAVAILABLE`; do not infer heat from FPS or software emulation.
6. **Build/host checks:** CI APK build and Robolectric/render tests are useful functional checks. Neither measures physical-device FPS, GPU completion, memory pressure under hardware load, power, or thermal throttling.

### Commands / collection checklist

Substitute the verified package ID for `$PKG`; do not assume a package name from an older report. Keep outputs with the run ID and APK SHA outside the gameplay save directory.

```sh
adb devices -l
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell wm size
adb shell dumpsys display
adb shell dumpsys gfxinfo "$PKG" reset
# Run the fixed Sanctuary scene/path for 60 seconds; save overlay samples and a Perfetto/System Trace.
adb shell dumpsys gfxinfo "$PKG"
adb shell dumpsys meminfo "$PKG"
adb shell dumpsys thermalservice   # only if supported; preserve “unsupported” as unavailable
```

For each raw trace/output, record the test ID, mode, start/end timestamps, viewport, zoom/camera path, warm/cold state, and APK SHA. Summarize frame interval/presented-frame p50/p95/p99 where the data source supports those percentiles; record missed/deadline frames, CPU main-thread and RenderThread spans, GC pauses, memory fields, and thermal timeline. Cite the trace segment/query used. If a tool cannot produce a metric on that device, mark that metric unavailable rather than substituting the in-app probe.

## Interpretation / decision gates

- Report in-app `averageFrameMs` / `p95FrameMs` as **frame-callback intervals**; report `averageUpdateMs` and `averageDrawMs` as **CPU timings**. Use FrameTimeline/other appropriate device evidence for presented frames. GPU time requires a GPU/vendor counter or trace that really measures it.
- Separate cold asset decode/load from warm steady state; compare equal warm-up, route, roster, orientation and device conditions.
- Only attribute a regression to culling/cache/allocations when traces or counters provide a reproducible path-level cause. Avoid blanket draw-count claims from the `visibleDrawables` counter.
- Do not change art quality, introduce a lower tier, or edit renderer files as part of this read-only task. Any implementation follow-up needs a new/updated issue with exact file ownership, before/after evidence and independent review.
- Final report must include raw evidence locations and limitations. Without a physical device: **FPS / GPU / thermal = NOT TESTED**; CI/Robolectric are explicitly labeled as such.

## Audit environment and current evidence state

- The audit checkout is at the same commit as `main`: `bdff6f9976dc4927e1e095a503bc339509c7a9aa`.
- `adb`, `java`, Gradle and a local Android SDK are absent in this sandbox; no emulator or physical device is attached. This task therefore records no fresh runtime, FPS, GPU, memory or thermal measurement.
- The GitHub Actions APK build at the baseline SHA completed successfully: [run 42452402051](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42452402051). The Lyra engine render-evidence workflow was still `in_progress` when checked: [run 42453053914](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42453053914). These are baseline context only, not validation of this documentation change.
- Existing `ScenePerformanceTest` synthetic timestamps validate accounting only; no test or runtime was executed for this read-only audit.
