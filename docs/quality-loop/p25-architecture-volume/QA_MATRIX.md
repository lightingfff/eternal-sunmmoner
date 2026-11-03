# P25 — QA MATRIX (final, REAL ENGINE ROBOLECTRIC RENDER)

**Baseline**: `b2a71629a87c3099a4267c1590e955c71a8f476e` = fresh `origin/main` (P24 merged). Baseline CI `41618836345` (`workflow_dispatch`, success) → snapshot `docs/visual-evidence/p25/engine-render-before/` (32 PNG, 17.235.610 B) + `docs/visual-evidence/p25/authored-before/` (7 PNG).
**Candidate code**: `9a204e2` (3 pilot) → `ba1056c` (Phase C, 7 facility) → bot evidence `0ace057` / `98732ba` (PNG-only).
**Camera lock**: `docs/visual-evidence/p25/camera_props.json` SHA `ac6a005fd5725d2c…`, `cmp` với `p23/camera_props.json` = **IDENTICAL** trên cả hai vòng CI. Không đổi `timeSec`, weather, actor, world state, zoom, camX/camY giữa before/after.

## Nhãn evidence (không lẫn lộn)

- `REAL ENGINE ROBOLECTRIC RENDER` — 32 PNG trong `docs/visual-evidence/p23/engine-render/` do `MapForgeP23RenderTest` (24) + `HdrLightingRenderTest` (8) vẽ qua `drawSanctuaryFloatingWorld` → `drawGroundShadows` → `drawWorldBackground` → `drawProp` → actor + canopy, 1280×720, `FilterQuality.None`.
- `ART CONTACT SHEET` — chỉ để soi sprite rời (`app/src/main/assets/premium_slice/*.png`), **KHÔNG** dùng làm bằng chứng engine.
- `EMULATOR` / `PHYSICAL DEVICE` — **NOT TESTED**.
- `LOCAL BUILD` — **NOT RUN** (không có JDK/Gradle/Android SDK trong sandbox); mọi build/test lấy từ GitHub Actions thật.

## Camera / world state (24 khung P23 + 8 khung HDR)

| file | camX | camY | zoom | timeSec | weather |
| --- | --- | --- | --- | --- | --- |
| `sanctuary_overview_z036.png` | 1600 | 1380 | 0.36 | 0 | CLEAR |
| `service_cluster_z088.png` | 960 | 860 | 0.88 | 0 | CLEAR |
| `service_cluster_z100.png` | 960 | 860 | 1.00 | 0 | CLEAR |
| `armory_full_z088.png` | 995 | 580 | 0.88 | 0 | CLEAR |
| `armory_full_z100.png` | 995 | 580 | 1.00 | 0 | CLEAR |
| `armory_full_z148.png` | 995 | 560 | 1.48 | 0 | CLEAR |
| `armory_door_trigger_actor_z148.png` | 995 | 585 | 1.48 | 0 | CLEAR |
| `armory_behind_occlusion_z148.png` | 995 | 555 | 1.48 | 0 | CLEAR |
| `quarters_door_actor_z148.png` | 825 | 710 | 1.48 | 0 | CLEAR |
| `restaurant_door_actor_z148.png` | 1105 | 845 | 1.48 | 0 | CLEAR |
| `registry_close_z148.png` | 785 | 1105 | 1.48 | 0 | CLEAR |
| `cottages_nw_z148.png` | 575 | 865 | 1.48 | 0 | CLEAR |
| `synthesis_cottage_z148.png` | 1320 | 480 | 1.48 | 0 | CLEAR |
| `summoning_hall_whole_z148.png` | 2430 | 1715 | 1.48 | 0 | CLEAR |
| `summoning_hall_zone_actor_z100.png` | 2405 | 1855 | 1.00 | 0 | CLEAR |
| `tower_gate_whole_z148.png` | 1960 | 310 | 1.48 | 0 | CLEAR |
| `tower_gate_portal_actor_z100.png` | 1960 | 460 | 1.00 | 0.25 | CLEAR |
| `avenue_props_lantern_f0_z148.png` | 1185 | 1015 | 1.48 | 0 | CLEAR |
| `avenue_props_lantern_f1_z148.png` | 1185 | 1015 | 1.48 | 0.18 | CLEAR |
| `south_gate_training_z088.png` | 1320 | 1980 | 0.88 | 0 | CLEAR |
| `tower_f1_ruins_lantern_z088.png` | 580 | 1090 | 0.88 | 0 | CLEAR |
| `tower_f1_ruin_arch_close_z148.png` | 640 | 890 | 1.48 | 0 | CLEAR |
| `tower_f2_ruin_arch_plum_z088.png` | 511.91 | 639.08 | 0.88 | 0 | CLEAR |
| `tower_f4_obsidian_column_z148.png` | 1591.59 | 562.35 | 1.48 | 0 | CLEAR |
| `hdr_off_z088.png`, `hdr_reduced_z088.png`, `hdr_balanced_day_z088.png` | 960 | 860 | 0.88 | 0 | OFF / REDUCED / BALANCED |
| `hdr_balanced_night_z088.png`, `hdr_balanced_night_rain_z088.png` | 960 | 860 | 0.88 | night | CLEAR / RAIN |
| `hdr_balanced_night_overview_z036.png` | 1600 | 1380 | 0.36 | night | CLEAR |
| `hdr_balanced_night_tower_gate_z100.png` / `_z148.png` | 1960 | 310 | 1.00 / 1.48 | night | CLEAR |

