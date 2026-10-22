# The Technology Module: Computers, Racks and Networking

CSM: Technology (`csm_technology`, tree `modules/technology`, Java package `technology`) is the
optional module for the electronics in a city's buildings. Before the 2026-10 expansion it was
one tab of AV gear, phones, set-top boxes, the iMac family, a few access points and the school
time and PA set (SCHOOL_TIME_AND_PA.md). The expansion added the two things a modern office or a
data centre could not be furnished without: **computers and their peripherals**, and **network
and IT gear**, from a desk switch to a room of 42U racks and the radios on the roof. It added 90
blocks and 19 items in two new tabs, all Core only.

Every block here is decorative or nearly so. The module's rule for this expansion was the
Healthcare module's: **baked models, animated textures, click states; nothing ticks, nothing
links, and no renderer.** A rack full of servers costs what an empty one costs to draw (measured,
below).

## Three tabs

| Tab | Id | Order | Holds |
|---|---|---|---|
| **CSM: Technology** | `tabtechnology` | 8 | What was there before: TVs, speakers, the iMac family, set-top boxes, phones, the three older access points (`waptpl225`, `wapac`, `wapn`), the NEMA enclosure, the ONT, and the school time and PA set (`technology.school`) |
| **CSM: Technology - Computers & Peripherals** | `tabtechcomputers` | 31 | Gaming rigs and desktops, monitors, keyboards, mice and pads, desk extras, consoles (51 blocks); console controllers and a VR headset as items (5) |
| **CSM: Technology - Networking & IT** | `tabtechnetworking` | 32 | Micafi desktop switches, gateway, Control Hub, the Corvane modem, PoE injector, wall jacks, floor box, cable trays; Micafi access points and outdoor radios; the 42U racks, the pre-filled racks and the 12U wall cabinet (38 blocks); the fourteen rack-mount units as items (14) |

The new tabs were given the next free orders, so nothing that existed moved. The computing tab
is not called "Gaming": Furnishings already has a tab of that name, for its arcade cabinets.

Creative order inside a tab is registration order (`gen_*.py --fragments`): the generators
group their catalogue by registry prefix before writing the tab lines (`_GROUPS` at the end of
`gen_technology_computers.py`).

## No overlap with other modules

Furnishings' Commercial & Office tab already has an office desktop, a tower, a silver laptop, a
retro beige computer and gaming chairs; Utilities has the telecom pieces (small cell, monopole,
ice bridge, fibre cabinet). Nothing here re-skins either: a new piece had to be different kit.
Pole-mounted radios fit Roads' poles through Core's `ICsmPoleFitted` / `CsmPoleFit` (as
`nema_enclosure` does), so the module still depends on Core only.

## Micafi, and the brand rule

Every brand is invented. The networking brand is **Micafi**, a member of the house Mica family
with Micaplex (the school clocks) and Micarolla (the ONT), with its own pixel mark
(`gen_technology_common.micafi_mark`): never a real maker's name, logo or livery, and never a
look-alike of one (no U with dots, no "UniFi"). Its products have their own names too: the Beam
building bridge, the Station CPE, the Sector antenna, the Link 60, the Control Hub, the Cloud
Gateway. The consoles are the Orbix, the Vertex, the Pocketeer handheld and an unnamed retro
16-bit console; the cable modem is a Corvane. A new product gets an invented name the same way.

## Desk devices sit at the bottom of their block

Every computer, monitor and peripheral is drawn standing at the bottom of its block, as the
iMac and the Mac keyboard already were (decision D8). A device placed on top of any full block
stands on its top: a plain block, a counter, another mod's desk. There is no desk-height drop
and no per-surface model (Furnishings' countertop appliances have one model per surface they
can rest on; these do not), which also halves the model count.

The trade-off: Furnishings' office desks are 12 px tall, so a device placed on one floats 4 px
above it. That was the user's choice; build desks from full blocks, as the demo world does.

Desk devices are `BlockDeskDeviceFactory` instances (a registry name and a box; they turn to the
placer and never stand on end), so the 43 of them need no class each.

## Click-cycled states in metadata

A click changes what something shows, through the blockstate swapping one texture, so nothing
ticks and there is no tile entity:

