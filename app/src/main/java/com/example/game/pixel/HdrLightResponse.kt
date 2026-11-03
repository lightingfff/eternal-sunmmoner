package com.example.game.pixel

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.game.map.ObliqueCamera
import com.example.game.map.PropKind
import com.example.game.map.WorldObstacle
import com.example.game.world.WorldEnvironment
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * P24 — bounded HDR-style light response for the existing 2.5D Canvas renderer.
 *
 * This is NOT a second lighting pipeline: it is the single light-stamp owner used by
 * [PixelSpriteRenderer.drawProp] so the production lobby AND the Robolectric render
 * harness produce the same image. It deliberately does not add a fullscreen pass,
 * RenderEffect, AGSL/RuntimeShader, blur, or a global brightness/contrast overlay.
 *
 * What "HDR" means here, concretely and cheaply:
 *  - one consistent cold key light from the upper-left (the same direction as
 *    `SanctuaryLight`, whose cast shadow falls toward SE);
 *  - per-source emissive cores, ground spill on the foreshortened .72 plane and a
 *    very light halo for genuinely strong sources only;
 *  - material-dependent selective highlight on the key-lit face plus a paired
 *    shade on the opposite face, giving roof/front/side planes separate values;
 *  - exposure balance driven by day/night and rain, applied ONLY to local light
 *    alpha (never to the whole scene), with a soft-knee highlight cap;
 *  - cached 48px radial masks built once per tint, culled before any draw, and
 *    OFF / REDUCED / BALANCED tiers that shed draw work without touching gameplay.
 *
 * Sprite bitmaps themselves keep `FilterQuality.None`. Only cached light masks use
 * `FilterQuality.Low`, and their alpha is capped so pixel art is never washed out.
 */
object HdrLightResponse {

  /** Cached mask palette. Warm amber/gold for windows and lanterns, cold for arcane sources. */
  enum class Tint { AMBER, GOLD, AETHER, VIOLET, EMBER, MOON, SHADE }

  /** Drives how strongly a material answers the key light and how sharp its highlight is. */
  enum class Material { STONE, WOOD, METAL, ROOF, FOLIAGE, CRYSTAL }

  data class LightSpec(
    val tint: Tint,
    val material: Material,
    val groundRadius: Float,
    val groundAlpha: Float,
    val coreRadius: Float,
    val coreAlpha: Float,
    val haloRadius: Float,
    val haloAlpha: Float,
    val emitterHeight: Float,
    val flicker: Boolean,
  )

  private const val MASK_SIZE = 48
  private const val MAX_STAMP_ALPHA = 0.22f
  private const val MAX_CORE_ALPHA = 0.46f

  @Volatile
  private var maskBuilds = 0

  /** Number of masks actually decoded. Must stay constant after warm-up (no per-frame bitmap). */
  val cachedMaskCount: Int get() = maskBuilds

  private val amberMask by lazy { radial(0xffffb84d.toInt(), 3) }
  private val goldMask by lazy { radial(0xffffdca0.toInt(), 3) }
  private val aetherMask by lazy { radial(0xff70d0ff.toInt(), 3) }
  private val violetMask by lazy { radial(0xffa78bfa.toInt(), 3) }
  private val emberMask by lazy { radial(0xffff6b35.toInt(), 3) }
  private val moonMask by lazy { radial(0xffe8e0c8.toInt(), 2) }
  private val shadeMask by lazy { radial(0xff0a1020.toInt(), 2) }

  private fun radial(rgb: Int, power: Int): ImageBitmap {
    val px = IntArray(MASK_SIZE * MASK_SIZE)
    val half = (MASK_SIZE - 1) / 2f
    for (y in 0 until MASK_SIZE) for (x in 0 until MASK_SIZE) {
      val dx = (x - half) / half
      val dy = (y - half) / half
      val r2 = dx * dx + dy * dy
      val f = (1f - r2).coerceAtLeast(0f)
      var a = f
      repeat(power - 1) { a *= f }
      px[y * MASK_SIZE + x] = ((255f * a).roundToInt().coerceIn(0, 255) shl 24) or (rgb and 0x00ffffff)
    }
    maskBuilds++
    return Bitmap.createBitmap(px, MASK_SIZE, MASK_SIZE, Bitmap.Config.ARGB_8888).asImageBitmap()
  }

  /** Exposed for the render/contract test to assert the masks are reused, not rebuilt. */
  fun maskFor(tint: Tint): ImageBitmap = when (tint) {
    Tint.AMBER -> amberMask
    Tint.GOLD -> goldMask
    Tint.AETHER -> aetherMask
    Tint.VIOLET -> violetMask
    Tint.EMBER -> emberMask
    Tint.MOON -> moonMask
    Tint.SHADE -> shadeMask
  }

