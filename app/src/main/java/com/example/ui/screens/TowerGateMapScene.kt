package com.example.ui.screens

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.data.model.HeroRecord
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import java.security.MessageDigest
import org.json.JSONObject

/** Authored 960x540 composition. One uniform transform for art, VFX and touch/accessibility.
 * Ground art uses .72 oblique compression; upright architecture is never vertically squashed.
 */
internal data class GateMapViewport(val width: Float, val height: Float) {
  val scale = minOf(width / 960f, height / 540f)
  val left = (width - 960f * scale) / 2f
  val top = (height - 540f * scale) / 2f
  fun screenRect(rect: Rect) = Rect(left + rect.left * scale, top + rect.top * scale,
    left + rect.right * scale, top + rect.bottom * scale)
}

internal data class GateArch(val x: Float, val base: Float, val radius: Float, val height: Float) {
  val bounds get() = Rect(x - radius - 24f, base - height - 24f, x + radius + 24f, base + 42f)
  fun path(): Path = Path().apply {
    val spring = base - height + radius
    moveTo(x - radius, base)
    lineTo(x - radius, spring)
    for (i in 0..32) {
      val angle = PI.toFloat() + i * PI.toFloat() / 32f
      lineTo(x + radius * cos(angle), spring + radius * sin(angle))
    }
    lineTo(x + radius, base)
    close()
  }
}

internal val CourtArch = GateArch(480f, 326f, 72f, 231f)
internal val NexusArches = listOf(
  GateArch(205f, 323f, 47f, 164f),
  GateArch(480f, 285f, 60f, 200f),
  GateArch(755f, 323f, 47f, 164f),
)

/** Owned by the screen, decoded once on entry, released when the screen leaves composition.
 * No asset IO, random generation or masonry allocation in a frame.
 */
internal class TowerGateMapArt(private val assets: AssetManager) {
  private val names = listOf("eternal_gate_court", "three_gate_nexus", "engraved_door_leaves")
  internal var sourcePack = "tower_gate_map"; private set
  internal var retainedBytes = 0L; private set
  private fun prepare(root: String, checked: Boolean): List<ImageBitmap> {
    val owned = mutableListOf<Bitmap>()
    try {
      val meta = if (checked) assets.open("$root/manifest.json").bufferedReader().use { JSONObject(it.readText()) } else null
      if (meta != null) require(meta.getInt("version") == 1 && meta.getJSONArray("assets").length() == 3)
      for ((i, name) in names.withIndex()) {
        val bytes = assets.open("$root/$name.png").use { it.readBytes() }
        require(bytes.size in 1..4_000_000)
        if (meta != null) {
          val entry = meta.getJSONArray("assets").getJSONObject(i)
          require(entry.getString("id") == name && entry.getString("file") == "$name.png")
          require(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } == entry.getString("sha256"))
        }
        val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
          BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 }))
        owned += bitmap
        require(bitmap.width == (if (i == 2) 144 else 960) && bitmap.height == (if (i == 2) 240 else 540))
        require(!bitmap.hasAlpha() || bitmap.isPremultiplied)
      }
      val total = owned.sumOf { it.allocationByteCount.toLong() }
      require(total <= 4_285_440L)
      val result = owned.map { it.asImageBitmap() }
      retainedBytes = total
      sourcePack = root
      return result
    } catch (error: Throwable) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      throw error
    }
  }
  private val images = try { prepare("art_completion_v1/gates", true) } catch (error: Exception) {
    prepare("tower_gate_map", false)
  } catch (error: OutOfMemoryError) { prepare("tower_gate_map", false) }
  val court = images[0]
  val nexus = images[1]
  val doors = images[2]
  val courtMask = CourtArch.path()
  val nexusMasks = NexusArches.map { it.path() }
}

