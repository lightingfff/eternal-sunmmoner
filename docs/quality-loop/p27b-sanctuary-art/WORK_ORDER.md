# WORK_ORDER — P27B Sanctuary art + ivory summon + UI

- Ngày: 2026-11-05. Baseline main SHA `bd68697445d4a5511cccfc2060689fba99ee6f4f`; nhánh phiên **cố định** `arena/683a732b-eternal-sunmmoner`. Không tạo/switch branch khác theo quy tắc môi trường Arena; PR draft mở từ nhánh này, không merge. Trước khi thực thi: `gh pr list --state open` thấy PR #28 (Mission01, DRAFT) có sửa `SanctuaryFacilityModals.kt`; PR #13 sửa workflow APK và packaging (tránh các file này). Không sửa PR đó.
- Preflight: `git status --short` có `art/candidates/`, `docs/quality-loop/asset-forge-v1/` và 4 scripts `tools/*asset_forge*` / `tools/package_asset_forge_v1.py` dạng untracked từ công việc V2/V3. **Bảo toàn; không add/commit/chỉnh sửa/xóa các thư mục/file đó khi làm P27B**.
- Mục tiêu người chơi: nền cỏ/đường và công trình trên **Sanctuary hiện có** rõ vật liệu hơn; khu Summoning Hall trắng ngà ở world **và** modal; Gate có khối và hốc sâu; HUD đọc được, vẫn chơi được như cũ. Việc thiếu art nguồn hoặc ảnh renderer được công bố thay vì báo hoàn tất.
- Current behavior: `PixelTileAtlas` đọc P03 8 grass/6 soil/8 flagstone/6 cobble/4 gravel; grass overview dùng block 4×4 dựng `by lazy`. `PixelStructureAtlas` PNG-first cho các facilities. `PixelSpriteRenderer.drawCrispSprite` bottom-centered, `FilterQuality.None`; `SanctuaryWorldRenderer` world `0.72f`, plaza/road clipped, Summon ground chưa ivory riêng; `SanctuaryFacilityModals` skin tối dùng chung. Xem `docs/p27b/01_AUDIT.md` và `BASELINE.md`.
- Năng lực: **một agent duy nhất**, vai trò Director → environment/asset artist → reviewer → integration QA theo thứ tự; không giả lập subagents. Image tool Arena có thể gọi, nhưng output chỉ là art nguồn; Pillow dùng normalize/crop/QA, không coi code hình khối là premium.

## Acceptance khóa trước khi triển khai
1. P27B `sanctuary_p27b/` chỉ gồm PNG **gốc mới**, 64×64 cho tile và RGBA alpha thật cho structures; manifest có SHA, canvas, source, pivot, state. Các tile được QA 3×3/5×5 trực quan; zero edge-jump không tự chứng minh seamless. Phần nào chưa qua → `NEEDS_REWORK`/`GAME_CANDIDATE`, không `READY`.
2. P27B opt-in/PNG-first fallback P03 nếu thiếu/corrupt, tất cả draw passes và grass block thấy cùng biến thể (không decode mỗi frame); không overwite asset P03/premium_slice.
3. Ảnh `before/after` **Robolectric native Compose Canvas** cùng SHA code core khác art, cùng 1280×720, camera, zoom, `timeSec=0`, hour=12/CLEAR và hour=0/CLEAR, tier BALANCED. UI screenshot từ Compose Rule; ảnh nguồn/concept không được ghi AFTER. Nếu CI baseline không chạy, ghi blocker và không claim đạt mỹ thuật.
4. Không đổi `ObliqueCamera.OBLIQUE_Y_SCALE=0.72f`, `TILE_PX=64`, `TowerFloorCatalog` map/footprint/entrance, depth sort, shadows, paths, weather/sky, schema/save, rewards/gate logic. Summon busy/confirm/cancel/result và UI test tags giữ nguyên.
5. Android compilation/unit/render + APK thành công trên **P27B SHA** trước khi review; local thiếu JDK/SDK → chạy Actions, report SHA/run/artifacts. Failing tests/blocker giữ trạng thái DRAFT/BLOCKED.
6. QA đánh giá 10 tiêu chí visual của P27B theo ảnh engine (0–5) với tiêu chí dưới 4 thành art debt; mô hình quality loop 6 chiều 0–4, cần từng chiều ≥3 và tổng ≥20/24 *khi có bằng chứng*. Critical blockers nhị phân: build, input/tags/callback, door/collision/anchor, image missing fallback, renderer evidence. Chưa đo thì NOT VERIFIED, tuyệt đối không tự chấm PASS.
7. FPS/GPU/thermal chỉ báo `NOT TESTED` nếu không có máy thật. PR draft, không auto merge.

