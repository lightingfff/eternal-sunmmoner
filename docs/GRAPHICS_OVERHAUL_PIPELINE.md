# ETERNAL SUMMONER — Complete Dark Fantasy 2.5D Graphics Overhaul Pipeline

> Master plan 2026-11-01 cho yêu cầu: "Complete Dark Fantasy 2.5D Graphics Overhaul + Android Stability"
> Mọi tài sản mới giữ footprint SANCTUARY_BASELINE (8-vertex polygon, plaza 1940,1240 r555, 8 paths, 7 zones, 70+ props) — không đổi world coordinates.
> Tuân thủ Scenario Skills đọc verbatim từ `/tmp/arena-coding-uploads/` → `docs/SCENARIO_ART_PIPELINE.md`.

## 1. Mục tiêu & ràng buộc bất biến

- **Giữ layout:** 8-vertex wall 980,340→2900,1780 etc., world 3200×2760, plaza stepped curb + 16 radial joints + 8-step SE staircase, 8 PathSegments spine 1560,390→1610,2220, 7 SANCTUARY_ZONES centers/radii, obstacle foot-Y anchors, camera 0.36–1.48.
- **Không đổi engine:** Kotlin Canvas2D FilterQuality.None, 64px tiles × tiledDraw, ObliqueCamera OBLIQUE_Y_SCALE 0.72, Y-sorted drawables + foreground walls pass.
- **Scenario offline:** sandbox này không kết nối MCP `model_run/asset_get` — asset tạm procedural `PixelTileAtlas/PixelStructureAtlas/PixelSanctuaryLandmarks` + ImageMagick darken pack; khi có MCP sẽ thay bằng `model_scenario-texture` / `model_scenario-game-assets` theo §3.
- **Android stability song song:** Firebase guard, google-services.json placeholder, logcat evidence, perf budget (§8).

## 2. Kiến trúc 2.5D hiện tại (baseline duyệt)

```
SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
 ├─ Sky twilight gradient 0D0F1A→181B2E→242742→2E314F + moon glow 8A9AB8 α0.08
 ├─ Chasm void 0B0814 + depth haze
 ├─ Ground: tiledDraw sanctuaryGrassTiles (8 vars) clip polygon, sandTiles (6) ellipse 890,1790 rx435 ry270, stoneTiles (8) plaza
 ├─ Roads: PathSegments causewayTiles (6) + curbTiles (4) + curb shadows
 ├─ Vegetation: 70+ obstacles — Oak/Pine/Mystic Plum/Sakura/Cypress/Bush/Boulder/Monolith/Pillar/Brazier/Lantern...
 ├─ Structures: PixelSanctuaryLandmarks 7 facilities + PixelStructureAtlas fortress/wall
 ├─ Shaders: SanctuaryDarkFantasyShaders — directional moonlight 0.35,-0.45,0.82, contact shadow oval α0.65 + offset 0A0C14 α0.35,
 │           AO 0.18–0.22 chân tường, brazier amber FFB844 r90 + plaza rune 7DD3FC r160 + window FFC86B r70 pulse sin*1.8
 └─ Foreground walls + canopy occlusion (upper Z)
```

## 3. Pipeline Scenario chuẩn (từ skills verbatim)

### 3.1 Texture tiles (scenario-textures)
```
recommend(prompt nguyên văn, size 1024² power-of-two)
→ model_schema_get(model_scenario-texture) — đọc width/height enum, prompt limit, overlap/featherRadius/eraseSeam
→ dry_run=true (cost check)
→ model_run(wait=false) wave ≤ concurrency ceiling
→ jobs_wait(pending_job_ids) — không poll job_get, không retry model_run khi timeout
→ asset_display → asset_download (png giữ alpha)
→ verify tiling: test 3×3 assembled map kiểm joins
→ upscale: CHỈ model_sc-upscale-flux-texture (precise/balanced/creative + strength), không generic upscaler
→ PBR (scenario-patina-retexture): model_patina → 5 maps (albedo/normal/smoothness/metallic/height), map qua metadata.type
```

### 3.2 Isometric building / prop (scenario-game-assets)
- Grid trước style: 2:1 diamond, footprint ≠ silhouette, test 3×3 occlusion, Wang simple contract → autotile thực tế.
- Variation batch: crop sát bounds, strip baked shadow/glow, nine-slice stretchable ghi trong prompt, verify alpha bounding box.
- Masked fill: đọc polarity & base-image từ schema, white-inside → adapt alpha.

### 3.3 Sprite animation (scenario-sprite-animation)
- Seam check `delta_seam ≤ mean_baseline_delta` window, loại frame 0 nếu diverge.
- Grid pipeline: video-to-image-seq → subset → grid-maker → image-slicer (6×6 max) → image-seq-to-video (fps 1–120, GIF).
- Shared palette/canvas per character, pixel snap re-detect grid + quantize.

