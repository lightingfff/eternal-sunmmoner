# P22 — Vegetation Runtime & Asset Inventory (Verified at `main` `8cb16d8`)

> Phase A audit — measured directly from active repository source and packaged PNG bytes before any P22 art writes.

## 1. Engine & Projection Contract

- **Projection:** `ObliqueCamera.OBLIQUE_Y_SCALE = 0.72f` (upright sprites keep 1:1 vertical height; ground Y foreshortened by `0.72`).
- **Sampling:** `FilterQuality.None` nearest-neighbor in `PixelSpriteRenderer.drawCrispSprite` and `drawCrispUpperCanopyPortion`.
- **Tree ground anchoring (`ObliqueSpriteGeometry`):**
  - `isTree(kind)` is `true` for `OAK_TREE`, `MYSTIC_PLUM_TREE`, `PINE_SPIRIT_TREE`, `SAKURA_BLOSSOM_TREE`, `CYPRESS_CONIFER_TREE`.
  - `bounds(image)` scans `alpha >= 128` for `top` and `bottom`; `bottomPadding = imageHeight - 1 - bottom`.
  - `drawFootOffset(kind, image) = bounds(image).bottomPadding * pixelScale(kind)` for trees (`pixelScale = 2.05f` for `OAK_TREE`/`MYSTIC_PLUM_TREE`, `2.0f` for `SAKURA_BLOSSOM_TREE`/`PINE_SPIRIT_TREE`/`CYPRESS_CONIFER_TREE`).
  - Because `drawCrispSprite` places canvas bottom `H` at `cy + drawFootOffset * s` (`s = zoom * obs.scale`), the lowest `alpha >= 128` row (`bottom`) lands **exactly** on `cy = worldToScreenY(obs.y)`. Changing `bottomPadding` would shift the visual foot relative to `SanctuaryVegetation` / manifest ground anchors, so P22 preserves the exact `bottom` row (`alpha >= 128`) of every tree and shrub.
- **Shrub ground anchoring (`GARDEN_HEDGE_BUSH`, `FOREST_CANOPY_BUSH`):**
  - `isTree(kind)` is `false`; `pixelScale` is `1.9f` (`GARDEN_HEDGE_BUSH`) and `2.0f` (`FOREST_CANOPY_BUSH`); `footOffset` is `4f`.
  - Procedural `SanctuaryVegetation.shrub()` is `64×40` with `alpha >= 128` bounds `x=[4..60], y=[7..36]` (`bottomPadding = 3`).
  - `SanctuaryEnvironmentShadows.geometry` computes `foot = (4f - bottomPadding * scale) * obs.scale`; preserving `64×40` canvas and `bottom = 36` (`bottomPadding = 3`) keeps sprite foot and cast shadow contact invariant.
- **Upper canopy occlusion (`PixelSpriteRenderer`):**
  - Production lobby (`SanctuaryLobbyScreen.kt:636`) and battle (`TowerBattleScreen.kt:566,600,620`) invoke `drawActorCanopyOcclusion` after each actor draw: clips `topFraction = 0.52f` for `PINE_SPIRIT_TREE` and `0.56f` for all other trees when an actor stands south of `obs.y` within the crown overhang window (`actorWorldY <= obs.y + bmp.width * scale * 0.18f`).
  - `drawUpperCanopyOcclusionPass` uses `topFraction`: `OAK_TREE = 0.54f`, `MYSTIC_PLUM_TREE = 0.54f`, `PINE_SPIRIT_TREE = 0.52f`, `SAKURA_BLOSSOM_TREE = 0.56f`.
  - Actors north of `obs.y + obs.sortYBias` sort before `drawProp(obs)` and are occluded naturally by the sprite silhouette.
- **Directional cast shadows (`SanctuaryEnvironmentShadows`):**
  - Key light upper-left (`SanctuaryLight.AZIMUTH_DEG = 142f`, `ELEVATION_DEG = 48f`, shadow cast toward SE).
  - `silhouette(bitmap)` caches one horizontal scanline-quad `Path` per `ImageBitmap` (`alpha >= 128`); `casters` caches `CasterGeometry` per `WorldObstacle` in a `WeakHashMap`. No extra draw pass per tree.

## 2. Pre-P22 Asset & Runtime Presence Table

