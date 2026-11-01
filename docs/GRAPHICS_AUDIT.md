# ETERNAL SUMMONER — Graphics Audit (2026-11-01)

> Phase 1 audit trên branch `arena/a133794d-eternal-sunmmoner` (main@a2412fc sau PR #2 đã merge 7 commits graphics+stability). Kiểm tra trực tiếp engine, texture, renderer, lighting, shader, camera, animation, Android path.

## 1. Hệ thống đồ họa hiện tại hoạt động như thế nào

| Thành phần | Triển khai hiện tại | Vị trí file | Đánh giá |
|---|---|---|---|
| **Engine** | Kotlin + Jetpack Compose Canvas2D, `FilterQuality.None` nearest-neighbor, 64px procedural `PixelTileAtlas` × `tiledDraw` clip polygon | `PixelTileAtlas.kt` (915L), `SanctuaryWorldRenderer.kt` (1189L) | Ổn định, không WebGL/AGSL — mọi shader mô phỏng bằng layered draw |
| **Camera** | `ObliqueCamera` `OBLIQUE_Y_SCALE=0.72`, `worldToScreenX/Y`, clamp `wallMargin 120f + radius 44f`, zoom 0.36–1.48, pan `worldDx=-pan.x/zoom` | `WorldMapModels.kt` | Giữ silhouette 2.5D rõ, không kéo giãn |
| **World layout** | 8-vertex polygon 3200×2760, plaza 1940,1240 r555 (stepped curb + 16 radial joints + 8-step SE stair), 8 PathSegments spine 1560,390→1610,2220, 7 SANCTUARY_ZONES, 70+ `WorldObstacle` foot-Y + sortYBias | `WorldMapModels.kt`, `SanctuaryLobbyScreen.kt` | Bất biến, đã khóa baseline |
| **Ground** | `sanctuaryGrassTiles 8v` (base 2B352E + 11 moss patches + 26 slate grit + 135 specks 7-tone + 15 tufts AO + 6 clover + lavender/rose + leaf/mushroom), `sanctuarySandTiles 6v` (7 earth patches + 65 grit + scuff/puddle), `sanctuaryStoneTiles 8v` (cold ashlar 3A3F4A/444A56 + crack/chip/lichen/chisel + rune) | `PixelTileAtlas.kt:410-620` | Premium, đã qua 2 lần overhaul |
| **Path** | `sanctuaryCausewayTiles 6v` (8×8px cobble 3E424D/4B4F5C/383C46/525663/2F333E, mortar 1B1E24, moss 2A332E, wear) + curb 4v + curb shadow 1E2028 7.5f + pale 3A3D4A 4.5f | `PixelTileAtlas.kt:620-700` | Phân biệt rõ, seamless hash |
| **Interior** | `tavernWoodPlankTiles 6v` (8 boards + 6 grain/knot + wear scuff + nail + AO), `tavernFlagstoneAisleTiles 4v` (beveledSlab + chip/moss) | `PixelTileAtlas.kt:750-820` | Đã nâng Part IV |
| **Water** | Không có water body trong Sanctuary (floating sky-island, chasm void 0B0D1A + depth overlay 1A1F33 α0.22 + cliff strata) | `SanctuaryWorldRenderer:520-560` | Thiếu bề mặt nước phản chiếu |
| **Vegetation** | `PixelStructureAtlas` 15 props: Oak/Plum/Pine/Sakura/Cypress/Hedge/Bush/Boulder/Monolith/Brazier/Lantern... Oak có buttress roots+A0, bark furrow+highlight, 6 leaf clusters crownHi 6B8A5A; Pine 5-tier pine; Cypress snow tip 8A9AB8; Plum bloom FAE8FF; Sakura 6 clusters; Hedge AO+lavender | `PixelStructureAtlas.kt:857L` | Đã nâng Part II/IV, còn thiếu secondary branches rõ, cast shadow hướng, root nổi chi tiết |
| **Architecture** | `PixelSanctuaryLandmarks` 9 landmarks: TowerGate 260×224 obsidian portal, SummoningHall 268×206 amphitheatre 9 columns, HalfTimber 2-plane (sideW 56% + gable + sloped roof + chimney brick coursing + garden AO shrubs), Townhouse/Cottage/Arch etc. | `PixelSanctuaryLandmarks.kt:611L` | Đã nâng Part III (plaster speck, timber grain, chimney coursing, stone wall quoin chamfer) |
| **Material** | `materialLightFactor(stone 0.55+0.45*NdotL, wood 0.5+0.5*NdotL, foliage 0.6+0.4*NdotL, metal 0.45+0.55*NdotL²)` + base/ambient/light/shadow per-material via shader uniforms | `SanctuaryDarkFantasyShaders.kt:55-70` | Có nhưng chưa bảng roughness/highlight chi tiết |
| **Lighting** | Directional moonlight 0.35,-0.45,0.82 cold 9AA8C8, ambient dark 0D0F1A/shadow 1A1C24, local brazier FFB844 r90 + plaza 7DD3FC r160 + window FFC86B r70 pulse sin1.8 radialGradient, vignette 2-pass | `SanctuaryWorldRenderer:1120-1189`, `SanctuaryDarkFantasyShaders.kt:90-180` | Đủ ambient+local, thiếu bloom, occlusion awareness, flicker tự nhiên |
| **Shadows/AO** | Contact oval 0.65 + offset 0A0C14 0.35 height-scaled, AO strip under N wall 0.22, plaza rim 0.18, gate platform 0.20 | `SanctuaryDarkFantasyShaders.kt:105-140`, `SanctuaryWorldRenderer:1080` | Thiếu cast shadow hướng, canopy/building shadow, AO khe đá |
| **Fog/Depth** | `drawGroundFog` (vertical 242742 α0.04→1A1F33 0.09→0D0F1A 0.14 drift sin0.6*8), `drawChasmDepth` radial 0B0814 0.55→181B2E 0.22, moon rays 8A9AB8 α0.10 diagonal | `SanctuaryDarkFantasyShaders.kt:150-200`, `SanctuaryWorldRenderer:560-700` | 2 lớp, thiếu background haze/midground/local mist/distant fade phân cấp |
| **Dithering/Outline** | `applyCrispOutline(0B0814)` 1px material dark, chưa dithering cho shadow transition | `PixelCanvas.kt:260-280` | Thiếu dithering có chủ đích |
| **2.5D** | Layer 13: sky→cloud→cliff→grass→sand→garden→roads→plaza→AO→gate→walls→expansions→moon rays→local lights→foreground walls→canopy occlusion; Y-sort `prop sortY = y+sortYBias` vs hero y, southern foreground walls pass | `SanctuaryWorldRenderer` + `PixelSpriteRenderer:drawWorldBackground` | Đúng thứ tự, chưa lỗi chồng |
| **Animation** | `timeSec` drift sin0.22*28 cloud, sin0.9*8 isle bob, sin2.4 plaza pulse, sin1.8 brazier pulse, fire 3-frame, sand scuff static | `SanctuaryWorldRenderer` | Thiếu leaf sway, dust, water ripple, flicker variation |
| **Camera/Zoom** | pinch-to-zoom 0.36–1.48, clamp 0.27–0.54 overview, no blur NearestNeighbor, no bleeding | `SanctuaryLobbyScreen.kt` | Ổn |
| **UI** | Weathered stone + muted gold trim, dark translucent panels — chưa audit chi tiết | `ui/screens/*`, `ui/theme/*` | Cần kiểm nhất quán Dark Fantasy |
| **Android** | AGP 9.1.1, Kotlin 2.2.10, compileSdk 36.1, minSdk24 target36, `google-services.json` placeholder offline, `FirebaseAvailability` guard, 50 tiles ~800KB, 60fps mid-zoom | `gradle/*`, `FirebaseAvailability.kt`, `MainActivity.kt` | Đã fix crash, chưa Quality Settings |

## 2. Vấn đề tồn tại vs yêu cầu Premium (Phần III-XI)

| Yêu cầu Master | Hiện trạng | Khoảng cách |
|---|---|---|
| **Ground detail** | Grass đã premium nhưng gravel texture chưa có; moss trên đá vs trên đất chưa phân biệt rõ; dirt wear chỉ 3/8 variant | Thiếu gravel tile, moss differentiation, wear đa dạng hơn |
| **Path** | Seamless nhưng mép vẫn hơi vuông, thiếu wear ở giao nhau/góc, chưa có rêu đa tông | Cần soft edge + corner wear |
| **Water** | Không có water tile — chasm void không có reflection/gợn sóng | Cần base water color + depth + ripple + reflection (dù nhỏ, để đáp ứng task) |
| **Tree** | Tán nhiều lớp nhưng secondary branches chưa lộ, highlight chưa tách 3 vùng (sáng/trung/tối), cast shadow chỉ oval, thiếu rễ nổi/rêu/cây khô | Cần layered highlight + directional cast shadow + prop bổ sung |
| **Structure** | Stone/wood/metal đã phân biệt nhưng roof chưa dày (chưa có tiles thickness + shadow), window chưa bloom, thiếu carved symbols/magical patterns | Cần roof tiles volume + window bloom + ornaments |
| **Material** | Có light factor nhưng chưa bảng roughness/highlight per-material rõ | Cần material table Base/Ambient/Light/Shadow/Roughness/HL |
| **Ambient light** | Có base/sky/ground/structure/shadow nhưng wall base tối chưa đủ, plaza rim AO mờ | Cần fine-tune AO strength per-region |
| **Local light** | Radial falloff có nhưng chưa occlusion awareness, flicker đơn sin1.8 hơi máy móc | Cần soft edge + intensity variation tự nhiên |
| **AO** | Chỉ 3 vùng, thiếu khe đá, gốc cây, mái hiên | Cần AO khe đá + chân nhà + tán cây |
| **Cast shadow** | Chỉ contact oval, thiếu tree canopy/building directional | Cần directional offset shadow với soft edge |
| **Bloom** | Chưa có — lửa/rune/cửa sổ chưa lan sáng | Cần bloom nhẹ giữ core sharp |
| **Fog** | 2 lớp, chưa 4 lớp background/midground/local/distinct + distance fade | Cần thêm haze layers |
| **Dithering** | Chưa có | Cần dithering pattern cho stone/ground/shadow/water |
| **Outline** | 1px 0B0814 đồng nhất, chưa material dark | Cần material edge shading/rim |
| **2.5D depth** | Đã có layer nhưng chưa foliage volume layers tách sáng | Cần foliage volume highlight phân lớp |
| **Animation** | Cloud/isle/plaza/brazier có nhưng lá rung đồng bộ, thiếu dust/falling leaves/bloom flicker | Cần leaf offset + particles |
| **Camera** | Ổn, nhưng texture bleeding chưa test 1.48 chi tiết | Cần verify 3 mức zoom |
| **UI** | Chưa audit — có thể lệch tone | Cần weathered stone + muted gold + readable |
| **Performance** | Đã tối ưu nhưng chưa Quality Low/Med/High thực chất | Cần settings thực |

## 3. Thành phần cần nâng cấp (ưu tiên 1→7 theo Master)

1. **Texture & asset quality** (P0): water tile mới + gravel + moss diff + tree secondary branches + roof volume
2. **Material definition** (P1): material table + per-material roughness/HL
3. **Lighting & shadow** (P1): AO mở rộng + directional cast shadow + bloom
4. **Shader & atmospheric** (P2): fog 4 lớp + dithering + outline material
5. **2.5D depth** (P2): foliage volume layers + depth sorting verify
6. **Animation & env** (P3): leaf sway offset + brazier flicker + dust
7. **Android perf** (P3): Quality Settings Low/Med/High thực

## 4. Phần giữ nguyên (không động)

- **Layout**: 8-vertex polygon, plaza r555, 8 PathSegments, 7 SANCTUARY_ZONES, obstacle footprint/anchor/interaction, nav graph 20 nodes, camera clamp 44f — đã khóa `SANCTUARY_BASELINE.md`, tuyệt đối không đổi.
- **Engine**: Kotlin Canvas2D + FilterQuality.None + Oblique 0.72 — không đổi OpenGL/Vulkan.
- **Controls**: pinch/pan/tap priority facility→beacon→hero→terrain, zoom 0.36–1.48.
- **Collision**: `CollisionPhysics.resolvePosition` rectangular+circular + 8-sided wall normals.
- **Firebase guard**: `FirebaseAvailability` offline placeholder — giữ.

## 5. Rủi ro khi thay đổi

| Rủi ro | Giảm thiểu |
|---|---|
| Atlas footprint đổi → collision/tile lệch | Chỉ đổi pixel nội tile 64², giữ `TILE_PX` và world coords |
| Shadow/bloom che đường/tương tác | AO α0.18–0.22 chỉ khe, bloom core sharp + outer α0.12–0.18, test tap tại plaza/gate 0.36/0.72/1.48 |
| Fog quá dày che map | 4 lớp α0.04–0.09–0.14, không che gate/plaza, toggle Quality |
| Performance drop 60→30fps | Bloom/fog 2 gradients/frame, dithering pre-baked vào tile, không per-frame alloc, cache lazy |
| Skill MCP offline (scenario-textures/game-assets/patina...) | Dùng procedural `PixelCanvas` placeholder + ghi rõ tool gaps trong pipeline |
| Outline quá dày nuốt pixel | 1px 0B0814, chỉ material edge shading, rim highlight 3D5A36 α0.45 |

## 6. Cách kiểm chứng từng thay đổi

- **Visual**: build `assembleDebug` → `adb install` → mở Sanctuary → screenshot 0.36/0.72/1.48 so sánh before/after `docs/SANCTUARY_BASELINE.md` + 3×3 tile seam test
- **Functional**: tap plaza/gate/sand arena/hero, pan/zoom, Y-sort foreground walls/canopy, không che interaction
- **Perf**: `adb shell dumpsys gfxinfo` 60fps, `logcat` no `IllegalStateException`, memory tiles ~800KB, no ANR `SanctuaryAssetManager` fallback
- **Android**: API 24–36, minSdk24, compileSdk36.1, test offline (placeholder) + online (real google-services.json)

## 7. Kế hoạch triển khai chi tiết (Phase 2→7)

- **Phase 2 Asset Pipeline** (đã có `GRAPHICS_OVERHAUL_PIPELINE.md`): bổ sung water tile `createSanctuaryWaterTile`, gravel variants, tree secondary branches prop
- **Phase 3 Material/Renderer**: bảng material 7 nhóm (stone/wood/metal/grass/moss/water/magic) + `drawStoneWall34`/`drawOrganicLeafCluster` volume
- **Phase 4 Lighting/Shader**: AO mở rộng (khe đá/chân nhà/tán cây), directional cast shadow (0A0C14 α0.35 down-right 6px*zoom), bloom nhẹ (rune/brazier/window core sharp outer 0.10–0.18), fog 4 lớp, dithering pattern, outline material
- **Phase 5 2.5D**: foliage volume highlight phân lớp, depth sorting verify, oblique 0.72 giữ
- **Phase 6 Sanctuary Integration**: áp dụng vào `SanctuaryWorldRenderer` thực tế (không map thử nghiệm), verify 70+ props
- **Phase 7 Android Testing**: build + install + zoom/pan/interaction/lighting/texture/smoothness/crash log

---
*Audit thực hiện trên main@a2412fc — basis cho đợt nâng cấp tiếp theo trên arena/a133794d.*
