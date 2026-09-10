# Work Zone Accessories

The channelizing devices a road crew puts out: cones, drums, channelizers, barricades, delineator
posts, temporary pavement markers, barrier walls, sand barrels and the arrow board. Thirty-seven blocks in the Traffic Accessories tab, all
generated from one script, and all sharing one behaviour that nothing else in the mod has — they
**settle onto the surface underneath them** instead of floating a cell above it.

This document covers that settling mechanism, the barricade connection and customization systems,
and the arrow board's animated display. It does not cover the traffic signs that can be mounted
on a barricade; those are `assets/docs/TRAFFIC_SIGNS.md`.

## What is in game

| Block | Class | Notes |
|---|---|---|
| `traffic_cone`, `traffic_cone_lime` | `BlockWorkZoneDevice` | |
| `traffic_cone_knocked` | `BlockWorkZoneDeviceRotatable` | the same cone laid on its side, not a second model |
| `traffic_drum` | `BlockWorkZoneDeviceFlashing` | carries a flashing warning light |
| `traffic_drum_unlit` | `BlockWorkZoneDevice` | |
| `channelizer_tube`, `channelizer_tube_lime` | `BlockWorkZoneDevice` | the tall slim tube |
| `channelizer_cade_left`, `_right` | `BlockWorkZoneDeviceRotatable` | tube with a small striped panel |
| `barricade_type_1_left`, `_right` | `BlockWorkZoneBarricade` | one rail, joins into runs |
| `barricade_type_2_left`, `_right` | `BlockWorkZoneBarricadeFolding` | two rails on a folding A-frame, stands alone |
| `barricade_type_3_left`, `_right` | `BlockWorkZoneBarricade` | three rails, joins into runs |
| `delineator_post`, `delineator_post_yellow` | `BlockWorkZoneDevice` | |
| `delineator_zebra` | `BlockWorkZoneDeviceDiagonal` | the low rubber lane separator; runs the length of its cell so a line of them is continuous, and takes all eight facings |
| `pavement_marker_white`, `_yellow`, `_orange`, `_red`, `_blue`, `_green` | `BlockWorkZoneDeviceDiagonal` | the small folded tabs taped down a lane line |
| `channelizing_wall_orange`, `_white` | `BlockWorkZoneDeviceRotatable` | the plastic wall filled with water on site |
| `concrete_barrier` | `BlockWorkZoneDeviceRotatable` | precast, in the New Jersey profile |
| `road_plate` | `BlockWorkZoneDevice` | steel plate over an open trench |
| `safety_fence` | `BlockWorkZoneDeviceRotatable` | orange mesh fence closing off the work area |
| `vertical_panel_left`, `_right` | `BlockWorkZoneDeviceRotatable` | narrow striped panel for places too tight for a barricade |
| `portable_signal_trailer_arm`, `portable_signal_trailer`, `portable_ped_signal_trailer` | `BlockWorkZoneDeviceRotatable` | towed signal; carries no heads of its own |
| `sand_barrel_array` | `BlockWorkZoneDevice` | |
| `arrow_board` | `BlockWorkZoneArrowBoard` | trailer board with seven animated modes |

Left and right variants differ only in which way the stripes slope — toward the side traffic
should pass.

## Settling onto the road below

A road that climbs is built from blocks whose top face sits partway up their cell. A device
placed on such a road goes in the cell **above** it, and without help stands in the air by
whatever the road did not fill. Every block here corrects for that.

`codeutils/RoadSurfaceHeight` decides the correction, and it does so **without knowing or asking
which mod supplied the surface**. It reads the top of the bounding box the block below already
reports through the vanilla block API. The same code therefore lands a cone correctly on a road
that steps in sixteenths, on a vanilla bottom slab and on any of the eight snow layer depths,
depending on nothing.

The rules, in the order they are tested:

| Below the device is… | Offset |
|---|---|
| air | none — nothing to stand on |
| another settling device (`ICsmRoadSurfaceAware`) | none — devices sit on each other as placed, and this cannot recurse down a stack |
| something without a full cell footprint (a torch, a fence post, a degenerate empty box) | none — settling onto a fence post's top face would leave the device mid-air |
| a block filling or overfilling its cell | none |
| anything else | the top of its box |

