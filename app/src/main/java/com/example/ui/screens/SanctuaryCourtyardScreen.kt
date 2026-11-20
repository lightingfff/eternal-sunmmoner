package com.example.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.game.diorama.Courtyard
import com.example.game.diorama.CourtyardRenderer
import com.example.game.diorama.GroundPoint
import kotlinx.coroutines.delay
import kotlin.math.abs

private class CourtyardView(
  context: Context,
  onReady: () -> Unit,
  onFailure: () -> Unit,
) : GLSurfaceView(context) {
  private val courtyard = CourtyardRenderer(
    context.applicationContext,
    onReady = { post { if (isAttachedToWindow) onReady() } },
    onFailure = { post { if (isAttachedToWindow) onFailure() } },
  )
  private val slop = ViewConfiguration.get(context).scaledTouchSlop
  private var startX = 0f
  private var startY = 0f
  private var tapped = false

  init {
    setEGLContextClientVersion(3)
    setEGLConfigChooser(8, 8, 8, 0, 16, 0)
    preserveEGLContextOnPause = true
    setRenderer(courtyard)
    renderMode = RENDERMODE_WHEN_DIRTY
    contentDescription = "Sân Bậc Đá, chạm đường đi để di chuyển Kaelen"
    isClickable = true
  }

  fun walkTo(destination: GroundPoint) {
    queueEvent { courtyard.walkTo(destination) }
  }

  fun zoomBy(delta: Float) {
    queueEvent { courtyard.zoomBy(delta) }
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        startX = event.x
        startY = event.y
        tapped = true
      }
      MotionEvent.ACTION_MOVE -> {
        if (abs(event.x - startX) > slop || abs(event.y - startY) > slop) tapped = false
      }
      MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> tapped = false
      MotionEvent.ACTION_UP -> {
        if (tapped) {
          val x = event.x
          val y = event.y
          queueEvent { courtyard.chooseDestination(x, y) }
          performClick()
        }
        tapped = false
      }
    }
    return true
  }

  override fun performClick(): Boolean {
    super.performClick()
    return true
  }
}

@Composable
internal fun SanctuaryCourtyardScreen(onExit: () -> Unit) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val supported = remember(context) {
    val gl = context.getSystemService(ActivityManager::class.java)?.deviceConfigurationInfo?.reqGlEsVersion ?: 0
    gl >= 0x30000
  }
  var view by remember { mutableStateOf<CourtyardView?>(null) }
  var ready by remember { mutableStateOf(false) }
  var failed by remember { mutableStateOf(false) }
  val scene = view

  BackHandler(onBack = onExit)
  if (scene != null && !failed) {
    DisposableEffect(scene, lifecycle) {
      val observer = LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_PAUSE -> scene.onPause()
          Lifecycle.Event.ON_RESUME -> scene.onResume()
          else -> Unit
        }
      }
      lifecycle.addObserver(observer)
      if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) scene.onPause()
      onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(scene, lifecycle) {
      lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        while (true) {
          scene.requestRender()
          delay(41L)
        }
      }
    }
  }

  Box(Modifier.fillMaxSize().background(Color(0xFF161F23)).testTag("sanctuary_3d_courtyard")) {
    if (supported && !failed) {
      AndroidView(
        factory = { host ->
          CourtyardView(host, onReady = { ready = true }, onFailure = { failed = true }).also { view = it }
        },
        modifier = Modifier.fillMaxSize().testTag("sanctuary_3d_surface"),
      )
    }
    if (!supported || failed) {
      Text(
        if (!supported) "Sân 3D cần thiết bị hỗ trợ OpenGL ES 3.0." else "Không thể mở cảnh 3D trên thiết bị này.",
        modifier = Modifier.align(Alignment.Center).padding(20.dp),
        color = Color.White,
      )
    } else if (!ready) {
      CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
    Column(
      modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.safeDrawing).padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Button(onClick = onExit, modifier = Modifier.testTag("sanctuary_3d_exit")) { Text("Về Thánh Địa") }
      if (ready && !failed) {
        Column(
          Modifier.background(Color(0xDF141D22), MaterialTheme.shapes.small).padding(horizontal = 4.dp),
        ) {
          for (place in Courtyard.landmarks) {
            TextButton(onClick = { view?.walkTo(place.destination) }) {
              Text(place.label, color = Color(0xFFFFEBCB), style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    }
    if (ready && !failed) {
      Row(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(onClick = { view?.zoomBy(.13f) }, modifier = Modifier.testTag("sanctuary_3d_zoom_out")) { Text("−") }
        Text(
          "Chạm đường đi để di chuyển",
          modifier = Modifier.background(Color(0xDF141D22), MaterialTheme.shapes.small).padding(8.dp),
          color = Color.White,
          style = MaterialTheme.typography.labelMedium,
        )
        Button(onClick = { view?.zoomBy(-.13f) }, modifier = Modifier.testTag("sanctuary_3d_zoom_in")) { Text("+") }
      }
    }
  }
}
