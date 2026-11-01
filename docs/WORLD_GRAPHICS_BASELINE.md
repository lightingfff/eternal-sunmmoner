# WORLD GRAPHICS BASELINE — Eternal Summoner Sanctuary (LOCKED)

> **Status:** LOCKED CANON — Do not change without explicit approval.  
> Source of truth: `WorldMapModels.kt` (`ObliqueCamera`, `SanctuaryIslandGeometry`, `CollisionPhysics`), `TowerFloorCatalog.kt` (`SANCTUARY_ZONES`, `buildSanctuaryMap`, `buildSanctuaryPaths`), `SanctuaryLobbyEngine.kt` (nav graph 20 nodes), `PixelStructureAtlas.kt` / `PixelSanctuaryLandmarks.kt`.

---

## 1. World Space & Island Geometry (MUST PRESERVE)

### 1.1 Global Extents
| Constant | Value | Notes |
|---|---|---|
| `SANCTUARY_WIDTH` | **3200f** | Total world width — Unity/Compose world units |
| `SANCTUARY_HEIGHT` | **2760f** | Total world height |
| `SANCTUARY_WALL_MARGIN` | **82f** | Interior-biome walls + Sanctuary perimeter cliff line |
| `SANCTUARY_VIEW_W` | 3036f (`WIDTH - margin*2`) | Usable floor bounds clamped by camera |
| `SANCTUARY_VIEW_H` | 2596f | Usable floor bounds |
| Coordinate origin | top-left (0,0) | **Y+ = South** (south is larger Y, forehead north) |
| Clamp padding | **30f** | `SanctuaryIslandGeometry.clampInside(x,y)` |

### 1.2 8-Vertex Octagonal Island Polygon
Defined in `SanctuaryIslandGeometry.POLYGON` (worldXY pairs, clockwise from NW):
```
(780, 140) → (2450, 140) → (2920, 560) → (3050, 1760) → (2860, 2560) → (1220, 2560) → (280, 1920) → (280, 520)
```
- **Ray-casting `isInside(x,y)`** must stay as point-in-polygon; all gameplay, camera clamp, and decoration placement must respect it.
- `clampInside` clamps with 30f inset — camera `computeLobbyCam` uses it + additional 180f/200f margin against `wallMargin`.
- Plaza center lies strictly inside polygon; all zone centers are inside (verified).
- **Preservation rule:** Never move polygon vertices beyond ±4px without re-validating path intersections and zone containment. Never delete inset clamp.

### 1.3 Plaza
- Center: **`SANCTUARY_CENTER_X = 1940f, SANCTUARY_PLAZA_Y = 1240f`** (sometimes referenced as (1940, 1240)).
- Visual radius: **555f** (world units) for floor medallion; logical `SANCTUARY_ZONES[TOWER_GATE].interactRadius` overlaps but does not replace geometry.
- Outer medallion tile: `PixelTileAtlas.astrolabePlazaMedallion` scaled 1.95×, centered at plaza world pos via `sx/sy`.

---

## 2. Camera & Projection (MUST PRESERVE)

### 2.1 Oblique 3/4 Camera (~43° elevation, dimetric 2:1 foreshortening)
```kotlin
object ObliqueCamera {
  const val OBLIQUE_Y_SCALE = 0.72f          // 1 world Y → 0.72 screen Y
  const val GROUND_ELLIPSE_RATIO = 0.66f     // foreshortened circle → ellipse ry = rx * 0.66
  fun worldToScreenX(wx, camX, zoom, halfW) = (wx - camX) * zoom + halfW
  fun worldToScreenY(wy, camY, zoom, halfH, z=0f) = (wy - camY) * zoom * OBLIQUE_Y_SCALE + halfH - z*zoom
  fun screenToWorldX(sx, ...) = (sx - halfW)/zoom + camX
  fun screenToWorldY(sy, ...) = (sy - halfH + z*zoom)/zoom/OBLIQUE_Y_SCALE + camY
  fun clampInside(x, y, wallMargin, w, h)
}
```
- **Zoom in Sanctuary:**
  - Exterior: `[0.36, 1.48]`, default `SANCTUARY_DEFAULT_ZOOM = 0.62f`
  - Interior: fixed `0.92f` (exterior saved/restored on enter/exit)
