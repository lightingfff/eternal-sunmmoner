# P19 — Báo cáo (tiếng Việt): độ đồng nhất bề mặt nền

**Phạm vi:** chỉ art. Không đổi layout, độ rộng đường, anchor công trình, cửa vào, collision,
camera (`OBLIQUE_Y_SCALE = 0.72`), vị trí prop hay logic gameplay. Batch 4×4 cỏ của P16,
culling đường và nearest sampling giữ nguyên.

## 1. Chẩn đoán (render thật, camera cố định)

Camera cố định trong `MapForgeTerrainRenderTest` (0.36 / 0.88 / 1.00 / 1.48) → hai bộ
`engine-render-before/` (main) và `engine-render/` (P19) so sánh trực tiếp được.

| # | Lỗi thấy trong renderer thật | Bằng chứng |
|---|---|---|
| 1 | Sàn bãi tập là mảng nâu phẳng + đốm sáng 1 px rải rác → phẳng và "procedural" hơn hẳn cỏ/đường của P18 | `terrain_training_default.png`, `terrain_training_close.png` |
| 2 | Dải sỏi giữa cỏ và cát chỉ là confetti, không đọc ra "đá nhỏ", nên chuyển chất liệu cỏ→sỏi→đất bị gãy | `terrain_arena_rim.png` |
| 3 | Mạch vữa cobble chạy thành dải ngang hết bề rộng tile → ở 1.48 đường đọc như tường gạch | `terrain_road_joints_close.png`, `tiles_seams_5x5_before.png` |
| 4 | Cụm cỏ đều cỡ, pixel sáng nhất vẫn bật cả ở cụm nhỏ → 1.48 hơi "đốm", 0.36 hơi rậm | `terrain_lawn_close.png` |

Số liệu gốc (`tiles_stats_before.json`): soil 8 màu/variant, gravel 6, grass 8–10,
cobble 9–10, flagstone 9. 8 màu không đủ để có base → shade → highlight → vữa → sạn.

## 2. Đã thay gì

### `soil_0..5` (viết lại, 64×64 RGBA, seamless)
- **3 thang giá trị**: mảng đất nhỏ (6–14 px, lệch 1 bậc), vệt đầm nén ngắn/đứt (không bao
  giờ thành dải hết bề rộng), 3–5 cụm sỏi nằm trong vùng đất bị xáo.
- Sỏi: bóng tiếp xúc dưới-phải, thân, 1 px rim sáng trên-trái; tông xám lạnh để ăn nhập bộ
  từ vựng cobble.
- Vết nứt khô 1 px (có đường shade bên cạnh), sạn mịn tương phản thấp.
- 8 → **11 màu**/variant. **Cân bằng tông giữa các variant** (xem §3).

### `gravel_0..3` (viết lại)
- 6–8 cụm × 2–5 viên sỏi trên nền đất thô; rêu chỉ ở 2 variant ẩm.
- Mật độ tiết chế để chân hero nhỏ không bị "nuốt" ở 0.88.
- 6 → **9–11 màu**/variant, giá trị đá tương thích cobble nhưng ít hình học hơn.

### `cobble_0..5` (chỉ tinh chỉnh mạch)
- Thêm 3–4 **break stone**/tile bắc qua ranh giới lớp → cắt dải vữa liền mạch thành đoạn ngắn.
- Vữa thu còn 1–2 px, nâng từ `#181f24` → `#1e262b` (giảm tương phản "báo đốm").
- Không đổi số đá, mask đường, độ rộng hay culling.

### `grass_0..7` (chỉ mật độ)
- 10–13 cụm (trước 12–16), dịch phân bố về cỡ trung, pixel sáng nhất chỉ bật ở cỡ > 0,
  3 cụm focal thay vì 2. Cách ghép block 4×4 không đổi.

### Sinh asset
`tools/mapforge_ground.py` (mở rộng, deterministic, chạy offline). Helper mới: `pebble()`,
`pebble_cluster()`, `mean_luma()`, `equalize_tone()`. Manifest `sha256`/`size` làm mới;
`tools/validate_assets.py` PASS.

## 3. Hai lỗi do chính pass này gây ra và đã sửa

