# SHADOW SYSTEM DESIGN — True Unified Directional Shadows

> Replaces the current `drawDirectionalShadow34` generic ellipse with a geometry-aware, height-aware, light-directional system. Target: 1-2 Canvas oval draws per caster at ≤0.8ms total on mid-tier Android @60fps.

---

## 1. Current State (Baseline to Replace)

```kotlin
// PixelSpriteRenderer.kt — current fake shadow
private fun DrawScope.drawDirectionalShadow34(cx, footY, halfW, halfH, alpha=0.52f) {
  drawOval(Color(0xFF0A0C14).copy(0.58*alpha), Offset(cx-0.68halfW+8, footY-0.6halfH+6), Size(2.35halfW,1.65halfH))
  drawOval(Color.Black.copy(0.90*alpha), Offset(cx-0.88halfW, footY-0.85halfH), Size(1.75halfW,1.35halfH))
}
```
**Defects confirmed by WORLD_GRAPHICS_BASELINE audit:**

| Defect | Evidence |
|---|---|
| Generic axis — not directional | Fixed +8,+6 offset for every prop, ignores light azimuth |
| Ignores height/silhouette | Tree canopy and rock use same 2-oval as armory building; no footprint shape (RECT vs ELLIPSE vs POLYGON) |
| Ignores light direction & camera zoom in length | `halfW/halfH` hand-tuned per call site, not derived from `heightPx`/`elevAngle` |
| No softness / AO separation | Contact AO collapsed into second oval with uniform alpha; double-counts over semi-transparent roofs |
| Fixed alpha not material-aware | Stone vs grass vs water receive same opacity; blooms over glow decals |
| Receiver unaware | Shadows draw under sprite but bleed over elevated terrace walls (south cliff drop) |

---

## 2. Unified Light Model

### 2.1 Directional Light (single source, all casters)
```kotlin
object SanctuaryLight {
  const val AZIMUTH_DEG = 142f        // from north clockwise — light FROM north-west, shadow cast TO south-east
  const val ELEVATION_DEG = 48f       // height angle above horizon — lower = longer shadows
  const val COLOR_ARGB = 0xFFE8E0C8 // cool moonlight (desaturated ivory)
  val DIR_2D: Offset                 // normalized ground-plane projection: (sin az, cos az)
  val TAN_ELEV: Float                // tan(elevation) for length: L = H / tan(elev)

  // Precomputed per frame:
  // DIR_2D = ( sin(142°)=0.615, cos(142°)=-0.787 ) rotated to world X=+east, Y=+south
  // In world space Y+ south, shadow offset = DIR_2D_shad * length
  // shadowGround = foot + shadDir * (heightPx / tanElev)
  // shadDir = direction shadows are cast TOWARD = +SE (complement of light FROM NW)
}
```
- **Why NW light?** Sanctuary north wall has buttresses + lancet windows facing south — NW grazing light rakes wall depth without washing out plaza center glow. Aligns with existing tower-gate portal back-light (already north).
- **Elevation 48°** chosen: typical building height `h≈92px` body + 38px roof ≈ 130 world units scaled 0.8 → `L = 130 / tan48 (≈1.11) ≈117` world units ground length → ~84px screen at zoom 0.62 — long enough to be dramatic, short enough not to overlap neighboring zone rings.
- **Zoom handling:** Length computed in world units; conversion to screen via `*zoom` only at draw, so shadow world length is zoom-invariant (physically correct).

### 2.2 Per-Caster Shadow Extrinsics (height, footprint, silhouette)

```kotlin
enum class ShadowSilhouette { RECT, ELLIPSE, CUSTOM_POLYGON, IMPOSTOR_ELLIPSE }

data class ShadowCaster(
  val id: String,                     // mirrors WorldObstacle.id
  val heightPx: Float,                // vertical extrusion world units (from foot to roof ridge / canopy top)
  val footprintRX: Float = footprintHalfW, // half W of ground contact
  val footprintRY: Float = footprintDepthNorth * GROUND_ELLIPSE_RATIO, // foreshortened
  val silhouette: ShadowSilhouette,
  val softnessPx: Float = 6f,         // blur radius equivalent (extra outer oval)
  val opacity: Float,                 // material-aware (stone 0.38, wood 0.32, foliage 0.28, water 0.0)
  val receivesShadows: Boolean = true,
  val customPoly: List<Offset>? = null // for half-timber gables (authored polygon approximating roof overhang)
)
```