### 3.4 Tài nguyên thất bại phải kiểm (scenario-image §3)
Size step 16 / prompt 2000–32000 / reference array cap — off-step bị reject hoặc thay thế im lặng, string trần khi cần array bị drop.

### 3.5 Thay thế offline tạm
| Cần | Tạm procedural | Khi có MCP |
|-----|----------------|------------|
| Grass/Sand/Stone/Cobble tiles | PixelTileAtlas 64px beveledSlab + grit + tufts + flowers | model_scenario-texture "seamless cold moss forest floor, desaturated ancient stone, tileable" 1024² → upscale 2× |
| Wall/Fortress/Roof | PixelStructureAtlas beveledSlab + drawTerracottaRoof/halfTimber | model_scenario-game-assets isometric 2:1 fortress kit, Wang edges |
| Tree/Sakura/Bush/Brazier | PixelSanctuaryLandmarks silhouette + layers | game-assets variation batch + transparent BG detection |
| Albedo→PBR | — | scenario-patina 5-map PBR cho stone/wood/metal |
| QA | Visual 3×3 + zoom 1.48 pixel shimmer test | asset_analyze batch 10 + detect (background strip false cho whole-map) |

## 4. Giai đoạn overhaul (Part I–VII, mỗi part 1 commit có preview)

### Part I — Ground (lớn nhất, 70% màn hình) [→ đang làm]
- **Grass 8 vars → 8 vars v2 premium:** 5-layer depth — deep earth 1A1E18 base, moss shadow 25302A, midMoss 2A3D2B, mutedMid 344E35, mossLight 3D5A36, mossHi 4A6B3E (never neon). 9 organic ellipse patches (moss/dirt/stone), 18 slate pebbles, 110 speck foliage 6-tone, 12 wind-bent tufts w/ AO hLine base 1A1E1B + 4-px warm stem, rare dusty lavender 8A7A97 / dried rose 9A6570 flowers (vars 2,5,7), fallen leaf litter 5A4630 (vars 1,6). Seamless-safe (no detail <8px from border, tufts 6–58 inner).
- **Dirt path 5 vars:** compacted 5C4E3A earth-brown base, 5 ellipse variation patches, 55 grit pebbles, scuff arcs variant 1/4, moss transition edge.
- **Quality gate:** 3×3 tile test no seam, zoom 1.48 crisp, color sampled cold green 2B352E not emerald, leaf/moss visible but sparse.

### Part II — Path & Plaza
- **Cobble 6 vars v2:** 8-row 8px causeway, 5-tone cold grey palette 3E424D/4B4F5C/383C46/525663/2F333E, hi 5E636F sh 252830, beveledSlab per cobble (mortar 1B1E24 near-black), moss in 1/5 cracks 2A332E, age cracks hLine 1E2026.
- **Plaza stone 8 vars v2:** 16px limestone blocks 3A3F4A/444A56/353A45/40454F, hi 5A5F6E sh 23262E, beveledSlab + vertical crack 1/7, rune scratch 6B7A9A→7CA8C8 on vars 3/7.
- **Curb 4 vars:** limestone 3A3D45/383C46 hi 525663 sh 1E2026, moss dots 2A332E.
- **Astrolabe medallion 224×152:** stepped limestone curb 1E2028→3A3D4A, concentric rings 1.01/0.95/0.72/0.62/0.44/0.34/0.22, 16 radial joints 2A2D3A, elemental rune pulses + SE staircase 2115,1690→2255,1575.

### Part III — Water & Chasm
- Hiện void 0B0814 + depth haze; nâng cấp: chasm gradient 0B0814→181B2E, subtle wave lerp sin(time*0.6) qua SanctuaryDarkFantasyShaders, shore foam 3A3F4A α0.12 at wall foot, bridge shadow volume.

### Part IV — Vegetation (70+ props)
- **Oak/Pine/Mystic Plum/Sakura/Cypress:** silhouette thân nhánh + 3-layer foliage cluster (shadow 162118, mid 2A4A2B, hi 4A6B3E / sakura burst F2AEBE→FCE7F3, plum 6B3A5A), AO blob Black α0.22 under trunk, wind sway sin sway 0.02.
- **Bushes:** Dense Green 3-tone, Flowering Pink/Lavender/Rose clusters dusty, wild grass/ferns tufts matching ground palette.
- **Stone:** Mossy Boulder 2A332E moss caps, Ancient Monolith runes 6B7A9A, Ruined Pillar crack moss.

