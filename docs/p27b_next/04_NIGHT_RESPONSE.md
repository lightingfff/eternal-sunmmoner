# CHECKPOINT 3 — landmark phản ứng với ánh sáng đêm (debt C)

Trạng thái: **đã commit + render thật + test đo được.** Ảnh: `docs/p27b_next/previews/after/world/` (bot commit `18d1a1d`, run `42035221819` PASS).

## 1. Lỗi, đo trước khi sửa
`drawWorldGrade()` (`LivingWorldRenderer.kt:27-35`) phủ **một lớp navy phẳng lên toàn khung hình**:
```
drawRect(ambient  α=(1-ambient)*.55)
drawRect(0xFF101D33 α=(1-ambient)*.42)
```
Vì phẳng, đá ivory sáng và cỏ tối bị giảm **cùng một lượng**. Đo trên ảnh thật:

| | noon | night (trước CP3) | giảm |
|---|---|---|---|
| facade Gate | 171.9 | **137.9** | 19.8% |
| nền cỏ/paving cùng khung | 102.3 | 80.9 | 21.0% |

Toà nhà giảm **ít hơn** nền → đúng cảm giác "ảnh dán lên cảnh đêm". Đây là debt C, và nó là lỗi quan hệ, không phải lỗi "chưa đủ tối".

## 2. Đã làm
`HdrLightResponse.drawNightAbsorption()`:
- một `drawPath`/toà nhà qua **silhouette đã cache** (`SanctuaryEnvironmentShadows.silhouette`), đúng transform mà volume grade dùng;
- alpha = `night × (0.16 + 0.26 × độ_sáng_của_sprite)`, trong đó độ sáng lấy từ **mean luma của chính sprite** (cache theo bitmap, không đọc pixel mỗi frame) → đá ivory mất nhiều hơn mái slate tối;
- chạy **sau** volume grade nhưng **trước** stamp cửa sổ phát sáng, nên tường tối đi còn cửa sổ giữ nguyên độ sáng;
- chỉ BALANCED, chỉ khi `s ≥ 0.30`, `night > 0.02`, có cull theo màn hình. Không fullscreen pass, không bitmap per-frame.

## 3. Kết quả đo sau khi sửa
| | noon | night (sau CP3) | giảm |
|---|---|---|---|
| facade Gate | 171.9 | **126.7** | **26.3%** |
| nền cùng khung | 102.3 | 80.9 | 21.0% |

Quan hệ đã **đảo đúng chiều**: đá sáng mất nhiều ánh trăng hơn cỏ (26.3% > 21.0%), thay vì ít hơn. Facade tối đi 11.2 luma so với trước CP3.

## 4. Test mới (`P27BNightResponseTest`)
| Kiểm tra | Nội dung |
|---|---|
| `landmarkFacadeAnswersNightMoreThanTheGroundDoes` | render thật noon vs midnight cùng camera; **FAIL nếu facade giảm < 1.05× mức giảm của nền** — đúng lỗi cũ |
| `absorptionScalesWithSpriteBrightness` | Gate (đá ivory) phải sáng hơn một toà nhà slate+kem; nếu không thì absorption sẽ là hằng số |
| `lumaLookupIsCachedPerBitmap` | luma ổn định giữa các lần gọi → không đọc pixel lại |

## 5. Giới hạn — nói rõ
- Absorption hiện là **một lớp tối theo silhouette**, chưa phải shading theo pháp tuyến: mặt hướng trăng và mặt khuất chưa khác nhau về độ tối do CP3 (phần đó vẫn do `drawVolumeGrade` cũ lo). Đây là bước đúng hướng nhưng chưa phải "moonlight chính xác".
- Chỉ áp cho `isBuilding(...)`; prop nhỏ (lantern, bench) vẫn chỉ có grade toàn cục.
- Ngưỡng test đặt ở 1.05× — đo được 1.25×, nên còn biên an toàn; nếu sau này art tối hơn thì ngưỡng này sẽ phải xem lại **có ghi lý do**, không nới lặng lẽ.

## 6. NOT TESTED
FPS/thermal/máy thật.
