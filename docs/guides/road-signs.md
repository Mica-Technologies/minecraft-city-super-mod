# Road Signs

**644 road sign blocks**, modelled on real US signs and grouped the way the MUTCD groups them.

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
- whether the sign goes **back to back** with one facing the other way in the next block

You do not configure any of it. Place the sign and it works out what it needs.

### Two signs, one post

Put two signs in adjacent blocks facing opposite ways and they build one assembly rather than two:
one of them moves its plate across onto the back of the other's post, so a single post carries a
face each way and the pair reads correctly from both sides.

The one that moves is the one with nothing supporting it below — the sign standing on its own post
keeps its place. A pair hanging in the air, off a span wire or a mast, has support under neither,
and in that case one of them is still picked, consistently, so you never get both moving and
swapping places.

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

## Signs bigger than their block

Three signs carry a panel several blocks across:

| Sign | Panel |
|---|---|
| **Danger Low Clearance** | 3 x 2 blocks |
| **Cars Only Banner** | 5 x 1 blocks |
| **Truckers Use Expressways Not Parkways** | 5 x 3 blocks |

Each is still one ordinary sign block, placed, rotated and stacked like any other. The plate is
centred on that block and **overhangs into the blocks around it**, so leave the room clear. You
still click the panel where its block is, not out where the plate reaches.

## Beyond the fixed signs

Three sign families are configurable rather than fixed:

- **[Guide & street signs](dynamic-signs.md)** — highway guide signs and street name blades you
  compose yourself.
- **[Message & speed signs](message-signs.md)** — DMS boards and variable speed limits.