  private fun color(tint: Tint): Color = when (tint) {
    Tint.AMBER -> Color(0xffffb84d.toInt())
    Tint.GOLD -> Color(0xffffdca0.toInt())
    Tint.AETHER -> Color(0xff70d0ff.toInt())
    Tint.VIOLET -> Color(0xffa78bfa.toInt())
    Tint.EMBER -> Color(0xffff6b35.toInt())
    Tint.MOON -> Color(0xffe8e0c8.toInt())
    Tint.SHADE -> Color(0xff0a1020.toInt())
  }

  /**
   * Exposure balance: artificial and arcane sources read stronger at night, and wet
   * stone answers a little more under rain. Daytime stays calm. This multiplies LOCAL
   * light alpha only — the ambient grade, sky tint and sprite colours are untouched,
   * so the scene is never globally brightened.
   */
  fun exposureBalance(): Float {
    val lighting = WorldEnvironment.lighting
    val night = (1f - lighting.ambientIntensity).coerceIn(0f, 1f)
    val wet = WorldEnvironment.weather.rain.coerceIn(0f, 1f)
    return (0.92f + night * 1.35f + wet * 0.16f).coerceIn(0.80f, 1.65f)
  }

  /** Soft-knee highlight cap: strong cores approach [cap] without a hard white clip. */
  private fun toneMapAlpha(a: Float, cap: Float): Float {
    val x = a.coerceAtLeast(0f)
    if (x <= 0f) return 0f
    return (2f * cap * x / (x + cap)).coerceAtMost(cap)
  }

  private fun boundedAlpha(a: Float, cap: Float): Float = a.coerceIn(0f, cap)

  private fun enabled(): Boolean = DepthPresentation.quality != DepthPresentation.Quality.OFF
  private fun balanced(): Boolean = DepthPresentation.quality == DepthPresentation.Quality.BALANCED

  private fun flicker(spec: LightSpec, obs: WorldObstacle, timeSec: Float): Float =
    if (spec.flicker) 0.94f + 0.06f * sin(timeSec * 3.1f + obs.x * 0.013f) else 1f

  /**
   * Derive the emitter pixel from the real cached sprite, exactly as the previous
   * lobby light pass did. A halo must sit on the glass/flame, not float at an
   * unrelated fixed height. Falls back to the authored spec value when no sprite exists.
   */
  private fun emitterHeightFor(obs: WorldObstacle, fallback: Float): Float {
    val sprite = if (obs.propKind == PropKind.ARCANE_BRAZIER) PixelStructureAtlas.brazierFrames[0]
    else SanctuaryEnvironmentShadows.sprite(obs) ?: return fallback
    val spriteScale = if (obs.propKind == PropKind.ARCANE_BRAZIER) 1.95f
    else ObliqueSpriteGeometry.pixelScale(obs.propKind)
    val foot = if (obs.propKind == PropKind.ARCANE_BRAZIER) 4f
    else ObliqueSpriteGeometry.drawFootOffset(obs.propKind, sprite)
    return when (obs.propKind) {
      PropKind.STREET_LANTERN -> (sprite.height - 22f) * spriteScale - foot
      PropKind.ARCANE_BRAZIER -> (sprite.height - 18f) * spriteScale - foot
      else -> fallback
    }
  }

  private val buildings = setOf(
    PropKind.FACILITY_TOWER_GATE, PropKind.TOWER_PORTAL_GATE,
    PropKind.FACILITY_SUMMONING_HALL, PropKind.FACILITY_HERO_REGISTRY,
    PropKind.FACILITY_ARMORY, PropKind.FACILITY_SYNTHESIS_LAB,
    PropKind.FACILITY_HERO_QUARTERS, PropKind.FACILITY_RESTAURANT,
    PropKind.FACILITY_TRAINING_GROUND,
    PropKind.MEDIEVAL_TOWNHOUSE_A, PropKind.MEDIEVAL_TOWNHOUSE_B,
    PropKind.SANCTUARY_COTTAGE_BROWN, PropKind.SANCTUARY_COTTAGE_SLATE,
    PropKind.FORTRESS_CORNER_TOWER, PropKind.SOUTHERN_GATE_TOWER,
  )

  fun isBuilding(kind: PropKind): Boolean = kind in buildings

