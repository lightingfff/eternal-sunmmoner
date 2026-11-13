# Báo cáo A02 — Android Build & CI (ES-A02-001)

**Trạng thái:** PARTIAL / chờ review độc lập; đây là audit/spec, không phải sửa lỗi CI hay chấp thuận merge. Chủ dự án là người duy nhất quyết định merge. Chi tiết test âm tính và ownership nằm trong [ROUND_1.md](ROUND_1.md) và [WORK_ORDER.md](WORK_ORDER.md).

**Nguồn/định danh:** [Issue #53](https://github.com/lightingfff/eternal-sunmmoner/issues/53), [Task Center #39](https://github.com/lightingfff/eternal-sunmmoner/issues/39). Baseline `main` và local HEAD trước tài liệu: `bdff6f9976dc4927e1e095a503bc339509c7a9aa`; branch riêng `arena/617438e9-eternal-sunmmoner`. Snapshot đối chiếu 2026-11-08 06:09 UTC; trạng thái có thể đổi sau đó. PR/SHA cuối cùng và kết quả CI của **PR tài liệu** được ghi trong `[HANDOFF]` tại Issue #53, không tự tham chiếu SHA trong chính commit.

## Tóm tắt cho owner

1. `main` baseline có APK CI **SUCCESS**, gồm các bước verify chữ ký/ZIP và verify artifact trên runner. Điều này **không** chứng minh toàn bộ unit tests hoặc chạy offline trên điện thoại.
2. PR #50 HEAD tại snapshot là `8cc082fbbee5938dbfcfe159d2d2bba0d1645b1c`; ba run mới `ACTION_REQUIRED`, chưa có kết quả thực thi trên HEAD đó. Trên HEAD **trước** `e43dea26d9de69a8b0ada860a2ba551ba0ff79e1`, P27B thật sự FAIL bài Back. Không suy diễn nguyên nhân runtime/harness, không gán kết quả cũ cho HEAD mới.
3. PR #47 HEAD `c296e0958e35b1e8d0d4b339857e188f81776ea8` có các run nhắm HEAD đã báo SUCCESS; một số workflow pull-request dùng checkout merge ref mặc định, và mọi run này trước các thay đổi mới trên `main`. Chúng không chứng minh merge vào baseline hiện tại đã được test.
4. Hai rủi ro cần ticket/owner riêng: P27B dispatch có bước commit/push lên ref được chọn; và validator được gắn nhãn “PNG CRC” chấp nhận fixture IDAT có CRC sai. A02 không sửa workflow, tài nguyên hay test của agent khác.

## Ma trận build / test theo SHA và sự kiện

| Đối tượng / SHA | Build / test thật từ GitHub API | Giới hạn |
| --- | --- | --- |
| `main` `bdff6f9976dc4927e1e095a503bc339509c7a9aa` | [APK run 42452402051](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42452402051) `push`, SUCCESS. Job/step metadata ghi SUCCESS cho `Build APK`, `Verify signed APK and package Android-compatible ZIP`, upload, `Verify the uploaded artifact ZIP`, và publish release. | Đây là checkout `main` trên run đó, không phải full unit suite hay thử cài/chơi. Download APK/log về sandbox không thành công. |
| `main` `bdff6f9…` | [Lyra manual 42453053914](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42453053914/job/128646165891) được A02 kích hoạt sau khi kiểm tra không có run main đang hoạt động. `workflow_dispatch` pin `github.sha`; job hoàn tất SUCCESS gồm compile unit tests, P26C contract/render, các selector khác và assemble debug. | P26C dùng `@Config(sdk = [36])`, chứng minh **các selector đó** chạy được trên SDK 36/Robolectric ở SHA này; chưa phải toàn bộ test. Publish-to-branch SKIPPED trên manual dispatch. |
| [PR #50](https://github.com/lightingfff/eternal-sunmmoner/pull/50), HEAD `8cc082fbbee5938dbfcfe159d2d2bba0d1645b1c` | [APK](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42453097349), [P27B](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42453097332), [Lyra](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42453097314): `ACTION_REQUIRED` tại snapshot. | Không có kết luận thực thi cho HEAD mới; cần owner/A01 xem GitHub, ổn định HEAD và tổ chức re-run an toàn. Không đoán lý do action required. |
| PR #50, HEAD **cũ** `e43dea26d9de69a8b0ada860a2ba551ba0ff79e1` | [APK](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42450674341) SUCCESS; [Lyra](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42450674322) SUCCESS; [P27B attempt 2](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42450674309/job/128645203944) FAILURE. | Annotation P27B: 33 tests completed, 1 failed, 1 skipped; `masterModalDismissedByAndroidBack` không thấy modal đóng. Không gán kết quả này cho `8cc082f`. P27B/Lyra pin HEAD; APK workflow checkout PR merge ref mặc định, exact checkout commit trong log không truy cập được. |
| [PR #47](https://github.com/lightingfff/eternal-sunmmoner/pull/47), HEAD `c296e0958e35b1e8d0d4b339857e188f81776ea8` | [P27B 42255354811](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42255354811) SUCCESS trên HEAD pin; [Lyra manual 42255477644](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42255477644) SUCCESS trên HEAD pin, gồm test P26C SDK36 và assemble; [P30 42255355089](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42255355089), [APK 42255355109](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42255355109) SUCCESS cho sự kiện PR có `head_sha=c296e09…`. | P30 và APK dùng checkout PR merge ref mặc định; log checkout inaccessible, không khẳng định commit checkout chính xác. P30 selector gồm `LostCourierPersistenceRobolectricTest`, nhưng không chứng minh offline launch trên thiết bị hoặc tích hợp với `main` mới. |

**Lưu ý về SHA:** API `run.head_sha` liên kết sự kiện với PR head, không tự chứng minh workflow checkout HEAD. Các workflow P27B/Lyra được kiểm tra có `ref: github.event.pull_request.head.sha || github.sha`; `main.yml` và P30 không đặt `ref`, nên PR event checkout merge ref mặc định. API mô tả PR base có thể là snapshot cũ; kiểm tra parent của merge ref và log mới trước khi coi đó là integration test với main hiện tại. Không suy ra SHA merge đã chạy từ việc PR hiện có một merge ref mới.

## SDK, dependency và offline

- Trên các commit `bdff6f9…`, `e43dea2…`, `c296e09…`, catalog cùng Git blob `ab356f2bb8392c4523cacc68c9510c780af0c3e1`: AGP `9.1.1`, Kotlin `2.2.10`, Robolectric `4.16.1`; `app/build.gradle.kts` compile SDK `36.1`, target SDK `36`. Các workflow kiểm tra dùng Java `21`, Gradle `9.3.1` và cài SDK `36.1`/build-tools `36.0.0`.
- Lịch sử [#48](https://github.com/lightingfff/eternal-sunmmoner/issues/48) / [PR #49](https://github.com/lightingfff/eternal-sunmmoner/pull/49): catalog commit `5d3534e…` là Robolectric `4.14.1`; commit merge `e12d960…` khôi phục `4.16.1`. Việc khôi phục phiên bản không thay thế test SDK36 trên SHA hiện tại; P26C selector `main` nêu ở bảng đã có kết quả step SUCCESS.
- `googleServices { missingGoogleServicesStrategy = WARN }` và resource debug tạm trong CI là cấu hình build có sẵn; **không** tạo `google-services.json` giả. Code có nhánh guest-local, và PR #47 có test persistence chọn lọc, nhưng **guest offline startup/cài đặt trên Android: NOT TESTED** trong phiên này.
- Không có `gradlew`, `gradlew.bat` hay `gradle-wrapper.jar` được Git theo dõi, chỉ có wrapper properties; CI cài Gradle riêng. Sandbox không có Java, Gradle, Android SDK hoặc `adb`; các lệnh local Android exit 127. `LOCAL BUILD NOT RUN`.

## Integrity APK / artifact: phân biệt nguồn bằng chứng

| Run | Metadata GitHub về artifact ZIP | Kết luận có thể nói |
| --- | --- | --- |
| [main 42452402051](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42452402051) | Artifact `13606023831`, 78.435.667 bytes, digest API `sha256:dac054878697c17e75997d1b24ef1905f7be325fd5cca6dd35a0b980bb6a8139` | CI báo SUCCESS cho `apksigner`, kiểm tra APK/portable ZIP và tải lại + verify artifact. Đây là kết quả trên runner, không phải A02 tự tải byte. |
| [PR #50 e43 run 42450674341](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42450674341) | Artifact `13606845139`, 78.435.667 bytes, digest API `sha256:153798514387f7168323c3438939610ae522a5ffe2346399db9496e61a470bb6` | Các bước verify/upload/verify artifact SUCCESS ở SHA sự kiện cũ, không phải HEAD `8cc082f`. |
| [PR #47 run 42255355109](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42255355109) | Artifact `13522252122`, 78.190.037 bytes, digest API `sha256:2b23f599160e83b207c47cfa39e6e6b2d1b22a2100306d41d558f4767ec04357` | Verify/upload/verify artifact SUCCESS theo step metadata của sự kiện PR; checkout merge ref mặc định. |

[Release APK main](https://github.com/lightingfff/eternal-sunmmoner/releases/tag/apk-320-bdff6f9976dc4927e1e095a503bc339509c7a9aa) được API liệt kê với APK 39.217.353 bytes, digest `sha256:eb6974eae69384069b5053e574ee9df08875cd2a55619e6ee901aca54ada17a0`. Đây là **metadata từ GitHub**, không phải hash do A02 tính từ file tải về. Artifact API digest là của archive bên ngoài, không phải hash APK bên trong. Artifact có thời hạn, xem live link. Các phép thử helper bằng ZIP giả ở ROUND_1 không xác minh APK thực hoặc chữ ký.

`gh run view <id> --log` và `gh release download ... --pattern SHA256SUMS` thất bại EOF do redirect đến host ngoài allowlist; API job/step/annotation và release metadata vẫn đọc được. Chưa đọc log đầy đủ, chưa tải APK, chưa tự cài hoặc xác minh APK đã tải.

## Ưu tiên khắc phục / tránh va chạm

- **P1 — PR #50:** A01/owner và A09/Codex xác định nguyên nhân bài Back trên HEAD mới, chạy lại CI trên một HEAD ổn định. Cả ba run `ACTION_REQUIRED` trên `8cc082f` không phải PASS; [đã thông báo PR](https://github.com/lightingfff/eternal-sunmmoner/pull/50#issuecomment-6718030116). Nếu HEAD lại đổi, lặp lại đối chiếu; không lấy evidence cũ làm kết luận.
- **P1 — P27B manual safety:** step `always()` có thể **thử** commit/push ref được chọn nếu có ảnh diff, kể cả sau một số lỗi. Ruleset `main` hiện active, yêu cầu pull request và API liệt kê không có bypass actors: đây là lớp chặn hiện tại, không có bằng chứng A02 đã push main. Tránh dispatch P27B lên main/branch không thuộc mình; A01/owner giao issue sửa workflow riêng, giữ một writer và kiểm thử negative cases. Không đổi ruleset để tiện chạy.
- **P2 — PNG validator:** fixture bad-CRC không thuộc hai manifest SHA được validator kiểm tra; validator trả PASS. Đề nghị owner giao file + negative tests riêng; không khẳng định tài sản thật corrupt. Repro có trong ROUND_1.
- **P2 — coverage:** APK job không chạy full unit suite; workflow còn path filters, nên kiểm tra selector và exact SHA thay vì coi tất cả test đã chạy. Trên PR #47 cần test lại integration với main hiện tại theo điều phối của Codex/A01.

## QA, reviewer và giới hạn

- Đã chạy lại nguyên văn hai lệnh fixture trong ROUND_1: test CRC âm tính tái hiện validator PASS sai; helper package synthetic positive/negative đạt các kỳ vọng; `python3 tools/validate_assets.py` trên baseline exit 0 với WARN. Đây là local checks trên script, **không** là Android build.
- Không thay camera oblique `Y=.72`, map, saves, gameplay, assets, Gradle, Firebase hoặc workflow. Không có screenshots mới, gameplay playthrough hay thiết bị. Visual, full offline startup, physical-device FPS/thermal: **NOT TESTED**.
- Cần `[ARGON_REVIEW]`, `[CODEX_REVIEW]`, `[QA_REVIEW]` của A09 trên exact PR HEAD; chưa có review độc lập. Danh sách collaborator API chỉ cho thấy `@lightingfff`, không biết account GitHub riêng cho ba reviewer, nên không tự gán username hay nói họ đã chạy. Owner phải khởi chạy/cấp quyền các phiên thực; comment không tự khởi động agent. A01/Argon chỉ định một writer/file nếu có `[FIX_PLAN]`; mọi SHA mới cần review lại.
