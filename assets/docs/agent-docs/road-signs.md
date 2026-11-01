# Roads & Traffic: road signs

The road sign generators and the rules they share (face sources, texture size, plate backing, LED
signs).

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_dynamic_street_sign_texture.py` -- inventory/particle texture for the dynamic street sign block
- `gen_bike_route_shield.py` -- the bicycle route marker (MUTCD M1-8) for the shared sign
  atlas, which had no bicycle marker. Takes the book's own drawing, paints out its sample
  number with the oval's green, squares it for the cell, stamps that one cell into the
  committed atlas (touching no other pixel) and prints the four placement values
  `GuideSignShieldType.BIKE_ROUTE` carries, measured off the numerals it removed;
  `--check` fails on drift
- `gen_route_markers.py` -- the Dynamic Route Marker Sign's assets: one face texture and one
  gray back per route shield, cut from the guide sign atlas that already ships, plus the three
  shift models and the blockstate whose `shield` variant picks the marker. The shield list and
  its order are parsed out of `GuideSignShieldType` rather than repeated, since the ordinals
  are serialized. Faces are 128 px, backs their 64 px source; shields with identical pixels
  share one texture and the orphans are deleted; `--check` fails on drift
- `gen_road_markers.py` -- the mile markers (D10-1 to D10-5, `BlockMileMarkerSign`) and the post
  markers: the mile marker plates drawn from the book's dimensions with MILE in the FHWA series
  and cut into near-square cells (one per quarter of the texture, so a 1:5 plate does not blur),
  the glyph sheet (Series D numerals, the guide sign font's numerals cut from its atlas,
  NORTH/SOUTH/EAST/WEST in Series B), the three shift models and blockstates, and
  `MileMarkerLayout.java`, every slot and glyph the baked model draws the number from, so the
  two cannot drift; the OM1-OM4 object markers from the book's drawings and the U-channel and
  flexible delineators, each a settling `BlockWorkZoneDeviceDiagonal` on its own post. `--apply`
  inserts the four languages' lang lines; `--check`, `--fragments`, `--sheet`
- `measure_shield_legends.py` -- where each state, DC and province route marker on the sign atlas
  sets its route number (cap height, width, centre, colour): fits the largest two-digit number
  into the face region under a seed point and writes the values into `GuideSignShieldType`.
  Run it after `GuideSignAtlasTool` regenerates the atlas; `--check` fails on drift. The markers
  themselves are sourced public-domain SVGs, provenance in `guidesign/shields/SOURCES.md`
- `gen_gap_signs.py` -- the signs that filled the 2026-09 catalogue review's gaps (Keep Left,
  Speed Limit 10/60/70, reverse curves, advisory-speed and distance plaques, No Passing pennant,
  ...): one catalogue of registry + four-language display name + plate shape + face, the face
  either the official FHWA drawing through `shs_signs.py` or, where the book has no sign at the
  mod's wording, Highway Gothic text through `render_sign.py`; fitted at the plate's aspect,
  blockstate cloned from a same-shape sibling, `--apply` inserts the lang lines and tab lines
  after each sign's sibling; `--check` fails on drift (faces and `_back`s, each stored at its
  plate's size through `sign_texture_size.fit`). Silhouettes that are none of the eight
  shapes use the `yield_sign` model with a gray `_back` texture on slot `2`
- `shs_signs.py` -- accurate sign faces from the FHWA Standard Highway Signs drawings (public
  domain): fetches the 2004 book chapters and the interim per-sign ZIPs into the gitignored
  `_shs_cache/` on first use (~8 MB a chapter, so a fresh clone's `--check` needs the network
  once), then strips a dimensioned book page down to the sign -- the filled paths, the strokes
  heavier than a dimension line and the glyphs set in a Highway Gothic font -- and renders it
  with alpha. `recolour` maps the drawings' print colours onto the mod's palette, `fit_plate`
  squishes a face to the square texture its plate stretches back out. `find` locates a sign's
  page by legend; the page map lives in `gen_gap_signs.py`'s catalogue
- `gen_official_faces.py` -- swaps an EXISTING road sign's face for its SHS drawing: reads the
  sign's own blockstate for the plate model and the texture it paints (slot `1`, often not
  named after the registry), measures the plate's aspect off the model's `#1` faces, and
  writes that texture, at its plate's size (`sign_texture_size.fit`), and nothing else -- registration and blockstates are untouched, so
  `--check` is a byte comparison and a batch reverts with `git checkout`. `--sheet` makes the
  before/after contact sheet a batch is reviewed on; `--verify-sheet` puts each unverified
  match-table guess beside its cited book page. `replace=('50', '35')` re-sets the one numeral
  a page draws. Also the drawers for signs with no drawing (book panels with the mod's legend,
  public-domain MUTCD SVGs in `artwork/`, photo-measured panels). Every set legend uses the real
  FHWA Series B-F, extracted from the book at run time and never committed; see "Where Sign Faces
  Come From" in `assets/docs/TRAFFIC_SIGNS.md`
- `gen_large_custom_signs.py` -- road signs whose plate is several blocks across (the 5 x 3
  TRUCKERS panel): an OBJ plate centred on the placed block for each shift, since a JSON element
  cannot reach past -16..32, plus the blockstate with a slot-fitted inventory transform;
  `--art <dir>` rebuilds the face textures from the source art (not in the repository),
  `--from-existing` reduces the committed ones to the catalogue size; `--check` fails on drift
  and on a face texture not at its catalogue size
- `sign_texture_size.py` -- the road sign texture resolution rule every sign generator applies:
  measures each sign texture's largest plate off every model that draws it (JSON faces through
  their UV window, OBJ polygons through their UVs) and gives the smallest power of two with at
  least 85.3 texels a block (128 px to 1.5 blocks, 256 to 3, 512 to 6, never below 128);
  `fit(img, path)` reduces with the one approved filter (linear-light area average, light
  unsharp on opaque pixels). `--all` lists every texture over its size. See "Texture
  resolution" in `assets/docs/TRAFFIC_SIGNS.md`
- `cap_sign_textures.py` -- `--check` fails on any road sign texture above its plate's size,
  whatever wrote it, and names the fix; `--apply` reduces only the ones no checked generator
  writes (hand-made faces, retired one-off scripts' output); `--claims` lists who writes each.
  `SignTextureSizeTest` holds the same rule in the build
- `detect_legend_series.py` -- measures a sign's original texture (first git version): each legend
  line's centre, cap height and nearest FHWA series, for a remake's `layout=`
- `fix_sign_plate_backing.py` -- recesses the bare-metal face behind a hand-built sign plate's
  art, which z-fought with it from a dozen blocks out; rewrites a model only if it can reproduce
  the file byte for byte first, so the diff is the geometry and nothing else. `--apply` repairs,
  `--check` fails on a plate that still has the defect (`SignFaceDepthTest` holds the same rule
  for road signs); the pull request integrity job runs `--check` over every module's models, so
  a generated model with the defect is fixed in its generator
- `gen_led_signs.py` -- the LED-enhanced flashing STOP / WRONG WAY / DO NOT ENTER / PEDESTRIAN
  signs and the pedestrian arrow plaques: composites the border LEDs into the plain sign's face
  texture as a two-frame strip (one 100 ms blink a
  second, timed by the `.mcmeta` so every sign in the world blinks in step), writes the OptiFine
  `_e` companion on the same clock, and clones the plain sign's blockstate, so no model changes.
  The LED positions are read off each base texture's own outline, not hard-coded, and the strip
  is drawn at the base's size (the plate's). Re-run it whenever a base face changes; `--check`
  fails on drift
