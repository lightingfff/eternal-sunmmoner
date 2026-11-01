# SANCTUARY BASELINE — Khóa bố cục đã duyệt

> Tài liệu này ghi lại tọa độ/kích thước/anchor/đường đi/camera thực tế trên `main` trước khi nâng cấp.
> Dùng để so sánh before/after — mọi đồ họa mới phải giữ footprint này.

## 1. Thông tin repo
- Branch gốc: `main` @ `5656bb3 Update main.yml`
- Engine: Kotlin + Jetpack Compose (Canvas 2D, không WebGL/PixiJS)
- Renderer duy nhất cho SANCTUARY_ASTRAL: `SanctuaryWorldRenderer.drawSanctuaryFloatingWorld`
- Delegation: `PixelSpriteRenderer.drawWorldBackground` → SanctuaryWorldRenderer khi `biome==SANCTUARY_ASTRAL`
- Asset pipeline: `PixelTileAtlas` (procedural 64px, FilterQuality.None) + `sanctuary_pack/` PNGs (hiện bypass, đã darken để fallback đồng bộ)
- Camera: `ObliqueCamera` OBLIQUE_Y_SCALE=0.72, GROUND_ELLIPSE_RATIO=0.66
- ViewModel: `SummonerViewModel` + `SanctuaryLobbyEngine` (nav graph 20 nodes, autonomous hero actors)

## 2. Hình dạng đảo
- **World size:** 3200 × 2760 px
- **8-vertex polygon** (clockwise, wall centerline):
  ```
  0: (980,340)   NW_TOP
  1: (2240,340)  NE_TOP
  2: (2900,880)  E_TOP
  3: (2900,1780) E_BOT
  4: (2320,2360) SE_CORNER
  5: (900,2360)  SW_CORNER
  6: (300,1780)  W_BOT
  7: (300,880)   W_TOP
  ```
- **Wall margin:** 120f, **clamp clearance:** 44f + entityRadius
- **Sky:** twilight indigo gradient 0xFF0D0F1A→0xFF181B2E→0xFF242742→0xFF2E314F + moon glow 0xFF8A9AB8 α0.08

## 3. Quảng trường trung tâm
- **Center:** (1940, 1240) **Radius:** 555
- Cấu trúc: stepped limestone curb 1E2028→3A3D4A, concentric rings 1.01/0.95/0.72/0.62/0.44/0.34/0.22, 16 radial joints 2A2D3A, 8-step SE staircase (2115,1690)→(2255,1575) với cheek balustrades DFDBD0
- Elemental runes: N Sakura (F472B6/FCE7F3), W Solar Gold (EAB308/FEF9C3), E Amber (F59E0B/FEF3C7), S Astral Azure (818CF8/E0E7FF) + pulse sin(time*2.4)

## 4. Khu vực
- **Training Sand Arena:** center (890,1790) rx 435 ry 270, organic wobble (sin*0.038+cos*0.024), outer rim 1.035×, lip 1.012×, core 0.992×; dirt path 935..1255,985..1285 với 3 lawn beds + hedge strips L-shaped
- **Residential Courtyard:** dirt P1 935,985→1255,1285, hedges 1125,1145→1238,1168 etc.
- **Southern Gate Platform:** 1380..1840,2210..2390, front ashlar wall drop 42*zoom, gate pedestals 1495 & 1725, wing stairs left 1385→1525/right 1695→1835, wood drawbridges 1455..1535 & 1685..1765 over chasm to 2605

## 5. Đường (PathSegments — 8 đoạn)
Giữ nguyên tuyến, chỉ đổi chất liệu thành cobble cold-grey với curb:
- Spine: (1560,390)→(1560,680)→(1330,880)→(1330,1340)→(560,1340) & (1330,1340)→(1610,2220)
- Curb: outer shadow 1E2028 7.5f, pale limestone 3A3D45 4.5f, cobble interior -2f
- Tile: 64px causewayTiles (6 variants, mortar 1B1E24, moss 2A332E)

## 6. Công trình (SANCTUARY_ZONES — 7 zones)
| Facility | center | interactRadius | color |
|----------|--------|---------------|-------|
| TOWER_GATE | (1960,635) | 120 | Gold |
| SYNTHESIS_LAB | (1270,585) | 100 | Violet |
| HERO_QUARTERS | (735,875) | 100 | Green |
| RESTAURANT | (1085,935) | 100 | Orange |
| HERO_REGISTRY | (785,1185) | 90 | Gold |
| TRAINING_GROUND | (890,1790) | 130 | Sand |
| SUMMONING_HALL | (2380,1930) | 140 | Blue |

Mỗi landmark có elevationZ riêng cho plaque (Gate 215, Hall 165, Dorms 142, v.v.), anchor foot-Y = obs.y, sortY = y+sortYBias.

## 7. Mở rộng đảo
- **East Celestial Garden:** beacon (2845,1265), bridge Y 1265, terrace center (3195,1265), expanded width 3460
- **North Astral Dock:** beacon (2565,615), dock center (2825,535)
- Locked = blueprint preview, unlocked = arched bridge + floating terrace (InteriorWorldRenderer)

## 8. Thực vật & Props (70+ obstacles)
- Trees: Oak Large, Pine Large, Mystic Plum, Sakura Large, Small Oak/Sakura, Cypress — mỗi cây có silhouette thân+nhánh+tán cụm, foot anchor, scale 1.0
- Bushes: Dense Green, Flowering Pink, Lavender/Rose clusters, wild grass/ferns
- Stone: Mossy Boulder, Ancient Monolith, Ruined Pillar, Obsidian Column
- Decor: Arcane Brazier (1490,2155 & 1730,2155 r90 warm), Street Lantern, Barrel Cluster, Wooden Bench 3/4, Terrace Grand Stairs, Stone Balustrade

## 9. Camera & Tương tác
- **Zoom clamp:** 0.36–1.48 (overview fit 0.27–0.54)
- **Pan:** panCameraWorld(worldDx=-pan.x/zoom, worldDy=-pan.y/(zoom*OBLIQUE_Y_SCALE))
- **Tap priority:** 1) Facility → prompt, 2) Expansion beacon → modal, 3) Hero/NPC → select, 4) Empty terrain → setTapDestination
- **Nearby check:** facility proximity → golden button, interior entry via E/Enter
- **Depth:** Y-sorted drawables (Prop sortY vs Hero y), southern foreground walls pass trên cùng, upper canopy occlusion pass
- **HUD:** top 7-9% slim bar (profile + 3 resource pills + 5 icon buttons), bottom-left camera suite, bottom-right hero command panel

## 10. Ảnh preview baseline
- `sanctuary_pack/preview_contact_sheet.png` background #0D0F1A (đã darken từ #121A22)
- So sánh zoom gần: cỏ thấy cụm lá+stone grit, đá có mortar+crack, mái có shingle lip+ridge, shadow bám chân+directional offset

## 11. Ràng buộc bất biến
- Không đổi world coordinates để vừa asset mới — asset mới phải fit footprint hiện tại, anchor foot-Y giữ
- Không di chuyển/xóa landmark, không đổi chiều rộng đường, không che nút tương tác
- Mọi shader/AO/shadow chỉ làm rõ depth, không làm tối toàn map

---
*Lưu: 2026-11-01 trên branch arena/35648dc2 từ main — dùng làm baseline before/after.*
