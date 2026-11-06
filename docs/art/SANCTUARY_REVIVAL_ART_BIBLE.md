# ETERNAL SUMMONER — SANCTUARY REVIVAL ART BIBLE (P31-A)
**Role:** Creative Director & Art Director (Google AI Studio / Gemini)  
**Coordination:** GitHub Issue #39 | **Mission Task:** GitHub Issue #44 (P31-A)  
**Sibling Tasks:** Arena #41 / PR #42 & #45 (P31-B Runtime Integration) | OpenAI Codex #43 & #46 (P31-C Event Logic)  
**Baseline & Target:** `main` @ `d9c330f` | **Engine Constraints:** Kotlin/Compose Canvas, Oblique 2.5D (Ground Y 0.72)  
**Status:** PROPOSED & AUTHORIZED DESIGN SPECIFICATION — READY FOR ARENA/CODEX HANDOFF  

---

## 1. TỔNG QUAN SỨ MỆNH NGHỆ THUẬT (ARTISTIC VISION & CREATIVE PILLARS)

### 1.1. Hiện trạng & Thách thức
Người chơi và chủ dự án phản hồi rằng Sảnh Chờ (Sanctuary Lobby) hiện tại:
- Trông còn **bland (nhạt nhòa, đơn điệu)**, thiếu sức sống và bầu không khí ma mị của một thế giới huyền bí đen tối.
- Các công trình và cụm địa hình rời rạc, có cảm giác sprite bị "dán" lên nền phẳng thay vì cắm rễ vào một hòn đảo đá cổ xưa.
- Giao diện người dùng (Master HUD) quá cồng kềnh, che khuất thế giới 2.5D, làm mất đi 30 giây ấn tượng đầu tiên của một người triệu hồi bước vào căn cứ tiền đồn.

### 1.2. Ba Trụ cột Sáng tạo (Creative Pillars)
1. **The Forsaken Astral Citadel (Thánh Địa Tàn Tích Hư Không):**
   - Đảo Thánh Địa không phải là một thành phố hoàng gia xa hoa lộng lẫy, mà là một **pháo đài phong ấn cổ đại trôi nổi giữa ranh giới hư không (The Astral Void)**.
   - Chất liệu chủ đạo: Đá vôi phong hóa (weathered limestone), đá phiến thô xám đen (black slate), gỗ sồi hun khói cằn cỗi, rêu phong ma mị, và những vết nứt phát quang cổ ngữ.
2. **Atmospheric Contrast & Living Grimdark (Ánh Sáng Đối Lập & Thế Giới Sống):**
   - Sự giao thoa tương phản giữa **sắc ấm hổ phách (warm amber/torchlight)** từ sinh hoạt của con người và **sắc lạnh âm u (cold celestial azure / void violet)** từ Cổng Tháp và các vết rách không gian.
   - Môi trường không tĩnh lặng: ngọn đuốc lay động theo gió hư không, khói bốc lên từ ống khói nhà ăn, lá cây rụng chầm chậm, và các anh hùng có nhịp thở (breathing/idle) cùng lịch trình sinh hoạt cụ thể.
3. **Tactical & Functional Clarity (Rõ Ràng & Tinh Gọn):**
   - Mọi yếu tố nghệ thuật phục vụ trực tiếp cho lối chơi. Cổng Tháp (Tower Gate) là mốc thị giác tối cao (dominant landmark), Quảng trường trung tâm (Plaza) là ngã rẽ định hướng, và HUD đóng vai trò người hầu thầm lặng — tinh giản tối đa, nhường toàn bộ sân khấu cho thế giới game.

---

## 2. PHÂN CẤP THỊ GIÁC & QUY HOẠCH KHÔNG GIAN (VISUAL HIERARCHY & SPATIAL ANCHORS)

