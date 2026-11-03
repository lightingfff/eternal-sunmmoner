# WORK_ORDER — P26C CLOUD & STORM (Visible Clouds + Storm Mood + True Gameplay-Camera Validation)

**Baseline**: `origin/main = f0dc0de6` (PR #32 P26B đã merge) · **Branch**: `arena/06fa6c23-eternal-sunmmoner` (hiện tại) · **Mission**: P26C remaining slice từ PR #32 + 8-image art reference directives · **PR mới**, KHÔNG auto-merge, KHÔNG merge tự.

## 0. Scope decision (bounded subset, không lan tới P27/PR#28)

**P26C delivers (này là làm ngay):**
1. **Mây thực sự nhìn thấy trong camera gameplay** (không chỉ camera test riêng): cloud layout phù hợp vùng bầu trời thật của camera chơi, nhiều lớp khoảng cách, cull off-screen, deterministic drift, pixel-art khối (3 cấp sáng/tối, bụng mây có shade, bên đón nắng).
2. **STORM làm trạng thái thời tiết thực**: thêm enum STORM vào WorldWeather, mô hình khác RAIN (mây dày nhiều lớp, sky/ambient lạnh, ánh mặt trời suy giảm, haze nặng, bóng mềm hơn, mưa mạnh có tiết chế), không dùng overlay xám/đen toàn cảnh.
3. **Camera render matrix BEFORE + AFTER** (P26C §6): morning/noon/evening/night CLEAR, noon CLOUDY/RAIN/STORM, night CLEAR/STORM, gate z1.48, overview z0.36, cloud-visible bookmarks z1.0/z1.48, motion A/B timeSec 0/4/8.
4. **QA docs**: WORK_ORDER.md, QA_MATRIX.md, ROUND_1.md, FINAL_REPORT_VI.md dưới `docs/quality-loop/p26c-cloud-storm/`.
5. **docs/art/ETERNAL_VISUAL_REFERENCE_BIBLE_V1.md**: chuẩn chung từ 8 ảnh tham khảo (REF01..08), dùng lại cho P27+.

**Deliberately OUT of P26C (là P27, giao riêng sau khi P26C xong):**
- Gate-entry cutscene 4–5s + 3-gate destination screen (REF01).
- Profile/party cards, event board, loading/title (REF02–05, 08).
- Island lvl5 expansion mới (REF07 chỉ là chỉ dẫn thiết kế, không tự mở đảo).
- UI/party/cutscene được khẳng định hoàn thành chỉ vì viết design brief.
- PR #28 Mission 01 files: tuyệt đối không chạm.

## 1. Audit findings (đọc code thật, không phỏng đoán)

- `SanctuaryWorldRenderer.drawDriftingSkyClouds`: 6 cloudSpecs ở world-space Y = 260, 1650, 420, 1520, 2580, 2560 — nhiều khung camera gameplay không thấy mây (P26B ghi rõ). Mây vẽ bằng `drawOval` lặp, cụm ellipse phẳng, không khối, không shade.
- `WorldWeather` enum: CLEAR, CLOUDY, RAIN, FOG — **chưa có STORM**.
- `WorldLighting.withWeather`: đã có weather multipliers cho cloud/rain/fog, cần thêm nhánh STORM.
- `WorldWeather.at()`: lịch CLEAR/CLOUDY/RAIN/CLEAR/FOG — chưa có STORM.
- `drawSkyBands` / `drawSkyRays` / `drawCelestialDiscs` đã có theo giờ — giữ, không làm lại từ đầu.
- Camera: `ObliqueCamera.OBLIQUE_Y_SCALE = 0.72f` (no-go), world anchors / collision / save schema không đổi.

## 2. Acceptance thresholds — PRECOMMITTED, không soften sau khi thấy candidate

Đo trên PNG 1280×720, Rec.709 mean luminance. Camera dùng các khung trong `thresholds.json` (service_z088 camX=960 camY=860 zoom=0.88; gate_z148 camX=1960 camY=310 zoom=1.48; overview_z036 camX=1600 camY=1380 zoom=0.36).

### C — Clouds (P26C)
- **C1 Visible cloud in sky-visible gameplay bookmark**: tại z0.88/1.00 morning CLEAR (7h) và noon CLEAR (12h), pixel evidence (không chỉ hàm được gọi) cho thấy mây trong vùng bầu trời đang hiển thị; không đặt mây ở world Y cố định ngoài vùng nhìn rồi chụp camera riêng.
- **C2 Cloud depth layers**: ≥2 lớp khoảng cách (far soft bank + middle cumulus/sparse wisps), pixel density nhất quán, không phải hàng oval thô thiếu khối.
- **C3 CLOUDY denser than CLEAR**: cùng camera/giờ, mây CLOUDY có mật độ/khối lớn hơn CLEAR (không chỉ alpha cao hơn một đám mây cũ).
- **C4 Storm cloud character**: STORM có mây xám-than/dày nặng, khác RAIN và CLEAR ở cùng giờ/camera.
- **C5 Deterministic drift**: cùng camera/hour/weather, timeSec 0 render 2 lần byte-identical; cloud drift nhẹ, seed cố định.
- **C6 Culling**: cloud hoàn toàn off-screen không được vẽ (không vẽ khi cx/cy ngoài biên + margin).

### E — Environment (giữ P26B, mở rộng thêm STORM)
- **E1 Sky distinctness** (CLEAR, z0.88): |m−n|≥8, |n−e|≥6, |e−ng|≥8, |n−ng|≥18.
- **E2 Noon brightest**: noon sky ≥ morning; noon whole − night whole ≥ 5.
- **E3 Weather mood** (noon service camera): cloudy ≤ clear−5; rain ≤ clear−8; **storm ≤ clear − 12** và storm distinct from rain.
- **E4 No-flood**: same hour/camera/weather BEFORE↔AFTER world region |Δmean| < 8%.
- **E5 Determinism**: timeSec 0 byte-identical; cached sky band buffer reused (a === b).

### P — Performance
- **P1**: không tạo listOf/Triple/Path/Brush mới mỗi frame trong hot loop (cache cloud shapes/vertices); culling trước draw; không per-frame bitmap decode; không fullscreen pass mới.
- FPS/GPU/thermal: NOT TESTED (không thiết bị thật).

## 3. Execution order (tối đa 3 vòng sửa)

1. **Round 0 baseline**: tạo branch + WORK_ORDER → render BEFORE matrix (code chưa đổi) → ghi số liệu cơ sở → CI xanh (hoặc LOCAL BUILD NOT RUN + đợi CI trên SHA).
2. **Round 1 implement**: `WorldEnvironment` (STORM enum + lighting) → `SanctuaryWorldRenderer` (cloud layout可见 + storm character) → render AFTER matrix cùng camera → QA đo thresholds.
3. **Round 2 QA/fix**: so sánh BEFORE/AFTER,fix lỗi cụ thể (tối đa 1–3 nguyên nhân), re-render, cập nhật QA_MATRIX/ROUND_1/FINAL_REPORT.
4. Mở PR cho owner review (không merge).

## 4. HARD STOP conditions

CI build/test FAIL · E1–E3 FAIL sau 2 vòng fix · G1/P26B gate hồi quy (rift void tối, portal halo) · E4 |Δmean| ≥ 8% · hết 3 vòng · đụng camera/0.72/layout/save/collider/P25 assets/PR#28 → BLOCKED, báo Issue + phương án tiếp, KHÔNG hạ rubric, KHÔNG fake.

## 5. Constraints

- Không bloom/fullscreen filter/phát sáng toàn cục để "đạt" E2/E3; không overlay xám/đen toàn cảnh giả trời giông.
- Không đổi `OBLIQUE_Y_SCALE=0.72f`, camera, layout đảo, save, collider, P24/P25 assets, PR #28.
- `LOCAL BUILD NOT RUN` (không JDK/Gradle/SDK sandbox); evidence = GitHub Actions Robolectric thực tế trên đúng candidate SHA.
- Không fake agent song parallel; không claim "game đẹp" mà không có screenshot engine.
- Ảnh tham khảo (8 REF) chỉ dùng làm art direction, KHÔNG commit nguyên ảnh manhwa/bản quyền vào repo, KHÔNG dùng làm background UI/game.

## 6. Files MAY change (mỗi file = 1 writer tại một thời điểm)

| path | writer role | nối tiếp sau |
|---|---|---|
| `app/src/main/java/com/example/game/world/WorldEnvironment.kt` | Technical Artist (lighting/weather) | ENV Architect (cloud layout) |
| `app/src/main/java/com/example/game/pixel/SanctuaryWorldRenderer.kt` | ENV Architect (clouds) | QA (render evidence) |
| `docs/quality-loop/p26c-cloud-storm/WORK_ORDER.md` | Director | — |
| `docs/quality-loop/p26c-cloud-storm/QA_MATRIX.md` | QA | — |
| `docs/quality-loop/p26c-cloud-storm/ROUND_1.md` | QA | — |
| `docs/quality-loop/p26c-cloud-storm/FINAL_REPORT_VI.md` | Director | — |
| `docs/art/ETERNAL_VISUAL_REFERENCE_BIBLE_V1.md` | Technical Artist | — |
| `docs/visual-evidence/p26c/` (BEFORE/AFTER PNG + camera_props.json) | QA (render) | — |

## 7. Files MUST NOT change

- `ObliqueCamera.OBLIQUE_Y_SCALE`, `WorldMapModels.kt` camera constants
- WorldObstacle layout, island footprint, anchors, entrances, collision, save schema
- PR #28 Mission 01 files (nhánh gameplay đang chờ duyệt)
- `.codex`, skill cũ, assets người dùng, commit/PR đã merge

## 8. Selected skills

- Router: `eternal-summoner-skill-router/SKILL.md`
- QA loop: `eternal-arena-quality-loop/SKILL.md` (1 agent, logical roles)
- Art/source: `scenario-game-assets`, `scenario-textures` (mây pixel-art, khối, shade)
- Visual refine: `scenario-refine-loop` (1 lỗi → 1 sửa → chụp lại)

## 9. Test plan

- `gradle :app:assembleDebug` (nếu có SDK) / `P26SkyContractTest` + `P26SkyRenderTest` + test P26C mới trên đúng candidate SHA qua GitHub Actions `lyra-render-evidence.yml` / `main.yml`.
- Render test BEFORE+AFTER cùng camera/zoom/timeSec/hour/weather; ảnh PNG engine (Robolectric NATIVE), không contact sheet.
- Visual bookmarks: z0.88 gameplay, z1.48 gate, z0.36 overview, z1.0/z1.48 cloud-visible.

## 10. Evidence ledger fields

baseline SHA, candidate SHA, lệnh test, kết quả, CI conclusion, tên file screenshot, kích thước kiểm tra, giới hạn (NOT TESTED nếu không có máy thật).

## 11. Round budget & stop

Tối đa 3 vòng sửa đầy đủ mỗi ticket. Blocker còn lại → BLOCKED (honest delivery), không DONE. Sau commit/push/mở PR: DỪNG để owner duyệt, không tự merge.
