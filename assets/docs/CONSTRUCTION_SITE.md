# Construction Site

The tools and temporary works that stand around a building while it goes up: frame scaffolding,
formwork, shoring and rebar, fencing, earthworks, the materials stacked around a site, its
facilities, and a tower crane. One creative tab, **Construction Site**, shipped by
`csm_building` alongside the materials in `WALL_MATERIALS.md` and the structure in
`FRAMING_SYSTEM.md`.

| Group | Blocks and items | Generator |
|---|---|---|
| Scaffolding | Frame Scaffold; add-on items Ladder Frame, Debris Netting, Casters | `gen_scaffold.py` |
| Formwork and shoring | Wall Formwork, Column Formwork, Post Shore | `gen_formwork.py` |
| Rebar | Rebar Mat, Rebar Dowels, Rebar Column Cage, Rebar Bundle | `gen_formwork.py` |
| Tower crane | Tower Crane Mast (1x1), Tower Crane Mast (2x2), Tower Crane Head | `gen_crane.py` (masts); the head is drawn in Java |
| Earthworks | Trench Plate, Trench Box, Soil / Gravel / Sand Stockpile | `gen_earthworks.py` |
| Fencing | Temporary Fence (plain and with privacy screen), Silt Fence; in Building Materials, Chain-Link Fence (galvanized, black) and its Barbed Wire Top | `gen_fencing.py` |
| Logistics | Brick, Concrete Block, Drywall and Bagged Concrete Pallets; Lumber Stack; Insulation Rolls; PVC Pipe and Conduit Bundles; Wire Spool | `gen_logistics.py` |
| Facilities | Shipping Container and Roll-Off Dumpster (blue, maroon, green, grey); Job Trailer Wall, Window, Door; Portable Toilet; Gang Box; Concrete Washout | `gen_facilities.py` |

Classes are in `modules/building/src/main/java/com/micatechnologies/minecraft/csm/constructionsite/`,
the tab in `tabs/CsmTabConstructionSite.java`. Every generator has `--check`.

---

## Frame scaffold

One block, `scaffold_frame`, that draws itself from its neighbours. A real frame scaffold is end
frames, cross braces and planks, but a cell holds one block, so each block is one bay and decides
its own parts: an end frame on its near side always and on its far side only where the run ends, a
cross brace on each open long face, a plank deck on top when nothing is stacked on it, screw jacks
where it stands on something that is not scaffold, and a guardrail on every top edge you could fall
from.

**Stored vs actual state.** Stored: the axis the frames run across, and three add-ons (ladder frame,
netting, casters), one bit each -- exactly the four bits of metadata. Everything else is actual
state. That was a requirement of the first build: the look can change in the generator without
invalidating a placed block, so the design could pivot if it read wrong in game.

**Sides are three-valued** (scaffold / open / rail). With the add-ons, two-valued up/down plus
per-side booleans came to 16,384 states; three-valued sides hold it at 5,184.

**Guardrails are automatic, with no per-edge toggle**, by the user's choice. An edge gets none where
the scaffold carries on, where the neighbour's top is solid so you would step out level, or where a
solid face stands at walking height (the building the scaffold is against). A toggle would also
not fit in metadata beside the axis and add-ons.

**Add-ons are items used on a placed bay**, not variants in the tab: sneak-right-click with an empty
hand takes the last one off. Each has a crafting-table recipe in Core
(`recipes/scaffold_{netting,casters,ladder_frame}.json`, gated on `csm_building`).

**Climbing.** The whole block is a ladder, and jump climbs it (`ScaffoldClimbHandler`, a client
`PlayerTickEvent` that answers to any block implementing `ICsmSiteClimbable`). The deck is solid
only to something standing on it and not sneaking, so climbing up through a stack never meets a
plank overhead, and sneaking on the top deck drops you back inside to climb down -- the behaviour
later versions' vanilla scaffolding has.

### Traps

