# ETERNAL SUMMONER — Quality Loop (hồ sơ vận hành)

Thư mục này là **cơ chế lưu bền trong repo** để các phiên Arena sau này tự vận hành vòng lặp
`baseline → implement → evidence → critique → fix → retest` mà không cần người dùng tạo agent con.
Skill điều phối nằm ở `.agents/skills/eternal-arena-quality-loop/SKILL.md`; 8 skill vai trò chuyên
môn nằm trong `.agents/skills/eternal-arena-*/SKILL.md`; bảng phân vai ở
[ARENA_SPECIALIST_TRAINING_MATRIX.md](ARENA_SPECIALIST_TRAINING_MATRIX.md); báo cáo cài đặt ở
[ARENA_BOOTSTRAP_REPORT_VI.md](ARENA_BOOTSTRAP_REPORT_VI.md).

Đây là **tài liệu quy trình offline**, không phải huấn luyện lại mô hình, không cài MCP/API trả phí,
và không chứng minh Arena có subagent chạy song song.

## 1. Cấu trúc thư mục

```
docs/quality-loop/
├── README.md                              ← file này (cách dùng, rubric, dry-run)
├── ARENA_SPECIALIST_TRAINING_MATRIX.md    ← 9 role skill + ví dụ ownership
├── ARENA_BOOTSTRAP_REPORT_VI.md           ← báo cáo cài đặt + 1 dry-run ticket (READ ONLY)
└── <TICKET>/
    ├── WORK_ORDER.md        ← VIẾT TRƯỚC khi sửa file bất kỳ (scope, acceptance, 1 writer/file)
    ├── QA_MATRIX.md         ← tuỳ chọn: bảng PASS/FAIL/WARN/NOT TESTED của QA critic
    ├── BLOCKERS.md          ← tuỳ chọn: tối đa 5 lỗi xếp theo severity + root cause
    ├── ROUND_1.md           ← 1 vòng: scope, rubric, blockers, fix, validation, perf, decision
    ├── ROUND_2.md           ← chỉ khi ROUND_1 FAIL (tối đa 3 vòng sửa đầy đủ)
    ├── ROUND_3.md
    └── FINAL_REPORT_VI.md   ← before/after người chơi, ownership, CI, hạn chế, PR chưa merge
```

Quy tắc đặt tên: `<TICKET>` viết thường không dấu, ví dụ `p24-roof-repair`, `mission02-escort`.
`ROUND_N.md` luôn cùng camera/seed/commands với vòng trước để so sánh được. Mỗi file chỉ có
**một writer** tại một thời điểm; file dùng chung (renderer, catalog, save schema, ViewModel,
`MainActivity`) do Director giữ và tích hợp nối tiếp.

## 2. Rating matrix

Hai lớp, chốt **trước** khi xem candidate (xem template trong quality-loop SKILL.md):

| Lớp | Tiêu chí | Ngưỡng | Kết quả |
| --- | --- | --- | --- |
| Blocker (nhị phân) | Android build, startup, navigation, collision, door/trigger, command effects, save/restart, mission conclusion, no regression | một FAIL chặn `DONE` | `PASS` / `FAIL` |
| Chất lượng mềm (0–4) | visual coherence, technical correctness, player clarity, tactical value, polish, efficiency | mỗi chiều ≥ 3/4 và tổng ≥ 20/24 **chỉ khi có bằng chứng** | 0–4 + artifact |

Nhãn bằng chứng bắt buộc, không trộn lẫn:

| Nhãn | Ý nghĩa |
| --- | --- |
| `ART CONTACT SHEET` | ảnh offline/contact sheet — **không** phải engine render, không được dùng để PASS thị giác |
| `REAL ENGINE ROBOLECTRIC RENDER` | PNG từ test renderer thật (Compose/Robolectric) kèm `camX/camY/zoom/timeSec` |
| `EMULATOR` / `PHYSICAL DEVICE` | chạy máy thật; chỉ ở đây mới được nói FPS/nhiệt |
| `LOCAL TEST` / `CI` | unit test / Gradle / GitHub Actions trên đúng SHA |
| `NOT VERIFIED` | thiếu engine screenshot/hardware → **không** tự chấm PASS thị giác |
| `NOT TESTED` / `NOT MEASURED` | không có thiết bị/số liệu; không ghi 0, không bịa FPS |

