# ETERNAL SUMMONER graphics

The game uses a 2.5D oblique three-quarter camera. Preserve ObliqueCamera and its world-to-screen projection (ground Y scale 0.72); do not replace it with top-down or 45-degree isometric projection.

Terrain lies on the foreshortened ground plane. Upright sprites retain vertical height, visible roof/front/side planes and nearest-neighbor filtering. Keep existing world coordinates, footprints, collision, entrances and navigation.

Use shared sprite bounds/scale/anchors for visible feet and projected shadows; use SanctuaryLight for directional light. Preserve depth sorting and canopy foreground occlusion. Do not replace directional shadows with generic circles.

For graphics changes, build the APK and capture real runtime overview/gameplay, architecture, partial canopy occlusion and multiple zoom screenshots. Inspect the images; passing tests alone does not establish visual quality. Do not claim physical-device FPS without profiling.

## User's game-development skill preference

For future game implementation tasks in this repository, read
`.agents/skills/eternal-summoner-skill-router/SKILL.md` and select only relevant
guidance from `docs/GAME_SKILL_SELECTION.md`. For sprite/art/animation tasks also
read `.agents/skills/ai-game-spritesheets/SKILL.md` and use the supplied templates.
At the start of the task, explicitly mention in chat the one to three relevant
skills being used and how they fit the existing camera and asset pipeline.
Scenario skill documents do not install Scenario MCP or paid model access. Engine-
specific Unity/Godot/Unreal/Roblox instructions must not replace Kotlin/Compose.
Preserve the 2.5D rules above; upstream top-down examples and suggested frame sizes
do not override this game's projection, pivots, gameplay timing or existing systems.
The local skill is saved project guidance, not a promise of memory in unrelated chats.

## Arena iterative game studio
When Arena Agent Mode develops or refines ETERNAL SUMMONER, first read `.agents/skills/eternal-arena-quality-loop/SKILL.md` in addition to `eternal-summoner-skill-router`. Treat subroles as logical roles unless verified native subagent tools exist. For meaningful code/art changes run baseline -> implement -> evidence -> critique -> targeted fix -> re-test (max 3 refinement cycles). Preserve all original graphics, gameplay and Android constraints. Never fake CI, true device FPS, image review, provider QA score, or parallel worker execution. Do not merge PRs without owner approval.

## Arena specialist skill orchestration
When Arena works on ETERNAL SUMMONER, read `.agents/skills/eternal-summoner-skill-router/SKILL.md` and `.agents/skills/eternal-arena-quality-loop/SKILL.md`, then select relevant specialist skills by actual task. Preserve the original AGENTS.md and all project contracts. Use native parallel subagents only if verified available; otherwise execute roles sequentially with file ownership and honest role labels. Require baseline, real tests/render/playthrough evidence, targeted corrections (at most 3 rounds), final QA and PR for owner. Never fake CI or device measurements. Never auto-merge.

## Twelve real-agent GitHub dispatch (2026-11)

For all new agent instances (A01–A12), first read `docs/agents/START_HERE.md`, `docs/agents/ROLE_MATRIX.md`, `docs/agents/CLAIM_PROTOCOL.md` and the assigned `.agents/skills/es-aXX-*/SKILL.md`. Existing instructions above remain authoritative. Work originates from independently numbered GitHub Issues linked from #39. Post `[CLAIM]` in the task issue only after checking `STATE: READY`, unmet dependencies, other live claims and file ownership; blocked/backlog means read-only triage. Post a real `[HANDOFF]` with PR/SHA/checks, then receive independent review. User is sole merge approver. Codex owns critical battle/save/events reviews outside the twelve standard roles. Do not assume skills/comments auto-launch agents, imply live GitHub polling or fabricate separate agent sessions. Read the live task status; docs/agents/PHASE0_TASKS.md is a snapshot.
