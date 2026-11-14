package com.example.game.pixel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.game.combat.CombatFxStyle
import java.security.MessageDigest
import org.json.JSONObject

/** Optional authored execution clips; bounded with the original enemy bank, independent of damage. */
internal object TowerCombatFxAtlas {
  data class Frame(val image: ImageBitmap,val pivotX:Int,val pivotY:Int)
  @Volatile private var frames: Array<Array<Frame>>? = null
  private var attempted=false
  internal var sourcePack: String? = null; private set
  internal val hasAuthoredExecution: Boolean get() = frames != null && sourcePack == "art_completion_v1/effects"
  @Volatile internal var decodeCount=0;private set
  @Volatile internal var retainedBytes=0L;private set
  @Synchronized fun preload(context:Context) {
    if(attempted)return
    for (root in listOf("art_completion_v1/effects", "pixelfx_v1", "tower_combat_v1")) {
      val allocated=mutableListOf<Bitmap>()
      try {
        val meta=context.assets.open("$root/vfx_manifest.json").bufferedReader().use { JSONObject(it.readText()) }
        require(meta.getInt("version")==1 && meta.getString("file")=="vfx.png" && meta.getInt("columns")==8)
        val rows=meta.getJSONArray("rows")
        val expected = if (root == "art_completion_v1/effects")
          listOf("guard", "arcane", "cleave", "holy", "shadow", "impact", "shield_impact", "slam")
          else listOf("guard", "arcane", "cleave", "holy")
        require(rows.length() == expected.size && expected.indices.map { rows.getString(it) } == expected)
        val width=meta.getJSONArray("frame").getInt(0);val height=meta.getJSONArray("frame").getInt(1)
        val px=meta.getJSONArray("pivot").getInt(0);val py=meta.getJSONArray("pivot").getInt(1)
        require(width==96 && height==96 && px in 0 until width && py in 0 until height)
        val bytes=context.assets.open("$root/vfx.png").use { it.readBytes() }
        require(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }==meta.getString("sha256"))
        decodeCount++
        val source=requireNotNull(BitmapFactory.decodeByteArray(bytes,0,bytes.size,
          BitmapFactory.Options().apply { inScaled=false;inPreferredConfig=Bitmap.Config.ARGB_8888 }))
        val prepared=try {
          require(source.width==width*8 && source.height==height*expected.size && source.isPremultiplied)
          val paint=Paint().apply { isFilterBitmap=false;isAntiAlias=false }
          Array(expected.size) { row -> Array(8) { column ->
            val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
            allocated+=bitmap
            Canvas(bitmap).drawBitmap(source,android.graphics.Rect(column*width,row*height,(column+1)*width,(row+1)*height),
              android.graphics.Rect(0,0,width,height),paint)
            Frame(bitmap.asImageBitmap(),px,py)
          } }
        } finally { source.recycle() }
        val total=allocated.sumOf { it.allocationByteCount.toLong() }
        require(total+TowerMonsterAtlas.retainedBytes+CompletionProjectileAtlas.retainedBytes<=8L*1024*1024)
        retainedBytes=total
        sourcePack=root
        // Publish pixels last: acquiring the frame bank also acquires its matching source metadata.
        frames=prepared
        attempted=true
        return
      } catch(error:Exception) {
        allocated.forEach { if(!it.isRecycled)it.recycle() }
        android.util.Log.w("TowerCombatFxAtlas","Optional $root unavailable; trying existing fallback",error)
      } catch(error:OutOfMemoryError) {
        allocated.forEach { if(!it.isRecycled)it.recycle() }
        android.util.Log.w("TowerCombatFxAtlas","Effect pack exceeded available memory; using fallback")
      }
    }
    attempted=true
  }
  fun frame(style:CombatFxStyle,progress:Float):Frame? {
    val pack=frames?:return null
    val row=when(style) {
      CombatFxStyle.GUARD -> 0
      CombatFxStyle.ARCANE -> 1
      CombatFxStyle.CLEAVE -> 2
      CombatFxStyle.HOLY -> 3
      CombatFxStyle.SHADOW -> if (pack.size >= 8) 4 else 2
      CombatFxStyle.IMPACT -> 5
      CombatFxStyle.SHIELD_IMPACT -> 6
      CombatFxStyle.SLAM -> 7
      else -> return null
    }
    if (row !in pack.indices) return null
    return pack[row][(progress.coerceIn(0f,.99999f)*8).toInt()]
  }
}
