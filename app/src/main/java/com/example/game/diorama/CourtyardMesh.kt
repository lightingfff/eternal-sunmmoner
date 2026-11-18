package com.example.game.diorama

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal data class Point3(val x: Float, val y: Float, val z: Float)

internal enum class Material3d(val asset: String? = null) {
  GRASS("art_depth_v1/materials/sanctuary_grass.png"),
  STONE("art_depth_v1/materials/cobble.png"),
  WALL("art_completion_v1/surfaces/limestone.png"),
  TIMBER("art_depth_v1/materials/oak.png"),
  TERRACOTTA("art_depth_v1/materials/ruin.png"),
  SOIL("art_depth_v1/materials/soil.png"),
  FOLIAGE("art_depth_v1/materials/forest_moss.png"),
  WATER, SOLID, HERO
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
    val wallTint = if (house.style == 1) 0xf7edd0 else 0xe2cdae
    val roofTint = when (house.style) { 0 -> 0x618d96; 1 -> 0x995f51; else -> 0x657489 }
    box(l - .1f, y, n - .1f, r + .1f, y + .33f, s + .12f, Material3d.WALL, 0xa19b85)
    box(l, y + .33f, n, r, eave, s, Material3d.WALL, wallTint, .64f)
    triangle(Material3d.WALL, Point3(l, eave, s), Point3(r, eave, s), Point3(mid, ridge, s), wallTint)
    triangle(Material3d.WALL, Point3(r, eave, n), Point3(l, eave, n), Point3(mid, ridge, n), wallTint)
    roofPlane(l - .24f, mid, n - .24f, s + .26f, eave, ridge, roofTint)
    roofPlane(mid, r + .24f, n - .24f, s + .26f, ridge, eave, roofTint)
    box(mid - .07f, ridge - .035f, n - .24f, mid + .07f, ridge + .13f, s + .26f, Material3d.TIMBER, 0x554742)

