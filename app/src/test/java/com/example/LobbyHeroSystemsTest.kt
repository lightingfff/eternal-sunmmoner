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
    val advanced = hero().copy(level = 10, survivedExpeditions = 3)
    val revealed = LobbyHeroAssignments.assign(listOf(advanced), "h", HeroAssignment.IN_PARTY, 1000).single()
    assertTrue(revealed.hiddenTrait.unlocked)
    assertTrue(HeroHiddenTrait.AWAKENED_TRAIT_POOL.any { it.first == revealed.hiddenTrait.id && it.second == revealed.hiddenTrait.displayName })
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
    assertEquals(10, HeroData.fromRecord(resting, 1060).currentHP)
    assertEquals(10, HeroData.fromRecord(resting, 1120).currentHP)
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

  @Test fun contextAwareDialogueDatabaseProvidesAtLeast25LinesAndExactContextLines() {
    assertTrue("Must contain at least 25 distinct lines", ContextAwareDialogueDatabase.allCatalogDialogues().distinct().size >= 25)
    val coward = HeroData.fromRecord(hero("c").copy(personality = "COWARD", sanityState = "NORMAL"))
    val arrogant = HeroData.fromRecord(hero("a").copy(personality = "ARROGANT", sanityState = "NORMAL"))
    val selfAware = HeroData.fromRecord(hero("s").copy(personality = "STOIC", sanityState = "SELF_AWARE"))
    assertEquals(
      "Á! Đừng ném tôi vào lò dung hợp mà Master! Tôi xin ngài!",
      ContextAwareDialogueDatabase.lineForPickedUp(coward),
    )
    assertEquals(
      "Bỏ cái ngón tay bẩn thỉu của ngươi ra khỏi người ta!",
      ContextAwareDialogueDatabase.lineForPickedUp(arrogant),
    )
    assertEquals(
      "Lại định sắp xếp đội hình đấy à? Chọn người cho khôn ngoan vào.",
      ContextAwareDialogueDatabase.lineForPickedUp(selfAware),
    )
    val lowMorale = HeroData.fromRecord(hero("m1", morale = 20))
    val highMorale = HeroData.fromRecord(hero("m2", morale = 80))
    assertEquals(
      "Không... tầng trước đám đồng đội đã chết sạch rồi, tôi không muốn vào đó!",
      ContextAwareDialogueDatabase.lineForPartyAssignment(lowMorale),
    )
    assertEquals(
      "Cứ giao vị trí tiên phong cho tôi.",
      ContextAwareDialogueDatabase.lineForPartyAssignment(highMorale),
    )
    assertEquals(
      "Vết thương rỉ máu mãi không ngừng... làm ơn cho tôi vào Nhà nghỉ ngơi...",
      ContextAwareDialogueDatabase.lineForLowHpNearHouse(),
    )
    val star4 = HeroData.fromRecord(hero("s4").copy(rarity = 4))
    val star1 = HeroData.fromRecord(hero("s1").copy(rarity = 1))
    assertEquals(
      "Tránh đường ra đồ rác rưởi 1 sao!",
      ContextAwareDialogueDatabase.lineForHighStarDisdain(star4, star1),
    )
  }

  @Test fun socialProximityTriggerDeductsTwoMoraleFromLowStarHeroAndShowsDisdainBubble() {
    val lobby = SanctuaryLobbyEngine()
    val highStar = hero("high", morale = 90).copy(rarity = 4, personality = "ARROGANT")
    val lowStar = hero("low", morale = 50).copy(rarity = 1, personality = "COWARD")
    lobby.syncHeroes(listOf(highStar, lowStar), 1000)
    val highActor = lobby.heroActors.first { it.heroId == "high" }
    val lowActor = lobby.heroActors.first { it.heroId == "low" }
    highActor.x = 1600f; highActor.y = 1200f; highActor.targetX = 1600f; highActor.targetY = 1200f
    lowActor.x = 1640f; lowActor.y = 1200f; lowActor.targetX = 1640f; lowActor.targetY = 1200f
    lobby.step(1f / 60f)
    assertEquals("Tránh đường ra đồ rác rưởi 1 sao!", highActor.speechBubble)
    assertEquals(48, lowActor.lobbyData?.morale)
    assertEquals(listOf("low"), lobby.consumePendingDisdainPenaltyHeroIds())
  }

  @Test fun sanctuaryBlindAppraisalInfirmaryAndCruelSynthesisMechanicsWorkCompletely() {
    val summaryLowHp = LobbyHeroAssignments.recordExpeditionSurvival("", survivedThisRun = true, returnedUnder15PctHp = true)
    assertTrue(LobbyHeroAssignments.hasReturnedUnder15PctHp(summaryLowHp))
    val lowStar = hero("low1", hp = 12, morale = 40).copy(rarity = 1, level = 1, bondSummary = summaryLowHp, hiddenTrait = HeroHiddenTrait(unlocked = false))
    val unlockedByLowHp = LobbyHeroAssignments.checkAndUnlockHiddenTrait(lowStar, 1000L)
    assertTrue(unlockedByLowHp.hiddenTrait.unlocked)
    assertTrue(HeroHiddenTrait.AWAKENED_TRAIT_POOL.any { it.second == unlockedByLowHp.hiddenTrait.name })

    var summary3Runs = ""
    repeat(3) { summary3Runs = LobbyHeroAssignments.recordExpeditionSurvival(summary3Runs, survivedThisRun = true, returnedUnder15PctHp = false) }
    assertEquals(3, LobbyHeroAssignments.survivedExpeditionCount(summary3Runs))
    val unlockedBy3Runs = LobbyHeroAssignments.checkAndUnlockHiddenTrait(
      hero("low2").copy(rarity = 2, level = 2, bondSummary = summary3Runs, hiddenTrait = HeroHiddenTrait(unlocked = false)),
      1000L,
    )
    assertTrue(unlockedBy3Runs.hiddenTrait.unlocked)

    // Infirmary Heal (+50% MaxHP) & Hearty Meal (+40 Morale & clears PANICKED)
    val initialProfile = MasterProfile(id = "p1", gold = 200L)
    val injured = hero("inj", hp = 30, morale = 20).copy(maxHp = 200, currentHp = 40, sanityState = "PANICKED")
    val (profileAfterHeal, healedList) = LobbyHeroAssignments.healHeroAtInfirmary(initialProfile, listOf(injured), "inj", now = 1000L)
    assertEquals(120L, profileAfterHeal.gold)
    assertEquals(140L, healedList.single().currentHp)

    val (profileAfterMeal, fedList) = LobbyHeroAssignments.serveHeartyMealToHero(profileAfterHeal, healedList, "inj", now = 1000L)
    assertEquals(55L, profileAfterMeal.gold)
    assertEquals(60L, fedList.single().morale)
    assertEquals("NORMAL", fedList.single().sanityState)
    assertFalse(HeroData.fromRecord(fedList.single(), 1000L).isFrightened)

    // Cruel Synthesis: Permadeath + 20% stat transfer + Coward -10 Morale + 20% Hidden Trait inheritance
    val seedForTraitInherit = (0..1000).first { kotlin.random.Random(it).nextInt(100) < 20 }
    val target = hero("target", hp = 200, morale = 85).copy(attack = 100, defense = 50, maxHp = 300, currentHp = 300, rarity = 4)
    val mat1 = hero("mat1", hp = 100, morale = 60).copy(
      attack = 50, defense = 25, maxHp = 150, rarity = 2,
      hiddenTrait = HeroHiddenTrait(id = "assassin_instinct", name = "Bản Năng Sát Thủ - Tăng 30% Crit", unlocked = true),
    )
    val cowardSurvivor = hero("coward_survivor", hp = 100, morale = 50).copy(personality = "COWARD")
    val outcome = LobbyHeroAssignments.performCruelSynthesis(
      roster = listOf(target, mat1, cowardSurvivor),
      targetHeroId = "target",
      sacrificedHeroIds = listOf("mat1"),
      random = kotlin.random.Random(seedForTraitInherit),
      now = 1000L,
    )
    assertEquals(2, outcome.updatedRoster.size)
    assertTrue(outcome.updatedRoster.none { it.id == "mat1" })
    assertEquals(110L, outcome.updatedTarget.attack)
    assertEquals(55L, outcome.updatedTarget.defense)
    assertEquals(330L, outcome.updatedTarget.maxHp)
    assertEquals("Bản Năng Sát Thủ - Tăng 30% Crit", outcome.inheritedTraitName)
    assertEquals("Bản Năng Sát Thủ - Tăng 30% Crit", outcome.updatedTarget.hiddenTrait.name)
    val updatedCoward = outcome.updatedRoster.first { it.id == "coward_survivor" }
    assertEquals(40L, updatedCoward.morale)
  }
}


