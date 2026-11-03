---
name: eternal-arena-technical-artist
description: Improve ETERNAL SUMMONER's existing Kotlin/Compose 2.5D lighting, directionally correct shadows, restrained VFX, depth and atmosphere without overdraw or pixel blurring.
---

# TRAINING 05 — TECHNICAL ARTIST / LIGHTING / VFX

## Role and prerequisite
Improve an already coherent asset/world design, not disguise poor architectural geometry with glow. Check whether environment/architecture repairs are integrated first. Existing code already includes `SanctuaryDarkFantasyShaders.kt`, `SanctuaryEnvironmentShadows.kt`, `DepthPresentation.kt`, `SanctuaryWorldRenderer.kt`, lighting model `SanctuaryLight` and local light stamps. **Do not add a duplicate pipeline**.

## Baseline audit
Trace exact runtime draw order: far atmosphere → ground → shadows → Y-sorted sprites → foreground occluders → UI/post. Identify which supposedly available shader functions are truly called; never describe AGSL documentation string as active hardware RuntimeShader if not wired. Audit existing `DepthPresentation.Quality {OFF, REDUCED, BALANCED}` and cache behavior. Read real code rather than claiming each module always participates.

## Lighting craft
Primary key light: consistent cold moonlight from northwest/top-left; shadows fall opposite with height-aware projection. Amber windows/torches have small local spill on adjacent floor or door reveals; arcane portal light is coherent with dark portal void and existing animated ring. One clear point of attention; don't wash sky/foliage/buildings with identical neon rims. Use small, precomputed/cached stamps rather than fullscreen compositing or allocating dynamic gradients each frame. If dynamic lightning exists, make flashes rare, bounded and non-obstructive; no sudden intense screen-wide flashing.

## AO/shadow rules
Contact AO belongs tightly at foundation, root, hoof/foot. Cast shadow needs a silhouette/height, matching camera projective geometry; tree canopy shadows may have gaps according to alpha. Never substitute generic circles for architecture. Shadow receiver must be ground, not free sky or floating cliff; retain partial crown occlusion. Audit cache lifetime, WeakHashMap strategy and scene transitions. If modifying a shadow path algorithm, count scanline segments and benchmark CPU path complexity against baseline.

## VFX rules
Provide visual cause and legible gameplay effect. Combat hit marker, attack animation and VFX impact must agree; no huge particle bursts that obscure Master targets, enemy telegraphs or rescue markers. Spell glow falloff is bounded; sharp sprite silhouettes remain nearest neighbor. Quality tiers must omit high-cost decoration without changing hit timing or game state.

## Performance gates
Never add fullscreen bloom/RenderEffect/AGSL by default. Device target is Android; compositing costs must be measured, not assumed. First implement 1 representative region: Tower Gate portal + Summoning Hall courtyard OR one combat impact, and compare same camera/time, same zoom, same visible actor count. Existing `ScenePerformanceProbe` measures CPU timing/frame pacing, NOT GPU completion or heat. Without phone: `GPU/FPS/thermal NOT TESTED`. Do not claim optimization from a Robolectric PNG alone.

## Acceptance
No visible roof, wall or actor blurring at z=0.88; no double halo, pale flat-stone washout, wrong-direction shadow or missing portal ring; offscreen effects culled; CPU per-frame allocations not knowingly added; controls and objective markers readable. All shader changes must be shown OFF vs REDUCED vs BALANCED using repeatable evidence if active tiers are involved.

## Qualification drills
A: List actual call sites for chosen lighting helper and one function defined but unused. B: Show a fixed-camera A/B screen for an amber doorway. C: Compare shadow cast direction against SanctuaryLight. D: Prove `FilterQuality.None` maintained on foreground assets. E: Produce a performance caveat without inventing device FPS.

## Deliverable
Tight code changes, effect parameters/quality tier matrix, actual engine captures, cache/overdraw reasoning and tests, no unrelated architecture repaint.
