# Art completion — Issue #117

Implementation branch: `codex/art-completion`. Baseline production main:
`e858f1422b4e1f45a08b2f42c36373882d1367d4` (owner merged #116).
The subsequent #114 merge adds reference documentation only. This task neither
rewrites #116 nor changes the frozen Argon #67 branch.

## Coverage

| Category | Actual preferred runtime path | Retained fallback / boundary |
|---|---|---|
| Six hero classes | Existing named Kaelen/Lyra/Vesper banks for Knight/Archer/Mage; three new Assassin/Priest/Berserker banks, 72 additional original poses and three portraits | Named identities retain precedence; unknown names receive the correct class. Original procedural bodies remain available when optional banks fail. |
| Eight enemy archetypes | Original four atlases plus Wraith/Treant/Sentinel/Executioner: 48 additional original poses | One facing stored, mirrored at draw time; existing procedural fallback remains. All floors can use authored art. |
| Execution effects | Eight distinct styles / 64 PixelFX frames: guard, arcane, cleave, holy, shadow, impact, shield impact, slam | Original packs then vector effects on failed assets. Telegraph/ground rings intentionally remain vectors. |
| Travelling projectiles | Five original flight rows, four poses each: bolt, arrow, arcane, holy, hostile arrow | Simulation positions, lifetimes and collisions unchanged; old vector fallback retained. |
| Monumental entrance | Original richer court, three-gate Nexus and engraved doors | Existing opening masks, six-second presentation, skip, portal destinations and UI visibility unchanged. Whole-pack fallback avoids mismatched scenery/doors. |
| Sanctuary construction | Cliff strata, masonry wall/gatehouse faces, canal banks and aligned carpet use four original face-mapped textures | Same silhouette, step geometry, water, building coordinates, entrances, navigation and lighting. Upright height is not foreshortened twice. |
| Outdoor Tower terrain | Existing material banks plus continuous roads in all five outdoor biomes | Existing path coordinates and widths retained; no new route/collision changes. |
| Props | Original statue, crystal and banner | Original getters/generators remain fallbacks. Shared foot anchors and shadow lookup. |
| House interiors | Eight original furnishings: anvil, armor stand, chest, care table, long table, bookshelf, tavern counter, recovery bed | Same rooms, interactive props and house logic. Native indoor shadow pass now also runs in the actual Lobby screen. |
| UI | Actual HUD avatars, hero cards and mini portraits use class-aware portraits | Existing panel polish from #116 retained. No HUD layout, joystick or battle input behavior rewritten. |

This is coverage of the audited gaps, not a claim every legacy icon, procedural
fallback or already validated building/tree has been redrawn. Existing vegetation,
architecture and atmosphere remain where they already have authored depth. No
proprietary manhwa image is used. Sources are genuine hosted image-generation
outputs or offline exports from the pinned MIT PixelFX code, not mock screenshots.

## Authoring and reproducibility

Sources, prompts, per-pack normalizers, roots, clipping/alpha/frame/hash reports,
contacts and animation previews live in the corresponding subdirectories here.
Generated sources are normalized derivatives, not claims of native target-size
generation. Body scale and anatomical roots are preserved across each strip;
there is no per-frame bounding-box fit. Left images are runtime reflections, not
separately authored facings. Baked sprite lighting therefore cannot represent a
fully relit 3D model.

Use `python3 tools/art/verify_completion_art.py` for independent integrity checks.
The class/enemy/prop/interior normalizers require Python, Pillow, NumPy and SciPy.
Surface/projectile builders use Pillow; the offline PixelFX exporter uses Node.
None is shipped as runtime code. PixelFX remains pinned at
`5294518ce7c72e48b6100a744596e0acf57f2826`; the five added modules are byte-identical,
their hashes are in `effects/upstream-additions.json`, and the original 28 vendored
files/notices are unchanged. No provider runner, account or paid API was activated.

The project `ai-game-spritesheets`, `pixelfx-eternal` and director instructions were
used for production. Newly merged `character-asset-pipeline` / Meowa alignment and
speed/trim guidance was also read during final QA: fixed anatomical anchors,
independent effect layers and simulation-owned release/contact remain authoritative.
Source anticipation/hitstop never becomes a second damage event or extra delay.

## Technical contracts and corrections

Kotlin/Compose native Canvas, ground Y scale .72, upright sprites, nearest sampling,
depth sorting, canopy occlusion and world-light shadows remain. No game model,
save schema, mission outcome, command handling, map layout, Android identity,
Gradle/manifest/signing/entrypoint or workflow file changes are included.

Optional banks publish completely, cache failures and are bounded; no draw-time PNG
decode/network. Shared geometry/shadow caches are warmed only in serialized Lobby
initialization, not Tower IO preload. The effect pixel bank is published after its
metadata and acquired before row selection. Existing monsters share right/left
pixels with body/local-shadow reflection while keeping world light direction.

QA found and corrected: opaque RGB gate fallback falsely rejected as not
premultiplied; known-name portrait identity incorrectly depending on optional
asset success; generic Knight receiving named Kaelen's delayed contact visuals;
tall Wraith HP bar overlapping its head; ungrounded new interior furnishings in
the actual screen; dead hero idle aura/empty world HP bars. UI tests now dispatch
physical pointer events and label arranged component galleries accurately.

## Memory and performance limits

Listed pixel banks total **27,570,400 bytes (26.29 MiB)** versus the previous listed
21,078,688 bytes. This excludes legacy/UI caches, shared paths, transient source
decodes, Java/native overhead and GPU copies: it is not total process memory.
Original enemy + new effects + projectiles = **6,021,120 bytes**, within the
unchanged 8 MiB guard. Extra classes 3,858,432 and extra enemies 3,538,944 each
have their own 4 MiB guard. Gate bytes stay 4,285,440; new surfaces 1,048,576;
props 56,192; interiors 225,984. No duplicate mirrored enemy bitmaps are retained.

A fresh native-host test measured Lobby init **508.38 ms**, first Tower decode
98.86 ms, first gate decode 32.49 ms, warm preload average .0056 ms. Synchronous
first Lobby initialization is still a launch-stall risk needing Android profiling;
this task does not claim to eliminate it. Culling/caching avoid per-frame decode.
No attached device was found; physical install/update, FPS/GPU/heat/battery and
human creative/play acceptance remain **NOT TESTED**. Repeated grass texture and
mirrored baked lighting remain visible limitations, not promises of true 3D.

## Evidence and review

See [native captures and exact commands](../../visual-evidence/art-completion/README.md).
Arranged actor/biome/component fixtures exercise production rendering, not unlocked
player progression. Guest battle, pointer, save and retry tests are reported
separately. Native Skia captures are not phone screenshots or a human playtest.
External Argon/A08 creative acceptance and A09 independent QA are requested on
the final exact SHA; this author cannot supply those approvals. Keep REVIEW,
with owner-only merge.
