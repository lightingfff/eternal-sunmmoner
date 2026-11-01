# ETERNAL SUMMONER — DARK FANTASY 2.5D GRAPHICS TRANSFORMATION

## Nguyên tắc bất di bất dịch
**Bố cục Sanctuary đã được khóa cứng.** Mọi tọa độ WorldMap (POLYGON_VERTICES, SANCTUARY_ZONES, PathSegments, WorldObstacles) được giữ nguyên 100%. Chỉ thay đổi cách thế giới **được thể hiện** (palette, texture, material, lighting, shadow, shader), không thay đổi cách thế giới **được bố trí**.

Đặt 2 ảnh trước/sau cạnh nhau: cùng hình dạng đảo 8-cạnh, cùng quảng trường tròn tâm (1940,1240) bán kính 555, cùng tuyến đường cobble, cùng vị trí Tower Gate (1960,635), Summoning Hall (2430,2005), Hero Registry, Dorms, Armory, Synthesis Lab, Training Grounds, v.v. Chỉ chất lượng nghệ thuật nâng cấp.

---

## 1. Kiểm tra Repository thực tế (Part I)
- **Renderer thực tế**: `SanctuaryWorldRenderer.drawSanctuaryFloatingWorld` là nơi duy nhất vẽ Sanctuary ngoại thất (floating island, cliff, grass, roads, plaza, walls, bridges, expansions). `PixelSpriteRenderer.drawWorldBackground` chỉ delegate sang nó khi `biome == SANCTUARY_ASTRAL`.
- **Phát hiện quan trọng**: Tiles thủ công trong `app/src/main/assets/sanctuary_pack/` (extract từ `B__t_i_nguy_n_Pixel_Art_...png` via `extract_sanctuary_pack.py`) **không được dùng** bởi SanctuaryWorldRenderer — SanctuaryWorldRenderer chỉ dùng `PixelTileAtlas.sanctuaryGrassTiles / sanctuarySandTiles / sanctuaryCausewayTiles` sinh procedural bằng `PixelCanvas`. Đây là lý do phải nâng cấp **PixelTileAtlas** chứ không chỉ thay PNG.
- **Camera**: `ObliqueCamera` với `OBLIQUE_Y_SCALE=0.72`, `worldToScreenX/Y` quy đổi tọa độ thế giới -> màn hình, hỗ trợ zoom 0.26–1.52, anchor foot-Y cho depth sorting Y-sorted.
- **Depth ordering**: `LobbyDrawableItem.Prop` sortY = `obs.y + sortYBias`, hero sortY = `actor.y`, vẽ theo thứ tự tăng dần + upper-canopy pass cho tán cây che sau thân.

## 2. Palette Dark Fantasy Thống Nhất
### Trời & Khí quyển
- Cũ: cerulean sáng `#539CE0→76B9F2→A5D7FC→CBE9FE`, mây trắng 0.85–0.96 alpha, nắng vàng 30% warm.
- Mới: twilight indigo `#0D0F1A→181B2E→242742→2E314F`, mây thấp `#2A2E4A` 0.32–0.45 alpha, moon glow `#8A9AB8` 0.06–0.08, moonbeams lạnh `#1A8A9AB8` thay vì warm yellow.

### Mặt đất
- Cỏ: `69BD4F` neon → `#2B352E` moss-stone desaturated. 9 patch clusters `#25302A..334739`, earth exposure `#2E2A24`, stone grit `#3A3F45`, blade shadow `#162118` mid `#344E35` highlight `#4A6B3E`, hoa hiếm `#8A7A97` lavender / `#9A6570` dried rose.
- Đường: cobble `#B6B3AD` → `#3E424D/#383C46/#525663`, mortar `#1B1E24` near-black, moss in cracks `#2A332E`, curb `#3A3D45` outer / `#3E424D` inner.
- Plaza: alabaster `#FFF3F0EA` → ancient runic `#3A3D4A/#4A4D5E/#2E3040`, joints `#2A2D3A`, rings `#1E2028/#3A3D4A`, steps `#4A4D5E/#3A3D4A`.

