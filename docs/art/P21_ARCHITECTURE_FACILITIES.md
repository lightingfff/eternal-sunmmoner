# P21 — premium Sanctuary facilities (six authored 2.5D sprites)

Scope: art + asset-loading only. No camera change (`ObliqueCamera.OBLIQUE_Y_SCALE = 0.72`), no
island silhouette, road width, prop anchor, footprint, collider, entrance trigger, zone, sort
key, elevation or `PropKind` change. `PixelSpriteRenderer.drawProp` keeps one
`drawCrispSprite` blit per facility with `FilterQuality.None`. No renderer geometry, shader,
post-process, per-frame decode or runtime generation was added.

Pipeline: `tools/mapforge_p21_architecture.py` (deterministic, offline, Pillow, no external art)
→ `app/src/main/assets/premium_slice/<facility>.png` → manifest `size` + `sha256` →
`PixelStructureAtlas` (`assetSafe` PNG-first, procedural fallback) → `drawProp`.

## 1. Checkpoint A — asset + geometry contract inventory (verified at this HEAD)

| facility | PropKind | obstacle anchor | scale | canvas | door trigger | notes |
|---|---|---|---|---|---|---|
| Summoning Hall | `FACILITY_SUMMONING_HALL` | (2430, 2005) `building_summon_hall` | 1.08 | 268×206 | — (zone 2380,1930 r210) | zone-only; interaction centre inverts to sprite (111,176) |
| Tower Gate | `FACILITY_TOWER_GATE` | (1960, 585) `monument_tower_gate` | 0.96 | 260×224 | — (zone 1960,635 r185) | renderer strokes an animated rift at sprite (130,120) rx16/ry27 |
| Hero Registry | `FACILITY_HERO_REGISTRY` | (785, 1185) `building_hero_registry` | 0.80 | 188×166 | — (proxy → Summoning Hall prompt) | zone-only |
| Hero Quarters | `FACILITY_HERO_QUARTERS` | (825, 790) | 0.80 | 200×168 | **(823, 790)** | enterable, dormitory interior |
| Restaurant | `FACILITY_RESTAURANT` | (1105, 925) | 0.80 | 206×172 | **(1103, 925)** | enterable, restaurant interior |
| Synthesis Lab | `FACILITY_SYNTHESIS_LAB` | (1270, 555) | 0.80 | 188×168 | — | zone-only |
| Armory | `FACILITY_ARMORY` | (995, 655) | 0.80 | 190×166 | (993, 655) | **P20 art, untouched** |
| Training Ground | `FACILITY_TRAINING_GROUND` | — | — | 192×160 | (890, 1942) | **no `WorldObstacle` exists on the Sanctuary map**, so the sprite is never drawn; not repainted this pass (see §6) |

Drawn bottom-centred: `cx = worldToScreenX(obs.x)`, `cy = worldToScreenY(obs.y) + 6*s`,
`pixelScale = 2*s`, `s = zoom * obs.scale`. Therefore one sprite pixel is `2*scale` world units
wide and `2*scale/0.72` world units deep. The inverse mapping (sprite pixel → world) is:

```
worldX = obs.x + (spriteX - W/2) * 2 * scale
worldY = obs.y - ((H-3) - spriteY) * (2 * scale) / 0.72
```

and the forward mapping used to place a painted door on an existing trigger is:

```
door_cx   = W/2 + (doorWorldX - obs.x) / (2 * scale)
door_rows = (H-3) - (obs.y - doorWorldY) * 0.72 / (2 * scale)
```

For Hero Quarters and Restaurant (`doorWorldX = obs.x − 2`, `doorWorldY = obs.y`) that is the
canvas centre X and row `H−3`. Both P21 doors were authored **at** those coordinates and the
contract test re-derives them from `SanctuaryHouseEntrances` rather than hard-coding a guess.

### 1.1 A measured defect in the P20 Armory (documented, not silently "fixed")

Applying the same inverse mapping to the P20 Armory art (door box `x 133..150, y 109..139`,
canvas 190×166, obstacle (995,655), scale 0.80) puts its painted door centre at world
**(1069.4, 601.7)** while its trigger is at **(993, 655)** — the art door sits ≈ **+76 world
units east and −53 world units north** of the doorway it is supposed to advertise. The Armory is
enterable, so a player aiming at the painted door is aiming ~76 units off the trigger.

