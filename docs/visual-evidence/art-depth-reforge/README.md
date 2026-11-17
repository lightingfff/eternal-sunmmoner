# Native renderer evidence — art depth reforge

Before: unmodified runtime from `main@380e4e3ba4840e673e54966715ff12a49cbdfab4`.
After: this branch's production renderer/assets. Review the exact PR HEAD SHA.
Captures use native Skia via Robolectric SDK36, software rasterization. They are
not mockups, screenshots from a physical phone, GPU benchmarks, or human-playtest
proof. Specimen scenes deliberately arrange actors/effects; `production_*` files
come from the existing fresh-guest Tower screen/input tests.

| Evidence | Files |
| --- | --- |
| Paired architecture, day/night, three zooms | `before/world/`, `after/world/` |
| Paired meadow, enemies, hero and simultaneous VFX | `before/tower/arranged_*`, `after/tower/arranged_*` |
| Real production guest sequence / short landscape command | `after/tower/production_*` |
| Armory roof/front/side, doorway and partial tree occlusion | `after/architecture/` |
| Three authored heroes, front/back and both horizontal facings | `after/hero-ui/heroes-production-directions.png` |
| Actual800x360 portrait cards with verified tap actions | `after/hero-ui/authored-hero-cards-800x360.png` |
| Fixed-world-light mirrored caster | `after/hero-ui/shadow-mirror-*.png` |
| Synchronous asset initialization, host only | `after/hero-ui/asset-init-host.json` |

Inspected results: Hall roof, column recesses and terrace thickness read clearly;
extra slabs/old AO stains are gone. Heroes preserve their feet and silhouettes;
left-facing shadows still lean in the same world direction. Roads have continuous
rounded edges without the initial union holes or fractional-pan tile hairlines.
Cards show original portraits and readable dynamic names/stars. Grass still has a
visible repeating macro pattern and the unchanged island cliff is comparatively
flat; these remain visual limitations rather than hidden acceptance claims.

## Actual checks

`test-summary.json` records72 distinct passing tests and the successful command
groups. A repeated case counts only once. Tests run on the production sources
included in this delivery; later edits only add documentation/evidence. Commands
use JDK21, Gradle9.3.1 and SDK36.1. Existing Gradle/Manifest/signing/entrypoint files
are unchanged.

```sh
gradle :app:testDebugUnitTest --tests com.example.ArtDepthAssetsTest --tests com.example.P28DRenderTest --tests com.example.MapForgeArchitectureRenderTest
gradle :app:testDebugUnitTest --tests com.example.TowerCombatArtEvidenceTest --tests com.example.ui.P28UiEvidenceTest --tests com.example.ui.LayoutResponsivenessRobolectricTest
gradle :app:testDebugUnitTest --tests com.example.ArtDepthFallbackTest --tests com.example.ui.ArtDepthUiEvidenceTest --tests com.example.ui.LobbyHeroTouchTest --tests com.example.ui.RescueRetryFlowTest --tests com.example.data.LobbyHeroPersistenceTest --tests com.example.data.GuestSavePersistenceRobolectricTest
gradle :app:testDebugUnitTest --tests com.example.ArtDepthRenderContractTest --tests com.example.TowerCombatArtEvidenceTest --tests com.example.ArtDepthFallbackTest
gradle :app:testDebugUnitTest --tests com.example.ui.SanctuaryHudContrastTest --tests com.example.ui.SanctuaryUiTokenTest --tests com.example.TowerCombatArtEvidenceTest
gradle :app:testDebugUnitTest --tests com.example.ArtDepthColdStartTest :app:assembleDebug
python3 tools/art/verify_art_depth.py
```

The asset tests require real pack selection, hashes/dimensions, warm decode counts,
material continuity, byte caps and original combat contact selection. Degraded
asset packages exercise corrupt PixelFX→old PNG, both missing→real vector drawing,
and partial hero preparation→no published roster. Native tests sample rounded
junctions/zero-length caps and fractional pan at all three zooms, and test that
mirrored shadows retain world-light direction. Existing UI/input/save/retry tests
cover the affected controls and persistence behavior.

Two real defects were caught and fixed during refinement: winding-dependent road
holes and faint alpha seams at fractional material-cell boundaries. An initial
fallback test also needed process-lifetime loader isolation because Robolectric
reuses application static state. These failed runs are superseded by passing
targeted reruns; their failures are not counted as success.

| Retained decoded pixels | Bytes |
| --- | ---: |
|16material families plus grass macro |4,456,448 |
|3hero bodies/portraits |3,858,432 |
|Retextured Hall |220,832 |
|PixelFX bank, unchanged from original VFX size |1,179,648 |
|Existing enemies + VFX |8,257,536 (under8MiB) |

These are bitmap-bank figures, not total-process/GPU/geometry-cache memory. The new
optional materials/heroes/Hall add8,535,712 retained pixel bytes before considering
avoided legacy HD copies. Independent total-memory/device profiling is pending.

The existing12-frame host world probe recorded baseline avg32.17ms/p95=33ms and
after avg27.42ms/p95=28ms at1280x720,z.88,day. This small host sample is indicative
only; it does **not** establish phone FPS improvement. Separate fresh host asset
initialization measured394.12ms;100 warm init calls averaged.00193ms. Initialization
is synchronous and could affect cold launch on lower-end phones.

Local APK:42MiB, package `com.aistudio.eternalsummoner.kvrpzm`, minSDK24/target36.
SHA256: `e40e572037283f991625c7c6011f44f6e764900fb129952a2d9852f71959b7dd`.
`apksigner verify --print-certs` passed using the existing local debug key.
Certificate SHA256: `b6ae9520912a8fc2308abfc299c34c593b237c9b15bb7b1b766471eb8180109f`.
This does not establish compatibility with every APK previously downloaded from CI.

ADB listed no connected device. Physical install/update, FPS, GPU time, heat and
battery: **NOT TESTED**. CI results and exact-SHA independent review are reported
in the PR/issue handoff, not invented here. This branch remains review-only; no merge.
