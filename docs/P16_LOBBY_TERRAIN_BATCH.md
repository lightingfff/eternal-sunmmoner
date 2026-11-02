# P16 — Sanctuary/Lobby terrain draw-call batching

Scope: rendering only. No gameplay, save, AI, layout, collision, camera, Gradle or CI change.
Every world coordinate, obstacle, path, pivot and piece of art is untouched.

## 1. Objective

Reduce the Lobby/Sanctuary frame cost that makes the phone lag and heat up, without
downgrading the art. The previous phases (P0.5, P1.5A) already fixed per-key sprite
loading, depth-merge allocation, shadow caching and full-screen blur. What was left is the
largest remaining cost: how many `drawImage` commands the ground is composed from.

## 2. Files inspected

| File | What was read |
| --- | --- |
| `ui/screens/SanctuaryLobbyScreen.kt` | `SceneFrameLoop`, the Sanctuary `Canvas`, depth queue, plaque/name-pill text pass |
| `game/lobby/SanctuaryLobbyEngine.kt` | `step()` (fixed 60 Hz simulation), colliding hero movement, `heroActors` cache |
| `game/pixel/PixelSpriteRenderer.kt` | `drawWorldBackground`, `drawProp`, `drawCharacterSprite`, canopy occlusion |
| `game/pixel/SanctuaryWorldRenderer.kt` | `drawSanctuaryFloatingWorld`, grass loop, sand arena, road network, material bands |
| `game/pixel/PixelTileAtlas.kt` | authored 64×64 tile families, asset-backed tiles |
| `game/pixel/SanctuaryEnvironmentShadows.kt` | cached silhouette paths, per-obstacle shadow pass |
| `game/pixel/DepthPresentation.kt` | cached light stamps, distant-island focus |
| `game/pixel/LivingWorldRenderer.kt` | weather, local lights, world grade |
| `game/map/WorldMapModels.kt` | `ObliqueCamera` projection (ground Y 0.72) and collision |
| `game/tower/TowerFloorCatalog.kt` | sanctuary map (3200×2760, 8 road segments, 24 obstacles) |
| `performance/ScenePerformance.kt` | frame pacer, fixed simulation, snapshot overlay |

## 3. Bottlenecks found

The Sanctuary ground was composited per tile, and three separate passes iterated the whole
visible tile rectangle:

1. **Grass base** — `for (ty …) for (tx …) drawImage(tile)`, up to **1575** commands per
   frame at overview zoom.
2. **Cobblestone road** — every tile of a 34×32 rectangle was submitted and then discarded
   by `clipPath(cobbleInterior)`. The clip does not remove the command cost: each clipped
   `drawImage` still costs a Skia command plus a path-clip test. Up to **1088** commands.
3. **Road verge ring** — `drawMaterialBand(..., startTx..endTx, startTy..endTy)` drew the
   *entire visible rectangle* of verge tiles inside `clipPath(cobbleInterior)` minus
   `clipPath(buildRoadUnionPath(-12f))`, so almost every one of those **1575** commands was
   thrown away by the clips.

`tools/analyze_lobby_drawcalls.py` reproduces the exact Kotlin range math and prints the
command counts (it is a static model, not a device profile):

```
case                         zoom  grass  road  verge  total  after  saved      %
overview fit (island wide)   0.27   1575  1056   1575   4206    542  3664  87.1%
zoomed out                   0.36   1575  1056   1575   4206    542  3664  87.1%
mid zoom                     0.50   1260  1024   1260   3544    509  3035  85.6%
default gameplay zoom        0.88    770   704    770   2244   1090  1154  51.4%
close inspection             1.20    630   576    630   1836    917   919  50.1%
720x1600 default zoom        0.88    560   512    560   1632    831   801  49.1%
```

At the default 30 FPS `BALANCED` mode that is **~67 000 `drawImage` submissions per second**
at gameplay zoom and **~126 000 per second** in the island overview, on a 1080×2400 screen.
Frame pacing was already correct (`FixedSceneSimulation` at 60 Hz, pacer at 30/60 FPS), so
the frame time was dominated by command submission and clipped overdraw, not by simulation.
That matches the reported symptom: smooth-ish but hot and janky, worst when the camera is
pulled back to show the whole island.

Secondary findings, deliberately **not** changed in this cycle:

- `drawCurbedCobblestoneRoadNetwork` rebuilds three road `Path` objects in screen space on
  every frame (curve caps included). Correct, just not free; it should be cached in world
  space and transformed, which changes more geometry code than this cycle allows.
- The plaque/name-pill pass calls `textMeasurer.measure` 9–11 times per frame and allocates
  a `MutableList<Rect>` plus a `Rect` per tag.