### 2.1. Bất biến Hình học & Camera 2.5D Oblique (Strict Invariants)
- **Tọa độ thế giới:** 3200 × 2760 px.
- **Hệ số Oblique:** Trục đứng giữ nguyên 1.0 (sprite thẳng đứng giữ nguyên chiều cao thực); trục mặt đất Y co lại chính xác **0.72** (`ObliqueCamera.OBLIQUE_Y_SCALE = 0.72f`).
- **Tỷ lệ elip nền:** 0.66 (`GROUND_ELLIPSE_RATIO`).
- **Nearest-Neighbor Scaling:** Giữ nguyên FilterQuality.None để đảm bảo độ sắc cạnh (pixel-crispness) của từng hạt pixel, tuyệt đối không làm mờ nội suy (bilinear blur).
- **Ranh giới đảo (8-Vertex Polygon):** Giữ nguyên tuyệt đối 8 đỉnh tường thành từ (980, 340) qua (2240, 340) đến (300, 880) theo `docs/SANCTUARY_BASELINE.md`. Không mở rộng hay bóp méo hình học va chạm.

### 2.2. Phân Cấp Điểm Nhấn Thị Giác (Landmark Hierarchy)
1. **Bậc 1 — Ngọn Hải Đăng Tử Thần (The Dominant Eye-Anchor): CỔNG THÁP (TOWER GATE)**
   - *Tọa độ trung tâm:* (1960, 635) | Bán kính tương tác: 120 px.
   - *Quy cách thẩm mỹ:* Vòm đá hắc diện thạch khổng lồ sừng sững, vươn cao nhất bản đồ (Elevation Z = 215). Lỗ hổng cổng không phải là hình elip dán đè mà là một **Vết Rách Không Gian (Astral Rift Void)** sâu thẳm, phát ra các xung nhịp ma pháp xanh thẫm chu kỳ 3.2 giây. Chân cổng có bậc thềm đá 5 cấp ăn khớp trực tiếp vào trục đường chính.
2. **Bậc 2 — Trái Tim Hội Tụ: QUẢNG TRƯỜNG CỔ NGỮ (CENTRAL RUNIC PLAZA)**
   - *Tọa độ trung tâm:* (1940, 1240) | Bán kính: 555 px.
   - *Quy cách thẩm mỹ:* 4 vòng tròn đồng quy bậc đá bậc thang, khắc 4 ấn ký nguyên tố (Sakura, Solar Gold, Amber, Astral Azure). Bề mặt lát đá cổ thạch xen kẽ các vết rạn nứt có rêu xanh bám rễ. Đây là điểm phân luồng tự nhiên dẫn tới mọi phân khu.
3. **Bậc 3 — Vòng Sinh Hoạt & Triệu Hồi (Functional Orbit):**
   - **Điện Triệu Hồi (Summoning Hall):** (2380, 1930) — Mái vòm bát giác xanh sẫm, tháp chuông linh hồn, bệ đá triệu hồi với ánh sáng ma thuật tím huyền bí.
   - **Khu Nghỉ Ngơi (Hero Quarters):** (735, 875) — Mái ngói nâu rêu, dây leo tường đá, ban công gỗ hun khói.
   - **Nhà Ăn (Restaurant / Dining Hall):** (1085, 935) — Ống khói bốc làn khói xám nhẹ, đèn lồng ấm cúng trước cửa, thùng rượu gỗ sồi tựa chân tường.
   - **Kho Vũ Khí (Armory) & Viện Nghiên Cứu (Synthesis Lab):** (1270, 585) — Lò rèn bễ lửa tí tách than hồng, giá kiếm sắt mài mòn, bàn thí nghiệm tinh thể xanh ngọc.
   - **Võ Đường (Training Ground):** (890, 1790) — Sàn cát hữu cơ sẫm màu, cọc gỗ tập luyện quấn dây thừng, bù nhìn rơm có vết kiếm chém chân thực.

---

## 3. BỘ TIÊU CHUẨN ĐỒ HỌA MẶT ĐẤT 64×64 (GROUND & TERRAIN REVOLUTION)

