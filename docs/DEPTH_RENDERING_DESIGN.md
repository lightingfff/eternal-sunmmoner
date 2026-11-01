# DEPTH RENDERING DESIGN — True 2.5D World Space, Sorting, Fog & DOF

> Builds on `WORLD_GRAPHICS_BASELINE.md` ground truth. Defines the fore/mid/back world model, `groundAnchor`/`elevationZ` system, Y-sort depth, occlusion, and 4-band atmospheric perspective that replace the prior flat 2.5D.

---

## 1. Goals & Non-Goals

**Goals:**
- Fore/mid/back separation visible on a still screenshot (no animation required to read depth).
- Correct ground contact: every foot, shadow, and threshold sits on the same foreshortened ground plane (`OBLIQUE_Y_SCALE 0.72`).
- Occlusion sells height: canopies occlude heroes near trunks; walls occlude paths beyond; roofs occlude interiors.
- 60fps on Android mid-tier; no custom shaders that break Canvas hardware accel.

**Non-goals (this phase):**
- Normal-mapped dynamic lighting on walls (keep baked AO/gradient).
- Parallax multi-layer scrolling (camera is orthographic oblique, not perspective).
- Screen-space ambient occlusion on tiles (baked per-tile variation suffices).

---

## 2. World Space Model

### 2.1 Three Depth Zones (based on world Y — north→south is back→fore)

| Zone | World Y range (approx) | Content | Camera parallax / Atmosphere | Shadow behavior |
|---|---|---|---|---|
| **BACK** (north vista) | `y < 520` (island top) | Sky, north cliff wall, buttresses, distant tree silhouettes, portal glow | Strongest desaturation + cool shift, faintest shadows | Shadows clipped at wall face; no ground cast beyond `wallMargin+40` |
| **MID** (plaza & housing) | `520 ≤ y < 1760` | Hero quarters / restaurant / armory, monument, parks, paths, most heroes | Base reference — no tint | Full contact+cast shadows |
| **FORE** (south foreground) | `1760 ≤ y ≤ 2560` (island bottom) | Training sands, summoning hall, fortress walls, cliff drop | Slight warm lift + highest contrast, richest shadow density | Cliff-drop fade (see Shadow Design §3.3) |

Zones are **not hard layers** for sorting — tiling is continuous — but they drive: fog tint per `worldY`, particle density, and which foreground walls are drawn **after** actors (south walls occlude actors behind them).

### 2.2 Per-Object Depth Attributes (extension of WorldObstacle)

```kotlin
// Existing (preserved):
val x: Float, y: Float             // ground anchor (south edge foot) — groundAnchor
val footprintHalfW: Float, footprintDepthNorth: Float
val sortYBias: Float
val elevationZ: Float = 0f          // already present — lift above ground plane (raised terrace)
val scale: Float

// New (this design):
val groundAnchor: Offset get() = Offset(x, y) // alias — never move anchor away from foot
val visualHeightPx: Float          // body+roof or canopy height for shadow & DOF blur radius
val depthBand: DepthBand            // derived: BACK/MID/FORE from y (cached at build)
val receivesAtmosphere: Boolean = true
val occludesUpperCanopy: Boolean    // already exists for trees — keep
```

**Invariants (must hold for every prop):**
1. `groundAnchor` lies on ground plane `z=0` — shadow center is `worldToScreenY(y, ..., z=0)`, sprite bottom is `footY = worldToScreenY(y, ..., z=elevationZ) + lift`. Never offset anchor north to “fake” depth.
2. Tall buildings (body 92 + roof 38) have `visualHeightPx ≈ 130*scale`; rocks `≈18*scale`; so `footY - topY ≈ visualHeightPx * zoom`.
3. Moving characters share same model: `worldX, worldY` foot, `elevationZ` 0 normally, plus jump arc adds `z>0` (uses same `worldToScreenY` with z).
4. `sortY = y + sortYBias - elevationZ*0.5f` — elevated south terrace objects (stairs at y=320) sort slightly earlier so plaza heroes passing in front occlude them correctly even though their Y is smaller.

