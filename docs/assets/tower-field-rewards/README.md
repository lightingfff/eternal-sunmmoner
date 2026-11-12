# Original Tower field-reward icons

Original static item icons generated for this project using the session's hosted
`image_gen` tool on 2026-11-13 (Asia/Saigon). No existing commercial art was supplied
or copied. The tool did not expose a model ID or monetary cost; neither is invented.
This is icon art, not character animation, terrain or a replacement world renderer.

`source-icons.png` is the exact returned RGBA source (1254x1254), SHA-256
`2c56a38abfa3836127d1c375acc769120fbd366d23b9211c6178304decec5e38`.
Source prompt is retained in `prompt.txt`. Reading order: scouting/readiness map,
protective ward, healing flask, valuables satchel. The 2x2 cells are 627px each;
generated dimensions are measured, not inferred from the requested square grid.

Processing used Agent Sprite Forge `generate2dsprite` from commit
`6b3da5c444363378e20f3fce130ebecc22f343dd` (MIT, 0x0funky), available through PR102.
Only local inspection/PNG processing scripts ran; no nested CLI, video or paid API route.

```sh
python .agents/skills/generate2dsprite/scripts/sheet_qc.py spill --input docs/assets/tower-field-rewards/source-icons.png --rows 2 --cols 2 --output-dir build/field-reward-art/raw-qc
python .agents/skills/generate2dsprite/scripts/generate2dsprite.py process --input docs/assets/tower-field-rewards/source-icons.png --target asset --mode sheet --rows 2 --cols 2 --background-mode native_alpha --cell-size 64 --anchor-mode center --align center --fit-scale 0.85 --component-mode all --resampler lanczos --output-dir build/field-reward-art/reprocessed --strict-qc
```

Each command requires a new output directory. Source QC and pipeline metadata are
retained verbatim; their original working paths refer to the generation workspace.
Match portable copies by hashes, using the manifest mapping below. Four normalized
64x64 PNGs ship under `app/src/main/assets/tower_field_rewards/`; `sheet-1.png` through
`sheet-4.png` map to `readiness`, `ward`, `healing`, `valuables` respectively.
The runtime manifest records their dimensions, hashes and alpha bounds.

## Actual QC and limits

Raw `sheet_qc.py spill` returned **WARN**, not an all-pass report:

- `safe_frame`: value `3`, threshold `0`, source cells `[0,1]`, `[1,0]`, `[1,1]`.
  Some art is inside the default 15% preferred margin. Cross-cell solid/visible components,
  boundary-band pixels, sheet-edge cuts and empty cells are all zero. No icon is cut by a cell.
- `faint_specks`: value `{"floor_px":62037,"detached_components":19}`, threshold `0`,
  source `source-icons.png`. Pipeline default `alpha-hygiene both` removed those low-alpha
  haze/specks; its measured maximum removed alpha is 7/255, detached area 45 source pixels.
  `component-mode all` retained actual disconnected coin/rune detail; no largest-component filter.

Normalized pipeline QA is **PASS** for empty/clamped/source-edge/output-edge checks.
Body-scale CV and anchor-Y statistics are **SKIPPED** by the asset pipeline, appropriate
for four different static items; they are not animation consistency proof. No animation
or video generation is claimed. Independent fit scale is intentional for UI item icons.
Downsampling used LANCZOS; runtime uses nearest filtering. These are reduced illustrated
pixel-style icons, not claimed hand-authored logical 1px grids.

All four runtime alpha bounds lie strictly inside 64x64. Retained decoded ARGB is
65,536 bytes total, loaded once outside composition/drawing. Missing/corrupt/wrong-size
packs are atomic cosmetic failures: recycle partial work, negative-cache the failure,
keep text rewards usable. Focused failure tests cover all three cases.

Inspected `runtime-size-review.png` and production native renders in the linked gameplay
report, including the icons at actual HUD size. Host native rendering does not establish
physical-phone performance or human enjoyment. No frame count/automated score is offered
as proof of art quality. This pack changes no camera, gameplay collider or existing art.
