package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.HeroClassType
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.map.PropKind
import com.example.game.pixel.PixelSpriteRenderer.drawActorCanopyOcclusion
import com.example.game.pixel.PixelSpriteRenderer.drawCharacterSprite
import com.example.game.pixel.PixelSpriteRenderer.drawProp
import com.example.game.pixel.PixelSpriteRenderer.drawWorldBackground
import com.example.game.pixel.SanctuaryArtAssets
import com.example.game.pixel.SanctuaryEnvironmentShadows.drawGroundShadows
import com.example.game.pixel.SanctuaryWorldRenderer.drawSanctuaryFloatingWorld
import com.example.game.tower.GeneratedMapLayout
import com.example.game.tower.TowerFloorCatalog
import com.example.game.world.WorldEnvironment
import com.example.game.world.WorldWeather
import java.io.File
import kotlin.math.abs
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * P23 REAL ENGINE RENDER (Robolectric native Skia) screenshot matrix for Sanctuary architecture
 * 2.5D repair + environmental props cohesion across zooms 0.36, 0.88, 1.00, and 1.48.
 *
 * Every camera, zoom, timeSec, weather, and actor position is locked so pre-P23
 * (`docs/visual-evidence/p23/engine-render-before/`) and post-P23
 * (`docs/visual-evidence/p23/engine-render/`) compare pixel-for-pixel through the production
 * rendering path without cropping roofs, ridges, portals, or foundations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapForgeP23RenderTest {

  private val outDir = File("build/visual_screenshots/p23")

  private data class Actor(
    val x: Float,
    val y: Float,
    val moving: Boolean = false,
    val progress: Float = 0f,
    val facingNorth: Boolean = false,
  )

  private fun prepare() {
    val context = RuntimeEnvironment.getApplication()
    SanctuaryArtAssets.init(context)
    WorldEnvironment.init(context)
    WorldEnvironment.debugOverride(12f, WorldWeather.CLEAR)
    WorldEnvironment.update(50000)
  }

  @Test
  fun captureArchitectureAndPropsThroughTheRealRenderer() {
    prepare()
    outDir.mkdirs()
    val sanctuary = SanctuaryLobbyEngine().mapLayout
    val floor1 = TowerFloorCatalog.buildTowerFloorMap(TowerFloorCatalog.FLOORS[0])
    val floor3 = TowerFloorCatalog.buildTowerFloorMap(TowerFloorCatalog.FLOORS[2])
    val f3Column = floor3.obstacles
      .filter { it.propKind == PropKind.OBSIDIAN_COLUMN }
      .minBy { abs(it.y - 680f) + abs(it.x - 1400f) * 0.25f }

    val records = JSONArray()

    try {
      fun renderScene(
        name: String,
        layout: GeneratedMapLayout,
        isSanctuary: Boolean,
        camX: Float,
        camY: Float,
        zoom: Float,
        timeSec: Float = 0f,
        focusIds: List<String> = emptyList(),
        actors: List<Actor> = emptyList(),
        minBytes: Long = 20_000,
      ) {
        val w = 1280
        val h = 720
        val image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat())) {
          if (isSanctuary) {
            drawSanctuaryFloatingWorld(camX, camY, zoom, timeSec, layout.paths)
            drawGroundShadows(layout.obstacles, camX, camY, zoom, clipToIsland = true)
          } else {
            drawWorldBackground(
              biome = layout.biome,
              worldWidth = layout.width,
              worldHeight = layout.height,
              wallMargin = layout.wallMargin,
              camX = camX,
              camY = camY,
              zoom = zoom,
              timeSec = timeSec,
              patches = layout.patches,
              paths = layout.paths,
            )
            drawGroundShadows(
              obstacles = layout.obstacles,
              camX = camX,
              camY = camY,
              zoom = zoom,
              clipToIsland = false,
              groundWidth = layout.width,
              groundHeight = layout.height,
            )
          }
          val pending = actors.sortedBy { it.y }.toMutableList()
          for (obs in layout.obstacles.sortedBy { it.y + it.sortYBias }) {
            while (pending.isNotEmpty() && pending.first().y < obs.y + obs.sortYBias) {
              val a = pending.removeAt(0)
              drawActor(a, camX, camY, zoom, timeSec)
              drawActorCanopyOcclusion(layout.obstacles, a.x, a.y, camX, camY, zoom, timeSec)
            }
            drawProp(obs, camX, camY, zoom, timeSec, renderShadow = null)
          }
          for (a in pending) {
            drawActor(a, camX, camY, zoom, timeSec)
            drawActorCanopyOcclusion(layout.obstacles, a.x, a.y, camX, camY, zoom, timeSec)
          }
        }
        val file = File(outDir, "$name.png")
        file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        require(file.length() > minBytes) { "$name.png must not be empty (${file.length()} B)" }

        val propDetails = JSONArray()
        for (pid in focusIds) {
          val o = layout.obstacles.firstOrNull { it.id == pid } ?: continue
          propDetails.put(
            JSONObject()
              .put("id", o.id)
              .put("kind", o.propKind.name)
              .put("x", o.x.toDouble())
              .put("y", o.y.toDouble())
              .put("scale", o.scale.toDouble())
          )
        }
        records.put(
          JSONObject()
            .put("file", "$name.png")
            .put("scene", if (isSanctuary) "SANCTUARY_ASTRAL" else layout.biome.name)
            .put("camX", camX.toDouble())
            .put("camY", camY.toDouble())
            .put("zoom", zoom.toDouble())
            .put("timeSec", timeSec.toDouble())
            .put("props", propDetails)
        )
      }

      // 1) Whole Sanctuary overview at zoom 0.36 — island silhouette, perimeter towers, facilities, props.
      renderScene(
        name = "sanctuary_overview_z036",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1600f,
        camY = 1380f,
        zoom = 0.36f,
        focusIds = listOf("monument_tower_gate", "building_summon_hall", "building_armory", "building_hero_quarters", "building_restaurant", "building_hero_registry", "building_synthesis_lab"),
        minBytes = 60_000,
      )

      // 2) Service district cohesion at gameplay zoom 0.88 and mid zoom 1.00.
      renderScene(
        name = "service_cluster_z088",
        layout = sanctuary,
        isSanctuary = true,
        camX = 960f,
        camY = 860f,
        zoom = 0.88f,
        focusIds = listOf("building_armory", "building_hero_quarters", "building_restaurant", "building_hero_registry", "nw_outer_slate_house"),
      )
      renderScene(
        name = "service_cluster_z100",
        layout = sanctuary,
        isSanctuary = true,
        camX = 960f,
        camY = 860f,
        zoom = 1.00f,
        focusIds = listOf("building_armory", "building_hero_quarters", "building_restaurant", "building_hero_registry"),
      )

      // 3) Armory vertical slice: full uncropped roof+base at 0.88, 1.00, 1.48 + door trigger actor + behind occlusion.
      renderScene(
        name = "armory_full_z088",
        layout = sanctuary,
        isSanctuary = true,
        camX = 995f,
        camY = 580f,
        zoom = 0.88f,
        focusIds = listOf("building_armory", "armory_outdoor_anvil", "nw_corner_tower"),
      )
      renderScene(
        name = "armory_full_z100",
        layout = sanctuary,
        isSanctuary = true,
        camX = 995f,
        camY = 580f,
        zoom = 1.00f,
        focusIds = listOf("building_armory", "armory_outdoor_anvil"),
      )
      renderScene(
        name = "armory_full_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 995f,
        camY = 560f,
        zoom = 1.48f,
        focusIds = listOf("building_armory", "armory_outdoor_anvil"),
      )
      // Lyra standing right at Armory's playable entrance trigger (993, 655) / spawn (993, 673).
      renderScene(
        name = "armory_door_trigger_actor_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 995f,
        camY = 585f,
        zoom = 1.48f,
        focusIds = listOf("building_armory"),
        actors = listOf(Actor(993f, 666f, moving = false, facingNorth = true)),
      )
      // Lyra behind Armory (north of y=655) verifying solid roof/wall Y-sort occlusion without alpha holes.
      renderScene(
        name = "armory_behind_occlusion_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 995f,
        camY = 555f,
        zoom = 1.48f,
        focusIds = listOf("building_armory"),
        actors = listOf(Actor(972f, 520f, moving = false)),
      )

      // 4) Enterable Hero Quarters (trigger 823, 790) & Restaurant (trigger 1103, 925) — uncropped roof + door + Lyra.
      renderScene(
        name = "quarters_door_actor_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 825f,
        camY = 710f,
        zoom = 1.48f,
        focusIds = listOf("building_hero_quarters"),
        actors = listOf(Actor(823f, 798f, moving = false, facingNorth = true)),
      )
      renderScene(
        name = "restaurant_door_actor_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1105f,
        camY = 845f,
        zoom = 1.48f,
        focusIds = listOf("building_restaurant"),
        actors = listOf(Actor(1103f, 933f, moving = false, facingNorth = true)),
      )

      // 5) Zone-only facilities (Hero Registry & Synthesis Lab) + Sanctuary cottages & corner tower at 1.48.
      renderScene(
        name = "registry_close_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 785f,
        camY = 1105f,
        zoom = 1.48f,
        focusIds = listOf("building_hero_registry"),
        actors = listOf(Actor(785f, 1196f, moving = false, facingNorth = true)),
      )
      renderScene(
        name = "cottages_nw_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 575f,
        camY = 865f,
        zoom = 1.48f,
        focusIds = listOf("nw_outer_slate_house", "nw_outer_brown_cottage"),
      )
      renderScene(
        name = "synthesis_cottage_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1320f,
        camY = 480f,
        zoom = 1.48f,
        focusIds = listOf("building_synthesis_lab", "inner_slate_wing_cottage"),
      )

      // 6) Monumental landmarks: Summoning Hall & Tower Gate (uncropped at 1.48 + gameplay/zone actor at 1.00).
      renderScene(
        name = "summoning_hall_whole_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 2430f,
        camY = 1715f,
        zoom = 1.48f,
        focusIds = listOf("building_summon_hall"),
      )
      renderScene(
        name = "summoning_hall_zone_actor_z100",
        layout = sanctuary,
        isSanctuary = true,
        camX = 2405f,
        camY = 1855f,
        zoom = 1.00f,
        focusIds = listOf("building_summon_hall"),
        actors = listOf(Actor(2380f, 1930f, moving = false, facingNorth = true)),
      )
      renderScene(
        name = "tower_gate_whole_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1960f,
        camY = 310f,
        zoom = 1.48f,
        focusIds = listOf("monument_tower_gate", "ne_corner_tower"),
      )
      renderScene(
        name = "tower_gate_portal_actor_z100",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1960f,
        camY = 460f,
        zoom = 1.00f,
        timeSec = 0.25f,
        focusIds = listOf("monument_tower_gate"),
        actors = listOf(Actor(1960f, 635f, moving = false, facingNorth = true)),
      )

      // 7) Environmental props close-ups: Street Lantern (frame 0 vs frame 1), Planter Bed, Wooden Bench, South Gate + Training Yard.
      renderScene(
        name = "avenue_props_lantern_f0_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1185f,
        camY = 1015f,
        zoom = 1.48f,
        timeSec = 0.0f,
      )
      renderScene(
        name = "avenue_props_lantern_f1_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1185f,
        camY = 1015f,
        zoom = 1.48f,
        timeSec = 0.18f,
      )
      renderScene(
        name = "south_gate_training_z088",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1320f,
        camY = 1980f,
        zoom = 0.88f,
        focusIds = listOf("south_gate_tower_west", "south_gate_tower_east", "sw_corner_tower", "training_striking_post"),
      )

      // 8) Tower Floor 1 & Floor 3 stone ruins + lanterns alongside P22 vegetation.
      renderScene(
        name = "tower_f1_ruins_lantern_z088",
        layout = floor1,
        isSanctuary = false,
        camX = 580f,
        camY = 1090f,
        zoom = 0.88f,
        focusIds = listOf("f1_ruin_arch_0", "f1_ruin_arch_1", "f1_lantern_w"),
      )
      renderScene(
        name = "tower_f1_ruin_arch_close_z148",
        layout = floor1,
        isSanctuary = false,
        camX = 640f,
        camY = 890f,
        zoom = 1.48f,
        focusIds = listOf("f1_ruin_arch_1", "f1_lantern_w"),
      )
      renderScene(
        name = "tower_f3_obsidian_column_z148",
        layout = floor3,
        isSanctuary = false,
        camX = f3Column.x,
        camY = f3Column.y - 35f,
        zoom = 1.48f,
        focusIds = listOf(f3Column.id),
      )

      File(outDir, "camera_props.json").writeText(records.toString(2) + "\n")
    } finally {
      WorldEnvironment.debugOverride()
      WorldEnvironment.update()
    }
  }

  private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawActor(
    a: Actor,
    camX: Float,
    camY: Float,
    zoom: Float,
    timeSec: Float,
  ) = drawCharacterSprite(
    worldX = a.x,
    worldY = a.y,
    camX = camX,
    camY = camY,
    zoom = zoom,
    isMaster = false,
    heroClass = HeroClassType.ARCHER,
    facingRight = true,
    isMoving = a.moving,
    attackAnimProgress = a.progress,
    hurtFlash = 0f,
    hpFraction = 1f,
    shieldOrAuraColor = null,
    isInjuredResting = false,
    timeSec = timeSec,
    facingNorth = a.facingNorth,
    heroName = "Lyra",
    skillAnimation = false,
    emotion = "calm",
  )
}