- **Camera clamp** (exterior): `x ∈ [wallMargin+180, width-wallMargin-200]`, `y ∈ [wallMargin+180, height-wallMargin-200]` after `clampInside`.
- `screenToWorldY` inverts `OBLIQUE_Y_SCALE`; any new height (`elevationZ` / `z`) MUST pass `z` param to `worldToScreenY` consistently (groundOffset vs sprite foot).
- **Preservation rule:** `OBLIQUE_Y_SCALE = 0.72 ±0.02`, `GROUND_ELLIPSE_RATIO = 0.66 ±0.02` — changing them retiles every ground texture and breaks path width. Any new depth/height system chains through these helpers, never bypasses them.

### 2.2 Screen ↔ World Consistency Requirement
- All interaction (tap, drag, facility prompt, shadow ground projection) must `screenToWorld` via `ObliqueCamera`, never ad-hoc `*(1/ zoom)` math.
- Ground-contact shadows are foreshortened with `GROUND_ELLIPSE_RATIO`; character shadows keep `0.66` unless light direction stretches length.

---

## 3. Zone Graph (7 SANCTUARY_ZONES — LOCKED)

```kotlin
SANCTUARY_ZONES = mapOf(
  TOWER_GATE         → (1960,  635) r185 color 0xFF00E5FF  // North gate, portal
  SYNTHESIS_LAB      → (1270,  585) r150 0xFFFFD54F
  ARMORY             → ( 995,  685) r150 0xFFF4511E
  HERO_QUARTERS      → ( 825,  820) r155 0xFF66BB6A  // enterable
  RESTAURANT         → (1105,  955) r155 0xFFFFA726  // enterable
  SUMMONING_HALL     → (2380, 1930) r210 0xFF5C6BC0
  TRAINING_GROUND    → ( 890, 1790) r280 0xFF8D6E63  // enterable, largest
)
+ 3 special anchor points (not in map but used for interaction):
  HERO_REGISTRY_X 785, HERO_REGISTRY_Y 1185 → maps to SUMMONING_HALL (tap proxy)
  FLOOR_VISTA_X 1560, FLOOR_VISTA_Y 425 → maps to TOWER_GATE
  Plaza center (1940,1240) — no zone, floor FX only
```
- Radii are **interaction** radii (floor ring + `selectFacilityAt` / `nearbyFacility`), not collision.
- Tap tolerance `*1.15`, proximity `*1.45` (plus 185f fallback for registry/vista) — tuned for zoom range.
- Color ARGB drives floor ring tint.

**Preservation:** Never move zone centers >12px or shrink radii >10% — doing so desyncs obstacle footprints (building center is zone center − ≈30px north) and breaks saved `nearbyFacility` muscle memory.

---

## 4. Obstacles & Rendering Catalog (≈78 exterior obstacles — DO NOT REMOVE)

### 4.1 Structure Source & Anchor Convention
- All world obstacles defined in `TowerFloorCatalog.buildSanctuaryMap()` `exteriorObstacles` list.
- Each entry: `WorldObstacle(id, x, y, radius, blocksMovement, blocksVision, tags, propKind, isAnimated, sortYBias, footprintHalfW, footprintDepthNorth, scale, occludesUpperCanopy, elevationZ)`.
- **`x,y` = ground anchor** at **south edge** of sprite foot (where `drawCrispSprite(cx, footY = cy+6*s, ...)` bottom aligns). **Not** sprite center.
- `footprintHalfW` / `footprintDepthNorth` define rectangular collision south-aligned (see §4.3).
- `sortY = y + sortYBias` drives Y-sorted draw; `elevationZ` lifts sprite via `worldToScreenY(..., z=elevationZ)` (currently 0 for most exterior, 0 for trees/rocks).
- Scale: `0.8f` for Sanctuary buildings (200×168 native → ~160×135 world), `1.0f` decorative.

