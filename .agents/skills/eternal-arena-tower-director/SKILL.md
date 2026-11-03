---
name: eternal-arena-tower-director
description: Design and implement authored Tower Floor 1–2 missions, rescue stories, encounter pacing, enemy compositions and persistent player choices using actual game state and existing maps.
---

# TRAINING 03 — TOWER MISSION DESIGNER / ENCOUNTER BUILDER

## Mission
Transform generic grind into a cohesive 5–10-minute target **human play experience**, with no artificial timer padding. Reuse actual tower floor map, enemy archetypes, save rules and gameplay systems. The first floor is a field/meadow, not a new generic stone dungeon. Create an original narrative, not copied dialogue/panels/world-building from any existing manhwa.

## Audit before writing
Read `TowerFloorCatalog`, existing mission/courier encounter sources, AI/combat state, quest UI, progress flags, encounter scripts, world obstruction and spawn/extraction. Understand current implementation and open PRs. For Mission01 on the existing branch, preserve Support, Supplies Recovered, Supplies Missed and their rewards/consequences; don't replace or silently invalidate saves. Account for an observed ~55-second deterministic route if still current.

## Pacing model: meaningful segments, not walls
**Approach**: guide player through authentic movement/route selection and one environmental clue; avoid blank travel across map.
**Discovery**: meaningful rescue clue and enemy-scout reaction; allow a Master Focus decision with actual consequence.
**Escalation**: encounter with 2 distinct enemy tactics (flank and telegraphed heavy); change tactical priority, not only enemy quantity.
**Choice**: support NPC or recover supplies with physically verifiable objective; communicate opportunity cost and real status.
**Extraction**: protect moving courier/evacuate; route must be navigable and recoverable. If lost, give clear failure/retry or partial result.
**Resolution**: branch-specific reward and persistent consequence, visible return to Sanctuary.
Do not require every phase to occupy an equal fixed number of seconds. Human input/decision duration varies; measure simulation and human wall time separately.

## Requirements for engaging content
Choose one significant new mechanic per iteration: e.g. scout alarm, fog-of-war reveal, escort detour, intercept decision or terrain hazard with clear tell. It must create a measurable tactical choice. Follow with playable confirmation; do not add 10 shallow features. Add variation to timing via player action and branching routes, not by hardcoded wait loops, heavy HP scaling or repeated trash waves. Include fallbacks when a player never issues command, an enemy wanders away, courier is blocked, or a choice is missed.

## Task interface/ownership
Own only explicitly assigned encounter scripts and mission-specific structures. The shared `TowerFloorCatalog.kt`, navigation, `SummonerViewModel`, `MainActivity`, saves and renderer are coordinator-only unless assigned one-by-one. Supply interface contract for gameplay engineer: event IDs, state transitions, AI spawns, order callbacks, and outcome flags. QA owns verification but not gameplay code.

## Acceptance criteria
Two genuinely different completion outcomes plus a partial/failure/retry route; player sees objective at gameplay zoom; NPC physically present; no fake spawn/despawn transitions; two types of enemy counterplay; selected choice impacts at least one later observable consequence. End-to-end save/reload tests. A playable target of 5–10 minutes should be **tested by players**; automated route timings are only regression data. Report exact tested durations and environments.

## Qualification drills
1. Produce an authored beat sheet tied to real map coordinates without editing Sanctuary. 2. Show two tactical commands changing outcomes in actual state traces. 3. Verify a resource cache must be physically reached to count. 4. Run a deterministic success, partial, failure/retry path. 5. Show a narrative consequence after restarting the app.

## Anti-patterns
Time countdown with idle filler, huge HP bars for duration, static NPC speech bubbles pretending gameplay, arbitrary quip spam, copied manhwa canon, untested dead-end routes, automatic reward granted before save, cutscene blocking Master control.

## Deliverable
Finite-state diagram/table, encounter implementation, tested traces and screenshot bookmarks, mission QA verdict, unresolved human playtest limitation.
