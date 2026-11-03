# P25 — ROUND 1: DISCOVER → BASELINE → IMPLEMENT (pre-engine)

**Ngày**: 2026-11-03. **Baseline**: `origin/main = b2a71629a87c3099a4267c1590e955c71a8f476e` (P24 đã merge). Nhánh `arena/e486a516-eternal-sunmmoner`, sạch, chưa có PR.

## 1. DISCOVER (facts, không suy diễn)

| Nguồn | Kết quả |
| --- | --- |
| `gh pr list` | #30 `MERGED` (2026-11-03T10:09:43Z, merge commit chính là baseline), #29 `MERGED`, #28 `OPEN/Draft` (29 file gameplay) |
| `gh pr diff 28 --name-only` ∩ file P25 | **rỗng** (kiểm chứng bằng `comm -12`) |
| `java` / `gradle` / Android SDK / `/usr/lib/jvm` | **KHÔNG CÓ** → `LOCAL BUILD NOT RUN`; dùng GitHub Actions thật |
| tool spawn/delegate subagent | **KHÔNG CÓ** → 6 vai trò là logical checkpoint tuần tự trong 1 agent, không "đội ngũ độc lập" |
| skills | router, quality-loop (+phụ lục V1), director, environment-architect, technical-artist, visual-QA, android-performance, integration-reviewer; 3 nguồn `scenario-game-assets` / `scenario-textures` / `scenario-refine-loop` |
| `docs/visual-evidence/p23/engine-render/` trên main | 32 PNG (24 P23 + 8 HDR), `camera_props.json` 24 camera cố định → chính là baseline P25 |
| `tools/mapforge_p23_architecture.py` | 2.091 dòng, deterministic offline generator, 13 `BUILDERS`, `DOOR_CONTRACT`, `validate()` (alpha/kích thước/chân nhà), `stamp()` |
| `PixelSpriteRenderer.kt:471` | `drawCrispSprite` bottom-centred tại `footY = cy + 6*s`, `pixelScale = 2*s`, `FilterQuality.None` |
| `WorldMapModels.kt:14` | `OBLIQUE_Y_SCALE = 0.72f`, `worldToScreenX/Y` |

### Ảnh engine baseline đã SOI trực tiếp (không chỉ đọc code)

- `service_cluster_z100.png`: bốn nhà dịch vụ đọc như những mảng xám phẳng; mái slate và tường trước cùng giá trị; mặt hồi bên hầu như không tối hơn; cửa sổ ấm là điểm nhấn duy nhất.
- `summoning_hall_whole_z148.png`: pediment + tường trước + cột cùng một dải xám; band dưới cornice không tối; hốc cửa trung tâm đen + vòng rune cyan vẫn nguyên (phải bảo vệ).
- `hero_quarters.png` / `restaurant.png` / `summoning_hall.png` (PNG tài sản): mái, tường trước, tường hồi là 3 họ màu gần trùng giá trị.

### Baseline CI thật (trước mọi sửa code)

`gh workflow run lyra-render-evidence.yml --ref arena/e486a516-eternal-sunmmoner` → run **`41618836345`**, `head_sha = b2a71629...`, `conclusion = success`, job `125859019767`. Bot commit `d61194e` chỉ đổi `hdr_balanced_night_rain_z088.png` 642099 → 641978 B (hạt mưa không hoàn toàn deterministic), `camera_props.json` SHA `ac6a005fd5725d2c…` không đổi. 32 PNG được chụp bất biến vào `docs/visual-evidence/p25/engine-render-before/` (17.235.610 B).

### Số đo ROI baseline trên PNG tài sản (`measure_asset_rois.py`, alpha ≥ 200, Rec.709)

| asset | roof_key | roof_return | front | side | soffit | door |
| --- | --- | --- | --- | --- | --- | --- |
| `hero_quarters` | 44.11 | 38.97 | 55.19 | **56.76** | 28.24 | 41.03 |
| `restaurant` | 65.93 | **64.99** | 63.73 | **63.59** | 28.49 | 43.05 |
| `summoning_hall` | 64.86 | **72.84** | 80.01 | 67.87 | 79.70 | 103.40 (rune stage, không phải cửa) |

