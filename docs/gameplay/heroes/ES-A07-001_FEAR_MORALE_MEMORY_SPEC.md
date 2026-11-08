# ES-A07-001 — SPEC: Sợ hãi / Sĩ khí / Ký ức anh hùng (Floor 1 Rescue)

**Ticket:** [Issue #58 — ES-A07-001](https://github.com/lightingfff/eternal-sunmmoner/issues/58)
**Role:** A07 — Hero Identity & Narrative Designer
**Trạng thái tài liệu:** `SPEC ONLY` — **chưa nối logic runtime, chưa có dòng Kotlin nào bị sửa**
**Baseline SHA:** `bdff6f9976dc4927e1e095a503bc339509c7a9aa` (`main`, 2026-11-08)
**Branch / PR:** `arena/00b39daa-eternal-sunmmoner` → PR mở cho owner duyệt
**Reviewer đề xuất:** A05 (Tower Level/Quest), Codex (khi chạm save/event — tài liệu này **không** chạm), A09 (QA), Argon (game feel/tone)

---

## 0. Phạm vi và giới hạn

### 0.1 Tài liệu này LÀM gì
- Chốt **danh mục nội dung gốc** (original) cho ba anh hùng khởi đầu trong tình huống **sợ hãi** ở mission cứu người đưa thư Tầng 1.
- Chốt **ngữ nghĩa thiết kế** của ba trục: Fear (sợ hãi), Morale (sĩ khí), Memory (ký ức/khế ước), **chỉ dùng các con số và cơ chế đã có trong `main`**.
- Đưa ra 3 **hero responses** có mã nội dung ổn định để A05/A11/Argon tham chiếu, kèm điều kiện kích hoạt đo được trên state hiện có.
- Chỉ rõ **ranh giới tích hợp** cho ticket tương lai (ai được sửa file nào).

### 0.2 Tài liệu này KHÔNG làm gì
- Không sửa `LocalGuestSaveManager.kt`, `SummonerRepository.kt`, `SummonerViewModel.kt`, `AutonomousHeroAi.kt`, `ContactAttackTimeline.kt`, `LostCourierEncounter.kt`, `TowerBattleScreen.kt`, `ManhwaStoryData.kt`.
- Không thêm/sửa enum, field save, event ID, idempotency, camera, layout, renderer.
- Không khẳng định bất kỳ câu thoại nào **đã chạy trong game**. Toàn bộ là spec chờ ticket triển khai.
- Không đổi cân bằng combat. Nếu nội dung đề xuất cần đổi số (v.d. thời lượng sợ), việc đó thuộc **A11 + Codex**, không thuộc A07.

---

## 1. Baseline audit (đọc code thật trên `main`, không suy đoán)

| Cơ chế | Bằng chứng trong repo (baseline `bdff6f9`) | Số liệu/ngữ nghĩa **thật** | Hệ quả cho thiết kế A07 |
|---|---|---|---|
| Sợ hãi (fear) | `app/src/main/java/com/example/game/combat/CombatEntities.kt:40-42` | `fearSecondsRemaining`; khi > 0: sát thương ×0.8, tốc độ ×0.85 | Không được hứa hẹn hiệu ứng khác (né, bỏ chạy, mất lệnh) |
| Gán sợ | `LostCourierEncounter.kt:56-57, 116-127` | Khi trinh sát xuất hiện (`courier.x >= 1320`), **đúng một** anh hùng "non" bị sợ **10s**. `noviceId` = hero có `level <= 3` hoặc `morale < 50`, ưu tiên level thấp nhất, hòa thì Kaelen | Câu thoại sợ hãi phải đọc được **cho cả 3 hero**, không chỉ Kaelen |
| Cách Master giảm sợ | `LostCourierEncounter.kt:86-91` | Tốc độ giảm sợ: `PROTECT_ALLY` ×2, `REGROUP` ×2.5, mọi lệnh khác ×1 | Đây là **cơ chế dạy người chơi** tốt nhất; thoại nên chỉ thẳng vào nó |
| Nhánh truy kích | `TowerCombatEngine.kt:256-276` | `CourierOrder.INTERCEPT` → đặt `TacticalOrder.ATTACK_TARGET`; **mất** bọc lót 40% cho người đưa thư | Xung đột thật: cứu người đưa thư vs chặn tù và. Thoại phải nói rõ giá của lựa chọn |
| Nhãn HUD sợ | `TowerCombatEngine.kt:568`; `TowerBattleScreen.kt:892-893` | `"Lo sợ • -20% sát thương / -15% tốc độ"`, `"<Tên>: Sợ Ns (-20% lực đánh/-15% tốc độ)"` | Thoại của A07 **không được lặp** chuỗi này; HUD đã lo phần số |
| Sĩ khí khi kết toán | `LocalGuestSaveManager.kt:1119, 1168-1173` (và bản Firestore `SummonerRepository.kt:1114-1116`) | Thắng −6 (sàn `max(20, moraleFloor)`), rút lui −10 (sàn `max(25, moraleFloor)`), thua −22 (sàn `moraleFloor`); `moraleFloor` = 15, hoặc 30 nếu mở East Garden | Kết cục A/B **không tự đặt số mới**; mô tả cảm xúc phải khớp mức phạt có sẵn |
| Ngưỡng hiển thị sĩ khí | `HeroProfileModal.kt:598-603` | `morale >= 85` → Emerald; ngược lại Amber | Band nội dung: ≥85 "vững", 50–84 "chao đảo", <50 "rạn" (khớp `noviceId`) |
| Ký ức đồng đội | `LostCourierProgression.kt` | `FLAG_RESCUED="COURIER_RESCUED"`, `FLAG_FAILED="COURIER_FAILED"`, `RESCUE_MEMORY` (58 ký tự), `rememberRescue()` **cắt cứng 200 ký tự**, chỉ ghi khi `isFirstRescue` | Ngân sách ký ức: thêm ≤ **141** ký tự khi nối vào `bondSummary` có sẵn |
| Ai nhận ký ức | `LocalGuestSaveManager.kt:1144-1147`; `TowerBattleScreen.kt:1333` | `courierMemoryHeroId` (UI truyền `lostCourier.frightenedHeroId`) nếu thuộc đội, nếu không thì hero đầu tiên của đội | **Người từng sợ là người giữ ký ức** — trục thiết kế trung tâm của tài liệu này |
| Kết cục | `data/model/CourierOutcome.kt` | Chỉ `RESCUED` / `FAILED` | Không có kết cục thứ ba; không thêm enum |
| Hiển thị kết cục | `TowerBattleScreen.kt:1369-1373` | 1 dòng text cho RESCUED ("lò rèn 90 vàng, ký ức đã lưu") và FAILED ("có thể trở lại Tầng 1") | Nội dung mới phải **bổ sung**, không mâu thuẫn câu đang có |

**Kết luận audit:** mission cứu người đưa thư Tầng 1 **đang tồn tại và có thể chạm tới** trên `main` (`TowerBattleScreen.kt:219` bật `enableLostCourier` cho Tầng 1; `TowerCombatEngine.kt:121-130` khởi tạo encounter và đặt mặc định `PROTECT_ALLY`). Vì vậy A07 viết nội dung cho **hệ thống thật**, nhưng **không sửa** nó.

---

## 2. Ba anh hùng khởi đầu — giọng điệu khóa cứng

Dữ liệu lấy từ `LocalGuestSaveManager.kt:260-310` (guest offline) và `FirestoreModels.kt:71-113` (`PersonalityType`), thoại tham chiếu từ `ManhwaStoryData.kt`.

| Mã | Hero | Chức danh (canonical) | `personality` | Câu mẫu tính cách (đã có trong repo) | Giọng A07 được phép viết |
|---|---|---|---|---|---|
| H-K | Kaelen | Khiên Thép Tiền Phong | `STOIC` | "Chỉ cần Master ra lệnh, tấm khiên này sẽ không lùi nửa bước." | Câu ngắn, thì hiện tại, nhận lỗi về mình, không kêu ca. Sợ = **thân thể**, không phải ý chí |
| H-L | Lyra | Mắt Ưng Gió Đêm | `CYNICAL` | "Trong tòa tháp này, kẻ do dự một giây sẽ hóa thành tro bụi." | Khô, mỉa nhẹ, nói bằng chi phí/phương án. Không an ủi sáo rỗng |
| H-V | Vesper | Học Giả Tinh Vân | `CALCULATING` | "Quái vật tầng trên không chỉ mạnh hơn... chúng biết phối hợp phục kích đấy." | Nói bằng dữ kiện, xác suất, ghi chép. Cảm xúc được **đặt tên**, không được thể hiện |

**Quy tắc chung:** xưng "tôi" với Master; không dùng từ hiện đại (game, buff, lag, OK); không nhắc tới UI; mỗi câu ≤ 130 ký tự để vừa một khối thoại trên màn 800×360; tiếng Việt có dấu, không emoji trong lời thoại (emoji chỉ dùng ở tầng HUD do A04/A11 quyết).

---

## 3. Trục 1 — FEAR: sợ hãi phải *đọc được*, không phải *bị trừng phạt*

### 3.1 Ngữ nghĩa thiết kế (dùng đúng cơ chế hiện có)
1. Sợ chỉ xảy ra với **một** người, ở **một** thời điểm (tiếng tù và trinh sát). Đây là **khoảnh khắc nhân vật**, không phải debuff rải đều.
2. Sợ **giảm nhanh hơn** khi Master chọn `PROTECT_ALLY` (×2) hoặc `REGROUP` (×2.5). Vì vậy thoại phải nói rằng **có người đứng chắn** thì mới hết sợ — đúng toán học của code.
3. Truy kích (`INTERCEPT` → `ATTACK_TARGET`) vừa **mất bọc lót 40%** cho người đưa thư, vừa khiến sợ **chỉ giảm ×1**. Thiết kế nội dung phải để người chơi *cảm* thấy giá đó, không cần thêm số.
4. Không hero nào được "hết sợ ngay lập tức vì lời nói". Chỉ thời gian + lệnh đúng.

### 3.2 Band sĩ khí và cách nội dung phản ứng

| Band | Điều kiện (khớp code) | Ý nghĩa kể chuyện | Được phép dùng |
|---|---|---|---|
| Vững | `morale >= 85` | Tin nhau, trêu nhau được | Câu ngắn, tự tin, có thể đùa khô |
| Chao đảo | `50 <= morale < 85` | Còn kỷ luật, bớt nói | Câu ngắn hơn, bỏ hẳn đùa |
| Rạn | `morale < 50` | Đã lọt vào điều kiện `noviceId` — **dễ bị sợ** | Chỉ nói sự thật; không hứa hẹn |

Bảng này chỉ mô tả **cách viết**, không đề xuất đổi công thức sĩ khí.

---

## 4. Trục 2 — MORALE: kết cục nặng/khẹ nhẹ phải khớp con số

| Kết quả expedition | Delta sĩ khí **đang có** | Cách nội dung được phép diễn đạt |
|---|---|---|
| Thắng (kể cả cứu được người đưa thư) | −6 | "Mệt, nhưng đứng thẳng" — thoại ngắn, không ăn mừng lớn |
| Rút lui chiến thuật | −10 | "Còn sống, còn nợ" — thoại nhìn lại, không oán trách Master |
| Thua / hộ tống thất bại | −22 | "Mất người" — thoại nặng, cấm hài, cấm đổ lỗi cho người đưa thư |

**Cấm:** viết thoại "sĩ khí tăng vọt" sau khi cứu người đưa thư, vì code **trừ 6** chứ không cộng. Nếu owner muốn thưởng sĩ khí cho lần cứu đầu, đó là **thay đổi cân bằng của A11/Codex**, phải có ticket riêng; A07 ghi nhận là **đề xuất mở**, không mặc định.

---

## 5. Trục 3 — MEMORY: người đã sợ là người giữ ký ức

### 5.1 Hành vi **thật** hiện tại
- Chỉ ghi **một lần** (`isFirstRescue`), chỉ khi `RESCUED`.
- Ghi vào `HeroRecord.bondSummary` của **đúng một** hero: `courierMemoryHeroId` nếu có trong đội, ngược lại hero đầu tiên của đội.
- UI đang truyền `frightenedHeroId` → nghĩa là **anh hùng bị sợ ở tiếng tù và** chính là người nhận ký ức (nếu còn trong đội).
- Chuỗi ký ức hiện tại là **một hằng số chung cho mọi hero**: `"Đã vượt sợ hãi, đưa người đưa thư về cổng nhờ lệnh Master."` (58 ký tự).
- Toàn bộ `bondSummary` bị cắt ở **200 ký tự**; hiển thị ở `HeroProfileModal.kt:579-584` dưới nhãn "Khế ước với Master".

### 5.2 Đề xuất nội dung (PROPOSAL — **chưa triển khai**)
Thay hằng số chung bằng **ba biến thể theo hero**, chọn theo `HeroRecord.personality` hoặc `id` của memory hero:

| Mã | Hero | Chuỗi ký ức đề xuất | Ký tự | Tổng sau khi nối vào bond hiện tại |
|---|---|---|---|---|
| M-K | Kaelen | Lần đầu giữ được một mạng ở Tầng 1: tay run, khiên không run. | 61 | 49 + 1 + 61 = **111** / 200 |
| M-L | Lyra | Đã hạ trinh sát trước tiếng tù và. Nghe lệnh đúng lúc quan trọng hơn bắn giỏi. | 78 | 67 + 1 + 78 = **146** / 200 |
| M-V | Vesper | Mười giây sợ hãi, một người sống. Khế Ước ghi: sợ hãi không phải thất bại. | 74 | 52 + 1 + 74 = **127** / 200 |

**Ràng buộc kỹ thuật cho ticket triển khai (A11/Codex, không phải A07):**
- Giữ nguyên chữ ký `LostCourierProgression.rememberRescue(bond: String): String` và hành vi idempotent (`hasRescueMemory`) — nếu đổi sang 3 chuỗi thì phải kiểm tra **cả ba** biến thể khi phát hiện đã có ký ức, nếu không sẽ ghi trùng khi lần cứu đầu thất bại rồi thắng lại.
- Không thêm field save. Không đổi 200 ký tự.
- Nếu owner muốn giữ chuỗi chung hiện tại: tài liệu này vẫn dùng được, chỉ bỏ mục 5.2.

### 5.3 Ký ức **không** được ghi khi thất bại
`COURIER_FAILED` chỉ là flag. Không viết "ký ức dũng cảm" cho thất bại — sẽ mâu thuẫn với settlement UI ("Người đưa tin chưa tới cổng"). Xem kết cục B ở tài liệu story.

---

## 6. BA HERO RESPONSES (mã ổn định, spec chưa nối logic)

Điều kiện kích hoạt được viết trên **state đã tồn tại**, để ticket triển khai không phải thêm khái niệm mới.

### HR-F1-01 — Kaelen: "tay run, khiên không run"

| Trường | Giá trị |
|---|---|
| Hero | Kaelen (`STOIC`) |
| Kích hoạt (đề xuất) | `LostCourierEncounter.phase == SCOUT_ALARM` **và** hero bị sợ (`fearSecondsRemaining > 0`) **và** `engine.activeOrder == PROTECT_ALLY` |
| Vì sao trigger này | Đây đúng là tổ hợp mà code thưởng: bọc lót giảm sợ ×2. Thoại dạy người chơi **giữ lệnh** thay vì hoảng |
| `emotionTag` (khớp `DialogueBeat`) | `[Cảnh giác]` |
| `heroClassForPortrait` | `HeroClassType.KNIGHT` |
| Thời lượng hiển thị | 3.0s hoặc tới khi người chơi đổi lệnh (cái nào tới trước) |
| Lời thoại | Tay tôi đang run — cứ để nó run. Khiên thì không. Master, giữ lệnh Bảo Vệ, tôi sẽ đứng trước người đưa thư. |
| Ký tự | 107 |
| Bề mặt hiển thị đề xuất | Khối thoại ngắn cạnh `CourierMissionHud` (A04 quyết layout) — **không** che thanh HP người đưa thư |
| Điều cấm | Không ngụ ý Kaelen bất tử; không ngụ ý đổi lệnh là sai |

### HR-F1-02 — Lyra: "trước tiếng tù và thứ ba"

| Trường | Giá trị |
|---|---|
| Hero | Lyra (`CYNICAL`) |
| Kích hoạt (đề xuất) | `phase == SCOUT_ALARM` **và** `scoutWarningRemaining > 0` **và** `scoutEnemyId != null` **và** người chơi vừa chọn `CourierOrder.INTERCEPT` (`mission.order == INTERCEPT`) |
| Vì sao trigger này | Truy kích là **lựa chọn có giá**: mất bọc lót 40%, sợ giảm ×1. Lyra là người nói ra cái giá, không phải người phán xét |
| `emotionTag` | `[Sắc bén]` |
| `heroClassForPortrait` | `HeroClassType.ARCHER` |
| Thời lượng | 2.5s hoặc tới khi trinh sát chết / tù và vang |
| Lời thoại | Hắn chạy nhanh hơn tôi tưởng. Bắn hạ trước tiếng tù và thứ ba — hoặc cả cánh đồng này sẽ biết chúng ta ở đây. |
| Ký tự | 109 |
| Ràng buộc số | "tiếng tù và thứ ba" phải **khớp** cửa sổ cảnh báo hiện có (`scoutWarningRemaining = 9f` tại `LostCourierEncounter.kt:123`, tức 3s cảnh báo + 6s gọi viện). Nếu A11 đổi cửa sổ, câu này **phải** được sửa — ghi rõ trong ticket |
| Điều cấm | Không mô tả Lyra bỏ vị trí khi chưa có lệnh; không hứa "chắc chắn chặn được" |

### HR-F1-03 — Vesper: "ký ức thuộc về người đã sợ"

| Trường | Giá trị |
|---|---|
| Hero | Vesper (`CALCULATING`) |
| Kích hoạt (đề xuất) | Cạnh chuyển trạng thái của hero bị sợ: `fearSecondsRemaining` từ `> 0` xuống `== 0f`, **và** `phase` là `ESCORT`/`SCOUT_ALARM`, **và** mission chưa kết thúc |
| Vì sao trigger này | Đây là mốc duy nhất mà cơ chế hiện có **thực sự** kết thúc sợ; thoại neo vào đúng mốc đó thay vì bịa "hết sợ vì lời nói" |
| `emotionTag` | `[Phân tích]` |
| `heroClassForPortrait` | `HeroClassType.MAGE` |
| Thời lượng | 3.0s, chỉ **một lần** mỗi expedition |
| Lời thoại | Run tay kết thúc sau mười giây. Không phải vì can đảm — vì có người đứng chắn. Ghi vào Khế Ước: ký ức này thuộc về người đã sợ. |
| Ký tự | 127 |
| Ràng buộc số | "mười giây" phải khớp `fearSecondsRemaining = 10f` (`LostCourierEncounter.kt:122`). Nếu đổi, sửa câu |
| Điều cấm | Không nói Vesper đã ghi vào save (ghi thật chỉ xảy ra khi `RESCUED` + lần cứu đầu) |

### 6.1 Ma trận phủ (để A05 kiểm tra thiết kế có "dạy" hay không)

| Người chơi làm gì | Họ nghe gì | Họ học được gì |
|---|---|---|
| Giữ `PROTECT_ALLY` khi có tù và | HR-F1-01 | Bọc lót vừa giảm sợ ×2 vừa giảm 40% sát thương vào người đưa thư |
| Đổi sang `INTERCEPT` để dí trinh sát | HR-F1-02 | Truy kích mua thời gian nhưng bán bọc lót |
| Để sợ tự hết (hoặc `REGROUP`) | HR-F1-03 | Sợ là trạng thái có thể đi qua; ký ức sẽ thuộc về người yếu nhất |

---

## 7. Hai kết cục — tóm tắt ngữ nghĩa (chi tiết beat sheet ở tài liệu story)

| Mã | `CourierOutcome` | Tên nội dung | Cảm giác mục tiêu | Ký ức/flag | Sĩ khí (code) |
|---|---|---|---|---|---|
| NO-F1-A | `RESCUED` | **Cổng Sáng Trở Lại** | "Mình vừa giữ được một mạng người bằng mệnh lệnh" | `COURIER_RESCUED` + memory vào hero đã sợ (lần đầu) | −6 |
| NO-F1-B | `FAILED` | **Tiếng Tù Và Không Đáp** | "Lựa chọn của mình có giá, và cái giá đó có tên" | `COURIER_FAILED`, **không** memory | −22 |

Cả hai đều **không** tạo kết cục thứ ba và **không** đụng `synthesisGoldCost` (90/120 đã có trong `LostCourierProgression.kt`).

---

## 8. Hợp đồng tích hợp đề xuất (cho ticket TƯƠNG LAI — không phải PR này)

> Trạng thái: `NOT IMPLEMENTED`. PR này chỉ có 2 file markdown. Mọi mục dưới đây là đề xuất để A01/A11 mở ticket riêng.

**Đường dữ liệu đã có sẵn (không cần migration):**
- `HeroRecord.personalityType` (`STOIC` / `CYNICAL` / `CALCULATING`) → chọn biến thể thoại/ký ức.
- `LostCourierEncounter.phase` (`CLEAR_PATH`…`FAILED`), `scoutWarningRemaining`, `scoutEnemyId`, `frightenedHeroId`, `fearSecondsRemaining`, `outcome`, `mission.order`.
- `TacticalOrder` đang có (`PROTECT_ALLY`, `REGROUP`, `ATTACK_TARGET`…).
- `DialogueBeat` (`game/story/ManhwaStoryData.kt`) đã đủ trường cho 3 response (speakerName, speakerTitle, emotionTag, heroClassForPortrait, lineVi, panelArtRes).

**Đề xuất tối thiểu, một writer duy nhất:**
- Thêm object thuần dữ liệu `game/story/CourierStoryBeats.kt` với các hàm thuần (không I/O, không save): `fearResponse(personality, order)`, `outcomeBeats(outcome, personality)`.
- Chỉ `TowerBattleScreen`/`CourierMissionHud` hiển thị; **không** thêm state vào save.
- Test gợi ý: hàm thuần trả đúng mã cho ma trận 3 personality × 3 tình huống; không phụ thuộc thời gian thực.
- **Người viết:** A11 hoặc A01 theo `[LEAD-ASSIGN]`; **review bắt buộc:** Codex (vì chạm event/kết toán) + A05 (nhịp độ) + A09 (test).

**Điều A07 từ chối làm trong PR này:** sửa `LostCourierEncounter.kt`, `TowerBattleScreen.kt`, `ManhwaStoryData.kt`, `LocalGuestSaveManager.kt` dù chỉ 1 dòng — vì các file đó đang thuộc PR #47/#50/#28 hoặc là critical save/combat.

---

## 9. Tự kiểm (self-check, đã chạy thật)

| Tiêu chí | Cách kiểm | Kết quả |
|---|---|---|
| Đúng 3 hero responses | Đếm mục HR-F1-01..03 | 3 ✔ |
| Đúng 2 kết cục, khớp enum thật | Đối chiếu `CourierOutcome.kt` | 2 ✔ (`RESCUED`, `FAILED`) |
| Không thêm enum/field save | `git diff --stat` chỉ 2 file `.md` | ✔ (xem PR) |
| Mọi câu thoại ≤ 130 ký tự | Đo bằng script (mục 10) | max 127 ✔ |
| Ký ức nối vào `bondSummary` ≤ 200 | 49/67/52 + 1 + 61/78/74 | 111/146/127 ✔ |
| Không chép nguyên văn manhwa | Nội dung tự viết; kiểm trùng chuỗi với source (mục 10) | 0 trùng ✔ |
| Số liệu fear/morale khớp code | Đọc trực tiếp file:line ở mục 1 | ✔ |
| Đã chạy trong game | — | **KHÔNG** — spec, không có code |

## 10. Validation log (lệnh thật, chạy trong phiên)

```
$ python3 /tmp/a07_check.py   # script tạm trong phiên, KHÔNG commit vào repo
lines checked: 11 (3 hero responses + 8 outcome beats)
per-line: HR-F1-01=107, HR-F1-02=109, HR-F1-03=127, NO-A1=67, NO-A2=94,
          NO-A3=91, NO-A4=95, NO-B1=52, NO-B2=81, NO-B3=77, NO-B4=72
max chars: 127 / limit 130 -> PASS
unique lines: 11/11 -> PASS
all 11 lines present verbatim in committed docs -> PASS
bondSummary combined: M-K=49+1+61=111, M-L=67+1+78=146, M-V=52+1+74=127 / cap 200 -> PASS
verbatim >=40-char overlap with app/src/main/java: 0 -> PASS
files changed: only 2 markdown files -> PASS
RESULT: PASS
```

Script kiểm tra là công cụ tạm trong phiên (không nằm trong PR) để không mở rộng phạm vi file được giao. Người review có thể chạy lại bằng bất kỳ script đếm ký tự nào.

## 11. NOT TESTED / giới hạn trung thực
- **NOT TESTED:** render thật của thoại trong HUD Android, thời lượng đọc thực tế, va chạm bố cục với `CourierMissionHud` ở 800×360 và portrait.
- **NOT TESTED:** cảm nhận người chơi thật (không có playtest 5–10 phút trong phiên này).
- **Không đo FPS/nhiệt:** tài liệu không đổi runtime nên phép đo là vô nghĩa ở bước này; sẽ cần khi ticket triển khai thêm hiển thị.
- **Chưa xác nhận** với A05 rằng "tiếng tù và thứ ba" khớp nhịp encounter; đây là **câu hỏi mở** (mục 12).

## 12. Câu hỏi mở cho reviewer
1. **A05:** Cửa sổ cảnh báo 3s + 6s gọi viện có phải nhịp chính thức của Floor 1? Nếu đổi, HR-F1-02 phải sửa theo.
2. **Codex:** Việc chọn ký ức theo `personality` có phá idempotency hiện tại (`hasRescueMemory` chỉ nhận một chuỗi) không? A07 đề xuất giữ 1 chuỗi nếu Codex thấy rủi ro.
3. **Argon:** Giọng ba hero có đủ khác biệt khi đọc trên màn nhỏ, hay cần rút ngắn thêm?
4. **Owner:** Có muốn thưởng sĩ khí cho lần cứu đầu tiên không? (Hiện code **trừ 6**; thay đổi này thuộc A11/Codex, không thuộc phạm vi A07.)

## 13. Tuyên bố nội dung gốc
Toàn bộ thoại, tên "Bách" (người đưa thư tuyến Đông) và các tên kết cục trong tài liệu này do A07 viết cho ETERNAL SUMMONER. Không sao chép nguyên văn từ manhwa/truyện/webtoon nào; không dùng tên nhân vật có bản quyền bên ngoài repo. Nội dung bám dark-fantasy hiện có của dự án (xem `DARK_FANTASY_TRANSFORMATION.md`) và không thêm yếu tố tôn giáo/thực tế nhạy cảm.
