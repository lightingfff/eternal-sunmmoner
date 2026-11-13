# A08 asset QA — ES-A08-002 (partial, asset-level only)

- **Reference immutable source:** `17c99aea77860958346acf59cecab18aa514c9c1` (`main` tại thời điểm kiểm). 24 PNG tại `docs/art/argon_production_v2/`; 3 QA sheet không tính vào 24.
- **Kết luận:** 14 PASS ở mức *đủ điều kiện thử tích hợp có kiểm soát*, 7 NEEDS_FIX, 3 BLOCKED. Không kết luận hoàn chỉnh thị giác, tính tương thích runtime hay FPS. Tác giả tự khai provenance trong manifest; chưa có bằng chứng độc lập để xác minh quyền gốc/qui trình tạo nên không xác nhận provenance pháp lý.
- PASS chỉ cho asset bytes/visual ở kiểm tra tĩnh và mục đích đã mô tả, KHÔNG cho phép thay layout/collision/camera, không xác nhận pivot/footprint, ánh sáng, hoạt hình hoặc chất lượng ảnh engine. A03 chỉ triển khai sau khi A01 xác nhận quyền sở hữu file; A03/A09 cần kiểm real runtime ở nhiều zoom. #59 và #45 không tự được bỏ HOLD.
- Đã thực sự nhìn 3 contact sheet gốc; ảnh QA độc lập ở `a08_evidence/` là asset preview, không phải gameplay. Hai ảnh 1×, hai ảnh 4× dùng nearest neighbor; kích thước game thật chưa xác thực. Dải tile theo chu kỳ/motif có thể liền về pixel nhưng vẫn lặp khó chịu. Điểm mép trung bình không chứng minh seam/visual đạt.

## Phương pháp và kết quả

Cài Pillow 13.0.0 (local tạm) và chạy:

```sh
python3 -m pip install --target /tmp/a08-pillow 'Pillow==13.0.0'
PYTHONWARNINGS=ignore PYTHONPATH=/tmp/a08-pillow python3 docs/quality-loop/ARGON-WORK-002/a08_evidence/inspect_assets.py
```

Script kiểm PNG signature, chunk CRC32 tất cả chunk và IEND, Pillow full decode RGBA, kích thước, sha, số bytes, IHDR bit depth/color type, alpha counts, khung, nội dung/pixel alpha, pivot số nằm trong hoặc trên biên cell, uniqueness, và dữ liệu seam. 24/24 file giải mã và CRC; 24/24 hash/bytes/dimensions/IHDR/alpha counts khớp manifest; 24/24 không rỗng, tất cả 28 animation frame là unique (8+8+6+6). Các fixture thực sự bị từ chối: cắt 33 byte, IHDR CRC hỏng, và kỳ vọng kích thước sai. Test này kiểm **verifier độc lập A08**, không chứng minh `tools/verify_p31_assets.py` cũ hoạt động. Bảng chi tiết alpha, bounds, edge và delta trong `measurements.json`.

So sánh nhị phân frame không chứng minh animation trông mượt; liên tục/nhịp và loop chưa được xem như playback thực. Kiểm tra số pivot chỉ chứng minh tọa độ nằm trong/on cell, không chứng minh semantic attachment. Các PNG và 3 contact sheet đã đọc/giải mã; chưa so sánh từng pixel của sheet gốc với asset để xác minh sheet gốc không sai. Không build, không CI mới, không ảnh engine/thiết bị, không FPS/thermal. [CI build trên main](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42476655567) thành công trên source SHA, nhưng không phải art QA/runtime.

## Verdict từng file (path tương đối với `docs/art/argon_production_v2/`)

