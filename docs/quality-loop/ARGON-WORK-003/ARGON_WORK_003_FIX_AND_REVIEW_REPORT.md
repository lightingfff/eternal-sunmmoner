# ARGON-WORK-003 (Issue #82) — Corrective Art Authoring for 7 `NEEDS_FIX` Assets & Playback/Candidate Evidence for 3 `BLOCKED` VFX

- **Issue:** `#82` (`[ARGON-WORK-003][P0] Fix 7 rejected Sanctuary V2 assets; resolve 3 VFX blockers after independent A08 QA`)
- **Sole Writer:** `GEMINI 4 ARGON HIGH — Creative & Production Co-Lead`
- **Coordinator:** `Arena A01`
- **Independent Reviewer:** `A08` (with visual review by `A03`, `A09`, `Codex`)
- **Source Immutable QA Referenced:** Issue `#77` (`ES-A08-002`), Draft PR `#81` (`76b95c1dc74c2b4de443f508cfaec266b964eac1`)
- **Baseline Asset Pack SHA:** `17c99aea77860958346acf59cecab18aa514c9c1`
- **Proposed Branch:** `argon/work-003-fix-7-assets-issue82`

---

## 1. Formal `[CLAIM]` Record

```markdown
[CLAIM] Tôi sẽ phụ trách phần này — ARGON-WORK-003 (Issue #82)
- Agent: GEMINI 4 ARGON HIGH (Creative & Production Co-Lead)
- Runtime: AI Studio Android Cloud Build Container (Python 3.11 PNG/GIF verification + Gradle compile_applet)
- Base SHA: 17c99aea77860958346acf59cecab18aa514c9c1
- Proposed Branch: argon/work-003-fix-7-assets-issue82
- Owned Files Edited in docs/art/argon_production_v2/:
  1. docs/art/argon_production_v2/batch_01_terrain/terrain_forest_grass_v1_64.png
  2. docs/art/argon_production_v2/batch_01_terrain/terrain_forest_grass_v2_64.png
  3. docs/art/argon_production_v2/batch_01_terrain/terrain_forest_grass_v3_64.png
  4. docs/art/argon_production_v2/batch_01_terrain/terrain_compacted_earth_64.png
  5. docs/art/argon_production_v2/batch_01_terrain/terrain_tower_gate_runic_stone_64.png
  6. docs/art/argon_production_v2/batch_01_terrain/terrain_grass_stone_transition_64.png
  7. docs/art/argon_production_v2/batch_04_animated_vfx/vfx_tower_gate_portal_swirl_8f.png
  8. docs/art/argon_production_v2/ASSET_MANIFEST_V2.json
- Owned New Evidence Directory:
  - docs/quality-loop/ARGON-WORK-003/**
- Read-Only / Bit-Identical Preservation Verified:
  - 14/14 PASS assets in docs/art/argon_production_v2/** preserved 100% bit-identical to 17c99aea77860958346acf59cecab18aa514c9c1.
  - 3/3 BLOCKED VFX in docs/art/argon_production_v2/batch_04_animated_vfx/** preserved 100% bit-identical (HELD per Issue #82 read-only path lock; candidate reworks + animated GIF playbacks placed in docs/quality-loop/ARGON-WORK-003/).
  - 0 changes to app/**, build.gradle.kts, metadata.json, old P31 assets, or docs/quality-loop/ARGON-WORK-002/**.
```

---

## 2. Exact Fixes Applied to the 7 `NEEDS_FIX` PNG Files

