# ES-A07-001 — STORY: Floor 1 Rescue — Hai kết cục & Phản ứng anh hùng

**Ticket:** [Issue #58 — ES-A07-001](https://github.com/lightingfff/eternal-sunmmoner/issues/58)
**Role:** A07 — Hero Identity & Narrative Designer
**Trạng thái:** `SPEC ONLY` — **chưa nối logic, chưa đổi một dòng Kotlin nào**
**Baseline SHA:** `bdff6f9976dc4927e1e095a503bc339509c7a9aa`
**Tài liệu đồng bộ:** [`docs/gameplay/heroes/ES-A07-001_FEAR_MORALE_MEMORY_SPEC.md`](../heroes/ES-A07-001_FEAR_MORALE_MEMORY_SPEC.md)
**Reviewer:** A05 (+ Codex nếu chạm save/event — tài liệu này không chạm), A09, Argon

---

## 1. Mission này là mission có thật trong repo

Không phải ý tưởng trừu tượng. Trên `main` tại baseline:

| Thành phần | Vị trí | Hành vi thật |
|---|---|---|
| Encounter | `app/src/main/java/com/example/game/combat/LostCourierEncounter.kt` | Một encounter có tác giả trên con đường sẵn có; không thêm navigation mới |
| Phase | `enum class CourierPhase { CLEAR_PATH, ESCORT, SCOUT_ALARM, GATE_GUARD, RESCUED, FAILED }` | 4 phase chơi + 2 phase kết thúc |
| Kích hoạt | `TowerCombatEngine.kt:121` (`enableLostCourier && floorNumber == 1`) và `TowerBattleScreen.kt:219` | Tầng 1 luôn bật |
| Kết cục | `data/model/CourierOutcome.kt` | `RESCUED` / `FAILED` |
| Kết toán | `LocalGuestSaveManager.kt:1124-1126` | Chỉ `RESCUED` khi `floorNumber == 1 && victory`, ngược lại là `FAILED` |
| HUD | `ui/components/CourierMissionHud.kt` (gọi ở `TowerBattleScreen.kt:897-903`) | 2 nút Bảo Vệ / Chặn Trinh Sát |
| Dòng kết cục | `TowerBattleScreen.kt:1369-1373` | 1 câu cho mỗi kết cục |

Vì vậy tài liệu này viết cho **ui/flow đang tồn tại**, và giữ đúng luật: không kết cục thứ ba, không phase mới, không lưu mới.

---

## 2. Nhân vật

### 2.1 Ba anh hùng (canonical, không đổi tên/chức danh)
Lấy từ `LocalGuestSaveManager.kt:262-326` và `ManhwaStoryData.kt`:

- **Kaelen** — *Khiên Thép Tiền Phong* — Knight, `STOIC`.
- **Lyra** — *Mắt Ưng Gió Đêm* — Archer, `CYNICAL`.
- **Vesper** — *Học Giả Tinh Vân* — Mage, `CALCULATING`.

### 2.2 Nhân vật mới do A07 viết: người đưa thư **Bách** (original)
- Danh xưng khi hiển thị: `Bách` — `Người Đưa Thư Tuyến Đông` (`speakerTitle`), `heroClassForPortrait = null`, `panelArtRes = null` (chưa có art; **không** giả định PNG nào tồn tại).
- Vai trò: người sống sót cuối cùng của tuyến tiếp vận phía Đông, bị thương chân, đang bò về cổng. Không phải anh hùng, không vào đội, không có chỉ số chiến đấu mới.
- Đặc điểm viết: nói câu ngắn vì thiếu máu; gọi Master là "ngài", gọi anh hùng là "các ngươi"; **không** biết gì về hệ thống tầng/tháp → giữ tính "người thường" của thế giới.
- Lý do cần tên: hiện tại kết cục chỉ là câu hệ thống. Một cái tên biến "nhiệm vụ hộ tống" thành "một mạng người", đúng trọng tâm vai trò A07 — mà **không cần** đổi save, đổi enum hay đổi HUD.

### 2.3 Finding continuity (báo cáo, **không sửa**)
`ManhwaStoryData.kt` dùng **hai** chức danh cho Kaelen: `"Khiên Thép Tiền Phong"` (dòng 31) và `"Khiên Thép Tiền Tuyến"` (dòng 59, 86). Hai chỗ còn lại trong save/repository dùng `"Tiền Phong"`. A07 **không** sửa vì đây là file runtime/story thuộc người khác; đề nghị A05/A01 mở ticket chuẩn hóa. Tài liệu này dùng `"Khiên Thép Tiền Phong"`.

---

## 3. Bản đồ nhịp: phase → hành động → thoại

| Bước | `CourierPhase` | Người chơi làm gì (thật) | Nội dung A07 xuất hiện |
|---|---|---|---|
| 1 | `CLEAR_PATH` | Dọn 2 lính gác mở đường | Không thoại (để HUD hiện có dẫn dắt) |
| 2 | `ESCORT` | Đi trong 300px quanh người đưa thư để nó tiến | Không thoại |
| 3 | `SCOUT_ALARM` (`courier.x >= 1320`) | Một hero bị sợ 10s; chọn Bảo Vệ hay Chặn Trinh Sát | **HR-F1-01 / HR-F1-02** (spec heroes) + **HR-F1-03** khi sợ kết thúc |
| 4 | `GATE_GUARD` (`courier.x >= 2220`) | Né vòng đỏ 1.5s của Kraag, dồn hỏa lực khi hắn hồi sức | Không thoại mới (HUD đã dạy) — tránh nghẽn chữ lúc combat |
| 5A | `RESCUED` | Thắng + hộ tống tới cổng | **NO-F1-A**, 4 beat |
| 5B | `FAILED` | Người đưa thư gục, hoặc cả đội gục | **NO-F1-B**, 4 beat |

Nguyên tắc nhịp: **không** đặt thoại dài trong `GATE_GUARD` (người chơi đang né đòn), và **không** lặp lại lời giải thích cơ chế đã có trong `statusVi`.

---

## 4. KẾT CỤC A — `RESCUED` / **"Cổng Sáng Trở Lại"**

**Cảm giác mục tiêu:** người chơi vừa *mua* một mạng người bằng mệnh lệnh, không phải bằng may mắn. Không ăn mừng lớn — vì code trừ 6 sĩ khí, đội mệt thật.

**Điều kiện hiển thị (đề xuất):** `mission.outcome == CourierOutcome.RESCUED`; mỗi beat một lần/expedition; tổng ≤ 9s nếu người chơi bấm "Tiếp tục" nhanh.

| Mã | Thứ tự | Speaker | `emotionTag` | Lời thoại | Ký tự | Vì sao câu này |
|---|---|---|---|---|---|---|
| NO-A1 | 1 | Kaelen | `[Điềm tĩnh]` | Cổng đã khuất sau lưng nó. Nó còn thở. Chúng ta còn đủ người để về. | 67 | Kaelen báo cáo tình trạng, không cảm xúc — đúng `STOIC` |
| NO-A2 | 2 | Lyra | `[Thực dụng]` | Đừng nhìn tôi như thế. Tôi chỉ bắn đúng một mũi tên — người quyết định giữ đội hình là Master. | 94 | Chuyển công lao về Master, đúng chất `CYNICAL` không nhận credit |
| NO-A3 | 3 | Vesper | `[Phân tích]` | Một người sống sót ở Tầng 1. Xác suất đó thấp hơn tôi tính. Có lẽ vì lệnh của ngài, Master. | 91 | `CALCULATING` thừa nhận mô hình của mình sai — cách khen Master gián tiếp |
| NO-A4 | 4 | Bách | `[Kiệt sức]` | Ta tên Bách, người đưa thư cuối cùng của tuyến Đông. Ta tưởng tuyến này đã bỏ hoang từ lâu rồi. | 95 | Trao tên + mở rộng thế giới mà không cần cutscene mới |

**Hệ quả thật sau kết cục A (đang có trong code, A07 chỉ mô tả):**
- `COURIER_RESCUED` được ghi; lần cứu đầu mở giá rèn 90 (so với 120) — `LostCourierProgression.synthesisGoldCost`.
- Ký ức ghi vào hero **đã từng sợ** (`frightenedHeroId` → `courierMemoryHeroId`), chỉ lần đầu.
- Sĩ khí −6 cho toàn đội (`LocalGuestSaveManager.kt:1170`).
- Settlement UI hiện câu: *"Đã cứu người đưa tin. Dịch vụ lò rèn: phí chế tạo còn 90 vàng. Ký ức đồng đội đã lưu."*

**Đề xuất bổ sung (cần ticket của A04/A11, KHÔNG thuộc PR này):** thêm **một** dòng ngắn dưới câu trên, ví dụ theo hero giữ ký ức:
> `«Kaelen sẽ nhớ lần này lâu hơn một trận thắng.»`

Chỉ một dòng; không thêm panel, không thêm animation.

**Điều cấm ở kết cục A:** không nói "sĩ khí tăng", không nói "phần thưởng lớn" (thực tế là −6 và loot theo tầng), không hứa dịch vụ nào chưa có trong code.

---

## 5. KẾT CỤC B — `FAILED` / **"Tiếng Tù Và Không Đáp"**

**Cảm giác mục tiêu:** thất bại có tên, có giá, và **có đường thử lại**. Tuyệt đối không mỉa người chơi, không đổ lỗi cho người đưa thư, không hài.

**Điều kiện hiển thị (đề xuất):** `mission.outcome == CourierOutcome.FAILED` (hoặc `phase == FAILED`); hiển thị **một lần** trước màn kết toán.

| Mã | Thứ tự | Speaker | `emotionTag` | Lời thoại | Ký tự | Vì sao câu này |
|---|---|---|---|---|---|---|
| NO-B1 | 1 | Kaelen | `[Nặng nề]` | Tôi nghe tiếng nó gọi. Rồi không còn gì để nghe nữa. | 52 | `STOIC` nhận phần trách nhiệm về mình; câu ngắn = sốc |
| NO-B2 | 2 | Lyra | `[Lạnh]` | Tôi đã bảo rồi. Người đưa thư không phải bao cát để chúng ta đổi lấy một mũi tên. | 81 | Nói thẳng cái giá của truy kích; không chửi Master |
| NO-B3 | 3 | Vesper | `[Cẩn trọng]` | Sự thật: chúng ta chọn truy kích. Ký ức giữ nguyên lựa chọn đó, không tô lại. | 77 | Đặt ranh giới đạo đức của ký ức, mở đường cho "thử lại" |
| NO-B4 | 4 | Dẫn chuyện (speaker = `Hệ Thống Tháp Vĩnh Hằng`, `emotionTag = [Đồng bộ ý thức]`) | — | Tù và vang ba lần. Không ai đáp. Người đưa thư nằm lại giữa lối đất mòn. | 72 | Đóng cảnh bằng hình ảnh, không bằng UI; khớp giọng hệ thống đã có ở prologue |

**Hệ quả thật sau kết cục B:**
- `COURIER_FAILED` được ghi (thay cho `COURIER_RESCUED` nếu có), giá rèn giữ 120.
- **Không** ghi ký ức vào `bondSummary` — vì `isFirstRescue` chỉ đúng với `RESCUED`. Điều này đúng về mặt thiết kế: ký ức là thứ phải *kiếm được*.
- Sĩ khí −22 (`LocalGuestSaveManager.kt:1172`), sàn `moraleFloor` (15 hoặc 30 nếu có East Garden) → hero dễ rơi vào vùng "Rạn" `< 50`, tức dễ bị chọn làm `noviceId` ở lần sau. **Đây là vòng nhân quả đã có sẵn trong code**; A07 chỉ đặt ngôn ngữ cho nó.
- Settlement UI hiện câu: *"Người đưa tin chưa tới cổng. Có thể trở lại Tầng 1 để thử hộ tống lần nữa."*

**Đề xuất bổ sung (ticket riêng):** thêm **một** dòng hướng dẫn cụ thể thay vì chung chung:
> `«Lần sau: giữ Bảo Vệ tới khi trinh sát chết, hoặc chặn nó trong 6 giây đầu.»`

Lý do: câu này dạy đúng cơ chế thật (bọc lót 40% + cửa sổ cảnh báo), nhưng **phải** khớp cửa sổ 9s hiện có. Nếu A11 đổi cửa sổ, dòng này phải sửa cùng lúc.

**Điều cấm ở kết cục B:** không nói "người đưa thư yếu", không nói "Master đã thất bại" bằng giọng hệ thống phán xét, không chế nhạo, không dùng từ nặng ngoài đời.

---

## 6. Đối chiếu hai kết cục

| Tiêu chí | NO-F1-A `RESCUED` | NO-F1-B `FAILED` |
|---|---|---|
| Hero nhận ký ức | Có (hero từng sợ, lần cứu đầu) | Không |
| Flag | `COURIER_RESCUED` | `COURIER_FAILED` |
| Giá rèn | 90 | 120 |
| Sĩ khí | −6 | −22 |
| Số beat | 4 | 4 |
| Có mở nội dung mới? | Tên Bách + tuyến Đông | Tên Bách xuất hiện **gián tiếp** (không nói tên — họ chưa kịp biết) |
| Đường thử lại | Qua settlement + Lobby | Trực tiếp nhắc "trở lại Tầng 1" |
| Tông | Mệt nhưng đứng thẳng | Nặng, dứt khoát, có lối ra |

Chi tiết quan trọng: ở kết cục B, người chơi **không** biết tên Bách. Tên chỉ xuất hiện khi cứu được — biến thông tin thành phần thưởng cảm xúc, không cần thêm UI.

---

## 7. Ràng buộc kỹ thuật nội dung

| Ràng buộc | Giá trị | Lý do |
|---|---|---|
| Độ dài 1 dòng thoại | ≤ 130 ký tự (dài nhất hiện tại: 127) | Khối thoại trên màn 800×360, font hiện có |
| `bondSummary` sau khi nối | ≤ 200 ký tự (code cắt cứng) | `LostCourierProgression.rememberRescue` |
| Số enum mới | 0 | Chỉ `RESCUED`/`FAILED` |
| Field save mới | 0 | Không migration, không phá guest offline |
| Ngôn ngữ | Tiếng Việt có dấu, không emoji trong lời thoại | Khớp `ManhwaStoryData`; i18n ngoài phạm vi |
| `panelArtRes` | `null` cho beat mới | Không có PNG mới; **không** tái dùng art có sẵn một cách sai ngữ cảnh |
| Idempotency | Beat chỉ phát 1 lần/expedition, state trong bộ nhớ | Không ghi save, không phá retry |
| Determinism | Chọn beat theo `personality` + `outcome`, không random | Test được |

---

## 8. Tự kiểm & validation log (chạy thật trong phiên)

| Tiêu chí | Kết quả |
|---|---|
| Đúng 2 kết cục, đúng tên enum thật | ✔ `RESCUED` / `FAILED` |
| 3 hero responses (spec heroes) + 4 beat mỗi kết cục | ✔ |
| Mọi câu ≤ 130 ký tự | ✔ max 127 |
| Không trùng nguyên văn chuỗi nào trong `app/src/main/java` (≥40 ký tự liên tiếp) | ✔ 0 trùng |
| Không chép manhwa ngoài repo | ✔ nội dung tự viết |
| Không sửa code/save | ✔ `git diff --stat` chỉ file `.md` |
| Chạy thật trong Android | ✘ **KHÔNG** (spec) |

```
$ python3 /tmp/a07_check.py
lines checked: 11 (3 hero responses + 8 outcome beats)
max chars: 127 / limit 130 -> PASS
unique lines: 11/11 -> PASS
bondSummary combined: 111 / 146 / 127 vs cap 200 -> PASS
verbatim >=40-char overlap with app/src/main/java: 0 -> PASS
files changed: 2 markdown only -> PASS
```

Script là công cụ tạm trong phiên, **không** commit vào repo để giữ phạm vi file đúng ticket.

---

## 9. NOT TESTED (trung thực)
- **NOT TESTED:** hiển thị thật của 4 beat trong Android Compose; thời gian đọc; chồng lấn với `CourierMissionHud`/panel kết toán khi xoay ngang/dọc.
- **NOT TESTED:** playtest người thật 5–10 phút; không có trong phiên này.
- **NOT TESTED:** tương tác với PR #28 (draft) đang mở — PR đó chứa `RescueJourney`/`HeroAgency` **chưa merge**; tài liệu này chỉ dựa trên `main`, không dựa trên PR #28.
- **Không** đo FPS/nhiệt: không đổi runtime.

---

## 10. Câu hỏi mở
1. **A05:** 4 beat mỗi kết cục có làm chậm nhịp "5–10 phút chơi" không? A07 sẵn sàng rút còn 3 beat (bỏ NO-A4 / NO-B4) nếu cần nhịp nhanh.
2. **A04:** Có chỗ đặt một dòng "Người giữ ký ức: <tên>" ở màn kết toán mà không phá bố cục hiện tại không?
3. **Codex:** Beat hiển thị-only (không state save) có cần event ID/idempotency riêng không? A07 hiểu là **không**, vì không có settlement thay đổi.
4. **Argon:** Tông "không ăn mừng khi cứu được" có phù hợp dark-fantasy co-lead muốn không, hay cần một nhịp sáng hơn?
5. **Owner:** Có muốn Bách xuất hiện lại ở Sanctuary sau khi cứu (dialogue ngắn ngoài mission) không? Nếu có, đây sẽ là ticket A07 tiếp theo, không gộp vào PR này.

## 11. Tuyên bố nội dung gốc
Nhân vật Bách, tuyến Đông, toàn bộ lời thoại và tên kết cục trong tài liệu này do A07 viết riêng cho ETERNAL SUMMONER. Không sao chép nguyên văn từ manhwa/truyện/webtoon; không dùng tài sản hoặc nhân vật có bản quyền ngoài repo. Nội dung bám tông dark-fantasy hiện có và không khẳng định bất kỳ đoạn nào đã chạy trong game.
