# The Transit Module

CSM: Transit (`csm_transit`, tree `modules/transit`, Java package `transit`) is the optional module
for public transit. Like every module it pins Core to its own exact version and registers nothing
itself. It also **requires Roads & Traffic** (`required-after:csm_roads`), because its bus stop
signs are road signs (below); like Text to Speech on Technology, that is the one other module it
may name. Its creative tab, **Transit** (`tabtransit`, `@CsmTab.Load(order = 27)`), is found by
Core's tab scan, and every block and item keeps the `csm:` namespace.

The module holds the working fare system, which it took over from Technology; the bus stops and
shelters; the bus station departure boards; the station and platform fit-out and the stations'
entrances and fare lines, made to complement the stations of RCMC, the author's train mod; and an
airport's terminal and airside pieces. A demo world with all of it laid out to walk round is built
by `dev-env-utils/scripts/build_transit_demo.py` (Demo world, below). What the module was left
without, and what it may grow into, is at the end of this page.

| Block or item | Registry name | Class |
|---|---|---|
| Fare Vending Machine | `csm:farevend` | `transit.fare.BlockFareVendingMachine` |
| Fare Gate | `csm:fare_gate` | `transit.fare.BlockFareGate` |
| Fare Gate (ADA, 2-Wide) | `csm:fare_gate_ada_2` | `transit.fare.BlockFareGateAda2` |
| Fare Gate (ADA, 3-Wide) | `csm:fare_gate_ada_3` | `transit.fare.BlockFareGateAda3` |
| Fare Ticket (item) | `csm:fareticket` | `transit.fare.ItemFareTicket` |
| Transit Card (item) | `csm:transitcard` | `transit.fare.ItemTransitCard` |
| Bus Stop Flag (CITYLINE, RIVERWAY, VERDANT, EMBERLINE) | `csm:bus_stop_flag_<agency>` | `transit.stop.BlockBusStopFlag` |
| Bus Stop Timetable Case | `csm:bus_stop_timetable_case` | Roads' `trafficsigns.BlockTrafficSign` |
| Bus Stop Route Map Case | `csm:bus_stop_route_map_case` | Roads' `trafficsigns.BlockTrafficSign` |
| Bus Arrival Display | `csm:bus_stop_arrival_display` | `transit.stop.BlockBusArrivalDisplay` |
| Bus Stop Curb Plaque | `csm:bus_stop_curb_plaque` | `transit.stop.BlockBusStopPlaque` |
| Bus Shelter (Glass, Cantilever, Flat Roof; four agencies each) | `csm:bus_shelter_<style>_<agency>` | `transit.shelter.BlockBusShelter` |
| Bus Departure Board, Bus Bay Display | `csm:bus_departure_board`, `csm:bus_bay_display` | `transit.board.BlockBusBoard` |
| Station and platform fit-out (29 blocks) | see Station and platform fit-out, below | `transit.platform` |
| Station entrances, the fare line, line bullets, the booth counter (13 blocks) | see Stations, below | `transit.station` |
| Airport terminal pieces (25 blocks) and the Boarding Pass (item) | see Airports, below | `transit.airport` |
| Airport airside pieces (26 blocks) | see Airports, Airside, below | `transit.airport` |

---

## The fare gates

