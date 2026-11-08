# FLOOR 1 — ĐỒNG CỎ KHỞI NGUYÊN: đặc tả mission cứu hộ 5–10 phút (v1, DRAFT chờ review)

- **Tác giả:** A05 Tower Level & Quest Designer (Arena Agent Mode, phiên `arena/df5f7758-eternal-sunmmoner`) — ticket [ES-A05-001/#56](https://github.com/lightingfff/eternal-sunmmoner/issues/56).
- **Ngày:** 2026-11-08 · **Baseline main:** `bdff6f9976dc4927e1e095a503bc339509c7a9aa` · **Tham chiếu code:** PR #28 @ `09c553d81c53046fe1038b70c7046e2a6cff0661` (đọc-only, xem `ES-A05-001_PR28_REVIEW.md` cùng thư mục).
- **Trạng thái:** đặc tả thiết kế, **chưa nối logic, chưa sửa runtime**. Mọi mục "ĐỀ XUẤT" cần Codex (core combat/save) + A06 (hành vi) + owner duyệt trước khi lập ticket triển khai. Reviewer: A06, A07, Codex; Argon/A09 theo DUAL_LEAD_WORKFLOW. User merge duy nhất.
- **Bằng chứng hiện trạng:** số liệu pacing trong review là từ CSV simulation thật của PR #28; **đặc tả này chưa được playtest — mọi thời lượng bên dưới là ngân sách thiết kế, NOT TESTED**.

## 1. Mục tiêu người chơi & phạm vi

Người chơi mới (party khách khởi đầu Knight/Archer/Mage lv1, không chỉnh chỉ số) hoàn thành một chuyến cứu hộ người đưa thư trên **đúng đồng cỏ Floor 1 hiện có** trong 5–10 phút chơi thật: mở đường → chọn cứu người/thu tiếp vận → chọn tuyến Bắc/Nam → chặn báo động → hạ Kraag → hộ tống về cổng, với **2 nhánh kết quả chính + partial + fail/retry**, hệ quả lưu bền sau restart.

**Giữ nguyên (bất khả xâm phạm):** Kotlin/Compose Canvas offline; `OBLIQUE_Y_SCALE=0.72f`; meadow gốc 3400×2150 và mọi prop/đường cũ đúng tọa độ; Master chỉ huy ngoài map (không avatar điều khiển); courier 44u/s, HP 300; 4 outcome và toàn bộ cờ `COURIER_*` hiện có; thưởng lần đầu +20 Mana (support) / +60 Vàng (supplies); giảm giá synthesis 120→90 sau cứu hộ; save schema không đổi; không tăng HP địch/giảm tốc courier/thêm timer chờ để kéo giờ.

**Không làm trong iteration này:** city/NPC crowd, stress-trust lưu bền, skill/ultimate mới, enemy role thứ tư, đổi renderer/asset, mở rộng map. Mỗi iteration chỉ thêm **một cơ chế mới có quyết định đo được** (§4).

## 2. Vấn đề cần giải quyết (tóm tắt từ review)

Bản hiện tại trong PR #28 đạt ~318s simulation nhưng 69,6% là đi đường (route ~9.891u), combat hành trình ~4%, gap chết tới 36,3s, 1 quyết định giữa hành trình, field action không rủi ro. **Hướng sửa: rút quãng đường, dồn thời gian vào quyết định và chiến đấu có counterplay — không kéo dài bằng di chuyển.**

## 3. Cấu trúc 6 beat trên tọa độ thật của meadow

Tọa độ lấy từ `TowerFloorCatalog`/`LostCourierEncounter` @ 09c553d8: spawn courier (600,1049); guard (880,1000)+(940,1140); ngã ba đường mòn + cache tiếp vận (1220,1130); nhánh Bắc có sẵn `northBranch` (960,1085)→(1340,640)→(1740,630)→(2120,1030) với rương `f1_discovery_chest_north` (1540,585); nhánh Nam có sẵn `southBranch` (1180,1125)→(1480,1520)→(1820,1530)→(2240,1075) với đài thuốc `f1_discovery_shrine_south` (1660,1565); Kraag (2540,1050); cổng `f1_goal_portal` (3035,1060). Địch hai nhánh cũng đã có sẵn trong `FLOORS[0].enemies`: Bắc 2 DIRE_WOLF (1400,620)+(1510,700) + 1 archer (1610,640); Nam 1 raider (1540,1490) + 1 wolf (1660,1540). Không cần prop/asset mới; ĐỀ XUẤT thêm 1 `PathSegment` nối cache (1220,1130)→(1340,640) (~504u, đường vẽ thủ tục như các PathSegment hiện có, không phải asset) để vào nhánh Bắc từ ngã ba không phải quay đầu.

| Beat | Vị trí thật | Nội dung & quyết định | Ngân sách sim* |
|---|---|---|---:|
| B1 Mở đường | 600→1100, guard (880,1000)/(940,1140) | Dạy FOCUS_FIRE dọn 2 guard; courier nhập đội. Mục tiêu hiện ngay ở gameplay zoom. | 20–30s |
| B2 Ngã ba & lựa chọn tiếp vận | (1220,1130) | Courier dừng nghỉ. **Choice giữ nguyên hợp đồng:** SUPPORT (băng +60HP, đi ngay) vs RECOVER_SUPPLIES (giữ hero trong 90u/3s tại cache; ĐỀ XUẤT A06: 1 raider lẻ tạo áp lực trong lúc channel — rời vị trí là reset, không úp sọt quá 2 con). | 15–30s |
| B3 Chọn tuyến (cơ chế mới của iteration) | Bắc qua (1340,640)→rương (1540,585)→(1740,630)→hẹn lại (2120,1030); Nam qua (1480,1520)→đài thuốc (1660,1565)→(1820,1530)→hẹn lại (2240,1075) | **Một quyết định, hai hệ quả thấy ngay, độ dài gần bằng nhau** (đo từ tọa độ thật: Bắc ~3019u/69s, Nam ~2930u/67s toàn tuyến): Bắc có rương +65 vàng/+15 mana nhưng gặp tổ mạnh 2 sói flank + 1 archer kiter (§5); Nam có đài hồi 35% máu +20 mana, tổ nhẹ 1 raider + 1 sói. Trade-off là **loot+rủi ro vs hồi phục+an toàn**, không phải độ dài. HUD nêu rõ trước khi chọn; đổi ý được trước khi courier qua điểm rẽ. | 45–70s |
| B4 Báo động | sau điểm hẹn lại (2120,1030) hoặc (2240,1075) tùy tuyến, trên đường tới Kraag | Giữ nguyên cơ chế trinh sát 3s chạy + 6s tù và (kích hoạt theo vị trí courier như hiện tại, mốc x do Codex chỉnh theo tuyến): INTERCEPT chặn (không viện binh) vs PROTECT giữ đội (2 viện binh nếu tù và vang). Fear novice 10s + agency alert như hiện có. | 10–20s |
| B5 Kraag chắn cổng | (2540,1050) | Giữ nguyên telegraph 1,5s → MOVE_TO né → hồi 1,8s FOCUS_FIRE +35%. Viện binh từ B4 (nếu có) đánh vào courier buộc PROTECT thật. | 45–75s |
| B6 Rút về cổng | (2540→3035,1060) | **Làm đoạn rút về thành hộ tống thật:** hiện outcome chốt ngay khi tới waypoint cuối sau khi Kraag chết. ĐỀ XUẤT: tối đa 2 sói đuổi kích spawn một lần sau khi Kraag chết (đuổi courier, không đuổi hero — A06), đội vừa đi vừa bọc lót tới portal (3035,1060). Spawn hữu hạn, không wave lặp. | 15–25s |

\* Ngân sách thiết kế cho simulation kịch bản tốt, **NOT TESTED**. Tổng sim mục tiêu ~150–250s; phần còn lại của 5–10 phút là thời gian người thật đọc tình huống, ra lệnh, sửa sai — phải xác nhận bằng playtest người thật (§8), không giả định.

Hình học (đã tính từ tọa độ thật của road/discovery hiện có, gồm connector Bắc): tổng đường đi ~3.019u (Bắc) / ~2.930u (Nam) ≈ 67–69s đi đường thuần ở 44u/s ⇒ travel ≤ ~40% sim nếu phần còn lại là combat/quyết định (so với 69,6% hiện tại). Không quay đầu về spawn: extraction một chiều ra portal, bỏ loop debrief.

## 4. Cơ chế mới duy nhất của iteration: quyết định tuyến Bắc/Nam

- **Vì sao chọn:** tạo lựa chọn chiến thuật đo được (tham loot + rủi ro vs hồi phục + an toàn) bằng **discovery node và đường đã có**, không cần art mới, không đụng renderer; dạy người chơi đọc bản đồ ở zoom gameplay.
- **Đo được:** trace phải ghi `ROUTE_NORTH|ROUTE_SOUTH`, thời điểm quyết định, HP party lúc vào/ra tuyến, số lệnh đổi trong tuyến; hai tuyến phải cho delta khác biệt (loot hoặc HP) trong ≥1 lần chạy mỗi tuyến.
- **Fallback:** không chọn trước khi courier tới điểm rẽ → mặc định tuyến Nam (an toàn cho người mới), HUD báo rõ "chưa chọn — đi tuyến Nam"; không softlock.

## 5. Hợp đồng hành vi địch (giao A06 thiết kế chi tiết + test; Codex duyệt core)

Tối thiểu 2 kiểu hành vi khác biệt thực sự, đều có telegraph/counterplay, dùng archetype sẵn có:

| Kiểu | Archetype sẵn có | Hành vi mong đợi | Telegraph / counterplay | Dùng ở beat |
|---|---|---|---|---|
| KITER giữ khoảng cách | FIELD_GOBLIN_ARCHER | Giữ cự ly ~200u, lùi khi bị áp sát <105u (đã có trong engine), ưu tiên hero xa đội hình | Mũi tên thấy được; counter: MOVE_TO áp sát hoặc FOCUS_FIRE từng con | B3-Bắc (archer 1610,640 có sẵn) |
| FLANKER vòng sau | DIRE_WOLF | Vòng qua đội hình đánh courier/người yếu nhất thay vì lao thẳng (ĐỀ XUẤT: waypoint vòng sườn 1 lần, không teleport) | Hướng chạy vòng thấy trước ~1s; counter: PROTECT_ALLY (đã giảm 40% dmg courier) hoặc chặn bằng hero | B3 cả hai tuyến (sói có sẵn), B6 |
| HEAVY có cửa sổ phản công | GOBLIN_WAR_CAPTAIN (Kraag) | Giữ nguyên telegraph 1,5s/né/hồi 1,8s +35% | Vòng đỏ 105u; counter đã dạy | B5 |
| RUNNER báo động | FIELD_GOBLIN_RAIDER (scout, HP 100) | Giữ nguyên 3s chạy + 6s tù và | Đồng hồ tù và hiện HUD; counter: INTERCEPT | B4 |

Viện binh B5 khi tù và vang giữ nguyên 2 raider cận chiến như code hiện tại (đánh vào courier, buộc PROTECT), không thêm archer mới.

Test deterministic tối thiểu (A06/A09): (1) kiter không đứng yên cho đánh khi bị áp sát; (2) flanker thực sự đổi mục tiêu sang courier khi đội hình hở và dừng đổi mục tiêu khi PROTECT; (3) cùng seed cho cùng kết quả; (4) không enemy nào teleport/đánh xuyên vật cản; (5) tổng live enemies ≤ 12 giữ nguyên trần hiện tại.

## 6. Máy trạng thái & hợp đồng interface (để Codex/A11 nối, A05 không tự sửa)

**Giữ spine `CourierPhase` hiện có làm trục:** `CLEAR_PATH → ESCORT → SCOUT_ALARM → GATE_GUARD → RESCUED | FAILED`. B2/B3 nằm trong ESCORT dưới dạng sub-state nhiệm vụ; B6 là đoạn sau GATE_GUARD trước khi chốt outcome. Hai phương án triển khai cho Codex chọn (spec không khóa): (a) thu gọn `RescueJourneyContent` còn các beat trên, tái dùng framework chapter/field-action; (b) mở rộng `LostCourierEncounter` trực tiếp, tắt journey dài. Yêu cầu chung: bỏ map mở rộng 6200×3600 khỏi đường mission chính (A10 xác nhận lợi ích hiệu năng sau khi đo).

| Sự kiện (ID đề xuất) | Điều kiện phát | Hiệu ứng | Cờ/ghi nhận |
|---|---|---|---|
| `m01r_path_cleared` | 2 guard chết | courier bắt đầu đi | (như hiện tại, không cờ mới) |
| `m01r_choice_committed` | `chooseChoice()` tại ngã ba | băng bó hoặc mở channel pickup | `COURIER_SUPPORTED` / nhánh supplies **như hiện hành** |
| `m01r_route_north` / `m01r_route_south` | courier qua điểm rẽ theo lựa chọn | kích hoạt tổ địch tương ứng | ĐỀ XUẤT cờ mới additive `DISC_m01r_route_north/south` (Codex duyệt; không đổi schema, dùng storyFlags sẵn có) |
| `m01r_scout_resolved` | scout chết trước tù và / hết 9s | không viện binh / +2 viện binh ở B5 | như hiện hành (`scoutCalledReinforcements`) |
| `m01r_kraag_down` | Kraag chết | mở B6, spawn tối đa 2 sói đuổi kích một lần | không cờ mới |
| `m01r_extraction_done` | courier tới portal (3035,1060) ±32u | chốt outcome | 4 outcome **không đổi** |

**Outcome & phần thưởng — giữ nguyên tuyệt đối:** `RESCUED` (+20 Mana lần đầu, ký ức đồng đội), `SUPPLIES_RECOVERED` (+60 Vàng lần đầu), `RESCUED_WITHOUT_SUPPLIES` (không thưởng nhánh, được quay lại thu), `FAILED` (retry từ preparation, thương tích giữ nguyên). Settlement đi qua `LocalGuestDataSource`/transaction như hiện tại; cấm phát thưởng trước khi lưu.

**Vấn đề cần Codex quyết (phát hiện khi đọc code):** hiện `afterStep()` nếu người chơi **không chọn gì** vẫn ra `RESCUED` và nhận thưởng nhánh support lần đầu (outcome mặc định về nhánh RESCUED). ĐỀ XUẤT: giữ nguyên semantics để tương thích save, nhưng HUD bắt buộc nêu lựa chọn tối thiểu một lần trước GATE_GUARD; nếu owner muốn nhánh "trung lập" thật thì phải là ticket riêng có Codex duyệt, không âm thầm đổi trong rework pacing.

**Fallback bắt buộc:** không ra lệnh → courier đứng chờ khi không hero sống trong 300u (đã có) + gợi ý HUD sau ~20s đứng yên (chỉ UI, không phạt); địch đi lạc → aggro leash về điểm sinh (A06 định số); courier kẹt va chạm → `CollisionPhysics.resolvePosition` + retreat theo waypoint đã qua (đã có); cả đội gục/courier 0HP → FAILED, retry sạch (đã có test); bỏ lỡ choice → semantics hiện hành ở trên.

## 7. Nhịp & tiêu chí đo (thay tiêu chí "tổng giây 300–600")

Trace CSV mỗi lượt phải ghi như format mission01b hiện tại + các event mới §6. Tiêu chí nghiệm thu pacing **trên simulation kịch bản tốt**:

1. Khoảng cách giữa 2 sự kiện có nghĩa liên tiếp ≤ 20s (hiện 36,3s) — mọi gap >20s là defect phải giải trình hoặc sửa.
2. Tỉ lệ travel của courier ≤ 45% tổng sim (hiện 69,6%); combat+tương tác có quyết định ≥ 35%.
3. ≥ 3 quyết định khác loại có hậu quả quan sát được trong một lượt (choice tiếp vận, tuyến, intercept; + né/counter Kraag là kỹ năng thực thi).
4. Trần tài nguyên giữ nguyên: live enemies ≤ 12, roster ≤ 40, không wave vô hạn.
5. Tổng sim kịch bản tốt trong ~150–250s **không phải mục tiêu tự thân** — chỉ là hệ quả của mật độ; nghiêm cấm đạt chỉ tiêu bằng cách kéo đường/tăng HP/chờ.

**5–10 phút chỉ được kết luận bằng playtest người thật** (§8). Simulation không phải bằng chứng thời lượng người chơi.

## 8. Ma trận playthrough & quy trình đo người thật (giao A09 chủ trì, A05/A06 hỗ trợ)

| Lượt | Kịch bản lệnh | Kỳ vọng |
|---|---|---|
| A smart | Intercept scout, né telegraph, counter đúng cửa sổ, tuyến hợp trạng thái party | Thắng nhanh nhất, courier HP cao, đúng nhánh đã chọn |
| B passive | Chỉ lệnh mặc định, không micro | Vẫn qua được (người mới không bị trừng phạt oan) nhưng chậm hơn/HP thấp hơn; không softlock |
| C wrong | Bỏ tù và, đứng trong vòng đỏ, bỏ rơi courier đi loot | Viện binh gây áp lực thật, có thể FAILED → retry sạch; sai phải ra hậu quả đo được |
| Nhánh | A×{support, supplies-đủ, supplies-lỡ}, B3×{Bắc, Nam} | Đúng outcome/cờ/thưởng từng nhánh; restart giữ cờ; supplies-lỡ không thưởng giả |

Mỗi lượt: cùng seed, party lv1 không sửa, ghi CSV + screenshot engine đúng nhãn (Robolectric ≠ thiết bị). **Người thật:** tối thiểu 3 phiên người chơi mới trên thiết bị/emulator, bấm đồng hồ từ lúc vào Floor 1 tới màn kết quả, ghi lại số lần khựng >10s không biết làm gì; báo cáo median + khoảng, đạt 5–10 phút và không đoạn chết mới nghiệm thu. FPS/nhiệt: A10 đo trên máy thật, tách khỏi tiêu chí pacing. Hiện tất cả: **NOT TESTED**.

## 9. Ownership đề xuất cho ticket triển khai sau khi spec được duyệt

| Phần | Writer dự kiến | Ghi chú |
|---|---|---|
| Dữ liệu beat/route/tuyến (`RescueJourneyContent` hoặc module mission mới) | A05 | chỉ sau khi có ticket READY ghi exact paths |
| Core encounter/command/save (`LostCourierEncounter`, `TowerCombatEngine`, settlement) | **Codex** | critical, single writer |
| Hành vi kiter/flanker + test deterministic | A06 | qua contract §5, Codex duyệt |
| Thoại/hậu quả Sanctuary theo nhánh (được cứu ở tuyến Nam, ký ức fear…) | A07 | spec thoại riêng, không đụng logic |
| HUD mục tiêu/đồng hồ tù và/trade-off tuyến | A04 | giữ trần chiều cao HUD 30%/32% hiện có |
| Ghép UI–ViewModel, discovery flags | A11 | không đụng persistence critical |
| Đo hiệu năng sau khi bỏ map mở rộng | A10 | trước/sau cùng máy |
| Kiểm chứng độc lập toàn bộ | A09 | không sửa thứ mình review |

## 10. Giới hạn trung thực của đặc tả

Chưa implement, chưa render, chưa playtest; ngân sách giây là ước lượng thiết kế từ hình học route và trace cũ. Các cơ chế ĐỀ XUẤT (áp lực khi channel, sói đuổi kích B6, flanker đổi mục tiêu, cờ tuyến) chưa tồn tại trong code và cần Codex/A06 duyệt. Không sao chép nhân vật/thoại manhwa; tên Kraag/người đưa thư dùng lại nội dung gốc sẵn có của repo. Mọi thay đổi save/combat phải qua cổng review bắt buộc; owner là người duy nhất merge.
