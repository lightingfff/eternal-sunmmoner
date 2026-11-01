# HOUSE INTERACTION AUDIT — Sanctuary Enterable Buildings Repair

> Scope: `TowerFloorCatalog.buildSanctuaryMap()` exterior + `SanctuaryLobbyEngine` + `SanctuaryLobbyScreen` + `WorldMapModels.CollisionPhysics` + `PixelStructureAtlas` half-timber rendering.  
> Goal: Every door the art shows as enterable must be enterable (walk to it, see prompt, enter, return) — no invisible walls, no dead taps.

---

## 1. House Inventory — All Sanctuary Buildings

### 1.1 Facility Buildings (have SANCTUARY_ZONE center — interaction candidate)
| # | FacilityType | Zone center (x,y) r | ObstacleId center (x,y) footprint | PropKind / Visual | Enterable today | Interior map |
|---|---|---|---|---|---|---|
| 1 | **HERO_QUARTERS** | (825,820) r155 | `building_hero_quarters` (825,790) W148 D54 | FACILITY_HERO_QUARTERS · 200×168 half-timber A | **YES** | Dormitory (820×640, wall 82) |
| 2 | **RESTAURANT** | (1105,955) r155 | `building_restaurant` (1105,925) W148 D54 | FACILITY_RESTAURANT · 206×172 half-timber B + awning | **YES** | Restaurant (820×640) |
| 3 | **ARMORY** | (995,685) r150 | `building_armory` (995,655) W144 D54 | FACILITY_ARMORY · 190×166 half-timber C | **YES** | Armory (820×640) |
| 4 | **TRAINING_GROUND** | (890,1790) r280 | *(no building obstacle)* — sand patches at (890,1785) R52 + (890,1820) R38 / weapon racks | FACILITY_TRAINING_GROUND · 192×160 open arena | **YES** | Training Hall (820×640) |
| 5 | SYNTHESIS_LAB | (1270,585) r150 | `building_synthesis_lab` (1270,555) W140 D52 | FACILITY_SYNTHESIS_LAB · 188×164 apothecary | NO (management only) | — none |
| 6 | SUMMONING_HALL | (2380,1930) r210 | `building_summon_hall` (2430,2005) W195 D135 | FACILITY_SUMMONING_HALL · 320×210 classical | NO | — none |
| 7 | TOWER_GATE | (1960,635) r185 | `monument_tower_gate` (1960,585) W155 D110 | FACILITY_TOWER_GATE · split-monolith portal | NO | — none (tower floors) |
| S1 | *HERO_REGISTRY proxy* | — registry at (785,1185) R26 | `hero_registry` (785,1185) R26 | FACILITY_HERO_REGISTRY · ledger obelisk | NO (→ SummoningHall prompt) | — |
| S2 | *FLOOR_VISTA proxy* | — vista at (1560,425) R32 | `floor_vista_arch` (1560,425) R32 | FLOOR_VISTA_ARCH · ruined arch | NO (→ TowerGate prompt) | — |

**Enterable = 4** (`ENTERABLE_FACILITIES` in lobby engine) — these are the repair priority. Synthesis / Summoning / Tower are intentionally management-only (no interior map built).

### 1.2 Decorative Houses (visible but NOT enterable — no zone, no interior)
| ID | Position (x,y) radius | PropKind | Visual note |
|---|---|---|---|
| `nw_outer_slate_house` | (655,930) 30 | MEDIEVAL_TOWNHOUSE_B · slate roof B | NW outer lane, no zone |
| `nw_outer_brown_cottage` | (495,1075) 26 | SANCTUARY_COTTAGE_BROWN · brown cottage | NW edge |
| `inner_slate_wing_cottage` | (1395,675) 27 | SANCTUARY_COTTAGE_SLATE · slate wing | NE lane |
| `south_grove_oaks` etc | — | Tree/bush kits | Not houses |

These **must stay non-enterable**; audit records them to prevent accidental “tap-to-enter” regression when doorway detection is widened.

### 1.3 Door Art Ground Truth (from PixelSanctuaryLandmarks — paintAnimeHalfTimberHouse)
All 3 half-timber enterable buildings share `paintAnimeHalfTimberHouse(leftX=18, bodyW*, bodyH92, roofH38–40)`:
- Front wall `x=leftX … leftX+bodyW`, `y=topY+roofH`, h=92.
- **Arch door centered at `gableCenter-8`** (gableCenter = leftX+bodyW/2), width 22, height 30, `bottomY = topY+roofH+92+?` essentially **ground line = sprite bottom -2px**, threshold step projects 4px outward south.
- Shutters/windows symmetric; no second door.
- Training ground is *open arena* — entry is through a symbolic south gate between weapon racks, not an arch.

Measured **door local offset from building anchor** (pixel canvas → world units at scale 0.8):
- Door world X ≈ `obstacle.x - 3.5f * zoom` (≈ 2–4px north-west bias due to `gableCenter-8`); for 0.8 scale treat as **`obstacle.x - 2f`**.
- Door world Y = `obstacle.y` (foot baseline) — threshold spans `[y-2, y+2]` south protrusion.
- Training ground gate: between racks at (880, 1910?) approx — see §3 trigger table.

