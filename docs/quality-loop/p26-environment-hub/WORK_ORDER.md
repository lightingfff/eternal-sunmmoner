# P26 — ROUND 1 WORK ORDER: Environment (Sky / Day-Night / Weather) + Gate Portal + Hub UI Shell

**Baseline**: `origin/main = 358a359fcde37fc2f85c34050a9ff63bba0d1f5c` (P25 đã merge) · **Branch**: `arena/c4f4092c-eternal-sunmmoner` · **Mission**: `P26_MASTER_HUB_UI_GATE_MISSION_PLAN.txt` + `P26B_SKILL_TRAINING_AND_ENVIRONMENT_ADDENDUM.txt` · **PR mới**, KHÔNG auto-merge.

## 0. Scope decision (bounded subset, không cắt ngẫu nhiên)

Mission cho phép: "complete the highest-value bounded subset, explicitly say what remains, recommend the exact next slice". Ưu tiên theo FINAL DECISION RULE của mission (fun/clarity → premium UI → gate structure → ...) và P26B priority (#1 time-of-day distinction, #2 sky/sunlight, #3 building integration, #4 weather).

**PR này delivers**:
1. **Sky + time-of-day + weather + sunlight system** (P26B §3–§6) — sky gradient theo trạng thái, mây theo thời gian/weather, sun/moon disc + rays, ambient/directional phân tách, weather ảnh hưởng lighting.
2. **Gate portal drama** (P26 Part II.1) — portal energy mạnh hơn ban đêm, **localized** (không screen-wide), rift tối giữ nguyên (bảo vệ P24).
3. **Hub UI shell** (P26 Part I.1–2) — panels từ opaque đen → glass trong mờ + border vàng premium; bottom nav thêm icon; world vẫn thấy.

**Deliberately out of this PR (next slices, liệt kê rõ)**: gate-entry cutscene + 3-gate destination screen (Part II.2–3), profile/party cards (Part III), assistant foundation (Part IV), mission variety beyond clear-all (Part V), island expansion *new plots* (Part VI — level-5 expansion foundation **đã có sẵn**: `IslandExpansionMilestone`, `unlockIslandExpansion`), title/loading polish (Part VII).

## 1. Audit findings (đo/xác nhận trên code, không phỏng đoán)

- `SanctuaryWorldRenderer.drawSanctuaryFloatingWorld`: sky = gradient tĩnh `0xFF0D0F1A→0xFF2E314F` (indigo→violet) + moon glow oval + mây tối oval + `drawDiagonalMoonlightRays` luôn bật → **tất cả giờ trong ngày giống nhau** (P26B §10 #1–#2).
- `WorldEnvironment.WorldLighting.at(hour)`: 8 anchor (0,5,7,12,16,18.5,21,24) với ambient tint/intensity — không có sky gradient riêng theo trạng thái, mây không phân biệt.
- `LivingWorldRenderer.drawWorldGrade`: lift daytime + night overlay — bị giới hạn (đã test), không tạo sự khác biệt trạng thái.
- `PixelSpriteRenderer` gate branch: portal oval pulse `0.88+0.12 sin` — rung động nhưng không mạnh hơn ban đêm; rift (coreRadius=0) giữ tối — đúng, cần bảo vệ.
- `FantasyManagement.FantasyPanel` / `FantasySkin.Panel`: `Ink=0xFF1B222B`, `Panel=0xFF1C242E` — **opaque**, đúng mô tả "giant black slabs". `SanctuaryManagementNav`: text-only tabs.
- Cơ chế evidence có sẵn: `HdrLightingRenderTest` (render matrix) + `lyra-render-evidence.yml` (publish PNG) + `LayoutResponsivenessRobolectricTest.saveViewScreenshot` (UI screenshot).

## 2. Acceptance thresholds — PRECOMMITTED, không soften sau khi thấy candidate

Đo trên PNG 1280×720, sky band = top 25% (y 0..180), whole-frame = toàn khung. Mean luminance Rec.709 (0.299R+0.587G+0.114B), 0–255.

### E — Environment (P26B)
- **E1 Sky distinctness** (CLEAR, z0.88, service cluster): `|morning(7h) − noon(12h)| ≥ 8`, `|noon − evening(18.5h)| ≥ 6`, `|evening − night(0h)| ≥ 8`, `|noon − night| ≥ 18`.
- **E2 Noon brightest**: noon sky band ≥ morning sky band; noon whole-frame mean ≥ night whole-frame mean + 5.
- **E3 Weather mood** (same camera+hour noon): `cloudy sky band ≤ clear − 5`, `rain sky band ≤ clear − 8`.
- **E4 No-flood** (giữ P24): same hour/camera/weather before↔after whole-frame `|Δmean| < 8%`.
- **E5 Determinism/no per-frame allocation**: render 2 lần tại timeSec=0 byte-identical; cached HDR mask count = 7 (giữ); không fullscreen pass mới (sky gradient thay gradient tĩnh, không rect opaque mới toàn màn hình).

### G — Gate portal
- **G1 Rift dark (protected)**: rift ROI (gate z1.48) mean ≤ 40 (void tối giữ); before↔after `|Δmean| ≤ 4`.
- **G2 Night gate energy**: portal halo region (ROI quanh oval portal) mean tại đêm ≥ mean ban trưa (local light mạnh hơn ban đêm) và ≤ 150 (không wash).

### U — UI shell
- **U1 Panels translucent**: `FantasyPanel`/`FantasySkin.Panel` container alpha < 1f (test đơn vị), gold border giữ.
- **U2 Nav icons**: `nav_heroes/nav_summon/nav_team/nav_tower/nav_inventory` tags giữ, thêm icon.
- **U3 UI screenshot**: lobby narrow 360×640 + wide 800×360 captured (evidence), panels không che kín world.

### P — Performance
- **P1**: không per-frame allocation mới (brush/mây cached), không fullscreen blur/bloom; `LivingWorldTest` + `HdrLightingRenderTest` + `ScenePerformanceTest` xanh.

## 3. Execution order (3 round max, giống P25)

1. **Round 0 baseline**: commit test files + workflow (renderer **không đổi**) → dispatch workflow → BEFORE frames (sky vẫn tối) + CI xanh.
2. **Round 1 implement**: `WorldEnvironment` lighting model → `SanctuaryWorldRenderer` sky/rays/clouds → gate portal night emphasis → UI shell → dispatch → AFTER frames + CI.
3. **Round 2 QA**: measure BEFORE/AFTER theo thresholds §2 → `QA_MATRIX.md` → fix bounded (tối đa 2 round) → `ROUND_1.md` / `FINAL_REPORT` → PR mới.

## 4. HARD STOP conditions (giống P24/P25)

generator/contract FAIL · CI build/test FAIL · E1–E3 FAIL sau 2 round fix · G1 FAIL (rift bị wash) · E4 `|Δmean| ≥ 8%` · Hết 3 vòng · đụng layout/camera/save/collider/door/P25 assets. → BLOCKED với sub-ticket, không hạ rubric, không fake.

## 5. Constraints

- Không bloom/fullscreen filter/phát sáng toàn cục; không tăng sáng toàn cục để "đạt E2"; không đổi `.72`, camera, layout đảo, save, collider, P24 tiers, P25 assets, PR #28.
- `LOCAL BUILD NOT RUN` (không JDK/Gradle/SDK trong sandbox); evidence = GitHub Actions Robolectric thực tế. FPS/GPU/thermal/emulator/device = NOT TESTED.
- Không fake agent song song; không claim "game is now fun" mà không bằng chứng.