  /** One authored light spec per real emitting prop. Null = no local source (no invented lights). */
  fun specFor(kind: PropKind): LightSpec? = when (kind) {
    PropKind.STREET_LANTERN -> LightSpec(
      Tint.AMBER, Material.METAL, 58f, .100f, 7f, .30f, 20f, .10f, 46f, true,
    )
    PropKind.ARCANE_BRAZIER -> LightSpec(
      Tint.AETHER, Material.CRYSTAL, 66f, .095f, 9f, .32f, 24f, .10f, 54f, true,
    )
    // The rift keeps its DARK aperture (QA contract). No filled core here: the cold
    // ground pool, the violet halo and the existing animated ring carry the source.
    PropKind.FACILITY_TOWER_GATE, PropKind.TOWER_PORTAL_GATE -> LightSpec(
      Tint.VIOLET, Material.CRYSTAL, 150f, .150f, 0f, .00f, 54f, .14f, 196f, true,
    )
    PropKind.FACILITY_SUMMONING_HALL -> LightSpec(
      Tint.AETHER, Material.STONE, 96f, .090f, 0f, .00f, 32f, .09f, 70f, false,
    )
    PropKind.FACILITY_ARMORY -> LightSpec(
      Tint.EMBER, Material.STONE, 72f, .085f, 0f, .00f, 22f, .075f, 78f, false,
    )
    PropKind.FACILITY_HERO_QUARTERS, PropKind.FACILITY_RESTAURANT,
    PropKind.FACILITY_HERO_REGISTRY, PropKind.FACILITY_SYNTHESIS_LAB,
    PropKind.MEDIEVAL_TOWNHOUSE_A, PropKind.MEDIEVAL_TOWNHOUSE_B,
    PropKind.SANCTUARY_COTTAGE_BROWN, PropKind.SANCTUARY_COTTAGE_SLATE -> LightSpec(
      Tint.GOLD, Material.WOOD, 48f, .072f, 0f, .00f, 16f, .055f, 62f, false,
    )
    PropKind.FORGE_ANVIL_STATION, PropKind.ALCHEMICAL_CRUCIBLE,
    PropKind.TAVERN_HEARTH_FIREPLACE -> LightSpec(
      Tint.EMBER, Material.METAL, 56f, .100f, 10f, .30f, 20f, .09f, 26f, true,
    )
    PropKind.CRIMSON_CRYSTAL, PropKind.OBSIDIAN_COLUMN,
    PropKind.ANCIENT_MONOLITH, PropKind.SUMMON_OBELISK,
    PropKind.ASTRAL_HERB_SHRINE -> LightSpec(
      Tint.VIOLET, Material.CRYSTAL, 52f, .085f, 9f, .28f, 18f, .08f, 40f, false,
    )
    else -> null
  }

  /** Cached radial blit. Always culled first; alpha is capped so no white flood is possible. */
  fun DrawScope.drawHdrStamp(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    tint: Tint,
    alpha: Float,
    crisp: Boolean = false,
  ) {
    if (alpha <= 0f || radiusX <= 0f || radiusY <= 0f) return
    val rx = radiusX.coerceAtMost(320f)
    val ry = radiusY.coerceAtMost(320f)
    if (center.x + rx < 0f || center.x - rx > size.width ||
      center.y + ry < 0f || center.y - ry > size.height
    ) return
    drawImage(
      image = maskFor(tint),
      dstOffset = IntOffset((center.x - rx).roundToInt(), (center.y - ry).roundToInt()),
      dstSize = IntSize((rx * 2f).roundToInt().coerceAtLeast(1), (ry * 2f).roundToInt().coerceAtLeast(1)),
      alpha = alpha.coerceIn(0f, MAX_STAMP_ALPHA),
      filterQuality = if (crisp) FilterQuality.None else FilterQuality.Low,
    )
  }

