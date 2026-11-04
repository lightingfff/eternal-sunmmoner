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
| Round 2 | chờ SHA mới | chờ CI | CHỜ ĐO | CHỜ ĐO | CHỜ ĐO | CHỜ ĐO | CHỜ ĐO | CHỜ ĐO | CHỜ ĐO | CHỜ ĐO | KHÔNG CHẤM TRƯỚC |

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
  đã hiệu chỉnh; PR này không đổi/giấu ngưỡng legacy. Chưa chạy lại ROI gate P26B
  trên ảnh vòng 2; không tuyên bố chúng PASS.

## Targeted Round 2 (đợi ảnh mới rồi mới chấm)

1. Di chuyển các neo mây vào **khe trời thật** ở trái/trên bức tường service;
   tăng độ tương phản thể tích và giữ ba lớp, 8 neo; không giảm VFX.
2. Điều chỉnh `skyBrightness` **chỉ với STORM**, không phủ tối toàn cảnh; phân biệt
   cache STORM/RAIN dù lượng mưa khi chuyển tiếp cùng bucket.
3. Tool C1 so pixel thay đổi với nguồn BEFORE có kiểm SHA/camera; E4 so trực tiếp,
   gate không có baseline thì ghi NOT AVAILABLE. Ghi lại số đo/ảnh/CI trên SHA mới.

**Hardware**: FPS/GPU/thermal trên điện thoại = NOT TESTED. Không suy từ Robolectric.
