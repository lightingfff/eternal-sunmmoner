# Monumental gate art — Issue117

This directory contains genuine hosted-imagegen edits and reproducible technical
normalization for the existing six-second gate cutscene and three-gate Nexus.
No Kotlin, timing, runtime aperture, touch rectangle, camera or navigation changes
were made by this asset worker. The coordinator owns runtime integration and QA.

The court and Nexus replace flat outlined masonry with layered carved ashlar,
gold-inlaid voussoirs, recessed windows, roof courses, cornice occlusion, thick
stairs, right-side planes and textured moonlit paving. Existing stars, mountains,
evergreen setting, side balustrades, plants and centered carpet remain. The doors
use original beveled bronze/iron panel art with the exact old alpha silhouettes.
All generated originals remain in `source/`; their prompts are alongside this file.
Output files are normalized derivatives, not native 960x540 generation claims.

## Reproduce

```sh
python3 docs/art/art-completion/gates/normalization.py
```

Python 3 and Pillow are required. Source image dimensions and SHA256 are recorded
in both the runtime manifest and `normalization-report.json`. Scene outputs use
nearest resampling and a 256-color palette. Ground scenes are opaque RGBA;
door leaves preserve the existing 144x240 alpha silhouette, including gutters.
The two 960x540 scenes plus 144x240 doors retain exactly 4,285,440 pixel bytes,
the same decoded size as the old pack. Source files, prompts and contact sheets
are documentation, not intended to be packaged in the APK.

## Registration and honest residuals

The generated Nexus raised the portals relative to the runtime masks. At the
coordinator's explicit direction, one global monotonic nearest-neighbor coordinate
map registers the entire image to the unchanged mask landmarks. Its measured
source-to-runtime control points are declared in `normalization.py` and manifest.
There are no pasted local gate patches or seams, and the raw generated source
remains unchanged. The court needs no registration.

Every aperture inset by 4 pixels is completely dark and unobstructed. Some original
generated bevel pixels remain inside the outer 4-pixel rim of the existing runtime
mask: 262 court, 308 left Nexus, 766 center Nexus, 465 right Nexus. These are disclosed,
not silently declared exact. Inspect `*-aperture-overlay.png`: cyan traces the
actual existing runtime mask; orange identifies non-dark pixels it touches.
The normalizer does not paint over openings or redraw architecture. The coordinator
must inspect the actual animated portal composite before visual acceptance.

`before-after-contact.png` compares actual old PNGs and normalized new PNGs. These
are asset comparisons, not gameplay screenshots, phone FPS evidence or a claim
that the cutscene has been tested in this worker. Native renderer, skip/back,
physical portal input and selection/locked-floor tests belong to integration QA.
