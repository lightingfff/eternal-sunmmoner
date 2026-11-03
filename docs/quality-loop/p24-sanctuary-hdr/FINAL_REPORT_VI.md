# BÁO CÁO CUỐI (Tiếng Việt) — `p24-sanctuary-hdr`

**Ngày**: 2026-11-03 · **Baseline**: `12c6d2f59f022310ba057f5f02c70451737c4e95` (`main`, đã merge PR #29 P23 + bộ skill) · **Nhánh**: `arena/3d2dcfc6-eternal-sunmmoner` · **PR**: [#30](https://github.com/lightingfff/eternal-sunmmoner/pull/30) — `OPEN`, `mergedAt=null`, **không merge**.

> ## KẾT LUẬN: `BLOCKED` — KHÔNG TỰ NHẬN PASS
> CI xanh, nhưng ảnh engine ở zoom chơi **0.88 / 1.00 vẫn còn phẳng**. Cổng nghiệm thu B3 đã chốt trước khi viết code đòi `p90−p10` tăng ≥3 trên ≥2/3 khung; thực tế **1/3**. Tôi **không** đổi cổng, **không** gọi đây là PASS, **không** dùng CI-green để hợp thức hoá. Đã dùng hết 3 vòng sửa theo đúng ngân sách.

---

## 1. Kích hoạt V2 & năng lực thật (không bịa)

Đã đọc `AGENTS.md`, router `eternal-summoner-skill-router`, **toàn bộ** `eternal-arena-quality-loop` (kể cả phụ lục V1) và các skill vai trò: director, environment-architect, technical-artist, visual-QA, android-performance, integration-reviewer. Tham khảo thêm 3 tài liệu Scenario có path thật (chỉ nguyên tắc, không gọi dịch vụ trả phí).

**Không có tool native spawn/delegate subagent** trong runtime này → 6 vai trò là **checkpoint tuần tự trong MỘT Arena agent**, không phải 6 agent/model chạy song song. Không dùng `.codex/agents/*.toml`, không gọi Scenario MCP/API trả phí, không tải model/LoRA, không "huấn luyện" gì. Không có số điểm QA của provider nào.

## 2. Audit phát hiện gì (trước khi sửa)

Soi code + **mở trực tiếp** ảnh engine P23 (`sanctuary_overview_z036`, `service_cluster_z088/z100`, `armory_full_z148`, `tower_gate_whole_z148`):

- Mái / mặt tiền / mặt bên **đồng giá trị** → nhà đọc như sticker; ô cửa sổ vàng sáng nhưng **không chiếu** xuống đá; Tower Gate masonry tương phản thấp; quảng trường không có tiêu điểm.
- **Gap harness quan trọng**: `LivingWorldRenderer.drawLocalLights` chỉ chạy ở `SanctuaryLobbyScreen:653`, **không** chạy trong `MapForgeP23RenderTest` → 24 ảnh evidence P23 **không hề có local lights của lobby**. `SANCTUARY_AGSL` chỉ là chuỗi docs, không phải RuntimeShader.active; nhiều hàm "premium" định nghĩa nhưng không được gọi.
- Số đo baseline (luminance `0.2126R+0.7152G+0.0722B`): `service_cluster_z088` mean 62.94 / p10 35.2 / p90 88.2 / spread 53.1; `tower_gate_whole_z148` 58.71 / 30.2 / 81.4 / 51.2; `sanctuary_overview_z036` 70.59 / 39.4 / 97.0 / 57.6.

## 3. Đã triển khai (một slice nhỏ, có kiểm soát)

| File | Nội dung |
| --- | --- |
| `HdrLightResponse.kt` (mới, 434 dòng) | 7 radial mask 48px cache `by lazy`; `MAX_STAMP_ALPHA=.30`, halo `.22`, lõi `.46` — **có ngưỡng**; cull trước mọi draw; 3 tier `OFF/REDUCED/BALANCED`; `exposureBalance()` chỉ scale alpha **cục bộ** theo đêm/mưa; `toneMapAlpha` nén highlight mềm. |
| `PixelSpriteRenderer.kt` | 2 call site trong `drawProp` (trước sprite = vũng sáng trên mặt phẳng `.72`; sau sprite = lõi/quầng/grade) → **một pipeline duy nhất** chạy cả lobby lẫn render harness. |
| `LivingWorldRenderer.kt` | Bỏ stamp trùng (tránh double light), chỉ giữ vệt phản xạ ướt khi mưa. |
| `SanctuaryWorldRenderer.kt` | 1 tiêu điểm quảng trường có điều khiển + vũng lạnh Tower Gate lan xuống bậc thang + 2 điểm ấm cổng nam. |
| `HdrLightingRenderTest.kt` (mới) | 8 khung engine OFF/REDUCED/BALANCED × ngày/đêm/mưa × 0.36/0.88/1.00/1.48, **có post pass thật của lobby** (`drawWorldGrade`/`drawLocalLights`/`drawWeather`) → vá gap harness; test cache mask & exposure bị chặn. |
| `lyra-render-evidence.yml` | Thêm path + 1 step render; **snapshot 24 khung P23 thành `p24/engine-render-before/` TRƯỚC KHI** ghi đè `engine-render/` → before/after cùng camera thật. |
| `docs/quality-loop/p24-sanctuary-hdr/*` | `WORK_ORDER.md` (chốt rubric TRƯỚC code), `QA_MATRIX.md`, `BLOCKERS.md`, `ROUND_1..4.md`, `measure_luminance.py`, báo cáo này. |

**Hiệu ứng từ chối vì chi phí / chất lượng** (ghi rõ theo yêu cầu): fullscreen bloom, god-ray cho mọi nguồn, RuntimeShader/AGSL, blur mềm đè pixel-art, tăng brightness toàn cục, đĩa sáng đặc lấp hốc rift, dynamic light cho mọi prop nhỏ, vignette toàn màn hình. Thay vào đó: mask cache + `Path` silhouette cache (vốn có sẵn cho bóng đổ), 2 `drawPath`/nhà, chỉ BALANCED, cull offscreen.

**Không thay đổi**: `OBLIQUE_Y_SCALE=0.72f`, `SanctuaryLight`, `WorldObstacle`/anchors/footprints/collider/door trigger/save, depth sort, canopy occlusion, `FilterQuality.None` cho sprite, mọi PNG asset (`premium_slice`, `sanctuary_p03`), gameplay/combat/AI, `.codex`, `AGENTS.md`, và **29 file PR #28** (`comm -12` = rỗng, đã kiểm tra live bằng `gh pr diff 28 --name-only`).

## 4. Vòng lặp & số đo thật (4 chu trình = 1 triển khai + **3 vòng sửa**)

| Vòng | SHA | mean (3 khung gate) | spread (3 khung gate) | Kết luận |
| --- | --- | --- | --- | --- |
| R1 | `09f33e1` | +0.09 / +0.73 / +1.38 % | +0.0 / −1.0 / −1.9 | FAIL: "glow nhưng vẫn phẳng" — alpha bị suy hao 2 lần, quầng vô hướng, contact AO nằm dưới sprite |
| R2 | `ddccd77` | +0.10 / +4.51 / **+10.16** % | +0.0 / +1.7 / **+10.1** | FAIL: volume đã xuất hiện nhưng **washed** (vượt ngưỡng 8%) |
| R3 | `546e6a5` | +0.09 / −0.03 / +1.87 % | +0.0 / −0.4 / +3.7 | FAIL: mean trung hoà nhưng cắt quá nhiều highlight → mất độ giãn |
| R4 | `c0e2c03` | **+0.01 / −0.29 / +1.09 %** | **+0.0 / +0.0 / +5.4** | **BLOCKED**: no-flood 3/3 PASS, contrast-gain **1/3 FAIL** |

`|Δmean|` lớn nhất trên toàn 24 khung ở R4 = **3.60%** (`summoning_hall_whole_z148`) → không hề tăng brightness toàn cục. `dark<20` không tăng đáng kể → vùng tối không bị crush. Một số khung cận cảnh đạt +2.7…+3.1 spread (`armory_door_trigger_actor_z148` +3.1, `armory_full_z148` +2.8, `cottages_nw_z148` +2.9, `synthesis_cottage_z148` +2.8, `south_gate_training_z088` +2.7). **Ngược chiều, không giấu**: `summoning_hall_whole_z148` **−3.8** spread / −3.60% mean, `summoning_hall_zone_actor_z100` −1.4, `service_cluster_z100` −0.7.

**Soi ảnh trực tiếp (không chỉ contact sheet)**:
- `tower_gate_whole_z148.png` (R4): tháp trái đón sáng, tháp phải sâu, móng tối, hốc rift **vẫn đen**, vũng cyan dưới chân, tinh thể hai bên → **có volume thật, không wash, pixel vẫn sắc**. Đây là thắng lợi rõ nhất.
- `service_cluster_z100.png` (R4): đã có quầng ấm quanh nhóm cửa sổ và vũng đèn, nhưng **mái/mặt tiền/mặt bên vẫn khá đồng giá trị** → vẫn là "có glow nhưng vẫn phẳng" ở zoom chơi.
- `armory_full_z148.png`, `hdr_balanced_night_z088.png`: cải thiện vừa phải; khung đêm/mưa giờ có grade lobby thật (79.91 → 63.47 → 61.43 mean).

## 5. CI / test thật (không fake)

| Run | SHA | Kết quả |
| --- | --- | --- |
| [41586525855](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41586525855) + [41586525853](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41586525853) | `09f33e1` | success / success |
| [41587811048](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41587811048) + [41587811026](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41587811026) | `ddccd77` | success / success |
| [41588829278](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41588829278) + [41588829263](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41588829263) | `546e6a5` | success / success |
| [41589948067](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41589948067) | `c0e2c03` | **success ở attempt 2**. Attempt 1 `failure` **chỉ** ở step `Download and verify the actual uploaded artifact ZIP`; `Build APK`, `Verify signed APK`, `Upload APK` đều success. Đã `POST .../rerun-failed-jobs` → step 13 success. Lỗi tải artifact hạ tầng, không phải lỗi biên dịch. |
| [41589948058](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41589948058) | `c0e2c03` | **success**, 15/15 step: render Lyra/ground/architecture, P20/P21/P22/P23 contract, **P24 HDR matrix**, Sanctuary environment/performance regressions, terrain, publish, upload. |

Bằng chứng ảnh do bot commit `dcdc870` (85 PNG, **không đổi source**). 2 run trên SHA bot (`41590628143`, `41590628175`) ở trạng thái `action_required` (commit `github-actions[bot]` chờ owner approve) nên `gh pr checks 30` báo "no checks reported" ở head SHA — đã ghi nhận, không che giấu; CI được gắn với **đúng SHA code** `c0e2c03`.

`LOCAL BUILD NOT RUN`: sandbox không có `java`, `gradle`, Android SDK, `/usr/lib/jvm`. Local chỉ chạy `git diff --check`, parse YAML, và Pillow trong venv để đo PNG. `EMULATOR`, `PHYSICAL DEVICE`, **FPS / GPU / nhiệt = NOT TESTED** — không có thiết bị, không phát minh số FPS. `ScenePerformanceProbe` là CPU submission/frame pacing, không phải GPU completion.

## 6. QA matrix & blocker

| Gate | Kết quả |
| --- | --- |
| B1 CI xanh | PASS |
| B2 24 + 8 khung cùng camera `0.36/0.88/1.00/1.48` | PASS (24 BEFORE / 7 356 408 B; `camera_props.json` diff rỗng) |
| B3a không flood sáng | **PASS 3/3** |
| B3b tương phản tăng ≥3 trên ≥2/3 | **FAIL 1/3** |
| B4 hợp đồng `.72` / anchors / save / `FilterQuality.None` / không đụng PR #28 | PASS |
| B5 OFF ⊂ REDUCED ⊂ BALANCED, không fullscreen effect | PASS |
| B6 không còn "glow nhưng vẫn phẳng" ở 0.88/1.00 | **FAIL** |

Sáu chiều mềm: visual coherence **2**, technical correctness 4, player clarity 3, material differentiation 3, polish 3, efficiency 4 → **19/24 (<20), có chiều <3 → FAIL**. Chi tiết: `QA_MATRIX.md`, `BLOCKERS.md`.

## 7. Vì sao dừng ở BLOCKED & hướng xử lý tiếp (không sửa vòng thứ 4)

1. Grade trên silhouette là **theo không gian**, không **theo độ sáng**: nó sáng hoá cả pixel tối ở góc NW và tối hoá cả pixel sáng ở góc SE. Muốn giãn histogram (`p90↑`, `p10↓`) mà mean đứng yên cần tone curve **chọn theo luminance** (ví dụ `BlendMode.Overlay/SoftLight` giới hạn trong silhouette, hoặc 2 mask ngưỡng sáng/tối). Đó là thay đổi bản chất pipeline, vượt slice nhỏ và ngân sách 3 vòng.
2. Asset P23 tự thân vẽ mái/tường/mặt hồi cùng dải giá trị; ánh sáng đè lên **không tạo được cạnh khối thật**. Cần sửa sprite/atlas (dải sáng cạnh mái, soffit tối, mặt hồi riêng) — nằm trong `MUST NOT change` đã chốt, phải có ticket riêng và phối hợp owner.
3. `summoning_hall_whole_z148 −3.8` cho thấy một hệ số chung cho mọi công trình là chưa đủ: cần bảng material/face riêng cho mái slate vs mái gỗ nâu thay vì `zoomFactor` đơn nhất.

**Không** kéo dài mission bằng timer giả / HP sponge, **không** tự nhận "55s simulation = 5–10 phút chơi", **không** tự nhận FPS/độ mượt máy thật.

---

## 8. Hướng dẫn owner kiểm tra (PR giữ OPEN, KHÔNG merge)

1. Mở **https://github.com/lightingfff/eternal-sunmmoner/pull/30** → tab **Files changed** (12 file source/docs, 1 013 insertions / 75 deletions; 85 PNG evidence do bot commit).
2. Tab **Checks**: xem run `41589948067` (attempt 2, success) và `41589948058` (success). Lưu ý 2 run `action_required` trên SHA bot `dcdc870` cần owner approve nếu muốn check hiển thị ở head.
3. Tải artifact **`engine-render-evidence`** của run `41589948058`, hoặc mở thẳng trong repo:
   - BEFORE: `docs/visual-evidence/p24/engine-render-before/` (24 PNG P23).
   - AFTER: `docs/visual-evidence/p23/engine-render/` (24 PNG + 8 `hdr_*`).
   - Soi **trực tiếp từng PNG**, đặc biệt `service_cluster_z088.png`, `service_cluster_z100.png`, `sanctuary_overview_z036.png`, `armory_full_z148.png`, `tower_gate_whole_z148.png`, `hdr_off_z088.png` ↔ `hdr_balanced_day_z088.png` ↔ `hdr_balanced_night_rain_z088.png`.
4. Chạy lại số đo nếu muốn: `python3 -m venv /tmp/qa-venv && /tmp/qa-venv/bin/pip install pillow` rồi `/tmp/qa-venv/bin/python docs/quality-loop/p24-sanctuary-hdr/measure_luminance.py docs/visual-evidence/p24/engine-render-before docs/visual-evidence/p23/engine-render`.
5. Đọc `ROUND_1.md` → `ROUND_4.md` để thấy đúng 3 vòng sửa và lý do từng lần; `FINAL_REPORT_VI.md` là báo cáo này.
6. **Quyết định của owner**: (a) merge như một cải thiện lighting có kiểm soát dù chưa đạt premium, **hoặc** (b) giữ OPEN và mở ticket riêng cho sprite/face-atlas + tone curve chọn theo luminance. Tôi **không** tự merge, **không** tự chuyển Draft→PASS, và **không** khẳng định đã xong phần "Sanctuary after P23".

> PR [#30](https://github.com/lightingfff/eternal-sunmmoner/pull/30) vẫn **OPEN / chưa merge**. Owner vui lòng tự kiểm tra ảnh và CI trước khi quyết định.