| Asset ID / Path | `PropKind` | Loader (`PixelStructureAtlas`) | Canvas `W×H` | `alpha>=128` bbox (`x`, `y`) | Foot row / `bottomPadding` | Pre-P22 SHA-256 | Canopy `topFraction` (pass / actor) | Sanctuary instances (`buildSanctuaryMap`) | Tower Floor instances (`buildTowerFloorMap`) | P22 Scope |
|---|---|---|---|---|---|---|---|---|---|---|
| `sanctuary_p03/canopy_a.png` | `SAKURA_BLOSSOM_TREE` (`hashCode & 1 == 0`) | `SanctuaryArtAssets.image("canopy_a") ?: fallback` | `114×132` | `x=[3..107]`, `y=[0..126]` | `126` / `5` | `c7f35ea681eff667da52af939b5e48d24cfa3c7937159fd122b9fb0cd16e51c0` | `0.56` / `0.56` (row `74`) | **9** (`sanctuary_sakura_1,3,5,7,9,10,12,14,16`) | 0 | **Repaint (Phase B Vertical Slice)** |
| `sanctuary_p03/canopy_b.png` | `SAKURA_BLOSSOM_TREE` (`hashCode & 1 == 1`) | `SanctuaryArtAssets.image("canopy_b") ?: fallback` | `96×114` | `x=[4..79]`, `y=[0..109]` | `109` / `4` | `ae452536bc8c4b6c580290d9454b3bd4bc787666cfc4f30e5823eb9c9bf73b1b` | `0.56` / `0.56` (row `64`) | **9** (`sanctuary_sakura_0,2,4,6,8,11,13,15,17`) | 0 | **Repaint (Phase B Vertical Slice)** |
| *(procedural `shrub()`)* → `sanctuary_p03/hedge_bush.png` | `GARDEN_HEDGE_BUSH` | `assetSafe("sanctuary_p03/hedge_bush.png") ?: shrub()` | `64×40` | `x=[4..60]`, `y=[7..36]` | `36` / `3` | *(procedural pre-P22)* | N/A (not `isTree`) | **5** (`sanctuary_hedge_bush_0..4`) | 0 | **Authored PNG-first + safe fallback (Phase B/C)** |
| `sanctuary_p03/cypress.png` | `CYPRESS_CONIFER_TREE` | `SanctuaryArtAssets.image("cypress") ?: fallback` | `48×88` | `x=[4..42]`, `y=[3..87]` | `87` / `0` | `442cbc617c6d61f79f2105f7c4b2072a2fc721fa3a4e56ee11d695d576cf988b` | — / `0.56` | **3** (`(1060,485)`, `(1155,470)`, `(1485,655)`) | 0 | **LOCKED CONTROL (P20) — DO NOT TOUCH** |
| `sanctuary_p03/pine.png` | `PINE_SPIRIT_TREE` | `SanctuaryArtAssets.image("pine") ?: fallback` | `96×128` | `x=[20..74]`, `y=[0..122]` | `122` / `5` | `d421e5b36eb1fdb5a73c7028f5a3904536d15d8a432b1c005310c755af847b7a` | `0.52` / `0.52` (row `67`) | **0** *(absent from Sanctuary)* | **Floor 1 & 2** (`f1_spawn_grove_1` @ `(430,920)`, `f1_spawn_grove_5` @ `(790,1245)`, border & woodland) | **Repaint (Phase C — Tower runtime verified)** |
| `sanctuary_p03/twisted.png` | `MYSTIC_PLUM_TREE` | `SanctuaryArtAssets.image("twisted") ?: fallback` | `112×132` | `x=[2..102]`, `y=[1..126]` | `126` / `5` | `c739f681811a0ad765fb4645f32e3260b5622f5af7c1f49a65d965d0c3c7204b` | `0.54` / `0.56` (row `71`/`74`) | **0** *(absent from Sanctuary)* | **Floor 2** (`ELDERWOOD_FOREST`, `f2_obs_*` where `i % 6 == 1`) | **Repaint (Phase C — Tower runtime verified)** |
| *(procedural `shrub()`)* → `sanctuary_p03/forest_bush.png` | `FOREST_CANOPY_BUSH` | `assetSafe("sanctuary_p03/forest_bush.png") ?: shrub()` | `64×40` | `x=[4..60]`, `y=[7..36]` | `36` / `3` | *(procedural pre-P22)* | N/A (not `isTree`) | **0** *(absent from Sanctuary)* | **Floor 1 & 2** (`f1_spawn_grove_3` @ `(460,1255)`, woodland) | **Authored PNG-first + safe fallback (Phase C)** |
| `sanctuary_p03/oak.png` | `OAK_TREE` | `SanctuaryArtAssets.image("oak") ?: fallback` | `112×136` | `x=[13..92]`, `y=[9..134]` | `134` / `1` | `556eefd5c1b1f14872b67e6e651b8c7d6645e4632519d0719a6ce614b240a229` | `0.54` / `0.56` | **0** *(absent from Sanctuary)* | **Floor 1 & 2** (`f1_spawn_grove_0` @ `(210,945)`, etc.) | **LOCKED CONTROL (P18) — DO NOT TOUCH** |
| *(Canvas primitives)* | `FLOWER_PLANTER_BED` | `InteriorWorldRenderer.drawCustomProp34` | procedural | — | — | — | N/A | **7** (`sanctuary_flower_planter_0..6`) | 0 | **Untouched** (low curb vector prop, no door obstruction) |

