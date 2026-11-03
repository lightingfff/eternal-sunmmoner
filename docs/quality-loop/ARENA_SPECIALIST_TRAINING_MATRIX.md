# ARENA SPECIALIST TRAINING MATRIX — ETERNAL SUMMONER

These are operational skill manuals, not model retraining. Install into `.agents/skills/` and route per ticket. One Arena Agent Mode session can execute multiple role protocols sequentially; independent subagents require verified native support. Never claim true parallel model workers from a file directory alone.

| Training ID | Role | Primary deliverable | Hard exit test |
| --- | --- | --- | --- |
| 00 | Arena Quality Loop | Ticket, file ownership, evidence and bounded iterations | No blocker falsely marked PASS; max3 |
| 01 | Studio Director | Plan/ownership/integration and player-visible value | Work-order before writes + preserved canon |
| 02 | Gameplay Engineer | Real tactical commands, hero AI, save-safe combat | Commands do something + replay/retry tests |
| 03 | Tower Director | Original authored Tower mission with branches | Two outcomes + failure/retry + persistence |
| 04 | Environment Architect | Cohesive roof, wall, door, foundation 2.5D art | Same-camera engine render + true trigger alignment |
| 05 | Technical Artist | Coherent bounded lights/shadows/VFX | Pixel sharpness + quality tiers + no unsupported FPS |
| 06 | Android Performance | Identified bottleneck and conservative fix | Reproducible measured baseline + limited patch |
| 07 | Visual & Gameplay QA | Structured FAIL/PASS with specific evidence | Native render/playthrough, no taste-only green score |
| 08 | Integration Reviewer | Diff audit, tests, honest PR for owner | Real commit SHA/CI + no auto-merge |

## File-ownership examples
- `PixelSpriteRenderer.kt`, `SanctuaryWorldRenderer.kt`, `TowerFloorCatalog.kt`, `MainActivity.kt`, `SummonerViewModel.kt`, core save schema: one Director writer at a time unless explicit transfer.
- `premium_slice` assets/generated sprite scripts: Environment Architect.
- `SanctuaryDarkFantasyShaders.kt`, bounded lighting integration: Technical Artist.
- `game/combat`, `game/ai`: Gameplay Engineer, except shared battle engine integration scheduled by Director.
- `docs/quality-loop/`, test reports: QA/reviewer; QA may add tests with coordinator approval but does not change implementation under review.
- `ScenePerformance.kt` and performance profiling: Android Performance Engineer.

## Practical skill selection
Always load existing `eternal-summoner-skill-router`, choose one to three upstream references relevant to current task, and load needed Arena role skill(s). For sprite work read existing `ai-game-spritesheets`. For art read `scenario-game-assets`, `scenario-textures`, `scenario-refine-loop` as needed. For performance use only transferable principles from scenario Unity mobile/performance references, never Unity engine code. Never preload entire skill library or use paid tools automatically.

## Example automated assignment
User: "Sửa nhà bị hở rồi nâng sáng nhưng đừng giật": Director discovers current art/baseline → Environment Architect fixes true roof/wall/door/foundation geometry → QA records 0.88/1.48 engine evidence → Technical Artist adjusts bounded local lights → Performance Engineer checks benchmark limitations → Integration Reviewer signs off when checks pass. If Arena cannot spawn agents, these are checkpoints inside one ongoing session, not five new chats.