## Ownership & serialization
| Path (may change) | Writer logical | Reason |
|---|---|---|
| `docs/p27b/**`, `docs/quality-loop/p27b-sanctuary-art/**` | Director/QA sequential | Audit, baseline, manifest, evidence, handover |
| `app/src/test/java/com/example/P27B*Test.kt`, `app/src/test/java/com/example/ui/P27B*Test.kt`, `.github/workflows/p27b-render.yml` | Integration | Baseline and after same harness; CI artifacts |
| `app/src/main/assets/sanctuary_p27b/**`, `tools/p27b_*.py` | Art/QA sequential | Original PNG and offline validation |
| `app/src/main/java/com/example/game/pixel/{SanctuaryArtAssets,PixelTileAtlas,PixelStructureAtlas,SanctuaryWorldRenderer}.kt` | Environment architect then Director | Variant lookup, summon ground; **no edits to camera/path/collision code** |
| `app/src/main/java/com/example/ui/components/{FantasySkin,FantasyManagement}.kt`, `app/src/main/java/com/example/ui/screens/{SanctuaryFacilityModals,SanctuaryLobbyScreen}.kt` | UI artist then Director | Opt-in variants/HUD; #28 conflict note above |

MUST NOT edit: `.git`, user V2/V3 untracked originals, `premium_slice/` and `sanctuary_p03/` old files, map/tower catalogs, combat/auth/backend/save, P26 sky/cloud/weather, existing PR branches, `.codex`. No force-push/clean/reset hard. File writer 1 tại một thời điểm; không chạy 2 Gradle trong cùng checkout.

## Selected skills / QA plan
- Router `.agents/skills/eternal-summoner-skill-router/SKILL.md`; quality-loop `.agents/skills/eternal-arena-quality-loop/SKILL.md`; specialist `.agents/skills/eternal-arena-environment-architect/SKILL.md`, integration reviewer; upstream `scenario-textures`, `scenario-game-assets`; sprite adapter `ai-game-spritesheets`. Mọi ví dụ Unity/isometric phải dịch sang Kotlin/Compose oblique và pivot thực.
- THỨ TỰ: record baseline → style rules → texture source/QA → opt-in wiring → engine AFTER → critic & targeted fix (≤3 cycles) → ivory building/zone/modal + gate only if compatible art → further buildings/props/HUD only if renderer verifies prior slice. Unfinished items are explicitly art debt, not fake complete.
- Bookmark engine: overview (1600,1380,0.36), service (960,860,0.88), plaza (1940,1240,1.0), Summon (2430,1715,1.48), gate (1960,310,1.48). Time/day matrix + night Summon/gate. `docs/p27b/BASELINE.md` records metadata/harness; include UI landscape 800×360dp and portrait 360×640dp.
- Test commands: `gradle :app:testDebugUnitTest --tests 'com.example.P27BRenderTest'` and `--tests 'com.example.ui.P27BUiTest'`, `gradle :app:assembleDebug` in Actions, Python PNG/manifest validator, existing P23/P26C regression tests where possible. Each reported run must match committed candidate SHA; inspect PNGs by eye before scoring.
- Rollback: remove **only P27B-specific new provider lookup** (fallback old PNG) and new P27B assets via a normal follow-up commit if visual fail; preserve all original art. Stop if engine baseline or CI after unavailable; open draft PR with blocker/evidence and next best action, not a READY claim.
