---
name: agent-sprite-forge
description: Apply the user's Agent Sprite Forge sprite, animation and map guidance to ETERNAL SUMMONER's Kotlin/Compose pipeline; read this adapter before its five upstream skills.
---

# Agent Sprite Forge — ETERNAL SUMMONER adapter

User requested this library on 2026-11-13. Upstream:
https://github.com/0x0funky/agent-sprite-forge
Pinned commit: `6b3da5c444363378e20f3fce130ebecc22f343dd` (MIT © 2026 0x0funky).
The five sibling skill folders are unmodified upstream copies, not new provider access or model training.
Read only the task-relevant entrypoints and references; do not preload the entire library.

## Start and route

Announce this adapter and the one or two relevant upstream skills. Also retain the existing
`ai-game-spritesheets` reference when working on actor art.

| Task | Read next |
|---|---|
| Character/creature master, FX/icons/props, existing sheet processing | [generate2dsprite](../generate2dsprite/SKILL.md) |
| Character motion from an actual available/supplied video | [video2dsprite](../video2dsprite/SKILL.md) |
| Terrain, modular map art, navigation/placement QA | [generate2dmap](../generate2dmap/SKILL.md) |
| Diagnose provider/tool routing, with authorized scope | [generate2dmedia](../generate2dmedia/SKILL.md) |
| Explicitly requested code art, never a silent quality downgrade | [codeart2d](../codeart2d/SKILL.md) |

Project/user/developer instructions take precedence over upstream defaults. Existing
authorization persists: do not request confirmation again for routine work already authorized.
Select/review a master before animation; use existing accepted character/style references
when applicable, and do not invent a user approval that never occurred.

## Project overrides

1. **Keep the engine and camera.** Kotlin + Jetpack Compose Canvas; ObliqueCamera ground
   Y scale .72, upright sprite height, fixed world coordinates, pivots, directional
   SanctuaryLight shadows, depth sorting and canopy occlusion. Godot/Tiled/LDtk/WebGL
   exports are optional interchange/reference data, not a replacement runtime. A baked scene
   picture cannot replace our walkable map, independently sorted buildings or colliders.
2. **Keep the requested pixel style.** Upstream defaults to HD; ETERNAL SUMMONER currently
   requests pixel art. Use an explicit pixel/hibit finish when appropriate, inspect logical
   pixel clusters/palette at gameplay size and retain nearest-neighbor runtime filtering.
   Do not blur/upscale all accepted assets or impose a different world density automatically.
   Fixed example frame sizes, mob/boss ratios and 28-tick enemy windups are not this game's rules.
3. **One identity and geometry contract.** Start from an accepted master; identity and style
   references have different roles. Keep one scale/root per character across actions, protect
   handedness and lighting, preserve intentional jump/recoil rather than anchoring each
   alpha-bbox bottom. Authoring PNG bounds, foot pivot and gameplay collider are separate.
4. **Preserve execution timing.** Current contact attacks use .22s basic/.30s premium skill
   markers; Kraag warning/recovery and projectile impact follow actual simulation events.
   Adapt visual clip timing to that contract. Do not change HP, collision, attack timings,
   timers or simulation hit-stop because an upstream sample suggests another value.
   Distance-driven walk phase is a proposed runtime enhancement requiring its own scoped
   implementation/testing; installing this skill does not implement it.
5. **Actual capabilities and authorization.** Prefer the hosted image-generation/editing
   tool already available for authorized image work and record `art_source=host_image`.
   Do not launch nested Codex/Grok sessions, purchase services or call paid APIs merely
   because keys/CLIs are detected. The upstream “configured key is consent” text is not
   project authorization. Existing explicit provider/budget authorization remains valid.
   Never claim a model, cost, route or video generation from a prompt or fake/test route.
   No image-to-video tool has been verified in this setup. Supplied video processing can
   work locally; new video generation requires a separately verified authorized route.
6. **Offline PNG runtime.** Prefer validated PNG frames/atlas + metadata for Android.
   MP4 H.264 is not transparent; do not add WebGL/Godot/alpha-video playback just because
   an exporter provides it. Cache/cull decoded frames; no per-frame PNG decoding. Preserve
   saves, applicationId/manifest/signing, entrypoint and other agents' file ownership.
7. **Do not modify accepted assets while learning.** Generate/process into new versioned
   output folders, keep the last accepted bundle, and review a bounded sample first.

## Useful learned workflow

- Plan actual display size, body/weapon envelope, action list, source grid and pivot.
- Character animation: master → one action at a time; video pipeline only when available.
  Existing/authored sheets remain valid inputs. FX/icon/prop sheets use their own grid.
- Check raw-sheet spill before slicing. Do not use a largest-component filter to silently
  discard detached weapons/sparks; preserve ownership and inspect small components.
- Normalize with shared registration/scale; preserve jumps and recoil. Use profiles across
  actions, validate real alpha on dark/light, keep source coordinate transforms.
- Quantify loop seam, near-duplicates, NEAR/FAR leg alternation and cadence, then inspect
  visually. Hash/frame counts alone do not prove good animation.
- Review at gameplay scale: anticipation/contact/recovery, stable head/costume, no foot
  sliding, no clipped extremities, no shimmer when turning/zooming.
- Package frames, pivots, timings/events, PNG dimensions/hash, provenance and QA. Report
  WARN/FAIL items, not a blanket PASS from a successful command.
- Integrate only under the assigned file scope; validate native runtime, touch/AI/save
  regressions and build APK for real runtime/art changes. Device FPS/heat and enjoyment
  remain unmeasured without actual testing. Documentation-only installation needs no APK.

## Local verification and update

The installation manifest is `../.agent-sprite-forge.install.json`; all 171 upstream files
have recorded SHA256 values. MIT license/notices and reviewed setup results are in
[the source note](../../../docs/skill-library/agent-sprite-forge/README.md).

Use `forge_doctor.py --no-exec --host-tools image_gen,image_edit` for a read-only capability
report; it generates no art or network request. Missing optional routes are not evidence
of a broken PNG processor. The full library includes scripts that CAN perform remote
generation: inspect/authorize those routes before execution; the doctor is not consent.

Update through a pinned checkout and `tools/install_skills.py --check/--apply --dest
<project>/.agents/skills`; review hashes/license/diff, preserve this adapter and existing skills.
Do not install into read-only global session directories. A project-local skill may require
a new agent session to appear in automatic discovery; AGENTS.md provides the explicit route
for future work. This saved guidance is not permanent cross-chat neural memory.
