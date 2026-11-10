package com.example

import com.example.data.LobbyHeroAssignments
import com.example.data.model.*
import com.example.game.lobby.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class LobbyHeroSystemsTest {
  private fun hero(id: String = "h", hp: Long = 100, morale: Long = 100, party: Boolean = false) =
    HeroRecord(id = id, name = id, maxHp = 100, currentHp = hp, speed = 100, morale = morale, inActiveSquad = party)

  @Test fun modelValidatesBoundsAndLocksThenRevealsTheActualHiddenTrait() {
    val legacy = HeroData.fromRecord(HeroRecord(id = "old", maxHp = 420), 1000)
    assertEquals(420, legacy.currentHP); assertEquals("???", legacy.hiddenTrait.displayName)
    assertEquals(HeroSanityState.NORMAL, legacy.sanityState)
    val advanced = hero().copy(level = 10)
    val revealed = LobbyHeroAssignments.assign(listOf(advanced), "h", HeroAssignment.IN_PARTY, 1000).single()
    assertTrue(revealed.hiddenTrait.unlocked)
    assertEquals("Linh hồn bất khuất", revealed.hiddenTrait.displayName)
    assertEquals(LobbyPersonality.REBEL, HeroData.fromRecord(hero().copy(personality = "REBEL")).personality)
    assertFalse(HeroData.fromRecord(hero(hp = 30)).isLimping)
    assertEquals(50f, HeroData.fromRecord(hero(hp = 29)).movementSpeed, .001f)
    assertTrue(HeroData.fromRecord(hero(morale = 24)).isFrightened)
    assertFalse(HeroData.fromRecord(hero(morale = 25)).isFrightened)
  }

  @Test fun fifthDropEvictsOldestAndRepeatDropDoesNotEvictAnotherMember() {
    val roster = (1..5).map { hero("h$it", party = it < 5).copy(partyJoinedOrder = it.toLong()) }
    val next = LobbyHeroAssignments.assign(roster, "h5", HeroAssignment.IN_PARTY, 1000)
    assertEquals(setOf("h2", "h3", "h4", "h5"), next.filter { it.inActiveSquad }.map { it.id }.toSet())
    assertEquals(4, roster.count { it.inActiveSquad })
    assertEquals(next, LobbyHeroAssignments.assign(next, "h5", HeroAssignment.IN_PARTY, 1000))
    assertEquals(3, LobbyHeroAssignments.assign(next, "h5", HeroAssignment.RESTING, 1000).count { it.inActiveSquad })
  }

  @Test fun deadAndInjuredHeroesCannotBeSentIntoPartyAndHouseDropNeverSacrifices() {
    val injured = hero().copy(status = "INJURED", injuryRecoveryUntilEpochSec = 2000)
    assertThrows(IllegalArgumentException::class.java) { LobbyHeroAssignments.assign(listOf(injured), "h", HeroAssignment.IN_PARTY, 1000) }
    val dead = hero().copy(currentAssignment = "SACRIFICED")
    assertThrows(IllegalArgumentException::class.java) { LobbyHeroAssignments.assign(listOf(dead), "h", HeroAssignment.RESTING, 1000) }
    assertThrows(IllegalArgumentException::class.java) { LobbyHeroAssignments.assign(listOf(hero()), "h", HeroAssignment.SACRIFICED, 1000) }
    val resting = LobbyHeroAssignments.assign(listOf(hero(hp = 10)), "h", HeroAssignment.RESTING, 1000).single()
    assertEquals(60, HeroData.fromRecord(resting, 1060).currentHP)
    assertEquals(100, HeroData.fromRecord(resting, 1120).currentHP)
    assertEquals(10, HeroData.fromRecord(resting, 900).currentHP)
  }

  @Test fun lowHpHalvesActualMovementAndPickupSuspendsAiThenCancelRestoresPosition() {
    val healthy = SanctuaryLobbyEngine(); val limp = SanctuaryLobbyEngine()
    healthy.syncHeroes(listOf(hero()), 1000); limp.syncHeroes(listOf(hero(hp = 20)), 1000)
    val a = healthy.heroActors.first { it.heroId == "h" }; val b = limp.heroActors.first { it.heroId == "h" }
    val x = a.x; val y = a.y
    healthy.commandMoveHero("h", x + 120, y); limp.commandMoveHero("h", x + 120, y)
    repeat(30) { healthy.step(1f/60); limp.step(1f/60) }
    assertEquals(2f, (a.x-x)/(b.x-x), .03f)
    val origin = a.x to a.y
    assertTrue(healthy.beginHeroPickUp("h")); healthy.dragPickedHeroTo(2000f, 620f)
    repeat(120) { healthy.step(1f/60) }
    assertEquals(2000f, a.x, .01f); assertEquals(LobbyLocomotion.BEING_PICKED_UP, a.locomotion)
    assertEquals(HeroAssignment.IN_PARTY, healthy.dropRequestAt(1960f, 620f)?.assignment)
    assertNull(healthy.dropRequestAt(1400f, 1100f))
    healthy.finishHeroPickUp(false)
    assertEquals(origin.first, a.x, .01f); assertEquals(origin.second, a.y, .01f)
    assertFalse(healthy.beginHeroPickUp("npc_gatekeeper"))
  }

  @Test fun nearbyUnassignedHeroesStopAndFaceEachOtherForConversation() {
    val seed = (0..1000).first { kotlin.random.Random(it).nextInt(100) > 80 }
    val lobby = SanctuaryLobbyEngine(kotlin.random.Random(seed))
    lobby.syncHeroes(listOf(hero("a"), hero("b")),1000)
    val actors = lobby.heroActors.filter { !it.isSanctuaryKeeperNpc }
    actors.forEachIndexed { i, actor ->
      actor.x = 1800f+i*50;actor.y=1200f;actor.targetX=actor.x;actor.targetY=actor.y;actor.waitTimer=0f
    }
    lobby.step(1f/60)
    assertEquals("b", actors[0].chatPartnerId);assertEquals("a", actors[1].chatPartnerId)
    assertTrue(actors[0].facingRight);assertFalse(actors[1].facingRight)
    assertTrue(actors.all { it.activity==LobbyActivityState.CHATTING && !it.isMoving })
  }

  @Test fun heldHeroFollowsViewportEdgeScrollWithoutAssigningOrMovingOnItsOwn() {
    val lobby = SanctuaryLobbyEngine();lobby.syncHeroes(listOf(hero()),1000)
    assertTrue(lobby.beginHeroPickUp("h"));val before=lobby.camY
    repeat(120) { lobby.dragAtViewportEdge(600f,2f,1200f,720f,0f,40f,1f/60) }
    assertTrue(lobby.camY < before-400f)
    assertEquals("h",lobby.pickedHeroId)
    assertFalse(lobby.heroActors.first { it.heroId=="h" }.lobbyData!!.currentAssignment==HeroAssignment.IN_PARTY)
    lobby.finishHeroPickUp(false)
  }

  @Test fun acceptedDropDiscardsOldWanderRouteAndPickupHitTestingUsesProjectedSpriteAtAllZooms() {
    val lobby=SanctuaryLobbyEngine();lobby.syncHeroes(listOf(hero()),1000)
    val actor=lobby.heroActors.first { it.heroId=="h" }
    lobby.commandMoveHero("h",1400f,1300f);assertTrue(lobby.beginHeroPickUp("h"))
    lobby.dragPickedHeroTo(1960f,675f);lobby.finishHeroPickUp(true)
    assertEquals(actor.x,actor.targetX,.01f);assertTrue(actor.waypointQueue.isEmpty())
    val size=androidx.compose.ui.geometry.Size(1200f,720f)
    for(z in listOf(.36f,.88f,1.48f)) {
      val cx=actor.x-80;val cy=actor.y+60
      val point=androidx.compose.ui.geometry.Offset(
        com.example.game.map.ObliqueCamera.worldToScreenX(actor.x,cx,z,600f),
        com.example.game.map.ObliqueCamera.worldToScreenY(actor.y,cy,z,360f)-32*z)
      assertEquals(actor,com.example.ui.components.pickupHeroAtScreen(listOf(actor),point,cx,cy,z,size))
    }
  }

  @Test fun afraidHeroActuallyReachesSafetyWhileFourPartyMembersFormBeforeGate() {
    val afraid = SanctuaryLobbyEngine(); afraid.syncHeroes(listOf(hero(morale = 15)), 1000)
    val a = afraid.heroActors.first { it.heroId == "h" }
    val initial = hypot(a.x-1960, a.y-585)
    repeat(3600) { afraid.step(1f/60) }
    assertTrue("Must walk away from gate: ${a.x},${a.y}", hypot(a.x-1960,a.y-585) > initial + 400)
    assertEquals(LobbyActivityState.COWERING, a.activity)
    val party = SanctuaryLobbyEngine(); party.syncHeroes((0..3).map { hero("p$it", morale = 10, party = true) }, 1000)
    repeat(4200) { party.step(1f/60) }
    val members = party.heroActors.filter { !it.isSanctuaryKeeperNpc }
    assertEquals(4, members.size)
    members.forEachIndexed { i, actor ->
      assertTrue("${actor.heroId} failed to form: ${actor.x},${actor.y}", hypot(actor.x - (1890+44*i), actor.y-690) < 25)
    }
    party.syncHeroes(listOf(hero("dead").copy(currentAssignment="SACRIFICED"), hero("rest").copy(status="RESTING")),1000)
    assertTrue(party.heroActors.none { !it.isSanctuaryKeeperNpc })
  }
}