Điểm do agent tự chấm chỉ là review chủ quan, không phải chứng nhận độc lập. Test xanh ≠ hình đẹp hay
gameplay hay. Với đồ họa: capture zoom `0.36 / 0.88 / 1.00 / 1.48`, có nhân vật trước/sau cửa hoặc
dưới tán cây. Với gameplay: trace `input → state → reward/save` cho `success / partial / fail / retry /
restart`. `ScenePerformanceProbe` đo CPU submission/frame pacing, **không** phải GPU completion hay nhiệt.

## 3. Tái lập baseline (chạy thật, ghi output vào evidence ledger)

```bash
# Trạng thái repo & PR thật (refresh, không tin mô tả trong prompt cũ)
git status --porcelain; git rev-parse HEAD; git branch --show-current
gh pr list --state all --limit 20 --json number,title,state,isDraft,headRefName
gh pr view <n> --json files --jq '.files[].path'      # kiểm tra giao thoa file

# Đọc baseline bắt buộc
sed -n '1,200p' AGENTS.md
sed -n '1,80p'  .agents/skills/eternal-summoner-skill-router/SKILL.md
sed -n '1,80p'  docs/GAME_SKILL_SELECTION.md
sed -n '1,60p'  .github/workflows/main.yml

# Build & test (chỉ khi có JDK 21 + Android SDK; thiếu thì ghi LOCAL BUILD NOT RUN)
gradle :app:assembleDebug
gradle :app:testDebugUnitTest --tests '*LostCourierPersistenceRobolectricTest*'
gradle :app:testDebugUnitTest --tests '*MapForgeP23RenderTest*'   # engine render evidence

# CI thật trên đúng candidate SHA (không giả mạo)
gh run list --commit <candidate_sha> --json databaseId,workflowName,status,conclusion,url
```

Camera cho ảnh trước/sau: dùng giá trị cố định đã lưu trong
`docs/visual-evidence/p23/camera_props.json` (`camX`, `camY`, `zoom`, `timeSec`, danh sách props) và
giữ nguyên giữa baseline/candidate; ví dụ `sanctuary_overview_z036.png` có `camX=1600`, `camY=1380`,
`zoom=0.36`, `timeSec=0`. Render test `app/src/test/java/com/example/MapForgeP23RenderTest.kt` dùng
các zoom `0.36f / 0.88f / 1.48f`. Workflow CI: `.github/workflows/main.yml` chạy
`gradle :app:assembleDebug` cho mọi PR vào `main`; `.github/workflows/lyra-render-evidence.yml` chỉ
chạy khi diff chạm `app/src/main/**` hoặc các render test (PR chỉ sửa docs/skill sẽ **không** có ảnh
render mới — phải ghi rõ điều đó, không tự nhận có ảnh).

Evidence ledger tối thiểu cho mỗi PASS/FAIL: `baseline SHA`, `candidate SHA`, lệnh chính xác, output,
tên file screenshot, kích thước/zoom đã kiểm tra, giới hạn.

## 4. Vì sao `BLOCKED` là kết quả hợp lệ

Vòng lặp có trần **3 vòng sửa đầy đủ**. Nếu sau 3 vòng còn blocker (crash, softlock, cửa lệch
trigger, thưởng không lưu, thiếu render engine, thiếu máy đo), verdict đúng là `BLOCKED`: nêu blocker
đo được, repro, severity, root cause và next fix; mở PR dạng **Draft** kèm hạn chế; dừng, không tuyên
bố `DONE`. `BLOCKED` trung thực có giá trị hơn `PASS` giả: nó bảo vệ camera/anchor/save của chủ sở hữu,
tránh lặp vô hạn và tránh đốt tài nguyên. Không được: tự nhận CI xanh khi chỉ push, tự nhận FPS/nhiệt
khi không có máy, bịa điểm QA của provider, nhận có N agent song song khi chỉ chạy N logical pass
trong một agent, hay tự merge PR. Một `WARN`/`NOT VERIFIED` cũng không được nâng thành `PASS`.