## Full-frame trước/sau (32 khung, Rec.709 mean / p10 / p90)

| file | mean → | Δmean% | spread → | Δspread |
| --- | --- | --- | --- | --- |
| `sanctuary_overview_z036.png` | 70.60 → 70.40 | −0.28% | 57.6 → 57.6 | +0.0 |
| `service_cluster_z088.png` | 62.76 → 62.29 | −0.75% | 53.1 → 56.0 | +2.9 |
| `service_cluster_z100.png` | 61.44 → 61.02 | −0.68% | 52.7 → 55.6 | +2.9 |
| `armory_full_z088.png` | 64.24 → 63.74 | −0.78% | 49.0 → 51.9 | +2.9 |
| `armory_full_z100.png` | 63.16 → 62.52 | −1.01% | 50.0 → 52.8 | +2.8 |
| `armory_full_z148.png` | 60.24 → 59.34 | −1.49% | 50.3 → 55.4 | +5.1 |
| `armory_door_trigger_actor_z148.png` | 59.71 → 58.79 | −1.54% | 50.6 → 56.0 | +5.4 |
| `armory_behind_occlusion_z148.png` | 60.27 → 59.38 | −1.48% | 50.2 → 55.3 | +5.1 |
| `quarters_door_actor_z148.png` | 58.32 → 57.46 | −1.47% | 50.0 → 55.3 | +5.3 |
| `restaurant_door_actor_z148.png` | 57.25 → 56.25 | −1.75% | 51.4 → 57.7 | +6.3 |
| `registry_close_z148.png` | 56.85 → 56.59 | −0.46% | 48.8 → 52.1 | +3.3 |
| `cottages_nw_z148.png` | 61.23 → 60.82 | −0.67% | 56.0 → 59.0 | +3.0 |
| `synthesis_cottage_z148.png` | 58.58 → 57.48 | **−1.88%** | 51.8 → 56.0 | +4.2 |
| `summoning_hall_whole_z148.png` | 67.59 → 66.97 | −0.92% | 59.3 → 60.4 | +1.1 |
| `summoning_hall_zone_actor_z100.png` | 67.15 → 66.88 | −0.40% | 57.6 → 57.9 | +0.3 |
| `tower_gate_whole_z148.png` | 59.35 → 58.66 | −1.16% | 56.6 → 60.3 | **+3.7** |
| `tower_gate_portal_actor_z100.png` | 64.69 → 64.27 | −0.65% | 58.0 → 59.2 | +1.2 |
| `avenue_props_lantern_f0_z148.png` | 64.19 → 63.43 | −1.18% | 55.1 → 60.2 | +5.1 |
| `avenue_props_lantern_f1_z148.png` | 64.58 → 63.81 | −1.19% | 55.1 → 60.9 | +5.8 |
| `south_gate_training_z088.png` | 61.37 → 61.37 | +0.00% | 55.9 → 55.9 | +0.0 |
| `tower_f1_ruins_lantern_z088.png` | 76.55 → 76.55 | +0.00% | 89.3 → 89.3 | +0.0 |
| `tower_f1_ruin_arch_close_z148.png` | 78.55 → 78.55 | +0.00% | 99.0 → 99.0 | +0.0 |
| `tower_f2_ruin_arch_plum_z088.png` | 39.80 → 39.80 | +0.00% | 25.8 → 25.8 | +0.0 |
| `tower_f4_obsidian_column_z148.png` | 39.53 → 39.53 | +0.00% | 42.1 → 42.1 | +0.0 |
| `hdr_off_z088.png` | 79.96 → 79.41 | −0.69% | 48.3 → 48.8 | +0.5 |
| `hdr_reduced_z088.png` | 80.22 → 79.67 | −0.69% | 48.3 → 49.9 | +1.6 |
| `hdr_balanced_day_z088.png` | 79.91 → 79.49 | −0.53% | 47.4 → 50.3 | +2.9 |
| `hdr_balanced_night_z088.png` | 63.47 → 63.15 | −0.50% | 43.6 → 45.1 | +1.5 |
| `hdr_balanced_night_rain_z088.png` | 61.43 → 61.15 | −0.46% | 37.9 → 40.6 | +2.7 |
| `hdr_balanced_night_overview_z036.png` | 58.87 → 58.74 | −0.22% | 40.4 → 40.5 | +0.1 |
| `hdr_balanced_night_tower_gate_z100.png` | 60.78 → 60.48 | −0.49% | 51.3 → 51.4 | +0.1 |
| `hdr_balanced_night_tower_gate_z148.png` | 57.05 → 56.56 | −0.86% | 45.6 → 48.5 | +2.9 |

