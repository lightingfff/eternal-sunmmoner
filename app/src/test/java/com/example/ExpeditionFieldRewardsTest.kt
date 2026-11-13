package com.example

import com.example.data.model.HeroClassType
import com.example.data.model.PersonalityType
import com.example.game.combat.ExpeditionFieldRewards
import com.example.game.combat.ExpeditionSiteKnowledge
import com.example.game.combat.ExpeditionSiteState
import com.example.game.combat.FieldRewardKind
import com.example.game.combat.HeroCombatUnit
import com.example.game.combat.MissionStage
import org.junit.Assert.*
import org.junit.Test

class ExpeditionFieldRewardsTest {
  private fun site(id: String, state: ExpeditionSiteState = ExpeditionSiteState.RESOLVED) =
    ExpeditionSiteKnowledge(id, id, 0f, 0f, state, "")

  private fun hero(id: String = "hero", maxHp: Int = 100, hp: Int = maxHp) = HeroCombatUnit(
    id = id, name = id, title = "", heroClass = HeroClassType.KNIGHT,
    personality = PersonalityType.STOIC, maxHp = maxHp, hp = hp,
    attack = 20, defense = 10, speed = 100f, skillLevel = 1,
    x = 0f, y = 0f, anchorX = 0f, anchorY = 0f, formationIndex = 0,
  )

  @Test fun onlyAuthoredResolvedSitesUnlockAndOffersSurviveLaterObservationChanges() {
    val rewards = ExpeditionFieldRewards()
    rewards.observe(listOf(site("north", ExpeditionSiteState.UNKNOWN), site("south", ExpeditionSiteState.REVEALED), site("fake")))
    assertTrue(rewards.pendingOffers.isEmpty())
    rewards.observe(listOf(site("south")))
    rewards.observe(emptyList())
    rewards.observe(listOf(site("north"), site("east"), site("south"), site("fake")))
    assertEquals(listOf("south", "north", "east"), rewards.pendingOffers.map { it.siteId })
    assertTrue(rewards.pendingOffers.all { it.options.size == 2 })
    assertEquals(3, rewards.pendingOffers.size)
  }

  @Test fun eachSiteAllowsOneMutuallyExclusiveClaimAndRepeatedObservationCannotReopenIt() {
    val rewards = ExpeditionFieldRewards()
    val heroes = listOf(hero())
    rewards.observe(listOf(site("north")))
    assertNull(rewards.claim("south", "south_valuables", heroes, MissionStage.IN_PROGRESS))
    assertNull(rewards.claim("north", "south_valuables", heroes, MissionStage.IN_PROGRESS))
    val result = rewards.claim("north", "north_valuables", heroes, MissionStage.IN_PROGRESS)!!
    assertEquals(60L, result.gold)
    assertEquals(10L, result.mana)
    assertEquals(FieldRewardKind.VALUABLES, result.option.kind)
    assertEquals("field_north_valuables", result.markerId)
    assertTrue(result.affectedHeroIds.isEmpty())
    assertNull(rewards.claim("north", "north_valuables", heroes, MissionStage.IN_PROGRESS))
    assertNull(rewards.claim("north", "north_readiness", heroes, MissionStage.IN_PROGRESS))
    rewards.observe(listOf(site("north")))
    assertTrue(rewards.pendingOffers.isEmpty())
    assertEquals(setOf(result.markerId), rewards.claimedIds)
  }

  @Test fun allNonProgressStagesAndSquadCollapseRejectWithoutConsuming() {
    val rewards = ExpeditionFieldRewards().apply { observe(listOf(site("south"))) }
    val alive = hero()
    for (stage in MissionStage.entries.filterNot { it == MissionStage.IN_PROGRESS }) {
      assertFalse(rewards.canClaim("south", "south_ward", listOf(alive), stage))
      assertNull(rewards.claim("south", "south_valuables", listOf(alive), stage))
    }
    val knockedOut = hero("out").apply { this.knockedOut = true }
    val zeroHp = hero("zero", hp = 0)
    assertNull(rewards.claim("south", "south_valuables", emptyList(), MissionStage.IN_PROGRESS))
    assertNull(rewards.claim("south", "south_ward", listOf(knockedOut, zeroHp), MissionStage.IN_PROGRESS))
    assertEquals(1, rewards.pendingOffers.size)
    assertTrue(rewards.claimedIds.isEmpty())
    assertEquals(0, alive.shieldHp)
  }

  @Test fun readinessReducesSkillRecoveryByMaxCooldownFractionWithoutBypassingManualLock() {
    val rewards = ExpeditionFieldRewards().apply { observe(listOf(site("north"))) }
    val actor = hero("actor").apply { skillCooldown = 6f; manualCommandCooldown = 7f }
    val nearlyReady = hero("near").apply { skillCooldown = .2f; manualCommandCooldown = 4f }
    val out = hero("out").apply { skillCooldown = 6f; knockedOut = true }
    val result = rewards.claim("north", "north_readiness", listOf(actor, actor, nearlyReady, out), MissionStage.IN_PROGRESS)!!
    assertEquals(1.5f, actor.skillCooldown, .0001f)
    assertEquals(0f, nearlyReady.skillCooldown, 0f)
    assertEquals(7f, actor.manualCommandCooldown, 0f)
    assertEquals(4f, nearlyReady.manualCommandCooldown, 0f)
    assertEquals(6f, out.skillCooldown, 0f)
    assertEquals(setOf("actor", "near"), result.affectedHeroIds)
    assertEquals(0L, result.gold)
    assertEquals(0L, result.mana)
  }

