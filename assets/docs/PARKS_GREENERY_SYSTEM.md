# Parks & Greenery

**CSM: Parks & Greenery** (`csm_parks`, tree `modules/parks`) is street trees, plantings and park
amenities. It has two creative tabs: **Trees & Plants** (order 19) and **Parks** (order 20).

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
| Hanging moss | `BlockHangingMoss` | Spanish moss and willow strands, hung under a limb or crown |

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

### Leaves: sheets and cards, not cubes

Leaves drawn as full cubes read as stacked boxes, and a one-wide crown looks like a pillar. A
leaves block is drawn from the species' leaf-cluster sprite (`TreeLeavesGeometry`) as:

- a **sheet** on each **open** face (one with no leaves and no opaque block beyond it): one quad
  1 px inside the face, facing out, whose ragged alpha keeps the outline soft;
- a **tuft**, a double-sided cutout card reaching up to 3 px past each open side and the top, so a
  crown's outline is not the block grid;
- one card inside the cell (two in an upright needle column), so a gap in the crown is not sky.

Nothing is drawn between two leaves cells, and Fast graphics leaves out the tufts and the curtain.
The crowns read a little blockier than cards alone would, which is deliberate: see Rendering cost.

The open faces and a per-position variant (0 to 3) travel in the extended state (`SHAPE`), so
neighbouring blocks differ but a given block always looks the same.

`TreeLeafType` decides the arrangement. The species decides the texture.

| Type | For |
|---|---|
| `BROADLEAF` | oaks, elm, plane, ginkgo, poplar, sweetgum, hornbeam, linden |
| `AIRY` | honey locust, jacaranda, gum: an open sprite, so the sheets let the light through |
| `NEEDLE` | cypress, arborvitae: upright narrow cards, so a one-wide column is the whole tree |
| `WEEPING` | pepper tree, willow: from the crown's underside (a cell open below), long narrow strands hang down each open side and under the cell, reaching well below it, in place of the side tufts |
| `CLIPPED` | the pleached linden: a flat leafy face flush with each open side, for topiary |
| `PALM_FAN`, `PALM_FAN_SKIRT`, `PALM_FEATHER` | palm crowns, drawn by `TreePalmGeometry` |

Leaves never decay on their own: a street tree that disappears would ruin a build. They go only
when the logs holding them are felled (see Felling). They have **no collision**,
so a canopy never blocks a sidewalk or snags on a sign, but the selection box is the full block so
they are still easy to break.

### Palm crowns

A palm crown is one leaves block on top of a palm log. Its fronds are bent strips that reach well
past the cell: a fan palm's round fans, or a feather palm's long arching fronds. The skirted fan
crown adds dead fronds hanging down the trunk. The sprite is a 2 x 2 sheet (live frond, dead frond,
boot), so one crown needs one texture. The palm log below reaches into the crown because the crown
counts as leaves.

The cabbage palm has its own crown (`PALM_CABBAGE`, and `_SKIRT` with the dead fronds): a full
round head rather than the fan palm's tight ball. It has 33 fronds in three tiers, reaching up, level
and bowed down, each tier turned from the one above so it fills the gaps, and each frond bending
further toward its tip, as a sabal's costapalmate fans do. The fronds are 48 px long and 26 px
wide, against the fan palm's 26 and 16. The boot is sized for the stouter trunk (a leaf type's
`bootRadius`), and the sheet has its own darker, bluer green. The lowest tier may bow half a block
below the crown's cell; only a skirt hangs further. The trunk is its own wood, `sabal`, a medium
log whose bark is the criss-cross of old frond bases.

### Seasons

Autumn and blossom sets are separate blocks: `tree_leaves_<species>_autumn`, and
`tree_leaves_jacaranda_blossom`. An autumn sprite is drawn with its summer sibling's seed
(`SEASON_OF` in `gen_trees.py`), so the leaves are the same shapes in a new colour. Colours are
fixed for each species, with no biome tint, so a street looks the same in any biome.

---

## The Tree Planting Tool

`ItemTreePlantingTool` plants a whole tree of the selected species, made of ordinary log and leaves
blocks. It works in three steps:

