# Agent Sprite Forge — installed project guidance

Owner request 13/11/2026: learn https://github.com/0x0funky/agent-sprite-forge.git.

Pinned source: `6b3da5c444363378e20f3fce130ebecc22f343dd`, reported package version 0.5.0.
MIT © 2026 0x0funky; [LICENSE](LICENSE) and [third-party notices](THIRD_PARTY_NOTICES.md) retained.
This is a repository-local installation and adaptation, not model training or a promise
of cross-chat memory, installed remote providers or new game assets.

## Installed

All five upstream folders are unmodified in `.agents/skills/`: `generate2dsprite`,
`video2dsprite`, `generate2dmap`, `generate2dmedia`, `codeart2d`. Their 171 files have exact
SHA256/size provenance in `.agents/skills/.agent-sprite-forge.install.json`.
The root MIT license/notices are here rather than repeated inside every skill.
Existing project skills and global host skill directories were not overwritten.

Read [the ETERNAL SUMMONER adapter](../../../.agents/skills/agent-sprite-forge/SKILL.md)
first; AGENTS.md and the router point to it. It keeps Kotlin/Compose and camera ground Y=.72,
existing anchors/collision/save/contact timing, pixel finish and cached PNG runtime.
Upstream HD defaults, foreign-engine exports, example hit timings and provider consent
conventions do not supersede user/project instructions. Use available hosted image tools
for authorized image work; do not automatically launch paid/nested media generators.

## What was learned and reviewed

Read the README, all five SKILL.md entrypoints, sprite processing/runtime integration,
installer source, offline capability doctor, license/notices and the QC entrypoint.
Did not audit every one of the83 Python modules or run all upstream tests/provider routes.

Useful workflow: establish a consistent master, process one action at a time, keep shared
registration/scale, retain intentional jumps/recoil, detect spill/alpha/leg alternation,
review loop transitions at game size, then export frames with explicit pivots and events.
The library's character image-to-video lane is conditional on a real available route;
FX/icon/prop sheets and previously authored sheets remain useful inputs.
Detached weapons/sparks must not be silently removed by a largest-component filter.
Clocks, root geometry and simulation damage stay separate from preview/video FPS.

## Checks actually run

- Reviewed upstream installer and used `--apply --dest <project>/.agents/skills`;171files
  installed with no overwritten skill folders. `--check` reports **in-sync**.
- Parsed83upstream Python modules and24 JSON files without executing their generation code.
- Offline `forge_doctor.py --no-exec --host-tools image_gen,image_edit` reports overall OK
  for the local processor/install; it also lists the optional MISSING/UNKNOWN checks
  verbatim in [verification.json](verification.json). OverallOK does not mean all routes exist.
- numpy 2.3.5, Pillow 12.3.0 and scipy 1.17.0 already present; no pip/package installation.
- ffmpeg 7.1.5 found; the local video processor doctor output is retained in
  [video-doctor.json](video-doctor.json). This is local codec capability, not video generation
  or Android playback/latency/FPS evidence.
- Ran `sheet_qc.py spill` on the existing normalized goblin atlas: **PASS**, no warning/fail.
  This smoke check explicitly used a 5% safe margin (upstream default 15%) to match this
  existing pack. [QC](raider-spill-qc.json) and [inspected overlay](raider-spill-overlay.png).
  It checks slicing/spill/alpha margins, not whether animation anatomy/motion is finished.
- Checked adapter/router local links and confirmed no app/runtime/art/Androidconfig diff.
- Full diff whitespace check reports three upstream EOF blank lines in the codeart2d,
  generate2dmap and video2dsprite copies of `forge_schema.py:526`; retained verbatim
  for source hashes. The project-authored adapter/routing/docs whitespace check passes.

Current optional gaps: resvg-py SVG rasterizer absent; direct provider API keys and Grok
CLI absent; Codex CLI media proof unverified; no new image-to-video generation route.
The hosted image generation/editing tool is available. No provider calls, quota purchases,
new creative asset generation or Android modifications were made for this setup.

## Reproduce/update

With a checkout of the pinned upstream commit:

```sh
python /path/to/agent-sprite-forge/tools/install_skills.py --check --dest "$PWD/.agents/skills"
```

A deliberate update uses the same command with `--apply` after source/license/diff review;
the upstream installer backs up only the Forge folders it replaces. Retain the project
adapter and routing. A fresh agent session may be needed for automatic skill discovery;
the explicit AGENTS route remains available. Do not run the default host install into
read-only global directories. Do not run provider-generation commands just to validate
installation. No Android build is required for skill/docs-only setup; any automatic PR
CI build is reported separately.
