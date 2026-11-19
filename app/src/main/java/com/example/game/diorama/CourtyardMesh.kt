package com.example.game.diorama

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal data class Point3(val x: Float, val y: Float, val z: Float)

internal enum class Material3d(val asset: String? = null, val uvScale: Float = 1f) {
  GRASS("art_depth_v1/materials/sanctuary_grass.png"),
  STONE("diorama/cobblestone.png", uvScale = .5f),
  WALL("diorama/masonry.png"),
  TIMBER("art_depth_v1/materials/oak.png"),
  SLATE("diorama/slate.png", uvScale = .5f),
  PLASTER("diorama/plaster.png"),
  ROCK("art_depth_v1/materials/rock.png"),
  FOLIAGE("art_depth_v1/materials/forest_moss.png"),
  WATER, SOLID, HERO, EMISSIVE
}

internal class CourtyardMesh {
  private val vertices = LinkedHashMap<Material3d, FloatList>()

  private class FloatList {
    var data = FloatArray(512)
    var length = 0
    fun add(value: Float) {
      if (length == data.size) data = data.copyOf(length * 2)
      data[length++] = value
    }
  }

  private fun vertex(material: Material3d, p: Point3, n: Point3, u: Float, v: Float, rgb: Int) {
    val list = vertices.getOrPut(material, ::FloatList)
    list.add(p.x); list.add(p.y); list.add(p.z)
    list.add(n.x); list.add(n.y); list.add(n.z)
    list.add(u); list.add(v)
    list.add((rgb shr 16 and 255) / 255f)
    list.add((rgb shr 8 and 255) / 255f)
    list.add((rgb and 255) / 255f)
  }

  fun buffers(): Map<Material3d, FloatArray> = vertices.mapValues { (_, list) -> list.data.copyOf(list.length) }

  private fun normal(a: Point3, b: Point3, c: Point3): Point3 {
    val ab = Point3(b.x - a.x, b.y - a.y, b.z - a.z)
    val ac = Point3(c.x - a.x, c.y - a.y, c.z - a.z)
    return Point3(ab.y * ac.z - ab.z * ac.y, ab.z * ac.x - ab.x * ac.z, ab.x * ac.y - ab.y * ac.x)
  }

  fun quad(material: Material3d, a: Point3, b: Point3, c: Point3, d: Point3, rgb: Int, u: Float = 1f, v: Float = 1f) {
    val n = normal(a, b, c)
    vertex(material, a, n, 0f, 0f, rgb)
    vertex(material, b, n, u, 0f, rgb)
    vertex(material, c, n, u, v, rgb)
    vertex(material, a, n, 0f, 0f, rgb)
    vertex(material, c, n, u, v, rgb)
    vertex(material, d, n, 0f, v, rgb)
  }

  private fun triangle(material: Material3d, a: Point3, b: Point3, c: Point3, rgb: Int) {
    val n = normal(a, b, c)
    vertex(material, a, n, 0f, 0f, rgb)
    vertex(material, b, n, 1f, 0f, rgb)
    vertex(material, c, n, .5f, 1f, rgb)
  }

  fun plane(x0: Float, x1: Float, z0: Float, z1: Float, y: Float, material: Material3d, rgb: Int = 0xffffff, scale: Float = .66f) {
    quad(material, Point3(x0, y, z1), Point3(x1, y, z1), Point3(x1, y, z0), Point3(x0, y, z0),
      rgb, (x1 - x0) * scale, (z1 - z0) * scale)
  }

  private fun box(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
    material: Material3d, rgb: Int = 0xffffff, scale: Float = 1f,
  ) {
    val a = Point3(x0, y0, z0); val b = Point3(x1, y0, z0)
    val c = Point3(x1, y0, z1); val d = Point3(x0, y0, z1)
    val e = Point3(x0, y1, z0); val f = Point3(x1, y1, z0)
    val g = Point3(x1, y1, z1); val h = Point3(x0, y1, z1)
    val w = (x1 - x0) * scale; val y = (y1 - y0) * scale; val z = (z1 - z0) * scale
    quad(material, d, c, g, h, rgb, w, y)
    quad(material, b, a, e, f, rgb, w, y)
    quad(material, a, d, h, e, rgb, z, y)
    quad(material, c, b, f, g, rgb, z, y)
    quad(material, h, g, f, e, rgb, w, z)
  }