### Part V — Structure per-facility (giữ footprint/coords, chỉ nâng chi tiết)
| Facility | Center | Nâng cấp chính |
|----------|--------|----------------|
| TOWER_GATE 1960,635 r120 | Crenellated ashlar wall merlon 8A9AB8 moonlight hi, gate arch C4A882 limestone voussoir, portcullis iron 3A3F4A, banner burgundy 7A3358 |
| SYNTHESIS_LAB 1270,585 r100 | Alchemist tower stepped slate roof 3A3F4A, half-timber plaster C4A482/timber 3B231A, glowing retorts F59E0B |
| HERO_QUARTERS 735,875 r100 | Long dormer row, timber 593728 hi, chimney smoke α0.08 |
| RESTAURANT 1085,935 r100 | Tavern interior tavernWoodPlank 8C5A38 oak + flagstone 8C877B, hearth glow |
| HERO_REGISTRY 785,1185 r90 | Ledger hall limestone colonnade, brass plaque 4A3A2E |
| TRAINING_GROUND 890,1790 r130 | Sand arena rim 1.035× + lip 1.012×, target dummies, weapon racks |
| SUMMONING_HALL 2380,1930 r140 | Astral spire E0E7FF runes, void glass 2A2E4A, rift beam |

### Part VI — Material & Shadow
- Contact shadow oval Black α0.65 + directional 0A0C14 α0.35 down-right, pillar/wall AO 0.18–0.22 khe đá, shingle lip 2B151D 1px, ridge gold D69A43→ shingleHi BF4B4C→ warm 9E383F→ mid 7D2A33→ dark 4E1B24.
- Interior wood grain 5px hLine 6E4328/A66D46 + nail 4A2C1A, stone mortar 1A1E24 near-black không beige.
- Pixel density: mọi facade giữ 64px base, consistent 1px outline 0B0814, FilterQuality.None nearest-neighbor.

### Part VII — Shader & Perf
- SanctuaryDarkFantasyShaders: moonlight dir 0.35,-0.45,0.82 cold 8A9AB8, brazier FFB844 r90 pulse sin1.8, plaza rune 7DD3FC r160, window FFC86B r70, volumetric fog 242742 α0.06 layer.
- Perf: tile lazy init (8+6+8+6+4+6+4+... = 50 bitmaps 64² ~ 800KB), draw call batch tiledDraw clip path, no per-frame alloc, shader time uniform only, zoom 0.36–1.48 LOD: overview skip tuft detail.

## 5. Phong cách Dark Fantasy Pixel (chuẩn asset)

- Palette lạnh: Deep Forest 2B352E, Moss Stone 2A3D2E, Cold Stone 3A3F4A, Charcoal 1B1E24, Smoky Violet 2A2E4A, Aged Bronze 4A3A2E, Burgundy 7A3358 — không vàng phủ đều, chỉ local warm brazier/window.
- Hướng sáng moonlight top-left, không tint toàn map; AO chỉ chân tường/khe đá.
- Pixel: outline 0B0814 1px, highlight cold 8A9AB8 (đã fix F7F2E7→8A9AB8), grain light/dark specks w*h/10, consistent density.
- Material structure: đá beveledSlab mortar+hi/shadow, gỗ plank seam 3B2112 + grain + nail, mái shingle 4-tone + ridge gold.

## 6. QA & Test mỗi part

- Visual: screenshot sanctuary_pack/preview_contact_sheet.png BG 0D0F1A, so sánh before/after 3×3 tile seams, zoom near (1.48) crisp pixel, zoom far (0.36) no shimmer.
- Functional: tap priority facility→beacon→hero→terrain, pathfinding 20 nav nodes, camera pan/zoom clamp, no interaction blocked by new decor.
- Android: logcat no crash, cold start <2s, frame 60fps mid-zoom, memory tiles ~800KB.

## 7. Tool gaps & cách xử lý

- Scenario MCP offline: ghi rõ procedural placeholder trong commit/CR, không claim asset Scenario.
- Không JVM local: không chạy assembleDebug, chỉ kiểm tĩnh + CI.
- Không gen 2048 trực tiếp (cap 3840, aliasing): gen 1024 → upscale 2× tiling-safe.
- Generic upscaler phá tiling: chỉ sc-upscale-flux-texture.
- Asset order/metadata.type không đoán: đọc sau download.

## 8. Android crash fix (đã xong, PR #2)

IllegalStateException Default FirebaseApp not initialized → FirebaseAvailability.isFirebaseReady() qua FirebaseApp.getApps() cache, MainActivity guarded flowOf null→offline guest, AuthHelper early-return, google-services.json placeholder eternal-summoner-offline, strings default_web_client_id offline-placeholder, offline banner 2A2E4A. Evidence: stack trace + 2 commits 75a9d70,59c844f. Tiếp: logcat verification, startup perf, graphics perf 0.36–1.48.

---
*Tham chiếu bắt buộc khi edit asset: nêu skill source & schema field đã đọc. Next: Part I commit ground detail upgrade → Part II path/plaza → ... Part VII shader/perf.*