- `CollisionPhysics.resolvePosition` returns a `Pair` per moving hero per 60 Hz step.

## 4. Changes

| File | Change |
| --- | --- |
| `game/pixel/SanctuaryRoadCoverage.kt` (new) | Per-tile signed clearance to the road network, computed once per `paths` list and cached by identity. |
| `game/pixel/PixelTileAtlas.kt` | `sanctuaryGrassBlocks`: eight pre-composed 256×184 grass blocks (4×4 authored 64×64 tiles baked onto the same 0.72 foreshortened ground plane), built lazily from the shipped PNGs. |
| `game/pixel/SanctuaryWorldRenderer.kt` | Grass base uses one block per sixteen tiles **only when a tile is under 25 px tall on screen**; cobble loop and verge band skip tiles whose whole square is provably outside the road. |
| `tools/analyze_lobby_drawcalls.py` (new) | Reproducible draw-call model used for the table above. |
| `tools/preview_grass_blocks.py` (new) | Rebuilds the blocks in Python from the shipped PNGs and renders a before/after comparison. |

### Why this cannot change what the player sees

- **Cobble / verge skip** — a tile is skipped only when its centre is farther from every
  road segment than `segment width / 2 + half tile diagonal` (32 + 45.25 world px). Every
  pixel of such a tile is outside the road, so the clip paths were already discarding them.
  Tiles that can contain a single cobble pixel are always kept. The verge band uses an extra
  7.5 world px margin, which is the widest curb union it can reach.
- **Grass blocks** — only active below 25 px of screen tile height. At the 0.88 default
  camera a tile is 40 px tall, and at inspection zoom 55 px, so the sharp per-tile path is
  what the player sees in normal play. At overview the block is drawn with
  `FilterQuality.None` from the same authored tiles: same palette, same tuft density, same
  tone (mean channel delta 5.7/255 against the per-tile composition; see evidence).
- No texture, sprite, colour, obstacle, path, pivot, camera or depth-order value changed.

## 5. Evidence

- `docs/visual-evidence/p16/grass_blocks_sheet.png` — the eight composed blocks, nearest-neighbour.
- `docs/visual-evidence/p16/grass_blocks_before_after.png` — 24×18 tile patch at zoom 0.45,
  per-tile composition vs block composition: 432 draws → 42 draws, mean channel delta
  5.65/255, max 93 (one channel, at pattern edges). The patch is visually equivalent; it is
  not bit-identical because the block variant is chosen per block, so tuft positions differ.
- `python3 tools/analyze_lobby_drawcalls.py` — the command-count table above.
- `python3 tools/validate_assets.py` — PASS, package id `com.aistudio.eternalsummoner.kvrpzm`.

Existing Robolectric captures that render the world above 25 px of tile height
(`SanctuaryEnvironmentArtTest` at 0.65/0.8/1/1.6, `LivingWorldTest` at 0.75,
`DepthAddendumTest` at 0.65/1/1.8) must come out **pixel-identical**, so a passing diff
there is the real regression gate. `PremiumSliceEvidenceTest` and `DepthAddendumTest` also
capture at 0.45, which now uses the block path; those two `overview`/`.45` images are
expected to differ slightly (pattern phase only, same palette and density) and should be
reviewed by eye, not diffed byte-for-byte.

## 6. Not verified — read this before trusting the numbers

**The Android build and the Robolectric visual suite were not run in this session.** The
sandbox has no JDK, no Android SDK and no `adb`; Maven/Gradle repositories are not reachable
from it either. The edits were written against the existing code and checked by inspection
(brace/paren balance, call sites, scopes), but the first real gate is:

```
gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

and then, on the phone:

```
adb shell dumpsys gfxinfo com.aistudio.eternalsummoner.kvrpzm reset
# 60 s of the same pan/zoom route at noon/clear, then:
adb shell dumpsys gfxinfo com.aistudio.eternalsummoner.kvrpzm
adb shell dumpsys meminfo com.aistudio.eternalsummoner.kvrpzm
```

Expected on-device signal: total frame time and GPU command count drop most in the
island-overview camera, least at close inspection, with no change in colour or composition
in the captured screenshots.

## 7. Next step

1. Build + run the Robolectric visual evidence suite in CI or locally; diff the P1.5A
   screenshots against the new ones. Only then report a measured improvement.
2. If that passes, cache the road curb paths in world space (removes three `Path`
   allocations per frame) and memoise the plaque/name-pill text layouts.
3. Then, and only then, spend the cycle on the premium vertical slice (hero / starter
   monster / Sanctuary sample area / battle sample) — the art pipeline in
   `tools/generate_premium_slice.py` + `app/src/main/assets/premium_slice/manifest.json`
   is already the right place for it.
