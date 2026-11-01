# World Graphics Discovery — 2026-11-01 (Skill-Grounded)

> Branch: `arena/e82ef083-eternal-sunmmoner` @ `cc291ce` (PR #4 merged). Inspector: Argent Agent Mode. Skill: `eternal-summoner-world-graphics` v1.0.0 + refs: texture-art-direction, shadow-rendering, 2.5d-depth-and-camera, android-performance-and-qa.

## 1. Repository & Tool Discovery

- **Workspace:** editable, `git status` clean, no pending user edits. `main` at `cc291ce` includes house-interaction + shadow registry PR #4.
- **App entry:** `app/src/main/java/com/example/MainActivity.kt` → `SummonerApp` → `SanctuaryLobbyScreen` (compose).
- **World renderer:** `SanctuaryWorldRenderer.drawSanctuaryFloatingWorld` (Canvas2D, exclusive for `SANCTUARY_ASTRAL`). Delegated from `PixelSpriteRenderer.drawWorldBackground` when `biome == SANCTUARY_ASTRAL`.
- **Screen renderer:** `SanctuaryLobbyScreen.kt` (1768L) — gesture handling, camera, Y-sorted `LobbyDrawableItem` (Prop vs HeroNpc), HUD, modals.
- **Atlas loaders:** `PixelTileAtlas.kt` (1004L, 64×64 procedural `PixelCanvas` → `ImageBitmap` with `FilterQuality.None`), `PixelStructureAtlas.kt` (857L, landmark/building/tree sprites from `PixelSanctuaryLandmarks.kt` 611L + `PixelCanvas34Extensions.kt`), `SanctuaryAssetManager.kt` (expansion assets, bypassed — darkened fallback).
- **Map model:** `WorldMapModels.kt` (832L) — `ObliqueCamera` (OBLIQUE_Y_SCALE 0.72, GROUND_ELLIPSE_RATIO 0.66, worldToScreenX/Y), `SanctuaryIslandGeometry` (3200×2760, 8-vertex polygon, wallMargin 120f, plaza 1940,1240 r555), `WorldObstacle` (x,y = front-ground anchor, footprintHalfW/footprintDepthNorth, elevationZ, sortYBias, occludesUpperCanopy), `SanctuaryHouseEntrances`, `SanctuaryLight` (AZ 142°, ELEV 48°, SHADOW_DIR 0.615,0.788, TAN_ELEV 1.11), `SanctuaryShadowRegistry`, `DepthBand`, `CollisionPhysics`.
- **Lobby engine:** `SanctuaryLobbyEngine.kt` (nav 24 nodes incl. 4 doorway leaves, `selectFacilityAt` doorway-priority, debounce 600ms).
- **Shaders:** `SanctuaryDarkFantasyShaders.kt` (334L) — AGSL stub, `MaterialProps` table (stone/wood/metal/grass/moss/water/magic), vignette, contact vs cast, bloom, 4 fog layers, dithering, but Canvas-only (no RuntimeShader).
- **Graphics tests:** `firestore.test.js` only; no Kotlin screenshot/graphics tests. `ANDROID_STABILITY_REPORT.md` exists.

### Tool Availability

- **Offline PixelCanvas:** present `PixelCanvas(width,height)` with `set/fillRect/fillEllipse/strokeEllipse/beveledSlab/drawGlowingWindow/applyCrispOutline/toImageBitmap`. Verified — 64×64 seamless generation is available.
- **Scenario MCP:** not connected (no `scenario` tool in arena; `list_connector_tools` would show). Cannot use Scenario texture models; must use PixelCanvas only.
- **Image processing:** Compose `ImageBitmap`, `FilterQuality.None` (nearest-neighbor), `Brush.radialGradient/verticalGradient`, `Path`, `clipPath`, `withTransform`. No GPU shader pipeline, no RenderScript, no AGSL runtime in current renderer.
- **Build:** Android Gradle 9.1.1, Kotlin 2.2.10, compileSdk 36.1 minSdk 24 target 36, Compose. Build command ` ./gradlew` missing wrapper exe on this image but `./gradlew` via `gradle wrapper` is standard; non-destructive `assembleDebug` available when requested.

### Rendering Backend

- **Android Canvas / bitmap renderer** (Jetpack Compose `DrawScope` over `Canvas`). Verified: all draws are `drawRect/drawOval/drawPath/drawImage/drawCircle` with `FilterQuality.None`. No OpenGL/Vulkan, no `RuntimeShader` active (AGSL string is doc only). Blend modes are default `SrcOver`; no custom `BlendMode` beyond `Brush` gradients. Offscreen layers via `clipPath` + `drawCircle(brush=radialGradient)`.

## 2. Renderer Pass Map (Verified Order)

**SanctuaryLobbyScreen frame (Compose `Canvas`):**
1. `drawWorldBackground` → `SanctuaryWorldRenderer.drawSanctuaryFloatingWorld`:
   - 1a sky gradient (0D0F1A→181B2E→242742→2E314F) + moon glow (8A9AB8 α0.08) + drifting clouds + floating islands + cliff underside + grass rim
   - 1b `clipPath(islandGroundPath)` — tiled grass (sanctuaryGrassTiles 8 variants hash), organic sand arena (3-layer wobble + clip + sand tiles), residential garden courtyards, curbed cobble roads (curb outer 1E2028 + limestone 3A3D4A + cobble clip), circular plaza (rings + runes + staircase), AO (wall/plaza/gate/tree dither)
   - 1c southern gate platform/bridges, back/side fortress walls + north tunnel, expansions (locked/bridged), moonlight rays (rotated rects), local lights (brazier/rune/window bloom + vignette + 4 fog + chasm)
2. Zone floor rings (`drawSanctuaryZoneFloorRing`) — stroke ellipse, pulse
3. Tap destination rune
4. **Y-sorted drawables** `sortedBy { sortY }` where `Prop.sortY = obs.y + sortYBias` and `HeroNpc.sortY = actor.y` — interleaved `drawProp` + `drawCharacterSprite`
5. `drawUpperCanopyOcclusionPass` (oak/mystic/pine/sakura topFraction 0.52–0.56)
6. `drawSouthernForegroundWalls` (SANCTUARY_ASTRAL only)
7. Landmark plaques + name pills + active speaker bubble
8. `drawForegroundAtmosphere` (particles, light shafts, canopy sway)

**Inside `drawProp` per `obs.propKind` (≈38 branches):** `drawDirectionalShadow34`/`drawCastersShadow` → glow → `drawCrispSprite` (2.0× scale). Character: `drawOval` contact + `drawCrispSprite` + HP bar.

- **Pass order correct per skill:** background → ground tiles → static objects → dynamic shadows (weak) → characters → foreground occluders → local lights/fog → UI. Receivers: ground/paths/plaza; walls not receiving shadows (clipped). No Z-buffer.

## 3. Coordinate & Height Contracts (Verified)

- **World origin:** top-left (0,0), **Y+ = South** (south larger Y). Island polygon clockwise (980,340)→(2240,340)→(2900,880)→(2900,1780)→(2320,2360)→(900,2360)→(300,1780)→(300,880).
- **Ground anchor:** `WorldObstacle.x,y` = front-most ground contact (south edge of footprint). `footprintHalfW` × `footprintDepthNorth` rect: `left=x-halfW, right=x+halfW, top=y-depth, bottom=y`. Verified in `CollisionPhysics.resolvePosition`.
- **Elevation:** `elevationZ` rarely used (0 most, 30f plaque, 84f speech bubble, cliff walls via `sy(wy,z)`). `ObliqueCamera.worldToScreenY(..., elevationZ)` lifts vertically: `((wy-camY)*OBLIQUE_Y_SCALE - elevationZ)*zoom + halfH`.
- **Sprite anchor:** `drawCrispSprite(cx, footY=cy+6*s, pixelScale=2*s)` — footY is `sy(y) + 6*s`, image bottom aligned to foot. No top-left sort.
- **Camera:** `worldToScreenX/Y` and `screenToWorldX/Y` are inverses, used consistently for pan/zoom (`-pan.x/zoom`, `-pan.y/(zoom*OBLIQUE_Y_SCALE)`), tap (`screenToWorldX/Y`), and hero draw. No duplicate math.
- **Layout locked:** 3200×2760, WALL_MARGIN 120f (interior 82f), plaza 1940,1240 r555, 8 PathSegments spine, 7 SANCTUARY_ZONES, 70+ obstacles — preserved since `SANCTUARY_BASELINE.md`.

## 4. Baseline Visual Defects (Source Inspection, Not Device)

> **Not tested on device** — Agent Mode has no emulator/preview. Findings are from code reading + `GRAPHICS_AUDIT.md` + prior PR diffs.

| Category | Current | Skill Reference | Gap |
|----------|---------|-----------------|-----|
| **Shadows — generic ellipse** | `drawDirectionalShadow34` hand-tuned halfW/halfH per site, fixed +8,+6 offset legacy (now heightProxy directional but still ellipse-core). `drawCastersShadow` exists but few branches use it; all cast shadows are still `drawOval` ellipses (2–3 ovals) | `shadow-rendering.md` § core distinction: silhouette-based (polygon/alpha/parts), contact vs cast vs AO distinct, directional `offsetXY = -h*Lx/Lz` | **P1** — No polygon caster for roofs/walls, no irregular alpha for canopy, no rock contour; all share ellipse; contact AO not footprint-matched for buildings |
| **Shadows — softness/tint** | Alpha 0.28–0.50, black outer; no per-part softness, no material-tint | § pixel style: keep contact defined, soften cast only as light source, material-aware tint | Moderate — uniform, too dark in places |
| **Shadows — receivers** | Shadows drawn before sprite but inside Y-sorted pass → may be occluded by nearer props; wall receiver not masked | § receivers: ground/path only, clip at wall, no bleed through walls/UI | Moderate — bleed risk on south cliff |
| **Shadows — caching** | Per-frame `drawOval` with new `Offset/Size`, no mask cache; `SanctuaryShadowRegistry` computes height/opacity per frame but not geometry | § cache policy: cache static masks, rebuild on change | Minor — 78×2 ovals ~156 draws, OK but no allocation saving |
| **Textures — grass** | `sanctuaryGrassTiles` 8 variants: base 2B352E + 11 moss patches + earth + 26 grit + 135 specks + 15 tufts + clover/flowers/mushroom — dense, somewhat noisy, dark-fantasy muted palette | `texture-art-direction.md` grass/foliage: layered clumps, dark interior, mid clumps, sparse light tips, breathing room | **P1** — Value masses okay but grit overly dense, seam risk at 0/63 edge, flower placement may align across tiles (repetition), highlight sparse vs desaturated OK |
| **Stone/cobble** | `sanctuaryStoneTiles` beveledSlab 3A3F4A/444A56 + mortar 1A1E24 + cracks/lichen; `sanctuaryCausewayTiles` 8×8 cobble 3E424D etc. + curb | stone/cobble: readable slabs, varied edges, darker joints, selective cracks, top-plane highlights | OK — slabs readable, joints dark, wear selective, but top highlight subtle; edge matching good (hash stagger) but curb joint could show motif repeat |
| **Water/gravel** | `sanctuaryWaterTiles` deep navy 0F1A2E + wave hLine ripples + moon highlight; `sanctuaryGravelTiles` earth 2B2620 + varied radii | water: depth regions, restrained ripples, controlled reflections | Moderate — water outside Sanctuary void only; gravel underused |
| **Wood/metal** | `tavernWoodPlankTiles` plank grain/hi/shadow + knot + scuff; metal via `4E5A6A` specular | wood: plank direction, grain groups, joins, edge wear; metal: compact specular | OK — plank aligned, wear restrained |
| **Buildings/trees** | `PixelSanctuaryLandmarks` + `PixelStructureAtlas` — oak with buttress roots, bark furrow, 6 leaf clusters + frost; pine tiers; sakura 6 clusters; cottage half-timber with timber grain | texture brief: silhouette/layout → base → medium → material response → wear → accents → tiling → in-game test | Good — silhouettes readable, material response present, but roof tile thickness not emphasized, window bloom moderate |
| **Depth — anchors** | `sortY = y + sortYBias` for props, `y` for heroes; `occludesUpperCanopy` true for 4 tree kinds → second pass | `2.5d-depth-and-camera.md` depth sorting: sort by ground anchor + stable tie-break, split only when needed | OK — correct for most; tree split exists, building rear/front not split (not needed; footprint 54f small) |
| **Depth — occlusion** | Upper canopy draws after heroes (0.52–0.56 topFraction); southern foreground walls after heroes | Same: trunk vs canopy, rear roof vs front facade split when walk-around | Good — hero can pass behind trunk/in front of foliage; wall occludes near rim |
| **Depth — camera** | One `ObliqueCamera` used for world, heroes, shadows (now via `SanctuaryLight`), particles | One shared transform, pan/zoom invariance | Good — shadows via `SanctuaryLight.shadowWorldOffset` then `*zoom*OBLIQUE_Y_SCALE` matches camera |
| **Fog/atmosphere** | `drawGroundFog` vertical 242742→1A1F33→0D0F1A, `drawChasmDepth` radial 0B0814→181B2E, moon rays diagonal, plus `BackgroundHaze/MidgroundFog/LocalMist/DistantFade` (4 layers) | atmosphere: distant lower contrast/saturation toward haze, mid crisp, foreground framed, no full-screen blur | Moderate — 4 layers present but haze α0.05–0.06 restrained, may under-read at far zoom |
| **Local lights** | Brazier radial 90*zoom + `drawPixelBloom` 22*zoom core sharp outer soft; plaza rune 160*zoom; window 70*zoom | local lights: world position, color, radius, falloff, masked, restrained glow, core crisp | OK — restrained, core sharp, not flat circle |
| **Performance** | Atlas lazy `List<ImageBitmap>` (8+6+8+6+4+6+4 ≈ 800KB est), no per-frame decode, `clipPath` per frame (island + roads), 78 ovals shadows + 20 hero steps, no offscreen bitmap cache | android-performance: no per-frame alloc, cache static, reuse paths, cull safe, avoid full-screen passes, profile | Good — no bitmap alloc in loop, but `clipPath` for island is full-screen path each frame; shadow ovals allocate `Offset/Size` each call (minor) |

## 5. Non-Negotiable Checks

- [x] Sanctuary geometry not changed (POLYGON_VERTICES 8 pts, wallMargin 120f, plaza r555, paths, 70+ obstacles footprints, nav 24 nodes, camera OBLIQUE_Y_SCALE 0.72)
- [x] Renderer inspected first (Canvas2D, not GPU)
- [x] No new dependencies required
- [ ] **Failing:** No generic ellipse shadows — currently still ellipse-dominant (must fix)
- [x] Pixel sharpness via `FilterQuality.None` + `applyCrispOutline` kept
- [x] No decorative PBR maps

## 6. Tool Gaps & Mitigation

- Scenario MCP offline → use PixelCanvas only for texture work; verify seamless via 3×3 grid mental check + edge continuity (keep patches 8..56 interior, beveledSlab handles edges)
- No device/preview → mark visual QA **Not tested**; build-only verification via `assembleDebug` when available; code inspection + hash diversity checks substitute

## 7. Immediate Plan (Bounded, Layout-Preserving)

1. **Shadows — silhouette + caching (P1):** Extend `SanctuaryShadowRegistry` with per-kind polygon vs alpha dispatch; add `SanctuaryShadowCache` (Lru for static paths/masks); rewrite `drawCastersShadow` to draw rect polygon for buildings/walls, irregular wobble path for canopy/bushes, narrow capsule for pillars/rocks; keep contact ellipse footprint-matched; draw shadows in dedicated ground-receiver pass before Y-sorted props (or keep per-prop but clip at wall). Invalidate on position/scale/light change.
2. **Textures — refine grass + stone (P1):** Keep IDs/padding/filtering; adjust `createSanctuarySunlitGrassTile` to reduce grit density (26→18 where noisy), ensure seamless (wrap patches + edge 2px mortar), hierarchy large patches → medium clumps → small frost tips (already) but remove uniform speck where overlapping tufts; adjust `createSanctuaryStoneTile` top-plane highlight stronger on sun-facing row.
3. **Depth — keep anchors, add debug overlay toggle (P2):** No anchor move; add dev `drawDebugAnchors` (ground anchor dot + footprint rect + height line + light dir) behind debug flag, removable.
4. **Performance — cache paths (P2):** Cache islandGroundPath, roadUnionPath, shadow paths; reuse `Path()` instances via pool.

Files to touch: `WorldMapModels.kt` (shadow registry + cache key), `PixelSpriteRenderer.kt` (caster dispatch + cache + shadow pass), `SanctuaryWorldRenderer.kt` (receiver clip + AO tuning), `PixelTileAtlas.kt` (grass/stone refinements), `SanctuaryDarkFantasyShaders.kt` (blend AO), `SanctuaryLobbyScreen.kt` (shadow pass order), docs. Preserve: polygon, plaza, paths, obstacles, nav, camera.