A fare gate is one block with a tile entity (`TileEntityFareGate`, `csm:tileentityfaregate`). The
two ADA gates are the same class with a wider lane: the model, the collision box and the exit
sensing box reach past the placed cell sideways (the iMac's trick), rather than placing a
multi-block. Its item (`ItemBlockFareGate`) places the gate one cell above the block aimed at, so
the gate's cell sits at chest height, where a player looking straight ahead clicks it, and the
cabinet is drawn down into the floor-level cell.

**Direction.** `FACING` points at the gate's outside, the side the player placed it from.

- **Entering** takes fare media, from the outside. Right-clicking with a Fare Ticket uses it up;
  with a Transit Card, one trip comes off the card's balance, and a card with none left is
  refused with a low note. Either opens the gate as `OPEN_ENTRY` (green arrow).
- **Leaving** is free. Every five ticks the tile entity looks for players in the cells behind the
  gate, and one who has just arrived there opens it as `OPEN_EXIT`, which shows the outside a red
  X, so nobody tries to come in against the flow. A player has to leave that box before they can
  open the gate again.

An open gate closes itself after 60 ticks (3 s). The `state` property is a `GateState`: `CLOSED`,
`OPEN_ENTRY`, `OPEN_EXIT`, or `CLOSED_LOCKED` (held shut by an operator, shown with the red X). An
open gate gives off a little light.

**Operator modes.** Sneak and right-click with an empty hand to open the gate's operator screen
(`FareGateConfigGui`, GUI id 17): **Normal**, **Always Open** (held in `OPEN_ENTRY`, no fare
needed) or **Always Closed (Maintenance)** (held in `CLOSED_LOCKED`). The choice goes to the
server as a `FareGateOpModePacket`, which the server takes only from a player within six blocks
of the gate, and is saved in the tile entity as `opMode` (the enum's ordinal).

## The fare vending machine and the fare media

The machine itself is drawn by `gen_transit_fare_vending.py`: a free-standing ticket machine two
blocks tall in the invented CITYLINE livery (teal, a yellow accent), with a lit TICKETS header, a
hooded touchscreen, keypad and tap pad, card, cash and coin slots and a ticket tray in relief. It is
one block whose model reaches into the space above (so every machine already placed keeps its
metadata); the model turns ambient occlusion off because that upper half was being shaded by the
lower block's neighbours, and the block gives light 6 for the screen and header. Its bounding box
follows the cabinet. The GUI is unchanged.

Right-clicking the vending machine opens `FareVendingGui` (GUI id 16). It sells, for emeralds
(`FareVendingPurchase`): a single-use ticket for one emerald; a new card with 1, 2, 5, 10 or 25
trips, at one emerald a trip; and reloads of +1, +5 or +25 trips onto the card in the player's
main hand. The choice goes to the server as a `FareVendingPurchasePacket`; the handler checks the
player is within six blocks of a real vending machine, that they can pay, and for a reload that
they hold a card, builds the item before taking the emeralds, and drops the item at the player's
feet if their inventory is full.

A purchase plays the checkout card terminal's approval beep, `csm:verifone_mx915`. That sound
belongs to the Furniture & Novelties module, so the handler looks it up by name and plays a note
block's chime when that module is not installed. Transit requires Roads, and nothing else but
Core.

The **Fare Ticket** is a plain single-use item. The **Transit Card** stacks to one, because every
card carries its own balance (`trips` in its NBT) and its tooltip shows it.

## Moved from Technology, nothing renamed

The fare system shipped in Technology until this module existed. The move kept everything a world
or a server depends on:

- the registry names of all four blocks and both items, and the tile entity's name, so placed
  gates and machines, their tile entity data and the tickets and cards in inventories load as they
  were;
- the GUI ids, 16 and 17 (GUI ids are global across the mod), now answered by
  `TransitGuiProvider`;
- every asset path. The blockstates, models and textures moved into the Transit tree with
  `git mv` but still live under `models/block/technology/shared_models/` and
  `textures/blocks/technology/`, because those paths are what the JSON names. Do not "tidy" them.

What did change: the classes' package (`technology` to `transit.fare`), the creative tab
(Technology to Transit, so these six entries left the Technology tab and appear in the new tab
instead), and the network channel. The two fare packets are registered on Transit's own channel
(`csm_transit`), and Technology's channel keeps its first two packets with their discriminators.

The Fabricator prices the Transit tab through `TransitFabricatorRules`, which gives the fare
equipment the price it had in Technology: a control board, sheet metal and a wiring harness.
`audit_fabricator_costs.py` mirrors that rule.

The textures come from `dev-env-utils/generate_fare_gate_textures.py` and
`generate_fare_item_textures.py`, which find the Transit tree through `csm_layout.owner_of`.

## Bus stops

Everything on a stop is drawn by `gen_transit_stops.py` and lives in `transit.stop`.

### A stop is a signed post

A bus stop is built on the road sign system, not beside it. The flag, the arrival display and the
two poster cases are **road signs**: subclasses of Roads' `BlockTrafficSign` (and so of
`AbstractBlockSign`), each one block of Roads' sign post with its piece on the front of the post
(the flag's across its front and off to one side), exactly as every road sign's model is a length
of post with a plate on it. A player builds a stop
the way a signed post goes up:

1. **Sign posts** (`signpost`, in Roads' Road Signs tab) from the ground, one or two.
2. **The poster cases and the arrival display**, stacked on the posts in any order.
3. **The flag** on top. It takes the facing of the sign below it, as every road sign does, so only
   the bottom post needs turning.

A typical stop is post, timetable case, arrival display, flag: four blocks, the flag's top at about
4.3 m. Because every piece is a road sign, the sign system gives it everything a road sign has, with
no code of Transit's own:

- **Eight facings**, and the facing passed up from the sign or post below.
- **The extension post** (`downward`) onto a slab or down through a guardrail.
- **Setback** (`shift=setback`): in front of a signal arm or hung from a span wire, the piece and
  its post move 12.5 back, into line with the arm's hardware, like any sign on a signal pole.
- **Back to back** (`shift=backtoback`): two flags (or a flag and any road sign) facing opposite
  ways, one on the post and one hanging in the block behind it, share the one post: the hanging one
  is drawn on the far side of the post. `AbstractBlockSign.getShouldBackToBack` picks which moves.
- **Span wires**: a flag hangs from a span like any sign.

Nothing settles onto the road any more: a road sign stands where its post is, and a slab or a
guardrail below is what the extension post is for.

### The models, and the rules they follow

Each piece has the three shift models every road sign has, written by the generator from one
drawing: `sign_<piece>` as drawn (the piece on the front of the post, the post at z 0.5 to 3.5),
`_setback` (all of it 12.5 back) and `_back_to_back` (the piece alone, no post, 28.3 back). The
post is the same five bars every road sign's model carries, so a stop's pieces and the sign posts
between them read as one post. The blockstates are Forge format, like the road signs': facing by
`transform`, `downward` by the `sign_pole` submodel, `shift` by model -- except the flag's, whose
model `hang` picks (Flags and route plates, below).

Two differences from Roads' own signs, both deliberate:

- **Back to back is 28.3, not 28.5.** The road signs' convention puts a shifted plate's front at
  28.5, level with the end of the partner's post, and paints the art on a sliver a hundredth of a
  unit in front of it. Here the whole piece stops 0.2 short, so its face stands the depth test's
  gap clear of the post end and needs no sliver. It still pairs with every road sign: the
  partner's post is where it always is.
- **The post's end caps each paint one texel.** The five bars' ends are coplanar by design, and
  `model_depth.separate` would otherwise stair-step them past the block into the next post.

`SignShiftModelTest` and `SignFaceDepthTest` (Roads' tests, run in the one suite) hold these models
to the same rules as Roads' own: they take a blockstate whose default model is under
`csm:transit/stops/sign_` as a road sign, as well as one under `csm:trafficsigns/`. For a
blockstate with a `hang` variant block (the flag) `SignShiftModelTest` reads the three shift
models from `hang` instead, once for each side.

### Flags and route plates

Four invented agencies, each with its own flag layout so they read as four agencies and not one in
four colours: **CITYLINE** (teal and yellow, the fare machine's livery), **RIVERWAY** (navy and
orange), **VERDANT** (green and white) and **EMBERLINE** (red and graphite). Each carries the bus
pictogram, BUS STOP, the agency's name and the accessibility symbol, all drawn by the generator in
its pixel font and pixel art. The flag is a 10 x 13 plate printed on both faces (the back uses the
same art uv, which reads the right way round from behind); it stands on the top of its block and
above it, as the road signs' tall plates do, so the route plates can hang under it inside the
block, where a click reaches them.

Under the flag hang up to three route plates, 10 x 2.5, each in the agency's colour with a white
rim and a small bus. `TileEntityBusStopFlag` keeps their numbers (0 for no plate, 1 to 99; saved as
a three-byte array under `r`). Clicking a plate steps its number up and a sneaking click steps it
down, the aisle sign's pattern. The plate is chosen from the hit's height, split halfway between
plate middles, and the action bar says which plate now shows what.

**The flag hangs off the side of the post**, as New York's bus stop flags do: the post runs up one
edge of the sign and the sign's edge is bolted across the post's front. With the post beside the
sign rather than behind it, the back is as readable as the front. The player picks the side: a
click on the flag itself (anything above y 8.1, between the top plate and the flag's bottom edge;
sneaking or not) moves flag and plates to the other side, and the action bar says which. The
default, and what every flag saved before the choice existed loads as, is reaching to the reader's
right with the post at the sign's left edge. The side is saved under `l` (written only while
left).

- **Geometry.** The model faces north and the sign frame mirrors x, so the reader's right is the
  model's *low* x: right is x -2 to 8, left 8 to 18 (`SIDES` in the generator,
  `BlockBusStopFlag.FLAG_X_*`). The sign's inner edge ends at the post's middle. A unit further
  across looked more bolted-on, but from behind the post (x 6.5 to 9.5) stands in front of the
  back's inner edge, and there it hid 2.5 units of the ten -- the P of STOP and the end of a
  two-digit route number. Ending at the middle it hides 1.5, the margin beside the art. Every face
  past x 0..16 already names its uv.
- **Why one property picks the model.** A Forge blockstate lets every property's variant name a
  model, and when two do, the last one wins: `shift` and a separate side could never together say
  "setback, reaching left". So the flag's blockstate has no `shift` variants at all, and an
  actual-state `hang` property (`BusStopFlagHang`: `none_right`, `setback_right`,
  `backtoback_right` and the three `_left`), worked out from the sign system's shift and the tile
  entity's side, names one of six models: `sign_flag`, `sign_flag_setback`,
  `sign_flag_back_to_back` and the same three as `sign_flag_left*`. The state count is facing 8 x
  downward 2 x shift 3 x hang 6 x routes 8, 2,304 a flag, of which only a third can occur. Since
  `hang` already carries the shift, the block's state mapper (`BlockBusStopFlag.registerModels`)
  leaves `shift` out of the model locations: 768 a flag rather than 2,304 identical triples, which
  took the Transit module from 11,657 model locations to 4,461. `SignShiftModelTest` reads a
  blockstate with a `hang` block from `hang` and does not ask it for `shift`.
- **The box** is the road sign's, narrowed across to the flag and the post (x -2 to 9.5 or 6.5 to
  18, reaching a little past the block on the flag's side) so the empty side of the post does not
  take clicks; set back, the collision is the road sign's thin slab at the flag's plane, the same
  width. It is read off `hang`, so it follows a flip.
- **Back to back** each flag keeps its own side. Both at the default, right, they reach out on
  opposite sides of the one post, as reading right from both faces means; flip one to put both on
  the same side.
- **The other pieces stay centred.** The timetable and route map cases and the arrival display are
  in the blocks below the flag's, and the flag's lowest plate is in its own block, so nothing
  meets; a case or display centred on the post under a flag hung to one side is how real stops
  mount them.

Which plates are there is actual state (`route1` to `route3`), and it does not pick a model: the
four agencies share one flag model per shift, whose plate faces paint texture slots `p1` to `p3`,
and the blockstate fills a slot with the agency's plate while the plate is there and with a clear
texture while it is not. So the plates are baked in all three shift models without a model for
every combination. A new number rebuilds the chunk section only when a plate appears or goes, or the
flag changes side (`getBakedModelKey` is the mask of plates and the side).

The numbers are drawn by `TileEntityBusStopFlagRenderer`, white, on both faces, each number compiled
once into a display list shared by every flag (`CsmSharedDisplayLists`, keyed on the number) and
replayed under each plate's transform. The renderer works in the route marker sign's frame (the
facing's turn plus a half turn, the reader in front with +x to the right), at the depth of the
shift the sign system has put the flag in (`BusStopSigns.SHIFT_Z`), across the side it hangs
(`BlockBusStopFlag.middleX`: a plate's middle at model x is at 16 - x in that frame, and the
back's number turns a half turn about that middle, so it lands on the moved plate), and multiplies
the white by the plate's diffuse shade (0.8, or 0.6 facing due east or west), which a baked face
carries and a renderer does not. The facing and the shift are looked up once a second
(`AbstractTileEntityBusStopSign.refreshView`), not every frame.

The plate middles, the numbers' place on a plate and the plate faces' depth are constants in
`BlockBusStopFlag` (`PLATE_MIDDLE_Y`, `FLAG_X_*` and the rest) that must match `PLATE_TOPS`,
`PLATE_HT`, `SIDES` and `PZ0`/`PZ1` in the generator.

### Cases, the arrival display and the plaque

The **timetable case** and **route map case** are poster cases on the front of the post with two
bands round it: a shallow graphite box whose front is the poster, a frame standing proud of it,
and a faint glass sheen printed into the poster. The timetable is invented and the map a generic
diagram (a river, a park, three coloured lines, an interchange and a "you are here" dot) with no
place names. They are plain `BlockTrafficSign`s with their own models.

The **arrival display** (`BlockBusArrivalDisplay`, `TileEntityBusArrivalDisplay`,
`TileEntityBusArrivalDisplayRenderer`) is a small LED panel under a hood, clamped to the front of
the post. Its housing and dark screen are baked; the renderer draws three amber dot-matrix lines a
page, "12 DOWNTOWN 3 MIN", fullbright, turning the page every 5 seconds, on the screen at the depth
of the display's shift. It lists the routes on its own stop's flag -- the nearest bus stop flag up
or down its post, walking through the signs and posts of the column (`BusStopSigns.flagNear`) --
or routes 12 and 40 on a post with none. Each route's headway (6 to 16 minutes) and destination are
fixed by its number, from a list of generic destinations (DOWNTOWN, HARBOR, CITY HALL and so on),
so a route goes to the same place everywhere; its phase comes from the world clock and the column
of the stop's post, so neighbouring stops differ while every player at one stop sees the same countdown, a
minute each real minute. The next two buses of each route are listed, soonest first; under a minute
reads DUE. The route numbers, destinations and readings are one shared display list each (99, 16
and 33 at most), already laid out in their column, so a line is three list calls. The display saves
nothing: the routes, the facing and the shift are looked up once a second, not every frame.

The timetable is `BusDepartures`, shared with the departure board and the bay display (Departure
boards, below): the phase is seeded by the column of the stop's post (its x and z, not its
height), so the flag, the display on the same post and a board across the concourse, each looking
at that one post, count down the same buses.

The **curb plaque** (`BlockBusStopPlaque`) is a cast bronze plate reading BUS STOP to the player
who placed it, settling onto the surface below like the Streetscape fixtures. Its texture is
stored turned 180 degrees, because the top face shows it that way to the placer. A plate and not
paint: road markings belong to the external road mod.

A bus stop bench or bin is Parks'; Transit adds none.

### Traps

- **The flag's blockstate has no `shift` block, on purpose.** `hang` picks the model and carries
  the shift; the state mapper leaves `shift` out of the model locations. Adding `shift` variants
  back (or a second property that names a model) brings back the clash described above, and a
  `shift` block without the mapper's change makes every location miss its variant.
- **The plate numbers are drawn where the generator put the plates.** `PLATE_MIDDLE_Y`,
  `FLAG_X_*` and the depths in `BlockBusStopFlag` must match `PLATE_TOPS`, `PLATE_HT`, `SIDES` and
  `PZ0`/`PZ1` in `gen_transit_stops.py`, or the numbers float off their plates on one side only.
- **A flag is a road sign, so its shift comes from its neighbours.** A flag with a traffic pole
  behind it is set back onto the pole, and a flag hung in the block behind another sign's post
  facing the other way goes back to back; a builder who sees a flag "jump" has usually put it
  next to one of these.
- **Nothing on a stop settles.** Only the curb plaque settles onto the surface below; the posts
  stand where they are placed, and a slab below is what the extension post is for.

### Prices

`TransitFabricatorRules` prices a stop by what it is made of: a flag a sign blank and a fastener
kit; a case a sign blank and sheet metal; the arrival display an LED module, a control board and
sheet metal; the plaque sheet metal. The sign posts are Roads', priced by Roads.
`audit_fabricator_costs.py` mirrors the branches.

### Why there is no bus stop pole

The first bus stops had a pole family of their own: six pole blocks (round and square, galvanized
and painted) that the flag, cases and display stacked on, settling onto the road. They were
removed before any release, because a transit sign is a road sign: a stop built on Roads' sign
posts stands next to a signal arm, hangs from a span wire, pairs back to back with a street name
blade or a NO PARKING sign and matches every other post on the street, and a second pole family
could do none of that without copying the sign system. The registry names of the flags, cases and
display were kept.

## Shelters

Twelve blocks, `bus_shelter_<style>_<agency>`: three styles (`BusShelterStyle`) in the four
agencies' liveries, all one class, `BlockBusShelter`, and all drawn by `gen_transit_shelters.py`.

| Style | What it is | Layer |
|---|---|---|
| `glass` | glass back and end walls with a dotted frit band at eye level, steel posts, a roof with the agency's name on its fascia, a timber bench, and an empty frame at one end for an ad panel | translucent |
| `cantilever` | a canopy on one column a block, curving down at the back, open front and ends, a perforated steel screen and a bench | cutout |
| `flat` | a thin flat roof with a slim name fascia on slim corner posts, and a lean rail | cutout |

### One piece, two blocks

A shelter is placed as a door is: into its block and the one above (`upper`), both broken
together, only the lower half dropping the item. It faces the player who places it; its open
front is that side.

### Joining

Shelters of the same block facing the same way join both ways, from actual state only:

- **Along their length** (`left`, `right`: the same block continues on the sitter's left or
  right). End walls, end posts and the roof's end overhang are drawn only where the run stops; the
  back wall, bench and fascia run on through, and a post stands at every joint along the back. Any
  number in a row read as one shelter.
- **Front to back** (`ahead`, `behind`: the same block continues in front or behind). The back
  wall and bench are drawn only in the back row, the fascia and front posts only along the front,
  and the end walls run through, so two rows make a shelter two blocks deep (or three, or more).

Every part is written once in "shelter coordinates" (y from 0 to 32 over both halves) and cut at
the block line, so a post or a glass pane is one piece in the generator and two models in the
game. The collision boxes (`BusShelterStyle.collision`) are the walls, bench, rail, posts and roof
of each half, from the same numbers. The selection box is the whole block, so the end of a shelter
is easy to click when setting an ad panel against it.

### The roof light

A lens under the roof, lit when the shelter is placed (or matching the shelter it joins). It is
switched as the Residential lamps are: a click with an empty hand, or a change of redstone power at
either half -- on when power comes, off when it goes. The lower half remembers the power it last
saw (`powered`), the upper holds `lit`, each in the bit above `upper`. Either switches every upper
half of the shelter the block belongs to, up to 64 blocks, so one daylight sensor or switch lights
a long shelter. A lit upper half gives light 8. A click holding anything is left to the item, so
an ad panel (or any block) can be placed against a shelter.

### The ad panel slot

The glass shelter keeps the sitter's left end of its back row for an advertising panel: no glass,
but an empty steel frame -- two posts and a rail top and bottom -- set in 1.25 px from the block
edge, with the back wall, bench and roof stopping at it too. The panel itself is Signage's **Bus
Shelter Ad Panel** (`ad_shelter_panel`, `ADVERTISING_SYSTEM.md`), which the player sets against
that frame from outside the shelter: a 1 x 2 backlit lightbox in the next block, its back lip
reaching the pixel into this one that the shelter leaves clear, one ad facing along the pavement
and one facing into the shelter. Transit and Signage never refer to each other; they meet only in
the world, so a shelter without Signage installed simply has an empty frame.

### Liveries

The geometry is drawn once per style in CITYLINE's colours; every other agency's model of a part
that shows livery is a child model naming its own `frame` and `fascia` textures (`fascia_slim` on
the flat roof). The fascias carry the agency's name and the stripe its flag uses: CITYLINE teal
with yellow, RIVERWAY navy with orange end bars, VERDANT green with white lines, EMBERLINE red on a
graphite frame.

### Traps

- **The collision boxes share the generator's numbers.** `BusShelterStyle.collision` and
  `gen_transit_shelters.py` describe the same walls, bench, posts and roof; change one, change
  both.
- **The ad panel slot is 1.25 px of nothing.** The glass shelter's back row stops short of its
  left end so Signage's panel can put its back lip there; drawing anything in that sliver makes
  the two fight.
- **Only shelters of the same block join.** A glass CITYLINE shelter beside a glass RIVERWAY one
  is two shelters with two sets of end walls, which is right for two agencies' shelters but
  surprises a builder mixing liveries in one run.
- **A shelter is placed as a door is.** Set by command or by a script, both halves must be set,
  the upper with its bit (4), and the lit bit (8) on the upper half only.

### Prices

`TransitFabricatorRules`: a glass shelter is a pole section, two sheet metal, four glass panes and
an LED module; the cantilever a pole section, two sheet metal and an LED module; the flat roof a
pole section, one sheet metal and an LED module. `audit_fabricator_costs.py` mirrors them.

## Departure boards

Two blocks in `transit.board`, both `BlockBusBoard`, drawn by `gen_transit_boards.py`: the **Bus
Departure Board**, the big concourse board a bus station has, and the **Bus Bay Display**, a small
amber LED sign hung over one bay. Bus only: a train's departure board is RCMC's, and the airport has
its own flight information boards.

### What a board lists, and why it agrees with the stops

A board has nothing to be linked to. It looks for the stops around it (`BusStation`): every bus
stop flag within two chunks of the board's chunk and twelve blocks above or below its sixteen-block
band of height. Each flag is a **bay**, numbered 1 up in world order (west to east, then north to
south, then bottom to top) so every board and bay display looking at the same stops numbers them
alike, whichever way it faces; at most sixteen are kept. A board lists the next two buses of every
route plate of every bay, soonest first (then by bay, then by route): route, destination, bay and
minutes, DUE under a minute.

The buses come from `BusDepartures`, the arrival display's timetable moved out of its renderer so
the three share it: a route's headway and destination are fixed by its number, and its phase by the
world clock and the column of the stop's post. So a board agrees to the minute with the flag's
plates and with the arrival display on the same post, and a bay display with both. This was checked
in game: at one moment the arrival display on bay 1's post read 12 RIVERSIDE 7 MIN, 40 NORTH END
7 MIN, 40 NORTH END 15 MIN, and the board across the concourse listed the same three buses at bay 1.

Invented throughout: the agencies are the flags' four, the destinations the arrival display's
sixteen generic ones.

### Banks, pages and cycling: the flight board's way, not build-to-size

The plan proposed a build-to-size board, a controller with parts and no tile entity on the parts,
as the ad boards are. It was not done. A departure board is a row of screens in life, and the flight
information board had already shown the simpler way: every block is one monitor, and monitors of the
same block facing the same way side by side and stacked are a **bank**, read like text, left to right
and then down, each monitor showing the page after the one to its reader's left or above. That needs
no controller, no part blocks, no rules for breaking a multi-block and no drawing one quad across
blocks, and a bank of any shape grows or shrinks by placing or breaking a monitor. The cost is a
small tile entity on every monitor, which saves two bytes (below). The two kinds of board behave
alike, so a player who has built one has built the other.

Eight departures a page. When a station has more departures than the bank has rows, the whole bank
turns to the next set every ten seconds (`CYCLE_TICKS`), and each header shows which set ("2/3").

### Setting a board up

A click steps which agency the board lists: every agency, then CITYLINE, RIVERWAY, VERDANT and
EMBERLINE; the title follows ("BUS DEPARTURES", "EMBERLINE DEPARTURES"). A sneaking click turns
spoken announcements on or off. Either is set on every board of the bank at once (a flood fill, at
most 64), and the action bar says what it now is. That is all a board has to set, so there is no
screen. `TileEntityBusDepartureBoard` saves them as `f` (0 every agency, else 1 + the agency's
ordinal) and `a`; the baked model reads neither, so a change never rebuilds the chunk section
(`getBakedModelKey` is 0). The bay display has nothing to set.

### Drawing

Both follow the flight board. The **departure board** is a slim landscape monitor against the back
of its block, on a wall or on two rods to the block above (`hung`, actual state: no solid face
behind and no board above). The screen's texture (a header band with a bus pictogram on amber, the
column heads' band and the rows' bands) is baked, and `TileEntityBusDepartureBoardRenderer` draws it
again a hair in front, fullbright, so the board glows at night, and every word on it from display
lists shared by every board (`CsmSharedDisplayLists`, one cache, `bus_board_text`): route numbers,
destinations, bays, minute readings, the five titles, the column heads, the set count and the two
empty-board notices ("NO BUS STOPS NEARBY", "NO DEPARTURES LISTED"), each laid out in its column in
the font's units when compiled. A row is one translation and four list calls, the route in its
agency's colour (`BusAgency.getTextColour`), the minutes in amber and DUE in green. The lists hold
geometry only; the font atlas, the colours, the depth mask and the lightmap are set outside them,
every frame ("Display lists: one texture, no cached state").

The **bay display** is a black case with NEXT BUSES printed over a dark LED screen under a hood,
hung on rods or on a wall. It lists the stop nearest it, a flag within four blocks across and six
up or down, found through the same `BusStation`; with none that close it lists the arrival display's
two stock routes. Its screen is exactly the arrival display's size, 12.8 x 4.4, so its renderer
calls the arrival display's own panel (`TileEntityBusArrivalDisplayRenderer.drawPanel`) and the two
share every list.

Both tile entities look their facing, bank and stop up once a second, and `BusStation` walks the
loaded chunks' tile entity maps (never block by block) once every five seconds per chunk and height
band, shared by every board in it; the departures are sorted once a minute of the countdown for each
filter.

### Announcements

With a board's announcements on and the Text to Speech module installed, the player hears "Route 12
to Downtown is now departing from bay 3" as each bus becomes due. `BusBoardAnnouncer` (client only)
speaks through Core's `CsmTts`, the service the Text to Speech module registers its engine with, so
Transit never names that module. Without it the board is silent: nothing is said unless an engine is
registered and loaded (`CsmTts.isReady()`), rather than falling back to the system narrator as a
player-built Redstone TTS block would, because a board talks by itself. Once a second the first
board of each announcing bank within 24 blocks of the player queues every due bus not yet called; a
bus is known by its stop, route and minute, so two boards over the same stops do not both call it.
The queue is read one message every six seconds, since the engine drops a message that arrives while
it is still speaking, and a message older than thirty seconds is dropped rather than read late. The
speech is English, as the voices are. Announcements are off on a new board.

### Measurements

Measured in one session in MKTNG: boards hung in an 8 x 8 grid two blocks apart facing the camera,
every one listing the test scene's four bays (22 departures), daylight pinned, frame rate uncapped,
samples in the order empty, 16, 64, empty, 64, 16, empty, eight seconds each. The empty frame was
1.21-1.24 ms throughout.

| Block | Frame at 16 | Frame at 64 | Per board from the frame | Profiler, per board |
|---|---|---|---|---|
| Bus departure board | 1.43 / 1.56 ms | 1.89 / 2.31 ms | 11-17 µs | 15.7 µs |
| Flight information board (for comparison) | 1.39 / 1.45 ms | 2.00 / 2.07 ms | 12-13 µs | 12.0 µs |
| Bus bay display | 1.27 / 1.30 ms | 1.48 / 1.58 ms | 4-6 µs | 3.5-4.6 µs |

A departure board costs a little more than a flight board because it draws eight rows of four lists
against the flight board's seven of two. Beyond 48 blocks nothing is drawn but the baked model.

### Traps

- **The screens stand 0.4 proud of their bezels** (`SCREEN_Z` 14.6), as the flight board's does, so
  `model_depth.separate` leaves them where the renderers draw. See the Airports traps.
- **The bay display's screen must stay 12.8 x 4.4.** The arrival display's panel lists are laid out
  for that width; a bigger screen would need lists of its own.
- **The countdown is seeded by the post's column, not the block.** A board works out a stop's buses
  at its flag, and the arrival display at itself, lower down the same post; seeding by height would
  put the two a few minutes apart.
- **Bays are numbered among the stops a board can see.** Two boards far enough apart to see
  different sets of stops (more than about two chunks) can number the same stop differently. Keep a
  station's stops and boards within a couple of chunks of each other.
- **The agency comes from the flag's registry name** (`BusAgency.ofRegistryName`, the suffix after
  the last underscore); a new agency's flag must end in its id, and the enum keeps the generators'
  order.
- **The board and the generator share the screen's numbers.** `SCREEN_*`, `HEADER_H`, `COLHEAD_H`,
  `ROW_PITCH`, `ROWS`, `TITLE_X` and the 256 x 189 window are in both
  `TileEntityBusDepartureBoardRenderer` and `gen_transit_boards.py`; the bay display's `BAY_SCREEN_*`
  in `TileEntityBusBayDisplayRenderer`. Change one, change both.

### Prices

`TransitFabricatorRules`: both as the arrival display, an LED module, a control board and sheet
metal. `audit_fabricator_costs.py` mirrors the branch.

## Station and platform fit-out

Twenty-nine blocks in `transit.platform`, all drawn by `gen_transit_platforms.py`, with three
sounds from `gen_transit_sounds.py`.

### Made to complement RCMC

The set is designed to fit out stations for **RCMC** (Rails & Coasters: Minecraft), the author's
own train mod, and never to repeat it. RCMC already has the station itself: its **Station
Platform** and **Platform Edge** blocks (decking laid at exactly a metro car's floor height, the
edge carrying its own tactile strip), the Station Line Map sign, the Arrival Board, the Station
Speaker, the operator panel and line desk, and spline track with catenary, switches and
signalling. So Transit has no platform or edge block, no speaker, no arrival board for platforms
(its departure boards, below, are for bus stops and bus concourses), and nothing laid
along the track: RCMC's track is splines, not blocks, and a block-aligned third-rail cover or
buffer stop would clash with it. What is here is the fit-out an RCMC station lacks.

**Clearance.** A metro car in RCMC is 3.8 blocks wide (1.9 either side of the track's centre)
and its roof stands about four blocks above the platform surface. Keep furniture (benches, bins,
help points, validators, columns) at least a block back from the platform edge, hang signs and
clocks over the platform rather than the track, and keep a canopy over the platform: nothing here
checks, so a sign hung over the track will be driven through. The tactile paving is the one piece
meant for the edge itself: a one-pixel overlay, below any car.

| Block | Registry name | Class |
|---|---|---|
| Tactile Paving (Warning or Guidance; Yellow or Grey) | `csm:tactile_warning_yellow` and the other three | `BlockTactilePaving` |
| Platform Bench | `csm:platform_bench` | `BlockPlatformBench` |
| Platform Perch | `csm:platform_perch` | `BlockPlatformRun` |
| Help Point | `csm:platform_help_point` | `BlockPlatformHelpPoint` |
| Station Emergency Point | `csm:station_emergency_point` | `BlockPlatformHelpPoint` |
| CCTV Dome Camera, CCTV Camera (Wall Bracket) | `csm:platform_cctv_dome`, `csm:platform_cctv_camera` | `BlockPlatformFixture` |
| Platform Clock | `csm:platform_clock` | `BlockPlatformClock` |
| Platform Litter Bin (Clear Bag) | `csm:platform_litter_bin` | `BlockPlatformFixture` |
| Platform Number Sign | `csm:platform_number_sign` | `BlockPlatformNumberSign` |
| Hanging Sign (To Trains, Exit, Line Bullets) | `csm:platform_sign_to_trains`, `_exit`, `_lines` | `BlockPlatformFixture` |
| Gap Warning Sign | `csm:platform_gap_sign` | `BlockPlatformFixture` |
| Station Name Sign | `csm:station_name_sign` | `BlockStationNameSign` |
| Network Map Board | `csm:station_network_map` | `BlockPlatformFixture` |
| Station Wall Tile (White, and a Band in each agency's colour) | `csm:station_tile_white`, `csm:station_tile_band_<agency>` | `BlockStationTile` |
| Platform Column (Tiled, Steel) | `csm:platform_column_tile`, `csm:platform_column_steel` | `BlockPlatformColumn` |
| Platform Column (Number Band) | `csm:platform_column_number` | `BlockPlatformColumnNumber` |
| Platform Canopy | `csm:platform_canopy` | `BlockPlatformCanopy` |
| Ticket Validator | `csm:platform_validator` | `BlockPlatformValidator` |

### Tactile paving

One-pixel overlays laid on any solid floor, as vanilla carpet is, RCMC's platform decking
included: truncated warning domes and directional guidance bars, in yellow and in grey with
stainless studs. The class follows Building's floor finish, copied rather than shared (a module
names only Core and the modules it requires): it stores only `axis`, the way the player faced,
and comes up when its floor goes.

A floor of it shows no repeat. Each texture is drawn twice, every dome or bar a little different,
and the blockstate picks one of the two drawings and a turn per block position. The domes sit
centred in 2 px cells on a square grid, so a quarter turn leaves the grid where it was; the bars
run the way they were laid and turn only end for end, their segments on a 32-texel period that
divides the texture, so a run of blocks has no seam.

### Furniture

Every piece faces the player who places it; a wall piece has the wall behind it.

- **Bench** (`BlockPlatformBench`): two perforated steel seats a block on a beam on one
  pedestal, with armrests between and at the ends. Benches side by side facing the same way join
  (`left`, `right`, actual state): the beam runs through, an armrest stands at every joint, and
  the end armrests are drawn only at the bench's ends. A click sits the player in the nearer seat
  through Core's `EntityCsmSeat`, one person a seat. It is a platform bench, not Parks' timber
  park bench.
- **Perch** (`BlockPlatformRun`): a lean rail, a padded bar tilted forward over a stainless rail
  on one post a block, joining the same way, capped only at its ends.
- **Help point** (`BlockPlatformHelpPoint`): a column two blocks tall under a lit HELP POINT
  header (light 4), with a green information button over a speaker grille and a guarded red
  emergency button. A click plays `help_point_chime`, three struck bars rising, and says on the
  action bar who is answering; a click on the front below 12.6 px (`HELP_SPLIT_Y`) is the
  emergency button, and its chime is pitched lower. Both buttons are in the column's lower block
  on purpose: a click reaches only the block the player's aim passes through, so anything drawn
  in the space above (the header) cannot be clicked. The **emergency point** is a red wall cabinet
  (a lit EMERGENCY header, a phone handset, an extinguisher behind glass) whose every click is an
  emergency call. Nothing is sent anywhere: a help point is a place, not a service.
- **CCTV**: a dome on a short pipe from the ceiling, and a bullet camera on a wall bracket tilted
  down the platform. Decoration.
- **Clock** (`BlockPlatformClock`, `TileEntityPlatformClock`,
  `TileEntityPlatformClockRenderer`): a double-faced clock hung from the ceiling, its drum and
  dials baked, its hands the renderer's -- the world's time on both dials, four untextured quads
  a frame, the Residential wall clock's approach. The dials stand the same distance either side
  of the block's middle, so the back dial is the front one turned half round.
- **Litter bin**: the clear-bag bin stations use, a steel hoop on a post holding a see-through
  bag (translucent layer) under a lid ring.

### Signs

- **Platform number sign** (`BlockPlatformNumberSign`): PLATFORM over a big number, hung on two
  rods, printed on both faces. A click steps the number 1 to 20 and round, a sneaking click back;
  the action bar says the new number. The number is kept by `TileEntityPlatformSign` and read as
  actual state (`number`), and the blockstate swaps the face texture: the aisle sign's pattern,
  shared with the column band and the name sign through `PlatformSigns`.
- **Hanging wayfinding signs**: TO TRAINS and EXIT with an arrow, and a strip of line bullets
  (1, 4, 7 and E, in the four agencies' colours). The back face is read mirrored so it reads the
  right way round, and a sign with an arrow has its own back art with the arrow reversed, so from
  either side it points the same way in the world.
- **Gap warning**: a yellow wall sign, CAUTION, a figure stepping from the platform into the car,
  STEP OVER THE GAP. The wording is invented, as every phrase here is.
- **Station name sign** (`BlockStationNameSign`): a porcelain-enamel panel, white on navy, set on
  the wall. A click steps through ten invented names (Alder Park, Civic Square, Foundry Row,
  Harbor Lights, Kestrel Hill, Lantern Quay, Millstone, Orchard End, Saxton Cross, Willow Bend),
  kept in the same order in `BlockStationNameSign.NAMES` and the generator's `STATION_NAMES`.
- **Network map board**: a framed wall map of a generic network, four lines in the agencies'
  colours over a river and a park, interchanges, a "you are here" dot and a legend of bullets,
  with no place names: the city is whatever the player builds.

### Architecture

- **Station wall tile** (`BlockStationTile`): full blocks of glazed subway tile in running bond,
  plain white, or with a frieze course in CITYLINE teal, RIVERWAY orange, VERDANT green or
  EMBERLINE red between two dark liners, on the sides at the same height on every block, so a
  row is one unbroken band.
- **Columns** (`BlockPlatformColumn`): tiled square or painted steel octagon, one block of shaft
  at a time. Stacked, they read as one column: `up` and `down` (actual state) draw the plinth
  only at the foot and the capital only at the head. The **number band column**
  (`BlockPlatformColumnNumber`) is the tiled column with a navy band carrying PLATFORM and its
  number on all four faces, stepped by clicking like the number sign; its band is one model per
  number (children of band 1 naming their texture), picked by the multipart blockstate.
- **Canopy** (`BlockPlatformCanopy`): a steel roof one block at a time, set on columns (the deck
  lies at the foot of its block, so it rests on the column below). Canopies join on all four
  sides, the fascia drawn only round the outside (world sides, actual state); a light strip on the
  white soffit runs along `axis`, the way the player faced, and gives light 11.

### The ticket validator

`BlockPlatformValidator` is a tap post for a platform or a station without gates. Held to it, a
Fare Ticket is used up and a Transit Card gives up one trip, exactly as at a fare gate (the same
items and `ItemTransitCard.consumeTrip`); the screen shows a green tick and `validator_accept`
beeps, or a red cross and `validator_deny` sounds for a card with no trips or anything else, and
the action bar says why. The screen (`light`: 0 idle, 1 accepted, 2 refused, stored in the two
bits above the facing) goes back to idle after 30 ticks by a scheduled tick; lit, it gives light
5. Validating opens nothing: pairing a validator with a gate or RCMC's doors is the builder's
business.

### Traps

- **Tactile paving is an overlay, not a floor.** It stands on the floor block, in the block above
  it, as carpet does; set in place of the floor it leaves a hole under a one-pixel plate.
- **A click reaches only the block the aim passes through.** The help point's buttons and the
  validator's screen are in the lower block for that reason; anything drawn in the space above a
  block cannot be clicked.
- **The name list is written twice.** `BlockStationNameSign.NAMES` and the generator's
  `STATION_NAMES` must stay in the same order: the saved value indexes both.
- **Keep clear of the train.** Nothing checks RCMC's clearance (Made to complement RCMC, above):
  a sign or a clock hung over the track is driven through.

### Prices

`TransitFabricatorRules`: tactile paving and wall tile a concrete mix; the help and emergency
points a control board, a sounder driver and sheet metal; CCTV an optical sensor and sheet metal;
the validator a control board, an LED module and sheet metal; the clock a control board and sheet
metal; the canopy two sheet metal and an LED module; a column a pole section and a concrete mix;
the signs a sign blank and a fastener kit; the bench, perch and bin two sheet metal and a
fastener kit. `audit_fabricator_costs.py` mirrors the branches.

## Stations

Thirteen blocks in `transit.station`, all drawn by `gen_transit_stations.py`: a subway entrance
built to size, an open stair entrance with lit globe lamps, the fare line's railing and service
gate, line bullets and the station agent's booth counter. No sounds. Like the platform fit-out,
the set complements RCMC rather than repeating it.

| Block | Registry name | Class |
|---|---|---|
| Station Entrance Glass | `csm:station_entrance_glass` | `BlockStationGlass` |
| Station Entrance Roof (CITYLINE, RIVERWAY, VERDANT, EMBERLINE) | `csm:station_entrance_roof_<agency>` | `BlockStationEntranceRoof` |
| Station Entrance Globe Lamp | `csm:station_entrance_globe` | `BlockStationGlobe` |
| Station Entrance Railing, and with a Name Plate | `csm:station_entrance_railing`, `_railing_sign` | `BlockStationRailing`, `BlockStationRailingSign` |
| Fare Line Railing, and with a Paid Area Plate | `csm:station_fare_railing`, `_railing_sign` | `BlockStationRailing` |
| Fare Line Service Gate | `csm:station_service_gate` | `BlockStationGate` |
| Line Bullet | `csm:station_line_bullet` | `BlockStationLineBullet` |
| Station Agent Booth Counter | `csm:station_booth_counter` | `BlockStationBoothCounter` |

### What RCMC already has, and what is left out

RCMC has its stations' platforms and edges, the Station Line Map sign (a post-mounted diagram of
one line's stops, drawn from its line registry), arrival boards, station speakers, the coaster
operator panel and the metro line desk. So there is **no line diagram strip** here: it was on the
list, and it would be RCMC's line map sign drawn again without the data behind it. There are
**no "to trains" or "to street" signs** either: the platform fit-out's hanging TO TRAINS and EXIT
signs already say both, and the author asked for no more exit signage (Life Safety has plenty).
The **booth counter** is kept because RCMC's panel and desk run the trains; a booth is where a
station sells and checks fares, and RCMC has no block for that.

### The entrance: a kiosk from pieces, and an open stair

Two entrances, because the two real kinds are built differently and each is simple:

- **The kiosk** is built block by block from two pieces, in the container's pattern
  (`BlockSiteShell`: each block draws its outside only where its neighbour is not part of the
  same thing), not the job trailer's. The trailer has to tell inside from outside by looking up
  and down eight blocks for a floor and a roof, because its walls look different on each side. A
  kiosk's walls do not: glass is glass from both sides, and a roof's only outside is its edge. So
  nothing looks further than the next block, nothing is stored but the name board, and no render
  updater is needed.
  - **Glass** (`BlockStationGlass`) joins as a pane does, toward more glass, a booth counter whose
    run lies that way, or a solid face (`north`..`west`, actual state). A slim post stands at
    every block, a mullion a metre, which is how a kiosk's framing reads. `up` and `down` say
    whether the wall carries on, so the head rail and the stainless kick plate are drawn only
    where it stops: two blocks of glass are one pane. A lone block is drawn as a panel along x.
    Translucent layer. Collision is the pane's, post and arms, a block tall.
  - **The roof** (`BlockStationEntranceRoof`, one per agency) follows the platform canopy's rule:
    a deck at the foot of its block, so it rests on the glass below, joining on all four sides,
    with the agency's fascia (its colour and accent line) drawn only round the outside. It joins a
    roof of any agency. A square soffit light is always on (light 11).
  - **The name board**: a click with an empty hand steps a roof block's board through none,
    SUBWAY, METRO and the station name sign's ten names (`StationLegends`); a sneaking click
    goes back. The board stands on the fascia on every outside side of that block, so set the
    middle of the front to the station's name and the corners to SUBWAY. The value is the
    platform signs' `TileEntityPlatformSign`, read as `legend` (1 is no board). The roof has no
    facing: which side is the front is only which side a board is wanted on.
  - The **stair** is vanilla stairs dug down under the kiosk: the walls stand on the rim of the
    stair well, open at the front.
- **The open stair** has no roof at all, only a railing round the well and a lamp either side of
  the mouth. The **railing** (`BlockStationRailing`) is a vanilla fence (it joins other iron
  railings, the service gate, the globe lamp and solid faces, collides a fence's 1.5 high, and
  takes a lead), painted cast iron with a ball on each post. The **name plate** railing
  (`BlockStationRailingSign`) is the same with a plate on both faces, stepped through the same
  legends (SUBWAY first). The plate is drawn only on a straight run, since a plate across a
  corner has nowhere to hang.
- **The globe lamp** (`BlockStationGlobe`) serves both: a cast-iron post with an opal globe, two
  blocks tall from one block, always lit. Transit may not use Lighting's light logic, so, like the
  airfield lights, the globe glows from its texture (an `_e` companion for OptiFine, faces
  unshaded) and the block gives light 14. A click steps it green, red, white (in the metadata).

### The fare line

- **Fare line railing**: the same fence class in brushed stainless at 16, the fare gates' cabinet
  height, with rails and balusters on two-pixel centres. It **joins the fare gates**. A gate stands
  one block up with its cabinet drawn down into the floor-level cell, which is air, so the railing
  looks past it for a gate whose box reaches down over that cell, directly above it or up to two
  further along (the ADA gates reach past their cell), and whose lane runs across the railing.
- **Paid Area plate**: the stainless railing with a yellow PAID AREA / FARE REQUIRED plate on
  both faces, again only on a straight run. No floor marking was made: a stencil on one block does
  not read, and floor paint is the road mod's business.
- **Service gate** (`BlockStationGate`): a vanilla fence gate, in the picket gate's pattern. A
  click opens it away from the player, redstone opens it, open it is walked through, and the
  railings join it. It is stainless with a SERVICE GATE plate on both faces; open, the leaf swings
  a quarter about its hinge and reaches past its block, as a real leaf does. It checks no fare:
  which side is paid is the builder's business. It is not labelled as an exit (no exit wording,
  as above).

### Line bullets

`BlockStationLineBullet`: a round enamel plate on the wall, a disc in the line's colour with a
white rim and its letter or number. A click steps through sixteen invented lines, a sneaking click
back (`PlatformSigns`, the value in `TileEntityPlatformSign`). The first four are the platform line
strip's (1, 4, 7, E in the agencies' colours), each with a sister line in the same colour (2, 5, 8,
F), then A and C (blue), K (purple), M (magenta), S (grey), T (brown), X (yellow, black glyph)
and Z (navy). The plate is an octagon set in a little behind a round face texture whose corners
are clear, so it reads round; its edge reads a patch of the disc's own colour.

### The booth counter

`BlockStationBoothCounter`, facing the player who placed it (the customer's side): a stainless
front with the STATION AGENT plate, a ledge with the deal tray, glass from the ledge up with a
speaking grille, and the agent's shelf behind. It is one straight run across its block, in line
with the entrance glass, so glass stacked on it is the booth's window and glass beside it the
booth's walls; a roof (or a ceiling) and a door close it. It does nothing: the fare machine sells.

### Traps

- **A plate on a railing must be thicker than the post.** The first plates were thinner than the
  entrance railing's post, which cut through the middle of the name ("SU|BWAY"). They are 2.6
  thick now, round a 2.2 post.
- **The lists are written twice.** `StationLegends.LEGENDS` (built from
  `BlockStationNameSign.NAMES`) and the generator's `LEGENDS` (from `STATION_NAMES`) must agree in
  order; so must `BlockStationLineBullet.LINES` and the generator's `LINES`, and
  `BlockStationGlobe.Colour` and `GLOBES`.
- **The roof's `legend` counts one more than the railing's.** Roof 1 is no board and 2 is
  SUBWAY; railing 1 is SUBWAY. The generator's board and plate models are numbered by legend, the
  rules by value.
- **The swung gate keeps its plate's uv.** The open leaf's faces take their uv from their new
  place, except the plate's picture, which keeps its window (north turns to west, whose u runs the
  same way over the swung leaf).
- **A fascia's top reads one flat texel.** Two fascias overlap at a roof's corner; reading one
  texel makes the overlap one colour, so it cannot be seen to fight (the octagon caps' trick).

### Prices

`TransitFabricatorRules.station`: glass a sheet metal and two glass panes; a roof two sheet metal
and an LED module; the globe lamp a pole section, a lens assembly and an LED module; a railing a
sheet metal and a fastener kit, with a sign blank more for a plate; the service gate two sheet
metal and a fastener kit; a line bullet a sign blank; the booth counter two sheet metal, two glass
panes and a fastener kit. `audit_fabricator_costs.py` mirrors the branch.

## Airports

Twenty-seven blocks and one item in `transit.airport`, and the large hanging sign in
`transit.wayfinding`, all drawn by `gen_transit_airport.py`, with one sound (`kiosk_print`) from `gen_transit_sounds.py`. Every airline, flight number and city is
invented, and every pictogram generic (a plane, a suitcase, a bus, a taxi).

### Terminal only

These are the pieces inside a terminal: check-in, the queue, security, the gate, the boards,
baggage claim and carts. There are no aircraft; the airside pieces (jet bridges, ground equipment
and airfield lights) are below, under Airside. Two things a terminal needs come from other modules rather
than being drawn again:

- **The walk-through metal detector** is Life Safety's (`metal_detector`, in Emergency Services),
  which already works: it alarms and lights red on a player carrying metal. An airport one was
  built and cut before release, because it was the same block under the same name.
- **Duty-free and cafe fit-out** is Furnishings' Market & Store tab (shelving, coolers, the
  checkout, the counters).

Transit cannot name either module (it may reference only Core and Roads), so these meet only in
the world: an install without Life Safety simply has no detector.

| Block | Registry name | Class |
|---|---|---|
| Check-In Desk | `csm:airport_checkin_desk` | `BlockCheckinDesk` |
| Bag Drop Scale | `csm:airport_checkin_scale` | `BlockAirportCounter` |
| Gate Desk | `csm:airport_gate_desk` | `BlockAirportCounter` |
| Self Check-In Kiosk | `csm:airport_self_checkin_kiosk` | `BlockSelfCheckinKiosk` |
| Queue Stanchion (Black Belt, Blue Belt) | `csm:airport_queue_stanchion_black`, `_blue` | `BlockQueueStanchion` |
| Security X-Ray Scanner | `csm:airport_xray_scanner` | `BlockSecurityLine` |
| Security Roller Conveyor, Divesting Table | `csm:airport_security_roller`, `csm:airport_divest_table` | `BlockSecurityLine` |
| Security Trays (Stack), Security Tray (With Belongings) | `csm:airport_security_trays`, `csm:airport_security_tray_items` | `BlockSecurityTray` |
| Boarding Pass Scanner | `csm:airport_boarding_pass_scanner` | `BlockBoardingPassScanner` |
| Airport Seating (Black, Blue) | `csm:airport_seating_black`, `_blue` | `transit.platform.BlockPlatformBench` |
| Flight Information Board (Departures, Arrivals) | `csm:airport_flight_board_departures`, `_arrivals` | `BlockFlightBoard` |
| Large Flight Board (Departures, Arrivals) | `csm:airport_flight_board_large_departures`, `_arrivals` | `BlockFlightBoardLarge` |
| Baggage Carousel | `csm:airport_baggage_carousel` | `BlockBaggageCarousel` |
| Gate Sign | `csm:airport_gate_sign` | `BlockGateSign` |
| Airport Sign (Gates, Arrivals, Check-In, Baggage Claim, Ground Transport) | `csm:airport_sign_<name>` | `transit.platform.BlockPlatformFixture` |
| Large Hanging Sign | `csm:airport_wayfinding_panel` | `transit.wayfinding.BlockWayfindingPanel` |
| Luggage Cart | `csm:airport_luggage_cart` | `transit.platform.BlockPlatformFixture` |
| Luggage Cart Rack | `csm:airport_cart_rack` | `transit.platform.BlockPlatformRun` |
| Boarding Pass (item) | `csm:boarding_pass` | `ItemBoardingPass` |

Every model faces north as the platform fit-out's do: a counter's customer side, a sign's front
and a monitor's screen look north, a wall piece has its wall at z = 16, and every piece faces the
player who places it. The element helpers are `gen_transit_platforms.py`'s.

### The schedule

`FlightSchedule` is the whole airport's timetable, and it is a function of the world's clock and
nothing else: it saves nothing, sends nothing, and every board, kiosk and scanner in the world
agrees. Each list (departures, arrivals) has a flight every 20 minutes of the day, 72 a day, the
same flights every day as an airline timetable repeats. A flight is made from its slot's place in
the day by a hash: its airline (one of four: KESTREL AIR, LANTERN AIRWAYS, WILLOWJET, SAXTON
AIRLINES), number, city (sixteen invented, PORT VESTA to ST AUBREN), gate (A1 to D20), its time a
few minutes into its slot, and now and then a 30-minute delay (about one in nine) or a
cancellation (about one in thirty). A slot is counted from the world's first 6:00 as a `long`, so a
boarding pass names its flight by slot for as long as anyone keeps it.

The schedule runs on the **world's time of day**, the one the platform clock shows, so the boards
agree with the clocks and the sun. That makes an airport busy: a game hour is 50 real seconds, so
a flight leaves every 17 seconds and the rows roll up the board about that often. The statuses
follow from the minutes left: BOARDING under 45, FINAL CALL under 20, GATE CLOSED under 10, then
DEPARTED for ten minutes; arrivals are ON TIME, LANDING under 15, LANDED for twenty. A world with
the daylight cycle off stops the boards, as it stops the clocks.

### The flight information boards

`BlockFlightBoard` is a slim landscape monitor against the back of its block: on a wall, or on two
rods to the block above when there is no wall behind it (`hung`, actual state; not under another
board). Its bezel and the screen's texture (the DEPARTURES or ARRIVALS header and the row bands)
are baked. `TileEntityFlightBoardRenderer` draws the rest, a board being one quad and at most
sixteen list calls a frame:

- **The screen, lit.** The same texture again, a hair in front, fullbright, one quad from the
  block atlas, so a board glows at night like a screen rather than going dark with the room.
  Beyond the renderer's range (48 blocks) only the baked screen shows.
- **The text**, all display lists shared by every board (`CsmSharedDisplayLists`), laid out in
  their columns in the font's units when compiled: a row's white part (time, flight, city, gate)
  keyed by the flight's place in the day (at most 144 lists over both boards), each remark in its
  colour (nine), the column heads (two) and the clock in the header (one per minute of the day).
  Following "Display lists: one texture, no cached state", the lists hold geometry only; the font
  atlas, the colours, the depth mask and the fullbright lightmap are set outside them.
- **Pages.** Boards of the same kind facing the same way side by side and stacked are a bank, read
  like text: left to right along a row, then the next row down. Each board lists seven flights
  from its page, the number of boards to its reader's left plus the whole rows above it, so a bank
  of six lists the next 42 flights. `TileEntityFlightBoard` looks the page and facing up once a
  second, up to eight boards each way.

The screen's place and the bands (`SCREEN_*`, `HEADER_H`, `COLHEAD_H`, `ROW_PITCH`, `ROWS` and the
256 x 148 window) are in both the generator and the renderer: change one, change both.

