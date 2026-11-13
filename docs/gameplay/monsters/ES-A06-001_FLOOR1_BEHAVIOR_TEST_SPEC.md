# ES-A06-001 — Đặc tả kiểm thử hành vi quái Floor 1

**Vai trò:** A06 — Monster AI / Encounter Tactics
**Trạng thái:** SPEC FOR REVIEW — docs only, chưa implement test/runtime
**Baseline `main`:** `bdff6f9976dc4927e1e095a503bc339509c7a9aa`
**Nhánh tài liệu:** `arena/cde28904-eternal-sunmmoner`
**Phạm vi ghi duy nhất:** `docs/gameplay/monsters/ES-A06-001_FLOOR1_BEHAVIOR_TEST_SPEC.md`

Tài liệu này đặc tả hành vi và test deterministic cho hai encounter đã có trong Lost Courier: **Goblin trinh sát** và **Thủ lĩnh Kraag**. Đây không phải yêu cầu thay balance, thêm state machine mới hoặc quyền sửa combat core.

## 1. Work order và giá trị cho người chơi

Người chơi cần nhận biết hai quyết định khác nhau:

1. **Chặn trinh sát trước tiếng tù và** để tránh hai viện binh, hoặc chấp nhận viện binh rồi quay về bảo vệ courier.
2. **Ra khỏi vòng đỏ đã khóa vị trí của Kraag**, sau đó dồn hỏa lực trong recovery; không bị hit trễ, hit hai lần hoặc mất lệnh di chuyển vì một đòn hero đang wind-up.

Spec phải làm cho các lựa chọn đó kiểm chứng được bằng simulation time và input thật, không chỉ bằng text HUD hoặc ảnh tĩnh.

### File ownership và no-go

- Writer duy nhất của tài liệu này: A06 trong phiên Arena đã claim Issue #57.
- Không sửa `AutonomousHeroAi.kt`, `ContactAttackTimeline.kt`, `TowerCombatEngine.kt`, `LostCourierEncounter.kt`, tactical commands, cooldown, save, map, collision, renderer, camera, Gradle hoặc asset.
- Không tạo test Kotlin trong ticket này. Mọi test implementation tiếp theo cần Codex giao exact paths vì chạm shared battle behavior.
- Giữ `ObliqueCamera` và ground Y scale `0.72`; vòng telegraph là world-ground geometry, không phải top-down/isometric mới.
- PR #28 và #47 vẫn mở ở thời điểm audit. Không coi source của hai HEAD đã được tích hợp với nhau hoặc với `main`.

## 2. Nguồn đã đối chiếu

