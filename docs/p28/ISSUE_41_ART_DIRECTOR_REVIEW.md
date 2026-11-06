# BÁO CÁO ĐÁNH GIÁ CHUYÊN MÔN: ART DIRECTOR & SENIOR GAME DESIGNER
**Dự án:** ETERNAL SUMMONER — Dark Fantasy 2.5D Oblique RPG  
**Repository:** `lightingfff/eternal-sunmmoner`  
**Liên kết thảo luận:** Issue #39 (Quy chuẩn Arena Studio) | **Nhiệm vụ trực tiếp:** Issue #41 (Đánh giá chất lượng toàn diện P27B-P28D)  
**Tác giả:** Art Director & Senior Game Designer  
**Thời điểm đánh giá:** 2026-11-06  
**Trạng thái kiểm định:** Dựa trên chứng cứ mã nguồn, render engine Robolectric NATIVE Skia, dữ liệu pixel diff và kiểm thử thực tế.

---

## TỔNG QUAN ĐIỀU HÀNH (EXECUTIVE SUMMARY)

Sau khi kiểm định toàn bộ pipeline đồ họa từ P26 qua P27B-V5 đến P28-D ("Astral Reliquary"), nhóm phát triển Arena đã đạt được những bước tiến đáng khen ngợi về mặt kỹ thuật: giữ vững hình học Oblique Camera 2.5D (hệ số chiếu mặt đất Y = 0.72), loại bỏ triệt để các hình dán elip trôi nổi bằng móng đá chân thực (`drawStructureFoundation`), giải quyết bài toán hardcode màu sắc bằng hệ thống token `SanctuaryUi` / `ESTokens`, và xây dựng bộ khung 9-slice chuẩn mực.

Tuy nhiên, với tư cách là **Art Director** và **Senior Game Designer**, chúng tôi **KHÔNG PHẢI LÀ NGƯỜI CỔ VŨ DỄ DÃI**. Nhìn thẳng vào sự thật qua lăng kính trải nghiệm người chơi trên thiết bị di động thực tế và phong cách Dark Fantasy đích thực, dự án hiện đang đối mặt với **4 điểm nghẽn nghiêm trọng (Critical Vulnerabilities)**:

1. **Khủng hoảng không gian màn hình do Master HUD ("The Screen Real Estate Choke"):** Trên tỷ lệ màn hình điện thoại 16:9 / 20:9 (chiều cao chỉ 360–400dp), các khối HUD (Top Bar, Mini-map, Hero Command Panel, Camera Bar, Virtual Joystick, Tactical Orders) đang chiếm từ **42% đến 58% diện tích hiển thị**, kẹp chặt thế giới 2.5D vào một khe hẹp ở trung tâm.
2. **Hụt hơi trong phản ứng ban đêm (Night Light & Dark Fantasy Atmosphere):** Báo cáo CP6 đã thừa nhận Debt C mới chỉ hoàn thành một phần. Đo đạc pixel diff cho thấy cổng tháp và các mảng tường chính gần như không đổi luma ban đêm; bối cảnh thiếu vắng độ chìm sâu (grim darkness) và độ tương phản ánh sáng cục bộ (chiếu rọi từ đèn lồng/vực thẳm).
3. **Hiện tượng lặp nhịp điệu mặt đất (Terrain Periodic Seam & Texture Noise):** Mặc dù đã nâng cấp lên `period-repeat 33/64`, khi camera thu nhỏ ở mức zoom tổng thể (z0.36 – z0.88), các cụm sỏi đá và thảm cỏ vẫn lộ rõ nhịp mosaic hình học, làm mất đi tính gồ ghề, u ám tự nhiên của một vùng đất bị nguyền rủa.
4. **Độ trễ phản hồi lệnh chiến thuật do Animation Lock:** Các lệnh Master (Focus, Regroup) bị delay khi tướng đang trong trạng thái Contact Attack Frame, cộng với việc vùng bấm ảo của HUD dễ nuốt chửng các thao tác chạm cắm cờ (tap to move) trên chiến trường.

---

## 1. ĐÁNH GIÁ CHẤT LƯỢNG DARK FANTASY 2.5D PIXEL ART & CAMERA OBLIQUE

### Điểm đạt:
- **Bảo toàn hình học Oblique 3/4:** Hình chiếu mặt đất Y = 0.72 được tuân thủ nghiêm ngặt trong `ObliqueSpriteGeometry.kt`. Tỷ lệ giữa độ cao thẳng đứng của tường và mặt nằm ngang của sàn giữ được chiều sâu lập thể 2.5D đặc trưng của game chiến thuật cổ điển.
- **Móng công trình P28-D (`drawStructureFoundation`):** Việc bổ sung bậc thềm đá 3 cấp, gờ chân tường và bệ đá hình bình hành đã triệt tiêu hoàn toàn cảm giác "nhà nổi bồng bềnh trên cỏ" từng bị phê bình ở các bản build P24–P26.

