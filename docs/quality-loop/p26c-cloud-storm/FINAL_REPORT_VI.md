# P26C — báo cáo tích hợp PR #33 (đang QA, chưa merge)

**Nhánh** `arena/06fa6c23-eternal-sunmmoner` · **P26B baseline** `f0dc0de` ·
**PR** [#33](https://github.com/lightingfff/eternal-sunmmoner/pull/33) OPEN, không tự merge.
**Trạng thái trước Round 3**: Round 1 CI xanh nhưng visual FAIL; Round 2 CI xanh,
9/9 cổng tự động PASS trên SHA `8b26848`, song hình khối mây vẫn giống các oval
chồng nhau (**C2 visual 2/4**). Đang tinh chỉnh silhouette cuối; chưa gọi DONE.

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

## Round 2 đã đo — Round 3 đang kiểm tra

- [Render CI 41769520424](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41769520424),
  [APK CI 41769520387](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41769520387)
  **success** trên SHA `8b26848`; ảnh mới ở commit bot `b6ece51`.
- C1 changed-sky ratio CLEAR z0.88: **14.116% sáng / 12.771% trưa** (min 5%).
  C4 STORM−RAIN **−10.56** (max −2). E1–E5 PASS theo phạm vi cặp matched.
  `tools/p26c_measure_clouds.py` so đúng BEFORE; **9/9 cổng tự động PASS**.
- [ROUND_2.md](ROUND_2.md) và ảnh engine cho thấy cloud far/middle/near đều
  giống các oval chồng nhau. C2 **visual 2/4** chưa đạt ngưỡng 3/4.
- Round 3 giữ 8 neo nhưng làm silhouette far/mid/near khác nhau bằng các Path
  **tạo một lần**; vẫn phải chạy CI và kiểm ảnh thật. Không hạ ngưỡng C1/C4;
  nếu thất bại ở vòng cuối thì báo BLOCKED, không tô hồng.
- Việc kiểm CI trên **SHA cuối cùng** (sau bot commit ảnh) vẫn chờ manual dispatch;
  không dùng kết quả trên SHA nguồn trước đó thay cho SHA cuối.

## Ràng buộc và nợ kỹ thuật

- `ObliqueCamera` 0.72, world anchors, collision, save schema, depth sort,
  FilterQuality.None, PR #28 Mission01 không đổi.
- Legacy P26B G1/G2 **FAIL** theo ngưỡng gốc; không xóa lịch sử thất bại.
- FPS/GPU/thermal trên máy thật: **NOT TESTED**. CI software render không đại diện
  GPU/thời gian thực của Android. UI P27 và huấn luyện LoRA là task riêng, không thuộc PR #33.