## 3. Complete Sanctuary Runtime Coordinates (Vegetation Obstacles)

### 3.1 `SAKURA_BLOSSOM_TREE` (18 obstacles, `blocksMovement = true`, `occludesUpperCanopy = true`, `radius = 22f`, `footprintHalfW = 22f`, `footprintDepthNorth = 16f`)

| Obstacle ID | World `(x, y)` | Scale | `id.hashCode()` | Parity (`& 1`) | Resolved Asset | District |
|---|---|---|---|---|---|---|
| `sanctuary_sakura_0` | `(1435, 490)` | `0.92` | `196185747` | `1` (odd) | `canopy_b.png` (`96×114`) | North tunnel west flank |
| `sanctuary_sakura_1` | `(1690, 495)` | `0.98` | `196185748` | `0` (even) | `canopy_a.png` (`114×132`) | North tunnel east / Tower Gate west |
| `sanctuary_sakura_2` | `(2245, 520)` | `1.04` | `196185749` | `1` (odd) | `canopy_b.png` (`96×114`) | Tower Gate east flank |
| `sanctuary_sakura_3` | `(2385, 605)` | `0.92` | `196185750` | `0` (even) | `canopy_a.png` (`114×132`) | NE Sakura lawn |
| `sanctuary_sakura_4` | `(2520, 725)` | `0.98` | `196185751` | `1` (odd) | `canopy_b.png` (`96×114`) | NE Sakura lawn |
| `sanctuary_sakura_5` | `(2645, 890)` | `1.04` | `196185752` | `0` (even) | `canopy_a.png` (`114×132`) | East plaza border |
| `sanctuary_sakura_6` | `(2765, 1095)` | `0.92` | `196185753` | `1` (odd) | `canopy_b.png` (`96×114`) | East plaza border |
| `sanctuary_sakura_7` | `(2775, 1355)` | `0.98` | `196185754` | `0` (even) | `canopy_a.png` (`114×132`) | East plaza border |
| `sanctuary_sakura_8` | `(2715, 1590)` | `1.04` | `196185755` | `1` (odd) | `canopy_b.png` (`96×114`) | SE lawn north of Summoning Hall |
| `sanctuary_sakura_9` | `(415, 1485)` | `0.92` | `196185756` | `0` (even) | `canopy_a.png` (`114×132`) | West of SW Training Arena |
| `sanctuary_sakura_10` | `(535, 1475)` | `0.98` | `1786790940` | `0` (even) | `canopy_a.png` (`114×132`) | NW rim of SW Training Arena |
| `sanctuary_sakura_11` | `(410, 1655)` | `1.04` | `1786790941` | `1` (odd) | `canopy_b.png` (`96×114`) | West of SW Training Arena |
| `sanctuary_sakura_12` | `(980, 2245)` | `0.92` | `1786790942` | `0` (even) | `canopy_a.png` (`114×132`) | South of SW Training Arena |
| `sanctuary_sakura_13` | `(1115, 2250)` | `0.98` | `1786790943` | `1` (odd) | `canopy_b.png` (`96×114`) | South of SW Training Arena |
| `sanctuary_sakura_14` | `(1715, 1845)` | `1.04` | `1786790944` | `0` (even) | `canopy_a.png` (`114×132`) | Central-South Sakura lawn (unobstructed) |
| `sanctuary_sakura_15` | `(1865, 2025)` | `0.92` | `1786790945` | `1` (odd) | `canopy_b.png` (`96×114`) | Central-South Sakura lawn (unobstructed) |
| `sanctuary_sakura_16` | `(2010, 2240)` | `0.98` | `1786790946` | `0` (even) | `canopy_a.png` (`114×132`) | SE Main Gate flank |
| `sanctuary_sakura_17` | `(2135, 2248)` | `1.04` | `1786790947` | `1` (odd) | `canopy_b.png` (`96×114`) | SE Main Gate flank |