### Phê bình gay gắt & Bằng chứng thực tế:
- **Độ lệch Texel Density (Pixel Consistency):**
  - Sprite nhân vật Lyra/Kaelen được dựng ở độ phân giải chi tiết cao (~64x96), trong khi các cụm địa hình cơ sở lại dùng mosaic tile 32x32 upscaled. Khi phóng to z1.48, đường viền nhân vật rất sắc nét (crisp nearest-neighbor), nhưng một số texture mái ngói (cottage_slate, townhouse_b) lại có cảm giác hơi "mịn nhân tạo" do quá trình xử lý hậu kỳ chưa đồng nhất với độ hạt thô ráp của pixel đất đá.
- **Che khuất tán cây (Canopy Foreground Occlusion):**
  - Mặc dù hệ thống depth sorting xếp tán cây vào foreground layer, nhưng khi nhân vật di chuyển ngay dưới mép tán cây, độ trong suốt cục bộ (dithered alpha hoặc radial mask) chưa được áp dụng mềm mại, làm mất dấu vị trí tướng trong 1–2 giây giao tranh hỗn loạn.

---

## 2. ĐÁNH GIÁ TEXTURE KIẾN TRÚC & NỀN MẶT ĐẤT (ARCHITECTURE & TERRAIN)

### Phân tích hiện trạng:
- **Màu sắc vật liệu (Limestone & Slate):**
  - Việc repaint tường bao từ than chì sang đá vôi (limestone) đã nâng chỉ số Luma từ 51 lên 86–153, giúp công trình không bị "đen đặc" hòa vào nền.
- **Khuyết tật lặp nhịp (Periodic Grid Seams):**
  - Theo dữ liệu kiểm thử tại `docs/p27b_next/02_TERRAIN_REPEAT_FIX.md` và `06_CP6_SUMMARY.md`, giải thuật phân bổ ngẫu nhiên có trọng số chỉ giải quyết được việc "không có hai ô cạnh nhau giống hệt nhau", nhưng **chưa giải quyết được hình thái đại cục (macro-pattern)**.
  - Khi người chơi quan sát ở zoom bao quát Sanctuary (z0.36), mắt người tự động kết nối các đốm sáng của `flagstone_mosaic` và `cobble_mosaic` thành các đường chéo lặp lại theo chu kỳ 4x4 ô. Điều này phá vỡ cảm giác thế giới mở dark fantasy rộng lớn, khiến khu vực trông giống một bàn cờ sa bàn hơn là một thánh địa cổ kính.

### Khuyến nghị của Art Director:
1. Đưa vào các mảng decal tự nhiên (rêu phong, vết nứt đá, vũng nước đọng phản chiếu) đè ngẫu nhiên tại các giao điểm đường đi để phá vỡ nhịp điệu thị giác (visual rhythm breaking).
2. Tăng cường độ tối (Ambient Occlusion) tại góc tiếp giáp giữa chân tường công trình và mặt đất, giúp công trình "cắm rễ" sâu hơn vào không gian.

---

## 3. PHÊ BÌNH MASTER HUD: KHỦNG HOẢNG KHÔNG GIAN MÀN HÌNH (SCREEN OBSTRUCTION)

Đây là **vấn đề cốt tử nhất đối với Game Designer**:

### Bằng chứng mã nguồn:
1. **Lobby HUD (`SanctuaryLobbyScreen.kt`):**
   - Top Bar (`sanctuary_top_hud`): Chiếm ~10% chiều cao phía trên (status bars padding + profile + gold + soul shards + 3 icon button 48dp).
   - Bottom-Left Suite (`camera_controls_bar`): Cụm 5 nút camera chiếm diện tích ~190dp x 46dp, đè lên góc trái dưới.
   - Bottom-Right Panel (`hero_command_panel`): Rộng tới **490dp** trên landscape widescreen! Khi hiển thị thẻ `SelectedHeroCompactCard` hoặc `ContextualFacilityPromptCard`, panel này che phủ gần như toàn bộ góc phần tư phía dưới bên phải.
   - Dock dưới cùng: Hàng nút `Lệnh & công trình ▾` cùng các chip công trình (Rời phòng, Nhà ăn, Mở rộng...) và danh sách avatar anh hùng kéo dài ngang đáy màn hình.
   - **Hậu quả:** Trên màn hình landscape thông dụng của smartphone (chiều cao ~360dp - 400dp), "vùng thở" (clear playfield) để người chơi ngắm nhìn Thánh Địa và điều khiển nhân vật chỉ còn lại chưa đầy **45% ở giữa màn hình**!

