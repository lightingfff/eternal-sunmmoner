# MISSION01-B / Tower Reforge V2 — PARTIAL, NEEDS REWORK

Tiếp tục từ `mission01/floor1-rescue` 7bdd1c9. Bản này là checkpoint để review, **chưa đạt định nghĩa hoàn thành V2 và chưa sẵn sàng merge**. `origin/main` tại thời điểm kiểm tra là f0dc0de, PR #33 head c466d657; nhánh này chưa tích hợp và kiểm thử trên main mới. GraphQL của GitHub trả Forbidden nên chưa có kết quả CI exact SHA từ API.

Bổ sung hành trình Floor 1 qua các chặng khảo sát, trinh sát, cứu toán gác, trạm tín hiệu, cầu, thuốc, ấn và tiếp sức trước chặng hộ tống cũ. Có 14 thao tác hiện trường dựa trên vị trí hero và tương tác; trạm tín hiệu chọn giảm quân truy đuổi hoặc cứu trinh sát và hồi sức; kết quả được lưu. Bảo toàn ba kết quả cứu hộ và save/retry. Không thay tốc độ courier hoặc tăng HP địch để kéo dài. Generic enemy tấn công hero/courier thay vì dấu Master; rút lui từ UI phát lệnh đi tới điểm thoát. Fear hiện có thêm cảnh báo và phản hồi theo tính cách; đây chưa phải hệ stress/trust hoặc cá tính hoàn chỉnh.

## Bằng chứng worker

`python3 tools/validate_assets.py` PASS (cảnh báo preview >3MB đã được loại khỏi APK). `:app:assembleDebug` PASS; APK ký debug xác minh bằng apksigner. Bộ Gradle chọn lọc gồm 62 test PASS, 0 fail, 0 skipped, bao gồm ba lượt đi từ đội khách mới, interaction/field, lựa chọn/save/restart, rút lui từ xa, đường đi/va chạm, enemy targeting, fear alert và UI, lifecycle. Không teleport nhân vật hay ép quái chết trong ba lượt chính. Native Robolectric screenshots và CSV: `docs/visual-evidence/mission01b/`; đây không phải ảnh thiết bị thật.

| Nhánh | Giây mô phỏng | Di chuyển courier (giây) | Combat hành trình (giây) | Thao tác thực địa | Lệnh focus |
|---|---:|---:|---:|---:|---:|
| Tắt báo động / hỗ trợ | 318.30 | 221.17 | 12.85 | 14 | 7 |
| Cứu trinh sát / tiếp vận | 317.72 | 221.17 | 12.25 | 14 | 7 |
| Tắt báo động / bỏ lỡ tiếp vận | 317.15 | 221.17 | 11.67 | 14 | 6 |

Khoảng cách dài nhất giữa sự kiện thật theo bộ kiểm thử khoảng 36 giây; đây không phải bằng chứng 5–10 phút nội dung thực chất. Phần lớn thời gian vẫn là đi đường, field actions phần nhiều là di chuyển rồi xác nhận, và không có so sánh no-orders/smart/wrong-choice hoặc thời gian người chơi thật. Không coi puzzle, giải cứu NPC actor hay nội dung thành phố là đã hoàn thiện chỉ vì có marker/lời thoại. Gameplay cần thiết kế lại và thử với người chơi, tiếp tục giảm đường trống và tăng quyết định chiến thuật có hậu quả.

## Giới hạn quan trọng

- Agent reviewer xác nhận còn Master bolt tự bắn từ dấu điều khiển và dấu có thể kích hoạt điểm khám phá trên các tầng không có courier; không tuyên bố Master-off-field đã hoàn tất toàn Tower.
- Chưa có stress/trust lưu bền, thử nghiệm hai tính cách trong trận thật, hoặc toàn bộ skill/ultimate yêu cầu V2. UI chỉ có HUD nhiệm vụ và cảnh báo bước đầu; chưa đạt toàn bộ REF09. Bốn ảnh người dùng cung cấp được xem trực tiếp; không sao chép art/tên/thoại. Các ảnh tham chiếu khác không có trong phiên.
- Chưa có city rescue, NPC crowd/path, multi-map, human interactive playthrough, thiết bị/emulator, FPS/GPU/nhiệt hoặc full-screen screenshot cho mọi trạng thái field/alert ở cả hai hướng. Không tuyên bố đo thiết bị.
- Không merge. Kết quả CI cần xác nhận trên đúng SHA sau khi cập nhật PR; cập nhật branch không đồng nghĩa CI pass.
