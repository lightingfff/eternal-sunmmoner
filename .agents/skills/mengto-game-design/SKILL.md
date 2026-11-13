---
name: mengto-game-design
description: Apply selected MengTo game-level, combat-animation, monster, VFX and evidence-based UI/review methods to ETERNAL SUMMONER while preserving native Android Kotlin/Compose, pixel art and existing gameplay contracts.
---

# MengTo design methods for ETERNAL SUMMONER

Use for an authorized task improving level readability, monster identity/AI, combat
animation/VFX, HUD clarity or evidence-based visual review. First read AGENTS.md and the
current task/file ownership. Announce the one to three selected skills and purpose.
The [catalog](../../../docs/skill-library/mengto/CATALOG.md) indexes183upstream entrypoints;
read only relevant bodies and then their supporting references on demand.

## Choose the specific method

| Need | Read | Native application |
|---|---|---|
| Map has unclear routes or meaningless decoration | [author-game-levels](../../../docs/skill-library/mengto/upstream/agent-skills/game-development/author-game-levels/SKILL.md) | Give spaces a traversal/combat/discovery/reward purpose. Keep stable anchors; separate scenery, collision, navigation, interactions and encounters. Verify every critical/optional route, gate and retry; motivate light with visible emitters. |
| Combat feels arbitrary or unresponsive | [design-action-combat](../../../docs/skill-library/mengto/upstream/agent-skills/game-development/design-action-combat/SKILL.md) | Define startup/contact/recovery/cancel/resource rules. Resolve contacts once from authoritative events; visuals follow results. Check direction, range, interruption, repeated inputs, cooldown and reset. |
| Monsters look like unrelated shapes or use mismatched hitboxes | [build-game-monster-system](../../../docs/skill-library/mengto/upstream/agent-skills/game-development/build-game-monster-system/SKILL.md) | Keep authored definition, mutable runtime, sprite/clip contract and rendering adapter separate. Preserve shared feet/pivots, direction, attack origins and collider ownership across actions; rendering never decides hits. |
| Enemies chase forever, flicker or attack unfairly | [tune-enemy-ai](../../../docs/skill-library/mengto/upstream/agent-skills/game-development/tune-enemy-ai/SKILL.md) | Extend current perception/intent/movement layers with explicit transitions, commitment and recovery. Reproduce acquisition/loss, blockers, interruption, reset and multiple enemies deterministically. |
| Awkward poses, sliding feet or clipped weapons | [game-dev-combat-animation](../../../docs/skill-library/mengto/upstream/agent-skills/game-combat/game-dev-combat-animation/SKILL.md) | Write move-specific observable rules, retain an unchanged baseline, sample actual action times, inspect contact strips and full-resolution frames at gameplay camera/scale. Keep anticipation, contact, follow-through and recovery distinct. |
| Skills are generic rings or flashes | [game-dev-combat-skill-polish](../../../docs/skill-library/mengto/upstream/agent-skills/game-combat/game-dev-combat-skill-polish/SKILL.md) | Review caster motion, effect and target reaction together. Give each skill a readable silhouette, palette, path, contact and aftermath; preserve targets and controls instead of covering them with bloom. Separate art quality from readability. |
| HUD obscures play or repeats decoration | [no-ai-design-slop](../../../docs/skill-library/mengto/upstream/agent-skills/ui/no-ai-design-slop/SKILL.md) | Use existing tokens and pixel-fantasy identity. Prefer a clear primary task/action; remove redundant containers, labels and effects only when their state/information survives. Keep useful density, contrast, touch targets and full disabled/error states. |
| Review requested without implementation | [audit-ai-design-slop](../../../docs/skill-library/mengto/upstream/agent-skills/ui/audit-ai-design-slop/SKILL.md) | Report concrete component/state/viewport evidence and harm, then smallest correction. Preserve art direction. No speculative redesign, AI-authorship guess or arbitrary slop score. |
| Need a measurable visual target | [workflow-score-to-target](../../../docs/skill-library/mengto/upstream/agent-skills/workflow/workflow-score-to-target/SKILL.md) | Define the user's actual target/rubric before edits; retain before/after evidence and independent reviewer identity. Do not invent a target or round a score up. Record measured criteria separately from subjective judgement. |
| Need proof the game works | [test-playable-web-games](../../../docs/skill-library/mengto/upstream/agent-skills/game-development/test-playable-web-games/SKILL.md) | Transfer its journey/transition matrix to actual Android tests: launch, commands, encounter, reward, save/continue, loss/retry, pause and touch variants. Browser examples are not native-game execution evidence. |

## Keep the actual engine, art and simulation

This game uses offline Kotlin/Compose Canvas and .72 oblique ground projection, not a
Three.js browser game. Map concepts to existing code/call sites and tests before writing.
Keep world geometry/footprints, entrances, stairs/cutscenes, collision, navigation, upright
sprite height, depth/canopy, nearest filtering and directional SanctuaryLight shadows.
The upstream flat-world/no-stairs rule is a template constraint, not permission to remove
approved architecture. Three.js axes/meters, skinned-mesh joints, PBR, triangle/Retina
budgets and generic9-state rigs do not become requirements for the current PNG pipeline.

Use existing EnemyArchetype/CombatEntities, contact timelines, monster/VFX atlases and
render adapters; do not create a second combat/AI/monster framework because of sample
type names. Preserve actual basic/premium contact markers and projectile-arrival timing.
New timing or state changes require explicit task scope and tests, not visual convenience.
Use existing sprite guidance (and Agent Sprite Forge when installed) for source masters,
shared registration, palette, alpha/clip QA and original PNG export. A skeleton demo is
not sprite animation; a contact sheet alone is not proof of motion quality.

Keep Android identity/signing/entry point, save/HP compatibility, receipt idempotence,
rescue/retreat/retry branches, hero autonomy and Master override. Do not edit Sanctuary
renderer/HUD or another agent's files without assigned ownership. Preserve current asset,
culling/cache/loading budgets; never add full-screen VFX or per-frame decoding by default.

## Evidence and source-specific limits

Source-specific projects, example scores, limb limits, vendor models, account restrictions,
personal file paths and claimed A/B results describe their authors' work. They are not
this user's preferences, available accounts/tools or ETERNAL SUMMONER measurements.
Do not infer Tripo/Mixamo/Higgsfield access, install dependencies, call providers or publish
because a source playbook says so. No foreign install/capture/ship script is run by reading it.

For combat review, use unchanged-source deterministic fixtures plus a real play path;
inspect actual full-resolution/native runtime frames at intended zoom with target and HUD
visible. State exactly what was staged. Stepped footage is not FPS, and screenshots are
not a5–10minute human playtest. Verify each critic's claim against actual frames/timing.
If a selected polish workflow calls for blind critics, use actual independent reviewers
when available; never fabricate judges, score your own work as blind, or imply agents
were launched by saving this file. No score is reported from merely reading code.

Use rubrics fitted to this game's pixel art, not literal Diablo/3D photorealism. Do not
apply a numeric slop score: that audit intentionally uses evidence/severity instead.
Require focused tests/build/native inspection for later implementation and real hardware
for phone latency/FPS/thermal. Record unmet targets and remaining unknowns candidly.
Project exact-SHA reviews and owner-only merge rules govern shipping.

This adapter is saved project guidance, not model training or memory in unrelated chats.
The183upstream entrypoints remain reference texts under docs; only this adapter is placed
in the active skill directory. Learning this library does not authorize game modifications.
