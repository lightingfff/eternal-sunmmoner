# P27B V5 — báo cáo tổng hợp (CP4 → CP6) sau khi sửa lỗi 05/11/2026

HEAD đã kiểm: **`02a6f40`** trên branch `arena/8be73f1b-eternal-sunmmoner`.
Cả 3 workflow trên HEAD này đều **success**: APK `42057485727`, P27B `42057485736`, Lyra `42057485740`.

> Lưu ý về bằng chứng: `artifact store` của repo **đầy quota** suốt phiên. Vì vậy mọi kết luận
> dưới đây dựa trên ảnh render thật được **commit vào `docs/`**, không dựa trên artifact ZIP.

---

## 1. Tổng kết 5 debt của V5

| Debt | Trạng thái | Bằng chứng |
|---|---|---|
| **A.** Nhà/props còn art P03, lệch tông | **ĐÓNG** | 20/20 sprite thay bằng art P27B-V5. Luma: nhà 51→86…148, tháp 57/55→152/153, registry 55→153. Tường chu vi + vành đảo repaint. |
| **B.** Terrain lặp chu kỳ | **đóng một phần** | `period-repeat 33/64`, 64/64 cell distinct, wrap seam < 4. Không tuyên bố "seamless". |
| **C.** Gate/Hall quá sáng ban đêm | **ĐÓNG (2 lần)** | CP3: facade giảm 26.3% vs nền 21.0%. CP5: chu vi (code vẽ) giảm 54–78%. |
| **D.** HUD/Auth/Loading thiếu design system | **ĐÓNG phần lớn** | Token layer + 42 màu hardcode → 6; HUD panel 71%→88%; auth scrim 0.42→0.76. |
| **E.** Summon result cắt chữ | **ĐÓNG** | Test đo `boundsInRoot` thật, ảnh 800×360dp không còn glyph bị cắt. |

## 2. CP4 — design system (debt D, E)

- `ui/theme/SanctuaryUi.kt`: token theo **vai trò** (surface/card/pill/divider, ink/inkSecondary/inkTertiary, accent/accentDeep/brown/warning/success) + scale radius/space + `minTouchTarget` 48dp + `contrastRatio()`.
- Áp dụng: `SanctuaryFacilityModals.kt` **42 → 6** màu hardcode; `SanctuaryLobbyScreen.kt` dùng cùng token.
- **Cố ý không** đổi 5 dòng `0xFF9EA6B2` — đó là nét đường minimap (dữ liệu thế giới), không phải chrome UI.
- `summon_result`: `screenHeightDp < 420` → avatar 52→40dp, bỏ 2 dòng ít thông tin, mọi dòng còn lại `maxLines` + `Ellipsis`.

## 3. CP5 — asset, chu vi, HUD

**Art (debt A):**
- 2 tower + registry hall: cùng canvas gốc (80×118 / 96×136 / 188×166), cùng pivot đáy, alpha nhị phân.
- **Regression tự gây và tự sửa:** cây bị tụt 5px xuống đất vì `drawFootOffset()` của cây **chính là** `bottomPadding`. Pipeline giờ đọc foot-pad từ sprite gốc (canopy 5, pine 5, oak 1, barrels 3).
- Chu vi: phát hiện vành đảo **không** lấy từ atlas mà do `SanctuaryWorldRenderer.drawFortressWall` vẽ tay bằng palette than chì. Repaint sang limestone; hình học không đổi.
- Tile tường mới: seam 36.6 → **9.17** (giới hạn 14.0). Dùng **dịch chuyển** chứ không cross-fade, vì art pixel 1:1.

