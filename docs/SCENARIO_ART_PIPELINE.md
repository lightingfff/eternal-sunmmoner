# ETERNAL SUMMONER — Scenario Art Pipeline

> Tổng hợp từ 10 Scenario Skills đã đọc verbatim, chuyển thành quy chuẩn áp dụng cho dự án.
> Tài liệu tham chiếu bắt buộc khi tạo/chỉnh sửa asset.

## 1. Skills đã đọc & phân loại

### Trực tiếp áp dụng cho Sanctuary (10)
| # | Skill file | `name` | Vai trò |
|---|------------|--------|---------|
| 1 | `SKILL.md` | `scenario-game-assets` | Sprite/icon/tileset/isometric/pixel art, transparent, pixel cleanup, parallax |
| 2 | `SKILL-2.md` | `scenario-textures` | Seamless tileable PBR, texture upscale tiling-safe |
| 3 | `SKILL-3.md` | `scenario-sprite-animation` | Walk/idle/attack cycles, GIF, spritesheet, seamless loop, video-to-frames |
| 4 | `SKILL-16.md` | `scenario-side-view-game-kit` | Magenta-key sprites, shared palette, loop search |
| 5 | `SKILL-13.md` | `scenario-image` | txt2img/img2img, sizing/prompt/reference per-model |
| 6 | `SKILL-15.md` | `scenario-gpt-image` | GPT Image 2.5 Flare/Sunburst, sizing grid, transparent cutout |
| 7 | `SKILL-17.md` | `scenario-patina-retexture` | PATINA 5-map PBR (albedo/normal/roughness/metalness/height) |
| 8 | `SKILL-14.md` | `scenario-sparc3d` | Image-to-3D watertight mesh |
| 9 | `SKILL-12.md` | `scenario-asset-analysis` | asset_caption/describe/analyze/detect, QA batch |
| 10 | `SKILL-11.md` | `scenario-storyboards` | Panel-by-panel consistency, 1 run = 1 panel |

### Không trực tiếp (music/meta/PR)
- `scenario-ace-step` (SKILL-18) — music generation
- `scenario-model-training`, `scenario-gemini-image` — useful but not current pipeline
- `Skill Development`, `skill-creator`, `pr-handle-comments`, `pr-squash-message`, `pr-summary`, `scenario-workflows`, `scenario-workflow-authoring` — workflow/PR automation

## 2. Quy trình lõi (từ `scenario` skill gốc)
Mọi generation tuân thủ loop:
```
recommend (capability + prompt người dùng nguyên văn)
→ model_schema_get (đọc contract thực tế: size field, prompt limit, reference cap, background enum)
→ dry_run=true (định giá, đọc cost_impact fields)
→ model_run wait=false (launch theo wave dưới concurrency ceiling)
→ jobs_wait (re-call pending_job_ids, không poll job_get, không retry model_run khi timeout)
→ asset_display → asset_download (png giữ alpha)
```

## 3. Ba trường thất bại (scenario-image)
- **Size:** Mỗi model có họ sizing khác: `width/height` min/max/step (thường step 16), hoặc enum `aspectRatio/resolution/quality`, hoặc chỉ ratio. Giá trị off-step bị từ chối; request dưới floor bị thay thế im lặng. Sau mỗi run kiểm `asset_get properties.width/height`.
- **Prompt length:** 2000–32000 chars, khác nhau giữa models. Prompt vừa với model A có thể 400 ở model B.
- **References:** Tên field (`referenceImages` vs `image`), cap, cardinality (`array:true`?) đều từ schema. String trần khi schema đòi array sẽ bị drop im lặng.

## 4. Isometric & Tileset (scenario-game-assets)
- **Chọn grid trước style:** 2:1 diamond (Sanctuary dùng 2:1), không đổi projection giữa chừng.
- **Footprint ≠ silhouette:** Cây/nhà cao vượt tile, phải test assembled map 3×3 kiểm joins/path/occlusion.
- **Tileset contract:** Engine cần đủ edge/corner (Wang). Member tileset thời điểm viết skill chỉ promise Wang đơn giản, không 47-tile blob — map sang autotile rules thực tế.
- **Variation batch:** Crop sát bounds object, strip baked shadow/glow trước khi gen, nêu rõ stretchable (nine-slice) trong prompt, verify bounding box alpha.
- **Masked fill:** Đọc polarity & base-image field từ schema, template white-inside → adapt alpha nếu cần.

