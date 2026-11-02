# P20 — Báo cáo (tiếng Việt): kiến trúc 2.5D + thảm thực vật, vertical slice

**Slice chọn:** **Armory** (facility gần quảng trường, anchor `(995,655)`, cửa `(993,673)`) +
**cây cypress** (`PropKind.CYPRESS_CONIFER_TREE`, asset `cypress.png`) + nền P18/P19 giữ nguyên.
Không đổi toạ độ, collider, cửa, camera (`0.72`), batch cỏ 4×4, culling đường,
`FilterQuality.None`, không sửa code renderer, không shader.

## 1. Đường dẫn thay đổi

| file | thay đổi | lý do |
|---|---|---|
| `app/src/main/assets/premium_slice/armory.png` | repaint 190×166 (giữ nguyên canvas + hộp cửa) | kiến trúc 2.5D premium cho armory |
| `app/src/main/assets/sanctuary_p03/cypress.png` | repaint 48×88 (giữ anchor `[24,83]`, `topFraction 0.54`) | tán lá nhiều lớp, thân rõ, có khoang tối |
| `app/src/main/assets/premium_slice/manifest.json`, `sanctuary_p03/manifest.json` | cập nhật `sha256` + `size` | gate validator |
| `tools/mapforge_architecture.py` | **mới** — generator offline, deterministic | tái lập được asset |
| `app/src/test/java/com/example/MapForgeArchitectureRenderTest.kt` | **mới** — 11 camera cố định + actor | bằng chứng engine trước/sau |
| `app/src/test/java/com/example/MapForgeArchitectureContractTest.kt` | **mới** — contract test | canvas/hộp cửa/anchor/manifest, regression P19 |
| `.github/workflows/lyra-render-evidence.yml` | thêm 2 bước test + publish `p20/` | xuất bằng chứng |
| `docs/visual-evidence/p20/**` | contact sheet + engine render | bằng chứng |
| `docs/art/P20_ARCHITECTURE_SLICE.md` | tài liệu kỹ thuật | chi tiết |

## 2. Lỗi art cũ và cách sửa

**Armory (190×166)**
- Mái chỉ là một mặt phẳng "sticker": không có diềm mái, không có bóng hắt, không có sống mái → không có khối. **Sửa:** mái thành **2 mặt phẳng thật** (mặt timber từ diềm `(14,96)-(99,112)` lên sống `(58,30)-(150,16)`, và mặt slate hip `(99,112)-(186,84)-(150,16)`), mỗi mặt có courses so le (`_shingle_plane`: tông từng viên theo ramp 3 bậc, lip sáng trên, seam tối dưới), sống mái 2 px, diềm mái + **2 dải bóng hắt** (`shadow`+`ao`) đổ xuống tường.
- Khối đá đồng dạng, giá trị phẳng. **Sửa:** `masonry()` — courses cao 7–9 px, block 9–21 px, rim sáng trên-trái, mặt dưới tối, joint dọc, mẻ góc; **quoin** ở góc trước; **course móng** tối hơn + **AO tiếp xúc**.
- Cửa phẳng. **Sửa:** giữ **đúng hộp pixel `x133..150, y109..139`** (có test), thêm khung đá + keystone, reveal 2 px, lá cửa ván, 2 đai sắt + đinh, tay nắm, ánh sáng ấm ở ngưỡng, 3 bậc đá.
- Cửa sổ là ô vàng phẳng. **Sửa:** khung + mullion + bệ cửa sổ + bóng bệ, phát sáng có kiểm soát (không cháy), shutter gỗ.
- Thêm **khung gỗ lộ** (post + rail + brace) để armory cùng ngôn ngữ với các nhà medieval bên cạnh thay vì "hộp đá trơn"; **ống khói** có chóp + bóng đổ trên mái; **biển hiệu rèn** (anvil) treo trên giá sắt.

**Cypress (48×88)**
- Tán là các "đĩa ngang" xếp lớp → đọc như hộp chồng. **Sửa:** 9 **branch spray** chồng nhau (đáy răng cưa, đỉnh thuôn), nửa phải vào shade, cụm sáng ở nửa trái trên, khoang tối mỏng giữa các tầng, tip cành lòi ra, ngọn có điểm sáng khô, rêu ở chân.
- Thân mảnh, trơn. **Sửa:** thân thuôn, vân bark, highlight, buttress roots, đường tối ở chân.

## 3. Bằng chứng (có nhãn)

`docs/visual-evidence/p20/`
- **REAL ENGINE RENDER**: `engine_before_after_close.png` (1.48), `engine_before_after_default.png` (0.88), `engine-render/` (11 camera, asset P20), `engine-render-before/` (11 camera, main `72c4701`)
- **ART CONTACT SHEET**: `armory_contact_sheet.png` (1×/2×/3×), `cypress_contact_sheet.png` (1×/3×/6×)

## 4. Gate

- `tools/validate_assets.py` → **PASS** (CRC/mode/kích thước + manifest SHA + package)
- `tools/validate_lyra_pack.py` → **PASS**
- `MapForgeArchitectureContractTest` → **PASS** (armory 190×166 + hộp cửa vẫn là ô tối mở + SHA; cypress 48×88 + anchor `[24,83]` + mật độ tán/chân; grass 8 / soil 6 / gravel 4 và atlas Lyra vẫn load)
- CI `assembleDebug`: [41432905414](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41432905414) **success**
- CI render job: [41432905556](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41432905556) **success** (commit `327453c`; artifact `engine-render-evidence`)
- **FPS / jank / nhiệt thiết bị: NOT TESTED** (không có máy thật).

## 5. RAM và chi phí vẽ

- 2 asset thay thế, không thêm asset/family, không đổi contract manifest.
- RAM decode: armory 190×166×4 B = **126 KB** (không đổi), cypress 48×88×4 B = **16,5 KB** (không đổi) vì canvas giữ nguyên.
- Draw cost không đổi: cùng số sprite, cùng `drawCrispSprite`, cùng nearest sampling, cùng shadow pass, không sinh asset lúc runtime, không cấp phát mỗi frame, không thêm pass blend/offscreen.

## 6. Tự phê bình (thật)

- **Armory vẫn phẳng nhất ở 0.88**: camera 1.48 cố định (đặt trước khi mái cao hơn) cắt mất phần mái, nên bằng chứng close-up bị nghiêng về tường/cửa.
- **Course của mặt mái timber còn nhẹ** ở zoom gameplay; pass sau chỉ nên tăng tương phản seam 1 bậc, không thêm chi tiết.
- **Bằng chứng engine của cypress yếu hơn của armory**: 3 prop cypress nằm sau sprite armory ở zoom close và ngoài khung ở zoom gameplay, nên ảnh engine chưa bắt được cây. Cypress được chứng minh bằng contract test + contact sheet; hành vi trong game không đổi (cùng canvas/anchor/topFraction). Việc tiếp theo: dump list prop runtime một lần rồi bắn lại camera đúng cây.
- **Mới 1 nhà + 1 cây**: các facility khác (hero quarters, restaurant, synthesis lab, cottage) vẫn dùng sprite thời P16 → chênh lệch thấy rõ cạnh armory mới; đó chính là slice kế tiếp.
- Không claim ngang Brown Dust 2: đây là cải thiện độ đọc/khối ở quy mô native, đánh giá bằng render thật, không so trực tiếp với asset thương mại.
