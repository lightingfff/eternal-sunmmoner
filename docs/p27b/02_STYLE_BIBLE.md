# P27B / 02 — style lock, **rules not copied pixels**

Nguồn quyết định: (a) engine thực ở SHA baseline (`docs/p27b/BASELINE.md`), (b) 8 ảnh tham khảo người dùng cung cấp chỉ để rút ngôn ngữ, (c) original sprites trong `sanctuary_p03/`, `premium_slice/` như các mẫu kích thước/lighting. Không đưa ảnh tham khảo, watermark, nhân vật, chữ, logo hay bố cục của chúng vào APK/repo. Một ảnh thị trấn nhiều mái vật liệu/đường lát gợi ý nhịp vật liệu **không** là lệnh đổi layout. Một ảnh sanctuary lớn có cây + đá trắng gợi ý thang sáng/tối **không** là lệnh vẽ lại bản đồ. UI warning gợi ý phân cấp + nút rõ **không** là lệnh trace viền/text.

`docs/p27b/previews/STYLEBOARD_FROM_EXISTING_ASSETS.png` là board/QA từ asset gốc của game, **KHÔNG** phải game asset hay screenshot engine. `docs/p27b/previews/before|after/` dành riêng ảnh từ renderer/test thật.

## STYLE_LOCK_ENVIRONMENT
| Thành phần | Luật sản xuất | Phép thử |
|---|---|---|
| Projection | `ObliqueCamera.OBLIQUE_Y_SCALE=0.72f`; tile nguồn là mặt đất vuông 64×64, renderer tự foreshorten Y, **không** skew/double-iso PNG | đường road union không răng cưa ở .36/.88/1.0; đứng đúng foot |
| Grass | 70% nền yên, 20% cụm cỏ/khối lá 2–6px, ≤10% hoa/sỏi; **P27B 16 ô trong torus 4×4**, P03 còn 8 fallback. Có chu kỳ 4 ô lộ ở 5×5: `GAME_CANDIDATE`, không gọi seamless READY | 3×3 + 5×5 + zoom .36/.88; không checkerboard, không nhiều nhiễu 1px |
| Soil | đất cát nén ấm, không vẽ bậc/tường giả ở trung tâm training ground; P27B 16 ô, P03 6 fallback | training clip intact, heroes không như đi xuyên vật cản vẽ giả |
| Flagstone | đá vôi xám sáng/lạnh nhẹ, grout vừa đủ + mẻ cạnh theo mặt; P27B 16 ô, P03 8 fallback; `summon_ivory` 16 ô **chỉ trong zone riêng** (grade từ cùng limestone source) | ring Summon rõ hơn plaza nhưng không cháy noon/night |
| Road | cobble P27B 16 ô, P03 6 fallback; rãnh liên tục; chi tiết theo khối đá thay vì per-pixel noise | đường nối rẽ không vuông seam; clip path network nguyên vẹn |
| Light | `SanctuaryLight` upper-left (P21 đã ghi azimuth 142°, elevation 48°), top plane sáng hơn front, side tối hơn 15–25% tương đối; night vẫn đọc volume | cùng camera 12h CLEAR / 0h CLEAR, không tô full-screen white |

Swatches **điểm khởi đầu để tune bằng ảnh engine**, không phải filter overlay: grass shadow `#354A33`, mid `#557044`, sun `#78905B`; soil `#8A7055`/`#B09471`; warm limestone `#E8E5DC`, upper lit `#FAF9F3`, carving `#687B8D`, grout `#97A2A6`; cobble mid `#78848A`, depth `#485966`; wood `#755641`/`#A06D50`; cyan glow `#74D6EE` với alpha thấp. Họ tile 64×64 nguyên; không downscale nguồn 512 đầy tiểu tiết rồi gọi crisp. Preview lặp không phải renderer.

## STYLE_LOCK_ARCHITECTURE
| Luật | Áp dụng / giới hạn |
|---|---|
| Mái/mặt trước/mặt hông | Sprite đứng 3/4 riêng, không áp nghiêng Y thêm lần 2; cạnh mái có ridge/eave/soffit và bóng đè tường; hông lùi tối tạo thể tích |
| Tường/cửa | stone courses theo cấu trúc, không noise; cửa là **hốc** tối có lintel/jamb/step bám nền, không decal dán; vàng chỉ trong cửa sổ/metal |
| Hall | Canvas cũ **268×206**, anchor centre foot `(134,206)` vẽ bottom-centered (`H−3` cho world door approximate); khu triệu hồi khác biệt bằng limestone `#F5F1E7`, steel-slate `#364B60`, carved shadow `#AAB9C7`, gold `#C7A36C` và small rune teal `#2DA9C5`. Tâm rune/hoạt động P21 `(111,176)` không được trôi khỏi zone `(2380,1930)` |
| Tower Gate | Canvas cũ **260×224**; engine rift oval tại `(130,120)`, bóng cast theo PNG. Duy trì opening ở vị trí đó, ngưỡng bậc bám foot. Cổng sáng hơn sandstone cũ nhưng void vẫn tối thật; no giant aqua rectangle |
| Houses | Existing: Quarters 200×168, Restaurant 206×172, Registry 188×166, Lab 188×168, Armory 190×166; **không** gán sprite V3 untracked vào facility khi door/pivot chưa QA. Ở .36 vẫn nhận ra silhouette của từng vai trò |
| Alpha/pivot | Structure RGBA trong suốt ở ngoài, canvas giữ đúng W×H nếu swap 1:1. Chỉ đổi bằng pad/crop khi kiểm engine cùng camera; collision, coordinates, triggers không lấy từ PNG bbox |