### 3.1. Khắc Phục Triệt Để Khuyết Tật Lặp Nhịp (Periodic Seam Annihilation)
- **Vấn đề cũ (Debt B):** Chu kỳ 33/64 ô vẫn tạo ra các vệt chéo mắt lưới khi quan sát ở góc rộng z0.36.
- **Giải pháp 64×64 Mới (P31 Macro-Tiles):**
  - Mở rộng lưới đơn vị từ 32px lên **64×64 pixel chuẩn**.
  - Áp dụng nguyên tắc **Sub-pixel Organic Dithering**: Các cạnh biên của ô đất đá không cắt thẳng theo đường kẻ mà lượn sóng tự nhiên (irregular edge jittering ±2px).
  - Phân bổ 4 biến thể vi mô (A, B, C, D) cho mỗi loại bề mặt kèm theo **Decal Overlays (Lớp phủ ngẫu nhiên không lặp)**:
    1. `flagstone_ancient_64`: Đá tảng lát đường cổ xưa, rãnh vữa tối màu (`#1A1C23`), rêu xanh ẩm len lỏi ở các góc nứt.
    2. `cobble_path_64`: Đá cuội mài mòn, chuyển tiếp tự nhiên ra hai bên lề đường (curb apron).
    3. `corrupted_grass_64`: Cỏ úa xanh lục sẫm pha sắc xám tro (`#2C3E35` hòa cùng `#1E2621`), điểm xuyết cỏ ba lá tăm tối.
    4. `packed_soil_64`: Đất thịt nện chặt, rải rác đá dăm và rễ cây trồi lên mặt đất.
    5. `abyss_verge_64`: Mép đảo tiếp giáp hư không, đá vỡ lởm chởm, rễ cây đứt gãy lơ lửng, tạo độ sâu thăm thẳm.

### 3.2. Chân Tường & Móng Công Trình (Building Footing & Apron Integration)
- Mọi công trình khi đặt xuống đất bắt buộc phải có **Lớp Móng Đá Tự Nhiên (Stone Plinth & Apron)**:
  - Bậc tam cấp bằng đá mài mòn ăn khớp từ sàn nhà xuống mặt đường.
  - Vạt cỏ úa và rêu bám quanh chân tường (contact occlusion band) có độ cao 4–6 pixel, màu sẫm (`#12141A`), tạo cảm giác trọng lượng công trình đè thật lên mặt đất.

---

## 4. BẦU KHÔNG KHÍ, ÁNH SÁNG & PHẢN ỨNG BAN ĐÊM (ATMOSPHERICS & DUAL-LIGHT)

### 4.1. Triết Lý Ánh Sáng Kép (Dual-Tone Chiaroscuro)
- **Ánh Sáng Ban Ngày (Noon / Clear Dawn):**
  - Hướng sáng chính: **Góc Tây Bắc (Góc trên bên trái, ~135 độ)** theo chuẩn Oblique.
  - Mặt trước và mặt trên của mái nhà nhận ánh sáng chính (`Luma 135–150`). Mặt sườn phía Đông Nam nhận bóng đổ bản thân (`Luma 70–85`).
- **Ánh Sáng Ban Đêm (Deep Night 00:00 - 04:00):**
  - *Giải quyết dứt điểm Debt C:* Toàn bộ bề mặt tường và mái nhà phải giảm luma thực sự xuống **35% - 45%** so với ban ngày (chìm vào bóng đêm xanh đen `#0C0E17`).
  - *Nguồn sáng điểm (Local Emissive Glow):*
    - **Đèn lồng đường phố & Đuốc (Amber Lanterns):** Phát sáng bán kính 48–64px, tâm sáng ấm `#F59E0B`, quầng sáng mờ `#78350F` alpha 0.4.
    - **Cổng Tháp & Vòng Triệu Hồi (Astral Mana Core):** Phát sáng xanh huyền bí `#38BDF8` và tím ma thuật `#A855F7`, có xung nhịp nhấp nháy nhẹ nhàng (sine pulse).
  - *Bóng đổ ban đêm:* Đèn lồng tạo bóng đổ cục bộ ngược hướng nguồn sáng, mang lại chiều sâu thị giác điện ảnh.

### 4.2. Hiệu Ứng Môi Trường Động (Living VFX) Theo 3 Cấp Cấu Hình
- **LOW:** Hạt tĩnh, không vẽ sương mù chuyển động; đèn lồng bật/tắt theo logic ngày đêm không có quầng sáng phụ.
- **BALANCED (Khuyên dùng trên Android):**
  - Khói bốc từ ống khói nhà ăn (4 hạt sprite tái sử dụng, bay lên và tan dần).
  - Đốm sáng đom đóm hư không (void motes) lập lòe quanh Cổng Tháp (tối đa 8 hạt).
  - Cờ rách trên tháp canh lay nhẹ theo chu kỳ 2 frame.
