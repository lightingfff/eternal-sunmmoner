# ETERNAL SUMMONER — Visual Reference Bible v1

**Nguồn**: 8 ảnh tham khảo từ chủ dự án (REF01..08) · **Mục đích**: chuẩn thiết kế chung cho P26C+ và P27+ · **Không commit ảnh bản quyền vào repo**, không dùng ảnh tham khảo làm asset/game background.

**Nguyên âm**: ảnh là NGUỒN THAM KHẢO (khung, khối, thứ bậc thị giác, tương phản sáng, bố cục chức năng), KHÔNG phải đích copy pixel-per-pixel. Tạo nhân vật mới, logo mới, biểu tượng mới, hoa văn mới — không vẽ lại nhân vật tham khảo, không sao chép bảng chữ/lời thoại bản quyền.

---

## REF01 — Cổng ma thuật xanh, năng lượng xoáy, vòm đá lớn

**Tệp tham khảo**: images(3).jpg

**Đặc trưng thiết kế**:
- Vòm/cửa khổng lồ bằng đá, trung tâm xanh lam lộng lẫy, năng lượng xoáy
- Tia sáng tập trung, bầu không khí bí ẩn
- Nhân vật đứng/lơ lửng trước cổng làm thước đo quy mô
- Đá có khối: jamb/arch/keystone/intrados, hốc sâu, đèn/ruins
- Ánh sáng xanh gắn với lỗ mở, phát sáng từ nét rune, rìa rift, ngưỡng, mặt đá hứng sáng
- Vùng void tối bên trong cổng

**Nơi áp dụng**: Tower Gate trong Sanctuary; điểm hội tụ 4–5s cutscene sau khi click; hiệu ứng cổng kích hoạt/đóng/mở.

**KHÔNG sao chép**: Không tạo oval dán lên tường; không biến void thành quả cầu sáng; không dùng màu xanh neon.

**Asset/callsite cần audit**: `SanctuaryWorldRenderer.drawSouthernMainGateAndBridges` (gate z1.48), local lights gần gate, portal rift halo (P24/P26 đã bảo vệ rift tối).

**Tiêu chí thị giác**: Screenshot overview z0.88, close-up z1.48, cutscene 0s/1s/2s/3.5s/5s — hình phải đọc ra "cổng lớn đang mở" thay vì "vòng ellipse sáng nổi".

**P26C liên quan**: Chỉ kiểm tra độ gắn kết bầu trời–cổng–đảo (sky color với gate glow). Không mở lại đại phẫu asset P25/P26.

---

## REF02 — Thông báo fantasy trong khung trang trí

**Tệp**: image(20261103-103624).png

**Đặc trưng**: Bảng thông báo nhỏ, cạnh trang trí cổ điển, typography tương phản, sự kiện quan trọng.

**Nơi áp dụng**: System Alert, Mission Cleared, Hero Returned, Rescue Consequence.

**KHÔNG sao chép**: Không làm tấm đen to che nửa màn hình; dùng panel gọn, nền kính tối trong mờ/parchment mỏng, viền kim loại cổ; font rõ trên Android, ngắt dòng thích nghi, animation xuất hiện/biến mất không chặn hành động.

**Asset/callsite cần audit**: UI alert system (P27).

**Tiêu chí**: Thông báo đúng sự kiện game thật, safe area portrait/landscape, UI vẫn thấy map và nút Master.

---

## REF03 — Thẻ anh hùng chân dung, khung kim loại cổ, sao

**Tệp**: Main_character_-_Han_Islat.webp

**Đặc trưng**: Thẻ dọc với chân dung làm trung tâm, khung chạm khắc dạng vòm, bảng tên dưới, biểu tượng sao/bậc.

**Nơi áp dụng**: Hero Registry hồ sơ, summon reward, Character Details, hero result.

**KHÔNG sao chép**: Không dùng tên hoặc khuôn mặt nhân vật tham khảo trong bản phát hành; không dùng ảnh manhwa làm data thật.