## STYLE_LOCK_UI
| Token dự kiến | Sáng/Summon | World/khác |
|---|---|---|
| `Surface.Ivory` / `Surface.WorldGlass` | `#F5F1E7` ivory với một lớp khung | slate rgba gần `#172637` alpha ~.72; xem map qua |
| `Border.Soft` / `Border.Focus` | `#C7A36C` thin frame / `#2DA9C5` focus | muted bronze + cyan selected |
| `Text.PrimaryDark` / `Text.SecondaryDark` | `#283B4B` / `#506474` | **không** dùng gold/silver nhạt trên ivory |
| `Text.PrimaryLight` / `Text.SecondaryLight` | N/A | `#F4EFE6` / `#C1CCD4` |
| `Accent.Azure` / `Accent.Gold` | `#2D8EAC` CTA on ivory, `#A3793E` nhỏ | cyan/glowing rune, antique gold nhỏ |
| Typography/spacing | header 20–24sp, panel 16–18sp, body 13–15sp, meta 11–12sp; spacing 4/8/12/16/24dp; panel radius 12–16dp, chip 8–10dp; touch >=48dp. | Responsive portrait/landscape, max width <=760dp; text thật Compose; test tags và click remain |

## 10 anti-pattern (ảnh ví dụ nguồn **game hiện có** hoặc test cần tạo; không copy ảnh reference)
1. Phủ trắng cả màn để "ivory" → sai; đối chiếu `before/summon_noon_z148` với **after**: chỉ zone Hall sáng. Chưa có AFTER thì đây là test rule, không ảnh chứng minh.
2. Trắng phẳng che vân đá → sai; xem close-up Hall `summon_noon_z148` sau engine.
3. Chỉ một đường sprite cho cả roof/front/side → sai; kiểm P23 hall trước và candidate tại .88/1.48.
4. Cửa dán sáng không hốc → sai; zoom gate `gate_noon_z148` trước/sau.
5. Rift oval nổi lệch khỏi vòm → sai; `gate_noon_z148` và PNG local `(130,120)`.
6. Seam cỏ/cobble không jump RGB nhưng lặp hàng → sai; preview 5×5 sau script kiểm thị giác.
7. Ô cỏ một biến thể sáng chói giữa 8 ô → sai; family contact 5×5 và pixel stats.
8. Đá ngói downsample bị mờ → sai; 1x/2x nearest và renderer .88/1.48.
9. Chữ gold trên nền ivory → sai; `ui/summon_modal_land_800` kiểm contrast, chữ dark mới đúng.
10. Slab HUD quá cao che road → sai; `ui/hud_land_800` trước/sau, giữ camera/test tags.

Đây là **checklist kiểm lỗi**, không khẳng định ảnh xấu đã được chụp ở mọi mục. Board/thumbnail của tile trước khi có AFTER là **ART CONTACT SHEET**, không phải camera screenshot.

## 5 cặp expected vs bad (dùng chấm visual từ screenshot thật)
| Kiểm | Expected | Bad/fail |
|---|---|---|
| Summon noon | mặt đá ấm sáng, cột/hốc sâu, grass/gate không đổi toàn cục | full-screen trắng/bệt, cổng mất tối |
| Summon night | mái/đá đọc được nhưng sky vẫn tối, rune tập trung | cả sân neon cháy, sky bị làm sáng |
| Road junction | cobble liên tục qua giao nhau, curb clip sạch | seam vuông/rãnh đứt ở góc tile |
| Gate | 3 mặt rõ, khe void ở `(130,120)` và bậc chạm nền | flat facade hoặc oval glow lấn cột |
| Modal summon | tiêu đề dark trên ivory, cost 20/40 và CTA rõ 2 viewport | gold trên trắng hoặc CTA mất khỏi màn hình |

Không dùng LoRA/weights, không tuyên bố provider QA có điểm số. Nếu image tool sinh checkerboard giả, script tách alpha chỉ là kỹ thuật, phải soi viền từng sprite; status `GAME_CANDIDATE` cho đến khi renderer chứng minh.