- **HIGH:** Bổ sung lớp sương mù mỏng (thin mist layer) trôi lững lờ dưới chân vách đá hư không, hiệu ứng lá rụng rơi chéo góc theo hướng gió Oblique.

---

## 5. ĐẶC TẢ ANIMATION & SPRITE SHEET (ACTORS & PROPS)

### 5.1. Quy chuẩn Khung Hình & Điểm Neo (Rigid Coordinate Standards)
- **Pivots & Foot Anchors:** Tọa độ neo của mọi nhân vật và prop luôn đặt tại **Tâm Đáy (Bottom-Center, x = width/2, y = height)**. Chân của sprite tiếp xúc chính xác tại mặt phẳng Y oblique.
- **Tốc độ khung hình (Animation FPS):**
  - Routine Idle/Breathing: 4–6 FPS (nhẹ nhàng, không giật lag).
  - Run/Walk Cycle: 8–10 FPS.
  - Cảnh báo/Chiến đấu (Action/Attack): 10–12 FPS (tiếp xúc va chạm tại đúng contact frame).

### 5.2. Danh Mục Animation Cần Bàn Giao
1. **Linh Hồn Cổng Tháp (Tower Gate Portal Core):**
   - Kích thước: 128×160 px | 8 frames loop liên tục | Alpha trong suốt.
   - Hiệu ứng: Tâm xoáy đen sâu hút vào trong, các tia chớp ma pháp xanh lam lượn vòng quanh vòm đá.
2. **Ngọn Đuốc & Đèn Lồng (Amber Torch & Street Lantern Flame):**
   - Kích thước: 16×24 px | 4 frames loop | Sắc màu: Đỏ cam → Vàng hổ phách.
3. **Khói Ống Khói Nhà Ăn (Chimney Hearth Smoke):**
   - Kích thước: 24×48 px | 6 frames loop mượt | Khói tan dần ở đỉnh.
4. **Viên Tinh Thể Triệu Hồi (Summoning Crystal Pulse):**
   - Kích thước: 32×48 px | 6 frames loop | Phát xung ánh sáng tím nhạt mỗi 2.4s.
5. **Micro-Animations Cho Anh Hùng (Sanctuary Routine Idles):**
   - **Kaelen (Hiệp sĩ):** Đứng canh gác nghiêm cẩn, thỉnh thoảng chỉnh lại bao tay hoặc chống kiếm xuống sàn đá (4 frames).
   - **Lyra (Cung thủ):** Ngồi vắt vẻo trên bờ tường đá, ngước nhìn bầu trời hư không hoặc kiểm tra dây cung (4 frames).
   - **Vesper (Pháp sư):** Đọc sách phép cổ bay lơ lửng, bàn tay phát ra luồng sáng mana mỏng (6 frames).

---

## 6. KHÁI NIỆM TRẢI NGHIỆM NGƯỜI CHƠI & GIAO DIỆN "DARK RELIQUARY" (UX & MINIMAL HUD)

### 6.1. Quy Tắc "5 Giây Đầu Tiên" (The 5-Second First Impression)
Khi người chơi vượt qua màn hình tải và bước vào Thánh Địa:
1. **Camera Framing:** Điểm hội tụ ban đầu đặt tại trung tâm Quảng trường (1940, 1240), hướng nhẹ về Cổng Tháp phía trên. Người chơi ngay lập tức nhìn thấy ngọn tháp hùng vĩ và cổng ma thuật đang xoay chuyển.
2. **Sự Hiện Diện Của Nhân Vật:** Các anh hùng trong đội hình xuất hiện tại các vị trí sinh hoạt tự nhiên (Lyra ở gờ đá, Kaelen bên cạnh cọc tập, NPC thương nhân mở sạp), không đứng tụm một chỗ như robot.
3. **Không Bị Tấn Công Bởi Popup:** Tuyệt đối không mở modal hay popup che kín màn hình trong 5 giây đầu.

