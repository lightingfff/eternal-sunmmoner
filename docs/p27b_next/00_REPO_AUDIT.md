# P27B V5 / 00 — REPO AUDIT + BASELINE (Checkpoint 0)

Ngày: 2026-11-05. Nhánh phiên: `arena/8be73f1b-eternal-sunmmoner` (nhánh làm việc cố định của Arena).
Base: `e5e04e1` — merge commit của PR [#34](https://github.com/lightingfff/eternal-sunmmoner/pull/34) vào `main`.

## 1. Trạng thái thật lấy được từ git (không phải giả định)
- PR #34 **đã MERGED** ở lượt trước (`mergedAt 2026-11-05T11:28:29Z`, merge commit `e5e04e1`). Prompt V5 giả định PR còn OPEN với HEAD `c09050f`; thực tế không còn đúng. Vì vậy "tiếp tục trên nhánh PR" = làm trên `main` hiện tại qua nhánh Arena.
- Workspace là **shallow clone** khi bắt đầu (chỉ 1 commit). Đã `git fetch --unshallow` để kiểm chứng lịch sử thay vì suy đoán.
- **Kiểm chứng lại bằng chứng cũ**: `git diff 4baa7d1 HEAD -- app/src/main` → **rỗng**. Nghĩa là toàn bộ ảnh AFTER đã commit (`docs/p27b/previews/after/`, commit `c6f5adc` trên `4baa7d1`) được render từ **đúng runtime code hiện tại**. Ảnh BEFORE (`docs/p27b/previews/before/`, `5a2876e`) cũng vậy. Không có chuyện "dùng ảnh của bản khác".
  - Kết luận: baseline hợp lệ, không cần render lại BEFORE một cách lãng phí. Bổ sung: camera/zoom/hour/weather/resolution lấy từ `docs/p27b/previews/*/world/camera_props.json`, `source_sha` ghi trong từng file.
- Trạng thái check của lần merge đó (đã ghi ở PR #34): `evidence` PASS; 2 workflow khác fail **chỉ ở bước `upload-artifact`** vì quota storage của repo; và **P26C E4 FAIL thật** (+20.8% noon, +66.1% gate) — xem mục 5.

## 2. Cấu hình renderer đang dùng (đọc từ code, không đoán)
| Hằng số | Giá trị | Nguồn |
|---|---|---|
| `OBLIQUE_Y_SCALE` | `0.72f` | `WorldMapModels.kt` (`ObliqueCamera`) |
| `TILE_PX` | `64` | `PixelTileAtlas.kt` |
| zoom lobby mặc định / clamp | `0.88f` / `0.26f..1.52f` | `SanctuaryLobbyEngine.kt:236,501` |
| blit sprite | `drawCrispSprite(bmp, cx, footY, 2.0f * s)`, `s = zoom * obs.scale`, `FilterQuality.None` | `PixelSpriteRenderer.kt:639,713` |
| world grade | `drawWorldGrade()` phủ toàn màn sau props | `LivingWorldRenderer.kt:27-35` |

## 3. ASSET BINDING MAP (trạng thái trước Checkpoint 2)
Xem `01_ASSET_BINDINGS.csv` (cùng thư mục) để có bảng đầy đủ. Tóm tắt điều quan trọng:

| Nhóm | Nguồn art hiện tại | Canvas | Hệ quả thị giác |
|---|---|---|---|
| Summoning Hall, Tower Gate | `sanctuary_p27b/structures/*.png` (mới) | 268×206, 260×224 | đã hợp nền sáng |
| Hero Registry, Armory, Synthesis Lab, Hero Quarters, Restaurant | `premium_slice/*.png` | 188×166, 190×166, 188×168, 200×168, 206×172 | **lệch tone rõ** — đây là lỗi A |
| Townhouse A/B, cottage brown/slate | `sanctuary_p03/*.png` | 152×156, 144×160, 132×144 | **lệch tone rõ** — lỗi A |
| Nền grass/soil/flagstone/cobble | `sanctuary_p27b/terrain/*` (16 ô/family, torus 4×4) | 64×64 | **lặp chu kỳ 4** — lỗi B |
| Props/vegetation | `sanctuary_p03/*` (lantern 3 frame, bench, barrels, canopy_a/b, oak/pine/twisted, hedge, boulder…) | 32×66 … 114×132 | còn art cũ, chưa pass riêng |

**Kích thước hiển thị thật** (tính từ code, không phải ước lượng): `2.0 * zoom * scale` × canvas.
Ví dụ Hall: `2 × 1.48 × 1.08 × 268 ≈ 857 px` wide ở zoom 1.48 → **hệ số ~3.2× so với canvas 268 px**. Đây là lý do sprite pixel 268 px trông "to pixel" khi zoom gameplay.

## 4. NĂM LỖI TỒN ĐỌNG — vị trí code + ảnh chứng minh
| # | Lỗi | Vị trí code | Ảnh chứng minh |
|---|---|---|---|
| A | Nhà ở/props lệch tone với nền P27B sáng | `PixelStructureAtlas.kt:30-43` (`premium_slice`), `:57-71` (`sanctuary_p03` townhouse/cottage) | `previews/after/world/service_noon_z088.png`, `overview_noon_z036.png` |
| B | Lặp texture 4×4 ở zoom 1.0 | `PixelTileAtlas.kt:269` `sanctuaryTileIndex` = `(ty&3)<<2 or (tx&3)`; grass block `:314-340` (1 block cache) | `previews/after/world/plaza_noon_z100.png`; đo được: chênh luma chu kỳ-4 ≈ 20 vs chu kỳ-1 ≈ 12 |
| C | Gate/Hall ivory ban đêm quá sáng | `PixelSpriteRenderer.drawProp` → `drawCrispSprite` (blit thường) rồi `LivingWorldRenderer.drawWorldGrade` (fill toàn màn) | `summon_night_z148.png`, `gate_night_z148.png` |
| D | HUD/auth/loading chưa có design system | `SanctuaryLobbyScreen.kt`, `FantasySkin.kt`, `FantasyManagement.kt`, `AuthGateScreen.kt` | `previews/after/ui/hud_land_800.png` (giống BEFORE) |
| E | Summon result card cắt chữ | `SanctuaryFacilityModals.kt` `SummoningHallContent` + `summon_result` item | `previews/after/ui/summon_result_land_800.png` |

## 5. Ghi chú về P26C E4 (không được lờ đi)
`tools/p26c_measure_clouds.py` gate **E4** đo độ sáng vùng world <42% frame so với baseline P26B và yêu cầu |Δ| < 8%. Hiện tại: noon +20.8%, night +20.8%, gate z1.48 **+66.1%** → FAIL. Đây là hệ quả trực tiếp, có chủ đích của việc nền P27B sáng hơn P03. **Không** được nới ngưỡng. Hai hướng hợp lệ, phải chọn có ý thức:
1. retune vật liệu về gần exposure P26B (mất lợi ích "sáng, đọc được vật liệu" đã đạt), hoặc
2. lập baseline P26C mới theo look P27B kèm lý do + ảnh, và cập nhật gate E4 thành "so với baseline P27B" thay vì P26B.
Việc này thuộc Checkpoint 2 khi chốt palette; **chưa xử lý ở Checkpoint 0**.

## 6. File được phép sửa / bất khả xâm phạm
**Sẽ sửa (dự kiến, chốt dần theo checkpoint):**
- `app/src/main/java/com/example/game/pixel/PixelTileAtlas.kt`, `SanctuaryArtAssets.kt`, `SanctuaryWorldRenderer.kt`, `PixelSpriteRenderer.kt` (chỉ phần sampling/light response, **không** camera/anchor/collision)
- `app/src/main/java/com/example/game/pixel/PixelStructureAtlas.kt` (binding PNG mới)
- `app/src/main/java/com/example/ui/**` (chỉ Checkpoint 4-6, giữ testTag/callback)
- `app/src/main/assets/sanctuary_p27b_2/**` (asset mới, **không** ghi đè P03/premium_slice/P27B cũ)
- `app/src/test/java/com/example/**`, `.github/workflows/p27b-render.yml`, `tools/p27b_*.py`, `docs/p27b_next/**`

**Bất khả xâm phạm:**
`WorldMapModels.kt` geometry/zone/path, `TowerFloorCatalog.kt` anchors/footprints/colliders, `SanctuaryLobbyEngine.kt` state/save, `LivingWorldRenderer` weather/sky (P26C), `HudrLightResponse`? *(xem `HdrLightResponse.kt` — chỉ đọc)*, auth/backend/save schema, `sanctuary_p03/**`, `premium_slice/**`, `sanctuary_p27b/**` (đã ship).

## 7. Baseline screenshots (nguồn của mọi so sánh sau này)
| Ảnh | Camera (x,y,zoom) | hour/weather | Kích thước |
|---|---|---|---|
| `overview_noon_z036` | 1600,1380,0.36 | 12/CLEAR | 1280×720 |
| `service_noon_z088` | 960,860,0.88 | 12/CLEAR | 1280×720 |
| `plaza_noon_z100` | 1940,1240,1.00 | 12/CLEAR | 1280×720 |
| `summon_noon_z148` / `summon_night_z148` | 2430,1715,1.48 | 12 / 0, CLEAR | 1280×720 |
| `gate_noon_z148` / `gate_night_z148` | 1960,310,1.48 | 12 / 0, CLEAR | 1280×720 |
| `summon_modal_land_800` / `_port_360` | Compose, 800×360dp / 360×640dp | — | view |
| `summon_result_land_800`, `hud_land_800` | Compose landscape | — | view |

Thiếu để chấm đủ (sẽ bổ sung khi harness mở rộng): **gate/Hall ở zoom .88**, **z1.48 cho từng nhà ở** (Hero Quarters/Armory/Restaurant/Registry/Lab), **4 mốc giờ 6/12/18/0**, **STORM chỉ để so sánh**.

## 8. Kế hoạch checkpoint (đúng thứ tự V5, không nhảy cóc)
0. (tài liệu này) ✔
1. Sửa lặp 4×4: cả asset (biến thể thật) + chiến lược sampling world-space; test đo chu kỳ; ảnh z0.36/0.88/1.0/1.48.
2. Buildings/props theo style P27B; bind PNG mới; verify pivot/collision/depth.
3. Night light response cho landmark (diffuse vs emissive).
4. UI tokens + fix result card clipping.
5. HUD/hero/facility panels.
6. Auth/loading + final matrix + CI/APK trên SHA cuối.

## 9. Rủi ro đã biết trước khi làm
- Không có JDK/Gradle/Android SDK trong sandbox → mọi build/test/render phải qua GitHub Actions; mỗi vòng CI ~3-5 phút.
- Không có máy thật → **FPS/thermal luôn là NOT TESTED**; mọi tuyên bố hiệu năng chỉ được nói ở mức số draw call/bộ nhớ cache đọc từ code.
- Repo đã dùng hết quota artifact → ảnh evidence phải commit vào nhánh (workflow `p27b-render.yml` đã làm vậy), không upload artifact.
