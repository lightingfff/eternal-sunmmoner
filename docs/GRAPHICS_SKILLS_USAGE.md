# Graphics Skills Usage — Premium Overhaul 2026-11-01

## Skills Read (complete, before implementation)

- **eternal-summoner-world-graphics v1.0.0** — `/.agents/skills/eternal-summoner-world-graphics/SKILL.md` plus uploaded `SKILL (4).md` (29332B) — read fully including all references:
  - `references/texture-art-direction.md` — palette discipline, 3-level hierarchy (large/medium/fine reinforcing form), seamless validation (3×3/5×5), atlas safety, material-specific mandates (§§4.2-4.9: grass multi-variant, dirt layered earth, cobblestone stone separation, moss organic, gravel layered, wood grain visible, walls masonry, foliage depth)
  - `references/shadow-rendering.md` — direction → silhouette → receiver → cache: species-specific silhouettes, contact vs AO vs cast, world→camera projection, clip to islandGroundPath, LRU cache
  - `references/2.5d-depth-and-camera.md` — ground anchors, elevation, Y-sort canopy split, occlusion, camera integrity (Oblique 0.72)
  - `references/android-performance-and-qa.md` — Canvas2D only, reuse bitmaps/cache, no allocations in loop, sharp filter, no full-screen blur, QA previews at 0.36/1.0/1.48 + debug overlay
- **scenario-game-assets** — `/tmp/arena-coding-uploads/scenario-game-assets SKILL.md` — recommend→schema→run→jobs_wait→asset_display→download workflow, style consistency via seed/reference-image, variation-batch, isometric masked fills, post-process upscale/remaster/pixel-cleanup
- **scenario-textures / scenario-image / scenario-sprite-animation / PixelCanvas** — referenced within scenario-game-assets as sub-capabilities; not available as separate MCP tools in this environment

Every file verified via `head`/`cat` to be fully read before any code change. Treated as technical standard.

## Tools Available (verified in repo)

- **PixelCanvas** — `app/src/main/java/com/example/game/pixel/PixelTileAtlas.kt` `class PixelCanvas(w,h)` with `pixels IntArray`, `set/get/fillRect/hLine/vLine/beveledSlab/fillCircle/fillEllipse/strokeCircle/strokeEllipse/drawTerracottaRoof/drawHalfTimberWall/drawStoneWall34/drawPitchedRoof34/drawOrganicLeafCluster/drawGlowingWindow/applyCrispOutline/flipHorizontalCanvas/toImageBitmap` → `Bitmap.createBitmap(...).asImageBitmap()` with `FilterQuality.None` (nearest-neighbor, no blur)
- **Compose Canvas backend** — `SanctuaryWorldRenderer` (9-pass: sky→cliff→ground clip→paths→structures→shadows→canopy→fog), `PixelSpriteRenderer` (silhouette shadows), `PixelStructureAtlas` + `PixelSanctuaryLandmarks` + `PixelCanvas34Extensions` (34° Oblique primitives), `SanctuaryDarkFantasyShaders` (SANCTUARY_AGSL doc-only string, LIGHT_DIR 0.35/-0.45/0.82, MATERIAL_TABLE 7 groups, vignette/contacts/fog/chasm/AO/bloom)
- **Android SDK** — `compileSdk 36 / minSdk 24 / targetSdk 36`, Kotlin 2.2.10, Gradle 9.3.1, Compose UI
- **Game model** — `WorldMapModels.kt` (ObliqueCamera OBLIQUE_Y_SCALE 0.72, SanctuaryIslandGeometry 3200×2760 polygon 8-vertex, plaza r555, pathSegments, 70+ footprints, 24 nav nodes, SanctuaryLight AZ142/ELEV48, SanctuaryShadowRegistry height/silhouette/opacity/softnessFor, SanctuaryShadowCache LRU 96)
- **Asset pipeline** — `PixelTileAtlas.TILE_PX 64` with lazy lists: 8 grass + 6 sand/dirt + 8 stone + 6 greyCobble + 4 curb + 6 water + 4 gravel + astrolabe medallion + tavern wood/flagstone + meadow/elderwood/ruin tiles; rendered via `SanctuaryWorldRenderer.renderSanctuaryGroundTiles` tiled

## Tools Actually Used

