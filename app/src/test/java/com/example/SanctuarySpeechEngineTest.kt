package com.example

import com.example.data.model.ContextAwareDialogueDatabase
import com.example.data.model.FacilityType
import com.example.data.model.HeroRecord
import com.example.game.lobby.HeroTaskType
import com.example.game.lobby.LobbyActivityState
import com.example.game.lobby.LobbyHeroActor
import com.example.game.lobby.LobbyLocomotion
import com.example.game.lobby.SanctuaryLobbyEngine
import com.example.game.tower.TowerFloorCatalog
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class SanctuarySpeechEngineTest {
  private fun hero(id: String = "hero", rarity: Long = 2, hp: Long = 100, party: Boolean = false) = HeroRecord(
    id = id, name = id, rarity = rarity, maxHp = 100, currentHp = hp, morale = 50,
    speed = 100, personality = "LOYAL", inActiveSquad = party,
  )

  private fun actor(engine: SanctuaryLobbyEngine, id: String = "hero") = engine.heroActors.first { it.heroId == id }

  private fun park(actor: LobbyHeroActor, x: Float = 1800f, y: Float = 1200f) {
    actor.x = x; actor.y = y
    actor.targetX = x; actor.targetY = y
    actor.lastProgressX = x; actor.lastProgressY = y
    actor.waypointQueue.clear()
    actor.isMoving = false
    actor.waitTimer = 1000f
    actor.speechBubble = null
    actor.speechTimer = 0f
    actor.ambientSpeechCooldown = 1000f
  }

  private fun socialPair(): Triple<SanctuaryLobbyEngine, LobbyHeroActor, LobbyHeroActor> {
    val engine = SanctuaryLobbyEngine(Random(41))
    engine.syncHeroes(listOf(hero("high", 4), hero("low", 1)), 1000)
    val high = actor(engine, "high")
    val low = actor(engine, "low")
    park(high); park(low, high.x + 40f, high.y)
    return Triple(engine, high, low)
  }

  @Test fun pickedUpBubbleExpiresAfterThreeAndHalfSecondsWhileHeroRemainsHeld() {
    val engine = SanctuaryLobbyEngine(Random(2))
    engine.syncHeroes(listOf(hero().copy(personality = "COWARD")), 1000)
    val held = actor(engine)
    park(held)
    assertTrue(engine.beginHeroPickUp(held.heroId))
    val phrase = "Á! Đừng ném tôi vào lò dung hợp mà Master! Tôi xin ngài!"
    assertEquals(phrase, held.speechBubble)
    repeat(3) { engine.step(1f) }
    assertEquals(phrase, held.speechBubble)
    assertEquals(.5f, held.speechTimer, .0001f)
    engine.step(.5f)
    assertNull(held.speechBubble)
    assertEquals(0f, held.speechTimer, .0001f)
    engine.step(12f)
    assertNull(held.speechBubble)
    assertEquals(held.heroId, engine.pickedHeroId)
    assertEquals(LobbyLocomotion.BEING_PICKED_UP, held.locomotion)
    assertEquals(1800f, held.x, .0001f)
  }

  @Test fun tappingSpeaksImmediatelyAndRefreshesTheCompleteThreeAndHalfSecondLifetime() {
    val engine = SanctuaryLobbyEngine(Random(3))
    engine.syncHeroes(listOf(hero()), 1000)
    val resident = actor(engine)
    park(resident)
    assertEquals(engine.triggerHeroTapSpeech(resident.heroId), resident.speechBubble)
    assertNotNull(resident.speechBubble)
    engine.step(2f)
    engine.triggerHeroTapSpeech(resident.heroId)
    assertEquals(3.5f, resident.speechTimer, .0001f)
    engine.step(3f)
    assertNotNull(resident.speechBubble)
    engine.step(.5f)
    assertNull(resident.speechBubble)
  }

  @Test fun ambientSpeechWaitsSixIdleSecondsAndReschedulesWithinSixToTenSeconds() {
    val engine = SanctuaryLobbyEngine(Random(4))
    engine.syncHeroes(listOf(hero()), 1000)
    val resident = actor(engine)
    park(resident)
    resident.ambientSpeechCooldown = 6f
    repeat(5) { engine.step(1f) }
    assertNull(resident.speechBubble)
    engine.step(1f)
    assertNotNull(resident.speechBubble)
    assertEquals(3.5f, resident.speechTimer, .0001f)
    assertTrue(resident.ambientSpeechCooldown in 6f..10f)
    val intervals = mutableSetOf<Float>()
    repeat(10) {
      resident.speechBubble = null; resident.speechTimer = 0f
      resident.ambientSpeechCooldown = 0f
      engine.step(.01f)
      assertNotNull(resident.speechBubble)
      assertTrue(resident.ambientSpeechCooldown in 6f..10f)
      intervals += resident.ambientSpeechCooldown
    }
    assertTrue(intervals.size > 1)
  }

  @Test fun movingTrainingPartyAndPlayerHoldDoNotConsumeIdleSpeechInterval() {
    val moving = SanctuaryLobbyEngine(Random(5))
    moving.syncHeroes(listOf(hero()), 1000)
    val traveler = actor(moving)
    park(traveler)
    traveler.targetX += 200f
    traveler.ambientSpeechCooldown = 7f
    moving.step(.25f)
    assertTrue(traveler.isMoving)
    assertEquals(7f, traveler.ambientSpeechCooldown, .0001f)
    assertNull(traveler.speechBubble)

    for (task in listOf(HeroTaskType.TRAIN, HeroTaskType.PREPARE_TOWER)) {
      val tasked = SanctuaryLobbyEngine(Random(6))
      tasked.syncHeroes(listOf(hero()), 1000)
      val busy = actor(tasked)
      park(busy)
      tasked.commandHeroTask(busy.heroId, task)
      park(busy)
      busy.ambientSpeechCooldown = 0f
      tasked.step(.25f)
      assertNull(busy.speechBubble)
    }

    val party = SanctuaryLobbyEngine(Random(7))
    party.syncHeroes(listOf(hero(party = true)), 1000)
    val member = actor(party)
    park(member)
    member.ambientSpeechCooldown = 0f
    party.step(.25f)
    assertNull(member.speechBubble)

    moving.commandHeroHold(traveler.heroId)
    park(traveler)
    traveler.ambientSpeechCooldown = 0f
    moving.step(.25f)
    assertNull(traveler.speechBubble)
  }

  @Test fun lowHpHouseDialogueUsesTheSameProximityForTapAndAutomaticTrigger() {
    val house = TowerFloorCatalog.SANCTUARY_ZONES.first { it.facility == FacilityType.HERO_QUARTERS }
    val engine = SanctuaryLobbyEngine(Random(8))
    engine.syncHeroes(listOf(hero(hp = 39)), 1000)
    val wounded = actor(engine)
    park(wounded, house.centerX, house.centerY + house.interactRadius * 1.4f)
    engine.step(.01f)
    assertEquals(ContextAwareDialogueDatabase.lineForLowHpNearHouse(), wounded.speechBubble)
    assertEquals(ContextAwareDialogueDatabase.lineForLowHpNearHouse(), engine.triggerHeroTapSpeech(wounded.heroId))

    park(wounded, house.centerX + house.interactRadius * 1.5f, house.centerY)
    wounded.lowHpHouseSpeechCooldown = 0f
    engine.step(.01f)
    assertNull(wounded.speechBubble)
    assertNotEquals(ContextAwareDialogueDatabase.lineForLowHpNearHouse(), engine.triggerHeroTapSpeech(wounded.heroId))

    engine.syncHeroes(listOf(hero(hp = 40)), 1000)
    park(wounded, house.centerX, house.centerY + house.interactRadius * 1.4f)
    wounded.lowHpHouseSpeechCooldown = 0f
    engine.step(.01f)
    assertNull(wounded.speechBubble)
  }

  @Test fun socialDisdainPenalizesOncePerPassAndDoesNotRepeatWhileStandingNearby() {
    val (engine, high, low) = socialPair()
    engine.step(.01f)
    assertEquals("Tránh đường ra đồ rác rưởi 1 sao!", high.speechBubble)
    assertEquals(48, low.lobbyData?.morale)
    assertEquals(listOf("low"), engine.consumePendingDisdainPenaltyHeroIds())
    repeat(20) { engine.step(1f) }
    assertEquals(48, low.lobbyData?.morale)
    assertTrue(engine.consumePendingDisdainPenaltyHeroIds().isEmpty())

    park(low, high.x + 205f, high.y)
    engine.step(.01f)
    park(low, high.x + 40f, high.y)
    engine.step(.01f)
    assertEquals(46, low.lobbyData?.morale)
    assertEquals(listOf("low"), engine.consumePendingDisdainPenaltyHeroIds())
    assertTrue(engine.consumePendingDisdainPenaltyHeroIds().isEmpty())
  }

  @Test fun proximityHysteresisAndCooldownPreventBoundaryJitterAndRapidReentryPenalties() {
    val (engine, high, low) = socialPair()
    engine.step(.01f)
    engine.consumePendingDisdainPenaltyHeroIds()
    park(low, high.x + 180f, high.y)
    repeat(12) { engine.step(1f) }
    park(low, high.x + 160f, high.y)
    engine.step(.01f)
    assertEquals(48, low.lobbyData?.morale)
    assertTrue(engine.consumePendingDisdainPenaltyHeroIds().isEmpty())

    park(low, high.x + 210f, high.y)
    engine.step(.01f)
    park(low, high.x + 160f, high.y)
    engine.step(.01f)
    assertEquals(46, low.lobbyData?.morale)
    engine.consumePendingDisdainPenaltyHeroIds()
    park(low, high.x + 210f, high.y)
    engine.step(.01f)
    park(low, high.x + 160f, high.y)
    engine.step(.01f)
    assertEquals(46, low.lobbyData?.morale)
    assertTrue(engine.consumePendingDisdainPenaltyHeroIds().isEmpty())
  }

  @Test fun playerDialogueAndPickedUpHeroesCannotBeInterruptedBySocialDisdain() {
    val (engine, high, low) = socialPair()
    val tapLine = engine.triggerHeroTapSpeech(high.heroId)
    engine.step(1f)
    assertEquals(tapLine, high.speechBubble)
    assertEquals(50, low.lobbyData?.morale)
    assertTrue(engine.consumePendingDisdainPenaltyHeroIds().isEmpty())
    assertTrue(engine.beginHeroPickUp(low.heroId))
    val heldLine = low.speechBubble
    engine.step(1f)
    assertEquals(heldLine, low.speechBubble)
    assertEquals(50, low.lobbyData?.morale)
    assertTrue(engine.consumePendingDisdainPenaltyHeroIds().isEmpty())
  }

  @Test fun routineConversationCannotOverwriteTapDialogue() {
    val seed = (0..1000).first {
      val random = Random(it)
      random.nextInt(100); random.nextFloat()
      random.nextInt(100) > 80
    }
    val engine = SanctuaryLobbyEngine(Random(seed))
    engine.syncHeroes(listOf(hero("speaker"), hero("listener")), 1000)
    val speaker = actor(engine, "speaker")
    val listener = actor(engine, "listener")
    park(speaker); park(listener, speaker.x + 50f, speaker.y)
    val tapLine = engine.triggerHeroTapSpeech(speaker.heroId)
    speaker.waitTimer = 0f
    engine.step(.01f)
    assertEquals(LobbyActivityState.CHATTING, speaker.activity)
    assertEquals(tapLine, speaker.speechBubble)
    assertEquals(3.49f, speaker.speechTimer, .0001f)
  }

  @Test fun ownedIndoorSpeechUsesHeroContextAndExpiresAfterThreeAndHalfSeconds() {
    val engine = SanctuaryLobbyEngine(Random(9))
    engine.syncHeroes(listOf(hero().copy(status = "RESTING", personality = "COWARD")), 1000)
    engine.enterFacilityInterior(FacilityType.HERO_QUARTERS)
    val resident = actor(engine)
    assertNotNull(resident.lobbyData)
    assertEquals(3.5f, resident.speechTimer, .0001f)
    engine.step(3.5f)
    assertNull(resident.speechBubble)
    assertNotNull(engine.triggerHeroTapSpeech(resident.heroId))
    assertEquals(3.5f, resident.speechTimer, .0001f)
  }
}
