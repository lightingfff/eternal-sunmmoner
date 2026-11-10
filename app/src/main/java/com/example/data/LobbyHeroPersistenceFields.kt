package com.example.data

import com.example.data.model.*

/** Same additive fields for guest→cloud import and transaction writes. */
object LobbyHeroPersistenceFields {
  fun fields(hero: HeroRecord): Map<String, Any> = mapOf(
    "currentHp" to hero.currentHp,
    "sanityState" to HeroSanityState.fromRaw(hero.sanityState).name,
    "currentAssignment" to HeroData.fromRecord(hero).currentAssignment.name,
    "partyJoinedOrder" to hero.partyJoinedOrder,
    "restStartedAtEpochSec" to hero.restStartedAtEpochSec,
    "personality" to hero.personality,
    "hiddenTrait" to traitFields(hero.hiddenTrait),
  )
  fun traitFields(trait: HeroHiddenTrait): Map<String, Any> = mapOf(
    "id" to trait.id, "name" to trait.name, "minimumLevel" to trait.minimumLevel,
    "minimumMorale" to trait.minimumMorale, "requiresSelfAwareness" to trait.requiresSelfAwareness, "unlocked" to trait.unlocked)

}