  private fun house(house: Courtyard.House) {
    val (l, n, r, s) = house.bounds
    val y = Courtyard.height(l, n)
    val eave = y + house.height
    val ridge = eave + house.roof
    val mid = (l + r) / 2
    val wallTint = if (house.style == 1) 0xe5d5b6 else 0xf0e1c8
    val roofTint = when (house.style) { 0 -> 0x76979e; 1 -> 0xa89684; else -> 0x868eac }
    box(l - .1f, y, n - .1f, r + .1f, y + .33f, s + .12f, Material3d.WALL, 0xa19b85)
    box(l, y + .33f, n, r, y + .74f, s, Material3d.WALL, 0xc4b79c, .7f)
    box(l, y + .74f, n, r, eave, s, Material3d.PLASTER, wallTint, .9f)
    triangle(Material3d.PLASTER, Point3(l, eave, s), Point3(r, eave, s), Point3(mid, ridge, s), wallTint)
    triangle(Material3d.PLASTER, Point3(r, eave, n), Point3(l, eave, n), Point3(mid, ridge, n), wallTint)
    for (h in 0..3) {
      val base = y + .45f + h * .34f
      val width = if (h % 2 == 0) .36f else .25f
      box(l - .025f, base, s - .23f, l + width, base + .315f, s + .025f, Material3d.WALL, 0xc6b8a0)
      box(r - width, base, s - .23f, r + .025f, base + .315f, s + .025f, Material3d.WALL, 0xc6b8a0)
    }
    roofPlane(l - .34f, mid, n - .28f, s + .36f, eave - .12f, ridge, roofTint)
    roofPlane(mid, r + .34f, n - .28f, s + .36f, ridge, eave - .12f, roofTint)
    frontBeam(l - .34f, eave - .12f, mid, ridge, s + .34f, .15f, .16f)
    frontBeam(mid, ridge, r + .34f, eave - .12f, s + .34f, .15f, .16f)
    box(mid - .07f, ridge - .035f, n - .24f, mid + .07f, ridge + .13f, s + .26f, Material3d.TIMBER, 0x554742)

    val beamTint = 0x9c6f4f
    for (x in listOf(l + .12f, r - .12f, mid)) {
      val bottom = if (x == mid) y + 1.98f else y + .28f
      box(x - .07f, bottom, s, x + .07f, eave, s + .09f, Material3d.TIMBER, beamTint)
    }
    for (level in floatArrayOf(y + .32f, y + 1.95f, eave - .09f)) {
      box(l, level, s, r, level + .13f, s + .10f, Material3d.TIMBER, beamTint)
    }
    val guildhall = house.style == 0
    doorway(mid, y, s, if (guildhall) .79f else .48f, if (guildhall) 1.63f else 1.27f)
    if (guildhall) {
      tower(r - .63f, y, n + 1.04f)
      val bannerX = l + .77f
      box(bannerX - .45f, y + 2.34f, s + .08f, bannerX + .45f, y + 2.43f, s + .37f, Material3d.TIMBER, 0x5b4038)
      pennant(bannerX, y + 2.37f, s + .35f, .70f, 1.32f)
      for (x in listOf(mid - 1.22f, mid + 1.22f)) lantern(x, y + 1.34f, s + .28f, .68f)
      frontBeam(l + .14f, y + 1.95f, l + .84f, eave - .12f, s + .035f, .11f, .12f)
      frontBeam(r - .14f, y + 1.95f, r - .84f, eave - .12f, s + .035f, .11f, .12f)
    } else if (house.style == 1) {
      val shadeY = y + 2.13f
      val count = 9
      for (panel in 0 until count) {
        val left = l + (r - l) * panel / count
        val right = l + (r - l) * (panel + 1) / count
        val tint = if (panel % 2 == 0) 0x9b6656 else 0xc5a888
        quad(Material3d.PLASTER, Point3(left, shadeY - .28f, s + .66f),
          Point3(right, shadeY - .28f, s + .66f), Point3(right, shadeY, s),
          Point3(left, shadeY, s), tint)
        quad(Material3d.PLASTER, Point3(left, shadeY - .43f, s + .657f),
          Point3(right, shadeY - .43f, s + .657f), Point3(right, shadeY - .28f, s + .66f),
          Point3(left, shadeY - .28f, s + .66f), tint)
      }
      for (x in listOf(l + .08f, r - .08f))
        box(x - .064f, y, s + .12f, x + .064f, shadeY - .14f, s + .28f, Material3d.TIMBER, 0x77513d)
      lantern(mid, y + 1.76f, s + .47f, .62f)
    }
    for (x in floatArrayOf(l + .94f, r - .94f)) {
      window(x, y + .92f, s + .015f)
      if (house.style != 2) windowBox(x, y + .71f, s + .105f)
    }
    for (side in floatArrayOf(-1f, 1f)) {
      val inner = mid + side * .40f
      val outer = mid + side * (r - l) * .32f
      frontBeam(outer, eave + .01f, inner, eave + house.roof * .65f, s + .002f, .10f, .092f)
      frontBeam(inner, y + 1.98f, outer, y + 1.25f, s + .002f, .095f, .087f)
    }
    window(mid, eave + .28f, s + .015f, .65f)
    val chimneyX = if (house.style == 0) mid - .68f else mid + .7f
    box(chimneyX - .21f, eave + .35f, n + .6f, chimneyX + .21f, ridge + .46f, n + 1.04f, Material3d.WALL, 0x998678)
    box(chimneyX - .28f, ridge + .41f, n + .53f, chimneyX + .28f, ridge + .54f, n + 1.11f, Material3d.STONE)
  }

