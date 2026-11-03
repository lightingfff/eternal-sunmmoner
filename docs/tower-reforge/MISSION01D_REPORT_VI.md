# Tower Reforge V3 — C0 integration, chưa hoàn tất V3

PR: https://github.com/lightingfff/eternal-sunmmoner/pull/28. Nhánh `mission01/floor1-rescue`. Một agent thực hiện tuần tự. Owner đã xác nhận “cả hai”: triển khai V3 rồi merge sau kiểm chứng; chưa merge. Không sử dụng subagent.

## Tích hợp và lỗi thực tế

Remote refs kiểm tra được trong phiên: PR28 `ba7677a36d1be225544b4062bd1b39c71acb8f84`, main `f0dc0de6b63794082ba5db88244a2cc8c486bcdb`. Code candidate sửa CI/HUD: `fe4356a` (commit sau này chỉ thêm bằng chứng/báo cáo không đổi source). Commit tích hợp `15e6cad` có đúng hai parent đó. Chỉ có add/add conflict trong `ci-diagnostics/gradle-error.log` và `render-tests.log`. Hai bản gốc được giữ byte-identical trong `ci-diagnostics/archive/{pr28-ba7677a,main-f0dc0de}`; file ở cấp gốc giữ log PR28. Không chọn ours/theirs hàng loạt. Mã combat/data của PR và renderer/assets của main được đối chiếu không mất thay đổi.

- `.github/workflows/mission01-rescue.yml:18`: thay `android-actions/setup-android@v3` bằng command-line SDK có sẵn trên runner, cùng quy trình license/package với workflow APK. Cài API 36.1, build-tools 36.0.0, platform-tools; không yêu cầu gói legacy `tools`. Java 21/Gradle 9.3.1 được giữ. Thêm debug Google resource giống workflow APK và đưa field-action/targeting regression vào acceptance. YAML và các đoạn Bash đã kiểm tra cú pháp.
- `tools/p25_validate_faces.py` và dependencies được tích hợp từ main, không skip validator. Chạy thật: **85 PASS / 0 FAIL**, gồm alpha/geometry/door/manifest/ROI và deterministic regeneration. Asset validator PASS; preview >3 MB vẫn được loại khỏi APK như trước.
- Nguồn của hai lỗi CI ban đầu là snapshot trong tài liệu V3: acceptance run `41694514871` lỗi SDK package; render run `41694514954` thiếu validator. Không tải lại được log live vì GitHub API forbidden; không gọi những snapshot này là CI mới nhất.
- Khi chạy thêm đúng bộ regression render, phát hiện lỗi thực tế ở `LayoutResponsivenessRobolectricTest.kt:610`: HUD ngang cao 315/720 px (43,75%), giới hạn là dưới 30%. Log lỗi được giữ trong `docs/visual-evidence/mission01d/c0/render-baseline-09.log`. Sửa `RescueJourneyHud` để landscape có một hàng mục tiêu/HP/lệnh, mở lời giải thích qua “Chi tiết”; giữ nút lựa chọn và thao tác hiện trường. Giảm padding dọc khi màn chiến đấu ở chế độ ngang, không thay assertion. Sau đó test dọc phát hiện 449/1280 px (35,08%) vượt 32%; chuyển phần giải thích dài vào “Chi tiết” ở cả hai hướng và kiểm thử mở/đóng nội dung.

## Bằng chứng local

Toolchain cloud: JDK 21, Gradle 9.3.1, Android SDK 36.1/build-tools 36.0.0. Mỗi lần chỉ một Gradle process. `--offline` dùng cache đã cài, không giả kết quả CI.

- Baseline sau merge: `:app:assembleDebug` và 62 test acceptance PASS, 0 fail/error/skip; chữ ký APK v2 xác minh bằng apksigner. `rescue-build.log` chứa task/filter và kết quả.
- P23–P26 baseline: 20 test PASS, 0 fail/error/skip; `p23-p26-render.log` và `p23-p26-counts.json`.
- Các nhóm còn lại chạy theo lệnh trong workflow render, kết quả từng nhóm lưu `remaining-render-checks.json`. Lần đầu dừng đúng tại lỗi HUD; không tính test bị skip do fail-fast là PASS.
- Sau sửa HUD: build APK và 20 test layout/UI/render PASS, 0 fail/error/skip (`hud-round2.log`). Hai assertion giới hạn 30% landscape và 32% portrait giữ nguyên và đều qua; thêm kiểm thử “Chi tiết” mở/đóng đúng nội dung. Lượt cuối trên code SHA `fe4356abc0de0794163c42d817d4c79f765c9d7a`: **85 test PASS**, 0 fail/error/skip, gồm toàn bộ acceptance, layout, Sanctuary art/material/world và terrain render còn lại. Chi tiết `final-checks.json`, lệnh/filter ở `final-regression.log`. APK cuối ký v2 hợp lệ, hash/size ở `apk.json`.

Ảnh native thật trong `docs/visual-evidence/mission01d/c0/`: `battle_portrait.png`, `battle_landscape.png`, `captain_telegraph.png`, `architecture_z100.png`, `gate_night_z148.png`. Đây là baseline trước sửa HUD, đã xem trực tiếp; dùng Canvas/Compose production qua Robolectric. Ảnh captain dùng party fixture của visual test, không thay thế lượt simulation guest. Ảnh component HUD đơn lẻ không được coi là full gameplay. Ảnh sau sửa đã xem: `battle_landscape_after.png` và `battle_portrait_after.png`; mục tiêu và lệnh vẫn nhìn được, giảm phần phủ bản đồ. Giữ cảnh quan Sanctuary/P23–P26; không sửa PR33/P27.

## Gameplay và các gate còn thiếu

Chưa thay gameplay V3 trong C0. CSV MISSION01-B cũ: hỗ trợ 318,30 s / đi đường hành trình 221,17 s / combat hành trình 12,85 s; thu hồi tiếp vận 317,72 / 221,17 / 12,25; bỏ lỡ tiếp vận 317,15 / 221,17 / 11,67. Chỉ riêng travel hành trình đã bằng 69,5–69,7% tổng sim time. Đây không phải phép đo tổng combat toàn nhiệm vụ hay thời gian chơi của người thật. Ba nhánh cũ chưa phải A/B/C smart/passive/wrong-order. Số marker/confirm không chứng minh meaningful decisions; idle max và phân loại engagement cần telemetry mới.

C1–C5 chưa đạt: route compression/encounter mới, stress/trust có tác dụng lưu bền, ba skill starter/ultimate, bốn enemy roles, năm zone chơi có hệ quả, A/B/C cùng seed và party, full UI/render matrix. Master auto-bolt/discovery ngoài courier vẫn là lỗi đã biết. City **chưa playable**, chỉ là scope sau khi Floor1 đạt. Không dùng baseline xanh để tuyên bố V3 hoàn thành hoặc gameplay 5–10 phút đạt chất lượng.

`adb devices -l` không có thiết bị. Human play duration, FPS/GPU/nhiệt/điện thoại thật: **NOT TESTED**.

REST và GraphQL GitHub đều bị proxy từ chối `Forbidden`; network policy chưa có `api.github.com`. Git fetch/push là khả năng khác với GitHub API. C0 remote CI trên code SHA cuối **UNVERIFIED**; cần quyền mạng cho API để kiểm tra Actions/PR và thực hiện merge sau khi các gate V3 đạt. Không bypass proxy, không push trực tiếp main hoặc force-push. Theo thứ tự C0 trước C1 của V3, chưa đánh dấu baseline remote đạt để mở rộng gameplay.