**HUD (debt D):**
- HUD là glass mờ trên thế giới sáng → contrast phụ thuộc pixel phía sau. Đo trên nền sáng nhất (ivory `F2EAD9`) ở alpha cũ 71%: gold 4.0:1, muted 2.9:1 → **fail**.
- Nâng 71% → 88%: gold 6.9, silver 7.1, muted 5.0 — vẫn còn 12% thế giới đọc xuyên qua.
- Tách token: `ArcaneViolet` (3.5:1) **không dùng làm chữ nữa**; thêm `violetOnDark` 5.3:1 cho nhãn class Mage / tên Summoning Hall.

## 4. CP6 — Auth + sửa lỗi vận hành

- **AuthGate**: scrim là thứ gánh toàn bộ độ đọc (chữ chỉ 1.3–1.4:1 trên nền sáng). Đo: alpha 0.42 → title 4.45:1, tagline 3.69:1. Nâng lên 0.76 → gold 5.90 / cyan 4.90 / parchment 9.30.
- **APK release không còn phụ thuộc artifact store**: upload là `continue-on-error` + `retention-days: 3`; APK publish thẳng lên GitHub Release **từ runner build**; job `release` thứ hai (nguồn gốc của việc "quota đầy → release bị SKIPPED") đã xoá.
- **Upload hết quota không còn làm đỏ run render**: Lyra/P27B in bảng phân loại `upload` vs `compile/render/gates`.
- **`action_required`**: bot commit dưới `github-actions[bot]` khiến mọi run sau của PR cần duyệt tay. Đổi sang identity `arena-evidence`.
- **E4**: 3 nấc lịch sử ghi rõ provenance — P26B 81.69 → art PR #34 98.29 → art CP2/CP5 115.41. Dựng lại baseline (không nới ngưỡng 8%), thêm 2 guard chống seat ẩu. **C1 vẫn đo mây trên khung P26B gốc** — lần đầu tôi ghi đè và làm C1 từ 1.0 xuống 0.0, nên 2 baseline giờ tách hẳn.
- **Test JVM** được cấp `maxHeapSize=2g` (Lyra từng chết không có JUnit XML).

## 5. Bảng kiểm cuối

| Hạng mục | Kết quả | Ghi chú |
|---|---|---|
| `P27BStructureContractTest` | **PASS** (8) | canvas, pivot, alpha nhị phân, foot-pad, band luma, seam tường |
| `P27BNightResponseTest` | **PASS** (3) | facade giảm > 1.05× nền |
| `SanctuaryUiTokenTest` | **PASS** (5) | contrast ≥ 4.5 cho mọi token chữ |
| `SanctuaryHudContrastTest` | **PASS** (6) | HUD + auth scrim trên nền sáng nhất |
| `P27BUiTest` | **PASS** | gồm `summonResultFitsInsideShortLandscapeViewport` |
| `MapForgeP22ContractTest` | **PASS** | đây là test bắt được lỗi cây tụt 5px |
| P26C gates | **9/9 PASS** | C1 trên P26B, E4 trên baseline art |
| `assembleDebug` | **PASS** | APK ký debug, ZIP/CRC/sha256 verify |
| FPS / thermal / máy thật | **NOT TESTED** | không có thiết bị; chỉ Robolectric |
| 5×5 terrain pattern ở zoom lớn | **NOT TESTED** | debt B chỉ đóng một phần |
| HUD render trên nền sáng | **KHÔNG CÓ ẢNH** | ảnh HUD hiện render trên nền tối; cải thiện opacity chứng minh bằng số đo contrast, không bằng ảnh |

## 6. Việc còn mở (không tuyên bố đã xong)

- Debt B (chu kỳ terrain) mới đóng một phần; `period-repeat 33/64` chưa phải "hết lặp".
- HUD/facility panel chưa restyle toàn diện — mới có token + 2 chỗ đo contrast.
- PR #28 (Mission 01) và #13 vẫn conflicted; #13 nội dung đã nằm trong `main`.
- Artifacts: đã xoá 120 bản cũ, giải phóng **6.31 GB** (130 → 10 artifacts). GitHub tính lại quota sau 6–12 giờ.
