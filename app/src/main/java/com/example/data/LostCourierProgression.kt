package com.example.data

import com.example.data.model.CourierOutcome

/** A small story slice using the existing flags and hero bond text, without a save migration. */
object LostCourierProgression {
  const val FLAG_RESCUED = "COURIER_RESCUED"
  const val FLAG_FAILED = "COURIER_FAILED"
  const val FLAG_SUPPORTED = "COURIER_SUPPORTED"
  const val FLAG_SUPPLIES_RECOVERED = "COURIER_SUPPLIES_RECOVERED"
  const val FLAG_SUPPLIES_MISSED = "COURIER_SUPPLIES_MISSED"
  const val RESCUE_MEMORY = "Đã vượt sợ hãi, đưa người đưa thư về cổng nhờ lệnh Master."

  const val FLAG_ALARM_SILENCED = "DISC_m01b_alarm_silenced"
  const val FLAG_SCOUT_RESCUED = "DISC_m01b_scout_rescued"
  const val FLAG_JOURNEY_COMPLETE = "DISC_m01b_debrief"

  fun isPermanentFlag(flag: String): Boolean = flag == FLAG_ALARM_SILENCED || flag == FLAG_SCOUT_RESCUED ||
    flag == FLAG_JOURNEY_COMPLETE || flag == FLAG_RESCUED || flag == FLAG_FAILED ||
    flag == FLAG_SUPPORTED || flag == FLAG_SUPPLIES_RECOVERED || flag == FLAG_SUPPLIES_MISSED

  fun isRescue(outcome: CourierOutcome?): Boolean =
    outcome == CourierOutcome.RESCUED || outcome == CourierOutcome.SUPPLIES_RECOVERED ||
      outcome == CourierOutcome.RESCUED_WITHOUT_SUPPLIES

  fun synthesisGoldCost(flags: List<String>): Long = if (FLAG_RESCUED in flags) 90L else 120L

  fun isFirstRescue(flags: List<String>, outcome: CourierOutcome?): Boolean =
    isRescue(outcome) && FLAG_RESCUED !in flags

  // Old saves only had RESCUED. Treat that result as the original support path, so
  // upgrading a save never grants its first-completion reward a second time.
  private fun hasSupportOutcome(flags: List<String>): Boolean = FLAG_SUPPORTED in flags ||
    (FLAG_RESCUED in flags && FLAG_SUPPLIES_RECOVERED !in flags && FLAG_SUPPLIES_MISSED !in flags)

  fun bonusGold(flags: List<String>, outcome: CourierOutcome?): Long =
    if (outcome == CourierOutcome.SUPPLIES_RECOVERED && FLAG_SUPPLIES_RECOVERED !in flags) 60L else 0L

  fun bonusMana(flags: List<String>, outcome: CourierOutcome?): Long =
    if (outcome == CourierOutcome.RESCUED && !hasSupportOutcome(flags)) 20L else 0L

  fun outcomeDescription(outcome: CourierOutcome?): String = when (outcome) {
    CourierOutcome.RESCUED -> "Đã ưu tiên cứu người đưa tin. Lần đầu: +20 Mana; ký ức và dịch vụ lò rèn được lưu."
    CourierOutcome.SUPPLIES_RECOVERED -> "Đã cứu người đưa tin cùng tiếp vận. Lần đầu: +60 Vàng; tiếp vận đã về Thánh Địa."
    CourierOutcome.RESCUED_WITHOUT_SUPPLIES -> "Đã cứu người đưa tin nhưng bỏ lỡ tiếp vận. Không nhận thưởng nhánh; có thể trở lại để thu hồi tiếp vận."
    CourierOutcome.FAILED -> "Người đưa tin chưa tới cổng. Không nhận thưởng nhánh; có thể trở lại Tầng 1 để thử lại."
    null -> ""
  }

  fun outcomeFlags(flags: List<String>, outcome: CourierOutcome?): List<String> {
    if (outcome == null) return flags
    val previous = flags.filterNot { it == FLAG_FAILED }.toMutableList()
    if (hasSupportOutcome(flags)) previous += FLAG_SUPPORTED
    val additions = when (outcome) {
      CourierOutcome.RESCUED -> listOf(FLAG_RESCUED, FLAG_SUPPORTED)
      CourierOutcome.SUPPLIES_RECOVERED -> listOf(FLAG_RESCUED, FLAG_SUPPLIES_RECOVERED)
      CourierOutcome.RESCUED_WITHOUT_SUPPLIES -> listOf(FLAG_RESCUED, FLAG_SUPPLIES_MISSED)
      CourierOutcome.FAILED -> listOf(FLAG_FAILED)
    }
    return SanctuaryProgressionHelper.compactStoryFlags(
      previous + additions,
    )
  }

  fun hasRescueMemory(bond: String): Boolean = RESCUE_MEMORY in bond

  fun rememberRescue(bond: String): String {
    if (hasRescueMemory(bond)) return bond.take(200)
    val prefix = bond.trim().take((200 - RESCUE_MEMORY.length - 1).coerceAtLeast(0))
    return if (prefix.isBlank()) RESCUE_MEMORY else "$prefix $RESCUE_MEMORY"
  }
}
