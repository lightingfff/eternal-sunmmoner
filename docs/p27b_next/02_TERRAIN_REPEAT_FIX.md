# CHECKPOINT 1 — sửa lỗi lặp texture 4×4 (debt B)

Trạng thái: **code + asset + gate + ảnh renderer thật đã xong, đã commit** (`91563f4`, ảnh bot commit `dca70dc`, CI run `42026395257` PASS).

Kết luận trung thực: **lỗi "lặp khối 4×4" đã bị loại về mặt cấu trúc và đo được; nhưng lỗi "nền còn lặp" CHƯA đóng.** Lý do ở mục 6.2 — đây là chỗ dễ tự lừa nhất nên phải nói rõ.

## 1. Đo lỗi trước khi sửa (không đoán)
Cách lấy mẫu cũ: `PixelTileAtlas.sanctuaryTileIndex` → `(ty and 3) shl 2 or (tx and 3)`, tức **torus 4×4 dịch chuyển thuần tuý**. Hệ quả toán học: ảnh lặp với chu kỳ **4 tile = 256 world px**, và vì phép lấy mẫu chỉ tịnh tiến, mọi cấu trúc tần số thấp cũng lặp theo đúng chu kỳ đó.

Đo trên PNG đã ship (Pillow 11.3.0, luma Rec.601):
| family | cell mean luma min..max | spread | sd |
|---|---|---|---|
| grass | 93.1 .. 97.8 | 4.7 | 1.3 |
| soil | 141.4 .. 154.9 | 13.5 | 3.2 |
| flagstone | 163.2 .. 179.5 | 16.4 | 4.4 |
| cobble | 110.9 .. 113.4 | 2.6 | 0.9 |

Hai kết luận quan trọng từ bảng này:
1. Trong **một** torus 4×4, 16 ô đã gần như đồng nhất về độ sáng → **thêm biến thể 64 px không thể chữa lỗi**, vì chu kỳ không đổi. Đây chính là điều V5 cấm ("tăng variant bằng 16 PNG khác nhưng vẫn lặp chu kỳ 4").
2. Lỗi người chơi thấy không phải "16 ô giống nhau" mà là **cấu trúc lặp nguyên khối**: cùng một cụm đá/cỏ xuất hiện lại sau 256 px.

## 2. Phương án đã cân nhắc và loại
| Phương án | Vì sao loại |
|---|---|
| Thêm 16 PNG biến thể khác trong cùng torus 4×4 | chu kỳ vẫn 256 px — đúng thứ V5 cấm |
| Hash chọn ô ngẫu nhiên từ 16 ô (bỏ ràng buộc lân cận) | **đo được**: ô đã ship không tự wrap — self-wrap biên flagstone tới **52/255**, grass 27.7. Ghép tuỳ ý sẽ tạo lưới "đá bị cắt" ở mọi mốc 64 px |
| Upscale mờ / lọc song tuyến | V5 cấm; phá pixel-art |
| Dùng lại ảnh tham khảo | V5 cấm |

## 3. Phương án đã làm
**8×8 mosaic sheet, giữ nguyên lân cận thật, chu kỳ 512 world px (dài gấp 4 mỗi trục).**

- Dựng từ **cùng bộ minh hoạ Arena** đã dùng cho P27B (không sinh art mới, không dùng ảnh tham khảo). Nguồn: bản gốc trong `art/p27b_sources/` nếu có, nếu không có thì dùng preview 768 px đã commit — manifest ghi rõ `source_kind`.
- Torus bằng **roll-and-heal**: cuộn nửa sheet để đường cắt cứng của ảnh gốc rơi vào giữa sheet, hàn bằng crossfade hẹp, rồi quantize no-dither. Nhờ vậy **mọi** cặp lân cận — kể cả cặp wrap — đều là lân cận thật.
- Ship **1 PNG 512×512/family**, blit bằng `srcRect` 64×64. Bộ nhớ 1 MB/family (ARGB_8888) — **không tăng**; số texture giảm 64 → 1.
- Downscale 768 → 512 (1.5×) **nét hơn** atlas cũ 1254 → 256 (4.9×).

