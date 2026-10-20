# The Healthcare Module

CSM: Healthcare (`csm_healthcare`, tree `modules/healthcare`, Java package `healthcare`) is the
optional module for a hospital's fit-out: patient rooms and wards, the ICU, the emergency
department, nursing stations, surgery support, clinics, dialysis and infusion, imaging, the lab,
the pharmacy, the morgue, maternity and the NICU, rehabilitation, the waiting room, teaching, the
paediatric playroom, wayfinding and the building systems a hospital runs on (nurse call, code
blue, medical gas, emergency power, infant security). Like every module it pins Core to its own
exact version and registers nothing itself. It is 162 blocks (158 in two tabs and four hidden
cells), 3,332 block states.

It was asked for in GitHub #293, for the hospitals being built on the Alto server: they were
furnished from the Hospital Mod and its Theatre Pack, which cover the operating room and little
else, so a corridor read as white walls and solid white doors and a ward as beds in a box. The
hospitals are placed by a generator, server side, by the hundred (the hospital builder, a
separate Claude session working on Alto through MCMCP), which set the module's first rule: every
block can be set with `/setblock` and `/blockdata` alone, with its facing in metadata and no
screen needed, and every block's metadata and NBT is written down (Placing by command, below).

**It depends on Core only.** Nothing in the module names another module's Java: a hospital
without traffic signals, parks or vehicles should not have to install them, and a server without
hospitals can leave the jar out. Where a hospital needs something another module already makes,
that module made it (decision table, below): the cross-corridor smoke doors, hospital doors,
sliding doors, accent panels, wayfinding stripes, room signs and nurse station screens are
Building's (DOORS.md, INTERIOR_FINISHES.md), the helipad is Transit's airside (TRANSIT_SYSTEM.md,
The helipad), the pews, altar table and commercial laundry are Furnishings', the decon sink and
eyewash station are Life Safety's, and plantable planters are Parks' (PARKS_GREENERY_SYSTEM.md).
The generators may import other modules' Python helpers (the bed, curtain, office and Life Safety
font code); the dependency rule is about Java.

## Two tabs

| Tab | Id | Order | Holds |
|---|---|---|---|
| **CSM: Healthcare - Patient Care** | `tabhealthcarepatientcare` | 29 | Beds and everything round a bed (headwalls, monitors, IV poles, curtains, room furniture, nurse call devices and dome lights), corridor rails, wheelchairs and lifts, the exam room, dental and eye exam chairs, dialysis and infusion chairs, maternity, rehabilitation, the waiting room and its queue, the playroom, the transport stretchers, and the wayfinding panel (65) |
| **CSM: Healthcare - Clinical** | `tabhealthcareclinical` | 30 | Nursing (the master panel, the nurses' station, medication, dispensers, carts and bins), the ICU and ED, surgery support, the dialysis and endoscopy equipment, imaging and the portable ultrasound cart, the lab, the pharmacy, the morgue, teaching models, the building systems, and the operating room (93) |
| Hidden | `tabhealthcarehidden` | -11 | `hospital_bed_foot`, `cubicle_curtain`, `ceiling_boom_column`, `healthcare_unit_part`: cells placed by another block, never by hand (4) |

About 160 blocks are too many for one tab's page, and the split follows who reaches for them: a
builder furnishing rooms, or one fitting out a department's equipment. New tabs at the next free
order values register after everything that exists, so no existing block moved.

The Fabricator prices both tabs by what a piece is made of (`HealthcareFabricatorRules`, mirrored
by `audit_fabricator_costs.py`): a bed is a frame, a motor and its controls; a piece with a screen
or that calls someone is electronics; a headwall sheet metal and wiring; the CT and MRI several of
each; a curtain cloth; a sign a sign blank; the rest sheet metal and fittings.

## Performance first

The module's rule, kept in every phase: **baked models, nothing ticking, and a renderer only where
nothing else will do.**

- **Two renderers in the whole module**, the ED tracking board and the now-serving display. Both
  are built like the Transit flight board's: every piece of text is a display list shared by every
  board (`CsmSharedDisplayLists`), keyed by a bounded value, with the font atlas, colours, depth
  mask and lightmap set outside the lists every frame ("Display lists: one texture, no cached
  state" in TRAFFIC_SIGNAL_SYSTEM.md), and a render range (32 blocks for the board, 48 for the
  display). The wayfinding panel, which on Transit has a renderer, is baked here.
- **Every screen is an animated texture** (`.mcmeta`): monitors, pumps, analysers, the dialysis
  and endoscopy screens, the kiosk, the code blue strobe, the gas alarm's flashing lamp, the
  infant panel's ALARM band. A screen that is on costs what one that is off costs.
- **Nothing ticks.** The code blue chime and the gas alarm's buzzer repeat from a scheduled block
  tick that each sound schedules again, only while the alarm lasts. The ED board's page is looked
  up once a second on the client, not every frame.
- **Links are event-driven and live in saved data**: nurse call (`csm_healthcare_nursecall`),
  the queues (`csm_healthcare_queue`) and code blue (`csm_healthcare_codeblue`), each per
  dimension. A press, a cancel, a link or a reset updates what it reaches once; nothing polls. A
  block in a chunk that is not loaded is left alone and catches up when it next changes (a display
  pulls its number when its chunk loads).
- **Tile entities are few and data-only**: the master panel's lamp byte, the ED board's two
  settings, the now-serving display's number and desk, and the wayfinding cell's sign and place.
  Everything else is metadata and actual state.
- **Every block uses `CsmBlockStateContainer`**, and every property was weighed against its state
  product. The largest are the station counter with monitors (1,024: the master panel's six room
  lamps over the counter's facings and ends, below), the curtain panel (288: cloth x drawn x its
  shape), the master panel
  (256: six lamp bits and the alarm, actual state over four facings), the curtain track (162) and
  the shared bed foot (144). Most blocks are 4 to 16.
- **Shared swatches and `model_depth.py`.** Every generator shares the palette's swatch textures
  by path and runs `model_depth.py` over what it writes (coplanar faces separated, hidden faces
  pruned).

Measured figures are in Weight, below.

---

## The bed: a head and a shared foot

A hospital bed is one block wide and two long, as a vanilla bed is: a **head** cell, one registry
name per bed (`hospital_bed_white`, `_blue`, `_green`, `_mauve`, `_icu`, `_bariatric`, `_labor`),
and a **foot** cell every bed shares (`hospital_bed_foot`, hidden). The head carries the item and
the bed's whole state in its metadata (facing, rails up, raised); the foot stores only its facing
and reads which bed it is, its rails and its height from the head behind it. The two halves can
never disagree, and a generator places them with `/setblock` in either order: nothing removes a
half whose partner is missing, so a half-built bed waits for its other half. Breaking either
breaks both, and only the head drops.


The facing is CSM's, the way the foot end faces (the same as the headwall over it), not vanilla's
head direction. The head goes against the wall in the wall-face cell at floor level, the foot at
`head.offset(facing)`, and the patient headwall in the wall-face cells above the head (y+1, its
upper panel y+2), drawn at the back of its cell. The bed is drawn whole in one frame by
`gen_healthcare_wards.py` and cut at the block line into one model a cell; `HospitalBedVariant`'s
order is the foot's `variant` property's, so a bed is only ever appended (the labour bed made it
seven values, 112 foot states; the stretchers nine, 144).

**Transport stretchers** (`hospital_stretcher`, `hospital_stretcher_trauma`) are beds in all but
their drawing: the same `BlockHospitalBed` head and shared foot, the same metadata, clicks and
the bed's two pad heights (so the seat, and the simulation mannequin laid on one, rest on the pad
as on a bed). The geometry is `stretcher_elements` in `gen_healthcare_wards.py`: a narrow litter
on a shrouded base with two lift columns and four large casters, a pad with its back raised and a
pillow, one long side rail a side (up, or folded down below the litter), push handles at the
head, a bumper bar at the foot, and an IV pole standing in its socket at the head on the
patient's right with a bag on its hook. The rails are boxes, not the bed's cutout texture: a
face longer than a block is cut into two cells, and a texture fitted to the whole rail cannot be
fitted to both halves. The trauma stretcher is a texture swap only, the ED's black pad with no
sheet. They are for pre-op, PACU, the ED and urgent care, in place of Life Safety's
`ems_stretcher`, which reads as an ambulance's.

