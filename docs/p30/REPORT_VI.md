# P30 / #43 — Battle command responsiveness

Baseline main `d9c330fe768a789e57ad634dc597ae1256e663fd`; code/test/workflow SHA `b2e4a26` (full SHA trong evidence/validation.json). Nhánh `fix/issue43-master-command-latency`, một agent. Tài liệu/evidence bổ sung sau commit source không đổi code. Không merge hoặc force push; PR28 độc lập, không nhập thay đổi chưa merge của Mission01. #46 hiện chỉ audit/contract trong P31C_HOOK_AUDIT.md, chưa triển khai event queue hoặc migration.

## Xác minh và thay đổi

**CONFIRMED:** REGROUP và MOVE_TO trong pending contact của hero premium chờ hết attack; Priest có thể ưu tiên auto-heal trước urgent movement. **RETREAT premium trước contact đã hoạt động trên main**, không gọi đây là lỗi mới. Baseline AI 6 test: 3 fail, các test còn lại pass; baseline MOVE_TO riêng fail. Log/XML tại evidence/baseline-*.

`AutonomousHeroAi.kt`: policy hủy pending contact theo trạng thái/tọa độ đích, không theo tên hero. RETREAT luôn rời giao chiến; REGROUP/MOVE_TO chỉ ngắt khi còn phải đi tới slot. Khi đã tới nơi, attack/skill mới được hoàn tất bình thường (có regression test riêng). Không hoàn tác hit hoặc shield đã phát, không hoàn cooldown đã tiêu; không tạo damage muộn sau cancel. Urgent positioning đứng trước optional Priest heal. Focus/Protect/Hold giữ delivery một hit.

`TowerBattleScreen.kt`: engine đã nhận lệnh trong callback nhưng plain engine state chỉ được HUD lấy mẫu 250ms/lần. Input giờ cập nhật HUD và invalidate draw ngay trên sự kiện: nút, hotkey, skill và tap thế giới. Periodic sampler vẫn phục vụ simulation, không đổi target FPS. Test sceneActive=false tách phản hồi input khỏi sampler; Compose clock vẫn chạy để dispatch gesture/recompose. Không quy đổi thời gian test thành input latency thiết bị thật.

**NOT REPRODUCED:** tap xuyên từ control xuống world hoặc overlay vô hình lớn nuốt tap trong các tình huống đã test. Gesture recognizer, camera bounds/projection và hình thức HUD được giữ nguyên; không thêm lớp chắn fullscreen. Event trace xác nhận world tap tới Canvas. Một harness thử nghiệm khóa cả đồng hồ Compose đã chờ sai; bản cuối chỉ dừng simulation, dùng real touch/swipe/pinch và auto Compose clock. Gesture pan test chủ động vượt clamp đầu tầng để phân biệt thao tác bị chặn biên với input hỏng.

## Kiểm thử và ảnh

Lệnh local, chạy tuần tự bằng /workspace/cloud-setup/env.sh:

```sh
gradle :app:compileDebugUnitTestKotlin :app:assembleDebug :app:testDebugUnitTest --offline --console=plain \
  --tests 'com.example.MasterCommandResponsivenessTest' \
  --tests 'com.example.ui.BattleTouchOwnershipTest' \
  --tests 'com.example.PremiumSliceCombatTest' \
  --tests 'com.example.LostCourierEncounterTest' \
  --tests 'com.example.data.LostCourierPersistenceRobolectricTest' \
  --tests 'com.example.performance.ScenePerformanceTest' \
  --tests 'com.example.performance.SceneFrameLoopLifecycleRobolectricTest'
```

**36 PASS, 0 fail/error/skip**. Gồm 8 test lệnh/timeline, 3 touch scenarios và combat/rescue/save/lifecycle regressions. Engine test đo acceptance ngay lúc issue order và movement ở tick tiếp theo 1/60s; unit before/after-contact xác minh không duplicate damage, không mất shield đã cấp, cooldown không được hoàn. Test at-slot ngăn regression hủy lặp mọi đòn khi giữ REGROUP/MOVE_TO.

Native Compose/Robolectric test: 800×360 landscape và 360×640 portrait, Floor2 fixture, simulation đứng yên. Chạm nút thực, scroll bằng swipe thực; HOLD hủy pending placement; zoom/skill không đặt beacon; pan và pinch thay đổi pixel của vùng world và không trở thành tap; tap world đặt beacon thật; phím Six được xử lý. Không dùng performClick thay cho các thao tác touch đang được audit (chỉ dùng cho bỏ intro).

Ảnh đã xem trực tiếp: `evidence/{portrait,landscape}_world_beacon_hitboxes.png` có bounds thật màu tím, CSV kèm theo; bản không overlay và hold acknowledgement cùng thư mục. `landscape_skill.png` cho phản hồi manual skill. Đây là ảnh production Canvas/Compose qua native Skia, không phải điện thoại/emulator hoặc screenshot full nhiệm vụ PR28. Border hitbox do test vẽ thêm, không nằm trong game.

APK build PASS, apksigner v2 verify PASS; bytes/hash trong evidence/apk.json. Workflow riêng `.github/workflows/p30-battle.yml` chạy cùng bộ test/build và upload PNG/XML/APK; CI remote sẽ được ghi theo run trên đúng HEAD, không lấy local PASS làm GitHub CI green.

## Ownership và giới hạn

Source chỉ đổi `game/ai/AutonomousHeroAi.kt` và `ui/screens/TowerBattleScreen.kt`; thêm hai test, workflow và docs. Không sửa file Sanctuary HUD/renderer, shader/art, camera geometry, floor map, save schema hoặc rewards. Đối chiếu diff PR42 không có file source chồng nhau; cần kiểm tra lại trước hợp nhất. PR28 đã dirty với main trước khi bắt đầu, vẫn cần review tích hợp riêng khi owner quyết định.

Không có Android trong adb devices: physical touch latency, FPS/GPU/thermal và human playtest **NOT TESTED**. Không kết luận mọi hitbox trên mọi thiết bị đã hoàn hảo; ảnh/test chỉ bao phủ viewport và input paths nêu trên. Chưa đổi kích thước/style HUD hoặc thực hiện gameplay reforge của PR28 trong PR này.