  private fun roofPlane(x0: Float, x1: Float, north: Float, south: Float, y0: Float, y1: Float, color: Int) {
    val left = Point3(x0, y0, south)
    val right = Point3(x1, y1, south)
    val backRight = Point3(x1, y1, north)
    val backLeft = Point3(x0, y0, north)
    // Slate courses run along the eaves. Rotate the UV axes to follow the roof slope.
    quad(Material3d.SLATE, backLeft, left, right, backRight, color, (south - north) * .60f, .98f)
  }

  private fun frontBeam(x0: Float, y0: Float, x1: Float, y1: Float, z: Float, thickness: Float, depth: Float) {
    val length = kotlin.math.hypot(x1 - x0, y1 - y0)
    val dx = -(y1 - y0) / length * thickness / 2f
    val dy = (x1 - x0) / length * thickness / 2f
    val a = Point3(x0 + dx, y0 + dy, z + depth)
    val b = Point3(x0 - dx, y0 - dy, z + depth)
    val c = Point3(x1 - dx, y1 - dy, z + depth)
    val d = Point3(x1 + dx, y1 + dy, z + depth)
    val color = 0x795843
    quad(Material3d.TIMBER, a, b, c, d, color, .5f, length)
    quad(Material3d.TIMBER, d, c, c.copy(z = z), d.copy(z = z), color)
    quad(Material3d.TIMBER, a.copy(z = z), a, d, d.copy(z = z), color, .5f, length)
    quad(Material3d.TIMBER, b, b.copy(z = z), c.copy(z = z), c, color, .5f, length)
  }

  private fun doorway(x: Float, y: Float, z: Float, radius: Float, spring: Float) {
    val sill = y + .13f
    val arch = y + spring
    val front = z + .13f
    box(x - radius, sill, z, x + radius, arch, z + .03f, Material3d.TIMBER, 0x51372b)
    for (sign in intArrayOf(-1, 1)) {
      val pier = x + sign * (radius + .105f)
      box(pier - .116f, sill, z, pier + .116f, arch, front, Material3d.WALL, 0xc1af90)
    }
    for (i in 0 until 12) {
      val a = i * PI / 12
      val b = (i + 1) * PI / 12
      fun point(angle: Double, r: Float, pz: Float) =
        Point3(x + cos(angle).toFloat() * r, arch + sin(angle).toFloat() * r, pz)
      triangle(Material3d.TIMBER, Point3(x, arch, z + .034f),
        point(a, radius, z + .034f), point(b, radius, z + .034f), 0x5d4030)
      quad(Material3d.WALL, point(a, radius, front), point(a, radius + .23f, front),
        point(b, radius + .23f, front), point(b, radius, front), if (i % 3 == 0) 0xcdbc9d else 0xb6a486)
      quad(Material3d.WALL, point(a, radius, z + .04f), point(a, radius, front),
        point(b, radius, front), point(b, radius, z + .04f), 0x857057)
    }
    box(x - radius - .18f, y + .02f, z, x + radius + .18f, sill, z + .26f, Material3d.STONE, 0xbcad93)
    box(x - .032f, sill, z + .036f, x + .032f, arch + radius - .022f, z + .055f, Material3d.TIMBER, 0x413329)
    for (h in listOf(-.18f, .18f)) {
      box(x + h - .039f, y + .85f, z + .054f, x + h + .039f, y + .95f, z + .092f, Material3d.SOLID, 0xdcb779)
    }
  }

  private fun tower(x: Float, y: Float, z: Float) {
    val top = y + 4.48f
    for (i in 0 until 8) {
      val a = i * PI / 4 + PI / 8
      val b = (i + 1) * PI / 4 + PI / 8
      fun ring(angle: Double, h: Float, radius: Float) =
        Point3(x + cos(angle).toFloat() * radius, h, z + sin(angle).toFloat() * radius)
      val r = .62f
      quad(Material3d.WALL, ring(b, y, r), ring(a, y, r), ring(a, top, r), ring(b, top, r), 0xc9b598, .73f, 2.8f)
      quad(Material3d.STONE, ring(b, top - .16f, .71f), ring(a, top - .16f, .71f),
        ring(a, top + .07f, .71f), ring(b, top + .07f, .71f), 0xc5b79c)
      triangle(Material3d.SLATE, ring(b, top, .78f), ring(a, top, .78f), Point3(x, top + 1.46f, z), 0x8b9ab4)
    }
    box(x - .10f, y + 3.79f, z + .571f, x + .10f, y + 4.13f, z + .615f, Material3d.EMISSIVE, 0xc08a51)
  }

  private fun pennant(x: Float, y: Float, z: Float, width: Float, length: Float) {
    val half = width / 2f
    quad(Material3d.SOLID, Point3(x - half, y - length * .79f, z), Point3(x + half, y - length * .79f, z),
      Point3(x + half, y, z), Point3(x - half, y, z), 0x9e4945)
    triangle(Material3d.SOLID, Point3(x - half, y - length * .79f, z), Point3(x, y - length, z + .06f),
      Point3(x + half, y - length * .79f, z), 0xa25244)
    val crest = y - length * .39f
    quad(Material3d.SOLID, Point3(x, crest - .17f, z + .014f), Point3(x + .095f, crest, z + .014f),
      Point3(x, crest + .17f, z + .014f), Point3(x - .095f, crest, z + .014f), 0xe0b278)
    for (side in intArrayOf(-1, 1)) {
      val edge = x + side * (half - .044f)
      box(edge - .015f, y - length * .79f, z, edge + .015f, y - .04f, z + .013f, Material3d.SOLID, 0xdab173)
    }
  }