| Class | Property | Values | Metadata | States |
|---|---|---|---|---|
| `BlockDeskScreenFactory` (8 monitors, the open laptop) | `screen` | desktop, game, code, idle | facing bits 0-1, screen bits 2-3 | 16 |
| `BlockMicafiCeilingAp` (3 ceiling APs) | `ring` | blue, white, off, adopting | up/down bit 0 (from the face clicked), ring bits 1-2 | 8 |
| `BlockMicafiDevice` (wall and in-wall AP, outdoor mesh AP: `ring`; Beam, Station, Sector, dishes, Link 60: `link`) | `ring` or `link` (`MicafiLight`) | link: green, blue, white, off | facing bits 0-1, light bits 2-3 | 16; x3 pole fit x2 diagonal for `PoleFitted`; x6 aim for `Aimed` |

Each screen is a 64 px animated texture (`screen_<w|uw|tall>_<kind>`, drawn at the screen's
aspect and squeezed square); `adopting` flashes white and blue by `.mcmeta`. The enums' ordinals
are saved in the metadata: **only ever append, and never past four values** (two bits). Sneak
clicking is left alone, so a sneaking player can place against the device; the one exception is
the wall-arm Beam's aim (below), and only with an empty hand.

## The rack

### The scale

A 42U rack is about two metres tall and a block is a metre, so a rack is **two blocks**: 2 px of
plinth, 42U on y 2..30 and 2 px of roof, which makes **a U exactly 2/3 px** (three U to two
pixels). The cell line at y 16 then falls exactly between U21 and U22, so no unit is cut by it
except a unit spanning that line, which each cell draws its own part of. A rack is about 600 mm
wide and drawn a block wide, so a unit's 19-inch face is 12 px across (x 2..14); at a true
aspect a 42U rack would have been three blocks tall. Every frame uses the same 2/3 px, so a unit
looks the same in any rack (`RackFrame`: cells, U, first U, the units' face depth).

### The blocks

- `BlockServerRack`: the open two-post frame and the cabinets with a perforated door or a glass
  door. Placed as one piece into its block and the one above, as a door is (the two-block split
  is copied from Furnishings' `BlockResidentialTall`), and broken as one piece from either; only
  the lower cell (`upper` false) drops the rack and holds the tile entity. Stored: facing (bits
  0-1) and `upper` (bit 2).
- **Rows join by actual state.** Cabinets side by side, facing the same way at the same height,
  draw their side panels only where the row ends (`panel_w`, `panel_e`, named in model space: the
  model's west is the viewer's right facing the door). A fillable cabinet and a pre-filled one
  join each other, so a pre-filled rack sits in a row of filled ones unseen. The open frame has
  no panels and no join state (8 states; a cabinet 32).
- `BlockNetworkCabinet`: the 12U wall-mount cabinet, one block, hung on the wall behind it, glass
  door. Its 12U is y 4..12 at the same 2/3 px a U, but it is only 400 mm deep, so it takes only
  the units no deeper than 6.2 px (`RackFrame.WALL_12U`): the switches, patch panel, firewall,
  gateway, PDU, cable manager and blanking plate. A deeper unit is refused with a message.

### Mounting and removing

Right-click a rack with a unit (`ItemRackUnit`, one factory class, an instance per
`RackUnitType`) to mount it in the highest free span that fits. Sneak-right-click with an empty
hand takes out the unit **at the height clicked** (`RackFrame.uAt(cell * 16 + hitY * 16)`), so
any unit can be taken out, not only the last; right-click with an empty hand reports how full
the rack is. Any cell takes the click. Breaking the rack drops what it holds. A pre-filled rack
ignores clicks.

### Storage: `TileEntityServerRack`

The lowest cell's tile entity holds `RackContents`: each unit and the U its top is at, in the
order mounted. It is **one short key, `u`: a byte array of `[unit ordinal, top U]` pairs**, read
unsigned, absent when empty; units are numbered from U1 at the bottom, as a real rack's are.
Reading drops a pair naming a unit this version does not know, a top outside 1..42 or a span
overlapping one read before it, and keeps the rest. A rack can be filled by command:

```
/setblock X Y Z csm:server_rack_42u_glass 0
/setblock X Y+1 Z csm:server_rack_42u_glass 4
/blockdata X Y Z {u:[B;5b,42b,4b,41b,0b,40b,1b,39b]}
```

(patch panel at U42, 48-port switch at U41, a 1U server at U40, a 2U server at U39..38.) A change
is sent once with a block update that rebuilds both cells' sections; the chunk data carries it to
a client that loads the rack later. `RackContents` is immutable, so it can key a cache.

### Drawing: baked, never a renderer

`AbstractBlockRack` hands the contents to the model as the unlisted property **`CONTENTS`**, which
adds no states, through `getExtendedState` (read from the lowest cell, so the upper cell draws its
half; an empty rack while the tile entity is not there yet). `RackBakedModel` adds the units'
quads to the frame's own, **baked into the chunk mesh**: nothing about a unit runs per frame.
The approach is the mile markers' (`MileMarkerBakedModel`):

- `RackModels` wraps every world variant of every fillable rack **through
  `CsmBakedModelWrappers`**, never by walking the model registry, so a pack running VintageFix's
  dynamic resources draws the units too. It is registered after Core's glass split
  (`CsmGlassLayerModel`), so it wraps that model.
- **Quads are cached by (frame, contents, facing, cell)**, so a room of racks filled alike bakes
  them once. The cache is dropped at every bake, when the sheet's sprite may have moved.
- **Glass and mesh doors are drawn in Core's glass layer** (`CsmGlassLayer`, the rack implements
  `ICsmGlassFronted`; `technology/rack/door_mesh` is in its texture list): the cabinet's
  translucent pass draws only its door, and the units, being opaque, are added in every pass but
  the translucent one.
- `RackUnitBoxes` builds each unit's boxes and leaves out faces nothing can see: the back of a
  unit inside a cabinet, a top or bottom against the next unit when that one is at least as deep,
  and the face a box would have at the cell line.

### The unit sheet

Every unit's face, and the solid colour of its sides, is **one animated 128 px texture**
(`rack/rack_units`, 8 frames, 3 ticks a frame): a band 6 texels a U per unit, top to bottom in
catalogue order, and four colour swatches along the bottom 8 rows. One sprite on the atlas for
every unit there is; the LEDs blink because the sheet does. The band rows are written into
`RackUnitType.java` by the generator, so the two cannot drift.

**The sheet is full.** Fourteen units fill rows 0..119, the swatches the last eight: **the next unit needs a
second sheet** (a sheet index on `RackUnitType`, and a second sprite kept on the atlas by
`RackModels`). `RackUnitType` ordinals are saved in every rack: **units are only ever appended.**

### Pre-filled racks (no tile entity)

For data centres built by the dozens of rows (decision D9) there are seven **pre-filled racks**:
six cabinets (compute, mixed, storage, network, GPU, sparse) and an open-frame network core.
They are `BlockServerRack` with `fillable` false: **no tile entity, no extended state**, plain
baked models whose units the generator writes from **the same unit boxes** as `RackUnitBoxes`.
`RackUnitBoxesTest` reads the generated models and fails the build if a pre-filled rack and the
same units mounted by hand would be drawn differently. They join rows like any cabinet.

### Measured

`csm_bench.py`-style, in the dev client (worlds "Rack Check" and "Rack Perf", 2026-10-21): a
room of 40 filled racks, the same room of 40 pre-filled racks and the room empty each measured
about **0.6 ms renderWork**, indistinguishable. A filled rack is chunk geometry like any other block, so the choice between
fillable and pre-filled is about placing time and tile entities, not frame time.

## Cable trays: `BlockCableTray`

A wire basket and a cable ladder. Stored: the run's axis (along the placer's look) and whether
it hangs at the top of its block on rods to the ceiling or lies at the bottom (on a rack, say),
picked as a slab's half is (the underside of a block, or a side clicked in its upper half,
hangs it): 2 bits. Actual state: `north`, `east`, `south`, `west`, which way the tray reaches out
of its middle (64 states). Every part is drawn once for one direction and turned by a multipart
blockstate: the middle, an arm to the block edge, and a side rail where it does not reach.