A flat road marking is itself drawn a cell high and pulled down onto the road under it, so its top
face can sit *below* its own cell. That is why the last rule takes the box top rather than
clamping at zero: a device settles onto whatever is actually there, standing on the marking rather
than through it, at every road height alike. `MAX_DROP` is two blocks — one for the road, one for
a marking over it — and anything asking for more is left where the player put it, because dropping
a device two metres through the world is worse than a device that floats.

### Three consumers must agree

This is the part that is easy to get wrong. A settling block has to apply the same offset in
three places:

1. **`getOffset`** — the render offset. `BlockModelRenderer` reads `IBlockState#getOffset`
   unconditionally, so this moves the baked model.
2. **`getBoundingBox`** — which in this codebase feeds both selection and collision, funnelled
   through `AbstractBlock`.
3. **Any tile entity renderer.** A TESR draws in world space and knows nothing about
   `getOffset`. Leave it out and the lit part of a device sits at a different height from the
   device.

Miss the second and the block's hitbox floats above its model. Miss the third and a barricade's
warning lights hover over a barricade that has settled beneath them.

The three trailer-mounted devices in this tab settle too, and did not to begin with: the
portable changeable message sign, the portable variable speed limit sign and the radar speed
feedback sign predate this mechanism and were left at the block grid, so an arrow board sat
flush on a sloped road while the trailer parked beside it floated. They use the same base
class now, and their renderers apply the offset by hand for the reason above.

`RoadSurfaceHeightTest` covers the decision as two pure functions — `surfaceFromBox` and
`offsetForSurface` — precisely so the rules can be tested without a block registry, which this
project has no mocking framework for.

### Runs on a slope

Each block settles independently, so a run of connected barricades crossing a height change
steps rather than ramping. One block of height change per barricade reads correctly; a steeper
change would not. This is by design and has been checked in game.

## Barricades

### Runs

Type I and Type III barricades join onto the ones beside them, so a line reads as one long run
rather than a row of separate panels. Connection is resolved in `getActualState` from the
neighbours rather than stored, so breaking a barricade out of the middle of a run closes the two
halves up without anything having to be notified.

A barricade joins only along its own length, only to a barricade of the same kind, and only to
one facing the same way — the striping slopes toward the side traffic should pass, so a keep-left
joined to a keep-right would contradict itself mid-run.

The model is a **core plus two detachable ends**. The core is what every barricade draws: the
rails, and the upright on its **left-hand** edge. An end adds the overhang past the upright, the
cap over the rail ends, and on the right-hand side the upright itself; it is drawn only where
nothing connects.

That asymmetry — left upright always, right upright only when free — is what leaves exactly one
upright on each joint. Drawing both and hiding both at a seam would leave the joint with no leg;
drawing both and hiding neither would leave two back to back.

Three geometric facts make a run read as continuous:

- **The rails span exactly one cell.** Overhanging rails would overlap a neighbour's and z-fight,
  and the striped panel's UVs cannot continue past the edge of their sprite to cover an overhang
  anyway. It also means a run of five barricades is five blocks long, which is what anyone laying
  one out expects.
- **The legs are centred on the cell edges**, not inside them, so the shared upright at a seam
  sits *on* the seam rather than beside it.
- **The stripe pitch is the rail width over four**, putting exactly two full light-dark periods
  across a cell. That gives the pattern the same phase at both rail edges, so the striping
  carries straight across a seam instead of jumping — and it is also what lets a free end's
  overhang continue the pattern by wrapping round to the far edge of the same sprite.

### The Type II folding barricade

The plastic A-frame that folds flat: two striped rails on two uprights, with legs hinged near the
top that swing out behind it to stand it up.

It deliberately does **not** join. A folding barricade is a self-contained unit that is carried
in, opened and set down, so a row of them is a row of separate devices each on its own legs
rather than one continuous rail. Joining them would also have to decide what happens to the legs
at a seam, and there is no good answer. So it is narrower than a cell and is one baked shape with
no ends to add or take away.

Because its legs trail off behind it, the assembly is centred in its cell **as a whole**, which
leaves its panel forward of the block's axis. That is why the renderer asks the block where its
rails are rather than assuming the axis — see below.

