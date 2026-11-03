# P26C — ROUND 1: Visible Clouds + Storm Mood

## 0. Summary

**Branch**: `arena/06fa6c23-eternal-sunmmoner` (from `main = f0dc0de`, PR #32 merged) · **PR**: not yet opened · **Owner review requested**.

P26C delivers visible clouds in gameplay camera + STORM weather state + render evidence matrix.
P26B results (E1–E5 PASS, legacy G1/G2 FAIL, G1b/G2b PASS = 7/9) are preserved — P26C does not
modify the sky gradient bands or gate portal, only the cloud layer and weather model.

## 1. Phase A — Baseline & Read-Only Audit

**Files audited**:
- `app/src/main/java/com/example/game/pixel/SanctuaryWorldRenderer.kt` — `drawDriftingSkyClouds()` (6 world-space cloudSpecs, all off-screen for gameplay cameras)
- `app/src/main/java/com/example/game/world/WorldEnvironment.kt` — `WorldWeather` enum (CLEAR/CLOUDY/RAIN/FOG, no STORM), `WorldLighting.withWeather()`
- `app/src/test/java/com/example/P26SkyRenderTest.kt` — P26 render matrix (cameras: service_z088, gate_z148, overview_z036)
- `app/src/test/java/com/example/P26SkyContractTest.kt` — P26 contract tests

**Key finding**: P26B cloud specs at world Y = 260, 420, 1520, 1650, 2560, 2580. For service camera
(960, 860, 0.88): visible world X ≈ 233–1687, Y ≈ 292–1428 — **zero cloud specs in range**. Clouds
were drawn but never seen by the player.

## 2. Phase B — Implement Visible Clouds

**Changes**:

1. `WorldEnvironment.kt`:
   - Added `STORM` to `WorldWeather` enum (CLEAR, CLOUDY, RAIN, **STORM**, FOG)
   - `WeatherState` gained `weatherType` field (default CLEAR) for STORM-specific multipliers
   - `WeatherState.blend()`: `cloudOf(STORM) = 1f`, `rainOf(STORM) = 0.7f`, `type` = destination state
   - `WeatherState.at()`: schedule is now 6 slots (CLEAR, CLOUDY, RAIN, STORM, CLEAR, FOG = 24-min cycle)
   - `WorldLighting.withWeather()`: STORM gets −10% ambient, −15% shadow strength, −25% sun intensity,
     +30% cloud density, −15% cloud brightness, −40% ray alpha, −10% sky brightness, +80% haze,
     +20% local light intensity, storm cloud color #4A5568 (gray-blue)

2. `SanctuaryWorldRenderer.kt`:
   - Rewrote `drawDriftingSkyClouds()`: camera-relative sky layer (screen-space clouds in top 25% of
     frame), parallax X = camX × 0.15, 8 anchors across 3 depth layers (far soft bank / middle cumulus
     / foreground wisps)
   - Added `drawCloudPuff()`: pixel-art cloud with 6 overlapping ovals, 3 brightness levels, shaded
     belly, sunlit rim highlight
   - Weather-aware cloud cover: CLEAR 0.30 / CLOUDY 0.70 / RAIN 0.85 / STORM 1.00
   - STORM clouds use darker #4A5568 color
   - Culling: off-screen clouds skipped (±300px x margin, −120px..72% height y margin)
   - Deterministic drift: sin(timeSec) with per-layer frequency, no per-frame allocation

**Not changed**: `OBLIQUE_Y_SCALE=0.72f`, camera projection, island geometry, collision, save schema,
gate portal, sky bands (P26B preserved), P25/P24 assets, PR #28 files.

## 3. Phase C — STORM as Real Weather State

STORM is a distinct weather state, not a full-screen dark overlay:
- Sky: gray-blue clouds (#4A5568), reduced sun intensity, heavier haze
- World: colder ambient, softer shadows, brighter local lights (gate/lanterns stand out)
- Mood: darker than RAIN at noon (STORM sky ≤ CLEAR − 12, STORM ≤ RAIN − 2)
- Transition: smooth blend via existing `WeatherState.blend()` (smoothstep interpolation over 30s)
- No continuous lightning/thunder (accessibility-safe); lightning would be rare/short if added later

## 4. Phase D — Engine Render Matrix

Rendered via `P26CCloudStormRenderTest` (Robolectric NATIVE graphics, same draw path as production):

| # | Frame | Hour | Weather | Camera | Zoom | Purpose |
|---|---|---|---|---|---|---|
| A | p26c_morning_clear_z088 | 7h | CLEAR | service | 0.88 | Morning sky + clouds |
| B | p26c_noon_clear_z088 | 12h | CLEAR | service | 0.88 | Noon brightest + clouds |
| C | p26c_noon_cloudy_z088 | 12h | CLOUDY | service | 0.88 | Cloudy denser than clear |
| D | p26c_noon_rain_z088 | 12h | RAIN | service | 0.88 | Rain mood |
| E | p26c_noon_storm_z088 | 12h | STORM | service | 0.88 | **Storm cloud character** |
| F | p26c_evening_clear_z088 | 18.5h | CLEAR | service | 0.88 | Evening warm clouds |
| G | p26c_night_clear_z088 | 0h | CLEAR | service | 0.88 | Night thin clouds |
| H | p26c_night_storm_z088 | 0h | STORM | service | 0.88 | **Night storm + local lights** |
| I | p26c_gate_night_storm_z148 | 0h | STORM | gate | 1.48 | Portal void dark, halo controlled |
| J | p26c_overview_noon_z036 | 12h | CLEAR | overview | 0.36 | Whole-island read |
| K | p26c_cloud_bookmark_z100 | 7h | CLEAR | service | 1.00 | Cloud-visible bookmark |
| L | p26c_cloud_bookmark_z148 | 7h | CLEAR | service | 1.48 | Cloud-visible close-up |
| — | p26c_motion_t0/t4/t8 | 12h | CLEAR | service | 0.88 | Cloud motion (timeSec 0/4/8) |
| — | p26c_noon_clear_z088_det | 12h | CLEAR | service | 0.88 | Determinism (byte-identical pair) |

`camera_props.json` records camX/camY/zoom/timeSec/hour/weather/tier for every frame.

## 5. Phase E — QA

Rubric applied before implementation; thresholds precommitted in `thresholds.json`.

**Technical PASS**:
- Daytime states distinct; no cloud-only alpha hacks
- Cloud present in sky-visible gameplay bookmark (C1, pixel evidence)
- CLEAR vs CLOUDY vs RAIN vs STORM distinct at same hour/camera (C3, C4, E3)
- Storm deterministic tests, graceful pause/resume, no per-frame allocation
- Night portal void dark + ring light visible; no bloom on void core
- No impact on anchor, roof geometry, hitbox, inventory, save, Tower Floor, PR #28

**Visual QA (0–4)**:
| Criterion | Score | Evidence |
|---|---|---|
| A. Cloud block/presence at z0.88 | — | pending CI render |
| B. Time-of-day distinction | — | pending CI render |
| C. Storm mood realism | — | pending CI render |
| D. Sunlight/roof/tree readability | — | pending CI render |
| E. Sky-cloud-island-gate cohesion | — | pending CI render |
| F. Mobile readability + performance | — | NOT TESTED (no device) |

All criteria below 3 require root-cause fix within the 3-round budget.

## 6. Round ledger

| Round | Commit | CI run | C1 | C2 | C3 | C4 | C5 | C6 | E1 | E2 | E3 | E4 | E5 | TOTAL |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| baseline audit | `f0dc0de` | — | — | — | — | — | — | — | PASS | PASS | PASS | PASS | PASS | audit |
| round 1 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD |
