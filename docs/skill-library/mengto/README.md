# MengTo Skills reference snapshot

Owner request: learn https://github.com/MengTo/Skills.git (2026-11-14, Asia/Saigon).
Pinned source: `07274a3b3dba5f6a311186ddaf65765d94f78cec`.
License: [MIT](LICENSE), copyright2026MengTo. Source LICENSE is retained verbatim;
all retained files under `upstream/` preserve their exact original bytes.

## What was learned and saved

[Project adapter](../../../.agents/skills/mengto-game-design/SKILL.md) routes the useful
game/combat/UI/review methods to Kotlin/Compose, existing pixel art and .72projection.
[CATALOG.md](CATALOG.md) indexes183skill names/descriptions across9categories.
Ten selected main texts were read for this task:

- author-game-levels, design-action-combat, build-game-monster-system, tune-enemy-ai,
  test-playable-web-games;
- game-dev-combat-animation, game-dev-combat-skill-polish;
- no-ai-design-slop, audit-ai-design-slop;
- workflow-score-to-target.

The monster contract and skill art/readability rubrics were also inspected. This is not
a claim that every body/reference/tool was read or that any game scored better already.

All183SKILL.md texts are saved as inert references. Complete supporting files are retained
only for the ten selected folders (61files including their ten entrypoints). Other skills'
scripts/articles/assets/references, upstream demo site/screenshots/root automation and
large preview images are not bundled. `upstream/README.md` can link to these online-only
source materials. Root CLAUDE.md is not imported as ETERNAL SUMMONER instructions.

[inventory.json](inventory.json) records selected folders/review depth and hashes/sizes
of all236retained upstream files (about1.6MB). This README, adapter, catalog, verifier and
inventory are project additions, separate from unmodified MIT source. No upstream
installer, Node/Swift/Python capture tool, engine, provider or demo was executed/installed.

## Verify and update

From project root:

```sh
python docs/skill-library/mengto/verify_snapshot.py
```

The offline standard-library verifier checks exact snapshot file set, hashes/sizes,
MIT license identity,183skill headers, selected supporting-file completeness, Python/JSON
syntax where applicable, project links and selected entrypoints' static relative Markdown
resources. It does not execute source scripts, validate WebGL APIs, render a game or score
art. Root README and unselected bodies can reference excluded source material; consult
the pinned upstream for those resources before selecting them. Hashes detect recorded
snapshot drift, not cryptographic publisher authentication.

For updates, review a new upstream commit/license, preserve its source bytes, refresh the
inventory/catalog, verify resources and request review. Do not silently follow moving main.
No game/runtime/art/Android configuration changed in this learning task. A local Android
build/device test is not needed for reference-only changes; automatic CI is reported
separately if it runs. Lightweight independent review remains pending until performed.