    val beamTint = 0xa57d59
    for (x in listOf(l + .12f, r - .12f, mid)) {
      val bottom = if (x == mid) y + 1.98f else y + .28f
      box(x - .07f, bottom, s, x + .07f, eave, s + .09f, Material3d.TIMBER, beamTint)
    }
    for (level in floatArrayOf(y + .32f, y + 1.95f, eave - .09f)) {
      box(l, level, s, r, level + .13f, s + .10f, Material3d.TIMBER, beamTint)
    }
    box(mid - .52f, y + .20f, s, mid + .52f, y + 1.88f, s + .08f, Material3d.TIMBER, 0x4a3832)
    box(mid - .66f, y + .03f, s, mid + .66f, y + .20f, s + .21f, Material3d.STONE, 0xbcb099)
    for (x in floatArrayOf(l + .94f, r - .94f)) window(x, y + .92f, s + .015f)
    window(mid, eave + .33f, s + .015f, .67f)
    val chimneyX = if (house.style == 0) mid - .68f else mid + .7f
    box(chimneyX - .21f, eave + .35f, n + .6f, chimneyX + .21f, ridge + .46f, n + 1.04f, Material3d.WALL, 0x998678)
    box(chimneyX - .28f, ridge + .41f, n + .53f, chimneyX + .28f, ridge + .54f, n + 1.11f, Material3d.STONE)
  }

  private fun roofPlane(x0: Float, x1: Float, north: Float, south: Float, y0: Float, y1: Float, color: Int) {
    quad(Material3d.TERRACOTTA, Point3(x0, y0, south), Point3(x1, y1, south),
      Point3(x1, y1, north), Point3(x0, y0, north), color, 2.1f, 3.4f)
    val courses = 10
    // Raised shingle lips modelled along the slope and ridge instead of drawn as screen strokes.
    for (i in 0..courses) {
      val t = i / courses.toFloat()
      val x = x0 + (x1 - x0) * t
      val y = y0 + (y1 - y0) * t
      box(x - .021f, y + .011f, north, x + .023f, y + .039f, south, Material3d.SOLID, color)
    }
  }

  private fun window(x: Float, y: Float, z: Float, scale: Float = 1f) {
    box(x - .36f * scale, y - .065f, z, x + .36f * scale, y + .8f * scale, z + .065f, Material3d.TIMBER, 0x534946)
    box(x - .26f * scale, y, z + .066f, x + .26f * scale, y + .68f * scale, z + .083f, Material3d.SOLID, 0xdcad68)
    box(x - .033f, y, z + .084f, x + .033f, y + .69f * scale, z + .112f, Material3d.SOLID, 0x4a463d)
    box(x - .42f * scale, y - .11f, z, x + .42f * scale, y - .032f, z + .18f, Material3d.STONE)
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

  /** The dirt track wanders from the island rim to line up with the stone stairs. */
  private fun trail() {
    val start = Courtyard.STONE_EDGE
    val end = Courtyard.SOUTH - .38f
    fun middle(t: Float) = .80f * sin(t * PI.toFloat()) - .72f * t * t
    fun halfWidth(t: Float) = 1.55f - .33f * t + .12f * sin(t * 11f)
    for (i in 0 until 25) {
      val a = i / 25f
      val b = (i + 1) / 25f
      val za = start + (end - start) * a
      val zb = start + (end - start) * b
      val x0 = middle(a); val x1 = middle(b)
      quad(Material3d.SOIL, Point3(x1 - halfWidth(b), .012f, zb),
        Point3(x1 + halfWidth(b), .012f, zb), Point3(x0 + halfWidth(a), .012f, za),
        Point3(x0 - halfWidth(a), .012f, za), 0xc3a883)
    }
  }

  fun build() {
    // A surrounding water plane and the exposed retaining walls establish an actual drop.
    plane(-48f, 48f, -46f, 48f, -.95f, Material3d.WATER, 0x29484b)
    box(Courtyard.LEFT, -.78f, Courtyard.NORTH, Courtyard.RIGHT, 0f, Courtyard.SOUTH, Material3d.WALL, 0x6e6d57, .65f)
    plane(Courtyard.LEFT, Courtyard.RIGHT, Courtyard.STONE_EDGE, Courtyard.SOUTH, .002f, Material3d.GRASS, 0xb7b68a)
    box(Courtyard.LEFT, 0f, Courtyard.NORTH, Courtyard.RIGHT, Courtyard.RISE, Courtyard.STONE_EDGE, Material3d.WALL, 0x948974, .70f)
    plane(Courtyard.LEFT, Courtyard.RIGHT, Courtyard.UPPER_EDGE, Courtyard.STONE_EDGE, Courtyard.RISE + .006f, Material3d.STONE, 0xdcd2b3)
    box(Courtyard.LEFT, Courtyard.RISE, Courtyard.NORTH, Courtyard.RIGHT, Courtyard.RISE * 2, Courtyard.UPPER_EDGE, Material3d.WALL, 0x958a74, .70f)
    plane(Courtyard.LEFT, Courtyard.RIGHT, Courtyard.NORTH, Courtyard.UPPER_EDGE, Courtyard.RISE * 2 + .007f, Material3d.GRASS, 0xa7b479)
    trail()
    plane(-2.3f, 2.3f, Courtyard.NORTH, Courtyard.UPPER_EDGE, Courtyard.RISE * 2 + .016f, Material3d.STONE, 0xb4ae98)
    for ((edge, base) in listOf(Courtyard.STONE_EDGE to 0f, Courtyard.UPPER_EDGE to Courtyard.RISE)) {
      for (i in 0 until Courtyard.STEP_COUNT) {
        val z = edge + Courtyard.STEP_DEPTH * i
        val y = base + (Courtyard.STEP_COUNT - i) * Courtyard.STEP_HEIGHT
        box(-Courtyard.STAIR_HALF, base - .03f, z, Courtyard.STAIR_HALF, y + .018f,
          z + Courtyard.STEP_DEPTH, Material3d.WALL, 0xb0a48c)
        plane(-Courtyard.STAIR_HALF, Courtyard.STAIR_HALF, z, z + Courtyard.STEP_DEPTH,
          y + .022f, Material3d.STONE, 0xd6cdb4)
      }
      for ((l, r) in listOf(Courtyard.LEFT to -2.1f, 2.1f to Courtyard.RIGHT)) {
        box(l, base + Courtyard.RISE, edge - .27f, r, base + Courtyard.RISE + .24f, edge + .04f, Material3d.WALL, 0xc2bba0)
      }
    }
    Courtyard.houses.forEach(::house)
    val random = Random(1731)
    for ((index, tree) in Courtyard.trees.withIndex()) {
      val (x, z) = tree
      val y = Courtyard.height(x, z)
      box(x - .19f, y, z - .18f, x + .18f, y + 2.66f, z + .19f, Material3d.TIMBER, 0x897457)
      val palette = if (index % 3 == 0) intArrayOf(0xc9c47a, 0xb8bc76, 0xe4d386) else intArrayOf(0x98b079, 0xb0bf81, 0xd2d692)
      repeat(19) { i ->
        val offsetX = (random.nextFloat() - .5f) * 2.5f
        val offsetZ = (random.nextFloat() - .5f) * 2.2f
        val rx = .46f + random.nextFloat() * 1f
        crown(x + offsetX, y + 2.5f + random.nextFloat() * 1.32f, z + offsetZ, rx, rx * .68f, rx * .88f, palette[i % 3], Material3d.FOLIAGE)
      }
    }
    for ((x, z) in Courtyard.bollards) {
      val y = Courtyard.height(x, z)
      box(x - .25f, y, z - .25f, x + .25f, y + .25f, z + .25f, Material3d.WALL, 0xb5ae94)
      box(x - .13f, y + .25f, z - .13f, x + .13f, y + 1.03f, z + .13f, Material3d.TIMBER, 0x74624f)
      box(x - .16f, y + 1.02f, z - .16f, x + .16f, y + 1.32f, z + .16f, Material3d.SOLID, 0xe5b65b)
      box(x - .22f, y + 1.32f, z - .22f, x + .22f, y + 1.43f, z + .22f, Material3d.TIMBER, 0x776155)
    }
    // Small grouped plants break the repeating tile pattern at the stair and cliff edges.
    for ((x, z) in listOf(-3.2f to 2.1f, 2.7f to 2.05f, -4.6f to 7.8f, 4.8f to 7.6f, 7.5f to 7.7f)) {
      repeat(5) { i ->
        val spread = i * .19f
        crown(x + spread, .23f + random.nextFloat() * .16f, z + .18f * sin(i.toFloat()),
          .30f + random.nextFloat() * .19f, .24f, .35f, if (i % 2 == 0) 0xcad691 else 0x93a679, Material3d.FOLIAGE)
      }
    }
    repeat(520) { i ->
      val x = -11.7f + random.nextFloat() * 23.4f
      val z = 3.3f + random.nextFloat() * 5.55f
      if (kotlin.math.abs(x) < 2.3f) return@repeat
      val stem = .1f + random.nextFloat() * .31f
      val color = intArrayOf(0x728844, 0x677a3d, 0x9b9e51)[i % 3]
      triangle(Material3d.SOLID, Point3(x - .08f, .012f, z), Point3(x + .08f, .012f, z),
        Point3(x + .07f, stem, z - .08f), color)
      triangle(Material3d.SOLID, Point3(x, .012f, z + .09f), Point3(x, .012f, z - .09f),
        Point3(x - .05f, stem * .65f, z - .02f), color)
      if (i % 7 == 0) crown(x + .065f, stem, z - .082f, .052f, .021f, .049f, if (i % 2 == 0) 0xe4c888 else 0xf0dfd5)
    }
  }
}