Clicks: a long side raises or lowers the rails; a sneak-click raises or lowers the bed; anywhere
else sits the player up in it on Core's `EntityCsmSeat`, facing the foot. There is no sleeping in a
hospital bed: the seat's pose is sitting, and sleeping stays with the Residential beds.

## Cubicle curtains: the track fills its curtain

The curtain hangs from a **ceiling track** (`cubicle_curtain_track`) that goes in the
ceiling-tile layer in place of a tile: its cell draws a ceiling tile across its bottom and the
track just below it, in the room. Tracks side by side join by themselves (straight, inside and
outside corners, tees, crosses, an end cap where a run stops); only a lone track stores an axis.

Under each track cell hangs a column of **curtain panels** (`cubicle_curtain`, hidden), one block
for every cloth: the metadata holds the cloth (mint, blue check, beige, white) and how it is drawn
(closed, open, bunched); its shape, the white mesh band on the top panel and the hem on the bottom
one are worked out from the neighbours. Nothing collides. The track fills: a track placed by hand
fills its own column, matching the column beside it or down to one block above the floor;
`/csmhealthcare curtain fill <track> [drop|auto] [pattern]` fills a whole run. A click opens or
closes the whole curtain, bunched at the run's end nearest the click; a sneak-click changes the
whole curtain's cloth; breaking a track takes its column. Every operation is one bounded walk
(`CubicleCurtains`), done on the event that asks for it.

Hospitals here have eight clear blocks to the ceiling-tile layer, so a curtain's drop is anything
from 1 to 15 panels, and `auto` finds the floor.

## Ceiling-hung pieces: a head and a column

The ICU pendant, the surgical equipment boom, the trauma bay exam light and the ceiling X-ray tube
hang from the ceiling on a drop column: a **head** block with its own metadata (facing, screen or
light) and above it a stack of `ceiling_boom_column` cells (hidden) up to the ceiling, the top one
carrying the ceiling flange (actual state). Nothing is stored about the stack: a column is a
column, and the head is found by walking down it. A generator places the head and the column
cells with `/setblock`, or the head and `/csmhealthcare boom fill <head>`, which fills the air
above it up to the first block (at most eight cells). By hand, the item used on the underside of a
ceiling puts the head at its working height above the floor (one block up for the pendant and the
boom, two for the lights) and fills the column. Breaking the head or any column cell takes down
the whole piece, and only the head drops. The column hangs from the cell below the tile layer, so
no ceiling tile is replaced.

The ceiling patient lift is not one of these: it sits in the ceiling-tile layer and draws its own
tile, like the curtain track, with the motor and spreader bar hanging into the cell below.

## Units bigger than a pair: a master and parts

Two-cell pieces (wide tables, tall cabinets, the headwall, the status board) are
`BlockHealthcarePair`: both cells are the same block, the second marked by a flag, wide to the
viewer's right (`first.offset(facing.rotateYCCW())`) or tall above.

The CT and MRI scanners are bigger, so they are a `BlockHealthcareUnit` **master** that draws the
whole unit (one JSON model reaching up to a block past its cell each way, the JSON limit, the
master centred in x) and invisible `healthcare_unit_part` cells (hidden) filling the rest of the
footprint, so the unit is solid and can be clicked anywhere. This is Roads' utility box pattern,
which Healthcare may not name. A part's metadata is the `EnumFacing` index of the step towards the
master, a path through the footprint, so a part finds its master in a few lookups and one whose
master has gone knows it is an orphan. The master fills every free cell of its footprint in
`onBlockAdded`, so a `/setblock` of the master alone places the unit; cells that were not free
are left, and `/csmhealthcare unit fill <master>` fills them later. By hand, the item places the
unit only when every cell is free, and refuses with "Not enough room -- blocked at x, y, z" on
the action bar. Breaking the master or any part takes the unit; only the master drops.

The C-arm is a tall pair whose C reaches into the cell in front with no collision, so it can be
wheeled over a table; nothing reserves the table's cells.

## Joining runs

The nurses' station counters (`BlockNurseStation`), the lab benches (`BlockLabBench`) and the run
pieces (`BlockHealthcareRun`: beam seating, parallel bars, lecture seating) draw their ends only
where a run stops, so pieces side by side read as one. The ends are actual state, never stored,
named in model space as the corridor rail's are: `east` is the end at the left of someone facing
the piece. A run piece joins another of its `group` facing the same way (any colour of beam
seating joins any other); a station piece joins any station piece; the station's corner turns an
outside corner. Lab benches put the worktop at the top of the cell, so a bench-top instrument goes
in the cell above with no height offset, and the reagent shelf is drawn over the back of that
cell. Lecture seating adds a stored `raised` flag: a half-block riser, so rows alternate floor and
riser and climb a block every two rows, and its collision leaves the front of the cell free so a
sitter gets off into the legroom.

## Nurse call

A patient's **devices** (pillow speaker, wall call station, bathroom pull cord) call through their
room's **dome light** over the corridor door, and dome lights are the rooms of a nurses' station
**master panel**, six rooms a panel, numbered in the order they were linked (more rooms, more
panels side by side). A device can be linked straight to a panel when it is a room of its own. The
links and the calls live in `NurseCallNetwork` (saved data); a press, a cancel or a link updates
the dome light and the panel lamp it reaches once.

- A device click places a call or cancels its own; a call gives redstone 15 from the device while
  it lasts, so it reaches a chime, a lamp or another mod. A `/setblock` with the calling bit
  starts the call exactly as a click does (dome light, master panel lamp and chime, station
  monitors), and one with the bit clear ends it, whether the cell held air or the same device
  before: a device set over itself is heard by the module's world listener
  (`HealthcareStateChanges`), since the game calls no block method when only the metadata
  changes. A device that calls before it is linked keeps its call, and its room lights when the
  link is made.
- The dome light keeps Lighting's metadata: in states 0 and 1 it follows the room's calls or a
  redstone signal; a generator can force it with 2 (dark) or 3 (lit). A click on it cancels the
  room's calls, as a nurse does on arriving. It takes redstone and gives none, so a lamp that
  powered the dust powering it cannot hold itself on.
- The dome is all lens: an amber dome standing 6 px off its white bezel, so its sides show from down a
  corridor. Lit, the lens is a saturated amber drawn unshaded (`"shade": false`, so it is as bright
  from every side), with an OptiFine `_e` companion, and the block gives light 9; dark, it is a
  greyed amber that reads as off in daylight. Only the texture changes with the state.
