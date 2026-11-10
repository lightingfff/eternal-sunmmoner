package com.example.data

import com.example.data.model.*

/** Pure operation reused inside the guest mutex and the cloud transaction. */
object LobbyHeroAssignments {
  const val PARTY_LIMIT = 4
  fun assign(roster: List<HeroRecord>, heroId: String, assignment: HeroAssignment, now: Long,
    replaceWhenFull: Boolean = true): List<HeroRecord> {
    require(assignment != HeroAssignment.SACRIFICED) { "Kéo-thả không được hiến tế Anh hùng." }
    val hero = requireNotNull(roster.find { it.id == heroId }) { "Anh hùng không còn trong danh sách sở hữu." }
    val data = HeroData.fromRecord(hero, now)
    require(data.currentAssignment != HeroAssignment.SACRIFICED) { "Anh hùng đã hiến tế không thể nhận nhiệm vụ." }
    if (assignment == HeroAssignment.IN_PARTY) {
      require(!hero.isCurrentlyInjured(now) && data.currentHP > 0) { "Anh hùng cần hồi phục tại Nhà trước khi vào đội." }
    }
    val party = roster.filter { it.id != heroId && it.inActiveSquad && it.currentAssignment != HeroAssignment.SACRIFICED.name }
      .sortedWith(compareBy<HeroRecord> { it.partyJoinedOrder }.thenBy { it.id })
    if (assignment == HeroAssignment.IN_PARTY && party.size >= PARTY_LIMIT) {
      require(replaceWhenFull) { "Đội hình viễn chinh tối đa 4 Anh hùng." }
    }
    val removed = if (assignment == HeroAssignment.IN_PARTY) party.take((party.size - PARTY_LIMIT + 1).coerceAtLeast(0)).map { it.id }.toSet() else emptySet()
    val nextOrder = (roster.maxOfOrNull { it.partyJoinedOrder } ?: 0L).coerceAtMost(Long.MAX_VALUE - 1) + 1
    return roster.map { h ->
      when {
        h.currentAssignment == HeroAssignment.SACRIFICED.name -> h.copy(inActiveSquad = false)
        h.id == heroId -> {
          val unlocked = h.hiddenTrait.unlocked || h.hiddenTrait.canUnlock(data.level, data.morale, data.sanityState)
          h.copy(currentAssignment = assignment.name, inActiveSquad = assignment == HeroAssignment.IN_PARTY,
            status = if (h.isCurrentlyInjured(now)) h.status else if (assignment == HeroAssignment.RESTING) HeroStatusType.RESTING.name else HeroStatusType.READY.name,
            currentHp = data.currentHP.toLong(),
            restStartedAtEpochSec = if (assignment == HeroAssignment.RESTING) now else 0L,
            partyJoinedOrder = if (assignment == HeroAssignment.IN_PARTY) {
              if (h.inActiveSquad) h.partyJoinedOrder else nextOrder
            } else 0L, hiddenTrait = h.hiddenTrait.copy(unlocked = unlocked))
        }
        h.id in removed -> h.copy(inActiveSquad = false, currentAssignment = HeroAssignment.UNASSIGNED.name, partyJoinedOrder = 0L)
        else -> h
      }
    }
  }
}