### Vách & Tường
- Cliff: `#7B6756` warm brown → `#2E303A/#262830` cold slate, chasm `#3E3A46`→`#13131A`, rim `#69BD4F`→`#2F4A2E`.
- Fortress walls: `#C2B7A1` limestone → `#3A3F4A` cold stone, walkway `#EAE2CE`→`#444A5A`, merlons `#F0E8D6`→`#52566A`.

### Kiến trúc
- Half-timbered: plaster `#F4ECDC`→`#3A3F4A`, timber `#432A1B`→`#1B120E`, slate roof `#2A3B52`→`#1E2430`, terracotta `#472C1B`→`#2A1B14`.
- Fortress tower: `#EAE1CF...` → `#4A4E5E/#3A3F4A/#2F333E/#1E2028`.

### Thực vật
- Sakura: `#BE185D/EC4899/F9A8D4/FCE7F3` → `#5A2540/7A3358/9A6570/B88A95` dusky plum.
- Oak: `#122C36/215736/488F3E/9FD64B` → `#0F1A1F/1A2E1F/2F4A2E/3D5A36`.
- Pine tiers: `#102A38/1B4954/2D706E/63B084` → `#0C1A22/132A2E/1B3A2E/2F4A2E`.

Mỗi vật liệu có màu gốc riêng, không phủ tint tím toàn màn. Dark qua substance, không qua giảm brightness.

## 3. Xây lại Terrain (Phần IV)
- **Grass**: 64×64 seamless, 110 pixel specks mỗi tile với 9 patch ellipses, 2 dirt exposures (variant%3), 18 stone grit, 12 clumped tufts với AO base, rare desaturated flowers (variant 2,5,7), leaf litter (1,6).
- **Roads**: 6 curbed cobble variants với beveledSlab + mortar dark, staggered 8 rows×8px, moss in cracks (1/5 stones), age cracks.
- **Plaza**: concentric alabaster rings → cold stone, 16 radial joints `#2A2D3A`, stepped curb `#1E2028/#3A3D4A`, SE 8-step staircase `#4A4D5E/#3A3D4A`, cheek balustrades `#DFDBD0` giữ.
- **Rìa đảo**: grass overhang rim shadow `#1B2A20` + bright `#2F4A2E`, stroke `#1A2520`.

## 4. 2.5D Thực sự (Phần V)
- Mái có ridge cap + sideDepth bevel, eave beam + shadow lip, roof plane trapezoid với `topHalfSpan/bottomHalfSpan`.
- Tường có front + sideDepth return wall với shaded side, corner quoins, eave shadow 3px, foundation plinth 5px.
- Foot anchor: `WorldObstacle.y` là front baseline, `footprintDepthNorth` xác định occlusion depth, painter’s sortY = y + sortYBias.
- Southern foreground walls vẽ sau Y-sorted pass để hero đứng sau tường → bị che, đi trước → hiện trên.

## 5. Ánh sáng & Shader (Phần VI–IX)
### Hướng sáng tổng thể
- Chọn **top-left directional** (lightDir 0.35, -0.45, 0.82), moonlight cool `#9AA8C8`, ambient `#0D0F1A`. Tất cả mái, đá, cây highlight cùng hướng, không random per-asset.

### Shadow
- **Contact shadow** (AO): dense small oval trực tiếp dưới chân `Black α0.65`, scaleByHeight cho tower.
- **Directional shadow**: offset down-right cold `#0A0C14 α0.35`, larger softer, pixel edge không blur mạnh.
- Implemented via `drawDirectionalShadow34` (2 layers) và `drawSanctuaryAmbientOcclusion` (wall base strip, plaza outer ring AO, gate platform oval).

### Ambient Occlusion
- Wall base long strip `#0A0C14 α0.22`, plaza outer ring `#0A0C14 α0.18`, southern gate oval `α0.20`. Chỉ vùng giao nhau, không đều toàn map.

