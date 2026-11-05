# P27B / 03 — BEFORE vs AFTER từ renderer thật (đánh giá trung thực)

## Bằng chứng
- BEFORE: CI run `41982824376` (SHA `5a2876e`), ảnh `docs/p27b/previews/before/`.
- AFTER: cùng harness `P27BRenderTest` + `P27BUiTest`, cùng 1280×720, `timeSec=0`, BALANCED, hour 12/0 CLEAR, camera/zoom y hệt ảnh `docs/p27b/previews/after/`. CI [run #42017255236](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42017255236) trên `4baa7d1`, bot commit `c6f5adc`; validator 82/82, `P27BAssetContractTest`, `SanctuaryMaterialTest`, world render, Compose UI và `assembleDebug` đều PASS.
- Ảnh đều là Robolectric native Skia + code vẽ thật. Không ảnh concept, không screenshot điện thoại. **Không có FPS/GPU/thermal** — `NOT TESTED`.

## Đã xem và so sánh trực tiếp
| Cặp | Kết quả thật |
|---|---|
| `overview_noon_z036` | Nền cỏ sáng và có hoa văn rõ hơn hẳn; đường cobble/flagstone đọc được khối đá; Summoning Hall trắng ngà nổi khỏi cảnh; Gate trắng ngà đọc được giữa tường xám. Bệ đá quanh Hall **nhỏ đi rõ rệt** so với BEFORE vì sprite mới có apron riêng hẹp hơn. |
| `service_noon_z088` | Cỏ, đường và sân trước các nhà sáng hơn, mạch đá liên tục hơn; không thấy seam dọc theo path network. Nhà ở vẫn art cũ (`premium_slice`) nên độ tương phản giữa nhà xám và nền sáng mới lớn hơn BEFORE. |
| `plaza_noon_z100` | Flagstone mới có khối đá lớn hơn, grout xanh xám; ở zoom 1.0 thấy rõ chu kỳ 4 ô (đo: chênh trung bình theo chu kỳ 4 lớn hơn chênh giữa ô liền kề, ~20 vs ~12 mức xám). Vẫn là `GAME_CANDIDATE`, **không** gọi seamless. |
| `summon_noon_z148` / `summon_night_z148` | Hall mới có mái/mặt trước/mặt hông rõ ràng, cột và bậc thềm đọc được; ban đêm vẫn giữ khối chứ không bệt. Đây là cải thiện lớn nhất của slice. |
| `gate_noon_z148` / `gate_night_z148` | Gate trắng ngà có vòm, lớp gờ và hoa văn; rift vẫn là hốc tối, bậc thềm chạm đất đúng pivot. Ban đêm cổng sáng hơn cảnh xung quanh khá nhiều (xem debt bên dưới). |
| `summon_modal_land_800` / `summon_modal_port_360` | Modal ivory đọc tốt: tiêu đề, resource pill, ritual card, CTA xanh trầm, danh sách anh hùng đều có contrast thật; tag/callback giữ nguyên. |
| `summon_result_land_800` | Card kết quả ivory, chữ nâu/lam đọc được; nội dung dài vẫn bị cắt ở đáy khung như BEFORE (debt layout có sẵn). |
| `hud_land_800` | Không đổi so với BEFORE — HUD chưa được làm trong slice này. |

## Lỗi tìm thấy trong chính AFTER và cách xử lý
1. **Sàn ivory tràn qua rìa đảo.** Lần AFTER đầu tiên vẽ ellipse ivory đục trên nền đá; pixel ivory xuất hiện ở vùng trước đó là vách đảo/void (`y≈680–715`, ví dụ `(400,700)` BEFORE `(54,65,60)` → AFTER `(121,127,131)`). Đã thêm clip theo `SanctuaryIslandGeometry.POLYGON_VERTICES`.
2. **Sàn ivory phủ lên apron/steps/grass của chính sprite Hall** và viền ellipse cắt ngang mặt đá đã vẽ sẵn trong sprite → đọc như decal dán. Đã thử thu nhỏ (0.90 → 0.62) và bỏ stroke: vẫn còn mép ellipse lạ.
3. **Thử pave theo tile 64 px** (đúng vocabulary plaza): vẫn là một mảng dán lên cỏ vì sprite Hall **đã nướng sẵn apron đá của nó**.
4. Kết luận: bỏ hẳn lớp ground ivory trong `drawSanctuaryZoneFloorRing`. Family `summon_ivory` vẫn được đóng gói + validate (82/82) nhưng **không vẽ**, vì vẽ thêm chỉ nhân đôi bề mặt. Regression test `zoneFloorRingStaysGroundNeutral` chặn việc âm thầm thêm lại một lớp đục.

## Art debt còn lại (không tuyên bố đạt)
- **Chu kỳ 4×4** thấy được ở cỏ/flagstone khi nhìn lâu (đặc biệt plaza z1.0). Cần thêm biến thể hoặc phá chu kỳ ở pass sau.
- **Night response**: sprite ivory sáng hơn hẳn bối cảnh (gate noon→night chỉ giảm ~19%, trong khi cỏ giảm ~17% nhưng nền tối hơn nhiều). Chưa có light-response riêng cho landmark; cần pass `drawWorldGrade`/local light cho sprite mới.
- **Nhà ở/props** vẫn art P03/`premium_slice` → tương phản giữa nhà xám và nền sáng mới lớn hơn BEFORE. Slice buildings/props chưa làm.
- **HUD/auth/loading** chưa đổi; `hud_land_800` giống BEFORE.
- **Modal result** vẫn cắt nội dung ở đáy khung hẹp — layout debt có từ trước, chưa sửa.

## Chấm theo 10 tiêu chí (0–5, chỉ dựa trên ảnh engine ở trên)
| Tiêu chí | Điểm | Ghi chú |
|---|---:|---|
| Material readability (grass/soil/stone/road) | 4 | Rõ hơn BEFORE rõ rệt, trừ chu kỳ lặp |
| Seamless ground | 3 | Không thấy seam ở path/plaza; còn chu kỳ 4 ô |
| Summoning Hall volume/light | 4 | Mái–mặt trước–mặt hông rõ, ban đêm giữ khối |
| Ivory identity (world + modal) | 4 | Modal và sprite đạt; sàn ground bị bỏ có chủ đích |
| Tower Gate structure/rift | 4 | Hốc tối + bậc thềm đúng; ban đêm hơi sáng |
| Palette cohesion | 3 | Nền mới sáng hơn làm nhà P03 lệch tông |
| Silhouette/pivot/foot contact | 5 | Không lệch chân, không đổi collider |
| UI contrast/legibility | 4 | Modal ivory đọc tốt ở 2 viewport |
| UI consistency với các modal khác | 3 | Chỉ Summon ivory; modal khác vẫn tối |
| Premium feel tổng thể | 3 | Slice tốt nhưng scene chưa đồng bộ art |

Quality loop 6 chiều: mới có bằng chứng cho *environment materials*, *landmark structure*, *UI summon*; **thiếu** *buildings/props*, *HUD*, *auth/loading* → chưa đủ để chấm tổng ≥20/24.

## Trạng thái
`GAME_CANDIDATE` cho 5 family tile + 2 structure + modal ivory. **Không READY.** Chưa có build/APK trên thiết bị thật, chưa FPS, chưa sign-off mỹ thuật.