**B4 no-flood**: `max |Δmean|` trên 3 khung khoá P24 = **1.16%**, trên **toàn 32 khung = 1.88%** → **PASS** (<8%). Không khung nào sáng hoá; không wash tier OFF/REDUCED/BALANCED, night vẫn tối hơn day, rain vẫn tối/hazy hơn night clear.

**Disclosure cổng P24** `Δ(p90−p10) ≥ +3 trên ≥2/3`: overview `+0.0`, service z088 `+2.9`, tower z148 `+3.7` → **1/3, FAIL**. P24 tự thân từng đạt 1/3 và vẫn `BLOCKED`. **P25 KHÔNG đổi ngưỡng, KHÔNG gọi FAIL thành PASS, KHÔNG tăng brightness toàn cục để đạt chỉ số.** Báo cáo hai hệ số độc lập: ROI separation (B5, PASS) và whole-frame p90/p10 (disclosure, FAIL). Điểm yếu của chỉ số whole-frame đã ghi ở `WORK_ORDER.md`: nền cỏ/đường/cây/hốc tối chiếm đa số khung, nên nó không phản ánh cạnh khối công trình.

## B5 — ROI mặt phẳng trên PNG tài sản (ngưỡng chốt trước candidate)

| asset | front−side | roof−return | \|roof−front\| | front−soffit | roof spread | door gap | verdict |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `hero_quarters` | −1.57 → **19.77** | 5.14 → 11.13 | 11.08 → 13.86 | 26.95 → 41.01 | 32.8 → 43.8 | 21.53 → 27.12 | PASS 6/6 |
| `restaurant` | 0.14 → **23.51** | 0.94 → **23.74** | 2.20 → **15.67** | 35.24 → 53.21 | 12.9 → 16.6 | 29.56 → 39.06 | PASS 6/6 |
| `summoning_hall` | 12.14 → **33.16** | −7.98 → **22.12** | 15.15 → 18.16 | 0.31 → **60.02** | 26.3 → 32.6 | n/a | PASS 5/5 |
| `armory` | −2.43 → **19.72** | 1.14 → 14.35 | 18.51 → 15.19 | 31.19 → 44.36 | 10.4 → **38.9** | 25.42 → 30.25 | PASS 6/6 |
| `hero_registry` | 1.09 → **24.08** | 1.22 → 26.10 | 4.26 → 11.11 | 29.69 → 46.85 | 14.6 → 18.7 | n/a | PASS 5/5 |
| `synthesis_lab` | −4.35 → **13.90** | −4.86 → 20.09 | 3.65 → 10.47 | 31.36 → 44.50 | 12.9 → 17.6 | n/a | PASS 5/5 |
| `tower_gate` | 0.59 → **26.10** | 18.63 → 22.63 | 1.31 → 15.69 | 13.71 → 42.76 | 42.3 → 33.6 | n/a | PASS 5/5 |

→ **38/38 PASS**. `portal_aperture` (Tower Gate) = 20.36 → **20.36** và `rune_stage` (Summoning Hall) = 103.4 → **103.4**: hốc tối và vòng rune cyan **không đổi một pixel**.

## ROI chiếu lên ảnh engine (corroboration, không phải gate cứng)

