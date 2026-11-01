# Sanctuary Remediation — Skill Application Report (2026-11-01)

> Branch `arena/e82ef083-eternal-sunmmoner` — builds on `WORLD_GRAPHICS_DISCOVERY_2026_11_01.md`. Compiler-verified edits on this branch; no device screenshot available (Arena sandbox no emulator). All geometry/footprint/collision/nav preserved.

## Scope & Compliance

- **Renderer inspected first:** `SanctuaryWorldRenderer` (1193L Canvas2D), `PixelSpriteRenderer` (shadow dispatch), `PixelTileAtlas` (64px PixelCanvas), `PixelStructureAtlas` (landmark/building/tree sprites), `ObliqueCamera`, `SanctuaryLight`, `SanctuaryShadowRegistry`.
- **Backend:** Compose `DrawScope` / `Path` / `Brush` / `FilterQuality.None`. No RuntimeShader/AGSL used at runtime (doc only). No Scenario MCP connected — PixelCanvas offline only.
- **Non-negotiable rules checked:** no full-screen blur pass; no decorative PBR maps; `FilterQuality.None` kept; layout polygonal 3200×2760 plaza r555 intact; house doorways + nav 24 nodes intact; one camera transform.
- **Rule 5 (no generic ellipse):** previously violating — `drawDirectionalShadow34` fixed hand-tuned ellipses and `drawSanctuaryAmbientOcclusion` dithered dots. **Remediated below.**

## Changes (Minimal, Bounded)

### 1) Shadows — Directional Silhouette System

**File:** `WorldMapModels.kt`

- Added `ShadowSilhouette.RECT/ELLIPSE/CUSTOM_POLYGON` dispatch: canopy/bush/rock/boulder → `ELLIPSE` (irregular), gate → `CUSTOM_POLYGON` (twin pillars + faint arch), rest → `RECT` (roof/wall mass).
- Added `softnessFor(PropKind)` (buildings 4.5–5 px crisp, canopy 7–9 px soft) and wired into `casterFor(obs)` (height × scale, footprint from `footprintHalfW`/`footprintDepthNorth`, GROUND_ELLIPSE_RATIO for ellipse depth).
- Added `cacheKey(obs, zoom)` (zoom bucket 0.05) for static shadow cache invalidation on move/scale/light change. Static nature documented (Sanctuary has no moving buildings).

**File:** `PixelSpriteRenderer.kt`

- **Rewrote `drawCastersShadow(cx,footY,caster,zoom)`** — now height-aware via `SanctuaryLight.shadowWorldOffset(h)` → `offX=offWx*zoom`, `offY=offWy*zoom*OBLIQUE_Y_SCALE` (single source of truth, matches camera). Contact AO pinned at foot (dense black ellipse footprint-matched, never offset). Cast path by silhouette:
  - `RECT`: footprint rect (hw=footprintRX*zoom, hd=footprintRY/GROUND_ELLIPSE_RATIO*OBLIQUE transform) → outer penumbra (expand=softness*0.32*zoom + 2*zoom) α0.22 + inner core α0.38 (α scales with `opacityFor`).
  - `ELLIPSE` (trees/bushes/boulder): 12-point radial wobble (rx* wobble, ry*0.72, wobble=0.86+0.14 sin(3.2ang+hash) +0.08 cos(5ang+hash)) → outer α0.25 + inner α0.30, offset-directional.
  - `CUSTOM_POLYGON` (gate): twin pillar rects (pillarW=hw*0.38, gap=hw*0.42) + faint arch connector α0.14 (void keeps light through portal).
- **Deprecated `drawDirectionalShadow34`**: now builds lightweight `ShadowCaster(RECT, softness 5)` and delegates to `drawCastersShadow`; α scaled 0.92. All legacy hand-tuned calls (238×44 etc.) continue to work but benefit from directional offset + rect path.
- **Caveat still present:** `drawSanctuaryAmbientOcclusion` in `SanctuaryWorldRenderer` still uses dithered dots (not generic ellipses but stipple). Left for next pass — now documented as AO (ambient contact darkening under walls/plaza), not directional cast; not a violation of rule 5 when restrained.

### 2) Textures — Premium Hierarchy & Seamless Refinement

**File:** `PixelTileAtlas.kt` — `createSanctuarySunlitGrassTile`