### 3.2 `GARDEN_HEDGE_BUSH` (5 obstacles, `blocksMovement = true`, `occludesUpperCanopy = false`, `radius = 15f`, `scale = 0.90f`)

- `sanctuary_hedge_bush_0` @ `(1100, 565)` — North residential lawn between Armory `(995, 655)` and Synthesis Lab `(1270, 555)`.
- `sanctuary_hedge_bush_1` @ `(965, 1055)` — Residential hedge courtyard SW.
- `sanctuary_hedge_bush_2` @ `(1065, 1095)` — Residential hedge courtyard center (south of Restaurant `(1105, 925)`, well clear of doorway trigger `(1103, 925)`).
- `sanctuary_hedge_bush_3` @ `(1215, 1045)` — Residential hedge courtyard NE.
- `sanctuary_hedge_bush_4` @ `(2355, 735)` — NE lawn south of `sanctuary_sakura_3` `(2385, 605)`.

### 3.3 `CYPRESS_CONIFER_TREE` (3 obstacles — P20 Protected Control)

- `sanctuary_cypress_0` @ `(1060, 485)`, `scale = 0.94f`
- `sanctuary_cypress_1` @ `(1155, 470)`, `scale = 0.94f`
- `sanctuary_cypress_2` @ `(1485, 655)`, `scale = 0.94f`

## 4. Measured Pre-P22 Visual Defects

1. **`canopy_a.png` & `canopy_b.png` (Sanctuary Sakura A/B):**
   - Generated by `tools/generate_sanctuary_art.py` `tree(w, h, kind=0/1)` — painted green (`#1e3534..#8f9c70`) and slate-cyan (`#20363e..#95a299`) instead of muted dusk-rose Sakura blossoms, so Sanctuary completely lacked its authored cherry-blossom identity!
   - Both sprites share the **exact same rescaled Y-forked trunk polygon** and left-foot moss polygon, making A and B look like a rescaled recolour rather than two distinct trees.
   - Both sprites touch `y = 0` (`top = 0`), clipping their crowns flat against the top canvas border.
2. **`GARDEN_HEDGE_BUSH` (`SanctuaryVegetation.shrub()`):**
   - Three flat dark-green octagons with horizontal 4–5 px barcode stripes (`#647657` / `#3d5543`), zero 3D top/front plane structure, and identical to `FOREST_CANOPY_BUSH`. Visually degrades the P21 Restaurant/Quarters courtyard at `z = 0.88`.
3. **`pine.png` & `twisted.png` (Tower Floor 1 & 2 runtime, absent from Sanctuary):**
   - Both reuse the exact same cloned deciduous Y-forked oak trunk from `generate_sanctuary_art.py`! `pine.png` stacks 5 round blobs atop a forked oak trunk and clips at `y = 0`; `twisted.png` places 5 uniform grey-purple blobs atop the same straight trunk with zero kinked/windswept limb architecture.

## 5. Known Unrelated Defects (Documented, Out of P22 Scope)

- **P20 Armory door offset:** painted door on `premium_slice/armory.png` maps east/north of `SanctuaryHouseEntrances` trigger `(993, 655)` — owned by Architecture/Gameplay pass, untouched here.
- **Training Ground obstacle:** `PropKind.FACILITY_TRAINING_GROUND` has no `WorldObstacle` in `buildSanctuaryMap()` — untouched here.
