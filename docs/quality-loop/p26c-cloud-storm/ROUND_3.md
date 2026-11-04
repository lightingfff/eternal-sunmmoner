# P26C ROUND 3 — kết quả vòng silhouette cuối (ảnh engine thật)

- Source renderer: `d89c73a9ef5c9ad2c6bc077ce0cb007730f51539`.
- Evidence bot: `6d67edc9353ba58925f8e364066a8779968b4beb` (18 PNG, `camera_props.json`).
- [Render CI 41770644293](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41770644293): **SUCCESS**, chạy tuần tự compile test → P26C contract/render → P17–P26 regression → assembleDebug → pixel gate. [APK workflow 41770644177](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41770644177): **SUCCESS**; cả hai trên source SHA `d89c73a`, không mạo nhận là chạy trên SHA bot.
- BEFORE: P26B `f0dc0de6b63794082ba5db88244a2cc8c486bcdb`, 7 cặp có cùng camera/hour/weather/tier theo [before_manifest.json](../../visual-evidence/p26c/before_manifest.json); STORM chưa tồn tại ở P26B. AFTER: [18 ảnh engine](../../visual-evidence/p26c/engine-render/) 1280×720, Robolectric native graphics, không phải mockup hay ảnh trên thiết bị.
- [roi_measurements.json](roi_measurements.json) / [txt](roi_measurements.txt) được sinh **sau khi fetch evidence SHA trên** bằng `tools/p26c_measure_clouds.py`; ngưỡng trong `thresholds.json` giữ nguyên.

| Cổng | Ngưỡng đã chốt | Round 3 thực đo | Kết quả |
|---|---|---|---|
| C1 CLEAR morning/noon z0.88 | changed-sky ≥5% so BEFORE | **10.170% / 9.573%** | PASS |
| C2 3 lớp mây nhìn ra hình khác nhau | đánh giá trực tiếp ≥3/4, không chỉ số pixel | far bank oval mỏng, middle bậc cumulus có rim/bụng, near wisp thon; **3/4** theo tự đánh giá thị giác | PASS có giới hạn |
| C3 CLOUDY − CLEAR | sky ≤−3 luma | **−8.68** | PASS |
| C4 STORM − RAIN / CLEAR | ≤−2 / ≤−12 luma | **−8.94 / −27.55** | PASS |
| C5/E5 deterministic | frame t0 lặp byte-identical | **YES**; PNG noon và `_det` cùng hash | PASS |
| C6/P1 source review | cull trước draw, không alloc Brush/Path mới trong cloud loop | 8 neo, guard center ±300px (prefetch margin), 4 Path tĩnh; GPU/FPS chưa đo | PASS code review, chưa profile |
| E1 ngày đêm | ≥8 / ≥6 / ≥8 / ≥18 | **11.86 / 40.15 / 12.59 / 52.73** | PASS |
| E2 trưa sáng hơn đêm | whole delta ≥5; noon sky ≥ morning | **27.18**, noon sky ≥ morning | PASS |
| E3 CLOUDY/RAIN/STORM − CLEAR | ≤−5 / ≤−8 / ≤−12; STORM < RAIN | **−8.68 / −18.61 / −27.55** | PASS |
| E4 no-flood world y≥42% | abs delta <8% cho 7 cặp matched | lớn nhất **0.005%**; STORM không có BEFORE | PASS phạm vi matched |

**9/9 cổng tự động PASS** (C1/C3/C4/C5/E1–E5), C2 **3/4 do người xem tự chấm**, C6/P1 chỉ kiểm mã. Ảnh trực tiếp đã mở tại `p26c_noon_clear_z088.png`, `p26c_noon_storm_z088.png`, `p26c_noon_rain_z088.png`, `p26c_night_clear_z088.png`, `p26c_night_storm_z088.png`, `p26c_gate_cloud_bookmark_z148.png`, `p26c_gate_night_storm_z148.png`, `p26c_overview_noon_z036.png` và bookmark z1.00. Mây middle bây giờ pixel-stepped khác far oval/near wisp; far bank vẫn có đầu oval và sky wedge ở service khá hẹp, **không** gọi đây là hoàn thiện mỹ thuật toàn game. Service zoom1.48 không lộ trời là đối chứng che khuất, gate zoom1.48 mới là camera có trời để kiểm mây.

Trong ô trời x=0..350,y=0..160 cùng camera/giờ/trạng thái, t0→t4/t8 có **19.95% / 32.20%** pixel thay đổi >8 kênh: có chuyển động hình ở ảnh, t0 lặp byte-identical; phép đo ROI này không suy ra FPS/nhịp mượt trên điện thoại. Không thêm full-screen storm overlay. STORM còn khác RAIN ở lighting/sky/world; đèn địa phương, kiến trúc và hốc cổng vẫn đọc được khi xem ảnh.

**Gate/legacy không tô hồng**: so P26B đúng camera/giờ/weather, mean void core noon **43.60→43.60** (Δ0.00), night CLEAR **39.15→39.15** (Δ0.00), night RAIN **42.75→42.62** (Δ−0.14). Không có hồi quy làm sáng void ở ba cặp này. Night STORM z1.48 có void core **44.54**: cao hơn mức 40 nếu áp ngưỡng G1 tuyệt đối, không có STORM BEFORE để gọi là regression/PASS. Original G1/G2 P26B vẫn **FAIL**, không đổi ROI/ngưỡng, không thay gate art ở P26C. Owner cần đánh giá/giải quyết hợp đồng gate riêng, không gộp với 9/9 cổng clouds/lighting.

**Kết luận phạm vi P26C**: new cloud/storm gates đạt, hình mây đủ 3/4 để chuyển PR #33 sang **owner review**, không merge; giới hạn gate legacy và chất lượng thị giác ở trên giữ công khai. Hết ngân sách 3 vòng tinh chỉnh, không nới rubric. FPS/GPU/thermal thiết bị thật **NOT TESTED**; P27 UI/gate cutscene không triển khai trong PR này. Xác minh CI trên SHA cuối sau commit báo cáo bằng workflow dispatch riêng (ghi URL run ở phần bàn giao; không ghi giả kết quả trước khi chạy).
