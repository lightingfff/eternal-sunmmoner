# P20 — 2.5D architecture + vegetation vertical slice (armory + cypress)

Scope: two existing sprites repainted in place. Sanctuary geometry, facility placement, road
widths, door triggers, collision, prop anchors, camera (`OBLIQUE_Y_SCALE = 0.72`), P16 4×4
grass batching, road culling and `FilterQuality.None` are untouched. No renderer code change,
no shader, no new draw call.

## 1. Selected slice (verified at HEAD)

| item | value |
|---|---|
| Building | **Armory** — `PropKind.FACILITY_ARMORY`, anchor `(995, 655)`, `scale 0.80`, footprint halfW 72 / depthNorth 54, doorway `(993, 673)` in `TowerFloorCatalog.buildSanctuaryMap` |
| Asset | `app/src/main/assets/premium_slice/armory.png`, 190×166, drawn by `drawCrispSprite` **bottom-centred** at `2.0 × zoom × scale` |
| Tree | **Cypress conifer** — `PropKind.CYPRESS_CONIFER_TREE`, props at `(1060,485)`, `(1155,470)`, `(1485,655)` |
| Asset | `app/src/main/assets/sanctuary_p03/cypress.png`, 48×88, ground anchor `[24, 83]`, upper-canopy occlusion split at `topFraction 0.54` |
| Ground | P19 soil/gravel/cobble and P18 grass, unchanged in this PR |

## 2. What was wrong with the old sprites

**Armory** (`armory.png`, 190×166)
- Roof read as one sticker plane: no ridge cap, no eave thickness, no overhang shadow, so the
  building had no volume above the wall line.
- Masonry was a uniform grid of same-sized blocks with a flat value; no rim light, no corner
  strengthening, no foundation course.
- Door was a flat brown rectangle in a thin frame: no recess, no iron furniture, no threshold.
- Windows were flat yellow rectangles, no frame, no mullion, no sill.
- No contact shadow: the sprite did not sit on the ground.

**Cypress** (`cypress.png`, 48×88)
- Foliage tiers were horizontal plates: at 1× the tree read as stacked boxes (`SEAM`-style
  bands), with a symmetrical cone silhouette.
- One flat green ramp, no cavities, no lit/shade half, so no depth.
- Trunk was a thin straight column with no bark, flare or footprint darkening.

## 3. What was authored (`tools/mapforge_architecture.py`, deterministic, offline)

### Armory
- **Roof as two real planes**: a timber plane from the eave edge `(14,96)-(99,112)` up to the
  ridge `(58,30)-(150,16)`, and a slate hip return `(99,112)-(186,84)-(150,16)`. Both are
  filled with staggered courses (`_shingle_plane`: per-shingle tone from a 3-step ramp, a lit
  upper lip, a dark course seam), a 2 px ridge cap, an eave fascia and a **two-band overhang
  shadow** (`shadow` + `ao`) falling on the wall underneath.
- **Masonry** (`masonry()`): per-row courses of 9–21 px blocks with upper-left rim light,
  darker lower faces, vertical joints and occasional chipped corners; mortar is `#232b31`,
  never black. Front corner gets alternating **quoins**; a darker **foundation course** plus a
  contact **AO** polygon closes the base.
- **Exposed timber frame** on the lit face (posts + rail + brace) so the armory speaks the same
  language as the neighbouring medieval townhouses instead of reading as a plain stone box.
- **Door**: the exact pixel box `x 133..150, y 109..139` is preserved (verified by test), now
  with a stone surround + keystone, a 2 px recess, plank leaf, two iron straps with studs, a
  ring handle, warm spill on the threshold, three stone steps and a worn sill.
- **Windows**: framed, mullioned, sill + darkened sill shadow, restrained amber emission with
  an inner highlight (no blown-out yellow squares), timber shutters on the lit face.
- **Chimney** with cap, flue shadow, coursed stone and its own cast shadow on the roof; a
  handcrafted **hanging armory sign** (anvil silhouette) on an iron bracket.

