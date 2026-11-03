# P25 — ROUND 2: REAL ENGINE RENDER → VISUAL QA → ROOT CAUSE → PHASE C

## 1. CI thật trên SHA code `9a204e2` (3 pilot)

PR **#31 (Draft)** mở trên nhánh `arena/e486a516-eternal-sunmmoner`. Hai workflow chạy trên đúng head SHA `9a204e2`:

| run | workflow | kết quả |
| --- | --- | --- |
| `41620768746` | Build Eternal Summoner APK (`gradle :app:assembleDebug` + unit tests) | **success** |
| `41620768647` | Lyra engine render evidence | **success** |

Job `41620768647` từng bước: `Render P23 architecture and props matrix` ✅, `Verify P23 architecture and props contract` ✅, **`Verify P25 face grammar and asset invariants` ✅** (step mới), `Render P24 HDR light-response matrix` ✅, `Verify Sanctuary environment art, manifest, layout, and performance regressions` ✅, `Publish render PNGs` ✅, `Upload engine render PNGs` ✅; `Capture compiler diagnostics on failure` = skipped. Không có `action_required`, không có lỗi tải artifact. Bot commit `0ace057` ghi 73 file PNG evidence; `camera_props.json` so sánh `cmp` với baseline = **IDENTICAL**. Đây là `REAL ENGINE ROBOLECTRIC RENDER`, không phải contact sheet / emulator / thiết bị thật.

## 2. Đo before/after thật (`measure_rois.py`, 32 khung + ROI chiếu)

Lệnh (deterministic, nearest-neighbour, `ObliqueCamera.worldToScreenX/Y` + `drawCrispSprite`):

```
python3 docs/quality-loop/p25-architecture-volume/measure_rois.py \
  --baseline-assets docs/visual-evidence/p25/authored-before \
  --candidate-assets app/src/main/assets/premium_slice \
  --before-engine docs/visual-evidence/p25/engine-render-before \
  --after-engine docs/visual-evidence/p23/engine-render \
  --camera-props docs/visual-evidence/p25/camera_props.json \
  --json docs/quality-loop/p25-architecture-volume/roi_measurements.json \
  --text docs/quality-loop/p25-architecture-volume/roi_measurements.txt
```

### Authored PNG ROI (B5, 3 pilot) — 17/17 PASS, ngưỡng giữ nguyên

| asset | front−side | roof−return | \|roof−front\| | front−soffit | roof spread | door gap |
| --- | --- | --- | --- | --- | --- | --- |
| `hero_quarters` | −1.57 → **19.77** | 5.14 → 11.13 | 11.08 → 13.86 | 26.95 → 41.01 | 32.8 → 43.8 | 21.53 → 27.12 |
| `restaurant` | 0.14 → **23.51** | 0.94 → **23.74** | 2.20 → **15.67** | 35.24 → 53.21 | 12.9 → 16.6 | 29.56 → 39.06 |
| `summoning_hall` | 12.14 → **33.16** | −7.98 → **22.12** | 15.15 → 18.16 | 0.31 → **60.02** | 26.3 → 32.6 | n/a (zone-only) |

### Engine-projected ROI (corroboration trên ảnh engine, KHÔNG phải mockup)

| khung | asset | front−side before → after | roof−return | front−soffit |
| --- | --- | --- | --- | --- |
| `service_cluster_z088` | `hero_quarters` | 3.57 → 18.78 | 12.87 → 18.28 | 21.82 → 33.40 |
| `service_cluster_z088` | `restaurant` | 8.10 → **27.93** | 11.41 → 30.53 | 30.74 → 48.17 |
| `service_cluster_z100` | `hero_quarters` | 4.40 → 18.96 | 14.25 → 19.65 | 20.72 → 32.05 |
| `service_cluster_z100` | `restaurant` | 8.82 → 27.91 | 12.70 → 31.31 | 29.56 → 46.50 |
| `quarters_door_actor_z148` | `hero_quarters` | 7.00 → 20.90 | 16.12 → 21.51 | 21.50 → 32.83 |
| `restaurant_door_actor_z148` | `restaurant` | 12.55 → 30.71 | 15.07 → 32.97 | 30.24 → 47.02 |
| `summoning_hall_whole_z148` | `summoning_hall` | 30.26 → 44.74 | 18.01 → 39.83 | **−5.55 → 47.92** |
| `sanctuary_overview_z036` | `hero_quarters` | −2.74 → 15.73 | 5.89 → 12.15 | 26.32 → 38.23 |
| `sanctuary_overview_z036` | `restaurant` | 0.52 → 24.24 | 0.87 → 23.26 | 34.89 → 53.02 |
| `sanctuary_overview_z036` | `summoning_hall` | 21.72 → 39.11 | 5.32 → 30.91 | −0.79 → 55.92 |

