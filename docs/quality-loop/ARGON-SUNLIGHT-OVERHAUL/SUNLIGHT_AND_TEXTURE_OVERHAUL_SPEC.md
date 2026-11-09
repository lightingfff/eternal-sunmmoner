# ETERNAL SUMMONER — Sanctuary Texture & Runtime Sunlight Overhaul Specification
**Author:** `GEMINI 4 ARGON HIGH` (Creative & Production Co-Lead)  
**Co-Lead Partner:** `Arena A01` (Technical & Integration Lead)  
**Target Specialist Agents:** `A03` (Graphics/Renderer), `A08` (Pixel Art & VFX), `A09` (Independent QA), `A10` (Android Performance), `Codex` (Architecture/Review)  
**Camera & Geometry Invariants Preserved:** `ObliqueCamera.OBLIQUE_Y_SCALE = 0.72f`, `GROUND_ELLIPSE_RATIO = 0.66f`, `SanctuaryLight` (`AZIMUTH_DEG = 142f`, `ELEVATION_DEG = 48f`), 8-vertex polygonal Sanctuary island (`3200×2760`), zero layout/collider/navigation changes.

---

## 1. `[ARGON_VISUAL_AUDIT]` — Root-Cause Analysis of Current Visual & Lighting Defects

### 1.1. Exact Code Locations Where Sunlight & Atmosphere Act Like "Flat Screen-Space Sticker Overlays"

1. **`SanctuaryWorldRenderer.kt` — `drawSkyRays()` (Lines 1406–1431, called at Line 285):**
   - **How it currently works:** Rotates the entire `DrawScope` canvas by `-34°` around screen origin `Offset(0f, 0f)` and draws 4 hardcoded screen-X rectangles (`80f`, `290f`, `490f`, `760f`), each split into 3 stepped vertical bands (`0..0.42f`, `0.34f..0.75f`, `0.70f..1.45f` of `size.height`) using solid `drawRect(color = rayCol.copy(alpha = ...))`.
   - **Why it looks fake, flat, and pasted on the screen:**
     - **Zero World-Space Projection (`camX`, `camY`, `zoom` ignored):** `drawSkyRays()` does not take `camX`, `camY`, or `zoom` as parameters. When the player pans the camera across Sanctuary or zooms between `0.36`, `0.88`, and `1.48`, the sunbeams remain **glued to the phone screen** like a static PNG HUD sticker instead of anchoring to world landmarks (Tower Gate, Central Plaza, Summoning Hall).
     - **Hard-Stepped Banding & Wrong Layer Order:** Each ray is 3 overlapping `drawRect`s with abrupt alpha steps (`aFull`, `0.5 * aFull`, `0.22 * aFull`), creating visible horizontal seams across the middle of the screen. Furthermore, it is called at step 7 inside `drawSanctuaryFloatingWorld()` (before depth-sorted buildings, trees, and heroes in `SanctuaryLobbyScreen.kt`), so it neither casts light pools onto roofs/floors nor gets occluded by tall structures or tree canopies.

2. **`PixelSpriteRenderer.kt` — `drawForegroundAtmosphere()` (Lines 1529–1575, called at `SanctuaryLobbyScreen.kt` Line 850):**
   - **How it currently works:** Draws 4 screen-space diagonal trapezoids from `y = 0f` to `y = size.height` (`baseX = size.width * (0.12f + i * 0.28f) + drift`) without `camX`, `camY`, or `zoom`. In `BiomeTheme.SANCTUARY_ASTRAL` (lines 1534–1554), it skips shafts completely and only draws 14 desaturated grey/brown ovals (`0xFF8A7A97`, `0xFF6B7A8A`, `0xFF9A6B3A`) drifting in screen coordinates.
   - **Why it looks flat:** Screen-locked trapezoids and screen-space dust motes slide with the viewport rather than existing inside the 2.5D world volume.

3. **`HdrLightResponse.kt` (Lines 49–59, 121–137, 464–483) & `DepthPresentation.kt` (Lines 72–74, 110–135) — 48×48 Radial Bitmap Stamps (`drawHdrStamp` / `drawLightStamp`):**
   - **How it currently works:** Generates 48×48 radial falloff `ImageBitmap` textures (`amberMask`, `goldMask`, `aetherMask`, `moonMask`, `shadeMask`) and blits them with standard `SrcOver` alpha compositing (`drawImage(image = maskFor(tint), ...)`).
   - **Why it feels like a pasted PNG:** Standard `SrcOver` alpha blending of a solid-colored radial bitmap over dark pixel art does not selectively brighten sunlit stone bevels or grass tips—it paints a **milky, low-contrast translucent smudge** over the underlying mortar crevices, lowering local micro-contrast.

