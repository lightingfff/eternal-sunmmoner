# ROUND 2 — FIX → RETEST → CRITIQUE (vẫn FAIL 2 gate)

- **SHA ứng viên**: `ddccd7762469dc376eef0ff00ed00f9d0ec7823e`
- **CI thật**: `https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41587811048` (`Build Eternal Summoner APK`, `success`) · `https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41587811026` (`Lyra engine render evidence`, `success`).
- **Bot evidence**: `d9b7de7822f57fa2c1142929ca8bc573e65af724` (93 PNG cập nhật). BEFORE vẫn là `p24/engine-render-before/` 24 PNG / 7 356 408 B, `camera_props.json` không đổi. PR #30 vẫn OPEN.
- Đã soi trực tiếp: `service_cluster_z100.png`, `tower_gate_whole_z148.png`, `hdr_balanced_night_z088.png`.

## Kết quả đo (cùng script, cùng BEFORE)

| khung gate | mean B→A | Δmean% | p90−p10 B→A | Δspread |
| --- | --- | --- | --- | --- |
| `service_cluster_z088` | 62.94 → 65.78 | +4.51% (PASS) | 53.1 → 54.8 | +1.7 (FAIL) |
| `tower_gate_whole_z148` | 58.71 → 64.67 | **+10.16% (FAIL)** | 51.2 → 61.3 | **+10.1 (PASS)** |
| `sanctuary_overview_z036` | 70.59 → 70.66 | +0.10% (PASS) | 57.6 → 57.6 | +0.0 (FAIL) |

Kết quả khác (thật): `armory_full_z148` +5.2 spread / +9.79% mean, `summoning_hall_whole_z148` +6.2 / +4.86%, `restaurant_door_actor_z148` +5.4 / +5.43%, `synthesis_cottage_z148` +5.7 / +7.22%, `quarters_door_actor_z148` +3.8 / +9.83%. Khung `hdr_balanced_night_z088` giờ có `drawWorldGrade` thật (bầu trời lạnh, nền tối) — đã vá gap harness ghi nhận ở audit.

## Critique

**Tốt lên rõ rệt so với ROUND 1**: 0/3 → 1/3 khung đạt tương phản; 11/24 khung tăng spread ≥3. Soi `tower_gate_whole_z148`: tháp trái sáng, tháp phải sâu, hốc rift vẫn tối, vũng cyan dưới chân cổng đọc được → đã có volume, không còn sticker phẳng. `hdr_balanced_night_z088` thể hiện đúng pipeline lobby.

**Vẫn FAIL, không tự nhận PASS**:
1. **Washed / sáng lên quá**: `tower_gate_whole_z148` +10.16% mean vượt ngưỡng <8%; `armory_*` +9.8%. Soi ảnh: mặt đá tháp trái bị nhợt, đúng lỗi "washed" người dùng cấm. Nguyên nhân: gradient 4 stop nghiêng hẳn về phía sáng (2 stop sáng trên 58% chiều gradient, chỉ 1 stop tối ở 42% cuối) → kỹ thuật blend về gần trắng đẩy luminance xa hơn nhiều so với blend về navy.
2. **`sanctuary_overview_z036` Δspread = 0.0**: `drawVolumeGrade` bị chặn bởi `s < .45f`, mà ở zoom 0.36 thì `s ≈ .36` → không có grade nào trên khung tổng thể.
3. `service_cluster_z088` mới +1.7 (<3).

## Root cause định lượng

- Bất đối xứng vật lý của alpha blending: với nền ~70 luminance, `alpha .24` về trắng cho `+.24*185 ≈ +44`, còn `alpha .30` về navy (~14) chỉ cho `-.30*56 ≈ −17`. Muốn mean trung hoà mà vẫn tăng tương phản, **alpha phía tối phải lớn gấp ~2–3 lần phía sáng**.
- Ngưỡng `s < .45f` quá cao cho zoom tổng thể 0.36.
- Gradient cũ chạy chủ yếu theo trục ngang (`start=-w/2 → end=+w/2`) nên tách trái/phải tốt nhưng **chưa tách mái → mặt tiền → móng** theo chiều đứng như yêu cầu.

## ROUND 3 (vòng sửa cuối, đúng ngân sách 3 vòng)

1. **Tone curve trung hoà mean, mở tương phản**: highlight `.34→.16` (và key `.24→.11`), shade `.30→.36` với hệ số `key*2.15` (≈3× highlight), stop trong suốt dời `.58→.44` để nửa tối chiếm đa số diện tích. Kỳ vọng mean về gần 0 trong khi p90 tăng (mái) và p10 giảm (mặt khuất) → spread tăng.
2. **Gradient lệch dọc**: `start=(-.30w, H)` → `end=(+.30w, 0)` → mái sáng, tường giữa, móng tối, vẫn giữ nghiêng NW→SE cho mặt bên.
3. **Bật grade ở 0.36**: ngưỡng `.45→.30`, thêm `zoomFactor=(s/.88).coerceIn(.55,1.10)` để khung tổng thể nhận hiệu ứng nhẹ, cận cảnh 1.48 nhận nhỉnh hơn, không đổi cường độ đột ngột.
4. Giảm nhẹ vũng sáng Tower Gate (`ground .195→.160`, `halo .20→.17`) để kiểm soát mean mà vẫn giữ nguồn lạnh mạnh.

Không đổi hợp đồng, không thêm hiệu ứng fullscreen, không chạm file PR #28. Sau ROUND 3: nếu vẫn còn blocker → báo `BLOCKED` trung thực kèm số đo, giữ PR OPEN/draft, không tự PASS.