| Path | SHA-256 | Kích thước | Màu RGBA | Verdict | Nhận xét / giới hạn |
|---|---|---:|---:|---|---|
| `batch_01_terrain/terrain_forest_grass_v1_64.png` | `882b6b3f3028e28a6a0cabceed753cc78500bdf17e4fd7d6806710c27fd96cb1` | 64×64 | 35 | NEEDS_FIX | 4×4 lộ cụm thẳng đứng lặp; cần giảm motif/bổ sung biến thể trước phủ nền rộng. |
| `batch_01_terrain/terrain_forest_grass_v2_64.png` | `0ada6311a939878956a0152b1434ec90c240c0c5df1ce44c1063532fd4175013` | 64×64 | 36 | NEEDS_FIX | Dải chéo sáng/tối lặp 64px rõ ở 4×4. |
| `batch_01_terrain/terrain_forest_grass_v3_64.png` | `8616a6899cfe0347e5b82cb3fa7d4b75f175614295eb894e091d8a26592d4295` | 64×64 | 46 | NEEDS_FIX | Đốm đỏ dạng vòng lặp theo lưới; rất dễ nhận ra ở 1×. |
| `batch_01_terrain/terrain_mossy_flagstone_v1_64.png` | `cc203cb74a572722496d9e3db28020765cb81534c1b104a29ee7112998ebdadd` | 64×64 | 133 | PASS | Đá và khe đọc được; dùng quy mô nhỏ trước, kiểm repetition trong engine. |
| `batch_01_terrain/terrain_mossy_flagstone_v2_64.png` | `fde1d867834c7e29e200105990b583aeaccfe1b6d895b9dcd88eb0bd24d7612e` | 64×64 | 134 | PASS | Đá + rêu đọc được; cần xem tiếp nối và lặp ở camera thực. |
| `batch_01_terrain/terrain_compacted_earth_64.png` | `5ea9cd385196573281ad1081d06008c8e2985c35906f8826083688e3c05373cc` | 64×64 | 37 | NEEDS_FIX | Các vệt nâu tạo dải tuần hoàn thấy rõ khi lát. |
| `batch_01_terrain/terrain_tower_gate_runic_stone_64.png` | `42f28457c539cb3675d8fe957bbe807e3ccb47d6e7cacbc6782105062e636984` | 64×64 | 32 | NEEDS_FIX | Hoa văn xanh dạng ô lặp dày và quá đều khi lát 4×4; chỉ là ý tưởng điểm nhấn. |
| `batch_01_terrain/terrain_grass_stone_transition_64.png` | `b05ad98be76606b2abc670badd1c4e17869841c7a1489985b98a6dfd8e38fa8e` | 64×64 | 154 | NEEDS_FIX | Lặp tile tạo dải xen kẽ cỏ/đá, không chứng minh chuyển tiếp từ cỏ sang đá tại ranh giới. |
| `batch_02_foliage_props/foliage_ancient_pine_96x128.png` | `9d218b0b9f2e0be12384165656f5cd8d88480cc8f5ef59165dd6176faa389986` | 96×128 | 9 | PASS | Silhouette dễ đọc; cần thử tách canopy/occlusion, pivot gốc và bóng trong engine. |
| `batch_02_foliage_props/foliage_moss_flower_bush_64x64.png` | `6f933d571f6bb30da45a472c07d237fe627ea677413d6488dda455e410321bfe` | 64×64 | 8 | PASS | Khối cây và hoa vàng đọc được; kiểm scale ở zoom thật. |
| `batch_02_foliage_props/foliage_ashen_violet_shrub_64x64.png` | `da7da58a047c5b40766c559229d51c866cf92c3733d4cda8c9b4d84d456ce84c` | 64×64 | 7 | PASS | Silhouette bụi và màu tím khác biệt; kiểm tương phản ban đêm. |
| `batch_02_foliage_props/foliage_dorm_blossom_clump_64x48.png` | `9ad3c38322f753db0d964ab07fc2da253eb136c2cebb7a2e351a0d4873f5a393` | 64×48 | 12 | PASS | Cụm cây có viền đá và điểm màu; kiểm footprint tại vị trí dùng. |
| `batch_02_foliage_props/prop_wall_ivy_drape_64x96.png` | `1c6e9d94dd8650f96303d668a3522f27016926226625300c1729ad9fc7f97615` | 64×96 | 7 | PASS | Dây leo đọc được; không coi pivot cuối ảnh là neo tường đã xác thực. |
| `batch_02_foliage_props/prop_wooden_bronze_wayfinder_64x96.png` | `22d71139e34b09464821613625641dc2e61383ce741caba2645a15a988000f9d` | 64×96 | 14 | PASS | Cột và đèn đọc được; kiểm chân/collision và ánh sáng thực. |
| `batch_03_architecture_details/arch_black_slate_roof_moss_96x64.png` | `70256eeca024cf9c9166a79f658f33f352f8234a3cc23022bae5c82cc98d4d39` | 96×64 | 32 | PASS | Mái có hình và vật liệu; tỷ lệ, góc roof và neo với building chưa thử. |
| `batch_03_architecture_details/arch_ancient_timber_bronze_door_64x80.png` | `248b9323d718692120629a9634e2d569b8eec571123abda6234f1756963f92c2` | 64×80 | 27 | PASS | Cửa và viền rõ; tuyệt đối không coi là cửa gameplay đã khớp. |
| `batch_03_architecture_details/arch_summoning_hall_pedestal_trim_96x48.png` | `c9d181d78428e44b31ec1d5be67d80e17444e2a6e71fa53dd506cc9bf1f00398` | 96×48 | 12 | PASS | Bệ nhìn rõ; kiểm alignment và không che entrance. |
| `batch_03_architecture_details/arch_warm_stained_window_frame_48x64.png` | `ce8bc68f3fa128ae532a70d2797ecf2fc6ec4d3b305a21d9b39fef450466601f` | 48×64 | 10 | PASS | Ô kính đọc rõ ở 1×; kiểm kích cỡ và ánh sáng trong cảnh. |
| `batch_03_architecture_details/arch_tower_gate_buttress_pillar_64x128.png` | `d4d828ef3f5229634e4c40d58b36f6488141e0943a8688b4b6e39b4882ac5451` | 64×128 | 28 | PASS | Cột có silhouette và ô sáng; kiểm độ sâu thực và offset. |
| `batch_03_architecture_details/arch_dorm_carved_timber_frieze_96x32.png` | `d97926caa4831b1c261fe512be7da809a0addc2790f34b28c12cff2956231bac` | 96×32 | 19 | PASS | Diềm ngang đọc được; khớp mặt tiền chưa thử. |
| `batch_04_animated_vfx/vfx_tower_gate_portal_swirl_8f.png` | `9901049c8a136e7155eba12ea8086838441544c289326d31fb11a4bd737901e5` | 512×64 | 177 | NEEDS_FIX | Alpha chạm mép trên/dưới ở nhiều frame; có nguy cơ cắt, cần tác giả xác nhận/sửa có padding rồi review. |
| `batch_04_animated_vfx/vfx_summoning_hall_mana_motes_8f.png` | `fb0b7986a4176b676f87d5670b82ff63ac9b39102c706e4f49d8f4e42d9be243` | 384×48 | 93 | BLOCKED | Hạt rất nhỏ/nhạt ở 1×; chưa xem playback thật hay chứng minh đọc được ở game zoom. |
| `batch_04_animated_vfx/vfx_wind_petals_leaves_6f.png` | `868712048522d40cfe70aa28331d959f8314e17acbe5654f840893b79f918a2e` | 288×48 | 5 | BLOCKED | Mỗi frame có 14 pixel alpha khác 0, rất khó đọc ở 1×; gameplay zoom/playback chưa thử. |
| `batch_04_animated_vfx/vfx_rain_droplets_splash_6f.png` | `57a088a144298aa10bf9557ef4bfdd08400e6ae7ec0b7654c87e8bcd6f7d4077` | 288×48 | 11 | BLOCKED | Các frame có alpha chạm biên trên/phải; cần xem playback thực để phân biệt cố ý/cắt; chưa thử gameplay. |