  /**
   * BEFORE the sprite: ground spill on the .72 plane, plus a tight directional contact
   * rim at building feet. The long cast silhouette stays owned by
   * `SanctuaryEnvironmentShadows`; this only adds the short ambient-occlusion contact.
   */
  fun DrawScope.drawHdrGround(
    obs: WorldObstacle,
    cx: Float,
    cy: Float,
    s: Float,
    timeSec: Float,
  ) {
    if (!enabled() || s <= 0f) return
    val wet = WorldEnvironment.weather.rain.coerceIn(0f, 1f)
    if (isBuilding(obs.propKind)) {
      // Short, SE-biased contact AO. Not a generic oval: it sits on the same ground
      // plane and follows the authored shadow direction.
      val rimW = 76f * s
      drawHdrStamp(
        Offset(cx + 8f * s, cy + 2f * s),
        rimW, rimW * ObliqueCamera.OBLIQUE_Y_SCALE * 0.22f,
        Tint.SHADE, .13f,
      )
    }
    val spec = specFor(obs.propKind) ?: return
    val exposure = exposureBalance()
    val pulse = flicker(spec, obs, timeSec)
    val rx = spec.groundRadius * s
    drawHdrStamp(
      Offset(cx, cy + 4f * s),
      rx, rx * ObliqueCamera.OBLIQUE_Y_SCALE,
      spec.tint,
      boundedAlpha(spec.groundAlpha * exposure * pulse, MAX_STAMP_ALPHA),
    )
    // Wet stone answers the source with a tighter, flatter specular pool (BALANCED only).
    if (balanced() && wet > 0.01f) {
      drawHdrStamp(
        Offset(cx, cy + 2f * s),
        rx * 0.60f, rx * ObliqueCamera.OBLIQUE_Y_SCALE * 0.32f,
        Tint.MOON,
        boundedAlpha(spec.groundAlpha * exposure * wet * 0.85f, .11f),
      )
    }
  }

  /**
   * AFTER the sprite: emissive core at the real emitter pixel, a restrained halo for
   * strong sources, and the paired key-light / shade material response that gives the
   * roof, front and side planes separate values. No screen-wide effect.
   */
  fun DrawScope.drawHdrEmissive(
    obs: WorldObstacle,
    cx: Float,
    cy: Float,
    s: Float,
    timeSec: Float,
  ) {
    if (!enabled() || s <= 0f) return
    val spec = specFor(obs.propKind) ?: return
    val exposure = exposureBalance()
    val pulse = flicker(spec, obs, timeSec)
    val top = cy - emitterHeightFor(obs, spec.emitterHeight) * s
    val envelope = (spec.haloRadius + spec.coreRadius + 40f) * s
    if (cx + envelope < 0f || cx - envelope > size.width ||
      top + envelope < 0f || top - envelope > size.height
    ) return

    if (balanced() && spec.haloAlpha > 0f) {
      drawHdrStamp(
        Offset(cx, top), spec.haloRadius * s, spec.haloRadius * s,
        spec.tint, boundedAlpha(spec.haloAlpha * exposure * pulse, .14f),
      )
    }
    // Crisp core: one solid circle at the actual glass/flame/rift pixel. Windows and
    // other multi-source facades declare coreRadius = 0 so no invented dot appears.
    if (spec.coreRadius > 0f && spec.coreAlpha > 0f) {
      val a = toneMapAlpha(spec.coreAlpha * exposure * pulse, MAX_CORE_ALPHA)
      drawCircle(color(spec.tint).copy(alpha = a), spec.coreRadius * s, Offset(cx, top))
      drawCircle(
        Color.White.copy(alpha = boundedAlpha(a * 0.32f, .16f)),
        spec.coreRadius * s * 0.40f, Offset(cx, top),
      )
    }
    drawMaterialResponse(obs, cx, cy, s, spec, exposure)
  }

  /**
   * Local tone/volume grade: the key-lit NW plane gains a cool highlight, the opposite
   * SE plane gains a paired shade. Both are cached stamps with low alpha, so this
   * expands LOCAL contrast (richer light) without a global brightness or blur pass.
   * Skipped below gameplay size, where the extra work would not be visible.
   */
  private fun DrawScope.drawMaterialResponse(
    obs: WorldObstacle,
    cx: Float,
    cy: Float,
    s: Float,
    spec: LightSpec,
    exposure: Float,
  ) {
    if (s < 0.45f || !balanced()) return
    val wet = WorldEnvironment.weather.rain.coerceIn(0f, 1f)
    val key = when (spec.material) {
      Material.STONE -> .085f
      Material.WOOD -> .065f
      Material.METAL -> .140f
      Material.ROOF -> .100f
      Material.FOLIAGE -> .050f
      Material.CRYSTAL -> .130f
    }
    val w = 78f * s
    val h = 96f * s
    drawHdrStamp(
      Offset(cx - w * 0.45f, cy - h * 0.72f),
      w * 0.95f, h * 0.78f,
      Tint.MOON,
      boundedAlpha(key * exposure * (1f + wet * 0.5f), .16f),
    )
    drawHdrStamp(
      Offset(cx + w * 0.52f, cy - h * 0.30f),
      w * 0.92f, h * 0.70f,
      Tint.SHADE,
      boundedAlpha(key * 0.85f * exposure, .14f),
    )
  }
}