### 4.2 Full Exterior Obstacle Inventory (78 entries — summary by band)
| Band (Y) | Count | Representative IDs / PropKind |
|---|---|---|
| North wall & terrace (`y 140–400`) | 8 | north_cliff_left/right, terrace_parapet_w/e, grand stairs, balustrade w/e, vista_arch |
| North facilities (`y 555–685`) | 10 | building_synthesis_lab, building_armory, lantern×3, lantern×2, crates/barrels |
| Northwest housing (`y 790–930`) | 12 | building_hero_quarters, nw_outer_slate_house, nw_outer_brown_cottage, building_restaurant, oaks×3, sakura×2, hedge, lantern |
| Central spine (`y 1000–1285`) | 16 | hero_registry, cypress×2, rock clusters×6, lantern×2, sakura×3, bench, monument_tower_gate |
| Southern parks & summoning (`y 1335–2005`) | 20 | s_lawn cypress×2/bush/bench/lantern, oaks×2/sakura/hedge/w_bench/n_gate, s_garden oaks/lantern×2, s_grove 3 oaks + lantern + bench, building_summon_hall + monolith |
| Training grounds (`y 1770–1870`) | 12 | sand_training north/central/south patches, weapon_rack×2, striking_post, oaks×2, sakura, cypress, lantern, hedge |
| **Total** | **78** | + 4 invisible anchor obstacles (registry/vista) = 82 logical entries per prior OVERHAUL_PIPELINE §9.4 |

Interior layouts: `82f wallMargin`, each with own `exteriorObstacles` for furniture (restaurant 14, dormitory ~12, training hall ~10, armory ~8).

Full list with exact floats is in `TowerFloorCatalog.kt:120..280` — treat as source of truth; this doc summaries counts to avoid drift (verify via `grep "id = " TowerFloorCatalog.kt | wc -l`).

### 4.3 Collision Footprint Convention
```
Rect(
  left   = x - footprintHalfW,
  right  = x + footprintHalfW,
  top    = y - footprintDepthNorth,
  bottom = y
)
```
- **Width = footprintHalfW×2**, **depth = footprintDepthNorth** (northward only; south face is at y).
- Typical sanctuary building: `footprintHalfW 72–78f, footprintDepthNorth 52–56f`.
- `radius` kept for legacy circle broad-phase; engine uses rect `resolvePosition` (axis-separate, no sliding).
- `sortYBias` e.g., `16f` for tall corner towers (raise sort to avoid Y-fighting), `-120f` for exit mats (render behind).
- `occludesUpperCanopy` true for OAK/MYSTIC/PINE/SAKURA — enables second-pass canopy occlusion over heroes.

**Preservation:** Never switch to pure circles, never drop `footprintHalfW/DepthNorth` — rect is needed for terrace wall corridors (long thin walls W170 H36). Any doorway cutout is a **subtraction** from this rect (see House Audit).

---

## 5. Paths & Terrain (LOCKED)

### 5.1 8 Sanctuary Paths (PathSegment)
| ID | start | end | width | Color |
|---|---|---|---|---|
| sanctuary_nw_road_main | (825,820) | (1105,955) | 34f | 0xFF9F937A |
| sanctuary_armory_to_synthesis | (995,685) | (1270,555) | 26f | 0xFF9F937A |
| sanctuary_restaurant_to_center | (1105,955) | (1940,1240) | 40f | 0xFF9F937A |
| sanctuary_central_to_vista | (1940,1240) | (1560,425) | 48f | 0xFFD8D0B8 |
| sanctuary_west_arm_to_gate | (825,820) | (1960,635) | 36f | 0xFF9F937A |
| sanctuary_plaza_to_summon | (1940,1240) | (2430,2005) | 42f | 0xFF9F937A |
| sanctuary_hero_registry_link | (785,1185) | (1105,955) | 22f | 0xFF9F937A |
| sanctuary_training_connector | (890,1790) | (1105,955) | 36f | 0xFF9F937A |

- Rendered with road core `width*0.50` and curb `width*0.76` in `drawWorldBackground`.
- Road tiles: `sanctuaryCausewayTiles` (or SanctuaryAssetManager overrides).
- **Preservation:** Widths define sidewalk clearances; changing them pinches hero wandering corridors.

### 5.2 Terrain Patches & Expansion Milestones
- 4 base plus 4 unlockable `TerrainPatch` (medallion glow) at zone centers with `radiusX`/`colorArgb`.
- `SanctuaryAssetManager` overrides tileAssets/roadAssets when unlocked expansions present.