### Signs and warning lights

Right-clicking any barricade:

| With… | Does |
|---|---|
| a sign in hand | mounts that sign on its face |
| an empty hand | cycles the warning lights: none → left only → right only → both ends |
| sneak | takes the mounted sign off and hands it back |

**Any sign in the mod can be mounted**, with no curated list. `TileEntityBarricade` stores the
sign's *registry name*, not an index, and `TileEntityBarricadeRenderer` reads the panel's shape
and sprite straight off that sign block's own baked model. A sign added to the mod later works
the day it is added, and a sign removed leaves a bare barricade rather than one pointing at
missing art. A stack counts as a sign if its block's class sits in the `trafficsigns` package.

Rendering the sign's baked model outright would be simpler and is wrong: sign models carry their
own mounting post, which would arrive bolted through the barricade.

Three things about reading a baked model that this got wrong first:

- **`getQuads` files faces by cull side.** A face lying exactly on the block's north boundary
  goes under `NORTH` so it can be culled; a face a hair in front of it goes under `null`. Sign
  models put a blank backing board *on* the boundary and the legend just ahead of it, so reading
  only the first non-empty list picks the blank board every time. Both lists are read, and among
  equal-sized panels the frontmost wins.
- **A north-facing quad runs its `u` opposite to `+x`.** Standing in front of a sign, the model's
  `+x` is on your left. Taking `u` along `+x` draws the legend mirrored.
- **Signs are not all square.** Across the 91 sign models there are 22 distinct panel sizes with
  aspects from 0.20 to 15.5. The panel is fitted to the mount box on whichever axis runs out
  first, never stretched to fill it.

Warning lights reuse the drums' flash cadence, each barricade starting at a random point in its
own cycle so a run of them drifts apart the way real ones do. The offset is deliberately not
persisted.

### What the renderer asks the block

`AbstractBlockWorkZoneBarricade` holds everything about what a barricade *carries*; the subclass
says what it *is*. The renderer asks the block four questions rather than assuming the trestle
numbers:

| Method | Why it varies |
|---|---|
| `getTopY()` | Type I, II and III are three different heights |
| `getLeftUprightX()`, `getRightUprightX()` | the folding one is narrower than a cell |
| `getRailHalfZ()` | rail thickness differs |
| `getRailCentreZ()` | the folding one's panel is forward of the block axis |

The sign size caps are fractions of the barricade's own frame for the same reason, so a smaller
barricade gets a proportionately smaller sign rather than one hanging off its ends.

`BarricadeGeometry` is **generated** alongside the models, so the renderer and the baked geometry
cannot disagree about where the uprights and rails are.

### Why some devices get eight facings

The zebra delineator and the pavement markers do; nothing else here does. Four facings are enough for anything that stands up and
faces traffic, and not enough for something laid ALONG a line: a lane edge, a taper into a work
zone or a bike lane running off the grid all need the in-between angles, and a line of long
devices that can only lie north-south or east-west has to staircase across a diagonal instead of
following it.

That applies to a dotted line of markers as much as to a continuous line of delineators, and
arguably more: a staircase is more obvious on something small and repeated than on
something long enough to read as a line on its own.

The in-between facings are not quarter turns, so they cannot use a blockstate variant's own
`y` shorthand -- that only takes right angles. They use an explicit `transform` rotation, which
the OBJ loader accepts at any angle and which the eight-way blocks already in this tab use.

### The temporary pavement markers

A flat foot taped to the road, a panel standing up off the back of it, and a beaded reflective
strip along the panel's top edge under a moulded lip. White, yellow, MUTCD/ADA blue and the FHWA
green a bike lane is surfaced in, plus construction orange and stop sign red.

Unlike the zebra delineator these do **not** span their cell. Real ones are set out at intervals
with clear road between them, so one small marker per block already gives a line the spacing it
is supposed to have; stretching them to touch would turn a dotted line into a solid one.

Foot, panel and lip each step in a little from the one behind and each finishes *inside* it.
Three pieces the same width would put three pairs of faces in the same two planes, which
z-fights along both ends of the marker.