## 5. Dry-run ví dụ: `DRYRUN-01` (READ ONLY, không sửa game)

Ticket giả lập để chứng minh cách dùng rubric **không tạo ra kết quả giả**. Baseline: `main` @
`a5c1b5b56541d11cf5fad02497d342d94706f714` (checkout hiện tại). Phạm vi: **chỉ đọc** chuỗi lệnh
Master `Focus / Regroup / Retreat` → AI → lưu kết quả. Không ghi file game, không chạy Gradle (sandbox
không có `java`/`gradle`/`ANDROID_HOME`), không tạo PNG nào, không đọc ý kiến người dùng.

| Tiêu chí | Ngưỡng | Bằng chứng đọc được trong repo | Kết quả |
| --- | --- | --- | --- |
| Lệnh tồn tại thật | `FOCUS_FIRE`, `REGROUP`, `RETREAT` có định nghĩa | `app/src/main/java/com/example/data/model/FirestoreModels.kt:174–188` (`enum class TacticalOrder`) | `PASS` (tĩnh) |
| Lệnh đổi state | `issueTacticalOrder()` gán `activeOrder` và xử lý riêng từng lệnh | `game/combat/TowerCombatEngine.kt:276–334` (FOCUS_FIRE chọn `designatedTargetId` trong `livingEnemies`; RETREAT/REGROUP có nhánh riêng) | `PASS` (tĩnh) |
| AI tiêu thụ lệnh | RETREAT/REGROUP đổi vận động, không bị tick tự động ghi đè ngay | `game/ai/AutonomousHeroAi.kt:101–120` (`safeX = masterX - 110f`, `formationSlotAround`); `:55–58` huỷ `contactAttack` khi RETREAT | `PASS` (tĩnh) |
| Retreat có điều kiện kết thúc thật | không "bấm là thắng" | `TowerCombatEngine.kt:637–655`: chỉ `confirmTacticalRetreat()` khi toàn bộ hero sống về trong bán kính `185f` quanh spawn | `PASS` (tĩnh) |
| Lưu thưởng một lần / restart | flag bền, retry không cộng thưởng hai lần | `data/LostCourierProgression.kt` (`isFirstRescue`, `outcomeFlags`), `data/local/LocalGuestSaveManager.kt:1076–1171` (`retreatUsed`), test `app/src/test/java/com/example/data/LostCourierPersistenceRobolectricTest.kt` (5 `@Test`, gồm restart & atomic write retry) | `PASS` (tĩnh: test tồn tại, **chưa chạy**) |
| Lệnh thay đổi state khi chạy thật | cần input/state trace | không có trace mới; sandbox thiếu toolchain | `NOT VERIFIED` |
| Ảnh engine trước/sau 0.36/0.88/1.00/1.48 | `REAL ENGINE ROBOLECTRIC RENDER` | không tạo/inspect PNG nào ở dry-run này; `docs/visual-evidence/p23/engine-render/` là artifact có sẵn của P23, **không** phải kết quả của ticket này | `NOT VERIFIED` |
| FPS/nhiệt thiết bị | `PHYSICAL DEVICE` | không có máy | `NOT TESTED` |
| 5–10 phút chơi người thật | playthrough người chơi | không có; simulation ≠ wall time người chơi | `NOT TESTED` |

**Verdict: `BLOCKED`** — cấu trúc code đạt ở mức đọc tĩnh, nhưng mọi tiêu chí nghiệm thu cần runtime
đều `NOT VERIFIED/NOT TESTED`. Đây chính là kết quả trung thực: không nâng `PASS`, không bịa ảnh, không
bịa CI. CI có thật đã quan sát được bằng `gh` là của **baseline**, không phải của dry-run:
`main@a5c1b5b` → run `41499813922` `success`; đầu PR #28 `7bdd1c9` → run `41544579176` `success`.
Bước tiếp theo nếu ticket thành thật: chạy `gradle :app:assembleDebug` và các test lệnh trên CI
(`RescueTacticalCommandsTest.kt` hiện thuộc PR #28, **chưa có trên `main`**), capture engine render
cùng camera, rồi mới xét `PASS`. Dry-run này dừng ở đây, không mở PR
riêng, không đụng vào 29 file của PR #28.
