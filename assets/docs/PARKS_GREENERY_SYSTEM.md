# Parks & Greenery

**CSM: Parks & Greenery** (`csm_parks`, tree `modules/parks`) is street trees, plantings and park
amenities. It has two creative tabs: **Trees & Plants** (order 18) and **Parks** (order 19).

This document is the design record: how a tree is built, why the blocks are drawn the way they are,
what the planting tool does, and the traps.

---

## Trees are built from blocks

A tree is built block by block from log and leaves blocks, as vanilla trees are. There is no
multi-block tree object and no tree entity. A player can build one by hand, edit one the planting
tool made, or mix vanilla logs in.

The kit has three kinds of block:

| Block | Class | What it is |
|---|---|---|
| Logs | `BlockTreeLog` | One block for every wood in each of five widths: twig 2 px, thin 4, medium 8, thick 12, full 16 |
| Leaves | `BlockTreeLeaves` | One block for each species and season. The same class draws the palm crowns |
| Hanging moss | `BlockHangingMoss` | Spanish moss, hung under a limb or crown |

The logs and leaves are one class each, constructed by name. The tab source has one explicit
`initTabBlock(new BlockTreeLog("tree_log_<wood>_<width>", ...))` line per block, because the tools
that read tab sources (the integrity tool, `csm_block_index.py`, `gen_wiki_reference.py`) cannot
follow a loop. `gen_trees.py --fragments` prints those lines.

### Logs: connections carried in an extended state

A log draws an **arm** toward everything it joins:

- each face neighbour that is a log (any width, vanilla logs included);
- leaves it reaches into (up, or opposite a log);
- solid ground below, which gets a flared foot;
- each of its **12 edge-diagonal** neighbours that is a log, when no face neighbour already joins
  the two.

The last rule is what makes a leaning tree work. A trunk leans the vanilla acacia and dark-oak way,
by stepping diagonally (`- T / T -`). Without a bridge, a thin log stepping diagonally is a row of
separate sticks. With it, each block draws half a straight tube to the diagonal neighbour's centre,
and the stepped chain reads as one continuous trunk. Limbs that spread at 45 degrees in plan use
the horizontal edge diagonals the same way.

A wider log under a narrower one tapers into it. Where the arms bend, a slightly wider **knuckle**
reads as a joint. Those two, the bridges and the flare are all the "angle supports" there are, and
nothing more is added.

18 booleans would be 262,144 block states per log, all built eagerly (the MAST_ARM_CURVE_SYSTEM
trap). Instead, `TreeLogConnections.compute` packs the connection mask into a long:

- the faces, the diagonals and the leaves;
- the ground flag;
- a 3-bit neighbour width per face, so a taper knows how far to narrow;
- the axis.

The mask travels in the **extended (unlisted) state**. `TreeLogBakedModel` builds the quads from it
through `TreeLogGeometry`, which is pure maths and unit tested, and caches them per mask. The only
listed property is `AXIS`, which orients the bark on a horizontal limb.

`TreeModels` puts the baked models in place at `ModelBakeEvent`, over the placeholder variants the
blockstates name (`axis=x|y|z` for logs, `normal` for leaves). The placeholders are plain JSON
models that also serve as the item model and put the textures on the atlas.

There is deliberately no custom state mapper. One registered in pre-initialization keys on a
registry name that does not exist yet, and collapses every block into one entry. That trap is
described in PEDESTAL_POLE_SYSTEM.md.

### Leaves: cards, not cubes

Leaves drawn as cubes read as stacked boxes, and a one-wide crown looks like a pillar. A leaves
block is drawn as **leaf cards** instead: double-sided cutout quads showing the species'
leaf-cluster sprite (`TreeLeavesGeometry`). Each block has:

- a few cards inside the cell;
- a **fringe** of cards reaching up to 3 px past each **open** face (one with no leaves and no
  opaque block beyond it), so a crown's outline is not the block grid;
- a near-flat **cover card** across an open top or bottom, so a canopy seen from below is a mass of
  leaves rather than sky between scattered cards.

Nothing is drawn between two leaves cells. A cell with no open face draws a reduced interior, and
Fast graphics draws the interior only.

The open faces and a per-position variant (0 to 3) travel in the extended state (`SHAPE`), so
neighbouring blocks differ but a given block always looks the same.

`TreeLeafType` decides the arrangement. The species decides the texture.

| Type | For |
|---|---|
| `BROADLEAF` | oaks, elm, plane, ginkgo, poplar, sweetgum, hornbeam, linden |
| `AIRY` | honey locust, jacaranda, gum: fewer cards, lets the light through |
| `NEEDLE` | cypress, arborvitae: upright narrow cards, so a one-wide column is the whole tree |
| `WEEPING` | pepper tree, willow: a curtain hangs below any open bottom |
| `CLIPPED` | the pleached linden: a flat leafy face flush with each open side, for topiary |
| `PALM_FAN`, `PALM_FAN_SKIRT`, `PALM_FEATHER` | palm crowns, drawn by `TreePalmGeometry` |