→ Kết luận: sửa giá trị **bên trong PNG** thực sự đi qua cache decode → `drawCrispSprite(2s, FilterQuality.None)` → pass HDR P24 và vẫn đọc được trên ảnh engine. Root cause (b)+(g) được xác nhận; (f) **không cần sửa `HdrLightResponse.kt`** ở ROUND này vì grade P24 không triệt tiêu separation đã author (front−side sau grade vẫn ≥15.7 ở z0.88/z1.00).

### Full-frame (`no-flood` + disclosure cổng P24)

| khung | mean before → after | Δmean% | spread before → after | Δspread |
| --- | --- | --- | --- | --- |
| `sanctuary_overview_z036` | 70.60 → 70.52 | −0.11% | 57.6 → 57.6 | +0.0 |
| `service_cluster_z088` | 62.76 → 62.59 | −0.27% | 53.1 → 54.1 | +1.0 |
| `tower_gate_whole_z148` | 59.35 → 59.35 | +0.00% | 56.6 → 56.6 | +0.0 |

`max |Δmean|` trên **toàn bộ 32 khung** = **1.09%** → **B4 no-flood PASS** (<8%). Cổng P24 `Δ(p90−p10) ≥ +3 trên ≥ 2/3` tính trên before/after của **P25** = **0/3** — **báo cáo độc lập, KHÔNG gọi là PASS, KHÔNG đổi ngưỡng**. P24 tự thân từng đạt 1/3 và vẫn `BLOCKED`; P25 không sửa cổng đó bằng brightness toàn cục. Đây cũng minh chứng cho điểm yếu đã ghi ở `WORK_ORDER`: chỉ số whole-frame p90/p10 pha lẫn nền/đường/cây nên không phản ánh việc 3 mặt công trình đã tách rõ (ROI tăng 3–8× trong khi Δspread toàn khung ≈ 0).

## 3. VISUAL QA (soi ảnh engine AFTER thật)

| khung | quan sát trực tiếp |
| --- | --- |
| `service_cluster_z100.png` | HQ mái shingle ấm có mặt hồi phải xanh-đen tách rõ; Restaurant mái slate có hip return tối, mặt tiền sáng ấm, awning/timber đọc được; cửa cả hai vẫn lõm, jamb sáng |
| `service_cluster_z088.png` | cùng kết luận ở zoom chơi; **tuy nhiên** 4 facility chưa sửa (armory, registry, synthesis, gate) cùng dãy townhouse/cottage vẫn phẳng → coherence toàn khu còn yếu |
| `quarters_door_actor_z148.png` | actor đứng đúng hốc cửa HQ (P20/P21 trigger không đổi), hood/jamb/bậc nguyên, mặt hồi phải tối rõ |
| `summoning_hall_whole_z148.png` | cột/cella sáng, mặt hồi phải tối, band soffit dưới cornice tối rõ, hốc cửa đen + vòng rune cyan nguyên; **khuyết điểm nhỏ**: band soffit dày ~13px ở z1.48 hơi nặng, đọc như vạch đen |
| `hdr_balanced_night_rain_z088.png` | tier BALANCED/night/rain giữ nguyên, không wash, không flood, cửa sổ ấm còn nguyên |
| `tower_gate_whole_z148.png` | không đổi ở ROUND 1: hốc rift đen + oval + spill cyan nguyên, không glow |
| `sanctuary_overview_z036.png` | HQ (nâu) / Restaurant (slate) / Summoning Hall (đá) đã khác nhau, nhưng 4 facility còn lại vẫn cùng họ xám |

