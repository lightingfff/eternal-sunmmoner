---
name: ai-game-spritesheets
description: Use the user's supplied anchor-to-animation prompt templates for game sprite work, with stable identity, consistent directions, foot pivots, normalization and runtime verification.
---

# AI game spritesheets — project adapter

User preference: for game implementation, sprite, art and animation tasks, consult this
skill and announce it in the opening chat update. Suggested Vietnamese wording:
“Tôi sẽ dùng skill ai-game-spritesheets và các template đã lưu, điều chỉnh theo camera và pipeline hiện có của game.”
Apply the relevant parts; do not regenerate unrelated assets or expand the requested scope.

## Source and capabilities

Imported from the user-uploaded `ai-game-spritesheets-main.zip` on 2026-11-02.
The unchanged prompt pack is in [upstream/README.md](upstream/README.md), with its
[MIT license](upstream/LICENSE), reference images, guides and previews preserved.
This SKILL.md is a local adapter, not an upstream skill or model training.
The ZIP does NOT include the commercial `animated-spritesheets` / `gamedev-assets`
automation skills linked by its README. Do not claim those skills, Seedance, fal.ai,
Bria or remove.bg are installed. Discover actual available tools; do not subscribe,
purchase or call external paid services merely because a template mentions them.

## Read only the needed template

1. [Concept identity](upstream/prompts/01-box-art.md): optional for new characters;
   use as silhouette/palette direction, not direct painterly sprite input.
2. [Canonical anchor](upstream/prompts/02-south-anchor.md): neutral full-body pose,
   readable identity, controlled palette, stable ground contact and logical pixel size.
3. [Neutral reset](upstream/prompts/03-neutral-anchor.md): remove baked-in action VFX.
4. [Directional anchors](upstream/prompts/04-directional-anchors.md): keep identity,
   outfit and prop handedness coherent across the camera's supported directions.
5. [Walk motion](upstream/prompts/05-walk-cycle-i2v.md): camera locked, in-place gait,
   alternating contacts, secondary motion and a clean loop. Image-to-video is optional;
   authored sprite poses are valid when no approved video tool is available.
6. [Attack](upstream/prompts/06-attack-spritesheet.md): anticipation, contact, recovery.
7. [Idle](upstream/prompts/07-idle-spritesheet.md): restrained breathing/cloth motion.
8. [Normalization](upstream/prompts/08-normalization.md): isolate frames, real alpha,
   stable body scale, consistent pivot, atlas metadata, contact sheet and GIF review.

## ETERNAL SUMMONER overrides

- Preserve ObliqueCamera's .72 ground Y projection and upright sprite height. The
  upstream “top-down”, 256×256 cells and 5×2 sheet are examples, not game requirements.
- Inspect the current manifest before authoring. The existing sample uses 64×96
  frames, pivot (32,90), world pixel scale 1.08 and metadata-defined clip order.
- Keep IDs, saves, map coordinates, collision, actor sorting and navigation intact.
- Mirror directions only if asymmetric gear, handedness and lighting remain correct;
  a horizontal flip alone is not proof of consistent directional illumination.
- Prefer transparent PNG output when supported. If keying a chroma background,
  verify alpha and edge spill; never treat a checkerboard as actual transparency.
- Normalize using anatomical body/feet anchors, not the lowest sword/VFX pixel.
  Do not rescale every frame to the same bounding-box height: crouches, recoil and
  lifted feet are intentional motion. Preserve contact-foot placement and root motion.
- Keep VFX separate from neutral anchors and use the engine's actual damage marker.
  Existing sample contacts are .22s basic and .30s skill; preserve gameplay timing
  unless the requested task explicitly changes it.
- Use clean pixel clusters and nearest-neighbor sampling. Upstream acceptance of
  “mixels” for prototypes does not relax this project's crisp pixel-art requirement.
- Use original project characters; reference examples guide process and are not
  replacement game assets. Keep the license when redistributing the reference pack.

## Before declaring completion

Check frame bounds, alpha, direction, unique poses, pivots, scale, loop seam and
contact timing. Review contact sheets/GIFs at gameplay size, then integrate through
the existing loader. Run relevant tests and the Android build for runtime changes.
Capture/inspect actual renderer output; identify desktop native captures versus
device screenshots. Do not invent FPS or claim a commercial art target is reached.
For documentation-only installation, check paths/archive integrity; no APK rebuild needed.
