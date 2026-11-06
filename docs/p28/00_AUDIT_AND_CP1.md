# P28 — Premium UI rebuild ("Astral Reliquary") — audit + CP1 (asset pack + component library)

Branch: `arena/149245a4-eternal-sunmmoner` (from `main` @ `f6bd21d`, PR #35 merged).
Task source: `/tmp/arena-coding-uploads/ARENA_P28_UI.txt` + 5 reference images.

## 0. Environment audit (honest)

- **No JDK / Android SDK in this sandbox** → Kotlin cannot be compiled locally; build+tests run in GitHub Actions (Robolectric NATIVE graphics), same pipeline the P27B render evidence used.
- **Image generation IS available** and was used for the original art assets; geometric chrome (buttons, progress bar, icons) and the silver/obsidian frame variants were drawn procedurally with PIL so they share exact geometry with the bronze frame.
- Reference images were read and used as **art direction only**: no frame layout, ornament, face, name, logo or dialogue was copied into any asset. All 22 shipped PNGs are original.

## 1. What exists already (from merged P27B)

- `ui/theme/SanctuaryUi.kt` — ivory sanctuary token layer with measured contrast (debt D).
- `FantasySkin.kt` / `FantasyManagement.kt` — dark-glass panels, `FantasyPanel`, `FantasyAction`, `HeroManagementDialog` (testTags `assign_*`, `inspect_*`, `hero_management_dialog`).
- `PixelHeroPortraitAvatar` + `PremiumPortraits` (authored busts from `premium_slice/portraits/`, cached per hero name).
- Existing testTags preserved: `prep_hero_*`, `tactical_command_feedback_pill`, summon/auth tags — all kept intact.

## 2. CP1 deliverables (this commit)

### Asset pack — `app/src/main/assets/ui_p28/` (22 PNG + manifest.json)

| Group | Assets |
|---|---|
| cards | `hero_card_frame_bronze/silver/obsidian.png` (384×576 RGBA, transparent portrait window), `hero_card_backdrop.png`, `hero_card_nameplate.png` (384×128), `rarity_star_on/off.png` (64×64) |
| notices | `notice_frame_standard/warning/ceremony.png` (288×96, 9-slice inset 24, transparent center) |
| loading | `loading_bg_fantasy.png` (1280×720, quantized 256-color, 645 KB, no text/logo), `eternal_summoner_sigil.png` (256×256 RGBA), `progress_track.png` / `progress_fill.png` (480×48) |
| controls | `button_primary.png`, `button_secondary.png` (288×96, 9-slice inset 24) |
| icons | `icon_event/shield/debuff/retreat/heal/summon.png` (64×64 RGBA) |

`manifest.json` records id, path, use, width/height, transparency, nine-slice inset, sha256 and generation method per asset. Contact sheet with labels: `docs/p28/contact_sheet.png` (asset evidence, NOT an engine screenshot).

### Component library — `app/src/main/java/com/example/ui/components/p28/`

- `ESP28Assets.kt` — manifest-driven resolver; decode via `SanctuaryArtAssets.assetSafe` (missing/corrupt PNG → null, never throws, never cached as failure); per-id memo; `ESTokens` palette/spacing.
- `ESNineSlice.kt` — 9-slice drawn with srcRect/dstRect only (no per-recompose Bitmap crops), clamps when destination < 2×inset.
- `ESHeroCard.kt` — `ESHeroCard` (backdrop→portrait→frame→nameplate/stars→selection outline; frame tier by rarity 1-2 bronze / 3-4 silver / 5 obsidian; name/stars/class from `HeroRecord`, nothing baked) + `ESMiniHeroPortrait` for roster rows.
- `ESNotices.kt` — `GameNoticeUi` adapter model, `NoticeCenter` state owner (dedup by stableId, priority order, cap 8 retained / 3 visible, dismiss), `ESNoticeFrame` (9-slice + dark center fill, vector fallback), `ESNoticeSurface`, `ESNoticeStack`.
- `ESLoading.kt` — `ESLoadingState` (Hidden/Indeterminate/Determinate/Failed) + `ESLoadingOverlay`; fraction is clamped and NaN-guarded, only ever fed from measured progress; no fake 0→100%, no "connecting to server" text in offline/guest mode.

### Wiring into real screens (callbacks and testTags unchanged)

- `MainActivity.kt` — bootstrap spinner replaced by `ESLoadingOverlay(Indeterminate(real stage text))`.
- `TowerGateScreens.kt` — party prep rows use `ESMiniHeroPortrait` (tag `prep_hero_*` and toggle behavior untouched).
- `FantasyManagement.kt` — hero management rows use `ESMiniHeroPortrait` (`assign_*`/`inspect_*` untouched).
- `TowerBattleScreen.kt` — `tactical_command_feedback_pill` now renders inside `ESNoticeFrame(TACTICAL)`; tag and message text unchanged.

### Tests — `app/src/test/java/com/example/ui/P28ComponentsTest.kt` (Robolectric, NATIVE graphics)

1. Manifest lists all 22 assets; every PNG decodes from assets at manifest dimensions; frames/stars/icons have real alpha.
2. Missing asset id → null, no throw (fallback contract).
3. NoticeCenter dedup by stableId / priority order / retention cap / dismiss.
4. Loading overlay renders real stage text, error tag on Failed, nothing on Hidden.
5. ESHeroCard binds hero name from `HeroRecord` (no baked text).

## 3. Status table

| Item | Status |
|---|---|
| Design tokens (ESTokens) | TRUE (extends merged SanctuaryUi) |
| PNG asset pack + manifest + checksums | TRUE (22 assets, contact sheet) |
| Asset resolver/cache/fallback | TRUE (unit-tested contract) |
| 9-slice renderer | TRUE (code + inset metadata) |
| Hero card (full) | PARTIAL — component done + unit test; not yet swapped into the collection grid UI (kept list rows to protect existing tests) |
| Party grid | PARTIAL — framed portraits wired into prep rows; full card grid pending |
| Notifications | PARTIAL — queue/frames/stack done + unit tests; only command-feedback wired; battle event-feed adapter pending |
| Loading/Auth | PARTIAL — overlay wired at root bootstrap with real stage text; auth screen visuals pending |
| Battle HUD | PARTIAL — feedback pill re-framed; roster/objective ribbons pending |
| assembleDebug / full CI | NOT TESTED locally (no JDK) — runs in Actions on push |
| Device FPS p50/p95/p99, thermal | NOT TESTED (no physical device) |
| Engine screenshots BEFORE/AFTER | NOT TESTED — no device/emulator here; Robolectric render only |

No auto-merge. PR opened from this branch for review.
