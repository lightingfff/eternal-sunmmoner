# P23 — Environmental props cohesion (Sanctuary + Tower)

Scope: **art + asset loading only.** No prop anchor, radius, `blocksMovement`, `sortYBias`,
footprint or `PropKind` change. Draw order, `FilterQuality.None`, the P16 4×4 grass block cache,
road culling and the shadow silhouette cache are untouched. No runtime PNG generation, no new
render pass, no per-frame allocation.

Pipeline: `tools/mapforge_p23_props.py` (deterministic, offline, Pillow) →
`app/src/main/assets/sanctuary_p03/<name>.png` → manifest `size` + `sha256` →
`PixelStructureAtlas` (`assetSafe` PNG-first) → `drawCrispSprite` / `drawCustomProp34`.

---

## 1. Why the props were the last visible seam

P22 (vegetation) and P20/P21 (buildings) moved the world onto authored pixel PNGs. The small
props did not move with them, so Sanctuary ended up with three art languages in one frame:

| prop | P22 HEAD path | why it read as a placeholder |
|---|---|---|
| `FLOWER_PLANTER_BED` ×7 | `InteriorWorldRenderer.drawCustomProp34` — `drawRect` + five `drawCircle` | bright `#D6CFC2` box with smooth pastel candy circles; no top face, no shadow registration |
| `STREET_LANTERN` ×6 | `PixelStructureAtlas.buildStreetLantern` ×3 frames | flat purple post, black cartoon outline, flat yellow octagon; the flame changed the **outline radius** so the lamp silhouette jittered every frame |
| `WOODEN_BENCH_34` ×4 | `buildWoodenBench34` | flat orange front elevation, black outline, no seat top plane, no rear legs |
| `ANCIENT_RUIN_ARCH` ×6 (F1/F2) | `buildAncientRuinArch` | two flat purple brick rectangles floating **10 px above** their own shadow ovals with a neon-lime blob on top |
| `STONE_RETAINING_WALL` | `buildStoneRetainingWall` | flat purple rectangle, mint-green blobs, floating base |
| `ANCIENT_MONOLITH` (F1 ruins) | `buildAncientMonolith` | flat indigo rectangle with a sky-blue `+`, floating base |
| `TRAINING_DUMMY` ×10 | `PixelSpriteRenderer` `drawRect`/`drawCircle` | bright daytime orange-tan vector rectangles on the dark P19 training soil |
| `WEAPON_RACK` ×4 | `PixelSpriteRenderer` `drawRect` | same vector read, no shadow sprite registered |

The wall-shadow pass (`SanctuaryEnvironmentShadows.sprite`) returned `null` for the planter,
dummy and rack, which is why they also had no projected cast shadow and floated on the soil.

---

## 2. What was authored

All eleven sprites share the buildings' materials and their single key light (upper-left,
`SanctuaryLight` azimuth 142°, elevation 48°), each mass has a top plane + lit face + shaded
return face, and every base reaches the canvas ground row.

| asset | canvas | colours | replaces | contract |
|---|---|---|---|---|
| `flower_planter.png` | 64×36 | 43 | Canvas curb + candy circles | same 60×13 curb footprint, drawn bottom-centre at `cy + 8s`, `1s` scale (`ObliqueSpriteGeometry.pixelScale = 1f`, `footOffset = 8f`) |
| `street_lantern_0/1/2.png` | 32×66 ×3 | 15 | procedural 3-frame lantern | **29 of 32 columns byte-identical across all frames**; only x 21–23 (the glass flame) differ |
| `wooden_bench.png` | 58×36 | 11 | `buildWoodenBench34` | seat top face + apron + 4 legs on the ground row |
| `ruin_arch.png` | 96×86 | 18 | `buildAncientRuinArch` | genuinely **open** archway (`alpha = 0` passage) + 4 px 3/4 stone intrados + stepped tumbled crown; both piers on row 85 |
| `retaining_wall.png` | 88×48 | 19 | `buildStoneRetainingWall` | coursed wall + lit coping + ivy clusters |
| `monolith.png` | 44×74 | 13 | `buildAncientMonolith` | leaning shaft, carved rune channel, stone plinth |
| `training_dummy.png` | 40×58 | 13 | `drawRect`/`drawCircle` dummy | cross-stand grounded on row 57 (`H-1`), straw torso in the P19 sand value range, iron bands, painted target |
| `weapon_rack.png` | 68×48 | 10 | `drawRect` rack | A-frame with two rails and five practice weapons |
| `striking_post.png` | 36×86 | 14 | `buildTrainingStrikingPost` | stone collar, oak pillar, three rope zones, iron pegs |

Only new assets were created — **no existing PNG was overwritten**, so `oak.png`, `cypress.png`,
`boulder.png`, `barrels.png` and `pillar.png` keep their bytes.

### 2.1 Planter: keep the footprint, fix the material

The Canvas version drew a 60 × 13 unit curb centred on the anchor. The PNG keeps that footprint
exactly (64 × 36 canvas, 57 px of curb, blitted bottom-centre at `cy + 8s` with `1s` scale in
`InteriorWorldRenderer`, matched by `ObliqueSpriteGeometry.pixelScale = 1f` and `footOffset = 8f`
so `SanctuaryEnvironmentShadows` casts a 1:1 shadow instead of a 2× wide shadow), so
the seven `sanctuary_flower_planter_*` obstacles keep their radius, their collider and their
planting positions. What changed is the material: a foreshortened lit top face (5 px of stone),
a coursed front face, a shaded right return, a plinth on the ground row, and three bounded leaf
clusters with twelve blooms instead of five smooth circles.

