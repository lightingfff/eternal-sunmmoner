package com.example

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.game.diorama.Courtyard
import com.example.game.diorama.CourtyardCamera
import com.example.game.diorama.CourtyardRenderer
import com.example.game.diorama.GroundPoint
import java.io.File
import java.nio.ByteBuffer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the production renderer in an EGL pbuffer to validate real shaders and depth attachments. */
@RunWith(AndroidJUnit4::class)
class CourtyardGlInstrumentedTest {
  @Test
  fun renders3dSceneWithRealTextureAndDepthAttachments() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    assertTrue("EGL display", display != EGL14.EGL_NO_DISPLAY)
    assertTrue("EGL initialize", EGL14.eglInitialize(display, null, 0, null, 0))
    val config = arrayOfNulls<EGLConfig>(1)
    val available = IntArray(1)
    val wanted = intArrayOf(
      EGL14.EGL_RENDERABLE_TYPE, 0x0040, // EGL_OPENGL_ES3_BIT
      EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
      EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
      EGL14.EGL_DEPTH_SIZE, 16, EGL14.EGL_NONE,
    )
    assertTrue(EGL14.eglChooseConfig(display, wanted, 0, config, 0, 1, available, 0) && available[0] > 0)
    val session = EGL14.eglCreateContext(display, config[0], EGL14.EGL_NO_CONTEXT,
      intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0)
    val width = 960
    val height = 540
    val buffer = EGL14.eglCreatePbufferSurface(display, config[0],
      intArrayOf(EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE), 0)
    try {
      assertTrue(EGL14.eglMakeCurrent(display, buffer, buffer, session))
      val actual = IntArray(1)
      assertTrue(EGL14.eglQuerySurface(display, buffer, EGL14.EGL_WIDTH, actual, 0) && actual[0] == width)
      assertTrue(EGL14.eglQuerySurface(display, buffer, EGL14.EGL_HEIGHT, actual, 0) && actual[0] == height)
      var ready = false
      var failed = false
      val renderer = CourtyardRenderer(context, onReady = { ready = true }, onFailure = { failed = true })
      renderer.onSurfaceCreated(null, null)
      renderer.onSurfaceChanged(null, width, height)
      repeat(2) {
        renderer.onDrawFrame(null)
        GLES30.glFinish()
      }
      assertFalse("Production renderer must compile and draw on OpenGL ES 3", failed)
      assertTrue(ready)

      val camera = CourtyardCamera()
      camera.frame(Courtyard.start, width, height, 1f)
      val landing = GroundPoint(3.75f, 4.30f)
      assertGroundPick(camera, landing, width, height)
      val bridge = GroundPoint(-5.9f, 11.5f)
      camera.frame(bridge, width, height, .82f)
      assertGroundPick(camera, bridge, width, height)

      val fullColor = capture(width, height)
      saveCapture(File(context.filesDir, "sanctuary-3d-egl.png"), fullColor, width, height)
      val paletteSize = fullColor.toSet().size
      val firstColor = fullColor[0].toUInt().toString(16)
      assertTrue("Visible geometry and lighting must cover the frame ($paletteSize colors, first $firstColor)", paletteSize > 512)

      renderer.zoomBy(.25f)
      drawAtGameCadence(renderer, 14)
      saveCapture(File(context.filesDir, "sanctuary-kingdom-wide.png"), capture(width, height), width, height)
      renderer.zoomBy(-.43f)
      drawAtGameCadence(renderer, 18)
      saveCapture(File(context.filesDir, "sanctuary-kingdom-close.png"), capture(width, height), width, height)
      val sample = ByteBuffer.allocateDirect(4)
      val frameTimes = DoubleArray(24) {
        val start = System.nanoTime()
        renderer.onDrawFrame(null)
        // Read a pixel to wait for completion, including on virtualized GL drivers.
        GLES30.glReadPixels(width / 2, height / 2, 1, 1, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, sample)
        (System.nanoTime() - start) / 1_000_000.0
      }.sorted()
      val device = GLES30.glGetString(GLES30.GL_RENDERER)
      File(context.filesDir, "sanctuary-kingdom-timing.txt").writeText(
        "pbuffer ${width}x${height}, 24 warmed draws with one-pixel readback, no swap or UI; renderer=$device\n" +
          "median_ms=${(frameTimes[11] + frameTimes[12]) / 2} p95_ms=${frameTimes[22]}\n",
      )
      assertFalse("Repeated drawing and zooming must keep the production renderer active", failed)

      renderer.zoomBy(.18f)
      val market = Courtyard.landmarks[1].destination
      val marketColor = walkAndCapture(renderer, market,
        File(context.filesDir, "sanctuary-harbor.png"), width, height)
      camera.frame(market, width, height, 1f)
      assertGroundPick(camera, market, width, height)
      recordAfternoonWalk(renderer, context.filesDir, width, height)
      val orchard = Courtyard.landmarks[2].destination
      walkAndCapture(renderer, orchard, File(context.filesDir, "sanctuary-orchard.png"), width, height)
      camera.frame(orchard, width, height, 1f)
      assertGroundPick(camera, orchard, width, height)
      walkAndCapture(renderer, GroundPoint(11.30f, 19.35f),
        File(context.filesDir, "sanctuary-orchard-canopy.png"), width, height)

      val backup = CourtyardRenderer(context, onReady = {}, onFailure = { failed = true }, preferHalfFloat = false)
      backup.onSurfaceCreated(null, null)
      backup.onSurfaceChanged(null, width, height)
      repeat(2) {
        backup.onDrawFrame(null)
        GLES30.glFinish()
      }
      assertFalse("RGBA8 fallback must compile and draw even without float targets", failed)
      val compactColor = capture(width, height)
      assertComparableExposure(fullColor, compactColor, "Royal Quarter")
      saveCapture(File(context.filesDir, "sanctuary-3d-packed.png"), compactColor, width, height)
      val marketCompact = walkAndCapture(backup, market,
        File(context.filesDir, "sanctuary-harbor-packed.png"), width, height)
      assertComparableExposure(marketColor, marketCompact, "River Market sun shafts and water")
    } finally {
      EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
      EGL14.eglDestroySurface(display, buffer)
      EGL14.eglDestroyContext(display, session)
      EGL14.eglTerminate(display)
    }
  }

  private fun assertComparableExposure(hdr: IntArray, packed: IntArray, scene: String) {
    val red = hdr.indices.sumOf { i ->
      kotlin.math.abs((hdr[i] shr 16 and 255) - (packed[i] shr 16 and 255))
    }.toDouble() / hdr.size
    assertTrue("$scene must preserve comparable exposure across radiance formats ($red)", red < 4.0)
  }

  private fun recordAfternoonWalk(renderer: CourtyardRenderer, directory: File, width: Int, height: Int) {
    val frames = File(directory, "afternoon-walk").apply { mkdirs() }
    val shade = GroundPoint(-6.36f, 15.58f)
    val awning = GroundPoint(4.60f, 18.60f)
    renderer.walkTo(shade)
    repeat(360) { renderer.advance(1f / 24f) }
    assertTrue("The market walk must start by the tree", renderer.actorPosition.distance(shade) < .36f)
    renderer.walkTo(GroundPoint(-4.65f, 25.45f))
    for (index in 1..30) {
      if (index == 11) renderer.walkTo(awning)
      if (index == 27) renderer.zoomBy(.22f)
      if (index == 29) renderer.zoomBy(-.40f)
      repeat(12) { renderer.advance(1f / 24f) }
      renderer.onDrawFrame(null)
      GLES30.glFinish()
      val image = Bitmap.createBitmap(capture(width, height), width, height, Bitmap.Config.ARGB_8888)
      val reduced = Bitmap.createScaledBitmap(image, 480, 270, false)
      try {
        File(frames, "frame-${index.toString().padStart(2, '0')}.jpg").outputStream().use {
          assertTrue(reduced.compress(Bitmap.CompressFormat.JPEG, 89, it))
        }
      } finally {
        reduced.recycle()
        image.recycle()
      }
    }
    assertTrue("The market walk must reach the awning", renderer.actorPosition.distance(awning) < .36f)
    renderer.zoomBy(.18f)
  }

  private fun walkAndCapture(
    renderer: CourtyardRenderer,
    destination: GroundPoint,
    output: File,
    width: Int,
    height: Int,
  ): IntArray {
    renderer.walkTo(destination)
    repeat(600) { renderer.advance(1f / 24f) }
    assertTrue("The renderer must walk the route to $destination", renderer.actorPosition.distance(destination) < .36f)
    repeat(2) {
      renderer.onDrawFrame(null)
      GLES30.glFinish()
    }
    val image = capture(width, height)
    assertTrue("Destination framebuffer must show the environment", image.toSet().size > 512)
    saveCapture(output, image, width, height)
    return image
  }

  private fun drawAtGameCadence(renderer: CourtyardRenderer, frames: Int) {
    repeat(frames) {
      SystemClock.sleep(42)
      renderer.onDrawFrame(null)
      GLES30.glFinish()
    }
  }

  private fun assertGroundPick(camera: CourtyardCamera, target: GroundPoint, width: Int, height: Int) {
    val clip = FloatArray(4)
    Matrix.multiplyMV(clip, 0, camera.viewProjection, 0,
      floatArrayOf(target.x, Courtyard.height(target.x, target.z), target.z, 1f), 0)
    val x = (clip[0] / clip[3] + 1f) * width * .5f
    val y = (1f - clip[1] / clip[3]) * height * .5f
    val picked = requireNotNull(camera.groundAt(x, y)) { "No walkable surface projected at $target" }
    assertTrue("Screen tap must resolve to $target, but hit $picked", picked.distance(target) < .20f)
  }

  private fun capture(width: Int, height: Int): IntArray {
    val bytes = ByteBuffer.allocateDirect(width * height * 4)
    GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, bytes)
    assertTrue("Framebuffer readback", GLES30.glGetError() == GLES30.GL_NO_ERROR)
    return IntArray(width * height) { index ->
      val i = ((height - 1 - index / width) * width + index % width) * 4
      0xff000000.toInt() or ((bytes[i].toInt() and 255) shl 16) or
        ((bytes[i + 1].toInt() and 255) shl 8) or (bytes[i + 2].toInt() and 255)
    }
  }

  private fun saveCapture(file: File, pixels: IntArray, width: Int, height: Int) {
    val image = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    try {
      file.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    } finally {
      image.recycle()
    }
  }
}
