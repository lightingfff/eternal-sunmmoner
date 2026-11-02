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
 * P22 REAL ENGINE RENDER (Robolectric native Skia) screenshot matrix for Sanctuary & Tower vegetation.
 *
 * Every camera, zoom, time-of-day, weather, and actor position is locked so pre-P22 (`engine-render-before/`)
 * and post-P22 (`engine-render/`) captures compare pixel-for-pixel through the exact production draw loop:
 * - Sanctuary frames: `drawSanctuaryFloatingWorld` + `drawGroundShadows` + Y-sorted `drawProp` +
 *   `drawCharacterSprite` + `drawActorCanopyOcclusion` (matching `SanctuaryLobbyScreen`).
 * - Tower Floor 1 & 2 frames (explicitly prefixed `tower_f1_` / `tower_f2_` because `pine.png`,
 *   `twisted.png`, and `FOREST_CANOPY_BUSH` have 0 instances in `buildSanctuaryMap` and live in
 *   Tower biomes): `drawWorldBackground` + `drawGroundShadows(clipToIsland=false)` + Y-sorted
 *   `drawProp` + `drawCharacterSprite` + `drawActorCanopyOcclusion` (matching `TowerBattleScreen`).
 *
 * This is REAL ENGINE RENDER (Robolectric software Skia at 1x density), NOT physical-device FPS/thermal data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapForgeP22RenderTest {

  private val outDir = File("build/visual_screenshots/p22")

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
  fun captureVegetationMatrixThroughTheRealRenderer() {
    prepare()
    outDir.mkdirs()
    val sanctuary = SanctuaryLobbyEngine().mapLayout
    val floor1 = TowerFloorCatalog.buildTowerFloorMap(TowerFloorCatalog.FLOORS[0])
    val floor2 = TowerFloorCatalog.buildTowerFloorMap(TowerFloorCatalog.FLOORS[1])
    val plumFocus = floor2.obstacles
      .filter { it.propKind == PropKind.MYSTIC_PLUM_TREE }
      .minBy { abs(it.y - 620f) + abs(it.x - 1500f) * 0.25f }

    val records = JSONArray()

    try {
      fun renderScene(
        name: String,
        layout: GeneratedMapLayout,
        isSanctuary: Boolean,
        camX: Float,
        camY: Float,
        zoom: Float,
        focusProps: List<String>,
        actors: List<Actor> = emptyList(),
        minBytes: Long = 20_000,
      ) {
        val w = 1280
        val h = 720
        val image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat())) {
          if (isSanctuary) {
            drawSanctuaryFloatingWorld(camX, camY, zoom, 0f, layout.paths)
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
              timeSec = 0f,
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
              drawActor(a, camX, camY, zoom)
              drawActorCanopyOcclusion(layout.obstacles, a.x, a.y, camX, camY, zoom, 0f)
            }
            drawProp(obs, camX, camY, zoom, 0f, renderShadow = null)
          }
          for (a in pending) {
            drawActor(a, camX, camY, zoom)
            drawActorCanopyOcclusion(layout.obstacles, a.x, a.y, camX, camY, zoom, 0f)
          }
        }
        val file = File(outDir, "$name.png")
        file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        require(file.length() > minBytes) { "$name.png must not be empty (${file.length()} B)" }

        val propDetails = JSONArray()
        for (pid in focusProps) {
          val o = layout.obstacles.first { it.id == pid }
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
            .put("props", propDetails)
        )
      }

      // 1) Sanctuary island overview z=0.36 — whole-scene architectural + vegetation balance.
      renderScene(
        name = "sanctuary_overview_z036",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1600f,
        camY = 1380f,
        zoom = 0.36f,
        focusProps = listOf("sanctuary_sakura_1", "sanctuary_sakura_2", "sanctuary_sakura_14", "sanctuary_sakura_15", "sanctuary_hedge_bush_2"),
        minBytes = 60_000,
      )

      // 2) Gameplay medium z=0.88 — NE Sakura garden + hedge_bush_4 beside P21 Tower Gate.
      renderScene(
        name = "sanctuary_ne_garden_z088",
        layout = sanctuary,
        isSanctuary = true,
        camX = 2360f,
        camY = 620f,
        zoom = 0.88f,
        focusProps = listOf("sanctuary_sakura_2", "sanctuary_sakura_3", "sanctuary_sakura_4", "sanctuary_hedge_bush_4"),
      )

      // 3) Gameplay medium z=0.88 — unobstructed South Sakura pair: even canopy_a (#14) + odd canopy_b (#15).
      renderScene(
        name = "sanctuary_south_sakura_pair_z088",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1790f,
        camY = 1890f,
        zoom = 0.88f,
        focusProps = listOf("sanctuary_sakura_14", "sanctuary_sakura_15"),
      )

      // 4) Mid z=1.00 — full-tree screenshot with crowns, trunks, roots, and cast shadows visible.
      renderScene(
        name = "sanctuary_south_sakura_mid_z100",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1790f,
        camY = 1880f,
        zoom = 1.00f,
        focusProps = listOf("sanctuary_sakura_14", "sanctuary_sakura_15"),
      )

      // 5) Gameplay z=0.88 — residential hedge courtyard + Restaurant/Quarters/Registry door visibility.
      renderScene(
        name = "sanctuary_courtyard_hedges_z088",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1060f,
        camY = 960f,
        zoom = 0.88f,
        focusProps = listOf("sanctuary_hedge_bush_1", "sanctuary_hedge_bush_2", "sanctuary_hedge_bush_3"),
      )

      // 6) Mid z=1.00 — North residential lawn: odd Sakura (#0) + P20 Cypress control (#2) + hedge (#0) + Synthesis/Armory.
      renderScene(
        name = "sanctuary_north_lawn_control_z100",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1350f,
        camY = 560f,
        zoom = 1.00f,
        focusProps = listOf("sanctuary_sakura_0", "sanctuary_cypress_2", "sanctuary_hedge_bush_0"),
      )

      // 7) Close z=1.48 — FULL-TREE framing of even Sakura A (`sanctuary_sakura_14` @ 1715,1845; camera pulled north so crown is unclipped).
      renderScene(
        name = "sakura_a_full_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1715f,
        camY = 1705f,
        zoom = 1.48f,
        focusProps = listOf("sanctuary_sakura_14"),
      )

      // 8) Close z=1.48 — foliage/bark/root inspection framing on `sanctuary_sakura_14`.
      renderScene(
        name = "sakura_a_foliage_detail_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1715f,
        camY = 1765f,
        zoom = 1.48f,
        focusProps = listOf("sanctuary_sakura_14"),
      )

      // 9) Close z=1.48 — FULL-TREE framing of odd Sakura B (`sanctuary_sakura_15` @ 1865,2025; unclipped crown + roots + shadow).
      renderScene(
        name = "sakura_b_full_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1865f,
        camY = 1915f,
        zoom = 1.48f,
        focusProps = listOf("sanctuary_sakura_15"),
      )

      // 10-13) Real engine layer sorting + `drawActorCanopyOcclusion` around `sanctuary_sakura_14` (1715,1845) at z=0.88:
      //   - front of trunk (y=1892)
      //   - under upper crown overhang (y=1868, inside drawActorCanopyOcclusion south window)
      //   - behind upper canopy (x=1742, y=1805, north of trunk under lateral bough)
      //   - at canopy edge (x=1785, y=1825)
      renderScene(
        name = "sakura_occlusion_front_z088",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1715f,
        camY = 1800f,
        zoom = 0.88f,
        focusProps = listOf("sanctuary_sakura_14", "sanctuary_sakura_15"),
        actors = listOf(Actor(1715f, 1892f)),
      )
      renderScene(
        name = "sakura_occlusion_under_crown_z088",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1715f,
        camY = 1800f,
        zoom = 0.88f,
        focusProps = listOf("sanctuary_sakura_14", "sanctuary_sakura_15"),
        actors = listOf(Actor(1715f, 1868f)),
      )
      renderScene(
        name = "sakura_occlusion_behind_crown_z088",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1715f,
        camY = 1800f,
        zoom = 0.88f,
        focusProps = listOf("sanctuary_sakura_14", "sanctuary_sakura_15"),
        actors = listOf(Actor(1742f, 1805f)),
      )
      renderScene(
        name = "sakura_occlusion_canopy_edge_z088",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1715f,
        camY = 1800f,
        zoom = 0.88f,
        focusProps = listOf("sanctuary_sakura_14", "sanctuary_sakura_15"),
        actors = listOf(Actor(1785f, 1825f)),
      )

      // 14) Close z=1.48 — courtyard shrubs front/behind depth sort + Restaurant doorway actor unobstructed.
      renderScene(
        name = "courtyard_hedge_front_behind_z148",
        layout = sanctuary,
        isSanctuary = true,
        camX = 1090f,
        camY = 1005f,
        zoom = 1.48f,
        focusProps = listOf("sanctuary_hedge_bush_1", "sanctuary_hedge_bush_2", "sanctuary_hedge_bush_3"),
        actors = listOf(
          Actor(1065f, 1118f),                 // south/in front of hedge_bush_2
          Actor(1215f, 1028f),                 // north/behind hedge_bush_3 (feet occluded, body clear)
          Actor(1103f, 938f, moving = true),   // Restaurant doorway threshold clear
        ),
      )

      // 15-17) Tower Floor 1 (`FIRST_FIELD_MEADOW`) — runtime verification for `pine.png` & `FOREST_CANOPY_BUSH`
      //        alongside P18 `oak.png` control (explicitly labeled Tower Floor 1; 0 Sanctuary instances).
      renderScene(
        name = "tower_f1_pine_oak_bush_z088",
        layout = floor1,
        isSanctuary = false,
        camX = 430f,
        camY = 1060f,
        zoom = 0.88f,
        focusProps = listOf("f1_spawn_grove_0", "f1_spawn_grove_1", "f1_spawn_grove_2", "f1_spawn_grove_3", "f1_spawn_grove_5"),
      )
      renderScene(
        name = "tower_f1_pine_full_z148",
        layout = floor1,
        isSanctuary = false,
        camX = 430f,
        camY = 800f,
        zoom = 1.48f,
        focusProps = listOf("f1_spawn_grove_1"),
        actors = listOf(
          Actor(448f, 886f), // behind pine upper boughs
          Actor(412f, 952f), // in front of pine trunk
        ),
      )
      renderScene(
        name = "tower_f1_forest_bush_close_z148",
        layout = floor1,
        isSanctuary = false,
        camX = 460f,
        camY = 1235f,
        zoom = 1.48f,
        focusProps = listOf("f1_spawn_grove_3"),
        actors = listOf(
          Actor(460f, 1238f), // behind forest bush
          Actor(460f, 1278f), // in front of forest bush
        ),
      )

      // 18-19) Tower Floor 2 (`ELDERWOOD_FOREST`) — runtime verification for `twisted.png` (`MYSTIC_PLUM_TREE`)
      //        alongside `PINE_SPIRIT_TREE`, `FOREST_CANOPY_BUSH`, and P18 `OAK_TREE`.
      renderScene(
        name = "tower_f2_twisted_plum_z088",
        layout = floor2,
        isSanctuary = false,
        camX = plumFocus.x,
        camY = plumFocus.y - 40f,
        zoom = 0.88f,
        focusProps = listOf(plumFocus.id),
      )
      renderScene(
        name = "tower_f2_twisted_plum_full_z148",
        layout = floor2,
        isSanctuary = false,
        camX = plumFocus.x,
        camY = plumFocus.y - 125f,
        zoom = 1.48f,
        focusProps = listOf(plumFocus.id),
        actors = listOf(
          Actor(plumFocus.x + 28f, plumFocus.y - 36f), // behind mystic plum canopy
          Actor(plumFocus.x - 18f, plumFocus.y + 34f), // in front of mystic plum trunk
        ),
      )

      File(outDir, "camera_props.json").writeText(JSONObject().put("captures", records).toString(2) + "\n")
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
    timeSec = 0f,
    facingNorth = a.facingNorth,
    heroName = "Lyra",
    skillAnimation = false,
    emotion = "calm",
  )
}