  private fun lantern(x: Float, y: Float, z: Float, scale: Float = 1f) {
    val width = .16f * scale
    val top = y + .29f * scale
    box(x - width, y, z - width, x + width, top, z + width, Material3d.EMISSIVE, 0xe89c4e)
    for (h in listOf(y - .06f * scale, top)) {
      box(x - width * 1.28f, h, z - width * 1.28f, x + width * 1.28f, h + .065f * scale, z + width * 1.28f,
        Material3d.TIMBER, 0x665146)
    }
    for (dx in floatArrayOf(-width, width)) for (dz in floatArrayOf(-width, width)) {
      box(x + dx - .023f, y, z + dz - .023f, x + dx + .023f, top, z + dz + .023f,
        Material3d.TIMBER, 0x69523c)
    }
  }

  private fun window(x: Float, y: Float, z: Float, scale: Float = 1f) {
    box(x - .36f * scale, y - .065f, z, x + .36f * scale, y + .8f * scale, z + .065f, Material3d.TIMBER, 0x534946)
    box(x - .26f * scale, y, z + .066f, x + .26f * scale, y + .68f * scale, z + .083f, Material3d.EMISSIVE, 0xac784a)
    box(x - .033f, y, z + .084f, x + .033f, y + .69f * scale, z + .112f, Material3d.SOLID, 0x4a463d)
    for (sign in intArrayOf(-1, 1)) {
      val sx = x + sign * .34f * scale
      box(sx - .065f * scale, y - .007f, z + .011f, sx + .065f * scale, y + .69f * scale, z + .12f,
        Material3d.TIMBER, 0x64825f)
    }
    box(x - .26f * scale, y + .31f * scale, z + .084f, x + .26f * scale, y + .36f * scale, z + .112f,
      Material3d.TIMBER, 0x695343)
    box(x - .42f * scale, y - .11f, z, x + .42f * scale, y - .032f, z + .18f, Material3d.STONE)
  }

  private fun windowBox(x: Float, y: Float, z: Float) {
    box(x - .39f, y - .035f, z, x + .39f, y + .17f, z + .22f, Material3d.TIMBER, 0x895d3d)
    for (i in 0..6) {
      val xx = x - .29f + i * .096f
      val height = y + .22f + (i % 3) * .058f
      val zz = z + .10f + (i % 2) * .081f
      crown(xx, y + .17f, zz, .102f, .092f, .091f, 0x708357, Material3d.FOLIAGE)
      crown(xx, height, zz, .047f, .035f, .043f, if (i % 3 == 1) 0xbd6d83 else 0xe6c5a1)
    }
  }

  private fun crown(
    x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, rgb: Int,
    material: Material3d = Material3d.SOLID,
  ) {
    val segments = 10
    val bands = 6
    fun sample(r: Int, c: Int) {
      val theta = c * (2 * PI / segments)
      val phi = PI * r / bands
      val nx = (sin(theta) * sin(phi)).toFloat()
      val ny = cos(phi).toFloat()
      val nz = (cos(theta) * sin(phi)).toFloat()
      vertex(material, Point3(x + nx * rx, y + ny * ry, z + nz * rz),
        Point3(nx / rx, ny / ry, nz / rz), c / segments.toFloat(), r / bands.toFloat(), rgb)
    }
    for (r in 0 until bands) for (c in 0 until segments) {
      if (r < bands - 1) {
        sample(r, c); sample(r + 1, c); sample(r + 1, c + 1)
      }
      if (r > 0) {
        sample(r, c); sample(r + 1, c + 1); sample(r, c + 1)
      }
    }
  }

  private fun promenade() {
    // One continuous plaza, with cut corners reserved for planted beds.
    val landings = listOf(
      Triple(-5.92f, 9.03f, 1.40f), Triple(-2.4f, 7.52f, 5.75f),
      Triple(-.35f, 4.75f, 7.70f), Triple(.40f, 3.66f, 8.00f),
      Triple(1.01f, 2.43f, 7.50f), Triple(3.75f, 1.5f, 1.55f),
    )
    for ((a, b) in landings.zipWithNext()) {
      quad(Material3d.STONE,
        Point3(a.first - a.third, .020f, a.second), Point3(a.first + a.third, .020f, a.second),
        Point3(b.first + b.third, .020f, b.second), Point3(b.first - b.third, .020f, b.second), 0xdecdb4)
    }
    val centerX = -.82f
    val centerZ = 4.88f
    val size = 1.86f
    val y = .024f
    val colors = intArrayOf(0xb5a184, 0x977c65)
    // An inlaid compass marks the center without adding collision or height.
    for (i in 0 until 24) {
      val a = i * PI / 12
      val b = (i + 1) * PI / 12
      fun ring(angle: Double, r: Float) =
        Point3(centerX + sin(angle).toFloat() * r, y, centerZ + cos(angle).toFloat() * r)
      quad(Material3d.STONE, ring(a, size), ring(b, size), ring(b, size - .13f), ring(a, size - .13f), colors[i % 2])
    }
    for (i in 0 until 8) {
      val a = i * PI / 4
      fun ray(along: Float, across: Float) = Point3(centerX + (sin(a) * along + cos(a) * across).toFloat(), y,
        centerZ + (cos(a) * along - sin(a) * across).toFloat())
      quad(Material3d.STONE, ray(.11f, 0f), ray(.39f, -.13f),
        ray(if (i % 2 == 0) 1.43f else .86f, 0f), ray(.39f, .13f), 0x957b69)
    }
  }

