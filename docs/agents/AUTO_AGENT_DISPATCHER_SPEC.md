# ETERNAL SUMMONER — Enterprise Auto Agent Dispatcher Specification v3

**Authority:** Human Game Director (`@lightingfff`). Sole merge authority (`no_self_merge: true`).
**Scope:** Live GitHub API Issue Reading, Real-Time Dependency & File Ownership Verification, Production Backend Adapters (Gemini CLI / Codex / Arena Session), Budget Guardrails, and Dual-Lead Review Routing.

---

## 1. Nâng Cấp Nổi Bật Trong Bản V3 (Live Integration)

1. **Đọc Dữ Liệu Thật từ GitHub API (`fetch_live_issue`)**:
   - Không còn sử dụng dữ liệu giả lập. Dispatcher truy vấn trực tiếp GitHub API qua `gh api repos/:owner/:repo/issues/{number}` để lấy toàn bộ `title`, `body`, `labels`, và `state`.
2. **Kiểm Tra Trạng Thái Dependencies Thực Tế (`fetch_live_dependency_status`)**:
   - Quét trường `DEPENDENCIES: #xx, #yy`.
   - Gọi GitHub API kiểm tra xem PR đã `MERGED` hay Issue đã `CLOSED`. Nếu chưa hoàn thành, nhiệm vụ lập tức bị chặn với `BLOCKED: Unmet dependencies`.
3. **Ma Trận Khóa File Chống Xung Đột (Single Accountable Writer)**:
   - So khớp các đường dẫn được cấp phép sửa với các PR/Issue đang mở.
   - Nếu phát hiện tranh chấp giữa 2 agent khác nhau, lập tức chặn và chỉ rõ issue đang chiếm giữ khóa file.
4. **Adapter Backend Với Lời Gọi Thực Sự (Real Invocations)**:
   - **`gemini_cli`**: Kiểm tra `GEMINI_API_KEY` / `GEMINI_CLI_ENABLED`. Kiểm soát ngân sách qua `ALLOW_PAID_API` (mặc định false). Khi được phép, thực thi lệnh `gemini run` thực tế với timeout (120s) và retry lũy thừa tối đa 3 lần.
   - **`codex_cli`**: Kiểm tra `OPENAI_API_KEY` / `CODEX_API_KEY`. Khi được phép, thực thi lệnh `codex exec` thực tế với timeout (180s) và retry tối đa 2 lần.
   - **`arena_session`**: Chặn các lệnh cố gắng chạy ngầm không hỗ trợ; yêu cầu khởi tạo phiên trực tiếp theo đúng `CLAIM_PROTOCOL.md`.
   - **Tính toàn vẹn phán quyết**: **Tuyệt đối không trả về `DISPATCHED` nếu lời gọi backend chưa thành công.**
5. **Định Tuyến Review Dual-Lead PR Thực Tế**:
   - Tự động kiểm tra trạng thái kết nối của Argon và Codex. Nếu backend chưa kết nối, gắn cờ `BLOCKED_BACKEND_UNAVAILABLE`.
   - Tuân thủ quy tắc bất biến: `no_self_merge: true`.

---

## 2. Hướng Dẫn Bật Chế Độ Tự Động Thực Sự (Production Enablement)

Khi Game Director quyết định bật tự động hoàn toàn:
1. Cấu hình Repository Secrets trên GitHub:
   - `GEMINI_API_KEY`: API key cho Gemini 4 Argon.
   - `OPENAI_API_KEY` / `CODEX_API_KEY`: API key cho OpenAI Codex.
2. Bật cờ cho phép gọi API có phí trong workflow environment:
   - `ALLOW_PAID_API=true`
3. Gán nhãn `ai:ready` cho Issue mục tiêu. Hệ thống sẽ tự động quét, xác thực và thực thi.

---

## 3. Kết Quả Kiểm Thử (Unit & Integration Tests)

Bộ kiểm thử tại `tools/dispatcher/test_agent_dispatcher.py` đã chạy thực tế đạt **6/6 test passed (`OK`)**:
- Kiểm tra cổng `STATE: READY`.
- Kiểm tra bóc tách và giải quyết dependency thật/giả lập.
- Kiểm tra chống tranh chấp file giữa 2 agent.
- Kiểm tra rào chắn an toàn cho Arena interactive session.
- Kiểm tra xác thực và ngân sách an toàn (`ALLOW_PAID_API`) của Gemini/Codex.
- Kiểm tra định tuyến review PR.
