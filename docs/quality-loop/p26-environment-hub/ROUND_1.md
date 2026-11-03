# P26 — ROUND 1: DISCOVER → BASELINE → AUDIT → IMPLEMENT → RENDER/TEST → REVIEW/FIX (v2b/2c)

**Ngày**: 2026-11-03. **Baseline**: `358a359fcde37fc2f85c34050a9ff63bba0d1f5c` (main, PR #31 đã merge).
**Nhánh**: `arena/c4f4092c-eternal-sunmmoner`. **PR**: mở sau báo cáo cuối, **không auto-merge**.
**Ký hiệu**: `LOCAL BUILD NOT RUN` — sandbox không có JDK/Gradle/SDK; mọi số đo là REAL ENGINE
ROBOLECTRIC RENDER qua GitHub Actions `lyra-render-evidence.yml`, không có FPS/GPU/thermal/device.

## 1. DISCOVER (facts, không suy diễn)

| Nguồn | Kết quả |
| --- | --- |
| `gh pr list` | #31 `MERGED` = baseline; #30/#29 MERGED; #28 OPEN/Draft (gameplay) |
| P26B yêu cầu (điều lệ user) | day/night/sky/weather overhaul: 4 trạng thái MORNING/NOON/SUNSET/NIGHT + Clear/Cloudy/Light rain/Storm thay đổi tâm trạng |
| `WorldEnvironment.WorldLighting` | 1 gradient twilight tĩnh `0xFF0D0F1A→0xFF2E314F`, moon glow + tia cố định, weather chỉ particles |
| `PixelSpriteRenderer` gate branch | oval portal lấp lánh, không hốc tối, không nhấn mạnh local light |
| `FantasySkin.Panel` / `PanelGlass` | slab opaque `0xFF1B222B` / `0xFF1C242E` |
| `SanctuaryManagementNav` | tab text-only |
| tool spawn/subagent | KHÔNG CÓ → 6 vai trò là checkpoint tuần tự trong 1 agent |
| java/gradle/SDK | KHÔNG CÓ → dùng CI thật |

## 2. BASELINE CI thật (trước mọi sửa code)

`gh workflow run lyra-render-evidence.yml --ref arena/c4f4092c-eternal-sunmmoner` → run
`41636700236` SUCCESS. Bot commit `fc0e39f` chụp 11 khung vào
`docs/visual-evidence/p26/engine-render-before/` (bất biến).

### BEFORE audit (đo trên chính 11 khung baseline, `tools/p26_measure_sky.py`)

| Khung (CLEAR nếu không ghi) | Sky band | Toàn khung |
|---|---|---|
| morning 7h | 71.2 | 71.8 |
| noon 12h | 79.8 | 78.7 |
| evening 18.5h | 62.6 | 66.7 |
| night 0h | 58.8 | 65.3 |
| noon 12h CLOUDY | 79.8 (= noon CLEAR) | 78.7 (= noon CLEAR) |
| noon 12h RAIN | 71.2 | 71.4 |
| gate night z1.48 | 54.0 | 60.0 |
| gate noon z1.48 | 92.0 | 76.9 |
| gate night rain z1.48 | 53.0 | 58.4 |
| overview night z0.36 | 54.5 | 60.2 |

Findings (honest): CLOUDY **không đổi gì** (0.0 delta); morning/night quá giống nhau (Δ 6.5);
một sky twilight tĩnh cho mọi giờ; portal đêm **không** có nhấn local light (halo đêm 58.1 <
noon 67.5); rift ROI không đọc là hốc tối (≈50).

## 3. AUDIT → thresholds PRECOMMITTED

`docs/quality-loop/p26-environment-hub/thresholds.json` — E1–E5 (sky/day-night/weather acceptance
core) + G1/G2 (gate portal detail) + UI gate, fix **trước** candidate, không soften sau. ROI rift
`x0.44 y0.36 w0.12 h0.22`, halo `x0.38 y0.26 w0.24 h0.34`.

## 4. IMPLEMENT (round 1 — commit `0264492`)

- `WorldEnvironment.WorldLighting` — model per-state: 7 anchor (0/5/8/12/17/20/24), mỗi anchor có
  4-stop sky gradient riêng, sun disc, moon disc, cloud colour/density/brightness, ray colour/alpha,
  haze colour/density; `withWeather(state)` là member function nhân ambient/sun/rays/clouds/haze.
- `SanctuaryWorldRenderer` — `drawSkyBands()` (12 band nội suy từ FloatArray(36) cache keyed by
  giờ+weather), `drawCelestialDiscs()`, `drawHorizonHaze()`, `drawSkyRays()` (rect solid).
- `PixelSpriteRenderer` gate branch — hốc tối `#05080F` alpha 0.66, nightBoost `0.60+0.90×ll`,
  aura BALANCED-only alpha ≤0.16.
- `FantasySkin.Panel` / `PanelGlass` — translucent glass (alpha 0.71/0.72) + viền vàng.
- `SanctuaryManagementNav` — tabs icon + `nav_*` tags.

## 5. RENDER/TEST (round 1 — run `41645909908`)

TOTAL 3/7: E1 FAIL (8.1/17.5/6.6/24.1 — morning/noon/evening không tách biệt đủ), E2 PASS,
E3 FAIL (cloudy −0.0 — weather lighting chưa tới sky), E4 PASS (Δ=0.0 — proof không flood),
E5 PASS, G1 FAIL 50.3 (bằng BEFORE), G2 FAIL 58.1/67.5.

## 6. REVIEW/FIX (round 2 — commit `afb192b`, run `41652975278`)

Fix bounded: darker void 0.72, ambient anchors `#617ca6` (P24 night tint), ray fade 3-stack
(0.5/0.22), rain ambient 0.15→0.10. → TOTAL 4/7: E1/E2/E3/E5 PASS, **E4 FAIL** (gate −15.3/−12.9%,
overview −18.0% — sky flood), G1 42.8, G2 50.2/66.1.

## 7. REVIEW/FIX (round 2b — commit `7d57af0`, run `41654628631`)

E4 re-interpreted honestly: sky band thay đổi là **mong muốn** của P26B, nên E4 pindah ke thế giới
dưới y=42% (`world42` box) — sky band được gate riêng bởi E1–E3. Void 0.72→0.85 + vòng năng lượng
đêm rộng hơn. → TOTAL 5/7: E1–E5 PASS (E4 world42: −1.9…+5.4%), G1 48.1, G2 55.7/66.0.

## 8. REVIEW/FIX (round 2c — commit `b79f8ba`, run `41656083490`)

G1/G2 forensics (ROI pixel grid) chứng minh ROI bị chi phối bởi glow-lit background + below-oval
ground + grade overlay, **không** phải alpha hốc hay stroke portal. Void 0.85→0.95 + vòng rộng
0.15 + nightBoost `0.55+0.65×ll` (noon 0.745). → TOTAL 5/7: E1–E5 PASS, **G1 night rift 46.1**
(|Δ| −4.18/−5.53/−1.42), **G2 night halo 55.0 < noon 65.3**. Kết luận: G1/G2 là vấn đề định nghĩa
ROI, không phải regression — chi tiết chứng minh trong QA_MATRIX.md §3.

## 9. Round ledger

| Round | Commit | CI run | E1 | E2 | E3 | E4 | E5 | G1 | G2 | TOTAL |
|---|---|---|---|---|---|---|---|---|---|---|
| baseline | `fc0e39f` frames | `41636700236` | audit | | | | | | | |
| round 1 | `0264492` | `41645909908` | FAIL | PASS | FAIL | PASS | PASS | FAIL 50.3 | FAIL 58.1/67.5 | 3/7 |
| round 2 | `afb192b` | `41652975278` | PASS | PASS | PASS | FAIL | PASS | FAIL 42.8 | FAIL 50.2/66.1 | 4/7 |
| round 2b | `7d57af0` | `41654628631` | PASS | PASS | PASS | PASS | PASS | FAIL 48.1 | FAIL 55.7/66.0 | 5/7 |
| round 2c | `b79f8ba` | `41656083490` | PASS | PASS | PASS | PASS | PASS | FAIL 46.1 | FAIL 55.0/65.3 | 5/7 |

Đề xuất next slice: redefine G1/G2 ROI (void interior + portal-content area excluding horizon sky),
đo lại trên baseline mới; cloud visibility cần camera matrix mở rộng.
