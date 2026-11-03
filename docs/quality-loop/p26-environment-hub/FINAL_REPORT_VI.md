# P26B — BÁO CÁO CUỐI (Sky / Day-Night / Weather Overhaul + Gate Portal + Hub UI Shell)

**Ngày**: 2026-11-03 · **Baseline**: `358a359fcde37fc2f85c34050a9ff63bba0d1f5c` (main, PR #31 đã merge)
· **Nhánh**: `arena/c4f4092c-eternal-sunmmoner` · **PR mới**: mở sau báo cáo này, **chưa merge**,
owner tự duyệt · **Không auto-merge** · không đụng lại PR đã merge.
**LOCAL BUILD NOT RUN** — sandbox không có JDK/Gradle/SDK; mọi bằng chứng là REAL ENGINE
ROBOLECTRIC RENDER qua GitHub Actions; FPS/GPU/thermal/emulator/device = NOT TESTED.

## 1. Vấn đề đã đo được, không phải phỏng đoán

Baseline CI (`41636700236` → bot commit `fc0e39f`, 11 khung bất biến trong
`docs/visual-evidence/p26/engine-render-before/`):

- **CLOUDY weather thay đổi NOTHING** — sky-band deltas vs CLEAR noon = 0.0; weather chỉ thêm
  particles. P26B yêu cầu weather thay đổi lighting + mood.
- **Một sky twilight tĩnh** cho mọi giờ (`0xFF0D0F1A→0xFF2E314F`) + moon glow + tia cố định.
- **Morning/night quá giống nhau** (whole-frame Δ 6.5, sky Δ 12.4) — đúng complaint của user.
- **Gate portal đêm không có local-light emphasis** — halo đêm 58.1 < noon 67.5; portal pulse
  đều mọi giờ; rift ROI ≈50, không đọc là hốc tối bảo vệ.

## 2. Đã làm

- `WorldEnvironment.WorldLighting` — model **per-state**: 7 anchors (0/5/8/12/17/20/24), mỗi anchor
  có 4-stop sky gradient riêng, sun disc (colour/intensity/elevation), moon disc, cloud colour/
  density/brightness, ray colour/alpha, haze colour/density. `withWeather(state)` là member
  function nhân ambient/sun/rays/clouds/haze → CLOUDY/RAIN/FOG thay đổi lighting + mood, không
  chỉ particles.
- `SanctuaryWorldRenderer` — `drawSkyBands()` (12 band nội suy từ FloatArray(36) cache, keyed by
  quantised hour + weather buckets — contract-tested `P26SkyContractTest`), `drawCelestialDiscs()`,
  `drawHorizonHaze()`, `drawSkyRays()` (solid rects, bounded alpha; 3-stack fade 0.5/0.22).
- `PixelSpriteRenderer` gate branch — aperture là hốc tối thực sự (void `#05080F`→`#02040A`,
  alpha 0.66→0.95 qua các vòng), energy rings + floor milestone scale với
  `nightBoost = 0.55 + 0.65 × localLightIntensity` (coerce 0.55..1.20), aura BALANCED-only
  (≤0.18), wide night ring (≤0.15). Tất cả bounded.
- `FantasySkin.Panel` / `FantasyManagement.PanelGlass` — slab opaque → translucent dark glass
  (alpha 0.71/0.72) + viền vàng.
- `SanctuaryManagementNav` — tabs icon + `nav_*` tags.
- `tools/p26_measure_sky.py` — pixel gates E1–E5 + G1/G2; `world42` box cho E4 no-flood.
- `docs/quality-loop/p26-environment-hub/`: `WORK_ORDER.md`, `thresholds.json` (PRECOMMITTED),
  `roi_measurements.{json,txt}`, `QA_MATRIX.md`, `ROUND_1.md`, báo cáo này.

## 3. Bằng chứng ảnh engine thật

`docs/visual-evidence/p26/engine-render/` (AFTER, run `41656083490`, commit `b79f8ba`, bot push
`d794b84`) vs `engine-render-before/` (baseline). Camera z0.36/0.88/1.48, hour 0/7/12/17/18.5,
Clear/Cloudy/Rain, gate camera + overview.

| Gate | Ngưỡng PRECOMMITTED | Kết quả cuối (round 2c) |
|---|---|---|
| E1 sky distinctness | \|m−n\|≥8, \|n−e\|≥6, \|e−ng\|≥8, \|n−ng\|≥18 | **PASS** 12.5/39.6/11.7/51.3 |
| E2 noon brightest | noon sky ≥ morning; noon−night whole ≥5 | **PASS** (true; 26.8) |
| E3 weather mood | cloudy ≤ clear−5; rain ≤ clear−8 | **PASS** −11.3 / −23.2 |
| E4 no-flood (world42) | \|Δmean\| < 8% mọi khung | **PASS** −1.9…+5.4% |
| E5 determinism | det pair byte-identical | **PASS** |
| G1 rift dark | night rift ≤40; \|Δ\|≤4 vs BEFORE | **FAIL** 46.1; Δ −4.18/−5.53/−1.42 |
| G2 night gate energy | night halo ≥ noon, ≤150 | **FAIL** 55.0 < 65.3 |

**TOTAL: 5/7** — E1–E5 (P26B acceptance core) PASS toàn bộ, ổn định qua rounds 2b/2c.

## 4. Honest analysis của G1/G2 (không giả vờ)

- **G1**: ROI rift (x0.44–0.56, y0.36–0.58) bị chi phối bởi glow-lit background + below-oval
  ground + grade overlay. Forensics pixel-grid: void nội thất render đúng (L 29–45, centre oval
  = 0.95×7.9 + 0.05×glow ≈ 24.5) nhưng **ROI mean** vẫn ~46 vì đa số pixel ROI **không** là void.
  Void alpha 0.72→0.85→0.95 chỉ đổi ROI mean ~4 bậc (50.3→46.1). Joint condition (≤40 ∧ |Δ|≤4)
  **vô nghiệm** chống baseline 50.3: ≤40 cần Δ≥−10.3, vi phạm |Δ|≤4 (kết quả ≥46.3 > 40).
  One-sided intent (rift **không** bị wash sáng) thỏa mãn — mọi Δ ≤ 0.
- **G2**: halo ROI chứa trời horizon noon sáng (E2 **yêu cầu** sáng), nên noon ≈65 là anchor.
  Đêm đã mang tất cả nội dung portal (strokes×1.5, aura, wide ring, glow cap). Đảo bất đẳng thức
  trong ROI này = wash noon hoặc phá E2. Cần định nghĩa lại ROI (portal-content area **không**
  chứa horizon sky) — đó là next slice, không phải tuning thêm.

## 5. Remaining slice (next steps, không cắt ngầm)

1. Redefine G1 ROI → void interior (pixel-forensics box), G2 ROI → portal-content area excluding
   horizon sky; đo lại trên baseline mới.
2. Camera matrix mở rộng để cloud sprites xuất hiện trong khung (hiện world y 2560–2580 off-screen).
3. Storm mood chưa có khung render (Light rain hiện có); P26B yêu cầu Storm — thêm khung + gate.
4. On-device FPS/GPU/thermal vẫn ngoài scope (sandbox không có device).

## 6. Round ledger

| Round | Commit | CI run | E1 | E2 | E3 | E4 | E5 | G1 | G2 | TOTAL |
|---|---|---|---|---|---|---|---|---|---|---|
| baseline | `fc0e39f` | `41636700236` | audit | | | | | | | |
| round 1 | `0264492` | `41645909908` | FAIL | PASS | FAIL | PASS | PASS | FAIL 50.3 | FAIL 58.1/67.5 | 3/7 |
| round 2 | `afb192b` | `41652975278` | PASS | PASS | PASS | FAIL | PASS | FAIL 42.8 | FAIL 50.2/66.1 | 4/7 |
| round 2b | `7d57af0` | `41654628631` | PASS | PASS | PASS | PASS | PASS | FAIL 48.1 | FAIL 55.7/66.0 | 5/7 |
| round 2c | `b79f8ba` | `41656083490` | PASS | PASS | PASS | PASS | PASS | FAIL 46.1 | FAIL 55.0/65.3 | 5/7 |