### The large flight boards

Under a terminal ceiling fourteen blocks up the one-block board reads as a dot, so there is a
large one beside it (the small board is unchanged): `airport_flight_board_large_departures` and
`_arrivals`, `BlockFlightBoardLarge`, a screen built to size from cells.

- **Building it.** Cells of one board placed side by side and stacked, facing the same way (each
  faces the player who places it), join into one screen from one cell up to 8 wide and 5 tall
  (`MAX_WIDTH`, `MAX_HEIGHT`). The rules are the large hanging sign's, through the same code
  (`transit.panel.CellPanel` and `PanelLayout`, factored out of the sign): the bottom-left cell as
  read is the controller, its row's run to the reader's right the width and its column's run up
  the height, and a cell outside that rectangle is drawn as a plain bezel. Departures and arrivals
  are two blocks and never join each other. Nothing is set up and nothing is saved, so a script
  places them with `/setblock` (facing in the metadata) and needs no NBT.
- **The model.** A cell is a dark bezel slab against the back of its block (z 13..16 facing
  north) with a graphite frame on the board's outer edges only, the same four `edge_*` actual-state
  booleans as the sign (4 x 16 states). Nothing of the screen is baked: past 96 blocks only the
  bezel shows. The item shows a cell with the small board's screen on it.