Số đo trên sheet mới (`tools/p27b_ground_gate.py`):
```
grass      step1   2.35 step4   2.12 distinct 64/64 wrap 15.10 interior_p95 17.01
soil       step1   3.43 step4   5.95 distinct 64/64 wrap  8.19 interior_p95  8.16
flagstone  step1   9.35 step4  11.21 distinct 64/64 wrap  8.48 interior_p95 10.42
cobble     step1   2.08 step4   1.94 distinct 64/64 wrap 15.34 interior_p95 16.62
```
`step4` = chênh lệch giữa ô và ô cách 4 tile. Trên atlas cũ giá trị này **bằng 0 tuyệt đối** (vì cùng một ô); nay nó ngang với `step1` (lân cận thật). `wrap` ≤ `interior_p95 + 8` → mép sheet không tạo lưới.

## 4. Thay đổi code
| File | Thay đổi |
|---|---|
| `SanctuaryArtAssets.kt` | `groundSheet(name)`: bind `sanctuary_p27b_2/terrain/{name}_mosaic.png`, validate 512×512, negative-cache |
| `PixelTileAtlas.kt` | `sanctuaryGrassSheet/SoilSheet/FlagstoneSheet/CobbleSheet`, `sanctuarySheetCell(tx,ty)`, và helper dùng chung `DrawScope.drawGroundTile(...)` |
| `SanctuaryWorldRenderer.kt` | đường per-tile (grass/sand/cobble/plaza) + đường overview: khi có sheet thì blit **cả sheet**/world cell (1 lệnh thay vì 64–256 lệnh), dùng `floorDiv` để đúng ở toạ độ âm |
| `PixelSpriteRenderer.kt` | nhánh ground của sprite overlay dùng cùng helper |
| `InteriorWorldRenderer.kt` | sân terrace dùng cùng helper |

Fallback **nguyên tầng**, không trộn atlas: `sheet → P27B 4×4 (16 ô) → P03 PNG → procedural`.

Về draw call: đường làng xóm có ~(W/64)×(H/64) lệnh như trước, chỉ đổi nguồn texture (1 texture thay vì 16). Đường overview **giảm** từ ~256 lệnh tile (hoặc 16 lệnh block cũ) xuống 1 lệnh/world-cell 512 px. Không thêm cấp phát per-tile per-frame: `srcRect` là tham số của lệnh vẽ, không tạo object.

## 5. Test/gate đã thêm
| Kiểm tra | Nội dung | Nếu quay lại 4×4 |
|---|---|---|
| `tools/p27b_ground_gate.py` | period-4 identity, wrap vs interior p95, đếm ô phân biệt, manifest 8×8 | FAIL ngay |
| `P27BGroundRepeatTest.sheetIsAn8x8MosaicWith64DistinctCells` | 512×512, 64 ô | FAIL |
| `...fourByFourLatticeIsGone` | `|cell(x)-cell(x+4)|` phải > 0.05 và ≥ 0.5·`|cell(x)-cell(x+1)|` | FAIL (bằng 0) |
| `...samplingKeepsMosaicAdjacency` | `sanctuarySheetCell` đúng 0..63, chu kỳ 8, đúng cả toạ độ âm | FAIL |
| `...sheetWrapsWithoutAVisibleSeam` | wrap ≤ p95 biên trong + 8 | FAIL |
| `...runtimeBindsSheetAndFallsBackWholeWhenAbsent` | runtime thật sự bind sheet, family 4×4 vẫn còn nguyên 16 ô | FAIL/ẩn lỗi fallback |