**Asset/callsite cần audit**: Hero card UI (P27), portrait assets.

**Tiêu chí**: Ít nhất 3 hero có UI thật, không dùng ảnh manhwa làm data thật; chữ và sao đọc được trên màn hình nhỏ.

**P26C liên quan**: Không liên quan trực tiếp (P27). Ghi lại làm chuẩn cho sau.

---

## REF04 — Party 5 anh hùng thẻ đồng bộ

**Tệp**: images(4).jpg

**Đặc trưng**: Bố cục 5 thẻ thành party, khung đồng bộ, sự khác nhau giữa chân dung/cấp độ/icon sao; khoảng trắng có chủ ý.

**Nơi áp dụng**: Party Formation / Team Edit; chuẩn hóa tỷ lệ card, slot 1..5, role colors, hero readiness, morale/fatigue/injury, power và synergy.

**KHÔNG sao chép**: Nếu gameplay chưa hỗ trợ 5 hero, không giả 5 hero có thể ra trận; hiện đúng giới hạn engine, slot khóa ghi rõ.

**Asset/callsite cần audit**: Party system (P27).

**Tiêu chí**: Mở party từ hub, đổi vị trí hero, lưu + restart, xác nhận dữ liệu và ảnh không méo.

**P26C liên quan**: Không liên quan trực tiếp (P27). Ghi lại làm chuẩn cho sau.

---

## REF05 — Loading screen phép thuật xanh / logo / progress

**Tệp**: image(20261103-103749).png

**Đặc trưng**: Nền xanh siêu nhiên có chiều sâu, tiêu điểm logo trung tâm, thanh loading fantasy mảnh.

**Nơi áp dụng**: Game title, load/transition vào world, chuyển cảnh khi Enter Tower.

**KHÔNG sao chép**: Không sao chép logo/chữ tham khảo; viết logo ETERNAL SUMMONER nguyên bản; loading indicator phải bám tiến độ thực hoặc hoạt ảnh chờ gắn nhãn trung thực; không giả % tải.

**Asset/callsite cần audit**: Loading/title screen (P27).

**Tiêu chí**: Launch/guest/save/re-enter không treo loading, không lỗi nền đen đột ngột.

**P26C liên quan**: Không liên quan trực tiếp (P27). Ghi lại làm chuẩn cho sau.

---

## REF06 — Toà nhà tròn bằng đá, cửa vòm, bậc thẻm, ánh sáng lạnh

**Tệp**: Synthesis_Chamber.webp

**Đặc trưng**: Khối đá hình trụ/mái cong phân tầng, cột đá, cổng vòm sâu, cầu thang rõ, điểm sáng lạnh tại cửa.

**Nơi áp dụng**: Summoning Hall / Synthesis Lab / công trình dịch chuyển; cảm giác kiến trúc.

**Nguyên lý**: Mái nằm trên tường, tường chịu mái, cửa có hốc sâu, nền móng chạm đất, bóng tiếp xúc đúng camera. Tôn trọng Oblique 3/4 y=0.72, sprite/anchor/door triggers, path và map cũ.

**KHÔNG sao chép**: Không sao chép tòa nhà nguyên bản; P25/P23 đã làm — không vẽ lại tràn lan nếu kết quả mới không vượt baseline.

**Asset/callsite cần audit**: Sanctuary facilities, door triggers, anchors.

**Tiêu chí**: Zoom 0.88/1/1.48, mặt trên-mặt hông-mặt trước có bậc value rõ, không thấy khe alpha cỏ xuyên nhà, cửa trùng trigger.

**P26C liên quan**: Đánh giá độ gắn kết bầu trời–cổng–đảo (sky color với kiến trúc đá).

---

## REF07 — Toàn cảnh khu lobby / quần trường / sân tập / tường thành

**Tệp**: images(5).jpg

**Đặc trưng**: Góc nhìn cao thấy các trục kiến trúc, sân rộng, khu chức năng, lớp tường thành; cảm giác khu căn cứ được thiết kế có mục đích.

