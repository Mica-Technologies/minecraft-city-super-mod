# Agent docs

The detail the repository `CLAUDE.md` links to rather than carries, so an agent reads the short
`CLAUDE.md` every session and only the files here that its task needs.

- [`modules.md`](modules.md) -- the full module table: every module's packages, tabs and contents
- [`system-docs.md`](system-docs.md) -- what each in-depth doc in `assets/docs/` covers
- The `dev-env-utils/scripts/` entries, one file per module: [`core-and-shared-tools.md`](core-and-shared-tools.md),
  [`road-signs.md`](road-signs.md), [`roads.md`](roads.md), [`life-safety.md`](life-safety.md)
  (with the fire alarm sound architecture), [`lighting.md`](lighting.md), [`signage.md`](signage.md),
  [`building.md`](building.md), [`hvac.md`](hvac.md), [`parks.md`](parks.md),
  [`furnishings.md`](furnishings.md), [`technology.md`](technology.md), [`transit.md`](transit.md),
  [`healthcare.md`](healthcare.md), [`venues.md`](venues.md), [`vehicles.md`](vehicles.md),
  [`utilities.md`](utilities.md)

## Keeping them current

- A new generator or build script gets its entry in its module's file here, not in `CLAUDE.md`.
- A new `assets/docs/` doc gets its paragraph in `system-docs.md` and its file name in the list
  in `CLAUDE.md`'s In-Depth System Documentation section.
- A new module gets its full row in `modules.md`, a one-line row in `CLAUDE.md`'s module table,
  and, once it has generators, its own file here linked from `CLAUDE.md`'s Developer Utilities
  table.
- Keep `CLAUDE.md` under 40,000 characters; anything one subsystem needs belongs here.