Leaves never decay: a street tree that disappears would ruin a build. They have **no collision**,
so a canopy never blocks a sidewalk or snags on a sign, but the selection box is the full block so
they are still easy to break.

### Palm crowns

A palm crown is one leaves block on top of a palm log. Its fronds are bent strips that reach well
past the cell: a fan palm's round fans, or a feather palm's long arching fronds. The skirted fan
crown adds dead fronds hanging down the trunk. The sprite is a 2 x 2 sheet (live frond, dead frond,
boot), so one crown needs one texture. The palm log below reaches into the crown because the crown
counts as leaves.

### Seasons

Autumn and blossom sets are separate blocks: `tree_leaves_<species>_autumn`, and
`tree_leaves_jacaranda_blossom`. An autumn sprite is drawn with its summer sibling's seed
(`SEASON_OF` in `gen_trees.py`), so the leaves are the same shapes in a new colour. Colours are
fixed for each species, with no biome tint, so a street looks the same in any biome.

---

## The Tree Planting Tool

`ItemTreePlantingTool` plants a whole tree of the selected species, made of ordinary log and leaves
blocks. It works in three steps:

1. **Grow.** `TreeGenerators.grow(preset, facing, rng)` fills a `TreePlan`, a map from each
   position to a named part. Logs win over leaves, and leaves over moss.
2. **Check.** Every position must be air, a plant or snow, and editable by the player. If anything
   is in the way, nothing is placed, and the action bar says how many blocks are in the way.
3. **Place.** Logs first, then leaves, then moss, so each has what it hangs from.

The tree leans and reaches the way the player faces. Stand on a sidewalk facing the road and the
tree arches over the road. Sneak and right-click to change species. The preset is the tool's
`csm_tree_preset` NBT ordinal, so **add presets at the end** of `TreePreset`.

A preset (`TreePreset`) is a generator **shape** plus species parameters: trunk and height ranges,
lean, limb count, reach, rise, cluster size, street clearance and the leaves block. Each planting
draws within those ranges from a fresh seed, so a planted row looks alike without being identical.
That idea comes from Biomes O' Plenty's generator builders (see Decisions).

| Shape | How it grows | Presets |
|---|---|---|
| Profile | Straight trunk, crown radius from a profile of height (rounded bottom, pointed top) | cypress, ginkgo, poplar, sweetgum, hornbeam, arborvitae |
| Limb | Trunk leaning by diagonal steps, then limbs drawn as voxel lines to flattened clusters | live oak, elm, plane, honey locust, jacaranda, pepper tree, coast live oak, willow, lemon-scented gum |
| Palm | Sideways offset grows with the square of the height, so the trunk curves; crown on the top log | fan palm, leaning feather palm, queen palm |
| Head | Clear trunk and a clipped ball | ball-head plane |
| Box | Clear trunk and a box crown, wide across the facing | pleached linden: a row joins into a hedge on stilts |
| Pollard | Stout trunk cut back to knuckles, a tuft on each | pollarded plane |

A limbed tree has **no leaves below its street clearance** (about 4 to 5 blocks over a road, 3
in a park). Its canopy stays above traffic and its trunk stays clear.

A limb line never steps on all three axes at once. The log kit bridges edge diagonals, not corner
diagonals, so a corner step is split in two. `TreeGeneratorsTest` grows every preset in every
facing from 20 seeds and fails if the logs are not one piece joined through faces and edge
diagonals. The same test fails if a limbed tree has leaves under its clearance, or if a palm's
crown is not on its top log.

---

## Street tree accessories and plantings (Trees & Plants tab)

All written by `gen_park_plantings.py`, and built from three classes:

- `BlockParkProp`: a sized prop. Its `Kind` is GROUND, COVER, PLANTER, POST, SHRUB or PLANT.
- `BlockParkJoining`: joins its neighbours in four directions, from actual state and a multipart
  blockstate. Its `Kind` is HEDGE, FENCE, BED or PERGOLA.
- `BlockParkFacing`: faces what it attaches to, which sits behind it at +Z. Its `PoleFitted`
  flavour is for the hanging baskets.

**Nothing shares a cell with a trunk.** A tree grate or a mulched tree pit is a full ground block
that the trunk stands on, so the log's ground flare still sees solid ground. A guard round the
trunk would have to share the trunk's cell, so the tree guard became:

- a **hoop fence** round the pit, a joining fence of arches;
- a **tree stake** that stands in the next cell, with its tie reaching back to a thin trunk's skin
  (the element runs past the cell, to z = 22).

**Hanging baskets** are side-mounted accessories (`ICsmPoleFitted`). Their `_thin` and `_pedestal`
model copies move the bracket plate back to the thinner pole's skin, as
`gen_pole_fit_models.py` does for the light mounts. The copies are generated by
`gen_park_plantings.py`, not by that script's catalogue.

**Raised beds** join into one bed with walls only round the outside, as do **fountain basins**.
Their wall is drawn where the side is *not* joined (`side_when "false"`). A hedge or fence draws an
arm where it *is* joined.