internal fun DrawScope.drawTowerGateMap(
  art: TowerGateMapArt,
  cutscene: Boolean,
  time: Float,
  heroes: List<HeroRecord>,
) {
  drawRect(Color(0xFF091424))
  val viewport = GateMapViewport(size.width, size.height)
  if (viewport.scale <= 0f) return
  val opening = ((time - 1.5f) / 2.8f).coerceIn(0f, 1f)
  val climax = ((time - 4.3f) / 1.7f).coerceIn(0f, 1f)
  val tremor = if (cutscene && time in 1.5f..4.6f) sin(time * 45f) * 1.5f else 0f
  withTransform({
    translate(viewport.left, viewport.top)
    scale(viewport.scale, viewport.scale, Offset.Zero)
    translate(tremor, tremor * .3f)
  }) {
    // Sprite culling/projection must use scene coordinates too, including low-density portrait.
    // withTransform restores DrawContext.size after this block.
    drawContext.size = Size(960f, 540f)
    drawImage(if (cutscene) art.court else art.nexus, filterQuality = FilterQuality.None)
    if (cutscene) {
      drawGateLight(CourtArch, art.courtMask, Color(0xFF71DEEB), time, opening)
      if (opening > .02f) {
        repeat(7) { ray ->
          val endX = 170f + ray * 103f
          val fan = Path().apply {
            moveTo(480f - opening * 24f, 326f); lineTo(480f + opening * 24f, 326f)
            lineTo(endX + 26f, 540f); lineTo(endX - 26f, 540f); close()
          }
          drawPath(fan, Brush.verticalGradient(listOf(Color(0xFF9AE8EC).copy(alpha = .19f * opening), Color.Transparent), 326f, 540f))
        }
      }
      // Doors slide behind the arch, never over the pilasters or through the masonry.
      clipPath(art.courtMask) {
        for (side in 0..1) {
          val x = if (side == 0) 408f - 74f * opening else 480f + 74f * opening
          drawImage(art.doors, srcOffset = IntOffset(side * 72, 0), srcSize = IntSize(72, 240),
            dstOffset = IntOffset(x.toInt(), 90), dstSize = IntSize(72, 240), filterQuality = FilterQuality.None)
        }
      }
      val approach = (time / 1.8f).coerceIn(0f, 1f)
      for (i in 0..7) {
        val a = i * PI.toFloat() / 4f + time * .18f
        val p = Offset(480f + cos(a) * 134f, 417f + sin(a) * 96f)
        val strength = (approach * 8f - i).coerceIn(0f, 1f)
        drawLine(Color(0xFF87DDE2).copy(alpha = strength * .7f), p, p - Offset(0f, 30f * strength), 2f)
        drawRect(Color(0xFFF4D697).copy(alpha = strength), p - Offset(2f, 2f), Size(4f, 4f))
      }
      val squad = heroes.filter { it.inActiveSquad }.ifEmpty { heroes }.take(4)
      squad.forEachIndexed { i, hero ->
        val x = 480f + (i - (squad.size - 1) / 2f) * 44f
        val y = 428f + (1f - approach) * 42f + if (i % 3 == 0) 8f else 0f
        // The existing sprite projector retains its .72 ground scale in scene coordinates.
        val zoom = .62f
        drawCharacterSprite(worldX = 1000f, worldY = 1000f,
          camX = 1000f - (x - size.width / 2f) / zoom,
          camY = 1000f - (y - size.height / 2f) / (zoom * .72f), zoom = zoom,
          isMaster = false, heroClass = hero.classType, facingRight = i < 2,
          isMoving = approach < .95f, attackAnimProgress = 0f, hurtFlash = 0f,
          hpFraction = 1f, shieldOrAuraColor = Color(0xFF8CDCE3),
          isInjuredResting = false, timeSec = time, facingNorth = true, heroName = hero.name)
      }
      if (climax > .25f) drawRect(Color(0xFFE0F6FF).copy(alpha = ((climax - .25f) * .65f).coerceAtMost(.48f)), size = Size(960f, 540f))
    } else {
      NexusArches.forEachIndexed { index, arch ->
        drawGateLight(arch, art.nexusMasks[index], DimensionalNexusGate.entries[index].primaryColor, time + index * 1.2f, 1f)
      }
    }
    // Bounded pixel motes; no fullscreen blur or bloom pass.
    repeat(22) { i ->
      val x = (i * 137f + sin(time * .4f + i) * 13f) % 960f
      val y = 510f - ((time * 12f + i * 43f) % 390f)
      drawRect(Color(0xFFB7D9CD).copy(alpha = .32f + .18f * sin(time + i)), Offset(x.toInt().toFloat(), y.toInt().toFloat()), Size(2f, 2f))
    }
  }
}

private fun DrawScope.drawGateLight(arch: GateArch, mask: Path, color: Color, time: Float, strength: Float) {
  val pulse = .85f + .15f * sin(time * 2f)
  clipPath(mask) {
    drawRect(Brush.radialGradient(listOf(Color(0xFFF4F7D7), color, Color(0xFF14223F)),
      center = Offset(arch.x, arch.base - arch.height * .43f), radius = arch.height * .7f),
      topLeft = Offset(arch.x - arch.radius, arch.base - arch.height), size = Size(arch.radius * 2, arch.height), alpha = pulse)
    // Layered pixel currents follow an ellipse in the upright opening.
    repeat(34) { i ->
      val a = time * .65f + i * .63f
      val r = .22f + (i % 7) * .1f
      val x = arch.x + cos(a) * arch.radius * r
      val y = arch.base - arch.height * .48f + sin(a) * arch.height * .43f * r
      drawRect(Color(0xFFEDFFE8).copy(alpha = .25f + (i % 4) * .12f), Offset(x.toInt().toFloat(), y.toInt().toFloat()), Size(2f + i % 3, 2f))
    }
  }
  if (strength <= 0f) return
  val glowCenter = Offset(arch.x, arch.base + 31f)
  drawOval(Brush.radialGradient(listOf(color.copy(alpha = .28f * strength * pulse), Color.Transparent), glowCenter, arch.radius * 2.1f),
    topLeft = glowCenter - Offset(arch.radius * 2.1f, arch.radius * .72f), size = Size(arch.radius * 4.2f, arch.radius * 1.44f))
  // Light spills along the stairs; the opening itself remains the brightest point.
  repeat(3) { i ->
    val phase = (time * .3f + i / 3f) % 1f
    val r = arch.radius * (1f + phase)
    drawOval(color.copy(alpha = (1f - phase) * .35f * strength),
      Offset(arch.x - r, arch.base + 28f - r * .32f), Size(r * 2f, r * .64f), style = Stroke(1f))
  }
}
