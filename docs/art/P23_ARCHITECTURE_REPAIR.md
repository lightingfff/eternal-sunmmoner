# P23 — Architecture 2.5D integration repair (Sanctuary + Tower Gate)

Scope: **art + asset loading only.** No camera change (`ObliqueCamera.OBLIQUE_Y_SCALE = 0.72`),
no island polygon, road width, prop anchor, footprint, collider, entrance trigger, zone, sort
key, elevation or `PropKind` change. `PixelSpriteRenderer.drawProp` still issues one
`drawCrispSprite` blit per building with `FilterQuality.None`. No render pass, shader,
post-process, per-frame decode or runtime generation was added.

Pipeline: `tools/mapforge_p23_architecture.py` (deterministic, offline, Pillow, no external art)
→ `app/src/main/assets/{premium_slice,sanctuary_p03}/<name>.png` → manifest `size` + `sha256` →
`PixelStructureAtlas` (`assetSafe` PNG-first, procedural fallback) → `drawProp`.

---

## 1. The failure class this pass fixes

P20 and P21 both did the same thing well and the same thing badly. They authored beautiful
material detail — coursed masonry, shingle courses, mullioned windows, iron door furniture — on
top of **a silhouette that did not close**. Measured at the P22 HEAD:

| sprite | columns with an interior transparent band |
|---|---|
| `summoning_hall.png` | **199 / 268** |
| `hero_quarters.png` | **126 / 200** |
| `restaurant.png` | **125 / 206** |
| `synthesis_lab.png` | **118 / 188** |
| `hero_registry.png` | **103 / 188** |
| `tower_gate.png` | 42 / 260 |
| `armory.png` | 13 / 190 |

Those are not stylistic gaps. They are places where the camera sees grass or sky *through a
building*. The three mechanical causes, all reproduced in the P21 source:

1. **Roof plane ended on one line, wall started on another.** `shingle_plane` painted up to
   `eave`, the wall body started at `eave + rise_l`, and nothing filled the band between them.
   In the Summoning Hall this was 10 px of open sky between the entablature and the capitals.
2. **Detached 1 px ridge highlight.** `hip_roof` drew `ridge` one row above the ridge cap with
   the row between left transparent, which appeared as a floating wire above every roof.
3. **Detail painted outside its mass.** A chimney cast-shadow polygon, a turret roof overhang or
   a smoke puff placed one or two pixels past the wall silhouette produced single-pixel islands
   in otherwise empty columns — and the per-column alpha holes they created are what the engine
   exposed as "the building has a gap".

The same class of defect hit the Armory's door, but in world space instead of pixel space: P20
painted it at `x 133..150, y 109..139`, which inverts to world **(1069.4, 601.7)** — 76 world
units east and 120 north of the playable trigger **(993, 655)**. The player walked to the painted
door and the prompt never appeared.

---

## 2. The structural rule set the generator now enforces

`tools/mapforge_p23_architecture.py` builds every mass in this order, and the order is the point:

```
1  silhouette first      one opaque quad per face (lit front, receding right return)
2  materials second      courses, quoins, joints painted INSIDE the silhouette
3  roof third            eave -> pitch -> ridge on top of a wall that already closes
4  join last             opaque soffit band between eave underside and wall top, with rafter tails
5  contact               plinth course + baked AO on the ground row
```

plus three automated gates that fail the build (not the reviewer):

* `validate()` rejects any column with an interior alpha run, any semi-transparent pixel, a
  non-transparent top/left/right border, a narrow ground contact, or less than 28 % coverage;
* `seal_seams()` closes any 1–7 px slit between two opaque runs in the same column using the
  eave's own underside tone, so even a rounding slip reads as shadow under an overhang;
* `prune_specks()` deletes sub-12-pixel islands (1 px line spill), which is exactly the bug
  that produced the "detached ridge wire".

