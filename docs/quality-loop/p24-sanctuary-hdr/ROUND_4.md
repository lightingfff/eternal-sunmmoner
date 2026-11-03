# ROUND 4 — CORRECTION #3 (VÒNG CUỐI) → KẾT LUẬN: `BLOCKED`, KHÔNG TỰ NHẬN PASS

- **SHA code cuối**: `c0e2c034f9d97364179f9c9b6a6ff59cecd3a89b` · các commit sau đó trên nhánh chỉ là **PNG evidence** (bot `dcdc870`) và **báo cáo docs**, không đổi source.
- **CI thật trên `c0e2c03`**:
  - `https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41589948067` — `Build Eternal Summoner APK`: **success** ở attempt 2. Attempt 1 `failure` **chỉ** ở step `Download and verify the actual uploaded artifact ZIP`; các step `Build APK`, `Verify signed APK`, `Upload APK` đều `success`. Rerun job lỗi (`POST .../rerun-failed-jobs`) → step 13 `success`. Đây là lỗi tải artifact hạ tầng, không phải lỗi biên dịch; đã ghi nhận trung thực, không giấu.
  - `https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41589948058` — `Lyra engine render evidence`: **success**, 15/15 step render/verify/publish/upload `success` (kể cả `Render P24 HDR light-response matrix`, `Verify P23 architecture and props contract`, `Verify Sanctuary environment art, manifest, layout, and performance regressions`).
  - 2 run trên bot SHA `dcdc870` (`41590628143`, `41590628175`) có trạng thái `action_required` (commit của `github-actions[bot]` chờ owner approve); `gh pr checks 30` vì vậy báo "no checks reported" ở head SHA. Bằng chứng CI được gắn với **đúng SHA code** `c0e2c03`.
- **PR**: #30 `https://github.com/lightingfff/eternal-sunmmoner/pull/30` — `OPEN`, `mergedAt=null`, **không merge**.
- Đã soi trực tiếp PNG engine cuối: `service_cluster_z100.png`, `tower_gate_whole_z148.png`, `armory_full_z148.png` (và các vòng trước: `hdr_balanced_night_z088.png`, `sanctuary_overview_z036.png`).

## Số đo cuối (`measure_luminance.py`, BEFORE = 24 PNG P23 / 7 356 408 B)

| khung gate | mean B→A | Δmean% | p90−p10 B→A | Δspread | verdict |
| --- | --- | --- | --- | --- | --- |
| `sanctuary_overview_z036` | 70.59 → 70.60 | +0.01% PASS | 57.6 → 57.6 | +0.0 FAIL | — |
| `service_cluster_z088` | 62.94 → 62.76 | −0.29% PASS | 53.1 → 53.1 | +0.0 FAIL | — |
| `tower_gate_whole_z148` | 58.71 → 59.35 | +1.09% PASS | 51.2 → 56.6 | **+5.4 PASS** | — |
| **Tổng gate B3** | **no-flood 3/3 PASS** | | **contrast-gain 1/3 FAIL** | | **FAIL** |

Khung khác (thật, không chọn lọc): `armory_door_trigger_actor_z148` +3.1 spread / −0.22% mean, `armory_full_z148` +2.8 / −0.16%, `cottages_nw_z148` +2.9 / −0.52%, `south_gate_training_z088` +2.7 / +0.29%, `synthesis_cottage_z148` +2.8 / −0.54%. **Ngược chiều**: `summoning_hall_whole_z148` **−3.8** spread / −3.60% mean, `summoning_hall_zone_actor_z100` −1.4, `service_cluster_z100` −0.7, `hdr` day frame spread giảm nhẹ.

`|Δmean|` lớn nhất toàn 24 khung = **3.60%** → đã chữa dứt lỗi washed của ROUND 2 (+10.16%). `dark<20` không tăng đáng kể (vùng tối không bị crush), `hi>150` giảm nhẹ — chứng tỏ **không hề tăng brightness toàn cục**, đúng yêu cầu "giàu hơn chứ không chỉ sáng hơn" về mặt exposure.

## Tier / day-night / weather (`REAL ENGINE ROBOLECTRIC RENDER`)