| # | Asset Path (`docs/art/argon_production_v2/...`) | A08 QA Finding on Baseline `17c99aea` (`76b95c1`) | Root Cause in Baseline | Corrective Art Fix in `ARGON-WORK-003` | Old SHA-256 -> New SHA-256 | New Bytes & IHDR / IDAT CRC32 | New Seam / Frame Bounds Proof |
| :-: | :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| **1** | `batch_01_terrain/terrain_forest_grass_v1_64.png` | `4×4 lộ cụm thẳng đứng lặp; cần giảm motif/bổ sung biến thể trước phủ nền rộng.` | Low-frequency `sin(x * 2π/64)` vertical wave + 18 large vertical tuft stamps created 4 repeating vertical columns in `4×4`. | Removed all periodic wave functions; synthesized an isotropic cellular turf with 96 horizontal-fanned (`Y=0.72`) grass blade micro-clusters + 8×8 sub-block, column, row, and diagonal luminance equalization. | Old: `882b6b3f3028e28a...`<br>New: `7061c0a1f3e497536ef9a23d4f0bfd14e04d94e87def6453bcf55145dc8dd335` | `8,331 B`<br>`0xaa6971de` / `0xfca67b9c` | `L-R delta: 1.51`<br>`T-B delta: 2.01`<br>Luma: `43.19` |
| **2** | `batch_01_terrain/terrain_forest_grass_v2_64.png` | `Dải chéo sáng/tối lặp 64px rõ ở 4×4.` | `sin((x+y) * 2π/64)` created diagonal stripes repeating every 64px. | Removed all `(x+y)` / `(x-y)` harmonics; synthesized 64 stratified 3-leaf clover rosettes & micro slate pebbles with strict main/anti-diagonal (`(x±y)%64`) luminance equalization. | Old: `0ada6311a9398789...`<br>New: `fac3d8e0949fa54feecd35d13b517ec32bf50e42949b3aaa8f5e7c1e865ead61` | `8,049 B`<br>`0xaa6971de` / `0xf048b71a` | `L-R delta: 0.97`<br>`T-B delta: 1.34`<br>Luma: `43.26` |
| **3** | `batch_01_terrain/terrain_forest_grass_v3_64.png` | `Đốm đỏ dạng vòng lặp theo lưới; rất dễ nhận ra ở 1×.` | Circular rings of reddish-ochre pine needles around 12 pockets formed high-contrast red ring hotspots on `4×4`. | Removed all circular ring stamps and red/ochre pixels; synthesized deep forest humus, damp viridian moss cushions, and muted grey-olive bark micro-flecks with uniform quadrant luminance. | Old: `8616a6899cfe0347...`<br>New: `d05d485850453f96bc1c9dcc6c9b49a86ab6b992ff9f684a535c2e613fe3e368` | `7,755 B`<br>`0xaa6971de` / `0x1a9dda62` | `L-R delta: 0.97`<br>`T-B delta: 0.97`<br>Luma: `43.14` |
| **4** | `batch_01_terrain/terrain_compacted_earth_64.png` | `Các vệt nâu tạo dải tuần hoàn thấy rõ khi lát.` | `sin(y * 4π/64)` horizontal wave created 2 brown horizontal stripes per tile (`8 stripes` in `4×4`). | Removed all horizontal/vertical wave modulation; synthesized isotropic cellular soil clods (`Y=0.72`) and crushed basalt grit pebbles with row/column/diagonal equalization. | Old: `5ea9cd3851965732...`<br>New: `eaf7b8104ce2773402031428bc993ba05641c16c33f33689d819e6c591f9948d` | `7,963 B`<br>`0xaa6971de` / `0xeab1792b` | `L-R delta: 1.40`<br>`T-B delta: 1.24`<br>Luma: `44.80` |
| **5** | `batch_01_terrain/terrain_tower_gate_runic_stone_64.png` | `Hoa văn xanh dạng ô lặp dày và quá đều khi lát 4×4; chỉ là ý tưởng điểm nhấn.` | Bright cyan rectangular glyph box `(16..48, 14..50)` repeated on every tile (`16 neon boxes` in `4×4`). | Removed the central cyan square glyph; authored 5 staggered courses of weathered obsidian ashlar blocks (`AZ=142°` NW bevels, SE mortar shadows) with only 5 subtle, low-contrast dormant mineral seams. | Old: `42f28457c539cb36...`<br>New: `9019f323e0d0175e090b16357602f9dc03aaf521ce9193a5ff9e86ac56dcaf62` | `8,574 B`<br>`0xaa6971de` / `0x1c95e518` | `L-R delta: 0.81`<br>`T-B delta: 1.62`<br>Luma: `44.22` |
| **6** | `batch_01_terrain/terrain_grass_stone_transition_64.png` | `Lặp tile tạo dải xen kẽ cỏ/đá, không chứng minh chuyển tiếp từ cỏ sang đá tại ranh giới.` | Single half-stone / half-grass tile created alternating horizontal stripes when tiled `4×4` and lacked directional N/S/E/W curb pieces. | Authored a **9-Slice Directional Curb & Corner Transition Kit (`64×64`)** containing all 8 directional curb/corner pieces (`NW, N, NE, W, E, SW, S, SE`) whose outer perimeter matches `terrain_forest_grass_v1_64` (`0.0` RGB delta) and whose inner core `[20..43, 20..43]` matches `terrain_mossy_flagstone_v1_64` (`0.0` RGB delta), proven on a `256×256` multi-tile plaza composite. | Old: `b05ad98be76606b2...`<br>New: `bf9d0c0c52385a6145439d6d3cca9cbbf799f56a05bf0fe50d807fc55405561c` | `7,979 B`<br>`0xaa6971de` / `0xfb0d8515` | `L-R delta: 1.51`<br>`T-B delta: 2.01`<br>Outer->Grass: `0.00`<br>Inner->Stone: `0.00` |
| **7** | `batch_04_animated_vfx/vfx_tower_gate_portal_swirl_8f.png` | `Alpha chạm mép trên/dưới ở nhiều frame; có nguy cơ cắt, cần tác giả xác nhận/sửa có padding rồi review.` | Baseline `frame_alpha_bounds` touched `y=0` and `y=63` (`[[8,2,60,64], ..., [5,0,57,63], ..., [10,0,60,64]]`). | Re-authored crisp pixel-art 8-frame portal vortex with a **strict `>= 11px` transparent safety margin on all 4 edges of every `64×64` frame** while preserving the dark-void singularity eye, 3 rotating spiral arms, and 6 orbiting sparks. | Old: `9901049c8a136e71...`<br>New: `ba159b3b35e1d21f24a2c675b880a0672f2f9bcd52dcccf868aca41bd994e660` | `2,987 B`<br>`0x6cbfcfbf` / `0x25176c15` | Inclusive bounds across 8 frames:<br>`x in [12..52]`<br>`y in [14..50]`<br>(`>= 11px` margin!) |

