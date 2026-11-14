# PixelFX integration

Pinned source: [patch-gg/pixelfx](https://github.com/patch-gg/pixelfx/tree/5294518ce7c72e48b6100a744596e0acf57f2826).
The catalog contains244 effects across21 categories; this delivery applies four,
not all244. Source inventory and hashes are in `inventory.json`. Selected MIT
source and its required export helpers are in `tools/art/vendor/pixelfx`.

| Game execution style | PixelFX source |
| --- | --- |
| Guard | impacts/shield-block |
| Arcane | arcane/mana-burst |
| Cleave (and existing shadow fallback row) | melee/sword-slash |
| Holy | holy/heal-aoe |

`node tools/art/export_pixelfx.mjs` exports original execution clips; then
`python3 tools/art/build_pixelfx_atlas.py` produces the bounded Android atlas.
The upstream stock actor and hitstop are excluded. Damage, simulation timing,
cooldowns and save data do not change. The APK contains exported PNG/manifest and
attribution; it does not execute Node or download effects at runtime.

See the [project adapter](../../../.agents/skills/pixelfx-eternal/SKILL.md),
[license](LICENSE), [attribution and exclusions](NOTICE.md), and preserved upstream
[authoring guide](upstream/AUTHORING.md). Node18+, Python3 and Pillow are sufficient
for the selected export; no `npm install`, external provider or API key is needed.
