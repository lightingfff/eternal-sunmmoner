# P23 Integration Audit — Sanctuary Architecture 2.5D & Environmental Props Cohesion

**Branch:** `arena/c67c3cc2-eternal-sunmmoner` (audited at `main` commit `b40563e7d0df5b03ac489db75873cd77f39736a2`, post-P22 merge)  
**Selected Repo Skills (Offline Guidance Only — No Neural Training / LoRA / Paid Scenario MCP):**
- `docs/skill-library/scenario/upstream/skills/scenario-game-assets/SKILL.md` — 2.5D architectural massing, silhouette hierarchy, ground plane discipline, readable focal landmarks.
- `docs/skill-library/scenario/upstream/skills/scenario-textures/SKILL.md` — coursed stone/slate/shingle/timber material ramps, upper-left moonlight rim vs recessed mortar, value discipline against P18–P22 terrain/vegetation.
- `docs/skill-library/scenario/upstream/skills/scenario-refine-loop/SKILL.md` — diagnose -> repair -> real-engine render verification loop across zooms `0.36, 0.88, 1.00, 1.48`.
- `.agents/skills/ai-game-spritesheets/SKILL.md` — static post/base pixel-lock across animated `STREET_LANTERN` frames and binary alpha hygiene.

---

## 1. Exact Sprite-to-World Projection & Foot-Offset Contract

Verified from `ObliqueCamera.kt`, `ObliqueSpriteGeometry.kt`, `PixelSpriteRenderer.kt`, and `SanctuaryEnvironmentShadows.kt`:

- **Camera projection (`ObliqueCamera`):**
  - `OBLIQUE_Y_SCALE = 0.72f` (locked; vertical sprite pixels rise 1:1 on screen without ground squash).
  - `screenX(wx) = viewportW / 2 + (wx - camX) * zoom`
  - `screenY(wy, zHeight) = viewportH / 2 + (wy - camY) * zoom * 0.72f - zHeight * zoom`
- **Bottom-centred crisp sprite blit (`drawCrispSprite`):**
  - Drawn with `FilterQuality.None` (`Paint.isFilterBitmap = false`) bottom-centred at `(cx, cy + drawFootOffset * s)` with `pixelScale * s`, where `s = zoom * obs.scale`.
  - Facilities (`FACILITY_*`), townhouses (`MEDIEVAL_TOWNHOUSE_*`), cottages (`SANCTUARY_COTTAGE_*`), corner towers (`FORTRESS_CORNER_TOWER`), and ruin arches (`ANCIENT_RUIN_ARCH`) use `drawFootOffset = 6f`, `pixelScale = 2.0f`.
  - `SOUTHERN_GATE_TOWER` uses `drawFootOffset = 8f`, `pixelScale = 2.0f`.
  - Small/mid props (`STREET_LANTERN`, `WOODEN_BENCH_34`, `FLOWER_PLANTER_BED`, `STONE_RETAINING_WALL`, `OBSIDIAN_COLUMN`/`RUINED_PILLAR`, `ANCIENT_MONOLITH`, `TRAINING_STRIKING_POST`) use `drawFootOffset = 4f`, `pixelScale = 2.0f`.
- **Exact Sprite <-> World Mapping (for `drawFootOffset = 6f`, `pixelScale = 2f`):**
  - Sprite row `H - 3` (`bottomPadding = 3`, with rows `H - 2..H - 1` carrying threshold step / contact rim) lands directly on `obs.y`:
    ```text
    worldX  = obs.x + (spriteX - W / 2f) * 2f * obs.scale
    worldY  = obs.y - ((H - 3f) - spriteY) * (2f * obs.scale) / 0.72f

    spriteX = W / 2f + (worldX - obs.x) / (2f * obs.scale)
    spriteY = (H - 3f) - (obs.y - worldY) * 0.72f / (2f * obs.scale)
    ```
- **Shadow Origin Contract (`SanctuaryEnvironmentShadows.geometry`):**
  - `foot = (drawFootOffset - bounds.bottomPadding * pixelScale) * obs.scale`
  - Any sprite whose opaque silhouette ends high above the expected foot row (`bottomPadding >> drawFootOffset / pixelScale`, e.g., procedural cottages with `bottomPadding = 13` or `ANCIENT_RUIN_ARCH` with `bottomPadding = 10`) floats both its visible base and its cast-shadow contact origin north of `obs.y`.

---

## 2. Full Sanctuary & Tower Architectural + Prop Inventory (`HEAD` `b40563e`)

### 2.1 Authored PNG Facilities (`app/src/main/assets/premium_slice/`)

