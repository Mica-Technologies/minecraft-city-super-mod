# Road Signs

**574 road sign blocks**, modelled on real US signs and grouped the way the MUTCD groups them.

## Placing a sign

Signs are simple to place and have two conveniences worth knowing.

### Stacking aligns automatically

A sign placed on top of another sign **inherits the facing of the sign below it**. So set the
direction on the bottom sign of an assembly and everything you stack on it lines up without
fiddling.

### Posts and setback are automatic

The sign blockstates carry dynamic properties that decide, from what is around them:

- whether a **post extension** is drawn below the sign
- whether the sign is **set back** to clear a signal arm passing in front of it, or to hang in
  line under a [span wire](span-wire.md) mount

You do not configure either. Place the sign and it works out what it needs.

## Finding the one you want

Signs are ordered in the creative tab by MUTCD category rather than alphabetically, so the tab
reads the way the manual does — regulatory, warning, guide, and so on, in groups.

The full list, with the registry id for each, is in the
[Road Signs block reference](../reference/road-signs.md). That page is generated from the source, so
it is complete — the old wiki was missing 136 of these.

!!! tip "Registry ids are not descriptive"

    A sign's id is often nothing like its display name, and ids have been **reused** for different
    signs over time to avoid bloating existing worlds. Always take the id from the reference page
    rather than guessing it from the sign's name.

## Beyond the fixed signs

Three sign families are configurable rather than fixed:

- **[Guide & street signs](dynamic-signs.md)** — highway guide signs and street name blades you
  compose yourself.
- **[Message & speed signs](message-signs.md)** — DMS boards and variable speed limits.
