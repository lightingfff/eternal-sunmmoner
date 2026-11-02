# Lyra 4-direction sprite pack (P17 → P17-B)

Status: **complete 11-clip production pack, shipped in the APK and wired into
`PremiumActorAtlas`.** Idle + walk landed in P17 (`0fdd15a`); attack, skill, hit, rest,
defeat, calm, alert, fear and victory landed in P17-B.

| | |
| --- | --- |
| Generator | `tools/generate_lyra_sprites.py` (single deterministic run rebuilds everything) |
| QA gate | `tools/validate_lyra_pack.py` → `art/p16_lyra/pack/lyra_pack_validation.json` |
| Visual evidence | `tools/lyra_visual_evidence.py` → `docs/visual-evidence/p17/` |
| Runtime asset | `app/src/main/assets/premium_slice/lyra.png` (SHA-256 pinned in `premium_slice/manifest.json`) |
| Review pack | `art/p16_lyra/pack/` (atlas copy, per-clip sheets, 4 direction sheets, contact sheet, both manifests) |
| Integration | `PremiumActorAtlas` id `lyra`; `PixelSpriteRenderer.drawCharacterSprite` maps `heroName == "Lyra" && heroClass == ARCHER` |

Rebuild + verify:

```
python3 tools/generate_lyra_sprites.py     # writes the atlas, pack manifest, contact sheets
python3 tools/validate_lyra_pack.py        # exit 0 = PASS, exit 1 = FAIL
python3 tools/lyra_visual_evidence.py      # regenerates docs/visual-evidence/p17
python3 tools/validate_assets.py           # CI's own asset gate (PNG + manifest SHA)
```

## 1. Engine contract

| Property | Value |
| --- | --- |
| Frame | 64 × 96 px |
| Foot pivot | (32, 90) — the lowest opaque row is exactly 90 in all 256 frames |
| Rows | 4, engine order: `front_right`, `front_left`, `back_right`, `back_left` |
| Row rule | `row = (north ? 2 : 0) + (right ? 0 : 1)` — `PremiumActorAtlas.frame()` |
| Columns | 64 = the full `clip_order` below |
| Atlas | 4096 × 384 RGBA — 6.00 MB decoded, 144 KB PNG |
| Sampling | `FilterQuality.None`, `worldPixelScale` 1.08 |
| Aisle | ≥1 px transparent margin around every frame (validator gate) |
| Palette | 40 authored tones, ≤38 per frame (multi-material hero, gate documented in the validator); P17-C lifted the jerkin ramp one step so the chest separates from the cloak |

Ground clipping is structural: `ground_clip()` clears every pixel below row 90 before the
frame is written, so the pivot assertion cannot be broken by an outline or a collapsed pose.

## 2. Clip table (identical to `premium_slice/manifest.json`, enforced by the validator)

| Clip | Frames | Loop | Total (s) | Beat content |
| --- | --- | --- | --- | --- |
| `idle` | 6 | yes | 1.20 | breathing, cloak sway, blink on f4 |
| `walk` | 8 | yes | 0.60 | ±4 px stride, 3 px foot lift, cloak/hair phase offset |
| `attack` | 8 | no | 0.44 | carry → nock → raise → draw → full draw → release (f4) → follow-through → recover |
| `skill` | 9 | no | 0.72 | magic nock → aim → charge → overhead draw → release (f4) → spark → settle |
| `hit` | 4 | no | 0.30 | recoil back, deepest impact (f1), weight returns, settle |
| `rest` | 1 | no | 1.00 | seated on the ground, knees up, bow across the lap |
| `defeat` | 6 | no | 1.00 | stagger → buckle → four collapsed stages (dedicated `draw_collapsed`, not a rotated still) |
| `calm` | 6 | yes | 1.20 | shallow breathing, head turned, bow lowered |
| `alert` | 4 | yes | 0.80 | weight forward, bow half-raised, head up |
| `fear` | 6 | yes | 0.80 | pull in, step back, cower (deepest f2), partial recover |
| `victory` | 6 | yes | 1.20 | bow raised overhead (f1–f3), settle back to a low guard |

Column mapping (`start_column`): idle 0, walk 6, attack 14, skill 22, hit 31, rest 35,
defeat 36, calm 42, alert 48, fear 52, victory 58.

## 3. VFX / event markers

Recorded in `lyra_pack_manifest.json → vfx_markers`:

| Marker | Frame | Why |
| --- | --- | --- |
| `attack_release_column` | 19 (attack f5) | the frame after the string clears — arrow spawn |
| `skill_release_column` | 26 (skill f4) | magic projectile spawn |
| `skill_charge_columns` | 23–25 | magic arrow draw + glow window |
| `hit_reaction_column` | 32 (hit f1) | deepest impact — hurt flash / knockback |
| `walk_contact_columns` | 8, 12 | zero-stride foot-contact ticks for dust |
| `defeat_start_column` | 36 | death handling starts here |

The existing runtime does **not** consume these markers yet: VFX are still driven by
`PremiumSliceRenderer.drawSkillGround` and the generic hurt flash. They are authored data
for the next pass, not a claim that they are wired.

