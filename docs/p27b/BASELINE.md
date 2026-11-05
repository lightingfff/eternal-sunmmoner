# P27B / baseline trước khi tích hợp asset

- Baseline HEAD `bd68697445d4a5511cccfc2060689fba99ee6f4f` (`main` lúc tạo branch Arena), nhánh `arena/683a732b-eternal-sunmmoner`. Working tree có untracked V2/V3 **không đưa vào Android runtime**. Baseline commit kế tiếp chỉ có tài liệu/render harness, không art/visual code.
- Local **không có JDK/Gradle/Android SDK** → không nói local Android render/build PASS. Chạy CI Android/Robolectric native graphics `P27BRenderTest` + `P27BUiTest` trên SHA pre-art; lưu screenshot và `camera_props.json` do CI **commit về chính nhánh** tại `docs/p27b/previews/before/`. Sau art, chạy *chính cùng test/hàm* trên SHA candidate; lưu `after/`. Nếu CI không chạy, ghi `BEFORE/AFTER_NOT_VERIFIED`.
- CI thử nghiệm #41981966373 trên `8b3988c`: Kotlin compile PASS nhưng UI dialog transition FAIL; artifact storage quota hết. Sửa clock test/đổi sang commit PNG vào nhánh, provisioning **debug keystore tạm chỉ trong CI**. CI baseline cuối **PASS**: [run #41982824376](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/41982824376) trên `5a2876e`, bot commit ảnh before `e8c2e51`. 7 ảnh world + `camera_props.json` và 4 ảnh UI ở `docs/p27b/previews/before/{world,ui}` đã mở/xem. APK `assembleDebug` thành công với chữ ký debug CI tạm, KHÔNG có APK artifact do quota, KHÔNG xác nhận release signature hay hiệu năng thiết bị. Đây là baseline từ renderer thật, không phải concept.
- Renderer path theo P26C: `SanctuaryLobbyEngine().mapLayout`, `SanctuaryWorldRenderer.drawSanctuaryFloatingWorld`, `SanctuaryEnvironmentShadows.drawGroundShadows`, Y-sorted `PixelSpriteRenderer.drawProp`, `LivingWorldRenderer.drawWorldGrade/drawLocalLights/drawWeather`. World environment `WorldEnvironment.debugOverride(hour, weather)`, quality `DepthPresentation.Quality.BALANCED`, 1280×720 px, Density(1), `timeSec=0`, seed cố định theo renderer. Đây là **Robolectric Skia/native graphics**, KHÔNG phải ảnh trên máy thật/APK runtime và không phải concept.
- UI: Compose `createComposeRule`/View.draw, 800×360dp landscape (xhdpi) và 360×640dp portrait (xhdpi); riêng UI không có camera/time/weather trực tiếp ngoài lobby world; nếu test không khóa world hour/weather thì ghi N/A, không ghép chung với world screenshot.

| Screenshot pair (test/harness dự kiến) | camX | camY | zoom | hour | weather | Kiểm |
|---|---:|---:|---:|---:|---|---|
| `overview_noon_z036` | 1600 | 1380 | .36 | 12 | CLEAR | island silhouette, ground block 4×4, roof tone |
| `service_noon_z088` | 960 | 860 | .88 | 12 | CLEAR | lawn, road junction/buildings; per-tile path |
| `plaza_noon_z100` | 1940 | 1240 | 1.00 | 12 | CLEAR | curved border/flagstone/runes |
| `summon_noon_z148` | 2430 | 1715 | 1.48 | 12 | CLEAR | roof/front/side + zone/building contact |
| `summon_night_z148` | 2430 | 1715 | 1.48 | 0 | CLEAR | ivory vs ambient lighting |
| `gate_noon_z148` | 1960 | 310 | 1.48 | 12 | CLEAR | opening/steps/contact with renderer rift |
| `gate_night_z148` | 1960 | 310 | 1.48 | 0 | CLEAR | portal remains dark void, stone reads |
| `summon_modal_land_800` | UI | UI | N/A | 12 if override | CLEAR | Compose art/contrast/CTA/tags |
| `hud_land_800` + `hud_port_360` | Lobby UI | Lobby UI | default | 12 if override | CLEAR | HUD bounds/touch/readability |

**Ảnh đã commit tại repo, chưa phải P27B pair:** `docs/visual-evidence/p26c/engine-render/p26c_noon_clear_z088.png`, `p26c_gate_noon_clear_z148.png`, `p26c_overview_noon_z036.png` đều 1280×720, metadata `docs/visual-evidence/p26c/camera_props.json` là 12/CLEAR/BALANCED, time 0, nhưng tạo ở SHA P26C cũ; P23 `summoning_hall_whole_z148.png` 1280×720 có camera (2430,1715,1.48) và time 0, **không ghi rõ hour/weather**. Không gán nhãn chúng là P27B before cùng SHA nếu chưa rerender; có thể dùng để đối chiếu lịch sử trong audit.

- **AFTER (cùng harness)**: CI [run #42017255236](https://github.com/lightingfff/eternal-sunmmoner/actions/runs/42017255236) trên `4baa7d1`, bot commit ảnh `c6f5adc` vào `docs/p27b/previews/after/`. Cùng bước validator (82/82 PASS), `P27BAssetContractTest`, `SanctuaryMaterialTest`, world renderer, Compose UI, `assembleDebug` — **tất cả PASS**. Ảnh AFTER cùng 1280×720/camera/zoom/timeSec/hour/weather với BEFORE; `camera_props.json` ghi `source_sha` của từng ảnh. Đánh giá chi tiết ở `docs/p27b/03_AFTER_REVIEW.md`.
- Ba vòng AFTER trung gian (`0966144`, `5f27cd7`) bị loại vì lỗi do chính slice gây ra (sàn ivory tràn rìa đảo, rồi phủ lên apron/steps của sprite). Bản cuối không vẽ lớp ground ivory; xem lý do trong `03_AFTER_REVIEW.md`.

Đã xem P27B BEFORE `summon_noon_z148`, `overview_noon_z036`, `summon_modal_land_800`: Hall đang xám than so với nền đá sáng, zone triệu hồi chưa có ivory riêng; grass overview tối/rất đều; modal tối slate với card tím, CTA ít tương phản. Đây là nhận xét thị giác chủ quan từ screenshot thật. `gate_noon_z148`, `service_noon_z088`, `plaza_noon_z100` cần đối chiếu riêng khi có AFTER. P26C/P23 chỉ là tham khảo lịch sử, không bị tráo thành P27B BEFORE.
