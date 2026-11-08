# Role matrix — one accountable writer per file

| ID | Agent | Domain | Kickoff task | Reviewer |
|---|---|---|---|---|
| A01 | Arena Argent / Technical Director | Điều phối tích hợp kỹ thuật, đọc PR, khóa giao diện giữa các module, bảo vệ kiến trúc Kotlin/Compose 2 | Rà soát PR #50 so với #42 đã merge | A09 + Codex (khi liên quan P1). |
| A02 | Android Build & CI Engineer | Kiểm tra Gradle/SDK36, APK, dependency, GitHub Actions và guest offline build | Audit trạng thái CI trên main và PR #50/#47 | A09. |
| A03 | Graphics / 2.5D Rendering Engineer | Hiển thị oblique, atlas, layering, shadows, VFX adapter; tối ưu kỹ thuật render mà giữ nguyên island | Read-only audit P31-A3 integration interfaces, check actual renderer pivots + camera + visual layers | A08 + A10 + A09. |
| A04 | HUD & UI/UX Engineer | Compose HUD, management screens, touch accessibility, landscape/portrait adaptation | Read-only accessibility/HUD audit trên main và PR #50 | A09. |
| A05 | Tower Level & Quest Designer | Thiết kế Floor 1 meadow, encounter cứu hộ, branching objectives và 5–10 phút nhịp chơi | Read-only đánh giá PR #28 so với mục tiêu 5–10 phút chơi thật | A06 + A07 + Codex cho core. |
| A06 | Monster AI / Encounter Tactics | Tập trung hành vi địch, telegraph, đường đi, counterplay; thiết kế và test độc lập khi battle API ổn định | Audit read-only 2 kiểu quái trong Floor 1, nêu test deterministic và hành vi mong đợi | Codex + A09. |
| A07 | Hero Identity & Narrative Designer | Thiết kế hero memory, fear/morale, NPC rescue, personality, original dialogues | Soạn original character consequences cho Floor1 rescue: 3 hero responses + 2 narrative outcomes, dạng spec chưa nối logic. | A05 + Codex khi liên quan save. |
| A08 | Pixel Art, Animation & Asset QA | 64×64 tileset, spritesheet, palette, pivots, animation timing, asset validation; thay Gemini | Tiếp nhận P31-A3 bundle/ZIP khi được cung cấp, kiểm tra SHA/PNG/frame/manifest/CRC/negative tests, tạo ART-ONLY PR | A09 + A03 + Codex contract. |
| A09 | Independent QA & Test Engineer | Test độc lập, audit diff, test save/restart/UI/AI và yêu cầu sửa bằng chứng rõ | Review PR #50 exact HEAD R1–R4, xem test + screenshot, file PNG cũ có thay đổi không | A01 + Codex (P1); reviewer independent. |
| A10 | Android Performance & Thermal Auditor | Đánh giá main thread, bitmap cache, culling, frame pacing, GC, thermal; không hạ đồ họa bừa | Read-only xác định đường dẫn SceneFrameLoop/render và measurement plan | A03 + A09. |
| A11 | Gameplay Systems Engineer | Quest inventory, rewards UI glue, building activities, progression features không đụng critical persistence | Read-only audit Tower→Sanctuary feature wish list | Codex (mandatory core contract) + A09. |
| A12 | Audio & SFX Designer | Bản đồ âm thanh gameplay, ưu tiên công cụ sẵn có, license sạch, độ trễ thấp Android | Audit audio pipeline | A05 + A10. |

**Codex:** senior for core combat/tactical commands, transactional guest save, event ID/idempotency and critical architectural QA. Not to be used for simple sprite, documentation or UI polish. Do not alter PR #47 or #46 contract without Codex review.

**Critical files:** `LocalGuestSaveManager.kt`, `SummonerViewModel.kt` event settlement, `AutonomousHeroAi.kt`, `ContactAttackTimeline.kt`, shared camera/map/renderer/Gradle files: single writer only, with approval and explicit handoff. Domain directory globs are NOT automatic authorization: a TASK must enumerate exact files. A03/A08/A10 overlap = choose sole owner for that task; A01 does serial integration. Any cross-role change requires separate issue or explicit approval before editing.

**Kickoff order**: A01+A09 review PR #50; A02 CI audit; A08 import/verify Gemini P31-A3 *only when archive supplied*; Codex handles PR #47; A03,A04 hold integration until #50 and art acceptance. A05–A07,A11,A12 may independently draft limited read-only designs/specs.