**Chấm mềm (chỉ trên ảnh engine, 6 chiều 0–4)**: visual coherence **3/4** (3 pilot), technical correctness **4/4**, player clarity **3/4**, material differentiation **3/4**, polish **3/4** (band soffit hơi nặng), efficiency **4/4** = **20/24**. Đây là điểm cho **3 facility pilot**; toàn khu dịch vụ vẫn 2/4 → đó chính là lý do mở Phase C, không phải lý do tự PASS.

## 4. Phase C — mở rộng grammar lên 7 facility (Environment Artist, cùng 1 writer)

Điều kiện `WORK_ORDER` đã thoả (B5 17/17 + B6 3/4 trên engine thật). Thêm ROI/target cho 4 asset còn lại, **ngưỡng B5 không đổi**:

| asset | baseline đáng chú ý | target / bảo vệ |
| --- | --- | --- |
| `armory` 190×166 | side 61.85 **>** front 59.42; roof−return 1.14 | shingle ấm; `protect [76,110,116,165]` giữ **nguyên** cửa trigger (993,655) P20/P21; `door_reveal` chỉ đo, không grade |
| `hero_registry` 188×166 | front−side 1.09; roof−return 1.22 | civic archive front sáng nhất họ slate; protect 3 vòm loggia (**negative space có chủ đích, không phải lỗi alpha**) + tháp chuông; zone-only, không áp ngưỡng cửa |
| `synthesis_lab` 188×168 | return 68.04 **>** pitch 63.18; side 63.88 > front 59.53 | verdigris tối; protect workshop bay + condenser đồng; zone-only |
| `tower_gate` 260×224 | front 60.66 ≈ side 60.07 | **conservative**: key-lit trái / phải shaded, band machicolation tối; `protect [106,88,154,202]` = hốc rift + oval + spill; `portal_aperture` chỉ đo. Không đụng emitter P24 |

`measure_rois.py` bổ sung `PROP_TO_ASSET` 7 id, `FOOT_OFFSET` 6f·s cho cả 7, và 11 khung engine để chiếu ROI. Bỏ face `protected` khỏi phép chiếu. `p25_validate_faces.py` tự động lấy 7 key từ `roi_definitions.json`.

**Calibration trong ROUND 2, KHÔNG nới rubric**: `armory roof_key−return` ban đầu 8.37 < 10 vì ~72% ROI là viền ngói tối được bảo vệ (chỉ 1156/4142 pixel được grade). Hạ `return 24→18` chỉ được 9.31; nâng `roof_key 62→80` (ghi chú rõ hệ số ~0.28 mean/target) → **PASS**. Không đổi ngưỡng, không đổi ROI, không bật `allow_dark` cho mái (sẽ làm phẳng vân ngói), không đụng cửa.

`tools/p25_validate_faces.py` sau Phase C: **85 PASS / 0 FAIL** — `git diff --check`, generator `--check --only` 7 asset, `validate_assets.py`, alpha channel byte-identical cả 7, ground row 167/171/205/165/165/167/223, mọi protect rect 0 drifted px, **17+? ROI checks**, sinh lại byte-identical, manifest SHA/size 7 entry.

`docs/visual-evidence/p25/authored-before/` bổ sung 4 PNG baseline lấy từ `9a204e2` (= `b2a7162` với 4 asset này, chưa từng đổi ở ROUND 1). **Chưa có** engine AFTER cho Phase C — đó là bước REGRESSION ở ROUND 3.

## 5. Trạng thái vòng

- ROUND 1: implement + baseline + CI ✅ (không phải vòng sửa lỗi).
- ROUND 2: engine render thật + QA + root cause + **Phase C extension** ✅ (calibration trước CI, không phải cycle sửa sau QA thất bại).
- ROUND 3 (còn lại 1): CI + regression + final review trên SHA Phase C. Nếu B1–B7 chưa đạt sau ROUND 3 → `BLOCKED` trung thực, không hạ rubric.
- `HdrLightResponse.kt` **không đổi**. `LOCAL BUILD NOT RUN`, `PHYSICAL DEVICE`/GPU/FPS/nhiệt = `NOT TESTED`.