## [A08_ASSET_ACCEPTANCE] — danh sách chính xác ở SHA nguồn nêu trên

**PASS (chỉ thử nghiệm asset-level):**
- `docs/art/argon_production_v2/batch_01_terrain/terrain_mossy_flagstone_v1_64.png`
- `docs/art/argon_production_v2/batch_01_terrain/terrain_mossy_flagstone_v2_64.png`
- `docs/art/argon_production_v2/batch_02_foliage_props/foliage_ancient_pine_96x128.png`
- `docs/art/argon_production_v2/batch_02_foliage_props/foliage_moss_flower_bush_64x64.png`
- `docs/art/argon_production_v2/batch_02_foliage_props/foliage_ashen_violet_shrub_64x64.png`
- `docs/art/argon_production_v2/batch_02_foliage_props/foliage_dorm_blossom_clump_64x48.png`
- `docs/art/argon_production_v2/batch_02_foliage_props/prop_wall_ivy_drape_64x96.png`
- `docs/art/argon_production_v2/batch_02_foliage_props/prop_wooden_bronze_wayfinder_64x96.png`
- `docs/art/argon_production_v2/batch_03_architecture_details/arch_black_slate_roof_moss_96x64.png`
- `docs/art/argon_production_v2/batch_03_architecture_details/arch_ancient_timber_bronze_door_64x80.png`
- `docs/art/argon_production_v2/batch_03_architecture_details/arch_summoning_hall_pedestal_trim_96x48.png`
- `docs/art/argon_production_v2/batch_03_architecture_details/arch_warm_stained_window_frame_48x64.png`
- `docs/art/argon_production_v2/batch_03_architecture_details/arch_tower_gate_buttress_pillar_64x128.png`
- `docs/art/argon_production_v2/batch_03_architecture_details/arch_dorm_carved_timber_frieze_96x32.png`

