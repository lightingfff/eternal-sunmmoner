# ROUND 3 — FIX → RETEST → CRITIQUE (mean đạt, tương phản chưa đạt)

- **SHA ứng viên**: `546e6a5103112676006868b5c3dc160a5c15a677`
- **CI thật**: `https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41588829278` (`Build Eternal Summoner APK`, `success`) · `https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41588829263` (`Lyra engine render evidence`, `success`). Bot evidence `97063b8b1df253c211bbda501c86c27425cc2eae` (84 PNG). PR #30 OPEN.
- Đã soi trực tiếp `tower_gate_whole_z148.png`, `service_cluster_z100.png` (ROUND 3).

## Số đo

| khung gate | mean B→A | Δmean% | p90−p10 B→A | Δspread |
| --- | --- | --- | --- | --- |
| `service_cluster_z088` | 62.94 → 62.92 | −0.03% (PASS) | 53.1 → 52.6 | **−0.4 (FAIL)** |
| `tower_gate_whole_z148` | 58.71 → 59.80 | +1.87% (PASS) | 51.2 → 54.8 | **+3.7 (PASS)** |
| `sanctuary_overview_z036` | 70.59 → 70.66 | +0.09% (PASS) | 57.6 → 57.6 | **+0.0 (FAIL)** |

`no-flood 3/3 PASS`, `contrast-gain 1/3 FAIL`. Toàn bộ 24 khung: `|Δmean|` lớn nhất chỉ **2.02%** (`summoning_hall_whole_z148`), tức đã chữa được lỗi washed của ROUND 2. `tower_gate_whole_z148` soi ảnh: tháp phải sâu, tháp trái đón sáng, móng tối, hốc rift vẫn đen, vũng cyan dưới chân — **đẹp và đúng dark-fantasy, không wash, không mất nét pixel**.

## Critique trung thực

- **Đã đạt**: không tăng brightness toàn cục (mọi khung <2.1%), không blur/burn, Tower Gate tách khối rõ (+3.7 spread).
- **Chưa đạt**: `service_cluster_z088/z100` soi ảnh vẫn **khá phẳng** — chỉ có vài quầng vàng nhỏ quanh cột đèn; mái/mặt tiền/mặt bên chưa tách đủ, cửa sổ vẫn là ô sáng không có spill lên đá. Đây vẫn là dạng "có glow nhưng vẫn phẳng" ở zoom chơi 0.88/1.00 mà người dùng yêu cầu khắc phục. `sanctuary_overview_z036` Δspread 0.0. Không tự nhận PASS.

## Root cause

Tone curve trung hoà mean đã **cắt quá nhiều phía highlight** (`.34→.16`), nên p90 không nhích lên; phần shade thì tối đều theo không gian chứ không theo độ sáng, kéo cả p90 lẫn p10 xuống → spread gần như đứng yên. Ngoài ra grade vẫn là một dốc mềm duy nhất: thiếu một **mặt bên (return wall) tách rõ** và thiếu **ánh sáng ấm thực sự bám lên đá quanh cửa sổ**.

## Vòng sửa CUỐI (correction #3)

Làm rõ ngân sách: ROUND 1 là chu trình *triển khai ban đầu*; ROUND 2 = correction #1, ROUND 3 = correction #2, ROUND 4 = **correction #3 và là vòng cuối** đúng yêu cầu "tối đa 3 vòng sửa". Ba sửa tập trung:

1. **Mặt bên tách rõ**: vẽ thêm một `Brush.linearGradient` ngang trên **cùng silhouette đã cache** (2 `drawPath`/nhà, không bitmap/frame) để 1/3 bên phải đọc là return wall tối; đồng thời khuếch đại nhẹ cả hai phía của dốc dọc (`highlight .16→.20`, `shade .36→.42`, giữ tỉ lệ ~2× nghiêng tối) để mean vẫn trung hoà mà p90/p10 giãn ra.
2. **Spill ấm bám lên đá**: halo cửa sổ từ `radius 18/.10` → `40/.17` (đặt ở độ cao cửa sổ, tức đổ lên tường), vũng nền `54/.115` → `62/.150`.
3. **Tiêu điểm quảng trường** rõ hơn ở 0.36: `96/.105` → `120/.160` (vẫn là 1 stamp cache, không fullscreen).

Nếu sau ROUND 4 gate B3 vẫn <2/3 → `BLOCKED` trung thực, giữ PR OPEN để owner quyết, không tự PASS, không sửa vòng thứ 4.
