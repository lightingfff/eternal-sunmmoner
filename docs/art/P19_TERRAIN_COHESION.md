# P19 — Terrain cohesion: soil + gravel families, cobble joints, grass density

Scope: art only. Sanctuary layout, path widths, obstacle anchors, entrances, collision,
camera (`ObliqueCamera.OBLIQUE_Y_SCALE = 0.72`), prop positions and gameplay logic are
untouched. P16 batching (4×4 grass blocks, conservative road culling) and nearest sampling
are unchanged.

## 1. Diagnosis (real engine renders, fixed cameras)

Cameras are fixed in `MapForgeTerrainRenderTest` (0.36 / 0.88 / 1.00 / 1.48), so
`engine-render-before/` (main) and `engine-render/` (P19) are directly comparable.

| # | Defect seen in the real renderer | Evidence |
|---|---|---|
| 1 | Training arena floor is a flat tan field plus scattered 1 px light specks; visibly flatter/more procedural than the P18 lawn and road | `terrain_training_default.png`, `terrain_training_close.png` |
| 2 | The gravel band between lawn and sand reads as speckle confetti, not stones, so the material change lawn→gravel→sand has no vocabulary | `terrain_arena_rim.png` |
| 3 | Cobble courses form continuous full-width grout bands; at 1.48 the road reads as a brick wall | `terrain_road_joints_close.png`, `tiles_seams_5x5_before.png` |
| 4 | Grass clumps are uniform in size and the brightest crown pixel fires on the smallest clumps → "dotty" at 1.48, crowded at 0.36 | `terrain_lawn_close.png` |

Measured starting point (`tiles_stats_before.json`): soil 8 unique colours/variant, gravel 6,
grass 8–10, cobble 9–10, flagstone 9. Eight colours cannot carry base → shade → highlight →
mortar → grit, which is why the earth read as one flat value with stamps on top.

## 2. What changed

### `soil_0..5` (rewritten, 64×64 RGBA, seamless)
- Three value scales: small organic earth masses (6–14 px, one value step), compaction
  smears (short broken runs, never a full-width band), and 3–5 pebble clusters bedded in
  disturbed ground.
- Pebbles: contact shadow below-right, body, single upper-left rim pixel; cool grey values
  so the arena shares the cobble vocabulary.
- Occasional 1 px dry cracks with a shade line beside them, plus low-contrast grit runs.
- 8 → 11 unique colours/variant.
- **Tone equalised across variants** (see §3) so hash-picked neighbours do not quilt.

### `gravel_0..3` (rewritten)
- 6–8 clusters of 2–5 pebbles each on a rough dirt base, moss only in the two damp variants.
- Density deliberately restrained so tiny hero feet are not swallowed at 0.88.
- 6 → 9–11 unique colours/variant; stone values compatible with cobble but less geometric.

### `cobble_0..5` (joint polish only)
- Added 3–4 taller **break stones per tile that straddle a course boundary**, cutting the
  full-width grout bands into short uneven runs.
- Grout narrowed to 1–2 px and lifted from `#181f24` to `#1e262b` (less "leopard" contrast).
- Stone count, road mask, road width and culling are untouched.

### `grass_0..7` (density only)
- 10–13 clumps instead of 12–16, scale distribution shifted to mid, brightest crown pixel
  only on scale > 0, three focal clumps instead of two. Block recomposition is unchanged.

### Generation
`tools/mapforge_ground.py` (extended, deterministic, offline). New helpers: `pebble()`,
`pebble_cluster()`, `mean_luma()`, `equalize_tone()`. Manifest `sha256`/`size` refreshed;
`tools/validate_assets.py` PASS.

## 3. Two defects introduced and fixed during this pass

1. **Shared 2 px border frame.** The P18 family-adjacency copy pasted variant 0's border into
   every tile. On low-frequency earth materials that border does not match the interior and
   painted a visible rectangle around each soil/gravel tile. Earth families now opt out
   (`shared = False`); structured materials (grass/cobble/flagstone) keep it.
2. **Hash-variant checkerboard.** First attempt gave some variants a darker base and a
   full-width compaction band. Because the renderer picks variants by hash, the arena became
   a quilt of 56 px squares at 0.88. Fixed by (a) shrinking all earth features below the tile
   scale, (b) removing variant-level value shifts, (c) `equalize_tone()` normalising each
   variant's mean luma to variant 0 (max shift ±12, hue untouched — only a per-channel
   offset). Measured after: soil 74.8–75.3, gravel 69.1–69.3 mean luma.

## 4. Evidence

`docs/visual-evidence/p19/`

| file | label |
|---|---|
| `engine_before_after.png` | **REAL ENGINE RENDER** side-by-side (training arena 0.88, cobble 1.48) |
| `engine-render/*.png` | **REAL ENGINE RENDER** — P19 assets, 8 fixed cameras |
| `engine-render-before/*.png` | **REAL ENGINE RENDER** — main (pre-P19) assets, same 8 cameras |
| `before_after_side_by_side.png` | **ART CONTACT SHEET** — 4× nearest |
| `tiles_contact_1x_*`, `tiles_contact_4x_*` | **ART CONTACT SHEET** |
| `tiles_mosaic_*` | **ART CONTACT SHEET** — 4×4 oblique mosaic at 0.36/0.88/1.48 |
| `tiles_seams_5x5_*` | **SEAM CHECK** — every variant tiled 5×5 at 1× |
| `tiles_stats_*_json` | unique colours per variant |

## 5. Performance / memory

- Asset counts unchanged: grass 8, cobble 6, flagstone 8, soil 6, gravel 4, verge, oak.
  No new files, no new families, no manifest contract change.
- Decoded RAM unchanged: 34 assets at 64×64×4 B ≈ 0.55 MB, plus oak (112×136×4 ≈ 61 KB) and
  the 27 grass blocks (256×184×4 ≈ 5.1 MB) — all identical to P18/P16.
- Draw calls unchanged: same tiles, same 4×4 block path, same road culling. No runtime PNG
  generation, no per-tile allocations in the draw loop, no extra blending passes, no shader.
- PNG disk size is not RAM; the numbers above are decoded estimates, **not** device
  measurements.
- Recommended on-device acceptance test (NOT performed here): fixed pan/zoom at
  0.36/0.88/1.48 for 5 minutes, log frame pacing, memory and thermal against the previous
  build; stop on throttling.

## 6. Remaining weaknesses

- The gravel band is thin at island scale, so its new material detail is only readable at
  1.48; a wider band would need a layout change (out of scope).
- The arena lip is a flat renderer-colour band (`0xFF4A3D2E`) that now contrasts with the
  textured soil underneath; tuning it is renderer work, not asset work.
- Pebble clusters are still slightly "creature-like" at 1.48 on very flat ground; a later
  pass could add two-cluster overlap so silhouettes read as rubble piles.
- Soil/gravel are now darker than the old "golden sand". This follows the brief (dark packed
  earth, contrast below actors) but it is a mood change reviewers should confirm on device.
