# Remaining enemy archetypes — task117 original art

Four genuine hosted-imagegen sheets supply the four archetypes absent from the
existing meadow pack: `FOREST_WRAITH`, `ELDER_TREANT`, `RUIN_SENTINEL`, and
`ABYSSAL_EXECUTIONER`. Existing raider, archer, wolf and Kraag art is untouched.
The images are original project designs, not copied manhwa characters.

Each original is2172x724 with six logical columns and two rows. The originals and
exact generation prompts are preserved under `source/` and `prompts/`. They are
normalized derivatives, not a claim that imagegen emitted native128x144 pixels.
The tool-generated originals also remain in `/workspace/generated_images`.

## Runtime interface

Each `app/src/main/assets/art_completion_v1/enemies/<lowercase_enum>/` contains:

- `atlas.png`:1536x144 RGBA,12 columns, one row,128x144 cells.
- `metadata.json`: exact enum, file/hash, cell/pivot, world scale, pose order,
  source hash/dimensions, fixed normalization roots and unique pose hashes.

All pivots are(64,134). World scales are.82 for wraith/sentinel and1.05 for the
treant/executioner bosses. These affect drawing only; no stats, collision,
navigation, contact timing or engine state were changed by this asset writer.
Left-facing instances must mirror at draw time around the same pivot. There is
no duplicate left-facing bitmap bank or separate-left artwork claim.

Frame order: idle0, idle1, walk0, walk1, walk2, walk3, anticipation, contact,
recovery, hurt, collapse, defeat. Wraith's contact image is its ranged release.
Engine selectors must preserve the existing actual attack timing rather than
introduce new delay from this sheet.

## Normalization and checks

```sh
python3 docs/art/art-completion/enemies/normalize_enemies.py
```

Offline requirements: Python3, Pillow, NumPy, SciPy. No Python/Node/provider is
required by the Android runtime. Sources use actual alpha; threshold96 removes
the low-alpha fringe. Final alpha is binary and the shared atlas palette has at
most128 colors. Resampling is nearest-neighbor only.

Generated spears/axes cross nominal source-cell boundaries. The normalizer
recovers each connected complete figure before cutting, then associates tiny
detached foreground clusters with the nearest major silhouette. It preserves
all source foreground above threshold, rather than clipping weapons at a logical
grid line. Placement uses the original logical slot and a single fixed scale/root
per monster, with one common baseline for each source row. There is no per-frame
resizing or bbox-center alignment; collapse retains its anatomical size.

The first sentinel scale exposed five clipped pixels in the defeated shield.
The common scale/root was adjusted for the entire sentinel, then all48 poses were
rebuilt. Current assertions pass:48 distinct normalized poses, no clipping, positive
inner margins, no empty frames and exactly3,538,944 retained RGBA bytes. This fits
the separate4MiB extended-enemy bank; it is not a total-process/GPU memory claim.

`validation.json` and per-enemy `normalization-report.json` record measured source
bounds, roots, destination placement, alpha counts and output hashes. Each enemy
folder also contains a nearest-scaled contact sheet and lossless animated WebP
previews for idle, walk, attack and defeat. All four contact sheets were inspected:
distinct alternating gait, readable attack extension, recoil and fallen silhouettes
are visible. The treant's branch crown keeps the final collapsed silhouette taller
than a humanoid corpse; this is authored shape, not automatic height equalization.

Runtime integration, in-engine captures, Android build, independent creative QA,
physical install/FPS/thermal are root tasks and **not claimed by this asset handoff**.
No Kotlin, legacy PNG, Git history, branch or GitHub file was changed here.
