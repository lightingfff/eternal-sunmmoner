# ES-A03-001 — Audit renderer / chuẩn bị P31-B (không tích hợp)

## Baseline và trạng thái

2026-11-08, main `bdff6f9976dc4927e1e095a503bc339509c7a9aa`.
A03 / Arena.ai session 788e635f, GitHub @lightingfff. Đây là audit nguồn và
kiểm tra metadata, **không phải nghiệm thu hình ảnh hay P31-A3**.

Nguồn điều phối đã đọc trên GitHub:
- [#39 và bình luận mới nhất](https://github.com/lightingfff/eternal-sunmmoner/issues/39): dual lead A01/Argon, Codex + Argon + A09 review.
- [#54](https://github.com/lightingfff/eternal-sunmmoner/issues/54): READY, read-only audit, không runtime.
- [#51](https://github.com/lightingfff/eternal-sunmmoner/pull/51): MERGED lúc 2026-11-08T04:32:52Z.
- [#45](https://github.com/lightingfff/eternal-sunmmoner/issues/45): integration freeze, art QA trước runtime.
- [#59](https://github.com/lightingfff/eternal-sunmmoner/issues/59): BLOCKED, chờ bundle thực; không nhận thay A08.
- [#50](https://github.com/lightingfff/eternal-sunmmoner/pull/50): OPEN, head `e43dea26d9de69a8b0ada860a2ba551ba0ff79e1` khi kiểm tra. Không sửa branch này.

## Bản đồ giao diện thực tế

Các đường dẫn dưới đây tính từ repo root, tại baseline trên.

| Giao diện | Nguồn / symbol | Ràng buộc tích hợp sau này |
|---|---|---|
| Projection | `app/src/main/java/com/example/game/map/WorldMapModels.kt:13–32`, `ObliqueCamera` | X=(wx-camX)*zoom+halfW; Y=((wy-camY)*.72-elevationZ)*zoom+halfH. Chỉ mặt đất bị nén Y; chiều cao sprite không nhân .72. |
| Terrain | `app/src/main/java/com/example/game/pixel/SanctuaryWorldRenderer.kt`, `drawSanctuaryFloatingWorld` | Polygon clip đảo, 64 world px/tile, culling theo camera; `GRASS_BLOCK_MIN_TILE_SCREEN_PX=25`, overview sheet/block khác gameplay tiles; giữ nearest-neighbor và seam overlap. |
| Asset cache | `app/src/main/java/com/example/game/pixel/SanctuaryArtAssets.kt`, `visualProfile`, `assetSafe`, `p27bFamily` | Profile hiện P27B, optional PNG fallback, family negative-cache. `asset()` có decode non-null assertion, không dùng làm safe fallback cho optional P31. Không decode/đọc JSON mỗi frame. |
| Geometry | `app/src/main/java/com/example/game/pixel/ObliqueSpriteGeometry.kt`, `bounds`, `pixelScale`, `drawFootOffset` | Alpha threshold 128, cache bounds; tree foot từ bottom padding, building giữ offset sẵn. Bounds hiện quét cả bitmap: không áp thẳng cho sheet nhiều frame nếu cần foot ổn định từng frame. |
| Draw prop | `app/src/main/java/com/example/game/pixel/PixelSpriteRenderer.kt`, `drawProp`, `drawBuildingPlinth` | Chiếu elevationZ trước, scale zoom*obs.scale, culling; giữ foundation, cửa và footprint tách khỏi silhouette. |
| Directional shadows | `app/src/main/java/com/example/game/pixel/SanctuaryEnvironmentShadows.kt`, `ObliqueSpriteGeometry`; `PixelSpriteRenderer.drawCastersShadow` | Hình học shared bounds/scale; dùng SanctuaryLight, height projection. Không thay bằng vòng tròn tổng quát. |
| Depth / occlusion | `app/src/main/java/com/example/ui/screens/SanctuaryLobbyScreen.kt:154–203,577–647`; `PixelSpriteRenderer.drawActorCanopyOcclusion` | Props sort y+sortYBias, actor y; merged list; actor-local canopy clip sau actor, foreground walls sau. Không append mọi effect ở cuối Canvas. |
| Older loader | `app/src/main/java/com/example/game/pixel/SanctuaryAssetManager.kt`, `init` | Đọc sanctuary_pack manifest, không phải P31 ASSET_MANIFEST adapter. Không nhầm hai pipeline. |
| UI notices | `app/src/main/java/com/example/ui/components/p28/ESNotices.kt`, `NoticeCenter` | MAX_VISIBLE=3 / MAX_RETAINED=8 là UI, không phải world VFX budget hay durable event queue. |

Tìm kiếm `sanctuary_p31|ASSET_MANIFEST|loopMode` trong `app/src/main/java` không
có kết quả ở baseline. Đây chỉ là bằng chứng static về các literal này, không
chứng minh tuyệt đối không tồn tại đường dẫn dựng động.

## Atlas / pivot / lifecycle

`docs/art/sanctuary_revamp/ASSET_MANIFEST.json` có 12 entries: 8 GENERATED,
4 PROPOSED (2 actor routines + 2 UI). Không gọi đây là 12 PNG P31-A3 đã nhận.

| GENERATED VFX | Sheet / frame / count | FPS | Pivot / anchor | Cần quyết định trước runtime |
|---|---|---|---|---|
| Lantern | 64×24 / 16×24 / 4 | 4 | (8,24), bốn worldAnchors | Loop/lifecycle và elevation ngọn đèn, day/night, fallback không vẽ hai lamp chồng nhau. |
| Tower rift | 1024×160 / 128×160 / 8 | 6 | (64,140); world (1960,635), Z=215 | Giữ authored pivot; `portal_interior_underlay` phải nằm trong gate, không phủ facade. Chưa có runtime evidence để kết luận pivot sai. |
| Forge smoke | 144×48 / 24×48 / 6 | 5 | (12,48); world (1095,880), Z=86 | `foreground_atmosphere`, culling/budget riêng; manifest đề nghị omit trên LOW; chưa có lifecycle/loop. |

Atlas widths khớp frameWidth*frameCount, height khớp frameHeight. FPS đã có,
không yêu cầu thêm lần nữa. Thiếu `loopMode` rõ ràng trong manifest hiện tại;
không mặc định cyclic/pingpong/one-shot hoặc thời hạn event.

Công thức đề xuất cho sprite upright ở scale s (s bao gồm zoom):
`dstLeft=projectX(anchor)-pivotX*s`,
`dstTop=projectY(anchor,elevationZ)-pivotY*s`;
source rect phải crop đúng frame trước. Đây là **spec**, chưa phải code shipped.
Không tự đổi rift thành (64,160). Cần render chồng gate và kiểm tra pivot tại
.36/.88/1.48 trước quyết định của A08/A03/A01.

## Bằng chứng kiểm tra thực và lỗ hổng gate

Local Python đọc JSON và bytes từng GENERATED targetPath: **8/8 sizeBytes và
SHA-256 khớp manifest**. Đây không phải decode/alpha/seam QA.
Không chạy main của `tools/verify_p31_assets.py` vì nó ghi đè report ngoài scope.
Import module rồi gọi `verify_png()` với bản tower_rift chỉ giữ 33 bytes
(signature + IHDR, thiếu IDAT/IEND): trả `True`. Negative probe đã tái hiện
validator chấp nhận file không đầy đủ. Không đổi verifier (owner A08).

Static review verifier: chỉ parse signature/IHDR; không kiểm CRC, decode IDAT,
manifest hash/alpha/frame grid; `exp_ct` chưa được dùng và `all_valid=False`
không chuyển thành nonzero exit. Không xem dòng “Valid PNG” là art acceptance.

## Kế hoạch sau gate — chưa cấp quyền sửa

1. A08 nhận đúng bundle và art-only PR; kiểm decode đầy đủ, CRC, hash, alpha,
   negative cases, provenance, preview không cắt và loop/lifecycle. Giữ PNG hợp lệ.
2. A03/A08 kiểm pivot + frame jitter bằng engine, A10 định budget world effects
   riêng theo quality/culling; không lấy UI MAX_VISIBLE làm budget đo được.
3. A01 xác nhận #50 và art gate đã qua review/owner merge, cấp READY issue mới
   với exact code paths và sole writer. Không tự mở scope runtime từ audit này.
4. Adapter future: semantic actor/facility anchor + stable source/event IDs;
   resolver chỉ phục vụ visual, fallback/no-op khi thiếu VFX. Không teleport hero,
   reward/save mutation hay ack event từ draw loop; Codex duyệt contract #46.
5. Build APK và captures engine thật before/after ở .36/.88/1.48, gate interior,
   chimney, day/night, actor bắc/nam/partially under canopy; kiểm cache/fallback
   thiếu hoặc corrupt asset. A09 regression và A10 device profiling riêng.

## Giới hạn và review

Không chạy Android build/unit test, không render/capture runtime, không inspect
ảnh art trong audit này, không đo FPS/thermal/memory/device. Không có visual PASS.
Không xác nhận CI của PR khác. Hash match không mở khóa P31 integration.
Yêu cầu Argon + Codex + A09 review đúng PR head SHA, A08/A10 specialist review.
Reviewer read-only; mọi sửa sau review cần FIX_PLAN một writer/file. User tự merge.