### 2.3 Camera Consistency Guarantee

All projection does **not** branch:
```kotlin
fun screenPos(ob: WorldObstacle) = ObliqueCamera.worldToScreen(ob.x, ob.y, camX, camY, zoom, halfW, halfH, ob.elevationZ)
fun screenPosCharacter(wx, wy, wz=0f) = ObliqueCamera.worldToScreenY(wy, camY, zoom, halfH, wz)
```
New depth/fog/DOF layers call these helpers; no duplicate math exists after this doc.

---

## 3. Sorting & Occlusion Pipeline

### 3.1 Six-Pass Draw Order (Canvas, no Z-buffer)

```
PASS 0  Sky / north wall vista                (behind ground)
PASS 1  Ground tiling + paths + plaza medallion (foreshortened 64×46)
PASS 2  Ground-contact shadows + contact AO   (Shadow Design §3.1) — clipped to ground
PASS 3  Floor rings (zone interaction ellipses) — thin stroke over shadows, under actors
PASS 4  WorldObstacle Y-sorted draw (PASS 3.5 canopy occlusion second half happens inside)
          sortedBy { it.sortY } → drawProp (building body+roof with baked depth)
PASS 5  Actors (heroes, enemies) sortedBy worldY — interleaved with obstacles at same Y
          (hero at Y=850 draws AFTER building at Y=790 if hero.y > building.y, else before)
PASS 5.5 UpperCanopyOcclusionPass (tree crowns over same-Y actors)
PASS 6  Southern foreground walls + cliff face    (strong occluders — draw after actors to pop fore)
PASS 7  Atmosphere (4-band fog + DOF bands + particles)
```

**Why obstacles and actors interleave (not two separate lists):** A hero standing just north of a 54-deep building should be occluded by its north wall but be visible over its south foot. Y-sort on `sortY` vs `worldY` approximates this without per-pixel depth. Terrace walls (depth 36) have `sortYBias +16` to ensure their tall north face occludes plaza actors even though their Y is smaller than plaza.

### 3.2 Occlusion Sources & Handling

| Occluder | Occludes | Technique |
|---|---|---|
| Tree canopy (oak/mystic/pine/sakura) | Hero standing at trunk north | `occludesUpperCanopy` + second-pass `drawCrispUpperCanopyPortion(topFraction 0.54)` after actors |
| Building front wall + pitched roof | Actor north of building (behind) | Y-sort: building `sortY = y` > actor `y` (south is larger Y, so building draws after actor → covers) |
| Building side bevel (sideDepthPx 8–10) | Diagonal neighbor props | Baked into `drawPitchedRoof34` / `drawStoneWall34` side bevel polygon — no runtime pass |
| North terrace / south cliff walls | Everything behind/in front appropriately | PASS 6 draws walls after actors — wall face depth is baked wall shading; south cliff is void so actors must not be visible beyond island edge (clamped) |
| Tall monolith / gate | Actors passing behind its legs | Y-sort with radius 96 vs actor radius; split-arch leaves gap (visual only — no special casing) |

---

## 4. Material Atmospheric Perspective (4-Band Fog + DOF)

### 4.1 4-Band World Fog (replaces prior 2-band center fog)

Each band is a horizontal screen rect tinted and alpha-modulated by `worldY` distance from camera center — creates depth through desaturation, not blur.

| Band | Screen Y range | Color / Alpha | Trigger |
|---|---|---|---|
| Band 0 — North haze | `wallBaseY - 60*zoom` → `wallBaseY` | `Color(0xFF1C3744) alpha 0.10 → 0.0` | Always — subdues north wall lancet glow distance |
| Band 1 — Mid plaza veil | `southEdgeY * 0.35` → `southEdgeY * 0.58` | `Color(0xFF7B6BB8) alpha 0.06` | When camera Y > 900 (sees mid) |
| Band 2 — South cliff bloom | `southEdgeY - 68*zoom` → `southEdgeY + cliffDropH` | `Color(0xFF231B42) alpha 0.16` vertical gradient to void | Always when `southEdgeY < height+cliffDropH` |
| Band 3 — Near-ground AO dust | `southEdgeY - 180*zoom` → `southEdgeY` | `Color(0xFF120B08) alpha 0.04` dust motes | Interior biomes only |

