# Doors

Doors in the **Building Materials** tab of `csm_building`, beside the garage doors: twelve doors in
one class (`BlockBuildingDoor`, constructed by registry name), the **Door Closer** add-on item, the
**Door Swing Tool**, and the **Door Keypad** from the garage doors, which locks a door as well. Every asset comes from
`dev-env-utils/scripts/gen_doors.py` (`--check` fails on drift).

| Door | Registry name |
|---|---|
| Interior Door (Oak / White), and each with a Vision Lite | `door_wood_oak`, `door_wood_oak_lite`, `door_wood_white`, `door_wood_white_lite` |
| Hollow Metal Door (Grey) | `door_metal_grey` |
| Fire Door (Wired Lite) | `door_metal_fire` |
| Exit Door (Push Bar) | `door_metal_exit` |
| Storefront Door (Dark Bronze) | `door_storefront_bronze` |
| Front Door (White / Red / Black) | `door_front_white`, `door_front_red`, `door_front_black` |
| Back Door (Half Glass) | `door_back_halfglass` |
| Job Trailer Door (in the Construction Site tab, for the job trailer; see `CONSTRUCTION_SITE.md`) | `job_trailer_door` |
| Hospital Door (Maple / Blue Metal / Sage Metal; Vision Lite or Half Glass), and each as a Narrow Leaf | `door_hosp_<finish>_<leaf>`, `door_hosp_<finish>_<leaf>_narrow`; finish `maple`, `metal_blue`, `metal_sage`; leaf `lite`, `halfglass` |
| Smoke Barrier Door (Grey / Beige) | `door_smoke_metal_grey`, `door_smoke_metal_beige` |
| Lead-Lined Door (Radiation) | `door_lead_lined` |
| Automatic Sliding Door (Clear Aluminium / Dark Bronze), and its fixed Sidelite | `auto_door_slider_clear`, `auto_door_slider_bronze`, `auto_door_sidelite_clear`, `auto_door_sidelite_bronze` |
| Automatic Door Activation Mat | `auto_door_mat` |
| Interior Door (Dark Oak), with a Vision Lite, Front Door (Dark Oak), Fire Door (Dark Oak, Wired Lite) | `door_wood_dark_oak`, `door_wood_dark_oak_lite`, `door_front_dark_oak`, `door_wood_dark_oak_fire` |
| Cathedral Door (Dark Oak / Weathered Oak / Bronze), and its Arch Head | `cathedral_door_<variant>`, `cathedral_door_<variant>_arch`; variant `dark_oak`, `weathered_oak`, `bronze` |
| Tympanum (Dark Oak / Weathered Oak / Bronze) | `cathedral_tympanum_<variant>` |
| Carved Panelling (Dark Oak) | `panelling_carved_dark_oak` |

The dark oak and cathedral doors were added for a Gothic church (2026-10), whose great west
portal had to be built of CSM doors; see **Dark oak doors** and **Cathedral doors** below.

The hospital, smoke, lead-lined and automatic doors were added for a hospital build (2026-10),
whose corridors read as white walls with solid white doors; see **Hospital doors** and
**Automatic sliding doors** below, and **Placing by command** for the metadata of every door.

## Using them

- **Placing.** From outside, looking in: the door's facing is the way the player looked, the leaf
  hangs on the outside face of its cell and swings inward, as a vanilla door's does -- except the
  **Exit Door, the Storefront Door and the Fire Door, which swing out** (below). **Sneak while
  placing** to hang it the other way: an interior door that swings out, an exit door that swings
  in. A door placed beside another of the same kind facing the same way, not already one of a
  pair, makes a **pair** with it: it hinges on its far side, so the latches (and handles) meet in
  the middle, and a shut neighbour hinged on the shared side is turned round to match -- one
  already hinged on its far side is preferred, since it needs no change. A pair opens and closes
  together, and swings the way the first door does, sneaking or not. With no door to pair with,
  the hinge is on the side of the opening clicked.
- **Door Swing Tool** (item, crafted from an iron ingot and a Fastener Kit): right-click a door,
  either half, to make it swing the other way, and its pair with it, so a pair always swings the
  same way. It refuses (a status line says so) while the door or its pair is open or still moving,
  and on a custom door, whose movement is set in the Door Workshop.
- **Redstone** holds a door (and its pair) open while it is powered.
- **Door Closer** (item, crafted from two iron ingots, a Sheet Metal and a Fastener Kit): right-click
  a door to fit one. The door then shuts itself three seconds after a player (or a keypad) last
  opened it, unless redstone is holding it open, and wears a parallel-arm closer on its **push side** --
  the side it swings away from: the outside of a door that swings in, the inside of one that swings
  out -- with a shoe on the wall above the opening and an arm that folds and unfolds between them
  as the door moves. Sneak-click the door with an empty hand to take it off again. It is an
  add-on rather than a property of some doors because a real closer is -- any door may have one, and
  none does to begin with (the user asked for it to be optional, 2026-09-18).
- **Door Keypad**: linked to a door (sneak-click the keypad, then the door, both with an empty
  hand) it locks the door. From outside it then opens only with the keypad's code; from inside --
  the side the door faces -- it opens as ever. The button and control station link to doors too.
  Breaking the keypad unlocks the door. **A lock heals itself if its keypad is gone some other way**
  (a command, an editor, or a keypad broken before 2026-09-26, when breaking one did not unlock its
  door): the lock names its keypad in `DoorLocks`, and when it is about to stop someone -- a click
  from outside, or someone outside in a sensor door's zone -- the keypad's position is looked at;
  no keypad linked to the door there, and the lock is cleared and the door opens. Only then, never
  on a tick, and never loading a chunk: with the keypad's chunk unloaded the lock stands. A lock
  saved before locks named their keypad is named by its keypad when that loads; one still nameless
  is cleared once the chunks round the door are loaded, since a keypad is put up beside its door
  (one linked from further away, in a chunk not loaded since, is the case this gets wrong).