---

## 6. Navigation Graph (20 nodes — LOCKED TOPOLOGY)

```
tower_gate (1960,560) ←→ n_central_plateau  ←→ n_terrace_stairs_w/e
n_central_plateau ←→ w_armory_road ←→ synthesis_yard ←→ armory_yard ←→ armory_to_synthesis
armory_to_synthesis ←→ housing_lane ←→ hero_quarters_yard ←→ restaurant_yard ←→ restaurant_to_plaza
restaurant_to_plaza ←→ central_plaza (1940,1200) ←→ plaza_to_summon ←→ hero_registry_plaza
central_plaza ←→ training_lane ←→ sand_training_north/south (890,1800+)
central_plaza ←→ south_grove / s_garden_lawn / summoning_forecourt
```
- Edge connections (undirected) are in `SanctuaryLobbyEngine.resolveNavPositions()` — **do not remove edges**; doing so strands heroes behind walls.
- Entrance nodes: `hero_quarters_yard (880,840)`, `restaurant_yard (1060,980)`, `armory_yard (980,720)`, `sand_training_*` — these are **outside** building footprints (verified: yard Y is 20–35px south of building foot Y, clear of collision).

**Preservation:** Keep 20 nodes; any doorway fix adds a **new doorway node** connected to its yard node, not replacing existing edges.

---

## 7. Interaction, Sorting & Performance Baseline

### 7.1 Tap → Proximity Model (current — to be repaired in House Audit)
- `selectFacilityAt(wx,wy)`: min-by-distance `radius*1.15` (plus registry/vista 130 proxy)
- `nearbyFacility` (every 350ms): min-by-distance `radius*1.45` (plus 185 fallback) from `camX/camY` — **camera-based, not hero**
- Prompt card → `Enter Interior` if `ENTERABLE_FACILITIES` contains facility.
- **Enterable:** `HERO_QUARTERS, RESTAURANT, TRAINING_GROUND, ARMORY` (4 only; `SYNTHESIS_LAB` / `SUMMONING_HALL` / `TOWER_GATE` are management-only).

### 7.2 Draw Order & Sorting
- `drawWorldBackground` (tiling + paths + plaza medallion)
- Floor rings → `obstacles.sortedBy { it.sortY }` (Y-sort; `sortYBias` included)
- `drawUpperCanopyOcclusionPass` (second canopy layer over heroes)
- Heroes → `drawCharacterSprite` (elliptical shadow, aura ring)
- Foreground walls / atmosphere particles

### 7.3 Performance Budget (Android 60 FPS target)
- Canvas `drawImage` per tile (capped by view frustum via `startTx/endTx`) — measured ~14–22 ms frame on mid-tier at 0.62 zoom.
- One-pass `drawDirectionalShadow34` per obstacle (ellipse, no blur) — cheap; future silhouette must stay ≤2 oval draws per caster.
- Nav graph BFS per hero ≤20 nodes, throttled.

---

## 8. Preservation Checklist (MUST PASS BEFORE MERGE)

- [ ] `SanctuaryIslandGeometry` 8 vertices unchanged (Δ ≤4px) and `clampInside(30f)` intact.
- [ ] `SANCTUARY_CENTER_X/Y`, plaza radius 555, `SANCTUARY_WALL_MARGIN 82f`, `isInteriorBiome` set intact.
- [ ] 7 `SANCTUARY_ZONES` centers/radii unchanged (moved only if House Audit doorway realignment specifies, ≤12px).
- [ ] All 8 PathSegments present with original widths (±2f).
- [ ] All ~78 exterior obstacles present; `footprintHalfW/DepthNorth`, `sortYBias`, `occludesUpperCanopy` unchanged unless shadow/depth design explicitly permits.
- [ ] 20 nav nodes + edges connectivity preserved; doorway nodes added as leaves, not replacements.
- [ ] `ObliqueCamera.OBLIQUE_Y_SCALE 0.72`, `GROUND_ELLIPSE_RATIO 0.66`, `worldToScreen*` math untouched.

Any violation must be listed in PR description with visual before/after evidence.