All 13 buildings (including `tower_gate.png`) pass with **zero vertical column holes**
(`GAP_ALLOW_X = {}`): Tower Gate preserves its twin-tower sky cleft by stepping the central
curtain wall's top row down (`y >= 34` across `x = 105..155` vs twin tower battlements `y <= 16`)
while supporting its parapet caps on stepped stone machicolations so zero floating overhang pixels
or vertical gaps occur.

---

## 3. Per-facility changes

| facility | PropKind | anchor | scale | canvas | door / opening |
|---|---|---|---|---|---|
| Armory | `FACILITY_ARMORY` | (995, 655) | 0.80 | 190×166 | **enterable**, re-authored onto trigger (993, 655) → sprite x 94, sill 163 |
| Hero Quarters | `FACILITY_HERO_QUARTERS` | (825, 790) | 0.80 | 200×168 | enterable (823, 790) → x 99, sill 165 |
| Restaurant | `FACILITY_RESTAURANT` | (1105, 925) | 0.80 | 206×172 | enterable (1103, 925) → x 102, sill 169 |
| Hero Registry | `FACILITY_HERO_REGISTRY` | (785, 1185) | 0.80 | 188×166 | **zone-only → arcaded loggia** |
| Synthesis Lab | `FACILITY_SYNTHESIS_LAB` | (1270, 555) | 0.80 | 188×168 | **zone-only → open workshop bay** |
| Summoning Hall | `FACILITY_SUMMONING_HALL` | (2430, 2005) | 1.08 | 268×206 | zone-only; rune stage at sprite (111, 176) |
| Tower Gate | `FACILITY_TOWER_GATE` | (1960, 585) | 0.96 | 260×224 | zone-only; dark rift box at sprite (130, 120) ± (16, 27) |

### 3.1 Armory — the measured defect, fixed in art (not by moving the trigger)

The entrance constant was **not** touched (`SanctuaryHouseEntrances` still says 993, 655, and
`MapForgeP23ContractTest` asserts both numbers). The art moved instead:

```
door_cx   = W/2 + (doorWorldX − obs.x) / (2·scale)     = 95 + (993 − 995)/1.6  = 93.75
door_rows = (H−3) − (obs.y − doorWorldY)·0.72/(2·scale) = 163 − 0                = 163.00
```

The doorway is cut at canvas centre X with its leaf base on row 163 and two stone steps on rows
164–165, so the renderer's `cy + 6s` blit puts the stoop exactly on the world anchor. Residual
error: **0.25 px in X, 0 px in Y**, i.e. 0.4 world units — the previous art was off by 76 and 120.

Beyond the door, the Armory was rebuilt as one mass: single timber-shingle roof language (the P20
sprite mixed warm-brown and blue-slate planes in the same roof), a coursed chimney whose cap no
longer floats, exposed timber framing on the plaster storey, quoins on both corners, and a
proper foundation course. The painted chimney shadow is now a tone applied *inside* the roof
polygon instead of a free-standing quad in the sky.

### 3.2 Hero Quarters & Restaurant — recessed portals that read as recesses

Both keep the canvas, the door column and the `H−3` sill row exactly where P21 put them, so
`MapForgeP21ContractTest.enterableDoorsLandOnTheirWorldTriggers` still passes untouched. What
changed is the door *construction*: outer stone surround → lit left jamb → shaded right jamb →
lintel + voussoirs + keystone → near-black reveal → leaf inset 2 px on every side → warm spill on
the sill row only → two projecting steps that reach the ground row. The previous leaf was a
plank rectangle butted straight against a thin frame, which is why it read as a decal.

Supporting structure added: Quarters gets a pitched porch hood resting on two posts that reach the
ground (the P21 porch was a floating shingle slab over the door head), a dormer, a lean-to wing
and a louvred vent; Restaurant gets a striped canvas awning with a scalloped valance and a
projecting entrance gable that frames the real door.

### 3.3 Hero Registry & Synthesis Lab — stopping the false affordance

Both are zone-only facilities: there is no interior map and no `SanctuaryHouseEntrances` entry.
P21 nevertheless painted a residential plank door with a lantern on both, so the art advertised
an entrance that does not exist — the exact "looks like a door but you cannot go in" complaint.

