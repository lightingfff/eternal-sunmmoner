# QA_MATRIX — `p24-sanctuary-hdr`

Quy ước: mọi bằng chứng ảnh là `REAL ENGINE ROBOLECTRIC RENDER` (Canvas thật, Robolectric native graphics), không phải contact sheet, không phải emulator, không phải máy thật. Điểm mềm chỉ chấm khi có PNG engine và chỉ số đo được; thiếu → `NOT VERIFIED`.

## Blocker gates (PASS/FAIL)

| # | Gate | Cách đo | Kết quả |
| --- | --- | --- | --- |
| B1 | `main.yml` + `lyra-render-evidence.yml` xanh trên candidate SHA | `gh api actions/runs?head_sha=<sha>`, `gh pr checks` | `PENDING` |
| B2 | 24 khung engine trước/sau cùng camera đủ zoom 0.36/0.88/1.00/1.48 + khung HDR tier/weather | `camera_props.json` diff + danh sách PNG | `PENDING` |
| B3 | Không flood sáng: `\|Δmean\|/mean < 8%` trên `service_cluster_z088` và `tower_gate_whole_z148`; `p90−p10` tăng ≥3.0 trên ≥2/3 khung | script Pillow trên PNG thật | `PENDING` |
| B4 | Không regression hợp đồng `.72` / `FilterQuality.None` / anchors / collider / door / save | `MapForgeP23ContractTest`, `SanctuaryEnvironmentArtTest` | `PENDING` |
| B5 | OFF không vẽ HDR, REDUCED ⊂ BALANCED, không fullscreen bloom/blur/RuntimeShader | soi `hdr_off_z088` vs `hdr_reduced_z088` vs `hdr_balanced_day_z088` | `PENDING` |

## Sáu chiều mềm (0–4, mỗi ≥3, tổng ≥20/24)

| Chiều | Tiêu chí cụ thể | Điểm | Bằng chứng |
| --- | --- | --- | --- |
| Visual coherence | Mái/mặt tiền/mặt bên khác giá trị; chân nhà có AO; cổng/quảng trường có tiêu điểm; không sticker | `—` | `PENDING` |
| Technical correctness | Không lỗi alpha, không lệch foot pivot, lõi sáng đúng pixel nguồn, depth sort đúng | `—` | `PENDING` |
| Player clarity | Đường vào cửa đọc được, HUD không bị che, nguồn sáng phân biệt ấm/lạnh | `—` | `PENDING` |
| Material differentiation | Đá/gỗ/kim loại/mái/kính phản ứng khác nhau; ướt có highlight nhẹ | `—` | `PENDING` |
| Polish | Sắc nét pixel-art, không wash/burn/blur, không "có glow nhưng vẫn phẳng" | `—` | `PENDING` |
| Efficiency | mask cache, cull trước draw, không allocation/frame, tier cắt được việc | `—` | `PENDING` |

## Phân loại bằng chứng (không gộp)

| Nhãn | Nguồn | Trạng thái |
| --- | --- | --- |
| `REAL ENGINE ROBOLECTRIC RENDER` | `docs/visual-evidence/p24/engine-render-before/` (P23) + `docs/visual-evidence/p23/engine-render/` (P24) + `hdr_*.png` | `PENDING CI` |
| `ART CONTACT SHEET` | không dùng làm bằng chứng PASS | `N/A` |
| `EMULATOR` | không có emulator trong sandbox | `NOT TESTED` |
| `PHYSICAL DEVICE` / GPU / nhiệt / FPS | không có thiết bị | `NOT TESTED` |

## Local capability truth

`java`, `gradle`, Android SDK, `/usr/lib/jvm` **KHÔNG CÓ** → `LOCAL BUILD NOT RUN`, `LOCAL ROBOLECTRIC NOT RUN`. Local chỉ chạy `git diff --check`, parse YAML, và Pillow trong venv `/tmp/qa-venv` để đo luminance trên PNG engine thật. `ScenePerformanceProbe` là CPU submission/frame pacing, **không** phải GPU completion hay FPS máy thật.