## 4. Direction mapping (do not reverse this)

| Row | Name | What the player sees | When the engine picks it |
| --- | --- | --- | --- |
| 0 | `front_right` | camera-side 3/4, moving to screen-right | `facingNorth = false`, `facingRight = true` |
| 1 | `front_left` | camera-side 3/4, moving to screen-left | `facingNorth = false`, `facingRight = false` |
| 2 | `back_right` | back view, moving away right | `facingNorth = true`, `facingRight = true` |
| 3 | `back_left` | back view, moving away left | `facingNorth = true`, `facingRight = false` |

Rows 1 and 3 are drawn from the same construction and mirrored, then `relight_left()`
reramps glints back toward the fixed upper-left key light, so the light never flips with the
direction. The oblique camera never shows a pure profile or a symmetric south pose.

## 5. What is drawn, per direction

- **Front rows** — face with 2 px eyes and amber iris, three-clump fringe, two braids, open
  cloak over a quilted jerkin, oxblood sash with a hanging tail, brass pauldron on the far
  shoulder, bracer on the bow arm, charred-wood recurve bow, two-arrow quiver.
- **Back rows** — braid fall over the cloak, no face, quiver and bow visible on the far side.
- The bow arm rotates around the shoulder: carried at the hip in `idle`/`walk`/`calm`, at the
  silhouette edge while aiming, overhead in `skill`/`victory`. The grip always stays outside
  the torso silhouette so the aiming frames still read at 1x.
- The pauldron never swaps shoulders between clips — a side swap would pop mid-animation.

## 6. Memory

| | Lyra | Kaelen |
| --- | --- | --- |
| Atlas | 4096 × 384 RGBA | 4096 × 384 RGBA |
| Decoded | 6.00 MB | 6.00 MB |
| Per frame | 24 KB | 24 KB |
| PNG on disk | 144 KB | 148 KB |
| Blits per actor per frame | 1 | 1 |

No new shader, no second decode: `PremiumActorAtlas` crops sub-bitmaps once and caches the
atlas instance per id. Lyra and Kaelen can both be resident (12 MB of decoded bitmaps) —
that is the same arithmetic the game already accepted for Kaelen + goblin.

## 7. Integration (what actually changed)

1. `PremiumActorAtlas.get()` — allowlist is now `kaelen`, `goblin_raider`, `lyra`, and the
   whole build is wrapped so a missing asset, a malformed manifest or a bad sheet returns
   `null` instead of throwing. Every call site keeps its procedural fallback.
2. `PixelSpriteRenderer.drawCharacterSprite()` — `premiumActorId(heroName, heroClass)` maps
   `Lyra` + `ARCHER` → `lyra`; the `drawSkillGround` ring is now Kaelen-only so an archer
   does not inherit a knight's shield shockwave.
3. `premium_slice/manifest.json` — new `assets.lyra` entry with the exact SHA-256, so
   `tools/validate_assets.py` (run in CI) verifies the shipped bytes.
4. No save, gameplay, balance, Gradle, package-ID or renderer-optimisation changes. P16
   batching is untouched.

## 7b. P17-C runtime follow-ups

Engine-render evidence, the authored HUD portrait, the bow-release anchor and the Lobby
allocation audit are documented in `docs/art/P17C_RUNTIME_QA.md`. The portrait lives at
`premium_slice/portraits/lyra_portrait.png` (64×64, authored by
`tools/generate_lyra_portrait.py`) and is resolved by `PremiumPortraits`, so the HUD no longer
shows Kaelen's face for Lyra.

## 8. Verification actually run

| Check | Command / run | Result |
| --- | --- | --- |
| Generator determinism | run twice, compare SHA-256 of `lyra.png` | identical — `dd072ffd…48d04d2d` (updated value recorded in the manifest) |
| Pack QA | `python3 tools/validate_lyra_pack.py` | **PASS** — 256 frames, ≤38 colours/frame, no edge bleed, baseline row 90, no duplicates, no pure-translation frames, engine contract match, asset SHA match |
| Asset gate (CI step) | `python3 tools/validate_assets.py` | PASS |
| Android build | GitHub Actions run [41413474427](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41413474427) on `4fc1612` | **Build Android APK: pass (2m13s)**; "Publish direct APK to Release": skipped (PR event) |

Not run: unit tests (the workflow has no test step), any device/emulator render, FPS,
thermal or memory measurement.

## 9. Known gaps

- `VirtualJoystickAndControls` still crops Kaelen's portrait (`srcOffset 19,4 / 26×26`) for
  every premium hero; Lyra's portrait crop is not authored yet, so her HUD portrait falls
  back to the existing sheet.
- Emotion clips play through `emotion_clips` (`calm`/`alert`/`fear`/`victory`); the lobby
  only ever asks for `fear`, `alert` and `calm`, so `victory` is authored but unused.
- VFX markers are data only (see §3).
- No device render, no FPS/thermal measurement was performed. The only build evidence is
  the GitHub Actions run recorded in the PR.