### Local lights
- Brazier amber `#FFB844 α0.18 pulse 0.85-1.0` ×2 tại (1490,2155) & (1730,2155) 90px radius.
- Plaza rune cold `#7DD3FC α0.12` 160px radius.
- Window warm `#FFC86B α0.10` 70px từ mỗi cottage cluster (995,655 etc).
- Tất cả dùng `Brush.radialGradient` offscreen → compositing, không white/yellow circle đơn.

### Shader
- File `SanctuaryDarkFantasyShaders.kt` cung cấp AGSL source `SANCTUARY_AGSL` (shadowMap, aoMap, NdotL), vật liệu light factor, vignette, contact/local light helpers. **Đã tích hợp**: `SanctuaryWorldRenderer` gọi `with(SanctuaryDarkFantasyShaders){ drawDarkFantasyVignette() }` mỗi frame → file shader không nằm im.

## 6. Môi trường (Phần X)
- Particles: 22 warm sakura motes `α0.25-0.88` → 14 muted embers `α0.08-0.35`, colors `#8A7A97/#6B7A8A/#9A6B3A` drifting slow 9-14px/s, không che texture.
- Vignette: vertical + horizontal black 0.10-0.18 alpha focus plaza/gate.
- Static textures cached via `lazy` ImageBitmap, chỉ particles animate.

## 7. Asset Pipeline & Preservation (Phần XI-XII)
- **Không single background**: Mỗi cây, nhà, đường, tường là object riêng với ID, anchor, footprint, collision. Không rasterize toàn map.
- **Asset Manager**: `SanctuaryAssetManager` lazy cache, nhưng sanctuary procedural tiles quan trọng hơn. PNG pack vẫn được darken via ImageMagick (`-modulate 48-58,55-68 -fill "#2E3544" -colorize 12-20`) để fallback đồng bộ.
- **Thứ tự**: terrain → walls/cliff → trees → buildings → shading/shadow → local light → atmosphere (đã thực hiện).
- **Performance**: tile lazy 8/6 variants, FilterQuality.None nearest-neighbor, culling via screenToWorld bounds, static cliff/grass cached, no per-frame asset reload.

## 8. So sánh trước/sau
- Zoom thường: nhận ra ngay cùng đường, quảng trường, công trình nhưng tone từ “vườn hoạt hình sunny” → “căn cứ cổ đại đá-gỗ-ma thuật” với stone cold, gỗ dark walnut, metal muted.
- Zoom gần: grass thấy cụm lá + stone grit, đá có mortar + crack, mái có shingle course lip + ridge, cây có trunk bark furrow + multi-cluster canopy, shadow bám chân + directional offset.

## 9. Tiêu chuẩn đánh giá
- Không chỉ đổi background color (đã đổi từng tile generator + wall mesh).
- Không vẽ khối cũ (đã thay PixelCanvas generators).
- Shader đã integrated (Shaders.kt + Renderer calls).
- Shadow không chỉ ellipse mờ (2-layer contact+directional + AO map).
- Asset không orphan (mọi building/tree có propKind + footprint, PNG pack darkened).
- 2.5D có side depth, thickness, occlusion (paintAnimeHalfTimberHouse sideDepth, drawStoneWall34 sideDepth, terrace walls sideDepthPx).

## 10. Cách kiểm tra (không cần build Android vì thiếu JVM)
- Mở `app/src/main/java/com/example/game/pixel/SanctuaryWorldRenderer.kt` dòng 52-64: sky gradient 4 deep indigo.
- `PixelTileAtlas.kt` dòng 289-383: grass dark fantasy generator với comment.
- `SanctuaryDarkFantasyShaders.kt`: AGSL string + vignette active.
- `git diff app/src/main/assets/sanctuary_pack/preview_contact_sheet.png`: contact sheet background `#0D0F1A` thay vì `#121A22`, toàn bộ assets tối hơn.

--
*Layout preserved. Art reborn.*
