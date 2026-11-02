# P17-C — Lyra runtime visual QA, portrait, bow VFX, Lobby perf audit

Stage after the merged P16+P17-B work (PR #19, merge commit `3136780`). This page records
what was actually executed, with the honest limits of a sandbox that has no Android SDK,
emulator or device.

## 1. Real engine render (not a mockup)

**Method.** `app/src/test/java/com/example/LyraEngineRenderTest.kt` runs under
`RobolectricTestRunner` with `@GraphicsMode(NATIVE)` (Robolectric 4.16.1, SDK 36, native Skia).
It calls the same functions the Lobby uses:

```
drawSanctuaryFloatingWorld(...) → drawGroundShadows(...) → drawProp(...) per depth-sorted
obstacle → drawCharacterSprite(heroName = "Lyra", heroClass = ARCHER, ...) →
drawUpperCanopyOcclusionPass(...)
```

Every pixel in the PNGs below came out of those calls. Nothing is pasted or composited.
The test also asserts, on the runtime atlas: pivot (32, 90), `worldPixelScale` 1.08, distinct
frames for idle/walk/attack/skill/hit/rest/fear/victory, four distinct facing rows, the
packaged PNG's SHA-256 against the manifest entry, and that the lowest opaque row of every
sampled runtime frame is exactly the pivot row (feet never drift).

**Where the images live.** The runner commits them to the branch because CI artifact and log
hosts (`*.blob.core.windows.net`, `results-receiver.actions.githubusercontent.com`) are not
reachable from the authoring sandbox:

| File | Content |
| --- | --- |
| `docs/visual-evidence/p17c/engine-render/lyra_zoom_036.png` | overview zoom 0.36 |
| `…/lyra_zoom_088.png` | default gameplay zoom 0.88 |
| `…/lyra_zoom_148.png` | close zoom 1.48 |
| `…/lyra_four_directions.png` | four facings side by side, same ground line |
| `…/lyra_clip_states.png` | attack / skill / hit / walk / fear / victory in one scene |
| `…/lyra_walk_cycle.png` | four walk phases for foot-slide review |

**Not** evidence of: device frames per second, thermal behaviour, DPI scaling, or touch input.
Robolectric rasterises in software at density 1.

## 2. Lyra HUD portrait

Before: `PixelHeroPortraitAvatar` cropped `srcOffset 19,4 / 26×26` out of **Kaelen's** atlas for
every premium hero, so Lyra's card showed a knight's helmet.

Now: `app/src/main/assets/premium_slice/portraits/lyra_portrait.png` — an original 64×64
pixel bust authored by `tools/generate_lyra_portrait.py` with the same palette and the same
upper-left key light as the world sprite (red hair with fringe and braids, amber 2 px eyes,
olive cloak, brass pauldron, oxblood sash, recurve stave at the silhouette edge, transparent
background). 64×64 native means the 48 dp avatar stays crisp at 2×–3× density instead of
stretching a 26 px crop.

Loader: `PremiumPortraits.get(heroName)` reads `manifest.json → portraits`, resolves through
`SanctuaryArtAssets.asset()` (decoded once, cached) and memoises the result — including
"no portrait" — keyed on the raw display name so the HUD draw path allocates nothing per
frame. Any hero without an authored bust keeps the old crop path.

Preview: `docs/visual-evidence/p17c/lyra_portrait_6x.png` (inspection) and
`lyra_portrait_hud_preview.png` (48/40/32 dp frames — art preview, **not** a runtime screenshot).

## 3. Bow-release event / VFX

Reconfirmed basis: the P17-B markers in `art/p16_lyra/pack/lyra_pack_manifest.json` are
**absolute atlas columns** (attack release 19 = attack frame 5, skill release 26 = skill
frame 4, charge 23–25, impact 32, walk contacts 8/12). The runtime `PremiumActorAtlas`
selects frames by *seconds inside the clip*, so the markers are not frame indices and are not
consumed directly.

What changed (`AutonomousHeroAi.bowReleasePoint`): an archer's arrow now leaves from the bow
hand instead of the hero's centre.

```
before: x = hero.x,          y = hero.y - 16f      // torso, knee height
after : x = hero.x + dx/d*12, y = hero.y - 24f + dy/d*12*0.36
```

The battle renderer already lifts projectiles by `elevationZ = 28f`, so the arrow now flies at
roughly the bow's height (~52 world px above the ground row) and steps forward along the aim
line. **Damage, cadence, cooldowns and hit resolution are untouched** — the projectile still
resolves through the same 28 px proximity check; the flight is ~0.03 s shorter.

Live state wiring that already exists and now feeds Lyra's clips: `fear` (from
`fearSecondsRemaining`, retreat order or <25 % HP), `alert` (has target), `victory`
(VICTORY stage), `hit` (hurt flash), `attack`/`skill` (attack progress). No VFX is emitted in
Sanctuary idle/walk, and no projectile state was added, so there is no per-frame allocation
and no leak.

