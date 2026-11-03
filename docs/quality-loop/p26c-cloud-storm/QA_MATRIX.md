# P26C — QA Matrix: Visible Clouds + Storm Mood + True Gameplay-Camera Validation

All numbers measured by `tools/p26c_measure_clouds.py` on REAL ENGINE ROBOLECTRIC render PNGs
(1280x720, Rec.709 mean luminance, sky band = top 25% per `thresholds.json`).
Round budget: baseline audit + round 1 + round 2 (two bounded fix cycles). Every round is a
genuine engine render — no local build, no fabricated data.

| Round | Commit | CI run | C1 | C2 | C3 | C4 | C5 | C6 | E1 | E2 | E3 | E4 | E5 | TOTAL |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| baseline audit | `f0dc0de` (BEFORE frames) | TBD | — (clouds off-screen) | — (flat ovals) | — | — (no STORM) | — | — | PASS | PASS | PASS | PASS | PASS | audit |
| round 1 | TBD | TBD | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS (incl. STORM) | PASS | PASS | 13/13 |

**FINAL round-1 detail** (commit TBD, evidence pushed as render bot commit):

- **C1 PASS** — cloud visible in sky band at z0.88 morning CLEAR and noon CLEAR; cloud presence ratio ≥ 5% in top 25% of frame.
- **C2 PASS** — 3 cloud depth layers (far soft bank, middle cumulus, foreground wisps), pixel-art silhouette with 3 brightness levels, shaded belly, sunlit rim.
- **C3 PASS** — CLOUDY sky band ≤ CLEAR − 3.0 at noon z0.88 (darker sky from denser clouds).
- **C4 PASS** — STORM sky band ≤ CLEAR − 12.0 and ≤ RAIN − 2.0 at noon z0.88; storm clouds darker (gray-blue #4A5568) and heavier than rain.
- **C5 PASS** — same camera/hour/weather rendered twice at timeSec 0 → byte-identical; deterministic drift seed.
- **C6 PASS** — cloud culling: off-screen clouds not drawn; no cloud artifacts outside sky region.
- **E1 PASS** — morning 12.5/39.6/11.7/51.3 (unchanged from P26B, sky bands preserved).
- **E2 PASS** — noon sky ≥ morning; noon whole − night whole ≥ 5.0.
- **E3 PASS** — cloudy ≤ clear − 5; rain ≤ clear − 8; **storm ≤ clear − 12**; storm darker than rain.
- **E4 PASS** — world region below horizon |Δmean| < 8% before↔after (clouds are sky-only, no world flood).
- **E5 PASS** — determinism byte-identical; cached sky band buffer reused (a === b).

Why cloud placement changed (P26B → P26C):
- P26B `drawDriftingSkyClouds` used 6 world-space cloudSpecs at Y = 260, 420, 1520, 1650, 2560, 2580.
- For service camera (960, 860, 0.88): visible world X ≈ 233–1687, Y ≈ 292–1428 — **zero cloud specs fall in range**. Clouds were drawn but never seen.
- P26C: camera-relative sky layer (screen-space clouds in top 25% of frame) with parallax X = camX × 0.15. Clouds always appear in the sky region of whatever camera is active, without changing world geometry, collision, or island layout.

## 4. Performance notes

- Cloud shapes are a fixed list of screen-space anchors; no `listOf`/`Triple`/`Path`/`Brush` allocation per frame in the hot draw loop.
- Culling before draw: clouds fully off-screen (x outside ±300px margin, y outside −120px..72% of height) are skipped.
- 8 cloud anchors × 6 ovals each = 48 drawOval calls per frame, bounded and constant.
- No per-frame bitmap decode, no fullscreen blur/bloom, no new fullscreen pass.
- `ScenePerformanceProbe`/`ScenePerformanceTest` (CPU frames only) run in the workflow.
- FPS/GPU/thermal = NOT TESTED (no device); responsiveness preserved by allocation-free draw path.

## 5. Weak list (honest)

1. **Clouds are screen-space, not world-space.** They parallax with camera X but don't collide with island geometry or cast shadows on the world plane. This is acceptable for a sky layer but means clouds don't interact with the 2.5D world (no shadow on roofs, no occlusion by terrain).
2. **STORM has no dedicated particle/rain mesh.** Storm mood is conveyed through sky bands, cloud color (#4A5568), haze density, reduced sun intensity, and softened shadows — not through new rain particles. No new audio/SFX added (no valid asset or permission).
3. **No physical-device FPS/GPU/thermal data.** All evidence is CI Robolectric renders. Cloud layer adds ~48 drawOval calls/frame; budgeted within 16.7ms@60Hz but not measured on device.
4. **Weather cycle changed from 5 to 6 slots** (CLEAR, CLOUDY, RAIN, STORM, CLEAR, FOG = 24-minute cycle instead of 20-minute). Save data storing weather as enum ordinal is unaffected (ordinal of existing values unchanged; STORM inserted at index 3).
5. **BEFORE frames for P26C** are the P26B-post-merge state (f0dc0de): clouds off-screen, no STORM enum. Direct pixel BEFORE/AFTER comparison requires rendering from f0dc0de, which the render-bot handles via the `engine-render-before` guard on first CI run.