- The master panel's lamps are its tile entity's `rooms` byte, read into the actual state for the
  baked model; a new call chimes (vanilla's note chime), and while any room calls the panel gives
  redstone and its alarm lamp lights. A click lists the calling rooms.
- Linking: `/csmhealthcare nursecall link <parent> <device>` (dome or device to a panel, device
  to a dome), or by hand: sneak-click a panel or dome to pick it, then sneak-click each dome or
  device (`NurseCallLinking`, kept per player until they pick another or log out).

### The nurses' station monitors

The counter with monitors (`nurse_station_counter_monitors`, `BlockNurseStationMonitors`) shows
the calls of **one master panel**: while any of the panel's rooms calls, both its charting screens
give way to a CALL screen, a red band flashing over the panel's room numbers (1-3 on the nurse's
left screen, 4-6 on the right), and each calling room's number is lit amber. With no call the
screens chart again. The numbers are the panel's, so the nurse reads the same room at the counter
and on the wall.

- **Linking.** `/csmhealthcare nursecall link <panel x y z> <monitors x y z>`, or by hand:
  sneak-click the panel to pick it, then sneak-click the counter. Any number of counters can show
  one panel; a counter shows one (linking it again moves it), and it takes no room on the panel.
  `unlink`, `status` (on the counter: its panel and the calling rooms; on a panel: the counters
  showing it) and `prune` take counters too. A click on a linked counter lists the calling rooms,
  as the panel's does.
- **Cheap.** It is the master panel's own mechanism: a data-only tile entity with the panel's
  `rooms` byte (`TileEntityNurseStationMonitor`), read into the actual state (`room1`..`room6`)
  for the baked multipart, and set by `NurseCallNetwork` when the panel is brought up to date, on
  a call starting or ending or a link. The CALL band flashes by an animated texture
  (`station_call`, two 32 px frames, its top half the left screen and the bottom half the right),
  and each lit number is a quad over its cell from `station_call_lit`. Nothing ticks and nothing
  is rendered by a renderer. The counters' links live in the same saved data as the rest
  (`monitors`: panel -> counters), and a counter's lamps are worked out from the network, so a
  counter is kept up to date even while its panel's chunk is not loaded; one in a chunk that is
  not loaded catches up when it is next refreshed, as a panel does (and is refreshed when it is
  placed).
- **States.** The room lamps multiply the counter's 16 states (4 facings x its two ends) by 64,
  to **1,024**, the module's largest; they are plain state objects with no neighbour table, and
  the blockstate is a multipart, baked once for all of them. The plain counter and the corner are
  unchanged (the corner has no monitors).

## The ED tracking board

`ed_tracking_board` is a wall monitor listing the department's beds, seven a board: bed, patient
initials, status, time in the department and nurse, under a title and the clock. The list is
`EdTrackingSchedule`, a pure function of the world's time of day and the board's NBT (`beds`,
1-40, default 24; `seed`), the way the Transit flight boards' schedule is: it saves nothing, sends
nothing and costs the server nothing. Each bed runs a visit every 480 game minutes, offset by a
hash of the bed and the seed: 200 to 439 minutes through TRIAGE, WAITING, IN ROOM, LAB or IMAGING,
IN ROOM and ADMIT or DISCHARGE, then CLEANING for 20 and OPEN. Patients are invented initials from
a table of 64 pairs, nurses one of 12 per four beds a shift; nothing names a real person.

Boards facing the same way side by side and stacked are a bank and page on through the beds, as
the flight boards do; nothing links them. The page is looked up from where the board stands in
its bank once a second, not every frame. The screen's place and bands are the generator's
(`ED_W`, `ED_H`, `ED_SCREEN_Z`, `ED_HEADER`, `ED_COLHEAD`, `ED_PITCH`): change one, change both.

## The waiting room queue

A **queue** belongs to a controller, the check-in kiosk or a desk's call console, and holds one
number (0-999, 0 showing dashes). A click on the controller, or `/csmhealthcare queue next`, calls
the next number; `queue set` sets it. A **now-serving display** links to one controller
(`queue link <controller> <display>`; linking again moves it) and shows NOW SERVING, the number and
DESK n. Numbers and links are in `QueueNetwork` (saved data), pushed to loaded displays on a change
and pulled by a display when its chunk loads, so a display placed by a generator can also carry its
own `number` and `desk` (0-99, 0 none) by `/blockdata`. The renderer keys its display lists by the
number and the desk, so a waiting room of displays shares a handful of lists.

A controller placed by `/setblock` starts at 0, so its first click calls 1.

## Code blue

The **button** (`code_blue_button`) is the blue wall station a nurse presses: it lights, gives
redstone 15 (strongly into its wall, as a vanilla button does, so a lamp or annunciator on the far
side answers too) and activates every **annunciator** it is linked to. The annunciator
(`code_blue_annunciator`) is active while a linked button is active or while it is powered: its
strobe is an animated lens swapped in by the active state, it gives light 6, and it chimes as it
goes active and every 70 ticks while it stays active, from a scheduled tick. The code stays on
until it is reset, by a sneak-click on the button or on any linked annunciator, or
`/csmhealthcare codeblue reset`: a reset clears every active button linked to it and refreshes all
of their annunciators. Links are many to many (a button can call a floor's annunciators, an
annunciator answer a unit's buttons) and made by command only, in `CodeBlueNetwork` (saved data).
The annunciator gives no redstone, so it cannot hold itself on. Redstone works with no links at
all.

The **medical gas master alarm** goes to alarm on redstone (the oxygen LOW lamp flashing, an
animated texture) and buzzes every 50 ticks until silenced by a click. Its metadata is the lit
pieces' `facing * 4 + state`, because a three-bit wall facing leaves no room for two flags. The
**infant security panel** links by redstone only: its input is the wall block behind it powered
(read from that block alone), its output a weak 15 on every other side, and its alarm latches until
a click with the input off. A command link was not worth it for a one-way alarm, and the output
never goes into its own wall, so it cannot hold itself in alarm.

The three sounds are synthesised by `gen_healthcare_sounds.py` (code blue chime 2.25 s, medical gas
alarm 2.0 s, infant security alarm 1.5 s), the module's own `sounds.json` and `HealthcareSounds`.

## The wayfinding panel: a drop-in for Transit's

`hospital_wayfinding_panel` takes **exactly** what Transit's `airport_wayfinding_panel` takes, so
the hospital builder, which already wrote airport panels, switches by changing the block name:
the same metadata (facing S 0, W 1, N 2, E 3, towards the reader), the same tile entity NBT keys
and meanings (`l1`, `l2`, `p`, `a`, `s`, `d`), a compound without `p` leaving the sign alone, the
same 16 x 6 limit, the same rule that the bottom-left cell as read is the controller and that a
`/blockdata` of any cell writes the whole panel, and rods from the top row up to the first block
above. Its values differ where a hospital needs them: department colour schemes (Transit's four
kept with their colours), hospital pictograms (Transit's `restroom` and `exit` ids kept, its
airport and transport ones reading as none), and an unknown scheme reading as neutral where
Transit's reads as airport. Every enum is saved by its id, never its ordinal. It is its own Java:
Healthcare may not name Transit.

**Baked, not rendered.** Each cell's model is a charcoal slab with a frame on the panel's outer
edges (four actual-state booleans, 64 states), and `HospitalWayfindingBakedModel` adds the cell's
share of the background, pictogram, arrow, legend and rods from one 512 px sheet
(`wayfinding_sheet.png`: the committed highway font's Series D glyphs, 14 pictograms, two arrows
and a white swatch), clipped to the cell's square, front and back, coloured in the vertices and
unshaded. The layout is worked out once across the panel in the reader's space
(`HospitalWayfindingLayout`, Transit's large hanging sign's proportions) and clipped per cell, so
the cells show one sign with no seams. Each cell needs only its own tile entity: its place in its
panel (`pc`, `pr`, `pw`, `ph`) is worked out on the server whenever a cell is added or removed
(`HospitalWayfindingSign.refresh`) and synced, and a cell outside its controller's rectangle (a
ragged build) is drawn plain. Quads are cached by facing and face (up to 2,048), so a ward of
identical signs bakes each cell's share once. The panel gives light 8 and is CUTOUT_MIPPED.