Drawn in `PASS 7` after all world, before UI — `Brush.verticalGradient`, no extra textures.

### 4.2 Depth of Field Bands (fake DOF via sprite scale + alpha, not blur)

True Gaussian blur is too expensive on Canvas. **Banded DOF** sells focus by:

- **In-focus band:** Actor/obstacle whose `worldY` within `±220` of `camY` rendered at `alpha 1.0, pixelScale 1.0`.
- **Out-of-focus** (distance `>220` and `>650`): `alpha *= 0.86` (far) and `pixelScale *= 0.97` (slightly smaller/crisper, opposite of blur but reads as depth at low zoom). Fog tint already does desaturation, so blur omission is forgiven.
- Applied to **actors only** (not tiles/obstacles) to avoid tiling shimmer.

Measured cost: 0ms extra (alpha/scale multiply already in `drawCrispSprite`).

### 4.3 Material Perspective Coefficients

Per `MATERIAL_TABLE` entry (wood, stone, grass, moss, water, metal, magic), add `fogResponseFactor`:

- Stone / metal: `1.35×` band 0 haze (they grey out fastest with distance — sells north wall recession).
- Grass / foliage: `0.85×` (stays saturated slightly longer — mid parks pop).
- Water: `1.0×` but also reflect band color (already handled in `sanctuaryWaterTiles` deep indigo 0xFF0F1A2E).
- Implementation: multiply band alpha by `fogResponseFactor` when tinting prop glow (not per-pixel — per-prop glow oval tint).

---

## 5. Ground Contact & Height Consistency

- Every sprite bottom aligns to `footY` (computed from `y + elevationZ`). Threshold step of door sits `+1px` below footY (on ground).
- Shadows project from `footY`, not from sprite center — keeps height illusion even during jump (`z>0` lifts sprite but shadow stays pinned).
- Trees: trunk shadow at footY, canopy shadow slightly north (`-ry*0.3`) because canopy is offset north over trunk (matches `skew` in `PixelStructureAtlas.spiritPineSprite` where trunk leans).
- Terrace walls: `elevationZ` positive (≈14 world) so their foot sits above plaza ground — shadow falls onto plaza below, drawn as foreshortened ellipse under elevated foot (reuse same caster but with elevation correction: `shadowY = worldToScreenY(y, ..., 0)` not `elevationZ`).

---

## 6. Performance Budget

| Addition | Cost | Guard |
|---|---|---|
| Depth band enum + `sortY` math | ~0.05ms/frame | None |
| 4-band fog rects (4 Brush gradients) | 4 rect draws | Cull when off-screen via `wallBaseY/southEdgeY` range |
| DOF alpha/scale on 8 actors | 0 | Inline |
| Sorted combined list (78+8) | 86 log86 sort | Once per frame — already paid; now includes actors, ~0.08ms |

No new textures, no RTT, no blur kernel — stays Canvas hardware line.

---

## 7. Files & Migration

- `WorldMapModels.kt` — add `DepthBand` enum, `visualHeightPx` fallback table, `SANCTUARY_HOUSE_ENTRANCES` cutout predicate.
- `PixelSpriteRenderer.kt` — restructure `drawWorldBackground` fog bands (4→current 1), plumb DOF band, keep PASS order.
- `SanctuaryLobbyScreen.kt` — PASS orchestration (actor vs obstacle interleave) + `south foreground walls` PASS 6.
- `SanctuaryShadowEngine.kt` (new) — owns shadow caster registry + height/fogResponse tables (shared with depth).

