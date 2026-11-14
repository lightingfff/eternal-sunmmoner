# Superpowers reference library

Source: [obra/superpowers](https://github.com/obra/superpowers), pinned at
[`22f0b463e7a5e579904dd6211a520321035526f7`](https://github.com/obra/superpowers/tree/22f0b463e7a5e579904dd6211a520321035526f7),
package version **7.0.2**. Read and captured **2026-11-14, Asia/Saigon**.
MIT © 2025 Jesse Vincent; original [license](upstream/LICENSE) retained.

The owner asked to learn this library for ETERNAL SUMMONER. All **15 main
SKILL.md texts** were read. This is a reusable source archive and original project
application guide, not model-weight training, global installation, a running plugin
or proof that automatic skill activation works in the Codex App.

- [Catalog of all 15 skills](CATALOG.md)
- [Project application, differences and inspected script side effects](READING_NOTES.md)
- [Project adapter](../../../.agents/skills/superpowers-eternal/SKILL.md)
- [Pinned inventory / hashes](inventory.json)

## What is retained

**90 original files, 487,203 bytes**, copied byte-for-byte: every tracked file under
upstream `skills/` (80 files, including all local helper/reference/template files),
`hooks/` (4 files), README, LICENSE, package.json, the Codex plugin manifest and
the packaging/synchronization script sources. All files are ordinary non-executable
documentation copies. Their contents have not been edited to change upstream rules.

The rest of the upstream repository is excluded: Git metadata, tests/evals, assets,
other platform plugin implementations, historical docs and dependencies. Upstream
README links to excluded directories or external repositories do not imply they
were copied. Full support bytes are retained for each skill, but not every support
script was individually audited. Main-text reading and selective source inspection
are not a complete behavioral/security audit or an upstream eval-harness run.

## Using the learning

The existing project skill router now points to `superpowers-eternal`. Future work
can select root-cause investigation, meaningful regression checks, explicit plans,
scoped independent review and evidence before completion without importing the
upstream bootstrap's automatic invocation rules. Existing authorized execution,
exact-SHA independent re-review and user-only merge remain authoritative.

No install/update command, hook, visual companion server, telemetry request,
diagnostic transcript access/export or source synchronizer was executed. No global
`~/.codex` / `~/.agents` configuration was changed. No upstream issue or PR was
opened. No app, asset, Gradle, Manifest, workflow, save, combat or map file changed.

## Verify

From the game repository root:

```sh
python3 docs/skill-library/superpowers/verify_snapshot.py
```

With the original checkout available, also verify the pinned revision, every
selected tracked path and byte equality against it:

```sh
python3 docs/skill-library/superpowers/verify_snapshot.py --upstream /workspace/obra-superpowers
```

For this task's scope/append-only check, add
`--base 507973cbceb258013bb585e261d38d3f38714966`. This optional change-review check
is separate from the reusable snapshot check, so later unrelated game changes
do not invalidate the archived source.

The stdlib-only verifier reads files and emits JSON; it never imports/executes
archived code, makes network calls or writes configuration. It checks source
hashes/coverage/license, the 15-entry catalog, authored local links and router
append-only integration. A documentation-only check is not an Android build or
proof of agent behavior under pressure. Independent review is requested on the
final commit; external Argon/A09 review remains pending until actually performed.

Authored files pass `git diff --check` with the `upstream/` reference directory
excluded. Full-diff whitespace checking reports four original trailing-space
lines in upstream README (38,40,42,352); those bytes are deliberately preserved
and verified against the pinned source, rather than silently rewriting it.
