# Phase 0 dispatch board — snapshot, not an automated live queue

**Authority:** Use the state and latest comments of the **individual GitHub issue**, not this snapshot. Onboard new roles first. Do not recreate already-merged work. Do not assume Gemini assets/ZIP are in GitHub if branch absent.

| Task ID | Role | First deliverable | Initial state | Dependency |
|---|---|---|---|---|
| ES-A01-001 | A01 Arena Argent | Rà soát PR #50 so với #42 đã merge; lập sơ đồ file collision, chốt thứ tự nghiệm thu; không merge. | READY (audit/spec only) | Check live main/PR status |
| ES-A02-001 | A02 Android Build & CI Engineer | Audit trạng thái CI trên main và PR #50/#47; viết ma trận build/test có URL và SHA; chỉ sửa workflow khi phát hiện lỗi tái hiện được. | READY (audit/spec only) | Check live main/PR status |
| ES-A03-001 | A03 Graphics | Read-only audit P31-A3 integration interfaces, check actual renderer pivots + camera + visual layers; lên plan, KHÔNG tích hợp. | BLOCKED (no runtime writes) | PR #50 + art gate |
| ES-A04-001 | A04 HUD & UI/UX Engineer | Read-only accessibility/HUD audit trên main và PR #50; ghi điểm che game, labels, Back và contrast; không sửa source còn đang ở PR #50. | BLOCKED (no runtime writes) | PR #50 + art gate |
| ES-A05-001 | A05 Tower Level & Quest Designer | Read-only đánh giá PR #28 so với mục tiêu 5–10 phút chơi thật; draft Floor1 mission spec với 2 nhánh kết quả, không thay runtime. | READY (audit/spec only) | Check live main/PR status |
| ES-A06-001 | A06 Monster AI | Audit read-only 2 kiểu quái trong Floor 1, nêu test deterministic và hành vi mong đợi; giao đề xuất cho Codex. | READY (audit/spec only) | Codex critical-system approval |
| ES-A07-001 | A07 Hero Identity & Narrative Designer | Soạn original character consequences cho Floor1 rescue: 3 hero responses + 2 narrative outcomes, dạng spec chưa nối logic. | READY (audit/spec only) | Check live main/PR status |
| ES-A08-001 | A08 Pixel Art, Animation & Asset QA | Tiếp nhận P31-A3 bundle/ZIP khi được cung cấp, kiểm tra SHA/PNG/frame/manifest/CRC/negative tests, tạo ART-ONLY PR; nếu chưa có ZIP thì BLOCKED và nêu rõ. | BLOCKED (no runtime writes) | P31-A3 ZIP not yet in repository |
| ES-A09-001 | A09 Independent QA & Test Engineer | Review PR #50 exact HEAD R1–R4, xem test + screenshot, file PNG cũ có thay đổi không; gửi findings có severity, không merge. | READY (audit/spec only) | Check live main/PR status |
| ES-A10-001 | A10 Android Performance & Thermal Auditor | Read-only xác định đường dẫn SceneFrameLoop/render và measurement plan; checklist low/balanced/high; đề xuất canary profiling. | READY (audit/spec only) | Check live main/PR status |
| ES-A11-001 | A11 Gameplay Systems Engineer | Read-only audit Tower→Sanctuary feature wish list; viết proposal interface để Codex thẩm định, không implement save/event. | READY (audit/spec only) | Codex critical-system approval |
| ES-A12-001 | A12 Audio & SFX Designer | Audit audio pipeline; lập cue sheet cho Tower Floor1 (ambient, monster alert, impact, rescue result) và mapping events đề xuất. | READY (audit/spec only) | Check live main/PR status |

**Codex reserved task:** PR #47 critical combat review, #46 typed guest event/persistence review; no extra Codex work for routine polish. Existing #39, #41, #43, #44, #45, #46 and PR #28/#47/#50 remain references; no new issue overrides them.

## Live first-ticket links (created with PR #51)

- **A01 Arena Argent / Technical Director:** [Issue #52 — ES-A01-001](https://github.com/lightingfff/eternal-sunmmoner/issues/52)
- **A02 Android Build & CI:** [Issue #53 — ES-A02-001](https://github.com/lightingfff/eternal-sunmmoner/issues/53)
- **A03 Graphics / 2.5D Renderer:** [Issue #54 — ES-A03-001](https://github.com/lightingfff/eternal-sunmmoner/issues/54)
- **A04 HUD & UI/UX:** [Issue #55 — ES-A04-001](https://github.com/lightingfff/eternal-sunmmoner/issues/55)
- **A05 Tower Level Design:** [Issue #56 — ES-A05-001](https://github.com/lightingfff/eternal-sunmmoner/issues/56)
- **A06 Monster AI Tactics:** [Issue #57 — ES-A06-001](https://github.com/lightingfff/eternal-sunmmoner/issues/57)
- **A07 Hero Identity & Story:** [Issue #58 — ES-A07-001](https://github.com/lightingfff/eternal-sunmmoner/issues/58)
- **A08 Pixel Art & VFX:** [Issue #59 — ES-A08-001](https://github.com/lightingfff/eternal-sunmmoner/issues/59)
- **A09 Independent QA:** [Issue #60 — ES-A09-001](https://github.com/lightingfff/eternal-sunmmoner/issues/60)
- **A10 Android Performance:** [Issue #61 — ES-A10-001](https://github.com/lightingfff/eternal-sunmmoner/issues/61)
- **A11 Gameplay Systems:** [Issue #62 — ES-A11-001](https://github.com/lightingfff/eternal-sunmmoner/issues/62)
- **A12 Audio & SFX:** [Issue #63 — ES-A12-001](https://github.com/lightingfff/eternal-sunmmoner/issues/63)

**Bootstrap dependency:** merge [governance PR #51](https://github.com/lightingfff/eternal-sunmmoner/pull/51) first, then launch/connect the actual agents. A08 Issue #59 remains BLOCKED until P31-A3 ZIP/bundle is really supplied; do not claim local Gemini completion as a remote asset. Other ticket statuses allow at most scoped **read-only audit/spec** until separate production authorization is given.
