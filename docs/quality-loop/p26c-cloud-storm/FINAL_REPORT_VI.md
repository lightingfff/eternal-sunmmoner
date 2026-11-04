# P26C — báo cáo tích hợp PR #33 (đang QA, chưa merge)

**Nhánh** `arena/06fa6c23-eternal-sunmmoner` · **P26B baseline** `f0dc0de` ·
**PR** [#33](https://github.com/lightingfff/eternal-sunmmoner/pull/33) OPEN, không tự merge.
**Trạng thái trước Round 2**: CI đã xanh trên SHA `c255395`, nhưng **visual FAIL**;
không kết luận P26C hoàn thành chỉ nhờ Gradle thành công.

## Phần đã có mã chạy thật

- `WorldEnvironment.kt`: enum STORM, sáu slot thời tiết, `WeatherState.weatherType`,
  ánh sáng/bóng/sun/haze theo loại thời tiết. Không lưu `WorldWeather` dưới dạng ordinal;
  toàn bộ usage trong `app/src/main` chỉ là runtime/debug override, không thay save schema.
- `SanctuaryWorldRenderer.kt`: mây screen-space 3 lớp/8 neo; drift deterministic, cull;
  không cấp phát Brush/Path mới trong hot loop vẽ mây.
- `P26CCloudStorm{Contract,Render}Test.kt`: test state/ánh sáng/cache và ma trận
  Robolectric native renderer. Thiếu import `GraphicsMode` được sửa sau khi người dùng
  chỉ đúng lỗi import `WeatherState`/`toArgb` (commit `ac973d0`).
- `.github/workflows/lyra-render-evidence.yml`: trên **cùng SHA**, chạy tuần tự
  compile → P26C contract → P26C render → P17–P26 regression → assembleDebug.

## Bằng chứng Round 1 (thất bại thị giác, không tô hồng)

| Mốc | Nguồn | Kết quả |
|---|---|---|
| Compile/P26C contract/P26C render/regressions/assemble | [Actions 41686884922](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41686884922), SHA `c255395` | **SUCCESS** |
| APK build workflow | [Actions 41686885163](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41686885163), SHA `c255395` | **SUCCESS** |
| Ảnh AFTER thực | [P26C PNGs](../../visual-evidence/p26c/engine-render/), commit `c466d65` của render bot | 17 PNG, có `camera_props.json` |
| Ảnh BEFORE thực | [P26B frames](../../visual-evidence/p26c/engine-render-before/), [SHA/camera manifest](../../visual-evidence/p26c/before_manifest.json) | 7 cặp cùng hour/weather/camera/zoom/tier, từ `f0dc0de` |
| C1 mây CLEAR z0.88 | so `|L_after−L_before|>12` trên toàn top25% | **FAIL: 0%**, yêu cầu ≥5%; phép đo cũ ~69% là false positive |
| C4 STORM vs RAIN | noon sky luminance 84.68 vs 82.06 | **FAIL: STORM sáng hơn +2.62**, yêu cầu tối hơn ít nhất 2 |
| E3 weather mood | yêu cầu STORM < RAIN | **FAIL** |
| E4 no-flood | so world y≥42% của 7 cặp matched | **PASS trên các cặp có BEFORE**; STORM không có baseline cùng state |

Chi tiết `QA_MATRIX.md`, `ROUND_1.md`, ngưỡng không đổi `thresholds.json`.
**Chưa có bằng chứng STORM BEFORE** vì enum STORM chưa tồn tại ở P26B; không dựng ảnh giả.
Bookmark zoom 1.48 tại khu service bị công trình che hết trời: không được gán PASS
cho độ hiện mây; Round 2 bổ sung camera gate gameplay z1.48 có vùng trời thật.

## Round 2 — thay đổi và kiểm thử chờ ảnh mới

- Đưa neo mây vào khe trời *đang lộ trong camera gameplay*, chỉnh body/shade/rim
  để mây đọc được khi trời nắng và giông, **không giảm số neo/VFX**.
- STORM tối hơn ở **sky bands** (không phủ màn hình tối), khóa cache sky bands theo
  weatherType để RAIN/STORM không dùng nhầm màu khi chuyển trạng thái.
- `tools/p26c_measure_clouds.py` dùng đúng ảnh BEFORE; E4 không tự so với chính nó;
  CI sẽ chạy cổng pixel sau các bài test/render/build.
- **Chờ** CI và ảnh trên SHA mới: chưa thể gọi DONE / READY sau sửa mã.

## Ràng buộc và nợ kỹ thuật

- `ObliqueCamera` 0.72, world anchors, collision, save schema, depth sort,
  FilterQuality.None, PR #28 Mission01 không đổi.
- Legacy P26B G1/G2 **FAIL** theo ngưỡng gốc; không xóa lịch sử thất bại.
- FPS/GPU/thermal trên máy thật: **NOT TESTED**. CI software render không đại diện
  GPU/thời gian thực của Android. UI P27 và huấn luyện LoRA là task riêng, không thuộc PR #33.
