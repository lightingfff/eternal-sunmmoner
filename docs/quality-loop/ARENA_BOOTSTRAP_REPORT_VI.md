# BÁO CÁO BOOTSTRAP — ARENA AUTONOMOUS GAME STUDIO (VI)

Ngày: **2026-11-03 (UTC)**. Đây là báo cáo cài đặt **chỉ dẫn/skill**, không phải huấn luyện lại mô
hình, không thay đổi gameplay/map/art/renderer, không tự merge PR.

## 1. Repo đã kiểm tra (trước khi ghi file)

| Hạng mục | Giá trị quan sát được |
| --- | --- |
| Branch phiên | `arena/56e30acd-eternal-sunmmoner` (active, không bị khóa). Đây chính là feature branch tách từ `main@a5c1b5b` của phiên Arena; cơ chế Arena cố định tên nhánh này nên **không tạo nhánh khác**, vẫn đúng yêu cầu "tạo feature branch từ main, một phiên một PR". |
| HEAD / `origin/main` | `a5c1b5b56541d11cf5fad02497d342d94706f714` (đã `git fetch origin main`; hai ref trùng nhau) |
| `git status` trước | sạch |
| File bắt buộc đã đọc | `AGENTS.md`, `.agents/skills/eternal-summoner-skill-router/SKILL.md`, `docs/GAME_SKILL_SELECTION.md`, `.github/workflows/main.yml`, `.github/workflows/lyra-render-evidence.yml` |
| Skill đã có | `ai-game-spritesheets`, `codex-eternal-director`, `codex-tower-rescue`, `eternal-summoner-skill-router`. **Chưa có** `docs/quality-loop/` hay bất kỳ `.agents/skills/eternal-arena-*` nào → không phải hợp nhất/ghi đè công việc cũ; riêng `eternal-arena-quality-loop/SKILL.md` được gộp v1+v2 thành một file duy nhất theo đúng yêu cầu "không tạo skill trùng tên". |
| Hằng số engine đã xác minh | `ObliqueCamera.OBLIQUE_Y_SCALE = 0.72f` tại `app/src/main/java/com/example/game/map/WorldMapModels.kt:14`; `SanctuaryLight` tại `:278`; `DepthPresentation.Quality{OFF,REDUCED,BALANCED}` tại `game/pixel/DepthPresentation.kt:43`; `FilterQuality.None` tại `PixelSpriteRenderer.kt:348` |
| Pipeline render đã xác minh | `SanctuaryWorldRenderer.drawSanctuaryFloatingWorld` (`SanctuaryWorldRenderer.kt:43`, gọi từ `PixelSpriteRenderer.kt:90`), `PixelSpriteRenderer.drawProp` (`:613`), `ScenePerformanceProbe` (`performance/ScenePerformance.kt:126`) |
| Toolchain local | `java`: không có; `gradle`: không có; `ANDROID_HOME`/`ANDROID_SDK_ROOT`: unset → **không chạy `:app:assembleDebug` local**, ghi `LOCAL BUILD NOT RUN` |
| CI thật | `.github/workflows/main.yml` chạy `gradle :app:assembleDebug` cho PR vào `main`; `lyra-render-evidence.yml` có path filter `app/src/main/**` → PR chỉ docs/skill sẽ không sinh ảnh render mới |

## 2. Baseline PR — refresh live bằng `gh` (không dùng mô tả cũ trong prompt)

| PR | Trạng thái | Head | Ghi chú |
| --- | --- | --- | --- |
| #28 | **OPEN, Draft** | `mission01/floor1-rescue` | "Mission 01 (draft): Floor 1 rescue tactical gameplay + persistence", 29 file, 2 commit, head `7bdd1c9…`. Đây là PR gameplay đang hoạt động. |
| #27 | MERGED (2026-11-02) | `arena/2230f477-eternal-sunmmoner` | "install Codex multi-agent config and skills" → nguồn của `.codex/agents/*.toml` và 2 skill `codex-*`. **Không phải** subagent native của Arena. |
| #26 | MERGED (2026-11-02) | `arena/c67c3cc2-eternal-sunmmoner` | P23 architecture 2.5D repair = commit HEAD hiện tại. |
| #13 | OPEN | `fix/apk-artifact-integrity` | PR cũ về APK artifact. |

