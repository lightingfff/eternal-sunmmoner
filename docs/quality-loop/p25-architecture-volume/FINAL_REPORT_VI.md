# P25 — BÁO CÁO CUỐI (Architecture Face / Material Volume Repair)

**Ngày**: 2026-11-03 · **Baseline**: `b2a71629a87c3099a4267c1590e955c71a8f476e` (fresh `origin/main`, P24 đã merge) · **Nhánh**: `arena/e486a516-eternal-sunmmoner` · **PR mới**: **#31** (mở, chưa merge, owner tự duyệt) · **Không auto-merge**, không đụng lại PR #30 (đã MERGED), không đụng PR #28 / Mission01.

## 1. Vấn đề đã đo được, không phải phỏng đoán

Ảnh engine `service_cluster_z100.png` trên main: mái, tường trước, mặt hồi của các nhà dịch vụ đọc thành **một mảng xám**. Đo ROI trên chính PNG tài sản (Rec.709): `restaurant` bốn mặt lệch ≤2.34 (roof 65.93 / return 64.99 / front 63.73 / side 63.59); `hero_quarters` mặt bên **sáng hơn** mặt trước (+1.57); `summoning_hall` hip return **sáng hơn** pitch chính (+7.98) và cornice không có band tối. Root cause là **(b) giá trị các mặt phẳng bị dồn cụm + (g) vật liệu không khác biệt ngay trong sprite đã author**, không phải lỗ alpha (P23 đã sửa) cũng không phải thiếu pass ánh sáng runtime. P24 `drawVolumeGrade` là gradient theo không gian trên toàn silhouette nên chỉ nhuốm màu, không tạo được cạnh khối — đó là lý do `summoning_hall_whole_z148` mất 3.8 `p90−p10`.

## 2. Đã làm

- `tools/p25_face_grammar.py`: grammar **baked OFFLINE** vào PNG. Với mỗi đa giác mặt phẳng: `newL = target + (L − mean)·gain`, scale RGB, đẩy hue nhẹ rồi chuẩn hoá lại đúng `newL` → giữ nguyên vân ngói / coursing / gỗ, chỉ dịch và giãn tương phản. Bảo vệ tuyệt đối: cửa + jamb (P20/P21), cửa sổ `AMBER`, `CYAN`/`TEAL` magic, hốc rift Tower Gate, vòng rune Summoning Hall, 3 vòm loggia, workshop bay; viền tối dưới `dark_floor=40` không grade. Mỗi pixel grade đúng 1 lần theo priority.
- `tools/mapforge_p23_architecture.py`: thêm `--only` để sinh đúng danh sách asset và gọi `apply_face_grade()` sau `seal_seams`/`prune_specks`, trước `validate()`. **Không đổi** geometry, alpha, canvas, foot row, anchor, `OBLIQUE_Y_SCALE=0.72f`, collider/door/save/depth, `FilterQuality.None`, `HdrLightResponse.kt`, gameplay, PR #28.
- Sinh lại **đúng 7 facility**: `hero_quarters`, `restaurant`, `summoning_hall` (ROUND 1) rồi `armory`, `hero_registry`, `synthesis_lab`, `tower_gate` (ROUND 2 / Phase C), cộng 7 entry `manifest.json`. Tổng 43.731 B → 51.912 B (+8.181 B).
- `docs/quality-loop/p25-architecture-volume/`: `WORK_ORDER.md` (rubric + ROI + ngưỡng **trước** candidate), `roi_definitions.json` (một nguồn duy nhất), `measure_rois.py`, `roi_measurements.{json,txt}`, `ROUND_1/2/3.md`, `QA_MATRIX.md`, `BLOCKERS.md`, báo cáo này.
- `tools/p25_validate_faces.py` + bước CI `Verify P25 face grammar and asset invariants`, path filter, guard snapshot baseline.
- `docs/visual-evidence/p25/`: 32 ảnh engine baseline bất biến (từ CI `41618836345` trên `b2a7162`), 7 PNG tài sản baseline, `camera_props.json`.

## 3. Bằng chứng ảnh engine thật (REAL ENGINE ROBOLECTRIC RENDER)

`docs/visual-evidence/p23/engine-render/` (AFTER, 32 khung) so với `docs/visual-evidence/p25/engine-render-before/` (BEFORE). Camera `0.36 / 0.88 / 1.00 / 1.48`, actor ở cửa HQ / Restaurant / Armory, canopy occlusion, đêm / mưa, tier OFF / REDUCED / BALANCED — **camera props so sánh `cmp` = IDENTICAL**.