### 6.2. Thiết Kế Master Mini-Dock (Giải Pháp Triệt Tiêu "Nạn Che Màn Hình")
Thay thế toàn bộ các panel đặc chiếm 490dp bằng kiến trúc giao diện **Floating Glass & Weathered Iron (Kính Đen & Sắt Phong Hóa)**:
- **Thanh Master Chip Tinh Gọn (Góc trên bên trái):**
  - Chiều cao gói gọn trong **36dp** (thay vì 64dp).
  - Nền kính bán trong suốt (alpha 0.75), viền sắt đen mài mòn điểm xuyết đinh tán cổ.
  - Hiển thị: Avatar tròn 28dp, Cấp độ, Vàng & Mảnh Hồn dạng pill badge mini.
- **Thanh Điều Khiển Camera Thu Gọn (Góc dưới bên trái):**
  - Nút tròn nổi (Floating Action Pills) 36×36dp, tự động mờ xuống alpha 0.3 khi người chơi không chạm vào sau 3 giây.
- **Smart Contextual Action Chip (Thay cho Hero Command Panel 490dp khổng lồ):**
  - Khi người chơi chạm vào một công trình hoặc anh hùng, chỉ có **MỘT CHIP HÀNH ĐỘNG DUY NHẤT** (rộng tối đa 180dp, cao 44dp) nổi lên phía trên đầu đối tượng đó (World-Space Floating Anchor).
  - *Ví dụ:* Chạm vào Cổng Tháp → Hiện chip gọn gàng: `[ ⚔️ Vào Tháp Tầng 1 ]`. Chạm ra ngoài → Tự động thu biến mất.
  - **Kết quả:** Trả lại hơn **75% diện tích màn hình** cho người chơi thưởng thức đồ họa 2.5D!

---

## 7. ĐỊNH HƯỚNG ÂM THANH MÔI TRƯỜNG TỰ NHIÊN (OPTIONAL SFX & AMBIENCE SPEC)

Dành cho lộ trình hoàn thiện âm thanh không phụ thuộc bên thứ ba:
- **Gió Hư Không (Astral Chasm Ambience):** Tiếng gió rít trầm ấm, chậm rãi, tạo cảm giác trôi nổi giữa không gian vô tận.
- **Tiếng Đe Rèn (Distant Smithing Clang):** Tiếng búa gõ đe vang nhẹ từ Kho Vũ Khí (ngắt quãng 8–12s).
- **Âm Vang Cổng Ma Pháp (Portal Hum):** Tiếng năng lượng dao động trầm thấp (low frequency oscillation ~55Hz) khi camera tiến gần Cổng Tháp.
- **Bước Chân Trên Đá (Footsteps on Flagstone):** Tiếng gõ đế giày giòn tan, khô ráo khi nhân vật đi trên đá lát; tiếng sột soạt êm dịu khi đi qua cát võ đường.

---

## 8. HỢP ĐỒNG BÀN GIAO CHO ARENA (PR #45) & CODEX (PR #46)

### Bảng Phân Quyền Sở Hữu (Ownership Matrix)
- **Gemini (Creative Director):** Cung cấp toàn bộ Art Bible, Asset Manifest, thông số kỹ thuật, bảng màu, mẫu thiết kế UI/UX và tài liệu kiểm định.
- **Arena (Technical Artist & Engine Engineer - Issue #45):** Nhận asset theo đúng path, cập nhật `SanctuaryWorldRenderer.kt`, `LivingWorldRenderer.kt`, và tinh chỉnh bố cục Compose theo spec của Master Mini-Dock.
- **Codex (Gameplay & Event Architect - Issue #46):** Nhận trigger coordinates, gắn kết logic sự kiện anh hùng trở về từ Tháp, phản ứng lời thoại bối cảnh vào `ManhwaStoryData.kt` và `TowerCombatEngine.kt`.

### Đánh Nhãn Minh Bạch (Status Verification)
- **`[GENERATED]`**: Tài liệu Art Bible, Danh mục Asset Manifest chuẩn mực 64px, Bảng thông số tọa độ và kịch bản tương tác.
- **`[PROPOSED]`**: Bộ texture mặt đất 64×64 nâng cấp và thiết kế Master Mini-Dock thu gọn để Arena tích hợp.
- **`[VERIFIED]`**: Các ràng buộc bất biến (8-đỉnh đảo, camera 0.72, nearest-neighbor, và cấu trúc móng đá P28-D) đã được đối chiếu chính xác với mã nguồn `main`.