That is P20's art and P20 is out of scope for this pass (the brief forbids overwriting
`premium_slice/armory.png` without explicit authorization), so **P21 does not touch it**. It is
recorded here, and `MapForgeP21ContractTest.enterableDoorsLandOnTheirWorldTriggers` makes the
same measurement impossible for Hero Quarters and Restaurant. Recommended next step: one bounded
follow-up that re-authors only the Armory's door surround to `x 88..99, y 119..161` (or moves the
entrance constant with the owner's approval — not both).

## 2. What was wrong with the six previous sprites

All six were `PixelSanctuaryLandmarks` procedural canvases. At zoom 0.88 they shared one weakness:
they were built from flat ellipses/rectangles with a single tone per surface, so they read as
sticker silhouettes with no mass. Specifically:

- **Summoning Hall** — an amphitheatre drawn as six concentric ellipses plus nine thin columns.
  At 0.88 it was a grey ring with a bright crescent; no statue-scale entry, no roof volume, and the
  "rune stage" was an ellipse floating inside the bowl rather than a place a hero stands.
- **Tower Gate** — two tapered monolith slabs with a vertical glowing ellipse between them. The
  pillar faces were one flat value, the portal read as a decal in front of stone (no reveal, no
  jambs), and the renderer's animated rift ring floated on the sky cleft instead of inside an
  opening.
- **Hero Registry / Quarters / Restaurant / Synthesis Lab** — all four were the *same*
  `paintAnimeHalfTimberHouse` call with different numbers, so four different professions looked
  like four recolours of one cottage; the door was a 20×28 arch placed by `gableCenter − 8` with
  no relationship to the world trigger; roofs had one plane and no eave thickness.
- None of the six had a baked contact rim, so they visually floated a few pixels above the lawn.

## 3. What the new art does (per facility)

Shared language: graphite-blue coursed masonry with 1 px upper-left rim light and dark-grey (not
black) mortar; jettied plaster storeys with structural oak posts/rails/braces sized to load;
two-plane roofs with shingle courses, ridge cap, stepped eave fascia and a soffit band falling on
the wall; deep-framed mullioned windows with a bounded amber ramp; a tight baked contact rim on the
base row; one key light only (upper-left, matching `SanctuaryLight` azimuth 142°, elevation 48°).

- **Summoning Hall** (268×206) — three terraced ritual tiers with radial slab joints, a colonnaded
  hexastyle temple front (cella wall deliberately recessed and darkened so the columns read as
  standing in front of it), bronze-crested two-plane roof with antefixae, arched portal with
  voussoirs, swung-open bronze doors and a cold arcane pool, flanking wings with lit slits, grand
  stair and braziers. **The inlaid rune ring is authored at sprite (111,176)** — the exact inverse
  of the zone centre (2380,1930) — so the hero activity spot and the visual focus coincide.
- **Tower Gate** (260×224) — battered basalt piers with lit outer edges, receding side faces, pierced
  crenellated caps, carved channels and small rune lamps; heavy lintel with cornice, dentils and
  three carved runes; **portal void authored to the renderer's animated rift box** (sprite centre
  (130,120), half-size 16×27 px), with lit/shaded jambs, voussoir ring, keystone, four depth bands
  and floor spill; three-tier dais; threshold stairs; rune obelisks as overview-zoom markers;
  weathering (moss at the feet, hairline cracks).
- **Hero Registry** (188×166) — civic scale, not another portal: stone ground floor, jettied
  timber-framed plaster storey, hipped slate roof with a **bell turret and clock face** (the
  0.36-zoom silhouette marker), bronze plaque row, ledger lectern, notice board and hanging ledger
  sign, stone bench.
- **Hero Quarters** (200×168) — warm-shingle hip roof, dormer, **lean-to wing on the left** so the
  mass reads as a building that grew over time, louvred roof vent, porch with posts, boot scraper,
  bench, hooped water barrel, crescent sign. Door on canvas centre, leaf bottom on row H−3.
- **Restaurant** (206×172) — the widest, warmest mass: hipped slate roof plus dormer, smoking hearth
  stack, **projecting entrance gable**, ochre/pale striped canvas awning, dining windows with a
  restrained ramp, terrace bench, barrel and lantern post, tankard sign.
- **Synthesis Lab** (188×168) — copper condenser stack with teal vapour, pipe run into the wall, ridge
  vent cupola, **teal** (not cyan, not purple) interior ramp, glass retort on an iron stand with a
  coiled worm, crates, calibration table, retort sign.