## 5. Texture & Material (scenario-textures + patina-retexture)
- **Seamless:** Generator `model_scenario-texture` append tileable hint tự động, `eraseSeam=true` + `overlap/featherRadius` để xóa seam.
- **Upscale tiling-safe:** Chỉ dùng `model_sc-upscale-flux-texture` (preset precise/balanced/creative + strength). Generic upscaler phá tiling.
- **Engine sizing:** Square power-of-two (1024 default, 2048 nếu cần). Gen 1024 rồi upscale 2×, không gen 2048 trực tiếp (cap 3840).
- **PBR maps:** 2D extractor `model_patina` trả 5 assets (`texture-albedo/normal/smoothness/metallic/height`), đọc `metadata.type` để map — không đoán order. `smoothness` label có thể thực chất là roughness (author đã gặp brass map tối hơn matte gray). PATINA prompt chỉ mô tả bề mặt phẳng seamless, không scene/object.

## 6. Sprite Animation (scenario-sprite-animation)
- **Seam check:** `delta_seam ≤ mean_baseline_delta` của window đang loop. Frame 0 thường diverge — search window bên trong, loại frame 0 nếu delta 0→1 lớn.
- **Frame order:** `assetIds` từ extractor có thể lệch. Verify qua grid maker contact sheet so với `firstFrame/lastFrame` trước mọi stride math.
- **Grid pipeline:** `model_scenario-video-to-image-seq` → subset → `model_scenario-grid-maker` → `model_scenario-image-slicer` (cap 6×6, row-major) → `model_scenario-image-seq-to-video` (fps 1–120, gif/mp4). FPS timing duy nhất sau gen — chọn frame count trước.
- **Pixel snap:** Re-detect grid + quantize palette đồng nhất mỗi tile, cùng color count/seed.

## 7. Side-view Kit Principles áp dụng cho Sanctuary 2.5D
Mặc dù Sanctuary là oblique 3/4, nguyên tắc key-color & loop áp dụng khi gen spritesheet hero:
- Magenta pure cho keying — không để pink/purple trên character.
- Idle/hurt pinned first=last, run/walk chỉ first frame (pin 2 đầu làm clip speed-up/slow-down).
- `--drop` frame bị white slash block, `--range` widen trước khi re-run.
- Shared palette/canvas cho mọi cycle của 1 character.

## 8. Asset Analysis & QA (scenario-asset-analysis)
- `asset_analyze` batch 10 ảnh/1 instruction với output shape cố định (`<index>: pass|fail, <reason>`). `num_outputs` ≠ batch size.
- `asset_detect` strip background mặc định true — set false khi cần whole-image map.
- Retrieval `search target=assets` cần ít nhất 1 trong `query/image/filters`; keyword mặc định, `query_semantic_ratio` 0.5–0.8 cho mood query.
- Verdict QA phải `asset_download` text asset để đọc.

## 9. Palette & Lighting quy chuẩn cho Sanctuary Dark Fantasy
- **Palette chủ đạo (không phủ tint toàn map):** Deep Forest 0xFF2B352E, Moss Stone 0xFF2A3D2E, Cold Stone 0xFF3A3F4A, Charcoal 0xFF1B1E24, Smoky Violet 0xFF2A2E4A, Aged Bronze 0xFF4A3A2E, Muted Burgundy 0xFF7A3358.
- **Hướng sáng:** top-left directional (0.35, -0.45, 0.82), moonlight cool 0xFF8A9AB8, không phủ vàng đều.
- **Contact shadow:** oval đặc dưới chân `Black α0.65`, directional offset down-right cold 0xFF0A0C14 α0.35.
- **AO:** chỉ chân tường/dưới mái hiên/khe đá (alpha 0.18–0.22), không tối toàn map.
- **Local light:** brazier amber 0xFFFFB844 r90, plaza rune cold 0xFF7DD3FC r160, window warm 0xFFFFC86B r70 — đều radial gradient pulse sin(time*1.8).

## 10. Tool gaps & giới hạn
- **Scenario MCP chưa kết nối trong sandbox này:** Mọi `model_run/asset_get/jobs_wait` không khả dụng local. Thay thế tạm: procedural Kotlin `PixelTileAtlas` + `SanctuaryWorldRenderer` + ImageMagick darken pack.
- **Build Android:** Gradle wrapper 9.3.1, AGP 9.1.1, Kotlin 2.2.10, compileSdk 36 minor 1 — không có JVM local nên không chạy `assembleDebug` để verify, chỉ kiểm tĩnh.
- **Không tuyên bố asset Scenario** khi chỉ dùng Canvas placeholder — ghi rõ nguồn trong commit message.

---
*Tham chiếu khi edit: mọi asset mới phải nêu skill source & schema field đã đọc.*
