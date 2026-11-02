# Lyra — premium hero art specification (P16 sample)

Target: `hero_starter_lyra`, Archer, "Mắt Ưng Gió Đêm" — the one Sanctuary hero that
still runs on the older procedural atlas. Kaelen already owns the P1.5A premium slice, so
Lyra is the next non-destructive upgrade and nothing here overwrites existing art.

Status of this cycle: **art direction + one validated derived idle frame**. Not integrated
into the renderer, not a production animation sheet. See §8 for what is and is not proven.

## 1. Engine contract (must not change)

| Property | Value | Source |
| --- | --- | --- |
| Frame | 64 × 96 px | `app/src/main/assets/premium_slice/manifest.json` |
| Foot pivot | (32, 90) | same manifest; used by `PremiumActorAtlas` |
| World pixel scale | 1.08 | same manifest |
| Visible world height | ≈ 92 world units | P1.5A note |
| Directions | front_right, front_left, back_right, back_left | same manifest |
| Clips | idle, walk, attack, skill, hit, rest, defeat, calm, alert, fear, victory | same manifest |
| Filtering | `FilterQuality.None` (nearest neighbour) | `PixelSpriteRenderer` |
| Camera | oblique 3/4, ground Y × 0.72, upright sprites keep full height | `ObliqueCamera` |
| Key light | warm `0xFFE8E0C8`, azimuth 142°, elevation 48° → key from upper-left, shadows to lower-right | `SanctuaryLight` |
| Memory | 64 × 96 × 4 B = **24 KB per frame**; a full 11-clip sheet is the budget to watch | measured |

Anything that changes these numbers changes the renderer, which is out of scope for art.

## 2. Design

Silhouette first: a long, torn moss-green cloak gives a wide, wind-broken triangle; the
quiver rises behind the left shoulder; the recurve bow reads as a long dark curve low on
the right. Read at 64 px, the shape must still say *"cloaked ranger, bow, quiver"*.

- **Head** — small, sharp. Auburn hair in three value ramps (deep `#5A2611`, mid `#89230F`,
  light `#BD9251`), two braids breaking the outline. One 2 px eye pair, amber, slightly
  narrowed. A single pale scar pixel over the left brow.
- **Torso** — quilted smoke-grey leather jerkin (`#34341E`, `#4B482A`) over an oxblood sash
  (`#89230F` → `#5A2611`). Brass clasp on the right shoulder as the one bright accent.
- **Cloak** — the largest shape: moss `#404427` lit face, `#2E2C1A` mid, `#212617` shadow,
  torn hem with 2-3 px notches so it never reads as a blanket rectangle.
- **Arms** — leather bracers with 1 px strap lines; the draw hand stays dark so the bow
  silhouette stays clean.
- **Legs** — dark fitted trousers, laced knee boots with a bright `#967646` top edge.
- **Weapons** — bow: near-black limb (`#150F06`) with a brass riser (`#BD9251`) and a
  single ivory nock highlight; quiver: three ivory fletchings at the top, the only place
  white is allowed at full value.

## 3. Palette (curated from the concept, 5 ramps)

| Ramp | Shadow | Mid | Light | Highlight |
| --- | --- | --- | --- | --- |
| Cloak moss | `#212617` | `#2E2C1A` | `#404427` | `#635D37` |
| Leather (jerkin / boots) | `#1A1A0E` | `#34341E` | `#4B482A` | `#967646` |
| Oxblood sash | `#421C0C` | `#5A2611` | `#89230F` | `#A8402A` |
| Auburn hair | `#301F0F` | `#5A2611` | `#794723` | `#BD9251` |
| Brass / metal | `#44321B` | `#8C6237` | `#BD9251` | `#F1D28B` |
| Ink / outline | `#100E16` | `#050804` | — | — |

Rules: no neon, no pure black fills (outline only), no more than one full-value white
cluster per frame (fletching or a glint). Skin stays in the ivory→`#8C6237` family so it
does not glow against the dark ground.

## 4. Shading rules for 2.5D

