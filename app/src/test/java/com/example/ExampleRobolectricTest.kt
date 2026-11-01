package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.HeroClassType
import com.example.data.model.HeroRecord
import com.example.data.model.PersonalityType
import com.example.data.model.TacticalOrder
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.tower.TowerFloorCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app_name matches Eternal Summoner`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Eternal Summoner", appName)
  }

  @Test
  fun `tower combat engine executes tactical orders and autonomous hero AI`() {
    val floor01 = TowerFloorCatalog.FLOORS.first()
    val squad = listOf(
      HeroRecord(
        id = "h_knight",
        name = "Kaelen",
        title = "Khiên Thép",
        heroClass = HeroClassType.KNIGHT.name,
        personality = PersonalityType.STOIC.name,
        maxHp = 480,
        attack = 46,
        defense = 28,
        speed = 105,
        inActiveSquad = true,
      ),
      HeroRecord(
        id = "h_archer",
        name = "Lyra",
        title = "Mắt Ưng",
        heroClass = HeroClassType.ARCHER.name,
        personality = PersonalityType.CYNICAL.name,
        maxHp = 330,
        attack = 54,
        defense = 16,
        speed = 132,
        inActiveSquad = true,
      ),
      HeroRecord(
        id = "h_mage",
        name = "Vesper",
        title = "Tinh Vân",
        heroClass = HeroClassType.MAGE.name,
        personality = PersonalityType.CALCULATING.name,
        maxHp = 310,
        attack = 60,
        defense = 14,
        speed = 112,
        inActiveSquad = true,
      ),
    )

    val engine = TowerCombatEngine(
      floorSpec = floor01,
      squadRecords = squad,
      equipment = emptyList(),
    )
    engine.stage = MissionStage.IN_PROGRESS

    // Verify all 8 tactical orders transition cleanly
    TacticalOrder.entries.forEach { order ->
      engine.issueTacticalOrder(order)
      assertEquals(order, engine.activeOrder)
    }

    // Verify Move-To battlefield tap places a valid collision-resolved beacon
    engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
    engine.onBattlefieldTapped(920f, 960f)
    assertNotNull(engine.moveBeacon)

    // Step simulation forward and verify heroes autonomously move toward the tactical beacon
    val startKnightX = engine.heroes.first().x
    repeat(30) {
      engine.step(0.05f, 1f, 0f)
    }
    assertTrue("Knight should advance during real-time combat step", engine.heroes.first().x > startKnightX)
  }
}