→ `restaurant`: 4 mặt lệch ≤ 2.34; `hero_quarters`: side **sáng hơn** front +1.57 (ngược đổ sáng); `summoning_hall`: return **sáng hơn** pitch +7.98. Đây là (b) + (g), **không** phải (a) lỗ alpha (đã sửa P23, `validate()` chặn), cũng không phải (e) nhà lơ lửng.

### P24 full-frame (giữ nguyên, báo cáo độc lập, KHÔNG đổi ngưỡng)

`Δ(p90−p10)`: overview `+0.0`, service z088 `+0.0`, tower z148 `+5.4` → cổng `≥+3 trên ≥2/3` đạt **1/3**; `summoning_hall_whole_z148 = −3.8`; QA mềm 19/24, visual coherence 2/4; `no-flood` max |Δmean| 3.60% PASS. P24 `BLOCKED` về chất lượng vẫn còn giá trị dù PR #30 đã merge (xem comment `6600828309`). **P25 không tự động gọi đây là PASS.**

### Điểm yếu của whole-frame p90/p10

`p90−p10` toàn khung đo cả nền cỏ, đường, bầu trời, cây và hốc tối; một công trình chiếm ~20–30% khung có thể tách mặt rõ mà chỉ đổi vài đơn vị p90/p10, ngược lại một gradient nền có thể làm tăng chỉ số mà không tạo cạnh khối. Vì vậy P25 thêm **ROI theo mặt phẳng** (`roi_definitions.json`) và báo cáo hai hệ số **độc lập**: ROI separation (B5) và whole-frame p90/p10 (disclosure). Cả hai không được quy về nhau.

### Root-cause trace theo từng công trình (obstacle → PNG → bitmap → scale/anchor → mask/silhouette/shadow → P24 HDR)

| Bước | `hero_quarters` / `restaurant` | `summoning_hall` |
| --- | --- | --- |
| obstacle | `building_hero_quarters (825,790) s=.8`, `building_restaurant (1105,925) s=.8` | `building_summon_hall (2430,2005) s=1.08` |
| atlas PNG | `PixelStructureAtlas.assetSafe("hero_quarters")` / `…restaurant` | `…summoningHallStructure` |
| decoded bitmap | `SanctuaryArtAssets` cached decode, 200×168 / 206×172 | 268×206 |
| draw scale/anchor | `drawCrispSprite(..., cx, cy+6s, 2s)`, bottom-centred, `FilterQuality.None` | cùng, `2s` |
| mask / silhouette / shadow | `SanctuaryEnvironmentShadows.sprite()` + `drawDirectionalShadow34`; `HdrLightResponse.cachedMask` 1 mask/tint | cùng |
| P24 HDR overlay | `drawHdrGround` trước sprite, `drawVolumeGrade` gradient **theo không gian** trên toàn silhouette | cùng; gây `−3.8` spread |
| Kết luận | sprite **đã phẳng từ khâu author** → mọi pass đè lên không tạo được cạnh | pediment/cột/cornice cùng dải; return đảo sáng |

## 2. RUBRIC / ROI (chốt TRƯỚC khi có candidate)

`WORK_ORDER.md` + `roi_definitions.json` (một nguồn duy nhất, đọc bởi generator / measure / validator). Ngưỡng B5: `front−side ≥ 12`, `roof_key−return ≥ 10`, `|roof_key−front| ≥ 10`, `front−soffit ≥ 18`, `roof spread ≥ 14`, `door ≤ front−20`. `no-flood |Δmean| < 8%`. Cổng P24 `+3 trên ≥2/3` giữ nguyên, chỉ **báo cáo độc lập**.

## 3. IMPLEMENT (Environment Artist lead, 1 writer)

