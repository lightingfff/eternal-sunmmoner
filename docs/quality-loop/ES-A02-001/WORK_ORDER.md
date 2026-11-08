# WORK_ORDER — ES-A02-001: kiểm toán Android Build & CI

- Ngày: 2026-11-08 (UTC). Ticket: [#53](https://github.com/lightingfff/eternal-sunmmoner/issues/53); điều phối: [#39](https://github.com/lightingfff/eternal-sunmmoner/issues/39).
- Baseline `main` khi claim: `bdff6f9976dc4927e1e095a503bc339509c7a9aa`; nhánh phiên riêng: `arena/617438e9-eternal-sunmmoner`. Xác nhận lại các SHA và trạng thái live khi lập báo cáo; không suy diễn PR head từ run trên merge commit.
- Writer: một phiên thực Arena.ai Agent Mode, A02. A01 là technical/integration co-lead; Argon là creative/production co-lead. A09, Codex, Argon là reviewer độc lập được yêu cầu, không giả lập trong phiên này.
- Mục tiêu: cho owner ma trận có thể kiểm tra lại về build/APK/test SDK36 trên `main`, [PR #50](https://github.com/lightingfff/eternal-sunmmoner/pull/50) và [PR #47](https://github.com/lightingfff/eternal-sunmmoner/pull/47), để tránh chấp nhận nhầm APK xanh là toàn bộ test/gameplay xanh.
- Trước thay đổi: audit/spec-only, không thay runtime. `app/build.gradle.kts` đặt compile SDK 36.1, target SDK 36; `.github/workflows/main.yml` cài JDK 21, SDK 36.1, Gradle 9.3.1 rồi assemble và xác minh APK. Các giá trị, triggers và kết quả từng nhánh phải được đối chiếu với đúng ref tương ứng.

## Quyền ghi và tuần tự

| File mới duy nhất được phép ghi | Writer |
| --- | --- |
| `docs/quality-loop/ES-A02-001/WORK_ORDER.md` | A02 |
| `docs/quality-loop/ES-A02-001/ROUND_1.md` | A02 |
| `docs/quality-loop/ES-A02-001/FINAL_REPORT_VI.md` | A02 |

Đã claim các path chính xác trên [Issue #53](https://github.com/lightingfff/eternal-sunmmoner/issues/53#issuecomment-6717970264), kiểm tra không giao với danh sách file trong PR #50/#47/#28/#13 tại thời điểm claim. Đây không phải khóa nguyên tử: nếu có claim đối kháng, dừng và nhờ điều phối. Không sửa workflow, Gradle/dependency, Firebase, tài liệu agent khác, runtime, art, camera/anchors/occlusion, map/collision, saves hoặc combat. Những thay đổi đó cần ticket/quyền riêng. Không chạy đồng thời Gradle trên cùng checkout; không sửa PR của người khác.

## Tiêu chí nghiệm thu (đặt trước kiểm toán)

1. Ghi SHA `main`, HEAD và base của từng PR, thời gian kiểm tra và URL; xác định run thực sự checkout HEAD, PR merge ref hay ref khác. Kiểm tra SHA lại trước bàn giao.
2. Liệt kê kết luận/tình trạng thật của từng workflow/check có liên quan, URL, event, SHA, test đã chạy và test không chạy. Không biến check bị thiếu/cancelled/thất bại thành PASS, hoặc dùng run cũ làm bằng chứng cho HEAD mới.
3. Đối chiếu cấu hình SDK/Gradle/Robolectric và các test SDK36 với bằng chứng trên từng SHA. Chỉ kết luận runtime tương thích khi có test phù hợp thành công; ghi riêng lịch sử regression và các giả thuyết.
4. Đối chiếu `assembleDebug`, kiểm tra chữ ký/ZIP/hash và tình trạng artifact ở log thực tế; chỉ nói đã tự kiểm tra APK tải về nếu thật sự tải và kiểm tra byte. Phân biệt verify trong CI với artifact upload/download và chất lượng cài/chạy.
5. Ghi lệnh local thật, kết quả và thiết bị/mạng bị thiếu. Nếu JDK/Gradle/SDK hoặc host phụ thuộc không có, `LOCAL BUILD NOT RUN` và không ngụy tạo kết quả; không tạo `google-services.json` giả hoặc đổi dependency để né lỗi.
6. Rà soát `git diff --check`, giới hạn chính xác ba path, không có thay đổi game. PR chưa merge; `[PROGRESS]` và `[HANDOFF]` dẫn exact SHA, URL/giới hạn và yêu cầu Argon, Codex, A09 review.

## Phương pháp và bằng chứng

- Baseline: `git status`, `git rev-parse HEAD`, API GitHub issue/PR/checks/runs/artifacts; đọc workflow và cấu hình theo exact commit liên quan. Trích URL CI, SHA, lệnh và outcome vào ledger; thời điểm snapshot có thể cũ sau đó.
- Nỗ lực kiểm tra artifact/asset hoặc unit test chỉ khi môi trường thực sự hỗ trợ; tách `PASS`, `FAIL`, `WARN`, `NOT TESTED` và lý do. So sánh guest-offline chỉ theo test/thiết bị thực tế.
- Với tài liệu audit, sáu chiều hình ảnh/gameplay của quality loop không có cơ sở để chấm; ghi `NOT VERIFIED` thay vì đặt điểm. Mọi phát hiện CI được gắn mức độ, evidence và bước kiểm tra tiếp theo; sửa workflow chỉ sau quyền riêng.
- Bảo vệ 2.5D `Y=.72`, anchors, pixel filtering, graphics, persistence và hiệu năng: không thay các file đó; test trên thiết bị thật/FPS/nhiệt là `NOT TESTED` nếu không có thiết bị.
- Skills: `es-a02-android-build`, `eternal-arena-quality-loop`, `eternal-arena-integration-reviewer`, router và `eternal-arena-studio-director` cho quy trình/giới hạn. Không dùng subagent giả; review độc lập ở ngoài phiên này.
- Chu trình: baseline → audit bằng chứng → tự kiểm tra giới hạn và tính nhất quán → chỉnh tài liệu nếu cần → test lại (`ROUND_1`; tối đa ba vòng). Đọc lại trạng thái trước bàn giao. Rollback an toàn: để owner bỏ PR hoặc revert đúng ba file tài liệu nếu cần; không reset/force push. User là người duy nhất quyết định merge.