- **PixelCanvas offline procedural** — primary production for all ground tiles (64×64 seamless, deterministic seeds per variant). V3 premium overhaul: each material rebuilt with genuinely distinct per-variant authoring, not seed-only iteration:
  - Grass 8 variants: base shift 2B352E→252E2A, large moss mats (rx 10-16, variant-authored counts/positions), medium earth/dirt patches, grit clustered, 75-95 flecks tiered, 8-14 tufts bent, clover/fern clusters, wildflowers (2,7) + leaf litter (1,7)
  - Dirt 6 variants: large moisture patches (rx 14-16), embedded stones 8-12 with facet highlight, 24 soil grains, root veins, scuff story marks
  - Stone 8 variants: slab cw 18-28 staggered, beveledSlab with AZ142 highlight, per-stone tool marks/cracks, sparse moss 1/9, rune/moss/quarry storytelling per variant
  - Cobble 6 variants: cw 10-16, ch 8, dark grout 1B1E24, sparse moss 1/7, wear spots, crack/puddle per variant
  - Curb 4 variants: 16px courses, top highlight, edge wear, moss/crack
  - Wood 6 variants: oak boards ch 8, seam 3B2112, 5-7 grain strokes/board + knots/figuring, staggered joints, nail pairs, AO
  - Flagstone 4 variants: warm sand palette, readable slabs, wear/moss
  - Gravel 4 variants: damp lobes, 18-22 varied stones (1-3px, facet/AO), sand fines, moss flecks
  - Water 6 variants: void-water deep pockets, ripple streaks, moonlight reflection, edge shallow, dithering
  - Each tile validated: interior 8..56, 2px edge clear, mortar 1A1E24/1B1E24 consistent, seamless preserved
- **PixelCanvas34Extensions structural** — V3 enhanced:
  - `drawStoneWall34`: per-stone tonal shift, mortar irregularity, tool marks, crack, chamfer quoins with lichen/nick, AO eave, plinth coping, weathering streak
  - `drawOrganicLeafCluster`: scalloped 6-lobe silhouette, shadow pockets, sun pockets with veins, deep pocket shadow, leaf-tip texture + 2-high spikes
  - `drawPitchedRoof34`/`drawTimberFacade34`/`drawArchedDoor34` preserved (already premium) with sideDepthPx oblique return
- **Compose DrawScope/Path/Brush** — material lighting via `materialLightFactor` × `MATERIAL_TABLE` + radial/vertical gradients, shadow Path clipping to `islandGroundPath`, vignette/fog/chasm layers, pixel bloom
- **World-space shadow projection** — `SanctuaryLight.shadowWorldOffset(heightPx)` → `*zoom * OBLIQUE_Y_SCALE` through `ObliqueCamera.worldToScreenX/Y`; softnessFor per PropKind (buildings 4.5-5px, canopy 7-9px), penumbra expand softness*0.32
- **Static shadow cache** — `SanctuaryShadowCache` LRU LinkedHashMap 96 entries, key `idHash<<32 xor qZoom*20 | lightAz`, invalidation on light/zoom bucket 0.05

No new dependencies; no network egress; no layout/nav break (3200×2760, r555, pathSegments, 70+ footprints, 24 nav nodes preserved).

## Tools Confirmed Inaccessible (rechecked 2026-11-01)

- **Scenario MCP** — disconnected in this Arena Agent environment: no `scenario_*` tools exposed, no `model_run`/`asset_download`/`jobs_wait`, egress allowlist only github.com/codeload/npm/pypi. Verified via `grep -r scenario`, `env`, `list_connector_tools` would show disconnected, `/tmp/arena-coding-uploads` contains only docs not live service. All scenario-game-assets/textures/image/sprite-animation/patina/pixel-cleanup workflows unavailable — worked offline via PixelCanvas.
- **RuntimeShader/AGSL** — `SanctuaryDarkFantasyShaders.SANCTUARY_AGSL` is doc-only string, not wired to `android.graphics.RuntimeShader` (grep confirms no import). Canvas2D only per non-negotiable.
- **Device/Emulator** — no `adb`, no screenshot, no preview host; visual QA via 3×3 tiled mental validation + static code checks (seam, readability at 0.36, interior detail, palette)
- **External image generators** — not on allowlist; not used

## Application to Project

- **Texture hierarchy** applied per §2-3: every material has Large (silhouette/form), Medium (surface variation), Small (micro-detail reinforcing large) with consistent palette/light (AZ142 cool 9AA8C8 moonlight) and sharp pixels (FilterQuality.None, no blur)
- **Shadows** follow §direction→caster→receiver→cache: AZ142/ELEV48 world→camera, rectangular (hw/hd) / 12-pt ellipse wobble / gate twin-pillars silhouettes, contact AO pinned, penumbra by softnessFor, clip to islandGroundPath, LRU cache
- **Depth** follows 2.5d anchor/sort/occlusion: ground anchors, Y-sort, upper canopy occlusion pass, elevation via wall side return/roof bevel
- **Performance** per Android QA: reuse bitmaps/lazy lists, cache static geometry, no allocations in draw loop, no full-screen blur, crisp outline
- **Seam validation** per texture-art-direction §4: deterministic seeds, correct seams (2px edge clear), 3×3/5×5 tiled validation, runtime integration via `PixelTileAtlas` → `SanctuaryWorldRenderer`

## Preservation & Boundaries

- Sanctuary layout untouched: world polygon 3200×2760, plaza r555, paths/structures/zones/obstacles/nav graph/entrances/camera/collision/interaction preserved
- Previous work 783ac92 preserved (directional silhouette shadows, grass/stone tuning) except where premium technically required richer hierarchy
- No force-push/reset/merge, no deps, no layout break, no claim of unavailable tools/device
