# Lyra 4-direction sprite pack (P17)

First production milestone: **four coherent direction poses + complete idle and walk
cycles**, authored as pixel clusters, validated, and exported atlas-ready.

- Generator: `tools/generate_lyra_sprites.py`
- Validator: `tools/validate_lyra_pack.py` → `art/p16_lyra/pack/lyra_pack_validation.json`
- Pack: `art/p16_lyra/pack/` (atlas, per-direction sheets, per-frame PNGs, manifest)
- **Not integrated into the game yet** — see §6 for the exact reason and the diff to do it.

## 1. Engine contract

| Property | Value |
| --- | --- |
| Frame | 64 × 96 px |
| Foot pivot | (32, 90) — lowest opaque row is exactly 90 in every frame |
| Rows | 4, engine order: `front_right`, `front_left`, `back_right`, `back_left` |
| Row rule | `row = (north ? 2 : 0) + (right ? 0 : 1)` — `PremiumActorAtlas.frame()` |
| Columns | 14 = idle 6 + walk 8 |
| Atlas | 896 × 384 RGBA, 1.31 MB |
| Sampling | `FilterQuality.None`, world pixel scale 1.08 |
| Aisle | 1 px transparent margin on all four sides of every frame |

## 2. Direction mapping (do not reverse this)

| Row | Name | What the player sees | When the engine picks it |
| --- | --- | --- | --- |
| 0 | `front_right` | camera-side 3/4, walking to screen-right — the "hero" pose | `facingNorth = false`, `facingRight = true` |
| 1 | `front_left` | camera-side 3/4, walking to screen-left | `facingNorth = false`, `facingRight = false` |
| 2 | `back_right` | back view, moving away to the right | `facingNorth = true`, `facingRight = true` |
| 3 | `back_left` | back view, moving away to the left | `facingNorth = true`, `facingRight = false` |

The oblique camera never shows a pure side profile or a symmetric straight-on south pose:
the four engine combinations above are the real vocabulary of the game. Anything else would
produce facing pops while walking.

## 3. What is in the art, per direction

- **South / front rows (0, 1)** — face with 2 px eyes and amber iris, three-clump fringe,
  two braids, open cloak over a quilted jerkin, oxblood sash with a hanging tail, brass
  pauldron on the bow-arm shoulder, quiver fletchings behind the opposite shoulder, laced
  boots, recurve bow held low and unstrung (no bright string at rest, matching the concept).
- **North / back rows (2, 3)** — full auburn hair mass with no face pixels, braids over the
  cloak, hood bunched at the collar, quiver box and two fletchings on the shoulder, cloak
  folds, boots below the hem.
- **Asymmetry is corrected, not mirrored blindly** — row 1 is mirrored and then re-lit, and
  the brass pauldron is always drawn on the bow-arm shoulder while the quiver always sits on
  the opposite shoulder, so gear never swaps sides between rows.

## 4. Animation

| Clip | Frames | Timing | Motion |
| --- | --- | --- | --- |
| idle | 6 | 0.24 / 0.16 / 0.20 / 0.24 / 0.16 / 0.20 s | 2 px breathing that moves the chest but never the feet, cloak weight-lag after each rise, 1 px head settle, blink on frame 4 |
| walk | 8 | 0.08 / 0.07 / 0.07 / 0.08 / 0.08 / 0.07 / 0.07 / 0.08 s | contact → down → pass → up twice, ±4 px stride, 3 px swing-leg lift, hip/shoulder counter-motion, cloak lag, bow-hand swing, planted foot never leaves row 90 |

Every frame inside a clip is a distinct authored pose (SHA-256 uniqueness is asserted by
both the generator and the validator). There are no translation-only frames.

## 5. Validation (all gates pass)

`python3 tools/validate_lyra_pack.py` re-derives everything from the PNG + manifest:

```
status                     PASS
frames_checked             56
grid                       14 columns x 4 rows, 64x96, pivot (32,90)
max colours per frame      35 (gate: 40 for a multi-material character)
palette membership         all colours inside the authored ramps
atlas RGBA                 1.31 MB   (Kaelen's full 11-clip atlas: 6.00 MB)
split actor copies         1.31 MB
per frame                  24 KB
```

Gates enforced per frame: non-empty, binary alpha, no content on the frame edge (atlas
bleed), lowest opaque row == pivot row, ≤ 40 colours, unique inside its clip, colours inside
the authored palette.

Previews: `art/p16_lyra/pack/lyra_contact_sheet_3x.png` (3× grid with the pivot line),
`lyra_actual_size_preview.png` (≈1 screen px per art px),
`docs/visual-evidence/p17/lyra_ingame_scale_mockup.png` (**composite mockup on a real
P1.5A capture — not an engine render**).

## 6. Why it is not wired in yet, and the exact integration diff

`PremiumActorAtlas.get(id)` currently:

1. returns `null` unless `id` is `kaelen` or `goblin_raider`;
2. builds clips from `premium_slice/manifest.json` `clip_order` and `getValue(key)`s
   `attack` / `skill` / `hit` / `rest` / `defeat` / `fear` in `frame()`;
3. requires `source.width == column * width`, i.e. **every clip in `clip_order` must exist
   in the sheet**.

So dropping a 2-clip sheet in now would either be ignored or crash on a missing clip. The
integration diff, once the remaining clips are authored, is:

1. author `attack`, `skill`, `hit`, `rest`, `defeat`, `fear` (+ `calm`/`alert`/`victory`)
   into the same generator so `clip_order` matches the existing manifest;
2. add a `lyra` entry to `premium_slice/manifest.json` `assets` (file, size, sha256) and
   extend the allowlist in `PremiumActorAtlas.get` to `"lyra"`;
3. map `hero_starter_lyra` (or `HeroClassType.ARCHER` + name) to the `lyra` atlas in the
   character draw path, keeping the existing 64×96 / pivot / 1.08 scale contract;
4. confirm no new allocation per frame: the atlas is decoded once and frames are
   `ImageBitmap` crops, same as Kaelen.

None of that is done here: it needs a build (unavailable in this workspace) and the
remaining clips, and it must not destabilise the PR #19 renderer work.

## 7. Memory / performance impact when integrated

- One extra texture of **1.31 MB** at 11 clips × 11+ frames it will grow to roughly
  Kaelen's size (≈6 MB for 64 columns); that is the same order as the actor already in the
  game, so two premium heroes ≈ 12 MB of decoded ARGB_8888.
- No new draw call: one sprite blit per actor per frame, exactly like Kaelen.
- No new shader, no full-screen pass, no per-frame decode.
- Lyra idle/walk frames are static bitmaps; the Lobby keeps its existing animation clock.

## 8. Remaining work

1. attack (8) + skill (9) + hit (4) + fear (6) + defeat (6) + rest (1) + emotions for all
   four rows — this is what blocks integration.
2. Portrait/card art already exists (`art/p16_lyra/lyra_portrait_bust.png`), not wired to
   the hero panel.
3. Real-device check at zoom 0.36 / 0.88 / 1.48 to confirm readability and that the cloak
   hem does not collide with the canopy occlusion pass.