### 2.2 Lantern: lock the static pixels

The procedural frames changed `fillCircle` radius between frames, so the *silhouette* moved — a
lamp that jitters is a bug even when it "flickers". The three authored frames share every pixel
of the base, post, bracket and glass housing; frame 1 is one pixel brighter inside the glass,
frame 2 leans the flame the other way. The contract test compares all 32 × 66 pixels across all
three files and asserts the differing columns are contained in x 19..25.

### 2.3 Training yard: measured, then toned down

The first authored dummy used a straw ramp around `#BDA659`, which in the engine render at 0.88
came out brighter than the sand arena and read as a row of scarecrows. It was re-toned into the
P19 sand/soil range (`#5F5230 → #877445`) on the render evidence, and the iron bands now carry
the contrast instead of the straw.

---

## 3. Loading and fallback

```kotlin
// PixelStructureAtlas — PNG-first, procedural fallback, cached by SanctuaryArtAssets
val woodenBenchSprite: ImageBitmap get() =
  SanctuaryArtAssets.assetSafe("sanctuary_p03/wooden_bench.png") ?: woodenBenchFallback
private val woodenBenchFallback: ImageBitmap by lazy { buildWoodenBench34() }

// props that never had a procedural builder return null, and the caller keeps its Canvas path
val flowerPlanterSprite: ImageBitmap?
  get() = SanctuaryArtAssets.assetSafe("sanctuary_p03/flower_planter.png")
```

`SanctuaryArtAssets.assetSafe` returns `null` on a missing/corrupt asset, never throws, never
caches a failure, and caches a success exactly like `asset`, so steady-state cost stays one blit
per prop per frame. Call sites:

* `PixelSpriteRenderer.drawProp` — `TRAINING_DUMMY`, `WEAPON_RACK` blit the authored sprite when
  present and fall through to the previous Canvas drawing when not (`return@drawProp` keeps the
  doorway-indicator tail common to every branch);
* `InteriorWorldRenderer.drawCustomProp34` — `FLOWER_PLANTER_BED` blits the authored sprite with
  the same `cy + 8s` anchor the Canvas curb used, then returns `true`;
* `PixelStructureAtlas.streetLanternFrames` — per-frame `assetSafe` with the procedural frame as
  fallback, so a partial deploy (one missing frame) still animates.

### 3.1 Shadow registration

`SanctuaryEnvironmentShadows.sprite` now resolves `FLOWER_PLANTER_BED`, `TRAINING_DUMMY`,
`WEAPON_RACK`, `ANCIENT_RUIN_ARCH` and `ANCIENT_MONOLITH`/`SUMMON_OBELISK`. Registering them is
what turns their painted blob into the shared projected silhouette + contact AO, and it is why
`drawProp`'s `legacyShadow` branch is skipped for them rather than drawn twice.

`SanctuaryShadowRegistry.heightFor` gained real heights for the five kinds (ruin arch 46, monolith
50, planter 10, dummy 42, rack 36) instead of the `24f` default, so the projected silhouette is
proportionate to the caster. No projection maths, opacity curve or softness table was rewritten.

---

## 4. Measured results

| asset | decoded ARGB_8888 | semi-transparent px | ground contact |
|---|---|---|---|
| flower_planter | 9.0 KB | 0 | full curb width |
| street_lantern ×3 | 8.2 KB each | 0 | base on last row |
| wooden_bench | 8.2 KB | 0 | 4 legs |
| ruin_arch | 32.2 KB | 0 | both piers + rubble |
| retaining_wall | 16.5 KB | 0 | full wall width |
| monolith | 12.7 KB | 0 | plinth |
| training_dummy | 9.1 KB | 0 | cross-stand feet |
| weapon_rack | 12.8 KB | 0 | both side posts |
| striking_post | 12.1 KB | 0 | stone collar |
| **total** | **140.9 KB** | **0** | — |

Net decoded RAM change: **+140.9 KB** for eleven new prop bitmaps (the Canvas props they replace
allocated nothing persistent, so this is a real, small increase and it is stated as one).

Draw cost: the planter/dummy/rack paths moved from N vector draw calls per frame to **one cached
blit** each, which is cheaper; the bench/arch/wall/monolith/post replaced a procedural bitmap blit
1:1. No per-frame allocation, no new pass, no runtime decode after warm-up.

---

## 5. Gates

| gate | status |
|---|---|
| `tools/mapforge_p23_props.py --check` (alpha, contact, palette, frame lock) | PASS |
| lantern static-pixel lock (29/32 columns identical) | PASS |
| `MapForgeP23ContractTest.propsReachTheGroundRowAndLanternFramesAreLocked` | **PASS in CI** |
| `MapForgeP23ContractTest.reAuthoredPropsCastApprovedSilhouetteShadows` | **PASS in CI** |
| `SanctuaryMaterialTest.manifestAssetsDecodeAndMatchRuntimeAtlas` | **PASS in CI** |
| device FPS / jank / thermal | **NOT TESTED** — no physical device |

## 6. Not done in this pass (stated, not hidden)

* `TALL_GRASS_CLUMP`, `WILDFLOWERS` and `TATTERED_BANNER` are still drawn with Canvas primitives.
  They are 1–7 px tall at gameplay zoom and were not the visible seam; they are next in line.
* The Sanctuary training yard's **sand patch** (`drawOrganicTrainingSandArena`) is a renderer
  ellipse, not a prop, so it is out of scope for an asset pass.
* `PropKind.TRAINING_GROUND` still has no Sanctuary obstacle; no sprite or obstacle was invented.
