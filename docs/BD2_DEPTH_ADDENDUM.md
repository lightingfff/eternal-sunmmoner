# Depth addendum — verified slice

Continues Kaelen / FIELD_GOBLIN_RAIDER / NW smithy from `a5483ef`.
No map coordinates, footprints, navigation, controls, HeroRecord or save format changed.
The camera still foreshortens ground Y by .72 and preserves upright sprite height.

- Two original PNG atlases now contain 64 distinct poses each in four directions:
  idle6, walk8, attack8, skill9, hit4, defeat6, rest1, calm6, alert4, fear6, victory6.
  Hair/cloth, articulated steps, grip, scar/bone details and collapse poses are authored.
  Contact remains .22s / .30s, total attack .48s / skill .72s; pivots remain (32,90).
- `PremiumActorAtlas` reads authored order/loops/emotions and releases source sheets.
  Actual retreat/low HP selects fear; target awareness selects alert; victory selects salute.
- `DepthPresentation` formalizes nine render layers. Only FAR_BACKDROP permits focus:
  six unchanged decorative anchors use cached original art and a radius-one 24% filter.
  Terrain, characters, foreground and UI never pass through that filter; no full-screen buffer,
  AGSL or GPU post-process dependency. OFF/REDUCED/BALANCED are internal test controls.
- Static caster geometry/contact footprints are weakly cached. Canopy penumbra differs from
  root contact; silhouette direction/length use shared live world lighting. Tower shadows
  use the same projection, clipped to its ground bounds. Actor-local canopy overlap replaces
  the blanket redraw, retaining normal Y-sorted building/trunk occlusion.
- Three cached light stamps replace recurring radial gradients. Window/flame halos use
  sprite anchors; spill follows the ground. Repeated blanket fog/vignette/chasm layers and
  duplicate generic landmark shadows were removed; existing weather and world time remain.

Evidence: [before](visual-evidence/bd2-depth/before/overview.png),
[after](visual-evidence/bd2-depth/overview.png),
[emotions](visual-evidence/bd2-depth/kaelen_emotions_normal_zoom.png),
[skill](visual-evidence/bd2-depth/skill.gif). These are native Android Canvas renders under
Robolectric, not phone screenshots/recordings. Full focus pairs and zoom/occlusion sequences
are generated in `app/build/visual_screenshots/depth_addendum`; collect with
`python3 tools/collect_depth_evidence.py`. The before images were recaptured from untouched
`a5483ef` in an isolated worktree, with the same foreground-wall pass and camera anchors.
Tests assert distinct poses, exact contact frames, fixed anchors, focus isolation, occlusion,
warm geometry reuse, save round-trips and existing responsive UI behavior.
Android debug build and 44 distinct targeted tests passed (43 in the combined run,
plus the added portal regression; updated emotion evidence also passed).
The focus comparison changed 3,177 far pixels and zero opaque gameplay/actor/UI pixels.
Portal core retained 2,529 blue pixels with zero changes from either foreground pass.

Performance: no device/emulator is attached, so FPS/frame-time/jank/RAM/GC before and after
are **unmeasured**. Source changes remove recurring island paths, glow gradients and caster
metadata work. Additional animation pixels cost ~12 MiB cached for both actors versus
~4.5 MiB before, excluding object/GPU overhead; temporary full sheets are released.
Presentation textures add ~219 KiB by dimensions, not a measured process memory value.
Next: profile this same scene/save on a real phone before rolling the pattern to other actors.
Remaining: commercial-reference art quality, physical-device verification, and full material
relighting (large authored planes still mirror; fixed-direction accents cover material edges).
