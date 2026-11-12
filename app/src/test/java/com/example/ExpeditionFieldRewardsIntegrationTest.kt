package com.example

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.example.data.GuestSaveSnapshot
import com.example.data.local.LocalGuestDataSource
import com.example.data.local.LocalGuestSaveManager
import com.example.data.model.CourierOutcome
import com.example.data.model.TacticalOrder
import com.example.game.combat.CombatFxStyle
import com.example.game.combat.CourierChoice
import com.example.game.combat.CourierOrder
import com.example.game.combat.CourierTelegraph
import com.example.game.combat.ExpeditionSiteState
import com.example.game.combat.FieldRewardKind
import com.example.game.combat.MissionStage
import com.example.game.combat.TowerCombatEngine
import com.example.game.tower.TowerFloorCatalog
import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlin.math.hypot
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Public-command starter-party paths; terminal/damage fixtures are identified separately. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExpeditionFieldRewardsIntegrationTest {
  private data class Fixture(
    val context: Context,
    val manager: LocalGuestSaveManager,
    val initial: GuestSaveSnapshot,
    val engine: TowerCombatEngine,
  )

  private suspend fun fixture(): Fixture {
    val base = ApplicationProvider.getApplicationContext<Context>()
    val files = Files.createTempDirectory(base.cacheDir.toPath(), "field-rewards-").toFile()
    val context = object : ContextWrapper(base) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = files
    }
    val manager = LocalGuestSaveManager(context)
    val initial = manager.startOrResumeGuestSession()
    val party = initial.heroes.filter { it.inActiveSquad }
    assertEquals(3, party.size)
    assertTrue(party.all { it.level == 1L })
    val engine = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), party, initial.equipment,
      enableLostCourier = true, enableLivingExploration = true, random = Random(91))
    engine.stage = MissionStage.IN_PROGRESS
    return Fixture(context, manager, initial, engine)
  }

  private fun useAvailableSkills(engine: TowerCombatEngine) {
    for (hero in engine.heroes) {
      if (!hero.knockedOut && engine.enemies.any { !it.dead && hypot(it.x - hero.x, it.y - hero.y) < 400f })
        engine.triggerHeroManualSkill(hero.id)
    }
  }

  private fun navigationState(engine: TowerCombatEngine) = listOf(
    engine.activeOrder, engine.designatedTargetId, engine.selectedHeroId,
    engine.lostCourier!!.livingExpedition!!.automationEnabled,
    engine.heroes.map { it.x to it.y },
  )

  @Test fun freshAttemptCannotClaimUnknownUnrevealedOrUnresolvedSupplies() = runTest {
    val engine = fixture().engine
    assertTrue(engine.fieldRewardOffers.isEmpty())
    assertTrue(engine.discoveredNodeIds().none { it.startsWith("field_") })
    for ((site, option) in listOf("north" to "north_valuables", "south" to "south_ward",
      "east" to "east_healing", "unknown" to "unknown_valuables")) {
      assertFalse(engine.canClaimFieldReward(site, option))
      assertFalse(engine.claimFieldReward(site, option))
    }
    engine.step(1f / 60f, 0f, 0f)
    assertTrue(engine.lostCourier!!.livingExpedition!!.sites.none { it.state == ExpeditionSiteState.RESOLVED })
    assertTrue(engine.fieldRewardOffers.isEmpty())
    assertEquals(0L, engine.bonusDiscoveryGold)
    assertEquals(0L, engine.bonusDiscoveryMana)
  }

  @Test fun actualNorthEastRunClaimsOncePreservesOrdersAndPersistsThePhysicalExtractionBonuses() = runTest {
    val f = fixture()
    val engine = f.engine
    val mission = engine.lostCourier!!
    val expedition = mission.livingExpedition!!
    assertTrue(engine.resumeLivingExploration("north"))
    val claimed = linkedSetOf<String>()
    var seconds = 0f
    var northDecisionObserved = false
    var eastDecisionObserved = false
    var escortStarted = false
    var intercepted = false
    var protectedAfterScout = false
    var telegraphs = 0
    var counters = 0
    var walkingSeconds = 0f
    var combatSeconds = 0f
    var dodge: CourierTelegraph? = null

    fun protectEscort() {
      if (engine.activeOrder != TacticalOrder.PROTECT_ALLY) engine.chooseCourierOrder(CourierOrder.PROTECT)
    }

    while (seconds < 900f && mission.outcome == null) {
      engine.step(1f / 60f, 0f, 0f)
      seconds += 1f / 60f
      if (engine.heroes.any { !it.knockedOut && it.isMoving }) walkingSeconds += 1f / 60f
      if (engine.enemies.any { !it.dead && it.aggroed }) combatSeconds += 1f / 60f
      useAvailableSkills(engine)
      val resolved = expedition.sites.filter { it.state == ExpeditionSiteState.RESOLVED }.map { it.id }.toSet()
      assertTrue("Only physically resolved sites may produce field offers", engine.fieldRewardOffers.all { it.siteId in resolved })
      if (mission.outcome != null) break

      for (site in listOf("north", "east")) {
        if (site !in resolved || site in claimed) continue
        assertEquals(site, engine.fieldRewardOffers.single().siteId)
        if (site == "north") {
          engine.issueTacticalOrder(TacticalOrder.HOLD_POSITION)
          assertFalse(expedition.automationEnabled)
        } else assertTrue("Resource collection must also preserve active autonomous investigation", expedition.automationEnabled)
        val before = navigationState(engine)
        val goldBefore = engine.bonusDiscoveryGold
        val manaBefore = engine.bonusDiscoveryMana
        val option = engine.fieldRewardOffers.single().options.single { it.id == "${site}_valuables" }
        assertTrue(engine.canClaimFieldReward(site, option.id))
        assertTrue(engine.claimFieldReward(site, option.id))
        assertEquals("Collecting supplies is not a movement/resume command", before, navigationState(engine))
        assertEquals(goldBefore + option.gold, engine.bonusDiscoveryGold)
        assertEquals(manaBefore + option.mana, engine.bonusDiscoveryMana)
        val afterClaimIds = engine.discoveredNodeIds()
        assertTrue("field_${site}_valuables" in afterClaimIds)
        assertFalse(engine.claimFieldReward(site, option.id))
        assertFalse(engine.claimFieldReward(site, if (site == "north") "north_readiness" else "east_healing"))
        assertEquals(goldBefore + option.gold, engine.bonusDiscoveryGold)
        assertEquals(manaBefore + option.mana, engine.bonusDiscoveryMana)
        assertEquals(afterClaimIds, engine.discoveredNodeIds())
        claimed += site
        if (site == "north") assertTrue(engine.resumeLivingExploration("east"))
      }

      if (expedition.active) {
        val decision = expedition.decision
        val optionId = when (decision?.id) {
          "north_approach" -> {
            northDecisionObserved = true
            assertFalse(engine.fieldRewardOffers.any { it.siteId == "north" })
            "north_scout"
          }
          "east_gate" -> {
            eastDecisionObserved = true
            assertFalse(engine.fieldRewardOffers.any { it.siteId == "east" })
            "east_cooperate"
          }
          "extraction" -> if (resolved.size >= 2) "evacuate" else null
          else -> null
        }
        if (optionId != null && decision!!.options.any { it.id == optionId && it.enabled }) {
          if (optionId == "evacuate") {
            assertEquals(setOf("north", "east"), claimed)
            assertNull(mission.outcome)
            assertTrue("Selecting extraction must still leave physical escort gameplay", mission.progress < .99f)
          }
          if (engine.chooseLivingExpedition(optionId) && optionId == "evacuate") escortStarted = true
        }
        continue
      }

      assertTrue(escortStarted)
      if (mission.choiceAvailable) assertTrue(mission.chooseChoice(CourierChoice.SUPPORT_COURIER))
      if (!intercepted && mission.scoutWarningRemaining > 0f) {
        engine.chooseCourierOrder(CourierOrder.INTERCEPT)
        intercepted = true
      }
      if (intercepted && !protectedAfterScout && mission.scoutWarningRemaining <= 0f) {
        protectedAfterScout = true
        protectEscort()
      }
      val cue = mission.eliteTelegraph
      if (cue != null && cue !== dodge) {
        engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
        engine.onBattlefieldTapped(cue.x - 220f, cue.y)
        dodge = cue
        telegraphs++
      }
      if (cue == null && dodge != null && mission.eliteRecoveryRemaining > 0f) {
        engine.enemies.firstOrNull { it.id == mission.eliteEnemyId && !it.dead }?.let {
          engine.onBattlefieldTapped(it.x, it.y)
          engine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE)
          counters++
        }
        dodge = null
      }
      if (mission.eliteEnemyId != null && engine.enemies.any { it.id == mission.eliteEnemyId && it.dead }) protectEscort()
    }

    println("FIELD_REWARD_ACTUAL_SIMULATION route=north_east seconds=$seconds walking=$walkingSeconds combat=$combatSeconds outcome=${mission.outcome} claimed=$claimed")
    assertTrue(northDecisionObserved && eastDecisionObserved)
    assertEquals(CourierOutcome.RESCUED, mission.outcome)
    assertEquals(MissionStage.VICTORY, engine.stage)
    assertTrue(escortStarted && intercepted && protectedAfterScout)
    assertTrue(telegraphs > 0 && counters > 0)
    assertFalse(mission.scoutCalledReinforcements)
    assertTrue(walkingSeconds > 0f && combatSeconds > 0f)
    assertTrue(mission.courier.hp > 0)
    assertTrue(hypot(mission.courier.x - 2860f, mission.courier.y - 1066f) <= 8.1f)
    assertEquals(setOf("field_north_valuables", "field_east_valuables"), engine.discoveredNodeIds().filter { it.startsWith("field_") }.toSet())
    assertTrue(expedition.sites.single { it.id == "south" }.state != ExpeditionSiteState.RESOLVED)
    assertTrue(engine.fieldRewardOffers.isEmpty())
    assertFalse(engine.claimFieldReward("south", "south_valuables"))
    val attemptId = UUID.randomUUID().toString()
    val source = LocalGuestDataSource(f.manager)
    val party = f.initial.heroes.filter { it.inActiveSquad }
    val settlement = source.settleExpedition(f.initial.profile, party, engine.fallenHeroIds(), 1, true,
      bonusGold = engine.bonusDiscoveryGold, bonusMana = engine.bonusDiscoveryMana,
      discoveredNodeIds = engine.discoveredNodeIds(), courierOutcome = mission.outcome,
      remainingHeroHpRatios = engine.remainingHeroHpRatios(), attemptId = attemptId)
    assertEquals(engine.bonusDiscoveryGold, settlement.bonusDiscoveryGold)
    assertEquals(engine.bonusDiscoveryMana, settlement.bonusDiscoveryMana)
    assertTrue(settlement.bonusDiscoveryGold >= 135L)
    assertTrue(settlement.bonusDiscoveryMana >= 30L)
    val restored = LocalGuestSaveManager(f.context).guestSnapshot.value!!
    assertEquals(f.initial.profile.gold + settlement.goldEarned, restored.profile.gold)
    assertTrue("DISC_field_north_valuables" in restored.profile.storyFlags)
    assertTrue("DISC_field_east_valuables" in restored.profile.storyFlags)
    val replay = LocalGuestDataSource(LocalGuestSaveManager(f.context)).settleExpedition(
      f.initial.profile, party, engine.fallenHeroIds(), 1, true,
      bonusGold = engine.bonusDiscoveryGold, bonusMana = engine.bonusDiscoveryMana,
      discoveredNodeIds = engine.discoveredNodeIds(), courierOutcome = mission.outcome,
      remainingHeroHpRatios = engine.remainingHeroHpRatios(), attemptId = attemptId)
    assertEquals(settlement, replay)
    assertEquals(restored, LocalGuestSaveManager(f.context).guestSnapshot.value!!)
    val retry = TowerCombatEngine(TowerFloorCatalog.FLOORS.first(), party, f.initial.equipment,
      enableLostCourier = true, enableLivingExploration = true, random = Random(91))
    retry.stage = MissionStage.IN_PROGRESS
    assertTrue(retry.fieldRewardOffers.isEmpty())
    assertTrue(retry.discoveredNodeIds().none { it.startsWith("field_") })
    assertEquals(0L, retry.bonusDiscoveryGold)
    assertEquals(0L, retry.bonusDiscoveryMana)
  }

  @Test fun actualSouthRescueUnlocksWardWhileTerminalStageFixturesRejectAndExistingDamageConsumesIt() = runTest {
    val engine = fixture().engine
    val expedition = engine.lostCourier!!.livingExpedition!!
    assertTrue(engine.resumeLivingExploration("south"))
    var seconds = 0f
    while (seconds < 400f && expedition.sites.single { it.id == "south" }.state != ExpeditionSiteState.RESOLVED) {
      engine.step(1f / 60f, 0f, 0f)
      seconds += 1f / 60f
      useAvailableSkills(engine)
      if (expedition.sites.single { it.id == "south" }.state != ExpeditionSiteState.RESOLVED)
        assertTrue(engine.fieldRewardOffers.isEmpty())
      if (expedition.decision?.id == "south_witness") assertTrue(engine.chooseLivingExpedition("south_rescue"))
    }
    assertTrue("Starter party must physically rescue the witness before the time bound", expedition.witness?.rescued == true)
    assertTrue(expedition.witness!!.alive)
    assertEquals("south", engine.fieldRewardOffers.single().siteId)
    assertTrue(engine.canClaimFieldReward("south", "south_ward"))
    // Deliberate terminal-stage fixtures; this loop does not claim actual wins or losses.
    for (stage in MissionStage.entries.filterNot { it == MissionStage.IN_PROGRESS }) {
      engine.stage = stage
      assertTrue(engine.fieldRewardOffers.isEmpty())
      assertFalse(engine.canClaimFieldReward("south", "south_ward"))
      assertFalse(engine.claimFieldReward("south", "south_ward"))
      assertFalse(engine.claimFieldReward("south", "south_valuables"))
      assertTrue(engine.discoveredNodeIds().none { it.startsWith("field_") })
    }
    engine.stage = MissionStage.IN_PROGRESS
    val hp = engine.heroes.associate { it.id to it.hp }
    assertTrue(engine.claimFieldReward("south", "south_ward"))
    assertTrue(engine.heroes.all { it.hp == hp.getValue(it.id) })
    val hero = engine.heroes.first { !it.knockedOut && it.shieldHp >= 12 }
    val shield = hero.shieldHp
    val health = hero.hp
    // Injected damage fixture uses production mitigation/absorption, not a played enemy attack.
    engine.applyDamageToHero(hero, hero.defense / 2 + 12)
    assertEquals(health, hero.hp)
    assertEquals(shield - 12, hero.shieldHp)
    assertTrue(engine.aoeEffects.any { it.style == CombatFxStyle.SHIELD_IMPACT && it.sourceId == hero.id })
    engine.applyDamageToHero(hero, hero.defense / 2 + hero.shieldHp + 7)
    assertEquals(0, hero.shieldHp)
    assertEquals(health - 7, hero.hp)
    assertFalse(engine.claimFieldReward("south", "south_valuables"))
    assertEquals(0L, engine.bonusDiscoveryGold)
    assertEquals(0L, engine.bonusDiscoveryMana)
    assertTrue("field_south_ward" in engine.discoveredNodeIds())
  }

  @Test fun actualOptionalThirdSiteSuppliesThreeDistinctToolsBeforePhysicalExtraction() = runTest {
    val engine = fixture().engine
    val mission = engine.lostCourier!!
    val expedition = mission.livingExpedition!!
    val route = listOf("north", "south", "east")
    val claims = linkedMapOf<String, FieldRewardKind>()
    val markers = linkedSetOf<String>()
    var seconds = 0f
    var choseOptionalThirdSite = false
    var escortStarted = false
    var intercepted = false
    var protectedAfterScout = false
    var dodges = 0
    var counters = 0
    var dodge: CourierTelegraph? = null
    assertTrue(engine.resumeLivingExploration(route.first()))

    fun protectEscort() {
      if (engine.activeOrder != TacticalOrder.PROTECT_ALLY) engine.chooseCourierOrder(CourierOrder.PROTECT)
    }

    while (seconds < 900f && mission.outcome == null) {
      engine.step(1f / 60f, 0f, 0f)
      seconds += 1f / 60f
      useAvailableSkills(engine)
      if (mission.outcome != null) break
      val resolved = expedition.sites.filter { it.state == ExpeditionSiteState.RESOLVED }.map { it.id }.toSet()
      assertTrue(engine.fieldRewardOffers.all { it.siteId in resolved })

      for (site in route) {
        if (site !in resolved || site in claims) continue
        val offer = engine.fieldRewardOffers.single { it.siteId == site }
        val tactical = offer.options.single { it.kind != FieldRewardKind.VALUABLES }
        val option = if (engine.canClaimFieldReward(site, tactical.id)) tactical
          else offer.options.single { it.kind == FieldRewardKind.VALUABLES }
        val actors = engine.heroes.filter { !it.knockedOut && it.hp > 0 }
        val hp = actors.associate { it.id to it.hp }
        val shield = actors.associate { it.id to it.shieldHp }
        val cooldown = actors.associate { it.id to it.skillCooldown }
        val navigation = navigationState(engine)
        val gold = engine.bonusDiscoveryGold
        val mana = engine.bonusDiscoveryMana
        assertTrue(engine.claimFieldReward(site, option.id))
        assertEquals(navigation, navigationState(engine))
        when (option.kind) {
          FieldRewardKind.READINESS -> assertTrue("Readiness must reduce an actual remaining cooldown",
            actors.any { it.skillCooldown < cooldown.getValue(it.id) })
          FieldRewardKind.WARD -> assertTrue("Ward must strengthen actual living heroes",
            actors.any { it.shieldHp > shield.getValue(it.id) })
          FieldRewardKind.HEALING -> assertTrue("Bandages must heal actual battle injuries",
            actors.any { it.hp > hp.getValue(it.id) })
          FieldRewardKind.VALUABLES -> assertTrue(option.gold > 0L || option.mana > 0L)
        }
        assertEquals(gold + option.gold, engine.bonusDiscoveryGold)
        assertEquals(mana + option.mana, engine.bonusDiscoveryMana)
        val marker = "field_${site}_${option.kind.name.lowercase(java.util.Locale.ROOT)}"
        assertTrue(marker in engine.discoveredNodeIds())
        assertTrue(markers.add(marker))
        claims[site] = option.kind
        assertFalse(engine.claimFieldReward(site, option.id))
        val next = route.getOrNull(route.indexOf(site) + 1)
        if (next != null) {
          if (site == "south") {
            assertTrue("Two sites already permit the player to leave", expedition.readyToExtract)
            assertTrue("The third site is selected instead of evacuating", expedition.decision?.id == "extraction")
            choseOptionalThirdSite = true
          }
          assertTrue(engine.resumeLivingExploration(next))
        }
      }

      if (expedition.active) {
        val decision = expedition.decision
        val optionId = when (decision?.id) {
          "north_approach" -> "north_scout"
          "south_witness" -> "south_rescue"
          "east_gate" -> "east_cooperate"
          "extraction" -> if (resolved.size == 3) "evacuate" else null
          else -> null
        }
        if (optionId != null && decision!!.options.any { it.id == optionId && it.enabled }) {
          if (optionId == "evacuate") {
            assertEquals(route, claims.keys.toList())
            assertNull(mission.outcome)
            assertTrue(mission.progress < .99f)
          }
          if (engine.chooseLivingExpedition(optionId) && optionId == "evacuate") escortStarted = true
        }
        continue
      }

      assertTrue(escortStarted)
      if (mission.choiceAvailable) assertTrue(mission.chooseChoice(CourierChoice.SUPPORT_COURIER))
      if (!intercepted && mission.scoutWarningRemaining > 0f) {
        engine.chooseCourierOrder(CourierOrder.INTERCEPT)
        intercepted = true
      }
      if (intercepted && !protectedAfterScout && mission.scoutWarningRemaining <= 0f) {
        protectedAfterScout = true
        protectEscort()
      }
      val cue = mission.eliteTelegraph
      if (cue != null && cue !== dodge) {
        engine.issueTacticalOrder(TacticalOrder.MOVE_TO)
        engine.onBattlefieldTapped(cue.x - 220f, cue.y)
        dodge = cue
        dodges++
      }
      if (cue == null && dodge != null && mission.eliteRecoveryRemaining > 0f) {
        engine.enemies.firstOrNull { it.id == mission.eliteEnemyId && !it.dead }?.let {
          engine.onBattlefieldTapped(it.x, it.y)
          engine.issueTacticalOrder(TacticalOrder.FOCUS_FIRE)
          counters++
        }
        dodge = null
      }
      if (mission.eliteEnemyId != null && engine.enemies.any { it.id == mission.eliteEnemyId && it.dead }) protectEscort()
    }

    val resolved = expedition.sites.filter { it.state == ExpeditionSiteState.RESOLVED }.map { it.id }.toSet()
    println("FIELD_REWARD_THREE_SITE_ACTUAL_SIMULATION seconds=$seconds resolved=$resolved claims=$claims outcome=${mission.outcome} courierHp=${mission.courier.hp} heroHp=${engine.heroes.map { it.id to it.hp }}")
    assertEquals("All three paths must physically resolve within the bound", route.toSet(), resolved)
    assertEquals(route, claims.keys.toList())
    assertEquals(3, markers.size)
    assertEquals(markers, engine.discoveredNodeIds().filter { it.startsWith("field_") }.toSet())
    assertTrue("Actual injuries or cooldowns must make at least one tactical reward useful",
      claims.values.any { it != FieldRewardKind.VALUABLES })
    assertTrue(choseOptionalThirdSite && escortStarted && intercepted && protectedAfterScout)
    assertTrue(dodges > 0 && counters > 0)
    assertTrue(expedition.witness?.rescued == true && expedition.witness?.alive == true)
    assertEquals("Optional exploration must not prevent the starter party completing rescue", CourierOutcome.RESCUED, mission.outcome)
    assertEquals(MissionStage.VICTORY, engine.stage)
    assertTrue(mission.courier.hp > 0)
    assertTrue(hypot(mission.courier.x - 2860f, mission.courier.y - 1066f) <= 8.1f)
    assertTrue(engine.fieldRewardOffers.isEmpty())
  }
}
