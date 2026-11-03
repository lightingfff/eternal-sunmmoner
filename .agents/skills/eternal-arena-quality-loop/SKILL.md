---
name: eternal-arena-quality-loop
description: Persistent Arena orchestration and iterative quality loop for ETERNAL SUMMONER; trigger whenever Arena is asked to improve gameplay, art, QA or performance; routes to specialist skills, enforces file ownership and real evidence, and runs targeted corrections up to three rounds.
---

# TRAINING 00 — ARENA AUTONOMOUS GAME STUDIO (SKILL ROUTER + QA LOOP)

## What this actually trains
These files are **procedural skills, not neural model fine-tuning**. Treat them as actionable operating manuals for one Arena Argent session. If Arena exposes native subagent tools, use them with explicit role/file isolation. Otherwise role-switch sequentially; do not invent parallel execution, agent IDs, separate thinking or independent QA model. The user does not have to create separate new agent profiles for each role.

## Required startup
1. Read root `AGENTS.md`, `.agents/skills/eternal-summoner-skill-router/SKILL.md`, `docs/GAME_SKILL_SELECTION.md` and repo/PR status.
2. Read this skill plus `eternal-arena-studio-director/SKILL.md`.
3. Pick ONLY specialist skills needed; normally 2–4 role skills per ticket, plus 1–3 relevant existing reference source skills (no need to preload all 174 Scenario guidance files).
4. Announce selected roles/source skill names and actual native-vs-logical agent capability truthfully.
5. Preserve source, gameplay, saves, art/style and map/camera contracts. Working code matters more than generated documents.

## Dynamic role routing
| User asks about | Lead specialist | Reviewer | Optional |
|---|---|---|---|
| Floor 1 mission, quests, story | `eternal-arena-tower-director` | `eternal-arena-visual-gameplay-qa` | gameplay-engineer |
| Master orders, combat, enemy AI | `eternal-arena-gameplay-engineer` | QA | android-performance |
| Roof gaps, facades, doors, map art | `eternal-arena-environment-architect` | QA | technical-artist only after geometry correct |
| Shadow, lighting, spells, night atmosphere | `eternal-arena-technical-artist` | QA | android-performance |
| Stutter, overheating, low FPS | `eternal-arena-android-performance` | integration-reviewer | technical-artist |
| Release, tests, PR integration | `eternal-arena-integration-reviewer` | director | any relevant role |

## Standard round trip
`DISCOVER → REPRODUCE/BASELINE → WORK_ORDER + FILE OWNERS → IMPLEMENT ONE WORKABLE SLICE → EVIDENCE → STRICT QA → ROOT CAUSE/FIX → RETEST → FINAL REVIEW → DRAFT PR IF NEEDED`.

A red criterion must lead to a real code/art modification and a repeated check, not a new enthusiastic summary. Max three revision cycles per ticket. If a blocker cannot be resolved in this environment or after the cap, label `BLOCKED`, with exact evidence and future remediation; do not invent PASS. A PR can be Draft while incomplete. When code/test work can continue, work through the allocated iteration rather than asking owner for each small step.

## Critical evidence
Visual: real Compose/Robolectric engine screenshot for zoom 0.36/0.88/1.00/1.48, camera parameters, before/after, visible actor at door/canopy when applicable; contact sheet is not a runtime render. Gameplay: actual event/state/input/action and reward/save traces for success, partial, fail/retry and restart; automated simulation duration is not measured human session length. Technical: Gradle and CI results only on tested SHA; perf metrics correctly labeled CPU vs GPU/device; no claimed physical FPS/thermal without device.

## Non-negotiable contracts
Kotlin + Android Compose Canvas; `OBLIQUE_Y_SCALE=0.72f`; Sanctuary layout, world anchors, depth ordering, entrances, bitmap cached sprites, 2.5D shadow light direction, offline guest state and user work retained. No forced rewrite to Unity/Godot. No force push/auto-merge. No blanket use of image-API/paid tools inferred from a Scenario skill.

## Minimum written trail
`docs/quality-loop/<TICKET>/WORK_ORDER.md` before writes; `ROUND_N.md` for each QA cycle; `FINAL_REPORT_VI.md` and PR for owner. Include observed failures instead of overclaiming.

## Role checks
Director owns shared files and integration. Artists own designated art/generator/palette/asset atlas changes. Gameplay engineer owns command/AI/combat modules. Tower designer owns mission-specific rules. Performance engineer owns bounded optimization if actually needed. QA normally does not edit the component it critiques. Integration reviewer is last mandatory gate. Parallelize ONLY independent workloads with isolated file owners or worktrees; serialization is normal, not a failure.

