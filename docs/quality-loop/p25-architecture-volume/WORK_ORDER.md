# WORK_ORDER — `p25-architecture-volume`

- **Ngày / baseline**: 2026-11-03. `origin/main = b2a71629a87c3099a4267c1590e955c71a8f476e` (merge PR #30 / P24). Nhánh làm việc `arena/e486a516-eternal-sunmmoner` (Arena cố định tên nhánh; đây là nhánh feature MỚI, sạch, chưa có PR).
- **PR live (refresh bằng `gh`)**: #30 `MERGED` (không đụng lại), #29 `MERGED`, #28 `mission01/floor1-rescue` = `OPEN/Draft` (29 file gameplay). Kiểm tra giao thoa: `gh pr diff 28 --name-only` ∩ file ticket này = rỗng (sẽ kiểm tra lại trước push). Báo cáo P24 ghi "OPEN/BLOCKED" là **từ lịch sử**, không phải trạng thái PR hiện tại.
- **Năng lực thật**: KHÔNG có tool native spawn/delegate subagent → 6 vai trò là **logical checkpoint tuần tự trong MỘT Arena agent**. `java`/`gradle`/Android SDK/`/usr/lib/jvm` KHÔNG CÓ → `LOCAL BUILD NOT RUN`, build/test/render lấy từ GitHub Actions thật. Pillow cài trong venv `/tmp/qa-venv` (offline pypi). Không Scenario MCP/API trả phí/model/LoRA.
- **Skill đã đọc**: `AGENTS.md`, router, `eternal-arena-quality-loop` (toàn bộ + phụ lục V1), director, environment-architect, technical-artist, visual-gameplay-qa, android-performance, integration-reviewer, `docs/GAME_SKILL_SELECTION.md`, 3 nguồn `scenario-game-assets` / `scenario-textures` / `scenario-refine-loop` (chỉ nguyên tắc: rubric trước, batch nhỏ, sửa đúng lỗi, giới hạn 3 vòng).

## Mục tiêu người chơi thật

Ở zoom chơi 0.88 / 1.00, cụm dịch vụ Sanctuary phải đọc ra **khối nhà có mái, mặt tiền, mặt hồi và cửa lõm** thay vì bốn mảng xám cùng giá trị; Summoning Hall ở 1.48 phải có mái/đầu hồi/cột/tường hồi tách bạch và không bị kéo tối như P24. Ở 0.36 bốn công trình dịch vụ không được trông như bốn cottage giống hệt nhau.

## Current behavior (BASELINE thật, đo được)

`docs/visual-evidence/p23/engine-render/` trên `main` = 32 ảnh engine (24 P23 + 8 HDR-tier) do CI P24 sinh ra, camera cố định trong `docs/visual-evidence/p23/camera_props.json`. Đây chính là baseline P25 (code P24 đã lên main).

Số đo ROI trên **PNG tài sản thật** (`roi_definitions.json`, script `measure_asset_rois.py`, luminance `0.2126R+0.7152G+0.0722B`, chỉ pixel alpha ≥200):

| asset | roof_key | roof_return | front | side | soffit | Nhận xét |
| --- | --- | --- | --- | --- | --- | --- |
| `hero_quarters` | 44.11 | 38.97 | 55.19 | **56.76** | 28.24 | **side sáng hơn front +1.57** (ngược chiều đổ sáng) |
| `restaurant` | 65.93 | **64.99** | 63.73 | **63.59** | 28.49 | 4 mặt lệch ≤2.34 → mái slate / tường plaster cùng dải giá trị |
| `summoning_hall` | 64.86 | **72.84** | 80.01 | 67.87 | 79.70 | return sáng hơn pitch **+7.98** (đảo chiều), "soffit" 79.7 không tối |

P24 full-frame (đã công bố, **giữ nguyên và báo cáo độc lập**): `sanctuary_overview_z036 Δ(p90−p10)=+0.0`, `service_cluster_z088=+0.0`, `tower_gate_whole_z148=+5.4`; `summoning_hall_whole_z148=−3.8`; gate `+3 trên ≥2/3` đạt **1/3**; QA mềm 19/24, visual coherence 2/4. `no-flood` |Δmean| tối đa 3.60% = PASS.

### Phân loại nguyên nhân (tách "đã xác minh bằng ảnh engine" vs "suy từ code")

| Nhóm | Kết luận | Bằng chứng |
| --- | --- | --- |
| (a) lỗ alpha / topology | **ĐÃ SỬA ở P23**, không phải nguyên nhân hiện tại | `docs/art/P23_REPORT_VI.md`; generator `seal_seams`/`prune_specks`/`validate` chặn lỗ trong suốt; `MapForgeP23ContractTest` xanh |
| (b) clustering giá trị mái/trước/bên | **NGUYÊN NHÂN CHÍNH, đã xác minh định lượng** | bảng ROI ở trên + soi trực tiếp `service_cluster_z100.png`, `armory_full_z148.png` |
| (c) soffit / eave / gable | soffit band tồn tại (`#191d1f`) nhưng chỉ 4–5px và bị `edge_light`/grade P24 làm mờ; Summoning Hall cornice sáng, không có band tối | code `soffit()` + ROI 28.24/28.49/79.70 |
| (d) cửa lõm / jamb / bậc | generator `recessed_door` đã có reveal + jamb + bậc; **không** đổi vị trí (P20 armory trigger phải giữ) | code `tools/mapforge_p23_architecture.py:337`, `DOOR_CONTRACT` |
| (e) chân / foot pivot | `ground_ao`, `plinth`, `drawFootOffset` ổn; không có nhà lơ lửng trong ảnh engine | code + ảnh engine |
| (f) grade HDR P24 che sai mặt | `HdrLightResponse.drawVolumeGrade` là gradient **theo không gian** trên toàn silhouette, không theo luminance/material → sáng hoá cả pixel tối góc NW, tối hoá cả pixel sáng góc SE; đây là lý do `summoning_hall −3.8` | `ROUND_4.md` P24 + `HdrLightResponse.kt:drawVolumeGrade` |
| (g) identity vật liệu | palette `SLATE` (`#333f49`) và `PLAS` (`#3d454c`) gần như cùng hue/value; `upper["dk"]` chỉ kém `base` ~9 luminance | `tools/mapforge_p23_architecture.py:61-88` |

**Kết luận root cause**: (b) + (g) là gốc — **giá trị mặt phẳng không có trong chính sprite đã author**. P24 chỉ thêm ánh sáng đè lên, không thể tạo cạnh khối. Vì vậy sửa bằng **offline baked per-face palette / value grammar** trong generator, KHÔNG bằng glow/shader mới. (f) chỉ xử lý có điều kiện nếu grade P24 vẫn triệt tiêu giá trị đã author.

## User-visible win

Mái có mặt đón sáng / mặt hồi tối / sống mái / viền eave / band soffit tối riêng biệt; tường trước sáng, mặt hồi bên tối rõ, góc có cột/quoin đỡ; cửa vẫn lõm đúng trigger; chân nhà có bậc/plinth đọc được. Không tăng brightness toàn cục, không wash hốc rift Tower Gate.

## Acceptance (CHỐT TRƯỚC KHI XEM CANDIDATE)

### Blocker nhị phân
1. **B1 CI**: `main.yml` + `lyra-render-evidence.yml` = `success` trên đúng SHA code ứng viên; `MapForgeP23RenderTest`, `MapForgeP23ContractTest`, `HdrLightingRenderTest`, `SanctuaryEnvironmentArtTest`, `ScenePerformanceTest`, `DepthAddendumTest` xanh. Phân biệt `action_required`/lỗi tải artifact hạ tầng với lỗi build/test thật.
2. **B2 evidence**: trước/sau engine PNG cùng camera `0.36/0.88/1.00/1.48` + actor ở cửa + đêm/mưa; `camera_props.json` không đổi; có baseline render chạy thật từ `b2a7162`. Contact sheet chỉ bổ trợ.
3. **B3 hợp đồng**: không đổi `OBLIQUE_Y_SCALE=0.72f`, canvas W/H, foot row, alpha silhouette, `DOOR_CONTRACT`, anchors/collider/nav/save, `FilterQuality.None`; generator `--check` sạch (không lỗi alpha/size/pivot), `tools/validate_assets.py` PASS, manifest SHA khớp, sinh lại 2 lần cho byte giống hệt. Không đụng 29 file PR #28.
4. **B4 không flood**: `|Δmean|/mean < 8%` trên `sanctuary_overview_z036`, `service_cluster_z088`, `tower_gate_whole_z148` (giữ nguyên cổng P24). **Báo cáo độc lập** cổng P24 `Δ(p90−p10) ≥ +3 trên ≥2/3`, KHÔNG đổi ngưỡng, KHÔNG gọi FAIL thành PASS.
5. **B5 ROI mặt phẳng** (ngưỡng chốt trước sửa; đo trên PNG tài sản bằng `measure_asset_rois.py`, đối chiếu ROI chiếu lên ảnh engine bằng `measure_engine_rois.py`):
   - `front_mean − side_mean ≥ 12.0` luminance (mặt hồi phải tối hơn mặt tiền) cho **cả 3 asset pilot**.
   - `roof_key_mean − roof_shaded_return_mean ≥ 10.0` (pitch đón sáng sáng hơn hip return).
   - `|roof_key_mean − front_mean| ≥ 10.0` (mái là họ giá trị riêng, không trùng tường).
   - `front_mean − soffit_mean ≥ 18.0` (band soffit tối).
   - `roof_key spread ≥ 14.0` (đọc được lớp ngói, không salt-and-pepper phẳng).
   - `door_reveal_mean ≤ front_mean − 20.0` (chỉ áp dụng 2 service có cửa; Summoning Hall là zone-only, ROI `rune_stage` và KHÔNG áp ngưỡng cửa).
6. **B6 visual coherence ≥3/4** từ ảnh engine thật ở `z0.88` VÀ `z1.00`: mái / mặt trước / mặt bên / cửa đọc là khối; reviewer mô tả được lỗi cụ thể nếu FAIL. Không tự PASS nếu chỉ có CI xanh.
7. **B7 hiệu năng**: không thêm draw call runtime, không bitmap/gradient allocation mỗi frame, không fullscreen pass; OFF/REDUCED/BALANCED giữ nguyên; `PHYSICAL DEVICE`/GPU/FPS/nhiệt = `NOT TESTED`.

### Sáu chiều mềm 0–4 (chỉ chấm khi có ảnh engine)
visual coherence, technical correctness, player clarity, material differentiation, polish, efficiency — mỗi ≥3, tổng ≥20/24. Thiếu evidence → `NOT VERIFIED`.

## Regression risks

camera 0.72 & anchor, alpha foot row, door trigger P20 armory, manifest/CRC, nearest-neighbor, silhouette shadow cache, P24 tier/emitter/dark aperture, không đổi gameplay/save, không đổi weather/time giữa before/after, không đụng PR #28.

## Files MAY change (mỗi file đúng 1 writer, tích hợp nối tiếp)

| path | writer | thứ tự |
| --- | --- | --- |
| `docs/quality-loop/p25-architecture-volume/*` | Director / QA | 1 (rubric, ROI, rounds, report) |
| `tools/mapforge_p23_architecture.py` | Environment Artist (lead) | 2 |
| `app/src/main/assets/premium_slice/{hero_quarters,restaurant,summoning_hall}.png` | generator (deterministic) | 3, sau khi generator tồn tại |
| `app/src/main/assets/premium_slice/{armory,hero_registry,synthesis_lab,tower_gate}.png` | generator (deterministic) | **ROUND 2 / Phase C**, chỉ sau khi 3 pilot PASS |
| `app/src/main/assets/premium_slice/manifest.json` | generator `stamp()` | 3 |
| `tools/p25_validate_faces.py` (mới) | QA (validator, không sửa generator) | 4 |
| `.github/workflows/lyra-render-evidence.yml` | Director (thêm snapshot P25 + path validator) | 5 |
| `app/src/main/java/com/example/game/pixel/HdrLightResponse.kt` | Technical Artist — **CHỈ** nếu ROI engine chứng minh grade P24 vẫn triệt tiêu mặt phẳng | 6 (có điều kiện) |
| `docs/visual-evidence/p25/engine-render-before/*`, `docs/visual-evidence/p23/engine-render/*` | CI bot | 7 |

## Files MUST NOT change

`WorldMapModels.kt` (`ObliqueCamera`, `SanctuaryLight`, `WorldObstacle`, `SanctuaryHouseEntrances`), `ObliqueSpriteGeometry.kt`, `SanctuaryEnvironmentShadows.kt`, `PixelSpriteRenderer.kt` (trừ khi có quyết định owner-level), `HdrLightResponse.kt` (chỉ mở nếu B5 engine ROI chứng minh grade P24 triệt tiêu mặt phẳng — tới ROUND 2 chưa cần), mọi gameplay/combat/AI/ViewModel/save/`model`/`data`, `AGENTS.md`, `.agents/skills/**`, `.codex/**`, `docs/quality-loop/p24-sanctuary-hdr/**` (tài liệu lịch sử), `sanctuary_p03/*.png` (background buildings, KHÔNG nằm trong 7 facility), `tools/validate_assets.py`, `docs/visual-evidence/p25/engine-render-before/**` (baseline bất biến), `docs/visual-evidence/p25/authored-before/**` (baseline bất biến), 29 file PR #28, PR #30.

**Cập nhật quyền Phase C (sau ROUND 1)**: 3 pilot PASS B5 (17/17 authored ROI + engine-projected ROI) và B6 (3/4 visual coherence trên ảnh engine thật z0.88/z1.00) → `premium_slice/{armory,hero_registry,synthesis_lab,tower_gate}.png` ĐƯỢC PHÉP sinh lại trong ROUND 2, cùng grammar, cùng ROI/threshold, không nới rubric. Xem `ROUND_2.md`.

## Song song / nối tiếp

Không có subagent native → mọi bước nối tiếp. Chỉ lệnh đọc (`grep`, `read_file`, `gh`) gọi cùng lượt. Generator + manifest + PNG là **một writer**; CI render không chạy đồng thời với push code. Phase C chỉ mở sau khi 3 pilot PASS (B5 + B6).

## Selected skills

Router + quality-loop + `environment-architect` (lead) + `technical-artist` (điều kiện) + `visual-gameplay-qa` (critic) + `android-performance` (gate) + `integration-reviewer` (cổng cuối). Nguồn: `scenario-game-assets`, `scenario-textures`, `scenario-refine-loop` (3 path đã xác minh tồn tại).

## Test plan

- Local (KHÔNG có JDK): `python3 tools/mapforge_p23_architecture.py --check` (venv Pillow), `python3 tools/validate_assets.py`, sinh lại 2 lần so sánh SHA PNG, `measure_asset_rois.py`, `measure_engine_rois.py`, `git diff --check`.
- CI thật: `gradle :app:assembleDebug` (`main.yml`) + render/contract suites (`lyra-render-evidence.yml`) trên đúng SHA ứng viên; đọc `gh api actions/runs?head_sha=<sha>`.
- Ảnh: 24 khung P23 + 8 HDR, zoom `0.36/0.88/1.00/1.48`, actor ở cửa (`quarters_door_actor_z148`, `restaurant_door_actor_z148`, `armory_door_trigger_actor_z148`), đêm/mưa (`hdr_*`), canopy occlusion (`armory_behind_occlusion_z148`). Nhãn `REAL ENGINE ROBOLECTRIC RENDER`.
- Perf: đếm draw call tĩnh, số mask cache, bytes PNG thêm, không per-frame decode. `ScenePerformanceProbe` chỉ CPU/frame pacing.

## Visual bookmarks (camera lock, `docs/visual-evidence/p23/camera_props.json`)

`sanctuary_overview_z036` (1600,1380,0.36), `service_cluster_z088/z100` (960,860), `armory_full_z148` (995,560,1.48), `quarters_door_actor_z148`, `restaurant_door_actor_z148`, `summoning_hall_whole_z148` (2430,1715,1.48), `tower_gate_whole_z148` (1960,310,1.48), `hdr_balanced_night_z088`, `hdr_balanced_night_rain_z088`. `MapForgeP23RenderTest.prepare()` = `debugOverride(12f, CLEAR)`; `HdrLightingRenderTest` dùng post pass lobby thật. **Không** so sánh khác camera/giờ/thời tiết như thể chỉ vật liệu đổi.

## Evidence ledger

baseline `b2a7162` + baseline CI dispatch run; candidate SHA từng vòng; run URL + conclusion; tên PNG trước/sau; bảng ROI roof/front/side/soffit/door; bảng full-frame mean/p10/p90; SHA PNG & manifest; giới hạn `NOT TESTED`.

## Round budget / STOP / rollback / PR

Tối đa **3 vòng sửa** (ROUND_1..3), mỗi vòng tối đa 1–3 sửa tập trung. **STOP ngay** nếu: generator `--check` lỗi alpha/size/door; manifest mismatch; CI build/contract FAIL; `|Δmean| ≥ 8%`; hốc rift phát sáng; phát hiện đụng file PR #28; hết 3 vòng mà B5/B6 chưa đạt → `BLOCKED` trung thực + danh sách sub-ticket kế tiếp, **không** scale Phase C. Rollback = revert commit code trên nhánh (không force push, không xoá file user, không `git clean`/`reset --hard`). PR mở (Draft nếu chưa đạt), **KHÔNG auto-merge**, không đụng #30, owner tự duyệt.