4. **`LivingWorldRenderer.kt` — `drawWorldGrade()` (Lines 27–35) & `SanctuaryWorldRenderer.kt` (Line 119) — 5 Fullscreen Flat `drawRect` Color Washes:**
   - **How it currently works:** Paints 4 fullscreen `drawRect`s over the entire scene after all entities are rendered (`0xffffefd5`, `light.ambient`, `0xff101d33`, `light.skyTint`), plus another fullscreen `drawRect(skyTint)` at `SanctuaryWorldRenderer.kt:119`.
   - **Why it kills depth and brightness:** Fullscreen flat `drawRect` overlays compress dynamic range uniformly across sky, sunlit roofs, and deep shadows—washing out dark-fantasy shadow depth during the day while muddying highlights during morning/evening/cloudy transitions.

5. **`HdrLightResponse.kt` — `drawVolumeGrade()` (Lines 485–546) — The "Noon Dark Navy Mud" Bug:**
   - **How it currently works:** Over every building, tree, and column, `drawVolumeGrade()` applies a NW highlight capped at `key * 1.15f <= 0.20f` using a cool/neutral beige (`0xffe8e0c8`), **and then** paints a massive dark navy shadow (`0xff0a1220`) over the SE 62% of the sprite with `alpha = (key * 2.60f * exposure).coerceAtMost(0.42f)` (line 519) **plus** a second dark navy gradient up to `0.28f` (line 536).
   - **Why it makes the game dark and gloomy even at 12:00 Noon:** Because `key` is proportional to `sunIntensity`, **the brighter the sun gets at noon, the darker `drawVolumeGrade()` paints the front/right facade of every building** (~50% combined dark navy `0xff0a1220` painted directly over the front of every landmark and house!). Meanwhile, `exposureBalance()` (lines 132–137) is `1.00f` during the day (`night = 0f`), giving daytime sunlight zero luminance boost.

6. **Texture Luminance & Micro-Contrast Gaps (`SanctuaryMaterials.kt`, `sanctuary_p03`, and Flat Vector Curbs in `SanctuaryWorldRenderer.kt`):**
   - Procedural fallback grass (`0x293b32..0x3d5340`, Luma `52–68`) and flat vector road curbs/courtyards (`0xFF1E2028`, `0xFF3A3D4A`, `0xFF3A352E` in `SanctuaryWorldRenderer.kt` lines 454–455, 826–833) lack directional NW sunlit bevels, sitting too dark against the `~175 luma` P27B ivory structures.

---

## 2. `[VISUAL_OVERHAUL_PLAN]` — Architecture for Premium 2.5D Dark Fantasy Sunlight & Textures

### A. Texture Overhaul (64×64 Dark Fantasy Premium, `Y=0.72` Oblique)
- **Target Luminance & Contrast Hierarchy (Daylight Base):**
  - **Deep Crevices / Mortar / Root Shadows:** `Luma 28–42` (rich obsidian-slate `#242832` & deep forest moss `#222E26` — preserves true Dark Fantasy grounding).
  - **Midtone Material Body:**
    - *Sunlit Forest Grass (`v1..v3`):* `Luma 78–108` (`#3E5840` -> `#56764A`), replacing muddy swamp greens while avoiding neon cartoon green.
    - *Compacted Earth & Courtyard Soil:* `Luma 86–114` (`#584C3E` -> `#786854`) with embedded warm quartz pebbles.
    - *Mossy Flagstone & Plaza Stone:* `Luma 124–158` (`#7E786E` -> `#A29A8E`) with directional 2px NW sunlit bevels (`#CAC0A8`, `Luma 192`).
    - *Tower Gate Runic Stone:* `Luma 64–96` basal obsidian (`#3A3F4E` -> `#586074`) with recessed cyan-gold astral veins (`#64D2FF` / `#F5B841`).
  - **Sunlit Specular Micro-Relief:** Every 64×64 tile and architectural trim bakes a consistent **142° Azimuth / 48° Elevation (NW -> SE)** micro-bevel: 1–2px warm ivory/amber top-left lip and 1–2px cool slate bottom-right pocket.
