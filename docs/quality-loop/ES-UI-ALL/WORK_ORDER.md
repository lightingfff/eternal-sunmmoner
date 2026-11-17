# WORK_ORDER — ES-UI-ALL

- **Ngày:** 2026-11-03 (UTC theo phiên); **baseline HEAD:** `83e45ce09220ec3c2d7e7e4c797b7e62126a459d`; **branch:** `arena/bb4a4f21-eternal-sunmmoner`; **PR của branch hiện tại:** chưa có.
- **Working-tree baseline:** có sẵn 22 tracked files sửa dở và 2 đường dẫn asset P28 chưa track từ lượt UI trước. Không commit, reset hay ghi đè trạng thái đó; hoàn thiện trực tiếp các thay đổi này.
- **PR giao thoa đã refresh:** #47 (OPEN, sửa 23 dòng trong `TowerBattleScreen.kt` cho độ trễ lệnh Master/touch); #28 (OPEN/DRAFT, có `MainActivity.kt`, `CourierMissionHud.kt`, `RescueJourneyHud.kt`, `SanctuaryFacilityModals.kt`, `TowerBattleScreen.kt` cùng gameplay/save); #96 (OPEN/DRAFT, mechanics Floor 1, không sửa UI nguồn chính); #73 và #70 chỉ là báo cáo/đề xuất docs. Không chỉnh sửa nhánh/PR khác. Giữ các hợp đồng runtime; phần giao thoa được ghi rõ để tích hợp tuần tự khi PR upstream được xử lý.
- **Mục tiêu người chơi:** mọi giao diện người chơi nhìn thấy—từ khởi động/đăng nhập, Thánh Địa, trang Master, quản lý anh hùng/cơ sở, cổng Tháp/chọn tầng/chuẩn bị, HUD chiến đấu, lệnh Master, joystick, cứu hộ/tiếp vận, thông báo, cài đặt và tải—được thiết kế lại theo cùng ngôn ngữ dark-fantasy dễ đọc, có thứ bậc, co giãn và rõ trạng thái; không chỉ đổi wrapper. Tương tác hiện hữu, offline/guest, save, điều hướng, camera, hitboxes và mô phỏng giữ nguyên.
- **Current behavior / baseline audit:** 109 Kotlin files trong `app/src/main/java`; 23 file có `@Composable`; 8 screen files; 34 Kotlin UI/supporting files dưới `ui/` + `performance/`, cộng `MainActivity.kt`. Đã truy vết route ở `MainActivity.kt` và modal wiring ở `SanctuaryLobbyScreen.kt`/`TowerGateScreens.kt`. Custom Canvas UI nằm trong Sanctuary/Tower scenes, `TowerGateMapScene.kt`, `LivingExpeditionPresentation.kt`, `LobbyHeroPresentation.kt`, joystick và notice/loading layers; nó không thể được tính bằng scan Material3. Hai `AlertDialog` trong `SanctuaryFacilityModals.kt` cũng thuộc phạm vi.

## Coverage inventory — không bỏ UI

| Nhóm người chơi thấy | Path đã kiểm kê | Nội dung cần giữ trong redesign |
|---|---|---|
| Khởi chạy, phiên guest/cloud, route và loading | `app/src/main/java/com/example/MainActivity.kt`; `ui/screens/AuthGateScreen.kt`; `ui/components/SettingsAndAccountModal.kt`; `ui/components/p28/ESLoading.kt` | Auth/guest continuation, bootstrap, status/error, root route; không tuyên bố cloud khi offline. |
| Sanctuary scene/HUD và entry overlays | `ui/screens/SanctuaryLobbyScreen.kt`; `ui/components/LobbyHeroGestures.kt`; `ui/components/LobbyHeroPresentation.kt`; `ui/screens/MasterHubScreen.kt`; `ui/components/VirtualJoystickAndControls.kt` | World/canvas, top/bottom HUD, master chip + trang Master, resource badges, camera, fast travel, minimap/overview, chronicle/prologue, hero touch/drag, map legend. Master là yêu cầu tường minh. |
| Facility & roster/profile dialogs | `ui/screens/SanctuaryFacilityModals.kt`; `ui/screens/HeroProfileModal.kt`; `ui/components/FantasyManagement.kt`; `ui/components/p28/ESHeroCard.kt` | Summon/train/armory/synthesis/quarters/restaurant, cả hai xác nhận, equipment/recipe/selection, hero overview/stats/skills/recovery, island expansion, hero/team management, cards/portraits. |
| Tower gate, floors, squad prep | `ui/screens/TowerGateScreens.kt`; `ui/screens/TowerGateMapScene.kt` | Canvas gate/map, floor picker/details/cutscene, squad deployment/roster, back/floor navigation. |
| Combat screen, Master orders, mission HUDs | `ui/screens/TowerBattleScreen.kt`; `ui/components/CourierMissionHud.kt`; `ui/components/ExpeditionFieldRewardHud.kt`; `ui/components/LivingExpeditionHud.kt`; `ui/components/RescueJourneyHud.kt`; `ui/components/LivingExpeditionPresentation.kt`; `ui/components/TowerEncounterFraming.kt`; `ui/components/ExpeditionRewardIcons.kt` | Combat canvas, top objective/party and bottom tactical controls/skills/map, alerts, living objectives, courier/rescue choices, world cues, field rewards/results. Không sửa mechanics/latency contract. |
| Shared navigation/chrome, state and visual tokens | `ui/components/FantasySkin.kt`; `ui/components/ESUiKit.kt`; `ui/components/p28/ESNineSlice.kt`; `ui/components/p28/ESP28Assets.kt`; `ui/components/p28/ESNotices.kt`; `ui/theme/ESDesignTokens.kt`; `ui/theme/Color.kt`; `ui/theme/SanctuaryUi.kt`; `ui/theme/Theme.kt`; `ui/theme/Type.kt`; `ui/SummonerViewModel.kt` | Panel/frame/button states, notices/banner queue, theme/typography, player-visible UI state. State machine/callback contract là no-go; chỉ đổi visual adapter khi cần. |
| Runtime diagnostics overlay | `performance/ScenePerformanceOverlay.kt`; `performance/SceneFrameLoop.kt`; `performance/ScenePerformance.kt` | Opt-in overlay phải dễ đọc và nói rõ giới hạn CPU/GPU; frame loop/probe không đổi. |

