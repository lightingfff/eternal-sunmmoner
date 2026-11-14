# Original prop completion — Issue #117

This owned asset batch replaces three verified weak procedural shapes: the
rectangular royal guardian statue, flat crimson diamond and plain violet banner.
No existing house, tree, game state, source code or old PNG was changed here.

The actual hosted-imagegen output is retained unchanged in
`source/three-props-v1.png`, alongside the exact prompt. The image is 2172×724 RGBA
and contains three separated original objects. The tool output was
`/workspace/generated_images/exec-e9b2a027-661e-49a2-bc96-065e14346281.png`.
These are normalized derivatives, **not native-size generation claims**.

| Runtime ID | Canvas | Ground pivot | Nontransparent bounds | Decoded bytes |
| --- | --- | --- | --- | ---: |
| `royal_statue` |52×88 |26,82 |7,3–45,82 |18,304 |
| `crimson_crystal` |48×64 |24,60 |3,4–44,60 |12,288 |
| `tattered_banner` |80×80 |40,76 |37,11–72,76 |25,600 |

The statue retains the old 52×88 canvas and lower pedestal contact at 82. The
banner's brass pole is fixed at x40; the cloth extends to the right, leaving the
left half transparent. All three have transparent margins, binary alpha, at most
128 RGB colors and no baked cast-ground shadow. The new statue puts its halberd
on the viewer's left; it remains a static guardian sculpture with shield and
carved pedestal. This is original object artwork, not a flipped live character.

`normalize_props.py` isolates each occupied source column, thresholds alpha at 96,
uses one uniform nearest-neighbor scale per static object, places the fixed base,
and quantizes to 128 colors. It checks that no opaque source pixels are lost during
placement, then writes exact SHA256/dimensions/pivots/source bounds to the runtime
manifest and this batch's normalization report.

```sh
python3 docs/art/art-completion/props/normalize_props.py
```

Requires Python 3 and Pillow. The total retained pixel bank is 56,192 bytes, below
the 1 MiB limit. `contact.png` shows native size above and nearest 3× views below on a
dark inspection background; the runtime PNGs are genuinely transparent.

The contact was visually inspected: the pedestal has front/top/right planes,
the guardian has an anatomical armored silhouette rather than white blocks,
the crystal shows asymmetric material facets and the banner has folds, tears
and brass highlights. Runtime integration, shared directional shadows, actual
world placement/occlusion, Android build and physical-device behavior are owned
by the root integrator and remain to be checked there. This handoff is not an
in-engine visual acceptance or a device-performance claim.