**The joining rule.** A tray reaches sideways to a tray beside it at the same height **whose own
run points at it**, which makes a tee, a corner or a crossing; two parallel runs side by side do
not mat together. Along its own axis it reaches to any tray at the same height, and at an open
end too (a run that stops), unless it also reaches sideways: then that end is closed with a
rail, so a corner does not reach two pixels past itself. Ladder rungs are 4 px apart across block
lines: each arm carries half a rung at the block face, which meets the next block's half.

The trays are cutout, not mipped: a mipped mesh of one-texel wires fades out at a distance. The
ladder's black powder coat would make its item icon a black shape on the dark hotbar, so the
inventory model alone uses a charcoal (`ladder_icon`).

### Hanger extensions (D12)

`cable_tray_hanger` (`BlockCableTrayHanger`) is a block of threaded rod, stacked under a ceiling
so a hung tray (one at the top of its block) can sit one or more blocks lower. One block serves
both trays: they carry their rods at the same places (x 7.75 to 8.25, z 2 to 2.5, turned per
side). It draws a rod, with a coupling nut at its foot where it meets the rod below, on each side
where the tray beneath it has a rail: it looks down through the stack to the first block that is
not a hanger and reads that tray's actual state, so a straight run gets two rods, a tee one, a
corner the outside two, and the rods run on unbroken into the tray's own, which reach the top of
the tray's block. A crossing has no rails, and with no hung tray beneath the hanger uses its own
stored axis (the placer's look, or the tray's or hanger's below when placed on one): rods on the
two sides across it. Stored: the axis, one bit; 32 states; no tile entity, nothing ticks, no
collision (a selection box only). Its last English word, "Hanger", prices it as mounting hardware
(sheet metal and a fastener kit), which is what it is.

## Pole fitting

The seven outdoor radios (`BlockMicafiDevice.PoleFitted`: outdoor mesh AP, Beam on a pole
clamp, Station, Sector, both dishes, Link 60) have a clamp whose saddles reach 2 px into the
block behind, to a large pole's skin. `gen_pole_fit_models.fitted(model, delta)` writes the thin
and pedestal copies with those saddles moved back to each pole's skin, and the blockstate gains a
`polefit` variant picked in Java from the pole behind (`CsmPoleFit`). On a wall the clamp is
inside the wall. The wall-arm Beam (`micafi_beam_bridge`) is not pole-fitted.

## Eight ways round a pole (D10)

A pole device turns a full 45 degrees about the pole, so it points eight ways. The metadata was
already full (two bits of facing, two of light) and every placed device's metadata had to keep its
meaning, so **nothing more is stored**: the diagonal is in the *position*. A device pointing
north-east sits in the block diagonally north-east of the pole, stored facing **north** (the
cardinal 45 degrees anticlockwise of where it points). Its actual state reads `diagonal`: true
when nothing it could be mounted on is straight behind it (no pole, no full block) and a pole
stands at `pos - diagonalOf(facing)` (north-east for north, south-east for east, and so on). Each
stored facing has exactly one diagonal, so there is never a choice to make, and a device placed
before D10 (with a pole or a wall straight behind it) reads exactly as before. The pole fit is
resolved from the diagonal pole.

The blockstate cannot say "turn 45 degrees" with the vanilla `y` (90-degree steps), and the parts
are JSON elements already turned about z (the 16-gon dishes), which cannot take a second axis.
The road signs' answer is used: a Forge `transform` with a y rotation, proven by every eight-way
sign. `BlockMicafiDevice.PoleFitted` registers a state mapper that folds `diagonal` into the
facing the blockstate reads (`facing=northeast` and so on, `variantOf`), and the generator writes
four more facings, each `{"rotation": {"y": -compass}, "translation": ...}`: turned about the
block's middle, then moved so the model's pole axis (8, 24 in sixteenths, a block south) lands on
the diagonal pole (`diagonal_transform` in `gen_technology_wireless.py`). The **whole** model goes
round the pole's axis, clamp and saddles included, so each of the three fits still meets its
pole's skin; the device sits radially, inside its own block (the large dish no longer overhangs
it). 4 facings x 4 lights x 3 fits x 2 = 96 states; the mapper gives 96 model locations.

