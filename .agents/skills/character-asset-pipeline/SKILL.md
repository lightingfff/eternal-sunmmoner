---
name: character-asset-pipeline
description: Use the user-requested Character Animation Creator, Meowa and current Scenario references for ETERNAL SUMMONER sprites, animation, VFX and environment asset preparation while preserving native Android runtime contracts.
---

# ETERNAL SUMMONER character and asset pipeline

Use for character/monster frames, cross-action alignment, combat VFX and environment
asset preparation. Announce the one to three relevant sources, then read those parts
of the [library](../../../docs/skill-library/character-assets/README.md). Do not load
all 181 entrypoints into every implementation task. This is saved project guidance;
it is not model-weight training or an installed provider connection.

## Choose the route

| Work | Read | Project application |
|---|---|---|
| Reference-grounded idle/walk/attack strips | [Character notes](../../../docs/skill-library/character-assets/READING_NOTES.md), pinned `game-character-sprites` | Lock identity, requested action/direction/cell scope; make a sample strip first; regenerate failed strips only. |
| Existing animation snaps between actions | Meowa `tutorials/frame-animation/animation-alignment.md` | Review anchor evidence; use one fixed integer translation per clip, preserving intended motion. |
| Attack feels slow or contact/VFX is late | Meowa `tutorials/frame-animation-postprocess/speed-and-trim.md` and `tutorials/game-integration/frame-animation.md` | Keep source frames, preserve impact readability, map timing to the existing simulation's contact event. |
| Several directions or a video-derived loop | Current Scenario `scenario-sprite-animation` + `references/directional-cycles.md` | Verify facings, frame order, motion amplitude, loop seam and asymmetrical weapon hands. |
| Props, terrain, building textures | Meowa `references/maps-tiles-and-textures.md`, existing `scenario-game-assets` / `scenario-textures` | Produce independent layers/props, real alpha/texture seams; retain oblique camera and world collision. |
| Capability/provider selection | Meowa `references/capability-routing.md`, current `scenario` | Check actual access/schema/budget first; reading never authorizes a billed call. |

Resolve Meowa paths below
`docs/skill-library/character-assets/meowa/upstream/skills/game-assets/`.
Current Scenario paths are in
`docs/skill-library/character-assets/scenario/delta/skills/`; unchanged source texts
are reused from the earlier Scenario archive. See the [catalog](../../../docs/skill-library/character-assets/CATALOG.md)
for exact paths, per-skill support completeness and pinned source links.

## Establish the actual game contract first

1. Inspect the current loader, manifest and combat state before planning a new atlas.
   The base reviewed here uses `tower_combat_v1/manifest.json`: 128×144 enemy cells,
   twelve columns, foot pivot (64,132), right-facing with runtime-mirrored left.
   Its contact clip is .48 seconds, with contact at .22 seconds. VFX use 96×96,
   eight columns, four rows and pivot (48,84). These are checked by the current
   loaders; upstream 64px/six-frame/eight-direction defaults cannot replace them.
   Re-check current code: these figures are baseline evidence, not an eternal schema.
2. A different authored size/direction set needs its own assigned integration task
   and tests, not silent resizing, a fake four-direction label, or a loader workaround.
   Separate native redraws from nearest-neighbor preview/scaled variants.
3. Preserve Kotlin/Compose Canvas, camera ground Y=.72 and upright sprite height.
   Terrain footprints, sprite silhouettes, collision and input geometry are separate.
   Keep world anchors, depth sorting, canopy occlusion and SanctuaryLight shadows.
   Pixel previews use nearest-neighbor integer zoom; do not change the game's
   continuous camera/zoom/input transforms to match another engine's example.

## Author, align and inspect

- Lock original character identity, silhouette, palette, weapon hand and facing with
  an accepted reference. Read the actual image; do not substitute invented identity
  notes for unavailable visual grounding. Generate missing directions only when needed.
- Pilot one action/direction before a batch. A turnaround is still poses, not proof
  of animation. Walk must carry distinct contacts/passing poses and weight shift;
  attacks need readable anticipation, contact and recovery. Keep full weapons in cell.
- Keep generated originals and provenance. Record source/tool/output hash, prompt,
  frame order, requested versus delivered sizes, durations, pivot, loop and event
  markers. No copied proprietary manhwa panels or borrowed character artwork.
- Preserve a foot/root placement reference between idle, walk, attack, hurt and death.
  Review overlay/transition playback at one fixed world position. Correct a padding
  offset with one reviewed fixed translation for the whole clip, or supported pivot
  metadata. **Do not re-center every frame by bounding box/centroid/lowest pixel**:
  jumping, leaning, knockback, weapons and capes make those measures move legitimately.
  Confirm code owns translation; do not double-apply root motion from artwork.
- Remove only the intended background. Choose a key not present in the costume/VFX;
  border-connected cleanup must preserve eyes, armor gaps and translucent effects.
  Inspect the decoded alpha and every frame, not a composited preview alone.
- Quantization/snapping is conditional on actual defects. Meowa calls its pixel
  outputs already pixelated; Scenario may prescribe per-tile snapping. Neither claim
  replaces inspection or authorizes another lossy pass on an already valid atlas.
  If cleanup is required, retain a consistent palette and pixel grid across clips.
- Preserve source order and duration explicitly. Check walk/idle seam against the
  clip's own motion; identical frames also make a low-difference loop. Require visible
  real motion. Attack/hurt/death are not automatically looping or ping-ponged.

## Integrate with combat and Android safely

- The simulation owns damage, cooldowns, targeting, hit/death processing, projectile
  spawn and retries. Renderers consume those states. Never create damage from an
  animation draw callback or fire a second hit after a frame skip/resume.
- For a visual-only replacement preserve existing contact/release markers. If retiming
  changes gameplay, use an explicit combat task and independent review. Check irregular
  dt, pause/resume, cancellation, death and retry; verify one impact event per attack.
- Keep travelling projectiles and large VFX separate from the body atlas. Time contact
  sparks/audio to the real hit; preserve telegraphs and hero/enemy readability.
  Do not export a promotional composite as the interactive game asset.
- Keep finite cached bitmaps preloaded outside drawing. Measure decoded dimensions
  and bytes, mirrored copies, overlays and combined loader budgets before expanding.
  Existing Tower packs have a combined 8 MiB check; do not bypass it. Preserve fallback,
  culling, nearest sampling and bounded effects. No per-frame bitmap decode/network.
- Run focused geometry/timing/loader tests and APK build for actual integration.
  Inspect real runtime contact, transitions, edges and multiple gameplay zooms.
  A contact sheet or green CI is not proof of visual quality or physical-device FPS.

## Provider and source boundaries

Meowa/Scenario runners are retained under documentation as inactive references.
Do not execute their code, install dependencies, authenticate, upload assets, publish
previews, change account settings or spend credits just to learn a skill. For a later
authorized task, inspect the selected runner and real tool capabilities first, reuse
existing authorization/budget and resume existing jobs instead of submitting duplicates.
Never put credentials into chat, manifests, commits or example output. No attempt to
evade provider moderation. Use the hosted image-generation tool when applicable and
available, respecting its instructions and saving real output for local inspection.

Character README states MIT but supplies no full license text at the pinned revision.
Only original project notes and pinned links/hashes are saved here; its code/media are
not redistributed. Meowa and Scenario retain their actual MIT notices and source bytes.
Other engines, web publishing, marketing, model training, speech cloning and enterprise
administration remain conditional references, not requirements of this offline game.
