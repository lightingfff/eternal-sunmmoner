---
name: pixelfx-eternal
description: Export licensed PixelFX execution effects into bounded Kotlin/Compose sprite atlases without altering combat simulation.
---

# PixelFX for ETERNAL SUMMONER

Read root AGENTS.md and the actual task ownership first. This is an offline asset
workflow, not a new Android runtime library. It does not provide a paid service,
launch agents, or grant permission to edit another branch.

Use `docs/skill-library/pixelfx/CATALOG.md` to select relevant effects. The pinned
upstream is `patch-gg/pixelfx@5294518ce7c72e48b6100a744596e0acf57f2826`.
Read its preserved SKILL/AUTHORING references and LICENSE/NOTICE before importing
additional source. Only MIT code is vendored; restricted gallery/font assets are
excluded. Never bundle PatchPixel or gallery examples on the assumption they are MIT.

For the current four-effect pack run from the repository root:

```sh
node tools/art/export_pixelfx.mjs
python3 tools/art/build_pixelfx_atlas.py
```

Node 18+ runs only during asset production. Python requires Pillow. The export uses
seed19, no stock actor, transparent color plus light output. The normalizer keeps
one fixed origin and scale per clip, samples eight nonempty unique execution frames,
and writes a 768x384 RGBA atlas with 96x96 cells and pivot(48,84). Inspect the
contact sheet AND actual production-render captures; neither static sheets nor
passing tests establish animation feel.

Preserve .72 ground projection, upright height, nearest sampling, original world
anchors and combat markers. PixelFX hitstop, cues and actor timelines are reference
metadata only; never apply their damage/timing to the game. `TowerCombatFxAtlas`
loads the new optional pack, then the existing pack, then vector fallback. No PNG
decoding or bitmap resampling belongs in the draw loop. Enemy plus VFX retained
bitmaps remain within the existing8MiB bank. Named heroes use a separate4MiB bank.

Run `ArtDepthAssetsTest`, `TowerCombatArtEvidenceTest`, the affected UI/input tests
and `:app:assembleDebug`. Capture representative early/contact/recovery effects at
.36/.88/1.48 zoom and inspect silhouettes, actor readability and simultaneous FX.
Report real-device GPU/FPS/thermal separately; native host Skia is not a phone.
