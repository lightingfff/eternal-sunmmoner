# P22 — Vegetation Forge Technical Specification (`sanctuary_p03`)

## 1. Scope & Selected Source Skills

P22 upgrades Eternal Summoner's active 2.5D pixel trees and foliage shrubs to match the P18/P19 ground tiles (`grass_0..7.png`, flagstone, cobble) and P20/P21 `premium_slice/` architecture while keeping gameplay, colliders, doorways, world anchors, Y=0.72 oblique projection, hero canopy occlusion, and directional cast shadows strictly intact.

**Repository source skills selected and adapted (offline pixel-art guidance only):**
1. `docs/skill-library/scenario/upstream/skills/scenario-game-assets/SKILL.md` — adapted for distinct botanical silhouette grammar, upper-left directional lighting hierarchy, transparent-margin discipline, and world-sheet cohesion across species.
2. `docs/skill-library/scenario/upstream/skills/scenario-textures/SKILL.md` — adapted for controlled dark-fantasy night luminance/value ranges (`lum 19–161` vs `grass_0.png` `lum 37–108` and P20/P21 roofs `lum 22–73`) and non-noisy leaf cluster rhythm.
3. `.agents/skills/ai-game-spritesheets/` — adapted for binary alpha (`alpha in {0, 255}`), zero semi-transparent fringe halos, locked pixel-row ground pivots (`alpha >= 128`), and `FilterQuality.None` nearest-neighbor rendering.

> **Important constraint note:** These skills are used strictly as deterministic offline art/engineering specifications. No paid Scenario API, Scenario MCP server, neural LoRA training, or external third-party image assets were used. All P22 PNGs are generated deterministically offline by `tools/mapforge_p22_vegetation.py` (Python + Pillow).

---

## 2. Runtime Species Split (Sanctuary vs. Tower Floors 1–2)

A pre-change audit of `TowerFloorCatalog.buildSanctuaryMap()` and `TowerFloorCatalog.FLOORS` established the exact runtime distribution (full audit in `docs/art/P22_VEGETATION_INVENTORY.md`):

- **Sanctuary (`buildSanctuaryMap()`, 3200×2400):**
  - `PropKind.SAKURA_BLOSSOM_TREE` (18 instances `sanctuary_sakura_0..17`): 9 even `(id.hashCode() and 1) == 0` -> `sanctuary_p03/canopy_a.png`; 9 odd -> `sanctuary_p03/canopy_b.png`.
  - `PropKind.GARDEN_HEDGE_BUSH` (5 instances `sanctuary_hedge_0..4` in Service Quarter parterre): upgraded via `SanctuaryArtAssets.assetSafe("sanctuary_p03/hedge_bush.png") ?: buildGardenHedgeBush()`.
  - `PropKind.CYPRESS_CONIFER_TREE` (3 instances `sanctuary_cypress_0..2`): **locked P20 control** (`sanctuary_p03/cypress.png`, untouched).
  - `PropKind.FLOWER_PLANTER_BED` (7 Canvas-drawn curb beds): **untouched**.
  - `PINE_SPIRIT_TREE`, `MYSTIC_PLUM_TREE`, `OAK_TREE`, and `FOREST_CANOPY_BUSH` have **0** instances in Sanctuary.
- **Tower Floor 1 (`FIRST_FIELD_MEADOW`) & Floor 2 (`ELDERWOOD_FOREST`):**
  - `PropKind.PINE_SPIRIT_TREE` (`sanctuary_p03/pine.png`): Floor 1 (`f1_spawn_grove_1`, `f1_spawn_grove_5`, procedural perimeter) & Floor 2.
  - `PropKind.MYSTIC_PLUM_TREE` (`sanctuary_p03/twisted.png`): Floor 2 (`ELDERWOOD_FOREST` procedural perimeter).
  - `PropKind.FOREST_CANOPY_BUSH`: Floor 1 & Floor 2 procedural perimeter, upgraded via `SanctuaryArtAssets.assetSafe("sanctuary_p03/forest_bush.png") ?: buildForestCanopyBush()`.
  - `PropKind.OAK_TREE`: **locked P18 control** (`sanctuary_p03/oak.png`, untouched).

---

## 3. Sprite Geometry, Anchor, Shadow & Canopy Occlusion Contract

`ObliqueSpriteGeometry.drawFootOffset(kind, sprite)` computes:

```kotlin
val bottomPadding = (sprite.height - 1 - bounds.bottom).coerceAtLeast(0)
bottomPadding * pixelScale(kind)
```

where `bounds` scans pixels with `(pixel ushr 24) >= 128`. Preserving `bounds.bottom` identically to the pre-P22 PNGs guarantees **0.000 px world-anchor drift** without touching `ObliqueSpriteGeometry.kt`, `SanctuaryEnvironmentShadows.kt`, or `PixelSpriteRenderer.kt`.

