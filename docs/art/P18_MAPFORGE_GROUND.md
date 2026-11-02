# P18 — Map Forge: premium ground vertical slice

**Area:** central plaza → stone road → lawn (Sanctuary island, `sanctuary_p03`).
**Scope:** textures + one prop only. Layout, road width, buildings, entrances, collision,
camera (`ObliqueCamera`, ground Y 0.72), prop positions and interactive logic are untouched.

## 1. Baseline audit (why the old look was weak)

| family | files | unique colours (before) | visible failure at 4× |
|---|---|---|---|
| grass | 8 | 9–10 | one flat green plus confetti blades; no clusters, no value structure, reads as noise |
| cobble | 6 | 12 | 5 rows of identical rounded rectangles, uniform gap, no mortar depth |
| flagstone | 8 | 8–12 | 3 rigid courses of identical slabs, pasted border motif, olive cast |
| soil | 6 | 8 | flat brown, unchanged in this pass |
| gravel | 4 | 6 | confetti pebbles, unchanged in this pass |

Two structural causes, both verified in `tools/generate_sanctuary_art.py`:

1. **Low value count.** 8–12 colours per tile cannot carry weight grading (base → shade →
   highlight → mortar → moss), so every stone read as one flat shape.
2. **Shared perimeter motifs.** The old generator copied the same 2 px border and border
   motif into every variant, so mosaic repetition was pinned to the tile edges:
   a visible "grid" at 0.36 overview zoom.

## 2. How the new assets are constructed

`tools/mapforge_ground.py` (new, deterministic, seeded per asset):

- **Torus painting.** Motifs are painted on a 192×192 torus and cropped at 64..128, so every
  tile is seamless against itself; the shared 2 px perimeter contract is kept so mixed
  variants still meet cleanly, but the *interior* is now variant-specific
  (cobble courses carry a per-variant phase offset, which removes the repeated seam rows).
- **`stone()`** — one stone form: base polygon, 1 px top bevel from the light direction,
  1 px bottom shade, optional chipped corner, optional moss in a corner only.
  Colour grade: `shade → body → light` (+ `cool` / `wear` accents), 16–18 colours per family.
- **`blade_fan()`** — compact clump: dark halo polygon, root polygon, at most 3 short blades.
  Scale 0/1/2 per clump so the lawn has rhythm instead of uniform stamps.
- **Grass base** — three-tone mottle plus two low-contrast trough bands per tile.
- **`verge()`** — keeps the exact road-clip mask contract the renderer uses
  (`SanctuaryWorldRenderer` L307/L413), only re-authored pixels inside it.
- **`oak()`** — repaint at the original 112×136 with the same ground anchor, lit from the
  same direction as the rest of the prop set; no footprint change.
- Manifest `sha256` + `size` are refreshed by the generator; `validate_assets.py` PASS.

Replaced in place (reversible via git, no file renames, no partial families):
`grass_0..7`, `cobble_0..5`, `flagstone_0..7`, `verge.png`, `oak.png` — 24 files.
Not touched: soil, gravel, wood, all other props, `SanctuaryMaterials` procedural fallback.

## 3. Integration

- `SanctuaryArtAssets.tiles("grass", 8)` / `("cobble", 6)` / `("flagstone", 8)` still resolve
  the same counts — no loader or `PixelTileAtlas` change was needed.
- `buildSanctuaryGrassBlocks()` recomposes the 4×4 blocks (256×184 px) from the **updated**
  tiles at load time, so overview zoom picks up the new art automatically;
  `docs/visual-evidence/p18/grass_blocks_after.png` is the recomposed block sheet and
  `grass_blocks_batching_parity.png` is the P16 per-tile vs block parity check re-run.
- Filtering stays `FilterQuality.None`; no shader, no blur, no bloom was added.

## 4. Evidence (labelled)

`docs/visual-evidence/p18/`

| file | type |
|---|---|
| `before_after_side_by_side.png` | **ART CONTACT SHEET** — 4× nearest, before vs after, same framing |
| `tiles_contact_1x_before/after.png` | **ART CONTACT SHEET** — 1× tile accuracy |
| `tiles_contact_4x_before/after.png` | **ART CONTACT SHEET** — 4× nearest |
| `tiles_mosaic_before/after.png` | **ART CONTACT SHEET** — 4×4 oblique mosaic at 0.36 / 0.88 / 1.48 (authoring preview, not engine output) |
| `tiles_stats_before/after.json` | colour counts + mean value per family |
| `grass_blocks_after.png`, `grass_blocks_batching_parity.png` | recomposed overview blocks / parity |
| `engine-render/ground_plaza_default.png` | **REAL ENGINE RENDER** — 0.88, plaza |
| `engine-render/ground_plaza_overview.png` | **REAL ENGINE RENDER** — 0.36, block path |
| `engine-render/ground_road_verge_close.png` | **REAL ENGINE RENDER** — 1.48, road/verge/lawn seam |
| `engine-render/ground_lawn_close.png` | **REAL ENGINE RENDER** — 1.48, lawn only |
| `engine-render/ground_oak_verge.png` | **REAL ENGINE RENDER** — 1.00, oak against the verge |

Engine renders were produced by render run
[41424137414](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41424137414)
(APK build [41424137317](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41424137317)),
both green on commit `5701574`.

They come from `MapForgeGroundRenderTest` (`@GraphicsMode(NATIVE)`, Robolectric),
which calls `drawSanctuaryFloatingWorld`, `drawGroundShadows` and the depth-sorted `drawProp`
path — the same functions the game draws with. Software rasterised at 1× density.

## 5. Self-critique / open items

- Cobble still reads slightly **course-like** (regular horizontal joints). It is a large step
  from the baseline's identical rounded rectangles, but a later pass should break the joints
  into shorter runs.
- Soil and gravel were left at the old standard, so the training area next to the new road
  now looks visibly flatter. Next bounded slice should take them.
- Grass clumps are intentionally lower-contrast than round 1; at 1.48 they are readable but
  still a touch "spotty" on very large open lawn.
- No device FPS / thermal data: **NOT TESTED**.
- RAM estimate: 8 grass + 6 cobble + 8 flagstone + 6 soil + 4 gravel + verge = 33 tiles at
  64×64×4 B ≈ 0.54 MB decoded, plus 1 oak (112×136×4 ≈ 61 KB). The 4×4 grass blocks add
  27 blocks × 256×184×4 B ≈ **5.1 MB** decoded — unchanged from P16, because block count and
  block size did not change; only pixel content did. PNG disk size is not RAM.
