# P26C — báo cáo tích hợp PR #33 (vòng QA 3/3 đã đo, chờ owner review)

**Nhánh** `arena/06fa6c23-eternal-sunmmoner` · **P26B baseline** `f0dc0de` ·
**PR** [#33](https://github.com/lightingfff/eternal-sunmmoner/pull/33) OPEN, không tự merge.
**Kết luận phạm vi P26C**: Round 1 visual FAIL; Round 2 9/9 automated PASS
nhưng C2 visual 2/4; Round 3 trên code `d89c73a` + ảnh bot `6d67edc` **9/9
cổng tự động PASS, C2 3/4 theo tự đánh giá trực tiếp trên PNG 1280×720**.
Chuyển PR cho owner review; **không** coi G1/G2 gate legacy, P27 UI hoặc hiệu năng
thiết bị đã hoàn tất. Không còn vòng tinh chỉnh thứ 4.

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

## Round 2 → Round 3: đối chiếu engine thật theo cùng camera

- Round 2 [render 41769520424](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41769520424)
  + [APK 41769520387](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41769520387)
  success trên SHA `8b26848`, ảnh bot `b6ece51`: C1 **14.116% / 12.771%**,
  C4 storm−rain **−10.56**, 9/9 automated PASS nhưng C2 mây oval **2/4**.
  [ROUND_2.md](ROUND_2.md) ghi rõ lỗi hình thật, không dùng CI để tự nhận đẹp.
- Round 3 [render CI 41770644293](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41770644293)
  + [APK CI 41770644177](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41770644177)
  **SUCCESS trên source `d89c73a`**, qua compile → P26C tests/render → P17–P26
  regressions → assembleDebug → pixel gate. Commit bot `6d67edc` lưu
  [18 ảnh engine 1280×720](../../visual-evidence/p26c/engine-render/) và
  [camera props](../../visual-evidence/p26c/camera_props.json).
- [ROUND_3.md](ROUND_3.md) / [số đo JSON](roi_measurements.json): C1 CLEAR
  **10.170% / 9.573%** so BEFORE P26B; C3 **−8.68**; C4 storm−rain **−8.94**;
  E1–E5 PASS; E4 world max abs **0.005%** trên 7 cặp matched, không có BEFORE
  STORM. **9/9 cổng số PASS**; tự đánh giá C2 **3/4**: far bank, stepped cumulus,
  near wisp nhận dạng riêng. Vẫn còn far bank hơi oval, service sky wedge hẹp;
  gate z1.48 có sky là bookmark hợp lệ, service z1.48 không thấy trời là đối chứng.
- Same camera/timeSec=0 render 2 lần byte-identical; ô trời t0→t4/t8 có
  **19.95% / 32.20%** pixel đổi >8 (bằng chứng motion ảnh, không phải FPS).
- Gate void core BEFORE P26B→AFTER P26C: noon **43.60→43.60**, night CLEAR
  **39.15→39.15**, night RAIN **42.75→42.62**. Night STORM core **44.54**
  >40 nếu áp G1 tuyệt đối; P26B không có STORM để nói hồi quy. G1/G2 legacy
  vẫn FAIL; không tuyên bố gate contract PASS hoặc đổi ROI sau khi biết kết quả.
- **CI trên SHA cuối cùng sau commit tài liệu** sẽ xác minh bằng dispatch riêng;
  các run phía trên là source `d89c73a`, không được gán cho SHA bot/cuối khi
  chưa chạy. Kết quả chính xác/URL run cuối được ghi lúc bàn giao.

## Ràng buộc và nợ kỹ thuật

- `ObliqueCamera` 0.72, world anchors, collision, save schema, depth sort,
  FilterQuality.None, PR #28 Mission01 không đổi.
- Legacy P26B G1/G2 **FAIL** theo ngưỡng gốc; không xóa lịch sử thất bại.
- FPS/GPU/thermal trên máy thật: **NOT TESTED**. CI software render không đại diện
  GPU/thời gian thực của Android. UI P27 và huấn luyện LoRA là task riêng, không thuộc PR #33.