Known limit: the projectile still spawns when the AI fires, not on the animation's release
frame; the animation is ~0.22 s long. Retiming the spawn would move damage authority, which
is a balance change, so it was deliberately not done.

## 4. Art polish applied

Targeted cluster edits, not a regeneration of the concept:

- Jerkin lifted one ramp step (`#464632 → #52523a`, highlight `#5e5e42 → #6a6a4c`) so the chest
  no longer reads as the same value as the cloak. Palette count unchanged per frame.
- Bow highlight now traces the whole limb instead of stopping short of the tips, so the bow
  separates from the dark cloak at 1×.
- A second rim line on the left cloak contour (`cloak_hi`) so Lyra separates from cobble and
  grass at gameplay zoom.
- Portrait authored with explicit value contrast (dark socket → pale sclera → dark iris →
  catchlight) because only 3×2 px of the eye survives at 48 dp.

Honest note: this is generator-authored pixel art. It reads clearly at 1× and at gameplay
zoom, but it is **not** claimed to be at Brown Dust 2 art direction yet — that needs a human
pixel-polish pass on the face, cloak folds and bow silhouette.

## 5. Performance: modelled vs measured

Preserved: PR #19's P16 batching is untouched (`SanctuaryRoadCoverage`, grass-block batching
under 25 px tile height). The draw-call figures below are the **offline analytical model**
already documented in `docs/P16_LOBBY_TERRAIN_BATCH.md` (overview 4206 → 542, default
2244 → 1090). They are not a device measurement.

Audited in this stage (code reading, no profiler available):

| Path | Finding |
| --- | --- |
| `PremiumActorAtlas.get(id)` | map lookup after first load; no decode per frame |
| `PremiumActorAtlas.frame(...)` | returns a cached `ImageBitmap`; no allocation |
| `SanctuaryEnvironmentShadows.silhouette(bmp)` | `getOrPut` cache per bitmap — no per-frame `Path` build |
| `PremiumPortraits.get(name)` | fixed to key on the raw name (a `lowercase()` here would have allocated a `String` per HUD frame) |
| Lyra portrait draw | one `drawImage` with `FilterQuality.None`; bitmap decoded once |
| Lyra atlas memory | 6.00 MB decoded, one blit per actor per frame |
| Bow release point | pure arithmetic at spawn time, no per-frame work |

Not measured on device: frame time, jank, memory, temperature. If the phone still heats, the
next candidates (unchanged from P16) are the name-pill `TextLayoutResult` cache, road-curb
`Path` caching, sconce brush hoisting and `drawWeather` culling.

## 6. CI

| Workflow | Run | Result |
| --- | --- | --- |
| Build Eternal Summoner APK | `41417844891` | **pass** |
| Lyra engine render evidence (new, additive) | `41417844912` | **pass** — rendered and committed the PNGs |

The render workflow is additive: `.github/workflows/lyra-render-evidence.yml` does not modify
`main.yml`. It exists because the existing pipeline only ran `assembleDebug`, so no test and
no renderer evidence could ever be produced.

## 7. Not tested — on-device checklist for the user

- [ ] Install the debug APK on a real device; open the Sanctuary.
- [ ] Walk Lyra in all four directions: feet stay on the ground, no row popping.
- [ ] Check the hero card/HUD avatar shows the red-haired archer bust, not a knight.
- [ ] Start a tower battle with Lyra: fire arrows — the arrow should leave her bow, not her waist.
- [ ] Trigger her skill: three-arrow fan still resolves once per arrow (no double damage).
- [ ] Let her HP drop below 25 % → `fear` clip; victory → `victory` clip.
- [ ] Watch the Lobby for 2–3 minutes with the island overview zoom: no stutter regression
      versus the P16 build; note device model, refresh rate and whether the phone warms.
- [ ] Report the actual frame pacing; a 60/45/30 FPS target should only be chosen after that.