| khung | tiêu biểu | before → after |
| --- | --- | --- |
| `service_cluster_z088` z0.88 | restaurant front−side | 8.10 → **27.93** |
| `service_cluster_z088` z0.88 | hero_quarters front−side | 3.57 → 18.78 |
| `service_cluster_z100` z1.00 | restaurant roof−return | 12.70 → 31.31 |
| `service_cluster_z100` z1.00 | hero_registry front−side | 9.43 → 27.41 |
| `quarters_door_actor_z148` | HQ front−side | 7.00 → 20.90 |
| `restaurant_door_actor_z148` | restaurant front−side | 12.55 → 30.71 |
| `armory_full_z148` / `armory_door_trigger_actor_z148` | armory roof−return | 18.31 → 29.71 / 18.24 → 29.65 |
| `registry_close_z148` | registry front−side | 11.26 → 28.36 |
| `synthesis_cottage_z148` | synthesis roof−return | 10.24 → 30.17 |
| `summoning_hall_whole_z148` | hall front−soffit | **−5.55 → 47.92** |
| `tower_gate_whole_z148` | gate front−side | 22.08 → 41.98 |
| `sanctuary_overview_z036` | restaurant front−side | 0.52 → 24.24 |

**Giới hạn trung thực**: phép chiếu ROI lên ảnh engine lẫn các sprite khác do depth sort, actor, canopy, props và grade nền. `armory front−side` trên engine chỉ −4.55 → +4.44 và `synthesis_lab` −8.80 → −2.53 dù authored PNG đạt 19.72 / 13.90; soi trực tiếp `armory_full_z148.png` và `synthesis_cottage_z148.png` cho thấy khối nhà vẫn tách rõ, phần chồng lấp là cottage/tiền cảnh + hốc loggia/bay được bảo vệ có chủ đích. Vì vậy B5 đo trên **PNG tài sản** (gate cứng), còn engine ROI là **corroboration** có chú thích nhiễu — không tự nâng điểm.

## Soi ảnh engine AFTER (visual QA, z0.88 / z1.00 / z1.48 / z0.36)

| khung | mái | mặt trước | mặt bên | cửa / hốc | ghi chú |
| --- | --- | --- | --- | --- | --- |
| `service_cluster_z088.png` | pitch/return tách | sáng ấm, timber đọc được | xanh-đen tối rõ | cửa lõm, jamb sáng | 7 facility đều có return tối; background `sanctuary_p03` vẫn phẳng |
| `service_cluster_z100.png` | ngói/coursing rõ | quoin/góc đỡ | tách khỏi front | hood + bậc nguyên | không flood, không wash |
| `quarters_door_actor_z148.png` | shingle ấm | plaster sáng | navy tối | **actor đứng đúng hốc** | P20/P21 trigger không đổi |
| `restaurant_door_actor_z148.png` | slate / return tối | awning + timber | tối rõ | đúng trigger | — |
| `armory_door_trigger_actor_z148.png` | shingle tương phản | front sáng | return tối | đúng trigger (993,655) | CI contract xanh |
| `summoning_hall_whole_z148.png` | pediment / return tách | cột + cella sáng | hồi phải tối | hốc đen + rune cyan nguyên | **band soffit dày ~13px hơi nặng** |
| `tower_gate_whole_z148.png` | battlement / return | trái key-lit | phải shaded | **rift đen + oval + spill cyan nguyên** | không glow, không đổi emitter P24 |
| `hdr_balanced_night_rain_z088.png` | giữ tách mặt | ấm cửa sổ | tối | — | tier/weather không đổi |
| `sanctuary_overview_z036.png` | nâu / slate / đá khác nhau | — | — | — | 7 facility identity đọc được; chi tiết nhỏ ở 0.36 |

**Sáu chiều mềm (chấm trên ảnh engine thật)**: visual coherence **3/4**, technical correctness **4/4**, player clarity **3/4**, material differentiation **3/4**, polish **3/4**, efficiency **4/4** = **20/24** → đạt `mỗi ≥3, tổng ≥20`. B6 = **3/4** tại z0.88 và z1.00 cho 7 facility. Không tự cho 4/4 vì band soffit Summoning Hall hơi nặng và background houses chưa sửa.

## Hard gates