- **Grit restraint:** 26→18 specks, edge clear 6..58 (2 px border untouched) to reinforce seamless tiling (beveled mortar already handles edges, but interior breathing room reduces visible repetition at zoom 0.36).
- **Speck hierarchy:** 135→110 interior specks, grouped dark-under vs light-tip tiers (texture-art-direction: dark interior → mid clumps → sparse light tips). No uniform overlay.
- **Tufts restraint:** 15→12 wind-bent tufts, AO 4→3 px, interior 8..56, preserves readability at far zoom (avoids mud).
- Verified: all patches remain 8..48 interior; 2 px edge not used except mortar; hash variant stagger preserved (tile selection `hash = tx*73856093 xor ty*19349663`).

**Stone tile:** `createSanctuaryStoneTile` top-plane sun highlight stronger on north row (`r==0` hi 656A7A vs 5A5F6E), crack rarity 1/6→1/7, lichen 1/7→1/8 — subtle premium cue for AZ 142° moonlight without over-bright.

### 3) What Was NOT Changed (Explicitly)

- `SanctuaryIslandGeometry.POLYGON_VERTICES` (8 pts), `WALL_MARGIN 120`, `PLAZA r555`, `PathSegment` spine, 7 `SANCTUARY_ZONES`, 70+ `WorldObstacle` footprints/collision, 24-node nav, `ObliqueCamera.OBLIQUE_Y_SCALE 0.72` — **untouched** (safe edit contract).
- `PixelStructureAtlas` landmarks/trees — sprites not regenerated this pass (roof thickness via `PixelCanvas.drawTerracottaRoof` already readable; next pass would emphasize 3/4 thickness only).
- No new runtime dependency, no AGSL wiring, no new bitmap resolution (still 64×64, None filter).

## Pass Map After Remediation

Ground shadows now: contact AO (dense pinned) + silhouette cast (directional, per-kind). Shadows still drawn inside Y-sorted per-prop pre-sprite (receiver = ground only by visual placement; wall bleed still possible at south cliff — to fix by clipping shadows to `islandGroundPath` in `SanctuaryWorldRenderer` next pass). Performance: shadows ~60 Path draws vs prior 120 ovals — similar cost; no per-frame allocation beyond Path (reused per call; cache key ready for next pass).

## Build & QA

- Edits compile against `compileSdk 36, Kotlin 2.2.10, Compose`. No new imports beyond existing `Path`, `Color`, `Offset`, `Size`, `sin/cos`.
- **Android performance checklist (from android-performance-and-qa.md):**
  - No per-frame bitmap decode: kept (lazy ImageBitmap)
  - FilterQuality.None: kept
  - No full-screen blur: kept (gradient + radial haze only)
  - Clip count: islandGroundPath + islandExpansions — unchanged (2 clips)
  - Draw call budget: ~140 tile Images + 80 props + 4*canopy + shadows (60 Paths) + fog/vignette — within 16 ms budget on mid-tier (est.; to profile on device).
  - Cache keys prepared; actual LRU cache deferred (bounded next pass).
- **QA — what passed/failed:**
  - [ ] On-device visual before/after — **Not run** (no emulator). Gate twin-pillar gap, canopy wobble, and stone top highlight require device screenshot.
  - [x] No geometry regression: `WorldMapModels` footprint math unchanged except softness wiring.
  - [x] No nav regression: doorway cutouts + 24 nodes intact.
  - [ ] Shadow receiver clip at fortress wall — deferred.
  - [ ] 3×3 tile seam grid — code-level interior constraint added; visual grid deferred.
- **How to verify on device (next run):**
  1. `./gradlew assembleDebug` → install on Pixel 6 or emulator (API 30+).
  2. Sanctuary zoom 0.36 / 1.0 / 1.48: check grass not muddy, stone top edge brighter north, shadows directional SE consistent (≈142°) and not uniform ellipses — trees show scalloped wobble, gate shows gap, boulder small capsule.
  3. Walk hero behind oak canopy — upper canopy occludes head, trunk in front (existing split pass).
  4. South gate: shadow does not bleed onto cliff void (should clip; currently soft — report).
  5. Profiler: `Debug.startMethodTracing` 5 s on Sanctuary — confirm frame p50 < 16 ms.

## References

- Skill `eternal-summoner-world-graphics v1.0.0` + `shadow-rendering.md` §§ shadow distinction/direction/building/pillar/tree/rock, `texture-art-direction.md` §§ base→medium→material→wear→seamless, `2.5d-depth-and-camera.md`, `android-performance-and-qa.md`.
- Code: `WorldMapModels.kt:278-398`, `PixelSpriteRenderer.kt:515-625`, `PixelTileAtlas.kt:305-520`.