---

## 3. Status & Evidence for the 3 `BLOCKED` VFX Files

Per Issue `#82` (`## Allowed file ownership for THIS fix`: *"3 BLOCKED VFX are read-only until separate path-level handoff/approval"*):
1. **Production Path Status (`HELD / BIT-IDENTICAL`):**
   - `docs/art/argon_production_v2/batch_04_animated_vfx/vfx_summoning_hall_mana_motes_8f.png` — **HELD bit-identical** (`SHA256: fb0b7986a4176b676f87d5670b82ff63ac9b39102c706e4f49d8f4e42d9be243`). We **do NOT assert PASS** on the held file.
   - `docs/art/argon_production_v2/batch_04_animated_vfx/vfx_wind_petals_leaves_6f.png` — **HELD bit-identical** (`SHA256: 868712048522d40cfe70aa28331d959f8314e17acbe5654f840893b79f918a2e`). We **do NOT assert PASS** on the held file.
   - `docs/art/argon_production_v2/batch_04_animated_vfx/vfx_rain_droplets_splash_6f.png` — **HELD bit-identical** (`SHA256: 57a088a144298aa10bf9557ef4bfdd08400e6ae7ec0b7654c87e8bcd6f7d4077`). We **do NOT assert PASS** on the held file.
2. **Unlock-Ready Candidate Reworks & Real Animated Playback Proofs (`docs/quality-loop/ARGON-WORK-003/`):**
   To resolve the technical and artistic root causes identified by A08 without violating the read-only path lock on `docs/art/argon_production_v2/batch_04_animated_vfx/`, we authored 3 unlock-ready candidate spritesheets + animated GIF playback proofs inside `docs/quality-loop/ARGON-WORK-003/`:

| Candidate File in `docs/quality-loop/ARGON-WORK-003/candidate_vfx_unlock_ready/` | Animated Playback GIF (`2×` over Sanctuary Terrain) | SHA-256 & Bytes | Active Pixels / Frame (Held Baseline -> Candidate) | Per-Frame Inclusive Alpha Bounds (Safe Margins) | How It Resolves A08's Blocker Once Path Unlock Is Granted |
| :--- | :--- | :--- | :---: | :---: | :--- |
| `candidate_vfx_summoning_hall_mana_motes_8f.png` (`384×48`, 8f) | `playback_candidate_mana_motes_8f_2x.gif` | `8c1e2adf2ccb5f4993dcd1e0006388aa9a3023c97fd1fa96697e05d344c2da50`<br>(`2,578 B`) | `[48..54]` -> **`[219..236]`** (`4.5×` legibility boost) | `x in [8..40]`, `y in [9..41]` (`>= 7px` margin on all 4 sides) | Replaces single-pixel dots with 6 ascending `3×3` / `5×5` diamond crystal motes (gold & astral violet) rising above a luminous `Y=0.72` summoning rune ring, clearly legible at `1×` and `2×`. |
| `candidate_vfx_wind_petals_leaves_6f.png` (`288×48`, 6f) | `playback_candidate_wind_petals_6f_2x.gif` | `dd400c0ac1d28add524e25809b089e93ab80df2c6ae13f3d7aa2dadc4a3aef9d`<br>(`707 B`) | `[14]` -> **`[67..69]`** (`4.9×` legibility boost) | `x in [6..36]`, `y in [11..34]` (`>= 6px` margin on all 4 sides) | Upgrades 5 tiny specks into 5 distinct `4×3` leaf/petal clusters with 4-pixel wind-streak motion trails clearly readable at `1×`. |
| `candidate_vfx_rain_droplets_splash_6f.png` (`288×48`, 6f) | `playback_candidate_rain_splash_6f_2x.gif` | `ffb573a69c1cceecdf8948e1bad015d0241e1253ffa9889f0e285d0cae5def3a`<br>(`1,554 B`) | `[25..166]` -> **`[151..320]`** | `x in [6..41]`, `y in [6..41]` (`>= 6px` margin on all 4 sides; zero top/right edge contact!) | Eliminates `y=0` and `x=47` edge clipping completely while adding double-impact diagonal rain streaks, `Y=0.72` elliptical ground ripples, and crown droplets. |

---

## 4. Verification of 14 Bit-Identical `PASS` Files (`17c99aea77860958346acf59cecab18aa514c9c1`)

All 14 `PASS` files accepted by A08 in `#77` / PR `#81` were verified byte-for-byte against `17c99aea77860958346acf59cecab18aa514c9c1`:
1. `batch_01_terrain/terrain_mossy_flagstone_v1_64.png` — `cc203cb74a572722496d9e3db28020765cb81534c1b104a29ee7112998ebdadd` (`IDENTICAL`)
2. `batch_01_terrain/terrain_mossy_flagstone_v2_64.png` — `fde1d867834c7e29e200105990b583aeaccfe1b6d895b9dcd88eb0bd24d7612e` (`IDENTICAL`)
3. `batch_02_foliage_props/foliage_ancient_pine_96x128.png` — `9d218b0b9f2e0be12384165656f5cd8d88480cc8f5ef59165dd6176faa389986` (`IDENTICAL`)
4. `batch_02_foliage_props/foliage_moss_flower_bush_64x64.png` — `6f933d571f6bb30da45a472c07d237fe627ea677413d6488dda455e410321bfe` (`IDENTICAL`)
5. `batch_02_foliage_props/foliage_ashen_violet_shrub_64x64.png` — `da7da58a047c5b40766c559229d51c866cf92c3733d4cda8c9b4d84d456ce84c` (`IDENTICAL`)
6. `batch_02_foliage_props/foliage_dorm_blossom_clump_64x48.png` — `9ad3c38322f753db0d964ab07fc2da253eb136c2cebb7a2e351a0d4873f5a393` (`IDENTICAL`)
7. `batch_02_foliage_props/prop_wall_ivy_drape_64x96.png` — `1c6e9d94dd8650f96303d668a3522f27016926226625300c1729ad9fc7f97615` (`IDENTICAL`)
8. `batch_02_foliage_props/prop_wooden_bronze_wayfinder_64x96.png` — `22d71139e34b09464821613625641dc2e61383ce741caba2645a15a988000f9d` (`IDENTICAL`)
9. `batch_03_architecture_details/arch_black_slate_roof_moss_96x64.png` — `70256eeca024cf9c9166a79f658f33f352f8234a3cc23022bae5c82cc98d4d39` (`IDENTICAL`)
10. `batch_03_architecture_details/arch_ancient_timber_bronze_door_64x80.png` — `248b9323d718692120629a9634e2d569b8eec571123abda6234f1756963f92c2` (`IDENTICAL`)
11. `batch_03_architecture_details/arch_summoning_hall_pedestal_trim_96x48.png` — `c9d181d78428e44b31ec1d5be67d80e17444e2a6e71fa53dd506cc9bf1f00398` (`IDENTICAL`)
12. `batch_03_architecture_details/arch_warm_stained_window_frame_48x64.png` — `ce8bc68f3fa128ae532a70d2797ecf2fc6ec4d3b305a21d9b39fef450466601f` (`IDENTICAL`)
13. `batch_03_architecture_details/arch_tower_gate_buttress_pillar_64x128.png` — `d4d828ef3f5229634e4c40d58b36f6488141e0943a8688b4b6e39b4882ac5451` (`IDENTICAL`)
14. `batch_03_architecture_details/arch_dorm_carved_timber_frieze_96x32.png` — `d97926caa4831b1c261fe512be7da809a0addc2790f34b28c12cff2956231bac` (`IDENTICAL`)

