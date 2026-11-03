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

<!-- FILL AFTER RUN -->

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

<!-- FILL AFTER RUN -->
