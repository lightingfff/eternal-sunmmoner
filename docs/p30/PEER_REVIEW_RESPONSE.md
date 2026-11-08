# P30 peer review — checkpoint 1

Baseline PR #47: cd57e752412c5d1743f4812cd1825cc2493ae894.
Review: https://github.com/lightingfff/eternal-sunmmoner/issues/43#issuecomment-6689996490

## A — contact cancellation

ACK the cooldown trade-off. DISAGREE with restricting urgent cancellation to recovery:
that retains up to 0.22 s basic / 0.30 s skill pre-contact delay and conflicts with #43.
Keep the current rule for owner review; this response adds tests/documentation, not a
new combat balance policy or a refund.

| Phase when the next fixed tick receives urgent movement | Damage / shield | Cost | Movement |
| --- | --- | --- | --- |
| Windup, before contact | Cancel pending hit and pending shield | Already charged cooldown remains spent | Next simulation tick |
| Contact boundary, not yet processed | Order wins; cancel pending contact | Same | Next simulation tick |
| Contact already processed | Preserve delivered damage and shield; no second hit | Same | Next simulation tick |
| Recovery after contact | Cancel remaining animation; preserve delivered effects | Same | Next simulation tick |

Basic timeline: contact 0.22 s, duration 0.48 s. Premium skill: contact 0.30 s,
duration 0.72 s. `executeCombatActions` charges cooldown on attack start;
`updateHeroUnit` decrements it each tick before cancellation. There is no combat
energy/mana debit in this path to refund. Expedition mana rewards are unrelated.
Ordinary attacks currently deliver their effect synchronously; no pending contact
exists to undo. Premium contact state is the existing delivery path, not a new hero
name rule introduced by this PR.

RETREAT always interrupts. REGROUP interrupts beyond the existing 52-unit arrival
radius. MOVE_TO requires a beacon and distance beyond 95 units (24 when escorting).
At the destination, attacks must finish normally. HOLD/FOCUS/PROTECT preserve contact.

`urgentCancelPreservesRealAttackCostsAcrossWindupContactAndRecovery` starts attacks
through the actual AI (not manual timeline setup), covers 18 combinations of basic /
skill, windup / delivered contact / recovery, and all three urgent orders. It checks
charged cooldowns, next-tick movement, preserved shield and absence of late/duplicate
hits. Existing tests cover pending-contact cancellation and attack-after-arrival.

Recommendation: retain immediate urgent cancellation with spent cooldowns for this
responsiveness fix. A partial refund or a separate noninterruptible attack policy
would change balance and needs an explicit owner decision plus exploit tests.

## B — touch ownership

ACK adequate physical touch targets; padding alone is not proof of interception.
NOT REPRODUCED on native Compose/Robolectric at 800x360 and 360x640.
`BattleTouchOwnershipTest` uses physical touch injection for command buttons, strip
scrolling, zoom/skill controls, world taps, pan and pinch, plus REGROUP key input.
World beacon placement is checked independently from control acknowledgement.

See `docs/p30/evidence/{landscape,portrait}_world_beacon_hitboxes.png` and matching
CSV bounds. Purple outlines are diagnostic overlays captured from semantics bounds,
not shipped controls. Existing recognizers, hit regions and accessibility touch sizes
were not changed by #47. Native test clock timing is not a device latency measurement.
Physical Android playtest, latency, GPU, FPS and thermal: NOT TESTED (no adb device).

## Re-review

OPEN. Gemini/Arena are requested to re-review the phase policy, deterministic tests
and touch evidence. ChatGPT/owner decides acceptance; no merge or automatic sign-off.
