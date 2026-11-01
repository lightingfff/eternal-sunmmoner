package com.example.game.pixel

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Cached atlas recipes. All coordinates wrap: motifs cross tile edges instead of leaving bare borders. */
internal object SanctuaryMaterials {
  private const val N = 64
  private fun color(rgb: Int) = rgb or (0xff shl 24)
  private fun PixelCanvas.wrap(x: Int, y: Int, rgb: Int) = set(Math.floorMod(x, N), Math.floorMod(y, N), color(rgb))

  fun grass(variant: Int): ImageBitmap {
    val c = PixelCanvas(N, N)
    val rng = Random(419 + variant * 73)
    // Periodic broad value fields, quantized to a restrained palette, rather than pixel noise.
    val palette = intArrayOf(0x293b32, 0x304437, 0x374c3c, 0x3d5340)
    for (y in 0 until N) for (x in 0 until N) {
      val field = sin(x * Math.PI / 32) * cos(y * Math.PI / 32) +
        0.45 * sin((x + y) * Math.PI / 16)
      c.wrap(x, y, palette[((field + 1.5) * 1.2).toInt().coerceIn(0, 3)])
    }
    // Sparse soil islands; clipped by lobed boundaries and surrounded by darker moss.
    repeat(if (variant == 3 || variant == 7) 3 else 1) {
      val cx = rng.nextInt(N); val cy = rng.nextInt(N)
      for (dy in -4..4) for (dx in -8..8) {
        val d = dx * dx / 64f + dy * dy / 16f
        if (d < 1f) c.wrap(cx + dx, cy + dy, if (d > 0.6f) 0x293c30 else 0x3e3930)
      }
    }
    // Each motif is a connected fan of blades, with contact shade and upper-left light.
    repeat(if (variant == 4) 40 else 27) {
      val x = rng.nextInt(N); val y = rng.nextInt(N)
      val length = 2 + rng.nextInt(3)
      for (dx in -2..2) c.wrap(x + dx, y + 1, 0x24372d)
      for (h in 0..length) {
        c.wrap(x, y - h, 0x587451)
        if (h < length - 1) {
          c.wrap(x - 1 - h / 2, y - h, 0x486544)
          c.wrap(x + 2 + h / 2, y - h, 0x3b573b)
        }
      }
      c.wrap(x - 1, y - length, 0x6b8058)
    }
    repeat(7) {
      val x = rng.nextInt(N); val y = rng.nextInt(N)
      for (dy in 0..2) for (dx in 0..3) {
        if (dx + dy != 0) c.wrap(x + dx, y + dy, if (dy == 0) 0x526344 else 0x3f5239)
      }
    }
    return c.toImageBitmap()
  }

  /** Toroidal jittered cells form individual worn stones, including across tile boundaries. */
  fun stone(variant: Int, large: Boolean): ImageBitmap {
    val c = PixelCanvas(N, N)
    val cols = if (large) 3 else 5
    val rows = if (large) 4 else 7
    val rng = Random(1031 + variant * 151 + if (large) 70 else 0)
    val count = cols * rows
    val xs = FloatArray(count); val ys = FloatArray(count)
    for (i in 0 until count) {
      xs[i] = ((i % cols + 0.5f + (i / cols % 2) * 0.45f + rng.nextFloat() * 0.3f) * N / cols) % N
      ys[i] = ((i / cols + 0.5f + rng.nextFloat() * 0.3f) * N / rows) % N
    }
    val palette = intArrayOf(0x59636a, 0x626b70, 0x535e65, 0x6b7171, 0x505b62)
    for (y in 0 until N) for (x in 0 until N) {
      var first = Float.MAX_VALUE; var second = Float.MAX_VALUE
      var cell = 0; var lx = 0f; var ly = 0f
      for (i in 0 until count) {
        var dx = x - xs[i]; var dy = (y - ys[i])
        if (dx > 32) dx -= N else if (dx < -32) dx += N
        if (dy > 32) dy -= N else if (dy < -32) dy += N
        val d = sqrt(dx * dx + dy * dy * 1.5f)
        if (d < first) { second = first; first = d; cell = i; lx = dx; ly = dy }
        else if (d < second) second = d
      }
      val edge = second - first
      val rgb = when {
        edge < 1.15f -> 0x252f32
        edge < 2.6f -> if (lx + ly < 0) 0x899088 else 0x3a464d
        // Tooling follows the stone face; no independent random specks.
        ly.toInt() == 1 && lx > -3 && lx < 3 && cell % 7 == 0 -> 0x46535a
        edge < 3.4f && cell % 5 == 0 && ly > 0 -> 0x4a5943
        else -> palette[(cell + variant) % palette.size]
      }
      c.wrap(x, y, rgb)
    }
    return c.toImageBitmap()
  }

  fun soil(variant: Int, gravel: Boolean = false): ImageBitmap {
    val c = PixelCanvas(N, N)
    val rng = Random(900 + variant * 31)
    val tones = intArrayOf(0x443e34, 0x4c4538, 0x574d3d, 0x605542)
    for (y in 0 until N) for (x in 0 until N) {
      val field = sin(x * Math.PI / 32) + cos((x + y) * Math.PI / 32)
      c.wrap(x, y, tones[((field + 2) * 0.85).toInt().coerceIn(0, 3)])
    }
    repeat(if (gravel) 42 else 12) {
      val x = rng.nextInt(N); val y = rng.nextInt(N)
      val w = 2 + rng.nextInt(3)
      for (dx in 0 until w) {
        c.wrap(x + dx, y + 2, 0x343930)
        c.wrap(x + dx, y + 1, 0x6b6c5e)
        if (dx > 0) c.wrap(x + dx, y, 0x8a8972)
      }
    }
    // Short connected sediment ridges establish direction without a noisy grain overlay.
    repeat(8) {
      val x = rng.nextInt(N); val y = rng.nextInt(N)
      repeat(5 + rng.nextInt(6)) { dx ->
        c.wrap(x + dx, y + dx / 5, 0x3f3b33)
        c.wrap(x + dx, y + dx / 5 + 1, 0x655943)
      }
    }
    return c.toImageBitmap()
  }

  fun wood(variant: Int): ImageBitmap {
    val c = PixelCanvas(N, N)
    for (y in 0 until N) for (x in 0 until N) {
      val plank = y / 8
      val across = y % 8
      val end = Math.floorMod(x + (plank % 2) * 32, 64)
      val grain = sin(x * Math.PI / 16 + plank) + sin(x * Math.PI / 8 + across)
      val rgb = when {
        across == 0 || end == 0 -> 0x282b29
        across == 1 -> 0x877053
        across == 7 -> 0x3c352d
        grain > 1.1 -> 0x715b41
        grain < -1.1 -> 0x493d30
        else -> if ((plank + variant) % 3 == 0) 0x65533d else 0x5c4b38
      }
      c.wrap(x, y, rgb)
    }
    for (plank in 0..7) {
      val x = (plank * 19 + variant * 7) % 64; val y = plank * 8 + 4
      for (dx in -4..4) {
        c.wrap(x + dx, y, if (abs(dx) < 2) 0x332f29 else 0x796247)
        if (abs(dx) < 3) c.wrap(x + dx, y - 1, 0x493b2e)
      }
    }
    return c.toImageBitmap()
  }
}
