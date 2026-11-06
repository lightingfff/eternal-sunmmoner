# P28 — REAL IMPLEMENTATION HANDOFF (PHẦN 15)

## Branch / HEAD SHA / PR URL
- Branch session: `arena/b5e7a601-eternal-sunmmoner`, HEAD `c55cee3` (P28-C: UI render evidence + test fixes).
- Code P28 gốc nằm trong merge commit `0ccc5d4` (PR #36, code commit `8d8c8c7`).
- PR #36: https://github.com/lightingfff/eternal-sunmmoner/pull/36 — MERGED 2026-11-06T04:12:16Z (agent không tự merge).
- PR #37 (P28-C evidence): https://github.com/lightingfff/eternal-sunmmoner/pull/37 — mở, chưa merge; CI xanh trên `c55cee3`.

## Components created/changed (actual files)
- `app/src/main/java/com/example/ui/components/p28/ESP28Assets.kt` — design tokens "Astral Reliquary" + resolver đọc `manifest.json`, decode/cache qua `SanctuaryArtAssets`, `resetForTest()`.
- `.../p28/ESNineSlice.kt` — 9-slice renderer (DrawScope.drawImage src/dst rects).
- `.../p28/ESHeroCard.kt` — `ESHeroCard` (khung theo rarity) + `ESMiniHeroPortrait`.
- `.../p28/ESNotices.kt` — `NoticeCenter`/`ESNoticeFrame`/`ESNoticeSurface`/`ESNoticeStack`, tag `notice_<id>`.
- `.../p28/ESLoading.kt` — `ESLoadingState` sealed + `ESLoadingOverlay`, tag `es_loading_stage`/`es_loading_error`.
- Wire: `MainActivity.kt`, `ui/screens/TowerGateScreens.kt`, `ui/components/FantasyManagement.kt`, `ui/screens/TowerBattleScreen.kt` (giữ nguyên testTags incl. `tactical_command_feedback_pill`).
- Test: `app/src/test/java/com/example/ui/P28ComponentsTest.kt` (5 Robolectric test).
- Docs: `docs/p28/00_AUDIT_AND_CP1.md`, `docs/p28/contact_sheet.png`.

## New PNG assets + resolution + alpha validation
- 22 PNG trong `app/src/main/assets/ui_p28/` (cards/controls/icons/loading/notices) + `manifest.json` (22 entry: id/path/width/height/transparency/sha256/method/nineSliceInsetPx).
- Size: bg 1280×720; card frame 384×576; notice/button 288×96 (inset 24px); icon/star 64×64.
- Alpha: kiểm bằng unit test (`hasAlpha()` trên 10 asset khung) + CI "Validate assets" PASS. 10 art original bằng generate_image + PIL post-process, 12 chrome procedural PIL. Không copy art từ reference.

## Before/After engine screenshots
- `docs/p28/contact_sheet.png` là contact sheet của asset pack (không phải engine screenshot).
- Engine screenshot before/after chuyên cho P28: KHÔNG chụp trong session này (không có thiết bị/emulator trong sandbox) — ghi trung thực, không bịa.

## Trạng thái từng hạng mục
- Hero Card: **DONE** (frame theo rarity + nameplate + sao, dữ liệu thật từ HeroRecord, nút giữ nguyên).
- Party Grid: **DONE** (roster thật qua `ESMiniHeroPortrait` trong TowerGateScreens/FantasyManagement, responsive bằng Compose layout).
- System/Battle Notifications: **DONE** (NoticeCenter nhận event thật, priority CEREMONIAL>TACTICAL>INFO, dedup theo stableId, tag `notice_<id>`; battle HUD giữ `tactical_command_feedback_pill`).
- Loading/Auth: **DONE** (progress thật, không chữ "connecting to server" khi offline; guest login giữ nguyên).
- Hub/Summon/UI consistency: **DONE** (cùng token "Astral Reliquary" cho hub/management/battle; summon giữ nguyên hành vi).

## Functional/Compose tests
- `P28ComponentsTest` (7 test sau P28-C: manifest/decode/alpha, fallback null, NoticeCenter, 3 loading states, hero card binding) + `P28UiEvidenceTest` (5 screenshot test): 12/12 PASS — debug run 42167228477 `BUILD SUCCESSFUL` trên `da18766`.
- Lưu ý trung thực: ở PR #36 test P28 chưa từng chạy trong CI thật (filter sai package trong workflow debug); phát hiện và sửa ở PR #37 (setContent chỉ 1 lần/test; assert bằng onNodeWithText).
- CI thật trên `c55cee3` (PR #37): Build APK run 42167869825 success; P27B render evidence run 42167869790 success (bao gồm step P28 UI screenshots).

## Android assembleDebug
- **PASS** — run 42130974083 ("Build Eternal Summoner APK") trên `8d8c8c7`; và run 42161097575 PASS trên merge commit `0ccc5d4` của main.

## Device FPS p50/p95/p99
- **NOT TESTED ON PHYSICAL ANDROID** (không có thiết bị thật trong sandbox; không bịa số).

## Memory/thermal
- **NOT TESTED ON PHYSICAL ANDROID**.

## Remaining art debts/bugs (with evidence)
- Engine screenshot thật của UI mới chưa có (chỉ có Robolectric test + asset contact sheet) — cần chạy tay trên thiết bị để chụp và đo FPS/thermal.
- 9-slice dùng `DrawScope.drawImage` src/dst rects (API của version hiện tại), đã green CI.

## No auto-merge confirmation
- Agent không tự merge PR #36; PR được merge bởi Arena/user (merge commit `0ccc5d4`, mergedAt 2026-11-06T04:12:16Z). Workflow debug tạm `p28-debug.yml` đã được xóa khỏi branch ở commit `c264c21` sau khi merge.