- **Hanging.** Against a wall the board just sits on it. With nothing behind a column's top cell,
  the renderer hangs rods from the board's top to the first block above, as the sign's are (two, or
  one for about every four blocks, at most 24 blocks up, none with a block right on top).
- **Scale.** `TileEntityFlightBoardLargeRenderer` draws the small board bigger: the same header,
  column-head band, row pitch and text heights in the same proportions, times
  `k = 2 (height / 2)^0.75`: 1.2 for one block tall, 2 for two (the text twice the small
  board's), 2.7 for three, 4 for five, so a taller board has both bigger text and more rows. A
  board narrower in proportion than the small board at that scale is scaled down to fit its
  columns. As many rows of the scaled pitch as fit under the heads are drawn, then spread to fill
  the screen: 9 on a 2 x 1 board, 13 on a 4 x 2, 15 on a 6 x 3, 18 on an 8 x 5. (A one-wide column
  of cells is width-bound: tall and thin, it lists up to 96 rows of small text.) Extra width
  goes to the city column; time and flight keep the left, gate and remark the right, each as far
  from its edge as on the small board.
- **Pages.** A page is `rows` flights from `page x rows` in the list, as a bank of small boards
  pages. A board whose page holds fewer than 24 flights (`PAGE_FLIGHTS`) turns through enough pages
  to list 24, one every eight seconds of world time (`PAGE_TICKS`, total time, so a board pauses
  with the game), all boards in step, with PAGE n/m in the header beside the clock: a 4 x 2 board
  turns two pages of 13, a 2 x 1 three of 9.
- **The screen** is the small board's texture (`fids_departures`, `fids_arrivals`) cut into its
  bands rather than stretched whole: the header's left part (the plane and the title) at its own
  proportions, the rest of the header, the column heads' band and the two zebra row bands stretched
  across, fullbright, one draw. No texture is added.
- **The text.** Display lists shared by every large board, each compiled in the font's units from
  x = 0 and placed and scaled outside, so one list serves every size: a row's time and flight
  (keyed by the flight's place in the day), its city (and by its room, in steps of eight font
  units, as the city is cut to fit), its gate, its remark, the two halves of the column heads, the
  clock and the page. Geometry only, as "Display lists: one texture, no cached state" asks. The
  renderer is global (a board eight wide reaches past its controller's section) and draws to 96
  blocks.
- **What it costs.** A non-controller cell, two block lookups a frame. A controller, one draw for
  the screen and four list calls a row: about 55 on a 4 x 2 board.

### Check-in and the kiosk

- **Check-in desk** (`BlockCheckinDesk`): a customer ledge over a laminate front and a stainless
  kick plate, the agent's work top, monitor and bag-tag printer behind, and a lit airline panel on
  a post. A click with an empty hand steps the airline, a sneaking click back; the airline is kept
  in the two metadata bits above the facing, so the desk has no tile entity.
- **Bag drop scale** (`BlockAirportCounter`, family `checkin`): a low weighing belt between two
  stainless guards, running into a rubber curtain under a hood, its weight read out on the
  customer's side. Desks and scales facing the same way join into one counter (`left`, `right`):
  a desk draws its end panel only where the run stops, so desk, scale, desk reads as one.
- **Gate desk** (`BlockAirportCounter`, family `gate`): the same counter in charcoal with a lit
  strip and a boarding pass reader set in the ledge; gate desks join with each other only.
- **Self check-in kiosk** (`BlockSelfCheckinKiosk`): a pedestal with a tilted touchscreen over the
  card and passport readers and the pass slot, under a lit CHECK-IN header on a stem. A click
  prints a **Boarding Pass** for one of the next six departures at least 45 minutes off and not
  cancelled, with a seat, and plays `kiosk_print`. The screen and slots are in the lower block
  on purpose: a click reaches only the block the aim passes through, so the header above cannot be
  clicked (the help point's lesson).

The **Boarding Pass** (`ItemBoardingPass`) carries only its flight's slot, a seat and whether it
has been used (`s`, `seat`, `b`); the tooltip works the flight number, city, time and gate out of
`FlightSchedule`, so a pass always agrees with the boards. One taken from the creative tab has no
flight and says where passes come from.

### The queue

`BlockQueueStanchion`: a weighted base, a chrome post and a belt cassette. Its belt reaches out to
every stanchion beside it (world sides, actual state), whatever the colour of the other's belt, so
a row of stanchions is one barrier. A queue is rows of stanchions with a block of floor between
them, each row stopping a block short where the lane turns. The belts collide up to a fence's
height, so a queue cannot be jumped. The belt's faces map the whole texture top to bottom onto
their 1.8 units, which is how the blue belt keeps its two white edges. There are no diagonal
belts: a belt runs only to the four sides.

### Security

The X-ray unit, roller conveyor and divesting table (`BlockSecurityLine`) all have their tops at
12 sixteenths (`LANE_Y`), so a lane of them reads as one. Pieces side by side join whichever way
each faces, as long as the lane runs along the same axis, and the rollers and the table draw their
end plates only where the lane stops. The **X-ray unit** is a housing with a tunnel through it,
lead curtains at both mouths, an animated belt (`xray_belt`, four frames), a generic radiation
sticker and an operator's monitor on top; it collides to its housing's top (22 sixteenths).

**Security trays** (`BlockSecurityTray`), a stack or one tray with a laptop, a jacket and shoes,
stand on the floor, and set on a roller or the divesting table they drop onto its top (`low`,
actual state): the model and the box reach four sixteenths down into the lane's block, which is
where a player means them to be. Not onto the X-ray, whose housing stands over its belt
(`BlockSecurityLine.hasOpenTop`).

### The gate

- **Boarding pass scanner** (`BlockBoardingPassScanner`): the ticket validator's idea for a plane.
  Held to it, a Boarding Pass whose flight has neither departed nor been cancelled, and that has
  not been used, is marked boarded; the screen shows a green tick and the validator's accept tone
  sounds. Otherwise a red cross, the refusal tone, and the action bar says why. It does not check
  the gate: which gate a scanner stands at is the builder's business, and the schedule has no way
  to know.
- **Gate sign** (`BlockGateSign`): GATE over the letter (black on yellow) and number (yellow on
  charcoal), hung on rods, on both faces. A click steps the number 1 to 20, a sneaking click steps
  the letter A to D. The gate is one value 1 to 80 in the platform signs' `TileEntityPlatformSign`,
  read as two actual-state properties, `letter` and `number`, each of which swaps the texture of
  its own cell, so 24 textures make every gate. The back's cells are laid out mirrored, so it reads
  the same from behind.
- **Airport seating**: beam seating with upholstered seats, chrome armrests and a T leg, black or
  blue. It is `BlockPlatformBench` with its own model: the seats are in the platform bench's places
  (`SEATS`), so the bench's sitting code works unchanged, and seats side by side join the same way.

### Baggage claim

`BlockBaggageCarousel` is one block of carousel that joins on all four sides (world sides, actual
state) into a loop of any size; a 2 x N rectangle is the usual carousel. Which of its sides are
open picks its top from sixteen rules in the blockstate: one open side is a straight run of plates
(an animated texture, `carousel_belt`, eight frames) turned so the plates move **clockwise round
the loop** seen from above; two open sides that meet are a corner whose plates fan round the
inner corner; no open side is the middle island of a loop three or more wide; anything else (the
ends of a one-wide run, a lone block) is a round end plate. An open side gets the stainless skirt
and its rubber bumper. The carousel stores nothing, so it needs no facing: the plates' direction
follows from where the loop's edge is.

### Carts and wayfinding

- **Luggage cart**: a ribbed bed on four small wheels, a nose stop and a ladder frame up to a red
  grip. **Cart rack** (`BlockPlatformRun`): nested carts, three a block, between two blue guide
  rails, running along the player's left and right; a hoop at one end of a row and a post with
  the CARTS sign at the other.
- **Wayfinding signs**: hanging panels 24 units wide (reaching four past each side of their
  block), yellow capitals on charcoal with a black pictogram on a yellow square and an arrow:
  GATES (a plane taking off), ARRIVALS (a plane landing), CHECK-IN (a figure at a desk), BAGGAGE
  CLAIM (a case over a belt) and GROUND TRANSPORT (a bus over a taxi). Each has its own back art
  with the pictogram and the arrow swapped over, so from either side the arrow points the same way
  in the world; turning the sign round points it the other way.

### The large hanging sign

The one-block wayfinding signs read as specks under a terminal ceiling fourteen blocks up, so
there is a large one beside them (they are unchanged): `airport_wayfinding_panel`, a backlit
panel built to size from cells, the legend set in a screen rather than baked.

- **Building it.** Cells placed side by side and stacked, facing the same way (each faces the
  player who places it), join into one panel up to 16 wide and 6 tall (`WayfindingSign.MAX_*`).
  Each cell's baked model is a graphite slab 4 px deep (z 6..10 facing north) with a frame on the
  panel's outer edges only, picked by four actual-state booleans (`edge_left`, `edge_right`,
  `edge_top`, `edge_bottom`, named as the reader sees them), so the state count is 4 x 16.
- **The controller** is the bottom-left cell as read. Its width is how far its row runs to the
  reader's right, its height how far its column runs up; a cell outside that rectangle (a ragged
  build, or past 16 x 6) is drawn plain. Only the controller's renderer
  (`TileEntityWayfindingPanelRenderer`) draws: the legend once across the whole face, fullbright
  as a backlit sign is, and the same on the back with the arrow mirrored so it points the same
  way in the world (`doubleSided`, on by default). Past 96 blocks only the slab shows.
- **Setting it up.** Right-click any cell with an empty hand (GUI 46): two lines (printable ASCII,
  32 characters), cycle buttons for the pictogram (none, departures, arrivals, check-in, baggage,
  ground transport, train, bus, taxi, restrooms, exit), the arrow (none and eight directions) and
  the colours (Airport yellow on charcoal with a yellow pictogram square, Metro white on navy,
  Exit white on green, Information white on blue), the back toggle, and a Preset button that sets
  the whole sign: GATES, ARRIVALS, CHECK-IN, BAGGAGE CLAIM, GROUND TRANSPORTATION, TO TRAINS,
  EXIT, RESTROOMS. A preset with no arrow of its own keeps the sign's, since which way a sign
  points depends on where it hangs. Every change is sent at once (`WayfindingPanelPacket`); the
  server checks reach to the cell clicked, `allowEdit` and `isBlockModifiable`, cleans the lines
  again, and writes the sign to every cell of the panel.
- **Every cell holds the sign.** An edit writes them all, and a cell a player places against a
  panel copies its neighbour's, so adding a column on the left or a row below (which moves the
  controller) or breaking the controller keeps the legend. `/fill` and `/setblock` do not run
  placement, so cells made that way start as the default GATES sign until edited.