**Trap: a turned transform folds a JSON model's plain elements flat.** The first cut used the
ordinary models, and in game every diagonal device was wrecked: clamps and saddles as stepped
slabs square to the world, the face pictures folded into a cross, the cable a flat blue sheet,
holes where faces had collapsed. Not lighting: geometry. Vanilla's `FaceBakery.makeBakedQuad`
bakes each face, then, **only for an element with no `rotation`**, trusts it to be square to an
axis and rebuilds its four corners from their bounding box on the nearest axis (`applyFacing`).
A 90-degree `y` keeps that true; a 45-degree Forge transform (or the Beam head's 20-degree tilt)
does not, so every plain element's faces were squared back onto the axes. The 16-gon's turned
rectangles, which carry a rotation, came out right, which is why the discs half survived. A flat
road sign plate turned 45 degrees hides it (a face that is all one picture stays a plate).

The fix is a copy of each model in which every plain element carries a **zero turn**
(`"rotation": {"angle": 0, ...}`, a valid angle): an element with a rotation skips
`applyFacing`, so its quads are the truly turned faces, with the normals Forge fills from them.
The mapper also folds `diagonal` into the pole fit (`polefit=large_diagonal`, `thin_diagonal`,
`pedestal_diagonal`), whose variants draw `<model>_diagonal`, `<model>_thin_diagonal` and
`<model>_pedestal_diagonal` (`turnable` in the generator; the copies are otherwise identical, and
use the same textures and the same `led` key, so the atlas and the light swap are untouched). The
upright models keep no zero turns: without `applyFacing` a face turned by a 90-degree `y` keeps its
corners in turned order, which vanilla's own smooth lighting (used when Forge's light pipeline is
off, as under OptiFine) reads per corner. An OBJ copy was the other way, but bakes the same quads
and would have cost every face picture's UVs again; nothing in it is better here. Any JSON model
a transform turns off the axes needs the same treatment.