- `tools/p25_face_grammar.py` (mới): grammar **baked OFFLINE** vào PNG. Với mỗi mặt phẳng author (đa giác sprite-space), tính mean luminance hiện tại rồi áp `newL = target + (L − mean) * gain`, scale RGB theo `newL/L`, đẩy hue nhẹ theo face rồi renormalise về đúng `newL`. **Giữ nguyên chi tiết texture** (shingle course / coursing / vân gỗ) vì chỉ dịch + giãn tương phản quanh mean, không tô phẳng.
- Bảo vệ: `protect` rect (cửa + jamb + hood; hốc portal/rune ring) **byte-identical**; `is_emissive` (ấm ≥ r130 & r>b+40, cyan ≥ b110 & b>r+30) không bao giờ grade; `dark_floor=40` giữ nguyên viền tối / hốc lõm / outline; `bright_ceiling=150` giữ highlight. Mỗi pixel grade đúng 1 lần theo `priority` (soffit / reveal / return / pitch / side / plinth / front).
- `tools/mapforge_p23_architecture.py`: thêm `--only` (sinh đúng asset được liệt kê, không đụng 10 asset khác) và gọi `apply_face_grade()` sau `seal_seams`/`prune_specks`, trước `validate()`. **Không đổi geometry, alpha, kích thước, pivot, vị trí cửa, anchor, collider, `.72`, depth, save, `FilterQuality.None`**.
- `.github/workflows/lyra-render-evidence.yml`: thêm path filter P25 + bước `Verify P25 face grammar and asset invariants` + guard snapshot baseline.
- Sinh lại **đúng 3 asset**: `hero_quarters.png`, `restaurant.png`, `summoning_hall.png` + `manifest.json` (`stamp()` chỉ refresh 3 entry).

### Pre-flight calibration (KHÔNG nới rubric)

Lần sinh đầu: `|roof_key−front|` FAIL ở `restaurant` (0.51) và `summoning_hall` (6.35) do ROI gồm cả viền tối được bảo vệ kéo mean xuống. **Giữ nguyên 6 ngưỡng**, chỉ chỉnh target của mái: `restaurant roof_key 68→56`, `front 80→88`; `summoning_hall roof_key 78→66` (mái slate/pediment là họ giá trị **tối hơn** tường đón sáng — đúng kiến trúc). Không đổi ngưỡng, không đổi ROI, không đổi pixel cửa.

### Kết quả local (trước CI, KHÔNG phải engine render)

| asset | front−side | roof−return | \|roof−front\| | front−soffit | roof spread | door gap | Kết quả |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `hero_quarters` | 19.77 | 11.13 | 13.86 | 41.01 | 43.80 | 27.12 | PASS 6/6 |
| `restaurant` | 23.51 | 23.74 | 15.67 | 53.21 | 16.60 | 39.06 | PASS 6/6 |
| `summoning_hall` | 33.16 | 22.12 | 18.16 | 60.02 | 32.60 | n/a (zone-only) | PASS 5/5 |

`tools/p25_validate_faces.py`: **40 PASS / 0 FAIL** — `git diff --check`, generator `--check`, `validate_assets.py`, alpha channel byte-identical, ground row 167/171/205, protect rect 0 drifted px, manifest SHA/size, **sinh lại 2 lần cho byte giống hệt**. SHA: `hero_quarters c8ae5d0c…`, `restaurant 517f0e18…`, `summoning_hall bc8adb3c…`. Soi trực tiếp 3 PNG mới: mái có pitch/return tách, tường trước ấm & sáng hơn, mặt hồi xanh-tối, band soffit tối, cửa sổ/cửa/hốc portal/rune cyan còn nguyên.

**Chưa có**: ảnh engine AFTER, QA mềm từ ảnh engine, số đo full-frame before/after trên candidate, CI trên SHA code ứng viên, PR. Các bước đó là REAL ENGINE RENDER → VISUAL QA → REGRESSION ở ROUND 2. `PHYSICAL DEVICE` / GPU / FPS / nhiệt = `NOT TESTED`.
