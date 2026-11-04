# P26C — QA matrix: mây gameplay và STORM

**Nguồn**: ảnh Robolectric renderer thật 1280×720 của PR #33. BEFORE là 7 ảnh P26B từ
`f0dc0de6b63794082ba5db88244a2cc8c486bcdb` có **cùng camera/giờ/thời tiết/tier**;
`docs/visual-evidence/p26c/before_manifest.json` giữ đường dẫn gốc và SHA-256.
STORM không tồn tại trong P26B, nên KHÔNG có BEFORE cùng trạng thái cho STORM.
Ngưỡng C1/C3/C4/E1–E5 vẫn là `thresholds.json` đã chốt trước vòng đo; không hạ ngưỡng.

| Vòng | SHA renderer | CI | C1 / C2 | C3 | C4 | C5 / C6 | E1/E2 | E3 | E4 | E5 | Kết luận |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Baseline | `f0dc0de` | P26B | Mây không thấy tại service z0.88 | — | Chưa có STORM | — | P26B | — | — | — | Ảnh BEFORE tồn tại |
| Round 1 | `c255395` (ảnh `c466d65`) | [Render success](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41686884922), [APK success](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41686885163) | **FAIL / FAIL** | PASS | **FAIL** | PASS / culling đã đọc code | PASS/PASS | **FAIL** | PASS trên 7 cặp | PASS | **6/9 cổng tự động**, visual FAIL |
| Round 2 | `8b26848` (ảnh `b6ece51`) | [Render success](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41769520424), [APK success](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41769520387) | PASS 14.12/12.77% / **WARN oval 2/4** | PASS −7.95 | PASS −10.56 vs rain | PASS / code cull | PASS/PASS | PASS | PASS trên 7 cặp | PASS | **9/9 auto, visual WARN** |
| Round 3 (silhouette cuối) | `d89c73a` (ảnh `6d67edc`) | [Render success](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41770644293), [APK success](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41770644177) | PASS 10.17/9.57% / **3/4 tự chấm** | PASS −8.68 | PASS −8.94 vs rain | PASS / code cull ±300px | PASS/PASS | PASS | PASS 7 cặp | PASS | **9/9 auto + C2 3/4, owner review** |

## Root cause Round 1 (đo sau khi toàn bộ CI xanh)

- **C1 FAIL**: phép đo cũ `|L−median(skyBand)| > 12` cho kết quả giả ~69%, vì đếm cả
  gradient/đường chéo/tòa nhà. So với BEFORE P26B cùng state bằng phép đo mới
  `|L_after−L_before| > 12` trên **toàn sky band 25%**: morning CLEAR **0.00%**,
  noon CLEAR **0.00%**, ngưỡng giữ nguyên **5%**. Pixel thay đổi chỉ trong hộp
  x≈371…461, y≈35…83 (một mẩu mây bị tường che); service camera không thấy khối mây.
- **C2 FAIL** (quan sát trực tiếp PNG): ba lớp có trong code nhưng không đọc ra ở service
  camera; zoom1.48 service bị công trình che hết bầu trời. Phải kiểm tra thêm camera
  gate z1.48 **có trời thật** thay vì gán PASS cho bookmark bị che.
- **C4 FAIL**: sky-band noon z0.88 STORM **84.68**, RAIN **82.06**,
  `storm−rain=+2.62`, trong khi yêu cầu `≤−2`. STORM tối hơn CLEAR nhưng chưa tối
  hơn RAIN. **E3 FAIL** vì ngưỡng `storm < rain` đã ghi trong thresholds nhưng mã đo cũ
  bỏ qua; đã sửa phép đo, không nới ngưỡng.
- **E4 PASS có điều kiện**: dùng **7 cặp BEFORE/AFTER thật**, ROI world y≥42% như
  công cụ P26; `|Δmean|` lớn nhất <0.01% ở vòng đầu, ngưỡng <8%. Không dùng
  self-comparison để tuyên bố STORM PASS; STORM không có baseline P26B.
- **C3/C5/E1/E2/E5 PASS** chỉ phản ánh cổng tương ứng; CI xanh không đồng nghĩa ảnh đẹp.
- Legacy G1/G2 của P26B vẫn **FAIL** theo metric gốc, G1b/G2b từng PASS theo metric
  đã hiệu chỉnh; PR này không đổi/giấu ngưỡng legacy. Round 3 so đúng ROI void core
  trên P26B→P26C: noon 43.60→43.60, night CLEAR 39.15→39.15,
  night RAIN 42.75→42.62. STORM night core 44.54 >40 nếu áp G1 tuyệt đối,
  không có STORM P26B để nói có/không hồi quy. Không tuyên bố gate G1/G2 PASS.

## Round 3 — vòng cuối đã đối chiếu bằng PNG engine

1. Round 2 đưa neo vào **khe trời thật** của service và làm STORM tối riêng sky
   bands: [chi tiết/số đo](ROUND_2.md). 9/9 auto PASS nhưng các oval chồng nhau
   còn giống bong bóng; C2 chỉ 2/4 theo kiểm ảnh thật.
2. Round 3 thay hình ba lớp (far bank, middle stepped cumulus, near wisp), 4 Path
   khai báo cấp object; giữ 8 neo và ngưỡng C1/C4. CI + [ảnh engine](../../visual-evidence/p26c/engine-render/)
   đã tồn tại; [ROUND_3.md](ROUND_3.md) và `roi_measurements.{json,txt}` ghi
   9/9 auto PASS, C2 **3/4 theo tự chấm**. Far bank còn chút oval; không gán
   mức mỹ thuật hoàn hảo hoặc lấy 9/9 thay bằng chứng thị giác.
3. G1/G2 legacy không đổi thành PASS; night STORM void core 44.54 >40 nếu áp
   ngưỡng tuyệt đối. Không có BEFORE STORM. Gate art/halo thuộc review riêng;
   không lén sửa gate hoặc tuyên bố full-game DONE.
4. PR #33 giữ OPEN cho owner; không thêm vòng tinh chỉnh thứ 4. Verify cuối trên
   đúng SHA báo cáo/bot bằng workflow dispatch, không đẩy bot evidence mới.

**Hardware**: FPS/GPU/thermal trên điện thoại = NOT TESTED. Không suy từ Robolectric.
