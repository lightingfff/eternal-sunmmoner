# ES-A05-001 — Review read-only PR #28 (Mission 01 Floor 1 rescue) so với mục tiêu 5–10 phút chơi thật

- **Người review:** A05 Tower Level & Quest Designer — một phiên Arena Agent Mode (`arena/df5f7758-eternal-sunmmoner`), vai logical, không phải background worker.
- **Ngày:** 2026-11-08 (UTC). **Ticket:** [#56](https://github.com/lightingfff/eternal-sunmmoner/issues/56) · **Điều phối:** #39.
- **Target:** PR [#28](https://github.com/lightingfff/eternal-sunmmoner/pull/28) — head `09c553d81c53046fe1038b70c7046e2a6cff0661`, nhánh `mission01/floor1-rescue`.
- **Baseline main đối chiếu:** `bdff6f9976dc4927e1e095a503bc339509c7a9aa`.
- **Phương pháp:** đọc trực tiếp source tại head PR (git fetch pull/28/head), đọc 3 CSV trace `docs/visual-evidence/mission01b/`, chạy script Python tính lại thời lượng/tỉ lệ từ chính các CSV đó (số liệu bên dưới là từ CSV, không phải chạy lại simulation). **Không** sửa file nào của PR #28. **Không** chạy Gradle (sandbox không có JDK/Android SDK), **không** playtest người thật, **không** thiết bị Android.

## 0. Trạng thái PR tại thời điểm review (live GitHub)

| Mục | Giá trị |
|---|---|
| State | OPEN, **DRAFT** |
| Mergeable | **CONFLICTING** (`mergeStateStatus=DIRTY`) với main hiện tại |
| CI trên head | `gh pr checks 28` → **"no checks reported"** — chưa có CI nào chạy trên head này |
| Bằng chứng test trong PR | Báo cáo cloud worker (42→62→85 test PASS theo từng chặng, `docs/MISSION01_RESCUE.md`, `MISSION01B_REPORT_VI.md`, `MISSION01D_REPORT_VI.md`) — **không phải remote CI**; chính các báo cáo này đã tự ghi nhận giới hạn |

Kết luận quy trình: PR chưa đủ điều kiện merge cả về mặt kỹ thuật (conflict, thiếu CI exact-SHA) lẫn mặt gameplay (xem dưới). Đây là review thiết kế theo ticket ES-A05-001, không thay thế `[CODEX_REVIEW]`/`[QA_REVIEW]`.

## 1. Số liệu thật từ trace CSV (3 lượt new-guest, party khởi đầu không chỉnh sửa)

Nguồn: `journey_true_SUPPORT_COURIER_false.csv`, `journey_false_RECOVER_SUPPLIES_true.csv`, `journey_true_RECOVER_SUPPLIES_false.csv` @ 09c553d8. Test nguồn: `Mission01BPlaythroughTest.kt` (dt=1/60, không teleport/ép chết/quên timer).

| Nhánh | Tổng sim (s) | Travel hành trình (s) | Combat hành trình (s) | Outcome |
|---|---:|---:|---:|---|
| Tắt báo động → Support | 318,30 | 221,17 (**69,5%**) | 12,85 (**4,0%**) | RESCUED |
| Cứu trinh sát → Supplies | 317,72 | 221,17 (**69,6%**) | 12,25 (**3,9%**) | SUPPLIES_RECOVERED |
| Tắt báo động → bỏ lỡ pickup | 317,15 | 221,17 (**69,7%**) | 11,67 (**3,7%**) | RESCUED_WITHOUT_SUPPLIES |

Thời lượng từng chapter (nhánh Support, từ các mốc `COMPLETED_m01b_*`):

| Chapter | Kết thúc (s) | Dài (s) | Ghi chú pacing |
|---|---:|---:|---|
| archives | 42,6 | 42,6 | 2 archer + 2 field step; mở đầu 27,6s mới có lệnh đầu tiên |
| recon | 69,6 | 27,0 | không địch; 1 field step "đi tới xem dấu" |
| wardens | 97,7 | 28,1 | 2 wolf bị dọn trong ~3,2s focus; 2 field step |
| signal | 116,0 | 18,4 | 1 raider; 2 gương + **quyết định nhánh duy nhất giữa hành trình** |
| crossing | 142,5 | 26,4 | 2–3 địch; 2 field step; sau đó **36,3s không có sự kiện có nghĩa** (gap lớn nhất trace) |
| medicine | 181,7 | 39,2 | không địch; 2 field step |
| seal | 214,5 | 32,8 | 2 địch; puzzle thứ tự 3 ấn (đúng/sai không mất gì ngoài thử lại) |
| relay | 241,8 | 27,3 | không địch; 1 interact |
| debrief | 267,1 | 25,3 | không địch; quay về đúng điểm xuất phát |
| Escort đuôi cũ | 318,3 | 51,2 | choice + scout alarm + Kraag; lưu ý: test không record dodge/counter thành event nên gap 35,3s cuối **gồm cả thời gian đánh Kraag không được ghi nhận**, không phải thuần đi bộ |

Đối chiếu hình học: route 21 điểm trong `RescueJourneyContent.routePoints` dài **~9.891 world units**; courier 44u/s (`RescueJourney.kt` — `44f * dt`) ⇒ **~225s đi đường thuần về mặt toán học**, khớp số đo 221,17s. Nói cách khác: **thời lượng 300–600s mà test áp đặt (`seconds in 300f..600f`) đạt được chủ yếu bằng độ dài quãng đường, không bằng mật độ nội dung**.

## 2. Đối chiếu mục tiêu 5–10 phút chơi thật

Mục tiêu của dự án (AGENTS.md, REVIEW_GATES mục 5, skill tower-director): 5–10 phút **chơi thật có chất lượng**, không đệm chờ nhân tạo; 55s simulation không phải 5–10 phút người chơi.

- **Đạt về hình thức:** 317–318s simulation nằm trong cửa sổ 5–6,3 phút; nhánh trước đây ~54–55s đã được thay bằng hành trình dài hơn thật, không dùng timer chờ.
- **Không đạt về chất lượng (kết luận chính):**
  1. **69,6% thời gian là courier đi đường**, chỉ ~3,9% là combat hành trình. Phần còn lại chủ yếu là field action kiểu "MOVE_TO tới điểm → bấm xác nhận", không rủi ro, không chi phí cơ hội.
  2. **Gap tới 36,3s** không có sự kiện có nghĩa (crossing→medicine, đoạn này thuần di chuyển vì medicine không có địch); các gap 25–28s khác ở mở màn, relay, debrief. Riêng gap 35,3s cuối escort có chứa trận Kraag nhưng test không ghi dodge/counter thành event — cần telemetry tốt hơn trước khi kết luận về đoạn này. Với người chơi thật, các đoạn thuần di chuyển vẫn là đoạn chết.
  3. **Áp lực chiến đấu quá thấp:** mỗi chapter 1–2 địch (signal 1 raider 155 HP; wardens 2 wolf chết trong ~3,2s) so với party 3 người khởi đầu; courier giữ 300/300 HP gần như cả hành trình trong cả 3 trace. Không có tình huống nào buộc đổi đội hình/đổi lệnh giữa chừng ngoài kịch bản đã dạy.
  4. **Nhánh quyết định thưa và yếu cảm nhận:** giữa hành trình chỉ có 1 quyết định (signal: tắt báo động vs cứu trinh sát); hệ quả khác biệt quan sát được chỉ là +1 archer ở crossing và vài % hồi máu — người chơi khó cảm nhận. Choice chính (Support/Supplies) chỉ xuất hiện ở ~51s cuối.
  5. **Lặp không gian:** debrief đưa đoàn về đúng spawn (600,1049) rồi escort lại đi về phía Đông trên chính con đường cũ → cảm giác "đi hai lần một đường". Map mở rộng 6200×3600 + tối đa 155 prop grove mới (vòng lặp có điều kiện khoảng cách) làm tăng diện tích phải đi nhưng chưa tăng quyết định; chi phí render/bộ nhớ của phần mở rộng **chưa đo trên thiết bị** (chính PR ghi NOT TESTED).
- **Human wall-time: NOT TESTED.** Không có playtest người thật/emulator/thiết bị trong PR; con số 317s là simulation theo kịch bản lệnh tối ưu viết sẵn trong test, không phải thời gian người thật (người thật sẽ chậm hơn ở khâu đọc/chọn, nhưng điều đó không biến đoạn chết thành nội dung).

## 3. Điểm tốt cần giữ nguyên khi rework (không đập đi xây lại)

1. **Hợp đồng outcome/save trung thực:** 4 outcome tách bạch; `RESCUED_WITHOUT_SUPPLIES` không phát thưởng giả; thưởng lần đầu +20 Mana / +60 Vàng đọc từ cờ đã lưu trong transaction; tương thích save cũ (`hasSupportOutcome` suy luận từ `COURIER_RESCUED` cũ); cờ vĩnh viễn sống sót qua restart/compaction (`LostCourierProgression.kt`, test persistence + restart trong `Mission01BPlaythroughTest`).
2. **Pickup vật lý thật:** phải giữ hero sống trong 90u đủ 3s liên tục, rời ra là reset — đúng chuẩn "objective kiểm chứng bằng vị trí".
3. **Kraag telegraph/counter:** vòng đỏ 1,5s → né bằng MOVE_TO → hồi sức 1,8s ăn +35% sát thương khi FOCUS_FIRE (`TowerCombatEngine.kt:874-876`); scout 3s bỏ chạy + 6s thổi tù và, chặn được thì không có viện binh; PROTECT giảm 40% sát thương cho courier. Đây là hạt nhân tactical tốt, cần **nhiều tình huống dùng nó hơn**, không phải thay nó.
4. **Giới hạn spawn hữu hạn:** test khóa `peakLiveEnemies ≤ 12`, `peakRoster ≤ 40` — không spam wave kéo giờ.
5. **Retreat/retry đúng:** retreat đi ngược waypoint đã qua về spawn thật, FAILED được lưu, retry tạo encounter mới sạch (2 test retreat trong `Mission01BPlaythroughTest`).
6. **Fear + hero agency bước đầu:** alert theo tính cách, lệnh REGROUP/PROTECT hồi fear nhanh hơn — nền tốt cho A07 mở rộng, chưa phải hệ stress/trust hoàn chỉnh.
7. **Additive map:** meadow gốc 3400×2150 và prop cũ giữ nguyên tọa độ (`buildRescueJourneyMap` chỉ copy + thêm).

## 4. Findings có severity (kèm đường dẫn để chủ file sửa, A05 không tự sửa)

| # | Severity | Finding | Bằng chứng | Hướng xử lý đề xuất (chủ sở hữu) |
|---|---|---|---|---|
| F1 | **P1** | Pacing dựa vào quãng đường: 69,6% travel, combat ~4%, gap 36,3s; cửa sổ 300–600s trong test khuyến khích kéo dài đường thay vì nội dung | CSV §1; `RescueJourneyContent.routePoints` ~9.891u; `Mission01BPlaythroughTest` assert `seconds in 300f..600f` | Rút route còn ~4.500–5.000u, dành thời gian dôi ra cho encounter/choice dày hơn; đổi tiêu chí test từ "tổng giây" sang **mật độ sự kiện + tỉ lệ travel/combat** (đặc tả mẫu: `FLOOR1_MEADOW_RESCUE_SPEC.md`) — Codex/A05/A06 |
| F2 | **P1** | Quyết định có hậu quả quá thưa: 1 choice giữa hành trình, hệ quả nhánh signal khó cảm nhận (+1 archer, vài % heal) | `RescueJourney.kt` (`chooseRoute`, `spawnThreats` crossing), `interact` medicine .50/.35 | Mỗi beat lớn phải có ≥1 quyết định đổi được cục diện (đường tắt rủi ro vs đường an toàn, cứu người vs giữ tài nguyên…); hệ quả phải hiện ngay trong 1 beat kế tiếp — A05 spec + Codex implement |
| F3 | **P2** | Field action không rủi ro/không chi phí: đứng yên bấm xác nhận, tách đội không bị trừng phạt, puzzle ấn sai thứ tự không mất gì | `RescueFieldActions.kt` (chỉ check bán kính 140u), CSV: nhiều FIELD_ACTION tức thì sau MOVE_TO | Thêm áp lực thời gian **theo trạng thái** (địch tuần tra tiến gần, courier bị đe khi vắng đội), không thêm timer chờ — A06 thiết kế hành vi, Codex duyệt core |
| F4 | **P2** | Địch quá yếu so với party khởi đầu: 1–2 con/chapter, courier 300/300 HP suốt trace; không tình huống nào buộc PROTECT/REGROUP cứu nguy thật | CSV cột `courier_hp`; `RescueJourneyContent.chapters` | Tăng **chất** tình huống (2 kiểu hành vi phối hợp: kiter giữ khoảng cách + flanker vòng sau) thay vì tăng HP/số lượng thô — A06 + Codex |
| F5 | **P2** | Lặp không gian debrief→escort đi lại đường cũ; phần map mở rộng 6200×3600 chưa có số đo hiệu năng thiết bị | `routePoints` kết thúc (600,1049) = điểm đầu; `buildRescueJourneyMap` thêm tối đa 155 prop; PR tự ghi NOT TESTED | Đặc tả đề xuất extraction một chiều không quay đầu (§ spec); đo culling/bitmap trước khi giữ phần mở rộng — A10 + A03 |
| F6 | **P1 quy trình** | PR CONFLICTING với main, không CI trên head; mọi số PASS là cloud-worker local | `gh pr view/checks` §0 | Chủ PR rebase/merge main vào nhánh + chạy CI exact-SHA trước khi xin review merge — chủ PR #28/Codex, A01 điều phối |
| F7 | **P2 ghi nhận** | Lỗi đã tự khai trong MISSION01B: master auto-bolt từ sigil và sigil kích hoạt discovery ở tầng không có courier (Floor 1 có courier thì bolt đã tắt: `TowerCombatEngine.kt:530` chỉ chạy khi `lostCourier == null`) | `TowerCombatEngine.kt:528-546, 380-388`; MISSION01B_REPORT_VI | Ngoài phạm vi Floor1-rescue nhưng phải chặn trước khi tuyên bố "Master off-field toàn Tower" — Codex/A06, ticket riêng |

## 5. Verdict (theo cổng REVIEW_GATES mục 5)

**REQUEST_CHANGES (về mặt thiết kế gameplay, cho mục đích merge-ready):** cấu trúc outcome/save/tactical hiện tại tốt và phải giữ, nhưng nhịp chơi hiện tại là "đi bộ có trạm xác nhận", chưa phải 5–10 phút chơi thật đúng nghĩa. Không đề xuất thêm timer/HP sponge; đề xuất rework theo đặc tả `FLOOR1_MEADOW_RESCUE_SPEC.md` (cùng thư mục): rút đường, tăng mật độ quyết định, 2 kiểu địch phối hợp, extraction một chiều, tiêu chí đo bằng mật độ sự kiện thay vì tổng giây.

**Các mục NOT TESTED trong review này:** human playtime; cảm nhận người chơi thật về choice; FPS/GPU/nhiệt thiết bị; CI remote trên head PR #28; toàn bộ screenshot trong PR là Robolectric native (đã được PR ghi đúng nhãn), A05 chưa tự render lại.

**Bước tiếp theo đề xuất cho owner/A01:** (1) yêu cầu chủ PR #28 giải quyết conflict + CI trước; (2) dùng spec của A05 làm cơ sở lập ticket rework pacing (Codex giữ core combat/save, A06 hành vi địch, A07 thoại/hậu quả truyện, A05 dữ liệu mission); (3) chỉ nghiệm thu "5–10 phút" khi có playtest người thật đo bằng đồng hồ, phân biệt với simulation.
