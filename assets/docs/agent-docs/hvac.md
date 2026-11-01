# HVAC

The HVAC tab's generators and the HVAC test lab.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_exposed_services.py` -- the HVAC tab's decorative exposed ducts and pipes
  (`BlockExposedDuct`: round spiral and rectangular duct, painted pipe), joining six ways by the
  cable trays' rule, octagon sections, the middle and fittings picked from the arms by multipart
  conditions, rods and straps under a solid ceiling or the stackable threaded rod hanger
  (`BlockExposedServiceHanger`); `--check`, `--fragments`
- `gen_hvac_data_hall.py` -- the HVAC tab's data hall CRAC/CRAH units (`BlockHvacCrac`: downflow,
  upflow with its plenum, the chilled-water CRAH in graphite; a cabinet two blocks tall from one
  block) and the HVAC Zone Boundary strip (`BlockHvacZoneBoundary`, #305), with their lang lines
  by key; `--check`, `--fragments`. `gen_hvac_exhaust.py` does the same for the exhaust fans (the
  2 x 2 lab fan as a master and parts, upblast, mushroom) and the exhaust grille (#314)
- `build_hvac_lab.py` -- builds the HVAC test lab (twelve scenarios from a closet to a replica of a
  real four-zone store) in a Cold Taiga or Desert superflat loaded in the dev client, over MCMCP,
  linked and powered through saved data; refuses any world not named "HVAC Lab". Drive it with
  `/csmhvac info|why|settemp|ff|rescan`