| Obstacle ID | `PropKind` | World `(x, y)` | Scale | Footprint `(halfW, depthN)` | Canvas `WxH` | Trigger / Zone `(wx, wy)` | Expected Sprite Target `(x, y)` | Vertical Alpha Gap Cols | Root-Cause Defects Found at `HEAD` |
|---|---|---:|---:|---:|---:|---|---|---:|---|
| `building_armory` | `FACILITY_ARMORY` | `(995, 655)` | `0.80` | `(136, 98)` | `190x166` | **Enterable** trigger `(993, 655)`, spawn `(993, 673)` | `door_cx = 93.75` (`~94`), `sill = 163` | `13` | **CRITICAL:** Painted door is at `x=133..150, y=109..139` (`world ~(1069, 535)`), **76.8 world units east & 120 units north** of trigger `(993, 655)`; Lyra spawns at corner anvil away from the door. Split orange-brown + grey-slate roof clashes in hue/material; chimney cast-shadow polygon protrudes into empty sky (`x=63..65, y=22..28`); overhang alpha gaps (`x=15..19`, `177..180`); flat decal door frame on angled side wall. |
| `building_hero_quarters` | `FACILITY_HERO_QUARTERS` | `(825, 790)` | `0.80` | `(156, 108)` | `200x168` | **Enterable** trigger `(823, 790)`, spawn `(823, 816)` | `door_cx = 98.75` (`~99`), `sill = 165` | **`126`** | **126/200 columns have interior transparent holes:** 1px detached floating ridge wire (`y = ridge - 2` with `y = ridge - 1` transparent), horizontal 1–2px jetty slit (`y=129`), roof-to-wall gap on left eave (`x=5..11`), chimney smoke holes, flat featureless right return wall, shallow sticker door surround. |
| `building_restaurant` | `FACILITY_RESTAURANT` | `(1105, 925)` | `0.80` | `(152, 104)` | `206x172` | **Enterable** trigger `(1103, 925)`, spawn `(1103, 952)` | `door_cx = 101.75` (`~102`), `sill = 169` | **`125`** | **125/206 columns have interior transparent holes:** detached floating 1px ridge wire, horizontal jetty alpha slit (`y=127`), left chimney-to-wall gap (`x=16..17, y=27..54`), flat grey right return polygon, shallow box door frame without deep recessed archivolts/steps. |
| `building_hero_registry` | `FACILITY_HERO_REGISTRY` | `(785, 1185)` | `0.80` | `(142, 98)` | `188x166` | **Zone-only** proxy zone `(1640, 1270)` (no interior entry) | Non-enterable civic counter / loggia | **`103`** | **103/188 columns have interior transparent holes** (detached ridge wire, jetty slit `y=123`). Paints an identical residential plank front door at `(94, 163)` creating a **false-affordance broken-door read** despite having no interior map. Flat grey right return wall. |
| `building_synthesis_lab` | `FACILITY_SYNTHESIS_LAB` | `(1270, 555)` | `0.80` | `(136, 94)` | `188x168` | **Zone-only** `(1270, 585)` `r=150` (no interior entry) | Non-enterable alchemical workshop bay | **`118`** | **118/188 columns have interior transparent holes** (detached ridge wire, jetty slit `y=125`). Paints a residential plank door at `(94, 165)` (false interior affordance) + flat neon-teal window grids + flat grey right return wall. |
| `building_summon_hall` | `FACILITY_SUMMONING_HALL` | `(2430, 2005)` | `1.08` | `(198, 128)` | `268x206` | **Zone-only** `(2380, 1930)` `r=210` | Inlaid rune stage `(111, 176)` | **`199`** | **SEVERE STRUCTURAL FLOAT (`199/268` columns with alpha holes):** Entablature (`y=56..62`) floats above columns (`y=74..133`) with green grass visible straight through `y=63..73`; right return wall (`y=60..126`) fails to meet cornice (`y=44..50`); temple podium (`y=134..146`) floats over a **33px transparent grass gap** above the circular plaza (`y=179..205`); detached 1px ridge wire. |
| `monument_tower_gate` | `FACILITY_TOWER_GATE` | `(1960, 585)` | `0.96` | `(168, 110)` | `260x224` | **Zone-only** `(1960, 635)` `r=185` + dynamic rift oval | Dark aperture `(130, 120) +/- (16, 27)` | `42` | Detached vertical 1px stone wire floating in mid-air outside battered left tower (`x=4..5, y=49..134`); transparent grass gap (`x=14..23, 237..246, y=206..216`) between outer tower plinths and stepped dais; flat front elevation lacking machicolation corbels & oblique parapet depth. |

