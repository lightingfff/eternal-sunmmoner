package com.example.game.pixel

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONObject
import java.io.InputStream

/**
 * SanctuaryAssetManager: Manages loading and caching of individual extracted PNG asset files
 * and JSON manifest from /app/src/main/assets/sanctuary_pack/.
 */
object SanctuaryAssetManager {

  private val imageCache = mutableMapOf<String, ImageBitmap>()
  private var isInitialized = false

  var tileAssets = mutableListOf<ImageBitmap>()
  var roadAssets = mutableListOf<ImageBitmap>()
  var treeAssets = mutableListOf<ImageBitmap>()
  var propAssets = mutableListOf<ImageBitmap>()
  var buildingMap = mutableMapOf<String, ImageBitmap>()

  fun init(context: Context) {
    if (isInitialized) return
    try {
      val assetManager = context.assets
      val manifestStream: InputStream = assetManager.open("sanctuary_pack/sanctuary_assets_manifest.json")
      val jsonString = manifestStream.bufferedReader().use { it.readText() }
      val rootJson = JSONObject(jsonString)
      val categories = rootJson.getJSONObject("categories")

      // Load Tiles
      val tilesArr = categories.optJSONArray("tiles")
      if (tilesArr != null) {
        for (i in 0 until tilesArr.length()) {
          val item = tilesArr.getJSONObject(i)
          val path = item.getString("path")
          loadAssetBitmap(context, path)?.let { tileAssets.add(it) }
        }
      }

      // Load Roads
      val roadsArr = categories.optJSONArray("roads")
      if (roadsArr != null) {
        for (i in 0 until roadsArr.length()) {
          val item = roadsArr.getJSONObject(i)
          val path = item.getString("path")
          loadAssetBitmap(context, path)?.let { roadAssets.add(it) }
        }
      }

      // Load Vegetation
      val vegArr = categories.optJSONArray("vegetation")
      if (vegArr != null) {
        for (i in 0 until vegArr.length()) {
          val item = vegArr.getJSONObject(i)
          val path = item.getString("path")
          loadAssetBitmap(context, path)?.let { treeAssets.add(it) }
        }
      }

      // Load Props
      val propsArr = categories.optJSONArray("props")
      if (propsArr != null) {
        for (i in 0 until propsArr.length()) {
          val item = propsArr.getJSONObject(i)
          val path = item.getString("path")
          loadAssetBitmap(context, path)?.let { propAssets.add(it) }
        }
      }

      // Load Buildings
      val bldgArr = categories.optJSONArray("buildings")
      if (bldgArr != null) {
        for (i in 0 until bldgArr.length()) {
          val item = bldgArr.getJSONObject(i)
          val id = item.getString("id")
          val path = item.getString("path")
          loadAssetBitmap(context, path)?.let { buildingMap[id] = it }
        }
      }

      isInitialized = true
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun loadAssetBitmap(context: Context, relativePath: String): ImageBitmap? {
    if (imageCache.containsKey(relativePath)) {
      return imageCache[relativePath]
    }
    return try {
      val stream: InputStream = context.assets.open(relativePath)
      val bmp = BitmapFactory.decodeStream(stream)
      stream.close()
      if (bmp != null) {
        val imgBmp = bmp.asImageBitmap()
        imageCache[relativePath] = imgBmp
        imgBmp
      } else null
    } catch (e: Exception) {
      null
    }
  }
}
