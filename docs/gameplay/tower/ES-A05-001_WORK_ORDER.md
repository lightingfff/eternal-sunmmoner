# WORK_ORDER — ES-A05-001 (A05 Tower Level & Quest Designer)

- **Ngày:** 2026-11-08 (UTC)
- **Issue:** [#56 — ES-A05-001](https://github.com/lightingfff/eternal-sunmmoner/issues/56), STATE: READY tại thời điểm claim; điều phối qua [#39](https://github.com/lightingfff/eternal-sunmmoner/issues/39).
- **Baseline main:** `bdff6f9976dc4927e1e095a503bc339509c7a9aa` (fetch trực tiếp; gồm PR #51 và #65 đã merge).
- **Đối tượng review read-only:** PR [#28](https://github.com/lightingfff/eternal-sunmmoner/pull/28) head `09c553d81c53046fe1038b70c7046e2a6cff0661` (DRAFT, `mergeable=CONFLICTING` với main, `gh pr checks 28` → "no checks reported").
- **Branch/PR của task này:** `arena/df5f7758-eternal-sunmmoner` (nhánh phiên Arena bị nền tảng khóa cố định, không tạo nhánh `agent/a05-…` được — đã khai báo trong [CLAIM]); một PR riêng, không merge.
- **Runtime thật:** một phiên Arena Agent Mode, vai trò A05 logical; không có native subagent; GitHub identity @lightingfff (login owner dùng chung, phân biệt bằng session). Không có JDK/Android SDK trong sandbox (`java: command not found`) → không chạy Gradle cục bộ; nhiệm vụ này là docs-only nên không cần build.

## Mục tiêu người chơi thật

Người chơi mới hoàn thành chuyến cứu hộ Floor 1 (đồng cỏ Khởi Nguyên) trong **5–10 phút chơi thật có chất lượng**: hiểu mục tiêu trong 10–20 giây, ra quyết định chiến thuật có hậu quả nhìn thấy được, gặp ≥2 kiểu hành vi địch khác nhau, chọn 1 trong 2 nhánh kết quả (cứu người / thu hồi tiếp vận) và thấy hệ quả còn lưu sau khi thoát game. Ticket này **chỉ tạo đặc tả + review**, chưa thay runtime.

## Current behavior (đã đọc tại PR #28 @ 09c553d8)

- `TowerCombatEngine.kt:35-38,132-133` bật `enableLostCourier/enableRescueJourney` cho Floor 1 từ `TowerBattleScreen.kt:259-260`; map mở rộng additive 6200×3600 (`TowerFloorCatalog.buildRescueJourneyMap`, dòng 1104+), giữ nguyên meadow gốc 3400×2150.
- `RescueJourneyContent.kt`: 9 chapter, route 21 điểm (~9.891 world units); `RescueJourney.kt:72` courier đi 44u/s; `RescueFieldActions.kt`: 14 bước tương tác vật lý (bán kính 140u).
- `LostCourierEncounter.kt`: escort đuôi cũ (guard → scout báo động 9s → Kraag telegraph 1,5s/counter +35%), choice Support/Supplies tại x≥1100, pickup 3s liên tục trong 90u tại (1220,1130); 4 outcome `RESCUED / SUPPLIES_RECOVERED / RESCUED_WITHOUT_SUPPLIES / FAILED`.
- `LostCourierProgression.kt`: cờ vĩnh viễn, thưởng lần đầu +20 Mana (support) / +60 vàng (supplies), tương thích save cũ, giảm giá synthesis 120→90.
- Trace CSV thật `docs/visual-evidence/mission01b/`: 317,15–318,30s simulation; travel 221,17s (~69,6%); combat hành trình 11,67–12,85s (~3,9%); gap dài nhất giữa sự kiện có nghĩa ~36,3s.

## User-visible win sau khi xong ticket này

Owner + Codex + A06/A07 có một bản review định lượng PR #28 và một đặc tả Floor 1 đủ rõ để lập ticket triển khai tiếp (route rút gọn, mật độ quyết định, 2 nhánh outcome giữ nguyên phần thưởng/cờ) mà không thay runtime trong PR này.

## Acceptance (đo được, cho chính ticket docs này)

1. Review PR #28 dẫn đúng số đo từ CSV/mã nguồn tại head `09c553d8`, phân biệt simulation giây vs phút chơi người thật; mọi mục chưa đo ghi `NOT TESTED`. (Đối chiếu: bảng số liệu trong `ES-A05-001_PR28_REVIEW.md` khớp 3 CSV.)
2. Spec có đủ: beat sheet gắn tọa độ thật của map Floor 1; FSM/bảng trạng thái; 2 nhánh outcome + partial/fail/retry; ≥2 kiểu địch có telegraph/counterplay; lệnh Master có tác động kiểm chứng được; interface contract (event ID/cờ/callback) cho Codex/A06/A11; fallback khi người chơi không ra lệnh. (Mục lục của `FLOOR1_MEADOW_RESCUE_SPEC.md`.)
3. Spec giữ nguyên hợp đồng hiện có: tên cờ `COURIER_*`/`DISC_m01b_*`, thưởng 20 mana/60 gold, courier 44u/s, camera Y=.72, Master off-map, không sửa save schema. (Grep đối chiếu tên cờ trong spec = tên cờ trong `LostCourierProgression.kt`.)
4. PR chỉ chứa đúng 3 file docs được giao; không file runtime/asset/Gradle/workflow. (`git diff --stat` so với main.)
5. Không tuyên bố human playtest/FPS/CI khi chưa có: các mục đó gắn nhãn NOT TESTED / chờ CI trên SHA của PR.

## Regression risks

Không có rủi ro runtime vì docs-only. Rủi ro quy trình: spec mâu thuẫn hợp đồng save/combat hiện hữu → giảm thiểu bằng cách trích nguyên văn tên cờ/giá trị từ code PR #28 và ghi rõ điểm nào là **đề xuất thay đổi** cần Codex duyệt.

## Files MAY change (mỗi file đúng 1 writer = A05 phiên này)

| path | writer | nối tiếp sau |
|---|---|---|
| `docs/gameplay/tower/ES-A05-001_WORK_ORDER.md` | A05 | — (file này, viết trước) |
| `docs/gameplay/tower/ES-A05-001_PR28_REVIEW.md` | A05 | WORK_ORDER |
| `docs/gameplay/tower/FLOOR1_MEADOW_RESCUE_SPEC.md` | A05 | review |

## Files MUST NOT change

Toàn bộ `app/`, `art/`, `public/`, Gradle, Firebase, `.github/`, `.agents/`, `.codex/`, `docs/agents/`, ảnh/asset người dùng, nhánh PR #28/#47/#50/#67, camera `OBLIQUE_Y_SCALE=0.72f`, save schema, Sanctuary layout. Không ghi đè skill/PR khác; không force push/reset/clean; không merge.

## Song song vs nối tiếp

Một writer duy nhất, ba file viết tuần tự. Đọc song song (git/gh) không xung đột. Không giao thoa file với bất kỳ PR mở nào (đã kiểm #67/#50/#47/#28/#13 — không PR nào đụng `docs/gameplay/**`).

## Selected skills

`eternal-summoner-skill-router` (router) + `es-a05-tower-level-designer` (vai chính) + `eternal-arena-tower-director` (pacing/mission design) + `eternal-arena-quality-loop` (bằng chứng/vòng lặp). Không nạp skill sprite/Scenario vì không làm asset.

## Test plan

- Docs-only: không chạy Gradle (thiếu JDK/SDK, đã khai báo). Kiểm tra tính nhất quán số liệu bằng script đọc CSV thật của PR #28 (đã chạy, kết quả trong review).
- CI thật của PR này: theo dõi `Build Eternal Summoner APK` trên đúng head SHA sau khi push; kết quả ghi vào [HANDOFF], không suy đoán.
- Review độc lập: A06 + A07 + Codex (ticket chỉ định) + Argon/A09 theo DUAL_LEAD_WORKFLOW; user merge.

## Evidence ledger fields

Baseline ref (main bdff6f9 / PR28 09c553d8) · candidate SHA của PR này · lệnh đã chạy + output thật · đường dẫn CSV/ảnh đã đối chiếu · limitations (không thiết bị, không human playtest).

## Round budget / rollback / PR policy

Tối đa 3 vòng sửa theo review; rollback = đóng PR (không cần revert runtime). Mở PR docs riêng từ nhánh phiên, **KHÔNG merge**; dừng sau [HANDOFF] chờ review.