**Every printable ASCII character draws.** The committed Series D font has no percent, semicolon,
angle or square brackets, backslash, caret, underscore, backtick, braces, bar or tilde; those
fourteen are drawn by `gen_healthcare_systems.py` (`DRAWN`) in the font's own stem width and full
stop, polylines with round joints in units of the cap height, and go onto the sheet and into the
glyph table like the font's own. Brackets, braces and the bar reach from the cap line to below the
baseline, as the font's parentheses do; the angle brackets, caret and tilde sit on the axis at
half the cap height.

By hand, without a GUI: a click steps through presets (INFORMATION, EMERGENCY, IMAGING, SURGERY,
MATERNITY, OUTPATIENT, WARDS, LABORATORY, PHARMACY, ADMINISTRATION, ELEVATORS, STAIRS, RESTROOMS,
CAFE, CHAPEL, PARKING, EXIT) keeping the arrow; a sneak-click steps the arrow. A cell placed fresh,
by any placement, adopts a joined neighbour's sign.

## The operating room

The OR set (`gen_healthcare_surgery.py`) was added after the module shipped, when the hospital
builder asked for an operating room with the module's look and placing rules rather than the
third-party theatre pack's: nothing in it is taken from that pack, neither a name nor a model.
Every piece is one of the module's existing kinds, so it places, faces and behaves as its
neighbours do, and nothing in it ticks or has a renderer:

| Block | Kind | What it does |
|---|---|---|
| `or_table` | wide pair (`BlockHealthcarePair`), a seat | The operating table: a column on a base plate, four pad sections on a steel frame with side rails, the headrest on the first cell, an arm board out to the front at the shoulders and the control pendant on the far rail. A click lies the player on it, on Core's seat at the pad's top, as the bed's sit-up does |
| `or_anaesthesia_machine` | screen (`BlockHealthcareScreen`) | A drawer cart with the gas panel (oxygen, nitrous oxide and air flowmeters), two vaporizers in their agents' colours, the bellows and absorber, cylinders on the back and the breathing circuit; its workstation screen on top is an animated texture (pressure, flow and CO2 traces), switched by a click |
| `or_surgical_light` | ceiling-hung lit head (`BlockCeilingHeadLight`, as the trauma bay light) | A hub on the drop column with an arm out each side to a round lamp head, lenses down, a sterile handle in each. Lighting's metadata; light 15 when lit; the lenses are the trauma light's own textures |
| `or_scrub_sink` | run (`BlockHealthcareRun`, group `or_scrub_sink`) | One bay a block, each with its gooseneck tap, sensor and knee panel; bays side by side are one deep trough under one splashback, with legs and side panels only where the run stops. It stands against a wall |
| `or_back_table` | wide pair | The instrument back table, draped, the set laid out on it, a lower shelf |
| `or_mayo_stand` | floor piece, no collision | A U base open to the front so it rolls over the table, a column and the draped tray with its instruments |
| `or_kick_bucket` | floor piece | A stainless bucket in a ring frame on casters |
| `or_supply_cabinet` | tall pair | Drawers below, a glazed door above showing the sterile packs on its shelves |
| `or_drape_trolley` | floor piece | A wire cart with folded drapes and gowns |

They share the wards' and the clinical generator's swatches (stainless, the frame grey, the warm
white, rubber, the monitor body, the drawer banks, the exam light's lenses, the gas cylinder,
the wire shelf, the linen stack); the set draws only its pad, drape and instrument layout, the
machine's front and screen, the bellows and the cabinet glazing, seven small textures. States:
76 for all nine (the light 16, the sink 16, the pairs and the screen 8 each, the rest 4). The
three powered pieces (table, machine, light) cost sheet metal, a control board and a wiring
harness at the Fabricator; the rest sheet metal and fittings.

## The simulation mannequin and what it rests on

`simulation_mannequin` is a block of its own set in the cell **above** what it lies on, drawn sunk
down onto that surface, so the bed or table under it is not replaced. What it lies on is read from
the cell below as actual state (`rest`): a bed's head or foot cell (on the mattress at the bed's
height, its back raised 22.5 degrees with the head section, drawn one cell further back over a
foot so it can be set over either cell), the exam table's first cell (on the pad, back raised),
any other cell of an exam, autopsy or dissection table (flat, its middle on the line between the
table's two cells), or anything else, an operating table from another mod included (flat on its
own floor). Its facing is the way its feet point: over a bed, the bed's facing; over a table's
first cell, the table's `facing.rotateYCCW()`. Placed by hand it takes that facing from what it is
put on. It has no collision.

---

## Commands

`/csmhealthcare` (op level 2), for wiring a hospital built by a generator:

