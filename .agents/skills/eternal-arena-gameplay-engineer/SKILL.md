---
name: eternal-arena-gameplay-engineer
description: Implement and refine real ETERNAL SUMMONER combat, hero AI, tactical Master commands, state transitions, damage/contact timing and recoverable gameplay without placeholder mechanics.
---

# TRAINING 02 — GAMEPLAY & AI ENGINEER

## Mission
Produce *real, testable, interesting decisions* rather than menus that imply game mechanics. Audit current `app/src/main/java/com/example/game/combat/`, `game/ai/`, game loop and ViewModel boundaries first. The world is a real-time top-down oblique 2.5D Android RPG where Master issues instructions from outside the map. Hero autonomy must coexist with Focus, Regroup and Retreat, not overwrite them every update.

## Architecture protocol
Locate source of truth for game state and each command; trace the actual call chain from HUD click → ViewModel → engine → AI/action → world result → HUD. Avoid duplicate state machines or a second combat engine living beside the first. Use stable entity IDs, deterministic seeds or fixtures for tests, finite vector calculations, and bounded update work. Keep gameplay state out of composables and Canvas; rendering observes state. Never persist transient coroutine/timer objects. Never put network requirements into guest-mode single player.

## Tactical command contract
- Focus: selectable real target, clear ownership/validity; allies redirect and visibly acknowledge. Enemy leaving range, dying or despawning clears target safely; never command invisible/reused entity IDs.
- Regroup: destination grounded to valid nav space and/or escort anchor, prevents hero flicker between autonomous pursuit and command destination, restores appropriate autonomy afterward.
- Retreat: goes to real extraction/spawn, not a movable marker; interrupts attacks in a safe state; a retreat should have an actual tradeoff and not silently produce full victory.
- Every order has **intent → acceptance → execution → visible outcome**, with cancellation/resumption tests. No button may be ornamental.

## Hero and monster behavior design
Different enemies require different counterplay: scout attempts to alert, flanker targets exposed unit with readable approach, guardian telegraphs a slower heavy area attack. Threat timing and hit frames must match animation and damage events. Eliminate teleport hits, attacks through collision, unavoidable one-shots and NPC stuck states. Hero AI has meaningful self-preservation, morale/fear cues and a safe fallback; avoid black-box outcomes where player cannot understand why heroes chose actions.

## Realism and replay
Use actual starting heroes, equipment, stats and collision; do not alter test-only power to obtain success. Run success, partial success, failure, retry and return-to-Sanctuary paths. Log choices, command timestamps, target changes, damage/telegraphs, state transitions and ending. Validate save migration/compatibility and one-time reward grants. A 55-second automated encounter is not proven 5–10 minute human gameplay.

## Unit/performance discipline
Modify only assigned gameplay modules; save schema, catalog/world map, UI, and shared renderer are Director-owned integration surfaces unless explicitly assigned. Don’t spawn unbounded coroutines per entity, allocate paths each frame, or duplicate decision trees per render pass. Every new rule has at least one deterministic test: command precedence, target removal, cancellation during wind-up, no hero loss softlock, proper extraction, restart persistence and one-time reward idempotency.

## Acceptance/QA
A mission path is PASS only when an actual input/command changes real state; not when a log prints “executed.” At least two enemy patterns produce observably distinct behavior in engine traces. Avoid reward granted twice on retry or loss. Build compile and unit tests required. Distinguish simulated traces from touch playtesting.

## Failure signatures to catch
Heroes ignore regroup due to next AI tick, attacks land on dead units, interaction triggers outside usable range, failure hides retry, retreat never completes, battle becomes idle forever, collision blocks escort, time-gated fake objectives, UI fires no-op intent, save flags reset on process restart.

## Qualification drills
A: Trace one command end-to-end in current repo and document owners. B: Write deterministic command interruption test. C: Solve a softlock without teleporting NPC or disabling collision in real gameplay. D: Demonstrate both retry and persisted one-time reward. E: Explain why each new phase increases meaningful player agency.

## Deliverable
Real implementation diff; tests (names and outputs); sample playthrough traces with inputs and state; remaining gameplay risks; no unrelated art/map edits.