**Height registry (authored, not inferred):**

| Category | Example PropKinds | heightPx (world) | Silhouette | Reason |
|---|---|---|---|---|
| Large facility building | FACILITY_HERO_QUARTERS/RESTAURANT/ARMORY (half-timber 92+38) | 118 | RECT (bodyW×) + CUSTOM gable | Roof eave projects south 4px — need rect with notch |
| Synthesis lab | FACILITY_SYNTHESIS_LAB | 110 | RECT | Flat-roof apothecary |
| Monument | TOWER_PORTAL_GATE (split monolith) | 145 | CUSTOM (split pillars + portal slit) | Portal interior should not cast |
| Townhouse / Cottage | MEDIEVAL_TOWNHOUSE, SANCTUARY_COTTAGE | 96 | RECT | 2-story + dormer |
| Tower / wall | FORTRESS_CORNER_TOWER, TERRACE_CLIFF_WALL | 68 / 18 | RECT | Tower casts long narrow |
| Sakura / Oak / Pine | SAKURA_BLOSSOM_TREE (canopy 78×52), OAK_TREE, CYPRESS | 86 / 72 / 58 | ELLIPSE (canopy) + small RECT trunk | Canopy self-shadows ground as ellipse; trunk as thin rect beneath |
| Hedge / Bush | GARDEN_HEDGE_BUSH | 22 | ELLIPSE | Low, very soft |
| Rock | MOSSY_BOULDER | 18 | ELLIPSE | Tiny but dense AO |
| Prop furniture interior | TAVERN_COUNTER, BOOKSHELF | 28–36 | RECT | Interior-only, dimmer |

Opacity tuned per material table in `SanctuaryDarkFantasyShaders.MATERIAL_TABLE` (stone 0.38, wood 0.32, foliage 0.28, metal 0.34, grass 0.22).

---

## 3. Shadow Kinds & Rendering Passes

### 3.1 Three-Layer Shadow Stack (drawn in ground pass, before sprite, clipped to ground)

1. **Contact AO** — 1.05× footprint ellipse, dense (opacity 1.8× base, limited to footprintRY*0.9), color `Color.Black` — pinned at `footY`.
2. **Cast soft shadow** — stretched ellipse offset by `shadowVec = DIR_2D_shad * (heightPx / TAN_ELEV)` in world, then foreshortened `ry = rx*GROUND_ELLIPSE_RATIO`. Size `rx = footprintRX*0.92`, `ry = heightPx*0.18 + footprintRY*0.5`. Drawn as **single oval** with softness halo (second larger oval at `alpha*0.35` +6px blur).
3. **Canopy / Roof self-shadow** (foliage/building only) — secondary small dense ellipse directly beneath canopy overhang, offset 0.55× cast length, used to darken ground under eaves.

**Draw order per caster:** soft cast (outer halo) → soft cast (core) → contact AO. Total 3 ovals per caster (first two can合并 into one gradient on low-end — adaptive quality).

### 3.2 Special Cases

- **Tree trunk vs Canopy:** Trunk draws `RECT` thin (w=10) base shadow; canopy draws large `ELLIPSE` slightly north of trunk (canopy center offset `-ry*0.3`). Two casters per tree if needed; or single composite where canopy ellipse is 70% of shadow.
- **Building roof overhang & wall:** Roof eave casts distinct 4px-south lip shadow (extra thin rect `w+8, h=4` at `footY+1` with darker 0.42 alpha) to sell the 3/4 depth of `drawPitchedRoof34` sideDepth bevel.
- **Rocks volume:** Flat base ellipse + tiny inner highlight cutout (inverted oval via DestinationOut) to hint top-face lift — cheapest volume cue, no 3D mesh.
- **Characters:** Foot elliptical `Color(0x7C000000) 44×13` stays, but offset by light dir (`+ shadDir*5f`) not fixed; plus shield/aura rings remain foreshortened.

