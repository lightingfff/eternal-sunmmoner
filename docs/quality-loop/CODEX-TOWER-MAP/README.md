# Tower gate map rebuild — Issue #84

Direct owner request: correct the entrance carpet and remove the two front water
channels; rebuild the six-second Eternal Tower opening and the following three-gate
map with textured pixel architecture and environment, preserving the transition,
with no persistent Nexus HUD.

Base: `9d9f5b6c8f11ffa84e3c15c50e0fc866e991091e`.
Branch: `codex/tower-gate-map-reforge`.

## Delivered behavior

- The entrance runner follows the actual road segment from `(1545,1735)` to
  `(1610,2240)`, then continues along the gatehouse center. It no longer uses an
  independent vertical axis or an offset carpet patch on a southern footbridge.
  Two southern channel arms, their front waterfall outlets and the obsolete
  southern footbridge are removed from drawing. Navigation/collision is unchanged.
- Original deterministic pixel artwork: individually shaded masonry, carved
  arch stones, fluted pilasters, spires, side planes, roofs, engraved door leaves,
  stairs, flagstones, brass floor inlay, pine backdrop and planted borders.
- Opening retains the six-second approach / seal awakening / sliding doors /
  camera tremor / light fan / floor waves / transition flash and optional skip.
  The existing hero renderer and its oblique projection are used, without putting
  a Master avatar into the scene.
- Nexus initially shows the three illuminated gates (event left, tower center,
  custom right) and their environment. No header, resource strip, portal-button
  row, subtitle panel or default mission panel covers it. Tap the actual gate to
  open its existing mission filters/list. Floor unlocking, select-floor callback,
  squad preparation and Android Back remain. An accessibility action can replay
  the opening; each gate has a named accessible button target.
- Art, effects and hit targets share one uniform viewport transform. Portrait
  preserves the entire landscape composition with letterboxing; it is not a
  separate full-height portrait level. Low-density portrait sprite culling uses
  scene bounds, so all squad slots remain visible.

## Runtime/asset constraints

`tools/art/build_tower_gate_map.py` authors the three original PNGs with Pillow;
no downloaded art, existing PNG editing or provider output is involved. Run it
from the repository root to reproduce the assets. Their hashes and scene-space
portal anchors are in `app/src/main/assets/tower_gate_map/manifest.json`.

PNG dimensions, all chunk CRCs, complete decoding and SHA-256 are checked in
`evidence/asset-validation.json`. Three textures total 4,285,440 bytes at RGBA8888
(calculated decode footprint, **not** measured Android heap). The screen loads
these once, uses nearest-neighbor sampling and cached arch masks, then draws
bounded light/mote effects at 30 Hz through the existing lifecycle-aware frame
loop. It pauses while the floor dialog is open. There is no PNG decode per frame,
new shader, save schema, combat or engine change.

## Reproduction and evidence

Run with the repository's Java 21 / Gradle 9.3.1 / Android SDK 36.1 setup:

```bash
gradle :app:assembleDebug :app:testDebugUnitTest --offline --console=plain \
  --tests com.example.ui.TowerGateMapTest \
  --tests com.example.SanctuaryEnvironmentArtTest \
  --tests com.example.performance.SceneFrameLoopLifecycleRobolectricTest \
  --tests '*LayoutResponsivenessRobolectricTest.narrow portrait 360x640 floor selection*' \
  --tests '*LayoutResponsivenessRobolectricTest.management navigation*'
```

Local final result: **APK build + 12 tests PASS**, zero failures/errors/skips.
Separate unmodified-main baseline capture: **2 tests PASS**. APK ZIP CRC and
embedded map-asset byte matching also PASS. Full counts and APK SHA-256 are in
`evidence/verification.json`. Remote CI is reported separately on the PR.

The UI interaction tests pause scene animation explicitly so Compose can become
idle; the separate natural-transition test runs the real frame loop for the full
six seconds. Existing UI tests now open the physical gate before looking for a
floor button and exercise Android Back instead of a removed HUD button. Their
navigation/viewport assertions are retained.

`before_*` images were captured from unmodified production source at the base SHA
in a detached worktree. `after_*` come from the branch production renderer; the
Nexus Compose capture additionally includes the actual composed screen. These
are **Robolectric native graphics with fixture heroes**, not phone captures or
concept mockups. Sanctuary comparisons use the same `(1610,2020,.88)` camera,
noon clear weather, and 1.2-second animation time. The other captures cover .36
and 1.48 zoom, architecture, existing canopy occlusion and cutscene phases.

## Integration/review boundary

This is an owner-assigned isolated map branch. `SanctuaryWorldRenderer.kt` also
appears in A03 PR #67 (`475154bab7507684a513e6df35af24ab5789a57c`). Our changes
are restricted to the southern channels/runner/footbridge; A03 owns its asset
bindings. No changes were pushed to its branch, no A03 assets were imported,
and no PR67 integration or QA is claimed. A01 must reconcile the shared renderer
when integrating reviewed work. PR #28, #47 and #50 remain separate.

Independent Argon/A09 review and owner visual acceptance are still required.
No physical Android device was attached (`adb devices -l` returned an empty
list): device FPS, heat, GPU timing, predictive-back gesture and on-phone visual
acceptance are **NOT TESTED**. No merge is performed.