---

## Park amenities (Parks tab)

The Parks tab holds the nine items moved from Furniture & Novelties, with their registry ids
unchanged: the swings, teeter totter, park trash can, water bubblers, bird bath and flower pots.
Everything else there is written by `gen_park_amenities.py`.

- **Benches and picnic tables** (`BlockParkBench`) are one block of seat each, placed side by side
  into a run. `LEFT` and `RIGHT` (the sitter's, actual state) say whether the same block, facing the
  same way, continues on that side. The multipart blockstate draws the legs and arms only where the
  run ends, so three in a row are one long bench.
- **Pergola.** Timber posts, and a joining roof of rafters with beams round the outside. The roof's
  collision starts 10 px up, so people walk under it.
- **Fountains.** The basin joins into a pool of any size, and also runs up to any `fountain_` block
  standing in it. The tiered fountain's water and falling water are animated textures (`.mcmeta`).
- **Irrigation.** `BlockIrrigationController` is a wall box that polls the world clock every 2
  seconds by scheduled tick. It powers redstone from 05:00 to 07:00 in game time (23000 to 1000),
  when a real controller runs before a park opens. Right-click toggles a manual override. Only its
  metadata is stored: facing, powered and manual.
  `BlockSprinkler` pops up while powered. Its `TileEntitySprinkler` throws a sweeping rotor stream
  of particles, on the client only. A sprinkler also runs off a **live redstone wire beside it**.
  Dust only powers the blocks it points into, and a line that connects to sprinklers on both sides
  becomes a cross that points nowhere, so a row of heads along one line would stay dry.

---

## The Fabricator

Two rules, registered from `CsmParks.preInit` (`ParksFabricatorRules`):

- **Trees & Plants:**
  - logs cost planks by width (twig or thin 1, medium 2, thick 3, full 4);
  - leaves cost vanilla leaves (2 for a palm crown); moss costs a vine;
  - plantings cost what they are planted with or made of (saplings, tall grass, flowers, gravel,
    sand, planks, stone);
  - grates and the pit fence take the generic steel cost.
- **Parks:**
  - flower pots cost clay; the bird bath and fountains cost stone;
  - wooden benches, tables and the pergola cost planks;
  - the controller costs a control board;
  - everything else takes the generic steel cost.

---

## Generators

| Script | Writes |
|---|---|
| `gen_trees.py` | Bark and leaf-cluster textures; the palm crown sheets and icons; moss; the log and leaves placeholder models and blockstates; the lang for woods, leaves and presets; the tool's icon, model and messages. `--fragments` prints the tab lines |
| `gen_park_plantings.py` | Every block in the accessories and plantings catalogue: textures, element models, blockstates, item models and lang. `--fragments` |
| `gen_park_amenities.py` | The same for the amenities. It borrows `gen_park_plantings.py`'s helpers |

All three take `--check`. Each writes lang lines by key in all four languages, leaving every other
line in place, so the three can share the module's lang files.

The woods, leaves and presets must agree with `TreeWood`, the tab lines and `TreePreset`. The
scripts only append, so existing textures, whose seeds are their index, never change when the
catalogue grows.

---

## Decisions

- **Trees are blocks, not objects.** The user asked for trees built like vanilla trees, which can
  also be edited.
- **The lean comes from diagonal steps, not a lean property.** It is how players already build
  curved trees. The bridge is what makes thin logs work.
- **Leaves are fixed colours for each species, never decay and have no collision.** See the leaves
  section.
- **Nothing bicycle-related.** LDIB covers bicycles, racks, docks and bike-share.
- **Biomes O' Plenty (CC BY-NC-ND 4.0) was a reference for ideas only.** Those ideas were a
  generator for each shape with parameter ranges, checking the whole volume before placing, a
  curving palm offset, limbs as voxel lines to clusters, and profile trees. Nothing of its code,
  textures or models was copied or adapted.

## Traps

- **The `Block` constructor asks before your fields exist.** `isOpaqueCube` and `createBlockState`
  run inside `super(...)`. A class constructed by name must stash whatever those calls read (the
  registry name, `BlockParkProp`'s `Kind`) in a `ThreadLocal` before calling `super`, as every
  class here does. A null field there crashes pre-initialization.
- **Item-model parents resolve from `models/`.** In a blockstate, `csm:parks/x` means
  `models/block/parks/x`. In a model's `parent` it means `models/parks/x`, which does not exist.
  Write `csm:block/parks/...`.
- **A world saved with another module's blocks stalls a client without that module.** It stalls on
  FML's missing-registry prompt. Test a subset in a fresh world, or with the modules the world was
  saved with.
- **An element can only turn in steps of 22.5 degrees, up to 45.** The slide's chute is 45
  degrees, and the fuller grasses are four planes at ±22.5 degrees about each axis.
- **Edge diagonals only.** Anything that lays logs (a generator, a player) must avoid corner-diagonal
  steps, or the trunk falls apart into pieces.