- **Animation** is a client option, `animateDoors` in the CSM configuration, default on. It changes
  only how a door is drawn, so each player's own setting applies and need not match the server's.

## How it works

**Two halves, eight bits.** As a vanilla door, the lower half stores its facing and whether it is
open, the upper half the hinge, whether a closer is fitted, and whether the door is **reversed**
(below); each half reads the rest from the other as actual state (`DoorSwingDirection` holds the
upper half's layout: 8 upper, 1 right hinge, 2 closer, 4 reversed). That is every bit either half
has, which is why a lock is not state: locks live in the world's saved data (`DoorLocks`, keyed by
the lower half, server side only).

**Free at rest.** Open or shut, a door is baked models and nothing else. While it swings -- eight
ticks -- its upper half has a `TileEntityDoorSwing` **on each client that can see it, and nowhere
else**, which holds only the direction and the tick it started. Whether a door is swinging is
whether that tile entity is there: `swing` is actual state, never stored, read off it in
`getActualState` (through the chunk cache's look-only `getTileEntity` while a chunk is built), and
the models draw nothing while it is true.

- **Starting.** Opening or shutting sets the lower half's `open` at once, as ever, and sends a
  block event; each client that gets it makes the tile entity itself (`eventReceived`) and has both
  halves rebuilt on the main thread (`notifyBlockUpdate` with flag 8), so the baked model and the
  renderer hand over in the same frame.
- **Ending.** The tile entity ticks -- on the client, for the eight ticks it exists -- and takes
  itself away once the swing is over, as a moving piston's does, then has the halves rebuilt again.
  Nothing has to tell it the swing is over, so a client that walks out of range mid-swing cannot be
  left holding one; a client that loads the chunk mid-swing sees the door where it is going, without
  the animation. Nothing is saved or sent.
- **The server makes none.** It knows a door is moving by the end-of-swing tick it schedules on the
  upper half (`isUpdateScheduled`), which is what the Door Swing Tool asks. That tick does nothing
  else now.
- **Why the upper half says it has a tile entity in every state.** A chunk refuses a tile entity
  whose block says it has none (`Chunk.addTileEntity`), and nothing in the stored state says the
  door is swinging any more. So `hasTileEntity` is true for every upper half, and
  `createNewTileEntity` makes none: a lookup at a door at rest asks for one and gets null, and the
  door stays free.

The renderer draws both halves' own closed models turned about the hinge, so handles, bars and a
fitted closer's body swing with the leaf; a closer's shoe stays put and its arm is solved for the
angle (below). With `animateDoors` off it draws them where they are going at once. Every face it
draws is shaded as the world shades a block face, by the way it faces at that moment, so the last
frame of a swing is as light or dark as the baked model that replaces it (the renderer used to draw
the leaf unshaded, a visible step at both ends of every swing).

**The open model is the closed one turned about the hinge**, a quarter turn about the pivot
(0.875, 15.125) px for a left hinge, which carries the leaf from the outside face to lie along the
jamb with its outside face to the room and its latch edge inward. The generator writes it out as
its own model, since a blockstate rotation turns about the block's centre and would put the hinge
in the wrong corner; the renderer turns about the same pivot, so the swing ends on exactly the
model that replaces it. The right-hinged models are the left's mirror.

**Which way a door swings: its kind's default, or reversed.** The exit, storefront and fire doors
(`OUTSWING`, in `gen_doors.py` and `DoorSwingDirection`, SHARED) swing **out** by default, toward
the outside, as real ones do: an exit door opens in the direction of escape, which is what lets a
push bar work at all -- on a door that swings toward you it is a pull handle nobody can pull. Every
other door swings in by default. Any placed door may be **reversed** from that (the upper half's
bit 4, the `reversed` state, which the lower half reads from it as it reads the hinge): the way a
door actually swings is its kind's default, the other way if it is reversed
(`BlockBuildingDoor.outward`, `outswing != reversed`). Everything that depends on the direction
reads that, never the kind: the models (through `reversed` in the blockstate), the collision box,
the renderer's pivot and turn, the closer's side and the push bar's press.

A reversed door's models are the **depth mirror of its kind's own**, shut and open, hardware and
all (`_out` for a kind that swings in, `_in` for one that swings out). So a push bar stays on the
face that is pushed: a reversed exit door swings in, and has its bar on the outside. The facing
still decides the inside, so a keypad lock still lets people out freely from the side the door
faces, whichever way it swings.

**Choosing.** Sneaking while placing hangs a door reversed; the Door Swing Tool flips a hung one
and its pair (`flipSwing`), only while both are shut and still, since flipping one open or
mid-swing would jump its leaf across the cell. A door placed as a pair takes its partner's
direction whatever the player does, so a pair can never swing two ways. A custom door is never
reversed (`reversible()`): its movement belongs to its settings, and its swing always goes in.

**Worlds saved before a door could be reversed.** Bit 4 used to mean "swinging", so it is read as
"reversed" now. A door at rest had it clear, so every existing door is unchanged. A door saved in
the eight ticks of a swing had it set -- and also had the swing's tile entity saved with the chunk,
which the server otherwise never has. That is how it is told apart: a `TileEntityDoorSwing` on the
server is put right on its first tick, or by the end-of-swing tick the save brought back, whichever
comes first (`TileEntityDoorSwing.putRightLegacySwing`): bit 4 is cleared, so the door is not
turned round, and the tile entity goes.

A door that swings out is the **depth mirror** of one that swings in: its leaf hangs on the *inside* face of
the cell (z 0..1.75 drawn north-inside) and turns outward about (0.875, 0.875), so the open leaf
lies along the hinge jamb inside its own cell exactly where an inswing door's does, and the open
collision box is the same. Only the shut box moves, to the inside face. The one thing that has to
leave the cell is the hardware on the inside face -- the push bar, the inside lever -- which stands
proud of the wall into the room by its own depth while the door is shut, as it does on a real door
hung flush with the wall; open, it is inside the cell. The facing, the placing rule, pairs,
redstone and keypad locks are untouched: the inside is still the side the door faces, and still
the side a locked door lets people out from, which for an exit door is the side with the push bar.

**The closer's arm articulates.** A real closer is three parts: a body on the leaf, a shoe fixed to
the frame, and an arm of two rigid links between them -- the main arm from the body's spindle to an
elbow, the forearm from the elbow to the shoe. The leaf carries the spindle round the hinge; the
shoe does not move; so the elbow can only be where a circle of the main arm's length about the
spindle meets one of the forearm's length about the shoe, on the side the arm folds to.
`DoorCloserArm.solve` works that out for any angle and `gen_doors.closer_joints` for the two it
bakes, from the same constants (`CLOSER_*`, SHARED): shut, the arm lies folded along the door with
its elbow toward the latch, as a parallel arm does; open, it reaches from the body, now in the
opening, out through the top of the opening to the shoe.

- **It is on the push side, for every door, whichever way it swings.** The leaf opens *within the wall's thickness* and ends
  lying along the jamb, so the face the door swings toward -- where a regular-arm closer would go --
  finishes against the jamb block, and an arm from a body there cannot reach the frame without
  passing through the open leaf. The push face turns into the opening instead, and a parallel-arm
  closer, the common mount on real commercial doors, is what goes there. So an inswing door's
  closer is on its outside (it used to be on the inside, with an arm that swung off with the leaf),
  and an outswing door's -- the exit door's, beside the push bar -- is on its inside. The body and
  shoe stand proud of the wall by a few pixels, as on a real door hung flush with the wall.
- **The models mark which part is which by tint index** (no colour handler is registered for the
  doors, so it tints nothing): the shoe `TINT_FIXED`, which the renderer draws unswung, and the arm
  `TINT_ARM`, which it leaves out and draws itself as bars between the solved points. The body is
  unmarked and swings with the leaf.
- **Both baked poses put each link on a multiple of 22.5 degrees**, the only angles a model element
  can be turned to. That is what fixed the link lengths and the shoe (5.3482 and 5.2133 px, shoe at
  (6.5317, 18.995)): shut the links are at 0 and 157.5, open at 45 and 90. Of the arrangements that
  land on those angles, this is the one that keeps the arm clear of the leaf and the jambs for the
  whole swing and does not pull straight or fold flat. `DoorCloserArmTest` holds the baked models
  to the solver and the arm clear all the way round: change a constant and it fails until the
  generator and the Java agree again.

**A push bar presses.** On the exit and storefront doors the touch bar (`TINT_PUSH`) dips a pixel
toward the leaf over the first quarter of an opening swing and comes back out over the second --
north in model space on a door that swings out, south on one reversed to swing in -- as
it does under the hand that unlatches a real one. Closing, it stays out: a door shutting on its
closer latches without the bar moving. This is the renderer's alone -- at rest the models draw the
bar out, as they always did, so a door with no swing in progress costs nothing more.

Glazed doors (the lites, the fire door, storefront, half-glass back door, job trailer door) draw
in **two passes**: the faces that wear the door's own face textures (`_upper`, `_lower`, where every
lite and pane is drawn) in the translucent pass, and everything else -- the leaf's edges, handles,
push bar and a fitted closer -- in the cutout pass (`DoorLayerModel`, which wraps each glazed door's
baked model and sorts its quads by the pass being drawn). The rest are cutout only. Drawn whole in
the translucent pass, a glazed door's closer lost its body to the frame behind it from some angles
and its arm looked folded the wrong way (issues #235 and #236): see the traps. The wrapper is
registered with Core's `CsmBakedModelWrappers` rather than put in place by walking the model registry
in a `ModelBakeEvent` handler, which in a pack running VintageFix finds no door at all and left every
glazed door's glass opaque (issue #242; PERFORMANCE_AND_SECURITY.md).

## Hospital doors

Twelve leaves on `BlockBuildingDoor`, by registry name, three finishes by two leaves, each full
or narrow: a **maple** veneer, or hollow metal painted **blue** or **sage** (named "Blue Metal",
"Sage Metal", which is also what prices them in sheet metal); a **vision lite**
(a narrow 4 x 25 in lite toward the latch) or **half glass**. Every one has a stainless kick plate
across the foot of both faces and a lever. They swing in, as an interior door does, and do
everything the other doors do: pairs, the Door Closer, the Door Swing Tool, keypad locks,
redstone.

- **Single and double.** One door is a single; two of a kind side by side, latch to latch, are a
  double pair, made by placing them (or by command, below) as any pair is.
- **1.5-leaf (unequal pair).** A **narrow leaf** (`..._narrow`) is an 8 px leaf hinged in the
  middle of its cell, with a fixed sidelite in the other half: an inactive leaf beside its
  frame's sidelite. It pairs with the full leaf of its own finish and leaf type
  (`BlockBuildingDoor.pairFamily`, the registry name without `_narrow`), and is placed on the full
  leaf's latch side, hinged the other way, so the two leaves meet in the middle. How the pair
  opens:
  - a hand (or a linked keypad, button or station) opening the **full leaf** opens it alone; the
    narrow leaf is **held**, as a real inactive leaf is by its flush bolts;
  - a click on the **narrow leaf** opens **both** -- for a bed or a cart;
  - **redstone** at either opens and shuts **both** (`setOpen` skips the held leaf only for a
    hand opening the full leaf);
  - shutting either shuts both.
  The narrow leaf takes no closer (its models have none, and the item refuses it); fit the
  closer to the full leaf, which shuts the pair. It turns about (8.875, 15.125) px rather than the
  usual pivot (`pivotX`, SHARED with `gen_doors.NARROW_PIVOT_X`), and its sidelite is marked
  `TINT_FIXED`, so the swing renderer turns the leaf and leaves the sidelite where it is (the
  renderer now draws a lower half's fixed quads too). Open, its box covers the sidelite and the
  open leaf. Its face texture is the 8 px leaf drawn twice side by side, and the sidelite's the
  same, so a face shows the whole drawing whichever half its position-derived UVs land on.
- **Smoke barrier doors** (`door_smoke_metal_grey`, `door_smoke_metal_beige`): cross-corridor
  smoke compartment doors, hollow metal in neutral colours with no red in them, a tall vision
  panel, a stainless push plate on the push face and a pull bar on the other, kick plates. Placed
  as a double pair across the corridor. **The closer is built in** (`builtInCloser`): the upper
  half reads its closer bit as set whatever the metadata says, sneak-clicking does not take it
  off, and breaking one drops no closer item. So the doors **shut the moment the redstone holding
  them goes**: Life Safety's Magnetic Door Holder powers the door beside it while its panel is
  normal and drops the signal on alarm, which is all the release there is. Nothing in Building
  names Life Safety; any redstone does the same.
- **Lead-lined door** (`door_lead_lined`): a solid veneer leaf with a small lead-glass lite and a
  radiation trefoil plate (the public-domain symbol, black on yellow) on its outside face. The
  plate stays on the outside face whichever way the door swings: reversed, the leaf is mirrored to
  the inside face of the cell and the plate is put back on its outside (`trefoil_plate` in the
  generator), rather than mirrored with it as handles are.

## Automatic sliding doors

`BlockAutoSlidingDoor` (by registry name) is a `BlockBuildingDoor` that slides: two halves, the
same metadata, keypad locks and pairs, so all of that works unchanged. A **slider**'s hinge is the
side it slides toward; it stands in the middle of the wall (z 4..5.75 px, inside to the north)
and slides 15.5 px along it, ending in the next cell: in front of a fixed **sidelite** (which
stands in the plane behind it, z 6.5..8.25) or into the wall, like a pocket door.

- **Bi-parting**: two sliders side by side hinged on their far sides, latch to latch (a pair); they
  part in the middle. **Single-slide**: one slider with a sidelite on its hinge side. An entrance:
  sidelite, slider, slider, sidelite.
- Every cell carries a length of the operator header across the top of its upper half (fixed,
  `TINT_FIXED`), and a slider the motion sensor on each face of it. The panels are aluminium (clear
  or dark bronze) round clear glass with a mid-rail at push height and dark break-out pivots at
  the hinge-side corners, so they read as break-out panels. Glass in the translucent pass, the rest
  in the cutout pass (`ICsmGlassFronted`).
- **Opening.** Redstone opens a slider and its pair while powered; the **activation mat**
  (`BlockAutoDoorMat`) beside it opens it when someone steps on it; a click opens it by hand; a
  linked keypad, button or station as on any door. A sidelite never moves (a click does nothing).
- **Closing.** Whatever opened it, it shuts **3 s** (`HOLD_TICKS`, 60) after the last thing that
  did: each step on a mat pushes the time back (`armClose`, the closer's deadline). The door's one
  scheduled tick then shuts it, unless redstone still holds it, or a player or a non-hostile
  creature is in the doorway or within a block and a half either side, when it looks again a
  second later. That look is the only entity search and only an open door makes it. When the
  redstone goes, it shuts the same way after the hold time. **Nothing ticks while the door is idle
  and there is no tile entity at rest.**
- **The slide is animated** by the swing's own machinery: the same client-only
  `TileEntityDoorSwing`, made by the block event and gone when the movement ends, now told how
  long the movement takes (`motionTicks`: 8 for a swing, 14 for a slide), and the same renderer,
  which moves the panel's shut models along the wall (eased) instead of turning them, leaving the
  header where it is. At rest it is baked models; the open models are the shut ones moved 15.5 px
  with the UVs they had shut written out, since past 0..16 a position-derived UV samples other
  sprites.
- **Locks.** A slider locked by a keypad does not open for a mat on its outside (the side away
  from its facing); a mat on the inside still opens it (free egress). A click from outside asks
  for the code as on any locked door.
- **The mat** is a pixel-thin rubber mat with an aluminium edge, laid on the floor in front of the
  door. It opens sliders in the four cells around it at its own height and the cell above it (a
  pair opens with either half). It answers players and any creature that is not hostile, at most
  every 10 ticks a mat. It listens through `onEntityCollision`, as a pressure plate does, not
  `onEntityWalk`: the game sends the walking callback to the block under the walker's feet, which
  for a mat a pixel thick is the floor under it.
- No closer (the item refuses it) and no reversing (the Door Swing Tool refuses it).

## Dark oak doors

Four more leaves on `BlockBuildingDoor`, drawn by the same generator rows as their oak and white
siblings, so they do everything those do (pairs, hinge sides, the Door Closer, the Door Swing
Tool, keypad locks, redstone): a flush veneer **interior door**, the same with a **vision lite**, a
six-panel **front door** (the family's panelled look) and a **fire door** with a wired lite, the
fire door's look on a veneer leaf. As the steel fire door does, the dark oak fire door swings
**out** by default (`OUTSWING`, in `gen_doors.py` and `DoorSwingDirection`). The plain and lite
leaves wear black levers, the front door brass, as the white and black front doors do.

## Cathedral doors

A great door for a church portal: `BlockCathedralDoor` (`cathedral_door_<variant>`), a leaf **two
blocks wide** and as tall as its opening, in dark oak, weathered oak or bronze, with iron strap
hinges on its bottom and top rows (bronze straps on the bronze door), a ring pull, and studded
panels on its outside face (boards and ledges inside). `BlockCathedralDoorArch`
(`cathedral_door_<variant>_arch`) is its **pointed head**: three courses of leaf above it, cut by
the arch, with the stone beside the arch (the spandrel) in the leaf's plane, in the module's own
ashlar or cast stone veneer (below). Two leaves
latch to latch are a **pair**, and their heads make one arch four blocks across; a single leaf's
head is a lancet two across. Both rise three blocks. `BlockCathedralTympanum` fills the same arch,
fixed, over a square-headed door, and `BlockCarvedPanelling` is a block of linenfold for the
surround. Every asset comes from `gen_cathedral_doors.py`, which `gen_doors.py` imports and writes
and checks with the rest (no religious symbols: the tympanum's carving is plain blind tracery,
two lancets and a ring).

- **Placing from the item.** Look in from outside, as for any door. The cell clicked into is the
  leaf's **hinge column** and the leaf grows to the right (**sneak**: hinged on the right, growing
  left), filling the opening upward: three blocks, or four if the fourth is clear. A leaf placed
  beside another leaf's latch column, that leaf still alone, becomes its **pair**: it hinges on
  the far side and grows away from it. The **arch head** is placed on top of a leaf (click the
  top of its top row): it fills three courses over both columns, hung and opened as the leaf is.
  The **tympanum** is placed from its left column: four blocks across where there is room
  (**sneak** for two), three high, on the top of a square-headed leaf or anywhere else. A click on
  the top of a leaf holding either places it rather than opening the door.
- **The spandrel's stone** is `stone_ashlar` or `stone_cast`, the Building module's own stone
  veneers (`gen_stone.py`), so an arch set in a wall of either reads as part of it: **cast stone
  where a jamb either side of the arch, at that course, is a cast stone block** (the block, its
  stairs, slab or fence), ashlar otherwise. Nothing is stored and nothing is chosen: build the jambs
  of cast stone for cast stone dressings round an ashlar wall's doorway. The tympanum does the
  same.
- **Using.** A click opens or shuts the leaf and its pair. **Redstone** at any cell of a leaf
  holds it and its pair open while powered. A **keypad, button or control station** links to a
  leaf as to any door (sneak-click the control, then any cell of the leaf); a keypad locks the
  leaf (and so the pair) from outside, and the inside still lets people out. No closer and no
  reversing: a cathedral door swings in.
- **Breaking** any cell of the leaf takes the whole leaf and its head; one item drops (and the
  head's, for a survival player). Breaking a head cell takes only the head.

**One block per cell, not a master and parts.** A leaf could have been one master cell with
invisible part cells, as Roads' utility box and Healthcare's units are. That was weighed and
turned down: a master's four bits are spent on the facing, open and hinge every leaf needs, so its
height and its head would have had to live in a tile entity, and a refill command would have been
needed to put back lost parts; one cell drawing a 2 x 7 leaf would be lit by its own light, culled
with its own chunk section, and reach past the -16..32 a JSON element allows. Instead, as a garage
door is built to its opening, **every cell stores the same four bits** -- facing (bits 0-1),
open (bit 2), hinged on the right (bit 3) -- and reads the rest from its neighbours as actual
state:

| Property | Where from |
|---|---|
| `column` (hinge / latch) | count the like cells beside it toward its hinge side: even is the hinge column, odd the latch, so two leaves hinged the same way side by side stay two leaves |
| `row` (bottom / middle / top) | whether a like cell is below it and above it; straps go on the bottom and top rows, the ring pull on the bottom row of the latch column |
| `course` (0-2), `paired` (head only) | like cells below it; whether, past its latch column, the next leaf's head is hinged the other way |
| `stone` (ashlar / cast, head and tympanum) | the jambs either side of the whole arch at the cell's height: beside this leaf's hinge column, and past its latch column (past the other leaf of a pair); cast if either is a `stone_cast` block |
| `swing` | whether the leaf's **master** -- its bottom cell on the hinge side -- has a `TileEntityDoorSwing` |

So each cell draws its own piece from baked models and nothing has a tile entity at rest. States:
the leaf block 4 facings x 2 open x 2 hinges x 2 columns x 3 rows x 2 swing = **192**, the head
4 x 2 x 2 x 2 x 3 courses x 2 paired x 2 stones x 2 swing = **768**, the tympanum 4 facings x
4 columns x 3 courses x 2 paired x 2 stones = **192**, the panelling 1; 3,457 for all ten blocks.
Every blockstate is a multipart, which CSM bakes once for all of a block's states, so the stone
adds states (plain objects, no neighbour table) and no baked model. A body cell draws one model;
a head or tympanum cell draws its piece of leaf and, where the arch leaves any, its spandrel, a
part of its own shared by every variant and the tympana (`cathedral_spandrel_<stone>_...`, 40
children of 20 geometry parents). Each model is a child of a geometry parent shared by the three
variants (a model names textures only): 110 geometry parents and 310 small children in all. The
cost of finding things out from
neighbours is a handful of `getBlockState` calls per cell when a chunk is built (the run walk
stops at the first unlike cell; a leaf is at most 12 cells tall), and the same when a door is
clicked or its neighbours change.

**The swing.** Opening sets every cell of both leaves' `open` (flag 10, no neighbour updates) and
sends a block event to each master; each client that gets it puts a `TileEntityDoorSwing` on the
master (the same client-only tile entity as the doors', told to take 16 ticks) and has every
cell rebuilt. The cells read `swing` true and draw nothing; the renderer draws every cell's own
shut model, moved to where the cell is in the leaf and turned about the hinge pivot (T/2, 16 -
T/2) px, T = 2.5, which `gen_cathedral_doors.py` turns the open models about, so the swing ends on
exactly the open models. The head's stone is marked `TINT_FIXED` and drawn where it is. When the
swing is over the tile entity takes itself away and the cells are rebuilt again. The latch
column's open model lies along the jamb a block inside its cell, at x -16..-13.5 px and z -16..0
in its own frame, inside what a JSON element may reach. Nothing on the inside face of the latch
column can stand proud of the leaf, or its open model would pass -16: the ring pull and the
straps are outside.

**The open leaf's far half.** That half lies in the cell inside the hinge column's, diagonal to
its own cell, and neither the game's collision nor its look ray ever asks a block about a cell
diagonal to it (below, Traps). So the **hinge column's** open box is the whole leaf, its own half
and the latch half beside it, which makes the far half solid; and a click whose look ray meets
the far half before what it landed on (the wall behind, as a rule) is given to the door by
`DoorClickThrough`: a right click works the door as a click on the leaf does, a left click does
nothing, so the wall is neither built on nor broken. The outline still shows the wall there.

**The arch.** Each side of the arch is an arc centred on the springing line: over a pair, a span of
64 px and a rise of 48 (radius 52, a drop arch); over a single leaf, 32 and 48 (radius 80, a
lancet). It is stepped a pixel row at a time and rows of the same width merged, so a head cell is
up to sixteen elements of leaf and as many of stone. The tympanum's carving is one 64 px sheet
drawn at 16 px a block, each cell's face given its window of it as explicit UVs.

## Placing by command

Every door can be placed and set up with `/setblock` alone; none needs a GUI or a click. A door
is two blocks: put the **lower half first, then the upper half** in the cell above (with nothing
changing round them between, the lower half does not look for its upper half until a neighbour
changes). Facing is a horizontal index: **S 0, W 1, N 2, E 3**. Hinge left or right is as seen
from outside, looking in (facing north: left is west).

| Half | Bits | Meaning |
|---|---|---|
| Lower | 0-1 | facing: the way to the **inside** (the way a player faced, from outside, placing it) |
| Lower | 2 (4) | open |
| Upper | 3 (8) | always set: this is the upper half |
| Upper | 0 (1) | hinge on the right |
| Upper | 1 (2) | closer fitted (always read as set on a smoke barrier door) |
| Upper | 2 (4) | reversed: swings the other way from its kind's default (ignored by sliding doors) |

So a shut door facing north, hinged left: lower `2`, upper `8`; the same hinged right: upper `9`;
with a closer: upper `10` / `11`. A **pair** facing north: the west door hinged left (`8`), the
east door hinged right (`9`). A **1.5 pair**: the full leaf and the narrow leaf of the same finish
and leaf, the narrow one on the full leaf's latch side and hinged the other way (west full `8`,
east narrow `9`). Keypad locks are not state: link a keypad as usual, or leave them out.

| Block | Lower | Upper | Notes |
|---|---|---|---|
| `door_hosp_*` (full) | facing, open | hinge, closer, reversed | swings in by default |
| `door_hosp_*_narrow` | facing, open | hinge, reversed (closer ignored) | pairs with its full leaf |
| `door_smoke_metal_*` | facing, open | hinge, reversed (closer always on) | held open by redstone, shuts when it goes |
| `door_lead_lined` | facing, open | hinge, closer, reversed | plate on the outside face |
| `auto_door_slider_*` | facing, open | hinge = the side it slides to | opens on redstone or a mat |
| `auto_door_sidelite_*` | facing | `8` (hinge ignored) | never moves |
| `auto_door_mat` | `0` | -- | one block, on the floor beside the door |
| `door_wood_dark_oak`, `_lite`, `door_front_dark_oak` | facing, open | hinge, closer, reversed | swing in by default |
| `door_wood_dark_oak_fire` | facing, open | hinge, closer, reversed | swings out by default |

No door has NBT to set: at rest no door has a tile entity (the custom door's settings are the
exception, in its own section).

### Cathedral doors by command

A cathedral door has no master to place and nothing to refill: **every cell of a leaf, head
included, takes one metadata value**, `facing + 4 * open + 8 * hinged right`, and works out the
rest from its neighbours. Fill the leaf's cells with `cathedral_door_<variant>`, the head's three
courses above with `cathedral_door_<variant>_arch`, in any order. Facing is the way to the
inside (the way a player faces, outside, placing it): **S 0, W 1, N 2, E 3**; left and right are
seen from outside, looking in.

| Facing | Left is | Leaf hinged left (shut / open) | Leaf hinged right (shut / open) |
|---|---|---|---|
| north | west (-x) | `2` / `6` | `10` / `14` |
| east | north (-z) | `3` / `7` | `11` / `15` |
| south | east (+x) | `0` / `4` | `8` / `12` |
| west | south (+z) | `1` / `5` | `9` / `13` |

A **pair** is a leaf hinged left with a leaf hinged right on its right, latch to latch. A
**2 x 4 arched pair facing north**, its lower west corner at (x, y, z), dark oak:

```
/fill x   y   z x+1 y+3 z csm:cathedral_door_dark_oak 2
/fill x   y+4 z x+1 y+6 z csm:cathedral_door_dark_oak_arch 2
/fill x+2 y   z x+3 y+3 z csm:cathedral_door_dark_oak 10
/fill x+2 y+4 z x+3 y+6 z csm:cathedral_door_dark_oak_arch 10
```

or cell by cell, the same thing: `/setblock` each of the 16 leaf cells (x..x+3, y..y+3) with
the leaf block and each of the 12 head cells (x..x+3, y+4..y+6) with the head block, `2` for the
west two columns and `10` for the east two. The west leaf's master is (x, y, z), the east
leaf's (x+3, y, z): that is where a keypad links, and where a lock is kept. Facing east, left
is north: the same pair in the wall at x, its north end at z, takes `3` in (x, z) and (x, z+1)
and `11` in (x, z+2) and (x, z+3). To open a placed leaf by command, set every one of its
cells (and its pair's) to the open value; a click or redstone does that for you. A single leaf
with a head is two columns and the head's lancet follows from it having no pair.

A **tympanum** cell's metadata is `facing + 4 * column`, column 0 to 3 from the left seen from
outside; course and pair are found from the neighbours. Over a square-headed pair facing north
at (x, y, z): `/setblock x+c y+k z csm:cathedral_tympanum_dark_oak <2 + 4c>` for c 0-3, k 0-2
(metadata 2, 6, 10, 14 by column). Over a single leaf, columns 0 and 1 only. The stone beside
an arch head or tympanum is not set: it is cast stone where a jamb either side of the arch is
`csm:stone_cast` (or its stairs, slab or fence) and ashlar otherwise, so set the jambs first or
after, in any order.
`panelling_carved_dark_oak` is a plain block, metadata 0.

**Replacing other doors in place.** Clear the old doors first (`/fill ... air` changes the whole
region before any neighbour is told, so nothing drops), or set each two-block door's **lower
half first**: setting an upper half first makes its lower half find itself alone, remove itself
and drop its item. Setting a cathedral cell to anything else takes the rest of its leaf away
(breaking any cell does), so swap a cathedral leaf for another variant by setting all its cells.

## Cost

A storefront door is two glass panes and a Sheet Metal, a hollow metal, fire or exit door two Sheet
Metal and a Fastener Kit, and a wood or residential door three planks. A cathedral door leaf (or
its arch head) is six planks and an iron ingot, or three Sheet Metal and two Fastener Kits in
bronze; a tympanum three planks (two Sheet Metal in bronze); carved panelling two planks. The Door Closer and the
Door Swing Tool are items, so they have crafting recipes (`recipes/door_closer.json`,
`recipes/door_swing_tool.json`, only while `csm_building` is loaded) instead of a Fabricator cost. The Door Workshop is four planks, two Fastener Kits and a Sheet Metal
(a bench with a vice), priced before the door rule catches its name.

## Traps

- **An item that acts on a block uses `onItemUseFirst`.** A right-click is offered to the block
  before the held item's `onItemUse`, and the door took it and opened; the closer item and the
  swing tool act in `onItemUseFirst`, which comes first.
- **`hasTileEntity` is asked with the stored state only**, and a chunk drops a tile entity its
  block says it has none of. A tile entity whose presence is itself the state (the swing) needs the
  block to say "yes" for every state and make none itself; making it is then someone else's job
  (here the block event), and so is taking it away (here its own tick).
- **"Hollow Metal Door" is a coloured metal set** to a cost rule on the word "metal", which comes
  first in the Building Materials rules. Doors are priced before it, and the garage door fittings
  and keypad, whose names also say "door", are kept out of the door rule.
- **`renderModelBrightnessColor` throws away a baked model's face shading.** It overwrites every
  vertex colour with the brightness it is given, so a model drawn that way in a renderer is lit
  evenly on every face. The swing renderer writes each quad itself and shades it by where it faces.
- **A closer on the pull face cannot work here** (see the closer's arm): the open leaf lies along
  the jamb inside the wall, and the pull face ends against the jamb block.
- **The translucent pass writes no depth.** `EntityRenderer` draws it under `depthMask(false)`, in
  the order of a per-face distance sort that is redone only as the camera moves. That is right for
  glass and wrong for anything solid: solid parts in that pass are drawn in whatever order the sort
  left them, so a nearer box can be painted over by a farther face. Anything solid on a glazed
  block belongs in the cutout pass.
- **A self-closing door's time runs from its latest opening.** A scheduled tick cannot be moved or
  cancelled, and a second schedule for the same block is dropped while the first is pending, so
  the closer's (and a custom door's auto-close) tick reads a deadline each hand-opening pushes back
  (`armClose` / `closeDue` in `BlockBuildingDoor`) and waits on if it has come early. Before that, a
  door shut by hand and opened again inside its time slammed on the first opening's tick (#234).
- **A cell that finds its leaf from its neighbours cannot do so once it is gone.** `breakBlock`
  is called after the world already holds what replaced the cell, so a cathedral cell works out
  its leaf from the cells still there and its own old state (`was`), not from the world.
- **A JSON element's -16..32 limit binds the open pose, not the shut one.** A cathedral latch
  column turns a block away from its cell; anything on its inside face would have passed -16.
- **An entity collides with nothing diagonal to the cells it is in.** `World.getCollisionBoxes`
  looks one cell past the entity's box each way but leaves out the corners of that ring, so a
  box that reaches into the cell diagonal to its own block is never asked. A cathedral leaf's
  latch half was walked through until the hinge column's box took it in.
- **The look ray asks only the cells it crosses.** A box that reaches into a cell of air is never
  hit by a ray that crosses only that cell: an open cathedral leaf's far half was clicked through,
  and a held block was placed on the wall behind it (`DoorClickThrough` gives such a click to the
  door).
- **A door takes a click before the held item does.** The arch head and tympanum are placed by
  clicking the top of a leaf, which opened the door instead (sneaking placed a head but made a
  tympanum two wide); the leaf passes a click on its top to those two items.
- **1.12's long array tag cannot be read back** (no getter), so `DoorLocks` stores positions as
  pairs of ints.
- **A door's box is not always where the door is drawn.** A swing's box jumps to where the swing
  ends as it starts, so a second click during the 8 ticks goes through the doorway; a sliding or
  splitting custom door leaves its cell clear once open. Such a click landed on the floor beyond
  and placed the held block (#244). `DoorClickThrough` (both sides) denies the held item's use
  when the look ray crosses a door's cell on the way to its target. The target still gets its
  own click, the cells the player stands in are left out, and sneaking still places.

## Custom doors

`BlockCustomDoor` (`custom_door`) is a door made of any three blocks -- a frame, an upper and a lower
material -- with its own movement, sound, speed, auto-close, redstone mode and proximity sensor, all
in `CustomDoorSettings`. They are made in the **Door Workshop** (below); the plain custom door in the
tab (oak) is what the Fabricator makes.
Modelled on another mod's door factory, which the user asked for; implemented from scratch and
built to be **free at rest**, where that one draws every custom door every frame.

- **Settings** live in a data-only `TileEntityCustomDoor` on the lower half. No renderer is
  registered for it, so it costs nothing a frame: a chunk lists only tile entities with one.
- **Drawn baked.** `CustomDoorBakedModel` reads the settings through the block's extended state and
  faces the leaf from `CustomDoorGeometry` (a frame of stiles and rails round a set-in panel) with
  each material's sprite -- its model's north face, so a log shows its bark. Quads are cached per
  combination (settings, half, facing, hinge, open, paired, render pass): a street of identical
  doors bakes once. Each material draws in the pass its own block draws in, so glass is translucent.
  Materials are any plain-model block with no tile entity (`CustomDoorMaterials`).
- **Moving doors** are drawn by `CustomDoorRenderer` from `RenderWorldLastEvent`, only the doors in
  `CustomDoorMotion` (put there by a block event the server sends when a door moves), from the same
  cached quads under the movement's transform; the baked model draws nothing for a door while it is
  there. Not a TESR, which would be visited for every door every frame.
- **Movements.** Swing (as the fixed doors: the leaf on the outside face, turned about the hinge
  pivot); Slide (toward the hinge, into the wall -- a pair parts in the middle); Slide Together (a
  pair both to the left, the second stacked behind); Slide Up; Split (upper up, lower down). A door
  that slides, lifts or splits stands in the **middle of the wall's thickness**, as a glass pane does
  (the user's suggestion): it travels along the middle of the wall and ends wholly inside the block
  it slides into. It stops half a pixel short of a whole block, so its edge shows in the jamb like a
  pocket door's rather than lying in the jamb's plane, where the two faces would fight.
- An open door that has slid away is walked through freely; a strip at the jamb it went into can be
  clicked to shut it.
- **Redstone modes:** normal, redstone only, hand only, and redstone lock (a signal shuts and locks
  it). Auto-close is a number of ticks. The closer item does not fit a custom door.
- **Proximity sensor.** A door with the sensor on looks every 5 ticks through its own scheduled
  block tick, which re-schedules itself -- no ticking tile entity, and a door without a sensor
  schedules nothing. It is armed when the door is placed, when its settings change (the workshop,
  or `/blockdata`) and when its chunk loads; a pending tick is saved with the chunk anyway. It sees
  players only (a sensor that opened for mobs would let them in), 2.5 blocks out either side of
  the opening, two blocks high; a pair watches both its openings, or one leaf would shut the pair
  on someone in the other. It shuts once nobody has been there for the auto-close time, or a
  second if that is off, unless redstone is holding it open; a redstone lock stops it opening.
- **Keypad locks** work as on the fixed doors (a custom door is a `BlockBuildingDoor`): link a
  keypad by sneak-clicking it and then the door, and the door is locked in `DoorLocks` from the
  outside. With a sensor, a locked door opens only for someone on the inside -- free to leave,
  the code to come in, as an access-controlled automatic door -- but once open **anyone** in the
  zone holds it open, so it does not shut on the person who has just keyed it open.

### The Door Workshop

`BlockDoorWorkshop` (`door_workshop`, a bench block) opens `GuiDoorWorkshop` (GUI id 27, the one
screen in the module with a server-side container). Its `TileEntityDoorWorkshop` holds five slots
(frame, upper, lower, edit, output), the **design** on the screen, and up to twelve **saved
designs**; it is never ticked or drawn.

- **The design is a whole door**: three materials and the behaviour. A block put in a material slot
  becomes the design's material and stays so when taken out, so an empty slot shows the block the
  next door still needs, faded (a ghost). What Make builds is the slot's block, or the design's
  where the slot is empty. In survival every slot must hold its block and one of each is used up; a
  player in creative uses nothing and may make from the ghosts. Shift-click Make for a stack.
- **Set** gives the doors in the edit slot the screen's behaviour and keeps their materials;
  **Copy** takes the whole design from the door in the edit slot. A placed door's design comes back
  the same way: pick block in creative, or break it (the drop keeps its settings).
- **Saved designs** are named on the workshop and shared by whoever uses it; Save replaces a design
  of the same name.
- **The preview** is the door as Make would build it, turning slowly, shut a second, opening at its
  own speed, open a second, closing -- a pair for Slide Together and Split. It is drawn with
  `CustomDoorRenderer.drawDoor`, the same call and the same cached quads as a moving door in the
  world, so the two cannot disagree, and only while the screen is open. It is clipped to its
  window (scissor), so a sliding panel goes out of sight as into its pocket.
- **Nothing is trusted from the client.** `DoorWorkshopPacket` carries only the behaviour, an action,
  a design index and a name (cut to 24 printable characters); the handler checks reach and that the
  player's open container is this workshop's, takes materials from the slots, and asks the player's
  own game mode for creative.
- **The screen is 236 high** with 18-high buttons (`ShortButton` draws the texture's bottom edge
  rather than cutting it off), so it fits a 240-high window: a small window at GUI scale 2.