Their texture is deliberately not `band_image`: that shades across u with a sine, which reads as
the round side of a lathed cone or drum and as a vignette on something flat. A marker is a
moulded plastic tab, so its body is left flat and only the strip carries any texture -- sparse
bright pixels, which is what separates a retroreflective strip from a painted stripe at
distance.

Their reflective strip is **emissive under OptiFine**, which is what makes it read as
retroreflective rather than painted -- a marker that goes dark at night is one that is not doing
its job, and these are put out precisely for the nights between milling a road and re-striping
it. Each marker texture has a `_e` companion holding the strip alone on transparency, and
Core's `assets/minecraft/optifine/emissive.properties` declares the `_e` suffix. Without
OptiFine nothing references those files, nothing is stitched, and the marker draws normally.

**Do not do this with `getPackedLightmapCoords`.** Making the block report full brightness is
not confined to the block: a neighbour's renderer asks the block across each face for its packed
light to shade that face's own vertices, so a fullbright marker visibly brightens the road it is
standing on. There is no way to distinguish drawing yourself from a neighbour sampling you. That
was tried first and had to come out.

The markers also carry a finer texture than the rest of the family, 128px against 32. Their
strip is a little over half a world unit tall, which at the shared size is barely one texel row
-- thin enough that the strip all but disappears and its emissive companion has nothing to
cover.

### The zebra delineator

The low rubber lane separator laid nose to tail along a bike lane edge. It is the odd one out
here in two ways.

It is built as a run of arched cross-sections rather than as a lathe, because its section
changes along its body: a lathe gives a shape of revolution, and this is wide in the middle and
pointed at both ends. Each face's outward direction is taken from the body's own centre line
rather than named, since every face leans a different way.

And its texture is mapped the other way round from every other device here: **v runs along the
body rather than up it**, and u runs around the arch. That is what lets `band_image` -- written
for the collars on a cone -- draw bands *across* this one, and it also puts that function's
one-sided lighting over the crown, where the highlight belongs.

The moulded dimples are not decoration. Without them the body is a flat black shape whose only
cue to its curve is that lighting, and it reads as a painted marking rather than as something
standing off the road.

## The wall devices

The water-filled plastic barrier and the precast concrete barrier are the same shape problem:
a wall of constant cross-section running the length of a cell. `swept_wall` takes the profile up
one side and mirrors it, mapping the two long faces by HEIGHT so a texture drawn the way
`band_image` draws one puts its bands across the wall at the heights it names.

Both span their cell **exactly** and join their neighbours, drawn as a core plus two end caps
with the cap left off wherever a wall of the same kind stands facing the same way. Without
that, two segments meeting on the boundary put their end faces in one plane and z-fight along
the whole joint.

Leaving a hair of gap and always capping was tried first. It avoids the z-fight for nothing,
and real walls of both kinds do show a joint line every segment — but a run then reads as a
line of separate blocks rather than as a wall, which is the one thing a wall has to do.

The plastic wall's moulded ribs are drawn into the texture rather than modelled. A rib deep
enough to catch the light is a rib deep enough to z-fight against the face it stands on, and at
this size it would be two texels wide regardless.

Both of those, and the barricades, resolve their connections through `WorkZoneJoins`, which
holds the one rule for what counts as a join: the same block, and — for anything that faces a
way — facing the same way. A run that changes kind or direction partway is two runs.

## The road plate and the safety fence

The road plate is the one device here meant to be driven **over** rather than steered around,
so it spans its cell exactly and sits barely off the road. Its tread face is also the one face
in this family mapped in two directions at once rather than by height — a diamond tread runs
both ways.

It joins on all **four** sides rather than two, because plates are laid in a patch rather than
a line and the block has no facing at all — "left" and "right" mean nothing to it. The piece
that comes and goes is the rim on each side: inside a patch a rim is not there to be seen, and
two rims in one plane z-fight anyway.

Its colour is deliberately dark. A plate goes down weathered and comes up weathered, so what
is on the road is nearly black, with the tread polished smooth by traffic and rust blooming
from the edges. Bright steel with a crisp tread is what a plate looks like in a catalogue and
not what one looks like in a street.

The safety fence's mesh is **cut out of the texture**, not modelled: a mesh built from geometry
is hundreds of faces for something read at two texels. The holes are real transparency, which is
why the block draws on the cutout layer — a fence you cannot see through is a wall. Its panel
spans the whole cell so a run is continuous, and its stakes are inset from the edges rather than
sitting on them, so a joint shows the pair of stakes a real run has instead of one shared post.

