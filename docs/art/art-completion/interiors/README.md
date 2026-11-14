# Original interior furnishings — Issue #117

Eight static furnishings replace verified flat interior primitives. The actual
native armory capture was inspected before authoring: the anvil was a simple
block, armor displays were circles/rectangles, personal chests were small boxes,
and the dining table retained a flat green panel. Existing world data, runtime
logic, old PNGs and the previously frozen prop batch were not edited here.

## Genuine sources and corrections

Three real hosted-imagegen outputs are retained in `source/`:

1. `eight-furnishings-v1.png` — original eight-object design sheet. Despite the
   transparent-background request, alpha was 153–251 across the white matte.
   This source **fails transparency acceptance and is never used by runtime**.
2. `eight-furnishings-clean.png` — actual imagegen background-removal correction,
   preserving the designs and removing the matte/cast shadows. Alpha is 0–255,
   with transparent gaps between legs and objects.
3. `wide-furnishings.png` — targeted original broader care table, communal table
   and service counter. Their construction was redrawn with wider planks/rails/
   panels so the old wide canvases can be filled with one uniform scale. No
   nonuniform image squash or synthetic plank extension was used.

Exact prompts are preserved beside this README. Original tool output paths:

- `/workspace/generated_images/exec-587be534-9fdb-4d20-9a53-6e1e918518f8.png`
- `/workspace/generated_images/exec-b33d576d-8f5e-4ba6-b8fd-e90e3b201820.png`
- `/workspace/generated_images/exec-c803fcab-121d-4622-96a4-cf2fafac0cf4.png`

These are normalized derivatives, not native-size generation claims. Static
sprites share NW key light, cool right-side shade, oblique top/front/right planes,
warm oak, forged metal, brass and linen. No live character, UI or cast-floor
shadow is baked into the runtime PNGs.

## Runtime contract

| ID | Canvas | Pivot | Nontransparent bounds |
| --- | --- | --- | --- |
| `forge_anvil` |96×80 |48,72 |11,4–84,72 |
| `armor_stand` |64×96 |32,88 |16,20–48,88 |
| `personal_chest` |64×48 |32,40 |11,6–53,40 |
| `care_table` |112×80 |56,72 |9,14–103,72 |
| `long_table` |108×68 |54,62 |4,14–104,62 |
| `bookshelf` |56×64 |28,58 |10,4–46,58 |
| `tavern_counter` |160×80 |80,74 |11,4–148,74 |
| `recovery_bed` |96×72 |48,64 |14,2–82,64 |

All requested canvas sizes and pivots are unchanged. Armor has 68px of visible
height; chest width is 42px (below 46); care/dining/counter widths are 94/100/137px;
bed width is 68px. The bookcase is a relatively slender 36px-wide design within its
56px canvas; do not silently stretch it nonuniformly during integration.

All runtime PNGs have binary alpha, transparent margins, at most 128 RGB colors
and zero clipped opaque pixels. The retained decoded bank is 225,984 bytes,
below the 1 MiB cap. This is pixel storage, not total process/GPU/path memory.

## Reproduce and inspect

```sh
python3 docs/art/art-completion/interiors/normalize_interiors.py
```

Requires Python 3 and Pillow. The normalizer isolates actual alpha-column runs,
uses one uniform nearest-neighbor scale per static object, anchors its foot to
the declared pivot, and quantizes to 128 colors. Exact PNG/source SHA256 values,
source bounds, paste coordinates, scales, alpha/palette checks and byte totals
are in the runtime `manifest.json` and `normalization-report.json` here.

`contact.png` was visually inspected: native-size sprites above and nearest 2×
views below, with visible wood/metal construction, support legs, fabric folds,
bench depth and different front/right planes. The contact background is inspection
only; the runtime PNGs are transparent. Repeated normalization reproduced identical
runtime PNG and manifest bytes, and independent hash/dimension/alpha/palette/bounds
verification passed.

Root owns runtime integration, current interior scene captures, consistent shadow
selection, controls and Android build. This asset handoff is not an in-engine
acceptance or a physical-device/FPS claim.
