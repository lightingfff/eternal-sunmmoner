# P28-D — Master Hub / Hero Card wiring + Sanctuary building foundations (PHẦN 15 addendum)

Branch: `arena/9db64866-eternal-sunmmoner` @ `25ce818` (+ evidence commit `88464a2`). PR #38 (open, not self-merged).
CI on `160c5b1`: Build APK run 42215217182 success; P27B render evidence run 42215217237 success (runs P28ComponentsTest + P28UiEvidenceTest + P28DRenderTest); Lyra run 42215217279 success.

## Master Hub / Hero Card P28 — DONE
- `ESHeroCard` (real HeroRecord: name, rarity frame, stars, portrait, injured state) wired into:
  - `HeroManagementDialog` (FantasyManagement.kt) — tag `hero_card_<id>`, tap = inspect; `inspect_<id>` / `assign_<id>` / `close_hero_management` / `hero_management_dialog` tags and onInspect/onToggle callbacks preserved.
  - Tower party prep (TowerGateScreens.kt) — tag `prep_card_<id>`, tap = toggle squad; `prep_hero_<id>` row tag + button preserved.
- Test: `P28UiEvidenceTest.heroManagementDialogWiresRealHeroCardsAndCallbacks` asserts both cards render and assign/inspect callbacks fire with the real hero id. PASS in CI.

## Sanctuary buildings — DONE (engine-verified)
- `PixelSpriteRenderer.drawStructureFoundation`: footprint-sized stone plinth (móng nhà), front wall-base band + edge line (chân tường), 3 entrance steps (bậc thềm), and a footprint-shaped parallelogram contact grounding — NO ellipse decals. Applied to all 14 building PropKinds (facilities, townhouses, cottages, fortress/gate towers). Size comes from the obstacle's authored `footprintHalfW/footprintDepthNorth` when present, else the sprite's visible base.
- Existing silhouette-projected ground shadows (`SanctuaryEnvironmentShadows.drawGroundShadows`) already match real footprints; deprecated ellipse `drawDirectionalShadow34` remains only for kinds without a registered silhouette.
- Map layout / colliders / gameplay: UNCHANGED (render-only change).

## Notifications / loading P28 — DONE
- New `ESBannerNotice` (P28 notice frame) now renders the REAL `bannerFeedback` event string at all 3 sites: SanctuaryLobbyScreen, TowerGateScreens, and the battle feedback pill now uses `ESNoticeFrame(TACTICAL)` keeping tag `tactical_command_feedback_pill`.
- Loading overlay (MainActivity) unchanged from P28: real stage text, offline guest never claims a server connection.

## BEFORE / AFTER engine screenshots (real renderer, Robolectric NATIVE Skia)
- AFTER (this branch): `docs/visual-evidence/p28d/world/` — 9 PNGs: summoning hall at z0.36/z0.88/z1.48 day + z0.88/z1.48 night, hero quarters z1.48 day/night, plaza wide day/night; plus `perf_probe.json`.
- BEFORE (main, pre-foundation): `docs/p27b_next/previews/after/world/` (gate/noon/night z148 etc.) and `docs/visual-evidence/p26/engine-render/` (overview night z036, gate shots) — same sanctuary scenes rendered before the foundation pass.
- Command: `gradle :app:testDebugUnitTest --tests 'com.example.P28DRenderTest'` (run in CI step "P28 Astral Reliquary UI screenshots").

## Performance
- Indicative Robolectric CPU probe (`perf_probe.json`): 1280×720, z0.88, day — avg 22.2 ms, p95 25 ms, min 18 ms, max 25 ms per full world frame (12 frames). This is a CPU/Skia number, NOT device FPS.
- Device FPS p50/p95/p99 on Android: **NOT TESTED ON PHYSICAL ANDROID** (no device in sandbox; the earlier 10 FPS report cannot be re-measured here — not fabricated).
- Memory/thermal: **NOT TESTED ON PHYSICAL ANDROID**.

## Not done / honest debts
- Foundation visuals verified at engine-render level only; on-device look/FPS still needs a physical device run.
- No map layout, camera, collider, or Tower gameplay changes were made.