- Light from the upper-left, consistent with `SanctuaryLight`. Cloak and hair glints only on
  the upper-left plane; the right side of every form goes to its shadow ramp.
- Contact shadow is *engine-side* (`SanctuaryEnvironmentShadows`), never painted into the
  sprite. The frame stays fully transparent below row 90.
- Left-facing directions are mirrored, so painted glints on the "light side" must be
  re-ramped after mirroring (same rule the P1.5A manifest already documents).
- Roof/canopy occlusion is depth-sorted by foot anchor; no fake drop shadow inside the art.

## 5. Animation beats (per direction)

| Clip | Frames | Timing | Notes |
| --- | --- | --- | --- |
| idle | 6 | 0.24 / 0.16 / 0.20 / 0.24 / 0.16 / 0.20 s | breathing 1 px, cloak hem 1 px drift, braid sway |
| walk | 8 | ~0.075 s/frame | 2 px foot travel, cloak trail lags one frame |
| attack (shoot) | 8 | contact at 0.22 s, frame 4 | anticipation = draw hand to cheek, release = cloak snap, recovery = re-nock |
| skill (hawk volley) | 9 | contact at 0.30 s, frame 4 | both hands, hair lift, 1 frame eye flash |
| hit | 4 | 0.045 → 0.105 s | torso recoil 1 px, cloak compress, never break the pivot |
| fear | 6 | 0.08-0.18 s | bow lowered, shoulders in, feet narrow |
| victory | 6 | 0.14 → 0.24 s | bow raised, quiver settle, cloak billow |

## 6. Weapon and VFX handoff

- Bow limb must stay readable in silhouette during attack; the arrow is a 2 px diagonal
  with an ivory fletch, spawned at the engine's release frame, not painted into the body.
- Skill VFX (three-arrow fan, hawk silhouette) is a separate strip; it must not cover the
  hero's face. Timing lives in the gameplay contract, not in this art.

## 7. Export and QA checklist

1. RGBA, straight alpha, exactly 64 × 96 per frame, grid rows = direction, columns = frames.
2. Lowest opaque row = 90 for every grounded frame (idle, walk, attack, hit, victory);
   `defeat` is the only clip allowed to break it.
3. Corners transparent; no semi-transparent halo pixels (1-bit alpha after quantise).
4. ≤ 32 colours per frame, no anti-aliased edge pixels.
5. 3× repetition test: tile the sprite against itself at 1×, 1.5× and 2× zoom, confirm the
   outline does not double.
6. Verify against a real renderer capture (Robolectric evidence harness) before claiming
   integration.

## 8. What this cycle produced, honestly

Produced:
- `art/p16_lyra/lyra_concept_fullbody.png` — original full-body concept, generated in this
  session, flat studio backdrop (**this is the strongest asset of the batch**).
- `art/p16_lyra/lyra_portrait_bust.png` — original bust portrait for a hero panel / card.
- `art/p16_lyra/lyra_idle_sprite_64x96.png` — 64 × 96 idle sample **derived automatically**
  from the concept by `tools/generate_hero_sample.py`.
- `art/p16_lyra/lyra_sample_report.json` — pivot/alpha/palette validation (feet on pivot
  row 90, corners transparent, 25 colours, 24 KB RGBA).
- `art/p16_lyra/lyra_idle_sprite_4x_preview.png`, `lyra_palette.png`,
  `docs/visual-evidence/p16/hero_sample_vs_existing.png`.

Not produced, and not claimed:
- A production animation sheet. The derived frame proves the palette, silhouette and export
  contract, but an automatic reduction of a painterly 1024 px concept cannot invent clean
  1-px cluster control, a readable 3-px face, or consistent frame-to-frame identity. A
  hand-authoring pass (or a pixel-art-specialised generator) over this concept is required
  before anything is wired into `PremiumActorAtlas`.
- Any in-game integration, any device screenshot, any performance measurement.

Recommended next micro-batch: hand-author (or regenerate at 4× and clean up) the 4-direction
idle sheet for Lyra from this concept, then the walk cycle, and only then integration.
