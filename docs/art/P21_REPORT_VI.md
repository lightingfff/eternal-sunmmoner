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
- **Tower Gate** cũ = hai tấm đá phiến phẳng, "portal" là ellipse dán trước đá. Mới: trụ basalt
  vát (battered) có mặt bên lùi, đỉnh lỗ châu mai, rãnh chạm và đèn rune; lintel nặng có cornice,
  dentil, 3 rune; **hốc cổng được dựng đúng hộp mà renderer vẽ vòng rift động** (tâm sprite
  (130,120), nửa kích thước 16×27 px), có jamb sáng/tối, vòng voussoir, keystone, 4 dải độ sâu,
  ánh sáng tràn xuống bậc thềm; bậc đài 3 tầng; obelisk rune hai bên; rêu + vết nứt.
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
| ART CONTACT SHEET 1x / 2x / 0.88 | **CÓ**, `docs/visual-evidence/p21/P21_ART_CONTACT_SHEET.png` |
| REAL ENGINE RENDER (Robolectric, `drawWorldBackground` + `drawGroundShadows` + depth-sorted `drawProp` + actor) | **CHƯA CHẠY Ở MÔI TRƯỜNG NÀY.** Test `MapForgeP21RenderTest` đã viết (fixed cameras: service cluster 0.36/0.88/1.00, từng cơ sở 1.48, cửa + actor, tower gate, summoning zone, island overview) và workflow sẽ commit PNG vào `docs/visual-evidence/p21/engine-render/` khi CI chạy trên PR |
| `python3 tools/validate_assets.py` | **PASS** (chạy tại máy) |
| `python3 tools/validate_lyra_pack.py` | **PASS** (chạy tại máy, có Pillow) |
| `MapForgeP21ContractTest` / `assembleDebug` | **CHƯA CHẠY** — sandbox không có JDK, Android SDK, Gradle distribution và không có mạng ra ngoài; CI trên PR sẽ chạy |
| FPS / nhiệt độ thiết bị | **KHÔNG ĐO** — không có thiết bị |

Vì sao không chạy được: môi trường soạn thảo này chỉ có Python + Pillow, không có `java`, không có
`ANDROID_SDK`, và không truy cập được `services.gradle.org` / `dl.google.com` / Maven. Việc "chạy
thử bằng tay" sẽ là bịa. Do đó toàn bộ gate Kotlin để CI chứng minh, và báo cáo này không tuyên bố
chúng đã pass.

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

- **Chưa có ảnh engine thật trong tài liệu này.** Đây là thiếu sót lớn nhất: mọi đánh giá "đọc được
  ở 0.88" hiện dựa trên contact sheet + tái tạo phép chiếu bằng số học, không phải ảnh chụp từ
  renderer. Cần chạy `MapForgeP21RenderTest` trên CI (hoặc máy có SDK) rồi đánh giá lại — đặc biệt
  mái Tower Gate (224 px cao) rất dễ bị camera 1.48 cũ cắt.
- **Ba toà dịch vụ vẫn cùng họ "nhà khung gỗ"**: đã khác silhouette (tháp chuông / chái thấp /
  gable nhô) nhưng ở 0.36 có thể vẫn đọc thành một cụm nhà giống nhau. Nếu sau khi xem ảnh engine
  thấy vậy, ưu tiên: làm Registry lùn và ngang hơn nữa, đẩy Restaurant cao hơn 1 tầng.
- **Tower Gate phía trên vẫn khá "tường đặc"** ở 1x; phần crenellation mới thêm giúp nhưng vai trụ
  có thể cần thêm một lớp gờ để đọc khối ở 0.88.
- Chi tiết 1 px (dentil, rune nhỏ) có thể thành nhiễu ở 0.88 — cần xác nhận bằng ảnh engine trước
  khi coi là đạt.
- Cả 6 sprite mới chỉ có 1 hướng nhìn (front-right), giống mọi asset khác của Sanctuary; chưa có
  biến thể, không nằm trong phạm vi P21.