**NEEDS_FIX / BLOCKED — không đưa vào whitelist:**
- `docs/art/argon_production_v2/batch_01_terrain/terrain_forest_grass_v1_64.png` — NEEDS_FIX
- `docs/art/argon_production_v2/batch_01_terrain/terrain_forest_grass_v2_64.png` — NEEDS_FIX
- `docs/art/argon_production_v2/batch_01_terrain/terrain_forest_grass_v3_64.png` — NEEDS_FIX
- `docs/art/argon_production_v2/batch_01_terrain/terrain_compacted_earth_64.png` — NEEDS_FIX
- `docs/art/argon_production_v2/batch_01_terrain/terrain_tower_gate_runic_stone_64.png` — NEEDS_FIX
- `docs/art/argon_production_v2/batch_01_terrain/terrain_grass_stone_transition_64.png` — NEEDS_FIX
- `docs/art/argon_production_v2/batch_04_animated_vfx/vfx_tower_gate_portal_swirl_8f.png` — NEEDS_FIX
- `docs/art/argon_production_v2/batch_04_animated_vfx/vfx_summoning_hall_mana_motes_8f.png` — BLOCKED
- `docs/art/argon_production_v2/batch_04_animated_vfx/vfx_wind_petals_leaves_6f.png` — BLOCKED
- `docs/art/argon_production_v2/batch_04_animated_vfx/vfx_rain_droplets_splash_6f.png` — BLOCKED

## Tiếp theo

Argon chỉ sửa PNG NEEDS_FIX sau khi có handoff/ownership chính xác, sau đó A08 đo lại SHA mới. Các asset BLOCKED cần kiểm playback/timing và game-zoom bằng pipeline có thật hoặc một bằng chứng gần tương đương trước khi quyết định. A03 cần đối chiếu ảnh engine trước/sau, Y=.72, neo, clipping, canopy và ánh sáng; không nhầm PASS file với acceptance runtime. A09 và Codex/Argon review độc lập báo cáo; chủ dự án quyết định merge. Điện thoại/FPS/thermal: NOT TESTED.
