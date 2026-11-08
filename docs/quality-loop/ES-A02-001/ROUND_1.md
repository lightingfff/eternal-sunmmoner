# ROUND_1 — ES-A02-001, kiểm toán CI (A02)

Baseline: `bdff6f9976dc4927e1e095a503bc339509c7a9aa`; branch: `arena/617438e9-eternal-sunmmoner`. Đây là kiểm toán của một phiên, **không** phải review độc lập; không sửa Kotlin, workflow hay asset. Bằng chứng và trạng thái live được tổng hợp trong `FINAL_REPORT_VI.md`.

## Kiểm tra chủ động: phát hiện, bằng chứng, hành động

| Probe / mức độ | Kết quả quan sát | Phân loại và bước tiếp theo |
| --- | --- | --- |
| P1 — PR #50, gate Back | [P27B run 42450674309 attempt 2](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42450674309/job/128645203944) trên **event head** `e43dea26d9de69a8b0ada860a2ba551ba0ff79e1` có annotation `P29HubHudTest > masterModalDismissedByAndroidBack FAILED`; 33 tests completed, 1 failed, 1 skipped; `master_hub_screen` vẫn được tìm thấy. P27B checkout pin PR head theo workflow. | FAIL trên SHA cũ; không kết luận root cause là lỗi runtime hay harness. A01/Codex/A09 cần kiểm tra và sửa/kiểm tra lại theo ownership; A02 không sửa PR khác. |
| P1 — trôi HEAD/không có test mới | Sau [Lyra run 42450674322](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42450674322), [commit evidence 8cc082f](https://github.com/lightingfff/eternal-sunmmoner/commit/8cc082fbbee5938dbfcfe159d2d2bba0d1645b1c) thay 10 PNG và là HEAD mới. Các run [APK](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42453097349), [P27B](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42453097332), [Lyra](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42453097314) báo `action_required`. | NOT TESTED trên HEAD mới, không chuyển PASS/FAIL từ SHA cũ. Chưa xác minh lý do `action_required`. Owner/A01 xử lý approval/CI an toàn và chốt HEAD trước review; tránh auto-publish lặp. |
| P1 — safety của workflow | [P27B workflow:172–175](https://github.com/lightingfff/eternal-sunmmoner/blob/bdff6f9976dc4927e1e095a503bc339509c7a9aa/.github/workflows/p27b-render.yml#L172-L175) cho phép `workflow_dispatch` vào bước `always()`, và [234–237](https://github.com/lightingfff/eternal-sunmmoner/blob/bdff6f9976dc4927e1e095a503bc339509c7a9aa/.github/workflows/p27b-render.yml#L234-L237) commit/push đến `github.ref_name` nếu có diff. | Rủi ro có điều kiện, **không phải sự cố push đã quan sát**. Ruleset main hiện active, yêu cầu PR, không có bypass actors theo API; nó là lớp bảo vệ trước push trực tiếp, không chứng minh các branch khác an toàn. Không dispatch P27B lên main/branch người khác; đề nghị A01/owner cấp ticket riêng cho fix, để manual chỉ kiểm tra hoặc xuất artifact. |
| P2 — gate CRC của PNG | [Tên bước CI](https://github.com/lightingfff/eternal-sunmmoner/blob/bdff6f9976dc4927e1e095a503bc339509c7a9aa/.github/workflows/main.yml#L83-L84) nói PNG CRC; [check_png](https://github.com/lightingfff/eternal-sunmmoner/blob/bdff6f9976dc4927e1e095a503bc339509c7a9aa/tools/validate_assets.py#L17-L34) chỉ kiểm tra signature, chuỗi IEND cuối và kích thước. Thử fixture bên dưới: chunk IDAT CRC sai nhưng validator exit 0, `PASS`. | FAIL của negative test; **không chứng minh** file PNG thật bị hỏng. Hai manifest có SHA riêng; fixture không thuộc hai manifest ấy. Cần file owner được giao bổ sung parse/CRC + negative tests và phân biệt hash manifest. |
| SDK36 — selector trên main | [Lyra manual run 42453053914](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42453053914/job/128646165891) trên `bdff6f9976dc4927e1e095a503bc339509c7a9aa` hoàn tất SUCCESS: compile test, P26C contract/render, các selector khác và assemble thành công; bước publish commit SKIPPED. P26C dùng `@Config(sdk = [36])`. | PASS cho các selector thực tế của workflow, không phải full unit suite hay device. |
| P2 — APK xanh không bao phủ suite | `main.yml` gọi `:app:assembleDebug` và xác minh đóng gói, không gọi full `:app:testDebugUnitTest`; Lyra và P27B chạy test có selector. | Không gán `full suite PASS` hoặc `offline startup PASS` từ APK CI. PR #47 P30 có selector persistence, không thay thế chạy ứng dụng offline trên thiết bị. |
| P2 — bộ chứng cứ PR #47 cũ hơn main | PR #47 event HEAD `c296e0958e35b1e8d0d4b339857e188f81776ea8`; [P30](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42255355089) và [APK](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42255355109) là pull_request với checkout mặc định (merge ref); [P27B](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42255354811) pin head; [Lyra dispatch](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42255477644) pin `github.sha`. | Các run đều SUCCESS, nhưng không phải kiểm chứng tích hợp với main `bdff6f9` sau này. Log checkout không tải được trong sandbox; không bịa merge SHA thực tế của run cũ. |
| WARN — local/device/artifact | `java -version`, `gradle :app:assembleDebug`, `gradle :app:testDebugUnitTest --tests 'com.example.P26CCloudStormContractTest'`, `adb devices` đều exit 127 (command not found). `gh run view --log` và `gh release download ... --pattern SHA256SUMS` gặp EOF khi redirect sang host ngoài allowlist. | `LOCAL BUILD NOT RUN`; không tự đọc/xác minh bytes của APK/log tải về, không đo offline launch, FPS/thermal hay visual. GitHub check/job/step metadata và annotation được truy cập qua API: phân biệt bằng chứng đó với log đã tải. |

## Tái hiện kiểm tra CRC âm tính (Python standard library, scratch)

Chạy ở root checkout baseline; chỉ tạo một cây tạm, không thay asset của repo. Script xác minh độc lập checksum chunk rồi chạy **bản sao nguyên vẹn** của validator thật:

```sh
python3 - <<'PY'
from pathlib import Path
import shutil, struct, subprocess, tempfile, zlib
with tempfile.TemporaryDirectory(prefix='es-a02-png-negative-') as t:
    root = Path(t)
    (root/'tools').mkdir()
    (root/'app/src/main/assets').mkdir(parents=True)
    shutil.copyfile('tools/validate_assets.py', root/'tools/validate_assets.py')
    def chunk(kind, data, bad=False):
        crc = zlib.crc32(kind + data) ^ int(bad)
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', crc)
    png = (b'\x89PNG\r\n\x1a\n'
           + chunk(b'IHDR', struct.pack('>IIBBBBB', 1, 1, 8, 6, 0, 0, 0))
           + chunk(b'IDAT', zlib.compress(b'\x00\xff\x00\x00\xff'), True)
           + chunk(b'IEND', b''))
    (root/'app/src/main/assets/bad-crc.png').write_bytes(png)
    pos, bad = 8, []
    while pos < len(png):
        length = struct.unpack_from('>I', png, pos)[0]
        kind, data = png[pos+4:pos+8], png[pos+8:pos+8+length]
        stored = struct.unpack_from('>I', png, pos+8+length)[0]
        if stored != zlib.crc32(kind + data): bad.append(kind.decode())
        pos += length + 12
    p = subprocess.run(['python3', str(root/'tools/validate_assets.py')],
                       capture_output=True, text=True)
    print('bad CRC chunks:', bad, 'validator exit:', p.returncode)
    print(p.stdout.rstrip())
    assert bad == ['IDAT'] and p.returncode == 0 and p.stdout.rstrip().endswith('PASS')
PY
```

Quan sát thực tế: `bad CRC chunks: ['IDAT']`, exit `0`, `PASS` (có WARN `google-services.json` thiếu trong cây scratch). Không cố sửa validator: ticket #53 chỉ cho ghi ba tài liệu audit.

## Kiểm tra bao bì APK bằng fixture tổng hợp

Chạy `tools/package_apk.py` trong `TemporaryDirectory` với một ZIP giả chỉ chứa tên `AndroidManifest.xml`, `classes.dex`, `resources.arsc`: packaging/verify exit 0; đổi một byte ZIP đầu ra thì verify exit 1 `Hash mismatch`; một ZIP thiếu hai mục thì exit 1 `Incomplete APK`. Đây chỉ là kiểm tra logic CRC/hash/thiếu mục của helper, **không phải APK Android hợp lệ, kiểm tra chữ ký, hay cài đặt**. CI có bước `apksigner` và verify artifact riêng; trạng thái thực được ghi ở báo cáo.

```sh
python3 - <<'PY'
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess, zipfile
with TemporaryDirectory(prefix='es-a02-package-') as t:
    d = Path(t)
    apk, out = d/'synthetic.apk', d/'out'
    with zipfile.ZipFile(apk, 'w') as z:
        for name in ('AndroidManifest.xml', 'classes.dex', 'resources.arsc'):
            z.writestr(name, 'synthetic test data, not a real APK')
    def run(*args):
        return subprocess.run(['python3', 'tools/package_apk.py', *args],
                              capture_output=True, text=True)
    packaged = run('--apk', str(apk), '--output', str(out))
    verified = run('--verify', str(out))
    archive = out/'Eternal-Summoner-Android.zip'
    data = bytearray(archive.read_bytes()); data[40] ^= 1; archive.write_bytes(data)
    tampered = run('--verify', str(out))
    incomplete = d/'incomplete.apk'
    with zipfile.ZipFile(incomplete, 'w') as z:
        z.writestr('AndroidManifest.xml', 'fake')
    missing = run('--apk', str(incomplete), '--output', str(d/'bad'))
    print('packaged, verified, tampered, missing:',
          packaged.returncode, verified.returncode, tampered.returncode, missing.returncode)
    print(tampered.stderr.strip().splitlines()[-1])
    print(missing.stderr.strip().splitlines()[-1])
    assert packaged.returncode == verified.returncode == 0
    assert tampered.returncode != 0 and missing.returncode != 0
PY
```

## Tự kiểm tra chất lượng

- `python3 tools/validate_assets.py` trên baseline: exit 0/PASS kèm WARN ảnh preview >3 MB, không có `app/google-services.json`, tên trùng ở các thư mục; không tự tạo credential/fake config.
- Chưa có Android SDK/JDK/Gradle/adb, không có ảnh thiết bị trước/sau; visual/gameplay/FPS/thermal là `NOT VERIFIED`, không chấm sáu chiều chất lượng bằng điểm giả.
- Sau phát hiện không sửa runtime/workflow do giới hạn ticket và writer khác; không có vòng fix ứng dụng. Tài liệu này là một vòng audit, yêu cầu review độc lập trên đúng SHA PR.
