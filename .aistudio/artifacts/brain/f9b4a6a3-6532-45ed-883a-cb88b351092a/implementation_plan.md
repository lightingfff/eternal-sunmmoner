# ETERNAL SUMMONER — Sanctuary Hybrid Asset Extraction & Implementation Plan (Revised)

Extract and implement the complete Sanctuary Pixel Art Asset Pack from the uploaded composite reference image (`B__t_i_nguy_n_Pixel_Art_Th_nh___a_Tri_u_H_i_c6ed3c87.png`) using a **Hybrid Extraction & Recreation Workflow**, generate an organized PNG library with JSON manifest and preview contact sheet, and integrate all assets across 4 staged phases into the live Android game environment.

## User Review & Critical Decisions

> [!IMPORTANT] Confirmed Architecture & Workflow Rules
> - **Hybrid Extraction Strategy**: Cleanly extract separated objects with transparent backgrounds. Where items overlap or contain embedded text/frames, recreate pristine original pixel-art PNGs guided strictly by the reference sheet.
> - **Asset Quality Standards**: No text labels, no black backgrounds, no unwanted borders, no overlapping crops, crisp nearest-neighbor pixel scaling, and true 3/4 oblique perspective.
> - **Staged Implementation Pipeline**: Execute rollout across 4 stages (Stage 1: Ground & Roads -> Stage 2: Vegetation & Props -> Stage 3: Walls, Bridges & Buildings -> Stage 4: Live Map Integration).

---

## 1. Overview & Core Concept

- **What It Does**: Processes the composite reference asset sheet (`B__t_i_nguy_n_Pixel_Art_Th_nh___a_Tri_u_H_i_c6ed3c87.png`) into a structured asset pack containing 64x64 ground/road tiles, variable-sized trees, props, waterfalls, and 3/4 sanctuary buildings. Generates a manifest `sanctuary_assets_manifest.json` and a contact sheet image (`preview_contact_sheet.png`), then integrates the assets into `SanctuaryLobbyScreen` via `SanctuaryAssetManager`.

---

## 2. User Experience & Staged Asset Library

### Complete Asset Categories:
- **A. Ground Tileset (64x64)**: Grass variations, stone plaza slabs, dirt, mossy stone, garden ground, water tiles, and seamless terrain transitions.
- **B. Road Tileset (64x64)**: Horizontal/vertical straights, inner/outer corners, T-junctions, cross intersections, dead ends, grass-to-road transitions, and decorative stone borders.
- **C. Vegetation Pack**: Sakura cherry blossom trees, ancient oaks, spirit pines, bushes, flower clusters, individual blooms, ferns, and moss.
- **D. Environment Props**: Rocks, stone walls, fences, grand stairs, stone bridges, tiered waterfalls, street lanterns, and arcane pillars.
- **E. Sanctuary Architecture**: Main Gate, Central Plaza structures, Dorms, Training Hall, Summoning Hall, Armory, Synthesis Chamber, Workshop, and Flying Dock.

---

## 3. Key Technical Decisions & Staged Execution Strategy

### Stage 1: Ground & Seamless Road Tileset
- Write Python slicing script (`extract_sanctuary_pack.py`) using PIL to crop 64x64 ground tiles and seamless 9-slice road tiles into `/app/src/main/assets/sanctuary_pack/tiles/`.
- Validate seamless edge alignment and transparent corner masks.

### Stage 2: Vegetation, Flowers, Rocks & Props
- Extract large sakura trees, oak trees, bushes, flowers, rocks, lanterns, and fences with transparent backgrounds.
- Recreate any obscured props as clean, un-occluded 3/4 pixel-art PNGs matching reference dimensions.

### Stage 3: Architecture, Walls, Waterfalls & Preview Contact Sheet
- Extract/recreate facility buildings (Synthesis Chamber, Main Gate, Summoning Hall, Dorms, Training Hall, Flying Dock), cliff waterfalls, bridges, and walls.
- Generate `preview_contact_sheet.png` displaying all extracted assets alongside `sanctuary_assets_manifest.json` with actual pixel dimensions, anchor points, and collision footprints.

### Stage 4: Android Game Engine Integration
- Implement `SanctuaryAssetManager.kt` to load assets from assets folder into Compose `ImageBitmap` caches.
- Update `PixelSpriteRenderer.kt` and `TowerFloorCatalog.kt` to render extracted ground/road tiles in the floor pass and Y-sorted vegetation, props, and buildings in the object pass.

---

## 4. Technical Architecture & System Mapping

```
┌────────────────────────────────────────────────────────────────────────┐
│                   Stage 1-3: Hybrid Extraction & Review                │
│                                                                        │
│  /public/assets/aistudio/.../B__t_i_nguy_n_Pixel_Art_...c6ed3c87.png   │
│                                  │                                     │
│                     [extract_sanctuary_pack.py]                        │
│                                  │                                     │
│     ┌────────────────────────────┼────────────────────────────┐        │
│     ▼                            ▼                            ▼        │
│  64x64 Tiles & Roads      Vegetation, Props & Buildings   preview_contact_sheet.png
│  (/assets/sanctuary_pack/) (/assets/sanctuary_pack/)      & manifest.json
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼─────────────────────────────────────┐
│             Stage 4: Android Runtime Integration & Testing              │
│                                                                        │
│                         SanctuaryAssetManager                          │
│                      (Cached Asset Loader & Manifest)                  │
│                                  │                                     │
│        ┌─────────────────────────┼─────────────────────────┐           │
│        ▼                         ▼                         ▼           │
│  PixelTileAtlas           PixelStructureAtlas        PixelSpriteRenderer
│  (64x64 Ground/Roads)     (Buildings & Props)        (Y-Sorted Render) 
│                                  │                                     │
│                                  ▼                                     │
│                      SanctuaryLobbyScreen (Canvas)                     │
└────────────────────────────────────────────────────────────────────────┘
```

### Verification & Testing Plan
1. **Compilation**: Run `compile_applet` after creating asset manager and renderer classes.
2. **Unit & Robolectric Tests**: Run `gradle :app:testDebugUnitTest` to verify tile manifest parsing, bitmap loading, asset dimensions, and responsive layout safety.