Kiểm tra giao thoa: `gh pr view 28 --json files` cho 29 đường dẫn đều nằm trong `app/src/`,
`.github/workflows/mission01-rescue.yml`, `docs/MISSION01_RESCUE.md`, `docs/visual-evidence/mission01/`.
**Không** trùng với 13 file cài đặt ở mục 4. CI nền đã quan sát thật: `main@a5c1b5b` → run
`41499813922` `success`; head PR #28 `7bdd1c9` → run `41544579176` `success`. Đây là CI của
**baseline**, không phải kết quả của PR cài đặt này.

## 3. Dry-run QA ticket `DRYRUN-01` (READ ONLY)

Một ticket duy nhất, đọc code trên `main@a5c1b5b`, **không sửa game, không tạo PNG, không chạy
Gradle**: truy vết `TacticalOrder.FOCUS_FIRE/REGROUP/RETREAT` → `TowerCombatEngine.issueTacticalOrder`
→ `AutonomousHeroAi` → `LostCourierProgression` / `LocalGuestSaveManager`. Bảng 9 tiêu chí + verdict
nằm ở [README.md §5](README.md). Kết quả trung thực: 5 tiêu chí `PASS (tĩnh)`, 4 tiêu chí
`NOT VERIFIED / NOT TESTED`, verdict **`BLOCKED`** — minh hoạ rằng thiếu runtime evidence thì không
được nâng lên `PASS`. Không có kết quả PNG nào được tạo hay bịa trong dry-run này.

## 4. File đã tạo / sửa

**V2 (10 block `BEGIN_FILE`, giữ nguyên nội dung + YAML frontmatter):**

```
.agents/skills/eternal-arena-studio-director/SKILL.md
.agents/skills/eternal-arena-gameplay-engineer/SKILL.md
.agents/skills/eternal-arena-tower-director/SKILL.md
.agents/skills/eternal-arena-environment-architect/SKILL.md
.agents/skills/eternal-arena-technical-artist/SKILL.md
.agents/skills/eternal-arena-android-performance/SKILL.md
.agents/skills/eternal-arena-visual-gameplay-qa/SKILL.md
.agents/skills/eternal-arena-integration-reviewer/SKILL.md
.agents/skills/eternal-arena-quality-loop/SKILL.md          ← + phụ lục v1 (mục 0–9, checklist, 3 template)
docs/quality-loop/ARENA_SPECIALIST_TRAINING_MATRIX.md
```

**V1 (bootstrap):**

```
docs/quality-loop/README.md                     (FILE B)
docs/quality-loop/ARENA_BOOTSTRAP_REPORT_VI.md (FILE D — file này)
AGENTS.md                                      (FILE C — CHỈ append 2 section, không sửa nội dung cũ)
```

Tổng diff: **13 file**. V1 yêu cầu "4 file", V2 yêu cầu "9 SKILL.md + matrix + append AGENTS.md";
vì một phiên Arena chỉ mở **một** PR, hai spec được gộp chung một PR. `AGENTS.md` nhận cả hai section
mới (`## Arena iterative game studio`, `## Arena specialist skill orchestration`) theo đúng nguyên văn
của từng spec; phần gốc giữ nguyên. Không chạm: `app/src/**`, `app/src/main/assets/premium_slice`,
`app/src/main/assets/sanctuary_p03`, `WorldObstacle` layout, save data, `.codex/**`, `.github/workflows/**`,
PNG, và 29 file của PR #28.

## 5. Validation (syntax / format / path)