---

# PHỤ LỤC V1 — QUY TRÌNH ĐẦY ĐỦ (BOOTSTRAP SPEC, MỤC 0–9)

> Phần này giữ lại các quy định nghiêm ngặt hơn của bootstrap v1. Nó là **tài liệu vận hành offline**, không phải huấn luyện trọng số mô hình, không cài MCP/API trả phí. Ngôn ngữ vận hành: Tiếng Việt. Toàn bộ nội dung tự chứa trong repo; không cần đọc lại chat.

## 0. Phát hiện năng lực thật, không mô phỏng bằng lời
- Trước khi phân việc, liệt kê tool thật của phiên. Chỉ kết luận "có subagent song song" khi tồn tại **tool native spawn/delegate đã chạy thành công** với output/ID kiểm chứng được; khi đó giao việc độc lập kèm ranh giới file, ghi rõ tên/ID đơn vị đã khởi tạo, và vẫn chạy tuần tự mọi tác vụ cùng file hoặc cùng Gradle output.
- Nếu KHÔNG có (trường hợp mặc định): vận hành **MỘT agent** với các checkpoint chuyên môn theo thứ tự `Director → Engineer/Artist → Critic (independent-style) → QA → Perf auditor → Director`. Báo trung thực "logical roles within one agent"; không được ghi "4 agent/model chạy song song". Tool hỗ trợ không xung đột (đọc file, `git`, `gh`) vẫn có thể gọi đồng thời.
- KHÔNG dùng `.codex/agents/*.toml` (PR #27) làm bằng chứng Arena có thể spawn Codex subagent; không sửa/không bắt `.codex` chạy. `.agents/skills/codex-tower-rescue/SKILL.md` chỉ là tài liệu tham khảo được đọc.
- Không gọi dịch vụ Scenario trả phí, không lấy model, không thêm plugin/API key. SKILL.md không bảo đảm Scenario MCP.

## 1. Hiểu trạng thái repo, giữ an toàn
1. Đọc `AGENTS.md`, `.agents/skills/eternal-summoner-skill-router/SKILL.md`, `docs/GAME_SKILL_SELECTION.md`, `.github/workflows/main.yml`, `.github/workflows/lyra-render-evidence.yml` trước khi ghi file bất kỳ; chạy `git status`, `git rev-parse HEAD`, `git branch --show-current`, `gh pr list --state all`.
2. Thông tin PR trong bất kỳ prompt nào chỉ là baseline ban đầu; PHẢI refresh trạng thái thật tại thời điểm chạy (`gh pr list`, `gh pr view <n> --json state,isDraft,headRefName,files`). Kiểm tra giao thoa file với PR đang mở trước khi sửa.
3. Nếu nhánh phiên đã merge/đóng bị khóa: không push/cherry-pick bừa; báo owner mở chat Agent Mode mới trỏ `main`. Một phiên Arena thường chỉ tạo một PR.
4. Cấm: `git clean`, `git reset --hard`, force push, revert, auto-merge, xóa ảnh/tệp người dùng, ghi đè skill sẵn có. Chỉ mở PR, KHÔNG merge (owner duyệt).
5. Bất di bất dịch: `ObliqueCamera.OBLIQUE_Y_SCALE = 0.72f` (`app/src/main/java/com/example/game/map/WorldMapModels.kt`), island outline, đường sá, plaza, bãi tập, footprints, anchors, entrances, collision, save schema, depth sorting, hero/canopy occlusion, pixel density, `FilterQuality.None`, tương tác độc lập của công trình. Không đổi engine, không biến đảo thành background PNG, không tạo Master avatar trên map, không chuyển Unity/Godot/Unreal/WebGL.
6. Test xanh ≠ hình đẹp hay gameplay hay. Không tự nhận FPS/nhiệt từ Robolectric.

## 2. Cơ chế phân chia công việc (WORK_ORDER trước khi sửa file)
Trước mọi thay đổi tạo `docs/quality-loop/<TICKET>/WORK_ORDER.md` với: ISSUE ID; mục tiêu người chơi thật; baseline (SHA/code/ảnh); 3–8 tiêu chí nghiệm thu đo được; nguy cơ regression (camera, anchors, pixel art, local saves, performance); hạng mục song song vs điểm giao phải nối tiếp; **ownership FILE-LEVEL — mỗi file đúng một writer tại một thời điểm**; tests/render snapshot/manual-device caveats; điều kiện kết thúc, số vòng, giới hạn an toàn; rollback; no-go files.
Vai trò (chọn, không cần đủ cả bộ): DIRECTOR (scope/acceptance), GAMEPLAY ENGINEER (combat/AI/lệnh Master/state machine), ENVIRONMENT/TECHNICAL ARTIST (assets, roof/wall/door/ground, lighting, VFX, giữ 2.5D), ANDROID PERFORMANCE ENGINEER (culling/cache/CPU draw/GC, không thêm runtime pass vô căn cứ), VISUAL QA/PLAYTEST CRITIC (tìm 5 lỗi dễ thấy nhất ở zoom gameplay, tách defect khỏi sở thích), REGRESSION TESTER (CI, save/retry, occlusion, fallback), INTEGRATION REVIEWER (so diff, không đổi ngoài scope). Ví dụ: nâng công trình → Artist làm khối/chất liệu, Visual QA đối chiếu ảnh engine, Integrator duyệt anchor + GPU budget; combat → Engineer làm state/AI, Reviewer đọc telemetry/playthrough, Artist chỉ sửa art khi nằm trong scope đã duyệt. Hai chuyên gia cần cùng một file → Director là writer duy nhất, tích hợp nối tiếp.

## 3. Vòng lặp trắng-đen (bắt buộc cho PR ảnh hưởng gameplay/đồ họa)
`[BASELINE] → [DESIGN RUBRIC] → [SMALL IMPLEMENTATION] → [REAL EVIDENCE] → [QA VERDICT]`; FAIL → `[ROOT CAUSE + PRIORITIZED FIX] → [RETEST] → [QUALITY VERDICT]`; PASS → `[INTEGRATION REVIEW] → [CI] → [PR + LIMITATIONS]`.
- BASELINE đọc code/điểm yếu thật; với game chạy flow người chơi hoặc simulation có state thật; với art render đúng renderer và camera. Contact sheet KHÔNG thay engine render.
- DESIGN RUBRIC chốt TRƯỚC khi xem candidate: blocker nhị phân PASS/FAIL (build, startup, navigation, collision, doors, command effects, save/restart, mission conclusion, no regression) + 6 chiều mềm 0–4 (visual coherence, technical correctness, player clarity, tactical value, polish, efficiency): mọi blocker = 0, không chiều nào < 3, tổng ≥ 20/24 **chỉ khi có bằng chứng**. Điểm tự chấm là review chủ quan; không có screenshot/hardware → `NOT VERIFIED`, không tự PASS.
- SMALL IMPLEMENTATION: một lát cắt (một nhóm công trình / một encounter / một hiệu ứng).
- REAL EVIDENCE: `gradle :app:assembleDebug` + test liên quan nếu có JDK 21/Android SDK; nếu sandbox thiếu → ghi `LOCAL BUILD NOT RUN`, push PR và đọc trạng thái GitHub Actions thật trên đúng candidate SHA (`gh run list --commit <sha>`), không bịa. Ảnh render từ Robolectric/engine thật, trước/sau cùng camera; contact sheet offline gắn nhãn riêng. Gameplay cần log/counter state + input thật; 55 giây simulation ≠ 5–10 phút chơi người thật.
- QA VERDICT: pass/fail/warn từng tiêu chí kèm artifact/đường dẫn + triệu chứng; chủ động tìm: cửa như sticker, mái hở/cắt mái, z-sorting sai, viền alpha, UI che gameplay, vật thể trôi, shader cháy sáng, frame spikes, no-op master commands, NPC softlock, thưởng không lưu.
- ROOT CAUSE + FIX: định vị (asset, sprite anchor, canvas projection, world trigger, caching, state machine, save path), sửa tập trung tối đa 1–3 nguyên nhân lớn; không tô bloom che phối cảnh sai, không kéo thời lượng bằng timer giả/HP sponge.
- RETEST: cùng camera/seed/commands, cập nhật QA. **Tối đa 3 vòng sửa đầy đủ** mỗi ticket; còn blocker → dừng, báo đúng blocker + next fix, KHÔNG tuyên bố DONE, không lặp vô hạn. Xong sớm thì dừng.
- EVIDENCE LEDGER cho mọi pass/fail: baseline ref, commit/asset SHA mới, lệnh test, kết quả, tên file screenshot, kích thước kiểm tra, giới hạn. Không giả mạo CI/render/thử máy/ý kiến người dùng.

## 4. Rubric đồ họa 2.5D
Capture scene tại zoom **0.36 / 0.88 / 1.00 / 1.48** (0.36 bố cục, 0.88 gameplay, 1.48 cấu tạo), có nhân vật đứng trước/sau cửa hoặc dưới tán cây khi phù hợp; lưu camera params cố định (`docs/visual-evidence/p23/camera_props.json` là mẫu). Kiểm tra: (a) liên kết mái–tường: trục sống, viền mái, soffit, cột/chân đỡ, đầu hồi, không khe trong suốt, mặt trước + return face đúng projection; (b) liên kết cửa: lõm thật với lintel/jamb/reveal, độ sáng đặt sau khung, khớp gameplay trigger, không nắn collider tùy tiện; (c) chân nhà–mặt đất: foundation/contact rim/world-anchor cùng mặt phẳng, bóng định hướng thật, không bóng bầu dục giả; (d) giá trị sáng dark fantasy: dải sáng mái/đá/cây/cỏ hợp nhau, ưu tiên portal/đèn, tránh trắng cháy/neon; (e) pixel density: alpha hợp lệ, nearest-neighbor foreground, không blur, không 1px noise rải; (f) độ sâu/hiệu năng: silhouette shadow có khối, occlusion đúng, không decode/shader allocation mỗi frame. Không kết luận "đã đẹp" từ PNG gốc: phải kiểm tra pipeline thật `SanctuaryWorldRenderer.drawSanctuaryFloatingWorld`, `PixelSpriteRenderer.drawProp`, shadow và character.

## 5. Rubric gameplay / chơi thật
Objective hiểu trong 10–20s và nhìn được tại gameplay zoom; Master điều khiển ngoài map (focus/regroup/retreat...) với tác động kiểm chứng, AI không giật cục; ≥2 kiểu AI kẻ địch có telegraph, không teleport/hit vô lý; hệ quả cứu hộ + thưởng giữ sau thoát game/restart, failure có retry không softlock; phút chơi tăng bằng nhịp khám phá/tác động chiến thuật/biến cố, KHÔNG bằng HP sponge/spawn tràn/timer giả; playthrough thật ghi state flags, input/command trace, reward/save, clear/partial/fail, phân biệt simulation vs on-device; UI nhỏ dễ đọc, không đè màn chiến đấu; không làm Sanctuary chậm đi vì Tower.

## 6. Performance / Android discipline
Đọc `app/src/main/java/com/example/performance/ScenePerformance.kt`, `ScenePerformanceOverlay.kt`, `SceneFrameLoop.kt` và frame loop trước khi sửa. `ScenePerformanceProbe` phản ánh CPU submission/frame pacing, KHÔNG phải GPU completion hay nhiệt. Kiểm tra bitmap caches, sprite/shadow silhouette path, culling, object/gradient allocations, draw count, GC, `DepthPresentation.Quality {OFF, REDUCED, BALANCED}`. Chỉ đưa số liệu khi có source output thật kèm device/test context, gắn nhãn `LOCAL TEST` / `Robolectric` / `CI` / `PHYSICAL DEVICE`; không số liệu → `GPU/FPS/thermal NOT TESTED`. Ngân sách 33.3ms@30Hz / 16.7ms@60Hz là toán học, không phải FPS đo được.

## 7. Chọn skill thông minh (đường dẫn đã xác minh)
Bắt đầu `eternal-summoner-skill-router`, rồi 1–3 role skill + 1–3 nguồn tham khảo có thật; không nạp 174 skill cùng lúc. Sprite: `.agents/skills/ai-game-spritesheets/SKILL.md`. Textures/visual: `docs/skill-library/scenario/upstream/skills/scenario-textures/SKILL.md`, `.../scenario-game-assets/SKILL.md`, `.../scenario-refine-loop/SKILL.md`. Art QA: `.../scenario-refine-loop/SKILL.md` + tùy khả năng offline `.../scenario-asset-analysis/SKILL.md`. Android perf: chỉ nguyên tắc từ `docs/skill-library/scenario/upstream/skills/game-engines/unity/scenario-unity-performance/SKILL.md` và `.../scenario-unity-mobile/SKILL.md`, KHÔNG chạy code Unity. Gameplay: `.agents/skills/codex-tower-rescue/SKILL.md` là tham khảo đọc được, không bắt Codex thực thi. Nếu file không tồn tại → báo rõ và chọn fallback; đừng tự bịa tên file. Không gọi provider/API trả phí.

## 8. Forms of reporting
Mỗi vòng ghi `docs/quality-loop/<TICKET>/ROUND_<N>.md` (mẫu ở dưới) gồm: **Scope** / commit / owner role / changed files; **Rubric** (bảng criterion, threshold, evidence, pass/fail); **Critical blockers** (measured, repro, severity, root cause); **Fix and delta** (path + reason); **Validation** (lệnh chính xác, output, CI links, ảnh gắn nhãn); **Perf** (đo ở đâu, `NOT TESTED` nếu thiếu); **Next round** (chỉ khi fail, max 3); **Decision** `PASS (eligible review)` hoặc `BLOCKED (honest delivery)`. `FINAL_REPORT_VI.md`: before/after người chơi thấy được, code ownership, CI status, trước/sau render, điểm yếu còn lại thật. Giữ evidence gọn; không commit binary test output vô giá trị; không nhận có 4-agent team nếu chỉ chạy 4 logical pass trong một agent.

## 9. Giới hạn phiên / dừng đúng chỗ
Nếu nhánh đã merge/PR đóng và GitHub write bị khóa: không push lặp lại, không yêu cầu owner reset repo; báo owner mở chat Agent Mode mới trên `main` và dán bootstrap. Mỗi chat thường chỉ một PR. Sau khi commit/push/mở PR: **DỪNG** để owner duyệt, không tự merge, không khởi động mission khác trước khi kiểm tra ownership/PR đang mở.

---

# CHECKLIST TÁI SỬ DỤNG (đánh dấu từng dòng, không bỏ bước)

```
[ ] 0  Tool thật đã liệt kê; subagent native: CÓ (ghi ID/output) / KHÔNG (ghi "logical roles, 1 agent").
[ ] 0  Không viện dẫn .codex/agents/*.toml; không gọi API/plugin trả phí.
[ ] 1  Đã đọc AGENTS.md, skill-router, GAME_SKILL_SELECTION.md, 2 workflow CI.
[ ] 1  git status / HEAD / branch / gh pr list --state all đã refresh; giao thoa file PR đã kiểm tra.
[ ] 1  Không clean/reset --hard/force push/revert/auto-merge; không ghi đè skill cũ.
[ ] 1  Hằng số 0.72f, anchors, save schema, depth sort, FilterQuality.None nằm trong no-go.
[ ] 2  WORK_ORDER.md tồn tại TRƯỚC khi sửa file; mỗi file có đúng 1 writer; có rollback + no-go.
[ ] 3  Rubric chốt trước implementation; blocker nhị phân + 6 chiều 0–4, ngưỡng 3/4 & 20/24.
[ ] 3  BASELINE thật (code + engine render/state), không dùng contact sheet thay render.
[ ] 3  Implementation là một lát cắt nhỏ, đúng scope.
[ ] 3  Evidence: lệnh build/test chính xác + kết quả thật (hoặc LOCAL BUILD NOT RUN + CI url trên SHA).
[ ] 3  QA chủ động tìm ≥5 lỗi; mỗi FAIL có repro/artifact/severity/root cause.
[ ] 3  Fix tập trung 1–3 nguyên nhân; RETEST cùng camera/seed/commands; ledger cập nhật.
[ ] 3  Số vòng ≤ 3; blocker còn lại → BLOCKED, không DONE.
[ ] 4  Với art: đủ zoom 0.36/0.88/1.00/1.48, trước/sau cùng camera, có actor ở cửa/tán.
[ ] 5  Với gameplay: trace input→state→reward/save cho success/partial/fail/retry/restart.
[ ] 6  Perf ghi rõ CPU vs GPU; không device → NOT TESTED.
[ ] 7  Chỉ 1–3 skill chuyên môn + router; path tồn tại thật.
[ ] 8  ROUND_N.md + FINAL_REPORT_VI.md có evidence ledger và giới hạn.
[ ] 9  PR đã mở (Draft nếu chưa đạt), CHƯA merge; owner có cách kiểm tra; dừng.
```

---

# TEMPLATE: `docs/quality-loop/<TICKET>/WORK_ORDER.md`

```
# WORK_ORDER — <TICKET>
- Ngày / baseline SHA / branch / PR liên quan (refresh live):
- Mục tiêu người chơi thật (1 đoạn):
- Current behavior (đọc code tại path:line):
- User-visible win sau khi xong:
- Acceptance (3–8 tiêu chí đo được, blocker nhị phân + 6 chiều 0–4, ngưỡng 3/24):
  1. ... (cách đo, lệnh, artifact)
- Regression risks: camera 0.72 / anchors / pixel art / local saves / performance / UI.
- Files MAY change (mỗi file = 1 writer):
  | path | writer role | nối tiếp sau |
- Files MUST NOT change: game engine constants, WorldObstacle layout, save schema,
  assets người dùng, .codex, PR đang mở của owner.
- Song song được / phải nối tiếp (cùng file, cùng Gradle output, cùng render test):
- Selected skills: router + 1–3 role + 1–3 upstream (path thật).
- Test plan: gradle :app:assembleDebug / test names / render test + zoom / CI workflow.
- Visual & playthrough bookmarks: scene, camX/camY/zoom/timeSec, seed, input trace.
- Evidence ledger fields: baseline ref, candidate SHA, lệnh, output, screenshot names, limitations.
- Round budget: tối đa 3 vòng sửa; điều kiện dừng; rollback; PR policy: mở, KHÔNG merge.
```

# TEMPLATE: `docs/quality-loop/<TICKET>/ROUND_<N>.md`

```
# ROUND_<N> — <TICKET>
Scope / commit (before → after) / owner role / changed files
Rubric:
| Criterion | Threshold | Evidence (path:line / artifact / SHA) | PASS/FAIL/WARN/NOT VERIFIED |
Critical blockers (measured, repro, severity, root cause hypothesis):
Fix and delta (path + lý do; tối đa 1–3 nguyên nhân):
Validation (lệnh chính xác + output thật; CI url trên đúng SHA; nhãn ảnh:
  ART CONTACT SHEET / REAL ENGINE ROBOLECTRIC RENDER / EMULATOR / PHYSICAL DEVICE):
Perf (đo ở đâu, CPU vs GPU, hoặc NOT TESTED):
Next round (chỉ khi FAIL; N ≤ 3):
Decision: PASS (eligible review) | BLOCKED (honest delivery)
```

# TEMPLATE: `docs/quality-loop/<TICKET>/FINAL_REPORT_VI.md`

```
# FINAL_REPORT_VI — <TICKET>
Before/after người chơi thấy được; scope thực tế; code ownership từng file;
Evidence ledger (baseline SHA, candidate SHA, lệnh, kết quả, CI conclusion, ảnh trước/sau);
Rubric tổng (blocker + 6 chiều, chỉ chấm khi có bằng chứng);
CI status thật (workflow url + conclusion trên candidate SHA) hoặc LOCAL BUILD NOT RUN;
Hardware/perf: NOT TESTED nếu không có máy thật;
Điểm yếu còn lại / BLOCKED blockers / next fix;
PR url (state OPEN/DRAFT), chưa merge; cách owner kiểm tra (lệnh + file + ảnh).
```

---

# ĐƯỜNG DẪN ĐÃ XÁC MINH (baseline `a5c1b5b`, phải re-verify khi chạy)

- `AGENTS.md`, `.agents/skills/eternal-summoner-skill-router/SKILL.md`, `docs/GAME_SKILL_SELECTION.md`
- `.agents/skills/eternal-arena-studio-director/SKILL.md` … `eternal-arena-integration-reviewer/SKILL.md` (8 role skills) và `docs/quality-loop/ARENA_SPECIALIST_TRAINING_MATRIX.md`
- `app/src/main/java/com/example/game/map/WorldMapModels.kt` (`ObliqueCamera`, `SanctuaryLight`)
- `app/src/main/java/com/example/game/pixel/{SanctuaryWorldRenderer,PixelSpriteRenderer,SanctuaryEnvironmentShadows,SanctuaryDarkFantasyShaders,DepthPresentation,SanctuaryArtAssets,PixelStructureAtlas}.kt`
- `app/src/main/java/com/example/game/combat/{TowerCombatEngine,LostCourierEncounter}.kt`, `game/ai/AutonomousHeroAi.kt`, `game/tower/TowerFloorCatalog.kt`
- `app/src/main/java/com/example/performance/{ScenePerformance,ScenePerformanceOverlay,SceneFrameLoop}.kt`
- `app/src/main/java/com/example/ui/SummonerViewModel.kt`, `MainActivity.kt`, `data/local/LocalGuestSaveManager.kt`
- `app/src/main/assets/premium_slice/`, `app/src/main/assets/sanctuary_p03/`
- `docs/art/P21_ARCHITECTURE_FACILITIES.md`, `docs/visual-evidence/p23/camera_props.json`
- `.github/workflows/main.yml` (build APK), `.github/workflows/lyra-render-evidence.yml` (render evidence, có path filter `app/src/main/**`)
