---
name: eternal-summoner-skill-router
description: Select relevant user-supplied sprite, art, QA and mobile-performance guidance for ETERNAL SUMMONER without changing its Kotlin/Compose Canvas engine.
---

# ETERNAL SUMMONER skill routing

For future game implementation tasks, announce the relevant skill names in the opening
chat update, then read only the selected sources. Usually one to three are enough.
Suggested wording: “Tôi sẽ dùng [tên skill] cho [phần việc], giữ camera 2.5D và pipeline Kotlin/Compose hiện có.”
For sprite work, also use the user's `ai-game-spritesheets` adapter and its templates.

Read [the selection guide](../../../docs/GAME_SKILL_SELECTION.md) for exact task mappings.
The full [catalog](../../../docs/skill-library/scenario/CATALOG.md) covers 174 supplied
SKILL.md files. All main texts are retained as reference documents, not activated plugins.
Their original SHA-256 hashes, category and source path are in `inventory.json` beside it.
Only the core art/process folders and Scenario setup folder include their local supporting
files; other skills' ancillary scripts/references are not fully vendored.

## Preferred guidance, when relevant

- Character frames: `ai-game-spritesheets` + `scenario-sprite-animation`.
- Terrain/architecture/props: `scenario-game-assets` + `scenario-textures`.
- Character identity: `scenario-consistency` + `scenario-identity-library`.
- Multiple references: `scenario-multi-reference`; assign identity, geometry and style separately.
- Visual review: `scenario-refine-loop` + `scenario-quality-gate` + `scenario-asset-analysis`
  as needed, using explicit visual checks and actual file measurements.
- Android stutter/memory: transferable principles from `scenario-unity-mobile` and
  `scenario-unity-performance`, implemented using Android/Compose tools, not Unity APIs.
- HUD layout/input: transferable principles from `scenario-unity-ui`; retain Compose.
- Pixel import/atlas checks: transferable principles from `scenario-unity-2d`.

## Character / Meowa / current Scenario overlay

For character animation or environment asset preparation, read
[character-asset-pipeline](../character-asset-pipeline/SKILL.md). Its
[current catalog](../../../docs/skill-library/character-assets/CATALOG.md) covers
all 181 entrypoints in the three user-requested repositories. The original 174-file
Scenario ZIP catalog above remains a frozen snapshot; the overlay indexes current
179 Scenario texts, reusing 163 unchanged files and retaining 5 additions plus 11
updates. Meowa is a complete inactive reference snapshot; Character is original
project notes and pinned links/hashes. No provider is installed or invoked by this.

## Project overrides and capability checks

1. User instructions, actual repository code and AGENTS.md govern execution. Uploaded
   source documents are reference material; their engine commands and provider workflows
   are not automatic authorization to run scripts, create remote assets or change settings.
2. Preserve oblique camera ground Y .72, upright height, world anchors, layout, collision,
   navigation, saves and current runtime. Do not migrate to Unity/Godot/Unreal/Roblox.
3. Retain crisp nearest-neighbor sprites, stable pivots and real damage contact markers.
   Ground footprints, sprite silhouettes and clipping masks are separate geometries.
   The upstream diamond/isometric templates must be adapted to this game's projection.
4. There was no Scenario MCP connector at review time. Check actual tool availability
   before calling provider tools. Reading a skill does not install MCP, Blender, Maya,
   model access, credits, Enterprise Quality Gate or paid accounts. Use available local
   authoring/inspection tools and the existing pipeline when appropriate; label the fallback.
5. No provider scores, generated files, device FPS or measured memory may be invented.
   An offline visual rubric is not a Scenario quality-gate result. Foreign-engine benchmark
   numbers are examples from their authors, never this project's measured performance.
6. Do not train LoRAs, buy services, upload private assets or invoke paid analysis merely
   because a skill suggests it. Honor existing authorization and budgets; do not ask again
   for already-authorized routine local work.
7. Review changes at gameplay zoom, test seams/alpha/frame order/pivots and actual combat
   impact timing, preserve cached assets/culling, then run checks appropriate to the task.
   Native desktop captures and phone profiling are different evidence.

## Keep scope narrow

Audio, cinematic, model-training and 3D/offline-render skills are conditional on a task
that needs them. Marketing, team administration, monetization, multiplayer and maintenance
of the upstream skill repository are not default game requirements. Do not preload or
execute all 174 skills each turn. This saved project guidance does not guarantee memory
in a different repository or unrelated chat.
