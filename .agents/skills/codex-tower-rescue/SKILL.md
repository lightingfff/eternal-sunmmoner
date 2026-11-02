---
name: codex-tower-rescue
description: Implement, verify and iterate a real 5–10 minute playable Tower Floor 1 rescue mission for ETERNAL SUMMONER with tactical Master choices, AI enemy variety, branching outcome and persistent progress.
---

# Tower Floor 1 rescue — implementation skill

This is a self-contained **first real playable encounter**, not an entire campaign pitch. Treat every item as work to route into the actual repo after code audit. Use the established world and AI, not fresh parallel clones. Original fiction only; *inspired by* genre pacing, do not copy text/characters/maps from commercial manhwa.

## Player fantasy

The off-screen Master sees a squad arrive in a dangerous meadow. Scouts notice a distress signal near a broken watchpost; the rescue target is separated from a safe exit. The player does not steer a physical Master. They focus a threat, regroup the squad, or order retreat, while heroes make situated, legible decisions. Different enemy types react differently; choosing to save supplies vs support an endangered NPC changes both the reward and a future Sanctuary note. Keep stakes age-appropriate, non-graphic.

## Minimum end-to-end state machine (adapt names to existing code)

`READY -> APPROACH -> DISCOVERY -> THREAT_WAVE -> CHOICE_POINT -> ESCAPE_OR_HOLD -> RESOLVED_SUCCESS | RESOLVED_PARTIAL | RESOLVED_FAILURE -> REWARD_AND_SAVE -> RETURN_TO_SANCTUARY`.
No fake objective transitions triggered solely by time; require gameplay state + player tactical decisions. Avoid softlocks: have an alternate path if NPC gets stuck, squad lost or enemy remains outside navmesh. Provide retry on failure and clean scene reset.

## Combat and enemy goals

- A scout moves to assess squad and calls for backup if left alone, revealing focus command utility.
- A flanker seeks weak backline target but has an observable tell / counter opportunity; no teleporting or unavoidable damage.
- A slower guardian telegraphs an area attack; regroup timing matters.
- Hero behavior reacts to clear Master command priorities; fallback AI continues when no command is issued.
- Order acknowledgments are visible/sounded via existing UI; never freeze gameplay awaiting cutscene or network calls.
- Use existing timing/damage contact markers. No random permanent punishments or unfair instant defeats.

## Visual and narrative affordances

- Brief opening scene tied to actual controllable gameplay; small, interruptible dialogue.
- Objective tracker updated by real flags, quest pointer visible at 0.88 gameplay zoom, minimal screen coverage.
- Every mission objective has a clear in-world target; no waypoint into blocked geometry.
- Reward summary based on actual play and local persistence, plus a short future consequence preview.

## Implementation checkpoints

A. **Audit** source location: flow owner, units, battle loop, scene event system, tactical commands, NPC control, rewards, save. Record minimal plan with owner for each existing module.
B. **Domain logic**: add small state structure/choice/outcome mapping with stable IDs, deterministic tests, save compatibility; keep UI separate from simulation.
C. **Game integration**: map spawn/extraction and NPC/enemies through existing level data without moving Sanctuary or blocking paths. New shared catalog edits only by coordinator, never multiple workers.
D. **Command + AI integration**: actual focus/regroup/retreat with priorities and safe cancellation. Capture unit tests for command resolution and enemy variations.
E. **Evidence**: Play two distinct paths and a failure retry, print actual state logs; CI compile/tests; real engine evidence for relevant moments; don't confuse static screenshots with complete gameplay testing.
F. **Review**: Fail task if player cannot finish rescue, reward/save lost, actor blocks, invisible objective, any command no-op or background loop causing overheating.

## Definition of done

One complete playable 5–10 minute (target, not a proven timer), recoverable encounter with original NPC, meaningful choice, at least two distinct enemy AI patterns, 3 tactical command hooks, reward, save, return/retry and proof from running game/test environments. No placeholder-only UI. No claiming real device FPS without actually measuring.