`sign_board`/`draw_icon` provide the profession marks (book, mug, moon, retort, anvil, glyph,
shield, sword, gear) as 2 px-legible clusters.

## 4. Loading contract (the `?:` trap from the P20 notes, now closed)

`SanctuaryArtAssets.asset(path)` is `images.getOrPut(path) { decodeAsset(path)!! }`: a missing file
throws inside `getOrPut` and an Elvis `?: fallback` after it can never run. P21 adds
`SanctuaryArtAssets.assetSafe(path)` which returns `null` on `IOException`/absent asset, **does not
cache failures**, and caches successes exactly like `asset`. `PixelStructureAtlas` uses it for all
seven authored facilities:

```kotlin
val summoningHallStructure: ImageBitmap get() =
  SanctuaryArtAssets.assetSafe("premium_slice/summoning_hall.png") ?: summoningHallFallback
private val summoningHallFallback: ImageBitmap by lazy { PixelSanctuaryLandmarks.buildSummoningHall() }
```

Every fallback canvas is byte-for-canvas identical to the PNG canvas (268×206, 260×224, 188×166,
200×168, 206×172, 188×168), so a fallback renders with the same anchor, door rows and shadow
silhouette. `MapForgeP21ContractTest.missingPngFallsBackToProceduralSprite` exercises the null path.
The procedural builders are retained (not deleted).

## 5. Gates

| gate | status |
|---|---|
| `tools/mapforge_p21_architecture.py` determinism (re-run ⇒ identical bytes) | PASS (verified locally) |
| offline canvas/alpha/edge/extent assertions in the generator | PASS |
| manifest `size` + `sha256` refreshed for 6 entries, no other key rewritten | PASS |
| `python3 tools/validate_assets.py` | PASS (local) |
| `python3 tools/validate_lyra_pack.py` | PASS (local, Pillow available) |
| `MapForgeP21ContractTest` (canvas, manifest pins, P20 intact, door↔trigger, world geometry, fallback, gate portal box) | **NOT RUN in this environment** — no JDK/Android SDK/network here; runs in CI |
| `MapForgeP21RenderTest` (engine frames) | **NOT RUN in this environment**; runs in CI and commits PNGs to `docs/visual-evidence/p21/engine-render/` |
| `assembleDebug` | **NOT RUN in this environment**; CI `Build Eternal Summoner APK` |
| device FPS / thermal | **NOT TESTED** — no device |

## 6. Deliberately out of scope

- **Training Ground sprite.** `PropKind.FACILITY_TRAINING_GROUND` is referenced by `drawProp` and the
  sprite atlas, but `TowerFloorCatalog.buildSanctuaryMap()` creates **no** `WorldObstacle` with that
  kind, so the 192×160 sprite is never blitted on the Sanctuary map and repainting it would be
  invisible. The training interaction is the sand-patch doorway at (890,1942). Wiring a prop in
  would be a map change (forbidden by this brief), so it is reported instead of faked.
- **Armory door mismatch** (§1.1) — P20 art, needs owner authorization.
- Cottages, townhouses, fortress towers, vegetation: untouched.
- Per-facility door-offset correction for the Armory, dynamic glow/bloom, reflections: Technical
  Artist / next pass.

## 7. Performance

Decoded bitmap cost is `W * H * 4` bytes (compressed PNG size is not RAM). The canvases are
unchanged from the procedural sprites they replace, so total decoded memory is unchanged:

| facility | canvas | decoded |
|---|---|---|
| summoning_hall | 268×206 | 215.7 KB |
| tower_gate | 260×224 | 227.5 KB |
| hero_registry | 188×166 | 121.9 KB |
| hero_quarters | 200×168 | 131.2 KB |
| restaurant | 206×172 | 138.4 KB |
| synthesis_lab | 188×168 | 123.4 KB |
| **total** | | **958.1 KB (0.94 MiB)** |

Draw cost: unchanged — one cached blit per facility per frame, same `drawCrispSprite` call, same
`FilterQuality.None`, same `SanctuaryEnvironmentShadows` silhouette path, no new pass, no
per-frame allocation. The only new runtime work is one PNG decode per facility on first access
(replacing the previous procedural build), cached in `SanctuaryArtAssets.images`. No FPS or
thermal claim is made: nothing was measured on a device.