| Kiểm tra | Lệnh | Kết quả |
| --- | --- | --- |
| Frontmatter hợp lệ & `name` == tên thư mục | script Python đọc 9 `SKILL.md`, parse `--- … ---`, so `name` với basename | 9/9 khớp |
| Đường dẫn trong link Markdown tồn tại | resolve mọi liên kết Markdown trong `docs/quality-loop/*.md` | hợp lệ (xem mục 5b) |
| Đường dẫn source được skill tham chiếu | `find`/`grep` trên `app/src/main/java`, `docs/skill-library/scenario/upstream/skills`, `.agents/skills` | khớp; `codex-tower-rescue` là tài liệu đọc được, không thực thi |
| Whitespace | `git diff --check` | sạch |
| `AGENTS.md` | `git diff -- AGENTS.md` | chỉ có dòng `+` ở cuối, không dòng `-` nào |
| Phạm vi diff | `git diff --name-only` / `git status --porcelain` | đúng 13 file, không file game/code |
| Provider/API | rà nội dung 10 file cài đặt | không có lệnh gọi API trả phí, không thêm plugin/API key, không lệnh train model |

**5b. Output thật của script validation (chạy trên checkout trước khi commit):**

```
PASS  9 skill files, valid frontmatter, name == folder
PASS  V2 verbatim: 8 role SKILL.md + ARENA_SPECIALIST_TRAINING_MATRIX.md (9 file, byte-identical)
PASS  quality-loop: toàn bộ 29 dòng body V2 được giữ nguyên; chỉ nâng `description` frontmatter
      theo V1 trigger ("improve gameplay, art, QA or performance") — không tạo skill trùng tên
PASS  V2 install: 9 verbatim + 1 merged
PASS  links resolve: README.md (2), ARENA_BOOTSTRAP_REPORT_VI.md (1), MATRIX (0)
PASS  quality-loop literal source paths exist — 15 path kiểm tra, missing=[]
PASS  no paid API / plugin / model-training instruction
PASS  V1 rule: PHỤ LỤC V1 / CHECKLIST / 3 TEMPLATE / zoom 0.36-1.48 / "Tối đa 3 vòng" / "không tự merge"
PASS  AGENTS.md original prefix byte-identical (so sánh byte với bản sao trước khi append)
PASS  AGENTS.md has both new sections once
RESULT: ALL PASS

git diff --check                 -> CLEAN
git status --porcelain -uall     -> 13 file (1 modified + 12 added)
grep game paths in changed set   -> NONE (không có app/src, PNG, workflow, .codex, premium_slice, sanctuary_p03)
```

Ghi chú trung thực: hai lần `FAIL` ở lượt validate đầu là lỗi của chính script kiểm tra (so sánh
toàn bộ file quality-loop trong khi file này đã được gộp có chủ đích; và regex bắt nhầm chuỗi mã
mẫu `](…)` trong bảng). Đã sửa script/đổi cụm mô tả và chạy lại → `ALL PASS`. Không có lỗi nội dung
file nào phải bỏ qua.

## 6. Năng lực subagent: quan sát được vs KHÔNG quan sát được

- **Quan sát được**: tool của phiên gồm `bash`, `read_file`, `write_file`, `edit_file`, `present_file`,
  `start_process`/`get_process_output`/`stop_process`, `web_search`/`fetch_page`/`image_search`,
  `generate_image`/`generate_speech`/`add_voice`, `ask_user`, `list_connector_tools`. Có `git` + `gh`
  trong sandbox. Các tool đọc/không xung đột được gọi song song (2–3 lệnh cùng lượt).
- **KHÔNG quan sát được**: **không có tool native spawn/delegate subagent** trong phiên này; không có
  ID/tên đơn vị con nào được khởi tạo; không có output của "4 model độc lập". Vì vậy 9 vai trò được
  thực hiện dạng **logical checkpoints tuần tự trong MỘT agent** (Director → audit → cài đặt →
  validation → Integration review), đúng như quy định mục 0. Không tuyên bố có subagent song song.