### Cypress
- Trunk with bark strips, highlight, buttress roots and a dark foot line.
- Nine overlapping **branch sprays** (scalloped jagged underside, tapered top) with a shade
  half on the right, lit clusters on the upper left, thin cavities between tiers, branch tips
  poking out, dry hi-light tips on the crown and moss at the foot so it meets the P19 lawn.

Both sprites keep their previous canvas, anchor and door/occlusion geometry, so no world
coordinate, collider, entrance or sort key changed. Manifest `sha256` + `size` refreshed for
both (`premium_slice/manifest.json`, `sanctuary_p03/manifest.json`).

## 4. Evidence

`docs/visual-evidence/p20/`

| file | label |
|---|---|
| `engine_before_after_close.png` | **REAL ENGINE RENDER** — armory slice @1.48, before (main `72c4701`) vs after |
| `engine_before_after_default.png` | **REAL ENGINE RENDER** — @0.88 |
| `engine-render-before/*.png` | **REAL ENGINE RENDER** — 11 fixed cameras, pre-P20 assets |
| `engine-render/*.png` | **REAL ENGINE RENDER** — same 11 cameras, P20 assets |
| `armory_contact_sheet.png` | **ART CONTACT SHEET** — 190×166 at 1× / 2× / 3× nearest, before vs after |
| `cypress_contact_sheet.png` | **ART CONTACT SHEET** — 48×88 at 1× / 3× / 6× nearest |

Cameras are fixed in `MapForgeArchitectureRenderTest` (armory @0.36/0.88/1.00/1.48, side view,
door egress with an actor, actor in front / behind).

## 5. Gates

- `tools/validate_assets.py` → PASS (PNG CRC/mode/dimensions + manifest SHA + package)
- `tools/validate_lyra_pack.py` → PASS
- `MapForgeArchitectureContractTest` (Robolectric): armory 190×166 + door box still a dark
  opaque opening + manifest SHA; cypress 48×88 + anchor `[24,83]` + canopy/trunk density split;
  P19 families (8 grass / 6 soil / 4 gravel) and the Lyra atlas still resolve.
- CI `assembleDebug` [41432905414](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41432905414) success ·
  render job [41432905556](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41432905556) success (commit `327453c`).
- Device FPS / jank / thermal: **NOT TESTED** — no device in this environment.

## 6. Performance

- Two PNGs replaced, no new assets, no new families, no manifest contract change.
- Decoded memory: armory 190×166×4 B = **126 KB** (was 126 KB), cypress 48×88×4 B = **16.5 KB**
  (was 16.5 KB) — identical, because the canvases are unchanged.
- Draw cost unchanged: same sprite count, same `drawCrispSprite` call, same `FilterQuality.None`
  sampling, same shadow pass (`SanctuaryEnvironmentShadows`), no runtime generation, no
  per-frame allocations, no extra blend or offscreen pass.
- The generator runs offline only; nothing is produced at draw time.

## 7. Honest self-critique

- **Armory still reads flattest at 0.88.** The fixed 1.48 camera crops the roof (a camera
  authored before the taller roof volume), so the roof only reads fully at 0.88/1.00; the
  close-up evidence is dominated by the wall and door.
- **The timber plane's courses are subtle at gameplay zoom**; a later pass should raise seam
  contrast by one value step rather than adding more detail.
- **Cypress engine framing missed.** The three cypress props sit behind the armory sprite at
  close zoom and outside the gameplay-zoom frames I locked, so the tree's engine evidence is
  weaker than the building's: it is proven by the asset contract test + contact sheet, and its
  in-world behaviour is unchanged (same canvas, anchor and `topFraction`). Next bounded step:
  dump the actual runtime prop list once and re-shoot a cypress frame.
- **Only one building, one tree.** The other facilities (hero quarters, restaurant, synthesis
  lab, cottages) still use the P16-era sprites; the contrast difference is visible next to the
  new armory, which is exactly the next slice.
- No claim of parity with Brown Dust 2: this is readability/volume improvement at native scale
  only, judged in engine renders — not a side-by-side with any commercial asset.