| Nguồn | SHA | Nội dung dùng cho spec |
|---|---|---|
| [`main`](https://github.com/lightingfff/eternal-sunmmoner/tree/bdff6f9976dc4927e1e095a503bc339509c7a9aa) | `bdff6f9976dc4927e1e095a503bc339509c7a9aa` | Baseline encounter và các test đã có trên `main`. |
| [PR #28](https://github.com/lightingfff/eternal-sunmmoner/pull/28) | `09c553d81c53046fe1038b70c7046e2a6cff0661` | Rescue Journey, party thật, retreat/retry và test dodge trong hero wind-up. |
| [PR #47](https://github.com/lightingfff/eternal-sunmmoner/pull/47) | `c296e0958e35b1e8d0d4b339857e188f81776ea8` | Chính sách urgent movement hủy contact wind-up, giữ cost/cooldown và ngăn late hit. |
| [Issue #57](https://github.com/lightingfff/eternal-sunmmoner/issues/57) | live `READY` khi claim | Docs-only; reviewer Codex + A09. |

Các đoạn implementation chính:

- [Scout spawn/timer](https://github.com/lightingfff/eternal-sunmmoner/blob/09c553d81c53046fe1038b70c7046e2a6cff0661/app/src/main/java/com/example/game/combat/LostCourierEncounter.kt#L152-L197).
- [Scout death resolution](https://github.com/lightingfff/eternal-sunmmoner/blob/09c553d81c53046fe1038b70c7046e2a6cff0661/app/src/main/java/com/example/game/combat/LostCourierEncounter.kt#L289-L301).
- [Kraag telegraph/recovery](https://github.com/lightingfff/eternal-sunmmoner/blob/09c553d81c53046fe1038b70c7046e2a6cff0661/app/src/main/java/com/example/game/combat/LostCourierEncounter.kt#L233-L266).
- [Counter multiplier](https://github.com/lightingfff/eternal-sunmmoner/blob/09c553d81c53046fe1038b70c7046e2a6cff0661/app/src/main/java/com/example/game/combat/TowerCombatEngine.kt#L869-L885).
- [Urgent cancellation của PR #47](https://github.com/lightingfff/eternal-sunmmoner/blob/c296e0958e35b1e8d0d4b339857e188f81776ea8/app/src/main/java/com/example/game/ai/AutonomousHeroAi.kt#L53-L68).
- [Ma trận wind-up/contact/recovery của PR #47](https://github.com/lightingfff/eternal-sunmmoner/blob/c296e0958e35b1e8d0d4b339857e188f81776ea8/app/src/test/java/com/example/MasterCommandResponsivenessTest.kt#L102-L127).

## 3. Audit hành vi hiện có

Các hằng số dưới đây là **hành vi quan sát tại source**, không phải đề xuất buff/nerf.

### 3.1 Goblin trinh sát — `courier_scout`

| Khía cạnh | Hành vi hiện có | Counterplay cần bảo vệ |
|---|---|---|
| Trigger | Khi `courier.x >= 1320`, spawn scout 100 HP cùng hai ambusher; phase chuyển `SCOUT_ALARM`. | Người chơi nhận cơ hội chọn INTERCEPT hoặc PROTECT. |
| Tổng cảnh báo | `scoutWarningRemaining = 9s`. | Có cửa sổ phản ứng thật, không gọi viện tức thời khi xuất hiện. |
| Bỏ chạy | Khi timer `> 6s`: di chuyển tối đa `58 world units/s` về hướng `x + 80`, `y = 990`, qua `CollisionPhysics.resolvePosition`. | Truy đuổi bằng movement/attack thật; không teleport hoặc đi xuyên collision. |
| Gọi viện | Khi `0 < timer <= 6s`: đứng yên, `attackAnim = 0.2`; lúc timer về `0`: đặt `scoutCalledReinforcements`, spawn đúng hai `courier_reinforcement_*`. | Giết scout trước khi timer về 0 để chặn viện binh. |
| INTERCEPT | Chọn scout còn sống, đặt `designatedTargetId`, phát `ATTACK_TARGET`; party rời khỏi đội hình bảo vệ courier. | Đánh đổi rõ ràng: truy đuổi scout làm courier dễ bị tấn công hơn. |
| Scout chết | `afterStep()` resolve, timer về 0, không spawn viện binh; phase quay `ESCORT` nếu đang `SCOUT_ALARM`. | Không có reinforcement trễ sau khi đã chặn thành công. |
| Bỏ qua scout | Hai viện binh xuất hiện ở offset quanh courier; scout tiếp tục được xử lý như enemy thường sau khi resolved. | Có thể PROTECT: hero còn sống trong 165 units giảm damage courier còn 60%. |
| Kết thúc nhiệm vụ | `outcome != null` chặn `beforeStep`; fail/retreat xóa elite telegraph và dừng progression. | Retry phải sạch, không giữ timer/viện binh từ lượt trước. |

**Coverage đã có:**

- `LostCourierEncounterTest.scoutHasACounterWindowAndInterceptionPreventsRealReinforcements` kiểm tra nhánh bỏ qua và chặn; nhánh chặn dùng fixture gán `dead/hp`, không phải proof rằng party tự giết scout.
- `LostCourierStarterPartyTest` dùng new-guest party/equipment thật và INTERCEPT, nên bổ sung bằng chứng integration cho khả năng chặn.
- `LostCourierVisualTest` có capture và trace của production renderer/Robolectric; đây không phải physical-device evidence.

**Khoảng trống:** chưa có test tập trung cho biên 3/6/9 giây, spawn đúng một lần, cái chết đúng tick timer về 0, late INTERCEPT, retreat/retry trong lúc gọi và tính lặp lại của trace.

### 3.2 Thủ lĩnh Kraag — `courier_gate_captain`

| Khía cạnh | Hành vi hiện có | Counterplay cần bảo vệ |
|---|---|---|
| Trigger | Khi `courier.x >= 2220`, spawn tại `(2540,1050)`, phase `GATE_GUARD`. | Người chơi biết phải né rồi phản công, không chỉ đổ HP. |
| Chọn điểm đánh | Hero còn sống gần nhất; nếu không có thì courier. Ở tầm `<=165`, khi cooldown `<=0`, tạo telegraph tại vị trí target hiện tại. | Vị trí vòng được snapshot; di chuyển target sau đó không kéo vòng theo. |
| Telegraph | `radius=105`, `duration=1.5s`; Kraag đứng yên và `attackAnim=0.22`. | Có 1,5 giây simulation để dùng ĐIỂM ĐẾN. |
| Impact | Hero còn sống trong vòng nhận raw damage `58`; courier trong vòng nhận raw `48` (sau đó quy tắc PROTECT áp dụng cho courier). Chỉ actor tại vị trí impact mới được kiểm tra. | Actor ngoài radius hoặc đã knocked out không nhận hit; không có hit trễ ở vị trí cũ. |
| Recovery | Sau impact xóa telegraph, đặt `eliteRecoveryRemaining=1.8s`, `attackCooldown=4.5s`; Kraag đứng yên trong recovery. | FOCUS_FIRE đúng elite trong recovery nhận `counterMult=1.35`. |
| Hệ số counter | Nhân raw damage trước variance/defense và floor tối thiểu; chỉ áp dụng khi đúng elite, recovery `>0`, order `FOCUS_FIRE`. | Không được kỳ vọng HP loss cuối cùng luôn tăng đúng 35%; phải test đúng thành phần multiplier. |
| Kraag chết | `afterStep()` xóa telegraph và recovery. | Đòn AoE chưa impact phải bị hủy, không giết hero/courier sau khi Kraag đã chết. |

**Coverage đã có:**

- Starter-party/visual tests đi tới telegraph, dodge và recovery counter nhưng chủ yếu assert sự kiện xảy ra và outcome cuối.
- PR #28 `RescueTacticalCommandsTest.moveOrderEvadesCaptainTelegraphDuringHeroWindup` kiểm tra một đường dodge thành công, không cover ma trận biên/damage/cancel/death.
- PR #47 có 18 tổ hợp basic/skill × wind-up/contact/recovery × REGROUP/RETREAT/MOVE_TO cho hero contact attack; đây là nền tảng để mở rộng test dodge, không phải bằng chứng combined tree với PR #28.

**Khoảng trống:** chưa có test tập trung cho vòng khóa vị trí, biên radius, đúng một impact, recovery 1,8 giây, cooldown 4,5 giây, counter chỉ trong cửa sổ, Kraag chết trước impact và lệnh đến sát tick impact.

## 4. Các phát hiện cần reviewer lưu ý

| ID | Mức | Phát hiện | Hướng xử lý |
|---|---|---|---|
| A06-F01 | P1 testability | `TowerCombatEngine` dùng `Random.nextFloat()` cho damage variance/VFX và `UUID.randomUUID()` cho projectile IDs. Chưa thể nói full combat trace deterministic. | Test hiện tại phải assert invariant hoặc chuẩn hóa ID. Seeded RNG/stable ID injection là một ticket critical-system riêng do Codex duyệt; A06 không tự sửa. |
| A06-F02 | P1 coverage | Unit test chặn scout gán trực tiếp `dead/hp`; integration party thật tồn tại nhưng không kiểm tra biên thời gian và duplicate spawn. | Giữ cả fixture state-transition và ít nhất một test giết scout bằng attack thật; không gọi fixture là playthrough. |
| A06-F03 | P1 coverage | Kraag chưa có focused test cho once-only damage, radius, death-before-impact và recovery/cooldown boundary. | Bắt buộc các case K02–K08 trước khi tuyên bố telegraph/counterplay đạt. |
| A06-F04 | P1 integration | `main`, PR #28 và PR #47 có HEAD khác nhau; chưa verify combined tree tại một SHA. | Test implementation phải chạy lại trên exact integrated SHA sau khi owner merge/tích hợp; không chuyển kết quả CI của HEAD cũ thành PASS cho combined tree. |
| A06-F05 | P2 evidence | Mô tả PR #28 nhắc route ~54–55 giây, trong khi `Mission01BPlaythroughTest` tại HEAD yêu cầu 300–600 giây simulation. | Codex/A09 đối chiếu artifact mới nhất; không suy ra 5–10 phút human playtime từ simulation. |
| A06-F06 | P2 UX | `chooseCourierOrder(INTERCEPT)` vẫn có thể target scout sống sau khi đã gọi viện; HUD availability dùng timer `>0`. | Chốt policy late INTERCEPT: từ chối lệnh hay cho tiêu diệt scout nhưng không xóa viện binh. Không âm thầm đổi behavior trong test ticket. |

## 5. Hợp đồng deterministic test harness

Đây là đặc tả cho implementation tương lai, không phải file test đã tồn tại.

### 5.1 Fixture và clock

- Dùng `TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), ..., enableLostCourier=true)` và `stage=IN_PROGRESS`; không tạo engine/state machine song song.
- Unit fixture có thể đặt vị trí, cooldown và loại enemy không liên quan để cô lập cơ chế; comment rõ phần nào là fixture. Integration dùng new-guest Knight/Archer/Mage và equipment thật, không buff stats hoặc gán enemy chết để đạt victory.
- Simulation step chuẩn: `dt = 1f/60f`. Dùng simulation time tích lũy, không `System.currentTimeMillis`, `System.nanoTime`, Compose frame clock hoặc coroutine wall-clock delay.
- Mỗi scripted order ghi ở tick xác định. Với biên, chạy ba điểm `just before / at / just after`; không chỉ `repeat` rồi kiểm tra trạng thái cuối.
- Vị trí/timer so sánh với epsilon thống nhất do test định nghĩa (đề xuất `1e-4` cho fixture đơn giản); event count, IDs, HP và phase không dùng epsilon.
- Test ổn định với render cadence: cùng fixed simulation script, gọi renderer 0/1/nhiều lần không được làm đổi combat trace. Không dùng test này để tuyên bố FPS.

### 5.2 Dữ liệu trace tối thiểu

Trace CSV/JSON nên có:

`tick, simulationTime, eventLabel, entityId, targetId, order, phase, x, y, hp, cooldown, remaining, contactActive, livingHeroes, courierHp`.

Các `eventLabel` dưới đây là nhãn quan sát trong test, **không yêu cầu thêm production event bus**:

- `SCOUT_SPAWNED`, `SCOUT_FLEE`, `SCOUT_HORN`, `SCOUT_INTERCEPTED`, `REINFORCEMENTS_CALLED`.
- `CAPTAIN_SPAWNED`, `TELEGRAPH_STARTED`, `AOE_IMPACT`, `RECOVERY_STARTED`, `RECOVERY_ENDED`.
- `ORDER_ACCEPTED`, `CONTACT_CANCELLED`, `DAMAGE_APPLIED`, `RETREAT_COMPLETED`, `OUTCOME`.

Snapshot phải sort actor theo stable gameplay ID. Không so UUID projectile như một giá trị cố định. Nếu chưa inject RNG, hai lớp kiểm chứng được tách:

1. **Deterministic state/timer suite:** không phụ thuộc damage variance; so exact trace của timer, movement, orders, spawn counts và target selection.
2. **Full combat replay suite:** chỉ đòi hỏi byte/event-equivalent damage trace sau khi Codex cung cấp seeded RNG hoặc controlled damage seam hợp lệ. Trước đó ghi `BLOCKED_TESTABILITY`, không fake PASS.

### 5.3 Tiêu chí lặp lại

- Hai engine cùng source SHA, fixture, seed (khi đã có), `dt` và script phải cho cùng normalized event trace và final state.
- Chạy lại tối thiểu hai lần trong cùng JVM và một lần ở CI trên exact candidate SHA.
- Thay số lần render không được đổi trace. Thay đổi `dt` là một kiểm tra robustness riêng, không mặc nhiên đòi event tick giống hệt fixed-step baseline.

## 6. Ma trận test đề xuất

### 6.1 Nhóm nền deterministic

| ID | Thao tác | PASS khi |
|---|---|---|
| DET-01 | Chạy cùng fixture/script hai engine ở 60 Hz; so normalized trace mỗi tick. | Timer, phase, positions theo epsilon, HP (khi RNG controlled), spawn/death/order và outcome giống nhau; không khác do wall clock/UUID. |
| DET-02 | Lặp cùng script với 0, 1 và nhiều render pass giữa simulation ticks. | Combat trace không đổi; chỉ visual artifact khác được phép. |
| DET-03 | Chạy biên timer bằng tick ngay trước, đúng và ngay sau threshold; đồng thời thử một bước lớn hơn threshold. | Không duplicate event, không bỏ impact/spawn, không âm timer; mọi khác biệt theo coarse `dt` được giải thích bằng tick quantization. |

### 6.2 Goblin trinh sát

| ID | Kịch bản | Assertions bắt buộc |
|---|---|---|
| S01 — spawn once | Đặt courier ngay dưới `x=1320`, step; sau đó qua threshold và step thêm. | Chỉ một `courier_scout` và hai ambusher ban đầu; phase `SCOUT_ALARM`; timer bắt đầu 9s; không có `courier_reinforcement_*`. |
| S02 — flee/horn boundary | Giữ party không tấn công; quan sát timer quanh 6s. | Trước biên scout di chuyển theo speed/dt và collision hợp lệ; từ horn phase scout đứng yên, animation cue active; chưa spawn viện binh. |
| S03 — zero boundary/once-only | Đi tới ngay trước 0, impact tick và nhiều giây sau. | Trước 0 không viện binh; tại resolve có đúng hai ID duy nhất; các tick sau không thêm; `scoutCalledReinforcements=true`. |
| S04 — real intercept | Với party thật, phát INTERCEPT khi còn đủ thời gian; để hero movement/attack/projectile giết scout. | `designatedTargetId=scout`, `ATTACK_TARGET`; scout chết do combat thật; sau mốc 9s vẫn không viện binh; phase/outcome hợp lệ; courier không tự được bất tử. |
| S05 — death at deadline | Fixture sắp damage contact thật của hero trùng tick timer về 0; chạy cả hai thứ tự input hợp lệ. | Nếu scout đã dead trước enemy update thì không viện binh; nếu horn resolve trước thì đúng hai viện binh và không bị xóa. Kết quả phải khớp thứ tự engine được Codex xác nhận, không flaky. |
| S06 — late intercept | Để horn resolve rồi mới INTERCEPT. | Không despawn hai viện binh, không reset timer/cooldown, không duplicate; command acceptance/target phản ánh policy đã chốt ở A06-F06. |
| S07 — protect trade-off | So sánh cùng một enemy hit khi có/không có hero còn sống trong 165 units và `PROTECT_ALLY`. | Nhánh protected giảm raw courier damage còn 60% theo rounding hiện có; ngoài radius/knocked out không kích hoạt; Master sigil không thay courier nhận hit. |
| S08 — retreat/retry | RETREAT trong flee và horn phases; về extraction rồi tạo retry từ flow thật. | Không victory; `retreatCompleted` đúng; timer không chạy sau outcome; không delayed spawn; retry không giữ scout/viện binh/telegraph/cooldown transient cũ. |

### 6.3 Thủ lĩnh Kraag

| ID | Kịch bản | Assertions bắt buộc |
|---|---|---|
| K01 — locked telegraph | Tạo warning tại target; di chuyển target ngay sau đó. | `(x,y,radius,duration)` của warning không đổi; Kraag đứng yên; target di chuyển không kéo vòng; không damage trước expiry. |
| K02 — radius and once-only impact | Đặt hero/courier ở trong, đúng biên và ngoài radius; thêm hero knocked out; đi qua expiry và step tiếp. | Chỉ living actor trong/biên radius nhận đúng một hit; ngoài/knocked out không hit; HP không giảm thêm ở tick sau; AoE visual không gây damage lần hai. |
| K03 — raw damage/protect | Dùng stats và RNG controlled để tách raw 58 cho hero, raw 48 cho courier; lặp courier có/không PROTECT. | Hero defense/shield áp dụng đúng pipeline; courier protected nhận 60% theo rounding; không có invisible Master damage. |
| K04 — successful MOVE_TO dodge | Trong warning, phát MOVE_TO tới điểm đủ xa, có hero basic/skill wind-up. | Order accept ngay; next fixed tick hủy pending hit nếu chưa contact, giữ cooldown đã tiêu; hero ra ngoài vòng bằng movement/collision thật; không captain damage và không stale hero hit. |
| K05 — late order | Phát MOVE_TO khi thời gian còn không đủ để ra radius và ngay tại impact boundary. | Không teleport/cancel AoE đã emitted; không late/duplicate hit; kết quả đúng thứ tự tick. Test không được đổi vị trí để giả vờ dodge thành công. |
| K06 — recovery window | Theo dõi từ impact tới ngay trước/tại/sau 1,8s. | Telegraph null, Kraag đứng yên trong recovery; recovery giảm đúng simulation time và không âm; không warning mới chỉ vì recovery vừa hết nếu cooldown 4,5s chưa hết. |
| K07 — counter multiplier | Với cùng raw attack, defense, RNG sample, so hit đúng elite trong/ngoài recovery và với order khác FOCUS_FIRE. | Chỉ `elite + recovery>0 + FOCUS_FIRE` có raw multiplier 1,35; hết recovery/order khác/enemy khác không có; kiểm tra trước defense/floor hoặc dùng controlled seam. |
| K08 — death/failure cancellation | Cho Kraag chết bằng combat thật trước impact; fixture bổ sung cho death đúng boundary; riêng test retreat/fail. | Warning bị xóa; không AoE damage trễ, recovery không chạy, không reward/victory sai; visual death có thể tiếp tục nhưng không tạo gameplay hit. |
| K09 — repeat cycle/finite motion | Cho Kraag sống qua ít nhất hai chu kỳ với actor còn trong tầm. | Mỗi warning có một impact; không chồng warning; cooldown/recovery không reset ngoài ý đồ; vị trí finite, di chuyển qua collision, không NaN/teleport. |

### 6.4 Ma trận lệnh khẩn liên encounter

Giữ nguyên policy PR #47 khi integrated:

| Hero contact phase khi nhận lệnh | REGROUP / RETREAT / MOVE_TO | Damage/shield | Cost |
|---|---|---|---|
| Wind-up trước contact | Hủy pending contact và bắt đầu movement ở next fixed tick nếu cần reposition | Không phát sinh hit/shield trễ | Cooldown đã charge vẫn mất |
| Contact boundary chưa xử lý | Lệnh thắng, cancel pending contact | Không hit trễ | Không refund |
| Contact đã xử lý | Cancel phần recovery còn lại nếu urgent | Giữ effect đã giao, không duplicate | Không refund |
| Đã tới slot hợp lệ | Không hủy mọi attack sau arrival | Attack sau arrival được hoàn tất bình thường | Theo cooldown hiện có |

Các case K04/K05 phải chạy trên integrated SHA để chứng minh policy này thực sự hoạt động với Kraag, không chỉ với unit fixture của `AutonomousHeroAi`.

## 7. Acceptance gates cho implementation tiếp theo

### Blocker nhị phân

1. Tất cả test DET, S01–S08, K01–K09 chạy trên cùng exact integrated SHA; không có skip ngầm.
2. Không late/duplicate damage, không reinforcement duplicate, không attack xuyên collision, không NaN/teleport và retreat/retry không softlock.
3. Ít nhất S04 và K04 dùng party/command/damage thật; fixture trực tiếp không được trình bày như human playthrough.
4. `Random`/UUID chưa được kiểm soát thì full-replay determinism phải ghi `BLOCKED_TESTABILITY`; state/timer tests vẫn có thể PASS độc lập.
5. Diff implementation tương lai chỉ gồm exact paths được Codex giao; không đổi camera `0.72`, map, save schema, Master off-map hay renderer ngoài phạm vi.

### Evidence cần nộp

- Candidate SHA, base/integrated SHA, diff paths và test list có tên từng case.
- Command phù hợp CI hiện có (repo dùng Gradle được setup trên runner, không có `gradlew` script tại baseline):

```sh
gradle :app:testDebugUnitTest \
  --tests "com.example.LostCourierEncounterTest" \
  --tests "com.example.LostCourierStarterPartyTest" \
  --tests "com.example.RescueTacticalCommandsTest" \
  --tests "com.example.MasterCommandResponsivenessTest" \
  --stacktrace

gradle :app:assembleDebug --stacktrace
```

Khi test mới được duyệt, thêm đúng class name vào lệnh; không dùng tên class giả định làm bằng chứng PASS.

- XML/JUnit counts và CI URL trên candidate SHA; normalized trace cho cả thành công, bỏ qua scout, fail/retreat và retry.
- Native renderer capture cho warning trước impact, impact/recovery và dodge ở gameplay zoom nếu visual behavior được thay đổi. Đánh nhãn Robolectric/native desktop, không gọi là emulator/device.
- Human 5–10 phút, touch latency, FPS, jank, GPU và thermal: `NOT TESTED` nếu không có thiết bị và protocol đo thật.

### Rubric review (không tự chấm khi thiếu evidence)

Codex/A09 đánh giá technical correctness, player clarity, tactical value, polish và efficiency; visual coherence chỉ chấm khi có runtime image thật. Một blocker nhị phân FAIL thì không được handoff như DONE.

## 8. Quyết định cần Codex/A09 chốt trước khi viết test mới

1. Exact test paths và writer; có tái sử dụng `RescueTacticalCommandsTest` hay tạo suite riêng.
2. Seeded RNG/stable projectile ID seam: có cần cho acceptance full replay không, và ai sở hữu `TowerCombatEngine.kt`.
3. Thứ tự kỳ vọng khi scout death/horn và hero movement/Kraag impact cùng tick.
4. Late INTERCEPT sau khi viện binh đã gọi: reject hay giữ target; cập nhật HUD availability tương ứng chỉ qua ticket phù hợp.
5. Kết quả settlement của retreat là fail/retry như hiện tại có đúng product policy không; A06 không tự đổi.
6. Artifact nào của PR #28 thay thế mô tả route ~55 giây và liệu simulation 300–600 giây đã được CI xác nhận trên HEAD nào.

## 9. Evidence và giới hạn của chính tài liệu này

- **Đã làm:** đọc live Issue #39/#57, claim protocol/role skills; inspect source và tests tại ba SHA ở mục 2; đối chiếu coverage scout/Kraag và policy PR #47.
- **Runtime changes:** NONE.
- **Local Gradle/tests:** NOT RUN — môi trường checkout không có `java`, `javac`, `gradle`, `kotlinc` và không có `gradlew` script. Không suy ra PASS từ việc đọc source.
- **CI của tài liệu:** phải kiểm tra trên PR/HEAD thật sau khi mở PR; chưa ghi PASS tại thời điểm soạn spec.
- **Screenshots:** NOT CAPTURED cho ticket docs-only này. Các screenshot/test claims trong PR #28/#47 là tài liệu tham chiếu, chưa được A06 chạy lại độc lập.
- **Physical Android device, human playtime, FPS/thermal:** NOT TESTED.
- **Definition of done cho ES-A06-001:** PR chỉ chứa file spec này, qua Argon + Codex + A09 review trên exact HEAD; owner quyết định merge. Không tự merge.