1. **Grow.** `TreeGenerators.grow(preset, heading, rng, space)` fills a `TreePlan`, a map from
   each position to a named part. Logs win over leaves, and leaves over moss. The `TreeSpace`
   answers, cell by cell, what is around the spot: `FREE` (air, a plant, snow, a ground cover such
   as a `COVER` prop or a carpet, and editable by the player), `FOLIAGE` (another tree's leaves)
   or `BLOCKED` (anything else).
2. **Refuse only for want of a trunk.** The tree grows into whatever room there is (next
   section). Nothing is planted only when the trunk cannot reach its shortest clear height, or the
   tree would have no leaves at all; the action bar then says there is no room for its trunk.
   Clicking a ground cover plants through it: the trunk replaces the cover and stands on the
   ground, where it would otherwise stand a block up, on top of a one-pixel layer.
3. **Place.** Logs first, then leaves, then moss, so each has what it hangs from. If anything was
   pruned to fit, the action bar says how many blocks were cut back.

In the open the tree leans and reaches the way the player looks (the yaw, not the nearest
facing). Stand on a sidewalk facing the road and the tree arches over the road. Sneak and
right-click to change species. The preset is the tool's `csm_tree_preset` NBT ordinal, so **add
presets at the end** of `TreePreset`.

### Growing into the room there is

A street tree planted against a building has spent years leaning away from it and being pruned
flat on that side. The generator reproduces the result rather than the years:

- **It looks round first.** `awayFromWalls` marches out in 16 directions at the heights the crown
  will fill, and sums a vector pointing from what is built toward the open, longer the nearer and
  wider the walls. The planter's heading is jittered (about 20 degrees, so a row is not cloned)
  and then turned by that vector; the walls win. Pushed off a wall, a tree leans a step or two
  further than it would in the open.
- **The trunk steps sideways on one axis at a time**, whichever keeps it nearer the heading's
  line, so a diagonal lean is a staircase of edge-diagonal steps the log kit bridges. It goes
  round something overhead where it can.
- **Limbs keep a block off anything built** (no wall beside a limb log, no roof on it). A limb
  that would run into something tries turning up to about 60 degrees either way, and takes the
  direction that runs furthest, less a little for each step turned and for crowding a limb already
  grown. Where it cannot turn it is cut back, and keeps a smaller tuft at the cut.
- **Foliage is filled outward from its limb through open cells only**, so a crown meets a wall
  with a flat face (leaves may touch the wall; logs may not) and never reaches through it or round
  it into a room. Leaves also need three clear blocks over them, so nothing grows under an awning
  or balcony.
- **Another tree's leaves are no wall.** A limb and a crown grow up to them without entering, and
  sensing ignores them, so a row planted a few blocks apart joins into one canopy over the street.

For the variety the open-ground trees lacked, a limbed tree now also:

- forks into two leaders now and then (`fork`: elm 60%, jacaranda 40%, plane and pepper tree
  30%), each carrying half the limbs and its own closing cluster;
- springs some limbs a block or two lower on the trunk;
- grows each limb as an arch (up early, then levelling off) with its own curve, reach jitter and
  fan jitter;
- puts out a side branch with a smaller cluster from about 45% of the way along a long limb
  (`branchChance`, 45%), sharing the limb's foliage rather than adding to it;
- draws each cluster a little lopsided.

A preset (`TreePreset`) is a generator **shape** plus species parameters: trunk and height ranges,
lean, limb count, reach, rise, cluster size, street clearance and the leaves block. Each planting
draws within those ranges from a fresh seed, so a planted row looks alike without being identical.
That idea comes from Biomes O' Plenty's generator builders (see Decisions).

| Shape | How it grows | Presets |
|---|---|---|
| Profile | Straight trunk, crown radius from a profile of height (rounded bottom, pointed top) | cypress, ginkgo, poplar, sweetgum, hornbeam, arborvitae, Colorado blue spruce, coast redwood |
| Limb | Trunk leaning by diagonal steps, then limbs drawn as voxel lines to flattened clusters | live oak, elm, plane, honey locust, jacaranda, pepper tree, coast live oak, willow, lemon-scented gum, paper birch, Japanese maple, Scots pine, European beech, old English oak, camphor |
| Palm | Sideways offset grows with the square of the height, so the trunk curves; crown on the top log | fan palm, leaning feather palm, queen palm, cabbage palm |
| Head | Clear trunk and a clipped ball | ball-head plane |
| Box | Clear trunk and a box crown, wide across the facing | pleached linden: a row joins into a hedge on stilts |
| Pollard | Stout trunk cut back to knuckles, a tuft on each | pollarded plane |
| Tiered | Tall trunk; above a clear stretch, a whorl of level limbs every two or three blocks, each tipped with a flat pad, shorter toward the top, each whorl turned from the last | eastern white pine |

The last six presets are regional: paper birch (New Hampshire), Colorado blue spruce, cabbage
palm (Florida), Japanese maple, Scots pine (Sweden) and European beech (Denmark). California was
already covered by the coast live oak. They brought five woods (birch, maple, spruce, pine,
beech) and eight leaves blocks, autumn sets for the birch, maple and beech among them. The
Japanese maple's leaves are small five-lobed stars (the `palmate` sprite), and the spruce and
pine wear a needle sprite across a whole cluster (`needle_wide`) with the broadleaf
arrangement, since a wide conifer crown needs sheets rather than the cypress's upright cards.
The cabbage palm first wore the fan palm's crown on a thin trunk, which looked spindly; it now
has its own crown and trunk (Palm crowns, above), skirted more often than not.

Four big trees followed, for specimens and parks rather than streets:

| Preset | Shape | How big | What it is |
|---|---|---|---|
| Coast redwood (California) | Profile | 30-36 tall | A full-width trunk clear for 7-9 blocks, a narrow dark spire above |
| Eastern white pine (New Hampshire's state tree) | Tiered | 22-27 tall | A tall straight trunk, layered pads of soft blue-green needles |
| Old English oak (Denmark's Kongeegen, Sweden's old oaks) | Limb | 25-30 across | A full-width trunk and heavy limbs under a vast, dense crown |
| Camphor tree (Japan's shrine trees) | Limb | a high dome some 25 across | A full-width trunk forking low into climbing limbs |

A big broadleaf crown reads as one crown only when its clusters are nearly as wide as its limbs
are long; with the reach of a small tree's and bigger clusters the crown broke into separate
lobes. They brought four woods (redwood, white pine, oak, camphor) and five leaves blocks,
`oak_autumn` among them.

A limbed tree has **no leaves below its street clearance** (about 4 to 5 blocks over a road, 3
in a park). Its canopy stays above traffic and its trunk stays clear.

A limb line never steps on all three axes at once. The log kit bridges edge diagonals, not corner
diagonals, so a corner step is split in two. `TreeGeneratorsTest` grows every preset in every
facing from 20 seeds and fails if the logs are not one piece joined through faces and edge
diagonals. The same test fails if a limbed tree has leaves under its clearance, or if a palm's
crown is not on its top log. It also grows every preset against a wall and in a building's
corner: nothing may grow into a wall, no limb may stand against one, a tree by a wall must reach
mostly away from it, nothing grows under an awning, another tree's leaves are never entered, and
every leaf must be near enough a log that felling a neighbouring limb could not strip it.

---

## Felling

Broken by a player, a tree log fells what it alone held up (`BlockTreeLog.removedByPlayer` to
`TreeFelling.fell`). **Sneaking breaks just that block**, so a tree can still be edited by hand.

- **Logs.** From each log joined to the broken one (through faces and edge diagonals, as the kit
  draws them), the connected logs are searched. A piece is held up if any of its logs stands on
  something solid that is not part of a tree, or is joined to a vanilla log; otherwise it falls,
  dropping its logs unless the player is in creative. Cut the trunk and the tree comes down; cut
  a limb and only the limb goes. A log touching a wall does not hold anything up: the generator
  keeps logs off walls, and a limb resting on a roof stands on it anyway.
- **Leaves.** The leaves joined, through leaves, to what went are kept if they are within
  `LEAF_REACH` (8) steps through leaves of a remaining log, or stand straight on one through
  leaves (a cypress's one-wide column, which has no room for a trunk inside it). The rest go,
  dropping nothing, as decaying leaves do. A neighbour's canopy that touched the felled tree keeps
  every leaf near its own logs.
- **Bounded.** More than 2,048 logs in a piece is taken as held up, and leaves are searched only
  within a box 26 blocks past what went, with the box's edge counted as held, so a felling can
  only ever do too little. The first 24 logs play their break effect; the rest go quietly.

The searches are pure functions over a `Cells` view (`TreeFellingTest`).

---

## Tree tools

Next to the Tree Planting Tool in the Trees & Plants tab, three tools take trees down and back
(`parks/tools/`): the **chainsaw** (`ItemChainsaw`), the **pole trimmer** (`ItemPoleTrimmer`) and
the **tree shears** (`ItemTreeShears`). None is enchantable and none is repairable by combining:
the chainsaw is not an axe to be given Efficiency or Unbreaking, and fuel and wear are its cost.
The chipper, stump grinder and any vehicle were left out on purpose.

### The chainsaw

| Action | What it does |
|---|---|
| Right-click (off) | Pulls the cord. It catches on a random third to fifth pull; each pull has its sound, the last the start, then it idles |
| Sneak + right-click (off) | Refuels: one item from the inventory, the one burning longest that still fits; a lava bucket hands back its bucket. In creative, with no fuel to hand, it fills the tank |
| Sneak + right-click (running) | Stops it |
| Left-click a log (running) | Cuts fast, and the tree standing on the log falls at once. Sneaking cuts only that log, as with a hand-broken tree log |

- **Fuel, because a free tree-feller is a free wood farm.** It burns what a furnace burns
  (`TileEntityFurnace.getItemBurnTime`, which includes Forge's fuel event), kept on the stack in
  burn ticks (`ChainsawFuel`). The tank holds 20,000 ticks, a lava bucket's worth, so a lava bucket
  only goes into an empty saw. It burns one tick a tick while running in the hand and 40 a log
  felled: a piece of coal is 80 seconds of idling or 40 logs. It also wears one durability a log
  (1,200 in all).
- **Running state.** The stack holds whether it runs, the pulls so far and the pulls needed. It
  stops when the tank runs dry, when it has not been the selected item for three seconds, or when
  it is dropped. Fuel is written once a second, not every tick, so the held item is not resent to
  the client each tick, and `shouldCauseReequipAnimation` keeps a fuel change from bobbing it. The
  item model swaps to its running sprite by the `csm:running` property, registered in the client
  proxy because `IItemPropertyGetter` is client-only.
- **Felling.** `onBlockStartBreak` takes over the break of a log (after the break event has
  passed): it removes and harvests the cut log itself, then fells. This module's trees fall by
  `TreeFelling`, exactly as a hand-broken log fells them. Any other tree falls by
  `AnyTreeFelling`, a guess kept timid: logs of any mod (`BlockLog`, `isWood`, or an ore
  dictionary `log*` name) joined to the cut through faces, edges and corners, **at or above the
  cut** and within 12 blocks sideways, and only when they touch at least two **natural** leaves
  (vanilla marks a leaves block a player placed as not decaying). A log cabin has no leaves, so a
  wall log is cut alone. More than 512 logs is taken as a build and nothing past the cut falls.
  Leaves within 8 steps of the felled logs go, except those within 4 of a log that stays (a
  neighbouring tree's, as vanilla leaves live within four of a log) or of leaves past the search;
  at most 2,048. Leaves drop what decaying leaves drop. A cell the player may not change is skipped.
- **The mess.** A felling leaves brush piles (`BlockBrushPile`: low, walked through, broken
  instantly, two to four sticks) around the stump, `chainsawBrushPiles` in Core's `csm.cfg`
  (`parks` category): `NONE`, `FEW` (default, one to three) or `MANY` (three to eight). A pile goes
  only in an air cell on a solid top **open to the sky**, within four blocks of the stump and two
  up or down, one a column, never replacing anything. The sky test is what keeps them out of
  buildings: a tree felled beside a house leaves nothing on its floor, under its porch or in its
  basement. Placing a block over a pile replaces it, as with tall grass.
- **Sounds** (`ParksSounds`, `gen_parks_tool_sounds.py`): the pull, the start, a one-second idle
  played back to back from the server while it runs, and the cut. All synthesised.

### The pole trimmer and the tree shears

Manual, no fuel, durability (350 and 476). Both cut small growth only (`BranchCutting`): leaves
of any mod, this module's hanging moss, and its twig and thin logs, and of a log only when what
falls with it is a branch: no more than 48 logs, every one twig or thin (`isBranch`). A thin
leader carrying the crown, a thicker log, a palm crown or another mod's log is too big: a
left-click on one is cancelled with the hint "Too big for the ... -- use a chainsaw"
(`TreeToolEvents`). On what they may cut they mine fast.

Right-click cuts: the shears within normal reach, the pole trimmer up to 8 blocks away by its own
ray trace from the eyes, for trimming a canopy from the ground. A cut log goes through
`TreeFelling.fell`, so the branch past the cut falls with the leaves only it held, and the tree
stays (`BranchCutTest` cuts every small log of every preset to hold that). A cut leaves block takes
the leaves it alone kept within reach of a log (`TreeFelling.orphanedLeaves`). Each cut fires the
break event, so protection mods can refuse it.

### Bounds

| | Limit |
|---|---|
| Logs felled by the chainsaw on another mod's tree | 512 (more: nothing past the cut falls) |
| Sideways reach of that felling | 12 blocks from the cut |
| Leaves taken with it | 2,048, within 8 steps of a felled log |
| Logs taken by one branch tool cut | 48, all twig or thin |
| Brush piles | 0, 1-3 or 3-8; within 4 blocks of the stump |
| This module's trees | `TreeFelling`'s own bounds (2,048 logs, 16,384 leaves) |

The searches are pure over cell views and tested without a world (`AnyTreeFellingTest`,
`ChainsawFuelTest`, `BranchCutTest`). Recipes (Core's `recipes/`, on `forge:mod_loaded`
`csm_parks`): the chainsaw from iron, sheet metal and a piston; the tree shears from shears and two
sticks; the pole trimmer from the tree shears and two sticks.

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
  (the element runs past the cell, to z = 22). Placed like any facing prop it would face the
  player, pointing the tie wherever the player happens to look, so `BlockParkFacing.TreeStake`
  turns it toward a neighbouring log when it is placed (the one looked toward first).

**Hanging baskets** are side-mounted accessories (`ICsmPoleFitted`). Their `_thin` and `_pedestal`
model copies move the bracket plate back to the thinner pole's skin, as
`gen_pole_fit_models.py` does for the light mounts. The copies are generated by
`gen_park_plantings.py`, not by that script's catalogue.

**Raised beds** join into one bed with walls only round the outside, as do **fountain basins**.
Their wall is drawn where the side is *not* joined (`side_when "false"`). A hedge or fence draws an
arm where it *is* joined.

A raised bed's wall stops 2 px short of each corner, and the corner is a post of its own, drawn
where either wall beside it is (a multipart `OR`). Two whole walls overlapped there, and a turned
wall's top carries turned pixels, so the corners flickered. The soil draws only its top and
underside: its sides lay on the walls' outer faces. The fountain basin keeps whole walls, turned
with `uvlock` as the pergola's beams are, so its corners carry the same stone.

### Regional plantings and nursery pots

A handful of plants native to each of six places, so a garden or a plant nursery can be stocked
by region. Each is one `BlockParkProp`, like the plantings above, grouped in the tab by region:

| Region | Plants |
|---|---|
| California | California poppy, manzanita, California lilac (ceanothus), white sage, deergrass |
| New Hampshire | mountain laurel, highbush blueberry, winterberry, wild lupine, pink lady's slipper |
| Colorado | blue columbine, Rocky Mountain penstemon, rubber rabbitbrush, big sagebrush, blue grama, soapweed yucca |
| Florida | saw palmetto, coontie, American beautyberry, firebush, coreopsis, pink muhly grass |
| Japan | satsuki azalea, camellia, Japanese iris, bamboo, Japanese forest grass (the hydrangea already there is Japan's too) |
| Sweden and Denmark | heather, lingonberry, wood anemone, harebell, oxeye daisy (marguerite) |

- **Flowers, grasses and the palm-like plants** are four crossed planes (`PLANT`, walked
  through), each its own drawing: a stem stand with a head per stem (`herb_tex`, which a lupine
  stand fills with mixed colours and a poppy stand with one), or a drawing of its own where the
  plant's shape is the point (the yucca's rosette and bell stalk, the saw palmetto's fans, the
  coontie's arching fronds, the iris's swords, the lady's slipper's pouch).
- **Shrubs** are stacked boxes as before, but shaped to the species (a clipped satsuki dome, a
  tall camellia oval, a low heather mound, sagebrush and manzanita lopsided), with bare stems
  under a raised canopy where the plant has them, white sage's spikes standing over it, and
  berries dotted singly over the leaves.
- **Bamboo's** canes, nodes and leaf sprays repeat every eight pixels, so blocks stacked on each
  other read as one tall grove.
- **Every regional plant also comes potted** (`potted_<name>`): the same model at 60% on the soil
  of a black plastic nursery container (8 x 6 px), so a nursery's benches and rows can be filled
  and a plant planted out beside its pot. The pot is added by the generator (`potted()`), not
  drawn per plant. A potted plant is a `SHRUB`: it has a box to stand on and to bump into.

### Nursery, garden centre and farm

Fifteen pieces for laying out a plant nursery, a garden centre or a small farm, at the end of
the tab. The plants for them are the `potted_` blocks; none of these is a potted plant again, and
none repeats the Furniture tabs' hose reel, storage crates, produce crates, wooden barrel,
pumpkins or scarecrow.

| Group | Pieces |
|---|---|
| Planters (`BlockParkProp`, `PLANTER`) | small and large terracotta pots, a glazed ceramic urn, a concrete bowl of bedding flowers, a half barrel |
| Nursery | a window box of trailing flowers (`BlockParkFacing`, hung against the wall behind it), the nursery growing bench (`BlockParkJoining.Kind.TABLE`), a potting bench, stacked empty nursery pots, seedling flats |
| Farm | a slatted compost bin, a wheelbarrow of soil, lettuce and staked tomato rows (`BlockParkCrop`), a trellis with a clematis |

- **A pot's rim is a ring drawn into its top texture** at the pixels the rim box's up face samples
  (`rim_top`), so a small pot's soil sits inside a rim rather than across the whole top.
- **The nursery bench is a block high**, so a potted plant placed on it stands on its mesh top. It
  joins into a run like the raised beds: rails only round the outside, stopping short of corner
  pieces. Each open side carries the leg at its left-hand corner, so a run has one leg a block
  along each long edge and every corner of the whole bench has one.
- **Crop rows face**, so a field can run either way; their ridge runs across the block, so rows
  placed end to end are one ridge. `BlockParkCrop` is the facing prop in plant material: broken
  by hand, walked through, and it burns.
- **The wheelbarrow's wheel is an exact octagon**, a cross of two rectangles and the same cross
  turned 45 degrees, in one flat colour so the four sides on one plane show the same pixels.

---

## Park amenities (Parks tab)

The Parks tab holds the nine items moved from Furniture & Novelties, with their registry ids
unchanged: the swings, teeter totter, park trash can, water bubblers, bird bath and flower pots.
The two swing sets, the teeter totter, the slatted trash can and the bird bath are drawn by
`gen_park_legacy_amenities.py` (the swing sets as OBJ, since at 2.78 m to the beam they pass
the two blocks a JSON model can reach); everything else there is written by
`gen_park_amenities.py`. The two flower pots keep their own classes (a six-way facing, which
saved worlds store) but draw the Trees & Plants tab's `planter_concrete` and
`planter_corten` models, with their box.

- **Benches and picnic tables** (`BlockParkBench`) are one block of seat each, placed side by side
  into a run. `LEFT` and `RIGHT` (the sitter's, actual state) say whether the same block, facing the
  same way, continues on that side. The multipart blockstate draws the legs and arms only where the
  run ends, so three in a row are one long bench.
- **Sitting.** Right-click a bench or table to sit on Core's `EntityCsmSeat` (the portable
  toilet's seat), and sneak to get up. A bench seats one a block, on the middle of its slats,
  facing out. A picnic table seats one on each side: the bench on the player's side, facing across
  the table, and you step out on that side. That second seat is why `EntityCsmSeat.sit` has an
  overload taking the box another seat must be in to count as taken.
- **Pergola.** Timber posts, and a joining roof that sits straight on them: a beam along each
  outer row, centred over the post line, and rafters across on top. Where two beams cross at a
  corner the turned one is drawn with `uvlock`, so the shared faces carry identical pixels and
  cannot z-fight.
- **Gazebos** are a kit, like the pergola:
  - posts (`BlockParkColumn`, which draws its base only on the bottom block of a stack and its
    capital only on the top one, so a three-block post is one column);
  - railings (a `RAIL` joining block, which also runs into a post beside it);
  - a board deck;
  - one **roof block** set on top of the middle of the footprint: `gazebo_roof_3x3` or
    `_5x5`.

  The roof is the one OBJ model in the module. A hip roof has sloped triangular faces, which no
  JSON element can draw, and its footprint (up to 86 px across) is past the -16..32 an element
  may reach. It has:
  - four shingled faces up to a cupola and finial;
  - a white fascia round the eave, which sits on the posts;
  - a board ceiling underneath, tiled at 16 px so the boards keep their width.

  The roof block is solid only inside its own cell (Kind `ROOF` clips its collision), because a
  box past the cell is only consulted when an entity is inside the cell. Its selection box
  covers the whole roof.
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
    sand, planks, stone), by name: `shrub_` and `plant_` a sapling, `grass_` tall grass,
    `flower_` a flower; a `potted_` plant costs its plant and a flower pot;
  - grates and the pit fence take the generic steel cost;
  - the nursery and farm pieces cost what they are made of: clay for the terracotta pots and
    the urn, planks for the barrel, window box, potting bench and compost bin, stone for the
    bowl, flower pots for the stacked pots, seeds for the flats and crop rows, sticks and a vine
    for the trellis. The nursery bench and the wheelbarrow take the generic steel cost.
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
| `gen_park_plantings.py` | Every block in the accessories and plantings catalogue, the regional plantings and their potted copies, and the nursery, garden centre and farm pieces: textures, element models, blockstates, item models and lang. `--fragments` |
| `gen_park_amenities.py` | The same for the amenities. It borrows `gen_park_plantings.py`'s helpers |
| `gen_park_legacy_amenities.py` | The models, textures and blockstates of the five amenities that kept their old ids (both swing sets, the teeter totter, the trash can, the bird bath). It writes no lang and no tab lines, since those blocks already have them |
| `gen_parks_tools.py` | The tree tools' item sprites and models (the chainsaw's running sprite by its `csm:running` override) and the brush pile's textures, model and blockstate. `--check`. The tools' lang and tab lines are hand-written |
| `gen_parks_tool_sounds.py` | The chainsaw's four sounds, synthesised (numpy to ffmpeg to OGG), and their `sounds.json` entries. No `--check`: Vorbis is not byte-stable |

All three take `--check`. Each writes lang lines by key in all four languages, leaving every other
line in place, so the three can share the module's lang files.

The woods, leaves and presets must agree with `TreeWood`, the tab lines and `TreePreset`. The
scripts only append, so existing textures, whose seeds are their index, never change when the
catalogue grows.

---

## The demo world

`dev-env-utils/scripts/build_parks_demo.py` builds a showcase of the whole module in a flat
creative world loaded in the dev client: a street of leaning trees on grates with pleached lindens
opposite, a park (fountain plaza, playground, pergola with picnic tables, community garden, pond
and willow, an irrigated lawn), an arboretum of every planting preset with a sign each, and the
tree kit: every leaves block on a plinth, a hand-built lean in each log width, every bark, the palm
crowns, moss and willow strands. It plants with the real tool, so it doubles as a test of it.

## Rendering cost

Trees are chunk geometry (baked models, no renderer), so their cost is the quads they add to
their chunks, and the cutout fragments those quads cover. A whole demo world of trees in view cost
about 0.2 ms a frame on a fast card, but a canopy is exactly where a slower one feels it.
`TreeRenderBudgetTest` counts each preset's quads exactly, from the geometry the baked models use,
and fails when any preset grows more than 15% past its recorded budget.

One pass (2026-09-23) took one of every preset from 82,178 quads to 31,199 without changing how
the trees read. The adaptive generator that followed (forks, side branches, lopsided clusters)
brought it back to about 33,700, the price of limbed trees that no longer all look alike. The six
regional presets added about 9,800 more (the beech, a big broad dome, is 3,951 of it). The
cabbage palm's own crown took it from 231 to 439, and the four big trees added about 29,400: the
old oak, the camphor and the white pine are some 9,000 each, nearly half of it their thick limbs.
They are specimens: a street of them costs what a street of three times as many live oaks does. Leaves were about 85% of a tree, at 25 to 30 quads a cell.
- **Sheeted leaves.** Every leaf type but clipped draws a leaf sheet on each open face (one quad,
  facing out, 1 px inside the face), a tuft card past each open side and the top, and one card
  inside, where they had drawn six interior cards, three fringe cards per open face and cover
  cards. The crowns read a little more like blocks, deliberately (the user preferred it), and keep
  their ragged outline. Airy leaves are sheeted too; their sprite is mostly gaps, so the jacaranda
  and honey locust still let the light through.
- **A curtain only under the crown.** Weeping leaves hang their strands only from cells open
  below, two to a side and two under, and draw no side tufts there. Strands hung higher up the
  crown's wall were hidden behind the ones below them. The willow went from 7,822 quads to 5,557
  and the pepper tree from 4,767 to 3,242.
- **Fewer log sides where they cannot be seen.** A twig is a 4-sided tube and a thin log 6-sided;
  8 sides only from medium up.
- **Straight-through tubes.** A log between two logs of its width in a straight line draws one
  tube through the cell, not two arms meeting at the centre.

Off the render thread: a log's collision boxes are cached per width and connection mask
(`TreeLogGeometry.boxes`), since every entity near a tree asks for them every tick. The sprinkler's
spray is client-only particles, which the game already drops beyond 32 blocks, and the irrigation
controller is one scheduled tick every two seconds. Nothing in the module has a tile entity
renderer.

## Decisions

- **Trees are blocks, not objects.** The user asked for trees built like vanilla trees, which can
  also be edited.
- **The lean comes from diagonal steps, not a lean property.** It is how players already build
  curved trees. The bridge is what makes thin logs work.
- **Leaves are fixed colours for each species, never decay on their own and have no collision.**
  See the leaves section.
- **A tree adapts to its site; it is not refused for it.** The tool used to refuse a tree if any
  cell was in the way. Now it grows into the room there is, and refuses only when the trunk has
  none, because a street tree against a building is exactly the case a city needs.
- **Leaves touch a wall, logs keep off it.** A crown pruned flat against a facade reads right; a
  limb pressed into one does not.
- **Felling is on by default, sneaking opts out.** The user asked for trees that come down when
  cut, and hand editing still needs single blocks.
- **Nothing bicycle-related.** LDIB covers bicycles, racks, docks and bike-share.
- **Biomes O' Plenty (CC BY-NC-ND 4.0) was a reference for ideas only.** Those ideas were a
  generator for each shape with parameter ranges, checking the whole volume before placing, a
  curving palm offset, limbs as voxel lines to clusters, and profile trees. Nothing of its code,
  textures or models was copied or adapted.

## Traps

- **Faces on one plane z-fight, and the generators separate them.** A bench's end frame over its
  seat, a raised bed's post over its side: two boxes putting different pixels on one plane flicker
  in game. `gen_park_plantings.py`, `gen_park_amenities.py` and `gen_park_legacy_amenities.py` end
  their `generate` with `model_depth.py`, which moves the smaller face along its normal until
  the planes are 0.2 px apart (see Residential Furniture in `NOVELTIES_SYSTEM.md`). Draw details
  flush and let it do so; hand-nudged offsets are measured as the design.
- **`model_depth.py` sees only the unturned parts of a multipart.** A side turned by `y` is
  never measured against the post or the other sides, and item models no blockstate draws are
  not measured at all. Where turned sides meet (a bed's corners), keep them from overlapping, or
  turn them with `uvlock` so they carry the same pixels. It also moves the smaller face out in
  front: the raised bed's soil side ended up 0.2 px in front of its wall, so leave out a face
  that only lies on another rather than letting it be separated.
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
- **A trunk cell may stand against a wall; a limb may not.** A tree may be planted in a pit right
  beside a building, so `canLog` accepts any open cell and only `canLimb` asks for the margin.
- **Leaf reach and cluster size go together.** Felling keeps leaves within `LEAF_REACH` of a log.
  A preset whose clusters reach further from their limbs would have part of its crown stripped
  whenever a neighbouring limb is cut; `TreeGeneratorsTest.everyLeafIsNearALog` catches it.
- **Edge diagonals only.** Anything that lays logs (a generator, a player) must avoid corner-diagonal
  steps, or the trunk falls apart into pieces.
