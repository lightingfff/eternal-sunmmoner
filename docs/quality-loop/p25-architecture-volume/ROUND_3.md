# P25 — ROUND 3: REGRESSION → FINAL REVIEW (vòng cuối, không sửa art thêm)

**Vòng sửa đã dùng**: ROUND 1 (implement + baseline), ROUND 2 (engine render thật + visual QA + root cause + Phase C). ROUND 3 là **regression + final review**, không phải cycle sửa lỗi, không hạ rubric, không thêm pass runtime. Hết 3 vòng → chốt hồ sơ, không chạy vòng 4.

## 1. Regression trên SHA code `ba1056c` (Phase C, 7 facility)

| kiểm tra | kết quả |
| --- | --- |
| `gh` CI `41622749053` Build Android APK | **success** (đúng `head_sha=ba1056cb6be6f405462fec3a3632f4f75ac0b381`) |
| `gh` CI `41622749135` Lyra engine render evidence | **success** (cùng SHA), 24 step xanh, `Verify P25 face grammar and asset invariants` ✅, `Verify P23 architecture and props contract` ✅, `Render P24 HDR light-response matrix` ✅, `Verify Sanctuary environment art, manifest, layout, and performance regressions` ✅, `Publish` ✅, `Upload` ✅ |
| `git diff --check` | sạch |
| `tools/p25_validate_faces.py` | **85 PASS / 0 FAIL** (7 asset: alpha byte-identical, canvas, foot row, protect rect, manifest SHA/size, determinism, 38 ROI checks) |
| `tools/mapforge_p23_architecture.py --check --only <7>` | rc=0, `col_gaps=0` |
| `tools/validate_assets.py` | PASS |
| `camera_props.json` | `cmp` IDENTICAL với baseline |
| PR #28 overlap | `comm -12` rỗng (29 file gameplay không đụng) |
| `HdrLightResponse.kt` | **không đổi**; OFF/REDUCED/BALANCED, emitter, dark aperture, cached masks giữ nguyên |

Bot commit `98732ba` (chỉ PNG evidence) có `41623486097` / `41623485866` = **`action_required`** (chờ phê duyệt commit bot), **không** phải lỗi build/test. Phân biệt rõ với 4 run `success` trên 2 SHA code.

## 2. Regression đo lường (before = baseline `b2a7162`, after = bot evidence `98732ba`)

- `no-flood`: 3 khung khoá −0.28% / −0.75% / −1.16%; **max |Δmean| toàn 32 khung = 1.88%** (<8%) → PASS. Không khung nào tăng brightness.
- ROI tài sản: **38/38 PASS** trên cả 7 facility, ngưỡng `WORK_ORDER.md` giữ nguyên (front−side ≥12, roof−return ≥10, |roof−front| ≥10, front−soffit ≥18, roof spread ≥14, door ≤ front−20).
- ROI chiếu engine: separation tăng rõ ở z0.88/z1.00 (restaurant front−side 8.10→27.93, registry 9.43→27.41, hall front−soffit −5.55→47.92). `armory` / `synthesis_lab` front−side trên engine vẫn yếu (+4.44 / −2.53) do chồng sprite tiền cảnh + hốc loggia/workshop được bảo vệ; **soi ảnh thật xác nhận khối vẫn tách** (`armory_full_z148.png`, `synthesis_cottage_z148.png`). Không tự nâng điểm, ghi rõ nhiễu.
- `portal_aperture` 20.36 → 20.36, `rune_stage` 103.4 → 103.4: hốc tối Tower Gate và vòng rune Summoning Hall **không đổi pixel**. `tower_gate_whole_z148.png` không glow.
- **Cổng P24 `p90−p10 ≥ +3 trên ≥2/3` = 1/3 (FAIL)** — overview +0.0, service z088 +2.9, tower z148 +3.7. Báo cáo độc lập, **KHÔNG relabel**, không tăng sáng toàn cục để "đạt". P24 vẫn `BLOCKED` về chất lượng dù PR #30 đã merge theo lệnh owner.

## 3. Final review theo logical checkpoint (KHÔNG phải agent độc lập)

Không có tool native spawn/delegate; 6 vai trò là checkpoint tuần tự trong MỘT Arena agent, ghi rõ để không ngụ ý "đội ngũ song song".

| checkpoint | kết luận |
| --- | --- |
| Router / Director | baseline `b2a7162`, PR #30 MERGED / #28 OPEN-Draft không đụng; nhánh mới + PR mới #31; rubric chốt trước candidate; 3 vòng, không auto-merge |
| Environment Architect (lead) | root cause (b)+(g) sửa tại nguồn: giá trị mặt phẳng trong PNG, không phải pass ánh sáng đè; 7 facility cùng grammar, identity riêng (nâu shingle / slate / đá / fortress) |
| Technical Artist | `HdrLightResponse.kt` **không cần đổi**: grade P24 không triệt tiêu separation đã author; không luminance-selective tone curve, không bloom/blur/RuntimeShader, không duplicate light pass |
| Visual QA (critic) | soi 10+ ảnh engine AFTER; B6 **3/4** tại z0.88 & z1.00; 20/24 mềm; nêu khuyết điểm: band soffit Summoning Hall ~13px hơi nặng, background `sanctuary_p03` houses vẫn phẳng, engine ROI armory/synthesis nhiễu chồng lấp |
| Android Performance | 0 draw call / 0 bitmap / 0 path allocate mới; 7 PNG +8.181 B; 7 cached mask giữ nguyên; `ScenePerformanceTest` xanh (CPU/frame pacing only); **FPS/GPU/thermal/device NOT TESTED** |
| Integration Reviewer | `git diff --check`, validator 85/85, `validate_assets.py`, 4 CI success trên 2 SHA code, `action_required` phân biệt, `comm -12` PR #28 rỗng, camera props identical, manifest SHA khớp |

## 4. Quyết định

**B1–B7 (A–F) của P25 đạt**, với E gồm `PASS` (no-flood) + `DISCLOSED` (cổng P24 còn FAIL). Không tuyên bố `100%` / "masterpiece" / "1000% success". Không tự merge. PR #31 để owner review. Các hạng mục còn mở liệt kê có chừng mực trong `BLOCKERS.md` dưới dạng **sub-ticket kế tiếp**, không phải `BLOCKED` cho toàn P25 và không phải lý do hạ rubric.

`LOCAL BUILD NOT RUN` (không JDK/Gradle/Android SDK); `EMULATOR` / `PHYSICAL DEVICE` / GPU / FPS / nhiệt = **NOT TESTED**.