  private fun citadel() {
    val base = Courtyard.RISE * 2
    val front = Courtyard.NORTH - .62f
    // The connected upper bank and its gate lie beyond the playable parapet.
    box(-17f, -1.08f, front - 11f, 17f, base, front, Material3d.ROCK, 0x868879, .54f)
    plane(-17f, 17f, front - 11f, front, base + .005f, Material3d.GRASS, 0x8b9167)
    box(-15f, base, front - 1.75f, 15f, base + 2.98f, front, Material3d.WALL, 0xcac1a9, .73f)
    box(-15.14f, base + 2.84f, front - 1.90f, 15.14f, base + 3.08f, front + .14f, Material3d.WALL, 0xd6c8a9)
    for (i in 0..28) {
      val x = -14.84f + i * 1.04f
      box(x, base + 3.04f, front - .59f, x + .48f, base + 3.64f, front + .12f, Material3d.WALL, 0xc7b99d)
      box(x - .045f, base + 3.60f, front - .68f, x + .525f, base + 3.72f, front + .20f, Material3d.WALL, 0xd9c9a7)
    }
    val gate = -1.25f
    box(gate - 2.12f, base, front - 2.21f, gate + 2.12f, base + 3.96f, front + .21f, Material3d.WALL, 0xcabc9d, .69f)
    doorway(gate, base, front + .222f, 1.04f, 1.68f)
    crest(gate, base + 3.40f, front + .25f, .57f)
    for (side in intArrayOf(-1, 1)) {
      val x = gate + side * 2.37f
      royalTower(x, base, front - .22f, 1.02f, 4.36f)
      pennant(x, base + 3.63f, front + .825f, .56f, 1.14f)
    }
    royalTower(-12.3f, base, front - .63f, 1.27f, 3.70f)
    royalTower(10.4f, base, front - .73f, 1.24f, 3.91f)
    royalTower(1.7f, base, front - 5.88f, 1.39f, 6.23f)
    royalTower(-5.4f, base, front - 6.7f, 1.22f, 5.55f)
    for (x in floatArrayOf(-9.1f, 6.9f)) {
      val rear = front - 6.2f
      val top = base + 5.5f
      val eave = top - 1.1f
      val frontZ = rear + 1.60f
      val backZ = rear - 2f
      box(x - 2.45f, base, backZ, x + 2.45f, eave, frontZ, Material3d.WALL, 0xafada0, .70f)
      triangle(Material3d.WALL, Point3(x - 2.45f, eave, frontZ), Point3(x + 2.45f, eave, frontZ),
        Point3(x, top, frontZ), 0xafada0)
      triangle(Material3d.WALL, Point3(x + 2.45f, eave, backZ), Point3(x - 2.45f, eave, backZ),
        Point3(x, top, backZ), 0xafada0)
      roofPlane(x - 2.58f, x, rear - 2.25f, rear + 1.78f, top - 1.1f, top, 0x667f92)
      roofPlane(x, x + 2.58f, rear - 2.25f, rear + 1.78f, top, top - 1.1f, 0x667f92)
    }
  }

  private fun crest(x: Float, y: Float, z: Float, r: Float) {
    quad(Material3d.SOLID, Point3(x - r, y, z), Point3(x, y - r, z),
      Point3(x + r, y, z), Point3(x, y + r, z), 0x495975)
    val h = r * .71f
    quad(Material3d.SOLID, Point3(x - h, y, z + .011f), Point3(x, y - h, z + .011f),
      Point3(x + h, y, z + .011f), Point3(x, y + h, z + .011f), 0xd4ae68)
    val core = h * .56f
    quad(Material3d.SOLID, Point3(x - core, y, z + .017f), Point3(x, y - core, z + .017f),
      Point3(x + core, y, z + .017f), Point3(x, y + core, z + .017f), 0x66798e)
  }