1. **Khung viền 2 px.** Bước copy viền dùng chung của P18 dán viền variant 0 vào mọi tile.
   Với vật liệu đất có mảng lớn, viền đó không khớp ruột tile → hiện **khung chữ nhật** quanh
   từng tile soil/gravel. Đã cho 2 family đất **opt-out** (`shared = False`); grass/cobble/
   flagstone vẫn giữ.
2. **Bàn cờ theo variant.** Bản đầu cho một số variant nền tối hơn + dải đầm nén hết bề rộng.
   Vì renderer chọn variant bằng hash, bãi tập thành **khung bàn cờ 56 px** ở 0.88. Sửa bằng:
   (a) thu mọi feature đất xuống nhỏ hơn tile, (b) bỏ chênh lệch giá trị giữa variant,
   (c) `equalize_tone()` kéo trung bình luma từng variant về variant 0 (lệch tối đa ±12, chỉ
   cộng offset đều 3 kênh nên không đổi hue). Đo lại: soil 74.8–75.3, gravel 69.1–69.3.

## 4. Bằng chứng

`docs/visual-evidence/p19/`

| file | nhãn |
|---|---|
| `engine_before_after.png` | **REAL ENGINE RENDER** cạnh nhau (bãi tập 0.88, cobble 1.48) |
| `engine-render/*.png` | **REAL ENGINE RENDER** — asset P19, 8 camera cố định |
| `engine-render-before/*.png` | **REAL ENGINE RENDER** — asset main (trước P19), cùng 8 camera |
| `before_after_side_by_side.png` | **ART CONTACT SHEET** — 4× nearest |
| `tiles_contact_1x_*`, `tiles_contact_4x_*` | **ART CONTACT SHEET** |
| `tiles_mosaic_*` | **ART CONTACT SHEET** — mosaic oblique 0.36/0.88/1.48 |
| `tiles_seams_5x5_*` | **SEAM CHECK** — mỗi variant lát 5×5 ở 1× |
| `tiles_stats_*.json` | số màu/variant |

## 5. Build & QA

- `tools/validate_assets.py` → PASS (package `com.aistudio.eternalsummoner.kvrpzm`)
- `tools/validate_lyra_pack.py` → PASS
- CI `assembleDebug` và render job: xem mục "Build/QA status" trong PR #22 (run ID ghi kèm).
- **FPS / nhiệt lượng thiết bị: NOT TESTED** (không có máy thật hay emulator có
  instrumentation trong môi trường này).
- Test đề xuất trên máy: pan/zoom cố định 0.36/0.88/1.48 trong 5 phút, ghi frame pacing,
  RAM và nhiệt so với build trước; dừng nếu throttle.

## 6. Ảnh hưởng RAM / runtime

- Số asset **không đổi**: grass 8, cobble 6, flagstone 8, soil 6, gravel 4, verge, oak.
  Không thêm file/family, không đổi contract manifest.
- RAM decode **không đổi**: 34 asset 64×64×4 B ≈ 0,55 MB + oak ≈ 61 KB + 27 block cỏ
  (256×184×4 B) ≈ 5,1 MB — giống P18/P16.
- Draw call **không đổi**: cùng số tile, cùng nhánh block 4×4, cùng culling. Không sinh PNG
  lúc runtime, không cấp phát mới trong draw loop, không thêm pass blend, không shader.
- Dung lượng PNG trên đĩa **không phải** RAM; các số trên là ước lượng decode, **không phải**
  đo trên thiết bị.

## 7. Điểm còn yếu

- Dải sỏi ở quy mô đảo khá mỏng → chi tiết mới chỉ đọc rõ ở 1.48; muốn rộng hơn phải đổi
  layout (ngoài phạm vi).
- Viền bãi tập vẫn là dải màu phẳng của renderer (`0xFF4A3D2E`), giờ tương phản với đất có
  texture bên dưới; sửa là việc của renderer, không phải asset.
- Cụm sỏi ở 1.48 vẫn hơi "giống con vật" trên nền quá phẳng; pass sau nên cho 2 cụm chồng
  nhau để đọc thành đống đá vụn.
- Soil/gravel giờ **tối hơn** "golden sand" cũ. Đúng brief (đất nén tối, tương phản dưới
  actor) nhưng là thay đổi mood — cần review trên máy thật.
