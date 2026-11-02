# P22 — BÁO CÁO KẾT QUẢ VEGETATION FORGE (`sanctuary_p03`)

> **Nhánh làm việc:** `arena/929b932f-eternal-sunmmoner` (kế thừa `main` @ `8cb16d8`, sau P21).
> **Pull Request:** [#25](https://github.com/lightingfff/eternal-sunmmoner/pull/25) — **KHÔNG tự động merge** theo đúng quy ước.
> **Kỹ năng nguồn (source skills) đã chọn & thích nghi:** `scenario-game-assets`, `scenario-textures`, `ai-game-spritesheets` (chỉ áp dụng làm quy chuẩn pixel-art/palette/anchor offline trên pipeline Kotlin/Compose `OBLIQUE_Y_SCALE = 0.72f`; không dùng Scenario MCP/API trả phí hay fine-tune mô hình).

---

## 1. Audit trước khi sửa (Phase A)

Trước khi ghi bất kỳ file ảnh nào, đã kiểm tra trực tiếp `TowerFloorCatalog.kt`, `PixelStructureAtlas.kt`, `SanctuaryArtAssets.kt`, `ObliqueSpriteGeometry.kt`, `SanctuaryEnvironmentShadows.kt`, `PixelSpriteRenderer.kt`, và `sanctuary_p03/manifest.json` (chi tiết đầy đủ tại `docs/art/P22_VEGETATION_INVENTORY.md`):

### Phân bố thực tế trong Sanctuary so với Tower Floor 1 & Floor 2
- **Sanctuary (`buildSanctuaryMap()`, 3200×2400):**
  - `PropKind.SAKURA_BLOSSOM_TREE`: **18 cây** (`sanctuary_sakura_0..17`), chia 9 cây chẵn `(id.hashCode() and 1) == 0` nạp `sanctuary_p03/canopy_a.png` (`114×132`) và 9 cây lẻ nạp `sanctuary_p03/canopy_b.png` (`96×114`).
  - `PropKind.GARDEN_HEDGE_BUSH`: **5 bụi** (`sanctuary_hedge_0..4`, `scale = 0.90f`) ở sân vườn Service Quarter — trước P22 đang vẽ bằng procedural `SanctuaryVegetation.buildGardenHedgeBush()` (`64×40`) với dải màu xanh tươi lệch hẳn tông đêm của P18/P21.
  - `PropKind.CYPRESS_CONIFER_TREE`: **3 cây** (`sanctuary_cypress_0..2`) nạp `sanctuary_p03/cypress.png` (`48×88`) — **control P20 đã khóa, giữ nguyên tuyệt đối**.
  - `PropKind.FLOWER_PLANTER_BED`: **7 bồn hoa đá** vẽ trực tiếp bằng Canvas curb — **giữ nguyên, không chạm**.
  - **Không có** `PINE_SPIRIT_TREE`, `MYSTIC_PLUM_TREE`, `OAK_TREE`, hay `FOREST_CANOPY_BUSH` nào trên map Sanctuary.
- **Tower Floor 1 (`FIRST_FIELD_MEADOW`) & Floor 2 (`ELDERWOOD_FOREST`):**
  - `PropKind.PINE_SPIRIT_TREE` (`sanctuary_p03/pine.png`, `96×128`): xuất hiện ở Floor 1 (`f1_spawn_grove_1(430,920)`, `f1_spawn_grove_5(790,1245)`, cộng vòng rìa procedural) và Floor 2.
  - `PropKind.MYSTIC_PLUM_TREE` (`sanctuary_p03/twisted.png`, `112×132`): xuất hiện ở vòng rìa rừng cổ thụ Floor 2 (`ELDERWOOD_FOREST`).
  - `PropKind.FOREST_CANOPY_BUSH`: xuất hiện ở Floor 1 & Floor 2 (ví dụ `(900,820)`, `(900,1400)`), trước P22 vẽ bằng procedural `buildForestCanopyBush()` (`64×40`).
  - `PropKind.OAK_TREE` (`sanctuary_p03/oak.png`, `112×136`): **control P18 đã khóa, giữ nguyên tuyệt đối**.

### Khiếm khuyết hình ảnh của art cũ (xác nhận trên 19 ảnh baseline trước khi sửa)
1. **Tán cây legacy (`canopy_a`, `canopy_b`, `pine`, `twisted` từ `generate_sanctuary_art.py`):** là các khối cầu/nón trơn rắc nhiễu hạt 1px (salt-and-pepper noise), thiếu cụm lá 3 sắc độ theo ánh trăng góc trên-trái, thiếu hốc tối tách cành, và phần thân/rễ tiếp đất mỏng khiến cây trông "dán nổi" cạnh nhà P20/P21 và nền cỏ P18 (`lum 37–108`).
2. **Độ chênh sáng màu đêm:** `canopy_a`/`canopy_b` cũ vọt sáng hồng phấn (`lum` tới `181`) trong khi mái nhà P20/P21 có `lum 22–73` và `cypress.png` control có `lum 22–107`.
3. **Bụi cây procedural (`GARDEN_HEDGE_BUSH`, `FOREST_CANOPY_BUSH`):** mảng bầu dục xanh phẳng chói trên sân vườn đêm của Khu Dịch Vụ và chiến trường Tầng 1.

---

## 2. Các loài cây / cảnh quan đã nâng cấp (Phase B & Phase C)

Tất cả 6 PNG được sinh offline, tất định (deterministic) bằng `tools/mapforge_p22_vegetation.py` (Python + Pillow), không đụng vào script cũ `tools/generate_sanctuary_art.py`:

| File tài sản (`sanctuary_p03/`) | Loài / `PropKind` | Pha | Kích thước | `alpha>=128` `bottom` (`pad`) | `drawFootOffset` | Tách tán che (`topFraction`) | Số màu (`alpha=255`) | Đặc trưng hình khối & chất liệu mới |
|---|---|---|---|---|---|---|---|---|
| `canopy_a.png` | Anh đào cổ thụ tán rộng (`SAKURA_BLOSSOM_TREE` chẵn) | Phase B (v4) | `114×132` | `126` (`pad=5`, giữ nguyên) | `10.0f` | `0.56f` (hàng `73`) | `18` | Tán ô rộng (`x_span=103`), thân chính vững chãi chẻ 3 nhánh lớn có khe hở nhìn xuyên nền gạch, cổ rễ loe bám cỏ kèm cụm cánh hoa rụng 2px, cụm cánh hoa xếp lớp ánh trăng trên-trái (`#c28496` -> `#190d1b`). |
| `canopy_b.png` | Anh đào dáng nghiêng gió (`SAKURA_BLOSSOM_TREE` lẻ) | Phase B (v4) | `96×114` | `109` (`pad=4`, giữ nguyên) | `8.0f` | `0.56f` (hàng `63`) | `16` | Thân mảnh uốn chữ S nhẹ với cành tạt trái thấp và vòm hoa phải cao (`IoU = 0.7627` so với `canopy_a`), xóa cảm giác lặp copy-paste trên 18 cây sakura. |
| `hedge_bush.png` | Bụi hàng rào tỉa (`GARDEN_HEDGE_BUSH`) | Phase B (v4) | `64×40` | `36` (`pad=3`, giữ nguyên) | `5.7f` | Không che trên đầu | `8` | Khối hàng rào boxwood 2.5D có mặt đỉnh đón trăng, mặt đứng sẫm tối, chân rêu bám đất và cụm 2px cánh anh đào vương trên đỉnh (`#b4788c`); nạp qua `assetSafe ?: buildGardenHedgeBush()`. |
| `pine.png` | Tùng linh tháp (`PINE_SPIRIT_TREE`) | Phase C | `96×128` | `122` (`pad=5`, giữ nguyên) | `10.0f` | `0.52f` (hàng `66`) | `15` | Đỉnh nhọn phân 7 tầng cành lá kim rủ răng cưa xuống tới `y=90`, hốc bóng tối tách tầng rõ ở cả `z=0.88` và `z=1.48`, vỏ tùng nâu ấm + rễ bạnh (`IoU = 0.4249` so với `canopy_a`). |
| `twisted.png` | Mận cổ thụ vặn xoắn (`MYSTIC_PLUM_TREE`) | Phase C | `112×132` | `126` (`pad=5`, giữ nguyên) | `10.25f` | `0.54f` (hàng `71`) | `14` | Thân già uốn chữ S rõ nét với gờ vỏ bắt trăng, các tầng tán ngang kiểu bonsai rừng sâu sắc tím chàm (`x_span=104`), khe cành thoáng đãng. |
| `forest_bush.png` | Bụi rậm rừng sâu (`FOREST_CANOPY_BUSH`) | Phase C | `64×40` | `36` (`pad=3`, giữ nguyên) | `6.0f` | Không che trên đầu | `10` | Bụi lá rừng tự nhiên bất đối xứng, lộ cành khô tối màu ở chân và điểm quả mọng rừng 2px (`#7889ad`); nạp qua `assetSafe ?: buildForestCanopyBush()`. |

**Hai tài sản đối chứng (control) được khóa nguyên vẹn byte-for-byte:**
- `app/src/main/assets/sanctuary_p03/oak.png` — SHA-256 `556eefd5c1b1f14872b67e6e651b8c7d6645e4632519d0719a6ce614b240a229` (**KHÔNG ĐỔI**).
- `app/src/main/assets/sanctuary_p03/cypress.png` — SHA-256 `442cbc617c6d61f79f2105f7c4b2072a2fc721fa3a4e56ee11d695d576cf988b` (**KHÔNG ĐỔI**).

---

## 3. Bằng chứng hình ảnh trước / sau

Toàn bộ ảnh đối chứng được lưu trong `docs/visual-evidence/p22/` và phân biệt minh bạch theo đúng loại nguồn kết xuất:

### A. `ART CONTACT SHEET` (mô-za-íc kiểm tra pixel offline 1x & 3x nearest-neighbor)
- `docs/visual-evidence/p22/p22_vegetation_contact_sheet_1x.png` — **ART CONTACT SHEET** (tỉ lệ gốc 1x của 6 tài sản P22 + 2 control `cypress_ctrl`, `oak_ctrl` trên nền cỏ đêm `#253429`).
- `docs/visual-evidence/p22/p22_vegetation_contact_sheet_3x.png` — **ART CONTACT SHEET** (phóng 3x nearest-neighbor kèm vạch chuẩn hàng chân `alpha>=128` màu vàng và hàng tách tán `topFraction` màu lam).

### B. `REAL ENGINE RENDER (Robolectric)` — 19 cặp khung hình cố định Trước (`engine-render-before/`) và Sau (`engine-render/`)
Kết xuất bằng code renderer thật của game (`SanctuaryWorldRenderer` + `SanctuaryEnvironmentShadows` + `PixelSpriteRenderer` + `FilterQuality.None`) trên Robolectric Native Skia (`1280×720`, thông số camera ghi tại `docs/visual-evidence/p22/camera_props.json`):

1. **Sanctuary toàn cảnh & cụm cảnh quan (`z=0.36`, `z=0.88`, `z=1.00`):**
   - `sanctuary_overview_z036.png`: toàn đảo nổi Sanctuary ở `z=0.36` — 18 cây sakura và 5 bụi hàng rào hòa sắc đêm tự nhiên với nhà P20/P21 và cỏ P18, không còn đốm hồng sáng gắt hay bụi xanh neon.
   - `sanctuary_south_sakura_pair_z088.png` & `sanctuary_south_sakura_mid_z100.png`: cặp `canopy_a` + `canopy_b` phía nam Quảng trường Triệu Hồi ở zoom gameplay mặc định `0.88` và `1.00`.
   - `sanctuary_courtyard_hedges_z088.png`: sân vườn Khu Dịch Vụ với 5 `hedge_bush` cạnh Hero Quarters, Restaurant, Hero Registry và Synthesis Lab.
   - `sanctuary_ne_garden_z088.png`: góc đông-bắc đối chiếu trực tiếp `canopy_a`/`canopy_b` cạnh `cypress.png` control P20 và Tower Gate P21.
   - `sanctuary_north_lawn_control_z100.png`: bãi cỏ phía bắc với `cypress.png` control P20 đứng cạnh sakura mới.
2. **Sanctuary cận cảnh nguyên cây & chi tiết lá (`z=1.48`):**
   - `sakura_a_full_z148.png`, `sakura_b_full_z148.png`, `sakura_a_foliage_detail_z148.png`: trọn vẹn từ chóp tán tới cổ rễ loe và bóng đổ chéo trên mặt đất; qua vòng tự phê bình Phase B -> v4 đã loại bỏ hoàn toàn đường kẻ cong dưới tán (thay bằng mảng lá xếp vảy tự nhiên) và giữ cụm cánh hoa 2px sau bước lọc pixel đơn lẻ.
   - `courtyard_hedge_front_behind_z148.png`: cận cảnh `hedge_bush.png` với Lyra đứng phía trước, phía sau bụi cây và tại ngưỡng cửa Restaurant.
3. **Kiểm chứng che khuất tán cây trên nhân vật Lyra (`z=0.88` tại `sanctuary_sakura_14`):**
   - `sakura_occlusion_behind_crown_z088.png`: Lyra đi phía bắc thân cây — phần vòm tán trên (`topFraction = 0.56f`) phủ tự nhiên lên nhân vật.
   - `sakura_occlusion_under_crown_z088.png` & `sakura_occlusion_canopy_edge_z088.png`: Lyra đứng ở rìa tán / dưới tán — thân trên được lá che một phần, chân và thân dưới vẫn đọc rõ để điều khiển.
   - `sakura_occlusion_front_z088.png`: Lyra đứng phía nam gốc cây — vẽ đè hoàn toàn phía trước thân và tán cây.
4. **Tower Floor 1 & Floor 2 (`z=0.88`, `z=1.48`):**
   - `tower_f1_pine_oak_bush_z088.png`, `tower_f1_pine_full_z148.png`, `tower_f1_forest_bush_close_z148.png`: `pine.png` và `forest_bush.png` đặt cạnh `oak.png` control P18 trên Tầng 1 (`FIRST_FIELD_MEADOW`).
   - `tower_f2_twisted_plum_z088.png`, `tower_f2_twisted_plum_full_z148.png`: `twisted.png` (`MYSTIC_PLUM_TREE`) đặt cạnh `pine.png` và `oak.png` control trên Tầng 2 (`ELDERWOOD_FOREST`) cùng Lyra trước/sau gốc cây.

---

## 4. Kiểm tra kỹ thuật, hiệu năng và gameplay (Phase D)

### Trình kiểm tra & bộ test đã chạy PASS
- **Offline validators (chạy trực tiếp trong workspace):**
  - `.venv/bin/python tools/validate_mapforge_p22.py` -> **PASS** (`failures: []`): khóa SHA-256 `oak`/`cypress`, đúng kích thước `W×H`, đúng hàng `bottom` (`alpha >= 128`), alpha nhị phân `{0, 255}` (`semi_transparent_px = 0`), bảng màu `8–18` màu trong ngưỡng `[8..24]`, đồng bộ `sanctuary_p03/manifest.json`, tái sinh tất định 100%, và độ khác biệt hình bóng `IoU` giữa các loài (`0.3781 – 0.7627`).
  - `python3 tools/validate_assets.py` -> **PASS**.
  - `.venv/bin/python tools/validate_lyra_pack.py` -> **PASS**.
- **GitHub Actions CI trên PR #25 (commit `1c0331c` / `8677ef2`):**
  - Workflow `Build Eternal Summoner APK` (run `41463620152`): `validate_assets.py` + `gradle :app:assembleDebug` -> **SUCCESS**.
  - Workflow `Lyra engine render evidence` (run `41463620084`):
    - `MapForgeP22RenderTest` (19 khung hình thực) -> **SUCCESS**.
    - `MapForgeP22ContractTest` (kiểm tra SHA-256 control, kích thước, `bounds.bottom`, `drawFootOffset`, `assetSafe` fallback trước `init()`, số lượng prop Sanctuary/Tower và khoảng cách an toàn tới toàn bộ `SanctuaryHouseEntrances`) -> **SUCCESS**.
    - Các bộ test hồi quy `SanctuaryMaterialTest.manifestAssetsDecodeAndMatchRuntimeAtlas`, `SanctuaryEnvironmentArtTest` (bao gồm `characterBehindCanopy` và `obliqueProjectionAndAnchorsRemainConsistent`), `LayoutResponsivenessRobolectricTest`, `ScenePerformanceTest`, `LivingWorldTest`, cùng toàn bộ render/contract test P17c–P21 -> **SUCCESS**.

### Tác động bộ nhớ, hiệu năng và tính bất biến gameplay
- **Không đổi gameplay & bố cục map:** 0 thay đổi lên polygon đảo, đường đi, quảng trường, tọa độ `WorldObstacle`, bán kính va chạm, `SanctuaryHouseEntrances`, camera `OBLIQUE_Y_SCALE = 0.72f`, hay `FilterQuality.None`; không thêm avatar Master lên bản đồ.
- **Bộ nhớ bitmap `ARGB_8888`:** 4 cây thay thế giữ nguyên kích thước (`+0 byte`); 2 bụi cây mới (`64×40` × 2) chỉ thêm **20.480 byte (+20 KB)** khi nạp tài sản và bỏ qua bước sinh mảng procedural tương ứng.
- **Tối ưu bóng đổ thực tế trên GPU:** nhờ xóa nhiễu lỗ 1px li ti trong tán cũ, số hình chữ nhật quét ngang (`Path.addRect`) của `SanctuaryEnvironmentShadows` **giảm 44,0% – 52,4% trên mỗi cây** (`canopy_a: 174 -> 86`, `canopy_b: 147 -> 70`, `pine: 134 -> 75`, `twisted: 198 -> 101`), không thêm bất kỳ render pass hay cấp phát mỗi khung hình nào.
- **Đo đạc trên thiết bị vật lý (FPS / jank / nhiệt độ):** **NOT TESTED** (môi trường sandbox và GitHub Actions không gắn điện thoại Android vật lý; không tự suy diễn số liệu FPS hay nhiệt độ).

---

## 5. Giới hạn còn lại và đề xuất bước tiếp theo

1. **Đo FPS / frame-time thực địa trên điện thoại Android:** khuyến nghị mở màn hình Sanctuary và Tầng 1–2 trên thiết bị thật với `ScenePerformanceProbe` bật để ghi nhận FPS/p95 thực tế sau khi gộp chung P18–P22.
2. **Các hạng mục ngoài phạm vi P22 được giữ nguyên có chủ đích:**
   - `PropKind.FLOWER_PLANTER_BED` (7 bồn hoa viền đá vẽ bằng Canvas) và `Training Ground` (cơ sở chưa nâng lên `premium_slice/` ở P21) được giữ nguyên để không mở rộng phạm vi sang kiến trúc/đạo cụ cứng.
   - Hai ghi chú tồn đọng từ các pha trước (`Armory` trigger lệch 2px ở P20; `DepthAddendumTest:277` đếm pixel xanh sáng của cổng tháp trước khi P21 chuyển hốc cổng sang nền tối và `SanctuaryMaterialTest.materialRepeatSheets` kiểm tra viền `soil` P19) không thuộc P22 và không bị sửa chồng chéo.
3. **Bước tiếp theo khả dĩ (P23+):** nâng cấp đồng bộ các đạo cụ cảnh quan nhỏ còn vẽ bằng Canvas/procedural (`FLOWER_PLANTER_BED`, đèn đường `STREET_LANTERN`, ghế gỗ `WOODEN_BENCH`, cổng tàn tích Tower `RUIN_STONE_WALL`) sang pixel-art đêm đồng bộ với kiến trúc P20/P21 và thảm thực vật P22.
