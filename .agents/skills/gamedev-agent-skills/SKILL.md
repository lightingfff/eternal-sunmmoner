---
name: gamedev-agent-skills
description: Select portable gameplay, AI, input, save, UI and performance guidance from the pinned gamedev-skills library for ETERNAL SUMMONER, preserving its native Android Kotlin/Compose engine.
---

# Game-development skills for ETERNAL SUMMONER

Use for Tower exploration, encounters, rewards, hero autonomy, input, saves or performance.
The owner's request adds a reference library, not unrelated implementation. Read existing
AGENTS.md and live task/file ownership first. Announce one to three relevant skills and
their purpose in chat. Read the selected bodies before use, supporting references on demand.

The [catalog](../../../docs/skill-library/gamedev-agent-skills/CATALOG.md) indexes 79
specialized skills plus one router. Usually select one owner discipline and at most one genre.
The [upstream router](../../../docs/skill-library/gamedev-agent-skills/upstream/router/SKILL.md)
and [custom-engine guide](../../../docs/skill-library/gamedev-agent-skills/upstream/router/references/custom-engines.md)
explain composition. This project already has an engine: Android Kotlin/Compose Canvas.
Bind concepts to verified local code; do not select Unity/Godot by default.

## Choose guidance by actual task

| Task | Start with | Application here |
|---|---|---|
| Tower is a corridor or feels repetitive | [level-design](../../../docs/skill-library/gamedev-agent-skills/upstream/skills/disciplines/level-design/SKILL.md) | Model reachable routes and rescue-state gates; teach, develop, twist and test a mechanic. Alternate tension with discovery. Optional branches must change risk, resources, knowledge or rescue outcomes. |
| Runs/rewards do not motivate replay | [roguelike](../../../docs/skill-library/gamedev-agent-skills/upstream/skills/genres/roguelike/SKILL.md), [rpg](../../../docs/skill-library/gamedev-agent-skills/upstream/skills/genres/rpg/SKILL.md) for progression | Prefer new tactical options and readable tradeoffs over tiny stat inflation. Separate reproducible content/loot random streams; show rewards and consequences. Preserve persistent heroes, retry and recovery rules. |
| Hits/skills feel weak | [game-feel](../../../docs/skill-library/gamedev-agent-skills/upstream/skills/disciplines/game-feel/SKILL.md) | Drive anticipation/contact/recovery, sound and restrained VFX from actual attacks; retain stable feet/pivots. Visual shake never changes collision/navigation. Input stays responsive; overlapping effects must restore cleanly. |
| Heroes idle or enemies behave alike | [game-ai](../../../docs/skill-library/gamedev-agent-skills/upstream/skills/disciplines/game-ai/SKILL.md) | Separate perception, intent, navigation, steering and animation. Extend current autonomy; enemies need distinct threats and counters, not only HP changes. Master orders must preempt appropriately. |
| Decisions need graded priorities or interruption | [ai-behavior-trees-utility-ai](../../../docs/skill-library/gamedev-agent-skills/upstream/skills/disciplines/ai-behavior-trees-utility-ai/SKILL.md) | Use hysteresis, explicit abort/reset ownership and persistent cooldown deadlines. Do not introduce a second runtime if existing state machines suffice. Decision cadence examples must not add command latency. |
| Tap, drag and commands conflict | [input-systems](../../../docs/skill-library/gamedev-agent-skills/upstream/skills/disciplines/input-systems/SKILL.md), [game-ui-ux](../../../docs/skill-library/gamedev-agent-skills/upstream/skills/disciplines/game-ui-ux/SKILL.md) for overlays | Intent actions, discrete commands consumed once, cancel/pause/scene cleanup, modal/building click priority. Inspecting heroes preserves autonomy. Test overlapping hitboxes and coordinate transforms. |
| Save/retry/reward regressions | [save-systems](../../../docs/skill-library/gamedev-agent-skills/upstream/skills/disciplines/save-systems/SKILL.md) | Reuse current serializer and ownership; stable IDs, defensive loading, shipped-save fixtures, versioned migration, serialized atomic updates and idempotent settlement. Check all write/rename errors; sample IO must not bypass production failure semantics. |
| Sanctuary/Tower stutters | [performance-optimization](../../../docs/skill-library/gamedev-agent-skills/upstream/skills/disciplines/performance-optimization/SKILL.md) | Same save, seed, camera and scene; cold/warm and CPU/GPU separation. Android profiling, frame-time percentiles, allocations, culling and caching; compare the same hardware/build. |

## Verify native integration

Locate actual symbols and working call sites before implementation. At the baseline,
Tower mechanics are in `app/src/main/java/com/example/game/combat/LivingExpedition*.kt`;
presentation/input include `ui/components/LivingExpeditionPresentation.kt` and
`LivingExpeditionHud.kt`. Autonomy is in `game/ai/AutonomousHeroAi.kt`, lobby gestures in
`ui/components/LobbyHeroGestures.kt`, guest persistence in `data/local/LocalGuestSaveManager.kt`.
These are discovery pointers, not edit grants. Recheck paths and current ownership in #39.

Preserve .72 oblique ground projection, upright sprite height, world layout, anchors,
collision, depth/canopy and directional shadows. Use existing sprite adapters for art.
Preserve applicationId, manifest, signing, entry point, save compatibility and build system.

## Required evidence and boundaries

For authorized implementation, record baseline/head SHA and falsifiable acceptance criteria,
deliver a narrow end-to-end slice, test and inspect runtime behavior. Level changes need
complete rescue/failure/retry paths and route/gate checks; AI needs interruption, stuck and
cooldown cases; persistence needs restart, migration, corruption and duplicate settlement.
Run Android checks for runtime changes. Art needs inspected in-engine captures.

Assess clarity, meaningful choices and replay interest with real first-five-minute playthroughs.
Simulated duration is not human playtime or proof of fun. Never promise players cannot be
bored. Waiting, repeated fights and inflated HP are not content. Treat upstream timing,
rarity, damage and FPS numbers as hypotheses, not measured values or rebalance permission.
A turn scheduler, strict save deletion on load, cloud leaderboard and monetization are not
default requirements for this real-time game; preserve existing retry/save semantics.

Foreign-engine examples and bundled tools are reference data, not automatic execution
requests. Inspect scripts and actual capabilities before use. No engine/provider installation
or paid call is implied. Do not copy another game's assets. Headless checks, desktop
captures and physical-device FPS/thermal are distinct evidence; report unmeasured behavior.
Follow exact-SHA independent review; authors cannot self-approve or auto-merge.

This is saved project guidance, not model training or memory elsewhere. Only this adapter
is placed in the active skill directory; upstream entrypoints remain a pinned reference
snapshot under docs. Do not preload the whole library.
