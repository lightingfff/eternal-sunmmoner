# ETERNAL SUMMONER — Auto Agent Dispatcher Specification

**Authority:** Game Director (human owner). Single merge authority; no automatic merges (`no_self_merge: true`).
**Scope:** Automated triage, role mapping, file-lock collision prevention, and dual-review routing for A01–A12, Gemini 4 Argon High, and OpenAI Codex.

---

## 1. Kiến Trúc Điều Phối (Architecture Overview)

```
                         GitHub Issue (Label: ai:ready)
                                       │
                                       ▼
                     [GitHub Action: agent-dispatcher.yml]
                                       │
            ┌──────────────────────────┴──────────────────────────┐
            ▼                                                     ▼
 [Triage & Role Extraction]                              [Dependency Check]
   - Parse A01–A12 / ARGON / CODEX                         - Verify prerequisite PRs/Issues
   - Load mapped role skill                                - Block if dependencies unmet
            │
            ▼
 [File Collision Matrix Check]
   - Compare requested paths with active locked files
   - If overlap: report [BLOCKED] COLLISION
            │
            ▼
 [Backend CLI / API Availability Check]
   - Check if GEMINI_API_KEY / GEMINI_CLI_ENABLED or CODEX_API_KEY is configured
   - Arena Argent: Runs interactively in session; no external webhook
   - If not configured: RUN IN DRY RUN / SIMULATION MODE ONLY
            │
            ▼
 [Standardized Artifact Generation]
   - Generate exact [CLAIM] comment payload
   - Name target branch: agent/<role>-issue<number>-auto
   - Post plan / log to Issue
```

---

## 2. Quy Tắc Bảo Vệ Dự Án (Safety Invariants)

1. **Khóa Quyền Sở Hữu File (Single Accountable Writer per File)**:
   - Mỗi file/directory chỉ có **duy nhất một agent** được cấp quyền ghi tại một thời điểm.
   - Khi có xung đột file giữa 2 task, dispatcher tự động gắn cờ `COLLISION` và giữ issue ở trạng thái chờ A01 điều phối.
2. **Không Tự Động Merge (No Self-Merge)**:
   - Tất cả PR do agent tạo ra phải được duyệt qua **Review Gates**:
     - `[ARGON_REVIEW]`: Thẩm định mỹ thuật, tỷ lệ $Y=0.72$, Nearest-Neighbor, game feel.
     - `[CODEX_REVIEW]`: Thẩm định kiến trúc Kotlin/Compose, combat core, transactional save, invariants.
     - `[QA_REVIEW]` (A09): Kiểm thử độc lập, bằng chứng headless Robolectric / CI APK.
   - **Chỉ có duy nhất Human Game Director có quyền bấm nút merge trên GitHub.**
3. **Bảo Vệ Branch Main**:
   - Mọi agent chỉ được phép đẩy lên branch riêng dạng `agent/<role>-...`.
   - Cấm tuyệt đối push trực tiếp lên `main` hoặc dùng `git push --force`.
4. **An Toàn Chi Phí & API (Cost & Privacy Guardrails)**:
   - Trong giai đoạn đầu, hệ thống mặc định chạy ở chế độ **Dry-Run / Simulation Mode**.
   - Không tự động gọi API sinh phí nếu chưa có biến môi trường xác thực tường minh.
   - Không yêu cầu API key công khai trong mã nguồn hoặc issue comments.

---

## 3. Bản Đồ Role & Skill Mapping

| Role ID | Tên Chuyên Trách | Backend CLI | File Skill Mặc Định |
|---|---|---|---|
| **A01** | Arena Argent / Technical Director | `arena_cli` (Interactive) | `.agents/skills/es-a01-technical-director/SKILL.md` |
| **A02** | Android Build & CI Engineer | `gemini_cli` | `.agents/skills/es-a02-android-build/SKILL.md` |
| **A03** | Graphics / 2.5D Renderer Engineer | `gemini_cli` | `.agents/skills/es-a03-graphics-renderer/SKILL.md` |
| **A04** | HUD & UI/UX Engineer | `gemini_cli` | `.agents/skills/es-a04-ui-ux/SKILL.md` |
| **A05** | Tower Level & Quest Designer | `gemini_cli` | `.agents/skills/es-a05-tower-level-designer/SKILL.md` |
| **A06** | Monster AI / Encounter Tactics | `codex_cli` | `.agents/skills/es-a06-monster-ai-designer/SKILL.md` |
| **A07** | Hero Identity & Narrative Designer | `gemini_cli` | `.agents/skills/es-a07-hero-narrative/SKILL.md` |
| **A08** | Pixel Art, Animation & Asset QA | `gemini_cli` | `.agents/skills/es-a08-pixel-art-vfx/SKILL.md` |
| **A09** | Independent QA & Test Engineer | `codex_cli` | `.agents/skills/es-a09-regression-qa/SKILL.md` |
| **A10** | Android Performance & Thermal Auditor | `codex_cli` | `.agents/skills/es-a10-android-performance/SKILL.md` |
| **A11** | Gameplay Systems Engineer | `gemini_cli` | `.agents/skills/es-a11-gameplay-systems/SKILL.md` |
| **A12** | Audio & Sound Designer | `gemini_cli` | `.agents/skills/es-a12-audio-sound/SKILL.md` |
| **ARGON** | Gemini 4 Argon High (Creative Co-Lead) | `gemini_cli` | `.agents/skills/eternal-argon-creative-technical-lead/SKILL.md` |
| **CODEX** | OpenAI Codex (Senior Critical-Systems) | `codex_cli` | `.agents/skills/codex-eternal-director/SKILL.md` |

---

## 4. Cách Chạy Test Mô Phỏng

Chạy test bộ phân luồng mô phỏng cục bộ trong repo:
```bash
PYTHONPATH=. python3 tools/dispatcher/test_agent_dispatcher.py
```
Kết quả kiểm thử: 7/7 test passed (bao gồm role extraction, file collision, dry-run safety, dual review routing).
