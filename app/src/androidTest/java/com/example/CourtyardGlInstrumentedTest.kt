package com.example

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES30
import android.opengl.Matrix
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
      var ready = false
      var failed = false
      val renderer = CourtyardRenderer(context, onReady = { ready = true }, onFailure = { failed = true })
      renderer.onSurfaceCreated(null, null)
      renderer.onSurfaceChanged(null, width, height)
      renderer.onDrawFrame(null)
      assertFalse("Production renderer must compile and draw on OpenGL ES 3", failed)
      assertTrue(ready)

      val camera = CourtyardCamera()
      camera.frame(Courtyard.start, width, height, 1f)
      val landing = GroundPoint(.35f, .68f)
      val clip = FloatArray(4)
      Matrix.multiplyMV(clip, 0, camera.viewProjection, 0,
        floatArrayOf(landing.x, Courtyard.height(landing.x, landing.z), landing.z, 1f), 0)
      val x = (clip[0] / clip[3] + 1f) * width * .5f
      val y = (1f - clip[1] / clip[3]) * height * .5f
      val picked = requireNotNull(camera.groundAt(x, y))
      assertTrue("Screen tap must resolve to the upper surface it projects", picked.distance(landing) < .20f)

      val bytes = ByteBuffer.allocateDirect(width * height * 4)
      GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, bytes)
      val pixels = IntArray(width * height)
      for (y in 0 until height) for (x in 0 until width) {
        val i = ((height - 1 - y) * width + x) * 4
        pixels[y * width + x] = 0xff000000.toInt() or
          ((bytes[i].toInt() and 255) shl 16) or ((bytes[i + 1].toInt() and 255) shl 8) or
          (bytes[i + 2].toInt() and 255)
      }
      assertTrue("Visible geometry and lighting must cover the frame", pixels.toSet().size > 512)
      val image = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
      try {
        File(context.filesDir, "sanctuary-3d-egl.png").outputStream().use {
          assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
      } finally {
        image.recycle()
      }
    } finally {
      EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
      EGL14.eglDestroySurface(display, buffer)
      EGL14.eglDestroyContext(display, session)
      EGL14.eglTerminate(display)
    }
  }
}