- **Scripting it.** A `/blockdata` edit of any cell is the whole panel's, as a GUI edit is:
  `TileEntityWayfindingPanel.readNBT` hands it to `WayfindingSign.applyToPanel`, the same method
  the GUI's packet handler calls, which writes the sign to every cell of the panel and syncs each
  to the clients. So a script places the cells with `/setblock` (facing in the metadata, 0 south,
  1 west, 2 north, 3 east) and then sets the sign on any one of them, for example
  `/blockdata x y z {l1:"BAGGAGE",l2:"CLAIM",p:"baggage",a:"down_left",s:"airport",d:1b}`. The keys:
  `l1` and `l2` the two lines (printable ASCII, 32 characters, cleaned as the GUI's are; an empty
  line is left out of the NBT), `p` the pictogram (`none`, `depart`, `arrive`, `checkin`,
  `baggage`, `ground`, `train`, `bus`, `taxi`, `restroom`, `exit`), `a` the arrow (`none`, `left`,
  `right`, `up`, `down`, `up_left`, `up_right`, `down_left`, `down_right`), `s` the colours
  (`airport`, `metro`, `exit`, `info`) and `d` whether the back carries the legend (a byte, on
  unless `0b`). An unknown id reads as the list's first value. Only a live edit spreads: a cell
  read as its chunk loads is not yet in the world (`isBlockLoaded` is false and it is not the tile
  entity at its position), and the cells the spread writes do not spread it again (a static flag).