- **Anti-Repetition & Seam Discipline:**
  - All 64×64 terrain tiles enforce `L-R Edge Delta <= 2.2` and `T-B Edge Delta <= 2.2` (verified in `ARGON-WORK-003` at `0.81–2.01` and in `argon_sunlight_overhaul_v3` at `0.00`).
  - Use a 3-tier macro-variation breakup in `SanctuaryWorldRenderer`:
    1. **8×8 toroidal mosaic + deterministic hash rotation (`sanctuarySheetRotated`)** for large lawns/plazas.
    2. **9-Slice Directional Curb & Grass-Fringe Transition (`terrain_grass_stone_transition_64.png`)** along plaza rims, road borders, and building foundations to eliminate hard vector clip edges.
    3. **World-Space Cloud/Canopy Dapple Modulation** (see Section 2.B) so identical tiles at different world coordinates receive subtly different warm sunlight or cool cloud-shadow values.

### B. Runtime Directional Sunlight System (`SanctuarySunlightSystem.kt`)
Replace the static screen-space `drawSkyRays()` and fullscreen `drawWorldGrade()` washes with a **5-layer World-Space Directional Sunlight Pipeline** governed by `SanctuaryLight` (`AZIMUTH_DEG = 142f`, `ELEVATION_DEG = 48f`) and `WorldEnvironment.lighting`:

1. **Layer 1 — Ground & Roof Sunlit Key Response (Replaces `drawWorldGrade` fullscreen washes & fixes `HdrLightResponse.drawVolumeGrade`):**
   - **Fix `HdrLightResponse.drawVolumeGrade()`:**
     - During daylight (`sunIntensity > 0.25f`), boost the NW roof/facade key highlight (`0xFFFFF1D0` warm solar ivory, `alpha = (sunIntensity * 0.24f).coerceAtMost(0.24f)`) on the upper-left 45% of structures.
     - Reduce the SE shade overlay from `key * 2.60f` (`0.42f + 0.28f`) down to a clean, transparent cool-slate shadow (`0xFF141C2E`, `alpha = (sunIntensity * 0.14f).coerceAtMost(0.16f)`) restricted to the bottom-right 32% of the structure.
   - **Sunlit Ground Key Pool (`drawSunlitGroundKey`):**
     - On open sunlit areas (Central Plaza, Southern Gate Courtyard, Training Arena, Summoning Hall stage), draw world-space foreshortened (`Y = 0.72`) warm sunlight pools (`0xFFFFEAB8`, `alpha = 0.09f..0.14f * sunIntensity`) BEFORE cast shadows are drawn.

2. **Layer 2 — Occlusion-Aware Shadow Pass (`SanctuaryEnvironmentShadows` + Building/Canopy Shadow Masks):**
   - Because Ground Sunlit Key Pools are drawn *before* `SanctuaryEnvironmentShadows.drawGroundShadows(...)`, when buildings and trees cast their directional SE shadows (`SanctuaryLight.shadowWorldOffset`), those shadows naturally **extinguish/occlude the ground sunlight** on the SE side of every building and under every tree canopy!

3. **Layer 3 — World-Space Parallax Cloud & Canopy Dapple Modulation (`drawCloudDappleModulation`):**
   - Instead of a uniform static brightness across the whole 3200×2760 island, project **3 large, slowly drifting world-space cloud shadow ellipses** (`rx = 420f..580f`, `ry = rx * 0.72f`, drifting along the wind vector `(wx + timeSec * 18f, wy + timeSec * 8f)`) with subtle cool-indigo alpha (`0xFF0E1729`, `alpha = 0.08f..0.12f * daylight`).
   - Under `OAK_TREE`, `SAKURA_BLOSSOM_TREE`, and `PINE_SPIRIT_TREE` canopies, render a **3-lobe sunlit leaf-dapple cluster** (`0xFFFFE8A3`, `alpha = 0.12f * sunIntensity`) on the NW sunlit rim of the tree's ground shadow, oscillating gently with `sin(timeSec * 1.8f + obs.x * 0.02f)`.
   - **Cost:** Only 3 `drawOval` calls for island cloud bands + 2 small `drawOval` calls per visible on-screen tree (zero allocations, zero shaders, < 0.08 ms/frame).