> **Verified Non-Existent Obstacle:** `PropKind.FACILITY_TRAINING_GROUND` has **0** `WorldObstacle` instances in `buildSanctuaryMap()` (the SW training yard at `(910, 1650)` is an open soil arena with dummies, racks, and striking post). Per P23 constraint, no new `FACILITY_TRAINING_GROUND` obstacle is added.

### 2.2 Procedural Sanctuary Houses & Perimeter Towers (`PixelStructureAtlas` / `PixelSanctuaryLandmarks`)

| Obstacle IDs | `PropKind` | Count | World Anchors | Canvas `WxH` | `botPad` | Root-Cause Defects Found at `HEAD` |
|---|---|---:|---|---:|---:|---|
| `nw_outer_slate_house` | `MEDIEVAL_TOWNHOUSE_B` (+ `_A` atlas pair) | `1` | `(655, 930)` `s=0.84` | `144x160` (`144x156`) | `13` | **Floats ~20 world units north of `obs.y`** (`botPad=13` vs expected `3`); pastel lavender cartoon door arch; neon `#FFEE55` checkerboard windows; orange chimney; tan `0xFF706B62` gable base strip. |
| `nw_outer_brown_cottage`, `inner_slate_wing_cottage` | `SANCTUARY_COTTAGE_BROWN`, `SANCTUARY_COTTAGE_SLATE` | `2` | `(495, 1075)` `s=0.86`, `(1395, 675)` `s=0.82` | `132x144` | `13` | Same `paintAnimeHalfTimberHouse` defects: floats 10px above `H-3`, pastel purple sticker door arch, bright yellow grid windows, orange brick chimney, coarse 2x2 roof fill. |
| `*_corner_tower` (8 around island) | `FORTRESS_CORNER_TOWER` | `8` | Perimeter octants `s=0.96` | `80x118` | `8` | **Inverted mortar contrast:** `mortar = 0xFF5C5446` is brighter than `stoneSun = 0xFF4A4E5E`, creating a glowing tan graph-paper grid over dark stone; base floats (`botY = h - 14`). |
| `south_gate_tower_west/east` | `SOUTHERN_GATE_TOWER` | `2` | `(1480, 2320)`, `(1760, 2320)` `s=1.02` | `96x136` | `8` | Same inverted bright-tan wireframe mortar (`0xFF5C5446`) and floating base (`botY = h - 14`) right at the player's southern Sanctuary arrival gate. |

### 2.3 Environmental Props Audited Across Sanctuary & Tower Floors 1–3

| `PropKind` | Instances | Current Render Path | Canvas / Draw | Root-Cause Defects Found at `HEAD` |
|---|---:|---|---|---|
| `FLOWER_PLANTER_BED` | `7` (Sanctuary) | `InteriorWorldRenderer.drawCustomProp34` (`Canvas` rects + circles), **missing** from `SanctuaryEnvironmentShadows` | Vector `~60x18` | Bright off-white box (`0xFFD6CFC2`) with 5 smooth pastel candy `drawCircle`s (pink, lemon yellow, baby blue, coral, lavender) — jarring vector placeholder next to P18/P19 cobblestones and P22 bushes; casts no world shadow. |
| `STREET_LANTERN` | `6` Sanctuary + `2` F1 + `4` Interiors | `PixelStructureAtlas.streetLanternFrames` (3 procedural frames) + oversized yellow glow | `32x66` x 3 (`botPad=4`) | Flat purple periscope post (`0xFF352D4D`) with thick black cartoon outline and flat yellow octagon; `glowRadius` changes outer silhouette between frames; base floats above `H-3`/`H-2`. |
| `WOODEN_BENCH_34` | `4` Sanctuary + `4` Interiors | `PixelStructureAtlas.woodenBenchSprite` (procedural) | `58x36` (`botPad=3`) | Flat front-elevation bright orange planks (`0xFF9C6641` / `0xFFC48B60`) with black cartoon outline, lacking true 2.5D 3/4 seat/leg depth and night oak palette. |
| `ANCIENT_RUIN_ARCH` | `6` F1 + F2 | `PixelStructureAtlas.ruinArchSprite` (procedural) | `96x86` (`botPad=10`) | Two flat purple brick rectangles floating **10px above** their ground shadow oval (`botY=76`) with a neon lime-green blob on top — severe mismatch next to P22 `oak.png` & `pine.png`. |
| `STONE_RETAINING_WALL` | Atlas / Ruin wall | `PixelStructureAtlas.stoneRetainingWallSprite` (procedural) | `88x48` (`botPad=6`) | Flat purple brick rectangle (`0xFF544B6E`) with bright mint-green blobs and floating base. |
| `ANCIENT_MONOLITH` | F1 procedural ruins | `PixelStructureAtlas.monolithSprite` (procedural) | `44x74` (`botPad=6`) | Flat indigo rectangle with a bright sky-blue `+` cross and floating base (visible in F1 renders). |
| `TRAINING_DUMMY`, `WEAPON_RACK`, `TRAINING_STRIKING_POST` | `10` + `4` + `1` (Sanctuary SW yard + Dojo) | `PixelSpriteRenderer` Canvas rects/circles (`DUMMY`, `RACK`) + procedural `36x86` (`POST`) | Vector / `36x86` | Bright daytime orange-tan vector rectangles (`0xFFC28956`, `0xFFD19A66`) on the dark P19 training soil; `DUMMY` and `RACK` lack cached shadow sprites. |
| `OBSIDIAN_COLUMN` / `RUINED_PILLAR`, `MOSSY_BOULDER`, `BARREL_CLUSTER` | F3 / F1 / Interiors | `sanctuary_p03/pillar.png` (`44x92`), `boulder.png` (`48x38`), `barrels.png` (`52x44`) | Existing PNGs (`botPad=2..3`, `semi=0`) | **Already crisp pixel-art PNGs** with binary alpha and grounded bases — keep locked/preserved as clean controls alongside P18 `oak.png` and P20 `cypress.png`. |

