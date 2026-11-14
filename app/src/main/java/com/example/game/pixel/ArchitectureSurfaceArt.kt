package com.example.game.pixel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.LightingColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import java.security.MessageDigest
import org.json.JSONObject

/** Finite original surface bank. Repeating nearest shaders follow face axes, not the ground camera twice. */
internal object ArchitectureSurfaceArt {
  private data class Material(val shader: BitmapShader, val paints: Array<Paint>)
  @Volatile private var materials: Map<String, Material> = emptyMap()
  private var attempted = false
  private val matrix = Matrix()
  private val values = FloatArray(9)
  internal var decodeCount = 0; private set
  internal var retainedBytes = 0L; private set
  internal val loadedCount get() = materials.size

  @Synchronized fun preload(context: Context) {
    if (attempted) return
    attempted = true
    val owned = mutableListOf<Bitmap>()
    try {
      val root = "art_completion_v1/surfaces"
      val meta = context.assets.open("$root/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
      require(meta.getInt("version") == 1)
      val entries = meta.getJSONArray("materials")
      val names = listOf("cliff", "limestone", "carpet", "canal")
      require(entries.length() == names.size)
      val prepared = linkedMapOf<String, Material>()
      for (i in names.indices) {
        val name = names[i]
        val entry = entries.getJSONObject(i)
        require(entry.getString("id") == name && entry.getString("file") == "$name.png")
        val bytes = context.assets.open("$root/$name.png").use { it.readBytes() }
        require(bytes.size in 1..512_000)
        require(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } == entry.getString("sha256"))
        val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
          BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 }))
        owned += bitmap
        decodeCount++
        require(bitmap.width == 256 && bitmap.height == 256 && (!bitmap.hasAlpha() || bitmap.isPremultiplied))
        val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        val paints = arrayOf(null, LightingColorFilter(0xFF9CA8BA.toInt(), 0),
          LightingColorFilter(0xFF6C7A91.toInt(), 0)).map { tint -> Paint().apply {
          isAntiAlias = false; isFilterBitmap = false; this.shader = shader; colorFilter = tint
        } }.toTypedArray()
        prepared[name] = Material(shader, paints)
      }
      val total = owned.sumOf { it.allocationByteCount.toLong() }
      require(total <= 1024L * 1024)
      retainedBytes = total
      materials = prepared.toMap()
    } catch (error: Exception) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("ArchitectureSurfaceArt", "Optional surfaces unavailable; keeping original geometry", error)
    } catch (error: OutOfMemoryError) {
      owned.forEach { if (!it.isRecycled) it.recycle() }
      android.util.Log.w("ArchitectureSurfaceArt", "Surface memory unavailable; keeping original geometry")
    }
  }

  /** U/V are screen displacement per texture pixel. World/face anchoring keeps texture phase stable on pan. */
  fun DrawScope.drawSurface(name: String, mask: Path, origin: Offset, u: Offset, v: Offset,
    alpha: Float = 1f, tone: Int = 0): Boolean {
    val material = materials[name] ?: return false
    val bounds = mask.getBounds()
    if (bounds.right < 0f || bounds.bottom < 0f || bounds.left > size.width || bounds.top > size.height) return true
    val determinant = u.x * v.y - u.y * v.x
    if (!determinant.isFinite() || kotlin.math.abs(determinant) < .000001f) return false
    values[0] = u.x; values[1] = v.x; values[2] = origin.x
    values[3] = u.y; values[4] = v.y; values[5] = origin.y
    values[6] = 0f; values[7] = 0f; values[8] = 1f
    matrix.setValues(values)
    material.shader.setLocalMatrix(matrix)
    val paint = material.paints[tone.coerceIn(0, 2)]
    paint.style = Paint.Style.FILL
    paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
    clipPath(mask) { drawContext.canvas.nativeCanvas.drawRect(maxOf(0f, bounds.left), maxOf(0f, bounds.top),
      minOf(size.width, bounds.right), minOf(size.height, bounds.bottom), paint) }
    return true
  }

  fun DrawScope.drawCanalBank(start: Offset, end: Offset, width: Float, origin: Offset, zoom: Float) {
    val material = materials["canal"] ?: return
    matrix.setScale(.7f * zoom, .7f * zoom * .72f)
    matrix.postTranslate(origin.x, origin.y)
    material.shader.setLocalMatrix(matrix)
    val paint = material.paints[0]
    paint.alpha = 255
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = width
    paint.strokeCap = Paint.Cap.ROUND
    drawContext.canvas.nativeCanvas.drawLine(start.x, start.y, end.x, end.y, paint)
    paint.style = Paint.Style.FILL
  }
}
