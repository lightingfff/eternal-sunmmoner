# P25 — OPEN ITEMS / BOUNDED SUB-TICKETS (không phải verdict BLOCKED cho P25)

P25 đạt các hard gate A–F của chính ticket (xem `QA_MATRIX.md`, `ROUND_3.md`). Dưới đây là các hạng mục **chưa giải quyết**, ghi trung thực để owner quyết định; đây **không** phải lệnh tự PASS, cũng không phải lý do nới rubric. P24 PR #30 đã merge theo lệnh owner nhưng trạng thái chất lượng P24 vẫn mở — không được gọi là đã xong.

## 1. Cổng whole-frame P24 `Δ(p90−p10) ≥ +3 trên ≥2/3` — **vẫn FAIL 1/3**

| khung | Δspread P25 | ngưỡng |
| --- | --- | --- |
| `sanctuary_overview_z036` | +0.0 | +3 |
| `service_cluster_z088` | +2.9 | +3 |
| `tower_gate_whole_z148` | +3.7 | +3 ✅ |

P24 gốc: 1/3. P25 3 pilot: 0/3. P25 7 facility: 1/3. **Không relabel, không đổi ngưỡng, không tăng brightness toàn cục.** Lý do cấu trúc: `p90−p10` toàn khung pha nền cỏ/đường/tường thành/cây/hốc tối; công trình chỉ chiếm ~20–30% khung. P25 giải quyết bằng **ROI theo mặt phẳng** (38/38 PASS) và báo cáo hai hệ số độc lập theo đúng yêu cầu ticket.

- **Sub-ticket P26-a** (nếu owner vẫn muốn cổng này): cần tone map **luminance-selective** và/hoặc tách nền khỏi phép đo — nhưng P24 đã dùng hết 3 vòng sửa và ghi root cause là sprite phẳng. Không nằm trong scope P25, không tự mở.

## 2. Background `sanctuary_p03` houses / cottages vẫn phẳng

7 `premium_slice` facility đã sửa; 6 background building (`townhouse_a/b`, `cottage_brown/slate`, `fortress_tower`, `gate_tower`) + props nằm ngoài `WORK_ORDER.md` → vẫn đọc phẳng ở `service_cluster_z100.png`, `synthesis_cottage_z148.png`, `cottages_nw_z148.png`. Ở z0.36 chúng nhỏ nhưng vẫn kéo coherence toàn khu xuống (visual coherence chấm 3/4, không 4/4).

- **Sub-ticket P26-b**: áp cùng grammar + ROI cho 6 `sanctuary_p03` asset, với baseline/ROI/threshold/CI riêng. Cần author ROI mới vì canvas và geometry khác hẳn `house_mass`.

## 3. Band soffit Summoning Hall hơi nặng

`soffit_eave` ROI y80..84 (4px sprite) nhân `2s ≈ 3.2` ở z1.48 → vạch tối ~13px dưới cornice trong `summoning_hall_whole_z148.png`. Đúng chức năng (underside tối, front−soffit −5.55→47.92) nhưng hơi đậm. Không sửa ở ROUND 3 để tránh một cycle CI nữa cho lỗi thẩm mỹ nhỏ.

- **Sub-ticket P26-c**: thu hẹp ROI còn 2px (y81..83) hoặc nâng target 24→32, sinh lại 1 PNG + 1 manifest entry, chạy lại ROI + 1 CI render.

## 4. Engine-projected ROI của `armory` / `synthesis_lab` còn nhiễu

Authored PNG đạt `front−side` 19.72 / 13.90, nhưng ROI chiếu lên `armory_full_z148.png` (+4.44) và `synthesis_cottage_z148.png` (−2.53) bị kéo bởi sprite tiền cảnh, actor/canopy, hốc loggia/workshop được bảo vệ và grade nền. Ảnh soi trực tiếp vẫn đọc khối rõ.

- **Sub-ticket P26-d**: nếu muốn dùng engine ROI làm gate cứng, cần mask occlusion theo depth buffer / per-prop z-order thay vì chiếu đa giác thuần tuý. Hiện engine ROI chỉ là **corroboration có chú thích**, gate cứng là authored PNG.

## 5. Chưa có thiết bị thật

`LOCAL BUILD NOT RUN` (thiếu JDK/Gradle/Android SDK trong sandbox; build lấy từ GitHub Actions). `ScenePerformanceProbe` / `ScenePerformanceTest` chỉ CPU submission + frame pacing. **FPS, GPU time, thermal, EMULATOR, PHYSICAL DEVICE = NOT TESTED.** Không bịa số đo hiệu năng điện thoại.

- **Sub-ticket P26-e**: chạy APK trên thiết bị thật với cùng 4 zoom + night/rain, đo frame pacing/GPU/thermal, đối chiếu `ScenePerformanceProbe`.

## 6. Phân biệt `action_required` với build/test failure

Commit bot PNG-only `0ace057` / `98732ba` có run `action_required` (chờ phê duyệt workflow cho `eternal-render-bot`). CI xanh thuộc **SHA code** `9a204e2` (`41620768746` / `41620768647`) và `ba1056c` (`41622749053` / `41622749135`). Không được gán success của SHA code cho head SHA bot, và không được gọi `action_required` là lỗi compiler/test.

## STOP conditions — đều KHÔNG kích hoạt

generator `--check` lỗi alpha/size/door ❌ | manifest mismatch ❌ | CI build/contract FAIL ❌ | `|Δmean| ≥ 8%` ❌ | hốc rift phát sáng ❌ | đụng file PR #28 ❌ | hết 3 vòng mà B5/B6 chưa đạt ❌. Vì vậy không rollback, không hạ rubric, không tự `BLOCKED` toàn ticket, không mở vòng 4.
