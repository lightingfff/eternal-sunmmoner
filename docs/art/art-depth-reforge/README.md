# Art depth reforge — first reviewable delivery (#115)

Base: `main@380e4e3ba4840e673e54966715ff12a49cbdfab4`.
Branch: `codex/art-depth-reforge`. This is a fresh original art integration, independent
of ArgonV2/PR67 and the gameplay PRs. No merge is authorized by this delivery.

The production renderer now uses16 coherent material families across Sanctuary,
interiors and Tower biomes, rather than ignoring the new PNGs in a procedural hot
path. Continuous64px cells preserve their4x4 spatial order; Sanctuary grass is
batched into256-world-unit macros. Meadow roads use cached rounded path unions,
with exact model widths and nearest, non-antialiased material cell boundaries.
This removes the64-world-unit staircase without moving gameplay paths or collision.

The Summoning Hall has original retextured stone, roof courses, shaded columns,
door recess and a thick terrace on its unchanged268x206 canvas. Shared building
grounding follows visible feet instead of appending a second slab/staircase. Stale
hardcoded AO marks are removed; actual directional silhouette shadows, depth order,
door positions and canopy occlusion remain. Authored architecture avoids the
previous enlarged bitmap with artificial noise; original destination scales remain.

Kaelen, Lyra and Vesper each have24 original normalized poses: front-right and
back-right idle, walk, attack, hurt/collapse/defeat. Left uses a pivot transform,
not a second bitmap bank. Both body and shadow mirror locally while the world-light
direction remains fixed. Their portraits also appear in the actual hero cards.
Kaelen's contact pose follows the existing.22s/.30s damage markers; ranged heroes
start at release because their existing projectiles execute immediately. Attack
timing, damage, AI, save schema and retry logic are unchanged.

Four PixelFX effects replace the existing same-size execution bank: shield block,
mana burst, sword slash and area healing. Their MIT source is pinned and vendored
for offline export, with LICENSE/NOTICE. Source actor, hitstop and damage cues are
not imported. The original PNG bank and vector rendering remain fallback options.
Shared management panels/buttons gain edge material, aged gold and subtle elevation;
card controls retain their dimensions, names and click behavior.

## Source and reproducibility

`source/` and `hero-production/` retain genuine hosted-imagegen originals, prompts
where recorded, normalization reports, atlas hashes and animation previews. These
are normalized derivatives, **not native64 generation claims**. One root/scale per
clip is recorded; no missing pose was replaced by a translated fake frame. Kaelen's
walk gait remains modest, with similar contact phases; distinct hashes alone do not
establish natural animation. A human creative review remains required.

```sh
python3 tools/art/build_art_depth_materials.py
python3 tools/art/build_art_depth_temple.py
python3 tools/art/normalize_authored_heroes.py
python3 docs/art/art-depth-reforge/hero-production/lyra/normalize_lyra.py
node tools/art/export_pixelfx.mjs
python3 tools/art/build_pixelfx_atlas.py
python3 tools/art/verify_art_depth.py
```

Python requires Pillow; the material normalizer also requires NumPy. Node18+ is
needed only for source export. No provider key, Node runtime, font/gallery asset
or network download is required in the APK. The learned skill references informed
this Kotlin/Compose pipeline; engine-specific Unity/Godot templates were not installed.

## Verification and limits

See [native evidence and test report](../../visual-evidence/art-depth-reforge/README.md).
Native host Skia captures were inspected at.36/.88/1.48, day/night, architecture,
doorway/canopy, named hero direction, short landscape UI and production Tower input.
Test fixtures are explicitly arranged; they are not a5–10minute human playtest.

This delivery applies changes to all requested categories, but is **not a claim
that every game asset has been redrawn or visually accepted**. Existing detailed
enemy sprites and most validated building/vegetation PNGs are retained. Remaining
creative work includes island cliff/outer-wall materials, richer gait transitions,
other hero identities, selected boss/monster silhouettes, broader skill variants
and the Tower gate/cutscene art. These need separate scoped art batches and review,
without replacing gameplay, camera, footprints or other agents' packs.

Argon/A08 creative acceptance, A09 independent QA and physical-device profiling
remain pending. Cold-start preparation is synchronous; do not move shared cache
warming onto IO without first designing safe publication and cache ownership.
