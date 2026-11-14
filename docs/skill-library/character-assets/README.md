# Character, Meowa và Scenario cho ETERNAL SUMMONER

Theo yêu cầu học cả ba repo ngày 2026-11-14. Bản này lưu hướng dẫn dùng lại,
nguồn có commit cố định và danh mục đầy đủ; không huấn luyện trọng số mô hình.

| Nguồn | Commit đã đọc | Phạm vi lưu |
|---|---|---|
| [Character Animation Creator](https://github.com/tachikomared/character-animation-creator-skill) | `9ce98dffb98c84488fe7b99b62c49fabee20abd6` | 1 skill; ghi chú gốc của dự án, link cố định, hash 16 file nguồn; không chép code/media. README ghi MIT nhưng không có toàn văn giấy phép. |
| [Meowa](https://github.com/Meowa-AI/meowa-skills) | `b98f772f39378d9bb3d088851e7ffb75ee3d0b3b` | 1 skill nhiều module; giữ nguyên cả 37 file repo, trừ `.git`, có [MIT](meowa/upstream/LICENSE). |
| [Scenario](https://github.com/scenario-labs/skills) | `94cab3f1da47841f52d4c7b3ea8583aa13dd0eb9` | 179 skill: 172 thư viện + 7 bảo trì nguồn; [MIT](scenario/LICENSE). So với bản ZIP cũ: thêm 5, thay đổi 11, giữ nguyên 163, không bỏ skill. |

- Dùng [adapter của game](../../../.agents/skills/character-asset-pipeline/SKILL.md)
  để chọn phương pháp phù hợp; [ghi chú đọc và ánh xạ](READING_NOTES.md) ghi rõ
  phần đọc kỹ, phần chỉ lập danh mục, mâu thuẫn cần xử lý và giới hạn chưa kiểm chứng.
- [CATALOG.md](CATALOG.md) liệt kê **181 entrypoint**. [inventory.json](inventory.json)
  chứa tên, mô tả, mục lục, commit, nguồn, đường dẫn local, hash và độ đầy đủ file phụ.
- Meowa là bản tham chiếu đầy đủ, không cài vào thư mục skill toàn máy, không ghi đè
  skill `game-assets` khác. Script/provider runner nằm trong docs, chưa được thực thi.
- Scenario giữ nguyên snapshot ZIP cũ trong `../scenario/`. Bản này chỉ chép 16 văn
  bản mới/thay đổi, cùng đầy đủ file phụ của `scenario`, `scenario-sprite-animation`,
  `scenario-asset-analysis` tại `scenario/delta/`. 163 văn bản không đổi dùng lại file
  cũ đã xác minh hash. Các skill khác có thể thiếu file phụ local: đọc URL cùng commit
  trước khi dùng; `supporting_files_complete` không được suy ra từ việc có SKILL.md.

Chạy kiểm tra offline từ root game:

```bash
python docs/skill-library/character-assets/verify_snapshot.py
```

Kiểm tra byte/hash, tái sử dụng snapshot cũ, delta, danh mục/metadata, JSON, cú pháp
Python và link do dự án viết. Không import/chạy script nguồn, không gọi mạng/provider.
Kết quả thật lưu trong [verification.json](verification.json).

Game vẫn là Android Kotlin/Compose Canvas, oblique Y=.72. Tham số 2:1 isometric,
top-down, Three.js, Unity/Godot hay workflow xuất bản của nguồn không thay engine,
camera, world geometry, save, input hoặc cấu hình APK. Không tạo asset mới trong
nhiệm vụ học tài liệu này; chưa có kiểm chứng art chạy thật hay FPS thiết bị.
