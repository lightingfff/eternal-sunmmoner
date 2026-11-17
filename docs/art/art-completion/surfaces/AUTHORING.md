# Surface source

`source.png` is the retained genuine hosted image-generation output used by
`tools/art/build_completion_surfaces.py`; the runtime manifest pins its hash.
Generation intent: four independent seamless dark-fantasy pixel material panels
for cliff strata, limestone masonry, embroidered crimson carpet and canal stone,
with directional facets and no scene, UI, text or character. This is a summary of
authoring intent, not a reconstructed verbatim generation transcript.

Normalization crops the four panels and nearest-resamples them to 256×256,
quantizes to 128 colors and makes opposite edges exactly periodic. The runtime
maps them into existing oblique face polygons with cached shaders; it does not
replace a building with a flattened scene image or alter collision geometry.