- **Layout.** The pictogram is a square the panel's height less its margins, at the end away from
  the arrow; the arrow is 0.8 of that at the end it points to (the left end for the three
  leftward arrows); the text sits left-aligned between them in Highway Gothic, its capitals 0.45
  of the panel's height on one line or 0.28 each on two, both lines scaled down together to fit
  the width. A long word on a narrow panel is therefore small: GROUND / TRANSPORTATION wants a
  panel seven or more wide at two tall.
- **Hanging.** Over the top row the renderer hangs two rods, or one for about every four blocks of
  a wider panel, each from the panel's top up through air to the first block above, at most 24
  blocks; a rod with a block right on top of the panel, or with nothing within reach, is not drawn.
- **What it costs.** The layout (controller or not, size, rods, render box) is cached on the tile
  entity and worked out again only when a chunk section holding a cell was rebuilt (the block's
  `getActualState` under a `ChunkCache` bumps a counter, which is how a neighbour change shows on
  the client) or every two seconds for a ceiling changing out of reach of that signal; a cell that
  is not the controller costs two block lookups. The glyphs are one display list per controller,
  keyed by the text and placed and scaled outside it; the background, pictogram and arrow are a
  quad each. The renderer is global, so a sixteen-block panel whose controller's section is culled
  still draws.