  private fun royalTower(x: Float, y: Float, z: Float, radius: Float, body: Float) {
    val top = y + body
    val count = 12
    val roofColor = 0x75959f
    fun point(angle: Double, r: Float, height: Float) =
      Point3(x + sin(angle).toFloat() * r, height, z + cos(angle).toFloat() * r)
    for (i in 0 until count) {
      val a = (i - .5) * 2 * PI / count
      val b = (i + .5) * 2 * PI / count
      quad(Material3d.WALL, point(a, radius, y), point(b, radius, y), point(b, radius, top), point(a, radius, top),
        0xcac2ae, .65f, body * .69f)
      quad(Material3d.WALL, point(a, radius * 1.10f, top - .23f), point(b, radius * 1.10f, top - .23f),
        point(b, radius * 1.10f, top + .035f), point(a, radius * 1.10f, top + .035f), 0xd3c7ad, .8f, .24f)
      quad(Material3d.SLATE, point(a, radius * 1.22f, top), point(b, radius * 1.22f, top),
        point(b, radius * .59f, top + 1.04f), point(a, radius * .59f, top + 1.04f), roofColor, .53f, .60f)
      triangle(Material3d.SLATE, point(a, radius * .59f, top + 1.04f),
        point(b, radius * .59f, top + 1.04f), Point3(x, top + 2.13f, z), roofColor)
    }
    window(x, top - 1.45f, z + radius * .972f, .59f)
    box(x - .035f, top + 2.02f, z - .035f, x + .035f, top + 2.54f, z + .035f, Material3d.SOLID, 0xd6ae63)
    crown(x, top + 2.14f, z, .11f, .14f, .11f, 0xe7be75)
  }

  private fun railing(left: Float, right: Float, z0: Float, z1: Float, bottom: Float) {
    val length = z1 - z0
    box(left, bottom + .50f, z0, right, bottom + .66f, z1, Material3d.STONE, 0xbeb49f)
    val center = (left + right) / 2f
    for (i in 0..5) {
      val z = z0 + length * i / 5f
      val half = if (i == 0 || i == 5) .17f else .068f
      box(center - half, bottom, z - half, center + half, bottom + .52f, z + half, Material3d.WALL, 0xa69985)
    }
  }

  private fun bridge() {
    val l = Courtyard.BRIDGE_LEFT
    val r = Courtyard.BRIDGE_RIGHT
    val s = Courtyard.SOUTH - .10f
    val end = Courtyard.BRIDGE_END
    box(l, -.32f, s, r, 0f, end, Material3d.WALL, 0xada08c)
    plane(l, r, s, end, .021f, Material3d.STONE, 0xc9bba4)
    railing(l - .045f, l + .21f, s + .13f, end - .20f, 0f)
    railing(r - .21f, r + .045f, s + .13f, end - .20f, 0f)
    for (z in floatArrayOf(s + .4f, end - .44f)) {
      box(l - .1f, -1.38f, z - .25f, r + .1f, -.28f, z + .25f, Material3d.WALL, 0x8c8c79)
    }
    val center = (s + end) * .5f
    for (face in listOf(l - .018f, r + .018f)) {
      for (i in 0 until 12) {
        val a = i * PI / 12
        val b = (i + 1) * PI / 12
        fun point(angle: Double, radius: Float) =
          Point3(face, -1.14f + .56f * sin(angle).toFloat() * radius, center + 1.64f * cos(angle).toFloat() * radius)
        val shade = if (i % 3 == 0) 0xa9a291 else 0x968b74
        if (face > 0.5f * (l + r)) {
          quad(Material3d.WALL, point(a, 1f), point(b, 1f), point(b, 1.16f), point(a, 1.16f), shade)
        } else {
          quad(Material3d.WALL, point(a, 1.16f), point(b, 1.16f), point(b, 1f), point(a, 1f), shade)
        }
      }
    }
    box(l - .75f, -1.50f, end, r + .85f, .031f, end + 4.1f, Material3d.ROCK, 0x959572)
    plane(l - .75f, r + .85f, end, end + 4.1f, .037f, Material3d.GRASS, 0xa0a776)
    box(l, .031f, end - .13f, r, .54f, end + .04f, Material3d.TIMBER, 0x776052)
    box(l - .04f, .54f, end - .16f, r + .04f, .65f, end + .07f, Material3d.WALL, 0xb1a58c)
  }

  private fun wallDetail(stair: Courtyard.Stair, random: Random) {
    for ((left, right) in listOf(Courtyard.LEFT to stair.left, stair.right to Courtyard.RIGHT)) {
      val capY = stair.base + Courtyard.RISE
      for (i in 0..((right - left) / .66f).toInt()) {
        val x = left + i * .66f + .025f
        val depth = .16f + random.nextFloat() * .11f
        val end = minOf(x + .64f, right - .013f)
        if (end - x < .06f) break
        val shade = intArrayOf(0xc3baa5, 0xaa9d86, 0xacaa96)[i % 3]
        box(x, capY + .035f, stair.edge - .39f, end, capY + .235f, stair.edge + depth, Material3d.WALL, shade)
        if (i % 6 == 2 && x > left + 1.45f && end < right - .9f) {
          box(x + .10f, stair.base, stair.edge - .05f, end, capY - .042f, stair.edge + .15f,
            Material3d.WALL, 0xab9d88)
        }
        if (i % 5 == 1 && x > left + .55f && end < right - .65f) {
          repeat(9) { strand ->
            val drop = strand * .081f
            val center = x + .18f + .32f * sin(strand * 1.78f)
            val leaf = .11f + random.nextFloat() * .10f
            val color = intArrayOf(0x566f46, 0x6c8151, 0x8d985e)[strand % 3]
            quad(Material3d.FOLIAGE,
              Point3(center - leaf, capY - drop, stair.edge + depth - drop * .06f),
              Point3(center, capY - drop - leaf, stair.edge + depth + .035f),
              Point3(center + leaf, capY - drop, stair.edge + depth - drop * .06f),
              Point3(center, capY - drop + leaf, stair.edge + depth), color)
          }
        }
      }
    }
  }

