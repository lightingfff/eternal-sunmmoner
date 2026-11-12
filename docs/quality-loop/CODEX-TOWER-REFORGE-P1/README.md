# Tower Reforge — Part 1 (#99)

Base: main `fd79c8b7efaead75d418489652bbf1e1e6d9b5f2` (owner merged PR #94). Branch: `codex/tower-reforge-p1`.

This part repairs command trust and upgrades Floor 1 combat presentation. It does not establish that the game is now fun, endlessly replayable, or balanced. The next parts below remain necessary. PR #96 owns its North deadline implementation separately; this branch does not change `LivingExpedition.kt` or `LivingExpeditionMechanicsTest.kt`.

## What changes in the playable game

- **Inspection keeps exploration running.** Empty taps that issue no command also preserve autonomous steering. When ally/enemy hit areas overlap, the nearer actor receives the tap. Armed Move remains higher priority than inspection. Deliberate movement, targeting and other orders retain ownership until explicit resume.
- **Auto framing includes the actual fight.** Nearby revealed active enemies join the party bounds; unseen, dead, distant and stale targets do not pull the frame. Manual zoom/pan stays under player control. The actual assault sequence exposed clipped enemies in the initial camera, prompting this correction.
- **Di chuyển stays visible.** A 56dp-high movement control lives outside the horizontally scrolling orders. The paused introduction explains Move → ground position → explicit resume, and Kraag uses the same wording. Existing touch/lifecycle/modal guards remain. This is positional evasion, not invulnerability or teleportation.
- **Four Floor 1 enemy families use original PNG poses.** Raider, archer, dire wolf and Kraag have distinct textured silhouettes; each has twelve source poses covering two idle, four walking, anticipation/contact/recovery, hurt and two collapse/body poses. Left-facing frames are mirrored and inspected; this is not a separately authored four-direction pack. Fixed foot pivots preserve body size rather than fitting every frame to a new bounding box.
- **Skill and projectile identity.** Shield, arcane impact, slash and holy light have four original eight-frame PNG clips. Arrows have shafts, fletching and tips; magic projectiles have distinct trails/cores. Contact, actual shield absorption and Kraag slam use execution metadata. Kaelen stops showing skill animation after its real timeline ends or is canceled. Kraag shows contact before settling into recovery.

The existing `.72` projection, depth queue, directional shadows, collision/navigation, canopy occlusion and upper-floor fallback remain. No damage values, attack timing, enemy HP, save schema, loot odds, Android package/signing/entry point, Sanctuary asset or shared renderer changed.

## Original art and reproducibility

Source files [enemy-original.png](source/enemy-original.png) and [vfx-original.png](source/vfx-original.png) were created with the available image-generation tool for this task. They do not reproduce manhwa panels or another game's assets. Runtime PNGs are in `app/src/main/assets/tower_combat_v1/` with dimensions, pivots and SHA256 manifests. Source art is kept outside Android assets so it is not shipped in the APK.

Technical normalization is reproducible (Pillow, numpy, scipy):

```sh
python tools/normalize_tower_combat_assets.py \
  docs/quality-loop/CODEX-TOWER-REFORGE-P1/source/enemy-original.png \
  --vfx-source docs/quality-loop/CODEX-TOWER-REFORGE-P1/source/vfx-original.png \
  --output app/src/main/assets/tower_combat_v1 \
  --evidence /tmp/tower-art-review
```

Recovery uses connected components to retain complete weapons that cross imperfect generated grid borders. One fixed scale per actor and per VFX pack preserves movement/crouching/impact growth. The first narrow normalization cell clipped Kraag's extended weapon; widening to128px fixed that before integration. Alpha, unique source poses, clip order and actual runtime output were reviewed. Unique pixel hashes alone are not proof of useful motion.

[Enemy contact sheet](evidence/enemy-contact-sheet.png) · [VFX contact sheet](evidence/vfx-contact-sheet.png). GIFs in this directory are asset reviews, not phone recordings.

## Runtime and memory evidence

Native tests enumerate all96 enemy surfaces and32 VFX surfaces, validate Android premultiplication, pixel-exact mirroring, contact selection and stable warmed decode counts. Retained CPU bitmap allocation is **7,077,888 enemy bytes +1,179,648 VFX bytes =8,257,536 bytes (7.875 MiB)**. Temporary decoded sheets, GPU textures, other game assets and shadow-path caches are additional. This is not measured Android process memory or FPS. The existing optional performance overlay counter excludes these new atlases; their native allocation is reported separately here.

Only the explicit preload decodes PNGs, off the draw path. Immutable completed packs are published after validation; bad packs are negative-cached and use the original/vector fallback. Frames own independent crops before temporary sheets are recycled. Per-draw culling happens before frame lookup. Execution effects expire in simulation time and are capped at64. First use of a pose still builds shared shadow/bounds geometry on the UI path; cold phone frame pacing remains unmeasured. These shared unsynchronized caches are not prewarmed from a background thread.

## Verification and evidence limits

Commands use the existing SDK/JDK/Gradle configuration. Local build initially found the new worktree missing its ignored `debug.keystore`; copying the existing local key restored the original local signing channel. No key was generated or committed and no Gradle/signing configuration was changed. The local signing channel differs from CI; install the separately verified CI APK for updates from that channel.

Validation covers new command/actual-execution tests, native premultiplied atlas/contact/zoom/edge/death specimens, fresh-guest physical Move+ground touch and North assault sequence, original and living route mechanics, NPC combat, manual override, legacy rescue, actual local save/replay/launch, existing phone-aspect UI, original escort world/layout and Android assembly. Local retained XML confirms **92 distinct JUnit cases, zero failures/errors/skips** across the final verification batches; [test summary](evidence/local-test-summary.json). CI on the published exact commit is reported separately in the PR handoff. A failed new test initially assumed no hero was preselected; the fixture was corrected to preserve the existing selection. The arranged comparison capture was moved outside composition after a zero-sized host failure. A later caption/hash check exposed stale first-specimen state, so arranged images now use direct native CanvasDrawScope with an immutable specification per frame; production UI captures remain separate. No failing result is presented as passing evidence.

The original-vs-new enemy comparison holds the arranged scene/camera/time constant and toggles only the enemy renderer; its VFX are the new implementation in both frames. Render specimens at `.36/.88/1.48` and light/dark/grass backgrounds are explicitly labelled **ARRANGED, NOT PLAYTHROUGH**. The production assault sequence uses the real guest party, physical site/decision controls and simulation movement/attacks; no actor teleports, forced deaths or injected outcome. Its MP4 is encoded native test-clock frames at150ms simulation intervals, not a device recording or measured frame rate.

No physical device or hardware emulator is attached. Real install/update, Android input latency, GPU/FPS/heat, human five-minute enjoyment and replay intention are **NOT TESTED**. Existing route times remain simulation evidence, not 5–10 human minutes. Argon/A09 reviews are requested on the published exact SHA; native Codex reviewers are not those identities. Root is the writer and cannot self-approve.

## Comparative research actually read

| Source | Verified description | Design lesson used or reserved |
|---|---|---|
| [Hades developer FAQ](https://www.supergiantgames.com/blog/hades-faq/) | Permanent progression, varied power combinations and characters remembering attempts. | Return should reveal a useful capability and remembered consequence; reward/story part remains next. |
| [Into the Breach publisher description](https://store.steampowered.com/app/590380/Into_the_Breach/) | Telegraphed attacks and protecting civilian buildings. | Make danger and command response readable before damage. This part exposes movement and matches contact presentation to execution. |
| [FTL publisher description](https://store.steampowered.com/app/212680/FTL_Faster_Than_Light/) | Crew orders, power/target decisions and several responses to a crisis. | Tactical interventions and alternative plans need clear costs/benefits. No change to turn-based or pause combat is introduced. |
| [Despot's Game publisher description](https://store.steampowered.com/app/1227280/Despots_Game_Dystopian_Battle_Simulator/) | Prepare/equip warriors who fight autonomously; perks/class combinations and changing dungeons. | Keep heroes executing while Master observes and intervenes, instead of requiring constant buff maintenance. |
| [Pick Me Up publisher description](https://tapas.io/series/pick-me-up/info) | Low-rarity hero treated as expendable material for learning boss patterns. | Original hero agency and consequences; publisher text does not establish our numerical formulas. |

Fetched12/11/2026. Subset's own ITB/FTL pages returned403 challenges; accessible publisher Steam descriptions were used instead. No player-review/retention data or inaccessible Fandom facts are asserted. [Research metadata](research/comparison-index.json) records statuses and separates verified descriptions from our proposals. Existing community wiki synthesis is supplementary and not newly verified canon.

## Self-critique and staged next work

The input and combat images now communicate more clearly. This alone does not fix the journey's repeated transit, dominant-looking choices or the second identical sword. The initial vector-only skill proposal still looked too schematic in native images, so original animated PNG effects replaced it before handoff. Independent review also caught melee inspection ambiguity; nearest-actor picking and a regression now cover it.

1. **Part 2 — decisions and encounter payoff.** Coordinate after PR #96: cut repeated camp transit, give assault/forced entry useful tactical benefits, teach a first dodge safely in actual play, make two plans defensible for different HP/rosters, and surface rescue/recovery consequences. Acceptance requires real alternative routes and an observed new-player decision, not more waits/HP.
2. **Part 3 — reward-to-next-run loop.** Show real item-ID comparison/equip and explicit retry care, prototype three one-slot doctrines with actual combat hooks, then review pity/targeted progression and versioned guest/cloud persistence separately. The prior desk-model economy is not shipped by this PR.
3. **Part 4 — remaining Tower visuals.** Reconcile hero/enemy pixel density, additional hero and upper-floor animations, environment/materials and canopy interaction with the relevant owner. This part does not replace shared Sanctuary/map art or claim every game animation is finished.

After each part: actual runtime evidence → independent critique → targeted correction (at most three review cycles) → repeat impacted checks. Five uncoached first-time phone sessions should record whether players can name a consequence, use a reward and volunteer a concrete next-run plan. Until that evidence exists, gameplay acceptance stays open. No automatic merge, main push, force push or production deployment.