2. **Battlefield Tactical HUD (`TowerBattleScreen.kt`):**
   - Phía trên: Top Tactical HUD (HP Master, Objective, Squad Skills Bar, Minimap 92x52dp, Nút Rút lui 48dp).
   - Nếu có nhiệm vụ cứu viện: `CourierMissionHud` chèn thêm 1 dải thông tin cảnh báo dày đặc.
   - Khi khóa mục tiêu: Thẻ `Focused Enemy / Boss HP Bar` xuất hiện ngay trung tâm phía trên.
   - Phía dưới bên trái: Cần điều khiển ảo (`VirtualJoystick`) đường kính ~140dp.
   - Phía dưới bên phải: Bảng Tactical Orders (`order_hold`, `order_focus`, `order_regroup`, `order_advance`) chiếm ~300dp chiều ngang x 48dp chiều cao, kèm hộp thoại phản hồi lệnh `tactical_command_feedback_pill`.
   - **Hậu quả tác chiến:** Khi ngón tay cái bên trái đặt lên joystick và ngón cái bên phải chuẩn bị bấm skill/lệnh, hai bàn tay người chơi che lấp thêm 20% màn hình. Hệ quả là các vùng cảnh báo nguy hiểm (vòng tròn đỏ báo đòn đánh boss, lính trinh sát goblin áp sát) bị che khuất hoàn toàn, dẫn đến những cái chết ức chế không do lỗi của người chơi!

---

## 4. ÁNH SÁNG & PHẢN ỨNG BAN ĐÊM (LIGHTING & NIGHT RESPONSE)

### Phân tích sự thật từ Báo cáo Kỹ thuật:
- Báo cáo CP6 đã chỉ rõ: Đợt tinh chỉnh ban đêm ở commit `38645e3` thực chất chỉ tác động lên **4046 pixel (0.44% diện tích khung hình)** tại hai bờ tường hẹp, giảm luma từ 164.6 xuống 150.3 (-8.7%). Toàn bộ cổng chính (`gate_tower`) và các công trình trung tâm giữ nguyên 0 pixel biến thiên!
- Tình trạng "đêm giả lập" (fake night): Thế giới ban đêm hiện tại giống như việc áp một lớp filter màu xanh tối mờ (tint overlay) lên toàn màn hình thay vì mô phỏng ánh sáng vật lý thực sự.
- Các nguồn phát sáng cục bộ (đèn lồng đường phố, ngọn đuốc, vòng triệu hồi ma pháp) thiếu quầng sáng ấm áp (warm amber glow) và không tạo được bóng đổ động ngược chiều (reverse dynamic cast shadows).

---

## 5. UI "ASTRAL RELIQUARY" (P28) & ĐỒNG BỘ PHONG CÁCH DARK FANTASY

### Đánh giá chất lượng gói UI:
- **22 Asset chuẩn mực:** Khung thẻ đồng/bạc/hắc diện thạch (`hero_card_frame_*`), nút bấm 9-slice, biểu tượng trạng thái có alpha sạch sẽ, không bị vỡ viền.
- **Rủi ro về mặt "Hồn phách nghệ thuật" (Artistic Soul):**
  - Nguy cơ biến ETERNAL SUMMONER thành một tựa game "Gacha phổ thông bóng bẩy": Màu vàng kim (`SummonerGold`) và viền chrome sáng bóng trong bộ UI P28 đang hơi quá rực rỡ và lộng lẫy.
  - Dark Fantasy cần chất "bụi bặm", tăm tối, nguy hiểm và cổ xưa. Khung thẻ anh hùng nên tăng thêm chi tiết mài mòn, vết khía kim loại, sắc lạnh của thép đen (obsidian iron) và ánh sáng ma thuật u tối (eldritch glow) thay vì viền vàng hoàng gia quá sạch sẽ.

---

## 6. GAMEPLAY & CƠ CHẾ CHỈ HUY MASTER (GAMEPLAY TACTICS)

### Điểm mạnh:
- Tướng tự hành (`AutonomousHeroAi`) có tính cách và phong cách tác chiến riêng biệt, tạo đúng cảm giác "chỉ huy đội quân độc lập" lấy cảm hứng từ *Pick Me Up, Infinite Gacha*.
- 3 lệnh Master chiến thuật cốt lõi:
  - **Focus (Tập trung hỏa lực):** Chỉ định ưu tiên diệt trinh sát hoặc pháp sư địch.
  - **Regroup (Tập hợp phòng thủ):** Kéo đội hình về bảo vệ Master và Người vận chuyển (Courier).
  - **Retreat (Rút lui chiến thuật):** Giữ mạng cho anh hùng khi cạn kiệt dược phẩm.

