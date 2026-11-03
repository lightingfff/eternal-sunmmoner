---
name: eternal-arena-environment-architect
description: Repair ETERNAL SUMMONER's 2.5D Sanctuary architecture, roof-wall-door-ground cohesion, sprite anchors, and prop integration using native engine before/after evidence.
---

# TRAINING 04 — ENVIRONMENT / ARCHITECTURE ARTIST

## Mission
Deliver convincing authored dark-fantasy 2.5D architecture that belongs to the game world, not cutout stickers. Review previously merged P20 Armory, P21 premium_slice buildings, P22 flora and P23 architecture repair before touching art. The task could already be solved in newer main; don't regress it by regenerating art blindly.

## Geometry first
1. Study `ObliqueCamera`, `PixelSpriteRenderer.drawProp`, `ObliqueSpriteGeometry`, `PixelStructureAtlas`, `SanctuaryArtAssets`, `SanctuaryEnvironmentShadows` and current map footprints/doors.
2. Ground plane uses `OBLIQUE_Y_SCALE=0.72`; upright walls retain true height. Sprites may render with a scale factor that is not equal to map tile px; derive precise inverse projection from actual code and obstacle scale. Never assume a door pixel is at its gameplay trigger.
3. Inventory every facility: canvas dimensions, pivot, bottom opaque row, roof footprint/eaves, wall/side-face joins, door rectangle and world trigger, screenshot coordinates, control files/hashes. P21 facility inventory in `docs/art/P21_ARCHITECTURE_FACILITIES.md` is background, not a replacement for live code.

## Architecture QA: never allow these defects
- **Floating roof**: transparent pixels at wall/ridge/eave boundary; roof sits like an unattached lid; side face lacks common perspective; ridge/eave unsupported. Fix with layered roof structure, continuous fascia, soffit, load-bearing posts, consistent vanishing family and obscured wall underside.
- **Sticker door**: doorway lies flat on front texture, no wall thickness, lintel or reveal; trim drawn outside building projection. Fix by cutting dark recess, side jambs, upper shadow, threshold attached to wall foundation, light trapped behind frame. The art door must land on actual gameplay trigger with verified inverse mapping.
- **Floating building**: no stone footing, contact shadow at wrong pixel, foundation appears above grass, front face clipped. Add fitted footing/contact rim and silhouette-aware shadow, not one giant generic oval.
- **Awkward facade**: windows/door face point a different direction from roof plane; projection inconsistent with building footprint; roof dormer not actually connected.
- **Color mismatch**: bleached stone/white roof in dark green night, overbright door, four different facilities look like generic cottage recolors at zoom 0.36.

## Repair methodology
Fix one *facility slice* first: choose severe defect from real runtime imagery, derive the local sprite → world mapping and structural drawing order, apply deterministic offline PNG edits or asset generator edits. Preserve PNG dimensions, data manifest entries, binary alpha as relevant, no new per-frame bitmap decode. Use `assetSafe` PNG-first and safe fallback. Keep real-depth sorting and canopy occlusion. Do NOT move collision/WorldObstacle/door gameplay triggers to cover an art error without explicit approval; prefer re-authoring painted entrance where contract permits. P20 Armory historical door misalignment requires fresh measurement and explicit scoped change.

## Render proof
Capture matching camera snapshots before and after in *actual* Compose/Robolectric game renderer, not just offline contact sheet. Required zoom 0.36 (world read), 0.88 (gameplay), 1.00 (scale), 1.48 (joins) and detail/full facade camera that includes entire roof; actor at painted door + behind/above appropriate object. Verify no gaps pixel-by-pixel at junctions and no occlusion regression. Contact sheet is supplementary, not pass evidence.

## Pass/fail rubric
Every relevant door visually and mechanically aligns within measured threshold derived from sprite/world scale; no visible transparent gaps roof/wall/foundation at 0.88 or 1.48; at least two distinct roof/front/side volume planes; building's role identifiable at 0.36/0.88; art remains sharp; shadow projects from correct contact geometry; map and colliders unchanged. Any functional door mismatch blocks pass.

## Qualification drills
A: Compute a real door pixel→world coordinate in a current asset. B: Explain how to fix roof/wall gap without introducing extra runtime layer. C: Render building complete at 1.48 without cropping; inspect before/after. D: Show actor walk-to-door and correct trigger activation. E: Validate manifest/hash and missing-file fallback.

## Deliverable
Source PNG + deterministic generator when used, manifest, tests and engine evidence, per-building defect inventory and severity, exact unchanged map contracts.
