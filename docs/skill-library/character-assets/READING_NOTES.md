# Ghi chú đọc và cách áp dụng

## Độ sâu đã đọc

Đã lập danh mục toàn bộ **181 SKILL.md** ở ba commit trong README: đọc metadata,
mô tả và mục lục để phân loại. Không tuyên bố đã đọc từng dòng 2.109 file Scenario,
mọi script/tutorial Meowa hoặc chạy thử toàn bộ dịch vụ. File phụ được giữ để đọc
theo nhiệm vụ; việc có file không phải bằng chứng nó đã được kiểm thử.

Đọc kỹ trong lượt này:

- Character: README, `SKILL.md`, `references/sheet-spec.md`, `agents/openai.yaml`.
  Quy trình: reference → base sprite → từng strip action/direction → assemble/cleanup
  → geometry/motion/hierarchy/provenance → xem contact sheet và GIF/WebP → sửa strip
  yếu. Bộ script được kiểm kê hash, chưa chạy/chưa audit toàn bộ. README nêu MIT,
  nhưng không có LICENSE/full notice; giữ ghi chú nguyên bản và link, không vendor code.
- Meowa: README, `skills/game-assets/SKILL.md`, `references/capability-routing.md`,
  `tutorials/game-integration/frame-animation.md`,
  `tutorials/frame-animation/animation-alignment.md`,
  `tutorials/frame-animation/animation-export.md`,
  `tutorials/frame-animation-postprocess/README.md`, `speed-and-trim.md`.
  Các module tạo ảnh, sửa UI, map/texture, animation/video, audio, game-design,
  game-development và local-tools đã kiểm kê; tài liệu chi tiết còn lại chưa đọc hết.
- Scenario: bản hiện tại `scenario-sprite-animation/SKILL.md`,
  `references/directional-cycles.md`, `scenario-asset-analysis/SKILL.md`,
  `scenario-task-tracking/SKILL.md`. Đối chiếu cả 179 entrypoint với bản ZIP;
  9 skill art/process đã đọc ở nhiệm vụ cũ tiếp tục có trong snapshot cũ, nhưng
  sprite-animation và asset-analysis phải đọc bản mới. Bốn skill mới về comic,
  UA video, kinetic film, playable ads được phân loại theo mô tả/mục lục; không
  giả vờ đã đọc kỹ mọi workflow quảng cáo không thuộc nhiệm vụ game hiện tại.

## Những điều học được có thể dùng ngay

**Character Animation Creator:** Không yêu cầu model vẽ trọn bộ nhiều hướng/mọi
động tác trong một ảnh rồi mặc nhiên cắt đều. Khóa identity và camera, thử strip
đơn trước. Walk cần các tư thế khác nhau rõ; melee có anticipation/contact/recovery;
archer có draw/release/recoil. Giữ nguồn thật, manifest và preview động. 32/64/128
native là ba ngân sách pixel riêng; bản resize chỉ được gọi là biến thể scaled.
Hướng north phải thật sự thấy lưng; mặt trái/phải không được làm đổi vũ khí bất ngờ.

**Meowa:** Chọn một capability chính theo asset contract thay vì ghép nhiều bước
tạo sinh tốn tiền và làm trôi identity. Căn giữa canvas không tương đương căn chân.
Chỉ bù một offset cố định cho toàn clip khi lỗi do padding; không kéo từng frame
về centroid vì xóa nhảy/lean/knockback. Giữ nguồn và bản game riêng, lưu frame/time
mapping sau trim; âm thanh và hit/release marker phải cùng timeline mới. Large VFX
và projectile tách khỏi body. UI extraction có thể chỉ trả aggregate sheet, không
được quảng cáo là từng component độc lập. Video quảng bá không thay asset tương tác.

**Scenario:** Route pixel model, single-sheet hoặc video/keyframe theo yêu cầu
điều khiển thực, không theo tên model cố định. Count/size/frame order của kết quả
phải đo lại: job assetIds từng có thứ tự sai; timestamp không sửa được thứ tự.
Loop seam phải so với biên độ chuyển động của chính cửa sổ clip và xem preview;
đo chênh lệch thấp không đủ vì ảnh đứng im cũng đạt. Dùng palette chung để tránh
nhấp nháy. Tool phân tích nhìn render không đo chính xác byte/hex/alpha của file.
Ngay cả analysis và thao tác ở read lane có thể tốn credits: kiểm tra schema/giá
thực và quyền hiện có trước khi gọi. Enterprise task attribution không phải quyền
được tự đặt task_id cho tài khoản người dùng.

## Quyết định cho code game hiện tại

Baseline `380e4e3ba4840e673e54966715ff12a49cbdfab4`; đã xem loader/manifest thật:

| Hợp đồng | Code/file hiện tại | Cách dùng kiến thức mới |
|---|---|---|
| Enemy atlas 12 cột, cell 128×144, pivot (64,132); contact .48s, hit .22s | [TowerMonsterAtlas.kt](../../../app/src/main/java/com/example/game/pixel/TowerMonsterAtlas.kt), [manifest.json](../../../app/src/main/assets/tower_combat_v1/manifest.json) | Không ép sang default 64×64/sáu frame của skill. Nếu thay format phải có integration riêng; art không đổi thời điểm damage. |
| VFX 4 hàng × 8 cột, 96×96, pivot (48,84), dùng state có sẵn | [TowerCombatFxAtlas.kt](../../../app/src/main/java/com/example/game/pixel/TowerCombatFxAtlas.kt), [vfx_manifest.json](../../../app/src/main/assets/tower_combat_v1/vfx_manifest.json) | Xuất từng loại effect rõ hướng/impact; không chuyển damage sang renderer hay gắn mọi effect vào nhân vật. |
| Combined pack guard 8 MiB, preload/cache, finite fallback | Hai loader ở trên | Tính cả mirrored bitmap/cropped frames, kiểm tra bộ nhớ giải mã; không decode trong draw. Không coi PNG nhỏ đồng nghĩa RAM nhỏ. |
| Upright height và foot/ground tách nhau | [ObliqueSpriteGeometry.kt](../../../app/src/main/java/com/example/game/pixel/ObliqueSpriteGeometry.kt) | Giữ oblique Y=.72, feet/pivot, shadow, depth và canopy; không làm dẹt chiều cao sprite theo mặt đất. |
| Simulation owns contact/death/projectiles | [TowerCombatEngine.kt](../../../app/src/main/java/com/example/game/combat/TowerCombatEngine.kt), [LostCourierEncounter.kt](../../../app/src/main/java/com/example/game/combat/LostCourierEncounter.kt) | Hiển thị theo simulation; animation retime không được gây hit hai lần hoặc bỏ hit khi dt lớn/pause/retry. |

### Xung đột giữa nguồn cần giải quyết

1. Scenario có ví dụ căn body box theo từng tile; Meowa cảnh báo căn từng frame làm
   mất chuyển động. Với game này, ưu tiên root/foot metadata và cố định offset theo
   clip; chỉ sửa drift riêng khi có bằng chứng phân biệt với chuyển động chủ đích.
2. Character mặc định strip 6 frame, Scenario thường 8, Meowa có nhiều tier. Số
   frame, thời lượng và pivot do task/loader game quyết định, không dùng default nguồn
   để ghi đè atlas hiện có. Không gọi mirrored art là hướng được vẽ riêng.
3. Meowa nói pixel output đã pixelated; Scenario có bước snapping. Đo file thật và
   xem tại scale game trước; giữ atlas hợp lệ, không tái quantize mù quáng.
4. Meowa có map 2:1 isometric/top-down và workbench TypeScript/Three.js; Scenario
   có Unity/Godot/Unreal/DCC. Chỉ mượn phương pháp; không đổi engine/project/camera.
5. Character cấm procedural art để giả kết quả reference test. Điều đó không cấm
   fixture kiểm thử hoặc phụ hiệu ứng code đã được giao trong game; mọi bằng chứng
   phải ghi đúng nguồn. Không thay yêu cầu asset thật bằng hình giả lập.
6. Scenario khuyến nghị upload kết quả vào collection. Trong nhiệm vụ local/offline
   này, không upload private art hoặc dùng tài khoản chỉ vì nguồn bảo vậy. Kiểm tra
   quyền/task/budget thực; thiếu provider thì ghi rõ, không bịa kết quả dịch vụ.

## Delta Scenario

Thêm: `scenario-comic-creator`, `scenario-game-ua-ads`, `scenario-kinetic-film`,
`scenario-playable-ads`, `scenario-task-tracking`.

Đổi: `skills-pr-handle`, `skills-validate`, `scenario-blender-blockout`,
`scenario-blender-export`, `scenario-3d`, `scenario-asset-analysis`,
`scenario-kinetic-music-video`, `scenario-model-training`, `scenario-sprite-animation`,
`scenario-video-editing`, `scenario`. Danh sách source path/hash chính xác trong
inventory, 163 file không đổi đã kiểm tra bằng hash thay vì chép lần nữa.

## Giới hạn

Chưa cài/cấu hình Meowa hoặc Scenario, chưa gọi API, chưa phát sinh generation job,
chưa chi credits/huấn luyện model. Chưa kiểm chứng khả năng dịch vụ, giá, model hay
đảm bảo “perfectly pixelated” được ghi trong nguồn. Không chạy runner/script nguồn.
Các kiểm tra cú pháp/hash của bản lưu không phải security audit toàn bộ SDK.
Không sửa runtime/assets/Android config; không có APK mới hay phép đo điện thoại
cho nhiệm vụ tài liệu. Học/lưu skill chưa chứng minh animation/gameplay đã hay hơn.