- **A rail's collision reaches out of its cell.** A guardrail belongs to the deck below the space
  it guards. 1.12 only asks blocks from one below an entity's feet upward for collision, so at the
  top of a jump the deck is not asked and the rail is not there: sprint-jumping cleared it every
  time. `ScaffoldRailCollision` (a `GetCollisionBoxesEvent` handler, players only, registered on
  both sides) asks the one row the world skips. A vanilla fence never has this problem because it
  lives in the cell at the feet.
- **An element outside 0..16 needs explicit UVs**, or it samples the neighbouring atlas sprites --
  the first braces looked like they floated off the frame. And **a UV span lying wholly in the next
  cell must be shifted by whole blocks, never clamped**: clamping collapsed the span to a sliver
  that speckled under mipmapping (seen on the guardrails' wood). `gen_scaffold.py`'s `_box`/`_fit`
  do both, and `gen_formwork.py` and `gen_crane.py` reuse them. The offline previewer wraps UVs, so
  it cannot catch either; check in the dev client, near and far.
- **An iron block with no tool drops nothing.** A custom `Material(MapColor.IRON)` that does not
  require a tool keeps the scaffold breakable by hand. Forge's dig speed reads the material, not
  `canHarvestBlock`, so overriding the latter alone still digs slowly.

## Formwork, shoring and rebar

Seven blocks (`gen_formwork.py`). The wall form and the rebar bundle lie along one horizontal
axis (`AbstractBlockSiteAxial`); the wall form runs and stacks with no neighbour logic at all,
because every part of it is the full length and height of its cell. The post shore reads its
stack: a real shore is two or three blocks tall, so each block works out its part (the outer
tube where the stack carries on; the adjusting collar, inner tube and U-head where it ends; a base
plate at its foot) and nothing is stored. The column form, rebar mat, dowels and cage are one
class, `BlockSiteProp`, constructed by registry name -- a ThreadLocal hands the name to the constructor, as `BlockBrickTrim` does.

## Fencing

Six blocks, one class: `BlockSiteFence`, constructed by registry name, which joins its neighbours
the way a vanilla fence does -- a post at the centre of the block and a panel out to each side it
connects on. All state is actual state: the four sides, whether the same family continues above
and below, and whether the post is a terminal (an end, corner or junction) rather than a line post
in a straight run. Fences join their own family -- both temporary fences, both chain-link finishes
-- and the temporary and chain-link fences also butt up against a solid face.

| Block | Tab | What it is |
|---|---|---|
| Temporary Fence | Construction Site | mesh panels in tube frames, 1.75 blocks tall (a real panel's six feet), standing in precast feet, with a clamp coupling each pair of end tubes |
| Temporary Fence (Privacy Screen) | Construction Site | the same with green shade cloth behind the mesh |
| Silt Fence | Construction Site | black geotextile on wooden stakes, low along a site's edge |
| Chain-Link Fence, (Black) | Building Materials | permanent chain-link, one block tall and **stacking** |
| Chain-Link Barbed Wire Top | Building Materials | V arms and three strands a side, placed on top of a chain-link fence |

- **Chain-link stacks instead of coming in heights**, at the user's request: a tall fence is several
  one-block courses sharing one continuous post and mesh. The top rail and post cap are drawn only
  on the top course (nothing of the family above), the tension wire only on the bottom one, and
  the heavier terminal post with a dome cap wherever a run ends or turns; line posts get a loop
  cap the rail passes through.
- **The mesh is a zero-thickness cutout plane** through the post, from a 32 px diamond texture whose
  pitch divides the block, so mesh on neighbouring blocks and courses is one mesh. A mesh is a
  pattern of holes; a box would draw every wire twice. The temporary fence uses the same mesh.
- **A panel is drawn once**, running east from the post, and the multipart blockstate turns it onto
  the other three sides. The temporary fence's foot turns to lie along the run; a lone fence gets
  it along x.
- **Except anything that hangs on one side of the panel.** Turning the east half 180 degrees for the
  west half also moves an off-centre part to the other side, so the privacy screen came out in
  front of the mesh on one half of every panel and behind it on the other. The screen has an east
  and a west model instead, mirrored only in x, and a run along z turns both a quarter.
- **The barbed wire is a strip of texture**, not geometry: a plane two pixels tall sampling the rows
  of `barbed_wire.png` the wire is drawn on (explicit UVs), because a barb in elements is eight
  boxes and a fence has dozens of barbs.
- **The temporary fence's icon is scaled down** (`display.gui`): at 1.75 blocks, block/block's
  inventory view pushed it out of the top of the slot.
- Collision is a vanilla fence's 1.5 blocks for the temporary and chain-link fences, 0.75 for the
  silt fence and 1.0 for the barbed top; all of them are steel that comes down by hand, like the
  scaffold, except the silt fence, which is wood.

## Earthworks

- **Trench Plate** (`BlockTrenchPlate`): the steel road plate laid over an open trench, a sixteenth
  thick. Plates side by side draw as one: the raised edge bar, with the plate's side face under it,
  is drawn only along a side with no plate beside it (the four sides are actual state), and the
  plate body has only a top and an underside. Priced as 2 Sheet Metal.
- **Trench Box** (`BlockTrenchBox`, on `AbstractBlockSiteAxial`): two steel side panels held apart
  by an orange spreader pipe, set into a trench. It runs along the placer's line of sight, the way
  you look down a trench; boxes end to end and stacked are one box, with the top rail and lifting
  lugs only on the top course (`up`, actual state). Only the panels collide, so a player can walk
  the trench between them. Priced as 2 Sheet Metal + a Pole Section.
- **Soil, Gravel and Sand Stockpiles** (`BlockStockpile`, one class by registry name): eight layers
  like vanilla snow, stored as metadata. Using a stockpile on a pile that is not full adds a layer
  (and uses the item in survival); on a full one it places a new block above. A heap is shaped by
  hand, full in the middle and fewer layers toward the edges -- chosen over a fixed one-block
  mound. Breaking a pile drops one item per layer, and one item costs one of the vanilla material
  (dirt, gravel, sand). The textures are deliberately not vanilla's: excavated soil is darker and
  redder than dirt, the gravel is angular crushed stone, the sand warmer.

## Site logistics

Nine loads that make a site look occupied rather than staged, all `BlockSiteAxialProp`: one class
by registry name (like `BlockSiteProp`, whose ThreadLocal hand-off it repeats) on
`AbstractBlockSiteAxial`, laid along the placer's line of sight. Pallets of brick, concrete block,
drywall and bagged concrete, each a load on a pine pallet under two steel bands; a lumber stack on
dunnage, end grain on its ends; insulation rolls; a PVC pipe bundle and a conduit bundle; a wire
spool standing on its flanges.

- **Round things are a square plus the same square turned 45 degrees** (the rolls, the spool's
  flanges and drum): an eight-pointed section, which reads as round at any distance a pallet is
  seen from, where a true circle in elements costs a box per facet.
- **Bundles run the full length of the cell**, so two laid end to end are one long bundle.
- Each load is priced by what it is: brick is planks + 2 clay balls, concrete block planks +
  Concrete Mix, drywall planks + 2 paper, bagged concrete planks + 2 Concrete Mix, lumber 3 planks,
  insulation and PVC 2 paper + a dye, conduit a Pole Section, the spool planks + a Wiring Harness.

## Site facilities

### Built to size: containers, dumpsters, the job trailer

A shipping container is six blocks long, a forty-footer twelve, a roll-off dumpster anything from
four to eight; a fixed multi-block prop would need its own placement and multi-block breaking and
would still come in one size. So these are **built block by block** (`BlockSiteShell`, the user's
choice over a one-click prefab): each block draws a wall only on a side whose neighbour is not part
of the same object, a frame rail only along an edge where both sides are outside, a roof where
nothing of it is above and an underside where nothing is below. Any box of blocks reads as one
object, with no seams inside it.

- **Containers** come in blue, maroon, green and grey. Each block of one joins only its own colour,
  so two containers side by side or stacked stay two objects if they differ in colour; two of the
  same colour touching join into one, which is the price of building to size. A container has its
  doors on the side it faces -- toward the player who placed it, so place the whole container from
  its door end. The facing is the only thing stored.
- **Dumpsters** are containers with the top left open, at the user's request: the walls are drawn
  as solid plates, painted outside and scuffed inside, with a lip along the top edge; only the walls
  and floor collide, so a dumpster can be stood in and filled.
- **The job trailer** is three blocks that all join one trailer: plain wall, window and door, so the
  windows and door go where they are placed. A door block with a door block under it draws the
  door's upper half (the light); on its own it draws the lower half (the handle).
- Walls are zero-thickness planes half a pixel in from the cell face, so the rails stand proud of
  them, and every face takes positional UVs, so the corrugation or siding runs unbroken across
  every block of a wall.
- **No texture detail that repeats.** A rust fleck drawn into a wall texture repeats on every
  block of a twelve-block wall and reads as polka dots; the shell walls have none.
- Priced per block, as they are built: container and dumpster 2 Sheet Metal, trailer Sheet Metal +
  planks.

### Props with a front

`BlockSiteFacingProp`, one class by registry name, facing the player who places it: the portable
toilet (two blocks tall, blue moulded walls, the door with its vent and vacancy indicator, a
translucent white roof; its icon is scaled down like the temporary fence's), the gang box (a
safety-yellow job box with a diamond-plate lid), and the concrete washout (a lined steel pan of
settled slurry with its sign at the front, the legend drawn as its layout since a legend is not
legible at sixteen pixels).

## Tower crane

A mast you build to the world's height, in a 1x1 or 2x2 section, and a head on top that draws the
whole slewing unit -- **flat-top**, **hammerhead** or **luffing jib** -- in **yellow**, **red** or
**white**. The crane is configured in a screen and grows by climbing: mast sections are inserted
under the head, the way a real crane is jacked.

### The mast

`crane_mast` (1x1) and `crane_mast_large` (2x2), each in three liveries stored in metadata, one
creative stack per livery (`ItemBlockCraneLivery`, which names each with
`tile.<name>.<livery>.name`). A base section with anchor feet is drawn where the block below is not
mast. The mast is climbable with a ladder up the inside of its north face; a player slides back
down when jump is released (the vanilla ladder rule), and sneaking holds on. At the top it comes
out onto the slewing deck (see "Standing on the crane").

- **Lacing is a cutout texture on a plane per face; the chords are geometry.** A 1x1 face's
  chord-to-chord diagonal is not an angle a JSON element can be turned to (22.5 degree steps). A
  texture diagonal can be any angle and meets the chord exactly at the cell edge.
- **Each 2x2 block is one quarter, worked out from its mast neighbours** (actual state; nothing is
  stored but livery). One NW model is turned for the other three. The X panel on each face is two
  blocks high, so which half a block draws is chosen by its absolute y parity -- stacked sections
  line up whatever height the mast was started at.
- Masts are baked block models, the cheapest thing to draw, because a mast is hundreds of blocks: a
  1x1 to y 255 rendered at 926 fps. From 150 blocks it is a thin line, dotted where sub-pixel chords
  alias, as a vanilla fence does.

### The head

`crane_head` goes only on a mast top. On placement it reads the mast under it -- 1x1 or 2x2, the
livery, and on a 2x2 where the section's centre is from the quarter it was placed on -- so the
slewing unit is centred on the mast whichever quarter the head went on. The block draws nothing
in the world; everything above the mast comes from `TileEntityCraneHeadRenderer`.

**Why a renderer and not jib blocks.** Only a renderer can slew, move the trolley, and rake a
luffing jib; a jib built of blocks would be static, and a raked one would be a job the size of the
mast arm curves. The cost is that nothing the renderer draws has collision of its own -- see
"Standing on the crane" below for how it gets some.

**Why Java geometry and not a baked model.** Jib length is a screen value; a baked model per length,
per model, per livery is thousands of files. `CraneGeometry` builds the unit at run time: bars
between any two points (wound so the outside faces out, shaded as vanilla shades block faces),
axis boxes, and triangular lattice girders -- two bottom chords and a top chord braced on all three
faces, the section almost every tower crane jib actually has. Everything is a sprite from the block
atlas, so the whole crane is one texture.

| Model | What it adds |
|---|---|
| Flat-top | jib, counter-jib a third of its length with winch and ballast, cab, trolley and hook block |
| Hammerhead | the flat-top plus an A-frame tower head with pendant lines to both jibs |
| Luffing jib | a jib pinned at the slewing unit and raked up at the luff angle, held by ropes from a short A-frame; the hook hangs from the tip, as a luffing crane has no trolley |

**Aviation lights.** Red obstruction lights sit at the jib tip, the counter-jib's end and the
hammerhead's apex. They flash like an FAA L-864: thirty a minute, a 90 ms rise, a 700 ms hold and a
fade, dark for the rest of the two-second cycle. Every light on one crane flashes together; each
crane takes its own phase from its position, so a skyline of cranes shares the rate but not the
step (real lights only have to share the rate). The lens housing is geometry in the display list;
the flash cannot be, since it changes every frame, so `CraneGeometry` records each lens centre and
the renderer draws, outside the list, the lens again full-bright and two camera-facing halos from
`crane_glow.png` (drawn by `gen_crane.py`: a hot core on a long soft tail that reaches zero before
the quad's edge), added onto what is behind them with depth writes off. The halos grow a little
with distance -- a lens the size of a block is under a pixel from across a city, and a real light
reads as a point of light at any range. By day the halos fade by the sun's brightness times the
sky the head can see -- the tight one to 40%, the wide one almost away -- since a real light in
daylight is a bright lens, not a glow; at night, in rain or under cover they are at full. They
are not gated on the strobe-effect config: thirty
slow red flashes a minute are not a strobe.

**Configuration** (`TileEntityCraneHead`, NBT short keys `m l j s t h f g cx cz`):

| Field | Range |
|---|---|
| Model, livery | the three of each |
| Jib length | 8-40 blocks on a 1x1, doubled on a 2x2 |
| Slew | 0-359 degrees |
| Trolley | 0-100% along the jib (not the luffing jib) |
| Hook drop | 1-250 blocks below the jib |
| Luff angle | 15-80 degrees (luffing jib only) |

A 2x2 scales everything by two, not just the jib: a wider lattice and a bigger slewing unit.

### Rendering

A crane is thousands of quads and changes almost never, so each head compiles its geometry once
into a display list, rebuilt only when `renderKey()` (the configuration) or the light at the head
changes. **The slew is a rotation applied outside the list**, so turning the crane never rebuilds
it. The rules are "Display lists: one texture, no cached state" in `TRAFFIC_SIGNAL_SYSTEM.md`: the
block atlas is bound outside the list every frame, nothing inside touches cached GL state, and the
light is in the key because it is baked into the vertices. `invalidate` and `onChunkUnload` release
the list.

- **Render box** is a square the jib's reach on every side (it can slew any way), from the hook's
  lowest drop to above the tallest model. Never `INFINITE_EXTENT_AABB`, which switches frustum
  culling off.
- **Render distance** is `(LONG_RANGE_RENDER_DISTANCE + reach)^2`, as the span wire does: measured
  from the head, a fixed 128 would drop the far end of a long jib while it was still on screen.
- Measured: three cranes of different models in view at 1,147 fps, each whole from 150 blocks. The
  renderer has not shown up as a cost worth a performance note.

### Standing on the crane

A player climbs the mast and comes out onto the slewing deck, then can walk the counter-jib (up
onto the winch and over the ballast), stand on the cab roof, and walk the jib's bottom chords out
to the tip. None of that is a block: `CraneCollision` adds it from `GetCollisionBoxesEvent`, on
both sides, for every loaded crane whose reach the queried box is inside (heads register
themselves in `validate` and leave in `invalidate`/`onChunkUnload`). A block's own collision could
not do it -- 1.12 only asks blocks within a cell of an entity, and the jib is up to eighty blocks
long -- so the head block has none at all.

- **The parts follow the slew.** At a multiple of 90 degrees each part is one exact box; at any
  other slew it is filled with small squares (0.2 blocks at scale 1, each big enough to cover its
  own cell at any angle), made only near the queried box, so a long jib costs nothing extra.
- **The sizes mirror `CraneGeometry`.** Change a part's size there and change it here.
- **The deck is solid from above and not to a sneaking entity**, like the scaffold deck: sneak on
  it to drop back into the mast and climb down. Everything else is solid from every side, which is
  what lets a player step up from the deck onto a walkway.
- **Nothing reaches into the mast's column**, so no part catches a climber's head.
- **Climbing out.** The head used to be a ladder with a one-block platform. On a 2x2 a player who climbed a
  quarter the head is not on came out into air, dropped back into the mast and bobbed there for
  ever. Now the climb handler keeps a player climbing while their feet are in the cell above a
  mast top at the head's height, until they are over the deck (`CraneCollision.isClimbingOut`).
- The luffing jib is raked too steeply to walk, so it has no walkway; its deck, cab and
  counter-jib do.
- **Two cranes too close together collide with each other's parts**, as they would for real:
  when testing, a neighbour's counter-jib over a mast top stops a climber below the deck, which
  looks exactly like the climb being broken.

### The screen

Right-click any block of a crane with an empty hand -- including the foot of its mast, since the
head can be two hundred blocks up. `CraneLocator` walks up the mast to the head (on a 2x2 it checks
all four quarters of the top section). GUI id 25, provided by `BuildingGuiProvider`.

Model and livery are buttons that cycle; the rest are sliders. **Every change previews on the crane
as it is made, on this client only**; Done sends `CraneHeadConfigPacket`, Cancel or Escape puts the
crane back. The packet follows the conventions in `PERFORMANCE_AND_SECURITY.md`: fixed size,
handled in `addScheduledTask`, `canPlayerReach` on the clicked block first, non-finite floats
rejected, every value clamped by `setConfiguration`, which then syncs to everyone. The packet names
the *clicked* block, not the head, because reach is checked against what the player can touch.

### Climbing

Use a mast item on any block of a crane whose mast is the same size, not sneaking (`CraneClimber`):

1. the head moves up one block, its tile entity NBT with it;
2. a section goes in where it was -- on a 2x2 the whole layer, the head's quarter and the three
   beside it, which takes four pieces;
3. the new section is the livery of the pieces used, so a mast can be built in two colours.

Refused, with a status-bar message, at the build limit, when anything is in the way, or without
enough pieces. Creative does not consume. Sneaking places the item as an ordinary block, so a mast
can still be built beside a crane, and a mast can be stacked by hand before the head goes on.

### Cost

The Fabricator prices by the display name. Masts hit the pole rule first ("mast": 2 pole sections);
the head hits the construction-site "crane" rule (4 pole sections, a control board and a wiring
harness -- the jib's steel and the cab's controls). `audit_fabricator_costs.py --grep crane` shows
both.

### Not done, and why

| Item | Why |
|---|---|
| Idle slewing / trolley animation | Changes the caching from per-configuration geometry to a per-frame transform; static first |
| A hook that lifts blocks or entities | A different feature: entities, physics, permissions |
| Riding in the cab | Needs a seat entity and camera work |
| A dark grey livery | The user chose yellow, red and white; one more texture set |
| Mobile and crawler cranes | A different machine |

### Release check

`check_reobf_refs.py` reports `BlockFaceShape.SOLID`, `EnumBlockRenderType.ENTITYBLOCK_ANIMATED`
and `NonNullList.add` as plain-named references new since 2026.09.17. All three are false positives:
the SRG mappings keep their names unchanged (enum constants, and a method Minecraft never
obfuscated).