| gate | kết quả | bằng chứng |
| --- | --- | --- |
| **A** không lỗ / floating / lệch cửa / fallback crash / manifest lệch / portal glow | **PASS** | `p25_validate_faces.py` 85/85; alpha channel byte-identical 7 asset; foot row 167/171/205/165/165/167/223; protect rect 0 drifted px; `MapForgeP23ContractTest`, `SanctuaryMaterialTest`, `HdrLightingRenderTest` xanh; `portal_aperture` 20.36→20.36 |
| **B** không regression gameplay/camera/pixel/perf | **PASS** | `OBLIQUE_Y_SCALE=0.72f` không đổi; không đụng `WorldMapModels.kt`/`PixelSpriteRenderer.kt`/`HdrLightResponse.kt`/gameplay/PR #28; `comm -12` giao thoa rỗng; không thêm draw call, không bitmap per-frame, không fullscreen pass, OFF/REDUCED/BALANCED giữ nguyên |
| **C** real engine before/after | **PASS** | 32 khung × 2, camera props identical, `docs/visual-evidence/p25/engine-render-before/` + `p23/engine-render/` |
| **D** Android APK / contract / render suites trên candidate SHA | **PASS** | `41620768746` + `41620768647` trên `9a204e2`; `41622749053` + `41622749135` trên `ba1056c` — cả 4 `success` |
| **E** P24 no-flood ≤8% + disclose cổng p90/p10 | **PASS / DISCLOSED** | max 1.88%; cổng P24 **1/3 FAIL** báo cáo độc lập, không relabel |
| **F** ROI roof/front/side separation + reviewer z0.88/1.00 ≥3/4 | **PASS** | 38/38 ROI; B6 3/4 từ ảnh engine thật |

## Alpha gap scan / glyph / window / step / door / anchors

`tools/mapforge_p23_architecture.py --check --only <7>`: `col_gaps` = 0, `opaque_pct` ≥ 28%, ground row chạm canvas; `MapForgeP23ContractTest` quét mọi cột trong silhouette (không lỗ alpha giữa khối), eave/fascia phải opaque, alpha chỉ 0/255 (không halo bán trong suốt). Các vùng tối có chủ đích được **bảo vệ và ghi chú**: 3 vòm loggia `hero_registry`, workshop bay `synthesis_lab`, hốc rift `tower_gate`, rune stage `summoning_hall`, cửa + jamb của `armory`/`hero_quarters`/`restaurant`. Đây là negative space có chủ đích, **không** bị quy thành lỗi alpha. Glyph/cửa sổ/`AMBER`/`CYAN` emissive không grade; bậc cửa, jamb, plinth, foot row và `ground_anchor` giữ nguyên.

## Palette IDs (giữ nguyên, chỉ dịch/giãn giá trị)

`STONE`, `LIME`, `LIME_T`, `BASA`, `GATE`, `PLAS`, `WALLSTONE`, `SLATE`, `SHING`, `THATCH`, `OAK`, `IRON`, `AMBER`, `CYAN`, `TEAL`, `VIOL`, `MOSS`, `BRONZE`, `AO`, `CONTACT`. Grammar P25 thêm **face target** (mean/gain/hue) trong `roi_definitions.json`, **không** thêm màu mới và không đụng palette gốc. Hue push nhỏ theo face: key ấm (`R↑ B↓`), return/shade lạnh (`B↑ R↓`), plinth mossy (`G↑`).

## Perf / tài nguyên

- 7 PNG tài sản: **43.731 B → 51.912 B** (+8.181 B, +18.7%). Không PNG nào vượt 3 MB; `validate_assets.py` PASS. Không thêm asset runtime.
- Runtime: **0 draw call mới, 0 bitmap decode mới, 0 gradient/path allocation mỗi frame, 0 fullscreen pass**. Grammar chạy **offline lúc generate**, engine chỉ decode PNG cache như cũ. `HdrLightResponse` giữ 7 cached mask (`HdrLightingRenderTest`: `masks must not be rebuilt`, `cachedMaskCount == 7`).
- Determinism: sinh lại 2 lần → byte-identical (SHA trong `ROUND_1.md` / validator 85 PASS).
- `ScenePerformanceProbe` / `ScenePerformanceTest` chỉ CPU submission + frame pacing. **FPS / GPU / thermal / EMULATOR / PHYSICAL DEVICE = NOT TESTED** (không có thiết bị, không bịa số).

## CI phân biệt theo SHA (không gán success của SHA cũ cho head)

| SHA | loại | Build APK | Render evidence |
| --- | --- | --- | --- |
| `b2a71629…` | baseline main | `41616933713` success (push main) | `41618836345` success (dispatch) |
| `9a204e2` | code 3 pilot | `41620768746` **success** | `41620768647` **success** |
| `0ace057` | bot PNG-only | `41621517573` **action_required** | `41621517619` **action_required** |
| `ba1056c` | code Phase C / 7 facility | `41622749053` **success** | `41622749135` **success** |
| `98732ba` | bot PNG-only (head lúc chốt) | `41623486097` **action_required** | `41623485866` **action_required** |

`action_required` = workflow chờ phê duyệt cho commit của bot (`eternal-render-bot`), **KHÔNG** phải lỗi compiler/test. Hai SHA code có CI xanh là `9a204e2` và `ba1056c`. Không có lỗi tải artifact ZIP ở P25.
