package com.example

import com.example.data.model.CourierOutcome
import com.example.data.model.HeroRecord
import com.example.game.combat.*
import com.example.game.tower.TowerFloorCatalog
import org.junit.Assert.*
import org.junit.Test

class CourierChoiceInteractionTest {
  private fun engine() = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(),
    listOf(HeroRecord(id="choice_hero",name="Kaelen")), emptyList(), enableLostCourier=true)
    .apply { stage=MissionStage.IN_PROGRESS }

  @Test fun choiceCommitsOnceAndSuppliesRequireContinuousLivingHeroPresence() {
    val e=engine();val m=e.lostCourier!!
    assertFalse(m.chooseChoice(CourierChoice.RECOVER_SUPPLIES))
    m.courier.x=1100f
    assertTrue(m.chooseChoice(CourierChoice.RECOVER_SUPPLIES))
    assertFalse(m.chooseChoice(CourierChoice.SUPPORT_COURIER))
    val h=e.heroes.first(); h.x=m.supplyX;h.y=m.supplyY
    m.beforeStep(2f);assertFalse(m.suppliesRecovered)
    h.x=600f;m.beforeStep(1f)
    h.x=m.supplyX;h.y=m.supplyY
    m.beforeStep(2f);assertFalse(m.suppliesRecovered)
    m.beforeStep(1.1f);assertTrue(m.suppliesRecovered)
    m.courier.hp=0;m.afterStep()
    assertEquals(CourierOutcome.FAILED,m.outcome)
    assertNull(engine().lostCourier!!.choice)
    assertFalse(engine().lostCourier!!.suppliesRecovered)
  }
}
