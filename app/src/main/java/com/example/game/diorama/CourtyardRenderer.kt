package com.example.game.diorama

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min

/** GPU-only rendering: the world and the four-facing pixel sprite share a hardware depth buffer. */
internal class CourtyardRenderer(
  private val context: Context,
  private val onReady: () -> Unit,
  private val onFailure: () -> Unit,
  private val preferHalfFloat: Boolean = true,
) : GLSurfaceView.Renderer {
  private data class Batch(val buffer: Int, val count: Int, val material: Material3d)
  private data class Targets(val framebuffer: Int, val color: Int, val depth: Int, val width: Int, val height: Int)

  private class Program(vertex: String, fragment: String) {
    private val locations = HashMap<String, Int>()
    val id: Int

    init {
      val vs = shader(GL_VERTEX_SHADER, vertex)
      val fs = shader(GL_FRAGMENT_SHADER, fragment)
      id = glCreateProgram()
      glAttachShader(id, vs)
      glAttachShader(id, fs)
      glLinkProgram(id)
      glDeleteShader(vs)
      glDeleteShader(fs)
      val status = IntArray(1)
      glGetProgramiv(id, GL_LINK_STATUS, status, 0)
      check(status[0] == GL_TRUE) { glGetProgramInfoLog(id) }
    }

    fun use() = glUseProgram(id)
    private fun location(name: String) = locations.getOrPut(name) { glGetUniformLocation(id, name) }
    fun matrix(name: String, data: FloatArray) = glUniformMatrix4fv(location(name), 1, false, data, 0)
    fun vector(name: String, x: Float, y: Float, z: Float) = glUniform3f(location(name), x, y, z)
    fun vector(name: String, x: Float, y: Float) = glUniform2f(location(name), x, y)
    fun uniform(name: String, value: Int) = glUniform1i(location(name), value)
    fun uniform(name: String, value: Float) = glUniform1f(location(name), value)

    private fun shader(type: Int, source: String): Int {
      val shader = glCreateShader(type)
      glShaderSource(shader, source)
      glCompileShader(shader)
      val status = IntArray(1)
      glGetShaderiv(shader, GL_COMPILE_STATUS, status, 0)
      check(status[0] == GL_TRUE) { glGetShaderInfoLog(shader) }
      return shader
    }
  }

  private lateinit var world: Program
  private lateinit var shadow: Program
  private lateinit var lens: Program
  private lateinit var bloom: Program
  private lateinit var meshes: List<Batch>
  private lateinit var shadowTargets: Targets
  private var scene: Targets? = null
  private var glow: Targets? = null
  private var glowScratch: Targets? = null
  private var halfFloatSupported = false
  private var packedRadiance = 1
  private val lamps = Courtyard.bollards.take(4).map { p ->
    val y = Courtyard.height(p.x, p.z) + 1.16f
    floatArrayOf(p.x, y, p.z)
  }
  private var spriteBuffer = 0
  private val textures = IntArray(Material3d.entries.size)
  private val sun = FloatArray(16)
  private val camera = CourtyardCamera()
  private val navigator by lazy(LazyThreadSafetyMode.NONE) { CourtyardNavigator() }
  private val sprite = floats(6 * STRIDE)
  private var viewportWidth = 0
  private var viewportHeight = 0
  private var failed = false
  private var ready = false
  private var hero = Courtyard.start
  private var facingRight = true
  private var facingAway = true
  private var route: List<GroundPoint> = emptyList()
  private var waypoint = 0
  private var lastFrame = 0L
  private var elapsed = 0f
  private var movingTime = 0f
  private var zoom = 1f
  private var wantedZoom = 1f
  private var tracked = Courtyard.start

  fun chooseDestination(screenX: Float, screenY: Float) {
    if (!ready || failed) return
    val destination = camera.groundAt(screenX, screenY) ?: return
    val next = navigator.route(hero, destination)
    if (next.isEmpty()) return
    route = next
    waypoint = 0
  }

  fun zoomBy(delta: Float) {
    wantedZoom = (wantedZoom + delta).coerceIn(.82f, 1.25f)
  }

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) = guard {
    scene = null
    glow = null
    glowScratch = null
    val extensions = glGetString(GL_EXTENSIONS).orEmpty().split(' ')
    halfFloatSupported = preferHalfFloat && (
      "GL_EXT_color_buffer_half_float" in extensions || "GL_EXT_color_buffer_float" in extensions
    )
    ready = false
    lastFrame = 0L
    world = Program(CourtyardShaders.worldVertex, CourtyardShaders.worldFragment)
    shadow = Program(CourtyardShaders.shadowVertex, CourtyardShaders.shadowFragment)
    lens = Program(CourtyardShaders.lensVertex, CourtyardShaders.lensFragment)
    bloom = Program(CourtyardShaders.lensVertex, CourtyardShaders.bloomFragment)
    textures[Material3d.SOLID.ordinal] = whiteTexture()
    for (material in Material3d.entries) {
      val asset = material.asset ?: continue
      textures[material.ordinal] = imageTexture(asset, atlas = false)
    }
    textures[Material3d.HERO.ordinal] = imageTexture("character_upgrade_v1/kaelen/kaelen.png", atlas = true)
    val geometry = CourtyardMesh().apply { build() }
    meshes = geometry.buffers().map { (material, data) ->
      val buffer = buffer()
      glBindBuffer(GL_ARRAY_BUFFER, buffer)
      val vertices = floats(data.size)
      vertices.put(data).rewind()
      glBufferData(GL_ARRAY_BUFFER, data.size * FLOAT_BYTES, vertices, GL_STATIC_DRAW)
      Batch(buffer, data.size / STRIDE, material)
    }
    spriteBuffer = buffer()
    glBindBuffer(GL_ARRAY_BUFFER, spriteBuffer)
    glBufferData(GL_ARRAY_BUFFER, 6 * STRIDE * FLOAT_BYTES, null, GL_DYNAMIC_DRAW)

    val light = FloatArray(16)
    val orthographic = FloatArray(16)
    Matrix.setLookAtM(light, 0, -24f, 16f, 12f, 0f, 0f, -3f, 0f, 1f, 0f)
    Matrix.orthoM(orthographic, 0, -20f, 20f, -19f, 19f, 2f, 67f)
    Matrix.multiplyMM(sun, 0, orthographic, 0, light, 0)
    shadowTargets = checkNotNull(targets(1024, 1024, colorFormat = null))
    renderShadowMap()
    Log.i(TAG, "OpenGL " + glGetString(GL_VERSION) + " · " + meshes.sumOf { it.count / 3 } + " scene triangles")
    checkGl("initialization")
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) = guard {
    viewportWidth = width
    viewportHeight = height
    if (width <= 0 || height <= 0) return@guard
    release(scene)
    release(glow)
    release(glowScratch)
    scene = null
    glow = null
    glowScratch = null
    val scale = min(1f, 960f / maxOf(width, height))
    val w = (width * scale).toInt().coerceAtLeast(1)
    val h = (height * scale).toInt().coerceAtLeast(1)
    if (halfFloatSupported && setupFrame(w, h, GL_RGBA16F)) {
      packedRadiance = 0
    } else {
      check(setupFrame(w, h, GL_RGBA8)) { "Unable to allocate courtyard color and depth buffers" }
      packedRadiance = 1
    }
    Log.i(TAG, "Lighting storage " + if (packedRadiance == 0) "RGBA16F" else "companded RGBA8")
    camera.frame(tracked, width, height, zoom)
  }

  override fun onDrawFrame(gl: GL10?) = guard {
    val frame = scene ?: return@guard
    val now = SystemClock.uptimeMillis()
    val dt = if (lastFrame == 0L) 0f else ((now - lastFrame) * .001f).coerceIn(0f, .05f)
    lastFrame = now
    elapsed = (elapsed + dt) % 600f
    advance(dt)
    camera.frame(tracked, viewportWidth, viewportHeight, zoom)
    val highlights = glow ?: return@guard
    val scratch = glowScratch ?: return@guard
    renderWorld(frame)
    renderBloom(frame, highlights, scratch)
    renderLens(frame, highlights)
    if (!ready) {
      checkGl("first frame")
      ready = true
      onReady()
    }
  }

  private inline fun guard(block: () -> Unit) {
    if (failed) return
    try {
      block()
    } catch (error: RuntimeException) {
      failed = true
      Log.e(TAG, "Unable to draw the 3D courtyard", error)
      onFailure()
    }
  }

  private fun advance(dt: Float) {
    var left = dt * 3.25f
    if (waypoint < route.size) movingTime += dt else movingTime = 0f
    while (left > .0001f && waypoint < route.size) {
      val next = route[waypoint]
      val distance = hero.distance(next)
      if (distance < .01f) {
        hero = next
        waypoint++
        continue
      }
      val dx = next.x - hero.x
      val dz = next.z - hero.z
      if (kotlin.math.abs(dx) > .02f) facingRight = dx > 0f
      if (kotlin.math.abs(dz) > .02f) facingAway = dz < 0f
      val step = min(1f, left / distance)
      hero = GroundPoint(hero.x + dx * step, hero.z + dz * step)
      left -= distance * step
      if (step >= 1f) waypoint++
    }
    val catchUp = (dt * 4.5f).coerceAtMost(1f)
    tracked = GroundPoint(tracked.x + (hero.x - tracked.x) * catchUp, tracked.z + (hero.z - tracked.z) * catchUp)
    zoom += (wantedZoom - zoom) * catchUp
  }

  private fun renderShadowMap() {
    glBindFramebuffer(GL_FRAMEBUFFER, shadowTargets.framebuffer)
    glViewport(0, 0, shadowTargets.width, shadowTargets.height)
    glEnable(GL_DEPTH_TEST)
    glDepthMask(true)
    glClear(GL_DEPTH_BUFFER_BIT)
    shadow.use()
    shadow.matrix("sunMatrix", sun)
    for (mesh in meshes) {
      if (mesh.material == Material3d.WATER) continue
      glBindBuffer(GL_ARRAY_BUFFER, mesh.buffer)
      attribute(0, 3, 0)
      glDrawArrays(GL_TRIANGLES, 0, mesh.count)
    }
    glDisableVertexAttribArray(0)
    glBindFramebuffer(GL_FRAMEBUFFER, 0)
  }

  private fun renderWorld(frame: Targets) {
    glBindFramebuffer(GL_FRAMEBUFFER, frame.framebuffer)
    glViewport(0, 0, frame.width, frame.height)
    glEnable(GL_DEPTH_TEST)
    glDepthMask(true)
    glClearColor(0f, 0f, 0f, 1f)
    glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    world.use()
    world.matrix("viewProjection", camera.viewProjection)
    world.matrix("sunMatrix", sun)
    world.vector("camera", camera.eye[0], camera.eye[1], camera.eye[2])
    world.vector("sunDirection", -.738f, .492f, .461f)
    world.vector("actor", hero.x, Courtyard.height(hero.x, hero.z), hero.z)
    world.uniform("timer", elapsed)
    world.uniform("packedRadiance", packedRadiance)
    for ((i, lamp) in lamps.withIndex()) world.vector("lamps[$i]", lamp[0], lamp[1], lamp[2])
    world.uniform("material", 0)
    world.uniform("sunDepth", 1)
    bindTexture(1, shadowTargets.depth)
    for (mesh in meshes) {
      world.uniform("kind", when (mesh.material) {
        Material3d.GRASS, Material3d.STONE -> 1
        Material3d.EMISSIVE -> 4
        Material3d.WATER -> 2
        else -> 0
      })
      val texture = textures[mesh.material.ordinal].takeIf { it != 0 } ?: textures[Material3d.SOLID.ordinal]
      bindTexture(0, texture)
      glBindBuffer(GL_ARRAY_BUFFER, mesh.buffer)
      worldAttributes()
      glDrawArrays(GL_TRIANGLES, 0, mesh.count)
    }
    drawHero()
    for (i in 0..3) glDisableVertexAttribArray(i)
  }

  private fun drawHero() {
    val cell = if (waypoint < route.size) 2 + (movingTime * 9f).toInt() % 4 else (elapsed * 1.5f).toInt() % 2
    val row = if (facingAway) 1 else 0
    val u0 = (cell * 128f + .5f) / 1536f
    val u1 = (cell * 128f + 127.5f) / 1536f
    val v0 = (row * 112f + .5f) / 224f
    val v1 = (row * 112f + 111.5f) / 224f
    val foot = Courtyard.height(hero.x, hero.z) + .022f
    val low = foot - .12f
    val high = foot + 1.55f
    val halfWidth = .93f
    val rx = camera.view[0] * halfWidth
    val rz = camera.view[8] * halfWidth
    val left = if (facingRight) u0 else u1
    val right = if (facingRight) u1 else u0
    sprite.clear()
    writeSprite(-rx, low, -rz, left, v1)
    writeSprite(rx, low, rz, right, v1)
    writeSprite(rx, high, rz, right, v0)
    writeSprite(-rx, low, -rz, left, v1)
    writeSprite(rx, high, rz, right, v0)
    writeSprite(-rx, high, -rz, left, v0)
    sprite.flip()
    glBindBuffer(GL_ARRAY_BUFFER, spriteBuffer)
    glBufferSubData(GL_ARRAY_BUFFER, 0, 6 * STRIDE * FLOAT_BYTES, sprite)
    worldAttributes()
    bindTexture(0, textures[Material3d.HERO.ordinal])
    world.uniform("kind", 3)
    glDrawArrays(GL_TRIANGLES, 0, 6)
  }

  private fun writeSprite(dx: Float, y: Float, dz: Float, u: Float, v: Float) {
    sprite.put(hero.x + dx); sprite.put(y); sprite.put(hero.z + dz)
    sprite.put(0f); sprite.put(0f); sprite.put(1f)
    sprite.put(u); sprite.put(v)
    sprite.put(1f); sprite.put(1f); sprite.put(1f)
  }

  private fun renderBloom(frame: Targets, highlights: Targets, scratch: Targets) {
    glDisable(GL_DEPTH_TEST)
    glBindBuffer(GL_ARRAY_BUFFER, 0)
    bloom.use()
    bloom.uniform("rendered", 0)
    bloom.uniform("packedRadiance", packedRadiance)
    bloom.uniform("extract", 1)
    bloomPass(frame, highlights, 1f / frame.width, 1f / frame.height)
    bloom.uniform("extract", 0)
    bloomPass(highlights, scratch, 1f / highlights.width, 0f)
    bloomPass(scratch, highlights, 0f, 1f / scratch.height)
  }

  private fun bloomPass(from: Targets, to: Targets, x: Float, y: Float) {
    glBindFramebuffer(GL_FRAMEBUFFER, to.framebuffer)
    glViewport(0, 0, to.width, to.height)
    bindTexture(0, from.color)
    bloom.vector("sampleStep", x, y)
    glDrawArrays(GL_TRIANGLES, 0, 3)
  }

  private fun renderLens(frame: Targets, highlights: Targets) {
    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    glViewport(0, 0, viewportWidth, viewportHeight)
    glDisable(GL_DEPTH_TEST)
    glBindBuffer(GL_ARRAY_BUFFER, 0)
    lens.use()
    bindTexture(0, frame.color)
    bindTexture(1, frame.depth)
    bindTexture(2, highlights.color)
    bindTexture(3, shadowTargets.depth)
    lens.uniform("fogSunDepth", 3)
    lens.matrix("sunMatrix", sun)
    lens.matrix("inverseViewProjection", camera.inverse)
    lens.vector("camera", camera.eye[0], camera.eye[1], camera.eye[2])
    lens.uniform("rendered", 0)
    lens.uniform("depth", 1)
    lens.uniform("glow", 2)
    lens.uniform("packedRadiance", packedRadiance)
    lens.vector("planes", CourtyardCamera.NEAR, CourtyardCamera.FAR)
    lens.vector("pixelStep", 1f / frame.width, 1f / frame.height)
    lens.uniform("focus", camera.depthAt(hero.x, Courtyard.height(hero.x, hero.z) + .7f, hero.z))
    glDrawArrays(GL_TRIANGLES, 0, 3)
  }

  private fun worldAttributes() {
    attribute(0, 3, 0); attribute(1, 3, 3); attribute(2, 2, 6); attribute(3, 3, 8)
  }

  private fun attribute(location: Int, components: Int, offset: Int) {
    glEnableVertexAttribArray(location)
    glVertexAttribPointer(location, components, GL_FLOAT, false, STRIDE * FLOAT_BYTES, offset * FLOAT_BYTES)
  }

  private fun bindTexture(slot: Int, name: Int) {
    glActiveTexture(GL_TEXTURE0 + slot)
    glBindTexture(GL_TEXTURE_2D, name)
  }

  private fun imageTexture(path: String, atlas: Boolean): Int {
    val bmp = context.assets.open(path).use {
      requireNotNull(BitmapFactory.decodeStream(it)) { "Unreadable texture $path" }
    }
    return try {
      if (atlas) check(bmp.width == 1536 && bmp.height == 224) { "Unexpected Kaelen atlas dimensions" }
      val name = texture(if (atlas) GL_NEAREST else GL_NEAREST_MIPMAP_LINEAR, GL_NEAREST, if (atlas) GL_CLAMP_TO_EDGE else GL_REPEAT)
      GLUtils.texImage2D(GL_TEXTURE_2D, 0, bmp, 0)
      if (!atlas) glGenerateMipmap(GL_TEXTURE_2D)
      name
    } finally {
      bmp.recycle()
    }
  }

  private fun whiteTexture(): Int {
    val white = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    white.eraseColor(android.graphics.Color.WHITE)
    return try {
      texture(GL_NEAREST, GL_NEAREST).also { GLUtils.texImage2D(GL_TEXTURE_2D, 0, white, 0) }
    } finally {
      white.recycle()
    }
  }

  private fun texture(min: Int, mag: Int, wrap: Int = GL_CLAMP_TO_EDGE): Int {
    val name = IntArray(1)
    glGenTextures(1, name, 0)
    glBindTexture(GL_TEXTURE_2D, name[0])
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, min)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, mag)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap)
    return name[0]
  }

  private fun buffer(): Int = IntArray(1).also { glGenBuffers(1, it, 0) }[0]

  private fun setupFrame(width: Int, height: Int, format: Int): Boolean {
    val frame = targets(width, height, format) ?: return false
    val w = (width / 4).coerceAtLeast(1)
    val h = (height / 4).coerceAtLeast(1)
    val highlights = targets(w, h, format, depthNeeded = false)
    val scratch = targets(w, h, format, depthNeeded = false)
    if (highlights == null || scratch == null) {
      release(frame)
      release(highlights)
      release(scratch)
      return false
    }
    scene = frame
    glow = highlights
    glowScratch = scratch
    return true
  }

  private fun targets(width: Int, height: Int, colorFormat: Int?, depthNeeded: Boolean = true): Targets? {
    val id = IntArray(1)
    glGenFramebuffers(1, id, 0)
    glBindFramebuffer(GL_FRAMEBUFFER, id[0])
    val depth = if (depthNeeded) {
      val image = texture(GL_NEAREST, GL_NEAREST)
      glTexStorage2D(GL_TEXTURE_2D, 1, GL_DEPTH_COMPONENT24, width, height)
      glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, image, 0)
      image
    } else 0
    val color = if (colorFormat != null) {
      val filter = if (depthNeeded) GL_NEAREST else GL_LINEAR
      val image = texture(filter, filter)
      glTexStorage2D(GL_TEXTURE_2D, 1, colorFormat, width, height)
      glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, image, 0)
      image
    } else {
      glDrawBuffers(1, intArrayOf(GL_NONE), 0)
      glReadBuffer(GL_NONE)
      0
    }
    val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    val target = Targets(id[0], color, depth, width, height)
    if (status != GL_FRAMEBUFFER_COMPLETE) {
      release(target)
      Log.w(TAG, "3D framebuffer status 0x${status.toString(16)} for format $colorFormat")
      return null
    }
    return target
  }

  private fun release(target: Targets?) {
    if (target == null) return
    glDeleteFramebuffers(1, intArrayOf(target.framebuffer), 0)
    glDeleteTextures(2, intArrayOf(target.color, target.depth), 0)
  }

  private fun checkGl(phase: String) {
    val error = glGetError()
    check(error == GL_NO_ERROR) { "OpenGL error 0x${error.toString(16)} during $phase" }
  }

  private companion object {
    const val TAG = "SanctuaryCourtyard3D"
    const val STRIDE = 11
    const val FLOAT_BYTES = 4
    fun floats(count: Int): FloatBuffer =
      ByteBuffer.allocateDirect(count * FLOAT_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer()
  }
}
