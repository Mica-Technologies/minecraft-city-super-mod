# Lighting

The Lighting module's generators.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_decorative_lighting.py` -- the decorative pendant and wall-sconce family: lathes the OBJ
  geometry for 12 models, draws the shared metal/shade/lens swatch textures, and emits all 36
  blockstates plus lang and tab-registration fragments from one catalogue, so an id cannot drift
  from its blockstate
- `gen_linear_lights.py` -- the linear LED lights (`BlockLinearLight`, opal and louvered, ten
  finishes) and their cable hanger, and the shared ten-finish palette in Core
  (`blocks/finishes/`): multipart parts drawn for a run along z, joined, capped, hung and cabled
  by actual state; `--check`, `--fragments`
- `gen_troffers.py` -- the modern lay-in troffers (louvered slot, center basket, four patterned
  lenses; 2x2 and 2x4) and the rebuilt Lithonia 2ESL2/2ESL4: lens drawn unshaded and swapped per
  STATE, states 0 and 2 naming the unlit lens so `gen_lighting_lit_atlases.py` leaves them alone,
  patterned faces cut into a sprite a block; `--check`, `--fragments`
- `gen_streetlight_fixtures.py` -- the OBJ roadway and area fixtures (GitHub #99): the GE M-400A,
  every converted JSON cobra head, and the new KarbonCobra, AutoConnect ACM, M-250A, Westinghouse
  OV-25 / L-250, SolarMax SMX-LL5, GE Evolve EALS (four mounts) and Holophane Mongoose. Lofts each
  housing from a profile table, hangs a flat glass, LED array or drop refractor under it, writes
  one OBJ per fixture, the shared `streetlight.mtl`, the blockstates (facing x STATE, the lens
  swapped per STATE), the 16 px swatches and the lens sprites (cut from the requester's art, or
  drawn by `led_array_png` for fixtures that had none). Asserts
  that every neck swallows the CSM arm tip and that nothing leaves the old JSON envelope. The
  glass is the `unshaded_lens` material, which `CsmObjModelLoader` bakes without directional
  shading; `--check`. Run `audit_obj_models.py` on its OBJs after a change
- `gen_decorative_poles.py` (entry in `roads.md`) -- also writes the decorative post tops: the
  LA lanterns and globes (`la*`) and the reproduction set (`repro*`), `PostTop` / `PostTopAimed`
  blocks on the decorative poles; LIGHTING_SYSTEM.md, "Decorative and Historic Post Tops"
- `gen_floodlights.py` -- the OBJ floodlights (GitHub #99): Lithonia DSXF1 / DSXF2 / DSXF3 and
  TFL, GE Evolve EFM, each on a knuckle, a yoke and a slipfitter. Draws each head level in its own
  frame, then turns it to its aim about the knuckle or the trunnion bolts, so one head serves every
  mount; writes one OBJ per block (the wall block and its `_g` ground / pole-top, `_c` ceiling and
  `_x` RealGrid crossarm companions, `BlockFloodlight`), `floodlight.mtl`, the blockstates and the
  32 px lens sprites with their OptiFine `_e` companions. The housings wear
  `gen_streetlight_fixtures.py`'s swatches and it imports that script's mesh helpers;
  `--check`, `--fragments` (tab lines for both Lighting tabs, and the lang). Run
  `audit_obj_models.py` on its OBJs after a change
- `gen_obstruction_beacons.py` -- the Lighting tab's tall building beacons (`BlockObstructionBeacon`):
  the red L-864 style flashing beacon and the white L-865 style strobe, each standing on a block or
  a shelf bracket off a wall. It writes the models with the dark lens only: the flash (FAA
  AC 150/5345-43J, 30 and 40 flashes a minute) is `TileEntityObstructionBeaconRenderer`'s, whose
  lens sizes must match the script's; `--check`, `--fragments`
