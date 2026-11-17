# Flight glyph source

`source.png` is the genuine hosted image-generation output. The manifest pins its
hash. Generation intent: a clean alpha sheet of twenty isolated right-facing
pixel projectiles, four visible phases each of master bolt, arrow, arcane, holy
and hostile arrow, preserving tip alignment and distinct palettes without bodies
or background. This is an authoring summary, not a verbatim prompt transcript.

`tools/art/build_completion_projectiles.py` retains each projectile's physical tip
as root (40,16), common scale per row, real soft alpha, 48×32 cells and 128 colors.
Twenty distinct frames are validated. The runtime rotates them using the existing
velocity projected by ground Y=.72, draws at the simulation position/elevation
and never changes flight speed, impact, damage or lifetime.