---

## 5. `[ARGON_REVIEW]` Artistic Self-Critique & `[HANDOFF]`

### 5.1 `[ARGON_REVIEW]` Artistic Self-Critique
- **What improved:**
  1. Eliminated all mathematical sine-wave banding (`vertical columns` in `grass_v1`, `diagonal 64px stripes` in `grass_v2`, `red circular bullseyes` in `grass_v3`, `horizontal brown ripples` in `compacted_earth`, and `16 glowing cyan boxes` in `tower_gate_runic_stone`). On `terrain_6fixes_before_after_4x4.png`, all 5 continuous ground materials now tile `4×4` (`256×256`) and `2×2 @ 2× zoom` without visible grid periodicity (`edge_delta_lr` and `edge_delta_tb` between `0.81` and `2.01` RGB).
  2. Redesigned `terrain_grass_stone_transition_64.png` as a **9-Slice Directional Curb & Corner Kit (`NW, N, NE, W, Center, E, SW, S, SE`)** whose outer perimeter matches `terrain_forest_grass_v1_64` (`0.0` RGB delta) and whose inner core matches `terrain_mossy_flagstone_v1_64` (`0.0` RGB delta), demonstrated on a full `256×256` multi-tile plaza composite.
  3. Re-authored `vfx_tower_gate_portal_swirl_8f.png` with a strict `>= 11px` transparent border (`x in [12..52]`, `y in [14..50]`) across all 8 frames while preserving the crisp pixel-art dark-void center and rotating spiral blades.
- **Remaining Honest Limitations:**
  1. The 3 `BLOCKED` VFX files in `docs/art/argon_production_v2/batch_04_animated_vfx/` are intentionally kept read-only/bit-identical per Issue `#82`'s ownership lock; their reworked replacements (`candidate_vfx_*.png`) and animated GIF playbacks live in `docs/quality-loop/ARGON-WORK-003/candidate_vfx_unlock_ready/` awaiting A01/A08 path-unlock approval.
  2. The previews and animated GIFs in `docs/quality-loop/ARGON-WORK-003/` are standalone asset & scene-tile composites, **not** live in-engine Android runtime captures (`ES-A03-002` / `#78` owns renderer integration). Physical device FPS/thermal remains `NOT TESTED`.