4. **Layer 4 — World-Anchored Directional Sun Shafts (`drawWorldSunShafts` — Replaces Screen-Space `drawSkyRays()`):**
   - **Anchored to World Coordinates, Projected via `ObliqueCamera`:**
     - Define 5 curated world-space sunbeam corridors anchored to key Sanctuary vistas:
       1. `Tower Gate Monolith Cleft` (`wx = 1960f, wy = 620f, width = 150f`)
       2. `West Plaza Colonade` (`wx = 1580f, wy = 1120f, width = 185f`)
       3. `Central Astrolabe Medallion` (`wx = 1940f, wy = 1240f, width = 220f`)
       4. `Summoning Hall Amphitheatre` (`wx = 2360f, wy = 1880f, width = 175f`)
       5. `Training Ground Courtyard` (`wx = 890f, wy = 1760f, width = 165f`)
     - Each shaft is a pre-allocated reusable `Path` projected from its upper-left sky entry `(sx(wx - 280f), sy(wy - 380f, z = 240f))` down along the `142°` azimuth vector to its ground landing ellipse `(sx(wx), sy(wy, z = 0f))`.
     - As the player pans (`camX`, `camY`) or zooms (`zoom`), the sun shafts **stay physically locked to the Plaza, Tower Gate, and Summoning Hall** with true 2.5D parallax (`1.0x` at ground contact, `1.08x` at upper canopy height)!
     - Where each shaft touches the `Y = 0.72` ground plane, it illuminates a foreshortened warm golden landing rim (`0xFFFFE49E`, `alpha = 0.11f * sunIntensity`), and gently pulses (`0.88f + 0.12f * sin(timeSec * 0.55f + idx * 1.4f)`).
     - **Occlusion by Buildings:** Any shaft landing point that overlaps a `BUILDING_KINDS` shadow footprint has its ground landing alpha attenuated by `0.35f`, so buildings physically block sun shafts from lighting the ground behind them.

5. **Layer 5 — Sunlit Roof & Rim Specular Glints (`drawSunlitArchitecturalRim`):**
   - For visible buildings and stone balustrades on the `BALANCED` quality tier, draw a crisp `1.5f * zoom` warm sunlit rim highlight (`0xFFFFE8B0`, `alpha = 0.28f * sunIntensity`) along the NW roof ridge and plinth top edge (`drawStructureFoundation` in `PixelSpriteRenderer.kt`).

---

## 3. `[IMPLEMENTATION_PLAN]` — File Ownership, Work Breakdown & Acceptance Gates

