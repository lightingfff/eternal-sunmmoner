# QA_MATRIX — `p24-sanctuary-hdr` (ĐÃ CHỐT KẾT QUẢ)

Quy ước: mọi bằng chứng ảnh là `REAL ENGINE ROBOLECTRIC RENDER` (Canvas thật, Robolectric native graphics), không phải contact sheet, không phải emulator, không phải máy thật. Điểm mềm chỉ chấm khi có PNG engine và chỉ số đo được.

- Code SHA cuối: `c0e2c034f9d97364179f9c9b6a6ff59cecd3a89b`. Mọi commit sau đó trên nhánh chỉ là **PNG evidence** (bot `dcdc870`) và **báo cáo docs** — không đổi source. PR #30 OPEN, chưa merge.
- BEFORE: `docs/visual-evidence/p24/engine-render-before/` — 24 PNG / **7 356 408 B**. AFTER: `docs/visual-evidence/p23/engine-render/` — 32 PNG. `camera_props.json` không đổi.

## Blocker gates

| # | Gate | Cách đo | Kết quả |
| --- | --- | --- | --- |
| B1 | `main.yml` + `lyra-render-evidence.yml` xanh trên candidate SHA | `41589948067` (success, attempt 2) · `41589948058` (success, 15/15 step) | **PASS** |
| B2 | 24 khung trước/sau cùng camera đủ 0.36/0.88/1.00/1.48 + 8 khung HDR | danh sách PNG + `git diff main -- camera_props.json` rỗng | **PASS** |
| B3 | `\|Δmean\|/mean < 8%` trên 2 khung VÀ `p90−p10` tăng ≥3 trên ≥2/3 khung | `measure_luminance.py` | **FAIL**: no-flood 3/3 PASS, contrast-gain **1/3** (+0.0 / +0.0 / +5.4) |
| B4 | Không regression `.72` / `FilterQuality.None` / anchors / collider / door / save | contract + environment + performance suites; `comm -12` với PR #28 rỗng | **PASS** |
| B5 | OFF không vẽ HDR, REDUCED ⊂ BALANCED, không fullscreen bloom/blur/RuntimeShader | `hdr_off` 406 741 B / `hdr_reduced` 443 296 B / `hdr_balanced_day` 669 462 B; soi diff | **PASS** |
| B6 | Không còn "glow nhưng vẫn phẳng" ở 0.88/1.00 | soi trực tiếp `service_cluster_z100.png` | **FAIL** |

## Sáu chiều mềm (0–4)

| Chiều | Điểm | Bằng chứng thật |
| --- | --- | --- |
| Visual coherence | **2** | `tower_gate_whole_z148` tách khối rõ (+5.4 spread), nhưng `service_cluster_z100` mái/tường/mặt bên vẫn phẳng; `summoning_hall_whole_z148` −3.8 spread |
| Technical correctness | **4** | `MapForgeP23ContractTest`, `SanctuaryEnvironmentArtTest`, `ScenePerformanceTest`, `DepthAddendumTest` xanh; lõi sáng đặt theo pixel sprite; hốc rift vẫn tối; không lỗi alpha |
| Player clarity | **3** | cửa vào đọc được, phân biệt ấm/lạnh, không che HUD; quầng cửa sổ còn nhạt |
| Material differentiation | **3** | bảng `STONE/WOOD/METAL/ROOF/CRYSTAL` khác hue & cường độ trong code và thể hiện rõ ở Tower Gate; còn nhạt ở cụm nhà |
| Polish | **3** | pixel sắc (`FilterQuality.None`), không wash/burn/blur, mean tối đa +3.60%; nhưng chưa đạt premium ở zoom chơi |
| Efficiency | **4** | 7 mask `by lazy` + silhouette `Path` cache (test `cachedMasksAreReusedAndExposureIsBounded` xanh), cull trước draw, 3 tier, không allocation/frame |
| **Tổng** | **19/24 (<20), có chiều <3** | **FAIL** |

## Phân loại bằng chứng (không gộp)

| Nhãn | Nguồn | Trạng thái |
| --- | --- | --- |
| `REAL ENGINE ROBOLECTRIC RENDER` | 24 BEFORE + 24 AFTER + 8 `hdr_*`, CI `41589948058` | **CÓ**, đã soi trực tiếp |
| `ART CONTACT SHEET` | không dùng làm bằng chứng PASS | `N/A` |
| `EMULATOR` | không có emulator | `NOT TESTED` |
| `PHYSICAL DEVICE` / GPU / nhiệt / FPS | không có thiết bị | `NOT TESTED` |

## Local capability truth

`java`, `gradle`, Android SDK, `/usr/lib/jvm` **KHÔNG CÓ** → `LOCAL BUILD NOT RUN`, `LOCAL ROBOLECTRIC NOT RUN`. Local chỉ chạy `git diff --check`, parse YAML, và Pillow (venv `/tmp/qa-venv`) để đo luminance trên PNG engine thật. `ScenePerformanceProbe` là CPU submission/frame pacing, **không** phải GPU completion hay FPS máy thật.

## Kết luận

**`BLOCKED` / KHÔNG TỰ NHẬN PASS.** Chi tiết số đo từng vòng: `ROUND_1.md` → `ROUND_4.md`; blocker: `BLOCKERS.md`; báo cáo owner: `FINAL_REPORT_VI.md`.
