# Skill dùng lại cho ETERNAL SUMMONER

Nguồn: `skills-main.zip`, MIT © 2026 Scenario Inc. Đã rà soát danh mục **174 SKILL.md**
(167 skill thư viện, 7 skill bảo trì repo nguồn), đọc kỹ 9 skill art/process phù hợp.
Không tuyên bố đã đọc từng dòng mọi script/reference phụ. Toàn bộ 174 văn bản chính
được lưu để lần sau đọc đúng phần cần thiết, thay vì nạp toàn bộ mỗi tác vụ.

Game hiện dùng Kotlin/Jetpack Compose Canvas, camera oblique 2.5D, ground Y .72.
Các skill Scenario mô tả cả phương pháp lẫn thao tác dịch vụ: **phương pháp có thể áp
dụng; thao tác MCP chưa dùng được vì phiên rà soát không có connector Scenario**.

| Việc cần làm | Skill | Áp dụng vào game |
|---|---|---|
| Hero/Goblin, idle/walk/attack/skill | `ai-game-spritesheets`, [scenario-sprite-animation](skill-library/scenario/upstream/skills/scenario-sprite-animation/SKILL.md) | Anchor trước, đếm pose thực, giữ hướng/pivot, kiểm tra frame order và loop seam, hit marker khớp damage. |
| Props, nhà, cây và lớp cảnh | [scenario-game-assets](skill-library/scenario/upstream/skills/scenario-game-assets/SKILL.md) | Tách geometry/style reference, alpha thật, footprint khác silhouette, thử một vùng nhỏ trước. |
| Cỏ, đất, đá, gỗ, nền seamless | [scenario-textures](skill-library/scenario/upstream/skills/scenario-textures/SKILL.md) | Kiểm tra hai trục seam và preview 2×2, giữ mật độ pixel; không thêm pipeline PBR vào Canvas chỉ vì tài liệu có ví dụ. |
| Nhận diện Kaelen/Goblin qua nhiều hướng | [scenario-consistency](skill-library/scenario/upstream/skills/scenario-consistency/SKILL.md) | Baseline cố định + một delta, luôn đối chiếu anchor gốc; seed không bảo đảm cùng nhân vật. |
| Thư viện nhân vật/props gốc | [scenario-identity-library](skill-library/scenario/upstream/skills/scenario-identity-library/SKILL.md) | Lưu mẫu đạt chuẩn, palette, hướng, phiên bản; sheet để review, ảnh view riêng để tham chiếu. |
| Kết hợp nhiều ảnh tham chiếu | [scenario-multi-reference](skill-library/scenario/upstream/skills/scenario-multi-reference/SKILL.md) | Mỗi ảnh một vai trò: identity, camera, pose, vật liệu hoặc palette; tránh trộn nhân vật của ảnh style. |
| Đo/check file asset | [scenario-asset-analysis](skill-library/scenario/upstream/skills/scenario-asset-analysis/SKILL.md) | Đo alpha bounds, kích thước, palette/hash bằng file thật; nhận xét thị giác không thay thế đo file. |
| Duyệt chất lượng | [scenario-quality-gate](skill-library/scenario/upstream/skills/scenario-quality-gate/SKILL.md) | Dùng rubric local khi thiếu dịch vụ; kiểm tra anatomy, vũ khí, phối cảnh và identity, không chỉ điểm số. |
| Sửa lỗi hình ảnh có giới hạn | [scenario-refine-loop](skill-library/scenario/upstream/skills/scenario-refine-loop/SKILL.md) | Chẩn đoán lỗi cụ thể, sửa phần lỗi, mỗi vòng một biến; không regenerate cả bộ khi chỉ một frame sai. |

## Chỉ mượn nguyên tắc, không chạy code engine khác

Nguồn nằm trong `skill-library/scenario/upstream/skills/game-engines/unity/`:

- `scenario-unity-mobile`: cùng máy/cảnh/save khi đo, kiểm tra safe area, input và bộ nhớ
  bitmap, thời gian tải; không coi Robolectric là đo FPS điện thoại.
- `scenario-unity-performance`: tìm CPU/GPU/I/O/GC bottleneck trước; phân biệt cold/warm;
  cache/cull dựa trên bằng chứng; dùng Perfetto, Android Studio/dumpsys cho dự án này.
- `scenario-unity-2d`: pixel density, atlas, nearest-neighbor, seam/bleed và shimmer khi pan.
- `scenario-unity-ui`: tách nội dung tĩnh và cập nhật liên tục, input/focus, safe area,
  text tiếng Việt và preview nhiều tỉ lệ; triển khai bằng Compose, không uGUI/UI Toolkit.

Không chép thông số ASTC, URP, HLSL, GI hoặc các số benchmark của Unity thành cấu hình
hay kết quả đo của ứng dụng Canvas này. Không đổi engine, camera hoặc hệ save.

## Có điều kiện / chưa phù hợp

10 skill có điều kiện: `scenario`, `scenario-image`, `scenario-image-editing`,
`scenario-formats`, `scenario-orbit-views`, `scenario-audio`, `scenario-sonilo`,
`scenario-storyboards`, `scenario-model-training`, `scenario-blender-2d-animation`.
Chỉ đọc khi tác vụ cần chỉnh ảnh, key art, âm thanh, cutscene, views, training hoặc
offline-render, và đã kiểm tra công cụ/quyền/budget. Không tự thêm các hệ này vào game.

83 skill còn lại về engine/DCC khác không áp dụng trực tiếp; 61 skill khác chủ yếu
về model/provider, media, marketing, 3D hoặc quản trị không thuộc phạm vi hiện tại.
7 skill bảo trì repo nguồn không được tự kích hoạt để rebase, merge hay gửi bình luận.
Xem từng tên và phân loại trong [CATALOG.md](skill-library/scenario/CATALOG.md).

## Lời nhắc lần sau

“Tôi sẽ dùng **[1–3 skill phù hợp]** cho **[phần việc]**, giữ camera 2.5D và pipeline
Kotlin/Compose hiện có.” Sprite work kết hợp thêm `ai-game-spritesheets` đã lưu trước đó.
Chỉ đọc nguồn liên quan. Giữ license và nguồn gốc khi tái sử dụng; không cần dịch vụ trả
phí để đọc hướng dẫn. Các file này là bộ nhớ trong repository, không phải huấn luyện mô hình.