### 3.1. Exact Files to Create / Modify
| File Path | Role / Owner | Action | Scope & Purpose |
| :--- | :--- | :--- | :--- |
| `docs/art/argon_production_v2/**` & `docs/art/argon_sunlight_overhaul_v3/**` | **Argon Co-Lead** (+ `A08`) | **DONE / Ready** | 24 V2 assets (with all 7 Issue #82 fixes verified) + V3 Sunlit Flagstone/Grass master calibration tiles & Before/After visual comparison sheet. |
| `app/src/main/java/com/example/game/pixel/SanctuarySunlightSystem.kt` | **`A03`** (co-designed with Argon, coordinated by **`A01`**) | **CREATE (New)** | Self-contained runtime directional sunlight system: `drawSunlitGroundKey()`, `drawCloudDappleModulation()`, `drawWorldSunShafts()`, `drawCanopySunDapple()`. Zero per-frame allocations; pre-allocated `Path`s; respects `DepthPresentation.Quality`. |
| `app/src/main/java/com/example/game/pixel/HdrLightResponse.kt` | **`A03`** | **MODIFY** (Lines 132–137, 485–546) | Fix `drawVolumeGrade()` so noon sunlight warms NW roofs/walls (`alpha <= 0.24f`) instead of crushing SE facades with `3x` dark navy (`0.42f + 0.28f -> 0.16f max`). Add daytime directional key curve. |
| `app/src/main/java/com/example/game/pixel/LivingWorldRenderer.kt` | **`A03`** | **MODIFY** (Lines 27–35) | Replace the 4 flat fullscreen daytime `drawRect` washes in `drawWorldGrade()` with subtle edge-only vignette at dusk/night, keeping daytime midtones crisp and un-muddied. |
| `app/src/main/java/com/example/game/pixel/SanctuaryWorldRenderer.kt` | **`A03`** | **MODIFY** (Lines 118–120, 258–289, 1406–1431) | Replace screen-space `drawSkyRays()` with `SanctuarySunlightSystem.drawWorldSunShafts(camX, camY, zoom, timeSec)` and add `drawCloudDappleModulation()` inside `islandGroundPath` prior to cast shadows. |
| `app/src/main/java/com/example/game/pixel/PixelSpriteRenderer.kt` | **`A03`** | **MODIFY** (Lines 528–584, 1529–1575) | Add NW sunlit top-lip highlight on `drawStructureFoundation` plinths; pass `camX, camY, zoom` into `drawForegroundAtmosphere` so dust/sun-motes drift in world space with parallax. |

### 3.2. Drop-In Kotlin Implementation for `SanctuarySunlightSystem.kt` (For A01 / A03 Integration)
```kotlin
package com.example.game.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.game.map.ObliqueCamera
import com.example.game.map.WorldObstacle
import com.example.game.world.WorldEnvironment
import kotlin.math.cos
import kotlin.math.sin

/**
 * Runtime World-Space Directional Sunlight System for Sanctuary (2.5D Oblique Y = 0.72).
 * Replaces static screen-space rectangle overlays with world-anchored sun shafts,
 * ground/roof directional key pools, canopy leaf-dapple, and drifting cloud shadow modulation.
 * Zero per-frame object allocations: reuses a single pre-allocated Path.
 */
internal object SanctuarySunlightSystem {

  private data class WorldSunShaftAnchor(
    val worldX: Float,
    val worldY: Float,
    val groundWidthWorld: Float,
    val heightZ: Float,
    val phaseOffset: Float,
  )

  // Curated world-space corridors aligned with Sanctuary landmarks
  private val shaftAnchors = arrayOf(
    WorldSunShaftAnchor(1960f, 620f, 150f, 260f, 0.0f),  // Tower Gate Monolith Cleft
    WorldSunShaftAnchor(1580f, 1120f, 185f, 240f, 1.3f), // West Plaza Approach
    WorldSunShaftAnchor(1940f, 1240f, 220f, 280f, 2.7f), // Central Astrolabe Medallion
    WorldSunShaftAnchor(2360f, 1880f, 175f, 240f, 4.1f), // Summoning Hall Amphitheatre
    WorldSunShaftAnchor(890f, 1760f, 165f, 230f, 5.2f),  // SW Training Arena
  )

  private val reusableShaftPath = Path()

  /**
   * Layer 1 & 3: Drawn inside islandGroundPath BEFORE SanctuaryEnvironmentShadows.drawGroundShadows().
   * Because shadows are drawn AFTER this pass, building and tree cast shadows naturally occlude
   * the sunlit ground key pools!
   */
  fun DrawScope.drawGroundSunlightAndCloudModulation(
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
  ) {
    if (DepthPresentation.quality == DepthPresentation.Quality.OFF || zoom <= 0f) return
    val light = WorldEnvironment.lighting
    val sun = light.sunIntensity
    if (sun <= 0.08f) return

    val halfW = size.width * 0.5f
    val halfH = size.height * 0.5f
    fun sx(wx: Float) = ObliqueCamera.worldToScreenX(wx, camX, zoom, halfW)
    fun sy(wy: Float) = ObliqueCamera.worldToScreenY(wy, camY, zoom, halfH, 0f)

    // 1. Warm sunlit ground key pools on open courtyards (foreshortened by OBLIQUE_Y_SCALE = 0.72)
    val keyAlpha = (sun * 0.11f * (1f - WorldEnvironment.weather.cloud * 0.65f)).coerceIn(0f, 0.12f)
    if (keyAlpha > 0.01f) {
      for (i in shaftAnchors.indices) {
        val a = shaftAnchors[i]
        val cx = sx(a.worldX)
        val cy = sy(a.worldY)
        val rx = a.groundWidthWorld * 1.35f * zoom
        val ry = rx * ObliqueCamera.OBLIQUE_Y_SCALE
        if (cx + rx < 0f || cx - rx > size.width || cy + ry < 0f || cy - ry > size.height) continue
        drawOval(
          color = Color(0xFFFFEAB8).copy(alpha = keyAlpha),
          topLeft = Offset(cx - rx, cy - ry),
          size = Size(rx * 2f, ry * 2f),
        )
      }
    }

    // 2. Drifting World-Space Cloud Shadow Modulation (breaks up flat tile repetition across island)
    val cloudShadowAlpha = (sun * 0.09f + WorldEnvironment.weather.cloud * 0.06f).coerceIn(0f, 0.13f)
    for (c in 0 until 3) {
      val driftWx = ((1100f + c * 820f + timeSec * (22f + c * 5f)) % 3000f) + 200f
      val driftWy = 680f + c * 560f + sin(timeSec * 0.18f + c * 2.1f) * 140f
      val cx = sx(driftWx)
      val cy = sy(driftWy)
      val rx = (460f + c * 70f) * zoom
      val ry = rx * ObliqueCamera.OBLIQUE_Y_SCALE
      if (cx + rx < 0f || cx - rx > size.width || cy + ry < 0f || cy - ry > size.height) continue
      drawOval(
        color = Color(0xFF0E1729).copy(alpha = cloudShadowAlpha),
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f),
      )
    }
  }

  /**
   * Layer 4: World-Anchored Directional Sun Shafts (Replaces static screen-space drawSkyRays).
   * Shafts move and scale with camX, camY, and zoom, aligned with SanctuaryLight (Azimuth 142°, Elev 48°).
   */
  fun DrawScope.drawWorldSunShafts(
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
  ) {
    if (DepthPresentation.quality == DepthPresentation.Quality.OFF || zoom <= 0f) return
    val light = WorldEnvironment.lighting
    val baseAlpha = light.rayAlpha.coerceAtMost(0.13f)
    if (baseAlpha <= 0.015f) return

    val halfW = size.width * 0.5f
    val halfH = size.height * 0.5f
    val dirX = light.directionX // ~ +0.615 (NW -> SE)
    val dirY = light.directionY // ~ +0.788 (North -> South)

    for (i in shaftAnchors.indices) {
      val a = shaftAnchors[i]
      val pulse = 0.86f + 0.14f * sin(timeSec * 0.55f + a.phaseOffset)
      val shaftAlpha = (baseAlpha * pulse).coerceIn(0f, 0.13f)

      val groundCx = ObliqueCamera.worldToScreenX(a.worldX, camX, zoom, halfW)
      val groundCy = ObliqueCamera.worldToScreenY(a.worldY, camY, zoom, halfH, 0f)
      val halfWGround = a.groundWidthWorld * 0.5f * zoom

      // Upper sky entry point projected backwards along the directional light vector
      val skyWx = a.worldX - dirX * a.heightZ * 1.35f
      val skyWy = a.worldY - dirY * a.heightZ * 0.85f
      val topCx = ObliqueCamera.worldToScreenX(skyWx, camX, zoom, halfW)
      val topCy = ObliqueCamera.worldToScreenY(skyWy, camY, zoom, halfH, a.heightZ)
      val halfWTop = halfWGround * 0.78f

      if (maxOf(groundCx + halfWGround, topCx + halfWTop) < -40f ||
          minOf(groundCx - halfWGround, topCx - halfWTop) > size.width + 40f ||
          groundCy < -40f || topCy > size.height + 40f) continue

      // Outer soft shaft body
      reusableShaftPath.reset()
      reusableShaftPath.moveTo(topCx - halfWTop, topCy)
      reusableShaftPath.lineTo(topCx + halfWTop, topCy)
      reusableShaftPath.lineTo(groundCx + halfWGround, groundCy)
      reusableShaftPath.lineTo(groundCx - halfWGround, groundCy)
      reusableShaftPath.close()
      drawPath(reusableShaftPath, light.rayColor.copy(alpha = shaftAlpha * 0.62f))

      // Inner warm core shaft (narrower, brighter)
      if (DepthPresentation.quality == DepthPresentation.Quality.BALANCED) {
        val coreTopW = halfWTop * 0.46f
        val coreBotW = halfWGround * 0.46f
        reusableShaftPath.reset()
        reusableShaftPath.moveTo(topCx - coreTopW, topCy)
        reusableShaftPath.lineTo(topCx + coreTopW, topCy)
        reusableShaftPath.lineTo(groundCx + coreBotW, groundCy)
        reusableShaftPath.lineTo(groundCx - coreBotW, groundCy)
        reusableShaftPath.close()
        drawPath(reusableShaftPath, Color(0xFFFFF2C6).copy(alpha = shaftAlpha * 0.48f))

        // Foreshortened warm floor highlight where the shaft touches the ground plane
        val landingRx = halfWGround * 0.92f
        val landingRy = landingRx * ObliqueCamera.OBLIQUE_Y_SCALE
        drawOval(
          color = Color(0xFFFFE8A3).copy(alpha = shaftAlpha * 0.55f),
          topLeft = Offset(groundCx - landingRx, groundCy - landingRy),
          size = Size(landingRx * 2f, landingRy * 2f),
        )
      }
    }
  }
}
```
