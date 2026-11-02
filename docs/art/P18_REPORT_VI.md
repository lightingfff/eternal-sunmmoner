# P18 — Báo cáo Map Forge: vertical slice bề mặt nền (tiếng Việt)

**Khu vực chọn:** quảng trường trung tâm → đường đá → bãi cỏ (Sanctuary island).
**Phạm vi:** chỉ texture + 1 prop. Không đổi layout, độ rộng đường, nhà cửa, cửa vào,
collision, camera (`ObliqueCamera`, ground Y 0.72), vị trí prop hay logic tương tác.

## 1. Vì sao bản cũ yếu

| family | file | số màu (trước) | lỗi thấy rõ ở 4× |
|---|---|---|---|
| grass | 8 | 9–10 | một màu xanh phẳng + "confetti" blade rời rạc, không cụm, không phân bậc sáng tối |
| cobble | 6 | 12 | 5 hàng hình chữ nhật bo tròn giống hệt nhau, khe đều, không có chiều sâu vữa |
| flagstone | 8 | 8–12 | 3 lớp slab cứng nhắc giống nhau, viền motif dán ở mép tile, ám vàng ô-liu |

Hai nguyên nhân cấu trúc (đã kiểm trong `tools/generate_sanctuary_art.py`):

1. **Quá ít màu** (8–12) → không thể có thang giá trị base → shade → highlight → mortar →
   moss, nên mọi phiến đá chỉ là một khối phẳng.
2. **Motif viền dùng chung** cho mọi variant → nhịp lặp bị "ghim" vào mép tile, ở zoom
   overview 0.36 nhìn thành lưới.

## 2. Asset đã thay (24 file, ghi đè tại chỗ, reversible bằng git)

`grass_0..7`, `cobble_0..5`, `flagstone_0..7`, `verge.png`, `oak.png`.
Manifest `sha256` + `size` được generator làm mới; `tools/validate_assets.py` = PASS.

**Không** đụng: soil, gravel, wood, các prop khác, fallback `SanctuaryMaterials`,
loader, mapping family trong `PixelTileAtlas` (8/6/8/6), batch 4×4, culling đường,
`FilterQuality.None`. Không shader, không blur, không bloom.

## 3. Cách dựng asset mới — `tools/mapforge_ground.py`

- **Torus painting**: vẽ trên vòng xuyến 192×192 rồi crop 64..128 → mỗi tile tự liền mạch;
  giữ hợp đồng "2 px perimeter dùng chung" để các variant ghép sạch, nhưng **phần ruột**
  giờ khác nhau theo variant (cobble có phase lớp đá lệch theo variant → mất các hàng seam
  lặp lại).
- **`stone()`**: một phiến đá = polygon base + bevel 1 px theo hướng sáng + shade 1 px dưới
  + mẻ góc (chip) + rêu ở góc. Thang màu `shade → body → light` (+ `cool`/`wear`),
  16–18 màu/family.
- **`blade_fan()`**: cụm cỏ compact — halo tối, polygon gốc, tối đa 3 blade ngắn; scale 0/1/2
  để bãi cỏ có nhịp, không bị "đóng dấu" đều nhau.
- **Nền cỏ**: mottle 3 tông + 2 dải trũng giá trị thấp/tile.
- **`verge()`**: giữ nguyên mask clip mà renderer đang dùng (`SanctuaryWorldRenderer`
  L307/L413), chỉ vẽ lại pixel bên trong mask.
- **`oak()`**: repaint đúng 112×136, giữ ground anchor, cùng hướng sáng với bộ prop còn lại.

## 4. Tích hợp với renderer thật

- `SanctuaryArtAssets.tiles("grass", 8)` / `("cobble", 6)` / `("flagstone", 8)` vẫn resolve
  đúng số lượng → không phải sửa loader hay `PixelTileAtlas`.
- `buildSanctuaryGrassBlocks()` tự ghép lại block 4×4 (256×184 px) từ tile **mới** khi load →
  overview tự có art mới mà không cần đổi code batch.
- Render thật đi qua `drawSanctuaryFloatingWorld`, `drawGroundShadows`, `drawProp` (depth
  sorted) — đúng hàm game đang vẽ.

## 5. Bằng chứng (có nhãn)

`docs/visual-evidence/p18/`

| file | loại |
|---|---|
| `before_after_side_by_side.png` | **ART CONTACT SHEET** — 4× nearest, trước/sau cùng khung |
| `tiles_contact_1x_before/after.png`, `tiles_contact_4x_before/after.png` | **ART CONTACT SHEET** |
| `tiles_mosaic_before/after.png` | **ART CONTACT SHEET** — mosaic oblique 0.36 / 0.88 / 1.48 (preview tác giả, không phải engine) |
| `tiles_stats_before/after.json` | số màu + giá trị trung bình mỗi family |
| `grass_blocks_after.png`, `grass_blocks_batching_parity.png` | block overview ghép lại / parity P16 |
| `engine-render/ground_plaza_default.png` (0.88) | **REAL ENGINE RENDER** |
| `engine-render/ground_plaza_overview.png` (0.36) | **REAL ENGINE RENDER** — nhánh block 4×4 |
| `engine-render/ground_road_verge_close.png` (1.48) | **REAL ENGINE RENDER** — seam đường/verge/cỏ |
| `engine-render/ground_lawn_close.png` (1.48) | **REAL ENGINE RENDER** — chỉ cỏ, không prop |
| `engine-render/ground_oak_verge.png` (1.00) | **REAL ENGINE RENDER** |

Render thật do `MapForgeGroundRenderTest` (`@GraphicsMode(NATIVE)`, Robolectric) sinh ra,
rasterize phần mềm ở density 1× → không đại diện DPI thiết bị.

## 6. Cổng QA

- `tools/validate_assets.py` → PASS (package `com.aistudio.eternalsummoner.kvrpzm`)
- `tools/validate_lyra_pack.py` → PASS
- CI `assembleDebug`: [41424137317](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41424137317) — **success**
- CI render job: [41424137414](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41424137414) — **success** (commit `5701574`)
- **FPS / nhiệt lượng thiết bị: NOT TESTED** (không có máy thật trong môi trường này).

Ước lượng RAM decode: 33 tile 64×64×4 B ≈ **0,54 MB** + oak 112×136×4 B ≈ **61 KB**;
27 block 4×4 (256×184×4 B) ≈ **5,1 MB** — **không đổi so với P16** vì số block và kích thước
block giữ nguyên, chỉ đổi nội dung pixel. Dung lượng PNG trên đĩa không phải RAM.

## 7. Tự phê bình / việc tiếp theo

- Cobble vẫn hơi "theo lớp" (joint ngang khá đều) — đã hơn hẳn baseline nhưng lần sau nên
  bẻ joint thành đoạn ngắn.
- Soil và gravel giữ chuẩn cũ → mảng đất/bãi tập cạnh đường mới **trông phẳng hơn rõ rệt**.
  Slice bounded kế tiếp nên lấy soil + gravel.
- Cụm cỏ ở 1.48 hơi "đốm"; ở 0.36 lại hơi rậm — cần một pass cân mật độ.
- Không có số đo FPS/nhiệt: **NOT TESTED**.
- Chưa claim "ngang Brown Dust 2": mới so ở quy mô tile/render, chưa so ở gameplay-scale
  trên thiết bị.
