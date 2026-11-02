# CODEX ETERNAL — Parallel Game Development Team

Bộ cấu hình repo-scoped cho Codex đa agent; không phải fine-tuning model. Căn cứ tài liệu Codex Subagents: https://learn.chatgpt.com/docs/agent-configuration/subagents

## Trạng thái repo lúc soạn
- Repo: `lightingfff/eternal-sunmmoner`
- `AGENTS.md`, `.agents/skills/eternal-summoner-skill-router/`, `docs/GAME_SKILL_SELECTION.md` đã có.
- P22 đã merge. P23 Environment Cohesion & Architecture Repair: PR #26 đang mở; KHÔNG để nhóm Codex sửa/chạm cùng asset, renderer hoặc geometry khi PR này chưa hoàn tất.
- Repo chưa có `.codex/` tại lúc tra cứu. Trước khi chép, kiểm tra nhánh và file lại vì mọi thứ có thể thay đổi.

## Cách cài trong repo
1. Mở repo trên Codex (máy tính hoặc workspace có quyền chỉnh sửa). Tạo nhánh cấu hình từ `main` mới nhất, không lấy nhánh P23 đang làm đồ họa.
2. Chép `.codex/`, `.agents/skills/codex-eternal-director/`, `.agents/skills/codex-tower-rescue/` vào root repo, **merge chứ không ghi đè** các file/skill hiện có.
3. Đọc `CODEX_SETUP_INSTRUCTIONS.txt` và `CODEX_FIRST_MISSION.txt`, giao prompt cho Codex chính.
4. Codex chính lập kiểm kê và phân quyền file trước, dùng các subagent TOML đã đăng ký khi giao nhiệm vụ.
5. Tạo PR cấu hình trước nếu cần. Nhiệm vụ gameplay thực hiện ở branch khác; không auto-merge.

Cấu hình `.codex/config.toml` dùng `[agents] enabled = true; max_concurrent_threads_per_session = 4`. Trong bản Codex hỗ trợ, file `.codex/agents/*.toml` đặt tên agent và hướng dẫn riêng; không khóa model để tự dùng model mà phiên Codex của người dùng cho phép.

## Đội hình
- `gameplay_architect`: đọc kiến trúc, thiết kế battle loop + lệnh Master; có thể code module gameplay được phân quyền.
- `tower_encounter_builder`: nội dung rescue Tower Floor 1; triển khai objectives, choices, state/encounter ở vùng file được phân quyền.
- `android_performance_auditor`: audit độc lập, read-only; chỉ sửa nếu chủ tọa mở một nhiệm vụ riêng.
- `integration_reviewer`: read-only QA, contract-test strategy và duyệt PR.

**Điểm sống còn:** Agent song song = song song theo *đầu việc độc lập*, không phải nhiều người ghi lên cùng `TowerFloorCatalog.kt` hay `PixelSpriteRenderer.kt`. Codex chính giữ quyền chọn file shared, tích hợp thay đổi và quyết định thứ tự. Quy trình đúng: audit song song → plan/file ownership → implement song song ở file riêng → integrate tuần tự → CI/render/QA → PR.

## Mục tiêu đầu tiên

Một rescue vertical slice **thật sự chơi được 5–10 phút** ở Tower Floor 1 với cảnh cánh đồng, chiến thuật của Master (focus/regroup/retreat), AI quái có khác biệt và kết cục lưu lại. Có victory/failure/retry/continue, animation/sfx nếu có asset hợp pháp từ repo; không fake gameplay bằng màn mock.

## Phân biệt rõ các loại bằng chứng
- CI xanh: build/test trong môi trường CI, KHÔNG phải FPS/nhiệt trên máy thật.
- Robolectric render: ảnh qua engine, KHÔNG phải screenshot thiết bị thực.
- `ScenePerformanceProbe`: FPS/p95 và CPU draw submission; KHÔNG đo thời gian GPU thực, nhiệt hoặc GC chi tiết.
- Chỉ khi người dùng chạy APK trên Android thật mới gọi là on-device verification.

## Giới hạn việc train

Skill/TOML là **instruction engineering** và workflow training, không biến Codex thành Claude Opus hay huấn luyện trọng số.