**Nơi áp dụng**: Sanctuary overview, map expansion lvl5, đường đi, vị trí công trình.

**KHÔNG sao chép**: Giữ hình đảo, các tòa nhà, footprint, trigger, camera, đường hiện có. Nếu thêm plot lvl5, làm bản thiết kế cho vùng mở rộng, chỉ triển khai khi phạm vi build được duyệt.

**Asset/callsite cần audit**: Island map, Sanctuary layout, camera bookmarks.

**Tiêu chí**: Before/after overview z0.36, path/collider/door regression.

**P26C liên quan**: Camera overview z0.36 trong render matrix P26C (frame J).

---

## REF08 — Bảng sự kiện nhiều thử thách / fantasy menu

**Tệp**: List_Of_Minigames.webp

**Đặc trưng**: Bảng danh sách hoạt động nhiều dòng, khung UI đặc sắc, mức độ thử thách phân biệt rõ.

**Nơi áp dụng**: Event Gate, quest board, Guild Trials, exploration/puzzle mode.

**KHÔNG sao chép**: Tạo game modes nguyên bản (relic puzzle, escort tactics, rune sequence, timed defense, scout expedition...) thay vì sao chép nguyên danh sách minigame, chữ, hình, độ khó.

**Asset/callsite cần audit**: Event board UI (P27).

**Tiêu chí**: Mỗi event card nêu mục tiêu, điều kiện mở, thưởng, hậu quả; event chưa có gameplay thật ghi LOCKED/COMING LATER rõ.

**P26C liên quan**: Không liên quan trực tiếp (P27). Ghi lại làm chuẩn cho sau.

---

## Hệ phối hợp chất lượng (cả 8 ảnh)

1. **Style guide chung**: "ETERNAL SUMMONER PREMIUM FANTASY 2.5D" — xanh aether và bạc lạnh cho cổng, đồng/gỗ tối cho UI, nền đá khối, ánh sáng hổ phäch cho tương phản; tránh neon xanh. Đồ họa gameplay giữ pixel density + FilterQuality.None; portrait/UI có thể dùng tranh/9-slice cao phân giải riêng nhưng phải sắc nét và thống nhất.
2. **Đối chiếu gốc→game**: Mỗi REF có bảng chức năng, vị trí trong game, asset thật có sẵn, thiếu gì, test thế nào. Phân định "design intent" và "implemented".
3. **Không giao UI đẹp trên hình mockup**: Phải có Compose screen và điều hướng thật, event thật hoặc đánh dấu chưa triển khai. Không dùng ảnh tham khảo trực tiếp làm background UI/game.
4. **Tham khảo cụ thể mới đạt**: Nếu nhận đủ tệp ảnh Arena, bắt buộc quan sát trực tiếp hình và so sánh từng nhóm.
5. **Đánh giá hai loại**: (a) lý do cảm xúc người chơi thấy hấp dẫn; (b) khả năng thao tác và chức năng lưu/chạy thật.
6. **Bảo toàn hiệu năng**: Chọn PNG/atlas kích thước hợp lý, cache portrait, không vẽ lại static asset mỗi frame, sky/cloud dynamic cực nhẹ, cutscene có SKIP.
7. **Không dùng từ "giống hệt" để sao chép IP**: Đây là định hướng nâng chất lượng nguyên bản. Không dùng logo, ảnh truyện, nhân vật hoặc UI screenshot nguồn trực tiếp trong game phát hành.

## Mối liên hệ với P26C

- REF01 (cổng) và REF06/07 (đá/cảnh): dùng để kiểm tra độ gắn kết bầu trời–cổng–đảo trong P26C render matrix (gate_z148, overview_z036).
- REF02–05, 08: là chuẩn chung cho P27 (UI/party/gate cutscene/loading/event board), **không nằm trong phạm vi P26C**.
- P26C đầu ra bắt buộc: clouds, STORM, ảnh engine, tests, PR riêng. Không tuyên bố hoàn thành UI/party/cutscene chỉ vì đã viết design brief này.
