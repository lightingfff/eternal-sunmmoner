# ETERNAL SUMMONER — Enterprise Auto Agent Dispatcher Specification v2

**Authority:** Human Game Director (`@lightingfff`). Sole merge authority (`no_self_merge: true`).
**Scope:** Automated Issue Triage, `STATE: READY` Verification, Dependency Checks, Single-Writer Lock Enforcement, Backend Execution Adapters, and PR Dual-Lead Review Routing.

---

## 1. Sửa Lỗi Workflow GitHub Actions (Run `42494064587`)

- **Nguyên nhân gốc rễ**: Tại `.github/workflows/agent-dispatcher.yml`, trường `inputs.dry_run.default` khai báo giá trị chuỗi `'true'` trong khi `type: boolean`. Parser của GitHub Actions yêu cầu boolean literal (`default: true`).
- **Khắc phục**:
  - Chuẩn hóa `default: true`.
  - Bổ sung job `test_dispatcher_engine` chạy trực tiếp bài test unit `PYTHONPATH=. python3 tools/dispatcher/test_agent_dispatcher.py` trước mọi bước dispatch.

---

## 2. Quy Trình Điều Phối 5 Cổng An Toàn (5-Gate Pipeline)

Mỗi khi một Issue được gắn nhãn `ai:ready` hoặc được kích hoạt thủ công, `agent_dispatcher.py` thực hiện tuần tự qua 5 cổng kiểm tra nghiêm ngặt:

1. **Cổng 1 — Trạng Thái `STATE: READY`**:
   - Issue **bắt buộc** phải chứa khai báo `STATE: READY` tường minh.
   - Nếu ở trạng thái `BACKLOG`, `BLOCKED`, hoặc `IN_PROGRESS`, hệ thống lập tức trả về `BLOCKED` và dừng lại.
2. **Cổng 2 — Bóc Tách Vai Trò & Nạp Kỹ Năng (Role & Skill Mapping)**:
   - Tự động nhận diện A01–A12, ARGON, CODEX.
   - Nạp đúng tệp kỹ năng tại `.agents/skills/es-aXX-*/SKILL.md`.
3. **Cổng 3 — Kiểm Tra Phụ Thuộc (Dependencies Met)**:
   - Quét trường `DEPENDENCIES: #xx, #yy`.
   - Đối soát trạng thái thực tế của các PR/Issue phụ thuộc. Nếu phụ thuộc chưa `MERGED` hoặc `RESOLVED`, hệ thống lập tức gắn cờ `BLOCKED: Unmet dependencies`.
4. **Cổng 4 — Khóa Chống Va Chạm File (Single Accountable Writer)**:
   - So khớp danh sách đường dẫn được phép sửa (`allowed_files`) với ma trận file lock đang được giữ bởi các issue/PR khác.
   - Nếu có bất kỳ đường dẫn nào trùng lặp, chặn ngay lập tức và thông báo tên issue đang giữ lock.
5. **Cổng 5 — Backend Execution Adapter**:
   - Chỉ trả về `DISPATCHED` khi backend CLI/API thực sự được gọi thành công.
   - Tuyệt đối không tạo claim giả lập hoặc tự tuyên bố đã nạp skill nếu backend chưa chạy thật.

---

## 3. Danh Mục Backend Adapters

| Backend ID | Đối Tượng Áp Dụng | Cơ Chế Xác Thực & Ranh Giới An Toàn |
|---|---|---|
| **`arena_session`** | A01 (và các agent có phiên Arena tương tác) | **Không hỗ trợ webhook/chạy ngầm.** Theo đúng `CLAIM_PROTOCOL.md`, bình luận GitHub không thể tự khởi động phiên Arena. Dispatcher tự động trả về `BLOCKED` kèm hướng dẫn người dùng khởi tạo phiên làm việc thủ công. |
| **`gemini_cli`** | A02, A03, A04, A05, A07, A08, A11, A12, ARGON | Xác thực qua `GEMINI_API_KEY` hoặc `GEMINI_CLI_ENABLED=true`. Tích hợp giới hạn token (8192 tokens), timeout (120s) và retry tối đa 3 lần. |
| **`codex_cli`** | A06, A09, A10, CODEX | Xác thực qua `OPENAI_API_KEY` hoặc `CODEX_API_KEY`. Tích hợp timeout (180s) và retry tối đa 2 lần cho các hệ thống combat/save trọng yếu. |

---

## 4. Định Tuyến Dual-Lead Review Cho PR

Tuân thủ `DUAL_LEAD_WORKFLOW.md`:
- Mọi PR mới mở sẽ kích hoạt phân luồng review song song:
  - **Argon (`[ARGON_REVIEW]`)**: Đánh giá mỹ thuật, tỷ lệ $Y=0.72$, Nearest-Neighbor, layout và game feel.
  - **Codex (`[CODEX_REVIEW]`)**: Đánh giá kiến trúc Kotlin, độ trễ Master command, an toàn save offline và tính bất biến của core.
  - **A09 (`[QA_REVIEW]`)**: Kiểm thử độc lập bằng chứng Robolectric, talkback và độ tương phản.
- **Quy tắc bất biến**: `no_self_merge: true` — Chỉ duy nhất Human Game Director có quyền duyệt và merge PR.

---

## 5. Kết Quả Kiểm Thử (Unit Tests)

Bộ kiểm thử `tools/dispatcher/test_agent_dispatcher.py` bao gồm 7 kịch bản:
- `test_state_ready_enforcement`: Chặn khi thiếu hoặc sai trạng thái `READY`.
- `test_dependency_gate`: Chặn khi PR phụ thuộc chưa merge.
- `test_file_collision_two_agents_competing`: Chặn khi 2 agent cùng yêu cầu sửa 1 thư mục/tệp.
- `test_arena_agent_interactive_only_blocked_in_background`: Chặn chạy ngầm đối với Arena session.
- `test_backend_unauthenticated_blocked`: Chặn khi thiếu API key/CLI credentials.
- `test_backend_authenticated_dry_run_success`: Kiểm tra luồng dry-run khi có cấu hình hợp lệ.
- `test_pr_dual_lead_review_routing`: Kiểm tra định tuyến Argon + Codex + A09.

**Kết quả chạy**: 7/7 test passed (`OK`).