  @Test fun unusableReadinessIsNotConsumedAndCanBeUsedAfterANewSkillCast() {
    val rewards = ExpeditionFieldRewards().apply { observe(listOf(site("north"))) }
    val actor = hero().apply { skillCooldown = 0f }
    assertFalse(rewards.canClaim("north", "north_readiness", listOf(actor), MissionStage.IN_PROGRESS))
    assertNull(rewards.claim("north", "north_readiness", listOf(actor), MissionStage.IN_PROGRESS))
    assertTrue(rewards.claimedIds.isEmpty())
    actor.skillCooldown = actor.maxSkillCooldown
    assertNotNull(rewards.claim("north", "north_readiness", listOf(actor), MissionStage.IN_PROGRESS))
  }

  @Test fun wardUsesQuarterMaximumHpWithoutStackingDowngradingHealingOrReviving() {
    val rewards = ExpeditionFieldRewards().apply { observe(listOf(site("south"))) }
    val weak = hero("weak", maxHp = 103, hp = 10).apply { shieldHp = 3 }
    val strong = hero("strong").apply { shieldHp = 60 }
    val dead = hero("dead", hp = 0).apply { knockedOut = true }
    val result = rewards.claim("south", "south_ward", listOf(weak, strong, dead), MissionStage.IN_PROGRESS)!!
    assertEquals(25, weak.shieldHp)
    assertEquals(60, strong.shieldHp)
    assertEquals(10, weak.hp)
    assertEquals(0, dead.hp)
    assertEquals(0, dead.shieldHp)
    assertTrue(dead.knockedOut)
    assertEquals(setOf("weak"), result.affectedHeroIds)
  }

  @Test fun wardRejectsWhenEveryLivingHeroAlreadyHasAnEqualOrStrongerShield() {
    val rewards = ExpeditionFieldRewards().apply { observe(listOf(site("south"))) }
    val actor = hero().apply { shieldHp = 25 }
    assertFalse(rewards.canClaim("south", "south_ward", listOf(actor), MissionStage.IN_PROGRESS))
    assertNull(rewards.claim("south", "south_ward", listOf(actor), MissionStage.IN_PROGRESS))
    assertEquals(1, rewards.pendingOffers.size)
    assertNotNull(rewards.claim("south", "south_valuables", listOf(actor), MissionStage.IN_PROGRESS))
  }

  @Test fun healingClampsAtMaxSkipsDeadAndDoesNotApplyTwiceForDuplicateHeroReferences() {
    val rewards = ExpeditionFieldRewards().apply { observe(listOf(site("east"))) }
    val injured = hero("injured", maxHp = 101, hp = 10)
    val almostFull = hero("near", hp = 98)
    val full = hero("full")
    val dead = hero("dead", hp = 0)
    val result = rewards.claim("east", "east_healing", listOf(injured, injured, almostFull, full, dead), MissionStage.IN_PROGRESS)!!
    assertEquals(35, injured.hp)
    assertEquals(100, almostFull.hp)
    assertEquals(100, full.hp)
    assertEquals(0, dead.hp)
    assertEquals(setOf("injured", "near"), result.affectedHeroIds)
    assertNull(rewards.claim("east", "east_healing", listOf(injured), MissionStage.IN_PROGRESS))
  }

  @Test fun healingRejectsAFullPartyButKeepsTheResourceAlternativeAvailable() {
    val rewards = ExpeditionFieldRewards().apply { observe(listOf(site("east"))) }
    val party = listOf(hero())
    assertFalse(rewards.canClaim("east", "east_healing", party, MissionStage.IN_PROGRESS))
    assertNull(rewards.claim("east", "east_healing", party, MissionStage.IN_PROGRESS))
    val payout = rewards.claim("east", "east_valuables", party, MissionStage.IN_PROGRESS)!!
    assertEquals(75L, payout.gold)
    assertEquals(20L, payout.mana)
  }

  @Test fun snapshotsStayDetachedAndNewAttemptsDoNotInheritConsumption() {
    val rewards = ExpeditionFieldRewards().apply { observe(listOf(site("north"))) }
    val oldOffers = rewards.pendingOffers
    val oldMarkers = rewards.claimedIds
    val party = listOf(hero())
    rewards.claim("north", "north_valuables", party, MissionStage.IN_PROGRESS)
    assertEquals(1, oldOffers.size)
    assertTrue(oldMarkers.isEmpty())
    assertEquals(1, rewards.claimedIds.size)
    val retry = ExpeditionFieldRewards().apply { observe(listOf(site("north"))) }
    assertEquals(1, retry.pendingOffers.size)
    assertTrue(retry.claimedIds.isEmpty())
    assertEquals(oldOffers.single(), retry.pendingOffers.single())
    assertTrue(rewards.claimedIds.all { it.length <= 36 })
  }

  @Test fun catalogIsDeterministicAndResourcePayoutsAreSiteSpecific() {
    val one = ExpeditionFieldRewards().apply { observe(listOf(site("north"), site("south"), site("east"))) }
    val two = ExpeditionFieldRewards().apply { observe(listOf(site("north"), site("south"), site("east"))) }
    assertEquals(one.pendingOffers, two.pendingOffers)
    val party = listOf(hero())
    val results = listOf("north", "south", "east").map { site ->
      one.claim(site, "${site}_valuables", party, MissionStage.IN_PROGRESS)!!
    }
    assertEquals(listOf(60L, 65L, 75L), results.map { it.gold })
    assertEquals(listOf(10L, 15L, 20L), results.map { it.mana })
    assertEquals(3, one.claimedIds.size)
    assertTrue(one.pendingOffers.isEmpty())
  }
}
