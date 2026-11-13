# Game-development reference library

Owner request (2026-11-13, Asia/Saigon): learn
https://github.com/gamedev-skills/awesome-gamedev-agent-skills.git alongside Sprite Forge.

Source revision: `c6ca6c41c839b68319ffc08430b99ab768debc32`.
License: [Apache-2.0](LICENSE), copyright 2026 Abhishek Barali and contributors;
original [NOTICE](NOTICE) retained verbatim. All files under `upstream/` are unmodified
source bytes. This README, adapter, catalog, inventory and verifier are project additions.
They do not change upstream licensing or imply endorsement.

## Contents and actual review depth

- [Project adapter](../../../.agents/skills/gamedev-agent-skills/SKILL.md): task routing,
  native Android bindings, project overrides and required evidence.
- [Catalog](CATALOG.md): 79 specialized skills plus one router. Ten specialized entrypoints
  read: game-feel, level-design, roguelike, rpg, game-ai, ai-behavior-trees-utility-ai,
  input-systems, save-systems, game-ui-ux and performance-optimization. Router and custom-engine
  guidance also read. Remaining entries are indexed; ancillary files are not all reviewed.
- `upstream/skills/` and `upstream/router/`: complete supporting files for these trees,
  including scripts retained as reference; no blanket activation or execution.
- `upstream/LICENSE`, `NOTICE`, `README.md`, `docs/VERSION-SUPPORT.md`: source provenance
  and version context. Other root docs, tests, site, evals, release automation and plugin
  manifests are excluded. Root README can link to those online-only materials; this is
  not a standalone upstream checkout or installed game engine.
- [inventory.json](inventory.json): source pin, review depth, SHA-256/size of 256 retained
  files. Duplicate LICENSE/NOTICE beside this README remain byte-identical to the originals.
- [verify_snapshot.py](verify_snapshot.py): offline standard-library integrity/resource check.

## Verification

From repository root:

```sh
python docs/skill-library/gamedev-agent-skills/verify_snapshot.py
```

Checks exact snapshot file set, hashes/sizes, 80 skill headers, Python parsing/JSON syntax,
entrypoints' literal bundled resource paths and project adapter/catalog links. It does not
execute engine examples or validate their API claims. Hashes detect recorded-snapshot drift,
not publisher authenticity. For an update, review a new commit/license, preserve source
bytes, regenerate inventory/catalog and request review; do not silently follow moving main.

No game logic, art, Android configuration or dependencies change. No paid APIs, model
training, engine installation, generated assets or device measurement occurred. No local
APK build is required for this reference-only change; automatic CI status is reported
separately on the PR. Independent review remains pending until actual reviewers assess
the exact SHA. Saving project guidance does not guarantee memory in unrelated chats.
