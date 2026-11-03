package com.example

import com.example.data.model.HeroRecord
import com.example.data.model.PersonalityType
import com.example.data.model.TacticalOrder
import com.example.game.combat.*
import com.example.game.tower.TowerFloorCatalog
import org.junit.Assert.*
import org.junit.Test

class HeroAgencyTest {
  @Test fun fearProducesContextualRequestAndRegroupHelpsDifferentlyWithoutSpam() {
    val records=listOf(
      HeroRecord(id="stoic",name="Canh",personality=PersonalityType.STOIC.name),
      HeroRecord(id="reckless",name="Lua",personality=PersonalityType.RECKLESS.name))
    val engine=TowerCombatEngine(TowerFloorCatalog.FLOORS.first(),records,emptyList(),enableLostCourier=true)
    engine.stage=MissionStage.IN_PROGRESS
    engine.enemies.clear()
    val stoic=engine.heroes[0];val reckless=engine.heroes[1]
    stoic.fearSecondsRemaining=10f;reckless.fearSecondsRemaining=10f
    engine.step(.01f,0f,0f)
    assertEquals("stoic",engine.heroAgencyAlert?.heroId)
    assertTrue(engine.heroAgencyAlert!!.messageVi.contains("rút lui"))
    val before=stoic.fearSecondsRemaining
    engine.issueTacticalOrder(TacticalOrder.REGROUP)
    assertEquals(before-2.5f,stoic.fearSecondsRemaining,.001f)
    engine.step(.01f,0f,0f)
    assertEquals("reckless",engine.heroAgencyAlert?.heroId)
    val recklessBefore=reckless.fearSecondsRemaining
    engine.issueTacticalOrder(TacticalOrder.PROTECT_ALLY)
    assertEquals(recklessBefore-1f,reckless.fearSecondsRemaining,.001f)
    val after=reckless.fearSecondsRemaining
    engine.issueTacticalOrder(TacticalOrder.PROTECT_ALLY)
    assertEquals("Repeated commands cannot clear an episode instantly",after,reckless.fearSecondsRemaining,.001f)
    assertNull(engine.heroAgencyAlert)
  }
  @Test fun fearRequestCanBeDismissedWithoutChangingHeroStateOrOrder() {
    val engine=TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), listOf(HeroRecord(id="hero",name="Hero")), emptyList(),enableLostCourier=true)
    engine.stage=MissionStage.IN_PROGRESS;engine.enemies.clear()
    val hero=engine.heroes.single();hero.fearSecondsRemaining=8f
    engine.step(.01f,0f,0f)
    val remaining=hero.fearSecondsRemaining; val order=engine.activeOrder
    assertNotNull(engine.heroAgencyAlert)
    engine.dismissHeroAgencyAlert()
    assertNull(engine.heroAgencyAlert)
    assertEquals(remaining,hero.fearSecondsRemaining,.001f)
    assertEquals(order,engine.activeOrder)
    engine.step(.01f,0f,0f)
    assertNull("Same episode does not repeatedly interrupt",engine.heroAgencyAlert)
  }
}