They now carry a **niche** instead: a recessed arcade opening with a stone counter lip across the
bottom. The Registry reads as a civic loggia (three arches, bronze plaque row, ledger lectern on
the counter, stone bench, bell turret with clock); the Lab reads as an open workshop bay with a
bench, a retort and a weathered bronze/copper condenser stack grounded on a stepped stone wall
corbel breast with lead roof flashing. Neither invites a walk-in, both still look like they
belong to the building they are cut into.

### 3.4 Summoning Hall — one continuous structure

The P21 sprite had the entablature floating 10 px above the colonnade with sky between them, the
right return stopping short of the cornice, and the podium hovering over a 33 px transparent band
above the plaza. The rebuild fixes the vertical order and closes every joint:

```
pediment (30..66) → entablature (66..83, cornice = its top 3 rows)
                  → capitals (84..89) → column shafts (90..145)
                  → horizontal frontal stylobate + oblique right return (146..161)
                  → terrace apron (142..205, opaque bed)
cella wall behind the colonnade (84..161); receding return's top edge = entablature underside
```

Because the apron is now an opaque bed and the 3-tier stylobate runs horizontally across the
frontal colonnade before turning along the oblique right return (with a coursed limestone cheek
beneath), the plaza tiers sit on stone instead of grass and the podium's outer corners align
cleanly with the locked 3/4 camera. The rune ring is still authored
at sprite **(111, 176)** — the inverse of the zone centre (2380, 1930) — so the hero stands on
the visual focus. `MapForgeP23ContractTest` re-derives that position and counts lit rune pixels.

### 3.5 Tower Gate — solid twin towers, dark aperture

The aperture contract is unchanged and re-verified: the renderer strokes its animated rift oval at
sprite (130, 120) ± (16, 27), so the painted box is dark (1127 dark pixels, **0** bright ones),
the ring's top edge sits on opaque stone, and the only painted light is the cold spill on the
threshold below it (150 px) — the animation then appears to come *from* the gate.

Structurally the gate is now: basement dais (solid, 200 → 223) → battered curtain wall (62 → 200)
with corbelled crenellated parapet → two proud battered towers in locked 3/4 oblique projection
(lit frontal face + darker 6 px right return, corbelled machicolations under the battlement caps,
quoins, plinth, loopholes, zero outer rabbit-ears and zero vertical ruler lines) → heavy lintel
carried on stepped stone corbels with carved runes. The recessed wall is
blended **26 %** darker than the proud towers (was 12 %), which is what finally separates the
three planes at gameplay zoom.

---

## 4. Sanctuary background buildings (new PNGs, same blit contract)

`MEDIEVAL_TOWNHOUSE_A/B` and `SANCTUARY_COTTAGE_BROWN/SLATE` were the last callers of
`PixelSanctuaryLandmarks.paintAnimeHalfTimberHouse`. They shipped with `bottomPadding = 13`
against the renderer's `drawFootOffset = 6 / pixelScale 2 = 3`-row expectation, so they floated
about 20 world units north of their anchors, plus pastel-accented doors and `#FFEE55` window grids.

| asset | replaces | canvas | anchor | scale |
|---|---|---|---|---|
| `townhouse_a.png` | `MEDIEVAL_TOWNHOUSE_A` procedural | 152×156 | (655, 930)* | 0.84 |
| `townhouse_b.png` | `MEDIEVAL_TOWNHOUSE_B` procedural | 144×160 | — | — |
| `cottage_brown.png` | `SANCTUARY_COTTAGE_BROWN` | 132×144 | (495, 1075) | 0.86 |
| `cottage_slate.png` | `SANCTUARY_COTTAGE_SLATE` | 132×144 | (1395, 675) | 0.82 |
| `fortress_tower.png` | `FORTRESS_CORNER_TOWER` | 80×118 | 8 perimeter octants | 0.96 |
| `gate_tower.png` | `SOUTHERN_GATE_TOWER` | 96×136 | (1360, 2395), (1860, 2395) | 1.02 |