## The portable signal trailers

Three styles: a mast arm, a plain mast, and a lower one for a pedestrian head.

They carry **no signal heads of their own**. The mod already has every signal anyone could want
and a controller system to drive them, and a head placed beside anything draws its own mounting
hardware, so the trailer's job is to be the thing they mount ON. What it has to get right is
therefore not the signals but the CELLS left free for them. Relative to the trailer's own cell
and its facing:

| Cell | Head | Mount type |
|---|---|---|
| three to eight cells out, under the level run | the overhead head, hanging (arm style only) | Overhead Mount |
| three to eight cells out, IN the level run | a Dynamic Signal Mount Kit clamped to the boom | — |
| the cell beyond that kit | the overhead head, mounted beside the boom | None |
| either cell to the SIDE, five up | the near-side head, on the mast | Rear Mount |

The near head goes **beside** the trailer rather than out along the boom because it faces ACROSS
the boom: the boom reaches over the road and the head looks back up the lane at the driver, so
the mast ends up squarely behind the head, and a head with its pole directly behind it is exactly
what Rear Mount describes. It also sits well below the overhead one, which is how these are
rigged -- the mast head is read from the stop line and the boom head from back down the lane.

**Which cell out is the wrong question for the head's height.** A signal body is not one block
tall: a three-section head reaches 1.5 blocks above its own block and a single-section one 0.75,
so a three-section head hangs six up where a single-section one hangs seven. Nothing has to know
that, because Overhead Mount measures the body it is on -- see below.

### The boom

Not a straight bar off the top of the mast. On every real one the boom **pivots** near the top of
the mast, rises to its working height over the first two cells, and then runs level out to the
tip. The first version here was one straight bar at full height with a knee brace from half way
down the mast, which is a gantry, not a towed machine.

The level run is also what the block grid wants. It is the part heads hang from, and it has to be
at ONE height across several cells or there is no row of cells to hang them in. So the rise takes
cells one and two and the level run cells three to eight, and every hanging cell has the boom's
underside at exactly eight blocks.

The boom is **eight cells long** -- nearly as long as the whole machine is tall, which is what
lets one trailer signal the far lane of a road it is parked beside. It tapers across three
stations and is held the way the references are rigged: a **king post** at the bend with a tie rod
out along the level run, and a **ram** close under the pivot. The mast stops just above the pivot
and carries none of the outer end's load, which is why on the real thing it looks so much shorter
than the boom is high.

The tie is a **rod**, not a wire. The first version drew it 1.2 units across, which at eight
cells' range is one pixel and reads as a cable someone strung between two points rather than as
the thing holding the boom up.

### Overhead Mount

A head hangs from the boom with the signal system's own **Overhead Mount**, set on the head with
the Signal Head Configuration Tool. That mount type was added for this: the others all reach
sideways or backwards to a pole beside the head, and none of them can describe a head that HANGS.

It is not a bracket, and the first version's mistake was trying to make it one. The other types
draw a stub, an elbow and an arm aimed at the centre of the **neighbouring block**, because that
is where the pole they reach for stands. There is no pole in the neighbouring block here -- the
boom is directly above, and how far above depends on how tall the body is. Aiming at the
neighbouring block's centre put the elbow ABOVE the target on any head of three sections or more,
so the arm came back down and forward, and the head hung under the boom with nothing joining the
two.

So the hanger is its own two pieces: a **cap** across the top of the housing, running forward from
the housing's rear shell to the block's centre axis, and a **post** rising from the cap. The post
reaches for the first **block boundary above the body**, whatever the section count, and a boom's
underside always sits on a block boundary -- so it lands on it for a one-section head and a
five-section head alike. The post also stands on the block's centre axis, which is the axis the
body tilt turns about, so an angle-mounted head keeps its post upright and under the boom instead
of swinging it out from under.

It works on CSM's own mast arm curves too, not just here.

### Mounting beside the boom instead of under it