## 6. Evidence renderer thật
Workflow `P27B matched Sanctuary render evidence` giờ chạy trên nhánh này và ghi ảnh vào `docs/p27b_next/previews/after/world/` (thư mục `docs/p27b/` của PR #34 **giữ nguyên byte-for-byte** làm baseline). Ma trận của `P27BNextRenderTest`:
- `overview_noon_z036`, `service_noon_z088`, `plaza_noon_z100`, `plaza_noon_z148`, `plaza_night_z100`
- `summon_/gate_` × hour 6 / 12 / 18 / 0 ở z1.48
- `house_quarters_noon_z148`, `house_restaurant_noon_z148`

Ảnh sẽ có sau khi run xong; `docs/p27b_next/runs.json` được cập nhật run id + verdict. **Chưa có ảnh thì chưa kết luận debt B đã đóng** — mới chỉ chứng minh được chu kỳ 4 đã bị loại bằng số đo và bằng gate tự động.

## 6.1. Ảnh thật (CI `42026395257`, SHA `91563f4`)
- `docs/p27b_next/previews/after/world/` gồm 15 ảnh 1280×720 từ `SanctuaryWorldRenderer` thật (Robolectric native Skia): z0.36 / z0.88 / z1.0 / z1.48, plaza đêm, Gate & Hall ở hour 6/12/18/0, và 2 ảnh cận nhà P03 (debt A).
- Baseline `docs/p27b/previews/` **được đóng băng**: `git diff e5e04e1 HEAD -- docs/p27b` rỗng. Trước đó workflow đã âm thầm ghi đè thư mục này (đã sửa, có bước `git restore`).

## 6.2. Cái gì đã đo được, cái gì CHƯA
| Đo | OLD | NEW | Ý nghĩa |
|---|---|---|---|
| Ô cách 4 tile có trùng ô gốc? | **có, tuyệt đối** | không (`step4` ≈ `step1`) | lỗi lặp khối 4×4 đã bị loại về cấu trúc |
| Self-similarity tại lag 256 px (plaza, dải đá trơn) | **+0.15** | **+0.05** | bớt tự giống hơn rõ, nhưng **vẫn > 0** |
| Self-similarity tại lag 512 px | — (ngoài chu kỳ cũ) | không phải cực đại | chu kỳ đã dời lên 512 px |
| Mép sheet so với biên trong p95 | — | ≤ p95 + 8 | không tạo lưới ở mép |
| Mean luma vs atlas cũ | — | grass −0.1, soil −0.3, cobble −0.4, flagstone −2.6 | không bị "sáng hơn nền" |

**Chưa đóng:** một ô 8×8 vẫn lặp sau **512 world px** (≈1024 px màn hình ở zoom 1.0). Cửa sổ plaza hiện rộng ~470 px nên vòng lặp thứ hai chưa lộ trong ảnh, nhưng **trên map lớn hoặc ở zoom 0.36 thì chu kỳ 512 px là thật**. Nói "đã seamless" ở đây là sai. Muốn đóng hẳn cần (a) atlas/splat lớn hơn hẳn ngân sách bộ nhớ hiện tại, hoặc (b) pha hai lớp sample so le phi tuyến — cả hai là thay đổi renderer lớn hơn phạm vi checkpoint này và phải đo lại toàn bộ ma trận ảnh.

## 6.3. Ba lỗi chỉ lộ ra khi xem ảnh thật (ghi để không lặp lại)
1. Dựng sheet từ nguồn 768 px của P27B → **đá to gấp đôi**. Test vẫn PASS (64 ô phân biệt, mép kín) vì test không biết gì về tỉ lệ.
2. Heel crossfade 20 px → **dải sáng lặp thành lưới**. Đã thay bằng torus roll không pha trộn (mở sheet ở chỗ nguồn có đường cắt sạch nhất).
3. Flagstone **sáng hơn atlas cũ 8 luma** → trông như dán lên nền. Đã khoá target luma theo atlas cũ.

Cả ba giờ đều có gate: uniform-sheet check, luma-delta check, và ảnh diff OLD/NEW ở cùng camera.

## 7. NOT TESTED
- FPS/thermal trên thiết bị thật. Số liệu duy nhất liên quan hiệu năng là số lệnh vẽ đọc từ code: đường overview giảm từ ~16 lệnh block xuống 1 lệnh/world cell; đường per-tile giữ nguyên số lệnh nhưng dùng 1 texture thay vì 16.
- Cảm nhận thẩm mỹ ở z1.48 trên màn hình thật.
- Việc 512 px có đủ cho màn hình mật độ cao khi zoom 1.48 hay không (đo bằng ảnh CI, không phải bằng suy luận).
