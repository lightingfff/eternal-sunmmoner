# WORK_ORDER — `p24-sanctuary-hdr`

- **Ngày / baseline**: 2026-11-03, `main` = `12c6d2f59f022310ba057f5f02c70451737c4e95` (đã merge PR #29 P23 + bootstrap skill). Branch phiên `arena/3d2dcfc6-eternal-sunmmoner` (tách từ `main`, Arena cố định tên nhánh).
- **PR liên quan (refresh live bằng `gh`)**: #28 `mission01/floor1-rescue` = OPEN/Draft, 29 file; #29 = MERGED; #26 P23 = MERGED. Đã kiểm tra `gh pr view 28 --json files`: **không giao thoa** với file ticket này.
- **Skill đã đọc**: `AGENTS.md`, `eternal-summoner-skill-router`, `eternal-arena-quality-loop` (toàn bộ, kể cả phụ lục V1), `eternal-arena-studio-director`, `eternal-arena-environment-architect`, `eternal-arena-technical-artist`, `eternal-arena-visual-gameplay-qa`, `eternal-arena-android-performance`, `eternal-arena-integration-reviewer`, `docs/GAME_SKILL_SELECTION.md`, 2 workflow CI.
- **Năng lực thật**: KHÔNG có tool native spawn/delegate subagent → 6 vai trò là **logical checkpoint tuần tự trong MỘT agent** (Director → Environment/Technical Artist → QA critic → Perf auditor → Integration reviewer → Director). Không gọi API trả phí / Scenario MCP / `.codex`.

## Mục tiêu người chơi thật

Ở Sanctuary lobby (0.36 tổng thể, 0.88/1.00 chơi, 1.48 cận cảnh), công trình phải đọc ra **khối đá có ánh sáng**: mặt mái/mặt tiền/mặt bên phân cấp sáng tối, cửa sổ/đèn lồng/cổng dịch chuyển phát sáng thật và chiếu nhẹ xuống đá lát, quảng trường có điểm sáng trọng tâm, chân nhà có bóng tiếp xúc gắn với bóng định hướng. Người chơi thấy "giàu ánh sáng" hơn chứ không phải "sáng hơn" — vùng tối vẫn đọc được chi tiết, pixel-art vẫn sắc.

## Current behavior (BASELINE thật, đọc code + ảnh engine)

Ảnh `REAL ENGINE ROBOLECTRIC RENDER` tại `docs/visual-evidence/p23/engine-render/` (24 ảnh, camera lock trong `camera_props.json`) đã được mở và soi trực tiếp:

| Defect | Bằng chứng |
| --- | --- |
| D1 Mái/mặt tiền/mặt bên **đồng giá trị**, nhà đọc phẳng như decal | `service_cluster_z088.png`, `armory_full_z148.png`: mái slate xám, tường xám, mặt bên xám cùng dải sáng; không có key-light NW rõ |
| D2 Cửa sổ vàng sáng nhưng **không chiếu** xuống tường/đá | `service_cluster_z088.png`: ô cửa `#FFB844`孤立, xung quanh không có spill; `drawProp` chỉ vẽ sprite, `drawSanctuaryLocalLights` chỉ 3 stamp cố định |
| D3 Tower Gate thiếu chiều sâu nguồn sáng, đá **tương phản thấp** | `tower_gate_whole_z148.png`: rift là oval viền nhạt + hốc đen, không có lõi emissive/đổ sáng lên bậc thang; P23 report chính mình ghi "Masonry vẫn hơi thấp tương phản ở 0.88" (`docs/art/P23_REPORT_VI.md:110`) |
| D4 Quảng trường **không có điểm sáng trọng tâm** | `sanctuary_overview_z036.png`: đĩa đá xám phẳng, 4 rune mờ; `drawSanctuaryLocalLights` có 1 stamp `COOL_AETHER` alpha `.08` |
| D5 Chân công trình thiếu AO/grounding đọc được | `service_cluster_z088.png`: chân nhà hoà vào cỏ; `drawGroundShadows` có contact path nhưng rất mỏng, không có rim tối sát móng |
| D6 Pipeline ánh sáng **tách đôi**, thiếu day/night & weather trong ảnh evidence | `LivingWorldRenderer.drawLocalLights` chỉ chạy ở `SanctuaryLobbyScreen:653`, **KHÔNG** chạy trong `MapForgeP23RenderTest` (harness gọi `drawSanctuaryFloatingWorld` + `drawGroundShadows` + `drawProp`). Vì vậy 24 ảnh baseline không hề có local lights của lobby. |
| D7 Nhiều hàm "premium" định nghĩa nhưng **không được gọi** | `SanctuaryDarkFantasyShaders.drawPixelBloom`, `drawDirectionalCastShadow`, `drawBackgroundHaze`, `drawMidgroundFog`, `drawDistantFade`, `drawTreeCanopyAO`…; chỉ `drawStoneCreviceAO`/`drawBuildingAO`/`drawTreeCanopyAO`/`drawDitheredShadow` được gọi từ `drawSanctuaryAmbientOcclusion`. `SANCTUARY_AGSL` chỉ là chuỗi docs, không phải RuntimeShader.active. |

Số đo baseline (local script trên PNG engine thật, luminance `0.2126R+0.7152G+0.0722B`):

| ảnh | mean | p10 | p90 | p90−p10 | std |
| --- | --- | --- | --- | --- | --- |
| `sanctuary_overview_z036` | 70.59 | 39.4 | 97.0 | 57.6 | 23.8 |
| `service_cluster_z088` | 62.94 | 35.2 | 88.2 | 53.1 | 24.2 |
| `service_cluster_z100` | 61.74 | 34.6 | 88.0 | 53.4 | 25.3 |
| `armory_full_z148` | 60.33 | 34.6 | 82.1 | 47.5 | 22.2 |
| `tower_gate_whole_z148` | 58.71 | 30.2 | 81.4 | 51.2 | 22.7 |
| `tower_gate_portal_actor_z100` | 64.39 | 34.2 | 91.4 | 57.3 | 25.8 |
| `summoning_hall_whole_z148` | 70.12 | 39.4 | 102.6 | 63.1 | 25.7 |

## User-visible win sau khi xong

Một pipeline ánh sáng duy nhất, có cache, chạy trong **cả lobby và render harness**: nguồn sáng (đèn/đuốc/cửa sổ/portal/rift/tinh thể/lò rèn) có lõi emissive + vũng sáng trên mặt phẳng `.72` + quầng rất nhẹ; mặt công trình được phân cấp bởi key-light NW cố định (`SanctuaryLight`); quảng trường có tiêu điểm; exposure tăng nhẹ về đêm/mưa chỉ trên **ánh sáng cục bộ**, không tăng brightness toàn cục. OFF/REDUCED/BALANCED cắt dần hiệu ứng mà không đổi gameplay.

## Acceptance (chốt TRƯỚC khi xem candidate)

**Blocker nhị phân** (một FAIL chặn `DONE`):
1. CI thật trên candidate SHA: `:app:assembleDebug` (`main.yml`) + `lyra-render-evidence.yml` = `success`; các test `MapForgeP23RenderTest`, `MapForgeP23ContractTest`, `SanctuaryEnvironmentArtTest`, `ScenePerformanceTest`, `DepthAddendumTest`, `HdrLightingRenderTest` (mới) xanh.
2. 24 ảnh engine trước/sau regenerate **cùng camera** (`camera_props.json` không đổi `camX/camY/zoom/timeSec`), đủ zoom 0.36 / 0.88 / 1.00 / 1.48; có ảnh `hdr_off/reduced/balanced/day/night/rain`.
3. **Không flood sáng**: `|mean_after − mean_before| / mean_before < 8%` trên `service_cluster_z088` VÀ `tower_gate_whole_z148`; đồng thời `p90−p10` (tương phản cục bộ) tăng ≥ 3.0 trên ít nhất 2/3 ảnh `service_cluster_z088`, `tower_gate_whole_z148`, `sanctuary_overview_z036`. Đây là cổng "đẹp hơn chứ không chỉ sáng hơn".
4. Không regression hợp đồng: `OBLIQUE_Y_SCALE=0.72f`, `FilterQuality.None` cho sprite, depth sort, anchors/collider/door trigger, save — `MapForgeP23ContractTest` + `SanctuaryEnvironmentArtTest` xanh; diff không chạm file PR #28.
5. OFF tier không vẽ HDR nào (ảnh `hdr_off_z088` không có lõi/quầng), REDUCED ⊂ BALANCED; không có fullscreen bloom/blur/shader/AGSL/RuntimeShader mới.

**Chất lượng mềm 0–4** (chỉ chấm khi có ảnh engine): visual coherence, technical correctness, player clarity, material differentiation, polish, efficiency — mỗi ≥3, tổng ≥20/24. Không có screenshot engine → `NOT VERIFIED`, không tự PASS.

## Regression risks

camera `.72` (mọi vũng sáng phải foreshorten đúng), anchors/foot pivot (lõi emissive phải đặt đúng pixel nguồn, không float), pixel-art (`FilterQuality.None` cho sprite; stamp ánh sáng dùng `Low` và alpha bị chặn), local saves (không đổi), performance (cached mask, cull trước draw, OFF/REDUCED/BALANCED, không allocation mỗi frame), UI (không che HUD), day-night/weather (exposure chỉ scale alpha cục bộ).

## Files MAY change (mỗi file = 1 writer, tích hợp nối tiếp)

| path | writer role | thứ tự |
| --- | --- | --- |
| `app/src/main/java/com/example/game/pixel/HdrLightResponse.kt` (mới) | Technical Artist | 1 |
| `app/src/main/java/com/example/game/pixel/PixelSpriteRenderer.kt` | Technical Artist (2 call site trong `drawProp`) | 2 (sau khi file mới tồn tại) |
| `app/src/main/java/com/example/game/pixel/LivingWorldRenderer.kt` | Technical Artist (bỏ stamp trùng, giữ wet streak) | 3 |
| `app/src/main/java/com/example/game/pixel/SanctuaryWorldRenderer.kt` | Technical Artist (`drawSanctuaryLocalLights`) | 4 |
| `app/src/test/java/com/example/HdrLightingRenderTest.kt` (mới) | QA/Regression (test, không sửa implementation) | 5 |
| `.github/workflows/lyra-render-evidence.yml` | Director (thêm path + 1 step + snapshot before) | 6 |
| `docs/visual-evidence/p24/engine-render-before/*.png` | CI bot (snapshot tự động) | 7 |
| `docs/visual-evidence/p23/engine-render/*.png` | CI bot (after, cùng camera) | 7 |
| `docs/quality-loop/p24-sanctuary-hdr/*` | Director / QA | liên tục |

## Files MUST NOT change

`WorldMapModels.kt` (`ObliqueCamera`, `SanctuaryLight`, `WorldObstacle`, `SanctuaryHouseEntrances`, `PropKind`), `ObliqueSpriteGeometry.kt`, `SanctuaryEnvironmentShadows.kt` projection, `SanctuaryArtAssets.kt`, mọi PNG trong `app/src/main/assets/**` (`premium_slice`, `sanctuary_p03`, `sanctuary_pack`), `tools/validate_assets.py` manifest, save/data (`LocalGuestSaveManager.kt`, `SummonerRepository.kt`), gameplay/combat/AI/ViewModel/MainActivity, `.codex/**`, `.aistudio/**`, `AGENTS.md`, skill đã cài, và **29 file của PR #28** (`mission01/floor1-rescue`).

## Song song / nối tiếp

Không có tool subagent native → mọi bước nối tiếp. Chỉ các lệnh đọc (`grep`, `read_file`, `gh`) chạy cùng lượt. Cùng file `PixelSpriteRenderer.kt` chỉ một writer; CI render job không chạy đồng thời với push code (tránh bot commit lẫn).

## Selected skills

Router + `eternal-arena-quality-loop` + `eternal-arena-technical-artist` (lead) + `eternal-arena-environment-architect` (đối chiếu khối) + `eternal-arena-visual-gameplay-qa` (critic) + `eternal-arena-android-performance` (gate) + `eternal-arena-integration-reviewer` (cổng cuối). Tham khảo có path thật: `docs/skill-library/scenario/upstream/skills/scenario-refine-loop/SKILL.md`, `.../scenario-textures/SKILL.md`, `.../game-engines/unity/scenario-unity-mobile/SKILL.md` (chỉ nguyên tắc, không chạy Unity).

## Test plan

- Local: **KHÔNG CÓ** JDK/Gradle/Android SDK (`java`/`gradle` not found, `ANDROID_HOME` unset) → `LOCAL BUILD NOT RUN`, không bịa kết quả. Local chỉ chạy được: `git diff --check`, script Python (Pillow trong venv) đo luminance trên PNG engine thật.
- CI thật: `gradle :app:assembleDebug`, render/contract suites trong `lyra-render-evidence.yml`, `HdrLightingRenderTest` mới; đọc `gh api actions/runs?head_sha=<sha>`.
- Ảnh: 24 khung P23 (0.36/0.88/1.00/1.48, actor ở cửa/tán) + 6 khung HDR tier/weather; nhãn `REAL ENGINE ROBOLECTRIC RENDER`. Contact sheet KHÔNG dùng làm bằng chứng PASS.
- Perf: đếm draw call tĩnh + kiểm tra cache (stamp `by lazy`, không `Bitmap.createBitmap` trong hàm draw); `GPU/FPS/thermal NOT TESTED` (không có máy thật), không tự nhận FPS từ Robolectric.

## Visual bookmarks (camera lock, từ `camera_props.json`)

`sanctuary_overview_z036` (1600,1380,z0.36), `service_cluster_z088/z100` (960,860), `armory_full_z148` (995,560,z1.48), `tower_gate_whole_z148` (1960,310), `tower_gate_portal_actor_z100` (1960,460,timeSec .25), `summoning_hall_whole_z148` (2430,1715), `avenue_props_lantern_f0_z148` (1185,1015). `WorldEnvironment.debugOverride(12f, CLEAR)` trong harness; `timeSec=0` trừ khung portal.

## Evidence ledger

baseline `12c6d2f`; candidate SHA từng vòng; lệnh CI + run URL + conclusion; tên PNG trước/sau; bảng mean/p10/p90/độ lệch; kích thước file; giới hạn `NOT TESTED`.

## Round budget / rollback / PR policy

Tối đa **3 vòng sửa đầy đủ**. Vòng nào FAIL → root cause + tối đa 1–3 sửa tập trung → render lại cùng camera → cập nhật `ROUND_N.md`. Còn blocker sau 3 vòng → `BLOCKED` trung thực, Draft PR, không tuyên bố DONE. Rollback = revert commit code trên nhánh (không force push, không xoá file user). PR mở, **KHÔNG merge**, owner duyệt.