A head does not have to hang. Put a **Dynamic Signal Mount Kit** in one of the boom's own level-run
cells and it clamps to the boom rather than to a pole, and a head in the next cell out then mounts
to it the traditional way, sitting beside the boom with its top about level with the bar. Give the
head **None** for its own mount type — the kit is the hardware, and a head drawing its own bracket
as well would double up.

The kit has a **Highway Orange** finish in its colour cycle for exactly this: a bracket bolted to a
trailer's boom is painted with the machine.

How the kit finds the boom, and why it has to be told rather than look, is in
`assets/docs/TRAFFIC_SIGNAL_SYSTEM.md` under "Clamped to a portable signal trailer's boom".

### The solar array

Tilted, and the one face on any device in this tab that carries a picture rather than a flat
colour. Both for the same reason: a swatch of dark blue lying flat on the deck reads as a
tarpaulin, and the whole point of a solar panel is that it is pointed at something. It reuses the
cell texture the mini solar panel block already ships, so the two match wherever they stand
together.

**The scale is set by the signals, not by the trailer.** A signal head here is a block, and a mast
arm intersection is built with its arm around ten blocks over the road; a temporary signal sits
lower but not much, or traffic does not fit under it. So the level run's underside is eight blocks
up and everything else follows.

**The chassis is then sized against the mast and boom, not against a real trailer.** Measured off
a real trailer's own dimensions it comes out a toy: a deck two cells long under a boom eight cells
long reads as a mast someone left standing on a go-kart, which is how the first version came out.
On the reference machines the deck is around a third of the overall height, so the deck here is
nearly three cells long, with wheels and jacks big enough to see from across the road.

Their bounding box is the **trailer only**, like the arrow board's. A box tall enough to hold the
mast is also a collision box tall enough to wall the road off, and one long enough to hold the
boom would have the player selecting the trailer from half way across the road.

That size makes the item icon a separate problem, the same one the arrow board has: given a model
this much bigger than a cube, `forge:default-block` draws it over the slots around it. So each
style has its own icon model, shrunk to the slot. The arm style's carries a **shortened** boom and
none of the rigging -- a slot is sixteen pixels, and a boom drawn its true eight cells long shrinks
the trailer under it to nothing, while a tie rod an eighth of a pixel across is not there at all.

## The arrow board

Right-click cycles its mode; sneak steps backwards, because with seven modes one-way cycling
leaves the mode just behind the current one six clicks away.

| Mode | Stage |
|---|---|
| Sequential Chevron (Right / Left) | 420 ms |
| Sequential Arrow (Right / Left) | 380 ms |
| Flashing Arrow (Right / Left) | 700 ms |
| Flashing Caution | 700 ms |

`CsmConfig.arrowBoardSpeedPercent` (10–400, default 100) scales every stage.

The lamp grid is 7 by 5 — the 25-lamp board — rather than the 15-lamp one drawn first. Seven
columns is what lets a chevron actually converge to a point, and five rows is what lets it have a
diagonal rather than a single barb; a 5 by 3 grid can only draw a shaft with a bump on it. Right
patterns are written out and mirrored for the left ones, so the pair cannot drift apart.

Two things learned from watching it run:

- A chevron **chase** lights each segment alone in turn, not cumulatively. Accumulating segments
  just draws a growing arrow.
- Chevrons at adjacent columns merge into one wedge, so they are spaced two columns apart.

### Why the board is split between a model and a renderer

Only the **chassis** — trailer, tongue, wheels and jacks — is a baked model. The mast, panel and
lamp grid are drawn by `TileEntityArrowBoardRenderer`.

The lamps have to animate and a baked model cannot. That is the real reason; culling is a
secondary benefit. The board is drawn several blocks wide, and a static model that leaves its
cell is drawn as part of its chunk section and pops when that section is culled. Splitting it
leaves the static part small enough to sit inside its own cell, and the tall part is now tile
entity geometry, culled by its own render bounding box.

The board's size is set by the company it keeps, not by the block grid: the portable message sign
and portable speed limit sign beside it in this tab are drawn four blocks and more by their own
renderers. An arrow board built to fit one cell stands next to them looking like a toy, which is
exactly how the first version came out.