**All 23 baseline Compose-bearing Kotlin files** are included above: `MainActivity.kt`; 5 shared/asset/notice/hero components (`FantasySkin.kt`, `FantasyManagement.kt`, `ESHeroCard.kt`, `ESLoading.kt`, `ESNotices.kt`); 6 controls/HUD files (`VirtualJoystickAndControls.kt`, `CourierMissionHud.kt`, `ExpeditionFieldRewardHud.kt`, `LivingExpeditionHud.kt`, `RescueJourneyHud.kt`, `SettingsAndAccountModal.kt`); 7 screen files (`AuthGateScreen.kt`, `HeroProfileModal.kt`, `MasterHubScreen.kt`, `SanctuaryFacilityModals.kt`, `SanctuaryLobbyScreen.kt`, `TowerBattleScreen.kt`, `TowerGateScreens.kt`); theme `Theme.kt`; performance `SceneFrameLoop.kt` and `ScenePerformanceOverlay.kt`. This redesign adds `ui/components/ESUiKit.kt`, bringing the current source total to 24 Compose-bearing files. `TowerGateMapScene.kt` and `LivingExpeditionPresentation.kt`/`LobbyHeroPresentation.kt` draw player-facing Canvas layers without `@Composable`; they are still in the coverage set. Remaining listed support files must be audited for visual contracts, not assumed to be screens.

## Acceptance rubric (locked before additional implementation)

**Binary blockers**
1. Coverage: each route/surface listed above is either visibly redesigned or documented as a deliberate renderer/engine surface whose player-facing layer is covered; Master Hub has a distinct improved pass, not merely inherited wrappers. Re-run source inventory and reconcile every Compose declaration and player-facing Canvas/dialog path.
2. Behavior: no callback, navigation destination, dialog confirmation, touch target/action, save/account behavior, camera/anchor/collision/depth order, combat command, objective/reward, or frame-loop behavior is removed/rebound. Preserve `ObliqueCamera.OBLIQUE_Y_SCALE = 0.72f` and current game engine.
3. Accessibility/layout: selected state is explicit where applicable; actions remain discoverable; close/back works; high-contrast readable hierarchy, wrapping/truncation for small windows, safe-drawing insets and practical touch sizes preserved.
4. Validation: asset validator and whitespace/static checks pass; run the focused UI test/build commands if the installed environment permits. Missing Android/Gradle toolchain or real render is `NOT VERIFIED`, never a pass.

**Six soft dimensions (0–4; rate only with real evidence):** visual coherence, technical correctness, player clarity, tactical information hierarchy, polish, efficiency; target each ≥3 and ≥20/24 only if appropriate visual evidence exists. Here runtime screenshots/hardware are currently unavailable, so visual/render/performance rubric stays `NOT VERIFIED` rather than self-scored.

## Risks, ownership and serialization

