# FINAL_REPORT_VI — P26C: Mây Hiển Thị + Trạng Thái Giông (Storm) + Kiểm Tra Camera Gameplay Thật

**Branch**: `arena/06fa6c23-eternal-sunmmoner` · **Base**: `main = f0dc0de` (PR #32 P26B đã merge) · **PR**: chưa mở.

## Trước/Sau người chơi thấy được

| Vựng | Trước (P26B, f0dc0de) | Sau (P26C) |
|---|---|---|
| Mây trong camera chơi | **Không thấy** — 6 cloudSpecs ở world Y = 260…2580, ngoài tầm camera gameplay (visible world Y ≈ 292–1428 ở z0.88) | **Thấy rõ** — layer bầu trời screen-space, 8 neo mây, 3 lớp khoảng cách, luôn nằm trong vùng trời của khung đang chơi |
| Mây theo thời tiết | Mây vẽ nhưng không phân biệt rõ CLEAR/CLOUDY/RAIN | CLEAR (30% coverage) < CLOUDY (70%) < RAIN (85%) < STORM (100%) |
| Trạng thái giông | **Chưa có** — WorldWeather chỉ có CLEAR/CLOUDY/RAIN/FOG | **STORM** — trạng thái thực: mây xám-than #4A5568, ambient lạnh hơn, mặt trời suy giảm, haze nặng, bóng mềm hơn, đèn cổng nổi bật |
| Động mây | Drift cố định theo world, không nhìn thấy | Drift deterministic theo sin(timeSec), parallax nhẹ với camera X |
| Culling mây | Không cần (mây off-screen) | Có — cull trước khi vẽ, không vẽ mây ngoài khung |

## Chi tiết code ownership từng file

| File | Vai trò | Thay đổi |
|---|---|---|
| `app/src/main/java/com/example/game/world/WorldEnvironment.kt` | Technical Artist | Thêm STORM enum, `weatherType` field, vớiWeather multipliers cho STORM, lịch 6 slot |
| `app/src/main/java/com/example/game/pixel/SanctuaryWorldRenderer.kt` | Environment Architect | Viết lại `drawDriftingSkyClouds` (camera-relative sky layer, 3 lớp, pixel-art puff), thêm `drawCloudPuff`, culling |
| `app/src/test/java/com/example/P26CCloudStormContractTest.kt` | QA | Contract test: STORM tồn tại, khác RAIN/CLEAR, chu kỳ có STORM, sky band cache |
| `app/src/test/java/com/example/P26CCloudStormRenderTest.kt` | QA | Render matrix: 13 frames BEFORE+AFTER cùng camera, motion t0/t4/t8, determinism |
| `.github/workflows/lyra-render-evidence.yml` | Integration Reviewer | Thêm path filter, test step, publish p26c PNGs |
| `docs/quality-loop/p26c-cloud-storm/*` | Director | WORK_ORDER, QA_MATRIX, ROUND_1, FINAL_REPORT, thresholds |
| `docs/art/ETERNAL_VISUAL_REFERENCE_BIBLE_V1.md` | Technical Artist | Chuẩn tham khảo từ 8 ảnh (REF01..08) |

## Evidence ledger

| Item | Đường dẫn | Trạng thái |
|---|---|---|
| Baseline SHA | `f0dc0de6b63794082ba5db88244a2cc8c486bcdb` | main (P26B merged) |
| Candidate SHA | TBD (PR commit) | Đang chờ CI |
| Render test | `P26CCloudStormRenderTest` | Robolectric NATIVE, 1280×720 |
| Contract test | `P26CCloudStormContractTest` | Model-level, deterministic |
| BEFORE frames | `docs/visual-evidence/p26c/engine-render/` | TBD (CI render-bot) |
| AFTER frames | `docs/visual-evidence/p26c/engine-render/` | TBD (CI render-bot) |
| Camera props | `docs/visual-evidence/p26c/camera_props.json` | TBD |
| CI URL | TBD | GitHub Actions `lyra-render-evidence.yml` |

## Rubric tổng (chỉ chấm khi có bằng chứng)

| Nhóm | Tiêu chí | Ngưỡng | Kết quả |
|---|---|---|---|
| C — Clouds | C1 Mây thấy ở sky band z0.88 | presence ≥ 5% | TBD |
| | C2 3 lớp khoảng cách | pixel-art, 3 cấp sáng | TBD |
| | C3 CLOUDY dày hơn CLEAR | sky ≤ clear − 3.0 | TBD |
| | C4 STORM đặc trưng | storm ≤ clear − 12, ≤ rain − 2 | TBD |
| | C5 Drift deterministic | byte-identical t=0 | TBD |
| | C6 Cull off-screen | code review | TBD |
| E — Environment | E1–E3 distinctness/mood | P26B thresholds + storm | TBD |
| | E4 No-flood | \|Δmean\| < 8% | TBD |
| | E5 Determinism | byte-identical | TBD |
| P — Performance | Không per-frame alloc | cached buffers | TBD |

## CI status

TBD — PR chưa mở. Sau khi mở PR, GitHub Actions `lyra-render-evidence.yml` chạy trên push nhánh `arena/06fa6c23-eternal-sunmmoner`.

## Hardware / Perf

FPS/GPU/thermal = **NOT TESTED** (không thiết bị thật trong sandbox). Cloud layer thêm ~48 `drawOval`/frame, bounded, không tạo allocation mỗi frame.

## Điểm yếu còn lại (honest)

1. Mây là layer screen-space, parallax với camera X nhưng không va chạm với hình đảo, không đổ bóng trên mái nhà.
2. STORM không có mesh mưa/particle riêng — mood truyền qua sky band, màu mây, haze, giảm nắng, bóng mờ. Không thêm SFX/audio mới.
3. Không có số liệu FPS/GPU/thermal trên thiết bị thật.
4. Lịch thời tiết đổi từ 5 sang 6 slot (chu kỳ 24 phút thay vì 20 phút).

## PR

TBD — sẽ mở PR từ `arena/06fa6c23-eternal-sunmmoner` đến `main`, **không tự merge**, owner review yêu cầu. Cách kiểm tra: `gh pr view <n>`, xem CI render evidence, mở PNG trong `docs/visual-evidence/p26c/engine-render/`.
