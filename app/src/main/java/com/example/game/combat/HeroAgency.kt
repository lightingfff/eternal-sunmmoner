package com.example.game.combat

import com.example.data.model.PersonalityType
import com.example.data.model.TacticalOrder

/** One request per fear episode; the player remains free to accept, dismiss or ignore it. */
data class HeroAgencyAlert(val heroId: String, val heroName: String, val messageVi: String)

internal class HeroAgency {
  private val notified = mutableSetOf<String>()
  private val reassured = mutableSetOf<String>()
  var alert: HeroAgencyAlert? = null
    private set

  fun update(heroes: List<HeroCombatUnit>) {
    // A later distinct fear episode can produce another request and response.
    notified.removeAll { id -> heroes.none { it.id == id && !it.knockedOut && it.fearSecondsRemaining > 0f } }
    reassured.removeAll { id -> heroes.none { it.id == id && !it.knockedOut && it.fearSecondsRemaining > 0f } }
    if (alert != null && heroes.none { it.id == alert?.heroId && !it.knockedOut && it.fearSecondsRemaining > 0f }) alert = null
    if (alert != null) return
    val hero = heroes.firstOrNull { !it.knockedOut && it.fearSecondsRemaining > 0f && it.id !in notified } ?: return
    notified += hero.id
    val message = when (hero.personality) {
      PersonalityType.RECKLESS -> "Tôi đang run, nhưng vẫn có thể giữ chân chúng! Master, tập hợp đội hay rút lui?"
      PersonalityType.STOIC, PersonalityType.LOYAL -> "Đội hình đang lung lay. Master, hãy tập hợp chúng tôi hoặc ra lệnh rút lui."
      else -> "Tuyến này quá nguy hiểm. Master, có nên rút lui và bảo toàn đồng đội?"
    }
    alert = HeroAgencyAlert(hero.id, hero.name, message)
  }

  fun dismiss() { alert = null }

  fun respond(order: TacticalOrder, heroes: List<HeroCombatUnit>) {
    if (order != TacticalOrder.REGROUP && order != TacticalOrder.PROTECT_ALLY && order != TacticalOrder.RETREAT) return
    val current = alert ?: return
    val hero = heroes.firstOrNull { it.id == current.heroId && !it.knockedOut } ?: return
    if (order != TacticalOrder.RETREAT && reassured.add(hero.id)) {
      val recovery = when (hero.personality) {
        PersonalityType.STOIC, PersonalityType.LOYAL -> 2.5f
        PersonalityType.RECKLESS -> 1f
        else -> 1.5f
      }
      hero.fearSecondsRemaining = (hero.fearSecondsRemaining - recovery).coerceAtLeast(0f)
    }
    alert = null
  }
}
