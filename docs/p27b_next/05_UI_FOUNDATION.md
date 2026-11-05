# CHECKPOINT 4 — design system + summon result card (debt D, E)

Trạng thái: **đã commit + màn hình Compose thật + test.** Ảnh UI: `docs/p27b_next/previews/after/ui/` (bot commit `8d9f8c7`).

## 1. Debt E — thẻ kết quả triệu hồi bị cắt
**Tái hiện được** trên ảnh thật 800×360dp: thẻ chạy tới sát đáy cửa sổ, dòng quote «…» bị cắt giữa chữ. Nguyên nhân: thẻ là item của `LazyColumn`, **không có ràng buộc chiều cao**, mọi dòng chữ không giới hạn số dòng.

Đã sửa (cục bộ, không đổi bố cục khác):
- thẻ đọc `LocalConfiguration.screenHeightDp`; cửa sổ **< 420dp** → avatar 52dp → 40dp;
- hai dòng ít thông tin nhất (gợi ý "đã lưu vào anh hùng", quote tính cách) **ẩn hẳn** thay vì bị cắt;
- mọi dòng còn lại có `maxLines` + `Ellipsis` → kiểu hỏng tệ nhất là **một dòng bị rút gọn**, không bao giờ là chữ tràn ra ngoài thẻ.

Kết quả: ảnh sau sửa cho thấy thẻ kết thúc gọn trong khung, không còn glyph bị cắt.

**Test:** `summonResultFitsInsideShortLandscapeViewport` lấy **bounds thật** của node `summon_result` và so với kích thước cửa sổ. Kiểm tra "tag tồn tại" không bắt được lỗi này (LazyColumn vẫn báo node tồn tại dù vẽ tràn) — đây chính là lý do lỗi sống sót qua P27B.

## 2. Debt D — design system
Trước CP4 **không có tầng token nào**: palette ivory được gõ tay **42 lần** trong `SanctuaryFacilityModals.kt` và 15 lần trong `SanctuaryLobbyScreen.kt`, với 3 màu card gần giống nhau và không có quy tắc dùng màu nào.

`ui/theme/SanctuaryUi.kt` (mới) đặt tên theo **vai trò**, không theo giá trị:
| Nhóm | Token |
|---|---|
| Surface | `surface`, `card`, `cardPressed`, `pill`, `divider` |
| Stroke | `gold` (trang trí), `goldSoft` |
| Ink | `ink`, `inkSecondary`, `inkTertiary` |
| Accent | `accent`, `accentDeep`, `brown`, `warning`, `success`, `onAccent` |
| Scale | `radiusSmall/Medium/Large`, `spaceXs..Lg`, `minTouchTarget = 48dp` |

Đã áp dụng: **42 → 6** màu hardcode trong facility modals (6 còn lại là accent một lần và màu disabled), lobby screen dùng cùng token.
**Cố ý không** chuyển 5 dòng `0xFF9EA6B2` — đó là nét đường đi trên minimap (dữ liệu thế giới), không phải chrome UI.

### Contrast đo bằng số, không phải cảm nhận
`SanctuaryUiTokenTest` tính tỉ lệ WCAG thật:

| Token | trên `card` | trên `surface` |
|---|---|---|
| ink | 11.37 | 10.24 |
| inkSecondary | 6.05 | 5.45 |
| accent | 5.30 | 4.78 |
| accentDeep | 5.53 | 4.99 |
| brown | 6.55 | 5.91 |
| warning | 5.44 | 4.90 |
| success | 5.89 | 5.31 |
| **inkTertiary** | **4.24** | **3.82** |
| **gold** | **2.33** | **2.10** |

Test **fail nếu bất kỳ token chữ nào < 4.5**. `inkTertiary` (4.24) và `gold` (2.33) được **document rõ** là ngoại lệ có chủ ý: hint không quan trọng và đường kẻ trang trí — và test cũng khẳng định `gold` **không được** chở chữ (nhưng `ink` trên nền gold thì vẫn ≥ 4.5).

Thêm: test kiểm tra thứ tự độ sáng các lớp surface (pill < cardPressed < surface < card) để card "nổi" vẫn thật sự nổi, và các scale phải đơn điệu tăng.

## 3. Chưa làm (CP5–CP6)
- HUD trên cao (top bar, resource chips, command panel), panel anh hùng, panel facility — mới có token, **chưa restyle**.
- AuthGate, Loading — chưa chạm.
- `.github/workflows/p27b-render.yml` giờ copy cả ảnh UI sang `docs/p27b_next/previews/after/ui/` (trước đó bot chỉ commit ảnh world).

## 4. NOT TESTED
FPS/thermal/máy thật; chưa test trên màn hình có tai thỏ/notch thật, chỉ Robolectric qualifier.