---

## 2. Diagnostic — Why Players Cannot Enter (5 Root Causes)

### Cause A — RECTANGULAR FOOTPRINT SEALS THE DOORWAY (P1 Blocker)
`WorldObstacle.footprintHalfW / footprintDepthNorth` creates solid rect `Rect(x-W, x+W, y-D, y)` with **no cutout**. `CollisionPhysics.resolvePosition` axis-separates on X then Y, so the south face at `y` is an invisible wall. A hero (or camera avatar, if future walk-to-door) approaching from the south collides exactly at the threshold and can never overlap the door arch visual (which sits at `y`).  
Impact: Players “slide” along the wall; door looks reachable but physics says blocked. Even though current interaction is camera-tap (not avatar walk), the blocked tile discourages players from positioning camera at door eye-level and creates a visual lie (doorway looks open, collision says closed).

### Cause B — TRIGGER CENTERED 30px NORTH OF DOOR VISUAL (P1 Blocker)
`SANCTUARY_ZONES` centers are **30px north** of the `WorldObstacle` anchors (hero_quarters 820 vs 790, etc. — inherited from plaza layout). Both `selectFacilityAt` (`r*1.15`) and `nearbyFacility` (`r*1.45`) test **circle centered at zone north bias**, not at the door arch.  
For a player framing the doorway, the tap target is *above* the door on the roof; tapping the visible arch/steps is near the circle edge, requiring pixel-perfect aim at max zoom. At min zoom 0.36 the 1.15 tolerance saves it, but at default 0.62 the miss rate is high. Training ground has no building obstacle — zone center (890,1790) is in empty sand with no visual proxy, so players hunt an invisible circle.

### Cause C — NAV GRAPH STOPS SHORT OF DOOR (P2)
Nav nodes `hero_quarters_yard (880,840)`, `restaurant_yard (1060,980)`, `armory_yard (980,720)` are **clear-approach yards** south-west of buildings, **not** doorway nodes. Edges stop at yards; no `doorway` node exists. Heroes wandering via `resolveNavPositions()` never path *to* the door, breaking the affordance “follow NPC to door = door is enterable”.

### Cause D — SCENE TRANSITION HAS NO DEBOUNCE / SINGLE-TRACK SAVE (P2)
`enterFacilityInterior(facility)` saves `savedExteriorX/Y/Zoom = camX/camY/zoom` only if `!isInsideInterior`; rapid double-tap (or tap+keyboard E) within one `syncHeroes` frame can overwrite saved exterior or toggle `activeInterior` without anim, causing camera snap-back to wrong pos on exit. No `isTransitioning` lock, no `lastTransitionMs` guard.

### Cause E — INTERIOR EXIT MISALIGNED WITH EXTERIOR DOOR (P3 polish)
Interior exit mat at `DORMITORY_EXIT_X 610` is a floor decal with `sortYBias -120`; exterior outside-spawn is **not defined** — `exitFacilityInterior()` blindly restores `savedExteriorX/Y` (wherever camera was before enter, possibly far from door). Player exits far from the door they entered, breaking spatial continuity.

---

## 3. Required Fix — Entrance Model (per-house authoring)

### 3.1 New Canonical Field (non-breaking)
Add registry (not overwriting obstacle rect) — `SanctuaryHouseEntrance` data class in `WorldMapModels.kt`:

```kotlin
data class SanctuaryHouseEntrance(
  val facility: SanctuaryFacilityType,
  val obstacleId: String,            // links to WorldObstacle.id ("" for training ground arena)
  val doorWorldX: Float,             // south-face arch center
  val doorWorldY: Float,             // foot baseline (≈ obstacle.y)
  val facingDeg: Float = 180f,       // door outward south = 180deg (Y+)
  val outsideSpawnX: Float,          // where avatar/camera appears after exit (8–18px south of door)
  val outsideSpawnY: Float,
  val triggerRadius: Float = 44f,   // circular doorway trigger (tuned for 0.62 zoom)
  val doorWidth: Float = 36f,        // collision cutout width on south edge
  val interiorId: String,            // maps to GeneratedMapLayout interior biome
)

val SANCTUARY_HOUSE_ENTRANCES = mapOf(
  //  doorWorld = obstacle.x + dx,  doorWorldY = obstacle.y
  HERO_QUARTERS    to SanctuaryHouseEntrance(HERO_QUARTERS, "building_hero_quarters", 823f, 790f, 180f, 823f, 808f, 44f, 36f, "dormitory"),
  RESTAURANT       to SanctuaryHouseEntrance(RESTAURANT,    "building_restaurant",    1103f, 925f, 180f,1103f, 943f, 44f, 36f, "restaurant"),
  ARMORY           to SanctuaryHouseEntrance(ARMORY,        "building_armory",         993f, 655f, 180f, 993f, 673f, 44f, 36f, "armory"),
  TRAINING_GROUND  to SanctuaryHouseEntrance(TRAINING_GROUND,"sand_training_south_patch", 890f, 1942f, 180f, 890f, 1960f, 52f, 48f, "training_hall"),
)
```
- `doorWorldX/Y` computed as `obstacle.x - 2` (painting offset) for half-timber; training ground uses south arena gate (south edge of central sand patch `890,1820 R38 → y=1854`). Chosen `890,1942` is midpoint between training racks `weapon_rack_north 930,1870` and `weapon_rack_south 845,1895`, clearly visible as gate.
- `outsideSpawn` = `doorWorld + (0, 16..18)` south — guaranteed outside collision after cutout.
- `triggerRadius 44` at zoom 0.62 ≈ 27px screen — comfortable thumb target without overlapping neighboring zones (closest zone separation: hero_quarters 825,820 vs restaurant 1105,955 = ~310 world → far).

