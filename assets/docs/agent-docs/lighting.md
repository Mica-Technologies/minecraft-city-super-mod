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
- `gen_obstruction_beacons.py` -- the Lighting tab's tall building beacons (`BlockObstructionBeacon`):
  the red L-864 style flashing beacon and the white L-865 style strobe, each standing on a block or
  a shelf bracket off a wall. It writes the models with the dark lens only: the flash (FAA
  AC 150/5345-43J, 30 and 40 flashes a minute) is `TileEntityObstructionBeaconRenderer`'s, whose
  lens sizes must match the script's; `--check`, `--fragments`