- **Sprites.** The ten pictograms (64 px, black art on a white square the renderer tints) and the
  arrow are `wayfinding_picto_<id>` and `wayfinding_arrow`, drawn by the generator's
  `wayfinding_panel()` from the same `pictogram()` drawings as the small signs plus a train, a bus,
  a taxi, a restroom pair and a running figure at a doorway, all generic. No model face draws them,
  so the renderer's `Sprites` handler puts them on the atlas at the stitch; the item model names
  them too, unused, so `atlas_budget.py` counts them.

### Traps

- **The airlines are named in two places.** `FlightSchedule.AIRLINES` and the generator's
  `AIRLINES` must list the same airlines in the same order: the desk's `airline` metadata indexes
  both.
- **A property value must be lower case.** The gate sign's letters are `a` to `d` in the
  blockstate, because a property's values are checked against `[a-z0-9_]+`; `BlockGateSign.Letter`
  returns its name in lower case for the same reason.
- **Do not add a second metal detector.** Life Safety has one; a Transit one clashed on its sound
  event name (`metal_detector_alarm`, which `CsmSoundsTest` holds to one module) before it was cut.
- **Keep a renderer's face clear of the model's.** The baked screen stands 0.4 proud of the
  bezel (`SCREEN_Z` 14.6) on purpose: at 0.1, `model_depth.separate` treated the two as a clash and
  moved the screen forward, past the depth the renderer draws at, and the whole lit screen and its
  text vanished behind the baked one with no error. A face a renderer draws on must be more than
  0.2 from any other face of its model.
- **The large sign's enums are saved by id.** `WayfindingSign`'s pictograms, arrows and schemes
  are written to NBT by their `id` strings (`p`, `a`, `s`), never their ordinals, so they may be
  reordered or added to; the pictogram ids are also the sprites' names in the generator's
  `PANEL_PICTOGRAMS`, which must list the same ones.
- **The large board reads its bands off the small board's texture.** Its renderer's `HEADER_V`,
  `COLHEAD_V0`/`_V1`, `ROW0_V`, `ROW1_V` and `TITLE_U` are texels of the 256 square `fids()` draws
  the screen on (the header's 27 rows, the column heads' band to 40, the first two rows' bands, the
  title ending before x 160): move a band in `fids()` and the large board samples the wrong one,
  with no error. They sample each row band's middle, so a band's edge blurred by the texture's
  reduction to 128 never shows.
- **The boards follow the world's time of day, not total time.** `/time set` jumps the boards with
  the clocks, which is right; total world time would put them out of step with every clock in the
  mod.

### Prices

`TransitFabricatorRules.airport`: the desks two sheet metal, a control board and a fastener kit;
the scale a sheet metal, a control board and a wiring harness; the kiosk a control board, an LED
module and sheet metal; the X-ray an enclosure shell, a control board, an optical sensor and a
wiring harness; the pass scanner a control board, an optical sensor and sheet metal; a board an LED
module, a control board and sheet metal, and each cell of a large board an LED module and sheet
metal; the carousel two sheet metal, a wiring harness and a
fastener kit; a stanchion a pole section and a fastener kit; trays and the cart a sheet metal and a
fastener kit; the signs a sign blank and a fastener kit, and each cell of the large hanging sign a
sign blank and an LED module; the rollers, divesting table, seating and
cart rack two sheet metal and a fastener kit. `audit_fabricator_costs.py` mirrors the branches.

### Airside

Twenty-six more blocks in `transit.airport`, all drawn by `gen_transit_airside.py`: the airfield's
lights and signs, the masts they stand on, the stand sign, ground equipment and the jet bridge.
**There are no aircraft of any kind**, static or otherwise, so nothing docks or taxis: a jet
bridge is a corridor to walk out along and ground equipment stands where it was parked. Apron and
runway paint is the external road mod's road paint, and cones are Roads' work-zone cones, so
neither is drawn here. No exit or emergency-exit signs either: Life Safety has them.

| Block | Registry name | Class |
|---|---|---|
| Runway Edge Light, Taxiway Edge Light, Runway Threshold Light | `csm:airport_runway_edge_light`, `_taxiway_edge_light`, `_runway_threshold_light` | `BlockAirfieldLight` |
| Runway Centreline Light, Taxiway Centreline Light, Stop Bar Light (inset) | `csm:airport_runway_centreline_light`, `_taxiway_centreline_light`, `_stop_bar_light` | `BlockAirfieldLight` |
| Approach Light Bar, Airport Beacon | `csm:airport_approach_light_bar`, `csm:airport_beacon` | `BlockAirfieldLight` |
| Wind Sock, Antenna Mast (obstruction lit) | `csm:airport_wind_sock`, `csm:airport_antenna_mast` | `BlockAirfieldLight` |
| Taxiway Location, Taxiway Direction, Runway Holding Position and Runway Distance Remaining Signs | `csm:airport_taxiway_location_sign` and the rest | `BlockAirfieldSign` |
| Airfield Mast | `csm:airport_airfield_mast` | `transit.platform.BlockPlatformColumn` |
| Stand Sign | `csm:airport_stand_sign` | `BlockStandSign` |
| Wheel Chocks, Ground Power Unit, Baggage Tug, Baggage Cart, Air Stairs | `csm:airport_wheel_chocks` and the rest | Roads' `streetscape.BlockUtilityBox` |
| Jet Bridge (Tunnel, Cab, Rotunda) | `csm:airport_jet_bridge_tunnel`, `_cab`, `_rotunda` | `BlockJetBridge` |
| Jet Bridge (Drive Leg, Rotunda Column) | `csm:airport_jet_bridge_drive`, `_column` | `transit.platform.BlockPlatformColumn` |