### 3.2 Collision Doorway Cutout
Amend `WorldObstacle.rect()` semantics: if `id` matches `SANCTUARY_HOUSE_ENTRANCES[].obstacleId`, subtract doorway slit from south edge:
```
if (x in [doorX - doorWidth/2, doorX + doorWidth/2] && y in [doorY - 2, doorY + 2])
  → do NOT collide on south 3px strip (gap height 6 world units)
```
Implemented in `CollisionPhysics.resolvePosition` as **exception interval** before axis test, not by shrinking global rect (so wall corners remain solid).

### 3.3 Interaction Resolution (priority order)
1. **Doorway proximity** — if `dist(cam, doorWorld) ≤ triggerRadius*zoomClamped` (zoomClamped ∈ [0.9,1.2] to normalize thumb) → `nearbyFacility = facility` and `selectFacilityAt` returns that facility even if zone center is farther.
2. Fallback to legacy zone center `r*1.15 / r*1.45` for synthesis/summoning/tower (which have no doorway).
3. Decorative houses (medieval townhouses) never match step 1 or 2 (not in map), so stay inert.

### 3.4 Navigation Doorway Node
Extend nav graph per enterable:
```
hero_quarters_door (823, 808) — leaf from hero_quarters_yard (880,840)
restaurant_door    (1103,943) — leaf from restaurant_yard   (1060,980)
armory_door        (993, 673) — leaf from armory_yard       (980,720)
training_gate      (890, 1960) — leaf from sand_training_south
```
Heroes with `task target = yard` can one-hop to door to demonstrate enterability.

### 3.5 Scene Transition Hardening
- `isTransitioning:Boolean` + `lastTransitionMs:Long` with `TRANSITION_DEBOUNCE_MS = 600L`.
- `enterFacilityInterior` returns early if `now - lastTransitionMs < 600`.
- `savedExterior` is `ExteriorSnapshot(x,y,zoom,doorSpawn)` — capture doorSpawn for exit fallback if saved pos is corrupted.
- `exitFacilityInterior` lerp camera to `outsideSpawn` (not blind restore) if saved pos was inside collision (sanity check via `CollisionPhysics.wouldCollide`).

---

## 4. Verification Matrix

| Enterable | Door tap (default zoom) | Door tap (min zoom 0.36) | walk-to-door collision gap | Nav hero reaches door | Enter → Exit round-trip restores outside door |
|---|---|---|---|---|---|
| HERO_QUARTERS | Tap arch → prompt + auto-nearby badge | Pass (fallback zone) | 36px gap, south approach unblocked | Yard → door leaf visited | Spawn 823,808 (south of door) |
| RESTAURANT | Same | Same | 36px gap | Yard → door | 1103,943 |
| ARMORY | Same | Same | 36px gap | Yard → door | 993,673 |
| TRAINING_GROUND | Tap south gate gap (central sand) | Tap large R52 patch | 48px gate | sand_south → gate | 890,1960 |

**Automated checks to add:**
- `isDoorwayCutoutOpen(id)` unit test: for each enterable, `resolvePosition(doorX, doorY+6, 12f, obstacles)` must not push north.
- `selectFacilityAt(doorX, doorY) == facility` at zoom 0.62 and 0.36.
- `isInsideInterior` debounce: double `enterFacilityInterior` within 100ms only fires once.

---

## 5. Rollout Order (per Part 14)
1. Author `SANCTUARY_HOUSE_ENTRANCES` + doorway cutout in `WorldMapModels.kt` (no UI change yet).
2. Wire `SanctuaryLobbyEngine.nearbyFacility` + debounce + outsideSpawn.
3. Update `SanctuaryLobbyScreen.selectFacilityAt` to prioritize door.
4. Add nav leaf nodes + verify hero reachability.
5. QA exit round-trip + Android thumb smoke test (see ANDROID_STABILITY_REPORT.md perf guard).

Preservation impact: **None** — island polygon, zone radii, path widths, obstacle `footprintHalfW/DepthNorth` untouched; only adds cutout predicate and new map constant.
