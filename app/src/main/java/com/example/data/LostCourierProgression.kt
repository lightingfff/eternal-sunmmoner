package com.example.data

import com.example.data.model.CourierOutcome

/** A small story slice using the existing flags and hero bond text, without a save migration. */
object LostCourierProgression {
  const val FLAG_RESCUED = "COURIER_RESCUED"
  const val FLAG_FAILED = "COURIER_FAILED"
  const val RESCUE_MEMORY = "Đã vượt sợ hãi, đưa người đưa thư về cổng nhờ lệnh Master."

  fun isPermanentFlag(flag: String): Boolean = flag == FLAG_RESCUED || flag == FLAG_FAILED

  fun synthesisGoldCost(flags: List<String>): Long = if (FLAG_RESCUED in flags) 90L else 120L

  fun isFirstRescue(flags: List<String>, outcome: CourierOutcome?): Boolean =
    outcome == CourierOutcome.RESCUED && FLAG_RESCUED !in flags

  fun outcomeFlags(flags: List<String>, outcome: CourierOutcome?): List<String> {
    if (outcome == null) return flags
    val previous = flags.filterNot { it == FLAG_FAILED }
    return SanctuaryProgressionHelper.compactStoryFlags(
      previous + if (outcome == CourierOutcome.RESCUED) FLAG_RESCUED else FLAG_FAILED,
    )
  }

  fun hasRescueMemory(bond: String): Boolean = RESCUE_MEMORY in bond

  fun rememberRescue(bond: String): String {
    if (hasRescueMemory(bond)) return bond.take(200)
    val prefix = bond.trim().take((200 - RESCUE_MEMORY.length - 1).coerceAtLeast(0))
    return if (prefix.isBlank()) RESCUE_MEMORY else "$prefix $RESCUE_MEMORY"
  }
}