  private fun promenadeParapet() {
    val z = Courtyard.SOUTH - .045f
    val lengths = listOf(Courtyard.LEFT + .15f to Courtyard.BRIDGE_LEFT - .19f,
      Courtyard.BRIDGE_RIGHT + .18f to Courtyard.RIGHT - .20f)
    for ((left, right) in lengths) {
      box(left, -.04f, z - .13f, right, .13f, z + .29f, Material3d.WALL, 0xafa289)
      box(left, .65f, z - .16f, right, .78f, z + .34f, Material3d.WALL, 0xcabd9e)
      val segments = ((right - left) / .46f).toInt()
      for (i in 0..segments) {
        val x = left + .14f + (right - left - .28f) * i / segments
        val pier = i == 0 || i == segments || i % 6 == 0
        val half = if (pier) .13f else .055f
        box(x - half, .12f, z - .055f, x + half, .67f, z + .24f, Material3d.WALL, 0xb7ab92)
        if (pier) box(x - .17f, .77f, z - .17f, x + .17f, .87f, z + .35f, Material3d.WALL, 0xd2c1a2)
      }
    }
  }

  private fun terrain() {
    plane(-60f, 60f, -65f, 56f, -1.08f, Material3d.WATER, 0x284f58)
    box(Courtyard.LEFT, -.98f, Courtyard.NORTH, Courtyard.RIGHT, 0f, Courtyard.SOUTH, Material3d.WALL, 0x998e78, .7f)
    plane(Courtyard.LEFT, Courtyard.RIGHT, Courtyard.STONE_EDGE, Courtyard.SOUTH, .003f, Material3d.GRASS, 0xaaa77d)
    box(Courtyard.LEFT, 0f, Courtyard.NORTH, Courtyard.RIGHT, Courtyard.RISE, Courtyard.STONE_EDGE, Material3d.WALL, 0xb4a791, .65f)
    plane(Courtyard.LEFT, Courtyard.RIGHT, Courtyard.UPPER_EDGE, Courtyard.STONE_EDGE, Courtyard.RISE + .006f, Material3d.STONE, 0xdad0ba)
    box(Courtyard.LEFT, Courtyard.RISE, Courtyard.NORTH, Courtyard.RIGHT, Courtyard.RISE * 2, Courtyard.UPPER_EDGE, Material3d.WALL, 0xb3a78d, .66f)
    plane(Courtyard.LEFT, Courtyard.RIGHT, Courtyard.NORTH, Courtyard.UPPER_EDGE, Courtyard.RISE * 2 + .007f, Material3d.GRASS, 0xa19e75)
    plane(-4.1f, .7f, Courtyard.NORTH, Courtyard.UPPER_EDGE, Courtyard.RISE * 2 + .017f, Material3d.STONE, 0xbdb79f)
    promenade()
    bridge()
    promenadeParapet()
    citadel()
    for (stair in Courtyard.stairs) {
      for (i in 0 until Courtyard.STEP_COUNT) {
        val z = stair.edge + Courtyard.STEP_DEPTH * i
        val y = stair.base + (Courtyard.STEP_COUNT - i) * Courtyard.STEP_HEIGHT
        val face = z + Courtyard.STEP_DEPTH
        box(stair.left, stair.base - .03f, z, stair.right, y + .018f, face, Material3d.WALL, 0xa69d88)
        box(stair.left, y - .035f, face - .065f, stair.right, y + .020f, face + .027f, Material3d.WALL, 0xb8ac96)
        plane(stair.left, stair.right, z, z + Courtyard.STEP_DEPTH, y + .022f, Material3d.STONE, 0xd8cab0)
      }
      for ((a, b) in listOf(Courtyard.LEFT to stair.left - .07f, stair.right + .07f to Courtyard.RIGHT)) {
        box(a, stair.base + Courtyard.RISE, stair.edge - .29f, b, stair.base + Courtyard.RISE + .20f,
          stair.edge + .04f, Material3d.WALL, 0xc6bca5)
      }
    }
    for ((left, north, right, south) in Courtyard.lowerRailings) {
      railing(left, right, north, south, 0f)
    }
  }