### 3.3 Clipping & Receiver Correction

- Clip cast shadow to **ground only**: skip drawing when `foot world Y < wallMargin + 40` (north wall top, where shadow would project onto wall face incorrectly). Terrace walls are vertical — shadows on them need separate wall projection (future, out-of-scope; just clip).
- South cliff drop: if `southEdgeY - footY < 20*zoom` (near cliff), fade shadow alpha `*= clamp(dropDist/20, 0,1)` so shadow doesn’t spill into void.
- Receiver tint: multiply shadow color by underlying `MATERIAL_TABLE` ambient (stone 0.92, grass 0.85) — implemented as alpha modulation, not blend-mode, to stay cheap.

---

## 4. Softness, Atmospheric Perspective & Time

- **Softness falloff:** Larger `heightPx` → softer penumbra (higher sun → sharper). Formula: `softnessPx = lerp(4f, 10f, heightPx/120)`, outer halo `alpha*=0.35`, `rx+6*softness/10`.
- **Distance fade (perspective):** Shadows farther from camera (higher world Y? actually south is fore) fade slightly — `alpha *= 1 - 0.12 * (worldY - camY)/worldHeight` so north distant shadows are subtler, matching fog.
- **No day-night flicker:** Light is static moonlight; `timeSec` affects only lantern/portal glows, not shadow direction (prevents shader re-calc per frame). A future sunset lerp (az+2°/minute) can be added without architecture change.

---

## 5. Performance & Caching (Android 60fps)

| Cost center | Baseline | New | Mitigation |
|---|---|---|---|
| Oval draws per frame | ~78 casters × 2 ovals = 156 | 78×3 = 234 worst | Cull: `cx,cy` frustum check (±380px) — ~40% off-screen at 0.62 → ~140 draws. Adaptive: on <30fps (measured frame >16.7ms for 3 frames), drop outer halo (→ 78×2=156, parity with baseline). |
| Allocations | 0 (inline colors) | `ShadowCaster` registry 80 entries, computed per frame `shadowVec` reused | Registry built once in `buildSanctuaryShadowCasters()`; `shadowVec = dir * (h/tan)` cached per caster unless `heightPx` changes (static). Dir constant. |
| Texture/blurs | None (flat color) | Same — no blur texture, only alpha layers | No offscreen render target; stays Canvas drawOval (GPU raster). |
| Overdraw | 2 layers | 3 layers over same ground | Early-Z: draw shadows **before** ground rings/characters so they sit under; no overlap double-blend over void. |

**Adaptive quality switch:**
```kotlin
if (avgFrameMs > 16.0f) SHADOW_QUALITY = REDUCED // 2 ovals
else SHADOW_QUALITY = FULL // 3 ovals
```

---

## 6. Integration Wiring

1. **New file** `app/src/main/java/com/example/game/pixel/SanctuaryShadowEngine.kt`
   - `SanctuaryLight` object, `ShadowCasterRegistry.build(obstacles): Map<id,ShadowCaster>`, `fun shadowWorldOffset(heightPx): Offset`.
2. **PixelSpriteRenderer.drawProp** → replace `drawDirectionalShadow34(cx,footY,halfW,halfH)` with `drawCastersShadow(caster, cx, footY, zoom)` (3-ovals, dir-offset).
3. **PixelSpriteRenderer.drawCharacterSprite** → light offset.
4. **WorldMapModels** → `WorldObstacle.shadowHeightPx` optional override (authored per building/rock/tree) — fallback to `when(propKind)` table.

No changes to `ObliqueCamera` constants; all math reuses `worldToScreen*` + `GROUND_ELLIPSE_RATIO`.
