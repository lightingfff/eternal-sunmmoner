# P26C ROUND 2 — pixel gates PASS, visual refinement still needed

- Source SHA: `8b26848b773dc7a6e20b2bd25b94df1842cce63e`
- Evidence-bot SHA: `b6ece51e3079133a543bbcdbe17d4222a3085f3b`
- Render/contract/regression + sequential APK:
  [Actions 41769520424](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41769520424) **success**.
  Separate signed APK workflow:
  [Actions 41769520387](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41769520387) **success**.
- BEFORE P26B `f0dc0de`, 7 frame pairs matched in
  `docs/visual-evidence/p26c/before_manifest.json`; STORM has no equivalent BEFORE.
- AFTER native-graphics PNGs: `docs/visual-evidence/p26c/engine-render/`;
  `p26c_cloud_bookmark_z100.png` and `p26c_gate_cloud_bookmark_z148.png` show sky
  at z1.0 / z1.48. The service z1.48 shot is **negative occlusion control**, not
  evidence of cloud visibility there.

| Gate | Fixed threshold | Measured Round 2 | Verdict |
|---|---|---|---|
| C1 CLEAR morning/noon z0.88 | `|L_after−L_before| >12` on ≥5% of whole top25% | 14.116% / 12.771% | PASS (was 0/0) |
| C2 visually distinct 3 layers | far bank / cumulus / wisps | 3 anchors types in code; screenshot shows similar stacked translucent oval blobs | **WARN 2/4**, targeted silhouette fix |
| C3 cloudy vs clear | cloudy sky ≤ clear−3 | `−7.95` | PASS |
| C4 STORM vs RAIN | storm sky ≤ clear−12, ≤ rain−2 | `−28.01` / `−10.56` | PASS (was +2.62 vs RAIN) |
| C5 determinism | byte-identical t=0 | YES | PASS |
| C6 cull | skip off-screen before drawing | Code reviewed (fixed 8-anchor loop, bounds check before draw) | PASS code; no device perf |
| E1 day cycle | 8/6/8/18 differences | 11.59/40.22/13.06/53.28 | PASS |
| E2 noon / night | noon brightest, whole delta ≥5 | 27.32, noon sky ≥ morning | PASS |
| E3 weather mood | cloudy≤clear−5, rain≤clear−8, storm≤clear−12 and storm<rain | −7.95/−17.44/−28.01, storm−rain−10.56 | PASS |
| E4 world no-flood | 7 matched pairs world y≥42%, abs delta <8% | max 0.006%; STORM no matched BEFORE | PASS **matched only** |
| E5 determinism | byte-identical | YES | PASS |

**9/9 automated gates PASS**; C2 visual criterion is **not yet 3/4**. Images
opened manually at full 1280×720: far, middle and near use the same 7 oval
shapes with only scale/alpha differences, looking like repeated bubbles in the
visible left sky wedge, most obvious in noon CLEAR and gate morning z1.48.
This is not corrected by passing the pixel count gate. Round 3 is the **last
allowed targeted refinement**: preserve eight anchors/three layers, give far
bank a low continuous profile, middle an authored stepped cumulus body + shade +
rim, foreground a distinct slim wind wisp. Cache all paths at object level;
no per-frame Path/Brush, no new fullscreen storm overlay or changed camera.

Gate regression vs merged P26B frames (same cam/hour/weather, no new baseline
render): noon actual void core **43.60→43.60** (Δ0.00), night **39.15→39.15**
(Δ0.00), night RAIN **42.75→42.78** (Δ+0.03). Legacy G1/G2 remain FAIL
by their old absolute/halo ROIs; these are not changed to claim PASS.

No physical Android device here: FPS/GPU/thermal **NOT TESTED**. Source-review
paths: `SanctuaryWorldRenderer.kt`, `WorldEnvironment.kt`,
`tools/p26c_measure_clouds.py`. Decision: numeric PASS, **visual WARN** → Round 3.