That size makes the item icon a separate problem — `forge:default-block` assumes what it is
handed fits a unit cube and silently draws anything larger over the slots around it. So the icon
is its own model (`workzone_arrow_board_inv.obj`), sized to the slot, and it carries the mast and
panel the placed block leaves to the renderer, because a picture of the bare trailer does not
read as an arrow board. Its panel is plain dark: an icon is one still frame, and a frozen lamp
pattern would suggest the board only ever shows that one.

## Files

| Path | Role |
|---|---|
| `dev-env-utils/scripts/gen_work_zone_devices.py` | generates every model, texture and blockstate below, plus two Java constants files |
| `src/main/java/…/codeutils/RoadSurfaceHeight.java` | the settle decision, in Core so any module can use it |
| `…/codeutils/ICsmRoadSurfaceAware.java` | marks a block as settling; also what stops devices stacking their offsets |
| `…/codeutils/AbstractBlockRoadSurface.java`, `…RotatableNSEW.java`, `…RotatableHZEight.java` | the three base classes that apply the offset to render and bounding box |
| `src/test/…/RoadSurfaceHeightTest.java` | 15 cases over the pure decision functions |
| `modules/roads/…/trafficaccessories/BlockWorkZoneDevice.java`, `…Rotatable.java`, `…Flashing.java`, `…Diagonal.java` | the plain devices, constructed per registry name from the tab |
| `…/AbstractBlockWorkZoneBarricade.java` | what a barricade carries: signs, warning lights, and the shape questions |
| `…/BlockWorkZoneBarricade.java` | the joining trestle barricades |
| `…/BlockWorkZoneBarricadeFolding.java` | the Type II |
| `…/BarricadeFlashers.java` | none / left / right / both |
| `…/TileEntityBarricade.java`, `…Renderer.java` | what one is carrying, and drawing it |
| `…/BarricadeGeometry.java` | **generated** — where a barricade's parts are |
| `…/BlockWorkZoneArrowBoard.java`, `TileEntityArrowBoard.java`, `…Renderer.java` | the board |
| `…/ArrowBoardPattern.java`, `ArrowBoardPatterns.java` | the seven modes and the lamp sequences |
| `…/ArrowBoardGeometry.java` | **generated** — the numbers the renderer draws the board from |
| `models/block/trafficaccessories/shared_models/workzone_*.obj` / `.mtl` | the models |
| `textures/blocks/trafficaccessories/workzone/` | the textures |

## Working on this

Run the generator after changing any of its constants, and audit what comes out:

```bash
python dev-env-utils/scripts/gen_work_zone_devices.py
python dev-env-utils/scripts/audit_obj_models.py <the models it wrote>
python dev-env-utils/scripts/preview_block_model.py \
    trafficaccessories/shared_models/workzone_barricade_type2.obj \
    csm:blocks/trafficaccessories/workzone/workzone_fold_rail_right -o /tmp/preview.png
```

`--scratch` writes into `_workzone_out/` instead of the repo tree; `--only <registry names>`
restricts what is generated. The generator also emits `_workzone_out/tab_fragment.java` and
`_workzone_out/lang_fragment.txt` for pasting into the tab and the lang file, so a registry name
cannot drift from its blockstate.

Four faults in this family only show up in game, and `audit_obj_models.py` catches three of them:

- **UVs outside 0–1 replace the whole model** with the purple-and-black placeholder — not just
  the offending face. The audit fails the build on these. The usual cause is a `y/16` mapping on
  a device that was scaled past 16 units tall; the fix is a per-device `v_span`.
- **Coplanar faces z-fight.** Where one member meets another, start the inner one *inside* the
  outer and omit the shared face. Braces run between post *centres*, not between their outer
  faces.
- **Inconsistent winding** makes a surface cull from the side you are looking at.
- The one it cannot catch: **the Forge OBJ loader mirrors the z axis.** A single-sided face
  authored at low z with a `-Z` normal renders facing *south*. Every other panel in this family
  was double-sided, so nothing revealed it for a long time.

Textures use a **flat swatch column** down the right-hand edge, sampled by everything that is not
striped. `flip-v` is on, so a face asking for swatch `v` samples sprite row `size * (1 - v)` —
the swatch rectangles must be drawn at the flipped rows. Drawn the natural way up, every swatch
face silently wears its neighbour's colour, with nothing failing anywhere.