| Command | What it does |
|---|---|
| `nursecall link <parent x y z> <device x y z>` | Links a dome light or device to a master panel, a device to a dome light, or a nurses' station counter with monitors to a master panel (whose calls it then shows) |
| `nursecall unlink <parent> <device>` | Undoes one link |
| `nursecall status <x y z>` | What a panel, dome, device or station counter is linked to, and which are calling |
| `nursecall prune` | Drops the links of blocks that are gone (loaded chunks only) |
| `curtain fill <track x y z> [drop\|auto] [pattern]` | Hangs the curtain under the whole run: `drop` panels a column (1-15) or `auto`, down to one above the floor; pattern `mint`, `blue_check`, `beige`, `white` |
| `curtain open\|close <x y z>` | Opens (bunched at the run's end nearest that position) or closes the curtain at a track or panel |
| `curtain pattern <x y z> <pattern>` | Changes a curtain's cloth |
| `curtain clear <track x y z>` | Takes down the curtain under a run |
| `boom fill <head x y z>` | Fills the drop column above a ceiling-hung head up to the ceiling (at most 8 cells) |
| `boom clear <head x y z>` | Takes the column down, leaving the head |
| `unit fill <master x y z>` | Fills a CT's or MRI's free cells with its parts |
| `queue link\|unlink <controller x y z> <display x y z>` | Links a now-serving display to a kiosk or call console, or undoes it |
| `queue next <x y z>` | Calls the next number at a controller, or a linked display's controller |
| `queue set <x y z> <number>` | Sets the number |
| `queue status <x y z>` / `queue prune` | A controller's number and displays, or a display's controller; drops what is gone |
| `codeblue link\|unlink <button x y z> <annunciator x y z>` | A button calls an annunciator, or stops |
| `codeblue status <x y z>` | A button's annunciators or an annunciator's buttons, and which are active |
| `codeblue reset <x y z>` | Resets the code at a button or annunciator |
| `codeblue prune` | Drops links to blocks that are gone |

---

## Placing by command

Every block is placed with `/setblock` and set up with `/blockdata`; no block needs a GUI. The
hospital generators depend on this table.

**Facing codes.**

- **Floor pieces**: bits 0-1 the horizontal index **S 0, W 1, N 2, E 3**
  (`AbstractBlockRotatableNSEW`), the way the front faces (towards the room, or the placer). Flags
  from bit 2.
- **Wall pieces**: bits 0-2 the `EnumFacing` index **N 2, S 3, W 4, E 5** (0 and 1 read as north),
  the way out of the wall; the model sits at the back of its own cell, the wall-face cell. One flag
  in bit 3.
- **Lit pieces**: Lighting's `facing * 4 + state`, the horizontal index and state 0 off under
  redstone, 1 on under redstone, 2 off by hand, 3 on by hand. Place lit with `facing * 4 + 3`.
  Healthcare's lights light their own cell and place no light-up air.
- **Pairs**: a wide pair's second cell is `first.offset(facing.rotateYCCW())`, the viewer's right
  (bit 2 on the floor, bit 3 on a wall); a tall pair's upper cell is above (bit 2). Set both cells.
- **Run pieces** join left and right (actual state) with the same group facing the same way.

### Patient rooms and wards

| Block (csm:) | Mount | Meta | NBT / notes |
|---|---|---|---|
| `hospital_bed_white`, `_blue`, `_green`, `_mauve`, `_icu`, `_bariatric`, `_labor` | head cell, floor | bits 0-1 facing (towards the foot), bit 2 rails up, bit 3 raised | head in the wall-face cell; foot at `head.offset(facing)` |
| `hospital_stretcher`, `hospital_stretcher_trauma` | head cell, floor | bits 0-1 facing (towards the foot), bit 2 rails up, bit 3 raised | exactly as a bed: foot (`hospital_bed_foot`) at `head.offset(facing)`, either cell first; a click on a long side raises or lowers the rails, a sneak-click the height, anywhere else lies the player on it |
| `hospital_bed_foot` (hidden) | foot cell, floor | bits 0-1 facing (the head's) | reads variant, rails and height from the head at `foot.offset(facing.opposite)`; either cell first; breaking one breaks both |
| `patient_headwall`, `patient_headwall_upper` | wall, 2 wide | bits 0-2 facing, bit 3 second cell | over the bed head, at +1 and +2 |
| `patient_wardrobe` | floor, 2 tall | bits 0-1 facing, bit 2 upper cell | |
| `patient_reading_light` | wall, lit | `facing * 4 + state` | light 12 when lit |
| `patient_monitor_wall`, `patient_room_tv`, `vitals_wall_unit` | wall | bits 0-2 facing, bit 3 on | screen animated when on; click switches |
| `patient_monitor_stand`, `iv_pole_pump` | floor | bits 0-1 facing, bit 2 on | |
| `iv_pole`, `overbed_table`, `patient_recliner` (seat), `visitor_chair` (seat) | floor | bits 0-1 facing | |
| `suction_regulator`, `sharps_container`, `glove_box_dispenser`, `hand_sanitizer_dispenser`, `medical_gas_outlet_o2`, `medical_gas_zone_sign` | wall | bits 0-2 facing | |
| `ppe_isolation_cart`, `biohazard_bin`, `soiled_linen_hamper`, `clean_linen_cart` | floor | bits 0-1 facing | |
| `nurse_call_button`, `nurse_call_pillow_speaker`, `nurse_call_pull_cord` | wall | bits 0-2 facing, bit 3 calling | redstone 15 while calling. A `/setblock` with bit 3 starts the call as a click does (dome light, panel lamp and chime, station monitors) and one without bit 3 ends it, from air or over the same device (re-setting in place works; no air first). Link before or after: an unlinked device keeps its call and its room lights when linked |
| `nurse_call_dome_light` | wall, lit | `facing * 4 + state` | 0/1 follow the room's calls or redstone; 2/3 force dark or lit |
| `nurse_call_master_panel` | wall | bits 0-2 facing | TE `rooms` (byte, bit i = room i + 1's lamp), set by the network on any change |
| `corridor_handrail`, `corridor_crash_rail` | wall | bits 0-2 facing | ends (join, inner, outer, end) from neighbours |
| `cubicle_curtain_track` | ceiling-tile layer | bit 0 axis of a lone track (0 E-W, 1 N-S) | sides from neighbours; by hand fills its column; breaking takes the column |
| `cubicle_curtain` (hidden) | under a track | bits 0-1 cloth (0 mint, 1 blue check, 2 beige, 3 white), bits 2-3 drawn (0 closed, 1 open, 2 bunched) | shape, top and bottom from neighbours; no collision |

### ICU, emergency department, nursing and surgery support

| Block (csm:) | Mount | Meta | NBT / notes |
|---|---|---|---|
| `icu_ventilator`, `crash_cart`, `defibrillator`, `workstation_on_wheels`, `triage_desk`, `autoclave` | floor | bits 0-1 facing, bit 2 screen on | |
| `ultrasound_cart` | floor | bits 0-1 facing (the console and screen face the sonographer), bit 2 screen on | the image is an animated texture; a click switches it. Probe holders on the left, the gel bottle on the right, both just past the collision box |
| `medication_cart`, `medication_fridge`, `case_cart`, `walker`, `wheelchair` (seat), `wheelchair_storage_rack` | floor | bits 0-1 facing | |
| `nurse_station_counter` | floor | bits 0-1 facing (front towards the corridor) | joins any station piece left and right |
| `nurse_station_counter_monitors` | floor | bits 0-1 facing (front towards the corridor) | joins any station piece left and right; TE `rooms` (byte, bit i = room i + 1 of its panel calling), set by the network: link it with `nursecall link <panel> <counter>` rather than writing it |
| `nurse_station_corner` | floor | bits 0-1 facing | an outside corner, front along its north and west (model) sides: a counter with the same facing joins on its east, one facing `facing.rotateYCCW()` on its south |
| `medication_dispensing_cabinet`, `blanket_warming_cabinet`, `scrub_attire_dispenser`, `patient_lift_mobile` | floor, 2 tall | bits 0-1 facing, bit 2 upper cell | |
| `patient_status_board` | wall, 2 wide | bits 0-2 facing, bit 3 second cell | |
| `decontamination_shower` | wall, 2 tall | bits 0-2 facing, bit 3 upper cell | pairs with Life Safety's `decon_sink` |
| `ambulance_only_marking` | floor overlay, 2 wide | bits 0-1 facing (text reads from the front), bit 2 second cell | no collision |
| `wheelchair_corral_sign`, `ambulance_only_sign` | wall | bits 0-2 facing | |
| `ed_tracking_board` | wall | bits 0-2 facing | TE `beds` (int 1-40, default 24), `seed` (int, default 0); boards side by side or stacked with one facing page on, 7 beds a board |
| `icu_pendant`, `surgical_equipment_boom` | head, hung | bits 0-1 facing, bit 2 monitor on | `ceiling_boom_column` (meta 0) in every cell above up to the ceiling, or `boom fill`; by hand on a ceiling the head goes 1 above the floor |
| `trauma_exam_light` | head, hung, lit | `facing * 4 + state` | as above, 2 above the floor by hand; light 15 |
| `ceiling_boom_column` (hidden) | column cell | any | top cell draws the flange; breaking any cell takes the piece |
| `ceiling_patient_lift` | ceiling-tile layer | bits 0-1 facing (rail along the facing axis) | draws its own tile |

### Clinics, dialysis, infusion, endoscopy, dental and eye exam

| Block (csm:) | Mount | Meta | NBT / notes |
|---|---|---|---|
| `exam_table` | floor, 2 wide | bits 0-1 facing, bit 2 second cell | the head (raised back, paper roll) is the first cell |
| `clinic_exam_light` | floor, lit | `facing * 4 + state` | light 12 |
| `wall_diagnostic_set`, `visual_acuity_chart` | wall | bits 0-2 facing | |
| `patient_scale`, `dialysis_machine`, `endoscopy_tower` | floor | bits 0-1 facing, bit 2 screen on | |
| `phlebotomy_chair`, `dialysis_chair`, `refraction_chair` (seats), `clinic_step_stool` | floor | bits 0-1 facing | |
| `infusion_recliner`, `dental_chair` | floor, seat | bits 0-1 facing, bit 2 pump screen / operatory light on | click sits; sneak-click with an empty hand switches |
| `dialysis_ro_unit` | floor, 2 tall | bits 0-1 facing, bit 2 upper cell | the controller display always runs |

### Imaging, laboratory, pharmacy and morgue

Unit offsets are in the model frame drawn facing north: +x east (the viewer's left facing the
front), +z south (towards the back), +y up; turned with the facing (east: +x to south, +z to west;
south: +x to west, +z to north; west: +x to north, +z to east).

| Block (csm:) | Mount | Meta | NBT / notes |
|---|---|---|---|
| `ct_scanner` | unit; master = the table's middle cell | bits 0-1 facing (the way the table's foot points) | 8 cells: master, table foot (0,0,-1), gantry x -1..1, y 0..1 at z +1. A `/setblock` of the master places the parts |
| `mri_scanner` | unit; master = the table's middle cell | bits 0-1 facing | 13 cells: master, table foot (0,0,-1), magnet x -1..1, y 0..1, z 0..1 |
| `healthcare_unit_part` (hidden) | unit cell | `EnumFacing` index of the step towards the master (D0 U1 N2 S3 W4 E5) | never set by hand; collision is its share of the unit's boxes |
| `xray_ceiling_tube` | head, hung | bits 0-1 facing, bit 2 collimator light on | as the pendant; 2 above the floor by hand |
| `xray_table`, `radiology_reading_workstation`, `autopsy_table`, `body_cart` | floor, 2 wide | bits 0-1 facing, bit 2 second cell | the workstation is a desk: set a chair in front |
| `xray_wall_bucky`, `c_arm`, `mammography_unit`, `fume_hood`, `biosafety_cabinet`, `blood_bank_fridge`, `iv_compounding_hood`, `pharmacy_bin_shelving`, `pharmacy_unit_dose_cabinet` | floor, 2 tall | bits 0-1 facing, bit 2 upper cell | the C-arm's C reaches into the cell in front with no collision |
| `morgue_cabinet` | floor, 2 tall | bits 0-1 facing, bit 2 upper cell, bit 3 this cell's door open | click opens or shuts that cell's door; the tray slides half out in front, no collision |
| `c_arm_monitor_cart`, `chemistry_analyser`, `haematology_analyser` | floor | bits 0-1 facing, bit 2 screen on | |
| `lab_centrifuge`, `lab_microscope` | floor (bench top) | bits 0-1 facing | in the cell above a lab bench |
| `lab_bench`, `lab_bench_sink` | floor | bits 0-1 facing | joins any lab bench left and right; worktop at the top of the cell |
| `lead_apron_rack`, `mri_zone_1_sign` .. `mri_zone_4_sign`, `radiation_warning_sign`, `specimen_pass_through`, `pneumatic_tube_station` | wall | bits 0-2 facing | |
| `xray_in_use_light`, `mri_in_use_light` | wall, lit | `facing * 4 + state` | light 7; redstone lights it in states 0/1 |

### Maternity, rehabilitation, waiting room, teaching and playroom

| Block (csm:) | Mount | Meta | NBT / notes |
|---|---|---|---|
| `infant_warmer` | floor, lit | `facing * 4 + state` | heater light 9 |
| `bassinet`, `kids_play_table`, `playroom_toy_box`, `anatomical_torso_model` | floor | bits 0-1 facing | the torso goes on a bench or table top (the cell above it) |
| `nicu_incubator` | floor | bits 0-1 facing, bit 2 monitor on | |
| `kids_chair_red`, `_blue`, `_green` | floor, seat | bits 0-1 facing (the way the sitter faces) | |
| `parallel_bars` | floor, run | bits 0-1 facing | walk-through; posts and bevelled ends where the run stops |
| `waiting_beam_chair_blue`, `_teal`, `_charcoal` | floor, run, seat | bits 0-1 facing (the way the sitter faces) | colours join each other |
| `lecture_hall_seat` | floor, run, seat | bits 0-1 facing, bit 2 raised (half-block riser) | front row on the floor (0), the row behind raised (1), the next on a full block one up (0), and so on |
| `treatment_plinth` (seat), `exercise_stairs`, `dissection_table` | floor, 2 wide | bits 0-1 facing, bit 2 second cell | the stairs collide as steps |
| `skeleton_stand` | floor, 2 tall | bits 0-1 facing, bit 2 upper cell | |
| `simulation_mannequin` | the cell above a bed or table | bits 0-1 facing (the way its feet point) | over a bed: the bed's facing; over an exam, autopsy or dissection table's first cell: the table's `facing.rotateYCCW()`; else flat on its own floor. No collision |
| `check_in_kiosk`, `queue_desk_console` | floor (the console on a counter: the cell above it) | bits 0-1 facing | a queue controller; a click calls the next number; its number is in saved data, from 0 |
| `now_serving_display` | wall | bits 0-2 facing | TE `number` (0-999, 0 dashes), `desk` (0-99, 0 no desk line); a linked display's number follows its controller |

### The operating room

| Block (csm:) | Mount | Meta | NBT / notes |
|---|---|---|---|
| `or_table` | floor, 2 wide, seat | bits 0-1 facing, bit 2 second cell | the head (headrest, arm board) is the first cell; the second at `first.offset(facing.rotateYCCW())`; a click lies the player on it |
| `or_anaesthesia_machine` | floor | bits 0-1 facing, bit 2 screen on | the front (gas panel) faces the facing; a click switches the screen |
| `or_surgical_light` | head, hung, lit | `facing * 4 + state` | `ceiling_boom_column` above it up to the ceiling, or `boom fill`; by hand on a ceiling it goes 2 above the floor; light 15 when lit; the arms run along the facing's left and right |
| `or_scrub_sink` | floor, run, against a wall | bits 0-1 facing (towards the room) | joins any scrub sink facing the same way left and right |
| `or_back_table` | floor, 2 wide | bits 0-1 facing, bit 2 second cell | |
| `or_mayo_stand`, `or_kick_bucket`, `or_drape_trolley` | floor | bits 0-1 facing | the Mayo stand has no collision, so it can stand over the table |
| `or_supply_cabinet` | floor, 2 tall | bits 0-1 facing, bit 2 upper cell | |

### Wayfinding and building systems

| Block (csm:) | Mount | Meta | NBT / notes |
|---|---|---|---|
| `hospital_wayfinding_panel` | hung cell, joins into a panel up to 16 x 6 | facing S 0, W 1, N 2, E 3 (towards the reader; Transit's) | TE as `airport_wayfinding_panel`: `l1`, `l2` (printable ASCII, 32 characters), `p` pictogram, `a` arrow, `s` scheme, `d` back carries the legend (byte, default 1). A compound without `p` leaves the sign alone. A `/setblock` with NBT or `/blockdata` on any cell writes the whole panel. `pc`, `pr`, `pw`, `ph` are the server's; never write them. Re-facing a cell by `/setblock` of the same block leaves its old panel and joins the new one, as placing it fresh does |
| `code_blue_button` | wall | bits 0-2 facing, bit 3 active | a `/setblock` with bit 3 activates the code, from air or over the same button; one without bit 3 over an active button stands that button down (its annunciators fall silent unless another linked button or redstone holds them; a reset clears every button sharing them). Redstone 15 while active, strong into its wall |
| `code_blue_annunciator` | wall | bits 0-2 facing, bit 3 active (worked out) | active while a linked button is active or it is powered |
| `medical_gas_zone_valve_box`, `hospital_outlet_normal`, `hospital_outlet_emergency`, `hospital_outlet_pair` | wall | bits 0-2 facing | outlets sit low in the wall-face cell at floor level (the pair: red on the viewer's left) |
| `medical_gas_master_alarm` | wall | `facing * 4 + state` (horizontal index); state bit 0 alarm (from redstone), bit 1 buzzer silenced | place quiet with `facing * 4 + 2` |
| `infant_security_panel` | wall | bits 0-2 facing, bit 3 alarm (latched) | input: the wall block behind it powered; output: 15 on every other side |

Wayfinding values. `p`: `none`, `emergency` (ambulance), `xray`, `lab` (flask), `pharmacy` (Rx),
`maternity` (parent and baby), `elevator`, `stairs`, `restroom`, `cafe`, `chapel` (candle),
`parking`, `info`, `accessible` (wheelchair), `exit` (to the street); Transit's `depart`, `arrive`,
`checkin`, `baggage`, `ground`, `train`, `bus` and `taxi` read as none. `a`: `none`, `left`,
`right`, `up`, `down`, `up_left`, `up_right`, `down_left`, `down_right`. `s`: `neutral` (white on
navy; the default and what an unknown id reads as), `emergency` (red), `imaging` (blue), `surgery`
(green), `maternity` (pink), `outpatient` (teal), `inpatient` (purple), `laboratory` (orange),
`admin` (grey), and Transit's `airport`, `metro`, `exit`, `info` in Transit's colours. A cell never
written shows INFORMATION, `info`, `right`, `neutral`.

### Elsewhere

- **Doors and finishes** (Building): hospital doors (maple, blue and sage metal; vision lite or
  half glass; single, double and 1.5-leaf), smoke barrier doors held open by Life Safety door
  holders through redstone, the lead-lined door, automatic sliding doors with sidelites and the
  activation mat (DOORS.md, Placing by command), and accent wall panels, wayfinding stripes, room
  signs and nurse station glass screens (INTERIOR_FINISHES.md, Placing by command).
- **Helipad** (Transit): `/csmtransit helipad <x y z> <size> hospital [facing] [boxes]`, or the
  cells by `/setblock` (TRANSIT_SYSTEM.md, The helipad). The deck under it must be solid: a sea
  lantern is not, and its cell is skipped.
- **Pews and altar** (Furnishings, Commercial & Office): `pew_oak`, `pew_walnut` (run, seat, bits
  0-1 the way the sitter faces), `altar_table_oak`, `_walnut` (bits 0-1 facing). **Commercial
  laundry** (Residential's laundry group): `commercial_washer_stainless`,
  `commercial_dryer_stainless` (bits 0-1 facing, bit 2 running, as `washing_machine_*`).
- **Eyewash station and decon sink** are Life Safety's (`eyewash_station`, `decon_sink`,
  Emergency Services); **planters** are Parks', plantable (PARKS_GREENERY_SYSTEM.md).

---

## Decisions

| Decision | Why |
|---|---|
| Its own module, Core only | A server without hospitals leaves the jar out; nothing a hospital needs is worth a dependency |
| Each neighbour's piece in its own module (smoke and hospital doors in Building, helipad in Transit, pews and laundry in Furnishings) | Each has a natural home and is not hospital-only |
| Leave the operating room to the Theatre Pack, at first | The issue says it covers the OR; the builder agreed. Later the builder asked for a consistent CSM OR with the module's facing and metadata docs, so the set was added (The operating room), drawn from scratch, nothing taken from the pack |
| No real brands, no red cross | The red cross is a protected emblem; a generic six-armed star or plain text instead, as Life Safety does |
| Light behaviour, plus three richer systems | Most blocks are light (clicks toggle state); the user added the nurse call network, the ED board and the now-serving display on top |
| Lying in bed is sitting up on Core's seat | `EntityCsmSeat` rides in the sitting pose; sleeping stays with the Residential beds |
| Lit pieces keep Lighting's metadata, with no light-up air | Healthcare may not name Lighting's air block, and a wall light needs no projection |
| Sign legends meant to be read down a corridor use the committed highway font (`hc.stamp_legend`), not the 3 x 5 pixel font | The pixel font's strokes vanish in the mip levels and its M reads as H |
| A seat with a screen (infusion recliner, dental chair) sits on a click and switches on a sneak-click | A click cannot do both; sitting is the common use |
| The dental and refraction chairs are one block each, their post, tray and phoropter drawn just past the cell | No multi-cell bookkeeping for a chair; those parts never need collision |
| The lead-lined door is Building's; the pharmacy adds bin shelving and a unit-dose cabinet but not Furnishings' retail pharmacy counters | No duplicates |
| No eyewash station here | Life Safety already registers `eyewash_station`; the same name twice would crash |
| Registry names `clinic_exam_light`, `clinic_step_stool`, `dialysis_ro_unit` | `exam_light` and `step_stool` are too generic for the shared `csm:` namespace |
| Any new toy box gets a new registry name | `toyboxgirl` and `toyboxboy` are retired names (below) |

## Traps

- **The eyewash name clash.** Life Safety registers `eyewash_station` in Emergency Services; a
  Healthcare block of that name would crash registration, so there is none, and the builder uses
  Life Safety's. Check the whole `csm:` namespace (`csm_block_index.py`) before naming a block.
- **`model_depth.py` moved a screen in front of the renderer.** The ED board's and now-serving
  display's renderers draw a lit copy of the screen just in front of the baked one
  (`ED_SCREEN_Z`, `NS_SCREEN_Z`). A screen drawn flush with its bezel is a coplanar face the
  generator's `model_depth` pass separates by growing it forwards, which put the baked screen
  over the renderer's text. The screens are written 0.3 px proud of the bezel, clear of the
  separation, and the renderer's plane is just in front of that: change one number, change both.
- **A block sprite that is not square is refused.** The room plate's glyph sheet was 128 x 64; the
  atlas treats a non-square sprite as a broken animation and the legend drew as the missing
  texture. Sheets are square (128 x 128), with the unused rows empty.
- **The font atlas's mipmaps were never sampled.** `CsmFontRenderer`'s `DynamicTexture` is
  allocated with `GL_TEXTURE_MAX_LOD` 0, so the mip levels it generates were ignored and minified
  text was bilinear on full-size glyphs: thin strokes fell between texels and vanished ("IMAG NG",
  "TRIAGL"). Core now lifts the clamp, which fixed the flight and bus boards too.
- **CSM bakes one shared model for every state of a multipart blockstate.** The wayfinding
  panel's wrapper first keyed on the variant name and never applied; Core's multipart state mapper
  gives every state one `#multipart` location, so the wrapper reads the facing from the state.
  Any baked-model wrapper on a multipart block must do the same.
- **Small red LEDs read as a red cross.** A disc a few texels across draws as a plus at the mip
  levels, and a red plus is the protected emblem. Status lamps on small textures are squares,
  never discs (`gen_healthcare_systems.py`'s lamp helper says so).
- **`toyboxgirl` and `toyboxboy` are retired names.** The builder reported a toy box that would not
  place: the two Decocraft-era toy boxes were retired on 2026-09-24 (`CsmRetiredNames`) and load as
  air. Not a bug; the playroom set replaces them, and any new toy box must use a new name.
- **Two halves placed by command never clean each other up.** A bed, pair or unit set cell by cell
  is half-built in between, so nothing removes a cell whose partner is missing; only breaking does.
  A generator that crashes half way leaves halves for the next run to finish.
- **A `/setblock` of the same block runs no block method.** The game calls `onBlockAdded` and
  `breakBlock` only when a block replaces a different one, so a nurse call device set over itself
  with the calling bit started no call (reported by the hospital builder after 2026.10.20), a
  code blue button did not activate and a re-faced wayfinding cell kept its old panel. Every
  change sent to clients passes a world's listeners with the state before and after, so the
  module adds one to each server world (`HealthcareStateChanges`) and hands a same-block change
  to a block that implements its `Listening`: the nurse call devices, the code blue button and
  the wayfinding panel. A block that keeps a link or a call in state should implement it too. A
  change made without flag 2 (not sent to clients) is still not heard.
- **Wayfinding cell updates go with the tick's block changes.** A panel built in one tick refreshes
  its cells as each is added; sent at once, those updates arrived at the client before the cells
  themselves and were dropped ("Received invalid update packet for null tile entity"), a packet
  per cell per cell added. They are queued with the chunk's changed blocks
  (`HospitalWayfindingSign.queueSync`).
- **A hole in a panel is a hole, not a smaller panel.** The panel is the controller's rectangle:
  its bottom row's width and its left column's height. Take out a top-right cell and the panel is
  still its full size with that cell missing (the arrow there loses its top half); a cell outside
  the rectangle is drawn plain.
- **The lecture seat's collision must leave the legroom.** With the riser's full cell solid a
  sitter dismounted into it; the collision leaves the front of the cell free.
- **A helipad needs a solid deck.** `/csmtransit helipad` skips a cell over a block whose top is
  not solid, and a sea lantern (glass material) is not; the demo lays concrete under its pad.

---

## Weight

Measured 2026-10-19 in the dev client (every module, plus the IV content packs, FAWE and a
third-party road mod's test jar in `run/mods`), `-Xmx6G`, on a machine also running a game.

**Since then** (2026-10-20, counted from the block classes, not measured): the station counter
with monitors' room lamps added 1,008 states, the operating room set nine blocks and 76
states, and the two transport stretchers (16 head states each, 32 more on the shared foot) and
the ultrasound cart (8) three blocks and 72 states, so the module is 162 blocks and **3,332
states**; both are multiparts or Forge
blockstates of small parts, and the set added seven textures of 16 and 32 px (the atlas stayed at
95.4%).

**Blocks and models** (`/csm memstats dump`, the module's row, before the above): 150 blocks, **2,176 states**,
1,060 model locations, 34,727 quads (5.6 MB), about 6.7 MB in all. The most states are the curtain
panel (288), the master panel (256), the curtain track (162), the bed foot (112), the corridor
rails and wayfinding panel (64 each); the heaviest models are the mannequin (2,512 quads across
its rests), the bed foot (2,140), the exercise stairs (1,142), the RO unit (1,076) and the skeleton
(1,063). Building's hospital doors and finishes added 7,689 states to Building.

**Heap and launch**, the dev client launched four times interleaved, with and without the module
(`-PcsmRunModules` naming every module but Healthcare), live heap at the main menu after a forced
full GC:

| | Launch (process to main menu) | Live heap at the menu |
|---|---|---|
| Without Healthcare | 51 s, 46 s | 1,688.6 MB, 1,688.0 MB |
| With Healthcare | 44 s, 43 s, 44 s | 1,702.7 MB, 1,704.1 MB, 1,704.0 MB |

So the module costs about **15 MB of heap** and no launch time that the noise of single runs
(+-5 s) can show. In the Healthcare demo world the client held 1,769 MB live. These absolute
figures are not comparable with the 0.6 GiB and 28 s in PERFORMANCE_AND_SECURITY.md (Memory): that
was every CSM module alone, before Vehicles; this client also carries the IV packs, FAWE and the
road mod's test jar, whose baked models alone come to about 308 MB in `/csm memstats`, and its
block atlas is 8192 x 8192 with their sprites in it.

**Block atlas**: `atlas_budget.py` puts CSM at **95.3% of 8192 x 4096** (4,091 sprites, 31.75 Mpx
padded), 0.10 Mpx over the 95% warning and still that size. Healthcare's own textures are 259
sprites, 0.56 Mpx; the atlas stood at 93.5% when the track started. The user accepts the atlas
growing past 8192 x 4096; what to watch is frame time and RAM.

**Frame time** (`client_frame_stats`, 10 s windows, input locked, `renderWork` the CPU time drawing
a frame; 854 x 480 window, render distance 12, the frame cap not binding):

| View in the demo world | renderWork mean | p99 |
|---|---|---|
| The corridor from the main entrance, its length in view | 0.79 ms | 1.19 ms |
| The ED, both tracking boards in view | 0.68 ms | 1.03 ms |
| The same with the two boards taken away | 0.65 ms | 0.99 ms |
| The waiting room, both now-serving displays in view | 0.67 ms | 1.03 ms |
| The whole floor from above (40 blocks up) | 0.66 ms | 1.02 ms |
| The same spot looking away, at empty flat land | 0.57 ms | 0.87 ms |

`client_profile_rendering` timed the two renderers directly: an ED tracking board **16-24 us** a
frame (two boards 40 us, in line with the A/B's 0.03 ms), a now-serving display **5-11 us** (two
16 us). Everything else is baked into the chunk mesh and has no per-frame cost of its own.

## The demo world

`dev-env-utils/scripts/build_healthcare_demo.py` builds one hospital floor in a flat, peaceful,
creative world named "Healthcare Demo" (it refuses any other), through the MCMCP orchestrator by
instance id (`--instance run-f2eb58`, `--x`/`--z` for the origin). A corridor five blocks wide runs
east from a sliding-door entrance, with hospital doors, room signs and dome lights, handrails, a
crash rail by the ED, blue and teal wayfinding stripes, a smoke barrier pair held open by redstone,
and hanging wayfinding panels of 5 x 2, 4 x 2, 3 x 1 and 1 x 1 cells (and a 3 x 1 read from the far
end). North of it: a two-bed ward (curtain round one bed, nurse call linked to the station, one
call left on), an ICU bay behind sliding doors (ICU and bariatric beds, pendant, ventilator,
ceiling lift, a code blue button linked to the station's annunciator), the ED (a tracking board
bank, trauma bay, decon shower, surgery support, an ambulance entrance with its marking and sign),
imaging (CT, MRI, an X-ray room, C-arm, mammography, lead doors, IN USE lights, MRI zone signs; the
ED also has a trauma stretcher and an ultrasound cart at its trauma bay),
the lab, the pharmacy, the morgue and the operating room (the table under the twin surgical
light, the anaesthesia machine at its head, the back table and Mayo stand at its foot, supply
cabinets, the drape trolley, a three-bay scrub sink and a transport stretcher in from pre-op). South: the nurses' station open to the corridor (counters
and corner, the monitors linked to the master panel and showing its calls, master panel, annunciator, gas alarm, outlets), a clinic with an eye lane and a dental
chair, dialysis and infusion, maternity and NICU (labour and postpartum beds, the infant security
panel at its door), the waiting room open to the corridor (beam seating facing two now-serving
displays, the kiosk serving 042 and the desk console 117), a chapel (pews, altar, lectern), a
lecture hall (tiered seats, a mannequin on a bed, skeleton, dissection table and torso), the
playroom, the laundry and a rehab gym. A 9 x 9 hospital helipad with its lights and wind sock
sits on the roof over the ED. It reads both tabs' registration lists and puts anything it did not
place on signed plinths; nothing is missing today (158 of 158).

---

## Left out, and what the module could grow into

- **More of the operating room**: an equipment boom and the surgical light share the drop column
  pattern, so a combined boom-and-light head, a second OR table pose (tilted), warming cabinets
  and a scrub-room hand dryer are the obvious next pieces.

Open items and checks still to do are in `agent_progress/UNFINISHED_ITEMS.md`, D.28.
