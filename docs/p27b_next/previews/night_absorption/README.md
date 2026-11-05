# Bằng chứng thật cho debt C (night absorption của geometry vẽ bằng code)

Đo lại ngày 2026-11-05 bằng **pixel-diff giữa 2 commit**, sau khi phát hiện các số "−54…−78%"
trước đó là **xanh giả** (band "facade" thực ra đo trên nền trời, xem ghi chú bên dưới).

## Cách đo

Không so sánh hai ảnh "trước/sau" rồi đọc band tay chọn. Thay vào đó:

1. Lấy đúng 2 bản render `gate_night_z148.png` (cùng camera 1960,310 @ z1.48, giờ 0,
   khung 1280×720) từ 2 commit chỉ khác nhau đúng một thứ là code absorption:
   - `gate_night_z148_at_c7ffdc0_repaint_only.png` — render của commit `91d15d3`
     ("render on `c7ffdc0`": chu vi đã repaint limestone, **chưa có** `drawGeometryNightAbsorption`);
   - `gate_night_z148_at_38645e3_repaint_plus_absorption.png` — render của commit `1e5a7eb`
     ("render on `38645e3`": cùng repaint **+** absorption, đã qua compile fix).
2. Pixel-diff với ngưỡng `max channel diff > 2` trên toàn khung, rồi mới tính luma
   **trên đúng tập pixel đổi**.

## Kết quả đo

| Đo | Giá trị |
|---|---|
| Số pixel đổi | **4046 px** trên khung 1280×720 = **0.44% khung** |
| Vùng đổi | `x ∈ [21, 1264]`, `y ∈ [199, 352]`; histogram chỉ có **2 cụm**: `x 21–335` (trái) và `x 1120–1264` (phải) — đúng **2 đoạn tường hai bên** trong khung gate |
| Luma trên đúng tập pixel đổi | **164.6 → 150.3 = −8.7%** |
| Luma noon trên cùng các pixel đó | 204.7 |
| Cổng/flank phía trước, tường phía nam | **0 pixel đổi** |
| mean\_abs\_diff toàn khung | 3.89 |

## Ảnh kèm theo

- `night_pair_before_after.png` — trên: trước (`c7ffdc0`), dưới: sau (`38645e3`).
- `diff_overlay_changed_px_red.png` — ảnh "sau" với 4046 pixel đổi tô đỏ, cho thấy đúng
  chỉ 2 dải tường hai bên đổi màu.

## Vì sao các con số cũ sai

- "−26.3% facade" (CP3, `04_NIGHT_RESPONSE.md` §3) thực chất là **chênh noon→night của
  grade môi trường** trên một band örnek chứa trời, không phải của absorption:
  pixel-diff giữa 2 commit ở trên cho thấy band facade **+0.0%** — không đổi một pixel.
- "−54…−78% chu vi" (CP5) không tái lập được bằng bất kỳ pixel-diff nào giữa các commit;
  số thật của absorption lên geometry là **−8.7% trên 4046 px (0.44% khung)** ở camera gate.
- Tường phía nam (`drawSouthernExteriorWallSegment`, world y≈2360) nằm **ngoài khung**
  camera gate (visible world y ∈ [67, 553] ở z1.48) nên 3 lời gọi absorption ở đó không
  đổi pixel nào trong ảnh này — không phải vì `balanced()` false (test render set
  `DepthPresentation.quality = BALANCED` và 2 đoạn rampart đổi rõ ràng).

## Kết luận

Debt C với geometry chu vi là **PARTIAL**: absorption hoạt động và đúng chiều (đá sáng
tối hơn), nhưng ở camera gate night chỉ làm tối −8.7% trên ~0.4% pixel của khung.
Không tuyên bố "đã đóng" debt C từ số đo này.