  fun build() {
    terrain()
    Courtyard.houses.forEach(::house)
    val random = Random(1731)
    for ((index, tree) in Courtyard.trees.withIndex()) {
      val (x, z) = tree
      val y = Courtyard.height(x, z)
      box(x - .19f, y, z - .18f, x + .18f, y + 2.66f, z + .19f, Material3d.TIMBER, 0x897457)
      val palette = if (index % 3 == 0) intArrayOf(0xc9c47a, 0xb8bc76, 0xe4d386) else intArrayOf(0x98b079, 0xb0bf81, 0xd2d692)
      repeat(13) { i ->
        val angle = i * 2.39996f
        val t = i / 12f
        val spread = .38f + t * 1.06f
        val px = x + cos(angle) * spread
        val pz = z + sin(angle) * spread
        val py = y + 2.57f + (1f - t) * 1.38f + random.nextFloat() * .38f
        val radius = .72f + random.nextFloat() * .25f
        crown(px, py, pz, radius, radius * .68f, radius * .85f, palette[i % 3], Material3d.FOLIAGE)
        for (j in 0 until 14) {
          val leafAngle = j * 2.39996f
          val ox = cos(leafAngle) * radius * (.75f + random.nextFloat() * .37f)
          val oz = sin(leafAngle) * radius
          val h = py + (random.nextFloat() - .5f) * .92f
          val leaf = .075f + random.nextFloat() * .12f
          triangle(Material3d.FOLIAGE, Point3(px + ox - leaf, h, pz + oz),
            Point3(px + ox + leaf, h - .08f, pz + oz - leaf * .58f),
            Point3(px + ox + leaf * .34f, h + leaf, pz + oz + .062f), palette[(i + j) % 3])
        }
      }
    }
    Courtyard.stairs.forEach { wallDetail(it, random) }
    // Cascading hills fill the view beyond the rear rooftops and frame the stream.
    for (i in 0 until 29) {
      val x = -34f + 2.52f * i
      val z = -20.2f - random.nextFloat() * 8.8f
      val h = .9f + random.nextFloat() * 5.4f
      crown(x, -.7f, z, 4.2f, h, 7.0f, 0x748579, Material3d.ROCK)
      crown(x, h * .58f, z + .27f, 3.5f, h * .41f + .24f, 4.55f, 0x778775, Material3d.FOLIAGE)
    }
    for (i in 0 until 12) {
      val z = -2.1f - i * 1.76f
      val r = 1.04f + 1.5f * random.nextFloat()
      crown(-17f - random.nextFloat() * 2.45f, -1.0f, z, r * 1.45f, r * 1.23f, r * 1.36f,
        0x889079, Material3d.ROCK)
    }
    for (i in 0 until 32) {
      val x = -12.4f + random.nextFloat() * 24.8f
      if (x in (Courtyard.BRIDGE_LEFT - .3f)..(Courtyard.BRIDGE_RIGHT + .3f)) continue
      val z = Courtyard.SOUTH + random.nextFloat() * .84f
      val size = .22f + random.nextFloat() * .63f
      crown(x, -.70f, z, size, size * .82f, size * .70f, 0xb3a487, Material3d.ROCK)
    }
    for ((x, z) in Courtyard.bollards) {
      val y = Courtyard.height(x, z)
      box(x - .25f, y, z - .25f, x + .25f, y + .25f, z + .25f, Material3d.WALL, 0xb5ae94)
      box(x - .13f, y + .25f, z - .13f, x + .13f, y + 1.03f, z + .13f, Material3d.TIMBER, 0x74624f)
      lantern(x, y + 1.02f, z)
    }
    // Small grouped plants break the repeating tile pattern at the stair and cliff edges.
    for ((x, z) in listOf(-9.3f to 2.15f, 9.1f to 2.40f, -9.2f to 7.8f, 5.7f to 7.9f, 9.3f to 6.87f)) {
      repeat(5) { i ->
        val spread = i * .19f
        crown(x + spread, .23f + random.nextFloat() * .16f, z + .18f * sin(i.toFloat()),
          .30f + random.nextFloat() * .19f, .24f, .35f, if (i % 2 == 0) 0xcad691 else 0x93a679, Material3d.FOLIAGE)
      }
    }
    repeat(520) { i ->
      val x = -11.7f + random.nextFloat() * 23.4f
      val z = 3.3f + random.nextFloat() * 5.55f
      if ((x in -8.45f..8.45f && z < 7.5f) || x in -8.45f..3.4f) return@repeat
      val stem = .068f + random.nextFloat() * .27f
      val color = intArrayOf(0x6d794c, 0x5d7045, 0x858450)[i % 3]
      triangle(Material3d.SOLID, Point3(x - .045f, .012f, z), Point3(x + .043f, .012f, z),
        Point3(x + .07f, stem, z - .08f), color)
      triangle(Material3d.SOLID, Point3(x, .012f, z + .047f), Point3(x, .012f, z - .052f),
        Point3(x - .05f, stem * .65f, z - .02f), color)
      if (i % 7 == 0) crown(x + .065f, stem, z - .082f, .052f, .021f, .049f, if (i % 2 == 0) 0xe4c888 else 0xf0dfd5)
    }
  }
}
