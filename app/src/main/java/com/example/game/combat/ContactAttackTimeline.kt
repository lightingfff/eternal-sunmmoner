package com.example.game.combat

/** Simulation seconds and a single contact marker; no render-frame or wall-clock dependency.
 * Runtime-only state: it is never written into HeroRecord or the save schema. */
class ContactAttackTimeline {
  var elapsed: Float = 0f; private set
  var duration: Float = .48f; private set
  var contact: Float = .22f; private set
  var active: Boolean = false; private set
  var skill: Boolean = false; private set
  var targetId: String? = null; private set
  private var emitted = false
  val progress: Float get() = if (active) (1f-elapsed/duration).coerceIn(0f,1f) else 0f
  fun start(targetId: String?, skill: Boolean = false) {
    this.targetId = targetId; this.skill = skill
    duration = if (skill) .72f else .48f
    contact = if (skill) .30f else .22f
    elapsed = 0f; emitted = false; active = true
  }
  fun advance(dt: Float): Boolean {
    if (!active) return false
    elapsed += dt.coerceAtLeast(0f)
    val hit = !emitted && elapsed + .00001f >= contact
    if (hit) emitted = true
    if (elapsed + .00001f >= duration) active = false
    return hit
  }
  fun cancel() { active = false }
}
