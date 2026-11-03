# BLOCKERS — `p24-sanctuary-hdr` (trạng thái cuối)

| ID | Blocker | Kết quả | Bằng chứng |
| --- | --- | --- | --- |
| B1 | CI thật trên candidate SHA | **PASS** | `41589948067` success (attempt 2) + `41589948058` success, 15/15 step. Attempt 1 chỉ lỗi tải lại artifact ZIP, đã rerun xanh. |
| B2 | 24 trước/sau + 8 HDR cùng camera 0.36/0.88/1.00/1.48 | **PASS** | 24 PNG BEFORE / 7 356 408 B + 32 PNG AFTER; `camera_props.json` diff rỗng. |
| B3a | Không flood sáng (`\|Δmean\|<8%`) | **PASS 3/3** | +0.01% / −0.29% / +1.09%; lớn nhất toàn 24 khung 3.60%. |
| B3b | Tương phản cục bộ tăng ≥3 trên ≥2/3 khung | **FAIL 1/3** | +0.0 / +0.0 / +5.4. Xem `ROUND_4.md`. |
| B4 | Hợp đồng `.72`, anchors, collider, door, save, `FilterQuality.None` | **PASS** | `MapForgeP23ContractTest`, `SanctuaryEnvironmentArtTest`, `ScenePerformanceTest`, `DepthAddendumTest` xanh trong `41589948058`; `comm -12` với 29 file PR #28 = rỗng. |
| B5 | OFF/REDUCED ⊂ BALANCED, không fullscreen effect | **PASS** | `hdr_off` 406 741 B / `hdr_reduced` 443 296 B / `hdr_balanced_day` 669 462 B; không RenderEffect/RuntimeShader/blur trong diff. |
| B6 | "Không còn glow nhưng vẫn phẳng" ở zoom chơi 0.88/1.00 | **FAIL** | Soi trực tiếp `service_cluster_z100.png`: mái/mặt tiền/mặt bên vẫn đồng giá trị dù đã có quầng ấm. |
| B7 | Sáu chiều mềm mỗi ≥3, tổng ≥20/24 | **FAIL 19/24** | visual coherence 2, technical 4, player clarity 3, material 3, polish 3, efficiency 4. |

## Kết luận

**`BLOCKED` — KHÔNG TỰ NHẬN PASS.** CI xanh nhưng ảnh ở zoom chơi chưa đạt premium; không đổi cổng nghiệm thu để tự hợp thức hoá. PR #30 giữ `OPEN`, không merge.

## Không phải blocker nhưng phải ghi rõ

- `LOCAL BUILD NOT RUN`: sandbox không có JDK/Gradle/Android SDK; mọi build/test lấy từ GitHub Actions thật.
- `EMULATOR / PHYSICAL DEVICE / GPU / FPS / nhiệt = NOT TESTED`: không có thiết bị. `ScenePerformanceProbe` chỉ là CPU submission/frame pacing. Không có số FPS nào được phát minh.
- 2 run trên bot SHA `dcdc870` (`41590628143`, `41590628175`) = `action_required` (commit bot chờ owner approve) → `gh pr checks` báo "no checks reported" ở head SHA. Bằng chứng CI gắn đúng SHA code `c0e2c03`.
- Không có tool subagent native; 6 vai trò là checkpoint tuần tự trong một Arena agent.
