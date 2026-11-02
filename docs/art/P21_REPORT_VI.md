# P21 — BÁO CÁO GIAO HÀNG (Sanctuary facilities)

> Trạng thái: **PR đang mở, chưa merge** (xem link ở mục 1). Tài liệu này ghi đúng những gì đã
> kiểm chứng được trong môi trường làm việc và những gì **chưa** chạy được, không tô hồng.

## 1. PR & commit

- Base: `main` @ `5e2b9f1` (P18/P19/P20 đã merge, PR #23 P20 verified 2026-11-02).
- Head: nhánh `arena/be360afb-eternal-sunmmoner` (commit đầu của P21 nằm trong PR mở).
- PR: xem liên kết PR do công cụ tạo ở cuối phiên — **không tự merge**.

## 2. Kiểm kê tài sản trước / sau

| Cơ sở | Trước P21 | Sau P21 | Canvas |
|---|---|---|---|
| Summoning Hall | procedural `buildSummoningHall()` | `premium_slice/summoning_hall.png` | 268×206 (không đổi) |
| Tower Gate | procedural `buildTowerGateMonument()` | `premium_slice/tower_gate.png` | 260×224 (không đổi) |
| Hero Registry | procedural `buildHeroRegistryHall()` | `premium_slice/hero_registry.png` | 188×166 (không đổi) |
| Hero Quarters | procedural `buildHeroQuarters()` | `premium_slice/hero_quarters.png` | 200×168 (không đổi) |
| Restaurant | procedural `buildRestaurantExterior()` | `premium_slice/restaurant.png` | 206×172 (không đổi) |
| Synthesis Lab | procedural `buildSynthesisLab()` | `premium_slice/synthesis_lab.png` | 188×168 (không đổi) |
| Armory | P20 `armory.png` | **không đổi** | 190×166 |
| Training Ground | procedural `buildTrainingGround()` | **không đổi** (xem §6) | 192×160 |

Công cụ sinh art: `tools/mapforge_p21_architecture.py` (offline, deterministic, Pillow, không dùng
art bên ngoài). Contact sheet: `docs/visual-evidence/p21/P21_ART_CONTACT_SHEET.png`
(**ART CONTACT SHEET**, không phải engine render).

## 3. Vì sao art cũ yếu và đã sửa gì

Art cũ là hình khối procedural một tông màu mỗi mặt, nên ở zoom 0.88 đọc ra như "sticker":
- **Summoning Hall** cũ = 6 vòng ellipse + 9 cột mảnh → một cái vành xám, không có khối mái, không
  có cửa vào đúng nghĩa. Mới: 3 tầng sân đàn có rãnh đá, mặt tiền hexastyle có cella lõm tối phía
  sau cột, mái hai mặt phẳng có sống mái đồng, cổng vòm có voussoir + cửa đồng mở, hồ sáng lam
  lạnh, tháp phụ hai bên, thang lớn và brazier (lò than).
- **Tower Gate** cũ = hai tấm đá phiến phẳng, "portal" là ellipse dán trước đá. Mới: tường thành
  lùi có parapet corbel + lỗ châu mai, hai tháp basalt vát (battered) đứng nhô ra với đỉnh châu mai,
  quoin, lỗ rune; lintel nặng có cornice + dentil + 2 rune chạm; **hốc cổng vẽ đúng hộp mà renderer
  vẽ vòng rift động** (tâm sprite (130,120), nửa kích thước 16×27 px) và **cố ý vẽ tối** — vòng rift
  động là nguồn sáng duy nhất, ánh vẽ tay chỉ còn ở ngưỡng cửa; bậc đài 3 tầng; chậu lửa hai bên;
  rêu + vết nứt.
- **Registry / Quarters / Restaurant / Lab** cũ = **cùng một** `paintAnimeHalfTimberHouse` với vài
  tham số khác → 4 nghề trông như 4 bản đổi màu của một nhà. Mới: mỗi cơ sở có khối riêng —
  Registry có tháp chuông + mặt đồng hồ trên sống mái; Quarters có mái ngói gỗ ấm, dormer và
  **chái thấp bên trái**; Restaurant rộng nhất, có **gable nhô ra trên cửa**, awning sọc, ống khói
  bốc khói; Lab có tháp ngưng tụ đồng, ống dẫn, chóp thông gió và ánh **teal** (không tím, không
  cyan).
- Cả 6 trước đây **không có viền tiếp đất** → trôi nhẹ trên cỏ. Nay có contact rim nướng sẵn ở
  hàng đáy.

## 4. Anchor / cửa / test cho từng cơ sở

Renderer blit **bottom-centred** tại `cx, cy+6s`, `pixelScale = 2s`. Suy ra:

```
spriteX→worldX: worldX = obs.x + (spriteX - W/2) * 2 * scale
spriteY→worldY: worldY = obs.y - ((H-3) - spriteY) * (2*scale) / 0.72
```

- **Hero Quarters** (obstacle 825,790; trigger **823,790**): cửa vẽ ở tâm canvas x=100, đáy lá cửa
  hàng **H−3 = 165**; nghịch đảo → world (823, 790) ⇒ khớp trigger trong 1 px sprite (1.6 đơn vị
  world). Test: `enterableDoorsLandOnTheirWorldTriggers`.
- **Restaurant** (1105,925; trigger **1103,925**): tâm x=103, đáy **H−3 = 169** ⇒ world (1103, 925).
- **Armory**: giữ nguyên art P20 và trigger (993,655).
- **Registry / Synthesis / Summoning / Tower Gate**: không có trigger cửa; kiểm tra anchor, scale,
  footprint, PropKind và zone không đổi (`worldGeometryIsUnchanged`).
- **Summoning Hall**: tâm tương tác zone (2380,1930) nghịch đảo về sprite **(111,176)** — vòng rune
  được vẽ đúng tại đó, nên hero đứng đúng chỗ hoạt động là đứng trên vòng rune.
- **Tower Gate**: hộp rift mà `drawProp` vẽ động là sprite (130,120) ± (16,27) — hốc cổng vẽ đúng
  hộp này (`towerGatePortalMatchesAnimatedRiftBox`).

### 4.1 Phát hiện: cửa Armory P20 lệch khỏi trigger (ghi nhận, chưa sửa)

Áp cùng công thức nghịch đảo cho art P20 (`x 133..150, y 109..139`), tâm cửa ra world
**(1069.4, 601.7)** trong khi trigger là **(993, 655)** — lệch khoảng **+76 đơn vị world theo X và
−53 theo Y**. Armory là toà nhà **vào được**, nên người chơi nhắm vào cửa vẽ sẽ nhắm lệch khỏi
vùng kích hoạt. P20 nằm ngoài phạm vi được phép sửa của pass này (brief cấm ghi đè
`armory.png` khi chưa được cho phép), nên P21 **không sửa** mà ghi lại và đã dựng test để lỗi
tương tự không thể lặp lại ở Quarters/Restaurant. Đề xuất bước tiếp theo (một PR nhỏ, cần owner
duyệt): chỉ vẽ lại khung cửa Armory về `x 88..99, y 119..161`, **hoặc** dời hằng số entrance —
không làm cả hai.

## 5. Bằng chứng render & CI

| hạng mục | trạng thái thực tế |
|---|---|
| ART CONTACT SHEET 1x / 2x / 0.88 | **CÓ**, `docs/visual-evidence/p21/P21_ART_CONTACT_SHEET.png` (nhãn ART CONTACT SHEET) |
| REAL ENGINE RENDER | **CÓ**, 16 ảnh trong `docs/visual-evidence/p21/engine-render/`, do `MapForgeP21RenderTest` sinh ra bằng `drawSanctuaryFloatingWorld` + `drawGroundShadows` + `drawProp` sắp theo depth + `drawCharacterSprite` (Robolectric native graphics), CI tự commit (run `41445386091`) |
| `MapForgeP21ContractTest` | **PASS** trong CI trên head `227ec20` |
| `:app:assembleDebug` | **PASS** trong CI ("Build Eternal Summoner APK") trên cùng SHA |
| `python3 tools/validate_assets.py` / `validate_lyra_pack.py` | **PASS** (chạy tại máy) |
| FPS / nhiệt độ thiết bị | **KHÔNG ĐO** — không có thiết bị |

Máy soạn thảo này không có JDK/Android SDK/mạng ra ngoài, nên mọi gate Kotlin do CI chạy; số liệu
trên là kết quả CI thật, không phải suy đoán. **Lưu ý:** `assembleDebug` chỉ chứng minh biên dịch và
đóng gói, không chứng minh khung hình khi chạy hay cảm ứng.

### 5.1 Ảnh engine đã buộc sửa gì

Bộ ảnh engine đầu tiên là lý do hai công trình chính phải vẽ lại — đây là phần quan trọng nhất của
báo cáo:

1. `hero_tower_gate_default` cho thấy **một tấm đá phẳng tối với quả cầu sáng trôi**: quả cầu vẽ tay
   nằm ngay dưới vòng rift động của renderer, hai trụ dính thành một bức tường, lỗ châu mai không có
   răng. Đã dựng lại: tường thành lùi phía sau có parapet corbel + lỗ châu mai, hai tháp vát đứng
   nhô ra với đỉnh châu mai, quoin và lỗ rune; **hốc cổng vẽ TỐI** để vòng rift động là nguồn sáng
   duy nhất chuyển động.
2. `hero_summoning_default` cho thấy **một cái miếu trắng bệch mặt tiền phẳng**: `LIME` đọc thành
   trắng bạc trên cỏ đêm, thân chùa không có mặt hông. Đã dựng lại trên `LIME_T` (cùng họ đá nhưng
   hạ xuống dải sáng của Sanctuary), có cella quay về bên phải dưới mái bốn mặt, cửa vẽ như ô cửa có
   ánh sáng dồn ở ngưỡng.
3. Hai khung 1.48 cắt mất mái (cao 636 px và 659 px trên khung 720). Test render nay hạ camera về
   phía bắc cho `hero_tower_gate_whole` / `hero_summoning_whole` và giữ thêm khung chi tiết
   `hero_tower_gate_portal`.

Cụm dịch vụ (`svc_cluster_default`, `svc_quarters_door_actor`, …) đọc đúng ngay từ bộ ảnh đầu: bốn
silhouette khác nhau, cửa vẽ nằm đúng dưới chân nhân vật.

### 5.2 Điểm còn yếu (thấy trong ảnh engine, chưa sửa)

- Đá của Tower Gate tương phản thấp ở 0.88: tường thành và hai tháp gần như cùng giá trị sáng, nên
  toà nhà đọc thành một khối, chỉ còn lỗ châu mai giữ silhouette.
- Hốc cổng cao và hẹp; vòm trên của vòng rift nằm trên nền quá tối nên ở 0.88 gần như không thấy.
- Bốn toà dịch vụ vẫn chung một ngôn ngữ khung gỗ. Chúng khác nhau ở silhouette (tháp chuông / chái
  thấp / gable nhô / tháp ngưng tụ) nhưng ở 0.36 người xem đọc thành "một ngôi làng", chưa đọc thành
  bốn cơ sở có tên.

## 6. Việc cố ý không làm

1. **Training Ground sprite**: `PropKind.FACILITY_TRAINING_GROUND` có trong `drawProp` nhưng
   `TowerFloorCatalog.buildSanctuaryMap()` **không tạo obstacle nào** với kind đó ⇒ sprite 192×160
   không bao giờ được vẽ trên bản đồ Sanctuary. Vẽ lại sẽ vô hình. Thêm prop = thay đổi bản đồ
   (brief cấm). Vì vậy báo cáo thay vì làm giả.
2. **Armory door remap** (§4.1) — cần owner cho phép.
3. Cottages / townhouse / tháp thành / thảm thực vật / Lyra: không đụng.
4. Bloom, glow động, reflection: thuộc Technical Artist.

## 7. RAM / chi phí vẽ

Canvas giữ nguyên ⇒ bộ nhớ bitmap decode **không đổi**: 215.7 + 227.5 + 121.9 + 131.2 + 138.4 +
123.4 KB = **958.1 KB (0.94 MiB)** cho 6 cơ sở. Draw cost không đổi: 1 blit cache/toà/frame,
`FilterQuality.None`, cùng silhouette shadow. Thay đổi runtime duy nhất: 1 lần decode PNG/toà ở
lần truy cập đầu (thay cho procedural build trước đây), cache trong `SanctuaryArtAssets.images`;
`assetSafe` **không cache lỗi**. Không có số đo FPS/nhiệt.

## 8. Tự phê bình

- **Bộ ảnh engine đầu tiên đã phủ định hai công trình chính** (xem §5.1). Đó là bài học: đánh giá
  trên contact sheet đã bỏ qua việc vòng rift động nằm đè lên quả cầu vẽ tay, và bỏ qua việc đá
  sáng bị "cháy" trên nền cỏ đêm. Sau khi dựng lại, ảnh engine đã xác nhận cả hai đã đúng.
- **Vẫn còn hai rủi ro chưa xử lý:** tương phản đá của Tower Gate ở 0.88 (§5.2) và việc bốn toà dịch
  vụ đọc thành một cụm làng. Đây là hạn chế thật, không phải "ghi chú cho có".
- **Ba toà dịch vụ vẫn cùng họ "nhà khung gỗ"**: đã khác silhouette (tháp chuông / chái thấp /
  gable nhô) nhưng ở 0.36 có thể vẫn đọc thành một cụm nhà giống nhau. Nếu sau khi xem ảnh engine
  thấy vậy, ưu tiên: làm Registry lùn và ngang hơn nữa, đẩy Restaurant cao hơn 1 tầng.
- **Tower Gate phía trên vẫn khá "tường đặc"** ở 1x; phần crenellation mới thêm giúp nhưng vai trụ
  có thể cần thêm một lớp gờ để đọc khối ở 0.88.
- Chi tiết 1 px (dentil, rune nhỏ) có thể thành nhiễu ở 0.88 — cần xác nhận bằng ảnh engine trước
  khi coi là đạt.
- Cả 6 sprite mới chỉ có 1 hướng nhìn (front-right), giống mọi asset khác của Sanctuary; chưa có
  biến thể, không nằm trong phạm vi P21.
