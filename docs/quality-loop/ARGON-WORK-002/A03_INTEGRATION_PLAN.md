# ES-A03-002 — Argon V2 runtime integration / WIP

Authority: Issue #78 latest A01 `[FILE_OWNERSHIP_OK]`; A08 whitelist Issue #77,
source `db45a8dfed7577213ecea9052f49571faf5b1a1a`. Sole writer A03, Arena session
788e635f / @lightingfff. Existing branch fixed by platform:
`arena/788e635f-eternal-sunmmoner`. Existing audit PR #67 must not be rewritten;
its four Markdown files remain unchanged. Separate head branch/PR is unavailable
in this session; report this constraint rather than inventing another branch.

## Scope and baseline

Main db45a8d merged into session branch without rewriting remote history.
Changes inherited from main (including its Gradle/settings changes) are NOT A03
edits. Runtime delta touches only four authorized Kotlin files, 20 new PNGs,
the new SanctuaryArgonV2RenderTest and this document. Original A03 audit retained.
No camera, map, collision, save, combat, UI, workflow or legacy PNG writes.
No VFX/candidate imported under app assets.

## Implementation

- Copy exact batch 01/02/03 PNG bytes, verify all 20 against source manifest SHA.
- SanctuaryArtAssets init preloads dimensions-checked optional images including
  negative entries. Runtime getters do no Argon decoding. Families fail as a whole
  to existing P27B/P03/procedural selectors. Four cached 512-square ground mosaics
  preserve the existing low-zoom batching path (about 4 MiB ARGB, excluding source
  PNGs/composites/GPU). A10 must measure actual device memory/frame pacing.
- PixelTileAtlas gives new sheets/families priority, disables legacy mirrored
  sheet mixing. Grass 3 variants, earth, stone 2 variants. Legacy art untouched.
- Transition: source inclusive intervals 0..19,20..43,44..63 become exclusive
  0/20/44/64. Nine fixed blits, not repeating the whole source tile. Current
  implementation stretches each segment; screenshot review must assess stretch.
- SanctuaryWorldRenderer changes only ground material within the existing Tower
  approach: nine-slice curb and clipped runic stone interior. No path changes.
- PixelStructureAtlas returns cached same-canvas foliage and facility composites
  through existing sprite selectors, preserving draw order, obstacle identity,
  canopy/shadow consumer and scale. Pine/hedge/forest bush/planter get new art;
  six architecture modules attach to quarters/restaurant/armory/summon/gate.
  Composition is cached on first selection; no Argon image decode in draw loop.
- Imported ivy and wayfinder are **not yet placed**: independent upright overlays
  need a safely depth-sorted anchor hook, not a foreground append that breaks
  actor occlusion. Do not claim all 20 assets are visibly integrated.

## Evidence and tests

Local source import: 20/20 SHA-256 checks match immutable source. Whitespace check
`git diff --check`. New Robolectric native Compose Canvas test adapts the existing
P27B engine harness, renders paired before/after at .36/.88/1.48 and day/night
architecture, and checks warmed selector identity/fallback/projection.
Execute with:

```
gradle :app:testDebugUnitTest --tests com.example.SanctuaryArgonV2RenderTest --tests com.example.P27BAssetContractTest --tests com.example.SanctuaryMaterialTest
gradle :app:assembleDebug
```

Outputs (only when executed):
`app/build/visual_screenshots/argon_v2/world/{before,after}_*.png`.
These are native engine renders, not Android screenshots or physical-device FPS.
Existing P27B CI selects its own test classes, not the new A03 test: do not claim
new paired harness executed just because APK/P27B CI is green.

## Current limitations / gates

Local environment lacks java, gradle, adb and Android SDK. Builds/tests/captures
NOT RUN locally. No fake screenshots, image quality PASS or FPS numbers.
Integration is **PARTIAL, not DONE**, pending actual compile, regression and
same-SHA engine images inspected by A03/Argon, actor/door/canopy evidence,
A09 independent tests and A10 device profiling. Potential visual risks: dark/noisy
grass, family boundary seams, nine-slice stretch, facade registration/clipping,
first-selection composition CPU/cache memory. Foliage fits uniformly into old canvases at the original visible foot, using authored pivots;
pivot/visible feet and shadow agreement require real scene review.

Review requests: A10 #79, A09 #80, Argon and Codex on exact head SHA. No merge;
any corrective work follows serialized one-writer FIX_PLAN, max three rounds.

## Round 1 static critique / targeted fix

Inspected actual source PNG hero_quarters and new door in the file viewer (asset
inspection, NOT game evidence). A full 64×80 overlay would cover too much of the
existing detailed facade. Reduced architectural modules to nearest-neighbor .5
local pixel scale and paired roof accents rather than painting one large central
roof. Registration remains provisional until actual engine captures. All 20 PNG
chunk CRC/IEND and zlib IDAT inflation checked locally; tools/validate_assets.py
returned PASS with existing preview-bloat, google-services-missing and duplicate
basename warnings. These do not establish APK/runtime quality.

## Round 2 — real CI regression / corrective selector boundary

APK build PASS on `36c0df8`: run 42492024059. P27B regression run 42492024008
failed `adjacentMosaicIndicesAndOverviewUseTheSameWorldCoords` (expected16,
actual3), per check-run annotation 128766305929. Engine captures were skipped.
Root cause: replacing the public legacy tile family broke its 4×4 contract.
Correction: retain legacy P27B getters and add explicit world* material getters
used only by SanctuaryWorldRenderer, preserving legacy family/block tests and
fallback. No legacy test edits. Must rerun new SHA; previous APK PASS does not
prove new-head success. Full logs endpoint blocked by outbound host policy;
GitHub check annotations provided exact assertion. No runtime images available.