**Airfield lights are simple on purpose.** The lights live in Transit rather than Lighting, and
Transit may name only Core and Roads, so none of Lighting's light logic (the 4-state control, the
light-up air) is used. A light is lit or not (`lit`, in its metadata beside the facing, with
`powered` above it): the blockstate swaps each lens for its lit texture, which has an `_e`
companion for OptiFine's emissive rendering, the lens faces are unshaded, and a lit light gives
block light (runway edge and threshold 12, runway centreline 11, taxiway lights and signs 10,
approach and beacon 15, obstruction 9), bright enough for a lit lens to read at night without
OptiFine. There is no renderer, no tile entity (except the signs'), and nothing ticks. `powered`
only remembers the redstone, and a sign's lens is its legend, lit by block light alone, so no
model reads `powered` (nor, on a sign, `lit`, nor `arrow` except on the direction sign): the
blockstates have no variants for them, and `BlockAirfieldLight.propertiesNoModelReads` leaves them
out of the model locations (a sign 36 locations rather than 288).

**Lights switch a circuit at a time.** A real airfield switches its runway and taxiway lighting
as circuits, and a runway may have a hundred lights, so one lever per light would be useless.
Every light carries a circuit (`runway`: edge, threshold, centreline and approach; `taxiway`:
blue edge, green centreline, the stop bar and the signs; `beacon`; `obstruction`), and all the
lights of one circuit within eight blocks across and two up or down of another are one circuit,
found by a flood fill (at most 512 lights). A click with an empty hand on any light, or a change
of redstone power at any light (on when power comes, off when it goes, the shelters' rule),
switches the whole circuit; the action bar says how many lights it reached. A light placed next
to a lit circuit comes on with it. Keep two runways' lights more than eight blocks apart if they
are to switch separately.

**The beacon rotates by texture.** Its lens band is an octagon whose eight sides each take their
own eight-frame animated strip, the frames a step apart from side to side: a side flashes white
when the beam passes it and green half a turn later. So the flash runs round the lens with no
renderer, and every beacon in the world turns in step.

**Signs** stand on two frangible legs, the legend on the front and a dark back: taxiway location
(yellow on black, A to H), direction (black on yellow, a letter and an arrow either side of it),
runway holding position (white on red: 4-22, 9-27, 13-31, 18-36, ILS) and distance remaining
(white on black, 1 to 9). What a sign reads is one value in the platform signs'
`TileEntityPlatformSign`, read as `legend` (which swaps the face's texture) and, on the direction
sign, `arrow` (which picks one of two models, the arrow's cell on that side). A click steps the
legend; a sneaking click turns the arrow round, or steps any other sign back. Every sign's
`legend` runs 1 to 9, so one class serves all four; the labels the action bar shows are passed in
the tab line from the generator, the one list of what the textures say. Signs are on the
taxiway circuit.

**The airfield mast** is a slim galvanised pole that stacks (the platform column's rule: a plinth
where nothing is below, a cap where nothing is above), the same thickness as the posts of the
approach light bar, the wind sock and the stand sign, so each of them stands on it and reads as
one pole. The **stand sign** is the gate sign twice the size on a post, with the gate sign's
cells, tile entity and clicks (1 to 20, sneaking A to D); only its message says "Stand".

**Ground equipment** is placed through Roads' `BlockUtilityBox`, which Transit may use because it
requires Roads: it settles onto a road surface (an apron built from the external road mod's
surfaces), and a piece two blocks long is placed whole or not at all, drawn by its root, with
Roads' invisible `utility_box_part` filling the other cells and breaking the whole. The tug, the
covered baggage cart, the ground power unit and the towable air stairs are one block wide, two
long and up to two tall, at about two thirds of real size. They are props: their collision is the
whole unit's box, so the air stairs cannot be climbed.

**The jet bridge** is a corridor two blocks wide and two tall (outside), centred on a line of
blocks: the player walks down the middle of the line with the walls half a block out either side
and 29 sixteenths between floor and ceiling, a hair over a player's height. Three ways were
weighed. A build-to-size shell like the job trailer (walls, floor and roof of one block, inside or
outside worked out from the neighbours) would be walkable and any size, but it is that block's
render updater, beads and rays for a corridor that only ever runs straight; the garage doors' build-to-size
opening does not fit a corridor at all. Invisible part blocks (the bounce castle) would stop other
blocks being placed inside the walls, but every tunnel would then place and break five blocks.
One block drawing and colliding past its cell was the simplest that looks right: a model may reach
a block past its cell, and the game looks a block past an entity's box for collision boxes, so
the walls and roof are solid with nothing else placed. What the player aims at is only the floor,
so inside the corridor everything else can still be clicked.

- **Tunnel**: carpet, panelled walls with a window a block, a lit strip down the ceiling, steel
  outside with a navy band. Tunnels facing along the same axis join; a frame closes each end
  that does not continue (`ahead`, `behind`, actual state).
- **Cab**: wider, with big windows, a console, the canopy's bellows round the open front, a
  hazard-striped bumper and a yellow safety bar that collides to a fence's height, so the open end
  cannot be walked off.
- **Rotunda**: an octagonal room three blocks across on its block, open north and south to the
  corridor's width, with a collar out to the edge of its three blocks, so a tunnel two blocks from
  it joins it. Chamfered corners are walls turned 45 degrees; their floor is a turned square a
  hair below the floor.
- **Drive leg** and **rotunda column** (`BlockPlatformColumn`): the leg's two posts stand under
  the tunnel's walls, with a yoke under the floor where nothing stacks above and the wheel bogie
  where nothing stacks below; the column is a thick round pier with a plinth and a head. Build the
  bridge up on them to the terminal's upper floor.
- **Sloped tunnel** (`BlockJetBridgeSlope`, 2026-10): a real bridge runs down from the terminal's
  door to the aircraft's lower sill (or up, to a big jet's). A run of sloped pieces drops one whole
  block over 8 pieces (about 7 degrees) or 4 (about 14), so the piece after it meets a level
  tunnel, the cab or the next run a block lower; sneak and use with an empty hand to cycle the
  grade (down or up, over 8 or 4). Each piece counts the pieces of its grade and facing uphill of
  it on its own level for its place in the run (`step`), so a run is just placed in a line, eight
  (or four) on a level and then on a block lower. The model is the level tunnel's corridor sheared
  along its length, as OBJ (`jet_bridge_slope` in `gen_transit_airside.py`, one a grade and step,
  written from the same elements as the level tunnel's JSON), since a JSON element can be neither
  sheared nor turned to that angle. Level pieces count a sloped run a block above or below as
  carrying on, so no frame closes the join.

- **Large jet bridge** (2026-10): for terminals built at a large scale, with 14-block ceilings
  and more, where the level bridge reads as a pipe. The tunnel, sloped tunnel, cab and drive leg
  again, three blocks wide and four tall inside (`airport_jet_bridge_large_*`). It is the same
  bridge scaled, drawn from the same elements (`gen_transit_airside.py`'s `jet_bridge_large`,
  `LJ_SX` and `LJ_SY`): across about the block's middle, and up above the floor's top, so the
  floor keeps its thickness and both sizes meet a door at the same height. The cab's console,
  bumper and safety bar keep their real heights. Every piece reaches up to two and a half blocks
  past its cell, so all of it is OBJ. It still needs no other block. The game looks for collision
  boxes in the blocks within a block of the *player's* box, not of the block's, and a player
  inside a three-wide corridor is never more than a block from its centre line, nor far above its
  floor. So the root's boxes (`BlockJetBridge.toLarge`, the level boxes scaled the same way) are
  always looked at. The two sizes do not join each other. There is no large rotunda: a room that
  wide would put a player more than a block from its root, and it would need part blocks.

Each piece gives light 9 inside. The facing is the way to the aircraft (the model's north), the
same for every piece of one bridge.

#### Traps

- **A sloped floor has to be walked by a player 0.6 blocks long.** A player stands on the highest
  floor under any of their footprint, which covers two or three of the slope's sixteenth steps, so
  a floor and roof that collided where they are drawn stopped a player at the second step (their
  head, still at the level tunnel's height, met the lowered roof) and again at the cab. The
  sloped tunnel's floor collides a player's length ahead of where it is drawn and its roof a few
  sixteenths above (`BlockJetBridgeSlope.boxes`): feet sink a sixteenth or two into the carpet,
  and nobody sees the roof. Walked down and up both grades, 2026-10-04.

- **The large jet bridge collides only from inside.** From outside, a player more than a block
  from the root's cell (beside a wall, or on the roof) is out of the reach that makes a single
  block work: the roof cannot be stood on, and an outer wall stops a player only once they are
  inside its thickness. Nobody walks there on a real bridge either.
- **The jet bridge's numbers are in two places.** `BlockJetBridge`'s collision boxes and floors
  and the generator's `JB_*`, `CAB_*` and `ROT` share the section; change one, change both.
- **An element may not reach past -16 or 32**, so the corridor stops at two blocks tall, the
  rotunda at three across, and its chamfer walls are sized so that their unturned box stays inside
  the limit. A long diagonal (the air stairs' rails and stringers) is several short pieces turned
  45 degrees.
- **A beacon's side is known by its bearing.** An octagon's four boxes have plain sides and sides
  turned 45 degrees (a positive turn about y takes east to north-east); `airfield_lights` gives
  each side the strip for its bearing. Get the table wrong and the flash jumps about instead of
  running round.
- **The wheel chocks' lower half is in the ground.** Each chock is a square prism turned 45
  degrees about x with its centre on the ground, so only the triangle above shows; on a partial
  road surface the utility box settles the chock down onto it.
- **A sign's legend must not outrun its textures.** `legend` runs 1 to 9 on every sign; the
  blockstate gives values past a sign's own count its last face, and the Java clamps the stored
  value to the sign's count.
- **Circuits are found by distance, not wiring.** Two circuits of the same kind closer than eight
  blocks are one; that is the rule to tell a builder.

#### Prices

`TransitFabricatorRules.airside`: a light a lens assembly, an LED module and a fastener kit (the
approach bar two of each and sheet metal, the beacon two lenses, an LED and a control board); a
lit sign a sign blank, an LED and a fastener kit; the stand sign a sign blank and a fastener kit;
the masts a pole section; the wind sock and antenna mast a pole section and a lens; the tug three
sheet metal, a control board and a wiring harness; the ground power unit an enclosure shell, a
control board and two harnesses; the cart two sheet metal; the stairs and a tunnel three sheet
metal; the cab adds a control board; the rotunda four sheet metal; the drive leg two pole
sections and a harness; the column two concrete mix; the chocks one sheet metal.
`audit_fabricator_costs.py` mirrors the branch.

## Demo world

`dev-env-utils/scripts/build_transit_demo.py` builds, over MCMCP, a walkable layout of the whole
Transit tab in a fresh flat creative world (the saved one is called "Transit Demo"): a bus street
with a stop for every agency (flags hung left and right, a back-to-back pair, a flag set back onto a
traffic pole, cases, arrival displays, the plaque), every shelter in every livery with Signage's
ad panel on one, a bus concourse with a board bank and bay displays; a subway station with the
kiosk and the open stair, the concourse, fare line and booth below and an open-cut platform with
the whole fit-out; an airport terminal with check-in, the queue, security, the gate lounge,
boards, baggage claim and carts; and the airside four blocks below it, with the jet bridge, a stand
and its ground equipment, the wind sock, beacon and antenna mast, and a taxiway and runway whose
four circuits are each switched by a labelled lever. It checks the tab's registration list against
what it placed and puts anything left over on a row of signed plinths, so a new Transit block
shows up in the demo without anyone remembering to add it. The landside is raised to y 8 so the
station's lower level fits above bedrock. `--client-port`, `--server-port` and `--config` point it
at a client other than the dev client.

Two things it has to do that a builder placing by hand does not: throw each circuit's lever off
and on again at the end (a light placed takes the state of the circuit it joins, so a batch placed
at once comes up dark), and close the ad panel's setup screen, which opens when the panel is
placed.

## Where the module is going

Every phase planned for the module shipped: the fare system's move, bus stops on Roads' sign
posts with side-hung flags, shelters with Signage's ad panel, the station and platform fit-out,
stations, the bus departure boards, and an airport's terminal and airside. What was left out, and
why, so it is not built by mistake:

- **RCMC's side of a station.** No platform or platform edge block, PA speaker, platform arrival
  board, line diagram strip or track-side piece (third-rail cover, buffer stop): RCMC has them or
  lays its track as splines. No train departure board either; RCMC's arrival boards serve its
  platforms.
- **Signage that is not wayfinding.** No "to trains" or "to street" direction signs beyond the
  platform fit-out's TO TRAINS and EXIT, and no exit signage at all: Life Safety has plenty. No
  floor PAID AREA marking: one block of stencil does not read, and floor paint is the road mod's.
- **Aircraft**, static or moving, and a control tower's glazed cab (tilted glass corners do not
  work in JSON elements; Building's glazing builds a cab, which the beacon and antenna mast top). No
  PAPI (its colour depends on the angle it is seen from), sloping jet bridge pieces, belt loader or
  container dollies, and the wind sock does not swing.
- **The walk-through metal detector** is Life Safety's, and duty-free and cafe fit-out is Market &
  Store's.
- **Vehicles, road markings and real branding** will not be added: CSM builds infrastructure,
  road paint belongs to the external road mod, and every agency, airline and livery is invented.

What it could grow into, each a candidate rather than a plan:

- a settable bay number on a bus stop (bays are numbered by world order among the stops a board
  sees), a setup screen for the departure boards, and speech in other languages;
- a gate information display over the gate desk, which needs a gate to find its flights (a flight
  per gate is rare with 80 gates and 72 departures a day), a gate check on the boarding pass
  scanner, and diagonal queue belts;
- a jet bridge whose length telescopes or swings, and a visual docking guidance display at a stand;
- fewer block atlas pixels: Transit's textures are 2.78 Mpx of an atlas that is 95.1% full.

Two rules hold throughout: every agency, livery and route bullet is invented, never a real transit
brand; and an advertising panel in a shelter is Signage's board, set into the shelter by the
player, since a module may reference only Core and the modules it requires (for Transit, Roads).