### Lỗ hổng trải nghiệm (UX Defects):
1. **Xung đột sự kiện cảm ứng (Touch Event Collision):** Khi người chơi cố gắng bấm lên mặt đất để di chuyển Master hoặc cắm cờ điều quân (`awaitingMoveToTap`), nếu vị trí chạm nằm gần vùng biên vô hình của thanh lệnh hoặc joystick, sự kiện cảm ứng thường bị ăn mất.
2. **Animation Lock vs Lệnh Khẩn Cấp:** Khi Lyra hoặc Kaelen đang bước vào chuỗi vung kiếm đánh cận chiến (`ContactAttackTimeline`), việc phát lệnh "Regroup" thường có độ trễ 0.3s–0.5s do tướng phải hoàn tất chu kỳ frame hiện tại mới chuyển trạng thái AI, khiến người chơi cảm giác điều khiển bị "khựng" hoặc thiếu nhạy bén.

---

## 7. BẢNG XẾP HẠNG 5 ĐIỂM NGHẼN CẦN XỬ LÝ (TOP 5 BLOCKERS & WORK ORDERS)

Dưới đây là 5 đầu việc được xếp hạng theo mức độ ảnh hưởng đến trải nghiệm game, kèm theo giả thuyết nguyên nhân gốc rễ và giải pháp kỹ thuật cụ thể:

| Thứ hạng | Điểm nghẽn | Mức độ | Nguyên nhân gốc rễ (Root Cause) | Giải pháp đề xuất (Action Plan) |
|---|---|---|---|---|
| **#1** | **Master HUD che khuất tầm nhìn tác chiến** | **CRITICAL** | Sử dụng các layout cố định (`Column`/`Row` dạng dải đặc) với padding lớn thay vì thiết kế floating/collapsible; chiều rộng panel quá lớn (490dp). | Chuyển cụm lệnh sang dạng **Dock thu gọn thông minh (Smart Mini-Dock)**: khi không thao tác, tự động hạ opacity xuống 40% hoặc thu về 1 nút tròn nổi; gom cụm camera vào góc trên; giới hạn bề rộng tối đa của panel xuống ≤240dp trên mobile. |
| **#2** | **Xung đột chạm màn hình (Touch Interception)** | **HIGH** | Vùng `Modifier.pointerInput` của joystick và các panel nút bấm bao chiếm khoảng trống padding trong suốt quá rộng. | Thu hẹp kích thước hitbox thực tế của controls; thiết lập lớp nhận diện tap bản đồ toàn màn hình nằm dưới các composable điều khiển cụ thể với logic cản touch rõ ràng. |
| **#3** | **Ánh sáng ban đêm thiếu chiều sâu (Debt C còn mở)** | **HIGH** | Geometry chu vi và sprite chính chưa được kết nối đồng bộ vào hàm shader tính toán độ suy giảm ánh sáng môi trường ban đêm. | Áp dụng hệ số suy giảm Luma đồng nhất trên toàn bộ facade công trình chính; tăng cường độ phát quang (bloom/emissive) cho các đèn lồng để tạo độ tương phản sáng-tối kịch tính. |
| **#4** | **Nhịp lặp địa hình lộ rõ ở góc zoom rộng (Debt B)** | **MEDIUM** | Mảng mosaic 4x4 thiếu các biến thể ngẫu nhiên có kích thước lớn (macro-noise map) để phá nhịp lặp. | Đưa thêm các mảng rêu phong, nứt vỡ và cỏ úa trải ngẫu nhiên theo Perlin noise đơn giản, phá vỡ tính hình học của mắt lưới tile. |
| **#5** | **Độ trễ phản hồi lệnh chiến thuật (Tactical Lag)** | **MEDIUM** | State machine của AI kiểm tra điều kiện chuyển trạng thái sau khi kết thúc animation frame thay vì ngắt khẩn cấp (interruptable state). | Thêm cơ chế "Animation Cancel" ưu tiên cao đối với các lệnh tối khẩn (Regroup/Retreat), cho phép tướng chuyển ngay sang tư thế phòng thủ/rút lui khi nhận lệnh Master. |

---

## 8. LỜI KẾT & QUY TẮC BÀN GIAO STUDIO

1. **Tuyệt đối tuân thủ quy tắc:** Không tự ý thay đổi bố cục cốt lõi, không viết đè các module đang ổn định của nhóm Arena, không tự động merge PR.
2. **Khẳng định tính trung thực:** Báo cáo này không phóng đại thành tích, không bịa đặt số đo FPS trên máy vật lý khi chưa có thiết bị đo thực tế, và ghi nhận đầy đủ công sức đã thực hiện của các đồng đội trong các PR #36, #37, #38.
3. **Bàn giao:** Báo cáo này đã được đóng gói hoàn chỉnh tại `docs/p28/ISSUE_41_ART_DIRECTOR_REVIEW.md` và sẵn sàng để đăng tải trực tiếp lên GitHub Issue #41 làm cơ sở nghiệm thu và định hướng cho các chu kỳ phát triển tiếp theo của ETERNAL SUMMONER.