- `.codex/agents/*.toml` (5 file, từ PR #27) thuộc Codex, **không đụng vào**, không dùng làm bằng chứng
  Arena spawn được agent. `.aistudio/` cũng không sửa. Skills là tài liệu offline; không có Scenario
  MCP, không gọi dịch vụ trả phí, không lấy model.

## 7. CI / build

- Local: **KHÔNG CHẠY** — thiếu JDK 21, Gradle và Android SDK (`java: command not found`,
  `gradle: command not found`, `ANDROID_HOME` unset). Không bịa kết quả `assembleDebug`/unit test.
- CI nền đã có thật (mục 2) là của `main`/PR #28, không phải của PR này.
- CI của chính PR cài đặt — **kết quả thật** trên candidate SHA
  `999292807a2aef533fd843e31c22d3c81c69c930` (đọc bằng `gh api
  repos/lightingfff/eternal-sunmmoner/actions/runs?head_sha=…`; `gh 2.23` không có
  `gh run list --commit`, nên đã đổi lệnh, không bịa output):
  - run `41583171747` — `Build Eternal Summoner APK`, event `pull_request`,
    `status=completed`, **`conclusion=success`**, tạo `2026-11-03T05:18:44Z`, xong
    `2026-11-03T05:20:07Z` → https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41583171747
  - `gh pr checks 29`: `Build Android APK` = **pass** (1m21s); job
    `Publish direct APK to Release` = `skipping` (điều kiện `github.ref == refs/heads/main` không thoả).
  - `total_count=1` workflow run trên SHA này: `lyra-render-evidence.yml` **không chạy** đúng path
    filter `app/src/main/**` → **không có ảnh render engine mới** cho PR cài đặt này. Đây là hạn chế
    thật, không được hiểu là "đã có visual QA evidence".
  - CI `success` ở đây chứng minh bản build APK compile được; **không** chứng minh gameplay hay chất
    lượng hình ảnh (đúng tinh thần "test xanh ≠ hình đẹp").

## 8. PR & cách owner kiểm tra

**PR #29**: https://github.com/lightingfff/eternal-sunmmoner/pull/29 —
`state=OPEN`, `isDraft=false`, `mergedAt=null`, `base=main`,
`head=arena/56e30acd-eternal-sunmmoner`, **CHƯA MERGE** (owner duyệt). PR được mở tại commit
`9992928` (push thật, xác minh bằng `git ls-remote`); commit thứ hai chỉ cập nhật file báo cáo Markdown
này với kết quả CI thật, nên không làm thay đổi 13 file hay bất kỳ file game nào. Một phiên = một PR:
PR #28 (gameplay, Draft) vẫn mở nguyên, không bị đụng. Kết quả CI trên SHA HEAD cuối cùng được ghi
trong phần thảo luận của PR (không giả mạo trong file này).

Cách kiểm tra:

```bash
git fetch origin arena/56e30acd-eternal-sunmmoner
git diff --stat origin/main...origin/arena/56e30acd-eternal-sunmmoner       # phải đúng 13 file
git diff --check origin/main...origin/arena/56e30acd-eternal-sunmmoner       # sạch
git diff origin/main...origin/arena/56e30acd-eternal-sunmmoner -- AGENTS.md  # chỉ append 2 section
gh pr view 29 --json state,isDraft,mergedAt,headRefOid,url
SHA=$(gh pr view 29 --json headRefOid --jq .headRefOid)
gh api "repos/lightingfff/eternal-sunmmoner/actions/runs?head_sha=$SHA" \
  --jq '.workflow_runs[] | "\(.name) \(.status) \(.conclusion) \(.html_url)"'
```

Sau khi PR được merge, phiên Arena **mới** (chat mới trỏ `main`) sẽ tự đọc skill đã lưu trong repo;
không cần tạo agent profile mới.