---

## 3. P23 Execution Plan (Checkpoints 1–5)

1. **Checkpoint 0 (This Audit + Baseline Renders):** Capture uncropped pre-P23 `REAL ENGINE RENDER` baseline frames in `docs/visual-evidence/p23/engine-render-before/` via `MapForgeP23RenderTest` at `0.36, 0.88, 1.00, 1.48` before modifying any art or production code.
2. **Checkpoint 1 & 2 (Pillar B — Architecture 2.5D Repair):**
   - Author `tools/mapforge_p23_architecture.py` to deterministically build all 7 `premium_slice/` facilities (`armory`, `hero_quarters`, `restaurant`, `hero_registry`, `synthesis_lab`, `summoning_hall`, `tower_gate`) + Sanctuary cottage/townhouse & perimeter tower PNGs (`townhouse_a`, `townhouse_b`, `cottage_slate`, `cottage_brown`, `fortress_tower`, `gate_tower` in `sanctuary_p03/` with matching upgraded procedural fallbacks in `PixelStructureAtlas`/`PixelSanctuaryLandmarks`).
   - Enforce **zero interior vertical alpha gaps** on building bodies/roofs, continuous roof-to-wall skeletons (ridge cap, front/hip planes, fascia, soffit, rafter tails/corbels, 2px cast overhang shadow), coursed oblique right-side returns (`1.5–2` value steps darker), grounded stone plinths (`H-3` footprint contact), Armory playable entrance aligned to `(993, 655)` (`cx=94, sill=163`) while preserving existing contract compatibility, Hero Quarters `(823, 790)` & Restaurant `(1103, 925)` recessed arched portals with 3D steps, walk-up counter/archway niches on zone-only `hero_registry` & `synthesis_lab`, unified Summoning Hall roof->colonnade->podium->plaza massing around rune `(111, 176)`, and solid Tower Gate battlements/dais around dark rift `(130, 120)`.
3. **Checkpoint 3 (Pillar A — Environmental Props Cohesion):**
   - Author `tools/mapforge_p23_props.py` for `flower_planter.png` (`64x36`), 3-frame `street_lantern_0..2.png` (`32x66` with byte-identical static plinth/post/bracket/frame pixels across all 3 frames), `wooden_bench.png` (`58x36`), `ruin_arch.png` (`96x86`), `retaining_wall.png` (`88x48`), `monolith.png` (`44x74`), `training_dummy.png` (`40x58`), `weapon_rack.png` (`68x48`), and `striking_post.png` (`36x86`).
   - Wire `SanctuaryArtAssets.assetSafe` + upgraded procedural fallbacks in `PixelStructureAtlas.kt`, `SanctuaryEnvironmentShadows.kt`, `PixelSpriteRenderer.kt`, and `InteriorWorldRenderer.kt` with zero per-frame allocations.
4. **Checkpoint 4 & 5 (QA Gates, Engine Render Before/After Proof & Docs):**
   - Add `tools/validate_mapforge_p23.py` + `MapForgeP23ContractTest.kt`, run full CI render + contract + debug APK verification, visually inspect every `engine-render-before/` vs `engine-render/` pair, and publish `P23_ARCHITECTURE_REPAIR.md`, `P23_ENVIRONMENT_PROPS.md`, and `P23_REPORT_VI.md`.