| Asset ID (`sanctuary_p03/`) | `PropKind` | Size (`W×H`) | `alpha>=128` `x_range` (`span`) | `alpha>=128` `y_range` (`bottom`, `pad`) | `pixelScale` | `drawFootOffset` | Canopy `topFraction` (split row) | Colors (`alpha=255`) | Shadow Rects (Before -> After) | SHA-256 |
|---|---|---|---|---|---|---|---|---|---|---|
| `canopy_a.png` | `SAKURA_BLOSSOM_TREE` (even) | `114×132` | `[4, 106]` (`103`) | `[6, 126]` (`bottom=126`, `pad=5`) | `2.0f` | `10.0f` | `0.56f` (`row 73`) | `18` | `174 -> 86` (**-50.6%**) | `422d5811ee81e809046885c10fc6e6c449204c5fb5614e0a55903106282e2cee` |
| `canopy_b.png` | `SAKURA_BLOSSOM_TREE` (odd) | `96×114` | `[7, 90]` (`84`) | `[6, 109]` (`bottom=109`, `pad=4`) | `2.0f` | `8.0f` | `0.56f` (`row 63`) | `16` | `147 -> 70` (**-52.4%**) | `6157716c6e15eeec00cbe639f613552bb642e7b53bbacb25b291e5af41fbe8a3` |
| `pine.png` | `PINE_SPIRIT_TREE` | `96×128` | `[16, 82]` (`67`) | `[2, 122]` (`bottom=122`, `pad=5`) | `2.0f` | `10.0f` | `0.52f` (`row 66`) | `15` | `134 -> 75` (**-44.0%**) | `34f749cbf703625fee8cd534a7b0f56b7e6b687b5700b408ea1b193180d75a1b` |
| `twisted.png` | `MYSTIC_PLUM_TREE` | `112×132` | `[4, 107]` (`104`) | `[6, 126]` (`bottom=126`, `pad=5`) | `2.05f` | `10.25f` | `0.54f` (`row 71`) | `14` | `198 -> 101` (**-49.0%**) | `647c7fea5fd15a44bd2d87cb5e9eb15d707c6c60d7f18ff809916d368a440a4c` |
| `hedge_bush.png` | `GARDEN_HEDGE_BUSH` | `64×40` | `[4, 59]` (`56`) | `[7, 36]` (`bottom=36`, `pad=3`) | `1.9f` | `5.7f` (`pad=3`) | — (`occludesUpperCanopy=false`) | `8` | `17` | `5bbc70adbbfae819c0d0c00081adebb2e3d641ecb245147f902e75e7a1cfc9f8` |
| `forest_bush.png` | `FOREST_CANOPY_BUSH` | `64×40` | `[4, 59]` (`56`) | `[7, 36]` (`bottom=36`, `pad=3`) | `2.0f` | `6.0f` (`pad=3`) | — (`occludesUpperCanopy=false`) | `10` | `19` | `11e573a5f85ac1fae58a20a15f799013211bdf3c733fdbcd536edeab365e6fb5` |
| `oak.png` *(locked P18 control)* | `OAK_TREE` | `112×136` | `[11, 100]` (`90`) | `[11, 134]` (`bottom=134`, `pad=1`) | `2.15f` | `2.15f` | `0.52f` | `9` | unchanged | `556eefd5c1b1f14872b67e6e651b8c7d6645e4632519d0719a6ce614b240a229` |
| `cypress.png` *(locked P20 control)* | `CYPRESS_CONIFER_TREE` | `48×88` | `[7, 41]` (`35`) | `[5, 87]` (`bottom=87`, `pad=0`) | `2.0f` | `0.0f` | `0.62f` | `16` | unchanged | `442cbc617c6d61f79f2105f7c4b2072a2fc721fa3a4e56ee11d695d576cf988b` |

### Why Each Species Reads Distinctly at `z=0.36`, `z=0.88`, `z=1.00`, and `z=1.48`
- **`canopy_a` (Broad Ancient Sakura, `114×132`):** Wide umbrella crown (`x_span=103`), sturdy central trunk splitting into three primary limbs with two open sky cavities, buttressed root collar with fallen 2px blossom clusters, scalloped shingled petal clusters (`#c28496` moonlit crest -> `#190d1b` deep plum night shadow), crown mass concentrated above row `73` (`0.56f`).
- **`canopy_b` (Asymmetric Wind-Leaning Sakura, `96×114`):** S-leaning slender trunk with a low left side-bough and taller right blossom dome (`IoU = 0.7627` vs `canopy_a`), breaking visual repetition across the 18 paired Sanctuary sakura props.
- **`pine` (Layered Spirit Spruce, `96×128`):** Narrow spire apex widening across 7 drooped bough tiers down to `y=90` (`IoU = 0.4249` vs `canopy_a`, `0.3781` vs `twisted`), serrated hanging needle tips, dark inter-tier occlusion pockets, warm moonlit cedar bark (`#74503d` -> `#18100f`) with buttressed roots.
- **`twisted` (Gnarled Elderwood Mystic Plum, `112×132`):** Dramatic S-curved trunk sweeping left then right into wide horizontal bonsai-like violet-indigo foliage shelves (`x_span=104 >= 100`), open branch windows, and cool moonlit elderwood bark ridges.
- **`hedge_bush` (`64×40`):** Trimmed 2.5D boxwood hedge block with distinct top plane, oblique front shadow face, notched leaf edges, grounded turf contact base, and 2px dusty-sakura fallen petal accents (`#b4788c`).
- **`forest_bush` (`64×40`):** Irregular wild bramble/undergrowth mound with overlapping leaf sprays, visible dark woody twigs, and 2px elderberry accents (`#7889ad`).