Placing: a block put against a face always lands straight out from it, so the diagonal block,
in the air beside a pole, could not be reached. `ItemBlockMicafiPoleDevice` handles a click on a
pole's side while looking from a diagonal that includes that side (from the south-east, clicking
the east or south face): the device goes into the diagonal block, if free, and
`getStateForPlacement` stores the facing (`aimFromYaw`, `storedFacing`). A click on a pole's side
from straight on now faces the device away from that side whatever the look. The selection box is
the north box carried round the pole the same way (`diagonalBox`), clamped to the block.

**Trap:** vanilla calls every face of every block solid unless told otherwise, and the devices
did not say: a device straight behind another (a cardinal one beside the diagonal block) counted
as "mounted on", and the diagonal never showed. `BlockMicafiDevice` now answers `UNDEFINED`, and
the test also wants a full block.

## The wall-arm Beam's aim (D11)

The Beam on its wall arm (`BlockMicafiDevice.Aimed`) keeps its wall facing; its head swivels on
the arm's ball joint. `MicafiAim`: centre, left, right (45 degrees, as the installer at the wall
looking out sees it), and each tipped down 20 degrees: six, cycled by sneak-clicking with an
empty hand (holding something, a sneak-click still places it); a plain click still cycles the
link light. Ordinals are saved: append only.

The metadata is full, so the aim lives on `TileEntityMicafiAim` (`a`, one byte; it does not tick,
has no renderer, and its `getBakedModelKey` is the aim, so only an aim change rebuilds the chunk),
read into the actual state. The model is split: the arm and wall plate are the block's model, the
head (disc, hub, and its cable as far as the swivel) a Forge **submodel** in the `aim` variants,
turned by a transform about the joint (`BEAM_PIVOT`, 8, 9.5, 8: the middle of the block across, so
yaw needs no translation; the tilt does). Two Forge facts make this work: a submodel's transform is
composed with the variant's `y` facing (`ModelStateComposition`), and the variant's textures (the
link light) are synced into its submodels. A rotation list is applied last to first, so
`[{"y": yaw}, {"x": tilt}]` tips the head, then swings it. 4 x 4 x 6 = 96 states. The five
aims off centre draw `micafi_beam_bridge_head_turned`, the head with the zero turns of D10's trap
(the centre aim keeps the plain head): without them the turned head baked as flat sheets too.

Only the Beam on its arm is aimed. The wall AP and the in-wall AP are flush on the wall, with no
swivel to turn on; the pole-fitted pieces on a wall have their clamp in the wall.

## Round faces and `model_depth.py`

A round face (an AP's ring, a dish's radome, the Beam's disc) is a **square plane whose picture
is cut to the part's outline**, so a disc costs one quad. The prism behind it (octagons from
`lsc._octagon`, and since 2026-10-21 16-gons from `round_z` for the dishes and the Beam: eight
rectangles turned 0, 22.5, -22.5 and 45 degrees, each giving two of the sixteen sides) is drawn
**without its own end cap there**. That rule matters: with a cap coplanar with the picture,
`model_depth.py` separates the two and moves the cap over the picture, and the face reads as a
blank octagon. The picture is cut to the same n-gon as the prism (`ngon` in the wireless
generator), not a circle, so its edge meets the sides. Small discs (the APs) stay octagons.

## Fabricator words come from the English display name

`CsmFabricatorCosts` prices both new tabs by words (`techComputersCost`, `techNetworkingCost`,
mirrored in `audit_fabricator_costs.py`): a rig is a board, sheet metal and wiring; a screen adds
an LED module; peripherals are a board and a shell; a mouse pad or desk mat is cloth; a rack or
cabinet is steel and bolts, a pre-filled one ("Pre-Filled" in its name) the servers in it
besides; jacks, trays and patch hardware are passive metalwork; outdoor radio gear adds a
weatherproof housing. The words are read from the **English display name**
(`CsmBlockDisplayNames`, which lower-cases it and strips anything in parentheses), not the
registry name.

