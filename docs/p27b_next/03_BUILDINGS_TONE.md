# CHECKPOINT 2 — art nhà ở/props/facility theo P27B (debt A)

Trạng thái: **đã commit + render thật + test hợp đồng.** Ảnh: `docs/p27b_next/previews/after/world/` (run `42032970434`, bot commit `c89cc91` + `ff0b4f1`).

## 1. Lỗi được đo trước khi vẽ
| Nhóm | Luma trung bình (pixel đục) |
|---|---|
| P27B landmark (Hall, Gate) | **179.7 / 170.5** |
| P03 nhà ở (townhouse_a/b, cottage_brown/slate) | 51.4 / 58.9 / 50.6 / 58.6 |
| premium_slice (registry, armory, lab, quarters, restaurant) | 54.7 / 51.1 / 54.5 / 50.0 / 58.5 |

Không phải "lệch tông nhẹ": hai bảng màu khác nhau trên cùng một map. Đây là debt A.

## 2. Đã thay (12 asset + 3 cây + 5 props + 4 facility)
Mỗi asset được cắt vào **đúng canvas cũ** và **giữ pivot đáy-giữa**, nên `footprintHalfW`, `footprintDepthNorth`, trigger cửa, silhouette bóng đổ và depth sort **không đổi**:

| Asset | Canvas (không đổi) | Luma mới | Luma cũ |
|---|---|---|---|
| townhouse_a (half-timber có crest) | 152×156 | 86.1 | 51.4 |
| townhouse_b (stone guild) | 144×160 | 117.9 | 58.9 |
| cottage_brown (mái rơm) | 132×144 | 148.4 | 50.6 |
| cottage_slate (slate wing) | 132×144 | 93.9 | 58.6 |
| hero_quarters | 200×168 | 83.4 | 50.0 |
| armory | 190×166 | 84.4 | 51.1 |
| synthesis_lab | 188×168 | 90.0 | 54.5 |
| restaurant | 206×172 | 75.7 | 58.5 |
| street_lantern / bench / barrels / planter / dummy | 32×66 / 58×36 / 52×44 / 64×36 / 40×58 | 92 / 89 / 73 / 119 / 110 | 48 / 46 / 86 / 78 / 62 |
| canopy_a (plum) / pine / oak | 114×132 / 96×128 / 112×136 | 130 / 70 / 103 | 57 / 50 / 51 |

Quy tắc cắt: chỉ xoá pixel **nối được từ biên** và **trung tính** (max−min ≤ 14), nên tường kem ấm và dầm gỗ sáng không bị ăn thủng; alpha ép nhị phân để bóng đổ silhouette của renderer không bị nhoè.

Binding 3 tầng cho từng asset: **P27B-V5 → P03/premium_slice → procedural**.

## 3. Test hợp đồng mới (`P27BStructureContractTest`)
| Kiểm tra | Vì sao |
|---|---|
| canvas đúng từng pixel | footprint/collider/door trigger viết theo kích thước này |
| 3 hàng đáy phải có pixel đục | nếu không sprite sẽ lơ lửng trên bóng và collider |
| alpha nhị phân (0/255) | alpha mềm làm nhoè bóng silhouette |
| luma trong dải 68..155 và tăng ≥25% so với art cũ | debt A thành con số, không phải cảm nhận |
| runtime thật sự bind art mới | chống fallback âm thầm về P03 |

## 4. Một sai lầm của chính tôi, đã sửa
Test đầu tiên so **mọi** toà nhà với luma landmark (~175) và **FAIL** trên art đúng. Nguyên nhân: chỉ 2 landmark là đá ivory; nhà mới là kem + slate ~84. So nhà với đền sẽ đẩy art thành bợt. Đã thay bằng: mỗi asset phải nằm trong dải đo được của chính họ nhà P27B-V5 (68..155, gồm cottage rơm 148 và tán hoa 130) và phải dịch ≥25% từ giá trị P03 cũ. CI đã xác nhận PASS.

## 5. Còn lại của debt A (chưa làm, không giấu)
- **hero_registry**: đã bind sang art mới nhưng **chưa có art** → vẫn rơi về premium_slice (luma 54.7). Nhìn thấy rõ trong `service_noon_z088.png` (toà xám đậm giữa ảnh).
- **fortress_tower / gate_tower** (8 tháp góc + 2 tháp cổng nam, P03): art tối, lệch hẳn với tường thành ivory; cần pass riêng.
- **weapon_rack, striking_post, boulder, hedge_bush, forest_bush, monolith, ruin_arch, retaining_wall, canopy_b, twisted, cypress**: vẫn P03.
- Mái slate xanh đậm xuất hiện dày hơn ở P27B-V5 so với nền sáng — đúng chủ ý (để toà nhà tách khỏi nền), nhưng cần review trên thiết bị thật.

## 6. NOT TESTED
FPS/thermal/máy thật. Không có JDK/Android SDK local; mọi build/render qua GitHub Actions.
