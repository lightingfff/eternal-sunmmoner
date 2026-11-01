package com.example.game.pixel

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.abs

/** Authored branching silhouettes; leaf sprays are connected stepped planes, never scattered dots. */
internal object SanctuaryVegetation {
  private fun PixelCanvas.polygon(points: List<Pair<Int, Int>>, color: Int) {
    val minY = points.minOf { it.second }; val maxY = points.maxOf { it.second }
    for (y in minY..maxY) {
      val hits = ArrayList<Int>()
      for (i in points.indices) {
        val a = points[i]; val b = points[(i + 1) % points.size]
        if ((a.second <= y && b.second > y) || (b.second <= y && a.second > y))
          hits.add(a.first + (y - a.second) * (b.first - a.first) / (b.second - a.second))
      }
      hits.sort()
      for (i in 0 until hits.size - 1 step 2) hLine(hits[i], y, hits[i + 1] - hits[i] + 1, color)
    }
  }
  fun tree(variant: Int, w: Int, h: Int): ImageBitmap {
    val c = PixelCanvas(w, h)
    val x = w / 2; val foot = h - 8
    val outline = 0xff17262b.toInt(); val bark = 0xff423d35.toInt()
    // Asymmetric roots, bending bole and branching negative spaces.
    c.polygon(listOf(x-19 to foot, x-8 to foot-9, x-6 to h/2, x-15 to h/3,
      x-8 to h/3+1, x+1 to h/2+8, x+13 to h/3-4, x+18 to h/3,
      x+7 to h/2+17, x+9 to foot-9, x+23 to foot+1, x+8 to foot-2, x to foot-7, x-8 to foot), outline)
    for (y in h/2..foot-3) {
      val bend = (foot-y)/13
      c.hLine(x-5-bend, y, 11, bark)
      c.vLine(x-4-bend, y, 1, 0xff75634b.toInt())
      if (y % 11 < 8) c.hLine(x+2-bend, y, 2, 0xff2a302c.toInt())
    }
    // Broad oak, wind-bent ash, or narrow blue spruce. Each crown has authored branch tiers.
    val masses = when (variant % 3) {
      0 -> listOf(Triple(-26, 58, 25), Triple(26, 64, 23), Triple(1, 46, 32), Triple(-18, 29, 24), Triple(19, 32, 23), Triple(-2, 16, 19))
      1 -> listOf(Triple(-24, 66, 24), Triple(24, 57, 23), Triple(-9, 45, 28), Triple(18, 30, 23), Triple(-9, 20, 22))
      else -> listOf(Triple(0, 76, 27), Triple(-3, 59, 24), Triple(2, 43, 20), Triple(0, 28, 16), Triple(-2, 14, 10))
    }
    val palette = when (variant % 4) {
      1 -> intArrayOf(0xff29313d.toInt(),0xff414957.toInt(),0xff59616a.toInt(),0xff7a817e.toInt())
      3 -> intArrayOf(0xff30303b.toInt(),0xff4b4355.toInt(),0xff66596a.toInt(),0xff8b7c86.toInt())
      else -> intArrayOf(0xff203532.toInt(),0xff344c41.toInt(),0xff506751.toInt(),0xff7b8960.toInt())
    }
    for ((index, mass) in masses.withIndex()) {
      val cx = x + mass.first * w / 114; val cy = mass.second * h / 132
      val r = mass.third * w / 114
      // Serrated, drooping branch outline rather than overlapping circles.
      val shape = listOf(-r to 5, -r+5 to -3, -r+3 to -7, -r/2 to -10,
        -r/3 to -17, 0 to -19, r/3 to -13, r/2+3 to -14,
        r-3 to -3, r to 8, r-7 to 8, r-5 to 13, r/3 to 12,
        r/5 to 17, -r/4 to 14, -r/2 to 16, -r/2-4 to 9)
      c.polygon(shape.map { cx+it.first to cy+it.second }, palette[0])
      for (row in 0..3) {
        val yy = cy-12 + row*6
        val span = r - abs(row-1)*4 - 4
        for (col in -span..span step 7) {
          val px = cx+col; val py = yy + ((col+index*3+48)/7%3)*2
          val shade = if (row == 3 || col > r/2) 1 else 2
          c.polygon(listOf(px-4 to py+3, px-2 to py-1, px+2 to py-3,
            px+5 to py, px+3 to py+2, px+1 to py+2, px-1 to py+5), palette[shade])
          if (row < 2 && col < r/3) c.hLine(px-1, py-2, 4, palette[3])
        }
      }
    }
    // Root-adjacent moss and broken slate compose a small, non-colliding base vignette.
    c.polygon(listOf(x-23 to foot+2,x-18 to foot-2,x-10 to foot,x-9 to foot+4,x-22 to foot+5),0xff424e4c.toInt())
    c.hLine(x-20,foot,7,0xff74806a.toInt())
    c.hLine(x+10,foot+2,9,0xff465c42.toInt())
    return c.toImageBitmap()
  }
  fun shrub(): ImageBitmap {
    val c = PixelCanvas(64, 40)
    for ((x,y,r) in listOf(Triple(17,24,13), Triple(36,18,17), Triple(49,28,11))) {
      c.polygon(listOf(x-r to y+4,x-r+3 to y-4,x-4 to y-9,x+2 to y-11,x+r-3 to y-4,x+r to y+5,x+4 to y+9,x-5 to y+7),0xff243a34.toInt())
      for (i in -r+4..r-4 step 5) {
        c.hLine(x+i,y-3+(i+20)%3,4,0xff647657.toInt())
        c.hLine(x+i-1,y+(i+20)%3,5,0xff3d5543.toInt())
      }
    }
    return c.toImageBitmap()
  }
}