\* `nw_outer_slate_house` uses the `_B` kind at that anchor; both townhouse variants are authored
because the atlas exposes both.

Every one of them now has `bottomPadding = 0` — the ground row is the last opaque row — and a
painted plinth, so the shared contact shadow lands under the wall instead of 20 units north of it.
Their doors are **closed and unlit** (dark recess, cross-braced leaf, no amber spill, no lantern):
these houses have no interior, and the closed door is the honest signal.

The perimeter towers were the other visible defect: the procedural builder used
`mortar = #5C5446`, *brighter* than its stone (`#4A4E5E`), which printed a glowing tan wireframe
over a dark shaft, and its silhouette was an axis-aligned rectangle. They are now elliptical with
courses that follow the curve, joints darker than the stone, corbelled parapet rings and merlon
caps, and a battered plinth on the ground row.

**A correction made from the engine render itself:** the first P23 pass painted these towers in
`LIME`, the Summoning Hall's pale limestone. In the render at 1.48 they read as bleached white
towers standing in front of SanctuaryWorldRenderer's dark slate curtain wall and dominated the
north skyline. They now use `WALLSTONE` (the Tower Gate's family, one step up), which sits with
the wall instead of in front of it.

---

## 5. What deliberately did **not** change

* No `WorldObstacle`, `PropKind`, `SanctuaryZoneZone`, `SanctuaryHouseEntrances`, `PathSegment`,
  `TerrainPatch` or `SanctuaryIslandGeometry` value.
* No change to `ObliqueCamera`, `ObliqueSpriteGeometry`, canopy occlusion fractions
  (`0.52–0.56`), `SanctuaryEnvironmentShadows` projection maths or depth sort.
* `FACILITY_TRAINING_GROUND` still has **no** obstacle on the Sanctuary map (the SW training yard
  is an open soil arena), so no sprite is invented for it and no obstacle was added.
* `oak.png` (P18) and `cypress.png` (P20) bytes are asserted unchanged by the contract test.
* No new render pass, no shader, no blur, no bloom, no full-screen effect, no runtime PNG work.

---

## 6. Gates

| gate | status |
|---|---|
| `tools/mapforge_p23_architecture.py --check` (holes, margin, contact, coverage, door targets) | PASS |
| generator determinism (re-run ⇒ identical bytes) | PASS (`sha256` pinned in the contract test) |
| `tools/validate_assets.py` | PASS |
| `tools/validate_lyra_pack.py` | PASS |
| `tools/validate_mapforge_p22.py` (no P22 regression) | PASS |
| `MapForgeP23ContractTest` (7 tests) | **PASS in CI** |
| `MapForgeP23RenderTest` (24 engine frames) | **PASS in CI** |
| `MapForgeP20/P21/P22` contract + render suites | **PASS in CI** |
| `:app:assembleDebug` ("Build Eternal Summoner APK") | **PASS in CI** |
| device FPS / jank / thermal | **NOT TESTED** — no physical device in this environment |

## 7. Performance

Decoded bitmap cost is `W × H × 4` bytes. Nine canvases are unchanged in size (the seven
`premium_slice` facilities are the exact P20/P21 sizes replaying the same blit), and the six new
`sanctuary_p03` building PNGs **replace** procedural canvases that were built at the same
dimensions, so total decoded RAM for the building set is unchanged:

| set | decoded ARGB_8888 |
|---|---|
| 7 facilities (was 958.1 KB) | **958.2 KB** |
| 4 houses + 2 towers (was the same canvases, built procedurally) | **419.0 KB** |
| total building bitmaps | **1.35 MiB** |

Draw cost is unchanged: one cached blit per building per frame, same `drawCrispSprite` call, same
`FilterQuality.None`, same `SanctuaryEnvironmentShadows` silhouette path, no new pass, no
per-frame allocation. The only new runtime work is one PNG decode per building on first access,
cached in `SanctuaryArtAssets.images`; the procedural builders remain as the fallback when a PNG
is absent (`assetSafe` returns null, it does not throw).
