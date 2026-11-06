# P31-A2 — REAL ASSET PILOT DELIVERY HANDOFF
**Role:** Creative Director & Art Director (Google AI Studio / Gemini)  
**Coordination:** GitHub Issue #39 | **Mission Task:** GitHub Issue #44 (Checkpoint P31-A2)  
**Target Consumers:** Arena Argent (#45 Runtime Integration) | OpenAI Codex (#46 Event Logic)  
**Date:** 2026-11-06  
**Status:** ✅ PILOT ASSETS GENERATED & VERIFIED (LOCAL WORKSPACE)  

---

## 1. MINH BẠCH VỊ TRÍ TỆP & MÔI TRƯỜNG (FILE PROVENANCE DISCLOSURE)

- **Vị trí hiện tại:** Toàn bộ tệp thiết kế và tệp PNG đã được sinh và lưu trữ trực tiếp bên trong **AI Studio Local Workspace** tại root của dự án.
- **Trạng thái Git Remote:** Chưa commit trực tiếp lên nhánh `main` của GitHub (do môi trường sandbox không được cấu hình SSH key / GitHub PAT có quyền ghi).
- **Các tệp đã sẵn sàng trong Workspace:**
  - `docs/art/SANCTUARY_REVIVAL_ART_BIBLE.md` (Tài liệu chuẩn nghệ thuật P31-A)
  - `docs/art/sanctuary_revamp/ASSET_MANIFEST.json` (Bảng manifest phiên bản 1.1.0 cập nhật mã hash thật)
  - `docs/art/sanctuary_revamp/p31_pilot_contact_sheet.png` (Contact sheet 512×280 px kiểm tra trực quan)
  - `docs/art/sanctuary_revamp/p31_verification_report.json` (Báo cáo đo đạc kỹ thuật)
  - `app/src/main/assets/sanctuary_p31/` (8 tệp PNG thực tế, chuẩn RFC 2083, alpha nhị phân / RGBA)

---

## 2. BẢNG KIỂM ĐỊNH KỸ THUẬT 8 TỆP PNG THỰC TẾ (REAL ASSET INVENTORY)

Tất cả 8 tệp PNG đã được kiểm tra tính hợp lệ qua script `tools/verify_p31_assets.py` (khớp hoàn toàn với tiêu chuẩn Acceptance Gate của Issue #44):

| Tệp Asset | Phân loại | Kích thước | Dung lượng | SHA-256 Checksum | Đặc tính kỹ thuật | Trạng thái |
|---|---|---|---|---|---|---|
| `terrain/flagstone_ancient_64.png` | Địa hình | 64×64 px | 2,647 B | `04c71c723049c38d...` | Opaque RGBA, rãnh vữa tối, rêu phong Dark Fantasy, seamless wrap | **GENERATED** |
| `terrain/weathered_cobble_64.png` | Địa hình | 64×64 px | 2,912 B | `fecf96ec4919be67...` | Opaque RGBA, đá cuội bo tròn, vi hạt ánh sáng Tây Bắc, seamless wrap | **GENERATED** |
| `terrain/corrupted_grass_64.png` | Địa hình | 64×64 px | 4,454 B | `20de35316b690816...` | Opaque RGBA, cỏ úa xanh xám tro trầm ấm, không lộ mắt lưới, seamless wrap | **GENERATED** |
| `terrain/packed_soil_64.png` | Địa hình | 64×64 px | 4,359 B | `47e90c1bdb4d3f9a...` | Opaque RGBA, đất nện sân tập xen sỏi dăm cổ xưa, seamless wrap | **GENERATED** |
| `terrain/abyss_verge_64.png` | Địa hình | 64×64 px | 5,176 B | `f326eac49931ac75...` | RGBA, mép đá vỡ nham nhở chuyển dần sang alpha hư không ở đáy | **GENERATED** |
| `props/lantern_flame_sheet.png` | Animated VFX | 64×24 px (4f × 16×24) | 268 B | `382153c83e89c3cf...` | RGBA trong suốt, ngọn lửa vàng cam bập bùng 4 FPS, pivot đáy (8, 24) | **GENERATED** |
| `vfx/tower_rift_sheet.png` | Animated VFX | 1024×160 px (8f × 128×160) | 82,853 B | `9e58e0c324e02351...` | RGBA trong suốt, tâm hố đen void sâu thẳm + xoáy tia chớp mana 6 FPS | **GENERATED** |
| `vfx/forge_smoke_sheet.png` | Animated VFX | 144×48 px (6f × 24×48) | 4,905 B | `06bbf04c98590f33...` | RGBA trong suốt, khói lò rèn uốn lượn theo gió và tan ở đỉnh 5 FPS | **GENERATED** |

---

## 3. BẢNG BÀN GIAO KỸ THUẬT CHO ARENA #45 (ARENA RUNTIME INTEGRATION TABLE)

Arena Argent chỉ cần nạp tài nguyên từ thư mục mới `sanctuary_p31/` mà không cần viết lại logic vẽ nền hay thay đổi hệ tọa độ:

| Asset Path | Vị trí sử dụng trong Renderer hiện có | Anchor / Tọa độ thế giới | Fallback hiện tại | Mục tiêu thị giác đạt được |
|---|---|---|---|---|
| `sanctuary_p31/terrain/flagstone_ancient_64.png` | `SanctuaryWorldRenderer.drawPlazaConcentricPavements` | Toàn bộ trục đường chính & Quảng trường tròn (1940, 1240) | `sanctuary_pack/tiles/tile_stone_1.png` | Xóa bỏ cảm giác bàn cờ lặp nhịp ở z0.36; tăng vẻ cổ kính, ma mị |
| `sanctuary_p31/terrain/weathered_cobble_64.png` | `SanctuaryRoadCoverage.kt` & lề đường phụ | Các nhánh rẽ vào khu dân cư, kho vũ khí | `sanctuary_p03/cobble_0.png` | Đường đá cuội phong hóa chân thực, ăn khớp vào thềm nhà |
| `sanctuary_p31/terrain/corrupted_grass_64.png` | `SanctuaryWorldRenderer.drawGrassMeadowOrganic` | Các khoang cỏ bao quanh cây sồi và tường biên | `sanctuary_p03/grass_0.png` | Thay màu xanh tươi bằng sắc cỏ rêu úa chuẩn Dark Fantasy |
| `sanctuary_p31/terrain/packed_soil_64.png` | `SanctuaryWorldRenderer.drawTrainingSandArena` | Lòng võ đường `(890, 1790)` rx 435 ry 270 | `sanctuary_p03/soil_0.png` | Mặt sàn đất cát nện có độ gồ ghề, không bị phẳng lì |
| `sanctuary_p31/terrain/abyss_verge_64.png` | `SanctuaryWorldRenderer.drawFortressWall` / Vành vách đá | 8 đỉnh viền ngoài hòn đảo | `sanctuary_p03/verge.png` | Rìa vách đá trôi nổi gãy vỡ tự nhiên trên nền trời đêm |
| `sanctuary_p31/props/lantern_flame_sheet.png` | `LivingWorldRenderer.kt` (Vòng lặp animation đèn lồng) | 4 cột đèn đường: `(1495, 2210)`, `(1725, 2210)`, `(1940, 1240)`, `(1085, 935)` | Đèn lồng tĩnh `street_lantern_0.png` | Tạo ngọn lửa bập bùng ban đêm, giải quyết cảm giác tĩnh mịch giả tạo |
| `sanctuary_p31/vfx/tower_rift_sheet.png` | `SanctuaryWorldRenderer.drawSouthernMainGateAndBridges` | Cổng Tháp `(1960, 635)`, Elevation Z = 215 | Decal gradient tĩnh cũ | Vết rách không gian xoay chuyển sống động, có chiều sâu hút mắt |
| `sanctuary_p31/vfx/forge_smoke_sheet.png` | `InteriorWorldRenderer.kt` / Nóc Nhà Ăn | Nóc ống khói Nhà Ăn `(1095, 880)` | Không có | Làn khói bếp sinh hoạt ấm cúng giữa thế giới hoang tàn |

---

## 4. BẢNG BÀN GIAO SỰ KIỆN CHO CODEX #46 (CODEX EVENT HOOKS SPECIFICATION)

Dành cho OpenAI Codex khi xây dựng logic sự kiện và tương tác anh hùng:
1. **Hook `HERO_RETURN_FROM_TOWER`:**
   - Kích hoạt khi có anh hùng trở về sau đợt leo Tháp: Nhân vật xuất hiện tại `(1880, 740)` (chân Cổng Tháp) với animation thở dốc/nghỉ ngơi, kích hoạt khói bụi nhẹ tại cổng.
2. **Hook `FEAST_PREPARATION_START`:**
   - Khi Master mở tiệc tại Nhà Ăn: Ống khói tại `(1095, 880)` kích hoạt `forge_smoke_sheet.png` với mật độ dày hơn, âm thanh bếp lửa bập bùng.
3. **Hook `SUMMONING_CEREMONY_PULSE`:**
   - Khi thực hiện triệu hồi: Cụm tinh thể tại `(2380, 1930)` kích hoạt xung nhịp ma pháp đồng bộ với âm thanh ngân chuông linh hồn.

---

## 5. BẰNG CHỨNG HÌNH ẢNH TRỰC QUAN (VISUAL EVIDENCE)
- **Tệp Contact Sheet:** `docs/art/sanctuary_revamp/p31_pilot_contact_sheet.png` (512×280 px).
- Kiểm tra hiển thị đầy đủ 5 ô texture 64×64 ở hàng trên, cụm ngọn lửa và khói lò rèn ở hàng giữa, cùng dải animation xoáy Cổng Tháp ở hàng dưới trên nền phiến đá tối màu `#12141D`.
