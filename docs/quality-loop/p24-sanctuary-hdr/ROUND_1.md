# ROUND 1 — BASELINE → IMPLEMENT → EVIDENCE → CRITIQUE (FAIL)

- **SHA ứng viên**: `09f33e18546f22ac4bf8ec3e6f025d7a74ea619f`
- **CI thật**: `https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41586525855` (`Build Eternal Summoner APK`, `completed/success`) · `https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41586525853` (`Lyra engine render evidence`, `completed/success`).
- **Bot evidence commit**: `dcb0bd552be8906d36cae50ecb9fca9965488b32` — 116 file, 24 PNG BEFORE snapshot + 24 PNG AFTER + 8 PNG `hdr_*`.
- **PR**: #30 `https://github.com/lightingfff/eternal-sunmmoner/pull/30` (OPEN, chưa merge).
- Nhãn bằng chứng: `REAL ENGINE ROBOLECTRIC RENDER`. Local build/test `NOT RUN` (không JDK/Gradle/SDK). `PHYSICAL DEVICE`/GPU/nhiệt/FPS `NOT TESTED`.

## Evidence thật

- `docs/visual-evidence/p24/engine-render-before/` = 24 PNG, tổng **7 356 408 B**, đúng bằng tổng 24 PNG P23 đã đo ở baseline → snapshot đúng ảnh P23 đã merge, không phải ảnh P22.
- `docs/visual-evidence/p23/engine-render/` = **32 PNG** (24 khung + 8 `hdr_*`).
- `docs/visual-evidence/p23/camera_props.json` **không đổi** so với `main` (`git diff main -- ...` rỗng) → trước/sau cùng camera `0.36 / 0.88 / 1.00 / 1.48`.
- Đã mở trực tiếp `service_cluster_z100.png` (BEFORE), `service_cluster_z100.png` (AFTER), `hdr_balanced_night_z088.png`.

## Số đo (script `measure_luminance.py`, Pillow trên PNG thật)

| khung | mean B→A | Δmean% | p90−p10 B→A | Δspread |
| --- | --- | --- | --- | --- |
| `service_cluster_z088` | 62.94 → 63.40 | **+0.73%** | 53.1 → 52.1 | **−1.0** |
| `tower_gate_whole_z148` | 58.71 → 59.52 | **+1.38%** | 51.2 → 49.2 | **−1.9** |
| `sanctuary_overview_z036` | 70.59 → 70.65 | **+0.09%** | 57.6 → 57.6 | **+0.0** |

Khung HDR: `hdr_off_z088` mean 62.83 / spread 53.1; `hdr_reduced_z088` 63.01 / 53.1; `hdr_balanced_day_z088` 63.40 / 52.1; `hdr_balanced_night_z088` 58.62 / **60.4**; `hdr_balanced_night_rain_z088` 59.18 / **60.9**.

## Critique (KHÔNG PASS)

| Gate | Kết quả |
| --- | --- |
| B1 CI xanh | PASS (2 run `success`) |
| B2 24 khung + 8 HDR cùng camera | PASS |
| B3 không flood sáng | PASS 3/3 (`\|Δmean\| < 8%`) |
| B3 tương phản cục bộ tăng ≥3 trên ≥2/3 | **FAIL 0/3** (−1.0 / −1.9 / +0.0) |
| B4 hợp đồng | PASS (contract + environment tests xanh) |
| B5 OFF/REDUCED ⊂ BALANCED | PASS (số đo + soi ảnh) |

**Soi ảnh thật**: AFTER `service_cluster_z100` chỉ có vài quầng vàng rất nhạt quanh cột đèn; mái slate, tường xám, mặt bên vẫn **đồng giá trị**. `hdr_balanced_night_z088` có vũng sáng rõ hơn nhưng công trình vẫn phẳng. Đây đúng là lỗi người dùng đã cảnh báo: **"có glow nhưng vẫn phẳng"**. Không tự nhận PASS dù CI xanh.

## Root cause (3 nguyên nhân định lượng được)

1. **Suy hao alpha hai lần**: mask radial có alpha nội tại `f³`, lại bị nhân tiếp `drawImage(alpha ≈ .05–.14)`. Một stamp bán kính ~60px chỉ phủ ~1% khung 1280×720 → tác động lên percentiles toàn ảnh gần bằng 0. `p90−p10` thậm chí giảm nhẹ vì vùng tối bị nâng sáng (dark<20 giảm, ví dụ `service_cluster_z088` 0.84% → 0.57%) thay vì được đào sâu.
2. **Phản ứng vật liệu là quầng mềm vô hướng**, đặt gần tâm nhà với bán kính lớn → phủ đều lên cả mái lẫn tường, **không** tạo phân cấp mái/mặt tiền/mặt bên. Đây là "glow" chứ không phải "volume".
3. **Contact AO bị chính sprite che mất**: rim vẽ TRƯỚC sprite với bán kính `76*s`, nhỏ hơn chân nhà (~`200*s`) → nằm hoàn toàn dưới sprite, không bao giờ nhìn thấy. Thêm vào đó 24 khung evidence dùng `debugOverride(12f, CLEAR)` nên `exposureBalance()` chỉ 0.92 (ban ngày êm) — đúng lúc cần chứng minh thì hiệu ứng yếu nhất.

## Fix tối đa 3 mục, cùng vòng 2 (ROUND 2)

1. Thay quầng vật liệu bằng **grade trên chính alpha silhouette đã cache** của từng công trình (`SanctuaryEnvironmentShadows.silhouette`): một `Brush.linearGradient` chạy theo hướng key-light NW→SE, 4 stop (mái → mặt đón sáng → trung tính → mặt khuất), màu highlight khác nhau theo `STONE/WOOD/METAL/CRYSTAL`. Một `drawPath` có cull + BALANCED-only, không fullscreen, không blur, không decode/frame.
2. Chuyển contact AO ra **SAU sprite**, bán kính `124*s` trên mặt phẳng `.72` (rộng hơn chân nhà) để thực sự nhìn thấy; bỏ rim cũ.
3. Nâng năng lượng **có ngưỡng** của nguồn sáng cục bộ (`MAX_STAMP_ALPHA .22→.30`, halo `.14→.22`, ground alpha ~×1.5), đưa `exposureBalance()` ban ngày về `1.00`; đồng thời thêm **post pass thật của lobby** (`drawWorldGrade` / `drawLocalLights` / `drawWeather`) vào `HdrLightingRenderTest` để khung đêm/mưa phản ánh đúng `SanctuaryLobbyScreen`. 24 khung P23 vẫn giữ harness trần để diff sạch.

Giữ nguyên: không tăng brightness toàn cục, không fullscreen bloom/blur, không RuntimeShader/AGSL, không đổi `.72`/anchors/save/gameplay, không chạm 29 file PR #28. Vòng 2 sẽ render lại **cùng camera** và đo lại đúng 3 khung gate.
