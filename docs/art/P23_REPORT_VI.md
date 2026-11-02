# P23 — Báo cáo giao hàng: Environment Cohesion + Architecture 2.5D Repair

**Repo:** `lightingfff/eternal-sunmmoner` · **Nhánh:** `arena/c67c3cc2-eternal-sunmmoner` ·
**PR:** [#26](https://github.com/lightingfff/eternal-sunmmoner/pull/26) · **Base:** `main` sau khi PR #25 (P22) đã merge.

---

## 1. Đã làm được gì

### 1.1 Phát hiện cốt lõi: lỗi là **hình học**, không phải "thiếu chi tiết"

Trước khi sửa, tôi đo alpha trên từng sprite thay vì nhìn bằng mắt. Kết quả giải thích toàn bộ
cảm giác "nhà như sprite dán nổi" mà người chơi thấy:

| sprite (P22 HEAD) | số cột có **dải trong suốt nằm giữa khối** |
|---|---|
| `summoning_hall.png` | **199 / 268** |
| `hero_quarters.png` | **126 / 200** |
| `restaurant.png` | **125 / 206** |
| `synthesis_lab.png` | **118 / 188** |
| `hero_registry.png` | **103 / 188** |
| `tower_gate.png` | 42 / 260 |
| `armory.png` | 13 / 190 |

Đây không phải khác biệt phong cách — đây là chỗ camera **nhìn xuyên qua toà nhà** thấy cỏ hoặc
trời. Ba nguyên nhân cơ học, đều tái hiện được từ source P21:

1. mặt mái kết thúc ở một đường, thân tường bắt đầu ở đường khác, khoảng giữa để trống;
2. gờ sống mái 1 px vẽ **tách rời** phía trên mái (dây lơ lửng trên mọi mái nhà);
3. chi tiết (bóng ống khói, diềm tháp, khói) vẽ **tràn ra ngoài** silhouette, tạo đảo pixel lẻ.

Song song đó là lỗi ở **world space**: cửa Armory do P20 vẽ nằm ở world **(1069.4, 601.7)**,
cách trigger **(993, 655)** tới **76 unit về đông và 120 unit về bắc**. Người chơi đi tới đúng
cái cửa nhìn thấy và không có gì xảy ra.

### 1.2 Sửa nguyên nhân: quy tắc dựng khối bắt buộc

`tools/mapforge_p23_architecture.py` dựng mọi khối theo đúng thứ tự, và thứ tự chính là cách sửa:

```
1 silhouette trước   một quad đục cho mỗi mặt (mặt tiền sáng, mặt hông lùi vào tối hơn)
2 vật liệu sau       mạch đá, quoin, rãnh vẽ BÊN TRONG silhouette
3 mái sau            diềm -> dốc -> sống mái, trên một thân tường đã khép kín
4 nối cuối           dải soffit ĐỤC giữa đáy diềm và đỉnh tường + đầu rui
5 tiếp đất           course móng + AO nướng sẵn trên hàng chân
```

kèm ba cổng tự động **chặn build** (không phải chặn reviewer):

- `validate()` loại sprite có cột thủng alpha, pixel bán trong suốt, viền trên/trái/phải không
  trong suốt, chân tiếp đất hẹp, hoặc độ phủ < 28 %;
- `seal_seams()` bịt mọi khe 1–7 px giữa hai đoạn đục trong cùng một cột bằng đúng tông bóng dưới
  diềm — nên lỗi làm tròn cũng đọc thành bóng dưới mái đua, không thành lỗ;
- `prune_specks()` xoá đảo < 12 px — chính là lỗi tạo ra "dây sống mái lơ lửng".

Ngoại lệ duy nhất được ghi rõ: khe trời giữa hai tháp Tower Gate là **silhouette thiết kế**, và
cả generator (`GAP_ALLOW_X`) lẫn test Kotlin đều ghim đúng dải x 96..166 để nó không thể nới ra.

### 1.3 Bảy công trình

| công trình | canvas | cửa / lỗ mở | sửa chính |
|---|---|---|---|
| **Armory** | 190×166 | **vào được**, vẽ lại đúng trigger (993, 655) → sprite x 94, sill 163 | sai số còn **0.25 px X / 0 px Y**; một ngôn ngữ mái; ống khói có nắp đứng; khung gỗ chịu lực; quoin 2 góc; móng thật |
| **Hero Quarters** | 200×168 | vào được (823, 790) → x 99, sill 165 | cửa lõm 5 lớp (viền đá → jamb sáng → jamb tối → reveal đen → cánh thụt → 2 bậc); mái hiên có 2 cột chạm đất; dormer; nhà phụ lean-to |
| **Restaurant** | 206×172 | vào được (1103, 925) → x 102, sill 169 | gable lối vào nhô ra ôm đúng cửa; awning sọc có diềm scallop; ống khói có bóng đổ trên mái |
| **Hero Registry** | 188×166 | **zone-only → loggia 3 vòm có mặt bàn đá** | bỏ cửa giả; turret chuông + đồng hồ; bảng đồng, bục sổ, ghế đá |
| **Synthesis Lab** | 188×168 | **zone-only → workshop bay mở** | bỏ cửa giả; ống ngưng đồng, retort thuỷ tinh, bench trong hốc |
| **Summoning Hall** | 268×206 | zone-only, rune stage vẫn ở sprite (111, 176) | khép kín trọn khối: pediment → entablature → capital → cột → stylobate → sân đài **đục**; không còn 10 px trời giữa entablature và cột, không còn 33 px trời dưới podium |
| **Tower Gate** | 260×224 | zone-only, hốc tối đúng box rift (130, 120) ± (16, 27) | **1127 px tối / 0 px sáng** trong box rift, đỉnh vòng rift nằm trên đá đục, 150 px spill ở ngưỡng; tường lùi tối hơn tháp 26 %; tháp có batter, quoin, plinth, lỗ châu mai |

### 1.4 Sáu công trình nền Sanctuary (PNG mới, cùng hợp đồng blit)

`MEDIEVAL_TOWNHOUSE_A/B`, `SANCTUARY_COTTAGE_BROWN/SLATE`, `FORTRESS_CORNER_TOWER`,
`SOUTHERN_GATE_TOWER` — trước đây là 6 lần gọi `paintAnimeHalfTimberHouse`/`buildFortressTower`.
Chúng có `bottomPadding = 13` trong khi renderer mong 3 hàng, nên **trôi ~20 world unit về bắc**
so với anchor, cộng cửa tím pastel và cửa sổ lưới `#FFEE55`.

Nay: `bottomPadding = 0` (hàng chân là hàng đục cuối), có course móng vẽ sẵn, cửa **đóng, không
đèn** (nhà không có interior thì cửa đóng là tín hiệu trung thực). Tháp vành đai chuyển từ
hình chữ nhật có `mortar` **sáng hơn** đá (in ra lưới vàng phát sáng) sang thân ellipse có mạch
tối hơn đá, parapet corbel, merlon, plinth.

### 1.5 Mười một prop

Bồn hoa (7), đèn đường 3 frame (6), ghế gỗ (4), vòm phế tích (6 ở F1/F2), tường chắn, monolith,
dummy tập luyện (10), giá vũ khí (4), cọc đấm. Tất cả là PNG pixel thật, cùng bảng màu và cùng
nguồn trăng trên-trái với công trình.

Đèn đường: **29/32 cột pixel giống tuyệt đối giữa 3 frame**, chỉ cột x 21–24 (lửa trong kính)
đổi — trước đây bán kính `fillCircle` đổi theo frame nên **silhouette rung**, một lỗi thật.

---

## 2. Những gì **CHƯA** làm / còn yếu (nói thẳng)

- **Chưa đo FPS/jank/nhiệt trên máy thật: `NOT TESTED`.** Không có điện thoại trong môi trường
  này. Mọi ảnh là `REAL ENGINE RENDER (Robolectric)` — Skia phần mềm ở density 1x, **không**
  phải ảnh chụp máy Android và **không** phải số đo hiệu năng.
- **`TALL_GRASS_CLUMP`, `WILDFLOWERS`, `TATTERED_BANNER` vẫn là Canvas.** Chúng cao 1–7 px ở zoom
  chơi nên chưa phải đường nối nhìn thấy được; là việc kế tiếp, không phải việc đã xong.
- **Sân tập SW vẫn là ellipse của renderer** (`drawOrganicTrainingSandArena`), không phải prop —
  ngoài phạm vi một pass asset.
- **`FACILITY_TRAINING_GROUND` vẫn không có obstacle ở Sanctuary.** Không bịa sprite, không thêm
  obstacle. Đây là hiện trạng đã ghi từ P21 và tôi giữ nguyên.
- **Masonry vẫn hơi thấp tương phản ở 0.88** trên Tower Gate: tường lùi và tháp đã tách được
  nhưng ở zoom chơi phần đá vẫn khá đồng giá trị; silhouette phải gánh. Nếu muốn nâng nữa thì
  tăng một bậc tương phản mạch đá, **không** thêm shader.
- **Registry/Synthesis vẫn cùng ngôn ngữ timber-frame** với 4 nhà ở; chúng phân biệt bằng
  silhouette (tháp chuông / bay mở / ống ngưng) chứ không bằng vật liệu. Ở 0.36 người xem đọc ra
  "một làng", chưa đọc ra 6 cơ sở có tên.
- **Ảnh before/after là cùng camera nhưng khác nội dung art** — đúng mục đích so sánh, nhưng
  không phải pixel-diff vì art đổi.

---

## 3. Bằng chứng (run ID + HEAD)

| mục | giá trị |
|---|---|
| HEAD cuối | `b1c92084` (branch `arena/c67c3cc2-eternal-sunmmoner`) |
| Baseline before | `b0dca99` → `docs/visual-evidence/p23/engine-render-before/` (24 ảnh, chụp trên art P22) |
| After | `docs/visual-evidence/p23/engine-render/` (24 ảnh, cùng camera/zoom/actor/timeSec) |
| Contact sheets | `docs/visual-evidence/p23/contact-sheets/` — nhãn **ART CONTACT SHEET** |
| Camera config | `docs/visual-evidence/p23/camera_props.json` |
| CI `Build Eternal Summoner APK` | run `41486894628` — **pass** (trên `4143a8aa`); run `41488123618` chạy lại trên HEAD cuối |
| CI `Lyra engine render evidence` | run `41486894782` — **pass** (trên `4143a8aa`); run `41488123522` chạy lại trên HEAD cuối |
| Trong đó | P23 render 24 frame + P23 contract 7 test + P20/P21/P22 contract & render + `SanctuaryMaterialTest` + `SanctuaryEnvironmentArtTest` + `LayoutResponsiveness` + `ScenePerformanceTest` + `LivingWorldTest` |
| Ghi chú diff | `docs/visual-evidence/p17c…p22/` **đổi byte** một cách hợp lệ: workflow render lại toàn bộ ma trận trên art mới, và các frame P20/P21/P22 có chứa đúng những toà nhà vừa sửa. `ci-diagnostics/` (log lỗi tạm) đã được xoá khỏi nhánh. |

Camera đã kiểm không crop (tính bằng công thức chiếu, không bằng mắt):

| frame | zoom | khung dọc |
|---|---|---|
| `armory_full_z148` | 1.48 | y 75.2 → 468.3 (720 px) |
| `summoning_hall_whole_z148` | 1.48 | y 20.1 → 678.6 |
| `tower_gate_whole_z148` | 1.48 | y 25.0 → 661.6 |
| `armory_full_z088` / `_z100` | 0.88 / 1.00 | y 178.0→411.7 / 153.2→418.8 |
| `sanctuary_overview_z036` | 0.36 | toàn đảo |

---

## 4. Giới hạn benchmark (đọc kỹ trước khi trích số)

- Toàn bộ ảnh là **Robolectric native Skia, raster phần mềm, density 1x**. Đây là *renderer thật
  chạy code thật*, nhưng **không** phải GPU của điện thoại.
- **Không có** số FPS, p95 frame time, jank, nhiệt, RAM thực đo. Không có số nào bị bịa.
- RAM decode là số **tính toán** từ `W × H × 4`, không phải `dumpsys meminfo`:

| nhóm | decoded ARGB_8888 |
|---|---|
| 7 facility (P21 cũng 958.1 KB) | **958.2 KB** — không đổi vì canvas không đổi |
| 4 nhà + 2 tháp (thay canvas procedural cùng kích thước) | **419.0 KB** — không đổi |
| 11 prop mới | **+140.9 KB** (thực tăng, đã ghi rõ) |

- Draw cost: planter/dummy/rack chuyển từ N lệnh vector mỗi frame sang **1 blit cache** (rẻ hơn);
  bench/arch/wall/monolith/post thay 1:1. Không thêm pass, không shader, không blur/bloom, không
  sinh ảnh lúc chạy, không cấp phát mỗi frame.
- Không đổi: `ObliqueCamera` (`.72`), `ObliqueSpriteGeometry`, phân mảnh tán cây (0.52–0.56),
  `SanctuaryEnvironmentShadows`, depth sort, cache cỏ 4×4 P16, road culling, `FilterQuality.None`,
  `oak.png` (P18) và `cypress.png` (P20) — hai file này được test khẳng định **không đổi byte**.

---

## 5. PR

**PR #26** — https://github.com/lightingfff/eternal-sunmmoner/pull/26 → `main`.

File **đã đụng**:
- `tools/mapforge_p23_architecture.py` (mới), `tools/mapforge_p23_props.py` (mới)
- `app/src/main/assets/premium_slice/`: 7 PNG + `manifest.json`
- `app/src/main/assets/sanctuary_p03/`: 6 PNG nhà/tháp + 11 PNG prop + `manifest.json`
- `app/src/main/java/.../pixel/PixelStructureAtlas.kt` (PNG-first + fallback + 3 prop mới)
- `app/src/main/java/.../pixel/SanctuaryEnvironmentShadows.kt` (đăng ký shadow cho prop)
- `app/src/main/java/.../pixel/PixelSpriteRenderer.kt` (dummy/rack dùng sprite khi có)
- `app/src/main/java/.../pixel/InteriorWorldRenderer.kt` (planter dùng sprite khi có)
- `app/src/main/java/.../map/WorldMapModels.kt` (chiều cao shadow cho 5 kind)
- `app/src/test/java/.../MapForgeP23ContractTest.kt` (mới), `MapForgeP23RenderTest.kt` (mới)
- `app/src/test/java/.../MapForgeArchitectureContractTest.kt` (hộp cửa Armory P20 → vị trí đúng trigger)
- `.github/workflows/lyra-render-evidence.yml` (thêm P23 render/contract + publish `p23/`, rebase trước khi push ảnh)
- `docs/art/P23_INTEGRATION_AUDIT.md`, `P23_ARCHITECTURE_REPAIR.md`, `P23_ENVIRONMENT_PROPS.md`, `P23_REPORT_VI.md`
- `docs/visual-evidence/p23/`

File **KHÔNG đụng** (cố ý): `TowerFloorCatalog.kt`, `SanctuaryHouseEntrances`, `ObliqueCamera.kt`,
`ObliqueSpriteGeometry.kt`, `SanctuaryWorldRenderer.kt`, `paintAnimeHalfTimberHouse` (giữ làm
fallback), `PixelSanctuaryLandmarks.kt`, toàn bộ P18/P19 terrain, `oak.png`, `cypress.png`,
`smithery_verge.png`, Lyra pack, save schema, collision/navmesh, camera.

**Không tự merge.** PR để owner review.

### Ghi chú riêng cho owner

Trong quá trình làm có **một quyết định vượt ra ngoài art**: tôi sửa `MapForgeArchitectureContractTest`
(P20) vì test đó đang khẳng định hộp cửa Armory ở `x 133..150, y 109..139` — tức là khẳng định
**chính lỗi cũ**. Muốn sửa lỗi cửa Armory thì bắt buộc phải cập nhật test đó; nó vẫn kiểm tra
đúng bản chất (cửa là lỗ mở tối, đục, có bậc), chỉ đổi toạ độ sang vị trí khớp trigger.
Trigger, anchor, footprint, collider **không** đổi. Nếu owner muốn giữ nguyên vị trí cửa P20 thay
vì sửa, xin nói — khi đó phải dời trigger, và đó là thay đổi map cần owner duyệt riêng.
