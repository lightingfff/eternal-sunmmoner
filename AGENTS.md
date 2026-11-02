# ETERNAL SUMMONER graphics

The game uses a 2.5D oblique three-quarter camera. Preserve ObliqueCamera and its world-to-screen projection (ground Y scale 0.72); do not replace it with top-down or 45-degree isometric projection.

Terrain lies on the foreshortened ground plane. Upright sprites retain vertical height, visible roof/front/side planes and nearest-neighbor filtering. Keep existing world coordinates, footprints, collision, entrances and navigation.

Use shared sprite bounds/scale/anchors for visible feet and projected shadows; use SanctuaryLight for directional light. Preserve depth sorting and canopy foreground occlusion. Do not replace directional shadows with generic circles.

For graphics changes, build the APK and capture real runtime overview/gameplay, architecture, partial canopy occlusion and multiple zoom screenshots. Inspect the images; passing tests alone does not establish visual quality. Do not claim physical-device FPS without profiling.

## User's game-development skill preference

For future game implementation and art/animation tasks in this repository, read
`.agents/skills/ai-game-spritesheets/SKILL.md` and use the relevant supplied templates.
At the start of the task, explicitly mention in chat that you are using
`ai-game-spritesheets` and adapting it to the existing camera and asset pipeline.
Preserve the 2.5D rules above; upstream top-down examples and suggested frame sizes
do not override this game's projection, pivots, gameplay timing or existing systems.
The local skill is saved project guidance, not a promise of memory in unrelated chats.