---

## 4. Runtime Wiring & Fallback Safety

Only two lines in `app/src/main/java/com/example/game/pixel/PixelStructureAtlas.kt` were updated:

```kotlin
val gardenHedgeBushSprite: ImageBitmap by lazy {
  SanctuaryArtAssets.assetSafe("sanctuary_p03/hedge_bush.png")
    ?: SanctuaryVegetation.buildGardenHedgeBush()
}

val forestBushSprite: ImageBitmap by lazy {
  SanctuaryArtAssets.assetSafe("sanctuary_p03/forest_bush.png")
    ?: SanctuaryVegetation.buildForestCanopyBush()
}
```

- All four tree properties (`sakuraTreeSprite`, `sakuraTreeVariantSprite`, `pineSpiritTreeSprite`, `mysticPlumTreeSprite`) already loaded `sanctuary_p03/{canopy_a,canopy_b,pine,twisted}.png` via `SanctuaryArtAssets.asset(...)` and required zero Kotlin changes.
- In unit tests that do not call `SanctuaryArtAssets.init(context)`, `assetSafe(...)` returns `null` and cleanly falls back to `SanctuaryVegetation.buildGardenHedgeBush()` / `buildForestCanopyBush()` (verified in `MapForgeP22ContractTest.assetSafeFallbackWorksBeforeAssetInit`).

---

## 5. Runtime Performance & Memory Impact

- **New render passes / shaders / blur / post-processing:** `0`
- **Per-frame allocations added:** `0` (`ImageBitmap`, `ObliqueSpriteGeometry.bounds`, and `SanctuaryEnvironmentShadows.CasterGeometry` remain cached once via `IdentityHashMap` / `by lazy`).
- **Decoded `ARGB_8888` bitmap memory delta:**
  - Replaced tree PNGs (`canopy_a`, `canopy_b`, `pine`, `twisted`): **0 bytes delta** (identical `W×H` dimensions).
  - New shrub PNGs (`hedge_bush` `64×40` + `forest_bush` `64×40`): `2 × 64 × 40 × 4 = 20,480 bytes` (**+20.0 KB** total when `SanctuaryArtAssets` is initialized, while skipping lazy procedural `IntArray` generation for those two sprites).
- **Shadow GPU `Path` complexity improvement:** Cohesive pixel-cluster silhouettes eliminated the noisy 1px interior holes of the legacy generator, reducing `SanctuaryEnvironmentShadows` horizontal scanline `Path.addRect` segments by **44.0% to 52.4%** across all four tree sprites (`canopy_a: 174 -> 86`, `canopy_b: 147 -> 70`, `pine: 134 -> 75`, `twisted: 198 -> 101`).
- **Physical Android device FPS / jank / thermal profiling:** `NOT TESTED` (no physical Android phone attached in CI/sandbox; unit-level `ScenePerformanceTest` and Robolectric Skia render suites verified in GitHub Actions).

---

## 6. Reproducible Regeneration & Validation Commands

```bash
# 1. Regenerate all 6 P22 PNGs + update sanctuary_p03/manifest.json + build 1x/3x ART CONTACT SHEETS
.venv/bin/python tools/mapforge_p22_vegetation.py --phase all

# 2. Run offline P22 contract validator (controls, bounds, binary alpha, palette, shadow rects, determinism, IoU)
.venv/bin/python tools/validate_mapforge_p22.py

# 3. Run repository asset & Lyra pack validators
python3 tools/validate_assets.py
.venv/bin/python tools/validate_lyra_pack.py

# 4. Run Robolectric native-Skia contract, render evidence, and Sanctuary regressions (executed in GitHub Actions)
gradle :app:assembleDebug
gradle :app:testDebugUnitTest --tests "com.example.MapForgeP22RenderTest" --stacktrace
gradle :app:testDebugUnitTest --tests "com.example.MapForgeP22ContractTest" --stacktrace
gradle :app:testDebugUnitTest \
  --tests "com.example.SanctuaryMaterialTest.manifestAssetsDecodeAndMatchRuntimeAtlas" \
  --tests "com.example.SanctuaryEnvironmentArtTest" \
  --tests "com.example.ui.LayoutResponsivenessRobolectricTest" \
  --tests "com.example.performance.ScenePerformanceTest" \
  --tests "com.example.LivingWorldTest" \
  --stacktrace
```
