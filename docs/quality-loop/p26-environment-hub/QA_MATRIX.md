# P26 ROUND 1 — QA MATRIX (Sky / Day-Night / Weather + Gate Portal + Hub UI Shell)

Baseline branch: `arena/c4f4092c-eternal-sunmmoner` · Baseline commit: `358a359` (PR #31 merged) ·
Implementation commit: `0264492` (incl. two compile fixes `a27bcb4`, `37a35f1` lineage) ·
Evidence: REAL ENGINE ROBOLECTRIC RENDER via GitHub Actions `lyra-render-evidence.yml`.

**LOCAL BUILD NOT RUN** — no JDK/Gradle/SDK in sandbox; every number below comes from the
CI render matrix, not from local runs. FPS / GPU / thermal / emulator / physical device = NOT TESTED.

## 1. BEFORE audit (measured on the immutable baseline frames, commit `fc0e39f`)

Quantified from `docs/visual-evidence/p26/engine-render-before/*.png` (1280x720, Rec.709 mean
luminance, sky band = top 25%):

| Frame (CLEAR unless noted) | Sky band mean | Whole-frame mean |
|---|---|---|
| morning 7h | 71.2 | 71.8 |
| noon 12h | 79.8 | 78.7 |
| evening 18.5h | 62.6 | 66.7 |
| night 0h | 58.8 | 65.3 |
| noon 12h CLOUDY | 79.8 (= noon CLEAR exactly) | 78.7 (= noon CLEAR exactly) |
| noon 12h RAIN | 71.2 | 71.4 |
| gate night z1.48 | 54.0 | 60.0 |
| gate noon z1.48 | 92.0 | 76.9 |
| gate night rain z1.48 | 53.0 | 58.4 |
| overview night z0.36 | 54.5 | 60.2 |

Findings (honest, code + pixel verified):
- **CLOUDY weather changes NOTHING** — the sky-band pixel stats are identical to CLEAR noon
  (0.0 delta); weather only added particles. P26B requires weather to change lighting + mood.
- **Morning vs night nearly identical** (whole-frame Δ 6.5; sky Δ 12.4) — the addendum's
  "morning/night too similar" complaint is confirmed; morning reads grayish, not fresh/crisp.
- **One fixed dark twilight sky** for all hours (indigo→violet static gradient, `0xFF0D0F1A→0xFF2E314F`),
  moon glow + dark cloud ovals + permanent moonlight rays regardless of state.
- **Night gate portal has no local-light emphasis** — the halo region at night (58.1) is *below*
  noon (67.5); the portal oval pulses equally at every hour.
- **Rift ROI is not a dark void** — the portal interior shows the sky through it (~50), the
  aperture does not read as a protected dark core.

## 2. Implementation (what changed, and where)

- `WorldEnvironment.WorldLighting` — rewritten to a per-state model: 7 anchors (0/5/8/12/17/20/24),
  each carrying its own 4-stop sky gradient, sun disc (colour/intensity/elevation), moon disc,
  cloud colour/density/brightness, ray colour/alpha, haze colour/density. `withWeather(state)` is now
  a member function and multiplies ambient/sun/rays/clouds/haze so CLOUDY/RAIN/FOG change lighting
  and mood, not just particles.
- `SanctuaryWorldRenderer` — the static gradient + moon glow + permanent rays are replaced by
  `drawSkyBands()` (12 interpolated bands from a **cached** `FloatArray(36)` buffer, keyed by
  quantised hour + weather buckets — same state key returns the same instance, contract-tested),
  `drawCelestialDiscs()` (bounded-alpha sun/moon glow + disc), `drawHorizonHaze()`, per-state cloud
  colour/density, and `drawSkyRays()` (solid rects, bounded alpha — no per-frame gradient brush).
- `PixelSpriteRenderer` gate branch — the aperture is now a real dark void (`#05080F` at alpha 0.66
  drawn inside the portal oval before the energy rings, P24 protection strengthened), the energy
  rings + floor milestone scale with `nightBoost = 0.60 + 0.90 × localLightIntensity` (noon 0.87,
  night 1.5), and a bounded BALANCED-only aura hugs the portal (alpha ≤ 0.16, stronger at night).
- `FantasySkin.Panel` / `FantasyManagement.PanelGlass` — opaque `0xFF1B222B`/`0xFF1C242E` slabs →
  translucent dark glass `0xB5141D2A` (alpha 0.71) / `0xB8121B2E` (alpha 0.72) with gold borders.
- `SanctuaryManagementNav` — text-only tabs → framed tabs with icons (Groups/AutoAwesome/Group/
  Castle/Backpack) and the same `nav_*` test tags.

## 3. AFTER evidence + gate measurement

All numbers measured by `tools/p26_measure_sky.py` on REAL ENGINE ROBOLECTRIC render PNGs
(1280x720, Rec.709 mean luminance, sky band = top 25%, gate ROIs per `thresholds.json`).
Round budget: baseline audit + round 1 + round 2 (+ two bounded fix cycles 2b/2c). Every round
was a genuine engine render — no local build, no fabricated data.

| Round | Commit | CI run | E1 | E2 | E3 | E4 | E5 | G1 | G2 | TOTAL |
|---|---|---|---|---|---|---|---|---|---|---|
| baseline audit | `fc0e39f` (BEFORE frames) | `41636700236` | — (static sky) | — | — (cloudy=clear) | — (static) | — | rift ≈50, no void | halo 58.1<67.5 | audit |
| round 1 | `0264492` | `41645909908` | FAIL (8.1/17.5/6.6/24.1) | PASS | FAIL (0.0/−9.1) | PASS (Δ=0.0) | PASS | FAIL 50.3 | FAIL 58.1/67.5 | 3/7 |
| round 2 | `afb192b` | `41652975278` | PASS (12.8/39.8/17.3/57.1) | PASS | PASS (−11.9/−22.7) | FAIL (gate −15.3/−12.9%, overview −18.0%) | PASS | FAIL 42.8 | FAIL 50.2/66.1 | 4/7 |
| round 2b | `7d57af0` | `41654628631` | PASS | PASS | PASS | PASS (world42 box: all |Δ|<8%) | PASS | FAIL 48.1 | FAIL 55.7/66.0 | 5/7 |
| round 2c (final) | `b79f8ba` | `41656083490` | PASS (12.5/39.6/11.7/51.3; 92.7/105.1/65.6/53.9) | PASS (noon>morn; +26.8) | PASS (−11.3/−23.2) | PASS (world42: −1.8…+5.4%) | PASS | FAIL 46.1 | FAIL 55.0/65.3 | 5/7 |

**FINAL round-2c detail** (run `41656083490`, commit `b79f8ba`, evidence pushed as `d794b84`):

- E1 PASS — morning 92.7, noon 105.1, evening 65.6, night 53.9.
- E2 PASS — noon sky ≥ morning sky; noon whole − night whole = 26.8.
- E3 PASS — cloudy −11.3 vs clear; rain −23.2 vs clear (noon service camera).
- E4 PASS — world region (below y=42%): gate_night_rain −1.9%, gate_night −4.5%, gate_noon
  +0.2%, overview_night −5.5%, sky_evening −2.3%, sky_morning +4.1%, sky_night −3.6%,
  sky_noon_cloudy −3.6%, sky_noon_rain +5.4%, sky_noon +2.3%. All |Δ| < 8%.
- E5 PASS — determinism pair byte-identical.
- G1 FAIL — night rift 46.1 (>40); |Δ|≤4 per frame: night −4.18, noon −5.53, night_rain −1.42.
- G2 FAIL — night halo 55.0 < noon 65.3 (halo ≤150 satisfied).

Why G1/G2 cannot pass in the precommitted ROIs (honest, pixel-forensics verified):
- G1: the rift ROI (x 0.44–0.56, y 0.36–0.58 of the gate frame) is dominated by the glow-lit
  background and the below-oval ground — pixel forensics showed the void interior renders
  correctly (L 29–45 at the void, oval centre = 0.95×7.9 + 0.05×glow ≈ 24.5) yet the ROI *mean*
  stays ~46 because most ROI pixels are NOT void. Void alpha 0.72→0.85→0.95 moved the ROI mean
  only ~2 levels (48.4→48.1→46.1). Additionally the joint condition is unsatisfiable against
  the BEFORE baseline 50.3: mean≤40 requires Δ≥−10.3, which violates |Δ|≤4 (joint requires
  result ≥46.3 > 40). The one-sided intent (the rift is darker, not washed brighter) is satisfied
  — every delta is ≤ 0.
- G2: the halo ROI includes the bright noon horizon sky behind the portal (E2 *requires* bright
  noon), so noon halo is anchored near 65. Night halo already carries every portal-content lever
  (void strokes ×1.5 at night, aura 0.18, wide ring 0.15, glow cap 0.20). Flipping the inequality
  inside this ROI would require washing the noon frame or breaking E2. A ROI redefinition
  (portal-content area excluding the horizon sky) is the correct next step, not more alpha tuning.

## 4. Performance notes

- No new fullscreen pass: the sky gradient replaced the static gradient (same rect count, +10 bands),
  rays are solid rects instead of gradient brushes, the gate aura is a bounded stroke oval.
- Cached assets: sky band colours (36 floats per state key), celestial/haze/cloud/ray colours read
  from the per-frame lighting object (no allocation — the model is rebuilt only by `WorldEnvironment.update`,
  once per game tick, not per frame).
- No per-frame bitmap decode, no fullscreen bloom, no blanket brightness — the world region is
  protected by E4 (no-flood).
- `ScenePerformanceProbe`/`ScenePerformanceTest` (CPU frames only) run in the workflow.
- FPS/GPU/thermal = NOT TESTED (no device); responsiveness preserved by keeping the draw path
  allocation-free.

## 5. Weak list (honest)

1. **G1 (rift dark) FAIL in final round.** The joint condition (ROI mean ≤40 AND |Δ|≤4 vs BEFORE)
   is mathematically unsatisfiable: BEFORE night rift = 50.3 > 40, so passing requires
   Δ ≥ −10.3, which violates |Δ|≤4 (the joint would need a result ≥ 46.3 > 40). The precommitted
   threshold was written before pixel forensics showed the ROI is dominated by glow-lit
   background + below-oval ground, not the void. Void alpha 0.72→0.95 over three rounds moved the
   ROI mean only ~4 levels (50.3→46.1). The one-sided intent — the aperture must not be washed
   brighter — is satisfied (all deltas ≤ 0). Fix path: redefine the ROI to the void interior
   (pixel-forensics box: x 0.46–0.52, y 0.42–0.52 approx) and re-measure against the new baseline.
2. **G2 (night gate energy) FAIL in final round.** Night halo 55.0 < noon halo 65.3. The halo ROI
   includes the noon horizon sky behind the portal; E2 requires that sky to be bright, so the noon
   side is anchored ~65. Night already carries all portal content. Redefine the halo ROI to the
   portal-content area (excluding horizon sky) for a meaningful measurement.
3. **Round budget exhausted at 5/7.** Three full render cycles + two bounded fix cycles were spent;
   E1–E5 (the P26B sky/day-night/weather acceptance core) are fully PASS and stable across
   rounds 2b/2c. G1/G2 are ROI-definition problems, not visual regressions — the aperture IS a
   real dark void and the portal energy rings DO scale with local light.
4. **Clouds not visible in any render matrix frame** (world y 2560–2580 → off-screen at all four
   render cameras). Weather lighting/mood is verified through E3 sky-band deltas instead; cloud
   sprite visibility remains untested by the current camera matrix.
5. **No FPS/GPU/thermal/device testing** — sandbox has no JDK/Gradle/SDK; all evidence is CI
   Robolectric renders. CPU-frame probes run in the workflow; on-device metrics are out of scope.