- **Single writer:** one Arena session; no verified native subagent-spawn tool is available in this session. Logical passes only: Director/integrator → A04 UI/UX implementation → independent-style QA pass → integration review. Do not claim independent agents or parallel reviews.
- **Writer per file:** this session is sole writer for the listed local branch files; code changes are serialized by subsystem, with shared tokens/components first, then screens/HUD, then tests/docs. No parallel writes to shared Kotlin/Gradle output.
- **Potential remote overlap:** `TowerBattleScreen.kt`, `CourierMissionHud.kt`, `RescueJourneyHud.kt`, `SanctuaryFacilityModals.kt`, and `MainActivity.kt` also appear in still-open PR #28/#47. Keep their gameplay APIs and existing callbacks intact; do not edit mechanics or import another branch. Record integration risk for owner and re-check live PR status before handoff.
- **Must not change:** `.git`, branch name, Firebase/auth/save schema, `SummonerViewModel` operations, combat/AI/event/reward implementation, world map layout/anchors/collision/navigation, camera projection, renderer/depth sorting, bitmap cache/filtering, scene frame loop, dependency/Gradle/workflow files, user/source art not specifically in UI pack, other PR branches, merge state.
- **Rollback:** revert only the specific UI/design-system paths added in this ticket; never reset/clean the worktree. Existing partial UI assets/source are part of baseline and remain intact.

## Selected guidance and actual capabilities

- Skills: `es-a04-ui-ux`, `eternal-summoner-skill-router`, `eternal-arena-quality-loop`, `eternal-arena-studio-director`; project UI remains Kotlin/Compose with 2.5D renderer.
- Source skills: use `scenario-unity-ui` only as transferable UX/layout/input principles, implemented in Compose; no foreign engine API. No image/art provider or paid asset tool is authorized/required.
- Existing toolchain check from the previous pass: Java/Kotlin/Gradle and executable `./gradlew` are unavailable. Re-check before validation. Repo asset validator is available. No Android emulator/runtime capture has been made.

## Test and evidence plan

- Reconcile every `@Composable` declaration under `app/src/main/java/com/example` plus Canvas/player-facing `draw*`, `Dialog`, `Popup` and overlay call sites against this inventory.
- `git diff --check`; `python3 tools/validate_assets.py`; targeted offline checks for P28 manifest/PNG dimensions/alpha and test semantics.
- If toolchain exists: `gradle :app:testDebugUnitTest --tests 'com.example.ui.*'` is too broad/fragile; run the UI contract tests selected by the final changes, then `gradle :app:assembleDebug`. At minimum, Master, navigation and layout/accessibility tests.
- Visual evidence: real Compose/Robolectric screenshots for title/auth, Master, Sanctuary, facility/profile, floor/prep, battle/HUD, loading/notice, both compact and wide layouts. If not runnable, report `LOCAL BUILD NOT RUN` and `REAL UI RENDER NOT TESTED`; source layout review is not equivalent.
- Hardware FPS/GPU/thermal: `NOT TESTED`.

## Round budget / stop

- Maximum 3 implementation/QA correction rounds. Any confirmed behavior regression is a blocker; fix and rerun relevant checks. If build/render remains unavailable, finish code/static review but mark those criteria `NOT VERIFIED`/`BLOCKED`—do not say all acceptance passed.
- PR policy: keep this active branch; no merge. Before handoff, refresh live PR overlap and report exact changed paths/tests and unavailable evidence.

## Continuation QA record — 2026-11-03 UTC

- **Date check:** the header's `2026-11-03` matches the session date; no date change was needed.
- **Source/UI review:** audited the changed account/settings imports and corrected a missing `PaddingValues` import; removed invalid `androidx.compose.foundation.layout.weight` imports (the modifier is scoped by `RowScope`/`ColumnScope`). Landscape auth branding now has a shared dark backplate; its contrast regression test was updated. Master readiness now counts `HeroStatusType.READY`, not every non-injured hero. The Master/settings/loading overlays now block stray touches to content below while preserving their explicit buttons/back behavior; focused regression tests were added but remain unrun.
- **Canvas coverage:** retokenized Sanctuary landmark plaques, hero labels/speech bubbles/drop-zone outlines; Tower mission/supply/discovery/boss callouts; Living Expedition world labels; and lobby selection/NPC auras. Their projection, anchors, hitboxes, draw/depth order, and game-state inputs were not changed. `AuthGateScreen`'s Canvas remains decorative background only. `TowerGateMapScene` was inspected as authored gate artwork/light/sprites without UI text or hit controls; its geometry/rendering intentionally remains unchanged, while selectable floor/squad overlays and accessibility descriptions remain in `TowerGateScreens`.
- **Static gates:** `git diff --check` — PASS. `python3 tools/validate_assets.py` — PASS (exit 0), with warnings for the >3 MiB preview sheet, missing local `app/google-services.json`, and case-insensitive duplicate basenames. A targeted P28 manifest check confirmed 24 entries and all referenced files exist. A source-only check confirmed all callsites use defined `ESDesignTokens` members; this is not a Kotlin compile.
- **Not verified:** Java, `kotlinc`, Gradle, `./gradlew`, and `gradle-wrapper.jar` are absent. Android build, UI unit/Robolectric tests, screenshots/device render, and hardware performance are **NOT RUN / NOT VERIFIED**. No acceptance claim is made for those gates.
