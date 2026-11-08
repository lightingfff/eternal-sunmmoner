# ARGON-WORK-001 — Art Direction & Technical Specification Note

**Task Reference:** [GitHub Issue #66](https://github.com/lightingfff/eternal-sunmmoner/issues/66) (`ARGON-WORK-001`)  
**Role:** GEMINI 4 ARGON HIGH — Creative & Production Co-Lead  
**Base Main SHA:** `bdff6f9976dc4927e1e095a503bc339509c7a9aa`  
**Target Branch:** `argon/work-001-visual-prototype`  
**Scope:** Isolated original pixel-art prototype (`docs/art/argon_pilot/**`, `docs/quality-loop/ARGON-WORK-001/**`). No runtime or existing P31 files modified.

---

## 1. Creative Vision & Camera Alignment

ETERNAL SUMMONER uses a **2.5D Oblique Three-Quarter Camera** (`ObliqueCamera` ground Y-scale `0.72`, `GROUND_ELLIPSE_RATIO = 0.66`) with directional illumination (`SanctuaryLight`: azimuth `142°`, elevation `48°`) and strict **Nearest-Neighbor** pixel sampling.

To harmonize with the existing Sanctuary island architecture and dark fantasy mood (`DARK_FANTASY_TRANSFORMATION.md` and `docs/art/sanctuary_revamp/ASSET_MANIFEST.json`):
1. **Foreshortened Ground Plane (`mossy_cobble_dark_64.png`):**
   - Standard top-down square stones look stretched or flat under an oblique `0.72` Y-foreshortening. Our 64×64 cobblestone courses are authored with **8 horizontal courses of ~8px vertical depth** and wider horizontal spans (`15–18px`), naturally matching the foreshortened ground plane.
   - Each basalt slab features a **North/North-West top bevel highlight** (`+8` to `+14` luma) and a **South/South-East deep mortar drop shadow** (`-10` to `-13` luma), locking into `SanctuaryLight` (`AZ 142°`).
   - Periodic modulo-64 boundary math guarantees **100% seamless wrap-around** across all 4 borders (`x=0..63`, `y=0..63`) without visible grid repetition when tiled across Sanctuary lanes.

2. **Upright Emissive VFX (`amber_lantern_flame_8f.png`):**
   - Upright sprites retain `1.0` vertical height scale. The 8-frame lantern flame strip (`192×32` RGBA, 8 frames of `24×32`) is authored with crisp pixel clusters rather than blurry Gaussian gradients.
   - Four distinct luminance bands create a readable dark-fantasy hearth glow at night:
     - **Incandescent Core:** `#FFF7D6` (`alpha = 255`)
     - **Golden Inner Tongue:** `#FBBF24` (`alpha = 255`)
     - **Warm Amber Body:** `#F59E0B` (`alpha = 240`)
     - **Burnt Crimson Rim & Night Halo:** `#D97706` (`alpha = 195`) with a soft outer radial falloff (`alpha = 18..115`) for night atmosphere without requiring expensive fullscreen shaders.

---

## 2. Color Palette Specification

| Material / Layer | Role | Hex Code | RGBA `(R, G, B, A)` | Luma (`Y`) |
| :--- | :--- | :---: | :---: | :---: |
| **Mortar Deep** | Intersecting stone joints | `#161920` | `(22, 25, 32, 255)` | `24.9` |
| **Mortar Shadow** | Standard crevice shadow | `#1D212A` | `(29, 33, 42, 255)` | `32.8` |
| **Cold Basalt Base** | Primary cobblestone slab | `#363B46` | `(54, 59, 70, 255)` | `58.7` |
| **Slate Grey Variant** | Secondary weathered slab | `#3C414D` | `(60, 65, 77, 255)` | `64.8` |
| **Weathered Granite** | Highlighted slab face | `#404652` | `(64, 70, 82, 255)` | `69.6` |
| **Crevice Moss Dark** | Damp shadow moss | `#213229` | `(33, 50, 41, 255)` | `45.7` |
| **Crevice Moss Mid** | Colonizing moss cluster | `#2D4536` | `(45, 69, 54, 255)` | `62.8` |
| **Crevice Moss Hi** | Sun-kissed moss tip | `#3E5C44` | `(62, 92, 68, 255)` | `83.9` |
| **Lantern Core** | White-gold flame heart | `#FFF7D6` | `(255, 247, 214, 255)` | `246.3` |
| **Lantern Mid** | Bright gold flame body | `#FBBF24` | `(251, 191, 36, 255)` | `192.6` |
| **Lantern Outer** | Warm amber tongue & halo | `#F59E0B` | `(245, 158, 11, 240)` | `165.9` |
| **Lantern Rim** | Burnt orange outer edge | `#D97706` | `(217, 119, 6, 195)` | `131.7` |
| **Ember Spark** | Rising pixel mote | `#FDE047` | `(253, 224, 71, 230)` | `219.1` |

---

## 3. Asset & Animation Metadata Contract

### 3.1 `mossy_cobble_dark_64.png`
- **Category:** `ground_terrain` (64×64 seamless tile prototype)
- **Dimensions:** `64 × 64` px (`8-bit/color RGBA`, non-interlaced, `color_type = 6`)
- **Alpha Channel:** `opaque_rgba` (all `4,096` pixels verified `alpha = 255`)
- **Pivot Convention:** `(32, 64)` (bottom-center ground anchor, matching `ASSET_MANIFEST.json`)
- **Lighting Response:**
  - `meanDayLuma`: `57.57` (calibrated for dark-fantasy basalt causeways)
  - `recommendedNightLuma`: `22.45`
  - `recommendedNightTint`: `#10141C`
- **File Size & Integrity:** `4,762` bytes | SHA-256: `98de6bec85f6b0af1b23db31b8eb5b7269db53a5d7a98e4d257bd45d742217c6`

### 3.2 `amber_lantern_flame_8f.png`
- **Category:** `animated_vfx` (8-frame horizontal sprite sheet)
- **Sheet Dimensions:** `192 × 32` px (`8` frames × `24 × 32` px per frame)
- **Grid Layout:** `8 × 1` horizontal strip (`frame_index = 0..7`, `x_offset = frame_index * 24`)
- **Pivot Convention:** `(12, 28)` per frame (wick base inside lantern glass cage; cyan marker in contact sheet)
- **Animation Timing:** `6 FPS` (`166.7 ms` per frame, `1.33 s` full cycle), `loopMode = LOOP_CONTINUOUS`
- **Motion Arc across 8 Frames:**
  - `F0`: Neutral upright flame (`sway = 0.0`, `stretch = 1.00`), spark at `(13, 14)`
  - `F1`: Slight east breeze (`sway = +0.6`, `stretch = 1.08`), spark rises to `(14, 12)`
  - `F2`: Peak east lean & crest (`sway = +1.0`, `stretch = 1.15`), spark at `(14, 10)`
  - `F3`: Recovery toward center (`sway = +0.5`, `stretch = 1.04`), spark at `(13, 8)`
  - `F4`: Low settle trough (`sway = 0.0`, `stretch = 0.94`), fading spark at `(12, 6)`
  - `F5`: Westward draft (`sway = -0.6`, `stretch = 0.98`), new spark ignites at `(11, 16)`
  - `F6`: Peak west lean (`sway = -1.0`, `stretch = 1.10`), spark rises to `(11, 14)`
  - `F7`: Smooth return to F0 (`sway = -0.4`, `stretch = 1.03`), spark at `(12, 12)`
- **Alpha Channel Breakdown:** `5,284` fully transparent pixels (`alpha = 0`), `374` semi-transparent halo pixels (`1 ≤ alpha ≤ 254`), `486` opaque core pixels (`alpha = 255`).
- **File Size & Integrity:** `535` bytes | SHA-256: `fdba5d9778d9eea9b7787d15458cfa3f901eb8b50513d15ab3b9edf3c557c46c`

---

## 4. Compatibility & Non-Destructive Guarantee

- **Originality:** Deterministically synthesized from scratch via `/tmp/generate_argon_pilot.py` specifically for ETERNAL SUMMONER. Zero external copyrighted art or AI style-rip.
- **Zero Runtime Disruption:** Stored exclusively under `docs/art/argon_pilot/` for A08 (`Pixel Art & VFX`) and A03 (`2.5D Renderer`) review prior to any future asset bundle integration ticket.