| khung | mean | p90−p10 | bytes |
| --- | --- | --- | --- |
| `hdr_off_z088` | 79.96 | 48.3 | 406 741 |
| `hdr_reduced_z088` | 80.22 | 48.3 | 443 296 |
| `hdr_balanced_day_z088` | 79.91 | 47.4 | 669 462 |
| `hdr_balanced_night_z088` | 63.47 | 43.6 | 654 237 |
| `hdr_balanced_night_rain_z088` | 61.43 | 37.9 | 642 682 |
| `hdr_balanced_night_tower_gate_z100` | 60.78 | 51.3 | 541 486 |
| `hdr_balanced_night_tower_gate_z148` | 57.05 | 45.6 | 424 313 |
| `hdr_balanced_night_overview_z036` | 58.87 | 40.4 | 649 584 |

OFF ≈ REDUCED (cùng bỏ grade BALANCED-only), BALANCED khác → B5 PASS. Đêm tối hơn ngày rõ rệt (79.91 → 63.47), mưa tối/đục hơn (61.43) → day/night/weather compatibility có bằng chứng. Các khung này **có** `drawWorldGrade`/`drawLocalLights`/`drawWeather` thật của `SanctuaryLobbyScreen` → đã vá gap harness ghi nhận lúc audit (P23 evidence cũ không hề có local lights của lobby).

## Critique cuối (KHÔNG PASS)

**Đạt được**: pipeline ánh sáng gộp làm một (lobby + harness), mask 48px cache + silhouette cache + cull + 3 tier, không fullscreen bloom/blur/RuntimeShader, không đổi `.72`/anchors/save/gameplay, không chạm 29 file PR #28 (`comm -12` rỗng), giữ `FilterQuality.None` cho sprite, hốc rift vẫn tối, pixel vẫn sắc. `tower_gate_whole_z148` soi ảnh: tháp trái đón sáng, tháp phải sâu, móng tối, vũng cyan dưới chân — **có volume thật, +5.4 spread, không wash**.

**Chưa đạt / BLOCKED**: soi `service_cluster_z100.png` ở zoom chơi 1.00 vẫn thấy **mái/mặt tiền/mặt bên khá phẳng**; quầng ấm quanh cửa sổ và vũng đèn có, nhưng chưa đủ để công trình đọc như khối đá 2.5D cao cấp. Đây vẫn là dạng **"có glow nhưng vẫn phẳng"** mà người dùng yêu cầu loại bỏ. `sanctuary_overview_z036` Δspread 0.0, `service_cluster_z088` 0.0, `summoning_hall_whole_z148` **−3.8** (mặt tiền sáng của Summoning Hall bị kéo tối). Gate B3 đã chốt trước khi triển khai đòi ≥2/3 khung tăng spread ≥3 → thực tế **1/3** → **FAIL**, không tự nhận PASS dù CI xanh.

## Root cause còn lại (để owner/quầng sau xử lý, KHÔNG sửa vòng thứ 4)

1. Grade trên silhouette là **theo không gian**, không **theo độ sáng**: nó sáng hoá cả pixel tối ở góc NW và tối hoá cả pixel sáng ở góc SE. Muốn giãn histogram (`p90↑`, `p10↓`) mà mean đứng yên cần tone curve **chọn theo luminance** (ví dụ `BlendMode.Overlay`/`SoftLight` giới hạn trong silhouette, hoặc 2 mask ngưỡng sáng/tối) — đây là thay đổi bản chất pipeline, vượt phạm vi slice nhỏ và ngân sách 3 vòng sửa.
2. Asset P23 tự thân đã vẽ mái/tường/mặt bên cùng dải giá trị; ánh sáng đè lên không tạo được cạnh khối (edge/crease) thật. Cần **sửa sprite/atlas** (thêm dải sáng cạnh mái, soffit tối, mặt hồi riêng) — nằm ngoài phạm vi `MUST NOT change` đã chốt (`SanctuaryArtAssets.kt`, PNG `premium_slice`/`sanctuary_p03`), phải có ticket riêng và phối hợp owner.
3. `summoning_hall_whole_z148` bị kéo tối cho thấy một hệ số grade chung cho mọi công trình là chưa đủ: cần bảng material/face riêng cho từng loại mái (đá slate vs gỗ nâu) thay vì `zoomFactor` đơn nhất.

## Ngân sách vòng

ROUND 1 = chu trình triển khai ban đầu; ROUND 2/3/4 = **3 correction rounds** đúng yêu cầu. Đã hết ngân sách → dừng, báo `BLOCKED` kèm số đo, giữ PR #30 OPEN để owner duyệt, không tự merge, không tự PASS.