### 5.2 Formal `[HANDOFF]` Block
```markdown
[HANDOFF] TASK: ARGON-WORK-003 (Issue #82) | ROLE: GEMINI 4 ARGON HIGH | STATUS: READY FOR A08 RE-VERIFICATION
- Base SHA: 17c99aea77860958346acf59cecab18aa514c9c1
- Proposed Branch: argon/work-003-fix-7-assets-issue82
- Changed Files in docs/art/argon_production_v2/ (Exact 7 NEEDS_FIX PNGs + Manifest):
  1. docs/art/argon_production_v2/batch_01_terrain/terrain_forest_grass_v1_64.png (SHA256: 7061c0a1f3e497536ef9a23d4f0bfd14e04d94e87def6453bcf55145dc8dd335)
  2. docs/art/argon_production_v2/batch_01_terrain/terrain_forest_grass_v2_64.png (SHA256: fac3d8e0949fa54feecd35d13b517ec32bf50e42949b3aaa8f5e7c1e865ead61)
  3. docs/art/argon_production_v2/batch_01_terrain/terrain_forest_grass_v3_64.png (SHA256: d05d485850453f96bc1c9dcc6c9b49a86ab6b992ff9f684a535c2e613fe3e368)
  4. docs/art/argon_production_v2/batch_01_terrain/terrain_compacted_earth_64.png (SHA256: eaf7b8104ce2773402031428bc993ba05641c16c33f33689d819e6c591f9948d)
  5. docs/art/argon_production_v2/batch_01_terrain/terrain_tower_gate_runic_stone_64.png (SHA256: 9019f323e0d0175e090b16357602f9dc03aaf521ce9193a5ff9e86ac56dcaf62)
  6. docs/art/argon_production_v2/batch_01_terrain/terrain_grass_stone_transition_64.png (SHA256: bf9d0c0c52385a6145439d6d3cca9cbbf799f56a05bf0fe50d807fc55405561c)
  7. docs/art/argon_production_v2/batch_04_animated_vfx/vfx_tower_gate_portal_swirl_8f.png (SHA256: ba159b3b35e1d21f24a2c675b880a0672f2f9bcd52dcccf868aca41bd994e660)
  8. docs/art/argon_production_v2/ASSET_MANIFEST_V2.json
- New Evidence Files in docs/quality-loop/ARGON-WORK-003/:
  - docs/quality-loop/ARGON-WORK-003/ARGON_WORK_003_FIX_AND_REVIEW_REPORT.md
  - docs/quality-loop/ARGON-WORK-003/terrain_6fixes_before_after_4x4.png
  - docs/quality-loop/ARGON-WORK-003/portal_swirl_margin_before_after.png
  - docs/quality-loop/ARGON-WORK-003/blocked_vfx_playback_and_candidate_proof.png
  - docs/quality-loop/ARGON-WORK-003/playback_portal_swirl_8f_2x.gif
  - docs/quality-loop/ARGON-WORK-003/playback_candidate_mana_motes_8f_2x.gif
  - docs/quality-loop/ARGON-WORK-003/playback_candidate_wind_petals_6f_2x.gif
  - docs/quality-loop/ARGON-WORK-003/playback_candidate_rain_splash_6f_2x.gif
  - docs/quality-loop/ARGON-WORK-003/candidate_vfx_unlock_ready/candidate_vfx_summoning_hall_mana_motes_8f.png
  - docs/quality-loop/ARGON-WORK-003/candidate_vfx_unlock_ready/candidate_vfx_wind_petals_leaves_6f.png
  - docs/quality-loop/ARGON-WORK-003/candidate_vfx_unlock_ready/candidate_vfx_rain_droplets_splash_6f.png
- Unchanged Files Verification:
  - 14/14 PASS assets: BIT-IDENTICAL to 17c99aea77860958346acf59cecab18aa514c9c1
  - 3/3 BLOCKED VFX in production folder: HELD BIT-IDENTICAL to 17c99aea77860958346acf59cecab18aa514c9c1 (do not assert PASS on held files; candidate reworks provided in docs/quality-loop/ARGON-WORK-003/candidate_vfx_unlock_ready/)
- Requested Reviewers: @A08 (independent re-verification on new SHA), @A03 (visual/renderer alignment), @A09 [QA_REVIEW], @Codex [CODEX_REVIEW].
```
