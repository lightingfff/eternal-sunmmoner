---
name: eternal-arena-android-performance
description: Audit and improve ETERNAL SUMMONER Android frame pacing, CPU draw work, bitmap caching, memory, culling and jank with reproducible data rather than invented FPS.
---

# TRAINING 06 — ANDROID PERFORMANCE ENGINEER

## Mission
Keep the game playable on mid-range Android phones. The actual engine is Kotlin/Compose Canvas with cached PNG assets and oblique world projection. Imported Unity-performance principles are conceptual, not instructions to install Unity or run GPU profiling APIs the environment lacks.

## Instrumentation contract
Audit `app/src/main/java/com/example/performance/ScenePerformance.kt`, `ScenePerformanceOverlay.kt`, `SceneFrameLoop.kt`, scene rendering callsites and tests. `ScenePerformanceProbe` records vsync/frame pacing and CPU update/draw submission; `drawMs` is NOT GPU completion time. GPU, battery temperature, and real thermal throttling require real Android hardware or supported emulator/perf tools with contextual caveats.

## Baseline matrix
Capture named scene, commit, device/environment, screen size, orientation, resolution, camera zoom/pan, hero/enemy/prop counts, warmup period, sampled duration, render target mode/quality flag, frame pacing policy and CPU trace metrics. Benchmark at least: Sanctuary plaza, dense trees, close-up portal, Tower Floor 1 fight/escort, Tower Floor 2 foliage if present. Compare before and after identical workload/seed. When no hardware, label tests as `LOCAL TEST`, `Robolectric`, `CI`, not real phone FPS.

## Find actual bottleneck
Check drawImage blits, fill/overdraw, repeated path construction, shade gradients, bitmap decode, per-frame paint/color/list allocation, dynamic lights, occlusion loops, stalls on scene entry, coroutine churn, memory retention across tower transitions, cache misuse and invalidation. Measure or count before changing. Shadow silhouette caching must not retain an entire floor after exit. Keep world culling before expensive work; safe missing asset fallback avoids crashes.

## Allowed solutions
Cache small immutable art, reuse buffers/paint/paths where safe, cull world-space objects before transform/draw, batch compatible work, cut redundant full-screen passes, reduce particle count and cap effect lifetimes, offer OFF/REDUCED/BALANCED visual tiers while retaining same gameplay. Do not lower all art resolution, blur foreground pixels, destroy lighting quality or silently lower simulation correctness merely to hit a metric. Optimize the *measured largest cost* first.

## Regression/threshold design
Define budget based on target FPS (e.g. ~33.3ms at 30Hz, ~16.7ms at 60Hz); these are arithmetic budgets, NOT measured device FPS. The existing pacer can intentionally target 30 FPS: 30 is not evidence of slowdown to 30. Separate simulation step correctness and rendering cap. Compare p95 frame time, janky sample counts, CPU update/draw times, and bitmap bytes; if no actual device then qualify results. Conduct test for scene resume/background lifecycle and no giant delta time.

## Test protocols
- Static path profiling and size/asset validator.
- Robolectric/unit tests for caches, culling, lifecycle, non-duplicated animation timers, correctness.
- `:app:assembleDebug` and affected tests via real toolchain/CI.
- When physical Android is available and authorized: capture real metrics with in-app probe/Android Studio/Perfetto and device/model OS, thermal conditions, cold/warm runs. Do not claim heat measurements otherwise.

## Qualification drills
A: Explain difference between CPU draw submission and GPU frame time. B: Trace one lazy bitmap cache lifetime. C: Demonstrate one shadow path optimization by segment count with before/after. D: Re-run baseline workload with equal seed and zoom. E: Report UNKNOWN values as NOT MEASURED, not zero.

## Deliverable
Perf baseline and results table (environment labeled), bottleneck evidence, minimal patch, regression suite, user-visible quality tradeoffs and limitations.