Ảnh tiêu biểu đã soi trực tiếp: `service_cluster_z088.png`, `service_cluster_z100.png`, `quarters_door_actor_z148.png`, `restaurant_door_actor_z148.png`, `armory_full_z148.png`, `armory_door_trigger_actor_z148.png`, `registry_close_z148.png`, `synthesis_cottage_z148.png`, `summoning_hall_whole_z148.png`, `tower_gate_whole_z148.png`, `hdr_balanced_night_rain_z088.png`, `sanctuary_overview_z036.png`. Actor vẫn đứng đúng hốc cửa; hốc rift Tower Gate vẫn đen + oval + spill cyan; vòng rune Summoning Hall không đổi.

| chỉ số | before → after | gate |
| --- | --- | --- |
| ROI `front−side` (7 asset, PNG) | âm / ≈0 → **13.90 … 33.16** | ≥12 → **38/38 PASS** |
| ROI `roof_key−return` | −7.98 … 18.63 → **11.13 … 26.10** | ≥10 → PASS |
| ROI engine `restaurant front−side` z0.88 | 8.10 → **27.93** | corroboration |
| ROI engine `summoning_hall front−soffit` z1.48 | **−5.55 → 47.92** | corroboration |
| `\|Δmean\|` tối đa / 32 khung | **1.88%** | <8% → **PASS** |
| `portal_aperture` / `rune_stage` | 20.36→20.36 / 103.4→103.4 | không wash → PASS |
| visual coherence z0.88 & z1.00 | **3/4**, mềm **20/24** | ≥3/4, ≥20/24 → PASS |

## 4. CI / hợp đồng (phân biệt theo SHA)

| SHA | Build APK | Render + contract |
| --- | --- | --- |
| `9a204e2` (code 3 pilot) | `41620768746` **success** | `41620768647` **success** |
| `ba1056c` (code 7 facility) | `41622749053` **success** | `41622749135` **success** |
| `0ace057` / `98732ba` (bot PNG-only) | **`action_required`** | **`action_required`** |

`action_required` = chờ phê duyệt workflow cho commit bot, **không phải lỗi build/test**. Validator nội bộ **85 PASS / 0 FAIL** (alpha byte-identical, foot row, protect rect, manifest SHA/size, sinh lại 2 lần byte-identical, 38 ROI checks). `MapForgeP23ContractTest`, `HdrLightingRenderTest`, `SanctuaryEnvironmentArtTest`, `ScenePerformanceTest`, `validate_assets.py` đều xanh trong CI. `git diff --check` sạch; giao thoa với 29 file PR #28 = rỗng. `LOCAL BUILD NOT RUN` (không JDK/Gradle/Android SDK), mọi build lấy từ GitHub Actions thật.

## 5. Còn chưa đạt / chưa kiểm tra (không giấu)

- **Cổng whole-frame P24 `Δ(p90−p10) ≥ +3 trên ≥2/3` vẫn FAIL 1/3** (`+0.0 / +2.9 / +3.7`). P25 **báo cáo độc lập**, không đổi ngưỡng, không relabel thành PASS, không tăng brightness toàn cục. Hai hệ số (ROI separation và whole-frame) được báo cáo riêng biệt. Trạng thái chất lượng P24 vẫn mở dù PR #30 đã merge.
- 6 background `sanctuary_p03` houses/cottages vẫn phẳng (ngoài scope `WORK_ORDER.md`) → kéo coherence toàn khu xuống, nên visual coherence chấm 3/4 chứ không 4/4.
- Band soffit Summoning Hall dày ~13px ở z1.48 hơi nặng; engine-projected ROI của `armory`/`synthesis_lab` còn nhiễu do sprite chồng lấp (ảnh soi trực tiếp vẫn đọc khối rõ).
- `EMULATOR` / `PHYSICAL DEVICE` / **FPS / GPU / thermal = NOT TESTED**. `ScenePerformanceProbe` chỉ CPU submission + frame pacing. Không có số đo hiệu năng điện thoại nào được phát minh.
- Chi tiết 6 sub-ticket có chừng mực: `BLOCKERS.md`.

## 6. Kết luận

Đã **implement trong repository**, không chỉ viết plan: sửa giá trị mặt phẳng/material ngay trong tài sản, chạy vòng lặp DISCOVER → BASELINE → logical specialists → IMPLEMENT → REAL ENGINE RENDER → VISUAL QA → ROOT CAUSE → TARGETED FIX → REGRESSION → FINAL REVIEW trong **3 vòng**, có baseline CI thật, ảnh before/after cùng camera, ROI/threshold chốt trước sửa, và PR mới để review. **Không tự merge, không tuyên bố 100%, không "masterpiece", không claim hiệu năng điện thoại.** Owner review PR #31 và tự quyết.