**The trap:** the mounting-hardware rule (`MOUNT_NOUNS`: mount, bracket, plate, arm, cap, top,
base, ...) runs before any tab's rule, on the name's last word. A display name ending in one of
those words is priced as a bracket. The names were written so that their last word is the thing
itself: the wall jacks are "Network Wall Jack (1 Port)", not "... Wall Plate"; the dual monitor is
"Dual 24" Monitors on a Desk Stand", not "... on an Arm"; a qualifier goes in parentheses, where it
is ignored. Keep to that when naming a new piece.

## Generators

All five share `gen_technology_common.py` (the Life Safety catalogue pointed at the Technology
tree, the `Pic` face-picture class, the element helper `E`, shared swatches, the Micafi mark,
`model_depth.py` at the end of every write) and each has `--check` and `--fragments`:

| Script | Writes |
|---|---|
| `gen_technology_computers.py` | the Computers & Peripherals tab: models, the 256 px shelf-packed keyboard sheet (`BANDS`), the animated screens, the console sprites |
| `gen_technology_networking.py` | the desktop networking gear, wall jacks, floor box, both cable trays and the tray hanger |
| `gen_technology_rack.py` | the racks, the wall cabinet, the unit sheet and item models, the pre-filled racks' models, `RackUnitType.java` and `RackFrame.java`, the units' recipes (conditional on `csm_technology`) |
| `gen_technology_wireless.py` | the Micafi access points and outdoor radios, their light textures, pole fits, the diagonal facings' transforms and the Beam's aimed head |
| `gen_technology_school.py` | the school time and PA set (SCHOOL_TIME_AND_PA.md) |

`gen_pole_fit_models.py --check` covers the pole-fit copies too.

## Weight

Counted from the block classes: the Computers & Peripherals tab is 300 states (43 desk devices
at 4, 8 screens at 16); Networking & IT about 1,300 (12 desk devices at 4, 2 trays at 64, the
tray hanger at 32, 3 ceiling APs at 8, 2 wall devices at 16, the aimed Beam at 96, 7 pole-fitted
devices at 96, 8 cabinets at 32, 2 open frames at 8, the wall cabinet at 4). Phase 7 added no
textures. The `blocks/technology` textures are 1.46 Mpx of the block
atlas, which stood at 96.2% of 8192 x 4096 after the expansion (`atlas_budget.py`, 2026-10-21).

## The demo world

`dev-env-utils/scripts/build_technology_demo.py` builds it through the MCMCP orchestrator by
instance id (default `run-f2eb58`) in a flat creative world named "Technology Demo" (it refuses
any other), loaded in a dev client with every module: an office of twelve workstations on
full-block desks with the whole Computers tab and a gaming corner; a server room with two rows of
fillable cabinets filled through the `u` byte array, an open frame, two rows of pre-filled racks,
a basket and a ladder tray hung over the rows (a corner and a tee; the ladder a block lower on
threaded rod hangers), ceiling APs; a network
closet with the wall cabinet, the desktop gear, the wall APs and jacks; and a roof of Roads poles
carrying every outdoor radio, aimed at a second building that carries the far Beam and Link 60,
with two more sector antennas turned 45 degrees either way round the sector's pole and a second
wall-arm Beam aimed left and down.
It checks both new tabs' registration lists against what it placed and puts the rest on signed
plinths (none, at the first run).

## Left out, and what the module could grow into

- **Linked networks** (PoE reachability, bridge pairs that show signal, an AP that knows its
  switch): rejected for now (D3). Everything is a look; a network is a link map in saved data,
  as nurse call is, if it is ever asked for.
- **Patch cables** between rack ports: expensive geometry for a look, revisit only with numbers.
- **Opening rack doors**: a door is part of the baked frame; an open pose would be a state on the
  cabinet (x2 states) and a second door model.
- **Glass and mesh door presets** for the pre-filled racks: they are all perforated-door
  cabinets; a glass-door copy of each preset is one more model each.
- **A gaming rig as a working computer**, like the iMac's notepad: not attempted.
- Growth: a second unit sheet (more units: tape library, blade chassis, KVM over IP), a raised
  floor tile with a perforated vent, a hot-aisle containment door and roof, overhead busway for
  the racks, a NOC video wall, a fibre patch frame.

Open items and checks still to do are in `agent_progress/UNFINISHED_ITEMS.md`, D.29.
